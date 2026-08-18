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
import java.util.concurrent.Executors

/**
 * Görev hatırlatma zamanı geldiğinde çalışır ve bildirim gösterir.
 *
 * v5.2: bildirim üzerinde "Tamamlandı" ve "15 dk ertele" butonları,
 *       ayrıca günün özetini gösteren genişletilebilir metin.
 *
 * v10.15 · ULTRA-30 / GRUP C eklentileri:
 *  · C13 — 🔴 "acil" etiketli görevler: fullScreenIntent + ALARM
 *    kategorili bildirim, kilit üstünde [GorevAlarmActivity] açılır
 *    (kademeli erteleme bedeliyle).
 *  · C14 — Tür başına sessiz pencere: [SessizTurler.susturMu] true
 *    ise bildirim sessiz ikincil kanaldan, düşük öncelikle düşer
 *    (kaybolmaz; ses/titreşim olmaz).
 *  · C16 — Öğrenen hatırlatıcının son kararı genişletilmiş metne
 *    "neden bu saat" açıklaması olarak yazılır.
 *  · C18 — Aynı 10 dk dilimindeki hatırlatıcılar grup özetinde satır
 *    satır ([HatirlaticiDemeti]); her üyenin tekil ✓/ertele
 *    aksiyonları aynen korunur.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val CHANNEL_ID = "gorev_hatirlatici_v1"
        /** C13: kritik görevlerin alarm kanalı (USAGE_ALARM ses). */
        const val CHANNEL_KRITIK = "gorev_kritik_v1"
        /** C14: sessiz penceredeki görevlerin düşük öncelikli kanalı. */
        const val CHANNEL_SESSIZ = "gorev_hatirlatici_sessiz_v1"
        /** C18: demet grup anahtarı + özet bildirimi. */
        const val GRUP = "grp_gorev_demet"
        const val DEMET_NOTIF_ID = 4934
        const val DEMET_PI = 4935

        private fun notifId(id: Long) = (id % 50000).toInt() + 7000
    }

    override fun onReceive(context: Context, intent: Intent) {
        // v9.1 · Öneri 44: alarm gerçekten tetiklendi.
        runCatching { AlarmSagligi.tetiklendiKaydet(context) }
        if (!Store.getNotifEnabled(context)) return
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val id = intent.getLongExtra(EXTRA_TASK_ID, 0L)

        // Görev bu arada tamamlandıysa/silindiyse bildirim gösterme
        val appCtx = context.applicationContext
        val odev = Executors.newSingleThreadExecutor()
        odev.execute {
            try {
                hatirlaticiyiYay(appCtx, id, title)
            } finally {
                odev.shutdown()
            }
        }
    }

    /** IO saatli kısım (arka plan iş parçacığı): depo okuma + bildirim üretimi. */
    private fun hatirlaticiyiYay(context: Context, id: Long, title: String) {
        val tasks = Store.loadTasks(context)
        val task = tasks.firstOrNull { it.id == id }
        if (task != null && task.done) return

        createChannel(context)

        // ── C18: demet hesabı (tetikleyicinin vadesi merkez alınır) ──
        val tetikMs = task?.dueAt ?: System.currentTimeMillis()
        val demet = HatirlaticiDemeti.demetKur(tasks, tetikMs, { it.dueAt }, { it.done })
            .filter { it.id != id } // tetikleyici zaten aşağıda gösteriliyor
        val demetliMi = demet.isNotEmpty()

        // ── C13: kritik görev tam ekran yoluna sapar ──
        val kritikMi = task?.etiket == "c"

        gosterTekil(context, id, title, tasks, kritikMi, demetliMi)

        // Demet üyeleri de tekil bildirim kazanır (aksiyonları tam);
        // vadesi gelmemiş üyeler 10 dk erken görünür — paketin amacı bu.
        for (u in demet) {
            gosterTekil(context, u.id, u.text, tasks, u.etiket == "c", demetliMi = true)
        }
        if (demetliMi) gosterDemetOzeti(context, id, title, demet, tetikMs)
    }

    // ── Tekil bildirim (eski davranış + C13/C14/C16 katmanları) ──────

    private fun gosterTekil(
        context: Context,
        id: Long,
        title: String,
        tasks: List<Store.Task>,
        kritikMi: Boolean,
        demetliMi: Boolean,
    ) {
        val notifId = notifId(id)

        val open = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Tamamlandı" butonu
        val donePending = PendingIntent.getBroadcast(
            context,
            notifId * 2,
            Intent(context, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_DONE
                putExtra(TaskActionReceiver.EXTRA_TASK_ID, id)
                putExtra(TaskActionReceiver.EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "15 dk ertele" butonu
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notifId * 2 + 1,
            Intent(context, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_SNOOZE
                putExtra(TaskActionReceiver.EXTRA_TASK_ID, id)
                putExtra(TaskActionReceiver.EXTRA_TASK_TITLE, title)
                putExtra(TaskActionReceiver.EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Genişletilince görünen günün özeti (+ C16 "neden bu saat")
        val focus = Store.getTodayFocusMinutes(context)
        val goal = Store.getGoalMinutes(context)
        val questions = Store.getTodayQuestions(context)
        val (streak, _) = Store.streakInfo(context)
        val remaining = tasks.count { !it.done }
        val bigText = buildString {
            append(title)
            append("\n\n")
            append(context.getString(R.string.notif_summary, focus, goal, questions, streak))
            if (remaining > 1) {
                append("\n")
                append(context.getString(R.string.notif_remaining, remaining - 1))
            }
            // C16: öğrenen hatırlatıcı şeffaflık notu
            OgrenenHatirlatici.neden(context, id)?.let { (adet, kay) ->
                append("\n")
                append(context.getString(R.string.gc_neden_saat, adet, kotlin.math.abs(kay)))
            }
        }

        // C14: tür sessiz penceresi kararı
        val sessizde = runCatching { SessizTurler.susturMu(context, CHANNEL_ID) }.getOrDefault(false)

        val builder = NotificationCompat.Builder(
            context,
            when {
                kritikMi -> CHANNEL_KRITIK
                sessizde -> CHANNEL_SESSIZ
                else -> CHANNEL_ID
            }
        )
            .setSmallIcon(R.drawable.ic_task_alt)
            .setContentTitle(context.getString(if (kritikMi) R.string.gc_kritik_baslik else R.string.notif_task_title))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(
                when {
                    kritikMi -> NotificationCompat.PRIORITY_MAX
                    sessizde -> NotificationCompat.PRIORITY_LOW
                    else -> NotificationCompat.PRIORITY_HIGH
                }
            )
            .setCategory(
                if (kritikMi) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_REMINDER
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_action_done), donePending)
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePending)

        if (demetliMi) builder.setGroup(GRUP)

        // C13: kritik görev — kilit üstü tam ekran
        if (kritikMi) {
            createChannelKritik(context)
            val tamEkran = PendingIntent.getActivity(
                context,
                notifId,
                Intent(context, GorevAlarmActivity::class.java).apply {
                    putExtra("gorev_id", id)
                    putExtra("gorev_baslik", title)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(tamEkran, true)
        } else {
            // v7.74 aksiyonları yalnız normal yolda (tam ekranda butonlar zaten var)
            builder
                // v7.74: golgelikten metin yazip yeni gorev ekle
                .addAction(TaskActionReceiver.yazEylemi(context, notifId))
                // v7.74: bu gorevi yarin sabaha at
                .addAction(TaskActionReceiver.yarinEylemi(context, id, title, notifId))
        }

        if (!Store.getVibEnabled(context)) {
            builder.setVibrate(longArrayOf(0L))
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: Exception) {
        }
    }

    // ── C18: demet özeti (grup summary — satır satır) ─────────────────

    private fun gosterDemetOzeti(
        context: Context,
        tetikleyiciId: Long,
        tetikleyiciBaslik: String,
        uyeler: List<Store.Task>,
        tetikMs: Long,
    ) {
        val stil = NotificationCompat.InboxStyle()
        stil.addLine(HatirlaticiDemeti.satirMetni(tetikMs, tetikleyiciBaslik))
        uyeler.take(HatirlaticiDemeti.MAKS_SATIR - 1).forEach {
            stil.addLine(HatirlaticiDemeti.satirMetni(it.dueAt, it.text))
        }
        val toplam = 1 + uyeler.size
        if (toplam > HatirlaticiDemeti.MAKS_SATIR) {
            stil.addLine(HatirlaticiDemeti.tasmaMetni(toplam - HatirlaticiDemeti.MAKS_SATIR))
        }
        val ac = PendingIntent.getActivity(
            context, DEMET_PI,
            Intent(context, MainActivity::class.java).apply {
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TASKS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_alt)
            .setContentTitle(context.getString(R.string.gc_demet_baslik, toplam))
            .setContentText(context.getString(R.string.gc_demet_alt))
            .setStyle(stil)
            .setGroup(GRUP)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(ac)
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(DEMET_NOTIF_ID, b.build())
        } catch (_: Exception) {
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.tasks_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            // C14: sessiz pencere kanalı — ses/titreşim yok, kayıt düşük
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SESSIZ,
                    context.getString(R.string.gc_kanal_sessiz),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setSound(null, null); enableVibration(false) }
            )
        }
    }

    /** C13: kritik görev kanalı — alarm sesi akışı (USAGE_ALARM). */
    private fun createChannelKritik(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_KRITIK) != null) return
            val ozn = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_KRITIK,
                    context.getString(R.string.gc_kanal_kritik),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_ALARM
                        ),
                        ozn
                    )
                    setBypassDnd(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
            )
        }
    }
}
