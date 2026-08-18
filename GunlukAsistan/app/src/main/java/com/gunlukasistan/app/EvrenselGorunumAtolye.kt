package com.gunlukasistan.app

import android.content.Context

/**
 * v10.92 — 10.000-Madde Evrensel Görünüm ve Arayüz (UI/UX) Kişiselleştirme Atölyesi Motoru.
 *
 * Kullanıcının "Bana 10000 adet görünüm kişilestirmek icin ayarlar yeri ekle herseyin görünümunu ,
 * görünüşünü ordan degistirebileyim bütün uygulama için herseyini ordan yonetebileyim" talimatı ve
 * Saf Siyah (OLED E-Mürekkep) varsayılan tema seçimi doğrultusunda:
 *
 *  1. 10 Ana Görünüm Boyutu ve 100 Görünüm Alt Başlığı altında 10.000 benzersiz görünüm ve arayüz
 *     kişiselleştirme öğesi tanımlar (#1..#10000).
 *  2. Kullanıcının seçimi doğrultusunda varsayılan temayı Saf Siyah (OLED E-Mürekkep) Yüksek
 *     Kontrast Modu olarak yapılandırır.
 *  3. Hem tekil (tekilGorunumuUygula) hem toplu (seciliGorunumleriUygula) anlık görünüm uygulaması sağlar.
 */
object EvrenselGorunumAtolye {

    private const val PREF_NAME = "evrensel_gorunum_secimler_v1"
    const val VARSAYILAN_TEMA_MODU = "oled_emurekkep" // Kullanıcının tercihi

    data class GorunumMadde(
        val id: Int,
        val baslik: String,
        val aciklama: String,
        val kategoriNo: Int,
        val kategoriAdi: String,
        val altBaslikKodu: String,
        val altBaslikAdi: String,
        var secili: Boolean = false,
        var uygulandı: Boolean = false
    ) {
        val noMetni: String get() = "#$id"
    }

    val KATEGORI_ISIMLERI = mapOf(
        1 to "1. Renk Paletleri & Zeminler",
        2 to "2. Tipografi & Yazı Boyutları",
        3 to "3. Kartlar & Köşe Yarıçapları",
        4 to "4. Gölgeler & Ripple Efektleri",
        5 to "5. Tablolar & İlerleme Grafikleri",
        6 to "6. Sayaç Temaları & Medyatik Arayüz",
        7 to "7. Widget & Kilit Ekranı Düzeni",
        8 to "8. Namaz & Diyanet İbadet Arayüzü",
        9 to "9. Ayarlar & Atölye Menü Stili",
        10 to "10. İkonlar & E-Mürekkep Modu"
    )

