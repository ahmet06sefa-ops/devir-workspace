package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Görev takvimi / yaklaşan görev motoru saf testleri.
 */
class GorevTakvimiMotoruTest {

    private val bugun = "20260815"

    @Test
    fun `gun farki dogru hesaplanir`() {
        assertEquals(0, GorevTakvimiMotoru.gunFarki("20260815", bugun))
        assertEquals(1, GorevTakvimiMotoru.gunFarki("20260816", bugun))
        assertEquals(-1, GorevTakvimiMotoru.gunFarki("20260814", bugun))
    }

    @Test
    fun `durum siniflarini dogru belirler`() {
        assertEquals("gecikti", GorevTakvimiMotoru.durum("20260814", bugun))
        assertEquals("bugun", GorevTakvimiMotoru.durum("20260815", bugun))
        assertEquals("yarin", GorevTakvimiMotoru.durum("20260816", bugun))
        assertEquals("ileri", GorevTakvimiMotoru.durum("20260820", bugun))
    }

    @Test
    fun `yaklasanlar bugun ve yarini icerir`() {
        val gorevler = listOf(
            GorevTakvimiMotoru.Gorev("A", "20260814"), // gecikti
            GorevTakvimiMotoru.Gorev("B", "20260815"), // bugun
            GorevTakvimiMotoru.Gorev("C", "20260816"), // yarin
            GorevTakvimiMotoru.Gorev("D", "20260820")  // ileri
        )
        val yaklasan = GorevTakvimiMotoru.yaklasanlar(gorevler, bugun)
        assertEquals(2, yaklasan.size)
        assertTrue(yaklasan.any { it.ad == "B" })
        assertTrue(yaklasan.any { it.ad == "C" })
    }

    @Test
    fun `gecikenler gecikmis gorevleri verir`() {
        val gorevler = listOf(
            GorevTakvimiMotoru.Gorev("A", "20260810"),
            GorevTakvimiMotoru.Gorev("B", "20260815"),
            GorevTakvimiMotoru.Gorev("C", "20260820")
        )
        val geciken = GorevTakvimiMotoru.gecikenler(gorevler, bugun)
        assertEquals(1, geciken.size)
        assertEquals("A", geciken[0].ad)
    }

    @Test
    fun `gune ata gorevi dogru baglar`() {
        val g = GorevTakvimiMotoru.guneAta("Ders", "20260816")
        assertEquals("Ders", g.ad)
        assertEquals("20260816", g.gunAnahtar)
    }
}
