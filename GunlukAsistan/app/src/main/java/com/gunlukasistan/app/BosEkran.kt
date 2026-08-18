package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/**
 * v8.3 — Boş ekran durumları (öneri 11).
 *
 * ── Ölçülen sorun ──
 * v8.2'de boş bir listeye bakınca ortada tek satır gri yazı vardı:
 * "Henüz görev yok". Uygulama bozuk mu, yükleniyor mu, gerçekten boş
 * mu belli değildi. `res/drawable/` içinde tek bir illüstrasyon yoktu.
 *
 * ── Neden PNG değil, kodla çizim ──
 * Her boş durum için PNG koysak 8-10 dosya × 4 yoğunluk = 40 dosya ve
 * ~1,5 MB APK artışı olurdu. Üstelik tema rengine uyum sağlamazlardı;
 * krem temada güzel duran bir çizim Zincir (neon koyu) temasında
 * yamalı görünürdü. Burada çizim `colorPrimary`'den türetiliyor,
 * 9 temanın hepsinde ve gece modunda doğru görünüyor.
 *
 * ── Neden eylem düğmesi ──
 * "Henüz görev yok" bilgi veriyor ama yol göstermiyor. "İlk görevini
 * ekle" düğmesi boş ekranı ölü bir uçtan başlangıç noktasına çeviriyor.
 */
object BosEkran {

    private const val TAG = "BosEkran"

    /** Hangi çizim gösterilecek. */
    enum class Tur { GOREV, NOT, KONU, SINAV, ETKINLIK, ALISKANLIK, ARAMA, HATA, GENEL }

    /**
     * Var olan bir "boş" TextView'ı zengin boş duruma çevirir.
     *
     * Mevcut layout'larda `emptyText` adında bir TextView var; onu
     * gizleyip yerine bu düzeni koyuyoruz. Böylece 15 layout dosyasını
     * değiştirmeye gerek kalmıyor.
     *
     * @param eskiGorunum yerine geçilecek TextView (görünürlüğü yönetilir)
     * @param eylem düğmeye basılınca çalışacak iş (null ise düğme çıkmaz)
     */
    fun kur(
        eskiGorunum: TextView?,
        tur: Tur,
        baslik: String,
        aciklama: String,
        eylemEtiketi: String? = null,
        eylem: (() -> Unit)? = null
    ): View? {
        eskiGorunum ?: return null
        return runCatching {
            val ebeveyn = eskiGorunum.parent as? ViewGroup ?: return null

            // Zaten kurulmuşsa yenisini oluşturma, içeriği tazele
            val mevcut = ebeveyn.findViewWithTag<View>("ga_bos_$tur")
            if (mevcut != null) return mevcut

            val ctx = eskiGorunum.context
            val yg = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * yg).toInt()

            val duzen = LinearLayout(ctx).apply {
                tag = "ga_bos_$tur"
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(32), dp(24), dp(32), dp(24))
                layoutParams = eskiGorunum.layoutParams
                visibility = View.GONE
            }

            duzen.addView(BosCizim(ctx).apply {
                this.tur = tur
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
            })

            duzen.addView(TextView(ctx).apply {
                text = baslik
                textSize = 16.5f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(renk(this, com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, dp(16), 0, 0)
            })

            duzen.addView(TextView(ctx).apply {
                text = aciklama
                textSize = 13f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.35f)
                setTextColor(renk(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(6), 0, 0)
            })

            if (eylemEtiketi != null && eylem != null) {
                duzen.addView(MaterialButton(ctx).apply {
                    text = eylemEtiketi
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(18) }
                    setOnClickListener {
                        Titresim.dokunus(it)
                        eylem()
                    }
                })
            }

