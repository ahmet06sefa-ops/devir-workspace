package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v10.5 · Öneri C27 — widget hazır ayar çipleri (saf).
 *
 * Liste uygulama içi çiplerle aynı tutulur; iki yüzeyin farklı
 * önermesi "hangisi doğru?" karmaşası yaratır.
 */
class SayacPresetTest {

    @Test
    fun `uc cip uygulama ici hazir ayarlarla ayni`() {
        assertEquals(listOf(5, 15, 25), SayacPreset.PRESETLER)
    }

    @Test
    fun `sinir disi indeks guvenle null verir`() {
        assertNull(SayacPreset.dakika(3))
        assertNull(SayacPreset.dakika(-1))
        assertEquals(5, SayacPreset.dakika(0))
    }

    @Test
    fun `cip etiketi dakika birimini tasir`() {
        assertEquals("15 dk", SayacPreset.etiket(15))
    }
}
