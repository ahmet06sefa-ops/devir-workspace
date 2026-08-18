package com.gunlukasistan.app

/**
 * v10.13 · ULTRA-30 / B9 — Uyku widget'ının saf veri hazırlığı.
 *
 * v10.9 defteri (`UykuCerceve.defter`) son 7 geceyi tutuyor ama hiç
 * görselleştirilmemişti. Bu nesne çubuk grafiğin sayısal kısmını
 * üretir: 7 günlük liste (eksik günler 0), plan çizgisi ve ölçek.
 */
object UykuPano {

    /**
     * Planlanan gece süresi (ms): akşam hedefinden sabah hedefine.
     * Gece yarısı aşımı (23:00 → 07:00) 1440 dk eklenerek çözülür.
     */
    fun planMs(sabahDk: Int, aksamDk: Int): Long {
        val fark = (sabahDk - aksamDk + 1440) % 1440
        return fark * 60_000L
    }

    /**
     * Son 7 geceyi döndürür: eksik kayıtlar 0 ms ile doldurulur,
     * sıralama eski → yeni (grafik soldan sağa akar).
     */
    fun son7(geceler: List<Long>): List<Long> {
        val sonDolu = geceler.takeLast(7)
        val eksik = 7 - sonDolu.size
        return List(eksik) { 0L } + sonDolu
    }

    /**
     * Çubuk ölçeği için tavan: en uzun gece ile plan arasından büyüğü;
     * ikisi de yoksa 8 saatlik makul gövde.
     */
    fun maksMs(planMsDeger: Long, geceler: List<Long>): Long {
        val enUzun = (geceler.maxOrNull() ?: 0L)
        return maxOf(enUzun, planMsDeger, 8L * 3_600_000L)
    }

    /** Çubuk oranı (0..1) — 0 geceler görünmez kalır, taşmalar kırpılır. */
    fun oran(uykuMs: Long, maksMsDeger: Long): Float {
        if (maksMsDeger <= 0L) return 0f
        return (uykuMs.toFloat() / maksMsDeger).coerceIn(0f, 1f)
    }
}
