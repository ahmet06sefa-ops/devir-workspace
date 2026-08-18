package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * v7.38 — Basit dikey çubuk grafiği.
 *
 * Saat dağılımı, gün dağılımı ve haftalık eğilim için kullanılır.
 * Kütüphane eklemeden, projenin tema renkleriyle uyumlu çizim yapar.
 *
 * Özellikler:
 *  · En yüksek çubuk vurgulanır (colorPrimary), diğerleri soluk
 *  · Alt etiketler otomatik seyreltilir (dar ekranda üst üste binmesin)
 *  · Değer 0 ise ince bir taban çizgisi gösterilir
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var degerler: List<Int> = emptyList()
    private var etiketler: List<String> = emptyList()

    /** Kaç etiketten birini göster (1 = hepsi). */
    private var etiketAtla = 1

    private val cubukBoya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vurguBoya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yaziBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val yogunluk = resources.displayMetrics.density

    init {
        renkleriTazele()
    }

    private fun renkleriTazele() {
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0xFF2BCFD0.toInt()
        )
        val yuzey = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurface, 0xFF888888.toInt()
        )
        vurguBoya.color = vurgu
        // Soluk çubuk: aynı renk, düşük alfa
        cubukBoya.color = (vurgu and 0x00FFFFFF) or 0x55000000
        yaziBoya.color = (yuzey and 0x00FFFFFF) or 0xAA000000.toInt()
        yaziBoya.textSize = GrafikDili.YAZI_KUCUK * yogunluk
    }

    /**
     * Veriyi ayarlar.
     * @param etiketAtlaSayisi 1 = tüm etiketler, 2 = bir atla bir göster…
     */
    fun setData(degerler: List<Int>, etiketler: List<String>, etiketAtlaSayisi: Int = 1) {
        this.degerler = degerler
        this.etiketler = etiketler
        this.etiketAtla = etiketAtlaSayisi.coerceAtLeast(1)
        renkleriTazele()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (degerler.isEmpty()) return

        val enBuyuk = (degerler.maxOrNull() ?: 0).coerceAtLeast(1)
        val etiketAlani = if (etiketler.isEmpty()) 0f else 14f * yogunluk
        val ustBosluk = 4f * yogunluk
        val cizimYuksekligi = height - etiketAlani - ustBosluk
        if (cizimYuksekligi <= 0) return

        val aralik = 2f * yogunluk
        val cubukGenisligi = (width.toFloat() - aralik * (degerler.size - 1)) / degerler.size
        if (cubukGenisligi <= 0) return

        val kose = 3f * yogunluk
        val enBuyukIndeks = degerler.indexOfFirst { it == enBuyuk }

        degerler.forEachIndexed { i, deger ->
            val sol = i * (cubukGenisligi + aralik)
            val sag = sol + cubukGenisligi

            // Değer 0 olsa bile ince taban görünsün
            val oran = deger.toFloat() / enBuyuk
            val yukseklik = (cizimYuksekligi * oran).coerceAtLeast(
                if (deger > 0) 2f * yogunluk else 1f * yogunluk
            )
            val ust = ustBosluk + cizimYuksekligi - yukseklik

            canvas.drawRoundRect(
                RectF(sol, ust, sag, ustBosluk + cizimYuksekligi),
                kose, kose,
                if (i == enBuyukIndeks && deger > 0) vurguBoya else cubukBoya
            )

            // Etiket
            if (etiketler.isNotEmpty() && i % etiketAtla == 0) {
                etiketler.getOrNull(i)?.let { etiket ->
                    canvas.drawText(
                        etiket,
                        sol + cubukGenisligi / 2f,
                        height - 2f * yogunluk,
                        yaziBoya
                    )
                }
            }
        }
    }
}
