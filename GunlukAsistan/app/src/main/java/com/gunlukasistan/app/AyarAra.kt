package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.6 · Öneri D46 — Ayar arama eşleme mantığı.
 *
 * ── Neden Türkçe katlama ──
 * Varsayılan `lowercase()` İngilizce kuralla çalışır ve "I"→"i",
 * "İ"→"i̠" üretir; kullanıcı "BİLDİRİM" yazdığında "bildirim"
 * satırı **eşleşmez**. tr-TR yerel kuralıyla katlanır:
 * "I"→"ı", "İ"→"i".
 *
 * ── Sorgu biçimi ──
 * Boşlukla ayrılan her sözcük metinde geçmeli (sözcük başına
 * contains) — "bild ses" hem "Bildirim" hem "ses" taşıyan satırı
 * bulur, aradaki kelimeleri önemsemez.
 *
 * Saf bölge — birim testli.
 */
object AyarAra {

    private val TR: Locale = Locale("tr", "TR")

    /**
     * Katlama: tr-TR küçük harf kuralı + noktalı/noktasız i birleşimi.
     *
     * İkinci adım bilinçli: klavyeden yazılan "BILDIRIM" tr kuralıyla
     * "bıldırım" olur ama hedef "bildirim" noktalı i taşır — iki i
     * türü eşleşmeden birbirine katlanmadan arama yarım kalır. Arama
     * affedicidir: ı → i katlanır.
     */
    fun normal(s: String): String =
        s.trim().lowercase(TR).replace('ı', 'i')

    /** Sorgu boşsa her şey eşleşir (filtre yok). */
    fun eslesme(sorgu: String, metin: String): Boolean {
        val sor = normal(sorgu)
        if (sor.isEmpty()) return true
        val hedef = normal(metin)
        return sor.split(Regex("\\s+")).all { hedef.contains(it) }
    }
}
