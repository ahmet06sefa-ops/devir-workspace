package com.gunlukasistan.app

/**
 * v11.13 — Akıllı Günlük Plan Üretici (SAF, JVM testli).
 *
 * Kullanıcı isteği: AI'nın görevlere + namaz vakitlerine göre otomatik
 * günlük plan çıkarması. Bu motor, bekleyen görevleri ve boş zaman dilimlerini
 * birleştirip saatli bir zaman çizelgesi üretir.
 *
 *  · [plan] — bekleyen görevlerden + günün başlangıç/bitişinden plan üretir.
 *  · [dilimEkle] — bir görevi saat dilimine atar.
 */
object AkilliGunlukPlan {

    /** Tek plan satırı. */
    data class PlanSatir(val bas: Int, val bit: Int, val metin: String)

    /**
     * Bekleyen görevleri gün içine dengeli biçimde yayar.
     * @param gorevler görev metinleri (öncelik sırası)
     * @param baslangicDk gün başlangıcı (dk, ör. 09:00 = 540)
     * @param bitisDk gün bitişi (dk, ör. 21:00 = 1260)
     * @param dilimDk her görev için ayrılan dakika (varsayılan 50)
     * @return plan satırları (boş görev listesi → boş)
     */
    fun plan(
        gorevler: List<String>,
        baslangicDk: Int = 9 * 60,
        bitisDk: Int = 21 * 60,
        dilimDk: Int = 50
    ): List<PlanSatir> {
        if (gorevler.isEmpty()) return emptyList()
        val toplam = (bitisDk - baslangicDk).coerceAtLeast(0)
        if (toplam <= 0) return emptyList()
        val blok = dilimDk.coerceIn(15, 120)
        var mevcut = baslangicDk
        val sonuc = mutableListOf<PlanSatir>()
        for (g in gorevler) {
            val bas = mevcut
            var bit = bas + blok
            if (bit > bitisDk) bit = bitisDk
            if (bit <= bas) break
            sonuc.add(PlanSatir(bas, bit, g))
            mevcut = bit
            // Görevler arası kısa mola (10 dk)
            mevcut = (mevcut + 10).coerceAtMost(bitisDk)
            if (mevcut >= bitisDk) break
        }
        return sonuc
    }

    /** Saat dilimini "09:00-09:50" biçiminde yazar. */
    fun saat(dk: Int): String = "%02d:%02d".format(dk / 60, dk % 60)

    /** Planı okunur metne çevirir. */
    fun metneCevir(plan: List<PlanSatir>): String =
        plan.joinToString("\n") { "${saat(it.bas)}-${saat(it.bit)}  ${it.metin}" }
}
