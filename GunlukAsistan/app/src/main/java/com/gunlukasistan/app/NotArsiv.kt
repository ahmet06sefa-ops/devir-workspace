package com.gunlukasistan.app

import android.content.Context

/**
 * v10.35 · Katalog #37 — not arşivi: silmeden gizleme.
 * Arşivli not kimlikleri ayrı tercihte (`not_arsiv_v1`, yedek kapsamına
 * doğal girer); not içeriğine dokunulmaz. Görev arşivi (v7.75) ile aynı
 * ilke: gizlenir ama silinmez, geri çıkarılabilir.
 */
object NotArsiv {

    // saf JSON köprüsü — NotKilit ile aynı güvenli biçim (JVM testli)
    fun kumeJsondan(json: String?): Set<Long> = NotKilit.kumeJsondan(json)
    fun kumeJsonaYaz(kume: Set<Long>): String = NotKilit.kumeJsonaYaz(kume)

    private const val PREF = "not_arsiv_v1"
    private const val K_KUME = "arsivli"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kume(ctx: Context): Set<Long> =
        runCatching { kumeJsondan(pref(ctx).getString(K_KUME, "[]")) }.getOrDefault(emptySet())

    fun arsivliMi(ctx: Context, notId: Long): Boolean = kume(ctx).contains(notId)

    fun yaz(ctx: Context, notId: Long, arsivli: Boolean) {
        val k = kume(ctx).toMutableSet()
        if (arsivli) k.add(notId) else k.remove(notId)
        pref(ctx).edit().putString(K_KUME, kumeJsonaYaz(k)).apply()
    }

    fun sayi(ctx: Context): Int = kume(ctx).size
}
