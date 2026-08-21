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
 * v11.55 — Uyku takibi ekranı.
 *
 * Uyku kaydı ekleme (süre + kalite), son 7 gün özeti ve kayıt listesi.
 * Veri [UykuMotor] üzerinden kalıcıdır. Tamamen programatik View.
 */
class UykuActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, UykuActivity::class.java))
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
            text = "😴 Uyku Takibi"
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
            text = "Uyku süreni ve kaliteni kaydet, haftalık özeti gör."
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
        val ortDakika = UykuMotor.son7GunOrtalamaDakika(this)
        val ortKalite = UykuMotor.son7GunOrtalamaKalite(this)
        val kayitSayisi = UykuMotor.son7GunKayitSayisi(this)
        val ozet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
        }
        ozet.addView(TextView(this).apply {
            text = "📊 Son 7 Gün"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        if (kayitSayisi == 0) {
            ozet.addView(TextView(this).apply {
                text = "Henüz uyku kaydı yok. \"Uyku Ekle\" ile başla."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, 0)
            })
        } else {
            ozet.addView(TextView(this).apply {
                text = "🌙 Ortalama süre: ${UykuMotor.sureMetni(ortDakika.toInt())}"
                textSize = 14f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, dp(2), 0, dp(2))
            })
            ozet.addView(TextView(this).apply {
                text = "⭐ Ortalama kalite: ${"%.1f".format(ortKalite)}/5"
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            ozet.addView(TextView(this).apply {
                text = "$kayitSayisi gece kayıtlı"
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        ic.addView(ozet)

        // ── Uyku ekleme butonu ──
        ic.addView(android.widget.Button(this).apply {
            text = "😴 Uyku Ekle"
            isAllCaps = false
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { uykuEkle() }
        })

        // ── Kayıt listesi ──
        ic.addView(TextView(this).apply {
            text = "📖 Uyku Geçmişi"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(16), dp(2), dp(6))
        })
        val kayitlar = UykuMotor.kayitlar(this).take(30)
        if (kayitlar.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Henüz kayıt yok."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(2), dp(4), dp(2), dp(8))
            })
        } else {
            val tarihFormat = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("tr", "TR"))
            kayitlar.forEachIndexed { i, k ->
                val satir = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(10), dp(8))
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
                    val tip = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, tip, true)
                    setBackgroundResource(tip.resourceId)
                    setOnClickListener {
                        MaterialAlertDialogBuilder(this@UykuActivity)
                            .setTitle("Kaydı sil?")
                            .setMessage("${UykuMotor.sureMetni(k.sureDakika)} uyku kaydı silinsin mi?")
                            .setPositiveButton("Sil") { _, _ ->
                                UykuMotor.sil(this@UykuActivity, i)
                                ciz()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
                satir.addView(TextView(this).apply {
                    text = "🌙"
                    textSize = 16f
                    setPadding(0, 0, dp(8), 0)
                })
                val metinKol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                metinKol.addView(TextView(this).apply {
                    text = UykuMotor.sureMetni(k.sureDakika)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                metinKol.addView(TextView(this).apply {
                    text = UykuMotor.kaliteEtiketi(k.kalite) + " · " + runCatching {
                        tarihFormat.format(java.util.Date(k.tarih))
                    }.getOrElse { "" }
                    textSize = 12f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
                satir.addView(metinKol)
                satir.addView(TextView(this).apply {
                    text = "${"★".repeat(k.kalite)}"
                    textSize = 13f
                    setTextColor(0xFFD4A017.toInt())
                })
                ic.addView(satir)
            }
        }

        icerik.addView(sari)
    }

    private fun uykuEkle() {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val saatEt = EditText(this).apply {
            hint = "Uyku süresi (saat, örn: 7.5)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
        }
        val kaliteEt = EditText(this).apply {
            hint = "Kalite 1-5 (örn: 4)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        ic.addView(saatEt)
        ic.addView(kaliteEt)

        MaterialAlertDialogBuilder(this)
            .setTitle("Uyku Ekle")
            .setView(ic)
            .setPositiveButton("Kaydet") { _, _ ->
                val saat = saatEt.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                val kalite = kaliteEt.text.toString().toIntOrNull() ?: 3
                if (saat in 0.5..16.0) {
                    UykuMotor.ekle(this, (saat * 60).toInt(), kalite.coerceIn(1, 5))
                    Titresim.basari(this)
                    ciz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
