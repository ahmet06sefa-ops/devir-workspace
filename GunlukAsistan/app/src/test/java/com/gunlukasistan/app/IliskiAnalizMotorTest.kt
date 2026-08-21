package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v11.63 — IliskiAnalizMotor saf birim testleri.
 */
class IliskiAnalizMotorTest {

    private fun gunler(): List<IliskiAnalizMotor.GunVerisi> = listOf(
        IliskiAnalizMotor.GunVerisi("2026-08-01", mood = 5, uykuDk = 480, kalori = 2000),
        IliskiAnalizMotor.GunVerisi("2026-08-02", mood = 4, uykuDk = 420, kalori = 1900),
        IliskiAnalizMotor.GunVerisi("2026-08-03", mood = 2, uykuDk = 360, kalori = 2200),
        IliskiAnalizMotor.GunVerisi("2026-08-04", mood = 1, uykuDk = 300, kalori = 1800)
    )

    @Test
    fun `iyi mood gunlerinin ortalama uykusu`() {
        // (480+420)/2 = 450
        assertEquals(450.0, IliskiAnalizMotor.iyiMoodOrtalamaUykuDk(gunler())!!, 0.01)
    }

    @Test
    fun `kotu mood gunlerinin ortalama uykusu`() {
        // (360+300)/2 = 330
        assertEquals(330.0, IliskiAnalizMotor.kotuMoodOrtalamaUykuDk(gunler())!!, 0.01)
    }

    @Test
    fun `uyku farki iyi gunlerde daha cok`() {
        // 450 - 330 = 120
        assertEquals(120.0, IliskiAnalizMotor.uykuFarkiDk(gunler())!!, 0.01)
    }

    @Test
    fun `yetersiz veri ile sonuc null`() {
        val tek = listOf(IliskiAnalizMotor.GunVerisi("2026-08-01", mood = null, uykuDk = 480, kalori = 2000))
        assertNull(IliskiAnalizMotor.iyiMoodOrtalamaUykuDk(tek))
        assertNull(IliskiAnalizMotor.uykuFarkiDk(tek))
    }
}
