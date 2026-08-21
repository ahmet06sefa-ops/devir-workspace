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

/**
 * v11.63 — Ruh–Uyku–Beslenme ilişki analizi ekranı.
 *
 * Mood, uyku ve beslenme verilerinden kişisel içgörü üretir.
 * Veri [IliskiAnalizMotor] üzerinden analiz edilir.
 */
class IliskiAnalizActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, IliskiAnalizActivity::class.java))
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
            text = "🧠 Ruh–Uyku–Beslenme"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        kok.addView(android.view.View(this).apply {
            setBackgroundColor(renk(com.google.android.material.R.attr.colorPrimary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(3)
            )
        })
        kok.addView(TextView(this).apply {
            text = "İyi hissettiğin günlerin uyku ve beslenmesi nasıl?"
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(8), dp(16), dp(8))
        })

        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        sari.addView(ic)

        val analiz = IliskiAnalizMotor.analiz(this)

        val kart = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
        }
        kart.addView(TextView(this).apply {
            text = analiz
            textSize = 14f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(kart)

        ic.addView(TextView(this).apply {
            text = "📌 İpucu: Analiz için aynı güne hem ruh hali hem uyku kaydetmen yeterli. " +
                "Daha fazla kayıt = daha net içgörü."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(12), dp(2), dp(4))
        })

        kok.addView(sari, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(kok)
    }
}
