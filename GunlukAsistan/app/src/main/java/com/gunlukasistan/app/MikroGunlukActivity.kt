package com.gunlukasistan.app

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors

/**
 * v10.14 · ULTRA-30 / E27 — "3 kısa soru" ekranı.
 *
 * İyi geceler bildirimindeki ✍ düğmesinden (veya Analitik'teki duygu
 * haritasından) açılır. Varsayılan puan 3 (😐) seçili gelir; iki tek
 * satırlık giriş isteğe bağlıdır — boş bırakmak sayılır, zorlamak değil.
 */
class MikroGunlukActivity : AppCompatActivity() {

    private val yogunluk get() = resources.displayMetrics.density
    private var seciliPuan = 3
    private lateinit var puanSatiri: LinearLayout
    private lateinit var tesekkurGiris: EditText
    private lateinit var yarinGiris: EditText

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * yogunluk).toInt(), (18 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (30 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@MikroGunlukActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )

        kap.addView(baslik(getString(R.string.ge_gunluk_baslik), 20f))
        kap.addView(bilgi(getString(R.string.ge_gunluk_aciklama)))

        // ── 1) Gün puanı ──
        kap.addView(baslik(getString(R.string.ge_gunluk_puan), 14.5f))
        puanSatiri = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        kap.addView(puanSatiri)
        puanSatiriCiz()

        // ── 2) Teşekkür ──
        kap.addView(baslik(getString(R.string.ge_gunluk_tesekkur), 14.5f))
        tesekkurGiris = giris(getString(R.string.ge_gunluk_tesekkur_ipucu))
        kap.addView(tesekkurGiris)

        // ── 3) Yarının tek şeyi ──
        kap.addView(baslik(getString(R.string.ge_gunluk_yarin), 14.5f))
        yarinGiris = giris(getString(R.string.ge_gunluk_yarin_ipucu))
        kap.addView(yarinGiris)

        // Bugünün eski kaydı varsa üstüne değil, doldurmuş göster:
        val bugun = UykuCerceve.gunKey(System.currentTimeMillis())
        MikroGunluk.gunluk(this, bugun)?.let { eski ->
            seciliPuan = eski.puan.coerceIn(1, 5)
            tesekkurGiris.setText(eski.tesekkur)
            yarinGiris.setText(eski.yarinTekSey)
            puanSatiriCiz()
        }

        kap.addView(
            dugme(getString(R.string.ge_gunluk_kaydet)) {
                kaydetVeKapat(bugun)
            }
        )
    }

    private fun puanSatiriCiz() {
        puanSatiri.removeAllViews()
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        (1..5).forEach { puan ->
            val secili = puan == seciliPuan
            puanSatiri.addView(
                TextView(this).apply {
                    text = MikroGunluk.emojiFor(puan)
                    textSize = if (secili) 30f else 24f
                    alpha = if (secili) 1f else 0.45f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        0, (54 * yogunluk).toInt(), 1f
                    )
                    if (secili) {
                        background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = 12 * yogunluk
                            setStroke((1.5f * yogunluk).toInt(), vurgu)
                            setColor((vurgu and 0x00FFFFFF) or 0x22000000)
                        }
                    }
                    isClickable = true
                    setOnClickListener {
                        seciliPuan = puan
                        puanSatiriCiz()
                    }
                }
            )
        }
    }

    private fun kaydetVeKapat(gunKey: String) {
        MikroGunluk.kaydet(
            this, gunKey,
            MikroGunluk.Gunluk(
                seciliPuan,
                tesekkurGiris.text?.toString()?.trim().orEmpty(),
                yarinGiris.text?.toString()?.trim().orEmpty()
            )
        )
        Toast.makeText(
            this, R.string.ge_gunluk_kaydedildi, Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    // ---------------- Arayüz yardımcıları ----------------

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (10 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12.5f
        alpha = 0.7f
        setLineSpacing(0f, 1.25f)
        setPadding(0, 0, 0, (6 * yogunluk).toInt())
    }

    private fun giris(ipucu: String) = EditText(this).apply {
        hint = ipucu
        isSingleLine = true
        maxLines = 1
        textSize = 15f
        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        setBackgroundColor(
            (MaterialColors.getColor(
                this@MikroGunlukActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x14000000
        )
        setPadding(
            (14 * yogunluk).toInt(), 0, (14 * yogunluk).toInt(), 0
        )
    }

    private fun dugme(metin: String, tikla: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 15f
        gravity = Gravity.CENTER
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        val vurgu = MaterialColors.getColor(
            this@MikroGunlukActivity,
            com.google.android.material.R.attr.colorPrimary, 0
        )
        setTextColor(
            MaterialColors.getColor(
                this@MikroGunlukActivity,
                com.google.android.material.R.attr.colorOnPrimary, 0
            )
        )
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 14 * yogunluk
            setColor(vurgu)
        }
        setPadding(0, (14 * yogunluk).toInt(), 0, (14 * yogunluk).toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (18 * yogunluk).toInt() }
        isClickable = true
        setOnClickListener { tikla() }
    }
}
