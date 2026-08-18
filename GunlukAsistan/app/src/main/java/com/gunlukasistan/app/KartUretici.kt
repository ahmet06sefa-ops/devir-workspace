package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.14 · ULTRA-30 / E29 — Görev paylaşım kartı (+ E30 özet kartı).
 *
 * ── Tarama kanıtı ──
 * Dışa aktarım yedek/JSON ve metin raporuyla ([AnalitikActivity.raporPaylas])
 * sınırlıydı; görsel kart üreten hiçbir şey yoktu.
 *
 * ── Üretim ──
 * 1080×1350 (4:5) PNG: tema zemin gradyanı, büyük emoji, görev metni
 * (sözcük kaydırmacalı), tarih ve seri altı bilgi. [satirlaraBol]
 * framework'süzdür ve birim testlidir.
 *
 * ── Paylaşım ──
 * Önbelleğe yazılır, FileProvider ile eylem menüsü açılır; dosya her
 * paylaşımda üstüne yazılır (çöp birikmez).
 */
object KartUretici {

    private const val TAG = "KartUretici"
    private const val GEN = 1080
    private const val YUK = 1350

    // ---------------- Saf kaydırma (birim testli) ----------------

    /**
     * Metni sözcük sınırında en çok [maxSatir] satıra böler.
     * Satıra sığmayan uzun sözcük sert kırılır; son satır sığmazsa
     * "…" ile kapatılır.
     */
    fun satirlaraBol(metin: String, satirGenisligi: Int = 24, maxSatir: Int = 4): List<String> {
        val temiz = metin.trim().replace(Regex("\\s+"), " ")
        if (temiz.isEmpty() || satirGenisligi < 4) return emptyList()
        val kelimeler = temiz.split(" ")
        val satirlar = mutableListOf<String>()
        var satir = ""
        var i = 0
        while (i < kelimeler.size && satirlar.size < maxSatir) {
            var k = kelimeler[i]
            // Sığmayan uzun sözcük: sert kır
            while (k.length > satirGenisligi) {
                val parca = if (satir.isEmpty()) k.take(satirGenisligi) else null
                if (parca != null) {
                    satirlar.add(parca)
                    k = k.substring(satirGenisligi)
                } else {
                    satirlar.add(satir)
                    satir = ""
                }
                if (satirlar.size >= maxSatir) break
            }
            if (satirlar.size >= maxSatir) break
            val aday = if (satir.isEmpty()) k else "$satir $k"
            if (aday.length <= satirGenisligi) {
                satir = aday
                i++
            } else {
                if (satir.isNotEmpty()) satirlar.add(satir)
                satir = ""
            }
        }
        // Döngü tüm sözcükleri tüketmişse taşma yoktur (üç nokta gerekmez);
        // erken çıkıldıysa (maxSatir duvarı) kalanlar "…" ile işaretlenir.
        val bitti = i >= kelimeler.size
        if (satir.isNotEmpty() && satirlar.size < maxSatir) satirlar.add(satir)
        if (!bitti && satirlar.isNotEmpty()) {
            val son = satirlar.last().trimEnd()
            satirlar[satirlar.size - 1] =
                (if (son.length >= satirGenisligi) son.dropLast(1) else son) + "…"
        }
        return satirlar
    }

    // ---------------- Görev kartı ----------------

    fun gorevKarti(context: Context, gorev: Store.Task): Bitmap {
        val bmp = Bitmap.createBitmap(GEN, YUK, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bmp)
        zemin(context, tuval)

        val dis = 84f
        // Üst rozet + telif
        yazi(tuval, "🗒", dis, dis + 96f, 84f, SOL, 0xFFFFFFFF.toInt())
        yazi(
            tuval,
            context.getString(R.string.app_name),
            GEN - dis, dis + 30f, 30f, SAG, 0x66FFFFFF
        )
        yazi(
            tuval, tarihMetni(context),
            GEN - dis, dis + 72f, 28f, SAG, 0x52FFFFFF
        )

        // Orta: durum + görev metni
        val etiketEmoji = Etiket.bul(gorev.etiket)?.emoji ?: "•"
        val durumEmoji = if (gorev.done) "✅" else etiketEmoji
        yazi(tuval, durumEmoji, GEN / 2f, YUK * 0.36f, 130f, ORTA, 0xFFFFFFFF.toInt())

        val satirlar = satirlaraBol(gorev.text, 24, 4)
        satirlar.forEachIndexed { i, satir ->
            yazi(
                tuval, satir, GEN / 2f, YUK * 0.47f + i * 74f, 56f, ORTA,
                0xFFF5EFE8.toInt(), kalin = true
            )
        }

        // Alt bant: kalan/biten durumu + seri
        val (seri, _) = Store.streakInfo(context)
        val altSatir = if (gorev.done) {
            context.getString(R.string.ge_kart_tamam, seri)
        } else {
            context.getString(R.string.ge_kart_bekliyor)
        }
        bant(tuval, altSatir, context)
        return bmp
    }

