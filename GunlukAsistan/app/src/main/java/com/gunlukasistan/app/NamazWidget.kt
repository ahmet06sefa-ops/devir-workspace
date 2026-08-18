package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/**
 * v7.47 — Minimalist namaz widget'ı (2×1).
 *
 * ── Kullanıcı isteği ──
 * "minimalist olarak widget ekle"
 *
 * ── Tasarım ilkesi: az ama net ──
 * Yalnızca üç bilgi:
 *   🕌  Sıradaki vakit adı
 *       19:58          ← en belirgin
 *       2 sa 14 dk kaldı
 *
 * İkon, düğme, çerçeve süsü yok. 1×1'e kadar küçülünce yalnızca saat kalır.
 *
 * ── Neden ayrı widget? ──
 * Brifing widget'ı zaten yoğun. Namaz kullanan kişi ana ekranında sürekli
 * vakti görmek ister; bunun için sade ve küçük bir bileşen gerekiyordu.
 */
class NamazWidget : AppWidgetProvider() {

    companion object {
        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_namaz)

            // Modül kapalıysa kullanıcıyı yönlendir
            if (!NamazVakti.acikMi(context)) {
                views.setTextViewText(R.id.nwEmoji, "🕌")
                views.setTextViewText(R.id.nwName, context.getString(R.string.nw_kapali))
                views.setTextViewText(R.id.nwTime, "—")
                views.setViewVisibility(R.id.nwLeft, View.GONE)
                views.setOnClickPendingIntent(R.id.nwRoot, ekraniAc(context))
                manager.updateAppWidget(widgetId, views)
                return
            }

            try {
                val gun = NamazVakti.bugunDuzeltilmis(context)
                val simdi = NamazVakti.simdiDakika()
                val (vakit, kalan) = gun.sonraki(simdi)

                // Genişliğe göre sadeleştir — 1x1'de yalnızca saat
                val genislik = WidgetCommon.genislikDp(manager, widgetId, 110)
                val dar = genislik < 100

                views.setTextViewText(R.id.nwEmoji, vakit.emoji)
                views.setViewVisibility(R.id.nwEmoji, if (dar) View.GONE else View.VISIBLE)

                views.setTextViewText(R.id.nwName, context.getString(vakit.adRes))
                // v10.17: vakit adı kullanıcı denetiminde (varsayılan açık)
                views.setViewVisibility(R.id.nwName, if (dar || !WidgetSecim.goster(context, WidgetSecim.W_NW_AD)) View.GONE else View.VISIBLE)

                views.setTextViewText(R.id.nwTime, gun.saat(vakit))
                // v10.20: örnek-başına yazı ölçeği
                val nwO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_NW)
                WidgetCommon.yaziBoyutu(
                    views, R.id.nwTime, 18f * nwO, 21f * nwO, 24f * nwO,
                    WidgetCommon.boyutKademesi(genislik)
                )

                views.setTextViewText(
                    R.id.nwLeft,
                    context.getString(R.string.nw_kalan, NamazPlan.sureMetni(kalan))
                )
                // v10.17: kalan süre kullanıcı denetiminde (varsayılan açık)
                views.setViewVisibility(R.id.nwLeft, if (dar || !WidgetSecim.goster(context, WidgetSecim.W_NW_KALAN)) View.GONE else View.VISIBLE)

                views.setOnClickPendingIntent(R.id.nwRoot, ekraniAc(context))
            } catch (e: Exception) {
                android.util.Log.w("NamazWidget", "Çizilemedi", e)
                views.setTextViewText(R.id.nwTime, "--:--")
            }

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.nwBg, context, pal, R.id.nwRoot)
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.nwTime),
                    soluklar = intArrayOf(R.id.nwName),
                    vurgular = intArrayOf(R.id.nwLeft)
                )
            } catch (e: Exception) {
                android.util.Log.w("NamazWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun ekraniAc(context: Context): PendingIntent {
            val intent = Intent(context, NamazActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = android.net.Uri.parse("gunlukasistan://namazwidget")
            }
            return PendingIntent.getActivity(
                context, 8700, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Bu türün tüm örneklerini tazeler. */
        fun hepsiniTazele(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                manager.getAppWidgetIds(ComponentName(context, NamazWidget::class.java))
                    .forEach { render(context, manager, it) }
            } catch (e: Exception) {
                android.util.Log.w("NamazWidget", "Tazelenemedi", e)
            }
        }
    }

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
}
