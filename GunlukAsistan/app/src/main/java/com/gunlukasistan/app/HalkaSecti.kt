package com.gunlukasistan.app

import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * v10.7 · Öneri A3 — Halka kadrandan süre seçiminin saf matematiği.
 *
 * ── Ne eksikti ──
 * Süre seçmek için ya hazır çipe (5/15/25) ya da NumberPicker
 * diyaloğuna giriliyordu. Kadranın kendisi hiç dokunma işlemi
 * yapmıyordu (`SayacKadraniView` içinde onTouch kodu yoktu).
 * Google Saat kadranı sürüklenerek ayarlanır; bu hissi getiriyoruz.
 *
 * ── Açı sistemi ──
 * Kadran 60 çizgili; tam tur = 60 dakika, her çizgi = 1 dakika (6°).
 * Açılar **halka kadranı düzeninde** tutulur: tepe (12 yönü) = 0°,
 * saat yönünde artar (sağ = 90°, alt = 180°, sol = 270°).
 * Ekran koordinatında y aşağı büyür, bu yüzden `atan2(dy, dx)`
 * zaten saat yönünde ilerler; yalnızca başlangıca kaydırmak yeter.
 *
 * ── Neden ayrı object ──
 * Dokunma geometrisi View katmanından sıyrılınca birim testiyle
 * her açının doğru dakikaya döndüğü kanıtlanır; View sadece
 * olayları buraya devreder.
 */
object HalkaSecti {

    /** Tam turdaki dakika sayısı — kadrandaki çizgi sayısıyla aynı. */
    const val TUR_DAKIKA = 60

    /** Seçilebilecek en küçük süre. */
    const val MIN_DAKIKA = 1

    /** Bir dakikanın derece karşılığı. */
    const val DERECE_BASINA_DK = 360f / TUR_DAKIKA

    /**
     * Dokunma noktasının halka açısı (derece, 0 ≤ açı < 360).
     *
     * Tepe = 0°, saat yönünde artar. Parmak tam merkezdeyse (mesafe
     * sıfır) açı tanımsızdır; çağıran [halkadaMi] ile bunu eler,
     * burası yine de 0 döner (güvenli varsayılan).
     */
    fun aci(x: Float, y: Float, merkezX: Float, merkezY: Float): Float {
        val dx = x - merkezX
        val dy = y - merkezY
        // atan2(dy, dx): sağ=0, alt=90(ekranda y aşağı) — saat yönü.
        // Tepe = -90; +90 kaydırıp +360 ile 0..360 aralığına sabitliyoruz.
        val radyan = atan2(dy.toDouble(), dx.toDouble())
        val derece = Math.toDegrees(radyan).toFloat()
        return (derece + 90f + 360f) % 360f
    }

    /**
     * Halka açısını dakikaya çevirir (1..60).
     *
     * En yakın tam dakikaya yuvarlanır: parmak 3°'deyse 1 dk,
     * 359°'deyse 60 dk. 0° (tepe) ham olarak 0 dk verir ama
     * alt sınıra çekilip 1 dk olur — "0 dakikalık sayaç" diye
     * bir şey kurulamasın.
     */
    fun acidanDakika(aciDerece: Float): Int {
        val ham = (aciDerece / DERECE_BASINA_DK).roundToInt()
        return ham.coerceIn(MIN_DAKIKA, TUR_DAKIKA)
    }

    /** Dakikanın kadrandaki açısı — testlerde ters eşleme için. */
    fun dakikadanAci(dakika: Int): Float =
        dakika.coerceIn(MIN_DAKIKA, TUR_DAKIKA) * DERECE_BASINA_DK

    /**
     * Dokunma, halka bandının içinde mi?
     *
     * Kadranın ortası tıklanınca başlat/duraklat çalışmalı — süre
     * seçimi yalnızca **dış halkada** aktif. Bant, çizgi kadranın
     * biraz içine ve dışına taşır (parmak ekranda çizgiyi tam
     * tutturamaz; tolerans yoksa jest asla başlamaz).
     *
     * @param icSinir kadran çizgilerinin bittiği iç yarıçap
     * @param disSinir kadran çizgilerinin dış ucu
     */
    fun halkadaMi(
        x: Float,
        y: Float,
        merkezX: Float,
        merkezY: Float,
        icSinir: Float,
        disSinir: Float
    ): Boolean {
        if (disSinir <= 0f || icSinir <= 0f) return false
        val dx = x - merkezX
        val dy = y - merkezY
        val mesafe = sqrt(dx * dx + dy * dy)
        // İçe %28, dışa %14 tolerans — band yaşanır kalınlıkta olsun.
        return mesafe >= icSinir * 0.72f && mesafe <= disSinir * 1.14f
    }

    /**
     * Sürükleme mesafesi eşiği geçti mi?
     *
     * Dokunma ile sürüklemeyi ayırır: mesafe küçükse bu bir
     * **tıklama**dır (başlat/duraklat), büyükse jest (süre seçimi).
     * Eşik View tarafında `scaledTouchSlop`'tan gelir.
     */
    fun suruklemeMi(basX: Float, basY: Float, x: Float, y: Float, esik: Float): Boolean {
        val dx = x - basX
        val dy = y - basY
        return dx * dx + dy * dy >= esik * esik
    }
}
