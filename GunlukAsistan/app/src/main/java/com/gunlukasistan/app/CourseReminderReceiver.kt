package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * v7.9 — Günlük ders çalışma hatırlatıcısı.
 *
 * Kullanıcının belirlediği saatte "bugün ders çalıştın mı?" bildirimi gönderir.
 * Bildirim, kaldığı yerden devam edebilmesi için ilerleme bilgisini de gösterir.
 */
class CourseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "kurs_hatirlatici_v1"
        const val NOTIF_ID = 5150
        const val REQUEST_CODE = 5150

        private const val PREF = "kurs_hatirlatici"
        private const val K_ENABLED = "acik"
        private const val K_HOUR = "saat"
        private const val K_MINUTE = "dakika"

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        fun isEnabled(context: Context): Boolean =
            prefs(context).getBoolean(K_ENABLED, false)

        fun hour(context: Context): Int = prefs(context).getInt(K_HOUR, 20)
        fun minute(context: Context): Int = prefs(context).getInt(K_MINUTE, 0)

        /** Hatırlatıcıyı açar/kapatır ve alarmı yeniden kurar. */
        fun setEnabled(context: Context, value: Boolean) {
            prefs(context).edit().putBoolean(K_ENABLED, value).apply()
            if (value) schedule(context) else cancel(context)
        }

        /** Hatırlatma saatini ayarlar. */
        fun setTime(context: Context, h: Int, m: Int) {
            prefs(context).edit().putInt(K_HOUR, h).putInt(K_MINUTE, m).apply()
            if (isEnabled(context)) schedule(context)
        }

        /** Bir sonraki hatırlatma zamanını hesaplar. */
        fun nextTime(context: Context): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour(context))
                set(Calendar.MINUTE, minute(context))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        /** Günlük alarmı kurar. */
        fun schedule(context: Context) {
            if (!isEnabled(context)) return
            try {
                val intent = Intent(context, CourseReminderReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val am = context.getSystemService(Context.ALARM_SERVICE)
                    as android.app.AlarmManager
                val at = nextTime(context)
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi)
                } else {
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi)
                }
            } catch (e: Exception) {
                android.util.Log.w("CourseReminder", "Alarm kurulamadı", e)
            }
        }

        fun cancel(context: Context) {
            try {
                val intent = Intent(context, CourseReminderReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                (context.getSystemService(Context.ALARM_SERVICE)
                    as android.app.AlarmManager).cancel(pi)
            } catch (e: Exception) {
                android.util.Log.w("CourseReminder", "Alarm iptal edilemedi", e)
            }
        }

        /** Saat metni: "20:00" */
        fun timeLabel(context: Context): String =
            String.format(java.util.Locale.US, "%02d:%02d", hour(context), minute(context))
    }

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            göster(context)
        } catch (e: Exception) {
            android.util.Log.w("CourseReminder", "Bildirim gösterilemedi", e)
        }
        // Ertesi gün için yeniden kur
        schedule(context)
    }

    private fun göster(context: Context) {
        if (!Store.getNotifEnabled(context)) return

        val dersler = Store.loadLessons(context)
        if (dersler.isEmpty()) return

        val tamam = dersler.count { it.done }
        val toplam = dersler.size
        val yuzde = if (toplam == 0) 0 else tamam * 100 / toplam

        // Sıradaki tamamlanmamış ders
        val sonraki = dersler.filter { !it.done }.minByOrNull { it.order }
        val kursAdi = sonraki?.let { l ->
            Store.loadCourses(context).firstOrNull { it.id == l.courseId }?.title
        }.orEmpty()

        val baslik = context.getString(R.string.cr_title)
        val govde = if (sonraki != null) {
            context.getString(R.string.cr_body_next, sonraki.title, yuzde)
        } else {
            context.getString(R.string.cr_body_done, toplam)
        }

        val genis = buildString {
            append(govde)
            if (kursAdi.isNotBlank()) append("\n📘 ").append(kursAdi)
            append("\n📊 ").append(
                context.getString(R.string.cr_progress, tamam, toplam, yuzde)
            )
        }

        createChannel(context)

        val açIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, 13)  // Mühendislik ekranı
        }
        val pi = PendingIntent.getActivity(
            context, REQUEST_CODE, açIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle(baslik)
            .setContentText(govde)
            .setStyle(NotificationCompat.BigTextStyle().bigText(genis))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(0, context.getString(R.string.cr_action_open), pi)

        // v10.12 · Grup D / D24 — derse kenetli odak: tek dokunuşla sayaç
        // sıradaki dersin etiketiyle başlar. Koşan oturum bölünmez; sayaç
        // çalışırken düğme gereksiz (bildirim zaten sayaç panelinde).
        if (!TimerEngine.isRunning(context)) {
            val odakDk = SayacAyar.varsayilanDk(context)
            val odakNiyet = Intent(context, TimerActionReceiver::class.java).apply {
                action = TimerActionReceiver.ACTION_BASLAT_DK
                putExtra(TimerActionReceiver.EXTRA_DAKIKA, odakDk)
                sonraki?.let { putExtra(TimerActionReceiver.EXTRA_ETIKET, it.title) }
            }
            val odakPi = PendingIntent.getBroadcast(
                context, 5151, odakNiyet,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            b.addAction(0, context.getString(R.string.fo_ders_odak, odakDk), odakPi)
        }

        if (!Store.getVibEnabled(context)) b.setVibrate(longArrayOf(0L))

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, b.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS izni yok
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.cr_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.cr_channel_desc)
        }
        nm.createNotificationChannel(ch)
    }
}
