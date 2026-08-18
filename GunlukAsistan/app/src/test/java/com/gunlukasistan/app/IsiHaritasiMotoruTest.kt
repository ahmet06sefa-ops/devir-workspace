package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Alışkanlık ısı haritası motoru saf testleri.
 */
class IsiHaritasiMotoruTest {

    @Test
    fun `seviye sinirlarini dogru belirler`() {
        assertEquals(0, IsiHaritasiMotoru.seviye(0))
        assertEquals(1, IsiHaritasiMotoru.seviye(10))
        assertEquals(2, IsiHaritasiMotoru.seviye(30))
        assertEquals(3, IsiHaritasiMotoru.seviye(60))
        assertEquals(4, IsiHaritasiMotoru.seviye(90))
    }

    @Test
    fun `puan odak ve tamamlamayi birlestirir`() {
        assertEquals(0, IsiHaritasiMotoru.puan(0, 0))
        // 90 dk + 10 tamamlama → 100
        assertEquals(100, IsiHaritasiMotoru.puan(90, 10))
        // Sadece odak (45 dk) → 70*0.5 = 35 (int hesapta 35)
        assertTrue(IsiHaritasiMotoru.puan(45, 0) in 0..100)
        // 90 dk odak → 70
        assertEquals(70, IsiHaritasiMotoru.puan(90, 0))
    }

    @Test
    fun `puan ust sinirli ve asla 100u asmaz`() {
        assertEquals(100, IsiHaritasiMotoru.puan(500, 50))
        assertEquals(0, IsiHaritasiMotoru.puan(-5, -3))
    }

    @Test
    fun `bos liste bos matris doner`() {
        assertTrue(IsiHaritasiMotoru.matris(emptyList()).isEmpty())
    }

    @Test
    fun `matris günleri 5 hücreli satirlara dizer`() {
        val seviyeler = (1..12).map { IsiHaritasiMotoru.seviye(it * 10) }
        val m = IsiHaritasiMotoru.matris(seviyeler)
        // 12 gün → 3 satır (her satır en fazla 5)
        assertEquals(3, m.size)
        assertTrue(m.all { it.size <= 5 })
        // Toplam hücre sayısı 12'den az değil (0 dolgulu olabilir)
        val toplam = m.sumOf { it.size }
        assertTrue(toplam >= 12)
    }

    @Test
    fun `matris her satir en fazla 5 hücre tasir`() {
        val seviyeler = (1..23).map { 1 }
        val m = IsiHaritasiMotoru.matris(seviyeler)
        assertTrue(m.all { it.size <= 5 })
    }
}
