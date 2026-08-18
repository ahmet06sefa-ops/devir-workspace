package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.4 · Öneri A7 — mola kişiliği (saf rotasyon).
 *
 * Aynı tur aynı öneriyi vermeli (bildirim yeniden kurulursa metin
 * titremesin), ardışık turlar havuzda ilerlemeli.
 */
class MolaKisilikTest {

    @Test
    fun `ayni tur ayni oneriyi verir`() {
        assertEquals(MolaKisilik.oneri(2, false), MolaKisilik.oneri(2, false))
    }

    @Test
    fun `ardisik turlar havuzda ilerler`() {
        val ilk = MolaKisilik.oneri(0, false)
        val ikinci = MolaKisilik.oneri(1, false)
        assertNotEquals(ilk, ikinci)
    }

    @Test
    fun `tur havuz boyunu asarsa basa doner`() {
        // Kısa mola havuzu 5 öğe — tur 0 ile tur 5 aynı öneriyi verir
        assertEquals(MolaKisilik.oneri(0, false), MolaKisilik.oneri(5, false))
    }

    @Test
    fun `uzun mola kendi havuzundan secilir`() {
        val kisa = MolaKisilik.oneri(0, false)
        val uzun = MolaKisilik.oneri(0, true)
        assertNotEquals(kisa.metin, uzun.metin)
    }

    @Test
    fun `govde temel metni korur ve oneriyi ekler`() {
        val g = MolaKisilik.govde("5 dk mola", 3, false)
        assertTrue(g.startsWith("5 dk mola · "))
        assertEquals(MolaKisilik.oneri(3, false).metin, g.substringAfter("· ", "").substringAfter(' '))
    }
}
