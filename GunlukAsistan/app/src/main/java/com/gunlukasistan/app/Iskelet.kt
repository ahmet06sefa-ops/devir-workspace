package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.color.MaterialColors

/**
 * v8.6 — Yükleniyor iskeletleri (öneri 25).
 *
 * ── Sorun ──
 * AI cevabı beklerken, PDF açılırken veya konu üretilirken ekran ya
 * boş kalıyor ya da donmuş görünüyordu. Bazı yerlerde dönen bir çark
 * vardı ama çark "bir şey oluyor" der, "ne oluyor" demez.
 *
 * ── Neden shimmer (parlayan iskelet) ──
 * Gelecek içeriğin şeklini önceden göstermek bekleme süresini
 * kısa hissettiriyor (ölçülmüş bir kullanıcı deneyimi bulgusu):
 * kullanıcı boşluğa değil, dolmakta olan bir yapıya bakıyor.
 *
 * ── Neden Facebook Shimmer kütüphanesi eklenmedi ──
 * Tek bir efekt için ~120 KB bağımlılık. Burada `LinearGradient` +
 * kayan `translate` ile aynı görünüm ~150 satırda elde ediliyor.
 *
 * ── Erişilebilirlik ──
 * `GorunumAyar.animasyonAcik` kapalıysa parıltı durur, iskelet düz
 * gri kutular olarak görünür — yapı bilgisi yine verilir.
 */
