package com.gunlukasistan.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream

/**
 * Ders PDF okuyucu.
 *
 * v7.5 — ilk sürüm: assets'ten okuma, sayfa sayfa gösterme
 * v7.7 — eklenenler:
 *   · Parmakla yakınlaştırma (pinch-zoom) ve çift dokunuşla büyütme
 *   · Gece modu (renk ters çevirme) — göz yormaz
 *   · Kaldığın sayfayı hatırlama ve oraya dönme
 *   · Okuma ilerlemesi göstergesi
 *   · Bellek dostu önbellek (en fazla 6 sayfa tutulur)
 * v7.14 — ders notu: okurken not al, not dersle birlikte saklanır
 */
class LessonPdfActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASSET = "pdf_asset"
        const val EXTRA_TITLE = "pdf_title"
        const val EXTRA_SUB = "pdf_sub"
        /** Tamamlandı olarak işaretlenecek ders kimliği (0 = yok). */
        const val EXTRA_LESSON_ID = "pdf_lesson_id"

        /** v7.39: doğrudan açılacak sayfa (0 tabanlı, -1 = kaldığı yerden). */
        const val EXTRA_START_PAGE = "pdf_start_page"

        private const val PREF = "pdf_okuma_v1"
        private const val K_NIGHT = "gece_modu"

        /** Bellekte aynı anda tutulacak en fazla sayfa görüntüsü. */
        private const val CACHE_LIMIT = 6
    }

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var lessonId = 0L
    private var assetPath = ""

    /** Yakınlaştırma çarpanı (1.0 = normal). */
    private var zoom = 1.0f

    /** Gece modu açık mı. */
    private var nightMode = false

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PageAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var progressText: TextView

    /** Oluşturulan sayfa görüntüleri — sınırlı önbellek. */
    private val cache = LinkedHashMap<Int, Bitmap>()

    /** Gece modu için renk tersleme süzgeci. */
    private val nightFilter by lazy {
        ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    private fun prefs() = getSharedPreferences(PREF, MODE_PRIVATE)

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
        setContentView(R.layout.activity_lesson_pdf)

        assetPath = intent?.getStringExtra(EXTRA_ASSET).orEmpty()
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val sub = intent?.getStringExtra(EXTRA_SUB).orEmpty()
        lessonId = intent?.getLongExtra(EXTRA_LESSON_ID, 0L) ?: 0L
        nightMode = prefs().getBoolean(K_NIGHT, false)

        findViewById<TextView>(R.id.lpTitle).text = title
        findViewById<TextView>(R.id.lpSub).text = sub
        findViewById<View>(R.id.lpClose).setOnClickListener { finish() }
        progressText = findViewById(R.id.lpProgress)

        val doneButton = findViewById<View>(R.id.lpDone)
        val doneText = findViewById<TextView>(R.id.lpDoneText)

        fun paintDone() {
            val done = Store.loadLessons(this)
                .firstOrNull { it.id == lessonId }?.done ?: false
            doneText.text = getString(
                if (done) R.string.lp_done_undo else R.string.lp_done
            )
        }
        if (lessonId > 0L) {
            paintDone()
            doneButton.setOnClickListener {
                Store.toggleLesson(this, lessonId)
                paintDone()
                Toast.makeText(this, R.string.lp_saved, Toast.LENGTH_SHORT).show()
            }
        } else {
            doneButton.visibility = View.GONE
        }

        // v7.10: PDF paylaşma
        findViewById<TextView>(R.id.lpShare).setOnClickListener { pdfPaylas() }

        // v7.8: yer imi düğmesi
        val favBtn = findViewById<TextView>(R.id.lpFav)
        fun paintFav() {
            val fav = Store.loadLessons(this)
                .firstOrNull { it.id == lessonId }?.fav ?: false
            favBtn.text = getString(
                if (fav) R.string.co_fav_filter_on else R.string.co_fav_filter_off
            )
        }
        if (lessonId > 0L) {
            paintFav()
            favBtn.setOnClickListener {
                val yeni = Store.toggleLessonFav(this, lessonId)
                paintFav()
                Toast.makeText(
                    this,
                    getString(if (yeni) R.string.co_fav_added else R.string.co_fav_removed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            favBtn.visibility = View.GONE
        }

        // v7.14: ders notu düğmesi
        val noteBtn = findViewById<TextView>(R.id.lpNote)
        // v7.97: uzun bas → bu sayfaya yer imi / sayfa notu (öneri 9)
        noteBtn.setOnLongClickListener {
            sayfaImiPenceresi()
            true
        }
        if (lessonId > 0L) {
            boyaNotSimgesi(noteBtn)
            noteBtn.setOnClickListener { notPenceresi(noteBtn) }
        } else {
            noteBtn.visibility = View.GONE
        }

        // v7.31: derse soru sor (RAG)
        findViewById<TextView>(R.id.lpAsk).setOnClickListener { derseSor() }

        // v7.32: sesli dinleme
        findViewById<TextView>(R.id.lpListen).setOnClickListener { sesliDinle() }

        // v7.7: gece modu ve yakınlaştırma düğmeleri
        findViewById<TextView>(R.id.lpNight).apply {
            text = if (nightMode) "☀" else "☾"
            setOnClickListener { toggleNight() }
        }
        findViewById<View>(R.id.lpZoomIn).setOnClickListener { changeZoom(1.25f) }
        findViewById<View>(R.id.lpZoomOut).setOnClickListener { changeZoom(0.8f) }

        recycler = findViewById(R.id.lpRecycler)
        layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager

        if (!open(assetPath)) {
            Toast.makeText(this, R.string.lp_error, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = PageAdapter()
        recycler.adapter = adapter
        applyNight()

        // Parmakla yakınlaştırma
        val scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    changeZoom(d.scaleFactor, sessiz = true)
                    return true
                }
            })
        recycler.setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)
            false
        }

        // Okuma ilerlemesini takip et
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateProgress()
            }
        })

        // v7.39: arama sonucundan gelindiyse doğrudan o sayfaya git
        val istenenSayfa = intent?.getIntExtra(EXTRA_START_PAGE, -1) ?: -1
        val toplamSayfa = renderer?.pageCount ?: 0

        if (istenenSayfa in 0 until toplamSayfa) {
            recycler.post {
                layoutManager.scrollToPositionWithOffset(istenenSayfa, 0)
                updateProgress()
                Toast.makeText(
                    this,
                    getString(R.string.lp_jumped, istenenSayfa + 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        // Kaldığı sayfaya dön
        val son = prefs().getInt(sayfaAnahtari(), 0)
        if (son > 0 && son < toplamSayfa) {
            recycler.post {
                layoutManager.scrollToPositionWithOffset(son, 0)
                updateProgress()
                Toast.makeText(
                    this,
                    getString(R.string.lp_resumed, son + 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            recycler.post { updateProgress() }
        }
    }

    /** v7.10: ders PDF'ini başka uygulamalarla paylaşır. */
    private fun pdfPaylas() {
        try {
            val kaynak = File(cacheDir, "ders_" + assetPath.substringAfterLast('/'))
            if (!kaynak.exists()) {
                Toast.makeText(this, R.string.lp_error, Toast.LENGTH_SHORT).show()
                return
            }
            // Okunabilir bir adla kopyala
            val baslik = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
                .replace(Regex("[^A-Za-z0-9ğüşıöçĞÜŞİÖÇ ._-]"), "")
                .trim().ifBlank { "ders" }
            val hedef = File(cacheDir, "$baslik.pdf")
            kaynak.copyTo(hedef, overwrite = true)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", hedef
            )
            val gonder = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, baslik)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(
                android.content.Intent.createChooser(gonder, getString(R.string.lp_share))
            )
        } catch (e: Exception) {
            android.util.Log.w("LessonPdf", "Paylaşım başarısız", e)
            Toast.makeText(this, R.string.lp_share_error, Toast.LENGTH_SHORT).show()
        }
    }

    /** v7.14: not düğmesinin simgesini duruma göre boyar. */
    private fun boyaNotSimgesi(btn: TextView) {
        val varMi = Store.lessonNote(this, lessonId).isNotBlank()
        btn.text = getString(
            if (varMi) R.string.lp_note_icon_full else R.string.lp_note_icon_empty
        )
    }

    /** v7.14: ders notu yazma penceresi. */
    private fun notPenceresi(btn: TextView) {
        val mevcut = Store.lessonNote(this, lessonId)
        val kutu = android.widget.EditText(this).apply {
            setText(mevcut)
            setSelection(mevcut.length)
            hint = getString(R.string.lp_note_hint)
            minLines = 4
            maxLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setPadding(48, 32, 48, 32)
        }
        val sarmal = android.widget.FrameLayout(this).apply { addView(kutu) }

        val yapici = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lp_note_title)
            .setView(sarmal)
            .setPositiveButton(R.string.lp_note_save) { _, _ ->
                val yeni = kutu.text?.toString().orEmpty()
                Store.setLessonNote(this, lessonId, yeni)
                boyaNotSimgesi(btn)
                Toast.makeText(
                    this,
                    getString(if (yeni.isBlank()) R.string.lp_note_cleared else R.string.lp_note_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.lp_note_cancel, null)

        // Not varsa temizleme seçeneği de sun
        if (mevcut.isNotBlank()) {
            yapici.setNeutralButton(R.string.lp_note_clear) { _, _ ->
                Store.setLessonNote(this, lessonId, "")
                boyaNotSimgesi(btn)
                Toast.makeText(this, R.string.lp_note_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        yapici.show()
    }

    /**
     * v7.32: Dersi sesli dinleme. Arka planda çalar, bildirimden kontrol edilir.
     */
    private fun sesliDinle() {
        // Zaten bu ders çalıyorsa kontrol penceresi göster
        if (SesliDersServisi.calisiyor && SesliDersServisi.aktifAsset == assetPath) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tts_title)
                .setMessage(R.string.tts_playing)
                .setPositiveButton(
                    if (SesliDersServisi.duraklatildi) R.string.tts_resume
                    else R.string.tts_pause
                ) { _, _ ->
                    SesliDersServisi.komut(
                        this,
                        if (SesliDersServisi.duraklatildi) SesliDersServisi.EYLEM_DEVAM
                        else SesliDersServisi.EYLEM_DURAKLAT
                    )
                }
                .setNegativeButton(R.string.tts_stop) { _, _ ->
                    SesliDersServisi.komut(this, SesliDersServisi.EYLEM_DUR)
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
            return
        }

        // Hız seçimi
        val hizlar = arrayOf(
            getString(R.string.tts_speed_slow),
            getString(R.string.tts_speed_normal),
            getString(R.string.tts_speed_fast),
            getString(R.string.tts_speed_faster)
        )
        val degerler = floatArrayOf(0.8f, 1.0f, 1.25f, 1.5f)
        var secili = 1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tts_title)
            .setSingleChoiceItems(hizlar, secili) { _, hangi -> secili = hangi }
            .setPositiveButton(R.string.tts_start) { _, _ ->
                sesliBaslat(degerler[secili])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sesliBaslat(hiz: Float) {
        val bekleme = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tts_title)
            .setMessage(R.string.tts_preparing)
            .setCancelable(false)
            .show()

        Thread {
            val metin = DersMetni.metniAl(this, assetPath)
            runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (metin.isNullOrBlank()) {
                    Toast.makeText(this, R.string.rag_no_text, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val baslik = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
                SesliDersServisi.baslat(this, metin, baslik, assetPath, hiz)
                Toast.makeText(this, R.string.tts_started, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    /**
     * v7.31: Ders içeriğine soru sorma penceresi.
     * Yapay zekâ genel bilgisiyle değil, bu dersin metniyle cevap verir.
     */
    private fun derseSor() {
        if (!AiSettings.isReady(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rag_title)
                .setMessage(R.string.rag_need_ai)
                .setPositiveButton(R.string.ocr_open_settings) { _, _ ->
                    startActivity(
                        android.content.Intent(this, MainActivity::class.java)
                            .putExtra("open_screen", 7)
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val gorunum = layoutInflater.inflate(R.layout.dialog_derse_sor, null)
        val giris = gorunum.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.dsInput
        )
        val cevapKart = gorunum.findViewById<View>(R.id.dsAnswerCard)
        val cevapYazi = gorunum.findViewById<TextView>(R.id.dsAnswer)
        val durum = gorunum.findViewById<TextView>(R.id.dsStatus)
        val sayfaYazi = gorunum.findViewById<TextView>(R.id.dsPage)
        val cipler = gorunum.findViewById<com.google.android.material.chip.ChipGroup>(R.id.dsChips)

        val aktifSayfa = layoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)
        val toplam = renderer?.pageCount ?: 0
        sayfaYazi.text = getString(R.string.rag_page_info, aktifSayfa + 1, toplam)

        val dersAdi = intent?.getStringExtra(EXTRA_TITLE).orEmpty()

        // Soruyu gönderen ortak işlev
        fun gonder(soru: String) {
            if (soru.isBlank()) return
            durum.visibility = View.VISIBLE
            durum.setText(R.string.rag_thinking)
            cevapKart.visibility = View.GONE

            Thread {
                val cevap = DersAsistan.sor(this, assetPath, aktifSayfa, soru, dersAdi)
                runOnUiThread {
                    durum.visibility = View.GONE
                    cevapKart.visibility = View.VISIBLE
                    cevapYazi.text = cevap.metin
                    if (!cevap.ok) {
                        cevapYazi.setTextColor(GrafikDili.HATA)
                    }
                }
            }.start()
        }

        // Hazır soru çipleri
        DersAsistan.HAZIR_SORULAR.forEach { hs ->
            val cip = com.google.android.material.chip.Chip(this).apply {
                text = hs.etiket
                isCheckable = false
                textSize = 12f
                setOnClickListener {
                    giris.setText(hs.soru)
                    gonder(hs.soru)
                }
            }
            cipler.addView(cip)
        }

        val pencere = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rag_title)
            .setView(gorunum)
            .setPositiveButton(R.string.rag_ask, null)   // kapanmasın diye null
            .setNegativeButton(R.string.done, null)
            .create()
        pencere.show()

        // Varsayılan davranış diyaloğu kapatır; cevabı görebilmek için ezildi
        pencere.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            gonder(giris.text?.toString()?.trim().orEmpty())
        }
    }

    private fun sayfaAnahtari() = "sayfa_" + assetPath.replace('/', '_')

    /** Okuma ilerlemesi metnini günceller. */
    private fun updateProgress() {
        val total = renderer?.pageCount ?: 0
        if (total == 0) return
        val pos = layoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)
        val yuzde = ((pos + 1) * 100 / total).coerceIn(0, 100)
        progressText.text = getString(R.string.lp_progress, pos + 1, total, yuzde)
    }

    /** Gece modunu açar/kapatır. */
    private fun toggleNight() {
        nightMode = !nightMode
        prefs().edit().putBoolean(K_NIGHT, nightMode).apply()
        findViewById<TextView>(R.id.lpNight).text = if (nightMode) "☀" else "☾"
        applyNight()
        adapter.notifyDataSetChanged()
        Toast.makeText(
            this,
            getString(if (nightMode) R.string.lp_night_on else R.string.lp_night_off),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyNight() {
        recycler.setBackgroundColor(
            if (nightMode) Color.parseColor("#0B0E12") else Color.parseColor("#3A3F45")
        )
    }

    /** Yakınlaştırma çarpanını değiştirir ve sayfaları yeniden çizer. */
    private fun changeZoom(factor: Float, sessiz: Boolean = false) {
        val yeni = (zoom * factor).coerceIn(0.6f, 3.0f)
        if (kotlin.math.abs(yeni - zoom) < 0.02f) return
        zoom = yeni
        temizleCache()
        adapter.notifyDataSetChanged()
        if (!sessiz) {
            Toast.makeText(
                this,
                getString(R.string.lp_zoom, (zoom * 100).toInt()),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Assets'teki PDF'i önbellek klasörüne kopyalayıp açar. */
    private fun open(asset: String): Boolean {
        if (asset.isBlank()) return false
        return try {
            val out = File(cacheDir, "ders_" + asset.substringAfterLast('/'))
            if (!out.exists() || out.length() == 0L) {
                assets.open(asset).use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
            descriptor = ParcelFileDescriptor.open(
                out, ParcelFileDescriptor.MODE_READ_ONLY
            )
            renderer = PdfRenderer(descriptor!!)
            true
        } catch (e: Exception) {
            android.util.Log.w("LessonPdf", "PDF açılamadı: $asset", e)
            false
        }
    }

    /** Bellek sınırını aşan eski sayfaları serbest bırakır. */
    private fun budaCache() {
        while (cache.size > CACHE_LIMIT) {
            val ilk = cache.keys.firstOrNull() ?: break
            cache.remove(ilk)?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun temizleCache() {
        cache.values.forEach { if (!it.isRecycled) it.recycle() }
        cache.clear()
    }

    /** Bir sayfayı bitmap'e çevirir (genişliğe ve yakınlaştırmaya göre). */
    private fun renderPage(index: Int, targetWidth: Int): Bitmap? {
        cache[index]?.let { if (!it.isRecycled) return it }
        val r = renderer ?: return null
        return try {
            synchronized(r) {
                val page = r.openPage(index)
                val ratio = page.height.toFloat() / page.width.toFloat()
                val w = (targetWidth * zoom).toInt().coerceIn(320, 2400)
                val h = (w * ratio).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                cache[index] = bmp
                budaCache()
                bmp
            }
        } catch (e: Exception) {
            android.util.Log.w("LessonPdf", "Sayfa çizilemedi: $index", e)
            null
        }
    }

    override fun onPause() {
        super.onPause()
        // Kaldığı sayfayı kaydet
        try {
            val pos = layoutManager.findFirstVisibleItemPosition()
            if (pos >= 0) prefs().edit().putInt(sayfaAnahtari(), pos).apply()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            renderer?.close()
            descriptor?.close()
        } catch (_: Exception) {
        }
        temizleCache()
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.pgImage)
            val label: TextView = view.findViewById(R.id.pgLabel)
        }

        override fun getItemCount(): Int = renderer?.pageCount ?: 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_page, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val total = renderer?.pageCount ?: 0
            holder.label.text = getString(R.string.lp_page, position + 1, total)
            holder.image.setImageBitmap(null)
            holder.image.colorFilter = if (nightMode) nightFilter else null
            holder.image.post {
                val w = holder.image.width.takeIf { it > 0 }
                    ?: resources.displayMetrics.widthPixels
                val bmp = renderPage(position, w)
                if (bmp != null && !bmp.isRecycled) holder.image.setImageBitmap(bmp)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.97 — SAYFA YER İMİ (öneri 9)
    // ═══════════════════════════════════════════════════════════════

    /** Şu an görünen sayfa (0 tabanlı). */
    private fun aktifSayfaNo(): Int =
        layoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)

    /**
     * Bu sayfaya yer imi ekler / düzenler.
     *
     * PDF bitmap olarak çizildiği için metin seçilemiyor; en yakın
     * karşılık sayfa bazlı işaret + not.
     */
    private fun sayfaImiPenceresi() {
        val sayfa = aktifSayfaNo()
        val mevcut = SayfaImi.sayfaImi(this, lessonId, sayfa)
        val dersAdi = intent?.getStringExtra(EXTRA_TITLE).orEmpty()

        val yogunluk = resources.displayMetrics.density
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (22 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (22 * yogunluk).toInt(), 0
            )
        }

        val giris = android.widget.EditText(this).apply {
            setText(mevcut?.not.orEmpty())
            hint = getString(R.string.si_not_ipucu)
            minLines = 2
            maxLines = 5
        }
        kap.addView(giris)

        // Renk seçimi
        kap.addView(TextView(this).apply {
            text = getString(R.string.si_renk)
            textSize = 12.5f
            alpha = 0.75f
            setPadding(0, (14 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
        })

        var secilenRenk = mevcut?.renk ?: 0
        val renkSatiri = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val renkGorunumleri = mutableListOf<TextView>()
        SayfaImi.renkler.forEachIndexed { i, renk ->
            val nokta = TextView(this).apply {
                text = SayfaImi.renkAdi(this@LessonPdfActivity, i)
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setPadding(
                    (8 * yogunluk).toInt(), (8 * yogunluk).toInt(),
                    (8 * yogunluk).toInt(), (8 * yogunluk).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { rightMargin = (4 * yogunluk).toInt() }
                isClickable = true
            }
            renkGorunumleri.add(nokta)
            renkSatiri.addView(nokta)
        }
        fun renkBoya() {
            renkGorunumleri.forEachIndexed { i, gorunum ->
                val secili = i == secilenRenk
                gorunum.setBackgroundColor(
                    if (secili) SayfaImi.renkler[i]
                    else (SayfaImi.renkler[i] and 0x33FFFFFF)
                )
                gorunum.setTextColor(if (secili) 0xFF000000.toInt() else 0xFF888888.toInt())
            }
        }
        renkGorunumleri.forEachIndexed { i, gorunum ->
            gorunum.setOnClickListener { secilenRenk = i; renkBoya() }
        }
        renkBoya()
        kap.addView(renkSatiri)

        val yapici = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.si_baslik, sayfa + 1))
            .setView(android.widget.ScrollView(this).apply { addView(kap) })
            .setPositiveButton(R.string.lp_note_save) { _, _ ->
                SayfaImi.kaydet(
                    this, lessonId, sayfa,
                    giris.text?.toString().orEmpty(), secilenRenk, dersAdi
                )
                Toast.makeText(this, R.string.si_kaydedildi, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.si_liste) { _, _ -> imListesi() }

        if (mevcut != null) {
            yapici.setNegativeButton(R.string.delete) { _, _ ->
                SayfaImi.sil(this, lessonId, sayfa)
                Toast.makeText(this, R.string.si_silindi, Toast.LENGTH_SHORT).show()
            }
        } else {
            yapici.setNegativeButton(R.string.cancel, null)
        }
        yapici.show()
    }

    /** Bu dersteki tüm yer imleri — dokununca o sayfaya atlar. */
    private fun imListesi() {
        val imler = SayfaImi.dersImleri(this, lessonId)
        if (imler.isEmpty()) {
            Toast.makeText(this, R.string.si_bos, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = imler.map { im ->
            val etiket = SayfaImi.renkAdi(this, im.renk)
            "s.${im.sayfa + 1} · $etiket" +
                (if (im.not.isNotBlank()) "\n   ${im.not.take(50)}" else "")
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.si_liste_baslik, imler.size))
            .setItems(adlar) { _, hangi ->
                runCatching {
                    layoutManager.scrollToPositionWithOffset(imler[hangi].sayfa, 0)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
