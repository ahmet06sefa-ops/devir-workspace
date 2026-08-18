package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.4 · Öneri A9 — sesli geri sayım eşik tablosu (saf).
 *
 * Ritim bilinçli kademeli: uzakken seyrek (5dk/2dk/1dk), son 30
 * saniyede sık, son 10'da sayarak. Hiçbir eşik "sürpriz" değildir;
 * tablo test ile sabitlenir.
 */
class SayacSesTest {

    @Test
    fun `dakika esikleri dogru metni verir`() {
        assertEquals("Beş dakika kaldı", SayacSes.konusmaMetni(300))
        assertEquals("Bir dakika kaldı", SayacSes.konusmaMetni(60))
    }

    @Test
    fun `son on saniyede sayar ama sifiri soylemez`() {
        assertEquals("Üç", SayacSes.konusmaMetni(3))
        assertNull(SayacSes.konusmaMetni(0)) // bitişi zil söyler
    }

    @Test
    fun `esik disinda sessiz kalir`() {
        assertNull(SayacSes.konusmaMetni(299))
        assertNull(SayacSes.konusmaMetni(45))
        assertNull(SayacSes.konusmaMetni(7))
    }

    @Test
    fun `tekrar korumasi ayni saniyeyi iki kez soyletmez`() {
        val soylenen = mutableSetOf(60)
        assertFalse(SayacSes.soylenmeli(60, soylenen))
        assertTrue(SayacSes.soylenmeli(30, soylenen))
        assertFalse(SayacSes.soylenmeli(7, soylenen)) // eşik değil
    }
}