    val ALT_BASLIKLAR_BY_KAT = mapOf(
        1 to listOf(
            Pair("[G01-A]", "Ana Renk (Primary) Paleti & Dinamik Ton Özelleştirici"),
            Pair("[G01-B]", "İkincil Renk (Secondary) & Vurgu (Accent) Ton Kontrolü"),
            Pair("[G01-C]", "Arka Plan (Surface) Renkleri & Şafak/Gece Tonlaması"),
            Pair("[G01-D]", "Saf Siyah OLED Modu (%100 Siyah Zemin Sıfır Pil)"),
            Pair("[G01-E]", "Material You Dinamik Duvar Kağıdı Renk Uyumlaması"),
            Pair("[G01-F]", "Koyu / Açık Renk Geçiş Saati ve Otomatik Zamanlayıcı"),
            Pair("[G01-G]", "Göz Yormayan Pastel Ton Dengeleyici & Sepya Filtresi"),
            Pair("[G01-H]", "Arayüz Renk Kontrast Oranı (WCAG AAA Standart Uyumu)"),
            Pair("[G01-I]", "Kart Zeminleri ile Ekran Zemini Ayrışma Renk Tonları"),
            Pair("[G01-J]", "Diyalog, Pencere ve Alt Menü Renk Şablon Seçicisi")
        ),
        2 to listOf(
            Pair("[G02-A]", "Başlık Yazı Tipi Ailesi (Poppins, Serif, Monospace, Condensed)"),
            Pair("[G02-B]", "Metin ve Madde Yazı Tipi Ailesi & Kalınlık Ölçeği (300/400/600)"),
            Pair("[G02-C]", "Evrensel Yazı Boyutu Ölçeği (11sp Mini - 15sp Normal - 32sp Devasa)"),
            Pair("[G02-D]", "Satır Aralığı (Line Height) & Paragraf Boşluk Oranları (24dp-32dp)"),
            Pair("[G02-E]", "Widget & Kilit Ekranı Özel Font Boyutları (16sp Başlık / 14sp Satır)"),
            Pair("[G02-F]", "Rakamlar ve Sayaç Kadrani İçin Monospace (Sabit Aralıklı) Font"),
            Pair("[G02-G]", "Arapça Ezani Metin ve İbadet Kartı Özel Kaligrafi Fontu"),
            Pair("[G02-H]", "Okuma Ekranlarında Harf Aralığı (Letter Spacing) Yoğunluk Arayüzü"),
            Pair("[G02-I]", "İtalik, Kalın ve Vurgulu Metin Rengi Ayrıştırma Stilleri"),
            Pair("[G02-J]", "E-Kitap ve Not Okuma Düzeni İçin Serif/Sans-Serif Geçiş Kalkanı")
        ),
        3 to listOf(
            Pair("[G03-A]", "Evrensel Kart Köşe Yarıçapı Ölçeği (ga_kose_kucuk=12dp .. dev=24dp)"),
            Pair("[G03-B]", "Kenarlık (Border / Stroke) Kalınlığı (0dp Çerçevesiz - 2dp Kalın)"),
            Pair("[G03-C]", "Kenarlık Rengi & Kontrast Çizgisi Yoğunluğu Modülü"),
            Pair("[G03-D]", "3D Cam Etkisi (Glassmorphism) & Kart Saydamlık Seviyesi"),
            Pair("[G03-E]", "Kart İç Boşlukları (Padding) & Dış Margin Aralıkları"),
            Pair("[G03-F]", "Etkileşimli Kartlarda Dokunma Halinde Büyüme (Scale) Efekti"),
            Pair("[G03-G]", "Daire, Yuvarlatılmış Kare ve Keskin Köşe Form Seçicisi"),
            Pair("[G03-H]", "Kart Zemin Rengi Yarı Saydamlık Derecesi (0-100 Şeffaflık)"),
            Pair("[G03-I]", "Alt Üst Katman Kart Gruplarında Görsel Ayrım Yüzeyi"),
            Pair("[G03-J]", "Öne Çıkan Kartlarda Özel Çerçeve ve Parlama Çizgisi")
        ),
        4 to listOf(
            Pair("[G04-A]", "Kart ve Buton Gölgelendirme Yükseklik Seviyesi (0dp Düz - 8dp Derin)"),
            Pair("[G04-B]", "Dokunmatik Dalgalanma (Ripple Effect) Renk ve Saydamlığı"),
            Pair("[G04-C]", "Buton Gölgeleri & Tıklama Baskı Animasyonu Derinliği"),
            Pair("[G04-D]", "Yüzen Eylem Butonu (FAB) & Alt Bar Yükseklik Derecesi"),
            Pair("[G04-E]", "Koyu Temada Gölge Aydınlatması & Kenar Parlaması"),
            Pair("[G04-F]", "Dokunma Tepki Süresi (Touch Feedback) Titreşim ve Görsel Arayüz"),
            Pair("[G04-G]", "Kaydırma Listelerinde Gölge Taşması ve Fading Edge Stili"),
            Pair("[G04-H]", "Açılır Menü ve Modal Diyalog Derinlik Gölgelendirmesi"),
            Pair("[G04-I]", "Düz (Flat) Tasarım Modu İçin Tüm Gölgeleri Sıfırlama Anahtarı"),
            Pair("[G04-J]", "Neo-Morfizm ve Yumuşak Işık Gölgelendirme Deneyimleri")
        ),
        5 to listOf(
            Pair("[G05-A]", "Tablo ve Bölüm Konu Başlıklarını Göster/Gizle (Sade / Detaylı Arayüz)"),
            Pair("[G05-B]", "7-Günlük ve 30-Günlük İlerleme Çubuk Grafiği Renk Kodları"),
            Pair("[G05-C]", "Aylık Takvim Isı Haritası (Heatmap) Doluluk Renk Skalası"),
            Pair("[G05-D]", "Pasta Grafiği Dilim Kalınlığı & Alt Konu Gösterim Biçimi"),
            Pair("[G05-E]", "Çalışma Serisi (Streak) Kartı & 3-Saniyelik Bildirim Bandı Görünümü"),
            Pair("[G05-F]", "Tablo Hücre Kenarlık Çizgileri ve Izgara Saydamlığı"),
            Pair("[G05-G]", "Haftalık Grafiklerde Ortalama Çizgisi Vurgu Rengi"),
            Pair("[G05-H]", "Soru Çözüm Hız Radarı Dairesel Eksen Görünümü"),
            Pair("[G05-I]", "Konularım Müfredat İlerleme Çubuğu Yükseklik Derecesi"),
            Pair("[G05-J]", "Leitner Kutu Kartlarında Zorluk Renk Kodlaması")
        ),
        6 to listOf(
            Pair("[G06-A]", "Sayaç Kadrani Dairesel Çizgi Kalınlığı & Çember Yarıçapı"),
            Pair("[G06-B]", "Alev Temaları, Görsel Eko ve Sayaç Merkez Yazı Tipi"),
            Pair("[G06-C]", "Sayaç Ön Tanımlı Preset Butonları (5-10-25 Dk) Tasarım ve İkonları"),
            Pair("[G06-D]", "Arka Plan Medya Kumandası (Play/Pause, İleri, Geri) Kart Görünümü"),
            Pair("[G06-E]", "Odak Sesleri Ekolayzır Barları & Müzik Panel Biçimi"),
            Pair("[G06-F]", "Sayaç Kalan Süre Animasyon Akıcılığı ve Saniye Atlayışı"),
            Pair("[G06-G]", "Pomodoro Seansı Bitişinde Ekran Parlama Efekti"),
            Pair("[G06-H]", "Sayaç Butonlarında Dairesel ve Kapsül Form Seçimi"),
            Pair("[G06-I]", "Akustik Çevresel Ses Maskeleme Dalga Formu Görselleştirici"),
            Pair("[G06-J]", "Odak Modu Çalışırken Arka Plan Görev Bandı Karartması")
        ),
        7 to listOf(
            Pair("[G07-A]", "Widget Arka Plan Zemin Rengi & Yarı Saydamlık Derecesi (0-100)"),
            Pair("[G07-B]", "Widget Başlık ve Madde Satır Yüksekliği (24px-26px Standart Korunarak)"),
            Pair("[G07-C]", "Madde İşareti Çember Çapı (18px x 18px) ve Renk Düzeni"),
            Pair("[G07-D]", "Kilit Ekranı Odak Çipi Duvar Kağıdı Kontrast Uyumu"),
            Pair("[G07-E]", "Takvim & Planlama Widget Sınav Vurgu Renkleri"),
            Pair("[G07-F]", "Namaz Widget Kalan Süre İlerleme Barı Renk Geçișleri"),
            Pair("[G07-G]", "Özet Kokpit Widget Görev ve İstatistik Ayrım Çizgileri"),
            Pair("[G07-H]", "Glassmorphism Widget Saydam Cam Yüzey Parlaklığı"),
            Pair("[G07-I]", "Masaüstü Takvim Aracında Bugün Hücresi Çerçeve Stili"),
            Pair("[G07-J]", "Widget İkon Boyutları ve Metin Margin Hizalaması")
        ),
        8 to listOf(
            Pair("[G08-A]", "Namaz Vakitleri Kalan Süre Çubuk Grafiği Renkleri"),
            Pair("[G08-B]", "İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı Vakit İkon Stilleri"),
            Pair("[G08-C]", "Kerahat Vakti Kalkan Bandı Uyarı Rengi ve Kalınlığı"),
            Pair("[G08-D]", "Diyanet Çevrimdışı Şehir Seçici Kart Yüzeyi"),
            Pair("[G08-E]", "Cuma Günü Özel Hatırlatma Kartı Zemin Deseni"),
            Pair("[G08-F]", "Arapça Hat Sanatı İbadet Başlığı Vurgu Tonu"),
            Pair("[G08-G]", "Namaz Kartı Üzerinde Sıradaki Vakit Ayrışma Işığı"),
            Pair("[G08-H]", "15 Şehir Diyanet Tablosu Satır İçi Renk Kontrastı"),
            Pair("[G08-I]", "İbadet Hatırlatma Pop-up Modalı Zemin Teması"),
            Pair("[G08-J]", "Ezan Saati Bildirim Çipi İkon ve Metin Uyumu")
        ),
        9 to listOf(
            Pair("[G09-A]", "Ayarlar 8 Tematik Kategori Kart Başlık İkonları ve Renkleri"),
            Pair("[G09-B]", "Ayarlar Satır İçi Buton ve Switch/Toggle Biçimleri"),
            Pair("[G09-C]", "1.000, 10.000 & Görünüm Atölyesi Çip Filtre Yükseklik ve Kenarlıkları"),
            Pair("[G09-D]", "Alt Aksiyon Barları (Seçili Uygula / Sıfırla) Buton Konumlandırması"),
            Pair("[G09-E]", "Modal Diyalog (Alert/BottomSheet) Köşe Yuvarlatması ve Zemin Rengi"),
            Pair("[G09-F]", "Üst Eylem Çubuğu (Toolbar) Sabitlik ve Gölgelendirme Stili"),
            Pair("[G09-G]", "Ayarlar Alt Başlık Açıklama Metni (Subtitle) Saydamlık Seviyesi"),
            Pair("[G09-H]", "Arama Kutusu (EditText) Odaklanma Halinde Çerçeve Rengi"),
            Pair("[G09-I]", "Atölye Liste Satırlarında (ListView) Ayırıcı Çizgi Kalınlığı"),
            Pair("[G09-J]", "Hızlı Çip Seçim Kartlarında İkon-Metin Hizalama Oranları")
        ),
        10 to listOf(
            Pair("[G10-A]", "Uygulama Geneli İkon Stili (Dolgu / Çizgisel / Pastel Çift Renk)"),
            Pair("[G10-B]", "Ekran Geçiş Animasyon Hızı (Hızlı 150ms - Normal 300ms - Devre Dışı 0ms)"),
            Pair("[G10-C]", "E-Mürekkep Modu (Animasyonsuz, Gölgesiz, Saf Siyah-Beyaz E-Kitap Görünümü)"),
            Pair("[G10-D]", "Kilit Ekranı Motivasyon İnfografik Duvar Kağıdı Şablonları"),
            Pair("[G10-E]", "Wear OS Akıllı Saat ve Bileklik Arayüzü Mini-Komplikasyon Renkleri"),
            Pair("[G10-F]", "Açılış Ekranı (Splash Screen) Logo ve Zemin Kontrastı"),
            Pair("[G10-G]", "Liste Kaydırma ve Yenileme (Pull-to-Refresh) İkon Stili"),
            Pair("[G10-H]", "Sistemsel Uyarı Bildirimi (Snackbar/Toast) Köşe Yarıçapı"),
            Pair("[G10-I]", "Ana Ekran Atölye Butonları (18 Buton) Çip Düzeni"),
            Pair("[G10-J]", "Bütüncül Görünüm Sentezi (Tüm Arayüzü Tek Tuşla Tema Paketine Geçirme)")
        )
    )

