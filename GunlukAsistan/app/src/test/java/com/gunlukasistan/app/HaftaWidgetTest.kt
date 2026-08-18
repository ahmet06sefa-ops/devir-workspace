package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.10 · ULTRA-50 C34 — Hafta widget'ı saf mantığı.
 *
 * 7 Ağustos 2026 = Cuma · 3 Ağustos = Pazartesi · 9 Ağustos = Pazar
 * (takvimde sabit; testler bu haftayı kullanır, saat diliminden bağımsız
 * kalması için kurulum ve doğrulama hep aynı Calendar üzerinden yapılır).
 */
class HaftaWidgetTest {

    private fun ms(gun: Int, saat: Int, dakika: Int): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, gun, saat, dakika, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ── haftaBaslangici ──────────────────────────────────────────

    @Test
    fun haftaBaslangici_cumadanPazartesiyeIner() {
        // Cuma 12:30 → aynı haftanın Pazartesi'si 00:00
        assertEquals(ms(3, 0, 0), HaftaWidget.haftaBaslangici(ms(7, 12, 30)))
    }

    @Test
    fun haftaBaslangici_pazarOncekiHaftayaBaglanir() {
        // TR haftası Pazartesi başlar: Pazar, haftanın SON günüdür.
        assertEquals(ms(3, 0, 0), HaftaWidget.haftaBaslangici(ms(9, 18, 5)))
    }

    @Test
    fun haftaBaslangici_pazartesiGeceyarsiKendisiDoner() {
        assertEquals(ms(3, 0, 0), HaftaWidget.haftaBaslangici(ms(3, 0, 0)))
    }

    @Test
    fun haftaBaslangici_saniyeSifirlanir() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 7, 12, 30, 45)
            set(Calendar.MILLISECOND, 999)
        }
        assertEquals(ms(3, 0, 0), HaftaWidget.haftaBaslangici(cal.timeInMillis))
    }

    // ── gunListesi ───────────────────────────────────────────────

    @Test
    fun gunListesi_yediArdisikGun() {
        val liste = HaftaWidget.gunListesi(ms(3, 0, 0))
        assertEquals(7, liste.size)
        assertEquals(ms(3, 0, 0), liste[0])
        assertEquals(ms(9, 0, 0), liste[6])
        // Her gün bir öncekinden tam bir gün sonra (00:00 hizalı)
        for (i in 1..6) {
            assertTrue(liste[i] > liste[i - 1])
        }
    }

    // ── ayniGunMu ────────────────────────────────────────────────

    @Test
    fun ayniGunMu_sabahVeAksamAyniGun() {
        assertTrue(HaftaWidget.ayniGunMu(ms(7, 1, 0), ms(7, 23, 59)))
        assertFalse(HaftaWidget.ayniGunMu(ms(7, 23, 59), ms(8, 0, 1)))
    }

    // ── planIsareti ──────────────────────────────────────────────

    @Test
    fun planIsareti_butunHaller() {
        // Plan kapalı → koşulsuz nötr boş halka
        assertEquals("○", HaftaWidget.planIsareti(90, dersVar = true, planAcik = false))
        // Hedef atanmış → dolu halka + dk
        assertEquals("● 90", HaftaWidget.planIsareti(90, dersVar = false, planAcik = true))
        // Yalnız ders atanmış → kitap işareti
        assertEquals("● 📖", HaftaWidget.planIsareti(-1, dersVar = true, planAcik = true))
        // Hedef 0 = izin günü
        assertEquals("🌿", HaftaWidget.planIsareti(0, dersVar = false, planAcik = true))
        // Açık ama gün tanımsız → boş halka
        assertEquals("○", HaftaWidget.planIsareti(-1, dersVar = false, planAcik = true))
    }

    @Test
    fun planIsareti_hedefDersdenOnceYazar() {
        // Hem dk hem ders varsa dk gösterilir (rozet alanı dar)
        assertEquals("● 60", HaftaWidget.planIsareti(60, dersVar = true, planAcik = true))
    }

    // ── gorevRozeti ──────────────────────────────────────────────

    @Test
    fun gorevRozeti_sifirGizlenirPozitifGorunur() {
        assertEquals("", HaftaWidget.gorevRozeti(0))
        assertEquals("3 ⚑", HaftaWidget.gorevRozeti(3))
        assertEquals("1 ⚑", HaftaWidget.gorevRozeti(1))
    }
}
