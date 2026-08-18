package com.gunlukasistan.app

import android.content.Context

/**
 * v10.32 · Katalog #23 — not renk etiketi (5 pastel ton + renge göre filtre).
 *
 * Renkler Note/Room modeline DOKUNMADAN ayrı tercihte yaşar (v10.27
 * NotSabitle ile aynı mimari karar — migrasyon yok). `not_renk_v1` dosyası
 * PrefYedek taramasına doğal olarak girer. Saf JSON köprüsü JVM testli.
 */
object NotRenk {

    data class Ton(val ad: String, val argb: Int)

    /** 5 pastel ton (Keep esintili); haritada DİZİN saklanır. */
    val TONLAR = listOf(
        Ton("Mercan", 0xFFF28B82.toInt()),
        Ton("Güneş", 0xFFFFF475.toInt()),
        Ton("Fıstık", 0xFFCCFF90.toInt()),
        Ton("Deniz", 0xFFA7FFEB.toInt()),
        Ton("Lavanta", 0xFFD7AEFB.toInt())
    )

    fun tonGuvenli(indeks: Int): Ton? = TONLAR.getOrNull(indeks)

    // ──────────────── saf JSON köprüsü (android YOK, JVM testli) ────────────────

    /** {"12":3,"45":0} okur; bozuk alanlar ve tanımsız tonlar atlanır. */
    fun jsondanOku(json: String?): Map<Long, Int> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val rx = Regex("\"(\\d+)\"\\s*:\\s*(\\d+)")
        val out = LinkedHashMap<Long, Int>()
        for (m in rx.findAll(json)) {
            val id = m.groupValues[1].toLongOrNull() ?: continue
            val t = m.groupValues[2].toIntOrNull() ?: continue
            if (tonGuvenli(t) != null) out[id] = t
        }
        return out
    }

    fun jsonaYaz(harita: Map<Long, Int>): String {
        if (harita.isEmpty()) return "{}"
        return harita.entries.joinToString(separator = ",", prefix = "{", postfix = "}") {
            "\"" + it.key.toString() + "\":" + it.value.toString()
        }
    }

    // ──────────────── depo (SharedPreferences) ────────────────

    private const val PREF = "not_renk_v1"
    private const val K_HARITA = "harita"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun harita(ctx: Context): Map<Long, Int> =
        runCatching { jsondanOku(pref(ctx).getString(K_HARITA, "{}")) }.getOrDefault(emptyMap())

    /** ton=null → rengi kaldırır; tanımsız ton dizini yoksayılır. */
    fun yaz(ctx: Context, notId: Long, ton: Int?) {
        val h = LinkedHashMap(harita(ctx))
        if (ton == null) h.remove(notId) else if (tonGuvenli(ton) != null) h[notId] = ton
        pref(ctx).edit().putString(K_HARITA, jsonaYaz(h)).apply()
    }

    fun ton(ctx: Context, notId: Long): Int? = harita(ctx)[notId]

    fun tonRenk(ton: Int): Int = TONLAR[ton].argb

    /** Listede fiilen kullanılan tonlar (sıralı, benzersiz) — filtre şeridi için. */
    fun kullanilanTonlar(ctx: Context): List<Int> = harita(ctx).values.distinct().sorted()
}
