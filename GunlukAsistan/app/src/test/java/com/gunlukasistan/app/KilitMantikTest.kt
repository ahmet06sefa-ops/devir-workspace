package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.22 · Gizlilik Kilidi — saf mantık birim testleri.
 *
 * Android yok; saatler enjekte edilir (cihaz saatinden bağımsız).
 * KilitMantik.MessageDigest kullanır — android.jar değil JDK sınıfı,
 * JVM testlerinde gerçekten çalışır.
 */
class KilitMantikTest {

    // ── PIN kuralları ─────────────────────────────────────────

    @Test
    fun `pin kurallari - 4 ila 8 rakam gecerli`() {
        assertTrue(KilitMantik.pinGecerliMi("1234"))
        assertTrue(KilitMantik.pinGecerliMi("0000"))
        assertTrue(KilitMantik.pinGecerliMi("12345678"))
    }

    @Test
    fun `pin kurallari - kisa uzun harfli bos reddedilir`() {
        assertFalse(KilitMantik.pinGecerliMi(""))
        assertFalse(KilitMantik.pinGecerliMi("123"))
        assertFalse(KilitMantik.pinGecerliMi("123456789"))
        assertFalse(KilitMantik.pinGecerliMi("12a4"))
        assertFalse(KilitMantik.pinGecerliMi("12 4"))
        assertFalse(KilitMantik.pinGecerliMi("1234 "))
    }

    // ── Hash ve tuz ───────────────────────────────────────────

