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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

/**
 * v7.63 — Bildirim durumu ekranı.
 *
 * "Bildirim gelmiyor" sorununu kullanıcının kendi başına çözebilmesi için
 * tüm engelleri listeler ve her birinin yanına "Düzelt" düğmesi koyar.
 */
class BildirimTaniActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, BildirimTaniActivity::class.java))
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
                        this@BildirimTaniActivity,
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
        ciz()   // izin ekranından dönünce durum tazelensin
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ciz()
    }

    private fun ciz() {
        kap.removeAllViews()

        val maddeler = BildirimTani.kontrolEt(this)
        val sorun = maddeler.count { !it.tamam }

        kap.addView(baslik(getString(R.string.bt_baslik), 20f))

        // Genel durum şeridi
        kap.addView(
            MaterialCardView(this).apply {
                radius = 16 * yogunluk
                cardElevation = 0f
                strokeWidth = (1 * yogunluk).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (14 * yogunluk).toInt() }
                addView(TextView(this@BildirimTaniActivity).apply {
                    text = if (sorun == 0) getString(R.string.bt_hepsi_tamam)
                    else getString(R.string.bt_sorun_var, sorun)
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(
                        (14 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                        (14 * yogunluk).toInt(), (14 * yogunluk).toInt()
                    )
                })
            }
        )

        // Sıradaki vakit bilgisi
        try {
            if (NamazVakti.acikMi(this)) {
                val gun = NamazVakti.bugunDuzeltilmis(this)
                val (v, kalan) = gun.sonraki(NamazVakti.simdiDakika())
                kap.addView(
                    bilgi(
                        getString(
                            R.string.bt_sonraki_vakit,
                            getString(v.adRes) + " " + gun.saat(v),
                            NamazPlan.sureMetni(kalan)
                        )
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("BildirimTani", "Vakit okunamadı", e)
        }

        // Kontrol maddeleri
        maddeler.forEach { m -> kap.addView(maddeKarti(m)) }

        kap.addView(ayirici())

        // Eylemler
        kap.addView(
            dugmeSatiri(getString(R.string.bt_test_gonder)) {
                try {
                    NamazBildirim.testGonder(this)
                    Toast.makeText(this, R.string.bt_test_gitti, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.util.Log.w("BildirimTani", "Test gönderilemedi", e)
                }
            }
        )
        kap.addView(
            dugmeSatiri(getString(R.string.bt_yeniden_kur)) {
                try {
                    NamazBildirim.hepsiniKur(this)
                    BildirimZamanlayici.kur(this)
                    Toast.makeText(this, R.string.bt_kuruldu, Toast.LENGTH_SHORT).show()
                    ciz()
                } catch (e: Exception) {
                    android.util.Log.w("BildirimTani", "Kurulamadı", e)
                }
            }
        )
        kap.addView(
            dugmeSatiri(getString(R.string.bt_sistem_ayar)) {
                BildirimTani.sistemAyariniAc(this)
            }
        )

        // ══════════════════════════════════════════════════════
        // v9.1 · Öneri 41-44 — Sağlık paneli ve gerçek test
        // ══════════════════════════════════════════════════════
        saglikPaneli()
        testPaneli()
    }

    /**
     * v9.1 · Öneri 42-44 — Alarm sağlığı.
     *
     * İzinler, pil kısıtı, üretici riski ve son kurulum bilgisi.
     * Samsung sorunu (v7.88-93) altı sürüm sürmüştü çünkü elimizde
     * hiç veri yoktu; artık kullanıcı tek ekranda görebiliyor.
     */
    private fun saglikPaneli() {
        val s = AlarmSagligi.kontrolEt(this)

        kap.addView(baslik(getString(R.string.as_baslik), 16f))

        // Puan şeridi
        kap.addView(
            MaterialCardView(this).apply {
                radius = 16 * yogunluk
                cardElevation = 0f
                setCardBackgroundColor(
                    when {
                        s.puan >= 90 -> 0xFF2E5B37.toInt()
                        s.puan >= 60 -> 0xFF6B5420.toInt()
                        else -> 0xFF6B2B28.toInt()
                    }
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (12 * yogunluk).toInt() }
                addView(LinearLayout(this@BildirimTaniActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        (16 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                        (16 * yogunluk).toInt(), (14 * yogunluk).toInt()
                    )
                    addView(TextView(this@BildirimTaniActivity).apply {
                        text = "%${s.puan}"
                        textSize = 26f
                        setTextColor(0xFFFFFFFF.toInt())
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                    addView(TextView(this@BildirimTaniActivity).apply {
                        setText(s.durumMetni)
                        textSize = 13f
                        setTextColor(0xCCFFFFFF.toInt())
                    })
                })
            }
        )

        satir(getString(R.string.as_bildirim_izni), s.bildirimIzni) {
            BildirimTani.sistemAyariniAc(this)
        }
        satir(getString(R.string.as_tam_alarm), s.tamAlarmIzni) {
            if (!AlarmSagligi.tamAlarmAyariniAc(this)) {
                AlarmSagligi.uygulamaAyariniAc(this)
            }
        }
        satir(getString(R.string.as_pil), s.pilKisitsiz) {
            AlarmSagligi.pilAyariniAc(this)
        }

        // Üretici uyarısı — programatik çözümü yok, yönlendirme şart
        AlarmSagligi.ureticiYonergesi()?.let { yonerge ->
            kap.addView(
                MaterialCardView(this).apply {
                    radius = 14 * yogunluk
                    cardElevation = 0f
                    setCardBackgroundColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            this, com.google.android.material.R.attr.colorSecondaryContainer, 0
                        )
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * yogunluk).toInt()
                        bottomMargin = (8 * yogunluk).toInt()
                    }
                    isClickable = true
                    setOnClickListener { AlarmSagligi.uygulamaAyariniAc(this@BildirimTaniActivity) }
                    addView(TextView(this@BildirimTaniActivity).apply {
                        text = getString(
                            R.string.as_uretici_uyari,
                            AlarmSagligi.ureticiAdi(), yonerge
                        )
                        textSize = 12.5f
                        setLineSpacing(0f, 1.3f)
                        setPadding(
                            (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                            (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
                        )
                    })
                }
            )
        }

        // Kurulum ve tetiklenme geçmişi (öneri 44)
        kap.addView(bilgi(
            getString(
                R.string.as_son_kurulum,
                AlarmSagligi.zamanMetni(this, s.sonKurulumMs),
                AlarmSagligi.sonKurulumSebebi(this).ifBlank { "—" }
            )
        ))
        kap.addView(bilgi(
            getString(
                R.string.as_son_tetik,
                AlarmSagligi.zamanMetni(this, s.sonTetikMs),
                s.tetikSayisi
            )
        ))
    }

    /**
     * v9.1 · Öneri 41 — Üç katmanlı bildirim testi.
     *
     * Anında / 10 saniye / 2 dakika. Hangisi gelmiyor, sorunun
     * hangi katmanda olduğunu söylüyor:
     *   · Anında gelmiyor  → izin veya kanal
     *   · 10 sn gelmiyor   → tam alarm izni
     *   · 2 dk gelmiyor    → üretici uygulamayı öldürüyor
     */
    private fun testPaneli() {
        kap.addView(baslik(getString(R.string.bt_test_baslik), 16f))
        kap.addView(bilgi(getString(R.string.bt_test_aciklama)))

        testSatiri(
            R.string.bt_test_aninda, R.string.bt_test_aninda_alt,
            BildirimTestReceiver.TUR_ANINDA
        )
        testSatiri(
            R.string.bt_test_kisa, R.string.bt_test_kisa_alt,
            BildirimTestReceiver.TUR_KISA
        )
        testSatiri(
            R.string.bt_test_uzun, R.string.bt_test_uzun_alt,
            BildirimTestReceiver.TUR_UZUN
        )
    }

    private fun testSatiri(baslikRes: Int, altRes: Int, tur: Int) {
        val sonGelis = BildirimTestReceiver.sonGelis(this, tur)
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            strokeColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutlineVariant, 0
            )
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yogunluk).toInt() }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
        }
        val metinler = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        metinler.addView(TextView(this).apply {
            setText(baslikRes)
            textSize = 14.5f
        })
        metinler.addView(TextView(this).apply {
            text = if (sonGelis > 0) {
                getString(R.string.bt_test_geldi, AlarmSagligi.zamanMetni(
                    this@BildirimTaniActivity, sonGelis
                ))
            } else {
                getString(altRes)
            }
            textSize = 11.5f
            setTextColor(
                if (sonGelis > 0) 0xFF4C9A5A.toInt()
                else com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0
                )
            )
        })
        ic.addView(metinler)
        ic.addView(TextView(this).apply {
            text = if (sonGelis > 0) "✓" else "▶"
            textSize = 17f
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        kart.addView(ic)
        kart.dalgaEkle()
        kart.setOnClickListener {
            Titresim.dokunus(it)
            val mesaj = BildirimTestReceiver.baslat(this, tur)
            Toast.makeText(this, mesaj, Toast.LENGTH_LONG).show()
            // Kısa gecikmeli testlerde sonucu görmek için tazele
            if (tur == BildirimTestReceiver.TUR_ANINDA) {
                kap.postDelayed({ if (!isFinishing) ciz() }, 700)
            }
        }
        kap.addView(kart)
    }

    /** Basit durum satırı: ✓ / ✕ + tıklanınca ayara git. */
    private fun satir(metin: String, tamam: Boolean, tikla: () -> Unit) {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            strokeColor = if (tamam) 0x334C9A5A else 0xFFD9534F.toInt()
            isClickable = !tamam
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * yogunluk).toInt() }
            if (!tamam) setOnClickListener { tikla() }
        }
        kart.addView(TextView(this).apply {
            text = (if (tamam) "✓  " else "✕  ") + metin +
                (if (tamam) "" else "   ›")
            textSize = 13.5f
            setPadding(
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
            setTextColor(
                if (tamam) com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurface, 0
                ) else 0xFFD9534F.toInt()
            )
        })
        kap.addView(kart)
    }

    private fun maddeKarti(m: BildirimTani.Madde): View {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            if (!m.tamam) {
                strokeColor = GrafikDili.HATA
                strokeWidth = (2 * yogunluk).toInt()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yogunluk).toInt() }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        satir.addView(TextView(this).apply {
            text = if (m.tamam) "✓" else "✕"
            textSize = 16f
            setTextColor(if (m.tamam) GrafikDili.BASARI else GrafikDili.HATA)
            setPadding(0, 0, (10 * yogunluk).toInt(), 0)
        })
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@BildirimTaniActivity).apply {
                    text = m.baslik
                    textSize = 13.5f
                })
                if (m.aciklama.isNotBlank()) {
                    addView(TextView(this@BildirimTaniActivity).apply {
                        text = m.aciklama
                        textSize = 11.5f
                        alpha = 0.7f
                        setLineSpacing(0f, 1.15f)
                        setPadding(0, (3 * yogunluk).toInt(), 0, 0)
                    })
                }
            }
        )
        if (!m.tamam && m.duzelt != null) {
            satir.addView(
                MaterialButton(
                    this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
                ).apply {
                    text = getString(R.string.bt_duzelt)
                    textSize = 12f
                    minWidth = 0
                    minimumWidth = 0
                    setOnClickListener {
                        try {
                            m.duzelt.invoke(this@BildirimTaniActivity)
                        } catch (e: Exception) {
                            android.util.Log.w("BildirimTani", "Düzeltilemedi", e)
                        }
                        kap.postDelayed({ ciz() }, 400)
                    }
                }
            )
        }
        kart.addView(satir)
        return kart
    }

    // ── Arayüz yardımcıları ──

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, (10 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12.5f
        alpha = 0.75f
        setPadding(0, 0, 0, (10 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (6 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@BildirimTaniActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun dugmeSatiri(metin: String, tikla: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@BildirimTaniActivity,
                com.google.android.material.R.attr.colorPrimary, 0
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
