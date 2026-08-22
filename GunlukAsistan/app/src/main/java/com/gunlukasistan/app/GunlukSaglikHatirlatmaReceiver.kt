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
 * v11.66 — Günlük sağlık hatırlatma bildirimi.
 *
 * Her akşam 21:00'de kullanıcıya ruh hali, uyku ve beslenme kaydetmeyi
 * hatırlatır. Bildirime dokununca Sağlık Karnesi açılır; kısayol eylemi
 * ile Ruh Hali ekranına gidilebilir.
 */
class GunlukSaglikHatirlatmaReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "gunluk_saglik_v1"
        const val NOTIF_ID = 441

        /** Bir sonraki 21:00 zaman damgasını döndürür. */
        fun nextEvening(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 21)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Store.getNotifEnabled(context)) {
            createChannel(context)
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, SaglikOzetActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val moodAc = PendingIntent.getActivity(
                context, 1,
                Intent(context, MoodActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("🩺 Günlük sağlık kaydın zamanı")
                .setContentText("Bugün nasıl hissediyorsun? Uyku ve beslenmeni kaydet.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(0, "🎭 Ruh hali", moodAc)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            } catch (_: Exception) {
            }
        }
        // Gelecek günü kur
        AlarmScheduler.scheduleDailyHealth(context)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.gunluk_saglik_kanal),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
