package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.49 — 8 Aşırı İşlevsel Görünüm Ayarı (#2, #3, #5, #6, #7, #8, #9, #10) saf mantık ve sınır testleri (12 test).
 */
class GorunumAtolyeTest {

    @Test
    fun `kart modu sinir icinde degeri korur`() {
        assertEquals(1, GorunumAyar.kartModuSinirla(1))
    }

    @Test
    fun `kart modu sinir disinda 0-2 araligina kelepce atar`() {
        assertEquals(0, GorunumAyar.kartModuSinirla(-5))
        assertEquals(2, GorunumAyar.kartModuSinirla(10))
    }

    @Test
    fun `oncelik vurgu sinirlari 0-2 dondurur`() {
        assertEquals(0, GorunumAyar.oncelikVurguSinirla(0))
        assertEquals(2, GorunumAyar.oncelikVurguSinirla(99))
    }

    @Test
    fun `font sablon sinirlari 0-2 korur`() {
        assertEquals(2, GorunumAyar.fontSablonSinirla(2))
        assertEquals(0, GorunumAyar.fontSablonSinirla(-1))
    }

    @Test
    fun `yazi yuzdesi 80 altini 80e tavanlar`() {
        assertEquals(80, GorunumAyar.yaziYuzdesiSinirla(50))
    }

    @Test
    fun `yazi yuzdesi 150 ustunu 150ye kelepceler`() {
        assertEquals(150, GorunumAyar.yaziYuzdesiSinirla(200))
        assertEquals(100, GorunumAyar.yaziYuzdesiSinirla(100))
    }

    @Test
    fun `satir nefesi dp 0 ile 16 arasina kelepcelenir`() {
        assertEquals(0, GorunumAyar.satirNefesiDpSinirla(-2))
        assertEquals(16, GorunumAyar.satirNefesiDpSinirla(25))
        assertEquals(6, GorunumAyar.satirNefesiDpSinirla(6))
    }

    @Test
    fun `acilis ekran kimligi 0-6 araligini kelepceler`() {
        assertEquals(6, GorunumAyar.acilisEkranSinirla(8))
        assertEquals(0, GorunumAyar.acilisEkranSinirla(-3))
    }

    @Test
    fun `fab islev secimi 0-3 araligini kelepceler`() {
        assertEquals(3, GorunumAyar.fabIslevSinirla(9))
        assertEquals(0, GorunumAyar.fabIslevSinirla(0))
    }

    @Test
    fun `yuzen serit metni uret calisan sayac varken kalan sureyi dondurur`() {
        val metin = GorunumAyar.yuzenSeritMetniUret(true, 18 * 60_000L + 42_000L, 5)
        assertEquals("⏱ Odak: 18:42 kaldı", metin)
    }

    @Test
    fun `yuzen serit metni uret sayac yokken seri bilgisini dondurur`() {
        val metin = GorunumAyar.yuzenSeritMetniUret(false, 0L, 14)
        assertEquals("🔥 Gün seriniz: 14 gün güvende", metin)
    }

    @Test
    fun `yuzen serit metni uret ikisi de yokken otopilot bilgisini dondurur`() {
        val metin = GorunumAyar.yuzenSeritMetniUret(false, 0L, 0)
        assertEquals("🤖 AI Otopilot & Ajan Aktif", metin)
    }
}
