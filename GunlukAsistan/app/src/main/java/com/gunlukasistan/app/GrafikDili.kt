package com.gunlukasistan.app

import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * v9.9 — Grafiklerin ortak görsel dili (görsel öneri 10).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN — SEKİZ GRAFİK, SEKİZ FARKLI DİL
 * ══════════════════════════════════════════════════════════════════
 * Uygulamada sekiz ayrı grafik View'ı var ve her biri farklı bir
 * zamanda, farklı ihtiyaçla yazıldı:
 *
 *   BarChartView · SparklineView · StatRingView · DagilimHalkasi
 *   YilIsiView · HabitGridView · AgacCizgiView · NetChartView
 *
 * Kodu taradığımda çıkan tablo:
 *
 * | Ne | Bulunan değerler |
 * |---|---|
 * | Yazı boyutu | 10f, 11f, 12f × yoğunluk — her grafikte farklı |
 * | Çizgi kalınlığı | 1f, 2f, 2.5f, 3f, stroke*2.1f |
 * | Animasyon süresi | 600, 700, 800, 900 ms |
 * | Izgara rengi | `#16232F`, `#1B2A3A` — **SERT KODLANMIŞ** |
 * | Vurgu rengi | `0xFF2BCFD0` varsayılan — temadan bağımsız |
 *
 * ── 🔴 En ciddi bulgu ──
 * `SparklineView` ızgara rengi olarak `Color.parseColor("#16232F")`
 * kullanıyor. Bu **koyu lacivert** bir renk. Uygulama v8.3'te açık
 * temayı da desteklemeye başladı ama bu değer değişmedi:
 *
 *   · Koyu temada  → doğru görünüyor (arka plan koyu)
 *   · Açık temada  → beyaz zeminde koyu lacivert çizgiler
 *
 * Aynı sorun `StatRingView`'da `#1B2A3A` ile var. Bu grafikler açık
 * temada olması gerekenden çok daha koyu ve sert görünüyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * BU SINIF NE YAPIYOR
 * ══════════════════════════════════════════════════════════════════
 * Tek kaynak. Her grafik değerlerini buradan alıyor:
 *
 * ```kotlin
 * yaziBoya.textSize = GrafikDili.YAZI_KUCUK * yogunluk
 * izgaraBoya.color  = GrafikDili.izgara(this)
 * animator.duration = GrafikDili.SURE_NORMAL
 * ```
 *
 * Renkler **temadan** okunuyor, sert kodlanmıyor. Tema değişince
 * grafikler de değişiyor.
 *
 * ── Neden `object`, neden `dimens.xml` değil ──
 * Bu değerlerin çoğu Kotlin'de kullanılıyor (özel `View.onDraw`
 * içinde). XML kaynağından okumak her çizimde `resources` erişimi
 * demek; `onDraw` saniyede 60 kez çağrılıyor. Sabitler derleme
 * anında gömülüyor, maliyeti sıfır.
 */
object GrafikDili {

    // ══════════════════════════════════════════════════════════
    // Yazı boyutları (dp cinsinden — yoğunlukla çarpılacak)
    // ══════════════════════════════════════════════════════════

    /** Eksen etiketleri, küçük sayılar. */
    const val YAZI_KUCUK = 10f

    /** Değer etiketleri, dilim adları. */
    const val YAZI_ORTA = 12f

    /** Halka ortası, öne çıkan sayı. */
    const val YAZI_BUYUK = 18f

    // ══════════════════════════════════════════════════════════
    // Çizgi kalınlıkları
    // ══════════════════════════════════════════════════════════

    /** Izgara çizgileri — görünür ama dikkat çekmeyen. */
    const val CIZGI_INCE = 1f

    /** Veri çizgisi. */
    const val CIZGI_NORMAL = 2.5f

    /** Vurgulanan seri. */
    const val CIZGI_KALIN = 3.5f

    // ══════════════════════════════════════════════════════════
    // Animasyon
    // ══════════════════════════════════════════════════════════

    /**
     * Tek bir kararla üç farklı süre yerine iki süre.
     *
     * 800 ms uzun geliyordu: kullanıcı ekrana bakıyor ve grafiğin
     * "dolmasını" bekliyor. 600 ms hem akıcı hem beklemesiz.
     */
    const val SURE_NORMAL = 600L

    /** Çok öğeli grafikler (365 günlük ısı haritası) için. */
    const val SURE_UZUN = 900L

    // ══════════════════════════════════════════════════════════
    // Köşe yarıçapları
    // ══════════════════════════════════════════════════════════

    const val KOSE_CUBUK = 4f
    const val KOSE_HUCRE = 3f

