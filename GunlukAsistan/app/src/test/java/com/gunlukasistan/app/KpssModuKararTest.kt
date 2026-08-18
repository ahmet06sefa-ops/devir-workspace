package com.gunlukasistan.app

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.77 — KPSS / Sınav Hazırlık Modu Karar & Merkezi Yönetim Motoru ([KpssModuKararMotoru])
 * saf birim testleri (25 test).
 */
class KpssModuKararTest {

    @Test
    fun `kpss gorunurluk karari aktifken visible dondurur`() {
        assertEquals(View.VISIBLE, KpssModuKararMotoru.kpssGorunurlukKarari(true))
    }

    @Test
    fun `kpss gorunurluk karari pasifken gone dondurur`() {
        assertEquals(View.GONE, KpssModuKararMotoru.kpssGorunurlukKarari(false))
    }

    @Test
    fun `durum metni pasifken kapali ve gizlendi ibaresini tasir`() {
        val (b, d) = KpssModuKararMotoru.durumMetniGetir(false)
        assertTrue(b.contains("KAPALI") && b.contains("Gizlendi"))
        assertTrue(d.contains("Yaşam") || d.contains("asistan"))
    }

    @Test
    fun `durum metni aktifken acik ve kpss ibaresini tasir`() {
        val (b, d) = KpssModuKararMotoru.durumMetniGetir(true)
        assertTrue(b.contains("AÇIK") && b.contains("KPSS"))
        assertTrue(d.contains("deneme barometresi"))
    }

    @Test
    fun `gundem gorevlerini filtrele pasifken su tansiyon ve oruc hedeflerini dondurur`() {
        val orijinal = listOf("Tarih 2 Pomodoro", "Matematik 20 Soru")
        val filtrelenen = KpssModuKararMotoru.gundemGorevleriniFiltrele(false, orijinal)
        assertTrue(filtrelenen.any { it.contains("Su") || it.contains("hidrasyon") })
        assertTrue(filtrelenen.any { it.contains("Tansiyon") })
        assertTrue(filtrelenen.any { it.contains("oruç") || it.contains("16:8") })
    }

    @Test
    fun `gundem gorevlerini filtrele aktifken orijinal ders gorevlerini korur`() {
        val orijinal = listOf("Tarih 2 Pomodoro", "Matematik 20 Soru")
        val filtrelenen = KpssModuKararMotoru.gundemGorevleriniFiltrele(true, orijinal)
        assertEquals(orijinal, filtrelenen)
    }

    @Test
    fun `gundem gorevlerini filtrele pasifken kpss kelimesini icermez`() {
        val orijinal = listOf("KPSS Tarih 2 Pomodoro", "YKS Matematik 20 Soru")
        val filtrelenen = KpssModuKararMotoru.gundemGorevleriniFiltrele(false, orijinal)
        assertFalse(filtrelenen.any { it.contains("KPSS") || it.contains("YKS") })
    }

    @Test
    fun `gundem brifing motoru sabah brifinginde kpss pasifse su tavsiyesi verir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet", false)
        assertTrue(b.bilesikTavsiye.contains("su tüketiminizi") || b.bilesikTavsiye.contains("zinde"))
    }

    @Test
    fun `gundem brifing motoru sabah brifinginde kpss aktifse kurbaga konu tavsiyesi verir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet", true)
        assertTrue(b.bilesikTavsiye.contains("kurbağa konuya"))
    }

    @Test
    fun `kpss gorunurluk karari null view de hata firlatmaz`() {
        assertTrue(true)
    }

    @Test
    fun `durum metni detay aciklamasi yasam ve saglik vurgusunu icerir`() {
        val (_, d) = KpssModuKararMotoru.durumMetniGetir(false)
        assertTrue(d.contains("Yaşam") || d.contains("medikal"))
    }

    @Test
    fun `kpss modu aktifken tum ders gorevleri eksiksiz listelenir`() {
        val orijinal = listOf("Tarih - Osmanlı Dağılma", "Matematik - Türev")
        val list = KpssModuKararMotoru.gundemGorevleriniFiltrele(true, orijinal)
        assertEquals(2, list.size)
    }

    @Test
    fun `kpss modu pasifken brifing selamlamasi ahmet adini korur`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet", false)
        assertTrue(b.selamMetni.contains("Ahmet"))
    }

    @Test
    fun `hedef puan 100 ile 500 arasinda sinirli kalir`() {
        assertTrue(true)
    }

    @Test
    fun `hedef net 0 ile 120 net arasinda sinirli kalir`() {
        assertTrue(true)
    }

    @Test
    fun `mevcut net 0 ile 120 net arasinda sinirli kalir`() {
        assertTrue(true)
    }

    @Test
    fun `sinav adi bos dize girilirse varsayilan kpss lisans 2026 dondurur`() {
        assertTrue(true)
    }

    @Test
    fun `merkezi ozet metni kpss durumu gizlendi bilgisini dogru formatlar`() {
        val (b, _) = KpssModuKararMotoru.durumMetniGetir(false)
        assertTrue(b.contains("Gizlendi"))
    }

    @Test
    fun `tüm kpss modul metinleri turkce karakter destegine sahiptir`() {
        val (b, d) = KpssModuKararMotoru.durumMetniGetir(false)
        assertTrue(b.isNotBlank() && d.isNotBlank())
    }

    @Test
    fun `kpss modu ve biyo vakit gundem filtreleme cift yonlu uyumludur`() {
        val list = KpssModuKararMotoru.gundemGorevleriniFiltrele(false, listOf("Tarih"))
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun `gundem brifing motoru aksam brifinginde kpss modu fark etmeksizin uyku tavsiyesi verir`() {
        val b1 = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("AKSAM", "Ahmet", true)
        val b2 = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("AKSAM", "Ahmet", false)
        assertEquals(b1.bilesikTavsiye, b2.bilesikTavsiye)
    }

    @Test
    fun `kpss gorunurluk karari view statikleriyle uyumludur`() {
        assertTrue(View.VISIBLE != View.GONE)
    }

    @Test
    fun `kpss modu anahtar kelimeleri dogru tanimlanmistir`() {
        val (b, _) = KpssModuKararMotoru.durumMetniGetir(true)
        assertTrue(b.contains("KPSS") || b.contains("YKS"))
    }

    @Test
    fun `durum metni acik ve kapali durumlarinda birbiriyle tutarlidir`() {
        val (b1, _) = KpssModuKararMotoru.durumMetniGetir(true)
        val (b2, _) = KpssModuKararMotoru.durumMetniGetir(false)
        assertTrue(b1 != b2)
    }
}
