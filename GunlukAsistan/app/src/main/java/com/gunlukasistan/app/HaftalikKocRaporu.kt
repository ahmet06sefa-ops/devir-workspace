package com.gunlukasistan.app

/**
 * v11.13 — Haftalık Koç Raporu üretici (SAF, JVM testli).
 *
 * Kullanıcı isteği: hafta sonunda AI'nın görsel/özet bir rapor üretmesi.
 * Bu motor, haftalık veriden tek paragraflık şık bir koç raporu metni üretir.
 *
 *  · [satinAl] — odak, görev, kurs verisinden haftalık rapor.
 *  · [derece] — odak dakikasını yıldız/derece etiketine çevirir.
 */
object HaftalikKocRaporu {

    /** Haftalık veri özeti. */
    data class Hafta(
        val odakDk: Int,
        val hedefDk: Int,
        val tamamlananGorev: Int,
        val kursYuzde: Int,
        val seriGun: Int
    )

    /**
     * Haftalık rapor metni. Tüm alanlar veriye dayanır (saf).
     */
    fun satinAl(h: Hafta): String {
        val hedefYuzde = if (h.hedefDk > 0) (h.odakDk * 100.0 / h.hedefDk).toInt().coerceIn(0, 999) else 100
        val derece = derece(h.odakDk)
        return buildString {
            append("📊 Haftalık Koç Raporu\n")
            append("Odak: $derece — toplam ${h.odakDk} dk (hedefin ${h.hedefDk} dk → %$hedefYuzde).\n")
            if (h.tamamlananGorev > 0) append("Görev: ${h.tamamlananGorev} tanesini tamamladın.\n")
            if (h.kursYuzde > 0) append("Kurs: genel ilerlemen %${h.kursYuzde}.\n")
            if (h.seriGun > 0) append("Seri: $h.seriGun gün üst üste çalıştın.\n")
            append(yorum(h))
        }
    }

    /** Odak dakikasını kısa bir derece etiketine çevirir. */
    fun derece(odakDk: Int): String = when {
        odakDk >= 1200 -> "★★★★★ (çok yoğun)"
        odakDk >= 700 -> "★★★★☆ (istikrarlı)"
        odakDk >= 350 -> "★★★☆☆ (orta-üstü)"
        odakDk >= 150 -> "★★☆☆☆ (düşük-orta)"
        else -> "★☆☆☆☆ (sakin)"
    }

    private fun yorum(h: Hafta): String = when {
        h.odakDk >= 1200 -> "Olağanüstü hafta! Bu tempoyu sürdür ama molaları unutma."
        h.seriGun >= 5 -> "Uzun serin var — kopmayı önlemek için yarın küçük de olsa bir adım at."
        h.odakDk >= 350 -> "Güzel ilerleme. Önümüzdeki hafta %10 daha hedefleyebilirsin."
        else -> "Sakin bir haftaydı; sorun değil. Önümüzdeki hafta için tek net hedef belirle."
    }
}
