package com.gunlukasistan.app

/**
 * v11.13 — Trend / eğilim analiz motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Devam et." Rakiplerde (Habitify) olan veri odaklı bir
 * özellik: dönemsel eğilim. Bu motor, günlük puan serisinden basit bir
 * doğrusal eğilim (artıyor / azalıyor / sabit) ve sonraki gün tahmini çıkarır.
 *
 *  · [ort] — bir serinin ortalaması.
 *  · [egilim] — serinin son yarısının ortalaması vs ilk yarısı → -1/0/1.
 *  · [egilimMetni] — eğilimi okunur Türkçe metne çevirir.
 *  · [sonrakiTahmin] — basit ortalama ile bir sonraki gün tahmini.
 */
object TrendAnalizMotoru {

    /** Bir serinin ortalaması; boşsa 0. */
    fun ort(seri: List<Int>): Double {
        if (seri.isEmpty()) return 0.0
        return seri.sum().toDouble() / seri.size
    }

    /**
     * Eğilim: son yarı ortalama vs ilk yarı ortalama.
     * 1 = artıyor, 0 = sabit (fark küçük), -1 = azalıyor.
     * 2 elementten az seride 0 döner.
     */
    fun egilim(seri: List<Int>): Int {
        if (seri.size < 2) return 0
        val yari = seri.size / 2
        val ilk = seri.take(yari)
        val son = seri.takeLast(yari)
        val fark = ort(son) - ort(ilk)
        return when {
            fark > 5 -> 1
            fark < -5 -> -1
            else -> 0
        }
    }

    /** Eğilimi okunur Türkçe metne çevirir. */
    fun egilimMetni(e: Int): String = when (e) {
        1 -> "📈 Artıyor — performansın yükseliyor!"
        -1 -> "📉 Azalıyor — biraz motivasyon gerek."
        else -> "➡️ Sabit — istikrarlı gidiyorsun."
    }

    /** Son N günün ortalamasına göre sonraki gün tahmini (puan). */
    fun sonrakiTahmin(seri: List<Int>): Int {
        if (seri.isEmpty()) return 0
        return ort(seri).toInt().coerceAtLeast(0)
    }

    /** Kısa eğilim raporu: seri + eğilim + tahmin. */
    fun rapor(seri: List<Int>): String {
        val e = egilim(seri)
        val tahmin = sonrakiTahmin(seri)
        return "Son ${seri.size} gün ortalaması: ${ort(seri).toInt()} · ${egilimMetni(e)}\n" +
            "Tahmini yarın: $tahmin puan"
    }
}
