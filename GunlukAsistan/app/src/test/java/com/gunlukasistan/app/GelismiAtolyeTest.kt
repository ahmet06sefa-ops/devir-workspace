package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.56 — C, D, E, G, H, I ve J Gelişmiş Hayat Atölyesi ([GelismiAtolye])
 * saf birim testleri (20 test).
 */
class GelismiAtolyeTest {

    // ── MODÜL C TESTLERİ ──
    @Test
    fun `modul c ai prompt hazirlama tum parametreleri icerir`() {
        val talimat = GelismiAtolye.AiKocTalimati(ozelTalimat = "Her gün felsefe yap")
        val prompt = GelismiAtolye.ModulC_OtonomAi.promptHazirla(talimat)
        assertTrue("Her gün felsefe yap" in prompt)
        assertTrue("Şefkatli Zen Rehberi" in prompt)
    }

    @Test
    fun `modul c notlardan gorev cikar eylem kelimesinde baslik dondurur`() {
        val not = "Yarın sabah saat 10da matematik ödevini çöz"
        val gorev = GelismiAtolye.ModulC_OtonomAi.notlardanGorevCikar(not)
        assertNotNull(gorev)
        assertTrue(gorev!!.contains("matematik"))
    }

    @Test
    fun `modul c notlardan gorev cikar eylemsiz notta null dondurur`() {
        val not = "Hava çok güzeldi ve kuşlar ötüyordu"
        val gorev = GelismiAtolye.ModulC_OtonomAi.notlardanGorevCikar(not)
        assertNull(gorev)
    }

    // ── MODÜL D TESTLERİ ──
    @Test
    fun `modul d rutbe getirme xp ye gore dogru secer`() {
        assertEquals("👑 Efsane", GelismiAtolye.ModulD_Oyunlastirma.rutbeGetir(350))
        assertEquals("⭐ Usta", GelismiAtolye.ModulD_Oyunlastirma.rutbeGetir(150))
        assertEquals("🌱 Çırak", GelismiAtolye.ModulD_Oyunlastirma.rutbeGetir(50))
    }

    @Test
    fun `modul d gorev tamamlama combo acikken 15 xp ekler`() {
        val durum = GelismiAtolye.XpDurumu(xp = 100)
        val yeni = GelismiAtolye.ModulD_Oyunlastirma.gorevTamamla(durum, comboMi = true)
        assertEquals(115, yeni.xp)
        assertEquals(1.5f, yeni.comboCarpan)
    }

    @Test
    fun `modul d hafta sonu odak ekleme 120 dakikada kupa verir`() {
        var durum = GelismiAtolye.XpDurumu(haftaSonuOdakDk = 90, kupaKazandiMi = false)
        durum = GelismiAtolye.ModulD_Oyunlastirma.haftaSonuOdakEkle(durum, 35)
        assertEquals(125, durum.haftaSonuOdakDk)
        assertTrue(durum.kupaKazandiMi)
    }

    @Test
    fun `modul d surpriz bilgi dondurme gecerli indeks dondurur`() {
        val bilgi = GelismiAtolye.ModulD_Oyunlastirma.surprizBilgiGetir(2)
        assertTrue("40Hz Gamma" in bilgi)
    }

    // ── MODÜL E TESTLERİ ──
    @Test
    fun `modul e ses ozeti getirme gamma acikken dogru basar`() {
        val ayar = GelismiAtolye.FrekansAyar(gamma40HzAcik = true, alfa10HzAcik = false)
        val ozet = GelismiAtolye.ModulE_SesVeFrekans.sesOzetiGetir(ayar)
        assertTrue("40Hz Gamma" in ozet)
        assertTrue("Yumuşak Gong" in ozet)
    }

    @Test
    fun `modul e frekans toggle gamma durumunu cevirir`() {
        val ayar = GelismiAtolye.FrekansAyar(gamma40HzAcik = true)
        val yeni = GelismiAtolye.ModulE_SesVeFrekans.frekansToggle(ayar, gammaMi = true)
        assertFalse(yeni.gamma40HzAcik)
    }

    // ── MODÜL G TESTLERİ ──
    @Test
    fun `modul g hazir sprint secme dogru dakikalari dondurur`() {
        val sprint1 = GelismiAtolye.ModulG_SayacVePip.hazirSprintSec(1)
        assertEquals(50, sprint1.calismaDk)
        assertEquals(10, sprint1.molaDk)
        val sprint2 = GelismiAtolye.ModulG_SayacVePip.hazirSprintSec(2)
        assertEquals(30, sprint2.calismaDk)
    }

