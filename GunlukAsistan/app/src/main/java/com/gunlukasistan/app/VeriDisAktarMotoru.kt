package com.gunlukasistan.app

/**
 * v11.13 — Zengin veri dışa aktarma / paylaşım motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Zengin görsel/PDF rapor paylaşımı zayıf; hepsini ekle."
 * Bu motor, uygulama verisinden biçimlendirilmiş, paylaşılabilir metin
 * raporları üretir. Görsel kart üretimi (`KartUretici`) ayrı; burada
 * metin/markdown rapor katmanı vardır.
 *
 *  · [raporBasligi] — "GG-AA-YYYY Günlük Asistan Raporu" başlığı.
 *  · [gorevOzeti] — görev tamamlanma özeti.
 *  · [odakOzeti] — odak/hedef özeti.
 *  · [markdownRapor] — hepsini tek markdown raporda birleştirir.
 */
object VeriDisAktarMotoru {

    /** Başlık: "15-08-2026 Günlük Asistan Raporu". */
    fun raporBasligi(gunAnahtar: String): String {
        val gun = gunAnahtar.substring(6)
        val ay = gunAnahtar.substring(4, 6)
        val yil = gunAnahtar.substring(0, 4)
        return "$gun-$ay-$yil Günlük Asistan Raporu"
    }

    /** Görev özeti satırı: "Bekleyen X · Tamamlanan Y · Toplam Z". */
    fun gorevOzeti(bekleyen: Int, tamamlanan: Int): String =
        "Bekleyen $bekleyen · Tamamlanan $tamamlanan · Toplam ${bekleyen + tamamlanan}"

    /** Odak özeti: "Odak 60/90 dk (%67)". */
    fun odakOzeti(odakDk: Int, hedefDk: Int): String {
        if (hedefDk <= 0) return "Odak $odakDk dk (hedef belirsiz)"
        val yuzde = (odakDk * 100 / hedefDk).coerceIn(0, 999)
        return "Odak $odakDk/$hedefDk dk (%$yuzde)"
    }

    /** Her şeyi tek markdown raporunda birleştirir. */
    fun markdownRapor(
        baslik: String,
        gorevSatiri: String,
        odakSatiri: String,
        kursSatiri: String? = null,
        notlar: List<String> = emptyList()
    ): String = buildString {
        append("# ").append(baslik).append("\n\n")
        append("## ✅ Görevler\n").append(gorevSatiri).append("\n\n")
        append("## ⏱️ Odak\n").append(odakSatiri).append("\n")
        if (!kursSatiri.isNullOrBlank()) {
            append("\n## 🎓 Kurslar\n").append(kursSatiri).append("\n")
        }
        if (notlar.isNotEmpty()) {
            append("\n## 📝 Notlar\n")
            notlar.forEach { append("- ").append(it).append("\n") }
        }
    }
}
