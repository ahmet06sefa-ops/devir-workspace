package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.64 — Ders Çalışma Uzman Faz 5: İleri Sınav Simülasyonu, Otonom Koçluk & Konu Denetim Merkezi
 * (saf mantık motoru).
 *
 *  1. Modül 1 (Uzman #7, #8): Pomodoro İçi Mikro-Tekrar Penceresi & Hafıza Çengeli ([Faz5_1_PomodoroCengel])
 *  2. Modül 2 (Uzman #10): Haftalık Bilişsel Konsolidasyon (Hafıza Birleştirme) Raporu ([Faz5_2_BiliselKonsolidasyon])
 *  3. Modül 3 (Uzman #20, #38): ÖSYM Çeldirici Şık Defteri & Masa Öncesi Ritüel Check-List ([Faz5_3_CeldiriciVeRituel])
 *  4. Modül 4 (Uzman #39, #40): Kişisel Motivasyon Çapası & Erteleme Serisi Uyarıcısı ([Faz5_4_MotivasyonVeErteleme])
 *  5. Modül 5 (Uzman #44, #49): Akıllı PDF İçindekiler (TOC) Atlayıcı & Yanlış Kes-Yapıştır Panosu ([Faz5_5_PdfTocVeYanlisPano])
 *  6. Modül 6 (Uzman #53, #55, #56): 50-10 Maraton Sprinti, Masaya Davet 15s & Serbest Akış Kronometresi ([Faz5_6_MaratonVeSerbestSayac])
 *  7. Modül 7 (Uzman #60, #62, #68, #70): Haftalık Odak Hedef Metresi, AI Eksik Müfettişi, Otomatik Quiz & AI Koç Kişilikleri ([Faz5_7_AiKocVeOtomatikQuiz])
 */
object DersUzmanFaz5 {

    // ── 1. Pomodoro İçi Mikro-Tekrar Penceresi & Hafıza Çengeli ──
    data class CengelSoru(
        val id: Int,
        val soruMetni: String,
        val ipucu: String,
        val cevapMetni: String
    )

    object Faz5_1_PomodoroCengel {
        fun mikroTekrarSuresiHesapla(toplamDakika: Int): Int {
            if (toplamDakika <= 15) return 2
            if (toplamDakika <= 30) return 3
            return 5
        }

        fun varsayilanCengelSorulari(): List<CengelSoru> {
            return listOf(
                CengelSoru(1, "Bu oturumun en önemli cümlesini 5 kelimeyle özetle", "Kitaba bakmadan hatırla", "Ana fikir kavrandı."),
                CengelSoru(2, "Az önce öğrendiğin formül veya tarih nedir?", "En çok çıkan detay", "Anahtar formül not edildi."),
                CengelSoru(3, "Bu konu sınava nasıl bir soru tarzıyla gelebilir?", "ÖSYM soru kökü", "Çeldirici nokta fark edildi.")
            )
        }

        fun cengelKontroluTamamla(soruId: Int, kullaniciCevabi: String): Pair<Boolean, String> {
            val temiz = kullaniciCevabi.trim()
            if (temiz.length < 5) {
                return Pair(false, "Lütfen en az 5 kelimelik veya anlamlı bir açıklama yazın.")
            }
            return Pair(true, "Hafıza Çengeli mühürlendi! Bilgi kalıcı hafıza yoluna girdi (+15 XP).")
        }
    }

    // ── 2. Haftalık Bilişsel Konsolidasyon (Hafıza Birleştirme) Raporu ──
    data class KonsolidasyonKaydi(
        val dersAdi: String,
        val konuAdi: String,
        val tekrarSayisi: Int,
        val kaliciHafizadaMi: Boolean
    )

    object Faz5_2_BiliselKonsolidasyon {
        fun varsayilanHaftalikKayitlar(): List<KonsolidasyonKaydi> {
            return listOf(
                KonsolidasyonKaydi("Tarih", "Osmanlı Dağılma Dönemi", 3, true),
                KonsolidasyonKaydi("Matematik", "Türev Bileşke Fonksiyon", 2, true),
                KonsolidasyonKaydi("Türkçe", "Paragrafta Yapı ve Akış", 1, false),
                KonsolidasyonKaydi("Fizik", "Newton'un Hareket Yasaları", 3, true),
                KonsolidasyonKaydi("Kimya", "Periyodik Sistem Trendleri", 1, false)
            )
        }

        fun konsolidasyonSkoruHesapla(kayitlar: List<KonsolidasyonKaydi>): Pair<Int, String> {
            if (kayitlar.isEmpty()) return Pair(0, "Henüz haftalık konu girişi yapılmadı.")
            val basarili = kayitlar.count { it.kaliciHafizadaMi || it.tekrarSayisi >= 2 }
            val yuzde = (basarili * 100) / kayitlar.size
            val yorum = when {
                yuzde >= 80 -> "Mükemmel! Bu hafta çalışılan konuların %$yuzde oranı kalıcı hafızaya geçti."
                yuzde >= 50 -> "İyi düzeyde. Konuların %$yuzde oranı pekişti, zayıf konulara 1 kısa tekrar ekleyin."
                else -> "Kritik Seviye: %$yuzde. Yeni konu yerine önceki konuları tekrar etmeniz önerilir."
            }
            return Pair(yuzde, yorum)
        }
    }

    // ── 3. ÖSYM Çeldirici Şık Defteri & Masa Öncesi Ritüel Check-List ──
    data class CeldiriciNot(
        val ders: String,
        val tuzakIfade: String,
        val gercekBilgi: String,
        val tehlikePuan: Int // 1-5 arası
    )

    object Faz5_3_CeldiriciVeRituel {
        fun varsayilanCeldiriciler(): List<CeldiriciNot> {
            return listOf(
                CeldiriciNot("Tarih", "'Yalnız I' şıkkındaki 'sadece / kesinlikle' ifadeleri", "Osmanlı ıslahatlarında Avrupa etkisi Lale Devri ile başlar, gerileme ile değil.", 5),
                CeldiriciNot("Türkçe", "'Değinilmemiştir / Ulaşılamaz' olumsuz soru kökleri", "Önce soru kökü oku, şıkları tara, sonra paragrafı eleyerek oku.", 4),
                CeldiriciNot("Matematik", "Pozitif tamsayı / Doğal sayı tanımı ayrımı", "0 (sıfır) doğal sayıdır ama pozitif veya negatif değildir.", 5)
            )
        }

        fun rituelAdimlar(): List<String> {
            return listOf(
                "1. Çalışma masasını topla ve gereksiz eşyaları kaldır",
                "2. Yanına 1 bardak su veya bitki çayını hazırla",
                "3. Telefonunu ters çevir ve bildirimleri sessize al",
                "4. 3 kez derin nefes alıp zihnini sınava odakla"
            )
        }

        fun rituelDurumuSorgula(tamamlananAdimSayisi: Int, toplamAdim: Int): Pair<Boolean, String> {
            if (tamamlananAdimSayisi >= toplamAdim) {
                return Pair(true, "🌟 Tüm ritüel adımları tamamlandı! Zihinsel olarak masaya 100% hazırsınız.")
            }
            val kalan = toplamAdim - tamamlananAdimSayisi
            return Pair(false, "Hazırlık devam ediyor. Kalan $kalan adımı tamamlayınca maratona başlayın.")
        }
    }

    // ── 4. Kişisel Motivasyon Çapası & Erteleme Serisi Uyarıcısı ──
    data class MotivasyonCapasi(
        val hedefBaslik: String,
        val hedefPuan: Int,
        val kisiselSlogan: String
    )

    data class ErtelenenGorev(
        val gorevAdi: String,
        val ertelemeGunSayisi: Int
    )

    object Faz5_4_MotivasyonVeErteleme {
        fun varsayilanCapa(): MotivasyonCapasi {
            return MotivasyonCapasi(
                hedefBaslik = "Hukuk Fakültesi / Atanmış Kamu Personeli",
                hedefPuan = 465,
                kisiselSlogan = "Bugün döktüğüm ter, yarınki bağımsızlığımın teminatıdır!"
            )
        }

        fun ertelemeAnalizi(gorev: ErtelenenGorev): Pair<Boolean, List<String>> {
            if (gorev.ertelemeGunSayisi < 3) {
                return Pair(false, listOf("Görev normal takviminde ilerliyor."))
            }
            val altGorevler = listOf(
                "Adım 1 (5 Dk): Sadece kaynağı aç ve ilk 3 sayfaya göz at.",
                "Adım 2 (15 Dk): Konunun en temel 5 sorusunu çöz.",
                "Adım 3 (10 Dk): Özet notu çıkarıp görevin tikini işaretle."
            )
            return Pair(true, altGorevler)
        }
    }

    // ── 5. Akıllı PDF İçindekiler (TOC) Atlayıcı & Yanlış Kes-Yapıştır Panosu ──
    data class TocBolum(
        val bolumNo: Int,
        val baslik: String,
        val sayfaNo: Int
    )

    data class KesYapistirYanlis(
        val soruId: Int,
        val ders: String,
        val hataTuru: String,
        val ozet: String,
        val cozulduMu: Boolean
    )

    object Faz5_5_PdfTocVeYanlisPano {
        fun varsayilanTocListesi(): List<TocBolum> {
            return listOf(
                TocBolum(1, "Bölüm 1: Sayılar ve Temel Kavramlar", 12),
                TocBolum(2, "Bölüm 2: Rasyonel ve Üslü Sayılar", 48),
                TocBolum(3, "Bölüm 3: Problemler ve Denklem Kurma", 96),
                TocBolum(4, "Bölüm 4: Fonksiyonlar ve Polinomlar", 154),
                TocBolum(5, "Bölüm 5: Olasılık ve Kombinasyon", 210)
            )
        }

        fun sayfaAtlamaHesapla(mevcutSayfa: Int, hedefSayfa: Int): String {
            val fark = hedefSayfa - mevcutSayfa
            return when {
                fark == 0 -> "Şu an zaten bu bölümün ilk sayfasındasınız (s.$hedefSayfa)."
                fark > 0 -> "İleriye doğru +$fark sayfa atlanıyor -> s.$hedefSayfa bölümüne ulaşıldı."
                else -> "Geriye doğru ${fark} sayfa atlanıyor -> s.$hedefSayfa bölümüne dönüldü."
            }
        }

        fun varsayilanYanlisPanosu(): List<KesYapistirYanlis> {
            return listOf(
                KesYapistirYanlis(101, "Matematik", "Dikkat Hatası", "İşlem hatasından eksiyi artı aldım", true),
                KesYapistirYanlis(102, "Tarih", "Bilgi Eksikliği", "Balkan Antantı üyelerini karıştırdım", false),
                KesYapistirYanlis(103, "Türkçe", "Zaman Yetmedi", "Paragrafın son cümlesini eksik okudum", false)
            )
        }
    }

    // ── 6. 50-10 Maraton Sprinti, Masaya Davet 15s & Serbest Akış Kronometresi ──
    object Faz5_6_MaratonVeSerbestSayac {
        fun maratonDayaniklilikPuan(sprintDakika: Int, tamamlananSprintSayisi: Int): Int {
            return (sprintDakika * tamamlananSprintSayisi * 15) / 10
        }

        fun masayaDavetMesaji(kalanSaniye: Int): String {
            return if (kalanSaniye > 0) {
                "⏳ Mola bitiyor! Masaya geçmek için son $kalanSaniye saniye..."
            } else {
                "🔔 Süre doldu! Mola sona erdi, yeni odak seansı için masaya davetlisiniz!"
            }
        }

        fun serbestSayacFormatla(toplamSaniye: Int): String {
            val saat = toplamSaniye / 3600
            val dakika = (toplamSaniye % 3600) / 60
            val saniye = toplamSaniye % 60
            return String.format(Locale.US, "%02d:%02d:%02d", saat, dakika, saniye)
        }
    }

    // ── 7. Haftalık Odak Hedef Metresi, AI Eksik Müfettişi, Otomatik Quiz & AI Koç Kişilikleri ──
    data class OdakHedefMetre(
        val hedefSaat: Int,
        val gerceklesenSaat: Double
    )

    data class RemedialQuizSoru(
        val id: Int,
        val soruMetni: String,
        val dogruCevap: String
    )

    object Faz5_7_AiKocVeOtomatikQuiz {
        fun haftalıkHedefDurumu(metre: OdakHedefMetre): Pair<Int, String> {
            val yuzde = ((metre.gerceklesenSaat / metre.hedefSaat) * 100).toInt().coerceIn(0, 100)
            val yorum = when {
                yuzde >= 100 -> "🚀 Tebrikler! Haftalık ${metre.hedefSaat} saatlik hedef %$yuzde ile tamamlandı!"
                yuzde >= 70 -> "İyi tempoda gidiyorsunuz (%$yuzde). Kalan hedefe ulaşmak için günde 1.5 saat yeterli."
                else -> "Tempo artırılmalı (%$yuzde). Haftalık hedefin gerisindesiniz."
            }
            return Pair(yuzde, yorum)
        }

        fun aiEksikDersMufettisi(dersDakikalar: Map<String, Int>): List<String> {
            if (dersDakikalar.isEmpty()) return listOf("Henüz ders çalışma verisi bulunmuyor.")
            val toplam = dersDakikalar.values.sum().coerceAtLeast(1)
            val uyarilar = mutableListOf<String>()
            for ((ders, dk) in dersDakikalar) {
                val pay = (dk * 100) / toplam
                if (pay < 10) {
                    uyarilar.add("⚠️ $ders dersi bu hafta sadece %$pay ($dk dk) çalışılmış. Denge için 45 dk ekleyin.")
                }
            }
            if (uyarilar.isEmpty()) {
                uyarilar.add("✅ Tüm dersler dengeli dağıtılmış! Eksik veya ihmal edilmiş branş yok.")
            }
            return uyarilar
        }

        fun varsayilanOtomatikQuiz(): List<RemedialQuizSoru> {
            return listOf(
                RemedialQuizSoru(1, "Osmanlı'da ilk denk bütçeyi kim hazırlamıştır?", "Tarhuncu Ahmet Paşa"),
                RemedialQuizSoru(2, "f(x) = 3x² + 4x fonksiyonunun türevi nedir?", "6x + 4"),
                RemedialQuizSoru(3, "0 sayısı pozitif mi, negatif mi, yoksa işaretsiz midir?", "İşaretsiz (Nötr)"),
                RemedialQuizSoru(4, "Paragraf sorusunda ilk okunduğunda neye bakılmalıdır?", "Soru köküne"),
                RemedialQuizSoru(5, "Newton'un 2. Hareket Yasası formülü nedir?", "F = m.a")
            )
        }

        fun quizPuanla(dogruSayisi: Int, toplamSoru: Int): Pair<Int, String> {
            val puan = (dogruSayisi * 100) / (toplamSoru.coerceAtLeast(1))
            val mesaj = when {
                puan == 100 -> "🏆 Mükemmel! Hatalı soruların tamamını öğrenmişsiniz."
                puan >= 60 -> "👏 Başarılı. Eksiklerin büyük bölümü kapatıldı ($dogruSayisi/$toplamSoru)."
                else -> "💡 Tekrar önerilir. Soruların çözümlerine tekrar göz atın ($dogruSayisi/$toplamSoru)."
            }
            return Pair(puan, mesaj)
        }

        fun aiKocYanitiAl(mod: String, kullaniciMesaji: String): String {
            return when (mod.uppercase(Locale.US)) {
                "SERT" -> "🔥 [Sert Öğretmen] Bahane yok! '$kullaniciMesaji' demeyi bırak, kronometreyi aç ve hemen 25 dakika masadan kalkma!"
                "SEFKATLI" -> "💖 [Şefkatli Mentor] '$kullaniciMesaji' hissetmen çok doğal. Bugün kendimizi yormadan sadece 15 dakika küçük bir adımla başlayalım mı?"
                else -> "🦉 [Sokratik Bilge] '$kullaniciMesaji' dediğinde seni engelleyen asıl neden ne? Bu engeli aşmak için kendine hangi küçük sözü verebilirsin?"
            }
        }
    }
}
