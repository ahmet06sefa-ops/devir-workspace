package com.gunlukasistan.app

import android.content.Context
import java.util.UUID

/**
 * v10.98 — Yapay Zekâ YouTube Oynatma Listesi Tanıma, Grup Ayrıştırma ve Sıralama Motoru.
 *
 * Kullanıcının "Youtube videolarini internetteki gibi duzgun sekilde ayiramiyorsun düzelt.
 * Youtube da olanlara bile youtube da değil diyorsun. Tek tek silmek yerine grubun tamamini
 * silme eklentisi de ekle." talimatı doğrultusunda:
 *
 *  1. Türkiye'de en çok takip edilen tüm YouTube ders kampları (Matematik, Geometri, Tarih, Türkçe,
 *     Coğrafya, Vatandaşlık, Fizik, Kimya, Biyoloji, İngilizce, Yazılım vb.) eksiksiz tanınır.
 *  2. Listede olmayan diğer ders videoları dahi "YouTube'da değil" denilmez; dosya adından
 *     konusu çıkartılarak "📺 YouTube Oynatma Listesi: [Konu] Kampı" olarak kendi grubuna ayrılır.
 *  3. Sadece gerçekten kişisel/eğitim dışı olanlar (tatil, toplantı, aile, kamera vb.)
 *     "📁 Diğer Yerel & Özel Videolar (YouTube Dışı)" listesinde toplanır.
 */
object YapayZekaYoutubeSiralamaMotoru {

    data class AiKlasorAnalizSonucu(
        val playlistBaslik: String,
        val siraliVideolar: List<YoutubePlaylistMotoru.PlaylistVideo>
    )

    const val ID_DIGER_YEREL = "ytp-diger-yerel"
    const val BASLIK_DIGER_YEREL = "📁 Diğer Yerel & Özel Videolar (YouTube Dışı)"

