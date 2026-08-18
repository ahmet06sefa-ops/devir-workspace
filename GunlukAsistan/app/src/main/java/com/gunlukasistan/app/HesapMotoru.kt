package com.gunlukasistan.app

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * v7.30 — İnşaat mühendisliği hesap araçları.
 *
 * Tasarım kararı: bu sınıf **saf hesap** yapar, Android'e bağımlı değildir.
 * Böylece test edilebilir ve arayüzden bağımsız kalır.
 *
 * ÖNEMLİ: Buradaki hesaplar **ön boyutlandırma ve kontrol** amaçlıdır.
 * Kesin proje hesabı yerine geçmez — her sonuçta bu uyarı gösterilir.
 */
object HesapMotoru {

    /** Bir hesabın sonucu: satırlar + uyarı. */
    data class Sonuc(
        val satirlar: List<Satir>,
        val uyari: String = "",
        val basarili: Boolean = true
    )

    /** Sonuç satırı. [vurgu] true ise kalın gösterilir. */
    data class Satir(val etiket: String, val deger: String, val vurgu: Boolean = false)

    /** Hesap türü tanımı — listede göstermek için. */
    data class Arac(
        val id: String,
        val ad: String,
        val aciklama: String,
        val simge: String,
        val alanlar: List<Alan>
    )

    /** Giriş alanı. */
    data class Alan(
        val anahtar: String,
        val etiket: String,
        val birim: String = "",
        val varsayilan: String = "",
        val secenekler: List<String> = emptyList()
    )

    // ═══════════════════════════════════════════════════════════════
    // ARAÇ TANIMLARI
    // ═══════════════════════════════════════════════════════════════

