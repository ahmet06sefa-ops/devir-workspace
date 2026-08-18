package com.gunlukasistan.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/**
 * v8.2 — Liste kartlarında kaydırma jesti (öneri 4).
 *
 * ── Ölçüm ──
 * v8.1'de `ItemTouchHelper` HİÇBİR dosyada geçmiyordu. Bir görevi
 * silmek için: karta uzun bas → menü aç → "Sil" seç → onay penceresi →
 * "Sil". Beş adım. Artık: sola kaydır. Tek adım, üstelik geri alınabilir.
 *
 * ── Görsel ──
 * Kaydırırken kartın altında renkli bir zemin ve ikon çizilir:
 *   sola  → kırmızı  🗑  "Sil"
 *   sağa  → yeşil    ✓  "Tamamla"
 * Zemin, kartın kaydığı mesafeyle orantılı olarak koyulaşır; eşik
 * geçilince ikon büyür ve haptic "tik" gelir — kullanıcı bırakmadan
 * önce ne olacağını bilir.
 *
 * ── Neden eşik geri bildirimi önemli ──
 * Eşiksiz kaydırmada kullanıcı ne kadar kaydırması gerektiğini bilemez
 * ve yarım bırakır. Eşikte titreşim + ikon büyümesi bunu çözüyor.
 *
 * ── Geri alma ──
 * Silme işlemi doğrudan [GeriAl] (v8.0) üzerinden duyuruluyor, yani
 * yanlış kaydıran 5 saniye içinde kurtarabiliyor. Onay penceresi
 * KOYMUYORUZ; onay penceresi kaydırmanın bütün hızını öldürür.
 */
object Kaydirma {

    private const val TAG = "Kaydirma"

    /** Eylemin tetiklenmesi için kartın genişliğinin kaçta kaçı kaymalı. */
    private const val ESIK = 0.38f

    /**
     * Kaydırma davranışı tanımı.
     *
     * @param solaEtiket sola kaydırınca görünecek yazı (null → sola kaydırma kapalı)
     * @param sagaEtiket sağa kaydırınca görünecek yazı (null → sağa kaydırma kapalı)
     */
    data class Ayar(
        val solaEtiket: String? = null,
        val solaIkon: String = "🗑",
        val solaRenk: Int = 0xFFD9534F.toInt(),
        val sagaEtiket: String? = null,
        val sagaIkon: String = "✓",
        val sagaRenk: Int = 0xFF4C9A5A.toInt(),
        /** Belirli bir satır kaydırılamıyorsa false döndür (başlık satırı vb.). */
        val kaydirilabilir: (Int) -> Boolean = { true }
    )

