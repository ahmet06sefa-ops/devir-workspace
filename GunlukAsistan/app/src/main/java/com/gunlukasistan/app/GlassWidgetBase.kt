package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast

/**
 * Cam görünümlü liste widget'larının ortak tabanı (v6.3).
 *
 * Google Tasks tarzı: yarı saydam koyu kart, dairesel onay kutuları,
 * ince ayraçlar. Satıra dokunmak öğeyi tamamlar/işaretler.
 */
abstract class GlassWidgetBase : AppWidgetProvider() {

    companion object {
        const val EXTRA_KIND = "gl_kind"
        const val EXTRA_ITEM_ID = "gl_item_id"
        const val ACTION_TOGGLE = "com.gunlukasistan.app.GLASS_TOGGLE"

        const val KIND_TASKS = "tasks"
        const val KIND_HABITS = "habits"
        const val KIND_TODAY = "today"

        /** Bu türe ait tüm widget'ları tazeler. */
        fun refresh(context: Context, cls: Class<*>, listId: Int = R.id.glList) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) {
                    manager.notifyAppWidgetViewDataChanged(ids, listId)
                    val intent = Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Alt sınıflar hangi içeriği gösterdiğini bildirir. */
    protected abstract val kind: String

    /** Başlıkta görünecek metin. */
    protected abstract fun title(context: Context): String

    /** Liste boşken gösterilecek metin. */
    protected abstract fun emptyText(context: Context): String

    /** "＋" butonuna basılınca açılacak hızlı eylem. */
    protected abstract fun addIntent(context: Context, requestCode: Int): PendingIntent

    /** Başlığa dokununca açılacak ekran. */
    protected abstract val screenIndex: Int

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    /** v7.42: Yeniden boyutlandırmada içeriği tazele. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, appWidgetManager, appWidgetId)
        try {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.glList)
        } catch (e: Exception) {
            android.util.Log.w("GlassWidget", "Liste tazelenemedi", e)
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_glass_list)
        views.setTextViewText(R.id.glTitle, title(context))
        views.setTextViewText(R.id.glEmpty, emptyText(context))

        // Liste bağlantısı — her widget/tür için ayrı Uri, yoksa veri karışır
        val service = Intent(context, GlassListService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(EXTRA_KIND, kind)
            data = Uri.parse("gunlukasistan://glass/$kind/$widgetId")
        }
        views.setRemoteAdapter(R.id.glList, service)
        views.setEmptyView(R.id.glList, R.id.glEmpty)

        // Satır tıklamaları için şablon
        val toggle = Intent(context, javaClass).apply {
            action = ACTION_TOGGLE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("gunlukasistan://glasstoggle/$kind/$widgetId")
        }
        views.setPendingIntentTemplate(
            R.id.glList,
            PendingIntent.getBroadcast(
                context, widgetId, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )

        views.setOnClickPendingIntent(
            R.id.glTitle,
            WidgetCommon.openScreen(context, screenIndex, 300 + kind.hashCode() % 50)
        )
        views.setOnClickPendingIntent(R.id.glAdd, addIntent(context, 350 + kind.hashCode() % 50))

        // v7.66: uygulama temasina gore boya
        try {
            val pal = WidgetTema.palet(context)
            WidgetTema.uygula(
                views, pal,
                metinler = intArrayOf(R.id.glTitle),
                soluklar = intArrayOf(R.id.glEmpty)
            )
        } catch (e: Exception) {
            android.util.Log.w("GlassWidget", "Tema uygulanamadi", e)
        }

        manager.updateAppWidget(widgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        val id = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
        if (id == 0L) return

        if (id < 0L) {
            // Negatif kimlik = alışkanlık (Bugün listesinde karışık gelir)
            toggleHabit(context, -id)
        } else if (kind == KIND_HABITS) {
            toggleHabit(context, id)
        } else {
            completeTask(context, id)
        }

        WidgetCommon.refreshAll(context)
        refresh(context, javaClass)
    }

    private fun completeTask(context: Context, taskId: Long) {
        val tasks = Store.loadTasks(context)
        val task = tasks.firstOrNull { it.id == taskId } ?: return
        if (task.done) return
        task.done = true
        Store.recordCompletion(context)
        try {
            AlarmScheduler.cancel(context, task.id)
        } catch (_: Exception) {
        }
        Store.saveTasks(context, tasks)
        toast(context, context.getString(R.string.gl_task_done, task.text))
    }

    private fun toggleHabit(context: Context, habitId: Long) {
        val habit = Store.loadHabits(context).firstOrNull { it.id == habitId } ?: return
        val next = Store.toggleHabit(context, habit)
        if (next >= habit.target) {
            val streak = Store.habitStreak(context, habit)
            toast(context, context.getString(R.string.gl_habit_done, habit.title, streak))
        }
    }

    private fun toast(context: Context, text: String) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }
}

/** Görevler — cam liste. */
class GlassTasksWidget : GlassWidgetBase() {
    override val kind = KIND_TASKS
    override val screenIndex = WidgetCommon.SCREEN_TASKS
    override fun title(context: Context): String {
        val n = Store.loadTasks(context).count { !it.done }
        return if (n > 0) context.getString(R.string.gl_tasks_n, n)
        else context.getString(R.string.gl_tasks)
    }
    override fun emptyText(context: Context): String = context.getString(R.string.gl_empty_tasks)
    override fun addIntent(context: Context, requestCode: Int): PendingIntent =
        WidgetCommon.quickAction(
            context, WidgetCommon.QUICK_TASK, WidgetCommon.SCREEN_TASKS, requestCode
        )
}

/** Alışkanlıklar — cam liste. */
class GlassHabitsWidget : GlassWidgetBase() {
    override val kind = KIND_HABITS
    override val screenIndex = WidgetCommon.SCREEN_HABITS
    override fun title(context: Context): String {
        val (done, total) = Store.habitProgressToday(context)
        return if (total > 0) context.getString(R.string.gl_habits_n, done, total)
        else context.getString(R.string.gl_habits)
    }
    override fun emptyText(context: Context): String = context.getString(R.string.gl_empty_habits)
    override fun addIntent(context: Context, requestCode: Int): PendingIntent =
        WidgetCommon.openScreen(context, WidgetCommon.SCREEN_HABITS, requestCode)
}

/** Bugün — alışkanlık + görev karışık. */
class GlassTodayWidget : GlassWidgetBase() {
    override val kind = KIND_TODAY
    override val screenIndex = WidgetCommon.SCREEN_TODAY
    override fun title(context: Context): String = context.getString(R.string.gl_today)
    override fun emptyText(context: Context): String = context.getString(R.string.gl_empty_today)
    override fun addIntent(context: Context, requestCode: Int): PendingIntent =
        WidgetCommon.quickAction(
            context, WidgetCommon.QUICK_TASK, WidgetCommon.SCREEN_TASKS, requestCode
        )
}
