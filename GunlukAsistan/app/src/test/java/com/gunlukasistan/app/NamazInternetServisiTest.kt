package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v11.13 — Namaz internet servisi saf testleri.
 * Aladhan API yanıt ayrıştırması ve tarih anahtarı (ağ çağrısı yok — saf).
 */
class NamazInternetServisiTest {

    @Test
    fun `tarih anahtari DD-MM-YYYY bicimindedir`() {
        val a = NamazInternetServisi.tarihAnahtari(java.util.Date(0L))
        // 01-01-1970 (epoch 0, UTC)
        assertEquals("01-01-1970", a)
    }

    @Test
    fun `sonucu coz gecerli api yanitini kayita cevirir`() {
        val yanit = """{"data":{"timings":{"Fajr":"05:12","Sunrise":"06:40","Dhuhr":"12:55","Asr":"16:22","Maghrib":"19:00","Isha":"20:15"}}}"""
        val kayit = NamazInternetServisi.sonucuCoz(yanit)
        assertNotNull(kayit)
        assertEquals("05:12", kayit!!.imsak)
        assertEquals("06:40", kayit.gunes)
        assertEquals("12:55", kayit.ogle)
        assertEquals("16:22", kayit.ikindi)
        assertEquals("19:00", kayit.aksam)
        assertEquals("20:15", kayit.yatsi)
    }

    @Test
    fun `sonucu coz saat yaninda isaret varsa isareti ayiklar`() {
        val yanit = """{"data":{"timings":{"Fajr":"05:12 (DTS)","Sunrise":"06:40","Dhuhr":"12:55","Asr":"16:22","Maghrib":"19:00","Isha":"20:15"}}}"""
        val kayit = NamazInternetServisi.sonucuCoz(yanit)
        assertNotNull(kayit)
        assertEquals("05:12", kayit!!.imsak)
    }

    @Test
    fun `sonucu coz eksik veya gecersiz yanit icin null doner`() {
        assertNull(NamazInternetServisi.sonucuCoz(""))
        assertNull(NamazInternetServisi.sonucuCoz("not json"))
        assertNull(NamazInternetServisi.sonucuCoz("""{"data":{}}"""))
        assertNull(NamazInternetServisi.sonucuCoz("""{"data":{"timings":{}}}"""))
    }
}
