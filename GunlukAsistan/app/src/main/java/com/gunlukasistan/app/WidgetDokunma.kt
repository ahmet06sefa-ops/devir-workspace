package com.gunlukasistan.app

import android.content.Context

/**
 * v10.21 · KULLANICI İSTEĞİ — Widget-bazlı dokunma hedefi.
 *
 * ── Kapsam ──
 * Bir widget'ın GÖVDE dokunması hangi uygulama sekmesini açsın? Her
 * widget için ayrı seçim: -1 (varsayılan = widget'ın kendi ekranı,
 * eski davranış) ya da geçerli bir sekme indeksi. Sağlayıcılar
 * `WidgetCommon.openScreen` çağrısına bu okuyucuyu katar.
 *
 * Alt düğmeler (başlat/duraklat, görev ekle, soru +1...) BİLEREK bu
 * kapsamın dışında — onlar eylemdir, sekme değiştirmek davranışı bozar.
 *
 * Seçilebilir sekmeler MainActivity.createFragment haritasından gelir;
 * Ayarlar/Tema/Araçlar bilinçli dışarıda bırakılmıştır (widget'tan ayar
 * sekmesine gitmek faydasız kabul edildi; istenirse liste genişletilir).
 */
object WidgetDokunma {

    private const val PREF = "wg_dokunma_v1"

    // ── Widget kodları (pref anahtarı öneki) ──
    const val CD = "dk_cd"        // Geri sayım (2×1)
    const val SY = "dk_sy"        // Sayaç (2×2)
    const val HD = "dk_hd"        // Hedef halkası
    const val SUM = "dk_sum"      // Özet
    const val TASKS = "dk_tasks"  // Görev listesi
    const val EV = "dk_ev"        // Çoklu geri sayım listesi
    const val ODAK = "dk_odak"    // Odak kutusu (koşarken gövde)

    /** Seçilebilir sekme indeksleri — MainActivity.createFragment ile birebir. */
    val EKRANLAR = listOf(0, 1, 2, 3, 4, 5, 6, 10, 11, 12, 13, 16)

    /** Sekme ad kaynakları — [EKRANLAR] ile aynı sırada (UI bu haritayı uçar). */
    val EKRAN_ADLARI = mapOf(
        0 to R.string.w21_e_ana,
        1 to R.string.w21_e_ilerleme,
        2 to R.string.w21_e_bugun,
        3 to R.string.w21_e_konular,
        4 to R.string.w21_e_zaman,
        5 to R.string.w21_e_notlar,
        6 to R.string.w21_e_gorevler,
        10 to R.string.w21_e_sinav,
        11 to R.string.w21_e_etkinlik,
        12 to R.string.w21_e_aliskanlik,
        13 to R.string.w21_e_kurs,
        16 to R.string.w21_e_plan
    )

    /** Kod geçerli bir sekme mi? Saf — birim testli. */
    fun gecerliMi(kod: Int): Boolean = kod in EKRANLAR

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /**
     * Widget'ın gövde dokunması için hedef sekme.
     * Tercihte geçersiz bir şey duruyorsa [varsayilan] döner (eski davranış).
     */
    fun ekran(c: Context, widgetKodu: String, varsayilan: Int): Int {
        val v = prefs(c).getInt(widgetKodu, -1)
        return if (gecerliMi(v)) v else varsayilan
    }

    /** -1 verilirse tercih silinir → widget kendi varsayılanına döner. */
    fun setEkran(c: Context, widgetKodu: String, ekran: Int) {
        val e = prefs(c).edit()
        if (ekran == -1) e.remove(widgetKodu) else e.putInt(widgetKodu, ekran)
        e.apply()
    }

    /** Kayıtlı seçim (UI'da göstermek için; -1 = varsayılan). */
    fun secili(c: Context, widgetKodu: String): Int = prefs(c).getInt(widgetKodu, -1)
}
