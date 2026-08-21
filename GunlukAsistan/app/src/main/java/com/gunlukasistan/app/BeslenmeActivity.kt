package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.54 — Beslenme & kalori takibi ekranı.
 *
 * Günlük öğünler, kalori hedefi/özeti ve su takibi. Veri [BeslenmeMotor]
 * üzerinden kalıcıdır. Tamamen programatik View kullanır.
 */
class BeslenmeActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, BeslenmeActivity::class.java))
                (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
            }
        }
    }

    private lateinit var icerik: FrameLayout

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
            text = "🍽️ Beslenme & Kalori"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        kok.addView(TextView(this).apply {
            text = "Öğün ekle, kalori hedefini takip et, su iç."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        icerik = FrameLayout(this)
        icerik.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        kok.addView(icerik)

        setContentView(kok)
        ciz()
    }

    private fun ciz() {
        icerik.removeAllViews()
        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        sari.addView(ic)

        // ── Özet kartı ──
        val hedef = BeslenmeMotor.kaloriHedefi(this)
        val alinan = BeslenmeMotor.bugunKalori(this)
        val kalan = BeslenmeMotor.bugunKalan(this)
        val ozet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
        }
        ozet.addView(TextView(this).apply {
            text = "📊 Bugünün Özeti"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ozet.addView(satirMetin("Alınan kalori", "$alinan kcal"))
        ozet.addView(satirMetin("Hedef", "$hedef kcal"))
        ozet.addView(satirMetin(
            "Kalan",
            (if (kalan >= 0) "$kalan kcal" else "${-kalan} kcal fazla")
                .let { it },
            vurgulu = kalan < 0
        ))
        // İlerleme çubuğu
        val yuzde = (alinan.toFloat() / hedef).coerceIn(0f, 1f)
        ozet.addView(android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (yuzde * 1000).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10)
            ).apply { topMargin = dp(8) }
        })
        ozet.addView(TextView(this).apply {
            text = "Hedefi değiştirmek için dokun"
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, 0)
        })
        ozet.isClickable = true
        ozet.isFocusable = true
        val tip = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, tip, true)
        ozet.setBackgroundResource(tip.resourceId)
        ozet.setOnClickListener { hedefAyarla() }
        ic.addView(ozet)

        // ── Su takibi ──
        val su = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            val tip2 = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, tip2, true)
            setBackgroundResource(tip2.resourceId)
            setOnClickListener {
                BeslenmeMotor.suEkle(this@BeslenmeActivity, 1)
                Titresim.dokunus(this@BeslenmeActivity.findViewById(android.R.id.content))
                ciz()
            }
        }
        val suBardak = BeslenmeMotor.suBardak(this)
        su.addView(TextView(this).apply {
            text = "💧 Su"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        su.addView(TextView(this).apply {
            text = "  $suBardak bardak · dokun = +1 (${suBardak * 250} ml)"
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        ic.addView(su)

        // ── Öğün ekleme butonu ──
        ic.addView(android.widget.Button(this).apply {
            text = "➕ Öğün Ekle"
            isAllCaps = false
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { ogunEkle() }
        })

        // ── Bugünün öğünleri ──
        ic.addView(TextView(this).apply {
            text = "🍽️ Bugünün Öğünleri"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(16), dp(2), dp(6))
        })
        val ogunler = BeslenmeMotor.ogunler(this)
        if (ogunler.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Henüz öğün eklemedin."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(2), dp(4), dp(2), dp(8))
            })
        } else {
            BeslenmeMotor.OGRUN_TIPLERI.forEach { tipAd ->
                val tipOgunler = ogunler.filter { it.tip == tipAd }
                if (tipOgunler.isNotEmpty()) {
                    ic.addView(TextView(this).apply {
                        text = tipAd
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                        setPadding(dp(2), dp(8), dp(2), dp(2))
                    })
                    tipOgunler.forEachIndexed { i, o ->
                        val satir = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(dp(10), dp(6), dp(10), dp(6))
                            background = android.graphics.drawable.GradientDrawable().apply {
                                cornerRadius = dp(10).toFloat()
                                setColor(0x20B08968)
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = dp(3) }
                            isClickable = true
                            isFocusable = true
                            val tip3 = android.util.TypedValue()
                            theme.resolveAttribute(android.R.attr.selectableItemBackground, tip3, true)
                            setBackgroundResource(tip3.resourceId)
                            setOnClickListener {
                                val ogunIndex = ogunler.indexOf(o)
                                MaterialAlertDialogBuilder(this@BeslenmeActivity)
                                    .setTitle("Öğünü sil?")
                                    .setMessage("${o.ad} (${o.kalori} kcal) silinsin mi?")
                                    .setPositiveButton("Sil") { _, _ ->
                                        BeslenmeMotor.ogunSil(this@BeslenmeActivity, ogunIndex, "")
                                        ciz()
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show()
                            }
                        }
                        satir.addView(TextView(this).apply {
                            text = o.ad
                            textSize = 14f
                            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        })
                        satir.addView(TextView(this).apply {
                            text = "${o.kalori} kcal"
                            textSize = 13f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                        })
                        ic.addView(satir)
                    }
                }
            }
        }

        icerik.addView(sari)
    }

    private fun satirMetin(sol: String, sag: String, vurgulu: Boolean = false): TextView =
        TextView(this).apply {
            text = "$sol — $sag"
            textSize = 13f
            setTextColor(if (vurgulu) renk(com.google.android.material.R.attr.colorError)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(2), 0, dp(2))
        }

    private fun hedefAyarla() {
        val giris = EditText(this).apply {
            setText(BeslenmeMotor.kaloriHedefi(this@BeslenmeActivity).toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Günlük Kalori Hedefi")
            .setView(giris)
            .setPositiveButton("Kaydet") { _, _ ->
                val h = giris.text.toString().toIntOrNull()
                if (h != null && h >= 500) {
                    BeslenmeMotor.kaloriHedefiAyarla(this, h)
                    ciz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun ogunEkle() {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val adEt = EditText(this).apply {
            hint = "Öğün adı (örn: Yumurta + peynir)"
            setSingleLine(true)
        }
        val kaloriEt = EditText(this).apply {
            hint = "Kalori (örn: 350)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        val tipEt = EditText(this).apply {
            hint = "Tip (Kahvaltı/Öğle/Akşam/Ara Öğün)"
            setSingleLine(true)
        }
        ic.addView(adEt)
        ic.addView(kaloriEt)
        ic.addView(tipEt)

        MaterialAlertDialogBuilder(this)
            .setTitle("Öğün Ekle")
            .setView(ic)
            .setPositiveButton("Ekle") { _, _ ->
                val ad = adEt.text.toString().trim()
                val kalori = kaloriEt.text.toString().toIntOrNull() ?: 0
                var tip = tipEt.text.toString().trim()
                if (tip.isEmpty()) tip = "Ara Öğün"
                if (ad.isNotEmpty() && kalori > 0) {
                    BeslenmeMotor.ogunEkle(this, ad, kalori, tip)
                    Titresim.basari(this)
                    ciz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
