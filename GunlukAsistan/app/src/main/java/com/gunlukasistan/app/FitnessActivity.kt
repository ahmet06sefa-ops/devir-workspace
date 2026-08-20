package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.39 — Fitness & Egzersiz ekranı.
 *
 * İki sekme: **Kütüphane** (egzersizleri kas grubu / ekipman / aramayla
 * keşfet) ve **Antrenman Günlüğü** (yaptığın set/tekrar/ağırlık kayıtları).
 *
 * Tamamen programatik View kullanır (layout'ta tıklanabilir öğe yok —
 * ripple kurallarına uyar). Veri: [FitnessMotor].
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

    private lateinit var icerik: FrameLayout
    private lateinit var sekmeKutuphane: TextView
    private lateinit var sekmeAntrenman: TextView
    private var sekme = 0

    private var seciliKas: String? = null
    private var seciliEkipman: String? = null
    private var aramaMetni = ""
    private lateinit var listeKap: LinearLayout

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

        // ── Üst bar ──
        kok.addView(TextView(this).apply {
            text = "💪 Fitness & Egzersiz"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(16), dp(16), dp(16), dp(12))
        })
        kok.addView(TextView(this).apply {
            text = "Açık kaynak egzersiz kütüphanesi (free-exercise-db) · çevrimdışı"
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(16), dp(0), dp(16), dp(8))
        })

        // ── Sekmeler ──
        val sekmeSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        sekmeKutuphane = sekmeChipi("📚 Kütüphane") { gec(0) }
        sekmeAntrenman = sekmeChipi("📋 Antrenmanlar") { gec(1) }
        sekmeSatir.addView(sekmeKutuphane)
        sekmeSatir.addView(sekmeAntrenman)
        kok.addView(sekmeSatir)

        icerik = FrameLayout(this)
        icerik.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        kok.addView(icerik)

        setContentView(kok)
        gec(0)
    }

    private fun sekmeChipi(metin: String, onTikla: () -> Unit): TextView =
        TextView(this).apply {
            this.text = metin
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(10), dp(16), dp(10))
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
        sekmeKutuphane.setTextColor(
            if (index == 0) renk(com.google.android.material.R.attr.colorPrimary)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        sekmeAntrenman.setTextColor(
            if (index == 1) renk(com.google.android.material.R.attr.colorPrimary)
            else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
        )
        icerik.removeAllViews()
        if (index == 0) kutuphaneCiz() else antrenmanCiz()
    }

    // ══════════════════════════════════════════════════════════
    // Kütüphane
    // ══════════════════════════════════════════════════════════

    private fun kutuphaneCiz() {
        val sar = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        sar.addView(ic)

        // Arama
        ic.addView(EditText(this).apply {
            hint = "Egzersiz ara…"
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = null
            setBackgroundColor(0x22B08968)
            setSingleLine(true)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    aramaMetni = s.toString()
                    listeYenile()
                }
            })
        })

        // Kas grubu seçimi (chip satırı)
        val kasSari = ScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val kasSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
        kasSatir.addView(chip(null, "Tümü"))
        FitnessMotor.kasGruplari.forEach { kod -> kasSatir.addView(chip(kod, FitnessMotor.kasTuru(kod))) }
        kasSari.addView(kasSatir)
        ic.addView(kasSari)

        // Ekipman seçimi
        val ekipSari = ScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val ekipSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(0), dp(4), dp(10))
        }
        ekipSatir.addView(chipEkip(null, "Tüm ekipman"))
        FitnessMotor.ekipmanlar.forEach { kod -> ekipSatir.addView(chipEkip(kod, FitnessMotor.ekipmanTuru(kod))) }
        ekipSari.addView(ekipSatir)
        ic.addView(ekipSari)

        listeKap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ic.addView(listeKap)

        icerik.removeAllViews()
        icerik.addView(sar)

        listeYenile()
    }

    private fun listeYenile() {
        listeKap.removeAllViews()
        listeDoldur(listeKap)
    }

    private fun chip(kod: String?, etiket: String): TextView =
        TextView(this).apply {
            this.text = etiket
            textSize = 13f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isClickable = true
            isFocusable = true
            val tip = android.util.TypedValue()
            this@FitnessActivity.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, tip, true
            )
            setBackgroundResource(tip.resourceId)
            setOnClickListener {
                seciliKas = kod
                listeYenile()
            }
        }

    private fun chipEkip(kod: String?, etiket: String): TextView =
        TextView(this).apply {
            this.text = etiket
            textSize = 13f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isClickable = true
            isFocusable = true
            val tip = android.util.TypedValue()
            this@FitnessActivity.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, tip, true
            )
            setBackgroundResource(tip.resourceId)
            setOnClickListener {
                seciliEkipman = kod
                listeYenile()
            }
        }

    private fun listeDoldur(kap: LinearLayout) {
        val tumu = FitnessMotor.tumu(this)
        var liste = FitnessMotor.kasGrubunaGore(tumu, seciliKas)
        liste = FitnessMotor.ekipmanaGore(liste, seciliEkipman)
        liste = FitnessMotor.ara(liste, aramaMetni)

        if (liste.isEmpty()) {
            kap.addView(TextView(this).apply {
                text = "Eşleşen egzersiz bulunamadı."
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(8), dp(20), dp(8), dp(8))
            })
            return
        }

        // Önce mevcut seçimi değil, tüm egzersizleri göster (filtreleme kullanıcının seçimine bağlı)
        val gorunecekler = liste
        gorunecekler.forEach { e ->
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
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
                text = FitnessMotor.kasEmoji(e.kaslar.firstOrNull() ?: "")
                textSize = 20f
                setPadding(0, 0, dp(10), 0)
            })
            val metinKol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            metinKol.addView(TextView(this).apply {
                text = e.isim
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            metinKol.addView(TextView(this).apply {
                text = FitnessMotor.ozet(e) + " · " + FitnessMotor.seviyeTuru(e.seviye)
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            satir.addView(metinKol)
            kap.addView(satir)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Egzersiz detay + set ekleme
    // ══════════════════════════════════════════════════════════

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
            text = FitnessMotor.ozet(e)
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, dp(4), 0, dp(6))
        })

        // Talimatlar
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

        // Set ekleme alanları
        val tekrarEt = EditText(this).apply {
            hint = "Tekrar (örn: 10)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        val agirlikEt = EditText(this).apply {
            hint = "Ağırlık kg (örn: 20)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setHintTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        val alanlar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tekrarEt.layoutParams = lp
        agirlikEt.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(10)
        }
        alanlar.addView(tekrarEt)
        alanlar.addView(agirlikEt)
        ic.addView(alanlar)

        val yapilma = FitnessMotor.egzersizYapilmaSayisi(this, e.id)
        if (yapilma > 0) {
            ic.addView(TextView(this).apply {
                text = "Bu egzersizi $yapilma kez kaydettin."
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(6), 0, 0)
            })
        }

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
                            kasKod = e.kaslar.firstOrNull() ?: "",
                            setler = listOf(FitnessMotor.AntrenmanSeti(tekrar, agirlik)),
                            tarih = System.currentTimeMillis()
                        )
                    )
                    Titresim.basari(this)
                    if (sekme == 1) antrenmanCiz()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ══════════════════════════════════════════════════════════
    // Antrenman günlüğü
    // ══════════════════════════════════════════════════════════

    private fun antrenmanCiz() {
        val sar = ScrollView(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }
        sar.addView(ic)

        // Özet
        val gunler = FitnessMotor.antrenmanlar(this).groupBy { FitnessMotor.gunAnahtari(it.tarih) }
        ic.addView(TextView(this).apply {
            text = "🗓️ Bugün: ${FitnessMotor.bugunToplamSet(this@FitnessActivity)} set · Toplam ${gunler.size} aktif gün"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(4), dp(4), dp(4), dp(10))
        })

        val kayitlar = FitnessMotor.antrenmanlar(this).reversed()
        if (kayitlar.isEmpty()) {
            ic.addView(TextView(this).apply {
                text = "Henüz antrenman kaydın yok.\nKütüphane sekmesinden bir egzersiz seç ve set kaydet."
                textSize = 14f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(8), dp(30), dp(8), dp(8))
            })
            icerik.removeAllViews()
            icerik.addView(sar)
            return
        }

        kayitlar.forEachIndexed { index, k ->
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
            }
            kart.addView(TextView(this).apply {
                text = "${FitnessMotor.kasEmoji(k.kasKod)} ${k.egzersizAdi}"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            val setOzet = k.setler.joinToString(", ") { s ->
                "${s.tekrar}×${if (s.agirlik > 0) "${s.agirlik}kg" else "kendi"}"
            }
            kart.addView(TextView(this).apply {
                text = "Setler: $setOzet"
                textSize = 13f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            kart.addView(TextView(this).apply {
                text = FitnessMotor.gunAnahtari(k.tarih)
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            // Silme
            kart.isClickable = true
            kart.isFocusable = true
            val tip = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, tip, true)
            kart.setBackgroundResource(tip.resourceId)
            kart.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Kaydı sil?")
                    .setMessage("${k.egzersizAdi} kaydı silinsin mi?")
                    .setPositiveButton("Sil") { _, _ ->
                        FitnessMotor.antrenmanSil(this, kayitlar.size - 1 - index)
                        antrenmanCiz()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            ic.addView(kart)
        }

        icerik.removeAllViews()
        icerik.addView(sar)
    }
}