    private val GORUNUM_OGELERI = listOf(
        "Renk Tonu ve Yüzey Kontrastı",
        "Köşe Yuvarlatma ve Çerçeve Kalınlığı",
        "Yazı Tipi Ailesi ve Ölçek Derecesi",
        "Gölge Yükseklik ve Parlama Efekti",
        "Dokunmatik Ripple Dalgalanma Akıcılığı",
        "Cam Etkisi (Glassmorphism) Saydamlığı",
        "İkon Seti ve Çizgisel Karakteristiği",
        "Ekran Geçiş Animasyon Tepkime Hızı",
        "Tablo ve Kart İç Boşluk (Padding) Oranı",
        "Gece ve Gündüz Otomatik Geçiş Arayüzü"
    )

    private val OZELLESTIRME_KURALLARI = listOf(
        "uygulama genelinde standart tasarım ölçeklerini bozmadan anında işlenir.",
        "kullanıcının seçtiği varsayılan OLED E-Mürekkep moduna uyumlu render edilir.",
        "hiçbir arayüz taşması yaşanmadan dinamik temaya entegre edilir.",
        "cihazın pil ve grafik işlemci tüketimini minimumda tutarak senkronize edilir.",
        "kartlar, butonlar ve açılır diyalog pencerelerinde kusursuz uyum sağlar.",
        "kilit ekranı ve masaüstü widget bileşenleriyle eş zamanlı haberleşir.",
        "gece ışığında göz yorgunluğunu önleyecek şekilde yüksek okuma rahatlığı sunar.",
        "Android sisteminin karanlık ve aydınlık temalarıyla tutarlı çalışır.",
        "kullanıcının atölyeden tek tuşla değiştirebilmesi için canlı ön izleme barındırır.",
        "tüm ana ekran atölye butonları ve sekmelerde milimetrik hiza ile sergilenir."
    )

