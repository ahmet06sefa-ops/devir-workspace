package com.gunlukasistan.app

import android.content.Context

/**
 * v10.38 · Katalog #13 — "bugün bitecek" görevleri listenin en üstüne
 * sabitleme seçeneği.
 *
 * Seçenek açıkken sıralama karşılaştırıcısına ek öncelik katmanı
 * eklenir: vadesi bugün dolan VE vadesi geçmiş görevler (henüz
 * tamamlanmamış olanlar üstte kalacak şekilde mevcut done katmanının
 * hemen altında) en üste taşınır; başlık altındaki şerit kaç görevin
 * bu bölümde olduğunu gösterir.
 */
object GorevBugunUstte {

    private const val PREF = "gorev_bugun_ustte_v1"
    private const val K_ACIK = "acik"

    private fun pref(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun acikMi(c: Context): Boolean = pref(c).getBoolean(K_ACIK, false)

    /** Durumu tersine çevirir ve yeni değeri döndürür. */
    fun acKapa(c: Context): Boolean {
        val yeni = !acikMi(c)
        pref(c).edit().putBoolean(K_ACIK, yeni).apply()
        return yeni
    }

    /**
     * Bugünün [başlangıç, bitiş) aralığı (ms, yerel saat). Saf.
     * Gece yarısı başlar, ertesi gece yarısında biter.
     */
    fun bugunAraligi(simdi: Long): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = simdi
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val baslangic = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return baslangic to cal.timeInMillis
    }

    /**
     * Sıralama önceliği. Saf.
     * 0 = bugün bitecek veya vadesi geçmiş (üstte), 1 = diğerleri.
     * Tarihsiz görev (dueAt = 0) asla üstlere taşınmaz.
     */
    fun oncelik(dueAt: Long, bugunBitis: Long): Int =
        if (dueAt > 0L && dueAt < bugunBitis) 0 else 1
}
