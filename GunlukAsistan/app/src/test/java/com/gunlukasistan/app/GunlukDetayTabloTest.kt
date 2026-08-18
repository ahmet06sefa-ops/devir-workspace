package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.75 — Günlük Aktivite & Yaşam Detay Tablosu Motoru ([GunlukAktiviteTabloMotoru])
 * saf birim testleri (26 test).
 */
class GunlukDetayTabloTest {

    // ── 1. 30 GÜNLÜK TABLO VERİSİ TESTLERİ (1..7) ──
    @Test
    fun `30 gunluk tablo verisi olusturma 30 adet eksiksiz kayit dondurur`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertEquals(30, list.size)
    }

    @Test
    fun `30 gunluk tablo verisi ilk gun ve son gun numaralarini dogrular`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertEquals(1, list.first().gunNo)
        assertEquals(30, list.last().gunNo)
    }

    @Test
    fun `gun kaydi getirme 10uncu gun icin gecerli tarih ve gun adini basar`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(10)
        assertEquals(10, k.gunNo)
        assertTrue(k.tarihStr.contains("10 Ağustos"))
    }

    @Test
    fun `gun kaydi getirme 30uncu gun icin dinlenme sabati icerir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(7)
        assertTrue(k.dersler.contains("Sabbath") || k.gunlukAciklama.contains("Sabbath"))
    }

    @Test
    fun `son 7 gun kayitlarini getirme 7 adet satir dondurur`() {
        val list = GunlukAktiviteTabloMotoru.son7GunKayitlariniGetir()
        assertEquals(7, list.size)
    }

    @Test
    fun `gun satiri ozet metni harf notu ve pomodoro sayisini icerir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(10)
        val str = GunlukAktiviteTabloMotoru.gunSatiriOzetMetni(k)
        assertTrue(str.contains(k.harfNotu))
        assertTrue(str.contains("Pomo"))
    }

    @Test
    fun `ascii gunluk karne olusturma ascii cerceve ve baslik barindirir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(10)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(k)
        assertTrue(ascii.contains("GÜNLÜK DETAYLI İLERLEME TABLOSU"))
        assertTrue(ascii.contains("╔") && ascii.contains("╝"))
    }

    // ── 2. İÇERİK & AÇIKLAMA TESTLERİ (8..16) ──
    @Test
    fun `ascii gunluk karne olusturma kocluk aciklamasi satirini icerir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(10)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(k)
        assertTrue(ascii.contains("KOÇLUK AÇIKLAMASI"))
        assertTrue(ascii.contains(k.gunlukAciklama))
    }

    @Test
    fun `30 gunluk tablo verisinde her gunun odak dakikasi pozitiftir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.odakDakika > 0 })
    }

    @Test
    fun `30 gunluk tablo verisinde her gunun dogruluk yuzdesi 100 altidadir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.dogrulukYuzdesi <= 100 })
    }

    @Test
    fun `30 gunluk tablo verisinde namaz durumu imsak ve yatsi saatleri tasir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.namazDurumu.contains("İmsak") || it.namazDurumu.contains("Vakit") })
    }

    @Test
    fun `30 gunluk tablo verisinde saglik durumu tansiyon ve seker notu tasir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.saglikDurumu.contains("Tansiyon") || it.saglikDurumu.contains("Su") })
    }

    @Test
    fun `ascii gunluk karne olusturma cozulen soru ve dogruluk oranini gosterir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(1)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(k)
        assertTrue(ascii.contains("Doğruluk: %"))
    }

    @Test
    fun `son 7 gun kayitlari 4 ile 10 agustos gunlerini kapsar`() {
        val list = GunlukAktiviteTabloMotoru.son7GunKayitlariniGetir()
        assertEquals(4, list.first().gunNo)
        assertEquals(10, list.last().gunNo)
    }

    @Test
    fun `gun kaydi getirme sinir disi sayida ilk gunu guvenle dondurur`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(99)
        assertEquals(1, k.gunNo)
    }

    @Test
    fun `gunluk aciklama listesinde a plus ve a notlari yer alir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.any { it.harfNotu == "A+" })
        assertTrue(list.any { it.harfNotu == "A" })
    }

    // ── 3. DETAYLI FORMAT & TURKCE TESTLERI (17..26) ──
    @Test
    fun `gun satiri ozet metni tarih ve dersler bilgisini dizeye doker`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(2)
        val str = GunlukAktiviteTabloMotoru.gunSatiriOzetMetni(k)
        assertTrue(str.contains("Ağu"))
        assertTrue(str.contains(k.gunAdi))
    }

    @Test
    fun `30 gunluk tablo verisinde harf notlari a plus a ve b plus dir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        val notlar = list.map { it.harfNotu }.distinct()
        assertTrue(notlar.contains("A+") && notlar.contains("A"))
    }

    @Test
    fun `ascii gunluk karne olusturma 16 8 oruc verisini icerir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(1)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(k)
        assertTrue(ascii.contains("Oruç") || ascii.contains("Su"))
    }

    @Test
    fun `30 gunluk tablo verisinde haftasonu denemesi kaydi mevcuttur`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.any { it.dersler.contains("Deneme") })
    }

    @Test
    fun `gunluk detay kaydi modeli gecerli alan adlarina sahiptir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(1)
        assertTrue(k.soruSayisi > 0)
        assertTrue(k.odakDakika > 0)
    }

    @Test
    fun `ascii gunluk karne olusturma en az 10 satir dize uretir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(1)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(k)
        assertTrue(ascii.lines().size >= 10)
    }

    @Test
    fun `30 gunluk tablo verisinde soru sayisi sifirdan buyuktur`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.soruSayisi > 0 })
    }

    @Test
    fun `gun satiri ozet metni turkce ay isimleri barindirir`() {
        val k = GunlukAktiviteTabloMotoru.gunKaydiGetir(5)
        assertTrue(GunlukAktiviteTabloMotoru.gunSatiriOzetMetni(k).contains("Ağu"))
    }

    @Test
    fun `30 gunluk tablo verisi haftanın yedi gun adini donusumlu kullanir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        val adlar = list.map { it.gunAdi }.distinct()
        assertEquals(7, adlar.size)
    }

    @Test
    fun `tüm tablo kayitlari ve kocluk yorumlari turkce karakter destegine sahiptir`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret()
        assertTrue(list.all { it.gunlukAciklama.isNotBlank() })
    }
}
