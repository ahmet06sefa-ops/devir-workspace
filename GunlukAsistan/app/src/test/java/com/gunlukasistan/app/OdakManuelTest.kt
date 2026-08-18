package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.19 · S2 — Manuel odak kelepçe tablosu.
 */
class OdakManuelTest {

    @Test
    fun kelepcele_iciDegerAynen() {
        assertEquals(1, OdakManuel.kelepcele(1))
        assertEquals(25, OdakManuel.kelepcele(25))
        assertEquals(480, OdakManuel.kelepcele(480))
    }

    @Test
    fun kelepcele_sinirlarDisi() {
        assertEquals(1, OdakManuel.kelepcele(0))
        assertEquals(1, OdakManuel.kelepcele(-40))
        assertEquals(480, OdakManuel.kelepcele(9999))
    }
}
