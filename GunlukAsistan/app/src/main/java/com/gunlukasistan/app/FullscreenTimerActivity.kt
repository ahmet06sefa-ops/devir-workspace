package com.gunlukasistan.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

/**
 * Yatay tam ekran zamanlayıcı (v5.7).
 *
 * Fliqlo tarzı büyük flip saat + seçilen ortam sesine göre
 * arka planda atmosfer efekti (şömine ateşi, yağmur, orman…).
 */
class FullscreenTimerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"          // 0 = geri sayım, 1 = kronometre
        const val EXTRA_TOTAL_MS = "total_ms"  // geri sayım süresi
        const val EXTRA_SOUND = "sound"        // seçili ses indeksi (-1 = yok)

        private const val MODE_DOWN = 0
        private const val MODE_WATCH = 1
    }

    private lateinit var clock: FlipClockView
    private lateinit var fx: AmbientFxView
    private lateinit var fxFront: AmbientFxView
    private lateinit var status: TextView
    private lateinit var playButton: TextView
    private lateinit var controls: LinearLayout

    private var mode = MODE_DOWN
    private var totalMs = 25 * 60_000L
    private var remaining = 25 * 60_000L
    private var elapsed = 0L
    private var running = false
    private var endStamp = 0L
    private var startStamp = 0L
    /** Bu oturumda odaklanma olarak yazılacak dakika. */
    private var creditedMinutes = 0

    // v10.12 · D22: çalma durumu SesManzarasi motorunda; bu alan yalnız
    // görsel ayna (çip boyama, alev efekti). Gerçek çalar motorda.
    private var selectedSound = -1
    private val soundChips = mutableListOf<TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            tick()
            handler.postDelayed(this, 250L)
        }
    }
    private val hideControls = Runnable { setControlsVisible(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Yatay + tam ekran + ekran açık kalsın
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_fullscreen_timer)
        goImmersive()

        clock = findViewById(R.id.fsClock)
        fx = findViewById(R.id.fsFx)
        fxFront = findViewById(R.id.fsFxFront)
        fxFront.setForeground(true)
        status = findViewById(R.id.fsStatus)
        playButton = findViewById(R.id.fsPlay)
        controls = findViewById(R.id.fsControls)

        mode = intent.getIntExtra(EXTRA_MODE, MODE_DOWN)
        totalMs = intent.getLongExtra(EXTRA_TOTAL_MS, 25 * 60_000L)
        remaining = totalMs
        // v10.12 · D22: ses tek motorda — motordaki akışla aynıysa dokunma
        selectedSound = SesManzarasi.calanIndeks
        val istenen = intent.getIntExtra(EXTRA_SOUND, -1)

        buildSoundChips()
        if (istenen >= 0 && istenen != selectedSound) {
            SesManzarasi.manuelCal(this, istenen)
            selectedSound = istenen
        }

        playButton.setOnClickListener { if (running) pause() else start() }
        findViewById<View>(R.id.fsReset).setOnClickListener { resetAll() }
        findViewById<View>(R.id.fsClose).setOnClickListener { finish() }

        // Ekrana dokunma → kontrolleri göster/gizle
        findViewById<View>(R.id.fsRoot).setOnClickListener { toggleControls() }

        updateDisplay()
        updateStatus()
        scheduleHide()
    }

    // ---------------- Görünüm ----------------

    private fun goImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    private fun toggleControls() = setControlsVisible(controls.alpha < 0.5f)

    private fun setControlsVisible(visible: Boolean) {
        controls.animate().alpha(if (visible) 1f else 0f).setDuration(220).start()
        status.animate().alpha(if (visible) 0.55f else 0.18f).setDuration(220).start()
        handler.removeCallbacks(hideControls)
        if (visible) scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideControls)
        handler.postDelayed(hideControls, 6000L)
    }

    // ---------------- Ses seçimi ----------------

    private val soundLabels = listOf(
        "🌧️" to R.string.sound_rain,
        "🌊" to R.string.sound_wave,
        "🌲" to R.string.sound_forest,
        "🔥" to R.string.sound_fire,
        "💨" to R.string.sound_wind,
        "☕" to R.string.sound_cafe,
        "🦗" to R.string.sound_cricket,
        "📻" to R.string.sound_white
    )


    /** Ses indeksini efekt türüne çevirir (liste sırası aynı). */
    private fun effectFor(soundIndex: Int): Int = when (soundIndex) {
        0 -> AmbientFxView.FX_RAIN
        1 -> AmbientFxView.FX_WAVE
        2 -> AmbientFxView.FX_FOREST
        3 -> AmbientFxView.FX_FIRE
        4 -> AmbientFxView.FX_WIND
        5 -> AmbientFxView.FX_CAFE
        6 -> AmbientFxView.FX_CRICKET
        7 -> AmbientFxView.FX_WHITE
        else -> AmbientFxView.FX_NONE
    }

    private fun buildSoundChips() {
        val row = findViewById<LinearLayout>(R.id.fsSoundRow)
        val density = resources.displayMetrics.density
        soundChips.clear()
        soundLabels.forEachIndexed { index, (emoji, labelRes) ->
            val chip = TextView(this).apply {
                text = "$emoji ${getString(labelRes)}"
                textSize = 12f
                setTextColor(0xFFB8B8B8.toInt())
                setPadding(
                    (14 * density).toInt(), (9 * density).toInt(),
                    (14 * density).toInt(), (9 * density).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * density).toInt() }
                setOnClickListener {
                    if (selectedSound == index) stopSound() else playSound(index)
                    scheduleHide()
                }
            }
            soundChips.add(chip)
            row.addView(chip)
        }
        refreshChips()
    }

    private fun refreshChips() {
        soundChips.forEachIndexed { index, chip ->
            chip.setBackgroundResource(
                if (index == selectedSound) R.drawable.fs_chip_on else R.drawable.fs_chip
            )
            chip.setTextColor(
                if (index == selectedSound) 0xFFE8C89A.toInt() else 0xFFB8B8B8.toInt()
            )
        }
    }

    private fun playSound(index: Int) {
        // v10.12 · D22: çalma merkezi motorda; görünüm motor durumunu yansıtır
        SesManzarasi.manuelCal(this, index)
        secimYansit(SesManzarasi.calanIndeks)
    }

    private fun stopSound() {
        SesManzarasi.manuelDur(this)
        secimYansit(-1)
    }

    /** Motor durumunu görsel efektlere, yanan rakamlara ve çiplere yansıtır. */
    private fun secimYansit(index: Int) {
        selectedSound = index
        val fxTur = effectFor(index)
        fx.setEffect(fxTur)
        fxFront.setEffect(fxTur)
        // Şömine seçilince rakamlar da yanar
        clock.setBurning(fxTur == AmbientFxView.FX_FIRE)
        refreshChips()
    }

    // ---------------- Zamanlayıcı ----------------

    private fun currentRemaining(): Long =
        if (running) (endStamp - System.currentTimeMillis()).coerceAtLeast(0L) else remaining

    private fun currentElapsed(): Long =
        if (running) elapsed + (System.currentTimeMillis() - startStamp) else elapsed

    private fun tick() {
        if (mode == MODE_DOWN && currentRemaining() <= 0L) {
            finishCountdown()
            return
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        val ms = if (mode == MODE_DOWN) currentRemaining() else currentElapsed()
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = totalSeconds % 3600 / 60
        val s = totalSeconds % 60

        // Saat varsa SS:DD, yoksa DD:SS göster
        if (h > 0) {
            clock.setTime(
                String.format(Locale.US, "%02d", h),
                String.format(Locale.US, "%02d", m),
                getString(R.string.fs_hour_short)
            )
        } else {
            clock.setTime(
                String.format(Locale.US, "%02d", m),
                String.format(Locale.US, "%02d", s),
                getString(R.string.fs_min_short)
            )
        }
    }

    private fun updateStatus() {
        val modeText = if (mode == MODE_DOWN) {
            getString(R.string.fs_mode_countdown, totalMs / 60_000)
        } else {
            getString(R.string.fs_mode_stopwatch)
        }
        val state = if (running) getString(R.string.fs_running) else getString(R.string.fs_paused)
        status.text = "$modeText · $state"
    }

    private fun start() {
        running = true
        if (mode == MODE_DOWN) {
            endStamp = System.currentTimeMillis() + remaining
        } else {
            startStamp = System.currentTimeMillis()
        }
        playButton.text = getString(R.string.fs_pause)
        updateStatus()
        handler.post(ticker)
        scheduleHide()
    }

    private fun pause() {
        if (mode == MODE_DOWN) {
            remaining = currentRemaining()
        } else {
            elapsed = currentElapsed()
        }
        running = false
        handler.removeCallbacks(ticker)
        playButton.text = getString(R.string.fs_start)
        creditFocus()
        updateStatus()
        updateDisplay()
    }

    private fun resetAll() {
        running = false
        handler.removeCallbacks(ticker)
        creditFocus()
        remaining = totalMs
        elapsed = 0L
        playButton.text = getString(R.string.fs_start)
        updateStatus()
        updateDisplay()
    }

    private fun finishCountdown() {
        running = false
        handler.removeCallbacks(ticker)
        remaining = 0L
        creditedMinutes += (totalMs / 60_000L).toInt()
        Store.addTodayFocusMinutes(this, (totalMs / 60_000L).toInt())
        WidgetCommon.refreshAll(this)
        playButton.text = getString(R.string.fs_start)
        status.text = getString(R.string.fs_done)
        setControlsVisible(true)
        updateDisplay()
    }

    /** Kronometrede geçen süreyi odak dakikası olarak kaydeder. */
    private fun creditFocus() {
        if (mode != MODE_WATCH) return
        val minutes = (elapsed / 60_000L).toInt() - creditedMinutes
        if (minutes > 0) {
            Store.addTodayFocusMinutes(this, minutes)
            creditedMinutes += minutes
            WidgetCommon.refreshAll(this)
        }
    }

    override fun onPause() {
        super.onPause()
        // v10.12 · D22: ses motor dolaşımda — sayaç koşarken akış sürebilir
        // (sayaç duruyorsa yalnız görsel efektler susar).
        fx.stop()
        fxFront.stop()
        clock.setBurning(false)
    }

    override fun onResume() {
        super.onResume()
        // v10.12 · D22: motordaki güncel akış görünüme yansır
        secimYansit(SesManzarasi.calanIndeks)
        if (selectedSound >= 0) {
            fx.start()
            fxFront.start()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // v10.12 · D22: sayaç koşmuyorsa ön dinleme de sessizce kapansın
        runCatching { SesManzarasi.ekranKapandi(this) }
        super.onDestroy()
    }
}
