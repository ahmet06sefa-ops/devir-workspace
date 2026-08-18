package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.4 — Süre analizi ve bütçe testleri (öneri 13, 14, 15).
 *
 * ── Neden test ──
 * Süre çarpanı kullanıcının planlarını doğrudan etkiliyor. Yanlış
 * hesaplarsa:
 *   · Çok yüksek çarpan → her işe aşırı süre ayırır, gün boşa gider
 *   · Çok düşük → sürekli yetişemez, motivasyon kırılır
 *
 * Aykırı değer koruması özellikle kritik: kullanıcı sayacı kapatmayı
 * unutursa tek bir 8 saatlik kayıt tüm çarpanı bozabilir.
 */
class SureAnaliziTest {

    // ══════════════════════════════════════════════════════════
    // Kayıt.oran
    // ══════════════════════════════════════════════════════════

    @Test
    fun `oran dogru hesaplanir`() {
        val k = SureAnalizi.Kayit(0L, tahminDk = 30, gercekDk = 45, etiket = "")
        assertEquals(1.5, k.oran, 0.001)
    }

    @Test
    fun `sifir tahmin cokmez`() {
        val k = SureAnalizi.Kayit(0L, tahminDk = 0, gercekDk = 45, etiket = "")
        assertEquals("Sıfıra bölme olmamalı", 1.0, k.oran, 0.001)
    }

    @Test
    fun `tam isabet orani bir`() {
        val k = SureAnalizi.Kayit(0L, tahminDk = 30, gercekDk = 30, etiket = "")
        assertEquals(1.0, k.oran, 0.001)
    }

    // ══════════════════════════════════════════════════════════
    // Bütçe hesabı (öneri 15)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kalan sure dogru`() {
        val b = SureAnalizi.Butce(hedefDk = 240, yapilanDk = 100, planlananDk = 90, bosDk = 300)
        assertEquals(140, b.kalanDk)
    }

    @Test
    fun `hedef asilinca kalan sifir`() {
        // Negatif kalan göstermek anlamsız — "eksi 30 dakika kaldı" saçma
        val b = SureAnalizi.Butce(hedefDk = 120, yapilanDk = 200, planlananDk = 0, bosDk = 100)
        assertEquals(0, b.kalanDk)
        assertEquals(0, b.acikDk)
    }

    @Test
    fun `acik sure planlanani dusurur`() {
        // 140 dk kaldı, 90 dk planlandı → 50 dk açık
        val b = SureAnalizi.Butce(hedefDk = 240, yapilanDk = 100, planlananDk = 90, bosDk = 300)
        assertEquals(50, b.acikDk)
    }

    @Test
    fun `plan kalandan fazlaysa acik sifir`() {
        val b = SureAnalizi.Butce(hedefDk = 240, yapilanDk = 200, planlananDk = 90, bosDk = 300)
        assertEquals(0, b.acikDk)
    }

    @Test
    fun `yetisir mi dogru hesaplanir`() {
        // 140 dk kaldı, 300 dk boş → yetişir
        assertTrue(
            SureAnalizi.Butce(240, 100, 90, 300).yetisirMi
        )
        // 140 dk kaldı, 60 dk boş → yetişmez
        assertTrue(
            !SureAnalizi.Butce(240, 100, 90, 60).yetisirMi
        )
    }

    @Test
    fun `yuzde 0-100 arasinda`() {
        listOf(
            SureAnalizi.Butce(240, 0, 0, 0),
            SureAnalizi.Butce(240, 120, 0, 0),
            SureAnalizi.Butce(240, 500, 0, 0),   // hedefi aştı
            SureAnalizi.Butce(0, 100, 0, 0)      // hedef yok
        ).forEach {
            assertTrue("Yüzde aralık dışı: ${it.yuzde}", it.yuzde in 0..100)
        }
    }

    @Test
    fun `hedef asilinca yuzde 100 de kalir`() {
        assertEquals(100, SureAnalizi.Butce(240, 500, 0, 0).yuzde)
    }

    @Test
    fun `hedef sifirsa yuzde sifir`() {
        // Sıfıra bölme olmamalı
        assertEquals(0, SureAnalizi.Butce(0, 100, 0, 0).yuzde)
    }

    // ══════════════════════════════════════════════════════════
    // Pomodoro özeti (öneri 14)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `basari orani dogru`() {
        val o = SureAnalizi.PomodoroOzet(
            toplam = 10, tamamlanan = 7, yarimKalan = 3,
            toplamDk = 175, enIyiSaat = 10, bugunToplam = 2
        )
        assertEquals(70, o.basariOrani)
    }

    @Test
    fun `bos pomodoro ozeti cokmez`() {
        val o = SureAnalizi.PomodoroOzet(0, 0, 0, 0, -1, 0)
        assertEquals("Sıfıra bölme olmamalı", 0, o.basariOrani)
    }

    @Test
    fun `tum turlar tamamlandiysa yuzde 100`() {
        val o = SureAnalizi.PomodoroOzet(5, 5, 0, 125, 14, 5)
        assertEquals(100, o.basariOrani)
    }

    // ══════════════════════════════════════════════════════════
    // Boş aralık (öneri 11)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `bos aralik bitisi dogru`() {
        val b = TakvimKopru.BosAralik(baslangicDk = 540, sureDk = 90)   // 09:00 + 1.5s
        assertEquals(630, b.bitisDk)
    }

    @Test
    fun `saat metni dogru bicimlenir`() {
        val b = TakvimKopru.BosAralik(baslangicDk = 540, sureDk = 90)
        assertEquals("09:00 – 10:30", b.saatMetni())
    }

    @Test
    fun `tek haneli dakika sifirla doldurulur`() {
        val b = TakvimKopru.BosAralik(baslangicDk = 545, sureDk = 20)   // 09:05 – 09:25
        assertEquals("09:05 – 09:25", b.saatMetni())
    }

    @Test
    fun `gece yarisina yakin aralik cokmez`() {
        val b = TakvimKopru.BosAralik(baslangicDk = 1380, sureDk = 60)  // 23:00 – 24:00
        assertEquals("23:00 – 24:00", b.saatMetni())
    }

    // ══════════════════════════════════════════════════════════
    // Takvim etkinliği
    // ══════════════════════════════════════════════════════════

    @Test
    fun `etkinlik suresi makul sinirlarda`() {
        val simdi = System.currentTimeMillis()
        // 10 saniyelik etkinlik → en az 15 dk çizilmeli
        val kisa = TakvimKopru.Etkinlik(1, "a", simdi, simdi + 10_000L, false, 0, false)
        assertTrue("Çok kısa etkinlik en az 15 dk olmalı", kisa.sureDk >= 15)

        // 3 günlük etkinlik → en fazla 8 saat çizilmeli
        val uzun = TakvimKopru.Etkinlik(2, "b", simdi, simdi + 259_200_000L, false, 0, false)
        assertTrue("Çok uzun etkinlik 480 dk ile sınırlı olmalı", uzun.sureDk <= 480)
    }
}
