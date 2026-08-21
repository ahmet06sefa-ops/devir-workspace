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
 * v11.58 — Haftalık Sağlık Raporu ekranı.
 *
 * Tüm sağlık modüllerinin son 7 günlük özetini tek bir rapor metni olarak
 * gösterir; paylaş/kopyala butonu sunar.
 */
class SaglikRaporuActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, SaglikRaporuActivity::class.java))
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
            text = "📋 Haftalık Sağlık Raporu"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        // v11.61: renkli aksan şeridi
        kok.addView(android.view.View(this).apply {
            setBackgroundColor(renk(com.google.android.material.R.attr.colorPrimary))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(3)
            )
        })
        kok.addView(TextView(this).apply {
            text = "Tüm sağlık modüllerinin özeti — paylaşabilirsin."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        val rapor = SaglikRaporuMotor.rapor(this)

        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(8))
        }
        sari.addView(ic)

        ic.addView(TextView(this).apply {
            text = rapor
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setTypeface(Typeface.MONOSPACE)
        })
        kok.addView(sari, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Alt butonlar
        val butonlar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
        }
        butonlar.addView(android.widget.Button(this).apply {
            text = "📤 Paylaş"
            isAllCaps = false
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { paylas(rapor) }
        })
        butonlar.addView(android.widget.Button(this).apply {
            text = "📋 Kopyala"
            isAllCaps = false
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { kopyala(rapor) }
        })
        kok.addView(butonlar)

        setContentView(kok)
    }

    private fun paylas(rapor: String) {
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, SaglikRaporuMotor.baslik())
                putExtra(android.content.Intent.EXTRA_TEXT, rapor)
            }
            startActivity(android.content.Intent.createChooser(intent, "Raporu paylaş"))
        }
    }

    private fun kopyala(rapor: String) {
        runCatching {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("rapor", rapor))
            android.widget.Toast.makeText(this, "Rapor kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
