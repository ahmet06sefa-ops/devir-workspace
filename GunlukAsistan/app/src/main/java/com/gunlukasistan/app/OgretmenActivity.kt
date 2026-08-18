package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executors

/**
 * v7.37 — Özel öğretmen ekranı.
 *
 * Akış:
 *   ANLATIM → SORU → CEVAP → GERİ BİLDİRİM → (doğruysa ileri / yanlışsa basitleştir)
 *
 * Oturum durumu her adımda kaydedilir; kullanıcı çıkıp geri gelince
 * kaldığı yerden devam eder.
 */
class OgretmenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LESSON_ID = "og_lesson_id"
        const val EXTRA_TITLE = "og_title"
        const val EXTRA_ASSET = "og_asset"

        fun ac(context: Context, lessonId: Long, baslik: String, assetPath: String) {
            context.startActivity(
                Intent(context, OgretmenActivity::class.java)
                    .putExtra(EXTRA_LESSON_ID, lessonId)
                    .putExtra(EXTRA_TITLE, baslik)
                    .putExtra(EXTRA_ASSET, assetPath)
            )
        }
    }

    private val worker = Executors.newSingleThreadExecutor()

    private var lessonId = 0L
    private var dersAdi = ""
    private var assetPath = ""

    private lateinit var oturum: OgretmenStore.Oturum

    /** Ekrandaki mevcut adım. */
    private var aktifDers: OgretmenMotoru.Ders? = null

    /** Kullanıcı bu adımda cevap verdi mi. */
    private var cevaplandi = false

    /** Bir sonraki anlatım basitleştirilmiş mi olsun. */
    private var basitIstenecek = false

    /** Seçilen şık (çoktan seçmelide). */
    private var secilenIndeks = -1

    // Görünümler
    private lateinit var vTitle: TextView
    private lateinit var vSubtitle: TextView
    private lateinit var vLevel: TextView
    private lateinit var vProgress: LinearProgressIndicator
    private lateinit var vTopic: TextView
    private lateinit var vLesson: TextView
    private lateinit var vQuestionCard: MaterialCardView
    private lateinit var vQuestion: TextView
    private lateinit var vOptions: LinearLayout
    private lateinit var vAnswerInput: TextInputEditText
    private lateinit var vFeedbackCard: MaterialCardView
    private lateinit var vVerdict: TextView
    private lateinit var vFeedback: TextView
    private lateinit var vLoading: LinearLayout
    private lateinit var vLoadingText: TextView
    private lateinit var vSimpler: MaterialButton
    private lateinit var vNext: MaterialButton
    private lateinit var vScroll: android.widget.ScrollView

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
        setContentView(R.layout.activity_ogretmen)

        lessonId = intent?.getLongExtra(EXTRA_LESSON_ID, 0L) ?: 0L
        dersAdi = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        assetPath = intent?.getStringExtra(EXTRA_ASSET).orEmpty()

        baglaGorunumler()

        vTitle.text = dersAdi
        findViewById<TextView>(R.id.ogClose).setOnClickListener { cikisSor() }
        vLevel.setOnClickListener { seviyeSec() }
        vSimpler.setOnClickListener { basitAnlat() }
        vNext.setOnClickListener { ileriDugmesi() }

        // Oturumu yükle ya da yeni başlat
        val kayitli = OgretmenStore.oturum(this, lessonId)
        if (kayitli != null && !kayitli.tamamlandi && kayitli.adim > 0) {
            oturum = kayitli
            devamSor()
        } else {
            oturum = kayitli?.apply {
                adim = 0; dogru = 0; yanlis = 0; tamamlandi = false; sonOzet = ""
            } ?: OgretmenStore.Oturum(lessonId, dersAdi)
            adimYukle()
        }
    }

    private fun baglaGorunumler() {
        vTitle = findViewById(R.id.ogTitle)
        vSubtitle = findViewById(R.id.ogSubtitle)
        vLevel = findViewById(R.id.ogLevel)
        vProgress = findViewById(R.id.ogProgress)
        vTopic = findViewById(R.id.ogTopic)
        vLesson = findViewById(R.id.ogLesson)
        vQuestionCard = findViewById(R.id.ogQuestionCard)
        vQuestion = findViewById(R.id.ogQuestion)
        vOptions = findViewById(R.id.ogOptions)
        vAnswerInput = findViewById(R.id.ogAnswerInput)
        vFeedbackCard = findViewById(R.id.ogFeedbackCard)
        vVerdict = findViewById(R.id.ogVerdict)
        vFeedback = findViewById(R.id.ogFeedback)
        vLoading = findViewById(R.id.ogLoading)
        vLoadingText = findViewById(R.id.ogLoadingText)
        vSimpler = findViewById(R.id.ogSimpler)
        vNext = findViewById(R.id.ogNext)
        vScroll = findViewById(R.id.ogScroll)
    }

    /** Kayıtlı oturum varsa devam mı baştan mı sorulur. */
    private fun devamSor() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tut_resume_title)
            .setMessage(getString(R.string.tut_resume_body, oturum.adim + 1))
            .setPositiveButton(R.string.tut_resume_yes) { _, _ -> adimYukle() }
            .setNegativeButton(R.string.tut_resume_restart) { _, _ ->
                oturum.adim = 0
                oturum.dogru = 0
                oturum.yanlis = 0
                oturum.sonOzet = ""
                OgretmenStore.zayifTemizle(this, lessonId)
                adimYukle()
            }
            .setCancelable(false)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ANLATIM
    // ═══════════════════════════════════════════════════════════════

    private fun adimYukle() {
        cevaplandi = false
        secilenIndeks = -1
        aktifDers = null

        vQuestionCard.visibility = View.GONE
        vFeedbackCard.visibility = View.GONE
        vTopic.visibility = View.GONE
        vLesson.text = ""
        vOptions.removeAllViews()
        vAnswerInput.setText("")
        vAnswerInput.visibility = View.GONE
        vNext.isEnabled = false
        vSimpler.visibility = View.GONE
        yukleniyor(true, getString(R.string.tut_preparing))
        basligiTazele()

        val basit = basitIstenecek
        basitIstenecek = false

        worker.execute {
            val sonuc = OgretmenMotoru.anlat(
                this, lessonId, dersAdi, assetPath, oturum.adim, basit, oturum.sonOzet
            )
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukleniyor(false)
                if (!sonuc.ok) {
                    hataGoster(sonuc.hata)
                    return@runOnUiThread
                }
                dersiGoster(sonuc)
            }
        }
    }

    private fun dersiGoster(ders: OgretmenMotoru.Ders) {
        aktifDers = ders

        if (ders.konuBasligi.isNotBlank()) {
            vTopic.text = ders.konuBasligi
            vTopic.visibility = View.VISIBLE
        }
        vLesson.text = ders.anlatim
        vSimpler.visibility = View.VISIBLE
        vScroll.smoothScrollTo(0, 0)

        if (ders.soru.isNotBlank()) {
            vQuestion.text = ders.soru
            vQuestionCard.visibility = View.VISIBLE
            if (ders.secenekler.isNotEmpty()) {
                siklariCiz(ders)
                vNext.isEnabled = false
                vNext.text = getString(R.string.tut_check)
            } else {
                // Serbest cevap
                vAnswerInput.visibility = View.VISIBLE
                vNext.isEnabled = true
                vNext.text = getString(R.string.tut_check)
            }
        } else {
            // Soru yoksa doğrudan ilerlenebilir
            vNext.isEnabled = true
            vNext.text = if (ders.sonAdim) {
                getString(R.string.tut_finish)
            } else {
                getString(R.string.tut_next)
            }
        }
        basligiTazele()
    }

    private fun siklariCiz(ders: OgretmenMotoru.Ders) {
        vOptions.removeAllViews()
        val dp = resources.displayMetrics.density

        ders.secenekler.forEachIndexed { i, metin ->
            val btn = MaterialButton(
                this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = ('A' + i) + ") " + metin
                textSize = 14f
                isAllCaps = false
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                cornerRadius = (12 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * dp).toInt() }
                setOnClickListener {
                    if (cevaplandi) return@setOnClickListener
                    secilenIndeks = i
                    sikSeciminiIsaretle()
                    vNext.isEnabled = true
                }
            }
            vOptions.addView(btn)
        }
    }

    private fun sikSeciminiIsaretle() {
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        for (i in 0 until vOptions.childCount) {
            val btn = vOptions.getChildAt(i) as? MaterialButton ?: continue
            if (i == secilenIndeks) {
                btn.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                btn.strokeColor = android.content.res.ColorStateList.valueOf(vurgu)
            } else {
                btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                btn.strokeColor = android.content.res.ColorStateList.valueOf(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorOutline, 0
                    )
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CEVAP
    // ═══════════════════════════════════════════════════════════════

    private fun ileriDugmesi() {
        val ders = aktifDers ?: return

        // Soru yoksa ya da cevaplandıysa: ilerle
        if (ders.soru.isBlank() || cevaplandi) {
            if (ders.sonAdim) bitir() else sonrakiAdim()
            return
        }

        // Cevabı değerlendir
        if (ders.secenekler.isNotEmpty()) {
            if (secilenIndeks < 0) {
                Toast.makeText(this, R.string.tut_pick_option, Toast.LENGTH_SHORT).show()
                return
            }
            val dogruMu = secilenIndeks == ders.dogruIndeks
            val dogruMetin = ders.secenekler.getOrNull(ders.dogruIndeks).orEmpty()
            val geri = if (dogruMu) {
                getString(R.string.tut_correct_body)
            } else {
                getString(R.string.tut_wrong_body, dogruMetin)
            }
            cevabiIsle(dogruMu, geri)
        } else {
            val cevap = vAnswerInput.text?.toString()?.trim().orEmpty()
            if (cevap.isBlank()) {
                Toast.makeText(this, R.string.tut_empty_answer, Toast.LENGTH_SHORT).show()
                return
            }
            yukleniyor(true, getString(R.string.tut_checking))
            vNext.isEnabled = false
            worker.execute {
                val d = OgretmenMotoru.degerlendir(
                    this, dersAdi, ders.soru, "", cevap
                )
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    yukleniyor(false)
                    if (!d.ok) {
                        vNext.isEnabled = true
                        Toast.makeText(this, d.hata, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }
                    cevabiIsle(d.dogruMu, d.geriBildirim)
                }
            }
        }
    }

    private fun cevabiIsle(dogruMu: Boolean, geriBildirim: String) {
        cevaplandi = true
        val ders = aktifDers

        if (dogruMu) oturum.dogru++ else oturum.yanlis++
        OgretmenStore.seviyeGuncelle(this, lessonId, dogruMu)

        // Yanlışsa konuyu zayıf nokta olarak kaydet
        if (!dogruMu && ders != null && ders.konuBasligi.isNotBlank()) {
            OgretmenStore.zayifEkle(this, lessonId, ders.konuBasligi)
        }

        // Şıkları kilitle ve doğruyu göster
        if (ders != null && ders.secenekler.isNotEmpty()) {
            for (i in 0 until vOptions.childCount) {
                val btn = vOptions.getChildAt(i) as? MaterialButton ?: continue
                btn.isEnabled = false
                if (i == ders.dogruIndeks) {
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(
                        GrafikDili.BASARI
                    )
                    btn.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                } else if (i == secilenIndeks) {
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(
                        GrafikDili.HATA
                    )
                    btn.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                }
            }
        }
        vAnswerInput.isEnabled = false

        vVerdict.text = if (dogruMu) {
            getString(R.string.tut_correct)
        } else {
            getString(R.string.tut_wrong)
        }
        vVerdict.setTextColor(if (dogruMu) GrafikDili.BASARI else GrafikDili.HATA)
        vFeedback.text = geriBildirim
        vFeedbackCard.visibility = View.VISIBLE

        // Yanlışsa aynı konuyu basit anlat, doğruysa ilerle
        val sonAdim = ders?.sonAdim == true
        vNext.isEnabled = true
        vNext.text = when {
            !dogruMu -> getString(R.string.tut_explain_again)
            sonAdim -> getString(R.string.tut_finish)
            else -> getString(R.string.tut_next)
        }
        if (!dogruMu) basitIstenecek = true

        oturumKaydet()
        basligiTazele()
        vScroll.post { vScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sonrakiAdim() {
        // Yanlış cevap sonrası aynı adım tekrar (basitleştirilmiş)
        if (!basitIstenecek) {
            oturum.adim++
            aktifDers?.let { d ->
                oturum.sonOzet = (d.konuBasligi + ": " + d.anlatim.take(220)).take(300)
            }
        }
        oturumKaydet()
        adimYukle()
    }

    private fun basitAnlat() {
        basitIstenecek = true
        adimYukle()
    }

    // ═══════════════════════════════════════════════════════════════
    // BİTİŞ
    // ═══════════════════════════════════════════════════════════════

    private fun bitir() {
        oturum.tamamlandi = true
        oturumKaydet()

        val zayif = OgretmenStore.zayifNoktalar(this, lessonId)
        val ozet = OgretmenMotoru.bitirmeOzeti(
            this, dersAdi, oturum.dogru, oturum.yanlis, zayif
        )

        val yapici = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tut_done_title)
            .setMessage(ozet)
            .setPositiveButton(R.string.done) { _, _ -> finish() }
            .setCancelable(false)

        // Dersi tamamlandı işaretlemeyi öner
        val ders = Store.loadLessons(this).firstOrNull { it.id == lessonId }
        if (ders != null && !ders.done) {
            yapici.setNeutralButton(R.string.tut_mark_done) { _, _ ->
                Store.toggleLesson(this, lessonId)
                Toast.makeText(this, R.string.tut_marked, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        // Quiz varsa çözmeyi öner
        if (QuizStore.soruVarMi(this, lessonId)) {
            yapici.setNegativeButton(R.string.tut_go_quiz) { _, _ ->
                QuizActivity.ac(this, lessonId, dersAdi)
                finish()
            }
        }
        yapici.show()
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCI
    // ═══════════════════════════════════════════════════════════════

    private fun oturumKaydet() {
        oturum.dersAdi = dersAdi
        OgretmenStore.oturumKaydet(this, oturum)
    }

    private fun basligiTazele() {
        val seviye = OgretmenStore.seviye(this, lessonId)
        vLevel.text = "📊 " + OgretmenStore.seviyeAdi(this, seviye)
        vSubtitle.text = getString(
            R.string.tut_subtitle, oturum.adim + 1, oturum.dogru, oturum.yanlis
        )
        // İlerleme: seviyeye göre toplam adım 5 veya 9
        val toplam = if (seviye >= 4) 5 else 9
        vProgress.max = toplam
        vProgress.setProgressCompat((oturum.adim + 1).coerceAtMost(toplam), true)
    }

    private fun seviyeSec() {
        val secenekler = (OgretmenStore.SEVIYE_MIN..OgretmenStore.SEVIYE_MAX)
            .map { OgretmenStore.seviyeAdi(this, it) }
            .toTypedArray()
        val simdiki = OgretmenStore.seviye(this, lessonId) - 1
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tut_level_title)
            .setSingleChoiceItems(secenekler, simdiki) { d, hangi ->
                OgretmenStore.seviyeAyarla(this, lessonId, hangi + 1)
                basligiTazele()
                d.dismiss()
                Toast.makeText(this, R.string.tut_level_changed, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun yukleniyor(goster: Boolean, metin: String = "") {
        vLoading.visibility = if (goster) View.VISIBLE else View.GONE
        if (metin.isNotBlank()) vLoadingText.text = metin
        vSimpler.isEnabled = !goster
    }

    private fun hataGoster(mesaj: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tut_error_title)
            .setMessage(mesaj.ifBlank { getString(R.string.tut_error_body) })
            .setPositiveButton(R.string.tut_retry) { _, _ -> adimYukle() }
            .setNegativeButton(R.string.close) { _, _ -> finish() }
            .show()
    }

    private fun cikisSor() {
        if (oturum.adim == 0 && !cevaplandi) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tut_exit_title)
            .setMessage(R.string.tut_exit_body)
            .setPositiveButton(R.string.tut_exit_yes) { _, _ ->
                oturumKaydet()
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onBackPressed() {
        cikisSor()
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdownNow()
    }
}
