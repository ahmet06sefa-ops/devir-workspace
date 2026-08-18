package com.gunlukasistan.app

/**
 * v11.13 — Başarı / istatistik analiz motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Devam et." Rakiplerde (Habitify/Daylio) olan ve değerli
 * bir özellik: dönemsel başarı raporu. Bu motor, tamamlama ve odak verisinden
 * anlamlı özetler üretir.
 *
 *  · [basariOrani] — tamamlanan / toplam → 0..100.
 *  · [seriOrani] — aktif gün / toplam gün → 0..100 (istikrar).
 *  · [durumNotu] — başarı oranına göre kısa değerlendirme cümlesi.
 *  · [ayRaporu] — günlük veriden aylık özet üretir.
 */
object BasariAnalizMotoru {

    /** Tamamlanan / toplam → 0..100 başarı oranı. */
    fun basariOrani(tamamlanan: Int, toplam: Int): Int {
        if (toplam <= 0) return 0
        return (tamamlanan.coerceIn(0, toplam) * 100 / toplam).coerceIn(0, 100)
    }

    /** Aktif gün / toplam gün → 0..100 istikrar oranı. */
    fun seriOrani(aktifGun: Int, toplamGun: Int): Int {
        if (toplamGun <= 0) return 0
        return (aktifGun.coerceIn(0, toplamGun) * 100 / toplamGun).coerceIn(0, 100)
    }

    /** Başarı oranına göre kısa değerlendirme. */
    fun durumNotu(basari: Int): String = when {
        basari >= 80 -> "Harika istikrar — devam et! 🏆"
        basari >= 60 -> "Güzel ilerleme — az kaldı. 💪"
        basari >= 40 -> "Orta tempo — bir adım daha at. 🌱"
        basari >= 20 -> "Zorlanıyor — küçük hedeflerle başla. 🌤️"
        else -> "Sakin bir dönem — sorun değil, taze başla. 🍃"
    }

    /**
     * Aylık rapor üretir.
     * @param toplamGun ayın gün sayısı
     * @param aktifGun veri olan gün sayısı
     * @param tamamlanan toplam tamamlama
     * @param odakDk toplam odak dakikası
     */
    fun ayRaporu(toplamGun: Int, aktifGun: Int, tamamlanan: Int, odakDk: Int): String {
        val seri = seriOrani(aktifGun, toplamGun)
        val ortalama = tamamlanan / aktifGun.coerceAtLeast(1)
        return buildString {
            append("📊 Aylık Başarı Raporu\n")
            append("İstikrar: %$seri ($aktifGun/$toplamGun gün aktif)\n")
            append("Toplam: $tamamlanan tamamlama · $odakDk dk odak\n")
            append("Günlük ort.: $ortalama tamamlama\n")
            append(durumNotu(seri))
        }
    }
}
