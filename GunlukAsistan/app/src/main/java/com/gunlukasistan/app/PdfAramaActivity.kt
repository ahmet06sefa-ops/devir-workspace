package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.concurrent.Executors

/**
 * v7.39 — PDF tam metin arama ekranı.
 *
 * Kullanıcı bir kelime yazar, 105 ders PDF'inde aranır, sonuca dokununca
 * doğrudan o dersin o sayfası açılır.
 *
 * İlk arama yavaştır (PDF'ler tek tek işlenir), sonrakiler anında.
 * İlerleme çubuğu kullanıcıyı bilgilendirir; ekrandan çıkılırsa iptal edilir.
 */
class PdfAramaActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, PdfAramaActivity::class.java))
        }
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val yogunluk get() = resources.displayMetrics.density

    private lateinit var girdi: EditText
    private lateinit var durum: TextView
    private lateinit var sonucKabi: LinearLayout
    private lateinit var ilerlemeKutusu: LinearLayout
    private lateinit var ilerlemeYazi: TextView
    private lateinit var ilerlemeCubuk: LinearProgressIndicator
    private lateinit var indeksBilgi: TextView
    private lateinit var indeksTumu: MaterialButton

    /** Şu an arama çalışıyor mu — çift tetiklemeyi önler. */
    @Volatile
    private var calisiyor = false

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
        setContentView(R.layout.activity_pdf_arama)

        girdi = findViewById(R.id.psInput)
        durum = findViewById(R.id.psStatus)
        sonucKabi = findViewById(R.id.psResults)
        ilerlemeKutusu = findViewById(R.id.psProgressBox)
        ilerlemeYazi = findViewById(R.id.psProgressText)
        ilerlemeCubuk = findViewById(R.id.psProgress)
        indeksBilgi = findViewById(R.id.psIndexInfo)
        indeksTumu = findViewById(R.id.psIndexAll)

        findViewById<TextView>(R.id.psClose).setOnClickListener { finish() }
        findViewById<TextView>(R.id.psSearch).setOnClickListener { aramayiBaslat() }
        indeksTumu.setOnClickListener { tumunuIndeksle() }

        girdi.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                aramayiBaslat()
                true
            } else false
        }

        durum.text = getString(R.string.ps_intro)
        indeksBilgiTazele()
    }

    private fun indeksBilgiTazele() {
        val indekslenen = PdfArama.indekslenenSayisi(this)
        val toplam = PdfArama.pdfliDersSayisi(this)
        indeksBilgi.text = getString(
            R.string.ps_index_info, indekslenen, toplam, PdfArama.indeksBoyutu(this)
        )
        indeksTumu.visibility = if (indekslenen >= toplam && toplam > 0) View.GONE
        else View.VISIBLE
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAMA
    // ═══════════════════════════════════════════════════════════════

    private fun aramayiBaslat() {
        if (calisiyor) return
        val sorgu = girdi.text?.toString()?.trim().orEmpty()
        if (sorgu.length < 2) {
            Toast.makeText(this, R.string.ps_too_short, Toast.LENGTH_SHORT).show()
            return
        }
        klavyeyiKapat()

        sonucKabi.removeAllViews()
        durum.text = getString(R.string.ps_searching)
        ilerlemeKutusu.visibility = View.VISIBLE
        ilerlemeCubuk.isIndeterminate = false
        calisiyor = true

        worker.execute {
            val sonuclar = PdfArama.ara(this, sorgu) { ilerleme ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    ilerlemeCubuk.max = ilerleme.toplam
                    ilerlemeCubuk.setProgressCompat(ilerleme.islenen, true)
                    ilerlemeYazi.text = getString(
                        R.string.ps_indexing,
                        ilerleme.islenen, ilerleme.toplam, ilerleme.suAnki
                    )
                }
            }
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                ilerlemeKutusu.visibility = View.GONE
                indeksBilgiTazele()
                sonuclariGoster(sorgu, sonuclar)
            }
        }
    }

    private fun sonuclariGoster(sorgu: String, sonuclar: List<PdfArama.Sonuc>) {
        sonucKabi.removeAllViews()

        if (sonuclar.isEmpty()) {
            durum.text = getString(R.string.ps_no_result, sorgu)
            sonucKabi.addView(
                TextView(this).apply {
                    text = getString(R.string.ps_no_result_tip)
                    textSize = 13f
                    alpha = 0.75f
                    setPadding(4, (16 * yogunluk).toInt(), 4, 0)
                }
            )
            return
        }

        // Derse göre grupla — aynı dersteki sonuçlar bir arada dursun
        val gruplar = sonuclar.groupBy { it.lessonId }
        durum.text = getString(R.string.ps_result_count, sonuclar.size, gruplar.size)

        gruplar.forEach { (_, grup) ->
            val ilk = grup.first()

            // Ders başlığı
            sonucKabi.addView(
                TextView(this).apply {
                    text = ilk.dersAdi
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(
                        (4 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                        (4 * yogunluk).toInt(), (2 * yogunluk).toInt()
                    )
                }
            )
            if (ilk.kursAdi.isNotBlank()) {
                sonucKabi.addView(
                    TextView(this).apply {
                        text = ilk.kursAdi
                        textSize = 11.5f
                        alpha = 0.7f
                        setPadding((4 * yogunluk).toInt(), 0, 0, (4 * yogunluk).toInt())
                    }
                )
            }

            // Sonuç kartları
            grup.forEach { sonuc -> sonucKabi.addView(sonucKarti(sonuc)) }
        }
    }

    /** Tek bir sonucun kartı — dokununca PDF'in o sayfası açılır. */
    private fun sonucKarti(sonuc: PdfArama.Sonuc): View {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yogunluk).toInt() }
            isClickable = true
            isFocusable = true
        }

        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((13 * yogunluk).toInt(), (11 * yogunluk).toInt(),
                (13 * yogunluk).toInt(), (11 * yogunluk).toInt())
        }

        // Sayfa etiketi
        ic.addView(
            TextView(this).apply {
                text = getString(R.string.ps_page, sonuc.sayfa + 1)
                textSize = 11.5f
                setTextColor(
                    MaterialColors.getColor(
                        this@PdfAramaActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        )

        // Vurgulu parça
        ic.addView(
            TextView(this).apply {
                text = vurgula(sonuc.parca, sonuc.vurguBas, sonuc.vurguUzunluk)
                textSize = 13f
                setLineSpacing(2.5f * yogunluk, 1f)
                setPadding(0, (4 * yogunluk).toInt(), 0, 0)
            }
        )

        kart.addView(ic)
        kart.dalgaEkle()
        kart.setOnClickListener { sayfayiAc(sonuc) }
        return kart
    }

    /** Eşleşen kelimeyi sarı zemin + kalın yapar. */
    private fun vurgula(metin: String, bas: Int, uzunluk: Int): CharSequence {
        if (bas < 0 || uzunluk <= 0 || bas + uzunluk > metin.length) return metin
        return SpannableString(metin).apply {
            setSpan(
                BackgroundColorSpan(0x66FFD54F),
                bas, bas + uzunluk, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                bas, bas + uzunluk, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /** Sonuca dokununca PDF'i o sayfadan açar. */
    private fun sayfayiAc(sonuc: PdfArama.Sonuc) {
        val ders = Store.loadLessons(this).firstOrNull { it.id == sonuc.lessonId }
        startActivity(
            Intent(this, LessonPdfActivity::class.java).apply {
                putExtra(LessonPdfActivity.EXTRA_ASSET, sonuc.assetPath)
                putExtra(LessonPdfActivity.EXTRA_TITLE, sonuc.dersAdi)
                putExtra(LessonPdfActivity.EXTRA_SUB, sonuc.kursAdi)
                putExtra(LessonPdfActivity.EXTRA_LESSON_ID, sonuc.lessonId)
                // v7.39: doğrudan bulunan sayfaya git
                putExtra(LessonPdfActivity.EXTRA_START_PAGE, sonuc.sayfa)
                if (ders == null) putExtra(LessonPdfActivity.EXTRA_LESSON_ID, 0L)
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // TOPLU İNDEKSLEME
    // ═══════════════════════════════════════════════════════════════

    private fun tumunuIndeksle() {
        if (calisiyor) return
        val kalan = PdfArama.pdfliDersSayisi(this) - PdfArama.indekslenenSayisi(this)
        if (kalan <= 0) {
            Toast.makeText(this, R.string.ps_already_indexed, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ps_index_all)
            .setMessage(getString(R.string.ps_index_ask, kalan))
            .setPositiveButton(R.string.ps_index_start) { _, _ -> indekslemeyiYurut() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun indekslemeyiYurut() {
        calisiyor = true
        ilerlemeKutusu.visibility = View.VISIBLE
        ilerlemeCubuk.isIndeterminate = false
        durum.text = getString(R.string.ps_indexing_all)
        indeksTumu.isEnabled = false

        worker.execute {
            val yeni = PdfArama.tumunuIndeksle(this) { ilerleme ->
                // v7.43: bildirimde de ilerleme göster (öneri 21)
                // Her 5 derste bir güncelle — bildirim spam'i olmasın
                if (ilerleme.islenen % 5 == 0) {
                    try {
                        BildirimUretici.pdfIndeksIlerleme(
                            this, ilerleme.islenen, ilerleme.toplam, false
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("PdfArama", "İlerleme bildirimi", e)
                    }
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    ilerlemeCubuk.max = ilerleme.toplam
                    ilerlemeCubuk.setProgressCompat(ilerleme.islenen, true)
                    ilerlemeYazi.text = getString(
                        R.string.ps_indexing,
                        ilerleme.islenen, ilerleme.toplam, ilerleme.suAnki
                    )
                }
            }
            try {
                BildirimUretici.pdfIndeksIlerleme(
                    this, yeni, PdfArama.pdfliDersSayisi(this), true
                )
            } catch (e: Exception) {
                android.util.Log.w("PdfArama", "Bitiş bildirimi", e)
            }
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                ilerlemeKutusu.visibility = View.GONE
                indeksTumu.isEnabled = true
                indeksBilgiTazele()
                durum.text = getString(R.string.ps_index_done, yeni)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ORTAK
    // ═══════════════════════════════════════════════════════════════

    private fun klavyeyiKapat() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(girdi.windowToken, 0)
        } catch (e: Exception) {
            android.util.Log.w("PdfAramaActivity", "Klavye kapatılamadı", e)
        }
    }

    override fun onDestroy() {
        // Arka planda süren indekslemeyi durdur — pil ve CPU boşa gitmesin
        PdfArama.iptal = true
        worker.shutdownNow()
        super.onDestroy()
    }
}
