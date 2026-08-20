package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.40 — Kas iskeleti ekranı.
 *
 * İnteraktif kas haritası ([KasHaritasiView]) önden ve arkadan gösterilir.
 * Bir kas grubuna tıklandığında: kasın adı, işlevi, nasıl geliştirileceği,
 * set önerisi ve o kas grubunun egzersizleri ([FitnessMotor]) görsel
 * olarak anlatılır.
 */
class FitnessActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            runCatching {
                context.startActivity(Intent(context, FitnessActivity::class.java))
                (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
            }
        }
    }

    private lateinit var detayAlan: LinearLayout
    private lateinit var haritaView: KasHaritasiView
    private var gorunum = KasHaritasiView.GORUNUM_ON
    private var seciliKas: String? = null

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

        // Üst bar
        kok.addView(TextView(this).apply {
            text = "🦴 Kas İskeleti"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(16), dp(16), dp(16), dp(4))
        })
        kok.addView(TextView(this).apply {
            text = "Vücudundaki kası seç — nasıl geliştireceğini gösterelim."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        // Ön / Arka sekmesi
        val sekmeSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(2), dp(12), dp(2))
        }
        val onChip = chip("🙍 Ön") { gorunum = KasHaritasiView.GORUNUM_ON; haritaView.gorunum = gorunum; haritaSecimTemiz() }
        val arkaChip = chip("🙎 Arka") { gorunum = KasHaritasiView.GORUNUM_ARKA; haritaView.gorunum = gorunum; haritaSecimTemiz() }
        sekmeSatir.addView(onChip)
        sekmeSatir.addView(arkaChip)
        kok.addView(sekmeSatir)

        // Harita — dikey alanın üst kısmı
        haritaView = KasHaritasiView(this) { kod -> kasSec(kod) }
        haritaView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.25f
        )
        kok.addView(haritaView)

        // Kas listesi (tümü) — hızlı seçim
        val listeSar = ScrollView(this)
        val liste = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
            // ScrollView çocuğu FrameLayout.LayoutParams ister (LinearLayout değil!)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        KasRehber.hepsi().forEach { k ->
            liste.addView(chip(k.emoji + " " + k.ad) { kasSec(k.kod) })
        }
        // Liste sabit yükseklikte bir ScrollView içinde — LinearLayout çocuğu olarak
        listeSar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(120)
        )
        listeSar.addView(liste)
        kok.addView(listeSar)

        // Detay alanı (kaydırılabilir)
        val detaySar = ScrollView(this)
        detayAlan = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        detaySar.addView(detayAlan)
        detaySar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.35f
        )
        kok.addView(detaySar)

        setContentView(kok)

        // Başlangıçta genel açıklama
        baslangicMesaji()
    }

    private fun chip(metin: String, onTikla: () -> Unit): TextView =
        TextView(this).apply {
            this.text = metin
            textSize = 13f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isClickable = true
            isFocusable = true
            val tip = android.util.TypedValue()
            this@FitnessActivity.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, tip, true
            )
            setBackgroundResource(tip.resourceId)
            setOnClickListener { onTikla() }
        }

    private fun haritaSecimTemiz() {
        seciliKas = null
        haritaView.sec(null)
        baslangicMesaji()
    }

    private fun baslangicMesaji() {
        detayAlan.removeAllViews()
        detayAlan.addView(TextView(this).apply {
            text = "👆 Haritadan bir kas grubuna dokun\nveya aşağıdaki listeden seç."
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(4), dp(12), dp(4), dp(8))
        })
        detayAlan.addView(TextView(this).apply {
            text = "Her kas için: ne işe yaradığını, nasıl geliştireceğini, " +
                "kaç set yapman gerektiğini ve hangi egzersizlerin işe yaradığını göstereceğim."
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(4), dp(0), dp(4), dp(8))
        })
    }

    private fun kasSec(kod: String) {
        seciliKas = kod
        haritaView.sec(kod)
        val rehber = KasRehber.getir(kod)
        if (rehber == null) {
            baslangicMesaji()
            return
        }

        detayAlan.removeAllViews()

        // Başlık
        detayAlan.addView(TextView(this).apply {
            text = "${rehber.emoji} ${rehber.ad}"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(4), dp(2), dp(2))
        })

        // İşlev
        detayAlan.addView(altBaslik("📌 Ne işe yarar?"))
        detayAlan.addView(paragraf(rehber.islev))

        // Nasıl geliştirilir
        detayAlan.addView(altBaslik("🏋️ Nasıl geliştirilir?"))
        detayAlan.addView(paragraf(rehber.gelistirme))

        // Set önerisi
        detayAlan.addView(altBaslik("🔁 Önerilen set/tekrar"))
        detayAlan.addView(paragraf(rehber.setOneri))

        // Egzersizler
        detayAlan.addView(altBaslik("💪 Bu kası çalıştıran egzersizler"))
        val egzersizler = FitnessMotor.kasGrubunaGore(FitnessMotor.tumu(this), kod).take(10)
        if (egzersizler.isEmpty()) {
            detayAlan.addView(paragraf("Bu kas için veritabanında egzersiz bulunamadı."))
        } else {
            egzersizler.forEach { e ->
                val satir = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    isClickable = true
                    isFocusable = true
                    val tip = android.util.TypedValue()
                    this@FitnessActivity.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground, tip, true
                    )
                    setBackgroundResource(tip.resourceId)
                    setOnClickListener { egzersizDetay(e) }
                }
                satir.addView(TextView(this).apply {
                    text = FitnessMotor.kasEmoji(kod)
                    textSize = 18f
                    setPadding(0, 0, dp(8), 0)
                })
                satir.addView(TextView(this).apply {
                    text = e.isim + "\n" + FitnessMotor.ekipmanTuru(e.ekipman)
                    textSize = 14f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                    setPadding(0, 0, dp(6), 0)
                })
                detayAlan.addView(satir)
            }
        }

        // İpucu
        detayAlan.addView(TextView(this).apply {
            text = "💡 İpucu: kası çalıştırırken hareketi yavaş ve kontrollü yap, " +
                "sırtını düz tut ve her setin son tekrarını zorlanarak bitir."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(4), dp(10), dp(4), dp(4))
        })
    }

    private fun altBaslik(t: String): TextView = TextView(this).apply {
        text = t
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        setPadding(dp(2), dp(10), dp(2), dp(2))
    }

    private fun paragraf(t: String): TextView = TextView(this).apply {
        text = t
        textSize = 13.5f
        setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        setPadding(dp(2), dp(2), dp(2), dp(2))
    }

    private fun egzersizDetay(e: FitnessMotor.Egzersiz) {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        ic.addView(TextView(this).apply {
            text = "${FitnessMotor.kasEmoji(e.kaslar.firstOrNull() ?: "")} ${e.isim}"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            text = FitnessMotor.ozet(e) + " · " + FitnessMotor.seviyeTuru(e.seviye)
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, dp(4), 0, dp(6))
        })
        ic.addView(TextView(this).apply {
            text = "📖 Talimatlar"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(2))
        })
        e.talimatlar.forEachIndexed { i, t ->
            ic.addView(TextView(this).apply {
                text = "${i + 1}. $t"
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(3), 0, dp(3))
            })
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Egzersiz")
            .setView(ic)
            .setPositiveButton("Kapat", null)
            .show()
    }
}
