package com.gunlukasistan.app

/**
 * v7.30 — Yönetmelik ve standart kütüphanesi.
 *
 * Tamamen **çevrimdışı** — internet gerekmez, sahada çalışır.
 * TS 500, TBDY 2018, Planlı Alanlar İmar Yönetmeliği ve
 * yaygın mühendislik tablolarından derlenmiştir.
 *
 * UYARI: Bu bilgiler hızlı başvuru içindir. Bağlayıcı değildir;
 * resmî metin esastır. Her kayıtta kaynak belirtilir.
 */
object YonetmelikVeri {

    /** Tek bir bilgi kaydı. */
    data class Kayit(
        val baslik: String,
        val deger: String,
        val kaynak: String,
        val not: String = "",
        val etiketler: String = ""
    ) {
        /** Arama için tüm metin. */
        val aranabilir: String
            get() = "$baslik $deger $kaynak $not $etiketler".lowercase()
    }

    /** Bilgi kategorisi. */
    data class Bolum(val ad: String, val simge: String, val kayitlar: List<Kayit>)

    val BOLUMLER: List<Bolum> = listOf(

        Bolum("Beton", "🪨", listOf(
            Kayit("Beton sınıfları ve dayanımı",
                "C16/20 · C20/25 · C25/30 · C30/37 · C35/45 · C40/50 · C45/55 · C50/60",
                "TS EN 206 / TS 500",
                "İlk sayı silindir (fck), ikinci küp dayanımıdır (MPa).",
                "beton sınıf dayanım fck c25 c30"),
            Kayit("Taşıyıcı sistemde minimum beton",
                "C25/30",
                "TBDY 2018 · Md. 5.2",
                "Deprem yönetmeliğine göre betonarme taşıyıcı sistemlerde en az C25 kullanılır.",
                "minimum beton deprem c25"),
            Kayit("Beton birim ağırlığı",
                "Betonarme: 25 kN/m³ (2.5 t/m³) · Donatısız: 24 kN/m³",
                "TS 498",
                "Yük hesaplarında kullanılır.",
                "birim ağırlık yoğunluk beton"),
            Kayit("Beton örtüsü (paspayı)",
                "Döşeme: 20 mm · Kiriş/Kolon: 25 mm · Temel (kalıplı): 35 mm · " +
                    "Temel (toprağa dökülen): 50 mm",
                "TS 500 · Md. 6.2",
                "Çevresel etkilere göre artabilir. Denize yakın yapılarda daha fazla.",
                "paspayı beton örtüsü pas payı"),
            Kayit("Beton kür süresi",
                "Normal koşullarda en az 7 gün · Soğuk havada 14 gün",
                "TS 1247",
                "Kür yapılmazsa dayanım %30'a kadar düşebilir.",
                "kür sulama bakım"),
            Kayit("Çökme (slump) değerleri",
                "Temel: 10-15 cm · Kolon/Perde: 15-18 cm · Döşeme: 12-16 cm",
                "TS EN 12350-2",
                "Pompa betonunda genelde S4 (16-21 cm) sınıfı istenir.",
                "slump çökme kıvam işlenebilirlik")
        )),

        Bolum("Donatı", "➰", listOf(
            Kayit("Donatı sınıfları",
                "S220 (düz) · S420 (nervürlü) · S500 (nervürlü)",
                "TS 708",
                "Taşıyıcı sistemde genelde S420 kullanılır.",
                "donatı çelik sınıf s420 s500"),
            Kayit("Donatı birim ağırlıkları",
                "Ø8: 0.395 · Ø10: 0.617 · Ø12: 0.888 · Ø14: 1.208 · Ø16: 1.578 · " +
                    "Ø18: 1.998 · Ø20: 2.466 · Ø22: 2.984 · Ø25: 3.853 · Ø32: 6.313 kg/m",
                "TS 708",
                "Formül: d² / 162 (d mm cinsinden).",
                "demir ağırlık kg/m çap birim"),
            Kayit("Minimum donatı oranı — kolon",
                "%1 (ρmin = 0.01) · Maksimum %4",
                "TBDY 2018 · Md. 7.3",
                "Bindirme bölgelerinde %6'yı aşamaz.",
                "kolon minimum donatı oranı"),
            Kayit("Minimum donatı oranı — kiriş",
                "Çekme donatısı: ρmin = 0.8 × fctd/fyd · Pratikte ~%0.3",
                "TS 500 · Md. 7.3",
                "Üst donatı, alt donatının en az 1/3'ü olmalı.",
                "kiriş minimum donatı"),
            Kayit("Etriye sıklaştırma bölgesi",
                "Kolon: kesit yüksekliği veya 1/6 kolon boyu (min 50 cm) · " +
                    "Kiriş: mesnetten 2h kadar",
                "TBDY 2018 · Md. 7.3-7.4",
                "Sıklaştırma bölgesinde etriye aralığı en fazla 10 cm.",
                "etriye sıklaştırma sarılma bölgesi"),
            Kayit("Kenetlenme boyu (yaklaşık)",
                "Çekmede: 40Ø · Basınçta: 30Ø",
                "TS 500 · Md. 9.1",
                "C25 ve S420 için yaklaşık değerdir. Ø12 için 48 cm.",
                "kenetlenme boyu ankraj bindirme"),
            Kayit("Bindirme boyu",
                "Genellikle 1.25 × kenetlenme boyu = ~50Ø",
                "TS 500 · Md. 9.2",
                "Aynı kesitte bindirme oranı %50'yi geçmemeli.",
                "bindirme ekleme boyu")
        )),

        Bolum("Boyutlandırma", "📐", listOf(
            Kayit("Minimum kolon boyutu",
                "25 × 25 cm (TS 500) · Deprem bölgesinde 30 × 30 cm",
                "TBDY 2018 · Md. 7.3",
                "Kolon en küçük boyutu 30 cm'den, alanı 90000 mm²'den az olamaz.",
                "kolon minimum boyut kesit"),
            Kayit("Minimum kiriş boyutu",
                "Genişlik: min 25 cm · Yükseklik: min 30 cm",
                "TBDY 2018 · Md. 7.4",
                "Kiriş genişliği, kolon genişliği + kiriş yüksekliğini aşamaz.",
                "kiriş minimum boyut"),
            Kayit("Kiriş ön boyutlandırma",
                "h = L/10 (basit) · L/12 (sürekli) · L/6 (konsol)",
                "Pratik kural",
                "b = h/2 alınır, 5 cm katına yuvarlanır.",
                "kiriş yükseklik ön boyutlandırma açıklık"),
            Kayit("Döşeme minimum kalınlığı",
                "Tek yönlü: L/25 (min 8 cm) · Çift yönlü: L/30 (min 10 cm) · " +
                    "Konsol: L/10 (min 12 cm)",
                "TS 500 · Md. 11.2",
                "L kısa kenar açıklığıdır. Kenar oranı 2'yi geçerse tek yönlü çalışır.",
                "döşeme kalınlık plak minimum"),
            Kayit("Perde duvar minimum kalınlığı",
                "Kat yüksekliğinin 1/20'si · En az 25 cm",
                "TBDY 2018 · Md. 7.6",
                "Perde uzunluğu, kalınlığının en az 6 katı olmalı.",
                "perde duvar kalınlık minimum"),
            Kayit("Temel minimum derinliği",
                "Donma derinliği altında · Genelde 1.00-1.50 m",
                "TS 500 / Zemin etüdü",
                "Bölgeye göre değişir, zemin etüdü belirler.",
                "temel derinlik donma")
        )),

        Bolum("Deprem", "🌍", listOf(
            Kayit("Deprem yer hareketi düzeyleri",
                "DD-1: 2475 yıl · DD-2: 475 yıl · DD-3: 72 yıl · DD-4: 43 yıl",
                "TBDY 2018 · Md. 2.2",
                "Normal binalarda DD-2 esas alınır.",
                "deprem düzey dd-2 tekrarlanma"),
            Kayit("Bina kullanım sınıfları (BKS)",
                "BKS-1: Deprem sonrası kullanım gereken (hastane, itfaiye) · " +
                    "BKS-2: Kalabalık (okul, AVM) · BKS-3: Diğer (konut, ofis)",
                "TBDY 2018 · Tablo 3.1",
                "Bina önem katsayısı: BKS-1 → I=1.5, BKS-2 → I=1.2, BKS-3 → I=1.0",
                "bks bina kullanım sınıfı önem katsayısı"),
            Kayit("Göreli kat ötelemesi sınırı",
                "δ/h ≤ 0.008 κ (gevrek dolgulu) · ≤ 0.016 κ (esnek dolgulu)",
                "TBDY 2018 · Md. 4.9",
                "κ: betonarme için 1.0, çelik için 0.5",
                "göreli öteleme drift sınır"),
            Kayit("Düzensizlik türleri — planda",
                "A1: Burulma · A2: Döşeme süreksizliği · A3: Planda çıkıntı",
                "TBDY 2018 · Tablo 3.6",
                "A1 burulma düzensizliği en yaygın olanıdır (ηbi > 1.2).",
                "düzensizlik plan burulma a1"),
            Kayit("Düzensizlik türleri — düşeyde",
                "B1: Komşu katlar arası dayanım · B2: Rijitlik (yumuşak kat) · " +
                    "B3: Taşıyıcı sistem süreksizliği",
                "TBDY 2018 · Tablo 3.7",
                "B3 (kolonun kirişe oturması) kesinlikle yasaktır.",
                "düzensizlik düşey yumuşak kat b2"),
            Kayit("Güçlü kolon - zayıf kiriş",
                "ΣMra + ΣMrü ≥ 1.2 (ΣMri + ΣMrj)",
                "TBDY 2018 · Md. 7.3.5",
                "Kolonların kirişlerden %20 daha güçlü olması istenir.",
                "güçlü kolon zayıf kiriş kapasite")
        )),

        Bolum("Yükler", "⚖", listOf(
            Kayit("Hareketli yükler — konut",
                "Oda/koridor: 2.0 kN/m² · Balkon: 4.0 · Merdiven: 3.5 · Çatı: 1.5",
                "TS 498",
                "Depo ve arşivlerde 5.0 kN/m² ve üzeri.",
                "hareketli yük konut q döşeme"),
            Kayit("Hareketli yükler — diğer",
                "Ofis: 2.0-3.0 · Okul: 3.5 · Mağaza: 5.0 · Toplantı salonu: 5.0 kN/m²",
                "TS 498 · Tablo 7",
                "",
                "hareketli yük ofis okul mağaza"),
            Kayit("Malzeme birim ağırlıkları",
                "Betonarme: 25 · Tuğla duvar: 18 · Gaz beton: 8 · Sıva: 20 · " +
                    "Şap: 22 · Su: 10 · Çelik: 78.5 kN/m³",
                "TS 498",
                "Ölü yük hesabında kullanılır.",
                "birim ağırlık malzeme ölü yük"),
            Kayit("Tipik döşeme ölü yükü",
                "15 cm plak + kaplama + sıva ≈ 5.5-6.0 kN/m²",
                "Pratik değer",
                "Plak 3.75 + şap 1.1 + kaplama 0.5 + sıva 0.4 + bölme 1.0",
                "ölü yük döşeme g kaplama"),
            Kayit("Kar yükü",
                "Bölgeye göre 0.75-2.00 kN/m²",
                "TS 498 · Md. 11",
                "Rakım ve çatı eğimine göre değişir.",
                "kar yükü çatı"),
            Kayit("Rüzgâr yükü",
                "Bölge ve yüksekliğe göre 0.5-1.6 kN/m²",
                "TS 498 · Md. 12",
                "Yüksek binalarda ayrıntılı hesap gerekir.",
                "rüzgar yükü")
        )),

        Bolum("İmar", "🏙", listOf(
            Kayit("TAKS — Taban Alanı Kat Sayısı",
                "TAKS = Taban alanı / Parsel alanı",
                "Planlı Alanlar İmar Yönetmeliği",
                "Örnek: 500 m² parsel, TAKS 0.30 → en fazla 150 m² taban.",
                "taks taban alanı kat sayısı imar"),
            Kayit("KAKS (Emsal) — Kat Alanı Kat Sayısı",
                "KAKS = Toplam inşaat alanı / Parsel alanı",
                "Planlı Alanlar İmar Yönetmeliği",
                "Örnek: 500 m² parsel, KAKS 1.20 → 600 m² toplam inşaat.",
                "kaks emsal kat alanı toplam inşaat"),
            Kayit("Çekme mesafeleri",
                "Ön bahçe: min 5 m · Yan bahçe: min 3 m · Arka bahçe: min 3 m",
                "Planlı Alanlar İmar Yönetmeliği · Md. 20",
                "İmar planı farklı belirleyebilir. Bitişik nizamda yan çekme yoktur.",
                "çekme mesafesi bahçe ön yan arka"),
            Kayit("Kat yüksekliği",
                "Konut: 2.80 m (net min 2.40 m) · Ticaret: 3.50-4.00 m",
                "Planlı Alanlar İmar Yönetmeliği · Md. 28",
                "Kat yüksekliği döşeme üstünden döşeme üstüne ölçülür.",
                "kat yüksekliği net"),
            Kayit("Otopark ihtiyacı",
                "Konut: her 1 daire için 1 araç · Ofis: her 50 m² için 1",
                "Otopark Yönetmeliği",
                "Belediyeler farklı oran belirleyebilir.",
                "otopark araç park"),
            Kayit("Asansör zorunluluğu",
                "Kat adedi 4 ve üzeri binalarda zorunlu",
                "Planlı Alanlar İmar Yönetmeliği · Md. 34",
                "Zemin + 3 kat üzeri. Asansör kabini min 1.10 × 1.40 m.",
                "asansör zorunlu kat")
        )),

        Bolum("Merdiven ve Kaçış", "🪜", listOf(
            Kayit("Merdiven altın kuralı",
                "2h + b = 61 ~ 65 cm (ideal 63)",
                "Ergonomi kuralı",
                "h: rıht yüksekliği, b: basamak genişliği. Adım uzunluğuna dayanır.",
                "merdiven 2h+b rıht basamak kural"),
            Kayit("Merdiven ölçüleri — konut içi",
                "Rıht: max 18 cm · Basamak: min 25 cm · Kol genişliği: min 90 cm",
                "Planlı Alanlar İmar Yönetmeliği",
                "Tüm rıhtlar eşit olmalıdır.",
                "merdiven konut rıht basamak"),
            Kayit("Merdiven ölçüleri — ortak alan",
                "Rıht: max 17.5 cm · Basamak: min 28 cm · Kol genişliği: min 120 cm",
                "Planlı Alanlar İmar Yönetmeliği · Md. 33",
                "Apartman ve umumi binalarda geçerlidir.",
                "merdiven ortak apartman"),
            Kayit("Baş kurtarma yüksekliği",
                "Min 220 cm",
                "Planlı Alanlar İmar Yönetmeliği",
                "Merdiven kolu üzerindeki serbest yükseklik.",
                "baş kurtarma yükseklik merdiven"),
            Kayit("Korkuluk yüksekliği",
                "Min 90 cm · Kaçış merdiveninde 110 cm",
                "Planlı Alanlar / BYKHY",
                "Dikey çubuk aralığı en fazla 12 cm (çocuk güvenliği).",
                "korkuluk yükseklik parmaklık"),
            Kayit("Kaçış mesafesi",
                "Konut: max 30 m · Diğer: max 45 m (sprinklerli 60 m)",
                "BYKHY · Md. 33",
                "En uzak noktadan kaçış merdivenine olan mesafe.",
                "kaçış mesafesi yangın"),
            Kayit("Kapı ölçüleri",
                "Giriş: 100 × 210 cm · Oda: 90 × 200 · Banyo/WC: 80 × 200 · " +
                    "Engelli erişimi: min 90 cm net",
                "Planlı Alanlar İmar Yönetmeliği",
                "Kaçış kapıları dışa açılmalıdır.",
                "kapı ölçü genişlik yükseklik")
        )),

        Bolum("Zemin", "⛏", listOf(
            Kayit("Zemin emniyet gerilmesi (yaklaşık)",
                "Kaya: 400-1000 · Sıkı kum: 200-400 · Orta kum: 100-200 · " +
                    "Yumuşak kil: 50-100 kPa",
                "Yaklaşık değerler",
                "Kesin değer zemin etüt raporundan alınır.",
                "zemin emniyet gerilmesi taşıma gücü"),
            Kayit("Zemin sınıfları (TBDY)",
                "ZA: Sağlam kaya · ZB: Az ayrışmış kaya · ZC: Sıkı kum/kil · " +
                    "ZD: Orta sıkı · ZE: Gevşek/yumuşak · ZF: Özel inceleme",
                "TBDY 2018 · Tablo 16.1",
                "Vs30 hızına göre belirlenir.",
                "zemin sınıfı za zc zd vs30"),
            Kayit("Kazı kabarma katsayıları",
                "Yumuşak toprak: 1.15 · Normal toprak: 1.25 · Sert: 1.30 · Kaya: 1.50",
                "Pratik değerler",
                "Kazılan zemin gevşeyip hacmi artar. Nakliye hesabında kullanılır.",
                "kabarma kazı nakliye hacim"),
            Kayit("Şev eğimleri (güvenli)",
                "Kum: 1/1.5 · Kil: 1/1 · Kaya: 1/0.25",
                "Pratik değerler",
                "Derin kazılarda iksa gerekir.",
                "şev eğim kazı iksa")
        )),

        Bolum("Çelik", "🔩", listOf(
            Kayit("Çelik sınıfları",
                "S235 · S275 · S355 · S420 · S460",
                "TS EN 10025",
                "Sayı, akma dayanımıdır (MPa). Yapıda genelde S235 ve S355.",
                "çelik sınıf s235 s355 akma"),
            Kayit("Çelik birim ağırlığı",
                "7850 kg/m³ (78.5 kN/m³)",
                "TS 498",
                "",
                "çelik birim ağırlık yoğunluk"),
            Kayit("Elastisite modülü",
                "Çelik: 200000 MPa · Beton (C25): ~30000 MPa",
                "TS 500 / TS EN 1993",
                "Beton için Ec = 3250√fck + 14000",
                "elastisite modülü e young"),
            Kayit("Kaynak dikiş kalınlığı",
                "a ≥ 0.7 × en ince parça kalınlığı · min 3 mm",
                "TS EN 1993-1-8",
                "Köşe kaynağı için geçerlidir.",
                "kaynak dikiş kalınlık a")
        )),

        Bolum("Yalıtım", "🌡", listOf(
            Kayit("Isı yalıtım kalınlıkları (bölgelere göre)",
                "1. bölge: 4-5 cm · 2. bölge: 5-6 cm · 3. bölge: 6-8 cm · 4. bölge: 8-10 cm",
                "TS 825",
                "EPS/XPS için yaklaşık değerler. Hesapla kesinleştirilir.",
                "ısı yalıtım kalınlık ts825 mantolama"),
            Kayit("Isı iletim katsayıları (λ)",
                "XPS: 0.030 · EPS: 0.035 · Taş yünü: 0.040 · Tuğla: 0.45 · " +
                    "Beton: 2.5 W/mK",
                "TS 825",
                "Düşük λ = iyi yalıtım.",
                "lambda ısı iletim katsayı"),
            Kayit("Su yalıtımı zorunlu alanlar",
                "Temel, bodrum perdeleri, ıslak hacimler, balkon, teras, çatı",
                "Su Yalıtımı Yönetmeliği",
                "Islak hacimlerde döşeme 5 cm düşürülür.",
                "su yalıtımı ıslak hacim temel"),
            Kayit("Ses yalıtımı",
                "Daireler arası duvar: min 50 dB · Döşeme darbe sesi: max 60 dB",
                "Binaların Gürültüye Karşı Korunması Yönetmeliği",
                "",
                "ses yalıtım db gürültü")
        ))
    )

    /** Tüm kayıtlar tek listede. */
    val TUMU: List<Pair<Bolum, Kayit>> by lazy {
        BOLUMLER.flatMap { b -> b.kayitlar.map { b to it } }
    }

    /** Arama — başlık, değer, kaynak, not ve etiketlerde geçer. */
    fun ara(sorgu: String): List<Pair<Bolum, Kayit>> {
        val q = sorgu.trim().lowercase()
        if (q.length < 2) return emptyList()
        // Türkçe karakter esnekliği
        val qs = q.replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
            .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
        return TUMU.filter { (_, k) ->
            val m = k.aranabilir
            val ms = m.replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
            m.contains(q) || ms.contains(qs)
        }
    }

    val toplamKayit: Int get() = TUMU.size
}
