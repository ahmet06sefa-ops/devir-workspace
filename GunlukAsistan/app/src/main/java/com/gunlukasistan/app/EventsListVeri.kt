package com.gunlukasistan.app

/**
 * v10.5 · Öneri C31 — Çoklu geri sayım widget'ının seçim katmanı.
 *
 * ── Dürüst not ──
 * `CountdownWidget` (2×1) v7.42'den beri tek etkinlik gösteriyor:
 * sabitlenmiş ya da en yakın. Kullanıcının sınavı, doğum günü ve
 * faturası varsa üçü için üç ayrı widget koyması gerekiyordu.
 * Bu sınıf, listenin "hangi etkinlikler görünsün" kararını verir;
 * satır üretimi `EventsListService`'te.
 *
 * ── Sıralama ──
 * Yaklaşanlar gün sayısına göre; aynı güne denk gelenlerde
 * sabitlenen üstte (kullanıcı onu seçmiş). Geçmiş etkinlikler liste
 * sonuna "geçti" olarak en fazla 1 tane alınır — yakın geçmiş bağlam
 * verir ama eskilerle doldurmaz.
 */
object EventsListVeri {

    const val AZAMI_SATIR = 6

    fun sec(events: List<Store.DayEvent>): List<Store.DayEvent> =
        sec(events, gecmisiDahil = true, yalnizSabit = false, limit = AZAMI_SATIR)

    /**
     * v10.21 · Liste satır filtreleri — kullanıcı yetkisi genişletildi.
     * Varsayılan parametreler eski davranışı BİREBİR üretir (mevcut 5
     * birim testi aynen geçer).
     *
     * @param gecmisiDahil  false = "geçti" satırı hiç gösterilmez
     * @param yalnizSabit   true = yalnız sabitlenen (pin'li) etkinlikler kalır
     * @param limit         satır sayısı (serbest, taban 1)
     */
    fun sec(
        events: List<Store.DayEvent>,
        gecmisiDahil: Boolean,
        yalnizSabit: Boolean,
        limit: Int
    ): List<Store.DayEvent> {
        val kaynak = if (yalnizSabit) events.filter { it.pinned } else events
        val gelecek = kaynak
            .filter { !it.isPast }
            .sortedWith(compareBy({ it.daysLeft }, { !it.pinned }))
        val gecmis = if (gecmisiDahil) {
            kaynak
                .filter { it.isPast }
                .sortedByDescending { it.daysLeft } // en yakın geçmiş önce
                .take(1)
        } else emptyList()
        return (gelecek + gecmis).take(limit.coerceAtLeast(1))
    }
}
