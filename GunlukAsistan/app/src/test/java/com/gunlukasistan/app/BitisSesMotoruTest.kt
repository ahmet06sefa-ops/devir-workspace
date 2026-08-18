package com.gunlukasistan.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Bitiş sesi motoru (`BitisSesMotoru`) saf JVM testleri.
 *
 * `susmaKarari` ortak mantığını doğrular: güç düğmesi (ekran kapandı),
 * kullanıcı "durdur" veya süre doldu — üçünden biri bile gerçekleşirse
 * alarm sesi ve titreşim susmalıdır ("güç düğmesi sesi susturmuyor"
 * düzeltmesinin mantık kısmı).
 */
class BitisSesMotoruTest {

    @Test
    fun `hicbir kosul yoksa susma gerekmez`() {
        assertFalse(
            BitisSesMotoru.susmaKarari(
                ekranKapandiMi = false,
                kullaniciDurdurduMu = false,
                sureDolduMu = false
            )
        )
    }

    @Test
    fun `ekran kapandiginda guc dugmesi ile susar`() {
        assertTrue(
            BitisSesMotoru.susmaKarari(ekranKapandiMi = true, kullaniciDurdurduMu = false, sureDolduMu = false)
        )
    }

    @Test
    fun `kullanici durdur dediginde susar`() {
        assertTrue(
            BitisSesMotoru.susmaKarari(ekranKapandiMi = false, kullaniciDurdurduMu = true, sureDolduMu = false)
        )
    }

    @Test
    fun `sure doldugunda susar`() {
        assertTrue(
            BitisSesMotoru.susmaKarari(ekranKapandiMi = false, kullaniciDurdurduMu = false, sureDolduMu = true)
        )
    }

    @Test
    fun `kombinasyonlardan herhangi biri bile susmayi tetikler`() {
        val gucVeKullanici = BitisSesMotoru.susmaKarari(true, true, false)
        val gucVeSure = BitisSesMotoru.susmaKarari(true, false, true)
        val kullaniciVeSure = BitisSesMotoru.susmaKarari(false, true, true)
        val hepsi = BitisSesMotoru.susmaKarari(true, true, true)
        assertTrue(gucVeKullanici)
        assertTrue(gucVeSure)
        assertTrue(kullaniciVeSure)
        assertTrue(hepsi)
    }

    @Test
    fun `saf mantik tek tip seyirli durumlarda da kararli calisir`() {
        // Yalnızca ekran kapalıyken susma isteyen kullanıcı (güç düğmesi önceliği)
        val sadeceGuc = BitisSesMotoru.susmaKarari(ekranKapandiMi = true, kullaniciDurdurduMu = false, sureDolduMu = false)
        assertTrue(sadeceGuc)
    }
}
