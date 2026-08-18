package com.gunlukasistan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * v7.19 — Fotoğrafı yapay zekâya göndermeden önce hazırlar.
 *
 * El yazısı okumada başarı büyük ölçüde görsel kalitesine bağlıdır.
 * Bu sınıf üç iş yapar:
 *   1. EXIF'e göre döndürür (telefon yan tutulduysa yazı yan kalmasın)
 *   2. Çözünürlüğü dengeler — çok küçükse harfler bozulur, çok büyükse istek reddedilir
 *   3. Kontrastı artırır — soluk kurşun kalem yazısı belirginleşir
 */
object GorselHazirla {

    private const val TAG = "GorselHazirla"

    /** Uzun kenar hedefi. El yazısı için 1600 px iyi bir denge. */
    private const val HEDEF_UZUN_KENAR = 1600

    /** İstek boyutu sınırı (base64 sonrası ~4/3 büyür). */
    private const val MAX_BAYT = 1_400_000

    /**
     * Görseli okumaya hazır base64 JPEG'e çevirir.
     * @param netlestir kontrast artırma uygulansın mı
     * @return base64 metin, başarısızsa null
     */
    fun base64Uret(context: Context, uri: Uri, netlestir: Boolean = true): String? {
        return try {
            var bmp = oku(context, uri) ?: return null
            bmp = dondur(context, uri, bmp)
            bmp = olcekle(bmp)
            if (netlestir) bmp = kontrastArtir(bmp)
            sikistirVeKodla(bmp)
        } catch (e: OutOfMemoryError) {
            android.util.Log.w(TAG, "Görsel çok büyük", e)
            null
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Görsel hazırlanamadı", e)
            null
        }
    }

    /** Bellek dostu okuma — önce boyutu öğren, sonra örnekleyerek yükle. */
    private fun oku(context: Context, uri: Uri): Bitmap? {
        val olcuSecenek = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, olcuSecenek)
        }
        val enBuyuk = maxOf(olcuSecenek.outWidth, olcuSecenek.outHeight)
        if (enBuyuk <= 0) return null

        // Hedefin iki katına kadar indir — sonra hassas ölçekleme yapılacak
        var ornek = 1
        while (enBuyuk / ornek > HEDEF_UZUN_KENAR * 2) ornek *= 2

        val secenek = BitmapFactory.Options().apply {
            inSampleSize = ornek
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, secenek)
        }
    }

    /** EXIF yön bilgisine göre düzeltir. */
    private fun dondur(context: Context, uri: Uri, bmp: Bitmap): Bitmap {
        return try {
            val yon = context.contentResolver.openInputStream(uri)?.use { girdi ->
                ExifInterface(girdi).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: return bmp

            val m = Matrix()
            when (yon) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                else -> return bmp
            }
            val yeni = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (yeni != bmp) bmp.recycle()
            yeni
        } catch (e: Exception) {
            android.util.Log.w(TAG, "EXIF okunamadı", e)
            bmp
        }
    }

    /** Uzun kenarı hedefe getirir. Küçük görselleri büyütmez (yazı bulanıklaşır). */
    private fun olcekle(bmp: Bitmap): Bitmap {
        val uzun = maxOf(bmp.width, bmp.height)
        if (uzun <= HEDEF_UZUN_KENAR) return bmp
        val oran = HEDEF_UZUN_KENAR.toFloat() / uzun
        val yeni = Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * oran).toInt().coerceAtLeast(1),
            (bmp.height * oran).toInt().coerceAtLeast(1),
            true
        )
        if (yeni != bmp) bmp.recycle()
        return yeni
    }

    /**
     * Kontrastı artırır ve hafif doygunluk düşürür.
     * Kurşun kalem ve soluk mürekkep bu sayede belirginleşir.
     * Tam siyah-beyaza çevirmiyoruz — renkli kalemle yazılmış notlarda bilgi kaybolmasın.
     */
    private fun kontrastArtir(bmp: Bitmap): Bitmap {
        return try {
            val hedef = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val tuval = Canvas(hedef)

            val kontrast = 1.45f          // 1.0 = değişiklik yok
            val parlaklik = -18f          // hafif koyulaştır, kağıt beyazı kalsın
            val doygunluk = 0.55f         // renk gürültüsünü azalt

            val doygunlukM = ColorMatrix().apply { setSaturation(doygunluk) }
            val kontrastM = ColorMatrix(
                floatArrayOf(
                    kontrast, 0f, 0f, 0f, parlaklik,
                    0f, kontrast, 0f, 0f, parlaklik,
                    0f, 0f, kontrast, 0f, parlaklik,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            doygunlukM.postConcat(kontrastM)

            val boya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(doygunlukM)
            }
            tuval.drawBitmap(bmp, 0f, 0f, boya)
            bmp.recycle()
            hedef
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kontrast uygulanamadı", e)
            bmp
        }
    }

    /** Boyut sınırına sığana kadar kaliteyi düşürerek JPEG'e çevirir. */
    private fun sikistirVeKodla(bmp: Bitmap): String? {
        var kalite = 92
        var veri: ByteArray
        do {
            val cikis = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, kalite, cikis)
            veri = cikis.toByteArray()
            cikis.close()
            if (veri.size <= MAX_BAYT) break
            kalite -= 12
        } while (kalite >= 45)

        bmp.recycle()
        if (veri.isEmpty()) return null
        return Base64.encodeToString(veri, Base64.NO_WRAP)
    }

    /** Önizleme için küçük bitmap (diyalogda gösterilir). */
    fun onizleme(context: Context, uri: Uri, enFazla: Int = 520): Bitmap? {
        return try {
            val olcu = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, olcu)
            }
            val buyuk = maxOf(olcu.outWidth, olcu.outHeight)
            if (buyuk <= 0) return null
            var ornek = 1
            while (buyuk / ornek > enFazla * 2) ornek *= 2

            val secenek = BitmapFactory.Options().apply { inSampleSize = ornek }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, secenek)
            } ?: return null
            dondur(context, uri, bmp)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önizleme üretilemedi", e)
            null
        }
    }
}
