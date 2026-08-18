package com.gunlukasistan.app

/**
 * v10.30 · Katalog #33 — iki notun başlık/içerik birleşimi (saf, JVM testli).
 * Görsel alanı hedef notta kalır (diğer notun görseli taşınmaz; not silme
 * geri alınabilir olduğundan kaybolmaz).
 */
object NotBirlestir {

    /** Dolu olmayan parçaları atar; kalanları boş satırla birleştirir. */
    fun govde(a: String, b: String): String =
        listOf(a.trim(), b.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")

    /** Başlık birleşimi: "A · B"; tek doluysa o; ikisi de boşsa boş. Üst sınır 80. */
    fun baslik(a: String, b: String): String =
        listOf(a.trim(), b.trim()).filter { it.isNotEmpty() }.joinToString(" · ").take(80)
}
