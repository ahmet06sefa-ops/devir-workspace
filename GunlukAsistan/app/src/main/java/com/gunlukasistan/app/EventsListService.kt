package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

/**
 * v10.5 · Öneri C31 — Çoklu geri sayım widget'ının satır fabrikası.
 *
 * Hangi etkinliklerin görüneceği `EventsListVeri.sec` kararını verir;
 * burada yalnız biçim vardır: emoji · ad · "N gün / bugün / geçti".
 */
class EventsListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        EventsFactory(applicationContext)
}

private class EventsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var items: List<Store.DayEvent> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = try {
            // v10.21: satır filtreleri kullanıcıda — geçmişi göster,
            // yalnız sabitlenenler, satır sayısı (varsayılan = eski davranış)
            EventsListVeri.sec(
                Store.loadEvents(context),
                gecmisiDahil = WidgetListe.gosterBool(context, WidgetListe.K_EV_GECMIS, true),
                yalnizSabit = WidgetListe.gosterBool(context, WidgetListe.K_EV_SABIT, false),
                limit = WidgetListe.satir(context, WidgetListe.K_EV_SATIR, EventsListVeri.AZAMI_SATIR)
            )
        } catch (e: Exception) {
            android.util.Log.w("EventsListService", "Etkinlikler okunamadı", e)
            emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_event_row)
        val e = items.getOrNull(position) ?: return row

        row.setTextViewText(R.id.evRowEmoji, e.emoji)
        row.setTextViewText(R.id.evRowAd, e.title)
        row.setTextViewText(
            R.id.evRowGun,
            when {
                e.daysLeft > 0 -> context.getString(R.string.ev_gun_kaldi, e.daysLeft)
                e.daysLeft == 0 -> context.getString(R.string.ev_bugun)
                else -> context.getString(R.string.ev_gecti)
            }
        )

        // Tema
        runCatching {
            val pal = WidgetTema.palet(context)
            row.setTextColor(R.id.evRowAd, pal.metin)
            row.setTextColor(R.id.evRowGun, pal.vurgu)
        }

        // Dokunma: şablon Etkinlikler ekranını açıyor; fillIn boş bırakılır
        row.setOnClickFillInIntent(R.id.evRowRoot, Intent())
        return row
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
