package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.50 — Kullanıcı maddesi #11..#20: 10 Aşırı İşlevsel Zamanlayıcı & Odak Özelliği.
 * Kesinti günlüğü, göreve bağlı sayaç, yorgunluk radarı, akıllı taşma, çıktı hasadı,
 * ses mikseri, çarpışma bekçisi, proje bütçesi, masaya dönüş geri sayımı ve kilit paneli (saf, JVM testli).
 */
object OdakMotoru {

    // 1. #1 Akıllı Kesinti & Bölünme Günlüğü
    enum class KesintiSebep {
        TELEFON,
        KAPI,
        IHTIYAC,
        ZIHIN,
        DIGER
    }

    data class KesintiKaydi(
        val sebep: KesintiSebep,
        val sureSn: Int,
        val damgaMs: Long
    )

    data class KesintiOzeti(
        val toplamSayi: Int,
        val enCokSebep: KesintiSebep?,
        val enCokSayi: Int,
        val bilgiMetni: String
    )

    fun kesintiAnalizEt(kayitlar: List<KesintiKaydi>): KesintiOzeti {
        if (kayitlar.isEmpty()) {
            return KesintiOzeti(0, null, 0, "Kesintisiz temiz odak seansı.")
        }
        val gruplu = kayitlar.groupingBy { it.sebep }.eachCount()
        val enCok = gruplu.maxByOrNull { it.value }
        val sebep = enCok?.key
        val adet = enCok?.value ?: 0
        val sebepAd = when (sebep) {
            KesintiSebep.TELEFON -> "Telefon / Mesaj"
            KesintiSebep.KAPI -> "Biri Geldi / Kapı"
            KesintiSebep.IHTIYAC -> "İhtiyaç Molası"
            KesintiSebep.ZIHIN -> "Zihnim Dağıldı"
            else -> "Diğer"
        }
        val dize = "En çok '$sebepAd' sebebiyle ($adet kez) bölündünüz. Toplam ${kayitlar.size} kesinti."
        return KesintiOzeti(kayitlar.size, sebep, adet, dize)
    }

    // 2. #2 Göreve Bağlı Zamanlayıcı
    fun gorevTamamlamaKarari(gorevId: Long, odanilanDk: Int): Boolean {
        return gorevId > 0L && odanilanDk >= 5
    }

    // 3. #3 Biyolojik Ritim & Dikkatin Dağılma Eşiği Radarı
    data class YorgunlukDurumu(
        val riskliMi: Boolean,
        val onerilenMolaDk: Int,
        val mesaj: String
    )

    fun yorgunlukRadari(kesintiSayisi: Int, gecenDk: Int): YorgunlukDurumu {
        if (kesintiSayisi >= 3 && gecenDk <= 20) {
            return YorgunlukDurumu(
                riskliMi = true,
                onerilenMolaDk = 5,
                mesaj = "🧠 Dikkatiniz dağılmaya başladı ($kesintiSayisi kesinti). 5 dk mikro nefes molası önerilir."
            )
        }
        if (gecenDk >= 18 && kesintiSayisi >= 2) {
            return YorgunlukDurumu(
                riskliMi = true,
                onerilenMolaDk = 5,
                mesaj = "🧠 Zihinsel eşiğe ulaştınız. Kendinizi zorlamak yerine 5 dk mola ile tazeleyelim."
            )
        }
        return YorgunlukDurumu(
            riskliMi = false,
            onerilenMolaDk = 0,
            mesaj = "Odak ritminiz stabil."
        )
    }

    // 4. #4 Akıllı Taşma (Overrun) Modu
    fun tasmaSuresiHesapla(bitisDamgaMs: Long, simdiMs: Long, tasmaAcik: Boolean): Long {
        if (!tasmaAcik || bitisDamgaMs <= 0L) return 0L
        if (simdiMs > bitisDamgaMs) {
            return simdiMs - bitisDamgaMs
        }
        return 0L
    }

