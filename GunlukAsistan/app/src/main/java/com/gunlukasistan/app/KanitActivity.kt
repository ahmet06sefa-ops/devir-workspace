package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File

/**
 * v7.78 — Kanıt ayarları ve kanıt geçmişi.
 *
 * ── İki bölüm ──
 *   1. **Ayarlar** — hangi görevler kanıt ister, denetim ne kadar katı
 *   2. **Geçmiş**  — çekilmiş kanıtlar, onay/red durumu, disk kullanımı
 *
 * Geçmiş bölümü sadece süs değil: kullanıcı yapay zekânın kararlarını
 * görebilmeli. Model sürekli yanlış karar veriyorsa katılığı düşürebilir
 * ya da özelliği kapatabilir — bu şeffaflık güven için şart.
 */
class KanitActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, KanitActivity::class.java))
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
                        this@KanitActivity,
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

        kap.addView(TextView(this).apply {
            text = getString(R.string.kn_baslik)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })
        kap.addView(bilgi(getString(R.string.kn_aciklama)))

        // ── Politika ───────────────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.kn_politika))

        kap.addView(satir(
            getString(R.string.kn_hangi_gorevler),
            politikaAdi(Kanit.politika(this))
        ) { politikaSec() })

        if (Kanit.politika(this) == Kanit.POL_ETIKETLI) {
            kap.addView(satir(
                getString(R.string.kn_etiketler),
                etiketlerMetni()
            ) { etiketlerSec() })
        }

        if (Kanit.politika(this) != Kanit.POL_KAPALI) {
            kap.addView(satir(
                getString(R.string.kn_katilik),
                katilikAdi(Kanit.katilik(this))
            ) { katilikSec() })

            kap.addView(anahtar(
                getString(R.string.kn_red_engeller_ad),
                getString(R.string.kn_red_engeller_alt),
                Kanit.redEngeller(this)
            ) { Kanit.setRedEngeller(this, it) })

            kap.addView(anahtar(
                getString(R.string.kn_cevrimdisi_ad),
                getString(R.string.kn_cevrimdisi_alt),
                Kanit.cevrimdisiKabul(this)
            ) { Kanit.setCevrimdisiKabul(this, it) })

            if (!AiSettings.isReady(this)) {
                kap.addView(bilgi(getString(R.string.kn_ai_uyari)))
            }
        }

        // ── Geçmiş ─────────────────────────────────────────────────
        ayirici()
        val ozet = Kanit.ozet(this)
        baslikKucuk(getString(R.string.kn_gecmis))

        if (ozet.toplam > 0) {
            kap.addView(bilgi(
                getString(R.string.kn_ozet, ozet.toplam, ozet.onayli, ozet.red, ozet.yuzde)
            ))
        }

        val gecmis = Kanit.gecmis(this).take(20)
        if (gecmis.isEmpty()) {
            kap.addView(bilgi(getString(R.string.kn_gecmis_bos)))
        } else {
            gecmis.forEach { kap.addView(gecmisKarti(it)) }
        }

        // ── Bakım ──────────────────────────────────────────────────
        ayirici()
        val disk = Kanit.diskKullanimi(this)
        kap.addView(bilgi(getString(R.string.kn_disk, Kanit.boyutMetni(disk))))

        kap.addView(dugme(getString(R.string.kn_temizle)) {
            val silinen = Kanit.artiklariTemizle(this)
            Toast.makeText(
                this,
                getString(R.string.kn_temizlendi, silinen),
                Toast.LENGTH_SHORT
            ).show()
            ciz()
        })
    }

    // ── Geçmiş kartı ───────────────────────────────────────────────

    private fun gecmisKarti(g: Kanit.GecmisKayit): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((10 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
        }

        // Küçük önizleme
        ic.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((46 * d).toInt(), (46 * d).toInt())
                .apply { rightMargin = (10 * d).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            kucukResim(g.yol)?.let { setImageBitmap(it) }
                ?: setImageResource(R.drawable.ic_task_alt)
        })

        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@KanitActivity).apply {
                text = g.baslik.ifBlank { getString(R.string.kn_gorev_silinmis) }
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            addView(TextView(this@KanitActivity).apply {
                text = Kanit.durumMetni(this@KanitActivity, g.durum) +
                    "  ·  " + Kanit.zamanMetni(g.zaman)
                textSize = 11.5f
                alpha = 0.7f
                setPadding(0, (2 * d).toInt(), 0, 0)
            })
        })

        ic.addView(TextView(this).apply {
            text = when (g.durum) {
                Kanit.ONAYLI -> "✓"
                Kanit.ITIRAZ -> "~"
                Kanit.RED -> "✕"
                else -> "•"
            }
            textSize = 17f
            setTextColor(
                MaterialColors.getColor(
                    this@KanitActivity,
                    when (g.durum) {
                        Kanit.RED -> com.google.android.material.R.attr.colorError
                        else -> com.google.android.material.R.attr.colorPrimary
                    }, 0
                )
            )
        })

        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { resmiBuyut(g) }
        }
    }

    private fun resmiBuyut(g: Kanit.GecmisKayit) {
        val bmp = kucukResim(g.yol, 1200)
        if (bmp == null) {
            Toast.makeText(this, R.string.kn_foto_yok, Toast.LENGTH_SHORT).show()
            return
        }
        val resim = ImageView(this).apply {
            setImageBitmap(bmp)
            adjustViewBounds = true
            setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), 0)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(g.baslik.ifBlank { Kanit.durumMetni(this, g.durum) })
            .setView(ScrollView(this).apply { addView(resim) })
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun kucukResim(yol: String, hedef: Int = 200): android.graphics.Bitmap? = try {
        val dosya = File(yol)
        if (!dosya.exists()) null else {
            val olcu = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(yol, olcu)
            var orn = 1
            while (olcu.outWidth / orn > hedef * 2) orn *= 2
            BitmapFactory.decodeFile(yol, BitmapFactory.Options().apply { inSampleSize = orn })
        }
    } catch (e: Exception) {
        null
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİCİLER
    // ═══════════════════════════════════════════════════════════════

    private fun politikaAdi(p: Int): String = getString(
        when (p) {
            Kanit.POL_KAPALI -> R.string.kn_pol_kapali
            Kanit.POL_ETIKETLI -> R.string.kn_pol_etiketli
            Kanit.POL_HEPSI -> R.string.kn_pol_hepsi
            else -> R.string.kn_pol_isaretli
        }
    )

    private fun politikaSec() {
        val secenekler = arrayOf(
            getString(R.string.kn_pol_kapali),
            getString(R.string.kn_pol_isaretli) + " — " + getString(R.string.kn_pol_isaretli_d),
            getString(R.string.kn_pol_etiketli) + " — " + getString(R.string.kn_pol_etiketli_d),
            getString(R.string.kn_pol_hepsi) + " — " + getString(R.string.kn_pol_hepsi_d)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.kn_hangi_gorevler)
            .setSingleChoiceItems(secenekler, Kanit.politika(this)) { dlg, hangi ->
                Kanit.setPolitika(this, hangi)
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun katilikAdi(k: Int): String = getString(
        when (k) {
            Kanit.KATI_GEVSEK -> R.string.kn_kati_gevsek
            Kanit.KATI_SERT -> R.string.kn_kati_sert
            else -> R.string.kn_kati_normal
        }
    )

    private fun katilikSec() {
        val secenekler = arrayOf(
            getString(R.string.kn_kati_gevsek) + " — " + getString(R.string.kn_kati_gevsek_d),
            getString(R.string.kn_kati_normal) + " — " + getString(R.string.kn_kati_normal_d),
            getString(R.string.kn_kati_sert) + " — " + getString(R.string.kn_kati_sert_d)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.kn_katilik)
            .setSingleChoiceItems(secenekler, Kanit.katilik(this)) { dlg, hangi ->
                Kanit.setKatilik(this, hangi)
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun etiketlerMetni(): String {
        val secili = Kanit.etiketler(this)
        if (secili.isEmpty()) return getString(R.string.kn_etiket_yok)
        return Etiket.hepsi.filter { secili.contains(it.kod) }
            .joinToString(", ") { getString(it.adRes) }
    }

    private fun etiketlerSec() {
        val secili = Kanit.etiketler(this).toMutableSet()
        val adlar = Etiket.hepsi.map { it.emoji + " " + getString(it.adRes) }.toTypedArray()
        val isaretli = Etiket.hepsi.map { secili.contains(it.kod) }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.kn_etiketler)
            .setMultiChoiceItems(adlar, isaretli) { _, hangi, secildi ->
                val kod = Etiket.hepsi[hangi].kod
                if (secildi) secili.add(kod) else secili.remove(kod)
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                Kanit.setEtiketler(this, secili)
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslikKucuk(m: String) {
        kap.addView(TextView(this).apply {
            text = m
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0.7f
            setPadding(0, (4 * d).toInt(), 0, (8 * d).toInt())
        })
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

    private fun satir(ad: String, deger: String, tikla: () -> Unit): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (13 * d).toInt(), (14 * d).toInt(), (13 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = ad
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        ic.addView(TextView(this).apply {
            text = deger
            textSize = 13f
            alpha = 0.8f
        })
        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { tikla() }
        }
    }

    private fun anahtar(
        ad: String,
        alt: String,
        acik: Boolean,
        degisti: (Boolean) -> Unit
    ): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }
        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@KanitActivity).apply {
                text = ad
                textSize = 15f
            })
            if (alt.isNotBlank()) {
                addView(TextView(this@KanitActivity).apply {
                    text = alt
                    textSize = 12f
                    alpha = 0.7f
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
        })
        ic.addView(MaterialSwitch(this).apply {
            isChecked = acik
            setOnCheckedChangeListener { _, v -> degisti(v) }
        })
        return kartSar(ic)
    }

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12.5f
        alpha = 0.75f
        setLineSpacing(0f, 1.2f)
        setPadding(0, (6 * d).toInt(), 0, (8 * d).toInt())
    }

    private fun ayirici() {
        kap.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
            ).apply {
                topMargin = (14 * d).toInt()
                bottomMargin = (6 * d).toInt()
            }
            setBackgroundColor(
                (MaterialColors.getColor(
                    this@KanitActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun dugme(m: String, tikla: () -> Unit) = TextView(this).apply {
        text = m
        textSize = 14f
        gravity = Gravity.CENTER
        setTextColor(
            MaterialColors.getColor(
                this@KanitActivity, com.google.android.material.R.attr.colorPrimary, 0
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
