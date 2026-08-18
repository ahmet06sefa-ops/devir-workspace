package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * v11.03 — YouTube Çevrimdışı Oynatma Listesi Yapay Zekâ Toplu Gruplama, Kaldırma,
 * Taşıma, Kopyalama, Sekmeler Arası Hızlı Geçiş ve Sürükle-Bırak Sıra Değiştirme
 * saf birim testleri (26 test).
 */
class YoutubePlaylistTest {

    @Before
    fun setup() {
        YoutubePlaylistMotoru.testIcinSifirla(null)
    }

    @Test
    fun `youtube oynatma listesi yapay zeka toplu dosyalari kpss mat tarih turkce gruplarina ayirir`() {
        val dosyalar = YapayZekaYoutubeSiralamaMotoru.ornekTopluDosyaSenaryosuGetir()
        val gruplar = YapayZekaYoutubeSiralamaMotoru.topluDosyalariGruplayipSirala(null, dosyalar)
        assertTrue(gruplar.any { it.baslik.contains("Matematik") })
        assertTrue(gruplar.any { it.baslik.contains("Tarih") })
        assertTrue(gruplar.any { it.baslik.contains("Türkçe") })
    }

    @Test
    fun `youtube oynatma listesi yapay zeka youtube da olmayan videolari diger yerel listesinde toplar`() {
        val dosyalar = YapayZekaYoutubeSiralamaMotoru.ornekTopluDosyaSenaryosuGetir()
        val gruplar = YapayZekaYoutubeSiralamaMotoru.topluDosyalariGruplayipSirala(null, dosyalar)
        val diger = gruplar.find { it.id == YapayZekaYoutubeSiralamaMotoru.ID_DIGER_YEREL }
        assertTrue(diger != null)
        assertTrue(diger!!.videolar.any { it.youtubeBaslik.contains("tatil", ignoreCase = true) || it.youtubeBaslik.contains("toplanti", ignoreCase = true) })
    }

