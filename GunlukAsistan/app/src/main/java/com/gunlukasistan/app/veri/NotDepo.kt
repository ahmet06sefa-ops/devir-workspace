package com.gunlukasistan.app.veri

import android.content.Context
import com.gunlukasistan.app.Store

/**
 * v8.1 — Notlar için Room köprüsü (öneri 3).
 *
 * [GorevDepo] ile aynı desen: `Store.loadNotes/saveNotes` imzaları
 * değişmiyor, çağıran dosyaların hiçbiri düzenlenmiyor. Store içeride
 * Room'a yazıyor, JSON'u da gölge kopya olarak güncel tutuyor.
 *
 * ── Güvenlik ağı ──
 * · Yedekleme (`exportJson`) JSON'u okuduğu için aynen çalışıyor
 * · [roomAktif] kapatılırsa hiçbir şey kaybetmeden eski yola dönülüyor
 * · Room okuması hata verirse otomatik JSON'a düşülüyor
 */
object NotDepo {

    private const val TAG = "NotDepo"
    private const val PREF = "veri_gecis_v1"

    /** Room kullanılsın mı? Sorun çıkarsa kapatılıp eski yola dönülür. */
    @Volatile
    var roomAktif: Boolean = true

    private fun tasindiMi(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("not_tasindi", false)

    private fun tasindiIsaretle(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean("not_tasindi", true).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // DÖNÜŞÜM
    // ═══════════════════════════════════════════════════════════════

    private fun varliga(n: Store.Note) = NotVarlik(
        id = n.id,
        baslik = n.title,
        icerik = n.content,
        olusturuldu = n.createdAt,
        gorsel = n.image
    )

    private fun nota(v: NotVarlik) = Store.Note(
        id = v.id,
        title = v.baslik,
        content = v.icerik,
        createdAt = v.olusturuldu,
        image = v.gorsel
    )

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notları okur.
     *
     * @param jsonYedek Room kullanılamazsa çağrılacak eski okuma yolu
     */
    fun oku(context: Context, jsonYedek: () -> MutableList<Store.Note>): MutableList<Store.Note> {
        if (!roomAktif) return jsonYedek()
        return try {
            gerekirseTasi(context, jsonYedek)
            Veritabani.al(context).notDao().hepsi()
                .map { nota(it) }
                .toMutableList()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Room okunamadı, JSON'a dönülüyor", e)
            roomAktif = false
            jsonYedek()
        }
    }

    /** @return Room'a yazıldıysa true (çağıran yine de JSON'u tazeler) */
    fun yaz(context: Context, notlar: List<Store.Note>): Boolean {
        if (!roomAktif) return false
        return try {
            Veritabani.al(context).notDao().tumunuDegistir(notlar.map { varliga(it) })
            true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Room'a yazılamadı", e)
            roomAktif = false
            false
        }
    }

    /** İlk açılışta mevcut JSON verisini veritabanına taşır. */
    private fun gerekirseTasi(context: Context, jsonYedek: () -> MutableList<Store.Note>) {
        if (tasindiMi(context)) return
        try {
            val dao = Veritabani.al(context).notDao()
            if (dao.sayi() == 0) {
                val eski = jsonYedek()
                if (eski.isNotEmpty()) {
                    dao.tumunuDegistir(eski.map { varliga(it) })
                    android.util.Log.i(TAG, "Notlar taşındı: " + eski.size)
                }
            }
            tasindiIsaretle(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Geçiş yapılamadı", e)
        }
    }

    /** Yedekten geri yükleme sonrası: JSON kaynak kabul edilip DB yenilenir. */
    fun jsondanTazele(context: Context, notlar: List<Store.Note>) {
        if (!roomAktif) return
        try {
            Veritabani.al(context).notDao().tumunuDegistir(notlar.map { varliga(it) })
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Tazelenemedi", e)
        }
    }
}
