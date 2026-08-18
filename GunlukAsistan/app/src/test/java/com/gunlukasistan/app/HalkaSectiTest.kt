package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.7 · A3 — Halka seçici geometrisinin birim testleri.
 *
 * Kadranın 4 ana yönü (tepe/sağ/alt/sol) ve kenar durumları
 * (tam tur, sıfır, tolerans bandı) sabit kalır; View katmanı
 * değişse bile matematik bozulmaz.
 */
class HalkaSectiTest {

    // ── açı hesabı ───────────────────────────────────────────────

    @Test
    fun tepe_sifirDerece() {
        // Merkezin tam üstü (12 yönü) = 0°
        val a = HalkaSecti.aci(100f, 0f, 100f, 100f)
        assertEquals(0f, a, 0.5f)
    }

    @Test
    fun sag_doksanDerece() {
        val a = HalkaSecti.aci(200f, 100f, 100f, 100f)
        assertEquals(90f, a, 0.5f)
    }

    @Test
    fun alt_yuzSeksenDerece() {
        val a = HalkaSecti.aci(100f, 200f, 100f, 100f)
        assertEquals(180f, a, 0.5f)
    }

    @Test
    fun sol_ikiYuzYetmisDerece() {
        val a = HalkaSecti.aci(0f, 100f, 100f, 100f)
        assertEquals(270f, a, 0.5f)
    }

    @Test
    fun aci_herZamanPozitif() {
        // Köşeler dahil hiçbir nokta negatif açı üretmemeli
        val noktalar = listOf(
            0f to 0f, 200f to 0f, 0f to 200f, 200f to 200f,
            100f to 40f, 160f to 100f, 100f to 160f, 40f to 100f
        )
        for ((x, y) in noktalar) {
            val a = HalkaSecti.aci(x, y, 100f, 100f)
            assertTrue("açı negatif: $a", a >= 0f)
            assertTrue("açı 360 ve üstü: $a", a < 360f)
        }
    }

    // ── açı → dakika ─────────────────────────────────────────────

    @Test
    fun ceyrekTur_15dakika() {
        assertEquals(15, HalkaSecti.acidanDakika(90f))
    }

    @Test
    fun yarimTur_30dakika() {
        assertEquals(30, HalkaSecti.acidanDakika(180f))
    }

    @Test
    fun tamTuraYakin_60dakika() {
        assertEquals(60, HalkaSecti.acidanDakika(359.5f))
    }

    @Test
    fun tepe_altSiniraCekilir() {
        // 0° ham olarak 0 dk verirdi; "0 dakikalık sayaç" kurulamasın
        assertEquals(HalkaSecti.MIN_DAKIKA, HalkaSecti.acidanDakika(0f))
    }

    @Test
    fun birCizgi_birDakika() {
        // 6° = 1 dk; kadran 60 çizgili
        assertEquals(5, HalkaSecti.acidanDakika(30f))
        assertEquals(25, HalkaSecti.acidanDakika(150f))
    }

    @Test
    fun yuvarlama_enYakinDakika() {
        // 93° 15.5 dk → en yakın 16 değil 15 (15.5 yukarı mı aşağı mı?)
        // roundToInt: 15.5 → 16
        assertEquals(16, HalkaSecti.acidanDakika(93f))
        assertEquals(15, HalkaSecti.acidanDakika(91f))
    }

    @Test
    fun tersEsleme_dakikaAcidanGeriDonuyor() {
        for (dk in listOf(1, 7, 15, 30, 45, 60)) {
            val geri = HalkaSecti.acidanDakika(HalkaSecti.dakikadanAci(dk))
            assertEquals("dk=$dk geri dönemedi", dk, geri)
        }
    }

    // ── halka bandı ──────────────────────────────────────────────

    @Test
    fun halkaninUstu_bandda() {
        // dış yarıçap 100; çizgi başı ~89. Bandın ortası 95 olmalı.
        assertTrue(HalkaSecti.halkadaMi(100f, 5f, 100f, 100f, 89f, 100f))
    }

    @Test
    fun merkez_banddaDegil() {
        // Ortaya dokunma başlat/duraklat olarak kalmalı
        assertFalse(HalkaSecti.halkadaMi(100f, 100f, 100f, 100f, 89f, 100f))
        assertFalse(HalkaSecti.halkadaMi(100f, 60f, 100f, 100f, 89f, 100f))
    }

    @Test
    fun disTasma_banddaDegil() {
        // Kadran bitiminden epey uzaktaki dokunma sayılmasın
        assertFalse(HalkaSecti.halkadaMi(100f, -30f, 100f, 100f, 89f, 100f))
    }

    @Test
    fun bozukGeometri_reddedilir() {
        assertFalse(HalkaSecti.halkadaMi(10f, 10f, 0f, 0f, 0f, 0f))
        assertFalse(HalkaSecti.halkadaMi(10f, 10f, 0f, 0f, -5f, -1f))
    }

    // ── sürükleme eşiği ──────────────────────────────────────────

    @Test
    fun kucukHareket_tiklama() {
        assertFalse(HalkaSecti.suruklemeMi(100f, 100f, 103f, 102f, 24f))
    }

    @Test
    fun buyukHareket_surukleme() {
        assertTrue(HalkaSecti.suruklemeMi(100f, 100f, 140f, 100f, 24f))
    }

    @Test
    fun esikTamSiniri_gecerli() {
        // Tan 24: (24,0) tam eşik → sürükleme
        assertTrue(HalkaSecti.suruklemeMi(100f, 100f, 124f, 100f, 24f))
    }
}
