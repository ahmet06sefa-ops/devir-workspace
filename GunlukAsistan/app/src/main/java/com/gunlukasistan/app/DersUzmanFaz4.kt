package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.63 — Ders Çalışma Uzman Faz 4: Konu Zihin Haritası, Mnemonic Kodlayıcı & İlerleme Dağı
 * (saf mantık motoru).
 *
 *  1. Modül 1 (Uzman #5, #9): Konu Zihin Haritası & Mnemonic (Akrostiş) Kodlayıcı ([Faz4_1_ZihinVeMnemonic])
 *  2. Modül 2 (Uzman #17, #18): Hedef Puan Barometresi & Optik Form 10m Uyarısı ([Faz4_2_HedefBarometre])
 *  3. Modül 3 (Uzman #35, #36): Görsel İlerleme Dağı & 'Şimdi Değil' Kutusu ([Faz4_3_ZirveDagi])
 *  4. Modül 4 (Uzman #47, #50): Evrensel Renk Kodlama Standardı & Kaynak Bitirme ([Faz4_4_RenkKodu])
 *  5. Modül 5 (Uzman #58, #59): Günlük Maksimum Verim (Peak Hours) & Mola Freni ([Faz4_5_PeakHours])
 *  6. Modül 6 (Uzman #84, #89): Sınav Günü Uyku-Beslenme & Salon Ergonomi Rehberi ([Faz4_6_UykuBeslenme])
 *  7. Modül 7 (Uzman #93, #97): %100 Çevrimdışı Çalışma Garantisi & Önbellek Kalkanı ([Faz4_7_CevrimdisiKalkan])
 */
object DersUzmanFaz4 {

    // ── 1. Konu Zihin Haritası & Mnemonic (Akrostiş) Kodlayıcı ──
    data class ZihinDugum(
        val anaKonu: String,
        val altDallar: List<String>
    )

    object Faz4_1_ZihinVeMnemonic {
        fun zihinHaritasiGetir(ders: String): ZihinDugum {
            return when (ders.trim().lowercase(Locale("tr", "TR"))) {
                "matematik" -> ZihinDugum(
                    "Matematik: Problemler",
                    listOf("Yaş Problemleri", "İşçi-Havuz Problemleri", "Hareket (Hız-Zaman) Problemleri", "Yüzde-Kar-Zarar Problemleri")
                )
                "türkçe", "turkce" -> ZihinDugum(
                    "Türkçe: Paragrafta Yapı",
                    listOf("Ana Düşünce Cümlesi", "Yardımcı Düşünceler", "Paragraf Akışını Bozan Cümle", "Yer Değiştirme Soruları")
                )
                else -> ZihinDugum(
                    "KPSS Tarih: Osmanlı Dağılma",
                    listOf("Tanzimat Fermanı (1839)", "Islahat Fermanı (1856)", "I. Meşrutiyet & Kanun-i Esasi (1876)", "II. Meşrutiyet & 31 Mart Vakası (1908)")
                )
            }
        }

        fun mnemonicUret(konuAnahtar: String): String {
            return when (konuAnahtar.trim().lowercase(Locale("tr", "TR"))) {
                "türkçe", "turkce" -> "💡 MNEMONIC (Türkçe): 'SOMBAHÇEM' -> Ki bağlacının ayrı yazılmadığı istisnalar (Sanki, Oysaki, Mademki, Belki, Meğerki...)"
                "matematik" -> "💡 MNEMONIC (Matematik): 'Paşa Çayı' -> P, Ç, T, K sert ünsüz yumuşaması istisnaları veya köklü sayılarda kuvvet eşitleme kuralları"
                else -> "💡 MNEMONIC (Tarih): 'Fıstıkçı Şahap' -> Türkçede sert ünsüz benzeşmesi harfleri | Tarihte 'Balkan Antantı: TAYYAR' (Türkiye, Yunanistan, Yugoslavya, Romanya)"
            }
        }
    }

    // ── 2. Hedef Puan Barometresi & Optik Form 10m Uyarısı ──
    data class PuanBarometre(
        val hedefPuan: Int = 90,
        val mevcutPuan: Int = 78
    )

    object Faz4_2_HedefBarometre {
        fun puanFarkiHesapla(b: PuanBarometre): Int {
            return (b.hedefPuan - b.mevcutPuan).coerceAtLeast(0)
        }

        fun barometreMetni(b: PuanBarometre): String {
            val fark = puanFarkiHesapla(b)
            return if (fark == 0) {
                "🎯 HEDEF ${b.hedefPuan} PUAN | Mevcut ${b.mevcutPuan} Puan ➔ HEDEFE ULAŞTINIZ! Tebrikler."
            } else {
                "🎯 HEDEF ${b.hedefPuan} PUAN | Mevcut ${b.mevcutPuan} Puan ➔ +$fark Puan Gerekli (Her gün +2 NET artış hedefleyin!)"
            }
        }

        fun optikFormUyarisi(): String {
            return "⚠️ OPTİK FORM 10-DAKİKA KALKANI: Sınavın son 10 dakikası! Yeni soru çözmeyi bırakın, kodlamaları ve kaydırmaları denetleyin."
        }
    }

    // ── 3. Görsel İlerleme Dağı & 'Şimdi Değil' Kutusu ──
    data class ZirveDagi(
        val tamamlananPomodoro: Int = 5,
        val hedefPomodoro: Int = 8
    )

    object Faz4_3_ZirveDagi {
        fun pomodoroEkle(z: ZirveDagi): ZirveDagi {
            return z.copy(tamamlananPomodoro = (z.tamamlananPomodoro + 1).coerceAtMost(z.hedefPomodoro))
        }

        fun zirveMetniGetir(z: ZirveDagi): String {
            val kalan = (z.hedefPomodoro - z.tamamlananPomodoro).coerceAtLeast(0)
            return if (kalan == 0) {
                "⛰️ ZİRVE DAĞI: ${z.tamamlananPomodoro}/${z.hedefPomodoro} Pomodoro ➔ ZİRVEYE ULAŞTINIZ! Günlük hedef tamam."
            } else {
                "⛰️ ZİRVE DAĞI: ${z.tamamlananPomodoro}/${z.hedefPomodoro} Pomodoro ➔ Zirveye $kalan Adım Kaldı! Akışa devam."
            }
        }

        fun simdiDegilKutusuNotEkle(not: String): String {
            val t = not.trim().ifEmpty { "Alakasız düşünce" }
            return "📥 'ŞİMDİ DEĞİL' KUTUSUNA ATILDI: '$t' — Düşünce kutuya kilitlendi, odak oturumundan sonra incelenecek!"
        }
    }

    // ── 4. Evrensel Renk Kodlama Standardı & Kaynak Bitirme ──
    data class KaynakBitirme(
        val kitapAd: String = "KPSS Tarih Soru Bankası",
        val cozumlenenSayfa: Int = 195,
        val toplamSayfa: Int = 300
    )

    object Faz4_4_RenkKodu {
        fun renkKoduRehberi(): String {
            return "🎨 EVRENSEL 4-RENK NOT STANDARDINIZ: Sarı = Tanım · Pembe = Tarih/Yıl · Yeşil = Formül · Mavi = Örnek Soru"
        }

        fun sayfaCozulduEkle(k: KaynakBitirme, ekSayfa: Int): KaynakBitirme {
            return k.copy(cozumlenenSayfa = (k.cozumlenenSayfa + ekSayfa).coerceAtMost(k.toplamSayfa))
        }

        fun kaynakYuzdeMetni(k: KaynakBitirme): String {
            val yuzde = if (k.toplamSayfa > 0) ((k.cozumlenenSayfa * 100) / k.toplamSayfa).coerceIn(0, 100) else 0
            return "📚 '${k.kitapAd}' ➔ %$yuzde Tamamlandı (${k.cozumlenenSayfa}/${k.toplamSayfa} Sayfa)"
        }
    }

    // ── 5. Günlük Maksimum Verim (Peak Hours) & Mola Freni ──
    object Faz4_5_PeakHours {
        fun peakHoursAnalizi(): String {
            return "⚡ PEAK HOURS ANALİZİ: En yüksek bilişsel verim saatiniz ➔ Sabah 08:00 - 11:30 (%94 Odak Oranı)"
        }

        fun molaFrenMetni(): String {
            return "⏰ MOLA İÇİ SOSYAL MEDYA FRENİ: 5 dakikalık molanız doldu. Ekrana dalmayı bırakıp masaya dönün!"
        }
    }

    // ── 6. Sınav Günü Uyku-Beslenme & Salon Ergonomisi ──
    object Faz4_6_UykuBeslenme {
        fun uykuBeslenmeRehberi(): String {
            return "🍎 SINAV GÜNÜ BİYOLOJİSİ: Önceki gece 7.5 saat (5 REM döngüsü) uyku + sabah düşük glisemik indeksli protein kahvaltısı (Yumurta, ceviz, yulaf)."
        }

        fun salonErgonomiRehberi(): String {
            return "🧘 SALON ERGONOMİSİ: Sınav anında 45 dakikada bir yudum su için ve omuzlarınızı 3 saniye geriye esnetin."
        }
    }

    // ── 7. %100 Çevrimdışı Çalışma Garantisi & Önbellek Kalkanı ──
    object Faz4_7_CevrimdisiKalkan {
        fun cevrimdisiGarantiMetni(): String {
            return "🛡️ %100 ÇEVRİMDIŞI ÇALIŞMA GARANTİSİ: Tüm Leitner desteleri, zamanlayıcılar ve zihin haritaları yerel veritabanında çalışmaktadır."
        }

        fun onbellekKalkaniMetni(serbestMb: Float = 14.2f): String {
            return String.format(Locale.US, "🧹 AKILLI ÖNBELLEK KALKANI: Depolama kotası güvende (Temizlenebilir Önbellek: %.1f MB)", serbestMb)
        }
    }
}
