package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build

/**
 * v7.3 — Kısayolları **kod ile** kaydeder (dinamik kısayol).
 *
 * Neden gerekti: `res/xml/shortcuts.xml` ile tanımlanan *statik* kısayollar
 * bazı başlatıcılarda (özellikle Samsung One UI) görünmüyordu. Dinamik
 * kısayollar `ShortcutManager` üzerinden doğrudan sisteme yazılır ve
 * her başlatıcıda güvenilir biçimde çıkar.
 *
 * `MainActivity.onCreate` içinde çağrılır — uygulama bir kez açıldıktan
 * sonra simgeye uzun basınca kısayollar görünür.
 */
object Shortcuts {

    /** Kısayolları oluşturur/tazeler. API 25 altında sessizce çıkar. */
    fun install(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return

        try {
            val list = listOf(
                build(
                    context, "quick_task",
                    context.getString(R.string.sc_task_short),
                    context.getString(R.string.sc_task_long),
                    R.drawable.ic_task_alt,
                    Intent(context, QuickAddActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(QuickAddActivity.EXTRA_MODE, QuickAddActivity.MODE_TASK)
                    }
                ),
                build(
                    context, "quick_note",
                    context.getString(R.string.sc_note_short),
                    context.getString(R.string.sc_note_long),
                    R.drawable.ic_edit_note,
                    Intent(context, QuickAddActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(QuickAddActivity.EXTRA_MODE, QuickAddActivity.MODE_NOTE)
                    }
                ),
                build(
                    context, "quick_focus",
                    context.getString(R.string.sc_focus_short),
                    context.getString(R.string.sc_focus_long),
                    R.drawable.ic_timer,
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        // Not: statik XML'de bu değer string olarak gidiyordu ve
                        // getIntExtra onu okuyamıyordu. Kodda gerçek Int yazılır.
                        putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TIMER)
                    }
                ),
                build(
                    context, "quick_today",
                    context.getString(R.string.sc_today_short),
                    context.getString(R.string.sc_today_long),
                    R.drawable.ic_today,
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TODAY)
                    }
                )
            )
            manager.dynamicShortcuts = list
        } catch (_: Exception) {
            // Kısayol kaydı başarısız olursa uygulama yine de çalışmalı
        }
    }

    private fun build(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        iconRes: Int,
        intent: Intent
    ): ShortcutInfo {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(Icon.createWithResource(context, iconRes))
            .setIntent(intent)
            .build()
    }
}
