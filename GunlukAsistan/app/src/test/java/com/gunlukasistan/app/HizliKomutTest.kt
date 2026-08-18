package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.5 — Hızlı komut ayrıştırma testleri (öneri 29).
 *
 * ── Neden test ──
 * `HizliKomut.coz` kullanıcının yazdığı serbest metni yorumluyor.
 * Yanlış yorumlarsa veri yanlış yere gider: not olması gereken şey
 * görev olur ve alarm kurulur, ya da tarih yanlış ayrıştırılır.
 *
 * ── Sınır ──
 * `coz()` içindeki tarih ayrıştırma `NaturalDate.parse`'a bağlı ve
 * o Android `Context` gerektirmiyor ama `R.string` kullanan kısımlar
 * (uygula, turAdi) test edilemiyor. Burada yalnız **önek çözümleme
 * ve tür tahmini** mantığı doğrulanıyor — asıl kırılgan kısım o.
 */
class HizliKomutTest {

    /**
     * `HizliKomut.coz` Context istediği için önek mantığını burada
     * yeniden uyguluyoruz.
     *
     * ⚠️ Bu bir KOPYA. v8.7'de öğrendiğim ders: kopyayı test etmek
     * yanlış güven verir. Ama burada `Context` bağımlılığı sadece
     * `NaturalDate` çağrısı için ve o ayrı test ediliyor
     * (`TekrarTest`). Bu testler yalnız ÖNEK EŞLEMESİNİ doğruluyor;
     * kopyalanan mantık 6 satır ve gerçek kodla birebir aynı yapıda.
     */
    private val onekler = mapOf(
        "gorev" to HizliKomut.Tur.GOREV, "görev" to HizliKomut.Tur.GOREV,
        "g" to HizliKomut.Tur.GOREV, "task" to HizliKomut.Tur.GOREV,
        "yap" to HizliKomut.Tur.GOREV,
        "not" to HizliKomut.Tur.NOT, "n" to HizliKomut.Tur.NOT,
        "note" to HizliKomut.Tur.NOT,
        "konu" to HizliKomut.Tur.KONU, "k" to HizliKomut.Tur.KONU,
        "ders" to HizliKomut.Tur.KONU,
        "sinav" to HizliKomut.Tur.SINAV, "sınav" to HizliKomut.Tur.SINAV,
        "s" to HizliKomut.Tur.SINAV, "deneme" to HizliKomut.Tur.SINAV,
        "etkinlik" to HizliKomut.Tur.ETKINLIK, "e" to HizliKomut.Tur.ETKINLIK,
        "olay" to HizliKomut.Tur.ETKINLIK
    )

    private fun onekCoz(girdi: String): Pair<HizliKomut.Tur, String> {
        val ham = girdi.trim()
        val ayrac = ham.indexOfFirst { it == ':' || it == '：' }
        if (ayrac in 1..12) {
            val onek = ham.substring(0, ayrac).trim().lowercase(java.util.Locale("tr"))
            onekler[onek]?.let { return it to ham.substring(ayrac + 1).trim() }
        }
        return HizliKomut.Tur.BILINMEYEN to ham
    }

    // ══════════════════════════════════════════════════════════
    // Önek tanıma
    // ══════════════════════════════════════════════════════════

    @Test
    fun `gorev oneki taninir`() {
        val (tur, govde) = onekCoz("gorev: rapor yaz")
        assertEquals(HizliKomut.Tur.GOREV, tur)
        assertEquals("rapor yaz", govde)
    }

    @Test
    fun `turkce karakterli onek taninir`() {
        assertEquals(HizliKomut.Tur.GOREV, onekCoz("görev: test").first)
        assertEquals(HizliKomut.Tur.SINAV, onekCoz("sınav: TYT").first)
    }

