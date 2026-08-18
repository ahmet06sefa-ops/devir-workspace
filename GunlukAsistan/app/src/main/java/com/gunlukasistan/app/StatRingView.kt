package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Referans tasarımdaki neon halka göstergesi (v6.0).
 *
 * Ortasında emoji, çevresinde ilerlemeyi gösteren parlak bir yay.
 * Yayın altında hafif bir parıltı katmanı bulunur (neon hissi).
 */
class StatRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        // v9.9 · Görsel öneri 10 — 🔴 DÜZELTİLEN HATA:
        // `Color.parseColor("#1B2A3A")` sert kodluydu. Açık temada
        // halkanın boş kısmı koyu gri çıkıyor, dolu kısımdan daha
        // baskın görünüyordu. Artık temadan geliyor.
        color = 0xFFE3E6EA.toInt()
    }

    private var renklerHazir = false

    private fun renkleriTazele() {
        if (renklerHazir) return
        renklerHazir = true
        runCatching { trackPaint.color = GrafikDili.izgara(this) }
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()

    /** 0..100 arası ilerleme. */
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    /** Halka rengi (neon palet). */
    var ringColor: Int = 0xFF2BCFD0.toInt()
        set(value) {
            field = value
            invalidate()
        }

    /** Ortadaki simge. */
    var icon: String = ""
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        renkleriTazele()
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h)
        if (size <= 0f) return

        val stroke = size * 0.085f
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke
        glowPaint.strokeWidth = stroke * 2.1f

        val pad = stroke * 0.75f
        val cx = w / 2f
        val cy = h / 2f
        val r = size / 2f - pad
        oval.set(cx - r, cy - r, cx + r, cy + r)

        canvas.drawArc(oval, 0f, 360f, false, trackPaint)

        if (progress > 0) {
            val sweep = 360f * progress / 100f
            glowPaint.color = Color.argb(
                55, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor)
            )
            canvas.drawArc(oval, -90f, sweep, false, glowPaint)
            arcPaint.color = ringColor
            canvas.drawArc(oval, -90f, sweep, false, arcPaint)
        }

        if (icon.isNotEmpty()) {
            iconPaint.textSize = size * 0.34f
            val fm = iconPaint.fontMetrics
            canvas.drawText(icon, cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
        }
    }
}
