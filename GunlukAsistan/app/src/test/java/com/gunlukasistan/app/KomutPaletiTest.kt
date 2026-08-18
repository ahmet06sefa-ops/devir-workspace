package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.6 · Öneri D39 — komut paleti puanlama (saf).
 *
 * "say" yazınca Sayaç en üstte olmalı; anahtar kelime eşleşmesi
 * başlık eşleşmesinin önüne geçmemeli.
 */
class KomutPaletiTest {

    @Test
    fun `tam baslik oneci kismi eslesmeyi gecer`() {
        // "say" hem "Sayaç"ı hem başlıkta "sayaç" geçen 5/15 dk
        // çiplerini bulur; tam başlık başlangıcı (puan 4) önce gelir
        assertEquals("Sayaç", KomutPaleti.sirala("say").first().baslik)
    }

    @Test
    fun `anahtar kelime eslesmesi dusuk puanli`() {
        val odak = KomutPaleti.KOMUTLAR.first { it.sayacDakika == 25 }
        assertEquals(1, KomutPaleti.puan("konsantre", odak))
    }

    @Test
    fun `sorguda hicbir sey bulunamazsa liste bos`() {
        assertTrue(KomutPaleti.sirala("zehir hafiyesi").isEmpty())
    }

    @Test
    fun `bos sorgu tum komutlari sirali tutar`() {
        assertEquals(KomutPaleti.KOMUTLAR, KomutPaleti.sirala(""))
    }

    @Test
    fun `pomodoro yazinca odak baslatma gorunur`() {
        val ilk = KomutPaleti.sirala("pomodoro").first()
        assertEquals(25, ilk.sayacDakika)
    }
}
