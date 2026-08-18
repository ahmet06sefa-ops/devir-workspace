package com.gunlukasistan.app

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.81 — Konu anlatımını PDF'e döker.
 *
 * ── Kullanıcının isteği ──
 * "PDF olarak uzun uzun destekleme"
 *
 * ── Türkçe karakter sorunu ──
 * PDFBox'ın gömülü `Helvetica` fontu **WinAnsi** kodlamasında; ş/ğ/ı/İ
 * karakterlerini yazmaya çalışınca `IllegalArgumentException` fırlatır ve
 * PDF üretimi çöker. Bu yüzden `assets/fonts/poppins_regular.ttf`
 * gömülüyor ([PDType0Font.load] Unicode destekler).
 *
 * Font yüklenemezse ([yedekFont]) Helvetica'ya düşülür ve metin
 * ASCII'ye sadeleştirilir — PDF bozuk çıkacağına Türkçesiz çıksın.
 *
 * ── Neden PdfBox ──
 * Android'in kendi `PdfDocument` sınıfı Canvas tabanlı; metin sarma,
 * sayfa taşması ve font gömme işlerini elle yapmak gerekir. PdfBox zaten
 * projede var (PDF bölme için) ve metin akışını daha iyi yönetiyor.
 */
object KonuPdf {

    private const val TAG = "KonuPdf"

    // Sayfa ölçüleri (punto)
    private const val KENAR = 50f
    private const val SATIR_ARA = 1.45f

    private const val BOY_BASLIK = 20f
    private const val BOY_BOLUM = 13.5f
    private const val BOY_METIN = 10.5f
    private const val BOY_DIPNOT = 8.5f

    /**
     * Tek bir konu anlatımını PDF'e yazar.
     *
     * @return oluşan dosya, hata olursa null
     */
    fun tekAnlatim(
        context: Context,
        anlatim: KonuUretici.Anlatim,
        konuAdi: String = ""
    ): File? = uret(context, konuAdi.ifBlank { anlatim.baslik }, listOf(anlatim))

    /**
     * Bir konunun **tüm maddelerini** tek PDF'te toplar — "kitap" çıktısı.
     *
     * @param anlatimlar sırayla yazılacak anlatımlar
     */
    fun kitap(
        context: Context,
        konuAdi: String,
        anlatimlar: List<KonuUretici.Anlatim>
    ): File? = uret(context, konuAdi, anlatimlar, icindekiler = true)

    // ═══════════════════════════════════════════════════════════════
    // ÜRETİM
    // ═══════════════════════════════════════════════════════════════

