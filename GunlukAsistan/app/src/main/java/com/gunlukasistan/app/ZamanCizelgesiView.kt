package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.color.MaterialColors
import java.util.Calendar

/**
 * v8.5 — Günün zaman çizelgesi (öneri 23).
 *
 * ── Sorun ──
 * Bugün ekranı bir listeydi: görevler alt alta, saatleri metin olarak
 * yazılı. Gün bir "plan" gibi değil, bir "yapılacaklar yığını" gibi
 * duruyordu. Sabah 9'da üç işin üst üste bindiğini görmek imkânsızdı.
 *
 * ── Ne çiziyor ──
 * Dikey saat şeridi (varsayılan 07:00-24:00). Üzerinde:
 *   · Görevler — vade saatlerine yerleşmiş renkli bloklar
 *   · Namaz vakitleri — ince yatay işaretler
 *   · Sayaç oturumları — dolu bloklar (geçmiş odak)
 *   · Şu an — kırmızı yatay çizgi + nokta
 *
 * ── Neden custom View ──
 * Saat başına bir satır + üstünde konumlanan bloklar, `ConstraintLayout`
 * ile yapılabilirdi ama her tazelemede 17 satır + N blok oluşturmak
 * gerekirdi. Tek `onDraw` daha hafif ve çakışan blokları yan yana
 * yerleştirmek (sütun ayırma) burada çok daha kolay.
 *
 * ── Çakışma çözümü ──
 * Aynı saat aralığına düşen bloklar yan yana daraltılıyor (takvim
 * uygulamalarındaki gibi). Üç iş aynı saatteyse üçü de görünüyor.
 */
class ZamanCizelgesiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** Çizelgedeki bir öğe. */
    data class Oge(
        val baslik: String,
        /** Gün içindeki başlangıç dakikası (0-1439). */
        val baslangicDk: Int,
        /** Süre (dakika). En az 30 çizilir ki okunabilsin. */
        val sureDk: Int,
        val renk: Int,
        val tur: Tur,
        val tamamlandi: Boolean = false,
        val veri: Any? = null
    )

    enum class Tur { GOREV, ODAK, NAMAZ, DERS }

    private var ogeler: List<Oge> = emptyList()
    private var yerlesim: List<Yerlesim> = emptyList()

    /** Görünen saat aralığı. */
    private var baslangicSaat = 7
    private var bitisSaat = 24

    private val cizgiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val saatBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }
    private val blokBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val blokYazi = Paint(Paint.ANTI_ALIAS_FLAG)
    private val simdiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dikdortgen = RectF()

    /** Bir öğeye dokunulunca. */
    var ogeSecildi: ((Oge) -> Unit)? = null

    /** Boş bir saate dokunulunca (ders koymak için). */
    var bosSaatSecildi: ((Int) -> Unit)? = null

    // ------------------------------------------------------------------

    fun ayarla(yeni: List<Oge>) {
        ogeler = yeni.sortedBy { it.baslangicDk }
        // Görünen aralığı içeriğe göre genişlet
        val enErken = ogeler.minOfOrNull { it.baslangicDk / 60 } ?: 7
        val enGec = ogeler.maxOfOrNull { (it.baslangicDk + it.sureDk + 59) / 60 } ?: 24
        val simdiSaat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        baslangicSaat = minOf(enErken, simdiSaat, 8).coerceAtLeast(0)
        bitisSaat = maxOf(enGec, simdiSaat + 2, 21).coerceAtMost(24)
        yerlesimiHesapla()
        requestLayout()
        invalidate()
    }

    // ------------------------------------------------------------------

    data class Yerlesim(val oge: Oge, val sutun: Int, val sutunSayisi: Int)

    private fun yerlesimiHesapla() {
        yerlesim = sutunlaraAyir(ogeler)
    }

    companion object {
        /** Bir bloğun en az çizim süresi — daha kısası okunmuyor. */
        const val EN_AZ_SURE_DK = 30
        private val SAAT_ETIKETLERI = Array(25) { String.format(java.util.Locale.US, "%02d:00", it) }

        /**
         * Çakışan öğeleri sütunlara ayırır (takvim uygulaması mantığı).
         *
         * ── Algoritma ──
         * Açgözlü yerleştirme: her öge, kendisiyle çakışmayan ilk
         * sütuna konur. Aynı anda kaç öge varsa o kadar sütun oluşur
         * ve hepsi eşit genişlikte daralır.
         *
         * ── Neden companion object'te ve public ──
         * v8.7'de test edilebilmesi için sınıf üyesi olmaktan çıkarıldı.
         * Çizim kodu (onDraw) JUnit'te doğrulanamaz ama bu hesap
         * doğrulanabilir — ve yanlış olursa çakışan işler üst üste
         * binerek görünmez olur. `ZamanCizelgesiTest` bunu sınıyor.
         */
        fun sutunlaraAyir(girdi: List<Oge>): List<Yerlesim> {
            if (girdi.isEmpty()) return emptyList()
            val sirali = girdi.sortedBy { it.baslangicDk }
            val sonuc = mutableListOf<Yerlesim>()

            var grup = mutableListOf<Oge>()
            var grupBitis = Int.MIN_VALUE

            fun bitisi(o: Oge) = o.baslangicDk + maxOf(o.sureDk, EN_AZ_SURE_DK)

            fun grubuIsle() {
                if (grup.isEmpty()) return
                val sutunlar = mutableListOf<MutableList<Oge>>()
                grup.forEach { o ->
                    // Bu ögenin sığabileceği ilk sütun: son ögesi bitmiş olan
                    val uygun = sutunlar.firstOrNull { s -> bitisi(s.last()) <= o.baslangicDk }
                    if (uygun != null) uygun.add(o) else sutunlar.add(mutableListOf(o))
                }
                val n = sutunlar.size
                sutunlar.forEachIndexed { i, s -> s.forEach { sonuc.add(Yerlesim(it, i, n)) } }
                grup = mutableListOf()
            }

            sirali.forEach { o ->
                if (grup.isEmpty() || o.baslangicDk < grupBitis) {
                    grup.add(o)
                    grupBitis = maxOf(grupBitis, bitisi(o))
                } else {
                    grubuIsle()
                    grup.add(o)
                    grupBitis = bitisi(o)
                }
            }
            grubuIsle()
            return sonuc
        }
    }

    // ------------------------------------------------------------------

    private var cizgiRengiOnbellek = 0xFFDDDDDD.toInt()
    private var yaziRengiOnbellek = 0xFF888888.toInt()

    private fun saatYuksekligi(): Float = resources.displayMetrics.density * 46f
    private fun solPay(): Float = resources.displayMetrics.density * 40f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val saatSayisi = (bitisSaat - baslangicSaat).coerceAtLeast(1)
        val h = (saatSayisi * saatYuksekligi() + resources.displayMetrics.density * 12).toInt()
        setMeasuredDimension(w, h)
        cizgiRengiOnbellek = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOutlineVariant, 0xFFDDDDDD.toInt()
        )
        yaziRengiOnbellek = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888.toInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val yg = resources.displayMetrics.density
        val sh = saatYuksekligi()
        val sol = solPay()
        val sag = width - yg * 6

        cizgiBoya.color = cizgiRengiOnbellek
        saatBoya.color = yaziRengiOnbellek
        saatBoya.textSize = yg * 10f

        // ---- Saat çizgileri ----
        for (saat in baslangicSaat..bitisSaat) {
            val y = (saat - baslangicSaat) * sh + yg * 6
            canvas.drawLine(sol, y, sag, y, cizgiBoya)
            if (saat < 24) {
                canvas.drawText(
                    SAAT_ETIKETLERI.getOrElse(saat) { "00:00" },
                    sol - yg * 6, y + yg * 3.5f, saatBoya
                )
            }
        }

        // ---- Bloklar ----
        blokYazi.textSize = yg * 11f
        yerlesim.forEach { y ->
            val o = y.oge
            val ustDk = o.baslangicDk - baslangicSaat * 60
            val sureDk = maxOf(o.sureDk, 30)
            val ust = ustDk / 60f * sh + yg * 6
            val alt = (ustDk + sureDk) / 60f * sh + yg * 6
            if (alt < 0 || ust > height) return@forEach

            val genislik = (sag - sol - yg * 4) / y.sutunSayisi
            val x1 = sol + yg * 4 + y.sutun * genislik
            val x2 = x1 + genislik - yg * 3

            // Namaz: ince şerit; diğerleri: dolu blok
            if (o.tur == Tur.NAMAZ) {
                blokBoya.color = Color.argb(
                    120, Color.red(o.renk), Color.green(o.renk), Color.blue(o.renk)
                )
                dikdortgen.set(sol, ust, sag, ust + yg * 2.5f)
                canvas.drawRoundRect(dikdortgen, yg, yg, blokBoya)
                blokYazi.color = o.renk
                blokYazi.textSize = yg * 9.5f
                canvas.drawText(o.baslik, sol + yg * 6, ust - yg * 2, blokYazi)
                return@forEach
            }

            val alfa = if (o.tamamlandi) 70 else 210
            blokBoya.color = Color.argb(
                alfa, Color.red(o.renk), Color.green(o.renk), Color.blue(o.renk)
            )
            dikdortgen.set(x1, ust + yg * 1.5f, x2, alt - yg * 1.5f)
            canvas.drawRoundRect(dikdortgen, yg * 7, yg * 7, blokBoya)

            // Başlık — bloğa sığdığı kadar
            blokYazi.color = if (parlakMi(o.renk) && !o.tamamlandi) 0xFF222222.toInt() else Color.WHITE
            blokYazi.textSize = yg * 11f
            val metin = kirp(
                if (o.tamamlandi) "✓ ${o.baslik}" else o.baslik,
                x2 - x1 - yg * 14
            )
            val fm = blokYazi.fontMetrics
            canvas.drawText(metin, x1 + yg * 8, ust + yg * 1.5f - fm.ascent + yg * 4, blokYazi)
        }

        // ---- Şu an çizgisi ----
        val simdi = Calendar.getInstance()
        val simdiDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        val simdiY = (simdiDk - baslangicSaat * 60) / 60f * sh + yg * 6
        if (simdiY in 0f..height.toFloat()) {
            simdiBoya.color = 0xFFD9534F.toInt()
            canvas.drawRect(sol, simdiY - yg * 0.9f, sag, simdiY + yg * 0.9f, simdiBoya)
            canvas.drawCircle(sol, simdiY, yg * 3.5f, simdiBoya)
        }
    }

    // ------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        val yg = resources.displayMetrics.density
        val sh = saatYuksekligi()
        val sol = solPay()
        val sag = width - yg * 6

        // Önce blokları dene
        yerlesim.forEach { y ->
            val o = y.oge
            if (o.tur == Tur.NAMAZ) return@forEach
            val ustDk = o.baslangicDk - baslangicSaat * 60
            val sureDk = maxOf(o.sureDk, 30)
            val ust = ustDk / 60f * sh + yg * 6
            val alt = (ustDk + sureDk) / 60f * sh + yg * 6
            val genislik = (sag - sol - yg * 4) / y.sutunSayisi
            val x1 = sol + yg * 4 + y.sutun * genislik
            val x2 = x1 + genislik - yg * 3
            if (event.x in x1..x2 && event.y in ust..alt) {
                Titresim.tik(this)
                ogeSecildi?.invoke(o)
                return true
            }
        }

        // Boş alana dokunuldu → o saati bildir
        if (event.x > sol) {
            val saat = (baslangicSaat + ((event.y - yg * 6) / sh).toInt()).coerceIn(0, 23)
            Titresim.tik(this)
            bosSaatSecildi?.invoke(saat)
            return true
        }
        return false
    }

    // ------------------------------------------------------------------

    private fun kirp(metin: String, genislik: Float): String {
        if (genislik <= 0) return ""
        if (blokYazi.measureText(metin) <= genislik) return metin
        var kes = metin.length
        while (kes > 1 && blokYazi.measureText(metin.substring(0, kes) + "…") > genislik) kes--
        return metin.substring(0, kes) + "…"
    }

    private fun parlakMi(renk: Int): Boolean {
        val r = Color.red(renk); val g = Color.green(renk); val b = Color.blue(renk)
        return (r * 299 + g * 587 + b * 114) / 1000 > 155
    }
}
