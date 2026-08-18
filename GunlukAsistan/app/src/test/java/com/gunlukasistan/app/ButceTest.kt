package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.7 — Harcama defteri testleri (öneri 43).
 *
 * Context gerektirmeyen hesap mantığı: bakiye, harcama oranı,
 * kategori kodları, gelir/gider ayrımı.
 */
class ButceTest {

    private fun ozet(gelir: Double, gider: Double, adet: Int = 5) =
        Butce.AyOzet("202608", "Ağu", gelir, gider, adet)

    // ══════════════════════════════════════════════════════════
    // Bakiye
    // ══════════════════════════════════════════════════════════

    @Test
    fun `bakiye gelir eksi gider`() {
        assertEquals(2000.0, ozet(10000.0, 8000.0).bakiye, 0.001)
    }

    @Test
    fun `gider gelirden fazlaysa bakiye negatif`() {
        assertEquals(-1500.0, ozet(8000.0, 9500.0).bakiye, 0.001)
    }

    @Test
    fun `gelir yoksa bakiye gider kadar negatif`() {
        assertEquals(-500.0, ozet(0.0, 500.0).bakiye, 0.001)
    }

    // ══════════════════════════════════════════════════════════
    // Harcama oranı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `harcama orani yuzde olarak doner`() {
        assertEquals(80, ozet(10000.0, 8000.0).harcamaOrani)
    }

    @Test
    fun `gelir sifirsa oran null doner`() {
        // Sıfıra bölme koruması
        assertNull(ozet(0.0, 5000.0).harcamaOrani)
    }

    @Test
    fun `oran yuz uzerinde de hesaplanir`() {
        // Gelirden fazla harcama mümkün (birikimden yeme)
        assertEquals(150, ozet(10000.0, 15000.0).harcamaOrani)
    }

    @Test
    fun `oran ust sinira kirpilir`() {
        // Absürt değerler arayüzü bozmasın
        assertEquals(999, ozet(10.0, 1000000.0).harcamaOrani)
    }

    @Test
    fun `sifir gider sifir oran`() {
        assertEquals(0, ozet(10000.0, 0.0).harcamaOrani)
    }

    // ══════════════════════════════════════════════════════════
    // Kategoriler — yedek uyumluluğu
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kategori kodlari sabit kalmali`() {
        // JSON'a yazılıyor; değişirse eski kayıtlar "Diğer"e düşer
        assertEquals("market", Butce.Kategori.MARKET.kod)
        assertEquals("fatura", Butce.Kategori.FATURA.kod)
        assertEquals("maas", Butce.Kategori.MAAS.kod)
        assertEquals("diger", Butce.Kategori.DIGER.kod)
    }

    @Test
    fun `bilinmeyen kategori diger olur`() {
        assertEquals(Butce.Kategori.DIGER, Butce.Kategori.bul("uydurma"))
        assertEquals(Butce.Kategori.DIGER, Butce.Kategori.bul(null))
    }

    @Test
    fun `gelir kategorileri gider listesinde yok`() {
        assertTrue(Butce.Kategori.MAAS !in Butce.Kategori.giderler)
        assertTrue(Butce.Kategori.EK_GELIR !in Butce.Kategori.giderler)
    }

    @Test
    fun `gider kategorileri gelir listesinde yok`() {
        assertTrue(Butce.Kategori.MARKET !in Butce.Kategori.gelirler)
        assertTrue(Butce.Kategori.KIRA !in Butce.Kategori.gelirler)
    }

    @Test
    fun `iki liste tum kategorileri kapsar`() {
        assertEquals(
            Butce.Kategori.entries.size,
            Butce.Kategori.giderler.size + Butce.Kategori.gelirler.size
        )
    }

    @Test
    fun `her kategorinin benzersiz kodu var`() {
        val kodlar = Butce.Kategori.entries.map { it.kod }
        assertEquals(kodlar.size, kodlar.toSet().size)
    }

    @Test
    fun `her kategorinin rengi var`() {
        // Renk 0 olursa grafik dilimi görünmez olur
        Butce.Kategori.entries.forEach {
            assertTrue("${it.kod} rengi eksik", it.renk != 0)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Kalem
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kalem tutari her zaman pozitif tutulur`() {
        // Yön `gelir` bayrağında; negatif tutar toplama hatası yaratır
        val kalem = Butce.Kalem(1L, 250.0, Butce.Kategori.MARKET, gelir = false)
        assertTrue(kalem.tutar > 0)
    }

    @Test
    fun `ay anahtari alti karakter`() {
        val kalem = Butce.Kalem(1L, 100.0, Butce.Kategori.MARKET)
        assertEquals(6, kalem.ayAnahtari.length)
    }

    @Test
    fun `gun anahtari sekiz karakter`() {
        val kalem = Butce.Kalem(1L, 100.0, Butce.Kategori.MARKET)
        assertEquals(8, kalem.gunAnahtari.length)
    }

    @Test
    fun `ay anahtari gun anahtarinin onekidir`() {
        val kalem = Butce.Kalem(1L, 100.0, Butce.Kategori.MARKET)
        assertTrue(kalem.gunAnahtari.startsWith(kalem.ayAnahtari))
    }

    @Test
    fun `ayAnahtari yardimcisi gecerli bicim uretir`() {
        val anahtar = Butce.ayAnahtari()
        assertEquals(6, anahtar.length)
        assertTrue(anahtar.all { it.isDigit() })
    }
}
