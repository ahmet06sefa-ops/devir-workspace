package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Sosyal meydan okuma ve akıllı bildirim filtresi saf testleri.
 */
class SosyalVeBildirimFiltreTest {

    // ── SosyalMeydanOkumaMotoru ──

    @Test
    fun `uye puan tamamlama ve bonus birlestirir`() {
        val u = SosyalMeydanOkumaMotoru.Uye("Ahmet", 5, 20)
        assertEquals(70, u.puan)
    }

    @Test
    fun `sirala puanlara gore azalan dizi verir`() {
        val uyeler = listOf(
            SosyalMeydanOkumaMotoru.Uye("A", 1),
            SosyalMeydanOkumaMotoru.Uye("B", 5),
            SosyalMeydanOkumaMotoru.Uye("C", 3)
        )
        val s = SosyalMeydanOkumaMotoru.sirala(uyeler)
        assertEquals("B", s[0].ad)
        assertEquals("C", s[1].ad)
        assertEquals("A", s[2].ad)
    }

    @Test
    fun `kazanan en yuksek puanli uyeyi doner`() {
        val uyeler = listOf(
            SosyalMeydanOkumaMotoru.Uye("A", 2),
            SosyalMeydanOkumaMotoru.Uye("B", 9)
        )
        assertEquals("B", SosyalMeydanOkumaMotoru.kazanan(uyeler)!!.ad)
    }

    @Test
    fun `kazanan bos listede null doner`() {
        assertNull(SosyalMeydanOkumaMotoru.kazanan(emptyList()))
    }

    @Test
    fun `hedef ozet yuzdeyi dogru hesaplar`() {
        val m = SosyalMeydanOkumaMotoru.MeydanOkuma("Ders", 7, 50, listOf(
            SosyalMeydanOkumaMotoru.Uye("A", 20),
            SosyalMeydanOkumaMotoru.Uye("B", 5)
        ))
        assertEquals(50, SosyalMeydanOkumaMotoru.hedefOzet(50, m.uyeler))
    }

    @Test
    fun `durum metni lider ve puanlari icerir`() {
        val m = SosyalMeydanOkumaMotoru.MeydanOkuma("Ders", 7, 10, listOf(
            SosyalMeydanOkumaMotoru.Uye("A", 6),
            SosyalMeydanOkumaMotoru.Uye("B", 3)
        ))
        val metin = SosyalMeydanOkumaMotoru.durumMetni(m)
        assertTrue(metin.contains("Lider: A"))
        assertTrue(metin.contains("B"))
    }

    // ── BildirimFiltreMotoru ──

    @Test
    fun `sessiz dilim icinde dogru tespit eder`() {
        val dilim = BildirimFiltreMotoru.SessizDilim(22 * 60, 7 * 60) // 22:00-07:00
        assertTrue(BildirimFiltreMotoru.sessizMi(23 * 60, dilim))
        assertTrue(BildirimFiltreMotoru.sessizMi(6 * 60, dilim))
        assertFalse(BildirimFiltreMotoru.sessizMi(12 * 60, dilim))
    }

    @Test
    fun `sessiz dilim yoksa false doner`() {
        assertFalse(BildirimFiltreMotoru.sessizMi(12 * 60, null))
    }

    @Test
    fun `oncelik odak ve gecikme ile artar`() {
        assertTrue(BildirimFiltreMotoru.oncelik(50, true, true) > BildirimFiltreMotoru.oncelik(50, false, false))
    }

    @Test
    fun `atla dusuk onem sessiz veya aktifken`() {
        assertTrue(BildirimFiltreMotoru.atlaMi(20, 50, sessizModAcik = true, kullaniciAktifMi = false))
        assertTrue(BildirimFiltreMotoru.atlaMi(20, 50, sessizModAcik = false, kullaniciAktifMi = true))
        assertFalse(BildirimFiltreMotoru.atlaMi(80, 50, sessizModAcik = true, kullaniciAktifMi = true))
    }

    @Test
    fun `tur onemi kritik tipleri yuksek puanlar`() {
        assertTrue(BildirimFiltreMotoru.turOnemi("namaz") > BildirimFiltreMotoru.turOnemi("motivasyon"))
        assertEquals(80, BildirimFiltreMotoru.turOnemi("alarm bitis"))
    }
}
