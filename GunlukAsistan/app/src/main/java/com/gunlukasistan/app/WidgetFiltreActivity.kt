package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors

/**
 * v10.13 · ULTRA-30 / B12 — Görev widget'ının etiket filtresi ekranı.
 *
 * Ana ekrandaki görev widget'ının 🏷 düğmesinden açılır; hangi ÖRNEKTEN
 * gelindiği `EXTRA_APPWIDGET_ID` ile bilinir. Seçim anında kaydedilir,
 * widget'lar zorla tazelenir (liste satırları dahil) ve ekran kapanır —
 * ana ekranda sonuç görülür.
 */
class WidgetFiltreActivity : AppCompatActivity() {

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** v8.6: kullanıcının yazı boyutu tercihi. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, R.string.wg_filtre_hata, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (18 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (28 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@WidgetFiltreActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(baslik(getString(R.string.wg_filtre_baslik)))
        kap.addView(bilgi(getString(R.string.wg_filtre_aciklama)))

        val secili = WidgetFiltre.filtre(this, widgetId)

        // "Tümü" seçeneği
        kap.addView(
            satir(getString(R.string.wg_filtre_tumu), secili.isBlank()) {
                sec(Etiket.YOK)
            }
        )
        // 6 hazır etiket
        Etiket.hepsi.forEach { tanim ->
            kap.addView(
                satir(
                    tanim.emoji + "  " + getString(tanim.adRes),
                    secili == tanim.kod
                ) {
                    sec(tanim.kod)
                }
            )
        }
    }

    private fun sec(kod: String) {
        WidgetFiltre.setFiltre(this, widgetId, kod)
        try {
            // Liste satırları da süzülsün: refreshAll hem UPDATE yayınlar
            // hem de liste widget'larının verisini tazeler.
            WidgetCommon.refreshAll(this, true)
        } catch (e: Exception) {
            android.util.Log.w("WidgetFiltre", "Tazelenemedi", e)
        }
        Toast.makeText(
            this,
            getString(R.string.wg_filtre_kaydedildi, WidgetFiltre.filtreAd(this, widgetId)),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    // ---------------- Arayüz yardımcıları ----------------

    private fun baslik(metin: String) = TextView(this).apply {
        text = metin
        textSize = 19f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (4 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12.5f
        alpha = 0.7f
        setLineSpacing(0f, 1.2f)
        setPadding(0, 0, 0, (8 * yogunluk).toInt())
    }

    /** Radyo satırı (WidgetTemaActivity'deki secenek deseninin sade hâli). */
    private fun satir(ad: String, secili: Boolean, tikla: () -> Unit): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (11 * yogunluk).toInt(), 0, (11 * yogunluk).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { tikla() }
        }
        satir.addView(TextView(this).apply {
            text = if (secili) "◉" else "○"
            textSize = 17f
            setPadding(0, 0, (12 * yogunluk).toInt(), 0)
            if (secili) {
                setTextColor(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            }
        })
        satir.addView(TextView(this).apply {
            text = ad
            textSize = 14.5f
        })
        return satir
    }
}
