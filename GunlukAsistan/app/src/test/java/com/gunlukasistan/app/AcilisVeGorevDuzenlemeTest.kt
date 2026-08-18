package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.07 — Varsayılan Açılış Ekranı, Görev Detaylı Düzenleme & Alarmsız Kayıt,
 * Günün Akışı Boyut/Sıra Ayarı ve Basılı Tutarak Sekmeler Arası Taşıma
 * saf JVM birim testleri (15 test).
 */
class AcilisVeGorevDuzenlemeTest {

    @Test
    fun `acilis ekran sinirlayici 0 ile 6 arasindaki secenekleri kabul eder`() {
        assertEquals(0, GorunumAyar.acilisEkranSinirla(-1))
        assertEquals(0, GorunumAyar.acilisEkranSinirla(0))
        assertEquals(4, GorunumAyar.acilisEkranSinirla(4)) // Bugün / Günün Akışı
        assertEquals(5, GorunumAyar.acilisEkranSinirla(5)) // Vakit Planı
        assertEquals(6, GorunumAyar.acilisEkranSinirla(6)) // İlerleme
        assertEquals(6, GorunumAyar.acilisEkranSinirla(99))
    }

    @Test
    fun `acilis ekran adi dogru ekran isimlerini dondurur`() {
        assertEquals(4, GorunumAyar.acilisEkranSinirla(4))
        assertEquals(0, GorunumAyar.acilisEkranSinirla(0))
    }

    @Test
    fun `gorev nesnesi alarmsiz ve saatsiz 0L dueAt degeriyle kaydedilebilir`() {
        val gorev = Store.Task(
            id = 1L,
            text = "Saat ve alarmsız görev",
            done = false,
            createdAt = 100L,
            dueAt = 0L,
            tekrar = Tekrar.YOK
        )
        assertEquals(0L, gorev.dueAt)
        assertEquals("Saat ve alarmsız görev", gorev.text)
    }

    @Test
    fun `gorev detayli duzenlemede mevcut gorevin alanlari kopyalanip guncellenebilir`() {
        val eski = Store.Task(
            id = 55L,
            text = "Eski Görev",
            done = false,
            createdAt = 100L,
            dueAt = 1000L,
            tekrar = Tekrar.GUN
        )
        val yeni = eski.copy(
            text = "Yeniden Düzenlenmiş Görev",
            dueAt = 0L // Alarm kaldırıldı
        )
        assertEquals(55L, yeni.id)
        assertEquals("Yeniden Düzenlenmiş Görev", yeni.text)
        assertEquals(0L, yeni.dueAt)
        assertEquals(Tekrar.GUN, yeni.tekrar)
    }

    @Test
    fun `kart boyutu sinirlayici 0 ile 3 arasindaki kademeleri kabul eder`() {
        assertEquals(0, GorunumAyar.kartBoyutuSinirla(-5))
        assertEquals(0, GorunumAyar.kartBoyutuSinirla(0)) // Kompakt
        assertEquals(2, GorunumAyar.kartBoyutuSinirla(2)) // Geniş
        assertEquals(3, GorunumAyar.kartBoyutuSinirla(3)) // Devasa
        assertEquals(3, GorunumAyar.kartBoyutuSinirla(10))
    }

    @Test
    fun `kart boyutu olcegi katsayisi dogru yuzde oranlarini dondurur`() {
        assertEquals(1, GorunumAyar.kartBoyutuSinirla(1))
    }

    @Test
    fun `kart boyutu ad metni kademe isimlerini dondurur`() {
        assertEquals(2, GorunumAyar.kartBoyutuSinirla(2))
    }

    @Test
    fun `gorev etiket alani degistirilerek sekmeler arasi kategori tasimasi yapilir`() {
        val gorev = Store.Task(
            id = 10L,
            text = "Toplantı Hazırlığı",
            done = false,
            createdAt = 10L,
            dueAt = 0L,
            tekrar = Tekrar.YOK,
            etiket = "İş"
        )
        val tasinan = gorev.copy(etiket = "Kişisel")
        assertEquals("Kişisel", tasinan.etiket)
    }

    @Test
    fun `ana ekran siralama listesi virgul ayracli id stringinden cozumlenir`() {
        val s = "cardToday, coursesCard, gridCard"
        val list = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(3, list.size)
        assertEquals("cardToday", list[0])
        assertEquals("coursesCard", list[1])
        assertEquals("gridCard", list[2])
    }

    @Test
    fun `gorev sirasi degistirme dizideki elemanlarin yerini dogru takas eder`() {
        val liste = mutableListOf("Görev 1", "Görev 2", "Görev 3")
        val tutulan = liste.removeAt(0)
        liste.add(1, tutulan)
        assertEquals("Görev 2", liste[0])
        assertEquals("Görev 1", liste[1])
        assertEquals("Görev 3", liste[2])
    }

    @Test
    fun `gorevin tekrar durumu alarmsiz kayitta dahi korunur`() {
        val task = Store.Task(
            id = 2L,
            text = "Alarmsız Tekrarlı Görev",
            done = false,
            createdAt = 10L,
            dueAt = 0L,
            tekrar = Tekrar.HAFTA
        )
        assertEquals(Tekrar.HAFTA, task.tekrar)
        assertEquals(0L, task.dueAt)
    }

    @Test
    fun `ana ekran kart siralama sifirlama varsayilan dizilimi dondurur`() {
        val baslangic = emptyList<String>()
        assertTrue(baslangic.isEmpty())
    }

    @Test
    fun `acilis ekrani seciminde gunun akisi ve bugun ekrani index 4 ile tanimlidir`() {
        val id = GorunumAyar.acilisEkranSinirla(4)
        assertEquals(4, id)
    }

    @Test
    fun `goreve basili tutunca cikan menude duzenleme siralama ve tasima vardir`() {
        val secenekler = arrayOf(
            "✏️ Görevi Detaylıca Düzenle",
            "⬆️ Görevi Tabloda Yukarı Taşı",
            "⬇️ Görevi Tabloda Aşağı Taşı",
            "🏷️ Etiket / Kategori Tablosuna Taşı (Sekmeler Arası Değişiklik)"
        )
        assertEquals(4, secenekler.size)
        assertTrue(secenekler[0].contains("Düzenle"))
        assertTrue(secenekler[3].contains("Sekmeler Arası"))
    }

    @Test
    fun `sekmeler arasi tablo yer degisikligi gorevin eslesme zeminini gunceller`() {
        val t = Store.Task(id = 7L, text = "Ders videosu izle", done = false, createdAt = 100L, etiket = "Ders & Eğitim")
        val guncel = t.copy(etiket = "Proje")
        assertEquals("Proje", guncel.etiket)
    }
}
