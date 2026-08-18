package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

/**
 * v7.65 — Vakit Planı widget'ının satırlarını üreten servis.
 *
 * Gösterilen işler: **o an aktif olan dilimin** işleri. Vakit ilerledikçe
 * liste kendiliğinden değişir — sabahleyin ezber işleri, öğleden sonra
 * pratik işleri görünür. Plan sekmesindeki sıralama kuralı burada da
 * geçerli: bitmemişler önce, sonra öncelik, sonra kullanıcı sırası.
 *
 * Aktif dilimde hiç iş yoksa gün içindeki diğer bekleyen işler gösterilir;
 * widget boş kalıp işe yaramaz hâle gelmesin.
 */
class PlanWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        PlanFactory(applicationContext)
}

private class PlanFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var items: List<NamazPlan.Is> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = try {
            val gun = NamazVakti.bugunDuzeltilmis(context)
            val dilim = NamazPlan.aktifDilim(gun, NamazVakti.simdiDakika())
            val buDilim = NamazPlan.dilimIsleri(context, dilim)

            if (buDilim.isNotEmpty()) {
                buDilim.take(30)
            } else {
                // Aktif dilim boşsa günün kalan işlerini göster
                NamazPlan.isleriYukle(context)
                    .filter { !it.tamamlandi }
                    .sortedWith(compareBy({ -it.oncelik }, { it.sira }, { it.id }))
                    .take(30)
            }
        } catch (e: Exception) {
            android.util.Log.w("PlanWidgetService", "Liste hazırlanamadı", e)
            emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_plan_row)
        val is_ = items.getOrNull(position) ?: return row

        row.setTextViewText(R.id.prBox, if (is_.tamamlandi) "✓" else "○")
        row.setTextViewText(
            R.id.prTitle,
            buildString {
                if (is_.oncelikSimgesi.isNotBlank()) append(is_.oncelikSimgesi).append(" ")
                append(is_.metin)
            }
        )
        row.setTextViewText(
            R.id.prTime,
            if (is_.sureDk > 0) context.getString(R.string.pe_dk, is_.sureDk) else ""
        )

        // v7.66: uygulama temasina gore boya (tamamlanan soluk)
        val pal = WidgetTema.palet(context)
        WidgetTema.metin(
            row, R.id.prTitle,
            if (is_.tamamlandi) pal.metinSoluk else pal.metin
        )
        WidgetTema.metin(row, R.id.prBox, pal.vurgu)
        WidgetTema.metin(row, R.id.prTime, pal.metinSoluk)

        // Satıra dokunma → işi tamamla / geri al
        row.setOnClickFillInIntent(
            R.id.prRoot,
            Intent().putExtra(PlanWidget.EXTRA_IS_ID, is_.id)
        )
        return row
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
