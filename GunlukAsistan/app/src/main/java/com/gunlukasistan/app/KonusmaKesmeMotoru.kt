package com.gunlukasistan.app

/**
 * v11.13 — Konuşma kesme (dur/kes) dedektörü (SAF, JVM testli).
 *
 * Kullanıcı isteği: sesli oturumda "dur", "kes", "yeter" deyince AI hemen
 * susmalı ve yeni komut dinlemeli. Bu motor, konuşma tanıma çıktısında
 * kesme sözcükleri arayıp karar verir.
 */
object KonusmaKesmeMotoru {

    /** Kesme komutu algılandı mı? */
    fun kesmeMi(ses: String): Boolean {
        val t = ses.trim().lowercase()
        if (t.isBlank()) return false
        return t == "dur" || t == "kes" || t == "yeter" || t == "bırak" ||
            t == "tamam dur" || t == "dur dur" || t == "sus" ||
            t.startsWith("dur ") || t.startsWith("kes ") ||
            t.endsWith(" dur") || t.endsWith(" kes")
    }

    /** Kesme komutunu sohbet göndermeden filtrele (kullanıcıya iletilmesin). */
    fun kesmeIse(ses: String): Boolean = kesmeMi(ses)
}
