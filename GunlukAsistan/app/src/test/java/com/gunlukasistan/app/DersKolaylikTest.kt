package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.58 — 10 Uzman Öğrenme & Kullanım Kolaylığı Modülü ([DersKolaylikAtolye])
 * saf birim testleri (20 test).
 */
class DersKolaylikTest {

    @Test
    fun `modul 1 aralikli tekrar gunu 1 kutu icin gun 2 dondurur`() {
        val konu = DersKolaylikAtolye.TekrarKonusu(leitnerKutu = 1)
        val metin = DersKolaylikAtolye.Modul1_AralikliTekrar.sonrakiTekrarGunu(konu)
        assertTrue("Gün 2" in metin)
    }

    @Test
    fun `modul 1 kutu ilerleme kutu numarasini artirir ve 3 te sinirlar`() {
        var konu = DersKolaylikAtolye.TekrarKonusu(leitnerKutu = 2)
        konu = DersKolaylikAtolye.Modul1_AralikliTekrar.kutuIlerle(konu)
        assertEquals(3, konu.leitnerKutu)
        konu = DersKolaylikAtolye.Modul1_AralikliTekrar.kutuIlerle(konu)
        assertEquals(3, konu.leitnerKutu)
    }

    @Test
    fun `modul 2 deneme net hesaplama 48 dogru 12 yanlistan 45 net bulur`() {
        val net = DersKolaylikAtolye.Modul2_DenemeNet.netHesapla(48, 12)
        assertEquals(45.0f, net, 0.01f)
    }

    @Test
    fun `modul 2 soru basina saniye 60 soruyu 60 dakikada 60 sn bulur`() {
        val sn = DersKolaylikAtolye.Modul2_DenemeNet.soruBasinaSaniye(60, 60)
        assertEquals(60, sn)
    }

    @Test
    fun `modul 2 deneme ozeti metni sinav adini ve neti icerir`() {
        val sonuc = DersKolaylikAtolye.DenemeSonucu("KPSS Test", 48, 12, 60)
        val metin = DersKolaylikAtolye.Modul2_DenemeNet.denemeOzeti(sonuc)
        assertTrue("KPSS Test" in metin)
        assertTrue("45.00" in metin)
    }

    @Test
    fun `modul 3 hizli aksiyon masaya otur metni son konuyu basar`() {
        val metin = DersKolaylikAtolye.Modul3_HizliAksiyon.masayaOturKisaYolMetni("KPSS Tarih")
        assertTrue("KPSS Tarih" in metin)
        assertTrue("Masaya Oturuldu" in metin)
    }

    @Test
    fun `modul 4 bes dakika kural motivasyon metni anti erteleme icerir`() {
        val metin = DersKolaylikAtolye.Modul4_AntiErteleme.besDakikaMotivasyon()
        assertTrue("5 DAKİKA KURALI" in metin)
    }

    @Test
    fun `modul 4 kurbaga kart metni sabah zor konu onceligini gosterir`() {
        val kalkan = DersKolaylikAtolye.ErtelemeKalkani(sabahKurbagaKonu = "Matematik")
        val metin = DersKolaylikAtolye.Modul4_AntiErteleme.kurbagaKartMetni(kalkan)
        assertTrue("GÜNÜN KURBAĞASI" in metin)
        assertTrue("Matematik" in metin)
    }

    @Test
    fun `modul 5 hata kart metni soru ozeti ve ogrenilen notu basar`() {
        val kayit = DersKolaylikAtolye.HataKaydi("Tarih", "Lozan maddesi", "Komisyon kalkmadı")
        val metin = DersKolaylikAtolye.Modul5_HataDefteri.hataKartMetni(kayit)
        assertTrue("HATA DEFTERİ" in metin)
        assertTrue("Lozan" in metin)
        assertTrue("Komisyon kalkmadı" in metin)
    }

    @Test
    fun `modul 6 animedoro ve ultradian metinleri sureleri formatlar`() {
        val a = DersKolaylikAtolye.Modul6_SprintSablonlari.animedoroOzeti(40, 20)
        assertTrue("40m" in a)
        assertTrue("20m" in a)
        val u = DersKolaylikAtolye.Modul6_SprintSablonlari.ultradianOzeti(90, 20)
        assertTrue("90m" in u)
    }

    @Test
    fun `modul 7 sokratik ipucu dogrudan cevap yerine soru sorar`() {
        val ipucu = DersKolaylikAtolye.Modul7_SokratikIpucu.sokratikIpucuUret("Türev nedir?")
        assertTrue("SOKRATİK KOÇ" in ipucu)
        assertTrue("Türev nedir?" in ipucu)
    }

    @Test
    fun `modul 7 net tahminleyici ortalama ve trendi dogru hesaplar`() {
        val netler = listOf(40.0f, 45.0f, 50.0f) // ort: 45, son > ort -> +2.5 = 47.5
        val metin = DersKolaylikAtolye.Modul7_SokratikIpucu.netTahminEt(netler)
        assertTrue("47.5" in metin)
    }

    @Test
    fun `modul 8 sanal masa metni zincir sayisini ve pofiyi basar`() {
        val metin = DersKolaylikAtolye.Modul8_SanalMasa.pofiMasaMetni(14)
        assertTrue("14 Gün" in metin)
        assertTrue("SANAL KÜTÜPHANE" in metin)
    }

    @Test
    fun `modul 9 nefes rehber metni 4 7 8 kuralini acıklar`() {
        val metin = DersKolaylikAtolye.Modul9_NefesVeKahve.nefesRehberMetni()
        assertTrue("4-7-8 ANKSİYETE YATIŞTIRICI" in metin)
    }

    @Test
    fun `modul 9 kahve uyari metni saat 17 de uyari verir`() {
        val metin = DersKolaylikAtolye.Modul9_NefesVeKahve.kahveUyariMetni(18)
        assertTrue("KAFEİN UYARISI" in metin)
    }

    @Test
    fun `modul 10 altin formul getir tarih icin irak sinirini dondurur`() {
        val metin = DersKolaylikAtolye.Modul10_FormulVeCsv.altinFormulGetir("Tarih")
        assertTrue("Musul Sorunu" in metin)
    }

    @Test
    fun `modul 10 altin formul getir matematik icin pisagoru dondurur`() {
        val metin = DersKolaylikAtolye.Modul10_FormulVeCsv.altinFormulGetir("Matematik")
        assertTrue("Pisagor" in metin)
    }

    @Test
    fun `modul 10 deneme csv uret gecerli csv satiri basar`() {
        val sonuclar = listOf(DersKolaylikAtolye.DenemeSonucu("KPSS Test", 48, 12, 60))
        val csv = DersKolaylikAtolye.Modul10_FormulVeCsv.denemeCsvUret(sonuclar)
        assertTrue("Sinav,Dogru,Yanlis,Net,SureDk" in csv)
        assertTrue("KPSS Test,48,12,45.00,60" in csv)
    }
}
