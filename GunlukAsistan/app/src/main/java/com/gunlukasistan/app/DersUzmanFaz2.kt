package com.gunlukasistan.app

import org.json.JSONObject
import java.util.Locale

/**
 * v10.61 — Ders Çalışma Uzman Faz 2: Sanal Kütüphane Masası & Sınav Anksiyetesi Yatıştırıcı
 * Merkezi (saf mantık motoru).
 *
 *  1. Modül 1 (Uzman #71, #75, #72): Sanal Kütüphane Masası (Pofi) & Akıllı Odak Odası ([Uzman1_SanalKutuphane])
 *  2. Modül 2 (Uzman #81, #85): 4-7-8 Sınav Anksiyetesi Yatıştırıcı Nefes Motoru ([Uzman2_478Nefes])
 *  3. Modül 3 (Uzman #83, #87): Gece 'Zihni Boşaltma' Defteri & Sabah Olumlamaları ([Uzman3_ZihniBosaltma])
 *  4. Modül 4 (Uzman #82, #88): Kahve REM Penceresi & Aşırı Çalışma (Burnout) Freni ([Uzman4_KafeinVeBurnout])
 *  5. Modül 5 (Uzman #73, #78): Günlük Soru Hedefi Kupa Rozetleri & XP Barı ([Uzman5_SoruKupasi])
 *  6. Modül 6 (Uzman #98, #69): Anahtar Kelimeyle Konu Arama & Önkoşul Rehberi ([Uzman6_KelimeAramaVeOnkosul])
 *  7. Modül 7 (Uzman #95, #94): Cihazlar Arası Tek Tıkla Pano Senkronu ([Uzman7_PanoSenkron])
 */
object DersUzmanFaz2 {

    // ── 1. Sanal Kütüphane Masası (Pofi) & Akıllı Odak Odası ──
    enum class PofiDurum(val baslik: String, val ikon: String) {
        ODAK("Masada Kitap Okuyor", "📖"),
        MOLA("Çay İçip Dinleniyor", "☕"),
        ZAFER("Zıplayıp Kutluyor", "🎉")
    }

    data class SanalMasaDurum(
        val pofiDurum: PofiDurum = PofiDurum.ODAK,
        val masaArkadasSayisi: Int = 3,
        val zincirGun: Int = 14
    )

    object Uzman1_SanalKutuphane {
        fun arkadasDavetEt(durum: SanalMasaDurum): SanalMasaDurum {
            val yeniSayi = (durum.masaArkadasSayisi + 1).coerceAtMost(10)
            return durum.copy(masaArkadasSayisi = yeniSayi)
        }

        fun sanalMasaMetniGetir(durum: SanalMasaDurum): String {
            val kolektifSaat = durum.masaArkadasSayisi * 4
            return "🐼 SANAL KÜTÜPHANE MASASI: Pofi şu an ${durum.pofiDurum.baslik} (${durum.pofiDurum.ikon}) · Masadaki Arkadaş: ${durum.masaArkadasSayisi} Kişi · Toplam Kolektif Odak: ${kolektifSaat} Saat"
        }
    }

    // ── 2. 4-7-8 Sınav Anksiyetesi Yatıştırıcı Nefes Motoru ──
    enum class NefesAdim(val baslik: String, val saniye: Int, val talimat: String) {
        AL_4S("4s Nefes Al", 4, "Burnundan derin ve sakin nefes al"),
        TUT_7S("7s Nefesini Tut", 7, "Nefesini tut ve nabzının yavaşlamasına izin ver"),
        VER_8S("8s Yavaşça Ver", 8, "Ağzından uzunca nefes vererek kaygıyı bırak")
    }

    object Uzman2_478Nefes {
        fun sonrakiNefesAdimi(adim: NefesAdim): NefesAdim {
            return when (adim) {
                NefesAdim.AL_4S -> NefesAdim.TUT_7S
                NefesAdim.TUT_7S -> NefesAdim.VER_8S
                NefesAdim.VER_8S -> NefesAdim.AL_4S
            }
        }

