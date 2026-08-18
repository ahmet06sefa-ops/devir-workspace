package com.gunlukasistan.app

/**
 * v10.33 · Katalog #15 — seçili görevlerin metne dökümü (saf, JVM testli).
 * WhatsApp/mesajlaşma için hazır blok üretir: "☑/☐ metin · ⏰ tarih".
 */
object GorevDisAktar {

    data class Satir(val metin: String, val bitti: Boolean, val tarihMetin: String?)

    /** Başlık satırı + görev satırları. Boş liste yalnız başlığı döndürür. */
    fun metin(satirlar: List<Satir>, baslik: String): String {
        val sb = StringBuilder(baslik.trim())
        for (s in satirlar) {
            sb.append('\n')
            sb.append(if (s.bitti) "☑ " else "☐ ")
            sb.append(s.metin.trim())
            val t = s.tarihMetin
            if (!t.isNullOrBlank()) sb.append(" · ⏰ ").append(t.trim())
        }
        return sb.toString()
    }
}