    // ---------------- Sene özeti kartı (E30) ----------------

    fun seneKarti(context: Context, ozet: SeneFilmi.Ozet): Bitmap {
        val bmp = Bitmap.createBitmap(GEN, YUK, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bmp)
        zemin(context, tuval)

        val dis = 84f
        yazi(tuval, "🎬", GEN / 2f, YUK * 0.16f, 120f, ORTA, 0xFFFFFFFF.toInt())
        yazi(
            tuval, context.getString(R.string.ge_film_kart_baslik, ozet.yil),
            GEN / 2f, YUK * 0.27f, 60f, ORTA, 0xFFF5EFE8.toInt(), kalin = true
        )

        val satirlar = listOf(
            "🔥 " + context.getString(R.string.ge_film_seri, ozet.enUzunSeri),
            "📅 " + context.getString(
                R.string.ge_film_ay,
                SeneFilmi.ayAdi(context, ozet.enCaliskanAy), ozet.enCaliskanAyDk
            ),
            "🏆 " + context.getString(
                R.string.ge_film_gun, ozet.enUzunGunMetin, ozet.enUzunGunDk
            ),
            "⏱ " + context.getString(R.string.ge_film_toplam, ozet.toplamDk)
        )
        satirlar.forEachIndexed { i, satir ->
            yazi(
                tuval, satir, GEN / 2f, YUK * 0.42f + i * 84f, 42f, ORTA,
                0xE6FFFFFF.toInt()
            )
        }
        yazi(
            tuval, "🐹 Pofi · " + context.getString(R.string.app_name),
            GEN / 2f, YUK - dis, 30f, ORTA, 0x80FFFFFF.toInt()
        )
        return bmp
    }

    // ---------------- Paylaşım ----------------

    fun paylas(context: Context, bmp: Bitmap, dosyaAdi: String) {
        try {
            val klasor = File(context.cacheDir, "kartlar").apply { mkdirs() }
            val dosya = File(klasor, dosyaAdi)
            FileOutputStream(dosya).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", dosya
            )
            val niyet = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(niyet, context.getString(R.string.ge_kart_paylas))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Paylaşılamadı", e)
            android.widget.Toast.makeText(
                context, R.string.ge_kart_paylas_hata, android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ---------------- İç çizim yardımcıları ----------------

    private const val SOL = 0
    private const val ORTA = 1
    private const val SAG = 2

    private fun zemin(context: Context, tuval: Canvas) {
        val spec = try {
            ThemeManager.specs[ThemeManager.selected(context)]
        } catch (e: Exception) {
            null
        }
        val ust = spec?.cardColor ?: 0xFF2A2118.toInt()
        val altSpec = try {
            ThemeManager.specs[(ThemeManager.selected(context) + 2) % ThemeManager.specs.size]
        } catch (e: Exception) {
            null
        }
        val alt = altSpec?.cardColor ?: 0xFF1A140E.toInt()
        val boya = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, YUK.toFloat(),
                koyultma(ust, 0.16f), koyultma(alt, 0.30f), Shader.TileMode.CLAMP
            )
        }
        tuval.drawRoundRect(
            RectF(0f, 0f, GEN.toFloat(), YUK.toFloat()), 0f, 0f, boya
        )
        // İnce iç çerçeve
        val cerceve = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0x40FFFFFF
        }
        tuval.drawRoundRect(
            RectF(30f, 30f, GEN - 30f, YUK - 30f), 36f, 36f, cerceve
        )
    }

    private fun koyultma(renk: Int, oran: Float): Int {
        val t = oran.coerceIn(0f, 1f)
        return android.graphics.Color.rgb(
            (android.graphics.Color.red(renk) * (1 - t)).toInt(),
            (android.graphics.Color.green(renk) * (1 - t)).toInt(),
            (android.graphics.Color.blue(renk) * (1 - t)).toInt()
        )
    }

    private fun yazi(
        tuval: Canvas, metin: String, x: Float, taban: Float,
        boyut: Float, hiza: Int, renk: Int, kalin: Boolean = false
    ) {
        val boya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = boyut
            color = renk
            isFakeBoldText = kalin
            textAlign = when (hiza) {
                SOL -> Paint.Align.LEFT
                SAG -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
        }
        tuval.drawText(metin, x, taban, boya)
    }

    /** Alt şerit: durum satırı. */
    private fun bant(tuval: Canvas, satir: String, context: Context) {
        val serit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x33000000
        }
        tuval.drawRoundRect(
            RectF(84f, YUK - 210f, GEN - 84f, YUK - 128f), 28f, 28f, serit
        )
        yazi(tuval, satir, GEN / 2f, YUK - 157f, 34f, ORTA, 0xD9FFFFFF.toInt())
    }

    private fun tarihMetni(context: Context): String =
        SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR"))
            .format(Date(System.currentTimeMillis()))
}
