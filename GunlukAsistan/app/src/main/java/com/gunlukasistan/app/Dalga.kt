package com.gunlukasistan.app

import com.google.android.material.card.MaterialCardView

/**
 * v10.1 · Görsel Grup C / Öneri 14 — Dokunma dalgası (ripple) tutarlılığı.
 *
 * ── Sorun ──
 * Tıklanabilir kartların bir bölümünde dokunma dalgası var, bir
 * bölümünde yok. En görünür sebep: layout'ta `clickable="true"` verilen
 * ama `?attr/selectableItemBackground` atanmayan kartlar ile koddan
 * `MaterialCardView(this)` yapılıp doğrudan `setOnClickListener`
 * bağlanan kartlar.
 *
 * ── Bu uzantı ne yapar ──
 * Koddan kurulan karta temanın standart seçilebilir öğe dalgasını
 * `foreground` olarak bağlar. `foreground` zaten atanmışsa (nadiren
 * özel bir görünüm kullanılmış olabilir) DOKUNMAZ — yalnız eksiği
 * tamamlar, var olan tasarım kararını ezmez.
 *
 * `RippleTutarlilikTest` XML tarafını korur; bu uzantı Kotlin tarafını.
 */
fun MaterialCardView.dalgaEkle() {
    if (foreground != null) return
    runCatching {
        val dizi = context.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.selectableItemBackground)
        )
        try {
            foreground = dizi.getDrawable(0)
        } finally {
            dizi.recycle()
        }
    }
}
