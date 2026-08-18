package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.20 · SINIRSIZ WIDGET KONTROLÜ — serbest değer matematiği birim testleri.
 *
 * Kullanıcı isteği: "sınır koyma". Testler iki ilkeyi kilitler:
 *  1) Kademe/sınır YOK — yalnız teknik güvenlik kelepçeleri (negatif
 *     setTextSize çökmesi, Canvas negatif yarıçap, alfa 0-255) kalır.
 *  2) Eski kademe değerleri yeni modelde AYNI sonucu üretmeli (taşıma
 *     doğruluğu) — örn. kontrast %-100 ≡ eski "açık" anahtarı.
 *
 * Renk matematiği bit işlemli; android.jar stub tuzağına düşülmez.
 */
class WidgetSerbestTest {

    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    private val temel = WidgetTema.Palet(
        zemin = 0xFF1C1814.toInt(),
        zeminAlt = 0xFF241E18.toInt(),
        metin = 0xFFF5EDE3.toInt(),
        metinSoluk = 0xFFA99C8C.toInt(),
        vurgu = 0xFFE0B183.toInt(),
        vurguSoluk = 0xFF3A2E23.toInt(),
        yesil = 0xFFA3BE96.toInt(),
        koyuMu = true
    )

    // ── yazı ölçeği: üst sınır YOK, taban %1 ──

    @Test
    fun guvenliOlcekYuzde_ustSinirYokTabanVar() {
        assertEquals(100, WidgetAtolye.guvenliOlcekYuzde(100))
        assertEquals(137, WidgetAtolye.guvenliOlcekYuzde(137))
        assertEquals(1, WidgetAtolye.guvenliOlcekYuzde(0))
        assertEquals(1, WidgetAtolye.guvenliOlcekYuzde(-40))
        // Üst sınır konulmadı: dev değerler aynen geçer
        assertEquals(9999, WidgetAtolye.guvenliOlcekYuzde(9999))
    }

    // ── saydamlık: serbest yüzde, fiziksel aralık tam ──

    @Test
    fun saydamlikYuzdeAlfa_tamAralik() {
        assertEquals(255, WidgetZemin.saydamlikYuzdeAlfa(0))
        assertEquals(0, WidgetZemin.saydamlikYuzdeAlfa(100))
        assertEquals(255, WidgetZemin.saydamlikYuzdeAlfa(-5))
        assertEquals(0, WidgetZemin.saydamlikYuzdeAlfa(120))
    }

    @Test
    fun saydamlikYuzdeAlfa_eskiKademelerleUyumlu() {
        // Eski 4 kademenin alfa karşılıkları yeni tabloda da üretilebilmeli
        assertEquals(WidgetZemin.saydamlikAlfa(3), WidgetZemin.saydamlikYuzdeAlfa(34))
        assertEquals(WidgetZemin.saydamlikAlfa(0), WidgetZemin.saydamlikYuzdeAlfa(0))
    }

    // ── köşe: negatif çökertir, dev değerler tavana çekilir ──

    @Test
    fun koseDpGuvenli_teknikKelepce() {
        assertEquals(0f, WidgetZemin.koseDpGuvenli(-2f), 0.001f)
        assertEquals(48f, WidgetZemin.koseDpGuvenli(48f), 0.001f)
        assertEquals(2000f, WidgetZemin.koseDpGuvenli(9999f), 0.001f)
    }

    // ── karartma şiddeti serbest yüzde ──

    @Test
    fun karartFaktorPct_kademeDegerleriniKapsar() {
        assertEquals(0f, WidgetSecim.karartFaktorPct(0), 0.001f)
        assertEquals(0.40f, WidgetSecim.karartFaktorPct(40), 0.001f)
        assertEquals(1f, WidgetSecim.karartFaktorPct(100), 0.001f)
        // Eski kademe tablosu yeni modelde aynen üretilebilir
        assertEquals(WidgetSecim.karartFaktor(2), WidgetSecim.karartFaktorPct(60), 0.001f)
    }

    // ── canlılık serbest yüzde ──

    @Test
    fun canlandirPct_100dokunmazTaşımaDogrulu() {
        val v = 0xFFE0B183.toInt()
        val z = 0xFF1C1814.toInt()
        assertEquals(v, WidgetSecim.canlandirPct(v, z, 100))
        // Eski "canlı" kademesi (135%) yeni modelde birebir aynı canlandir sonucu
        assertEquals(
            WidgetSecim.canlandir(v, 1.35f),
            WidgetSecim.canlandirPct(v, z, 135)
        )
        // 100 altı zemine yumuşatır (parlaklık zemine yaklaşır)
        val yumusak = WidgetSecim.canlandirPct(v, z, 40)
        assertTrue(
            kotlin.math.abs(WidgetSecim.parlaklikUc(yumusak) - WidgetSecim.parlaklikUc(z)) <
                kotlin.math.abs(WidgetSecim.parlaklikUc(v) - WidgetSecim.parlaklikUc(z))
        )
    }

