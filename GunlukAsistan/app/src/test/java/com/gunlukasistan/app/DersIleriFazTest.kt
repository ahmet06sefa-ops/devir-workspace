package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.60 — 7 Uzman Bilişsel Öğrenme & Sınav Net Alt-Modülü ([DersIleriFaz])
 * saf birim testleri (26 test).
 */
class DersIleriFazTest {

    // ── LEİTNER KUTU & ARALIKLI TEKRAR TESTLERİ (1..7) ──
    @Test
    fun `modul 1 leitner destesi dogru bilince kutuyu ilerletir`() {
        val kart = DersIleriFaz.LeitnerKart("1", "Soru", "Cevap", kutuNo = 1)
        val yeni = DersIleriFaz.Ileri1_LeitnerMotoru.kartDogruBildim(kart)
        assertEquals(2, yeni.kutuNo)
    }

    @Test
    fun `modul 1 leitner destesi yanlis bilince kutu 1 e geri indirir`() {
        val kart = DersIleriFaz.LeitnerKart("1", "Soru", "Cevap", kutuNo = 3)
        val yeni = DersIleriFaz.Ileri1_LeitnerMotoru.kartYanlisBildim(kart)
        assertEquals(1, yeni.kutuNo)
    }

    @Test
    fun `modul 1 leitner destesi kutu 3 un uzerine cikmaz`() {
        val kart = DersIleriFaz.LeitnerKart("1", "Soru", "Cevap", kutuNo = 3)
        val yeni = DersIleriFaz.Ileri1_LeitnerMotoru.kartDogruBildim(kart)
        assertEquals(3, yeni.kutuNo)
    }

    @Test
    fun `modul 1 kutu dagilim ozeti sayilari dogru gosterir`() {
        val kartlar = DersIleriFaz.Ileri1_LeitnerMotoru.varsayilanDeste()
        val ozet = DersIleriFaz.Ileri1_LeitnerMotoru.kutuDagilimOzeti(kartlar)
        assertTrue("Kutu 1 (Günlük): 1 Kart" in ozet)
        assertTrue("Kutu 3 (Aylık): 1 Kart" in ozet)
    }

    @Test
    fun `modul 1 bos destede kutu dagilim ozeti sifir basar`() {
        val ozet = DersIleriFaz.Ileri1_LeitnerMotoru.kutuDagilimOzeti(emptyList())
        assertTrue("Kutu 1 (Günlük): 0 Kart" in ozet)
        assertTrue("Kutu 2 (Haftalık): 0 Kart" in ozet)
    }

    @Test
    fun `modul 1 varsayilan deste k1 k2 k3 kartlarini icerir`() {
        val kartlar = DersIleriFaz.Ileri1_LeitnerMotoru.varsayilanDeste()
        assertEquals(3, kartlar.size)
        assertEquals("k1", kartlar[0].id)
        assertEquals("k3", kartlar[2].id)
    }

    @Test
    fun `modul 1 kart yanlis bilince kutu 2 den 1 e iner`() {
        val kart = DersIleriFaz.LeitnerKart("x", "S", "C", kutuNo = 2)
        assertEquals(1, DersIleriFaz.Ileri1_LeitnerMotoru.kartYanlisBildim(kart).kutuNo)
    }

    // ── PDF SAYFA ÜZERİ FLAŞ KART TESTLERİ (8..11) ──
    @Test
    fun `modul 2 pdf vurgudan flas kart uretme tire ayracini tanir`() {
        val vurgu = "Lozan Boğazlar - Montrö'ye kadar uluslararası komisyon"
        val kart = DersIleriFaz.Ileri2_PdfFlasKart.pdfVurgudanFlasKartUret(vurgu)
        assertTrue("Lozan Boğazlar nedir?" in kart.soru)
        assertTrue("Montrö" in kart.cevap)
        assertEquals(1, kart.kutuNo)
    }

    @Test
    fun `modul 2 pdf vurgudan flas kart uretme ok ayracini tanir`() {
        val vurgu = "İntegral -> Eğri altında kalan alan"
        val kart = DersIleriFaz.Ileri2_PdfFlasKart.pdfVurgudanFlasKartUret(vurgu)
        assertTrue("İntegral nedir?" in kart.soru)
        assertTrue("Eğri altında kalan alan" in kart.cevap)
    }

    @Test
    fun `modul 2 pdf vurgudan flas kart uretme iki nokta ayracini tanir`() {
        val vurgu = "Osmanlı Kuruluş : 1299 Söğüt"
        val kart = DersIleriFaz.Ileri2_PdfFlasKart.pdfVurgudanFlasKartUret(vurgu)
        assertTrue("Osmanlı Kuruluş nedir?" in kart.soru)
        assertTrue("1299 Söğüt" in kart.cevap)
    }

    @Test
    fun `modul 2 pdf vurgudan flas kart uretme ayracsiz metinde tanim sorusu uretir`() {
        val vurgu = "Tek cümlelik önemli bir tanım metni"
        val kart = DersIleriFaz.Ileri2_PdfFlasKart.pdfVurgudanFlasKartUret(vurgu)
        assertTrue("Tanım:" in kart.soru)
        assertTrue("Kavramın detaylarını" in kart.cevap)
    }

    // ── KPSS DENEME NET EĞRİSİ TESTLERİ (12..16) ──
    @Test
    fun `modul 3 deneme kaydi ekleme listeyi buyutur`() {
        val list = listOf(DersIleriFaz.DenemeKaydi("KPSS 1", 45, 12, 60))
        val yeni = DersIleriFaz.Ileri3_DenemeEgrisi.denemeKayitEkle(list, DersIleriFaz.DenemeKaydi("KPSS 2", 50, 10, 60))
        assertEquals(2, yeni.size)
        assertEquals("KPSS 2", yeni[1].sinavAd)
    }

