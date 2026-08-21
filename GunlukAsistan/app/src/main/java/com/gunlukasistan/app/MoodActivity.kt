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
 * v11.57 — Ruh hali (mood) takibi ekranı.
 *
 * Bugünkü ruh halini (1-5) ve notu kaydet; son 7 gün ortalamasını gör.
 * Veri [MoodMotor] üzerinden kalıcıdır. Tamamen programatik View.
 */
class MoodActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, MoodActivity::class.java))
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
            text = "🎭 Ruh Hali Takibi"
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
            text = "Bugün nasıl hissediyorsun? Kaydet, haftalık ortalamanı gör."
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
        val ort = MoodMotor.son7GunOrtalama(this)
        val kayitSayisi = MoodMotor.son7GunKayitSayisi(this)
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
                text = "Henüz ruh hali kaydı yok. \"Ruh Hali Ekle\" ile başla."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, 0)
            })
        } else {
            ozet.addView(TextView(this).apply {
                text = MoodMotor.emoji(Math.round(ort).toInt()) + " Ortalama: ${"%.1f".format(ort)}/5"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, dp(4), 0, dp(2))
            })
            ozet.addView(TextView(this).apply {
                text = MoodMotor.etiket(Math.round(ort).toInt()) + " · $kayitSayisi kayıt"
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        ic.addView(ozet)

        // ── Ekle butonu ──
        ic.addView(android.widget.Button(this).apply {
            text = "🎭 Ruh Hali Ekle"
            isAllCaps = false
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { moodEkle() }
        })

        // ── Kayıt listesi ──
        ic.addView(TextView(this).apply {
            text = "📖 Geçmiş"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(16), dp(2), dp(6))
        })
        val kayitlar = MoodMotor.kayitlar(this).take(30)
        if (kayitlar.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Henüz kayıt yok."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(2), dp(4), dp(2), dp(8))
            })
        } else {
            val tarihFormat = java.text.SimpleDateFormat("d MMM yyyy HH:mm", java.util.Locale("tr", "TR"))
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
                        MaterialAlertDialogBuilder(this@MoodActivity)
                            .setTitle("Kaydı sil?")
                            .setMessage("Ruh hali kaydı silinsin mi?")
                            .setPositiveButton("Sil") { _, _ ->
                                MoodMotor.sil(this@MoodActivity, i)
                                ciz()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
                satir.addView(TextView(this).apply {
                    text = MoodMotor.emoji(k.puan)
                    textSize = 18f
                    setPadding(0, 0, dp(8), 0)
                })
                val metinKol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                metinKol.addView(TextView(this).apply {
                    text = MoodMotor.etiket(k.puan)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                metinKol.addView(TextView(this).apply {
                    text = (if (k.not.isNotBlank()) k.not + " · " else "") + runCatching {
                        tarihFormat.format(java.util.Date(k.tarih))
                    }.getOrElse { "" }
                    textSize = 12f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
                satir.addView(metinKol)
                ic.addView(satir)
            }
        }

        icerik.addView(sari)
    }

    private fun moodEkle() {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val puanEt = EditText(this).apply {
            hint = "Ruh hali 1-5 (örn: 4)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        val notEt = EditText(this).apply {
            hint = "Not (opsiyonel)"
            setSingleLine(true)
        }
        ic.addView(puanEt)
        ic.addView(notEt)

        MaterialAlertDialogBuilder(this)
            .setTitle("Ruh Hali Ekle")
            .setView(ic)
            .setPositiveButton("Kaydet") { _, _ ->
                val puan = puanEt.text.toString().toIntOrNull()
                if (puan != null && puan in 1..5) {
                    MoodMotor.ekle(this, puan, notEt.text.toString())
                    Titresim.basari(this)
                    ciz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
