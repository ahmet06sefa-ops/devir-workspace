package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.56 — Sağlık & fitness özet ekranı (Sağlık Karnesi).
 *
 * Kas Sistemi, Beslenme, Su, Uyku ve Bütçe modüllerinin güncel özetini
 * tek ekranda gösterir. Her satır ilgili modülü açabilir.
 */
class SaglikOzetActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, SaglikOzetActivity::class.java))
                (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun renk(attr: Int): Int = com.google.android.material.color.MaterialColors.getColor(
        this, attr, 0xFFB08968.toInt()
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
        }

        kok.addView(TextView(this).apply {
            text = "🩺 Sağlık Karnesi"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        kok.addView(TextView(this).apply {
            text = "Tüm sağlık & fitness modüllerinin güncel özeti."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        sari.addView(ic)

        // Özet satırları
        val satirlar = SaglikOzetMotor.karnesi(this)
        satirlar.forEach { satir ->
            val kart = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                isClickable = true
                isFocusable = true
                val tip = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, tip, true)
                setBackgroundResource(tip.resourceId)
                setOnClickListener { modulAc(satir.baslik) }
            }
            // Modül rengi
            val modulRenk = when {
                satir.baslik.contains("Kas") -> 0xFFE64A19.toInt()
                satir.baslik.contains("Beslenme") || satir.baslik.contains("Su") -> 0xFFFF7043.toInt()
                satir.baslik.contains("Uyku") -> 0xFF5C6BC0.toInt()
                satir.baslik.contains("Mood") -> 0xFFFFEB3B.toInt()
                satir.baslik.contains("harcama") -> 0xFF4CAF50.toInt()
                else -> 0xFF2196F3.toInt()
            }
            // Renkli yarı saydam ikon çemberi
            val ikonKap = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    marginEnd = dp(12)
                }
            }
            ikonKap.addView(TextView(this).apply {
                text = satir.emoji
                textSize = 22f
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor((modulRenk and 0x00FFFFFF) or (0x47 shl 24))
                }
            })
            kart.addView(ikonKap)
            val metinKol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            metinKol.addView(TextView(this).apply {
                text = satir.baslik
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            metinKol.addView(TextView(this).apply {
                text = satir.detay
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            kart.addView(metinKol)
            kart.addView(TextView(this).apply {
                text = "›"
                textSize = 20f
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            })
            ic.addView(kart)
        }

        // Alt bilgi
        ic.addView(TextView(this).apply {
            text = "Satıra dokunarak ilgili modülü açabilirsin."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(6), dp(2), dp(4))
        })

        kok.addView(sari, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(kok)
    }

    private fun modulAc(baslik: String) {
        when {
            baslik.contains("Kas") -> FitnessActivity.ac(this)
            baslik.contains("Beslenme") || baslik.contains("Su") -> BeslenmeActivity.ac(this)
            baslik.contains("Uyku") -> UykuActivity.ac(this)
            baslik.contains("harcama") -> TakipActivity.ac(this, TakipActivity.S_BUTCE)
        }
    }
}