    // ── kontrast serbest yüzde: %-100 ≡ eski anahtar ──

    @Test
    fun kontrastUygula_yuzYuzEskiAcikAnahtariylaAyni() {
        assertEquals(temel, WidgetSecim.kontrastUygula(temel, 0))
        val eskiYol = WidgetSecim.kontrastUygula(temel)
        val yeniYol = WidgetSecim.kontrastUygula(temel, 100)
        assertEquals(eskiYol.metin, yeniYol.metin)
        assertEquals(eskiYol.metinSoluk, yeniYol.metinSoluk)
    }

    // ── hex çözümleyici: sınırsız renk girişi ──

    @Test
    fun hexOku_kabulEdilenBicimler() {
        assertNull(WidgetSecim.hexOku(null))
        assertNull(WidgetSecim.hexOku(""))
        assertNull(WidgetSecim.hexOku("#FFF"))        // 3 hane desteklenmez
        assertNull(WidgetSecim.hexOku("#GGGGGG"))
        assertNull(WidgetSecim.hexOku("#1234567"))    // 7 hane
        assertEquals(0xFFFF8800.toInt(), WidgetSecim.hexOku("#FF8800"))
        assertEquals(0xFFFF8800.toInt(), WidgetSecim.hexOku("ff8800"))   // # şart değil
        assertEquals(0xFF123ABC.toInt(), WidgetSecim.hexOku("  #12 3abc".replace(" ", "")))
        assertEquals(0x80FF8800.toInt(), WidgetSecim.hexOku("80FF8800")) // alfa 8 hane
    }

    @Test
    fun hexYaz_geriDonusum() {
        assertEquals("#FF8800", WidgetSecim.hexYaz(0xFFFF8800.toInt()))
        val c = 0xFFA3BE96.toInt()
        assertEquals(c, WidgetSecim.hexOku(WidgetSecim.hexYaz(c)))
    }

    // ── özel renk şablonu ──

    @Test
    fun ozRenkleriUygula_nullTumleriDegismez() {
        assertEquals(temel, WidgetSecim.ozRenkleriUygula(temel))
    }

    @Test
    fun ozRenkleriUygula_zeminVeTurevler() {
        val acik = 0xFFE8E0D4.toInt()
        val c = WidgetSecim.ozRenkleriUygula(temel, zemin = acik)
        assertEquals(acik, c.zemin)
        // Açık zemin → koyuMu tersine döner; zeminAlt türetilir (zeminden farklı)
        assertTrue(!c.koyuMu)
        assertTrue(c.zeminAlt != acik)
        // Metin ve vurgu dokunulmaz
        assertEquals(temel.metin, c.metin)
        assertEquals(temel.vurgu, c.vurgu)
    }

    @Test
    fun ozRenkleriUygula_metinVurguYesil() {
        val m = 0xFF00FF00.toInt()
        val v = 0xFF8800FF.toInt()
        val y = 0xFFFF0000.toInt()
        val c = WidgetSecim.ozRenkleriUygula(temel, metin = m, vurgu = v, yesil = y)
        assertEquals(m, c.metin)
        assertEquals(v, c.vurgu)
        assertEquals(y, c.yesil)
        // Türev soluklar değişti (zemine göre karıştırıldı)
        assertTrue(c.metinSoluk != temel.metinSoluk)
        assertTrue(c.vurguSoluk != temel.vurguSoluk)
        // Kanallar anlamlı aralıkta
        assertTrue(r(c.metinSoluk) in 0..255 && g(c.vurguSoluk) in 0..255 && b(c.metinSoluk) in 0..255)
    }

    // ── örnek-başına ölçek ──

    @Test
    fun ornekOlcekGuvenli_tabanVarTavanYok() {
        assertEquals(100, WidgetSecim.ornekOlcekGuvenli(100))
        assertEquals(1, WidgetSecim.ornekOlcekGuvenli(0))
        assertEquals(1, WidgetSecim.ornekOlcekGuvenli(-9))
        assertEquals(5000, WidgetSecim.ornekOlcekGuvenli(5000))
    }
}
