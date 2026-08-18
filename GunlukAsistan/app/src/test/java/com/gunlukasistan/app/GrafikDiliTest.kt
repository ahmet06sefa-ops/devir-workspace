package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.9 — Ortak grafik dili testleri (görsel öneri 10).
 *
 * ── Neden test ──
 * Bu sınıfın tek işi **tutarlılık**. Testler o tutarlılığı
 * kilitliyor: biri ileride yeni bir yeşil ton eklerse veya
 * `durumRengi` eşiklerini değiştirirse test uyarır.
 *
 * ── 🔴 `android.graphics.Color` KULLANMIYORUZ ──
 * İlk yazdığımda `alfa()` kullandım ve 7 test patladı:
 *
 *     RuntimeException: Method alpha in android.graphics.Color
 *     not mocked.
 *
 * Birim testlerinde `android.jar` bir SAPLAMA (stub); statik
 * metotlar bile gerçek gövde taşımıyor. v9.8'de aynı tuzağa
 * `org.json` ile düşmüştüm ve orada gerçek kütüphaneyi test
 * bağımlılığı olarak eklemiştim.
 *
 * Burada o gerekmiyor: ARGB ayrıştırma dört satırlık bit
 * işlemi. Kendi yardımcılarımızı yazmak hem bağımlılık
 * eklemekten hafif hem de testin neyi doğruladığını açık
 * bırakıyor.
 */
class GrafikDiliTest {

    // ARGB ayrıştırma — android.graphics.Color yerine (bkz. sınıf başlığı)
    private fun alfa(renk: Int) = (renk shr 24) and 0xFF
    private fun kirmizi(renk: Int) = (renk shr 16) and 0xFF
    private fun yesil(renk: Int) = (renk shr 8) and 0xFF
    private fun mavi(renk: Int) = renk and 0xFF

    // ══════════════════════════════════════════════════════════
    // Durum renkleri — tek kaynak
    // ══════════════════════════════════════════════════════════

    @Test
    fun `durum renkleri birbirinden farkli`() {
        val hepsi = listOf(
            GrafikDili.BASARI, GrafikDili.UYARI,
            GrafikDili.HATA, GrafikDili.NOTR
        )
        assertEquals(hepsi.size, hepsi.toSet().size)
    }

    @Test
    fun `durum renkleri tam opak`() {
        // Alfa kanalı eksikse renk saydam çıkar ve çizim kaybolur.
        // 0xFF ile başlamayan bir sabit bu testi düşürür.
        listOf(GrafikDili.BASARI, GrafikDili.UYARI, GrafikDili.HATA, GrafikDili.NOTR)
            .forEach { assertEquals(255, alfa(it)) }
    }

    @Test
    fun `soluk tonlar yari saydam`() {
        // Dolgu alanları için: opak olursa altındaki ızgara kaybolur
        assertTrue(alfa(GrafikDili.BASARI_SOLUK) < 255)
        assertTrue(alfa(GrafikDili.UYARI_SOLUK) < 255)
        assertTrue(alfa(GrafikDili.HATA_SOLUK) < 255)
    }

    @Test
    fun `soluk ton ana renkle ayni tonda`() {
        // BASARI_SOLUK gerçekten BAŞARI'nın soluk hâli mi?
        assertEquals(kirmizi(GrafikDili.BASARI), kirmizi(GrafikDili.BASARI_SOLUK))
        assertEquals(yesil(GrafikDili.BASARI), yesil(GrafikDili.BASARI_SOLUK))
        assertEquals(mavi(GrafikDili.BASARI), mavi(GrafikDili.BASARI_SOLUK))
    }

