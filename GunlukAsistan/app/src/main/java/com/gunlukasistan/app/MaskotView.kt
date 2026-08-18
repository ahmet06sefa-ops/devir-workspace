package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * v10.8 · Öneri D43 — Uygulama maskotu Pofi.
 *
 * ── Ne eksikti ──
 * Ana ekran her şeyi sayıyla anlatıyordu: seri 12, odak 85 dk,
 * hedef %60. Doğrudur ama soğuktur — "seri yanıyor"u hissettiren
 * hiçbir görsel yoktu. Pofi, hero kartın üst satırında yaşayan,
 * duruma göre yüz değiştiren küçük bir dosttur ([Maskot.ruhHali]).
 *
 * ── Neden asset yerine çizim ──
 * Altı ifade × iki tema renk ailesi = 12+ görsel dosyası üretmek
 * yerine `onDraw` içinde Paint ile çizilir. Böylece maskot seçili
 * temanın rengini otomatik giyer (body = colorPrimary) ve ağırlık
 * sıfırdır. [SayacKadraniView]'dan beri projede kanıtlı desen.
 *
 * ── Animasyon bütçesi ──
 * Göz kırpma seyrek ve kısadır (~3 sn'de bir 170 ms); animasyon
 * ayarı kapalıysa döngü hiç başlamaz (`GorunumAyar.animasyonAcik`).
 * Ekran dışındayken `onDetachedFromWindow` döngüyü kesin öldürür —
 * pil dostu olmak maskotun da görevi.
 */
class MaskotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** Mevcut ifade — [guncelle] ile değişir, kısa bir sıçrama yapar. */
    var ruh: Maskot.Ruh = Maskot.Ruh.NESHALI
        private set

    private var govdeRenk = 0xFF7C6BF5.toInt()
    private var cizgiRenk = Color.WHITE
    private var zeminRenk = 0x337C6BF5
    private var yanakRenk = 0x33FF7A9E

    // Sıçrama ölçeği (ifade değişimi) ve göz kırpma katsayısı
    private var sicrama = 1f
    private var kirpma = 1f

    // v10.11 · ULTRA-30 A5: giyilen aksesuarlar (MaskotGardrop anahtarları)
    private var aksesuarlar: Set<String> = emptySet()

    private val zeminBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val govdeBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cizgiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dolguBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val yanakBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val alevDis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFF7A3C.toInt()
    }
    private val alevIc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFC94D.toInt()
    }
    private val zBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val gecici = RectF()
    private val alevYol = Path()

    // ---- dışarıdan bağlantı ----

    /** Tema renklerini uygular — body vurguyu, çizgiler koyuyu alır. */
    fun renkleriAyarla(govde: Int, cizgi: Int, zemin: Int) {
        govdeRenk = govde
        cizgiRenk = cizgi
        zeminRenk = zemin
        invalidate()
    }

    /** v10.11 · A5: gardırop seçimini giyer (bilinmeyen anahtarlar düşülür). */
    fun setAksesuarlar(yeni: Set<String>) {
        val temiz = yeni.filter { it in MaskotGardrop.ANAHTARLAR }.toSet()
        if (temiz == aksesuarlar) return
        aksesuarlar = temiz
        invalidate()
    }

    /**
     * Yeni ruh hali. Değişim yoksa dokunmaz; değişimde küçük bir
     * sıçrama — kullanıcı "gözüyle" değişimi yakalar.
     */
    fun guncelle(yeni: Maskot.Ruh) {
        if (yeni == ruh) return
        ruh = yeni
        invalidate()
        sicrat()
    }

    // ---- animasyon ----

    private fun sicrat() {
        if (!GorunumAyar.animasyonAcik(context)) return
        runCatching {
            android.animation.ValueAnimator.ofFloat(0.80f, 1.12f, 1f).apply {
                duration = 420
                addUpdateListener {
                    sicrama = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    /** Göz kırpma: göz yüksekliğini 170 ms boyunca küçültüp geri açar. */
    private fun kirp() {
        if (!GorunumAyar.animasyonAcik(context)) return
        if (!isAttachedToWindow) return
        runCatching {
            android.animation.ValueAnimator.ofFloat(1f, 0.06f, 1f).apply {
                duration = 170
                addUpdateListener {
                    kirpma = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        // Bir sonraki kırpma 2,6–4,0 sn sonra (ritim organik olsun)
        val ara = 2600L + (System.currentTimeMillis() % 1400L)
        postDelayed({ kirp() }, ara)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        kirp()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler?.removeCallbacksAndMessages(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val kenar = when {
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED -> w
            else -> min(w, h)
        }
        setMeasuredDimension(kenar, kenar)
    }

    // ---- çizim ----

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cizgiBoya.strokeWidth = w / 15f
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val dis = min(width, height) / 2f * 0.97f
        if (dis <= 0f) return

        // ── 1. Zemin dairesi ──
        zeminBoya.color = zeminRenk
        canvas.drawCircle(cx, cy, dis, zeminBoya)

        // ── 2. Gövde (sıçrama ölçeğiyle) ──
        canvas.save()
        canvas.scale(sicrama, sicrama, cx, cy)
        val govdeYaricap = dis * 0.86f
        govdeBoya.color = govdeRenk
        canvas.drawCircle(cx, cy, govdeYaricap, govdeBoya)

        // ── 3. Yüz elemanları ──
        val gozY = cy - govdeYaricap * 0.16f
        val gozX = govdeYaricap * 0.31f
        val gozR = govdeYaricap * 0.085f
        cizgiBoya.color = cizgiRenk
        dolguBoya.color = cizgiRenk

        when (ruh) {
            Maskot.Ruh.ODAKLI -> {
                // Kapalı hedef gözleri: iki kısa kalın çizgi
                val uzunluk = gozR * 2.1f * kirpma
                canvas.drawLine(cx - gozX - uzunluk / 2, gozY, cx - gozX + uzunluk / 2, gozY, cizgiBoya)
                canvas.drawLine(cx + gozX - uzunluk / 2, gozY, cx + gozX + uzunluk / 2, gozY, cizgiBoya)
            }
            Maskot.Ruh.MOLADA, Maskot.Ruh.UYKULU -> {
                // Kapalı göz kemerleri (aşağı bakan ‿)
                gozArc(canvas, cx - gozX, gozY, gozR)
                gozArc(canvas, cx + gozX, gozY, gozR)
            }
            else -> {
                // Açık gözler (NESHALI / GURURLU / ALEV)
                dolguBoya.color = cizgiRenk
                val r = (gozR * kirpma).coerceAtLeast(gozR * 0.1f)
                canvas.drawCircle(cx - gozX, gozY, r, dolguBoya)
                canvas.drawCircle(cx + gozX, gozY, r, dolguBoya)
                if (ruh == Maskot.Ruh.ALEV && kirpma > 0.6f) {
                    // Alevde göz bebekleri parlar
                    dolguBoya.color = 0xE6FFFFFF.toInt()
                    canvas.drawCircle(
                        cx - gozX + gozR * 0.3f, gozY - gozR * 0.3f, gozR * 0.28f, dolguBoya
                    )
                    canvas.drawCircle(
                        cx + gozX + gozR * 0.3f, gozY - gozR * 0.3f, gozR * 0.28f, dolguBoya
                    )
                }
            }
        }

        // Ağız
        val agizY = cy + govdeYaricap * 0.22f
        when (ruh) {
            Maskot.Ruh.ODAKLI -> {
                // Kararlı düz çizgi
                canvas.drawLine(
                    cx - govdeYaricap * 0.14f, agizY + govdeYaricap * 0.06f,
                    cx + govdeYaricap * 0.14f, agizY + govdeYaricap * 0.06f,
                    cizgiBoya
                )
            }
            Maskot.Ruh.MOLADA -> {
                // Gevşemiş küçük "o"
                cizgiBoya.style = Paint.Style.STROKE
                gecici.set(
                    cx - gozR * 0.9f, agizY,
                    cx + gozR * 0.9f, agizY + gozR * 1.2f
                )
                canvas.drawOval(gecici, cizgiBoya)
            }
            Maskot.Ruh.GURURLU, Maskot.Ruh.ALEV -> {
                // Açık kocaman gülümseme (içi dolu yarım ay)
                gecici.set(
                    cx - govdeYaricap * 0.24f, agizY - govdeYaricap * 0.10f,
                    cx + govdeYaricap * 0.24f, agizY + govdeYaricap * 0.26f
                )
                val onceki = dolguBoya.color
                dolguBoya.color = cizgiRenk
                canvas.drawArc(gecici, 0f, 180f, true, dolguBoya)
                dolguBoya.color = onceki
            }
            else -> {
                // Klasik gülüş (NESHALI ve UYKULU'da hafif)
                gecici.set(
                    cx - govdeYaricap * 0.20f, agizY - govdeYaricap * 0.02f,
                    cx + govdeYaricap * 0.20f, agizY + govdeYaricap * 0.18f
                )
                canvas.drawArc(gecici, 25f, 130f, false, cizgiBoya)
            }
        }

        // Yanak kızarması (neşeli ve gururlu)
        if (ruh == Maskot.Ruh.NESHALI || ruh == Maskot.Ruh.GURURLU) {
            yanakBoya.color = yanakRenk
            val yanakY = cy + govdeYaricap * 0.12f
            canvas.drawCircle(cx - govdeYaricap * 0.46f, yanakY, govdeYaricap * 0.09f, yanakBoya)
            canvas.drawCircle(cx + govdeYaricap * 0.46f, yanakY, govdeYaricap * 0.09f, yanakBoya)
        }

        // Alev (başın tepesinde)
        if (ruh == Maskot.Ruh.ALEV) {
            alevCiz(canvas, cx, cy - govdeYaricap * 0.82f, govdeYaricap * 0.34f)
        }

        // Zz (uykulu)
        if (ruh == Maskot.Ruh.UYKULU) {
            zBoya.color = cizgiRenk
            zBoya.textSize = govdeYaricap * 0.30f
            canvas.drawText("Z", cx + govdeYaricap * 0.55f, cy - govdeYaricap * 0.45f, zBoya)
            zBoya.textSize = govdeYaricap * 0.20f
            zBoya.alpha = 170
            canvas.drawText("z", cx + govdeYaricap * 0.80f, cy - govdeYaricap * 0.70f, zBoya)
            zBoya.alpha = 255
        }

        // v10.11 · A5: aksesuarlar en üstte (alev ve Zz'yi de örter —
        // taç aleve binince doğal görünür)
        aksesuarlariCiz(canvas, cx, cy, govdeYaricap)

        canvas.restore()
    }

    /** Kapalı göz kemeri: ‿ yönünde ince yay. */
    private fun gozArc(canvas: Canvas, x: Float, y: Float, r: Float) {
        gecici.set(x - r * 1.6f, y - r * 0.2f, x + r * 1.6f, y + r * 1.4f)
        canvas.drawArc(gecici, 15f, 150f, false, cizgiBoya)
    }

    /** Başın üstündeki alev: dış turuncu + iç sarı gözyaşı damlası. */
    private fun alevCiz(canvas: Canvas, cx: Float, tabanY: Float, boy: Float) {
        alevYol.reset()
        // Dış alev
        alevYol.moveTo(cx, tabanY - boy * 1.5f)
        alevYol.quadTo(cx + boy * 0.9f, tabanY - boy * 0.7f, cx + boy * 0.55f, tabanY)
        alevYol.quadTo(cx + boy * 0.25f, tabanY + boy * 0.22f, cx, tabanY + boy * 0.22f)
        alevYol.quadTo(cx - boy * 0.25f, tabanY + boy * 0.22f, cx - boy * 0.55f, tabanY)
        alevYol.quadTo(cx - boy * 0.9f, tabanY - boy * 0.7f, cx, tabanY - boy * 1.5f)
        alevYol.close()
        canvas.drawPath(alevYol, alevDis)
        // İç alev (küçük ve aşağıda)
        alevYol.reset()
        alevYol.moveTo(cx, tabanY - boy * 0.85f)
        alevYol.quadTo(cx + boy * 0.45f, tabanY - boy * 0.3f, cx + boy * 0.3f, tabanY + boy * 0.05f)
        alevYol.quadTo(cx, tabanY + boy * 0.20f, cx - boy * 0.3f, tabanY + boy * 0.05f)
        alevYol.quadTo(cx - boy * 0.45f, tabanY - boy * 0.3f, cx, tabanY - boy * 0.85f)
        alevYol.close()
        canvas.drawPath(alevYol, alevIc)
    }

    // ══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A5 — AKSESUARLAR
    // ══════════════════════════════════════════════════════════════

    private fun aksesuarlariCiz(canvas: Canvas, cx: Float, cy: Float, govde: Float) {
        if (aksesuarlar.isEmpty()) return
        aksesuarlar.forEach { anahtar ->
            when (anahtar) {
                MaskotGardrop.BERE -> bereCiz(canvas, cx, cy, govde)
                MaskotGardrop.GOZLUK -> gozlukCiz(canvas, cx, cy, govde)
                MaskotGardrop.ESARP -> esarpCiz(canvas, cx, cy, govde)
                MaskotGardrop.TAC -> tacCiz(canvas, cx, cy, govde)
            }
        }
    }

    /** 🧢 Seri beresi: başın tepesinde kavisli kap + ponpon. */
    private fun bereCiz(canvas: Canvas, cx: Float, cy: Float, govde: Float) {
        val tepeY = cy - govde * 0.62f
        // Kap yarım elips
        dolguBoya.color = 0xFFC94F4F.toInt()
        gecici.set(
            cx - govde * 0.52f, tepeY - govde * 0.34f,
            cx + govde * 0.52f, tepeY + govde * 0.16f
        )
        canvas.drawArc(gecici, 180f, 180f, true, dolguBoya)
        // Kenar bandı
        dolguBoya.color = 0xFF9E3434.toInt()
        gecici.set(
            cx - govde * 0.52f, tepeY - govde * 0.02f,
            cx + govde * 0.52f, tepeY + govde * 0.14f
        )
        canvas.drawRoundRect(gecici, govde * 0.07f, govde * 0.07f, dolguBoya)
        // Ponpon
        dolguBoya.color = 0xFFFFE8E8.toInt()
        canvas.drawCircle(cx, tepeY - govde * 0.36f, govde * 0.12f, dolguBoya)
    }

    /** 🕶️ Usta gözlüğü: iki yuvarlak çerçeve + köprü. */
    private fun gozlukCiz(canvas: Canvas, cx: Float, cy: Float, govde: Float) {
        val gozY = cy - govde * 0.16f
        val gozX = govde * 0.31f
        val cerceveR = govde * 0.19f
        val oncekiGenislik = cizgiBoya.strokeWidth
        cizgiBoya.strokeWidth = govde * 0.045f
        canvas.drawCircle(cx - gozX, gozY, cerceveR, cizgiBoya)
        canvas.drawCircle(cx + gozX, gozY, cerceveR, cizgiBoya)
        canvas.drawLine(cx - gozX + cerceveR, gozY, cx + gozX - cerceveR, gozY, cizgiBoya)
        // Saplar
        canvas.drawLine(
            cx - gozX - cerceveR, gozY, cx - govde * 0.80f, gozY - govde * 0.10f, cizgiBoya
        )
        canvas.drawLine(
            cx + gozX + cerceveR, gozY, cx + govde * 0.80f, gozY - govde * 0.10f, cizgiBoya
        )
        cizgiBoya.strokeWidth = oncekiGenislik
    }

    /** 🧣 Gece kuşu eşarbı: alt gövdede kalın bant + sarkan uç. */
    private fun esarpCiz(canvas: Canvas, cx: Float, cy: Float, govde: Float) {
        dolguBoya.color = 0xFFE69F00.toInt()
        gecici.set(
            cx - govde * 0.60f, cy + govde * 0.30f,
            cx + govde * 0.60f, cy + govde * 0.52f
        )
        canvas.drawArc(gecici, 10f, 160f, false, dolguBoya)
        // Sarkan uç (sağdan aşağı)
        dolguBoya.color = 0xFFC77E00.toInt()
        gecici.set(
            cx + govde * 0.34f, cy + govde * 0.44f,
            cx + govde * 0.52f, cy + govde * 0.78f
        )
        canvas.drawRoundRect(gecici, govde * 0.06f, govde * 0.06f, dolguBoya)
    }

    /** 👑 Efsane tacı: üç uçlu zikzak + nokta taşlar. */
    private fun tacCiz(canvas: Canvas, cx: Float, cy: Float, govde: Float) {
        val tabanY = cy - govde * 0.58f
        val yuk = govde * 0.30f
        val gen = govde * 0.62f
        alevYol.reset() // genel yol taşıyıcısını taç için ödünç al
        alevYol.moveTo(cx - gen / 2, tabanY)
        alevYol.lineTo(cx - gen / 2, tabanY - yuk * 0.55f)
        alevYol.lineTo(cx - gen / 4, tabanY - yuk * 0.15f)
        alevYol.lineTo(cx, tabanY - yuk)
        alevYol.lineTo(cx + gen / 4, tabanY - yuk * 0.15f)
        alevYol.lineTo(cx + gen / 2, tabanY - yuk * 0.55f)
        alevYol.lineTo(cx + gen / 2, tabanY)
        alevYol.close()
        dolguBoya.color = 0xFFF0C53D.toInt()
        canvas.drawPath(alevYol, dolguBoya)
        // Tepe taşları
        dolguBoya.color = 0xFFFFF3C4.toInt()
        canvas.drawCircle(cx, tabanY - yuk * 0.92f, govde * 0.045f, dolguBoya)
    }
}
