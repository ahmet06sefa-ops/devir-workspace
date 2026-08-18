package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.27 (öneri #76) — Haftalık odak hedefi saf kararları.
 * Haftalık hedef = günlük × 7; yüzde kelepçeli 0..100.
 */
class OdakHaftaTest {

    @Test
    fun `haftalikHedef - gunlugun yedi kati`() {
        assertEquals(700, OdakHafta.haftalikHedef(100))
        assertEquals(175, OdakHafta.haftalikHedef(25))
        assertEquals(0, OdakHafta.haftalikHedef(0))
    }

    @Test
    fun `haftalikHedef - negatif gunluk hedef korunur`() {
        assertEquals(0, OdakHafta.haftalikHedef(-50))
    }

    @Test
    fun `yuzde - kelepce ve bolme korumasi`() {
        assertEquals(0, OdakHafta.yuzde(0, 700))
        assertEquals(50, OdakHafta.yuzde(350, 700))
        assertEquals(100, OdakHafta.yuzde(700, 700))
        assertEquals(100, OdakHafta.yuzde(900, 700)) // taşma kesilir
        assertEquals(0, OdakHafta.yuzde(100, 0))     // sıfır hedef güvenli
        assertEquals(0, OdakHafta.yuzde(-5, 700))    // negatif süre korunur
    }
}