    private fun uret(
        context: Context,
        baslik: String,
        anlatimlar: List<KonuUretici.Anlatim>,
        icindekiler: Boolean = false
    ): File? {
        if (anlatimlar.isEmpty()) return null

        var belge: PDDocument? = null
        return try {
            PdfSplitter.init(context)
            belge = PDDocument()

            val normal = fontYukle(context, belge, "fonts/poppins_regular.ttf")
            val kalin = fontYukle(context, belge, "fonts/poppins_bold.ttf") ?: normal
            val unicodeVar = normal != null

            val yaziciNormal = normal ?: PDType1Font.HELVETICA
            val yaziciKalin = kalin ?: PDType1Font.HELVETICA_BOLD

            val ctx = Yazici(belge, yaziciNormal, yaziciKalin, unicodeVar)

            // ── Kapak ──
            ctx.yeniSayfa()
            ctx.y = ctx.sayfaYuksekligi - 140f
            ctx.metin(baslik, BOY_BASLIK + 4, kalinMi = true)
            ctx.bosluk(10f)
            ctx.metin(
                context.getString(R.string.kp_kapak_alt, anlatimlar.size),
                BOY_METIN
            )
            ctx.bosluk(6f)
            ctx.metin(tarihMetni(), BOY_DIPNOT)
            ctx.bosluk(4f)
            ctx.metin(context.getString(R.string.kp_kapak_uygulama), BOY_DIPNOT)

            // ── İçindekiler ──
            if (icindekiler && anlatimlar.size > 1) {
                ctx.yeniSayfa()
                ctx.metin(context.getString(R.string.kp_icindekiler), BOY_BOLUM + 2, true)
                ctx.bosluk(10f)
                anlatimlar.forEachIndexed { i, a ->
                    ctx.metin("${i + 1}.  ${a.baslik}", BOY_METIN)
                    ctx.bosluk(3f)
                }
            }

            // ── Bölümler ──
            anlatimlar.forEachIndexed { i, anlatim ->
                ctx.yeniSayfa()

                if (anlatimlar.size > 1) {
                    ctx.metin("${i + 1}", BOY_DIPNOT)
                    ctx.bosluk(2f)
                }
                ctx.metin(anlatim.baslik, BOY_BASLIK, kalinMi = true)
                ctx.bosluk(14f)

                anlatim.bolumler.forEach { b ->
                    if (b.baslik.isNotBlank() && b.baslik != "—") {
                        ctx.bosluk(6f)
                        ctx.metin(b.baslik, BOY_BOLUM, kalinMi = true)
                        ctx.bosluk(6f)
                    }
                    // Paragrafları ayrı ayrı yaz — arada boşluk kalsın
                    b.metin.split("\n\n").forEach { p ->
                        val temiz = p.trim()
                        if (temiz.isNotBlank()) {
                            ctx.metin(temiz, BOY_METIN)
                            ctx.bosluk(7f)
                        }
                    }
                }

                // Özet
                if (anlatim.ozet.isNotBlank()) {
                    ctx.bosluk(8f)
                    ctx.metin(context.getString(R.string.kp_ozet), BOY_BOLUM, kalinMi = true)
                    ctx.bosluk(6f)
                    anlatim.ozet.lines().forEach { satir ->
                        val t = satir.trim()
                        if (t.isNotBlank()) {
                            ctx.metin(t, BOY_METIN)
                            ctx.bosluk(3f)
                        }
                    }
                }

                // Görsel önerileri
                if (anlatim.gorseller.isNotEmpty()) {
                    ctx.bosluk(10f)
                    ctx.metin(
                        context.getString(R.string.kp_gorseller), BOY_BOLUM, kalinMi = true
                    )
                    ctx.bosluk(6f)
                    anlatim.gorseller.forEach { g ->
                        ctx.metin("• " + g.aciklama, BOY_METIN)
                        ctx.bosluk(2f)
                        ctx.metin(
                            "   " + context.getString(R.string.kp_arama, g.aramaSorgusu),
                            BOY_DIPNOT
                        )
                        ctx.bosluk(5f)
                    }
                }

                // Kaynak alıntıları
                if (anlatim.kaynakAlintilari.isNotEmpty()) {
                    ctx.bosluk(10f)
                    ctx.metin(
                        context.getString(R.string.kp_kaynaklar), BOY_BOLUM, kalinMi = true
                    )
                    ctx.bosluk(6f)
                    anlatim.kaynakAlintilari.forEach { k ->
                        ctx.metin(
                            "• ${k.dersAdi} — ${context.getString(R.string.kp_sayfa, k.sayfa + 1)}",
                            BOY_DIPNOT
                        )
                        ctx.bosluk(2f)
                        ctx.metin("   " + k.parca.take(400).replace("\n", " "), BOY_DIPNOT)
                        ctx.bosluk(6f)
                    }
                }
            }

            ctx.kapat()

            val dosya = hedefDosya(context, baslik)
            belge.save(dosya)
            dosya
        } catch (e: OutOfMemoryError) {
            android.util.Log.w(TAG, "PDF için bellek yetmedi", e)
            null
        } catch (e: Exception) {
            android.util.Log.w(TAG, "PDF üretilemedi", e)
            null
        } finally {
            runCatching { belge?.close() }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YAZICI — sayfa taşmasını ve satır sarmayı yönetir
    // ═══════════════════════════════════════════════════════════════

    private class Yazici(
        val belge: PDDocument,
        val normal: PDFont,
        val kalin: PDFont,
        val unicodeVar: Boolean
    ) {
        var akis: PDPageContentStream? = null
        var y = 0f
        var sayfaYuksekligi = PDRectangle.A4.height
        private val genislik = PDRectangle.A4.width - 2 * KENAR

        fun yeniSayfa() {
            kapat()
            val sayfa = PDPage(PDRectangle.A4)
            belge.addPage(sayfa)
            akis = PDPageContentStream(belge, sayfa)
            sayfaYuksekligi = sayfa.mediaBox.height
            y = sayfaYuksekligi - KENAR
        }

        fun kapat() {
            runCatching { akis?.close() }
            akis = null
        }

        fun bosluk(h: Float) {
            y -= h
        }

        /** Metni sayfaya yazar; satır sarma ve sayfa taşması otomatik. */
        fun metin(ham: String, boyut: Float, kalinMi: Boolean = false) {
            val font = if (kalinMi) kalin else normal
            val temiz = hazirla(ham)
            if (temiz.isBlank()) return

            val satirYuksekligi = boyut * SATIR_ARA
            sarmala(temiz, font, boyut).forEach { satir ->
                if (y - satirYuksekligi < KENAR) yeniSayfa()
                try {
                    akis?.apply {
                        beginText()
                        setFont(font, boyut)
                        newLineAtOffset(KENAR, y)
                        showText(satir)
                        endText()
                    }
                } catch (e: Exception) {
                    // Tek bir satır yazılamazsa tüm PDF'i çöpe atma
                    android.util.Log.w(TAG, "Satır yazılamadı", e)
                }
                y -= satirYuksekligi
            }
        }

        /** Font Unicode desteklemiyorsa Türkçe karakterleri sadeleştirir. */
        private fun hazirla(ham: String): String {
            val tekSatir = ham.replace("\r", "").replace("\n", " ").trim()
            if (unicodeVar) return tekSatir
            return tekSatir
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ç', 'c').replace('Ç', 'C')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ü', 'u').replace('Ü', 'U')
                .replace(Regex("[^\\x20-\\x7E]"), "")
        }

        /** Satırı sayfa genişliğine göre böler. */
        private fun sarmala(metin: String, font: PDFont, boyut: Float): List<String> {
            val satirlar = mutableListOf<String>()
            val kelimeler = metin.split(" ").filter { it.isNotBlank() }
            var mevcut = StringBuilder()

            fun genislikOlc(s: String): Float = try {
                font.getStringWidth(s) / 1000f * boyut
            } catch (e: Exception) {
                // Font karakteri tanımıyorsa kaba tahmin
                s.length * boyut * 0.5f
            }

            for (kelime in kelimeler) {
                val deneme = if (mevcut.isEmpty()) kelime else "$mevcut $kelime"
                if (genislikOlc(deneme) <= genislik) {
                    mevcut = StringBuilder(deneme)
                } else {
                    if (mevcut.isNotEmpty()) satirlar.add(mevcut.toString())
                    // Tek kelime sayfaya sığmıyorsa (uzun URL vb.) parçala
                    if (genislikOlc(kelime) > genislik) {
                        var parca = StringBuilder()
                        kelime.forEach { c ->
                            if (genislikOlc("$parca$c") > genislik) {
                                satirlar.add(parca.toString())
                                parca = StringBuilder("$c")
                            } else parca.append(c)
                        }
                        mevcut = parca
                    } else {
                        mevcut = StringBuilder(kelime)
                    }
                }
            }
            if (mevcut.isNotEmpty()) satirlar.add(mevcut.toString())
            return satirlar
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    /** assets'teki TTF'i belgeye gömer. Başarısızsa null → Helvetica'ya düşülür. */
    private fun fontYukle(context: Context, belge: PDDocument, yol: String): PDFont? = try {
        context.assets.open(yol).use { PDType0Font.load(belge, it, true) }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Font yüklenemedi: $yol", e)
        null
    }

    private fun tarihMetni(): String =
        SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(Date())

    private fun hedefDosya(context: Context, baslik: String): File {
        val klasor = File(context.filesDir, "konu_pdf").apply { mkdirs() }
        val ad = baslik.replace(Regex("[^\\p{L}\\p{N} ]"), "").trim()
            .replace(" ", "_").take(40).ifBlank { "konu" }
        return File(klasor, "${ad}_${System.currentTimeMillis()}.pdf")
    }

    /** Paylaşım için FileProvider adresi. */
    fun uriVer(context: Context, dosya: File): Uri =
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", dosya
        )

    /** Üretilmiş PDF'lerin klasörü. */
    fun klasor(context: Context): File =
        File(context.filesDir, "konu_pdf").apply { mkdirs() }

    fun diskKullanimi(context: Context): Long =
        klasor(context).listFiles()?.sumOf { it.length() } ?: 0L

    fun temizle(context: Context): Int {
        val dosyalar = klasor(context).listFiles() ?: return 0
        var n = 0
        dosyalar.forEach { if (it.delete()) n++ }
        return n
    }
}
