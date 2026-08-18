package com.gunlukasistan.app

/**
 * v11.13 — Telefondaki diğer uygulamalara erişim: SAF MOTOR (JVM testli).
 *
 * Kullanıcı isteği: "Uygulama telefondaki diğer uygulamalara erişim sağlasın."
 *
 * Bu nesne, Android'e bağımlı olmayan karar katmanını barındırır:
 *  · [normalle] — Türkçe duyarlı arama anahtarı
 *  · [filtrle] — yüklü uygulama listesini arama metnine göre sıralar
 *  · [eslesme] — "WhatsApp" / "youtube" gibi bir adı paket adıyla eşleştirir
 *  · [kategoriAdi] — paket adından akıllı kategori çıkarır (mesajlaşma, video…)
 *  · [oncelikPuan] — arama sonuçlarını önem sırasına dizer
 *
 * Android'e bağımlı listeleme/başlatma [UygulamalarActivity] içindedir;
 * buradaki tüm fonksiyonlar saf ve birim testlidir.
 */
object UygulamaMotoru {

    /** Yüklü bir uygulamanın UI modeli. */
    data class Uygulama(
        val paket: String,
        val ad: String
    )

    /**
     * Türkçe duyarlı normalleştirme: harf duyarlılığını kaldırır, Türkçe
     * karakterleri ASCII'ye çevirir, harf/rakam dışındakileri temizler.
     */
    fun normalle(s: String): String =
        s.lowercase(java.util.Locale("tr", "TR"))
            .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .filter { it.isLetterOrDigit() }
            .trim()

    /**
     * Yüklü uygulama listesini bir arama metnine göre sıralar.
     * Boş/blank arama → orijinal sıra (tümü). Sonuçlar [oncelikPuan] ile
     * en iyi eşleşmeden en kötüye dizilir.
     */
    fun filtrle(liste: List<Uygulama>, arama: String): List<Uygulama> {
        val a = normalle(arama)
        if (a.isBlank()) return liste
        return liste.map { it to oncelikPuan(it, a) }
            .filter { (_, p) -> p > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Bir arama teriminin verilen uygulamayla ne kadar iyi eşleştiğini
     * 0-100 arası puanlar. 0 = eşleşme yok.
     * Öncelik: adın başlangıcı > adın içinde > paket adında > sözcük kısmen.
     */
    fun oncelikPuan(u: Uygulama, arama: String): Int {
        val a = normalle(arama)
        if (a.isBlank()) return 0
        val ad = normalle(u.ad)
        val paket = normalle(u.paket)
        return when {
            ad == a -> 100
            ad.startsWith(a) -> 90
            ad.contains(a) -> 80
            paket == a || paket.endsWith(a) || paket.contains(".$a") -> 60
            paket.contains(a) -> 50
            ad.split(" ").any { it.startsWith(a) } -> 70
            else -> 0
        }
    }

    /**
     * Tek bir adı en iyi eşleşen uygulamaya bağlar. `listedeYok` döner.
     * AI "youtube'u aç" dediğinde "YouTube" bulunur.
     */
    fun eslesme(liste: List<Uygulama>, arama: String): Uygulama? =
        filtrle(liste, arama).firstOrNull()

    /** Türkçe boşluk ayrılmış terimlerin tümü eşleşiyor mu? */
    fun tumTerimler(liste: List<Uygulama>, arama: String): List<Uygulama> {
        val terimler = arama.split(" ").map { normalle(it) }.filter { it.isNotBlank() }
        if (terimler.isEmpty()) return emptyList()
        return liste.filter { u ->
            val hedef = normalle(u.ad + " " + u.paket)
            terimler.all { hedef.contains(it) }
        }
    }

    /** Paket adından görsel bir kategori çıkarır. */
    fun kategori(paket: String): String {
        val p = paket.lowercase()
        return when {
            p.contains("whatsapp") || p.contains("telegram") ||
                p.contains("messenger") || p.contains("signal") ||
                p.contains("discord") || p.contains("slack") -> "Mesajlaşma"
            p.contains("youtube") || p.contains("twitch") ||
                p.contains("netflix") || p.contains("tiktok") ||
                p.contains("vimeo") -> "Video"
            p.contains("spotify") || p.contains("music") ||
                p.contains("muzik") || p.contains("fizy") -> "Müzik"
            p.contains("chrome") || p.contains("firefox") ||
                p.contains("opera") || p.contains("browser") -> "Tarayıcı"
            p.contains("gmail") || p.contains("mail") ||
                p.contains("outlook") || p.contains("email") -> "E-posta"
            p.contains("map") || p.contains("navigasyon") ||
                p.contains("yandex.navigator") || p.contains("gps") -> "Navigasyon"
            p.contains("instagram") || p.contains("facebook") ||
                p.contains("twitter") || p.contains("x.com") ||
                p.contains("linkedin") || p.contains("tiktok") -> "Sosyal"
            p.contains("camera") || p.contains("gallery") ||
                p.contains("foto") || p.contains("photo") -> "Kamera & Galeri"
            p.contains("bank") || p.contains("garanti") ||
                p.contains("isbank") || p.contains("ziraat") ||
                p.contains("akbank") || p.contains("finans") -> "Finans"
            else -> "Uygulama"
        }
    }
}
