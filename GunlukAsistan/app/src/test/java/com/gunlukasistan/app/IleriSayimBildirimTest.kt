package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.41 · Kullanıcı maddesi #1 — [IleriSayimBildirim.formatSure] saf testler. */
class IleriSayimBildirimTest {

    @Test
    fun `sifir ve dakika alti`() {
        assertEquals("00:00", IleriSayimBildirim.formatSure(0))
        assertEquals("00:07", IleriSayimBildirim.formatSure(7_000))
        assertEquals("00:59", IleriSayimBildirim.formatSure(59_999))
    }

    @Test
    fun `dakika ve saat duzeni`() {
        assertEquals("01:05", IleriSayimBildirim.formatSure(65_000))
        assertEquals("59:59", IleriSayimBildirim.formatSure(3_599_000))
        assertEquals("1:02:05", IleriSayimBildirim.formatSure(3_725_000))
        assertEquals("12:00:00", IleriSayimBildirim.formatSure(43_200_000))
    }

    @Test
    fun `negatif guvenli sifir`() {
        assertEquals("00:00", IleriSayimBildirim.formatSure(-5_000))
    }
}
