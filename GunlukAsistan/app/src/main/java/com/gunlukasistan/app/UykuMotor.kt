package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.55 — Uyku takibi motoru.
 *
 * Uyku kayıtları: yatış saati, kalkış saati, süre (saat) ve kalite (1-5).
 * Veri SharedPreferences/JSON ile kalıcıdır → çevrimdışı.
 *
 * ── Sorumluluklar ──
 *  · Uyku kaydı ekle/sil
 *  · Son 7 gün özeti (ortalama süre, kalite)
 *  · Bugünkü uyku durumu
 */
object UykuMotor {

    private const val TAG = "UykuMotor"
    private const val PREF = "uyku_v1"
    private const val K_KAYITLAR = "uyku_kayitlari_json"
    private const val TAVAN = 1000

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class UykuKaydi(
        val tarih: Long,          // hangi gece (yatış anı)
        val sureDakika: Int,      // dakika cinsinden süre
        val kalite: Int           // 1-5
    )

    fun kayitlar(context: Context): List<UykuKaydi> = runCatching {
        val ham = p(context).getString(K_KAYITLAR, null) ?: return emptyList()
        val dizi = JSONArray(ham)
        (0 until dizi.length()).mapNotNull { i ->
            val o = dizi.getJSONObject(i)
            try {
                UykuKaydi(
                    tarih = o.optLong("tarih"),
                    sureDakika = o.optInt("sure"),
                    kalite = o.optInt("kalite")
                )
            } catch (_: Exception) { null }
        }.sortedByDescending { it.tarih }
    }.getOrDefault(emptyList())

    fun ekle(context: Context, sureDakika: Int, kalite: Int) {
        val liste = kayitlar(context).toMutableList()
        liste.add(0, UykuKaydi(System.currentTimeMillis(), sureDakika, kalite.coerceIn(1, 5)))
        if (liste.size > TAVAN) liste.removeAt(liste.size - 1)
        yaz(context, liste)
    }

    fun sil(context: Context, index: Int) {
        val liste = kayitlar(context).toMutableList()
        if (index in liste.indices) {
            liste.removeAt(index)
            yaz(context, liste)
        }
    }

    private fun yaz(context: Context, liste: List<UykuKaydi>) {
        val dizi = JSONArray()
        liste.forEach { k ->
            dizi.put(JSONObject().apply {
                put("tarih", k.tarih); put("sure", k.sureDakika); put("kalite", k.kalite)
            })
        }
        p(context).edit().putString(K_KAYITLAR, dizi.toString()).apply()
    }

    /** Son 7 günde kaç gece uyku kaydedildi. */
    fun son7GunKayitSayisi(context: Context): Int {
        val esik = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return kayitlar(context).count { it.tarih >= esik }
    }

    /** Son 7 gündeki ortalama uyku süresi (dakika). */
    fun son7GunOrtalamaDakika(context: Context): Double {
        val esik = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val son = kayitlar(context).filter { it.tarih >= esik }
        if (son.isEmpty()) return 0.0
        return son.map { it.sureDakika }.average()
    }

    /** Son 7 gündeki ortalama uyku kalitesi (1-5). */
    fun son7GunOrtalamaKalite(context: Context): Double {
        val esik = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val son = kayitlar(context).filter { it.tarih >= esik }
        if (son.isEmpty()) return 0.0
        return son.map { it.kalite }.average()
    }

    /** Süreyi "7 sa 30 dk" biçimine çevirir (saf — test edilebilir). */
    fun sureMetni(dakika: Int): String {
        val h = dakika / 60
        val m = dakika % 60
        return if (h > 0 && m > 0) "${h} sa ${m} dk"
        else if (h > 0) "${h} sa"
        else "$m dk"
    }

    /** Kalite etiketi (saf). */
    fun kaliteEtiketi(kalite: Int): String = when (kalite) {
        5 -> "Mükemmel"
        4 -> "İyi"
        3 -> "Normal"
        2 -> "Kötü"
        else -> "Çok kötü"
    }

    fun gunAnahtari(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}
