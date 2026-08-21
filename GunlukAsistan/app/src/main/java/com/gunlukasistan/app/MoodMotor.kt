package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.57 — Ruh hali (mood) takibi motoru.
 *
 * Kullanıcının günlük ruh halini (1-5) ve notu kaydeder; son 7 gün
 * ortalamasını hesaplar. Veri SharedPreferences/JSON ile kalıcıdır.
 *
 * ── Sorumluluklar ──
 *  · Mood kaydı ekle/sil (puan 1-5 + opsiyonel not)
 *  · Son 7 gün ortalama puan ve kayıt sayısı
 *  · Puan → emoji/etiket çevirisi
 */
object MoodMotor {

    private const val TAG = "MoodMotor"
    private const val PREF = "mood_v1"
    private const val K_KAYITLAR = "mood_kayitlari_json"
    private const val TAVAN = 2000

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class MoodKaydi(val puan: Int, val not: String, val tarih: Long)

    fun kayitlar(context: Context): List<MoodKaydi> = runCatching {
        val ham = p(context).getString(K_KAYITLAR, null) ?: return emptyList()
        val dizi = JSONArray(ham)
        (0 until dizi.length()).mapNotNull { i ->
            val o = dizi.getJSONObject(i)
            try {
                MoodKaydi(
                    puan = o.optInt("puan"),
                    not = o.optString("not"),
                    tarih = o.optLong("tarih")
                )
            } catch (_: Exception) { null }
        }.sortedByDescending { it.tarih }
    }.getOrDefault(emptyList())

    fun ekle(context: Context, puan: Int, not: String) {
        val liste = kayitlar(context).toMutableList()
        liste.add(0, MoodKaydi(puan.coerceIn(1, 5), not.trim(), System.currentTimeMillis()))
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

    private fun yaz(context: Context, liste: List<MoodKaydi>) {
        val dizi = JSONArray()
        liste.forEach { k ->
            dizi.put(JSONObject().apply {
                put("puan", k.puan); put("not", k.not); put("tarih", k.tarih)
            })
        }
        p(context).edit().putString(K_KAYITLAR, dizi.toString()).apply()
    }

    /** Son 7 gündeki ortalama puan (0 ise kayıt yok). */
    fun son7GunOrtalama(context: Context): Double {
        val esik = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val son = kayitlar(context).filter { it.tarih >= esik }
        if (son.isEmpty()) return 0.0
        return son.map { it.puan }.average()
    }

    fun son7GunKayitSayisi(context: Context): Int {
        val esik = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return kayitlar(context).count { it.tarih >= esik }
    }

    /** Puan → emoji (saf). */
    fun emoji(puan: Int): String = when (puan.coerceIn(1, 5)) {
        5 -> "😄"
        4 -> "🙂"
        3 -> "😐"
        2 -> "😟"
        else -> "😞"
    }

    /** Puan → etiket (saf). */
    fun etiket(puan: Int): String = when (puan.coerceIn(1, 5)) {
        5 -> "Çok iyi"
        4 -> "İyi"
        3 -> "Normal"
        2 -> "Kötü"
        else -> "Çok kötü"
    }

    fun gunAnahtari(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}
