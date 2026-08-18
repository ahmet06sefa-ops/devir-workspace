package com.gunlukasistan.app

/**
 * v11.13 — Takvim üzerinde görev planlama çekirdeği (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (TickTick) olup bende olmayan takvimde
 * görev sürükle-planla ekle." Bu motor, bir görevi takvim gününe atama ve
 * haftalık plan üretme mantığını taşır (UI'ın sürükleme katmanı ayrı).
 *
 *  · [GorevPlan] — görev + hedef tarih anahtarı ("yyyyMMdd") + gün içi saat.
 *  · [guneAta] — görevi bir güne atar (tarih anahtarını kurar).
 *  · [haftalikDagilim] — bekleyen görevleri haftanın günlerine dengeli dağıtır.
 *  · [gunAnahtari] — tarihten "yyyyMMdd" anahtarı üretir.
 */
object TakvimPlanlamaMotoru {

    /** Bir görevin takvimdeki planı. */
    data class GorevPlan(
        val gorevAd: String,
        val gunAnahtar: String,   // "yyyyMMdd"
        val saatDk: Int = 540      // varsayılan 09:00
    )

    /** Görevi belirli bir güne atar (yeni plan). */
    fun guneAta(gorevAd: String, gunAnahtar: String, saatDk: Int = 540): GorevPlan =
        GorevPlan(gorevAd, gunAnahtar, saatDk.coerceIn(0, 1439))

    /**
     * Bekleyen görevleri haftanın günlerine dengeli dağıtır.
     * @param gorevler görev adları
     * @param gunAnahtarlari mevcut haftanın gün anahtarları (Pzt..Paz sıralı)
     * @return görev → gün eşlemesi
     */
    fun haftalikDagilim(gorevler: List<String>, gunAnahtarlari: List<String>): List<GorevPlan> {
        if (gorevler.isEmpty() || gunAnahtarlari.isEmpty()) return emptyList()
        val sonuc = mutableListOf<GorevPlan>()
        gorevler.forEachIndexed { i, g ->
            val gun = gunAnahtarlari[i % gunAnahtarlari.size]
            sonuc.add(guneAta(g, gun))
        }
        return sonuc
    }

    /** Yıl/ay/günden "yyyyMMdd" anahtarı üretir. */
    fun gunAnahtari(yil: Int, ay: Int, gun: Int): String =
        "%04d%02d%02d".format(yil, ay, gun)

    /** Planı "GG-AA: görev @ HH:MM" biçiminde okunur metne çevirir. */
    fun planMetni(p: GorevPlan): String =
        "${p.gunAnahtar.substring(6)}-${p.gunAnahtar.substring(4, 6)}: ${p.gorevAd} @ " +
            "%02d:%02d".format(p.saatDk / 60, p.saatDk % 60)
}
