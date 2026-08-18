package com.gunlukasistan.app

/**
 * v11.13 — Görev takvimi / yaklaşan görev motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Yeni bir özellik ekle." Rakiplerde (TickTick) olan ve
 * değerli bir özellik: görevleri tarihe bağlama ve yaklaşan görevleri görme.
 * Bu motor, görevi bir güne bağlar ve "yaklaşan" (bugün/yarın/gecikmiş) sınıflandırır.
 *
 *  · [Gorev] — görev + hedef gün anahtarı ("yyyyMMdd").
 *  · [guneAta] — görevi bir güne bağlar.
 *  · [durum] — hedef güne göre "gecikti / bugün / yarın / ileri" sınıflar.
 *  · [yaklasanlar] — bugün+yarın dahil yaklaşan görevleri listeler.
 */
object GorevTakvimiMotoru {

    data class Gorev(val ad: String, val gunAnahtar: String)

    /** Görevi bir güne bağlar. */
    fun guneAta(ad: String, gunAnahtar: String): Gorev = Gorev(ad, gunAnahtar)

    /** Bugünün anahtarından farkı (gün cinsinden). */
    fun gunFarki(gunAnahtar: String, bugunAnahtar: String): Int {
        val g = gunAnahtar.toIntOrNull() ?: return 0
        val b = bugunAnahtar.toIntOrNull() ?: return 0
        return g - b
    }

    /** Durum: "gecikti" / "bugun" / "yarin" / "ileri" / "bilinmeyen". */
    fun durum(gunAnahtar: String, bugunAnahtar: String): String {
        val fark = gunFarki(gunAnahtar, bugunAnahtar)
        return when {
            fark < 0 -> "gecikti"
            fark == 0 -> "bugun"
            fark == 1 -> "yarin"
            else -> "ileri"
        }
    }

    /** Bugün + yarın olan yaklaşan görevleri listeler. */
    fun yaklasanlar(gorevler: List<Gorev>, bugunAnahtar: String): List<Gorev> {
        val bugun = gunAnahtarInt(bugunAnahtar)
        return gorevler.filter { g ->
            val f = g.gunAnahtar.toIntOrNull() ?: return@filter false
            f == bugun || f == bugun + 1
        }
    }

    private fun gunAnahtarInt(a: String): Int = a.toIntOrNull() ?: 0

    /** Gecikmiş görevleri listeler. */
    fun gecikenler(gorevler: List<Gorev>, bugunAnahtar: String): List<Gorev> {
        val bugun = gunAnahtarInt(bugunAnahtar)
        return gorevler.filter { g -> (g.gunAnahtar.toIntOrNull() ?: 0) < bugun }
    }
}
