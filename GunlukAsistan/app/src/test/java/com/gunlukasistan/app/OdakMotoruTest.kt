package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.50 — Kullanıcı maddesi #11..#20: [OdakMotoru] 10 Aşırı İşlevsel Odak özelliği
 * saf birim testleri (18 test).
 */
class OdakMotoruTest {

    @Test
    fun `kesinti analiz et bos listede temiz seans dondurur`() {
        val ozet = OdakMotoru.kesintiAnalizEt(emptyList())
        assertEquals(0, ozet.toplamSayi)
        assertEquals(null, ozet.enCokSebep)
    }

    @Test
    fun `kesinti analiz et en cok tekrar eden sebebi dondurur`() {
        val kayitlar = listOf(
            OdakMotoru.KesintiKaydi(OdakMotoru.KesintiSebep.TELEFON, 30, 1000L),
            OdakMotoru.KesintiKaydi(OdakMotoru.KesintiSebep.TELEFON, 45, 2000L),
            OdakMotoru.KesintiKaydi(OdakMotoru.KesintiSebep.KAPI, 60, 3000L)
        )
        val ozet = OdakMotoru.kesintiAnalizEt(kayitlar)
        assertEquals(3, ozet.toplamSayi)
        assertEquals(OdakMotoru.KesintiSebep.TELEFON, ozet.enCokSebep)
        assertEquals(2, ozet.enCokSayi)
    }

    @Test
    fun `gorev tamamlama karari 5 dk altinda tamamlanmadi dondurur`() {
        assertFalse(OdakMotoru.gorevTamamlamaKarari(gorevId = 101L, odanilanDk = 4))
        assertFalse(OdakMotoru.gorevTamamlamaKarari(gorevId = 0L, odanilanDk = 25))
    }

    @Test
    fun `gorev tamamlama karari 5 dk ustunde ve gecerli gorevde dogru dondurur`() {
        assertTrue(OdakMotoru.gorevTamamlamaKarari(gorevId = 101L, odanilanDk = 25))
        assertTrue(OdakMotoru.gorevTamamlamaKarari(gorevId = 5L, odanilanDk = 5))
    }

    @Test
    fun `yorgunluk radari 3 kesinti ve kisa surede riskli dondurur`() {
        val rad = OdakMotoru.yorgunlukRadari(kesintiSayisi = 3, gecenDk = 15)
        assertTrue(rad.riskliMi)
        assertEquals(5, rad.onerilenMolaDk)
    }

    @Test
    fun `yorgunluk radari 18 dk ve 2 kesintide riskli dondurur`() {
        val rad = OdakMotoru.yorgunlukRadari(kesintiSayisi = 2, gecenDk = 19)
        assertTrue(rad.riskliMi)
    }

    @Test
    fun `yorgunluk radari stabil durumda risksiz dondurur`() {
        val rad = OdakMotoru.yorgunlukRadari(kesintiSayisi = 1, gecenDk = 15)
        assertFalse(rad.riskliMi)
        assertEquals(0, rad.onerilenMolaDk)
    }

    @Test
    fun `tasma suresi hesapla bitis asildiginda tasma ms dondurur`() {
        val tasma = OdakMotoru.tasmaSuresiHesapla(
            bitisDamgaMs = 1000L,
            simdiMs = 2500L,
            tasmaAcik = true
        )
        assertEquals(1500L, tasma)
    }

    @Test
    fun `tasma suresi hesapla tasma kapaliyken sifir dondurur`() {
        val tasma = OdakMotoru.tasmaSuresiHesapla(
            bitisDamgaMs = 1000L,
            simdiMs = 2500L,
            tasmaAcik = false
        )
        assertEquals(0L, tasma)
    }

    @Test
    fun `tasma metni saniye ve dakikayi dogru formatlar`() {
        val metin = OdakMotoru.tasmaMetni(74_000L) // 1 dk 14 sn
        assertEquals("⚡ +01:14 (Akış)", metin)
    }

    @Test
    fun `cikti notu formatla baslik ve ciktiyi birlestirir`() {
        val not = OdakMotoru.ciktiNotuFormatla("Türevden 30 soru çözüldü", 25, "Fizik Soru Çöz")
        assertEquals("[ODAK HASADI — 25 dk] Fizik Soru Çöz:\n💡 Türevden 30 soru çözüldü", not)
    }

    @Test
    fun `ses mikseri karari binaural kapaliyken sadece ortami dondurur`() {
        val mikser = OdakMotoru.sesMikseriKarari(
            "Yağmur", 60, OdakMotoru.BinauralFrekans.KAPALI, 0
        )
        assertEquals("Yağmur (%60)", mikser)
    }

    @Test
    fun `ses mikseri karari alfa ritmiyle yuzdeli metin dondurur`() {
        val mikser = OdakMotoru.sesMikseriKarari(
            "Kafe", 70, OdakMotoru.BinauralFrekans.GAMA_40HZ, 30
        )
        assertEquals("Kafe (%70) + 40Hz Gama (%30)", mikser)
    }

    @Test
    fun `carpisma denetimi siradaki vakit yakinsa uyari ve guvenli dk dondurur`() {
        val c = OdakMotoru.carpismaDenetimi(istenenDk = 45, siradakiVakitDk = 25)
        assertTrue(c.carpismaVar)
        assertEquals(22, c.guvenliDk)
        assertTrue("⚠️" in c.uyariMetni)
    }

    @Test
    fun `carpisma denetimi vakit uzaksan risksiz dondurur`() {
        val c = OdakMotoru.carpismaDenetimi(istenenDk = 25, siradakiVakitDk = 90)
        assertFalse(c.carpismaVar)
        assertEquals(25, c.guvenliDk)
    }

    @Test
    fun `proje butcesi ekle seans dakikasini mevcut bütceye ekler`() {
        assertEquals(165, OdakMotoru.projeButcesiEkle(140, 25))
    }

    @Test
    fun `masaya donus geri sayim mola bitiminden sonra kalan saniyeyi dondurur`() {
        val kalan = OdakMotoru.masayaDonusGeriSayim(
            molaBitimMs = 10_000L,
            simdiMs = 15_000L,
            beklemeSn = 15
        )
        assertEquals(10, kalan)
    }

    @Test
    fun `kilit paneli metni tasma ve normal durumda dogru dize dondurur`() {
        val normal = OdakMotoru.kilitPaneliMetni(25 * 60_000L, 0L, "Fizik")
        assertEquals("⚡ ODAK: 25:00 · Fizik", normal)
        val tasma = OdakMotoru.kilitPaneliMetni(0L, 80_000L, null)
        assertEquals("⚡ TAŞMA: +01:20", tasma)
    }
}
