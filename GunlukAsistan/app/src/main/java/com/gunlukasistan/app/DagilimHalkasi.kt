package com.gunlukasistan.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.color.MaterialColors

/**
 * v8.5 — Konu dağılımı halka grafiği (öneri 21).
 *
 * ── Sorun ──
 * İlerleme ekranında yalnız ısı haritası ve iki metin satırı vardı.
 * "Hangi konuya ne kadar emek verdim" sorusunun cevabı hiçbir yerde
 * yoktu. `BarChartView`, `SparklineView`, `NetChartView` bileşenleri
 * projede vardı ama İlerleme ekranında **hiç kullanılmıyordu**.
 *
 * ── Neden halka (donut), neden pasta değil ──
 * Ortadaki boşluk toplam değeri yazmak için kullanılıyor; pasta
 * grafikte o alan boşa gider. Ayrıca ince halka dilimleri, dar
 * pasta dilimlerinden daha kolay ayırt ediliyor.
 *
 * ── Dokunma ──
 * Dilime dokununca o dilim dışa taşıyor ve ortada adı+yüzdesi
 * yazıyor. Legend'e (açıklama listesi) gerek kalmıyor — dar
 * ekranda legend zaten okunmuyordu.
 *
 * ── "Diğer" birleştirmesi ──
 * 6'dan fazla konu varsa küçükler tek dilimde toplanıyor. 15 dilimli
 * bir halka okunmaz hale geliyor.
 */
class DagilimHalkasi @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class Dilim(val ad: String, val deger: Int, val renk: Int)

    private var dilimler: List<Dilim> = emptyList()
    private var toplam = 0
    private var seciliIndeks = -1
    private var ilerleme = 1f

    private val yayBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val ortaBuyuk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val ortaKucuk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val oval = RectF()

    /** Ortada gösterilecek varsayılan alt metin ("madde", "dk" vb.). */
    var birim: String = ""

    init {
        runCatching {
            ortaBuyuk.typeface = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_bold)
            ortaKucuk.typeface = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_medium)
        }
    }

    fun ayarla(yeni: List<Dilim>) {
        // Küçükleri "Diğer"de topla
        val sirali = yeni.filter { it.deger > 0 }.sortedByDescending { it.deger }
        dilimler = if (sirali.size <= 6) sirali else {
            val ilk = sirali.take(5)
            val kalan = sirali.drop(5).sumOf { it.deger }
            ilk + Dilim(context.getString(R.string.dg_diger), kalan, 0xFF9E9E9E.toInt())
        }
        toplam = dilimler.sumOf { it.deger }
        seciliIndeks = -1

        if (GorunumAyar.sayiAnimasyonu(context)) {
            runCatching {
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = GrafikDili.SURE_NORMAL   // v9.9: ortak grafik dili
                    interpolator = DecelerateInterpolator(1.6f)
                    addUpdateListener { ilerleme = it.animatedValue as Float; invalidate() }
                    start()
                }
            }.onFailure { ilerleme = 1f; invalidate() }
        } else {
            ilerleme = 1f
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val istenen = (resources.displayMetrics.density * 190).toInt()
        setMeasuredDimension(w, minOf(istenen, if (w > 0) w else istenen))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dilimler.isEmpty() || toplam <= 0) return
        val b = minOf(width, height).toFloat()
        val kalinlik = b * 0.16f
        yayBoya.strokeWidth = kalinlik

        val cx = width / 2f
        val cy = height / 2f
        val r = b / 2f - kalinlik * 0.85f

        var baslangic = -90f
        dilimler.forEachIndexed { i, d ->
            val aci = 360f * d.deger / toplam * ilerleme
            val secili = i == seciliIndeks
            // Seçili dilim biraz dışarı taşsın
            val ek = if (secili) kalinlik * 0.18f else 0f
            oval.set(cx - r - ek, cy - r - ek, cx + r + ek, cy + r + ek)

            yayBoya.color = d.renk
            yayBoya.strokeWidth = if (secili) kalinlik * 1.16f else kalinlik
            // Diliмler arasında 1.2° boşluk — sınırlar belli olsun
            canvas.drawArc(oval, baslangic + 0.6f, (aci - 1.2f).coerceAtLeast(0.4f), false, yayBoya)
            baslangic += aci
        }

        // ---- Orta metin ----
        val onSurface = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurface, 0xFF333333.toInt()
        )
        val soluk = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888.toInt()
        )

        if (seciliIndeks in dilimler.indices) {
            val d = dilimler[seciliIndeks]
            val yuzde = d.deger * 100 / toplam
            ortaBuyuk.color = d.renk
            ortaBuyuk.textSize = b * 0.15f
            canvas.drawText("%$yuzde", cx, cy - b * 0.01f, ortaBuyuk)
            ortaKucuk.color = soluk
            ortaKucuk.textSize = b * 0.072f
            canvas.drawText(kirp(d.ad, b * 0.5f), cx, cy + b * 0.09f, ortaKucuk)
        } else {
            ortaBuyuk.color = onSurface
            ortaBuyuk.textSize = b * 0.17f
            canvas.drawText(toplam.toString(), cx, cy + b * 0.015f, ortaBuyuk)
            ortaKucuk.color = soluk
            ortaKucuk.textSize = b * 0.072f
            canvas.drawText(birim, cx, cy + b * 0.10f, ortaKucuk)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        if (dilimler.isEmpty() || toplam <= 0) return false

        val cx = width / 2f
        val cy = height / 2f
        val dx = event.x - cx
        val dy = event.y - cy
        val uzaklik = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val b = minOf(width, height).toFloat()
        val kalinlik = b * 0.16f
        val r = b / 2f - kalinlik * 0.85f

        // Halkanın dışı veya ortası → seçimi temizle
        if (uzaklik < r - kalinlik || uzaklik > r + kalinlik) {
            if (seciliIndeks != -1) { seciliIndeks = -1; invalidate() }
            return true
        }

        // Açıyı bul (-90° tepe noktası)
        var aci = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
        if (aci < 0) aci += 360f

        var toplamAci = 0f
        dilimler.forEachIndexed { i, d ->
            val dilimAci = 360f * d.deger / toplam
            if (aci >= toplamAci && aci < toplamAci + dilimAci) {
                seciliIndeks = if (seciliIndeks == i) -1 else i
                Titresim.tik(this)
                invalidate()
                return true
            }
            toplamAci += dilimAci
        }
        return true
    }

    private fun kirp(metin: String, genislik: Float): String {
        if (ortaKucuk.measureText(metin) <= genislik) return metin
        var kes = metin.length
        while (kes > 1 && ortaKucuk.measureText(metin.substring(0, kes) + "…") > genislik) kes--
        return metin.substring(0, kes) + "…"
    }
}
