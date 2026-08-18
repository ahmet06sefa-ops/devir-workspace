package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v11.04 — Kişisel Gelişim ve Farkındalık Merkezi Motoru (`KisiselGelisimMotoru`)
 * saf JVM birim testleri (25 test).
 */
class KisiselGelisimTest {

    @Before
    fun setup() {
        KisiselGelisimMotoru.testIcinSifirla(null)
    }

    // ─── 1. RETROPERSPEKTİF TESTLERİ ───

    @Test
    fun `varsayilan retro aylari 12 adet ay barindirir`() {
        val aylar = KisiselGelisimMotoru.varsayilanRetroAylariGetir()
        assertEquals(12, aylar.size)
        assertEquals("Eylül 2025", aylar.first().ayAd)
        assertEquals("Ağustos 2026", aylar.last().ayAd)
    }

    @Test
    fun `retroperspektif neler katti ve neler degisti alanlari doludur`() {
        val aylar = KisiselGelisimMotoru.varsayilanRetroAylariGetir()
        assertTrue(aylar.all { it.nelerKatti.isNotEmpty() && it.nelerDegisti.isNotEmpty() })
    }

    @Test
    fun `yillik farkindalik ortalamasi 1 ile 10 arasindadir`() {
        val aylar = KisiselGelisimMotoru.varsayilanRetroAylariGetir()
        val ort = KisiselGelisimMotoru.yillikFarkindalikOrtalamasi(aylar)
        assertTrue(ort >= 1.0f && ort <= 10.0f)
    }

    @Test
    fun `retro aylik ozet metni puan bilgisini icerir`() {
        val aylar = KisiselGelisimMotoru.varsayilanRetroAylariGetir()
        val metin = KisiselGelisimMotoru.retroAylikOzetMetni(aylar)
        assertTrue(metin.contains("Farkındalık Puanı"))
    }

    @Test
    fun `retro ay listesi bos ise ortalama 0 doner`() {
        val ort = KisiselGelisimMotoru.yillikFarkindalikOrtalamasi(emptyList())
        assertEquals(0f, ort, 0.01f)
    }

    // ─── 2. MANİFESTO TESTLERİ ───

    @Test
    fun `varsayilan manifesto degerler ve kimlik tanimi bos degildir`() {
        val m = KisiselGelisimMotoru.varsayilanManifesto()
        assertTrue(m.degerler.isNotEmpty())
        assertTrue(m.kimlikTanimi.isNotBlank())
    }

    @Test
    fun `manifesto netlik skoru yuzde 100 hesaplanir tam dolu veride`() {
        val m = KisiselGelisimMotoru.varsayilanManifesto()
        assertEquals(100, m.netlikSkoruYuzdesi)
    }

    @Test
    fun `manifesto netlik skoru eksik alanlarda duser`() {
        val m = KisiselGelisimMotoru.ManifestoVeri(
            degerler = mutableListOf(),
            kimlikTanimi = "",
            besYilKariyer = "Kariyer",
            besYilSaglik = "",
            besYilFinans = "",
            besYilSosyal = "",
            besYilBilgelik = ""
        )
        assertEquals(20, m.netlikSkoruYuzdesi)
    }

    @Test
    fun `manifesto netlik ozeti kafadaki karisiklik ifadesini icerir`() {
        val m = KisiselGelisimMotoru.varsayilanManifesto()
        assertTrue(m.netlikOzeti.contains("Kafadaki Karışıklık"))
    }

    @Test
    fun `manifesto 5 yillik vizyon 5 farkli kategoriyi icerir`() {
        val m = KisiselGelisimMotoru.varsayilanManifesto()
        assertTrue(m.besYilKariyer.isNotEmpty())
        assertTrue(m.besYilSaglik.isNotEmpty())
        assertTrue(m.besYilFinans.isNotEmpty())
        assertTrue(m.besYilSosyal.isNotEmpty())
        assertTrue(m.besYilBilgelik.isNotEmpty())
    }

    // ─── 3. SWOT ANALİZİ TESTLERİ ───

    @Test
    fun `varsayilan swot guclu zayif firsat tehdit maddelerini barindirir`() {
        val s = KisiselGelisimMotoru.varsayilanSwot()
        assertTrue(s.gucluler.isNotEmpty())
        assertTrue(s.zayiflar.isNotEmpty())
        assertTrue(s.firsatlar.isNotEmpty())
        assertTrue(s.tehditler.isNotEmpty())
    }

    @Test
    fun `swot guclu orani yuzdesi dengeli hesaplanir`() {
        val s = KisiselGelisimMotoru.SwotVeri(
            gucluler = mutableListOf("1", "2"),
            zayiflar = mutableListOf("1"),
            firsatlar = mutableListOf("1"),
            tehditler = mutableListOf("1")
        )
        // Güçlü+Fırsat = 3, Zayıf+Tehdit = 2 -> Toplam 5 -> %60
        assertEquals(60, s.gucluOraniYuzde)
    }

