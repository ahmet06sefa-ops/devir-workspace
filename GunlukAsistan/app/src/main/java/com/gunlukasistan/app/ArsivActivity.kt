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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v7.75 — Arşiv ekranı.
 *
 * ── Kullanıcının isteği (10 iyileştirmeden 7. madde) ──
 * "Tamamlananları arşive taşı — liste temiz kalsın, geçmiş kaybolmasın.
 *  'Bu ay 47 iş bitirdin.'"
 *
 * ── Neden arşiv ──
 * Biten görevler ana listede birikiyordu. Silmek istatistiği bozuyor,
 * tutmak listeyi şişiriyordu. Arşiv ikisinin arasını buluyor: görev
 * listeden çıkıyor ama veri duruyor.
 *
 * Arşivdeki göreve dokununca geri getirilir; uzun basınca değil —
 * kazara geri getirme silmekten zararsız olduğu için tek dokunuş yeterli.
 */
class ArsivActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, ArsivActivity::class.java))
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
                (16 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (16 * yogunluk).toInt(), (28 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@ArsivActivity,
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
        val liste = Store.arsivGorevleri(this)

        // Başlık + sayaç
        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@ArsivActivity).apply {
                    text = getString(R.string.ars_baslik)
                    textSize = 20f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                addView(TextView(this@ArsivActivity).apply {
                    text = getString(R.string.ars_sayac, liste.size)
                    textSize = 12f
                    alpha = 0.7f
                })
            }
        )

        // Bu ayki başarı özeti — arşivin asıl değeri bu
        val buAy = Store.buAyBitenGorev(this)
        if (buAy > 0) {
            kap.addView(bilgi(getString(R.string.ars_ozet, buAy)))
        }

        if (liste.isEmpty()) {
            kap.addView(bilgi(getString(R.string.ars_bos)))
            return
        }

        kap.addView(ayirici())
        liste.forEach { kap.addView(kart(it)) }

        kap.addView(ayirici())
        kap.addView(
            dugme(getString(R.string.ars_sil_tumu)) {
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.ars_sil_sor, liste.size))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        Store.arsiviTemizle(this)
                        Toast.makeText(this, R.string.ars_silindi, Toast.LENGTH_SHORT).show()
                        ciz()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
    }

    private fun kart(g: Store.Task): View {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { geriGetir(g) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }

        // Etiket şeridi
        if (g.etiket.isNotBlank()) {
            ic.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (4 * yogunluk).toInt(), (30 * yogunluk).toInt()
                ).apply { marginEnd = (8 * yogunluk).toInt() }
                setBackgroundColor(Etiket.renk(g.etiket))
            })
        }

        ic.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@ArsivActivity).apply {
                    text = g.text
                    textSize = 13.5f
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                addView(TextView(this@ArsivActivity).apply {
                    text = Tekrar.tarihMetni(g.arsivZaman)
                    textSize = 11f
                    alpha = 0.65f
                    setPadding(0, (3 * yogunluk).toInt(), 0, 0)
                })
            }
        )
        ic.addView(TextView(this).apply {
            text = getString(R.string.ars_geri)
            textSize = 11.5f
            setTextColor(
                MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        kart.addView(ic)
        return kart
    }

    private fun geriGetir(g: Store.Task) {
        Store.arsiveTasi(this, g.id, false)
        Toast.makeText(this, R.string.ars_geri_ok, Toast.LENGTH_SHORT).show()
        ciz()
    }

    // ── Arayüz yardımcıları ──

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12.5f
        alpha = 0.75f
        setLineSpacing(0f, 1.2f)
        setPadding(0, (10 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (10 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@ArsivActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun dugme(m: String, tikla: () -> Unit) = TextView(this).apply {
        text = m
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@ArsivActivity, com.google.android.material.R.attr.colorError, 0
            )
        )
        setPadding(0, (12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tikla() }
    }
}
