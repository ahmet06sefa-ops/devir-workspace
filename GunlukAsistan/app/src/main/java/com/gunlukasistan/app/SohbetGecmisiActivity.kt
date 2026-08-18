package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
 * v7.59 — Kayıtlı AI sohbetlerinin listesi.
 *
 * Bir sohbete dokununca o sohbet aktif yapılır ve asistan ekranı açılır;
 * mesajlar yeniden çizilir, model de bağlamı hatırlayarak devam eder.
 *
 * Uzun basınca: yeniden adlandır · kopyala · sil.
 */
class SohbetGecmisiActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, SohbetGecmisiActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout
    /** v7.75: arama sorgusu. */
    private var sorgu: String = ""

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
                        this@SohbetGecmisiActivity,
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

        val liste = if (sorgu.length >= 2) SohbetGecmisi.ara(this, sorgu)
        else SohbetGecmisi.tumu(this)
        val aktif = SohbetGecmisi.aktifId(this)

        // Başlık + sayaç
        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@SohbetGecmisiActivity).apply {
                    text = getString(R.string.sg_baslik)
                    textSize = 20f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                addView(TextView(this@SohbetGecmisiActivity).apply {
                    text = getString(R.string.sg_sayac, liste.size)
                    textSize = 12f
                    alpha = 0.7f
                })
            }
        )

        // v7.75: arama kutusu
        kap.addView(
            android.widget.EditText(this).apply {
                hint = getString(R.string.sa_ara)
                setText(sorgu)
                setSelection(text?.length ?: 0)
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                maxLines = 1
                textSize = 14f
                setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(e: android.text.Editable?) {
                        val yeni = e?.toString()?.trim().orEmpty()
                        if (yeni != sorgu) {
                            sorgu = yeni
                            ciz()
                        }
                    }
                    override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                    override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                })
            }
        )
        if (sorgu.length >= 2) {
            kap.addView(TextView(this).apply {
                text = if (liste.isEmpty()) getString(R.string.sa_yok)
                else getString(R.string.sa_sonuc, liste.size)
                textSize = 11.5f
                alpha = 0.7f
                setPadding(0, 0, 0, (6 * yogunluk).toInt())
            })
        }

        // Yeni sohbet
        kap.addView(
            dugme(getString(R.string.sg_yeni)) {
                SohbetGecmisi.yeniBaslat(this)
                asistanaGit()
            }
        )

        if (liste.isEmpty()) {
            kap.addView(bilgi(getString(R.string.sg_bos)))
            return
        }

        kap.addView(ayirici())
        liste.forEach { s -> kap.addView(sohbetKarti(s, s.id == aktif)) }

        kap.addView(ayirici())
        kap.addView(
            dugme(getString(R.string.sg_tumunu_sil)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sg_tumunu_sil)
                    .setMessage(R.string.sg_tumunu_sil_sor)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        SohbetGecmisi.tumunuSil(this)
                        ciz()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
    }

    private fun sohbetKarti(s: SohbetGecmisi.Sohbet, aktifMi: Boolean): View {
        val kart = MaterialCardView(this).apply {
            radius = 16 * yogunluk
            cardElevation = 0f
            strokeWidth = ((if (aktifMi) 2 else 1) * yogunluk).toInt()
            if (aktifMi) {
                strokeColor = MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { sohbetiAc(s.id) }
            setOnLongClickListener { menu(s); true }
        }

        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
        }

        ic.addView(TextView(this).apply {
            text = if (s.sabit) getString(R.string.sa_sabit) + " " + s.baslik else s.baslik
            textSize = 14.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        if (s.onizleme.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = s.onizleme
                textSize = 12.5f
                alpha = 0.75f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, (3 * yogunluk).toInt(), 0, 0)
            })
        }

        // Alt satır: mesaj sayısı · zaman · aktif rozeti
        ic.addView(TextView(this).apply {
            val parcalar = mutableListOf(
                getString(R.string.sg_mesaj_sayisi, s.mesajlar.size),
                SohbetGecmisi.zamanMetni(s.guncellendi)
            )
            if (aktifMi) parcalar.add(getString(R.string.sg_aktif))
            text = parcalar.filter { it.isNotBlank() }.joinToString(" · ")
            textSize = 11f
            alpha = 0.6f
            setPadding(0, (6 * yogunluk).toInt(), 0, 0)
            if (aktifMi) {
                setTextColor(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
                alpha = 1f
            }
        })

        kart.addView(ic)
        return kart
    }

    /** Sohbeti aktif yapıp asistan ekranını açar. */
    private fun sohbetiAc(id: Long) {
        SohbetGecmisi.setAktif(this, id)
        asistanaGit()
    }

    private fun asistanaGit() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, 9)
            }
        )
        finish()
    }

    private fun menu(s: SohbetGecmisi.Sohbet) {
        MaterialAlertDialogBuilder(this)
            .setTitle(s.baslik)
            .setItems(
                arrayOf(
                    getString(R.string.sg_ac),
                    getString(R.string.sg_yeniden_adlandir),
                    if (s.sabit) getString(R.string.sa_sabit_kaldir)
                    else getString(R.string.sa_sabitle),
                    getString(R.string.sg_kopyala),
                    getString(R.string.sa_disa),
                    getString(R.string.delete)
                )
            ) { _, hangi ->
                when (hangi) {
                    0 -> sohbetiAc(s.id)
                    1 -> adSor(s)
                    2 -> {
                        SohbetGecmisi.sabitDegistir(this, s.id)
                        ciz()
                    }
                    3 -> kopyala(s.id)
                    4 -> disaAktar(s.id)
                    5 -> silSor(s.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun adSor(s: SohbetGecmisi.Sohbet) {
        val girdi = EditText(this).apply {
            setText(s.baslik)
            hint = getString(R.string.sg_ad_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sg_yeniden_adlandir)
            .setView(girdi)
            .setPositiveButton(R.string.save) { _, _ ->
                SohbetGecmisi.adDegistir(this, s.id, girdi.text?.toString().orEmpty())
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun kopyala(id: Long) {
        try {
            val metin = SohbetGecmisi.metneCevir(this, id)
            val pano = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            pano.setPrimaryClip(android.content.ClipData.newPlainText("sohbet", metin))
            Toast.makeText(this, R.string.sg_kopyalandi, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("SohbetGecmisi", "Kopyalanamadı", e)
        }
    }

    /** v7.75: Sohbeti dosyaya yazip paylas menusunu acar. */
    private fun disaAktar(id: Long) {
        try {
            val dosya = SohbetGecmisi.dosyayaYaz(this, id)
            if (dosya == null) {
                Toast.makeText(this, R.string.sa_disa_hata, Toast.LENGTH_SHORT).show()
                return
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, packageName + ".fileprovider", dosya
            )
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.sa_disa)
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SohbetGecmisi", "Disa aktarilamadi", e)
            Toast.makeText(this, R.string.sa_disa_hata, Toast.LENGTH_SHORT).show()
        }
    }

    private fun silSor(id: Long) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.sg_sil_sor)
            .setPositiveButton(R.string.delete) { _, _ ->
                SohbetGecmisi.sil(this, id)
                Toast.makeText(this, R.string.sg_silindi, Toast.LENGTH_SHORT).show()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12.5f
        alpha = 0.7f
        setLineSpacing(0f, 1.25f)
        setPadding(0, (14 * yogunluk).toInt(), 0, (8 * yogunluk).toInt())
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
                this@SohbetGecmisiActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 14f
        setTextColor(
            MaterialColors.getColor(
                this@SohbetGecmisiActivity,
                com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tiklayinca() }
    }
}
