package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.61 — 7 Uzman Oyunlaştırma, Nefes, Zihni Boşaltma & Soru Kupa Alt-Modülü
 * ([DersUzmanFaz2]) saf birim testleri (30 test).
 */
class DersUzmanFaz2Test {

    // ── 1. SANAL KÜTÜPHANE MASASI TESTLERİ (1..3) ──
    @Test
    fun `sanal kutuphane arkadas davet etme sayiyi artirir ve 10 da sinirlar`() {
        var durum = DersUzmanFaz2.SanalMasaDurum(masaArkadasSayisi = 9)
        durum = DersUzmanFaz2.Uzman1_SanalKutuphane.arkadasDavetEt(durum)
        assertEquals(10, durum.masaArkadasSayisi)
        durum = DersUzmanFaz2.Uzman1_SanalKutuphane.arkadasDavetEt(durum)
        assertEquals(10, durum.masaArkadasSayisi)
    }

    @Test
    fun `sanal kutuphane metni getirme pofi durumunu ve kolektif saati basar`() {
        val durum = DersUzmanFaz2.SanalMasaDurum(pofiDurum = DersUzmanFaz2.PofiDurum.ODAK, masaArkadasSayisi = 4)
        val metin = DersUzmanFaz2.Uzman1_SanalKutuphane.sanalMasaMetniGetir(durum)
        assertTrue("Masada Kitap Okuyor" in metin)
        assertTrue("16 Saat" in metin)
    }

    @Test
    fun `sanal kutuphane bos arkadasta kolektif saati sifir basar`() {
        val durum = DersUzmanFaz2.SanalMasaDurum(masaArkadasSayisi = 0)
        val metin = DersUzmanFaz2.Uzman1_SanalKutuphane.sanalMasaMetniGetir(durum)
        assertTrue("0 Saat" in metin)
    }

    // ── 2. 4-7-8 NEFES MOTORU TESTLERİ (4..8) ──
    @Test
    fun `nefes adimi al 4s sonrasi tut 7s dondurur`() {
        assertEquals(DersUzmanFaz2.NefesAdim.TUT_7S, DersUzmanFaz2.Uzman2_478Nefes.sonrakiNefesAdimi(DersUzmanFaz2.NefesAdim.AL_4S))
    }

    @Test
    fun `nefes adimi tut 7s sonrasi ver 8s dondurur`() {
        assertEquals(DersUzmanFaz2.NefesAdim.VER_8S, DersUzmanFaz2.Uzman2_478Nefes.sonrakiNefesAdimi(DersUzmanFaz2.NefesAdim.TUT_7S))
    }

    @Test
    fun `nefes adimi ver 8s sonrasi al 4s dondurur`() {
        assertEquals(DersUzmanFaz2.NefesAdim.AL_4S, DersUzmanFaz2.Uzman2_478Nefes.sonrakiNefesAdimi(DersUzmanFaz2.NefesAdim.VER_8S))
    }

    @Test
    fun `nefes rehber metni adim basligini ve kaygi dusus yuzdesini basar`() {
        val metin = DersUzmanFaz2.Uzman2_478Nefes.nefesRehberMetni(DersUzmanFaz2.NefesAdim.AL_4S, 2)
        assertTrue("4s Nefes Al" in metin)
        assertTrue("%40 Kaygı Düşüşü" in metin)
    }

    @Test
    fun `nefes rehber metni 5 turda yuzde 100 kaygi dususu basar`() {
        val metin = DersUzmanFaz2.Uzman2_478Nefes.nefesRehberMetni(DersUzmanFaz2.NefesAdim.VER_8S, 5)
        assertTrue("%100 Kaygı Düşüşü" in metin)
    }

    // ── 3. GECE ZİHNİ BOŞALTMA & SABAH OLUMLAMALARI (9..11) ──
    @Test
    fun `zihni bosaltma not ekleme endise metnini ve sokratik cozumu uretir`() {
        val kayit = DersUzmanFaz2.Uzman3_ZihniBosaltma.zihniBosaltNotEkle("Tarih denemesinden endişeliyim")
        assertTrue("Tarih denemesinden endişeliyim" in kayit.endiseMetni)
        assertTrue("Sokratik Koç:" in kayit.sokratikCozum)
    }

    @Test
    fun `zihni bosaltma not ekleme bos metinde varsayilan sinav kaygisi basar`() {
        val kayit = DersUzmanFaz2.Uzman3_ZihniBosaltma.zihniBosaltNotEkle("")
        assertEquals("Sınav kaygısı", kayit.endiseMetni)
    }

    @Test
    fun `sabah olumlamasi getirme gecerli indeks dondurur`() {
        val olumlama = DersUzmanFaz2.Uzman3_ZihniBosaltma.sabahOlumlamasiGetir(0)
        assertTrue("Sabah Olumlaması" in olumlama)
    }

    // ── 4. KAFEİN REM PENCERESİ & BURNOUT FRENİ (12..15) ──
    @Test
    fun `kafein penceresi uyari saat 17 de rem uyarisi basar`() {
        val uyari = DersUzmanFaz2.Uzman4_KafeinVeBurnout.kafeinPenceresiUyari(18)
        assertTrue("KAFEİN REM UYARISI" in uyari)
    }

