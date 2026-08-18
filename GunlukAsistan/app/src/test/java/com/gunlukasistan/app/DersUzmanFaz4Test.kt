package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.63 — 7 Uzman Zihin Haritası, Mnemonic, Zirve Dağı, Renk Kodu, Peak Hours, Beslenme &
 * Çevrimdışı Kalkan ([DersUzmanFaz4]) saf birim testleri (25 test).
 */
class DersUzmanFaz4Test {

    // ── 1. ZİHİN HARİTASI & MNEMONIC TESTLERİ (1..6) ──
    @Test
    fun `zihin haritasi getirme matematik dersinde problem kollarini basar`() {
        val dugum = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.zihinHaritasiGetir("Matematik")
        assertTrue("Problemler" in dugum.anaKonu)
        assertTrue(dugum.altDallar.contains("Yaş Problemleri"))
    }

    @Test
    fun `zihin haritasi getirme turkce dersinde paragraf kollarini basar`() {
        val dugum = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.zihinHaritasiGetir("Türkçe")
        assertTrue("Paragrafta Yapı" in dugum.anaKonu)
        assertTrue(dugum.altDallar.contains("Ana Düşünce Cümlesi"))
    }

    @Test
    fun `zihin haritasi getirme tarih dersinde osmanli dagilma basar`() {
        val dugum = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.zihinHaritasiGetir("Tarih")
        assertTrue("Osmanlı Dağılma" in dugum.anaKonu)
        assertTrue(dugum.altDallar.contains("Tanzimat Fermanı (1839)"))
    }

    @Test
    fun `mnemonic uretme turkce dersinde sombahcem dondurur`() {
        val akrostis = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.mnemonicUret("Türkçe")
        assertTrue("SOMBAHÇEM" in akrostis)
    }

    @Test
    fun `mnemonic uretme matematik dersinde pasa cayi dondurur`() {
        val akrostis = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.mnemonicUret("Matematik")
        assertTrue("Paşa Çayı" in akrostis)
    }

    @Test
    fun `mnemonic uretme tarih dersinde fistikci sahap dondurur`() {
        val akrostis = DersUzmanFaz4.Faz4_1_ZihinVeMnemonic.mnemonicUret("Tarih")
        assertTrue("Fıstıkçı Şahap" in akrostis)
    }

    // ── 2. HEDEF BAROMETRESİ & OPTİK FORM TESTLERİ (7..11) ──
    @Test
    fun `puan farki hesaplama hedef ve mevcut farki dogru bulur`() {
        val b = DersUzmanFaz4.PuanBarometre(hedefPuan = 90, mevcutPuan = 78)
        assertEquals(12, DersUzmanFaz4.Faz4_2_HedefBarometre.puanFarkiHesapla(b))
    }

    @Test
    fun `puan farki hesaplama hedef mevcuttan kucukse sifir dondurur`() {
        val b = DersUzmanFaz4.PuanBarometre(hedefPuan = 80, mevcutPuan = 85)
        assertEquals(0, DersUzmanFaz4.Faz4_2_HedefBarometre.puanFarkiHesapla(b))
    }

    @Test
    fun `barometre metni eksik puanda hedef ve farki formatlar`() {
        val b = DersUzmanFaz4.PuanBarometre(hedefPuan = 90, mevcutPuan = 78)
        val metin = DersUzmanFaz4.Faz4_2_HedefBarometre.barometreMetni(b)
        assertTrue("+12 Puan Gerekli" in metin)
    }

    @Test
    fun `barometre metni hedefe ulasilinca tebrik mesaji basar`() {
        val b = DersUzmanFaz4.PuanBarometre(hedefPuan = 85, mevcutPuan = 88)
        val metin = DersUzmanFaz4.Faz4_2_HedefBarometre.barometreMetni(b)
        assertTrue("HEDEFE ULAŞTINIZ" in metin)
    }

    @Test
    fun `optik form uyarisi 10 dakika kalkanini aciklar`() {
        assertTrue("OPTİK FORM 10-DAKİKA KALKANI" in DersUzmanFaz4.Faz4_2_HedefBarometre.optikFormUyarisi())
    }

    // ── 3. ZİRVE DAĞI & ŞİMDİ DEĞİL KUTUSU (12..16) ──
    @Test
    fun `zirve dagi pomodoro ekleme sayiyi artirir ve hedefte sinirlar`() {
        var z = DersUzmanFaz4.ZirveDagi(tamamlananPomodoro = 7, hedefPomodoro = 8)
        z = DersUzmanFaz4.Faz4_3_ZirveDagi.pomodoroEkle(z)
        assertEquals(8, z.tamamlananPomodoro)
        z = DersUzmanFaz4.Faz4_3_ZirveDagi.pomodoroEkle(z)
        assertEquals(8, z.tamamlananPomodoro)
    }

