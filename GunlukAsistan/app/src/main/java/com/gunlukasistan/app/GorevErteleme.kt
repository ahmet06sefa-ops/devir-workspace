package com.gunlukasistan.app

import android.content.Context

/**
 * v10.38 · Katalog #18 — haftalık "en çok ertelenen görev" tespiti.
 *
 * Erteleme kaynakları: alarm ekranında ⏰ Ertele ([GorevAlarmActivity]),
 * toplu seçimden yarına/takvimle ileri taşıma ve gecikmiş görev
 * erteleme diyaloğu. Hepsi [kaydet]'e akar; kayıtlar ISO haftasına
 * göre tutulur — yeni haftada sayaç kendini sıfırlar.
 *
 * Haftada 3+ ertelenen görev, görev ekranının üstünde bilgi şeridiyle
 * gösterilir (uyarı, engelleme değil).
 */
object GorevErteleme {

    private const val PREF = "gorev_erteleme_v1"
    private const val K_HAFTA = "hafta"
    private const val K_ONEK = "h_"

    /** Uyarı eşiği: hafta içinde bu kadar ve üzeri erteleme. */
    const val ESIK = 3

    private fun pref(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Hafta kodu: yil*100 + yılın haftası (ör. 202632). Saf. */
    fun haftaKodu(cal: java.util.Calendar): Int =
        cal.get(java.util.Calendar.YEAR) * 100 + cal.get(java.util.Calendar.WEEK_OF_YEAR)

    /** En çok ertelenen kaydı seçer; eşik altı/boş ise null. Saf. */
    fun enCokSatiri(harita: Map<Long, Int>, esik: Int = ESIK): Pair<Long, Int>? =
        harita.filterValues { it >= esik }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }

    /** Bir erteleme olayını kaydeder (haftalık otomatik sıfırlamalı). */
    fun kaydet(c: Context, gorevId: Long) {
        runCatching {
            val p = pref(c)
            val hafta = haftaKodu(java.util.Calendar.getInstance())
            val ayniHafta = p.getInt(K_HAFTA, -1) == hafta
            val e = p.edit()
            if (!ayniHafta) e.clear().putInt(K_HAFTA, hafta)
            val onceki = if (ayniHafta) p.getInt(K_ONEK + gorevId, 0) else 0
            e.putInt(K_ONEK + gorevId, onceki + 1).apply()
        }
    }

    /**
     * Bu hafta en çok ertelenen AKTİF görevi (silinmemiş/arşivlenmemiş)
     * eşiği aşıyorsa döndürür; yoksa null.
     */
    fun enCokErilenen(c: Context, gorevler: List<Store.Task>): Pair<Store.Task, Int>? {
        val p = pref(c)
        if (p.getInt(K_HAFTA, -1) != haftaKodu(java.util.Calendar.getInstance())) return null
        val idSeti = gorevler.map { it.id }.toSet()
        val harita = mutableMapOf<Long, Int>()
        for ((k, v) in p.all) {
            if (k.startsWith(K_ONEK) && v is Int) {
                val id = k.removePrefix(K_ONEK).toLongOrNull()
                if (id != null && id in idSeti) harita[id] = v
            }
        }
        val satir = enCokSatiri(harita) ?: return null
        val gorev = gorevler.firstOrNull { it.id == satir.first } ?: return null
        return gorev to satir.second
    }
}
