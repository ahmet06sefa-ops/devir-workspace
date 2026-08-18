package com.gunlukasistan.app

/**
 * v11.13 — Bulut senkron çekirdeği (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde olup bende olmayan bulut senkron / hesap
 * ekle." Gerçek sunucu altyapısı bu sandbox'ta kurulamaz; ancak senkronun
 * ZOR ve DEĞERLİ kısmı — sürümleme, birleştirme, çakışma çözümü ve hesap
 * modeli — saf mantıktır ve burada tam olarak inşa edilir. Gerçek HTTP
 * taşıyıcısı eklendiğinde bu motor hemen kullanılabilir.
 *
 *  · [VeriPaketi] — zaman damgalı, sürümlü veri anlık görüntüsü.
 *  · [birles] — iki cihazın paketini alan-zaman damgasına göre birleştirir.
 *  · [cakismaCoz] — çakışan alanlarda hangi sürümün kazandığını seçer.
 *  · [Hesap] — kullanıcı oturumu modeli (id, ad, email, kayitMs).
 */
object SenkronMotoru {

    /** Veri paketi sürümü — biçim değişirse bu artırılır. */
    const val PAKET_SURUM = 1

    /**
     * Zaman damgalı veri anlık görüntüsü.
     * @param anahtar veri türü (ör. "gorevler", "notlar")
     * @param json veri içeriği
     * @param degistirildiMs son değişiklik zamanı (daha büyük = daha yeni)
     */
    data class VeriPaketi(
        val anahtar: String,
        val json: String,
        val degistirildiMs: Long
    )

    /** Kullanıcı oturumu. */
    data class Hesap(
        val id: String,
        val ad: String,
        val email: String,
        val kayitMs: Long
    )

    /**
     * İki cihazın aynı anahtardaki verisini birleştirir.
     * Kural: daha YENİ (daha büyük zaman damgası) sürüm kazanır.
     * Zaman damgası eşitse [b] (bu seferki) kazanır — deterministik.
     */
    fun birles(a: VeriPaketi, b: VeriPaketi): VeriPaketi =
        if (b.degistirildiMs >= a.degistirildiMs) b else a

    /**
     * İki paket çakışıyor mu? Aynı anahtar ve ikisi de farklı içerikse
     * ve zaman damgaları eşitse çakışma var (elle çözüm önerilir).
     */
    fun cakismaMi(a: VeriPaketi, b: VeriPaketi): Boolean =
        a.anahtar == b.anahtar && a.json != b.json && a.degistirildiMs == b.degistirildiMs

    /**
     * Aynı anahtardaki iki sürümden hangisi kazanır? (0 = a, 1 = b)
     * Zaman damgasına göre; eşitse b.
     */
    fun hangisiKazanir(a: VeriPaketi, b: VeriPaketi): Int =
        if (b.degistirildiMs > a.degistirildiMs) 1 else 0

    /**
     * Bir liste paketini anahtara göre gruplayıp en yeni sürümleri döndürür.
     * Bulut yanıtında aynı anahtarın birden çok sürümü gelirse en yenisi alınır.
     */
    fun enYeniListe(paketler: List<VeriPaketi>): List<VeriPaketi> {
        val enYeni = mutableMapOf<String, VeriPaketi>()
        for (p in paketler) {
            val mevcut = enYeni[p.anahtar]
            if (mevcut == null || p.degistirildiMs > mevcut.degistirildiMs) {
                enYeni[p.anahtar] = p
            }
        }
        return enYeni.values.toList()
    }

    /**
     * Gerçek sunucuya gönderilecek istek gövdesini kurar (SAF).
     * @return JSON: {surum, hesapId, cihaz, paketler:[{anahtar, json, degistirildiMs}]}
     */
    fun istekGovdesi(hesap: Hesap, cihazAd: String, paketler: List<VeriPaketi>): String {
        val sb = StringBuilder()
        sb.append("{\"surum\":$PAKET_SURUM,\"hesapId\":\"").append(hesap.id)
            .append("\",\"cihaz\":\"").append(cihazAd).append("\",\"paketler\":[")
        paketler.forEachIndexed { i, p ->
            if (i > 0) sb.append(',')
            sb.append("{\"anahtar\":\"").append(p.anahtar).append("\",\"json\":\"")
                .append(p.json.replace("\"", "\\\"")).append("\",\"degistirildiMs\":")
                .append(p.degistirildiMs).append('}')
        }
        sb.append("]}")
        return sb.toString()
    }
}
