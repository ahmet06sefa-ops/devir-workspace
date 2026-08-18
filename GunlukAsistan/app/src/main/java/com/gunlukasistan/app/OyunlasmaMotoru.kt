package com.gunlukasistan.app

/**
 * v11.13 — Oyunlaştırma: XP ve seviye (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (Habitica) olup bende olmayan oyunlaştırma
 * (XP/seviye) ekle."
 *
 * Tamamlanan her görev/alışkanlık XP kazandırır; toplam XP seviye belirler.
 *  · [xpHedef] — bir seviyeden diğerine geçiş için gereken XP (seviyeyle büyür).
 *  · [seviye] — toplam XP'den seviye.
 *  · [seviyedeIlerleme] — mevcut seviye içindeki ilerleme (0..1).
 *  · [gorevXp] / [aliskanlikXp] / [odakXp] — eylem bazlı XP.
 */
object OyunlasmaMotoru {

    private const val TABAN = 100

    /** Bir sonraki seviye için gereken XP (seviyeyle birlikte artar). */
    fun xpHedef(seviye: Int): Int = TABAN + (seviye - 1) * 25

    /** Toplam XP'den seviye (1-tabanlı). */
    fun seviye(toplamXp: Int): Int {
        if (toplamXp <= 0) return 1
        var seviye = 1
        var kalan = toplamXp
        while (kalan >= xpHedef(seviye)) {
            kalan -= xpHedef(seviye)
            seviye++
        }
        return seviye
    }

    /** Mevcut seviye içinde ilerleme 0..1. */
    fun seviyedeIlerleme(toplamXp: Int): Float {
        if (toplamXp <= 0) return 0f
        val s = seviye(toplamXp)
        var kalan = toplamXp
        for (i in 1 until s) kalan -= xpHedef(i)
        return (kalan.toFloat() / xpHedef(s).toFloat()).coerceIn(0f, 1f)
    }

    /** Görev tamamlama XP'si. */
    fun gorevXp(): Int = 10

    /** Alışkanlık işaretleme XP'si. */
    fun aliskanlikXp(): Int = 15

    /** Odak dakikası başına XP (1 dk = 1 XP). */
    fun odakXp(dakika: Int): Int = dakika.coerceAtLeast(0)

    /** Rütbe adı (saf). */
    fun rutbe(seviye: Int): String = when {
        seviye >= 30 -> "🏆 Efsane"
        seviye >= 20 -> "🥇 Usta"
        seviye >= 12 -> "⭐ Kıdemli"
        seviye >= 6 -> "🎖️ Çalışkan"
        seviye >= 3 -> "🌱 Gelişen"
        else -> "🚀 Başlangıç"
    }
}
