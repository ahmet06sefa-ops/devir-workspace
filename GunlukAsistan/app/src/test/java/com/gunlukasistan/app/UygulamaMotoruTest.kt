package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Telefon uygulamalarına erişim motoru (`UygulamaMotoru`) saf JVM testleri.
 * Arama/eşleşme/kategori mantığı Android gerektirmez.
 */
class UygulamaMotoruTest {

    private val ornek = listOf(
        UygulamaMotoru.Uygulama("com.whatsapp", "WhatsApp"),
        UygulamaMotoru.Uygulama("com.google.android.youtube", "YouTube"),
        UygulamaMotoru.Uygulama("com.spotify.music", "Spotify"),
        UygulamaMotoru.Uygulama("com.google.android.apps.maps", "Haritalar"),
        UygulamaMotoru.Uygulama("com.google.android.gm", "Gmail")
    )

    @Test
    fun `normalle turkce karakterleri asciiye cevirir`() {
        assertEquals("isbilgisayari", UygulamaMotoru.normalle("İşBilgisayarı"))
        assertEquals("youtube", UygulamaMotoru.normalle("YouTube"))
    }

    @Test
    fun `bos arama tum listeyi orijinal sirayla doner`() {
        assertEquals(ornek.size, UygulamaMotoru.filtrle(ornek, "").size)
        assertEquals(ornek, UygulamaMotoru.filtrle(ornek, "   "))
    }

    @Test
    fun `arama adin baslangicini onceler`() {
        val sonuc = UygulamaMotoru.filtrle(ornek, "you")
        assertTrue(sonuc.isNotEmpty())
        assertEquals("com.google.android.youtube", sonuc.first().paket)
    }

    @Test
    fun `arama paket adiyla da eslesir`() {
        val sonuc = UygulamaMotoru.filtrle(ornek, "spotify")
        assertEquals("com.spotify.music", sonuc.first().paket)
    }

    @Test
    fun `eslesme tek bir uygulamayi dondurur`() {
        assertEquals("com.whatsapp", UygulamaMotoru.eslesme(ornek, "WhatsApp")?.paket)
        assertEquals("com.google.android.youtube", UygulamaMotoru.eslesme(ornek, "youtube")?.paket)
    }

    @Test
    fun `eslesme bulamayinca null doner`() {
        assertNull(UygulamaMotoru.eslesme(ornek, "varolmayanuygulama"))
    }

    @Test
    fun `kategori pakete gore dogru atanir`() {
        assertEquals("Mesajlaşma", UygulamaMotoru.kategori("com.whatsapp"))
        assertEquals("Video", UygulamaMotoru.kategori("com.google.android.youtube"))
        assertEquals("Müzik", UygulamaMotoru.kategori("com.spotify.music"))
        assertEquals("Navigasyon", UygulamaMotoru.kategori("com.google.android.apps.maps"))
    }

    @Test
    fun `oncelik puan eslesmeyen icin sifirdir`() {
        assertEquals(0, UygulamaMotoru.oncelikPuan(ornek[0], "zil sesi"))
    }

    @Test
    fun `tum terimler birden cok kelime ile eslesir`() {
        val sonuc = UygulamaMotoru.tumTerimler(ornek, "google youtube")
        assertTrue(sonuc.any { it.paket.contains("youtube") })
    }
}
