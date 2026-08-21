package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.58 — SaglikRaporuMotor saf birim testleri.
 */
class SaglikRaporuMotorTest {

    @Test
    fun `baslik beklenen metni icerir`() {
        assertTrue(SaglikRaporuMotor.baslik().contains("Haftalık Sağlık Raporu"))
        assertTrue(SaglikRaporuMotor.baslik().contains("Günlük Asistan"))
    }

    @Test
    fun `baslik tekrari aynidir`() {
        assertEquals(SaglikRaporuMotor.baslik(), SaglikRaporuMotor.baslik())
    }
}
