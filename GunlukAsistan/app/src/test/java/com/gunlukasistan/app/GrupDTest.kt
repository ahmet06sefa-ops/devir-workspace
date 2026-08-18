package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * v10.12 · ULTRA-30 / GRUP D (D19–D24) birim testleri.
 *
 * Kapsam:
 *   D19 — NefesProgrami: faz hesabı, halka ölçeği, döngü süreleri
 *   D20 — Hayalet: kümülatif karşılaştırma, rakip gün, oran kelepçesi
 *   D22 — SesManzarasi: otomasyon karar tablosu, mola kısması
 *   D23 — OdakRitim: işaret doluluğu, kutlama kuralı
 *   D21 — OdakKalkani: uyarı kapıları, cooldown, paket listesi, gün sıfırlama
 *
 * Android çağrısı yapılmaz (framework'süz); takvim yalnız java.util.
 */
class GrupDTest {

    private fun ms(yil: Int, ay: Int, gun: Int, saat: Int, dk: Int): Long =
        Calendar.getInstance().apply {
            set(yil, ay - 1, gun, saat, dk, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ---------------- D19 · NefesProgrami ----------------

    @Test
    fun nefes_donguSureleri() {
        assertEquals(19, NefesProgrami.desen(NefesProgrami.DESEN_478).donguSn)
        assertEquals(16, NefesProgrami.desen(NefesProgrami.DESEN_KUTU).donguSn)
        assertEquals(10, NefesProgrami.desen(NefesProgrami.DESEN_SAKIN).donguSn)
    }

    @Test
    fun nefes_fazBul_sinirlar() {
        val d = NefesProgrami.desen(NefesProgrami.DESEN_478)
        assertEquals(0 to 0.0, NefesProgrami.fazBul(d, 0.0))
        // 4.0 saniyede AL biter, TUT başlar
        assertEquals(1 to 0.0, NefesProgrami.fazBul(d, 4.0))
        // 4+3 = 7. saniyede TUT'un 3/7'si
        val (faz, oran) = NefesProgrami.fazBul(d, 7.0)
        assertEquals(1, faz)
        assertEquals(3.0 / 7.0, oran, 0.0001)
        // Tam döngü sonu: son faz bitmiş raporlanır
        assertEquals(2 to 1.0, NefesProgrami.fazBul(d, 19.0))
        // Negatif giriş kelepçelenir
        assertEquals(0 to 0.0, NefesProgrami.fazBul(d, -3.0))
    }

    @Test
    fun nefes_olcek_hareketi() {
        val d = NefesProgrami.desen(NefesProgrami.DESEN_478)
        // AL büyür
        assertEquals(0.55f, NefesProgrami.olcek(d, 0, 0.0), 0.001f)
        assertEquals(1.0f, NefesProgrami.olcek(d, 0, 1.0), 0.001f)
        assertTrue(NefesProgrami.olcek(d, 0, 0.7) > NefesProgrami.olcek(d, 0, 0.3))
        // AL sonrası TUT dolu durur
        assertEquals(1.0f, NefesProgrami.olcek(d, 1, 0.5), 0.001f)
        // VER küçülür
        assertEquals(1.0f, NefesProgrami.olcek(d, 2, 0.0), 0.001f)
        assertEquals(0.55f, NefesProgrami.olcek(d, 2, 1.0), 0.001f)
        assertTrue(NefesProgrami.olcek(d, 2, 0.8) < NefesProgrami.olcek(d, 2, 0.2))
    }

    @Test
    fun nefes_olcek_kutu_boslugu() {
        val d = NefesProgrami.desen(NefesProgrami.DESEN_KUTU)
        // VER sonrası BOS boş durur
        assertEquals(0.55f, NefesProgrami.olcek(d, 3, 0.5), 0.001f)
        // Kutu deseninde 0. indeks AL; oran 1.2 kelepçelenerek 1.0'a varır
        assertEquals(1.0f, NefesProgrami.olcek(d, 0, 1.2), 0.001f)
    }

    // ---------------- D20 · Hayalet ----------------

    @Test
    fun hayalet_rakipGun() {
        val simdi = ms(2026, 8, 8, 12, 0)
        assertEquals("20260808", Hayalet.gunAnahtari(simdi))
        assertEquals("20260807", Hayalet.rakipGunAnahtari(Hayalet.MOD_DUN, simdi))
        assertEquals("20260801", Hayalet.rakipGunAnahtari(Hayalet.MOD_HAFTA, simdi))
    }

    @Test
    fun hayalet_kumulatifAdi() {
        val dun = "20260807"
        val bugun = "20260808"
        val oturumlar = listOf(
            ms(2026, 8, 7, 10, 5) to 25,   // dün sabah seansı
            ms(2026, 8, 7, 14, 40) to 25,  // dün öğleden sonra
            ms(2026, 8, 8, 11, 0) to 30    // bugün sabah
        )
        val simdi = ms(2026, 8, 8, 12, 30)
        val sinir = Hayalet.gununDakikasi(simdi)
        // Bu saate kadar: dün yalnız sabahki; bugünkü seans sayılır
        assertEquals(25, Hayalet.buSaateKadarDk(oturumlar, dun, sinir))
        assertEquals(30, Hayalet.buSaateKadarDk(oturumlar, bugun, sinir))
        // Tam gün: dünkü ikisi birden
        assertEquals(50, Hayalet.tamGunDk(oturumlar, dun))
        assertEquals(30, Hayalet.tamGunDk(oturumlar, bugun))
        // Bilinmeyen gün: sıfır
        assertEquals(0, Hayalet.buSaateKadarDk(oturumlar, "20260101", sinir))
    }

    @Test
    fun hayalet_macOranlari() {
        val m = Hayalet.Mac(rakipDk = 25, senDk = 30, rakipTam = 50, senTam = 30)
        assertEquals(5, m.fark)
        assertEquals(0.5f, m.rakipOran, 0.001f)
        assertEquals(0.6f, m.senOran, 0.001f)

        // Rakip boşsa oranlar sıfırlanır (bölme kazası yok)
        val bos = Hayalet.Mac(rakipDk = 0, senDk = 10, rakipTam = 0, senTam = 10)
        assertEquals(0f, bos.rakipOran, 0.0f)
        assertEquals(0f, bos.senOran, 0.0f)

        // Sen günü uçarsan yay taşmaz
        val ucurum = Hayalet.Mac(rakipDk = 10, senDk = 500, rakipTam = 40, senTam = 500)
        assertEquals(1f, ucurum.senOran, 0.0f)
    }

    // ---------------- D22 · SesManzarasi ----------------

    @Test
    fun manzara_kararTablosu() {
        // Otomatik kapalı + otomatik akış çalıyor → kapat
        assertEquals(
            SesManzarasi.Aksiyon.KAPAT,
            SesManzarasi.aksiyon(false, kosuyor = true, secimVar = true,
                caliyorMu = true, otoCaliyor = true, manuelKapatti = false)
        )
        // Otomatik kapalı, manuel akış → karışma
        assertEquals(
            SesManzarasi.Aksiyon.HIC,
            SesManzarasi.aksiyon(false, kosuyor = true, secimVar = true,
                caliyorMu = true, otoCaliyor = false, manuelKapatti = false)
        )
        // Odak başladı, hiçbir şey çalmıyor → başlat
        assertEquals(
            SesManzarasi.Aksiyon.CAL,
            SesManzarasi.aksiyon(true, kosuyor = true, secimVar = true,
                caliyorMu = false, otoCaliyor = false, manuelKapatti = false)
        )
        // Kullanıcı bu oturumda eliyle kapattı → saygı duy
        assertEquals(
            SesManzarasi.Aksiyon.HIC,
            SesManzarasi.aksiyon(true, kosuyor = true, secimVar = true,
                caliyorMu = false, otoCaliyor = false, manuelKapatti = true)
        )
        // Koşarken bir şey çalıyor → yalnız ses uygula (mola kısması)
        assertEquals(
            SesManzarasi.Aksiyon.SES_UYGULA,
            SesManzarasi.aksiyon(true, kosuyor = true, secimVar = true,
                caliyorMu = true, otoCaliyor = false, manuelKapatti = false)
        )
        // Sayaç durdu → otomatik akış susar, manuel akış yaşar
        assertEquals(
            SesManzarasi.Aksiyon.KAPAT,
            SesManzarasi.aksiyon(true, kosuyor = false, secimVar = true,
                caliyorMu = true, otoCaliyor = true, manuelKapatti = false)
        )
        assertEquals(
            SesManzarasi.Aksiyon.HIC,
            SesManzarasi.aksiyon(true, kosuyor = false, secimVar = true,
                caliyorMu = true, otoCaliyor = false, manuelKapatti = false)
        )
        // Seçim yok → hiçbir şey başlamaz
        assertEquals(
            SesManzarasi.Aksiyon.HIC,
            SesManzarasi.aksiyon(true, kosuyor = true, secimVar = false,
                caliyorMu = false, otoCaliyor = false, manuelKapatti = false)
        )
    }

    @Test
    fun manzara_molaHacmi() {
        assertEquals(1.0f, SesManzarasi.hacim(molada = false, kisAcik = true), 0.0f)
        assertEquals(0.25f, SesManzarasi.hacim(molada = true, kisAcik = true), 0.0f)
        assertEquals(1.0f, SesManzarasi.hacim(molada = true, kisAcik = false), 0.0f)
    }

    // ---------------- D23 · OdakRitim ----------------

    @Test
    fun ritim_isaretVeKutlama() {
        assertEquals(3, OdakRitim.doluIsaret(3, 4))
        assertEquals(4, OdakRitim.doluIsaret(9, 4))   // taşma kelepçelenir
        assertEquals(0, OdakRitim.doluIsaret(3, 0))    // hedef kapalı
        assertEquals(0, OdakRitim.doluIsaret(3, -2))

        assertTrue(OdakRitim.kutlamaGerekliMi(4, 4, false))
        assertTrue(OdakRitim.kutlamaGerekliMi(5, 4, false))
        assertFalse(OdakRitim.kutlamaGerekliMi(4, 4, true))  // günde bir kez
        assertFalse(OdakRitim.kutlamaGerekliMi(3, 4, false))
        assertFalse(OdakRitim.kutlamaGerekliMi(4, 0, false))
    }

    // ---------------- D21 · OdakKalkani ----------------

    @Test
    fun kalkan_uyariKapilari() {
        val simdi = 1_000_000L
        val eski = simdi - 200_000L
        // Her şey uygun → uyar
        assertTrue(
            OdakKalkani.uyariGerekliMi(true, true, true, false, true, simdi, eski)
        )
        // Tek tek kapı kapat
        assertFalse(OdakKalkani.uyariGerekliMi(false, true, true, false, true, simdi, eski))
        assertFalse(OdakKalkani.uyariGerekliMi(true, false, true, false, true, simdi, eski))
        assertFalse(OdakKalkani.uyariGerekliMi(true, true, false, false, true, simdi, eski))
        assertFalse(OdakKalkani.uyariGerekliMi(true, true, true, true, true, simdi, eski))
        assertFalse(OdakKalkani.uyariGerekliMi(true, true, true, false, false, simdi, eski))
    }

    @Test
    fun kalkan_cooldownSinir() {
        val son = 500_000L
        // Tam cooldown sınırında UYAR (>=), bir milisaniye erken UYARMA
        assertTrue(OdakKalkani.uyariGerekliMi(true, true, true, false, true, son + 120_000L, son))
        assertFalse(OdakKalkani.uyariGerekliMi(true, true, true, false, true, son + 119_999L, son))
    }

    @Test
    fun kalkan_paketListesi() {
        val paketler = setOf("com.b.app", "com.a.app", "com.c.app")
        val metin = OdakKalkani.paketleriBirlestir(paketler)
        assertEquals(paketler, OdakKalkani.paketleriCoz(metin))
        // Boşluk/çöp temizlenir
        assertEquals(setOf("com.a.x"), OdakKalkani.paketleriCoz(" , com.a.x,,ab,"))
        assertTrue(OdakKalkani.paketleriCoz(null).isEmpty())
        assertTrue(OdakKalkani.paketleriCoz("").isEmpty())
    }

    @Test
    fun kalkan_gunSifirlama() {
        assertEquals(0, OdakKalkani.bugunIhlal("20260807", "20260808", 5))
        assertEquals(5, OdakKalkani.bugunIhlal("20260808", "20260808", 5))
    }
}
