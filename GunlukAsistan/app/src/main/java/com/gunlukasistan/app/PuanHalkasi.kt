package com.gunlukasistan.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * v8.4 — Quiz sonuç halkası (öneri 20).
 *
 * ── Sorun ──
 * Quiz bitince `MaterialAlertDialogBuilder` ile düz metin çıkıyordu:
 * "7/10 doğru (%70)". Bir sınavı bitirmenin verdiği his tamamen
 * kayboluyordu. Oysa quiz uygulamanın en çok tekrarlanan eylemi.
 *
 * ── Ne yapıyor ──
 * Yüzdeyi dolarak gösteren halka + ortada büyük sayı. Renk sonuca
 * göre: %90+ yeşil, %60+ amber, altı kırmızı. Halka 900 ms'de
 * doluyor, sayı da onunla birlikte sayıyor.
 *
 * ── Neden StatRingView kullanılmadı ──
 * O bileşen ana ekrandaki küçük istatistik halkaları için yazıldı:
 * ortasında emoji var, kalınlığı sabit oranlı, metin desteği yok.
 * Burada ortada iki satır metin (yüzde + "7/10") gerekiyor ve
 * halka çok daha kalın olmalı.
 */
class PuanHalkasi @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val izBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val yayBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val buyukBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val kucukBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val oval = RectF()

    private var yuzde = 0
    private var hedefYuzde = 0
    private var altMetin = ""

    init {
        runCatching {
            buyukBoya.typeface = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_bold)
            kucukBoya.typeface = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_medium)
        }
    }

    /** Sonuç rengi — dışarıdan da okunabiliyor (başlık rengi için). */
    val sonucRengi: Int
        get() = when {
            hedefYuzde >= 90 -> GrafikDili.BASARI
            hedefYuzde >= 60 -> 0xFFE0A33A.toInt()
            else -> GrafikDili.HATA
        }

    /**
     * Sonucu ayarlar ve animasyonu başlatır.
     *
     * @param dogru doğru sayısı
     * @param toplam soru sayısı
     */
    fun ayarla(dogru: Int, toplam: Int) {
        hedefYuzde = if (toplam <= 0) 0 else dogru * 100 / toplam
        altMetin = "$dogru / $toplam"

        if (!GorunumAyar.sayiAnimasyonu(context)) {
            yuzde = hedefYuzde
            invalidate()
            return
        }
        runCatching {
            ValueAnimator.ofInt(0, hedefYuzde).apply {
                duration = 900
                interpolator = DecelerateInterpolator(1.7f)
                addUpdateListener {
                    yuzde = it.animatedValue as Int
                    invalidate()
                }
                start()
            }
        }.onFailure {
            yuzde = hedefYuzde
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val istenen = (resources.displayMetrics.density * 150).toInt()
        val kenar = if (w > 0) minOf(w, istenen) else istenen
        setMeasuredDimension(kenar, kenar)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = minOf(width, height).toFloat()
        if (b <= 0f) return

        val kalinlik = b * 0.11f
        izBoya.strokeWidth = kalinlik
        yayBoya.strokeWidth = kalinlik

        val renk = sonucRengi
        izBoya.color = Color.argb(38, Color.red(renk), Color.green(renk), Color.blue(renk))
        yayBoya.color = renk

        val pad = kalinlik * 0.7f
        val cx = width / 2f
        val cy = height / 2f
        val r = b / 2f - pad
        oval.set(cx - r, cy - r, cx + r, cy + r)

        canvas.drawArc(oval, 0f, 360f, false, izBoya)
        if (yuzde > 0) {
            canvas.drawArc(oval, -90f, 360f * yuzde / 100f, false, yayBoya)
        }

        // Ortada yüzde
        buyukBoya.color = renk
        buyukBoya.textSize = b * 0.27f
        val fm = buyukBoya.fontMetrics
        canvas.drawText("%$yuzde", cx, cy - (fm.ascent + fm.descent) / 2f - b * 0.045f, buyukBoya)

        // Altında doğru/toplam
        kucukBoya.color = Color.argb(
            190, Color.red(renk), Color.green(renk), Color.blue(renk)
        )
        kucukBoya.textSize = b * 0.115f
        canvas.drawText(altMetin, cx, cy + b * 0.19f, kucukBoya)
    }
}
