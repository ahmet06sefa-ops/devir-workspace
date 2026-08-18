package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * v11.12 — Evrensel Veri Yedekleme & Geri Yükleme Motoru (`TumVeriYedeklemeMotoru`)
 * saf JVM birim testleri (20 test). Android Context gerektirmez.
 */
class TumVeriYedeklemeTest {

    private fun ornekPrefs(): Map<String, Map<String, Any?>> = mapOf(
        "gorunum_v1" to mapOf(
            "acilisEkran" to 0,
            "kartBoyutuOlcegi" to 1L,
            "acikMi" to true,
            "baslik" to "Ana Ekran",
        ),
        "canva_moduller_v1" to mapOf(
            "modul_json" to """{"CANVA_POMODORO":true,"CANVA_GOREVLER":false}""",
            "etiketler" to setOf("POMODORO", "GOREVLER"),
            "olcek" to 0.5f,
        ),
        "bos_v1" to emptyMap(),
    )

    @Test
    fun `yedek olustur anahtar ve surum icerir`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        assertTrue(json.contains("\"anahtar\":\"ga_yedek_v1\""))
        assertTrue(json.contains("\"uygulamaSurum\":\"11.12\""))
        assertTrue(json.contains("\"surum\":1"))
    }

    @Test
    fun `yedek dogrula gecerli yedek icin true doner`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        assertTrue(TumVeriYedeklemeMotoru.yedekDogrula(json))
    }

    @Test
    fun `yedek dogrula bozuk anahtar icin false doner`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val bozuk = json.replace("ga_yedek_v1", "ga_yedek_v9")
        assertFalse(TumVeriYedeklemeMotoru.yedekDogrula(bozuk))
    }

    @Test
    fun `yedek dogrula bos ya da anlamsiz metin icin false doner`() {
        assertFalse(TumVeriYedeklemeMotoru.yedekDogrula(""))
        assertFalse(TumVeriYedeklemeMotoru.yedekDogrula("sadece metin"))
        assertFalse(TumVeriYedeklemeMotoru.yedekDogrula("{bozuk json"))
    }

    @Test
    fun `geri yukle tum dosyalari ve degerleri birebir korur`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        assertEquals(3, sonuc.size)
        assertEquals(0, sonuc["gorunum_v1"]?.get("acilisEkran"))
        assertEquals(1L, sonuc["gorunum_v1"]?.get("kartBoyutuOlcegi"))
        assertEquals(true, sonuc["gorunum_v1"]?.get("acikMi"))
        assertEquals("Ana Ekran", sonuc["gorunum_v1"]?.get("baslik"))
    }

    @Test
    fun `geri yukle tip korur int long bool string`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        val g = sonuc["gorunum_v1"]!!
        assertTrue(g["acilisEkran"] is Int)
        assertTrue(g["kartBoyutuOlcegi"] is Long)
        assertTrue(g["acikMi"] is Boolean)
        assertTrue(g["baslik"] is String)
    }

    @Test
    fun `geri yukle set ve float tipini korur`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        val c = sonuc["canva_moduller_v1"]!!
        assertTrue(c["olcek"] is Float)
        assertEquals(setOf("POMODORO", "GOREVLER"), c["etiketler"])
    }

    @Test
    fun `bos prefs dosyasi korunur ve yedekten geri doner`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        assertNotNull(sonuc["bos_v1"])
        assertEquals(0, sonuc["bos_v1"]?.size)
    }

    @Test
    fun `saglama degeri kurcalanan yedekte degisir ve geri yukleme reddedilir`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val kurcalanmis = json.replace("\"acilisEkran\":{\"t\":\"I\",\"v\":0}", "\"acilisEkran\":{\"t\":\"I\",\"v\":5}")
        try {
            TumVeriYedeklemeMotoru.geriYukle(kurcalanmis)
            fail("Sağlama eşleşmeyen yedek reddedilmeliydi")
        } catch (_: IllegalArgumentException) {
            // beklenen
        }
    }

    @Test
    fun `yanlis anahtarli yedek geri yuklemede reddedilir`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val bozuk = json.replace("ga_yedek_v1", "ga_yedek_v9")
        try {
            TumVeriYedeklemeMotoru.geriYukle(bozuk)
            fail("Yanlış anahtar reddedilmeliydi")
        } catch (_: IllegalArgumentException) {
            // beklenen
        }
    }

    @Test
    fun `meta bilgi gecerli yedekten ozet verir`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val meta = TumVeriYedeklemeMotoru.metaBilgi(json)
        assertNotNull(meta)
        assertEquals("11.12", meta!!.uygulamaSurum)
        assertEquals(3, meta.dosyaSayisi)
        // gorunum 4 + canva 3 = 7
        assertEquals(7, meta.kalanSayisi)
        assertEquals(8, meta.saglama.length)
        assertTrue(meta.bayt > 0)
    }

    @Test
    fun `meta bilgi gecersiz yedekte null doner`() {
        assertNull(TumVeriYedeklemeMotoru.metaBilgi("rastgele"))
        assertNull(TumVeriYedeklemeMotoru.metaBilgi(""))
    }

    @Test
    fun `saglama deterministiktir ayni veri ayni degeri uretir`() {
        val a = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val b = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        assertEquals(
            TumVeriYedeklemeMotoru.saglamaHesapla(org.json.JSONObject(a).getJSONArray("dosyalar")),
            TumVeriYedeklemeMotoru.saglamaHesapla(org.json.JSONObject(b).getJSONArray("dosyalar"))
        )
    }

    @Test
    fun `saglama farkli veride farkli deger uretir`() {
        val a = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val farkli = ornekPrefs().toMutableMap().apply { this["gorunum_v1"] = mapOf("acilisEkran" to 9) }
        val b = TumVeriYedeklemeMotoru.yedekOlustur(farkli, "11.12")
        assertFalse(
            TumVeriYedeklemeMotoru.saglamaHesapla(org.json.JSONObject(a).getJSONArray("dosyalar"))
                == TumVeriYedeklemeMotoru.saglamaHesapla(org.json.JSONObject(b).getJSONArray("dosyalar"))
        )
    }

    @Test
    fun `geri yukle hicbir ekstra anahtar uretmez`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        assertEquals(setOf("gorunum_v1", "canva_moduller_v1", "bos_v1"), sonuc.keys)
    }

    @Test
    fun `dizi benzeri veri bile tip etiketiyle korunur`() {
        val veri = mapOf(
            "dizin" to mapOf(
                "liste" to setOf("a", "b", "c"),
            )
        )
        val json = TumVeriYedeklemeMotoru.yedekOlustur(veri, "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        assertEquals(setOf("a", "b", "c"), sonuc["dizin"]?.get("liste"))
    }

    @Test
    fun `buyuk deger kumesi kayipsiz tamamlanir`() {
        val buyuk = mutableMapOf<String, Any?>()
        for (i in 0 until 500) buyuk["anahtar_$i"] = "deger_$i".repeat(3)
        val veri = mapOf("kitaplik" to buyuk)
        val json = TumVeriYedeklemeMotoru.yedekOlustur(veri, "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        val k = sonuc["kitaplik"]!!
        assertEquals(500, k.size)
        assertEquals("deger_250".repeat(3), k["anahtar_250"])
    }

    @Test
    fun `null ve bilinmeyen tip stringe donusur ve kaybolmaz`() {
        val veri = mapOf(
            "coklu" to mapOf(
                "bosDeger" to null,
                "doubleDeger" to 3.14,
                "charDeger" to 'x',
            )
        )
        val json = TumVeriYedeklemeMotoru.yedekOlustur(veri, "11.12")
        val sonuc = TumVeriYedeklemeMotoru.geriYukle(json)
        val c = sonuc["coklu"]!!
        assertEquals("", c["bosDeger"])
        assertTrue(c["doubleDeger"] is String)
        assertEquals("x", c["charDeger"])
    }

    @Test
    fun `tarih iso bicimli ve parsellenebilir`() {
        val iso = TumVeriYedeklemeMotoru.tarihIso()
        assertTrue(iso.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")))
    }

    @Test
    fun `yedek icine yazilan tarih aynen korunur`() {
        val json = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val tarih = org.json.JSONObject(json).optString("tarihIso")
        assertNotNull(tarih)
        assertTrue(tarih.isNotBlank())
    }

    @Test
    fun `yuvarlak tur sicak cekirdeksiz kalir geri yukleme tekrar yedek ile esittir`() {
        val ilk = TumVeriYedeklemeMotoru.yedekOlustur(ornekPrefs(), "11.12")
        val geri = TumVeriYedeklemeMotoru.geriYukle(ilk)
        val ikinci = TumVeriYedeklemeMotoru.yedekOlustur(geri, "11.12")
        assertEquals(ilk, ikinci)
    }
}
