package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v7.99 — [YedekSifre] birim testleri (öneri 10).
 *
 * ── Kapsam sınırı ──
 * `sifrele`/`coz` fonksiyonları `android.util.Base64` kullanıyor; bu sınıf
 * JVM testinde yok (`RuntimeException: not mocked`). Şifreleme turunu test
 * etmek için Robolectric ya da Base64 soyutlaması gerekirdi — ikisi de bu
 * aşamada aşırı maliyet.
 *
 * Bunun yerine **Android'e bağımlı olmayan** kısımlar test ediliyor:
 * biçim algılama ve parola gücü. Şifrelemenin kendisi standart JCA
 * (AES-GCM + PBKDF2); doğruluğu platform garantisi.
 */
class YedekSifreTest {

    // ═══════════════════════════════════════════════════════════════
    // BİÇİM ALGILAMA
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `sifreli yedek basligindan taninir`() {
        assertTrue(YedekSifre.sifreliMi("GAENC1|tuz|iv|veri"))
    }

    @Test
    fun `bastaki bosluk algilamayi bozmaz`() {
        // Dosya okunurken başa boşluk/yeni satır gelebiliyor
        assertTrue(YedekSifre.sifreliMi("\n  GAENC1|a|b|c"))
    }

    @Test
    fun `duz json sifreli sayilmaz`() {
        assertFalse(YedekSifre.sifreliMi("""{"app":"GunlukAsistan","version":18}"""))
    }

    @Test
    fun `bos metin sifreli sayilmaz`() {
        assertFalse(YedekSifre.sifreliMi(""))
        assertFalse(YedekSifre.sifreliMi("   "))
    }

    @Test
    fun `benzer ama yanlis baslik reddedilir`() {
        assertFalse(YedekSifre.sifreliMi("GAENC2|a|b|c"))
        assertFalse(YedekSifre.sifreliMi("XGAENC1|a|b|c"))
    }

    // ═══════════════════════════════════════════════════════════════
    // PAROLA GÜCÜ
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `kisa parola zayif sayilir`() {
        assertEquals(0, YedekSifre.parolaGucu("abc"))
        assertEquals(0, YedekSifre.parolaGucu("12345"))
    }

    @Test
    fun `bos parola zayif sayilir`() {
        assertEquals(0, YedekSifre.parolaGucu(""))
    }

    @Test
    fun `karisik uzun parola guclu sayilir`() {
        // 12+ karakter, harf + rakam + simge
        assertEquals(2, YedekSifre.parolaGucu("Kalem23!Defter"))
    }

    @Test
    fun `orta uzunlukta parola orta sayilir`() {
        val guc = YedekSifre.parolaGucu("abc12345")
        assertTrue("Beklenen 1 veya 2, gelen $guc", guc in 1..2)
    }

    @Test
    fun `parola gucu her zaman gecerli aralikta`() {
        // Sınıflandırma dışına taşmamalı — arayüz 0..2 bekliyor
        listOf("", "a", "abcdef", "abc123", "Uzun!Parola#2026", "!!!!!!!!!!")
            .forEach { p ->
                val g = YedekSifre.parolaGucu(p)
                assertTrue("'$p' için güç $g — 0..2 dışında", g in 0..2)
            }
    }

    @Test
    fun `uzunluk arttikca guc dusmez`() {
        // Monotonluk: daha uzun ve daha zengin parola daha zayıf olamaz
        val kisa = YedekSifre.parolaGucu("abc123")
        val uzun = YedekSifre.parolaGucu("abc123defgh")
        assertTrue("Uzun parola ($uzun) kısadan ($kisa) zayıf çıktı", uzun >= kisa)
    }
}
