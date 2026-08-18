package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Adım adım görünür eylem yürütücüsü (`AdimliEylemMotoru`) saf JVM testleri.
 *
 * Kullanıcının "AI ekrandan gerçekleştirsin, hepsini tek tek göreyim" isteğinin
 * planlama katmanını doğrular: adım açıklamaları, sıra ve tamamlama mantığı.
 */
class AdimliEylemMotoruTest {

    @Test
    fun `bos komut listesi bos plan verir`() {
        assertEquals(0, AdimliEylemMotoru.adimlaraCevir(emptyList()).size)
    }

    @Test
    fun `komut listesi her komut icin bir adim olusturur`() {
        val komutlar = listOf(
            AsistanKomut.Komut("gorev_ekle", "Kitap oku :: yarın"),
            AsistanKomut.Komut("ayar_gece", "koyu"),
            AsistanKomut.Komut("ekran_ac", "kurslar")
        )
        val plan = AdimliEylemMotoru.adimlaraCevir(komutlar)
        assertEquals(3, plan.size)
    }

    @Test
    fun `gorev ekle adimi anlasilir aciklama uretir`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(
            listOf(AsistanKomut.Komut("gorev_ekle", "Proje sunumu"))
        )
        assertEquals("Görev ekle: Proje sunumu", plan[0].aciklama)
    }

    @Test
    fun `ayar komutu acik kapali durumunu aciklamaya yansitir`() {
        val acik = AdimliEylemMotoru.adimlaraCevir(listOf(AsistanKomut.Komut("ayar_ses", "acik")))
        val kapali = AdimliEylemMotoru.adimlaraCevir(listOf(AsistanKomut.Komut("ayar_ses", "kapanik")))
        assertTrue(acik[0].aciklama.contains("açık"))
        assertTrue(kapali[0].aciklama.contains("kapalı"))
    }

    @Test
    fun `ekran ac komutu hedef ekrani aciklar`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(listOf(AsistanKomut.Komut("ekran_ac", "kurslar")))
        assertTrue(plan[0].aciklama.contains("kurslar"))
    }

    @Test
    fun `siradaki plan baslangicta ilk adimda`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(
            listOf(AsistanKomut.Komut("gorev_ekle", "x"), AsistanKomut.Komut("ozet_ver", ""))
        )
        val d = AdimliEylemMotoru.siradaki(plan)
        assertEquals(0, d.siradaki)
        assertEquals(2, d.toplam)
        assertEquals(false, d.bitti)
    }

    @Test
    fun `tamamla sonraki adima gecer ve son adimda biter`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(
            listOf(AsistanKomut.Komut("ozet_ver", ""), AsistanKomut.Komut("yedek_al", ""))
        )
        var d = AdimliEylemMotoru.siradaki(plan)
        d = AdimliEylemMotoru.tamamla(d)
        assertEquals(1, d.siradaki)
        d = AdimliEylemMotoru.tamamla(d)
        assertEquals(2, d.siradaki)
        assertTrue(d.bitti)
    }

    @Test
    fun `gec de tamamla gibi sonraki adima gecer`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(listOf(AsistanKomut.Komut("ozet_ver", "")))
        var d = AdimliEylemMotoru.siradaki(plan)
        d = AdimliEylemMotoru.gec(d)
        assertEquals(1, d.siradaki)
        assertTrue(d.bitti)
    }

    @Test
    fun `bilinmeyen komut icin varsayilan aciklama uretilir`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(
            listOf(AsistanKomut.Komut("bilinmeyen_komut", "hedef"))
        )
        assertTrue(plan[0].aciklama.contains("hedef"))
    }

    @Test
    fun `tamamla toplam adim sayisini asmaz`() {
        val plan = AdimliEylemMotoru.adimlaraCevir(listOf(AsistanKomut.Komut("ozet_ver", "")))
        var d = AdimliEylemMotoru.siradaki(plan)
        d = AdimliEylemMotoru.tamamla(d)
        d = AdimliEylemMotoru.tamamla(d) // sınır ötesi
        assertEquals(1, d.siradaki)
        assertEquals(1, d.toplam)
    }
}
