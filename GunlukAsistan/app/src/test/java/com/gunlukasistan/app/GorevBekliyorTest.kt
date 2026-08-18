package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.36 · Katalog #16 — [GorevBekliyor] rozet mantığı testleri. */
class GorevBekliyorTest {

    @Test
    fun rozet_bekleyendeEklenir() {
        assertEquals("⏳ Ayşe'den onay bekle", GorevBekliyor.rozetliMetin("Ayşe'den onay bekle", true))
    }

    @Test
    fun rozet_beklemeyendeDegismez() {
        assertEquals("Market alışverişi", GorevBekliyor.rozetliMetin("Market alışverişi", false))
        assertEquals("", GorevBekliyor.rozetliMetin("", false))
    }
}
