package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** v10.31 · Katalog #24/#36 — [NotBicim] çözümleme ve okuma süresi testleri. */
class NotBicimTest {

    private fun duzBirlesik(p: List<NotBicim.Parca>) = p.joinToString("") { it.metin }

    @Test
    fun kalin_basit() {
        val p = NotBicim.cozumle("bu **önemli** bir not")
        assertEquals(3, p.size)
        assertEquals(NotBicim.Tip.DUZ, p[0].tip)
        assertEquals(NotBicim.Tip.KALIN, p[1].tip)
        assertEquals("önemli", p[1].metin)
        assertEquals("bu önemli bir not", duzBirlesik(p))
    }

    @Test
    fun kapanmamisIsaretDegismez() {
        val p = NotBicim.cozumle("yarım ** kalın değil")
        assertEquals(1, p.size)
        assertEquals(NotBicim.Tip.DUZ, p[0].tip)
        assertEquals("yarım ** kalın değil", duzBirlesik(p))
    }

    @Test
    fun baslik_satirBasiDiez() {
        val p = NotBicim.cozumle("# Alışveriş Listesi\nsüt al")
        assertEquals(NotBicim.Tip.BASLIK, p[0].tip)
        assertEquals("Alışveriş Listesi", p[0].metin)
        assertEquals("\n", p[1].metin)
        assertEquals("süt al", p[2].metin)
        // ortadaki # başlık sayılmaz
        val q = NotBicim.cozumle("a # b")
        assertEquals(NotBicim.Tip.DUZ, q[0].tip)
    }

    @Test
    fun baslikIcindeKalin_veBosKalinAtlanir() {
        val p = NotBicim.cozumle("# **önemli** gün **** x")
        val tipler = p.map { it.tip }
        assertEquals(listOf(NotBicim.Tip.KALIN, NotBicim.Tip.BASLIK), tipler)
        assertEquals("önemli", p[0].metin)
        assertEquals(" gün  x", p[1].metin)
        assertEquals("önemli gün  x", duzBirlesik(p))
    }

    @Test
    fun okuma_dakika() {
        assertEquals(0, NotBicim.okumaDk(""))
        assertEquals(1, NotBicim.okumaDk("kısa not"))
        val uzun = (1..450).joinToString(" ") { "kelime$it" }
        assertEquals(3, NotBicim.okumaDk(uzun)) // 450/200 → 3 (yukarı yuvarlı)
    }
}
