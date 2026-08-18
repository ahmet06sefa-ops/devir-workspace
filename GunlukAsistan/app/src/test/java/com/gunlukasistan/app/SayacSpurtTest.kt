package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.2 · Öneri A4 — Final spurt bölge testleri.
 *
 * ── Dürüst not ──
 * Renk geçişi + nabız kadranda zaten vardı. v10.2'nin işi uyarı
 * bölgesini `max(5 dk, toplamın %10'u)` yapmak: kısa sürelerde
 * davranış aynı kalmalı, uzun oturumlarda bölge büyümeli.
 * Bu test iki ucun da kaymadığını korur.
 */
class SayacSpurtTest {

    @Test
    fun `kisa surede zaman zemini korunur`() {
        // 25 dk Pomodoro: %10 = 150 sn ama zemin 5 dk — eski davranış
        assertEquals(300L, SayacSpurt.uyariBaslangiciSn(25 * 60_000L))
    }

    @Test
    fun `uzun surede oran devreye girer`() {
        // 2 saat: %10 = 12 dk = 720 sn — zaman zeminini geçer
        assertEquals(720L, SayacSpurt.uyariBaslangiciSn(120 * 60_000L))
    }

    @Test
    fun `seviye bolgeleri dogru`() {
        val toplam = 25 * 60_000L // 25 dk → uyarı başlangıcı 300 sn
        assertEquals(0, SayacSpurt.seviye(301, toplam))   // normal bölge
        assertEquals(1, SayacSpurt.seviye(300, toplam))   // uyarı bandı başı
        assertEquals(1, SayacSpurt.seviye(61, toplam))    // hâlâ uyarı
        assertEquals(2, SayacSpurt.seviye(60, toplam))    // kritik = son 60 sn
        assertEquals(2, SayacSpurt.seviye(5, toplam))
        assertEquals(0, SayacSpurt.seviye(-1, toplam))    // bitti — artık renk yok
    }

    @Test
    fun `band orani sinirlarda`() {
        val toplam = 25 * 60_000L
        assertEquals(0f, SayacSpurt.bandOrani(400, toplam), 0.001f)   // bant dışı
        assertEquals(0f, SayacSpurt.bandOrani(300, toplam), 0.001f)   // banda giriş
        assertEquals(1f, SayacSpurt.bandOrani(60, toplam), 0.001f)    // kritik eşiği
        assertTrue(SayacSpurt.bandOrani(180, toplam) in 0.4f..0.6f)   // orta ≈ %50
    }
}
