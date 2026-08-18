package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v8.7 — Ana ekran sıralama mantığı testleri (öneri 16 güvencesi).
 *
 * ── Neden bu testler yazıldı ──
 * v8.5'te ana ekran özelleştirme eklendi ama sandbox'ta emülatör
 * olmadığı için cihazda hiç denenmedi. Sıralama bozulursa kullanıcının
 * ana ekranı karışır — geri dönüşü "Sıfırla" düğmesi olsa da kötü bir
 * ilk izlenim.
 *
 * `siralaKayittan` Android'e bağımlı olmadığı için saf JUnit ile
 * doğrulanabiliyor. `Context` gerektiren kısımlar (SharedPreferences)
 * test edilemiyor; onlar zaten tek satırlık okuma/yazma.
 *
 * ── Test edilen kurallar ──
 * 1. Boş kayıt → varsayılan sıra
 * 2. Tam kayıt → kayıttaki sıra
 * 3. Eksik kayıt → eksikler SONA (ileri uyumluluk)
 * 4. Bilinmeyen kod → yok sayılır (geriye uyumluluk)
 * 5. Yinelenen kod → bir kez
 * 6. Hiçbir durumda blok kaybolmaz veya çoğalmaz
 */
class AnaEkranDuzenTest {

    /** Gerçek blok listesi yerine sade bir örnek — test kırılgan olmasın. */
    private val ornek = listOf(
        AnaEkranDuzen.Blok("a", 1, 0, "A", zorunlu = true),
        AnaEkranDuzen.Blok("b", 2, 0, "B"),
        AnaEkranDuzen.Blok("c", 3, 0, "C"),
        AnaEkranDuzen.Blok("d", 4, 0, "D")
    )

    private fun kodlar(liste: List<AnaEkranDuzen.Blok>) = liste.map { it.kod }

    @Test
    fun `bos kayit varsayilan sirayi verir`() {
        val sonuc = AnaEkranDuzen.siralaKayittan("", ornek)
        assertEquals(listOf("a", "b", "c", "d"), kodlar(sonuc))
    }

    @Test
    fun `sadece bosluk iceren kayit varsayilan sirayi verir`() {
        val sonuc = AnaEkranDuzen.siralaKayittan("   ", ornek)
        assertEquals(listOf("a", "b", "c", "d"), kodlar(sonuc))
    }

    @Test
    fun `tam kayit kayittaki sirayi verir`() {
        val sonuc = AnaEkranDuzen.siralaKayittan("d,c,b,a", ornek)
        assertEquals(listOf("d", "c", "b", "a"), kodlar(sonuc))
    }

    @Test
    fun `eksik kayitta eksik bloklar sona eklenir`() {
        // Kullanıcı v8.5'te sırayı kaydetti; v8.6'da "d" bloğu eklendi.
        // "d" kaybolmamalı, sona gelmeli.
        val sonuc = AnaEkranDuzen.siralaKayittan("c,a", ornek)
        assertEquals(listOf("c", "a", "b", "d"), kodlar(sonuc))
    }

    @Test
    fun `bilinmeyen kod yok sayilir`() {
        // Kullanıcı v8.6'da bir blok sıraladı; v8.7'de o blok kaldırıldı.
        // Çökme olmamalı, bilinmeyen kod atlanmalı.
        val sonuc = AnaEkranDuzen.siralaKayittan("b,kaldirildi,a", ornek)
        assertEquals(listOf("b", "a", "c", "d"), kodlar(sonuc))
    }

    @Test
    fun `yinelenen kod bir kez alinir`() {
        val sonuc = AnaEkranDuzen.siralaKayittan("b,b,a,b", ornek)
        assertEquals(listOf("b", "a", "c", "d"), kodlar(sonuc))
    }

    @Test
    fun `bosluklu kayit temizlenir`() {
        val sonuc = AnaEkranDuzen.siralaKayittan(" c , a ,b", ornek)
        assertEquals(listOf("c", "a", "b", "d"), kodlar(sonuc))
    }

    @Test
    fun `sondaki virgul sorun cikarmaz`() {
        val sonuc = AnaEkranDuzen.siralaKayittan("b,a,", ornek)
        assertEquals(listOf("b", "a", "c", "d"), kodlar(sonuc))
    }

    @Test
    fun `hicbir durumda blok kaybolmaz veya cogalmaz`() {
        val girdiler = listOf(
            "", "   ", "d,c,b,a", "c,a", "b,kaldirildi,a",
            "b,b,a,b", " c , a ,b", "b,a,", ",,,", "x,y,z"
        )
        girdiler.forEach { kayit ->
            val sonuc = AnaEkranDuzen.siralaKayittan(kayit, ornek)
            assertEquals(
                "Blok sayısı değişti. Kayıt: '$kayit'",
                ornek.size, sonuc.size
            )
            assertEquals(
                "Blok kümesi değişti. Kayıt: '$kayit'",
                ornek.map { it.kod }.toSet(), sonuc.map { it.kod }.toSet()
            )
        }
    }

    @Test
    fun `gercek blok listesi de kurallara uyar`() {
        // Üretimdeki listeyle de çalışmalı
        val gercek = AnaEkranDuzen.bloklar
        assertTrue("En az 2 blok olmalı", gercek.size >= 2)

        val tersKayit = gercek.reversed().joinToString(",") { it.kod }
        val sonuc = AnaEkranDuzen.siralaKayittan(tersKayit, gercek)
        assertEquals(gercek.size, sonuc.size)
        assertEquals(gercek.reversed().map { it.kod }, kodlar(sonuc))
    }

    @Test
    fun `blok kodlari benzersiz`() {
        // Aynı kod iki blokta olsaydı sıralama belirsizleşirdi
        val kodlar = AnaEkranDuzen.bloklar.map { it.kod }
        assertEquals(
            "Blok kodları benzersiz olmalı",
            kodlar.size, kodlar.toSet().size
        )
    }

    @Test
    fun `en az bir zorunlu blok var`() {
        // Hepsi gizlenebilseydi ana ekran bomboş kalırdı
        assertTrue(
            "En az bir blok zorunlu olmalı",
            AnaEkranDuzen.bloklar.any { it.zorunlu }
        )
    }
}
