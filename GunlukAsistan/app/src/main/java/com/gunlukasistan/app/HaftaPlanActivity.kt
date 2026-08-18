package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

/**
 * v7.97 — Haftalık plan ekranı (öneri 6).
 *
 * [HaftaPlan] modülü v7.96'da yazıldı ama arayüzü yoktu; kullanıcı
 * erişemiyordu. Bu ekran 7 günlük ızgarayı yönetiyor.
 *
 * ── Tasarım ──
 * Her gün bir satır: gün adı · hedef dakika · atanmış ders. Bugün
 * vurgulanıyor. Üstte hazır şablonlar, altta haftalık toplam.
 */
class HaftaPlanActivity : AppCompatActivity() {

    companion object {
        /** v10.10 · C34: widget hücresinden gelen gün numarası (1-7). */
        const val EXTRA_GUN = "hafta_plan_extra_gun"

        fun ac(context: Context) {
            context.startActivity(Intent(context, HaftaPlanActivity::class.java))
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
                        this@HaftaPlanActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()

        // v10.10 · C34: widget hücresinden gelindiyse "o günün plan
        // sayfası" taahhüdü — hedef diyaloğu beklemeden açılır.
        intent.getIntExtra(EXTRA_GUN, -1).takeIf { it in 1..7 }?.let { gun ->
            intent.removeExtra(EXTRA_GUN) // döndürmede tekrar açılmasın
            hedefSec(gun)
        }
    }

    /** v10.10 · C34: plan değişince hafta widget'ı da anında tazelenir. */
    private fun planDegisti() {
        ciz()
        runCatching { WidgetCommon.refreshAll(this) }
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(TextView(this).apply {
            text = getString(R.string.hp_baslik)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        kap.addView(bilgi(getString(R.string.hp_aciklama)))

        // ── Ana anahtar ────────────────────────────────────────────
        ayirici()
        kap.addView(anahtarKart())

        if (!HaftaPlan.acikMi(this)) {
            kap.addView(bilgi(getString(R.string.hp_kapali_bilgi)))
            kap.addView(dugme(getString(R.string.hp_sablonlar), vurgulu = true) {
                sablonSec()
            })
            return
        }

        // ── Gün ızgarası ───────────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.hp_gunler))
        val bugun = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        HaftaPlan.gunSirasi.forEach { g -> kap.addView(gunSatiri(g, g == bugun)) }

        // ── Özet ───────────────────────────────────────────────────
        ayirici()
        kap.addView(ozetKarti())

        // ── Şablonlar ──────────────────────────────────────────────
        kap.addView(dugme(getString(R.string.hp_sablonlar)) { sablonSec() })
        kap.addView(dugme(getString(R.string.hp_temizle)) {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.hp_temizle_sor)
                .setPositiveButton(R.string.ok) { _, _ ->
                    HaftaPlan.gunSirasi.forEach {
                        HaftaPlan.setHedef(this, it, -1)
                        HaftaPlan.setDers(this, it, 0L)
                    }
                    planDegisti()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        })
    }

    // ── Kartlar ────────────────────────────────────────────────────

    private fun anahtarKart(): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (13 * d).toInt(), (14 * d).toInt(), (13 * d).toInt())
        }
        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@HaftaPlanActivity).apply {
                text = getString(R.string.hp_acik)
                textSize = 15.5f
            })
            addView(TextView(this@HaftaPlanActivity).apply {
                text = getString(R.string.hp_acik_alt)
                textSize = 12f
                alpha = 0.7f
                setPadding(0, (2 * d).toInt(), (10 * d).toInt(), 0)
            })
        })
        ic.addView(MaterialSwitch(this).apply {
            isChecked = HaftaPlan.acikMi(this@HaftaPlanActivity)
            setOnCheckedChangeListener { _, v ->
                HaftaPlan.setAcik(this@HaftaPlanActivity, v)
                planDegisti()
            }
        })
        return kartSar(ic)
    }

    /**
     * Bir günün satırı.
     *
     * Dokunmak hedefi değiştirir, uzun basmak ders atar — iki ayrı
     * düğme koymak satırı kalabalıklaştırırdı.
     */
    private fun gunSatiri(calendarGun: Int, bugunMu: Boolean): View {
        val hedef = HaftaPlan.hedef(this, calendarGun)
        val dersAdi = HaftaPlan.dersAdi(this, calendarGun)

        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }

        // Gün adı
        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@HaftaPlanActivity).apply {
                text = HaftaPlan.gunAdi(this@HaftaPlanActivity, calendarGun) +
                    (if (bugunMu) "  •" else "")
                textSize = 15f
                if (bugunMu) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(
                        MaterialColors.getColor(
                            this@HaftaPlanActivity,
                            com.google.android.material.R.attr.colorPrimary, 0
                        )
                    )
                }
            })
            addView(TextView(this@HaftaPlanActivity).apply {
                text = dersAdi.ifBlank { getString(R.string.hp_ders_yok) }
                textSize = 11.5f
                alpha = 0.7f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, (3 * d).toInt(), (8 * d).toInt(), 0)
            })
        })

        // Hedef değeri
        ic.addView(TextView(this).apply {
            text = when {
                hedef < 0 -> getString(R.string.hp_varsayilan)
                hedef == 0 -> getString(R.string.hp_izin)
                else -> getString(R.string.koc_dk, hedef)
            }
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    this@HaftaPlanActivity,
                    if (hedef == 0) com.google.android.material.R.attr.colorOnSurfaceVariant
                    else com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })

        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { hedefSec(calendarGun) }
            setOnLongClickListener { dersAta(calendarGun); true }
        }
    }

    private fun ozetKarti(): View {
        val toplam = HaftaPlan.haftaToplami(this)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = getString(R.string.hp_hafta_toplam, toplam / 60, toplam % 60)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        ic.addView(TextView(this).apply {
            text = getString(
                R.string.hp_gun_sayisi, HaftaPlan.calismaGunSayisi(this@HaftaPlanActivity)
            )
            textSize = 12.5f
            alpha = 0.75f
            setPadding(0, (4 * d).toInt(), 0, 0)
        })
        ic.addView(TextView(this).apply {
            text = getString(R.string.hp_ipucu)
            textSize = 11.5f
            alpha = 0.65f
            setLineSpacing(0f, 1.2f)
            setPadding(0, (8 * d).toInt(), 0, 0)
        })
        return kartSar(ic)
    }

    // ── Seçiciler ──────────────────────────────────────────────────

    private fun hedefSec(calendarGun: Int) {
        val mevcut = HaftaPlan.hedef(this, calendarGun)
        val secici = NumberPicker(this).apply {
            minValue = 0
            maxValue = 300
            value = if (mevcut < 0) Koc.gunlukHedef(this@HaftaPlanActivity) else mevcut
            wrapSelectorWheel = false
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(HaftaPlan.gunAdi(this, calendarGun))
            .setMessage(R.string.hp_hedef_sec)
            .setView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                setPadding(0, (14 * d).toInt(), 0, 0)
                addView(secici)
            })
            .setPositiveButton(R.string.ok) { _, _ ->
                HaftaPlan.setHedef(this, calendarGun, secici.value)
                planDegisti()
            }
            .setNeutralButton(R.string.hp_varsayilan) { _, _ ->
                HaftaPlan.setHedef(this, calendarGun, -1)
                planDegisti()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dersAta(calendarGun: Int) {
        val adimlar = Mufredat.adimlar(this)
        if (adimlar.isEmpty()) {
            Toast.makeText(this, R.string.mf_bos_program, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = (listOf(getString(R.string.hp_ders_yok)) +
            adimlar.map { (if (it.bitti) "✓ " else "○ ") + it.baslik }).toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.hp_ders_ata))
            .setItems(adlar) { _, hangi ->
                HaftaPlan.setDers(
                    this, calendarGun,
                    if (hangi == 0) 0L else adimlar[hangi - 1].id
                )
                planDegisti()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sablonSec() {
        val adlar = (0..3).map { HaftaPlan.sablonAdi(this, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hp_sablonlar)
            .setItems(adlar) { _, hangi ->
                HaftaPlan.sablonUygula(this, hangi)
                Toast.makeText(this, R.string.hp_sablon_uygulandi, Toast.LENGTH_SHORT).show()
                planDegisti()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ── Arayüz yardımcıları ────────────────────────────────────────

    private fun baslikKucuk(m: String) {
        kap.addView(TextView(this).apply {
            text = m
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0.72f
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
                    this@HaftaPlanActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun dugme(m: String, vurgulu: Boolean = false, tikla: () -> Unit) =
        TextView(this).apply {
            text = m
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(
                typeface,
                if (vurgulu) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
            setTextColor(
                MaterialColors.getColor(
                    this@HaftaPlanActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
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
