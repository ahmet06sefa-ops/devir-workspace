package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Tık çevrimi emniyet sarmalayıcısı (`CevrimliTik`) saf JVM testleri.
 *
 * Bu testler "saat takılıyor" düzeltmesini doğrular: gövde içinde istisna
 * olsa bile çevrim asla durmaz ve bir sonraki tık zamanlanmaya devam eder.
 */
class CevrimliTikTest {

    @Test
    fun `govde istisna firlatinca cevrim durmaz ve devam eder`() {
        val raporlar = mutableListOf<Throwable>()
        var govdeCagri = 0
        val tik = CevrimliTik(
            govde = {
                govdeCagri++
                throw IllegalStateException("ekran güncellemesi patladı")
            },
            hataRaporla = { raporlar.add(it) }
        )
        // Hata olsa da tik() false döndürür → çağıran bir sonraki tıkı zamanlar
        assertFalse(tik.tik())
        assertFalse(tik.tik())
        assertEquals(2, govdeCagri)
        assertEquals(2, raporlar.size)
    }

    @Test
    fun `govde false donunce cevrim devam eder`() {
        val tik = CevrimliTik(govde = { false })
        assertFalse(tik.tik())
        assertFalse(tik.tik())
    }

    @Test
    fun `govde true donunce cevrim biter`() {
        var cagri = 0
        val tik = CevrimliTik(govde = { cagri++; true })
        assertTrue(tik.tik())
        assertTrue(tik.tik())
        assertEquals(2, cagri)
    }

    @Test
    fun `karisik donuslerde dogru bayrak doner`() {
        val sonuclar = listOf(false, true, false)
        var i = 0
        val tik = CevrimliTik(govde = { sonuclar[i++ % sonuclar.size] })
        assertFalse(tik.tik()) // devam
        assertTrue(tik.tik())  // dur
        assertFalse(tik.tik()) // devam
    }

    @Test
    fun `hata raporlamayan govde istisna sonrasi da hata raporcuya dusmez`() {
        var cagri = 0
        val tik = CevrimliTik(govde = {
            cagri++
            if (cagri == 1) throw RuntimeException("ilk sefer hata") else false
        })
        assertFalse(tik.tik()) // hata → devam
        assertFalse(tik.tik()) // ikinci sefer temiz → devam
        assertEquals(2, cagri)
    }

    @Test
    fun `null donmeyen govde turu kullanilabilir`() {
        // Sayaç bitiş bayrağı için true/false dönen bir gövde modeli
        var kalan = 3
        val tik = CevrimliTik(govde = {
            kalan--
            kalan <= 0 // kalan tükendiğinde "dur"
        })
        assertFalse(tik.tik()) // kalan 2
        assertFalse(tik.tik()) // kalan 1
        assertTrue(tik.tik())  // kalan 0 → dur
    }
}
