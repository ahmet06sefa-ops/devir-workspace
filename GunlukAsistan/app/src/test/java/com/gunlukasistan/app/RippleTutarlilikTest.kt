package com.gunlukasistan.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * v10.1 · Görsel Grup C / Öneri 14 — Ripple (dokunma dalgası) tutarlılık testi.
 *
 * ── Neden test ──
 * Tıklanabilir öğelerin bir bölümünde dokunma dalgası var, bir
 * bölümünde yoktu: kullanıcı "burası tıklanır mı" sorusuna görsel
 * cevap alamıyordu. v10.1'de eksikler giderildi:
 *   · `item_arac.xml`, `item_quiz_option.xml` + 5 kart → foreground ripple
 *   · Kotlin'de kurulan 9 tıklanabilir karta `dalgaEkle()`
 *
 * Bu test yeni ihlalleri engeller: layout'a tıklanabilir öğe ekleyen
 * herkes dalgasını da eklemeli.
 *
 * ── Kurallar ──
 * `android:clickable="true"` taşıyan her öğe için şunlardan biri şart:
 *   · `android:foreground` veya `android:background` içinde
 *     `selectableItemBackground`
 *   · `app:rippleColor` tanımlı
 *   · Öğe MaterialButton ailesinden (yerleşik ripple)
 *
 * Tasarım ölçeği testiyle aynı yaklaşım: layout klasörü yoksa sessizce
 * geçer (CI dışı ortamlarda).
 */
class RippleTutarlilikTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val appNs = "http://schemas.android.com/apk/res-auto"

    private val layoutKlasoru: File? by lazy {
        listOf(
            File("src/main/res/layout"),
            File("app/src/main/res/layout")
        ).firstOrNull { it.isDirectory }
    }

    @Test
    fun `tiklanabilir ogelerde dokunma dalgasi tanimli`() {
        val klasor = layoutKlasoru ?: return
        val dosyalar = klasor.listFiles { f -> f.extension == "xml" }?.toList().orEmpty()
        if (dosyalar.isEmpty()) return

        val fabrika = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val ihlaller = mutableListOf<String>()

        for (dosya in dosyalar) {
            val belge = runCatching {
                fabrika.newDocumentBuilder().parse(dosya)
            }.getOrNull() ?: continue

            val dugumler = belge.documentElements()
            for (dugum in dugumler) {
                val nitelikler = dugum.attributes ?: continue
                val tiklanabilir = nitelikler.getNamedItemNS(androidNs, "clickable")
                    ?.nodeValue == "true"
                if (!tiklanabilir) continue

                val foreground = nitelikler.getNamedItemNS(androidNs, "foreground")?.nodeValue.orEmpty()
                val background = nitelikler.getNamedItemNS(androidNs, "background")?.nodeValue.orEmpty()
                val ripple = nitelikler.getNamedItemNS(appNs, "rippleColor")?.nodeValue.orEmpty()
                val sinif = dugum.tagName.orEmpty()

                val dalgaVar = foreground.contains("selectableItemBackground") ||
                    background.contains("selectableItemBackground") ||
                    ripple.isNotBlank() ||
                    // MaterialButton ailesi ripple'ı yerleşik çizer
                    sinif.contains("Button")

                if (!dalgaVar) {
                    val kimlik = nitelikler.getNamedItemNS(androidNs, "id")?.nodeValue
                    ihlaller.add("${dosya.name}: <$sinif> ${kimlik ?: "(id yok)"}")
                }
            }
        }

        assertTrue(
            "Tıklanabilir ama dokunma dalgasız öğeler:\n" + ihlaller.joinToString("\n"),
            ihlaller.isEmpty()
        )
    }

    /** Belgedeki tüm öğeleri düz liste verir. */
    private fun org.w3c.dom.Document.documentElements(): List<org.w3c.dom.Element> {
        val sonuc = mutableListOf<org.w3c.dom.Element>()
        fun gez(dugum: org.w3c.dom.Node?) {
            if (dugum == null) return
            if (dugum.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                sonuc.add(dugum as org.w3c.dom.Element)
            }
            val cocuklar = dugum.childNodes
            for (i in 0 until cocuklar.length) gez(cocuklar.item(i))
        }
        gez(documentElement)
        return sonuc
    }
}
