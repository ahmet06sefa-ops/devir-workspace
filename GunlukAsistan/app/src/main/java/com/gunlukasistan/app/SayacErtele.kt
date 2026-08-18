package com.gunlukasistan.app

import java.util.Calendar

/**
 * v10.2 · Öneri B26 — Bitiş "sonra hatırlat" kademeleri (saf mantık).
 *
 * ── Sorun ──
 * Alarmı çalan kişi o anda başlayamayabilir (ders biterken, yolda).
 * Bitiş bildiriminin tek seçeneği "kapat"tı; alarm sessizce ölüyordu.
 *
 * ── Kademeler ──
 *   · 10 dk sonra
 *   · 1 saat sonra
 *   · Yarın sabah 08:00
 *
 * Zaman hesabı saf fonksiyon olarak ayrıldı — birim test edilebilir.
 */
object SayacErtele {

    const val SEC_ONDK = 0
    const val SEC_BIRSA = 1
    const val SEC_YARIN = 2

    /** Seçime göre milisaniye cinsinden hedef an. */
    fun hedefMilis(secim: Int, simdi: Long): Long = when (secim) {
        SEC_ONDK -> simdi + 10 * 60_000L
        SEC_BIRSA -> simdi + 60 * 60_000L
        else -> yarinSabah(simdi)
    }

    /**
     * "Yarın sabah" hedefi.
     *
     * Saat henüz 08:00'i geçmediyse BUGÜN 08:00 (mantıklı tek okuma:
     * gece 03:00'te "yarın sabah" = birkaç saat sonrası); geçmişse
     * YARIN 08:00.
     */
    fun yarinSabah(simdi: Long, sabahSaat: Int = 8): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = simdi }
        val gecmis = cal.get(Calendar.HOUR_OF_DAY) >= sabahSaat
        cal.set(Calendar.HOUR_OF_DAY, sabahSaat)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (gecmis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** Binder'a/bildirime yazılacak insan okuması adı. */
    fun adi(context: android.content.Context, secim: Int): String =
        context.getString(
            when (secim) {
                SEC_ONDK -> R.string.sb_ertele_10
                SEC_BIRSA -> R.string.sb_ertele_1sa
                else -> R.string.sb_ertele_yarin
            }
        )
}
