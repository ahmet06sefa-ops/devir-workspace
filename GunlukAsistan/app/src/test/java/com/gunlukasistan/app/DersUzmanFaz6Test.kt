package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.66 — Ders Çalışma Uzman Faz 6 ([DersUzmanFaz6]) saf birim testleri (27 test).
 */
class DersUzmanFaz6Test {

    // ── 1. RÜTBE, MARATON & PRESTİJ (1..6) ──
    @Test
    fun `rutbe hesaplama 30 saat icin altin efsane dondurur`() {
        val r = DersUzmanFaz6.Faz6_1_RutbeVePrestij.rutbeHesapla(30)
        assertEquals("Altın Efsane", r.unvan)
        assertEquals(150, r.xpBonus)
    }

    @Test
    fun `rutbe hesaplama 15 saat icin gumus usta dondurur`() {
        val r = DersUzmanFaz6.Faz6_1_RutbeVePrestij.rutbeHesapla(15)
        assertEquals("Gümüş Usta", r.unvan)
        assertEquals(75, r.xpBonus)
    }

    @Test
    fun `rutbe hesaplama 5 saat icin bronz cirak dondurur`() {
        val r = DersUzmanFaz6.Faz6_1_RutbeVePrestij.rutbeHesapla(5)
        assertEquals("Bronz Çırak", r.unvan)
        assertEquals(25, r.xpBonus)
    }

    @Test
    fun `maraton madalyasi 4 saatin uzerinde true ve tebrik dondurur`() {
        val res = DersUzmanFaz6.Faz6_1_RutbeVePrestij.maratonMadalyasiKontrol(4)
        assertTrue(res.first)
        assertTrue(res.second.contains("TEBRİKLER") || res.second.contains("Altın Maraton"))
    }

    @Test
    fun `maraton madalyasi 2 saatte false ve kalan saat dondurur`() {
        val res = DersUzmanFaz6.Faz6_1_RutbeVePrestij.maratonMadalyasiKontrol(2)
        assertFalse(res.first)
        assertTrue(res.second.contains("2 saat daha"))
    }

    @Test
    fun `prestij sifirlama mevcut seviyeyi 1 e indirip prestij rozeti dondurur`() {
        val res = DersUzmanFaz6.Faz6_1_RutbeVePrestij.prestijSifirlamaUygula(12)
        assertEquals(1, res.first)
        assertTrue(res.second.contains("PRESTİJ ROZETİ"))
    }

    // ── 2. CANAVAR KONU & BİLGİ SANDIĞI (7..9) ──
    @Test
    fun `canavar konu tamamlama 100 xp ve zafer mesaji dondurur`() {
        val zafer = DersUzmanFaz6.Faz6_2_CanavarVeSandik.canavarKonuTamamla("İntegral")
        assertEquals(100, zafer.first)
        assertTrue(zafer.second.contains("ZAFER"))
    }

    @Test
    fun `varsayilan sandik notlari en az 4 adet genel kultur notu icerir`() {
        val list = DersUzmanFaz6.Faz6_2_CanavarVeSandik.varsayilanSandikNotlari()
        assertEquals(4, list.size)
    }

    @Test
    fun `gunun sandik notu indeks donusumlu not secer`() {
        val not0 = DersUzmanFaz6.Faz6_2_CanavarVeSandik.gununSandikNotunuSec(0)
        val not4 = DersUzmanFaz6.Faz6_2_CanavarVeSandik.gununSandikNotunuSec(4)
        assertEquals(not0.id, not4.id)
    }

    // ── 3. GÖZ ERGONOMİSİ & SINAV SALONU (10..12) ──
    @Test
    fun `goz kuruluk hatirlaticisi 30 dkcik ekranda true ve uyari dondurur`() {
        val res = DersUzmanFaz6.Faz6_3_GozVeSalon.gozKurulukHatirlaticisi(30)
        assertTrue(res.first)
        assertTrue(res.second.contains("10 kez bilinçli göz kırpın"))
    }

    @Test
    fun `goz kuruluk hatirlaticisi 15 dk da false ve guvenli bolge dondurur`() {
        val res = DersUzmanFaz6.Faz6_3_GozVeSalon.gozKurulukHatirlaticisi(15)
        assertFalse(res.first)
        assertTrue(res.second.contains("güvenli bölgede"))
    }

    @Test
    fun `salon ergonomi taktikleri en az 3 adet pratik taktik icerir`() {
        val list = DersUzmanFaz6.Faz6_3_GozVeSalon.salonErgonomiTaktikleri()
        assertTrue(list.size >= 3)
        assertTrue(list[0].contains("21-22°C"))
    }

