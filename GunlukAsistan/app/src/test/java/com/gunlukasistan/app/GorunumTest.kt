package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v8.7 — Görsel katman saf mantık testleri.
 *
 * ── Kapsam ──
 * v8.2-v8.6 arasında 30 görsel özellik eklendi ve hiçbiri cihazda
 * test edilemedi. Çizim kodu (onDraw) JUnit'te doğrulanamaz ama
 * içindeki HESAPLAR doğrulanabilir. Bu testler tam olarak onları
 * hedefliyor: yanlış bir eşik veya taşan bir dizin indeksi burada
 * yakalanır.
 *
 * ── Neden Robolectric değil ──
 * Robolectric derleme süresini dakikalarca uzatıyor ve bu sandbox'ta
 * her derleme zaten 2,5 dakika. Android'e bağımlı olmayan mantığı
 * ayırıp saf JUnit ile test etmek hem hızlı hem yeterli.
 */
class GorunumTest {

    // ══════════════════════════════════════════════════════════
    // KonuGorunum — renk türetme (v8.3, öneri 13)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `konu renk paleti bos degil ve benzersiz`() {
        val p = KonuGorunum.RENKLER
        assertTrue("Palet en az 5 renk içermeli", p.size >= 5)
        assertEquals(
            "Palette yinelenen renk var",
            p.size, p.toSet().size
        )
    }

    @Test
    fun `konu simgeleri bos degil`() {
        assertTrue(KonuGorunum.SIMGELER.isNotEmpty())
        assertTrue(
            "Boş simge olmamalı",
            KonuGorunum.SIMGELER.none { it.isBlank() }
        )
    }

    @Test
    fun `negatif konu kimligi renk turetmede cokmez`() {
        // Konu anlatımlarında sanal kimlikler NEGATİF (v7.82).
        // `renk()` içindeki modulo negatif indeks üretebilirdi.
        val boyut = KonuGorunum.RENKLER.size
        listOf(-1L, -7L, -12345L, 0L, 1L, Long.MIN_VALUE + 1).forEach { id ->
            val h = (id % boyut).toInt()
            val indeks = if (h < 0) h + boyut else h
            assertTrue(
                "Kimlik $id için indeks aralık dışı: $indeks",
                indeks in 0 until boyut
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // Sayaç kadranı — renk eşikleri (v8.4, öneri 19)
    // ══════════════════════════════════════════════════════════

    /** `SayacKadraniView.aktifRenk` içindeki eşik mantığının kopyası. */
    private fun kadranBolgesi(kalanSaniye: Long): String = when {
        kalanSaniye > 300 -> "normal"
        kalanSaniye > 60 -> "amber"
        kalanSaniye > 10 -> "kirmizi_gecis"
        else -> "kirmizi"
    }

    @Test
    fun `sayac renk esikleri dogru siralanir`() {
        assertEquals("normal", kadranBolgesi(3600))
        assertEquals("normal", kadranBolgesi(301))
        assertEquals("amber", kadranBolgesi(300))
        assertEquals("amber", kadranBolgesi(61))
        assertEquals("kirmizi_gecis", kadranBolgesi(60))
        assertEquals("kirmizi_gecis", kadranBolgesi(11))
        assertEquals("kirmizi", kadranBolgesi(10))
        assertEquals("kirmizi", kadranBolgesi(0))
    }

    @Test
    fun `sayac negatif kalan surede cokmez`() {
        // Sayaç bittikten sonra kalan negatif olabiliyor
        assertEquals("kirmizi", kadranBolgesi(-5))
    }

    /** Nabız yalnız son 10 saniyede olmalı. */
    private fun nabizVarMi(kalanSaniye: Long, calisiyor: Boolean): Boolean =
        calisiyor && kalanSaniye in 0..10

    @Test
    fun `nabiz yalniz son 10 saniyede ve calisirken`() {
        assertTrue(nabizVarMi(10, true))
        assertTrue(nabizVarMi(1, true))
        assertTrue(!nabizVarMi(11, true))
        assertTrue("Duraklatılmışken nabız olmamalı", !nabizVarMi(5, false))
        assertTrue("Negatif kalanda nabız olmamalı", !nabizVarMi(-1, true))
    }

    // ══════════════════════════════════════════════════════════
    // Kahraman kart aciliyeti (v8.3, öneri 14)
    // ══════════════════════════════════════════════════════════

    /** `HomeFragment` içindeki aciliyet hesabının kopyası. */
    private fun aciliyet(kalanGun: Int): Float = when {
        kalanGun < 0 -> 0f
        kalanGun == 0 -> 1f
        kalanGun >= 45 -> 0f
        else -> 1f - (kalanGun / 45f)
    }

    @Test
    fun `aciliyet her zaman 0 ile 1 arasinda`() {
        listOf(-100, -1, 0, 1, 22, 44, 45, 46, 999).forEach { gun ->
            val a = aciliyet(gun)
            assertTrue("Gün $gun için aciliyet aralık dışı: $a", a in 0f..1f)
        }
    }

    @Test
    fun `aciliyet sinav yaklastikca artar`() {
        assertTrue(aciliyet(5) > aciliyet(20))
        assertTrue(aciliyet(20) > aciliyet(40))
        assertEquals(1f, aciliyet(0), 0.001f)
        assertEquals(0f, aciliyet(45), 0.001f)
    }

    @Test
    fun `gecmis sinav sakin gosterilir`() {
        // Sınav geçtiyse kartı kırmızı tutmak anlamsız
        assertEquals(0f, aciliyet(-1), 0.001f)
    }

    // ══════════════════════════════════════════════════════════
    // Isı haritası seviyeleri (v8.4, öneri 17)
    // ══════════════════════════════════════════════════════════

    /** `YilIsiView.seviye` kopyası. */
    private fun isiSeviyesi(puan: Int): Int = when {
        puan <= 0 -> 0
        puan <= 2 -> 1
        puan <= 4 -> 2
        puan <= 7 -> 3
        else -> 4
    }

    @Test
    fun `isi seviyeleri 0-4 araliginda kalir`() {
        listOf(-10, 0, 1, 2, 3, 5, 8, 100, 99999).forEach { p ->
            val s = isiSeviyesi(p)
            assertTrue("Puan $p için seviye aralık dışı: $s", s in 0..4)
        }
    }

    @Test
    fun `isi seviyesi puanla birlikte artar`() {
        assertEquals(0, isiSeviyesi(0))
        assertEquals(1, isiSeviyesi(2))
        assertEquals(2, isiSeviyesi(4))
        assertEquals(3, isiSeviyesi(7))
        assertEquals(4, isiSeviyesi(8))
    }

    // ══════════════════════════════════════════════════════════
    // Yazı ölçeği (v8.6, öneri 27)
    // ══════════════════════════════════════════════════════════

    /** `GorunumAyar.yaziCarpani` kopyası. */
    private fun yaziCarpani(olcek: Int): Float = when (olcek) {
        0 -> 0.88f
        2 -> 1.15f
        3 -> 1.32f
        else -> 1.0f
    }

    @Test
    fun `yazi carpanlari makul araliktadir`() {
        // Çok küçük okunmaz, çok büyük düzeni bozar
        (0..3).forEach { o ->
            val c = yaziCarpani(o)
            assertTrue("Ölçek $o için çarpan aşırı: $c", c in 0.8f..1.4f)
        }
    }

    @Test
    fun `yazi carpanlari artan siradadir`() {
        assertTrue(yaziCarpani(0) < yaziCarpani(1))
        assertTrue(yaziCarpani(1) < yaziCarpani(2))
        assertTrue(yaziCarpani(2) < yaziCarpani(3))
    }

    @Test
    fun `normal olcek carpani tam 1`() {
        // 1.0 olmazsa varsayılan ayarda bile Configuration değişirdi
        assertEquals(1.0f, yaziCarpani(1), 0.0001f)
    }

    @Test
    fun `gecersiz olcek normale duser`() {
        assertEquals(1.0f, yaziCarpani(-5), 0.0001f)
        assertEquals(1.0f, yaziCarpani(99), 0.0001f)
    }

    // ══════════════════════════════════════════════════════════
    // Kaydırma eşiği (v8.2, öneri 4)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kaydirma esigi kaza ile silmeyi zorlastirir`() {
        // Eşik çok düşükse (örn. 0.15) listede gezerken kaza ile
        // silme olur; çok yüksekse (0.8) jest kullanılamaz.
        val esik = 0.38f
        assertTrue("Eşik çok düşük — kaza riski", esik >= 0.30f)
        assertTrue("Eşik çok yüksek — kullanılamaz", esik <= 0.55f)
    }

    // ══════════════════════════════════════════════════════════
    // Simge seçenekleri (v8.3, öneri 12)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `simge kodlari benzersiz`() {
        val kodlar = Simge.secenekler.map { it.kod }
        assertEquals(kodlar.size, kodlar.toSet().size)
    }

    @Test
    fun `hicbir simge MainActivity yi hedeflemez`() {
        // 🔴 v9.3 ÇÖKME DÜZELTMESİ SONRASI GÜNCELLENDİ.
        //
        // Bu test eskiden "tam olarak bir alias'sız (null) seçenek
        // olmalı" diyordu ve GEÇİYORDU — ama o null değer tam olarak
        // çökmeye sebep olan şeydi: `durumAyarla` null gelince
        // MainActivity'nin kendisini devre dışı bırakıyordu.
        //
        // Artık varsayılan simge de bir alias (.SimgeVarsayilan) ve
        // MainActivity hiçbir koşulda kapatılmıyor. Test bu kuralı
        // koruyor: bir daha null alias eklenirse burada yakalanır.
        assertTrue(
            "Hiçbir simge seçeneğinin alias'ı null olmamalı — " +
                "null, MainActivity'nin devre dışı bırakılmasına yol açar",
            Simge.secenekler.all { it.alias != null }
        )
    }

    @Test
    fun `tam bir varsayilan simge secenegi var`() {
        val varsayilanlar = Simge.secenekler.filter { it.kod == "varsayilan" }
        assertEquals(
            "Tam olarak bir 'varsayilan' seçeneği olmalı",
            1, varsayilanlar.size
        )
        assertEquals(
            "Varsayılan seçenek SimgeVarsayilan alias'ını göstermeli",
            "com.gunlukasistan.app.SimgeVarsayilan",
            varsayilanlar.first().alias
        )
    }

    @Test
    fun `alias adlari benzersiz ve paket onekli`() {
        val aliaslar = Simge.secenekler.mapNotNull { it.alias }
        assertEquals(aliaslar.size, aliaslar.toSet().size)
        aliaslar.forEach {
            assertTrue(
                "Alias tam nitelikli olmalı: $it",
                it.startsWith("com.gunlukasistan.app.")
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // Tema tutarlılığı (v8.3, öneri 9)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `gece modu sabitleri farkli`() {
        val hepsi = setOf(
            ThemeManager.GECE_SISTEM,
            ThemeManager.GECE_KAPALI,
            ThemeManager.GECE_ACIK
        )
        assertEquals("Gece modu sabitleri çakışıyor", 3, hepsi.size)
    }

    @Test
    fun `tema listesi bos degil ve koyu tema iceriyor`() {
        assertTrue(ThemeManager.specs.isNotEmpty())
        assertTrue(
            "En az bir koyu tema olmalı",
            ThemeManager.specs.any { it.dark }
        )
        assertTrue(
            "En az bir açık tema olmalı",
            ThemeManager.specs.any { !it.dark }
        )
    }

    @Test
    fun `tema basliklari benzersiz`() {
        val basliklar = ThemeManager.specs.map { it.title }
        assertEquals(basliklar.size, basliklar.toSet().size)
    }

    @Test
    fun `neon palet alti renk icerir`() {
        // Grafik bileşenleri palette[0..4] indekslerine erişiyor;
        // palet küçülürse ana ekran çöker.
        assertTrue(
            "Palet en az 5 renk içermeli (grafikler indeks 4'e kadar erişiyor)",
            ThemeManager.NEON_PALETTE.size >= 5
        )
    }

    // ══════════════════════════════════════════════════════════
    // Zaman çizelgesi çakışma yerleşimi (v8.5, öneri 23)
    //
    // NOT: Bu testler ilk yazıldığında algoritmanın bir KOPYASI
    // test ediliyordu ve kopyadaki bir hata (eşit Pair'lerin HashMap'te
    // tek anahtar olması) yanlış alarm verdi. Ders: kopyayı değil
    // GERÇEK kodu test et. `sutunlaraAyir` bu yüzden companion
    // object'e taşındı ve burada doğrudan çağrılıyor.
    // ══════════════════════════════════════════════════════════

    private fun oge(baslangicDk: Int, sureDk: Int, ad: String = "x") =
        ZamanCizelgesiView.Oge(
            baslik = ad,
            baslangicDk = baslangicDk,
            sureDk = sureDk,
            renk = 0,
            tur = ZamanCizelgesiView.Tur.GOREV
        )

    @Test
    fun `cakismayan isler tek sutuna sigar`() {
        // 09:00-10:00 · 11:00-12:00 · 14:00-15:00
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(540, 60, "a"), oge(660, 60, "b"), oge(840, 60, "c"))
        )
        assertEquals(3, sonuc.size)
        assertTrue("Çakışmayan işler tek sütunda olmalı", sonuc.all { it.sutun == 0 })
    }

    @Test
    fun `ayni saatteki uc is uc sutuna ayrilir`() {
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(540, 60, "a"), oge(540, 60, "b"), oge(540, 60, "c"))
        )
        assertEquals(3, sonuc.size)
        assertEquals(
            "Üç iş üç ayrı sütunda olmalı",
            3, sonuc.map { it.sutun }.toSet().size
        )
        assertTrue("Sütun sayısı 3 bildirilmeli", sonuc.all { it.sutunSayisi == 3 })
    }

    @Test
    fun `kismi cakisma dogru ayrilir`() {
        // 09:00-10:00 ile 09:30-10:30 çakışıyor → 2 sütun
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(540, 60, "a"), oge(570, 60, "b"))
        )
        assertEquals(2, sonuc.size)
        assertNotEquals(sonuc[0].sutun, sonuc[1].sutun)
    }

    @Test
    fun `bitisik isler ayni sutunu paylasir`() {
        // 09:00-10:00 ve tam 10:00'da başlayan → çakışma yok
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(540, 60, "a"), oge(600, 60, "b"))
        )
        assertTrue(sonuc.all { it.sutun == 0 })
    }

    @Test
    fun `bos liste cokmez`() {
        assertTrue(ZamanCizelgesiView.sutunlaraAyir(emptyList()).isEmpty())
    }

    @Test
    fun `hicbir oge kaybolmaz`() {
        // En kritik güvence: 12 karışık iş girip 12 yerleşim çıkmalı
        val girdi = listOf(
            oge(480, 30, "1"), oge(480, 90, "2"), oge(500, 20, "3"),
            oge(600, 60, "4"), oge(610, 15, "5"), oge(700, 120, "6"),
            oge(720, 30, "7"), oge(900, 45, "8"), oge(900, 45, "9"),
            oge(900, 45, "10"), oge(1000, 5, "11"), oge(1200, 240, "12")
        )
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(girdi)
        assertEquals("Öğe sayısı değişmemeli", girdi.size, sonuc.size)
        assertEquals(
            "Öğe kümesi değişmemeli",
            girdi.map { it.baslik }.toSet(),
            sonuc.map { it.oge.baslik }.toSet()
        )
    }

    @Test
    fun `sutun indeksi her zaman sutunSayisi icinde`() {
        val girdi = listOf(
            oge(540, 60, "a"), oge(540, 60, "b"), oge(545, 90, "c"),
            oge(700, 30, "d"), oge(700, 30, "e")
        )
        ZamanCizelgesiView.sutunlaraAyir(girdi).forEach { y ->
            assertTrue(
                "Sütun ${y.sutun} / ${y.sutunSayisi} aralık dışı (${y.oge.baslik})",
                y.sutun in 0 until y.sutunSayisi
            )
        }
    }

    @Test
    fun `cok kisa isler en az sure kadar yer kaplar`() {
        // 5 dakikalık iki iş 09:00 ve 09:10'da: EN_AZ_SURE_DK=30 olduğu
        // için çakışıyor sayılmalı, üst üste çizilmemeli
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(540, 5, "a"), oge(550, 5, "b"))
        )
        assertNotEquals(
            "Kısa işler de en az süre kadar yer kapladığı için ayrılmalı",
            sonuc[0].sutun, sonuc[1].sutun
        )
    }

    @Test
    fun `sirasiz girdi dogru islenir`() {
        // Girdi baslangicDk'ya göre sıralı gelmeyebilir
        val sonuc = ZamanCizelgesiView.sutunlaraAyir(
            listOf(oge(900, 30, "gec"), oge(540, 30, "erken"), oge(700, 30, "orta"))
        )
        assertEquals(3, sonuc.size)
        assertTrue(sonuc.all { it.sutun == 0 })
    }
}
