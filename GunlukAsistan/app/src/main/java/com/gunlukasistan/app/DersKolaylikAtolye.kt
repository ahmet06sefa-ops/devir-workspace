package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.58 — 10 Uzman Öğrenme & Kullanım Kolaylığı Modülü (saf mantık motoru).
 *
 * Kullanıcının ders çalışma, KPSS/YKS sınav hazırlığı, hafıza mühendisliği ve arayüz
 * ergonomisini en üst seviyeye taşıyan 10 interaktif modülü barındırır:
 *  1. SR-2-7-30 Aralıklı Tekrar & Leitner Kutu Sayacı ([Modul1_AralikliTekrar])
 *  2. KPSS / YKS Deneme Sınavı Net & Süre Hesaplayıcısı ([Modul2_DenemeNet])
 *  3. Tek Dokunuş "Masaya Oturdum" & Son Konuya Devam ([Modul3_HizliAksiyon])
 *  4. "5 Dakika Kuralı" Anti-Erteleme & Sabah Kurbağası ([Modul4_AntiErteleme])
 *  5. PDF Vurgu Notu & Çözümlü Soru Hata Defteri ([Modul5_HataDefteri])
 *  6. Animedoro (40m/20m) & 90m Ultradian Sayaç Şablonu ([Modul6_SprintSablonlari])
 *  7. AI Sokratik Soru İpucu & Net Tahminleyicisi ([Modul7_SokratikIpucu])
 *  8. Sanal Kütüphane Masası (Pofi) & Zinciri Kırma Takvimi ([Modul8_SanalMasa])
 *  9. Sınav Anksiyetesi 4-7-8 Nefes & Kahve-Uyku Kılavuzu ([Modul9_NefesVeKahve])
 * 10. Çevrimdışı Altın Formül Kasası & Deneme CSV Çıktısı ([Modul10_FormulVeCsv])
 */
object DersKolaylikAtolye {

    // ── 1. MODÜL: Aralıklı Tekrar & Leitner Kutusu ──
    data class TekrarKonusu(
        val konuBaslik: String = "KPSS Tarih: Osmanlı Dağılma Dönemi",
        val leitnerKutu: Int = 1,
        val calismaGunIndex: Int = 1
    )

    object Modul1_AralikliTekrar {
        fun sonrakiTekrarGunu(konu: TekrarKonusu): String {
            return when (konu.leitnerKutu) {
                1 -> "Gün 2 (+1 Gün Sonra: Kısa Tekrar)"
                2 -> "Gün 7 (+6 Gün Sonra: Orta Hafıza Pekiştirme)"
                else -> "Gün 30 (+23 Gün Sonra: Kalıcı Hafıza Testi)"
            }
        }

        fun kutuIlerle(konu: TekrarKonusu): TekrarKonusu {
            val yeniKutu = (konu.leitnerKutu + 1).coerceAtMost(3)
            return konu.copy(leitnerKutu = yeniKutu)
        }
    }

    // ── 2. MODÜL: KPSS / YKS Deneme Sınavı Net & Süre ──
    data class DenemeSonucu(
        val sinavAd: String = "KPSS Genel Kültür - Deneme 1",
        val dogru: Int = 48,
        val yanlis: Int = 12,
        val sureDk: Int = 60
    )

    object Modul2_DenemeNet {
        fun netHesapla(dogru: Int, yanlis: Int): Float {
            return (dogru - (yanlis / 4.0f)).coerceAtLeast(0f)
        }

        fun soruBasinaSaniye(toplamSoru: Int, sureDk: Int): Int {
            if (toplamSoru <= 0) return 0
            val toplamSn = sureDk * 60
            return (toplamSn / toplamSoru)
        }

        fun denemeOzeti(sonuc: DenemeSonucu): String {
            val net = netHesapla(sonuc.dogru, sonuc.yanlis)
            val toplamSoru = sonuc.dogru + sonuc.yanlis
            val saniye = soruBasinaSaniye(toplamSoru, sonuc.sureDk)
            return String.format(Locale.US, "%s · Doğru: %d · Yanlış: %d · NET: %.2f (Soru Başı: %d sn)", sonuc.sinavAd, sonuc.dogru, sonuc.yanlis, net, saniye)
        }
    }

    // ── 3. MODÜL: Tek Dokunuş "Masaya Oturdum" & Son Konuya Devam ──
    object Modul3_HizliAksiyon {
        fun masayaOturKisaYolMetni(sonKonu: String): String {
            return "⚡ TEK TIKLA BAŞLA: '$sonKonu' ➔ 25 Dakika Pomodoro Kuruldu & Masaya Oturuldu!"
        }
    }

    // ── 4. MODÜL: "5 Dakika Kuralı" Anti-Erteleme & Sabah Kurbağası ──
    data class ErtelemeKalkani(
        val besDakikaKuralAcik: Boolean = true,
        val sabahKurbagaKonu: String = "Matematik: İntegral ve Türev Soruları"
    )

    object Modul4_AntiErteleme {
        fun besDakikaMotivasyon(): String {
            return "🛡️ 5 DAKİKA KURALI: Erteleme isteği geldiğinde kendine sadece '5 dakika çalışacağım' sözü ver. Zihinsel atalet kırıldığında akışa devam edersin."
        }

        fun kurbagaKartMetni(kalkani: ErtelemeKalkani): String {
            return "🐸 GÜNÜN KURBAĞASI (Sabah En Zor Konu Önceliği): ${kalkani.sabahKurbagaKonu}"
        }
    }

