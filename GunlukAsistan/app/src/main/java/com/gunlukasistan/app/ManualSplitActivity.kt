package com.gunlukasistan.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

/**
 * Elle bölme noktası seçme ekranı (v6.9).
 *
 * Kullanıcı sayfaları tek tek gezer, bölümün başladığı **tam noktaya** dokunur.
 * Sayfa ortasından başlayan bölümler için dikey konum da kaydedilir.
 */
class ManualSplitActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BOOK_ID = "book_id"
        /** Sonuç: "sayfa:oran" biçiminde noktalar, virgülle ayrılmış. */
        const val RESULT_POINTS = "points"
    }

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var pageIndex = 0
    private var pageCount = 0

    /** Seçilen bölme noktaları: sayfa → üstten oran (0f..1f). */
    private val points = linkedMapOf<Int, Float>()

    private lateinit var pageView: ImageView
    private lateinit var infoText: TextView
    private lateinit var listText: TextView
    private var currentBitmap: Bitmap? = null
    /** Görüntülenen sayfada dokunulan son oran (çizgi çizmek için). */
    private var previewRatio: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_split)

        pageView = findViewById(R.id.msPage)
        infoText = findViewById(R.id.msInfo)
        listText = findViewById(R.id.msList)

        val uriStr = intent.getStringExtra(EXTRA_URI)
        if (uriStr.isNullOrBlank()) {
            finish(); return
        }

        if (!openPdf(Uri.parse(uriStr))) {
            Toast.makeText(this, R.string.ms_open_error, Toast.LENGTH_LONG).show()
            finish(); return
        }

        findViewById<View>(R.id.msPrev).setOnClickListener { showPage(pageIndex - 1) }
        findViewById<View>(R.id.msNext).setOnClickListener { showPage(pageIndex + 1) }
        findViewById<View>(R.id.msMarkTop).setOnClickListener { addPoint(0f) }
        findViewById<View>(R.id.msUndo).setOnClickListener { undoLast() }
        findViewById<View>(R.id.msDone).setOnClickListener { finishWithResult() }
        findViewById<View>(R.id.msCancel).setOnClickListener { finish() }

        // Sayfaya dokunma → o noktadan böl
        pageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val ratio = (event.y / v.height.toFloat()).coerceIn(0f, 1f)
                previewRatio = ratio
                confirmPoint(ratio)
                v.performClick()
            }
            true
        }

        showPage(0)
    }

    private fun openPdf(uri: Uri): Boolean {
        return try {
            descriptor = contentResolver.openFileDescriptor(uri, "r") ?: return false
            renderer = PdfRenderer(descriptor!!)
            pageCount = renderer?.pageCount ?: 0
            pageCount > 0
        } catch (_: Exception) {
            false
        }
    }

    /** Sayfayı görüntüler, üzerine seçili bölme çizgilerini çizer. */
    private fun showPage(index: Int) {
        val r = renderer ?: return
        val i = index.coerceIn(0, pageCount - 1)
        pageIndex = i
        try {
            r.openPage(i).use { page ->
                // Ekran genişliğine göre ölçekle (bellek dostu)
                val targetW = resources.displayMetrics.widthPixels.coerceAtMost(1080)
                val scale = targetW.toFloat() / page.width
                val w = targetW
                val h = (page.height * scale).roundToInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Bu sayfadaki bölme çizgisini üzerine çiz
                points[i]?.let { ratio ->
                    val c = Canvas(bmp)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#E53935")
                        strokeWidth = 5f
                    }
                    val y = h * ratio
                    c.drawLine(0f, y, w.toFloat(), y, paint)
                    paint.textSize = 34f
                    paint.style = Paint.Style.FILL
                    c.drawText("✂ bölme", 16f, (y - 12f).coerceAtLeast(36f), paint)
                }

                currentBitmap?.recycle()
                currentBitmap = bmp
                pageView.setImageBitmap(bmp)
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.ms_render_error, Toast.LENGTH_SHORT).show()
        }
        updateInfo()
    }

    /** Dokunulan noktayı onaylatır (yanlışlıkla eklemeyi önler). */
    private fun confirmPoint(ratio: Float) {
        val percent = (ratio * 100).roundToInt()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ms_add_title)
            .setMessage(getString(R.string.ms_add_msg, pageIndex + 1, percent))
            .setPositiveButton(R.string.ms_add_ok) { _, _ -> addPoint(ratio) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addPoint(ratio: Float) {
        points[pageIndex] = ratio
        showPage(pageIndex)
        Toast.makeText(this, R.string.ms_added, Toast.LENGTH_SHORT).show()
    }

    private fun undoLast() {
        if (points.isEmpty()) return
        val last = points.keys.lastOrNull() ?: return
        points.remove(last)
        showPage(pageIndex)
    }

    private fun updateInfo() {
        infoText.text = getString(R.string.ms_page_of, pageIndex + 1, pageCount)
        listText.text = if (points.isEmpty()) {
            getString(R.string.ms_no_points)
        } else {
            points.entries.sortedBy { it.key }.joinToString("\n") { (p, r) ->
                val pct = (r * 100).roundToInt()
                if (pct == 0) {
                    getString(R.string.ms_point_top, p + 1)
                } else {
                    getString(R.string.ms_point_mid, p + 1, pct)
                }
            }
        }
    }

    private fun finishWithResult() {
        if (points.isEmpty()) {
            Toast.makeText(this, R.string.ms_need_point, Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = points.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}:${it.value}" }
        setResult(RESULT_OK, Intent().putExtra(RESULT_POINTS, encoded))
        finish()
    }

    override fun onDestroy() {
        try { renderer?.close() } catch (_: Exception) {}
        try { descriptor?.close() } catch (_: Exception) {}
        currentBitmap?.recycle()
        super.onDestroy()
    }
}
