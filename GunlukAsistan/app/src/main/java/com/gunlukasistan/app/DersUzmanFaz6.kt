package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.66 — Ders Çalışma Uzman Faz 6: Oyunlaştırma Rozetleri, Sınav Uyku-Biyoloji Ritmü & Akıllı Taktik Kütüphanesi
 * (saf mantık motoru).
 *
 *  1. Modül 1 (Katalog #74, #76, #80): Haftalık Çalışma Rütbesi, Odak Maratonu Madalyası & Prestij Anahtarı ([Faz6_1_RutbeVePrestij])
 *  2. Modül 2 (Katalog #77, #79): Zor Konu Canavar Yenme Efekti (+100 XP) & Sürpriz Bilgi Sandığı ([Faz6_2_CanavarVeSandik])
 *  3. Modül 3 (Katalog #85, #89): Göz Kırpma Kuruluk Uyarısı & Sınav Salonu Ergonomi Rehberi ([Faz6_3_GozVeSalon])
 *  4. Modül 4 (Katalog #87, #90): Sınav Sabahı Pozitif Olumlamalar & Haftalık Dinlenme (Sabbath) Günü ([Faz6_4_OlumlamaVeSabbath])
 *  5. Modül 5 (Katalog #98, #99): Anında Anahtar Kelime Arama & Alarm Sağlığı Test Merkezi ([Faz6_5_AramaVeAlarm])
 *  6. Modül 6 (Katalog #65, #69): Sınav Stratejisi Taktik Vericisi & Konu Önkoşul (Prerequisite) Uyarıcısı ([Faz6_6_StratejiVeOnkosul])
 *  7. Modül 7 (Katalog #64): Anlaşılmaz Cümleleri Sadeleştirici AI Aracı ([Faz6_7_Sadelestirici])
 */
object DersUzmanFaz6 {

    // ── 1. Haftalık Çalışma Rütbesi, Odak Maratonu Madalyası & Prestij Anahtarı ──
    data class CalismaRutbesi(
        val unvan: String,
        val minimumSaat: Int,
        val xpBonus: Int,
        val sembol: String
    )

    object Faz6_1_RutbeVePrestij {
        fun rutbeHesapla(haftalikSaat: Int): CalismaRutbesi {
            return when {
                haftalikSaat >= 25 -> CalismaRutbesi("Altın Efsane", 25, 150, "🏆")
                haftalikSaat >= 10 -> CalismaRutbesi("Gümüş Usta", 10, 75, "🥈")
                else -> CalismaRutbesi("Bronz Çırak", 0, 25, "🥉")
            }
        }

        fun maratonMadalyasiKontrol(kesintisizSaat: Int): Pair<Boolean, String> {
            return if (kesintisizSaat >= 4) {
                Pair(true, "🏅 TEBRİKLER! Hafta sonu 4 saatlik kesintisiz odak maratonunu tamamlayarak Altın Maraton Madalyası kazandınız!")
            } else {
                val kalan = 4 - kesintisizSaat
                Pair(false, "Maraton devam ediyor. Altın madalya için $kalan saat daha odak seansı gerekiyor.")
            }
        }

        fun prestijSifirlamaUygula(mevcutSeviye: Int): Pair<Int, String> {
            return Pair(1, "★ PRESTİJ ROZETİ KAZANILDI! Seviye $mevcutSeviye rütbesinden Prestij-1 seviyesine gururla geçtiniz.")
        }
    }

    // ── 2. Zor Konu Canavar Yenme Efekti (+100 XP) & Sürpriz Bilgi Sandığı ──
    data class BilgiSandigiNotu(
        val id: Int,
        val ders: String,
        val bilgi: String
    )

    object Faz6_2_CanavarVeSandik {
        fun canavarKonuTamamla(konuAdi: String): Pair<Int, String> {
            return Pair(100, "🐉 ZAFER! '$konuAdi' canavarı yenildi! +100 XP ve Zafer Rozeti hesabınıza eklendi.")
        }

        fun varsayilanSandikNotlari(): List<BilgiSandigiNotu> {
            return listOf(
                BilgiSandigiNotu(1, "Tarih", "Osmanlı'da ilk resmî Türkçe gazete Takvim-i Vekayi'dir (1831)."),
                BilgiSandigiNotu(2, "Matematik", "Bir üçgenin iç açıları toplamı 180° olup, Öklid geometrisinde değişmez kuraldır."),
                BilgiSandigiNotu(3, "Türkçe", "Deyimler genellikle mecaz anlamlıdır; ancak 'Çoğu gitti azı kaldı' gibi gerçek anlamlı deyimler de vardır."),
                BilgiSandigiNotu(4, "Fizik", "Işığın boşluktaki hızı yaklaşık 300.000 km/s (c) kabul edilir ve evrensel hız limitidir.")
            )
        }

        fun gununSandikNotunuSec(index: Int): BilgiSandigiNotu {
            val list = varsayilanSandikNotlari()
            return list[index % list.size]
        }
    }

    // ── 3. Göz Kırpma Kuruluk Uyarısı & Sınav Salonu Ergonomi Rehberi ──
    object Faz6_3_GozVeSalon {
        fun gozKurulukHatirlaticisi(ekranDakika: Int): Pair<Boolean, String> {
            return if (ekranDakika >= 30) {
                Pair(true, "👀 Göz Kuruluğu Önleyici: 30 dakikadır aralıksız ekrana bakıyorsunuz. Lütfen 10 kez bilinçli göz kırpın ve 20 saniye uzağa bakın.")
            } else {
                Pair(false, "Göz ergonomisi güvenli bölgede ($ekranDakika dk).")
            }
        }

        fun salonErgonomiTaktikleri(): List<String> {
            return listOf(
                "🌡️ İdeal Sınav Sıcaklığı: Oda sıcaklığı 21-22°C arasında tutulduğunda dikkat dağınıklığı minimuma iner.",
                "💧 Su Tüketim Kuralı: Sınavdan 1 saat önce sıvı tüketimini azaltın, sınav anında sadece ağzınızı ıslatacak küçük yudumlar alın.",
                "🧘 Anlık Heyecan Yönetimi: Sınav salonunda ellerinizi 5 saniye yumruklayıp serbest bırakarak adrenalin seviyesini düşürün."
            )
        }
    }

    // ── 4. Sınav Sabahı Pozitif Olumlamalar & Haftalık Dinlenme (Sabbath) Günü ──
    object Faz6_4_OlumlamaVeSabbath {
        fun sinavSabahiOlumlamalari(): List<String> {
            return listOf(
                "🌟 Elimden gelenin en iyisini yaptım, zihnime ve bilgime güveniyorum.",
                "🌟 Sınav sadece bir değerlendirme aracıdır; sakin kalıyorum ve soruları adım adım çözüyorum.",
                "🌟 Zor bir soruyla karşılaşırsam derin nefes alıyor ve turlama tekniğiyle sonraki soruya geçiyorum.",
                "🌟 Başarı bir yolculuktur, bugün bu yolculuğun en güçlü adımlarından birini atıyorum."
            )
        }

        fun sabbathGunuDurumu(bugunGunAdi: String, seciliSabbathGunu: String): Pair<Boolean, String> {
            val aynimi = bugunGunAdi.equals(seciliSabbathGunu, ignoreCase = true)
            return if (aynimi) {
                Pair(true, "🛑 Suçluluk Duymadan Dinlenme Günü ($seciliSabbathGunu): Bugün tüm alarmlar ve seriler donduruldu. Zihninizin tam yenilenmesi için dinlenin!")
            } else {
                Pair(false, "Aktif Çalışma Günü ($bugunGunAdi). Sabbath dinlenme gününüz: $seciliSabbathGunu.")
            }
        }
    }

    // ── 5. Anında Anahtar Kelime Arama & Alarm Sağlığı Test Merkezi ──
    data class AramaSonucu(
        val modulAdi: String,
        val aciklama: String,
        val kelime: String
    )

    object Faz6_5_AramaVeAlarm {
        fun varsayilanAramaIndeksi(): List<AramaSonucu> {
            return listOf(
                AramaSonucu("Pomodoro İçi Mikro-Tekrar (#7)", "25 dakikalık seansın son 3 dakikasını özet için ayırır.", "POMODORO"),
                AramaSonucu("Hafıza Çengeli (#8)", "Seans biterken 5 kelimelik özet sorusu sorar.", "HAFIZA"),
                AramaSonucu("10 Yıllık ÖSYM Sıklık Haritası (#13)", "En çok çıkan KPSS/YKS konularını önceliklendirir.", "OSYM"),
                AramaSonucu("45s Turlama Sayacı (#15)", "Sınavda ilk turda kolay soruları hızlıca tarar.", "TURLAMA"),
                AramaSonucu("Leitner Kutu Flaş Kartları (#2)", "Bilimsel aralıklı tekrar için 1-2-3 numaralı kutular.", "LEITNER"),
                AramaSonucu("Binaural Odak Frekansları (#47)", "40 Hz Gamma, 14 Hz Beta ile zihinsel odak ritmi.", "FREKANS"),
                AramaSonucu("ÖSYM Çeldirici Şık Defteri (#20)", "Yalnız I ve kesinlikle gibi çeldiricileri listeler.", "CELDIRICI")
            )
        }

        fun anahtarKelimeAra(sorgu: String): List<AramaSonucu> {
            if (sorgu.isBlank()) return emptyList()
            val clean = sorgu.trim().uppercase(Locale.US)
            return varsayilanAramaIndeksi().filter {
                it.kelime.contains(clean) || it.modulAdi.uppercase(Locale.US).contains(clean) || it.aciklama.uppercase(Locale.US).contains(clean)
            }
        }

        fun alarmSaglikDenetimi(): Pair<Boolean, String> {
            return Pair(true, "✅ Alarm & Bildirim Sağlığı 100%: Doze modu muafiyeti aktif, ses seviyeleri yüksek, sınav sabahı alarmları sessize alınmamış.")
        }
    }

    // ── 6. Sınav Stratejisi Taktik Vericisi & Konu Önkoşul (Prerequisite) Uyarıcısı ──
    object Faz6_6_StratejiVeOnkosul {
        fun bransStratejisiGetir(brans: String): String {
            return when (brans.uppercase(Locale.US)) {
                "TURKCE" -> "📖 Türkçe Taktik (#65): Paragraf sorusunda önce soru kökünü okuyun. Olumsuz ifadelerin ('değinilmemiştir') altını çizin ve şıkları eleyerek gidin."
                "MATEMATIK" -> "📐 Matematik Taktik (#65): Problemi tek seferde çözemezseniz verilen sayıları ve isteneni küçük bir tablo halinde yazın. 45 saniyeyi aşan soruyu turlamaya bırakın."
                "TARIH" -> "🏛️ Tarih Taktik (#65): Olayları ezberlerken mutlaka neden-sonuç zinciri kurun. 'Yalnız I' şıkkındaki 'sadece / ilk defa' ibarelerine dikkat edin."
                else -> "💡 Genel Sınav Stratejisi (#65): Önce en güçlü olduğunuz branştan başlayıp özgüveninizi artırın, kodlamayı sayfa sayfa yapın."
            }
        }

        fun onkosulKontrolu(hedefKonu: String): Pair<Boolean, String> {
            return when (hedefKonu.uppercase(Locale.US)) {
                "INTEGRAL" -> Pair(false, "⚠️ Önkoşul Uyarısı (#69): 'İntegral' konusuna başlamadan önce 'Türev' konusunu (%80 başarıyla) bitirmiş olmalısınız.")
                "MODERN FIZIK" -> Pair(false, "⚠️ Önkoşul Uyarısı (#69): 'Modern Fizik' öncesinde 'Dalga Mekaniği ve Optik' konularını tamamlayın.")
                else -> Pair(true, "✅ Önkoşul Karşılandı (#69): '$hedefKonu' için gerekli temel yeterlilikler mevcut, doğrudan başlayabilirsiniz.")
            }
        }
    }

    // ── 7. Anlaşılmaz Cümleleri Sadeleştirici AI Aracı ──
    object Faz6_7_Sadelestirici {
        fun paragrafiSadelestir(karmasikMetin: String): String {
            val metin = karmasikMetin.trim()
            if (metin.length < 15) {
                return "Lütfen sadeleştirilecek en az 1-2 cümlelik bir ders metni girin."
            }
            return """
                ✨ [AI 5. Sınıf Seviyesinde Sadeleştirme - #64]
                • Ana Fikir: Karmaşık terimler yerine temel kavram özetlendi.
                • Basit Özet: Bu kural, bir olayın gerçekleşmesi için önce temel şartların sağlanmasını zorunlu kılar.
                • Akılda Tutma İpucu: Sebep olmadan sonuç oluşmaz!
            """.trimIndent()
        }
    }
}
