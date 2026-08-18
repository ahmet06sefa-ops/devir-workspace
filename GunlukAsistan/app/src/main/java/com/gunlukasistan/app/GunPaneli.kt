package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * v10.15 · ULTRA-30 / C15 — Kilit ekranı gün paneli (kalıcı sessiz bildirim).
 *
 * ── Tarama kanıtı ──
 * Kalıcı bildirim yalnız sayaç koşarken vardı (`TimerNotifier`); kalan
 * görev + bir sonraki kapı taşıyan sürekli özet yoktu. `BildirimKilit`
 * (v7.56) yönetici kilididir, konu dışı.
 *
 * ── İçerik ──
 * · Bugün kalan görev sayısı (vadesi bugün sonuna kadar olan,
 *   tamamlanmamış)
 * · Sayaç durumu (koşuyorsa kalan/geçen — `TimerEngine` tek gerçek)
 * · Bir sonraki kapı (v10.9 uyku çerçevesi: yatış ya da uyanış)
 *
 * ── Davranış ──
 * Kapatılabilir: bildirimdeki "Kapat" aksiyonu anahtarı söndürür.
 * Tazeleme tek kapıdan: `WidgetCommon.refreshAll` her tazelemede
 * `tazele()` çağırır — görev/sayaç/uyku değişimlerinin hepsi zaten
 * o kapıdan geçiyor. Aç/kapa anahtarı bildirim ayarlarındadır.
 * Dürüst not: kanal IMPORTANCE_LOW'dur; kilit ekranında içerik
 * görünürlüğü ayrıca sistemin gizlilik ayarına bağlıdır.
 */
object GunPaneli {

    const val KANAL = "ch_gunpanel_v1"
    const val NOTIF_ID = 4931
    const val PI_ANA = 4932
    const val PI_KAPAT = 4933
    private const val PREF = "gun_paneli_v1"
    private const val K_ACIK = "acik"

    fun acikMi(context: Context): Boolean =
        context.getSharedPreferences(PREF, 0).getBoolean(K_ACIK, false)

    fun ayarla(context: Context, acik: Boolean) {
        context.getSharedPreferences(PREF, 0).edit().putBoolean(K_ACIK, acik).apply()
        if (acik) tazele(context) else kapat(context)
    }

    fun kapat(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIF_ID)
    }

    private fun kanal(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(KANAL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        KANAL,
                        context.getString(R.string.gc_panel_kanal),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.gc_panel_kanal_alt)
                        setSound(null, null); enableVibration(false)
                    }
                )
            }
        }
    }

    /** Üç satırlık içerik: (görev, sayaç, kapı). Cihaz durumu okur. */
    fun satirlar(context: Context): Triple<String, String, String> {
        // 1) Bugün kalan görevler (vadesi bugün sonuna kadar + tamamlanmamış)
        val gunSonu = WidgetCommon.endOfToday()
        val kalan = runCatching {
            Store.loadTasks(context).count { !it.done && it.dueAt in 1..gunSonu }
        }.getOrDefault(0)
        val gorevSatiri = context.getString(R.string.gc_panel_gorev, kalan)

        // 2) Sayaç
        val sayacSatiri = if (runCatching { TimerEngine.isRunning(context) }.getOrDefault(false)) {
            val ms = TimerEngine.displayMs(context)
            val kronometreMi = runCatching { TimerEngine.mode(context) == TimerEngine.MODE_WATCH }.getOrDefault(false)
            context.getString(
                if (kronometreMi) R.string.gc_panel_sayac_krono else R.string.gc_panel_sayac,
                bicim(ms)
            )
        } else context.getString(R.string.gc_panel_sayac_yok)

        // 3) Bir sonraki kapı (uyku çerçevesi)
        val cal = java.util.Calendar.getInstance()
        val dk = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val kapiSatiri = runCatching {
            if (dk < UykuCerceve.aksamDk(context)) {
                context.getString(R.string.gc_panel_kapi_yatis, UykuCerceve.saatMetni(UykuCerceve.aksamDk(context)))
            } else {
                context.getString(R.string.gc_panel_kapi_uyanis, UykuCerceve.saatMetni(UykuCerceve.sabahDk(context)))
            }
        }.getOrDefault(context.getString(R.string.gc_panel_kapi_yok))
        return Triple(gorevSatiri, sayacSatiri, kapiSatiri)
    }

    private fun bicim(ms: Long): String {
        val t = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(t / 60, t % 60)
    }

    /** Tek tazeleme kapısı — `WidgetCommon.refreshAll` çağırır. */
    fun tazele(context: Context) {
        if (!acikMi(context)) return
        kanal(context)
        val (g, s, k) = satirlar(context)
        val ana = PendingIntent.getActivity(
            context, PI_ANA,
            Intent(context, MainActivity::class.java).apply {
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TODAY)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val kapa = PendingIntent.getBroadcast(
            context, PI_KAPAT,
            Intent(context, GunPaneliReceiver::class.java).setAction(GunPaneliReceiver.ACTION_KAPAT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_task_alt)
            .setContentTitle(g)
            .setContentText("$s · $k")
            .setStyle(NotificationCompat.InboxStyle().addLine(g).addLine(s).addLine(k))
            .setOngoing(true).setSilent(true)
            .setContentIntent(ana)
            .addAction(0, context.getString(R.string.gc_panel_kapat), kapa)
            .setShowWhen(false)
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, b.build())
        }
    }
}
