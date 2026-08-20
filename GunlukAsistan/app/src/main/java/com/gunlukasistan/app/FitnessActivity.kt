package com.gunlukasistan.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.42 — Kas sistemi 3D ekranı.
 *
 * Gerçek interaktif 3D kas modeli: [WebView] içinde Three.js (WebGL)
 * ile çizilen, döndürülebilir, yakınlaştırılabilir ve her kas grubu
 * tıklanabilir bir insan iskeleti/kas modeli. Kasa dokunulunca JS,
 * [AndroidKopru] üzerinden [kasSecildi]'yi çağırır; alttaki panelde
 * o kasın rehberi gösterilir ([KasRehber]).
 *
 * 3D model ve Three.js, `assets/kas3d/` altındadır → çevrimdışı çalışır.
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
    private lateinit var webView: WebView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun renk(attr: Int): Int = com.google.android.material.color.MaterialColors.getColor(
        this, attr, 0xFFB08968.toInt()
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    @SuppressLint("SetJavaScriptEnabled")
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
            text = "🦴 Kas Sistemi — 3D"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(16), dp(14), dp(16), dp(2))
        })
        kok.addView(TextView(this).apply {
            text = "Modeli döndür, yakınlaştır, kasa dokun — nasıl geliştireceğini öğren."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        // 3D WebView — ekranın üst ~55%
        webView = WebView(this)
        val webLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f
        )
        webView.layoutParams = webLp
        webView.setBackgroundColor(0xFF121212.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.addJavascriptInterface(AndroidKopru(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/kas3d/kas3d.html")
        kok.addView(webView)

        // Detay paneli (kaydırılabilir)
        val detaySar = ScrollView(this)
        detayAlan = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        detaySar.addView(detayAlan)
        detaySar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        )
        kok.addView(detaySar)

        setContentView(kok)
        baslangicMesaji()
    }

    /** Kotlin ⇄ JS köprüsü. JS "kasSecildi(kod)" çağırınca buraya düşer. */
    inner class AndroidKopru {
        @android.webkit.JavascriptInterface
        fun kasSecildi(kod: String) {
            runOnUiThread { kasSec(kod) }
        }
    }

    private fun baslangicMesaji() {
        detayAlan.removeAllViews()
        detayAlan.addView(TextView(this).apply {
            text = "👆 3D modeldeki kaslara dokun"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(6), dp(2), dp(4))
        })
        detayAlan.addView(TextView(this).apply {
            text = "Sürükleyerek döndür, iki parmakla yakınlaştır. Bir kasa dokunduğunda " +
                "ne işe yaradığını, nasıl geliştireceğini ve hangi egzersizlerin işe " +
                "yaradığını burada göstereceğim."
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(0), dp(2), dp(6))
        })
    }

    private fun kasSec(kod: String) {
        val rehber = KasRehber.getir(kod)
        if (rehber == null) { baslangicMesaji(); return }

        detayAlan.removeAllViews()

        detayAlan.addView(TextView(this).apply {
            text = "${rehber.emoji} ${rehber.ad}"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(2), dp(2), dp(2))
        })

        // ── Gelişim istatistik kartı ──
        val toplam = FitnessMotor.kasToplamYapilma(this, kod)
        val bugun = FitnessMotor.kasBugunSet(this, kod)
        val sonMs = FitnessMotor.kasSonAntrenmanMs(this, kod)
        val kart = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4); bottomMargin = dp(4) }
            layoutParams = lp
        }
        kart.addView(TextView(this).apply {
            text = if (toplam == 0) "📈 Henüz bu kas için antrenman kaydın yok."
            else "📈 Bu kası $toplam kez çalıştın · Bugün $bugun set"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        if (sonMs > 0) {
            kart.addView(TextView(this).apply {
                text = "Son antrenman: " + java.text.SimpleDateFormat(
                    "d MMM yyyy HH:mm", java.util.Locale("tr", "TR")
                ).format(java.util.Date(sonMs))
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(2), 0, 0)
            })
        }
        detayAlan.addView(kart)

        detayAlan.addView(altBaslik("📌 Ne işe yarar?"))
        detayAlan.addView(paragraf(rehber.islev))
        detayAlan.addView(altBaslik("🏋️ Nasıl geliştirilir?"))
        detayAlan.addView(paragraf(rehber.gelistirme))
        detayAlan.addView(altBaslik("🔁 Önerilen set/tekrar"))
        detayAlan.addView(paragraf(rehber.setOneri))

        detayAlan.addView(altBaslik("💪 Bu kası çalıştıran egzersizler"))
        detayAlan.addView(TextView(this).apply {
            text = "Egzersize dokun: talimatları gör ve set kaydet."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(0), dp(2), dp(4))
        })
        val egzersizler = FitnessMotor.kasGrubunaGore(FitnessMotor.tumu(this), kod).take(8)
        if (egzersizler.isEmpty()) {
            detayAlan.addView(paragraf("Bu kas için veritabanında egzersiz bulunamadı."))
        } else {
            egzersizler.forEach { e ->
                val yapilma = FitnessMotor.egzersizYapilmaSayisi(this, e.id)
                val satir = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    val tip = android.util.TypedValue()
                    this@FitnessActivity.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground, tip, true
                    )
                    setBackgroundResource(tip.resourceId)
                    setOnClickListener { egzersizKaydi(e, kod) }
                }
                satir.addView(TextView(this).apply {
                    text = FitnessMotor.kasEmoji(kod)
                    textSize = 18f
                    setPadding(0, 0, dp(8), 0)
                })
                val metinKol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                metinKol.addView(TextView(this).apply {
                    text = e.isim
                    textSize = 14f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                metinKol.addView(TextView(this).apply {
                    text = FitnessMotor.ekipmanTuru(e.ekipman) +
                        (if (yapilma > 0) " · $yapilma× yaptın" else "")
                    textSize = 12f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
                satir.addView(metinKol)
                satir.addView(TextView(this).apply {
                    text = "＋"
                    textSize = 20f
                    setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                })
                detayAlan.addView(satir)
            }
        }
        detayAlan.addView(TextView(this).apply {
            text = "💡 İpucu: hareketi yavaş ve kontrollü yap, sırtını düz tut, " +
                "son tekrarı zorlanarak bitir."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(10), dp(2), dp(2))
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

    /** Egzersize dokununca: talimatlar + set/tekrar/ağırlık kaydetme. */
    private fun egzersizKaydi(e: FitnessMotor.Egzersiz, kasKod: String) {
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

        // Talimatlar (kısa, kaydırılabilir olmasın diye 5 ile sınırlı)
        ic.addView(TextView(this).apply {
            text = "📖 Talimatlar"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(2))
        })
        e.talimatlar.take(5).forEachIndexed { i, t ->
            ic.addView(TextView(this).apply {
                text = "${i + 1}. $t"
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(3), 0, dp(3))
            })
        }

        // Set kaydetme alanları
        ic.addView(TextView(this).apply {
            text = "🧾 Bu egzersizi kaydet"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, dp(10), 0, dp(2))
        })
        val tekrarEt = android.widget.EditText(this).apply {
            hint = "Tekrar (örn: 10)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        val agirlikEt = android.widget.EditText(this).apply {
            hint = "Ağırlık kg (örn: 20)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        val alanlar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tekrarEt.layoutParams = lp
        agirlikEt.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(10)
        }
        alanlar.addView(tekrarEt)
        alanlar.addView(agirlikEt)
        ic.addView(alanlar)

        MaterialAlertDialogBuilder(this)
            .setTitle("Antrenmana Ekle")
            .setView(ic)
            .setPositiveButton("💾 Kaydet") { _, _ ->
                val tekrar = tekrarEt.text.toString().toIntOrNull() ?: 0
                val agirlik = agirlikEt.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                if (tekrar > 0) {
                    FitnessMotor.antrenmanEkle(
                        this,
                        FitnessMotor.AntrenmanKaydi(
                            egzersizId = e.id,
                            egzersizAdi = e.isim,
                            kasKod = kasKod,
                            setler = listOf(FitnessMotor.AntrenmanSeti(tekrar, agirlik)),
                            tarih = System.currentTimeMillis()
                        )
                    )
                    Titresim.basari(this)
                    // İstatistiklerin güncellenmesi için kası yeniden çiz
                    kasSec(kasKod)
                } else {
                    Titresim.ret(tekrarEt)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
