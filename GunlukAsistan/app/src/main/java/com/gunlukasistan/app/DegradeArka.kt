package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * v8.3 — Kahraman kart degradesi (öneri 14).
 *
 * ── Sorun ──
 * Ana ekranın en üstündeki büyük kart (sınav geri sayımı + günlük
 * hedef + günün sözü) düz `colorPrimaryContainer` rengindeydi.
 * Ekranın en önemli kartı, altındaki istatistik kartlarından hiçbir
 * görsel ayrıcalığa sahip değildi.
 *
 * ── Neden kod, neden XML gradient değil ──
 * `<shape><gradient>` sabit renk ister. Bizde 9 tema × açık/koyu =
 * 18 varyant var; hepsine ayrı drawable yazmak gerekirdi. Burada
 * renkler `colorPrimaryContainer`'dan çalışma anında türetiliyor.
 *
 * ── Aciliyet rengi ──
 * [aciliyet] 0..1 arasında; sınav yaklaştıkça 1'e gidiyor. Degrade
 * soğuk (sakin) uçtan sıcak (uyarıcı) uca kayıyor. Bu, sayıya
 * bakmadan da "yaklaşıyor" hissi veriyor.
 *
 * ── Doku ──
 * Sağ üstte iki büyük saydam daire. Düz degrade biraz cansız
 * duruyordu; bu daireler derinlik veriyor ama dikkat dağıtmıyor
 * (alfa 10-16/255).
 */
class DegradeArka @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dokuBoya = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 0 = sakin (sınav uzak) · 1 = acil (sınav çok yakın). */
    var aciliyet: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            golgeleyici = null
            invalidate()
        }

    /** Köşe yarıçapı — sarmalayan kartla aynı olmalı. */
    var kose: Float = 0f
        set(v) { field = v; invalidate() }

    private var golgeleyici: Shader? = null
    private var sonGenislik = 0
    private var sonYukseklik = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (golgeleyici == null || sonGenislik != width || sonYukseklik != height) {
            golgeleyici = olustur(w, h)
            sonGenislik = width
            sonYukseklik = height
        }
        boya.shader = golgeleyici
        canvas.drawRoundRect(0f, 0f, w, h, kose, kose, boya)

        // Doku: sağ üstte iki halka
        dokuBoya.style = Paint.Style.FILL
        dokuBoya.color = Color.argb(14, 255, 255, 255)
        canvas.drawCircle(w * 0.88f, -h * 0.12f, h * 0.62f, dokuBoya)
        dokuBoya.color = Color.argb(10, 255, 255, 255)
        canvas.drawCircle(w * 1.02f, h * 0.62f, h * 0.45f, dokuBoya)
    }

    private fun olustur(w: Float, h: Float): Shader {
        val taban = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFEFE2D0.toInt()
        )
        val ikincil = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSecondaryContainer, taban
        )
        val hataRengi = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorErrorContainer, 0xFFF9DEDC.toInt()
        )

        // Aciliyet arttıkça ikinci durak hata rengine kayıyor
        val ust = karistir(taban, aydinlat(taban, 0.10f), 1f)
        val alt = karistir(ikincil, hataRengi, aciliyet * 0.55f)

        return LinearGradient(
            0f, 0f, w * 0.85f, h,
            intArrayOf(ust, karistir(ust, alt, 0.55f), alt),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    /** İki rengi [oran] kadar karıştırır (0 = a, 1 = b). */
    private fun karistir(a: Int, b: Int, oran: Float): Int {
        val o = oran.coerceIn(0f, 1f)
        val t = 1f - o
        return Color.argb(
            (Color.alpha(a) * t + Color.alpha(b) * o).toInt(),
            (Color.red(a) * t + Color.red(b) * o).toInt(),
            (Color.green(a) * t + Color.green(b) * o).toInt(),
            (Color.blue(a) * t + Color.blue(b) * o).toInt()
        )
    }

    /** Rengi beyaza doğru [miktar] kadar çeker. */
    private fun aydinlat(renk: Int, miktar: Float): Int =
        karistir(renk, Color.WHITE, miktar)
}
