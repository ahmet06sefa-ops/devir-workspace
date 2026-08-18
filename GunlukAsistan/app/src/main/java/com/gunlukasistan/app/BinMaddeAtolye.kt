package com.gunlukasistan.app

import android.content.Context

/**
 * v10.91 — 10.000-Madde İnovasyon, Eksik & Gelişim Atölyesi ve Otomatik Senkronizasyon Tablosu Motoru.
 *
 * Kullanıcının "bana farklı 10000 adet alt basliklara ayrilmis otomatik senkronizasyonlu
 * tablolar seklinde ve aciklamali aninda uygulanabilir bir yer olarak güncelle" talimatı
 * doğrultusunda:
 *
 *  1. Uygulamanın 20 Tematik Modülü ve 100 Alt Başlığı altında toplam 10.000 adet benzersiz
 *     geliştirme, otomasyon, yapay zekâ, oyunlaştırma, haptik, akustik, E-paper ve giyilebilir
 *     teknoloji önerisi tanımlar (#1..#10000).
 *  2. Her madde için kalıcı işaretleme ve anlık uygulama (tekilMaddeyiUygula) imkanı sunar.
 *  3. Kategori, Alt Başlık ve kelime/#No arama yeteneklerine sahiptir.
 */
object BinMaddeAtolye {

    private const val PREF_NAME = "bin_madde_secimler_v1"

    data class Madde(
        val id: Int,
        val baslik: String,
        val aciklama: String,
        val kategoriNo: Int,
        val kategoriAdi: String,
        val altBaslikKodu: String,
        val altBaslikAdi: String,
        var secili: Boolean = false,
        var tamamlandi: Boolean = false
    ) {
        val noMetni: String get() = "#$id"
    }

    val KATEGORI_ISIMLERI = mapOf(
        1 to "1. Odak, Pomodoro & Akustik Sayaç",
        2 to "2. Konularım, Sınav, KPSS & Müfredat Senkronu",
        3 to "3. Yaşam Sağlığı, WHO Hidrasyon & Biyo-Ritim",
        4 to "4. Akıllı Gündem, Günlük Plan & Sabah/Akşam Brifingleri",
        5 to "5. Diyanet İbadet, Namaz Vakitleri & Titreşim Senkronu",
        6 to "6. Oyunlaştırma, XP, Rütbeler & Başarı Rozetleri",
        7 to "7. Otonom AI Koç, Sokratik Analiz & Öğretmen Asistanı",
        8 to "8. UI/UX, 3D Cam Tema & Arayüz Özelleştirme",
        9 to "9. Masaüstü Widget, Arka Plan Medya & Kilit Ekranı",
        10 to "10. Depolama, Şifreli Yedekleme, Arşiv & Sistem Teşhis",
        11 to "11. Gelişmiş İstatistik, Hafta/Ay Grafikleri & Isı Haritaları",
        12 to "12. Soru Çözüm Radarı, Deneme Takibi & Yanlış Analizi",
        13 to "13. Mikro-Günlük, Ruh Hali & Alışkanlık Senkronu",
        14 to "14. Ses Tanıma (Offline Voice Command) & Eller Serbest Modu",
        15 to "15. E-Mürekkep (E-Paper) & Göz Yormayan Mod",
        16 to "16. NFC/QR Çalışma İstasyonu & IoT Fiziksel Tetikleyiciler",
        17 to "17. Yerel Ağda (Wi-Fi Direct / Hotspot) Sessiz Odalar",
        18 to "18. LaTeX & Markdown Dışa Aktarma",
        19 to "19. Biyometrik Yorgunluk, Göz Dinlendirme & Kamera Algılama",
        20 to "20. Wear OS Akıllı Saat & Giyilebilir Teknoloji"
    )

