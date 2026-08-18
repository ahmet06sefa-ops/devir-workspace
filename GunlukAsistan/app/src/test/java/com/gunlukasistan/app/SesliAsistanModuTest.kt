package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Kesintisiz sesli asistan çevrimi (`SesliAsistanModu`) saf JVM testleri.
 *
 * Konuşma döngüsü kararlarını doğrular: temizleme, tur sınırı, "tekrar dinle"
 * politikası ve TTS için sözlü cevap üretimi.
 */
class SesliAsistanModuTest {

    @Test
    fun `sesli soru temizleme bosluklari birlestirir ve uclari keser`() {
        assertEquals("Bugün ders çalış", SesliAsistanModu.sesliSoruTemizle("  Bugün   ders  çalış  "))
    }

    @Test
    fun `bos ses bos soru doner`() {
        assertEquals("", SesliAsistanModu.sesliSoruTemizle("   "))
    }

    @Test
    fun `oturum aktifken ve tur sinirinin altindayken tekrar dinler`() {
        assertTrue(SesliAsistanModu.surekliDinlemeliMi(oturumAktif = true, tur = 0))
        assertTrue(SesliAsistanModu.surekliDinlemeliMi(oturumAktif = true, tur = SesliAsistanModu.MAKS_TUR - 1))
    }

    @Test
    fun `oturum kapaliyken veya tur sinirinda tekrar dinlemez`() {
        assertFalse(SesliAsistanModu.surekliDinlemeliMi(oturumAktif = false, tur = 0))
        assertFalse(SesliAsistanModu.surekliDinlemeliMi(oturumAktif = true, tur = SesliAsistanModu.MAKS_TUR))
    }

    @Test
    fun `tur sinir isareti ve sayac dogru calisir`() {
        assertFalse(SesliAsistanModu.turSiniri(SesliAsistanModu.MAKS_TUR - 1))
        assertTrue(SesliAsistanModu.turSiniri(SesliAsistanModu.MAKS_TUR))
        assertEquals(3, SesliAsistanModu.yeniTur(2))
    }

    @Test
    fun `konusulabilir cevap on ekli isaretleri temizler`() {
        assertEquals("Görev eklendi", SesliAsistanModu.konusulabilirCevap("\u2713 Görev eklendi"))
        assertEquals("Not alındı", SesliAsistanModu.konusulabilirCevap("✓ Not alındı"))
        assertEquals("Plan hazır", SesliAsistanModu.konusulabilirCevap("✅ Plan hazır"))
    }

    @Test
    fun `konusulabilir cevap emoji ve kod bloklarini temizler`() {
        val s = SesliAsistanModu.konusulabilirCevap("```\nMerhaba 🤖 nasılsın?\n```")
        assertEquals("Merhaba nasılsın?", s)
    }

    @Test
    fun `konusulabilir cevap madde baslarini duz metne cevirir`() {
        val s = SesliAsistanModu.konusulabilirCevap("• Bugün plan yap\n• Ders çalış")
        assertEquals("Bugün plan yap Ders çalış", s)
    }

    @Test
    fun `sesli cevap istemi soruyu icerir`() {
        val istem = SesliAsistanModu.sesliCevapIstemi("bugün ne çalışayım")
        assertTrue(istem.contains("koç"))
        assertTrue(istem.contains("bugün ne çalışayım"))
        assertTrue(istem.contains("sesli"))
    }

    @Test
    fun `bos cevap bos doner`() {
        assertEquals("", SesliAsistanModu.konusulabilirCevap(""))
        assertEquals("", SesliAsistanModu.konusulabilirCevap("   "))
    }
}
