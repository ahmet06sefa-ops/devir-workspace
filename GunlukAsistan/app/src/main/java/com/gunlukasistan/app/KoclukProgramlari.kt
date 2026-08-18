package com.gunlukasistan.app

/**
 * v11.13 — Hazır koçluk / rutin programları (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (Fabulous) olup bende olmayan hazır, adım
 * adım rehberli programlar ekle."
 *
 * Bu motor, sabit program şablonlarını ve ilerlemelerini yönetir. Her program
 * bir dizi günlük görevden oluşur; kullanıcı gün gün ilerler.
 *
 *  · [Program] — ad, emoji, günlük görev listesi, toplam gün.
 *  · [varsayilanlar] — hazır program kütüphanesi (ders, erken kalk, odak…).
 *  · [gunGorevi] — verilen günün görev metni.
 *  · [ilerlemeYuzde] — tamamlanan gün / toplam gün.
 */
object KoclukProgramlari {

    data class Program(
        val id: String,
        val ad: String,
        val emoji: String,
        val gunler: List<String>
    ) {
        val toplamGun: Int get() = gunler.size
    }

    /** Hazır program kütüphanesi. */
    val varsayilanlar: List<Program> = listOf(
        Program(
            "ders_aliskani",
            "Ders Çalışma Alışkanlığı",
            "📚",
            listOf(
                "1 odak bloğu (25 dk) tamamla",
                "2 odak bloğu tamamla",
                "2 odak bloğu + 10 soru",
                "3 odak bloğu tamamla",
                "3 odak bloğu + 15 soru",
                "Günün konusunu tekrar et",
                "3 odak bloğu + tekrar",
                "4 odak bloğu + 20 soru",
                "4 odak bloğu + eksik konu",
                "Haftalık özet çıkar"
            )
        ),
        Program(
            "erken_kalk",
            "Erken Kalkma",
            "🌅",
            listOf(
                "Saat 07:00'dan önce kalk",
                "Saat 07:00'dan önce kalk + su iç",
                "Saat 06:45'ten önce kalk",
                "Kalkınca 10 dk ders",
                "06:30'dan önce kalk",
                "Sabah rutinini tamamla",
                "06:30 + sabah rutin + kahvaltı"
            )
        ),
        Program(
            "odak_ustasi",
            "Odak Ustası",
            "🎯",
            listOf(
                "1 pomodoro (25 dk) tamamla",
                "2 pomodoro tamamla",
                "3 pomodoro tamamla",
                "4 pomodoro + mola düzeni",
                "5 pomodoro tamamla",
                "6 pomodoro + gün özeti"
            )
        )
    )

    /** id'ye göre program bulur. */
    fun bul(id: String): Program? = varsayilanlar.firstOrNull { it.id == id }

    /** Verilen günün (1-tabanlı) görev metni. Sınır aşılırsa son görev. */
    fun gunGorevi(p: Program, gun: Int): String =
        p.gunler.getOrElse((gun - 1).coerceIn(0, p.toplamGun - 1)) { p.gunler.last() }

    /** Tamamlanan gün / toplam gün → 0..100. */
    fun ilerlemeYuzde(tamamlananGun: Int, toplamGun: Int): Int {
        if (toplamGun <= 0) return 0
        return (tamamlananGun.coerceIn(0, toplamGun) * 100 / toplamGun).coerceIn(0, 100)
    }

    /** Program adını içeren/benzer bir program bulur (arama). */
    fun ara(metin: String): Program? {
        val m = metin.trim().lowercase()
        if (m.isBlank()) return null
        return varsayilanlar.firstOrNull {
            it.ad.lowercase().contains(m) || it.emoji == metin.trim()
        }
    }
}