    @Test
    fun `kafein penceresi uyari saat 14 te uygun pencere basar`() {
        val uyari = DersUzmanFaz2.Uzman4_KafeinVeBurnout.kafeinPenceresiUyari(14)
        assertTrue("Kafein Penceresi Uygun" in uyari)
    }

    @Test
    fun `burnout fren denetimi 8 saatte asiri calisma uyarisi verir`() {
        val fren = DersUzmanFaz2.Uzman4_KafeinVeBurnout.burnoutFrenDenetimi(8.5f)
        assertTrue("AŞIRI ÇALIŞMA (BURNOUT) FRENİ" in fren)
    }

    @Test
    fun `burnout fren denetimi 5 saatte ideal ritm basar`() {
        val fren = DersUzmanFaz2.Uzman4_KafeinVeBurnout.burnoutFrenDenetimi(5.0f)
        assertTrue("İdeal Çalışma Ritm" in fren)
    }

    // ── 5. SORU KUPA ROZETLERİ & XP BARI (16..21) ──
    @Test
    fun `soru kupa durumu soru ekleme cozulen soru sayisini artirir`() {
        val durum = DersUzmanFaz2.SoruKupaDurum(cozulenSoru = 100, hedefSoru = 250)
        val yeni = DersUzmanFaz2.Uzman5_SoruKupasi.soruEkle(durum, 50)
        assertEquals(150, yeni.cozulenSoru)
    }

    @Test
    fun `aktif kupa getirme 500 soruda elmas dondurur`() {
        assertEquals(DersUzmanFaz2.KupaSeviye.ELMAS, DersUzmanFaz2.Uzman5_SoruKupasi.aktifKupaGetir(510))
    }

    @Test
    fun `aktif kupa getirme 250 soruda altin dondurur`() {
        assertEquals(DersUzmanFaz2.KupaSeviye.ALTIN, DersUzmanFaz2.Uzman5_SoruKupasi.aktifKupaGetir(260))
    }

    @Test
    fun `aktif kupa getirme 150 soruda gumus dondurur`() {
        assertEquals(DersUzmanFaz2.KupaSeviye.GUMUS, DersUzmanFaz2.Uzman5_SoruKupasi.aktifKupaGetir(160))
    }

    @Test
    fun `aktif kupa getirme 50 soruda bronz dondurur`() {
        assertEquals(DersUzmanFaz2.KupaSeviye.BRONZ, DersUzmanFaz2.Uzman5_SoruKupasi.aktifKupaGetir(60))
    }

    @Test
    fun `soru kupa ozeti soru sayisini yuzdeyi ve kupayi formatlar`() {
        val durum = DersUzmanFaz2.SoruKupaDurum(cozulenSoru = 125, hedefSoru = 250)
        val ozet = DersUzmanFaz2.Uzman5_SoruKupasi.soruKupaOzeti(durum)
        assertTrue("125/250 (%50)" in ozet)
        assertTrue("Bronz Kupa" in ozet)
    }

    // ── 6. KELİME ARAMA & ÖNKOŞUL REHBERİ (22..28) ──
    @Test
    fun `onkosul uyarisi matematik dersinde integral turev onkosulunu basar`() {
        val metin = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.onkosulUyarisi("Matematik")
        assertTrue("İntegral" in metin)
        assertTrue("Türev" in metin)
    }

    @Test
    fun `onkosul uyarisi tarih dersinde osmanli kronolojisini basar`() {
        val metin = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.onkosulUyarisi("Tarih")
        assertTrue("Osmanlı" in metin)
    }

    @Test
    fun `onkosul uyarisi turkce dersinde paragraf sozcuk anlami basar`() {
        val metin = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.onkosulUyarisi("Türkçe")
        assertTrue("Paragraf" in metin)
    }

    @Test
    fun `kelimeyle konu ara pofi kelimesini bulur`() {
        val bul = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.kelimeyleKonuAra("pofi")
        assertTrue("Sanal Kütüphane Masası" in bul)
    }

    @Test
    fun `kelimeyle konu ara nefes kelimesini bulur`() {
        val bul = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.kelimeyleKonuAra("nefes")
        assertTrue("4-7-8 Nefes" in bul)
    }

    @Test
    fun `kelimeyle konu ara dump kelimesini bulur`() {
        val bul = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.kelimeyleKonuAra("dump")
        assertTrue("Gece Zihni Boşaltma" in bul)
    }

    @Test
    fun `kelimeyle konu ara kafein kelimesini bulur`() {
        val bul = DersUzmanFaz2.Uzman6_KelimeAramaVeOnkosul.kelimeyleKonuAra("kafein")
        assertTrue("Kafein REM Penceresi" in bul)
    }

    // ── 7. PANO SENKRON JSON (29..30) ──
    @Test
    fun `pofi master json uretme ve cozumleme gecerli json paketi dondurur`() {
        val s = DersUzmanFaz2.SanalMasaDurum()
        val k = DersUzmanFaz2.SoruKupaDurum()
        val d = DersUzmanFaz2.BrainDumpKaydi()

        val json = DersUzmanFaz2.Uzman7_PanoSenkron.pofiMasterJsonUret(s, k, d)
        assertTrue(DersUzmanFaz2.Uzman7_PanoSenkron.pofiMasterJsonCoz(json))
    }

    @Test
    fun `pofi master json cozumleme null objede false dondurur`() {
        assertFalse(DersUzmanFaz2.Uzman7_PanoSenkron.pofiMasterJsonCoz(null))
    }
}
