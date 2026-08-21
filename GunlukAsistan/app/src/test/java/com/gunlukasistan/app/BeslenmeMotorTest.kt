package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.54 — BeslenmeMotor temel birim testleri.
 * (Context gerektirmeyen kısım: öğün tipleri ve veri modeli.)
 */
class BeslenmeMotorTest {

    @Test
    fun `ogun tipleri sirali ve beklenen`() {
        assertEquals(4, BeslenmeMotor.OGRUN_TIPLERI.size)
        assertEquals("Kahvaltı", BeslenmeMotor.OGRUN_TIPLERI[0])
        assertEquals("Öğle", BeslenmeMotor.OGRUN_TIPLERI[1])
        assertEquals("Akşam", BeslenmeMotor.OGRUN_TIPLERI[2])
        assertEquals("Ara Öğün", BeslenmeMotor.OGRUN_TIPLERI[3])
    }

    @Test
    fun `ogun veri modeli alanlari tasir`() {
        val o = BeslenmeMotor.Ogun("Yumurta", 350, "Kahvaltı", 1000L)
        assertEquals("Yumurta", o.ad)
        assertEquals(350, o.kalori)
        assertEquals("Kahvaltı", o.tip)
        assertEquals(1000L, o.tarih)
    }

    @Test
    fun `kalori hedefi sinirlandirilir`() {
        // Motor içi coerce: 500..10000
        assertTrue(BeslenmeMotor.kaloriHedefiDuzelt(100) >= 500)
        assertTrue(BeslenmeMotor.kaloriHedefiDuzelt(99999) <= 10000)
        assertEquals(2000, BeslenmeMotor.kaloriHedefiDuzelt(2000))
    }
}
