package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.48 — Kullanıcı maddesi #10: Otonom AI Ajanı, Gündem Orkestratörü,
 * Alışkanlık Bekçisi, Otonom Kütüphaneci ve Otopilot Karar Motoru (saf, JVM testli).
 */
object OtonomMotor {

    data class AjanEylemi(
        val tur: EylemTuru,
        val ozet: String,
        val saat: Int = 0,
        val dakika: Int = 0,
        val metinParam: String = "",
        val sayacDk: Int = 0
    )

    enum class EylemTuru {
        UYKU_SAATI_GUNCELLE,
        SAYAC_KUR,
        GOREV_EKLE,
        HEDEF_GUNCELLE
    }

    data class OrkestreGorev(
        val baslik: String,
        val oncelikPuan: Int,
        val onerilenSaat: String,
        val gerekce: String
    )

    data class KurtarmaOnerisi(
        val aliskanlikAd: String,
        val mevcutSeri: Int,
        val kurtarmaDk: Int,
        val mesaj: String
    )

    /**
     * Kullanıcının doğal dil veya komut metnini ayrıştırarak eylem listesi üretir.
     * Ör: "Sabah uyanma saatimi 07:30 yap, 25 dk sayaç kur ve 'Market alışverişi' görevi ekle"
     */
    fun ajanKomutuAyristir(komutMetni: String): List<AjanEylemi> {
        val metin = komutMetni.lowercase(Locale("tr"))
        val eylemler = mutableListOf<AjanEylemi>()

        // 1. Uyanma saati tespiti (ör: 07:30 veya 7:30)
        val saatRegex = Regex("""(\d{1,2}):(\d{2})""")
        if ("uyan" in metin || "sabah" in metin || "saat" in metin) {
            val match = saatRegex.find(metin)
            if (match != null) {
                val s = match.groupValues[1].toIntOrNull() ?: 7
                val d = match.groupValues[2].toIntOrNull() ?: 0
                eylemler.add(
                    AjanEylemi(
                        tur = EylemTuru.UYKU_SAATI_GUNCELLE,
                        ozet = "Sabah uyanma saati %02d:%02d olarak ayarlandı".format(s, d),
                        saat = s,
                        dakika = d
                    )
                )
            }
        }

        // 2. Sayaç tespiti (ör: 25 dk, 15 dk, sayaç kur)
        val sayacRegex = Regex("""(\d{1,3})\s*(dk|dakika)""")
        if ("sayac" in metin || "sayaç" in metin || "odak" in metin || sayacRegex.containsMatchIn(metin)) {
            val match = sayacRegex.find(metin)
            val dk = match?.groupValues?.get(1)?.toIntOrNull() ?: 25
            eylemler.add(
                AjanEylemi(
                    tur = EylemTuru.SAYAC_KUR,
                    ozet = "$dk dakikalık odak sayacı hazırlandı",
                    sayacDk = dk
                )
            )
        }

        // 3. Görev ekleme tespiti (ör: 'Market alışverişi' görevi ekle veya görev: xxx)
        val tirkRegex = Regex("""['"']([^'"']+)['"']""")
        if ("görev" in metin || "gorev" in metin || "ekle" in metin) {
            val match = tirkRegex.find(komutMetni)
            val baslik = if (match != null) {
                match.groupValues[1]
            } else if ("görev:" in metin || "gorev:" in metin) {
                komutMetni.substringAfter("rev:").trim()
            } else {
                "Yeni Ajan Görevi"
            }
            if (baslik.isNotBlank()) {
                eylemler.add(
                    AjanEylemi(
                        tur = EylemTuru.GOREV_EKLE,
                        ozet = "Görev eklendi: '$baslik'",
                        metinParam = baslik
                    )
                )
            }
        }

        // 4. Hedef tespiti (ör: hedefimi 45 dk yap)
        if ("hedef" in metin && ("dk" in metin || "dakika" in metin)) {
            val match = sayacRegex.find(metin)
            val dk = match?.groupValues?.get(1)?.toIntOrNull() ?: 45
            eylemler.add(
                AjanEylemi(
                    tur = EylemTuru.HEDEF_GUNCELLE,
                    ozet = "Günlük odak hedefi $dk dk olarak güncellendi",
                    sayacDk = dk
                )
            )
        }

        return eylemler
    }

