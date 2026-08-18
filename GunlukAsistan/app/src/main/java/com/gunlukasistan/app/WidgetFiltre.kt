package com.gunlukasistan.app

import android.content.Context

/**
 * v10.13 · ULTRA-30 / B12 — Örnek başına widget içerik filtresi.
 *
 * ── Kullanıcının isteği ──
 * "2. widget örneği yalnız iş etiketini göstersin."
 *
 * ── Çözüm ──
 * Her görev-listesi widget örneği kendi `widgetId`'si altında bir etiket
 * kodu saklar. Boş kod = filtre yok (eski davranış). Filtre, satırları
 * üreten [TasksWidgetService] fabrikasında uygulanır; başlık satırındaki
 * 🏷 düğmesi [WidgetFiltreActivity]'yi açar.
 *
 * [gecerMi] framework'süzdür ve birim testlidir.
 */
object WidgetFiltre {

    private const val PREF = "wg_filtre_v1"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Örneğin seçili etiket kodu; boş = tümü. */
    fun filtre(c: Context, widgetId: Int): String =
        prefs(c).getString("f_$widgetId", Etiket.YOK) ?: Etiket.YOK

    fun setFiltre(c: Context, widgetId: Int, kod: String) {
        prefs(c).edit().putString("f_$widgetId", kod).apply()
    }

    /**
     * Görev filtreden geçer mi?
     * Filtre boşsa her şey geçer; doluysa yalnız birebir eşleşme.
     */
    fun gecerMi(etiket: String, filtre: String): Boolean =
        filtre.isBlank() || etiket == filtre

    /** Başlık çipinde / ayar ekranında gösterilen ad. */
    fun filtreAd(c: Context, widgetId: Int): String {
        val f = filtre(c, widgetId)
        return if (f.isBlank()) c.getString(R.string.wg_filtre_tumu)
        else Etiket.ad(c, f)
    }
}
