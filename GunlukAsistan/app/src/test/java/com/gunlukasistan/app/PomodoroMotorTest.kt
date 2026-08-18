package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.14 — Pomodoro / Verimlilik Motoru ([PomodoroMotoru]) saf birim testleri.
 */
class PomodoroMotorTest {

    @Test
    fun `sure donustur 0 dakika icin 0 dk dondurur`() {
        assertEquals("0 dk", PomodoroMotoru.sureDonustur(0))
    }

    @Test
    fun `sure donustur 25 dakika icin 25 dk dondurur`() {
        assertEquals("25 dk", PomodoroMotoru.sureDonustur(25))
    }

    @Test
    fun `sure donustur 75 dakika icin 1 15 formatinda dondurur`() {
        assertEquals("1:15", PomodoroMotoru.sureDonustur(75))
    }

    @Test
    fun `sure donustur negatif degeri sifira cevirir`() {
        assertEquals("0 dk", PomodoroMotoru.sureDonustur(-10))
    }

    @Test
    fun `verimlilik skoru 120 dakika ve 8 gorevde 100 yapar`() {
        assertEquals(100, PomodoroMotoru.verimlilikSkoru(120, 8))
    }

    @Test
    fun `verimlilik skoru sifir girdide sifir yapar`() {
        assertEquals(0, PomodoroMotoru.verimlilikSkoru(0, 0))
    }

    @Test
    fun `verimlilik skoru 0 ile 100 arasinda kalir`() {
        assertTrue(PomodoroMotoru.verimlilikSkoru(9999, 9999) <= 100)
        assertTrue(PomodoroMotoru.verimlilikSkoru(-5, -5) >= 0)
    }

    @Test
    fun `verimlilik skoru odak agirlikli hesaplanir`() {
        val yuksekOdak = PomodoroMotoru.verimlilikSkoru(120, 0)
        val yuksekGorev = PomodoroMotoru.verimlilikSkoru(0, 8)
        assertTrue(yuksekOdak > yuksekGorev)
    }

    @Test
    fun `yildiz 90 ustu icin 5 dondurur`() {
        assertEquals(5, PomodoroMotoru.yildiz(92))
    }

    @Test
    fun `yildiz 70 ile 89 arasi icin 4 dondurur`() {
        assertEquals(4, PomodoroMotoru.yildiz(75))
    }

    @Test
    fun `yildiz sifir icin 0 dondurur`() {
        assertEquals(0, PomodoroMotoru.yildiz(0))
    }

    @Test
    fun `blok sayisi 50 dakika icin 2 dondurur`() {
        assertEquals(2, PomodoroMotoru.blokSayisi(50))
    }

    @Test
    fun `blok sayisi 25 dakika icin 1 dondurur`() {
        assertEquals(1, PomodoroMotoru.blokSayisi(25))
    }

    @Test
    fun `blok sayisi 20 dakika icin 0 dondurur`() {
        assertEquals(0, PomodoroMotoru.blokSayisi(20))
    }

    @Test
    fun `mola onerisi 4 blokta uzun mola verir`() {
        val m = PomodoroMotoru.molaOnerisi(4)
        assertEquals(20, m.sure)
        assertEquals("uzun", m.tur)
    }

    @Test
    fun `mola onerisi 1 blokta kisa mola verir`() {
        val m = PomodoroMotoru.molaOnerisi(1)
        assertEquals(5, m.sure)
        assertEquals("kisa", m.tur)
    }

    @Test
    fun `mola onerisi 8 blokta tekrar uzun mola verir`() {
        assertEquals("uzun", PomodoroMotoru.molaOnerisi(8).tur)
    }

    @Test
    fun `gun odak plani 0 dakikada yetersiz mesaj verir`() {
        assertTrue(PomodoroMotoru.gunIcinOdakPlani(0).contains("kalmamış"))
    }

    @Test
    fun `gun odak plani 50 dakikada 2 blok icerir`() {
        assertTrue(PomodoroMotoru.gunIcinOdakPlani(50).contains("2 odak bloğu"))
    }

    @Test
    fun `gun odak plani mola satirlari icerir`() {
        val plan = PomodoroMotoru.gunIcinOdakPlani(75)
        assertTrue(plan.contains("odak →"))
    }

    @Test
    fun `yorum yuksek skorda motive eder`() {
        assertTrue(PomodoroMotoru.yorum(95).isNotBlank())
    }
}
