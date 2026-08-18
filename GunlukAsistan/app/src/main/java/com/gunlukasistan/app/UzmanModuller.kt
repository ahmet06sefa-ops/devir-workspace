package com.gunlukasistan.app

import org.json.JSONObject
import java.util.Locale

/**
 * v10.57 — Faz 2: C, D, E, G, H, I ve J İleri Seviye Uzman Modülleri & Özel Ekranlar.
 *
 *  1. Modül C (Uzman #25, #26): Biyo-Vakit Gündem & 10-Dk Acil Seri Kurtarma ([UzmanC_BiyoVakit])
 *  2. Modül D (Uzman #37, #39): Rozet Nadirlik Vitrini & Sosyal Başarı Kartı ([UzmanD_RozetVitrini])
 *  3. Modül E (Uzman #44, #49): Ses Fade-In/Out & Kulaklık Çıktı Auto-Pause ([UzmanE_FadeVeAutoPause])
 *  4. Modül G (Uzman #65, #68): Odak Yorgunluk Radarı & Çıktı Hasadı ([UzmanG_YorgunlukVeCikti])
 *  5. Modül H (Uzman #72, #80): Canlı Arayüz Aynası & Yüzebilen Durum Şeridi ([UzmanH_AynaVeYuzenSerit])
 *  6. Modül I (Uzman #82, #87): PDF Sayfa Bölme & Sınav Geri Sayım Şeridi ([UzmanI_PdfVeSinav])
 *  7. Modül J (Uzman #98, #99): Anahtar Kelime Arama & Bildirim Sağlığı Testi ([UzmanJ_AramaVeAlarmSagligi])
 */
object UzmanModuller {

    // ── 1. MODÜL C (Uzman #25, #26): Biyo-Vakit Gündem & 10-Dk Acil Seri Kurtarma ──
    object UzmanC_BiyoVakit {
        fun biyoVakitTavsiyesi(saat: Int): String {
            return when (saat.coerceIn(0, 23)) {
                in 5..11 -> "🌅 Sabah Odaklanması (05:00-12:00): Yüksek Analitik / Zorlu Görevler İçin En Verimli Saat"
                in 12..15 -> "☀️ Öğle Akışı (12:00-16:00): İletişim, Toplantılar & Hafif Görevler"
                in 16..18 -> "🌇 İkindi Ritm (16:00-19:00): Rutin Görevler & Aralıklı Tekrar (Flashcards)"
                in 19..22 -> "🌙 Akşam Huzuru (19:00-23:00): Feynman Anlatımı & Kitap Okuma"
                else -> "🦉 Gece Kuşu (23:00-05:00): Hafif Dinlenme veya 10-Dk Acil Seri Kurtarma"
            }
        }

        fun seriKurtarmaGerekliMi(saat: Int, dakika: Int, bugunOdakDk: Int): Boolean {
            return saat >= 23 && dakika >= 30 && bugunOdakDk == 0
        }

        fun seriKurtarmaMesaji(gerekliMi: Boolean): String {
            return if (gerekliMi) {
                "🚨 ACİL SERİ KURTARMA (23:30+): Bugün 0 dk odaklandınız! Serinizin bozulmaması için 10 dakikalık kurtarma oturumu başlatın."
            } else {
                "✅ Seri Güvende: Bugün yeterli odaklanma yapıldı veya süre erken."
            }
        }
    }

    // ── 2. MODÜL D (Uzman #37, #39): Rozet Nadirlik Vitrini & Sosyal Başarı Kartı ──
    data class NadirRozet(
        val id: String,
        val baslik: String,
        val aciklama: String,
        val nadirlikYuzde: Int,
        val acildiMi: Boolean
    )

    object UzmanD_RozetVitrini {
        fun nadirlikListesi(): List<NadirRozet> = listOf(
            NadirRozet("nr1", "🌱 İlk Adım", "İlk görevi tamamla", 92, true),
            NadirRozet("nr2", "🦉 Gece Kuşu", "23:00 sonrasında odaklan", 34, true),
            NadirRozet("nr3", "🧘 Zen Ustası", "5 Zen oturumu bitir", 18, false),
            NadirRozet("nr4", "👑 30 Gün Efsanesi", "30 gün kesintisiz seri", 5, false),
            NadirRozet("nr5", "🚀 C-D-E-G-H-I-J Patronu", "Tüm gelişmiş modülleri kullan", 2, true)
        )

