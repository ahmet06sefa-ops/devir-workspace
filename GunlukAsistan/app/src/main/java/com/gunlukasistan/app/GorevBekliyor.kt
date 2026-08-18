package com.gunlukasistan.app

import android.content.Context

/**
 * v10.36 · Katalog #16 — "Bekliyor" durumu: başkasından dönüş beklenen
 * görevler ⏳ rozetli görünür. Görev kimlikleri ayrı tercihte tutulur
 * (`gorev_bekliyor_v1` — model/JSON'a dokunulmaz, yedek doğal kapsar).
 */
object GorevBekliyor {

    // ──────────────── saf (JVM testli) ────────────────

    /** Rozet öneki: bekleyen görevin metni "⏳ " ile başlar (StringBuilder'sız). */
    fun rozetliMetin(metin: String, bekliyorMu: Boolean): String =
        if (bekliyorMu) "⏳ " + metin else metin

    // ──────────────── depo ────────────────

    private const val PREF = "gorev_bekliyor_v1"
    private const val K_KUME = "kume"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kume(ctx: Context): Set<Long> =
        runCatching { NotKilit.kumeJsondan(pref(ctx).getString(K_KUME, "[]")) }
            .getOrDefault(emptySet())

    fun bekliyorMu(ctx: Context, gorevId: Long): Boolean = kume(ctx).contains(gorevId)

    fun yaz(ctx: Context, gorevId: Long, bekliyor: Boolean) {
        val k = kume(ctx).toMutableSet()
        if (bekliyor) k.add(gorevId) else k.remove(gorevId)
        pref(ctx).edit().putString(K_KUME, NotKilit.kumeJsonaYaz(k)).apply()
    }
}
