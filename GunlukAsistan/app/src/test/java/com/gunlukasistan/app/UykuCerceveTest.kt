package com.gunlukasistan.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.9 · Gün çerçevesi — saf mantık birim testleri.
 *
 * Context gerektiren preferans/defter katmanı burada YOK (android.jar
 * taslağı birim testte çöker); yalnız Context'siz hesaplar sınanır.
 * Tarih kurulumları hep [Calendar] üzerinden yapılır — iki yönlü
 * dönüşümde zaman dilimi sabit kaldığı için her ortamda belirleyici.
 */
class UykuCerceveTest {

    /** Yerel takvimde belirli gün/saat milisaniyesi. */
    private fun ms(gun: Int, saat: Int, dakika: Int): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, gun, saat, dakika, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ── gunKey ───────────────────────────────────────────────────

    @Test
    fun gunKey_temel() {
        assertEquals("20260807", UykuCerceve.gunKey(ms(7, 10, 30)))
    }

    @Test
    fun gunKey_geceYarisiOncesiAyniGun() {
        // 23:59 hâlâ aynı gün anahtarında olmalı
        assertEquals("20260807", UykuCerceve.gunKey(ms(7, 23, 59)))
    }

    // ── saatMetni ────────────────────────────────────────────────

    @Test
    fun saatMetni_temel() {
        assertEquals("07:00", UykuCerceve.saatMetni(420))
        assertEquals("23:15", UykuCerceve.saatMetni(1395))
    }

    @Test
    fun saatMetni_sinirlarKisitlanir() {
        assertEquals("00:00", UykuCerceve.saatMetni(-5))
        assertEquals("23:59", UykuCerceve.saatMetni(1500))
    }

    // ── sonrakiAlarm ─────────────────────────────────────────────

    @Test
    fun sonrakiAlarm_gelecekseAyniGun() {
        val simdi = ms(7, 6, 30)
        val hedef = UykuCerceve.sonrakiAlarm(simdi, 420)
        assertEquals(ms(7, 7, 0), hedef)
    }

    @Test
    fun sonrakiAlarm_gectiyseYarinaTasir() {
        val simdi = ms(7, 8, 0)
        assertEquals(ms(8, 7, 0), UykuCerceve.sonrakiAlarm(simdi, 420))
    }

    @Test
    fun sonrakiAlarm_tamSimdiyseYarinaTasir() {
        // Alarm ANI eşitse "bu saat geçti" sayılır; bugüne yeniden kurulmaz.
        val simdi = ms(7, 7, 0)
        assertEquals(ms(8, 7, 0), UykuCerceve.sonrakiAlarm(simdi, 420))
    }

    // ── uykuSuresiMs ─────────────────────────────────────────────

    @Test
    fun uykuSuresiMs_normal() {
        val uyudu = ms(7, 23, 30)
        val uyandi = ms(8, 7, 30)
        assertEquals(8 * 3600_000L, UykuCerceve.uykuSuresiMs(uyudu, uyandi))
    }

    @Test
    fun uykuSuresiMs_tersSiraNull() {
        // Uyanma, uymadan ÖNCE olamaz — ölçüm hatası, defteri kirletmez.
        assertNull(UykuCerceve.uykuSuresiMs(ms(8, 12, 0), ms(8, 7, 0)))
    }

    @Test
    fun uykuSuresiMs_esitseNull() {
        assertNull(UykuCerceve.uykuSuresiMs(ms(8, 7, 0), ms(8, 7, 0)))
    }

    @Test
    fun uykuSuresiMs_sifirGirdiNull() {
        assertNull(UykuCerceve.uykuSuresiMs(0L, ms(8, 7, 0)))
    }

    @Test
    fun uykuSuresiMs_cokUzunNull() {
        // 21 saat "uyku" → telefon kapanmış/uyku defteri kirlenmiş demektir.
        val uyudu = ms(7, 23, 0)
        val uyandi = uyudu + 21 * 3600_000L
        assertNull(UykuCerceve.uykuSuresiMs(uyudu, uyandi))
    }

    @Test
    fun uykuSuresiMs_tamSinirdaGecerli() {
        val uyudu = ms(7, 23, 0)
        val uyandi = uyudu + UykuCerceve.MAKS_UYKU_SAAT * 3600_000L
        assertEquals(
            UykuCerceve.MAKS_UYKU_SAAT * 3600_000L,
            UykuCerceve.uykuSuresiMs(uyudu, uyandi)
        )
    }

    // ── dakikaOfMs ───────────────────────────────────────────────

    @Test
    fun dakikaOfMs_temel() {
        assertEquals(825, UykuCerceve.dakikaOfMs(ms(7, 13, 45)))
        assertEquals(0, UykuCerceve.dakikaOfMs(ms(7, 0, 0)))
    }

    // ── ortalamalar ──────────────────────────────────────────────

    @Test
    fun ortalamaUyanmaDk_bosVeNormal() {
        assertNull(UykuCerceve.ortalamaUyanmaDk(emptyList()))
        assertEquals(420, UykuCerceve.ortalamaUyanmaDk(listOf(400, 440)))
    }

