package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.40 · Katalog #52 — [SeriAnaliz] saf testler. */
class SeriAnalizTest {

    @Test
    fun `bos ve tek gun`() {
        assertEquals(0, SeriAnaliz.enUzun(emptySet()))
        assertEquals(0, SeriAnaliz.ikinciEnUzun(emptySet()))
        assertEquals(1, SeriAnaliz.enUzun(setOf(20260809)))
        assertEquals(0, SeriAnaliz.ikinciEnUzun(setOf(20260809)))
    }

    @Test
    fun `iki ayrik seriden en uzun ve ikinci`() {
        val gunler = setOf(
            20260801, 20260802, 20260803,
            20260810, 20260811
        )
        assertEquals(listOf(3, 2), SeriAnaliz.seriler(gunler))
        assertEquals(3, SeriAnaliz.enUzun(gunler))
        assertEquals(2, SeriAnaliz.ikinciEnUzun(gunler))
    }

    @Test
    fun `ay sinirini asan seri tek sayilir`() {
        val gunler = setOf(20260830, 20260831, 20260901, 20260902)
        assertEquals(4, SeriAnaliz.enUzun(gunler))
        assertEquals(0, SeriAnaliz.ikinciEnUzun(gunler))
    }

    @Test
    fun `esit iki seri ikinciyi de sayar`() {
        val gunler = setOf(
            20260701, 20260702,
            20260805, 20260806
        )
        assertEquals(2, SeriAnaliz.enUzun(gunler))
        assertEquals(2, SeriAnaliz.ikinciEnUzun(gunler))
    }
}
