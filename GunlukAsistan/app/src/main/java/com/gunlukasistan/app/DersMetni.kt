package com.gunlukasistan.app

import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * v7.31 — Ders PDF'inden metin çıkarır ve önbelleğe alır.
 *
 * Yapay zekânın "bu dersle ilgili" cevap verebilmesi için PDF metnine
 * ihtiyaç var. `PdfRenderer` metin veremez (sadece görüntü), bu yüzden
 * pdfbox kullanılıyor.
 *
 * Tasarım kararları:
 *  · Metin **bir kez** çıkarılıp diskte önbelleğe alınır — her soruda
 *    yeniden ayrıştırmak yavaş ve gereksiz.
 *  · Sayfa sayfa saklanır: kullanıcı hangi sayfadaysa **o sayfa** ve
 *    komşuları öncelikli gönderilir. Böylece istem küçük kalır.
 *  · Token sınırı için akıllı kırpma yapılır.
 */
object DersMetni {

    private const val TAG = "DersMetni"
    private const val ONBELLEK = "ders_metin"

    /** Modele gönderilecek en fazla karakter (~4 karakter = 1 token). */
    private const val MAX_KARAKTER = 12000

    private fun onbellekKlasoru(context: Context): File =
        File(context.cacheDir, ONBELLEK).apply { if (!exists()) mkdirs() }

    private fun dosyaAdi(assetPath: String): String =
        assetPath.replace('/', '_').replace(".pdf", "") + ".txt"

    /**
     * PDF'in tüm metnini döndürür. İlk çağrıda çıkarır, sonra önbellekten okur.
     * @return metin, çıkarılamadıysa null
     */
    fun metniAl(context: Context, assetPath: String): String? {
        if (assetPath.isBlank()) return null

        // 1. Önbellekte var mı
        val onbellek = File(onbellekKlasoru(context), dosyaAdi(assetPath))
        if (onbellek.exists() && onbellek.length() > 0) {
            return try {
                onbellek.readText()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Önbellek okunamadı", e)
                null
            }
        }

        // 2. PDF'ten çıkar
        return try {
            PdfSplitter.init(context)   // pdfbox font önbelleği
            context.assets.open(assetPath).use { girdi ->
                PDDocument.load(girdi).use { belge ->
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                        // Sayfa ayracı koy — hangi sayfada olduğu bilinsin
                        pageEnd = "\n<<<SAYFA>>>\n"
                    }
                    val metin = stripper.getText(belge).trim()
                    if (metin.isBlank()) return null
                    try {
                        onbellek.writeText(metin)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Önbelleğe yazılamadı", e)
                    }
                    metin
                }
            }
        } catch (e: OutOfMemoryError) {
            android.util.Log.w(TAG, "PDF çok büyük", e)
            null
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Metin çıkarılamadı: $assetPath", e)
            null
        }
    }

    /** Metni sayfalara böler. */
    fun sayfalar(metin: String): List<String> =
        metin.split("<<<SAYFA>>>").map { it.trim() }.filter { it.isNotBlank() }

    /**
     * Soruya gönderilecek bağlamı hazırlar.
     *
     * Öncelik sırası:
     *   1. Kullanıcının bulunduğu sayfa
     *   2. Önceki ve sonraki sayfa (konu bütünlüğü)
     *   3. Yer kalırsa baştan itibaren diğer sayfalar
     *
     * Böylece "bu tabloyu açıkla" gibi sorular doğru sayfaya denk gelir.
     */
    fun baglamHazirla(metin: String, aktifSayfa: Int): String {
        val sayfaListe = sayfalar(metin)
        if (sayfaListe.isEmpty()) return metin.take(MAX_KARAKTER)
        if (sayfaListe.size == 1) return sayfaListe[0].take(MAX_KARAKTER)

        val sonuc = StringBuilder()
        val eklenen = mutableSetOf<Int>()

        fun ekle(i: Int, etiket: String) {
            if (i !in sayfaListe.indices || i in eklenen) return
            val parca = sayfaListe[i]
            if (sonuc.length + parca.length > MAX_KARAKTER) return
            if (sonuc.isNotEmpty()) sonuc.append("\n\n")
            sonuc.append("[").append(etiket).append("]\n").append(parca)
            eklenen.add(i)
        }

        val a = aktifSayfa.coerceIn(0, sayfaListe.size - 1)
        ekle(a, "Sayfa ${a + 1} — kullanıcının baktığı sayfa")
        ekle(a + 1, "Sayfa ${a + 2}")
        ekle(a - 1, "Sayfa $a")

        // Kalan yeri baştan doldur
        for (i in sayfaListe.indices) {
            if (sonuc.length >= MAX_KARAKTER * 0.9) break
            ekle(i, "Sayfa ${i + 1}")
        }
        return sonuc.toString()
    }

    /** Ders metni önbellekte var mı — hazır olup olmadığını göstermek için. */
    fun hazirMi(context: Context, assetPath: String): Boolean =
        File(onbellekKlasoru(context), dosyaAdi(assetPath)).let { it.exists() && it.length() > 0 }

    /** Önbelleği temizler (disk boşaltmak için). */
    fun onbellegiTemizle(context: Context): Int {
        return try {
            val klasor = onbellekKlasoru(context)
            val dosyalar = klasor.listFiles() ?: return 0
            var n = 0
            dosyalar.forEach { if (it.delete()) n++ }
            n
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önbellek temizlenemedi", e)
            0
        }
    }

    /** Önbellek boyutu (KB). */
    fun onbellekBoyutu(context: Context): Long {
        return try {
            (onbellekKlasoru(context).listFiles() ?: return 0)
                .sumOf { it.length() } / 1024
        } catch (_: Exception) {
            0
        }
    }
}
