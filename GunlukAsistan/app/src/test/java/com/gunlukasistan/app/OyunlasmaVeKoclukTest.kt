package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Oyunlaştırma (XP/seviye) ve hazır koçluk programları saf testleri.
 */
class OyunlasmaVeKoclukTest {

    // ── OyunlasmaMotoru ──

    @Test
    fun `seviye 1 baslangictadir`() {
        assertEquals(1, OyunlasmaMotoru.seviye(0))
        assertEquals(1, OyunlasmaMotoru.seviye(50))
    }

    @Test
    fun `xpHedefi seviyeyle artar`() {
        assertTrue(OyunlasmaMotoru.xpHedef(2) > OyunlasmaMotoru.xpHedef(1))
        assertEquals(100, OyunlasmaMotoru.xpHedef(1))
        assertEquals(125, OyunlasmaMotoru.xpHedef(2))
    }

    @Test
    fun `yeterli xp ile seviye atlanir`() {
        // Seviye 1: 100 XP gerekir → 100 XP = seviye 2
        assertEquals(2, OyunlasmaMotoru.seviye(100))
        // 100+125 = 225 → seviye 3
        assertEquals(3, OyunlasmaMotoru.seviye(225))
    }

    @Test
    fun `seviyedeIlerleme 0 ile 1 arasindadir`() {
        assertTrue(OyunlasmaMotoru.seviyedeIlerleme(0) in 0f..1f)
        assertTrue(OyunlasmaMotoru.seviyedeIlerleme(50) in 0f..1f)
        assertTrue(OyunlasmaMotoru.seviyedeIlerleme(100) in 0f..1f)
    }

    @Test
    fun `eylem xp degerleri pozitiftir`() {
        assertTrue(OyunlasmaMotoru.gorevXp() > 0)
        assertTrue(OyunlasmaMotoru.aliskanlikXp() > 0)
        assertEquals(25, OyunlasmaMotoru.odakXp(25))
    }

    @Test
    fun `rutbe seviyeye gore degisir`() {
        assertTrue(OyunlasmaMotoru.rutbe(1).contains("Başlangıç"))
        assertTrue(OyunlasmaMotoru.rutbe(35).contains("Efsane"))
    }

    // ── KoclukProgramlari ──

    @Test
    fun `varsayilan programlar bos degildir`() {
        assertTrue(KoclukProgramlari.varsayilanlar.isNotEmpty())
        assertTrue(KoclukProgramlari.varsayilanlar.any { it.id == "ders_aliskani" })
    }

    @Test
    fun `program gün görevini dogru doner`() {
        val p = KoclukProgramlari.bul("erken_kalk")!!
        assertEquals(7, p.toplamGun)
        assertTrue(KoclukProgramlari.gunGorevi(p, 1).contains("07:00"))
    }

    @Test
    fun `gun gorevi siniri asarsa son gorevi doner`() {
        val p = KoclukProgramlari.bul("odak_ustasi")!!
        assertEquals(p.gunler.last(), KoclukProgramlari.gunGorevi(p, 999))
    }

    @Test
    fun `ilerleme yuzdesi dogru hesaplanir`() {
        assertEquals(0, KoclukProgramlari.ilerlemeYuzde(0, 10))
        assertEquals(50, KoclukProgramlari.ilerlemeYuzde(5, 10))
        assertEquals(100, KoclukProgramlari.ilerlemeYuzde(10, 10))
        assertEquals(0, KoclukProgramlari.ilerlemeYuzde(0, 0))
    }

    @Test
    fun `program arama calisir`() {
        assertTrue(KoclukProgramlari.ara("ders")?.id == "ders_aliskani")
        assertEquals(null, KoclukProgramlari.ara("olmayan program"))
    }
}
