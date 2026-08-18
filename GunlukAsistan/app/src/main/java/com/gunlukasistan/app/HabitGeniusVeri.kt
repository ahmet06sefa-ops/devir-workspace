package com.gunlukasistan.app

import android.content.Context

/**
 * v11.25 — HabitGenius kalıcılık katmanı.
 *
 * Kullanıcının sağladığı HabitGenius özelliklerinin verilerini CİHAZDA kalıcı
 * yapar. Room projede mevcut olmakla birlikte, bu modül bağımsız ve saf olması
 * için SharedPreferences tabanlı basit bir anahtar-değer deposu kullanır;
 * JVM testlerine uygundur.
 *
 * Kaydedilen veriler:
 *  · su sayacı (waterCount)
 *  · habit onay durumları (habit_*) — {ad}:1/0
 *  · seçili ilerleme türü ve sıklık
 *  · günlük metni (journal)
 *  · seçili vurgu rengi indeksi
 */
object HabitGeniusVeri {

    private const val PREF = "habitgenius_veri"
    private const val K_SU = "su_count"
    private const val K_JOURNAL = "journal"
    private const val K_TIP = "ilerleme_tipi"
    private const val K_SIKLIK = "siklik"
    private const val K_VURGU = "vurgu"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ── Su sayacı ──
    fun suSayaci(c: Context): Int = p(c).getInt(K_SU, 3)
    fun suSayaci(c: Context, deger: Int) = p(c).edit().putInt(K_SU, deger.coerceIn(0, 99)).apply()

    // ── Günlük ──
    fun gunluk(c: Context): String = p(c).getString(K_JOURNAL, "") ?: ""
    fun gunluk(c: Context, metin: String) = p(c).edit().putString(K_JOURNAL, metin).apply()

    // ── İlerleme tipi / sıklık ──
    fun ilerlemeTipi(c: Context): Int = p(c).getInt(K_TIP, 0)
    fun ilerlemeTipi(c: Context, i: Int) = p(c).edit().putInt(K_TIP, i.coerceAtLeast(0)).apply()
    fun siklik(c: Context): Int = p(c).getInt(K_SIKLIK, 0)
    fun siklik(c: Context, i: Int) = p(c).edit().putInt(K_SIKLIK, i.coerceAtLeast(0)).apply()

    // ── Vurgu rengi ──
    fun vurgu(c: Context): Int = p(c).getInt(K_VURGU, 0)
    fun vurgu(c: Context, i: Int) = p(c).edit().putInt(K_VURGU, i.coerceAtLeast(0)).apply()

    // ── Habit onay durumları ──
    fun habitDurum(c: Context, ad: String): Boolean = p(c).getBoolean("habit_$ad", false)
    fun habitDurum(c: Context, ad: String, durum: Boolean) =
        p(c).edit().putBoolean("habit_$ad", durum).apply()

    // ── Saf test yardımcıları (Context'siz, değer aralığı doğrulama) ──
    fun suSayaciSinirla(deger: Int): Int = deger.coerceIn(0, 99)
    fun tipSinirla(i: Int): Int = i.coerceIn(0, 4) // 0..4 (5 ilerleme türü)
    fun siklikSinirla(i: Int): Int = i.coerceIn(0, 5) // 0..5 (6 sıklık)
    fun vurguSinirla(i: Int): Int = i.coerceIn(0, 19) // 0..19 (20 renk)
}
