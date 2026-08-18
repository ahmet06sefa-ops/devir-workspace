package com.gunlukasistan.app

import android.content.Context

/**
 * v9.5 — Tek satır hızlı komut (öneri 29).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN
 * ══════════════════════════════════════════════════════════════════
 * Bir görev eklemek için: FAB → tür seç → başlık yaz → tarih seç →
 * saat seç → kaydet. Altı dokunuş. Aklına gelen şeyi hızlıca
 * yazmak isteyen için fazla.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM
 * ══════════════════════════════════════════════════════════════════
 * Tek satır:
 *
 *     gorev: matematik çalış yarın 14:00
 *     not: kütüphane 22:00'de kapanıyor
 *     konu: türev kuralları
 *     sinav: TYT denemesi 15 mart
 *
 * Önek yazılmazsa tür tahmin ediliyor: tarih/saat varsa görev,
 * yoksa not.
 *
 * ── Neden AsistanKomut kullanılmadı ──
 * `AsistanKomut` AI'nın ürettiği komutları çözüyor (`[[GOREV:...]]`
 * gibi işaretli biçim) ve **AI çağrısı gerektiriyor**. Bu sınıf
 * tamamen yerel: çevrimdışı çalışıyor, anında sonuç veriyor,
 * kota harcamıyor.
 *
 * ── Tarih ayrıştırma ──
 * `NaturalDate.parse` zaten var ve "yarın 14:00", "pazartesi",
 * "3 gün sonra" gibi ifadeleri çözüyor. Yeniden yazmak yerine
 * o kullanılıyor.
 */
object HizliKomut {

    private const val TAG = "HizliKomut"

    enum class Tur { GOREV, NOT, KONU, SINAV, ETKINLIK, HARCAMA, BILINMEYEN }

    /**
     * Ayrıştırma sonucu.
     *
     * @param baslik temizlenmiş başlık (önek ve tarih ifadesi çıkarılmış)
     * @param zamanMs 0 = tarih yok
     * @param saatVar tarih ifadesinde saat de belirtilmiş mi
     */
    data class Sonuc(
        val tur: Tur,
        val baslik: String,
        val zamanMs: Long = 0L,
        val saatVar: Boolean = false,
        val tekrarGun: Int = 0,
        /** v9.7: harcama komutlarında yakalanan tutar (0 = yok). */
        val tutar: Double = 0.0
    ) {
        val gecerli: Boolean get() = tur != Tur.BILINMEYEN && baslik.isNotBlank()
    }

    /** Önek → tür eşlemesi. Türkçe kısaltmalar da kabul ediliyor. */
    private val ONEKLER = mapOf(
        "gorev" to Tur.GOREV, "görev" to Tur.GOREV, "g" to Tur.GOREV,
        "task" to Tur.GOREV, "yap" to Tur.GOREV,
        "not" to Tur.NOT, "n" to Tur.NOT, "note" to Tur.NOT,
        "konu" to Tur.KONU, "k" to Tur.KONU, "ders" to Tur.KONU,
        "sinav" to Tur.SINAV, "sınav" to Tur.SINAV, "s" to Tur.SINAV,
        "deneme" to Tur.SINAV,
        "etkinlik" to Tur.ETKINLIK, "e" to Tur.ETKINLIK, "olay" to Tur.ETKINLIK,
        // v9.7 · Grup F: harcama defterine tek satırda kayıt
        "harcama" to Tur.HARCAMA, "h" to Tur.HARCAMA, "gider" to Tur.HARCAMA,
        "masraf" to Tur.HARCAMA
    )

    /**
     * v9.7 — Metnin sonundaki tutarı yakalar.
     *
     * "market 250" → 250.0 · "kahve 45,50" → 45.5
     *
     * Neden yalnızca SONDAKİ sayı: "3 kg elma 120" gibi girdilerde
     * baştaki sayı miktar, sondaki tutar. Baştan aramak yanlış
     * değeri alırdı.
     *
     * @return tutar ve tutar çıkarılmış metin
     */
    fun tutarAyikla(metin: String): Pair<Double, String> {
        val kirp = metin.trim()
        // Sonda: isteğe bağlı ₺/tl, ondalık virgül veya nokta
        val kalip = Regex("""(.*?)\s*([0-9]+(?:[.,][0-9]{1,2})?)\s*(?:₺|tl|TL|lira)?\s*$""")
        val eslesme = kalip.find(kirp) ?: return 0.0 to kirp
        val sayi = eslesme.groupValues[2].replace(',', '.').toDoubleOrNull()
            ?: return 0.0 to kirp
        val kalan = eslesme.groupValues[1].trim()
        // Metin tamamen sayıysa açıklama kalmaz — yine de tutar geçerli
        return sayi to kalan
    }

    // ══════════════════════════════════════════════════════════

