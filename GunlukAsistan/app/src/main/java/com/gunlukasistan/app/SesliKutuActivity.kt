package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.14 · ULTRA-30 / E28 — Haftalık sesli gelen kutusu.
 *
 * [SesliKutu]'ya düşen her sesli not burada listelenir: bu hafta ve
 * daha eski diye ikiye ayrılır. Satıra dokunulunca hedefin ekranına
 * gidilir (✅ → görevler, 📝 → notlar, 🕌 → plan, 🛒 → görevler).
 */
class SesliKutuActivity : AppCompatActivity() {

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout
    private val turkce = Locale("tr", "TR")

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (18 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (26 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@SesliKutuActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    override fun onResume() {
        super.onResume()
        ciz()
    }

    private fun ciz() {
        kap.removeAllViews()
        kap.addView(baslik(getString(R.string.ge_kutu_baslik), 20f))

        val liste = SesliKutu.liste(this)
        if (liste.isEmpty()) {
            kap.addView(bilgi(getString(R.string.ge_kutu_bos)))
            return
        }
        kap.addView(bilgi(getString(R.string.ge_kutu_aciklama, liste.size)))

        val (hafta, eski) = SesliKutu.buHafta(liste, System.currentTimeMillis())
        if (hafta.isNotEmpty()) {
            kap.addView(baslik(getString(R.string.ge_kutu_hafta), 14.5f))
            hafta.forEach { kap.addView(satir(it)) }
        }
        if (eski.isNotEmpty()) {
            kap.addView(baslik(getString(R.string.ge_kutu_eski), 14.5f))
            eski.forEach { kap.addView(satir(it)) }
        }
    }

    private fun satir(not: SesliKutu.Not): android.view.View {
        val govde = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (9 * yogunluk).toInt(), 0, (9 * yogunluk).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { hedefeGit(not) }
        }
        govde.addView(TextView(this).apply {
            text = SesliKutu.hedefEmoji(not.hedef)
            textSize = 20f
            setPadding(0, 0, (12 * yogunluk).toInt(), 0)
        })
        govde.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@SesliKutuActivity).apply {
                    text = not.metin
                    textSize = 14.5f
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                addView(TextView(this@SesliKutuActivity).apply {
                    text = SimpleDateFormat("d MMM EEE, HH:mm", turkce)
                        .format(Date(not.ts))
                    textSize = 11.5f
                    alpha = 0.6f
                })
            }
        )
        govde.addView(TextView(this).apply {
            text = "›"
            textSize = 18f
            alpha = 0.5f
        })
        return govde
    }

    /** Satırın hedefini uygulamada aç. */
    private fun hedefeGit(not: SesliKutu.Not) {
        val ekran = when (not.hedef) {
            "GOREV", "ALISVERIS" -> WidgetCommon.SCREEN_TASKS
            "PLAN" -> WidgetCommon.SCREEN_PLAN
            "ASISTAN" -> WidgetCommon.SCREEN_HOME
            else -> 5 // Notlar sekmesi (MainActivity.createFragment)
        }
        startActivity(
            android.content.Intent(this, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, ekran)
            }
        )
        finish()
    }

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (10 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12.5f
        alpha = 0.7f
        setLineSpacing(0f, 1.25f)
        setPadding(0, 0, 0, (6 * yogunluk).toInt())
    }
}
