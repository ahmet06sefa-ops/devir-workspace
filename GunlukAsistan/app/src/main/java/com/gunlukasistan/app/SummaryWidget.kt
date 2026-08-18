package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Günlük özet widget'ı (4×2).
 * Selamlama, geri sayım rozeti, hedef ilerlemesi ve üç istatistik kutusu.
 */
class SummaryWidget : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_summary)

            // v7.42: gerçek ölçüye göre uyarlanabilir düzen
            val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
            val yukseklik = WidgetCommon.yukseklikDp(manager, widgetId, 110)
            val kademe = WidgetCommon.boyutKademesi(genislik)

            views.setTextViewText(R.id.sumGreeting, WidgetCommon.greeting(context))
            views.setTextViewText(R.id.sumCountdown, WidgetCommon.countdownShort(context))
            // v10.17: selamlama ve geri sayım rozeti kullanıcı denetiminde
            WidgetCommon.goster(views, R.id.sumGreeting, WidgetSecim.goster(context, WidgetSecim.W_SUM_SELAM))
            // Dar widget'ta geri sayım rozeti sığmıyor
            WidgetCommon.goster(views, R.id.sumCountdown, kademe >= 1 && WidgetSecim.goster(context, WidgetSecim.W_SUM_GERI))
            // v10.20: örnek-başına yazı ölçeği
            val sumO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_SUM)
            WidgetCommon.yaziBoyutu(views, R.id.sumGreeting, 12f * sumO, 13f * sumO, 14f * sumO, kademe)

            val focus = Store.getTodayFocusMinutes(context)
            val goal = Store.getGoalMinutes(context)
            val questions = Store.getTodayQuestions(context)
            val (streak, _) = Store.streakInfo(context)
            val percent = if (goal > 0) (focus * 100 / goal).coerceIn(0, 100) else 0

            views.setProgressBar(R.id.sumProgress, 100, percent, false)
            views.setTextViewText(
                R.id.sumGoalText,
                if (kademe == 0) focus.toString() + "/" + goal + " dk"
                else context.getString(R.string.w_goal_line, focus, goal, percent)
            )
            // Alçak widget'ta istatistik kutuları gizlenir, çubuk kalır
            // v10.17: istatistik kutuları ve seri sayacı kullanıcı denetiminde
            val kutular = WidgetSecim.goster(context, WidgetSecim.W_SUM_KUTU)
            WidgetCommon.goster(views, R.id.sumBoxFocus, yukseklik >= 100 && kutular)
            WidgetCommon.goster(views, R.id.sumBoxQuestions, yukseklik >= 100 && kutular)
            views.setTextViewText(R.id.sumFocus, focus.toString())
            views.setTextViewText(R.id.sumQuestions, questions.toString())
            views.setTextViewText(R.id.sumStreak, streak.toString())
            WidgetCommon.goster(views, R.id.sumStreak, WidgetSecim.goster(context, WidgetSecim.W_SUM_SERI))

            // Dokunma hedefleri
            views.setOnClickPendingIntent(
                R.id.sumRoot,
                WidgetCommon.openScreen(
                    context,
                    // v10.21: gövde dokunma hedefi kullanıcı seçimli
                    WidgetDokunma.ekran(context, WidgetDokunma.SUM, WidgetCommon.SCREEN_TODAY),
                    210
                )
            )
            // v7.45: odak kutusuna dokun → 25 dk oturum başlar (çalışıyorsa durur)
            views.setOnClickPendingIntent(
                R.id.sumBoxFocus,
                if (TimerEngine.isRunning(context))
                    WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_DUR, 2111)
                else WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_25, 2110)
            )
            // v7.45: soru kutusuna dokun → sayaç +1
            views.setOnClickPendingIntent(
                R.id.sumBoxQuestions,
                WidgetEylem.niyet(context, WidgetEylem.IS_SORU_ARTIR, 2120)
            )
            // v7.45: seri kutusuna dokun → bugünü işaretle
            views.setOnClickPendingIntent(
                R.id.sumStreak,
                WidgetEylem.niyet(context, WidgetEylem.IS_DERS_ISARETLE, 2130)
            )
            views.setOnClickPendingIntent(
                R.id.sumCountdown,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_EVENTS, 213)
            )

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.sumBg, context, pal, R.id.sumRoot)
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.sumGreeting, R.id.sumFocus, R.id.sumQuestions),
                    soluklar = intArrayOf(R.id.sumGoalText),
                    vurgular = intArrayOf(R.id.sumCountdown),
                    cipler = intArrayOf(R.id.sumCountdown, R.id.sumBoxFocus, R.id.sumBoxQuestions),
                    yesiller = intArrayOf(R.id.sumStreak)
                )
            } catch (e: Exception) {
                android.util.Log.w("SummaryWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
