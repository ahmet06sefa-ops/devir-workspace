package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.16 · KULLANICI İSTEĞİ — Birleştirilebilir widget'ın modül düzenleyicisi.
 *
 * Widget üstündeki ⚙ bandından açılır; yalnız O ÖRNEĞİN sırasını
 * değiştirir (aynı widget'ın 2. örneği bağımsız düzenlenebilir —
 * `ModulWidget.siraYaz` örnek kimliğiyle saklar).
 *
 * ── Etkileşim ──
 * Satır: emoji + ad + maliyet bilgisi · Switch dahil/dahil değil —
 * basitlik için "dahil" = listede var, "kapalı" = listeden düşer;
 * ↑ ↓ düğmeleri listeyi yeniden sıralar. Her dokunuş ANINDA kaydeder
 * ve widget'ları tazeler (geri düğmesiyle çıkılır, "kaydet" diye bir
 * eşik yok — atölye hissi budur).
 */
class ModulAyarActivity : AppCompatActivity() {

    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var kap: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val dp = resources.displayMetrics.density
        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (24 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt())
        }
        val kaydir = android.widget.ScrollView(this).apply { addView(kap) }
        setContentView(kaydir)
        ciz()
    }

    private fun tumModuller(): List<String> =
        ModulWidget.siraOku(this, widgetId).let { secili ->
            secili + Modul.TANIMLAR.map { it.anahtar }.filter { it !in secili }
        }

    private fun ciz() {
        kap.removeAllViews()
        val dp = resources.displayMetrics.density
        val secilmis = ModulWidget.siraOku(this, widgetId)

        kap.addView(TextView(this).apply {
            text = getString(R.string.wa_ayar_baslik)
            textSize = 20f; setTypeface(typeface, Typeface.BOLD)
        })
        kap.addView(TextView(this).apply {
            text = getString(R.string.wa_ayar_aciklama)
            textSize = 12.5f; alpha = 0.75f
            setPadding(0, (6 * dp).toInt(), 0, (14 * dp).toInt())
        })

        tumModuller().forEach { anahtar ->
            val t = Modul.tanim(anahtar) ?: return@forEach
            val dahil = anahtar in secilmis
            val sira = secilmis.indexOf(anahtar)

            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 14 * dp
                    setColor(if (dahil) 0x1FD9B892 else 0x10333333)
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = (7 * dp).toInt()
                layoutParams = lp
            }

            satir.addView(TextView(this).apply {
                text = "${t.emoji}  " + getString(t.adRes) +
                    getString(R.string.wa_maliyet, t.satir)
                textSize = 14f
                alpha = if (dahil) 1f else 0.55f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            fun mini(m: String, etkinMi: Boolean, tikla: () -> Unit): TextView = TextView(this).apply {
                text = m; textSize = 17f
                alpha = if (etkinMi) 1f else 0.25f
                setPadding((10 * dp).toInt(), (2 * dp).toInt(), (10 * dp).toInt(), (2 * dp).toInt())
                if (etkinMi) setOnClickListener { tikla() }
            }
            satir.addView(mini("↑", dahil && sira > 0) {
                kaydet(Modul.yukariTasi(secilmis, sira))
            })
            satir.addView(mini("↓", dahil && sira in 0 until secilmis.size - 1) {
                kaydet(Modul.asagiTasi(secilmis, sira))
            })

            val sw = android.widget.Switch(this).apply {
                isChecked = dahil
                setOnCheckedChangeListener { _, acik ->
                    val yeni = if (acik) {
                        (secilmis + anahtar).filter { Modul.tanim(it) != null }.distinct()
                    } else {
                        secilmis - anahtar
                    }
                    kaydet(yeni)
                }
            }
            satir.addView(sw)
            kap.addView(satir)
        }

        kap.addView(TextView(this).apply {
            text = getString(R.string.wa_varsayilan)
            textSize = 13.5f
            setPadding(0, (16 * dp).toInt(), 0, (10 * dp).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { kaydet(Modul.varsayilanSira()) }
        })
    }

    private fun kaydet(sira: List<String>) {
        val son = Modul.temizle(sira)
        ModulWidget.siraYaz(this, widgetId, son)
        WidgetCommon.refreshAll(this, true)
        ciz()
    }
}
