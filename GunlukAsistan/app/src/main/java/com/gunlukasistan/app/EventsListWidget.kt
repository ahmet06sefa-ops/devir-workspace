package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * v10.5 · Öneri C31 — Çoklu geri sayım widget'ı (2×2, kaydırılabilir).
 *
 * `CountdownWidget` tek etkinlik gösterir; bu widget sınav, doğum
 * günü, fatura… hepsini tek yerden verir. Satırlar kaydırılabilir
 * `ListView`'den gelir (`EventsListService`); seçim mantığı
 * `EventsListVeri`'de birim testlidir.
 *
 * Satıra dokunmak Etkinlikler ekranını açar.
 */
class EventsListWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

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
            val views = RemoteViews(context.packageName, R.layout.widget_events_list)
            try {
                views.setRemoteAdapter(
                    R.id.evList,
                    Intent(context, EventsListService::class.java)
                )
                views.setEmptyView(R.id.evList, R.id.evEmpty)

                // Satır dokunuşları bu şablonun fillIn'iyle Etkinlikler'e gider
                views.setPendingIntentTemplate(
                    R.id.evList,
                    WidgetCommon.openScreen(context, WidgetCommon.SCREEN_EVENTS, 2100)
                )
                // Başlığa dokunmak — v10.21: hedef kullanıcı seçimli
                views.setOnClickPendingIntent(
                    R.id.evBaslik,
                    WidgetCommon.openScreen(
                        context,
                        WidgetDokunma.ekran(context, WidgetDokunma.EV, WidgetCommon.SCREEN_EVENTS),
                        2101
                    )
                )

                // Tema
                runCatching {
                    val pal = WidgetTema.palet(context)
                    WidgetZemin.uygula(views, R.id.evBg, context, pal, R.id.evRoot)
                    views.setTextColor(R.id.evBaslik, pal.metinSoluk)
                    // v10.21: başlık çubuğu kullanıcı denetiminde (varsayılan açık)
                    WidgetCommon.goster(
                        views, R.id.evBaslik,
                        WidgetSecim.goster(context, WidgetSecim.W_EV_BASLIK)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("EventsListWidget", "Widget çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
