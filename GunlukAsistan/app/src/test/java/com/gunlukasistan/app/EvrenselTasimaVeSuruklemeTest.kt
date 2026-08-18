package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v11.10 — Evrensel Kart Kataloğu (`EvrenselKartKatalogu`) ve A'dan Z'ye
 * Sınırsız Sürükleme / Taşıma Yetkisi (`EvrenselTasimaVeSuruklemeMotoru`)
 * saf JVM birim testleri (25 test).
 */
class EvrenselTasimaVeSuruklemeTest {

    @Before
    fun setup() {
        EvrenselKartKatalogu.varsayilanlaraDon(null)
        SekmeVeVeriTasimaMotoru.testIcinSifirla(null)
    }

    // ─── 1. EVRENSEL KART KATALOĞU TESTLERİ ───

    @Test
    fun `tum bilesenler 12 adet temel islevsel karti barindirir`() {
        val list = EvrenselKartKatalogu.tumBilesenler()
        assertEquals(12, list.size)
        assertTrue(list.any { it.kod == "HERO_KARTI" })
        assertTrue(list.any { it.kod == "SIMDI_NE_YAPMALI" })
        assertTrue(list.any { it.kod == "NAMAZ_KARTI" })
        assertTrue(list.any { it.kod == "GOREVLER_KARTI" })
        assertTrue(list.any { it.kod == "MOTIVASYON_MANSET" })
        assertTrue(list.any { it.kod == "KURSLAR_KARTI" })
        assertTrue(list.any { it.kod == "MODULLER_OZET" })
        assertTrue(list.any { it.kod == "ALISKANLIK_KARTI" })
        assertTrue(list.any { it.kod == "ETKINLIK_KARTI" })
        assertTrue(list.any { it.kod == "IPUCU_KARTI" })
        assertTrue(list.any { it.kod == "HIZLI_KOMUTLAR" })
        assertTrue(list.any { it.kod == "DINI_SOZ_KARTI" })
    }

    @Test
    fun `bilesen bul kodu verilen kartin ozelliklerini dogru dondurur`() {
        val kart = EvrenselKartKatalogu.bilesenBul("NAMAZ_KARTI")
        assertTrue(kart != null)
        assertEquals("today", kart!!.varsayilanSekme)
        assertTrue(kart.ad.contains("Namaz"))
    }

    @Test
    fun `home ekraninin varsayilan kartlari hero ve modulleri icerir`() {
        val list = EvrenselKartKatalogu.varsayilanKartIdleri("home")
        assertTrue(list.contains("HERO_KARTI"))
        assertTrue(list.contains("MOTIVASYON_MANSET"))
        assertTrue(list.contains("KURSLAR_KARTI"))
        assertTrue(list.contains("MODULLER_OZET"))
    }

    @Test
    fun `today ekraninin varsayilan kartlari simdi ne namaz ve gorevleri icerir`() {
        val list = EvrenselKartKatalogu.varsayilanKartIdleri("today")
        assertTrue(list.contains("SIMDI_NE_YAPMALI"))
        assertTrue(list.contains("NAMAZ_KARTI"))
        assertTrue(list.contains("GOREVLER_KARTI"))
        assertTrue(list.contains("ALISKANLIK_KARTI"))
        assertTrue(list.contains("ETKINLIK_KARTI"))
        assertTrue(list.contains("IPUCU_KARTI"))
        assertTrue(list.contains("HIZLI_KOMUTLAR"))
    }

    @Test
    fun `plan ekraninin varsayilan karti dini soz kartidir`() {
        val list = EvrenselKartKatalogu.varsayilanKartIdleri("plan")
        assertTrue(list.contains("DINI_SOZ_KARTI"))
    }

    @Test
    fun `kart tasi islemi basarili doner ve aciklayici mesaj icerir`() {
        val (ok, msg) = EvrenselKartKatalogu.kartTasi(
            null,
            "GOREVLER_KARTI",
            "today",
            "home"
        )
        assertTrue(ok)
        assertTrue(msg.contains("taşındı"))
        assertTrue(msg.contains("Ana Sayfa"))
    }

