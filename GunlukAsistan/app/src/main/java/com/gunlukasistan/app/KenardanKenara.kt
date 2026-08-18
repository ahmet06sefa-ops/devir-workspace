package com.gunlukasistan.app

import android.app.Activity
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * v10.1 · Görsel Grup C / Öneri 15 — Kenardan kenara (edge-to-edge) temel katmanı.
 *
 * ── Neden ──
 * Android 15 (targetSdk 35) kenardan kenarayı **zorunlu** kılacak —
 * sistem çubuklarının arkası opak renkle dolmayacak, içerik altlarına
 * uzayacak. targetSdk 34'te hazırlıklı olmak değişikliğin kontrollü
 * yapılmasını sağlar.
 *
 * ── Bu sürüm kapsamı ──
 * Yalnızca pencere bayrakları kurulur:
 *   1. İçerik sistem çubuklarının arkasına uzanabilir
 *      (`setDecorFitsSystemWindows(false)`)
 *   2. Durum ve gezinme çubuğu saydam yapılır
 *   3. Simge kontrastı temaya göre ayarlanır (açık temada koyu simge)
 *
 * Yerleşim geometrisi DEĞİŞMEZ: `activity_main.xml` kökünde
 * `fitsSystemWindows="true"` olduğundan çubuk alanları eskisi gibi
 * dolguyla korunur; ekranlar aynı yerden başlar. Fark yalnızca
 * çubukların rengindedir — artık temadan bağımsız saydam, arkasında
 * uygulamanın kendi arka planı görünür.
 *
 * ── Bilinçli dışarıda bırakılanlar ──
 *   · Bağımsız Activity'ler (QuizActivity vb.) ve içeriklerin çubuk
 *     altında AKTARAK kayması (tam e2e estetiği) — bu, ekran ekran
 *     inset incelemesi ister ve cihazda görülmeden yapılması riskli.
 *     Bu sürüm temeldir; ileri adımlar ayrı sürümde.
 */
object KenardanKenara {

    fun uygula(activity: Activity) {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.navigationBarColor = Color.TRANSPARENT

            val koyu = ThemeManager.koyuMu(activity)
            val kumanda = WindowInsetsControllerCompat(
                activity.window,
                activity.window.decorView
            )
            kumanda.isAppearanceLightStatusBars = !koyu
            kumanda.isAppearanceLightNavigationBars = !koyu
        }.onFailure {
            android.util.Log.w("KenardanKenara", "Edge-to-edge kurulamadı", it)
        }
    }
}
