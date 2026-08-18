package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.62 — 7 Uzman ÖSYM Haritası, Turlama Hızı, Kitap Ayracı, 130m Simülatör & Şifreli Kasa
 * ([DersUzmanFaz3]) saf birim testleri (25 test).
 */
class DersUzmanFaz3Test {

    // ── 1. ÖSYM HARİTASI & TURLAMA TEKNİĞİ (1..4) ──
    @Test
    fun `osym konu haritasi tarih dersinde 3 konu dondurur`() {
        val list = DersUzmanFaz3.Faz3_1_OsymHaritasi.konuHaritasiGetir("Tarih")
        assertEquals(3, list.size)
        assertTrue(list.any { it.baslik.contains("Osmanlı") })
    }

    @Test
    fun `osym konu haritasi matematik dersinde problem konularini dondurur`() {
        val list = DersUzmanFaz3.Faz3_1_OsymHaritasi.konuHaritasiGetir("Matematik")
        assertTrue(list.any { it.baslik.contains("Problem") })
    }

    @Test
    fun `osym konu haritasi turkce dersinde paragraf konusunu dondurur`() {
        val list = DersUzmanFaz3.Faz3_1_OsymHaritasi.konuHaritasiGetir("Türkçe")
        assertTrue(list[0].baslik.contains("Paragraf"))
    }

    @Test
    fun `turlama simulasyonu 120 soruda 45 saniyeyi formatlar`() {
        val metin = DersUzmanFaz3.Faz3_1_OsymHaritasi.turlamaSimulasyonu(120, 45)
        assertTrue("TURLAMA TEKNİĞİ" in metin)
        assertTrue("45s" in metin)
    }

    // ── 2. DERS HAPLARI & AKILLI ÖNERİ (5..6) ──
    @Test
    fun `gunun akilli onerisi calisilmeyen dersi bulup onerir`() {
        val oneri = DersUzmanFaz3.Faz3_2_DersHaplari.gununAkilliOnerisi(listOf("Matematik Problemler"))
        assertTrue("BUGÜN NE ÇALIŞSAM?" in oneri)
        assertTrue("Türkçe Paragraf" in oneri)
    }

    @Test
    fun `gunun akilli onerisi tum dersler bitmisse genel deneme onerir`() {
        val oneri = DersUzmanFaz3.Faz3_2_DersHaplari.gununAkilliOnerisi(
            listOf("Türkçe Paragraf", "KPSS Tarih: Osmanlı Dağılma", "Matematik Problemler")
        )
        assertTrue("Genel Deneme Çözümü" in oneri)
    }

    // ── 3. OKUMA HIZI RADARI & AYRAÇ (7..10) ──
    @Test
    fun `okuma hizi hesaplama 30 sayfayi 60 dakikada 30 sayfa saat bulur`() {
        val metin = DersUzmanFaz3.Faz3_3_OkumaHizi.okumaHiziHesapla(30, 60)
        assertTrue("30 Sayfa/Saat" in metin)
        assertTrue("Mükemmel Akademik Okuma Hızı" in metin)
    }

    @Test
    fun `okuma hizi hesaplama 10 sayfayi 40 dakikada 15 sayfa saat bulur`() {
        val metin = DersUzmanFaz3.Faz3_3_OkumaHizi.okumaHiziHesapla(10, 40)
        assertTrue("15 Sayfa/Saat" in metin)
        assertTrue("Dikkatli Konu Çözüm Hızı" in metin)
    }

    @Test
    fun `okuma hizi hesaplama sifir surede uyari dondurur`() {
        assertEquals("Süre 0 olamaz.", DersUzmanFaz3.Faz3_3_OkumaHizi.okumaHiziHesapla(10, 0))
    }

    @Test
    fun `ayrac metni getirme ders adini ve sayfa numarasini formatlar`() {
        val ayrac = DersUzmanFaz3.KitapAyraci("Tarih", 180, "Soru Kitabı")
        val metin = DersUzmanFaz3.Faz3_3_OkumaHizi.ayracMetniGetir(ayrac)
        assertTrue("Tarih" in metin)
        assertTrue("180" in metin)
        assertTrue("Soru Kitabı" in metin)
    }

    // ── 4. MİKRO MOLA & 130m SİMÜLATÖR (11..12) ──
    @Test
    fun `goz boyun rehberi 20 20 20 kuralini aciklar`() {
        val rehber = DersUzmanFaz3.Faz3_4_MikroMola.gozBoyunRehberi()
        assertTrue("20-20-20 GÖZ" in rehber)
    }

