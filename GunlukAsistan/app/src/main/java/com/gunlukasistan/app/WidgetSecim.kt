package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * v10.17 · KULLANICI İSTEĞİ — Widget Ayar Envanteri (33 yeni ayar).
 * v10.20 · KULLANICI İSTEĞİ — "Uzarlık ve boyutlarda SINIR KOYMA":
 *   kademe sistemi serbest değere çevrildi (yatay %, girinti dp, kısıt ms,
 *   karartma/canlılık/kontrast %), üstüne özel renk şablonu (serbest hex)
 *   ve örnek-başına yazı ölçeği eklendi. Eski kademe anahtarları korunur,
 *   ilk okumada taşınır.
 *
 * ── İstek ──
 * "Daha fazla widget ayarlaması yap, en az 30 adet widget ayarı ekle.
 *  Yeni widget istemiyorum — ayarlarını değiştirmek istiyorum."
 *
 * ── Mimari ──
 * Yeni widget YOK; mevcut 20 sağlayıcının üstüne üç katman işlendi:
 *
 *  1) MERKEZİ RENK İŞLEMESİ (11 ayar) — `WidgetTema.palet` dönüşünden
 *     hemen önce [uygula] çalışır: metin rengi modu, vurgu canlılığı,
 *     tamamlanan rengi, yüksek kontrast ve gece karartması paleti
 *     baştan yazar. Paleti kullanan 20 widget'ın hepsi (bitmap zemin
 *     üretenler dahil) tek seferde kazanır — sağlayıcılara tek tek
 *     dokunmaya gerek kalmaz.
 *
 *  2) BOŞLUK & DAVRANIŞ (4 ayar) — yatay dolgu oranı ve satır girintisi
 *     mevcut enjeksiyon noktalarında (`WidgetAtolye.kokDolguUygula`,
 *     `satirDolguUygula`); tazeleme kısıtı `WidgetCommon.refreshAll`'da.
 *
 *  3) WIDGET BAZLI GÖRÜNÜRLÜK (22 anahtar) — 7 widget'ın parçaları
 *     (etiket, emoji, kutular, düğmeler, grafik çizgileri…) teker teker
 *     açılıp kapatılır. Varsayılan her şey AÇIK = eski davranış.
 *
 * ── Varsayılanlar eski davranışı korur ──
 * metin modu=0 (otomatik) · canlılık=1 (normal) · tamam rengi=0 (yeşil)
 * kontrast=kapalı · karartma=kapalı (22:00→07:00, orta) · yatay=1 (×1.0)
 * girinti=0 · kısıt=0 (400 ms) · hedef=% · tüm görünürlükler=açık.
 *
 * ── Test edilebilirlik ──
 * Renk matematiği `android.graphics.Color` YERİNE bit işlemleriyle
 * yazıldı — saf fonksiyonlar JVM birim testinde çalışır (android.jar
 * stub tuzağına düşmez).
 */
object WidgetSecim {

    private const val TAG = "WidgetSecim"
    private const val PREF = "wg_secim_v1"

    // ── Anahtarlar: merkezi renk işlemesi ──
    private const val K_METIN = "metin_mod"        // 0..3
    private const val K_CANLI = "vurgu_canli"      // 0..2
    private const val K_TAMAM = "tamam_renk"       // 0..3
    private const val K_KONTRAST = "kontrast"      // bool
    private const val K_KARART = "karart"          // bool
    private const val K_BAS = "karart_bas"         // 0..23 saat
    private const val K_BIT = "karart_bit"         // 0..23 saat
    private const val K_SIDDET = "karart_siddet"   // 0..2
    // ── Anahtarlar: boşluk & davranış ──
    private const val K_YATAY = "yatay"            // 0..2
    private const val K_GIRINTI = "girinti"        // 0..2
    private const val K_KISIT = "kisit"            // 0..2
    private const val K_HD_MOD = "hd_mod"          // 0..1

