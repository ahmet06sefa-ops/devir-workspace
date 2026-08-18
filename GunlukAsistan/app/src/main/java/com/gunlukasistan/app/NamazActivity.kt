package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.46 — Namaz vakitleri ekranı.
 *
 * Bölümler:
 *   1. Sıradaki vakit + geri sayım + "şimdi ne yapmalı" önerisi
 *   2. Bugünün 6 vakti (aktif olan vurgulu)
 *   3. Plan özeti + Plan sekmesine yönlendirme
 *
 * v7.55: Vakit aralarına iş planlama bölümü buradan alınıp alt menüdeki
 * **Plan** sekmesine ([PlanFragment]) taşındı. Veri kaynağı değişmedi
 * ([NamazPlan]) — iki ekran da aynı işleri görür.
 */
class NamazActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, NamazActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density

    /**
     * v8.6 · Öneri 27 — Kullanıcının yazı boyutu tercihini uygular.
     *
     * `Configuration.fontScale` tüm `sp` birimlerini bir kerede
     * ölçekliyor; 71 layout'a tek tek dokunmaya gerek kalmıyor.
     */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_namaz)

        findViewById<TextView>(R.id.nmClose).setOnClickListener { finish() }
        // v7.48: ⚙ → tüm Diyanet ayarlarının toplandığı tam ekran
        findViewById<TextView>(R.id.nmSettings).setOnClickListener {
            NamazAyarActivity.ac(this)
        }
        // v7.55: plan artik alt menudeki Plan sekmesinde
        findViewById<com.google.android.material.button.MaterialButton>(R.id.nmPlanOpen)
            .setOnClickListener { planSekmesiniAc() }

        ciz()
    }

    override fun onResume() {
        super.onResume()
        ciz()
    }

    private fun ciz() {
        ustKartiCiz()
        vakitleriCiz()
        planOzetiniCiz()
    }

    /**
     * v7.55: Plan bolumu Plan sekmesine tasindi.
     * Burada yalnizca "kac is bitti" ozeti gosterilir.
     */
    private fun planOzetiniCiz() {
        val (biten, toplam) = NamazPlan.bugunOzet(this)
        findViewById<TextView>(R.id.nmPlanSummary).text =
            if (toplam == 0) getString(R.string.nm_plan_empty)
            else getString(R.string.nm_plan_summary, biten, toplam)
    }

    /** Ana ekrandaki Plan sekmesini acar ve bu ekrani kapatir. */
    private fun planSekmesiniAc() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, 16)
            }
        )
        finish()
    }

    // ═══════════════════════════════════════════════════════════════
    // 1) SIRADAKİ VAKİT
    // ═══════════════════════════════════════════════════════════════

    private fun ustKartiCiz() {
        val gun = NamazVakti.bugunDuzeltilmis(this)
        val simdi = NamazVakti.simdiDakika()
        val (sonrakiVakit, kalan) = gun.sonraki(simdi)

        findViewById<TextView>(R.id.nmCity).text = getString(
            R.string.nm_city_date,
            NamazVakti.sehirAdi(this),
            SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR")).format(Date())
        )
        findViewById<TextView>(R.id.nmNextLabel).text =
            getString(R.string.nm_next, getString(sonrakiVakit.adRes))
        findViewById<TextView>(R.id.nmNextTime).text = gun.saat(sonrakiVakit)
        findViewById<TextView>(R.id.nmNextLeft).text =
            getString(R.string.nm_left, NamazPlan.sureMetni(kalan))

        val oneri = NamazPlan.simdiNeYapmali(this)
        findViewById<TextView>(R.id.nmSuggest).apply {
            text = oneri
            visibility = if (oneri.isBlank()) View.GONE else View.VISIBLE
            setTextColor(
                MaterialColors.getColor(
                    this@NamazActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2) BUGÜNÜN VAKİTLERİ
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // v11.06: VAKTİN SÖZÜ / HİKMETLİ DİNİ SÖZLER KARTI
    // ═══════════════════════════════════════════════════════════════
    private fun vaktinSozuKarti(
        ctx: android.content.Context,
        dilim: NamazPlan.Dilim
    ): View {
        val (baslik, metin) = DiniSozMotoru.vaktinSozunuGetir(dilim)
        fun dp(v: Int) = (v * yogunluk).toInt()

        val card = com.google.android.material.card.MaterialCardView(ctx).apply {
            radius = 16 * yogunluk
            cardElevation = 2 * yogunluk
            strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(
                ctx,
                com.google.android.material.R.attr.colorPrimary,
                0xFF6200EE.toInt()
            )
            setCardBackgroundColor(
                MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorPrimaryContainer,
                    0xFFE8EAF6.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val icLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val ustSatir = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val emojiTv = TextView(ctx).apply {
            text = "🕌✨ "
            textSize = 20f
        }

        val baslikTv = TextView(ctx).apply {
            text = baslik
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                    0xFF1A237E.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val metinTv = TextView(ctx).apply {
            text = metin
            textSize = 14f
            setLineSpacing(dp(3).toFloat(), 1f)
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setTextColor(
                MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                    0xFF1A237E.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        val btnDegistir = android.widget.Button(ctx, null, android.R.attr.borderlessButtonStyle).apply {
            text = "🔄 Başka Söz"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorPrimary,
                    0xFF6200EE.toInt()
                )
            )
            setOnClickListener {
                val (yeniBaslik, yeniMetin) = DiniSozMotoru.sonrakiSozuGetir(dilim, metinTv.text.toString())
                baslikTv.text = yeniBaslik
                metinTv.text = yeniMetin
            }
        }

        ustSatir.addView(emojiTv)
        ustSatir.addView(baslikTv)
        ustSatir.addView(btnDegistir)

        icLayout.addView(ustSatir)
        icLayout.addView(metinTv)
        card.addView(icLayout)

        return card
    }

    private fun vakitleriCiz() {
        val kap = findViewById<LinearLayout>(R.id.nmTimes)
        kap.removeAllViews()

        val gun = NamazVakti.bugunDuzeltilmis(this)
        val simdi = NamazVakti.simdiDakika()
        val aktif = gun.aktifVakit(simdi)
        val dilim = NamazPlan.aktifDilim(gun, simdi)

        // v11.06: Gösterişli Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Kartı
        kap.addView(vaktinSozuKarti(this, dilim))

        NamazVakti.Vakit.entries.forEach { v ->
            val bu = v == aktif
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    (8 * yogunluk).toInt(), (9 * yogunluk).toInt(),
                    (8 * yogunluk).toInt(), (9 * yogunluk).toInt()
                )
                if (bu) {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 12 * yogunluk
                        setColor(
                            (MaterialColors.getColor(
                                this@NamazActivity,
                                com.google.android.material.R.attr.colorPrimary, 0
                            ) and 0x00FFFFFF) or 0x22000000
                        )
                    }
                }
            }
            satir.addView(TextView(this).apply {
                text = v.emoji
                textSize = 15f
                setPadding(0, 0, (10 * yogunluk).toInt(), 0)
            })
            satir.addView(TextView(this).apply {
                text = getString(v.adRes)
                textSize = 14f
                if (bu) setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })
            satir.addView(TextView(this).apply {
                text = gun.saat(v)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (bu) setTextColor(
                    MaterialColors.getColor(
                        this@NamazActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
            kap.addView(satir)
        }
    }

}
