package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * v10.17 · Widget Ayar Envanteri — saf tabloların birim testleri.
 *
 * Renk matematiği bit işlemleriyle yazıldığı için android.jar stub'ına
 * takılmadan JVM'de koşar. Kanal okuma yardımcıları test içinde tanımlı.
 */
class WidgetSecimTest {

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

    // ── karıştırma / parlaklık ──

    @Test
    fun karistirUc_sinirDegerleri() {
        val a = 0xFF102030.toInt()
        val bb = 0xFF90A0B0.toInt()
        assertEquals(a, WidgetSecim.karistirUc(a, bb, 0f))
        assertEquals(bb, WidgetSecim.karistirUc(a, bb, 1f))
        // t=0.5 → her kanal iki ucun ortası
        val o = WidgetSecim.karistirUc(a, bb, 0.5f)
        assertEquals(0x50, r(o))
        assertEquals(0x60, g(o))
        assertEquals(0x70, b(o))
    }

    @Test
    fun parlaklikUc_beyazVeSiyah() {
        assertTrue(WidgetSecim.parlaklikUc(0xFFFFFFFF.toInt()) > 0.99f)
        assertTrue(WidgetSecim.parlaklikUc(0xFF000000.toInt()) < 0.01f)
        val griDeger = WidgetSecim.parlaklikUc(0xFF808080.toInt())
        assertTrue(abs(griDeger - 0.502f) < 0.01f)
    }

    @Test
    fun canlandir_griNotrDoygunArtar() {
        // Gri: tüm kanallar eşit → doygunluk ölçeği değişmez
        assertEquals(0xFF808080.toInt(), WidgetSecim.canlandir(0xFF808080.toInt(), 1.35f))
        // Doygun kırmızımsı renk: R kanalı yükselir, B düşer
        val c = WidgetSecim.canlandir(0xFFC04040.toInt(), 1.35f)
        assertTrue(r(c) > 0xC0)
        assertTrue(b(c) < 0x40)
        // Taşma kelepçesi: saf kırmızı 255'i geçemez
        assertEquals(0xFF, r(WidgetSecim.canlandir(0xFFFF0000.toInt(), 1.35f)))
    }

    // ── vurgu / tamamlanan ──

    @Test
    fun vurguAyarla_kademeTablosu() {
        val v = 0xFFE0B183.toInt()
        val z = 0xFF1C1814.toInt()
        assertEquals(v, WidgetSecim.vurguAyarla(v, z, 1))
        assertEquals(WidgetSecim.vurguAyarla(v, z, 2), WidgetSecim.vurguAyarla(v, z, 99)) // taşan → 2'ye kelepçe
        val soluk = WidgetSecim.vurguAyarla(v, z, 0)
        assertTrue(WidgetSecim.parlaklikUc(soluk) < WidgetSecim.parlaklikUc(v))
        val canli = WidgetSecim.vurguAyarla(v, z, 2)
        assertTrue(canli != v)
    }

    @Test
    fun tamamRengi_modTablosu() {
        val yesil = 0xFFA3BE96.toInt()
        val vurgu = 0xFFE0B183.toInt()
        assertEquals(yesil, WidgetSecim.tamamRengi(yesil, 0, vurgu))
        assertEquals(0xFF64A0DC.toInt(), WidgetSecim.tamamRengi(yesil, 1, vurgu))
        assertEquals(0xFF9E9E9E.toInt(), WidgetSecim.tamamRengi(yesil, 2, vurgu))
        assertEquals(vurgu, WidgetSecim.tamamRengi(yesil, 3, vurgu))
        assertEquals(yesil, WidgetSecim.tamamRengi(yesil, -7, vurgu))
    }

    // ── kontrast / metin modu / karartma ──

    @Test
    fun kontrast_solukMetneYaklasir() {
        val c = WidgetSecim.kontrastUygula(temel)
        // Koyu zeminde metin beyaza yaklaşır
        assertTrue(WidgetSecim.parlaklikUc(c.metin) > WidgetSecim.parlaklikUc(temel.metin))
        // Soluk, ana metne yaklaşır (parlaklık farkı azalır)
        val farkOnce = abs(
            WidgetSecim.parlaklikUc(temel.metin) - WidgetSecim.parlaklikUc(temel.metinSoluk)
        )
        val farkSonra = abs(
            WidgetSecim.parlaklikUc(c.metin) - WidgetSecim.parlaklikUc(c.metinSoluk)
        )
        assertTrue(farkSonra < farkOnce)
    }

    @Test
    fun metinModu_otomatikDokunmaz() {
        val c = WidgetSecim.metinModuUygula(temel, 0)
        assertEquals(temel.metin, c.metin)
        assertEquals(temel.metinSoluk, c.metinSoluk)
        // taşan değer en yakın geçerli kademeye (3) kelepçelenir
        assertEquals(WidgetSecim.metinModuUygula(temel, 3), WidgetSecim.metinModuUygula(temel, 99))
    }

