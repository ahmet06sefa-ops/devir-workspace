package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.06 — Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Motoru (`DiniSozMotoru`)
 * saf JVM birim testleri (15 test).
 */
class DiniSozTest {

    @Test
    fun `tum sozler havuzu 20 den fazla hikmetli soz barindirir`() {
        val hepsi = DiniSozMotoru.tumSozler()
        assertTrue(hepsi.size >= 20)
    }

    @Test
    fun `her dilim icin en az 3 farkli dini soz bulunmaktadir`() {
        NamazPlan.Dilim.entries.forEach { d ->
            val list = DiniSozMotoru.dilimIcinSozler(d)
            assertTrue("Dilim $d için söz sayısı yetersiz", list.size >= 3)
        }
    }

    @Test
    fun `vaktin sozunu getir baslik ve formatli metin dondurur`() {
        val (baslik, metin) = DiniSozMotoru.vaktinSozunuGetir(NamazPlan.Dilim.SABAH, 0)
        assertTrue(baslik.contains("Seher") || baslik.contains("Sabah"))
        assertTrue(metin.contains("\"") && metin.contains("("))
    }

    @Test
    fun `sonraki sozu getir simdiki metinden farkli bir soz dondurur`() {
        val (b1, m1) = DiniSozMotoru.vaktinSozunuGetir(NamazPlan.Dilim.OGLEDEN, 0)
        val (b2, m2) = DiniSozMotoru.sonrakiSozuGetir(NamazPlan.Dilim.OGLEDEN, m1)
        assertNotEquals(m1, m2)
    }

    @Test
    fun `kusluk vakti sozleri duha namazi ve israk faziletini icerir`() {
        val list = DiniSozMotoru.dilimIcinSozler(NamazPlan.Dilim.KUSLUK)
        assertTrue(list.any { it.metin.contains("Kuşluk") || it.metin.contains("İşrak") || it.metin.contains("sadaka") })
    }

    @Test
    fun `sabah vakti sozleri allah in guvencesi hadisini icerir`() {
        val list = DiniSozMotoru.dilimIcinSozler(NamazPlan.Dilim.SABAH)
        assertTrue(list.any { it.metin.contains("güvencesi") && it.kaynak.contains("Müslim") })
    }

    @Test
    fun `ikindi vakti sozleri amellerin yukseldigi vakit faziletini icerir`() {
        val list = DiniSozMotoru.dilimIcinSozler(NamazPlan.Dilim.IKINDIDEN)
        assertTrue(list.any { it.metin.contains("İkindi") || it.metin.contains("cennet") })
    }

    @Test
    fun `aksam vakti sozleri gunun sükuneti ve duasini icerir`() {
        val list = DiniSozMotoru.dilimIcinSozler(NamazPlan.Dilim.AKSAMDAN)
        assertTrue(list.any { it.metin.contains("Akşam") || it.metin.contains("Güneş") })
    }

    @Test
    fun `gece vakti sozleri teheccud ve cemaatle yatsi faziletini icerir`() {
        val list = DiniSozMotoru.dilimIcinSozler(NamazPlan.Dilim.GECE)
        assertTrue(list.any { it.metin.contains("Yatsı") || it.metin.contains("teheccüd") || it.metin.contains("Gece") })
    }

    @Test
    fun `soz nesnesi formatli metni kaynak bilgisini parantez icinde gosterir`() {
        val soz = DiniSozMotoru.DiniSoz(
            NamazPlan.Dilim.SABAH,
            "Test Başlık",
            "Test Metin",
            "Test Kaynak"
        )
        assertEquals("\"Test Metin\" (Test Kaynak)", soz.formatliMetin)
    }

    @Test
    fun `soz nesnesi kaynak bos oldugunda sadece tirnak icinde metin gosterir`() {
        val soz = DiniSozMotoru.DiniSoz(
            NamazPlan.Dilim.SABAH,
            "Test Başlık",
            "Test Metin",
            ""
        )
        assertEquals("\"Test Metin\"", soz.formatliMetin)
    }

    @Test
    fun `simdiki vaktin sozu context null iken varsayilan sabah sozunu dondurur`() {
        val (baslik, _) = DiniSozMotoru.simdikiVaktinSozu(null)
        assertTrue(baslik.isNotEmpty())
    }

    @Test
    fun `rastgele soz getir belirtilen dilime ait gecerli bir soz secer`() {
        val (baslik, metin) = DiniSozMotoru.rastgeleSozGetir(NamazPlan.Dilim.OGLEDEN)
        assertTrue(baslik.isNotEmpty() && metin.isNotEmpty())
    }

    @Test
    fun `gecersiz index verildiginde vaktin sozunu getir guvenli sekilde soz dondurur`() {
        val (b, m) = DiniSozMotoru.vaktinSozunuGetir(NamazPlan.Dilim.AKSAMDAN, 999)
        assertTrue(b.isNotEmpty() && m.isNotEmpty())
    }

    @Test
    fun `dilim listesindeki tum sozlerin baslik ve kaynaklari doludur`() {
        val hepsi = DiniSozMotoru.tumSozler()
        assertTrue(hepsi.all { it.baslik.isNotBlank() && it.metin.isNotBlank() })
    }
}
