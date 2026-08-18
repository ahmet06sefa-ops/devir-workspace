package com.gunlukasistan.app

/**
 * v10.18 · EKRAN ATÖLYESİ — iki ekranın (Ana ekran, Bugün) ortak saf mantığı.
 *
 * Android bağımlılığı YOK: her fonksiyon saf JUnit ile test edilir.
 * (`DuzenTest` bu tabloları kilitler.)
 */
object DuzenCekirdek {

    // ---------------- Boyut nefesi (yoğunluk) ----------------

    /**
     * Boyut kademesi → bloğun dikey nefesi (dp).
     * 0 = Kompakt · 1 = Normal (v10.16 öncesi his) · 2 = Geniş.
     * Taşan değer kelepçelenir.
     */
    fun boyutNefesDp(kademe: Int): Int = when (kademe.coerceIn(0, 2)) {
        0 -> 2; 2 -> 14; else -> 6
    }

    // ---------------- Boyut kaydı diziçimi ----------------
    //
    // Biçim: "kod:kademe,kod:kademe" — tek string pref'te tutulur.
    // Bozuk parçalar sessizce atlanır (eski/bozuk kayıtla uygulama
    // çökmesin; varsayılana düşer).

    /** Kayıt dizisini kod→kademe haritasına çevirir. */
    fun boyutKayitOku(kayit: String): Map<String, Int> {
        if (kayit.isBlank()) return emptyMap()
        val harita = LinkedHashMap<String, Int>()
        kayit.split(",").forEach { parca ->
            val i = parca.indexOf(':')
            if (i <= 0) return@forEach
            val kod = parca.substring(0, i).trim()
            val kademe = parca.substring(i + 1).trim().toIntOrNull() ?: return@forEach
            if (kod.isNotEmpty()) harita[kod] = kademe.coerceIn(0, 2)
        }
        return harita
    }

    /** Haritayı kayıt dizisine çevirir (yazarken kelepçelenir). */
    fun boyutKayitYaz(harita: Map<String, Int>): String =
        harita.entries.joinToString(",") { (kod, kademe) ->
            "$kod:${kademe.coerceIn(0, 2)}"
        }

    // ---------------- Taşıma ----------------

    /**
     * Bloku listede yukarı ([yon] = -1) ya da aşağı (+1) taşır.
     * Sınırda ve bilinmeyen kodda liste değişmez — çağıran her durumda
     * GERİ YAZSA bile görünüm korunur.
     */
    fun tasi(kodlar: List<String>, kod: String, yon: Int): List<String> {
        val i = kodlar.indexOf(kod)
        if (i < 0) return kodlar
        val j = i + if (yon < 0) -1 else 1
        if (j !in kodlar.indices) return kodlar
        val yeni = kodlar.toMutableList()
        yeni[i] = kodlar[j]
        yeni[j] = kodlar[i]
        return yeni
    }
}