    val ARACLAR: List<Arac> = listOf(
        Arac(
            "kiris", "Kiriş ön boyutlandırma",
            "Açıklığa göre kiriş yüksekliği ve genişliği", "🏗",
            listOf(
                Alan("acik", "Kiriş açıklığı", "m", "5.0"),
                Alan("tip", "Mesnet tipi", "", "Sürekli",
                    listOf("Basit mesnetli", "Sürekli", "Konsol"))
            )
        ),
        Arac(
            "kolon", "Kolon ön boyutlandırma",
            "Kat sayısı ve yüke göre kolon kesiti", "🧱",
            listOf(
                Alan("kat", "Üstteki kat sayısı", "adet", "4"),
                Alan("alan", "Kolonun taşıdığı alan", "m²", "20"),
                Alan("beton", "Beton sınıfı", "", "C25",
                    listOf("C20", "C25", "C30", "C35", "C40"))
            )
        ),
        Arac(
            "doseme", "Döşeme kalınlığı",
            "Kısa kenar ve mesnetlenmeye göre plak kalınlığı", "▭",
            listOf(
                Alan("kisa", "Kısa kenar", "m", "4.0"),
                Alan("uzun", "Uzun kenar", "m", "5.0"),
                Alan("tip", "Mesnetlenme", "", "Dört kenar",
                    listOf("Dört kenar", "Tek yön", "Konsol"))
            )
        ),
        Arac(
            "merdiven", "Merdiven hesabı",
            "2h+b kuralına göre basamak sayısı ve ölçüler", "🪜",
            listOf(
                Alan("kat", "Kat yüksekliği", "cm", "300"),
                Alan("riht", "Hedef rıht", "cm", "17.5"),
                Alan("kol", "Kol genişliği", "cm", "120")
            )
        ),
        Arac(
            "beton", "Beton ve demir metrajı",
            "Eleman hacmi, beton ve yaklaşık demir miktarı", "🪨",
            listOf(
                Alan("en", "En", "cm", "30"),
                Alan("boy", "Boy", "cm", "60"),
                Alan("uzunluk", "Uzunluk", "m", "5.0"),
                Alan("adet", "Adet", "", "1"),
                Alan("eleman", "Eleman tipi", "", "Kiriş",
                    listOf("Kolon", "Kiriş", "Döşeme", "Perde", "Temel"))
            )
        ),
        Arac(
            "duvar", "Duvar ve sıva metrajı",
            "Boşluk düşülmüş net duvar, sıva ve boya alanı", "🧱",
            listOf(
                Alan("boy", "Duvar boyu", "m", "9.0"),
                Alan("yuk", "Duvar yüksekliği", "m", "2.8"),
                Alan("kapi", "Kapı sayısı", "adet", "1"),
                Alan("pencere", "Pencere sayısı", "adet", "2")
            )
        ),
        Arac(
            "kazi", "Kazı ve dolgu",
            "Hacim, kabarma ve kamyon sayısı", "⛏",
            listOf(
                Alan("en", "En", "m", "10"),
                Alan("boy", "Boy", "m", "15"),
                Alan("derinlik", "Derinlik", "m", "2.0"),
                Alan("zemin", "Zemin cinsi", "", "Normal toprak",
                    listOf("Yumuşak toprak", "Normal toprak", "Sert toprak", "Kaya"))
            )
        ),
        Arac(
            "taks", "TAKS / KAKS kontrolü",
            "İmar haklarına göre izin verilen inşaat", "📐",
            listOf(
                Alan("parsel", "Parsel alanı", "m²", "500"),
                Alan("taks", "TAKS", "", "0.30"),
                Alan("kaks", "KAKS (Emsal)", "", "1.20"),
                Alan("mevcut", "Planlanan taban alanı", "m²", "140")
            )
        ),
        Arac(
            "demir", "Donatı ağırlığı",
            "Çap ve boya göre demir kilogramı", "➰",
            listOf(
                Alan("cap", "Donatı çapı", "mm", "12",
                    listOf("8", "10", "12", "14", "16", "18", "20", "22", "25", "32")),
                Alan("boy", "Toplam boy", "m", "100"),
                Alan("adet", "Adet", "", "1")
            )
        ),
        Arac(
            "egim", "Eğim ve kot",
            "Yüzde eğim, kot farkı ve mesafe", "📏",
            listOf(
                Alan("mesafe", "Yatay mesafe", "m", "50"),
                Alan("kot1", "Başlangıç kotu", "m", "100.00"),
                Alan("kot2", "Bitiş kotu", "m", "101.50")
            )
        ),
        Arac(
            "alan", "Alan ve hacim",
            "Dikdörtgen, üçgen, daire alanı ve hacim", "◻",
            listOf(
                Alan("sekil", "Şekil", "", "Dikdörtgen",
                    listOf("Dikdörtgen", "Üçgen", "Daire", "Yamuk")),
                Alan("a", "1. ölçü (en / taban / yarıçap)", "m", "5"),
                Alan("b", "2. ölçü (boy / yükseklik)", "m", "4"),
                Alan("h", "Derinlik (hacim için)", "m", "0")
            )
        ),
        Arac(
            "birim", "Birim çevirici",
            "Uzunluk, alan, hacim, ağırlık dönüşümü", "🔄",
            listOf(
                Alan("deger", "Değer", "", "1"),
                Alan("tip", "Dönüşüm", "", "m → cm",
                    listOf("m → cm", "cm → m", "m → mm", "mm → m",
                        "m² → cm²", "m³ → litre", "ton → kg", "kg → ton",
                        "inç → cm", "ft → m"))
            )
        )
    )

    fun arac(id: String): Arac? = ARACLAR.firstOrNull { it.id == id }

    // ═══════════════════════════════════════════════════════════════
    // HESAPLAMA
    // ═══════════════════════════════════════════════════════════════

    private fun sayi(g: Map<String, String>, k: String, v: Double = 0.0): Double =
        g[k]?.replace(",", ".")?.trim()?.toDoubleOrNull() ?: v

    private fun metin(g: Map<String, String>, k: String, v: String = ""): String =
        g[k]?.trim().orEmpty().ifBlank { v }

    private fun yuvarla(d: Double, basamak: Int = 2): String =
        String.format("%.${basamak}f", d)

