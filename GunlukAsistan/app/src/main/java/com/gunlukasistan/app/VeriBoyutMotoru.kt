package com.gunlukasistan.app

/**
 * v11.13 — Veri boyutu / depolama temizlik asistanı (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Devam et." Rakiplerde olan ve pratik değer veren bir
 * özellik: depolama durumu + temizlenebilir öğeler. Bu motor, kategori
 * bazlı boyut ve temizlik önerisi mantığını taşır.
 *
 *  · [Kalem] — bir depolama kalemi (kod, ad, bayt, temizlenebilir mi).
 *  · [boyutMetni] — bayt → okunur boyut (KB/MB).
 *  · [toplamBayt] — kalemlerin toplam boyutu.
 *  · [temizlenebilirToplam] — silinebileceklerin toplamı.
 *  · [onerilen] — temizlenebilir kalemleri azalan boyut sırasıyla önerir.
 */
object VeriBoyutMotoru {

    data class Kalem(
        val kod: String,
        val ad: String,
        val bayt: Long,
        val temizlenebilir: Boolean
    )

    /** Baytı okunur boyut metnine çevirir (B/KB/MB/GB). */
    fun boyutMetni(bayt: Long): String {
        if (bayt < 1024) return "$bayt B"
        val kb = bayt / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    /** Toplam boyut. */
    fun toplamBayt(kalemler: List<Kalem>): Long = kalemler.sumOf { it.bayt }

    /** Temizlenebilirlerin toplam boyutu. */
    fun temizlenebilirToplam(kalemler: List<Kalem>): Long =
        kalemler.filter { it.temizlenebilir }.sumOf { it.bayt }

    /** Temizlenebilir kalemleri azalan boyut sırasıyla önerir. */
    fun onerilen(kalemler: List<Kalem>): List<Kalem> =
        kalemler.filter { it.temizlenebilir && it.bayt > 0 }.sortedByDescending { it.bayt }

    /** Özet metni: "Toplam X · Temizlenebilir Y". */
    fun ozet(kalemler: List<Kalem>): String {
        val toplam = toplamBayt(kalemler)
        val temiz = temizlenebilirToplam(kalemler)
        return "Toplam ${boyutMetni(toplam)} · Temizlenebilir ${boyutMetni(temiz)}"
    }
}
