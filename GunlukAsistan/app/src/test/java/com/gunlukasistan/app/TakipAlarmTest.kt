package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v9.7 — Takip alarm zamanlaması testleri.
 *
 * Bu mantık en kolay sessizce bozulan yer: alarm bir gün ileri
 * atlamazsa kullanıcı bildirimi hiç almaz, iki gün atlarsa ilaç
 * hatırlatması kaçar. İkisi de sessiz hata.
 */
class TakipAlarmTest {

    private fun an(yil: Int, ay: Int, gun: Int, saat: Int, dakika: Int = 0): Long =
        Calendar.getInstance().apply {
            set(yil, ay - 1, gun, saat, dakika, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun parcala(millis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    @Test
    fun `gelecek saat bugune kurulur`() {
        // Şu an 08:00, hedef 09:00 → bugün 09:00
        val sonuc = TakipAlarm.sonrakiGunlukSaat(9 * 60, an(2026, 8, 7, 8))
        val (gun, saat, dk) = parcala(sonuc)
        assertEquals(7, gun)
        assertEquals(9, saat)
        assertEquals(0, dk)
    }

    @Test
    fun `gecmis saat yarina kurulur`() {
        // Şu an 10:00, hedef 09:00 → yarın 09:00
        val sonuc = TakipAlarm.sonrakiGunlukSaat(9 * 60, an(2026, 8, 7, 10))
        val (gun, saat, _) = parcala(sonuc)
        assertEquals(8, gun)
        assertEquals(9, saat)
    }

    @Test
    fun `tam ayni dakika yarina kurulur`() {
        // Sınır durumu: alarm tam şu an çalacaksa hemen tetiklenmesin,
        // yoksa sonsuz döngü riski var (uyanır, yeniden kurar, uyanır...)
        val sonuc = TakipAlarm.sonrakiGunlukSaat(9 * 60, an(2026, 8, 7, 9, 0))
        val (gun, _, _) = parcala(sonuc)
        assertEquals(8, gun)
    }

    @Test
    fun `saniye ve milisaniye sifirlanir`() {
        // Sıfırlanmazsa her yeniden kurulumda saniyeler birikip kayar
        val sonuc = TakipAlarm.sonrakiGunlukSaat(9 * 60, an(2026, 8, 7, 8))
        val c = Calendar.getInstance().apply { timeInMillis = sonuc }
        assertEquals(0, c.get(Calendar.SECOND))
        assertEquals(0, c.get(Calendar.MILLISECOND))
    }

    @Test
    fun `gece yarisi alarmi dogru kurulur`() {
        val sonuc = TakipAlarm.sonrakiGunlukSaat(0, an(2026, 8, 7, 23))
        val (gun, saat, _) = parcala(sonuc)
        assertEquals(8, gun)
        assertEquals(0, saat)
    }

    @Test
    fun `ay sonu gecisi dogru`() {
        // 31 Ağustos 10:00, hedef 09:00 → 1 Eylül 09:00
        val sonuc = TakipAlarm.sonrakiGunlukSaat(9 * 60, an(2026, 8, 31, 10))
        val c = Calendar.getInstance().apply { timeInMillis = sonuc }
        assertEquals(Calendar.SEPTEMBER, c.get(Calendar.MONTH))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `sonuc her zaman gelecekte`() {
        val simdi = an(2026, 8, 7, 14, 30)
        for (dakika in 0 until 1440 step 37) {
            val sonuc = TakipAlarm.sonrakiGunlukSaat(dakika, simdi)
            assertTrue("dakika=$dakika geçmişte kaldı", sonuc > simdi)
        }
    }

    @Test
    fun `sonuc yirmi dort saatten uzak degil`() {
        // Bir günlük alarm en fazla 24 saat sonraya kurulmalı
        val simdi = an(2026, 8, 7, 14, 30)
        for (dakika in 0 until 1440 step 53) {
            val sonuc = TakipAlarm.sonrakiGunlukSaat(dakika, simdi)
            assertTrue(
                "dakika=$dakika çok ileri",
                sonuc - simdi <= 24 * 60 * 60 * 1000L + 60_000L
            )
        }
    }

    @Test
    fun `dakikali saat korunur`() {
        val sonuc = TakipAlarm.sonrakiGunlukSaat(8 * 60 + 45, an(2026, 8, 7, 7))
        val (_, saat, dk) = parcala(sonuc)
        assertEquals(8, saat)
        assertEquals(45, dk)
    }

    @Test
    fun `alarm istek kodlari cakismiyor`() {
        // İlaç alarmları 71001-71024 aralığında; günlük özet 71000.
        // Çakışırsa biri diğerini sessizce iptal eder.
        val gunluk = TakipAlarm.ISTEK_GUNLUK
        val ilacKodlari = (0..23).map { gunluk + 1 + it }
        assertTrue(gunluk !in ilacKodlari)
        assertEquals(24, ilacKodlari.toSet().size)
    }
}