    /**
     * Tek satırı çözümler.
     *
     * ```
     * HizliKomut.coz(ctx, "gorev: matematik yarın 14:00")
     *   → Sonuc(GOREV, "matematik", <yarın 14:00>, saatVar=true)
     * ```
     */
    fun coz(context: Context, girdi: String): Sonuc {
        val ham = girdi.trim()
        if (ham.isBlank()) return Sonuc(Tur.BILINMEYEN, "")

        // ---- 1. Önek ----
        var tur = Tur.BILINMEYEN
        var govde = ham

        val ayrac = ham.indexOfFirst { it == ':' || it == '：' }
        if (ayrac in 1..12) {
            val onek = ham.substring(0, ayrac).trim().lowercase(java.util.Locale("tr"))
            ONEKLER[onek]?.let {
                tur = it
                govde = ham.substring(ayrac + 1).trim()
            }
        }

        if (govde.isBlank()) return Sonuc(Tur.BILINMEYEN, "")

        // ---- 2. Tarih/saat ----
        //
        // NaturalDate.parse hem tarihi buluyor hem metinden çıkarıyor.
        // Bulamazsa millis 0 dönüyor ve metin olduğu gibi kalıyor.
        var zaman = 0L
        var saatVar = false
        var tekrar = 0
        var baslik = govde

        runCatching {
            val sonuc = NaturalDate.parse(govde)
            if (sonuc.millis > 0) {
                zaman = sonuc.millis
                saatVar = sonuc.hasTime
                tekrar = sonuc.repeatDow
                // NaturalDate.Result.text = tarih ifadesi çıkarılmış metin
                if (sonuc.text.isNotBlank()) baslik = sonuc.text.trim()
            }
        }.onFailure { android.util.Log.w(TAG, "Tarih ayrıştırma", it) }

        // ---- 3. Tür tahmini ----
        //
        // Önek yoksa: tarih/saat varsa görev (yapılacak bir şey),
        // yoksa not (akla gelen bir bilgi). Bu, günlük kullanımda
        // doğru tahmin veriyor: "yarın 14:00 doktor" → görev,
        // "kütüphane 22'de kapanıyor" → not.
        if (tur == Tur.BILINMEYEN) {
            tur = if (zaman > 0) Tur.GOREV else Tur.NOT
        }

        // ---- 4. Harcama tutarı (v9.7) ----
        if (tur == Tur.HARCAMA) {
            val (tutar, kalan) = tutarAyikla(baslik)
            // Tutar yoksa harcama kaydedilemez — kullanıcı
            // "harcama: market" yazmışsa ne kadar bilmiyoruz
            if (tutar <= 0) return Sonuc(Tur.BILINMEYEN, "")
            return Sonuc(tur, kalan.ifBlank { baslik.trim() }, zaman, saatVar, tekrar, tutar)
        }

        if (baslik.isBlank()) return Sonuc(Tur.BILINMEYEN, "")
        return Sonuc(tur, baslik.trim(), zaman, saatVar, tekrar)
    }

    // ══════════════════════════════════════════════════════════
    // Uygulama
    // ══════════════════════════════════════════════════════════

