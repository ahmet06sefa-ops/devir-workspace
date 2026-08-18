package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Proaktif Koç, Haftalık Rapor ve Akıllı Günlük Plan saf testleri.
 */
class KocOzellikleriTest {

    // ── KocMotoru ──

    @Test
    fun `dilim saate gore dogru doner`() {
        assertEquals(KocMotoru.Dilim.SABAH, KocMotoru.dilim(7))
        assertEquals(KocMotoru.Dilim.OGLE, KocMotoru.dilim(13))
        assertEquals(KocMotoru.Dilim.AKSAM, KocMotoru.dilim(19))
        assertEquals(KocMotoru.Dilim.GECE, KocMotoru.dilim(23))
    }

    @Test
    fun `sabah mesaji bekleyen gorev sayisini yansitir`() {
        val m = KocMotoru.mesaj(KocMotoru.Dilim.SABAH, 5, 0, 0, 60, 0)
        assertTrue(m.contains("5 görev"))
    }

    @Test
    fun `aksam mesaji tamamlanan gorev sayisini icerir`() {
        val m = KocMotoru.mesaj(KocMotoru.Dilim.AKSAM, 2, 6, 80, 90, 3)
        assertTrue(m.contains("6 görev"))
    }

    @Test
    fun `aksam mesaji odak hedef altindayken odak bilgisi icerir`() {
        val m = KocMotoru.mesaj(KocMotoru.Dilim.AKSAM, 2, 1, 40, 90, 0)
        assertTrue(m.contains("40"))
        assertTrue(m.contains("90"))
    }

    @Test
    fun `gece mesaji seriyi icerir`() {
        val m = KocMotoru.mesaj(KocMotoru.Dilim.GECE, 0, 0, 0, 0, 7)
        assertTrue(m.contains("7 günlük serin"))
    }

    @Test
    fun `baslik dilime gore degisir`() {
        assertEquals("☀️ Koçunuz: Günaydın", KocMotoru.baslik(KocMotoru.Dilim.SABAH))
    }

    // ── HaftalikKocRaporu ──

    @Test
    fun `rapor odak derecesi ve veri icerir`() {
        val r = HaftalikKocRaporu.satinAl(HaftalikKocRaporu.Hafta(800, 700, 12, 40, 5))
        assertTrue(r.contains("★★★★☆"))
        assertTrue(r.contains("800 dk"))
        assertTrue(r.contains("12 tanesini"))
    }

    @Test
    fun `derece farkli seviyelerde farkli etiket verir`() {
        assertTrue(HaftalikKocRaporu.derece(1400).contains("★★★★★"))
        assertTrue(HaftalikKocRaporu.derece(100).contains("★☆☆☆☆"))
    }

    @Test
    fun `rapor seri yorumu icerir`() {
        val r = HaftalikKocRaporu.satinAl(HaftalikKocRaporu.Hafta(600, 600, 8, 30, 6))
        assertTrue(r.contains("kopma"))
    }

    // ── AkilliGunlukPlan ──

    @Test
    fun `bos gorev bos plan doner`() {
        assertTrue(AkilliGunlukPlan.plan(emptyList()).isEmpty())
    }

    @Test
    fun `plan gorevleri gun icine yayar ve mola ekler`() {
        val plan = AkilliGunlukPlan.plan(listOf("A", "B", "C"), 540, 900, 50)
        assertEquals(3, plan.size)
        assertEquals(540, plan[0].bas)
        assertEquals(590, plan[0].bit)   // 540+50
        assertEquals(600, plan[1].bas)   // 590+10 mola
    }

    @Test
    fun `plan gun bitisini asmaz`() {
        val plan = AkilliGunlukPlan.plan(listOf("A", "B", "C", "D", "E"), 540, 700, 50)
        assertTrue(plan.all { it.bit <= 700 })
    }

    @Test
    fun `saat ve metin cevirme dogru calisir`() {
        assertEquals("09:00", AkilliGunlukPlan.saat(540))
        val plan = AkilliGunlukPlan.plan(listOf("Ders"), 540, 900, 50)
        assertTrue(AkilliGunlukPlan.metneCevir(plan).contains("09:00-09:50"))
    }
}
