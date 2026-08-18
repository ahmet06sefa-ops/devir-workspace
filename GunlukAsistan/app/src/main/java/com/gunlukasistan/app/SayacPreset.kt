package com.gunlukasistan.app

import android.content.Context

/**
 * v10.5 · Öneri C27 — SayacWidget v2 hazır ayar çipleri.
 *
 * ── Sorun ──
 * Sayaç widget'ında (v7.85) "Başlat" vardı ama önce uygulamaya
 * girip süre seçmek gerekiyordu: ana ekrandan **odak başlatmak 3
 * dokunuş** sürüyordu. Çipler bunu tek dokunuşa indirir.
 *
 * ── Neden 5·15·25 ──
 * Uygulama içi hazır ayarlarla (Kısa/Orta/Odak) aynı üçlü; iki
 * yüzey farklı önermemeli. Sayaç zaten çalışıyorsa çipler görünmez
 * (`SayacWidget.doldur`) — koşan oturumu bölmez.
 *
 * ── Saf bölge ──
 * Liste ve erişim güvenliği birim testli.
 */
object SayacPreset {

    /** Widget çiplerindeki dakikalar — uygulama içi çiplerle aynı. */
    val PRESETLER = listOf(5, 15, 25)

    fun getPresetler(context: Context? = null): List<Int> {
        if (context != null) {
            return try { SayacAyar.presetlerGetir(context) } catch (_: Exception) { PRESETLER }
        }
        return PRESETLER
    }

    /** Çip indeksi dışına taşarsa null — render tarafı çipi gizler. */
    fun dakika(indeks: Int, context: Context? = null): Int? = getPresetler(context).getOrNull(indeks)

    /** Çip etiketi: "5 dk" biçiminde, birim bağımsız. */
    fun etiket(dakika: Int): String = "$dakika dk"
}
