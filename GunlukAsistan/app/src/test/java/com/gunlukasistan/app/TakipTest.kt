package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v9.7 — Takip modülü testleri (öneri 41-46).
 *
 * Context gerektirmeyen saf mantığı test ediyor: tarih hesabı,
 * tekrar aralıkları, stok tahmini, aylık yük dönüşümü.
 *
 * Context'e bağlı kısımlar (SharedPreferences okuma/yazma) burada
 * test edilemiyor — Robolectric bağımlılığı eklemek istemedim,
 * APK boyutunu etkilemese de derleme süresini uzatıyor.
 */
class TakipTest {

    // ══════════════════════════════════════════════════════════
    // Gün farkı
    // ══════════════════════════════════════════════════════════

    private fun gun(yil: Int, ay: Int, gun: Int, saat: Int = 12): Long =
        Calendar.getInstance().apply {
            set(yil, ay - 1, gun, saat, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `ayni gun sifir fark verir`() {
        assertEquals(0, Takip.gunFarki(gun(2026, 8, 7, 9), gun(2026, 8, 7, 23)))
    }

    @Test
    fun `gece yarisini gecen iki saat bir gun sayilir`() {
        // 23:00 → 01:00 arası 2 saat ama TAKVİM olarak 1 gün.
        // Ham milisaniye bölmesi 0 derdi; bu testin varlık sebebi bu.
        val fark = Takip.gunFarki(gun(2026, 8, 7, 23), gun(2026, 8, 8, 1))
        assertEquals(1, fark)
    }

    @Test
    fun `gecmis tarih negatif fark verir`() {
        assertEquals(-5, Takip.gunFarki(gun(2026, 8, 7), gun(2026, 8, 2)))
    }

    @Test
    fun `ay gecisi dogru hesaplanir`() {
        assertEquals(3, Takip.gunFarki(gun(2026, 1, 30), gun(2026, 2, 2)))
    }

    @Test
    fun `yil gecisi dogru hesaplanir`() {
        assertEquals(2, Takip.gunFarki(gun(2025, 12, 31), gun(2026, 1, 2)))
    }

    // ══════════════════════════════════════════════════════════
    // Tekrar aralıkları
    // ══════════════════════════════════════════════════════════

    @Test
    fun `tekrar yok tarihi degistirmez`() {
        val t = gun(2026, 8, 7)
        assertEquals(t, Takip.sonrakiTarih(t, Takip.TEKRAR_YOK))
    }

    @Test
    fun `gun bazli tekrar dogru ekler`() {
        val bas = gun(2026, 8, 7)
        val sonuc = Takip.sonrakiTarih(bas, 7)
        assertEquals(7, Takip.gunFarki(bas, sonuc))
    }

    @Test
    fun `aylik tekrar takvim ayini kullanir`() {
        // 31 Ocak + 1 ay = 28 Şubat (2026 artık yıl değil).
        // Sabit 30 gün eklemek 2 Mart verirdi — yanlış.
        val bas = gun(2026, 1, 31)
        val sonuc = Takip.sonrakiTarih(bas, Takip.TEKRAR_AY)
        val c = Calendar.getInstance().apply { timeInMillis = sonuc }
        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH))
        assertEquals(28, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `yillik tekrar on iki ay ekler`() {
        val bas = gun(2026, 3, 15)
        val sonuc = Takip.sonrakiTarih(bas, Takip.TEKRAR_YIL)
        val c = Calendar.getInstance().apply { timeInMillis = sonuc }
        assertEquals(2027, c.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH))
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `uc aylik tekrar dogru`() {
        val bas = gun(2026, 1, 15)
        val sonuc = Takip.sonrakiTarih(bas, Takip.TEKRAR_UC_AY)
        val c = Calendar.getInstance().apply { timeInMillis = sonuc }
        assertEquals(Calendar.APRIL, c.get(Calendar.MONTH))
    }

    // ══════════════════════════════════════════════════════════
    // Kayıt hesapları
    // ══════════════════════════════════════════════════════════

    private fun kayit(
        tur: Takip.Tur = Takip.Tur.FATURA,
        sonraki: Long = 0L,
        sonrakiKm: Long = 0L,
        stok: Int = -1,
        doz: Int = 0
    ) = Takip.Kayit(
        id = 1L, tur = tur, ad = "Test",
        sonrakiMillis = sonraki, sonrakiKm = sonrakiKm,
        stok = stok, gunlukDoz = doz
    )

    @Test
    fun `stok gun hesabi dogru`() {
        // 30 hap, günde 2 → 15 gün
        assertEquals(15, kayit(stok = 30, doz = 2).stokGun())
    }

    @Test
    fun `stok gun tam bolunmezse asagi yuvarlar`() {
        // 31 hap, günde 2 → 15 gün (16. günün yarısı sayılmaz)
        assertEquals(15, kayit(stok = 31, doz = 2).stokGun())
    }

    @Test
    fun `doz sifirsa stok gun null doner`() {
        // Sıfıra bölme koruması — doz girilmemişse hesaplanamaz
        assertNull(kayit(stok = 30, doz = 0).stokGun())
    }

    @Test
    fun `stok takip edilmiyorsa null doner`() {
        assertNull(kayit(stok = -1, doz = 2).stokGun())
    }

    @Test
    fun `stok sifirsa sifir gun doner`() {
        assertEquals(0, kayit(stok = 0, doz = 2).stokGun())
    }

    @Test
    fun `km bazli kayit kalan gun hesaplamaz`() {
        val k = kayit(tur = Takip.Tur.ARAC, sonrakiKm = 50000)
        assertEquals(Int.MAX_VALUE, k.kalanGun())
    }

    @Test
    fun `km bazli kalan km dogru`() {
        val k = kayit(tur = Takip.Tur.ARAC, sonrakiKm = 50000)
        assertEquals(2000L, k.kalanKm(48000))
    }

    @Test
    fun `km esigi gecilmisse negatif doner`() {
        val k = kayit(tur = Takip.Tur.ARAC, sonrakiKm = 50000)
        assertEquals(-500L, k.kalanKm(50500))
    }

    @Test
    fun `tarih girilmemisse kalan gun sonsuz`() {
        assertEquals(Int.MAX_VALUE, kayit(sonraki = 0L).kalanGun())
    }

    @Test
    fun `kmBazli bayragi turden geliyor`() {
        assertTrue(kayit(tur = Takip.Tur.ARAC).kmBazli)
        assertTrue(!kayit(tur = Takip.Tur.ILAC).kmBazli)
        assertTrue(!kayit(tur = Takip.Tur.FATURA).kmBazli)
        assertTrue(!kayit(tur = Takip.Tur.BELGE).kmBazli)
    }

    // ══════════════════════════════════════════════════════════
    // Tür kodları — yedek uyumluluğu
    // ══════════════════════════════════════════════════════════

    @Test
    fun `tur kodlari sabit kalmali`() {
        // Bu kodlar JSON'a yazılıyor. Değişirse eski yedekler
        // yanlış türe düşer — ilaç kayıtları fatura olur.
        assertEquals("ilac", Takip.Tur.ILAC.kod)
        assertEquals("fatura", Takip.Tur.FATURA.kod)
        assertEquals("belge", Takip.Tur.BELGE.kod)
        assertEquals("arac", Takip.Tur.ARAC.kod)
    }

    @Test
    fun `bilinmeyen tur kodu fatura olur`() {
        assertEquals(Takip.Tur.FATURA, Takip.Tur.bul("uydurma"))
        assertEquals(Takip.Tur.FATURA, Takip.Tur.bul(null))
        assertEquals(Takip.Tur.FATURA, Takip.Tur.bul(""))
    }

    @Test
    fun `belge esigi digerlerinden uzun`() {
        // Pasaport randevusu için 45 gün gerekiyor;
        // elektrik faturası için 3 gün yeterli
        assertTrue(Takip.Tur.BELGE.varsayilanEsik > Takip.Tur.FATURA.varsayilanEsik)
        assertEquals(45, Takip.Tur.BELGE.varsayilanEsik)
    }

    // ══════════════════════════════════════════════════════════
    // Biçimlendirme
    // ══════════════════════════════════════════════════════════

    @Test
    fun `saat metni iki haneli`() {
        assertEquals("08:00", Takip.saatMetni(8 * 60))
        assertEquals("08:05", Takip.saatMetni(8 * 60 + 5))
        assertEquals("23:59", Takip.saatMetni(23 * 60 + 59))
        assertEquals("00:00", Takip.saatMetni(0))
    }

    @Test
    fun `sifir tutar tire gosterir`() {
        assertEquals("—", Takip.paraMetni(0.0))
        assertEquals("—", Takip.paraMetni(-5.0))
    }

    @Test
    fun `para metni lira isareti icerir`() {
        assertTrue(Takip.paraMetni(150.0).contains("₺"))
        assertTrue(Takip.paraMetni(2500.0).contains("₺"))
    }

    @Test
    fun `sifir km tire gosterir`() {
        assertEquals("—", Takip.kmMetni(0))
    }

    @Test
    fun `sifir tarih tire gosterir`() {
        assertEquals("—", Takip.tarihMetni(0))
        assertEquals("—", Takip.kisaTarih(0))
    }

    @Test
    fun `gecerli tarih bicimlenir`() {
        val metin = Takip.tarihMetni(gun(2026, 8, 7))
        assertNotNull(metin)
        assertTrue(metin.contains("2026"))
        assertTrue(metin != "—")
    }
}
