package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.8 · D40 — Tema paketlerinin saf mantığı.
 *
 * Dizin sınırları, JSON turu ve şablon bütünlüğü Context'siz
 * kanıtlanır; uygulama katmanı yalnızca bu kapıları çağırır.
 */
class TemaPaketiTest {

    private fun paket(
        id: Long = 1,
        tema: Int = 2,
        vurgu: Int = 5,
        gece: Int = 1,
        yogunluk: Int = 0,
        yazi: Int = 3
    ) = TemaPaketi.Paket(id, "Deneme", "🎨", tema, vurgu, gece, yogunluk, yazi, true)

    // ── doğrulama ────────────────────────────────────────────────

    @Test
    fun dogrulanmis_aralikDisiBudanir() {
        val p = TemaPaketi.dogrulanmis(
            TemaPaketi.Paket(1, "  ", "", 99, 99, 7, -3, 9, false)
        )
        assertEquals("Paket", p.ad)
        assertEquals("🎨", p.emoji)
        assertTrue(p.tema >= 0 && p.tema < ThemeManager.specs.size)
        assertTrue(p.vurgu >= -1 && p.vurgu < ThemeManager.accents.size)
        assertEquals(2, p.gece)
        assertEquals(0, p.yogunluk)
        assertEquals(3, p.yazi)
    }

    @Test
    fun dogrulanmis_gecerliyiBozmaz() {
        val p = TemaPaketi.dogrulanmis(paket())
        assertEquals(2, p.tema)
        assertEquals(5, p.vurgu)
        assertEquals(1, p.gece)
        assertEquals("Deneme", p.ad)
    }

    @Test
    fun dogrulanmis_vurguVarsayilanaİzinVerir() {
        assertEquals(-1, TemaPaketi.dogrulanmis(paket(vurgu = -5)).vurgu)
        assertEquals(-1, TemaPaketi.dogrulanmis(paket(vurgu = -1)).vurgu)
    }

    // ── kota ─────────────────────────────────────────────────────

    @Test
    fun eklenebilirMi_kotayiSayar() {
        assertTrue(TemaPaketi.eklenebilirMi(0))
        assertTrue(TemaPaketi.eklenebilirMi(TemaPaketi.MAKS - 1))
        assertFalse(TemaPaketi.eklenebilirMi(TemaPaketi.MAKS))
        assertFalse(TemaPaketi.eklenebilirMi(TemaPaketi.MAKS + 3))
    }

    // ── JSON ─────────────────────────────────────────────────────

    @Test
    fun json_tamTur() {
        val p = paket(id = 7)
        val geri = TemaPaketi.cozle(TemaPaketi.kodla(listOf(p))).single()
        assertEquals(p, geri)
    }

    @Test
    fun json_bozukMetin_bosVerir() {
        assertTrue(TemaPaketi.cozle("json değil").isEmpty())
        assertTrue(TemaPaketi.cozle("").isEmpty())
        assertTrue(TemaPaketi.cozle("{}").isEmpty())
    }

    @Test
    fun json_bozukSatirAtlanir() {
        val karisik = """[${paket(5).json()}, {"ad":"", "tema":1}]"""
        val liste = TemaPaketi.cozle(karisik)
        assertEquals(1, liste.size)
        assertEquals(5L, liste[0].id)
    }

    // ── şablonlar ────────────────────────────────────────────────

    @Test
    fun sablonlar_kimlikleriBenzersizVeNegatif() {
        val ids = TemaPaketi.sablonlar().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it < 0 })
    }

    @Test
    fun sablonlar_dizinlerGecerliAralikta() {
        for (s in TemaPaketi.sablonlar()) {
            assertTrue("${s.ad}: tema taştı", s.tema in ThemeManager.specs.indices)
            assertTrue("${s.ad}: vurgu taştı", s.vurgu in -1 until ThemeManager.accents.size)
            assertTrue("${s.ad}: gece taştı", s.gece in 0..2)
            assertTrue("${s.ad}: yoğunluk taştı", s.yogunluk in 0..2)
            assertTrue("${s.ad}: yazı taştı", s.yazi in 0..3)
            assertTrue(s.ad.isNotBlank())
        }
    }

    @Test
    fun sablonlar_birbirindenFarkliKombinler() {
        // İki şablon aynı altı ayarı paylaşmamalı — yoksa ikisi
        // aynı şeyi yapar, biri dolgu olurdu.
        val kombinler = TemaPaketi.sablonlar().map {
            listOf(it.tema, it.vurgu, it.gece, it.yogunluk, it.yazi, it.dinamik)
        }
        assertEquals(kombinler.size, kombinler.toSet().size)
    }

    @Test
    fun farkliDurumlar_farkliPaketler() {
        // Gün ışığı (hep açık) ve Gece kuşu (hep koyu) zıt uçları
        // yakalamalı — katalog bunun için var.
        val gunIsigi = TemaPaketi.sablonlar().first { it.ad == "Gün ışığı" }
        val geceKusu = TemaPaketi.sablonlar().first { it.ad == "Gece kuşu" }
        assertNotEquals(gunIsigi.gece, geceKusu.gece)
        assertNotEquals(
            ThemeManager.specs[gunIsigi.tema].dark,
            ThemeManager.specs[geceKusu.tema].dark
        )
    }
}
