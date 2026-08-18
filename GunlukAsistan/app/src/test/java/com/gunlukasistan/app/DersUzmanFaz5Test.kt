package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.64 — 14 İleri Sınav Simülasyonu, Otonom Koçluk & Konu Denetim Merkezi
 * ([DersUzmanFaz5]) saf birim testleri (30 test).
 */
class DersUzmanFaz5Test {

    // ── 1. POMODORO İÇİ MİKRO-TEKRAR & HAFIZA ÇENGELİ (1..6) ──
    @Test
    fun `pomodoro mikro tekrar suresi 15 dk icin 2 dondurur`() {
        val dk = DersUzmanFaz5.Faz5_1_PomodoroCengel.mikroTekrarSuresiHesapla(15)
        assertEquals(2, dk)
    }

    @Test
    fun `pomodoro mikro tekrar suresi 25 dk icin 3 dondurur`() {
        val dk = DersUzmanFaz5.Faz5_1_PomodoroCengel.mikroTekrarSuresiHesapla(25)
        assertEquals(3, dk)
    }

    @Test
    fun `pomodoro mikro tekrar suresi 50 dk icin 5 dondurur`() {
        val dk = DersUzmanFaz5.Faz5_1_PomodoroCengel.mikroTekrarSuresiHesapla(50)
        assertEquals(5, dk)
    }

    @Test
    fun `varsayilan cengel sorulari en az 3 adet soru dondurur`() {
        val sorular = DersUzmanFaz5.Faz5_1_PomodoroCengel.varsayilanCengelSorulari()
        assertTrue(sorular.size >= 3)
        assertTrue(sorular.any { it.soruMetni.contains("özetle") })
    }

    @Test
    fun `cengel kontrolu kisa yanitta false ve uyari dondurur`() {
        val sonuc = DersUzmanFaz5.Faz5_1_PomodoroCengel.cengelKontroluTamamla(1, "kısa")
        assertFalse(sonuc.first)
        assertTrue(sonuc.second.contains("en az 5 kelimelik"))
    }

    @Test
    fun `cengel kontrolu uzun ve anlamli yanitta true ve xp dondurur`() {
        val sonuc = DersUzmanFaz5.Faz5_1_PomodoroCengel.cengelKontroluTamamla(1, "Bu seansın en önemli cümlesi bağımsızlık ideolojisidir")
        assertTrue(sonuc.first)
        assertTrue(sonuc.second.contains("+15 XP"))
    }