    @Test
    fun `modul g tasma suresi ekleme ve durum metni formatlar`() {
        var ayar = GelismiAtolye.SprintAyar(tasmaDk = 5)
        ayar = GelismiAtolye.ModulG_SayacVePip.tasmaSuresiEkle(ayar, 10)
        assertEquals(15, ayar.tasmaDk)
        assertTrue("+15 dk" in GelismiAtolye.ModulG_SayacVePip.sayacDurumMetni(ayar))
    }

    // ── MODÜL H TESTLERİ ──
    @Test
    fun `modul h sablon metni getirme bilgileri gosterir`() {
        val tercih = GelismiAtolye.TasarimTercihi(
            sablon = GelismiAtolye.TasarimSablonu.ULTRA_KESKIN_0DP,
            font = GelismiAtolye.FontTipi.LORA,
            akordiyonDaralma = false
        )
        val metin = GelismiAtolye.ModulH_TasarimVeAkordiyon.sablonMetniGetir(tercih)
        assertTrue("Ultra Keskin (0dp)" in metin)
        assertTrue("Lora" in metin)
        assertTrue("Hep Açık" in metin)
    }

    // ── MODÜL I TESTLERİ ──
    @Test
    fun `modul i soru ekleme hedefe gore yuzde hesaplar`() {
        var durum = GelismiAtolye.FeynmanCalismasi(cozulenSoruSayisi = 20, gunlukSoruHedefi = 100)
        durum = GelismiAtolye.ModulI_DersVeKpss.soruEkle(durum, 30)
        assertEquals(50, durum.cozulenSoruSayisi)
        assertTrue("50/100 (%50)" in GelismiAtolye.ModulI_DersVeKpss.kpssDurumMetni(durum))
    }

    @Test
    fun `modul i feynman anlatim skoru kisa ve net cumlelerde 95 dondurur`() {
        val anlatim = "Kurtuluş Savaşı halkın birleşmesidir. Düzenli ordu kurulmuştur. Zafer kazanılmıştır."
        val skor = GelismiAtolye.ModulI_DersVeKpss.feynmanAnlatimSkoru(anlatim)
        assertEquals(95, skor)
    }

    @Test
    fun `modul i feynman anlatim skoru cok kisa anlatimda 30 dondurur`() {
        assertEquals(30, GelismiAtolye.ModulI_DersVeKpss.feynmanAnlatimSkoru("Çok kısa"))
    }

    // ── MODÜL J TESTLERİ ──
    @Test
    fun `modul j depolama analizi toplam mb dogru toplayip metin uretir`() {
        val analiz = GelismiAtolye.DepolamaAnaliz(notlarMb = 2.0f, pdflerMb = 10.0f, cacheMb = 5.0f)
        assertEquals(17.0f, GelismiAtolye.ModulJ_SistemVeAnalitik.toplamKullanimMb(analiz), 0.01f)
        val metin = GelismiAtolye.ModulJ_SistemVeAnalitik.depolamaMetniGetir(analiz)
        assertTrue("17.0 MB" in metin)
    }

    @Test
    fun `modul j onbellek temizleme cache miktarini sifirlar`() {
        val analiz = GelismiAtolye.DepolamaAnaliz(cacheMb = 8.5f)
        val temiz = GelismiAtolye.ModulJ_SistemVeAnalitik.onbellekTemizle(analiz)
        assertEquals(0.0f, temiz.cacheMb, 0.01f)
    }

    @Test
    fun `modul j cdej master json uretme ve cozumleme gecerli dondurur`() {
        val c = GelismiAtolye.AiKocTalimati()
        val d = GelismiAtolye.XpDurumu()
        val e = GelismiAtolye.FrekansAyar()
        val g = GelismiAtolye.SprintAyar()
        val h = GelismiAtolye.TasarimTercihi()
        val i = GelismiAtolye.FeynmanCalismasi()
        val j = GelismiAtolye.DepolamaAnaliz()

        val json = GelismiAtolye.ModulJ_SistemVeAnalitik.cdejMasterJsonUret(c, d, e, g, h, i, j)
        assertTrue(GelismiAtolye.ModulJ_SistemVeAnalitik.cdejMasterJsonCoz(json))
    }

    @Test
    fun `modul j cdej master json null objede false dondurur`() {
        assertFalse(GelismiAtolye.ModulJ_SistemVeAnalitik.cdejMasterJsonCoz(null))
    }
}
