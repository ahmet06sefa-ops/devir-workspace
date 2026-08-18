package com.gunlukasistan.app

import org.json.JSONObject
import java.util.Locale

/**
 * v10.56 — C, D, E, G, H, I ve J Kategorileri Gelişmiş Hayat Atölyesi (saf mantık motoru).
 *
 * Kullanıcının talep ettiği 7 ana temayı (70 öneri maddesi) temsil eden 7 özel modülü barındırır:
 *  1. Modül C: Otonom Yapay Zeka Koçluğu & Özel Talimat (#21..#30)
 *  2. Modül D: Oyunlaştırma, XP & Hafta Sonu Odak Maratonu (#31..#40)
 *  3. Modül E: Binaural 40Hz/10Hz Mikseri & Titreşim Ritmi (#41..#50)
 *  4. Modül G: Esnek Pomodoro Sprintleri & Taşma Süresi (#61..#70)
 *  5. Modül H: Tasarım Şablonu Hızlı Seçicisi & Akordiyon (#71..#80)
 *  6. Modül I: Feynman Anlatım Simülatörü & KPSS Soru Sayacı (#81..#90)
 *  7. Modül J: Depolama Analitik Merkezi & Bütüncül JSON (#91..#100)
 */
object GelismiAtolye {

    // ── MODÜL C: Otonom Yapay Zeka Koçluğu & Özel Talimat (#21..#30) ──
    data class AiKocTalimati(
        val ozelTalimat: String = "Her gün bir Sokratik soru sor",
        val ttsHiz: Float = 1.0f,
        val ttsPerde: Float = 1.0f,
        val aktifTon: YasamModulleri.AiTonu = YasamModulleri.AiTonu.SEFKATLI_ZEN
    )

    object ModulC_OtonomAi {
        fun promptHazirla(talimat: AiKocTalimati): String {
            val tonEk = YasamModulleri.AiKocTonu.tonPromptGetir(talimat.aktifTon)
            return "Karakter: ${talimat.aktifTon.baslik} | Kural: $tonEk | Özel Talimat: '${talimat.ozelTalimat}' | TTS(Hız=${talimat.ttsHiz}x, Perde=${talimat.ttsPerde}x)"
        }

        /**
         * Kütüphaneci Not-Görev Ayrıştırıcısı (#27):
         * Serbest not içinde eyleme dönük kelime varsa ("al", "bitir", "yap", "oku", "çöz", "gönder", "çalış"),
         * temiz bir görev başlığı çıkarır.
         */
        fun notlardanGorevCikar(serbestNot: String): String? {
            val notTemiz = serbestNot.trim()
            if (notTemiz.isEmpty()) return null
            val anahtarlar = listOf("al", "bitir", "yap", "oku", "çöz", "gönder", "çalış", "ara", "yaz")
            val kucuk = notTemiz.lowercase(Locale("tr", "TR"))
            val kelimeler = kucuk.split(" ", ".", ",", "!", "?")
            val eylemVar = anahtarlar.any { ak -> kelimeler.contains(ak) }
            return if (eylemVar) {
                if (notTemiz.length > 50) notTemiz.substring(0, 50).trim() + "..." else notTemiz
            } else {
                null
            }
        }
    }

    // ── MODÜL D: Oyunlaştırma, XP & Hafta Sonu Odak Maratonu (#31..#40) ──
    data class XpDurumu(
        val xp: Int = 120,
        val comboCarpan: Float = 1.0f,
        val haftaSonuOdakDk: Int = 45,
        val kupaKazandiMi: Boolean = false
    )

    object ModulD_Oyunlastirma {
        fun rutbeGetir(xp: Int): String {
            return when {
                xp >= 300 -> "👑 Efsane"
                xp >= 100 -> "⭐ Usta"
                else -> "🌱 Çırak"
            }
        }

        fun gorevTamamla(durum: XpDurumu, comboMi: Boolean): XpDurumu {
            val carpan = if (comboMi) 1.5f else 1.0f
            val kazanc = (10 * carpan).toInt()
            return durum.copy(xp = durum.xp + kazanc, comboCarpan = carpan)
        }

        fun haftaSonuOdakEkle(durum: XpDurumu, dk: Int): XpDurumu {
            val yeniDk = durum.haftaSonuOdakDk + dk
            val kupa = yeniDk >= 120
            return durum.copy(haftaSonuOdakDk = yeniDk, kupaKazandiMi = kupa)
        }