    /**
     * Akıllı Gündem Orkestratörü:
     * Dün geceki uyku süresine ve görev içeriklerine bakarak görevleri gün içi en verimli saatlere orkestre eder.
     */
    fun gundemOrkestrasyonu(
        gorevBasliklari: List<String>,
        uykuSuresiMs: Long,
        simdiSaat: Int
    ): List<OrkestreGorev> {
        val azUyku = uykuSuresiMs > 0L && uykuSuresiMs < 6 * 3600_000L
        val list = mutableListOf<OrkestreGorev>()

        gorevBasliklari.forEachIndexed { idx, baslik ->
            val bAlt = baslik.lowercase(Locale("tr"))
            val agirMi = "soru" in bAlt || "rapor" in bAlt || "proje" in bAlt ||
                    "matematik" in bAlt || "fizik" in bAlt || "çalış" in bAlt || "calis" in bAlt
            val hafifMi = "market" in bAlt || "ara" in bAlt || "mail" in bAlt || "fatura" in bAlt

            val oncelik = when {
                agirMi -> 30
                hafifMi -> 10
                else -> 20
            }

            val (onerilenSaat, gerekce) = when {
                azUyku && agirMi -> {
                    "14:30 - 16:00" to "Az uyku nedeniyle zihinsel ağır iş öğleden sonraki odak penceresine alındı."
                }
                agirMi -> {
                    "09:30 - 11:30" to "Sabahın ilk yüksek enerji penceresinde odaklanılması önerilir."
                }
                hafifMi -> {
                    "11:30 - 12:30" to "Kolay tamamlanan rutin görev — ara zaman dilimine yerleştirildi."
                }
                else -> {
                    "13:00 - 14:00" to "Dengeli zaman dilimi ataması."
                }
            }

            list.add(OrkestreGorev(baslik, oncelik, onerilenSaat, gerekce))
        }

        return list.sortedByDescending { it.oncelikPuan }
    }

    /**
     * Akıllı Alışkanlık & Seri Bekçisi:
     * Akşam saatlerinde kırılmak üzere olan serileri tespit edip kurtarma önerisi döndürür.
     */
    fun seriKurtarmaAnalizi(
        aliskanlikAdlari: List<String>,
        tamamlananlar: List<Boolean>,
        seriler: List<Int>,
        simdiSaat: Int
    ): List<KurtarmaOnerisi> {
        if (simdiSaat < 18) return emptyList() // gündüz henüz risk yok
        val oneriler = mutableListOf<KurtarmaOnerisi>()
        for (i in aliskanlikAdlari.indices) {
            val ad = aliskanlikAdlari.getOrElse(i) { "Alışkanlık" }
            val tamam = tamamlananlar.getOrElse(i) { false }
            val seri = seriler.getOrElse(i) { 0 }
            if (!tamam && seri >= 2) {
                oneriler.add(
                    KurtarmaOnerisi(
                        aliskanlikAd = ad,
                        mevcutSeri = seri,
                        kurtarmaDk = 10,
                        mesaj = "$seri günlük '$ad' serin kırılmak üzere! 10 dk mikro odakla kurtar."
                    )
                )
            }
        }
        return oneriler
    }

    /**
     * Otonom Kütüphaneci: Notlar içindeki görev benzeri (todo/yap/al) satırları ayıklar.
     */
    fun notlardanGorevCikar(notMetinleri: List<String>): List<String> {
        val bulgular = mutableSetOf<String>()
        val gorevEylemleri = listOf(" al", " ara", " yaz", " öde", " ode", " gönder", " gonder", " hazırla", " hazirla", " tamamla")

        notMetinleri.forEach { metin ->
            metin.lines().forEach { satir ->
                val s = satir.trim()
                val sAlt = s.lowercase(Locale("tr"))
                val baslikMi = s.startsWith("- [ ]") || s.startsWith("[ ]") || s.startsWith("TODO:") ||
                        s.startsWith("YAP:") || s.startsWith("TODO ") || s.startsWith("• ")
                val eylemMi = gorevEylemleri.any { sAlt.endsWith(it) || sAlt.contains("$it ") }

                if (baslikMi || (s.length in 5..80 && eylemMi && !s.endsWith("."))) {
                    val temiz = s.removePrefix("- [ ]").removePrefix("[ ]").removePrefix("TODO:").removePrefix("YAP:")
                        .removePrefix("• ").trim()
                    if (temiz.length >= 3) {
                        bulgular.add(temiz)
                    }
                }
            }
        }
        return bulgular.toList()
    }

    /**
     * Otopilot Hedef Karar Mekanizması:
     * Uykusu az olan veya takvimi aşırı yoğun olan kullanıcılar için günlük odak hedefini güvenli esnek seviyeye kısıtlar.
     */
    fun otopilotHedefHesapla(mevcutHedefDk: Int, uykuSuresiMs: Long, takvimYogunlukDk: Int): Int {
        val azUyku = uykuSuresiMs > 0L && uykuSuresiMs < 6 * 3600_000L
        val yogunGun = takvimYogunlukDk >= 180
        return when {
            azUyku && yogunGun -> (mevcutHedefDk * 0.65).toInt().coerceIn(20, 120)
            azUyku || yogunGun -> (mevcutHedefDk * 0.80).toInt().coerceIn(25, 180)
            else -> mevcutHedefDk.coerceIn(25, 480)
        }
    }
}
