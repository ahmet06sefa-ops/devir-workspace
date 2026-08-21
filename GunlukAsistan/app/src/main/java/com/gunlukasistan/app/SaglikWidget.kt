package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * v11.64 — Sağlık Karnesi widget'ı (4×2).
 *
 * Beslenme (kalori), su, uyku ortalaması ve ruh hali ortalamasını tek
 * kartta gösterir. Dokunmak Sağlık Karnesi ekranını açar.
 */
class SaglikWidget : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_saglik)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.sgBg, context, pal)

                WidgetCommon.olcekliYazi(views, context, R.id.sgTitle, R.dimen.ga_yazi_normal, 1f)
                WidgetCommon.olcekliYazi(views, context, R.id.sgBeslenme, R.dimen.ga_yazi_kucuk, 1f)
                WidgetCommon.olcekliYazi(views, context, R.id.sgSu, R.dimen.ga_yazi_kucuk, 1f)
                WidgetCommon.olcekliYazi(views, context, R.id.sgUyku, R.dimen.ga_yazi_kucuk, 1f)
                WidgetCommon.olcekliYazi(views, context, R.id.sgMood, R.dimen.ga_yazi_kucuk, 1f)

                // Beslenme
                val kalori = BeslenmeMotor.bugunKalori(context)
                val hedef = BeslenmeMotor.kaloriHedefi(context)
                views.setTextViewText(
                    R.id.sgBeslenme,
                    "🍽️ $kalori / $hedef kcal" +
                        (if (kalori > hedef) " (aştı)" else "")
                )
                // Su
                val su = BeslenmeMotor.suBardak(context)
                views.setTextViewText(R.id.sgSu, "💧 $su bardak (${su * 250} ml)")

                // Uyku ortalaması
                val uykuSayisi = UykuMotor.son7GunKayitSayisi(context)
                if (uykuSayisi == 0) {
                    views.setTextViewText(R.id.sgUyku, "😴 Uyku kaydı yok")
                } else {
                    val ort = UykuMotor.son7GunOrtalamaDakika(context).toInt()
                    views.setTextViewText(R.id.sgUyku, "😴 Ort ${UykuMotor.sureMetni(ort)}")
                }

                // Ruh hali ortalaması
                val moodSayisi = MoodMotor.son7GunKayitSayisi(context)
                if (moodSayisi == 0) {
                    views.setTextViewText(R.id.sgMood, "🎭 Ruh hali kaydı yok")
                } else {
                    val ortMood = MoodMotor.son7GunOrtalama(context)
                    views.setTextViewText(
                        R.id.sgMood,
                        "🎭 ${MoodMotor.emoji(ortMood.toInt())} ${"%.1f".format(ortMood)}/5"
                    )
                }

                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.sgTitle, R.id.sgBeslenme),
                    soluklar = intArrayOf(R.id.sgSu, R.id.sgUyku, R.id.sgMood)
                )

                // Gövde → Sağlık Karnesi
                val niyet = android.content.Intent(context, SaglikOzetActivity::class.java)
                views.setOnClickPendingIntent(
                    R.id.sgRoot,
                    android.app.PendingIntent.getActivity(
                        context, 4985, niyet,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("SaglikWidget", "Çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