        fun nefesRehberMetni(adim: NefesAdim, tamamlananTur: Int): String {
            val kaygiDususYuzde = (tamamlananTur * 20).coerceAtMost(100)
            return "🧘 4-7-8 ANKSİYETE YATIŞTIRICI: ${adim.baslik} (${adim.talimat}) | Tamamlanan Tur: $tamamlananTur (%$kaygiDususYuzde Kaygı Düşüşü)"
        }
    }

    // ── 3. Gece 'Zihni Boşaltma' Defteri & Sabah Olumlamaları ──
    data class BrainDumpKaydi(
        val endiseMetni: String = "Yarınki KPSS matematik denemesinden korkuyorum",
        val sokratikCozum: String = "Sokratik Koç: 'Denemeler eksiklerini görmek için bir laboratuvardır. Hata yapmak öğrenmenin en hızlı yoludur!'"
    )

    object Uzman3_ZihniBosaltma {
        fun zihniBosaltNotEkle(endise: String): BrainDumpKaydi {
            val t = endise.trim().ifEmpty { "Sınav kaygısı" }
            return BrainDumpKaydi(
                endiseMetni = t,
                sokratikCozum = "Sokratik Koç: '$t' konusundaki endişeni anlıyorum. Şu an yapman gereken tek şey derin nefes alıp güzelce dinlenmek!"
            )
        }

        fun sabahOlumlamasiGetir(indeks: Int): String {
            val olumlamalar = arrayOf(
                "🌅 Sabah Olumlaması: Elimden gelenin en iyisini yaptım ve sınava hazırım!",
                "🌅 Sabah Olumlaması: Zihnim berrak, odaklanmış ve huzurluyum.",
                "🌅 Sabah Olumlaması: Hata yapmak öğrenmenin en hızlı yoludur, her deneme beni geliştirir.",
                "🌅 Sabah Olumlaması: Bugünün kurbağası olan zor konuyu bitirip zihnimi hafifleteceğim."
            )
            return olumlamalar[indeks.coerceIn(0, olumlamalar.lastIndex)]
        }
    }

    // ── 4. Kahve REM Penceresi & Aşırı Çalışma (Burnout) Freni ──
    object Uzman4_KafeinVeBurnout {
        fun kafeinPenceresiUyari(saat: Int): String {
            return if (saat >= 17) {
                "☕ KAFEİN REM UYARISI (Saat 17:00+): Gece REM uykunuzun zarar görmemesi için kafein alımını sonlandırın."
            } else {
                "☕ Kafein Penceresi Uygun: Zihinsel uyanıklık için kahve tüketebilirsiniz."
            }
        }

        fun burnoutFrenDenetimi(gunlukCalismaSaat: Float): String {
            return if (gunlukCalismaSaat >= 8.0f) {
                String.format(Locale.US, "🚨 AŞIRI ÇALIŞMA (BURNOUT) FRENİ: Bugün %.1f saat çalıştınız! Zihinsel doygunluk sınırına ulaştınız, mutlaka dinlenin.", gunlukCalismaSaat)
            } else {
                String.format(Locale.US, "⚡ İdeal Çalışma Ritm: Bugün %.1f saat çalışıldı, bilişsel verim yüksek.", gunlukCalismaSaat)
            }
        }
    }

    // ── 5. Günlük Soru Hedefi Kupa Rozetleri & XP Barı ──
    enum class KupaSeviye(val baslik: String, val esikSoru: Int) {
        BRONZ("🥉 Bronz Kupa", 50),
        GUMUS("🥈 Gümüş Kupa", 150),
        ALTIN("🥇 Altın Kupa", 250),
        ELMAS("💎 Elmas Kupa", 500)
    }

    data class SoruKupaDurum(
        val cozulenSoru: Int = 160,
        val hedefSoru: Int = 250
    )

    object Uzman5_SoruKupasi {
        fun soruEkle(durum: SoruKupaDurum, ek: Int): SoruKupaDurum {
            return durum.copy(cozulenSoru = (durum.cozulenSoru + ek).coerceAtLeast(0))
        }

