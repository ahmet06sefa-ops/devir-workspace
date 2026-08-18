package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.11 · ULTRA-30 Grup A — Görünüm Devrimi saf mantık testleri.
 *
 * Kapsam: A1 güneş kararı · A2 profil JSON/doğrulama · A3 gün ışığı
 * yüzdesi · A5 gardırop kilit tablosu. Context'siz hesaplar;
 * prefs katmanı test dışıdır (android.jar taslağı birim testte çöker).
 */
class GrupATest {

    // ── A1: koyuMuDakika ─────────────────────────────────────────

    @Test
    fun a1_normalAralikGunDuz() {
        // 07:00–19:00 penceresinde aydınlık
        assertFalse(ThemeManager.koyuMuDakika(420, 1140, 720))  // öğlen
        assertTrue(ThemeManager.koyuMuDakika(420, 1140, 360))   // 06:00
        assertTrue(ThemeManager.koyuMuDakika(420, 1140, 1380))  // 23:00
    }

    @Test
    fun a1_sinirDegerleri() {
        // Aydınlanma anı AÇIK, kararma anı KOYU
        assertFalse(ThemeManager.koyuMuDakika(420, 1140, 420))
        assertTrue(ThemeManager.koyuMuDakika(420, 1140, 1140))
        // Gece yarısı
        assertTrue(ThemeManager.koyuMuDakika(420, 1140, 0))
        assertTrue(ThemeManager.koyuMuDakika(420, 1140, 1439))
    }

    @Test
    fun a1_bosVeTersAralik() {
        // Boş aralık: hep açık (işaret kararması olmaz)
        assertFalse(ThemeManager.koyuMuDakika(600, 600, 0))
        assertFalse(ThemeManager.koyuMuDakika(600, 600, 600))
        // Ters yazılmış aralık takas edilir — "23'te karar 06'da açıl"
        assertTrue(ThemeManager.koyuMuDakika(1380, 360, 1420))  // 23:40 koyu
        assertTrue(ThemeManager.koyuMuDakika(1380, 360, 300))   // 05:00 koyu
        assertFalse(ThemeManager.koyuMuDakika(1380, 360, 720))  // öğlen açık
    }

    // ── A2: BaglamProfili JSON + doğrulama ───────────────────────

    private fun profil(
        id: Long = 1L,
        ad: String = "Deneme",
        gece: Int = 1,
        hedef: Int = BaglamProfili.DEGISMEZ
    ) = BaglamProfili.Profil(
        id = id, ad = ad, emoji = "🎭",
        tema = 2, vurgu = 4, gece = gece,
        yogunluk = 1, yazi = 1, dinamik = false,
        sessizBas = 22, sessizBit = 7,
        hedefDk = hedef, sabahDk = 420, aksamDk = 1380
    )

    @Test
    fun a2_jsonTamTur() {
        val orijinal = profil()
        val geri = BaglamProfili.Profil.jsondan(orijinal.json())
        assertEquals(orijinal, geri)
    }

    @Test
    fun a2_jsondanBozukNull() {
        assertNull(BaglamProfili.Profil.jsondan(org.json.JSONObject()))
    }

    @Test
    fun a2_sablonlarGecerliVeTekil() {
        val s = BaglamProfili.sablonlar()
        assertEquals(3, s.size)
        assertTrue(s.all { BaglamProfili.dogrulanmis(it) })
        // Kimlikler benzersiz ve negatif (şablon imzası)
        assertEquals(3, s.map { it.id }.toSet().size)
        assertTrue(s.all { it.id < 0 })
    }

    @Test
    fun a2_dogrulanmisSinirlar() {
        assertTrue(BaglamProfili.dogrulanmis(profil()))
        // DEGISMEZ işaretleri geçerli
        assertTrue(BaglamProfili.dogrulanmis(profil(hedef = BaglamProfili.DEGISMEZ)))
        // Sınır ihlalleri reddedilir
        assertFalse(BaglamProfili.dogrulanmis(profil(gece = 4)))
        assertFalse(BaglamProfili.dogrulanmis(profil().copy(yazi = 4)))
        assertFalse(BaglamProfili.dogrulanmis(profil().copy(sessizBas = 24)))
        assertFalse(BaglamProfili.dogrulanmis(profil().copy(hedefDk = 601)))
        assertFalse(BaglamProfili.dogrulanmis(profil(ad = "")))
        assertFalse(BaglamProfili.dogrulanmis(profil(ad = "x".repeat(21))))
    }

    // ── A3: GunIsigiView.yuzde / kararmayaKalanDk ────────────────

    @Test
    fun a3_gunduzYuzdesi() {
        // 06:20 doğuş, 19:10 batış; 12:45 tam ortaya yakın
        val y = GunIsigiView.yuzde(380, 1150, 765)
        assertEquals(50, y)
        // Doğuş anı sıfır
        assertEquals(0, GunIsigiView.yuzde(380, 1150, 380))
    }

    @Test
    fun a3_geceEksidir() {
        // Gün doğmadı da gece sayılır ("kararmaya 14 saat" saçmalığı yok)
        assertEquals(-1, GunIsigiView.yuzde(380, 1150, 120))
        assertEquals(-1, GunIsigiView.yuzde(380, 1150, 379))
        assertEquals(-1, GunIsigiView.yuzde(380, 1150, 1150)) // batış sonrası
        assertEquals(-1, GunIsigiView.yuzde(-1, 1150, 720))    // veri yok
    }

    @Test
    fun a3_kalanSure() {
        assertEquals(385, GunIsigiView.kararmayaKalanDk(380, 1150, 765))
        assertEquals(-1, GunIsigiView.kararmayaKalanDk(380, 1150, 100))
    }

    // ── A5: MaskotGardrop kilit tablosu ──────────────────────────

    private fun giris(
        seri: Int = 0, toplam: Int = 0, uyku: Int = 0, rekor: Int = 0
    ) = MaskotGardrop.Giris(seri, toplam, uyku, rekor)

    @Test
    fun a5_esiklerinKenari() {
        // Kenar tam üstü = açık, bir altı = kapalı
        assertTrue(MaskotGardrop.acikMi(MaskotGardrop.BERE, giris(seri = 7)))
        assertFalse(MaskotGardrop.acikMi(MaskotGardrop.BERE, giris(seri = 6)))
        assertTrue(MaskotGardrop.acikMi(MaskotGardrop.GOZLUK, giris(toplam = 250)))
        assertFalse(MaskotGardrop.acikMi(MaskotGardrop.GOZLUK, giris(toplam = 249)))
        assertTrue(MaskotGardrop.acikMi(MaskotGardrop.ESARP, giris(uyku = 3)))
        assertFalse(MaskotGardrop.acikMi(MaskotGardrop.ESARP, giris(uyku = 2)))
        assertTrue(MaskotGardrop.acikMi(MaskotGardrop.TAC, giris(rekor = 30)))
        assertFalse(MaskotGardrop.acikMi(MaskotGardrop.TAC, giris(rekor = 29)))
    }

    @Test
    fun a5_bilinmeyenAnahtarKapali() {
        assertFalse(MaskotGardrop.acikMi("pelerin", giris(seri = 999, rekor = 999)))
        // Durumlar haritası dört anahtarın tamamını verir
        assertEquals(4, MaskotGardrop.durumlar(giris()).size)
    }
}
