package com.gunlukasistan.app
import org.junit.Assert.assertEquals
import org.junit.Test
/** v11.25 — HabitGenius kalıcılık katmanı saf JVM testleri. */
class HabitGeniusVeriTest {
    @Test fun `su sayaci alt sinir`() { assertEquals(0, HabitGeniusVeri.suSayaciSinirla(-5)) }
    @Test fun `su sayaci ust sinir`() { assertEquals(99, HabitGeniusVeri.suSayaciSinirla(500)) }
    @Test fun `ilerleme tipi alt sinir`() { assertEquals(0, HabitGeniusVeri.tipSinirla(-1)) }
    @Test fun `ilerleme tipi ust sinir`() { assertEquals(4, HabitGeniusVeri.tipSinirla(9)) }
    @Test fun `siklik alt sinir`() { assertEquals(0, HabitGeniusVeri.siklikSinirla(-1)) }
    @Test fun `siklik ust sinir`() { assertEquals(5, HabitGeniusVeri.siklikSinirla(9)) }
    @Test fun `vurgu alt sinir`() { assertEquals(0, HabitGeniusVeri.vurguSinirla(-1)) }
    @Test fun `vurgu ust sinir`() { assertEquals(19, HabitGeniusVeri.vurguSinirla(50)) }
}
