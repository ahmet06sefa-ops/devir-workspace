package com.gunlukasistan.app

import android.widget.TextView

/**
 * v10.1 · Görsel Grup C / Öneri 12 — Sayı değişimlerinde "rulo" efekti.
 *
 * ── Ne ──
 * Bir TextView'un metni değiştiğinde eski değer yukarı kayıp solarak
 * çıkar, yeni değer alttan gelir. FlipClockView'un (v5.7) çevirme
 * fikrinin hafif, genel kullanımlı sürümü — özel View çizimi yok,
 * herhangi bir TextView'de çalışır.
 *
 * ── Fark: Canlandir.sayi ──
 * [Canlandir.sayi] 0'dan hedefe SAYARAK gider — ekran ilk açıldığında
 * doğru araç. [Rulo] iki ANLIK değer arası geçiş içindir
 * (29:58 → 29:57): sayı zaten görünüyor, yalnızca değişiyor.
 *
 * ── Güvenlikler ──
 *   · Metin değişmediyse hiçbir şey yapmaz (tick başına çağrılsan bile
 *     saniyede bir oynar)
 *   · [GorunumAyar.animasyonAcik] kapalıysa düz yazar
 *   · Hızlı çağrılarda süren animasyon iptal edilir, değer asla
 *     yarıda kalmaz — her yolculukta son durum yeni metindir
 */
object Rulo {

    private const val SURE_YARIM_MS = 130L

    /** Yeni metni rulo efektiyle yazar; metin aynıysa dokunmaz. */
    fun yaz(gorunum: TextView?, yeni: String) {
        gorunum ?: return
        val eski = gorunum.text?.toString().orEmpty()
        if (eski == yeni) return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) {
            gorunum.text = yeni
            return
        }
        runCatching {
            // Süren bir geçiş varsa iptal et — hızlı tazelemede üst üste
            // binen animasyonlar titreme yapar. Görünümü doğrudan yeni
            // metne kurup tek geçiş oynat.
            gorunum.animate().cancel()
            gorunum.translationY = 0f
            gorunum.alpha = 1f

            val kadar = if (gorunum.height > 0) {
                gorunum.height * 0.4f
            } else {
                20f * gorunum.resources.displayMetrics.density
            }

            gorunum.animate()
                .translationY(-kadar)
                .alpha(0f)
                .setDuration(SURE_YARIM_MS)
                .withEndAction {
                    runCatching {
                        gorunum.text = yeni
                        gorunum.translationY = kadar
                        gorunum.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(SURE_YARIM_MS)
                            .withEndAction(null)
                            .start()
                    }.onFailure {
                        gorunum.text = yeni
                        gorunum.translationY = 0f
                        gorunum.alpha = 1f
                    }
                }
                .start()
        }.onFailure {
            gorunum.text = yeni
            gorunum.translationY = 0f
            gorunum.alpha = 1f
            android.util.Log.w("Rulo", "Rulo oynatılamadı, düz yazıldı", it)
        }
    }

    /** Sayısal değer için kısayol — biçim verilirse birim eklenir. */
    fun sayi(gorunum: TextView?, yeni: Int, bicim: (Int) -> String = { it.toString() }) =
        yaz(gorunum, bicim(yeni))
}
