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
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.56 — Israrlı uyarı ayarları.
 *
 * Telefon sessizdeyken bile ses çıkaran uyarının tüm ayarları burada:
 * açma/kapama, ses seçimi, titreşim deseni, süre, kapsam ve test.
 *
 * Yönetici kilitliyse (online odada üye isen) kapatma engellenir.
 */
class ZorunluUyariActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, ZorunluUyariActivity::class.java))
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

    private val ekranKapatmaAlicisi = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF ||
                intent?.action == android.content.Intent.ACTION_SCREEN_ON) {
                if (SayacAyar.isKapatmaTusuyleAlarmDurdur(this@ZorunluUyariActivity)) {
                    ZorunluUyari.durdur(this@ZorunluUyariActivity)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val f = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF).apply {
                addAction(android.content.Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(ekranKapatmaAlicisi, f)
        } catch (_: Exception) {}
    }



    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (SayacAyar.isKapatmaTusuyleAlarmDurdur(this)) {
            if (keyCode == android.view.KeyEvent.KEYCODE_POWER ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                ZorunluUyari.durdur(this)
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
                (18 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (28 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@ZorunluUyariActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    override fun onPause() {
        super.onPause()
        // Ekrandan çıkarken test sesi çalıyorsa sussun
        ZorunluUyari.durdur(this)
        try { unregisterReceiver(ekranKapatmaAlicisi) } catch (_: Exception) {}
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(baslik(getString(R.string.zu_title), 20f))
        kap.addView(bilgi(getString(R.string.zu_aciklama)))

        // ── Ana anahtar ──
        val acik = ZorunluUyari.acikMi(this)
        kap.addView(
            anahtar(
                BildirimKilit.etiket(
                    this, getString(R.string.zu_acik), OnlineStore.Islem.ZORUNLU_KAPAT
                ),
                getString(R.string.zu_acik_d), acik
            ) { yeni ->
                if (BildirimKilit.engellendiMi(
                        this, OnlineStore.Islem.ZORUNLU_KAPAT, yeni
                    ) { mesaj -> uyariGoster(mesaj) }
                ) {
                    ciz()
                } else {
                    ZorunluUyari.setAcik(this, yeni)
                    ciz()
                }
            }
        )

        if (!acik) {
            kap.addView(bilgi(getString(R.string.zu_uyari_alarm)))
            return
        }

        kap.addView(ayirici())

        // ── Ses ──
        val sesAdi = ZorunluUyari.sesAdi(this).ifBlank { getString(R.string.zu_ses_yok) }
        kap.addView(baslik(getString(R.string.zu_ses), 15f))
        kap.addView(dugme(sesAdi) { sesSec() })

        kap.addView(ayirici())

        // ── Titreşim ──
        kap.addView(
            anahtar(
                getString(R.string.zu_titresim),
                getString(R.string.zu_titresim_d),
                ZorunluUyari.titresimAcik(this)
            ) { v ->
                ZorunluUyari.setTitresim(this, v)
                ciz()
            }
        )
        if (ZorunluUyari.titresimAcik(this)) {
            val desenAdi = when (ZorunluUyari.desen(this)) {
                0 -> getString(R.string.zu_desen_0)
                2 -> getString(R.string.zu_desen_2)
                else -> getString(R.string.zu_desen_1)
            }
            kap.addView(dugme(getString(R.string.zu_desen) + ": " + desenAdi) { desenSec() })
        }

        kap.addView(ayirici())

        // ── Süre ──
        kap.addView(
            dugme(getString(R.string.zu_sure, ZorunluUyari.sureSn(this))) { sureSec() }
        )

        // ── Alarm sesini zorla ──
        kap.addView(
            anahtar(
                getString(R.string.zu_ses_zorla),
                getString(R.string.zu_ses_zorla_d),
                ZorunluUyari.sesiZorla(this)
            ) { v -> ZorunluUyari.setSesiZorla(this, v) }
        )

        kap.addView(ayirici())

        // ── Kapsam ──
        kap.addView(baslik(getString(R.string.zu_kapsam), 15f))
        kap.addView(
            anahtar(getString(R.string.zu_k_namaz), "", ZorunluUyari.kapsamNamaz(this)) { v ->
                ZorunluUyari.setKapsam(this, "k_namaz", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.zu_k_gorev), "", ZorunluUyari.kapsamGorev(this)) { v ->
                ZorunluUyari.setKapsam(this, "k_gorev", v)
            }
        )
        kap.addView(
            anahtar(getString(R.string.zu_k_zaman), "", ZorunluUyari.kapsamZaman(this)) { v ->
                ZorunluUyari.setKapsam(this, "k_zaman", v)
            }
        )

        kap.addView(ayirici())

        // ── Rahatsız Etmeyin izni ──
        val dnd = ZorunluUyari.dndIzniVar(this)
        kap.addView(baslik(getString(R.string.zu_dnd_baslik), 15f))
        kap.addView(bilgi(getString(if (dnd) R.string.zu_dnd_var else R.string.zu_dnd_yok)))
        if (!dnd) {
            kap.addView(bilgi(getString(R.string.zu_dnd_govde)))
            kap.addView(dugme(getString(R.string.zu_dnd_ver)) {
                ZorunluUyari.dndAyarlariniAc(this)
            })
        }

        kap.addView(ayirici())

        // ── Test ──
        kap.addView(bilgi(getString(R.string.zu_test_bilgi)))
        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    dugme(getString(R.string.zu_test)) {
                        ZorunluUyari.cal(this@ZorunluUyariActivity, zorlaCal = true)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
                addView(
                    dugme(getString(R.string.zu_dur)) {
                        ZorunluUyari.durdur(this@ZorunluUyariActivity)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
            }
        )
        kap.addView(bilgi(getString(R.string.zu_uyari_alarm)))
    }

    private fun uyariGoster(mesaj: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nset_kilitli)
            .setMessage(mesaj)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİCİLER
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
                ZorunluUyari.setSes(this, "", "")
            } else {
                val ad = try {
                    android.media.RingtoneManager.getRingtone(this, uri)?.getTitle(this)
                        ?: getString(R.string.zu_ses_sec)
                } catch (e: Exception) {
                    android.util.Log.w("ZorunluUyariAyar", "Ses adı okunamadı", e)
                    getString(R.string.zu_ses_sec)
                }
                ZorunluUyari.setSes(this, uri.toString(), ad)
            }
            ciz()
        } catch (e: Exception) {
            android.util.Log.w("ZorunluUyariAyar", "Ses seçilemedi", e)
        }
    }

    private fun sesSec() {
        try {
            val mevcut = ZorunluUyari.sesUri(this)
            sesSecici.launch(
                Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    // Alarm sesleri öncelikli — sessizde duyulması gereken ses
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
                        android.media.RingtoneManager.TYPE_ALL
                    )
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_TITLE,
                        getString(R.string.zu_ses_sec)
                    )
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        if (mevcut.isBlank()) null else android.net.Uri.parse(mevcut)
                    )
                }
            )
        } catch (e: Exception) {
            android.util.Log.w("ZorunluUyariAyar", "Ses seçici açılamadı", e)
            Toast.makeText(this, R.string.zu_ses_hata, Toast.LENGTH_LONG).show()
        }
    }

    private fun desenSec() {
        val secenekler = arrayOf(
            getString(R.string.zu_desen_0),
            getString(R.string.zu_desen_1),
            getString(R.string.zu_desen_2)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.zu_desen)
            .setSingleChoiceItems(secenekler, ZorunluUyari.desen(this)) { d, hangi ->
                ZorunluUyari.setDesen(this, hangi)
                d.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sureSec() {
        val secici = NumberPicker(this).apply {
            minValue = 3
            maxValue = 60
            value = ZorunluUyari.sureSn(this@ZorunluUyariActivity)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.zu_sure_baslik)
            .setView(secici)
            .setPositiveButton(R.string.save) { _, _ ->
                ZorunluUyari.setSureSn(this, secici.value)
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (14 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12f
        alpha = 0.7f
        setLineSpacing(0f, 1.2f)
        setPadding(0, 0, 0, (8 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (4 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@ZorunluUyariActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun anahtar(
        ad: String,
        aciklama: String,
        acik: Boolean,
        degisince: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (7 * yogunluk).toInt(), 0, (7 * yogunluk).toInt())
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@ZorunluUyariActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@ZorunluUyariActivity).apply {
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
                setOnCheckedChangeListener { _, v -> degisince(v) }
            }
        )
        return satir
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@ZorunluUyariActivity,
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
