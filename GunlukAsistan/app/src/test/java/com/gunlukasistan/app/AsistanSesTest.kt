package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Sesli asistan (`AsistanSes`) saf JVM testleri.
 *
 * [konusmaMetni] mantığını doğrular: emoji/özel simgeler ayıklanır, çoklu
 * boşluk tekilleşir, uzun metin sınırlanır — TTS okunabilir çıktı üretilir.
 */
class AsistanSesTest {

    @Test
    fun `bos ve blank metin bos doner`() {
        assertEquals("", AsistanSes.konusmaMetni(""))
        assertEquals("", AsistanSes.konusmaMetni("   "))
        assertEquals("", AsistanSes.konusmaMetni("\n\t\n"))
    }

    @Test
    fun `duz metin oldugu gibi korunur`() {
        assertEquals("Bugün üç görev var", AsistanSes.konusmaMetni("Bugün üç görev var"))
    }

    @Test
    fun `onbasindaki isaret simgesi ayiklanir`() {
        // "\u2713 Yap\u0131ld\u0131" → onay işareti simgesi (So) temizlenir
        assertEquals("Yapıldı", AsistanSes.konusmaMetni("\u2713 Yapıldı"))
        // "⭐ teşekkür" gibi yıldız simgesi de temizlenir
        assertFalse(AsistanSes.konusmaMetni("\u2B50 teşekkür").startsWith("\u2B50"))
    }

    @Test
    fun `emoji ayiklanir`() {
        val sonuc = AsistanSes.konusmaMetni("Merhaba 🤖 nasılsın 😊")
        assertEquals("Merhaba nasılsın", sonuc)
    }

    @Test
    fun `coklu bosluk tekilesir`() {
        assertEquals("Tek satır metin", AsistanSes.konusmaMetni("Tek   satır \n metin"))
    }

    @Test
    fun `cok uzun metin sinirlanir ve uc nokta eklenir`() {
        val uzun = "x".repeat(2000)
        val sonuc = AsistanSes.konusmaMetni(uzun)
        assertTrue(sonuc.length <= 401)   // 400 + "…"
        assertTrue(sonuc.endsWith("…"))
    }

    @Test
    fun `kisa metin sinir icinde kalir uc nokta eklenmez`() {
        val sonuc = AsistanSes.konusmaMetni("kısa ve öz")
        assertEquals("kısa ve öz", sonuc)
    }
}
