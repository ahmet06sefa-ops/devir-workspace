package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.68 — Evrensel Otonom Yönetim & 200-Madde Kontrol Merkezi ([EvrenselOtonomMerkez])
 * saf birim testleri (26 test).
 */
class EvrenselOtonomMerkezTest {

    // ── 1. EVRENSEL 200-MADDE İNDEKS & ARAMA (1..4) ──
    @Test
    fun `varsayilan 200 indeks en az 15 adet evrensel madde icerir`() {
        val list = EvrenselOtonomMerkez.EvrenselAramaMotoru.varsayilan200Indeks()
        assertTrue(list.size >= 15)
        assertTrue(list.any { it.katalog == "YASAM" })
        assertTrue(list.any { it.katalog == "DERS" })
    }

    @Test
    fun `evrensel arama motoru tansiyon yazinca yasam katalogunu bulur`() {
        val res = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra("tansiyon")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.katalog == "YASAM" && it.anahtarKelime == "TANSIYON" })
    }

    @Test
    fun `evrensel arama motoru pomodoro yazinca ders katalogunu bulur`() {
        val res = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra("pomodoro")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.katalog == "DERS" })
    }

    @Test
    fun `evrensel arama motoru bos sorguda bos liste dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra("")
        assertTrue(res.isEmpty())
    }

    // ── 2. YAŞAM-DERS BÜTÜNCÜL DENGE ENDEKSİ (5..7) ──
    @Test
    fun `butuncul denge endeksi 90 85 skorunda mukemmel denge dondurur`() {
        val skor = EvrenselOtonomMerkez.YasamDersSkor(90, 85)
        val res = EvrenselOtonomMerkez.YasamDersDengeleyici.butunculDengeEndeksiHesapla(skor)
        assertEquals(87, res.first)
        assertTrue(res.second.contains("Mükemmel Denge"))
    }

    @Test
    fun `butuncul denge endeksi 40 85 skorunda dengesizlik uyarisi dondurur`() {
        val skor = EvrenselOtonomMerkez.YasamDersSkor(40, 85)
        val res = EvrenselOtonomMerkez.YasamDersDengeleyici.butunculDengeEndeksiHesapla(skor)
        assertTrue(res.second.contains("Dengesizlik Uyarısı"))
    }

    @Test
    fun `butuncul denge endeksi 65 65 skorunda dengeli seviye dondurur`() {
        val skor = EvrenselOtonomMerkez.YasamDersSkor(65, 65)
        val res = EvrenselOtonomMerkez.YasamDersDengeleyici.butunculDengeEndeksiHesapla(skor)
        assertEquals(65, res.first)
        assertTrue(res.second.contains("Dengeli Seviye"))
    }

    // ── 3. MANUEL OTONOMİ OVERRIDE KORUMASI (8..10) ──
    @Test
    fun `otonomi aciklamasi otopilot icin tam otopilot ai dondurur`() {
        val res = EvrenselOtonomMerkez.OtonomiSeviyesiKalkani.otonomiAciklamasiGetir("OTOPILOT")
        assertTrue(res.first.contains("Otopilot AI"))
    }

    @Test
    fun `otonomi aciklamasi yari icin yari otonom dondurur`() {
        val res = EvrenselOtonomMerkez.OtonomiSeviyesiKalkani.otonomiAciklamasiGetir("YARI")
        assertTrue(res.first.contains("Yarı-Otonom"))
    }

    @Test
    fun `otonomi aciklamasi manuel icin 100 manuel kontrol dondurur`() {
        val res = EvrenselOtonomMerkez.OtonomiSeviyesiKalkani.otonomiAciklamasiGetir("MANUEL")
        assertTrue(res.first.contains("100% Manuel Kontrol"))
    }

    // ── 4. ÇEVRİMDIŞI KASA & JSON (11..12) ──
    @Test
    fun `cevrimdisi arsiv dogrulama true ve 200 madde sifreli kasa dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselCevrimdisiKasa.cevrimdisiArsivDogrula()
        assertTrue(res.first)
        assertTrue(res.second.contains("200 maddelik") || res.second.contains("Çevrimdışı"))
    }

    @Test
    fun `evrensel ozet json uretme endeks ve otonomi bilgisini json formata dondurur`() {
        val json = EvrenselOtonomMerkez.EvrenselCevrimdisiKasa.evrenselOzetJsonUret(88, "MANUEL")
        assertTrue(json.contains("88"))
        assertTrue(json.contains("MANUEL"))
        assertTrue(json.contains("v10.68"))
    }

    // ── 5. USTALIK RÜTBESİ (13..15) ──
    @Test
    fun `ustalik rutbesi 150 ustu madde icin 200 madde ustadi dondurur`() {
        val r = EvrenselOtonomMerkez.EvrenselUstalikRutbesi.ustalikRutbesiHesapla(160)
        assertEquals("200-Madde Üstadı", r.unvan)
        assertEquals(500, r.xpBonus)
    }

    @Test
    fun `ustalik rutbesi 75 madde icin evrensel usta dondurur`() {
        val r = EvrenselOtonomMerkez.EvrenselUstalikRutbesi.ustalikRutbesiHesapla(80)
        assertEquals("Evrensel Usta", r.unvan)
        assertEquals(250, r.xpBonus)
    }

    @Test
    fun `ustalik rutbesi 10 madde icin evrensel cirak dondurur`() {
        val r = EvrenselOtonomMerkez.EvrenselUstalikRutbesi.ustalikRutbesiHesapla(10)
        assertEquals("Evrensel Çırak", r.unvan)
        assertEquals(100, r.xpBonus)
    }

    // ── 6. EVRENSEL HIZLI KOMUT PALETİ (16..22) ──
    @Test
    fun `varsayilan komutlar en az 5 kilit evrensel komut listeler`() {
        val list = EvrenselOtonomMerkez.EvrenselHizliKomut.varsayilanKomutlar()
        assertTrue(list.size >= 5)
    }

    @Test
    fun `komut calistirma cmd sos icin true ve kopyalandi mesaji dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_SOS")
        assertTrue(res.first)
        assertTrue(res.second.contains("SOS"))
    }

    @Test
    fun `komut calistirma cmd nefes icin true ve 4 7 8 mesaji dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_NEFES")
        assertTrue(res.first)
        assertTrue(res.second.contains("4s al, 7s tut"))
    }

    @Test
    fun `komut calistirma cmd turlama icin true ve 45s mesaji dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_TURLAMA")
        assertTrue(res.first)
        assertTrue(res.second.contains("İlk tur"))
    }

    @Test
    fun `komut calistirma cmd oruc icin true ve 16 8 mesaji dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_ORUC")
        assertTrue(res.first)
        assertTrue(res.second.contains("20:00 - 12:00"))
    }

    @Test
    fun `komut calistirma cmd canavar icin true ve 100 xp mesaji dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_CANAVAR")
        assertTrue(res.first)
        assertTrue(res.second.contains("+100 XP"))
    }

    @Test
    fun `komut calistirma bilinmeyen komut icin false ve hata dondurur`() {
        val res = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir("CMD_BILINMEYEN")
        assertFalse(res.first)
        assertTrue(res.second.contains("Bilinmeyen komut"))
    }

    // ── 7. SİSTEM SAĞLIĞI & ARAMA EK TESTLERİ (23..26) ──
    @Test
    fun `evrensel saglik raporu android sdk 34 ve 0 crash bilgisi icerir`() {
        val rapor = EvrenselOtonomMerkez.EvrenselSistemDenetci.evrenselSaglikRaporuGetir()
        assertTrue(rapor.contains("SDK 34"))
        assertTrue(rapor.contains("0 Crash"))
    }

    @Test
    fun `evrensel arama motoru sos kelimesini arayinca sos mesaj hazirlayiciyi bulur`() {
        val res = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra("SOS")
        assertTrue(res.any { it.anahtarKelime == "SOS" })
    }

    @Test
    fun `evrensel arama motoru leitner kelimesini arayinca leitner kutu kartlarini bulur`() {
        val res = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra("leitner")
        assertTrue(res.any { it.anahtarKelime == "LEITNER" })
    }

    @Test
    fun `evrensel ozet json uretme v10 68 ve aes kalkan bilgisini icerir`() {
        val json = EvrenselOtonomMerkez.EvrenselCevrimdisiKasa.evrenselOzetJsonUret(75, "YARI")
        assertTrue(json.contains("EvrenselOtonomMerkez"))
        assertTrue(json.contains("100%_yerel_aes"))
    }
}
