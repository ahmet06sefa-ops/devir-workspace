package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Rakiplerde olup eklenen yeni çekirdeklerin saf testleri:
 *  · [SenkronMotoru] — bulut senkron / hesap
 *  · [SaglikVeriMotoru] — Google Fit tarzı adım/aktivite
 *  · [TakvimPlanlamaMotoru] — takvimde görev planlama
 */
class YeniRakipFarkiTest {

    // ── SenkronMotoru ──

    @Test
    fun `birles daha yeni paketi kazandirir`() {
        val eski = SenkronMotoru.VeriPaketi("gorevler", "[1]", 100L)
        val yeni = SenkronMotoru.VeriPaketi("gorevler", "[2]", 200L)
        assertEquals(yeni, SenkronMotoru.birles(eski, yeni))
    }

    @Test
    fun `cakisma ayni anahtar farkli icerik esit zaman damgasi`() {
        val a = SenkronMotoru.VeriPaketi("notlar", "a", 100L)
        val b = SenkronMotoru.VeriPaketi("notlar", "b", 100L)
        assertTrue(SenkronMotoru.cakismaMi(a, b))
    }

    @Test
    fun `cakisma farkli zaman damgasinda olmaz`() {
        val a = SenkronMotoru.VeriPaketi("notlar", "a", 100L)
        val b = SenkronMotoru.VeriPaketi("notlar", "b", 200L)
        assertFalse(SenkronMotoru.cakismaMi(a, b))
    }

    @Test
    fun `enYeniListe ayni anahtarin en yenisini tutar`() {
        val list = listOf(
            SenkronMotoru.VeriPaketi("a", "1", 100L),
            SenkronMotoru.VeriPaketi("a", "2", 300L),
            SenkronMotoru.VeriPaketi("a", "3", 200L)
        )
        val sonuc = SenkronMotoru.enYeniListe(list)
        assertEquals(1, sonuc.size)
        assertEquals("2", sonuc[0].json)
    }

    @Test
    fun `istek govdesi surum ve hesap icerir`() {
        val hesap = SenkronMotoru.Hesap("1", "Ahmet", "a@x.com", 100L)
        val json = SenkronMotoru.istekGovdesi(hesap, "Telefon", emptyList())
        assertTrue(json.contains("\"surum\":1"))
        assertTrue(json.contains("\"hesapId\":\"1\""))
        assertTrue(json.contains("\"cihaz\":\"Telefon\""))
        assertTrue(json.contains("\"paketler\":[]"))
    }

    // ── SaglikVeriMotoru ──

    @Test
    fun `hedef yuzde dogru hesaplanir`() {
        assertEquals(50, SaglikVeriMotoru.hedefYuzde(5000, 10000))
        assertEquals(100, SaglikVeriMotoru.hedefYuzde(10000, 10000))
        assertEquals(0, SaglikVeriMotoru.hedefYuzde(0, 10000))
        assertEquals(0, SaglikVeriMotoru.hedefYuzde(5000, 0))
    }

    @Test
    fun `aktivite derece sinirlara gore seviye verir`() {
        assertEquals(0, SaglikVeriMotoru.aktiviteDerece(0, 10000))
        assertEquals(2, SaglikVeriMotoru.aktiviteDerece(5000, 10000))
        assertEquals(4, SaglikVeriMotoru.aktiviteDerece(9000, 10000))
    }

    @Test
    fun `hedef oner mevcut adima gore artar`() {
        assertTrue(SaglikVeriMotoru.hedefOner(12000) > SaglikVeriMotoru.hedefOner(3000))
        assertEquals(5000, SaglikVeriMotoru.hedefOner(1000))
    }

    @Test
    fun `toplam adim kayitlari toplar`() {
        val kayitlar = listOf(
            SaglikVeriMotoru.AdimKaydi("20260815", 3000),
            SaglikVeriMotoru.AdimKaydi("20260815", 2000)
        )
        assertEquals(5000, SaglikVeriMotoru.toplamAdim(kayitlar))
    }

    // ── TakvimPlanlamaMotoru ──

    @Test
    fun `gun anahtari yyyyMMdd biciminde`() {
        assertEquals("20260815", TakvimPlanlamaMotoru.gunAnahtari(2026, 8, 15))
    }

    @Test
    fun `gune ata gorevi tarih ve saate baglar`() {
        val p = TakvimPlanlamaMotoru.guneAta("Ders", "20260815", 600)
        assertEquals("Ders", p.gorevAd)
        assertEquals("20260815", p.gunAnahtar)
        assertEquals(600, p.saatDk)
    }

    @Test
    fun `gune ata saat sinirini zorlar`() {
        assertEquals(0, TakvimPlanlamaMotoru.guneAta("x", "20260815", -5).saatDk)
        assertEquals(1439, TakvimPlanlamaMotoru.guneAta("x", "20260815", 9999).saatDk)
    }

    @Test
    fun `haftalik dagilim gorevleri gunlere yayar`() {
        val gunler = listOf("20260810", "20260811", "20260812", "20260813", "20260814")
        val plan = TakvimPlanlamaMotoru.haftalikDagilim(listOf("A", "B", "C"), gunler)
        assertEquals(3, plan.size)
        assertEquals("20260810", plan[0].gunAnahtar)
        assertEquals("20260811", plan[1].gunAnahtar)
    }

    @Test
    fun `bos girdi bos plan doner`() {
        assertTrue(TakvimPlanlamaMotoru.haftalikDagilim(emptyList(), listOf("x")).isEmpty())
        assertTrue(TakvimPlanlamaMotoru.haftalikDagilim(listOf("A"), emptyList()).isEmpty())
    }

    @Test
    fun `plan metni okunur bicimdedir`() {
        val p = TakvimPlanlamaMotoru.guneAta("Ders", "20260815", 600)
        assertTrue(TakvimPlanlamaMotoru.planMetni(p).contains("15-08: Ders @ 10:00"))
    }
}