    // ── 2. HAFTALIK BİLİŞSEL KONSOLİDASYON (7..9) ──
    @Test
    fun `varsayilan haftalik konsolidasyon kayitlari bos degildir`() {
        val list = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.varsayilanHaftalikKayitlar()
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.dersAdi == "Tarih" })
    }

    @Test
    fun `konsolidasyon skoru hesaplama yuksek basarida 80 ustu dondurur`() {
        val list = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.varsayilanHaftalikKayitlar()
        val skor = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.konsolidasyonSkoruHesapla(list)
        assertTrue(skor.first >= 60)
        assertTrue(skor.second.contains("İyi düzeyde") || skor.second.contains("Mükemmel"))
    }

    @Test
    fun `konsolidasyon skoru hesaplama bos listede 0 dondurur`() {
        val skor = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.konsolidasyonSkoruHesapla(emptyList())
        assertEquals(0, skor.first)
    }

    // ── 3. ÖSYM ÇELDİRİCİ ŞIK DEFTERİ & MASA ÖNCESİ RİTÜEL (10..13) ──
    @Test
    fun `varsayilan celdirici listesi 3 adet kritik uyari barindirir`() {
        val list = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.varsayilanCeldiriciler()
        assertEquals(3, list.size)
        assertTrue(list.any { it.tuzakIfade.contains("Yalnız I") })
    }

    @Test
    fun `rituel adimlar 4 adet check list adimi listeler`() {
        val adimlar = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelAdimlar()
        assertEquals(4, adimlar.size)
        assertTrue(adimlar[0].contains("masasını", ignoreCase = true))
    }

    @Test
    fun `rituel durumu eksik adimda false dondurur`() {
        val durum = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelDurumuSorgula(2, 4)
        assertFalse(durum.first)
        assertTrue(durum.second.contains("2 adımı"))
    }

    @Test
    fun `rituel durumu tamamlanmis adimda true dondurur`() {
        val durum = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelDurumuSorgula(4, 4)
        assertTrue(durum.first)
        assertTrue(durum.second.contains("100% hazırsınız"))
    }

    // ── 4. KİŞİSEL MOTİVASYON ÇAPASI & ERTELEME SERİSİ (14..16) ──
    @Test
    fun `varsayilan motivasyon capasi hukuk hedefi icerir`() {
        val capa = DersUzmanFaz5.Faz5_4_MotivasyonVeErteleme.varsayilanCapa()
        assertTrue(capa.hedefBaslik.contains("Hukuk"))
        assertEquals(465, capa.hedefPuan)
    }

    @Test
    fun `erteleme analizi 2 gunde alt gorev dondurmez`() {
        val gorev = DersUzmanFaz5.ErtelenenGorev("Matematik Problemleri", 2)
        val analiz = DersUzmanFaz5.Faz5_4_MotivasyonVeErteleme.ertelemeAnalizi(gorev)
        assertFalse(analiz.first)
    }

    @Test
    fun `erteleme analizi 3 gunde 3 adim alt gorev dondurur`() {
        val gorev = DersUzmanFaz5.ErtelenenGorev("Fizik İtme ve Momentum", 3)
        val analiz = DersUzmanFaz5.Faz5_4_MotivasyonVeErteleme.ertelemeAnalizi(gorev)
        assertTrue(analiz.first)
        assertEquals(3, analiz.second.size)
    }

    // ── 5. AKILLI PDF TOC & YANLIŞ KES-YAPIŞTIR PANOSU (17..21) ──
    @Test
    fun `varsayilan toc listesi en az 5 bolum dondurur`() {
        val toc = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.varsayilanTocListesi()
        assertTrue(toc.size >= 5)
        assertEquals(1, toc[0].bolumNo)
    }

    @Test
    fun `sayfa atlama hesaplama ayni sayfada ilk sayfa mesaji dondurur`() {
        val msg = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.sayfaAtlamaHesapla(12, 12)
        assertTrue(msg.contains("zaten"))
    }

    @Test
    fun `sayfa atlama hesaplama ileri sayfalarda atlanan sayfa dondurur`() {
        val msg = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.sayfaAtlamaHesapla(10, 48)
        assertTrue(msg.contains("+38 sayfa"))
    }

    @Test
    fun `sayfa atlama hesaplama geri sayfalarda geriye donus mesaji dondurur`() {
        val msg = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.sayfaAtlamaHesapla(100, 48)
        assertTrue(msg.contains("Geriye doğru -52") || msg.contains("-52 sayfa"))
    }

    @Test
    fun `varsayilan yanlis panosu cozulmemis sorular icerir`() {
        val pan = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.varsayilanYanlisPanosu()
        assertEquals(3, pan.size)
        assertTrue(pan.any { !it.cozulduMu })
    }

    // ── 6. 50-10 MARATON SPRİNTİ & SERBEST SAYAC (22..25) ──
    @Test
    fun `maraton dayaniklilik puani saniye ve sayiya gore puan hesaplar`() {
        val puan = DersUzmanFaz5.Faz5_6_MaratonVeSerbestSayac.maratonDayaniklilikPuan(50, 2)
        assertEquals(150, puan)
    }

    @Test
    fun `masaya davet mesaji kalan saniye varsa geri sayim dondurur`() {
        val msg = DersUzmanFaz5.Faz5_6_MaratonVeSerbestSayac.masayaDavetMesaji(10)
        assertTrue(msg.contains("son 10 saniye"))
    }

    @Test
    fun `masaya davet mesaji saniye 0 ise zil mesaji dondurur`() {
        val msg = DersUzmanFaz5.Faz5_6_MaratonVeSerbestSayac.masayaDavetMesaji(0)
        assertTrue(msg.contains("Süre doldu"))
    }

    @Test
    fun `serbest sayac formatlama saniyeyi saat dakika saniye formatlar`() {
        val str = DersUzmanFaz5.Faz5_6_MaratonVeSerbestSayac.serbestSayacFormatla(3665)
        assertEquals("01:01:05", str)
    }

    // ── 7. AI KOÇ & OTOMATİK QUİZ (26..30) ──
    @Test
    fun `haftalik hedef durumu tamamlanmissa tebrik dondurur`() {
        val metre = DersUzmanFaz5.OdakHedefMetre(30, 31.0)
        val durum = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.haftalıkHedefDurumu(metre)
        assertEquals(100, durum.first)
        assertTrue(durum.second.contains("Tebrikler"))
    }

    @Test
    fun `ai eksik ders mufettisi dusuk oranli dersler icin uyari uretir`() {
        val map = mapOf("Matematik" to 180, "Tarih" to 10)
        val uyarilar = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.aiEksikDersMufettisi(map)
        assertTrue(uyarilar.any { it.contains("Tarih") })
    }

    @Test
    fun `quiz puanla 5 dogrudan 5 alinca 100 ve mukemmel mesaj dondurur`() {
        val (puan, msg) = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.quizPuanla(5, 5)
        assertEquals(100, puan)
        assertTrue(msg.contains("Mükemmel"))
    }

    @Test
    fun `ai koc yaniti sert modda sert mesaj dondurur`() {
        val yanit = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.aiKocYanitiAl("SERT", "uykum var")
        assertTrue(yanit.contains("Sert Öğretmen"))
        assertTrue(yanit.contains("Bahane yok"))
    }

    @Test
    fun `ai koc yaniti sefkatli modda sefkatli mesaj dondurur`() {
        val yanit = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.aiKocYanitiAl("SEFKATLI", "yoruldum")
        assertTrue(yanit.contains("Şefkatli Mentor"))
    }
}