    fun hesapla(id: String, g: Map<String, String>): Sonuc = try {
        when (id) {
            "kiris" -> kiris(g)
            "kolon" -> kolon(g)
            "doseme" -> doseme(g)
            "merdiven" -> merdiven(g)
            "beton" -> beton(g)
            "duvar" -> duvar(g)
            "kazi" -> kazi(g)
            "taks" -> taks(g)
            "demir" -> demir(g)
            "egim" -> egim(g)
            "alan" -> alanHacim(g)
            "birim" -> birim(g)
            else -> Sonuc(emptyList(), "Bilinmeyen hesap", false)
        }
    } catch (e: Exception) {
        Sonuc(emptyList(), "Hesap yapılamadı: ${e.message}", false)
    }

    // ─────────────────── Kiriş ───────────────────

    private fun kiris(g: Map<String, String>): Sonuc {
        val L = sayi(g, "acik", 5.0)
        if (L <= 0) return Sonuc(emptyList(), "Açıklık sıfırdan büyük olmalı", false)
        val tip = metin(g, "tip", "Sürekli")

        // Yaygın ön boyutlandırma oranları
        val bolen = when (tip) {
            "Basit mesnetli" -> 10.0
            "Konsol" -> 6.0
            else -> 12.0
        }
        val hHam = L * 100 / bolen
        val h = (ceil(hHam / 5) * 5).coerceAtLeast(30.0)   // 5 cm katına yuvarla
        val b = (ceil(h / 2 / 5) * 5).coerceAtLeast(25.0)

        return Sonuc(
            listOf(
                Satir("Açıklık", "${yuvarla(L)} m"),
                Satir("Mesnet tipi", tip),
                Satir("Oran", "L / ${bolen.toInt()}"),
                Satir("Kiriş yüksekliği (h)", "${h.toInt()} cm", true),
                Satir("Kiriş genişliği (b)", "${b.toInt()} cm", true),
                Satir("Kesit", "${b.toInt()} / ${h.toInt()}", true),
                Satir("Kalıp alanı (1 m)", "${yuvarla((b + 2 * h) / 100)} m²/m")
            ),
            "Ön boyutlandırmadır. Kesin kesit, statik hesapla belirlenir."
        )
    }

    // ─────────────────── Kolon ───────────────────

    private fun kolon(g: Map<String, String>): Sonuc {
        val kat = sayi(g, "kat", 4.0)
        val alan = sayi(g, "alan", 20.0)
        val betonAd = metin(g, "beton", "C25")
        if (kat <= 0 || alan <= 0) {
            return Sonuc(emptyList(), "Kat sayısı ve alan sıfırdan büyük olmalı", false)
        }

        // Yaklaşık yük: kat başına 12 kN/m² (ölü + hareketli)
        val yukKN = kat * alan * 12.0
        val fck = when (betonAd) {
            "C20" -> 20.0; "C30" -> 30.0; "C35" -> 35.0; "C40" -> 40.0
            else -> 25.0
        }
        // Ön boyutlandırma: N / (0.35 * fck) — güvenli tarafta
        val gerekliMm2 = yukKN * 1000 / (0.35 * fck)
        val gerekliCm2 = gerekliMm2 / 100
        val kenarHam = sqrt(gerekliCm2)
        val kenar = (ceil(kenarHam / 5) * 5).coerceAtLeast(30.0)

        return Sonuc(
            listOf(
                Satir("Üstteki kat", "${kat.toInt()} kat"),
                Satir("Taşınan alan", "${yuvarla(alan)} m²"),
                Satir("Yaklaşık yük", "${yuvarla(yukKN, 0)} kN"),
                Satir("Beton sınıfı", "$betonAd (fck = ${fck.toInt()} MPa)"),
                Satir("Gerekli kesit alanı", "${yuvarla(gerekliCm2, 0)} cm²"),
                Satir("Kare kolon", "${kenar.toInt()} / ${kenar.toInt()} cm", true),
                Satir(
                    "Dikdörtgen seçenek",
                    "${(kenar * 0.7).roundToInt() / 5 * 5 + 5} / " +
                        "${(kenar * 1.4).roundToInt() / 5 * 5} cm",
                    true
                ),
                Satir("Minimum (TS 500)", "25 / 25 cm — en az 30 cm önerilir")
            ),
            "Kaba ön boyutlandırma. Deprem yükleri ve moment hesaba katılmamıştır."
        )
    }

