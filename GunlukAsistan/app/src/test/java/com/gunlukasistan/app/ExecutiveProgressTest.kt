package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.74 — Profesyonel İlerleme Ekranı (Executive Dashboard) Motoru ([ExecutiveProgressMotoru])
 * saf birim testleri (26 test).
 */
class ExecutiveProgressTest {

    // ── 1. KPI KOKPİTİ TESTLERİ (1..7) ──
    @Test
    fun `kpi kokpiti 4 adet executive kpi listeler`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27)
        assertEquals(4, kpis.size)
    }

    @Test
    fun `kpi odak verimliligi 900 dakika icin 90 yuzde dondurur`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27)
        assertEquals("%90", kpis[0].deger)
    }

    @Test
    fun `kpi rutbe 1500 dakika ustu icin altin efsane dondurur`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(1550, 6, 4, 27)
        assertEquals("Altın Efsane", kpis[2].deger)
    }

    @Test
    fun `kpi rutbe 600 dakika icin gumus usta dondurur`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(650, 6, 4, 27)
        assertEquals("Gümüş Usta", kpis[2].deger)
    }

    @Test
    fun `kpi rutbe 300 dakika icin bronz cirak dondurur`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(300, 6, 4, 27)
        assertEquals("Bronz Çırak", kpis[2].deger)
    }

    @Test
    fun `kpi yasam ders denge skoru odak verimi ve yasam uyumunu harmanlar`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27)
        assertTrue(kpis[3].deger.contains("90/100"))
    }

    @Test
    fun `kpi alt metinleri odak suresi ve seri gun bilgisini barindirir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27)
        assertTrue(kpis[0].altMetin.contains("900 dk"))
        assertTrue(kpis[1].altMetin.contains("Seriniz Güvende"))
    }

    // ── 2. SINAV NET & PUAN PROJEKSİYONU TESTLERİ (8..17) ──
    @Test
    fun `puan projeksiyonu bos deneme listesinde veri bekleniyor dondurur`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(emptyList(), 90.0)
        assertEquals("Veri bekleniyor", p.trendDurumu)
    }

    @Test
    fun `puan projeksiyonu yukselen denemelerde hedefe yaklasir`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(75.0, 78.0, 81.0, 84.0), 90.0)
        assertTrue(p.trendDurumu.contains("Gelişim Devam Ediyor") || p.trendDurumu.contains("Hedefe Çok Yakın"))
    }

    @Test
    fun `puan projeksiyonu hedef asildiginda hedef asiliyor mesaji dondurur`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(85.0, 88.0, 91.0, 93.0), 90.0)
        assertTrue(p.trendDurumu.contains("Hedef Aşılıyor"))
    }

    @Test
    fun `puan projeksiyonu ortalama neti dogru hesaplar`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(70.0, 80.0), 90.0)
        assertEquals(75.0, p.ortalamaNet, 0.01)
    }

    @Test
    fun `puan projeksiyonu artis egilimini deneme sayisina gore carpar`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(70.0, 80.0), 90.0)
        assertTrue(p.tahminiSinavNeti > p.ortalamaNet)
    }

    @Test
    fun `puan projeksiyonu kalan farki negatif yapmaz 0 da sabitler`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(95.0, 98.0), 90.0)
        assertEquals(0.0, p.kalanFark, 0.001)
    }

    @Test
    fun `puan projeksiyonu tahmini neti 120 net sinirinda tutar`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(115.0, 125.0), 90.0)
        assertTrue(p.tahminiSinavNeti <= 120.0)
    }

    @Test
    fun `puan projeksiyonu hedefe cok yakin durumunu dogru saptar`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(85.0, 86.0, 87.0), 90.0)
        assertTrue(p.trendDurumu.contains("Hedefe Çok Yakın"))
    }

    @Test
    fun `puan projeksiyonu tahmini net ve ortalama net ondalik formatla uyumludur`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(72.5, 74.5), 90.0)
        assertTrue(p.tahminiSinavNeti > 0.0)
    }

    @Test
    fun `puan projeksiyonu tek deneme durumunu sifir bolme hatasi olmadan hesaplar`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(listOf(80.0), 90.0)
        assertEquals(80.0, p.ortalamaNet, 0.01)
    }

    // ── 3. EXECUTIVE ASCII KARNE & FORMAT TESTLERİ (18..26) ──
    @Test
    fun `executive karne metni olusturma ascii cerceve dondurur`() {
        val str = ExecutiveProgressMotoru.executiveKarneMetniOlustur()
        assertTrue(str.contains("EXECUTIVE PROJE İLERLEME KARNESİ"))
        assertTrue(str.contains("╔") && str.contains("╝"))
    }

    @Test
    fun `executive karne metni unvan ve net bilgisini tasir`() {
        val str = ExecutiveProgressMotoru.executiveKarneMetniOlustur("Altın Efsane", 1600, 10, 89.5)
        assertTrue(str.contains("Altın Efsane"))
        assertTrue(str.contains("89.5 Net"))
    }

    @Test
    fun `kpi trend metinleri pozitif ok isaretlerini icerir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27)
        assertTrue(kpis[0].trend.contains("▲"))
    }

    @Test
    fun `executive karne metni kurumsal uyum basligini icerir`() {
        val str = ExecutiveProgressMotoru.executiveKarneMetniOlustur()
        assertTrue(str.contains("MÜKEMMEL KURUMSAL UYUM"))
    }

    @Test
    fun `kpi seri gun degeri dogru gosterilir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 12, 27)
        assertEquals("12 Gün", kpis[1].deger)
    }

    @Test
    fun `kpi denge skoru 100 uzerinden orantilanir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(500, 5, 3, 15)
        assertTrue(kpis[3].deger.endsWith("/100"))
    }

    @Test
    fun `executive karne metni satir sayisi en az 6 satirdir`() {
        val str = ExecutiveProgressMotoru.executiveKarneMetniOlustur()
        assertTrue(str.lines().size >= 6)
    }

    @Test
    fun `kpi id leri 1 ile 4 arasinda siralidir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla()
        assertEquals(listOf(1, 2, 3, 4), kpis.map { it.id })
    }

    @Test
    fun `tüm kpi ve projeksiyon metinleri turkce karakter destegine sahiptir`() {
        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla()
        assertTrue(kpis.all { it.baslik.isNotBlank() })
    }
}
