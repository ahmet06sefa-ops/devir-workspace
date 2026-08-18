package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.16 · Widget Atölyesi — saf tablo/çekirdek testleri.
 * WidgetAtolye (yüzde snap, dolgu/nefes tabloları, migration haritası)
 * Modul (bütçe aritmetiği, sıra düzenleme, temizleme)
 */
class GrupWTest {

    // ── Yazı yüzdesi ───────────────────────────────────────────────

    @Test
    fun `kademe yuzde migration haritasi`() {
        assertEquals(85, WidgetAtolye.kademeToYuzde(0))
        assertEquals(100, WidgetAtolye.kademeToYuzde(1))
        assertEquals(115, WidgetAtolye.kademeToYuzde(2))
        assertEquals(100, WidgetAtolye.kademeToYuzde(9)) // aralık dışı → normal
    }

    @Test
    fun `yuzde snap beslik ve kelepce`() {
        assertEquals(75, WidgetAtolye.yuzdeSnap(40))   // alt sınır
        assertEquals(150, WidgetAtolye.yuzdeSnap(999)) // üst sınır
        assertEquals(100, WidgetAtolye.yuzdeSnap(102)) // 100'e yuvarlanır
        assertEquals(105, WidgetAtolye.yuzdeSnap(103))
        assertEquals(105, WidgetAtolye.yuzdeSnap(107))
        assertEquals(75, WidgetAtolye.yuzdeSnap(75))
        assertEquals(150, WidgetAtolye.yuzdeSnap(150))
    }

    // ── Dolgu / satır nefesi tabloları ─────────────────────────────

    @Test
    fun `kok dolgu tablosu ve kelepce`() {
        assertEquals(0, WidgetAtolye.kokDolguDp(0))
        assertEquals(2, WidgetAtolye.kokDolguDp(1))
        assertEquals(6, WidgetAtolye.kokDolguDp(2))
        assertEquals(12, WidgetAtolye.kokDolguDp(3))
        assertEquals(12, WidgetAtolye.kokDolguDp(99))  // üst kelepçe
        assertEquals(0, WidgetAtolye.kokDolguDp(-5))   // alt kelepçe
    }

    @Test
    fun `satir nefesi tablosu`() {
        assertEquals(0, WidgetAtolye.satirDolguDp(0))
        assertEquals(2, WidgetAtolye.satirDolguDp(1))
        assertEquals(6, WidgetAtolye.satirDolguDp(2))
        assertEquals(6, WidgetAtolye.satirDolguDp(7))
    }

    // ── Modul bütçe aritmetiği ─────────────────────────────────────

    @Test
    fun `varsayilan sira ve tanim bilgisi`() {
        val v = Modul.varsayilanSira()
        assertEquals(4, v.size)
        v.forEach { assertTrue(Modul.tanim(it) != null) }
        assertEquals(7, Modul.TANIMLAR.size)
        assertEquals(2, Modul.tanim("saat")?.satir)
        assertEquals(1, Modul.tanim("seri")?.satir)
    }

    @Test
    fun `sigacaklar butceye gore sirayla doldurur`() {
        val v = Modul.varsayilanSira() // saat2 · sayac1 · gorevler2 · seri1
        assertEquals(v, Modul.sigacaklar(v, 8))            // hepsi sığar (6)
        assertEquals(
            listOf("saat", "sayac", "gorevler"),
            Modul.sigacaklar(v, 5)
        )
        assertEquals(listOf("saat", "sayac"), Modul.sigacaklar(v, 3))
        assertEquals(emptyList<String>(), Modul.sigacaklar(v, 1)) // saat sığmaz → dur
        assertEquals(emptyList<String>(), Modul.sigacaklar(v, 0))
        // öz sıra: tek satırlıklar yan yana
        assertEquals(
            listOf("seri", "uyku"),
            Modul.sigacaklar(listOf("seri", "uyku", "sayac"), 2)
        )
    }

    @Test
    fun `sira duzenleme sinirlari`() {
        val s = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), Modul.yukariTasi(s, 1))
        assertEquals(s, Modul.yukariTasi(s, 0))  // en üstte
        assertEquals(s, Modul.yukariTasi(s, 5))  // aralık dışı
        assertEquals(listOf("a", "c", "b"), Modul.asagiTasi(s, 1))
        assertEquals(s, Modul.asagiTasi(s, 2))   // en altta
        assertEquals(s, Modul.asagiTasi(s, -1))
    }

    @Test
    fun `temizle bilinmeyeni atar bosluga izin verir`() {
        assertEquals(
            listOf("saat", "seri"),
            Modul.temizle(listOf("saat", "zombi", "seri", "saat"))
        )
        assertEquals(emptyList<String>(), Modul.temizle(emptyList())) // boş geçerli
        assertEquals(emptyList<String>(), Modul.temizle(listOf("x", "y")))
    }
}