    // ─────────────────── Döşeme ───────────────────

    private fun doseme(g: Map<String, String>): Sonuc {
        val kisa = sayi(g, "kisa", 4.0)
        val uzun = sayi(g, "uzun", 5.0)
        val tip = metin(g, "tip", "Dört kenar")
        if (kisa <= 0) return Sonuc(emptyList(), "Kısa kenar sıfırdan büyük olmalı", false)

        val oran = if (kisa > 0) uzun / kisa else 0.0
        val calisma = when {
            tip == "Konsol" -> "Konsol"
            tip == "Tek yön" || oran > 2.0 -> "Tek yönlü"
            else -> "Çift yönlü"
        }
        val bolen = when (calisma) {
            "Konsol" -> 10.0
            "Tek yönlü" -> 25.0
            else -> 30.0
        }
        val hHam = kisa * 100 / bolen
        val h = (ceil(hHam)).coerceAtLeast(if (calisma == "Konsol") 12.0 else 10.0)

        return Sonuc(
            listOf(
                Satir("Kısa kenar", "${yuvarla(kisa)} m"),
                Satir("Uzun kenar", "${yuvarla(uzun)} m"),
                Satir("Kenar oranı", yuvarla(oran)),
                Satir("Çalışma şekli", calisma, true),
                Satir("Plak kalınlığı", "${h.toInt()} cm", true),
                Satir("Beton (1 m²)", "${yuvarla(h / 100, 3)} m³/m²"),
                Satir("Yaklaşık ağırlık", "${yuvarla(h / 100 * 25, 1)} kN/m²")
            ),
            "Oran 2'den büyükse döşeme tek yönlü çalışır. Minimum kalınlık TS 500'e göre kontrol edilmelidir."
        )
    }

    // ─────────────────── Merdiven ───────────────────

    private fun merdiven(g: Map<String, String>): Sonuc {
        val katCm = sayi(g, "kat", 300.0)
        val hedefRiht = sayi(g, "riht", 17.5)
        val kol = sayi(g, "kol", 120.0)
        if (katCm <= 0 || hedefRiht <= 0) {
            return Sonuc(emptyList(), "Kat yüksekliği ve rıht sıfırdan büyük olmalı", false)
        }

        // Basamak sayısı tam sayı olmalı
        val hamAdet = katCm / hedefRiht
        val adet = hamAdet.roundToInt().coerceAtLeast(2)
        val riht = katCm / adet
        val basamak = 63.0 - 2 * riht
        val kural = 2 * riht + basamak

        val uygun = riht in 15.0..18.5 && basamak >= 25.0
        val kolAdet = ceil(adet / 2.0).toInt()
        val kolUzunluk = (kolAdet - 1) * basamak

        return Sonuc(
            listOf(
                Satir("Kat yüksekliği", "${yuvarla(katCm, 0)} cm"),
                Satir("Basamak sayısı", "$adet adet", true),
                Satir("Rıht yüksekliği (h)", "${yuvarla(riht)} cm", true),
                Satir("Basamak genişliği (b)", "${yuvarla(basamak)} cm", true),
                Satir("2h + b", "${yuvarla(kural)} cm  ${if (uygun) "✓ uygun" else "⚠ kontrol et"}"),
                Satir("Tek kol basamak", "$kolAdet adet"),
                Satir("Kol uzunluğu", "${yuvarla(kolUzunluk, 0)} cm"),
                Satir("Sahanlık (min)", "${yuvarla(kol, 0)} cm"),
                Satir(
                    "Toplam boşluk",
                    "${yuvarla(kolUzunluk + kol, 0)} × ${yuvarla(2 * kol + 15, 0)} cm"
                )
            ),
            if (uygun) "Ölçüler yönetmeliğe uygun görünüyor. Baş kurtarma en az 220 cm olmalı."
            else "Rıht 15-18 cm, basamak en az 25 cm olmalı. Hedef rıhtı değiştirip yeniden hesapla."
        )
    }

    // ─────────────────── Beton / demir ───────────────────

