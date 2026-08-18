package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

/**
 * Görev listesi widget'ı (4×3).
 * Kaydırılabilir liste; bir satıra dokunmak görevi tamamlar.
 */
class TasksWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.gunlukasistan.app.WIDGET_TASK_TOGGLE"
        const val EXTRA_TASK_ID = "w_task_id"

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tasks)

            val pending = Store.loadTasks(context).count { !it.done }
            // v7.42: dar widget'ta kısa başlık
            val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
            val kademe = WidgetCommon.boyutKademesi(genislik)
            views.setTextViewText(
                R.id.twTitle,
                if (kademe == 0) "📋 " + pending
                else context.getString(R.string.w_tasks_count, pending)
            )
            WidgetCommon.yaziBoyutu(views, R.id.twTitle, 12f, 13f, 14f, kademe)
            // v10.21: başlık çubuğu kullanıcı denetiminde (varsayılan açık)
            WidgetCommon.goster(views, R.id.twTitle, WidgetSecim.goster(context, WidgetSecim.W_TW_BASLIK))

            // Liste bağlantısı
            val serviceIntent = Intent(context, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse("gunlukasistan://tasks/$widgetId")
            }
            views.setRemoteAdapter(R.id.twList, serviceIntent)
            // Liste boşken otomatik olarak boş-durum metnini gösterir
            views.setEmptyView(R.id.twList, R.id.twEmpty)

            // Satır tıklamaları için şablon intent
            val toggleIntent = Intent(context, TasksWidget::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse("gunlukasistan://toggle/$widgetId")
            }
            val togglePending = PendingIntent.getBroadcast(
                context, widgetId, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.twList, togglePending)

            // Başlık → görevler ekranı, + → yeni görev
            views.setOnClickPendingIntent(
                R.id.twTitle,
                WidgetCommon.openScreen(
                    context,
                    // v10.21: gövde dokunma hedefi kullanıcı seçimli
                    WidgetDokunma.ekran(context, WidgetDokunma.TASKS, WidgetCommon.SCREEN_TASKS),
                    220
                )
            )
            views.setOnClickPendingIntent(
                R.id.twAdd,
                WidgetCommon.quickAction(
                    context, WidgetCommon.QUICK_TASK, WidgetCommon.SCREEN_TASKS, 221
                )
            )

            // v10.13 · B12: örnek bazlı etiket filtresi — çip, bu örneğin
            // seçili etiketini gösterir; dokunmak filtre ekranını açar.
            val filtreKod = WidgetFiltre.filtre(context, widgetId)
            views.setTextViewText(
                R.id.twFilter,
                Etiket.bul(filtreKod)?.emoji
                    ?: context.getString(R.string.wg_etiket_btn)
            )
            val filtreNiyet = Intent(context, WidgetFiltreActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse("gunlukasistan://filtre/$widgetId")
            }
            views.setOnClickPendingIntent(
                R.id.twFilter,
                PendingIntent.getActivity(
                    context, widgetId, filtreNiyet,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            // Filtreliyken boş-durum metni neden boş olduğunu söylesin
            if (filtreKod.isNotBlank()) {
                views.setTextViewText(
                    R.id.twEmpty,
                    context.getString(
                        R.string.wg_filtre_bos, WidgetFiltre.filtreAd(context, widgetId)
                    )
                )
            }

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.tasksBg, context, pal, R.id.tasksRoot)
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.twTitle),
                    soluklar = intArrayOf(R.id.twEmpty)
                )
                WidgetTema.vurguDugme(views, R.id.twAdd, R.id.twAdd, pal)
                // v10.13 · B12: filtre çipi de vurgu düğmesi
                WidgetTema.vurguDugme(views, R.id.twFilter, R.id.twFilter, pal)
            } catch (e: Exception) {
                android.util.Log.w("TasksWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    /**
     * v7.42: Kullanıcı widget'ı yeniden boyutlandırınca içeriği tazele.
     * Bu olmadan büyütme/küçültme sonrası düzen eski ölçüde kalıyordu.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
            if (taskId != 0L) {
                val tasks = Store.loadTasks(context)
                val task = tasks.firstOrNull { it.id == taskId }
                if (task != null && !task.done) {
                    task.done = true
                    Store.recordCompletion(context)
                    // v7.70: tekrarliysa sonraki tarihe tasi
                    if (task.tekrarliMi) Tekrar.gorevYenile(context, task)
                    try {
                        AlarmScheduler.cancel(context, task.id)
                    } catch (_: Exception) {
                    }
                    Store.saveTasks(context, tasks)
                    try {
                        Toast.makeText(
                            context,
                            context.getString(R.string.w_task_done, task.text),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {
                    }
                }
                WidgetCommon.refreshAll(context)
            }
        }
    }
}
