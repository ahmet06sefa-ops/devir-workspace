package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.32 · Katalog #23 — [NotRenk] saf JSON köprüsü testleri. */
class NotRenkTest {

    @Test
    fun json_turUydurma() {
        val h = mapOf(12L to 3, 45L to 0, 9999999999L to 4)
        assertEquals(h, NotRenk.jsondanOku(NotRenk.jsonaYaz(h)))
    }

    @Test
    fun json_bosVeBozukGuvenli() {
        assertTrue(NotRenk.jsondanOku(null).isEmpty())
        assertTrue(NotRenk.jsondanOku("").isEmpty())
        assertTrue(NotRenk.jsondanOku("{}").isEmpty())
        assertTrue(NotRenk.jsondanOku("bozuk{{{").isEmpty())
    }

    @Test
    fun json_bilinmeyenTonVeAlanlarAtlanir() {
        val okunan = NotRenk.jsondanOku("{\"1\":99,\"2\":1,\"abc\":2,\"3\":2}")
        assertEquals(mapOf(2L to 1, 3L to 2), okunan)
    }

    @Test
    fun tonGuvenli_sinirlar() {
        assertEquals(5, NotRenk.TONLAR.size)
        assertNull(NotRenk.tonGuvenli(-1))
        assertNull(NotRenk.tonGuvenli(5))
        assertEquals("Mercan", NotRenk.TONLAR[0].ad)
        assertEquals(NotRenk.TONLAR[3].argb, NotRenk.tonRenk(3))
    }
}
