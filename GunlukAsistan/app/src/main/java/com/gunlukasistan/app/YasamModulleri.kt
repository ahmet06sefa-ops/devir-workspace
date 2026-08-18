package com.gunlukasistan.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * v10.55 — 10 Özel Yaşam Modülü & Manuel Kontrol Merkezi (saf mantık motoru).
 *
 * Kullanıcının günlük hayatını bütüncül olarak yönetmesine olanak tanıyan 10 manuel
 * kontrol modülünü barındırır:
 *  1. İlaç & Vitamin Saati Takipçisi ([IlacHatirlatisi])
 *  2. Akıllı Fatura & Abonelik Bütçe Monitörü ([FaturaTakipci])
 *  3. Günlük Su & Kafein Tüketim Sayacı ([SuKafeinSayaci])
 *  4. Pofi Maskot Oyunlaştırma Rozet Kilit Merkezi ([RozetKilitMerkezi])
 *  5. Biyo-Ritim & Uyku Döngüsü Manuel Ayarlayıcısı ([BiyoRitimAyari])
 *  6. Gelişmiş Ambient Sound & Frekans Mikseri ([AmbientMikser])
 *  7. Hızlı Harcama & Fiş Kayıt Günlüğü ([HizliHarcama])
 *  8. Çevrimdışı Hayatta Kalma & Acil Durum Kasası ([AcilDurumKasasi])
 *  9. Yapay Zeka Koçluk Tonu Manuel Seçicisi ([AiKocTonu])
 * 10. Manuel Yedekleme & JSON Veri Klonlayıcı ([VeriKlonlayici])
 */
object YasamModulleri {

    // ── 1. MODÜL: İlaç & Vitamin Saati Takipçisi ──
    data class IlacKaydi(
        val ad: String,
        val dozMg: Int,
        val saat: String,
        val yemektenOnceMi: Boolean,
        val alindiMi: Boolean
    )

    object IlacHatirlatisi {
        fun ilacOzeti(ilaclar: List<IlacKaydi>): String {
            if (ilaclar.isEmpty()) return "Henüz ilaç/vitamin kaydı eklenmedi."
            val alinanlar = ilaclar.count { it.alindiMi }
            return "$alinanlar/${ilaclar.size} İlaç/Vitamin Alındı (%${(alinanlar * 100) / ilaclar.size})"
        }

        fun dozMetni(ilac: IlacKaydi): String {
            val zamanlama = if (ilac.yemektenOnceMi) "Yemekten Önce" else "Yemekten Sonra"
            val durum = if (ilac.alindiMi) "☑ Alındı" else "☐ Bekliyor"
            return "${ilac.ad} (${ilac.dozMg}mg) · ${ilac.saat} · $zamanlama · $durum"
        }

        fun ilacDurumuDegistir(ilac: IlacKaydi, alindi: Boolean): IlacKaydi {
            return ilac.copy(alindiMi = alindi)
        }
    }

    // ── 2. MODÜL: Akıllı Fatura & Abonelik Bütçe Monitörü ──
    data class Abonelik(
        val ad: String,
        val tutarTry: Int,
        val odemeGun: Int, // Ayın kaçıncı günü
        val odendiMi: Boolean
    )

    object FaturaTakipci {
        fun aylikToplamTutar(abonelikler: List<Abonelik>): Int {
            return abonelikler.sumOf { it.tutarTry }
        }

        fun faturaOzeti(abonelikler: List<Abonelik>): String {
            val toplam = aylikToplamTutar(abonelikler)
            val odenen = abonelikler.filter { it.odendiMi }.sumOf { it.tutarTry }
            val kalan = toplam - odenen
            return "Aylık Yük: ${toplam} ₺ · Ödenen: ${odenen} ₺ · Kalan: ${kalan} ₺"
        }

        fun gecikenUyarisi(abonelikler: List<Abonelik>, bugunGun: Int): String {
            val gecikenler = abonelikler.filter { !it.odendiMi && bugunGun > it.odemeGun }
            if (gecikenler.isEmpty()) return "Geciken ödeme yok."
            return "⚠️ GECİKEN ÖDEMELER (${gecikenler.size}): " + gecikenler.joinToString(", ") { "${it.ad} (${it.tutarTry} ₺)" }
        }
    }

    // ── 3. MODÜL: Günlük Su & Kafein Tüketim Sayacı ──
    data class SuKafeinDurumu(
        val suMl: Int = 0,
        val suHedefMl: Int = 2500,
        val kafeinMg: Int = 0,
        val kafeinSinirMg: Int = 400
    )

    object SuKafeinSayaci {
        fun suEkle(durum: SuKafeinDurumu, ekMl: Int): SuKafeinDurumu {
            return durum.copy(suMl = (durum.suMl + ekMl).coerceAtLeast(0))
        }

        fun kafeinEkle(durum: SuKafeinDurumu, ekMg: Int): SuKafeinDurumu {
            return durum.copy(kafeinMg = (durum.kafeinMg + ekMg).coerceAtLeast(0))
        }

