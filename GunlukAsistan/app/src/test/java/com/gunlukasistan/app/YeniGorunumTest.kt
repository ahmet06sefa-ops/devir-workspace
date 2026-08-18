package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.15 — "Yeni Görünüm" koyu teması koruma testleri.
 *
 * Kullanıcı isteği: ekranları koyu "Yeni Görünüm"e çevir, mevcut görünümü
 * "Eski Görünüm" olarak ayarlarda sakla. Bu testler yeni temanın listede
 * olduğunu, koyu olduğunu ve mevcut tema/neon mantığını bozmadığını korur.
 */
class YeniGorunumTest {

    @Test
    fun `yeni gorunum tema listesinde yer alir`() {
        assertTrue(
            ThemeManager.specs.any { it.title == "Yeni Görünüm" }
        )
    }

    @Test
    fun `yeni gorunum koyu bir temadir`() {
        val yeni = ThemeManager.specs.first { it.title == "Yeni Görünüm" }
        assertTrue(yeni.dark)
    }

    @Test
    fun `yeni gorunum bir stil kaynagi ile tanimlidir`() {
        val yeni = ThemeManager.specs.first { it.title == "Yeni Görünüm" }
        assertTrue(yeni.styleRes != 0)
    }

    @Test
    fun `yeni gorunum halka ve kart rengine sahiptir`() {
        val yeni = ThemeManager.specs.first { it.title == "Yeni Görünüm" }
        assertTrue(yeni.ringColor != 0 && yeni.cardColor != 0)
    }

    @Test
    fun `tema basliklari yeni tema ile birlikte benzersiz kalir`() {
        val basliklar = ThemeManager.specs.map { it.title }
        assertEquals(basliklar.size, basliklar.toSet().size)
    }

    @Test
    fun `zincir temasinin ismi korunmustur`() {
        assertTrue(ThemeManager.specs.any { it.title == "Zincir" })
    }

    @Test
    fun `eski gorunum temalari hala listededir`() {
        assertTrue(ThemeManager.specs.any { it.title == "Krem" })
        assertTrue(ThemeManager.specs.any { it.title == "Violet" })
    }

    @Test
    fun `tema listesi hem acik hem koyu icerir`() {
        assertTrue(ThemeManager.specs.any { it.dark })
        assertTrue(ThemeManager.specs.any { !it.dark })
    }
}
