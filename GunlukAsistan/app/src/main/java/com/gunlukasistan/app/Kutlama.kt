package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * v8.6 — Kutlama efektleri (öneri 24).
 *
 * ── Sorun ──
 * Günlük hedef tamamlanınca bir Toast çıkıyordu: "Hedefe ulaştın".
 * Serinin 30. günü ile sıradan bir gün arasında hiçbir görsel fark
 * yoktu. Uygulamanın bütün amacı devamlılık; başarı anını
 * ödüllendirmemek büyük bir eksikti.
 *
 * ── Düzeltme (önceki notumdaki hata) ──
 * v8.1 notlarında "AmbientFxView altyapısı var, kullanılmıyor"
 * yazmıştım. **Yanlıştı.** `AmbientFxView` ortam sesi görselleri için
 * (yağmur, dalga, ateş) ve `FullscreenTimerActivity` içinde
 * kullanılıyor. Konfeti için uygun değil — parçacıkları döngüsel
 * akıyor, yerçekimi ve dönme yok. Bu yüzden ayrı bir görünüm yazıldı.
 *
 * ── Performans ──
 * En fazla 90 parçacık, ~2,2 saniye. Bitince görünüm kendini
 * ebeveyninden çıkarıyor — arkada boşuna çizim yapan bir katman
 * kalmıyor. `GorunumAyar.animasyonAcik` kapalıysa hiç oluşturulmuyor.
 *
 * ── Neden dokunmaları geçiriyor ──
 * Katman `isClickable = false` ve tüm ekranı kaplıyor; kullanıcı
 * konfeti akarken altındaki düğmelere basabilmeli.
 */
