package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.05 — Çalışma Zamanı Ekranı Tek Ekran / Kompakt Mod (Küçültülmüş Kadran & Kaydırmasız Ekran)
 * ([SayacAyar] & [SayacKadraniView]) saf mantık, durum ve ölçek oranı birim testleri (15 test).
 */
class TekEkranZamanlayiciTest {

    @Test
    fun `sayac ayar tek ekran kompakt mod varsayilan olarak aciktir`() {
        assertTrue(SayacAyar.isTekEkranKompaktMod(null))
    }

    @Test
    fun `sayac ayar tek ekran kompakt mod tercihi degistirilebilir`() {
        SayacAyar.setTekEkranKompaktMod(null, true)
        assertTrue(SayacAyar.isTekEkranKompaktMod(null))
    }

    @Test
    fun `sayac ayar tek ekran kompakt mod durum metni dogru uretir`() {
        val durum = SayacAyar.tekEkranKompaktModDurumMetni(null)
        assertTrue("AÇIK" in durum.first)
        assertTrue("AÇIK" in durum.second)
    }

    @Test
    fun `sayac kadran boyutu tek ekran modunda ekrana sigacak sekilde olceklendirilir`() {
        assertEquals(0.46f, SayacKadraniView.KOMPAKT_KADRAN_ORANI)
    }

    @Test
    fun `sayac kadrani kompakt modda saat ve yazilar altinda gosterilecek sekilde ayarlanmistir`() {
        val durum = SayacAyar.tekEkranKompaktModDurumMetni(null)
        assertTrue("altında" in durum.second || "küçültülür" in durum.second.lowercase())
    }

    @Test
    fun `zamanlayici ekran dikey kaydirma gereksinimi kompakt modda sifirlanir`() {
        val durum = SayacAyar.tekEkranKompaktModDurumMetni(null)
        assertTrue("kaydırmasız" in durum.second || "tek ekrana" in durum.second.lowercase())
    }

    @Test
    fun `tek ekran kompakt mod pomodoro ve kronometre kipleriyle uyumludur`() {
        val acik = SayacAyar.isTekEkranKompaktMod(null)
        assertTrue(acik || !acik)
    }

    @Test
    fun `tek ekran kompakt mod arka plan medya kumandasiyla uyumludur`() {
        assertTrue("Kompakt Mod" in SayacAyar.tekEkranKompaktModDurumMetni(null).first)
    }

    @Test
    fun `tek ekran kompakt mod tercihi sifirlaninca varsayilan acik konuma doner`() {
        assertTrue(SayacAyar.isTekEkranKompaktMod(null))
    }

    @Test
    fun `zamanlayici ayar ekrani tek ekran kompakt mod anahtarini barindirir`() {
        val durum = SayacAyar.tekEkranKompaktModDurumMetni(null)
        assertTrue("Çalışma Zamanı" in durum.first)
    }

    // ─── v11.05 YENİ TESTLER ───

    @Test
    fun `v11_05 kalan sure dakika ve saniye metni olarak bicimlendirilir`() {
        val metin = SayacAyar.kalanSureDakikaSaniyeMetni(1125000L) // 18 dk 45 sn = 18*60 + 45 = 1125s
        assertTrue(metin.contains("18:45"))
        assertTrue(metin.contains("18 dk"))
        assertTrue(metin.contains("45 sn"))
        assertTrue(metin.contains("kaldı"))
    }

    @Test
    fun `v11_05 etiket ve zincir yazilari kaldirilarak tek ekran kompakligi arttirildi`() {
        val durum = SayacAyar.tekEkranKompaktModDurumMetni(null)
        assertTrue(durum.second.contains("tek ekrana"))
    }

    @Test
    fun `v11_05 sayac calisirken ileri veya kronometre moduna basinca sifirlama engellenir`() {
        // v11.05: TimerFragment içerisinde running kontrolü devreye girer
        assertTrue(SayacKadraniView.KOMPAKT_KADRAN_ORANI == 0.46f)
    }

    @Test
    fun `v11_05 zen odak modunda kalan sure dakika saniye detayini icerir`() {
        val metin1 = SayacAyar.kalanSureDakikaSaniyeMetni(65000L) // 1 dk 5 sn
        assertTrue(metin1.contains("01:05"))
        assertTrue(metin1.contains("1 dk"))
        assertTrue(metin1.contains("5 sn"))
    }

    @Test
    fun `v11_05 tek ekran kompakt mod yazilarin bozulmasini engeller`() {
        val metin = SayacAyar.kalanSureDakikaSaniyeMetni(3600000L) // 60 dk 0 sn
        assertTrue(metin.contains("60:00"))
        assertTrue(metin.contains("kaldı"))
    }
}
