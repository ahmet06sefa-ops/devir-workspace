package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.39 · Katalog #42/#45 — [AliskanlikMola] ve [Kural21] saf testler. */
class AliskanlikMolaTest {

    @Test
    fun `mola durumu acik aralik ve kapali set`() {
        assertTrue(AliskanlikMola.moladaMiPure(20260810, 20260809, emptySet()))
        assertTrue(AliskanlikMola.moladaMiPure(20260801, 0, setOf(20260801)))
        assertFalse(AliskanlikMola.moladaMiPure(20260808, 20260809, emptySet()))
        assertFalse(AliskanlikMola.moladaMiPure(20260808, 0, emptySet()))
        assertFalse(AliskanlikMola.moladaMiPure(20260808, 0, setOf(20260809)))
    }

    @Test
    fun `set yaz oku tam tur`() {
        val k = setOf(20260801, 20260803, 20260802)
        assertEquals(k, AliskanlikMola.setOku(AliskanlikMola.setYaz(k)))
    }

    @Test
    fun `bozuk set guvenli bos`() {
        assertEquals(emptySet<Int>(), AliskanlikMola.setOku("bozuk"))
        assertEquals(emptySet<Int>(), AliskanlikMola.setOku(null))
    }

    @Test
    fun `gun anahtari deterministik ve sekiz hane`() {
        val ms = System.currentTimeMillis()
        val a = AliskanlikMola.gunAnahtari(ms)
        assertEquals(a, AliskanlikMola.gunAnahtari(ms))
        assertTrue(a in 19000101..29991231)
    }

    @Test
    fun `kural21 yuzde sinirlari`() {
        assertEquals(0, Kural21.yuzde(0))
        assertEquals(100, Kural21.yuzde(21))
        assertEquals(100, Kural21.yuzde(40))
        assertEquals(47, Kural21.yuzde(10))
        assertEquals(0, Kural21.yuzde(-5))
    }
}