        fun aktifKupaGetir(soruSayisi: Int): KupaSeviye {
            return when {
                soruSayisi >= KupaSeviye.ELMAS.esikSoru -> KupaSeviye.ELMAS
                soruSayisi >= KupaSeviye.ALTIN.esikSoru -> KupaSeviye.ALTIN
                soruSayisi >= KupaSeviye.GUMUS.esikSoru -> KupaSeviye.GUMUS
                else -> KupaSeviye.BRONZ
            }
        }

        fun soruKupaOzeti(durum: SoruKupaDurum): String {
            val kupa = aktifKupaGetir(durum.cozulenSoru)
            val yuzde = if (durum.hedefSoru > 0) ((durum.cozulenSoru * 100) / durum.hedefSoru).coerceIn(0, 100) else 0
            return "Soru: ${durum.cozulenSoru}/${durum.hedefSoru} (%$yuzde) · Aktif Kupa: ${kupa.baslik}"
        }
    }

    // ── 6. Anahtar Kelimeyle Konu Arama & Önkoşul Rehberi ──
    object Uzman6_KelimeAramaVeOnkosul {
        fun onkosulUyarisi(ders: String): String {
            return when (ders.trim().lowercase(Locale("tr", "TR"))) {
                "matematik" -> "🔗 ÖNKOŞUL REHBERİ (Matematik): 'İntegral' çözmeden önce 'Türev' ve 'Limit' konularını %100 bitirmelisiniz."
                "tarih" -> "🔗 ÖNKOŞUL REHBERİ (Tarih): 'Osmanlı Dağılma Dönemi' öncesinde 'Islahatlar ve Nizam-ı Cedid' kronolojisini pekiştirin."
                "türkçe", "turkce" -> "🔗 ÖNKOŞUL REHBERİ (Türkçe): 'Paragrafta Yapı' çözmeden önce 'Sözcükte Anlam ve Bağlaçlar' eksiklerini kapatın."
                else -> "🔗 ÖNKOŞUL REHBERİ ($ders): Konu anlatımında takılırsanız Feynman ve Leitner kutu tekrarlarını çalıştırın."
            }
        }

        fun kelimeyleKonuAra(sorgu: String): String {
            val s = sorgu.trim().lowercase(Locale("tr", "TR"))
            return when {
                s.contains("pofi") || s.contains("masa") || s.contains("kutuphane") -> "🐼 Bulundu: Modül 1 (#71 Sanal Kütüphane Masası)"
                s.contains("nefes") || s.contains("kaygi") || s.contains("anksiyete") -> "🧘 Bulundu: Modül 2 (#81 4-7-8 Nefes Egzersizi)"
                s.contains("dump") || s.contains("endise") || s.contains("zihni") -> "📓 Bulundu: Modül 3 (#83 Gece Zihni Boşaltma Defteri)"
                s.contains("kafein") || s.contains("kahve") || s.contains("burnout") -> "☕ Bulundu: Modül 4 (#82 Kafein REM Penceresi)"
                s.contains("kupa") || s.contains("rozet") || s.contains("soru") -> "🏆 Bulundu: Modül 5 (#73 Günlük Soru Kupası)"
                s.contains("leitner") || s.contains("flas") || s.contains("pdf") -> "🚀 Bulundu: v10.60 Ders Çalışma İleri Fazı (#1 Leitner & #41 PDF Flaş Kart)"
                else -> "🔍 '$sorgu' arandı: 100 Maddelik Uzman Ders Çalışma Katalogunda genel tarama yapıldı."
            }
        }
    }

    // ── 7. Cihazlar Arası Tek Tıkla Pano Senkronu ──
    object Uzman7_PanoSenkron {
        fun pofiMasterJsonUret(
            sanal: SanalMasaDurum,
            kupa: SoruKupaDurum,
            dump: BrainDumpKaydi
        ): JSONObject {
            return JSONObject().apply {
                put("pofi_durum", sanal.pofiDurum.name)
                put("arkadas", sanal.masaArkadasSayisi)
                put("soru", kupa.cozulenSoru)
                put("dump_endise", dump.endiseMetni)
                put("paket", "DersUzmanFaz2-v10.61")
            }
        }

        fun pofiMasterJsonCoz(json: JSONObject?): Boolean {
            if (json == null) return false
            return json.optString("paket", "") == "DersUzmanFaz2-v10.61"
        }
    }
}
