package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * v11.40 — İnteraktif kas haritası görünümü.
 *
 * Önden ve arkadan basitleştirilmiş bir vücut silueti çizer; her kas
 * grubu ayrı renkli, tıklanabilir bir bölgedir. Kas seçilince
 * [onKasSecildi] geri çağrılır.
 *
 * Bölgeler 0-1 normalize koordinatlarda tanımlanır; görünüm boyutuna
 * göre ölçeklenir. Tıklama normalize koordinatlara çevrilip bölge
 * içinde mi diye kontrol edilir.
 */
class KasHaritasiView @JvmOverloads constructor(
    context: Context,
    private val onKasSecildi: (String) -> Unit = {}
) : View(context) {

    var gorunum: Int = GORUNUM_ON
        set(value) {
            field = value
            invalidate()
        }

    private var seciliKas: String? = null

    private val dolu = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cizgi = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val metin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 12f * resources.displayMetrics.density
        isFakeBoldText = true
    }
    private val arka = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val kafaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    companion object {
        const val GORUNUM_ON = 0
        const val GORUNUM_ARKA = 1
    }

    fun sec(kod: String?) {
        seciliKas = kod
        invalidate()
    }

    fun kasRengi(kod: String): Int = when (kod) {
        "neck" -> 0xFFC97B4A.toInt()
        "traps" -> 0xFFB8860B.toInt()
        "shoulders" -> 0xFFE07B39.toInt()
        "chest" -> 0xFFE5533D.toInt()
        "abdominals" -> 0xFFD48A32.toInt()
        "biceps" -> 0xFF2E86AB.toInt()
        "triceps" -> 0xFF3D8B96.toInt()
        "forearms" -> 0xFF6A8CAF.toInt()
        "lats" -> 0xFF7B5EA7.toInt()
        "middle back" -> 0xFF8E6C88.toInt()
        "lower back" -> 0xFFA66A46.toInt()
        "glutes" -> 0xFFC05A7A.toInt()
        "quadriceps" -> 0xFF4C9A5B.toInt()
        "hamstrings" -> 0xFF5B8C5A.toInt()
        "calves" -> 0xFF7BA05B.toInt()
        "abductors" -> 0xFFB08C3B.toInt()
        "adductors" -> 0xFF9C6B93.toInt()
        else -> 0xFF888888.toInt()
    }

    /**
     * Bölge verisi: kas kodu + normalize RectF.
     * Dizi içinde birden çok parça (ör. sol/sağ bacak) aynı koda sahip olabilir.
     */
    private data class B(val kod: String, val r: RectF)

    private val onBolgeler: List<B> = listOf(
        // Boyun
        B("neck", r(0.44f, 0.13f, 0.56f, 0.19f)),
        // Omuzlar (sol + sağ)
        B("shoulders", r(0.32f, 0.18f, 0.40f, 0.30f)),
        B("shoulders", r(0.60f, 0.18f, 0.68f, 0.30f)),
        // Göğüs
        B("chest", r(0.40f, 0.19f, 0.60f, 0.34f)),
        // Karın
        B("abdominals", r(0.42f, 0.34f, 0.58f, 0.52f)),
        // Biceps (sol + sağ)
        B("biceps", r(0.26f, 0.22f, 0.34f, 0.46f)),
        B("biceps", r(0.66f, 0.22f, 0.74f, 0.46f)),
        // Ön kol (sol + sağ)
        B("forearms", r(0.26f, 0.48f, 0.34f, 0.66f)),
        B("forearms", r(0.66f, 0.48f, 0.74f, 0.66f)),
        // Ön bacak (sol + sağ)
        B("quadriceps", r(0.41f, 0.56f, 0.49f, 0.84f)),
        B("quadriceps", r(0.51f, 0.56f, 0.59f, 0.84f)),
        // Baldır (sol + sağ)
        B("calves", r(0.42f, 0.86f, 0.49f, 0.96f)),
        B("calves", r(0.51f, 0.86f, 0.58f, 0.96f)),
        // Kaçıranlar (kalça yanları)
        B("abductors", r(0.37f, 0.54f, 0.41f, 0.62f)),
        B("abductors", r(0.59f, 0.54f, 0.63f, 0.62f))
    )

    private val arkaBolgeler: List<B> = listOf(
        B("neck", r(0.44f, 0.13f, 0.56f, 0.19f)),
        // Trapez (üst omuz/sırt)
        B("traps", r(0.36f, 0.16f, 0.64f, 0.26f)),
        // Orta sırt
        B("middle back", r(0.40f, 0.26f, 0.60f, 0.42f)),
        // Bel
        B("lower back", r(0.42f, 0.42f, 0.58f, 0.54f)),
        // Triceps (kol arkası üst)
        B("triceps", r(0.26f, 0.22f, 0.34f, 0.46f)),
        B("triceps", r(0.66f, 0.22f, 0.74f, 0.46f)),
        // Ön kol (arka)
        B("forearms", r(0.26f, 0.48f, 0.34f, 0.66f)),
        B("forearms", r(0.66f, 0.48f, 0.74f, 0.66f)),
        // Kalça
        B("glutes", r(0.41f, 0.54f, 0.59f, 0.66f)),
        // Arka bacak (sol + sağ)
        B("hamstrings", r(0.41f, 0.66f, 0.49f, 0.86f)),
        B("hamstrings", r(0.51f, 0.66f, 0.59f, 0.86f)),
        // Baldır (sol + sağ)
        B("calves", r(0.42f, 0.86f, 0.49f, 0.96f)),
        B("calves", r(0.51f, 0.86f, 0.58f, 0.96f))
    )

    private fun r(l: Float, t: Float, r: Float, b: Float) = RectF(l, t, r, b)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Arka plan
        arka.color = 0xFFF4EFE7.toInt()
        canvas.drawRoundRect(RectF(0f, 0f, w, h), 24f, 24f, arka)

        val bolgeler = if (gorunum == GORUNUM_ON) onBolgeler else arkaBolgeler

        // Gövde silueti (arka plan) — koyu ten
        kafaPaint.color = 0xFFD9C2A3.toInt()
        // Kafa
        canvas.drawOval(sc(0.40f, 0.03f, 0.60f, 0.13f, w, h), kafaPaint)
        // Gövde
        canvas.drawRoundRect(sc(0.34f, 0.15f, 0.66f, 0.56f, w, h), 26f, 26f, kafaPaint)
        // Kollar
        canvas.drawRoundRect(sc(0.24f, 0.20f, 0.34f, 0.68f, w, h), 20f, 20f, kafaPaint)
        canvas.drawRoundRect(sc(0.66f, 0.20f, 0.76f, 0.68f, w, h), 20f, 20f, kafaPaint)
        // Bacaklar
        canvas.drawRoundRect(sc(0.40f, 0.56f, 0.50f, 0.98f, w, h), 18f, 18f, kafaPaint)
        canvas.drawRoundRect(sc(0.50f, 0.56f, 0.60f, 0.98f, w, h), 18f, 18f, kafaPaint)

        // Kas bölgeleri
        bolgeler.forEach { b ->
            val rect = sc(b.r.left, b.r.top, b.r.right, b.r.bottom, w, h)
            val secili = b.kod == seciliKas
            dolu.color = if (secili) 0xFFFF5252.toInt() else kasRengi(b.kod)
            dolu.alpha = if (secili) 235 else 205
            canvas.drawRoundRect(rect, 14f, 14f, dolu)

            cizgi.color = if (secili) 0xFF8A0000.toInt() else 0x55000000.toInt()
            canvas.drawRoundRect(rect, 14f, 14f, cizgi)
        }

        // Etiketler — çakışmayı azaltmak için her benzersiz kasın ortasına tek etiket
        val gorulen = HashSet<String>()
        bolgeler.forEach { b ->
            if (gorulen.add(b.kod)) {
                val r = b.r
                val cx = ((r.left + r.right) / 2f) * w
                val cy = ((r.top + r.bottom) / 2f) * h
                val etiket = KasRehber.etiket(b.kod)
                metin.color = 0xFFFFFFFF.toInt()
                canvas.drawText(etiket, cx, cy, metin)
            }
        }
    }

    private fun sc(l: Float, t: Float, r: Float, b: Float, w: Float, h: Float): RectF =
        RectF(l * w, t * h, r * w, b * h)

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action != android.view.MotionEvent.ACTION_UP) return true
        val nx = event.x / width
        val ny = event.y / height
        val bolgeler = if (gorunum == GORUNUM_ON) onBolgeler else arkaBolgeler
        // En üstteki (sonda tanımlı) öncelikli — ters sırada ara
        var secilen: String? = null
        for (i in bolgeler.indices.reversed()) {
            val b = bolgeler[i]
            if (nx in b.r.left..b.r.right && ny in b.r.top..b.r.bottom) {
                secilen = b.kod
                break
            }
        }
        if (secilen != null) {
            seciliKas = secilen
            invalidate()
            onKasSecildi(secilen)
        }
        return true
    }
}
