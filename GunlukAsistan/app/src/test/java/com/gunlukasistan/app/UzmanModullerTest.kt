package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.57 — Faz 2: C, D, E, G, H, I ve J Uzman Modülleri ([UzmanModuller])
 * saf birim testleri (20 test).
 */
class UzmanModullerTest {

    // ── MODÜL C FAZ 2 TESTLERİ ──
    @Test
    fun `biyo vakit tavsiyesi sabah saatinde analitik gorev dondurur`() {
        val tavsiye = UzmanModuller.UzmanC_BiyoVakit.biyoVakitTavsiyesi(9)
        assertTrue("Sabah Odaklanması" in tavsiye)
        assertTrue("Analitik" in tavsiye)
    }

    @Test
    fun `seri kurtarma gerekli mi 23 30 ve 0 odak olunca true dondurur`() {
        assertTrue(UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaGerekliMi(23, 35, 0))
        assertFalse(UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaGerekliMi(22, 10, 0))
        assertFalse(UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaGerekliMi(23, 40, 50))
    }

    @Test
    fun `seri kurtarma mesaji gerekli durumda uyari verir`() {
        val mesaj = UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaMesaji(true)
        assertTrue("ACİL SERİ KURTARMA" in mesaj)
    }

    // ── MODÜL D FAZ 2 TESTLERİ ──
    @Test
    fun `nadirlik listesi 5 nadir rozet dondurur ve formatlar`() {
        val list = UzmanModuller.UzmanD_RozetVitrini.nadirlikListesi()
        assertEquals(5, list.size)
        val ozet = UzmanModuller.UzmanD_RozetVitrini.nadirlikVitriniOzeti(list)
        assertTrue("İlk Adım" in ozet)
        assertTrue("%92" in ozet)
    }

    @Test
    fun `sosyal paylasim metni rutbe odak ve kupa formatlar`() {
        val metin = UzmanModuller.UzmanD_RozetVitrini.sosyalPaylasimMetni("👑 Efsane", 150, true)
        assertTrue("Efsane" in metin)
        assertTrue("150 Dk" in metin)
        assertTrue("ALTIN KUPA" in metin)
    }

    // ── MODÜL E FAZ 2 TESTLERİ ──
    @Test
    fun `fade ozeti getirme saniyeleri ve autopause formatlar`() {
        val ayar = UzmanModuller.FadeAyari(fadeInSaniye = 3, fadeOutSaniye = 8, autoPauseAcik = true)
        val ozet = UzmanModuller.UzmanE_FadeVeAutoPause.fadeOzetiGetir(ayar)
        assertTrue("Fade-In: 3s" in ozet)
        assertTrue("Fade-Out: 8s" in ozet)
    }

    @Test
    fun `kulaklik cikti durumu autopause acikken duraklatildi dondurur`() {
        val uyari = UzmanModuller.UzmanE_FadeVeAutoPause.kulaklikCiktiDurumu(true)
        assertTrue("DURAKLATILDI" in uyari)
    }

    // ── MODÜL G FAZ 2 TESTLERİ ──
    @Test
    fun `pomodoro ekleme ardik sayiyi artirir ve yorgunluk ekler`() {
        val baslangic = UzmanModuller.YorgunlukEndeksi(ardikPomodoroSayisi = 2, zihinselYorgunlukYuzde = 50)
        val yeni = UzmanModuller.UzmanG_YorgunlukVeCikti.pomodoroEkle(baslangic)
        assertEquals(3, yeni.ardikPomodoroSayisi)
        assertEquals(75, yeni.zihinselYorgunlukYuzde)
    }

    @Test
    fun `yorgunluk radari uyari 75 yuzdede mola uyarisi verir`() {
        val endeks = UzmanModuller.YorgunlukEndeksi(zihinselYorgunlukYuzde = 80)
        assertTrue("YORGUNLUK RADARI" in UzmanModuller.UzmanG_YorgunlukVeCikti.yorgunlukRadariUyari(endeks))
    }

