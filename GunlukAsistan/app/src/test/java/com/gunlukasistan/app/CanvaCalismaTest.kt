package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v11.11 — Canva Çalışma Ekranı ve 10 Uygulama Arayüzü Motoru (`CanvaCalismaMotoru`)
 * saf JVM birim testleri (20 test).
 */
class CanvaCalismaTest {

    @Before
    fun setup() {
        CanvaCalismaMotoru.testIcinSifirla(null)
    }

    @Test
    fun `varsayilan moduller tam 1 adet farkli uygulama barindirir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertEquals(10, list.size)
    }

    @Test
    fun `tum modulleri getir null context icin varsayilan listeyi dondurur`() {
        val list = CanvaCalismaCalismaMotoruGetirNullTest().list
        assertEquals(10, list.size)
    }

    private class CanvaCalismaCalismaMotoruGetirNullTest {
        val list = CanvaCalismaMotoru.tumModulleriGetir(null)
    }

    @Test
    fun `akilli oneri uygula saat dilimine gore 4 adet modulu acik yapar`() {
        val (mesaj, list) = CanvaCalismaMotoru.akilliOneriUygula(null)
        val acikSayisi = list.count { it.acik }
        assertEquals(4, acikSayisi)
        assertTrue(mesaj.contains("Akıllı Öneri Uygulandı"))
    }

    @Test
    fun `tekrar dene karistir fonksiyonu tam 4 adet yeni modulu acik hale getirir`() {
        val (mesaj, list) = CanvaCalismaMotoru.tekrarDeneKaristir(null)
        val acikSayisi = list.count { it.acik }
        assertEquals(4, acikSayisi)
        assertTrue(mesaj.contains("Alternatif Çalışma Ekranı"))
    }

    @Test
    fun `tumunu ac fonksiyonu 10 uygulamanin tamamini acik true yapar`() {
        val list = CanvaCalismaMotoru.tumunuAc(null)
        assertTrue(list.all { it.acik })
    }

    @Test
    fun `tumunu kapat fonksiyonu 10 uygulamanin tamamini kapali false yapar`() {
        val list = CanvaCalismaMotoru.tumunuKapat(null)
        assertTrue(list.none { it.acik })
    }

    @Test
    fun `modul durumu degistir belirli bir uygulamanin acik bayragini gunceller`() {
        val list = CanvaCalismaMotoru.modulDurumuDegistir(null, "CANVA_KURSLAR", true)
        val m = list.find { it.kod == "CANVA_KURSLAR" }
        assertTrue(m != null && m.acik)
    }

    @Test
    fun `modullerin simge ad ve aciklama alanlari bos degildir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.all { it.ad.isNotBlank() && it.simge.isNotBlank() && it.aciklama.isNotBlank() })
    }

    @Test
    fun `canva pomodoro modulu ve namaz modulu listede bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.any { it.kod == "CANVA_POMODORO" })
        assertTrue(list.any { it.kod == "CANVA_NAMAZ" })
    }

    @Test
    fun `canva gorevler modulu ve bugun modulu listede bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.any { it.kod == "CANVA_GOREVLER" })
        assertTrue(list.any { it.kod == "CANVA_BUGUN" })
    }

    @Test
    fun `canva kurslar ve istatistik modulleri listede bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.any { it.kod == "CANVA_KURSLAR" })
        assertTrue(list.any { it.kod == "CANVA_ISTATISTIK" })
    }

    @Test
    fun `canva kisisel gelisim ve youtube modulleri listede bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.any { it.kod == "CANVA_KISISEL" })
        assertTrue(list.any { it.kod == "CANVA_YOUTUBE" })
    }

    @Test
    fun `canva gorunum ve inovasyon modulleri listede bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.any { it.kod == "CANVA_GORUNUM" })
        assertTrue(list.any { it.kod == "CANVA_INOVASYON" })
    }

    @Test
    fun `her modulun kendine ozgu renk hexi bulunmaktadir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        assertTrue(list.all { it.renkHex.startsWith("#") && it.renkHex.length == 7 })
    }

    @Test
    fun `varsayilan olarak ilk 4 modul acik digerleri kapalidir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        val aciklar = list.filter { it.acik }
        assertEquals(4, aciklar.size)
    }

    @Test
    fun `bilinmeyen bir modul koduna durum degistirildiginde liste bozulmaz`() {
        val list = CanvaCalismaMotoru.modulDurumuDegistir(null, "YOK_MODUL", true)
        assertEquals(10, list.size)
    }

    @Test
    fun `tekrar dene karistir en az bir modulu acik hale getirir`() {
        val (_, list) = CanvaCalismaMotoru.tekrarDeneKaristir(null)
        assertTrue(list.any { it.acik })
    }

    @Test
    fun `akilli oneri ve tekrar dene mesajlari kullaniciyi bilgilendirir`() {
        val (m1, _) = CanvaCalismaMotoru.akilliOneriUygula(null)
        val (m2, _) = CanvaCalismaMotoru.tekrarDeneKaristir(null)
        assertTrue(m1.isNotBlank() && m2.isNotBlank())
    }

    @Test
    fun `modul kodlari benzersizdir`() {
        val list = CanvaCalismaMotoru.varsayilanModulleriGetir()
        val kodlar = list.map { it.kod }.toSet()
        assertEquals(list.size, kodlar.size)
    }

    @Test
    fun `canva calisma motoru sifirlama null context ile true basarili doner`() {
        CanvaCalismaMotoru.testIcinSifirla(null)
        assertEquals(10, CanvaCalismaMotoru.tumModulleriGetir(null).size)
    }
}
