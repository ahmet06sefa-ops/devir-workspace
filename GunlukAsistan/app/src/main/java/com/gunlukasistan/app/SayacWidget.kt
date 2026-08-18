package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews

/**
 * v7.85 — Sayaç widget'ı (2×2) — "mini ekran eklentisi".
 *
 * ── Kullanıcının isteği ──
 * "Mini ekran olarak eklenti ekle" — geri sayımı ana ekrandan görmek
 * ve kontrol etmek.
 *
 * ── Neden `Chronometer` ──
 * Widget'ı saniye saniye güncellemek imkânsız: `updatePeriodMillis`
 * en hızlı 30 dakikada bir çalışır ve sık `updateAppWidget` çağrısı pili
 * bitirir. `Chronometer` RemoteViews'ta desteklenen bir görünüm ve
 * **sistem kendisi sayar** — biz sadece başlangıç noktasını veririz.
 * Bu, `TimerNotifier`'daki `setUsesChronometer` ile aynı mantık.
 *
 * Duraklatıldığında Chronometer gizlenip yerine sabit metin gösterilir;
 * durdurulmuş bir Chronometer yanlış değer gösterebiliyor.
 *
 * ── Senkronizasyon ──
 * Widget kendi durumunu tutmaz; her şeyi [TimerEngine]'den okur. Sayaç
 * uygulamadan, bildirimden ya da widget'tan kontrol edilsin — üçü de
 * aynı kaynağa yazdığı için görüntü tutarlı kalır.
 */
class SayacWidget : AppWidgetProvider() {

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

