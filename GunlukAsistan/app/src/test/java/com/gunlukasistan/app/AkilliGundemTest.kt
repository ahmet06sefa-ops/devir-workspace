package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.69 — Akıllı Gündem, Biyo-Ritim Brifingi & Otonom Asistan Merkezi ([AkilliGundemVeAsistanMerkezi])
 * saf birim testleri (26 test).
 */
class AkilliGundemTest {

    // ── 1. BRİFİNG MOTORU (1..4) ──
    @Test
    fun `sabah brifingi ahmet selamlamasi ve sabah tavsiyesi dondurur`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet")
        assertEquals("SABAH", b.vakitTuru)
        assertTrue(b.selamMetni.contains("Günaydın Ahmet"))
        assertTrue(b.bilesikTavsiye.contains("kurbağa") || b.bilesikTavsiye.contains("zinde"))
    }

    @Test
    fun `aksam brifingi ahmet selamlamasi ve kafein tavsiyesi dondurur`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("AKSAM", "Ahmet")
        assertEquals("AKSAM", b.vakitTuru)
        assertTrue(b.selamMetni.contains("İyi akşamlar Ahmet"))
        assertTrue(b.bilesikTavsiye.contains("kafein"))
    }

    @Test
    fun `brifing formatlama kilit gorevleri maddedik dizeye cevirir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet")
        val str = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingMetniFormatla(b)
        assertTrue(str.contains("SABAH BRİFİNGİ"))
        assertTrue(str.contains("•"))
    }

    // ── 2. 24-SAATLİK BİYO-VAKİT ORKESTRASYONU (5..8) ──
    @Test
    fun `varsayilan 24 saat plani 7 adet biyo blok listeler`() {
        val list = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.varsayilan24SaatPlan()
        assertEquals(7, list.size)
    }

    @Test
    fun `su anki blok bulma 7 saati icin sabah zinde odagi dondurur`() {
        val b = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.suAnkiBlokuBul(7)
        assertTrue(b.blokAdi.contains("Sabah"))
        assertEquals(40, b.idealFrekansHz)
    }

    @Test
    fun `su anki blok bulma 14 saati icin ogleden sonra pratigi dondurur`() {
        val b = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.suAnkiBlokuBul(14)
        assertTrue(b.blokAdi.contains("Öğleden Sonra"))
    }

    @Test
    fun `su anki blok bulma 23 saati icin gece rem uyku blokunu dondurur`() {
        val b = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.suAnkiBlokuBul(23)
        assertTrue(b.blokAdi.contains("REM"))
        assertEquals(4, b.idealFrekansHz)
    }

    // ── 3. BUGÜN NE YAPMALIYIM ASİSTANI (9..12) ──
    @Test
    fun `anlik oneri uretme yorgunluk durumunda nefes egzersizi onerir`() {
        val o = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(10, true)
        assertTrue(o.baslik.contains("4-7-8"))
        assertEquals(15, o.xpOdulu)
    }

    @Test
    fun `anlik oneri uretme sabah saatinde leitner flas kart onerir`() {
        val o = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(9, false)
        assertTrue(o.baslik.contains("Leitner"))
        assertEquals(25, o.xpOdulu)
    }

    @Test
    fun `anlik oneri uretme ogle saatinde 45s turlama denemesi onerir`() {
        val o = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(14, false)
        assertTrue(o.baslik.contains("Turlama"))
        assertEquals(30, o.xpOdulu)
    }

    @Test
    fun `anlik oneri uretme aksam saatinde yanlis soru kes yapistir onerir`() {
        val o = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(20, false)
        assertTrue(o.baslik.contains("Yanlış"))
        assertEquals(20, o.xpOdulu)
    }

    // ── 4. AKILLI DND & ŞİMDİ DEĞİL (13..15) ──
    @Test
    fun `dnd durumu aktif sayacta true ve dnd acik mesaji dondurur`() {
        val dnd = AkilliGundemVeAsistanMerkezi.AkilliDndOtomasyonu.dndDurumuGetir(true)
        assertTrue(dnd.first)
        assertTrue(dnd.second.contains("DND"))
    }

    @Test
    fun `dnd durumu pasifte false ve standart bildirim dondurur`() {
        val dnd = AkilliGundemVeAsistanMerkezi.AkilliDndOtomasyonu.dndDurumuGetir(false)
        assertFalse(dnd.first)
        assertTrue(dnd.second.contains("Standart"))
    }

    @Test
    fun `simdi degil kutusu mesaji kilitlenen fikri kaydeder`() {
        val msg = AkilliGundemVeAsistanMerkezi.AkilliDndOtomasyonu.simdiDegilKutusunaEkle("Ahmet'i ara")
        assertTrue(msg.contains("Ahmet'i ara"))
        assertTrue(msg.contains("Kilitle"))
    }

    // ── 5. HAFTALIK BÜTÜNCÜL RAPOR (16..18) ──
    @Test
    fun `haftalik butuncul rapor olusturma a plus ve 92 saglik skoru icerir`() {
        val r = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.raporOlustur()
        assertEquals("A+", r.harfNotu)
        assertEquals(92, r.yasamSkoru)
    }

    @Test
    fun `ascii karne formatlama harf notu ve bakiye bilgisini basar`() {
        val r = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.raporOlustur()
        val str = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.asciiKarneFormatla(r)
        assertTrue(str.contains("A+"))
        assertTrue(str.contains("Bütçe İdeal"))
    }

    // ── 6. SOKRATİK KOÇ & YEDEK DOĞRULAYICI (19..26) ──
    @Test
    fun `sokratik koc calismak istemeyen sorguya kucuk adim tavsiyesi verir`() {
        val yanit = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl("canım çalışmak istemiyor")
        assertTrue(yanit.contains("5 dakika"))
    }

    @Test
    fun `sokratik koc net sorusuna yanlis analizi tavsiyesi verir`() {
        val yanit = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl("deneme netlerim artmıyor")
        assertTrue(yanit.contains("bilgi eksikliği"))
    }

    @Test
    fun `sokratik koc genel sorguya risksiz adim tavsiyesi verir`() {
        val yanit = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl("nereden başlayacağım")
        assertTrue(yanit.contains("en küçük"))
    }

    @Test
    fun `akilli yedek dogrulama true ve md5 denetim basarili mesaji dondurur`() {
        val res = AkilliGundemVeAsistanMerkezi.AkilliYedekDogrulayici.yedekSaglikTesti()
        assertTrue(res.first)
        assertTrue(res.second.contains("MD5"))
    }

    @Test
    fun `brifing formatlama aksam brifinginde aksam ibaresini icerir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("AKSAM", "Sefa")
        val str = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingMetniFormatla(b)
        assertTrue(str.contains("AKSAM BRİFİNGİ"))
    }

    @Test
    fun `biyo vakit blok 10 hz alfa frekansi ogle ibadeti ile uyumludur`() {
        val b = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.suAnkiBlokuBul(12)
        assertEquals(10, b.idealFrekansHz)
    }

    @Test
    fun `otonom oneri gerekcesi xp odulunu ve sureyi icerir`() {
        val o = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(9, false)
        assertTrue(o.sureDakika > 0)
        assertTrue(o.xpOdulu > 0)
    }

    @Test
    fun `haftalik butuncul rapor ders saatinin 28 oldugunu dogrular`() {
        val r = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.raporOlustur()
        assertEquals(28, r.dersSaat)
    }

    @Test
    fun `dnd otomasyonu bildirimlerin simdi degil kutusuna aktarildigini bildirir`() {
        val dnd = AkilliGundemVeAsistanMerkezi.AkilliDndOtomasyonu.dndDurumuGetir(true)
        assertTrue(dnd.second.contains("Şimdi Değil"))
    }

    @Test
    fun `sokratik koc sorgu basarisi turkce karakter destegine sahiptir`() {
        val yanit = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl("istemi")
        assertTrue(yanit.contains("Sokratik Koç"))
    }
}
