package com.gunlukasistan.app

import android.app.PendingIntent
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Hızlı eylem şeridi widget'ı (4×1).
 * Dört kısayol: odaklan, soru ekle, görev ekle, bugün.
 */
class ActionsWidget : AppWidgetProvider() {

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

    companion object {
        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_actions)

            // v7.42: 2x1'e kadar küçülebiliyor — sığmayan düğmeleri gizle
            val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
            // v10.17: beş düğmenin tamamı kullanıcı denetiminde (varsayılan açık)
            WidgetCommon.goster(views, R.id.actTask, genislik >= 170 && WidgetSecim.goster(context, WidgetSecim.W_ACT_GOREV))
            WidgetCommon.goster(views, R.id.actToday, genislik >= 220 && WidgetSecim.goster(context, WidgetSecim.W_ACT_BUGUN))
            WidgetCommon.goster(views, R.id.actTimer, WidgetSecim.goster(context, WidgetSecim.W_ACT_ODAK))
            WidgetCommon.goster(views, R.id.actQuestion, WidgetSecim.goster(context, WidgetSecim.W_ACT_SORU))
            WidgetCommon.goster(views, R.id.actSes, WidgetSecim.goster(context, WidgetSecim.W_ACT_SES))

            // v7.45: ODAK artık doğrudan başlıyor — uygulama açılmıyor
            val calisiyor = TimerEngine.isRunning(context)
            views.setOnClickPendingIntent(
                R.id.actTimer,
                if (calisiyor) WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_DUR, 2301)
                else WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_25, 2300)
            )
            // v7.45: SORU sayacı anında artıyor
            views.setOnClickPendingIntent(
                R.id.actQuestion,
                WidgetEylem.niyet(context, WidgetEylem.IS_SORU_ARTIR, 2302)
            )
            views.setOnClickPendingIntent(
                R.id.actTask,
                WidgetCommon.quickAction(
                    context, WidgetCommon.QUICK_TASK, WidgetCommon.SCREEN_TASKS, 232
                )
            )
            views.setOnClickPendingIntent(
                R.id.actToday,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TODAY, 233)
            )

            // v7.71: bas-konus hizli not
            views.setOnClickPendingIntent(
                R.id.actSes,
                PendingIntent.getActivity(
                    context, 8901,
                    Intent(context, SesliNotActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                        data = android.net.Uri.parse("gunlukasistan://seslinot")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.actBg, context, pal, R.id.actRoot)
                listOf(R.id.actTimer, R.id.actQuestion, R.id.actTask, R.id.actToday, R.id.actSes)
                    .forEach { WidgetTema.notrDugme(views, it, pal) }
                // v10.20: düğme etiketleri serbest ölçek (etiketlere v10.20'de
                // id verildi; emoji simgeler Launcher ölçüsünde kalır)
                val actO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_ACT) * WidgetCommon.yaziOlcek
                listOf(
                    R.id.actTimYazi, R.id.actSoruYazi, R.id.actGorevYazi,
                    R.id.actBugunYazi, R.id.actSesYazi
                ).forEach {
                    WidgetCommon.olcekliYazi(views, context, it, R.dimen.ga_yazi_mini, actO)
                }
            } catch (e: Exception) {
                android.util.Log.w("ActionsWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
