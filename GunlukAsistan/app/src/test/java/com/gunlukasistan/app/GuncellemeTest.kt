package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

/**
 * v9.8 — Güncelleme kontrolü testleri (öneri 48).
 *
 * ── Neden test ──
 * Sunucudaki JSON'u ben elle yazıyorum. Yazım hatası yaparsam
 * (virgül unutma, alan adı yanlış) uygulama **çökmemeli** —
 * bu özellik "olsa iyi olur" kategorisinde, kritik değil.
 *
 * Ayrıca ters yönde bir risk var: bozuk veriyi "geçerli sürüm"
 * sanıp kullanıcıya sahte güncelleme göstermek. İkisini de
 * test ediyorum.
 */
class GuncellemeTest {

    // ══════════════════════════════════════════════════════════
    // Ayrıştırma — geçerli girdi
    // ══════════════════════════════════════════════════════════

    @Test
    fun `tam json dogru ayristirilir`() {
        val ham = """
            {"code":154,"name":"9.8","url":"https://gofile.io/d/ABC",
             "notes":"Grup G","min":150,"critical":true}
        """.trimIndent()
        val s = Guncelleme.ayristir(ham)
        assertNotNull(s)
        assertEquals(154, s!!.kod)
        assertEquals("9.8", s.ad)
        assertEquals("https://gofile.io/d/ABC", s.url)
        assertEquals("Grup G", s.notlar)
        assertEquals(150, s.enAz)
        assertTrue(s.kritik)
    }

    @Test
    fun `eksik alanlar varsayilana duser`() {
        val s = Guncelleme.ayristir("""{"code":100,"name":"1.0"}""")
        assertNotNull(s)
        assertEquals("", s!!.url)
        assertEquals("", s.notlar)
        assertEquals(0, s.enAz)
        assertFalse(s.kritik)
    }

    @Test
    fun `bosluklu json kabul edilir`() {
        val s = Guncelleme.ayristir("   \n  {\"code\":5,\"name\":\"a\"}  \n ")
        assertNotNull(s)
        assertEquals(5, s!!.kod)
    }

    // ══════════════════════════════════════════════════════════
    // Ayrıştırma — bozuk girdi (çökmemeli)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `bos metin null doner`() {
        assertNull(Guncelleme.ayristir(""))
        assertNull(Guncelleme.ayristir("   "))
    }

    @Test
    fun `json olmayan metin null doner`() {
        // textdb boş anahtarda HTML dönebiliyor
        assertNull(Guncelleme.ayristir("<html><body>404</body></html>"))
        assertNull(Guncelleme.ayristir("merhaba"))
    }

    @Test
    fun `bozuk json null doner`() {
        assertNull(Guncelleme.ayristir("""{"code":154,"name":}"""))
        assertNull(Guncelleme.ayristir("""{"code":"""))
    }

    @Test
    fun `kod sifirsa gecersiz`() {
        // Sürüm kodu 0 anlamsız — sahte güncelleme göstermeyelim
        assertNull(Guncelleme.ayristir("""{"code":0,"name":"9.9"}"""))
    }

    @Test
    fun `ad bossa gecersiz`() {
        assertNull(Guncelleme.ayristir("""{"code":200,"name":""}"""))
    }

    @Test
    fun `kod metin olarak gelirse gecersiz`() {
        // optInt metin "abc" için 0 döner → geçersiz sayılmalı
        assertNull(Guncelleme.ayristir("""{"code":"abc","name":"9.9"}"""))
    }

    @Test
    fun `dizi kok gecersiz`() {
        assertNull(Guncelleme.ayristir("""[{"code":154}]"""))
    }

    // ══════════════════════════════════════════════════════════
    // Yayın JSON üretimi
    // ══════════════════════════════════════════════════════════

    @Test
    fun `yayin jsonu geri okunabilir`() {
        // Ürettiğimiz JSON'u kendi ayrıştırıcımız okuyabilmeli.
        // Bu döngü testi, alan adı değişikliklerini yakalar.
        val ham = Guncelleme.yayinJsonu(
            160, "10.0", "https://x.co/a", "Notlar", 155, true
        )
        val s = Guncelleme.ayristir(ham)
        assertNotNull(s)
        assertEquals(160, s!!.kod)
        assertEquals("10.0", s.ad)
        assertEquals(155, s.enAz)
        assertTrue(s.kritik)
    }

    @Test
    fun `yayin jsonu gecerli json`() {
        val ham = Guncelleme.yayinJsonu(1, "a", "b", "c")
        val o = JSONObject(ham)
        assertEquals(1, o.getInt("code"))
        assertEquals("a", o.getString("name"))
    }

    // ══════════════════════════════════════════════════════════
    // Surum veri sınıfı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `gecerli bayragi dogru`() {
        assertTrue(Guncelleme.Surum(100, "1.0", "", "").gecerli)
        assertFalse(Guncelleme.Surum(0, "1.0", "", "").gecerli)
        assertFalse(Guncelleme.Surum(100, "", "", "").gecerli)
        assertFalse(Guncelleme.Surum(-5, "1.0", "", "").gecerli)
    }
}
