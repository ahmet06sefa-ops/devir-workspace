package com.gunlukasistan.app

/**
 * v10.13 · ULTRA-30 / B7 + B10 — Kokpit ve odak kutusu için saf hesaplar.
 *
 * Saat akrep/yelkovan açıları, sayaç ilerleme yüzdesi ve seri metni
 * burada framework'süz tutulur; birim testleri dokunmadan doğrular.
 */
object Kokpit {

    /**
     * Saat açıları (derece; 0 = saat 12 yönü, saat yönünde).
     *
     * @param saat24 0..23
     * @param dakika 0..59
     * @return (akrepAci, yelkovanAci)
     */
    fun acilar(saat24: Int, dakika: Int): Pair<Float, Float> {
        val dk = dakika.coerceIn(0, 59)
        val akrep = ((saat24 % 12) * 30f) + dk * 0.5f
        val yelkovan = dk * 6f
        return akrep to yelkovan
    }

    /**
     * Geri sayım ilerlemesi (%). Bitmemiş oturum 0'a, sonu 100'e
     * kelepçelenir; bilinmeyen toplamda 0 döner (bölme kazası yok).
     */
    fun yuzde(kalanMs: Long, toplamMs: Long): Int {
        if (toplamMs <= 0L) return 0
        val gecen = (toplamMs - kalanMs).coerceIn(0L, toplamMs)
        return ((gecen * 100L) / toplamMs).toInt().coerceIn(0, 100)
    }

    /** Seri rozetinin kısa metni: "12 gün". */
    fun seriGun(seri: Int): Int = seri.coerceAtLeast(0)
}
