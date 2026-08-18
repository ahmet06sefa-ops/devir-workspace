package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v10.38 · Katalog #13 — [GorevBugunUstte] saf mantık testleri. */
class GorevBugunUstteTest {

    @Test
    fun `bugun bitecek ve gecmis ustte`() {
        val (b0, b1) = GorevBugunUstte.bugunAraligi(System.currentTimeMillis())
        assertEquals(0, GorevBugunUstte.oncelik(b0 + 1000, b1))       // bugün içi
        assertEquals(0, GorevBugunUstte.oncelik(b0 - 3_600_000, b1))  // gecikmiş
    }

    @Test
    fun `tarihsiz ve gelecek geride`() {
        val (_, b1) = GorevBugunUstte.bugunAraligi(System.currentTimeMillis())
        assertEquals(1, GorevBugunUstte.oncelik(0L, b1))                    // tarihsiz
        assertEquals(1, GorevBugunUstte.oncelik(b1 + 3_600_000, b1))        // gelecek
        assertEquals(1, GorevBugunUstte.oncelik(Long.MAX_VALUE, b1))        // uç değer
    }

    @Test
    fun `bugun araligi tam bir gun`() {
        val simdi = System.currentTimeMillis()
        val (b0, b1) = GorevBugunUstte.bugunAraligi(simdi)
        assertEquals(86_400_000L, b1 - b0)
        assertTrue(simdi >= b0 && simdi < b1)
    }
}
