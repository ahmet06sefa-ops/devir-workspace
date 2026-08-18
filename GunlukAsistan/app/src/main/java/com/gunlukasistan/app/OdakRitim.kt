package com.gunlukasistan.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.12 · ULTRA-30 / D23 — Günlük seans hedefi halkası.
 *
 * ── Fikir ──
 * Kadranın iç çemberinde hedef kadar küçük nokta durur; o gün biten her
 * odak seansı bir noktayı vurgu rengiyle doldurur. Hedefe varınca halka
 * parıldar (kadranın mevcut bitiş parlaması) ve tek seferlik kutlama
 * gösterilir.
 *
 * ── Seans sayısının kaynağı (dürüstlük notu) ──
 * Ayrı bir sayaç TUTMUYORUZ: [OdakKaydi.bugunOturumSayisi] gerçek oturum
 * günlüğünden sayar. Böylece bildirimden ya da tam ekrandan biten seans
 * da sayılır. Tek şart: odak kaydı modu KAPALI ise günlükte kayıt
 * olmadığı için ilerleme görünmez — ayarlar açıklamasında belirtilir.
 *
 * ── Tek kutuvara yazma ──
 * Kutlama günde bir kez: bayrak `kutlama_gun` anahtarında saklanır ve
 * gün değişince kendiliğinden geçersizleşir (yarın yeni hedefe yeni
 * kutlama).
 */
object OdakRitim {

    private const val PREF = "fo_odak_ritim_v1"
    private const val K_HEDEF = "hedef"
    private const val K_KUTLAMA_GUN = "kutlama_gun"

    /** Halkaya sığan en çok işaret. */
    const val MAKS_HEDEF = 12

    /** Varsayılan: günde 4 seans (klasik pomodoro günlüğü). */
    private const val VARSAYILAN_HEDEF = 4

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** 0 = kapalı. */
    fun hedef(c: Context): Int =
        prefs(c).getInt(K_HEDEF, VARSAYILAN_HEDEF).coerceIn(0, MAKS_HEDEF)

    fun setHedef(c: Context, v: Int) {
        prefs(c).edit().putInt(K_HEDEF, v.coerceIn(0, MAKS_HEDEF)).apply()
    }

    /** Bugün biten gerçek odak seansı. */
    fun bugunSeans(c: Context): Int = OdakKaydi.bugunOturumSayisi(c)

    // ---------------- Saf hesap (birim testli) ----------------

    /** Dolu işaret sayısı — hedefi aşan seanslar taşmaz. */
    fun doluIsaret(bugun: Int, hedef: Int): Int =
        if (hedef <= 0) 0 else bugun.coerceIn(0, hedef)

    /** Kutlama yalnız hedefe İLK varışta ve günde bir kez. */
    fun kutlamaGerekliMi(bugun: Int, hedef: Int, zatenKutlandi: Boolean): Boolean =
        hedef > 0 && bugun >= hedef && !zatenKutlandi

    // ---------------- Kutlama bayrağı ----------------

    private fun bugunAnahtari(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun kutlandiMi(c: Context): Boolean =
        prefs(c).getString(K_KUTLAMA_GUN, "") == bugunAnahtari()

    fun kutlamayiIsaretle(c: Context) {
        prefs(c).edit().putString(K_KUTLAMA_GUN, bugunAnahtari()).apply()
    }
}