    @Test
    fun metinModu_acikKoyuVurgulu() {
        val acik = WidgetSecim.metinModuUygula(temel, 1)
        assertTrue(WidgetSecim.parlaklikUc(acik.metin) > WidgetSecim.parlaklikUc(temel.metin))
        val koyu = WidgetSecim.metinModuUygula(temel, 2)
        assertTrue(WidgetSecim.parlaklikUc(koyu.metin) < WidgetSecim.parlaklikUc(temel.metin))
        // Vurgu uyumlu: metin vurguya kayar → kırmızı kanalı değişir
        val vurgulu = WidgetSecim.metinModuUygula(temel, 3)
        assertTrue(vurgulu.metin != temel.metin)
    }

    @Test
    fun karartmaAktifMi_geceSarmasi() {
        // 22:00 → 07:00 penceresi (gece yarısını aşar)
        assertTrue(WidgetSecim.karartmaAktifMi(23 * 60, 22, 7))
        assertTrue(WidgetSecim.karartmaAktifMi(2 * 60 + 30, 22, 7))
        assertTrue(WidgetSecim.karartmaAktifMi(0, 22, 7))
        assertFalse(WidgetSecim.karartmaAktifMi(12 * 60, 22, 7))
        assertFalse(WidgetSecim.karartmaAktifMi(7 * 60, 22, 7))     // bitişte kapanır
        assertTrue(WidgetSecim.karartmaAktifMi(22 * 60, 22, 7))     // başlangıçta açılır
    }

    @Test
    fun karartmaAktifMi_kenarDurumlar() {
        // Normal pencere (başlangıç < bitiş)
        assertTrue(WidgetSecim.karartmaAktifMi(13 * 60, 12, 18))
        assertFalse(WidgetSecim.karartmaAktifMi(20 * 60, 12, 18))
        // Başlangıç == bitiş → hiç aktif olmaz (tüm gün karartma tuzağı yok)
        assertFalse(WidgetSecim.karartmaAktifMi(10 * 60, 10, 10))
        // Taşan saat 23'e kelepçelenir (22:00-23:00 penceresi); negatif dakika normalize edilir (-60 dk = 23:00)
        assertTrue(WidgetSecim.karartmaAktifMi(22 * 60 + 30, 22, 99))
        assertTrue(WidgetSecim.karartmaAktifMi(-60, 22, 7))
    }

    @Test
    fun karartFaktor_tablosu() {
        assertEquals(0.20f, WidgetSecim.karartFaktor(0), 0.001f)
        assertEquals(0.40f, WidgetSecim.karartFaktor(1), 0.001f)
        assertEquals(0.60f, WidgetSecim.karartFaktor(2), 0.001f)
        assertEquals(0.60f, WidgetSecim.karartFaktor(99), 0.001f) // taşan → 2
    }

    @Test
    fun karartUygula_zeminKararMetinKorunur() {
        val c = WidgetSecim.karartUygula(temel, 0.40f)
        assertTrue(WidgetSecim.parlaklikUc(c.zemin) < WidgetSecim.parlaklikUc(temel.zemin))
        assertTrue(WidgetSecim.parlaklikUc(c.zeminAlt) < WidgetSecim.parlaklikUc(temel.zeminAlt))
        assertEquals(temel.metin, c.metin)             // metin dokunulmaz
        assertTrue(c.koyuMu)                           // koyu kimliği korunur
        // sıfır şiddet → değişmez
        assertEquals(temel.zemin, WidgetSecim.karartUygula(temel, 0f).zemin)
    }

    // ── boşluk & davranış tabloları ──

    @Test
    fun yatayKatsayi_tablosu() {
        assertEquals(0.5f, WidgetSecim.yatayKatsayi(0), 0.001f)
        assertEquals(1.0f, WidgetSecim.yatayKatsayi(1), 0.001f)
        assertEquals(1.8f, WidgetSecim.yatayKatsayi(2), 0.001f)
        assertEquals(0.5f, WidgetSecim.yatayKatsayi(-3), 0.001f)  // taşan → 0
        assertEquals(1.8f, WidgetSecim.yatayKatsayi(42), 0.001f)     // taşan → 2
    }

    @Test
    fun girintiDp_tablosu() {
        assertEquals(0, WidgetSecim.girintiDp(0))
        assertEquals(4, WidgetSecim.girintiDp(1))
        assertEquals(10, WidgetSecim.girintiDp(2))
        assertEquals(10, WidgetSecim.girintiDp(9))
        assertEquals(0, WidgetSecim.girintiDp(-1))
    }

    @Test
    fun kisitMs_tablosu() {
        assertEquals(400L, WidgetSecim.kisitMs(0))
        assertEquals(2_000L, WidgetSecim.kisitMs(1))
        assertEquals(10_000L, WidgetSecim.kisitMs(2))
        assertEquals(400L, WidgetSecim.kisitMs(-5))
        assertEquals(10_000L, WidgetSecim.kisitMs(77))
    }

    // ── anahtar kaydı ──

    @Test
    fun gorunurlukAnahtarlari_benzersizVeDolu() {
        val a = WidgetSecim.GORUNURLUK_ANAHTARLARI
        // v10.21: 4 başlık çubuğu anahtarı eklendi (21 → 25)
        assertEquals(25, a.size)
        assertEquals(a.size, a.toSet().size)
        assertTrue(a.all { it.startsWith("w_") })
    }
}
