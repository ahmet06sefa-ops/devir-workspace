package com.gunlukasistan.app

import android.content.Context
import android.widget.RemoteViews
import kotlin.math.roundToInt

/**
 * v10.16 · KULLANICI İSTEĞİ — Widget Atölyesi (aşırı düzenleme katmanı).
 *
 * ── İstek ──
 * "Yazı boyutları, genişlik, metin aralıkları, widget birleştirebilme —
 * her şeyini ben ayarlayabileyim."
 *
 * ── Dürüst kapsam (RemoteViews'un teknik sınırları) ──
 * · YAZI BOYUTU: v10.16'da %75–%150 kaydırıcıydı; v10.20'de kullanıcının
 *   "sınır koyma" isteğiyle TAM SERBEST tam sayı yüzde oldu (taban %1,
 *   tavan yok). Eski widget (15) + yeni aile merkez `WidgetCommon.yaziOlcek`
 *   ile ölçeklenir.
 * · GENİŞLİK: widget genişliğini LAUNCHER belirler (hücre ızgarası);
 *   uygulama dışarıdan genişlik değiştiremez. Değiştirilebilen şey
 *   İÇ ÇERÇEVE boşluğudur: "İç dolgu" 0–3 kademe → bütün widget'ların
 *   kök görünümüne padding işler (v10.16'da merkez `WidgetTema.saydamlikUygula`
 *   idi; v10.20'de tüm zeminler bitmap yoluna geçince merkez `WidgetZemin.uygula`
 *   oldu). Yeni ailede içerik ve zemin
 *   birlikte içeri çekilir (genişlik hissi), eski ailede kart sabit
 *   kalıp içerik nefeslenir — notta açıklanır.
 * · METİN ARALIKLARI: RemoteViews'ta satır aralığı (lineSpacing) API'si
 *   YOKTUR (android.jar doğrulandı). Sunulan: "satır nefesi" 0–2 kademe
 *   → metin satırlarının dikey dolgusu (yeni aile + Birleştirici'nin
 *   tüm satırlarına otomatik işler).
 * · BİRLEŞTİRME: `ModulWidget` — modüller (saat, sayaç, görevler, seri,
 *   uyku, kapı, kronotip) kullanıcı sırasıyla tek widget'ta; ⚙ ile
 *   örnek başına düzenlenir.
 *
 * Saf tablolar birim testlidir.
 */
object WidgetAtolye {

    private const val PREF = "widget_atolye_v1"
    private const val K_YAZI = "yazi_yuzde"
    private const val K_DOLGU = "dolgu_kademe"
    private const val K_SATIR = "satir_kademe"

    // ---------------- Yazı boyutu: %75–%150 (%5 adım) ----------------

    const val YAZI_MIN = 75
    const val YAZI_MAKS = 150
    const val YAZI_ADIM = 5

    /** Eski 3 kademeli sistemin yüzde karşılığı (migration tablosu). */
    fun kademeToYuzde(kademe: Int): Int = when (kademe) {
        0 -> 85; 2 -> 115; else -> 100
    }

    /** %5'e yuvarlanmış ve kelepçelenmiş yüzde. */
    fun yuzdeSnap(v: Int): Int {
        val sn = ((v - YAZI_MIN + YAZI_ADIM / 2) / YAZI_ADIM) * YAZI_ADIM + YAZI_MIN
        return sn.coerceIn(YAZI_MIN, YAZI_MAKS)
    }

    fun yaziYuzde(context: Context): Int {
        val sp = context.getSharedPreferences(PREF, 0)
        if (!sp.contains(K_YAZI)) {
            // Migration: v10.13'ün "yazi" kademesi (0/1/2) varsa yüzdeye taşı
            val eski = context.getSharedPreferences("widget_tema_v1", 0)
                .getInt("yazi", 1).coerceIn(0, 2)
            return kademeToYuzde(eski)
        }
        // v10.20 · KULLANICI İSTEĞİ — "sınır koyma": %75-150 aralığı ve
        // %5 adım KALDIRILDI. Yazılan değer aynen saklanır; tek kelepçe
        // okuma tabanıdır (%1) çünkü negatif/sıfır sp değeri setTextSize'ı
        // başlatıcıda çökertir. ÜST SINIR YOK. Saf kelepçe: guvenliOlcekYuzde.
        return guvenliOlcekYuzde(sp.getInt(K_YAZI, 100))
    }

    /** v10.20 · Serbest yazı yüzdesi güvenlik tabanı. Saf — birim testli. */
    fun guvenliOlcekYuzde(v: Int): Int = v.coerceAtLeast(1)

    fun setYaziYuzde(context: Context, v: Int) {
        val serbest = v.coerceAtLeast(1)
        context.getSharedPreferences(PREF, 0).edit().putInt(K_YAZI, serbest).apply()
        WidgetCommon.yaziOlcek = serbest / 100f
    }

