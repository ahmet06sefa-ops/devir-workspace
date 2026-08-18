package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.7 · A6 — Zincir sayaç saf mantığının birim testleri.
 *
 * JSON kodlama/çözme, adım ilerlemesi, doğrulama ve mola
 * tahmini Context'siz kanıtlanır; UI yalnızca bu kapıları çağırır.
 */
class SayacZincirTest {

    private fun evre(ad: String = "Çalışma", sn: Int = 60, odak: Boolean = true) =
        SayacZincir.Evre(ad, "📖", sn, odak)

    private fun zincir(
        id: Long = 1L,
        evreler: List<SayacZincir.Evre> = listOf(evre(), evre("Mola", 30, false)),
        tekrar: Int = 2
    ) = SayacZincir.Zincir(id, "Deneme", "⛓", evreler, tekrar)

    // ── model hesapları ──────────────────────────────────────────

    @Test
    fun toplamAdim_evreCarpTekrar() {
        assertEquals(4, zincir(tekrar = 2).toplamAdim)
        assertEquals(16, zincir(tekrar = 8).toplamAdim)
    }

    @Test
    fun toplamSn_tumEvrelerVeTekrar() {
        // (60+30) × 2 = 180
        assertEquals(180, zincir(tekrar = 2).toplamSn)
    }

    @Test
    fun adimdaki_turSiniriniAsar() {
        val z = zincir(tekrar = 3)
        // Adım 4: 3. turun 1. evresi (2 evreli zincir)
        assertEquals("Çalışma", SayacZincir.adimdaki(z, 4).ad)
        assertEquals("Mola", SayacZincir.adimdaki(z, 5).ad)
    }

    @Test
    fun adimdaki_tasmayiKisitlar() {
        val z = zincir()
        // toplamAdim=4; adım 99 bile olsa güvenli evre dönmeli
        assertNotNull(SayacZincir.adimdaki(z, 99).ad)
    }

    @Test
    fun sonrakiAdim_sonraNull() {
        val z = zincir(tekrar = 2) // toplam 4 adım: 0..3
        assertEquals(1, SayacZincir.sonrakiAdim(z, 0))
        assertEquals(3, SayacZincir.sonrakiAdim(z, 2))
        assertNull(SayacZincir.sonrakiAdim(z, 3))
    }

    @Test
    fun kacinciAdim_gosterimCifti() {
        val z = zincir(tekrar = 2)
        assertEquals(1 to 4, SayacZincir.kacinciAdim(z, 0))
        assertEquals(4 to 4, SayacZincir.kacinciAdim(z, 3))
        // Taşan adım diziye sıkışır
        assertEquals(4 to 4, SayacZincir.kacinciAdim(z, 99))
    }

    // ── şablon tutarlılığı ───────────────────────────────────────