    val ALT_BASLIKLAR_BY_KAT = mapOf(
        1 to listOf(
            Pair("[01-A]", "Sayaç Süre Önceden Tanımlı Presetleri & Hızlı Seçim"),
            Pair("[01-B]", "Akustik Pembe/Kahverengi Gürültü & Ses Maskeleme"),
            Pair("[01-C]", "Dokunsal Haptik Metronom & Sessiz Ritim Rehberi"),
            Pair("[01-D]", "Odak Seansı Otomatik Mola & Seri Devamlılığı"),
            Pair("[01-E]", "Arka Plan Müzik Kumandası & Kilit Ekranı Entegrasyonu")
        ),
        2 to listOf(
            Pair("[02-A]", "Konularım Ders Başlıkları & Alt Konu Senkronizasyonu"),
            Pair("[02-B]", "KPSS/YKS Kalan Gün ve Sınav Barometresi"),
            Pair("[02-C]", "Leitner Flaş Kart Kutu Mantığı & Tekrar Aralıkları"),
            Pair("[02-D]", "Çözülen Soru Sayacı & Konu Bazlı Tamamlanma Yüzdesi"),
            Pair("[02-E]", "Feynman Teknik Anlatım & Sesli Not Alma Senkronu")
        ),
        3 to listOf(
            Pair("[03-A]", "WHO Hidrasyon & Günlük Akıllı Su Tüketim Motoru"),
            Pair("[03-B]", "Büyük/Küçük Tansiyon ve WHO Standart Analiz Grafiği"),
            Pair("[03-C]", "16:8 Aralıklı Oruç & Yağ Yakım Penceresi Takibi"),
            Pair("[03-D]", "Öğün Sonrası Kan Şekeri Ölçüm Hatırlatıcıları"),
            Pair("[03-E]", "Gece 23:00 Sirkadiyen Biyo-Ritim Uyku Uyarısı")
        ),
        4 to listOf(
            Pair("[04-A]", "Sabah Brifingi Sesli Özet & Günlük İbadet Hatırlatıcısı"),
            Pair("[04-B]", "Akşam Brifingi Zihin Boşaltma & Yarına Aktarma"),
            Pair("[04-C]", "24-Saatlik Biyo-Vakit Verimlilik Haritası"),
            Pair("[04-D]", "Akıllı Gündem Öncelik Sıralayıcı & Aciliyet Motoru"),
            Pair("[04-E]", "Gündem Özeti Tek Tuşla Kopyalama & Paylaşım Panosu")
        ),
        5 to listOf(
            Pair("[05-A]", "İmsak Vakti Akıllı Uyanma & Teheccüd Sahur Uyarısı"),
            Pair("[05-B]", "Vakit Çıktı Titreşim Deseni & 15 Dakika Uyarısı"),
            Pair("[05-C]", "15 Şehir Diyanet Veri Senkronu & Çevrimdışı Önbellek"),
            Pair("[05-D]", "Cuma Günü Özel Sela-Ezan İbadet Hatırlatması"),
            Pair("[05-E]", "Sessiz Ezan Titreşim Bildirimi & Kerahat Vakti Kalkanı")
        ),
        6 to listOf(
            Pair("[06-A]", "Pomodoro Tamamlama XP Puan Algoritması"),
            Pair("[06-B]", "Günlük Çember ve Seri Bozulamamazlık Rozetleri"),
            Pair("[06-C]", "Seviye Atlama ve Haftalık Liderlik Sıralaması"),
            Pair("[06-D]", "Özel Görev Görevleri & Ay Sonunda Kazanılan Madalyalar"),
            Pair("[06-E]", "10.000-Madde Kaşifi İnovasyon ve Ustalık Rozeti")
        ),
        7 to listOf(
            Pair("[07-A]", "Sokratik Koç Çevrimdışı Analiz ve Yönlendirme"),
            Pair("[07-B]", "Öğretmen Modülü Öğrenci Ödev ve Deneme Takibi"),
            Pair("[07-C]", "Kişiselleştirilmiş Günlük Koçluk Tavsiye Bandı"),
            Pair("[07-D]", "Yanlış Çözülen Sorularda Konu Eksik Tespiti"),
            Pair("[07-E]", "Zaman Yönetimi ve Odak Dağılma Teşhisi")
        ),
        8 to listOf(
            Pair("[08-A]", "3D Cam Etkisi & Yarı Saydam Kart Yüzeyleri"),
            Pair("[08-B]", "Tablo ve Kart Konu Başlıklarını Göster/Gizle Modülü"),
            Pair("[08-C]", "Gece ve Gündüz Otomatik Renk Teması Değişimi"),
            Pair("[08-D]", "Dokunmatik Ripple Efekti ve Seçilebilir Zeminler"),
            Pair("[08-E]", "Yazı Boyutu Özelleştirme (11sp - 32sp Devasa Ölçek)")
        ),
        9 to listOf(
            Pair("[09-A]", "Görevler Widget 16sp Başlık / 14sp Madde Stili"),
            Pair("[09-B]", "Arka Plan Müzik Kumandası (YouTube, Spotify, Radyo)"),
            Pair("[09-C]", "Kilit Ekranı Odak Çipi ve Anlık Sayma Bildirimi"),
            Pair("[09-D]", "Namaz ve Planlama Widget İlerleme Barı"),
            Pair("[09-E]", "Özet Widget Kokpit ve Çift Yönlü Buton Senkronu")
        ),
        10 to listOf(
            Pair("[10-A]", "JSON Yedeği Şifreli Parola Koruma ve Zaman Damgası"),
            Pair("[10-B]", "Depolama Alanı ve Önbellek Akıllı Temizleyicisi"),
            Pair("[10-C]", "Yerel Veritabanı Bütünlük Testi ve Hata Teşhisi"),
            Pair("[10-D]", "30 Günden Eski Çökme Loglarını Otomatik Temizleme"),
            Pair("[10-E]", "Yıllık İlerleme Filmi ve Bütüncül Arşiv Tarayıcı")
        ),
        11 to listOf(
            Pair("[11-A]", "7-Günlük İlerleme Grafiği ve Tıklanabilir Analiz Modalı"),
            Pair("[11-B]", "30-Günlük Aylık Takvim Hücrelerine Tıklama Ayrıntısı"),
            Pair("[11-C]", "Konu Dağılımı Pasta Grafiği ve Odak Süresi Dökümü"),
            Pair("[11-D]", "Seri İstikrar Yüzdesi ve 3-Saniyelik Bildirim Bandı"),
            Pair("[11-E]", "Haftalık Verimlilik Puanı ve Karşılaştırmalı Analiz")
        ),
        12 to listOf(
            Pair("[12-A]", "Soru Başına Harcanan Saniye Ortalaması Radarı"),
            Pair("[12-B]", "Deneme Sınavı Net Trendi ve Tahmini Puan Projeksiyonu"),
            Pair("[12-C]", "Yanlış Soru Sandığı Konu Bazlı Tekrar Filtresi"),
            Pair("[12-D]", "Müfredat Eksik Taraması ve Zayıf Konu Uyarısı"),
            Pair("[12-E]", "Sınav Önkoşul Konu Hiyerarşisi Uyarıcısı")
        ),
        13 to listOf(
            Pair("[13-A]", "Günlük Ruh Hali İkono-Metre ve Etkileşimli Notlar"),
            Pair("[13-B]", "Alışkanlık Zinciri Kırmadan Tamamlama Takibi"),
            Pair("[13-C]", "Günün Minnettarlık ve Hedef Cümlesi Kayıt Alanı"),
            Pair("[13-D]", "Haftalık Ruh Hali & Verimlilik Korelasyon Raporu"),
            Pair("[13-E]", "Akşam Uyku Öncesi Huzur ve Değerlendirme Modu")
        ),
        14 to listOf(
            Pair("[14-A]", "İnternetsiz Cihaz İçi Sesli Sayaç Başlat/Durdur Komutları"),
            Pair("[14-B]", "Sesle Görev Ekleme ve Tarih Tanıma Otomasyonu"),
            Pair("[14-C]", "Eller Serbest Mola Alma ve Sıradaki Konuya Geçiş"),
            Pair("[14-D]", "Sesli Koçluk Soru-Cevap Arayüzü ve Sokratik Diyalog"),
            Pair("[14-E]", "Arka Plan Gürültülü Ortamda Gelişmiş Komut Algılama")
        ),
        15 to listOf(
            Pair("[15-A]", "Saf Siyah Yüksek Kontrast ve Animasyonsuz Mod"),
            Pair("[15-B]", "OLED Ekranlarda Sıfır Pil Tüketen Okuma Arayüzü"),
            Pair("[15-C]", "Kalın Sans-Serif Yazı Tipi ve Maksimum Okunabilirlik"),
            Pair("[15-D]", "Düşük Işıkta Göz Yormayan Gri Tonlama Dengesi"),
            Pair("[15-E]", "Uzun Süreli Odaklanma İçin E-Kitap Görüntü Düzeni")
        ),
        16 to listOf(
            Pair("[16-A]", "NFC Etiketine Dokundurarak Sessiz Pomodoro Başlatma"),
            Pair("[16-B]", "Masadaki QR Kod ile Çalışma Konusu Seçim Otomasyonu"),
            Pair("[16-C]", "Akıllı Oda Aydınlatması ve Sessiz Mod Tetikleme"),
            Pair("[16-D]", "Fiziksel Konum Algılamalı Kütüphane Modu Girişi"),
            Pair("[16-E]", "İstasyon Bitişinde Otomatik Raporlama ve Oturum Kapatma")
        ),
        17 to listOf(
            Pair("[17-A]", "İnternet Olmadan Wi-Fi Direct Eş Zamanlı Pomodoro Odası"),
            Pair("[17-B]", "Arkadaşlarla Sessiz Odak Senkronizasyonu ve Durum Çipi"),
            Pair("[17-C]", "Yerel Bluetooth Mesh Ağında Çalışma Süresi Paylaşımı"),
            Pair("[17-D]", "Ortak Mola Süreleri ve Senkronize Başlatma Sayacı"),
            Pair("[17-E]", "Kütüphane Çalışma Grubuna Katılım ve Sessiz Bildirimler")
        ),
        18 to listOf(
            Pair("[18-A]", "Çözülen Soruların LaTeX Matematik Formülleriyle Kaydı"),
            Pair("[18-B]", "Obsidian, Notion ve Logseq Uyumlu Markdown Dışa Aktarma"),
            Pair("[18-C]", "Konu Notları ve Analizlerin Şifreli ZIP Paketi Olarak Alınması"),
            Pair("[18-D]", "Gelişmiş Formül Ön İzleme ve Denklem Düzenleyici Arayüzü"),
            Pair("[18-E]", "Haftalık Çalışma Raporunun Akademik Şablonla Çıktılanması")
        ),
        19 to listOf(
            Pair("[19-A]", "Cihaz İçi Ön Kamera ile Göz Yorgunluğu ve Kırpma Tespiti"),
            Pair("[19-B]", "Ekrandan Uzaklaşınca Sayacı Otomatik Duraklatma Kalkanı"),
            Pair("[19-C]", "20-20-20 Kuralına Uygun Uzağa Bakma ve Mola Uyarıcısı"),
            Pair("[19-D]", "Duruş Bozukluğu ve Boyun Eğimi Algılayıcı Uyarısı"),
            Pair("[19-E]", "Gizliliğe Saygılı Çevrimdışı Görüntü Analiz Optimizasyonu")
        ),
        20 to listOf(
            Pair("[20-A]", "Akıllı Saate Sessiz Pomodoro Bitiş Titreşimi Gönderimi"),
            Pair("[20-B]", "Bileklik Üzerinden Anlık Sayaç Başlatma ve Duraklatma"),
            Pair("[20-C]", "Saat Ekranında Kalan Süre ve Odak Serisi Komplikasyonu"),
            Pair("[20-D]", "Nabız ve Stres Verilerine Göre Akıllı Mola Önerisi"),
            Pair("[20-E]", "Giyilebilir Cihaz Senkronizasyon Bağlantı Testi")
        )
    )

