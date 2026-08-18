package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.33 · Katalog #15/#38 — [GorevDisAktar] + [NotOneri] testleri. */
class GorevDisAktarTest {

    @Test
    fun metin_formatTam() {
        val satirlar = listOf(
            GorevDisAktar.Satir("Süt al", false, "9 Ağu Pazar · 09:00"),
            GorevDisAktar.Satir("Faturayı öde", true, null)
        )
        assertEquals(
            "📋 Görevlerim:\n☐ Süt al · ⏰ 9 Ağu Pazar · 09:00\n☑ Faturayı öde",
            GorevDisAktar.metin(satirlar, "📋 Görevlerim:")
        )
    }

    @Test
    fun metin_bosListeSadeceBaslik() {
        assertEquals("başlık", GorevDisAktar.metin(emptyList(), " başlık "))
    }

    @Test
    fun metin_uclarKirpilir() {
        val m = GorevDisAktar.metin(listOf(GorevDisAktar.Satir("  boşluklu  ", false, "  ")), "b")
        assertEquals("b\n☐ boşluklu", m)
    }

    @Test
    fun oneri_ilkSatirdan() {
        assertEquals("kitap oku", NotOneri.baslik("- kitap oku\nsonra spor"))
    }

    @Test
    fun oneri_bosVeUzun() {
        assertEquals("", NotOneri.baslik(""))
        assertEquals("", NotOneri.baslik("   \n "))
        val uzun = "a".repeat(80)
        assertEquals(60, NotOneri.baslik(uzun).length)
    }
}