    private fun beton(g: Map<String, String>): Sonuc {
        val en = sayi(g, "en", 30.0) / 100
        val boy = sayi(g, "boy", 60.0) / 100
        val uzunluk = sayi(g, "uzunluk", 5.0)
        val adet = sayi(g, "adet", 1.0).coerceAtLeast(1.0)
        val eleman = metin(g, "eleman", "Kiriş")

        val hacimBir = en * boy * uzunluk
        val hacim = hacimBir * adet

        // Eleman tipine göre yaklaşık donatı oranı (kg/m³)
        val donatiOran = when (eleman) {
            "Kolon" -> 120.0
            "Perde" -> 90.0
            "Döşeme" -> 80.0
            "Temel" -> 70.0
            else -> 110.0   // Kiriş
        }
        val demir = hacim * donatiOran
        val kalip = (2 * (en + boy) * uzunluk) * adet
        val cimento = hacim * 350   // kg, C25 için yaklaşık

        return Sonuc(
            listOf(
                Satir("Eleman", "$eleman  (${adet.toInt()} adet)"),
                Satir("Kesit", "${yuvarla(en * 100, 0)} × ${yuvarla(boy * 100, 0)} cm"),
                Satir("Birim hacim", "${yuvarla(hacimBir, 3)} m³"),
                Satir("Toplam beton", "${yuvarla(hacim, 3)} m³", true),
                Satir("Yaklaşık demir", "${yuvarla(demir, 1)} kg", true),
                Satir("Demir oranı", "${donatiOran.toInt()} kg/m³"),
                Satir("Kalıp alanı", "${yuvarla(kalip)} m²"),
                Satir("Çimento (yaklaşık)", "${yuvarla(cimento, 0)} kg"),
                Satir("Beton ağırlığı", "${yuvarla(hacim * 2.5, 2)} ton")
            ),
            "Demir miktarı ortalama orandır. Kesin değer donatı projesinden alınmalıdır."
        )
    }

    // ─────────────────── Duvar / sıva ───────────────────

    private fun duvar(g: Map<String, String>): Sonuc {
        val boy = sayi(g, "boy", 9.0)
        val yuk = sayi(g, "yuk", 2.8)
        val kapi = sayi(g, "kapi", 1.0)
        val pencere = sayi(g, "pencere", 2.0)

        val brut = boy * yuk
        val kapiAlan = kapi * 1.0 * 2.1        // 100 × 210 cm
        val pencereAlan = pencere * 1.5 * 1.4  // 150 × 140 cm
        val net = (brut - kapiAlan - pencereAlan).coerceAtLeast(0.0)

        // 19 cm tuğla: ~10 adet/m², sıva iki yüz
        val tugla = net * 10
        val siva = net * 2
        val harc = net * 0.04                  // m³

        return Sonuc(
            listOf(
                Satir("Brüt alan", "${yuvarla(brut)} m²"),
                Satir("Kapı boşluğu", "−${yuvarla(kapiAlan)} m²  (${kapi.toInt()} adet)"),
                Satir("Pencere boşluğu", "−${yuvarla(pencereAlan)} m²  (${pencere.toInt()} adet)"),
                Satir("Net duvar alanı", "${yuvarla(net)} m²", true),
                Satir("Tuğla (19 cm)", "${ceil(tugla).toInt()} adet", true),
                Satir("Sıva alanı (2 yüz)", "${yuvarla(siva)} m²", true),
                Satir("Boya alanı", "${yuvarla(siva)} m²"),
                Satir("Harç", "${yuvarla(harc, 2)} m³")
            ),
            "0.50 m²'den küçük boşluklar genelde düşülmez. Sözleşmedeki metraj kuralına bak."
        )
    }

    // ─────────────────── Kazı ───────────────────