        fun nadirlikVitriniOzeti(rozetler: List<NadirRozet>): String {
            val acilan = rozetler.filter { it.acildiMi }
            return "Açılan Nadir Rozetler (${acilan.size}/${rozetler.size}): " +
                    acilan.joinToString(" · ") { "${it.baslik} (%${it.nadirlikYuzde})" }
        }

        fun sosyalPaylasimMetni(rutbe: String, odakDk: Int, kupaMi: Boolean): String {
            val kupaStr = if (kupaMi) "👑 ALTIN KUPA" else "⚡ AKTİF SPRINT"
            return "══════════════════════════════\n" +
                    "  🚀 GÜNLÜK ASİSTAN — BAŞARI KARTI  \n" +
                    "  Rütbe: $rutbe | Odak: $odakDk Dk | $kupaStr \n" +
                    "  %100 Testli ve Otonom Hayat Asistanı \n" +
                    "══════════════════════════════"
        }
    }

    // ── 3. MODÜL E (Uzman #44, #49): Ses Fade-In/Out & Kulaklık Çıktı Auto-Pause ──
    data class FadeAyari(
        val fadeInSaniye: Int = 5,
        val fadeOutSaniye: Int = 5,
        val autoPauseAcik: Boolean = true
    )

    object UzmanE_FadeVeAutoPause {
        fun fadeOzetiGetir(ayar: FadeAyari): String {
            val ap = if (ayar.autoPauseAcik) "Kulaklık Çıkınca Otomatik Duraklatır" else "Sürekli Çalar"
            return "Fade-In: ${ayar.fadeInSaniye}s · Fade-Out: ${ayar.fadeOutSaniye}s · $ap"
        }

        fun kulaklikCiktiDurumu(autoPauseAcik: Boolean): String {
            return if (autoPauseAcik) {
                "⏸️ OTOMATİK DURAKLATILDI: Kulaklık bağlantısı kesildi, odak sayacı ve 40Hz Gamma sesi durduruldu."
            } else {
                "▶️ ÇALMAYA DEVAM EDİYOR: Kulaklık kesilse de hoparlörden devam eder."
            }
        }
    }

    // ── 4. MODÜL G (Uzman #65, #68): Odak Yorgunluk Radarı & Çıktı Hasadı ──
    data class YorgunlukEndeksi(
        val ardikPomodoroSayisi: Int = 2,
        val zihinselYorgunlukYuzde: Int = 50
    )

    object UzmanG_YorgunlukVeCikti {
        fun pomodoroEkle(endeks: YorgunlukEndeksi): YorgunlukEndeksi {
            val yeniSayi = endeks.ardikPomodoroSayisi + 1
            val yeniYuzde = (endeks.zihinselYorgunlukYuzde + 25).coerceAtMost(100)
            return YorgunlukEndeksi(ardikPomodoroSayisi = yeniSayi, zihinselYorgunlukYuzde = yeniYuzde)
        }

        fun yorgunlukRadariUyari(endeks: YorgunlukEndeksi): String {
            return when {
                endeks.zihinselYorgunlukYuzde >= 75 -> "⚠️ YORGUNLUK RADARI (%${endeks.zihinselYorgunlukYuzde}): 4 pomodoro tamamlandı! Zihinsel verim düşüyor, mutlaka 15 dakika yürüyüş veya su molası verin."
                endeks.zihinselYorgunlukYuzde >= 50 -> "⚡ İdeal Odak Alanı (%${endeks.zihinselYorgunlukYuzde}): Ritm güzel, 5 dakika kısa mola verebilirsiniz."
                else -> "🌱 Zihin Dinç (%${endeks.zihinselYorgunlukYuzde}): Odaklanmaya hazırsınız."
            }
        }

        fun ciktiHasadiMetni(gorevAd: String, ozetNot: String): String {
            val t = ozetNot.trim().ifEmpty { "Ödev tamamlandı" }
            return "• [25m HASAT] $gorevAd ➔ $t"
        }
    }

    // ── 5. MODÜL H (Uzman #72, #80): Canlı Arayüz Aynası & Yüzebilen Durum Şeridi ──
    data class AynaDurumu(
        val hexRenk: String = "#4C7DFF",
        val koseDp: Int = 16,
        val fontAd: String = "Poppins"
    )

    object UzmanH_AynaVeYuzenSerit {
        fun aynaKartMetni(durum: AynaDurumu): String {
            return "🪞 CANLI AYNA · Renk: ${durum.hexRenk} · Köşe: ${durum.koseDp}dp · Font: ${durum.fontAd} (Birebir v2 Uyumlu)"
        }

