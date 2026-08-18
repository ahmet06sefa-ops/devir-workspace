package com.gunlukasistan.app

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.59 — Ana Ekran Buton Açma/Kapama Karar Motoru ([AnaEkranButonKarari])
 * saf birim testleri (15 test).
 */
class AnaEkranButonTest {

    @Test
    fun `atolye goster kapaliyken sadece temel 2 buton id dondurur`() {
        val aktif = AnaEkranButonKarari.aktifButonIdleri(false)
        assertEquals(2, aktif.size)
        assertTrue(aktif.contains("openTimerMenu"))
        assertTrue(aktif.contains("openSettings"))
        assertFalse(aktif.contains("openManuelKontrol"))
    }

    @Test
    fun `atolye goster acikken 18 buton id dondurur`() {
        val aktif = AnaEkranButonKarari.aktifButonIdleri(true)
        assertEquals(22, aktif.size)
        assertTrue(aktif.contains("openTimerMenu"))
        assertTrue(aktif.contains("openManuelKontrol"))
        assertTrue(aktif.contains("openDersKolaylik"))
        assertTrue(aktif.contains("openDersIleriFaz"))
        assertTrue(aktif.contains("openDersUzmanMerkez"))
        assertTrue(aktif.contains("openYasamSaglikFinans"))
        assertTrue(aktif.contains("openDersUzmanFaz6"))
        assertTrue(aktif.contains("openYasamSaglikFinansFaz3"))
        assertTrue(aktif.contains("openBinMaddeAtolye"))
        assertTrue(aktif.contains("openGorunumAtolye"))
        assertTrue(aktif.contains("openKisiselGelisimAtolye"))
        assertTrue(aktif.contains("openCanvaAtolye"))
        assertTrue(aktif.contains("openEvrenselOtonomMerkez"))
        assertTrue(aktif.contains("openAkilliGundemMerkezi"))
        assertTrue(aktif.contains("openNamazAylikYonetim"))
    }

    @Test
    fun `durum metni acik ve kapali duruma gore dogru uretir`() {
        assertTrue("AÇIK" in AnaEkranButonKarari.durumMetniGetir(true))
        assertTrue("KAPALI" in AnaEkranButonKarari.durumMetniGetir(false))
        assertTrue("Orijinal Sade" in AnaEkranButonKarari.durumMetniGetir(false))
    }

    @Test
    fun `alt metin acik ve kapali duruma gore bilgi icerir`() {
        assertTrue("10 ikon" in AnaEkranButonKarari.altMetinGetir(true))
        assertTrue("2 ikon" in AnaEkranButonKarari.altMetinGetir(false))
    }

    @Test
    fun `buton gorunurluk karari acikka visible kapaliyken gone dondurur`() {
        assertEquals(View.VISIBLE, AnaEkranButonKarari.butonGorunurlukKarari(true))
        assertEquals(View.GONE, AnaEkranButonKarari.butonGorunurlukKarari(false))
    }

    @Test
    fun `atolye buton id listesi 16 adet ozel modul butonunu icerir`() {
        val list = AnaEkranButonKarari.ATOLYE_BUTON_IDLERI
        assertEquals(20, list.size)
        assertTrue(list.contains("openTasarimAtolye"))
        assertTrue(list.contains("openKarne"))
        assertTrue(list.contains("openYasamModulleri"))
        assertTrue(list.contains("openGelismiAtolye"))
        assertTrue(list.contains("openUzmanModuller"))
        assertTrue(list.contains("openDersKolaylik"))
        assertTrue(list.contains("openDersIleriFaz"))
        assertTrue(list.contains("openBinMaddeAtolye"))
        assertTrue(list.contains("openGorunumAtolye"))
        assertTrue(list.contains("openKisiselGelisimAtolye"))
        assertTrue(list.contains("openCanvaAtolye"))
        assertTrue(list.contains("openDersUzmanMerkez"))
        assertTrue(list.contains("openYasamSaglikFinans"))
        assertTrue(list.contains("openDersUzmanFaz6"))
        assertTrue(list.contains("openYasamSaglikFinansFaz3"))
        assertTrue(list.contains("openEvrenselOtonomMerkez"))
        assertTrue(list.contains("openAkilliGundemMerkezi"))
        assertTrue(list.contains("openNamazAylikYonetim"))
    }
}
