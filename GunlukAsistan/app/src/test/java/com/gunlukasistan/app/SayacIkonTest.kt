package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.3 · Öneri B19 — durum çubuğu dakika ikonu (saf kararlar).
 *
 * Yuvarlama yönü bilinçli: geri sayımda 04:31 → "5" (kullanıcıya
 * kalan bütün dakikayı söyler), kronometrede 12:47 → "12" ("bu
 * kadardır çalışıyorum" cümlesi kurulsun).
 */
class SayacIkonTest {

    @Test
    fun `geri sayimda kalan dakika yukari yuvarlanir`() {
        assertEquals(5, SayacIkon.gosterilecekSayi(4 * 60_000L + 31_000L, true))
    }

    @Test
    fun `kronometrede gecen dakika asagi yuvarlanir`() {
        assertEquals(12, SayacIkon.gosterilecekSayi(12 * 60_000L + 47_000L, false))
    }

    @Test
    fun `tam dakikada geri sayim ayni rakami verir`() {
        assertEquals(7, SayacIkon.gosterilecekSayi(7 * 60_000L, true))
    }

    @Test
    fun `buyuk degerler 99 ile kestirilir`() {
        assertEquals(99, SayacIkon.gosterilecekSayi(180 * 60_000L, true))
        assertEquals(99, SayacIkon.gosterilecekSayi(240 * 60_000L, false))
    }

    @Test
    fun `sifir ve negatif degerler sifir verir`() {
        assertEquals(0, SayacIkon.gosterilecekSayi(0L, true))
        assertEquals(0, SayacIkon.gosterilecekSayi(-1L, false))
    }
}