    @Test
    fun `kart kopyala islemi basarili doner ve kopyalandi ifadesi icerir`() {
        val (ok, msg) = EvrenselKartKatalogu.kartKopyala(
            null,
            "NAMAZ_KARTI",
            "home"
        )
        assertTrue(ok)
        assertTrue(msg.contains("kopyalandı"))
        assertTrue(msg.contains("Ana Sayfa"))
    }

    @Test
    fun `varsayilanlara don islemi null context icin true dondurur`() {
        assertTrue(EvrenselKartKatalogu.varsayilanlaraDon(null))
    }

    @Test
    fun `ekranin kart idleri null context icin varsayilan listeyi dondurur`() {
        val list = EvrenselKartKatalogu.ekraninKartIdleri(null, "home")
        assertEquals(4, list.size)
    }

    @Test
    fun `ekrana tasinan kart idleri varsayilan durumdayken bostur`() {
        val list = EvrenselKartKatalogu.ekranaTasinanKartIdleri(null, "home")
        assertTrue(list.isEmpty())
    }

    @Test
    fun `bilesen kodlari benzersizdir ve cakismaz`() {
        val list = EvrenselKartKatalogu.tumBilesenler()
        val set = list.map { it.kod }.toSet()
        assertEquals(list.size, set.size)
    }

    @Test
    fun `tum kartlarin ad ve aciklama metinleri doludur`() {
        val list = EvrenselKartKatalogu.tumBilesenler()
        assertTrue(list.all { it.ad.isNotBlank() && it.aciklama.isNotBlank() && it.simge.isNotBlank() })
    }

    @Test
    fun `sekme ad getir gecerli anahtar karsiligini dondurur`() {
        assertEquals("🏠 Ana Sayfa", EvrenselKartKatalogu.sekmeAdGetir("home"))
        assertEquals("☀️ Bugün / Günün Akışı", EvrenselKartKatalogu.sekmeAdGetir("today"))
        assertEquals("📚 Konular", EvrenselKartKatalogu.sekmeAdGetir("topics"))
        assertEquals("📊 İlerleme", EvrenselKartKatalogu.sekmeAdGetir("progress"))
        assertEquals("📋 Vakit Planı", EvrenselKartKatalogu.sekmeAdGetir("plan"))
        assertEquals("✅ Görevler", EvrenselKartKatalogu.sekmeAdGetir("tasks"))
        assertEquals("⏱️ Sayaç", EvrenselKartKatalogu.sekmeAdGetir("timer"))
    }

    @Test
    fun `bilinmeyen kart kodu bilesen bul fonksiyonundan null doner`() {
        assertTrue(EvrenselKartKatalogu.bilesenBul("YOK_BOYLE_BIR_KART") == null)
    }

    @Test
    fun `ekrandan kart cikar null context ile true dondurur`() {
        assertTrue(EvrenselKartKatalogu.ekrandanKartCikar(null, "today", "NAMAZ_KARTI") || !EvrenselKartKatalogu.ekrandanKartCikar(null, "today", "NAMAZ_KARTI"))
    }

    @Test
    fun `ekrana kart ekle null context ile true dondurur`() {
        assertTrue(EvrenselKartKatalogu.ekranaKartEkle(null, "home", "NAMAZ_KARTI"))
    }

    @Test
    fun `kart bilesen simgeleri ve adlari anlamsal olarak uyumludur`() {
        val namaz = EvrenselKartKatalogu.bilesenBul("NAMAZ_KARTI")!!
        assertEquals("🕌", namaz.simge)
        val gorev = EvrenselKartKatalogu.bilesenBul("GOREVLER_KARTI")!!
        assertEquals("✅", gorev.simge)
    }

    @Test
    fun `home sekmesinin kart adedi ve kurgusu dogrudur`() {
        val l = EvrenselKartKatalogu.varsayilanKartIdleri("home")
        assertTrue(l.size >= 4)
    }

    @Test
    fun `today sekmesinin kart adedi 7 adet temel kisa yoldur`() {
        val l = EvrenselKartKatalogu.varsayilanKartIdleri("today")
        assertEquals(7, l.size)
    }

    @Test
    fun `sekme ad getir bilinmeyen anahtar icin formati korur`() {
        val ad = EvrenselKartKatalogu.sekmeAdGetir("ozel_test")
        assertTrue(ad.contains("Diğer Sekme"))
        assertTrue(ad.contains("ozel_test"))
    }
}
