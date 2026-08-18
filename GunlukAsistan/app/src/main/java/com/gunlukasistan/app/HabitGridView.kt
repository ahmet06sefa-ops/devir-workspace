package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

/**
 * Referans tasarımdaki "Günlük Alışkanlıklar" ızgarası (v6.1).
 *
 * Her satır bir alışkanlık, her sütun bir hafta. Hafta içinde 5 gün noktası
 * bulunur: tamamlananlar dolu ✓, boşlar sadece çember. Sağda aylık yüzde.
 * Her hafta farklı bir neon renkle vurgulanır.
 */
class HabitGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** Bir alışkanlık satırı: başlık + 4 haftalık günlük durum + yüzde. */
    class Row(
        val emoji: String,
        val title: String,
        /** 4 hafta × 5 gün; true = tamamlandı. */
        val weeks: Array<BooleanArray>,
        val percent: Int
    )

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val headerBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    private val cellRect = RectF()
    private var rows: List<Row> = emptyList()

    /** Hafta sütunlarının vurgu renkleri. */
    private val weekColors = intArrayOf(
        0xFF2BCFD0.toInt(), 0xFF54CA5A.toInt(), 0xFF9B6BFF.toInt(), 0xFFFFCF50.toInt()
    )

    private val weekLabels = arrayOf("1. Hafta", "2. Hafta", "3. Hafta", "4. Hafta")

    init {
        val bold = try {
            ResourcesCompat.getFont(context, R.font.poppins_semibold)
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
        headerPaint.typeface = bold
        pctPaint.typeface = bold
        textPaint.color = Color.parseColor("#C8D2DE")
        headerPaint.color = Color.parseColor("#E8EEF6")
    }

    fun setRows(data: List<Row>) {
        rows = data
        requestLayout()
        invalidate()
    }

    /** Satır yüksekliği — dar ekranda da okunabilir kalsın diye alt sınırlı. */
    private fun rowHeight(w: Int): Float =
        (w * 0.105f).coerceAtLeast(44f * resources.displayMetrics.density / 3f)

    private fun headerHeight(w: Int): Float = (w * 0.082f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (headerHeight(w) + rowHeight(w) * rows.size.coerceAtLeast(1) +
            w * 0.025f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rows.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()

        // Sol sütun (başlıklar) genişliği, sağ sütun (yüzde) genişliği
        val nameW = w * 0.335f
        val pctW = w * 0.10f
        val gridW = w - nameW - pctW
        val colW = gridW / 4f
        val headerH = headerHeight(width)
        val rowH = rowHeight(width)

        textPaint.textSize = w * 0.032f
        headerPaint.textSize = w * 0.027f
        pctPaint.textSize = w * 0.031f

        // ---- Başlık şeridi: hafta sütunları ----
        for (c in 0 until 4) {
            val left = nameW + c * colW
            cellRect.set(left, 0f, left + colW, headerH)
            headerBg.color = Color.argb(
                34,
                Color.red(weekColors[c]), Color.green(weekColors[c]), Color.blue(weekColors[c])
            )
            canvas.drawRect(cellRect, headerBg)
            val fm = headerPaint.fontMetrics
            canvas.drawText(
                weekLabels[c],
                left + colW / 2f,
                headerH / 2f - (fm.ascent + fm.descent) / 2f,
                headerPaint
            )
        }
        // "Bu Ay" başlığı
        val fmH = headerPaint.fontMetrics
        canvas.drawText(
            context.getString(R.string.grid_this_month),
            nameW + gridW + pctW / 2f,
            headerH / 2f - (fmH.ascent + fmH.descent) / 2f,
            headerPaint
        )

        // ---- Satırlar ----
        rows.forEachIndexed { r, row ->
            val top = headerH + r * rowH
            val centerY = top + rowH / 2f

            // Alışkanlık adı
            val fm = textPaint.fontMetrics
            val baseline = centerY - (fm.ascent + fm.descent) / 2f
            val label = "${row.emoji}  ${row.title}"
            canvas.drawText(
                ellipsize(label, nameW - w * 0.02f),
                w * 0.012f, baseline, textPaint
            )

            // Hafta hücreleri
            for (c in 0 until 4) {
                val days = row.weeks.getOrNull(c) ?: BooleanArray(5)
                val left = nameW + c * colW
                val dotR = rowH * 0.155f
                val spacing = colW / 5f
                for (d in 0 until 5) {
                    val cx = left + spacing * (d + 0.5f)
                    val done = days.getOrElse(d) { false }
                    if (done) {
                        dotPaint.style = Paint.Style.FILL
                        dotPaint.color = weekColors[c]
                        canvas.drawCircle(cx, centerY, dotR, dotPaint)
                        // Tik işareti
                        checkPaint.color = Color.parseColor("#0A1420")
                        checkPaint.strokeWidth = dotR * 0.34f
                        canvas.drawLine(
                            cx - dotR * 0.40f, centerY,
                            cx - dotR * 0.10f, centerY + dotR * 0.34f, checkPaint
                        )
                        canvas.drawLine(
                            cx - dotR * 0.10f, centerY + dotR * 0.34f,
                            cx + dotR * 0.44f, centerY - dotR * 0.36f, checkPaint
                        )
                    } else {
                        dotPaint.style = Paint.Style.STROKE
                        dotPaint.strokeWidth = dotR * 0.24f
                        dotPaint.color = Color.parseColor("#25313F")
                        canvas.drawCircle(cx, centerY, dotR, dotPaint)
                    }
                }
            }

            // Aylık yüzde
            pctPaint.color = weekColors[r % weekColors.size]
            canvas.drawText(
                "%${row.percent}",
                w - w * 0.012f, baseline, pctPaint
            )
        }
    }

    /** Metni verilen genişliğe sığdırır, taşarsa kısaltır. */
    private fun ellipsize(text: String, maxWidth: Float): String {
        if (textPaint.measureText(text) <= maxWidth) return text
        var cut = text.length
        while (cut > 3 && textPaint.measureText(text.substring(0, cut) + "…") > maxWidth) {
            cut--
        }
        return text.substring(0, cut.coerceAtLeast(1)) + "…"
    }
}
