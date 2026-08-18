package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.51 — Kullanıcı maddesi 1-16: [TasarimDili] saf birim testleri (20 test).
 */
class TasarimDiliTest {

    @Test
    fun `durum seviyesi gecikmis durumda gecikmisi dondurur`() {
        assertEquals(TasarimDili.DurumSeviyesi.GECIKMIS, TasarimDili.durumSeviyesi(50, gecikmisMi = true))
    }

    @Test
    fun `durum seviyesi 100 yuzdede tamamlandi dondurur`() {
        assertEquals(TasarimDili.DurumSeviyesi.TAMAMLANDI, TasarimDili.durumSeviyesi(100))
    }

    @Test
    fun `durum seviyesi 50 yuzdede devam ediyor dondurur`() {
        assertEquals(TasarimDili.DurumSeviyesi.DEVAM_EDIYOR, TasarimDili.durumSeviyesi(50))
    }

    @Test
    fun `durum seviyesi 0 yuzdede normal dondurur`() {
        assertEquals(TasarimDili.DurumSeviyesi.NORMAL, TasarimDili.durumSeviyesi(0))
    }

    @Test
    fun `durum rengi hex dogru renk kodlarini dondurur`() {
        assertEquals("#22C55E", TasarimDili.durumRengiHex(TasarimDili.DurumSeviyesi.TAMAMLANDI))
        assertEquals("#F59E0B", TasarimDili.durumRengiHex(TasarimDili.DurumSeviyesi.DEVAM_EDIYOR))
        assertEquals("#EF4444", TasarimDili.durumRengiHex(TasarimDili.DurumSeviyesi.GECIKMIS))
        assertEquals("#4C7DFF", TasarimDili.durumRengiHex(TasarimDili.DurumSeviyesi.NORMAL))
    }

    @Test
    fun `konu ilerleme opaklik 0 yuzdede alt sinir dondurur`() {
        assertEquals(0.35f, TasarimDili.konuIlerlemeOpaklik(0), 0.001f)
    }

    @Test
    fun `konu ilerleme opaklik 100 yuzdede tam opaklik dondurur`() {
        assertEquals(1.00f, TasarimDili.konuIlerlemeOpaklik(100), 0.001f)
    }

    @Test
    fun `konu ilerleme opaklik 50 yuzdede orta opaklik dondurur`() {
        assertEquals(0.675f, TasarimDili.konuIlerlemeOpaklik(50), 0.001f)
    }

    @Test
    fun `vakit rozet metni hem ad hem saat varken ikisini birlesik dondurur`() {
        assertEquals("Öğle 13:02", TasarimDili.vakitRozetMetni("Öğle", "13:02"))
    }

    @Test
    fun `vakit rozet metni saat yoksa sadece adi dondurur`() {
        assertEquals("Öğle", TasarimDili.vakitRozetMetni("Öğle", ""))
    }

    @Test
    fun `vakit rozet metni bos girdide bos dize dondurur`() {
        assertEquals("", TasarimDili.vakitRozetMetni("", ""))
    }

    @Test
    fun `gunun ozeti metni sinav tarihi varken ikisini birlesik dondurur`() {
        val metin = TasarimDili.gununOzetiMetni(7, 24, 10)
        assertEquals("Kararmaya 7sa 24dk · 10 gün kaldı sınava", metin)
    }

    @Test
    fun `gunun ozeti metni sinav yoksa sadece kararmayi dondurur`() {
        val metin = TasarimDili.gununOzetiMetni(7, 24, null)
        assertEquals("Kararmaya 7sa 24dk", metin)
    }

    @Test
    fun `aktif vakit dilimi indeksi seher saatinde 0 dondurur`() {
        assertEquals(0, TasarimDili.aktifVakitDilimiIndeksi(4))
    }

    @Test
    fun `aktif vakit dilimi indeksi ogle saatinde 2 dondurur`() {
        assertEquals(2, TasarimDili.aktifVakitDilimiIndeksi(13))
    }

    @Test
    fun `aktif vakit dilimi indeksi gece saatinde 5 dondurur`() {
        assertEquals(5, TasarimDili.aktifVakitDilimiIndeksi(22))
    }

    @Test
    fun `akordiyon daraltilmali mi liste bossa hep dogru dondurur`() {
        assertTrue(TasarimDili.akordiyonDaraltilmaliMi(0, kullaniciAcikMi = true))
        assertTrue(TasarimDili.akordiyonDaraltilmaliMi(0, kullaniciAcikMi = false))
    }

    @Test
    fun `akordiyon daraltilmali mi liste doluyken kullanici secimine bakar`() {
        assertFalse(TasarimDili.akordiyonDaraltilmaliMi(5, kullaniciAcikMi = true))
        assertTrue(TasarimDili.akordiyonDaraltilmaliMi(5, kullaniciAcikMi = false))
    }

    @Test
    fun `ikon sembolu namaz modulu icin dogru sembol dondurur`() {
        assertEquals("◊", TasarimDili.ikonSembolu("namaz"))
    }

    @Test
    fun `ikon sembolu gorev modulu icin dogru sembol dondurur`() {
        assertEquals("☑", TasarimDili.ikonSembolu("gorev"))
    }
}
