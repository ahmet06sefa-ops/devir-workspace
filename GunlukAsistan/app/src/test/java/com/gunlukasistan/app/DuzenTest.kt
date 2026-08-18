package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.18 · Ekran Atölyesi — DuzenCekirdek saf mantık testleri.
 *
 * Kullanıcı isteği: "basılı tutunca öğelerin yerini/boyutunu
 * değiştirebileyim". Taşıma, boyut nefesi ve boyut kaydı diziçimi
 * Android'siz doğrulanır (cihazsız sandbox güvencesi).
 */
class DuzenTest {

    // ---------------- Taşıma ----------------

    private val liste = listOf("a", "b", "c", "d")

    @Test
    fun tasi_ortadaYukariAsagi() {
        assertEquals(listOf("b", "a", "c", "d"), DuzenCekirdek.tasi(liste, "b", -1))
        assertEquals(listOf("a", "c", "b", "d"), DuzenCekirdek.tasi(liste, "b", +1))
    }

    @Test
    fun tasi_sinirdaDegismez() {
        // İlk öğe daha yukarı gidemez, son öğe daha aşağı inemez
        assertEquals(liste, DuzenCekirdek.tasi(liste, "a", -1))
        assertEquals(liste, DuzenCekirdek.tasi(liste, "d", +1))
    }

    @Test
    fun tasi_bilinmeyenKodDegismez() {
        assertEquals(liste, DuzenCekirdek.tasi(liste, "zz", -1))
        assertEquals(liste, DuzenCekirdek.tasi(liste, "zz", +1))
    }

    @Test
    fun tasi_tekOgeler() {
        assertEquals(listOf("x"), DuzenCekirdek.tasi(listOf("x"), "x", -1))
        // yön işareti normalize: -99 da yukarı sayılır
        assertEquals(listOf("b", "a", "c", "d"), DuzenCekirdek.tasi(liste, "b", -99))
    }

    // ---------------- Boyut nefesi ----------------

    @Test
    fun boyutNefesDp_tablosu() {
        assertEquals(2, DuzenCekirdek.boyutNefesDp(0))
        assertEquals(6, DuzenCekirdek.boyutNefesDp(1))
        assertEquals(14, DuzenCekirdek.boyutNefesDp(2))
        // Taşan kelepçe: üst sınır 2 → 14, alt sınır 0 → 2
        assertEquals(14, DuzenCekirdek.boyutNefesDp(99))
        assertEquals(2, DuzenCekirdek.boyutNefesDp(-5))
    }

    // ---------------- Boyut kaydı diziçimi ----------------

    @Test
    fun boyutKayitOku_gecerliKayit() {
        val h = DuzenCekirdek.boyutKayitOku("hero:2,grafik:0")
        assertEquals(2, h["hero"])
        assertEquals(0, h["grafik"])
        assertTrue(h["yok"] == null)
    }

    @Test
    fun boyutKayitOku_bosVeBozuk() {
        assertTrue(DuzenCekirdek.boyutKayitOku("").isEmpty())
        assertTrue(DuzenCekirdek.boyutKayitOku("   ").isEmpty())
        // Bozuk parçalar atlanır, geçerli parça korunur
        val h = DuzenCekirdek.boyutKayitOku("hero:x,:2,bozuk,grafik:1,3:")
        assertEquals(1, h.size)
        assertEquals(1, h["grafik"])
    }

    @Test
    fun boyutKayitOku_kekleyiciYazarken() {
        // Kayıttaki 99 kelepçelenerek 2 olur
        val h = DuzenCekirdek.boyutKayitOku("hero:99")
        assertEquals(2, h["hero"])
        // Geri yazma da kelepçeli ve ters çevrilebilir
        val yazi = DuzenCekirdek.boyutKayitYaz(mapOf("hero" to 99, "g" to -3))
        assertEquals("hero:2,g:0", yazi)
        // Gidiş-dönüş kararlı
        assertEquals(h, DuzenCekirdek.boyutKayitOku(DuzenCekirdek.boyutKayitYaz(h)))
    }

    @Test
    fun boyutKayitYaz_bosHarita() {
        assertEquals("", DuzenCekirdek.boyutKayitYaz(emptyMap()))
    }

    // ---------------- Blok kayıtlarının tutarlılığı ----------------

    @Test
    fun blokKayitlari_kodlarBenzersiz() {
        val ana = AnaEkranDuzen.bloklar.map { it.kod }
        val bugun = BugunDuzen.bloklar.map { it.kod }
        assertEquals(ana.size, ana.toSet().size)
        assertEquals(bugun.size, bugun.toSet().size)
        // İki ekran aynı id alanını kullanmaz
        val anaIdler = AnaEkranDuzen.bloklar.map { it.viewId }.toSet()
        val bugunIdler = BugunDuzen.bloklar.map { it.viewId }.toSet()
        assertTrue(anaIdler.intersect(bugunIdler).isEmpty())
        assertEquals(8, ana.size)
        assertEquals(8, bugun.size)
    }

    @Test
    fun blokKayitlari_katlanabilirBayragi() {
        // Katlanabilir bloklar kümesi boş değil ve bayrak varsayılanı false
        assertTrue(AnaEkranDuzen.bloklar.any { it.katlanabilir })
        assertTrue(BugunDuzen.bloklar.any { it.katlanabilir })
        // Bugün ekranında zorunlu blok yok (tamamı gizlenebilir — kullanıcı gücü)
        assertFalse(BugunDuzen.bloklar.any { it.zorunlu })
        // Ana ekranda hero zorunlu kalır
        assertTrue(AnaEkranDuzen.bloklar.first { it.kod == "hero" }.zorunlu)
    }
}