        private const val TAG = "SayacWidget"

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_sayac)

            try {
                doldur(context, views)
                runCatching {
                    val pal = WidgetTema.palet(context)
                    WidgetZemin.uygula(views, R.id.syBg, context, pal, R.id.syRoot)
                }

                // v10.20: sayaç yazıları serbest ölçek — bu widget boyutlarını
                // programatik hiç ayarlamıyordu (genel ölçek burada düşüyordu)
                val syO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_SY) * WidgetCommon.yaziOlcek
                WidgetCommon.olcekliYazi(views, context, R.id.syUst, R.dimen.ga_yazi_mini, syO)
                WidgetCommon.olcekliYazi(views, context, R.id.sySayac, R.dimen.ga_yazi_dev, syO)
                WidgetCommon.olcekliYazi(views, context, R.id.sySabit, R.dimen.ga_yazi_dev, syO)
                WidgetCommon.olcekliYazi(views, context, R.id.syBasla, R.dimen.ga_yazi_mini, syO)
                WidgetCommon.olcekliYazi(views, context, R.id.sySifirla, R.dimen.ga_yazi_mini, syO)
                listOf(R.id.syPreset1, R.id.syPreset2, R.id.syPreset3).forEach {
                    WidgetCommon.olcekliYazi(views, context, it, R.dimen.ga_yazi_mini, syO)
                }

                // Gövdeye dokunmak sayaç ekranını açar
                views.setOnClickPendingIntent(
                    R.id.syRoot,
                    WidgetCommon.openScreen(
                        context,
                        WidgetDokunma.ekran(context, WidgetDokunma.SY, WidgetCommon.SCREEN_TIMER),
                        8951
                    )
                )
                // Düğmeler doğrudan sayacı kontrol eder
                views.setOnClickPendingIntent(
                    R.id.syBasla,
                    eylem(context, TimerNotifier.ACTION_TOGGLE, 8952)
                )
                views.setOnClickPendingIntent(
                    R.id.sySifirla,
                    eylem(context, TimerNotifier.ACTION_RESET, 8953)
                )

                // v10.5 · C27: hazır ayar çipleri — tek dokunuşla
                // N dakikalık odak başlar. ACTION_TOGGLE'dan farkı:
                // süreyi de yazar, ekrana girmeye hiç gerek kalmaz.
                val presetIdler = intArrayOf(R.id.syPreset1, R.id.syPreset2, R.id.syPreset3)
                presetIdler.forEachIndexed { i, viewId ->
                    val dk = SayacPreset.dakika(i)
                    if (dk != null) {
                        views.setTextViewText(viewId, SayacPreset.etiket(dk))
                        views.setOnClickPendingIntent(
                            viewId,
                            android.app.PendingIntent.getBroadcast(
                                context, 8960 + i,
                                android.content.Intent(
                                    context, TimerActionReceiver::class.java
                                ).apply {
                                    action = TimerActionReceiver.ACTION_BASLAT_DK
                                    putExtra(TimerActionReceiver.EXTRA_DAKIKA, dk)
                                },
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                    android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Widget çizilemedi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun eylem(context: Context, act: String, kod: Int) =
            android.app.PendingIntent.getBroadcast(
                context, kod,
                android.content.Intent(context, TimerActionReceiver::class.java).apply {
                    action = act
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
            )

        private fun doldur(context: Context, views: RemoteViews) {
            val calisiyor = TimerEngine.isRunning(context)
            val geriSayim = TimerEngine.mode(context) == TimerEngine.MODE_DOWN
            val deger = TimerEngine.displayMs(context)
            val toplam = TimerEngine.totalMs(context).coerceAtLeast(1L)

            // ── Üst etiket ──
            views.setTextViewText(
                R.id.syUst,
                when {
                    !geriSayim -> context.getString(R.string.stopwatch)
                    calisiyor -> context.getString(
                        R.string.sy_dakika_kisa, (toplam / 60_000L).toInt()
                    )
                    deger >= toplam -> context.getString(R.string.sy_w_hazir)
                    else -> context.getString(R.string.sy_duraklatildi)
                }
            )

            // ── Sayaç / sabit metin ──
            if (calisiyor) {
                // Sistem kendi saysın: geri sayımda azalır, kronometrede artar
                val taban = if (geriSayim) {
                    SystemClock.elapsedRealtime() + deger
                } else {
                    SystemClock.elapsedRealtime() - deger
                }
                views.setChronometer(R.id.sySayac, taban, null, true)
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    views.setChronometerCountDown(R.id.sySayac, geriSayim)
                }
                WidgetCommon.goster(views, R.id.sySayac, true)
                WidgetCommon.goster(views, R.id.sySabit, false)
            } else {
                // Duraklatılmış Chronometer yanlış değer gösterebiliyor
                views.setChronometer(R.id.sySayac, SystemClock.elapsedRealtime(), null, false)
                WidgetCommon.goster(views, R.id.sySayac, false)
                WidgetCommon.goster(views, R.id.sySabit, true)
                views.setTextViewText(R.id.sySabit, TimerEngine.format(deger))
            }

            // ── İlerleme ──
            val yuzde = if (geriSayim) {
                (((toplam - deger).toFloat() / toplam) * 100).toInt().coerceIn(0, 100)
            } else {
                (((deger / 1000L) % 60L) * 100 / 60).toInt().coerceIn(0, 100)
            }
            views.setProgressBar(R.id.syBar, 100, yuzde, false)

            // ── Düğme etiketi ──
            views.setTextViewText(
                R.id.syBasla,
                context.getString(
                    if (calisiyor) R.string.sy_w_duraklat else R.string.sy_w_basla
                )
            )

            // ── v10.5 · C27: çipler yalnız boşta görünür ──
            // Çalışan ya da duraklatılmış oturumda çip göstermek
            // "yanlışlıkla üzerine yazma" riski yaratır; kronometre
            // modunda da süre kavramı olmadığı için gizli kalır.
            val bosta = geriSayim && !calisiyor && deger >= toplam
            // v10.17: çipler, sıfırla düğmesi ve ilerleme çubuğu kullanıcı denetiminde
            WidgetCommon.goster(views, R.id.syPresetler, bosta && WidgetSecim.goster(context, WidgetSecim.W_SY_PRESET))
            WidgetCommon.goster(views, R.id.sySifirla, WidgetSecim.goster(context, WidgetSecim.W_SY_SIFIRLA))
            WidgetCommon.goster(views, R.id.syBar, WidgetSecim.goster(context, WidgetSecim.W_SY_BAR))
        }
    }
}
