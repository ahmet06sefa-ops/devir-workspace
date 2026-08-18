package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Geri sayım bittiğinde uygulama kapalı olsa bile haber veren alarm (v6.4).
 */
object TimerAlarm {

    private const val REQUEST = 4713

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST,
        Intent(context, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_FINISHED
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Çalışan bir geri sayım varsa bitiş anına alarm kurar, yoksa iptal eder. */
    fun reschedule(context: Context) {
        cancel(context)
        if (!TimerEngine.isRunning(context)) return
        if (TimerEngine.mode(context) != TimerEngine.MODE_DOWN) {
            // v10.3 · B25: kronometre modunda bitiş alarmı yok ama
            // uyumluluk modundaki metin + B19 dakika ikonu ancak
            // tazeleme zinciri varsa güncel kalır. Eskiden kronometre
            // hiç zincir kurmuyordu; bildirim donuyordu.
            tazelemeyiKur(context)
            return
        }

        // v7.93: bildirim tazeleme alarmını da kur
        tazelemeyiKur(context)

        val remaining = TimerEngine.remainingMs(context)
        if (remaining <= 0L) return
        val at = System.currentTimeMillis() + remaining

        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
            }
        } catch (_: Exception) {
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.93 — PERİYODİK BİLDİRİM TAZELEME
    // ═══════════════════════════════════════════════════════════════

    private const val REQUEST_TAZELE = 4714

    /** Tazeleme aralığı — uyumluluk modunda süre düz metin olduğu için gerekli. */
    private const val TAZELE_ARALIK = 15_000L

    private fun tazelePending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_TAZELE,
        Intent(context, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_TAZELE
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Bildirim metnini periyodik tazeleyen alarmı kurar.
     *
     * ── Neden gerekli ──
     * Uyumluluk modunda ([SayacAyar.uyumlulukModu]) bildirimde kronometre
     * yok; süre düz metin. Uygulama kapalıyken ve ön plan servisi devre
     * dışıyken kimse bildirimi tazelemiyor, süre donuyordu.
     *
     * ── Neden inexact ──
     * Saniye hassasiyeti gerekmiyor; alt satırdaki **bitiş saati** zaten
     * hiç bayatlamıyor. `setAndAllowWhileIdle` pil dostu ve Doze'da da
     * çalışıyor. Kesin alarm kotasını bitiş bildirimi için saklıyoruz.
     */
    fun tazelemeyiKur(context: Context) {
        tazelemeyiIptalEt(context)
        if (!TimerEngine.isRunning(context)) return
        if (!SayacAyar.uyumlulukModu(context)) return
        if (!SayacAyar.miniGoster(context)) return

        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TAZELE_ARALIK,
                tazelePending(context)
            )
        } catch (e: Exception) {
            android.util.Log.w("TimerAlarm", "Tazeleme kurulamadı", e)
        }
    }

    fun tazelemeyiIptalEt(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(tazelePending(context))
        } catch (e: Exception) {
            android.util.Log.w("TimerAlarm", "Tazeleme iptal edilemedi", e)
        }
    }

    fun cancel(context: Context) {
        tazelemeyiIptalEt(context)
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(context))
        } catch (_: Exception) {
        }
    }
}
