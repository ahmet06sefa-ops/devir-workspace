package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.99 — Yapay zekâ yanıt önbelleği (öneri 4).
 *
 * ── Sorun ──
 * 16 dosyada AI çağrısı var, önbellek yalnızca 5 yerde kullanılıyordu.
 * Aynı soru iki kez sorulduğunda iki kez kota harcanıyor ve kullanıcı
 * ikinci kez de 5-15 saniye bekliyordu.
 *
 * En belirgin örnekler:
 *   · `KocMesaj` her açılışta motivasyon cümlesi üretiyor
 *   · `Sozluk` aynı terime tekrar bakılınca (kendi önbelleği var ama
 *     bağlam değişince ıskalıyor)
 *   · `SimdiNe` / plan önerileri gün içinde değişmiyor
 *
 * ── Tasarım ──
 * Anahtar = istemin **hash'i**. İstem birebir aynıysa cevap da aynı
 * olacaktır; farklıysa yeni istek atılır. Hash kullanılıyor çünkü istemler
 * kilobaytlarca olabiliyor, anahtar olarak saklamak israf.
 *
 * ── Neden JSON, neden Room değil ──
 * Önbellek **kaybedilebilir** veri: silinse en fazla bir kez daha AI'ya
 * gidilir. Room şeması eklemek bu risksiz veri için aşırı maliyet.
 *
 * ── Yaşam süresi ──
 * Varsayılan 24 saat. Motivasyon cümlesi ertesi gün tazelenmeli; terim
 * açıklaması ise değişmez, çağıran uzun süre verebilir.
 */
object AiOnbellek {

    private const val TAG = "AiOnbellek"
    private const val PREF = "ai_onbellek_v1"
    private const val K_KAYITLAR = "kayitlar_json"

    /** En fazla kaç yanıt saklanır — disk şişmesin. */
    private const val TAVAN = 120