class Iskelet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAG = "Iskelet"

        /** Liste satırları: ikon + iki metin çizgisi. */
        const val SEKIL_LISTE = 0
        /** Metin paragrafı: değişen uzunlukta çizgiler. */
        const val SEKIL_METIN = 1
        /** Kart ızgarası: 2×2 kutu. */
        const val SEKIL_KART = 2

        /**
         * Bir görünümün üzerine iskelet katmanı koyar.
         *
         * @return kaldırmak için kullanılacak katman
         */
        fun kapla(hedef: View?, sekil: Int = SEKIL_LISTE, satir: Int = 4): Iskelet? {
            hedef ?: return null
            return runCatching {
                val ebeveyn = hedef.parent as? ViewGroup ?: return null
                // Zaten kaplıysa yenisini ekleme
                (ebeveyn.findViewWithTag<View>("ga_iskelet") as? Iskelet)?.let { return it }

                val katman = Iskelet(hedef.context).apply {
                    tag = "ga_iskelet"
                    this.sekil = sekil
                    this.satirSayisi = satir
                }
                when (ebeveyn) {
                    is FrameLayout -> ebeveyn.addView(
                        katman,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    else -> {
                        // FrameLayout değilse hedefin hemen ardına ekle.
                        //
                        // 🔴 v8.7 düzeltmesi: eskiden `hedef.layoutParams`
                        // DOĞRUDAN veriliyordu — iki görünüm aynı
                        // LayoutParams nesnesini paylaşıyordu. Biri
                        // değiştirince diğeri de değişiyordu (Android'de
                        // klasik hata). Artık yeni bir nesne üretiliyor.
                        val sira = ebeveyn.indexOfChild(hedef)
                        val kaynak = hedef.layoutParams
                        val kopya = when (kaynak) {
                            is android.widget.LinearLayout.LayoutParams ->
                                android.widget.LinearLayout.LayoutParams(kaynak)
                            is ViewGroup.MarginLayoutParams ->
                                ViewGroup.MarginLayoutParams(kaynak)
                            null -> ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            else -> ViewGroup.LayoutParams(kaynak)
                        }
                        ebeveyn.addView(katman, sira + 1, kopya)
                    }
                }
                katman
            }.onFailure { android.util.Log.w(TAG, "kapla", it) }.getOrNull()
        }

        /** İskeleti kaldırır. */
        fun kaldir(katman: View?) {
            katman ?: return
            runCatching {
                (katman.parent as? ViewGroup)?.removeView(katman)
            }.onFailure { android.util.Log.w(TAG, "kaldir", it) }
        }

        /** Bir görünümün altındaki iskeleti bulup kaldırır. */
        fun kaldirTumu(kap: ViewGroup?) {
            kap ?: return
            runCatching {
                kap.findViewWithTag<View>("ga_iskelet")?.let { kap.removeView(it) }
            }
        }
    }

    var sekil: Int = SEKIL_LISTE
        set(v) { field = v; invalidate() }

    var satirSayisi: Int = 4
        set(v) { field = v.coerceIn(1, 10); invalidate() }

    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val parilti = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dikdortgen = RectF()

    private var faz = 0f
    private var golgeleyici: Shader? = null
    private var sonGenislik = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val yg = resources.displayMetrics.density

        val zeminRengi = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE6E6E6.toInt()
        )
        boya.color = Color.argb(
            120, Color.red(zeminRengi), Color.green(zeminRengi), Color.blue(zeminRengi)
        )

        when (sekil) {
            SEKIL_METIN -> metinCiz(canvas, w, yg)
            SEKIL_KART -> kartCiz(canvas, w, h, yg)
            else -> listeCiz(canvas, w, yg)
        }

        // ---- Parıltı ----
        if (!GorunumAyar.animasyonAcik(context)) return

        if (golgeleyici == null || sonGenislik != width) {
            val vurgu = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, Color.WHITE
            )
            golgeleyici = LinearGradient(
                0f, 0f, w * 0.42f, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(96, Color.red(vurgu), Color.green(vurgu), Color.blue(vurgu)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            sonGenislik = width
        }

        faz += 0.018f
        if (faz > 1.4f) faz = -0.4f
        val matris = android.graphics.Matrix().apply {
            setTranslate(w * faz, 0f)
        }
        golgeleyici?.setLocalMatrix(matris)
        parilti.shader = golgeleyici
        canvas.drawRect(0f, 0f, w, h, parilti)

        postInvalidateOnAnimation()
    }

    // ------------------------------------------------------------------

    private fun listeCiz(c: Canvas, w: Float, yg: Float) {
        val satirY = yg * 66
        val kose = yg * 8
        for (i in 0 until satirSayisi) {
            val ust = yg * 10 + i * satirY
            // Yuvarlak ikon
            c.drawCircle(yg * 30, ust + yg * 22, yg * 18, boya)
            // Başlık çizgisi
            dikdortgen.set(yg * 58, ust + yg * 8, w * 0.72f, ust + yg * 22)
            c.drawRoundRect(dikdortgen, kose, kose, boya)
            // Alt çizgi
            dikdortgen.set(yg * 58, ust + yg * 28, w * 0.48f, ust + yg * 38)
            c.drawRoundRect(dikdortgen, kose, kose, boya)
        }
    }

    private fun metinCiz(c: Canvas, w: Float, yg: Float) {
        val kose = yg * 6
        // Değişen uzunluklar — gerçek paragraf hissi
        val oranlar = floatArrayOf(0.96f, 0.88f, 0.94f, 0.62f, 0.91f, 0.79f, 0.45f)
        for (i in 0 until (satirSayisi * 2).coerceAtMost(oranlar.size * 2)) {
            val ust = yg * 12 + i * yg * 24
            val oran = oranlar[i % oranlar.size]
            dikdortgen.set(yg * 4, ust, w * oran, ust + yg * 14)
            c.drawRoundRect(dikdortgen, kose, kose, boya)
        }
    }

    private fun kartCiz(c: Canvas, w: Float, h: Float, yg: Float) {
        val kose = yg * 16
        val bosluk = yg * 10
        val kartW = (w - bosluk * 3) / 2
        val kartH = yg * 100
        for (satir in 0 until 2) {
            for (sutun in 0 until 2) {
                val x = bosluk + sutun * (kartW + bosluk)
                val y = bosluk + satir * (kartH + bosluk)
                if (y + kartH > h) return
                dikdortgen.set(x, y, x + kartW, y + kartH)
                c.drawRoundRect(dikdortgen, kose, kose, boya)
            }
        }
    }
}
