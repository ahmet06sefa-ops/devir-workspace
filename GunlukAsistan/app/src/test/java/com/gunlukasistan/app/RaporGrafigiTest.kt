package com.gunlukasistan.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * v10.3 · Öneri B16 — haftalık grafik veri katmanı (saf).
 *
 * `simdi` dışarıdan verildiği için testler cihaz saatinden bağımsız.
 * Dizinin 6. indeksi daima "bugün".
 */
class RaporGrafigiTest {

    /** Bugün 20:00 sabitlendi — test koştuğu saate göre kaymaz. */
    private val simdi: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private fun gununSaati(gunGeriye: Int, saat: Int, taban: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = taban
        cal.add(Calendar.DAY_OF_YEAR, -gunGeriye)
        cal.set(Calendar.HOUR_OF_DAY, saat)
        cal.set(Calendar.MINUTE, 15)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun kayit(ms: Long, dk: Int, tamam: Boolean = true) =
        SureAnalizi.PomodoroKayit(zaman = ms, sureDk = dk, tamamlandi = tamam, saat = 10)

    @Test
    fun `kayitlar dogru gunune duser`() {
        val simdi = this.simdi
        val liste = listOf(
            kayit(gununSaati(0, 9, simdi), 25),   // bugün
            kayit(gununSaati(0, 17, simdi), 15),  // bugün — toplanmalı
            kayit(gununSaati(2, 12, simdi), 50),  // 2 gün önce
            kayit(gununSaati(6, 8, simdi), 10)    // haftanın ilk günü
        )
        val d = RaporGrafigi.gunlukOdakDakikalari(liste, simdi)
        assertEquals(10, d[0])
        assertEquals(50, d[4])
        assertEquals(40, d[6])
        assertEquals(7, d.size)
    }

    @Test
    fun `tamamlanmayan oturum sayilmaz`() {
        val simdi = this.simdi
        val liste = listOf(
            kayit(gununSaati(1, 10, simdi), 25, tamam = false),
            kayit(gununSaati(1, 14, simdi), 30)
        )
        val d = RaporGrafigi.gunlukOdakDakikalari(liste, simdi)
        assertEquals(30, d[5])
    }

    @Test
    fun `pencere disi ve gelecek damgalar elenir`() {
        val simdi = this.simdi
        val liste = listOf(
            kayit(gununSaati(7, 10, simdi), 25),      // 7 gün önce — pencere dışı
            kayit(simdi + 3_600_000L, 25)             // gelecek — elenir
        )
        val d = RaporGrafigi.gunlukOdakDakikalari(liste, simdi)
        assertArrayEquals(IntArray(7), d)
    }

    @Test
    fun `olcek ustu bos haftada bire duser`() {
        assertEquals(1, RaporGrafigi.olcekUstu(IntArray(7)))
        assertEquals(80, RaporGrafigi.olcekUstu(intArrayOf(0, 80, 0, 0, 0, 0, 3)))
    }
}
