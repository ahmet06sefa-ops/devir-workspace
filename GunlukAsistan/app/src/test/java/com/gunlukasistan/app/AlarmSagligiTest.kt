package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.1 — Alarm sağlığı puanlama testleri (öneri 42-44).
 *
 * ── Neden test ──
 * Puanlama kullanıcıya "bildirimlerin çalışıyor mu" cevabını
 * veriyor. Yanlış hesaplarsa iki türlü zarar var:
 *   · Sorun varken "her şey yolunda" derse → kullanıcı bildirim
 *     beklemeye devam eder, gelmez, uygulamaya güvenmez
 *   · Sorun yokken "bozuk" derse → gereksiz panik ve ayar kurcalama
 *
 * Ağırlıklar bilinçli seçildi; bu testler onları koruyor.
 */
class AlarmSagligiTest {

    private fun saglik(
        bildirim: Boolean = true,
        tamAlarm: Boolean = true,
        pil: Boolean = true,
        agresif: Boolean = false
    ) = AlarmSagligi.Saglik(
        bildirimIzni = bildirim,
        tamAlarmIzni = tamAlarm,
        pilKisitsiz = pil,
        agresifUretici = agresif,
        sonKurulumMs = 0L,
        kurulanAlarm = 0,
        sonTetikMs = 0L,
        tetikSayisi = 0
    )

    @Test
    fun `her sey yolundaysa tam puan`() {
        assertEquals(100, saglik().puan)
    }

    @Test
    fun `hicbir izin yoksa sifir puan`() {
        assertEquals(
            0,
            saglik(bildirim = false, tamAlarm = false, pil = false, agresif = true).puan
        )
    }

    @Test
    fun `bildirim izni en agir kalem`() {
        // Bildirim izni yoksa hiçbir şey çalışmaz — en yüksek ağırlık onda
        val bildirimYok = saglik(bildirim = false).puan
        val tamAlarmYok = saglik(tamAlarm = false).puan
        val pilYok = saglik(pil = false).puan

        assertTrue(
            "Bildirim izni eksikliği en çok puan düşürmeli",
            bildirimYok < tamAlarmYok && bildirimYok < pilYok
        )
        assertEquals(60, bildirimYok)
    }

    @Test
    fun `puan her zaman 0-100 arasinda`() {
        val secenekler = listOf(true, false)
        for (b in secenekler) for (t in secenekler)
            for (p in secenekler) for (a in secenekler) {
                val puan = saglik(b, t, p, a).puan
                assertTrue("Puan aralık dışı: $puan", puan in 0..100)
            }
    }

    @Test
    fun `durum metni puanla tutarli`() {
        assertEquals(R.string.as_durum_iyi, saglik().durumMetni)
        // 100 - 20 (pil) = 80 → orta
        assertEquals(R.string.as_durum_orta, saglik(pil = false).durumMetni)
        // 100 - 40 (bildirim) - 30 (alarm) = 30 → kötü
        assertEquals(
            R.string.as_durum_kotu,
            saglik(bildirim = false, tamAlarm = false).durumMetni
        )
    }

    @Test
    fun `agresif uretici tek basina iyi durumu bozmaz`() {
        // Üretici riski var ama tüm izinler tamam: 90 puan, hâlâ "iyi".
        // Gerekçe: izinler verilmişse çoğu cihazda sorun çıkmıyor;
        // kullanıcıyı gereksiz korkutmamak lazım.
        val s = saglik(agresif = true)
        assertEquals(90, s.puan)
        assertEquals(R.string.as_durum_iyi, s.durumMetni)
    }

    @Test
    fun `iki eksik varsa kotu duruma duser`() {
        // 100 - 30 - 20 = 50 → kötü eşiği (60) altında
        val s = saglik(tamAlarm = false, pil = false)
        assertEquals(50, s.puan)
        assertEquals(R.string.as_durum_kotu, s.durumMetni)
    }

    @Test
    fun `agirliklarin toplami 100`() {
        // Ağırlıklar: 40 + 30 + 20 + 10. Biri değişirse bu test uyarır.
        val tam = saglik().puan
        val bildirimAgirlik = tam - saglik(bildirim = false).puan
        val alarmAgirlik = tam - saglik(tamAlarm = false).puan
        val pilAgirlik = tam - saglik(pil = false).puan
        val ureticiAgirlik = tam - saglik(agresif = true).puan

        assertEquals(40, bildirimAgirlik)
        assertEquals(30, alarmAgirlik)
        assertEquals(20, pilAgirlik)
        assertEquals(10, ureticiAgirlik)
        assertEquals(
            100,
            bildirimAgirlik + alarmAgirlik + pilAgirlik + ureticiAgirlik
        )
    }

    // ══════════════════════════════════════════════════════════
    // Test türü sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `bildirim test turleri benzersiz`() {
        val turler = listOf(
            BildirimTestReceiver.TUR_ANINDA,
            BildirimTestReceiver.TUR_KISA,
            BildirimTestReceiver.TUR_UZUN
        )
        assertEquals("Test türleri benzersiz olmalı", turler.size, turler.toSet().size)
    }
}
