package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.67 — Yaşam Sağlığı & Finans — Uzman Faz 3 ([YasamSaglikFinansFaz3]) saf birim testleri (26 test).
 */
class YasamSaglikFinansFaz3Test {

    // ── 1. DEPREM, CPR & SOS (1..5) ──
    @Test
    fun `deprem tahliye adimlari 4 adet kilit adim icerir`() {
        val list = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.depremTahliyeAdimlari()
        assertEquals(4, list.size)
        assertTrue(list[0].contains("Deprem Çantası"))
    }

    @Test
    fun `tahliye hazirlik durumu tamamlandiginda 100 hazir ve true dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.tahliyeHazirlikDurumu(4, 4)
        assertTrue(res.first)
        assertTrue(res.second.contains("%100") || res.second.contains("100%"))
    }

    @Test
    fun `tahliye hazirlik durumu eksik adimda false ve kalan sayiyi dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.tahliyeHazirlikDurumu(2, 4)
        assertFalse(res.first)
        assertTrue(res.second.contains("2 kritik"))
    }

    @Test
    fun `cpr ilk yardim rehberi kalp masaji heimlich ve yanik adimlarini listeler`() {
        val list = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.cprIlkYardimRehberi()
        assertEquals(3, list.size)
        assertTrue(list.any { it.contains("CPR") })
        assertTrue(list.any { it.contains("Heimlich") })
    }

    @Test
    fun `sos acil mesaji olustur konum kan grubu ve acil kisiyi mesaja dondurur`() {
        val msg = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.sosAcilMesajiOlustur("Ankara", "A Rh+", "Sefa")
        assertTrue(msg.contains("Ankara"))
        assertTrue(msg.contains("A Rh+"))
        assertTrue(msg.contains("Sefa"))
    }

    // ── 2. PUSULA, DÜŞÜK GÜÇ & GİZLİLİK (6..10) ──
    @Test
    fun `pusula kible rehberi gunes ve kible yon bilgisi icerir`() {
        val str = YasamSaglikFinansFaz3.Faz3_2_PusulaVeGuvenlik.pusulaKibleRehberi()
        assertTrue(str.contains("Kıble"))
        assertTrue(str.contains("Güneş"))
    }

    @Test
    fun `dusuk guc modu kontrolu 15 yuzde altinda true ve hayatta kalma dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_2_PusulaVeGuvenlik.dusukGucModuKontrolu(10)
        assertTrue(res.first)
        assertTrue(res.second.contains("Hayatta Kalma"))
    }

    @Test
    fun `dusuk guc modu kontrolu guvenli pil yuzdesinde false dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_2_PusulaVeGuvenlik.dusukGucModuKontrolu(50)
        assertFalse(res.first)
        assertTrue(res.second.contains("güvenli bölgede"))
    }

    @Test
    fun `gizlilik kalkan durumu aktifken engellendi mesaji dondurur`() {
        val str = YasamSaglikFinansFaz3.Faz3_2_PusulaVeGuvenlik.gizlilikKalkanDurumu(true)
        assertTrue(str.contains("AÇIK"))
        assertTrue(str.contains("engellendi"))
    }

    @Test
    fun `gizlilik kalkan durumu kapaliyken standart ekran mesaji dondurur`() {
        val str = YasamSaglikFinansFaz3.Faz3_2_PusulaVeGuvenlik.gizlilikKalkanDurumu(false)
        assertTrue(str.contains("KAPALI"))
    }

    // ── 3. ACİL TIBBİ KART (11..12) ──
    @Test
    fun `varsayilan tibbi kart ahmet sefa ve alerjileri icerir`() {
        val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
        assertEquals("Ahmet Sefa", kart.adSoyad)
        assertEquals("A Rh+", kart.kanGrubu)
    }

    @Test
    fun `tibbi kart metni olusturma yuksek kontrastli ascii cerceve dondurur`() {
        val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
        val str = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.tibbiKartMetniOlustur(kart)
        assertTrue(str.contains("ACİL TIBBİ KART"))
        assertTrue(str.contains("Ahmet Sefa"))
    }

    // ── 4. DEPOLAMA ANALİZÖRÜ & ÇÖKME TANISI (13..15) ──
    @Test
    fun `varsayilan depolama listesi en az 3 kategori listeler`() {
        val list = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.varsayilanDepolama()
        assertTrue(list.size >= 3)
    }

    @Test
    fun `depolama ozetleme mb olarak dogru toplar ve formatlar`() {
        val res = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.depolamaOzetle(
            YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.varsayilanDepolama()
        )
        assertEquals(6.5, res.first, 0.01)
        assertTrue(res.second.contains("6.5 MB"))
    }

    @Test
    fun `son cokme tanisi getir 0 crash bilgisi icerir`() {
        val str = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.sonCokmeTanisiGetir()
        assertTrue(str.contains("0 Crash"))
    }

    // ── 5. ARAMA İNDEKSİ & BİLDİRİM DENETİMİ (16..20) ──
    @Test
    fun `varsayilan arama listesi en az 7 modul karti barindirir`() {
        val list = YasamSaglikFinansFaz3.Faz3_5_AramaVeBildirim.varsayilanAramaListesi()
        assertTrue(list.size >= 7)
    }

    @Test
    fun `kelimeye gore ara deprem yazinca deprem modulu bulur`() {
        val res = YasamSaglikFinansFaz3.Faz3_5_AramaVeBildirim.kelimeyeGoreAra("deprem")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.kelime == "DEPREM" })
    }

    @Test
    fun `kelimeye gore ara sos yazinca sos modulu dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_5_AramaVeBildirim.kelimeyeGoreAra("sos")
        assertTrue(res.any { it.kelime == "SOS" })
    }

    @Test
    fun `kelimeye gore ara bos sorguda bos liste dondurur`() {
        val res = YasamSaglikFinansFaz3.Faz3_5_AramaVeBildirim.kelimeyeGoreAra("")
        assertTrue(res.isEmpty())
    }

    @Test
    fun `bildirim saglik raporu doze muafiyeti bilgisini dondurur`() {
        val str = YasamSaglikFinansFaz3.Faz3_5_AramaVeBildirim.bildirimSaglikRaporu()
        assertTrue(str.contains("Doze"))
    }

    // ── 6. BÜTÜNCÜL JSON EXPORT (21..23) ──
    @Test
    fun `json paketi olusturma tibbi kart bilgisini json string dondurur`() {
        val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
        val json = YasamSaglikFinansFaz3.Faz3_6_ButunculExport.jsonPaketiOlustur(kart)
        assertTrue(json.contains("Ahmet Sefa"))
        assertTrue(json.contains("YasamSaglikFinansFaz3"))
    }

    @Test
    fun `json paketi gecerliligini dogru json icin true dondurur`() {
        val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
        val json = YasamSaglikFinansFaz3.Faz3_6_ButunculExport.jsonPaketiOlustur(kart)
        assertTrue(YasamSaglikFinansFaz3.Faz3_6_ButunculExport.jsonPaketiGecerliMi(json))
    }

    @Test
    fun `json paketi gecerliligini hatali json icin false dondurur`() {
        assertFalse(YasamSaglikFinansFaz3.Faz3_6_ButunculExport.jsonPaketiGecerliMi("{\"hata\":true}"))
    }

    // ── 7. CANLI DURUM & YÜZEBİLEN HAP (24..26) ──
    @Test
    fun `kilit ekrani mesaji aktif oructa kalan sureyi dondurur`() {
        val msg = YasamSaglikFinansFaz3.Faz3_7_CanliDurum.kilitEkraniMesajiGetir(true)
        assertTrue(msg.contains("Aralıklı Oruç"))
    }

    @Test
    fun `kilit ekrani mesaji pasifte aktif sayac yok dondurur`() {
        val msg = YasamSaglikFinansFaz3.Faz3_7_CanliDurum.kilitEkraniMesajiGetir(false)
        assertTrue(msg.contains("Aktif sayaç bulunmuyor"))
    }

    @Test
    fun `yuzebilen durum seridi metni frekans hz degerini dondurur`() {
        val msg = YasamSaglikFinansFaz3.Faz3_7_CanliDurum.yuzebilenDurumSeridiMetni(40)
        assertTrue(msg.contains("40 Hz"))
    }
}
