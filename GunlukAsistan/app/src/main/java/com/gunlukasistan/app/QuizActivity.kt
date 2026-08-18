package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * v7.29 — Quiz ekranı.
 *
 * İki modda çalışır:
 *   · TEK DERS   — bir dersin 5 sorusu, sonunda aralıklı tekrar güncellenir
 *   · KARIŞIK    — bölüm/kurs sınavı, birden çok dersten soru
 *
 * Cevap verildikten sonra şık renklenir ve açıklama gösterilir —
 * yanlış cevabın nedenini görmeden geçilmez.
 */
class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LESSON_ID = "quiz_lesson_id"
        const val EXTRA_LESSON_IDS = "quiz_lesson_ids"
        const val EXTRA_TITLE = "quiz_title"
        const val EXTRA_TEKRAR = "quiz_tekrar_modu"

        /** v7.83: hata defterinden açılan tekrar oturumu. */
        const val EXTRA_HATA_MODU = "quiz_hata_modu"

        /** v7.84: geçici soru havuzundan (benzer sorular) oturum. */
        const val EXTRA_GECICI = "quiz_gecici"

        /** Tek ders quizi. */
        fun ac(context: Context, lessonId: Long, baslik: String, tekrarModu: Boolean = false) {
            context.startActivity(
                Intent(context, QuizActivity::class.java)
                    .putExtra(EXTRA_LESSON_ID, lessonId)
                    .putExtra(EXTRA_TITLE, baslik)
                    .putExtra(EXTRA_TEKRAR, tekrarModu)
            )
        }

        /** Bölüm/kurs sınavı. */
        /**
         * v7.83 — Hata defterindeki soruları çözdürür.
         *
         * Sorular [Hatalarim] deposundan gelir; sonuç ders istatistiğine
         * yazılmaz, bunun yerine her sorunun Leitner kutusu güncellenir.
         */
        fun acHatalar(context: Context, baslik: String) {
            context.startActivity(
                Intent(context, QuizActivity::class.java)
                    .putExtra(EXTRA_TITLE, baslik)
                    .putExtra(EXTRA_HATA_MODU, true)
            )
        }

        /**
         * v7.84 — [Hatalarim.geciciAyarla] ile hazırlanan soruları çözdürür.
         *
         * Bu sorular kalıcı depoya yazılmaz; tek seferlik pekiştirme
         * amaçlıdır. Yanlış yapılırsa normal akışla hata defterine düşer.
         */
        fun acGecici(context: Context, baslik: String) {
            context.startActivity(
                Intent(context, QuizActivity::class.java)
                    .putExtra(EXTRA_TITLE, baslik)
                    .putExtra(EXTRA_GECICI, true)
            )
        }

        fun acKarisik(context: Context, lessonIds: LongArray, baslik: String) {
            context.startActivity(
                Intent(context, QuizActivity::class.java)
                    .putExtra(EXTRA_LESSON_IDS, lessonIds)
                    .putExtra(EXTRA_TITLE, baslik)
            )
        }
    }

    private var sorular: List<QuizStore.Soru> = emptyList()

    /** v9.6: sınav simülasyonu süresi için quiz başlangıç anı. */
    private val quizBaslangic = System.currentTimeMillis()
    private var indeks = 0
    private var dogruSayisi = 0
    private var cevaplandi = false
    private var lessonId = 0L
    private var tekrarModu = false

    /** v7.83: hata defteri oturumu mu — sonuç kaydı farklı işler. */
    private var hataModu = false

    /** v7.84: geçici soru havuzundan mı — hiçbir yere kaydedilmez. */
    private var geciciModu = false

    /** v7.83: sorunun geldiği ders/konu adı — hata defterine yazılır. */
    private var kaynakAdi = ""

    private lateinit var optionsBox: LinearLayout
    private lateinit var nextBtn: MaterialButton
    private lateinit var progress: LinearProgressIndicator

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
        setContentView(R.layout.activity_quiz)

        lessonId = intent?.getLongExtra(EXTRA_LESSON_ID, 0L) ?: 0L
        tekrarModu = intent?.getBooleanExtra(EXTRA_TEKRAR, false) ?: false
        hataModu = intent?.getBooleanExtra(EXTRA_HATA_MODU, false) ?: false
        geciciModu = intent?.getBooleanExtra(EXTRA_GECICI, false) ?: false
        val baslik = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        kaynakAdi = baslik
        val coklu = intent?.getLongArrayExtra(EXTRA_LESSON_IDS)

        optionsBox = findViewById(R.id.qzOptions)
        nextBtn = findViewById(R.id.qzNext)
        progress = findViewById(R.id.qzProgress)

        findViewById<TextView>(R.id.qzTitle).text = baslik
        findViewById<View>(R.id.qzClose).setOnClickListener { cikisOnayi() }

        sorular = if (geciciModu) {
            // v7.84: benzer sorular — bellekteki geçici havuz
            Hatalarim.geciciAl()
        } else if (hataModu) {
            // v7.83: hata defterinden — vadesi gelenler önce
            Hatalarim.tekrarSorulari(this, 12)
        } else if (coklu != null && coklu.isNotEmpty()) {
            QuizStore.karisikSinav(this, coklu.toList(), 20)
        } else {
            // v8.0: havuzdan en fazla 10 soru — havuz büyüdükçe her
            // sınavda farklı sorular gelir (öneri 8)
            QuizStore.havuzdanSinav(this, lessonId, 10)
        }

        if (sorular.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quiz_title)
                .setMessage(R.string.quiz_no_questions)
                .setPositiveButton(R.string.done) { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        progress.max = sorular.size
        nextBtn.setOnClickListener { sonrakiSoru() }
        soruGoster()
    }

    // ─────────────────── Soru gösterimi ───────────────────

    private fun soruGoster() {
        if (indeks >= sorular.size) {
            sonucGoster()
            return
        }
        cevaplandi = false
        val soru = sorular[indeks]

        findViewById<TextView>(R.id.qzQuestion).text = soru.metin
        findViewById<TextView>(R.id.qzCounter).text =
            getString(R.string.quiz_counter, indeks + 1, sorular.size)
        findViewById<TextView>(R.id.qzScore).text =
            getString(R.string.quiz_score, dogruSayisi)
        progress.setProgressCompat(indeks, true)
        findViewById<View>(R.id.qzExplainCard).visibility = View.GONE
        nextBtn.isEnabled = false
        nextBtn.setText(
            if (indeks == sorular.size - 1) R.string.quiz_finish else R.string.quiz_next
        )

        optionsBox.removeAllViews()
        soru.siklar.forEachIndexed { i, metin ->
            val kart = LayoutInflater.from(this)
                .inflate(R.layout.item_quiz_option, optionsBox, false) as MaterialCardView
            kart.findViewById<TextView>(R.id.qoLetter).text = ('A' + i).toString()
            kart.findViewById<TextView>(R.id.qoText).text = metin
            kart.setOnClickListener { cevapla(i, soru) }
            optionsBox.addView(kart)
        }
    }

    private fun cevapla(secilen: Int, soru: QuizStore.Soru) {
        if (cevaplandi) return
        cevaplandi = true

        val dogruMu = secilen == soru.dogru
        if (dogruMu) dogruSayisi++

        // v8.2 · Öneri 2 + 20: dokunsal ve görsel geri bildirim.
        //
        // Eskiden yalnız renk değişiyordu; hızlı çözerken doğru mu yanlış
        // mı yaptığını anlamak için okumak gerekiyordu. Artık parmak
        // söylüyor: doğruda tek net vuruş, yanlışta çift vuruş + kartın
        // sarsılması.
        if (dogruMu) {
            Titresim.dogru(this)
            // v8.4 · Öneri 20: doğru şık kısa bir nabızla onaylanır
            runCatching { Canlandir.nabiz(optionsBox.getChildAt(secilen), 1.045f) }
        } else {
            Titresim.yanlis(this)
            runCatching {
                val secilenKart = optionsBox.getChildAt(secilen)
                Canlandir.sarsitKesin(secilenKart)
                // Doğru cevap da belirsin — nereye bakacağını göstersin
                optionsBox.getChildAt(soru.dogru)?.let { Canlandir.bel(it, gecikme = 260L) }
            }
        }

        // v7.83: hata defteri — yanlışı kaydet, doğruyu kutuda ilerlet
        try {
            if (dogruMu) {
                if (hataModu) Hatalarim.dogruCevaplandi(this, soru.id)
            } else if (!geciciModu) {
                // v7.84: geçici (benzer) sorular deftere yazılmaz — onlar
                // pekiştirme amaçlı üretildi, kalıcı hata sayılmamalı
                Hatalarim.yanlisEkle(this, soru, kaynakAdi)
            }
        } catch (e: Exception) {
            android.util.Log.w("QuizActivity", "Hata defteri güncellenemedi", e)
        }

        val yesil = GrafikDili.BASARI
        val kirmizi = GrafikDili.HATA

        // Şıkları boya: doğru yeşil, seçilen yanlışsa kırmızı
        for (i in 0 until optionsBox.childCount) {
            val kart = optionsBox.getChildAt(i) as MaterialCardView
            kart.isClickable = false
            val isaret = kart.findViewById<TextView>(R.id.qoMark)
            when {
                i == soru.dogru -> {
                    kart.strokeColor = yesil
                    kart.strokeWidth = 3
                    isaret.text = "✓"
                    isaret.setTextColor(yesil)
                    isaret.visibility = View.VISIBLE
                }
                i == secilen -> {
                    kart.strokeColor = kirmizi
                    kart.strokeWidth = 3
                    isaret.text = "✗"
                    isaret.setTextColor(kirmizi)
                    isaret.visibility = View.VISIBLE
                }
                else -> kart.alpha = 0.55f
            }
        }

        // Açıklama kartı
        val kart = findViewById<MaterialCardView>(R.id.qzExplainCard)
        // v8.2: açıklama kartı ani belirmesin, yumuşak girsin
        if (kart.visibility != View.VISIBLE) Canlandir.bel(kart)
        val hukum = findViewById<TextView>(R.id.qzVerdict)
        hukum.text = getString(if (dogruMu) R.string.quiz_correct else R.string.quiz_wrong)
        hukum.setTextColor(if (dogruMu) yesil else kirmizi)
        kart.strokeColor = if (dogruMu) yesil else kirmizi

        val aciklama = findViewById<TextView>(R.id.qzExplain)
        if (soru.aciklama.isBlank()) {
            aciklama.visibility = View.GONE
        } else {
            aciklama.visibility = View.VISIBLE
            aciklama.text = soru.aciklama
        }
        kart.visibility = View.VISIBLE

        findViewById<TextView>(R.id.qzScore).text =
            getString(R.string.quiz_score, dogruSayisi)
        nextBtn.isEnabled = true
    }

    private fun sonrakiSoru() {
        indeks++
        soruGoster()
    }

    // ─────────────────── Sonuç ───────────────────

    private fun sonucGoster() {
        progress.setProgressCompat(sorular.size, true)
        val yuzde = dogruSayisi * 100 / sorular.size
        val gecti = yuzde >= 60

        // Tek ders/konu quizi ise sonucu ve tekrar programını güncelle.
        // v7.83: koşul "> 0" idi; konu maddelerinin sanal kimlikleri NEGATİF
        // olduğu için konu sınavlarının sonucu hiç kaydedilmiyordu.
        if (lessonId != 0L && !hataModu && !geciciModu) {
            QuizStore.sonucKaydet(
                this,
                QuizStore.QuizSonuc(lessonId, dogruSayisi, sorular.size, System.currentTimeMillis())
            )
            QuizStore.tekrarSonucu(this, lessonId, gecti)
        }

        val govde = buildString {
            append(getString(R.string.quiz_result_score, dogruSayisi, sorular.size, yuzde))
            append("\n\n")
            append(
                getString(
                    when {
                        yuzde >= 90 -> R.string.quiz_msg_excellent
                        yuzde >= 60 -> R.string.quiz_msg_good
                        else -> R.string.quiz_msg_retry
                    }
                )
            )
            // Aralıklı tekrar bilgisi
            if (lessonId != 0L && !hataModu && !geciciModu) {
                QuizStore.tekrarDurumu(this@QuizActivity, lessonId)?.let { t ->
                    append("\n\n")
                    if (t.ogrenildi) {
                        append(getString(R.string.quiz_learned))
                    } else {
                        append(
                            getString(
                                R.string.quiz_next_review,
                                QuizStore.aralikMetni(t.kutu)
                            )
                        )
                    }
                }
            }
        }

        // v9.6 · Öneri 31 + 36: bekleyen ölçüm varsa sonucu kaydet.
        //
        // Ön/son test ve sınav simülasyonu geçici mod üzerinden
        // çalışıyor; OlcmeBekleyen "bu quiz bittiğinde şunu kaydet"
        // notunu tutuyor. Not okunduğu anda siliniyor.
        val olcumMesaji = runCatching {
            OlcmeBekleyen.tamamla(
                this,
                dogruSayisi,
                sorular.size,
                ((System.currentTimeMillis() - quizBaslangic) / 1000L).toInt()
            )
        }.getOrNull()

        val tamGovde = if (!hataModu || geciciModu) govde else buildString {
            append(govde)
            append("\n\n")
            val o = Hatalarim.ozet(this@QuizActivity)
            append(getString(R.string.ht_kalan, o.toplam, o.ogrenilen))
        }

        // Ölçüm mesajı varsa gövdeye ekle (ön/son test kazanımı veya
        // sınav değerlendirmesi)
        val sonGovde = if (olcumMesaji.isNullOrBlank()) tamGovde
        else tamGovde + "\n\n" + olcumMesaji

        // v8.4 · Öneri 20: düz metin yerine dairesel puan göstergesi.
        //
        // Quiz uygulamanın en çok tekrarlanan eylemi; bitirince
        // "7/10 doğru" yazan gri bir pencere çıkması emeği görünmez
        // kılıyordu. Artık halka dolarak geliyor, renk sonuca göre.
        val sonucGorunumu = runCatching {
            val yg = resources.displayMetrics.density
            fun dp(v: Int) = (v * yg).toInt()
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(dp(24), dp(18), dp(24), dp(4))

                addView(PuanHalkasi(this@QuizActivity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(150), dp(150))
                    ayarla(dogruSayisi, sorular.size)
                })
                addView(TextView(this@QuizActivity).apply {
                    text = sonGovde
                    textSize = 13.5f
                    gravity = android.view.Gravity.CENTER
                    setLineSpacing(0f, 1.3f)
                    setPadding(0, dp(14), 0, 0)
                    setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            this, com.google.android.material.R.attr.colorOnSurface, 0
                        )
                    )
                })
            }
        }.getOrNull()

        // Başarı titreşimi — geçtiyse kutlama deseni
        if (gecti) Titresim.basari(this) else Titresim.yanlis(this)

        MaterialAlertDialogBuilder(this)
            .setTitle(if (gecti) R.string.quiz_passed else R.string.quiz_failed)
            .apply {
                if (sonucGorunumu != null) setView(sonucGorunumu) else setMessage(sonGovde)
            }
            .setCancelable(false)
            .setPositiveButton(R.string.done) { _, _ -> finish() }
            .setNegativeButton(R.string.quiz_retry) { _, _ ->
                indeks = 0
                dogruSayisi = 0
                sorular = sorular.shuffled()
                soruGoster()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // v7.84: geçici havuz bellekte kalmasın
        if (geciciModu) runCatching { Hatalarim.geciciTemizle() }
    }

    private fun cikisOnayi() {
        if (indeks == 0 && !cevaplandi) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.quiz_exit_title)
            .setMessage(R.string.quiz_exit_body)
            .setPositiveButton(R.string.quiz_exit_yes) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        cikisOnayi()
    }
}
