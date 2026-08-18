package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Locale

/**
 * v7.48 — Tüm namaz ayarlarının tek merkezi.
 *
 * ── Kullanıcı isteği ──
 * "bütün diyanet namaz ayarları ordan yapılsın"
 *
 * ── Neden tam ekran? ──
 * v7.47'de ayarlar bir menü listesi + 6 ayrı diyalogdu. Kullanıcı bir ayarı
 * değiştirmek için menüye girip çıkmak zorundaydı. Artık her şey tek sayfada,
 * kaydırarak görülüyor.
 *
 * ── Bölümler ──
 *  1. Modül aç/kapat
 *  2. Konum (85 şehir + elle giriş)
 *  3. Hesaplama yöntemi (Diyanet varsayılan)
 *  4. İkindi mezhebi
 *  5. Dakika düzeltmesi (6 vakit ayrı)
 *  6. Bildirimler (ana + 6 vakit + ses + titreşim + önceden)
 *  7. Widget bilgisi
 *  8. Diyanet varsayılanına dön
 */
class NamazAyarActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, NamazAyarActivity::class.java))
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
                (18 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (30 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@NamazAyarActivity,
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

        kap.addView(baslik(getString(R.string.na_title), 20f))

        // v7.63: bildirim gelmiyorsa önce buraya bak
        val sorun = try { BildirimTani.sorunSayisi(this) } catch (_: Exception) { 0 }
        kap.addView(
            dugme(
                if (sorun == 0) getString(R.string.bt_menu) + "  ✓"
                else getString(R.string.bt_sorun_var, sorun)
            ) { BildirimTaniActivity.ac(this) }
        )

        // ── 1. Modül ──
        val modulAcik = NamazVakti.acikMi(this)
        kap.addView(
            anahtar(getString(R.string.na_modul), getString(R.string.na_modul_d), modulAcik) { a ->
                NamazVakti.setAcik(this, a)
                if (!a) NamazBildirim.hepsiniIptal(this)
                else NamazBildirim.hepsiniKur(this)
                NamazWidget.hepsiniTazele(this)
                WidgetCommon.refreshAll(this)
                ciz()
            }
        )
        if (!modulAcik) {
            kap.addView(bilgi(getString(R.string.na_modul_off)))
            return   // kapalıyken diğer ayarları göstermeye gerek yok
        }

        // Bugünün vakitleri — ayar değişiminin etkisi anında görülsün
        kap.addView(vakitOnizleme())

        kap.addView(ayirici())

        // ── 2. Konum ──
        kap.addView(baslik(getString(R.string.na_konum), 15f))
        kap.addView(
            dugme("📍 " + NamazVakti.sehirAdi(this)) { sehirSec() }
        )
        kap.addView(
            bilgi(
                getString(
                    R.string.na_koordinat,
                    String.format(Locale.US, "%.4f", NamazVakti.enlem(this)),
                    String.format(Locale.US, "%.4f", NamazVakti.boylam(this))
                )
            )
        )
        kap.addView(dugme(getString(R.string.nm_set_manual)) { elleKonumGir() })

        kap.addView(ayirici())

        // ── 3. Hesaplama yöntemi ──
        kap.addView(baslik(getString(R.string.na_yontem), 15f))
        kap.addView(bilgi(getString(R.string.na_yontem_d)))
        kap.addView(yontemSecici())

        // ── 4. İkindi ──
        kap.addView(baslik(getString(R.string.nm_set_asr), 15f))
        kap.addView(ikindiSecici())

        // ── 5. Dakika düzeltmesi ──
        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.nm_set_adjust), 15f))
        kap.addView(bilgi(getString(R.string.nm_adjust_msg)))
        NamazVakti.Vakit.entries.forEach { v -> kap.addView(duzeltmeSatiri(v)) }

        // ── 6. Bildirimler ──
        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.na_bildirim), 15f))

        val bildirimAcik = NamazBildirim.acikMi(this)
        kap.addView(
            anahtar(getString(R.string.nb_master), "", bildirimAcik) { a ->
                NamazBildirim.setAcik(this, a)
                ciz()
            }
        )

        if (bildirimAcik) {
            kap.addView(bilgi(getString(R.string.nb_vakitler)))
            NamazVakti.Vakit.entries.forEach { v ->
                kap.addView(
                    anahtar(
                        v.emoji + " " + getString(v.adRes), "",
                        NamazBildirim.vakitAcik(this, v)
                    ) { a -> NamazBildirim.setVakitAcik(this, v, a) }
                )
            }

            // Ne zaman
            kap.addView(baslik(getString(R.string.nb_once), 13.5f))
            kap.addView(onceSecici())

            // Ses
            kap.addView(baslik(getString(R.string.nb_ses), 13.5f))
            kap.addView(
                anahtar(
                    "🔊 Namaz Saatlerinde Sesli Alarm Çal",
                    "Vakit girdiğinde bildirimle birlikte sesli uyarı / alarm çalar",
                    NamazBildirim.sesliAlarmAcik(this)
                ) { a -> NamazBildirim.setSesliAlarmAcik(this, a) }
            )
            kap.addView(
                dugme("▶️ Alarm Sesini Şimdi Dinle & Test Et (30 sn)") {
                    ZorunluUyari.cal(this, zorlaCal = true)
                    Toast.makeText(this, "🔊 Namaz alarm sesi çalıyor... Durdurmak için güç tuşuna basabilirsiniz.", Toast.LENGTH_LONG).show()
                }
            )
            kap.addView(
                dugme(
                    "🎵 Ses Tonu Seç: " + NamazBildirim.sesAdi(this).ifBlank { getString(R.string.nb_ses_yok) }
                ) { sesSec() }
            )
            kap.addView(bilgi(getString(R.string.nb_ses_ipucu)))

            // Titreşim
            kap.addView(baslik(getString(R.string.nb_titresim), 13.5f))
            val titAcik = NamazBildirim.titresimAcik(this)
            kap.addView(
                anahtar(getString(R.string.nb_titresim_ac), "", titAcik) { a ->
                    NamazBildirim.setTitresim(this, a)
                    ciz()
                }
            )
            if (titAcik) kap.addView(desenSecici())

            kap.addView(dugme(getString(R.string.na_test)) { NamazBildirim.testGonder(this) })
        }

        // ── 7. Widget ──
        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.na_widget), 15f))
        kap.addView(bilgi(getString(R.string.na_widget_d)))

        // ── 8. Varsayılana dön ──
        kap.addView(ayirici())
        kap.addView(dugme(getString(R.string.na_reset)) { varsayilanaDon() })
    }

    // ═══════════════════════════════════════════════════════════════
    // ÖNİZLEME
    // ═══════════════════════════════════════════════════════════════

    /** Bugünün 6 vakti — ayar değiştikçe anında güncellenir. */
    private fun vakitOnizleme(): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (10 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
        }
        return try {
            val gun = NamazVakti.bugunDuzeltilmis(this)
            NamazVakti.Vakit.entries.forEach { v ->
                satir.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        addView(TextView(this@NamazAyarActivity).apply {
                            text = v.emoji
                            textSize = 13f
                            gravity = Gravity.CENTER
                        })
                        addView(TextView(this@NamazAyarActivity).apply {
                            text = gun.saat(v)
                            textSize = 11.5f
                            gravity = Gravity.CENTER
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        })
                    }
                )
            }
            satir
        } catch (e: Exception) {
            android.util.Log.w("NamazAyar", "Önizleme çizilemedi", e)
            satir
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİCİLER
    // ═══════════════════════════════════════════════════════════════

    /** (ad, imsak açısı, yatsı açısı) */
    private val yontemler by lazy {
        listOf(
            Triple(getString(R.string.nm_m_diyanet), 18.0, 17.0),
            Triple(getString(R.string.nm_m_mwl), 18.0, 17.0),
            Triple(getString(R.string.nm_m_isna), 15.0, 15.0),
            Triple(getString(R.string.nm_m_egypt), 19.5, 17.5),
            Triple(getString(R.string.nm_m_makkah), 18.5, 18.0)
        )
    }

    private fun yontemSecici(): View {
        val simdikiI = NamazVakti.imsakAcisi(this)
        val simdikiY = NamazVakti.yatsiAcisi(this)
        val secili = yontemler.indexOfFirst { it.second == simdikiI && it.third == simdikiY }
            .coerceAtLeast(0)
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@NamazAyarActivity,
                android.R.layout.simple_spinner_dropdown_item,
                yontemler.map { it.first }
            )
            setSelection(secili)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    val y = yontemler[pos]
                    if (y.second != NamazVakti.imsakAcisi(this@NamazAyarActivity) ||
                        y.third != NamazVakti.yatsiAcisi(this@NamazAyarActivity)
                    ) {
                        NamazVakti.setAcilar(this@NamazAyarActivity, y.second, y.third)
                        NamazBildirim.hepsiniKur(this@NamazAyarActivity)
                        NamazWidget.hepsiniTazele(this@NamazAyarActivity)
                        ciz()
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun ikindiSecici(): View {
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@NamazAyarActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.nm_asr_1), getString(R.string.nm_asr_2))
            )
            setSelection(NamazVakti.ikindiKat(this@NamazAyarActivity) - 1)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    if (pos + 1 != NamazVakti.ikindiKat(this@NamazAyarActivity)) {
                        NamazVakti.setIkindiKat(this@NamazAyarActivity, pos + 1)
                        NamazBildirim.hepsiniKur(this@NamazAyarActivity)
                        NamazWidget.hepsiniTazele(this@NamazAyarActivity)
                        ciz()
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun onceSecici(): View {
        val secenekler = intArrayOf(0, 5, 10, 15, 30)
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@NamazAyarActivity,
                android.R.layout.simple_spinner_dropdown_item,
                secenekler.map {
                    if (it == 0) getString(R.string.nb_once_tam)
                    else getString(R.string.nb_once_dk, it)
                }
            )
            setSelection(
                secenekler.indexOf(NamazBildirim.oncedenDk(this@NamazAyarActivity))
                    .coerceAtLeast(0)
            )
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    NamazBildirim.setOncedenDk(
                        this@NamazAyarActivity, secenekler.getOrElse(pos) { 0 }
                    )
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun desenSecici(): View {
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@NamazAyarActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    getString(R.string.nb_desen_kisa),
                    getString(R.string.nb_desen_orta),
                    getString(R.string.nb_desen_uzun)
                )
            )
            setSelection(NamazBildirim.titresimDeseni(this@NamazAyarActivity))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    NamazBildirim.setTitresimDeseni(this@NamazAyarActivity, pos)
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }
    }

    /** Tek vakit için ± dakika düzeltmesi. */
    private fun duzeltmeSatiri(v: NamazVakti.Vakit): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (2 * yogunluk).toInt(), 0, (2 * yogunluk).toInt())
        }
        satir.addView(TextView(this).apply {
            text = v.emoji + " " + getString(v.adRes)
            textSize = 13.5f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        satir.addView(TextView(this).apply {
            val d = NamazVakti.duzeltme(this@NamazAyarActivity, v)
            text = if (d > 0) "+" + d + " dk" else d.toString() + " dk"
            textSize = 13f
            setPadding(0, 0, (10 * yogunluk).toInt(), 0)
            alpha = if (d == 0) 0.5f else 1f
        })
        satir.addView(TextView(this).apply {
            text = "−"
            textSize = 20f
            setPadding((12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt(), 0)
            isClickable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            setOnClickListener { duzeltmeDegistir(v, -1) }
        })
        satir.addView(TextView(this).apply {
            text = "+"
            textSize = 20f
            setPadding((12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt(), 0)
            isClickable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            setOnClickListener { duzeltmeDegistir(v, 1) }
        })
        return satir
    }

    private fun duzeltmeDegistir(v: NamazVakti.Vakit, delta: Int) {
        NamazVakti.setDuzeltme(this, v, NamazVakti.duzeltme(this, v) + delta)
        NamazBildirim.hepsiniKur(this)
        NamazWidget.hepsiniTazele(this)
        ciz()
    }

    // ═══════════════════════════════════════════════════════════════
    // KONUM
    // ═══════════════════════════════════════════════════════════════

    private fun sehirSec() {
        val adlar = NamazVakti.SEHIRLER.map { it.ad }.toTypedArray()
        val simdiki = adlar.indexOf(NamazVakti.sehirAdi(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nm_set_city)
            .setSingleChoiceItems(adlar, simdiki) { d, hangi ->
                NamazVakti.sehirAyarla(this, NamazVakti.SEHIRLER[hangi])
                NamazBildirim.hepsiniKur(this)
                NamazWidget.hepsiniTazele(this)
                d.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun elleKonumGir() {
        val govde = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), 0
            )
        }
        val ad = EditText(this).apply {
            hint = getString(R.string.nm_konum_ad)
            setText(NamazVakti.sehirAdi(this@NamazAyarActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val enlem = EditText(this).apply {
            hint = getString(R.string.nm_konum_enlem)
            setText(String.format(Locale.US, "%.4f", NamazVakti.enlem(this@NamazAyarActivity)))
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val boylam = EditText(this).apply {
            hint = getString(R.string.nm_konum_boylam)
            setText(String.format(Locale.US, "%.4f", NamazVakti.boylam(this@NamazAyarActivity)))
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        govde.addView(ad); govde.addView(enlem); govde.addView(boylam)
        govde.addView(TextView(this).apply {
            text = getString(R.string.nm_konum_ipucu)
            textSize = 11.5f
            alpha = 0.7f
            setPadding(0, (10 * yogunluk).toInt(), 0, 0)
        })

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nm_set_manual)
            .setView(govde)
            .setPositiveButton(R.string.save) { _, _ ->
                val e = enlem.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
                val b = boylam.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
                if (e == null || b == null || e < -90 || e > 90 || b < -180 || b > 180) {
                    Toast.makeText(this, R.string.nm_konum_hata, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                NamazVakti.sehirAyarla(
                    this,
                    NamazVakti.Sehir(
                        ad.text?.toString()?.trim()?.ifBlank { "Özel konum" } ?: "Özel konum",
                        e, b
                    )
                )
                NamazBildirim.hepsiniKur(this)
                NamazWidget.hepsiniTazele(this)
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // SES
    // ═══════════════════════════════════════════════════════════════

    private val sesSecici = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { sonuc ->
        if (sonuc.resultCode != RESULT_OK) return@registerForActivityResult
        try {
            val uri = sonuc.data?.getParcelableExtra<android.net.Uri>(
                android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            if (uri == null) {
                NamazBildirim.setSes(this, "", "")
                Toast.makeText(this, R.string.nb_ses_kapatildi, Toast.LENGTH_SHORT).show()
            } else {
                val ad = try {
                    android.media.RingtoneManager.getRingtone(this, uri)?.getTitle(this)
                        ?: getString(R.string.nb_ses_secildi)
                } catch (e: Exception) {
                    android.util.Log.w("NamazAyar", "Ses adı okunamadı", e)
                    getString(R.string.nb_ses_secildi)
                }
                NamazBildirim.setSes(this, uri.toString(), ad)
            }
            ciz()
        } catch (e: Exception) {
            android.util.Log.w("NamazAyar", "Ses seçilemedi", e)
        }
    }

    private fun sesSec() {
        try {
            val mevcut = NamazBildirim.sesUri(this)
            sesSecici.launch(
                Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
                        android.media.RingtoneManager.TYPE_ALL
                    )
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_TITLE,
                        getString(R.string.nb_ses_sec)
                    )
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        if (mevcut.isBlank()) null else android.net.Uri.parse(mevcut)
                    )
                }
            )
        } catch (e: Exception) {
            android.util.Log.w("NamazAyar", "Ses seçici açılamadı", e)
            Toast.makeText(this, R.string.nb_ses_hata, Toast.LENGTH_LONG).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SIFIRLA
    // ═══════════════════════════════════════════════════════════════

    private fun varsayilanaDon() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.na_reset)
            .setMessage(R.string.na_reset_ask)
            .setPositiveButton(R.string.cmd_confirm_yes) { _, _ ->
                NamazVakti.setAcilar(this, 18.0, 17.0)      // Diyanet
                NamazVakti.setIkindiKat(this, 1)
                NamazVakti.Vakit.entries.forEach { NamazVakti.setDuzeltme(this, it, 0) }
                NamazBildirim.setOncedenDk(this, 0)
                NamazBildirim.setTitresim(this, true)
                NamazBildirim.setTitresimDeseni(this, 1)
                NamazBildirim.hepsiniKur(this)
                NamazWidget.hepsiniTazele(this)
                ciz()
                Toast.makeText(this, R.string.na_reset_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖRSEL YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (14 * yogunluk).toInt(), 0, (5 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12f
        alpha = 0.72f
        setPadding(0, 0, 0, (6 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (12 * yogunluk).toInt()
            bottomMargin = (2 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@NamazAyarActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun anahtar(
        ad: String, aciklama: String, acik: Boolean, degisince: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@NamazAyarActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@NamazAyarActivity).apply {
                        text = aciklama
                        textSize = 11.5f
                        alpha = 0.7f
                    })
                }
            }
        )
        satir.addView(
            MaterialSwitch(this).apply {
                isChecked = acik
                setOnCheckedChangeListener { _, yeni -> degisince(yeni) }
            }
        )
        return satir
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@NamazAyarActivity,
                com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tiklayinca() }
    }
}
