package com.gunlukasistan.app

import java.util.Calendar

/**
 * v10.13 · ULTRA-30 / B8 — Ay takvimi widget'ının saf hesapları.
 *
 * 42 hücre (6 hafta × 7 gün) haftanın ilk günü **Pazartesi** olacak
 * şekilde üretilir (HaftaPlan.gunSirasi ile aynı Türkiye geleneği).
 * Yoğunluk seviyesi: bekleyen görev sayısına göre 0..3 nokta.
 */
object TakvimMotoru {

    /** Bir ızgara hücresi. */
    data class Hucre(
        val gun: Int,          // ayın kaçı (1..31; ay dışı günlerde de dolu)
        val ayDisi: Boolean,   // önceki/sonraki aydan mı
        val bugunMu: Boolean,
        val yil: Int,
        val ay0: Int           // 0-tabanlı ay
    )

    /**
     * [yil]/[ay0] için 42 hücre üretir.
     * Ayın 1'i haftanın hangi gününe denk gelirse gelsin ızgara Pazartesi
     * başlar; taşan günler komşu aydan tamamlanır.
     */
    fun hucreler(yil: Int, ay0: Int, simdiMs: Long): List<Hucre> {
        val bugun = Calendar.getInstance().apply { timeInMillis = simdiMs }
        val ilk = Calendar.getInstance().apply {
            clear()
            set(yil, ay0, 1)
        }
        // Calendar.DAY_OF_WEEK: 1=Pazar ... 2=Pazartesi → Pazartesi=0 öteleme
        val haftaGunu = ilk.get(Calendar.DAY_OF_WEEK)
        val kaydir = (haftaGunu + 5) % 7
        ilk.add(Calendar.DAY_OF_YEAR, -kaydir)

        return (0 until 42).map { i ->
            val gun = Calendar.getInstance().apply {
                timeInMillis = ilk.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            Hucre(
                gun = gun.get(Calendar.DAY_OF_MONTH),
                ayDisi = gun.get(Calendar.MONTH) != ay0 || gun.get(Calendar.YEAR) != yil,
                bugunMu = gun.get(Calendar.YEAR) == bugun.get(Calendar.YEAR) &&
                    gun.get(Calendar.DAY_OF_YEAR) == bugun.get(Calendar.DAY_OF_YEAR),
                yil = gun.get(Calendar.YEAR),
                ay0 = gun.get(Calendar.MONTH)
            )
        }
    }

    /**
     * Yoğunluk seviyesi (0..3 nokta).
     * 0 görev → yok · 1-2 → bir · 3-4 → iki · 5+ → üç nokta.
     */
    fun yogunluk(adet: Int): Int = when {
        adet <= 0 -> 0
        adet <= 2 -> 1
        adet <= 4 -> 2
        else -> 3
    }

    /** Ay kaydırma düğmeleri için yıl/ay aritmetiği (0-tabanlı). */
    fun ayKaydir(yil: Int, ay0: Int, ofset: Int): Pair<Int, Int> {
        val toplam = yil * 12 + ay0 + ofset
        val yeniYil = Math.floorDiv(toplam, 12)
        val yeniAy = Math.floorMod(toplam, 12)
        return yeniYil to yeniAy
    }

    /** Sınır: ileri/geri en çok 12 ay (hataya açık sonsuz gezinti yok). */
    fun ofsetKelepce(ofset: Int): Int = ofset.coerceIn(-12, 12)
}
