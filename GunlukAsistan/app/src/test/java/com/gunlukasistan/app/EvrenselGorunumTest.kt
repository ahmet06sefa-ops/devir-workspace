package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.92 — 10.000-Madde Evrensel Görünüm ve Arayüz (UI/UX) Kişiselleştirme Atölyesi
 * ([EvrenselGorunumAtolye]) saf mantık, benzersizlik ve arama birim testleri (15 test).
 */
class EvrenselGorunumTest {

    @Test
    fun `evrensel gorunum atolye tam 10000 adet gorunum ogesi uretir`() {
        val list = EvrenselGorunumAtolye.tumGorunumleriGetir(null)
        assertEquals(10000, list.size)
    }

    @Test
    fun `evrensel gorunum atolye ilk ve son madde idleri 1 ve 10000 olarak dogrulanir`() {
        val list = EvrenselGorunumAtolye.tumGorunumleriGetir(null)
        assertEquals(1, list.first().id)
        assertEquals(10000, list.last().id)
    }

    @Test
    fun `evrensel gorunum atolye 10 ana kategori ve 100 gorunum alt basligi barindirir`() {
        val list = EvrenselGorunumAtolye.tumGorunumleriGetir(null)
        val kategoriler = list.map { it.kategoriNo }.distinct()
        val altBasliklar = list.map { it.altBaslikKodu }.distinct()
        assertEquals(10, kategoriler.size)
        assertEquals(100, altBasliklar.size)
    }

    @Test
    fun `evrensel gorunum atolye arama fonksiyonu no ve kelime ile suzer`() {
        val list = EvrenselGorunumAtolye.ara(null, "#5432")
        assertTrue(list.any { it.id == 5432 })
    }

    @Test
    fun `evrensel gorunum atolye kategoriye gore getir tam 1000 madde dondurur`() {
        val kat1 = EvrenselGorunumAtolye.kategoriyeGoreGetir(null, 1)
        assertEquals(1000, kat1.size)
        assertTrue(kat1.all { it.kategoriNo == 1 })
    }

    @Test
    fun `evrensel gorunum atolye alt basliga gore getir tam 100 madde dondurur`() {
        val g01a = EvrenselGorunumAtolye.altBasligaGoreGetir(null, "[G01-A]")
        assertEquals(100, g01a.size)
        assertTrue(g01a.all { it.altBaslikKodu == "[G01-A]" })
    }

    @Test
    fun `evrensel gorunum atolye varsayilan temayi oled emurekkep olarak ayarlar`() {
        assertTrue(EvrenselGorunumAtolye.varsayilanTemayiUygula(null))
        assertEquals("oled_emurekkep", EvrenselGorunumAtolye.VARSAYILAN_TEMA_MODU)
    }

    @Test
    fun `evrensel gorunum atolye tekil gorunumu uygula aninda calisir`() {
        val res = EvrenselGorunumAtolye.tekilGorunumuUygula(null, 101)
        assertTrue(res.first)
        assertTrue(res.second.contains("#101"))
    }

    @Test
    fun `evrensel gorunum atolye secili gorunumleri uygula hic madde secilmeyince 0 dondurur`() {
        val (n, _) = EvrenselGorunumAtolye.seciliGorunumleriUygula(null)
        assertEquals(0, n)
    }

    @Test
    fun `evrensel gorunum atolye tum 10000 oge baslik ve aciklamalari benzersizdir`() {
        val list = EvrenselGorunumAtolye.tumGorunumleriGetir(null)
        assertEquals(10000, list.map { it.id }.distinct().size)
        assertEquals(10000, list.map { it.baslik }.distinct().size)
        assertEquals(10000, list.map { it.aciklama }.distinct().size)
    }

    @Test
    fun `evrensel gorunum atolye ogelerin baslik ve aciklamalari bos olamaz`() {
        val list = EvrenselGorunumAtolye.kategoriyeGoreGetir(null, 1)
        assertTrue(list.all { it.baslik.isNotBlank() && it.aciklama.isNotBlank() })
    }

    @Test
    fun `evrensel gorunum atolye alt baslik kodu ile arama yapabilir`() {
        val list = EvrenselGorunumAtolye.ara(null, "[G05-A]")
        assertEquals(100, list.size)
    }

    @Test
    fun `evrensel gorunum atolye son madde 10000 numara ikon ve emurekkep basligi tasir`() {
        val son = EvrenselGorunumAtolye.ara(null, "#10000").first()
        assertEquals(10, son.kategoriNo)
        assertTrue(son.kategoriAdi.contains("E-Mürekkep"))
    }

    @Test
    fun `evrensel gorunum atolye 3uncu kategori kartlar ve kose yaricapi basligi tasir`() {
        val kat3 = EvrenselGorunumAtolye.kategoriyeGoreGetir(null, 3)
        assertTrue(kat3.all { it.kategoriNo == 3 })
        assertTrue(kat3.first().kategoriAdi.contains("Köşe"))
    }

    @Test
    fun `evrensel gorunum atolye 7nci kategori widget ve kilit ekrani basligi tasir`() {
        val kat7 = EvrenselGorunumAtolye.kategoriyeGoreGetir(null, 7)
        assertTrue(kat7.all { it.kategoriNo == 7 })
        assertTrue(kat7.first().kategoriAdi.contains("Widget"))
    }
}