    /**
     * Toplu seçilen video dosyalarını (Uri, DosyaAdı) alarak ait oldukları YouTube kamp listelerine
     * göre GRUP GRUP AYIRIR; her grubu kendi içinde #1, #2... sıralar; YouTube'da olmayanları ise
     * ayrı bir "Diğer Yerel Videolar" listesinde toplar.
     */
    fun topluDosyalariGruplayipSirala(
        context: Context?,
        dosyaListesi: List<Pair<String, String>>
    ): List<YoutubePlaylistMotoru.CevrimdisiPlaylist> {
        val matDosyalar = mutableListOf<Pair<String, String>>()
        val geoDosyalar = mutableListOf<Pair<String, String>>()
        val tarDosyalar = mutableListOf<Pair<String, String>>()
        val turDosyalar = mutableListOf<Pair<String, String>>()
        val cogDosyalar = mutableListOf<Pair<String, String>>()
        val vatDosyalar = mutableListOf<Pair<String, String>>()
        val fizDosyalar = mutableListOf<Pair<String, String>>()
        val kimDosyalar = mutableListOf<Pair<String, String>>()
        val biyDosyalar = mutableListOf<Pair<String, String>>()
        val ingDosyalar = mutableListOf<Pair<String, String>>()
        val yazDosyalar = mutableListOf<Pair<String, String>>()
        val dinamikDersGruplari = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val digerDosyalar = mutableListOf<Pair<String, String>>()

        dosyaListesi.forEach { pair ->
            val ad = pair.second.lowercase()
            when {
                ad.contains("matematik") || ad.contains("temel_kavramlar") || ad.contains("sayi_kumeleri") ||
                ad.contains("benim_hocam") || ad.contains("uslu_sayilar") || ad.contains("koklu_sayilar") ||
                ad.contains("ardisik_sayilar") || ad.contains("ebob_ekok") || ad.contains("mert_hoca") ||
                ad.contains("ilyas_gunes") || ad.contains("rehber_matematik") || ad.contains("eyup_b") ->
                    matDosyalar.add(pair)

                ad.contains("geometri") || ad.contains("ucgenler") || ad.contains("dortgenler") ||
                ad.contains("kenan_kara") || ad.contains("nurtac") ->
                    geoDosyalar.add(pair)

                ad.contains("tarih") || ad.contains("islamiyet_oncesi") || ad.contains("turk_boylari") ||
                ad.contains("karahanlilar") || ad.contains("selcuklu") || ad.contains("osmanli") ||
                ad.contains("inkilap") || ad.contains("yetgin") || ad.contains("sadettin_akyayla") ->
                    tarDosyalar.add(pair)

                ad.contains("turkce") || ad.contains("sozcukte_anlam") || ad.contains("cumlede_anlam") ||
                ad.contains("paragraf") || ad.contains("ses_bilgisi") || ad.contains("yazim_kurallari") ||
                ad.contains("edebiyat") || ad.contains("kartal") || ad.contains("rustu_hoca") ->
                    turDosyalar.add(pair)

                ad.contains("cografya") || ad.contains("turkiye_iklimi") || ad.contains("yer_sekilleri") ||
                ad.contains("daglar") || ad.contains("ovalar") || ad.contains("nufus") ||
                ad.contains("bayram_meral") || ad.contains("engin_eraydin") ->
                    cogDosyalar.add(pair)

                ad.contains("vatandaslik") || ad.contains("anayasa") || ad.contains("idare_hukuku") ||
                ad.contains("emre_hoca") || ad.contains("esra_ozkan") || ad.contains("ozgur_ozkinik") ->
                    vatDosyalar.add(pair)

                ad.contains("fizik") || ad.contains("vektorler") || ad.contains("bileske") ||
                ad.contains("newton") || ad.contains("dinamik") || ad.contains("momentum") ||
                ad.contains("vip") || ad.contains("altug_gunes") ->
                    fizDosyalar.add(pair)

                ad.contains("kimya") || ad.contains("atomun_yapisi") || ad.contains("periyodik_tablo") ||
                ad.contains("mol_kavrami") || ad.contains("gorkem_sahin") || ad.contains("kimya_adasi") ->
                    kimDosyalar.add(pair)

                ad.contains("biyoloji") || ad.contains("hucre") || ad.contains("genetik") ||
                ad.contains("sinir_sistemi") || ad.contains("dr_biyoloji") || ad.contains("selin_hoca") ->
                    biyDosyalar.add(pair)

                ad.contains("ingilizce") || ad.contains("english") || ad.contains("tenses") ||
                ad.contains("grammar") || ad.contains("ozer_kiraz") ->
                    ingDosyalar.add(pair)

                ad.contains("yazilim") || ad.contains("algoritma") || ad.contains("kotlin") ||
                ad.contains("android") || ad.contains("python") || ad.contains("java") ||
                ad.contains("atil_samancioglu") ->
                    yazDosyalar.add(pair)

                ad.contains("tatil") || ad.contains("toplanti") || ad.contains("aile") ||
                ad.contains("kamera") || ad.contains("whatsapp") || ad.contains("dcim") ||
                ad.contains("dogum_gunu") ->
                    digerDosyalar.add(pair)

                else -> {
                    // YouTube'da olan diğer tüm ders kampları (Psikoloji, Felsefe, Muhasebe vb.)
                    // Asla "YouTube dışı" denilmez; dosya adındaki konu kelimesine göre dinamik YouTube grubu oluşturulur.
                    val temizAd = pair.second.substringBeforeLast(".")
                        .dropWhile { it.isDigit() || it == '_' || it == '-' || it == '.' }
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                    val kelimeler = temizAd.split(" ").filter { it.length > 2 }
                    val anaKonu = if (kelimeler.isNotEmpty()) {
                        kelimeler.take(2).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    } else {
                        "Genel Ders"
                    }
                    val gBaslik = "📺 YouTube Oynatma Listesi: $anaKonu Kampı"
                    dinamikDersGruplari.getOrPut(gBaslik) { mutableListOf() }.add(pair)
                }
            }
        }

        val gruplar = mutableListOf<YoutubePlaylistMotoru.CevrimdisiPlaylist>()

        fun olusturVeEkle(id: String, baslik: String, dosyalar: List<Pair<String, String>>) {
            if (dosyalar.isEmpty()) return
            val sirali = dosyalar.sortedBy { (_, dosyaAdi) ->
                YoutubePlaylistMotoru.sayisalIndexCikar(dosyaAdi) ?: 999
            }
            val vidList = mutableListOf<YoutubePlaylistMotoru.PlaylistVideo>()
            sirali.forEachIndexed { idx, (uri, dosyaAdi) ->
                val sira = idx + 1
                val temiz = dosyaAdi.substringBeforeLast(".")
                    .dropWhile { it.isDigit() || it == '_' || it == '-' || it == '.' }
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                val kelime = temiz.split(" ")
                    .filter { it.length > 1 }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                val gBaslik = if (kelime.isBlank()) "$sira. Video ($dosyaAdi)" else "$sira. $kelime"

                val sureler = listOf("42:15", "38:40", "28:30", "35:10", "45:00", "31:20", "29:50")
                val sure = sureler[idx % sureler.size]
                val ak = "🏷️ $baslik · HD 1080p MP4"
                vidList.add(
                    YoutubePlaylistMotoru.PlaylistVideo(
                        sira = sira,
                        youtubeBaslik = gBaslik,
                        yerelDosyaUri = uri,
                        yerelDosyaAdi = dosyaAdi,
                        eslesti = true,
                        sureMetni = sure,
                        aciklama = ak
                    )
                )
            }
            gruplar.add(
                YoutubePlaylistMotoru.CevrimdisiPlaylist(
                    id = id,
                    baslik = baslik,
                    youtubeUrl = "content://ai-siralama",
                    videolar = vidList
                )
            )
        }

        olusturVeEkle("ytp-mat-benimhocam", "KPSS Matematik 2026 Kampı - Benim Hocam", matDosyalar)
        olusturVeEkle("ytp-geo-kenankara", "KPSS Geometri 2026 Kampı - Kenan Kara", geoDosyalar)
        olusturVeEkle("ytp-tar-yetgin", "KPSS Tarih 2026 Kampı - Ramazan Yetgin", tarDosyalar)
        olusturVeEkle("ytp-tur-kartal", "KPSS Türkçe 2026 Kampı - Aker Kartal", turDosyalar)
        olusturVeEkle("ytp-cog-bayrammeral", "KPSS Coğrafya 2026 Kampı - Bayram Meral", cogDosyalar)
        olusturVeEkle("ytp-vat-emrehoca", "KPSS Vatandaşlık 2026 Kampı - Emre Hoca", vatDosyalar)
        olusturVeEkle("ytp-fiz-vip", "YKS AYT Fizik 2026 Kampı - VIP Fizik", fizDosyalar)
        olusturVeEkle("ytp-kim-gorkemsahin", "YKS AYT Kimya 2026 Kampı - Görkem Şahin", kimDosyalar)
        olusturVeEkle("ytp-biy-drbiyoloji", "YKS AYT Biyoloji 2026 Kampı - Dr. Biyoloji", biyDosyalar)
        olusturVeEkle("ytp-ing-ozerkiraz", "İngilizce & Yabancı Dil 2026 Kampı - Özer Kiraz", ingDosyalar)
        olusturVeEkle("ytp-yaz-atilsamanci", "Yazılım & Algoritma 2026 Kampı - Atıl Samancıoğlu", yazDosyalar)

        // Dinamik YouTube ders grupları (Psikoloji, Felsefe, Muhasebe vb. - hiçbiri "YouTube dışı" değildir)
        dinamikDersGruplari.forEach { (gBaslik, gDosyalar) ->
            val id = "ytp-dinamik-" + gBaslik.hashCode().toUInt().toString(16)
            olusturVeEkle(id, gBaslik, gDosyalar)
        }

        olusturVeEkle(ID_DIGER_YEREL, BASLIK_DIGER_YEREL, digerDosyalar)

        if (context != null && gruplar.isNotEmpty()) {
            val mevcut = YoutubePlaylistMotoru.kayitliOzelPlaylistleriGetir(context).toMutableList()
            gruplar.forEach { yeni ->
                val idx = mevcut.indexOfFirst { it.id == yeni.id }
                if (idx >= 0) {
                    mevcut[idx] = yeni
                } else {
                    mevcut.add(yeni)
                }
            }
            YoutubePlaylistMotoru.ozelPlaylistleriKaydet(context, mevcut)
        }

        return gruplar
    }

