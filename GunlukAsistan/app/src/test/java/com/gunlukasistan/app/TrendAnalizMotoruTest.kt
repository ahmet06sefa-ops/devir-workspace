package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Trend / eğilim analiz motoru saf testleri.
 */
class TrendAnalizMotoruTest {

    @Test
    fun `ort bos seride 0 doner`() {
        assertEquals(0.0, TrendAnalizMotoru.ort(emptyList()), 0.001)
    }

    @Test
    fun `ort seriyi dogru ortalar`() {
        assertEquals(3.0, TrendAnalizMotoru.ort(listOf(1, 2, 3, 4, 5)), 0.001)
    }

    @Test
    fun `artan seri artis egilimi verir`() {
        assertEquals(1, TrendAnalizMotoru.egilim(listOf(10, 20, 30, 40, 50)))
    }

    @Test
    fun `azalan seri dusus egilimi verir`() {
        assertEquals(-1, TrendAnalizMotoru.egilim(listOf(50, 40, 30, 20, 10)))
    }

    @Test
    fun `sabit seri sabit egilim verir`() {
        assertEquals(0, TrendAnalizMotoru.egilim(listOf(30, 31, 30, 32, 31)))
    }

    @Test
    fun `tek element seri sabit egilim verir`() {
        assertEquals(0, TrendAnalizMotoru.egilim(listOf(30)))
    }

    @Test
    fun `egilim metni degerlere gore degisir`() {
        assertTrue(TrendAnalizMotoru.egilimMetni(1).contains("Artıyor"))
        assertTrue(TrendAnalizMotoru.egilimMetni(-1).contains("Azalıyor"))
        assertTrue(TrendAnalizMotoru.egilimMetni(0).contains("Sabit"))
    }

    @Test
    fun `sonraki tahmin ortalama kadar`() {
        assertEquals(3, TrendAnalizMotoru.sonrakiTahmin(listOf(1, 2, 3, 4, 5)))
        assertEquals(0, TrendAnalizMotoru.sonrakiTahmin(emptyList()))
    }

    @Test
    fun `rapor ortalamayı ve tahmini icerir`() {
        val r = TrendAnalizMotoru.rapor(listOf(10, 20, 30, 40, 50))
        assertTrue(r.contains("30"))
        assertTrue(r.contains("Tahmini"))
    }
}
