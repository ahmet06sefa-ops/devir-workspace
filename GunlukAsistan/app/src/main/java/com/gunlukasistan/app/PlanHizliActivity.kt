package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v7.69 — Widget'tan açılan hızlı işlem paneli.
 *
 * ── Kullanıcının isteği ──
 * "Namaz plan widgeti tıklamalı olsun, ordan bütün her şeyi yapabileyim"
 *
 * ── Neden ayrı bir ekran ──
 * Widget'ın kendisi `RemoteViews` ile çizilir; metin girişi, açılır menü
 * veya kaydırma çubuğu barındıramaz. Bu yüzden widget'a dokunulduğunda
 * uygulamanın tamamını açmak yerine **şeffaf, yarı ekran bir panel**
 * açılıyor (`Theme.QuickAdd`). Panel kapanınca kullanıcı ana ekranına
 * geri döner — uygulamaya girip çıkmış hissetmez.
 *
 * ── Panelde yapılabilenler ──
 * · Yeni iş ekleme (metin + süre çipleri + dilim seçimi)
 * · Dilimdeki işleri görme, dokunup tamamlama / geri alma
 * · Uzun basınca silme
 * · Bitenleri toplu temizleme
 * · Sıradaki vakit ve bugünün tüm vakitlerini görme
 * · Sayaç ekranına atlama
 * · Tam Plan sekmesine geçiş
 *
 * Her işlemden sonra widget anında tazelenir.
 */
class PlanHizliActivity : AppCompatActivity() {

