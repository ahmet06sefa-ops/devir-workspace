package com.gunlukasistan.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

/**
 * v10.3 · Öneri B19 — Durum çubuğunda "canlı dakika" ikonu.
 *
 * ── Ne değişiyor ──
 * Sayaç bildiriminin küçük ikonu (`setSmallIcon`) hep sabit
 * `ic_timer` idi: süre dolarken durum çubuğunda hep aynı kum
 * saati. Artık ikon **dakikayı kendi üzerinde taşıyor**:
 *
 *   · Geri sayım — kalan dakika yukarı yuvarlanır: 04:31 → "5".
 *     Kullanıcı göz ucuyla "5 dakikam kaldı" der; "4" beklenmedik
 *     şekilde erken bitti hissi vermez.
 *   · Kronometre — geçen dakika aşağı yuvarlanır: 12:47 → "12".
 *     "12 dakikadır çalışıyorum" cümlesi kurulabilir.
 *
 * Sınır: rakam tek haneli ya da çift haneli kalsın diye 99'da
 * kestirilir (100+ dakikalık oturumlarda "99" gösterilir).
 *
 * ── Neden bitmap ──
 * `IconCompat.createWithBitmap` API 23'ten beri destekleniyor;
 * 99 ayrı drawable üretmek yerine sayıyı çalışma anında tuvale
 * yazıyoruz. Küçük ikonlar alfa maskesiyle çizildiği için dolgu
 * beyaz: sistem koyu/açık temada kendisi renklendirir.
 *
 * ── Saf bölge ──
 * [gosterilecekSayi] saf fonksiyon; birim testli. Bitmap üretimi
 * cihazda çalışır, testlerde çağrılmaz (android.graphics stub).
 */
object SayacIkon {

    const val SINIR = 99

    /**
     * Bildirim ikonunda gösterilecek dakika.
     *
     * @param degerMs   sayacın o anki gösterim değeri (kalan ya da geçen)
     * @param geriSayim true → kalan (yukarı yuvarla), false → geçen (aşağı)
     */
    fun gosterilecekSayi(degerMs: Long, geriSayim: Boolean): Int {
        if (degerMs <= 0L) return if (geriSayim) 0 else 0
        val dk = if (geriSayim) {
            (degerMs + 59_999L) / 60_000L
        } else {
            degerMs / 60_000L
        }
        return dk.coerceIn(0L, SINIR.toLong()).toInt()
    }

    /** Beyaz rakam taşıyan küçük ikon. 1 hane büyük, 2 hane sığdırılır. */
    fun ikonCompat(sayi: Int): IconCompat {
        val temiz = sayi.coerceIn(0, SINIR)
        val boyut = 144
        val bmp = Bitmap.createBitmap(boyut, boyut, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bmp)
        val boya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (temiz < 10) boyut * 0.82f else boyut * 0.58f
        }
        val taban = boyut / 2f - (boya.descent() + boya.ascent()) / 2f
        tuval.drawText(temiz.toString(), boyut / 2f, taban, boya)
        return IconCompat.createWithBitmap(bmp)
    }
}
