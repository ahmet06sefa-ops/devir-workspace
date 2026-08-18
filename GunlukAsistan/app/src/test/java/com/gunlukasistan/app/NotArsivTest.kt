package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.35 · Katalog #37 — [NotArsiv] küme köprüsü testleri. */
class NotArsivTest {

    @Test
    fun kume_turUydurma() {
        val k = setOf(3L, 12L, 9999999999L)
        assertEquals(k, NotArsiv.kumeJsondan(NotArsiv.kumeJsonaYaz(k)))
    }

    @Test
    fun kume_bosVeBozukGuvenli() {
        assertTrue(NotArsiv.kumeJsondan(null).isEmpty())
        assertTrue(NotArsiv.kumeJsondan("").isEmpty())
        assertTrue(NotArsiv.kumeJsondan("[]").isEmpty())
    }

    @Test
    fun kume_yazimSirali() {
        assertEquals("[]", NotArsiv.kumeJsonaYaz(emptySet()))
        assertEquals("[3,12,45]", NotArsiv.kumeJsonaYaz(setOf(45L, 3L, 12L)))
    }
}
