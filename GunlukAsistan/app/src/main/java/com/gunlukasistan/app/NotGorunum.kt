package com.gunlukasistan.app

import android.content.Context

/**
 * v10.35 · Katalog #34 — not listesi görünüm tercihi (kompakt ⇄ kart).
 * Tek bayraklı tercih; `not_gorunum_v1` yedek kapsamına doğal girer.
 */
object NotGorunum {

    private const val PREF = "not_gorunum_v1"
    private const val K_KOMPAKT = "kompakt"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kompaktMi(ctx: Context): Boolean = pref(ctx).getBoolean(K_KOMPAKT, false)

    fun yaz(ctx: Context, kompakt: Boolean) {
        pref(ctx).edit().putBoolean(K_KOMPAKT, kompakt).apply()
    }

    fun degistir(ctx: Context): Boolean {
        val yeni = !kompaktMi(ctx)
        yaz(ctx, yeni)
        return yeni
    }
}