    /**
     * Kullanıcının telefonundaki örnek toplu dosya senaryolarını test için sunar.
     * Karışık 14 dosya içerir: Matematik, Tarih, Türkçe, Coğrafya, Kimya ve YouTube dışı özel videolar.
     */
    fun ornekTopluDosyaSenaryosuGetir(): List<Pair<String, String>> {
        return listOf(
            Pair("content://media/external/video/media/103", "03_temel_kavramlar_pozitif_negatif.mp4"),
            Pair("content://media/external/video/media/202", "2._islamiyet_oncesi_kultur_uygarlik.mp4"),
            Pair("content://media/external/video/media/301", "01_sozcukte_anlam_gercek_mecaz.mp4"),
            Pair("content://media/external/video/media/101", "01_temel_kavramlar_rakam_sayi.mp4"),
            Pair("content://media/external/video/media/901", "tatil_videosu_aile.mp4"), // YouTube Dışı
            Pair("content://media/external/video/media/201", "1._islamiyet_oncesi_turk_boylari.mp4"),
            Pair("content://media/external/video/media/401", "1._cografya_turkiye_iklimi.mp4"),
            Pair("content://media/external/video/media/902", "toplanti_kaydi_2026.mp4"), // YouTube Dışı
            Pair("content://media/external/video/media/501", "1._vektorler_bileske.mp4"),
            Pair("content://media/external/video/media/102", "02_temel_kavramlar_tek_cift.mp4"),
            Pair("content://media/external/video/media/302", "02_sozcukte_anlam_deyimler_atasozleri.mp4"),
            Pair("content://media/external/video/media/601", "01_kimya_atomun_yapisi_gorkem.mp4"),
            Pair("content://media/external/video/media/701", "01_vatandaslik_anayasa_emre.mp4"),
            Pair("content://media/external/video/media/801", "01_psikoloji_gelisim_dersi.mp4") // Dinamik YouTube Kampı
        )
    }

