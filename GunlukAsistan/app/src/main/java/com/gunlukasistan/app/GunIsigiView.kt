package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * v10.11 · ULTRA-30 A3 — Gün ışığı şeridi (ana ekran).
 *
 * Doğuş → batım ilerlemesi ince bir ray üzerinde: geçen kısım parlak
 * dolar, güneş konumu noktası ray üstünde kayar. Geceyse ray söner ve
 * üstüne kesti çizilir — "ışık dışı" anlamına gelir (çift-kod biçimi:
 * hem renk hem işaret, A6 ilkesi).
 *
 * Hesap [GunIsigiHesap] içinde saf tutulur — birim testi Context'siz.
 */
class GunIsigiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        /**
         * 0..100 arası doluluk; gece/verisiz durum -1.
         * Bilinçli karar: doğuş ÖNCESİ de gece sayılır (aksi hâlde
         * "kararmaya 14 saat" gibi saçma bir metin çıkardı).
         */
        fun yuzde(dogusDk: Int, batisDk: Int, simdiDk: Int): Int {
            if (dogusDk < 0 || batisDk <= dogusDk) return -1
            if (simdiDk < dogusDk) return -1 // gün doğmadı
            if (simdiDk >= batisDk) return -1 // akşam/batım sonrası = gece
            return ((simdiDk - dogusDk) * 100) / (batisDk - dogusDk)
        }

        /** Batış simdisine kalan süre (geceyse 0, veri yoksa -1). */
        fun kararmayaKalanDk(dogusDk: Int, batisDk: Int, simdiDk: Int): Int =
            if (yuzde(dogusDk, batisDk, simdiDk) < 0) -1
            else (batisDk - simdiDk).coerceAtLeast(0)
    }

    private var dolu = 0          // 0..100, -1 = gece/verisiz
    private var vurguRenk = 0xFFE69F00.toInt()
    private var zeminRenk = 0x33888888

    private val zeminBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val doluBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val gunesBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val halkaBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.6f
    }

    private val rekt = RectF()

    /** Veri + renk tek seferde; çağıran [invalidate] etmezse yeniden çizilmez. */
    fun ayarla(yuzde: Int, vurgu: Int, zemin: Int) {
        dolu = yuzde.coerceIn(-1, 100)
        vurguRenk = vurgu
        zeminRenk = zemin
        doluBoya.color = vurguRenk
        gunesBoya.color = vurguRenk
        halkaBoya.color = vurguRenk
        zeminBoya.color = zeminRenk
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val d = resources.displayMetrics.density
        val ray = h * 0.62f              // rayın dikey merkezi payı
        val yaricap = h / 2f

        rekt.set(0f, ray - yaricap * 0.35f, w, ray + yaricap * 0.35f)
        canvas.drawRoundRect(rekt, yaricap, yaricap, zeminBoya)

        if (dolu >= 0) {
            // Dolu kısım + güneş topuzu
            val px = (w * dolu) / 100f
            if (px > 0) {
                rekt.set(0f, ray - yaricap * 0.35f, px, ray + yaricap * 0.35f)
                canvas.drawRoundRect(rekt, yaricap, yaricap, doluBoya)
            }
            val topR = h * 0.30f
            canvas.drawCircle(px.coerceIn(topR, w - topR), ray, topR, gunesBoya)
            canvas.drawCircle(px.coerceIn(topR, w - topR), ray, topR, halkaBoya)
            // Işın çizgileri (statik 4 kısa çizgi)
            val outR = topR * 1.9f
            val inR = topR * 1.35f
            val pxC = px.coerceIn(topR, w - topR)
            for (a in 0..3) {
                val ang = Math.toRadians((45 + a * 90).toDouble())
                canvas.drawLine(
                    pxC + (inR * Math.cos(ang)).toFloat(), ray + (inR * Math.sin(ang)).toFloat(),
                    pxC + (outR * Math.cos(ang)).toFloat(), ray + (outR * Math.sin(ang)).toFloat(),
                    halkaBoya
                )
            }
        } else {
            // Gece işareti: rayın ortasında sönük halka + orta çizgi
            val r = h * 0.22f
            canvas.drawCircle(w / 2f, ray, r, halkaBoya)
            canvas.drawLine(w / 2f, ray - r, w / 2f, ray + r, halkaBoya)
        }
    }
}
