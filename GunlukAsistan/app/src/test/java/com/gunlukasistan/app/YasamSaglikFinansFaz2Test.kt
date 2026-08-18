package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.65 — Yaşam Sağlığı & Finans — Uzman Faz 2 ([YasamSaglikFinansFaz2]) saf birim testleri (26 test).
 */
class YasamSaglikFinansFaz2Test {

    // ── 1. MEDİKAL & NEFES (1..5) ──
    @Test
    fun `nefes egzersizi kare modunda 4444 dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.nefesEgzersiziMetniGetir("KARE")
        assertTrue(res.first.contains("Kare Nefes"))
        assertTrue(res.second.contains("4-4-4-4") || res.second.contains("4s"))
    }

    @Test
    fun `nefes egzersizi 478 modunda sakinlestirici dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.nefesEgzersiziMetniGetir("478")
        assertTrue(res.first.contains("4-7-8"))
        assertTrue(res.second.contains("7s Tut"))
    }

    @Test
    fun `tansiyon ve seker degerlendirme ideal kayitta normal mesaj dondurur`() {
        val kayit = YasamSaglikFinansFaz2.TansiyonKaydi(120, 80, 95)
        val yorum = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.tansiyonVeSekerDegerlendir(kayit)
        assertTrue(yorum.first.contains("İdeal"))
        assertTrue(yorum.second.contains("İdeal"))
    }

    @Test
    fun `tansiyon ve seker degerlendirme yuksek tansiyonda uyari dondurur`() {
        val kayit = YasamSaglikFinansFaz2.TansiyonKaydi(150, 95, 90)
        val yorum = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.tansiyonVeSekerDegerlendir(kayit)
        assertTrue(yorum.first.contains("Yüksek Tansiyon"))
    }

    @Test
    fun `tansiyon ve seker degerlendirme yuksek sekerde uyari dondurur`() {
        val kayit = YasamSaglikFinansFaz2.TansiyonKaydi(120, 80, 160)
        val yorum = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.tansiyonVeSekerDegerlendir(kayit)
        assertTrue(yorum.second.contains("Yüksek Şeker"))
    }

    // ── 2. BESLENME & ORUÇ (6..9) ──
    @Test
    fun `toplam kalori hesaplama ideal kalori icin dengeli mesaj dondurur`() {
        val ogun = YasamSaglikFinansFaz2.OgunKalori(500, 600, 600, 200) // 1900 kcal
        val res = YasamSaglikFinansFaz2.Faz2_2_BeslenmeVeOruc.toplamKaloriHesapla(ogun, 2000)
        assertEquals(1900, res.first)
        assertTrue(res.second.contains("Dengeli Beslenme"))
    }

    @Test
    fun `toplam kalori hesaplama yuksek kalori icin asiri yukleme uyarisi dondurur`() {
        val ogun = YasamSaglikFinansFaz2.OgunKalori(800, 900, 800, 300) // 2800 kcal
        val res = YasamSaglikFinansFaz2.Faz2_2_BeslenmeVeOruc.toplamKaloriHesapla(ogun, 2000)
        assertTrue(res.second.contains("Aşırı Yükleme"))
    }

    @Test
    fun `aralikli oruc 16 8 saat hesaplama 20 den sonra ertesi gun 12 dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_2_BeslenmeVeOruc.aralikliOruc168Hesapla(20)
        assertEquals(12, res.first)
        assertTrue(res.second.contains("12:00"))
    }

    @Test
    fun `aralikli oruc 16 8 saat hesaplama 18 den sonra ertesi gun 10 dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_2_BeslenmeVeOruc.aralikliOruc168Hesapla(18)
        assertEquals(10, res.first)
        assertTrue(res.second.contains("10:00"))
    }

    // ── 3. BÜTÇE & BORÇ/ALACAK (10..13) ──
    @Test
    fun `harcama radar durumu limiti asinca 100 ve kritik uyari dondurur`() {
        val radar = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.harcamaRadarDurumu(500, 600)
        assertTrue(radar.first >= 100)
        assertTrue(radar.second.contains("KRİTİK"))
    }

    @Test
    fun `harcama radar durumu guvenli aralikta yesil mesaj dondurur`() {
        val radar = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.harcamaRadarDurumu(500, 200)
        assertTrue(radar.second.contains("GÜVENLİ"))
    }

    @Test
    fun `net alacak borc hesaplama alacak fazla ise pozitif bakiye dondurur`() {
        val list = listOf(
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Ahmet", 1000, true),
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Market", 300, false)
        )
        val res = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.netAlacakBorcHesapla(list)
        assertEquals(700, res.first)
        assertTrue(res.second.contains("Alacaklısınız"))
    }

    @Test
    fun `net alacak borc hesaplama borc fazla ise negatif bakiye dondurur`() {
        val list = listOf(
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Market", 500, false),
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Kira", 1000, false)
        )
        val res = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.netAlacakBorcHesapla(list)
        assertEquals(-1500, res.first)
        assertTrue(res.second.contains("Borçlusunuz"))
    }

    // ── 4. VARLIK & KUMBARA (14..16) ──
    @Test
    fun `kumbara ilerleme hesabi yuzde ve kalan tutar dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_4_VarlikVeKumbara.kumbaraIlerleme(10000, 7500)
        assertEquals(75, res.first)
        assertTrue(res.second.contains("2500 ₺"))
    }

    @Test
    fun `toplam portfoy degeri tl hesabi altin usd eur u dogru toplar`() {
        val portfoy = YasamSaglikFinansFaz2.PortfoyVarlik(
            altinGram = 2.0, // 2 * 3300 = 6600
            usdMiktar = 100.0, // 100 * 39.5 = 3950
            eurMiktar = 100.0, // 100 * 43.0 = 4300 -> toplam = 14850
            altinFiyatTl = 3300.0,
            usdFiyatTl = 39.5,
            eurFiyatTl = 43.0
        )
        val res = YasamSaglikFinansFaz2.Faz2_4_VarlikVeKumbara.toplamPortfoyDegeriTl(portfoy)
        assertEquals(14850.0, res.first, 0.01)
        assertTrue(res.second.contains("14850"))
    }

    @Test
    fun `varsayilan abonelik listesi 4 adet kalem icerir`() {
        val list = YasamSaglikFinansFaz2.Faz2_5_AbonelikTasarruf.varsayilanAbonelikler()
        assertEquals(4, list.size)
    }

    // ── 5. ABONELİK TASARRUF (17) ──
    @Test
    fun `yillik tasarruf simule etme iptal edilen kalemleri yillik carpar`() {
        val list = YasamSaglikFinansFaz2.Faz2_5_AbonelikTasarruf.varsayilanAbonelikler()
        // iptal edilenler: bulut(120) + spor(850) = 970 -> 970 * 12 = 11640
        val res = YasamSaglikFinansFaz2.Faz2_5_AbonelikTasarruf.yillikTasarrufSimuleEt(list)
        assertEquals(11640, res.first)
        assertTrue(res.second.contains("11640"))
    }

    // ── 6. AI & TTS (18..19) ──
    @Test
    fun `varsayilan ozel prompt stoaci ve sade dil icerir`() {
        val prompt = YasamSaglikFinansFaz2.Faz2_6_AiVeTts.varsayilanOzelPrompt()
        assertTrue(prompt.contains("Stoacı"))
    }

    @Test
    fun `tts ayar ozetleme hiz ve ton metnini formatlar`() {
        val ayar = YasamSaglikFinansFaz2.TtsAyar(1.25f, 1.10f)
        val str = YasamSaglikFinansFaz2.Faz2_6_AiVeTts.ttsAyarOzetle(ayar)
        assertTrue(str.contains("1.25x"))
        assertTrue(str.contains("1.10"))
    }

    // ── 7. ROZETLER & BİNAURAL FREKANS (20..26) ──
    @Test
    fun `varsayilan rozetler 4 adet basari karti listeler`() {
        val list = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.varsayilanRozetler()
        assertEquals(4, list.size)
    }

    @Test
    fun `binaural frekans aciklamasi 40 hz icin gamma dondurur`() {
        val desc = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.binauralFrekansAciklamasi(40)
        assertTrue(desc.contains("Gamma"))
    }

    @Test
    fun `binaural frekans aciklamasi 14 hz icin beta dondurur`() {
        val desc = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.binauralFrekansAciklamasi(14)
        assertTrue(desc.contains("Beta"))
    }

    @Test
    fun `binaural frekans aciklamasi 10 hz icin alpha dondurur`() {
        val desc = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.binauralFrekansAciklamasi(10)
        assertTrue(desc.contains("Alpha"))
    }

    @Test
    fun `binaural frekans aciklamasi 4 hz icin delta dondurur`() {
        val desc = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.binauralFrekansAciklamasi(4)
        assertTrue(desc.contains("Delta"))
    }

    @Test
    fun `cevrimdisi kasa kontrolu true ve yerel json mesaji dondurur`() {
        val res = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.cevrimdisiKasaKontrolu()
        assertTrue(res.first)
        assertTrue(res.second.contains("Çevrimdışı Kasa Doğrulandı"))
    }

    @Test
    fun `tansiyon seker hipoglisemi durumunda dusuk seker uyarisi dondurur`() {
        val kayit = YasamSaglikFinansFaz2.TansiyonKaydi(120, 80, 65)
        val yorum = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.tansiyonVeSekerDegerlendir(kayit)
        assertTrue(yorum.second.contains("Düşük Şeker"))
    }
}
