package com.gunlukasistan.app

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * PDF'i bölümlere ayırır (v6.7).
 *
 * Bölüm tespiti iki aşamalıdır:
 *  1. **Yer imleri (outline)** — yayıncı tarafından gömülmüşse en güvenilir kaynak.
 *     Üst seviye başlıklar bölüm kabul edilir.
 *  2. **Metin analizi** — yer imi yoksa her sayfanın üst kısmı taranır,
 *     "BÖLÜM 3", "3. ÜNİTE", "CHAPTER 5" gibi kalıplar aranır.
 *
 * Bulunan her bölüm ayrı bir PDF dosyası olarak uygulamanın özel alanına yazılır.
 */
object PdfSplitter {

    /** Tespit edilen bir bölüm. */
    class Chapter(
        val title: String,
        /** 0 tabanlı ilk sayfa. */
        val startPage: Int,
        /** 0 tabanlı son sayfa (dahil). */
        var endPage: Int,
        /**
         * İlk sayfada bölümün başladığı dikey konum (üstten oran, 0f..1f).
         * 0f = sayfanın en üstü. Sayfa ortasından başlayan bölümler için kullanılır.
         */
        var startOffset: Float = 0f,
        /**
         * Son sayfada bölümün bittiği dikey konum (üstten oran, 0f..1f).
         * 1f = sayfanın en altı.
         */
        var endOffset: Float = 1f
    ) {
        val pageCount: Int get() = endPage - startPage + 1
        /** Bu bölüm sayfa içi kesim kullanıyor mu? */
        val hasPartialPage: Boolean get() = startOffset > 0.001f || endOffset < 0.999f
    }

    /** Bölme sonucu. */
    class Result(
        val ok: Boolean,
        val message: String,
        val chapters: List<Chapter> = emptyList(),
        val files: List<File> = emptyList()
    )

    /**
     * Bölüm başlığı kalıpları.
     * Türkçe ve İngilizce yaygın biçimleri kapsar, numarasız "ÖNSÖZ" gibi
     * başlıkları da yakalar.
     */
    private val chapterPatterns = listOf(
        // "3. BÖLÜM", "3 BÖLÜM — Sayılar" (satırın tamamı başlık olmalı)
        Regex("""^\s*(\d{1,2})\s*[.\-–)]?\s*(BÖLÜM|BOLUM|ÜNİTE|UNITE|KISIM|KONU)\s*[:\-–]?\s*(.{0,40})$""", RegexOption.IGNORE_CASE),
        // "BÖLÜM 3", "BÖLÜM III: Giriş"
        Regex("""^\s*(BÖLÜM|BOLUM|ÜNİTE|UNITE|KISIM|KONU)\s*[:\-–]?\s*(\d{1,2}|[IVXLC]{1,6})\s*[:\-–]?\s*(.{0,40})$""", RegexOption.IGNORE_CASE),
        // İngilizce
        Regex("""^\s*(CHAPTER|UNIT|PART|SECTION)\s*[:\-–]?\s*(\d{1,2}|[IVXLC]{1,6})\s*[:\-–]?\s*(.{0,40})$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*(\d{1,2})\s*[.\-–)]\s*(CHAPTER|UNIT|PART)\s*[:\-–]?\s*(.{0,40})$""", RegexOption.IGNORE_CASE),
        // Numarasız özel başlıklar (satırda başka bir şey olmamalı)
        Regex("""^\s*(ÖNSÖZ|ONSOZ|GİRİŞ|GIRIS|SONUÇ|SONUC|KAYNAKÇA|KAYNAKCA|EKLER|PREFACE|INTRODUCTION|CONCLUSION|APPENDIX|REFERENCES)\s*$""", RegexOption.IGNORE_CASE),
        // "1. TEMEL KAVRAMLAR" — numara + BÜYÜK HARFLİ başlık (en yaygın ders kitabı biçimi)
        Regex("""^\s*(\d{1,2})\s*[.\-–)]\s*([\p{Lu}ÇĞİÖŞÜ][\p{Lu}ÇĞİÖŞÜ\s.,'’-]{4,45})$"""),
        // "1.1 Alt başlık" biçimini DIŞLA (sadece tek düzey), "ÜNİTE-3" gibi tireli
        Regex("""^\s*(BÖLÜM|BOLUM|ÜNİTE|UNITE|KISIM|KONU)\s*[-–]\s*(\d{1,2})\s*[:\-–]?\s*(.{0,40})$""", RegexOption.IGNORE_CASE)
    )

