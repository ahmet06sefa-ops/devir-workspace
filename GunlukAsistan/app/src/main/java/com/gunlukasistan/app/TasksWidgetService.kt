package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Görev listesi widget'ının satırlarını üreten servis.
 * Kaydırılabilir ListView için her satırı ayrı RemoteViews olarak döndürür.
 */
class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        // v10.13 · B12: hangi ÖRNEĞİN satırları üretildiği bilinmeli —
        // filtre örnek bazlı saklanıyor.
        TasksFactory(
            applicationContext,
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
}

private class TasksFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Store.Task> = emptyList()
    private val turkish = Locale("tr", "TR")

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // v10.13 · B12: bu örneğin etiket filtresi (boş = tümü)
        val filtre = WidgetFiltre.filtre(context, widgetId)
        val all = Store.loadTasks(context)
            .filter { WidgetFiltre.gecerMi(it.etiket, filtre) }
        // v10.21: satır filtreleri kullanıcıda — tamamlananları göster,
        // tarihsizleri göster, ileri tarihlileri göster, satır sayısı.
        // kova düzeni (bugün → tarihsiz → ileri) korunur; "tamamlanmamış"
        // zorlaması artık filtre arkasında (varsayılan = eski davranış).
        items = WidgetListe.gorevleriSec(
            gorevler = all,
            bitenleriGoster = WidgetListe.gosterBool(context, WidgetListe.K_TW_BITEN, false),
            tarihsiziGoster = WidgetListe.gosterBool(context, WidgetListe.K_TW_TARIHSIZ, true),
            ilerisiniGoster = WidgetListe.gosterBool(context, WidgetListe.K_TW_ILERISI, true),
            limit = WidgetListe.satir(context, WidgetListe.K_TW_SATIR, 40),
            bugunSonuMs = WidgetCommon.endOfToday()
        )
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_task_row)
        val task = items.getOrNull(position) ?: return row

        row.setTextViewText(R.id.rowTitle, task.text)

        val now = System.currentTimeMillis()
        row.setTextViewText(
            R.id.rowTime,
            when {
                task.dueAt == 0L -> ""
                task.dueAt < now -> context.getString(R.string.w_late)
                task.dueAt <= WidgetCommon.endOfToday() ->
                    SimpleDateFormat("HH:mm", turkish).format(Date(task.dueAt))
                else -> SimpleDateFormat("d MMM", turkish).format(Date(task.dueAt))
            }
        )

        // v7.66: uygulama temasina gore boya
        val pal = WidgetTema.palet(context)
        WidgetTema.metin(row, R.id.rowTitle, pal.metin)
        WidgetTema.metin(row, R.id.rowBox, pal.vurgu)
        WidgetTema.metin(row, R.id.rowTime, pal.metinSoluk)

        // Satıra dokunma → görevi tamamla (collection widget'ta fillInIntent kullanılır)
        val fillIn = Intent().apply {
            putExtra(TasksWidget.EXTRA_TASK_ID, task.id)
        }
        row.setOnClickFillInIntent(R.id.rowRoot, fillIn)
        return row
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
