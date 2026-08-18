package com.gunlukasistan.app

/**
 * v10.29 · Katalog #27 + #28 — not editörü ölçüm ve satır ayıklama.
 * Saf mantık (android YOK), JVM testli: [NotOlcumTest].
 */
object NotOlcum {

    /** Kelime sayısı: boşluk/sekme/satır-sonu dizileri tek ayraç sayılır. */
    fun kelimeS(metin: String): Int =
        if (metin.isBlank()) 0 else metin.trim().split(Regex("\\s+")).size

    /** Karakter sayısı (ham uzunluk — kullanıcının beklediği "her şey dahil"). */
    fun karakterS(metin: String): Int = metin.length

    /**
     * Katalog #28: not satırlarından görev metni ayıklar.
     * Baştaki liste işaretlerini soyar: "- ", "* ", "• ", "[ ]", "[x]", "☐ ", "☑ ", "✓ " vb.
     * Boş satırlar atılır, iki ucu boşaltılır.
     */
    fun satirlariAyikla(metin: String): List<String> {
        val isaret = Regex("^\\s*(?:[-*•▪◦]|\\[[ xX✓✔]?\\]|☐|☑|✓|✔)\\s+")
        return metin.lines()
            .map { isaret.replace(it, "").trim() }
            .filter { it.isNotEmpty() }
    }
}
