package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v11.57 — MoodMotor saf birim testleri.
 */
class MoodMotorTest {

    @Test
    fun `emoji puana gore dogru`() {
        assertEquals("😄", MoodMotor.emoji(5))
        assertEquals("🙂", MoodMotor.emoji(4))
        assertEquals("😐", MoodMotor.emoji(3))
        assertEquals("😟", MoodMotor.emoji(2))
        assertEquals("😞", MoodMotor.emoji(1))
    }

    @Test
    fun `etiket puana gore dogru`() {
        assertEquals("Çok iyi", MoodMotor.etiket(5))
        assertEquals("İyi", MoodMotor.etiket(4))
        assertEquals("Normal", MoodMotor.etiket(3))
        assertEquals("Kötü", MoodMotor.etiket(2))
        assertEquals("Çok kötü", MoodMotor.etiket(1))
    }

    @Test
    fun `emoji sinir disi puani guvenle coerce eder`() {
        assertEquals("😞", MoodMotor.emoji(0))
        assertEquals("😄", MoodMotor.emoji(9))
    }

    @Test
    fun `gun anahtari tarih biciminde`() {
        assert(MoodMotor.gunAnahtari(0L).matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}
