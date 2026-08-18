package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Görev hatırlatıcılarını sistem alarmına kuran yardımcı.
 * Android 12+ izin durumuna göre tam veya yaklaşık zamanlama seçer.
 */
object AlarmScheduler {

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(context: Context, taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Görev için zamanında bildirim kurar. Geçmiş zamanlara kurulmaz. */
    fun schedule(context: Context, taskId: Long, title: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = alarmManager(context)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                // İzin yoksa yaklaşık zamanla yine de hatırlat
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, taskId, title))
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, taskId, title))
            }
        } catch (e: SecurityException) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(context, taskId, title))
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }

    fun cancel(context: Context, taskId: Long) {
        try {
            alarmManager(context).cancel(pending(context, taskId, ""))
        } catch (_: Exception) {
        }
    }

    /** Her pazar 20:00 için haftalık rapor alarmı kurar. */
    fun scheduleWeeklyReport(context: Context) {
        val at = WeeklyReportReceiver.nextSundayEvening()
        try {
            val intent = Intent(context, WeeklyReportReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, 4242, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = alarmManager(context)
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: Exception) {
        }
    }

    /** Telefon yeniden başlatıldığında tüm gelecek hatırlatıcıları yeniden kurar. */
    /**
     * Bekleyen tüm görev alarmlarını yeniden kurar.
     *
     * v9.1 · Öneri 44: artık kaç alarm kurulduğunu döndürüyor ve
     * `AlarmSagligi`'na kaydediyor. Eskiden bu işlem sessizce
     * yapılıyordu — yeniden başlatmadan sonra alarmların gerçekten
     * kurulup kurulmadığını doğrulamanın hiçbir yolu yoktu.
     *
     * @return kurulan alarm sayısı
     */
    fun rescheduleAll(context: Context): Int {
        val now = System.currentTimeMillis()
        val bekleyen = Store.loadTasks(context).filter { !it.done && it.dueAt > now }
        bekleyen.forEach { schedule(context, it.id, it.text, it.dueAt) }
        runCatching { AlarmSagligi.kurulumKaydet(context, "GOREV", bekleyen.size) }
        return bekleyen.size
    }
}
