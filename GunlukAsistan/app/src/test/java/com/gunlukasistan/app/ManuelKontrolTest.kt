package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * v10.47 — Kullanıcı maddesi #9: [ManuelKontrol] birim testleri (12 test).
 */
class ManuelKontrolTest {

    private val refMs: Long = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 10, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `uyanma zamani saat ve dakikayi kurar`() {
        val uyan = ManuelKontrol.uyanmaZamaniHesapla(refMs, 7, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = uyan }
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `uyanma zamani sinirlara kelepcenir`() {
        val uyan = ManuelKontrol.uyanmaZamaniHesapla(refMs, 25, 65)
        val cal = Calendar.getInstance().apply { timeInMillis = uyan }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `uyuma zamani aksam saatinde bir gun onceyi hedefler`() {
        val uyuma = ManuelKontrol.uyumaZamaniHesapla(refMs, 23, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = uyuma }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(9, cal.get(Calendar.DAY_OF_MONTH)) // 10 Ağustos -> 9 Ağustos akşamı
    }

    @Test
    fun `uyuma zamani gece yarisindan sonra ayni gunu hedefler`() {
        val uyuma = ManuelKontrol.uyumaZamaniHesapla(refMs, 1, 15)
        val cal = Calendar.getInstance().apply { timeInMillis = uyuma }
        assertEquals(1, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH)) // 10 Ağustos 01:15
    }

    @Test
    fun `uyku suresi normal uyku araliginda dogru ms dondurur`() {
        val uyudu = ManuelKontrol.uyumaZamaniHesapla(refMs, 23, 0)
        val uyandi = ManuelKontrol.uyanmaZamaniHesapla(refMs, 7, 0)
        val sure = ManuelKontrol.uykuSuresiHesapla(uyudu, uyandi)
        assertNotNull(sure)
        assertEquals(8 * 3600_000L, sure)
    }

    @Test
    fun `uyku suresi gecersiz veya cok uzun uykuda null dondurur`() {
        val uyudu = ManuelKontrol.uyumaZamaniHesapla(refMs, 23, 0)
        val uyandi = ManuelKontrol.uyanmaZamaniHesapla(refMs, 7, 0)
        assertNull(ManuelKontrol.uykuSuresiHesapla(uyandi, uyudu)) // ters saat
        assertNull(ManuelKontrol.uykuSuresiHesapla(0L, uyandi))
    }

    @Test
    fun `seri sinirla negatif degeri sifira ceker`() {
        assertEquals(0, ManuelKontrol.seriSinirla(-5))
    }

    @Test
    fun `seri sinirla 9999 ustunu tavanla sinirlar`() {
        assertEquals(9999, ManuelKontrol.seriSinirla(15000))
        assertEquals(14, ManuelKontrol.seriSinirla(14))
    }

    @Test
    fun `odak dakika sinirla gecerli eklemeleri yapar`() {
        assertEquals(45, ManuelKontrol.odakDakikaSinirla(30, 15))
        assertEquals(10, ManuelKontrol.odakDakikaSinirla(25, -15))
    }

    @Test
    fun `odak dakika sinirla 1440 ustune cikmaz`() {
        assertEquals(1440, ManuelKontrol.odakDakikaSinirla(1400, 100))
        assertEquals(0, ManuelKontrol.odakDakikaSinirla(10, -50))
    }

    @Test
    fun `gecmis gun liste yarat istenen sayida sirali gun dondurur`() {
        val list = ManuelKontrol.gecmisGunListeYarat(5, refMs)
        assertEquals(5, list.size)
        assertEquals("2026-08-10", list[0])
        assertEquals("2026-08-09", list[1])
        assertEquals("2026-08-06", list[4])
    }

    @Test
    fun `gun adi formatla tr ayinde dogru dize dondurur`() {
        val metin = ManuelKontrol.gunAdiFormatla("2026-08-10")
        assertEquals("10 Ağustos", metin)
    }
}