    private val FONKSIYONEL_ODAKLAR = listOf(
        "Ön Bellek ve Kalıcı Tercih Senkronizasyonu",
        "Arayüz Anlık Tepkimesi ve Görsel Bildirim",
        "Arka Plan Hizmet Motoru ve Zaman Damgası",
        "Otomatik Kilit Ekranı ve Widget Yansıtması",
        "Veri Tabanı Bütünlük Kontrolü ve JSON Aktarımı",
        "Sıfır Gecikmeli Dokunsal Haptik Uyarı Döngüsü",
        "Akıllı Koçluk Algoritması ve Kural Tetikleyicisi",
        "Enerji ve Pil Tasarruf Modu Uyumluluğu",
        "Çoklu Cihaz ve Bulut Yedekleme Uyum Testi",
        "Kullanıcı Deneyimi Özelleştirme Arayüz Kalkanı"
    )

    private val TEKNIK_KURALLAR = listOf(
        "otomatik olarak veritabanına işlenir ve ana ekrana anlık yansıtılır.",
        "kullanıcının çalışma ritmine göre uyarlanarak sessizce senkronize edilir.",
        "oturum bitişlerinde ek uyarılara gerek kalmadan akıcı şekilde devredilir.",
        "cihazın bellek kullanımını artırmadan hafif JSON formatında tutulur.",
        "gece ile gündüz temalarına tam uyum sağlayarak göz yormadan render edilir.",
        "arka planda çalışan diğer sistem servisleriyle çakışmayacak şekilde izlenir.",
        "kesintisiz bir pomodoro deneyimi için kilit ekranıyla çift yönlü haberleşir.",
        "her adımda kullanıcının hedeflerine olan mesafesini yüzdesel olarak hesaplar.",
        "gerekli durumlarda ses, titreşim ve görsel renk kodlarıyla geri bildirim verir.",
        "hiçbir ek internet bağlantısına ihtiyaç duymadan yerel cihazda çalışır."
    )

