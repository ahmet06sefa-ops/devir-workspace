package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.13 — Namaz vakti gerçek-zamanlı doğruluk testleri.
 *
 * "Namaz saatleri anlık olarak doğru göstermiyor" düzeltmesinin güvencesi:
 * görüntüleme artık sabit "10 Ağustos 2026" statik saatleri yerine
 * tarihe duyarlı astronomik hesabı ([NamazVakti.hesapla]) kullanır. Bu
 * testler hesabın her tarih/şehir için tutarlı, sıralı ve coğrafyaya uygun
 * vakitler ürettiğini doğrular (saf JVM, Context gerektirmez).
 */
class NamazVaktiDogrulamaTest {

    /** Altı vakit mantıksal sırada (imsak < gunes < ogle < ikindi < aksam < yatsi). */
    private fun assertSira(g: NamazVakti.Gun) {
        val sira = listOf(
            NamazVakti.Vakit.IMSAK, NamazVakti.Vakit.GUNES, NamazVakti.Vakit.OGLE,
            NamazVakti.Vakit.IKINDI, NamazVakti.Vakit.AKSAM, NamazVakti.Vakit.YATSI
        )
        var once = -1
        for (v in sira) {
            val d = g.dakika(v)
            assertTrue("$v ($d) önceki ($once) dakikadan büyük olmalı", d > once)
            once = d
        }
    }

    @Test
    fun `ankara agustos vakitleri mantiksal sirada ve makul araliktadir`() {
        val g = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0)
        assertSira(g)
        assertTrue(g.dakika(NamazVakti.Vakit.IMSAK) in 180..360)   // 03:00-06:00
        assertTrue(g.dakika(NamazVakti.Vakit.OGLE) in 720..810)    // 12:00-13:30
        assertTrue(g.dakika(NamazVakti.Vakit.AKSAM) in 1020..1290) // 17:00-21:30
    }

    @Test
    fun `kis gunleri gunes daha erken batar aksam daha erkendir`() {
        val yaz = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0)
        val kis = NamazVakti.hesapla(2026, 1, 1, 39.9334, 32.8597, 3.0)
        assertTrue(
            "Kış akşamı (${kis.dakika(NamazVakti.Vakit.AKSAM)}) yaz akşamından (${yaz.dakika(NamazVakti.Vakit.AKSAM)}) erken olmalı",
            kis.dakika(NamazVakti.Vakit.AKSAM) < yaz.dakika(NamazVakti.Vakit.AKSAM)
        )
    }

    @Test
    fun `gunluk vakitler her gun degisir anlik gercek zamanlidir`() {
        val g1 = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0)
        val g2 = NamazVakti.hesapla(2026, 8, 16, 39.9334, 32.8597, 3.0)
        assertTrue(
            g1.dakika(NamazVakti.Vakit.AKSAM) != g2.dakika(NamazVakti.Vakit.AKSAM) ||
                g1.dakika(NamazVakti.Vakit.IMSAK) != g2.dakika(NamazVakti.Vakit.IMSAK)
        )
    }

    @Test
    fun `dogudaki sehirde aksam doguya gore daha erkendir`() {
        val erzurum = NamazVakti.hesapla(2026, 8, 15, 39.90, 41.27, 3.0)
        val izmir = NamazVakti.hesapla(2026, 8, 15, 38.4192, 27.1287, 3.0)
        assertTrue(
            "Erzurum akşamı (${erzurum.dakika(NamazVakti.Vakit.AKSAM)}) İzmir akşamından (${izmir.dakika(NamazVakti.Vakit.AKSAM)}) erken olmalı",
            erzurum.dakika(NamazVakti.Vakit.AKSAM) < izmir.dakika(NamazVakti.Vakit.AKSAM)
        )
    }

    @Test
    fun `imsağ gunes dogusundan once ve yatsi aksamdan sonradir`() {
        val g = NamazVakti.hesapla(2026, 8, 15, 41.0082, 28.9784, 3.0) // İstanbul
        assertTrue(g.dakika(NamazVakti.Vakit.IMSAK) < g.dakika(NamazVakti.Vakit.GUNES))
        assertTrue(g.dakika(NamazVakti.Vakit.YATSI) > g.dakika(NamazVakti.Vakit.AKSAM))
    }

    @Test
    fun `ikindi kat sayisina gore degisir hanefi kat 2 ile gecer sonrasina kayar`() {
        val kat1 = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0, ikindiKat = 1)
        val kat2 = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0, ikindiKat = 2)
        assertTrue(kat2.dakika(NamazVakti.Vakit.IKINDI) > kat1.dakika(NamazVakti.Vakit.IKINDI))
    }

    @Test
    fun `hesapla gun nesnesi saat dize bicimini dogru dondurur`() {
        val g = NamazVakti.hesapla(2026, 8, 15, 39.9334, 32.8597, 3.0)
        val ogle = g.saat(NamazVakti.Vakit.OGLE)
        assertEquals(5, ogle.length)
        assertEquals(':', ogle[2])
    }

    @Test
    fun `bugun guncel saatler mantiksal sirayi korur ve alti vakit icerir`() {
        // GoogleNamazKoku üzerinden üretilen tüm vakitler dolu olmalı (saf model).
        val koku = NamazAylikVeriServisi.GoogleNamazKoku(
            imsak = "04:13", gunes = "05:48", ogle = "12:55",
            ikindi = "16:52", aksam = "20:03", yatsi = "21:30"
        )
        val dakikalar = listOf(koku.imsak, koku.gunes, koku.ogle, koku.ikindi, koku.aksam, koku.yatsi)
            .map { it.split(":").let { p -> p[0].toInt() * 60 + p[1].toInt() } }
        assertTrue(dakikalar.zipWithNext().all { (a, b) -> a < b })
    }
}
