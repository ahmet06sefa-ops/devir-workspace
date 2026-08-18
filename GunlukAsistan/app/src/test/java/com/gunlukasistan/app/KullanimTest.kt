package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.8 — Kullanım analitiği testleri (öneri 50).
 *
 * Context'e bağlı olmayan kısımlar: `Satir` modeli, ekran adı
 * sabitlerinin tutarlılığı, eylem öneki mantığı.
 *
 * ── Ekran adı sabitleri neden test ediliyor ──
 * Bu adlar SharedPreferences'a anahtar olarak yazılıyor.
 * Birini değiştirmek eski sayacı yetim bırakır: kullanıcı
 * "Görevler" yerine iki ayrı satır ("Görevler" ve "Gorevler")
 * görür. Test bu tür kaymaları yakalıyor.
 */
class KullanimTest {

    // ══════════════════════════════════════════════════════════
    // Satır modeli
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ekran satiri eylem degil`() {
        val s = Kullanim.Satir("Görevler", 12, false)
        assertFalse(s.eylemMi)
        assertEquals("Görevler", s.gosterim)
    }

    @Test
    fun `eylem oneki gosterimde temizlenir`() {
        // Depoda "!Görev eklendi" olarak duruyor; kullanıcıya
        // ünlem işareti gösterilmemeli
        val s = Kullanim.Satir("!Görev eklendi", 5, true)
        assertTrue(s.eylemMi)
        assertEquals("Görev eklendi", s.gosterim)
    }

    @Test
    fun `eylem olmayan satirda onek kirpilmaz`() {
        val s = Kullanim.Satir("Notlar", 3, false)
        assertEquals("Notlar", s.gosterim)
    }

    @Test
    fun `sayi korunur`() {
        assertEquals(42, Kullanim.Satir("Ana ekran", 42, false).sayi)
    }

    // ══════════════════════════════════════════════════════════
    // Ekran adı sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ekran adlari benzersiz`() {
        // İki ekran aynı adı kullanırsa sayaçları birleşir ve
        // istatistik yanlış olur
        val hepsi = Kullanim.Ekran.HEPSI
        assertEquals(hepsi.size, hepsi.toSet().size)
    }

    @Test
    fun `ekran adlari bos degil`() {
        Kullanim.Ekran.HEPSI.forEach {
            assertTrue("Boş ekran adı bulundu", it.isNotBlank())
        }
    }

    @Test
    fun `ekran adlari unlem ile baslamaz`() {
        // Ünlem eylem öneki — ekran adı onunla başlarsa
        // eylem sanılır ve yanlış listeye düşer
        Kullanim.Ekran.HEPSI.forEach {
            assertFalse("Ekran adı ünlemle başlıyor: $it", it.startsWith("!"))
        }
    }

    @Test
    fun `temel ekranlar listede`() {
        val hepsi = Kullanim.Ekran.HEPSI
        assertTrue(Kullanim.Ekran.ANA in hepsi)
        assertTrue(Kullanim.Ekran.GOREVLER in hepsi)
        assertTrue(Kullanim.Ekran.AYARLAR in hepsi)
        assertTrue(Kullanim.Ekran.GUNLUK_HAYAT in hepsi)
        assertTrue(Kullanim.Ekran.ISTATISTIK in hepsi)
    }

    @Test
    fun `v97 ve v98 ekranlari eklendi`() {
        // Yeni sürümlerde eklenen ekranların listeye girmesi
        // unutulursa "hiç açılmayan" listesinde hiç görünmezler
        assertTrue(Kullanim.Ekran.GUNLUK_HAYAT in Kullanim.Ekran.HEPSI)
        assertTrue(Kullanim.Ekran.OGRENME in Kullanim.Ekran.HEPSI)
        assertTrue(Kullanim.Ekran.SORU_COZ in Kullanim.Ekran.HEPSI)
    }

    // ══════════════════════════════════════════════════════════
    // Eylem sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `eylem adlari benzersiz`() {
        val adlar = listOf(
            Kullanim.Eylem.GOREV_EKLE, Kullanim.Eylem.GOREV_TAMAM,
            Kullanim.Eylem.NOT_EKLE, Kullanim.Eylem.KONU_EKLE,
            Kullanim.Eylem.SAYAC_BASLA, Kullanim.Eylem.QUIZ_COZ,
            Kullanim.Eylem.KART_TEKRAR, Kullanim.Eylem.YEDEK_AL,
            Kullanim.Eylem.AI_ISTEK, Kullanim.Eylem.HARCAMA_EKLE,
            Kullanim.Eylem.ILAC_ALINDI, Kullanim.Eylem.HIZLI_KOMUT
        )
        assertEquals(adlar.size, adlar.toSet().size)
    }

    @Test
    fun `eylem adlari ekran adlariyla cakismiyor`() {
        // Çakışırsa ekran mı eylem mi belirsizleşir
        val eylemler = setOf(
            Kullanim.Eylem.GOREV_EKLE, Kullanim.Eylem.NOT_EKLE,
            Kullanim.Eylem.YEDEK_AL, Kullanim.Eylem.HARCAMA_EKLE
        )
        Kullanim.Ekran.HEPSI.forEach {
            assertFalse("Çakışma: $it", it in eylemler)
        }
    }

    @Test
    fun `eylem adlari bos degil`() {
        assertTrue(Kullanim.Eylem.ILAC_ALINDI.isNotBlank())
        assertTrue(Kullanim.Eylem.HIZLI_KOMUT.isNotBlank())
    }

    // ══════════════════════════════════════════════════════════
    // Gizlilik sözleşmesi
    // ══════════════════════════════════════════════════════════

    @Test
    fun `eylem adlari kisisel veri icermez`() {
        // Bu test bir SÖZLEŞME testi: eylem adları sabit
        // etiketler olmalı, kullanıcı verisi taşımamalı.
        // Biri ileride `Kullanim.eylem(ctx, "Not: " + baslik)`
        // yazarsa bu testi göremez ama sabitler listesi
        // niyeti belgeliyor.
        val hepsi = listOf(
            Kullanim.Eylem.GOREV_EKLE, Kullanim.Eylem.ILAC_ALINDI,
            Kullanim.Eylem.HARCAMA_EKLE
        )
        hepsi.forEach {
            // Sabit etiketler kısa olmalı — uzun metin
            // kullanıcı verisi sızdığının işareti olurdu
            assertTrue("Şüpheli uzunlukta eylem adı: $it", it.length < 40)
        }
    }
}
