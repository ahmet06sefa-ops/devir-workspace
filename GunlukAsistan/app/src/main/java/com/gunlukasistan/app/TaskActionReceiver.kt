package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

/**
 * Bildirim üzerindeki butonları işler (v5.2):
 *  • "Tamamlandı" → görevi işaretler, bildirimi kapatır
 *  • "15 dk ertele" → hatırlatıcıyı yeniden kurar
 */
class TaskActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.gunlukasistan.app.TASK_DONE"
        const val ACTION_SNOOZE = "com.gunlukasistan.app.TASK_SNOOZE"

        /** v7.74: bildirimden metin yazip yeni gorev ekleme. */
        const val ACTION_YAZ = "com.gunlukasistan.app.TASK_YAZ"

        /** v7.74: gorevi yarina atma. */
        const val ACTION_YARIN = "com.gunlukasistan.app.TASK_YARIN"

        /** RemoteInput anahtari — yazilan metin bu anahtarla gelir. */
        const val KEY_YANIT = "hizli_yanit"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_NOTIF_ID = "notif_id"

        const val SNOOZE_MINUTES = 15

        /**
         * v7.74 — Bildirime "Yaz" eylemi ekler.
         *
         * `RemoteInput` sayesinde kullanici bildirimi acmadan, dogrudan
         * golgelikten metin yazip gorev olusturabilir. Android 7+ destekler;
         * eski surumlerde eylem yine gorunur ama uygulamayi acar.
         */
        fun yazEylemi(
            context: Context,
            notifId: Int
        ): androidx.core.app.NotificationCompat.Action {
            val girdi = androidx.core.app.RemoteInput.Builder(KEY_YANIT)
                .setLabel(context.getString(R.string.hy_ipucu))
                .build()

            val niyet = Intent(context, TaskActionReceiver::class.java).apply {
                action = ACTION_YAZ
                putExtra(EXTRA_NOTIF_ID, notifId)
                data = android.net.Uri.parse("gunlukasistan://yaz/" + notifId)
            }
            // MUTABLE sart: RemoteInput sonucu intent'e sistem tarafindan yazilir
            val bekleyen = android.app.PendingIntent.getBroadcast(
                context, 9200 + notifId, niyet,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_MUTABLE
            )
            return androidx.core.app.NotificationCompat.Action.Builder(
                0, context.getString(R.string.hy_yaz), bekleyen
            ).addRemoteInput(girdi).build()
        }

        /** v7.74: "Yarina at" eylemi. */
        fun yarinEylemi(
            context: Context,
            taskId: Long,
            baslik: String,
            notifId: Int
        ): androidx.core.app.NotificationCompat.Action {
            val niyet = Intent(context, TaskActionReceiver::class.java).apply {
                action = ACTION_YARIN
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, baslik)
                putExtra(EXTRA_NOTIF_ID, notifId)
                data = android.net.Uri.parse("gunlukasistan://yarin/" + taskId)
            }
            val bekleyen = android.app.PendingIntent.getBroadcast(
                context, 9300 + notifId, niyet,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
            )
            return androidx.core.app.NotificationCompat.Action.Builder(
                0, context.getString(R.string.hy_yarina), bekleyen
            ).build()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""

        when (intent.action) {
            ACTION_DONE -> {
                val tasks = Store.loadTasks(context)
                val task = tasks.firstOrNull { it.id == taskId }
                if (task != null && !task.done) {
                    task.done = true
                    Store.recordCompletion(context)
                    // v7.70: tekrarliysa sonraki tarihe tasi
                    if (task.tekrarliMi) {
                        // v10.15 · C16: "yapıldı" basma saati öğrenilir (son 8)
                        OgrenenHatirlatici.kaydet(context, taskId, System.currentTimeMillis())
                        Tekrar.gorevYenile(context, task)
                    }
                    Store.saveTasks(context, tasks)
                }
                AlarmScheduler.cancel(context, taskId)
                dismiss(context, notifId)
                // v10.15 · C18: demet özeti üyeleri azaldıkça yetim kalmasın
                runCatching {
                    context.getSystemService(android.app.NotificationManager::class.java)
                        ?.cancel(ReminderReceiver.DEMET_NOTIF_ID)
                }
                WidgetCommon.refreshAll(context)
                toast(context, context.getString(R.string.notif_done_toast))
            }

            // v7.74: golgelikten metin yazip gorev ekle
            ACTION_YAZ -> {
                val yazilan = androidx.core.app.RemoteInput
                    .getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_YANIT)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                if (yazilan.isNotBlank()) {
                    try {
                        // Dogal dilden zaman ayikla: "yarin 3'te ara"
                        val zaman = try {
                            NaturalDate.parse(yazilan)
                        } catch (e: Exception) {
                            android.util.Log.w("TaskAction", "Zaman ayiklanamadi", e)
                            null
                        }
                        val govde = if (zaman != null && zaman.found &&
                            zaman.text.isNotBlank()
                        ) zaman.text else yazilan
                        val sonTarih = if (zaman != null && zaman.found) zaman.millis else 0L

                        val liste = Store.loadTasks(context)
                        val yeni = Store.Task(
                            id = System.currentTimeMillis(),
                            text = govde.take(200),
                            done = false,
                            createdAt = System.currentTimeMillis(),
                            dueAt = sonTarih
                        )
                        liste.add(yeni)
                        Store.saveTasks(context, liste)
                        if (sonTarih > 0) {
                            AlarmScheduler.schedule(context, yeni.id, yeni.text, sonTarih)
                        }
                        WidgetCommon.refreshAll(context, true)
                        toast(context, context.getString(R.string.hy_eklendi, yeni.text))
                    } catch (e: Exception) {
                        android.util.Log.w("TaskAction", "Gorev eklenemedi", e)
                    }
                }
                dismiss(context, notifId)
            }

            // v7.74: gorevi yarin sabaha at
            ACTION_YARIN -> {
                val yarin = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 9)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                try {
                    val liste = Store.loadTasks(context)
                    liste.firstOrNull { it.id == taskId }?.let {
                        it.dueAt = yarin
                        it.done = false
                        Store.saveTasks(context, liste)
                    }
                    AlarmScheduler.schedule(context, taskId, title, yarin)
                    WidgetCommon.refreshAll(context, true)
                } catch (e: Exception) {
                    android.util.Log.w("TaskAction", "Yarina atilamadi", e)
                }
                dismiss(context, notifId)
                toast(context, context.getString(R.string.hy_yarina_ok))
            }

            ACTION_SNOOZE -> {
                val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
                AlarmScheduler.schedule(context, taskId, title, at)
                dismiss(context, notifId)
                toast(
                    context,
                    context.getString(R.string.notif_snooze_toast, SNOOZE_MINUTES)
                )
            }
        }
    }

    private fun dismiss(context: Context, notifId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (_: Exception) {
        }
    }

    private fun toast(context: Context, text: String) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }
}
