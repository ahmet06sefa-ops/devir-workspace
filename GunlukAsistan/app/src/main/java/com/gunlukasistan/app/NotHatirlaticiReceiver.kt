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

/**
 * v10.38 · Katalog #25 — not hatırlatıcısı bildirim alıcısı.
 * Alarm tetiklenince tek atımlık bildirim yayınlar ve kaydı siler.
 * İzin yoksa sessizce vazgeçer (Android 13+ politikası).
 */
class NotHatirlaticiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notId = intent.getLongExtra(NotHatirlatici.EXTRA_NOT_ID, 0L)
        if (notId <= 0L) return
        val baslik = intent.getStringExtra(NotHatirlatici.EXTRA_NOT_BASLIK).orEmpty()
        runCatching { NotHatirlatici.kaydiSil(context, notId) }
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        NotHatirlatici.KANAL,
                        context.getString(R.string.w38_kanal_ad),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.w38_kanal_aciklama)
                    }
                )
            }
            val ac = PendingIntent.getActivity(
                context,
                NotHatirlatici.notifId(notId),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val metin = if (baslik.isBlank()) {
                context.getString(R.string.w38_hatirlat_genel)
            } else {
                baslik
            }
            val b = NotificationCompat.Builder(context, NotHatirlatici.KANAL)
                .setSmallIcon(R.drawable.ic_task_alt)
                .setContentTitle(context.getString(R.string.w38_bildirim_baslik))
                .setContentText(metin)
                .setAutoCancel(true)
                .setContentIntent(ac)
            NotificationManagerCompat.from(context).notify(NotHatirlatici.notifId(notId), b.build())
        }
    }
}
