package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Gemini'den farkı kapatma paketinin saf testleri:
 *  · [EkranGoruntusuMotoru] — ekran görüntüsü → görsel karar
 *  · [KullaniciHafizasi] — kalıcı kullanıcı hafızası profili
 *  · [AjanModu] — çok adımlı hedef planlama
 *  · [KonusmaKesmeMotoru] — "dur/kes" dedektörü
 */
class GeminiFarkiTest {

    // ── EkranGoruntusuMotoru ──

    @Test
    fun `gorsel istem ekrandaki ogeleri ve amaci icerir`() {
        val istem = EkranGoruntusuMotoru.gorselIstemiKur(listOf("Görevler", "Ayarlar"), "görevleri aç")
        assertTrue(istem.contains("Görevler"))
        assertTrue(istem.contains("Ayarlar"))
        assertTrue(istem.contains("görevleri aç"))
        assertTrue(istem.contains("tikla|"))
    }

    @Test
    fun `karari ayristir tikla emri dogru cozer`() {
        val k1 = EkranGoruntusuMotoru.karariAyristir("tikla|Görevler")
        assertTrue(k1.ok)
        assertEquals("Görevler", k1.emir)
        val k2 = EkranGoruntusuMotoru.karariAyristir("TIKLA: Ayarlar")
        assertTrue(k2.ok)
        assertEquals("Ayarlar", k2.emir)
    }

    @Test
    fun `karari ayristir yok ise ok false doner`() {
        val k = EkranGoruntusuMotoru.karariAyristir("tikla|YOK")
        assertFalse(k.ok)
    }

    @Test
    fun `karari ayristir bos cevap ok false doner`() {
        assertFalse(EkranGoruntusuMotoru.karariAyristir("").ok)
        assertFalse(EkranGoruntusuMotoru.karariAyristir("   ").ok)
    }

    @Test
    fun `on ekran kisa etiketleri onceler ve sınırlar`() {
        val etiketler = listOf("Uzun açıklama satırı", "Aç", "Kaydet", "Sil", "Görevler", "", "  ")
        val on = EkranGoruntusuMotoru.onEkran(etiketler)
        assertTrue(on.size <= 12)
        assertFalse(on.contains(""))
        assertTrue(on.contains("Aç"))
    }

    // ── KullaniciHafizasi ──

    @Test
    fun `profil metni odak ve seriyi icerir`() {
        val p = KullaniciHafizasi.profilMetni(120, 120, 5, 9, 40)
        assertTrue(p.contains("120 dk"))
        assertTrue(p.contains("ulaştı"))
        assertTrue(p.contains("5 günlük"))
        assertTrue(p.contains("40 soru"))
    }

    @Test
    fun `profil metni hedef altinda yuzde gosterir`() {
        val p = KullaniciHafizasi.profilMetni(60, 120, 0, 0, 10)
        assertTrue(p.contains("%50"))
    }

    @Test
    fun `haftalik ozet farkli seviyelerde farkli metin uretir`() {
        assertTrue(KullaniciHafizasi.haftalikOzet(1200).contains("yoğun"))
        assertTrue(KullaniciHafizasi.haftalikOzet(100).contains("sakin"))
    }

    @Test
    fun `hatirlatici anahtari tarih temelli kararlidir`() {
        assertEquals("2026-08-15", KullaniciHafizasi.hatirlaticiAnahtari(15, 8, 2026))
    }

    // ── AjanModu ──

    @Test
    fun `hedef dakika saat ifadesini cevirir`() {
        assertEquals(240, AjanModu.hedefDk("4 saat çalış"))
        assertEquals(180, AjanModu.hedefDk("3 saat"))
    }

    @Test
    fun `hedef dakika dk ifadesini cevirir`() {
        assertEquals(120, AjanModu.hedefDk("120 dk"))
        assertEquals(90, AjanModu.hedefDk("90 dakika"))
    }

    @Test
    fun `hedef dakika süre birimi yoksa 0 doner`() {
        assertEquals(0, AjanModu.hedefDk("çalışmaya başla"))
        assertEquals(0, AjanModu.hedefDk("30 sayfa oku"))   // çıplak sayı tetiklenmez
        assertEquals(0, AjanModu.hedefDk("5 tane görev var"))
    }

    @Test
    fun `ajan modu acikca istenirse tetiklenir`() {
        assertTrue(AjanModu.ajanModuGerekliMi("ajan modu başlat 2 saat çalış"))
        assertTrue(AjanModu.ajanModuGerekliMi("4 saat çalış"))
        assertTrue(AjanModu.ajanModuGerekliMi("90 dk ders çalış"))
    }

    @Test
    fun `ajan modu rastgele sayili sohbette tetiklenmez`() {
        assertFalse(AjanModu.ajanModuGerekliMi("30 sayfa oku"))
        assertFalse(AjanModu.ajanModuGerekliMi("nasılsın"))
        assertFalse(AjanModu.ajanModuGerekliMi("5 tane görev var"))
        assertFalse(AjanModu.ajanModuGerekliMi("kaçta çalışalım"))
    }

    @Test
    fun `ajan modu sure yoksa calisma fiili olsa bile tetiklenmez`() {
        assertFalse(AjanModu.ajanModuGerekliMi("çalışmaya başla"))   // süre yok
        assertFalse(AjanModu.ajanModuGerekliMi("ders çalış"))         // süre yok
    }

    @Test
    fun `plana cevir sureyi bloklara boler ve ozet ekler`() {
        val plan = AjanModu.planaCevir("100 dk çalış, 20 soru çöz")
        assertTrue(plan.adimlar.size >= 5)  // 4 blok (25x4) + soru + özet
        assertEquals("ozet_ver", plan.adimlar.last().ad)
        assertTrue(plan.adimlar.any { it.ad == "gorev_ekle" })
    }

    @Test
    fun `plana cevir sure sinirina takilir maksimum 6 blok`() {
        val plan = AjanModu.planaCevir("300 dk")
        // 6 blok + özet
        assertEquals(7, plan.adimlar.size)
    }

    // ── KonusmaKesmeMotoru ──

    @Test
    fun `kesme sozcukleri algilanir`() {
        assertTrue(KonusmaKesmeMotoru.kesmeMi("dur"))
        assertTrue(KonusmaKesmeMotoru.kesmeMi("kes"))
        assertTrue(KonusmaKesmeMotoru.kesmeMi("yeter"))
        assertTrue(KonusmaKesmeMotoru.kesmeMi("tamam dur"))
    }

    @Test
    fun `kesme olmayan ifadeler algilanmaz`() {
        assertFalse(KonusmaKesmeMotoru.kesmeMi("duruma göre plan yap"))
        assertFalse(KonusmaKesmeMotoru.kesmeMi("nasılsın"))
    }
}
