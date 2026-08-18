package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v7.84 — Terim sözlüğü ekranı.
 *
 * Anlatım okurken "Ne demek?" ile sorulan terimler burada birikir.
 * Kullanıcı kendi öğrenme sözlüğünü görüyor: neyi kaç kez sorduğunu,
 * hangi konuda kaç terim biriktiğini.
 *
 * ── Neden arama kutusu üstte ──
 * Sözlük büyüdükçe (500 terime kadar) kaydırarak aramak imkânsız hâle
 * gelir. Arama kutusu ilk elemandır; klavye açılmadan da liste görünür.
 */
class SozlukActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, SozlukActivity::class.java))
        }
    }

    private val d get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout
    private lateinit var listeKap: LinearLayout
    private var sorgu = ""
    private var sadeceYildiz = false

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
                        this@SozlukActivity,
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
        listeyiTazele()
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(TextView(this).apply {
            text = getString(R.string.sz_baslik)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val toplam = Sozluk.sayi(this)
        kap.addView(bilgi(
            if (toplam == 0) getString(R.string.sz_aciklama)
            else getString(R.string.sz_sayac, toplam)
        ))

        if (toplam == 0) {
            kap.addView(bilgi(getString(R.string.sz_bos_ipucu)))
            return
        }

        // ── Arama ──────────────────────────────────────────────────
        kap.addView(EditText(this).apply {
            hint = getString(R.string.sz_ara)
            setSingleLine(true)
            textSize = 14f
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    sorgu = s?.toString().orEmpty()
                    listeyiTazele()
                }
            })
        })

        // ── Süzgeç ─────────────────────────────────────────────────
        val yildizliSayi = Sozluk.yildizlilar(this).size
        if (yildizliSayi > 0) {
            kap.addView(TextView(this).apply {
                text = if (sadeceYildiz) getString(R.string.sz_tumunu_goster)
                else getString(R.string.sz_yildizlilar, yildizliSayi)
                textSize = 13f
                setPadding(0, (10 * d).toInt(), 0, (6 * d).toInt())
                setTextColor(
                    MaterialColors.getColor(
                        this@SozlukActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
                isClickable = true
                setOnClickListener {
                    sadeceYildiz = !sadeceYildiz
                    ciz()
                }
            })
        }

        // ── En çok bakılanlar ──────────────────────────────────────
        val zorlar = Sozluk.enCokBakilanlar(this, 5)
        if (zorlar.isNotEmpty() && sorgu.isBlank() && !sadeceYildiz) {
            ayirici()
            kap.addView(baslikKucuk(getString(R.string.sz_en_cok)))
            kap.addView(bilgi(getString(R.string.sz_en_cok_alt)))
            zorlar.forEach { kap.addView(terimKarti(it, kisa = true)) }
        }

        // ── Liste ──────────────────────────────────────────────────
        ayirici()
        kap.addView(baslikKucuk(getString(R.string.sz_tum_terimler)))
        listeKap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        kap.addView(listeKap)
        listeyiTazele()

        // ── Bakım ──────────────────────────────────────────────────
        ayirici()
        kap.addView(silDugmesi(getString(R.string.sz_temizle)) {
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.sz_temizle_sor, toplam))
                .setPositiveButton(R.string.delete) { _, _ ->
                    Sozluk.temizle(this)
                    ciz()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        })
    }

    private fun listeyiTazele() {
        if (!::listeKap.isInitialized) return
        listeKap.removeAllViews()

        var liste = Sozluk.ara(this, sorgu)
        if (sadeceYildiz) liste = liste.filter { it.yildiz }

        if (liste.isEmpty()) {
            listeKap.addView(bilgi(getString(R.string.sz_sonuc_yok)))
            return
        }
        liste.take(120).forEach { listeKap.addView(terimKarti(it, kisa = false)) }
    }

    // ── Kartlar ────────────────────────────────────────────────────

    private fun terimKarti(t: Sozluk.Terim, kisa: Boolean): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }

        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@SozlukActivity).apply {
                text = (if (t.yildiz) "★ " else "") + t.terim
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })
            if (t.bakildi > 1) {
                addView(TextView(this@SozlukActivity).apply {
                    text = getString(R.string.sz_bakildi, t.bakildi)
                    textSize = 10.5f
                    alpha = 0.7f
                })
            }
        })

        ic.addView(TextView(this).apply {
            text = t.kisa
            textSize = 13f
            alpha = 0.88f
            setLineSpacing(0f, 1.25f)
            maxLines = if (kisa) 2 else 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, (5 * d).toInt(), 0, 0)
        })

        if (!kisa && t.baglam.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = t.baglam + "  ·  " + Sozluk.tarihMetni(t.eklendi)
                textSize = 10.5f
                alpha = 0.65f
                setPadding(0, (5 * d).toInt(), 0, 0)
            })
        }

        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { detay(t) }
        }
    }

    private fun detay(t: Sozluk.Terim) {
        val govde = buildString {
            append(t.kisa)
            if (t.uzun.isNotBlank() && t.uzun != t.kisa) append("\n\n").append(t.uzun)
            if (t.baglam.isNotBlank()) {
                append("\n\n").append(getString(R.string.sz_baglam, t.baglam))
            }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(t.terim)
            .setMessage(govde)
            .setPositiveButton(
                if (t.yildiz) R.string.sz_yildiz_kaldir else R.string.sz_yildizla
            ) { _, _ ->
                Sozluk.yildizDegistir(this, t.terim)
                ciz()
            }
            .setNegativeButton(R.string.delete) { _, _ ->
                Sozluk.sil(this, t.terim)
                ciz()
                // v8.0: silinen terim geri alınabilsin (öneri 6)
                GeriAl.sun(kap, getString(R.string.sz_silindi), tazele = { ciz() }) {
                    Sozluk.kaydet(this, t)
                }
            }
            .setNeutralButton(R.string.ok, null)
            .show()
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
                    this@SozlukActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun silDugmesi(m: String, tikla: () -> Unit) = TextView(this).apply {
        text = m
        textSize = 13.5f
        gravity = Gravity.CENTER
        setTextColor(
            MaterialColors.getColor(
                this@SozlukActivity, com.google.android.material.R.attr.colorError, 0
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
