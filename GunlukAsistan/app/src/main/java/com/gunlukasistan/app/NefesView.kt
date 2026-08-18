package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * v10.12 · ULTRA-30 / D19 — Nefes halkası.
 *
 * Soluk bir kılavuz halka içinde nefesle büyüyüp küçülen dolgu daire;
 * merkezde faz adı ve faza kalan saniye. Ritim matematiği tamamen
 * [NefesProgrami]'ndadır — bu sınıf yalnızca çizer.
 *
 * Zamanlama Activity'nin elindedir (33 ms kare döngüsü); görünüm
 * durum bilmez, [durumAyarla] ile ne verilirse onu gösterir.
 */
class NefesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var olcek = 0.55f
    private var fazMetni = ""
    private var kalanMetni = ""
    private var ustMetin = ""

    private var vurguRenk = 0xFF7C6BF5.toInt()
    private var zeminRenk = 0xFF2A2A2E.toInt()
    private var metinRenk = Color.WHITE
    private var ikincilRenk = 0xB3FFFFFF.toInt()
    private var solukRenk = 0x33FFFFFF

    private val halkaBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val dolguBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val kenarBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fazBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val kalanBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val ustBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    init {
        runCatching {
            val kalin = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_bold)
            val normal = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_medium)
            kalanBoya.typeface = kalin
            fazBoya.typeface = normal
            ustBoya.typeface = normal
        }
    }

    fun renkleriAyarla(vurgu: Int, zemin: Int, metin: Int, ikincil: Int, soluk: Int) {
        vurguRenk = vurgu
        zeminRenk = zemin
        metinRenk = metin
        ikincilRenk = ikincil
        solukRenk = soluk
        invalidate()
    }

    fun durumAyarla(yeniOlcek: Float, faz: String, kalan: String, ust: String) {
        olcek = yeniOlcek.coerceIn(0.55f, 1.0f)
        fazMetni = faz
        kalanMetni = kalan
        ustMetin = ust
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val kenar = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            w
        } else min(w, h)
        setMeasuredDimension(kenar, kenar)
    }

    override fun onDraw(canvas: Canvas) {
        val mx = width / 2f
        val my = height / 2f
        val dis = min(width, height) / 2f - 6f
        if (dis <= 0f) return

        // Kılavuz halka (hedef çap)
        halkaBoya.color = solukRenk
        halkaBoya.strokeWidth = dis * 0.02f
        canvas.drawCircle(mx, my, dis, halkaBoya)

        // Nefesle ölçeklenen dolgu
        val yaricap = dis * 0.92f * olcek
        dolguBoya.color = zeminRenk
        canvas.drawCircle(mx, my, yaricap, dolguBoya)
        kenarBoya.color = vurguRenk
        kenarBoya.strokeWidth = dis * 0.028f
        canvas.drawCircle(mx, my, yaricap, kenarBoya)

        // Metinler — iç yarıçapla ölçeklenir
        val ic = yaricap
        if (ustMetin.isNotBlank()) {
            ustBoya.color = ikincilRenk
            ustBoya.textSize = dis * 0.11f
            canvas.drawText(ustMetin, mx, my - ic * 0.42f, ustBoya)
        }
        if (fazMetni.isNotBlank()) {
            fazBoya.color = vurguRenk
            fazBoya.textSize = dis * 0.16f
            canvas.drawText(fazMetni, mx, my - ic * 0.08f, fazBoya)
        }
        if (kalanMetni.isNotBlank()) {
            kalanBoya.color = metinRenk
            kalanBoya.textSize = dis * 0.34f
            val fm = kalanBoya.fontMetrics
            val taban = my + ic * 0.32f - (fm.ascent + fm.descent) / 2f
            canvas.drawText(kalanMetni, mx, taban, kalanBoya)
        }
    }
}
