package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.53 — Kullanıcı maddesi 1-30 + Bonus 31-32:
 * [TasarimAtolye] 32 Maddelik Tasarım ve Yerleşim Özelleştirme Atölyesi
 * saf birim testleri (24 test).
 */
class TasarimAtolyeTest {

    @Test
    fun `hex renk dogrula 6li gecerli hex dizeyi tanir`() {
        assertTrue(TasarimAtolye.hexRenkDogrula("#4C7DFF"))
        assertTrue(TasarimAtolye.hexRenkDogrula("#0e0e13"))
    }

    @Test
    fun `hex renk dogrula 8li gecerli hex dizeyi tanir`() {
        assertTrue(TasarimAtolye.hexRenkDogrula("#FF4C7DFF"))
    }

    @Test
    fun `hex renk dogrula gecersiz dizeyi reddeder`() {
        assertFalse(TasarimAtolye.hexRenkDogrula("4C7DFF")) // # eksik
        assertFalse(TasarimAtolye.hexRenkDogrula("#4C7DF"))  // 5 haneli
        assertFalse(TasarimAtolye.hexRenkDogrula("#ZZZZZZ"))
    }

    @Test
    fun `parse hex veya varsayilan gecersiz dizede varsayilani dondurur`() {
        assertEquals("#4C7DFF", TasarimAtolye.parseHexVeyaVarsayilan("gecersiz"))
    }

    @Test
    fun `parse hex veya varsayilan gecerli dizede buyuk harfli hex dondurur`() {
        assertEquals("#22C55E", TasarimAtolye.parseHexVeyaVarsayilan("#22c55e"))
    }

    @Test
    fun `kart saydamlik alfa 0 yuzdede sifir dondurur`() {
        assertEquals(0, TasarimAtolye.kartSaydamlikAlfa(0))
    }

    @Test
    fun `kart saydamlik alfa 100 yuzdede 255 dondurur`() {
        assertEquals(255, TasarimAtolye.kartSaydamlikAlfa(100))
    }

    @Test
    fun `kart saydamlik alfa 50 yuzdede 127 dondurur`() {
        assertEquals(127, TasarimAtolye.kartSaydamlikAlfa(50))
    }

    @Test
    fun `konu ozel renk hex indekse gore dogru rengi dondurur`() {
        assertEquals("#4C7DFF", TasarimAtolye.konuOzelRenkHex(0))
        assertEquals("#2FA8A0", TasarimAtolye.konuOzelRenkHex(7))
    }

    @Test
    fun `konu ozel renk hex sinir disi indekste kelepce atar`() {
        assertEquals("#4C7DFF", TasarimAtolye.konuOzelRenkHex(-5))
        assertEquals("#2FA8A0", TasarimAtolye.konuOzelRenkHex(99))
    }

    @Test
    fun `kose yaricapi dp varsayilan indekste 16dp dondurur`() {
        assertEquals(16, TasarimAtolye.koseYaricapiDp(2))
    }

    @Test
    fun `kose yaricapi dp 0 indekste keskin 0dp dondurur`() {
        assertEquals(0, TasarimAtolye.koseYaricapiDp(0))
    }

    @Test
    fun `kose yaricapi dp 3 indekste devasa 24dp dondurur`() {
        assertEquals(24, TasarimAtolye.koseYaricapiDp(3))
    }

    @Test
    fun `max lines sinirla 1 ile 10 arasina kelepceler`() {
        assertEquals(5, TasarimAtolye.maxLinesSinirla(5))
        assertEquals(1, TasarimAtolye.maxLinesSinirla(-3))
        assertEquals(10, TasarimAtolye.maxLinesSinirla(99))
    }

    @Test
    fun `akordiyon durum karari her zaman acikta dogru dondurur`() {
        assertTrue(TasarimAtolye.akordiyonDurumKarari(TasarimAtolye.AkordiyonDurum.HER_ZAMAN_ACIK, 0))
    }

    @Test
    fun `akordiyon durum karari her zaman kapalida yanlis dondurur`() {
        assertFalse(TasarimAtolye.akordiyonDurumKarari(TasarimAtolye.AkordiyonDurum.HER_ZAMAN_KAPALI, 10))
    }

    @Test
    fun `akordiyon durum karari sadece doluyken dolu listeyle dogru dondurur`() {
        assertTrue(TasarimAtolye.akordiyonDurumKarari(TasarimAtolye.AkordiyonDurum.SADECE_DOLUYKEN_ACIK, 3))
    }

    @Test
    fun `akordiyon durum karari sadece doluyken bos listeyle yanlis dondurur`() {
        assertFalse(TasarimAtolye.akordiyonDurumKarari(TasarimAtolye.AkordiyonDurum.SADECE_DOLUYKEN_ACIK, 0))
    }

    @Test
    fun `profil json uret ve coz donusumu veriyi korur`() {
        val ozel = TasarimAtolye.AtolyeProfili(
            ad = "Ahmet Özel",
            ozelHexVurgu = "#22C55E",
            kartSaydamlikYuzde = 85,
            koseYaricapiIndeks = 3,
            maxLines = 5
        )
        val json = TasarimAtolye.profilJsonUret(ozel)
        val cozuldu = TasarimAtolye.profilJsonCoz(json)
        assertEquals("Ahmet Özel", cozuldu.ad)
        assertEquals("#22C55E", cozuldu.ozelHexVurgu)
        assertEquals(85, cozuldu.kartSaydamlikYuzde)
        assertEquals(3, cozuldu.koseYaricapiIndeks)
        assertEquals(5, cozuldu.maxLines)
    }

    @Test
    fun `profil json coz bos veya null dizede fabrika varsayilanini dondurur`() {
        val cozuldu = TasarimAtolye.profilJsonCoz(null)
        assertEquals("Varsayılan v2", cozuldu.ad)
        assertEquals("#4C7DFF", cozuldu.ozelHexVurgu)
    }

    @Test
    fun `canli onizleme kart metni profil ozetini dogru formatlar`() {
        val profil = TasarimAtolye.AtolyeProfili(
            ad = "Sakin Akşam",
            ozelHexVurgu = "#F59E0B",
            koseYaricapiIndeks = 2,
            kartSaydamlikYuzde = 90
        )
        val metin = TasarimAtolye.canliOnizlemeKartMetni(profil)
        assertTrue("Sakin Akşam" in metin)
        assertTrue("#F59E0B" in metin)
        assertTrue("16dp" in metin)
        assertTrue("%90" in metin)
    }

    @Test
    fun `fabrika varsayilan profili varsayilan v2 adini dondurur`() {
        assertEquals("Varsayılan v2", TasarimAtolye.fabrikaVarsayilanProfili().ad)
    }
}
