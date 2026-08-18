package com.gunlukasistan.app

/**
 * v10.45/v10.46 — Kullanıcı maddesi #7 & #8: PiP mini mod kararları ve canlı kontroller (saf, JVM testli).
 * Sistem en-boy aralığı: 0,4184..2,39 — dışarı taşan oran kenarlara kenetlenir.
 */
object MiniMod {

    /** Ekran ölçüsünden güvenli PiP en-boy oranı (pay, payda). */
    fun pipOrani(g: Int, y: Int): Pair<Int, Int> {
        if (g <= 0 || y <= 0) return 3 to 4
        val oran = g.toDouble() / y
        return when {
            oran < 0.42 -> 3 to 4
            oran > 2.39 -> 13 to 6
            y >= g -> 3 to 4      // dikey telefon: uzun mini pencere
            else -> 16 to 9       // yatay/tablet
        }
    }

    /** PiP penceresinde gösterilecek eylem kodları listesi (101=Bekle/Devam, 102=Sıfırla, 103=+5dk). */
    fun aksiyonKodlari(calisiyorMu: Boolean, geriSayimMi: Boolean): List<Int> {
        val list = mutableListOf(101, 102)
        if (geriSayimMi) {
            list.add(103)
        }
        return list
    }

    /** PiP modundayken kadranın pencereyi ferahça doldurması için ölçek katsayısı. */
    fun pipOlcegi(pip: Boolean): Float {
        return if (pip) 1.15f else 1.0f
    }

    /** PiP modundayken dış boşlukların sıfırlanıp sıfırlanmayacağı kararı (dp). */
    fun pipDolguDp(pip: Boolean): Int {
        return if (pip) 4 else 24
    }
}
