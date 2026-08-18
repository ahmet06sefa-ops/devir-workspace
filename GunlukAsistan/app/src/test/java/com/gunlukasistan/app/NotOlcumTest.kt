package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.29 · Katalog #27/#28 — [NotOlcum] saf mantık testleri. */
class NotOlcumTest {

    @Test
    fun kelime_bosVeBosluk() {
        assertEquals(0, NotOlcum.kelimeS(""))
        assertEquals(0, NotOlcum.kelimeS("   \n\t  "))
    }

    @Test
    fun kelime_cokluAyracTekSayilir() {
        assertEquals(4, NotOlcum.kelimeS("elma  armut\nkiraz\tmuz "))
        assertEquals(1, NotOlcum.kelimeS("tek"))
    }

    @Test
    fun karakter_hamUzunluk() {
        assertEquals(5, NotOlcum.karakterS("beş 5"))
        assertEquals(0, NotOlcum.karakterS(""))
    }

    @Test
    fun satirlar_isaretlerSoyulur() {
        val not = "- süt al\n* kitap oku\n• faturayı öde\n[ ] araba yıkama\n[x] tamamlandı gözükme\n✓ çöp"
        val s = NotOlcum.satirlariAyikla(not)
        assertEquals(
            listOf("süt al", "kitap oku", "faturayı öde", "araba yıkama", "tamamlandı gözükme", "çöp"),
            s
        )
    }

    @Test
    fun satirlar_bosSatirlarAtilir() {
        val s = NotOlcum.satirlariAyikla("birinci\n\n   \n ikinci  ")
        assertEquals(listOf("birinci", "ikinci"), s)
    }

    @Test
    fun satirlar_bosMetinBosListe() {
        assertTrue(NotOlcum.satirlariAyikla("").isEmpty())
        assertTrue(NotOlcum.satirlariAyikla("  \n \n").isEmpty())
    }
}
