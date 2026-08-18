package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.21 · Liste satır filtreleri + dokunma hedefi — saf mantık testleri.
 *
 * Görev seçimi saat-enjekte edilmeli (bugunSonuMs) — cihaz tarihinden
 * bağımsız çalışır. Geri sayım seçimi DayEvent'in güne göreli damgalarıyla
 * test edilir (mevcut EventsListVeriTest kalıbı).
 */
class WidgetListeTest {

    // ── Görev listesi ──

    private fun g(id: Long, done: Boolean = false, due: Long = 0L, created: Long = 0L) =
        Store.Task(id = id, text = "Görev$id", done = done, createdAt = created, dueAt = due)

    private val BUGUN_SONU = 1_000_000L   // sabit damga (saat enjeksiyonu)

    // t1: tarihsiz (eski) · t2: bugüne tarihli · t3: tamamlanmış bugüne
    // t4: yarın (ileri) · t5: tarihsiz (yeni)
    private val havuz = listOf(
        g(1, due = 0L, created = 100L),
        g(2, due = 500_000L),
        g(3, done = true, due = 600_000L),
        g(4, due = 2_000_000L),
        g(5, due = 0L, created = 900L)
    )

    @Test
    fun gorevleriSec_varsayilanEskiDavranis() {
        val s = WidgetListe.gorevleriSec(havuz, false, true, true, 40, BUGUN_SONU)
        // Tamamlanan (t3) elenir; kova düzeni: bugün → tarihsiz(yeni→eski) → ileri
        assertEquals(listOf(2L, 5L, 1L, 4L), s.map { it.id })
    }

    @Test
    fun gorevleriSec_bitenleriDahilEder() {
        val s = WidgetListe.gorevleriSec(havuz, true, true, true, 40, BUGUN_SONU)
        assertTrue(s.any { it.id == 3L })
        // bugün kovası saate göre: t2 (500k) önce, t3 (600k) sonra
        assertEquals(listOf(2L, 3L), s.take(2).map { it.id })
    }

    @Test
    fun gorevleriSec_tarihsizVeIleriKovalarIndirgenir() {
        assertEquals(
            listOf(2L, 4L),
            WidgetListe.gorevleriSec(havuz, false, false, true, 40, BUGUN_SONU).map { it.id }
        )
        assertEquals(
            listOf(2L, 5L, 1L),
            WidgetListe.gorevleriSec(havuz, false, true, false, 40, BUGUN_SONU).map { it.id }
        )
    }

    @Test
    fun gorevleriSec_limitVeTeknikTaban() {
        assertEquals(
            listOf(2L, 5L),
            WidgetListe.gorevleriSec(havuz, false, true, true, 2, BUGUN_SONU).map { it.id }
        )
        // limit 0 yazılsa bile teknik taban 1 (boş widget önlenir)
        assertEquals(1, WidgetListe.gorevleriSec(havuz, false, true, true, 0, BUGUN_SONU).size)
    }

    // ── Geri sayım listesi ──

    private val bicim = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun e(id: Long, gunFark: Int, pinned: Boolean = false) = Store.DayEvent(
        id = id, title = "E$id",
        dateKey = bicim.format(Date(System.currentTimeMillis() + gunFark * 86_400_000L)),
        emoji = "🎯", pinned = pinned, createdAt = 0L
    )

    @Test
    fun geriSayim_varsayilanEskiyleBirebir() {
        val liste = listOf(e(1, 30), e(2, 3), e(3, 12), e(4, -2))
        assertEquals(
            EventsListVeri.sec(liste),
            EventsListVeri.sec(liste, gecmisiDahil = true, yalnizSabit = false, limit = 6)
        )
    }

    @Test
    fun geriSayim_gecmisKapatilabilir() {
        val liste = listOf(e(1, 3), e(2, -1))
        val s = EventsListVeri.sec(liste, gecmisiDahil = false, yalnizSabit = false, limit = 6)
        assertEquals(listOf(1L), s.map { it.id })
    }

    @Test
    fun geriSayim_yalnizSabitlenenler() {
        val liste = listOf(e(1, 3), e(2, 5, pinned = true), e(3, -1))
        val s = EventsListVeri.sec(liste, gecmisiDahil = true, yalnizSabit = true, limit = 6)
        assertEquals(listOf(2L), s.map { it.id })
    }

    @Test
    fun geriSayim_serbestSatirLimiti() {
        val liste = listOf(e(1, 1), e(2, 2), e(3, 3), e(4, 4))
        assertEquals(2, EventsListVeri.sec(liste, true, false, 2).size)
        assertEquals(4, EventsListVeri.sec(liste, true, false, 50).size)
        // limit 0 → teknik taban 1
        assertEquals(1, EventsListVeri.sec(liste, true, false, 0).size)
    }

    // ── Dokunma hedefi ──

    @Test
    fun dokunmaHedefi_gecerlilikHaritasi() {
        assertTrue(WidgetDokunma.gecerliMi(0))
        assertTrue(WidgetDokunma.gecerliMi(4))
        assertTrue(WidgetDokunma.gecerliMi(16))
        assertFalse(WidgetDokunma.gecerliMi(-1))
        assertFalse(WidgetDokunma.gecerliMi(7))   // Ayarlar bilinçli yok
        assertFalse(WidgetDokunma.gecerliMi(99))
        assertEquals(12, WidgetDokunma.EKRANLAR.size)
        // Her sekmenin ad kaynağı var (UI boş isimle karşılaşmaz)
        assertTrue(WidgetDokunma.EKRANLAR.all { WidgetDokunma.EKRAN_ADLARI.containsKey(it) })
    }
}
