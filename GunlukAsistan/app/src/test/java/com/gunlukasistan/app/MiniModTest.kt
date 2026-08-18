package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.45/v10.46 · Madde #7 & #8 — [MiniMod] saf testler. */
class MiniModTest {

    @Test
    fun `dikey telefon uzun pencere`() {
        assertEquals(3 to 4, MiniMod.pipOrani(1080, 2400))
    }

    @Test
    fun `yatay genis pencere`() {
        assertEquals(16 to 9, MiniMod.pipOrani(2400, 1080))
    }

    @Test
    fun `asi dar keneta`() {
        assertEquals(3 to 4, MiniMod.pipOrani(400, 2400))
        assertEquals(13 to 6, MiniMod.pipOrani(6000, 1000))
    }

    @Test
    fun `sifir guvenli varsayilan`() {
        assertEquals(3 to 4, MiniMod.pipOrani(0, 0))
    }

    @Test
    fun `aksiyon kodlari geri sayimda 3 eylem dondurur`() {
        assertEquals(listOf(101, 102, 103), MiniMod.aksiyonKodlari(calisiyorMu = true, geriSayimMi = true))
    }

    @Test
    fun `aksiyon kodlari kronometrede 2 eylem dondurur`() {
        assertEquals(listOf(101, 102), MiniMod.aksiyonKodlari(calisiyorMu = true, geriSayimMi = false))
    }

    @Test
    fun `pip olcegi pip modunda kadrani buyutur`() {
        assertEquals(1.15f, MiniMod.pipOlcegi(true), 0.001f)
        assertEquals(1.0f, MiniMod.pipOlcegi(false), 0.001f)
    }

    @Test
    fun `pip dolgu dp pip modunda sifir dondurur`() {
        assertEquals(4, MiniMod.pipDolguDp(true))
        assertEquals(24, MiniMod.pipDolguDp(false))
    }
}
