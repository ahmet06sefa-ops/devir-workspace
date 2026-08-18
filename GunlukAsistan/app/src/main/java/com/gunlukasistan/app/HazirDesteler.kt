package com.gunlukasistan.app

/**
 * v7.33 — Uygulamayla gelen hazır bilgi kartı desteleri.
 *
 * Kurs içeriğinden ve yönetmelik verisinden damıtılmış, ezberlenmesi
 * gereken bilgiler. Kullanıcı ilk açılışta bunları yükler, sonra
 * kendi kartlarını ekleyebilir.
 *
 * Kart seçimi ilkesi: **sık kullanılan ve akılda kalması gereken** şeyler.
 * Uzun açıklamalar değil, tek satırda cevaplanabilen bilgiler.
 */
object HazirDesteler {

    /** Deste adı → simge eşlemesi. */
    private val SIMGELER = mapOf(
        "AutoCAD Kısayolları" to "📐",
        "Revit Kısayolları" to "🏗",
        "Mühendislik Formülleri" to "🧮",
        "Standart Değerler" to "📏",
        "Yönetmelik Sayıları" to "📋",
        "Kendi kartlarım" to "✍"
    )

    fun simge(deste: String): String = SIMGELER[deste] ?: "🗂"

    /** Deste adı → (ön yüz, arka yüz) listesi. */
    val TUMU: List<Pair<String, List<Pair<String, String>>>> = listOf(

        "AutoCAD Kısayolları" to listOf(
            "L" to "LINE — Çizgi çiz",
            "PL" to "PLINE — Birleşik çizgi (polyline)",
            "C" to "CIRCLE — Daire",
            "A" to "ARC — Yay",
            "REC" to "RECTANG — Dikdörtgen",
            "POL" to "POLYGON — Çokgen",
            "EL" to "ELLIPSE — Elips",
            "M" to "MOVE — Taşı",
            "CO" to "COPY — Kopyala",
            "RO" to "ROTATE — Döndür",
            "SC" to "SCALE — Ölçekle",
            "MI" to "MIRROR — Aynala",
            "O" to "OFFSET — Paralel kopya (duvar çizmenin en hızlı yolu)",
            "TR" to "TRIM — Buda",
            "EX" to "EXTEND — Uzat",
            "F" to "FILLET — Köşe yuvarlat (R=0 ile köşe birleştir)",
            "CHA" to "CHAMFER — Pah kır",
            "AR" to "ARRAY — Dizi oluştur",
            "S" to "STRETCH — Ger / boyut değiştir",
            "E" to "ERASE — Sil",
            "X" to "EXPLODE — Patlat",
            "H" to "HATCH — Tarama",
            "LA" to "LAYER — Katman yöneticisi",
            "MA" to "MATCHPROP — Özellik kopyala",
            "B" to "BLOCK — Blok oluştur",
            "I" to "INSERT — Blok yerleştir",
            "W" to "WBLOCK — Bloğu dosyaya kaydet",
            "DLI" to "DIMLINEAR — Doğrusal ölçü",
            "DCO" to "DIMCONTINUE — Sürekli ölçü",
            "DBA" to "DIMBASELINE — Temel ölçü",
            "D" to "DIMSTYLE — Ölçü stili",
            "T / MT" to "MTEXT — Çok satırlı yazı",
            "DT" to "TEXT — Tek satır yazı",
            "AA" to "AREA — Alan hesapla",
            "DI" to "DIST — İki nokta arası mesafe",
            "LI" to "LIST — Nesne bilgisi",
            "BO" to "BOUNDARY — Kapalı alan üret",
            "XR" to "XREF — Harici referans",
            "PU" to "PURGE — Kullanılmayanları temizle",
            "OS" to "OSNAP — Nesne kenetleme ayarları",
            "Z" to "ZOOM — Yakınlaştır",
            "P" to "PAN — Kaydır",
            "TB" to "TABLE — Tablo oluştur",
            "F3" to "OSNAP aç/kapat",
            "F8" to "ORTHO aç/kapat (dik çizim)",
            "F10" to "POLAR takip aç/kapat",
            "F12" to "Dinamik giriş (DYN) aç/kapat"
        ),

        "Revit Kısayolları" to listOf(
            "WA" to "Wall — Duvar",
            "DR" to "Door — Kapı",
            "WN" to "Window — Pencere",
            "LL" to "Level — Seviye (yalnızca cephe/kesitte)",
            "GR" to "Grid — Aks",
            "CL" to "Column — Kolon",
            "BM" to "Beam — Kiriş",
            "MV" to "Move — Taşı",
            "CO / CC" to "Copy — Kopyala",
            "RO" to "Rotate — Döndür",
            "MM" to "Mirror — Aynala",
            "AL" to "Align — Hizala",
            "TR" to "Trim/Extend — Buda / uzat",
            "OF" to "Offset — Paralel kopya",
            "DI" to "Dimension — Ölçülendirme",
            "TX" to "Text — Yazı",
            "VV / VG" to "Visibility/Graphics — Görünürlük ayarları",
            "ZF" to "Zoom to Fit — Ekrana sığdır",
            "HH" to "Hide Element — Öğeyi gizle",
            "HR" to "Reset Temporary Hide — Gizlemeyi sıfırla",
            "SD" to "Shaded — Gölgeli görünüm",
            "HL" to "Hidden Line — Gizli çizgi",
            "Tab" to "Üst üste nesnelerde sıradakine geç / zincir seç",
            "Ctrl + 1" to "Özellikler panelini aç",
            "MSPACE / PSPACE" to "Model ve kâğıt uzayı arası geçiş"
        ),

        "Mühendislik Formülleri" to listOf(
            "Merdiven altın kuralı" to "2h + b = 61 ~ 65 cm (ideal 63)\nh: rıht, b: basamak",
            "Donatı birim ağırlığı" to "d² / 162  (kg/m)\nÖrnek: Ø12 → 144/162 = 0.889 kg/m",
            "TAKS" to "Taban alanı / Parsel alanı",
            "KAKS (Emsal)" to "Toplam inşaat alanı / Parsel alanı",
            "Kiriş ön boyutlandırma" to "h = L/10 (basit) · L/12 (sürekli) · L/6 (konsol)",
            "Döşeme kalınlığı" to "Tek yönlü: L/25 · Çift yönlü: L/30 · Konsol: L/10",
            "Beton elastisite modülü" to "Ec = 3250√fck + 14000  (MPa)",
            "Eğim yüzdesi" to "(Kot farkı / Yatay mesafe) × 100",
            "Daire alanı" to "π × r²   ·   Çevre: 2πr",
            "Üçgen alanı" to "(taban × yükseklik) / 2",
            "Beton hacmi" to "En × Boy × Yükseklik  (m³)",
            "Duvar net alanı" to "Brüt alan − kapı − pencere boşlukları",
            "Kazı kabarma" to "Sıkı hacim × kabarma katsayısı\nNormal toprak: 1.25",
            "Basamak sayısı" to "Kat yüksekliği / Rıht  → tam sayı olmalı",
            "Beton ağırlığı" to "Hacim × 2.5 ton/m³",
            "Kalıp alanı (kiriş)" to "(b + 2h) × uzunluk"
        ),

        "Standart Değerler" to listOf(
            "Betonarme birim ağırlığı" to "25 kN/m³  (2.5 t/m³)",
            "Tuğla duvar birim ağırlığı" to "18 kN/m³",
            "Çelik birim ağırlığı" to "78.5 kN/m³  (7850 kg/m³)",
            "Su birim ağırlığı" to "10 kN/m³  (1 t/m³)",
            "Sıva birim ağırlığı" to "20 kN/m³",
            "Konut hareketli yükü" to "2.0 kN/m²  (balkon 4.0)",
            "Merdiven hareketli yükü" to "3.5 kN/m²",
            "Çelik elastisite modülü" to "200 000 MPa",
            "Standart kat yüksekliği" to "2.80 m net · 3.00 m brüt",
            "Oda kapısı ölçüsü" to "90 × 200 cm",
            "Giriş kapısı ölçüsü" to "100 × 210 cm",
            "Banyo/WC kapısı" to "80 × 200 cm",
            "Standart tuğla duvar" to "19 cm (yaklaşık 10 adet/m²)",
            "Dış duvar toplam" to "Sıva 2 + Tuğla 19 + Yalıtım 5 + Sıva 3 = 29 cm",
            "Normal kiriş kesiti" to "25/50 cm",
            "Az katlı konut kolonu" to "25/50 veya 30/60 cm",
            "Döşeme plak kalınlığı" to "12-15 cm (tipik)",
            "Islak hacim kot farkı" to "−5 cm (su taşmasın diye)",
            "Pencere alt/üst kotu" to "+0.90 / +2.10 m",
            "Kapı üst kotu (lento)" to "+2.10 m",
            "12 m demir çubuk" to "Piyasa standart boyu",
            "Kamyon hacmi" to "10 m³ (tipik damperli)"
        ),

        "Yönetmelik Sayıları" to listOf(
            "Taşıyıcı sistemde min beton" to "C25/30  (TBDY 2018)",
            "Min kolon boyutu" to "30 × 30 cm (deprem bölgesi)\nTS 500'de 25 × 25",
            "Min kiriş boyutu" to "Genişlik 25 cm · Yükseklik 30 cm",
            "Min perde kalınlığı" to "25 cm veya kat yüksekliği/20",
            "Kolon min donatı oranı" to "%1  (max %4)",
            "Paspayı — döşeme" to "20 mm",
            "Paspayı — kiriş/kolon" to "25 mm",
            "Paspayı — toprağa dökülen temel" to "50 mm",
            "Kenetlenme boyu (çekme)" to "40Ø  (Ø12 için 48 cm)",
            "Bindirme boyu" to "~50Ø  (kenetlenmenin 1.25 katı)",
            "Etriye sıklaştırma aralığı" to "Max 10 cm",
            "Beton kür süresi" to "En az 7 gün (soğukta 14)",
            "Ön bahçe çekme mesafesi" to "Min 5 m",
            "Yan / arka bahçe çekme" to "Min 3 m",
            "Asansör zorunluluğu" to "4 kat ve üzeri",
            "Ortak merdiven kol genişliği" to "Min 120 cm",
            "Ortak merdiven rıht" to "Max 17.5 cm",
            "Ortak merdiven basamak" to "Min 28 cm",
            "Baş kurtarma yüksekliği" to "Min 220 cm",
            "Korkuluk yüksekliği" to "Min 90 cm (kaçışta 110)",
            "Korkuluk çubuk aralığı" to "Max 12 cm (çocuk güvenliği)",
            "Konut kaçış mesafesi" to "Max 30 m",
            "Göreli kat ötelemesi" to "δ/h ≤ 0.008κ (gevrek dolgu)",
            "Güçlü kolon zayıf kiriş" to "Kolonlar kirişlerden %20 güçlü olmalı",
            "Deprem tasarım düzeyi" to "DD-2: 475 yıl tekrarlanma",
            "Bina önem katsayısı" to "BKS-1: 1.5 · BKS-2: 1.2 · BKS-3: 1.0",
            "Engelli rampası eğimi" to "Max %6  (yaya rampası %8)",
            "Metrajda boşluk indirimi" to "0.50 m²'den küçükler düşülmez"
        )
    )

    /** Toplam hazır kart sayısı. */
    val toplamKart: Int by lazy { TUMU.sumOf { it.second.size } }
}
