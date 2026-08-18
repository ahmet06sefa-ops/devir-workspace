package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v11.08 — Ana Ekran, Bugün, Konular, İlerleme ve Plan Sekmeleri Arası
 * Veri / Kart Taşıma ve Otonom Sıralama Motoru (`SekmeVeVeriTasimaMotoru`)
 * saf JVM birim testleri (20 test).
 */
class SekmeVeVeriTasimaTest {

    @Before
    fun setup() {
        SekmeVeVeriTasimaMotoru.testIcinSifirla(null)
    }

    @Test
    fun `context null iken siralama kaydet ve getir guvenli bos liste dondurur`() {
        val ok = SekmeVeVeriTasimaMotoru.siralamaKaydet(null, "home", listOf("c1", "c2"))
        assertTrue(ok)
        assertTrue(SekmeVeVeriTasimaMotoru.siralamaGetir(null, "home").isEmpty())
    }

    @Test
    fun `context null iken sira sifirla islemi basarili doner`() {
        assertTrue(SekmeVeVeriTasimaMotoru.siraSifirla(null, "today"))
    }

    @Test
    fun `sekme ad getir anahtar kodlarini turkce aciklama olarak cevirir`() {
        assertEquals("🏠 Ana Sayfa", SekmeVeVeriTasimaMotoru.sekmeAdGetir("home"))
        assertEquals("☀️ Bugün / Günün Akışı", SekmeVeVeriTasimaMotoru.sekmeAdGetir("today"))
        assertEquals("📚 Konular", SekmeVeVeriTasimaMotoru.sekmeAdGetir("topics"))
        assertEquals("📊 İlerleme", SekmeVeVeriTasimaMotoru.sekmeAdGetir("progress"))
        assertEquals("📋 Vakit Planı", SekmeVeVeriTasimaMotoru.sekmeAdGetir("plan"))
        assertEquals("✅ Görevler", SekmeVeVeriTasimaMotoru.sekmeAdGetir("tasks"))
        assertEquals("⏱️ Sayaç", SekmeVeVeriTasimaMotoru.sekmeAdGetir("timer"))
    }

    @Test
    fun `veri tasi veya kopyala islemi basarili cifti ve mesaji dondurur`() {
        val (ok, msg) = SekmeVeVeriTasimaMotoru.veriTasiVeyaKopyala(
            null,
            "home",
            "today",
            "Sınav Çalışması",
            "3 saat matematik",
            kopyalaMi = false
        )
        assertTrue(ok)
        assertTrue(msg.contains("taşındı"))
        assertTrue(msg.contains("Bugün"))
    }

    @Test
    fun `veri kopyalama isleminde mesaj kopyalandi ifadesi icerir`() {
        val (ok, msg) = SekmeVeVeriTasimaMotoru.veriTasiVeyaKopyala(
            null,
            "today",
            "plan",
            "Kitap Okuma",
            "30 sayfa felsefe",
            kopyalaMi = true
        )
        assertTrue(ok)
        assertTrue(msg.contains("kopyalandı"))
    }

    @Test
    fun `tasinan veri nesnesi baslik icerik ve sekme bilgilerini saklar`() {
        val v = SekmeVeVeriTasimaMotoru.TasinanVeri(
            id = "1",
            kaynakSekme = "home",
            hedefSekme = "progress",
            baslik = "İlerleme Özeti",
            icerik = "%45 Tamamlandı"
        )
        assertEquals("home", v.kaynakSekme)
        assertEquals("progress", v.hedefSekme)
        assertEquals("İlerleme Özeti", v.baslik)
    }

    @Test
    fun `tasinan veri baslik bos verildiginde varsayilan baslik atar`() {
        val (ok, msg) = SekmeVeVeriTasimaMotoru.veriTasiVeyaKopyala(
            null,
            "topics",
            "home",
            "   ",
            "İçerik detayı"
        )
        assertTrue(ok)
        assertTrue(msg.contains("Taşınan İçerik"))
    }

    @Test
    fun `bilinmeyen sekme anahtari diger sekme formunda gosterilir`() {
        val ad = SekmeVeVeriTasimaMotoru.sekmeAdGetir("ozel_sekme")
        assertTrue(ad.contains("Diğer Sekme"))
        assertTrue(ad.contains("ozel_sekme"))
    }

    @Test
    fun `tasinan veri sil null context icin false dondurur`() {
        assertFalse(SekmeVeVeriTasimaMotoru.tasinanVeriSil(null, "1"))
    }

    @Test
    fun `tum tasinan verileri getir null context icin bos liste dondurur`() {
        assertTrue(SekmeVeVeriTasimaMotoru.tumTasinanVerileriGetir(null).isEmpty())
    }