    // ── Anahtarlar: widget bazlı görünürlük (varsayılan TRUE) ──
    const val W_CD_ETIKET = "w_cd_etiket"
    const val W_CD_EMOJI = "w_cd_emoji"
    const val W_SUM_SELAM = "w_sum_selam"
    const val W_SUM_GERI = "w_sum_geri"
    const val W_SUM_KUTU = "w_sum_kutu"
    const val W_SUM_SERI = "w_sum_seri"
    const val W_ACT_ODAK = "w_act_odak"
    const val W_ACT_SORU = "w_act_soru"
    const val W_ACT_GOREV = "w_act_gorev"
    const val W_ACT_BUGUN = "w_act_bugun"
    const val W_ACT_SES = "w_act_ses"
    const val W_SY_PRESET = "w_sy_preset"
    const val W_SY_SIFIRLA = "w_sy_sifirla"
    const val W_SY_BAR = "w_sy_bar"
    const val W_HD_ALT = "w_hd_alt"
    const val W_NW_AD = "w_nw_ad"
    const val W_NW_KALAN = "w_nw_kalan"
    const val W_UY_ORT = "w_uy_ort"
    const val W_UY_HEDEF = "w_uy_hedef"
    const val W_UY_PLAN = "w_uy_plan"
    const val W_UY_HARF = "w_uy_harf"
    // v10.21: başlık çubukları gizle/göster
    const val W_TW_BASLIK = "w_tw_baslik"
    const val W_EV_BASLIK = "w_ev_baslik"
    const val W_UY_BASLIK = "w_uy_baslik"
    const val W_HW_BASLIK = "w_hw_baslik"

