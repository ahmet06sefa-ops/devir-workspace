package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * v10.5 · Öneri C32 — Hedef halkası widget'ı (1×1 / 2×1).
 *
 * ── Ne gösterir ──
 * Günlük odak hedefinin yüzdesi (kaynaktan: `GunOdak.gunYuzdesi`),
 * ince bar ve alt satırda "X/Y dk · 🔥 seri". Telefonu açar açmaz
 * "bugün neredeyim" sorusunun cevabı.
 *
 * ── Neden gerçek halka yok ──
 * RemoteViews özel View kabul etmez; belirlenebilen halka
 * ProgressBar'ı platformun desteklemediği bir biçim ister. Kağıt
 * üzerinde "halka", uygulamada büyük yüzde + bar: aynı bilgi,
 * sıfır çizim riski. (v7.40.1'de CardView ile widget kırılmıştı —
 * aynı tuzağa düşmeme.)
 *
 * Tazelenme: yarım saatlik sistem periyodu + `WidgetCommon.refreshAll`
 * (odak kaydı düşünce sayaç tarafından tetiklenir).
 */
class HedefWidget : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_hedef)
            try {
                val hedef = Store.getGoalMinutes(context)
                val bugun = Store.getTodayFocusMinutes(context)
                val yuzde = GunOdak.gunYuzdesi(context)
                val (seri, _) = Store.streakInfo(context)

                // Küçük boyutta alt satır gizlenir; yüzde büyür (C28 deseni)
                val genislik = WidgetCommon.genislikDp(manager, widgetId, 80)
                val kucuk = genislik < 100
                // v10.17: alt bilgi satırı kullanıcı denetiminde
                WidgetCommon.goster(views, R.id.hdAlt, !kucuk && hedef > 0 && WidgetSecim.goster(context, WidgetSecim.W_HD_ALT))
                // v10.20: örnek-başına yazı ölçeği
                val hdO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_HD)
                WidgetCommon.yaziBoyutu(
                    views, R.id.hdYuzde, 30f * hdO, 34f * hdO, 38f * hdO,
                    WidgetCommon.boyutKademesi(genislik)
                )

                if (hedef > 0) {
                    // v10.17: büyük rakam yüzde ya da kalan dakika olabilir
                    views.setTextViewText(
                        R.id.hdYuzde,
                        if (WidgetSecim.hedefMod(context) == 1) {
                            context.getString(R.string.w17_kalan_dk, (hedef - bugun).coerceAtLeast(0))
                        } else "%$yuzde"
                    )
                    views.setProgressBar(R.id.hdBar, 100, yuzde, false)
                    views.setTextViewText(
                        R.id.hdAlt,
                        context.getString(R.string.hd_alt, bugun, hedef, seri)
                    )
                } else {
                    // Hedef yoksa yüzde anlamsız — seri göster, kuruluma çağır
                    views.setTextViewText(R.id.hdYuzde, "🎯")
                    views.setProgressBar(R.id.hdBar, 100, 0, false)
                    views.setTextViewText(R.id.hdAlt, context.getString(R.string.hd_hedef_yok))
                    WidgetCommon.goster(views, R.id.hdAlt, true)
                }

                views.setOnClickPendingIntent(
                    R.id.hdRoot,
                    WidgetCommon.openScreen(
                    context,
                    WidgetDokunma.ekran(context, WidgetDokunma.HD, WidgetCommon.SCREEN_TODAY),
                    2200
                )
                )

                runCatching {
                    val pal = WidgetTema.palet(context)
                    WidgetZemin.uygula(views, R.id.hdBg, context, pal, R.id.hdRoot)
                    views.setTextColor(R.id.hdYuzde, pal.vurgu)
                }
            } catch (e: Exception) {
                android.util.Log.w("HedefWidget", "Widget çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
