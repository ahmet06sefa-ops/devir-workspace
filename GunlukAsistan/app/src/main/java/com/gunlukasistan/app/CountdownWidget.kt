package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Kompakt geri sayım widget'ı (2×1).
 * Sabitlenmiş ya da en yakın etkinliğe kalan günü büyük rakamla gösterir.
 */
class CountdownWidget : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            // v7.42: 1x1'e kadar küçülebiliyor — etiketi gizle, rakamı ölçekle
            val genislik = WidgetCommon.genislikDp(manager, widgetId, 110)
            val kucuk = genislik < 100
            // v10.17: etiket ve emoji kullanıcı denetiminde (varsayılan açık)
            WidgetCommon.goster(views, R.id.cdLabel, !kucuk && WidgetSecim.goster(context, WidgetSecim.W_CD_ETIKET))
            WidgetCommon.goster(views, R.id.cdEmoji, genislik >= 80 && WidgetSecim.goster(context, WidgetSecim.W_CD_EMOJI))
            // v10.20: örnek-başına yazı ölçeği (genel yüzdenin üstüne çarpılır)
            val cdO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_CD)
            WidgetCommon.yaziBoyutu(views, R.id.cdDays, 26f * cdO, 32f * cdO, 38f * cdO,
                WidgetCommon.boyutKademesi(genislik))

            val event = Store.highlightEvent(context)
            if (event != null) {
                val left = event.daysLeft
                views.setTextViewText(R.id.cdEmoji, event.emoji)
                views.setTextViewText(
                    R.id.cdDays,
                    when {
                        left > 0 -> left.toString()
                        left == 0 -> "🎉"
                        else -> "✓"
                    }
                )
                views.setTextViewText(
                    R.id.cdLabel,
                    when {
                        left > 0 -> "${event.title} · gün"
                        left == 0 -> "${event.title} · bugün!"
                        else -> "${event.title} · geçti"
                    }
                )
            } else {
                val examMillis = Store.getExamDateMillis(context)
                if (examMillis > 0) {
                    val days = ((examMillis - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
                    views.setTextViewText(R.id.cdEmoji, "⏳")
                    views.setTextViewText(R.id.cdDays, if (days > 0) days.toString() else "🍀")
                    views.setTextViewText(
                        R.id.cdLabel,
                        if (days > 0) context.getString(R.string.w_exam_left)
                        else context.getString(R.string.w_exam_now)
                    )
                } else {
                    views.setTextViewText(R.id.cdEmoji, "📅")
                    views.setTextViewText(R.id.cdDays, "—")
                    views.setTextViewText(R.id.cdLabel, context.getString(R.string.w_no_event))
                }
            }

            views.setOnClickPendingIntent(
                R.id.cdRoot,
                WidgetCommon.openScreen(
                    context,
                    WidgetDokunma.ekran(context, WidgetDokunma.CD, WidgetCommon.SCREEN_EVENTS),
                    200
                )
            )
            // v7.45: emojiye dokun → bugünü kurs serisinde işaretle
            views.setOnClickPendingIntent(
                R.id.cdEmoji,
                WidgetEylem.niyet(context, WidgetEylem.IS_DERS_ISARETLE, 2010)
            )
            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.cdBg, context, pal, R.id.cdRoot)
                WidgetTema.uygula(
                    views, pal,
                    soluklar = intArrayOf(R.id.cdLabel),
                    vurgular = intArrayOf(R.id.cdDays)
                )
            } catch (e: Exception) {
                android.util.Log.w("CountdownWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