    /** Birim testinde benzersizlik denetlenir. */
    val GORUNURLUK_ANAHTARLARI = listOf(
        W_CD_ETIKET, W_CD_EMOJI,
        W_SUM_SELAM, W_SUM_GERI, W_SUM_KUTU, W_SUM_SERI,
        W_ACT_ODAK, W_ACT_SORU, W_ACT_GOREV, W_ACT_BUGUN, W_ACT_SES,
        W_SY_PRESET, W_SY_SIFIRLA, W_SY_BAR,
        W_HD_ALT,
        W_NW_AD, W_NW_KALAN,
        W_UY_ORT, W_UY_HEDEF, W_UY_PLAN, W_UY_HARF,
        W_TW_BASLIK, W_EV_BASLIK, W_UY_BASLIK, W_HW_BASLIK
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // GETTER / SETTER (kelepçeli — bozuk değer hiçbir zaman dışarı çıkmaz)
    // ═══════════════════════════════════════════════════════════════

    fun metinMod(c: Context): Int = prefs(c).getInt(K_METIN, 0).coerceIn(0, 3)
    fun setMetinMod(c: Context, v: Int) { prefs(c).edit().putInt(K_METIN, v.coerceIn(0, 3)).apply() }

    fun canlilik(c: Context): Int = prefs(c).getInt(K_CANLI, 1).coerceIn(0, 2)
    fun setCanlilik(c: Context, v: Int) { prefs(c).edit().putInt(K_CANLI, v.coerceIn(0, 2)).apply() }

    fun tamamMod(c: Context): Int = prefs(c).getInt(K_TAMAM, 0).coerceIn(0, 3)
    fun setTamamMod(c: Context, v: Int) { prefs(c).edit().putInt(K_TAMAM, v.coerceIn(0, 3)).apply() }

    fun kontrast(c: Context): Boolean = prefs(c).getBoolean(K_KONTRAST, false)
    fun setKontrast(c: Context, v: Boolean) { prefs(c).edit().putBoolean(K_KONTRAST, v).apply() }

    fun karartAcik(c: Context): Boolean = prefs(c).getBoolean(K_KARART, false)
    fun setKarartAcik(c: Context, v: Boolean) { prefs(c).edit().putBoolean(K_KARART, v).apply() }

    fun basSaat(c: Context): Int = prefs(c).getInt(K_BAS, 22).coerceIn(0, 23)
    fun setBasSaat(c: Context, v: Int) { prefs(c).edit().putInt(K_BAS, v.coerceIn(0, 23)).apply() }

    fun bitSaat(c: Context): Int = prefs(c).getInt(K_BIT, 7).coerceIn(0, 23)
    fun setBitSaat(c: Context, v: Int) { prefs(c).edit().putInt(K_BIT, v.coerceIn(0, 23)).apply() }

    fun siddet(c: Context): Int = prefs(c).getInt(K_SIDDET, 1).coerceIn(0, 2)
    fun setSiddet(c: Context, v: Int) { prefs(c).edit().putInt(K_SIDDET, v.coerceIn(0, 2)).apply() }

    fun yatayKademe(c: Context): Int = prefs(c).getInt(K_YATAY, 1).coerceIn(0, 2)
    fun setYatayKademe(c: Context, v: Int) { prefs(c).edit().putInt(K_YATAY, v.coerceIn(0, 2)).apply() }

    fun girintiKademe(c: Context): Int = prefs(c).getInt(K_GIRINTI, 0).coerceIn(0, 2)
    fun setGirintiKademe(c: Context, v: Int) { prefs(c).edit().putInt(K_GIRINTI, v.coerceIn(0, 2)).apply() }

    fun kisitKademe(c: Context): Int = prefs(c).getInt(K_KISIT, 0).coerceIn(0, 2)
    fun setKisitKademe(c: Context, v: Int) { prefs(c).edit().putInt(K_KISIT, v.coerceIn(0, 2)).apply() }

    fun hedefMod(c: Context): Int = prefs(c).getInt(K_HD_MOD, 0).coerceIn(0, 1)
    fun setHedefMod(c: Context, v: Int) { prefs(c).edit().putInt(K_HD_MOD, v.coerceIn(0, 1)).apply() }

    /** Parça görünürlüğü — varsayılan TRUE (eski davranış korunur). */
    fun goster(c: Context, anahtar: String): Boolean = prefs(c).getBoolean(anahtar, true)
    fun setGoster(c: Context, anahtar: String, v: Boolean) {
        prefs(c).edit().putBoolean(anahtar, v).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.20 · SINIRSIZ KONTROL — KULLANICI İSTEĞİ:
    // "Yazı boyutları ve aralıkları şablonunu tamamen değiştirebileyim,
    //  her şeyin yetkisi ayarlarda olsun — SINIR KOYMA."
    // Eski kademeler korunur (ilk okumada taşınır); yeni anahtar yazıldığı
    // anda serbest değer devralır.
    // ═══════════════════════════════════════════════════════════════

    private const val K_YATAY_PCT = "yatay_pct"
    private const val K_GIRINTI_DP = "girinti_dp"
    private const val K_KISIT_MS = "kisit_ms"
    private const val K_SIDDET_PCT = "karart_siddet_pct"
    private const val K_CANLI_PCT = "vurgu_canli_pct"
    private const val K_KONTRAST_PCT = "kontrast_pct"

    // ── Özel renk şablonu anahtarları (UI da kullanır — dışa açık) ──
    const val K_OZ_ZEMIN = "oz_zemin"
    const val K_OZ_METIN = "oz_metin"
    const val K_OZ_VURGU = "oz_vurgu"
    const val K_OZ_YESIL = "oz_yesil"

    // ── Örnek-başına yazı ölçeği anahtarları (yüzde; 100 = genel ayar) ──
    const val ORNEK_CD = "oz_cd"
    const val ORNEK_SUM = "oz_sum"
    const val ORNEK_ACT = "oz_act"
    const val ORNEK_SY = "oz_sy"
    const val ORNEK_HD = "oz_hd"
    const val ORNEK_NW = "oz_nw"
    const val ORNEK_UY = "oz_uy"

    /** Karartma şiddeti SERBEST yüzde (0-100 fiziksel aralık = tam yetki). */
    fun siddetPct(c: Context): Int =
        if (prefs(c).contains(K_SIDDET_PCT))
            prefs(c).getInt(K_SIDDET_PCT, 40).coerceIn(0, 100)
        else (karartFaktor(siddet(c)) * 100f).toInt()

    fun setSiddetPct(c: Context, pct: Int) {
        prefs(c).edit().putInt(K_SIDDET_PCT, pct.coerceIn(0, 100)).apply()
    }

    /** Yüzde → karışım faktörü. Saf. */
    fun karartFaktorPct(pct: Int): Float = pct.coerceIn(0, 100) / 100f

    /** Vurgu canlılığı SERBEST yüzde. Eski kademeler: 0→42 · 1→100 · 2→135. */
    fun canliPct(c: Context): Int =
        if (prefs(c).contains(K_CANLI_PCT))
            prefs(c).getInt(K_CANLI_PCT, 100).coerceAtLeast(1)
        else when (canlilik(c)) { 0 -> 42; 2 -> 135; else -> 100 }

    fun setCanliPct(c: Context, pct: Int) {
        prefs(c).edit().putInt(K_CANLI_PCT, pct.coerceAtLeast(1)).apply()
    }

    /**
     * Serbest canlılık matematiği:
     *  <100 → vurgu zemine doğru yumuşar (100-pct) % × 0.6 kadar
     *   100 → dokunulmaz · >100 → doygunluk ölçeği (pct-100)% artar.
     * `canlandir` içindeki 0.5-2 aralığı taşma korumasıdır. Saf.
     */
    fun canlandirPct(vurgu: Int, zemin: Int, pct: Int): Int = when {
        pct < 100 -> karistirUc(vurgu, zemin, (100 - pct) / 100f * 0.6f)
        pct == 100 -> vurgu
        else -> canlandir(vurgu, 1f + (pct - 100) / 100f)
    }

    /** Yüksek kontrast SERBEST yüzde (0 = kapalı; 100 = eski "açık" davranışı). */
    fun kontrastPct(c: Context): Int =
        if (prefs(c).contains(K_KONTRAST_PCT))
            prefs(c).getInt(K_KONTRAST_PCT, 0).coerceIn(0, 100)
        else if (kontrast(c)) 100 else 0

    fun setKontrastPct(c: Context, pct: Int) {
        prefs(c).edit().putInt(K_KONTRAST_PCT, pct.coerceIn(0, 100)).apply()
    }

    /** Yüzdeli kontrast — 100 değeri eski anahtarlı sürümle birebir aynı sonucu üretir. Saf. */
    fun kontrastUygula(p: WidgetTema.Palet, pct: Int): WidgetTema.Palet {
        val t = pct.coerceIn(0, 100) / 100f
        if (t <= 0f) return p
        val yeniMetin = karistirUc(p.metin, if (p.koyuMu) BEYAZ else SIYAH, 0.35f * t)
        return p.copy(
            metin = yeniMetin,
            metinSoluk = karistirUc(p.metinSoluk, yeniMetin, 0.50f * t)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.20 · ÖZEL RENK ŞABLONU — kullanıcı kendi paletini yazar
    // ═══════════════════════════════════════════════════════════════

    /**
     * "#RRGGBB" veya "#AARRGGBB" çözer ('#' şart değil, boşluk/ büyük-küçük
     * harf serbest). Geçersizse null. Bit işlemi — android.jar stub tuzağı
     * yok, JVM birim testli. Saf.
     */
    fun hexOku(s: String?): Int? {
        if (s == null) return null
        val h = s.trim().removePrefix("#").trim()
        if (h.length != 6 && h.length != 8) return null
        val v = h.toLongOrNull(16) ?: return null
        return if (h.length == 6) (0xFF shl 24) or v.toInt() else v.toInt()
    }

    /** Renk → "#RRGGBB" (alfa kesri atılır). Saf. */
    fun hexYaz(c: Int): String =
        "#" + (c and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

    /** Kayıtlı özel rengi okur (yoksa null = otomatik). */
    fun ozelRenk(c: Context, anahtar: String): Int? =
        hexOku(prefs(c).getString(anahtar, null))

    fun setOzelRenk(c: Context, anahtar: String, renk: Int?) {
        prefs(c).edit()
            .putString(anahtar, if (renk == null) null else hexYaz(renk)).apply()
    }

    /**
     * Özel renk şablonu — dönüşüm boru hattının EN SON halkası.
     * Kullanıcının yazdığı renk karartma/kontrast/metin-modu gibi her
     * dönüşümün ÜSTÜNDE durur (tam yetki ilkesi). Türev renkler
     * (soluk metin, vurgu soluğu, zemin-alt) otomatik üretilir. Saf.
     */
    fun ozRenkleriUygula(
        p: WidgetTema.Palet,
        metin: Int? = null,
        zemin: Int? = null,
        vurgu: Int? = null,
        yesil: Int? = null
    ): WidgetTema.Palet {
        var c = p
        if (zemin != null) {
            val koyu = parlaklikUc(zemin) < 0.5f
            c = c.copy(
                zemin = zemin,
                zeminAlt = karistirUc(zemin, if (koyu) BEYAZ else SIYAH, 0.07f),
                koyuMu = koyu
            )
        }
        if (vurgu != null) {
            c = c.copy(
                vurgu = vurgu,
                vurguSoluk = karistirUc(c.zemin, vurgu, if (c.koyuMu) 0.24f else 0.18f)
            )
        }
        if (metin != null) {
            c = c.copy(
                metin = metin,
                metinSoluk = karistirUc(metin, c.zemin, 0.35f)
            )
        }
        if (yesil != null) c = c.copy(yesil = yesil)
        return c
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.20 · ÖRNEK-BAŞINA YAZI ÖLÇEĞİ (genel yüzdenin üstüne çarpılır)
    // ═══════════════════════════════════════════════════════════════

    /** Örnek ölçeği yüzdesi (varsayılan 100). Taban %1, tavan yok. */
    fun ornekPct(c: Context, anahtar: String): Int =
        ornekOlcekGuvenli(prefs(c).getInt(anahtar, 100))

    fun setOrnekPct(c: Context, anahtar: String, pct: Int) {
        prefs(c).edit().putInt(anahtar, ornekOlcekGuvenli(pct)).apply()
    }

    /** Örnek faktörü: yüzde/100 çarpan. */
    fun ornekFaktor(c: Context, anahtar: String): Float =
        ornekPct(c, anahtar) / 100f

    /** Ölçek güvenlik tabanı — Saf. */
    fun ornekOlcekGuvenli(pct: Int): Int = pct.coerceAtLeast(1)

    // ═══════════════════════════════════════════════════════════════
    // SAF TABLOLAR (JVM testli)
    // ═══════════════════════════════════════════════════════════════

    /** Kademe → yatay dolgu katsayısı (dikey kök dolguyla çarpılır). */
    fun yatayKatsayi(kademe: Int): Float = when (kademe.coerceIn(0, 2)) {
        0 -> 0.5f; 2 -> 1.8f; else -> 1.0f
    }

    /**
     * v10.20: yatay katsayı SERBEST yüzde (0 = yatay dolgu yok; üst sınır
     * yok). Eski 3 kademeli anahtar ilk okumada yüzdeye taşınır.
     */
    fun yatayPct(c: Context): Int =
        if (prefs(c).contains(K_YATAY_PCT))
            prefs(c).getInt(K_YATAY_PCT, 100).coerceAtLeast(0)
        else (yatayKatsayi(yatayKademe(c)) * 100f).toInt()

    fun setYatayPct(c: Context, pct: Int) {
        prefs(c).edit().putInt(K_YATAY_PCT, pct.coerceAtLeast(0)).apply()
    }

    fun yatayKatsayi(c: Context): Float = yatayPct(c) / 100f

    /** Kademe → satır başı girintisi (dp). */
    fun girintiDp(kademe: Int): Int = when (kademe.coerceIn(0, 2)) {
        0 -> 0; 1 -> 4; else -> 10
    }

    /** v10.20: satır girintisi SERBEST dp (tek taban 0 — padding negatifi
     *  başlatıcıyı bozar, üst sınır yok). */
    fun girintiDpC(c: Context): Int =
        if (prefs(c).contains(K_GIRINTI_DP))
            prefs(c).getInt(K_GIRINTI_DP, 0).coerceAtLeast(0)
        else girintiDp(girintiKademe(c))

    fun setGirintiDp(c: Context, dp: Int) {
        prefs(c).edit().putInt(K_GIRINTI_DP, dp.coerceAtLeast(0)).apply()
    }

    fun girintiPx(c: Context): Int =
        (girintiDpC(c) * c.resources.displayMetrics.density).roundToInt()

    /** Kademe → refreshAll kısıtı (ms). Ardışık tazelemeleri bastırır. */
    fun kisitMs(kademe: Int): Long = when (kademe.coerceIn(0, 2)) {
        0 -> 400L; 1 -> 2_000L; else -> 10_000L
    }

    /**
     * v10.20: tazeleme kısıtı SERBEST ms — 0 = hiç bastırma (her veri
     * değişimi anında widget'a yansır; pil pahasına, tam yetki).
     */
    fun kisitMs(c: Context): Long =
        if (prefs(c).contains(K_KISIT_MS))
            prefs(c).getInt(K_KISIT_MS, 400).coerceAtLeast(0).toLong()
        else kisitMs(kisitKademe(c))

    fun setKisitMs(c: Context, ms: Int) {
        prefs(c).edit().putInt(K_KISIT_MS, ms.coerceAtLeast(0)).apply()
    }

    /** Şiddet → karartma oranı (zeminin siyaha karışma payı). */
    fun karartFaktor(kademe: Int): Float = when (kademe.coerceIn(0, 2)) {
        0 -> 0.20f; 2 -> 0.60f; else -> 0.40f
    }

    /**
     * Gece karartması penceresi dakika cinsinden denetlenir.
     * Gece sarması desteklenir: 22:00 → 07:00 gibi başlangıç > bitiş
     * olduğunda pencere gece yarısını aşar. Başlangıç == bitiş = kapalı
     * pencere (hiç aktif olmaz) — "tüm gün karart" tuzağına düşülmez.
     */
    fun karartmaAktifMi(simdiDk: Int, basSaat: Int, bitSaat: Int): Boolean {
        val bas = basSaat.coerceIn(0, 23) * 60
        val bit = bitSaat.coerceIn(0, 23) * 60
        val m = ((simdiDk % 1440) + 1440) % 1440
        if (bas == bit) return false
        return if (bas < bit) m in bas until bit else m >= bas || m < bit
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF RENK MATEMATİĞİ (bit işlemi — android.graphics.Color YOK,
    // JVM birim testleri stub tuzağına düşmeden çalışır)
    // ═══════════════════════════════════════════════════════════════

    private const val SIYAH = 0xFF000000.toInt()
    private const val BEYAZ = 0xFFFFFFFF.toInt()

    private fun kR(c: Int) = (c shr 16) and 0xFF
    private fun kG(c: Int) = (c shr 8) and 0xFF
    private fun kB(c: Int) = c and 0xFF
    private fun kRGB(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** İki rengi [t] oranında karıştırır (0 = a, 1 = b). Saf. */
    fun karistirUc(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        return kRGB(
            ((kR(a) * (1 - k)) + (kR(b) * k)).toInt(),
            ((kG(a) * (1 - k)) + (kG(b) * k)).toInt(),
            ((kB(a) * (1 - k)) + (kB(b) * k)).toInt()
        )
    }

    /** Algılanan parlaklık (0..1). Saf. */
    fun parlaklikUc(c: Int): Float =
        0.299f * kR(c) / 255f + 0.587f * kG(c) / 255f + 0.114f * kB(c) / 255f

    /**
     * Doygunluğu [oran] kadar artırır: kanalların griden uzaklığı
     * ölçeklenir; gri nötr kalır, 255 kelepçesi taşmayı engeller. Saf.
     */
    fun canlandir(c: Int, oran: Float): Int {
        val gri = (kR(c) + kG(c) + kB(c)) / 3
        val k = oran.coerceIn(0.5f, 2f)
        return kRGB(
            (gri + (kR(c) - gri) * k).toInt(),
            (gri + (kG(c) - gri) * k).toInt(),
            (gri + (kB(c) - gri) * k).toInt()
        )
    }

    /** Vurgu canlılığı: 0 = sadeleştir (zemine çek) · 1 = olduğu gibi · 2 = canlandır. Saf. */
    fun vurguAyarla(vurgu: Int, zemin: Int, kademe: Int): Int = when (kademe.coerceIn(0, 2)) {
        0 -> karistirUc(vurgu, zemin, 0.35f)
        2 -> canlandir(vurgu, 1.35f)
        else -> vurgu
    }

    /** Tamamlanan rengi: 0 = yeşil (paradan) · 1 = mavi · 2 = gri · 3 = vurgu. Saf. */
    fun tamamRengi(yesil: Int, mod: Int, vurgu: Int): Int = when (mod.coerceIn(0, 3)) {
        1 -> 0xFF64A0DC.toInt()
        2 -> 0xFF9E9E9E.toInt()
        3 -> vurgu
        else -> yesil
    }

    /** Yüksek kontrast: soluk metinler ana metne yaklaşır; ana metin aşırı uca. Saf. */
    fun kontrastUygula(p: WidgetTema.Palet): WidgetTema.Palet {
        val yeniMetin = karistirUc(p.metin, if (p.koyuMu) BEYAZ else SIYAH, 0.35f)
        return p.copy(
            metin = yeniMetin,
            metinSoluk = karistirUc(p.metinSoluk, yeniMetin, 0.50f)
        )
    }

    /** Gece karartması: zeminler karar, metin dokunulmaz (okunabilirlik), ışıltı kısılır. Saf. */
    fun karartUygula(p: WidgetTema.Palet, faktor: Float): WidgetTema.Palet {
        val f = faktor.coerceIn(0f, 1f)
        val yeniZemin = karistirUc(p.zemin, SIYAH, f)
        return p.copy(
            zemin = yeniZemin,
            zeminAlt = karistirUc(p.zeminAlt, SIYAH, f),
            vurgu = karistirUc(p.vurgu, SIYAH, f * 0.35f),
            yesil = karistirUc(p.yesil, SIYAH, f * 0.35f)
        )
    }

    /**
     * Metin rengi modu:
     *  0 = otomatik (zemine göre palet karar verir — eski davranış)
     *  1 = açık tonlara çek · 2 = koyu tonlara çek · 3 = vurguyla uyumlu.
     * Kullanıcı zeminle çelişen mod seçerse bile karışım oranları düşük
     * tutulur; tam zıt renge zorlama yapılmaz. Saf.
     */
    fun metinModuUygula(p: WidgetTema.Palet, mod: Int): WidgetTema.Palet = when (mod.coerceIn(0, 3)) {
        1 -> p.copy(
            metin = karistirUc(p.metin, BEYAZ, 0.55f),
            metinSoluk = karistirUc(p.metinSoluk, BEYAZ, 0.30f)
        )
        2 -> p.copy(
            metin = karistirUc(p.metin, SIYAH, 0.60f),
            metinSoluk = karistirUc(p.metinSoluk, SIYAH, 0.35f)
        )
        3 -> p.copy(
            metin = karistirUc(p.metin, p.vurgu, 0.30f),
            metinSoluk = karistirUc(p.metinSoluk, p.vurgu, 0.18f)
        )
        else -> p
    }

    // ═══════════════════════════════════════════════════════════════
    // MERKEZİ BORU HATTI — WidgetTema.palet dönüşünden hemen önce
    // ═══════════════════════════════════════════════════════════════

    /**
     * Palete kullanıcının v10.17 tercihlerini uygular.
     * Sıra: gece karartması → yüksek kontrast → metin modu →
     * vurgu canlılığı + tamamlanan rengi. Her basamak arıza güvenli;
     * bütünü patlarsa palet olduğu gibi döner (eski davranış).
     */
    fun uygula(context: Context, p: WidgetTema.Palet): WidgetTema.Palet {
        return try {
            var c = p
            // 1) gece karartması (yalnız pencere içinde) — v10.20: şiddet serbest %
            if (karartAcik(context)) {
                val cal = Calendar.getInstance()
                val simdiDk = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                if (karartmaAktifMi(simdiDk, basSaat(context), bitSaat(context))) {
                    c = karartUygula(c, karartFaktorPct(siddetPct(context)))
                }
            }
            // 2) yüksek kontrast — v10.20: serbest yüzde (0 = kapalı)
            val kp = kontrastPct(context)
            if (kp > 0) c = kontrastUygula(c, kp)
            // 3) metin rengi modu
            c = metinModuUygula(c, metinMod(context))
            // 4) vurgu canlılığı (v10.20: serbest %) + tamamlanan rengi
            c = c.copy(
                vurgu = canlandirPct(c.vurgu, c.zemin, canliPct(context)),
                yesil = tamamRengi(c.yesil, tamamMod(context), c.vurgu)
            )
            // 5) v10.20: özel renk şablonu EN SON — kullanıcının renkleri
            //    her dönüşümün üstünde kalır (tam yetki)
            ozRenkleriUygula(
                c,
                metin = ozelRenk(context, K_OZ_METIN),
                zemin = ozelRenk(context, K_OZ_ZEMIN),
                vurgu = ozelRenk(context, K_OZ_VURGU),
                yesil = ozelRenk(context, K_OZ_YESIL)
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Palet zenginleştirilemedi", e)
            p
        }
    }
}