    @Test
    fun `cikti hasadi metni gorev adini ve notu formatlar`() {
        val metin = UzmanModuller.UzmanG_YorgunlukVeCikti.ciktiHasadiMetni("KPSS Tarih", "20 soru çözüldü")
        assertTrue("[25m HASAT]" in metin)
        assertTrue("KPSS Tarih" in metin)
        assertTrue("20 soru çözüldü" in metin)
    }

    // ── MODÜL H FAZ 2 TESTLERİ ──
    @Test
    fun `ayna kart metni hex ve kose bilgisini gosterir`() {
        val durum = UzmanModuller.AynaDurumu(hexRenk = "#22C55E", koseDp = 24, fontAd = "Lora")
        val metin = UzmanModuller.UzmanH_AynaVeYuzenSerit.aynaKartMetni(durum)
        assertTrue("#22C55E" in metin)
        assertTrue("24dp" in metin)
        assertTrue("Lora" in metin)
    }

    @Test
    fun `yuzen serit metni kalan dakika ve rutbeyi gosterir`() {
        val metin = UzmanModuller.UzmanH_AynaVeYuzenSerit.yuzenSeritMetni(18, "40Hz Gamma", "👑 Efsane")
        assertTrue("18m Kalan" in metin)
        assertTrue("40Hz Gamma" in metin)
        assertTrue("Efsane" in metin)
    }

    // ── MODÜL I FAZ 2 TESTLERİ ──
    @Test
    fun `sinav listesi 3 onemli sinavi dondurur`() {
        val list = UzmanModuller.UzmanI_PdfVeSinav.sinavListesi()
        assertEquals(3, list.size)
        assertTrue(list.any { it.sinavAd.contains("KPSS") })
    }

    @Test
    fun `sinav ozet metni 45 gun altinda yaklasti uyarisi verir`() {
        val sinav = UzmanModuller.SinavKaydi("KPSS Lisans", 40, "Hedef 90")
        val metin = UzmanModuller.UzmanI_PdfVeSinav.sinavOzetMetni(sinav)
        assertTrue("🚨 YAKLAŞTI" in metin)
    }

    @Test
    fun `pdf bolme hesaplama araligi dogru ayirir ve metin uretir`() {
        val metin = UzmanModuller.UzmanI_PdfVeSinav.pdfBolmeHesapla(toplamSayfa = 400, bolumBasla = 120, bolumBitir = 134)
        assertTrue("15 sayfalık çalışma paketi" in metin)
        assertTrue("Sayfa 120 - 134" in metin)
    }

    // ── MODÜL J FAZ 2 TESTLERİ ──
    @Test
    fun `alarm saglik raporu izin ve pil optimizasyon durumunu basar`() {
        val tani = UzmanModuller.AlarmTani(bildirimIzniVarMi = true, pilOptimizasyonKapatildiMi = true)
        val rapor = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.alarmSaglikRaporu(tani)
        assertTrue("AÇIK ✔" in rapor)
        assertTrue("KAPALI (Doğru) ✔" in rapor)
    }

    @Test
    fun `alarm saglik raporu riskli durumda uyari verir`() {
        val tani = UzmanModuller.AlarmTani(bildirimIzniVarMi = true, pilOptimizasyonKapatildiMi = false)
        val rapor = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.alarmSaglikRaporu(tani)
        assertTrue("AÇIK (Risk) ⚠️" in rapor)
    }

    @Test
    fun `anahtar kelime ara faturayi dogru bulur`() {
        val sonuc = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.anahtarKelimeAra("fatura")
        assertTrue("Modül 2 / Kategori B" in sonuc)
    }

    @Test
    fun `anahtar kelime ara kpss yi dogru bulur`() {
        val sonuc = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.anahtarKelimeAra("kpss")
        assertTrue("Modül I / Kategori I" in sonuc)
    }

    @Test
    fun `anahtar kelime ara bilinmeyen sorguyu genel mesajla dondurur`() {
        val sonuc = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.anahtarKelimeAra("uzay gemisi")
        assertTrue("100 Öneri Katalogu içinde genel arama yapıldı" in sonuc)
    }
}