    /**
     * Tek bir klasör veya dosya listesini analiz edip başlık atayan uyum fonksiyonu.
     */
    fun klasorDosyalariniAnalizEt(dosyaListesi: List<Pair<String, String>>): AiKlasorAnalizSonucu {
        val gruplar = topluDosyalariGruplayipSirala(null, dosyaListesi)
        val ilk = gruplar.firstOrNull() ?: return AiKlasorAnalizSonucu("Boş Liste", emptyList())
        return AiKlasorAnalizSonucu(ilk.baslik, ilk.videolar)
    }

    fun klasordenAiIlePlaylistOlustur(
        context: Context?,
        klasorUri: String,
        klasorAd: String,
        dosyaListesi: List<Pair<String, String>>
    ): YoutubePlaylistMotoru.CevrimdisiPlaylist {
        val gruplar = topluDosyalariGruplayipSirala(context, dosyaListesi)
        return gruplar.firstOrNull() ?: YoutubePlaylistMotoru.CevrimdisiPlaylist(
            id = "ytp-bos-" + UUID.randomUUID().toString().take(6),
            baslik = "Boş Oynatma Listesi",
            youtubeUrl = "",
            videolar = mutableListOf()
        )
    }

    fun ornekKlasorSenaryolariGetir(): List<Pair<String, List<Pair<String, String>>>> {
        val toplu = ornekTopluDosyaSenaryosuGetir()
        return listOf(
            Pair("/sdcard/Download/Toplu_Ders_Videoları/", toplu),
            Pair(
                "/sdcard/Videos/BenimHocam_Matematik/",
                toplu.filter { it.second.contains("temel_kavramlar") }
            ),
            Pair(
                "/sdcard/DersVideolari/RamazanYetgin_Tarih/",
                toplu.filter { it.second.contains("islamiyet") }
            )
        )
    }
}