    @Test
    fun sablonlar_kimlikleriBenzersizVeNegatif() {
        val ids = SayacZincir.sablonlar().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it < 0 })
    }

    @Test
    fun sablon_tabataDortDakika() {
        val tabata = SayacZincir.sablonlar().first { it.ad == "Tabata" }
        // 8 × (20 + 10) = 240 sn — klasik protokol
        assertEquals(240, tabata.toplamSn)
        assertEquals(16, tabata.toplamAdim)
    }

    @Test
    fun sablonlar_dogrulamadanGecer() {
        for (s in SayacZincir.sablonlar()) {
            assertEquals(
                "şablon doğrulanamadı: ${s.ad}",
                SayacZincir.Hata.YOK,
                SayacZincir.dogrula(s.evreler, s.tekrar)
            )
        }
    }

    @Test
    fun sablonlar_molaEvreleriOdaksiz() {
        for (s in SayacZincir.sablonlar()) {
            for (e in s.evreler) {
                if (SayacZincir.molaBenzeriMi(e.ad)) {
                    assertFalse("${s.ad} / ${e.ad} odak olmamalı", e.odakMi)
                }
            }
        }
    }

    // ── mola tahmini ─────────────────────────────────────────────

    @Test
    fun molaBenzeriMi_yakalar() {
        assertTrue(SayacZincir.molaBenzeriMi("Mola"))
        assertTrue(SayacZincir.molaBenzeriMi("kısa mola"))
        assertTrue(SayacZincir.molaBenzeriMi("Dinlenme"))
        assertTrue(SayacZincir.molaBenzeriMi("NEFES"))
        // Türkçe ı grafiği — BILDIRIM aramasındaki katlama dersi
        assertTrue(SayacZincir.molaBenzeriMi("ISINMA"))
        assertTrue(SayacZincir.molaBenzeriMi("Soğuma"))
        // "yürüyüş" kökü — iki yazımda da yakalanmalı
        assertTrue(SayacZincir.molaBenzeriMi("Yürüyüş"))
    }

    @Test
    fun molaBenzeriMi_calismayiYutmaz() {
        assertFalse(SayacZincir.molaBenzeriMi("Çalışma"))
        assertFalse(SayacZincir.molaBenzeriMi("Sprint"))
        assertFalse(SayacZincir.molaBenzeriMi("Kod yazma"))
        assertFalse(SayacZincir.molaBenzeriMi(""))
    }

    // ── emoji önerisi ────────────────────────────────────────────

    @Test
    fun emojiOner_taninanAdlar() {
        assertEquals("☕", SayacZincir.emojiOner("Mola"))
        assertEquals("⚡", SayacZincir.emojiOner("Sprint"))
        assertEquals("📖", SayacZincir.emojiOner("Okuma"))
        assertEquals("💻", SayacZincir.emojiOner("Kodlama"))
        assertEquals("⏱️", SayacZincir.emojiOner(""))
        assertEquals("⏱️", SayacZincir.emojiOner("bambaşka bir şey"))
    }

    // ── doğrulama ────────────────────────────────────────────────

    @Test
    fun dogrula_temizDizi() {
        assertEquals(
            SayacZincir.Hata.YOK,
            SayacZincir.dogrula(listOf(evre()), 3)
        )
    }

    @Test
    fun dogrula_bosDizi_reddedilir() {
        assertEquals(
            SayacZincir.Hata.EVRE_YOK,
            SayacZincir.dogrula(emptyList(), 1)
        )
    }

    @Test
    fun dogrula_evreSiniri() {
        val fazla = List(SayacZincir.MAKS_EVRE + 1) { evre("E$it") }
        assertEquals(
            SayacZincir.Hata.FAZLA_EVRE,
            SayacZincir.dogrula(fazla, 1)
        )
    }

    @Test
    fun dogrula_sureVeTekrarSiniri() {
        assertEquals(
            SayacZincir.Hata.SURE_GECERSIZ,
            SayacZincir.dogrula(listOf(evre(sn = 0)), 1)
        )
        assertEquals(
            SayacZincir.Hata.SURE_GECERSIZ,
            SayacZincir.dogrula(listOf(evre(sn = 99999)), 1)
        )
        assertEquals(
            SayacZincir.Hata.TEKRAR_GECERSIZ,
            SayacZincir.dogrula(listOf(evre()), 0)
        )
        assertEquals(
            SayacZincir.Hata.TEKRAR_GECERSIZ,
            SayacZincir.dogrula(listOf(evre()), 99)
        )
    }

    // ── JSON ─────────────────────────────────────────────────────

    @Test
    fun json_tamTur() {
        val z = SayacZincir.Zincir(
            id = 7, ad = "Sınav temposu", emoji = "📚",
            evreler = listOf(
                SayacZincir.Evre("Okuma", "📖", 600, true),
                SayacZincir.Evre("Not alma", "✏️", 300, true),
                SayacZincir.Evre("Mola", "☕", 120, false)
            ),
            tekrar = 3
        )
        val geri = SayacZincir.cozle(SayacZincir.kodla(listOf(z))).single()
        assertEquals(z.id, geri.id)
        assertEquals(z.ad, geri.ad)
        assertEquals(z.emoji, geri.emoji)
        assertEquals(z.tekrar, geri.tekrar)
        assertEquals(z.evreler, geri.evreler)
        assertEquals(z.toplamAdim, geri.toplamAdim)
    }

    @Test
    fun json_bozukMetin_bosVerir() {
        assertTrue(SayacZincir.cozle("bu json değil").isEmpty())
        assertTrue(SayacZincir.cozle("").isEmpty())
        assertTrue(SayacZincir.cozle("{}").isEmpty())
    }

    @Test
    fun json_bozukSatirAtlanir() {
        // Bir iyi bir bozuk satır: iyi olan kurtulur
        val iyi = zincir(id = 5)
        val karisik = """[${iyi.json()}, {"ad":"", "sn":-3}]"""
        val liste = SayacZincir.cozle(karisik)
        assertEquals(1, liste.size)
        assertEquals(5L, liste[0].id)
    }

    @Test
    fun json_uzunAdlarKisaltilir() {
        val o = org.json.JSONObject()
            .put("id", 1)
            .put("ad", "a".repeat(50))
            .put("emoji", "😀😀😀😀😀😀")
            .put("tekrar", 1)
            .put("evreler", org.json.JSONArray().put(
                org.json.JSONObject().put("ad", "b".repeat(30)).put("sn", 60)
            ))
        val z = SayacZincir.Zincir.jsondan(o)!!
        assertTrue(z.ad.length <= 24)
        assertTrue(z.evreler[0].ad.length <= 18)
        assertTrue(z.emoji.length <= 4)
    }

    // ── süre metni ───────────────────────────────────────────────

    @Test
    fun sureMetni_bicimleri() {
        assertEquals("0:20", SayacZincir.sureMetni(20))
        assertEquals("4:00", SayacZincir.sureMetni(240))
        assertEquals("1:24:00", SayacZincir.sureMetni(5040))
        assertEquals("0:00", SayacZincir.sureMetni(-5))
    }
}