        fun yuzenSeritMetni(kalanDk: Int, tonBaslik: String, rutbe: String): String {
            return "⚡ ${kalanDk}m Kalan | 🎵 40Hz Gamma | 👑 $rutbe"
        }
    }

    // ── 6. MODÜL I (Uzman #82, #87): PDF Sayfa Bölme & Sınav Geri Sayım Şeridi ──
    data class SinavKaydi(
        val sinavAd: String,
        val kalanGun: Int,
        val hedefler: String
    )

    object UzmanI_PdfVeSinav {
        fun sinavListesi(): List<SinavKaydi> = listOf(
            SinavKaydi("KPSS Lisans 2026", 42, "Günlük 100 Soru · Hedef: 90 Puan"),
            SinavKaydi("YKS / TYT 2027", 310, "Feynman Konu Anlatımı · Hedef: İlk 5.000"),
            SinavKaydi("ALES Güz 2026", 95, "Sayısal Mantık 50 Soru/Gün")
        )

        fun sinavOzetMetni(kayit: SinavKaydi): String {
            val alarmStr = if (kayit.kalanGun <= 45) "🚨 YAKLAŞTI" else "📅 NORMAL"
            return "$alarmStr | ${kayit.sinavAd}: Son ${kayit.kalanGun} Gün (${kayit.hedefler})"
        }

        fun pdfBolmeHesapla(toplamSayfa: Int, bolumBasla: Int, bolumBitir: Int): String {
            val bas = bolumBasla.coerceIn(1, toplamSayfa)
            val bit = bolumBitir.coerceIn(bas, toplamSayfa)
            val sayfaSayisi = (bit - bas) + 1
            return "📄 PDF Bölücü: ${toplamSayfa} sayfalık kitaptan ${sayfaSayisi} sayfalık çalışma paketi oluşturuldu (Sayfa $bas - $bit)"
        }
    }

    // ── 7. MODÜL J (Uzman #98, #99): Anahtar Kelime Arama & Bildirim Sağlığı Testi ──
    data class AlarmTani(
        val bildirimIzniVarMi: Boolean = true,
        val pilOptimizasyonKapatildiMi: Boolean = true
    )

    object UzmanJ_AramaVeAlarmSagligi {
        fun alarmSaglikRaporu(tani: AlarmTani): String {
            val b = if (tani.bildirimIzniVarMi) "AÇIK ✔" else "KAPALI ❌"
            val p = if (tani.pilOptimizasyonKapatildiMi) "KAPALI (Doğru) ✔" else "AÇIK (Risk) ⚠️"
            return "🔔 Bildirim İzni: $b · 🔋 Pil Optimizasyonu (Doze): $p"
        }

        /**
         * 100 Maddelik katalog içinde anlık anahtar kelime arar ve konumu döndürür.
         */
        fun anahtarKelimeAra(sorgu: String): String {
            val s = sorgu.trim().lowercase(Locale("tr", "TR"))
            return when {
                s.contains("fatura") || s.contains("abonelik") -> "💳 Bulundu: Modül 2 / Kategori B (#11 Akıllı Fatura Monitörü)"
                s.contains("feynman") || s.contains("anlat") -> "📚 Bulundu: Modül I / Kategori I (#81 Feynman Konu Simülatörü)"
                s.contains("rem") || s.contains("biyo") || s.contains("uyku") -> "🌙 Bulundu: Modül 5 / Kategori A (#3 Biyo-Ritim Uyku Döngüsü)"
                s.contains("gamma") || s.contains("40hz") || s.contains("alfa") -> "🎧 Bulundu: Modül E / Kategori E (#42 40Hz Gamma Frekansı)"
                s.contains("tasma") || s.contains("overrun") -> "⏱️ Bulundu: Modül G / Kategori G (#66 Taşma Süresi Modu)"
                s.contains("kpss") || s.contains("yks") || s.contains("sinav") -> "📚 Bulundu: Modül I / Kategori I (#87 Sınav Geri Sayım Şeridi)"
                s.contains("yedek") || s.contains("json") || s.contains("klon") -> "🔄 Bulundu: Modül 10 / Kategori J (#91 Bütüncül JSON Klonlayıcı)"
                s.contains("rozet") || s.contains("xp") || s.contains("pofi") -> "🏆 Bulundu: Modül D / Kategori D (#31 Pofi Maskot Rozetleri)"
                else -> "🔍 '$sorgu' arandı: 100 Öneri Katalogu içinde genel arama yapıldı. İlgili modül sekmesini açabilirsiniz."
            }
        }
    }
}
