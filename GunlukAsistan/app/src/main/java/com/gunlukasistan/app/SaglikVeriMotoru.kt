package com.gunlukasistan.app

/**
 * v11.13 — Sağlık verisi (adım / aktivite) çekirdeği (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (Streaks/Fabulous) olup bende olmayan sağlık
 * verisi entegrasyonu (Google Fit / Health Connect) ekle." Gerçek Google Fit
 * / Health Connect SDK'sı cihaz + Google Play Services ister; burada veri
 * mantığı inşa edilir:
 *
 *  · [AdimKaydi] — bir günün adım + mesafe + aktif dakika modeli.
 *  · [hedefYuzde] — günlük adım hedefine göre ilerleme.
 *  · [aktiviteDerece] — adım yoğunluğunu 0..4 seviyeye çevirir (ısı haritasına).
 *  · [hedefOner] — mevcut adıma göre makul bir günlük hedef önerir.
 *
 * UI katmanı bu veriyi alışkanlığa işler; bu nesne yalnızca hesap/karar taşır.
 */
object SaglikVeriMotoru {

    data class AdimKaydi(
        val gunAnahtar: String,   // "yyyyMMdd"
        val adim: Int,
        val mesafeMetre: Int = 0,
        val aktifDk: Int = 0
    )

    /** Günlük adım hedefine göre 0..100 ilerleme. */
    fun hedefYuzde(adim: Int, hedef: Int): Int {
        if (hedef <= 0) return 0
        return (adim.coerceAtLeast(0) * 100 / hedef).coerceIn(0, 100)
    }

    /**
     * Adım yoğunluğunu 0..4 seviyeye çevirir (günlük hedefe oranla).
     * 0 → 0; %25'e kadar 1; %50'ye 2; %75'e 3; üstü 4.
     */
    fun aktiviteDerece(adim: Int, hedef: Int): Int {
        if (hedef <= 0) return 0
        val oran = adim.coerceAtLeast(0) * 100 / hedef
        return when {
            oran <= 0 -> 0
            oran <= 25 -> 1
            oran <= 50 -> 2
            oran <= 75 -> 3
            else -> 4
        }
    }

    /**
     * Mevcut adıma göre makul bir günlük hedef önerir.
     * Çok hareketliyse daha yüksek, az hareketliyse ulaşılabilir bir hedef.
     */
    fun hedefOner(ortalamaAdim: Int): Int = when {
        ortalamaAdim >= 10000 -> 12000
        ortalamaAdim >= 7000 -> 10000
        ortalamaAdim >= 4000 -> 7000
        else -> 5000
    }

    /** Adım kayıtlarının toplam adımını döndürür. */
    fun toplamAdim(kayitlar: List<AdimKaydi>): Int = kayitlar.sumOf { it.adim }
}