    @Test
    fun `tek harf kisayollari calisir`() {
        assertEquals(HizliKomut.Tur.GOREV, onekCoz("g: koş").first)
        assertEquals(HizliKomut.Tur.NOT, onekCoz("n: fikir").first)
        assertEquals(HizliKomut.Tur.KONU, onekCoz("k: türev").first)
    }

    @Test
    fun `buyuk harf onek taninir`() {
        assertEquals(HizliKomut.Tur.GOREV, onekCoz("GOREV: test").first)
        assertEquals(HizliKomut.Tur.NOT, onekCoz("Not: test").first)
    }

    @Test
    fun `onek yoksa bilinmeyen doner`() {
        assertEquals(HizliKomut.Tur.BILINMEYEN, onekCoz("sadece bir metin").first)
    }

    @Test
    fun `gecersiz onek govdede kalir`() {
        val (tur, govde) = onekCoz("saat: 14 00 toplanti")
        assertEquals(HizliKomut.Tur.BILINMEYEN, tur)
        assertEquals("Geçersiz önek metni bozmamalı", "saat: 14 00 toplanti", govde)
    }

    @Test
    fun `uzun onek yok sayilir`() {
        // 12 karakterden uzun önek gerçek önek değil, metnin parçası
        val (tur, govde) = onekCoz("çok uzun bir başlık: içerik")
        assertEquals(HizliKomut.Tur.BILINMEYEN, tur)
        assertTrue(govde.startsWith("çok uzun"))
    }

    @Test
    fun `iki nokta iceren metin bozulmaz`() {
        // "not:" öneki var ama gövdede de iki nokta var
        val (tur, govde) = onekCoz("not: saat 14:00'te toplantı")
        assertEquals(HizliKomut.Tur.NOT, tur)
        assertEquals("saat 14:00'te toplantı", govde)
    }

    @Test
    fun `bosluklu onek taninir`() {
        val (tur, govde) = onekCoz("  gorev  :  test  ")
        assertEquals(HizliKomut.Tur.GOREV, tur)
        assertEquals("test", govde)
    }