        fun surprizBilgiGetir(indeks: Int): String {
            val bilgiler = arrayOf(
                "💡 Bilgi: Beyin, 25 dakikalık odak seansından sonra %15 daha hızlı bağ kurar.",
                "💡 Bilgi: Uyku esnasında beyin glimfatik sistem ile toksinleri temizler.",
                "💡 Bilgi: 40Hz Gamma sesleri nöronlar arası senkronizasyonu artırır.",
                "💡 Bilgi: Feynman tekniğinde basit anlatım, bilginin kalıcılığını 3 katına çıkarır.",
                "💡 Bilgi: Su tüketimi, gün içi zihinsel yorgunluk hissini %30 oranında azaltır."
            )
            return bilgiler[indeks.coerceIn(0, bilgiler.lastIndex)]
        }
    }

    // ── MODÜL E: Ses, Frekans, Binaural Beats (#41..#50) ──
    enum class BitisSesi(val baslik: String) {
        KILIS_CANI("🔔 Kilis Çanı"),
        DIJITAL_ALARM("⏰ Dijital Alarm"),
        YUMUSAK_GONG("🧘 Yumuşak Gong"),
        SADECE_TITRESIM("📳 Sadece Titreşim")
    }

    enum class TitresimRitm(val baslik: String) {
        KISA_3("• • • (3 Kısa)"),
        KALP_ATISI("♥♥ (Kalp Atışı)"),
        UZUN_2("— — (2 Uzun)")
    }

    data class FrekansAyar(
        val alfa10HzAcik: Boolean = false,
        val gamma40HzAcik: Boolean = true,
        val bitisSesi: BitisSesi = BitisSesi.YUMUSAK_GONG,
        val ritm: TitresimRitm = TitresimRitm.KALP_ATISI
    )

    object ModulE_SesVeFrekans {
        fun sesOzetiGetir(ayar: FrekansAyar): String {
            val frekans = when {
                ayar.gamma40HzAcik && ayar.alfa10HzAcik -> "40Hz Gamma + 10Hz Alfa"
                ayar.gamma40HzAcik -> "40Hz Gamma (Odak)"
                ayar.alfa10HzAcik -> "10Hz Alfa (Rahatlama)"
                else -> "Frekans Kapalı"
            }
            return "Frekans: $frekans · Bitiş: ${ayar.bitisSesi.baslik} · Ritm: ${ayar.ritm.baslik}"
        }

        fun frekansToggle(ayar: FrekansAyar, gammaMi: Boolean): FrekansAyar {
            return if (gammaMi) {
                ayar.copy(gamma40HzAcik = !ayar.gamma40HzAcik)
            } else {
                ayar.copy(alfa10HzAcik = !ayar.alfa10HzAcik)
            }
        }
    }

    // ── MODÜL G: Zamanlayıcı, Zen Odak, PiP & Kilit Ekranı (#61..#70) ──
    data class SprintAyar(
        val calismaDk: Int = 25,
        val molaDk: Int = 5,
        val tasmaDk: Int = 0,
        val masayaDonus15sMi: Boolean = true
    )

    object ModulG_SayacVePip {
        fun hazirSprintSec(preset: Int): SprintAyar {
            return when (preset) {
                1 -> SprintAyar(calismaDk = 50, molaDk = 10, tasmaDk = 0)
                2 -> SprintAyar(calismaDk = 30, molaDk = 5, tasmaDk = 0)
                3 -> SprintAyar(calismaDk = 15, molaDk = 0, tasmaDk = 0)
                else -> SprintAyar(calismaDk = 25, molaDk = 5, tasmaDk = 0)
            }
        }

        fun tasmaSuresiEkle(ayar: SprintAyar, ekDk: Int): SprintAyar {
            return ayar.copy(tasmaDk = (ayar.tasmaDk + ekDk).coerceAtLeast(0))
        }

        fun sayacDurumMetni(ayar: SprintAyar): String {
            val masaya = if (ayar.masayaDonus15sMi) "15s Geri Sayım Açık" else "Masaya Dönüş Kapalı"
            return "Sprint: ${ayar.calismaDk}m / ${ayar.molaDk}m · Taşma: +${ayar.tasmaDk} dk · $masaya"
        }
    }

    // ── MODÜL H: Gelişmiş Arayüz & Tasarım Özelleştirme (#71..#80) ──
    enum class TasarimSablonu(val baslik: String, val koseDp: Int) {
        ULTRA_KESKIN_0DP("⚡ Ultra Keskin (0dp)", 0),
        KOMPAKT_YUVARLAK_16DP("🎨 Modern Yuvarlak (16dp)", 16),
        GECE_ZEN("🌙 Gece Zen (24dp)", 24)
    }

    enum class FontTipi(val baslik: String) {
        POPPINS("Poppins (Modern)"),
        ATKINSON("Atkinson (Erişilebilir)"),
        LORA("Lora (Kitap / Serif)")
    }

