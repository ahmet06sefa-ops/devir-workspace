package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.41 · Kullanıcı maddesi #2 — [SayacAyar.kadranCarpani] saf testler. */
class SayacAyarOlcekTest {

    @Test
    fun `dort kademe carpani`() {
        assertEquals(0.80f, SayacAyar.kadranCarpani(0), 0.0001f)
        assertEquals(0.90f, SayacAyar.kadranCarpani(1), 0.0001f)
        assertEquals(1.00f, SayacAyar.kadranCarpani(2), 0.0001f)
        assertEquals(1.15f, SayacAyar.kadranCarpani(3), 0.0001f)
    }

    @Test
    fun `sinir disi kenetlenir`() {
        assertEquals(0.80f, SayacAyar.kadranCarpani(-3), 0.0001f)
        assertEquals(1.15f, SayacAyar.kadranCarpani(99), 0.0001f)
    }

    @Test
    fun `yuzde gosterimi`() {
        assertEquals(80, SayacAyar.kadranYuzde(0))
        assertEquals(100, SayacAyar.kadranYuzde(2))
        assertEquals(115, SayacAyar.kadranYuzde(3))
    }
}
