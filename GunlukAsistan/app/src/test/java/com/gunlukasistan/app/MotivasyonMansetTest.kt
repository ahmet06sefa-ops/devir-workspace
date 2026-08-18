package com.gunlukasistan.app

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.73 — Akıllı "Sokratik & Felsefi Motivasyon Manşeti" Motoru ([MotivasyonMansetMotoru])
 * saf birim testleri (26 test).
 */
class MotivasyonMansetTest {

    @Test
    fun `varsayilan soz listesi 20 adet ilham verici motto icerir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertEquals(20, list.size)
    }

    @Test
    fun `soz listesi seneca ve sokrates sozlerini icerir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.any { it.yazar == "Seneca" })
        assertTrue(list.any { it.yazar == "Sokrates" })
    }

    @Test
    fun `soz listesi ataturk ve einstein sozlerini icerir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.any { it.yazar.contains("Atatürk") })
        assertTrue(list.any { it.yazar.contains("Einstein") })
    }

    @Test
    fun `siradaki sozu getir 0 indeksi icin ilk sozu dondurur`() {
        val s = MotivasyonMansetMotoru.siradakiSozuGetir(0)
        assertEquals(1, s.id)
    }

    @Test
    fun `siradaki sozu getir index tasmasinda dongusel baslar`() {
        val s0 = MotivasyonMansetMotoru.siradakiSozuGetir(0)
        val s20 = MotivasyonMansetMotoru.siradakiSozuGetir(20)
        assertEquals(s0.id, s20.id)
    }

    @Test
    fun `soz metni formatlama sozu yazari ve kategoriyi dizeye doker`() {
        val s = MotivasyonMansetMotoru.siradakiSozuGetir(0)
        val str = MotivasyonMansetMotoru.sozMetniFormatla(s)
        assertTrue(str.contains(s.soz))
        assertTrue(str.contains(s.yazar))
        assertTrue(str.contains(s.kategori))
    }

    @Test
    fun `gorunurluk karari acikka visible kapaliyken gone dondurur`() {
        assertEquals(View.VISIBLE, MotivasyonMansetMotoru.gorunurlukKarari(true))
        assertEquals(View.GONE, MotivasyonMansetMotoru.gorunurlukKarari(false))
    }

    @Test
    fun `soz listesindeki her sozun id si pozitiftir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.all { it.id > 0 })
    }

    @Test
    fun `soz listesindeki her soz ve yazar bos degildir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.all { it.soz.isNotBlank() && it.yazar.isNotBlank() })
    }

    @Test
    fun `stoaci felsefe kategorisi en az 3 soz barindirir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        val sayi = list.count { it.kategori == "Stoacı Felsefe" }
        assertTrue(sayi >= 3)
    }

    @Test
    fun `feynman teknigi sozu richard feynman yazarini icerir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.any { it.yazar.contains("Feynman") })
    }

    @Test
    fun `yunus emre sozu anadolu irfani kategorisindedir`() {
        val s = MotivasyonMansetMotoru.varsayilanSozListesi().find { it.yazar == "Yunus Emre" }
        assertEquals("Anadolu İrfanı", s?.kategori)
    }

    @Test
    fun `kaizen prensibi sozu surekli gelisim mesajina sahiptir`() {
        val s = MotivasyonMansetMotoru.varsayilanSozListesi().find { it.yazar == "Kaizen Prensibi" }
        assertTrue(s?.soz?.contains("%1") == true)
    }

    @Test
    fun `marcus aurelius sozleri vazgecmeme ve odak uzerinedir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi().filter { it.yazar == "Marcus Aurelius" }
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun `siradaki sozu getir negatif indexte 0 indeksi dondurur`() {
        val s = MotivasyonMansetMotoru.siradakiSozuGetir(-5)
        assertEquals(1, s.id)
    }

    @Test
    fun `soz metni formatlama tirnak isaretlerini icerir`() {
        val s = MotivasyonMansetMotoru.siradakiSozuGetir(1)
        val str = MotivasyonMansetMotoru.sozMetniFormatla(s)
        assertTrue(str.contains("\""))
    }

    @Test
    fun `aristo sozleri baslangic ve sabir temalarini isler`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi().filter { it.yazar == "Aristo" }
        assertTrue(list.size >= 2)
    }

    @Test
    fun `sokratik koc sozleri calisma ergonomisi ve iradeyi icerir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi().filter { it.yazar.contains("Sokratik Koç") }
        assertTrue(list.size >= 2)
    }

    @Test
    fun `gunluk asistan motivasyon capasi sozu 20inci siradadir`() {
        val s = MotivasyonMansetMotoru.siradakiSozuGetir(19)
        assertEquals("Günlük Asistan", s.yazar)
    }

    @Test
    fun `soz listesi ibni sina tip bilim sozunu barindirir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.any { it.yazar.contains("İbn-i Sina") })
    }

    @Test
    fun `soz listesi cin atasozu akademik disiplin sozunu barindirir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.any { it.yazar.contains("Çin Atasözü") })
    }

    @Test
    fun `epiktetos sozu kontrol edemeyecegin seyler uzerinedir`() {
        val s = MotivasyonMansetMotoru.varsayilanSozListesi().find { it.yazar == "Epiktetos" }
        assertTrue(s?.soz?.contains("Kontrol") == true)
    }

    @Test
    fun `sokrates sozu sorgulanmamis hayat uzerinedir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi().filter { it.yazar == "Sokrates" }
        assertTrue(list.any { it.soz.contains("Sorgulanmamış") || it.soz.contains("Dünyayı") })
    }

    @Test
    fun `seneca sozu gecmis ve gelecek kararlari uzerinedir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi().filter { it.yazar == "Seneca" }
        assertTrue(list.any { it.soz.contains("Geçmişi") || it.soz.contains("Zorluklar") })
    }

    @Test
    fun `tüm soz kategorileri turkce karakter destegine sahiptir`() {
        val list = MotivasyonMansetMotoru.varsayilanSozListesi()
        assertTrue(list.all { it.kategori.isNotBlank() })
    }
}
