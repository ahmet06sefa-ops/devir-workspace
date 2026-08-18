package com.gunlukasistan.app

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fliqlo tarzı çevirmeli (flip) saat göstergesi (v5.7 · v5.8'de ateş modu eklendi).
 *
 * Normal modda koyu kart + açık gri rakam çizer.
 * "Yanan" modda ise rakamlar akkor hâlinde parlar, kenarlarından alev dilleri çıkar.
 */
class FlipClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141414")
    }
    private val splitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#050505")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B9B9B9")
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A8A8A")
        textAlign = Paint.Align.LEFT
    }
    /** Rakamın arkasındaki sıcak hale. */
    private val emberGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#FF5A18")
    }
    /** Rakam kenarındaki kor çizgisi. */
    private val emberStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFC24A")
    }
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val leftRect = RectF()
    private val rightRect = RectF()
    private val textPath = Path()
    private val measure = android.graphics.PathMeasure()
    private val rng = Random(System.nanoTime())

    private var left = "00"
    private var right = "00"
    private var label = ""

    private var flipLeft = 0f
    private var flipRight = 0f
    private var prevLeft = "00"
    private var prevRight = "00"

    /** Ateş modu: rakamlar yanar, kartlar isli görünür. */
    private var burning = false
    private var burnFrame = 0f

    private val flipTicker = object : Runnable {
        override fun run() {
            var busy = false
            if (flipLeft > 0f) { flipLeft -= 0.16f; busy = true }
            if (flipRight > 0f) { flipRight -= 0.16f; busy = true }
            if (flipLeft < 0f) flipLeft = 0f
            if (flipRight < 0f) flipRight = 0f
            invalidate()
            if (busy) postDelayed(this, 24L)
        }
    }

    /** Ateş modunda alevlerin sürekli oynaması için ayrı döngü. */
    private val burnTicker = object : Runnable {
        override fun run() {
            if (!burning) return
            burnFrame += 1f
            invalidate()
            postDelayed(this, 45L)
        }
    }

    init {
        val font = try {
            ResourcesCompat.getFont(context, R.font.poppins_bold)
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
        textPaint.typeface = font
        labelPaint.typeface = font
        emberGlow.typeface = font
        emberStroke.typeface = font
    }

    fun setTime(leftValue: String, rightValue: String, smallLabel: String = "") {
        var changed = false
        if (leftValue != left) {
            prevLeft = left; left = leftValue; flipLeft = 1f; changed = true
        }
        if (rightValue != right) {
            prevRight = right; right = rightValue; flipRight = 1f; changed = true
        }
        if (smallLabel != label) { label = smallLabel; changed = true }
        if (changed) {
            removeCallbacks(flipTicker)
            post(flipTicker)
        }
    }

    /** Rakamların yanmasını açar/kapatır. */
    fun setBurning(value: Boolean) {
        if (burning == value) return
        burning = value
        removeCallbacks(burnTicker)
        if (value) post(burnTicker)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(flipTicker)
        removeCallbacks(burnTicker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val gap = w * 0.035f
        val cardW = (w - gap) / 2f
        val cardH = h.coerceAtMost(cardW * 1.02f)
        val top = (h - cardH) / 2f
        val radius = cardH * 0.13f

        leftRect.set(0f, top, cardW, top + cardH)
        rightRect.set(cardW + gap, top, w, top + cardH)

        textPaint.textSize = cardH * 0.82f
        labelPaint.textSize = cardH * 0.10f
        emberGlow.textSize = textPaint.textSize
        emberStroke.textSize = textPaint.textSize
        emberStroke.strokeWidth = cardH * 0.012f

        drawCard(canvas, leftRect, left, prevLeft, flipLeft, radius, label)
        drawCard(canvas, rightRect, right, prevRight, flipRight, radius, "")
    }

    private fun drawCard(
        canvas: Canvas,
        rect: RectF,
        value: String,
        previous: String,
        flip: Float,
        radius: Float,
        smallLabel: String
    ) {
        // Kart gövdesi — ateş modunda isli/sıcak ton
        if (burning) {
            cardPaint.shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                intArrayOf(
                    Color.parseColor("#E62A1A12"),
                    Color.parseColor("#F2140A07")
                ),
                null, Shader.TileMode.CLAMP
            )
        } else {
            cardPaint.shader = null
            cardPaint.color = Color.parseColor("#141414")
        }
        canvas.drawRoundRect(rect, radius, radius, cardPaint)
        cardPaint.shader = null

        val centerY = rect.centerY()
        val baseline = centerY + textPaint.textSize * 0.36f

        canvas.save()
        canvas.clipRect(rect)

        if (flip > 0.02f) {
            canvas.save()
            canvas.clipRect(rect.left, rect.top, rect.right, centerY)
            val squeeze = flip.coerceIn(0f, 1f)
            canvas.save()
            canvas.scale(1f, 1f - squeeze * 0.9f, rect.centerX(), centerY)
            drawDigits(canvas, previous, rect.centerX(), baseline)
            canvas.restore()
            canvas.restore()

            canvas.save()
            canvas.clipRect(rect.left, centerY, rect.right, rect.bottom)
            drawDigits(canvas, value, rect.centerX(), baseline)
            canvas.restore()
        } else {
            drawDigits(canvas, value, rect.centerX(), baseline)
        }
        canvas.restore()

        // Ortadaki bölme çizgisi
        val lineH = (rect.height() * 0.012f).coerceAtLeast(2f)
        splitPaint.color = if (burning) Color.parseColor("#0A0503") else Color.parseColor("#050505")
        canvas.drawRect(rect.left, centerY - lineH / 2f, rect.right, centerY + lineH / 2f, splitPaint)

        if (smallLabel.isNotEmpty()) {
            labelPaint.color = if (burning) Color.parseColor("#E8873A") else Color.parseColor("#8A8A8A")
            canvas.drawText(
                smallLabel,
                rect.left + rect.width() * 0.07f,
                rect.bottom - rect.height() * 0.10f,
                labelPaint
            )
        }
    }

    /** Rakamları çizer; ateş modunda akkor + alev katmanları ekler. */
    private fun drawDigits(canvas: Canvas, text: String, cx: Float, baseline: Float) {
        if (!burning) {
            textPaint.shader = null
            textPaint.color = Color.parseColor("#B9B9B9")
            canvas.drawText(text, cx, baseline, textPaint)
            return
        }

        // 1) Geniş yumuşak hale — rakamın etrafını ısıtır
        emberGlow.color = Color.argb(120, 255, 90, 20)
        emberGlow.maskFilter = BlurMaskFilter(emberGlow.textSize * 0.16f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawText(text, cx, baseline, emberGlow)
        emberGlow.maskFilter = null

        // 2) Rakam gövdesi — alttan üste akkor geçişi (referanstaki #E96B38 tonu)
        textPaint.shader = LinearGradient(
            0f, baseline - textPaint.textSize * 0.75f, 0f, baseline,
            intArrayOf(
                Color.parseColor("#FFD98A"),
                Color.parseColor("#F5842F"),
                Color.parseColor("#D94E14"),
                Color.parseColor("#8E2A08")
            ),
            floatArrayOf(0f, 0.35f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawText(text, cx, baseline, textPaint)
        textPaint.shader = null

        // 3) Parlak kor kenarı
        emberStroke.color = Color.argb(210, 255, 200, 90)
        canvas.drawText(text, cx, baseline, emberStroke)

        // 4) Rakam konturundan çıkan alev dilleri
        drawFlameTongues(canvas, text, cx, baseline)
    }

    /**
     * Rakamın dış hattını takip ederek yukarı doğru alev dilleri çizer.
     * Konturu Path olarak alıp üzerinde eşit aralıklı noktalar örnekler.
     */
    private fun drawFlameTongues(canvas: Canvas, text: String, cx: Float, baseline: Float) {
        textPath.reset()
        textPaint.getTextPath(text, 0, text.length, cx, baseline, textPath)

        measure.setPath(textPath, false)
        val pos = FloatArray(2)
        val size = textPaint.textSize

        do {
            val len = measure.length
            if (len < 10f) continue
            // Kontur boyunca ~18 piksel aralıkla alev tut
            var d = 0f
            val stepLen = size * 0.075f
            var i = 0
            while (d < len) {
                measure.getPosTan(d, pos, null)
                val px = pos[0]
                val py = pos[1]
                // Her nokta için sabit bir rastgelelik (titremesin, aksın)
                val seed = ((px * 13.7f + py * 7.3f) % 100f) / 100f
                val t = burnFrame * 0.14f + seed * 6.28f
                // Sadece üst yarıdaki konturlardan alev çıksın
                val upward = sin(t) * 0.5f + 0.5f
                val flameH = size * (0.10f + upward * 0.16f) * (0.6f + seed * 0.8f)
                val sway = sin(t * 1.3f + seed * 3f) * size * 0.035f

                drawTongue(canvas, px, py, flameH, sway, seed)
                d += stepLen
                i++
                if (i > 420) break   // güvenlik sınırı
            }
        } while (measure.nextContour())
    }

    /** Tek bir alev dili: aşağıdan yukarı incelen, sarıdan kırmızıya sönen üçgen. */
    private fun drawTongue(canvas: Canvas, x: Float, y: Float, h: Float, sway: Float, seed: Float) {
        if (h <= 1f) return
        val w = h * 0.42f
        val path = Path()
        path.moveTo(x - w * 0.5f, y)
        // Sol kenar yukarı kıvrılır
        path.quadTo(x - w * 0.42f, y - h * 0.55f, x + sway, y - h)
        // Sağ kenar geri iner
        path.quadTo(x + w * 0.42f, y - h * 0.55f, x + w * 0.5f, y)
        path.close()

        flamePaint.shader = LinearGradient(
            x, y, x, y - h,
            intArrayOf(
                Color.argb(190, 255, 170, 40),
                Color.argb(140, 255, 105, 20),
                Color.argb(0, 200, 40, 10)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, flamePaint)
        flamePaint.shader = null

        // Sıcak çekirdek
        if (seed > 0.55f) {
            flamePaint.color = Color.argb(120, 255, 240, 170)
            canvas.drawCircle(x + sway * 0.4f, y - h * 0.28f, w * 0.16f, flamePaint)
        }
    }
}
