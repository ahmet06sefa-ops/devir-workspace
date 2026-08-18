package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.38 · Katalog #25 — [NotHatirlatici] saf mantık testleri. */
class NotHatirlaticiTest {

    @Test
    fun `harita yaz oku tam tur`() {
        val h = mapOf(11L to 1_700_000_000_000L, 22L to 1_800_000_000_000L)
        val geri = NotHatirlatici.haritadanOku(NotHatirlatici.haritayaYaz(h))
        assertEquals(h, geri)
    }

    @Test
    fun `bozuk veri guvenli bos doner`() {
        assertEquals(emptyMap<Long, Long>(), NotHatirlatici.haritadanOku("bozuk {"))
        assertEquals(emptyMap<Long, Long>(), NotHatirlatici.haritadanOku(null))
        assertEquals(emptyMap<Long, Long>(), NotHatirlatici.haritadanOku(""))
    }

    @Test
    fun `notif kimligi aralik icinde ve kararli`() {
        val a = NotHatirlatici.notifId(5L)
        val b = NotHatirlatici.notifId(5L)
        val c = NotHatirlatici.notifId(6L)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertTrue(a >= 910000 && a < 1_010_000)
    }
}
