package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.34 · Katalog #26 — [NotKilit] saf JSON köprüsü testleri. */
class NotKilitTest {

    @Test
    fun kume_turUydurma() {
        val k = setOf(12L, 45L, 9999999999L)
        assertEquals(k, NotKilit.kumeJsondan(NotKilit.kumeJsonaYaz(k)))
    }

    @Test
    fun kume_bosVeBozukGuvenli() {
        assertTrue(NotKilit.kumeJsondan(null).isEmpty())
        assertTrue(NotKilit.kumeJsondan("").isEmpty())
        assertTrue(NotKilit.kumeJsondan("[]").isEmpty())
        // Bozuk girdide sayılar toparlanır
        assertEquals(setOf(7L), NotKilit.kumeJsondan("[7,bozuk]"))
    }

    @Test
    fun kume_yazimSiraliVeTekrar() {
        assertEquals("[]", NotKilit.kumeJsonaYaz(emptySet()))
        assertEquals("[3,12,45]", NotKilit.kumeJsonaYaz(setOf(45L, 3L, 12L)))
    }
}
