package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.97 — PDF sayfa yer imleri ve sayfa notları (öneri 9).
 *
 * ── Kullanıcı isteği ──
 * "PDF okurken metin seçip renkli işaretleme ve kenar notu."
 *
 * ── Neden metin işaretleme yapılamadı — dürüst sınır ──
 * `LessonPdfActivity` PDF sayfalarını **bitmap olarak** çiziyor
 * (`PdfRenderer`). Bitmap'te metin katmanı yok; seçilebilir metin için
 * PDF'in içindeki karakter konumlarını okuyup ekrana bindirmek gerekir.
 * Bu, ayrı bir metin katmanı motoru demek — mevcut mimaride birkaç
 * sürümlük iş ve kaydırma/yakınlaştırma performansını riske atar.
 *
 * Bunun yerine aynı ihtiyacı karşılayan pratik çözüm: **sayfa bazlı**
 * yer imi + not. Kullanıcı "42. sayfada şu var" diye işaretleyip nota
 * yazabiliyor, listeden o sayfaya atlayabiliyor.
 *
 * ── Ders notundan farkı ──
 * `Store.lessonNote` ders başına **tek** not tutuyor. Burada sayfa başına
 * ayrı kayıt var; 300 sayfalık bir PDF'te tek not yetersizdi.
 */
object SayfaImi {

    private const val TAG = "SayfaImi"
    private const val PREF = "sayfa_imi_v1"
    private const val K_IMLER = "imler_json"

    /** Toplam saklanan im sayısı tavanı. */
    private const val TAVAN = 500

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // RENKLER
    // ═══════════════════════════════════════════════════════════════

    /** İşaret renkleri — kullanıcı kategorize edebilsin. */
    val renkler = listOf(
        GrafikDili.UYARI, // sarı — dikkat
        GrafikDili.BASARI, // yeşil — anladım
        GrafikDili.HATA, // kırmızı — zor
        0xFF2196F3.toInt(), // mavi — soru
        0xFF9C27B0.toInt()  // mor — sınavda çıkar
    )

    fun renkAdi(context: Context, indeks: Int): String = context.getString(
        when (indeks) {
            1 -> R.string.si_renk_anladim
            2 -> R.string.si_renk_zor
            3 -> R.string.si_renk_soru
            4 -> R.string.si_renk_sinav
            else -> R.string.si_renk_dikkat
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    data class Im(
        val lessonId: Long,
        val sayfa: Int,
        var not: String,
        var renk: Int,
        var zaman: Long,
        val dersAdi: String = ""
    )

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    fun hepsi(context: Context): MutableList<Im> {
        val ham = prefs(context).getString(K_IMLER, "[]") ?: "[]"
        val liste = mutableListOf<Im>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Im(
                        lessonId = o.optLong("l"),
                        sayfa = o.optInt("s"),
                        not = o.optString("n"),
                        renk = o.optInt("r"),
                        zaman = o.optLong("z"),
                        dersAdi = o.optString("d")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İmler okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Im>) {
        val dizi = JSONArray()
        liste.takeLast(TAVAN).forEach { im ->
            dizi.put(
                JSONObject()
                    .put("l", im.lessonId).put("s", im.sayfa)
                    .put("n", im.not).put("r", im.renk)
                    .put("z", im.zaman).put("d", im.dersAdi)
            )
        }
        prefs(context).edit().putString(K_IMLER, dizi.toString()).apply()
    }

    /** Bir dersin imleri — sayfa sırasına göre. */
    fun dersImleri(context: Context, lessonId: Long): List<Im> =
        hepsi(context).filter { it.lessonId == lessonId }.sortedBy { it.sayfa }

    fun sayfaImi(context: Context, lessonId: Long, sayfa: Int): Im? =
        hepsi(context).firstOrNull { it.lessonId == lessonId && it.sayfa == sayfa }

    fun varMi(context: Context, lessonId: Long, sayfa: Int): Boolean =
        sayfaImi(context, lessonId, sayfa) != null

    /** İm ekler ya da günceller. */
    fun kaydet(
        context: Context,
        lessonId: Long,
        sayfa: Int,
        not: String,
        renk: Int,
        dersAdi: String = ""
    ) {
        val liste = hepsi(context)
        val yer = liste.indexOfFirst { it.lessonId == lessonId && it.sayfa == sayfa }
        val yeni = Im(lessonId, sayfa, not.take(500), renk.coerceIn(0, renkler.size - 1),
            System.currentTimeMillis(), dersAdi)
        if (yer >= 0) liste[yer] = yeni else liste.add(yeni)
        yaz(context, liste)
    }

    fun sil(context: Context, lessonId: Long, sayfa: Int) {
        yaz(context, hepsi(context).filterNot {
            it.lessonId == lessonId && it.sayfa == sayfa
        })
    }

    fun dersiTemizle(context: Context, lessonId: Long) {
        yaz(context, hepsi(context).filterNot { it.lessonId == lessonId })
    }

    // ═══════════════════════════════════════════════════════════════
    // SORGULAR
    // ═══════════════════════════════════════════════════════════════

    fun sayi(context: Context): Int = hepsi(context).size

    fun dersSayisi(context: Context, lessonId: Long): Int =
        hepsi(context).count { it.lessonId == lessonId }

    /** Tüm imler, ders adına göre gruplu — genel liste ekranı için. */
    fun derseGore(context: Context): List<Pair<String, List<Im>>> =
        hepsi(context)
            .groupBy { it.dersAdi.ifBlank { "—" } }
            .map { (ad, liste) -> ad to liste.sortedBy { it.sayfa } }
            .sortedByDescending { it.second.size }

    fun tarihMetni(ms: Long): String =
        if (ms <= 0) "" else SimpleDateFormat("d MMM", Locale("tr")).format(Date(ms))

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("imler", prefs(context).getString(K_IMLER, "[]"))

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            if (o.has("imler")) {
                prefs(context).edit().putString(K_IMLER, o.optString("imler", "[]")).apply()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İmler içe aktarılamadı", e)
        }
    }
}
