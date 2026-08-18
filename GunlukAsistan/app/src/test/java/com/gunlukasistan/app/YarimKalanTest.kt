package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — "Yarım kalanları tamamla" paketinin saf testleri:
 *  · [FonksiyonCagrisiMotoru] — native function-calling adaptörü
 *  · [EkranYakalamaMotoru] — gerçek ekran görüntüsü karar motoru
 */
class YarimKalanTest {

    // ── FonksiyonCagrisiMotoru ──

    @Test
    fun `fonksiyon tanimi gerekli alanlari icerir`() {
        val f = FonksiyonCagrisiMotoru.fonksiyonTanimi("gorev_ekle", "Görev ekler", "Görev metni")
        assertEquals("uygulama_gorev_ekle", f.getString("name"))
        assertTrue(f.has("description"))
        assertTrue(f.has("parameters"))
    }

    @Test
    fun `fonksiyon listesi yaygin komutlari kapsar`() {
        val liste = FonksiyonCagrisiMotoru.fonksiyonListesi()
        assertTrue(liste.any { it.getString("name") == "uygulama_gorev_ekle" })
        assertTrue(liste.any { it.getString("name") == "uygulama_ekran_ac" })
        assertTrue(liste.size >= 10)
    }

    @Test
    fun `cevabi coz gemini functioncall jsonunu komuta cevirir`() {
        val json = """[{"name":"uygulama_gorev_ekle","args":{"deger":"Proje sunumu"}},{"name":"uygulama_ayar_gece","args":{"deger":"koyu"}}]"""
        val komutlar = FonksiyonCagrisiMotoru.cevabiCoz(json)
        assertEquals(2, komutlar.size)
        assertEquals("gorev_ekle", komutlar[0].ad)
        assertEquals("Proje sunumu", komutlar[0].deger)
        assertEquals("ayar_gece", komutlar[1].ad)
        assertEquals("koyu", komutlar[1].deger)
    }

    @Test
    fun `cevabi coz arguments alanini da destekler`() {
        val json = """[{"name":"uygulama_zamanlayici","arguments":{"deger":"25"}}]"""
        val komutlar = FonksiyonCagrisiMotoru.cevabiCoz(json)
        assertEquals("zamanlayici", komutlar[0].ad)
        assertEquals("25", komutlar[0].deger)
    }

    @Test
    fun `cevabi coz bos ve gecersiz girdide bos liste doner`() {
        assertTrue(FonksiyonCagrisiMotoru.cevabiCoz("").isEmpty())
        assertTrue(FonksiyonCagrisiMotoru.cevabiCoz("not json").isEmpty())
        assertTrue(FonksiyonCagrisiMotoru.cevabiCoz("[1,2,3]").isEmpty())
    }

    @Test
    fun `cevabi coz prefikssiz adi da kabul eder`() {
        val json = """[{"name":"gorev_ekle","deger":"Kitap oku"}]"""
        val komutlar = FonksiyonCagrisiMotoru.cevabiCoz(json)
        assertEquals("gorev_ekle", komutlar[0].ad)
    }

    // ── EkranYakalamaMotoru ──

    @Test
    fun `gorsel istem amaci ve tikla emrini icerir`() {
        val istem = EkranYakalamaMotoru.gorselIstemiKur("ayarları aç")
        assertTrue(istem.contains("ayarları aç"))
        assertTrue(istem.contains("tikla|"))
    }

    @Test
    fun `yakinlastir pikselleri kareye indirger ve ikilestirir`() {
        val satirlar = listOf(
            intArrayOf(0, 0, 255, 255),
            intArrayOf(255, 255, 0, 0)
        )
        val kare = EkranYakalamaMotoru.yakinlastir(satirlar, 2)
        assertEquals(2, kare.size)
        assertEquals(2, kare[0].length)
        assertEquals("01", kare[0])
        assertEquals("10", kare[1])
    }

    @Test
    fun `yakinlastir bos satir listesinde bos doner`() {
        assertTrue(EkranYakalamaMotoru.yakinlastir(emptyList()).isEmpty())
    }

    @Test
    fun `kucuk kare en sabiti gecerli`() {
        assertTrue(EkranYakalamaMotoru.KUCUK_KARE_EN > 0)
    }
}
