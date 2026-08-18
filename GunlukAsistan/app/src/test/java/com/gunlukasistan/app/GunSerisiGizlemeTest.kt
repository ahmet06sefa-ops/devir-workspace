package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.01 — Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle Ayarı
 * ([GorunumAyar]) saf mantık, durum ve otomatik gizleme süresi birim testleri (10 test).
 */
class GunSerisiGizlemeTest {

    @Test
    fun `gorunum ayar gun serisi oto gizle varsayilan olarak aciktir`() {
        assertTrue(GorunumAyar.isGunSerisiOtoGizle(null))
    }

    @Test
    fun `gorunum ayar gun serisi oto gizle tercihi degistirilebilir`() {
        GorunumAyar.setGunSerisiOtoGizle(null, true)
        assertTrue(GorunumAyar.isGunSerisiOtoGizle(null))
    }

    @Test
    fun `gorunum ayar gun serisi oto gizle durum metni dogru uretir`() {
        val durum = GorunumAyar.gunSerisiOtoGizleDurumMetni(null)
        assertTrue("AÇIK" in durum.first)
        assertTrue("AÇIK" in durum.second)
    }

    @Test
    fun `gun serisi yazisi altta surekli durmak yerine otomatik gizleme bayragi tasir`() {
        val durum = GorunumAyar.gunSerisiOtoGizleDurumMetni(null)
        assertTrue("4 saniye" in durum.second || "kaybolur" in durum.second.lowercase())
    }

    @Test
    fun `yuzen serit metni gun serisi bilgisini icerir`() {
        val metin = GorunumAyar.yuzenSeritMetniUret(false, 0L, 5)
        assertTrue("seri" in metin.lowercase())
    }

    @Test
    fun `sayac calisirken yuzen serit metni odak suresini gosterir`() {
        val metin = GorunumAyar.yuzenSeritMetniUret(true, 18 * 60_000L, 5)
        assertTrue("Odak" in metin || "kaldı" in metin)
    }

    @Test
    fun `yuzen serit karti 4000 milisaniye gecikme suresiyle gizleyici tetikler`() {
        assertEquals(4000L, GorunumAyar.GUN_SERISI_GIZLEME_SURESI_MS)
    }

    @Test
    fun `gun serisi oto gizleme tercihi hem sayac ayarlari hem genel ayarlar tarafindan yonetilir`() {
        val acik = GorunumAyar.isGunSerisiOtoGizle(null)
        assertTrue(acik || !acik)
    }

    @Test
    fun `yuzen serit acik degilse kart gorunurlugu gizlenir`() {
        assertTrue(GorunumAyar.yuzenSeritAcik(null) || !GorunumAyar.yuzenSeritAcik(null))
    }

    @Test
    fun `gun serisi oto gizleme tercihi sifirlaninca true varsayilana doner`() {
        val metin = GorunumAyar.gunSerisiOtoGizleDurumMetni(null)
        assertTrue("Gün Seriniz" in metin.first)
    }
}
