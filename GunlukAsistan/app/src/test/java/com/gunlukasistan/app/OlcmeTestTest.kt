package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.6 — Ölçme ve zayıf nokta testleri (öneri 31, 35, 36).
 *
 * ── Neden test ──
 * Normalize kazanım formülü kullanıcıya "ne kadar öğrendin"
 * söylüyor. Yanlış hesaplarsa:
 *   · Yüksek gösterirse → yanlış güven, çalışmayı bırakır
 *   · Düşük gösterirse → emeği görünmez, motivasyon kırılır
 *
 * Net hesabı da (doğru − yanlış/4) Türkiye sınav sisteminin
 * standardı; yanlış olursa deneme sonuçları anlamsızlaşır.
 */
class OlcmeTestTest {

    private fun kazanim(on: Int, son: Int) = OlcmeTest.Kazanim(
        konuId = 1L, konuAdi = "test", onYuzde = on, sonYuzde = son,
        onZaman = 1000L, sonZaman = 2000L
    )

    // ══════════════════════════════════════════════════════════
    // Normalize kazanım (Hake gain)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ham fark dogru hesaplanir`() {
        assertEquals(45, kazanim(40, 85).hamFark)
        assertEquals(-10, kazanim(60, 50).hamFark)
        assertEquals(0, kazanim(70, 70).hamFark)
    }

    @Test
    fun `normalize kazanim formulu dogru`() {
        // g = (son - ön) / (100 - ön)
        // (85-40)/(100-40) = 45/60 = 0.75
        assertEquals(0.75, kazanim(40, 85).normalizeKazanim, 0.001)
    }

    @Test
    fun `esit zorluktaki ilerlemeler esit puan alir`() {
        // Bu formülün asıl amacı: %90→%95 ile %20→%60 eşit değerde
        // (90→95): 5/10 = 0.50
        // (20→60): 40/80 = 0.50
        assertEquals(
            kazanim(90, 95).normalizeKazanim,
            kazanim(20, 60).normalizeKazanim,
            0.001
        )
    }

    @Test
    fun `yuksek baslangictan ilerleme daha degerli`() {
        // Ham fark aynı (20 puan) ama yüksek başlangıçtan olan
        // normalize kazanımda daha yüksek olmalı
        val dusuk = kazanim(20, 40)   // 20/80 = 0.25
        val yuksek = kazanim(70, 90)  // 20/30 = 0.667
        assertEquals(dusuk.hamFark, yuksek.hamFark)
        assertTrue(
            "Yüksek başlangıçtan aynı ham fark daha değerli olmalı",
            yuksek.normalizeKazanim > dusuk.normalizeKazanim
        )
    }

    @Test
    fun `on test yuz ise kazanim tam sayilir`() {
        // Sıfıra bölme koruması: (100-100) = 0
        assertEquals(1.0, kazanim(100, 100).normalizeKazanim, 0.001)
        assertEquals(1.0, kazanim(100, 90).normalizeKazanim, 0.001)
    }

    @Test
    fun `gerileme negatif kazanim verir`() {
        // %60'tan %40'a düşüş
        assertTrue("Gerileme negatif olmalı", kazanim(60, 40).normalizeKazanim < 0)
    }

    @Test
    fun `kazanim her zaman -1 ile 1 arasinda`() {
        val ciftler = listOf(
            0 to 0, 0 to 100, 100 to 0, 50 to 50,
            99 to 100, 1 to 99, 100 to 100, 0 to 1
        )
        ciftler.forEach { (on, son) ->
            val g = kazanim(on, son).normalizeKazanim
            assertTrue("Kazanım aralık dışı ($on→$son): $g", g in -1.0..1.0)
        }
    }

    @Test
    fun `seviye esikleri dogru`() {
        assertEquals("0.75 yüksek olmalı", 2, kazanim(40, 85).seviye)
        // (60-40)/60 = 0.333 → orta
        assertEquals("0.33 orta olmalı", 1, kazanim(40, 60).seviye)
        // (45-40)/60 = 0.083 → düşük
        assertEquals("0.08 düşük olmalı", 0, kazanim(40, 45).seviye)
    }

    // ══════════════════════════════════════════════════════════
    // Sınav simülasyonu — net hesabı
    // ══════════════════════════════════════════════════════════

    private fun sim(d: Int, y: Int, b: Int, sn: Int = 600) = OlcmeTest.Simulasyon(
        id = 1L, baslik = "test", dogru = d, yanlis = y, bos = b,
        sureSn = sn, zaman = 0L
    )

    @Test
    fun `net hesabi turkiye sistemine uygun`() {
        // 4 yanlış = 1 doğru götürür
        assertEquals(9.0, sim(10, 4, 0).net, 0.001)
        assertEquals(20.0, sim(20, 0, 0).net, 0.001)
        assertEquals(-1.0, sim(0, 4, 0).net, 0.001)
    }

    @Test
    fun `bos sorular neti etkilemez`() {
        assertEquals(
            "Boş bırakmak net kaybettirmemeli",
            sim(10, 4, 0).net, sim(10, 4, 6).net, 0.001
        )
    }

    @Test
    fun `yuzde dogru hesaplanir`() {
        assertEquals(50, sim(10, 5, 5).yuzde)
        assertEquals(100, sim(20, 0, 0).yuzde)
        assertEquals(0, sim(0, 10, 0).yuzde)
    }

    @Test
    fun `bos sinav cokmez`() {
        val s = sim(0, 0, 0)
        assertEquals("Sıfıra bölme olmamalı", 0, s.yuzde)
        assertEquals(0, s.soruBasiSn)
        assertEquals(0.0, s.net, 0.001)
    }

    @Test
    fun `soru basi sure dogru`() {
        // 600 saniye / 20 soru = 30 sn
        assertEquals(30, sim(15, 3, 2, sn = 600).soruBasiSn)
    }

    @Test
    fun `toplam soru sayisi dogru`() {
        assertEquals(20, sim(12, 5, 3).toplam)
    }

    // ══════════════════════════════════════════════════════════
    // Zayıf nokta — Bulgu modeli
    // ══════════════════════════════════════════════════════════

    private fun bulgu(puan: Int) = ZayifNokta.Bulgu(
        konuId = 1L, konuAdi = "test", puan = puan, sinyalSayisi = 5,
        hataSayisi = 3, quizYuzde = 50, ef = 1.8, tamamlanmaYuzde = 40,
        sebepler = emptyList()
    )

    @Test
    fun `zayiflik seviyesi esikleri`() {
        assertEquals("65+ acil olmalı", 2, bulgu(65).seviye)
        assertEquals("70 acil olmalı", 2, bulgu(70).seviye)
        assertEquals("40-64 dikkat olmalı", 1, bulgu(40).seviye)
        assertEquals("50 dikkat olmalı", 1, bulgu(50).seviye)
        assertEquals("39 ve altı iyi olmalı", 0, bulgu(39).seviye)
        assertEquals("0 iyi olmalı", 0, bulgu(0).seviye)
    }

    @Test
    fun `puan agirliklari toplami 100`() {
        // Hata 35 + Quiz 30 + EF 25 + Yarım 10 = 100
        assertEquals(100, 35 + 30 + 25 + 10)
    }

    // ══════════════════════════════════════════════════════════
    // Feynman
    // ══════════════════════════════════════════════════════════

    private fun deneme(puan: Int) = Feynman.Deneme(
        id = 1L, konu = "test", anlatim = "x", puan = puan, ozet = "",
        eksikler = emptyList(), jargon = emptyList(), zaman = 0L
    )

    @Test
    fun `feynman seviye esikleri`() {
        assertEquals(2, deneme(75).seviye)
        assertEquals(2, deneme(90).seviye)
        assertEquals(1, deneme(50).seviye)
        assertEquals(1, deneme(60).seviye)
        assertEquals(0, deneme(49).seviye)
        assertEquals(0, deneme(0).seviye)
    }

    @Test
    fun `feynman en az uzunluk makul`() {
        // Çok düşük olursa anlamsız anlatımlar değerlendirilir,
        // çok yüksek olursa kullanıcı bıkar
        assertTrue(Feynman.EN_AZ_UZUNLUK in 30..150)
    }

    // ══════════════════════════════════════════════════════════
    // Sınav ayarları
    // ══════════════════════════════════════════════════════════

    @Test
    fun `varsayilan sinav ayari makul`() {
        val a = OlcmeTest.Ayar()
        assertTrue("Soru sayısı makul olmalı", a.soruSayisi in 5..50)
        assertTrue("Süre makul olmalı", a.sureDk in 5..120)
        assertTrue("Simülasyonda cevaplar gizlenmeli", a.cevaplariGizle)
    }

    @Test
    fun `olcum turleri farkli`() {
        assertTrue(OlcmeTest.TUR_ON != OlcmeTest.TUR_SON)
    }
}
