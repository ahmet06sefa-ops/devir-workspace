package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — AI'nın uygulamayı "her şeyiyle" kontrol etmesi için eklenen saf
 * karar fonksiyonlarının testleri.
 *
 *  · [AsistanKomut.evetMi] — "aç/kapat" yorumlama
 *  · [AsistanKomut.geceSecimi] — koyu/açık/sistem tema seçimi
 */
class AsistanKontrolTest {

    @Test
    fun `evetMi olumlu ifadeleri acik sayar`() {
        assertTrue(AsistanKomut.evetMi("acik"))
        assertTrue(AsistanKomut.evetMi("1"))
        assertTrue(AsistanKomut.evetMi("evet"))
        assertTrue(AsistanKomut.evetMi("var"))
        assertTrue(AsistanKomut.evetMi("aç"))
    }

    @Test
    fun `evetMi olumsuz ifadeleri kapali sayar`() {
        assertFalse(AsistanKomut.evetMi("kapanik"))
        assertFalse(AsistanKomut.evetMi("kapalı"))
        assertFalse(AsistanKomut.evetMi("0"))
        assertFalse(AsistanKomut.evetMi("hayır"))
        assertFalse(AsistanKomut.evetMi("yok"))
    }

    @Test
    fun `geceSecimi koyu ifadeleri koyu tema yapar`() {
        assertEquals(ThemeManager.GECE_ACIK, AsistanKomut.geceSecimi("koyu"))
        assertEquals(ThemeManager.GECE_ACIK, AsistanKomut.geceSecimi("gece"))
        assertEquals(ThemeManager.GECE_ACIK, AsistanKomut.geceSecimi("ac"))
    }

    @Test
    fun `geceSecimi kapat ifadeleri acik tema yapar`() {
        assertEquals(ThemeManager.GECE_KAPALI, AsistanKomut.geceSecimi("kapat"))
        assertEquals(ThemeManager.GECE_KAPALI, AsistanKomut.geceSecimi("açık"))
    }

    @Test
    fun `geceSecimi sistem oto ifadeleri sistem temasina gonderir`() {
        assertEquals(ThemeManager.GECE_SISTEM, AsistanKomut.geceSecimi("sistem"))
        assertEquals(ThemeManager.GECE_SISTEM, AsistanKomut.geceSecimi("otomatik"))
    }

    @Test
    fun `komut ayikla yeni komutlari dogru ayristirir`() {
        val (temiz, liste) = AsistanKomut.ayiklaHepsi(
            "Tamam, yaptım.\n>>KOMUT:ayar_ses|acik\n>>KOMUT:ozet_ver|"
        )
        assertEquals(2, liste.size)
        assertEquals("ayar_ses", liste[0].ad)
        assertEquals("acik", liste[0].deger)
        assertEquals("ozet_ver", liste[1].ad)
        assertTrue(temiz.contains("yaptım"))
    }
}
