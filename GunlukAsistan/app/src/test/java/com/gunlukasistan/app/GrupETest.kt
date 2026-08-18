package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.14 · ULTRA-30 / GRUP E (E25–E30) birim testleri.
 *
 * Kapsam:
 *   E25 — SabahPlani: 3 maddelik taslak seçimi, yarım önceliği
 *   E26 — Kronotip: uyanış ortalaması/tip/pencere hesapları
 *   E27 — MikroGunluk: duygu emoji/ortalama/iyi gün sayısı
 *   E28 — SesliKutu: bu hafta / daha eski bölümlemesi
 *   E29 — KartUretici: sözcük kaydırmacı (sert kırma + üç nokta)
 *   E30 — SeneFilmi: seri zinciri, yıllık özet (en çalışkan ay/gün)
 *
 * Android çağrısı yapılmaz (framework'süz); takvim/JSON yalnız java+org.json.
 */
class GrupETest {

    private fun ms(yil: Int, ay: Int, gun: Int, saat: Int, dk: Int): Long =
        Calendar.getInstance().apply {
            set(yil, ay - 1, gun, saat, dk, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ---------------- E25 · SabahPlani ----------------

    private fun g(id: Long, metin: String, dueAt: Long, done: Boolean = false) =
        SabahPlani.GorevOzet(id, metin, dueAt, done)

    @Test
    fun sabah_sec_yarimOncelikli() {
        val baslangic = ms(2026, 8, 8, 0, 0)
        val son = baslangic + 86_400_000L - 1
        val liste = listOf(
            g(1, "dünkü fatura", ms(2026, 8, 7, 10, 0)),
            g(2, "evvelki başvuru", ms(2026, 8, 6, 10, 0)),
            g(3, "bugünkü randevu", ms(2026, 8, 8, 15, 0)),
            g(4, "tarihsiz not", 0L)
        )
        val taslak = SabahPlani.sec(liste, baslangic, son)
        assertEquals(3, taslak.size)
        // Yarım kalanlar en eski önce gelir, bayraklıdır
        assertEquals(2L, taslak[0].gorevId)
        assertTrue(taslak[0].yarimMi)
        assertEquals(1L, taslak[1].gorevId)
        assertTrue(taslak[1].yarimMi)
        // Bugünlü gorev tamamlar
        assertEquals(3L, taslak[2].gorevId)
        assertFalse(taslak[2].yarimMi)
    }

    @Test
    fun sabah_sec_tarihsizleDolarVeBitenlerElenir() {
        val baslangic = ms(2026, 8, 8, 0, 0)
        val son = baslangic + 86_400_000L - 1
        val liste = listOf(
            g(9, "biten iş", 0L, done = true),
            g(8, "tarihsiz eski", 0L),
            g(7, "tarihsiz yeni", 0L)
        )
        val taslak = SabahPlani.sec(liste, baslangic, son)
        assertEquals(2, taslak.size)
        assertTrue(taslak.all { !it.yarimMi })
        assertTrue(SabahPlani.sec(emptyList(), baslangic, son).isEmpty())
        assertTrue(
            SabahPlani.sec(listOf(g(1, "hepsi bitmiş", 0L, true)), baslangic, son).isEmpty()
        )
    }

    // ---------------- E26 · Kronotip ----------------

    @Test
    fun kronotip_ortalamaVeSapma() {
        assertEquals(440, Kronotip.ortUyanis(listOf(440, 460, 420)))
        assertEquals(-1, Kronotip.ortUyanis(emptyList()))
        assertEquals(40, Kronotip.sapma(listOf(440, 460, 420)))
        assertEquals(0, Kronotip.sapma(listOf(500)))
    }

    @Test
    fun kronotip_tipSinirlari() {
        assertEquals(Kronotip.Tip.SERCE, Kronotip.tip(419))
        assertEquals(Kronotip.Tip.GUVENCIN, Kronotip.tip(420))
        assertEquals(Kronotip.Tip.GUVENCIN, Kronotip.tip(539))
        assertEquals(Kronotip.Tip.GECE_KUSU, Kronotip.tip(540))
        assertEquals(Kronotip.Tip.GUVENCIN, Kronotip.tip(-5))
        assertEquals("🦉", Kronotip.tipEmoji(Kronotip.Tip.GECE_KUSU))
    }

    @Test
    fun kronotip_pencereVeAralik() {
        assertEquals(9, Kronotip.odakPenceresi(-1))
        assertEquals(9, Kronotip.odakPenceresi(9))
        assertEquals(22, Kronotip.odakPenceresi(23))
        assertEquals("09:00–11:00", Kronotip.saatAralik(9))
        assertTrue(Kronotip.penceredeMi(600, 9))
        assertFalse(Kronotip.penceredeMi(700, 9))
    }

    // ---------------- E27 · MikroGunluk ----------------

    @Test
    fun gunluk_emojiVeKelepce() {
        assertEquals("😞", MikroGunluk.emojiFor(1))
        assertEquals("😐", MikroGunluk.emojiFor(3))
        assertEquals("😄", MikroGunluk.emojiFor(5))
        assertEquals("😞", MikroGunluk.emojiFor(0))
        assertEquals("😄", MikroGunluk.emojiFor(9))
    }

    @Test
    fun gunluk_ortalamaVeIyiSayisi() {
        assertEquals(4.3f, MikroGunluk.ortalama(listOf(4, 4, 5)), 0.05f)
        assertEquals(0f, MikroGunluk.ortalama(emptyList()), 0.001f)
        assertEquals(2, MikroGunluk.iyiSayisi(listOf(4, 3, 5, 1)))
    }

    // ---------------- E28 · SesliKutu ----------------

    @Test
    fun kutu_buHaftaBolumu() {
        val gun = 86_400_000L
        val simdi = 70 * gun
        val liste = listOf(
            SesliKutu.Not(65 * gun, "yeni not", "GOREV"),
            SesliKutu.Not(63 * gun, "sinirda", "NOT"),
            SesliKutu.Not(50 * gun, "eski not", "ALISVERIS")
        )
        val (hafta, eski) = SesliKutu.buHafta(liste, simdi)
        assertEquals(2, hafta.size)
        assertEquals(1, eski.size)
        assertEquals("eski not", eski[0].metin)
        assertEquals("🛒", SesliKutu.hedefEmoji("ALISVERIS"))
        assertEquals("✅", SesliKutu.hedefEmoji("GOREV"))
        assertEquals("📝", SesliKutu.hedefEmoji("NOT"))
    }

    // ---------------- E29 · KartUretici kaydırmacı ----------------

    @Test
    fun kart_satirlaraBol_temel() {
        assertEquals(
            listOf("kısa bir görev metni"),
            KartUretici.satirlaraBol("kısa bir görev metni", 24, 4)
        )
        val uclu = KartUretici.satirlaraBol(
            "bugün pazartesi matematik sınavına çalış ve on soru çöz", 24, 4
        )
        assertEquals(3, uclu.size)
        assertEquals("bugün pazartesi", uclu[0])
        assertEquals("matematik sınavına çalış", uclu[1])
        assertEquals("ve on soru çöz", uclu[2])
    }

    @Test
    fun kart_satirlaraBol_sertKirmaVeUcNokta() {
        // 24'ten uzun sözcük sert kırılır
        val uzun = KartUretici.satirlaraBol("paleontolojisosyolojiktir", 24, 4)
        assertEquals(2, uzun.size)
        assertEquals(24, uzun[0].length)
        assertEquals("r", uzun[1])

        // Tek satırda kalan metin "…" ile kapanır
        val tas = KartUretici.satirlaraBol("aaa bbb ccc", 5, 1)
        assertEquals(listOf("aaa…"), tas)

        // Boş girdi güvenli
        assertTrue(KartUretici.satirlaraBol("   ", 24, 4).isEmpty())
    }

    // ---------------- E30 · SeneFilmi ----------------

    @Test
    fun film_gunSonrasi_sinirlar() {
        assertEquals("20270101", SeneFilmi.gunSonrasi("20261231"))
        assertEquals("20260301", SeneFilmi.gunSonrasi("20260228")) // 2026 artık yıl değil
        assertEquals("20261202", SeneFilmi.gunSonrasi("20261201"))
    }

    @Test
    fun film_enUzunSeri_zincirler() {
        val aktifler = setOf(
            "20261201", "20261202", "20261203",
            "20261205", "20261206",
            "20261231", "20270101", "20270102"
        )
        assertEquals(3, SeneFilmi.enUzunSeri(aktifler))
        assertEquals(0, SeneFilmi.enUzunSeri(emptySet()))
        assertEquals(1, SeneFilmi.enUzunSeri(setOf("20261201")))
    }

    @Test
    fun film_hesapla_yillikOzet() {
        val gunluk = JSONObject()
            .put("20260305", JSONObject().put("c", 2).put("f", 90))
            .put("20260401", JSONObject().put("c", 1).put("f", 120))
            .put("20260402", JSONObject().put("c", 3).put("f", 240))
            .put("20261225", JSONObject().put("c", 0).put("f", 60))
            .put("20260101", JSONObject().put("c", 0).put("f", 0))   // pasif
            .put("20251231", JSONObject().put("c", 5).put("f", 300)) // önceki yıl

        val ozet = SeneFilmi.hesapla(2026, gunluk)
        assertEquals(4, ozet.aktifGun)
        assertEquals(510, ozet.toplamDk)
        assertEquals(3, ozet.enCaliskanAy)          // Nisan (0-tabanlı 3)
        assertEquals(360, ozet.enCaliskanAyDk)
        assertEquals("20260402", ozet.enUzunGunAnahtar)
        assertEquals(240, ozet.enUzunGunDk)
        assertEquals(2, ozet.enUzunSeri)            // 1-2 Nisan zinciri
    }
}