    @Test
    fun `sinav simulatoru metni sureyi ve dnd modunu formatlar`() {
        val sim = DersUzmanFaz3.Faz3_4_MikroMola.sinavSimulatoruMetni(130)
        assertTrue("130m KESİNTİSİZ SINAV SİMÜLATÖRÜ" in sim)
        assertTrue("DND" in sim)
    }

    // ── 5. REM UYKU HESAPLAYICI & SABBATH (13..15) ──
    @Test
    fun `ideal yatis saati sabah 7 00 ve 5 dongu icin 23 15 bulur`() {
        // 5 döngü = 450 dk + 15 dk = 465 dk = 7 saat 45 dk. 07:00 - 7h45m = 23:15
        assertEquals("23:15", DersUzmanFaz3.Faz3_5_RemVeSabbath.idealYatisSaati(7, 0, 5))
    }

    @Test
    fun `ideal yatis saati sabah 6 00 ve 6 dongu icin 20 45 bulur`() {
        // 6 döngü = 540 dk + 15 = 555 dk = 9 saat 15 dk. 06:00 - 9h15m = 20:45
        assertEquals("20:45", DersUzmanFaz3.Faz3_5_RemVeSabbath.idealYatisSaati(6, 0, 6))
    }

    @Test
    fun `sabbath metni getirme pazar gununu suclusuz dinlenme olarak basar`() {
        val sabbath = DersUzmanFaz3.Faz3_5_RemVeSabbath.sabbathMetniGetir("Pazar")
        assertTrue("Pazar" in sabbath)
        assertTrue("suçluluk duymadan" in sabbath)
    }

    // ── 6. ŞİFRELİ KASA TESTLERİ (16..18) ──
    @Test
    fun `not kilit toggle kilitli notu acik duruma getirir`() {
        val not = DersUzmanFaz3.SifreliNot(kilitliMi = true)
        val acik = DersUzmanFaz3.Faz3_6_SifreliKasa.notKilitToggle(not)
        assertFalse(acik.kilitliMi)
    }

    @Test
    fun `not metni getirme kilitli notta gizli icerigi saklar`() {
        val not = DersUzmanFaz3.SifreliNot(gizliIcerik = "GİZLİ BİLGİ", kilitliMi = true)
        val metin = DersUzmanFaz3.Faz3_6_SifreliKasa.notMetniGetir(not)
        assertTrue("[KİLİTLİ]" in metin)
        assertTrue("****" in metin)
        assertFalse("GİZLİ BİLGİ" in metin)
    }

    @Test
    fun `not metni getirme acik notta gizli icerigi gosterir`() {
        val not = DersUzmanFaz3.SifreliNot(gizliIcerik = "GİZLİ BİLGİ", kilitliMi = false)
        val metin = DersUzmanFaz3.Faz3_6_SifreliKasa.notMetniGetir(not)
        assertTrue("[AÇIK]" in metin)
        assertTrue("GİZLİ BİLGİ" in metin)
    }

    // ── 7. GENİŞLETİLMİŞ ARAMA TESTLERİ (19..25) ──
    @Test
    fun `genisletilmis ara osym kelimesini bulur`() {
        assertTrue("ÖSYM Soru Sıklık" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("osym"))
    }

    @Test
    fun `genisletilmis ara turlama kelimesini bulur`() {
        assertTrue("Turlama" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("turlama"))
    }

    @Test
    fun `genisletilmis ara ayrac kelimesini bulur`() {
        assertTrue("Okuma Hızı" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("ayrac"))
    }

    @Test
    fun `genisletilmis ara sabbath kelimesini bulur`() {
        assertTrue("Sabbath" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("sabbath"))
    }

    @Test
    fun `genisletilmis ara sifre kelimesini bulur`() {
        assertTrue("Şifreli Soru" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("sifre"))
    }

    @Test
    fun `genisletilmis ara leitner kelimesini bulur`() {
        assertTrue("Leitner" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("leitner"))
    }

    @Test
    fun `genisletilmis ara bilinmeyen kelimede genel arama basar`() {
        assertTrue("Dev Katalog" in DersUzmanFaz3.Faz3_7_GenisletilmisArama.genisletilmisAra("uzay"))
    }
}