class Kutlama @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAG = "Kutlama"
        private const val SURE_MS = 2200L
        private const val KARE_MS = 16L

        /** Klasik konfeti — hedef tamamlandı. */
        const val TUR_KONFETI = 0
        /** Yukarı fırlayan kıvılcımlar — seri rekoru. */
        const val TUR_HAVAI = 1
        /** Yıldız yağmuru — kurs/rozet tamamlandı. */
        const val TUR_YILDIZ = 2

        /**
         * Etkinliğin kök görünümüne kutlama katmanı ekler.
         *
         * ```
         * Kutlama.goster(activity, Kutlama.TUR_KONFETI)
         * ```
         */
        fun goster(activity: Activity?, tur: Int = TUR_KONFETI) {
            activity ?: return
            if (!GorunumAyar.animasyonAcik(activity)) return
            runCatching {
                val kok = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
                // Zaten akan bir kutlama varsa ikincisini ekleme
                if (kok.findViewWithTag<View>("ga_kutlama") != null) return
                val katman = Kutlama(activity).apply {
                    tag = "ga_kutlama"
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isClickable = false
                    isFocusable = false
                }
                kok.addView(katman)
                katman.basla(tur)
            }.onFailure { android.util.Log.w(TAG, "goster", it) }
        }

        /** Fragment'tan çağrım kolaylığı. */
        fun goster(fragment: androidx.fragment.app.Fragment?, tur: Int = TUR_KONFETI) {
            goster(fragment?.activity, tur)
        }
    }

    // ------------------------------------------------------------------

    private class Parca {
        var x = 0f; var y = 0f
        var vx = 0f; var vy = 0f
        var boyut = 0f
        var aci = 0f; var donme = 0f
        var renk = 0
        var sekil = 0        // 0 dikdörtgen · 1 daire · 2 yıldız
        var omur = 1f
    }

    private val parcalar = mutableListOf<Parca>()
    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dikdortgen = RectF()
    private val yol = android.graphics.Path()

    private var baslangic = 0L
    private var tur = TUR_KONFETI
    private var calisiyor = false

    /** Canlı, birbirinden ayırt edilebilir palet. */
    private val palet = intArrayOf(
        0xFFE05C4F.toInt(), 0xFFE8A33A.toInt(), 0xFF5DAE5D.toInt(),
        0xFF3FA0C4.toInt(), 0xFF8B6BD0.toInt(), 0xFFE07BA8.toInt(),
        0xFFEFD358.toInt()
    )

    // ------------------------------------------------------------------

    private fun basla(tur: Int) {
        this.tur = tur
        baslangic = System.currentTimeMillis()
        calisiyor = true
        parcalar.clear()
        // Genişlik daha bilinmiyor olabilir; ilk çizimde üretilecek
        postInvalidateOnAnimation()
    }

    private fun uret() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val yg = resources.displayMetrics.density
        val rnd = java.util.Random()

        val adet = when (tur) {
            TUR_HAVAI -> 70
            TUR_YILDIZ -> 55
            else -> 90
        }

        repeat(adet) {
            val p = Parca()
            p.renk = palet[rnd.nextInt(palet.size)]
            p.omur = 1f
            when (tur) {
                TUR_HAVAI -> {
                    // Alttan yukarı fırlayan kıvılcımlar
                    p.x = w * (0.25f + rnd.nextFloat() * 0.5f)
                    p.y = h
                    p.vx = (rnd.nextFloat() - 0.5f) * 9f * yg
                    p.vy = -(14f + rnd.nextFloat() * 12f) * yg
                    p.boyut = (2.5f + rnd.nextFloat() * 3f) * yg
                    p.sekil = 1
                }
                TUR_YILDIZ -> {
                    // Tepeden süzülen yıldızlar
                    p.x = rnd.nextFloat() * w
                    p.y = -rnd.nextFloat() * h * 0.4f
                    p.vx = (rnd.nextFloat() - 0.5f) * 2.5f * yg
                    p.vy = (3f + rnd.nextFloat() * 4f) * yg
                    p.boyut = (5f + rnd.nextFloat() * 5f) * yg
                    p.sekil = 2
                    p.donme = (rnd.nextFloat() - 0.5f) * 6f
                }
                else -> {
                    // Üstten dökülen konfeti
                    p.x = rnd.nextFloat() * w
                    p.y = -rnd.nextFloat() * h * 0.35f
                    p.vx = (rnd.nextFloat() - 0.5f) * 5f * yg
                    p.vy = (5f + rnd.nextFloat() * 7f) * yg
                    p.boyut = (4f + rnd.nextFloat() * 5f) * yg
                    p.sekil = if (rnd.nextInt(4) == 0) 1 else 0
                    p.donme = (rnd.nextFloat() - 0.5f) * 14f
                }
            }
            p.aci = rnd.nextFloat() * 360f
            parcalar.add(p)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!calisiyor) return
        if (parcalar.isEmpty()) uret()

        val gecen = System.currentTimeMillis() - baslangic
        if (gecen > SURE_MS) {
            bitir()
            return
        }
        val yg = resources.displayMetrics.density
        // Son %30'da sol
        val genelAlfa = if (gecen > SURE_MS * 0.7f) {
            1f - ((gecen - SURE_MS * 0.7f) / (SURE_MS * 0.3f))
        } else 1f

        val yercekimi = when (tur) {
            TUR_HAVAI -> 0.62f * yg
            TUR_YILDIZ -> 0.10f * yg
            else -> 0.34f * yg
        }

        parcalar.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            p.vy += yercekimi
            p.vx *= 0.994f          // hava sürtünmesi
            p.aci += p.donme

            val alfa = (255 * genelAlfa).toInt().coerceIn(0, 255)
            boya.color = Color.argb(
                alfa, Color.red(p.renk), Color.green(p.renk), Color.blue(p.renk)
            )

            canvas.save()
            canvas.rotate(p.aci, p.x, p.y)
            when (p.sekil) {
                1 -> canvas.drawCircle(p.x, p.y, p.boyut * 0.5f, boya)
                2 -> yildizCiz(canvas, p.x, p.y, p.boyut)
                else -> {
                    dikdortgen.set(
                        p.x - p.boyut * 0.5f, p.y - p.boyut * 0.32f,
                        p.x + p.boyut * 0.5f, p.y + p.boyut * 0.32f
                    )
                    canvas.drawRoundRect(dikdortgen, yg, yg, boya)
                }
            }
            canvas.restore()
        }

        postInvalidateOnAnimation()
    }

    private fun yildizCiz(c: Canvas, cx: Float, cy: Float, boyut: Float) {
        yol.reset()
        val r1 = boyut * 0.5f
        val r2 = boyut * 0.21f
        for (i in 0 until 10) {
            val a = Math.PI / 5 * i - Math.PI / 2
            val r = if (i % 2 == 0) r1 else r2
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) yol.moveTo(x, y) else yol.lineTo(x, y)
        }
        yol.close()
        c.drawPath(yol, boya)
    }

    /** Bitince kendini kaldır — arkada boşuna çizen katman kalmasın. */
    private fun bitir() {
        calisiyor = false
        parcalar.clear()
        runCatching { (parent as? ViewGroup)?.removeView(this) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        calisiyor = false
    }
}
