package com.gunlukasistan.app

/**
 * v11.13 — Çok dillilik (i18n) seçici motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Uygulamada çok dillilik yok; hepsini ekle." Gerçek çeviri
 * string'lerinin tamamı ayrı bir yerelleştirme projesidir; bu motor, hangi
 * dilin seçili olduğu, desteklenen diller ve anahtar tabanlı çeviri araması
 * mantığını taşır.
 *
 *  · [Dil] — desteklenen diller (kod + ad).
 *  · [desteklenen] — seçilebilir diller.
 *  · [varsayilan] — Türkçe.
 *  · [yaziYonu] — dilin RTL/LTR olduğunu söyler.
 */
object DilSeciciMotoru {

    data class Dil(val kod: String, val ad: String, val anaAd: String)

    /** Desteklenen diller. Türkçe öncelikli; İngilizce, Almanca, Fransızca, Arapça. */
    val desteklenen: List<Dil> = listOf(
        Dil("tr", "Türkçe", "Türkçe"),
        Dil("en", "English", "İngilizce"),
        Dil("de", "Deutsch", "Almanca"),
        Dil("fr", "Français", "Fransızca"),
        Dil("ar", "العربية", "Arapça"),
        Dil("es", "Español", "İspanyolca"),
        Dil("ru", "Русский", "Rusça")
    )

    val varsayilan: Dil = desteklenen[0]

    /** Kod → Dil; bilinmiyorsa varsayılan (Türkçe). */
    fun dil(kod: String): Dil = desteklenen.firstOrNull { it.kod == kod.lowercase() } ?: varsayilan

    /** Dil RTL mi (sağdan sola)? */
    fun rtlMi(kod: String): Boolean = kod.lowercase() == "ar"

    /**
     * Anahtar tabanlı çeviri araması: çeviri haritasında yoksa anahtarı döndürür.
     * (Gerçek .xml yerelleştirmesine köprü.)
     */
    fun ceviri(ceviriler: Map<String, String>, anahtar: String): String =
        ceviriler[anahtar] ?: anahtar

    /** Uygulamanın varsayılan ayarlarına göre seçili dil. "en-US" → "en". */
    fun seciliKod(yerelDil: String): String {
        val kod = yerelDil.lowercase().substringBefore("-").substringBefore("_")
        return desteklenen.firstOrNull { it.kod == kod }?.kod ?: varsayilan.kod
    }
}
