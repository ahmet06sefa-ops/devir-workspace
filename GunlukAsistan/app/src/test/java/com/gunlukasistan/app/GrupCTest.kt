package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.15 · ULTRA-30 GRUP C — saf çekirdek testleri.
 * KritikAlarm (C13) · SessizTurler (C14) · HatirlaticiDemeti (C18)
 * OgrenenHatirlatici (C16) · RaporIsi (C17)
 */
class GrupCTest {

    // ── C13 · KritikAlarm ──────────────────────────────────────────

    @Test
    fun `kritik erteleme tablosu kademeli`() {
        assertEquals(0, KritikAlarm.ertelemeDakikasi(0))
        assertEquals(5, KritikAlarm.ertelemeDakikasi(1))
        assertEquals(10, KritikAlarm.ertelemeDakikasi(2))
        assertEquals(15, KritikAlarm.ertelemeDakikasi(3))
        assertEquals(0, KritikAlarm.ertelemeDakikasi(4)) // maks aşımı: bedel yolu
        assertEquals(0, KritikAlarm.ertelemeDakikasi(-3))
        assertEquals(30, KritikAlarm.TOPLAM_TAVAN_DK)
        assertEquals(3, KritikAlarm.MAKS_ERTELEME)
    }

    @Test
    fun `kritik bedel ve sonraki alarm`() {
        assertFalse(KritikAlarm.bedelGerekliMi(3))
        assertTrue(KritikAlarm.bedelGerekliMi(4))
        // normal: simdi + kademe
        assertEquals(1_000L + 5 * 60_000L, KritikAlarm.sonrakiUyariMs(1, 1_000L, 0L))
        // bedel: ertesi gün 09:00 (gün başı ms=0 verildi)
        assertEquals(
            86_400_000L + 540 * 60_000L,
            KritikAlarm.sonrakiUyariMs(4, 1_000L, 0L)
        )
    }

    // ── C14 · SessizTurler ─────────────────────────────────────────

    @Test
    fun `sessiz pencere gece sarmasi`() {
        // 23:00–08:00
        assertTrue(SessizTurler.sessizdeMi(1400, 1380, 480))
        assertTrue(SessizTurler.sessizdeMi(0, 1380, 480))
        assertTrue(SessizTurler.sessizdeMi(479, 1380, 480))
        assertFalse(SessizTurler.sessizdeMi(480, 1380, 480))
        assertFalse(SessizTurler.sessizdeMi(1300, 1380, 480))
        // gün içi 08:00–12:00
        assertTrue(SessizTurler.sessizdeMi(600, 480, 720))
        assertFalse(SessizTurler.sessizdeMi(470, 480, 720))
        // bas==bit → pencere yok
        assertFalse(SessizTurler.sessizdeMi(700, 720, 720))
        // negatif dakika normalize edilir (-60 → 23:00)
        assertTrue(SessizTurler.sessizdeMi(-60, 1380, 480))
    }

    @Test
    fun `tur karari global ve hafta sonu ayrimi`() {
        val g = 1380 to 480 // global 23–08
        // tür kapalı → global karar devralınır
        assertTrue(
            SessizTurler.turSessizdeMi(
                SessizTurler.Pencere(acik = false), 1400, false, g.first, g.second
            )
        )
        // tür açık → kendi penceresi karar verir (global geçersiz)
        val p = SessizTurler.Pencere(acik = true, bas = 480, bit = 720)
        assertFalse(SessizTurler.turSessizdeMi(p, 1400, false, g.first, g.second))
        assertTrue(SessizTurler.turSessizdeMi(p, 600, false, g.first, g.second))
        // hafta sonu ayrımı: hs penceresi 00–10
        val hs = p.copy(haftaSonuAyrimi = true, hsBas = 0, hsBit = 600)
        assertTrue(SessizTurler.turSessizdeMi(hs, 660, false, g.first, g.second))  // hafta içi pencere
        assertFalse(SessizTurler.turSessizdeMi(hs, 660, true, g.first, g.second))   // hs pencerede değil
        assertTrue(SessizTurler.turSessizdeMi(hs, 500, true, g.first, g.second))    // hs pencerede
    }

    @Test
    fun `kanal tur eslemesi`() {
        assertEquals(SessizTurler.Tur.GOREV, SessizTurler.kanaldanTur("gorev_hatirlatici_v1"))
        assertEquals(SessizTurler.Tur.GOREV, SessizTurler.kanaldanTur("ch_hatirlatici_v2"))
        assertEquals(SessizTurler.Tur.SAYAC, SessizTurler.kanaldanTur("ch_zaman_bitis_v2"))
        assertEquals(SessizTurler.Tur.RAPOR, SessizTurler.kanaldanTur("ch_rapor_v2"))
        assertEquals(SessizTurler.Tur.MOTIVASYON, SessizTurler.kanaldanTur("ch_motivasyon_v2"))
        assertEquals(SessizTurler.Tur.DIGER, SessizTurler.kanaldanTur("ch_arkaplan_v2"))
        assertEquals(SessizTurler.Tur.DIGER, SessizTurler.kanaldanTur(null))
    }

    // ── C18 · HatirlaticiDemeti ────────────────────────────────────

    private data class Aday(val id: Long, val v: Long, val bitti: Boolean)

