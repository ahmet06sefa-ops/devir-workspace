package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Çok dillilik ve zengin veri dışa aktarma motorları saf testleri.
 */
class DilVeDisAktarTest {

    // ── DilSeciciMotoru ──

    @Test
    fun `varsayilan dil turkcedir`() {
        assertEquals("tr", DilSeciciMotoru.varsayilan.kod)
    }

    @Test
    fun `desteklenen diller bos degildir ve turkce icerir`() {
        assertTrue(DilSeciciMotoru.desteklenen.any { it.kod == "tr" })
        assertTrue(DilSeciciMotoru.desteklenen.any { it.kod == "en" })
    }

    @Test
    fun `dil kodu bilinmiyorsa varsayilana doner`() {
        assertEquals("tr", DilSeciciMotoru.dil("zz").kod)
        assertEquals("en", DilSeciciMotoru.dil("en").kod)
    }

    @Test
    fun `arapca RTL digerleri LTR dir`() {
        assertTrue(DilSeciciMotoru.rtlMi("ar"))
        assertFalse(DilSeciciMotoru.rtlMi("tr"))
    }

    @Test
    fun `ceviri anahtar yoksa kendisini doner`() {
        assertEquals("merhaba", DilSeciciMotoru.ceviri(emptyMap(), "merhaba"))
        assertEquals("Günaydın", DilSeciciMotoru.ceviri(mapOf("selam" to "Günaydın"), "selam"))
    }

    @Test
    fun `secili kod yerel dile gore belirlenir`() {
        assertEquals("tr", DilSeciciMotoru.seciliKod("tr"))
        assertEquals("en", DilSeciciMotoru.seciliKod("en-US"))
    }

    // ── VeriDisAktarMotoru ──

    @Test
    fun `rapor basligi gg-aa-yyyy biciminde`() {
        assertEquals("15-08-2026 Günlük Asistan Raporu", VeriDisAktarMotoru.raporBasligi("20260815"))
    }

    @Test
    fun `gorev ozeti dogru sayilari icerir`() {
        assertEquals("Bekleyen 3 · Tamamlanan 7 · Toplam 10", VeriDisAktarMotoru.gorevOzeti(3, 7))
    }

    @Test
    fun `odak ozeti yuzde icerir`() {
        assertEquals("Odak 60/90 dk (%66)", VeriDisAktarMotoru.odakOzeti(60, 90))
        assertEquals("Odak 60 dk (hedef belirsiz)", VeriDisAktarMotoru.odakOzeti(60, 0))
    }

    @Test
    fun `markdown rapor tum bolumleri icerir`() {
        val r = VeriDisAktarMotoru.markdownRapor(
            "15-08-2026 Rapor",
            VeriDisAktarMotoru.gorevOzeti(1, 2),
            VeriDisAktarMotoru.odakOzeti(60, 90),
            kursSatiri = "Revit %40",
            notlar = listOf("Plan yap")
        )
        assertTrue(r.contains("# 15-08-2026 Rapor"))
        assertTrue(r.contains("## ✅ Görevler"))
        assertTrue(r.contains("## ⏱️ Odak"))
        assertTrue(r.contains("## 🎓 Kurslar"))
        assertTrue(r.contains("## 📝 Notlar"))
        assertTrue(r.contains("Revit %40"))
    }
}