    // ── 4. OLUMLAMALAR & SABBATH GÜNÜ (13..15) ──
    @Test
    fun `sinav sabahi olumlamalari 4 adet ozguven mesaji listeler`() {
        val list = DersUzmanFaz6.Faz6_4_OlumlamaVeSabbath.sinavSabahiOlumlamalari()
        assertEquals(4, list.size)
        assertTrue(list[0].contains("Elimden gelenin en iyisini"))
    }

    @Test
    fun `sabbath gunu durumu gunler eslesince true ve donduruldu mesaji dondurur`() {
        val res = DersUzmanFaz6.Faz6_4_OlumlamaVeSabbath.sabbathGunuDurumu("Pazar", "PAZAR")
        assertTrue(res.first)
        assertTrue(res.second.contains("donduruldu"))
    }

    @Test
    fun `sabbath gunu durumu gunler farkliysa false ve aktif gun mesaji dondurur`() {
        val res = DersUzmanFaz6.Faz6_4_OlumlamaVeSabbath.sabbathGunuDurumu("Pazartesi", "Pazar")
        assertFalse(res.first)
        assertTrue(res.second.contains("Aktif Çalışma Günü"))
    }

    // ── 5. ARAMA İNDEKSİ & ALARM SAĞLIĞI (16..20) ──
    @Test
    fun `varsayilan arama indeksi en az 7 temel ders aracini listeler`() {
        val idx = DersUzmanFaz6.Faz6_5_AramaVeAlarm.varsayilanAramaIndeksi()
        assertTrue(idx.size >= 7)
    }

    @Test
    fun `anahtar kelime ara pomodoro yazinca pomodoro modulleri bulur`() {
        val res = DersUzmanFaz6.Faz6_5_AramaVeAlarm.anahtarKelimeAra("pomodoro")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.kelime == "POMODORO" })
    }

    @Test
    fun `anahtar kelime ara bos sorguda bos liste dondurur`() {
        val res = DersUzmanFaz6.Faz6_5_AramaVeAlarm.anahtarKelimeAra("")
        assertTrue(res.isEmpty())
    }

    @Test
    fun `anahtar kelime ara osym yazinca osym modulleri dondurur`() {
        val res = DersUzmanFaz6.Faz6_5_AramaVeAlarm.anahtarKelimeAra("ösym")
        assertTrue(res.any { it.modulAdi.contains("ÖSYM") || it.kelime == "OSYM" || it.kelime == "CELDIRICI" })
    }

    @Test
    fun `alarm saglik denetimi true ve 100 yuzde saglik mesaji dondurur`() {
        val res = DersUzmanFaz6.Faz6_5_AramaVeAlarm.alarmSaglikDenetimi()
        assertTrue(res.first)
        assertTrue(res.second.contains("100%"))
    }

    // ── 6. STRATEJİ & ÖNKOŞUL (21..25) ──
    @Test
    fun `brans stratejisi turkce icin paragraf taktigini dondurur`() {
        val st = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.bransStratejisiGetir("TURKCE")
        assertTrue(st.contains("Paragraf"))
    }

    @Test
    fun `brans stratejisi matematik icin tablo ve turlama taktigini dondurur`() {
        val st = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.bransStratejisiGetir("MATEMATIK")
        assertTrue(st.contains("tablo") || st.contains("turlama"))
    }

    @Test
    fun `brans stratejisi tarih icin neden sonuc taktigini dondurur`() {
        val st = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.bransStratejisiGetir("TARIH")
        assertTrue(st.contains("neden-sonuç"))
    }

    @Test
    fun `onkosul kontrolu integral icin turev sartini dondurur`() {
        val k = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.onkosulKontrolu("INTEGRAL")
        assertFalse(k.first)
        assertTrue(k.second.contains("Türev"))
    }

    @Test
    fun `onkosul kontrolu genel konu icin karsilandi dondurur`() {
        val k = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.onkosulKontrolu("Osmanlı Dağılma")
        assertTrue(k.first)
        assertTrue(k.second.contains("Karşılandı"))
    }

    // ── 7. SADELEŞTİRİCİ AI (26..27) ──
    @Test
    fun `paragrafi sadelestir kisa metinde uyari dondurur`() {
        val s = DersUzmanFaz6.Faz6_7_Sadelestirici.paragrafiSadelestir("kısa")
        assertTrue(s.contains("en az 1-2 cümlelik"))
    }

    @Test
    fun `paragrafi sadelestir uzun metinde ai 5inci sinif ozeti dondurur`() {
        val s = DersUzmanFaz6.Faz6_7_Sadelestirici.paragrafiSadelestir("Bu felsefi yasa bir olayın meydana gelmesi için önkoşulların sağlanmasını öngörür.")
        assertTrue(s.contains("5. Sınıf Seviyesinde"))
        assertTrue(s.contains("Sebep olmadan sonuç oluşmaz"))
    }
}
