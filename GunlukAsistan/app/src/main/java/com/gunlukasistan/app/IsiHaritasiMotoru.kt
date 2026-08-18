package com.gunlukasistan.app

/**
 * v11.13 — Alışkanlık & aktivite ısı haritası (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (TickTick/Habitify) olup bende olmayan
 * özelliklerden alışkanlık ısı haritası (GitHub yeşil kareleri) ekle."
 *
 * Bu motor, son N günün günlük aktivite verisini (odak dk + tamamlama)
 * 0..4 seviyeli bir ısı matrisine çevirir. 5 sütunlu satır blokları (hafta
 * benzeri) üretir; her hücre bir günü temsil eder ve seviyesi kadar renk
 * yoğunluğu alır.
 *
 *  · [seviye] — puan (0..100) → 0..4 ısı seviyesi (saf).
 *  · [puan] — günlük odak + tamamlama → tek puan (saf).
 *  · [matris] — günlük puan listesini hafta-benzeri satırlara dizer.
 *
 * UI katmanı hücreleri seviyeye göre boyar; bu nesne yalnızca mantığı taşır.
 */
object IsiHaritasiMotoru {

    /** Isı seviyesi sayısı (0 = boş, 4 = en yoğun). */
    const val MAKS_SEVIYE = 4

    /**
     * 0..100 puandan ısı seviyesi çıkarır.
     * 0 → 0; 1-25 → 1; 26-50 → 2; 51-75 → 3; 76+ → 4.
     */
    fun seviye(puan: Int): Int = when {
        puan <= 0 -> 0
        puan <= 25 -> 1
        puan <= 50 -> 2
        puan <= 75 -> 3
        else -> 4
    }

    /**
     * Günlük aktiviteyi tek bir puan (0..100) altında birleştirir.
     * Ağırlık: odak dakikası (max 90) + tamamlama sayısı (max 10).
     */
    fun puan(odakDk: Int, tamamlama: Int): Int {
        val odakOr = (odakDk.coerceIn(0, 90).toFloat() / 90f)
        val tamOr = (tamamlama.coerceIn(0, 10).toFloat() / 10f)
        val toplam = (odakOr * 70 + tamOr * 30).toInt()
        return toplam.coerceIn(0, 100)
    }

    /**
     * Günlük puan listesini satır bloklarına dizer (5 hücre/satır, hafta benzeri).
     * Listenin SONU en sağa gelecek şekilde kaydırılır (bugün sağda, geçmiş solda).
     * Eksik günler başa 0 seviyeli boş hücre olarak eklenir.
     * @return her satır = [Int] seviye listesi (uzunluğu en fazla 5).
     */
    fun matris(seviyeler: List<Int>): List<List<Int>> {
        if (seviyeler.isEmpty()) return emptyList()
        val toplam = seviyeler.size
        val satirSayisi = (toplam + 4) / 5
        val satirlar = mutableListOf<List<Int>>()
        // En yeni (son) günü en alt satırın sonuna yerleştir:
        // geçmişten bugüne → soldan sağa, üstten alta.
        var indeks = 0
        for (s in 0 until satirSayisi) {
            val satir = mutableListOf<Int>()
            for (k in 0 until 5) {
                if (indeks < toplam) {
                    satir.add(seviyeler[indeks])
                    indeks++
                } else {
                    satir.add(0)
                }
            }
            satirlar.add(satir)
        }
        return satirlar
    }
}
