package com.gunlukasistan.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.6 · Öneri D46 — ayar arama eşleme kuralları (saf).
 *
 * Türkçe I/İ katlaması bu aramanın kalbi: yanlış yerelde "BİLDİRİM"
 * sorgusu "bildirim" satırını ıskalıyordu.
 */
class AyarAraTest {

    @Test
    fun `turkce buyuk I kucuk uya katlanir`() {
        assertTrue(AyarAra.normal("BILDIRIM").startsWith("bıldırım".first()))
        assertTrue(AyarAra.eslesme("BILDIRIM", "bildirim ayarları"))
    }

    @Test
    fun `turkce İ kucuk iye katlanir`() {
        assertTrue(AyarAra.eslesme("İLERLEME", "ilerleme grafiği"))
    }

    @Test
    fun `cok sozcuklu sorgu her sozcugu arar`() {
        assertTrue(AyarAra.eslesme("bild ses", "Bildirimler · ses ve titreşim"))
        assertFalse(AyarAra.eslesme("bild video", "Bildirimler · ses ve titreşim"))
    }

    @Test
    fun `bos sorgu her seyle eslesir`() {
        assertTrue(AyarAra.eslesme("", "herhangi bir satır"))
        assertTrue(AyarAra.eslesme("   ", "herhangi bir satır"))
    }

    @Test
    fun `alakasiz sorgu eslesmez`() {
        assertFalse(AyarAra.eslesme("namaz", "Tema rengi"))
    }
}
