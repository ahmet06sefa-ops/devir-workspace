package com.gunlukasistan.app

import android.content.Context

/**
 * v10.36 · Katalog #19 — görev listesi yoğunluk tercihi (kompakt ⇄ kart).
 * NotGorunum (v10.35) ile aynı desen; `gorev_gorunum_v1` yedekte.
 */
object GorevGorunum {

    private const val PREF = "gorev_gorunum_v1"
    private const val K_KOMPAKT = "kompakt"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kompaktMi(ctx: Context): Boolean =
        pref(ctx).getBoolean(K_KOMPAKT, false) || GorunumAyar.kartModu(ctx) >= 1

    fun degistir(ctx: Context): Boolean {
        val yeni = !kompaktMi(ctx)
        pref(ctx).edit().putBoolean(K_KOMPAKT, yeni).apply()
        return yeni
    }
}
