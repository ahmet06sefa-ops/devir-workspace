package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.13 · ULTRA-30 / B8 — Ay ızgarasının 42 hücresini üreten servis.
 *
 * Her `onDataSetChanged`'te seçili ayın hücreleri [TakvimMotoru]'ndan
 * alınır ve görev yoğunlukları (o güne tarihli bekleyen görevler)
 * hesaplanır. Hücre dokunması [TakvimWidget.ACTION_GUN]'a düşer.
 */
class TakvimWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        AyFabrikasi(
            applicationContext,
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
}

private class AyFabrikasi(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var hucreler: List<TakvimMotoru.Hucre> = emptyList()
    private var yogunlukHarita: Map<String, Int> = emptyMap()
    private val gunKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val simdi = Calendar.getInstance()
        val (yil, ay0) = TakvimMotoru.ayKaydir(
            simdi.get(Calendar.YEAR),
            simdi.get(Calendar.MONTH),
            TakvimWidget.ayOfset(context, widgetId)
        )
        hucreler = TakvimMotoru.hucreler(yil, ay0, System.currentTimeMillis())

        // Yoğunluk: bekleyen görevler gün anahtarına göre sayılır
        val sayac = HashMap<String, Int>()
        Store.loadTasks(context).forEach { gorev ->
            if (gorev.dueAt > 0L) {
                val key = gunKeyFormat.format(Date(gorev.dueAt))
                sayac[key] = (sayac[key] ?: 0) + 1
            }
        }
        yogunlukHarita = sayac
    }

    override fun onDestroy() {
        hucreler = emptyList()
        yogunlukHarita = emptyMap()
    }

    override fun getCount(): Int = hucreler.size

    override fun getViewAt(position: Int): RemoteViews {
        val hucre = RemoteViews(context.packageName, R.layout.widget_ay_cell)
        val h = hucreler.getOrNull(position) ?: return hucre

        hucre.setTextViewText(R.id.cellGun, h.gun.toString())

        val pal = WidgetTema.palet(context)
        val gunRengi = when {
            h.bugunMu -> pal.vurgu
            h.ayDisi -> (pal.metinSoluk and 0x00FFFFFF) or 0x66000000
            else -> pal.metin
        }
        WidgetTema.metin(hucre, R.id.cellGun, gunRengi)

        // Yoğunluk noktası: renk seviyeyi anlatır (● görünürlüğü + rengi)
        val cal = Calendar.getInstance().apply {
            clear()
            set(h.yil, h.ay0, h.gun)
        }
        val adet = yogunlukHarita[gunKeyFormat.format(Date(cal.timeInMillis))] ?: 0
        val seviye = TakvimMotoru.yogunluk(adet)
        if (seviye > 0) {
            hucre.setViewVisibility(R.id.cellNokta, View.VISIBLE)
            hucre.setTextViewText(R.id.cellNokta, noktaMetni(seviye))
            WidgetTema.metin(hucre, R.id.cellNokta, if (seviye >= 3) pal.yesil else pal.vurgu)
        } else {
            hucre.setViewVisibility(R.id.cellNokta, View.INVISIBLE)
        }

        // Bugün hücresinin zemini vurgu-soluk
        if (h.bugunMu) {
            hucre.setInt(R.id.cellRoot, "setBackgroundColor", pal.vurguSoluk)
        } else {
            hucre.setInt(R.id.cellRoot, "setBackgroundColor", 0x00000000)
        }

        hucre.setOnClickFillInIntent(
            R.id.cellRoot,
            Intent().apply { putExtra(TakvimWidget.EXTRA_GUN_MSB, cal.timeInMillis) }
        )
        return hucre
    }

    /** Seviye → görünür nokta sayısı (● karakterleri). */
    private fun noktaMetni(seviye: Int): String = when (seviye) {
        1 -> "●"
        2 -> "● ●"
        else -> "● ● ●"
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
