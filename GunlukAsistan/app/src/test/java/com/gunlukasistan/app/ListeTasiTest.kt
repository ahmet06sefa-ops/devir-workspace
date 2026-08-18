package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.44 · Madde #4 — [ListeTasi] saf testler. */
class ListeTasiTest {

    @Test
    fun `yukari tasima temel`() {
        val l = mutableListOf("a", "b", "c")
        assertTrue(ListeTasi.yukariTasi(l, 2))
        assertEquals(listOf("a", "c", "b"), l)
    }

    @Test
    fun `ilk oge yukari tasinamaz`() {
        val l = mutableListOf("a", "b")
        assertFalse(ListeTasi.yukariTasi(l, 0))
        assertEquals(listOf("a", "b"), l)
    }

    @Test
    fun `asagi tasima temel`() {
        val l = mutableListOf("a", "b", "c")
        assertTrue(ListeTasi.asagiTasi(l, 0))
        assertEquals(listOf("b", "a", "c"), l)
    }

    @Test
    fun `son oge asagi tasinamaz ve bos liste guvenli`() {
        val l = mutableListOf("a", "b")
        assertFalse(ListeTasi.asagiTasi(l, 1))
        assertEquals(listOf("a", "b"), l)
        val bos = mutableListOf<String>()
        assertFalse(ListeTasi.yukariTasi(bos, 0))
        assertFalse(ListeTasi.asagiTasi(bos, 0))
    }
}