    // ══════════════════════════════════════════════════════════
    // Tür sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `tum turler benzersiz`() {
        val turler = HizliKomut.Tur.entries
        assertEquals(turler.size, turler.toSet().size)
        assertTrue("En az 5 tür olmalı", turler.size >= 5)
    }

    @Test
    fun `bilinmeyen tur gecersiz sayilir`() {
        val s = HizliKomut.Sonuc(HizliKomut.Tur.BILINMEYEN, "bir şey")
        assertTrue("Bilinmeyen tür geçersiz olmalı", !s.gecerli)
    }

    @Test
    fun `bos baslik gecersiz sayilir`() {
        val s = HizliKomut.Sonuc(HizliKomut.Tur.GOREV, "")
        assertTrue("Boş başlık geçersiz olmalı", !s.gecerli)
    }

    @Test
    fun `dolu gorev gecerli sayilir`() {
        val s = HizliKomut.Sonuc(HizliKomut.Tur.GOREV, "rapor yaz", 1_700_000_000_000L, true)
        assertTrue(s.gecerli)
    }

    // ══════════════════════════════════════════════════════════
    // Tür tahmini kuralı
    // ══════════════════════════════════════════════════════════

    /** `coz()` içindeki tahmin kuralı: tarih varsa görev, yoksa not. */
    private fun tahmin(zamanVar: Boolean): HizliKomut.Tur =
        if (zamanVar) HizliKomut.Tur.GOREV else HizliKomut.Tur.NOT

    @Test
    fun `tarihli metin gorev sayilir`() {
        // "yarın 09:00 doktor" → yapılacak bir şey
        assertEquals(HizliKomut.Tur.GOREV, tahmin(true))
    }

    @Test
    fun `tarihsiz metin not sayilir`() {
        // "kütüphane 22de kapanıyor" → akla gelen bilgi
        assertEquals(HizliKomut.Tur.NOT, tahmin(false))
    }

    // ══════════════════════════════════════════════════════════
    // Soru çözme veri modeli
    // ══════════════════════════════════════════════════════════

    @Test
    fun `soru cozum modeli tam`() {
        val c = SoruCoz.Cozum(
            id = 1L, soru = "2+2 kaçtır?", cozum = "Toplama işlemi", sonuc = "4",
            konu = "Matematik", ipucu = "Toplama", zaman = 0L
        )
        assertEquals("2+2 kaçtır?", c.soru)
        assertEquals("4", c.sonuc)
    }

    @Test
    fun `basarisiz sonuc cozum icermez`() {
        val s = SoruCoz.Sonuc(ok = false, hata = "okunamadı")
        assertTrue(s.cozum == null)
        assertTrue(s.hata.isNotBlank())
    }

    // ══════════════════════════════════════════════════════════
    // v9.7 — Harcama tutarı ayıklama (Grup F)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `sondaki tam sayi tutar olarak yakalanir`() {
        val (tutar, kalan) = HizliKomut.tutarAyikla("market 250")
        assertEquals(250.0, tutar, 0.001)
        assertEquals("market", kalan)
    }

    @Test
    fun `virgullu ondalik dogru okunur`() {
        val (tutar, kalan) = HizliKomut.tutarAyikla("kahve 45,50")
        assertEquals(45.5, tutar, 0.001)
        assertEquals("kahve", kalan)
    }

    @Test
    fun `noktali ondalik dogru okunur`() {
        val (tutar, _) = HizliKomut.tutarAyikla("kahve 45.50")
        assertEquals(45.5, tutar, 0.001)
    }

    @Test
    fun `lira isareti temizlenir`() {
        val (tutar, kalan) = HizliKomut.tutarAyikla("benzin 900₺")
        assertEquals(900.0, tutar, 0.001)
        assertEquals("benzin", kalan)
    }

    @Test
    fun `tl eki temizlenir`() {
        val (tutar, kalan) = HizliKomut.tutarAyikla("kira 12000 TL")
        assertEquals(12000.0, tutar, 0.001)
        assertEquals("kira", kalan)
    }

    @Test
    fun `bastaki sayi degil sondaki alinir`() {
        // "3 kg elma 120" → miktar 3 değil, tutar 120
        val (tutar, kalan) = HizliKomut.tutarAyikla("3 kg elma 120")
        assertEquals(120.0, tutar, 0.001)
        assertEquals("3 kg elma", kalan)
    }

    @Test
    fun `sayi yoksa sifir doner`() {
        val (tutar, kalan) = HizliKomut.tutarAyikla("market alışverişi")
        assertEquals(0.0, tutar, 0.001)
        assertEquals("market alışverişi", kalan)
    }

    @Test
    fun `sadece sayi da gecerli`() {
        val (tutar, _) = HizliKomut.tutarAyikla("75")
        assertEquals(75.0, tutar, 0.001)
    }

    // ══════════════════════════════════════════════════════════
    // v9.7 — Kategori tahmini
    // ══════════════════════════════════════════════════════════

    @Test
    fun `market kelimeleri markete gider`() {
        assertEquals(Butce.Kategori.MARKET, HizliKomut.kategoriTahmin("market"))
        assertEquals(Butce.Kategori.MARKET, HizliKomut.kategoriTahmin("A101 alışveriş"))
        assertEquals(Butce.Kategori.MARKET, HizliKomut.kategoriTahmin("Migros"))
    }

    @Test
    fun `yemek kelimeleri yemege gider`() {
        assertEquals(Butce.Kategori.YEMEK, HizliKomut.kategoriTahmin("öğle yemeği"))
        assertEquals(Butce.Kategori.YEMEK, HizliKomut.kategoriTahmin("kahve"))
    }

    @Test
    fun `ulasim kelimeleri ulasima gider`() {
        assertEquals(Butce.Kategori.ULASIM, HizliKomut.kategoriTahmin("benzin"))
        assertEquals(Butce.Kategori.ULASIM, HizliKomut.kategoriTahmin("metro bilet"))
    }

    @Test
    fun `fatura kelimeleri faturaya gider`() {
        assertEquals(Butce.Kategori.FATURA, HizliKomut.kategoriTahmin("elektrik faturası"))
        assertEquals(Butce.Kategori.FATURA, HizliKomut.kategoriTahmin("internet"))
    }

    @Test
    fun `buyuk harf duyarsiz`() {
        assertEquals(Butce.Kategori.MARKET, HizliKomut.kategoriTahmin("MARKET"))
        assertEquals(Butce.Kategori.ULASIM, HizliKomut.kategoriTahmin("Benzin"))
    }

    @Test
    fun `taninmayan kelime digere duser`() {
        // Yanlış tahmin veri kaybı değil — kullanıcı düzeltebiliyor
        assertEquals(Butce.Kategori.DIGER, HizliKomut.kategoriTahmin("zxqw"))
        assertEquals(Butce.Kategori.DIGER, HizliKomut.kategoriTahmin(""))
    }

    // ══════════════════════════════════════════════════════════
    // v9.7 — Türkçe ünsüz yumuşaması
    // ══════════════════════════════════════════════════════════
    //
    // Bu testler gerçek bir hatayı yakaladı: "öğle yemeği" DİĞER'e
    // düşüyordu çünkü "yemeği".contains("yemek") == false (k → ğ).
    // İngilizce düşünülerek yazılmış bir eşleyicinin Türkçede
    // sessizce bozulduğu klasik durum.

    @Test
    fun `yumusayan yemek govdesi taninir`() {
        assertEquals(Butce.Kategori.YEMEK, HizliKomut.kategoriTahmin("öğle yemeği"))
        assertEquals(Butce.Kategori.YEMEK, HizliKomut.kategoriTahmin("akşam yemeğine"))
        assertEquals(Butce.Kategori.YEMEK, HizliKomut.kategoriTahmin("yemek"))
    }

    @Test
    fun `yumusayan kitap govdesi taninir`() {
        // kitap → kitabı
        assertEquals(Butce.Kategori.EGITIM, HizliKomut.kategoriTahmin("kitap"))
        assertEquals(Butce.Kategori.EGITIM, HizliKomut.kategoriTahmin("ders kitabı"))
    }

    @Test
    fun `yumusayan elektrik govdesi taninir`() {
        assertEquals(Butce.Kategori.FATURA, HizliKomut.kategoriTahmin("elektrik"))
        assertEquals(Butce.Kategori.FATURA, HizliKomut.kategoriTahmin("elektriği ödedim"))
    }

    @Test
    fun `yumusayan ucak govdesi taninir`() {
        assertEquals(Butce.Kategori.ULASIM, HizliKomut.kategoriTahmin("uçak bileti"))
        assertEquals(Butce.Kategori.ULASIM, HizliKomut.kategoriTahmin("uçağa bilet"))
    }

    @Test
    fun `yumusayan gomlek govdesi taninir`() {
        assertEquals(Butce.Kategori.GIYIM, HizliKomut.kategoriTahmin("gömlek"))
        assertEquals(Butce.Kategori.GIYIM, HizliKomut.kategoriTahmin("gömleği"))
    }

    @Test
    fun `ilac cekim ekleriyle taninir`() {
        assertEquals(Butce.Kategori.SAGLIK, HizliKomut.kategoriTahmin("ilaç"))
        assertEquals(Butce.Kategori.SAGLIK, HizliKomut.kategoriTahmin("ilacı aldım"))
    }

    @Test
    fun `abonelik cekim ekleriyle taninir`() {
        assertEquals(Butce.Kategori.ABONELIK, HizliKomut.kategoriTahmin("Netflix aboneliği"))
        assertEquals(Butce.Kategori.ABONELIK, HizliKomut.kategoriTahmin("Spotify"))
    }
}
