package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.48 — Kullanıcı maddesi #10: [OtonomMotor] saf birim testleri (14 test).
 */
class OtonomTest {

    @Test
    fun `ajan komutu ayristir uyanma saatini bulur`() {
        val eylemler = OtonomMotor.ajanKomutuAyristir("Sabah uyanma saatimi 07:45 yap")
        val eylem = eylemler.find { it.tur == OtonomMotor.EylemTuru.UYKU_SAATI_GUNCELLE }
        assertTrue(eylem != null)
        assertEquals(7, eylem?.saat)
        assertEquals(45, eylem?.dakika)
    }

    @Test
    fun `ajan komutu ayristir sayac kurmayi bulur`() {
        val eylemler = OtonomMotor.ajanKomutuAyristir("Bana 40 dk odak sayacı kur")
        val eylem = eylemler.find { it.tur == OtonomMotor.EylemTuru.SAYAC_KUR }
        assertTrue(eylem != null)
        assertEquals(40, eylem?.sayacDk)
    }

    @Test
    fun `ajan komutu ayristir gorev eklemeyi bulur`() {
        val eylemler = OtonomMotor.ajanKomutuAyristir("görev: Raporu cuma gönder")
        val eylem = eylemler.find { it.tur == OtonomMotor.EylemTuru.GOREV_EKLE }
        assertTrue(eylem != null)
        assertEquals("Raporu cuma gönder", eylem?.metinParam)
    }

    @Test
    fun `ajan komutu ayristir hedef guncellemeyi bulur`() {
        val eylemler = OtonomMotor.ajanKomutuAyristir("hedefimi 60 dk yap")
        val eylem = eylemler.find { it.tur == OtonomMotor.EylemTuru.HEDEF_GUNCELLE }
        assertTrue(eylem != null)
        assertEquals(60, eylem?.sayacDk)
    }

    @Test
    fun `gundem orkestrasyonu az uykuda agir gorevi ogleden sonraya alir`() {
        val sonuc = OtonomMotor.gundemOrkestrasyonu(
            listOf("Fizik Soru Çöz"),
            uykuSuresiMs = 4 * 3600_000L,
            simdiSaat = 8
        )
        assertEquals(1, sonuc.size)
        assertEquals("14:30 - 16:00", sonuc[0].onerilenSaat)
    }

    @Test
    fun `gundem orkestrasyonu normal uykuda agir gorevi sabaha alir`() {
        val sonuc = OtonomMotor.gundemOrkestrasyonu(
            listOf("Matematik Proje Çalış"),
            uykuSuresiMs = 8 * 3600_000L,
            simdiSaat = 8
        )
        assertEquals(1, sonuc.size)
        assertEquals("09:30 - 11:30", sonuc[0].onerilenSaat)
    }

    @Test
    fun `gundem orkestrasyonu hafif rutin gorevi ara saate alir`() {
        val sonuc = OtonomMotor.gundemOrkestrasyonu(
            listOf("Market Alışverişi"),
            uykuSuresiMs = 8 * 3600_000L,
            simdiSaat = 8
        )
        assertEquals(1, sonuc.size)
        assertEquals("11:30 - 12:30", sonuc[0].onerilenSaat)
    }

    @Test
    fun `seri kurtarma analizi aksam saatinde riskli seriyi bulur`() {
        val oneriler = OtonomMotor.seriKurtarmaAnalizi(
            aliskanlikAdlari = listOf("Kitap Okuma"),
            tamamlananlar = listOf(false),
            seriler = listOf(5),
            simdiSaat = 21
        )
        assertEquals(1, oneriler.size)
        assertEquals("Kitap Okuma", oneriler[0].aliskanlikAd)
        assertEquals(5, oneriler[0].mevcutSeri)
    }

    @Test
    fun `seri kurtarma analizi gunduz saatinde bos dondurur`() {
        val oneriler = OtonomMotor.seriKurtarmaAnalizi(
            aliskanlikAdlari = listOf("Kitap Okuma"),
            tamamlananlar = listOf(false),
            seriler = listOf(5),
            simdiSaat = 14
        )
        assertTrue(oneriler.isEmpty())
    }

    @Test
    fun `seri kurtarma analizi tamamlanmis aliskanlikta bos dondurur`() {
        val oneriler = OtonomMotor.seriKurtarmaAnalizi(
            aliskanlikAdlari = listOf("Kitap Okuma"),
            tamamlananlar = listOf(true),
            seriler = listOf(5),
            simdiSaat = 21
        )
        assertTrue(oneriler.isEmpty())
    }

    @Test
    fun `notlardan gorev cikar todo ve yap satirlarini ayiklar`() {
        val notlar = listOf(
            "Toplantı notları:\nTODO: Faturaları öde\n[ ] Müşteriye mail at\nNormal paragraf açıklaması."
        )
        val gorevler = OtonomMotor.notlardanGorevCikar(notlar)
        assertEquals(2, gorevler.size)
        assertTrue("Faturaları öde" in gorevler)
        assertTrue("Müşteriye mail at" in gorevler)
    }

    @Test
    fun `notlardan gorev cikar normal paragrafi atlar`() {
        val notlar = listOf("Bugün hava çok güzeldi ve kitap okudum.")
        val gorevler = OtonomMotor.notlardanGorevCikar(notlar)
        assertTrue(gorevler.isEmpty())
    }

    @Test
    fun `otopilot hedef hesapla az uyku ve yogun gunde hedefini 65 yuzdeye indirir`() {
        val yeni = OtonomMotor.otopilotHedefHesapla(
            mevcutHedefDk = 100,
            uykuSuresiMs = 5 * 3600_000L,
            takvimYogunlukDk = 200
        )
        assertEquals(65, yeni)
    }

    @Test
    fun `otopilot hedef hesapla normal gunde hedefini korur`() {
        val yeni = OtonomMotor.otopilotHedefHesapla(
            mevcutHedefDk = 100,
            uykuSuresiMs = 8 * 3600_000L,
            takvimYogunlukDk = 60
        )
        assertEquals(100, yeni)
    }
}
