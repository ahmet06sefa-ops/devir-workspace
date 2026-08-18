package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.8 · D43 — Pofi'nin ruh hali karar tablosu.
 *
 * Saat parametre olarak verilir (saat bağımlı test tuzağı yok);
 * öncelik sırasının her kombinasyonu bu tabloda sabittir.
 */
class MaskotTest {

    private fun girdi(
        saat: Int = 14,
        seri: Int = 0,
        calisiyor: Boolean = false,
        molada: Boolean = false,
        odakDk: Int = 0
    ) = Maskot.Girdi(saat, seri, calisiyor, molada, odakDk)

    // ── öncelik sırası ───────────────────────────────────────────

    @Test
    fun mola_herSeyiEzer() {
        // Molada + seri 30 + gece → yine de MOLADA
        assertEquals(
            Maskot.Ruh.MOLADA,
            Maskot.ruhHali(girdi(saat = 2, seri = 30, calisiyor = true, molada = true))
        )
    }

    @Test
    fun calisma_uykuSaatindeBileOdakli() {
        // 02:00'de sayaç koşuyor → çalışan yanında duran maskot
        assertEquals(
            Maskot.Ruh.ODAKLI,
            Maskot.ruhHali(girdi(saat = 2, calisiyor = true))
        )
    }

    @Test
    fun uyku_alevinOnerine() {
        // Gece 02:00 + seri 20: coşku gösterme, uyut
        assertEquals(Maskot.Ruh.UYKULU, Maskot.ruhHali(girdi(saat = 23, seri = 20)))
        assertEquals(Maskot.Ruh.UYKULU, Maskot.ruhHali(girdi(saat = 4, seri = 20)))
    }

    @Test
    fun uykuSinirlari_dogru() {
        assertEquals(Maskot.Ruh.UYKULU, Maskot.ruhHali(girdi(saat = 23)))
        assertEquals(Maskot.Ruh.NESHALI, Maskot.ruhHali(girdi(saat = Maskot.UYKU_BITIS, odakDk = 0)))
        assertEquals(Maskot.Ruh.NESHALI, Maskot.ruhHali(girdi(saat = 22, seri = 0)))
    }

    @Test
    fun alev_esikveUzerinde() {
        assertEquals(Maskot.Ruh.NESHALI, Maskot.ruhHali(girdi(seri = Maskot.ALEV_ESIK - 1)))
        assertEquals(Maskot.Ruh.ALEV, Maskot.ruhHali(girdi(seri = Maskot.ALEV_ESIK)))
        assertEquals(Maskot.Ruh.ALEV, Maskot.ruhHali(girdi(seri = 365)))
    }

    @Test
    fun gurur_esikveUzerinde() {
        assertEquals(
            Maskot.Ruh.GURURLU,
            Maskot.ruhHali(girdi(odakDk = Maskot.GURUR_ESIK_DK))
        )
        assertEquals(
            Maskot.Ruh.NESHALI,
            Maskot.ruhHali(girdi(odakDk = Maskot.GURUR_ESIK_DK - 1, seri = 0))
        )
    }

    @Test
    fun alev_gururuEzerCunkuDahaBuyukBasari() {
        assertEquals(Maskot.Ruh.ALEV, Maskot.ruhHali(girdi(seri = 8, odakDk = 200)))
    }

    @Test
    fun neshali_beklemeHali() {
        assertEquals(Maskot.Ruh.NESHALI, Maskot.ruhHali(girdi()))
    }

    @Test
    fun saatTasmasi_kisitlanir() {
        // Bozuk girdi çökertmemeli; saat sınıra budanır. -3→0 (gece
        // yarısı) ve 25→23 ikisi de uyku penceresine düşer — bu
        // kasıtlı davranıştır, bu test onu sabitler.
        assertEquals(Maskot.Ruh.UYKULU, Maskot.ruhHali(girdi(saat = 25)))
        assertEquals(Maskot.Ruh.UYKULU, Maskot.ruhHali(girdi(saat = -3)))
    }

    // ── mesaj sırası ─────────────────────────────────────────────

    @Test
    fun mesajSira_herZamanAralikta() {
        for (gun in 0..365) {
            val i = Maskot.mesajSira(gun, 0, 4)
            assert(i in 0..3)
        }
    }

    @Test
    fun mesajSira_dokunmaIleriDonderir() {
        // Aynı gün, ardışık dokunmalar → ardışık mesajlar
        val gun = 200
        val s0 = Maskot.mesajSira(gun, 0, 4)
        val s1 = Maskot.mesajSira(gun, 1, 4)
        assertEquals((s0 + 1) % 4, s1)
    }

    @Test
    fun mesajSira_negatifKaymaGuvenli() {
        assertEquals(0, Maskot.mesajSira(-5, -99, 0))
        // floorMod: negatif toplam da aralıkta kalır
        val i = Maskot.mesajSira(-50, -7, 4)
        assert(i in 0..3)
    }
}