    // ══════════════════════════════════════════════════════════
    // durumRengi eşikleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `yuksek yuzde basari rengi`() {
        assertEquals(GrafikDili.BASARI, GrafikDili.durumRengi(100))
        assertEquals(GrafikDili.BASARI, GrafikDili.durumRengi(70))
    }

    @Test
    fun `orta yuzde uyari rengi`() {
        assertEquals(GrafikDili.UYARI, GrafikDili.durumRengi(69))
        assertEquals(GrafikDili.UYARI, GrafikDili.durumRengi(40))
    }

    @Test
    fun `dusuk yuzde hata rengi`() {
        assertEquals(GrafikDili.HATA, GrafikDili.durumRengi(39))
        assertEquals(GrafikDili.HATA, GrafikDili.durumRengi(0))
    }

    @Test
    fun `sinir degerleri tutarli`() {
        // 70 başarı, 69 uyarı — sınırda kayma olmamalı
        assertNotEquals(GrafikDili.durumRengi(69), GrafikDili.durumRengi(70))
        assertNotEquals(GrafikDili.durumRengi(39), GrafikDili.durumRengi(40))
    }

    @Test
    fun `aralik disi degerler cokmez`() {
        // Hesap hatası negatif veya >100 verebilir
        assertEquals(GrafikDili.HATA, GrafikDili.durumRengi(-10))
        assertEquals(GrafikDili.BASARI, GrafikDili.durumRengi(500))
    }

    // ══════════════════════════════════════════════════════════
    // soluk() yardımcısı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `soluk alfa uygular`() {
        val sonuc = GrafikDili.soluk(GrafikDili.BASARI, 40)
        assertEquals(40, alfa(sonuc))
        assertEquals(kirmizi(GrafikDili.BASARI), kirmizi(sonuc))
    }

    @Test
    fun `soluk varsayilan alfa`() {
        assertEquals(40, alfa(GrafikDili.soluk(GrafikDili.HATA)))
    }

    // ══════════════════════════════════════════════════════════
    // Palet
    // ══════════════════════════════════════════════════════════

    @Test
    fun `palet renkleri benzersiz`() {
        assertEquals(GrafikDili.PALET.size, GrafikDili.PALET.toSet().size)
    }

    @Test
    fun `palet hepsi opak`() {
        GrafikDili.PALET.forEach { assertEquals(255, alfa(it)) }
    }

    @Test
    fun `paletten dongusel calisir`() {
        val boyut = GrafikDili.PALET.size
        assertEquals(GrafikDili.paletten(0), GrafikDili.paletten(boyut))
        assertEquals(GrafikDili.paletten(1), GrafikDili.paletten(boyut + 1))
    }

    @Test
    fun `paletten negatif indekste cokmez`() {
        // Kotlin'de -1 % 8 = -1 → dizi hatası verirdi.
        // Çift modulo bunu engelliyor.
        val renk = GrafikDili.paletten(-1)
        assertTrue(renk in GrafikDili.PALET.toList())
        assertTrue(GrafikDili.paletten(-100) in GrafikDili.PALET.toList())
    }

    @Test
    fun `palet renk korlugu icin parlaklik farki tasiyor`() {
        // Renk körü kullanıcı tonu ayırt edemese bile parlaklık
        // farkını görebilmeli. İlk 4 rengin parlaklığı yeterince
        // dağılmış olmalı.
        val parlakliklar = GrafikDili.PALET.take(4).map {
            (kirmizi(it) * 299 + yesil(it) * 587 + mavi(it) * 114) / 1000
        }
        val fark = (parlakliklar.maxOrNull() ?: 0) - (parlakliklar.minOrNull() ?: 0)
        assertTrue("Palet parlaklık farkı çok düşük: $fark", fark > 30)
    }

    // ══════════════════════════════════════════════════════════
    // Ölçek sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `yazi boyutlari artan sirada`() {
        assertTrue(GrafikDili.YAZI_KUCUK < GrafikDili.YAZI_ORTA)
        assertTrue(GrafikDili.YAZI_ORTA < GrafikDili.YAZI_BUYUK)
    }

    @Test
    fun `cizgi kalinliklari artan sirada`() {
        assertTrue(GrafikDili.CIZGI_INCE < GrafikDili.CIZGI_NORMAL)
        assertTrue(GrafikDili.CIZGI_NORMAL < GrafikDili.CIZGI_KALIN)
    }

    @Test
    fun `animasyon sureleri makul`() {
        // 300 ms altı fark edilmez, 1200 ms üstü bekletir
        assertTrue(GrafikDili.SURE_NORMAL in 300..1000)
        assertTrue(GrafikDili.SURE_UZUN in 300..1500)
        assertTrue(GrafikDili.SURE_NORMAL < GrafikDili.SURE_UZUN)
    }
}