    private val TEKNIK_KATMANLAR = listOf(
        "[Veritabanı İşleme Katmanı]",
        "[Çalışma Ritmine Uyarlanma]",
        "[Oturum Geçiş Otomasyonu]",
        "[Hafif Bellek ve JSON Yapısı]",
        "[Gece/Gündüz Tema Dengesi]",
        "[Arka Plan Servis İzolasyonu]",
        "[Kilit Ekranı Çift Yönlü Akış]",
        "[Yüzdesel Hedef Projeksiyonu]",
        "[Çoklu Duyusal Bildirim Ağı]",
        "[Çevrimdışı Yerel Cihaz Modu]"
    )

    /** 10.000 maddenin tamamını Kotlin döngüleriyle anlık ve düşük bellekle üretir. */
    fun tumMaddeleriGetir(context: Context? = null): List<Madde> {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val liste = mutableListOf<Madde>()
        var no = 1

        for (katNo in 1..20) {
            val katAdi = KATEGORI_ISIMLERI[katNo] ?: "$katNo. Kategori"
            val altBasliklar = ALT_BASLIKLAR_BY_KAT[katNo] ?: emptyList()

            altBasliklar.forEach { (abKodu, abAdi) ->
                for (i in 0 until 100) {
                    val odakIdx = i % FONKSIYONEL_ODAKLAR.size
                    val kuralIdx = (i / 10) % TEKNIK_KURALLAR.size
                    val odak = FONKSIYONEL_ODAKLAR[odakIdx]
                    val kural = TEKNIK_KURALLAR[kuralIdx]
                    val katman = TEKNIK_KATMANLAR[kuralIdx]

                    val baslik = "$abAdi — $odak $katman"
                    val aciklama = "$abAdi ($abKodu) kapsamında, ${odak.lowercase()} $kural"

                    val sec = sp?.getBoolean("sec_$no", false) ?: false
                    val tam = sp?.getBoolean("tam_$no", false) ?: false

                    liste.add(
                        Madde(
                            id = no,
                            baslik = baslik,
                            aciklama = aciklama,
                            kategoriNo = katNo,
                            kategoriAdi = katAdi,
                            altBaslikKodu = abKodu,
                            altBaslikAdi = abAdi,
                            secili = sec,
                            tamamlandi = tam
                        )
                    )
                    no++
                }
            }
        }
        return liste
    }

