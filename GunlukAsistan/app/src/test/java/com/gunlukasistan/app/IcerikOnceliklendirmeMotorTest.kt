package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.14 — İçerik Önceliklendirme (Eisenhower) Motoru ([IcerikOnceliklendirmeMotoru]) testleri.
 */
class IcerikOnceliklendirmeMotorTest {

    @Test
    fun `kadran onem ve aciliyet yuksekse onemli acil dondurur`() {
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_ACIL,
            IcerikOnceliklendirmeMotoru.kadran(8, 9)
        )
    }

    @Test
    fun `kadran onem yuksek aciliyet dusukse planla dondurur`() {
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_ACIL_DEGIL,
            IcerikOnceliklendirmeMotoru.kadran(8, 2)
        )
    }

    @Test
    fun `kadran onem dusuk aciliyet yuksekse devret dondurur`() {
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_DEGIL_ACIL,
            IcerikOnceliklendirmeMotoru.kadran(2, 9)
        )
    }

    @Test
    fun `kadran ikisi dusukse ertele dondurur`() {
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_DEGIL_ACIL_DEGIL,
            IcerikOnceliklendirmeMotoru.kadran(1, 1)
        )
    }

    @Test
    fun `oncelik puani onem ve aciliyet 10 da 100 yapar`() {
        assertEquals(100, IcerikOnceliklendirmeMotoru.oncelikPuani(10, 10))
    }

    @Test
    fun `oncelik puani 0 ve 0 da 0 yapar`() {
        assertEquals(0, IcerikOnceliklendirmeMotoru.oncelikPuani(0, 0))
    }

    @Test
    fun `oncelik puani 0 ile 100 arasinda kalir`() {
        assertTrue(IcerikOnceliklendirmeMotoru.oncelikPuani(10, 10) <= 100)
        assertTrue(IcerikOnceliklendirmeMotoru.oncelikPuani(-3, -3) >= 0)
    }

    @Test
    fun `oncelik puani onemi aciliyetten agirlikli tutar`() {
        val onemli = IcerikOnceliklendirmeMotoru.oncelikPuani(10, 0)
        val acil = IcerikOnceliklendirmeMotoru.oncelikPuani(0, 10)
        assertTrue(onemli > acil)
    }

    @Test
    fun `gorev kadran ve puan ozelliklerini dogru verir`() {
        val g = IcerikOnceliklendirmeMotoru.Gorev("Test", 8, 8)
        assertEquals(IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_ACIL, g.kadran)
        assertEquals(80, g.puan)
    }

    @Test
    fun `sirala listeyi puana gore azalan siralar`() {
        val list = listOf(
            IcerikOnceliklendirmeMotoru.Gorev("Dusuk", 1, 1),
            IcerikOnceliklendirmeMotoru.Gorev("Yuksek", 10, 10)
        )
        assertEquals("Yuksek", IcerikOnceliklendirmeMotoru.sirala(list).first().ad)
    }

    @Test
    fun `matris siralama onemli acili oteye alir`() {
        val list = listOf(
            IcerikOnceliklendirmeMotoru.Gorev("Ertele", 1, 1),
            IcerikOnceliklendirmeMotoru.Gorev("HemenYap", 9, 9)
        )
        assertEquals("HemenYap", IcerikOnceliklendirmeMotoru.matrisSiralama(list).first().ad)
    }

    @Test
    fun `okunur bos listede gorev yok mesaji verir`() {
        assertTrue(IcerikOnceliklendirmeMotoru.okunur(emptyList()).contains("yok"))
    }

    @Test
    fun `okunur gorev listesini oncelik sirasi ile dizer`() {
        val metin = IcerikOnceliklendirmeMotoru.okunur(
            listOf(IcerikOnceliklendirmeMotoru.Gorev("Deneme", 8, 8))
        )
        assertTrue(metin.contains("1) Deneme"))
        assertTrue(metin.contains("Önemli + Acil"))
    }

    @Test
    fun `tek gorev tavsiyesi kadran ve puan icerir`() {
        val t = IcerikOnceliklendirmeMotoru.tekGorevTavsiyesi(
            IcerikOnceliklendirmeMotoru.Gorev("Proje", 8, 5)
        )
        assertTrue(t.contains("Proje"))
        assertTrue(t.contains("/100"))
    }

    @Test
    fun `kadran sinir degerini dogru yorumlar`() {
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_ACIL,
            IcerikOnceliklendirmeMotoru.kadran(6, 6)
        )
        assertEquals(
            IcerikOnceliklendirmeMotoru.Kadran.ONEMLI_ACIL_DEGIL,
            IcerikOnceliklendirmeMotoru.kadran(6, 5)
        )
    }
}
