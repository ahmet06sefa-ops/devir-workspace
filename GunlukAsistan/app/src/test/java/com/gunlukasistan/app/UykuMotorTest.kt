package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v11.55 — UykuMotor saf birim testleri.
 */
class UykuMotorTest {

    @Test
    fun `sure metni dakikayi saat ve dk bicimine cevirir`() {
        assertEquals("7 sa 30 dk", UykuMotor.sureMetni(450))
        assertEquals("7 sa", UykuMotor.sureMetni(420))
        assertEquals("45 dk", UykuMotor.sureMetni(45))
    }

    @Test
    fun `kalite etiketi dogru`() {
        assertEquals("Mükemmel", UykuMotor.kaliteEtiketi(5))
        assertEquals("İyi", UykuMotor.kaliteEtiketi(4))
        assertEquals("Normal", UykuMotor.kaliteEtiketi(3))
        assertEquals("Kötü", UykuMotor.kaliteEtiketi(2))
        assertEquals("Çok kötü", UykuMotor.kaliteEtiketi(1))
    }

    @Test
    fun `gun anahtari tarih biciminde`() {
        assert(UykuMotor.gunAnahtari(0L).matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}