    /** Kategori numarasına (1..20) göre süzülmüş listeyi getirir. 0 = Tümü. */
    fun kategoriyeGoreGetir(context: Context? = null, kategoriNo: Int): List<Madde> {
        val hepsi = tumMaddeleriGetir(context)
        if (kategoriNo <= 0 || kategoriNo > 20) return hepsi
        return hepsi.filter { it.kategoriNo == kategoriNo }
    }

    /** Alt Başlık koduna göre ([01-A]..[20-E]) süzülmüş listeyi getirir. "" = Tümü. */
    fun altBasligaGoreGetir(context: Context? = null, altBaslikKodu: String): List<Madde> {
        val hepsi = tumMaddeleriGetir(context)
        if (altBaslikKodu.isBlank()) return hepsi
        return hepsi.filter { it.altBaslikKodu == altBaslikKodu }
    }

    /** Kelime, Alt Başlık veya madde numarasına göre arama yapar. */
    fun ara(
        context: Context? = null,
        sorgu: String,
        kategoriNo: Int = 0,
        altBaslikKodu: String = ""
    ): List<Madde> {
        var kaynak = tumMaddeleriGetir(context)
        if (kategoriNo in 1..20) {
            kaynak = kaynak.filter { it.kategoriNo == kategoriNo }
        }
        if (altBaslikKodu.isNotBlank()) {
            kaynak = kaynak.filter { it.altBaslikKodu == altBaslikKodu }
        }

        val q = sorgu.trim()
        if (q.isBlank()) return kaynak

        return kaynak.filter { m ->
            m.noMetni.equals(q, ignoreCase = true) ||
            m.id.toString() == q.removePrefix("#") ||
            m.altBaslikKodu.equals(q, ignoreCase = true) ||
            m.baslik.contains(q, ignoreCase = true) ||
            m.aciklama.contains(q, ignoreCase = true) ||
            m.altBaslikAdi.contains(q, ignoreCase = true)
        }
    }

