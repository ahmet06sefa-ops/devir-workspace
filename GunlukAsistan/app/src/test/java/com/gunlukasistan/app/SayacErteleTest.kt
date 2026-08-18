package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * v10.2 · Öneri B26 — "Sonra hatırlat" kademe testleri.
 *
 * ── Neden test ──
 * "Yarın sabah" hesabı gün sınırında hatalı olursa alarm ya hiç
 * çalmaz ya da bir gün erken çalar. Takvim matematiği saf
 * fonksiyonda; saat dilimi cihazınki.
 */
class SayacErteleTest {

    private fun saat(gun: Int, saat: Int, dakika: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, gun)
            set(Calendar.HOUR_OF_DAY, saat)
            set(Calendar.MINUTE, dakika)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `on dakika ve bir saat basit toplama`() {
        val simdi = saat(10, 14, 30)
        assertEquals(simdi + 10 * 60_000L, SayacErtele.hedefMilis(SayacErtele.SEC_ONDK, simdi))
        assertEquals(simdi + 60 * 60_000L, SayacErtele.hedefMilis(SayacErtele.SEC_BIRSA, simdi))
    }

    @Test
    fun `sabah gecmediyse bugunun sabahi`() {
        // Gece 03:00'te "yarın sabah" → aynı gün 08:00
        val gece = saat(10, 3, 0)
        val hedef = SayacErtele.yarinSabah(gece)
        val cal = Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `sabah gectiyse yarin sabah`() {
        // Akşam 23:00'te → ertesi gün 08:00
        val aksam = saat(10, 23, 0)
        val hedef = SayacErtele.yarinSabah(aksam)
        val cal = Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(11, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `tam sekizde bile yarin sayilir`() {
        // 08:00:00 öğrendiğimizde "yarın" demeli (sabah "geçti" kabulü)
        val sekiz = saat(10, 8, 0)
        val hedef = SayacErtele.yarinSabah(sekiz)
        val cal = Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(11, cal.get(Calendar.DAY_OF_MONTH))
    }
}
