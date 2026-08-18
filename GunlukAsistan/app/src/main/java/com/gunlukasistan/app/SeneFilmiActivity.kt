package com.gunlukasistan.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import java.util.Calendar

/**
 * v10.14 · ULTRA-30 / E30 — Senenin filminin oynatıldığı sahne.
 *
 * 5 sahne, her biri ~3 saniye, alfa geçişiyle akar; dokunmak sonraki
 * sahneye atlar. Son sahnede Pofi kalır ve özet [KartUretici.paylas]
 * ile tek görsel olarak paylaşılabilir.
 */
class SeneFilmiActivity : AppCompatActivity() {

    private data class Sahne(val emoji: String, val baslik: String, val alt: String)

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var ozet: SeneFilmi.Ozet
    private lateinit var kok: FrameLayout
    private lateinit var emojiYazi: TextView
    private lateinit var baslikYazi: TextView
    private lateinit var altYazi: TextView
    private lateinit var dugmeler: LinearLayout
    private lateinit var tiyatros: LinearLayout

    private var sahneNo = 0
    private lateinit var sahneler: List<Sahne>
    private val handler = Handler(Looper.getMainLooper())
    private val ilerle = Runnable { sonrakiSahne() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        val yil = Calendar.getInstance().get(Calendar.YEAR)
        ozet = SeneFilmi.olustur(this, yil)
        sahneler = sahneleriKur()

        kok = FrameLayout(this)
        setContentView(kok)

        tiyatros = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((28 * yogunluk).toInt(), 0, (28 * yogunluk).toInt(), 0)
            // Dokun → sonraki sahne
            setOnClickListener { sonrakiSahne() }
        }
        kok.addView(
            tiyatros,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        emojiYazi = TextView(this).apply {
            textSize = 88f
            gravity = Gravity.CENTER
        }
        baslikYazi = TextView(this).apply {
            textSize = 26f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, (14 * yogunluk).toInt(), 0, 0)
        }
        altYazi = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            alpha = 0.75f
            setLineSpacing(0f, 1.3f)
            setPadding(0, (8 * yogunluk).toInt(), 0, 0)
        }
        tiyatros.addView(emojiYazi)
        tiyatros.addView(baslikYazi)
        tiyatros.addView(altYazi)

        dugmeler = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        tiyatros.addView(dugmeler)

        kok.setBackgroundColor(
            MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, 0
            )
        )
        sahneGoster(0, animasyonlu = false)
        planIlerle()
    }

    private fun sahneleriKur(): List<Sahne> {
        val liste = mutableListOf<Sahne>()
        liste.add(
            Sahne(
                "🎬",
                getString(R.string.ge_film_sahne_acilis, ozet.yil),
                getString(R.string.ge_film_sahne_acilis_alt)
            )
        )
        if (ozet.enUzunSeri > 0) {
            liste.add(
                Sahne(
                    "🔥",
                    getString(R.string.ge_film_sahne_seri, ozet.enUzunSeri),
                    getString(R.string.ge_film_sahne_seri_alt)
                )
            )
        }
        if (ozet.enCaliskanAy >= 0) {
            liste.add(
                Sahne(
                    "📅",
                    getString(
                        R.string.ge_film_sahne_ay,
                        SeneFilmi.ayAdi(this, ozet.enCaliskanAy), ozet.enCaliskanAyDk
                    ),
                    getString(R.string.ge_film_sahne_ay_alt)
                )
            )
        }
        if (ozet.enUzunGunAnahtar.isNotEmpty()) {
            liste.add(
                Sahne(
                    "🏆",
                    getString(
                        R.string.ge_film_sahne_gun,
                        ozet.enUzunGunMetin, ozet.enUzunGunDk
                    ),
                    getString(R.string.ge_film_sahne_gun_alt)
                )
            )
        }
        // Pofi'nin kapanış konuşması
        liste.add(
            Sahne(
                "🐹",
                getString(R.string.ge_film_sahne_final, ozet.toplamDk, ozet.aktifGun),
                getString(R.string.ge_film_sahne_final_alt)
            )
        )
        return liste
    }

    private fun planIlerle() {
        handler.removeCallbacks(ilerle)
        if (sahneNo < sahneler.size - 1) {
            handler.postDelayed(ilerle, 3_200L)
        }
    }

    private fun sonrakiSahne() {
        if (sahneNo >= sahneler.size - 1) {
            filmiBitir()
            return
        }
        sahneGoster(sahneNo + 1, animasyonlu = true)
        planIlerle()
    }

    private fun sahneGoster(no: Int, animasyonlu: Boolean) {
        sahneNo = no
        val s = sahneler[no]
        val sonMu = no == sahneler.size - 1
        tiyatros.animate().alpha(0f).setDuration(if (animasyonlu) 220 else 0).withEndAction {
            emojiYazi.text = s.emoji
            baslikYazi.text = s.baslik
            altYazi.text = s.alt
            dugmeler.visibility = if (sonMu) android.view.View.VISIBLE else android.view.View.GONE
            if (sonMu) dugmeleriKur()
            tiyatros.animate().alpha(1f).setDuration(280).start()
        }.start()
    }

    private fun dugmeleriKur() {
        dugmeler.removeAllViews()
        dugmeler.addView(dugme(getString(R.string.ge_film_paylas)) {
            runCatching {
                val kart = KartUretici.seneKarti(this@SeneFilmiActivity, ozet)
                KartUretici.paylas(this@SeneFilmiActivity, kart, "sene_filmi_${ozet.yil}.png")
            }
        })
        dugmeler.addView(dugme(getString(R.string.ge_film_kapat)) { finish() })
    }

    private fun dugme(metin: String, tikla: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 14.5f
        gravity = Gravity.CENTER
        val vurgu = MaterialColors.getColor(
            this@SeneFilmiActivity, com.google.android.material.R.attr.colorPrimary, 0
        )
        setTextColor(vurgu)
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 14 * yogunluk
            setStroke((1.5f * yogunluk).toInt(), vurgu)
            setColor((vurgu and 0x00FFFFFF) or 0x22000000)
        }
        setPadding(
            (22 * yogunluk).toInt(), (12 * yogunluk).toInt(),
            (22 * yogunluk).toInt(), (12 * yogunluk).toInt()
        )
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = (8 * yogunluk).toInt()
            topMargin = (26 * yogunluk).toInt()
        }
        isClickable = true
        setOnClickListener { tikla() }
    }

    private fun filmiBitir() {
        // Zaten son sahnedeyiz; kapat düğmesi orada
        handler.removeCallbacks(ilerle)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
