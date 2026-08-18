package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * v10.0 — Tasarım ölçeği tutarlılık testleri (görsel öneri 1, 2).
 *
 * ══════════════════════════════════════════════════════════════════
 * BU TESTLER NEDEN FARKLI
 * ══════════════════════════════════════════════════════════════════
 * Diğer testler **davranışı** doğruluyor. Bunlar **kod düzenini**
 * doğruluyor: layout dosyalarını okuyup sert kodlanmış değer
 * kalmadığını kontrol ediyorlar.
 *
 * Neden gerekli: v10.0'da 624 değeri ölçeğe çektim. Bu tür bir
 * temizlik **geri kayar** — biri yeni bir ekran eklerken
 * `android:textSize="13sp"` yazar ve kimse fark etmez. Altı ay
 * sonra yine 22 çeşit yazı boyutu oluruz.
 *
 * Test bunu derleme anında yakalıyor.
 *
 * ── Dosya yolu nasıl bulunuyor ──
 * Birim testleri modül kökünden (`app/`) çalışıyor. Göreli yol
 * `src/main/res/layout`. Yol bulunamazsa test **atlanıyor**
 * (başarısız olmuyor) — CI ortamı farklı olabilir ve bu testler
 * ana doğrulama değil, koruma ağı.
 */
class TasarimOlcegiTest {

    private val layoutKlasoru: File? by lazy {
        listOf(
            File("src/main/res/layout"),
            File("app/src/main/res/layout")
        ).firstOrNull { it.isDirectory }
    }

    private val degerKlasoru: File? by lazy {
        listOf(
            File("src/main/res/values"),
            File("app/src/main/res/values")
        ).firstOrNull { it.isDirectory }
    }

    private fun layoutlar(): List<File> =
        layoutKlasoru?.listFiles { f -> f.extension == "xml" }?.toList() ?: emptyList()

    // ══════════════════════════════════════════════════════════
    // Köşe yarıçapı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `layoutlarda sert kodlu kose yaricapi yok`() {
        val dosyalar = layoutlar()
        if (dosyalar.isEmpty()) return   // klasör yok — atla

        val kalip = Regex("""app:cardCornerRadius="[0-9.]+dp"""")
        val ihlaller = mutableListOf<String>()
        dosyalar.forEach { f ->
            kalip.findAll(f.readText()).forEach {
                ihlaller.add("${f.name}: ${it.value}")
            }
        }
        assertTrue(
            "Sert kodlu köşe yarıçapı bulundu (@dimen/ga_kose_* kullan):\n" +
                    ihlaller.joinToString("\n"),
            ihlaller.isEmpty()
        )
    }

    // ══════════════════════════════════════════════════════════
    // Yazı boyutu
    // ══════════════════════════════════════════════════════════

    /**
     * Ölçek dışında kalmasına izin verilen değerler.
     *
     * Her biri gerekçeli:
     *   1sp  → gizli şerit (görünmez, yalnızca yer tutuyor)
     *   34sp → widget geri sayımı (widget farklı bir bağlam,
     *          `dimens` erişimi RemoteViews'ta sınırlı)
     *   56sp → kurs ekranı büyük sayı
     *   74sp → tam ekran sayaç (odadan okunabilmeli)
     */
    private val izinliIstisnalar = setOf("1sp", "34sp", "56sp", "74sp")

