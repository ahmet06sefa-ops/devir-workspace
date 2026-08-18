package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.27 (öneri #22) — Not sabitleme sıralaması.
 * Kararlılık kritik: sabitsizler girdi sırasını (yeniden eskiye) korur.
 */
class NotSabitleTest {

    private data class N(val id: Long)

    @Test
    fun `sabitOnce - sabitliler basa digerleri sirali kalir`() {
        val liste = listOf(N(5), N(4), N(3), N(2), N(1)) // yeniden eskiye
        val sonuc = NotSabitle.sabitOnce(liste, setOf(2L, 4L)) { it.id }
        assertEquals(listOf(4L, 2L, 5L, 3L, 1L), sonuc.map { it.id })
    }

    @Test
    fun `sabitOnce - bos ve pinsiz durum ayni liste`() {
        val liste = listOf(N(3), N(2), N(1))
        assertEquals(listOf(3L, 2L, 1L), NotSabitle.sabitOnce(liste, emptySet()) { it.id }.map { it.id })
        assertEquals(
            emptyList<N>(),
            NotSabitle.sabitOnce<N>(emptyList(), setOf(1L)) { it.id }
        )
    }

    @Test
    fun `sabitOnce - hepsi sabitliyse girdi sirasi korunur`() {
        val liste = listOf(N(9), N(7), N(1))
        val sonuc = NotSabitle.sabitOnce(liste, setOf(9L, 7L, 1L)) { it.id }
        assertEquals(listOf(9L, 7L, 1L), sonuc.map { it.id })
    }
}
