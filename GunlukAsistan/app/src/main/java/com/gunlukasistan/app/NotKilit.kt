package com.gunlukasistan.app

import android.content.Context

/**
 * v10.34 · Katalog #26 — tek notu uygulama PIN'i arkasına alma.
 *
 * Kilitli not kimlikleri ayrı tercihte tutulur (`not_kilit_v1`, yedek
 * kapsamına doğal girer); notun İÇERİĞİNE dokunulmaz. Doğrulama mevcut
 * uygulama kilidi altyapısıyla yapılır ([KilitDepo] + [KilitMantik]
 * kaba-kuvvet koruması paylaşılır — aynı kilitleme havuzu).
 */
object NotKilit {

    // ──────────────── saf JSON köprüsü (android YOK, JVM testli) ────────────────

    /** "[12,45]" okur; bozuk girdide bulunan sayılar toparlanır. */
    fun kumeJsondan(json: String?): Set<Long> {
        if (json.isNullOrBlank() || json == "[]") return emptySet()
        return Regex("\\d+").findAll(json).mapNotNull { it.value.toLongOrNull() }.toSet()
    }

    fun kumeJsonaYaz(kume: Set<Long>): String =
        if (kume.isEmpty()) "[]" else kume.sorted().joinToString(separator = ",", prefix = "[", postfix = "]")

    // ──────────────── depo (SharedPreferences) ────────────────

    private const val PREF = "not_kilit_v1"
    private const val K_KUME = "kilitli"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kume(ctx: Context): Set<Long> =
        runCatching { kumeJsondan(pref(ctx).getString(K_KUME, "[]")) }.getOrDefault(emptySet())

    fun kilitliMi(ctx: Context, notId: Long): Boolean = kume(ctx).contains(notId)

    fun yaz(ctx: Context, notId: Long, kilitli: Boolean) {
        val k = kume(ctx).toMutableSet()
        if (kilitli) k.add(notId) else k.remove(notId)
        pref(ctx).edit().putString(K_KUME, kumeJsonaYaz(k)).apply()
    }
}
