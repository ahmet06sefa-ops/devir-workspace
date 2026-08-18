package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.40 · Katalog #46 — [AliskanlikNot] saf katman testleri. */
class AliskanlikNotTest {

    @Test
    fun `yaz oku tam tur`() {
        val kok = AliskanlikNot.notYazPure(JSONObject(), 7L, 20260809, "hastaydım")
        assertEquals("hastaydım", AliskanlikNot.notOkuPure(kok, 7L, 20260809))
        assertTrue(AliskanlikNot.habitNotVarmiPure(kok, 7L))
    }

    @Test
    fun `bos metin notu siler dugum bosalirsa gider`() {
        var kok = AliskanlikNot.notYazPure(JSONObject(), 7L, 20260809, "x")
        kok = AliskanlikNot.notYazPure(kok, 7L, 20260809, "  ")
        assertEquals("", AliskanlikNot.notOkuPure(kok, 7L, 20260809))
        assertFalse(AliskanlikNot.habitNotVarmiPure(kok, 7L))
        assertFalse(kok.has("7"))
    }

    @Test
    fun `aliskanliklar ve gunler birbirine karismaz`() {
        var kok = AliskanlikNot.notYazPure(JSONObject(), 1L, 20260808, "a")
        kok = AliskanlikNot.notYazPure(kok, 2L, 20260808, "b")
        kok = AliskanlikNot.notYazPure(kok, 1L, 20260809, "c")
        assertEquals("a", AliskanlikNot.notOkuPure(kok, 1L, 20260808))
        assertEquals("b", AliskanlikNot.notOkuPure(kok, 2L, 20260808))
        assertEquals("c", AliskanlikNot.notOkuPure(kok, 1L, 20260809))
        assertEquals("", AliskanlikNot.notOkuPure(kok, 2L, 20260809))
    }

    @Test
    fun `bozuk kok guvenli bos`() {
        val kok = AliskanlikNot.kokOku("bozuk {")
        assertEquals("", AliskanlikNot.notOkuPure(kok, 7L, 20260809))
        assertFalse(AliskanlikNot.habitNotVarmiPure(kok, 7L))
    }
}
