package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** v10.38 · Katalog #18 — [GorevErteleme] saf mantık testleri. */
class GorevErtelemeTest {

    @Test
    fun `hafta kodu yil ve haftayi birlestirir`() {
        val cal = java.util.Calendar.getInstance()
        val kod = GorevErteleme.haftaKodu(cal)
        assertEquals(cal.get(java.util.Calendar.YEAR), kod / 100)
        assertEquals(cal.get(java.util.Calendar.WEEK_OF_YEAR), kod % 100)
    }

    @Test
    fun `en cok satir esigi uygular`() {
        assertNull(GorevErteleme.enCokSatiri(emptyMap()))
        assertNull(GorevErteleme.enCokSatiri(mapOf(5L to 2)))           // eşik altı
        assertEquals(5L to 3, GorevErteleme.enCokSatiri(mapOf(5L to 3)))
        assertEquals(9L to 7, GorevErteleme.enCokSatiri(mapOf(5L to 3, 9L to 7, 2L to 4)))
    }

    @Test
    fun `esik ozel verilebilir`() {
        assertEquals(1L to 2, GorevErteleme.enCokSatiri(mapOf(1L to 2), esik = 2))
        assertNull(GorevErteleme.enCokSatiri(mapOf(1L to 1), esik = 2))
    }
}
