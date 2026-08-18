package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.3 · Öneri B23 — günün odağı bildirimi (saf kararlar).
 *
 * Kural: öncelik yoksa bildirim yok ("dinlen" uydurulmaz);
 * günde bir kez (sabah turu tekrar çalışsa bile tekrar çıkmaz).
 */
class GunOdakBildirimTest {

    @Test
    fun `govde emojiyi tek boslukla one alir`() {
        assertEquals("💊 İlacını al", GunOdakBildirim.govde("💊", "İlacını al"))
    }

    @Test
    fun `govde emoji yoksa metni oldugu gibi verir`() {
        assertEquals("Raporu bitir", GunOdakBildirim.govde("", "Raporu bitir"))
    }

    @Test
    fun `odak yoksa bildirim cikmaz`() {
        assertFalse(GunOdakBildirim.gonderilmeli(odakVarMi = false, bugunGonderildi = false))
    }

    @Test
    fun `bugun gonderildiyse tekrar cikmaz`() {
        assertFalse(GunOdakBildirim.gonderilmeli(odakVarMi = true, bugunGonderildi = true))
    }

    @Test
    fun `odak var ve gonderilmediyse cikar`() {
        assertTrue(GunOdakBildirim.gonderilmeli(odakVarMi = true, bugunGonderildi = false))
    }
}
