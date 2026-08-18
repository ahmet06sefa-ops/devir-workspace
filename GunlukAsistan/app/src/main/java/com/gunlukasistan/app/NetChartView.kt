package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * Deneme netlerinin gelişimini gösteren basit çizgi grafik.
 */
class NetChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var values: List<Int> = emptyList()
    private var maxValue = 120

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f }

    fun setData(netsChronological: List<Int>, max: Int = 120) {
        values = netsChronological
        maxValue = max.coerceAtLeast((values.maxOrNull() ?: 0) + 10)
        val primary = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary
        )
        linePaint.color = primary
        fillPaint.color = primary and 0x22FFFFFF
        dotPaint.color = primary
        textPaint.color = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val padL = 46f
        val padB = 34f
        val padT = 20f
        val w = width - padL - 10f
        val h = height - padT - padB
        if (w <= 0 || h <= 0) return

        fun pointAt(i: Int): Pair<Float, Float> {
            val x = if (values.size == 1) padL + w / 2f
            else padL + w * i / (values.size - 1)
            val y = padT + h * (1f - values[i].toFloat() / maxValue)
            return x to y
        }

        val path = Path()
        values.forEachIndexed { i, _ ->
            val (x, y) = pointAt(i)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (values.size > 1) canvas.drawPath(path, linePaint)

        values.forEachIndexed { i, v ->
            val (x, y) = pointAt(i)
            canvas.drawCircle(x, y, 8f, dotPaint)
            canvas.drawText(v.toString(), x - 12f, y - 14f, textPaint)
        }
    }
}
