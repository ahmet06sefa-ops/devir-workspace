package com.gunlukasistan.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * v10.47 — Kullanıcı maddesi #9: Manuel Kontrol Merkezi saf mantık motoru.
 * Uyku/uyanma saati hesapları, odak süresi ve seri kısıtları, geçmiş liste üretimi.
 */
object ManuelKontrol {

    /**
     * Uyanma saati damgasını hesaplar.
     * Belirtilen gün üzerinde saat ve dakikayı kurar.
     */
    fun uyanmaZamaniHesapla(referansGunMs: Long, saat: Int, dakika: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referansGunMs
            set(Calendar.HOUR_OF_DAY, saat.coerceIn(0, 23))
            set(Calendar.MINUTE, dakika.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Uyuma saati damgasını akıllı takvim mantığıyla hesaplar.
     * - Eğer saat >= 12 ise (ör. 22:30, 23:45), bu süre bir önceki akşamı temsil eder (referans günden 1 gün önce).
     * - Eğer saat < 12 ise (ör. 00:30, 01:15), bu sabahın gece yarısı sonrasını temsil eder (referans günle aynı gün).
     */
    fun uyumaZamaniHesapla(referansGunMs: Long, saat: Int, dakika: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referansGunMs
            set(Calendar.HOUR_OF_DAY, saat.coerceIn(0, 23))
            set(Calendar.MINUTE, dakika.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (saat >= 12) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        return cal.timeInMillis
    }

    /**
     * İki damga arası uyku süresini (milisaniye) hesaplar.
     * Geçersiz (negatif, 0 veya 20 saatten uzun) durumlarda null döner.
     */
    fun uykuSuresiHesapla(uyuduMs: Long, uyandiMs: Long): Long? {
        if (uyuduMs <= 0L || uyandiMs <= 0L) return null
        val fark = uyandiMs - uyuduMs
        if (fark <= 0L || fark > 20 * 3600_000L) return null
        return fark
    }

    /**
     * Gün serisi değerini güvenli aralığa (0..9999) kısıtlar.
     */
    fun seriSinirla(seri: Int): Int {
        return seri.coerceIn(0, 9999)
    }

    /**
     * Günlük odak dakikası toplamını güvenli aralığa (0..1440) kısıtlar.
     */
    fun odakDakikaSinirla(mevcutDk: Int, ekleDk: Int): Int {
        return (mevcutDk + ekleDk).coerceIn(0, 1440)
    }

    /**
     * Son [gunSayisi] günün yyyy-MM-dd biçiminde gün anahtarları listesini döner.
     */
    fun gecmisGunListeYarat(gunSayisi: Int, referansMs: Long): List<String> {
        val n = gunSayisi.coerceIn(1, 30)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance().apply { timeInMillis = referansMs }
        for (i in 0 until n) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }

    /**
     * Yyyy-MM-dd anahtarını kullanıcı dostu tarihe çevirir (ör. "10 Ağustos").
     */
    fun gunAdiFormatla(gunKey: String): String {
        return try {
            val parseSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parseSdf.parse(gunKey) ?: return gunKey
            val outSdf = SimpleDateFormat("d MMMM", Locale("tr"))
            outSdf.format(date)
        } catch (_: Exception) {
            gunKey
        }
    }
}
