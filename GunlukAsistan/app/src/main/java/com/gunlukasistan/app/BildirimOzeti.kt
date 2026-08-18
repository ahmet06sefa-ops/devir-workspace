package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v10.4 · Öneri B18 — Sessizken birikenler için sabah özeti.
 *
 * ── Sorun ──
 * Sessiz saat ve günlük tavan (`BildirimMerkezi.gonder`) bildirimi
 * **yutuyordu**: kullanıcı gece kart tekrarını, gün ortasında tavana
 * takılan öneriyi hiç öğrenmiyordu. "Kaçırdıklarım" hiçbir yerde
 * birikmediği için bilgi kayboluyordu.
 *
 * ── Tasarım ──
 * Yutulan bildirimin **başlığı** deftere düşer (gövde değil).
 * Sabah turu (`BildirimUretici`) önce bu özeti tek bildirim olarak
 * sunar, sonra günün turuna geçer.
 *
 * Defter gün-bağımsızdır — sessiz saat gece yarısını geçiyor
 * (23:00→08:00), yutulanların çoğu "dün" damgalıdır; güne bağlı
 * defter tam da ana senaryoyu kaybederdi. Bunun yerine kayıtlar
 * 36 saatten eskiyse kendiliğinden düşer.
 *
 * Özet `gonder()` üzerinden çıktığı için kendisi de yutulabilir;
 * `Tur.OZET` biriktirme istisnasıyla sonsuz döngü olmaz.
 *
 * ── Saf bölge ──
 * [satirlar] saf; birim testli.
 */
object BildirimOzeti {

    private const val PREF = "bildirim_ozeti_v1"
    private const val K_LISTE = "liste"
    private const val TAVAN = 20
    private const val GOSTERILEN = 5
    private const val AZAMI_YAS_MS = 36 * 3_600_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Yutulan bildirimin başlığını deftere yazar. */
    fun biriktir(context: Context, baslik: String): Boolean {
        if (baslik.isBlank()) return false
        return try {
            val p = prefs(context)
            val simdi = System.currentTimeMillis()
            val yeni = JSONArray()
            // Yaşı geçenleri ve aynı başlıkları ele (başlık tek kalsın)
            val eski = JSONArray(p.getString(K_LISTE, "[]") ?: "[]")
            for (i in 0 until eski.length()) {
                val o = eski.optJSONObject(i) ?: continue
                if (o.optString("b") == baslik) continue
                if (simdi - o.optLong("z", 0L) > AZAMI_YAS_MS) continue
                yeni.put(o)
            }
            yeni.put(JSONObject().put("b", baslik).put("z", simdi))
            // Taşarsa en eskiden kırp
            val son = JSONArray()
            val bas = (yeni.length() - TAVAN).coerceAtLeast(0)
            for (i in bas until yeni.length()) son.put(yeni.get(i))
            p.edit().putString(K_LISTE, son.toString()).apply()
            true
        } catch (_: Exception) { false }
    }

    /** Defterdeki güncel başlıklar (36 saatten eski olmayan). */
    fun liste(context: Context, simdi: Long = System.currentTimeMillis()): List<String> =
        try {
            val dizi = JSONArray(prefs(context).getString(K_LISTE, "[]") ?: "[]")
            (0 until dizi.length())
                .mapNotNull { dizi.optJSONObject(it) }
                .filter { simdi - it.optLong("z", 0L) <= AZAMI_YAS_MS }
                .map { it.optString("b") }
                .filter { it.isNotBlank() }
        } catch (_: Exception) { emptyList() }

    /** Defteri kapat — özet gönderildikten sonra çağrılır. */
    fun temizle(context: Context) {
        prefs(context).edit().remove(K_LISTE).apply()
    }

    /** Gösterilecek satırlar: ilk [GOSTERILEN] açıkça, gerisi "+N daha". */
    fun satirlar(basliklar: List<String>): List<String> {
        if (basliklar.size <= GOSTERILEN) return basliklar.map { "• $it" }
        val ana = basliklar.take(GOSTERILEN).map { "• $it" }.toMutableList()
        ana.add("… +${basliklar.size - GOSTERILEN} daha")
        return ana
    }

    /** Özet bildiriminin geniş metni. Boş listeye çağrılmamalı. */
    fun ozetMetni(context: Context, basliklar: List<String>): String {
        val satir = satirlar(basliklar).joinToString("\n")
        return context.getString(R.string.bo_metin, basliklar.size) + "\n" + satir
    }
}
