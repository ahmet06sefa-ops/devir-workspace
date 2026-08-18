package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.43 · Madde #3 — [SesManzarasi.secimPure] saf testler. */
class SesManzarasiOzelTest {

    @Test
    fun `ozel kod dosya varken gecerli`() {
        assertEquals(SesManzarasi.OZEL_KOD, SesManzarasi.secimPure(SesManzarasi.OZEL_KOD, true))
    }

    @Test
    fun `ozel kod dosya yoksa secimsizlige duser`() {
        assertEquals(-1, SesManzarasi.secimPure(SesManzarasi.OZEL_KOD, false))
    }

    @Test
    fun `normal indeks kenetlenir`() {
        assertEquals(SesManzarasi.SESLER.lastIndex, SesManzarasi.secimPure(999, true))
        assertEquals(-1, SesManzarasi.secimPure(-7, true))
        assertEquals(3, SesManzarasi.secimPure(3, false))
    }
}