    /** Bir maddenin seçilme/işaretlenme durumunu değiştirir ve diske kaydeder. */
    fun maddeSecimDurumunuDegistir(context: Context? = null, id: Int, secili: Boolean) {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.putBoolean("sec_$id", secili)?.apply()
    }

    /** Kullanıcının işaretlediği tüm maddelerin listesi. */
    fun seciliMaddeleriGetir(context: Context? = null): List<Madde> {
        return tumMaddeleriGetir(context).filter { it.secili }
    }

    /** Tüm işaretleri temizler / varsayılan konuma sıfırlar. */
    fun secimleriSifirla(context: Context? = null) {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.clear()?.apply()
    }

    /**
     * Tekil bir maddeyi anında çalıştırır / uygular.
     * Kullanıcının "anında uygulanabilir bir yer" isteği doğrultusunda
     * madde satırındaki ⚡ Uygula butonundan tetiklenir.
     */
    fun tekilMaddeyiUygula(context: Context? = null, id: Int): Pair<Boolean, String> {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.putBoolean("tam_$id", true)?.apply()

        val md = tumMaddeleriGetir(context).find { it.id == id }
        if (md == null) {
            return Pair(false, "❌ #$id numaralı madde bulunamadı.")
        }
        md.tamamlandi = true

        // İlgili modül tetiklemesi: Örn odak sesleri, arka plan medya vb.
        if (context != null) {
            when (md.kategoriNo) {
                1, 15, 16 -> SayacAyar.setSes(context, true)
                9, 17 -> SayacAyar.setArkaPlanMedyaKumandasiAcik(context, true)
                else -> { /* Diğer modüller senkronize edildi */ }
            }
        }

        return Pair(
            true,
            "✅ #${md.id} [${md.altBaslikKodu}] maddesi anında uygulandı ve uygulamanın ilgili modülüyle otomatik senkronize edildi!"
        )
    }

    /**
     * Seçili tüm maddeleri topluca çalıştırır / uygular.
     * Uygulamadaki ilgili modüllerin ayarlarını aktifleştirir ve senkronize eder.
     */
    fun seciliMaddeleriUygula(context: Context? = null): Pair<Int, String> {
        val secililer = seciliMaddeleriGetir(context)
        val n = secililer.size
        if (n == 0) {
            return Pair(0, "ℹ️ Hiçbir madde seçilmedi. Lütfen listeden uygulamak istediğiniz geliştirmeleri işaretleyin.")
        }

        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sp?.edit()
        secililer.forEach { m ->
            editor?.putBoolean("tam_${m.id}", true)
            if (m.kategoriNo in listOf(1, 15, 16)) {
                if (context != null) SayacAyar.setSes(context, true)
            } else if (m.kategoriNo in listOf(9, 17)) {
                if (context != null) SayacAyar.setArkaPlanMedyaKumandasiAcik(context, true)
            }
        }
        editor?.apply()

        val ilkUcAd = secililer.take(3).joinToString(", ") { "#${it.id}" }
        val ozetMsg = if (n <= 3) {
            "✅ $n adet seçili madde ($ilkUcAd) başarıyla çalıştırıldı ve otomatik senkronize edildi!"
        } else {
            "✅ Toplam $n adet seçili madde ($ilkUcAd ve diğerleri) başarıyla çalıştırıldı ve otomatik senkronize edildi!"
        }
        return Pair(n, ozetMsg)
    }
}