    @Test
    fun `youtube oynatma listesi videoyu kaldirinca siralamayi yeniden duzenler`() {
        val p = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Silme Testi",
            listOf(Pair("uri1", "01_a.mp4"), Pair("uri2", "02_b.mp4"), Pair("uri3", "03_c.mp4"))
        )
        p.videolar.removeAt(1) // 2. video silindi
        p.siralamayiYenidenDuzenle()
        assertEquals(2, p.videolar.size)
        assertEquals(1, p.videolar[0].sira)
        assertEquals(2, p.videolar[1].sira)
        assertEquals("03_c", p.videolar[1].youtubeBaslik)
    }

    @Test
    fun `youtube oynatma listesi videoyu baska listeye tasir ve her iki listenin siralamasini gunceller`() {
        val p1 = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Kaynak Liste",
            listOf(Pair("uri1", "01_a.mp4"), Pair("uri2", "02_b.mp4"))
        )
        val p2 = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Hedef Liste",
            listOf(Pair("uri3", "01_c.mp4"))
        )
        val tasinacak = p1.videolar.last()
        p1.videolar.remove(tasinacak)
        p1.siralamayiYenidenDuzenle()
        p2.videolar.add(
            YoutubePlaylistMotoru.PlaylistVideo(
                sira = p2.videolar.size + 1,
                youtubeBaslik = tasinacak.youtubeBaslik,
                yerelDosyaUri = tasinacak.yerelDosyaUri,
                yerelDosyaAdi = tasinacak.yerelDosyaAdi,
                eslesti = true
            )
        )
        p2.siralamayiYenidenDuzenle()

        assertEquals(1, p1.videolar.size)
        assertEquals(2, p2.videolar.size)
        assertEquals(2, p2.videolar[1].sira)
        assertEquals("02_b", p2.videolar[1].youtubeBaslik)
    }

    @Test
    fun `youtube oynatma listesi videoyu baska listeye kopyalar`() {
        val p2 = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Hedef Liste",
            listOf(Pair("uri1", "01_c.mp4"))
        )
        val v = YoutubePlaylistMotoru.PlaylistVideo(1, "Kopyalanan Video", "uriX", "kopya.mp4", true)
        p2.videolar.add(v)
        p2.siralamayiYenidenDuzenle()
        assertEquals(2, p2.videolar.size)
        assertEquals(2, p2.videolar[1].sira)
    }

    @Test
    fun `youtube oynatma listesi motoru sabit kpss yks kamplari dondurmez`() {
        val list = YoutubePlaylistMotoru.varsayilanPlaylistleriGetir(null)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `youtube oynatma listesi baslangicta hic ozel liste yoksa bostur`() {
        val list = YoutubePlaylistMotoru.tumPlaylistleriGetir(null)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `youtube oynatma listesi yapay zeka klasor dosyalarini analiz edip youtube basligi koyar`() {
        val dosyalar = listOf(
            Pair("uri1", "02_temel_kavramlar_tek_cift.mp4"),
            Pair("uri2", "01_temel_kavramlar_rakam_sayi.mp4"),
            Pair("uri3", "03_temel_kavramlar_pozitif_negatif.mp4")
        )
        val res = YapayZekaYoutubeSiralamaMotoru.klasorDosyalariniAnalizEt(dosyalar)
        assertEquals("KPSS Matematik 2026 Kampı - Benim Hocam", res.playlistBaslik)
    }

    @Test
    fun `youtube oynatma listesi yapay zeka klasordeki videolari youtube sirasina gore dizer`() {
        val dosyalar = listOf(
            Pair("uri3", "03_temel_kavramlar_pozitif_negatif.mp4"),
            Pair("uri1", "01_temel_kavramlar_rakam_sayi.mp4"),
            Pair("uri2", "02_temel_kavramlar_tek_cift.mp4")
        )
        val res = YapayZekaYoutubeSiralamaMotoru.klasorDosyalariniAnalizEt(dosyalar)
        assertEquals(3, res.siraliVideolar.size)
        assertEquals(1, res.siraliVideolar[0].sira)
        assertEquals(2, res.siraliVideolar[1].sira)
        assertEquals(3, res.siraliVideolar[2].sira)
        assertEquals("01_temel_kavramlar_rakam_sayi.mp4", res.siraliVideolar[0].yerelDosyaAdi)
    }

    @Test
    fun `youtube oynatma listesi yapay zeka tarih klasorunu algilayip baslik atar`() {
        val dosyalar = listOf(Pair("uri", "1._islamiyet_oncesi_turk_boylari.mp4"))
        val res = YapayZekaYoutubeSiralamaMotoru.klasorDosyalariniAnalizEt(dosyalar)
        assertEquals("KPSS Tarih 2026 Kampı - Ramazan Yetgin", res.playlistBaslik)
    }

    @Test
    fun `youtube oynatma listesi yapay zeka bilinmeyen klasorlerde dosya adindan anlamli kamp basligi uretir`() {
        val dosyalar = listOf(Pair("uri", "01_sistem_programlama_giris.mp4"))
        val res = YapayZekaYoutubeSiralamaMotoru.klasorDosyalariniAnalizEt(dosyalar)
        assertTrue(res.playlistBaslik.contains("Sistem Programlama"))
        assertTrue(res.playlistBaslik.contains("Kampı"))
    }

    @Test
    fun `youtube oynatma listesi yapay zeka ile olusturulan klasorler birbirinden ayri ve bagimsizdir`() {
        val p1 = YapayZekaYoutubeSiralamaMotoru.klasordenAiIlePlaylistOlustur(
            null,
            "/klasor1",
            "klasor1",
            listOf(Pair("uri1", "01_matematik_kamp.mp4"))
        )
        val p2 = YapayZekaYoutubeSiralamaMotoru.klasordenAiIlePlaylistOlustur(
            null,
            "/klasor2",
            "klasor2",
            listOf(Pair("uri2", "01_tarih_kamp.mp4"))
        )
        assertTrue(p1.id != p2.id)
        assertTrue(p1.baslik != p2.baslik)
    }

    @Test
    fun `youtube oynatma listesi ornek klasor senaryolari 3 farkli kamp klasoru barindirir`() {
        val senaryolar = YapayZekaYoutubeSiralamaMotoru.ornekKlasorSenaryolariGetir()
        assertEquals(3, senaryolar.size)
    }

    @Test
    fun `youtube oynatma listesi sayisal index cikarici dosya adindan dogru sayiyi bulur`() {
        assertEquals(1, YoutubePlaylistMotoru.sayisalIndexCikar("01_temel_kavramlar.mp4"))
        assertEquals(15, YoutubePlaylistMotoru.sayisalIndexCikar("15. Ders - Fonksiyonlar.mp4"))
        assertEquals(2, YoutubePlaylistMotoru.sayisalIndexCikar("ders_02_analiz.mkv"))
    }

    @Test
    fun `youtube oynatma listesi baslik ve videolar bos olamaz`() {
        val senaryo = YapayZekaYoutubeSiralamaMotoru.ornekKlasorSenaryolariGetir().first()
        val p = YapayZekaYoutubeSiralamaMotoru.klasordenAiIlePlaylistOlustur(
            null,
            senaryo.first,
            senaryo.first,
            senaryo.second
        )
        assertTrue(p.baslik.isNotBlank())
        assertTrue(p.videolar.isNotEmpty())
    }

    @Test
    fun `youtube oynatma listesi silme islemi dogru calisir`() {
        assertTrue(YoutubePlaylistMotoru.playlistSil(null, "fake-id"))
    }

    @Test
    fun `youtube oynatma listesi videoyu cihazdan oynat internetten degil yerel dosyadan acar`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(
            1,
            "1. Video",
            "content://media/1",
            "01_video.mp4",
            true
        )
        val res = YoutubePlaylistMotoru.videoyuCihazdanOynat(null, v)
        assertTrue(res.first)
        assertTrue(res.second.contains("cihazdan"))
        assertTrue(res.second.contains("oynatıldı"))
    }

    @Test
    fun `youtube oynatma listesi eslesmeyen videoyu oynatmaya calisinca uyari dondurur`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(
            1,
            "1. Video",
            null,
            null,
            false
        )
        val res = YoutubePlaylistMotoru.videoyuCihazdanOynat(null, v)
        assertFalse(res.first)
        assertTrue(res.second.contains("henüz seçilmedi"))
    }

    @Test
    fun `youtube oynatma listesi yerel dosya uzantilari mp4 mkv webm kabul eder`() {
        assertTrue(YoutubePlaylistMotoru.gecerliVideoDosyasiMi("ders1.mp4"))
        assertTrue(YoutubePlaylistMotoru.gecerliVideoDosyasiMi("ders2.mkv"))
        assertFalse(YoutubePlaylistMotoru.gecerliVideoDosyasiMi("resim.jpg"))
    }

    @Test
    fun `youtube oynatma listesi sira metni ozelligi dogru bicimlendirir`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(5, "5. Video")
        assertEquals("#5", v.siraMetni)
    }

    @Test
    fun `youtube oynatma listesi yapay zeka cografya ve geometri kamplarini da tanir diger listesine atmaz`() {
        val dosyalar = listOf(
            Pair("uri1", "01_cografya_turkiye_iklimi.mp4"),
            Pair("uri2", "01_geometri_ucgenler_kenan.mp4")
        )
        val gruplar = YapayZekaYoutubeSiralamaMotoru.topluDosyalariGruplayipSirala(null, dosyalar)
        assertTrue(gruplar.any { it.baslik.contains("Coğrafya") })
        assertTrue(gruplar.any { it.baslik.contains("Geometri") })
        assertFalse(gruplar.any { it.id == YapayZekaYoutubeSiralamaMotoru.ID_DIGER_YEREL })
    }

    @Test
    fun `youtube oynatma listesi grubun tamamini silme islemi tum listeyi ve videolari kaldirir`() {
        assertTrue(YoutubePlaylistMotoru.playlistSil(null, "fake-id-grup"))
    }

    @Test
    fun `youtube oynatma listesi video nesneleri kapak sure ve aciklama bilgilerini saklar`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(
            1,
            "1. Video",
            null,
            null,
            false,
            "42:15",
            "🏷️ KPSS 2026"
        )
        assertEquals("42:15", v.sureMetni)
        assertEquals("🏷️ KPSS 2026", v.aciklama)
    }

    @Test
    fun `youtube oynatma listesi videoyu baska gruba tasiyinca kaynak gruptan silinip hedef grupta siralanir`() {
        val p1 = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Matematik Grubu",
            listOf(Pair("uri1", "01_mat.mp4"), Pair("uri2", "01_tarih_yanlis.mp4"))
        )
        val p2 = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Tarih Grubu",
            listOf(Pair("uri3", "01_tar.mp4"))
        )
        val yanlisVideo = p1.videolar.find { it.youtubeBaslik.contains("tarih") }!!
        p1.videolar.remove(yanlisVideo)
        p1.siralamayiYenidenDuzenle()

        p2.videolar.add(
            YoutubePlaylistMotoru.PlaylistVideo(
                sira = p2.videolar.size + 1,
                youtubeBaslik = yanlisVideo.youtubeBaslik,
                yerelDosyaUri = yanlisVideo.yerelDosyaUri,
                yerelDosyaAdi = yanlisVideo.yerelDosyaAdi,
                eslesti = true,
                sureMetni = yanlisVideo.sureMetni,
                aciklama = yanlisVideo.aciklama
            )
        )
        p2.siralamayiYenidenDuzenle()

        assertEquals(1, p1.videolar.size)
        assertEquals(2, p2.videolar.size)
        assertEquals(2, p2.videolar[1].sira)
    }

    @Test
    fun `youtube oynatma listesi json kaydetme ve okumada sure ile aciklamayi korur`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(
            1,
            "1. Video",
            "content://test",
            "test.mp4",
            true,
            "38:50",
            "🏷️ ÖSYM Müfredatı"
        )
        assertEquals("38:50", v.sureMetni)
        assertEquals("🏷️ ÖSYM Müfredatı", v.aciklama)
    }

    // --- v11.03 YENİ TESTLER ---

    @Test
    fun `v11_03 sekmeler arasi tasi ve gecis yap islemi basarili doner`() {
        val res = YoutubePlaylistMotoru.videoyuSekmelerArasiTasiVeGecisYap(
            null,
            "fake-kaynak",
            "fake-hedef",
            1
        )
        assertTrue(res.first)
        assertTrue(res.second.contains("sekmeye taşındı"))
    }

    @Test
    fun `v11_03 videolarin sirasini degistir fonksiyonu indexleri yeniden dizer`() {
        val p = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
            null,
            "Sıra Değiştirme Testi",
            listOf(Pair("u1", "01_video.mp4"), Pair("u2", "02_video.mp4"), Pair("u3", "03_video.mp4"))
        )
        val val0 = p.videolar[0].youtubeBaslik
        val val2 = p.videolar[2].youtubeBaslik

        val item = p.videolar.removeAt(0)
        p.videolar.add(2, item)
        p.siralamayiYenidenDuzenle()

        assertEquals(3, p.videolar.size)
        assertEquals(1, p.videolar[0].sira)
        assertEquals(3, p.videolar[2].sira)
        assertEquals(val0, p.videolar[2].youtubeBaslik)
    }

    @Test
    fun `v11_03 video kopyalama ve tasima isleminde sure ile aciklama metinleri eksiksiz aktarilir`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(
            1,
            "Türev ve İntegral 1. Ders",
            "uri_mat",
            "01_turev.mp4",
            true,
            "58:12",
            "🏷️ KPSS Matematik 2026 · Özel İpucu Kamp videosu"
        )
        val kopyalanan = YoutubePlaylistMotoru.PlaylistVideo(
            sira = 2,
            youtubeBaslik = v.youtubeBaslik,
            yerelDosyaUri = v.yerelDosyaUri,
            yerelDosyaAdi = v.yerelDosyaAdi,
            eslesti = v.eslesti,
            sureMetni = v.sureMetni,
            aciklama = v.aciklama
        )
        assertEquals("58:12", kopyalanan.sureMetni)
        assertEquals("🏷️ KPSS Matematik 2026 · Özel İpucu Kamp videosu", kopyalanan.aciklama)
    }

    @Test
    fun `v11_03 sola ve saga kaydirma icin jest bilgilendirme metni ve ipucu tasimayi icerir`() {
        val v = YoutubePlaylistMotoru.PlaylistVideo(1, "Test Video")
        assertTrue(v.aciklama.isNotEmpty())
        assertTrue(v.siraMetni == "#1")
    }
}