    private fun kazi(g: Map<String, String>): Sonuc {
        val en = sayi(g, "en", 10.0)
        val boy = sayi(g, "boy", 15.0)
        val d = sayi(g, "derinlik", 2.0)
        val zemin = metin(g, "zemin", "Normal toprak")

        val hacim = en * boy * d
        val kabarma = when (zemin) {
            "Yumuşak toprak" -> 1.15
            "Sert toprak" -> 1.30
            "Kaya" -> 1.50
            else -> 1.25
        }
        val tasinacak = hacim * kabarma
        val kamyon = ceil(tasinacak / 10.0).toInt()   // 10 m³ kamyon

        return Sonuc(
            listOf(
                Satir("Kazı boyutları", "${yuvarla(en)} × ${yuvarla(boy)} × ${yuvarla(d)} m"),
                Satir("Zemin cinsi", zemin),
                Satir("Sıkı hacim", "${yuvarla(hacim)} m³", true),
                Satir("Kabarma katsayısı", yuvarla(kabarma)),
                Satir("Taşınacak hacim", "${yuvarla(tasinacak)} m³", true),
                Satir("Kamyon (10 m³)", "$kamyon sefer", true),
                Satir("Yaklaşık ağırlık", "${yuvarla(hacim * 1.8, 1)} ton")
            ),
            "Kabarma oranları yaklaşıktır. Şev ve çalışma payı eklenmemiştir."
        )
    }

    // ─────────────────── TAKS / KAKS ───────────────────

    private fun taks(g: Map<String, String>): Sonuc {
        val parsel = sayi(g, "parsel", 500.0)
        val taks = sayi(g, "taks", 0.30)
        val kaks = sayi(g, "kaks", 1.20)
        val mevcut = sayi(g, "mevcut", 140.0)
        if (parsel <= 0) return Sonuc(emptyList(), "Parsel alanı sıfırdan büyük olmalı", false)

        val izinTaban = parsel * taks
        val izinToplam = parsel * kaks
        val katSayisi = if (izinTaban > 0) izinToplam / izinTaban else 0.0
        val uygun = mevcut <= izinTaban
        val kalan = izinTaban - mevcut

        return Sonuc(
            listOf(
                Satir("Parsel alanı", "${yuvarla(parsel, 0)} m²"),
                Satir("TAKS", yuvarla(taks)),
                Satir("KAKS (Emsal)", yuvarla(kaks)),
                Satir("İzin verilen taban", "${yuvarla(izinTaban, 1)} m²", true),
                Satir("İzin verilen toplam inşaat", "${yuvarla(izinToplam, 1)} m²", true),
                Satir("Yaklaşık kat sayısı", "${yuvarla(katSayisi, 1)} kat"),
                Satir("Planlanan taban", "${yuvarla(mevcut, 1)} m²"),
                Satir(
                    "Durum",
                    if (uygun) "✓ Uygun — ${yuvarla(kalan, 1)} m² boşta"
                    else "⚠ Aşım: ${yuvarla(-kalan, 1)} m²",
                    true
                )
            ),
            "Bodrum ve çatı arası genelde emsale dahil değildir. İlgili belediyenin imar yönetmeliğine bakınız."
        )
    }

    // ─────────────────── Donatı ağırlığı ───────────────────

    private fun demir(g: Map<String, String>): Sonuc {
        val cap = sayi(g, "cap", 12.0)
        val boy = sayi(g, "boy", 100.0)
        val adet = sayi(g, "adet", 1.0).coerceAtLeast(1.0)
        if (cap <= 0) return Sonuc(emptyList(), "Çap sıfırdan büyük olmalı", false)

        // Birim ağırlık: d² / 162 (kg/m)
        val birimKg = cap * cap / 162.0
        val toplamBoy = boy * adet
        val toplamKg = birimKg * toplamBoy
        val cubuk = ceil(toplamBoy / 12.0).toInt()   // 12 m'lik çubuk

        return Sonuc(
            listOf(
                Satir("Donatı çapı", "Ø${cap.toInt()} mm"),
                Satir("Birim ağırlık", "${yuvarla(birimKg, 3)} kg/m", true),
                Satir("Toplam boy", "${yuvarla(toplamBoy, 1)} m"),
                Satir("Toplam ağırlık", "${yuvarla(toplamKg, 2)} kg", true),
                Satir("Ton olarak", "${yuvarla(toplamKg / 1000, 3)} ton"),
                Satir("12 m çubuk", "$cubuk adet"),
                Satir("Formül", "d² / 162 = ${cap.toInt()}² / 162")
            ),
            "S420 nervürlü donatı içindir. Fire payı (%3-5) eklenmemiştir."
        )
    }

