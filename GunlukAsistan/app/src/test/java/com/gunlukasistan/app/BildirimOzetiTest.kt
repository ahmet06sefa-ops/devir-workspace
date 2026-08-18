package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.4 · Öneri B18 — sabah özetinin satır biçimlendirmesi (saf).
 */
class BildirimOzetiTest {

    @Test
    fun `bes ve alti tum satirlar korunur`() {
        val b = listOf("Kart tekrarı", "Görev: rapor", "Seri riski")
        assertEquals(listOf("• Kart tekrarı", "• Görev: rapor", "• Seri riski"),
            BildirimOzeti.satirlar(b))
    }

    @Test
    fun `besten fazlasi kac tane daha oldugunu soyler`() {
        val b = (1..8).map { "Bildirim $it" }
        val s = BildirimOzeti.satirlar(b)
        assertEquals(6, s.size)
        assertEquals("… +3 daha", s.last())
    }

    @Test
    fun `tek bildirimde arti kacak satiri olmaz`() {
        assertEquals(listOf("• Tek"), BildirimOzeti.satirlar(listOf("Tek")))
    }

    @Test
    fun `bos liste bos satir verir`() {
        assertTrue(BildirimOzeti.satirlar(emptyList()).isEmpty())
    }
}