    /**
     * RecyclerView'a kaydırma jesti bağlar.
     *
     * ```
     * Kaydirma.kur(recycler, Kaydirma.Ayar(
     *     solaEtiket = "Sil", sagaEtiket = "Tamamla"
     * ), sola = { pos -> sil(pos) }, saga = { pos -> tamamla(pos) })
     * ```
     *
     * @param sola sola kaydırınca çalışacak iş (satır konumu verilir)
     * @param saga sağa kaydırınca çalışacak iş
     * @return bağlanan yardımcı (kapatmak için `attachToRecyclerView(null)`)
     */
    fun kur(
        recycler: RecyclerView?,
        ayar: Ayar,
        sola: ((Int) -> Unit)? = null,
        saga: ((Int) -> Unit)? = null
    ): ItemTouchHelper? {
        recycler ?: return null
        if (!GorunumAyar.kaydirmaJesti(recycler.context)) return null

        return runCatching {
            var yonler = 0
            if (sola != null && ayar.solaEtiket != null) yonler = yonler or ItemTouchHelper.LEFT
            if (saga != null && ayar.sagaEtiket != null) yonler = yonler or ItemTouchHelper.RIGHT
            if (yonler == 0) return null

            val ciz = Cizici(ayar)

            val geriCagri = object : ItemTouchHelper.SimpleCallback(0, yonler) {

                override fun onMove(
                    rv: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    hedef: RecyclerView.ViewHolder
                ): Boolean = false

                override fun getSwipeDirs(
                    rv: RecyclerView,
                    vh: RecyclerView.ViewHolder
                ): Int {
                    val pos = vh.bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return 0
                    if (!ayar.kaydirilabilir(pos)) return 0
                    return super.getSwipeDirs(rv, vh)
                }

                override fun getSwipeThreshold(vh: RecyclerView.ViewHolder): Float = ESIK

                /** Kaydırma hızını biraz düşür: kaza ile silme azalsın. */
                override fun getSwipeEscapeVelocity(varsayilan: Float): Float =
                    varsayilan * 1.6f

                override fun onSwiped(vh: RecyclerView.ViewHolder, yon: Int) {
                    val pos = vh.bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return
                    Titresim.dokunus(vh.itemView)
                    runCatching {
                        if (yon == ItemTouchHelper.LEFT) sola?.invoke(pos)
                        else saga?.invoke(pos)
                    }.onFailure { android.util.Log.w(TAG, "onSwiped", it) }
                }

                override fun onChildDraw(
                    c: Canvas,
                    rv: RecyclerView,
                    vh: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    durum: Int,
                    aktif: Boolean
                ) {
                    if (durum == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        ciz.zemin(c, vh.itemView, dX)
                        // Eşik geçildiğinde bir kez titret
                        val gecti = Math.abs(dX) > vh.itemView.width * ESIK
                        val onceki = vh.itemView.getTag(R.id.ga_tag_kaydirma) as? Boolean ?: false
                        if (gecti != onceki) {
                            vh.itemView.setTag(R.id.ga_tag_kaydirma, gecti)
                            if (gecti) Titresim.tik(vh.itemView)
                        }
                        // Kart kaydıkça hafifçe soluklaşsın
                        val oran = (Math.abs(dX) / vh.itemView.width).coerceIn(0f, 1f)
                        vh.itemView.alpha = 1f - oran * 0.35f
                    }
                    super.onChildDraw(c, rv, vh, dX, dY, durum, aktif)
                }

                override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                    super.clearView(rv, vh)
                    vh.itemView.alpha = 1f
                    vh.itemView.setTag(R.id.ga_tag_kaydirma, false)
                }
            }

            val yardimci = ItemTouchHelper(geriCagri)
            yardimci.attachToRecyclerView(recycler)
            yardimci
        }.onFailure { android.util.Log.w(TAG, "kur", it) }.getOrNull()
    }

    // ------------------------------------------------------------------

    /** Kaydırılan kartın altındaki renkli zemini ve ikonu çizer. */
    private class Cizici(private val ayar: Ayar) {

        private val zeminBoya = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ikonBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        private val yaziBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        private val dikdortgen = RectF()

        fun zemin(c: Canvas, kart: android.view.View, dX: Float) {
            if (dX == 0f) return
            val yg = kart.resources.displayMetrics.density
            val sola = dX < 0

            val renk = if (sola) ayar.solaRenk else ayar.sagaRenk
            val ikon = if (sola) ayar.solaIkon else ayar.sagaIkon
            val etiket = (if (sola) ayar.solaEtiket else ayar.sagaEtiket) ?: return

            // Kaydırma mesafesiyle orantılı doygunluk
            val oran = (Math.abs(dX) / kart.width).coerceIn(0f, 1f)
            val alfa = (60 + oran * 195).toInt().coerceIn(0, 255)
            zeminBoya.color = Color.argb(
                alfa, Color.red(renk), Color.green(renk), Color.blue(renk)
            )

            // Kartın kendi köşe yarıçapına yakın bir zemin çiz
            val ust = kart.top.toFloat() + yg * 5
            val alt = kart.bottom.toFloat() - yg * 5
            if (sola) {
                dikdortgen.set(kart.right.toFloat() + dX, ust, kart.right.toFloat() - yg * 12, alt)
            } else {
                dikdortgen.set(kart.left.toFloat() + yg * 12, ust, kart.left.toFloat() + dX, alt)
            }
            c.drawRoundRect(dikdortgen, yg * 16, yg * 16, zeminBoya)

            // Eşik geçilince ikon büyüsün — kullanıcı bırakmadan sonucu bilsin
            val gecti = Math.abs(dX) > kart.width * ESIK
            val ikonBoyut = if (gecti) yg * 26 else yg * 21
            ikonBoya.textSize = ikonBoyut
            yaziBoya.textSize = yg * 12

            val merkezY = (ust + alt) / 2f
            val x = if (sola) kart.right - yg * 44 else kart.left + yg * 44

            // Yalnız yeterince kaydıysa ikon/yazı görünsün
            if (Math.abs(dX) > yg * 56) {
                val fm = ikonBoya.fontMetrics
                c.drawText(ikon, x, merkezY - (fm.ascent + fm.descent) / 2f - yg * 6, ikonBoya)
                c.drawText(etiket, x, merkezY + yg * 18, yaziBoya)
            }
        }
    }

    // ------------------------------------------------------------------

    /** Tema renklerini kullanan hazır ayar: sil / tamamla. */
    fun silTamamla(recycler: RecyclerView): Ayar {
        val hata = runCatching {
            MaterialColors.getColor(recycler, com.google.android.material.R.attr.colorError, 0xFFD9534F.toInt())
        }.getOrDefault(0xFFD9534F.toInt())
        val ctx = recycler.context
        return Ayar(
            solaEtiket = ctx.getString(R.string.delete),
            solaIkon = "🗑",
            solaRenk = hata,
            sagaEtiket = ctx.getString(R.string.ti_tamamla),
            sagaIkon = "✓",
            sagaRenk = 0xFF4C9A5A.toInt()
        )
    }
}
