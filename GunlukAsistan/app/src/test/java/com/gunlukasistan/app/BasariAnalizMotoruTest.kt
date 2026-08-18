package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Başarı / istatistik analiz motoru saf testleri.
 */
class BasariAnalizMotoruTest {

    @Test
    fun `basari orani dogru hesaplanir`() {
        assertEquals(50, BasariAnalizMotoru.basariOrani(5, 10))
        assertEquals(100, BasariAnalizMotoru.basariOrani(10, 10))
        assertEquals(0, BasariAnalizMotoru.basariOrani(0, 10))
        assertEquals(0, BasariAnalizMotoru.basariOrani(5, 0))
    }

    @Test
    fun `seri orani aktif gun yuzdesini verir`() {
        assertEquals(50, BasariAnalizMotoru.seriOrani(15, 30))
        assertEquals(100, BasariAnalizMotoru.seriOrani(30, 30))
        assertEquals(0, BasariAnalizMotoru.seriOrani(0, 30))
    }

    @Test
    fun `durum notu basariya gore degisir`() {
        assertTrue(BasariAnalizMotoru.durumNotu(90).contains("Harika"))
        assertTrue(BasariAnalizMotoru.durumNotu(50).contains("Orta"))
        assertTrue(BasariAnalizMotoru.durumNotu(10).contains("Sakin"))
    }

    @Test
    fun `ay raporu tum alanlari icerir`() {
        val r = BasariAnalizMotoru.ayRaporu(30, 20, 60, 1200)
        assertTrue(r.contains("Aylık Başarı"))
        assertTrue(r.contains("İstikrar"))
        assertTrue(r.contains("60 tamamlama"))
        assertTrue(r.contains("1200 dk"))
        assertTrue(r.contains("Günlük ort."))
    }

    @Test
    fun `ay raporu aktif gun sifirken de calisir`() {
        val r = BasariAnalizMotoru.ayRaporu(30, 0, 0, 0)
        assertTrue(r.contains("İstikrar"))
        assertTrue(r.isNotBlank())
    }
}
