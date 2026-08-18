package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.55 — 10 Özel Yaşam Modülü & Manuel Kontrol Merkezi ([YasamModulleri])
 * saf birim testleri (18 test).
 */
class YasamModulleriTest {

    @Test
    fun `modul 1 ilac ozeti dogru yuzde hesaplar`() {
        val list = listOf(
            YasamModulleri.IlacKaydi("B12", 1000, "08:00", true, true),
            YasamModulleri.IlacKaydi("D3", 2000, "12:00", false, false)
        )
        val ozet = YasamModulleri.IlacHatirlatisi.ilacOzeti(list)
        assertTrue("1/2" in ozet)
        assertTrue("%50" in ozet)
    }

    @Test
    fun `modul 1 doz metni yemekten once ve alindi formatlar`() {
        val ilac = YasamModulleri.IlacKaydi("C Vitamini", 500, "09:00", true, true)
        val metin = YasamModulleri.IlacHatirlatisi.dozMetni(ilac)
        assertTrue("Yemekten Önce" in metin)
        assertTrue("☑ Alındı" in metin)
    }

    @Test
    fun `modul 2 aylik fatura tutarini dogru toplar`() {
        val abonelikler = listOf(
            YasamModulleri.Abonelik("Netflix", 150, 15, true),
            YasamModulleri.Abonelik("Spotify", 60, 20, false)
        )
        assertEquals(210, YasamModulleri.FaturaTakipci.aylikToplamTutar(abonelikler))
    }

    @Test
    fun `modul 2 fatura ozeti odenen ve kalan dogru hesaplar`() {
        val abonelikler = listOf(
            YasamModulleri.Abonelik("İnternet", 300, 5, true),
            YasamModulleri.Abonelik("Su", 100, 10, false)
        )
        val ozet = YasamModulleri.FaturaTakipci.faturaOzeti(abonelikler)
        assertTrue("Aylık Yük: 400 ₺" in ozet)
        assertTrue("Ödenen: 300 ₺" in ozet)
        assertTrue("Kalan: 100 ₺" in ozet)
    }

    @Test
    fun `modul 3 su ekleme ve hidrasyon yuzdesi dogru calisir`() {
        var durum = YasamModulleri.SuKafeinDurumu(suMl = 0, suHedefMl = 2500)
        durum = YasamModulleri.SuKafeinSayaci.suEkle(durum, 1250)
        assertEquals(1250, durum.suMl)
        assertEquals(50, YasamModulleri.SuKafeinSayaci.hidrasyonYuzdesi(durum))
    }

    @Test
    fun `modul 3 kafein siniri asilinca uyari verir`() {
        val durum = YasamModulleri.SuKafeinDurumu(kafeinMg = 450, kafeinSinirMg = 400)
        val uyari = YasamModulleri.SuKafeinSayaci.saglikUyarisi(durum)
        assertTrue("KAFEİN SINIRI AŞILDI" in uyari)
    }

    @Test
    fun `modul 4 varsayilan rozetler 10 adettir ve acilis calisir`() {
        var rozetler = YasamModulleri.RozetKilitMerkezi.varsayilanRozetler()
        assertEquals(10, rozetler.size)
        assertFalse(rozetler.first { it.id == "r4" }.acildiMi)
        rozetler = YasamModulleri.RozetKilitMerkezi.rozetAc(rozetler, "r4")
        assertTrue(rozetler.first { it.id == "r4" }.acildiMi)
    }

    @Test
    fun `modul 5 biyo ritim ideal uyanma saatini dogru hesaplar`() {
        // 23:00'te yatış, 15 dk dalış + 5 döngü (450 dk) = 465 dk -> 06:45
        val plan = YasamModulleri.UykuDonguPlan(uyumaSaat = 23, uyumaDakika = 0, donguSayisi = 5)
        assertEquals("06:45", YasamModulleri.BiyoRitimAyari.idealUyanmaSaati(plan))
    }

    @Test
    fun `modul 5 dinc uyanma puan 5 dongude 95 verir`() {
        assertEquals(95, YasamModulleri.BiyoRitimAyari.dincUyanmaPuan(5))
    }

    @Test
    fun `modul 6 ambient mikser ozeti acik sesleri gosterir`() {
        val ayar = YasamModulleri.AmbientMikserAyari(yagmurSeviye = 50, gamma40HzAcik = true)
        val ozet = YasamModulleri.AmbientMikser.mikserOzeti(ayar)
        assertTrue("Yağmur: %50" in ozet)
        assertTrue("40Hz Gamma" in ozet)
    }

    @Test
    fun `modul 7 harcama ozeti ve en cok harcanan kategori dogru secer`() {
        val harcamalar = listOf(
            YasamModulleri.Harcama("Market", 200, "Peynir vb"),
            YasamModulleri.Harcama("Market", 150, "Meyve"),
            YasamModulleri.Harcama("Ulaşım", 50, "Otobüs")
        )
        val ozet = YasamModulleri.HizliHarcama.harcamaOzeti(harcamalar, 500)
        assertTrue("400 ₺" in ozet)
        assertTrue("Kalan: 100 ₺" in ozet)
        assertEquals("Market", YasamModulleri.HizliHarcama.enCokHarcamaKategori(harcamalar))
    }

    @Test
    fun `modul 8 acil durum kasasi kart metni bilgileri icerir`() {
        val kasa = YasamModulleri.AcilKasa(kanGrubu = "0 Rh+", sosKisi = "Ahmet", sosTelefon = "05550000000")
        val metin = YasamModulleri.AcilDurumKasasi.acilKasaKartMetni(kasa)
        assertTrue("0 Rh+" in metin)
        assertTrue("Ahmet" in metin)
        assertTrue("05550000000" in metin)
    }

    @Test
    fun `modul 9 ai koc tonu baslik ve prompt ek dondurur`() {
        val ton = YasamModulleri.AiTonu.SOKRATIK_FILOZOF
        assertTrue("Sokratik Filozof" in YasamModulleri.AiKocTonu.tonBasligiGetir(ton))
        assertTrue("felsefi bir derinlik" in YasamModulleri.AiKocTonu.tonPromptGetir(ton))
    }

    @Test
    fun `modul 10 veri klonlayici json uretim ve cozum dogru calisir`() {
        val su = YasamModulleri.SuKafeinDurumu(suMl = 1500, kafeinMg = 160)
        val ton = YasamModulleri.AiTonu.ESPIRILI_POFI
        val mikser = YasamModulleri.AmbientMikserAyari(yagmurSeviye = 80, gamma40HzAcik = true)

        val json = YasamModulleri.VeriKlonlayici.klonJsonUret(su, ton, mikser)
        val cozum = YasamModulleri.VeriKlonlayici.klonJsonCoz(json)

        assertEquals(1500, cozum.first.suMl)
        assertEquals(160, cozum.first.kafeinMg)
        assertEquals(YasamModulleri.AiTonu.ESPIRILI_POFI, cozum.second)
        assertEquals(80, cozum.third.yagmurSeviye)
        assertTrue(cozum.third.gamma40HzAcik)
    }

    @Test
    fun `modul 10 veri klonlayici null json verisinde varsayilan dondurur`() {
        val cozum = YasamModulleri.VeriKlonlayici.klonJsonCoz(null)
        assertEquals(0, cozum.first.suMl)
        assertEquals(YasamModulleri.AiTonu.SEFKATLI_ZEN, cozum.second)
        assertFalse(cozum.third.gamma40HzAcik)
    }
}
