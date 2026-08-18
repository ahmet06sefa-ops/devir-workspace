package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * v8.5 — Konu ağacı bağlantı çizgisi (öneri 22).
 *
 * ── Sorun ──
 * Alt konular düz bir liste halinde sıralanıyordu. Hangi maddenin
 * hangi konuya ait olduğu yalnız konumdan anlaşılıyordu; ekranı
 * kaydırınca bağ kopuyordu. 200 maddelik bir müfredatta gözle takip
 * etmek imkânsızdı.
 *
 * ── Ne çiziyor ──
 * Her alt madde satırının solunda bir ağaç bağlantısı:
 *
 * ```
 *   │
 *   ├──  madde 1
 *   ├──  madde 2
 *   └──  madde 3      ← son madde: dikey çizgi yarıda biter
 * ```
 *
 * ── Neden custom View, neden 9-patch/drawable değil ──
 * Son maddenin çizgisi diğerlerinden farklı (└ vs ├). Drawable ile
 * iki ayrı dosya gerekirdi ve renk temaya uymazdı. Burada tek View,
 * `sonMu` bayrağıyla iki durumu da çiziyor ve rengi konudan alıyor.
 *
 * ── Tamamlanma göstergesi ──
 * Madde tamamlandıysa çizgi dolu, değilse yarı saydam. Ağaç böylece
 * ilerlemenin kendisini de gösteriyor.
 */
class AgacCizgiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val boya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /** Bu, dalın son maddesi mi? (└ çizilir, dikey çizgi yarıda biter) */
    var sonMu: Boolean = false
        set(v) { field = v; invalidate() }

    /** Madde tamamlandı mı? (çizgi doygunluğu) */
    var tamamMi: Boolean = false
        set(v) { field = v; invalidate() }

    /** Çizgi rengi — konunun renk kodundan geliyor. */
    var cizgiRengi: Int = 0xFF9E9E9E.toInt()
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val yg = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        boya.strokeWidth = yg * 1.6f
        boya.color = if (tamamMi) {
            cizgiRengi
        } else {
            Color.argb(90, Color.red(cizgiRengi), Color.green(cizgiRengi), Color.blue(cizgiRengi))
        }

        // Dikey gövde: üstten ortaya (son madde) veya üstten alta
        val x = w * 0.42f
        val orta = h / 2f
        canvas.drawLine(x, 0f, x, if (sonMu) orta else h, boya)

        // Yatay dal: ortadan sağa
        canvas.drawLine(x, orta, w - yg * 2, orta, boya)

        // Uçta küçük nokta — maddenin başladığı yeri belirginleştirir
        boya.style = Paint.Style.FILL
        canvas.drawCircle(w - yg * 2, orta, yg * 2.2f, boya)
        boya.style = Paint.Style.STROKE
    }
}
