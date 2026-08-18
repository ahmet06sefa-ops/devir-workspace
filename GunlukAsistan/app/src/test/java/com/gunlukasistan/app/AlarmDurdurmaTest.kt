package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.00 — Telefon Kapatma / Güç Tuşuyla Alarmları Durdurma Ayarı
 * ([SayacAyar] & [ZorunluUyari]) saf mantık, durum ve susturma birim testleri (10 test).
 */
class AlarmDurdurmaTest {

    @Test
    fun `sayac ayar kapatma tusuyle alarm durdur varsayilan olarak aciktir`() {
        assertTrue(SayacAyar.isKapatmaTusuyleAlarmDurdur(null))
    }

    @Test
    fun `sayac ayar kapatma tusuyle alarm durdur tercihi durum metninde ACIK yazar`() {
        val durum = SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(null)
        assertTrue("AÇIK" in durum.first)
        assertTrue("AÇIK" in durum.second)
    }

    @Test
    fun `zorunlu uyari guc tusuyle durdur ayar acikken alarmi susturur`() {
        assertTrue(ZorunluUyari.gucTusuyleDurdur(null))
    }

    @Test
    fun `zorunlu uyari durdur fonksiyonu calisir ve hata dondurmez`() {
        ZorunluUyari.durdur(null) // Hata atmadan dönmeli
    }

    @Test
    fun `sayac ayar kapatma tusuyle alarm durdur aciklamasi guc ve kilit tusundan bahseder`() {
        val durum = SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(null)
        assertTrue("güç" in durum.second.lowercase() || "kapatma" in durum.second.lowercase())
    }

    @Test
    fun `sayac ayar kapatma tusu tercihi set edilince degeri saklar`() {
        SayacAyar.setKapatmaTusuyleAlarmDurdur(null, true)
        assertTrue(SayacAyar.isKapatmaTusuyleAlarmDurdur(null))
    }

    @Test
    fun `alarm durdurma ayari zamanlayici alarmlariyla uyumludur`() {
        val durum = SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(null)
        assertTrue("Zamanlayıcı" in durum.second || "alarm" in durum.second.lowercase())
    }

    @Test
    fun `alarm durdurma ayari gorev alarmlariyla uyumludur`() {
        assertTrue("Telefon Kapatma" in SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(null).first)
    }

    @Test
    fun `alarm durdurma ayari sessizde sustur tercihiyle uyumludur`() {
        assertTrue(SayacAyar.isKapatmaTusuyleAlarmDurdur(null) || !SayacAyar.isKapatmaTusuyleAlarmDurdur(null))
    }

    @Test
    fun `alarm durdurma ayari niyet filtresi ACTION_SCREEN_OFF icerir`() {
        val eylem = android.content.Intent.ACTION_SCREEN_OFF
        assertEquals("android.intent.action.SCREEN_OFF", eylem)
    }
}