    @Test
    fun `modul 3 egri analizi yukselisi algilar`() {
        val list = listOf(
            DersIleriFaz.DenemeKaydi("KPSS 1", 45, 12, 60), // 42.0 net
            DersIleriFaz.DenemeKaydi("KPSS 2", 50, 10, 60)  // 47.5 net -> yükseliş
        )
        val analizi = DersIleriFaz.Ileri3_DenemeEgrisi.egriAnalizi(list)
        assertTrue("📈 YÜKSELİŞTE" in analizi)
        assertTrue("47.50" in analizi)
    }

    @Test
    fun `modul 3 egri analizi dususu algilar`() {
        val list = listOf(
            DersIleriFaz.DenemeKaydi("KPSS 1", 50, 10, 60), // 47.5 net
            DersIleriFaz.DenemeKaydi("KPSS 2", 40, 12, 60)  // 37.0 net -> düşüş
        )
        val analizi = DersIleriFaz.Ileri3_DenemeEgrisi.egriAnalizi(list)
        assertTrue("📉 TEKRAR GEREKLİ" in analizi)
    }

    @Test
    fun `modul 3 egri analizi bos listeyi kontrol eder`() {
        assertEquals("Henüz deneme kaydı yok.", DersIleriFaz.Ileri3_DenemeEgrisi.egriAnalizi(emptyList()))
    }

    @Test
    fun `modul 3 deneme net hesabı 4 yanlis 1 dogruyu goturur`() {
        val kayit = DersIleriFaz.DenemeKaydi("Test", 52, 8, 60)
        assertEquals(50.0f, kayit.net, 0.01f)
    }

    // ── ACTIVE RECALL TESTLERİ (17..20) ──
    @Test
    fun `modul 4 active recall skoru uzun ve net ozette 95 dondurur`() {
        val ozet = "Lozan antlaşması boğazlar sözleşmesidir. Montrö ile komisyon kalkmıştır. Türkiye tam egemen olmuştur."
        val skor = DersIleriFaz.Ileri4_ActiveRecall.activeRecallSkoru(ozet)
        assertEquals(95, skor)
    }

    @Test
    fun `modul 4 active recall skoru cok kisa ozette 25 dondurur`() {
        assertEquals(25, DersIleriFaz.Ileri4_ActiveRecall.activeRecallSkoru("Çok kısa"))
    }

    @Test
    fun `modul 4 active recall skoru 2 cumle ve 8 kelimede 80 dondurur`() {
        val ozet = "Osmanlı İmparatorluğu 1299 yılında kurulmuştur. İstanbul 1453 yılında fethedilmiştir."
        assertEquals(80, DersIleriFaz.Ileri4_ActiveRecall.activeRecallSkoru(ozet))
    }

    @Test
    fun `modul 4 active recall skoru orta uzunlukta 60 dondurur`() {
        val ozet = "Sadece tek cümle ama yeterince uzun bir açıklama metni yazıyoruz."
        assertEquals(60, DersIleriFaz.Ileri4_ActiveRecall.activeRecallSkoru(ozet))
    }

    // ── BİYO SPRİNT & ÇELDİRİCİ & CSV TESTLERİ (21..26) ──
    @Test
    fun `modul 5 biyo sprint seans secimi animedoro ve ultradian dondurur`() {
        val a = DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(1)
        assertEquals("Animedoro (40m/20m)", a.ad)
        assertEquals(40, a.odakDk)
        val u = DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(2)
        assertEquals("Ultradian Ritm (90m/20m)", u.ad)
        assertEquals(90, u.odakDk)
    }

    @Test
    fun `modul 5 biyo sprint varsayılan seans pomodoro dondurur`() {
        val p = DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(0)
        assertEquals("Standart Pomodoro (25m/5m)", p.ad)
        assertEquals(25, p.odakDk)
    }

    @Test
    fun `modul 6 celdirici uyarisi tarih dersinde osym celdiricilerini listeler`() {
        val uyari = DersIleriFaz.Ileri6_CeldiriciVeSokratik.celdiriciUyarisi("Tarih")
        assertTrue("ÖSYM Çeldiricisi (Tarih)" in uyari)
        assertTrue("Tanzimat ile Islahat" in uyari)
    }

    @Test
    fun `modul 6 celdirici uyarisi turkce dersinde paragraf çeldiricilerini listeler`() {
        val uyari = DersIleriFaz.Ileri6_CeldiriciVeSokratik.celdiriciUyarisi("Türkçe")
        assertTrue("ÖSYM Çeldiricisi (Paragraf)" in uyari)
    }

    @Test
    fun `modul 7 kartlari csv uretme gecerli csv formati basar`() {
        val kartlar = listOf(DersIleriFaz.LeitnerKart("id1", "Soru 1", "Cevap 1", 2))
        val csv = DersIleriFaz.Ileri7_FormulVeCsv.kartlariCsvUret(kartlar)
        assertTrue("ID,Soru,Cevap,KutuNo" in csv)
        assertTrue("id1,\"Soru 1\",\"Cevap 1\",2" in csv)
    }

    @Test
    fun `modul 7 denemeleri csv uretme gecerli net formatli csv basar`() {
        val denemeler = listOf(DersIleriFaz.DenemeKaydi("Test 1", 48, 12, 60)) // 45.0 net
        val csv = DersIleriFaz.Ileri7_FormulVeCsv.denemeleriCsvUret(denemeler)
        assertTrue("SinavAd,Dogru,Yanlis,SureDk,Net" in csv)
        assertTrue("\"Test 1\",48,12,60,45.00" in csv)
    }
}
