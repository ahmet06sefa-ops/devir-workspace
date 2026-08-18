package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

/**
 * v10.13 · ULTRA-30 / B8 — Ay görünümü takvim widget'ı (4×4).
 *
 * ── Tarama kanıtı ──
 * PlanWidget liste, CountdownWidget tek olay — ay ızgarası yoktu.
 *
 * ── Yapı ──
 * 42 hücrelik `GridView` (hafta Pazartesi başlar, TR geleneği); her
 * gün görev yoğunluğuna göre 0-3 nokta taşır. Başlıkta ay/yıl, sağ ve
 * sol okla ±12 ay arasında gezinilir; güne dokunmak etkinlik ekranını
 * açar (güne özel ekran olmadığından en yakın tarih merkezli görünüm;
 * sınır notlarda yazılı).
 *
 * Veri, [TakvimWidgetService] fabrikasında üretilir; hesap kütüğü
 * saf [TakvimMotoru]'dur ve birim testlidir.
 */
class TakvimWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ONCEKI_AY = "com.gunlukasistan.app.WG_AY_ONCEKI"
        const val ACTION_SONRAKI_AY = "com.gunlukasistan.app.WG_AY_SONRAKI"
        const val ACTION_GUN = "com.gunlukasistan.app.WG_AY_GUN"
        const val EXTRA_GUN_MSB = "w_ay_gun_ms"

        private const val PREF = "wg_ay_ofset_v1"

        private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        fun ayOfset(c: Context, widgetId: Int): Int =
            TakvimMotoru.ofsetKelepce(prefs(c).getInt("o_$widgetId", 0))

        fun ayOfsetAyarla(c: Context, widgetId: Int, ofset: Int) {
            prefs(c).edit().putInt("o_$widgetId", TakvimMotoru.ofsetKelepce(ofset)).apply()
        }

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_ay)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.ayBg, context, pal)

                // Başlık: seçili ay (ofset dahil)
                val simdi = Calendar.getInstance()
                val (yil, ay0) = TakvimMotoru.ayKaydir(
                    simdi.get(Calendar.YEAR), simdi.get(Calendar.MONTH), ayOfset(context, widgetId)
                )
                views.setTextViewText(
                    R.id.ayBaslik,
                    ayAdi(context, yil, ay0)
                )

                // Hafta başı etiketleri (Pzt → Paz)
                val basliklar = intArrayOf(
                    R.id.ayH1, R.id.ayH2, R.id.ayH3, R.id.ayH4,
                    R.id.ayH5, R.id.ayH6, R.id.ayH7
                )
                basliklar.forEachIndexed { i, id ->
                    views.setTextViewText(id, HaftaPlan.gunAdi(context, HaftaPlan.gunSirasi[i]).take(1))
                }

                // Izgara bağlantısı
                val serviceIntent = Intent(context, TakvimWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("gunlukasistan://ay/$widgetId")
                }
                views.setRemoteAdapter(R.id.ayGrid, serviceIntent)

                // Hücre dokunma şablonu
                val gunNiyet = Intent(context, TakvimWidget::class.java).apply {
                    action = ACTION_GUN
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("gunlukasistan://aygun/$widgetId")
                }
                val gunPi = PendingIntent.getBroadcast(
                    context, 4980 + (widgetId % 10), gunNiyet,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.ayGrid, gunPi)

                // Ay gezinme
                views.setOnClickPendingIntent(
                    R.id.ayOnceki,
                    kendine(context, ACTION_ONCEKI_AY, widgetId, 4990)
                )
                views.setOnClickPendingIntent(
                    R.id.aySonraki,
                    kendine(context, ACTION_SONRAKI_AY, widgetId, 4991)
                )

                // Boyama
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(
                        R.id.ayBaslik, R.id.ayOnceki, R.id.aySonraki,
                        R.id.ayH1, R.id.ayH2, R.id.ayH3, R.id.ayH4,
                        R.id.ayH5, R.id.ayH6, R.id.ayH7
                    )
                )
                WidgetTema.metin(views, R.id.ayBaslik, pal.metin)
                basliklar.forEach { WidgetTema.metin(views, it, pal.metinSoluk) }
            } catch (e: Exception) {
                android.util.Log.w("TakvimWidget", "Çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun kendine(context: Context, action: String, widgetId: Int, kod: Int) =
            PendingIntent.getBroadcast(
                context, kod,
                Intent(context, TakvimWidget::class.java).apply {
                    this.action = action
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("gunlukasistan://aynav/$action/$widgetId")
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun ayAdi(context: Context, yil: Int, ay0: Int): String {
            val adlar = context.resources.getStringArray(R.array.w_ay_adlari)
            return adlar[ay0.coerceIn(0, 11)] + " " + yil
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context) ?: return
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        when (intent.action) {
            ACTION_ONCEKI_AY, ACTION_SONRAKI_AY -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val yon = if (intent.action == ACTION_ONCEKI_AY) -1 else 1
                ayOfsetAyarla(context, widgetId, ayOfset(context, widgetId) + yon)
                render(context, manager, widgetId)
                // Izgara verisi de yeni aya göre kurulsun
                manager.notifyAppWidgetViewDataChanged(widgetId, R.id.ayGrid)
            }
            ACTION_GUN -> {
                // Gün hücresi: etkinlik ekranına (tarih merkezli liste) git
                val acilis = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_EVENTS)
                    val ms = intent.getLongExtra(EXTRA_GUN_MSB, 0L)
                    if (ms > 0L) putExtra(EXTRA_GUN_MSB, ms)
                }
                context.startActivity(acilis)
            }
        }
    }
}