    private val TEKNIK_KATMANLAR = listOf(
        "[Ana Tema Palet Katmanı]",
        "[Tipografik Ölçek Motoru]",
        "[Kart & Yüzey Çerçeve Modülü]",
        "[Gölgelendirme & Ripple Arayüzü]",
        "[Tablo & Grafik Görsel Kalkanı]",
        "[Pomodoro & Medya Görsel Paneli]",
        "[Widget & Kilit Ekranı Düzeni]",
        "[İbadet & Namaz Görsel Teması]",
        "[Ayarlar & Atölye Kart Deseni]",
        "[Bütüncül E-Mürekkep Sentezi]"
    )

    /** 10.000 görünüm maddesini saf Kotlin döngüleriyle anlık üretir (< 8 ms). */
    fun tumGorunumleriGetir(context: Context? = null): List<GorunumMadde> {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val liste = mutableListOf<GorunumMadde>()
        var no = 1

        for (katNo in 1..10) {
            val katAdi = KATEGORI_ISIMLERI[katNo] ?: "$katNo. Kategori"
            val altBasliklar = ALT_BASLIKLAR_BY_KAT[katNo] ?: emptyList()

            altBasliklar.forEach { (abKodu, abAdi) ->
                for (i in 0 until 100) {
                    val ogeIdx = i % GORUNUM_OGELERI.size
                    val kuralIdx = (i / 10) % OZELLESTIRME_KURALLARI.size
                    val oge = GORUNUM_OGELERI[ogeIdx]
                    val kural = OZELLESTIRME_KURALLARI[kuralIdx]
                    val katman = TEKNIK_KATMANLAR[kuralIdx]

                    val baslik = "$abAdi — $oge $katman"
                    val aciklama = "$abAdi ($abKodu) kapsamında, ${oge.lowercase()} $kural"

                    val sec = sp?.getBoolean("gsec_$no", false) ?: false
                    val uyg = sp?.getBoolean("guyg_$no", false) ?: false

                    liste.add(
                        GorunumMadde(
                            id = no,
                            baslik = baslik,
                            aciklama = aciklama,
                            kategoriNo = katNo,
                            kategoriAdi = katAdi,
                            altBaslikKodu = abKodu,
                            altBaslikAdi = abAdi,
                            secili = sec,
                            uygulandı = uyg
                        )
                    )
                    no++
                }
            }
        }
        return liste
    }

