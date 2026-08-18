package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.54 — Sesli "Gündem & Vakit Brifingi" ([SesliBrifing]) + Akıllı "Odak & Verimlilik Karnesi" ([VerimlilikKarnesi])
 * saf birim testleri (16 test).
 */
class KarneTest {

    @Test
    fun `brifing metni uret sabah durumunu dogru formatlar`() {
        val metin = SesliBrifing.brifingMetniUret(
            vakitAd = "Öğle",
            kalanGorevSayisi = 5,
            odakDk = 0,
            seri = 12,
            sabahMi = true
        )
        assertTrue("Günaydın!" in metin)
        assertTrue("Öğle" in metin)
        assertTrue("12 günlük aktif seriniz" in metin)
    }

    @Test
    fun `brifing metni uret sabah serisiz durumunda seri eklemez`() {
        val metin = SesliBrifing.brifingMetniUret(
            vakitAd = "İkindi",
            kalanGorevSayisi = 2,
            odakDk = 0,
            seri = 0,
            sabahMi = true
        )
        assertTrue("Günaydın!" in metin)
        assertTrue("seriniz" notInMetin metin)
    }

    @Test
    fun `brifing metni uret gun ici ozetini dogru formatlar`() {
        val metin = SesliBrifing.brifingMetniUret(
            vakitAd = "Akşam",
            kalanGorevSayisi = 1,
            odakDk = 120,
            seri = 5,
            sabahMi = false
        )
        assertTrue("Günün özeti:" in metin)
        assertTrue("120 dakika" in metin)
    }

    @Test
    fun `brifing basligi sabaha gore gunaydin dondurur`() {
        assertEquals("🌅 Sabah Günaydın & Vakit Brifingi", SesliBrifing.brifingBasligi(true))
    }

    @Test
    fun `brifing basligi gun icine gore odak ozeti dondurur`() {
        assertEquals("☀️ Gün İçi Odak & Gündem Özeti", SesliBrifing.brifingBasligi(false))
    }

    @Test
    fun `ozet skor hesapla sifir gorev ve tam odakta yuz puan dondurur`() {
        assertEquals(100, SesliBrifing.ozetSkorHesapla(kalanGorev = 0, odakDk = 100, hedefDk = 100))
    }

    @Test
    fun `ozet skor hesapla sifir gorev ve sifir odakta 30 puan dondurur`() {
        assertEquals(30, SesliBrifing.ozetSkorHesapla(kalanGorev = 0, odakDk = 0, hedefDk = 100))
    }

    @Test
    fun `karne analiz et yuksek odak ve 6 aktif gunde a arti notu dondurur`() {
        val odakList = listOf(120, 100, 150, 90, 80, 70, 0) // 6 gün >= 25, toplam 610
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertEquals("A+", ozet.haftalikNot)
    }

    @Test
    fun `karne analiz et iyi odakta a notu dondurur`() {
        val odakList = listOf(80, 80, 80, 80, 80, 0, 0) // 5 gün >= 25, toplam 400
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertEquals("A", ozet.haftalikNot)
    }

    @Test
    fun `karne analiz et orta odakta b notu dondurur`() {
        val odakList = listOf(70, 70, 70, 0, 0, 0, 0) // 3 gün >= 25, toplam 210
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertEquals("B", ozet.haftalikNot)
    }

    @Test
    fun `karne analiz et az odakta c notu dondurur`() {
        val odakList = listOf(60, 0, 0, 0, 0, 0, 0) // 1 gün >= 25, toplam 60
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertEquals("C", ozet.haftalikNot)
    }

    @Test
    fun `karne analiz et hic odak yoksa d notu dondurur`() {
        val ozet = VerimlilikKarnesi.karneAnalizEt(emptyList(), emptyList(), emptyList())
        assertEquals("D", ozet.haftalikNot)
    }

    @Test
    fun `karne analiz et en verimli gun indeksini dogru secer`() {
        val odakList = listOf(0, 0, 200, 0, 0, 0, 0) // Çarşamba en yüksek
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertEquals("Çarşamba", ozet.enVerimliGunAd)
    }

    @Test
    fun `karne analiz et cok kesinti varsa kalkan tavsiyesi verir`() {
        val odakList = listOf(30, 30, 0, 0, 0, 0, 0)
        val kesintiler = listOf(4, 4, 3, 0, 0, 0, 0) // toplam 11 kesinti
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), kesintiler)
        assertTrue("Odak Kalkanı'nı" in ozet.kocTavsiyesi)
    }

    @Test
    fun `karne analiz et a notunda tebrik tavsiyesi verir`() {
        val odakList = listOf(100, 100, 100, 100, 100, 0, 0)
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertTrue("Harika bir hafta!" in ozet.kocTavsiyesi)
    }

    @Test
    fun `karne analiz et b notunda yayma tavsiyesi verir`() {
        val odakList = listOf(70, 70, 70, 0, 0, 0, 0)
        val ozet = VerimlilikKarnesi.karneAnalizEt(odakList, emptyList(), emptyList())
        assertTrue("seviyesine çıkabilirsiniz" in ozet.kocTavsiyesi)
    }

    private infix fun String.notInMetin(metin: String): Boolean = !metin.contains(this)
}