        fun hidrasyonYuzdesi(durum: SuKafeinDurumu): Int {
            if (durum.suHedefMl <= 0) return 0
            return ((durum.suMl * 100) / durum.suHedefMl).coerceIn(0, 100)
        }

        fun saglikUyarisi(durum: SuKafeinDurumu): String {
            return when {
                durum.kafeinMg >= durum.kafeinSinirMg -> "⚠️ KAFEİN SINIRI AŞILDI (${durum.kafeinMg}mg)! Kafeinli içecekleri bırakın."
                durum.suMl >= durum.suHedefMl -> "💧 Harika Hidrasyon! Günlük su hedefinize ulaştınız."
                durum.suMl < 1000 && durum.kafeinMg > 200 -> "⚠️ Susuzluk Riski: Kafein tüketimi yüksek ancak su alımı düşük!"
                else -> "💧 Su: ${durum.suMl}/${durum.suHedefMl} ml · ☕ Kafein: ${durum.kafeinMg}/${durum.kafeinSinirMg} mg"
            }
        }
    }

    // ── 4. MODÜL: Pofi Maskot Oyunlaştırma Rozet Kilit Merkezi ──
    data class PofiRozeti(
        val id: String,
        val baslik: String,
        val aciklama: String,
        val acildiMi: Boolean
    )

    object RozetKilitMerkezi {
        fun varsayilanRozetler(): List<PofiRozeti> = listOf(
            PofiRozeti("r1", "🌱 İlk Adım", "İlk görevi başarıyla tamamla", true),
            PofiRozeti("r2", "🔥 7 Günlük Seri", "7 gün üst üste uygulamayı kullan", true),
            PofiRozeti("r3", "⚡ 25m Odak Savaşçısı", "İlk pomodoro oturumunu bitir", true),
            PofiRozeti("r4", "🦉 Gece Kuşu", "Gece 23:00 sonrasında odaklan", false),
            PofiRozeti("r5", "☀️ Sabah Güneşi", "Sabah 06:00 önce uyan ve güne başla", false),
            PofiRozeti("r6", "🛡️ Odak Kalkanı", "Kesintisiz 100 dakika çalış", false),
            PofiRozeti("r7", "🧘 Zen Ustası", "Zen Odak modunda 5 oturum bitir", false),
            PofiRozeti("r8", "📚 Bilgi Çınarı", "50 ders notunu arşive ekle", false),
            PofiRozeti("r9", "🏆 30 Gün Efsanesi", "30 günlük kesintisiz seri yakala", false),
            PofiRozeti("r10", "👑 Asistan Patronu", "10 yaşam modülünü de aktif kullan", false)
        )

        fun rozetAc(rozetler: List<PofiRozeti>, rozetId: String): List<PofiRozeti> {
            return rozetler.map { if (it.id == rozetId) it.copy(acildiMi = true) else it }
        }

        fun rozetKilitOzeti(rozetler: List<PofiRozeti>): String {
            val acilan = rozetler.count { it.acildiMi }
            return "$acilan/${rozetler.size} Pofi Rozeti Açıldı (%${(acilan * 100) / rozetler.size})"
        }
    }

    // ── 5. MODÜL: Biyo-Ritim & Uyku Döngüsü Manuel Ayarlayıcısı ──
    data class UykuDonguPlan(
        val uyumaSaat: Int,
        val uyumaDakika: Int,
        val donguSayisi: Int = 5 // 5 döngü = 7.5 saat
    )

    object BiyoRitimAyari {
        /**
         * 90 dakikalık REM döngülerine ve 15 dk uykuya dalma süresine göre uyanma saati hesaplar.
         */
        fun idealUyanmaSaati(plan: UykuDonguPlan): String {
            val uykuyaDalmaDk = 15
            val toplamUykuDk = uykuyaDalmaDk + (plan.donguSayisi * 90)
            val baslangicDk = (plan.uyumaSaat * 60) + plan.uyumaDakika
            val uyanmaDk = (baslangicDk + toplamUykuDk) % (24 * 60)
            val s = uyanmaDk / 60
            val d = uyanmaDk % 60
            return String.format(Locale.US, "%02d:%02d", s, d)
        }

        fun dincUyanmaPuan(donguSayisi: Int): Int {
            return when (donguSayisi) {
                6 -> 100 // 9 saat (Mükemmel)
                5 -> 95  // 7.5 saat (İdeal)
                4 -> 80  // 6 saat (Minimum)
                3 -> 55  // 4.5 saat (Yetersiz)
                else -> 40
            }
        }
    }

    // ── 6. MODÜL: Gelişmiş Ambient Sound & Frekans Mikseri ──
    data class AmbientMikserAyari(
        val yagmurSeviye: Int = 0,    // 0-100
        val ormanSeviye: Int = 0,     // 0-100
        val beyazGurultuSeviye: Int = 0, // 0-100
        val gamma40HzAcik: Boolean = false
    )

