package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.39 — FitnessMotor saf birim testleri (Context gerektirmeyen kısım).
 * Veritabanı okuma (assets) Robolectric ister, burada sadece çeviri ve
 * filtreleme mantığı test edilir.
 */
class FitnessMotorTest {

    private fun ornek(): FitnessMotor.Egzersiz = FitnessMotor.Egzersiz(
        id = "1",
        isim = "Barbell Bench Press",
        kaslar = listOf("chest"),
        ikincilKaslar = listOf("triceps"),
        ekipman = "barbell",
        seviye = "beginner",
        kategori = "strength",
        mekanik = "compound",
        talimatlar = listOf("Lie down", "Press up")
    )

    @Test
    fun `kas gruplari Turkce cevrilir`() {
        assertEquals("Karın", FitnessMotor.kasTuru("abdominals"))
        assertEquals("Göğüs", FitnessMotor.kasTuru("chest"))
        assertEquals("Ön Bacak", FitnessMotor.kasTuru("quadriceps"))
        // Bilinmeyen kod olduğu gibi döner
        assertEquals("x", FitnessMotor.kasTuru("x"))
    }

    @Test
    fun `ekipman Turkce cevrilir`() {
        assertEquals("Vücut Ağırlığı", FitnessMotor.ekipmanTuru("body only"))
        assertEquals("Dambıl", FitnessMotor.ekipmanTuru("dumbbell"))
        assertEquals("Halter", FitnessMotor.ekipmanTuru("barbell"))
    }

    @Test
    fun `seviye ve kategori Turkce cevrilir`() {
        assertEquals("Başlangıç", FitnessMotor.seviyeTuru("beginner"))
        assertEquals("İleri", FitnessMotor.seviyeTuru("expert"))
        assertEquals("Kuvvet", FitnessMotor.kategoriTuru("strength"))
    }

    @Test
    fun `kas grubuna gore filtreleme calisir`() {
        val chest = ornek()
        val biceps = ornek().copy(id = "2", isim = "Curl", kaslar = listOf("biceps"))
        val liste = listOf(chest, biceps)

        val sonuc = FitnessMotor.kasGrubunaGore(liste, "biceps")
        assertEquals(1, sonuc.size)
        assertEquals("Curl", sonuc[0].isim)

        // Boş/kas yoksa hepsini verir
        assertEquals(2, FitnessMotor.kasGrubunaGore(liste, null).size)
        assertEquals(2, FitnessMotor.kasGrubunaGore(liste, "").size)
    }

    @Test
    fun `ekipmana gore filtreleme calisir`() {
        val barbell = ornek()
        val dumbbell = ornek().copy(id = "2", isim = "DB Press", ekipman = "dumbbell")
        val liste = listOf(barbell, dumbbell)

        val sonuc = FitnessMotor.ekipmanaGore(liste, "dumbbell")
        assertEquals(1, sonuc.size)
        assertEquals("DB Press", sonuc[0].isim)
        // Büyük/küçük harf duyarsız
        assertEquals(1, FitnessMotor.ekipmanaGore(liste, "DUMBBELL").size)
    }

    @Test
    fun `arama buyuk kucuk harf duyarsizdir`() {
        val liste = listOf(
            ornek(),
            ornek().copy(id = "2", isim = "Push Up")
        )
        assertEquals(1, FitnessMotor.ara(liste, "PUSH").size)
        assertEquals(1, FitnessMotor.ara(liste, "push").size)
        assertEquals(1, FitnessMotor.ara(liste, "barbell").size)
        assertEquals(0, FitnessMotor.ara(liste, "squat").size)
        assertEquals(2, FitnessMotor.ara(liste, "").size)
    }

    @Test
    fun `ozet isim kas ve ekipman icerir`() {
        val ozet = FitnessMotor.ozet(ornek())
        assertTrue("Göğüs" in ozet)
        assertTrue("Halter" in ozet)
    }

    @Test
    fun `gun anahtari tarih biciminde`() {
        val anahtar = FitnessMotor.gunAnahtari(0L)
        assertTrue(anahtar.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}