    /** Varsayılan geçerlilik (ms). */
    const val GUN = 24 * 60 * 60 * 1000L
    const val HAFTA = 7 * GUN
    const val AY = 30 * GUN

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // AYAR
    // ═══════════════════════════════════════════════════════════════

    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", true)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
        if (!acik) temizle(context)
    }

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    private data class Kayit(
        val anahtar: String,
        val cevap: String,
        val zaman: Long,
        var kullanim: Int
    )

    private fun oku(context: Context): MutableList<Kayit> {
        val ham = prefs(context).getString(K_KAYITLAR, "[]") ?: "[]"
        val liste = mutableListOf<Kayit>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Kayit(
                        anahtar = o.optString("a"),
                        cevap = o.optString("c"),
                        zaman = o.optLong("z"),
                        kullanim = o.optInt("k", 1)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önbellek okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Kayit>) {
        // Tavanı aşarsa en az kullanılan ve en eski kayıtlar atılır
        val kirpik = if (liste.size <= TAVAN) liste
        else liste.sortedWith(
            compareByDescending<Kayit> { it.kullanim }.thenByDescending { it.zaman }
        ).take(TAVAN)

        val dizi = JSONArray()
        kirpik.forEach { k ->
            dizi.put(
                JSONObject().put("a", k.anahtar).put("c", k.cevap)
                    .put("z", k.zaman).put("k", k.kullanim)
            )
        }
        prefs(context).edit().putString(K_KAYITLAR, dizi.toString()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // KULLANIM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Önbellekten yanıt okur.
     *
     * @param istem AI'ya gönderilecek metin — hash'i anahtar olur
     * @param omur geçerlilik süresi (ms)
     * @return kayıtlı yanıt, yoksa/bayatsa null
     */
    fun al(context: Context, istem: String, omur: Long = GUN): String? {
        if (!acikMi(context)) return null
        return try {
            val anahtar = anahtarla(istem)
            val liste = oku(context)
            val kayit = liste.firstOrNull { it.anahtar == anahtar } ?: return null

            if (System.currentTimeMillis() - kayit.zaman > omur) {
                // Bayat — sil ve yokmuş gibi davran
                yaz(context, liste.filterNot { it.anahtar == anahtar })
                return null
            }

            kayit.kullanim++
            yaz(context, liste)
            kayit.cevap
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önbellek okunamadı", e)
            null
        }
    }

    /** Yanıtı önbelleğe koyar. */
    fun koy(context: Context, istem: String, cevap: String) {
        if (!acikMi(context)) return
        if (cevap.isBlank()) return
        // Çok büyük yanıtlar önbellekte tutulmaz (PDF anlatımı vb.)
        if (cevap.length > 20_000) return

        try {
            val anahtar = anahtarla(istem)
            val liste = oku(context).filterNot { it.anahtar == anahtar }.toMutableList()
            liste.add(Kayit(anahtar, cevap, System.currentTimeMillis(), 1))
            yaz(context, liste)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önbelleğe yazılamadı", e)
        }
    }

    /**
     * Önbellekli AI çağrısı — en pratik kullanım.
     *
     * Önce önbelleğe bakar; yoksa [uret] çalıştırılır ve sonuç saklanır.
     *
     * ```
     * val cevap = AiOnbellek.getir(ctx, istem, AiOnbellek.GUN) {
     *     AiClient.sadeIstek(ctx, istem, 400)
     * }
     * ```
     */
    fun getir(
        context: Context,
        istem: String,
        omur: Long = GUN,
        uret: () -> AiClient.Result
    ): AiClient.Result {
        al(context, istem, omur)?.let { return AiClient.Result(true, it) }

        val sonuc = uret()
        if (sonuc.ok && sonuc.text.isNotBlank()) {
            koy(context, istem, sonuc.text)
        }
        return sonuc
    }

    // ═══════════════════════════════════════════════════════════════
    // BAKIM
    // ═══════════════════════════════════════════════════════════════

    fun sayi(context: Context): Int = runCatching { oku(context).size }.getOrDefault(0)

    fun boyut(context: Context): Int = runCatching {
        oku(context).sumOf { it.cevap.length }
    }.getOrDefault(0)

    /** Kaç kez önbellekten okundu — kazancı göstermek için. */
    fun toplamKullanim(context: Context): Int = runCatching {
        oku(context).sumOf { (it.kullanim - 1).coerceAtLeast(0) }
    }.getOrDefault(0)

    fun temizle(context: Context) {
        prefs(context).edit().remove(K_KAYITLAR).apply()
    }

    /** Süresi geçmiş kayıtları atar. */
    fun bayatlariTemizle(context: Context, omur: Long = HAFTA): Int {
        return try {
            val simdi = System.currentTimeMillis()
            val liste = oku(context)
            val kalan = liste.filter { simdi - it.zaman <= omur }
            val silinen = liste.size - kalan.size
            if (silinen > 0) yaz(context, kalan)
            silinen
        } catch (e: Exception) {
            0
        }
    }

    fun ozet(context: Context): String {
        if (!acikMi(context)) return context.getString(R.string.ao_kapali)
        val n = sayi(context)
        if (n == 0) return context.getString(R.string.ao_bos)
        return context.getString(R.string.ao_ozet, n, toplamKullanim(context))
    }

    // ═══════════════════════════════════════════════════════════════
    // ANAHTAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * İstemden kararlı bir anahtar üretir.
     *
     * `String.hashCode` 32 bit; 120 kayıtta çakışma ihtimali ihmal
     * edilebilir ama yine de uzunluk ekleniyor — farklı uzunluktaki
     * iki istemin aynı anahtarı alma olasılığı pratikte sıfırlanıyor.
     */
    private fun anahtarla(istem: String): String {
        val temiz = istem.trim()
        return temiz.hashCode().toString(16) + "_" + temiz.length
    }
}
