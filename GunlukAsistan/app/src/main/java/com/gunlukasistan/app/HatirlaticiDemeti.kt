package com.gunlukasistan.app

/**
 * v10.15 · ULTRA-30 / C18 — Hatırlatıcı demetinin saf kararları.
 *
 * ── Tarama kanıtı ──
 * `ReminderReceiver` her görevi tekil bildirimle gösteriyordu
 * (`notifId = (id % 50000) + 7000`); `setGroup` hiçbir yerde yoktu.
 * Aynı 10 dakikaya düşen 3 hatırlatıcı = panelde 3 ayrı kart.
 *
 * ── Model ──
 * Demet tetikleyen görevi de içerir: tetikleme anında, vadesi [tetik −
 * PENCERE, tetik + PENCERE] aralığında olan tamamlanmamış görevler tek
 * bildirimde satır satır listelenir (InboxStyle). Tekil aksiyonlar
 * korunur: her satıra "yapıldı" hedefli ayrı PendingIntent — ancak
 * Android bir bildirimde en fazla 3 aksiyon düğmesi gösterdiğinden
 * demet bildirimindeki satır dokunuşları görev ekranına düşer; tekil
 * "✓ / ertele" düğmeleri TEK görev kaldığında birebir eski davranışa
 * döner (dürüst kapsam notu).
 *
 * Saf bölge birim testlidir.
 */
object HatirlaticiDemeti {

    /** Demet penceresi: tetik anının ±10 dakikası (ms). */
    const val PENCERE_MS: Long = 10 * 60_000L

    /** Bir bildirimde listelenecek en fazla satır (taşma "+N görev daha"). */
    const val MAKS_SATIR: Int = 6

    /**
     * Demet üyelerini seçer: tarihi olan, tamamlanmamış, vadesi tetik
     * anına pencere içinde yakın olan görevler. Tetikleyici görev
     * listede yoksa başa eklenmez — çağıran zaten onu dahil ederek
     * gönderir (id kümesiyle filtrelemek çağıranın işi).
     * Dönüş vade sırasına göre artan, en çok [MAKS_SATIR] eleman.
     */
    fun <T> demetKur(
        adaylar: List<T>,
        tetikMs: Long,
        vade: (T) -> Long,
        bitti: (T) -> Boolean,
    ): List<T> = adaylar
        .asSequence()
        .filter { !bitti(it) }
        .filter { vade(it) > 0L }
        .filter { kotlin.math.abs(vade(it) - tetikMs) <= PENCERE_MS }
        .sortedBy { vade(it) }
        .take(MAKS_SATIR)
        .toList()

    /** Demet mi, tek görev mi? (tekse eski tekil akış korunur) */
    fun demetMi(adet: Int): Boolean = adet >= 2

    /** Liste satırı: "⏰ 15:00 — Raporu gönder" */
    fun satirMetni(vadeMs: Long, metin: String): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = vadeMs }
        return "⏰ %02d:%02d — %s".format(
            c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), metin
        )
    }

    /** Taşma satırı: "+3 görev daha aynı dilimde" */
    fun tasmaMetni(kalan: Int): String = "+%d görev daha aynı dilimde".format(kalan)
}
