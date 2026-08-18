package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.5 · Öneri C31 — çoklu geri sayım seçim kuralları (saf).
 *
 * `DayEvent.daysLeft` takvimden hesaplar; test damgaları göreli
 * üretildiği için cihaz tarihinden bağımsızdır.
 */
class EventsListVeriTest {

    private val bicim = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun e(id: Long, gunFark: Int, pinned: Boolean = false) = Store.DayEvent(
        id = id,
        title = "Etkinlik$id",
        dateKey = bicim.format(Date(System.currentTimeMillis() + gunFark * 86_400_000L)),
        emoji = "🎯",
        pinned = pinned,
        createdAt = 0L
    )

    @Test
    fun `gelecek etkinlikler gun sayisina gore siralanir`() {
        val liste = listOf(e(1, 30), e(2, 3), e(3, 12))
        val sec = EventsListVeri.sec(liste)
        assertEquals(listOf(2L, 3L, 1L), sec.map { it.id })
    }

    @Test
    fun `ayni gunde sabitlenmis once gelir`() {
        val liste = listOf(e(1, 7), e(2, 7, pinned = true))
        val sec = EventsListVeri.sec(liste)
        assertEquals(2L, sec.first().id)
    }

    @Test
    fun `gecmisten en fazla bir satir alinir`() {
        val liste = listOf(e(1, -1), e(2, -30), e(3, 10))
        val sec = EventsListVeri.sec(liste)
        assertEquals(2, sec.size)
        assertEquals(1L, sec.last().id) // en YAKIN geçmiş
        assertTrue(sec.none { it.id == 2L })
    }

    @Test
    fun `liste azami satirla sinirlanir`() {
        val liste = (1..9).map { e(it.toLong(), it) }
        assertEquals(6, EventsListVeri.sec(liste).size)
    }

    @Test
    fun `bugunku etkinlik en bastadir`() {
        val liste = listOf(e(1, 4), e(2, 0))
        assertEquals(2L, EventsListVeri.sec(liste).first().id)
    }
}