    @Test
    fun `demet pencere sirasi ve eleme`() {
        val t = 1_000_000L // pencere ±600_000 ms (10 dk): [400_000, 1_600_000]
        val adaylar = listOf(
            Aday(1, 1_000_000, false), // merkez
            Aday(2, 1_600_000, false), // üst kenar (dahil, fark tam 10 dk)
            Aday(3, 399_999, false),   // pencere DIŞI (fark 600_001)
            Aday(4, 1_600_001, false), // pencere DIŞI (fark 600_001)
            Aday(5, 500_000, true),    // tamamlanmış
            Aday(6, 1_100_000, false), // dahil
            Aday(7, 0, false),         // vadesiz
        )
        val d = HatirlaticiDemeti.demetKur(adaylar, t, { it.v }, { it.bitti })
        assertEquals(listOf(1L, 6L, 2L), d.map { it.id }) // vade sırası artan
        assertTrue(HatirlaticiDemeti.demetMi(d.size))
        assertFalse(HatirlaticiDemeti.demetMi(1))
        assertEquals("+3 görev daha aynı dilimde", HatirlaticiDemeti.tasmaMetni(3))
        val satir = HatirlaticiDemeti.satirMetni(t, "Rapor")
        assertTrue(satir.startsWith("⏰ ") && satir.endsWith(" — Rapor"))
    }

    // ── C16 · OgrenenHatirlatici ───────────────────────────────────

    @Test
    fun `dairesel ortalama sarmasi ve bos`() {
        assertEquals(-1, OgrenenHatirlatici.daireselOrtalama(emptyList()))
        assertEquals(720, OgrenenHatirlatici.daireselOrtalama(listOf(720)))
        // 23:50 + 00:10 → gece yarısı (doğrusal ortalama saçmalardı)
        val ort = OgrenenHatirlatici.daireselOrtalama(listOf(1430, 10))
        assertEquals(0, ort)
        // tek noktaya toplanmış kayıtlar → tam tutarlılık
        assertEquals(1.0, OgrenenHatirlatici.tutarlilik(listOf(600, 600, 600)), 0.001)
    }

    @Test
    fun `kaydirim kelepce ve sarma yonu`() {
        // ort 11:40, hedef 11:30 → +10 dk
        assertEquals(10, OgrenenHatirlatici.kaydirimDk(700, 690))
        // uzak fark kelepçelenir: ort 10:00, hedef 15:00 → -45 (tavan)
        assertEquals(-45, OgrenenHatirlatici.kaydirimDk(600, 900))
        // gece yarısı sarması: ort 00:10, hedef 23:50 → en kısa yön +20
        assertEquals(20, OgrenenHatirlatici.kaydirimDk(10, 1430))
        // ters sarma: ort 23:50, hedef 00:10 → -20
        assertEquals(-20, OgrenenHatirlatici.kaydirimDk(1430, 10))
    }

    @Test
    fun `karar veri yetersizse ve daginiksa kaydirmaz`() {
        assertFalse(OgrenenHatirlatici.kaydirmaliMi(listOf(600, 610)))        // az kayıt
        assertTrue(OgrenenHatirlatici.kaydirmaliMi(listOf(600, 610, 620)))     // düzenli
        assertFalse(OgrenenHatirlatici.kaydirmaliMi(listOf(0, 360, 720, 1080))) // dağınık
        val k = OgrenenHatirlatici.kararVer(listOf(600, 610, 620), 600)
        assertTrue(k.uygulandi); assertEquals(10, k.kaydirim); assertEquals(3, k.adet)
        assertFalse(OgrenenHatirlatici.kararVer(listOf(600), 600).uygulandi)
        assertFalse(OgrenenHatirlatici.kararVer(listOf(600, 610, 620), 610).uygulandi) // k=0
    }

    // ── C17 · RaporIsi ─────────────────────────────────────────────

    private fun ms(gun: Int, saat: Int, dk: Int = 0): Long =
        java.util.Calendar.getInstance().apply {
            set(2026, 7, gun, saat, dk, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `isi matrisi gun dilim ve eleme`() {
        // Hafta: 3 Ağu 2026 Pzt — simdi Çar 12:00
        val simdi = ms(5, 12)
        val kayitlar = listOf(
            SureAnalizi.PomodoroKayit(ms(3, 9, 30), 50, true, 9, 0),   // Pzt 08-12
            SureAnalizi.PomodoroKayit(ms(3, 10, 30), 25, true, 10, 0), // aynı hücre
            SureAnalizi.PomodoroKayit(ms(7, 20), 40, true, 20, 0),     // Cum 20-24
            SureAnalizi.PomodoroKayit(ms(3, 11), 77, false, 11, 0),    // tamamlanmamış
            SureAnalizi.PomodoroKayit(ms(27, 9), 999, true, 9, 0), // 27 Ağu: hafta penceresi DIŞI
        )
        val m = RaporIsi.gunSaatMatrisi(kayitlar, simdi)
        assertEquals(75, m[0][2]) // Pzt × 08-12
        assertEquals(40, m[4][5]) // Cum × 20-24
        assertEquals(75, RaporIsi.matrisMaks(m))
        var toplam = 0
        m.forEach { r -> r.forEach { toplam += it } }
        assertEquals(115, toplam) // elenenler (77 + 999) toplama girmez
    }

    @Test
    fun `isi hucre kademe olcegi`() {
        assertEquals(0, RaporIsi.hucreKademe(0, 75))
        assertEquals(0, RaporIsi.hucreKademe(10, 0)) // maks yok
        assertEquals(1, RaporIsi.hucreKademe(25, 75))
        assertEquals(2, RaporIsi.hucreKademe(50, 75))
        assertEquals(3, RaporIsi.hucreKademe(60, 75))
        assertEquals(4, RaporIsi.hucreKademe(75, 75)) // maks hücre en sıcak
        assertEquals(4, RaporIsi.hucreKademe(90, 75)) // taşma kelepçe
    }
}