    companion object {
        /** Hangi dilim açılsın (anahtar). Boşsa o an aktif olan dilim. */
        const val EXTRA_DILIM = "ph_dilim"

        /** Açılır açılmaz metin kutusuna odaklan. */
        const val EXTRA_EKLE = "ph_ekle"

        fun ac(context: Context, dilim: String = "", ekle: Boolean = false) {
            context.startActivity(
                Intent(context, PlanHizliActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_DILIM, dilim)
                    putExtra(EXTRA_EKLE, ekle)
                }
            )
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout
    private lateinit var girdi: EditText

    private var dilim: NamazPlan.Dilim = NamazPlan.Dilim.SABAH
    private var secilenSure = 0

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

        dilim = cozDilim(intent?.getStringExtra(EXTRA_DILIM))

        // Panel: alttan yükselen kart görünümü
        val dis = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setOnClickListener { finish() }   // dışına dokun → kapat
        }

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * yogunluk).toInt(), (18 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (20 * yogunluk).toInt()
            )
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    26 * yogunluk, 26 * yogunluk, 26 * yogunluk, 26 * yogunluk,
                    0f, 0f, 0f, 0f
                )
                setColor(
                    MaterialColors.getColor(
                        this@PlanHizliActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
            }
            // Panelin içine dokunmak kapatmasın
            isClickable = true
        }

        dis.addView(
            ScrollView(this).apply {
                isFillViewport = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(kap)
            }
        )
        setContentView(dis)

        ciz()

        if (intent?.getBooleanExtra(EXTRA_EKLE, false) == true) {
            girdi.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        ciz()
    }

    private fun cozDilim(anahtar: String?): NamazPlan.Dilim {
        if (!anahtar.isNullOrBlank()) {
            NamazPlan.Dilim.entries.firstOrNull { it.anahtar == anahtar }?.let { return it }
        }
        return try {
            val gun = NamazVakti.bugunDuzeltilmis(this)
            NamazPlan.aktifDilim(gun, NamazVakti.simdiDakika())
        } catch (e: Exception) {
            android.util.Log.w("PlanHizli", "Dilim bulunamadı", e)
            NamazPlan.Dilim.SABAH
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun ciz() {
        kap.removeAllViews()

        val gun = try {
            NamazVakti.bugunDuzeltilmis(this)
        } catch (e: Exception) {
            android.util.Log.w("PlanHizli", "Vakitler okunamadı", e)
            null
        }
        val simdi = NamazVakti.simdiDakika()

        // ── Başlık: dilim + saat aralığı ──
        kap.addView(TextView(this).apply {
            text = getString(
                R.string.ph_baslik,
                dilim.emoji + " " + getString(dilim.adRes),
                if (gun != null) gun.saat(dilim.baslangic) + "–" + gun.saat(dilim.bitis) else ""
            )
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        // ── Sıradaki vakit ──
        if (gun != null) {
            val (sonraki, kalan) = gun.sonraki(simdi)
            kap.addView(TextView(this).apply {
                text = getString(
                    R.string.ph_sonraki_vakit,
                    getString(sonraki.adRes), gun.saat(sonraki),
                    NamazPlan.sureMetni(kalan)
                )
                textSize = 12f
                alpha = 0.75f
                setPadding(0, (4 * yogunluk).toInt(), 0, 0)
            })
        }

        // ── Özet ──
        val isler = NamazPlan.dilimIsleri(this, dilim)
        val biten = isler.count { it.tamamlandi }
        val planli = NamazPlan.dilimPlanliSure(this, dilim)
        kap.addView(TextView(this).apply {
            text = getString(
                R.string.ph_ozet, biten, isler.size, NamazPlan.sureMetni(planli)
            )
            textSize = 11.5f
            alpha = 0.7f
            setPadding(0, (2 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        })

        // ── Hızlı ekleme kutusu ──
        girdi = EditText(this).apply {
            hint = getString(R.string.ph_ne)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 2
            setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
            setOnEditorActionListener { _, _, _ -> isEkle(); true }
        }
        kap.addView(girdi)

        // Süre çipleri
        val sureSatir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val sureler = listOf(0, 15, 25, 45, 60)
        val cipler = mutableListOf<TextView>()
        sureler.forEach { dk ->
            val c = cip(
                if (dk == 0) getString(R.string.wt_s_yok) else getString(R.string.pe_dk, dk),
                dk == secilenSure
            ) {
                secilenSure = dk
                cipler.forEachIndexed { i, tv -> cipBoya(tv, sureler[i] == dk) }
            }
            cipler.add(c)
            sureSatir.addView(c)
        }
        kap.addView(
            android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(sureSatir)
            }
        )

        // Dilim seçimi
        kap.addView(etiket(getString(R.string.ph_dilim_sec)))
        val dilimSatir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dilimCipler = mutableListOf<TextView>()
        NamazPlan.Dilim.entries.forEach { d ->
            val c = cip(d.emoji + " " + getString(d.adRes), d == dilim) {
                dilim = d
                ciz()
            }
            dilimCipler.add(c)
            dilimSatir.addView(c)
        }
        kap.addView(
            android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(dilimSatir)
            }
        )

        // Ekle düğmesi
        kap.addView(
            dugme("＋ " + getString(R.string.ph_ekle), vurgulu = true) { isEkle() }
        )

        kap.addView(ayirici())

        // ── İş listesi ──
        kap.addView(etiket(getString(R.string.ph_isler)))
        if (isler.isEmpty()) {
            kap.addView(TextView(this).apply {
                text = getString(R.string.ph_bos)
                textSize = 12.5f
                alpha = 0.6f
                setPadding(0, (6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
            })
        } else {
            isler.forEach { kap.addView(isSatiri(it)) }
            if (biten > 0) {
                kap.addView(
                    dugme(getString(R.string.ph_bitenleri_sil)) {
                        val adet = NamazPlan.bitenleriTemizle(this, dilim)
                        if (adet > 0) {
                            Toast.makeText(
                                this, getString(R.string.pe_biten_silindi, adet),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        tazele()
                    }
                )
            }
        }

        kap.addView(ayirici())

        // ── Bugünün vakitleri ──
        if (gun != null) {
            kap.addView(etiket(getString(R.string.ph_vakitler)))
            val aktifVakit = gun.aktifVakit(simdi)
            NamazVakti.Vakit.entries.forEach { v ->
                val bu = v == aktifVakit
                kap.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, (5 * yogunluk).toInt(), 0, (5 * yogunluk).toInt())
                        addView(TextView(this@PlanHizliActivity).apply {
                            text = v.emoji + "  " + getString(v.adRes)
                            textSize = 13f
                            if (bu) setTypeface(typeface, android.graphics.Typeface.BOLD)
                            layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                            )
                        })
                        addView(TextView(this@PlanHizliActivity).apply {
                            text = gun.saat(v)
                            textSize = 13f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            if (bu) setTextColor(
                                MaterialColors.getColor(
                                    this, com.google.android.material.R.attr.colorPrimary, 0
                                )
                            )
                        })
                    }
                )
            }
        }

        kap.addView(ayirici())

        // ── Alt eylemler ──
        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    dugme(getString(R.string.ph_sayac)) {
                        acVeKapat(WidgetCommon.SCREEN_TIMER)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
                addView(
                    dugme(getString(R.string.ph_tumu)) {
                        acVeKapat(WidgetCommon.SCREEN_PLAN)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
            }
        )
        kap.addView(dugme(getString(R.string.ph_kapat)) { finish() })
    }

    /** Bir iş satırı: dokun → tamamla, uzun bas → sil. */
    private fun isSatiri(i: NamazPlan.Is): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (9 * yogunluk).toInt(), 0, (9 * yogunluk).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener {
                val yeni = NamazPlan.isTamamla(this@PlanHizliActivity, i.id)
                Toast.makeText(
                    this@PlanHizliActivity,
                    getString(
                        if (yeni) R.string.ph_tamamlandi else R.string.ph_geri_alindi,
                        i.metin
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                tazele()
            }
            setOnLongClickListener { silSor(i); true }
        }
        satir.addView(TextView(this).apply {
            text = if (i.tamamlandi) "☑" else "☐"
            textSize = 17f
            setPadding(0, 0, (10 * yogunluk).toInt(), 0)
            if (i.tamamlandi) setTextColor(GrafikDili.BASARI)
        })
        satir.addView(TextView(this).apply {
            text = buildString {
                if (i.oncelikSimgesi.isNotBlank()) append(i.oncelikSimgesi).append(" ")
                append(i.metin)
            }
            textSize = 13.5f
            if (i.tamamlandi) {
                paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                alpha = 0.55f
            }
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        if (i.sureDk > 0) {
            satir.addView(TextView(this).apply {
                text = getString(R.string.pe_dk, i.sureDk)
                textSize = 11f
                alpha = 0.75f
                setTextColor(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        return satir
    }

    // ═══════════════════════════════════════════════════════════════
    // İŞLEMLER
    // ═══════════════════════════════════════════════════════════════

    private fun isEkle() {
        val metin = girdi.text?.toString()?.trim().orEmpty()
        if (metin.isBlank()) {
            Toast.makeText(this, R.string.pe_bos_uyari, Toast.LENGTH_SHORT).show()
            return
        }
        NamazPlan.isEkle(this, dilim, metin, secilenSure, 1, false)
        girdi.setText("")
        secilenSure = 0
        Toast.makeText(this, R.string.ph_eklendi, Toast.LENGTH_SHORT).show()
        tazele()
    }

    private fun silSor(i: NamazPlan.Is) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.ph_sil_sor, i.metin))
            .setPositiveButton(R.string.delete) { _, _ ->
                NamazPlan.isSil(this, i.id)
                Toast.makeText(this, R.string.ph_silindi, Toast.LENGTH_SHORT).show()
                tazele()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Veri değişti: ekranı ve widget'ları tazele. */
    private fun tazele() {
        ciz()
        try {
            WidgetCommon.refreshAll(this, true)
        } catch (e: Exception) {
            android.util.Log.w("PlanHizli", "Widget tazelenemedi", e)
        }
    }

    private fun acVeKapat(ekran: Int) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, ekran)
            }
        )
        finish()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun etiket(metin: String) = TextView(this).apply {
        text = metin
        textSize = 11.5f
        alpha = 0.7f
        setPadding(0, (12 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (2 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@PlanHizliActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun cip(metin: String, secili: Boolean, tikla: () -> Unit) =
        TextView(this).apply {
            text = metin
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(
                (14 * yogunluk).toInt(), (8 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (8 * yogunluk).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
            cipBoya(this, secili)
        }

    private fun cipBoya(tv: TextView, secili: Boolean) {
        try {
            val vurgu = MaterialColors.getColor(
                tv, com.google.android.material.R.attr.colorPrimary, 0
            )
            tv.background = GradientDrawable().apply {
                cornerRadius = 18 * yogunluk
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * yogunluk).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            if (secili) tv.setTextColor(vurgu)
            else tv.setTextColor(
                MaterialColors.getColor(
                    tv, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("PlanHizli", "Çip boyanamadı", e)
        }
    }

    private fun dugme(
        metin: String,
        vurgulu: Boolean = false,
        tikla: () -> Unit
    ) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        gravity = Gravity.CENTER
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        setTextColor(vurgu)
        setPadding(0, (12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt())
        if (vurgulu) {
            background = GradientDrawable().apply {
                cornerRadius = 18 * yogunluk
                setColor((vurgu and 0x00FFFFFF) or 0x22000000)
            }
        } else {
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
        }
        isClickable = true
        setOnClickListener { tikla() }
    }
}
