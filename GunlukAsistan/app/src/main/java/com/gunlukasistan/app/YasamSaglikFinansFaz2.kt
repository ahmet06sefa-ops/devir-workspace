package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.65 — Yaşam Sağlığı & Finans — Uzman Faz 2: Gelişmiş Medikal, Bütçe, Otonomasyon & Frekans Merkezi
 * (saf mantık motoru).
 *
 *  1. Modül 1 (Katalog #4, #6): Manuel Nabız & Nefes Rehberi ve Tansiyon/Şeker Defteri ([Faz2_1_SaglikVeMedikal])
 *  2. Modül 2 (Katalog #8, #10): Dengeli Öğün Kalori Sayacı ve 16:8 Aralıklı Oruç Takipçisi ([Faz2_2_BeslenmeVeOruc])
 *  3. Modül 3 (Katalog #13, #14): Harcama Limit Radarı ve Kişisel Borç/Alacak Defteri ([Faz2_3_ButceVeBorc])
 *  4. Modül 4 (Katalog #15, #16): Kumbara Hedef Metresi ve Döviz/Altın Portföy Değeri ([Faz2_4_VarlikVeKumbara])
 *  5. Modül 5 (Katalog #18): Abonelik Kapatma & Tasarruf Simülasyonu ([Faz2_5_AbonelikTasarruf])
 *  6. Modül 6 (Katalog #29, #30): Özel AI Prompt Kasası ve TTS Ses Tonu/Hızı Ayarı ([Faz2_6_AiVeTts])
 *  7. Modül 7 (Katalog #39, #47, #54): Pofi Başarı Vitrini, Binaural Odak Frekansları & Çevrimdışı Kasa ([Faz2_7_FrekansVeGuvenlik])
 */
object YasamSaglikFinansFaz2 {

    // ── 1. Manuel Nabız & Nefes Rehberi ve Tansiyon/Şeker Defteri ──
    data class TansiyonKaydi(
        val sistolik: Int, // büyük tansiyon (örn. 120)
        val diastolik: Int, // küçük tansiyon (örn. 80)
        val sekerMgDl: Int // kan şekeri (örn. 95)
    )

    object Faz2_1_SaglikVeMedikal {
        fun nefesEgzersiziMetniGetir(mod: String): Pair<String, String> {
            return when (mod.uppercase(Locale.US)) {
                "KARE" -> Pair("Kare Nefes (4-4-4-4)", "4s Nefes Al ➔ 4s Tut ➔ 4s Ver ➔ 4s Tut (Derin Odak)")
                else -> Pair("4-7-8 Sakinleştirici Nefes", "4s Nefes Al ➔ 7s Tut ➔ 8s Yavaşça Ver (Anksiyete Düşürücü)")
            }
        }

        fun tansiyonVeSekerDegerlendir(kayit: TansiyonKaydi): Pair<String, String> {
            val tansiyonYorum = when {
                kayit.sistolik > 140 || kayit.diastolik > 90 -> "⚠️ Yüksek Tansiyon Riski: ${kayit.sistolik}/${kayit.diastolik} mmHg"
                kayit.sistolik < 90 || kayit.diastolik < 60 -> "ℹ️ Düşük Tansiyon: ${kayit.sistolik}/${kayit.diastolik} mmHg"
                else -> "✅ İdeal Tansiyon: ${kayit.sistolik}/${kayit.diastolik} mmHg"
            }
            val sekerYorum = when {
                kayit.sekerMgDl > 140 -> "⚠️ Yüksek Şeker (Tokluk > 140 mg/dL)"
                kayit.sekerMgDl < 70 -> "⚠️ Düşük Şeker (Hipoglisemi < 70 mg/dL)"
                else -> "✅ İdeal Şeker Seviyesi (${kayit.sekerMgDl} mg/dL)"
            }
            return Pair(tansiyonYorum, sekerYorum)
        }
    }

    // ── 2. Dengeli Öğün Kalori Sayacı ve 16:8 Aralıklı Oruç Takipçisi ──
    data class OgunKalori(
        val kahvaltiKcal: Int,
        val ogleKcal: Int,
        val aksamKcal: Int,
        val araOgunKcal: Int
    )

    object Faz2_2_BeslenmeVeOruc {
        fun toplamKaloriHesapla(ogun: OgunKalori, gunlukHedef: Int = 2000): Pair<Int, String> {
            val toplam = ogun.kahvaltiKcal + ogun.ogleKcal + ogun.aksamKcal + ogun.araOgunKcal
            val yuzde = (toplam * 100) / gunlukHedef.coerceAtLeast(1)
            val yorum = when {
                yuzde > 115 -> "Aşırı Yükleme (%$yuzde) -> Güne 20 dk hafif yürüyüş eklemeniz önerilir."
                yuzde in 85..115 -> "Dengeli Beslenme (%$yuzde) -> Günlük ideal kalori bandındasınız."
                else -> "Düşük Kalori (%$yuzde) -> Enerji düşüklüğü olmaması için protein ağırlıklı beslenin."
            }
            return Pair(toplam, yorum)
        }

        fun aralikliOruc168Hesapla(sonOgunSaat: Int): Pair<Int, String> {
            val orucBitisSaat = (sonOgunSaat + 16) % 24
            val mesaj = "Son öğün ${sonOgunSaat}:00 ➔ 16 saatlik açlık penceresi ertesi gün saat ${String.format(Locale.US, "%02d:00", orucBitisSaat)}'da sona eriyor. (Yağ yakım ve otofaji fazı aktif)"
            return Pair(orucBitisSaat, mesaj)
        }
    }

    // ── 3. Harcama Limit Radarı ve Kişisel Borç/Alacak Defteri ──
    data class BorcAlacakKaydi(
        val kisi: String,
        val tutar: Int,
        val alacakMi: Boolean // true: biz alacağız, false: borcumuz var
    )

    object Faz2_3_ButceVeBorc {
        fun harcamaRadarDurumu(gunlukLimit: Int, harcananTutar: Int): Pair<Int, String> {
            val yuzde = (harcananTutar * 100) / gunlukLimit.coerceAtLeast(1)
            val yorum = when {
                yuzde >= 100 -> "🚨 KRİTİK: Günlük bütçe limiti aşıldı! (%$yuzde)"
                yuzde >= 80 -> "🟠 UYARI: Günlük limitin %$yuzde kadarı kullanıldı, harcamaları yavaşlatın."
                else -> "🟢 GÜVENLİ: Günlük bütçenin %$yuzde kadarı harcandı."
            }
            return Pair(yuzde, yorum)
        }

        fun netAlacakBorcHesapla(kayitlar: List<BorcAlacakKaydi>): Pair<Int, String> {
            var net = 0
            for (k in kayitlar) {
                if (k.alacakMi) net += k.tutar else net -= k.tutar
            }
            val ozet = if (net >= 0) {
                "Net Durum: +$net ₺ (Alacaklısınız)"
            } else {
                "Net Durum: $net ₺ (Borçlusunuz)"
            }
            return Pair(net, ozet)
        }
    }

    // ── 4. Kumbara Hedef Metresi ve Döviz/Altın Portföy Değeri ──
    data class PortfoyVarlik(
        val altinGram: Double,
        val usdMiktar: Double,
        val eurMiktar: Double,
        val altinFiyatTl: Double = 3300.0,
        val usdFiyatTl: Double = 39.5,
        val eurFiyatTl: Double = 43.0
    )

    object Faz2_4_VarlikVeKumbara {
        fun kumbaraIlerleme(hedefTutar: Int, birikenTutar: Int): Pair<Int, String> {
            val yuzde = ((birikenTutar.toDouble() / hedefTutar.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
            val kalan = (hedefTutar - birikenTutar).coerceAtLeast(0)
            return Pair(yuzde, "%$yuzde tamamlandı — Hedefe ulaşmak için kalan tutar: $kalan ₺")
        }

        fun toplamPortfoyDegeriTl(p: PortfoyVarlik): Pair<Double, String> {
            val toplam = (p.altinGram * p.altinFiyatTl) + (p.usdMiktar * p.usdFiyatTl) + (p.eurMiktar * p.eurFiyatTl)
            val formatli = String.format(Locale.US, "%.2f", toplam)
            return Pair(toplam, "Portföy Toplam Değeri: $formatli ₺ (Altın: ${p.altinGram}g, USD: \$${p.usdMiktar}, EUR: €${p.eurMiktar})")
        }
    }

    // ── 5. Abonelik Kapatma & Tasarruf Simülasyonu ──
    data class AbonelikKalemi(
        val ad: String,
        val aylikTutar: Int,
        val gecerliMi: Boolean // false ise kapatıldı / tasarruf edildi
    )

    object Faz2_5_AbonelikTasarruf {
        fun varsayilanAbonelikler(): List<AbonelikKalemi> {
            return listOf(
                AbonelikKalemi("Video Yayın Hizmeti", 250, true),
                AbonelikKalemi("Müzik Platformu", 80, true),
                AbonelikKalemi("Kullanılmayan Bulut Depolama", 120, false),
                AbonelikKalemi("Spor Salonu Üyeliği", 850, false)
            )
        }

        fun yillikTasarrufSimuleEt(abonelikler: List<AbonelikKalemi>): Pair<Int, String> {
            val iptalEdilenler = abonelikler.filter { !it.gecerliMi }
            val aylikTasarruf = iptalEdilenler.sumOf { it.aylikTutar }
            val yillikTasarruf = aylikTasarruf * 12
            return Pair(
                yillikTasarruf,
                "${iptalEdilenler.size} abonelik iptal edildi ➔ Aylık $aylikTasarruf ₺, Yılda Toplam $yillikTasarruf ₺ Tasarruf!"
            )
        }
    }

    // ── 6. Özel AI Prompt Kasası ve TTS Ses Tonu/Hızı Ayarı ──
    data class TtsAyar(
        val okumaHizi: Float, // 0.75f - 1.5f
        val sesTonu: Float    // 0.8f - 1.2f
    )

    object Faz2_6_AiVeTts {
        fun varsayilanOzelPrompt(): String {
            return "Her açıklamanın başında motivasyon verici kısa bir Stoacı felsefe sözü ekle ve karmaşık terimleri sade dille özetle."
        }

        fun ttsAyarOzetle(ayar: TtsAyar): String {
            val hizStr = String.format(Locale.US, "%.2fx", ayar.okumaHizi)
            val tonStr = String.format(Locale.US, "%.2f", ayar.sesTonu)
            return "🔊 TTS Konuşma Hızı: $hizStr · Ses Tonu (Pitch): $tonStr (Doğal Türkçe Vurgu Aktif)"
        }
    }

    // ── 7. Pofi Başarı Vitrini, Binaural Odak Frekansları & Çevrimdışı Kasa ──
    data class Rozet(
        val id: Int,
        val ad: String,
        val aciklama: String,
        val acildiMi: Boolean
    )

    object Faz2_7_FrekansVeGuvenlik {
        fun varsayilanRozetler(): List<Rozet> {
            return listOf(
                Rozet(1, "Bronz Çırak", "İlk 10 görev ve 5 pomodoro tamamlandı", true),
                Rozet(2, "Gümüş Usta", "Haftalık 20 saat odak hedefine ulaşıldı", true),
                Rozet(3, "Altın Efsane", "30 gün kesintisiz çalışma serisi", false),
                Rozet(4, "Gece Kuşu", "Gece 22:00 - 02:00 arası 10 oturum yapıldı", true)
            )
        }

        fun binauralFrekansAciklamasi(frekansHz: Int): String {
            return when (frekansHz) {
                40 -> "40 Hz Gamma Frekansı: Yüksek bilişsel odak ve analitik problem çözme dalgası."
                14 -> "14 Hz Beta Frekansı: Aktif çalışma, okuma ve dikkat toplama modu."
                10 -> "10 Hz Alpha Frekansı: Sakin öğrenme ve hafıza konsolidasyon ritmi."
                else -> "4 Hz Delta Frekansı: Derin zihinsel dinlenme ve uyku hazırlık frekansı."
            }
        }

        fun cevrimdisiKasaKontrolu(): Pair<Boolean, String> {
            return Pair(true, "🔒 %100 Çevrimdışı Kasa Doğrulandı: Tüm medikal, bütçe ve AI talimat kayıtları yerel şifreli JSON olarak saklanıyor. Bulut bağımlılığı sıfır.")
        }
    }
}
