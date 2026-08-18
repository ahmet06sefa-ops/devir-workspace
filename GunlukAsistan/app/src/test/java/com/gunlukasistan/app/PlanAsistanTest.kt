package com.gunlukasistan.app

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.TimeZone

/** v10.42 · Kullanıcı maddeleri #5/#6 — [PlanAsistan] saf testler. */
class PlanAsistanTest {

    companion object {
        private lateinit var eski: TimeZone

        @BeforeClass
        @JvmStatic
        fun tzKur() {
            eski = TimeZone.getDefault()
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        }

        @AfterClass
        @JvmStatic
        fun tzBirak() {
            TimeZone.setDefault(eski)
        }
    }

    @Test
    fun `dakika yaz`() {
        assertEquals("22:00", PlanAsistan.dakikaYaz(1320))
        assertEquals("06:05", PlanAsistan.dakikaYaz(365))
    }

    @Test
    fun `sabah penceresi gece elenir`() {
        assertFalse(PlanAsistan.sabahPenceresiMi(180))
        assertTrue(PlanAsistan.sabahPenceresiMi(240))
        assertTrue(PlanAsistan.sabahPenceresiMi(1400))
    }

    @Test
    fun `sabah ozet duzeni`() {
        assertEquals("5 görev bekliyor · bugün 2 · gecikmiş 1", PlanAsistan.sabahOzet(5, 2, 1))
        assertEquals("3 görev bekliyor", PlanAsistan.sabahOzet(3, 0, 0))
    }

    @Test
    fun `sonraki aksam hedef gecmediyse bugun`() {
        val simdi = java.util.Calendar.getInstance().apply {
            set(2026, 7, 9, 21, 30, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val hedef = PlanAsistan.sonrakiAksam(simdi, 22 * 60)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(9, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(22, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `sonraki aksam hedef gectiyse yarin`() {
        val simdi = java.util.Calendar.getInstance().apply {
            set(2026, 7, 9, 22, 30, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val hedef = PlanAsistan.sonrakiAksam(simdi, 22 * 60)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(10, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `sonraki aksam tam dakikada yarin sayilir`() {
        val simdi = java.util.Calendar.getInstance().apply {
            set(2026, 7, 9, 22, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val hedef = PlanAsistan.sonrakiAksam(simdi, 22 * 60)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = hedef }
        assertEquals(10, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
}
