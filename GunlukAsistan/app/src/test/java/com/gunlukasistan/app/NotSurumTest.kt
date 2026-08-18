package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.32 · Katalog #29 — [NotSurum] saf mantık testleri. */
class NotSurumTest {

    private fun k(z: Long, b: String = "b$z", i: String = "i$z") =
        NotSurum.Kayit(z, b, i, "")

    @Test
    fun it_basaEklerVeSiniriBudar() {
        var l: List<NotSurum.Kayit> = emptyList()
        for (x in 1..7L) l = NotSurum.it(l, k(x))
        assertEquals(NotSurum.SINIR, l.size)
        assertEquals(7L, l[0].zaman)      // en yeni başta
        assertEquals(3L, l.last().zaman)  // en eski budanmış (1,2 atıldı)
    }

    @Test
    fun zatenSonMu_kopyayiEngeller() {
        val l = listOf(k(1, "a", "i"))
        assertTrue(NotSurum.zatenSonMu(l, k(9, "a", "i")))
        assertFalse(NotSurum.zatenSonMu(l, k(9, "a", "farklı")))
        assertFalse(NotSurum.zatenSonMu(emptyList(), k(1)))
    }

    @Test
    fun metin_turUydurmaUnicode() {
        val liste = listOf(
            NotSurum.Kayit(1723000000000L, "Başlık 🌟 | özel", "satır1\nsatır2 \"tırnak\" %100", "img_1.jpg"),
            NotSurum.Kayit(1722990000000L, "", "sadece içerik", "")
        )
        val geri = NotSurum.metindenOku(NotSurum.metneCevir(liste))
        assertEquals(liste, geri)
    }

    @Test
    fun metin_bozukGuvenli() {
        assertTrue(NotSurum.metindenOku(null).isEmpty())
        assertTrue(NotSurum.metindenOku("").isEmpty())
        assertTrue(NotSurum.metindenOku("sadece|iki").isEmpty())
        assertTrue(NotSurum.metindenOku("abc|00|00|00").isEmpty())
    }
}