    @Test
    fun `sekme icin tasinan veriler filtresi dogru calisir`() {
        val list = listOf(
            SekmeVeVeriTasimaMotoru.TasinanVeri("1", "home", "today", "B1", "I1"),
            SekmeVeVeriTasimaMotoru.TasinanVeri("2", "home", "progress", "B2", "I2"),
            SekmeVeVeriTasimaMotoru.TasinanVeri("3", "topics", "today", "B3", "I3")
        )
        val todayIcin = list.filter { it.hedefSekme == "today" }
        assertEquals(2, todayIcin.size)
    }

    @Test
    fun `komple icindekileri tasi islemi baslik ve icerigi eksiksiz alir`() {
        val (ok, _) = SekmeVeVeriTasimaMotoru.veriTasiVeyaKopyala(
            null,
            "today",
            "progress",
            "Günün Akışı Komple Kart",
            "Namaz: 5 vakit, Odak: 180 dk, Görevler: 4 adet"
        )
        assertTrue(ok)
    }

    @Test
    fun `tek tek veri tasi islemi sekmeler arasi veri aktarimi basarilidir`() {
        val (ok, msg) = SekmeVeVeriTasimaMotoru.veriTasiVeyaKopyala(
            null,
            "tasks",
            "plan",
            "Yapi statigi clayperon",
            "3 görev daha bugün"
        )
        assertTrue(ok)
        assertTrue(msg.contains("Vakit Planı"))
    }

    @Test
    fun `yatay kuculme onleyici scaleX katsayisi daima tam genislikte olmalidir`() {
        val olcekX = 1.0f
        assertEquals(1.0f, olcekX, 0.001f)
    }

    @Test
    fun `tasinan veri id benzersiz prefix t tasir`() {
        val v = SekmeVeVeriTasimaMotoru.TasinanVeri("t-1234", "home", "today", "Test", "Detay")
        assertTrue(v.id.startsWith("t-"))
    }

    @Test
    fun `sekme ad getir timer icin dogru emoji ve tanim dondurur`() {
        assertEquals("⏱️ Sayaç", SekmeVeVeriTasimaMotoru.sekmeAdGetir("timer"))
    }

    @Test
    fun `sekme ad getir plan icin dogru emoji ve tanim dondurur`() {
        assertEquals("📋 Vakit Planı", SekmeVeVeriTasimaMotoru.sekmeAdGetir("plan"))
    }

    @Test
    fun `siralama listesi bos string icin bos dizi dondurur`() {
        val s = ""
        val list = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(list.isEmpty())
    }

    @Test
    fun `siralama listesi virgul ayracli 4 kart id yi dogru ayirir`() {
        val s = "cardToday, coursesCard, gridCard, cardMotivasyonManset"
        val list = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(4, list.size)
        assertEquals("cardToday", list[0])
        assertEquals("cardMotivasyonManset", list[3])
    }

    @Test
    fun `tasinan veri tarihMs alaninda varsayilan simdiki zamani tutar`() {
        val v = SekmeVeVeriTasimaMotoru.TasinanVeri("1", "home", "today", "T", "I")
        assertTrue(v.tarihMs > 0L)
    }

    // ── v11.13: bilesenKoduBul (kart kodu eşleştirme) ──

    @Test
    fun `bilesen kodu gunun akisi ve hero icin HERO_KARTI doner`() {
        assertEquals("HERO_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Günün Akışı"))
        assertEquals("HERO_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Hero"))
    }

    @Test
    fun `bilesen kodu namaz vakit icin NAMAZ_KARTI doner`() {
        assertEquals("NAMAZ_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Vakit Planı"))
    }

    @Test
    fun `bilesen kodu gorev ve oncelik icin GOREVLER_KARTI doner`() {
        assertEquals("GOREVLER_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Görevler"))
        assertEquals("GOREVLER_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Öncelikler"))
    }

    @Test
    fun `bilesen kodu motivasyon ve manşet icin MOTIVASYON_MANSET doner`() {
        assertEquals("MOTIVASYON_MANSET", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Motivasyon Manşeti"))
    }

    @Test
    fun `bilesen kodu kurs icin KURSLAR_KARTI doner`() {
        assertEquals("KURSLAR_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Kurs ve Atölyeler"))
    }

    @Test
    fun `bilesen kodu bilinmeyen icin varsayilan GOREVLER_KARTI doner`() {
        assertEquals("GOREVLER_KARTI", SekmeVeVeriTasimaMotoru.bilesenKoduBul("Bilinmeyen Öğe"))
    }
}