    // ─────────────────── Eğim ───────────────────

    private fun egim(g: Map<String, String>): Sonuc {
        val mesafe = sayi(g, "mesafe", 50.0)
        val kot1 = sayi(g, "kot1", 100.0)
        val kot2 = sayi(g, "kot2", 101.5)
        if (mesafe <= 0) return Sonuc(emptyList(), "Mesafe sıfırdan büyük olmalı", false)

        val fark = kot2 - kot1
        val yuzde = fark / mesafe * 100
        val derece = Math.toDegrees(Math.atan(fark / mesafe))
        val egimliBoy = sqrt(mesafe * mesafe + fark * fark)

        return Sonuc(
            listOf(
                Satir("Yatay mesafe", "${yuvarla(mesafe)} m"),
                Satir("Kot farkı", "${yuvarla(fark)} m", true),
                Satir("Eğim", "%${yuvarla(yuzde)}", true),
                Satir("Açı", "${yuvarla(derece)}°"),
                Satir("Eğimli uzunluk", "${yuvarla(egimliBoy)} m"),
                Satir("Oran", "1 / ${if (fark != 0.0) yuvarla(mesafe / Math.abs(fark), 1) else "—"}"),
                Satir("Yön", if (fark > 0) "Yokuş yukarı" else if (fark < 0) "Yokuş aşağı" else "Düz")
            ),
            "Yaya rampası en fazla %8, engelli rampası en fazla %6 olmalıdır."
        )
    }

    // ─────────────────── Alan / hacim ───────────────────

    private fun alanHacim(g: Map<String, String>): Sonuc {
        val sekil = metin(g, "sekil", "Dikdörtgen")
        val a = sayi(g, "a", 5.0)
        val b = sayi(g, "b", 4.0)
        val h = sayi(g, "h", 0.0)

        val (alan, cevre) = when (sekil) {
            "Üçgen" -> (a * b / 2) to 0.0
            "Daire" -> (Math.PI * a * a) to (2 * Math.PI * a)
            "Yamuk" -> ((a + b) / 2 * (if (h > 0) h else 1.0)) to 0.0
            else -> (a * b) to (2 * (a + b))
        }

        val satirlar = mutableListOf(
            Satir("Şekil", sekil),
            Satir("Alan", "${yuvarla(alan)} m²", true)
        )
        if (cevre > 0) satirlar.add(Satir("Çevre", "${yuvarla(cevre)} m"))
        if (h > 0 && sekil != "Yamuk") {
            satirlar.add(Satir("Hacim", "${yuvarla(alan * h, 3)} m³", true))
            satirlar.add(Satir("Litre", "${yuvarla(alan * h * 1000, 0)} L"))
        }
        satirlar.add(Satir("cm² olarak", "${yuvarla(alan * 10000, 0)} cm²"))

        return Sonuc(satirlar, "Daire için 1. ölçü yarıçaptır.")
    }

    // ─────────────────── Birim çevirici ───────────────────

    private fun birim(g: Map<String, String>): Sonuc {
        val d = sayi(g, "deger", 1.0)
        val tip = metin(g, "tip", "m → cm")

        val (sonuc, birimAd) = when (tip) {
            "m → cm" -> (d * 100) to "cm"
            "cm → m" -> (d / 100) to "m"
            "m → mm" -> (d * 1000) to "mm"
            "mm → m" -> (d / 1000) to "m"
            "m² → cm²" -> (d * 10000) to "cm²"
            "m³ → litre" -> (d * 1000) to "litre"
            "ton → kg" -> (d * 1000) to "kg"
            "kg → ton" -> (d / 1000) to "ton"
            "inç → cm" -> (d * 2.54) to "cm"
            "ft → m" -> (d * 0.3048) to "m"
            else -> d to "?"
        }

        return Sonuc(
            listOf(
                Satir("Girilen", "${yuvarla(d, 4).trimEnd('0').trimEnd('.')} ${tip.substringBefore(" →")}"),
                Satir("Sonuç", "${yuvarla(sonuc, 4).trimEnd('0').trimEnd('.')} $birimAd", true)
            )
        )
    }
}
