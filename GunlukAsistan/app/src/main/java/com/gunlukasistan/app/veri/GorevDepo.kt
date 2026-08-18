package com.gunlukasistan.app.veri

import android.content.Context
import com.gunlukasistan.app.Store
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.76 — Görevler için Room köprüsü.
 *
 * ── Görev ──
 * `Store.loadTasks/saveTasks` çağrılarını Room'a yönlendirir. Store'un
 * dış API'si değişmediği için çağıran 76 dosyanın hiçbiri düzenlenmedi.
 *
 * ── Güvenlik ağı ──
 * Room'a yazarken JSON gölge kopyası da güncel tutulur. Böylece:
 *   · Yedekleme/geri yükleme (`exportJson`/`importJson`) aynen çalışır
 *   · [roomAktif] kapatılırsa uygulama hiçbir şey kaybetmeden eski
 *     yola döner
 *   · Room okuması hata verirse otomatik olarak JSON'a düşülür
 *
 * ── İlk açılış ──
 * Veritabanı boşsa mevcut JSON verisi bir kez içeri aktarılır
 * ([gerekirseTasi]). Kullanıcı hiçbir şey fark etmez.
 */
object GorevDepo {

    private const val TAG = "GorevDepo"
    private const val PREF = "veri_gecis_v1"

    /** Room kullanılsın mı? Sorun çıkarsa kapatılıp eski yola dönülür. */
    @Volatile
    var roomAktif: Boolean = true

    /** Geçiş bir kez yapıldı mı? */
    private fun tasindiMi(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("gorev_tasindi", false)

    private fun tasindiIsaretle(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean("gorev_tasindi", true).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // DÖNÜŞÜM
    // ═══════════════════════════════════════════════════════════════

    private fun adimlariKodla(adimlar: List<Store.SubItem>): String = try {
        JSONArray().also { d ->
            adimlar.forEach { a ->
                d.put(
                    JSONObject()
                        .put("id", a.id).put("text", a.text)
                        .put("done", a.done).put("createdAt", a.createdAt)
                )
            }
        }.toString()
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Adımlar kodlanamadı", e); "[]"
    }

    private fun adimlariCoz(json: String): MutableList<Store.SubItem> {
        val liste = mutableListOf<Store.SubItem>()
        try {
            val d = JSONArray(json)
            for (i in 0 until d.length()) {
                val o = d.optJSONObject(i) ?: continue
                liste.add(
                    Store.SubItem(
                        id = o.optLong("id"),
                        text = o.optString("text"),
                        done = o.optBoolean("done"),
                        createdAt = o.optLong("createdAt")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Adımlar çözülemedi", e)
        }
        return liste
    }

    private fun varliga(g: Store.Task) = GorevVarlik(
        id = g.id,
        metin = g.text,
        bitti = g.done,
        olusturuldu = g.createdAt,
        sonTarih = g.dueAt,
        tekrar = g.tekrar,
        tekrarBitis = g.tekrarBitis,
        yapildi = g.yapildi,
        etiket = g.etiket,
        arsiv = g.arsiv,
        arsivZaman = g.arsivZaman,
        adimlarJson = adimlariKodla(g.adimlar)
    )

    private fun goreve(v: GorevVarlik) = Store.Task(
        id = v.id,
        text = v.metin,
        done = v.bitti,
        createdAt = v.olusturuldu,
        dueAt = v.sonTarih,
        tekrar = v.tekrar,
        tekrarBitis = v.tekrarBitis,
        yapildi = v.yapildi,
        etiket = v.etiket,
        arsiv = v.arsiv,
        arsivZaman = v.arsivZaman,
        adimlar = adimlariCoz(v.adimlarJson)
    )

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Görevleri okur.
     *
     * @param jsonYedek Room kullanılamazsa çağrılacak eski okuma yolu
     */
    fun oku(context: Context, jsonYedek: () -> MutableList<Store.Task>): MutableList<Store.Task> {
        if (!roomAktif) return jsonYedek()
        return try {
            gerekirseTasi(context, jsonYedek)
            Veritabani.al(context).gorevDao().hepsi()
                .map { goreve(it) }
                .toMutableList()
        } catch (e: Exception) {
            // Room bozulduysa uygulama çalışmaya devam etmeli
            android.util.Log.w(TAG, "Room okunamadı, JSON'a dönülüyor", e)
            roomAktif = false
            jsonYedek()
        }
    }

    /**
     * Görevleri yazar.
     * @return Room'a yazıldıysa true (çağıran yine de JSON'u tazeler)
     */
    fun yaz(context: Context, gorevler: List<Store.Task>): Boolean {
        if (!roomAktif) return false
        return try {
            Veritabani.al(context).gorevDao()
                .tumunuDegistir(gorevler.map { varliga(it) })
            true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Room'a yazılamadı", e)
            roomAktif = false
            false
        }
    }

    /** İlk açılışta mevcut JSON verisini veritabanına taşır. */
    private fun gerekirseTasi(context: Context, jsonYedek: () -> MutableList<Store.Task>) {
        if (tasindiMi(context)) return
        try {
            val dao = Veritabani.al(context).gorevDao()
            if (dao.sayi() == 0) {
                val eski = jsonYedek()
                if (eski.isNotEmpty()) {
                    dao.tumunuDegistir(eski.map { varliga(it) })
                    android.util.Log.i(TAG, "Görevler taşındı: " + eski.size)
                }
            }
            tasindiIsaretle(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Geçiş yapılamadı", e)
        }
    }

    /**
     * Yedekten geri yükleme sonrası çağrılır.
     * JSON kaynak kabul edilip veritabanı yeniden kurulur.
     */
    fun jsondanTazele(context: Context, gorevler: List<Store.Task>) {
        if (!roomAktif) return
        try {
            Veritabani.al(context).gorevDao()
                .tumunuDegistir(gorevler.map { varliga(it) })
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Tazelenemedi", e)
        }
    }
}