    @Test
    fun `swot liste bos iken orantili 50 yuzde doner`() {
        val s = KisiselGelisimMotoru.SwotVeri(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
        assertEquals(50, s.gucluOraniYuzde)
    }

    @Test
    fun `swot madde silme ve ekleme fonksiyonu dogru calisir`() {
        val s = KisiselGelisimMotoru.swotMaddeEkle(null, "GUCLU", "Yeni Güçlü Yön")
        assertTrue(s.gucluler.contains("Yeni Güçlü Yön"))
    }

    @Test
    fun `swot bolum kodlarina gore madde silinebilir`() {
        val s = KisiselGelisimMotoru.varsayilanSwot()
        val ilkSayi = s.gucluler.size
        s.gucluler.removeAt(0)
        assertEquals(ilkSayi - 1, s.gucluler.size)
    }

    // ─── 4. DERİN ÇALIŞMA PERİYODU TESTLERİ ───

    @Test
    fun `varsayilan derin calisma 3 saatlik 180 dk odak icerir`() {
        val d = KisiselGelisimMotoru.varsayilanDerinCalisma()
        assertEquals(180, d.seciliSureDk)
        assertTrue(d.konular.isNotEmpty())
    }

    @Test
    fun `derin calisma haftalik toplam saat 7 gunun toplamini doner`() {
        val d = KisiselGelisimMotoru.varsayilanDerinCalisma()
        assertEquals(23, d.haftalikToplamSaat)
    }

    @Test
    fun `derin calisma haftalik ortalama saat dogru hesaplanir`() {
        val d = KisiselGelisimMotoru.DerinCalismaVeri(
            mutableListOf("Test"),
            180,
            mutableListOf(7, 7, 7, 7, 7, 7, 7)
        )
        assertEquals(7f, d.haftalikOrtalamaSaat, 0.01f)
    }

    @Test
    fun `derin calismayi sayaca gonder fonksiyonu basarili doner`() {
        val res = KisiselGelisimMotoru.derinCalismayiSayacaGonder(
            null,
            "Yazılım Geliştirme",
            180
        )
        assertTrue(res.first)
        assertTrue(res.second.contains("180") || res.second.contains("3"))
    }

    @Test
    fun `derin calisma konular listesi bos olamaz`() {
        val d = KisiselGelisimMotoru.varsayilanDerinCalisma()
        assertTrue(d.konular.any { it.contains("Yazılım") || it.contains("Dil") || it.contains("Felsefe") })
    }

    // ─── 5. RESET GÜNÜ TESTLERİ ───

    @Test
    fun `varsayilan reset gorevleri oda bilgisayar ve hedefleri barindirir`() {
        val list = KisiselGelisimMotoru.varsayilanResetGorevleri()
        assertTrue(list.any { it.kategori.contains("Oda Toplama") })
        assertTrue(list.any { it.kategori.contains("Bilgisayar") })
        assertTrue(list.any { it.kategori.contains("Hedefler") })
    }

    @Test
    fun `daginiklik giderme yuzdesi tamamlanan gorev sayisina gore hesaplanir`() {
        val gorevler = listOf(
            KisiselGelisimMotoru.ResetGorev("1", "K1", "B1", true),
            KisiselGelisimMotoru.ResetGorev("2", "K1", "B2", false),
            KisiselGelisimMotoru.ResetGorev("3", "K1", "B3", true),
            KisiselGelisimMotoru.ResetGorev("4", "K1", "B4", false)
        )
        assertEquals(50, KisiselGelisimMotoru.daginiklikGidermeYuzdesi(gorevler))
    }

    @Test
    fun `daginiklik giderme durum metni yuzdeyi icerir`() {
        val gorevler = KisiselGelisimMotoru.varsayilanResetGorevleri()
        val metin = KisiselGelisimMotoru.daginiklikGidermeDurumMetni(gorevler)
        assertTrue(metin.contains("Hayatı Toparlama İlerlemesi"))
    }

    @Test
    fun `yeni reset gorevi ekleme listeye gorevi ekler`() {
        val list = KisiselGelisimMotoru.yeniResetGoreviEkle(null, "🏠 Oda Toplama", "Kitaplığı Düzenle")
        assertTrue(list.any { it.baslik == "Kitaplığı Düzenle" })
    }

    @Test
    fun `reset gorev silme gorevi listeden cikarir`() {
        val id = KisiselGelisimMotoru.varsayilanResetGorevleri().first().id
        val list = KisiselGelisimMotoru.resetGorevSil(null, id)
        assertFalse(list.any { it.id == id })
    }
}