    @Test
    fun `pin hash deterministik ve 64 hex`() {
        val h1 = KilitMantik.pinHash("2580", "abc123")
        val h2 = KilitMantik.pinHash("2580", "abc123")
        assertEquals(h1, h2)
        assertEquals(64, h1.length)
        assertTrue(h1.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `tuz degisince hash degisir - ayni pin farkli sonuc`() {
        val a = KilitMantik.pinHash("1234", "tuzA")
        val b = KilitMantik.pinHash("1234", "tuzB")
        assertNotEquals(a, b)
    }

    @Test
    fun `tuz uretimi deterministik 16 hex`() {
        val t1 = KilitMantik.tuzUret(111L, 222L)
        val t2 = KilitMantik.tuzUret(111L, 222L)
        assertEquals(t1, t2)
        assertEquals(16, t1.length)
        assertNotEquals(t1, KilitMantik.tuzUret(111L, 223L))
    }

    // ── Sabit zamanlı karşılaştırma ───────────────────────────

    @Test
    fun `sabit zamanli esitlik dogru calisir`() {
        assertTrue(KilitMantik.sabitZamanliEsit("abc", "abc"))
        assertTrue(KilitMantik.sabitZamanliEsit("", ""))
        assertFalse(KilitMantik.sabitZamanliEsit("abc", "abd"))
        assertFalse(KilitMantik.sabitZamanliEsit("abc", "ab"))
        assertFalse(KilitMantik.sabitZamanliEsit("", "a"))
    }

    // ── Deneme sayacı ─────────────────────────────────────────

    @Test
    fun `yanlis deneme sayaci artar`() {
        var d = KilitMantik.dogruDeneme()
        d = KilitMantik.yanlisDeneme(d, 1_000L)
        assertEquals(1, d.hatalar)
        assertEquals(0L, d.kilitBitisMs)
        d = KilitMantik.yanlisDeneme(d, 2_000L)
        assertEquals(2, d.hatalar)
        assertEquals(3, KilitMantik.kalanHak(d))
    }

    @Test
    fun `besinci yanliste 30 sn bekleme baslar ve sayac sifirlanir`() {
        var d = KilitMantik.dogruDeneme()
        val t0 = 100_000L
        repeat(4) { d = KilitMantik.yanlisDeneme(d, t0 + it) }
        assertEquals(4, d.hatalar)
        d = KilitMantik.yanlisDeneme(d, t0 + 10)
        assertEquals(0, d.hatalar)
        assertEquals(t0 + 10 + KilitMantik.BEKLEME_MS, d.kilitBitisMs)
        assertTrue(KilitMantik.beklemedeMi(d, t0 + 11))
    }

    @Test
    fun `kalan bekleme saniyesi yukari yuvarlanir`() {
        val t0 = 1_000_000L
        val d = KilitMantik.DenemeDurum(hatalar = 0, kilitBitisMs = t0 + 30_000L)
        assertEquals(30L, KilitMantik.kalanBeklemeSn(d, t0))
        assertEquals(1L, KilitMantik.kalanBeklemeSn(d, t0 + 29_500L))
        assertEquals(0L, KilitMantik.kalanBeklemeSn(d, t0 + 30_000L))
        assertFalse(KilitMantik.beklemedeMi(d, t0 + 30_000L))
    }

    @Test
    fun `beklemedeyken yanlis deneme durumu degistirmez`() {
        val t0 = 5_000L
        val d = KilitMantik.DenemeDurum(hatalar = 0, kilitBitisMs = t0 + 30_000L)
        assertEquals(d, KilitMantik.yanlisDeneme(d, t0 + 100L))
    }

    @Test
    fun `dogru deneme sifirlar`() {
        val d = KilitMantik.DenemeDurum(hatalar = 4, kilitBitisMs = 999L)
        assertEquals(KilitMantik.DenemeDurum(0, 0L), KilitMantik.dogruDeneme())
        assertFalse(KilitMantik.beklemedeMi(KilitMantik.dogruDeneme(), 0L))
        assertEquals(5, KilitMantik.kalanHak(KilitMantik.dogruDeneme()))
    }

    @Test
    fun `kalan hak hicbir zaman 1 in altina dusmez`() {
        val d = KilitMantik.DenemeDurum(hatalar = KilitMantik.HATA_LIMITI, kilitBitisMs = 1L)
        assertEquals(1, KilitMantik.kalanHak(d))
    }

    // ── Otomatik kilit kararı ─────────────────────────────────

    @Test
    fun `pin kurulu degilse kilit asla gerekmez`() {
        assertFalse(KilitMantik.kilitGerekliMi(false, false, 0L, 1_000L, 0L))
        assertFalse(KilitMantik.kilitGerekliMi(false, false, 500L, 999_999L, 300_000L))
    }

    @Test
    fun `az once acildiysa kilit gerekmez - sonsuz dongu onlemi`() {
        assertTrue(KilitMantik.kilitGerekliMi(true, false, 0L, 1_000L, 0L))
        assertFalse(KilitMantik.kilitGerekliMi(true, true, 0L, 1_000L, 0L))
    }

    @Test
    fun `soguk acilis kilitler - arka plan kaydi yok`() {
        assertTrue(KilitMantik.kilitGerekliMi(true, false, 0L, 50_000L, 300_000L))
    }

    @Test
    fun `saat kaymasinda kilitleme yapilmaz`() {
        assertFalse(KilitMantik.kilitGerekliMi(true, false, 10_000L, 5_000L, 0L))
    }

    @Test
    fun `zaman asimi esiginde karar dogru`() {
        val gitti = 1_000_000L
        // 5 dk eşiği: 299_999 geçti → gerekmez, 300_000 geçti → gerekli
        assertFalse(KilitMantik.kilitGerekliMi(true, false, gitti, gitti + 299_999L, 300_000L))
        assertTrue(KilitMantik.kilitGerekliMi(true, false, gitti, gitti + 300_000L, 300_000L))
    }

    @Test
    fun `her zaman kipi dondurme gecisini kilitmez ama ayrilisi kilitler`() {
        val gitti = 2_000_000L
        // Ekran döndürme: 500 sn değil 500 ms — eşik altında → kilit yok
        assertFalse(KilitMantik.kilitGerekliMi(true, false, gitti, gitti + 500L, 0L))
        // Gerçekten ayrıldı (1,5 sn eşik + ötesi) → kilit
        assertTrue(KilitMantik.kilitGerekliMi(true, false, gitti, gitti + 1_500L, 0L))
    }

    @Test
    fun `zaman asimi secenek listesi dort kademe`() {
        assertEquals(4, KilitMantik.ZAMAN_ASIMLARI.size)
        assertEquals(0L, KilitMantik.ZAMAN_ASIMLARI[0])
        assertEquals(60_000L, KilitMantik.ZAMAN_ASIMLARI[1])
        assertEquals(300_000L, KilitMantik.ZAMAN_ASIMLARI[2])
        assertEquals(900_000L, KilitMantik.ZAMAN_ASIMLARI[3])
    }
}
