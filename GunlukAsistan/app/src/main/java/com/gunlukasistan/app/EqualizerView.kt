package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import java.util.Random

/**
 * Çalan odak sesine eşlik eden animasyonlu ekolayzır görseli.
 * (Gerçek mikrofon izni gerektirmez; süsleme amaçlı yumuşak dalga animasyonu.)
 */
class EqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        private const val BAR_COUNT = 16
        private const val FRAME_MS = 110L
    }

    private val rng = Random()
    private val bars = FloatArray(BAR_COUNT) { 0.12f }
    private val targets = FloatArray(BAR_COUNT) { 0.5f }
    private var playing = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = object : Runnable {
        override fun run() {
            if (!playing) return
            step()
            invalidate()
            postDelayed(this, FRAME_MS)
        }
    }

    init {
        paint.color = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary
        )
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun start() {
        paint.color = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary
        )
        if (playing) return
        playing = true
        removeCallbacks(animator)
        post(animator)
    }

    fun stop() {
        playing = false
        removeCallbacks(animator)
        bars.fill(0.10f)
        targets.fill(0.10f)
        invalidate()
    }

    private fun step() {
        for (i in bars.indices) {
            if (rng.nextFloat() < 0.30f) {
                targets[i] = 0.15f + rng.nextFloat() * 0.85f
            }
            bars[i] += (targets[i] - bars[i]) * 0.35f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val slot = width / (BAR_COUNT * 2f)
        val barWidth = slot * 1.1f
        val centerY = height / 2f
        val maxHalf = height * 0.46f
        for (i in 0 until BAR_COUNT) {
            val half = maxHalf * bars[i].coerceAtLeast(0.06f)
            val left = slot * 0.5f + i * 2f * slot
            canvas.drawRoundRect(
                left, centerY - half, left + barWidth, centerY + half,
                barWidth / 2f, barWidth / 2f, paint
            )
        }
    }
}
