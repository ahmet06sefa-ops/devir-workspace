package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.13 · ULTRA-30 / GRUP B (B7–B12) birim testleri.
 *
 * Kapsam:
 *   B7  — Kokpit: akrep/yelkovan açıları, ilerleme yüzdesi, seri metni
 *   B8  — TakvimMotoru: 42 hücre, Pazartesi başlangıcı, yoğunluk, ay kaydırma
 *   B9  — UykuPano: plan süresi, 7 gece doldurma, ölçek, oran
 *   B10 — OdakKutusu: halka yüzdesi (Kokpit.yuzde ile aynı çekirdek)
 *   B11 — WidgetZemin: köşe/alfa/yazı kademe eşlemeleri
 *   B12 — WidgetFiltre: etiket süzgeci karar tablosu
 *
 * Android çağrısı yapılmaz (framework'süz); takvim yalnız java.util.
 */
class GrupBTest {

    private fun ms(yil: Int, ay: Int, gun: Int, saat: Int, dk: Int): Long =
        Calendar.getInstance().apply {
            set(yil, ay - 1, gun, saat, dk, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ---------------- B7 · Kokpit açıları ----------------

    @Test
    fun kokpit_acilar_temel() {
        val (a0, y0) = Kokpit.acilar(0, 0)
        assertEquals(0f, a0, 0.0001f)
        assertEquals(0f, y0, 0.0001f)

        val (a3, _) = Kokpit.acilar(3, 0)
        assertEquals(90f, a3, 0.0001f)

        // 12:30 — akrep 12 ile 1 arasında yarıda (15°), yelkovan 6'da (180°)
        val (a12, y12) = Kokpit.acilar(12, 30)
        assertEquals(15f, a12, 0.0001f)
        assertEquals(180f, y12, 0.0001f)
    }

    @Test
    fun kokpit_acilar_geceYarisiOncesi() {
        val (a, y) = Kokpit.acilar(23, 59)
        assertEquals(359.5f, a, 0.0001f)
        assertEquals(354f, y, 0.0001f)
    }

    @Test
    fun kokpit_acilar_sinirDisiDakikaKelepcelenir() {
        val (_, y) = Kokpit.acilar(9, 99)
        assertEquals(354f, y, 0.0001f)
    }

    // ---------------- B7 + B10 · İlerleme yüzdesi ----------------

    @Test
    fun kokpit_yuzde_dogruOran() {
        assertEquals(0, Kokpit.yuzde(100_000L, 100_000L))  // henüz başladı
        assertEquals(75, Kokpit.yuzde(25_000L, 100_000L))
        assertEquals(50, Kokpit.yuzde(500L, 1_000L))
        assertEquals(100, Kokpit.yuzde(0L, 100_000L))      // bitti
    }

    @Test
    fun kokpit_yuzde_korunanKosular() {
        // Toplam bilinmiyorsa bölme hatası yerine 0
        assertEquals(0, Kokpit.yuzde(10L, 0L))
        assertEquals(0, Kokpit.yuzde(10L, -5L))
        // Kalan negatife (taşmaya) düştüyse 100'e kelepçelenir
        assertEquals(100, Kokpit.yuzde(-5L, 100L))
        // Kalan toplamı aşarsa 0'a kelepçelenir
        assertEquals(0, Kokpit.yuzde(250L, 100L))
    }

    @Test
    fun kokpit_seriGun_negatifOlmaz() {
        assertEquals(0, Kokpit.seriGun(-5))
        assertEquals(0, Kokpit.seriGun(0))
        assertEquals(12, Kokpit.seriGun(12))
    }

    // ---------------- B8 · TakvimMotoru ----------------

    @Test
    fun takvim_yogunluk_esikler() {
        assertEquals(0, TakvimMotoru.yogunluk(0))
        assertEquals(1, TakvimMotoru.yogunluk(1))
        assertEquals(1, TakvimMotoru.yogunluk(2))
        assertEquals(2, TakvimMotoru.yogunluk(3))
        assertEquals(2, TakvimMotoru.yogunluk(4))
        assertEquals(3, TakvimMotoru.yogunluk(5))
        assertEquals(3, TakvimMotoru.yogunluk(100))
    }

    @Test
    fun takvim_ayKaydir_yilTasmasi() {
        assertEquals(2026 to 8, TakvimMotoru.ayKaydir(2026, 7, 1))
        assertEquals(2025 to 11, TakvimMotoru.ayKaydir(2026, 0, -1))
        assertEquals(2027 to 0, TakvimMotoru.ayKaydir(2026, 11, 1))
        assertEquals(2026 to 7, TakvimMotoru.ayKaydir(2026, 7, 0))
    }

    @Test
    fun takvim_ofsetKelepce_artiEksi12() {
        assertEquals(12, TakvimMotoru.ofsetKelepce(20))
        assertEquals(-12, TakvimMotoru.ofsetKelepce(-30))
        assertEquals(5, TakvimMotoru.ofsetKelepce(5))
        assertEquals(0, TakvimMotoru.ofsetKelepce(0))
    }

    @Test
    fun takvim_hucreler_42VePazartesi() {
        // Ağustos 2026: ayın 1'i Cumartesi → ızgara 27 Temmuz Pazartesi başlar
        val h = TakvimMotoru.hucreler(2026, 7, ms(2026, 8, 8, 12, 0))
        assertEquals(42, h.size)

        // İlk hücre: 27 Temmuz (ay dışı, Temmuz)
        assertEquals(27, h[0].gun)
        assertTrue(h[0].ayDisi)
        assertEquals(6, h[0].ay0)
        assertEquals(2026, h[0].yil)

        // Ayın 1'i 6. hücrede (0-tabanlı 5) ve ay içi
        assertEquals(1, h[5].gun)
        assertFalse(h[5].ayDisi)
        assertEquals(7, h[5].ay0)

        // Bugün (8 Ağustos) doğru işaretli; komşuları değil
        assertEquals(8, h[12].gun)
        assertTrue(h[12].bugunMu)
        assertFalse(h[11].bugunMu)
        assertFalse(h[13].bugunMu)

        // Izgara kesintisiz akar: her hücre bir önceki gün +1
        for (i in 1 until h.size) {
            val once = Calendar.getInstance().apply { set(h[i-1].yil, h[i-1].ay0, h[i-1].gun) }
            once.add(Calendar.DAY_OF_YEAR, 1)
            assertEquals(once.get(Calendar.DAY_OF_MONTH), h[i].gun)
        }
    }

    // ---------------- B9 · UykuPano ----------------

    @Test
    fun uyku_planMs_geceYarisiAsimi() {
        // 23:00 → 07:00 = 8 saat
        assertEquals(28_800_000L, UykuPano.planMs(420, 1380))
        // 08:00 → 00:00 = 16 saat
        assertEquals(57_600_000L, UykuPano.planMs(0, 480))
    }

    @Test
    fun uyku_son7_doldurmaVeSiralama() {
        // 9 kayıt: son 7 korunur, sıra eski → yeni
        val dokuz = UykuPano.son7(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))
        assertEquals(listOf(3L, 4L, 5L, 6L, 7L, 8L, 9L), dokuz)

        // 2 kayıt: sol taraf 0 ile dolar
        val iki = UykuPano.son7(listOf(2L, 5L))
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 2L, 5L), iki)

        // Hiç kayıt: 7 sıfır
        assertEquals(7, UykuPano.son7(emptyList()).size)
    }

    @Test
    fun uyku_maksVeOran() {
        // Kayıt ve plan yoksa 8 saatlik gövde
        assertEquals(8L * 3_600_000L, UykuPano.maksMs(0L, emptyList()))

        // En uzun gece tavanı belirler
        val dokuzSaat = 9L * 3_600_000L
        assertEquals(dokuzSaat, UykuPano.maksMs(0L, listOf(dokuzSaat)))

        assertEquals(0.5f, UykuPano.oran(4L * 3_600_000L, 8L * 3_600_000L), 0.0001f)
        assertEquals(1f, UykuPano.oran(20L * 3_600_000L, 8L * 3_600_000L), 0.0001f)
        assertEquals(0f, UykuPano.oran(5L, 0L), 0.0001f)
    }

    // ---------------- B11 · WidgetZemin kademeleri ----------------

    @Test
    fun zemin_koseDp_kademeler() {
        assertEquals(6f, WidgetZemin.koseDp(0), 0.0001f)
        assertEquals(26f, WidgetZemin.koseDp(1), 0.0001f)
        assertEquals(38f, WidgetZemin.koseDp(2), 0.0001f)
        assertEquals(48f, WidgetZemin.koseDp(3), 0.0001f)
        // Aralık dışı güvenli biçimde uca oturur
        assertEquals(48f, WidgetZemin.koseDp(99), 0.0001f)
        assertEquals(6f, WidgetZemin.koseDp(-1), 0.0001f)
    }

    @Test
    fun zemin_alfaVeYaziCarpan() {
        assertEquals(0xFF, WidgetZemin.saydamlikAlfa(0))
        assertEquals(0xA8, WidgetZemin.saydamlikAlfa(3))
        assertEquals(0xC4, WidgetZemin.saydamlikAlfa(2))

        assertEquals(0.85f, WidgetZemin.yaziCarpan(0), 0.0001f)
        assertEquals(1.0f, WidgetZemin.yaziCarpan(1), 0.0001f)
        assertEquals(1.15f, WidgetZemin.yaziCarpan(2), 0.0001f)
        assertEquals(1.0f, WidgetZemin.yaziCarpan(7), 0.0001f)
    }

    // ---------------- B12 · WidgetFiltre ----------------

    @Test
    fun filtre_gecerMi_tablo() {
        // Filtre boş → her şey geçer
        assertTrue(WidgetFiltre.gecerMi("i", ""))
        assertTrue(WidgetFiltre.gecerMi("", ""))

        // Dolu filtre: yalnız birebir eşleşme
        assertTrue(WidgetFiltre.gecerMi("i", "i"))
        assertFalse(WidgetFiltre.gecerMi("e", "i"))
        assertFalse(WidgetFiltre.gecerMi("", "i"))   // etiketsizler süzülür
    }
}
