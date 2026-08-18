package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.57 — Arka plan kontrol ayarları.
 *
 * Kullanıcı buradan kontrolü açar, sıklığı seçer ve nelerin
 * bildirileceğini belirler. "Şimdi kontrol et" ile anında deneyebilir.
 */
class OnlineBekciActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, OnlineBekciActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

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
                        this@OnlineBekciActivity,
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

        kap.addView(baslik(getString(R.string.ob_baslik), 20f))
        kap.addView(bilgi(getString(R.string.ob_aciklama)))

        // Odaya bağlı değilse hiçbir şey yapılamaz
        if (!OnlineStore.bagliMi(this)) {
            kap.addView(bilgi(getString(R.string.ob_bagli_degil)))
            kap.addView(dugme(getString(R.string.on_title)) {
                OnlineActivity.ac(this)
                finish()
            })
            return
        }

        // ── Ana anahtar ──
        val acik = OnlineBekci.acikMi(this)
        kap.addView(
            anahtar(getString(R.string.ob_acik), getString(R.string.ob_acik_d), acik) { v ->
                OnlineBekci.setAcik(this, v)
                ciz()
            }
        )

        if (!acik) {
            kap.addView(bilgi(getString(R.string.ob_kapali_bilgi)))
            return
        }

        kap.addView(ayirici())

        // ── Sıklık ──
        val dk = OnlineBekci.siklikDk(this)
        kap.addView(baslik(getString(R.string.ob_siklik), 15f))
        kap.addView(dugme(siklikAdi(dk)) { siklikSec() })

        kap.addView(ayirici())

        // ── Neler bildirilsin ──
        kap.addView(baslik(getString(R.string.ob_neler), 15f))
        kap.addView(
            anahtar(getString(R.string.ob_n_mesaj), "", OnlineBekci.bilMesaj(this)) { v ->
                OnlineBekci.setBayrak(this, "b_mesaj", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.ob_n_gorev), "", OnlineBekci.bilGorev(this)) { v ->
                OnlineBekci.setBayrak(this, "b_gorev", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.ob_n_not), "", OnlineBekci.bilNot(this)) { v ->
                OnlineBekci.setBayrak(this, "b_not", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.ob_n_konu), "", OnlineBekci.bilKonu(this)) { v ->
                OnlineBekci.setBayrak(this, "b_konu", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.ob_n_tamam), "", OnlineBekci.bilTamam(this)) { v ->
                OnlineBekci.setBayrak(this, "b_tamam", v)
            }
        )

        kap.addView(ayirici())

        kap.addView(
            anahtar(
                getString(R.string.ob_sadece_karsi),
                getString(R.string.ob_sadece_karsi_d),
                OnlineBekci.sadeceKarsi(this)
            ) { v -> OnlineBekci.setBayrak(this, "sadece_karsi", v) }
        )

        kap.addView(ayirici())

        // ── Pil optimizasyonu ──
        if (!OnlineBekci.pilMuafMi(this)) {
            kap.addView(baslik(getString(R.string.ob_pil_baslik), 15f))
            kap.addView(bilgi(getString(R.string.ob_pil_govde)))
            kap.addView(dugme(getString(R.string.ob_pil_ac)) {
                OnlineBekci.pilAyariniAc(this)
            })
            kap.addView(ayirici())
        }

        // ── Son kontrol + elle kontrol ──
        val son = OnlineBekci.sonKontrol(this)
        kap.addView(
            bilgi(
                if (son <= 0) getString(R.string.ob_hic)
                else getString(R.string.ob_son_kontrol, OnlineStore.zamanMetni(son))
            )
        )
        kap.addView(dugme(getString(R.string.ob_test)) { elleKontrol() })
    }

    private fun siklikAdi(dk: Int): String = when (dk) {
        15 -> getString(R.string.ob_s_15)
        60 -> getString(R.string.ob_s_60)
        180 -> getString(R.string.ob_s_180)
        else -> getString(R.string.ob_s_30)
    }

    private fun siklikSec() {
        val degerler = listOf(15, 30, 60, 180)
        val adlar = degerler.map { siklikAdi(it) }.toTypedArray()
        val simdiki = degerler.indexOf(OnlineBekci.siklikDk(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ob_siklik)
            .setSingleChoiceItems(adlar, simdiki) { d, hangi ->
                OnlineBekci.setSiklikDk(this, degerler[hangi])
                d.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** "Şimdi kontrol et" — ağ işlemi arka planda. */
    private fun elleKontrol() {
        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.ob_kontrol_ediliyor)
            .setCancelable(false)
            .show()
        Thread {
            val adet = try {
                OnlineBekci.kontrolEt(this, elle = true)
            } catch (e: Exception) {
                android.util.Log.w("OnlineBekciAyar", "Kontrol başarısız", e)
                0
            }
            runOnUiThread {
                try {
                    bekle.dismiss()
                } catch (_: Exception) {
                }
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (adet == 0) {
                    Toast.makeText(this, R.string.ob_yeni_yok, Toast.LENGTH_SHORT).show()
                }
                ciz()
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (14 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12f
        alpha = 0.7f
        setLineSpacing(0f, 1.2f)
        setPadding(0, 0, 0, (8 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (4 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@OnlineBekciActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun anahtar(
        ad: String,
        aciklama: String,
        acik: Boolean,
        degisince: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (7 * yogunluk).toInt(), 0, (7 * yogunluk).toInt())
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@OnlineBekciActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@OnlineBekciActivity).apply {
                        text = aciklama
                        textSize = 11.5f
                        alpha = 0.7f
                    })
                }
            }
        )
        satir.addView(
            MaterialSwitch(this).apply {
                isChecked = acik
                setOnCheckedChangeListener { _, v -> degisince(v) }
            }
        )
        return satir
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@OnlineBekciActivity,
                com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tiklayinca() }
    }
}
