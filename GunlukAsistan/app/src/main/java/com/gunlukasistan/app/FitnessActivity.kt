package com.gunlukasistan.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
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
    private lateinit var kok: LinearLayout
    private lateinit var icerik: FrameLayout
    private lateinit var detaySar: ScrollView
    private lateinit var btnSekmeModel: TextView
    private lateinit var btnSekmeProgram: TextView
    private lateinit var btnSekmeGecmis: TextView
    private var sekme = 0

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

        kok = LinearLayout(this).apply {
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
            text = "Modeli döndür, kasa dokun, set kaydet — ya da hazır programları gör."
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        // Sekmeler: Model / Program
        val sekmeSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(2), dp(12), dp(4))
        }
        btnSekmeModel = sekmeChip("🧍 3D Model") { gec(0) }
        btnSekmeProgram = sekmeChip("📋 Program") { gec(1) }
        btnSekmeGecmis = sekmeChip("📜 Geçmiş") { gec(2) }
        sekmeSatir.addView(btnSekmeModel)
        sekmeSatir.addView(btnSekmeProgram)
        sekmeSatir.addView(btnSekmeGecmis)
        kok.addView(sekmeSatir)

        // İçerik alanı
        icerik = FrameLayout(this)
        icerik.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        kok.addView(icerik)

        // 3D WebView — model sekmesi içeriği
        webView = WebView(this)
        webView.setBackgroundColor(0xFF121212.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.addJavascriptInterface(AndroidKopru(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/kas3d/kas3d.html")

        // Detay paneli (model sekmesi)
        detaySar = ScrollView(this)
        detayAlan = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        detaySar.addView(detayAlan)

        val modelKok = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modelKok.addView(webView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f))
        modelKok.addView(detaySar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f))
        icerik.addView(modelKok)

        setContentView(kok)
        gec(0)
        baslangicMesaji()
    }

    private fun sekmeChip(metin: String, onTikla: () -> Unit): TextView =
        TextView(this).apply {
            this.text = metin
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
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

    private fun gec(index: Int) {
        sekme = index
        btnSekmeModel.setTextColor(
            if (index == 0) renk(com.google.android.material.R.attr.colorPrimary)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        btnSekmeProgram.setTextColor(
            if (index == 1) renk(com.google.android.material.R.attr.colorPrimary)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        btnSekmeGecmis.setTextColor(
            if (index == 2) renk(com.google.android.material.R.attr.colorPrimary)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        if (index == 0) {
            // Model sekmesi: WebView + detay göster
            icerik.removeAllViews()
            val modelKok = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            modelKok.addView(webView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f))
            modelKok.addView(detaySar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f))
            icerik.addView(modelKok)
        } else if (index == 1) {
            programCiz()
        } else {
            gecmisCiz()
        }
    }

    /** Program sekmesini (ayrı içerik) çizer. */
    private fun programCiz() {
        icerik.removeAllViews()
        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        sari.addView(ic)
        ic.addView(TextView(this).apply {
            text = "🏆 Hazır Antrenman Programları"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(4), dp(2), dp(4))
        })
        ic.addView(TextView(this).apply {
            text = "Bir program seç, haftalık planını gör. Egzersizleri Kas Sistemi'nden takip et."
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(2), dp(0), dp(2), dp(8))
        })

        // ── Haftalık durum kartı ──
        val haftalik = FitnessMotor.haftalikKasSetleri(this)
        val ihmal = FitnessMotor.ihmalEdilenKaslar(this)
        val durumKart = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            layoutParams = lp
        }
        durumKart.addView(TextView(this).apply {
            text = "📅 Son 7 gün"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        if (haftalik.isEmpty()) {
            durumKart.addView(TextView(this).apply {
                text = "Bu hafta henüz antrenman kaydın yok. Kasa dokunup set kaydederek başla!"
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, 0)
            })
        } else {
            val sirali = haftalik.toList().sortedByDescending { it.second }.take(6)
            sirali.forEach { (kod, set) ->
                durumKart.addView(TextView(this).apply {
                    text = "${FitnessMotor.kasEmoji(kod)} ${FitnessMotor.kasTuru(kod)} — $set set"
                    textSize = 12.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                    setPadding(0, dp(2), 0, 0)
                })
            }
            if (ihmal.isNotEmpty()) {
                durumKart.addView(TextView(this).apply {
                    text = "\n⚠️ İhmal edilenler: " + ihmal.take(5).joinToString(", ") { FitnessMotor.kasTuru(it) }
                    textSize = 12f
                    setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                    setPadding(0, dp(4), 0, 0)
                })
            }
        }
        ic.addView(durumKart)

        FitnessMotor.programlar.forEach { p ->
            val kart = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                layoutParams = lp
                isClickable = true
                isFocusable = true
                val tip = android.util.TypedValue()
                this@FitnessActivity.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, tip, true
                )
                setBackgroundResource(tip.resourceId)
                setOnClickListener { programDetay(p) }
            }
            kart.addView(TextView(this).apply {
                text = p.ad
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            kart.addView(TextView(this).apply {
                text = "Haftada ${p.haftadaGun} gün · ${p.seviye} · ${p.gunler.size} gün planı"
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, dp(2), 0, dp(2))
            })
            kart.addView(TextView(this).apply {
                text = p.aciklama
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            ic.addView(kart)
        }

        icerik.addView(sari)
    }

    /** Geçmiş sekmesi: kaydedilen antrenmanların tarihe göre listesi + silme. */
    private fun gecmisCiz() {
        icerik.removeAllViews()
        val sari = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        sari.addView(ic)

        ic.addView(TextView(this).apply {
            text = "📜 Antrenman Geçmişi & Gelişim"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(2), dp(4), dp(2), dp(2))
        })

        // ── Gelişim özeti kartı ──
        val gelisim = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            layoutParams = lp
        }
        gelisim.addView(TextView(this).apply {
            text = "📊 Gelişim Özeti"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        val kayitSayisi = FitnessMotor.antrenmanlar(this).size
        if (kayitSayisi == 0) {
            gelisim.addView(TextView(this).apply {
                text = "Henüz veri yok. Set kaydettikçe PR ve hacim istatistiklerin burada oluşur."
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, 0)
            })
        } else {
            val hacim = FitnessMotor.toplamHacim(this)
            gelisim.addView(TextView(this).apply {
                text = "🏋️ Toplam hacim: ${String.format(java.util.Locale("tr", "TR"), "%.0f", hacim)} kg·tekrar"
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, dp(2), 0, dp(2))
            })
            val enCok = FitnessMotor.enCokCalisilanKaslar(this, 3)
            if (enCok.isNotEmpty()) {
                gelisim.addView(TextView(this).apply {
                    text = "🎯 En çok çalışılanlar: " + enCok.joinToString(", ") { (kod, adet) ->
                        "${FitnessMotor.kasTuru(kod)} ($adet×)"
                    }
                    textSize = 12.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                    setPadding(0, dp(2), 0, dp(2))
                })
            }
            // PR listesi
            val pr = FitnessMotor.prListesi(this, 5)
            if (pr.isNotEmpty()) {
                gelisim.addView(TextView(this).apply {
                    text = "🏆 Kişisel rekorlar:"
                    textSize = 12.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                    setPadding(0, dp(4), 0, dp(2))
                })
                pr.forEach { (_, ad, kg) ->
                    gelisim.addView(TextView(this).apply {
                        text = "· $ad — ${kg}kg"
                        textSize = 12f
                        setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                        setPadding(0, dp(1), 0, 0)
                    })
                }
            }
        }
        ic.addView(gelisim)

        val kayitlar = FitnessMotor.antrenmanlar(this).reversed()
        if (kayitlar.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Henüz antrenman kaydın yok. 3D Model'de kasa dokunup set kaydet, " +
                    "ya da Program'dan bir gün seç."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(2), dp(16), dp(2), dp(8))
            })
            icerik.addView(sari)
            return
        }

        // Tarihe göre grupla
        val gunler = kayitlar.groupBy { FitnessMotor.gunAnahtari(it.tarih) }
        val tarihFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("tr", "TR"))
        gunler.forEach { (gun, list) ->
            val ilkTarih = list.firstOrNull()?.tarih ?: System.currentTimeMillis()
            ic.addView(TextView(this).apply {
                text = "🗓️ " + (runCatching {
                    tarihFormat.format(java.util.Date(ilkTarih))
                }.getOrElse { gun })
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                setPadding(dp(2), dp(12), dp(2), dp(4))
            })
            list.forEach { k ->
                val satir = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
                    }
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(4) }
                    layoutParams = lp
                    isClickable = true
                    isFocusable = true
                    val tip = android.util.TypedValue()
                    this@FitnessActivity.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground, tip, true
                    )
                    setBackgroundResource(tip.resourceId)
                    setOnClickListener { gecmisSil(k, gun) }
                }
                satir.addView(TextView(this).apply {
                    text = FitnessMotor.kasEmoji(k.kasKod)
                    textSize = 16f
                    setPadding(0, 0, dp(8), 0)
                })
                val metinKol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                metinKol.addView(TextView(this).apply {
                    text = k.egzersizAdi
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                val setOzet = k.setler.joinToString(", ") { s ->
                    "${s.tekrar}×${if (s.agirlik > 0) "${s.agirlik}kg" else "kendi"}"
                }
                metinKol.addView(TextView(this).apply {
                    text = "$setOzet · dokun=sil"
                    textSize = 12f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
                satir.addView(metinKol)
                ic.addView(satir)
            }
        }

        icerik.addView(sari)
    }

    /** Kayıt silme onayı + silme işlemi. */
    private fun gecmisSil(k: FitnessMotor.AntrenmanKaydi, gun: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Kaydı sil?")
            .setMessage("${k.egzersizAdi} kaydı silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                val kayitlar = FitnessMotor.antrenmanlar(this)
                val idx = kayitlar.indexOfLast { it.egzersizId == k.egzersizId && it.tarih == k.tarih }
                if (idx >= 0) {
                    FitnessMotor.antrenmanSil(this, idx)
                    gecmisCiz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun programDetay(p: FitnessMotor.Program) {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        ic.addView(TextView(this).apply {
            text = p.ad
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            text = p.aciklama
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(4), 0, dp(8))
        })
        val kaslar = FitnessMotor.programKaslari(p)
        ic.addView(TextView(this).apply {
            text = "🎯 Hedef kaslar: " + kaslar.joinToString(", ") { FitnessMotor.kasTuru(it) }
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, dp(2), 0, dp(6))
        })
        p.gunler.forEachIndexed { i, g ->
            // Her gün tıklanabilir → o günün egzersizlerini göster
            val gunKart = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                layoutParams = lp
                isClickable = true
                isFocusable = true
                val tip = android.util.TypedValue()
                this@FitnessActivity.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, tip, true
                )
                setBackgroundResource(tip.resourceId)
                setOnClickListener { gunEgzersizleri(p, g) }
            }
            gunKart.addView(TextView(this).apply {
                text = "${i + 1}. ${g.ad} — ${g.odak}"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            gunKart.addView(TextView(this).apply {
                text = g.kaslar.joinToString(", ") { FitnessMotor.kasTuru(it) } + "  ·  👆 egzersizleri gör"
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, dp(2), 0, 0)
            })
            ic.addView(gunKart)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Antrenman Programı")
            .setView(android.widget.ScrollView(this).apply { addView(ic) })
            .setPositiveButton("Kapat", null)
            .show()
    }

    /** Program gününün kaslarına uygun egzersizleri listeler, dokununca set kaydettirir. */
    private fun gunEgzersizleri(p: FitnessMotor.Program, g: FitnessMotor.ProgramGunu) {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        ic.addView(TextView(this).apply {
            text = "${p.ad} — ${g.ad}"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            text = "Egzersize dokun → talimat + set kaydet"
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(2), 0, dp(6))
        })

        val egzersizler = FitnessMotor
            .kasGrubuListesineGore(FitnessMotor.tumu(this), g.kaslar)
            .take(12)
        if (egzersizler.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Bu kaslar için egzersiz bulunamadı."
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        } else {
            egzersizler.forEach { e ->
                val satir = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(6), dp(8), dp(6), dp(8))
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    val tip = android.util.TypedValue()
                    this@FitnessActivity.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground, tip, true
                    )
                    setBackgroundResource(tip.resourceId)
                    setOnClickListener { egzersizKaydi(e, e.kaslar.firstOrNull() ?: "") }
                }
                satir.addView(TextView(this).apply {
                    text = FitnessMotor.kasEmoji(e.kaslar.firstOrNull() ?: "")
                    textSize = 16f
                    setPadding(0, 0, dp(8), 0)
                })
                val metinKol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                metinKol.addView(TextView(this).apply {
                    text = e.isim
                    textSize = 13.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                metinKol.addView(TextView(this).apply {
                    text = FitnessMotor.ekipmanTuru(e.ekipman)
                    textSize = 11.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
                satir.addView(metinKol)
                satir.addView(TextView(this).apply {
                    text = "＋"
                    textSize = 18f
                    setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                })
                ic.addView(satir)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Egzersiz Önerileri")
            .setView(android.widget.ScrollView(this).apply { addView(ic) })
            .setPositiveButton("Kapat", null)
            .show()
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