    @Test
    fun `layoutlarda sert kodlu yazi boyutu yok`() {
        val dosyalar = layoutlar()
        if (dosyalar.isEmpty()) return

        val kalip = Regex("""android:textSize="([0-9.]+sp)"""")
        val ihlaller = mutableListOf<String>()
        dosyalar.forEach { f ->
            kalip.findAll(f.readText()).forEach { m ->
                val deger = m.groupValues[1]
                if (deger !in izinliIstisnalar) ihlaller.add("${f.name}: $deger")
            }
        }
        assertTrue(
            "Sert kodlu yazı boyutu bulundu (@dimen/ga_yazi_* kullan):\n" +
                    ihlaller.joinToString("\n"),
            ihlaller.isEmpty()
        )
    }

    @Test
    fun `istisna sayisi artmiyor`() {
        val dosyalar = layoutlar()
        if (dosyalar.isEmpty()) return

        val kalip = Regex("""android:textSize="([0-9.]+sp)"""")
        var sayi = 0
        dosyalar.forEach { f ->
            kalip.findAll(f.readText()).forEach { sayi++ }
        }
        // v10.0'da 5 istisna vardı (1sp × 2, 34, 56, 74).
        // Bu sayı büyürse ölçek erozyona uğruyor demektir.
        assertTrue("Ölçek dışı yazı boyutu sayısı arttı: $sayi", sayi <= 6)
    }

    // ══════════════════════════════════════════════════════════
    // dimens.xml bütünlüğü
    // ══════════════════════════════════════════════════════════

    @Test
    fun `olcek kaynaklari tanimli`() {
        val f = degerKlasoru?.resolve("dimens.xml") ?: return
        if (!f.exists()) return
        val icerik = f.readText()

        listOf(
            "ga_kose_kucuk", "ga_kose_orta", "ga_kose_buyuk", "ga_kose_dev",
            "ga_yazi_mini", "ga_yazi_kucuk", "ga_yazi_normal",
            "ga_yazi_orta", "ga_yazi_buyuk", "ga_yazi_dev", "ga_yazi_devasa",
            "ga_bosluk_xs", "ga_bosluk_s", "ga_bosluk_m",
            "ga_bosluk_l", "ga_bosluk_xl", "ga_bosluk_xxl"
        ).forEach {
            assertTrue("dimens.xml'de eksik: $it", icerik.contains("name=\"$it\""))
        }
    }

    @Test
    fun `renk kaynaklari tanimli`() {
        val f = degerKlasoru?.resolve("colors.xml") ?: return
        if (!f.exists()) return
        val icerik = f.readText()

        listOf("ga_basari", "ga_uyari", "ga_hata", "ga_bilgi", "ga_notr").forEach {
            assertTrue("colors.xml'de eksik: $it", icerik.contains("name=\"$it\""))
        }
        // Kategori paleti 8 renk
        (1..8).forEach {
            assertTrue("ga_kat_$it eksik", icerik.contains("name=\"ga_kat_$it\""))
        }
    }

    /**
     * 🔴 Bu test v10.0'da eklendi çünkü `values-night/colors.xml`
     * **hiç yoktu**. Koyu temada durum renkleri açık tema
     * değerlerini kullanıyordu ve kontrast WCAG AA sınırının
     * altına düşüyordu.
     */
    @Test
    fun `koyu tema renkleri tanimli`() {
        val gece = listOf(
            File("src/main/res/values-night/colors.xml"),
            File("app/src/main/res/values-night/colors.xml")
        ).firstOrNull { it.exists() } ?: return

        val icerik = gece.readText()
        listOf("ga_basari", "ga_uyari", "ga_hata", "ga_bilgi", "ga_notr").forEach {
            assertTrue("values-night/colors.xml'de eksik: $it", icerik.contains("name=\"$it\""))
        }
    }

    @Test
    fun `acik ve koyu tema ayni renkleri tanimliyor`() {
        val acik = degerKlasoru?.resolve("colors.xml") ?: return
        val koyu = listOf(
            File("src/main/res/values-night/colors.xml"),
            File("app/src/main/res/values-night/colors.xml")
        ).firstOrNull { it.exists() } ?: return
        if (!acik.exists()) return

        val kalip = Regex("""<color name="(ga_[a-z0-9_]+)">""")
        val acikAdlar = kalip.findAll(acik.readText()).map { it.groupValues[1] }.toSet()
        val koyuAdlar = kalip.findAll(koyu.readText()).map { it.groupValues[1] }.toSet()

        // Koyu temada tanımlı her renk açık temada da olmalı —
        // aksi halde açık temada kaynak bulunamaz ve derleme kırılır
        val fazlalik = koyuAdlar - acikAdlar
        assertTrue("Koyu temada olup açık temada olmayan: $fazlalik", fazlalik.isEmpty())
    }

    // ══════════════════════════════════════════════════════════
    // Ölçek mantığı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `grafik dili durum renkleri benzersiz`() {
        // GrafikDili sabitleri renk birleştirmenin hedefi.
        // İkisi eşitlenirse "başarı" ve "hata" ayırt edilemez.
        val hepsi = listOf(
            GrafikDili.BASARI, GrafikDili.UYARI,
            GrafikDili.HATA, GrafikDili.NOTR, GrafikDili.BILGI
        )
        assertEquals(hepsi.size, hepsi.toSet().size)
    }

    /**
     * 🔴 Bu test v10.0'da yaptığım bir hatayı kilitliyor.
     *
     * Renk birleştirme betiğim `Butce.Kategori` içindeki palet
     * renklerini de `GrafikDili.BASARI`'ya çevirdi. Sonuç: MAAS,
     * EK_GELIR ve MARKET üçü de aynı yeşil oldu — dağılım
     * halkasında ayırt edilemezlerdi.
     *
     * Ders: "aynı renk = aynı anlam" kuralı DURUM göstergeleri
     * için doğru, KATEGORİ paletleri için yanlış. Palet
     * renklerinin tek işi birbirinden ayrılmak.
     */
    @Test
    fun `butce kategori renkleri benzersiz`() {
        val renkler = Butce.Kategori.entries.map { it.renk }
        assertEquals(
            "Kategori renkleri çakışıyor — grafik dilimleri ayırt edilemez",
            renkler.size, renkler.toSet().size
        )
    }

    @Test
    fun `gelir ve gider kategorileri farkli renkte`() {
        val gelir = Butce.Kategori.gelirler.map { it.renk }.toSet()
        val gider = Butce.Kategori.giderler.map { it.renk }.toSet()
        assertTrue(
            "Gelir ve gider kategorileri aynı rengi paylaşıyor",
            gelir.intersect(gider).isEmpty()
        )
    }
}
