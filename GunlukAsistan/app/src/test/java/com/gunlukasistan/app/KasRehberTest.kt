package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.40 — KasRehber saf birim testleri.
 */
class KasRehberTest {

    @Test
    fun `ana kas gruplari rehberde var`() {
        val kodlar = listOf("chest", "abdominals", "biceps", "triceps", "quadriceps",
            "hamstrings", "shoulders", "lats", "glutes", "calves", "neck", "traps")
        kodlar.forEach { assertNotNull("$it rehberde yok", KasRehber.getir(it)) }
    }

    @Test
    fun `her kas islev ve gelistirme metni icerir`() {
        KasRehber.hepsi().forEach { k ->
            assertTrue("${k.kod} islev bos", k.islev.isNotBlank())
            assertTrue("${k.kod} gelistirme bos", k.gelistirme.isNotBlank())
            assertTrue("${k.kod} set onerisi bos", k.setOneri.isNotBlank())
            assertTrue("${k.kod} emoji bos", k.emoji.isNotBlank())
        }
    }

    @Test
    fun `her kas biricik ad tasir`() {
        val adlar = KasRehber.hepsi().map { it.ad }
        assertEquals(adlar.size, adlar.distinct().size)
    }

    @Test
    fun `etiket ad doner`() {
        assertEquals("Göğüs", KasRehber.etiket("chest"))
        assertEquals("Karın", KasRehber.etiket("abdominals"))
        assertEquals("x", KasRehber.etiket("x"))
    }

    @Test
    fun `gosterilen kas sayisi en az 15`() {
        assertTrue(KasRehber.hepsi().size >= 15)
    }
}
