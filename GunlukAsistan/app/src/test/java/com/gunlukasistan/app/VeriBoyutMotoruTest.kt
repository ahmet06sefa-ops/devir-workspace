package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Veri boyutu / depolama temizlik asistanı saf testleri.
 */
class VeriBoyutMotoruTest {

    @Test
    fun `boyut metni bayt icin B kullanir`() {
        assertEquals("512 B", VeriBoyutMotoru.boyutMetni(512))
    }

    @Test
    fun `boyut metni KB icin KB kullanir`() {
        assertEquals("1.5 KB", VeriBoyutMotoru.boyutMetni(1536))
    }

    @Test
    fun `boyut metni MB icin MB kullanir`() {
        assertEquals("2.0 MB", VeriBoyutMotoru.boyutMetni(2 * 1024 * 1024))
    }

    @Test
    fun `toplam bayt tum kalemleri toplar`() {
        val k = listOf(
            VeriBoyutMotoru.Kalem("a", "A", 100, true),
            VeriBoyutMotoru.Kalem("b", "B", 50, false)
        )
        assertEquals(150, VeriBoyutMotoru.toplamBayt(k))
    }

    @Test
    fun `temizlenebilir toplam yalnizca temizlenebilirleri sayar`() {
        val k = listOf(
            VeriBoyutMotoru.Kalem("a", "A", 100, true),
            VeriBoyutMotoru.Kalem("b", "B", 50, false)
        )
        assertEquals(100, VeriBoyutMotoru.temizlenebilirToplam(k))
    }

    @Test
    fun `onerilen temizlenebilirleri buyukten kucuge dizer`() {
        val k = listOf(
            VeriBoyutMotoru.Kalem("a", "A", 10, true),
            VeriBoyutMotoru.Kalem("b", "B", 500, true),
            VeriBoyutMotoru.Kalem("c", "C", 999, false)
        )
        val onerilen = VeriBoyutMotoru.onerilen(k)
        assertEquals(2, onerilen.size)
        assertEquals("B", onerilen[0].ad)
        assertEquals("A", onerilen[1].ad)
    }

    @Test
    fun `ozet toplam ve temizlenebilir icerir`() {
        val k = listOf(VeriBoyutMotoru.Kalem("a", "A", 100, true))
        val ozet = VeriBoyutMotoru.ozet(k)
        assertTrue(ozet.contains("Toplam"))
        assertTrue(ozet.contains("Temizlenebilir"))
    }
}