    /**
     * Çözümlenen komutu kaydeder.
     *
     * @return kullanıcıya gösterilecek onay metni, başarısızsa null
     */
    fun uygula(context: Context, sonuc: Sonuc): String? {
        if (!sonuc.gecerli) return null
        // v9.8 · Öneri 50: hızlı komut kullanımı sayılıyor.
        // Bu özelliğin (v9.5) gerçekten kullanılıp kullanılmadığını
        // bilmiyoruz — ölçmezsek asla öğrenemeyiz.
        runCatching { Kullanim.eylem(context, Kullanim.Eylem.HIZLI_KOMUT) }
        return runCatching {
            when (sonuc.tur) {
                Tur.GOREV -> {
                    val gorevler = Store.loadTasks(context)
                    val yeni = Store.Task(
                        id = System.currentTimeMillis(),
                        text = sonuc.baslik,
                        done = false,
                        createdAt = System.currentTimeMillis(),
                        dueAt = sonuc.zamanMs
                    )
                    gorevler.add(yeni)
                    Store.saveTasks(context, gorevler)
                    if (sonuc.zamanMs > System.currentTimeMillis()) {
                        runCatching {
                            AlarmScheduler.schedule(context, yeni.id, yeni.text, yeni.dueAt)
                        }
                    }
                    if (sonuc.zamanMs > 0) {
                        context.getString(
                            R.string.hk_gorev_tarihli,
                            sonuc.baslik,
                            NaturalDate.describe(sonuc.zamanMs, sonuc.saatVar)
                        )
                    } else {
                        context.getString(R.string.hk_gorev, sonuc.baslik)
                    }
                }

                Tur.NOT -> {
                    val notlar = Store.loadNotes(context)
                    notlar.add(
                        Store.Note(
                            id = System.currentTimeMillis(),
                            title = sonuc.baslik.take(48),
                            content = sonuc.baslik,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    Store.saveNotes(context, notlar)
                    context.getString(R.string.hk_not, sonuc.baslik.take(40))
                }

                Tur.KONU -> {
                    val konular = Store.loadTopics(context)
                    konular.add(
                        Store.Topic(
                            id = System.currentTimeMillis(),
                            title = sonuc.baslik,
                            createdAt = System.currentTimeMillis(),
                            items = mutableListOf()
                        )
                    )
                    Store.saveTopics(context, konular)
                    context.getString(R.string.hk_konu, sonuc.baslik)
                }

                Tur.SINAV -> {
                    // Sınav tarihi tek bir değer; tarih verildiyse onu ayarla
                    if (sonuc.zamanMs > 0) {
                        val anahtar = java.text.SimpleDateFormat(
                            "yyyyMMdd", java.util.Locale.US
                        ).format(java.util.Date(sonuc.zamanMs))
                        Store.setExamDate(context, anahtar)
                    }
                    // Ayrıca etkinlik olarak da ekle — birden çok sınav olabilir
                    etkinlikEkle(context, sonuc, "📚")
                    context.getString(R.string.hk_sinav, sonuc.baslik)
                }

                Tur.ETKINLIK -> {
                    etkinlikEkle(context, sonuc, "🎯")
                    context.getString(R.string.hk_etkinlik, sonuc.baslik)
                }

                // v9.7 · Grup F: "harcama: market 250" → bütçe defterine
                Tur.HARCAMA -> {
                    if (sonuc.tutar <= 0) return@runCatching null
                    val kategori = kategoriTahmin(sonuc.baslik)
                    Butce.ekle(
                        context,
                        Butce.Kalem(
                            id = System.currentTimeMillis(),
                            tutar = sonuc.tutar,
                            kategori = kategori,
                            aciklama = sonuc.baslik,
                            millis = if (sonuc.zamanMs > 0) sonuc.zamanMs
                            else System.currentTimeMillis(),
                            gelir = false
                        )
                    )
                    runCatching {
                        Kullanim.eylem(context, Kullanim.Eylem.HARCAMA_EKLE)
                    }
                    context.getString(
                        R.string.hk_harcama,
                        Takip.paraMetni(sonuc.tutar),
                        context.getString(kategori.adRes)
                    )
                }

                Tur.BILINMEYEN -> null
            }
        }.onFailure { android.util.Log.w(TAG, "uygula", it) }.getOrNull()
    }

    /**
     * v9.7 — Açıklamadan kategori tahmini.
     *
     * Basit anahtar kelime eşlemesi. AI kullanmıyorum: bu işlem
     * çevrimdışı ve anında olmalı, ayrıca "market" kelimesini
     * tanımak için model çalıştırmak abartı olurdu.
     *
     * Tahmin tutmazsa DİĞER'e düşüyor ve kullanıcı bütçe
     * ekranından düzeltebiliyor — yanlış tahmin veri kaybı değil.
     *
     * ── 🔴 TESTİN YAKALADIĞI HATA: ünsüz yumuşaması ──
     * İlk yazdığım liste yalnızca yalın kökleri içeriyordu ve
     * "öğle yemeği" DİĞER'e düşüyordu. Sebep Türkçeye özgü:
     *
     *     yemek + i  →  yeme**ğ**i     (k → ğ)
     *     kitap + ı  →  kita**b**ı     (p → b)
     *     ilaç  + ı  →  ila**c**ı      (ç → c)
     *
     * `"yemeği".contains("yemek")` **false** döner. İngilizce
     * yazılmış bir eşleyici bu tuzağa düşmez çünkü İngilizcede
     * kök değişmiyor. Çözüm: yumuşamış gövdeleri de listeye
     * eklemek ("yemeğ", "kitab", "ilac"). Ekler değişken
     * (-i/-ı/-u/-ü) olduğu için sondaki ünlüyü hiç yazmıyorum;
     * gövde eşleşmesi yeterli.
     */
    fun kategoriTahmin(metin: String): Butce.Kategori {
        val m = metin.lowercase(java.util.Locale("tr"))
        // Her satır: kategori → aranacak gövdeler.
        // Yumuşayan sözcüklerin HEM yalın HEM yumuşamış gövdesi var.
        val esleme = listOf(
            Butce.Kategori.MARKET to listOf(
                "market", "bakkal", "manav", "migros", "bim", "a101",
                "şok market", "alışveriş", "alisveris", "carrefour", "tarım kredi"
            ),
            Butce.Kategori.YEMEK to listOf(
                "yemek", "yemeğ", "yemeg",          // yemeği/yemeğe
                "restoran", "restoran", "lokanta", "kahve", "kafe", "cafe",
                "börek", "böreğ", "borek", "boreg", // böreği
                "döner", "doner", "pizza", "burger", "çay", "tost", "simit",
                "kahvaltı", "kahvalti", "yemekhane", "menü", "menu"
            ),
            Butce.Kategori.ULASIM to listOf(
                "otobüs", "otobus", "metro", "taksi", "benzin", "benzini",
                "yakıt", "yakit", "mazot", "motorin", "bilet", "bileti",
                "dolmuş", "dolmus", "hgs", "ogs", "otopark", "otoparkı",
                "uçak", "ucak", "uçağ", "ucag",     // uçağı
                "tren", "vapur", "marmaray"
            ),
            Butce.Kategori.FATURA to listOf(
                "fatura", "elektrik", "elektriğ", "elektrig",  // elektriği
                "doğalgaz", "dogalgaz", "internet", "telefon", "aidat",
                "su fatura", "su parası", "su parasi"
            ),
            Butce.Kategori.KIRA to listOf("kira", "depozito", "emlak"),
            Butce.Kategori.SAGLIK to listOf(
                "ilaç", "ilac",                     // ilacı (ç→c zaten kapsanıyor)
                "eczane", "doktor", "hastane", "muayene", "tahlil",
                "diş", "dis", "gözlük", "gözlüğ", "gozluk", "gozlug",
                "spor salon", "fitness", "psikolog", "aşı", "asi"
            ),
            Butce.Kategori.EGITIM to listOf(
                "kitap", "kitab",                   // kitabı
                "kurs", "ders", "okul", "kırtasiye", "kirtasiye",
                "defter", "sınav", "sinav", "yayın", "yayin",
                "kalem", "fotokopi", "servis ücret", "yurt"
            ),
            Butce.Kategori.GIYIM to listOf(
                "giyim", "ayakkabı", "ayakkabi", "pantolon",
                "gömlek", "gömleğ", "gomlek", "gomleg",
                "tişört", "tisort", "mont", "elbise", "çorap", "corap",
                "etek", "eteğ", "ceket", "kazak", "kazağ"
            ),
            Butce.Kategori.EGLENCE to listOf(
                "sinema", "konser", "oyun", "tiyatro",
                "maç", "mac", "gezi", "tatil", "otel", "müze", "muze",
                "bilgisayar oyun", "hediye"
            ),
            Butce.Kategori.ABONELIK to listOf(
                "abonelik", "aboneliğ", "abonelig", "netflix", "spotify",
                "youtube", "üyelik", "üyeliğ", "uyelik", "uyelig",
                "premium", "disney", "exxen", "blutv"
            ),
            Butce.Kategori.BIRIKIM to listOf(
                "birikim", "altın", "altin", "yatırım", "yatirim",
                "tasarruf", "dolar", "euro", "fon", "hisse"
            )
        )
        esleme.forEach { (kategori, govdeler) ->
            if (govdeler.any { m.contains(it) }) return kategori
        }
        return Butce.Kategori.DIGER
    }

    private fun etkinlikEkle(context: Context, sonuc: Sonuc, emoji: String) {
        val ms = if (sonuc.zamanMs > 0) sonuc.zamanMs else System.currentTimeMillis()
        val anahtar = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date(ms))
        val etkinlikler = Store.loadEvents(context)
        etkinlikler.add(
            Store.DayEvent(
                id = System.currentTimeMillis(),
                title = sonuc.baslik,
                dateKey = anahtar,
                emoji = emoji,
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveEvents(context, etkinlikler)
    }

    /** Tek adımda: çözümle + uygula. */
    fun calistir(context: Context, girdi: String): String? =
        uygula(context, coz(context, girdi))

    // ══════════════════════════════════════════════════════════

    /** Kullanıcıya gösterilecek örnekler. */
    fun ornekler(context: Context): List<String> = listOf(
        context.getString(R.string.hk_ornek_1),
        context.getString(R.string.hk_ornek_2),
        context.getString(R.string.hk_ornek_3),
        context.getString(R.string.hk_ornek_4),
        context.getString(R.string.hk_ornek_5)
    )

    /** Tür adı — önizleme için. */
    fun turAdi(context: Context, tur: Tur): String = context.getString(
        when (tur) {
            Tur.GOREV -> R.string.hk_t_gorev
            Tur.NOT -> R.string.hk_t_not
            Tur.KONU -> R.string.hk_t_konu
            Tur.SINAV -> R.string.hk_t_sinav
            Tur.ETKINLIK -> R.string.hk_t_etkinlik
            Tur.HARCAMA -> R.string.hk_t_harcama
            Tur.BILINMEYEN -> R.string.hk_t_bilinmeyen
        }
    )
}
