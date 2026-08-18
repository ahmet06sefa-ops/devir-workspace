package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.37 — [SaglikMotoru] saf yardımcılarının birim testleri.
 * (Context gerektiren kontroller cihaz tarafında koşar; burada
 * yalnızca JVM'de kanıtlanabilir mantık test edilir.)
 */
class SaglikMotoruTest {

    @Test
    fun `yuzde sinirlar icinde dogru`() {
        assertEquals(0, SaglikMotoru.yuzde(0, 21))
        assertEquals(50, SaglikMotoru.yuzde(10, 20))
        assertEquals(100, SaglikMotoru.yuzde(21, 21))
        assertEquals(100, SaglikMotoru.yuzde(0, 0))
    }

    @Test
    fun `tahmin sinir durumlarinda sifir`() {
        assertEquals(0L, SaglikMotoru.tahminiKalanMs(1000, 0, 10))
        assertEquals(0L, SaglikMotoru.tahminiKalanMs(1000, 10, 10))
        assertEquals(0L, SaglikMotoru.tahminiKalanMs(1000, 5, 0))
    }

    @Test
    fun `tahmin orantisal hesapliyor`() {
        // 10 maddenin 5'i 1000 ms'de bitti → kalan 5 ≈ 1000 ms.
        assertEquals(1000L, SaglikMotoru.tahminiKalanMs(1000, 5, 10))
    }

    @Test
    fun `yetim tespiti dogru ve guvenli`() {
        val yetim = SaglikMotoru.yetimBul(setOf(1L, 2L, 3L, 4L), setOf(2L, 4L))
        assertEquals(setOf(1L, 3L), yetim)
        assertTrue(SaglikMotoru.yetimBul(emptySet(), setOf(1L)).isEmpty())
        assertTrue(SaglikMotoru.yetimBul(setOf(5L), setOf(5L)).isEmpty())
    }

    @Test
    fun `yazi temizligi cift bosluklari teke indirir`() {
        assertEquals("merhaba dünya", SaglikMotoru.temizYazi("  merhaba   dünya  "))
        assertEquals("a b", SaglikMotoru.temizYazi("a  b"))
        assertEquals("", SaglikMotoru.temizYazi("   "))
    }

    @Test
    fun `json gecerlilik denetimi`() {
        assertTrue(SaglikMotoru.jsonGecerliMi("[]"))
        assertTrue(SaglikMotoru.jsonGecerliMi("{\"a\":1}"))
        assertTrue(SaglikMotoru.jsonGecerliMi("[{\"id\":1}]"))
        assertFalse(SaglikMotoru.jsonGecerliMi("bozuk {"))
        assertFalse(SaglikMotoru.jsonGecerliMi(""))
        assertFalse(SaglikMotoru.jsonGecerliMi(null))
    }

    @Test
    fun `json kimlik cikarma`() {
        assertEquals(setOf(5L, 9L), SaglikMotoru.jsonKimlikler("[{\"id\":5},{\"id\":9}]"))
        assertEquals(emptySet<Long>(), SaglikMotoru.jsonKimlikler("[]"))
        assertNull(SaglikMotoru.jsonKimlikler("bozuk"))
        assertNull(SaglikMotoru.jsonKimlikler("[{\"adi\":\"idsiz\"}]"))
    }

    @Test
    fun `ozet sayilari dogru sayiyor`() {
        val l = listOf(
            SaglikMotoru.Madde("a", "a", SaglikMotoru.Durum.IYI, ""),
            SaglikMotoru.Madde("b", "b", SaglikMotoru.Durum.UYARI, ""),
            SaglikMotoru.Madde("c", "c", SaglikMotoru.Durum.HATA, ""),
            SaglikMotoru.Madde("d", "d", SaglikMotoru.Durum.ONARILDI, ""),
            SaglikMotoru.Madde("e", "e", SaglikMotoru.Durum.BILGI, "")
        )
        val s = SaglikMotoru.ozetSayilari(l)
        assertEquals(2, s[0]) // IYI + ONARILDI
        assertEquals(1, s[1])
        assertEquals(1, s[2])
        assertEquals(1, s[3])
        assertEquals(1, s[4])
    }
}
