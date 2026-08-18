package com.gunlukasistan.app

/**
 * v10.14 · ULTRA-30 / E26 — Kronotip kartının saf hesapları.
 *
 * ── Tarama düzeltmesi (dürüstlük) ──
 * Öneri metni "h verisi toplanıyor ama YORUMLANMIYOR" diyordu — bu
 * yanlıştı: `Analitik.saatDilimleri` / `enVerimliSaat` v7.38'den beri
 * saat dağılımını yorumluyor ve Analitik ekranında çiziliyor.
 * E26'nın gerçek boşluğu: uyku defteri (uyanış saatleri) ile saat
 * analizi hiç TEK KARTTA birleşmemişti ve karta bağlı bir eylem
 * (odak önerisi) yoktu. Kronotip kartı bu ikisini birleştirir.
 *
 * Tüm fonksiyonlar framework'süzdür; birim testleri parametreyle besler.
 */
object Kronotip {

    /** Kronotip sınıfları. */
    enum class Tip { SERCE, GUVENCIN, GECE_KUSU }

    /** Ortalama uyanış dakikası (0-1439). Boş listede -1. */
    fun ortUyanis(dakikalar: List<Int>): Int =
        if (dakikalar.isEmpty()) -1 else dakikalar.average().toInt()

    /** Yayılım: en erken ↔ en geç uyanış farkı (dk). Boş/tek kayıtta 0. */
    fun sapma(dakikalar: List<Int>): Int =
        if (dakikalar.size < 2) 0
        else (dakikalar.maxOrNull() ?: 0) - (dakikalar.minOrNull() ?: 0)

    /**
     * Ortalama uyanışa göre tip.
     * < 07:00 erkenci serçe · 07:00–08:59 dengeli güvercin · ≥ 09:00 gece kuşu
     */
    fun tip(ortDk: Int): Tip = when {
        ortDk < 0 -> Tip.GUVENCIN
        ortDk < 420 -> Tip.SERCE
        ortDk < 540 -> Tip.GUVENCIN
        else -> Tip.GECE_KUSU
    }

    fun tipEmoji(t: Tip): String = when (t) {
        Tip.SERCE -> "🐦"
        Tip.GUVENCIN -> "🕊"
        Tip.GECE_KUSU -> "🦉"
    }

    /**
     * En verimli saatten 2 saatlik odak penceresi (başlangıç, 0-22).
     * -1 (veri yok) → varsayılan 09:00. Uçta 23:00'i aşmaz.
     */
    fun odakPenceresi(enVerimliSaat: Int): Int {
        if (enVerimliSaat < 0) return 9
        return enVerimliSaat.coerceIn(0, 22)
    }

    /** "09:00–11:00" biçiminde okunur pencere. [bas] 0-22 arası. */
    fun saatAralik(bas: Int): String {
        val b = bas.coerceIn(0, 22)
        return "%02d:00–%02d:00".format(b, b + 2)
    }

    /** [simdiDk] pencerede mi? (başlangıç dahil, +2 saat) */
    fun penceredeMi(simdiDk: Int, bas: Int): Boolean =
        simdiDk in bas * 60 until (bas + 2) * 60
}
