package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.24 · İleri Sayım — saf mantık birim testleri.
 *
 * Android yok; saatler enjekte edilir. Durum geçişleri elle zincirlenir:
 * "başlat → bekle → ekranı kapat → devam → bitir" senaryosu tam koşulur.
 */
class IleriSayimTest {

    private fun sifir() = IleriSayim.Durum(calisiyor = false, birikenMs = 0L, baslangicMs = 0L)

    // ── gecenMs ───────────────────────────────────────────────

    @Test
    fun `bekleyen durum yalniz birikim dondurur`() {
        val d = IleriSayim.Durum(calisiyor = false, birikenMs = 90_000L, baslangicMs = 0L)
        assertEquals(90_000L, IleriSayim.gecenMs(d, 999_999L))
    }

    @Test
    fun `calisiyorken birikime canli bolum eklenir`() {
        val d = IleriSayim.Durum(calisiyor = true, birikenMs = 60_000L, baslangicMs = 1_000L)
        assertEquals(65_000L, IleriSayim.gecenMs(d, 6_000L))
    }

    @Test
    fun `saat kaymasinda canli bolum sifira kenetlenir - negatif sure yok`() {
        val d = IleriSayim.Durum(calisiyor = true, birikenMs = 60_000L, baslangicMs = 10_000L)
        assertEquals(60_000L, IleriSayim.gecenMs(d, 5_000L))
    }

    // ── anaDugmeGecis ─────────────────────────────────────────

    @Test
    fun `baslat ve devam calisiyor yapar birikim korunur`() {
        val once = IleriSayim.Durum(calisiyor = false, birikenMs = 45_000L, baslangicMs = 0L)
        val sonra = IleriSayim.anaDugmeGecis(once, 100_000L)
        assertTrue(sonra.calisiyor)
        assertEquals(45_000L, sonra.birikenMs)
        assertEquals(100_000L, sonra.baslangicMs)
    }

    @Test
    fun `bekle bolumu birikime katlar baslangic sifirlar`() {
        val kosan = IleriSayim.Durum(calisiyor = true, birikenMs = 45_000L, baslangicMs = 100_000L)
        val duran = IleriSayim.anaDugmeGecis(kosan, 130_000L)
        assertFalse(duran.calisiyor)
        assertEquals(75_000L, duran.birikenMs)
        assertEquals(0L, duran.baslangicMs)
    }

    @Test
    fun `beklemede saat kaymasi birikimi sisirmez`() {
        val kosan = IleriSayim.Durum(calisiyor = true, birikenMs = 45_000L, baslangicMs = 100_000L)
        val duran = IleriSayim.anaDugmeGecis(kosan, 50_000L) // simdi < baslangic
        assertEquals(45_000L, duran.birikenMs)
    }

    @Test
    fun `tam senaryo - baslat bekle ekran kapat devam bitir`() {
        // t=0 başlat → t=30sn bekle → (ekran kapalı geçen 10 dk ÖNEMLİ DEĞİL,
        // beklemede süre donar) → t=600sn devam → t=630sn ölçüm = 60 sn
        var d = sifir()
        d = IleriSayim.anaDugmeGecis(d, 0L)              // başlat @0
        d = IleriSayim.anaDugmeGecis(d, 30_000L)         // bekle @30sn → birikim 30sn
        assertEquals(30_000L, IleriSayim.gecenMs(d, 700_000L)) // donmuş: 11 dk sonra da 30sn
        d = IleriSayim.anaDugmeGecis(d, 600_000L)        // devam @600sn
        assertEquals(60_000L, IleriSayim.gecenMs(d, 630_000L)) // 30 + 30
        assertEquals(1, IleriSayim.dakikayaDonustur(IleriSayim.gecenMs(d, 630_000L)))
    }

    // ── dakika / onay / oturum ────────────────────────────────

    @Test
    fun `dakika dosemesi sinirlar`() {
        assertEquals(0, IleriSayim.dakikayaDonustur(0L))
        assertEquals(0, IleriSayim.dakikayaDonustur(59_999L))
        assertEquals(1, IleriSayim.dakikayaDonustur(60_000L))
        assertEquals(2, IleriSayim.dakikayaDonustur(125_000L))
        assertEquals(60, IleriSayim.dakikayaDonustur(3_600_000L))
    }

    @Test
    fun `uzun oturum onayi - 480 dk serbest ustunde sor`() {
        assertFalse(IleriSayim.onayGerekliMi(1))
        assertFalse(IleriSayim.onayGerekliMi(480))
        assertTrue(IleriSayim.onayGerekliMi(481))
    }

    @Test
    fun `oturum varligi - yalniz sifir durum bos sayilir`() {
        assertFalse(IleriSayim.oturumVarMi(sifir()))
        assertTrue(IleriSayim.oturumVarMi(sifir().copy(birikenMs = 1L)))
        assertTrue(IleriSayim.oturumVarMi(sifir().copy(calisiyor = true)))
    }

    // ── v10.26 (öneri #61): oturum adı normalleştirme ─────────

    @Test
    fun `adTemiz - kenar bosluklar atilir ic bosluk teklesin`() {
        assertEquals("Matematik 2. ünite", IleriSayim.adTemiz("  Matematik   2. ünite  "))
        assertEquals("ders", IleriSayim.adTemiz("\t ders \n"))
        assertEquals("a b c", IleriSayim.adTemiz("a    b\t\tc"))
    }

    @Test
    fun `adTemiz - bos ve beyazlik isimsiz oturum demek`() {
        assertEquals("", IleriSayim.adTemiz(""))
        assertEquals("", IleriSayim.adTemiz("      "))
        assertEquals("", IleriSayim.adTemiz("\n\t "))
    }

    @Test
    fun `adTemiz - ust sinirda keser`() {
        val uzun = "x".repeat(200)
        assertEquals(IleriSayim.AD_SINIR, IleriSayim.adTemiz(uzun).length)
        // Sinir + sona gelen boşluk tekrar kırpılır
        val kenarli = "a".repeat(59) + " bcdef"
        assertEquals(59, IleriSayim.adTemiz(kenarli).length)
    }
}
