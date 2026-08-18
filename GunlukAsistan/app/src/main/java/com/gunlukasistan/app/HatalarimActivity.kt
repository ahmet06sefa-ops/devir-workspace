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
 * v7.83 — Hata defteri ekranı.
 *
 * ── Neden bu ekran ──
 * Quiz sonucu "8/10" der ve geçer. Hangi 2 soruyu yanlış yaptığın kaybolur.
 * Oysa öğrenmenin asıl değeri orada: doğru bildiğini tekrar çözmek zaman
 * kaybı, yanlış bildiğini tekrar çözmek öğrenmenin kendisi.
 *
 * Bu ekran [Hatalarim] deposunu gösterir: bugün tekrar edilecekler,
 * en çok yanlış yapılanlar ve konu bazlı zayıflık dağılımı.
 */
class HatalarimActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, HatalarimActivity::class.java))
        }
    }

    private val d get() = resources.displayMetrics.density
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
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (28 * d).toInt())
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@HatalarimActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    override fun onResume() {
        super.onResume()
        ciz()
    }

    private fun ciz() {
        kap.removeAllViews()
        val ozet = Hatalarim.ozet(this)

        kap.addView(TextView(this).apply {
            text = getString(R.string.ht_baslik)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        kap.addView(bilgi(getString(R.string.ht_aciklama)))

        // ── Özet kartı ─────────────────────────────────────────────
        ayirici()
        kap.addView(ozetKarti(ozet))

        if (ozet.toplam == 0) {
            kap.addView(bilgi(getString(R.string.ht_bos)))
            return
        }

        // ── Tekrar düğmesi ─────────────────────────────────────────
        kap.addView(
            dugme(
                if (ozet.bugun > 0) getString(R.string.ht_tekrar_et, ozet.bugun)
                else getString(R.string.ht_yine_calis),
                vurgulu = true
            ) {
                QuizActivity.acHatalar(this, getString(R.string.ht_baslik))
            }
        )

        // ── Konu dağılımı ──────────────────────────────────────────
        val dagilim = Hatalarim.kaynakDagilimi(this)
        if (dagilim.size > 1) {
            ayirici()
            kap.addView(baslikKucuk(getString(R.string.ht_zayif_konular)))
            dagilim.take(6).forEach { (kaynak, adet) ->
                kap.addView(dagilimSatiri(kaynak, adet, dagilim.first().second))
            }
        }

        // ── Soru listesi ───────────────────────────────────────────
        ayirici()
        kap.addView(baslikKucuk(getString(R.string.ht_sorular)))
        Hatalarim.hepsi(this)
            .sortedByDescending { it.yanlisSayisi }
            .take(40)
            .forEach { kap.addView(soruKarti(it)) }

        // ── Bakım ──────────────────────────────────────────────────
        ayirici()
        kap.addView(silDugmesi(getString(R.string.ht_temizle)) {
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.ht_temizle_sor, ozet.toplam))
                .setPositiveButton(R.string.delete) { _, _ ->
                    Hatalarim.temizle(this)
                    ciz()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        })
    }

    // ── Kartlar ────────────────────────────────────────────────────

    private fun ozetKarti(o: Hatalarim.Ozet): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((14 * d).toInt(), (16 * d).toInt(), (14 * d).toInt(), (16 * d).toInt())
        }
        fun kutu(sayi: Int, etiket: String, vurgu: Boolean) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@HatalarimActivity).apply {
                text = sayi.toString()
                textSize = 22f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (vurgu) setTextColor(
                    MaterialColors.getColor(
                        this@HatalarimActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
            addView(TextView(this@HatalarimActivity).apply {
                text = etiket
                textSize = 11f
                alpha = 0.75f
                gravity = Gravity.CENTER
                setPadding(0, (3 * d).toInt(), 0, 0)
            })
        }
        ic.addView(kutu(o.bugun, getString(R.string.ht_k_bugun), true))
        ic.addView(kutu(o.toplam, getString(R.string.ht_k_toplam), false))
        ic.addView(kutu(o.ogrenilen, getString(R.string.ht_k_ogrenilen), false))
        return kartSar(ic)
    }

    private fun dagilimSatiri(kaynak: String, adet: Int, enYuksek: Int): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = kaynak
            textSize = 13.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        ic.addView(android.widget.ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = enYuksek.coerceAtLeast(1)
            progress = adet
            layoutParams = LinearLayout.LayoutParams((90 * d).toInt(), (6 * d).toInt())
                .apply { rightMargin = (10 * d).toInt() }
        })
        ic.addView(TextView(this).apply {
            text = adet.toString()
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        return kartSar(ic)
    }

    private fun soruKarti(h: Hatalarim.Hata): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = h.metin
            textSize = 13.5f
            setLineSpacing(0f, 1.25f)
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        ic.addView(TextView(this).apply {
            text = buildString {
                append(getString(R.string.ht_yanlis_kez, h.yanlisSayisi))
                if (h.kaynak.isNotBlank()) append("  ·  ").append(h.kaynak.take(24))
                if (h.sonrakiGun.isNotBlank()) {
                    append("  ·  ")
                    append(getString(R.string.ht_sonraki, Hatalarim.tarihMetni(h.sonrakiGun)))
                }
            }
            textSize = 11f
            alpha = 0.7f
            setPadding(0, (6 * d).toInt(), 0, 0)
        })
        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { soruDetay(h) }
        }
    }

    /** Soruyu, doğru cevabı ve açıklamasını gösterir. */
    private fun soruDetay(h: Hatalarim.Hata) {
        val govde = buildString {
            append(h.metin).append("\n\n")
            h.siklar.forEachIndexed { i, sik ->
                append(if (i == h.dogru) "✓ " else "   ").append(sik).append("\n")
            }
            if (h.aciklama.isNotBlank()) {
                append("\n").append(h.aciklama)
            }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.ht_yanlis_kez, h.yanlisSayisi))
            .setMessage(govde)
            .setPositiveButton(R.string.hb_benzer_uret) { _, _ -> benzerUret(h) }
            .setNegativeButton(R.string.ht_listeden_cikar) { _, _ ->
                Hatalarim.sil(this, h.soruId)
                ciz()
                // v8.0: yanlışlıkla çıkarılan soru geri gelsin (öneri 6)
                GeriAl.sun(kap, getString(R.string.ht_cikarildi), tazele = { ciz() }) {
                    Hatalarim.yanlisEkle(this, h.soruya(), h.kaynak)
                }
            }
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    /**
     * v7.84 — Aynı kavramı farklı biçimde soran yeni sorular üretir.
     *
     * Aynı soruyu tekrar çözmek ezberi ödüllendirir: kullanıcı kavramı
     * değil "C şıkkı" cevabını hatırlar. Farklı sorulmuş soru gerçekten
     * öğrenilip öğrenilmediğini ölçer.
     */
    private fun benzerUret(h: Hatalarim.Hata) {
        if (!AiSettings.isReady(this)) {
            Toast.makeText(this, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }

        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.hb_uretiliyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val sonuc = Hatalarim.benzerUret(this, h, adet = 3)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }

                if (!sonuc.ok || sonuc.sorular.isEmpty()) {
                    Toast.makeText(
                        this,
                        sonuc.hata.ifBlank { getString(R.string.quiz_err_parse) },
                        Toast.LENGTH_LONG
                    ).show()
                    return@anaIs
                }

                Hatalarim.geciciAyarla(sonuc.sorular, getString(R.string.hb_baslik))
                QuizActivity.acGecici(this, getString(R.string.hb_baslik))
            }
        }
    }

    // ── Arayüz yardımcıları ────────────────────────────────────────

    private fun baslikKucuk(m: String) = TextView(this).apply {
        text = m
        textSize = 13f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        alpha = 0.72f
        setPadding(0, (4 * d).toInt(), 0, (8 * d).toInt())
    }

    private fun kartSar(ic: View): View = MaterialCardView(this).apply {
        radius = 14 * d
        cardElevation = 0f
        strokeWidth = (1 * d).toInt()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (8 * d).toInt() }
        addView(ic)
    }

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12.5f
        alpha = 0.75f
        setLineSpacing(0f, 1.25f)
        setPadding(0, (4 * d).toInt(), 0, (6 * d).toInt())
    }

    private fun ayirici() {
        kap.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
            ).apply {
                topMargin = (14 * d).toInt()
                bottomMargin = (8 * d).toInt()
            }
            setBackgroundColor(
                (MaterialColors.getColor(
                    this@HatalarimActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun dugme(m: String, vurgulu: Boolean = false, tikla: () -> Unit) =
        TextView(this).apply {
            text = m
            textSize = 14.5f
            gravity = Gravity.CENTER
            setTypeface(
                typeface,
                if (vurgulu) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
            setTextColor(
                MaterialColors.getColor(
                    this@HatalarimActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            setPadding(0, (14 * d).toInt(), 0, (14 * d).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { tikla() }
        }

    private fun silDugmesi(m: String, tikla: () -> Unit) = TextView(this).apply {
        text = m
        textSize = 13.5f
        gravity = Gravity.CENTER
        setTextColor(
            MaterialColors.getColor(
                this@HatalarimActivity, com.google.android.material.R.attr.colorError, 0
            )
        )
        setPadding(0, (13 * d).toInt(), 0, (13 * d).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tikla() }
    }
}
