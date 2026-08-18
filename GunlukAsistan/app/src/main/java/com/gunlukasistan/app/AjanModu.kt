package com.gunlukasistan.app

/**
 * v11.13 — Ajan modu: çok adımlı hedef planlayıcı (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Bugün 4 saati tamamla" gibi bir hedef söyleyince AI tek
 * komut değil, ADIM ADIM bir plan kursun ve yürütsün. Bu motor, hedefi
 * [AsistanKomut.Komut] dizisine çevirir; her adım görünür ve sıralı işlenir.
 *
 *  · [planaCevir] — "3 saat çalış, 20 soru çöz" → çalışma adımı + görev adımı.
 *  · [hedefDk] — "4 saat", "250 dk" gibi metinden dakika çıkarır.
 *  · [adimSayisi] — planın kaç adım olduğunu verir.
 *
 * Gerçek yürütme, adım listesinin [AdimliEylemMotoru]'na verilmesiyle olur.
 */
object AjanModu {

    /** Ajan planı: hedef + üretilen komut adımları. */
    data class AjanPlan(val hedef: String, val adimlar: List<AsistanKomut.Komut>)

    /**
     * Metinden toplam çalışma dakikası çıkarır (SAF).
     * YALNIZCA açık bir süre birimi varsa sayar: "N saat", "N dk", "N dakika".
     * Çıplak bir sayı ("30 sayfa oku") veya hiç süre yoksa 0 döner — böylece
     * rastgele sayı içeren mesajlar ajan modunu tetiklemez.
     */
    fun hedefDk(metin: String): Int {
        val t = metin.trim().lowercase()
        val saat = Regex("(\\d+)\\s*saat").find(t)?.groupValues?.get(1)?.toIntOrNull()
        if (saat != null) return (saat * 60).coerceIn(10, 720)
        val dk = Regex("(\\d+)\\s*(dk|dakika|dakikasi)").find(t)?.groupValues?.get(1)?.toIntOrNull()
        if (dk != null) return dk.coerceIn(5, 720)
        return 0
    }

    /**
     * Ajan modunu tetiklemek için GÜVENLİ karar (SAF, JVM testli).
     *
     * Yalnızca kullanıcı AÇIKÇA bir çalışma hedefi/planı istiyorsa true:
     *  · mesaj "ajan" sözcüğünü içeriyorsa VEYA
     *  · "N saat / N dk / N dakika" biçiminde bir süre VARSÇA VE mesaj bir
     *    çalışma/odaklanma fiili içeriyorsa (çalış, odaklan, çöz, plan, hedef).
     *
     * Aksi hâlde false — normal sohbete devam edilir (zamanlayıcıya takılmaz).
     */
    fun ajanModuGerekliMi(metin: String): Boolean {
        val t = metin.trim().lowercase()
        if (t.isBlank()) return false
        if (t.contains("ajan")) return true

        val sureVar = Regex("\\d+\\s*(saat|dk|dakika|dakikasi)").containsMatchIn(t)
        if (!sureVar) return false

        val calismaFiili = t.contains("çalış") || t.contains("calis") ||
            t.contains("odaklan") || t.contains("çöz") || t.contains("coz") ||
            t.contains("plan") || t.contains("hedef") || t.contains("kurs") ||
            t.contains("ders")
        return calismaFiili
    }

    /**
     * Bir hedefi çalışma adımlarına çevirir.
     *  · Toplam süreyi 25-50 dk'lık parçalara böler → her parça bir "odak" adımı.
     *  · Metinde "N soru" varsa bir görev adımı ekler.
     *  · Sonuca bir "özet" adımı ekler.
     */
    fun planaCevir(hedef: String): AjanPlan {
        val dk = hedefDk(hedef)
        val adimlar = mutableListOf<AsistanKomut.Komut>()
        // Süre varsa 25'er dakikalık odak bloklarına böl (en fazla 6 blok)
        if (dk > 0) {
            val blok = 25
            var kalan = dk
            var blokNo = 0
            while (kalan > 0 && blokNo < 6) {
                val sure = minOf(blok, kalan)
                adimlar.add(AsistanKomut.Komut("zamanlayici", "$sure"))
                kalan -= sure
                blokNo++
            }
        }
        // "N soru" varsa görev ekle
        val soru = Regex("(\\d+)\\s*soru").find(hedef.lowercase())?.groupValues?.get(1)?.toIntOrNull()
        if (soru != null) {
            adimlar.add(AsistanKomut.Komut("gorev_ekle", "$soru soru çöz (ajan planı)"))
        }
        // Özet adımı
        adimlar.add(AsistanKomut.Komut("ozet_ver", ""))
        return AjanPlan(hedef = hedef.trim(), adimlar = adimlar)
    }

    /** Plan kaç adımdan oluşuyor? */
    fun adimSayisi(plan: AjanPlan): Int = plan.adimlar.size
}