    // ── 5. MODÜL: PDF Vurgu Notu & Çözümlü Soru Hata Defteri ──
    data class HataKaydi(
        val dersAd: String = "Tarih",
        val soruOzeti: String = "Lozan Antlaşması'nda Boğazlar Komisyonu",
        val ogrenilenNot: String = "Montrö'ye kadar uluslararası komisyon devam etti."
    )

    object Modul5_HataDefteri {
        fun hataKartMetni(hata: HataKaydi): String {
            return "📝 HATA DEFTERİ · [${hata.dersAd}] Soru: ${hata.soruOzeti} ➔ Doğru Bilgi: '${hata.ogrenilenNot}'"
        }
    }

    // ── 6. MODÜL: Animedoro & 90m Ultradian Sayaç ──
    object Modul6_SprintSablonlari {
        fun animedoroOzeti(odakDk: Int = 40, odulDk: Int = 20): String {
            return "⏱️ ANİMEDORO SPRINT: ${odakDk}m Yüksek Odak / ${odulDk}m Anime veya Ödül Molası (Sıkılmadan 4 saat çalışma)"
        }

        fun ultradianOzeti(odakDk: Int = 90, molaDk: Int = 20): String {
            return "⏱️ ULTRADİAN BİYO-RİTM: ${odakDk}m Derin Odak / ${molaDk}m Zihinsel Reset (Beynin doğal odaklanma döngüsü)"
        }
    }

    // ── 7. MODÜL: AI Sokratik Soru İpucu & Net Tahminleyicisi ──
    object Modul7_SokratikIpucu {
        fun sokratikIpucuUret(soruMetni: String): String {
            return "🤖 SOKRATİK KOÇ İPUCU: Doğrudan cevabı vermek yerine soruyorum -> '$soruMetni' sorusunda hangi temel bağıntı veya tarihi kronolojiyi referans almalısın? İlk basamağı nasıl sadeleştirirsin?"
        }

        fun netTahminEt(gecmisNetler: List<Float>): String {
            if (gecmisNetler.isEmpty()) return "Tahmin için henüz deneme sonucu yok."
            val ortalama = gecmisNetler.average().toFloat()
            val trendEk = if (gecmisNetler.last() > ortalama) 2.5f else 0f
            val tahmin = (ortalama + trendEk).coerceAtLeast(0f)
            return String.format(Locale.US, "📈 NET TAHMİNLEYİCİ: Mevcut ortalama %.1f NET. Sınav günkü tahmini performansınız: %.1f NET!", ortalama, tahmin)
        }
    }

    // ── 8. MODÜL: Sanal Kütüphane Masası (Pofi) & Zinciri Kırma Takvimi ──
    object Modul8_SanalMasa {
        fun pofiMasaMetni(zincirGun: Int): String {
            val alev = if (zincirGun >= 10) "🔥🔥" else "🔥"
            return "🐼 SANAL KÜTÜPHANE MASASI: Pofi şu an masada notlarını okuyor ve sana eşlik ediyor! (Aktif Zincir: $zincirGun Gün $alev)"
        }
    }

    // ── 9. MODÜL: Sınav Anksiyetesi 4-7-8 Nefes & Kahve-Uyku Kılavuzu ──
    object Modul9_NefesVeKahve {
        fun nefesRehberMetni(): String {
            return "🧘 4-7-8 ANKSİYETE YATIŞTIRICI: 4 saniye nefes al → 7 saniye tut → 8 saniye ver. Sınav öncesi ve deneme aralarında kalp atışını anında sakine çevirir."
        }

        fun kahveUyariMetni(saat: Int): String {
            return if (saat >= 17) {
                "☕ KAFEİN UYARISI (Saat 17:00+): Gece uyku kaliteniz ve REM döngünüzün zarar görmemesi için kafein alımını sonlandırın."
            } else {
                "☕ Kafein Penceresi Uygun: Zihinsel uyanıklık için kahve tüketebilirsiniz."
            }
        }
    }

    // ── 10. MODÜL: Çevrimdışı Altın Formül Kasası & Deneme CSV Çıktısı ──
    object Modul10_FormulVeCsv {
        fun altinFormulGetir(ders: String): String {
            return when (ders.trim().lowercase(Locale("tr", "TR"))) {
                "tarih" -> "🎒 ALTIN FORMÜL (Tarih): Lozan'da çözülemeyen tek konu -> 'Irak Sınırı / Musul Sorunu' (1926 Ankara Antlaşması ile çözüldü)."
                "matematik" -> "🎒 ALTIN FORMÜL (Matematik): Yol = Hız × Zaman (X = V × t) | Pisagor: a² + b² = c²"
                "turkce", "türkçe" -> "🎒 ALTIN FORMÜL (Türkçe): 'Ki' bağlacı ayrı yazılır; 'ki' ek olanleşik yazılır (SOMBAHÇEM istisnaları hariç)."
                else -> "🎒 ALTIN FORMÜL ($ders): Önemli tanımları hafızada tutmak için flaş kart kuralını kullanın."
            }
        }

        fun denemeCsvUret(sonuclar: List<DenemeSonucu>): String {
            val sb = StringBuilder()
            sb.appendLine("Sinav,Dogru,Yanlis,Net,SureDk")
            for (s in sonuclar) {
                val net = Modul2_DenemeNet.netHesapla(s.dogru, s.yanlis)
                sb.appendLine(String.format(Locale.US, "%s,%d,%d,%.2f,%d", s.sinavAd, s.dogru, s.yanlis, net, s.sureDk))
            }
            return sb.toString().trim()
        }
    }
}
