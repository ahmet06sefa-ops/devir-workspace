package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.54 — Beslenme & kalori takibi motoru.
 *
 * Günlük öğünler (kahvaltı/öğle/akşam/ara), kalori ve su takibi.
 * Veri SharedPreferences/JSON ile kalıcı tutulur → çevrimdışı çalışır.
 *
 * ── Sorumluluklar ──
 *  · Öğün ekle/sil (ad + kalori)
 *  · Günlük kalori hedefi belirle/oku
 *  · Su takibi (bardak)
 *  · Günlük özet (toplam kalori, kalan, su)
 */
object BeslenmeMotor {

    private const val TAG = "BeslenmeMotor"
    private const val PREF = "beslenme_v1"
    private const val K_OGRUNLER = "ogunler_json"
    private const val K_HEDEF = "kalori_hedefi"
    private const val K_SU = "su_bardak"
    private const val K_SU_GUN = "su_gun"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class Ogun(val ad: String, val kalori: Int, val tip: String, val tarih: Long)

    /** Öğün tipleri (sıralı gösterim için). */
    val OGRUN_TIPLERI = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")

    private fun gunAnahtari(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    // ══════════════════════════════════════════════════════════
    // Öğünler
    // ══════════════════════════════════════════════════════════

    /** Belirli bir güne ait öğünler. */
    fun ogunler(context: Context, gun: String = gunAnahtari()): List<Ogun> = runCatching {
        val ham = p(context).getString(K_OGRUNLER, null) ?: return emptyList()
        val dizi = JSONArray(ham)
        (0 until dizi.length()).mapNotNull { i ->
            val o = dizi.getJSONObject(i)
            try {
                Ogun(
                    ad = o.optString("ad"),
                    kalori = o.optInt("kalori"),
                    tip = o.optString("tip"),
                    tarih = o.optLong("tarih")
                )
            } catch (_: Exception) { null }
        }.filter { gunAnahtari(it.tarih) == gun }
    }.getOrDefault(emptyList())

    fun ogunEkle(context: Context, ad: String, kalori: Int, tip: String) {
        val liste = tumOgunler(context).toMutableList()
        liste.add(Ogun(ad, kalori, tip, System.currentTimeMillis()))
        yazOgunler(context, liste)
    }

    fun ogunSil(context: Context, index: Int, gun: String) {
        val liste = ogunler(context, gun).toMutableList()
        if (index in liste.indices) {
            val silinecek = liste.removeAt(index)
            // Tüm listeden aynı kaydı çıkar
            val tum = tumOgunler(context).toMutableList()
            tum.removeIf { it.ad == silinecek.ad && it.kalori == silinecek.kalori && it.tarih == silinecek.tarih }
            yazOgunler(context, tum)
        }
    }

    private fun tumOgunler(context: Context): List<Ogun> = runCatching {
        val ham = p(context).getString(K_OGRUNLER, null) ?: return emptyList()
        val dizi = JSONArray(ham)
        (0 until dizi.length()).mapNotNull { i ->
            val o = dizi.getJSONObject(i)
            try {
                Ogun(o.optString("ad"), o.optInt("kalori"), o.optString("tip"), o.optLong("tarih"))
            } catch (_: Exception) { null }
        }
    }.getOrDefault(emptyList())

    private fun yazOgunler(context: Context, liste: List<Ogun>) {
        val dizi = JSONArray()
        liste.forEach { o ->
            dizi.put(JSONObject().apply {
                put("ad", o.ad); put("kalori", o.kalori); put("tip", o.tip); put("tarih", o.tarih)
            })
        }
        p(context).edit().putString(K_OGRUNLER, dizi.toString()).apply()
    }

    /** Bugünkü toplam kalori. */
    fun bugunKalori(context: Context): Int =
        ogunler(context).sumOf { it.kalori }

    // ══════════════════════════════════════════════════════════
    // Kalori hedefi
    // ══════════════════════════════════════════════════════════

    fun kaloriHedefi(context: Context): Int = p(context).getInt(K_HEDEF, 2000)

    /** Hedefi geçerli aralığa çeker (saf — test edilebilir). */
    fun kaloriHedefiDuzelt(hedef: Int): Int = hedef.coerceIn(500, 10000)

    fun kaloriHedefiAyarla(context: Context, hedef: Int) =
        p(context).edit().putInt(K_HEDEF, kaloriHedefiDuzelt(hedef)).apply()

    /** Bugünkü kalan kalori (hedef - alınan). */
    fun bugunKalan(context: Context): Int = kaloriHedefi(context) - bugunKalori(context)

    // ══════════════════════════════════════════════════════════
    // Su takibi
    // ══════════════════════════════════════════════════════════

    fun suBardak(context: Context): Int {
        if (p(context).getString(K_SU_GUN, null) != gunAnahtari()) {
            p(context).edit().putString(K_SU_GUN, gunAnahtari()).putInt(K_SU, 0).apply()
            return 0
        }
        return p(context).getInt(K_SU, 0)
    }

    fun suEkle(context: Context, adet: Int = 1) {
        val simdi = suBardak(context)
        p(context).edit().putString(K_SU_GUN, gunAnahtari()).putInt(K_SU, (simdi + adet).coerceAtLeast(0)).apply()
    }
}