    @Test
    fun `zirve metni getirme eksik pomodoro sayisini basar`() {
        val z = DersUzmanFaz4.ZirveDagi(tamamlananPomodoro = 5, hedefPomodoro = 8)
        val metin = DersUzmanFaz4.Faz4_3_ZirveDagi.zirveMetniGetir(z)
        assertTrue("5/8 Pomodoro" in metin)
        assertTrue("Zirveye 3 Adım Kaldı" in metin)
    }

    @Test
    fun `zirve metni getirme hedef tamamlaninca zirveye ulastiniz basar`() {
        val z = DersUzmanFaz4.ZirveDagi(tamamlananPomodoro = 8, hedefPomodoro = 8)
        val metin = DersUzmanFaz4.Faz4_3_ZirveDagi.zirveMetniGetir(z)
        assertTrue("ZİRVEYE ULAŞTINIZ" in metin)
    }

    @Test
    fun `simdi degil kutusu not ekleme mesaji kutuya kilitler`() {
        val log = DersUzmanFaz4.Faz4_3_ZirveDagi.simdiDegilKutusuNotEkle("Ahmet'i ara")
        assertTrue("'ŞİMDİ DEĞİL' KUTUSUNA ATILDI" in log)
        assertTrue("Ahmet'i ara" in log)
    }

    @Test
    fun `simdi degil kutusu bos mesaja varsayilan alakasiz dusunce yazar`() {
        val log = DersUzmanFaz4.Faz4_3_ZirveDagi.simdiDegilKutusuNotEkle("")
        assertTrue("Alakasız düşünce" in log)
    }

    // ── 4. RENK KODU & KAYNAK BİTİRME (17..20) ──
    @Test
    fun `renk kodu rehberi evrensel 4 renk standardini aciklar`() {
        val rehber = DersUzmanFaz4.Faz4_4_RenkKodu.renkKoduRehberi()
        assertTrue("Sarı = Tanım" in rehber)
        assertTrue("Yeşil = Formül" in rehber)
    }

    @Test
    fun `kaynak sayfa cozuldu ekleme toplam sayfayi asmaz`() {
        val k = DersUzmanFaz4.KaynakBitirme(cozumlenenSayfa = 280, toplamSayfa = 300)
        val yeni = DersUzmanFaz4.Faz4_4_RenkKodu.sayfaCozulduEkle(k, 50)
        assertEquals(300, yeni.cozumlenenSayfa)
    }

    @Test
    fun `kaynak yuzde metni cozumlenen ve yuzdeyi formatlar`() {
        val k = DersUzmanFaz4.KaynakBitirme("Tarih Soru Bankası", 195, 300) // %65
        val metin = DersUzmanFaz4.Faz4_4_RenkKodu.kaynakYuzdeMetni(k)
        assertTrue("Tarih Soru Bankası" in metin)
        assertTrue("%65" in metin)
        assertTrue("195/300 Sayfa" in metin)
    }

    @Test
    fun `kaynak yuzde metni sifir sayfalik kaynakta sifir basar`() {
        val k = DersUzmanFaz4.KaynakBitirme("Bos Kitap", 0, 0)
        val metin = DersUzmanFaz4.Faz4_4_RenkKodu.kaynakYuzdeMetni(k)
        assertTrue("%0" in metin)
    }

    // ── 5. PEAK HOURS & MOLA FRENİ (21..22) ──
    @Test
    fun `peak hours analizi en verimli sabah saatini basar`() {
        assertTrue("PEAK HOURS ANALİZİ" in DersUzmanFaz4.Faz4_5_PeakHours.peakHoursAnalizi())
    }

    @Test
    fun `mola fren metni 5 dakikalik sosyal medya uyarisi basar`() {
        assertTrue("MOLA İÇİ SOSYAL MEDYA FRENİ" in DersUzmanFaz4.Faz4_5_PeakHours.molaFrenMetni())
    }

    // ── 6. UYKU-BESLENME & ERGONOMİ (23..24) ──
    @Test
    fun `uyku beslenme rehberi rem uykusu ve protein kahvaltisi basar`() {
        val r = DersUzmanFaz4.Faz4_6_UykuBeslenme.uykuBeslenmeRehberi()
        assertTrue("SINAV GÜNÜ BİYOLOJİSİ" in r)
        assertTrue("protein kahvaltısı" in r)
    }

    @Test
    fun `salon ergonomi rehberi su icme ve omuz esnetme basar`() {
        val r = DersUzmanFaz4.Faz4_6_UykuBeslenme.salonErgonomiRehberi()
        assertTrue("SALON ERGONOMİSİ" in r)
        assertTrue("su için" in r)
    }

    // ── 7. ÇEVRİMDIŞI KALKAN (25) ──
    @Test
    fun `cevrimdisi garanti metni yerel calisma garantisi basar`() {
        val r = DersUzmanFaz4.Faz4_7_CevrimdisiKalkan.cevrimdisiGarantiMetni()
        assertTrue("%100 ÇEVRİMDIŞI ÇALIŞMA GARANTİSİ" in r)
    }
}
