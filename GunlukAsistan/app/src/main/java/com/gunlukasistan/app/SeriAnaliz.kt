package com.gunlukasistan.app

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * v10.40 — Katalog #52: En uzun İKİNCİ seri (saf motor, JVM testli).
 *
 * Mevcut seri gösteriliyordu ama "geçmişte en çok kaç gün üst üste
 * yaptım, ikinci en iyi rekor neydi" bilinmiyordu. Bu motor tamamlanmış
 * gün anahtarları kümesinden ardışık takvim günü serileri çıkarır —
 * ay yıl sınırı güvenlidir (epoch gün farkı üzerinden).
 */
object SeriAnaliz {

    /** Her ardışık serinin uzunluğu (ayrık dönemler ayrı sayılır). */
    fun seriler(gunler: Set<Int>): List<Int> {
        if (gunler.isEmpty()) return emptyList()
        val sur = gunler.map { epochGun(it) }.sorted()
        val out = ArrayList<Int>()
        var n = 1
        for (i in 1 until sur.size) {
            if (sur[i] == sur[i - 1] + 1) n++ else { out.add(n); n = 1 }
        }
        out.add(n)
        return out
    }

    fun enUzun(gunler: Set<Int>): Int = seriler(gunler).maxOrNull() ?: 0

    /** En uzun seriden FARKLI bir dönemin en uzunu; yoksa 0. */
    fun ikinciEnUzun(gunler: Set<Int>): Int =
        seriler(gunler).sortedDescending().drop(1).firstOrNull() ?: 0

    /** yyyyMMdd → UTC epoch gün (yaz saati kaymasına kapalı). */
    private fun epochGun(gunKey: Int): Long {
        val bicim = SimpleDateFormat("yyyyMMdd", Locale.US)
        bicim.timeZone = TimeZone.getTimeZone("UTC")
        return (runCatching { bicim.parse(gunKey.toString())?.time }.getOrNull() ?: 0L) / 86_400_000L
    }
}
