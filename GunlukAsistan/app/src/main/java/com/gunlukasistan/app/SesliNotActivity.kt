package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors

/**
 * v7.71 — Bas-konuş hızlı not.
 *
 * ── Kullanıcının isteği (10 öneriden 10. madde) ──
 * "Widget'tan bas, konuş, bırak → metne çevrilir, AI kategorize eder
 *  (görev mi, not mu, alışveriş mi)."
 *
 * ── Akış ──
 * 1. Ekran açılır açılmaz mikrofon dinlemeye başlar (ekstra dokunuş yok)
 * 2. Konuşma metne çevrilir
 * 3. [SesliNot] önce yerel kurallarla, gerekirse yapay zekâyla sınıflandırır
 * 4. Önerilen hedef vurgulu, diğerleri seçilebilir olarak gösterilir
 * 5. Kullanıcı onaylar → kaydedilir → ekran kapanır
 *
 * Şeffaf panel (`Theme.QuickAdd`) — araba kullanırken/elin doluyken
 * uygulamaya girip çıkmadan kayıt yapılabilsin diye.
 */
class SesliNotActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(
                Intent(context, SesliNotActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

    private var taniyici: SpeechRecognizer? = null
    private var metin: String = ""
    private var secili: SesliNot.Hedef = SesliNot.Hedef.GOREV
    private var aiCalisiyor = false

    private val mikrofonIzni =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { verildi ->
            if (verildi) dinlemeyeBasla()
            else {
                Toast.makeText(this, R.string.sn_izin_yok, Toast.LENGTH_LONG).show()
                finish()
            }
        }

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

        val dis = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setOnClickListener { finish() }
        }
        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (22 * yogunluk).toInt(), (20 * yogunluk).toInt(),
                (22 * yogunluk).toInt(), (22 * yogunluk).toInt()
            )
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    26 * yogunluk, 26 * yogunluk, 26 * yogunluk, 26 * yogunluk,
                    0f, 0f, 0f, 0f
                )
                setColor(
                    MaterialColors.getColor(
                        this@SesliNotActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
            }
            isClickable = true
        }
        dis.addView(
            ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(kap)
            }
        )
        setContentView(dis)

        durumCiz(getString(R.string.sn_dinliyor), true)
        izinKontrol()
    }

    private fun izinKontrol() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.sn_yok, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val izin = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (izin) dinlemeyeBasla()
        else mikrofonIzni.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // ═══════════════════════════════════════════════════════════════
    // DİNLEME
    // ═══════════════════════════════════════════════════════════════

    private fun dinlemeyeBasla() {
        try {
            taniyici?.destroy()
            taniyici = SpeechRecognizer.createSpeechRecognizer(this)
            val niyet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            taniyici?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    durumCiz(getString(R.string.sn_dinliyor), true)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val ara = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!ara.isNullOrBlank()) durumCiz(ara, true)
                }

                override fun onResults(results: Bundle?) {
                    val soylenen = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (soylenen.isNullOrBlank()) {
                        durumCiz(getString(R.string.sn_duyulmadi), false)
                        tekrarDugmesi()
                    } else {
                        metin = soylenen
                        siniflandir()
                    }
                }

                override fun onError(error: Int) {
                    durumCiz(getString(R.string.sn_duyulmadi), false)
                    tekrarDugmesi()
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    durumCiz(getString(R.string.sn_isleniyor), true)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            taniyici?.startListening(niyet)
        } catch (e: Exception) {
            android.util.Log.w("SesliNot", "Dinleme başlatılamadı", e)
            Toast.makeText(this, R.string.sn_yok, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * v7.95 — Önce komut mu diye bakılır (öneri 5).
     *
     * "25 dakika sayaç başlat" bir not değil, bir emirdir. Komut
     * çözümlenirse doğrudan uygulanır; anlaşılmazsa eski akışa
     * (not/görev sınıflandırma) devredilir.
     */
    private fun komutMu(): Boolean {
        val komut = runCatching { SesliKomut.coz(this, metin) }.getOrNull()
            ?: return false
        if (komut is SesliKomut.Komut.Anlasilmadi) return false

        when (komut) {
            // Ekran açma: MainActivity'yi hedef ekranla başlat
            is SesliKomut.Komut.EkranAc -> {
                runCatching {
                    startActivity(
                        android.content.Intent(this, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, komut.ekran)
                        }
                    )
                }
                Toast.makeText(
                    this, SesliKomut.komutAdi(this, komut), Toast.LENGTH_SHORT
                ).show()
                finish()
                return true
            }

            SesliKomut.Komut.HataTekrar -> {
                runCatching { QuizActivity.acHatalar(this, getString(R.string.ht_baslik)) }
                finish()
                return true
            }

            is SesliKomut.Komut.TerimSor -> {
                runCatching { KonuAnlatimActivity.ac(this, komut.terim) }
                finish()
                return true
            }

            else -> {
                // Sayaç/ders/durum komutları — sonucu göster, ekranda kal
                val sonuc = runCatching { SesliKomut.uygula(this, komut) }
                    .getOrElse { "" }
                if (sonuc.isNotBlank()) {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(SesliKomut.komutAdi(this, komut))
                        .setMessage(sonuc)
                        .setPositiveButton(R.string.ok) { _, _ -> finish() }
                        .setOnDismissListener { finish() }
                        .show()
                } else {
                    finish()
                }
                return true
            }
        }
    }

    /** Yerel sınıflandır, gerekirse AI'a sor (arka planda). */
    private fun siniflandir() {
        // v7.95: önce komut denetimi
        if (komutMu()) return

        val yerel = SesliNot.yerelSinifla(metin)
        secili = yerel.hedef

        if (yerel.guven >= 0.6f || !AiSettings.isReady(this)) {
            sonucCiz(yerel)
            return
        }

        // AI'a danış — ağ işlemi arka planda
        aiCalisiyor = true
        durumCiz(getString(R.string.sn_ai_dusunuyor), true)
        Thread {
            val ai = try {
                SesliNot.aiSinifla(this, metin, yerel)
            } catch (e: Exception) {
                android.util.Log.w("SesliNot", "AI sınıflandırma hatası", e)
                yerel
            }
            runOnUiThread {
                aiCalisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                secili = ai.hedef
                sonucCiz(ai)
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    /** Dinleme/işleme durumu. */
    private fun durumCiz(mesaj: String, calisiyor: Boolean) {
        kap.removeAllViews()
        kap.addView(baslik(getString(R.string.sn_baslik)))
        kap.addView(TextView(this).apply {
            text = if (calisiyor) "🎙  " + mesaj else mesaj
            textSize = 15f
            setLineSpacing(0f, 1.25f)
            setPadding(0, (14 * yogunluk).toInt(), 0, (14 * yogunluk).toInt())
        })
        kap.addView(dugme(getString(R.string.sn_iptal)) { finish() })
        // v10.14 · E28: sesli gelen kutusu girişi (iz varsa)
        val kutuAdet = runCatching { SesliKutu.liste(this).size }.getOrDefault(0)
        if (kutuAdet > 0) {
            kap.addView(
                dugme(getString(R.string.ge_kutu_ac, kutuAdet)) {
                    startActivity(Intent(this, SesliKutuActivity::class.java))
                }
            )
        }
    }

    private fun tekrarDugmesi() {
        kap.addView(dugme(getString(R.string.sn_tekrar), vurgulu = true) {
            dinlemeyeBasla()
        })
    }

    /** Metin + hedef seçimi + kaydet. */
    private fun sonucCiz(sonuc: SesliNot.Sonuc) {
        kap.removeAllViews()
        kap.addView(baslik(getString(R.string.sn_baslik)))

        // Düzenlenebilir metin — tanıma hatasını elle düzeltebilsin
        val girdi = EditText(this).apply {
            setText(metin)
            setSelection(text?.length ?: 0)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
            textSize = 15f
            setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        }
        kap.addView(girdi)

        // AI önerisi bilgisi
        if (sonuc.gerekce == "yapay zekâ") {
            kap.addView(bilgi(
                getString(R.string.sn_ai_onerisi, SesliNot.hedefAdi(this, sonuc.hedef))
            ))
        } else if (!AiSettings.isReady(this) && sonuc.guven < 0.6f) {
            kap.addView(bilgi(getString(R.string.sn_ai_kapali)))
        }

        kap.addView(bilgi(getString(R.string.sn_ne_yapayim)))

        // Hedef çipleri
        val satir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cipler = mutableListOf<Pair<TextView, SesliNot.Hedef>>()
        SesliNot.Hedef.entries.forEach { h ->
            val c = cip(SesliNot.hedefAdi(this, h), h == secili) {
                secili = h
                cipler.forEach { (tv, hh) -> cipBoya(tv, hh == h) }
            }
            cipler.add(c to h)
            satir.addView(c)
        }
        kap.addView(
            android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, (4 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
                addView(satir)
            }
        )

        // Kaydet
        kap.addView(dugme(getString(R.string.sn_kaydet), vurgulu = true) {
            metin = girdi.text?.toString()?.trim().orEmpty()
            if (metin.isBlank()) {
                Toast.makeText(this, R.string.sn_duyulmadi, Toast.LENGTH_SHORT).show()
                return@dugme
            }
            kaydet()
        })

        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(dugme(getString(R.string.sn_tekrar)) {
                    dinlemeyeBasla()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                addView(dugme(getString(R.string.sn_iptal)) { finish() }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
            }
        )
    }

    private fun kaydet() {
        // Asistan seçildiyse kaydetme — soruyu asistana taşı
        if (secili == SesliNot.Hedef.ASISTAN) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, 9)
                }
            )
            try {
                SohbetGecmisi.mesajEkle(this, "user", metin)
            } catch (e: Exception) {
                android.util.Log.w("SesliNot", "Sohbete yazılamadı", e)
            }
            finish()
            return
        }

        // v10.14 · E28: işlenen not gelen kutusunda görünsün
        runCatching { SesliKutu.ekle(this, secili, metin) }
        val onay = SesliNot.kaydet(this, secili, metin)
        if (onay.isNotBlank()) {
            Toast.makeText(
                this, getString(R.string.sn_kaydedildi, onay), Toast.LENGTH_LONG
            ).show()
        }
        try {
            WidgetCommon.refreshAll(this, true)
        } catch (e: Exception) {
            android.util.Log.w("SesliNot", "Widget tazelenemedi", e)
        }
        finish()
    }

    override fun onDestroy() {
        try {
            taniyici?.destroy()
        } catch (e: Exception) {
            android.util.Log.w("SesliNot", "Tanıyıcı kapatılamadı", e)
        }
        taniyici = null
        super.onDestroy()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(m: String) = TextView(this).apply {
        text = m
        textSize = 18f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12f
        alpha = 0.72f
        setLineSpacing(0f, 1.2f)
        setPadding(0, (8 * yogunluk).toInt(), 0, (2 * yogunluk).toInt())
    }

    private fun cip(m: String, secili: Boolean, tikla: () -> Unit) = TextView(this).apply {
        text = m
        textSize = 12.5f
        gravity = Gravity.CENTER
        setPadding(
            (14 * yogunluk).toInt(), (9 * yogunluk).toInt(),
            (14 * yogunluk).toInt(), (9 * yogunluk).toInt()
        )
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (6 * yogunluk).toInt() }
        isClickable = true
        setOnClickListener { tikla() }
        cipBoya(this, secili)
    }

    private fun cipBoya(tv: TextView, secili: Boolean) {
        try {
            val vurgu = MaterialColors.getColor(
                tv, com.google.android.material.R.attr.colorPrimary, 0
            )
            tv.background = GradientDrawable().apply {
                cornerRadius = 18 * yogunluk
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * yogunluk).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            tv.setTextColor(
                if (secili) vurgu
                else MaterialColors.getColor(
                    tv, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SesliNot", "Çip boyanamadı", e)
        }
    }

    private fun dugme(m: String, vurgulu: Boolean = false, tikla: () -> Unit) =
        TextView(this).apply {
            text = m
            textSize = 14f
            gravity = Gravity.CENTER
            val vurgu = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
            setTextColor(vurgu)
            setPadding(0, (13 * yogunluk).toInt(), 0, (13 * yogunluk).toInt())
            background = if (vurgulu) {
                GradientDrawable().apply {
                    cornerRadius = 18 * yogunluk
                    setColor((vurgu and 0x00FFFFFF) or 0x22000000)
                }
            } else {
                android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x22888888), null, null
                )
            }
            isClickable = true
            setOnClickListener { tikla() }
        }
}
