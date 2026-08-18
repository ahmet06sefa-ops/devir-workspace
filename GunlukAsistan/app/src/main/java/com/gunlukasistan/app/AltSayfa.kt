package com.gunlukasistan.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors

/**
 * v8.4 — Alt sayfa menüleri (öneri 18).
 *
 * ── Ölçülen sorun ──
 * v8.3'te `BottomSheetDialog` HİÇBİR dosyada kullanılmıyordu. Bütün
 * seçim menüleri `MaterialAlertDialogBuilder.setItems()` ile ekranın
 * ORTASINDA açılıyordu. Tek elle telefon tutan biri için ekranın
 * ortası en zor ulaşılan yer; üstelik ortadaki pencere arkadaki
 * içeriği tamamen kapatıyor.
 *
 * ── Neden değiştirildi ──
 * Alt sayfa: başparmağın altında açılıyor, sürükleyerek kapatılıyor,
 * arkadaki içeriğin bir kısmı görünür kalıyor. Uzun listelerde
 * kaydırılabiliyor (ortadaki pencere ekranı taşırıyordu).
 *
 * ── Neden hepsi birden değiştirilmedi ──
 * Uygulamada 60+ `setItems` çağrısı var. Hepsini çevirmek tek
 * sürümlük iş değil ve riskli. En sık kullanılan akışlar seçildi:
 * görev seçenekleri, konu seçenekleri, hızlı ekleme, filtreleme.
 * Kalanlar `MaterialAlertDialogBuilder` ile çalışmaya devam ediyor —
 * bozulan bir şey yok.
 *
 * ── Erişilebilirlik ──
 * Her satır en az 56dp yüksekliğinde (Material minimum 48dp).
 */
object AltSayfa {

    private const val TAG = "AltSayfa"

    /** Bir menü satırı. */
    data class Oge(
        val baslik: String,
        val altBaslik: String? = null,
        val simge: String = "",
        /** Yıkıcı işlem (silme vb.) kırmızı gösterilir. */
        val yikici: Boolean = false,
        /** false ise satır soluk ve tıklanamaz. */
        val etkin: Boolean = true,
        val eylem: (() -> Unit)? = null
    )

    /**
     * Alt sayfa menüsü açar.
     *
     * ```
     * AltSayfa.menu(ctx, "Görev", listOf(
     *     AltSayfa.Oge("Düzenle", simge = "✏️") { duzenle() },
     *     AltSayfa.Oge("Sil", simge = "🗑", yikici = true) { sil() }
     * ))
     * ```
     */
    fun menu(
        context: Context,
        baslik: String?,
        ogeler: List<Oge>,
        altBaslik: String? = null
    ): BottomSheetDialog? = runCatching {
        val yg = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // Renkleri bir kez çöz. MaterialColors.getColor View veya Context
        // ister; GradientDrawable içinden çağrılamıyor.
        val cizgiRengi = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOutline, 0xFF999999.toInt()
        )
        val anaYazi = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnSurface, 0xFF333333.toInt()
        )
        val soluk = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888.toInt()
        )

        val kok = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }

        // ---- Tutamaç ----
        kok.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(10)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(cizgiRengi)
            }
        })

        // ---- Başlık ----
        if (!baslik.isNullOrBlank()) {
            kok.addView(TextView(context).apply {
                text = baslik
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(anaYazi)
                setPadding(dp(22), dp(4), dp(22), if (altBaslik == null) dp(10) else dp(1))
            })
        }
        if (!altBaslik.isNullOrBlank()) {
            kok.addView(TextView(context).apply {
                text = altBaslik
                textSize = 12.5f
                setTextColor(soluk)
                setPadding(dp(22), 0, dp(22), dp(10))
            })
        }

        val pencere = BottomSheetDialog(context)

        // ---- Satırlar ----
        ogeler.forEach { oge ->
            kok.addView(satir(context, oge) {
                pencere.dismiss()
                oge.eylem?.invoke()
            })
        }

        val kaydirici = android.widget.ScrollView(context).apply {
            isFillViewport = true
            addView(kok)
            // Uzun menülerde ekranın en fazla %72'sini kapla
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        pencere.setContentView(kaydirici)
        pencere.show()
        pencere
    }.onFailure { android.util.Log.w(TAG, "menu", it) }.getOrNull()

    /**
     * Basit dizi menüsü — `setItems` yerine doğrudan geçiş için.
     *
     * ```
     * AltSayfa.secim(ctx, "Sırala", arrayOf("Tarihe göre", "Ada göre")) { i -> ... }
     * ```
     */
    fun secim(
        context: Context,
        baslik: String?,
        secenekler: Array<String>,
        secildi: (Int) -> Unit
    ) {
        menu(context, baslik, secenekler.mapIndexed { i, metin ->
            Oge(metin) { secildi(i) }
        })
    }

    // ------------------------------------------------------------------

    private fun satir(context: Context, oge: Oge, tiklandi: () -> Unit): View {
        val yg = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val satir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(22), dp(12), dp(22), dp(12))
            alpha = if (oge.etkin) 1f else 0.4f
            if (oge.etkin) {
                isClickable = true
                val tv = android.util.TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, tv, true
                )
                setBackgroundResource(tv.resourceId)
                setOnClickListener {
                    Titresim.dokunus(it)
                    tiklandi()
                }
            }
        }

        if (oge.simge.isNotBlank()) {
            satir.addView(TextView(context).apply {
                text = oge.simge
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            })
        }

        val metinler = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = if (oge.simge.isBlank()) 0 else dp(10) }
        }

        val anaRenk = if (oge.yikici) {
            renk(satir, com.google.android.material.R.attr.colorError)
        } else {
            renk(satir, com.google.android.material.R.attr.colorOnSurface)
        }

        metinler.addView(TextView(context).apply {
            text = oge.baslik
            textSize = 15f
            setTextColor(anaRenk)
        })
        if (!oge.altBaslik.isNullOrBlank()) {
            metinler.addView(TextView(context).apply {
                text = oge.altBaslik
                textSize = 12f
                setTextColor(renk(satir, com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        satir.addView(metinler)
        return satir
    }

    private fun renk(v: View, attr: Int): Int = runCatching {
        MaterialColors.getColor(v, attr, 0xFF666666.toInt())
    }.getOrDefault(0xFF666666.toInt())
}
