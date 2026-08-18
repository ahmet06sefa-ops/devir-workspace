package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Referans tasarımdaki "Günlük İlerleme" çizgi grafiği (v6.1).
 *
 * Yumuşak eğriyle bağlanmış veri noktaları, altında gradyan dolgu,
 * her noktada parlak bir tepe işareti. Neon teal vurgu.
 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = GrafikDili.CIZGI_INCE
        // v9.9 · Görsel öneri 10 — 🔴 DÜZELTİLEN HATA:
        // Burada `Color.parseColor("#16232F")` vardı — KOYU LACİVERT,
        // sert kodlanmış. v8.3'te açık tema eklendi ama bu değer
        // değişmedi. Sonuç: açık temada beyaz zemin üzerinde koyu
        // lacivert ızgara çizgileri — sert ve yanlış görünüyordu.
        // Artık `onDraw`'da temadan okunuyor (bkz. renkleriTazele).
        color = 0xFFE3E6EA.toInt()
    }

    /** Tema renkleri okunmuş mu — her çizimde tekrar okumayalım. */
    private var renklerHazir = false

    /**
     * Tema renklerini bir kez okur.
     *
     * `onDraw` içinde çağrılıyor çünkü yapıcıda (`init`) tema henüz
     * hazır olmayabiliyor: View henüz bir pencereye eklenmemişse
     * `MaterialColors.getColor` varsayılan döner.
     */
    private fun renkleriTazele() {
        if (renklerHazir) return
        renklerHazir = true
        runCatching { gridPaint.color = GrafikDili.izgara(this) }
    }

    private val linePath = Path()
    private val fillPath = Path()

    private var values: FloatArray = FloatArray(0)

    /** Çizgi rengi (neon palet). */
    var lineColor: Int = 0xFF2BCFD0.toInt()
        set(value) {
            field = value
            invalidate()
        }

    /** Veriyi ayarlar; değerler 0..1 aralığına normalize edilir. */
    fun setData(data: List<Float>) {
        if (data.isEmpty()) {
            values = FloatArray(0)
            invalidate()
            return
        }
        val max = data.max().coerceAtLeast(0.0001f)
        values = FloatArray(data.size) { (data[it] / max).coerceIn(0f, 1f) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        renkleriTazele()
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Zemin ızgarası — 3 yatay çizgi
        for (i in 1..3) {
            val y = h * i / 4f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        if (values.size < 2) return

        val padTop = h * 0.14f
        val padBottom = h * 0.10f
        val usableH = h - padTop - padBottom
        val stepX = w / (values.size - 1f)

        fun px(i: Int) = i * stepX
        fun py(i: Int) = padTop + usableH * (1f - values[i])

        // Yumuşak eğri (quadratic, orta noktalardan geçen)
        linePath.reset()
        linePath.moveTo(px(0), py(0))
        for (i in 1 until values.size) {
            val midX = (px(i - 1) + px(i)) / 2f
            val midY = (py(i - 1) + py(i)) / 2f
            linePath.quadTo(px(i - 1), py(i - 1), midX, midY)
        }
        linePath.lineTo(px(values.size - 1), py(values.size - 1))

        // Alt dolgu
        fillPath.reset()
        fillPath.addPath(linePath)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, h,
            intArrayOf(
                Color.argb(90, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.argb(24, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)
        fillPaint.shader = null

        // Dış parıltı + asıl çizgi
        val stroke = (h * 0.022f).coerceIn(2f, 5f)
        glowPaint.strokeWidth = stroke * 2.6f
        glowPaint.color = Color.argb(
            60, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)
        )
        canvas.drawPath(linePath, glowPaint)

        linePaint.strokeWidth = stroke
        linePaint.color = lineColor
        canvas.drawPath(linePath, linePaint)

        // Nokta işaretleri
        val r = (h * 0.028f).coerceIn(2.5f, 6f)
        for (i in values.indices) {
            dotPaint.color = Color.argb(
                70, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)
            )
            canvas.drawCircle(px(i), py(i), r * 2f, dotPaint)
            dotPaint.color = lineColor
            canvas.drawCircle(px(i), py(i), r, dotPaint)
        }
    }
}