    object AmbientMikser {
        fun mikserOzeti(ayar: AmbientMikserAyari): String {
            val parcalar = mutableListOf<String>()
            if (ayar.yagmurSeviye > 0) parcalar.add("Yağmur: %${ayar.yagmurSeviye}")
            if (ayar.ormanSeviye > 0) parcalar.add("Orman: %${ayar.ormanSeviye}")
            if (ayar.beyazGurultuSeviye > 0) parcalar.add("Beyaz Gürültü: %${ayar.beyazGurultuSeviye}")
            if (ayar.gamma40HzAcik) parcalar.add("40Hz Gamma Frekansı ON")
            if (parcalar.isEmpty()) return "Tüm sesler kapalı."
            return parcalar.joinToString(" · ")
        }
    }

    // ── 7. MODÜL: Hızlı Harcama & Fiş Kayıt Günlüğü ──
    data class Harcama(
        val kategori: String,
        val tutarTry: Int,
        val aciklama: String
    )

    object HizliHarcama {
        fun harcamaOzeti(harcamalar: List<Harcama>, gunlukButceTry: Int): String {
            val toplam = harcamalar.sumOf { it.tutarTry }
            val kalan = gunlukButceTry - toplam
            return "Günlük Harcama: ${toplam} ₺ / Bütçe: ${gunlukButceTry} ₺ · Kalan: ${kalan} ₺"
        }

        fun enCokHarcamaKategori(harcamalar: List<Harcama>): String {
            if (harcamalar.isEmpty()) return "Harcama Yok"
            return harcamalar.groupBy { it.kategori }
                .maxByOrNull { entry -> entry.value.sumOf { it.tutarTry } }?.key ?: "Genel"
        }
    }

    // ── 8. MODÜL: Çevrimdışı Hayatta Kalma & Acil Durum Kasası ──
    data class AcilKasa(
        val kanGrubu: String = "A Rh+",
        val sosKisi: String = "Acil Kişi",
        val sosTelefon: String = "112",
        val tibbiNot: String = "Alerji yok"
    )

    object AcilDurumKasasi {
        fun acilKasaKartMetni(kasa: AcilKasa): String {
            return "🚨 ACİL DURUM KASASI · Kan Grubu: ${kasa.kanGrubu} · SOS: ${kasa.sosKisi} (${kasa.sosTelefon}) · Not: ${kasa.tibbiNot}"
        }
    }

    // ── 9. MODÜL: Yapay Zeka Koçluk Tonu Manuel Seçicisi ──
    enum class AiTonu(val baslik: String, val promptEk: String) {
        SERT_ASKER("🎖️ Sert Askeri Koç", "Bahane kabul etme, net ve disiplinli komutlar ver."),
        SEFKATLI_ZEN("🧘 Şefkatli Zen Rehberi", "Sakin, motive edici, anlayışlı ve huzurlu bir dille konuş."),
        SOKRATIK_FILOZOF("📜 Sokratik Filozof", "Sorular sorarak düşündür ve felsefi bir derinlik kat."),
        ESPIRILI_POFI("🐼 Esprili Pofi Maskot", "Neşeli, emojili ve arkadaş canlısı bir maskot gibi konuş.")
    }

    object AiKocTonu {
        fun tonBasligiGetir(ton: AiTonu): String = ton.baslik
        fun tonPromptGetir(ton: AiTonu): String = ton.promptEk
    }

    // ── 10. MODÜL: Manuel Yedekleme & JSON Veri Klonlayıcı ──
    object VeriKlonlayici {
        fun klonJsonUret(
            suKafein: SuKafeinDurumu,
            aiTonu: AiTonu,
            mikser: AmbientMikserAyari
        ): JSONObject {
            return JSONObject().apply {
                put("suMl", suKafein.suMl)
                put("kafeinMg", suKafein.kafeinMg)
                put("aiTon", aiTonu.name)
                put("yagmur", mikser.yagmurSeviye)
                put("gamma", mikser.gamma40HzAcik)
                put("surum", "10.55")
            }
        }

        fun klonJsonCoz(json: JSONObject?): Triple<SuKafeinDurumu, AiTonu, AmbientMikserAyari> {
            if (json == null) {
                return Triple(SuKafeinDurumu(), AiTonu.SEFKATLI_ZEN, AmbientMikserAyari())
            }
            val su = SuKafeinDurumu(
                suMl = json.optInt("suMl", 0),
                kafeinMg = json.optInt("kafeinMg", 0)
            )
            val tonStr = json.optString("aiTon", AiTonu.SEFKATLI_ZEN.name)
            val ton = runCatching { AiTonu.valueOf(tonStr) }.getOrDefault(AiTonu.SEFKATLI_ZEN)
            val mikser = AmbientMikserAyari(
                yagmurSeviye = json.optInt("yagmur", 0),
                gamma40HzAcik = json.optBoolean("gamma", false)
            )
            return Triple(su, ton, mikser)
        }
    }
}