    fun tasmaMetni(tasmaMs: Long): String {
        val dk = (tasmaMs / 60_000L).toInt()
        val sn = ((tasmaMs % 60_000L) / 1000L).toInt()
        return "⚡ +%02d:%02d (Akış)".format(dk, sn)
    }

    // 5. #5 Oturum Sonu Çıktı Hasadı
    fun ciktiNotuFormatla(ciktiMetni: String, odakDk: Int, gorevAd: String?): String {
        val baslik = if (!gorevAd.isNullOrBlank()) {
            "[ODAK HASADI — $odakDk dk] $gorevAd"
        } else {
            "[ODAK HASADI — $odakDk dk]"
        }
        return "$baslik:\n💡 $ciktiMetni"
    }

    // 6. #6 Çift Katmanlı Ses Manzarası & Binaural Ritim Mikseri
    enum class BinauralFrekans {
        KAPALI,
        ALFA_10HZ,
        GAMA_40HZ
    }

    fun sesMikseriKarari(
        ortamAd: String,
        ortamYuzde: Int,
        binaural: BinauralFrekans,
        binauralYuzde: Int
    ): String {
        val bAd = when (binaural) {
            BinauralFrekans.ALFA_10HZ -> "10Hz Alfa"
            BinauralFrekans.GAMA_40HZ -> "40Hz Gama"
            BinauralFrekans.KAPALI -> ""
        }
        if (binaural == BinauralFrekans.KAPALI) {
            return "$ortamAd (%$ortamYuzde)"
        }
        return "$ortamAd (%$ortamYuzde) + $bAd (%$binauralYuzde)"
    }

    // 7. #7 Vakit & Toplantı Çarpışma Bekçisi
    data class CarpismaSonucu(
        val carpismaVar: Boolean,
        val guvenliDk: Int,
        val uyariMetni: String
    )

    fun carpismaDenetimi(istenenDk: Int, siradakiVakitDk: Int?): CarpismaSonucu {
        if (siradakiVakitDk != null && siradakiVakitDk in 4 until istenenDk) {
            val guvenli = maxOf(5, siradakiVakitDk - 3)
            return CarpismaSonucu(
                carpismaVar = true,
                guvenliDk = guvenli,
                uyariMetni = "⚠️ $siradakiVakitDk dk sonra vakit/etkinlik var. Sayacı $guvenli dk olarak kuralım mı?"
            )
        }
        return CarpismaSonucu(
            carpismaVar = false,
            guvenliDk = istenenDk,
            uyariMetni = ""
        )
    }

    // 8. #8 Proje & Ders Zaman Bütçesi Defteri
    fun projeButcesiEkle(mevcutDk: Int, seansDk: Int): Int {
        return (mevcutDk + seansDk).coerceIn(0, 99999)
    }

    // 9. #9 Mola Sonrası 'Masaya Dönüş' Geri Sayımı
    fun masayaDonusGeriSayim(molaBitimMs: Long, simdiMs: Long, beklemeSn: Int = 15): Int {
        if (molaBitimMs <= 0L || simdiMs <= molaBitimMs) return beklemeSn
        val gecenSn = ((simdiMs - molaBitimMs) / 1000L).toInt()
        val kalan = beklemeSn - gecenSn
        return kalan.coerceIn(0, beklemeSn)
    }

    // 10. #10 Kilit Ekranı Canlı Odak Kontrol Paneli Metni
    fun kilitPaneliMetni(kalanMs: Long, tasmaMs: Long, gorevAd: String?): String {
        val baslik = if (!gorevAd.isNullOrBlank()) " · $gorevAd" else ""
        if (tasmaMs > 0L) {
            val dk = (tasmaMs / 60_000L).toInt()
            val sn = ((tasmaMs % 60_000L) / 1000L).toInt()
            return "⚡ TAŞMA: +%02d:%02d$baslik".format(dk, sn)
        }
        val dk = (kalanMs.coerceAtLeast(0L) / 60_000L).toInt()
        val sn = ((kalanMs.coerceAtLeast(0L) % 60_000L) / 1000L).toInt()
        return "⚡ ODAK: %02d:%02d$baslik".format(dk, sn)
    }
}