    // ══════════════════════════════════════════════════════════
    // Renkler — TEMADAN okunuyor
    // ══════════════════════════════════════════════════════════

    /**
     * Izgara / iz (track) rengi.
     *
     * 🔴 Eskiden `#16232F` ve `#1B2A3A` sert kodluydu ve açık temada
     * bozuk görünüyordu. Artık `colorSurfaceVariant`'tan geliyor:
     * koyu temada koyu, açık temada açık.
     *
     * @param gorunum tema bağlamı için herhangi bir View
     */
    fun izgara(gorunum: View): Int = runCatching {
        MaterialColors.getColor(
            gorunum,
            com.google.android.material.R.attr.colorSurfaceVariant,
            0
        ).let { if (it == 0) varsayilanIzgara(gorunum) else it }
    }.getOrElse { varsayilanIzgara(gorunum) }

    /**
     * Tema okunamazsa: gece modunda mıyız diye bak.
     *
     * Sert kodlanmış tek bir değer döndürmek eski hatanın tekrarı
     * olurdu. En azından koyu/açık ayrımını yapıyoruz.
     */
    private fun varsayilanIzgara(gorunum: View): Int =
        if (geceModuMu(gorunum.context)) 0xFF2A3441.toInt() else 0xFFE3E6EA.toInt()

    /** Ana veri rengi — vurgu. */
    fun vurgu(gorunum: View): Int = runCatching {
        MaterialColors.getColor(
            gorunum, com.google.android.material.R.attr.colorPrimary, 0
        ).let { if (it == 0) 0xFF2BCFD0.toInt() else it }
    }.getOrDefault(0xFF2BCFD0.toInt())

    /** Yazı rengi — eksen etiketleri. */
    fun yazi(gorunum: View): Int = runCatching {
        MaterialColors.getColor(
            gorunum, com.google.android.material.R.attr.colorOnSurfaceVariant, 0
        ).let { if (it == 0) varsayilanYazi(gorunum) else it }
    }.getOrElse { varsayilanYazi(gorunum) }

    private fun varsayilanYazi(gorunum: View): Int =
        if (geceModuMu(gorunum.context)) 0xFFB0B8C1.toInt() else 0xFF6B7280.toInt()

    /** Öne çıkan yazı rengi — değerler. */
    fun yaziGuclu(gorunum: View): Int = runCatching {
        MaterialColors.getColor(
            gorunum, com.google.android.material.R.attr.colorOnSurface, 0
        ).let { if (it == 0) varsayilanYaziGuclu(gorunum) else it }
    }.getOrElse { varsayilanYaziGuclu(gorunum) }

    private fun varsayilanYaziGuclu(gorunum: View): Int =
        if (geceModuMu(gorunum.context)) Color.WHITE else 0xFF1F2937.toInt()

    fun geceModuMu(context: Context): Boolean = runCatching {
        val mod = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        mod == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }.getOrDefault(false)

    // ══════════════════════════════════════════════════════════
    // Durum renkleri — ANLAM bazlı
    // ══════════════════════════════════════════════════════════

    /**
     * 🔴 Kod taramasında bulunan sorun:
     *
     * "Başarı yeşili" için **5 farklı ton** kullanılmış:
     *   `4C9A5A` (12 kez) · `4CAF50` (10) · `2E7D32` (5)
     *   `66A75B` (1) · `3E8E7E` (1)
     *
     * "Hata kırmızısı" için de 5 ton:
     *   `D9534F` (25) · `E57373` (4) · `C62828` (4)
     *   `E53935` (1) · `F44336` (1)
     *
     * Aynı ekranda iki farklı yeşil görünebiliyordu. Bunlar tek
     * kaynağa bağlanıyor. En çok kullanılan tonu seçtim — böylece
     * en az sayıda yerde görsel değişiklik oluyor.
     */
    const val BASARI = 0xFF4C9A5A.toInt()
    const val UYARI = 0xFFE0A33A.toInt()
    const val HATA = 0xFFD9534F.toInt()
    const val NOTR = 0xFF7A8B99.toInt()
    const val BILGI = 0xFF3A7BD5.toInt()

