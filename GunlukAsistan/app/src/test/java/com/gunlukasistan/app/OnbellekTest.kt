package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v8.9 — Önbellek testleri (öneri 15 güvencesi).
 *
 * ── Neden bu testler kritik ──
 * Önbellek, Faz 2'nin en riskli değişikliği. Yanlış çalışırsa
 * belirtileri sinsi olur:
 *   · Kullanıcı görev ekler, listede görünmez ("kaydetmiyor")
 *   · Yedek yüklenir, eski veri görünür ("yedek çalışmıyor")
 *   · Bir ekrandaki düzenleme diğerinde beliriverir (hayalet değişiklik)
 *
 * Bunların hiçbiri çökmeye yol açmaz — sessizce yanlış davranır.
 * Cihazda test edemediğim için tek güvence bu testler.
 */
class OnbellekTest {

    @Before
    fun temizle() {
        Onbellek.hepsiniBoz()
        Onbellek.istatistikSifirla()
    }

    // ══════════════════════════════════════════════════════════
    // Temel davranış
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ilk cagri uretir ikincisi onbellekten gelir`() {
        var uretimSayisi = 0
        val uret = { uretimSayisi++; mutableListOf("a", "b") }

        Onbellek.al("t", { it.toMutableList() }, uret)
        Onbellek.al("t", { it.toMutableList() }, uret)
        Onbellek.al("t", { it.toMutableList() }, uret)

        assertEquals("Üretim yalnız bir kez olmalı", 1, uretimSayisi)
    }

    @Test
    fun `bozma sonrasi yeniden uretilir`() {
        var uretimSayisi = 0
        val uret = { uretimSayisi++; mutableListOf("a") }

        Onbellek.al("t", { it.toMutableList() }, uret)
        Onbellek.boz("t")
        Onbellek.al("t", { it.toMutableList() }, uret)

        assertEquals(2, uretimSayisi)
    }

    @Test
    fun `hepsiniBoz tum anahtarlari temizler`() {
        Onbellek.al("a", { it }, { "1" })
        Onbellek.al("b", { it }, { "2" })
        assertEquals(2, Onbellek.boyut())

        Onbellek.hepsiniBoz()
        assertEquals(0, Onbellek.boyut())
    }

    @Test
    fun `farkli anahtarlar birbirini etkilemez`() {
        Onbellek.al("a", { it }, { "birinci" })
        Onbellek.al("b", { it }, { "ikinci" })
        Onbellek.boz("a")

        var bUretildi = false
        val b = Onbellek.al("b", { it }, { bUretildi = true; "yeni" })
        assertEquals("b hâlâ önbellekte olmalı", "ikinci", b)
        assertTrue("b yeniden üretilmemeli", !bUretildi)
    }

    // ══════════════════════════════════════════════════════════
    // 🔴 EN KRİTİK: kopya izolasyonu
    // ══════════════════════════════════════════════════════════

    @Test
    fun `donen liste onbellektekinden farkli nesne`() {
        val a = Onbellek.al("t", { l: MutableList<String> -> l.toMutableList() }) {
            mutableListOf("x")
        }
        val b = Onbellek.al("t", { l: MutableList<String> -> l.toMutableList() }) {
            mutableListOf("x")
        }
        assertNotSame("Her çağrı ayrı nesne dönmeli", a, b)
    }

    @Test
    fun `donen listeyi degistirmek onbellegi bozmaz`() {
        val kopyala = { l: MutableList<String> -> l.toMutableList() }
        val uret = { mutableListOf("a", "b") }

        val ilk = Onbellek.al("t", kopyala, uret)
        ilk.add("HAYALET")
        ilk.clear()

        val ikinci = Onbellek.al("t", kopyala, uret)
        assertEquals(
            "Önbellekteki asıl veri değişmemeli",
            listOf("a", "b"), ikinci
        )
    }

    /**
     * Bu test, `Store.loadTopics` için yazılan DERİN kopyanın neden
     * gerektiğini gösteriyor. Sığ kopya kullanılsaydı iç listedeki
     * değişiklik önbelleğe sızardı.
     */
    @Test
    fun `sig kopya ic listeyi korumaz - derin kopya sart`() {
        data class IcIce(val ad: String, val alt: MutableList<String>)

        // YANLIŞ: sığ kopya
        val sigKopyala = { l: MutableList<IcIce> -> l.toMutableList() }
        val uret = { mutableListOf(IcIce("kok", mutableListOf("m1"))) }

        val ilk = Onbellek.al("sig", sigKopyala, uret)
        ilk[0].alt.add("SIZAN")

        val ikinci = Onbellek.al("sig", sigKopyala, uret)
        assertEquals(
            "Sığ kopyada iç liste SIZAR — bu yüzden Store'da derin kopya kullanıldı",
            2, ikinci[0].alt.size
        )

        // DOĞRU: derin kopya
        Onbellek.hepsiniBoz()
        val derinKopyala = { l: MutableList<IcIce> ->
            l.mapTo(mutableListOf()) { it.copy(alt = it.alt.toMutableList()) }
        }
        val ilk2 = Onbellek.al("derin", derinKopyala, uret)
        ilk2[0].alt.add("SIZMAZ")

        val ikinci2 = Onbellek.al("derin", derinKopyala, uret)
        assertEquals(
            "Derin kopyada iç liste korunur",
            1, ikinci2[0].alt.size
        )
    }

    // ══════════════════════════════════════════════════════════
    // İstatistik
    // ══════════════════════════════════════════════════════════

    @Test
    fun `isabet ve kacak sayaclari dogru`() {
        val kopyala = { s: String -> s }
        Onbellek.al("t", kopyala) { "v" }   // kaçak
        Onbellek.al("t", kopyala) { "v" }   // isabet
        Onbellek.al("t", kopyala) { "v" }   // isabet

        assertEquals(2, Onbellek.isabet)
        assertEquals(1, Onbellek.kacak)
        assertEquals(66, Onbellek.isabetOrani())
    }

    @Test
    fun `bos onbellekte isabet orani sifir`() {
        assertEquals(0, Onbellek.isabetOrani())
    }

    // ══════════════════════════════════════════════════════════
    // Anahtar sabitleri
    // ══════════════════════════════════════════════════════════

    @Test
    fun `anahtar sabitleri benzersiz`() {
        // Aynı anahtar iki türe verilseydi biri diğerinin verisini
        // okurdu — sessiz ve çok kötü bir hata.
        val anahtarlar = listOf(
            Onbellek.K_TOPICS, Onbellek.K_TASKS, Onbellek.K_NOTES,
            Onbellek.K_COURSES, Onbellek.K_LESSONS, Onbellek.K_SECTIONS,
            Onbellek.K_HABITS, Onbellek.K_EXAMS, Onbellek.K_EVENTS,
            Onbellek.K_KAYNAKLAR, Onbellek.K_LOG
        )
        assertEquals(
            "Önbellek anahtarları benzersiz olmalı",
            anahtarlar.size, anahtarlar.toSet().size
        )
        assertTrue("Boş anahtar olmamalı", anahtarlar.none { it.isBlank() })
    }
}