    @Test
    fun ortalamaUyumaDk_geceYarisiSarmasi() {
        // 23:50 ile 00:10 → düz ortalama 12:00 (öğlen!) çıkarırdı;
        // sarmal ortalama gece yarısında durmalı.
        val ort = UykuCerceve.ortalamaUyumaDk(listOf(1430, 10))
        assertEquals(0, ort)
    }

    @Test
    fun ortalamaUyumaDk_tekDegerAynenDoner() {
        assertEquals(1395, UykuCerceve.ortalamaUyumaDk(listOf(1395)))
        assertNull(UykuCerceve.ortalamaUyumaDk(emptyList()))
    }

    @Test
    fun ortalamaUykuMs_sifirlariEler() {
        val gunler = listOf(
            UykuCerceve.Gun("20260806", 1L, 1L, 8 * 3600_000L),
            UykuCerceve.Gun("20260807", 1L, 1L, 0L), // süresi belli olmayan gün
            UykuCerceve.Gun("20260808", 1L, 1L, 6 * 3600_000L)
        )
        assertEquals(7 * 3600_000L, UykuCerceve.ortalamaUykuMs(gunler))
    }

    @Test
    fun ortalamaUykuMs_bosNull() {
        assertNull(UykuCerceve.ortalamaUykuMs(emptyList()))
        assertNull(
            UykuCerceve.ortalamaUykuMs(listOf(UykuCerceve.Gun("20260807", 0L, 0L, 0L)))
        )
    }

    // ── sureKisa ─────────────────────────────────────────────────

    @Test
    fun sureKisa_saatVeDakika() {
        assertEquals("7 sa 32 dk", UykuCerceve.sureKisa((7 * 60 + 32) * 60_000L))
        assertEquals("45 dk", UykuCerceve.sureKisa(45 * 60_000L))
    }

    // ── ozetSecimi ───────────────────────────────────────────────

    @Test
    fun ozetSecimi_hepsiAcikSirali() {
        assertEquals(
            listOf(
                UykuCerceve.OzetParca.ODAK,
                UykuCerceve.OzetParca.GOREV,
                UykuCerceve.OzetParca.ZINCIR,
                UykuCerceve.OzetParca.SERI
            ),
            UykuCerceve.ozetSecimi(odak = true, gorev = true, seri = true, zincir = true)
        )
    }

    @Test
    fun ozetSecimi_kapaliVeHicbiri() {
        assertTrue(UykuCerceve.ozetSecimi(false, false, false, false).isEmpty())
        assertEquals(
            listOf(UykuCerceve.OzetParca.ODAK, UykuCerceve.OzetParca.SERI),
            UykuCerceve.ozetSecimi(odak = true, gorev = false, seri = true, zincir = false)
        )
    }

    // ── tekrar kararları ─────────────────────────────────────────

    @Test
    fun tekrarGerekliMi_butunKosullar() {
        // Normal: sayaç dolmadı, cevap yok → sor
        assertTrue(UykuCerceve.tekrarGerekliMi(0, 3, cevaplandi = false, verildi = false))
        // Sayaç doldu → sus
        assertTrue(
            !UykuCerceve.tekrarGerekliMi(3, 3, cevaplandi = false, verildi = false)
        )
        // Cevaplanmış kapı tekrarlanmaz
        assertTrue(
            !UykuCerceve.tekrarGerekliMi(0, 3, cevaplandi = true, verildi = false)
        )
        // Teslim edilmiş kapı tekrarlanmaz
        assertTrue(
            !UykuCerceve.tekrarGerekliMi(1, 3, cevaplandi = false, verildi = true)
        )
        // "Yalnız bir kez sor" (maks 0) → hiç tekrar olmaz
        assertTrue(!UykuCerceve.tekrarGerekliMi(0, 0, cevaplandi = false, verildi = false))
    }

    @Test
    fun sonrakiTekrar_birArttirir() {
        assertEquals(1, UykuCerceve.sonrakiTekrar(0))
        assertEquals(4, UykuCerceve.sonrakiTekrar(3))
    }

    // ── aksamGunAnahtari (öz denetim bulgusu) ────────────────────

    @Test
    fun aksamGunAnahtari_geceYarisiOncekiAksamaAit() {
        // 00:15'teki "Uyuyorum", o gün 23:00'teki özeti tüketmemeli.
        assertEquals("20260806", UykuCerceve.aksamGunAnahtari(ms(7, 0, 15)))
        // 23:20 aynı akşamın döngüsünde kalır
        assertEquals("20260807", UykuCerceve.aksamGunAnahtari(ms(7, 23, 20)))
        // Şafak sınırı: 06:00'dan itibaren yeni gün
        assertEquals("20260806", UykuCerceve.aksamGunAnahtari(ms(7, 5, 59)))
        assertEquals("20260807", UykuCerceve.aksamGunAnahtari(ms(7, 6, 30)))
    }

    // ── Gun JSON ─────────────────────────────────────────────────

    @Test
    fun gunJson_tamTur() {
        val gun = UykuCerceve.Gun("20260807", 1000L, 2000L, 3_600_000L)
        val geri = UykuCerceve.Gun.jsondan(gun.json())
        assertEquals(gun, geri)
    }

    @Test
    fun gunJsondan_bozuklarNull() {
        assertNull(UykuCerceve.Gun.jsondan(JSONObject()))
        assertNull(UykuCerceve.Gun.jsondan(JSONObject().put("g", "")))
    }
}
