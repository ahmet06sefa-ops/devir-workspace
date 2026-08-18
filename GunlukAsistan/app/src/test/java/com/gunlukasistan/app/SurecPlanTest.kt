package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.28 · Katalog #62 — [SurecPlan] saf mantık testleri.
 * Gün sınırı bölme, budama penceresi, günlük toplamlar, JSON köprüsü.
 */
class SurecPlanTest {

    private fun gun(y: Int, m: Int, d: Int, saat: Int, dk: Int): Long {
        val c = Calendar.getInstance()
        c.set(y, m - 1, d, saat, dk, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun gunBaslangici_geceYarisi() {
        val t = gun(2026, 8, 9, 14, 35)
        val bas = SurecPlan.gunBaslangici(t)
        val c = Calendar.getInstance().apply { timeInMillis = bas }
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(9, c.get(Calendar.DAY_OF_MONTH))
        assertTrue(bas <= t)
    }

    @Test
    fun parcala_ayniGunTekKayit() {
        val bitis = gun(2026, 8, 9, 14, 0)
        val p = SurecPlan.gunlereParcala(bitis, 45)
        assertEquals(1, p.size)
        assertEquals(45, p[0].dakika)
        assertEquals(bitis, p[0].bitisMs)
    }

    @Test
    fun parcala_geceYarisiAsimiIkiyeBolunur() {
        // 23:50'de biten 30 dk → bugün 10 dk + dün 20 dk
        val bitis = gun(2026, 8, 9, 0, 10)
        val p = SurecPlan.gunlereParcala(bitis, 30)
        assertEquals(2, p.size)
        val bugun = p.first { it.bitisMs == bitis }
        val dun = p.first { it.bitisMs != bitis }
        assertEquals(10, bugun.dakika)
        assertEquals(20, dun.dakika)
        // Dün parçası dünün son dakikasına yazılır
        assertTrue(dun.bitisMs < SurecPlan.gunBaslangici(bitis))
    }

    @Test
    fun parcala_tamGeceYarisiTekKayit() {
        // 00:00'da biten 25 dk tamamen düne aittir (00:00 ≥ gün başı)
        val bitis = gun(2026, 8, 9, 0, 0)
        val p = SurecPlan.gunlereParcala(bitis, 25)
        assertEquals(1, p.size)
        assertEquals(25, p[0].dakika)
    }

    @Test
    fun kayitEkle_yenilerBasEskiBudanir() {
        val simdi = gun(2026, 8, 9, 12, 0)
        val eski = SurecPlan.Oturum(simdi - SurecPlan.BUDAMA_MS - 1000L, 10) // 35s'yi aşmış
        val yeni = SurecPlan.kayitEkle(listOf(eski), simdi, 25, simdi)
        assertEquals(1, yeni.size)
        assertEquals(25, yeni[0].dakika)
        assertEquals(simdi, yeni[0].bitisMs)
    }

    @Test
    fun kayitEkle_pencereIcindekiKorunur() {
        val simdi = gun(2026, 8, 9, 12, 0)
        val dunOturum = SurecPlan.Oturum(simdi - 24L * 60 * 60 * 1000, 40)
        val yeni = SurecPlan.kayitEkle(listOf(dunOturum), simdi, 25, simdi)
        assertEquals(2, yeni.size)
        assertEquals(25, yeni[0].dakika) // yeni başta
    }

    @Test
    fun kayitEkle_sifirDakikaEklenmez() {
        val simdi = gun(2026, 8, 9, 12, 0)
        val yeni = SurecPlan.kayitEkle(emptyList(), simdi, 0, simdi)
        assertTrue(yeni.isEmpty())
    }

    @Test
    fun kayitEkle_sinirBudanir() {
        val simdi = gun(2026, 8, 9, 12, 0)
        var liste: List<SurecPlan.Oturum> = emptyList()
        repeat(SurecPlan.KAYIT_SINIR + 10) { i ->
            liste = SurecPlan.kayitEkle(liste, simdi - i * 60_000L, 5, simdi)
        }
        assertEquals(SurecPlan.KAYIT_SINIR, liste.size)
    }

    @Test
    fun toplamlar_bugunVeDunAyrimi() {
        val simdi = gun(2026, 8, 9, 12, 0)
        val liste = listOf(
            SurecPlan.Oturum(simdi - 3_600_000L, 25),          // bugün
            SurecPlan.Oturum(simdi - 2 * 3_600_000L, 15),      // bugün
            SurecPlan.Oturum(simdi - 20L * 3_600_000L, 40)     // dün
        )
        assertEquals(40, SurecPlan.bugunToplam(liste, simdi))
        assertEquals(40, SurecPlan.dunToplam(liste, simdi))
    }

    @Test
    fun json_turUydurma() {
        val liste = listOf(
            SurecPlan.Oturum(1691500000000L, 25),
            SurecPlan.Oturum(1691406400000L, 20)
        )
        val geri = SurecPlan.jsondanOku(SurecPlan.jsonaYaz(liste))
        assertEquals(liste, geri)
    }

    @Test
    fun json_bozukGuvenli() {
        assertTrue(SurecPlan.jsondanOku(null).isEmpty())
        assertTrue(SurecPlan.jsondanOku("").isEmpty())
        assertTrue(SurecPlan.jsondanOku("bozuk {{").isEmpty())
        assertTrue(SurecPlan.jsondanOku("[{\"bitis\":abc}]").isEmpty())
    }
}
