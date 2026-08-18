package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** v10.30 · Katalog #31/#33 — [NotBaglant] + [NotBirlestir] testleri. */
class NotBaglantTest {

    @Test
    fun ilkUrl_httpsDogrudan() {
        assertEquals("https://ornek.com/sayfa", NotBaglant.ilkUrl("bak: https://ornek.com/sayfa sonra"))
    }

    @Test
    fun ilkUrl_wwwSemaEklenir() {
        assertEquals("https://www.ornek.com", NotBaglant.ilkUrl("site www.ornek.com burada"))
    }

    @Test
    fun ilkUrl_yoksaNull() {
        assertNull(NotBaglant.ilkUrl("sıradan bir not, bağlantısız"))
        assertNull(NotBaglant.ilkUrl(""))
    }

    @Test
    fun birlestir_govdeBoslukKurallari() {
        assertEquals("birinci\n\nikinci", NotBirlestir.govde(" birinci ", " ikinci "))
        assertEquals("tek taraf", NotBirlestir.govde("tek taraf", "   "))
        assertEquals("diğer taraf", NotBirlestir.govde("", "diğer taraf"))
        assertEquals("", NotBirlestir.govde("", " "))
    }

    @Test
    fun birlestir_baslikNoktaIle() {
        assertEquals("Alışveriş · Market", NotBirlestir.baslik("Alışveriş", "Market"))
        assertEquals("Alışveriş", NotBirlestir.baslik("Alışveriş", ""))
        assertEquals("Market", NotBirlestir.baslik("", "Market"))
    }
}