    data class TasarimTercihi(
        val sablon: TasarimSablonu = TasarimSablonu.KOMPAKT_YUVARLAK_16DP,
        val font: FontTipi = FontTipi.POPPINS,
        val akordiyonDaralma: Boolean = true
    )

    object ModulH_TasarimVeAkordiyon {
        fun sablonMetniGetir(tercih: TasarimTercihi): String {
            val akordiyon = if (tercih.akordiyonDaralma) "Akordiyon Daralır" else "Hep Açık"
            return "Şablon: ${tercih.sablon.baslik} · Font: ${tercih.font.baslik} · $akordiyon"
        }
    }

    // ── MODÜL I: Ders, KPSS, PDF & Öğrenme Motoru (#81..#90) ──
    data class FeynmanCalismasi(
        val konuBasligi: String = "KPSS Tarih: Kurtuluş Savaşı",
        val cozulenSoruSayisi: Int = 40,
        val gunlukSoruHedefi: Int = 100,
        val dersButceSaat: Float = 10.0f,
        val harcananSaat: Float = 4.5f
    )

    object ModulI_DersVeKpss {
        fun soruEkle(durum: FeynmanCalismasi, ekSoru: Int): FeynmanCalismasi {
            return durum.copy(cozulenSoruSayisi = (durum.cozulenSoruSayisi + ekSoru).coerceAtLeast(0))
        }

        fun feynmanAnlatimSkoru(aciklama: String): Int {
            val kelimeSayisi = aciklama.trim().split(Regex("\\s+")).size
            val cumleSayisi = aciklama.split(".", "!", "?").count { it.trim().isNotEmpty() }.coerceAtLeast(1)
            val ortKelime = kelimeSayisi / cumleSayisi
            // 10 yaşındakine anlatım: kısa cümleler ve net kelimeler (ortKelime <= 12 ideal)
            return when {
                kelimeSayisi < 5 -> 30
                ortKelime <= 12 -> 95
                ortKelime <= 20 -> 80
                else -> 65
            }
        }

        fun kpssDurumMetni(durum: FeynmanCalismasi): String {
            val yuzde = if (durum.gunlukSoruHedefi > 0) ((durum.cozulenSoruSayisi * 100) / durum.gunlukSoruHedefi).coerceIn(0, 100) else 0
            val kalanSaat = (durum.dersButceSaat - durum.harcananSaat).coerceAtLeast(0f)
            return "📚 Konu: ${durum.konuBasligi} · Soru: ${durum.cozulenSoruSayisi}/${durum.gunlukSoruHedefi} (%$yuzde) · Bütçe Kalan: ${kalanSaat} saat"
        }
    }

    // ── MODÜL J: Sistem, Otomasyon, Yedekleme & Analitik (#91..#100) ──
    data class DepolamaAnaliz(
        val notlarMb: Float = 2.4f,
        val pdflerMb: Float = 14.8f,
        val cacheMb: Float = 6.2f
    )

    object ModulJ_SistemVeAnalitik {
        fun toplamKullanimMb(analiz: DepolamaAnaliz): Float {
            return analiz.notlarMb + analiz.pdflerMb + analiz.cacheMb
        }

        fun onbellekTemizle(analiz: DepolamaAnaliz): DepolamaAnaliz {
            return analiz.copy(cacheMb = 0.0f)
        }

        fun depolamaMetniGetir(analiz: DepolamaAnaliz): String {
            val toplam = toplamKullanimMb(analiz)
            return String.format(Locale.US, "📦 Toplam: %.1f MB (Notlar: %.1f MB · PDF: %.1f MB · Önbellek: %.1f MB)", toplam, analiz.notlarMb, analiz.pdflerMb, analiz.cacheMb)
        }

        fun cdejMasterJsonUret(
            c: AiKocTalimati,
            d: XpDurumu,
            e: FrekansAyar,
            g: SprintAyar,
            h: TasarimTercihi,
            i: FeynmanCalismasi,
            j: DepolamaAnaliz
        ): JSONObject {
            return JSONObject().apply {
                put("c_talimat", c.ozelTalimat)
                put("c_ton", c.aktifTon.name)
                put("d_xp", d.xp)
                put("d_kupa", d.kupaKazandiMi)
                put("e_gamma", e.gamma40HzAcik)
                put("g_calisma", g.calismaDk)
                put("h_sablon", h.sablon.name)
                put("i_soru", i.cozulenSoruSayisi)
                put("j_toplamMb", toplamKullanimMb(j).toDouble())
                put("paket", "C-D-E-G-H-I-J-v10.56")
            }
        }

        fun cdejMasterJsonCoz(json: JSONObject?): Boolean {
            if (json == null) return false
            return json.optString("paket", "") == "C-D-E-G-H-I-J-v10.56"
        }
    }
}