            val sira = ebeveyn.indexOfChild(eskiGorunum)
            ebeveyn.addView(duzen, sira + 1)
            // Eski yazı artık hiç görünmesin
            eskiGorunum.visibility = View.GONE
            duzen
        }.onFailure { android.util.Log.w(TAG, "kur", it) }.getOrNull()
    }

    /** Boş durumu göster/gizle. Giriş animasyonu ile. */
    fun goster(gorunum: View?, gorunsun: Boolean) {
        gorunum ?: return
        runCatching {
            if (gorunsun) {
                if (gorunum.visibility != View.VISIBLE) Canlandir.bel(gorunum)
            } else {
                gorunum.visibility = View.GONE
            }
        }.onFailure { android.util.Log.w(TAG, "goster", it) }
    }

    private fun renk(v: View, attr: Int): Int = runCatching {
        MaterialColors.getColor(v, attr, 0xFF888888.toInt())
    }.getOrDefault(0xFF888888.toInt())

    // ==================================================================

    /**
     * Boş durum çizimi.
     *
     * Her tür için basit, tanınabilir bir çizgi resim. Renkler temadan
     * geliyor: ana hat `colorPrimary`, dolgu `colorPrimaryContainer`.
     */
    class BosCizim @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : View(context, attrs) {

        var tur: Tur = Tur.GENEL
            set(v) { field = v; invalidate() }

        private val hat = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val dolgu = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val yol = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val b = minOf(w, h)
            if (b <= 0f) return

            val anaRenk = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0xFFB08968.toInt()
            )
            val yumusak = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFEFE2D0.toInt()
            )

            hat.color = anaRenk
            hat.strokeWidth = b * 0.045f
            dolgu.color = yumusak

            // Arkada yumuşak bir daire — çizimi zemine oturtuyor
            canvas.drawCircle(w / 2f, h / 2f, b * 0.44f, dolgu)

            val cx = w / 2f
            val cy = h / 2f
            val s = b * 0.26f  // yarı boyut

            when (tur) {
                Tur.GOREV -> gorevCiz(canvas, cx, cy, s)
                Tur.NOT -> notCiz(canvas, cx, cy, s)
                Tur.KONU -> konuCiz(canvas, cx, cy, s)
                Tur.SINAV -> sinavCiz(canvas, cx, cy, s)
                Tur.ETKINLIK -> etkinlikCiz(canvas, cx, cy, s)
                Tur.ALISKANLIK -> aliskanlikCiz(canvas, cx, cy, s)
                Tur.ARAMA -> aramaCiz(canvas, cx, cy, s)
                Tur.HATA -> hataCiz(canvas, cx, cy, s)
                Tur.GENEL -> genelCiz(canvas, cx, cy, s)
            }
        }

        /** Yapılacaklar listesi: üç satır, ilki işaretli. */
        private fun gorevCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            val satirY = floatArrayOf(cy - s * 0.72f, cy, cy + s * 0.72f)
            satirY.forEachIndexed { i, y ->
                // kutucuk
                val kx = cx - s * 0.95f
                c.drawRoundRect(
                    kx, y - s * 0.22f, kx + s * 0.44f, y + s * 0.22f,
                    s * 0.1f, s * 0.1f, hat
                )
                if (i == 0) {
                    // içinde tik
                    yol.reset()
                    yol.moveTo(kx + s * 0.1f, y)
                    yol.lineTo(kx + s * 0.2f, y + s * 0.11f)
                    yol.lineTo(kx + s * 0.34f, y - s * 0.12f)
                    c.drawPath(yol, hat)
                }
                // yazı çizgisi
                val uzunluk = if (i == 0) s * 1.0f else if (i == 1) s * 1.3f else s * 0.8f
                c.drawLine(kx + s * 0.7f, y, kx + s * 0.7f + uzunluk, y, hat)
            }
        }

        /** Not: köşesi kıvrık kâğıt + kalem. */
        private fun notCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            yol.reset()
            yol.moveTo(cx - s * 0.8f, cy - s)
            yol.lineTo(cx + s * 0.4f, cy - s)
            yol.lineTo(cx + s * 0.9f, cy - s * 0.5f)
            yol.lineTo(cx + s * 0.9f, cy + s)
            yol.lineTo(cx - s * 0.8f, cy + s)
            yol.close()
            c.drawPath(yol, hat)
            // kıvrık köşe
            yol.reset()
            yol.moveTo(cx + s * 0.4f, cy - s)
            yol.lineTo(cx + s * 0.4f, cy - s * 0.5f)
            yol.lineTo(cx + s * 0.9f, cy - s * 0.5f)
            c.drawPath(yol, hat)
            // satırlar
            c.drawLine(cx - s * 0.5f, cy - s * 0.05f, cx + s * 0.55f, cy - s * 0.05f, hat)
            c.drawLine(cx - s * 0.5f, cy + s * 0.4f, cx + s * 0.2f, cy + s * 0.4f, hat)
        }

        /** Konu: açık kitap. */
        private fun konuCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            yol.reset()
            yol.moveTo(cx, cy - s * 0.55f)
            yol.cubicTo(
                cx - s * 0.4f, cy - s * 0.85f,
                cx - s * 0.75f, cy - s * 0.8f,
                cx - s, cy - s * 0.6f
            )
            yol.lineTo(cx - s, cy + s * 0.7f)
            yol.cubicTo(
                cx - s * 0.75f, cy + s * 0.5f,
                cx - s * 0.4f, cy + s * 0.55f,
                cx, cy + s * 0.85f
            )
            c.drawPath(yol, hat)
            yol.reset()
            yol.moveTo(cx, cy - s * 0.55f)
            yol.cubicTo(
                cx + s * 0.4f, cy - s * 0.85f,
                cx + s * 0.75f, cy - s * 0.8f,
                cx + s, cy - s * 0.6f
            )
            yol.lineTo(cx + s, cy + s * 0.7f)
            yol.cubicTo(
                cx + s * 0.75f, cy + s * 0.5f,
                cx + s * 0.4f, cy + s * 0.55f,
                cx, cy + s * 0.85f
            )
            c.drawPath(yol, hat)
            c.drawLine(cx, cy - s * 0.55f, cx, cy + s * 0.85f, hat)
        }

        /** Sınav: takvim + saat. */
        private fun sinavCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            c.drawRoundRect(
                cx - s * 0.95f, cy - s * 0.7f, cx + s * 0.6f, cy + s * 0.9f,
                s * 0.16f, s * 0.16f, hat
            )
            c.drawLine(cx - s * 0.95f, cy - s * 0.25f, cx + s * 0.6f, cy - s * 0.25f, hat)
            c.drawLine(cx - s * 0.55f, cy - s * 0.95f, cx - s * 0.55f, cy - s * 0.5f, hat)
            c.drawLine(cx + s * 0.2f, cy - s * 0.95f, cx + s * 0.2f, cy - s * 0.5f, hat)
            // saat
            c.drawCircle(cx + s * 0.55f, cy + s * 0.5f, s * 0.45f, dolgu)
            c.drawCircle(cx + s * 0.55f, cy + s * 0.5f, s * 0.45f, hat)
            c.drawLine(cx + s * 0.55f, cy + s * 0.5f, cx + s * 0.55f, cy + s * 0.22f, hat)
            c.drawLine(cx + s * 0.55f, cy + s * 0.5f, cx + s * 0.78f, cy + s * 0.5f, hat)
        }

        /** Etkinlik: konum iğnesi. */
        private fun etkinlikCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            yol.reset()
            yol.moveTo(cx, cy + s * 0.95f)
            yol.cubicTo(
                cx - s * 0.85f, cy - s * 0.1f,
                cx - s * 0.7f, cy - s * 0.95f,
                cx, cy - s * 0.95f
            )
            yol.cubicTo(
                cx + s * 0.7f, cy - s * 0.95f,
                cx + s * 0.85f, cy - s * 0.1f,
                cx, cy + s * 0.95f
            )
            c.drawPath(yol, hat)
            c.drawCircle(cx, cy - s * 0.35f, s * 0.3f, hat)
        }

        /** Alışkanlık: takvim ızgarası, bazıları dolu. */
        private fun aliskanlikCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            val a = s * 0.42f       // hücre kenarı
            val bosluk = s * 0.12f
            val dolular = setOf(0, 1, 4, 5, 6, 9, 10)
            var i = 0
            for (satir in 0 until 3) {
                for (sutun in 0 until 4) {
                    val x = cx - s * 0.95f + sutun * (a + bosluk)
                    val y = cy - s * 0.7f + satir * (a + bosluk)
                    if (i in dolular) {
                        val g = Paint(hat).apply { style = Paint.Style.FILL }
                        c.drawRoundRect(x, y, x + a, y + a, a * 0.28f, a * 0.28f, g)
                    } else {
                        c.drawRoundRect(x, y, x + a, y + a, a * 0.28f, a * 0.28f, hat)
                    }
                    i++
                }
            }
        }

        /** Arama: büyüteç. */
        private fun aramaCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            c.drawCircle(cx - s * 0.18f, cy - s * 0.2f, s * 0.62f, hat)
            c.drawLine(
                cx + s * 0.28f, cy + s * 0.28f,
                cx + s * 0.85f, cy + s * 0.85f, hat
            )
        }

        /** Hata defteri: hedef tahtası. */
        private fun hataCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            c.drawCircle(cx, cy, s * 0.92f, hat)
            c.drawCircle(cx, cy, s * 0.58f, hat)
            val g = Paint(hat).apply { style = Paint.Style.FILL }
            c.drawCircle(cx, cy, s * 0.2f, g)
        }

        /** Genel: kutu + yıldız. */
        private fun genelCiz(c: Canvas, cx: Float, cy: Float, s: Float) {
            c.drawRoundRect(
                cx - s * 0.9f, cy - s * 0.5f, cx + s * 0.9f, cy + s * 0.85f,
                s * 0.16f, s * 0.16f, hat
            )
            c.drawLine(cx - s * 0.9f, cy - s * 0.05f, cx + s * 0.9f, cy - s * 0.05f, hat)
            // yıldız
            yol.reset()
            val r1 = s * 0.36f
            val r2 = s * 0.15f
            for (k in 0 until 10) {
                val aci = Math.PI / 5 * k - Math.PI / 2
                val r = if (k % 2 == 0) r1 else r2
                val x = cx + (r * Math.cos(aci)).toFloat()
                val y = cy - s * 0.95f + (r * Math.sin(aci)).toFloat()
                if (k == 0) yol.moveTo(x, y) else yol.lineTo(x, y)
            }
            yol.close()
            c.drawPath(yol, hat)
        }
    }
}
