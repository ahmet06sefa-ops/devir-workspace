package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject

/**
 * v10.38 · Katalog #25 — nota hatırlatıcı bağlama.
 *
 * Seçilen not, seçilen tarih+saatte bildirim verir. Kayıtlar tek
 * JSON haritada tutulur (notId -> tetik ms); alarm [AlarmManager]
 * üzerinden kurulur, bildirim [NotHatirlaticiReceiver]'dan yayınlanır.
 * Tek atımlıktır: bildirim gösterilince kayıt silinir.
 * Yeniden başlatma/güncelleme sonrası [yenidenKur] gelecekteki
 * kayıtları tekrar kurar (BootReceiver çağırır).
 */
object NotHatirlatici {

    const val KANAL = "not_hatirlatici_v1"
    const val EXTRA_NOT_ID = "not_id"
    const val EXTRA_NOT_BASLIK = "not_baslik"

    private const val PREF = "not_hatirlat_v1"
    private const val K_HARITA = "harita"

    private fun pref(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Saf: harita -> JSON metni. */
    fun haritayaYaz(harita: Map<Long, Long>): String {
        val o = JSONObject()
        harita.forEach { (k, v) -> o.put(k.toString(), v) }
        return o.toString()
    }

    /** Saf: JSON metni -> harita; bozuk veride boş harita (patlamaz). */
    fun haritadanOku(ham: String?): Map<Long, Long> = try {
        val o = JSONObject(ham ?: "{}")
        val m = mutableMapOf<Long, Long>()
        val it = o.keys()
        while (it.hasNext()) {
            val k = it.next()
            k.toLongOrNull()?.let { id -> m[id] = o.getLong(k) }
        }
        m
    } catch (_: Exception) {
        emptyMap()
    }

    /** Saf: bildirim/alarm kimliği — görev alarm aralığından (7000+) uzak. */
    fun notifId(notId: Long): Int = 910000 + (notId % 100000).toInt()

    /** Notta kurulu hatırlatıcının tetik zamanı; yoksa 0. */
    fun zaman(c: Context, notId: Long): Long =
        haritadanOku(pref(c).getString(K_HARITA, "{}"))[notId] ?: 0L

    /** Hatırlatıcıyı kaydeder ve alarmı kurar. Başarıysa true. */
    fun kur(c: Context, notId: Long, baslik: String, atMillis: Long): Boolean = try {
        val h = haritadanOku(pref(c).getString(K_HARITA, "{}")) + (notId to atMillis)
        pref(c).edit().putString(K_HARITA, haritayaYaz(h)).apply()
        alarmKur(c, notId, baslik, atMillis)
        true
    } catch (_: Exception) {
        false
    }

    /** Varsa hatırlatıcıyı ve alarmını kaldırır. */
    fun iptal(c: Context, notId: Long) {
        runCatching { alarmIptal(c, notId) }
        runCatching { kaydiSil(c, notId) }
    }

    /** Yalnızca kaydı siler (alıcı, bildirim gösterdikten sonra çağırır). */
    fun kaydiSil(c: Context, notId: Long) {
        val h = haritadanOku(pref(c).getString(K_HARITA, "{}")) - notId
        pref(c).edit().putString(K_HARITA, haritayaYaz(h)).apply()
    }

    /**
     * Yeniden başlatma/güncelleme sonrası: gelecekteki kayıtları tekrar
     * kurar, geçmiş kayıtları ayıklar. Kurulan alarm sayısını döndürür.
     */
    fun yenidenKur(c: Context): Int {
        val simdi = System.currentTimeMillis()
        val h = haritadanOku(pref(c).getString(K_HARITA, "{}"))
        val saglam = h.filterValues { it > simdi }
        if (saglam.size != h.size) {
            pref(c).edit().putString(K_HARITA, haritayaYaz(saglam)).apply()
        }
        var kurulan = 0
        saglam.forEach { (id, ms) ->
            runCatching {
                alarmKur(c, id, "", ms)
                kurulan++
            }
        }
        return kurulan
    }

    private fun pending(c: Context, notId: Long, baslik: String): PendingIntent =
        PendingIntent.getBroadcast(
            c,
            notifId(notId),
            Intent(c, NotHatirlaticiReceiver::class.java).apply {
                putExtra(EXTRA_NOT_ID, notId)
                putExtra(EXTRA_NOT_BASLIK, baslik)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun alarmKur(c: Context, notId: Long, baslik: String, atMillis: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(c, notId, baslik)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // İzin yoksa en yakın yaklaşık kurulum (Android politikası).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun alarmIptal(c: Context, notId: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(c, notId, ""))
    }
}
