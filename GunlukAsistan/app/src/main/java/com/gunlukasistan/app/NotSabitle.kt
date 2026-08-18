package com.gunlukasistan.app

import android.content.Context

/**
 * v10.27 (öneri #22) — Not sabitleme (pin).
 *
 * ── Mimari karar ──
 * `Store.Note` modeline ve Room şemasına DOKUNULMADI: alan eklemek
 * Room migrasyonu gerektirirdi ve risk/geçiştir bölgesi değildi.
 * Sabitlenen not kimlikleri ayrı bir tercih kümesinde tutulur
 * (`not_sabitle_v1`). Model değişmediği için Room, yedek ve eski
 * sürüm uyumu aynen korunur; küme v10.25 yedek taramasıyla zaten
 * yedeğe giriyor.
 *
 * ── Sıralama ──
 * Yeniden eskiye sıralı gelen liste [sabitOnce] ile sabitliler başa
 * alınır; kararlı sıralama sayesinde kalanların düzeni bozulmaz.
 */
object NotSabitle {

    private const val PREF = "not_sabitle_v1"
    private const val K_KUME = "kume"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /**
     * Sabitliler başa gelecek şekilde sıralar (saf — birim testli).
     * `sortedByDescending` kararlıdır: pin olmayanlar girdi sırasını
     * (yeniden eskiye) korur.
     */
    fun <T> sabitOnce(liste: List<T>, pinler: Set<Long>, id: (T) -> Long): List<T> =
        liste.sortedByDescending { pinler.contains(id(it)) }

    fun pinler(c: Context): Set<Long> = prefs(c)
        .getStringSet(K_KUME, emptySet()).orEmpty()
        .mapNotNull { it.toLongOrNull() }.toSet()

    fun sabitMi(c: Context, id: Long): Boolean = pinler(c).contains(id)

    /**
     * Sabitle/kaldır.
     * @return yeni durum (true = artık sabit)
     */
    fun degistir(c: Context, id: Long): Boolean {
        val mevcut = pinler(c).toMutableSet()
        val sabit = !mevcut.remove(id)
        if (sabit) mevcut.add(id)
        prefs(c).edit()
            .putStringSet(K_KUME, mevcut.mapTo(mutableSetOf()) { it.toString() })
            .apply()
        return sabit
    }
}