    /**
     * v10.0 · Görsel öneri 2 — TEMA DUYARLI durum renkleri.
     *
     * ══════════════════════════════════════════════════════════
     * NEDEN SABİTLERİN YANINDA BİR DE FONKSİYON
     * ══════════════════════════════════════════════════════════
     * Yukarıdaki `const` değerler açık temaya göre seçilmiş.
     * v10.0'da `values-night/colors.xml` eklendi ve koyu tema için
     * **açılmış** karşılıkları tanımlandı (kontrast 2.9:1 → 4.5:1).
     *
     * Sabitler duruyor çünkü:
     *   · `Context` erişimi olmayan yerlerde (saf hesap) gerekli
     *   · Testlerde `Context` yok
     *   · Geriye dönük: 30+ çağrı yeri var
     *
     * Ama **Context varsa bu fonksiyonlar tercih edilmeli** —
     * koyu temada doğru renk geliyor.
     */
    fun basari(context: Context): Int = kaynak(context, R.color.ga_basari, BASARI)
    fun uyari(context: Context): Int = kaynak(context, R.color.ga_uyari, UYARI)
    fun hata(context: Context): Int = kaynak(context, R.color.ga_hata, HATA)
    fun notr(context: Context): Int = kaynak(context, R.color.ga_notr, NOTR)
    fun bilgi(context: Context): Int = kaynak(context, R.color.ga_bilgi, BILGI)

    /**
     * Renk kaynağını okur; başarısızsa sabite düşer.
     *
     * `ContextCompat.getColor` tema/gece moduna göre doğru
     * klasörden (values veya values-night) okuyor.
     */
    private fun kaynak(context: Context, resId: Int, varsayilan: Int): Int = runCatching {
        androidx.core.content.ContextCompat.getColor(context, resId)
    }.getOrDefault(varsayilan)

    /** Yüzdeye göre durum rengi — tema duyarlı sürüm. */
    fun durumRengi(context: Context, yuzde: Int): Int = when {
        yuzde >= 70 -> basari(context)
        yuzde >= 40 -> uyari(context)
        else -> hata(context)
    }

    /** Kategori paleti — tema duyarlı sürüm. */
    fun paletten(context: Context, indeks: Int): Int {
        val idler = intArrayOf(
            R.color.ga_kat_1, R.color.ga_kat_2, R.color.ga_kat_3, R.color.ga_kat_4,
            R.color.ga_kat_5, R.color.ga_kat_6, R.color.ga_kat_7, R.color.ga_kat_8
        )
        val i = ((indeks % idler.size) + idler.size) % idler.size
        return kaynak(context, idler[i], paletten(indeks))
    }

    /** Başarının soluk tonu — arka plan dolgusu için. */
    const val BASARI_SOLUK = 0x334C9A5A
    const val UYARI_SOLUK = 0x33E0A33A
    const val HATA_SOLUK = 0x33D9534F

    /**
     * Yüzdeye göre durum rengi.
     *
     * Tek yerde tanımlı olması şart: farklı ekranlarda "%70 iyi mi
     * kötü mü" sorusuna farklı cevap verilirse kullanıcı kafası
     * karışır.
     */
    fun durumRengi(yuzde: Int): Int = when {
        yuzde >= 70 -> BASARI
        yuzde >= 40 -> UYARI
        else -> HATA
    }

    /**
     * Alfa uygulanmış renk — dolgu alanları için.
     *
     * ── Neden `Color.argb` değil de bit işlemi ──
     * İki sebep:
     *
     *  1. **Test edilebilirlik.** Birim testlerinde `android.jar`
     *     bir saplama; `Color.argb` çağrısı
     *     "Method argb not mocked" fırlatıyor. Bu fonksiyonu test
     *     etmek istiyorsam saf Kotlin olmalı.
     *  2. **Hız.** `onDraw` içinde çağrılabiliyor. Bit kaydırma
     *     JNI sınırını geçmiyor.
     *
     * Sonuç `Color.argb` ile bit bit aynı.
     */
    fun soluk(renk: Int, alfa: Int = 40): Int {
        val a = alfa.coerceIn(0, 255)
        return (a shl 24) or (renk and 0x00FFFFFF)
    }

    // ══════════════════════════════════════════════════════════
    // Kategori paleti
    // ══════════════════════════════════════════════════════════

    /**
     * Çok serili grafiklerde kullanılacak ayırt edilebilir renkler.
     *
     * Renk körlüğü gözetilerek seçildi: sadece renk tonuna değil
     * **parlaklığa** da göre ayrışıyorlar. Kırmızı-yeşil ayrımı
     * yapamayan biri için bile dilimler farklı koyulukta.
     */
    val PALET = intArrayOf(
        0xFF3A7BD5.toInt(),   // mavi
        0xFF4C9A5A.toInt(),   // yeşil
        0xFFE0A33A.toInt(),   // amber
        0xFF8E5BA6.toInt(),   // mor
        0xFF2FA8A0.toInt(),   // turkuaz
        0xFFD9534F.toInt(),   // kırmızı
        0xFFC2568F.toInt(),   // pembe
        0xFF7A8B99.toInt()    // gri
    )

    fun paletten(indeks: Int): Int = PALET[((indeks % PALET.size) + PALET.size) % PALET.size]
}
