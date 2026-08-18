package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.16 — Görünüm Modu (1. Görünüm Klasik / 2. Görünüm Habit Genius) koruma testleri.
 *
 * Kullanıcı isteği: Habit Genius görünümünü "2. Tema" olarak ekle, mevcut
 * özellikler/temalar "1. Tema" olarak kalsın, aralarında seçim yapılabilsin.
 * Bu testler mod değerlerini, varsayılanı (klasik) ve mod kararlarını korur.
 */
class GorunumModuTest {

    @Test
    fun `klasik gorunum modu 1 degerindedir`() {
        assertEquals(1, ThemeManager.GORUNUM_KLASIK)
    }

    @Test
    fun `habitgenius gorunum modu 2 degerindedir`() {
        assertEquals(2, ThemeManager.GORUNUM_HABITGENIUS)
    }

    @Test
    fun `varsayilan gorunum modu klasiktir`() {
        // Pref yokken (ya da bozuk degerde) gorunumModu 1..2'ye cekilir.
        val deger = ThemeManager.gorunumModuSaf(-999)
        assertEquals(ThemeManager.GORUNUM_KLASIK, deger)
    }

    @Test
    fun `gorunum modu 1 ile 2 arasinda sinirlanir`() {
        assertEquals(ThemeManager.GORUNUM_KLASIK, ThemeManager.gorunumModuSaf(0))
        assertEquals(ThemeManager.GORUNUM_HABITGENIUS, ThemeManager.gorunumModuSaf(99))
        assertEquals(ThemeManager.GORUNUM_KLASIK, ThemeManager.gorunumModuSaf(1))
        assertEquals(ThemeManager.GORUNUM_HABITGENIUS, ThemeManager.gorunumModuSaf(2))
    }

    @Test
    fun `habitgenius secildiginde koyu sayilmaz`() {
        assertFalse(ThemeManager.habitGeniusKoyuMu())
    }

    @Test
    fun `klasik temalar ve habitgenius ayri varliklardir`() {
        // Habit Genius modu mevcut theme_index'i ezmez; yalnızca üst katman anahtardır.
        assertTrue(ThemeManager.GORUNUM_KLASIK != ThemeManager.GORUNUM_HABITGENIUS)
    }

    @Test
    fun `habitgenius bir acik (light) temadir`() {
        // 2. Görünüm sabit açık tema olduğu için koyu moda geçmez.
        assertFalse(ThemeManager.habitGeniusKoyuMu())
    }
}
