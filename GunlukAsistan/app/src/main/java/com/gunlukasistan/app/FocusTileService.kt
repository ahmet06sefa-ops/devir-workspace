package com.gunlukasistan.app

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * v7.1 — Bildirim panelindeki hızlı ayarlar kutucuğu.
 *
 * Perdeyi aşağı çekip kutucuğa basınca pomodoro/sayaç uygulamayı açmadan
 * başlar veya durur. Kutucuk çalışırken kalan süreyi alt yazı olarak gösterir.
 *
 * Android 7.0 (API 24) ve üzeri destekler — minSdk zaten 24.
 */
@TargetApi(Build.VERSION_CODES.N)
class FocusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        paint()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        try {
            if (TimerEngine.isRunning(context)) {
                TimerEngine.pause(context)
                TimerAlarm.cancel(context)
            } else {
                // Süre hiç ayarlanmadıysa varsayılan odak süresini kur
                if (TimerEngine.mode(context) == TimerEngine.MODE_DOWN &&
                    TimerEngine.remainingMs(context) <= 0L
                ) {
                    val minutes = Store.getGoalMinutes(context).coerceIn(5, 120)
                    TimerEngine.setTotalMs(context, minutes * 60_000L)
                }
                TimerEngine.start(context)
                TimerAlarm.reschedule(context)
            }
            TimerNotifier.show(context)
        } catch (_: Exception) {
        }
        paint()
    }

    /** Kutucuğun görünümünü güncel duruma göre çizer. */
    private fun paint() {
        val tile = qsTile ?: return
        val context = applicationContext
        try {
            val running = TimerEngine.isRunning(context)
            tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_focus)
            tile.icon = Icon.createWithResource(context, R.drawable.ic_timer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (running) {
                    TimerEngine.format(TimerEngine.displayMs(context))
                } else {
                    getString(R.string.tile_focus_idle)
                }
            }
            tile.updateTile()
        } catch (_: Exception) {
        }
    }
}
