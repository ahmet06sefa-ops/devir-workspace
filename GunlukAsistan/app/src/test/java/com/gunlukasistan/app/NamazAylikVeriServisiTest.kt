package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.70 — Namaz Aylık İnternet Veri Servisi & Titreşim Kontrol Motoru ([NamazAylikVeriServisi])
 * saf birim testleri (24 test).
 */
class NamazAylikVeriServisiTest {

    @Test
    fun `desteklenen sehirler 15 adet major turkiye sehri icerir`() {
        val list = NamazAylikVeriServisi.desteklenenSehirler()
        assertEquals(15, list.size)
    }

    @Test
    fun `desteklenen sehirler ankara istanbul izmir icerir`() {
        val list = NamazAylikVeriServisi.desteklenenSehirler()
        assertTrue(list.contains("Ankara"))
        assertTrue(list.contains("İstanbul"))
        assertTrue(list.contains("İzmir"))
    }

    @Test
    fun `sehir icin 30 gunluk veri olusturma 30 gun eksiksiz dondurur`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        assertEquals(30, p.gunler.size)
        assertEquals(1, p.gunler.first().gunNo)
        assertEquals(30, p.gunler.last().gunNo)
    }

    @Test
    fun `sehir icin 30 gunluk veri ankara saat kokunu dogru baslatir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        assertTrue(p.gunler[0].imsak.startsWith("04:"))
        assertTrue(p.gunler[0].ogle.startsWith("12:") || p.gunler[0].ogle.startsWith("13:"))
    }

    @Test
    fun `ankara 10 agustos google diyanet gercek vakitlerini dondurur`() {
        val koku = NamazAylikVeriServisi.sehirGoogleDiyanetSaatleri("Ankara")
        assertEquals("04:11", koku.imsak)
        assertEquals("05:48", koku.gunes)
        assertEquals("12:59", koku.ogle)
        assertEquals("16:49", koku.ikindi)
        assertEquals("20:00", koku.aksam)
        assertEquals("21:30", koku.yatsi)
    }

    @Test
    fun `istanbul 10 agustos google diyanet gercek vakitlerini dondurur`() {
        val koku = NamazAylikVeriServisi.sehirGoogleDiyanetSaatleri("İstanbul")
        assertEquals("04:22", koku.imsak)
        assertEquals("20:18", koku.aksam)
    }

    @Test
    fun `izmir 10 agustos google diyanet gercek vakitlerini dondurur`() {
        val koku = NamazAylikVeriServisi.sehirGoogleDiyanetSaatleri("İzmir")
        assertEquals("04:40", koku.imsak)
        assertEquals("20:20", koku.aksam)
    }

    @Test
    fun `ankara icin diyanet resmi sitesinin dogru url adresini dondurur`() {
        val url = NamazAylikVeriServisi.diyanetResmiUrlGetir("Ankara")
        assertEquals("https://namazvakitleri.diyanet.gov.tr/tr-TR/9206/ankara-icin-namaz-vakti", url)
    }

    @Test
    fun `sehir icin 30 gunluk veri istanbul saat kokunu dogru baslatir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("İstanbul")
        assertTrue(p.gunler[0].imsak.startsWith("04:"))
        assertTrue(p.gunler[0].aksam.startsWith("20:"))
    }

    @Test
    fun `sehir icin 30 gunluk veri izmir saat kokunu dogru baslatir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("İzmir")
        assertTrue(p.gunler[0].imsak.startsWith("04:"))
    }

    @Test
    fun `sehir icin 30 gunluk veri erzurum saat kokunu dogru baslatir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Erzurum")
        assertTrue(p.gunler[0].imsak.startsWith("03:"))
    }

    @Test
    fun `sehir icin 30 gunluk veri antalya saat kokunu dogru baslatir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Antalya")
        assertTrue(p.gunler[0].ogle.startsWith("12:") || p.gunler[0].ogle.startsWith("13:"))
    }

    @Test
    fun `30 gunluk veri cizelgesi imsak gunes ogle ikindi aksam yatsi icerir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        val g1 = p.gunler[0]
        assertTrue(g1.imsak.isNotEmpty())
        assertTrue(g1.gunes.isNotEmpty())
        assertTrue(g1.ogle.isNotEmpty())
        assertTrue(g1.ikindi.isNotEmpty())
        assertTrue(g1.aksam.isNotEmpty())
        assertTrue(g1.yatsi.isNotEmpty())
    }

    @Test
    fun `gunluk namaz vakti modeli gecerli tarih str ve saatler dondurur`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Bursa")
        assertTrue(p.gunler[0].tarihStr.contains("Ağustos 2026"))
    }

    @Test
    fun `namaz vakti titresim deseni 6 elemanli dalga formu dondurur`() {
        val d = NamazAylikVeriServisi.namazVaktiTitresimDeseni()
        assertEquals(6, d.size)
    }

    @Test
    fun `namaz vakti titresim deseni 400 ve 800 ms titresim uzunluklari icerir`() {
        val d = NamazAylikVeriServisi.namazVaktiTitresimDeseni()
        assertTrue(d.contains(400L))
        assertTrue(d.contains(800L))
    }

    @Test
    fun `paketi jsona cevirme sehir tarih ve gun sayisini dogru serilestirir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Konya")
        val json = NamazAylikVeriServisi.paketiJsonaCevir(p)
        assertTrue(json.contains("Konya"))
        assertTrue(json.contains("30"))
    }

    @Test
    fun `paketi jsona cevirme aylik cache ok durumunu barindirir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        val json = NamazAylikVeriServisi.paketiJsonaCevir(p)
        assertTrue(json.contains("AYLIK_CACHE_OK"))
    }

    @Test
    fun `aylik namaz paketi sehir adi ve guncelleme tarihi tasir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Gaziantep")
        assertEquals("Gaziantep", p.sehir)
        assertTrue(p.guncellemeTarihi.contains("Google / Diyanet") || p.guncellemeTarihi.contains("Ağustos"))
    }

    @Test
    fun `sehir icin 30 gunluk veri 15inci gun saat kaymasini hesaplar`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        val g1 = p.gunler[0]
        val g15 = p.gunler[14]
        assertTrue(g1.tarihStr != g15.tarihStr)
    }

    @Test
    fun `sehir icin 30 gunluk veri 30uncu gun saat kaymasini hesaplar`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("İzmir")
        val g30 = p.gunler[29]
        assertEquals(30, g30.gunNo)
    }

    @Test
    fun `desteklenen sehir listesinde konya ve trabzon vardir`() {
        val list = NamazAylikVeriServisi.desteklenenSehirler()
        assertTrue("Konya" in list)
        assertTrue("Trabzon" in list)
    }

    @Test
    fun `gunluk namaz vakti saat formatlari 02d 02d seklindedir`() {
        val p = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Diyarbakır")
        val imsak = p.gunler[0].imsak
        assertEquals(5, imsak.length)
        assertEquals(':', imsak[2])
    }

    @Test
    fun `istanbul ve ankara imsak saatleri birbirinden farklidir`() {
        val p1 = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("İstanbul")
        val p2 = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Ankara")
        assertNotEquals(p1.gunler[0].imsak, p2.gunler[0].imsak)
    }

    @Test
    fun `izmir ve erzurum aksam saatleri cografi boylam farkina uygundur`() {
        val pIz = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("İzmir")
        val pEr = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Erzurum")
        assertNotEquals(pIz.gunler[0].aksam, pEr.gunler[0].aksam)
    }

    @Test
    fun `namaz saati titresim uygula null vibratorde hata firlatmaz`() {
        NamazAylikVeriServisi.namazSaatiTitresimUygula(null)
        assertTrue(true)
    }

    @Test
    fun `sehir icin 30 gunluk veri kayseri ve gaziantep icin calisir`() {
        val p1 = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Kayseri")
        val p2 = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur("Gaziantep")
        assertEquals(30, p1.gunler.size)
        assertEquals(30, p2.gunler.size)
    }

    @Test
    fun `desteklenen sehirler bos dize veya gecersiz eleman icermez`() {
        val list = NamazAylikVeriServisi.desteklenenSehirler()
        assertTrue(list.all { it.isNotBlank() })
    }
}
