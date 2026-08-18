package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — CSV veri dışa aktarma motoru saf testleri.
 */
class CsvDisAktarMotoruTest {

    @Test
    fun `virgulsuz deger degismeden doner`() {
        assertEquals("görev", CsvDisAktarMotoru.hucre("görev"))
    }

    @Test
    fun `virgullu deger tirnakla sarilir`() {
        assertEquals("\"a,b\"", CsvDisAktarMotoru.hucre("a,b"))
    }

    @Test
    fun `ic tirnak ikiye katlanir`() {
        assertEquals("\"de\"\"di\"", CsvDisAktarMotoru.hucre("de\"di"))
    }

    @Test
    fun `satir sonlu deger sarilir`() {
        assertEquals("\"sat1\nsat2\"", CsvDisAktarMotoru.hucre("sat1\nsat2"))
    }

    @Test
    fun `gorev satirlari veriyi dogru siralar`() {
        val gorevler = listOf(
            Store.Task(1L, "Proje sunumu", false, 100L),
            Store.Task(2L, "Rapor, yaz", true, 200L)
        )
        val satirlar = CsvDisAktarMotoru.gorevSatirlari(gorevler)
        assertEquals(2, satirlar.size)
        // varsayılan tekrar = "yok", etiket = ""
        assertEquals("1,Proje sunumu,bekliyor,yok,", satirlar[0])
        assertTrue(satirlar[1].startsWith("2,\"Rapor, yaz\",tamamlandı"))
    }

    @Test
    fun `aliskanlik satirlari veriyi dogru siralar`() {
        val aliskanliklar = listOf(
            Store.Habit(1L, "Su iç", "💧", 8, 0, false, 100L)
        )
        val satirlar = CsvDisAktarMotoru.aliskanlikSatirlari(aliskanliklar)
        assertEquals(1, satirlar.size)
        assertEquals("1,Su iç,8,false", satirlar[0])
    }

    @Test
    fun `birlestir baslik ve satirlari ayirir`() {
        val csv = CsvDisAktarMotoru.birlestir(
            CsvDisAktarMotoru.GOREV_BASLIK,
            listOf("1,A,bekliyor,,", "2,B,tamamlandı,,")
        )
        val satirlar = csv.split("\n")
        assertEquals(3, satirlar.size)
        assertEquals(CsvDisAktarMotoru.GOREV_BASLIK, satirlar[0])
    }
}