    fun yaziCarpan(context: Context): Float = yaziYuzde(context) / 100f

    // ---------------- İç dolgu: 0..3 ----------------

    /** Kademe → kök dolgusu (dp). */
    fun kokDolguDp(kademe: Int): Int = when (kademe.coerceIn(0, 3)) {
        0 -> 0; 1 -> 2; 2 -> 6; else -> 12
    }

    fun dolguKademe(context: Context): Int =
        context.getSharedPreferences(PREF, 0).getInt(K_DOLGU, 1).coerceIn(0, 3)

    fun setDolguKademe(context: Context, k: Int) {
        context.getSharedPreferences(PREF, 0).edit()
            .putInt(K_DOLGU, k.coerceIn(0, 3)).apply()
    }

    // ---------------- v10.20 · Serbest dp değerleri (kademe bitti) ----------------

    private const val K_DOLGU_DP = "dolgu_dp"
    private const val K_SATIR_DP = "satir_dp"

    /**
     * Kök iç dolgusu — serbest dp (0'dan itibaren sınırsız; launcher hücreyi
     * kendi sınırlar, negatif padding başlatıcıyı bozduğundan tek taban 0).
     * Eski kademe ilk okumada tablo karşılığı dp'ye taşınır.
     */
    fun kokDolguDp(context: Context): Int {
        val sp = context.getSharedPreferences(PREF, 0)
        if (sp.contains(K_DOLGU_DP)) return sp.getInt(K_DOLGU_DP, 2).coerceAtLeast(0)
        return kokDolguDp(dolguKademe(context))
    }

    fun setDolguDp(context: Context, dp: Int) {
        context.getSharedPreferences(PREF, 0).edit()
            .putInt(K_DOLGU_DP, dp.coerceAtLeast(0)).apply()
    }

    /** Satır nefesi — serbest dp (eski 0/2/6 tablosundan taşınır). */
    fun satirDolguDp(context: Context): Int {
        val sp = context.getSharedPreferences(PREF, 0)
        if (sp.contains(K_SATIR_DP)) return sp.getInt(K_SATIR_DP, 2).coerceAtLeast(0)
        return satirDolguDp(satirKademe(context))
    }

    fun setSatirDp(context: Context, dp: Int) {
        context.getSharedPreferences(PREF, 0).edit()
            .putInt(K_SATIR_DP, dp.coerceAtLeast(0)).apply()
    }

    fun kokDolguPx(context: Context): Int =
        (kokDolguDp(context) * context.resources.displayMetrics.density).roundToInt()

    // ---------------- Satır nefesi: 0..2 ----------------

    /** Kademe → satır dikey dolgusu (dp). */
    fun satirDolguDp(kademe: Int): Int = when (kademe.coerceIn(0, 2)) {
        0 -> 0; 1 -> 2; else -> 6
    }

    fun satirKademe(context: Context): Int =
        context.getSharedPreferences(PREF, 0).getInt(K_SATIR, 1).coerceIn(0, 2)

    fun setSatirKademe(context: Context, k: Int) {
        context.getSharedPreferences(PREF, 0).edit()
            .putInt(K_SATIR, k.coerceIn(0, 2)).apply()
    }

    fun satirDolguPx(context: Context): Int =
        (satirDolguDp(context) * context.resources.displayMetrics.density).roundToInt()

    // ---------------- Uygulayıcılar ----------------

    /**
     * Kök dolgusunu verilen görünüme uygular (serbest dp; yatayı ayrıca
     * `WidgetSecim.yatayKatsayi` ile katsayılar).
     * `WidgetZemin.uygula` bunu dolguKokId verildiğinde işletir →
     * eski 12 widget + yeni aile tek enjeksiyondan kazanır.
     */
    fun kokDolguUygula(views: RemoteViews, id: Int, context: Context) {
        try {
            val px = kokDolguPx(context)
            // v10.17: yatay dolgu oranı — dikey px korunur, yatay katsayılanır
            val yx = (px * WidgetSecim.yatayKatsayi(context)).roundToInt()
            views.setViewPadding(id, yx, px, yx, px)
        } catch (e: Exception) {
            android.util.Log.w("WidgetAtolye", "Kök dolgu uygulanamadı", e)
        }
    }

    /** Satır nefesini metin satırına uygular (dikey dolgu + v10.17 sol girinti). */
    fun satirDolguUygula(views: RemoteViews, id: Int, context: Context) {
        try {
            val py = satirDolguPx(context)
            // v10.17: satır başı girintisi (varsayılan 0 = eski davranış)
            val gx = WidgetSecim.girintiPx(context)
            views.setViewPadding(id, gx, py, 0, py)
        } catch (e: Exception) {
            android.util.Log.w("WidgetAtolye", "Satır dolgu uygulanamadı", e)
        }
    }
}
