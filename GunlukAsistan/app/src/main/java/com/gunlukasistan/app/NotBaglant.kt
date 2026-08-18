package com.gunlukasistan.app

/**
 * v10.30 · Katalog #31 — nottaki ilk web bağlantısını bulur (saf, JVM testli).
 * Şemasız "www." ile başlayanlara https:// eklenir ki ACTION_VIEW çalışsın.
 */
object NotBaglant {

    private val URL_RX = Regex("(?:https?://|www\\.)[^\\s<>\"'\\)\\]]+", RegexOption.IGNORE_CASE)

    /** Metindeki ilk URL; yoksa null. "www.ornek.com" → "https://www.ornek.com" */
    fun ilkUrl(metin: String): String? {
        val ham = URL_RX.find(metin)?.value ?: return null
        return if (ham.startsWith("www.", ignoreCase = true)) "https://$ham" else ham
    }
}
