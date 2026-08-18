package com.gunlukasistan.app

/**
 * v11.13 — Kesintisiz sesli asistan çevriminin SAF mantığı (JVM testli).
 *
 * Kullanıcı isteği: "Sadece sesli anlatım değil, normal ChatGPT/Gemini gibi
 * sesli asistan kur." Bu modül, sesli asistan oturumunun **karar** katmanını
 * yönetir (Android API gerektirmez):
 *
 *   · [sesliSoruTemizle] — konuşma tanıma çıktısını sohbete hazırlar.
 *   · [surekliDinlemeliMi] / [yeniTur] / [turSiniri] — cevap verdikten sonra
 *     "tekrar dinle" döngüsünü yönetir (eller serbest sohbet; sınırsız değil).
 *   · [konusulabilirCevap] — AI cevabını TTS'in temiz okuyacağı sözlü metne
 *     çevirir (eylem ön ekleri ve işaret simgeleri ayıklanır).
 *
 * Ses yakalama (SpeechRecognizer), AI çağrısı (AsistanBrain / AiClient) ve
 * seslendirme (AsistanSeslendirici) ayrı katmanlarda; bu modül yalnızca
 * "ne zaman dinle, neyi oku" kararlarını verir.
 */
object SesliAsistanModu {

    /** Sınırsız döngüyü önlemek için bir oturumdaki azami konuşma turu. */
    const val MAKS_TUR = 12

    /** Sesli asistan oturumunun anlık durumu (UI göstergesi). */
    enum class Durum { KAPALI, DINLIYOR, DUSUNUYOR, KONUSUYOR }

    /**
     * Konuşma tanıma çıktısını sohbet sorusuna hazırlar.
     * Boşlukları birleştirir, uçları keser; boş kalırsa "" döner.
     */
    fun sesliSoruTemizle(ses: String): String {
        val s = ses.trim().replace(Regex("\\s+"), " ")
        return s
    }

    /**
     * Cevap verdikten sonra tekrar dinlemeli mi (kesintisiz sohbet)?
     * Oturum aktif VE tur sınırı aşılmamış olmalı.
     */
    fun surekliDinlemeliMi(oturumAktif: Boolean, tur: Int): Boolean =
        oturumAktif && tur < MAKS_TUR

    /** Tur sınırına ulaşıldı mı? */
    fun turSiniri(tur: Int): Boolean = tur >= MAKS_TUR

    /** Bir sonraki tur sayacı. */
    fun yeniTur(tur: Int): Int = tur + 1

    /**
     * AI cevabını TTS için temiz, sözlü bir metne çevirir.
     * "✓ Yapıldı" gibi eylem ön ekleri ve madde işaretleri, kod blokları ve
     * emoji simgeleri ayıklanır — asistan konuşurken gevelemez.
     */
    fun konusulabilirCevap(ham: String): String {
        if (ham.isBlank()) return ""
        // Kod bloğu işaretlerini ve eylem ön eklerini at
        var s = ham
            .replace("```", " ")
            .replace("✓ ", "")
            .replace("✔ ", "")
            .replace("ℹ ", "")
            .replace("✅ ", "")
            .replace("❌ ", "")
        // Madde başı satırları yalın metne çevir: "• x" → "x", "- x" → "x"
        s = s
            .replace(Regex("^[•▪◦*]\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^-\\s*", RegexOption.MULTILINE), "")
        // Boşlukları/satırları tek boşluğa indir (satırlar arası sözcük ayracı kalsın)
        s = s.replace(Regex("\\s+"), " ")
        // Emoji ve simge karakterlerini temizle
        s = s.replace(Regex("[\\p{So}\\p{Cs}]"), "")
        // Emoji sözcükler arasındaysa çift boşluk kalabilir — tekrar tekilleştir
        s = s.replace(Regex("\\s+"), " ")
        return s.trim()
    }

    /**
     * Çevrimiçi AI için sözlü, kısa "koç gibi" yanıt istemi.
     * Bu istem, cevabı konuşmaya uygun ve kısa tutmaya yönlendirir.
     */
    fun sesliCevapIstemi(soru: String): String {
        val temiz = sesliSoruTemizle(soru)
        return "Sen kullanıcının kişisel çalışma koçusun. Kullanıcı seninle sesli konuşuyor. " +
            "Kısa, samimi ve akıcı konuşma diliyle yanıt ver (en fazla 3-4 cümle). " +
            "Madde madde veya kod kullanma; düz konuş. SORU: $temiz"
    }
}