    /**
     * Yanlış pozitifleri eleyen kalıp.
     * "Bölüm 3 - devam sayfası 2", "Bölüm 5 ....... 42" gibi satırlar
     * gerçek bölüm başlangıcı değildir; üstbilgi ya da içindekiler satırıdır.
     */
    private val rejectPattern = Regex(
        """(devam|continued|cont\.|sayfa\s*\d|page\s*\d|s\.\s*\d|\.{3,}|…|^\s*\d+\.\d+)""",
        RegexOption.IGNORE_CASE
    )

    /** PdfBox'ın font/kaynak önbelleğini hazırlar (uygulama başında bir kez). */
    fun init(context: Context) {
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
        } catch (_: Exception) {
        }
    }

    /**
     * PDF'i inceleyip bölümleri tespit eder ve ayrı dosyalara böler.
     * Ağ/disk işlemi olduğu için arka planda çağrılmalıdır.
     *
     * @param maxChapters güvenlik sınırı — çok parçalı belgelerde taşmayı önler
     */
    fun split(
        context: Context,
        uri: Uri,
        baseName: String,
        maxChapters: Int = 60,
        /** >0 ise otomatik tespit yerine belgeyi bu kadar eşit parçaya böler. */
        equalParts: Int = 0,
        /**
         * Elle seçilmiş bölme noktaları: "sayfa:oran,sayfa:oran…"
         * Oran, sayfanın üstünden itibaren 0f..1f arası konumdur.
         */
        manualPoints: String? = null
    ): Result {
        init(context)
        var doc: PDDocument? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result(false, context.getString(R.string.split_err_open))
            doc = input.use { PDDocument.load(it) }

            val total = doc.numberOfPages
            if (total < 2) {
                return Result(false, context.getString(R.string.split_err_short))
            }

            // 0a) Elle işaretlenmiş noktalar varsa (sayfa içi kesim dahil)
            if (!manualPoints.isNullOrBlank()) {
                val marks = parsePoints(manualPoints, total)
                if (marks.size < 1) {
                    return Result(false, context.getString(R.string.split_err_none))
                }
                val manual = buildFromMarks(context, marks, total)
                val files = writeParts(context, doc, manual, baseName, total)
                return Result(
                    true,
                    context.getString(R.string.split_ok_marks, manual.size),
                    manual, files
                )
            }

            // 0b) Elle eşit bölme istendiyse
            if (equalParts > 1) {
                val per = (total + equalParts - 1) / equalParts
                val manual = mutableListOf<Chapter>()
                var start = 0
                var no = 1
                while (start < total) {
                    val end = (start + per - 1).coerceAtMost(total - 1)
                    manual.add(Chapter(context.getString(R.string.split_part_n, no), start, end))
                    start = end + 1
                    no++
                }
                val files = writeParts(context, doc, manual, baseName, total)
                return Result(
                    true,
                    context.getString(R.string.split_ok_manual, manual.size),
                    manual, files
                )
            }

            // 1) Önce yer imlerini dene
            var chapters = fromOutline(doc, total)
            var source = "outline"

            // 2) Yer imi yoksa/yetersizse metni tara
            if (chapters.size < 2) {
                chapters = fromText(doc, total)
                source = "text"
            }

            if (chapters.size < 2) {
                return Result(false, context.getString(R.string.split_err_none))
            }
            if (chapters.size > maxChapters) {
                chapters = chapters.take(maxChapters)
                // Son bölümün sonunu belgeye kadar uzat
                chapters.lastOrNull()?.let { it.endPage = total - 1 }
            }

            val outFiles = writeParts(context, doc, chapters, baseName, total)

            return Result(
                true,
                context.getString(
                    if (source == "outline") R.string.split_ok_outline else R.string.split_ok_text,
                    chapters.size
                ),
                chapters,
                outFiles
            )
        } catch (e: OutOfMemoryError) {
            return Result(false, context.getString(R.string.split_err_memory))
        } catch (e: Exception) {
            return Result(false, context.getString(R.string.split_err_generic, e.message ?: "?"))
        } finally {
            try { doc?.close() } catch (_: Exception) {}
        }
    }

    /** "sayfa:oran,sayfa:oran" metnini çözer, sayfaya göre sıralar. */
    private fun parsePoints(raw: String, total: Int): List<Pair<Int, Float>> {
        val out = mutableListOf<Pair<Int, Float>>()
        raw.split(",").forEach { chunk ->
            val parts = chunk.trim().split(":")
            if (parts.size != 2) return@forEach
            val page = parts[0].toIntOrNull() ?: return@forEach
            val ratio = parts[1].toFloatOrNull() ?: return@forEach
            if (page in 0 until total) out.add(page to ratio.coerceIn(0f, 1f))
        }
        return out.sortedWith(compareBy({ it.first }, { it.second }))
    }

    /**
     * İşaretlenen noktalardan bölümleri kurar.
     * Her nokta bir bölümün **başlangıcı** sayılır; ilk noktadan önceki kısım
     * varsa "Giriş" olarak ayrı bir parça olur.
     */
    private fun buildFromMarks(
        context: Context,
        marks: List<Pair<Int, Float>>,
        total: Int
    ): List<Chapter> {
        val list = mutableListOf<Chapter>()

        // İlk noktadan önce içerik varsa giriş parçası
        val first = marks.firstOrNull() ?: return list
        if (first.first > 0 || first.second > 0.02f) {
            val ch = Chapter(
                context.getString(R.string.split_part_intro),
                0,
                first.first,
                0f,
                first.second
            )
            list.add(ch)
        }

        marks.forEachIndexed { i, (page, ratio) ->
            val next = marks.getOrNull(i + 1)
            val endPage = next?.first ?: (total - 1)
            val endOffset = next?.second ?: 1f
            list.add(
                Chapter(
                    context.getString(R.string.split_part_n, list.size + 1),
                    page,
                    endPage,
                    ratio,
                    endOffset
                )
            )
        }
        return list
    }

    /** Bölümleri ayrı PDF dosyalarına yazar. */
    private fun writeParts(
        context: Context,
        doc: PDDocument,
        chapters: List<Chapter>,
        baseName: String,
        total: Int
    ): List<File> {
        val dir = File(context.filesDir, "kitaplik").apply { mkdirs() }
        val safeBase = baseName.replace(Regex("""[^\p{L}\p{N} _-]"""), "").trim().take(40)
        val outFiles = mutableListOf<File>()
        chapters.forEachIndexed { index, ch ->
            val part = PDDocument()
            try {
                for (p in ch.startPage..ch.endPage) {
                    if (p !in 0 until total) continue
                    val srcPage = doc.getPage(p)
                    part.addPage(srcPage)
                }
                if (part.numberOfPages == 0) return@forEachIndexed

                // Sayfa içi kesim: ilk ve/veya son sayfayı kırp
                if (ch.hasPartialPage) {
                    applyCrop(part, ch)
                }
                val safeTitle = ch.title.replace(Regex("""[^\p{L}\p{N} _-]"""), "").trim()
                    .take(30).ifBlank { "bolum" }
                val out = File(
                    dir,
                    String.format(
                        java.util.Locale.US, "%s_%02d_%s.pdf",
                        safeBase.ifBlank { "kitap" }, index + 1, safeTitle
                    )
                )
                part.save(out)
                outFiles.add(out)
            } catch (_: Exception) {
                // Bozuk bölümü atla, diğerleri devam etsin
            } finally {
                try { part.close() } catch (_: Exception) {}
            }
        }
        return outFiles
    }

    /**
     * Bölümün ilk/son sayfasını dikey olarak kırpar.
     *
     * PDF koordinatları **alttan** başlar, kullanıcı oranı **üstten** verir;
     * dönüşüm burada yapılır. Hem MediaBox hem CropBox ayarlanmalıdır —
     * yalnızca CropBox yeterli olmaz, görüntüleyiciler tam sayfayı gösterir.
     */
    private fun applyCrop(part: PDDocument, ch: Chapter) {
        val count = part.numberOfPages
        if (count == 0) return

        // İlk sayfa: üstten startOffset kadarı atılır
        if (ch.startOffset > 0.001f) {
            val page = part.getPage(0)
            val m = page.mediaBox
            val h = m.height
            val topCut = h * ch.startOffset
            // Üstten kesince alt kısım kalır: y = lowerLeft .. (upper - topCut)
            val newTop = m.upperRightY - topCut
            // Tek sayfalık bölümde alt kesim de aynı sayfaya uygulanır
            val newBottom = if (count == 1 && ch.endOffset < 0.999f) {
                m.upperRightY - h * ch.endOffset
            } else {
                m.lowerLeftY
            }
            if (newTop - newBottom > 20f) {
                val r = PDRectangle(m.lowerLeftX, newBottom, m.width, newTop - newBottom)
                page.mediaBox = r
                page.cropBox = r
            }
        }

        // Son sayfa (ilk sayfadan farklıysa): alttan kırpılır
        if (ch.endOffset < 0.999f && count > 1) {
            val page = part.getPage(count - 1)
            val m = page.mediaBox
            val h = m.height
            val newBottom = m.upperRightY - h * ch.endOffset
            if (m.upperRightY - newBottom > 20f) {
                val r = PDRectangle(m.lowerLeftX, newBottom, m.width, m.upperRightY - newBottom)
                page.mediaBox = r
                page.cropBox = r
            }
        }
    }

    /** Yalnızca bölümleri tespit eder (bölmeden önce önizleme için). */
    fun detect(context: Context, uri: Uri): Result {
        init(context)
        var doc: PDDocument? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result(false, context.getString(R.string.split_err_open))
            doc = input.use { PDDocument.load(it) }
            val total = doc.numberOfPages
            var chapters = fromOutline(doc, total)
            var source = "outline"
            if (chapters.size < 2) {
                chapters = fromText(doc, total)
                source = "text"
            }
            return if (chapters.size < 2) {
                Result(false, context.getString(R.string.split_err_none))
            } else {
                Result(
                    true,
                    context.getString(
                        if (source == "outline") R.string.split_ok_outline else R.string.split_ok_text,
                        chapters.size
                    ),
                    chapters
                )
            }
        } catch (e: Exception) {
            return Result(false, context.getString(R.string.split_err_generic, e.message ?: "?"))
        } finally {
            try { doc?.close() } catch (_: Exception) {}
        }
    }

    // ---------------- Yöntem 1: Yer imleri ----------------

    private fun fromOutline(doc: PDDocument, total: Int): List<Chapter> {
        val result = mutableListOf<Chapter>()
        try {
            val outline = doc.documentCatalog?.documentOutline ?: return emptyList()
            var item: PDOutlineItem? = outline.firstChild ?: return emptyList()

            val pages = doc.pages.toList()
            while (item != null) {
                val title = item.title?.trim().orEmpty()
                val page = resolvePage(doc, item, pages)
                if (title.isNotEmpty() && page in 0 until total) {
                    result.add(Chapter(title, page, total - 1))
                }
                item = item.nextSibling
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return finalize(result, total)
    }

    /** Yer iminin işaret ettiği sayfa numarasını bulur. */
    private fun resolvePage(doc: PDDocument, item: PDOutlineItem, pages: List<PDPage>): Int {
        return try {
            val dest = item.destination
            if (dest is PDPageDestination) {
                val p = dest.page
                if (p != null) pages.indexOf(p) else dest.pageNumber
            } else {
                val action = item.action
                if (action is com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo) {
                    val d = action.destination
                    if (d is PDPageDestination) {
                        val p = d.page
                        if (p != null) pages.indexOf(p) else d.pageNumber
                    } else -1
                } else -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    // ---------------- Yöntem 2: Metin analizi ----------------

    private fun fromText(doc: PDDocument, total: Int): List<Chapter> {
        val result = mutableListOf<Chapter>()
        val stripper = PDFTextStripper()
        // Çok uzun belgelerde tarama süresini sınırla
        val limit = total.coerceAtMost(1200)

        for (page in 0 until limit) {
            try {
                stripper.startPage = page + 1
                stripper.endPage = page + 1
                val text = stripper.getText(doc)
                // Sayfanın ilk birkaç satırında başlık aranır
                val head = text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .toList()

                for (line in head) {
                    if (line.length > 60) continue                 // uzun satır başlık değildir
                    if (rejectPattern.containsMatchIn(line)) continue  // "devam sayfası" vb. ele
                    // matches değil match: satırın TAMAMI başlık kalıbına uymalı
                    val matched = chapterPatterns.any { it.matches(line) }
                    if (matched) {
                        // Aynı sayfada birden fazla eşleşmeyi engelle
                        if (result.isEmpty() || result.last().startPage != page) {
                            result.add(Chapter(cleanTitle(line), page, total - 1))
                        }
                        break
                    }
                }
            } catch (_: Exception) {
                // Bozuk sayfayı atla
            }
        }
        return finalize(result, total)
    }

    private fun cleanTitle(raw: String): String =
        raw.replace(Regex("""\s+"""), " ")
            .replace(Regex("""[.…·•]{2,}.*$"""), "")   // içindekiler noktalarını at
            .trim()
            .take(60)

    // ---------------- Ortak ----------------

    /**
     * Bölüm listesini düzenler: sayfaya göre sıralar, bitiş sayfalarını hesaplar,
     * tek sayfalık sahte başlıkları (içindekiler satırları) eler.
     */
    private fun finalize(list: List<Chapter>, total: Int): List<Chapter> {
        if (list.isEmpty()) return emptyList()
        val sorted = list.sortedBy { it.startPage }.toMutableList()

        // Aynı sayfadan başlayan tekrarları temizle
        val unique = mutableListOf<Chapter>()
        sorted.forEach { ch ->
            if (unique.isEmpty() || unique.last().startPage != ch.startPage) unique.add(ch)
        }

        // Bitiş sayfalarını ayarla
        for (i in unique.indices) {
            unique[i].endPage = if (i < unique.size - 1) {
                (unique[i + 1].startPage - 1).coerceAtLeast(unique[i].startPage)
            } else {
                total - 1
            }
        }

        // İçindekiler sayfasındaki yalancı başlıkları ele:
        // ilk 3 bölüm arka arkaya tek sayfalıksa muhtemelen içindekiler listesidir
        val head = unique.take(3)
        if (head.size == 3 && head.all { it.pageCount <= 1 }) {
            val rest = unique.drop(3)
            if (rest.size >= 2) {
                for (i in rest.indices) {
                    rest[i].endPage = if (i < rest.size - 1) {
                        (rest[i + 1].startPage - 1).coerceAtLeast(rest[i].startPage)
                    } else total - 1
                }
                return rest
            }
        }
        return unique
    }
}
