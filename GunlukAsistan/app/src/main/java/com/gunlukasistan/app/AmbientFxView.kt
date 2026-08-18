package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tam ekran zamanlayıcının arka planındaki atmosfer efekti (v5.7).
 *
 * Seçilen ortam sesine göre farklı bir parçacık sistemi çizer:
 * şömine için yükselen kor ve alev parıltısı, yağmur için damlalar,
 * orman için süzülen yapraklar, dalga için ufuk çizgileri…
 *
 * Mikrofon kullanılmaz; animasyon sesin ritmini taklit eden
 * yumuşak bir "nabız" ile sürülür.
 */
class AmbientFxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        const val FX_NONE = -1
        const val FX_RAIN = 0
        const val FX_WAVE = 1
        const val FX_FOREST = 2
        const val FX_FIRE = 3
        const val FX_WIND = 4
        const val FX_CAFE = 5
        const val FX_CRICKET = 6
        const val FX_WHITE = 7

        private const val FRAME_MS = 33L      // ~30 fps, pil dostu
        private const val MAX_PARTICLES = 90
    }

    private class Particle {
        var x = 0f; var y = 0f
        var vx = 0f; var vy = 0f
        var size = 0f
        var life = 0f
        var maxLife = 1f
        var seed = 0f
    }

    private val rng = Random(System.nanoTime())
    private val particles = ArrayList<Particle>(MAX_PARTICLES)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    /** Alev dillerini çizmek için ayrı fırça (gradyanı sık değişir). */
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var fx = FX_NONE
    private var running = false
    /** Ön plan katmanı: yalnızca saatin önüne geçecek alevleri çizer. */
    private var foreground = false
    private var frame = 0L
    /** 0..1 arası yumuşak nabız — sesin şiddetini taklit eder. */
    private var pulse = 0.5f
    private var pulseTarget = 0.5f

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            step()
            invalidate()
            postDelayed(this, FRAME_MS)
        }
    }

    /** Bu görünümü ön plan katmanı yapar (saatin üstüne çizer). */
    fun setForeground(value: Boolean) {
        foreground = value
    }

    /** Efekti ayarlar. [FX_NONE] verilirse animasyon durur. */
    fun setEffect(effect: Int) {
        if (fx == effect) return
        fx = effect
        particles.clear()
        if (effect == FX_NONE) {
            stop()
        } else {
            seed()
            start()
        }
        invalidate()
    }

    fun start() {
        if (running || fx == FX_NONE) return
        running = true
        removeCallbacks(ticker)
        post(ticker)
    }

    fun stop() {
        running = false
        removeCallbacks(ticker)
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (fx != FX_NONE) seed()
    }

    // ---------------- Parçacık üretimi ----------------

    private fun count(): Int = when (fx) {
        FX_RAIN -> 80
        FX_FIRE -> 46
        FX_FOREST -> 22
        FX_WIND -> 40
        FX_CAFE -> 26
        FX_CRICKET -> 30
        FX_WAVE -> 7
        FX_WHITE -> 60
        else -> 0
    }

    private fun seed() {
        particles.clear()
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        repeat(count()) {
            particles.add(newParticle(w, h, initial = true))
        }
    }

    private fun newParticle(w: Int, h: Int, initial: Boolean = false): Particle {
        val p = Particle()
        p.seed = rng.nextFloat()
        when (fx) {
            FX_RAIN -> {
                p.x = rng.nextFloat() * w
                p.y = if (initial) rng.nextFloat() * h else -rng.nextFloat() * 120f
                p.vy = 14f + rng.nextFloat() * 16f
                p.vx = -1.5f - rng.nextFloat()
                p.size = 8f + rng.nextFloat() * 16f
            }
            FX_FIRE -> {
                // Ateşin içinden fırlayan kıvılcımlar (referanstaki gibi geniş yayılır)
                p.x = w * (0.06f + rng.nextFloat() * 0.88f)
                p.y = if (initial) rng.nextFloat() * h else h * (0.86f + rng.nextFloat() * 0.2f)
                p.vy = -(1.6f + rng.nextFloat() * 3.4f)
                p.vx = (rng.nextFloat() - 0.5f) * 1.5f
                p.size = 1.4f + rng.nextFloat() * 3.2f
                p.maxLife = 70f + rng.nextFloat() * 110f
            }
            FX_FOREST -> {
                p.x = rng.nextFloat() * w
                p.y = if (initial) rng.nextFloat() * h else -rng.nextFloat() * 80f
                p.vy = 0.7f + rng.nextFloat() * 1.1f
                p.vx = (rng.nextFloat() - 0.5f) * 0.6f
                p.size = 5f + rng.nextFloat() * 7f
            }
            FX_WIND -> {
                p.x = if (initial) rng.nextFloat() * w else -rng.nextFloat() * 200f
                p.y = rng.nextFloat() * h
                p.vx = 5f + rng.nextFloat() * 9f
                p.vy = (rng.nextFloat() - 0.5f) * 0.5f
                p.size = 26f + rng.nextFloat() * 70f
            }
            FX_CAFE -> {
                // Yukarı süzülen buhar
                p.x = rng.nextFloat() * w
                p.y = if (initial) rng.nextFloat() * h else h + 20f
                p.vy = -(0.5f + rng.nextFloat() * 0.8f)
                p.vx = (rng.nextFloat() - 0.5f) * 0.4f
                p.size = 18f + rng.nextFloat() * 40f
                p.maxLife = 140f + rng.nextFloat() * 120f
            }
            FX_CRICKET -> {
                // Yanıp sönen ateş böcekleri
                p.x = rng.nextFloat() * w
                p.y = rng.nextFloat() * h
                p.vx = (rng.nextFloat() - 0.5f) * 0.7f
                p.vy = (rng.nextFloat() - 0.5f) * 0.7f
                p.size = 2.5f + rng.nextFloat() * 3f
                p.maxLife = 120f + rng.nextFloat() * 160f
            }
            FX_WAVE -> {
                p.y = h * (0.45f + it_index() * 0.075f)
                p.size = 1.5f + rng.nextFloat() * 2f
            }
            FX_WHITE -> {
                p.x = rng.nextFloat() * w
                p.y = rng.nextFloat() * h
                p.size = 1.5f + rng.nextFloat() * 2.5f
                p.maxLife = 8f + rng.nextFloat() * 20f
            }
        }
        p.life = if (initial) rng.nextFloat() * p.maxLife else 0f
        return p
    }

    /** Dalga çizgilerini eşit aralıklı yaymak için sayaç. */
    private var waveIndex = 0
    private fun it_index(): Float {
        val v = waveIndex % 7
        waveIndex++
        return v.toFloat()
    }

    // ---------------- Animasyon adımı ----------------

    private fun step() {
        frame++
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)

        // Yumuşak nabız: sesin dalgalanmasını taklit eder
        if (frame % 10L == 0L) {
            pulseTarget = 0.35f + rng.nextFloat() * 0.65f
        }
        pulse += (pulseTarget - pulse) * 0.08f

        val iterator = particles.iterator()
        val dead = ArrayList<Particle>()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life += 1f

            when (fx) {
                FX_FIRE -> {
                    // Yükseldikçe yavaşla ve yalpala
                    p.x += p.vx + sin((p.life + p.seed * 60f) * 0.06f) * 0.7f
                    p.y += p.vy * (0.6f + pulse * 0.7f)
                    p.vy *= 0.995f
                }
                FX_CAFE -> {
                    p.x += p.vx + sin(p.life * 0.02f + p.seed * 6f) * 0.5f
                    p.y += p.vy
                }
                FX_CRICKET -> {
                    p.x += p.vx
                    p.y += p.vy
                }
                FX_WAVE -> {
                    // Dalga çizgileri yerinde salınır
                }
                else -> {
                    p.x += p.vx
                    p.y += p.vy
                }
            }

            val out = when (fx) {
                FX_FIRE, FX_CAFE -> p.y < -60f || p.life > p.maxLife
                FX_RAIN, FX_FOREST -> p.y > h + 60f
                FX_WIND -> p.x > w + 220f
                FX_CRICKET, FX_WHITE -> p.life > p.maxLife
                else -> false
            }
            if (out) dead.add(p)
        }
        dead.forEach { particles.remove(it) }
        while (particles.size < count()) {
            particles.add(newParticle(w, h))
        }
    }

    // ---------------- Çizim ----------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fx == FX_NONE) return
        val w = width.toFloat()
        val h = height.toFloat()

        if (foreground) {
            // Ön planda yalnızca ateşin uzun dilleri ve kıvılcımları görünür
            if (fx == FX_FIRE) drawFireFront(canvas, w, h)
            return
        }

        when (fx) {
            FX_FIRE -> drawFire(canvas, w, h)
            FX_RAIN -> drawRain(canvas, w, h)
            FX_FOREST -> drawForest(canvas, w, h)
            FX_WAVE -> drawWave(canvas, w, h)
            FX_WIND -> drawWind(canvas, w, h)
            FX_CAFE -> drawCafe(canvas, w, h)
            FX_CRICKET -> drawCricket(canvas, w, h)
            FX_WHITE -> drawWhite(canvas, w, h)
        }
    }

    /**
     * Şömine: referans görseldeki gibi ekranı saran gerçek ateş.
     * Katmanlar: sıcak zemin parıltısı → arka duman → büyük alev dilleri → kıvılcımlar.
     */
    private fun drawFire(canvas: Canvas, w: Float, h: Float) {
        // 1) Üstte yükselen duman perdesi
        glowPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.62f,
            intArrayOf(
                Color.argb((30 + pulse * 18).toInt(), 92, 38, 22),
                Color.argb(16, 60, 24, 14),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h * 0.62f, glowPaint)

        // 2) Alttan yükselen ana ısı
        val glowH = h * (0.46f + pulse * 0.12f)
        glowPaint.shader = LinearGradient(
            0f, h - glowH, 0f, h,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((26 + pulse * 22).toInt(), 170, 45, 12),
                Color.argb((62 + pulse * 40).toInt(), 235, 95, 22),
                Color.argb((105 + pulse * 50).toInt(), 255, 140, 45)
            ),
            floatArrayOf(0f, 0.45f, 0.80f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h - glowH, w, h, glowPaint)
        glowPaint.shader = null

        // 3) Büyük alev dilleri — ekranın altından yukarı uzanır
        drawBigFlames(canvas, w, h)

        // 4) Kıvılcımlar ve korlar
        particles.forEach { p ->
            val t = (p.life / p.maxLife).coerceIn(0f, 1f)
            val fade = 1f - t
            val alpha = (230 * fade * fade).toInt().coerceIn(0, 255)
            val g = (200 * (1f - t * 0.8f)).toInt().coerceIn(40, 255)
            val b = (70 * (1f - t)).toInt().coerceIn(0, 100)
            // Kıvılcım izi
            paint.color = Color.argb(alpha / 3, 255, g, b)
            canvas.drawCircle(p.x, p.y + p.size * 1.6f, p.size * 0.7f, paint)
            // Kıvılcım
            paint.color = Color.argb(alpha, 255, g, b)
            canvas.drawCircle(p.x, p.y, p.size * (0.5f + fade * 0.6f), paint)
        }
    }

    /**
     * Ön plan ateşi: saatin önüne geçen birkaç uzun alev + yükselen kıvılcımlar.
     * Yarı saydam çizilir ki rakamlar okunabilir kalsın.
     */
    private fun drawFireFront(canvas: Canvas, w: Float, h: Float) {
        val baseY = h * 1.02f
        // Sadece 3 uzun dil — ekranı boğmasın
        for (i in 0 until 3) {
            val seed = i * 1.37f
            val phase = frame * 0.05f + seed * 6.28f
            val cx = w * (0.24f + i * 0.26f)
            val hgt = h * (0.72f + (sin(phase) * 0.5f + 0.5f) * 0.34f) * (0.8f + pulse * 0.4f)
            val wid = w * (0.075f + (sin(phase * 0.8f) * 0.5f + 0.5f) * 0.03f)
            canvas.saveLayerAlpha(0f, 0f, w, h, 150)
            drawFlameShape(canvas, cx, baseY, wid, hgt, phase)
            canvas.restore()
        }
        // Önde uçuşan birkaç kıvılcım
        particles.take(14).forEach { p ->
            val t = (p.life / p.maxLife).coerceIn(0f, 1f)
            val a = (200 * (1f - t) * (1f - t)).toInt().coerceIn(0, 255)
            paint.color = Color.argb(a, 255, (210 * (1f - t * 0.7f)).toInt().coerceIn(60, 255), 80)
            canvas.drawCircle(p.x, p.y - h * 0.12f, p.size * 0.8f, paint)
        }
    }

    /** Ekran genişliğine yayılmış, canlı salınan büyük alev dilleri. */
    private fun drawBigFlames(canvas: Canvas, w: Float, h: Float) {
        val columns = 7
        val baseY = h * 1.02f
        for (i in 0 until columns) {
            val seed = i * 0.7139f
            val phase = frame * 0.045f + seed * 6.28f
            val cx = w * (0.07f + i * (0.86f / (columns - 1)))
            // Her sütun farklı yükseklikte nefes alır
            val hgt = h * (0.42f + (sin(phase) * 0.5f + 0.5f) * 0.34f) * (0.75f + pulse * 0.5f)
            val wid = w * (0.11f + (sin(phase * 0.7f) * 0.5f + 0.5f) * 0.05f)
            drawFlameShape(canvas, cx, baseY, wid, hgt, phase)
        }
        // Ortada daha büyük bir alev
        val cphase = frame * 0.038f
        drawFlameShape(
            canvas, w * 0.5f, baseY,
            w * (0.16f + (sin(cphase) * 0.5f + 0.5f) * 0.05f),
            h * (0.60f + (sin(cphase * 1.2f) * 0.5f + 0.5f) * 0.28f) * (0.8f + pulse * 0.4f),
            cphase
        )
    }

    /** Tek bir alev dili — kıvrılarak yükselen, uçta sönen organik şekil. */
    private fun drawFlameShape(
        canvas: Canvas, cx: Float, baseY: Float,
        width: Float, height: Float, phase: Float
    ) {
        val path = android.graphics.Path()
        val tipSway = sin(phase * 1.6f) * width * 0.42f
        val midSway = sin(phase * 1.15f + 1.1f) * width * 0.26f

        path.moveTo(cx - width * 0.5f, baseY)
        // Sol kenar: tabandan uca kıvrılarak
        path.cubicTo(
            cx - width * 0.55f, baseY - height * 0.34f,
            cx - width * 0.34f + midSway, baseY - height * 0.66f,
            cx + tipSway, baseY - height
        )
        // Sağ kenar: uçtan tabana
        path.cubicTo(
            cx + width * 0.34f + midSway, baseY - height * 0.66f,
            cx + width * 0.55f, baseY - height * 0.34f,
            cx + width * 0.5f, baseY
        )
        path.close()

        flamePaint.shader = LinearGradient(
            cx, baseY, cx, baseY - height,
            intArrayOf(
                Color.argb(225, 255, 215, 110),
                Color.argb(205, 255, 130, 30),
                Color.argb(130, 225, 55, 12),
                Color.argb(0, 130, 20, 6)
            ),
            floatArrayOf(0f, 0.26f, 0.60f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, flamePaint)
        flamePaint.shader = null

        // İç çekirdek — daha parlak ve dar
        val corePath = android.graphics.Path()
        val cw = width * 0.42f
        val ch = height * 0.62f
        corePath.moveTo(cx - cw * 0.5f, baseY)
        corePath.cubicTo(
            cx - cw * 0.5f, baseY - ch * 0.4f,
            cx - cw * 0.28f + midSway * 0.5f, baseY - ch * 0.72f,
            cx + tipSway * 0.5f, baseY - ch
        )
        corePath.cubicTo(
            cx + cw * 0.28f + midSway * 0.5f, baseY - ch * 0.72f,
            cx + cw * 0.5f, baseY - ch * 0.4f,
            cx + cw * 0.5f, baseY
        )
        corePath.close()
        flamePaint.shader = LinearGradient(
            cx, baseY, cx, baseY - ch,
            intArrayOf(
                Color.argb(200, 255, 250, 205),
                Color.argb(150, 255, 205, 90),
                Color.argb(0, 255, 140, 40)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(corePath, flamePaint)
        flamePaint.shader = null
    }

    /** Yağmur: eğik damla çizgileri + hafif mavi sis. */
    private fun drawRain(canvas: Canvas, w: Float, h: Float) {
        glowPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.argb(30, 60, 110, 165), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        paint.strokeWidth = 1.7f
        paint.strokeCap = Paint.Cap.ROUND
        particles.forEach { p ->
            paint.color = Color.argb((70 + p.seed * 90).toInt(), 175, 205, 240)
            canvas.drawLine(p.x, p.y, p.x + p.vx * 1.6f, p.y + p.size, paint)
        }
    }

    /** Orman: süzülen yapraklar + yeşil derinlik. */
    private fun drawForest(canvas: Canvas, w: Float, h: Float) {
        glowPaint.shader = LinearGradient(
            0f, h * 0.35f, 0f, h,
            intArrayOf(Color.TRANSPARENT, Color.argb(46, 40, 95, 45)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        particles.forEach { p ->
            val sway = sin((p.y + p.seed * 200f) * 0.012f) * 16f
            paint.color = Color.argb((60 + p.seed * 70).toInt(), 130, 180, 110)
            canvas.save()
            canvas.rotate((p.y + p.seed * 360f) * 0.35f, p.x + sway, p.y)
            canvas.drawOval(
                p.x + sway - p.size, p.y - p.size * 0.45f,
                p.x + sway + p.size, p.y + p.size * 0.45f, paint
            )
            canvas.restore()
        }
    }

    /** Dalga: yatay salınan ufuk çizgileri. */
    private fun drawWave(canvas: Canvas, w: Float, h: Float) {
        glowPaint.shader = LinearGradient(
            0f, h * 0.4f, 0f, h,
            intArrayOf(Color.TRANSPARENT, Color.argb(52, 25, 85, 130)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        particles.forEachIndexed { index, p ->
            paint.color = Color.argb(
                (36 + index * 12 + pulse * 28).toInt().coerceIn(0, 190),
                140, 195, 235
            )
            val amp = (5f + index * 2.2f) * (0.6f + pulse * 0.7f)
            val speed = 0.02f + index * 0.004f
            var x = 0f
            var first = true
            val path = android.graphics.Path()
            while (x <= w) {
                val y = p.y + sin(x * 0.014f + frame * speed + index) * amp
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                x += 12f
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    /** Rüzgâr: yatay akan ince çizgiler. */
    @Suppress("UNUSED_PARAMETER")
    private fun drawWind(canvas: Canvas, w: Float, h: Float) {
        paint.strokeWidth = 1.5f
        paint.strokeCap = Paint.Cap.ROUND
        particles.forEach { p ->
            paint.color = Color.argb((28 + p.seed * 60).toInt(), 200, 220, 240)
            val wob = sin((p.x + p.seed * 300f) * 0.01f) * 6f
            canvas.drawLine(p.x, p.y + wob, p.x + p.size, p.y + wob, paint)
        }
    }

    /** Kafe: sıcak ışık + yükselen buhar. */
    private fun drawCafe(canvas: Canvas, w: Float, h: Float) {
        glowPaint.shader = RadialGradient(
            w * 0.5f, h * 0.85f, w * 0.7f,
            intArrayOf(Color.argb(40, 190, 130, 70), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, glowPaint)
        glowPaint.shader = null

        particles.forEach { p ->
            val t = (p.life / p.maxLife).coerceIn(0f, 1f)
            paint.color = Color.argb(((1f - t) * 26).toInt().coerceIn(0, 255), 225, 205, 180)
            canvas.drawCircle(p.x, p.y, p.size * (0.5f + t * 0.9f), paint)
        }
    }

    /** Cırcır böcekleri: yanıp sönen noktalar. */
    @Suppress("UNUSED_PARAMETER")
    private fun drawCricket(canvas: Canvas, w: Float, h: Float) {
        particles.forEach { p ->
            val phase = sin(p.life * 0.05f + p.seed * 6.28f)
            val a = (abs(phase) * 190 * (0.5f + pulse * 0.5f)).toInt().coerceIn(0, 255)
            paint.color = Color.argb(a, 210, 235, 150)
            canvas.drawCircle(p.x, p.y, p.size, paint)
            paint.color = Color.argb(a / 4, 210, 235, 150)
            canvas.drawCircle(p.x, p.y, p.size * 3.2f, paint)
        }
    }

    /** Beyaz gürültü: hafif titreşen statik noktalar. */
    @Suppress("UNUSED_PARAMETER")
    private fun drawWhite(canvas: Canvas, w: Float, h: Float) {
        particles.forEach { p ->
            val a = (30 + p.seed * 70 * pulse).toInt().coerceIn(0, 255)
            paint.color = Color.argb(a, 220, 220, 225)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }
}
