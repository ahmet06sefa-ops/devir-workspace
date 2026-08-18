package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v7.99 — [Tekrar] birim testleri (öneri 10).
 *
 * ── Neden bu sınıf seçildi ──
 * Tekrarlayan görevlerin sonraki tarihini hesaplıyor. Hata yaparsa
 * kullanıcı görevini yanlış günde görür ya da hiç görmez — sessiz ve
 * fark edilmesi zor bir bozulma. Saf mantık: Android çerçevesine bağımlı
 * olmadığı için doğrudan test edilebilir.
 *
 * ── Testleri yazarken öğrenilen davranış ──
 * `sonraki()` sonucu **her zaman gelecekte** olacak şekilde ileri sarıyor:
 * geçmişte kalan bir görev için "bir gün sonrası" değil, "şimdiden sonraki
 * ilk uygun tarih" veriyor. Bu doğru davranış — iki ay açılmamış günlük
 * görev iki ay öncesine kurulmamalı. Testler bu sözleşmeyi sabitliyor.
 */
class TekrarTest {

    /** Bugünden [gunSonra] gün sonrası, saat sabit. */
    private fun ileriGun(gunSonra: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, gunSonra)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun gunFarki(a: Long, b: Long): Int =
        Math.round((b - a) / 86_400_000.0).toInt()

    // ═══════════════════════════════════════════════════════════════
    // AKTİFLİK
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `yok kodu tekrarsiz sayilir`() {
        assertFalse(Tekrar.aktifMi(Tekrar.YOK))
        assertFalse(Tekrar.aktifMi(null))
        assertFalse(Tekrar.aktifMi(""))
    }

    @Test
    fun `gecerli kodlar aktif sayilir`() {
        assertTrue(Tekrar.aktifMi("gun"))
        assertTrue(Tekrar.aktifMi("hafta"))
        assertTrue(Tekrar.aktifMi("ay"))
    }

    // ═══════════════════════════════════════════════════════════════
    // TEMEL ARALIKLAR — gelecekteki bir tarihten
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `gunluk tekrar bir gun ekler`() {
        val baslangic = ileriGun(10)
        val sonraki = Tekrar.sonraki("gun", baslangic)
        assertEquals(1, gunFarki(baslangic, sonraki))
    }

    @Test
    fun `haftalik tekrar yedi gun ekler`() {
        val baslangic = ileriGun(10)
        val sonraki = Tekrar.sonraki("hafta", baslangic)
        assertEquals(7, gunFarki(baslangic, sonraki))
    }

    @Test
    fun `iki haftalik tekrar on dort gun ekler`() {
        val baslangic = ileriGun(10)
        val sonraki = Tekrar.sonraki("2hafta", baslangic)
        assertEquals(14, gunFarki(baslangic, sonraki))
    }

    @Test
    fun `ozel aralik kodu dogru gun ekler`() {
        val baslangic = ileriGun(10)
        val sonraki = Tekrar.sonraki("ozel:3", baslangic)
        assertEquals(3, gunFarki(baslangic, sonraki))
    }

    @Test
    fun `aylik tekrar makul araliktadir`() {
        val baslangic = ileriGun(10)
        val sonraki = Tekrar.sonraki("ay", baslangic)
        val fark = gunFarki(baslangic, sonraki)
        assertTrue("Aylık aralık $fark gün — beklenen 28..31", fark in 28..31)
    }

    // ═══════════════════════════════════════════════════════════════
    // GEÇMİŞ TARİH — ileri sarma sözleşmesi
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `gecmisteki gorev gelecege tasinir`() {
        // 60 gün önce kurulmuş günlük görev
        val eski = ileriGun(-60)
        val sonraki = Tekrar.sonraki("gun", eski)
        assertTrue(
            "Sonuç gelecekte olmalı — geçmişe kurulmuş tekrar anlamsız",
            sonraki > System.currentTimeMillis()
        )
    }

    @Test
    fun `gecmisteki haftalik gorev gelecege tasinir`() {
        val eski = ileriGun(-30)
        val sonraki = Tekrar.sonraki("hafta", eski)
        assertTrue(sonraki > System.currentTimeMillis())
    }

    // ═══════════════════════════════════════════════════════════════
    // BİTİŞ TARİHİ
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `bitis tarihi asilinca sifir doner`() {
        val baslangic = ileriGun(10)
        // Bitiş başlangıçla aynı: bir gün sonrası bitişi aşar
        assertEquals(0L, Tekrar.sonraki("gun", baslangic, baslangic))
    }

    @Test
    fun `bitis tarihi ileriyse tekrar surer`() {
        val baslangic = ileriGun(10)
        val bitis = ileriGun(200)
        assertTrue(Tekrar.sonraki("gun", baslangic, bitis) > 0L)
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜN LİSTESİ KODLAMA
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `gun listesi kodlanip geri cozulebilir`() {
        val gunler = listOf(2, 4, 6)
        val kod = Tekrar.gunleriKodla(gunler)
        assertEquals(gunler, Tekrar.gunleriCoz(kod))
    }

    @Test
    fun `gun listesi siralanir ve tekrarlar temizlenir`() {
        val kod = Tekrar.gunleriKodla(listOf(6, 2, 2, 4))
        assertEquals(listOf(2, 4, 6), Tekrar.gunleriCoz(kod))
    }

    @Test
    fun `bos gun listesi bos coz verir`() {
        assertTrue(Tekrar.gunleriCoz(null).isEmpty())
        assertTrue(Tekrar.gunleriCoz("").isEmpty())
        assertTrue(Tekrar.gunleriCoz("gun").isEmpty())
    }

    @Test
    fun `bozuk gun kodu cokmez ve suzulur`() {
        // Bozulmuş kullanıcı verisi — çökmek yerine geçerli değerler kalmalı
        val sonuc = Tekrar.gunleriCoz("gunler:abc,,9,3,0,5")
        assertEquals(listOf(3, 5), sonuc)
    }

    // ═══════════════════════════════════════════════════════════════
    // GEÇERSİZ GİRDİ
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `tekrarsiz kodlar sifir doner`() {
        val baslangic = ileriGun(10)
        assertEquals(0L, Tekrar.sonraki(Tekrar.YOK, baslangic))
        assertEquals(0L, Tekrar.sonraki(null, baslangic))
        assertEquals(0L, Tekrar.sonraki("", baslangic))
    }
}
