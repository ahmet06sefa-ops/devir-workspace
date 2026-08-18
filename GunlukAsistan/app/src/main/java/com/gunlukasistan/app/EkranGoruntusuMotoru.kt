package com.gunlukasistan.app

/**
 * v11.13 — Ekran görüntüsü → AI görsel karar motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "AI ekrana dokunabilsin ama ekranda NE olduğunu da görsün."
 * Bu, Gemini/OpenAI'nin asıl gücünü (görsel anlama) uygulamanın dokunma
 * yetkisiyle birleştirir: ekrandaki tıklanabilir etiketler AI'a verilir, AI
 * hangisine dokunacağına karar verir ve uygulama o tıklamayı gerçekleştirir.
 *
 *  · [gorselIstemiKur] — ekran etiketlerinden görsel modele gidecek istemi kurar.
 *  · [karariAyristir] — AI'nın "tikla|..." cevabını bir dokunma emrine çevirir.
 *  · [onEkran] — ekranda en belirgin öğeleri öncelik sırasıyla döner.
 *
 * Android'e bağımlı ekran yakalama (AccessibilityService) [EkranDokunmaServisi]
 * içindedir; bu nesne yalnızca karar mantığını taşır.
 */
object EkranGoruntusuMotoru {

    /** Görsel karar sonucu. */
    data class Karar(val ok: Boolean, val emir: String = "", val mesaj: String = "")

    /**
     * Görsel model için istem kurar. AI'dan SADECE "tikla|etiket" döndürmesi
     * istenir; uygulama bu emri ekranda uygular.
     */
    fun gorselIstemiKur(ekrandakiler: List<String>, kullaniciAmac: String): String {
        val liste = ekrandakiler.take(20).joinToString(", ")
        return "Sen ekrana dokunabilen bir asistan koçusun. Ekranda şu tıklanabilir öğeler var: [$liste]. " +
            "Kullanıcı amacı: \"$kullaniciAmac\". " +
            "Hangi öğeye dokunman gerektiğine karar ver ve SADECE şu biçimde yanıtla: tikla|ETIKET. " +
            "Uygun öğe yoksa: tikla|YOK."
    }

    /**
     * AI'nın yanıtını dokunma emrine çevirir.
     * Kabul biçimleri: "tikla|X", "TIKLA: X", "tikla X", satır başı "tikla|YOK".
     * "YOK" → ok=false (dokunma yapma). Boş → ok=false.
     */
    fun karariAyristir(aiYanit: String): Karar {
        val t = aiYanit.trim()
        if (t.isBlank()) return Karar(false, mesaj = "AI boş cevap verdi")
        val norm = t.lowercase()
        if (norm.contains("yok") && !norm.contains("yoksa")) {
            // "tikla|YOK" → dokunma yapma
            return Karar(false, mesaj = "AI uygun öğe bulamadı")
        }
        val emir = when {
            t.startsWith("tikla|", ignoreCase = true) -> t.substring(6).trim()
            t.startsWith("tikla:", ignoreCase = true) -> t.substring(6).trim()
            t.startsWith("tikla ", ignoreCase = true) -> t.substring(6).trim()
            t.startsWith("TIKLA|", ignoreCase = false) -> t.substring(6).trim()
            else -> t
        }
        if (emir.isBlank()) return Karar(false, mesaj = "Dokunma hedefi boş")
        return Karar(true, emir = emir)
    }

    /**
     * Ekran etiketlerini en belirgin/öncelikli sırayla önerir (ilk 12).
     * Görsel model bütün ekranı göremiyorsa da en kritik öğeleri görür.
     */
    fun onEkran(tumEtiketler: List<String>): List<String> {
        val temiz = tumEtiketler.map { it.trim() }.filter { it.isNotBlank() && it.length in 1..40 }
        val sirali = temiz.distinct().sortedBy { it.length } // kısa düğme etiketleri önce
        return sirali.take(12)
    }
}