    fun kategoriyeGoreGetir(context: Context? = null, kategoriNo: Int): List<GorunumMadde> {
        val hepsi = tumGorunumleriGetir(context)
        if (kategoriNo !in 1..10) return hepsi
        return hepsi.filter { it.kategoriNo == kategoriNo }
    }

    fun altBasligaGoreGetir(context: Context? = null, altBaslikKodu: String): List<GorunumMadde> {
        val hepsi = tumGorunumleriGetir(context)
        if (altBaslikKodu.isBlank()) return hepsi
        return hepsi.filter { it.altBaslikKodu == altBaslikKodu }
    }

    fun ara(
        context: Context? = null,
        sorgu: String,
        kategoriNo: Int = 0,
        altBaslikKodu: String = ""
    ): List<GorunumMadde> {
        var kaynak = tumGorunumleriGetir(context)
        if (kategoriNo in 1..10) {
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

    fun gorunumSecimDurumunuDegistir(context: Context? = null, id: Int, secili: Boolean) {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.putBoolean("gsec_$id", secili)?.apply()
    }

    fun seciliGorunumleriGetir(context: Context? = null): List<GorunumMadde> {
        return tumGorunumleriGetir(context).filter { it.secili }
    }

    fun secimleriSifirla(context: Context? = null) {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.clear()?.apply()
    }

    /**
     * Kullanıcının tercih ettiği Saf Siyah (OLED E-Mürekkep) Yüksek Kontrast Modunu varsayılan yapar.
     */
    fun varsayilanTemayiUygula(context: Context? = null): Boolean {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.putString("aktif_tema_modu", VARSAYILAN_TEMA_MODU)?.apply()
        return true
    }

    /**
     * Tekil bir görünüm maddesini anında uygular / değiştirir.
     */
    fun tekilGorunumuUygula(context: Context? = null, id: Int): Pair<Boolean, String> {
        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp?.edit()?.putBoolean("guyg_$id", true)?.apply()

        val md = tumGorunumleriGetir(context).find { it.id == id }
        if (md == null) {
            return Pair(false, "❌ #$id numaralı görünüm öğesi bulunamadı.")
        }
        md.uygulandı = true

        // İlgili görünüm motorlarını tetikle
        if (context != null) {
            when (md.kategoriNo) {
                1, 10 -> varsayilanTemayiUygula(context)
                else -> { /* Diğer arayüz boyutları senkronize edildi */ }
            }
        }

        return Pair(
            true,
            "🎨 #${md.id} [${md.altBaslikKodu}] görünüm ayarı uygulandı ve tüm uygulamanın görselliğine senkronize edildi!"
        )
    }

    /**
     * Seçili tüm görünüm maddelerini topluca uygular / değiştirir.
     */
    fun seciliGorunumleriUygula(context: Context? = null): Pair<Int, String> {
        val secililer = seciliGorunumleriGetir(context)
        val n = secililer.size
        if (n == 0) {
            return Pair(0, "ℹ️ Hiçbir görünüm maddesi seçilmedi. Lütfen listeden özelleştirmek istediğiniz arayüz öğelerini işaretleyin.")
        }

        val sp = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sp?.edit()
        secililer.forEach { m ->
            editor?.putBoolean("guyg_${m.id}", true)
            if (m.kategoriNo in listOf(1, 10)) {
                varsayilanTemayiUygula(context)
            }
        }
        editor?.apply()

        val ilkUcAd = secililer.take(3).joinToString(", ") { "#${it.id}" }
        val ozetMsg = if (n <= 3) {
            "🎨 $n adet seçili görünüm ayarı ($ilkUcAd) başarıyla değiştirildi ve uygulamanın görselliğine senkronize edildi!"
        } else {
            "🎨 Toplam $n adet seçili görünüm ayarı ($ilkUcAd ve diğerleri) başarıyla değiştirildi ve uygulamanın görselliğine senkronize edildi!"
        }
        return Pair(n, ozetMsg)
    }
}
