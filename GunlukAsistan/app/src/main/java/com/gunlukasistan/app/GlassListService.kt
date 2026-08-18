package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cam görünümlü liste widget'larının satırlarını üretir (v6.3).
 *
 * Tek servis üç farklı içeriği besler; hangi içerik olduğu intent'teki
 * [GlassWidgetBase.EXTRA_KIND] ile belirlenir.
 */
class GlassListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val kind = intent.getStringExtra(GlassWidgetBase.EXTRA_KIND)
            ?: GlassWidgetBase.KIND_TASKS
        return GlassFactory(applicationContext, kind)
    }
}

private class GlassFactory(
    private val context: Context,
    private val kind: String
) : RemoteViewsService.RemoteViewsFactory {

    /** Satır verisi: metin, sağ etiket, tamamlandı mı, tıklama kimliği. */
    private class Item(
        val text: String,
        val badge: String,
        val done: Boolean,
        val id: Long
    )

    private var items: List<Item> = emptyList()
    private val turkish = Locale("tr", "TR")

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Hata durumunda bile boş liste dönmeli; aksi halde widget
        // "Yükleniyor…" ekranında takılı kalır.
        items = try {
            when (kind) {
                GlassWidgetBase.KIND_HABITS -> buildHabits()
                GlassWidgetBase.KIND_TODAY -> buildToday()
                else -> buildTasks()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildTasks(): List<Item> {
        val all = Store.loadTasks(context)
        val endOfToday = WidgetCommon.endOfToday()
        val now = System.currentTimeMillis()
        val pending = all.filter { !it.done }
        val dated = pending.filter { it.dueAt in 1..endOfToday }.sortedBy { it.dueAt }
        val undated = pending.filter { it.dueAt == 0L }.sortedByDescending { it.createdAt }
        val later = pending.filter { it.dueAt > endOfToday }.sortedBy { it.dueAt }
        // v7.42: limit 15 → 30 (widget artık 4x5'e kadar büyüyebiliyor)
        return (dated + undated + later).take(30).map { task ->
            Item(
                text = task.text,
                badge = when {
                    task.dueAt == 0L -> ""
                    task.dueAt < now -> context.getString(R.string.gl_late)
                    task.dueAt <= endOfToday ->
                        SimpleDateFormat("HH:mm", turkish).format(Date(task.dueAt))
                    else -> SimpleDateFormat("d MMM", turkish).format(Date(task.dueAt))
                },
                done = false,
                id = task.id
            )
        }
    }

    private fun buildHabits(): List<Item> =
        Store.loadHabits(context).filterNot { it.archived }.take(30).map { habit ->
            val count = Store.habitCount(context, habit.id)
            val done = count >= habit.target
            // Seri yalnızca tek hedefli alışkanlıklarda gösteriliyor;
            // gereksiz hesaplama yapmıyoruz (performans)
            val streak = if (habit.target > 1) 0 else Store.habitStreak(context, habit)
            Item(
                text = "${habit.emoji}  ${habit.title}",
                badge = when {
                    habit.target > 1 -> "$count/${habit.target}"
                    streak > 0 -> "🔥$streak"
                    else -> ""
                },
                done = done,
                id = habit.id
            )
        }

    /** Bugün: önce bekleyen alışkanlıklar, sonra bugüne tarihli görevler. */
    private fun buildToday(): List<Item> {
        val out = mutableListOf<Item>()
        Store.loadHabits(context).filterNot { it.archived }.forEach { habit ->
            val count = Store.habitCount(context, habit.id)
            out.add(
                Item(
                    text = "${habit.emoji}  ${habit.title}",
                    badge = if (habit.target > 1) "$count/${habit.target}" else "",
                    done = count >= habit.target,
                    id = -habit.id   // negatif = alışkanlık
                )
            )
        }
        val endOfToday = WidgetCommon.endOfToday()
        Store.loadTasks(context)
            .filter { !it.done && (it.dueAt == 0L || it.dueAt <= endOfToday) }
            .sortedBy { if (it.dueAt == 0L) Long.MAX_VALUE else it.dueAt }
            .forEach { task ->
                out.add(
                    Item(
                        text = task.text,
                        badge = if (task.dueAt > 0L) {
                            SimpleDateFormat("HH:mm", turkish).format(Date(task.dueAt))
                        } else "",
                        done = false,
                        id = task.id
                    )
                )
            }
        return out.take(30)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_glass_row)
        val item = items.getOrNull(position) ?: return row

        row.setTextViewText(R.id.rowText, item.text)
        row.setTextViewText(R.id.rowBadge, item.badge)

        // v7.66: sabit gri/beyaz yerine uygulama temasinin metin renkleri
        val pal = WidgetTema.palet(context)
        if (item.done) {
            row.setInt(R.id.rowCheck, "setBackgroundResource", R.drawable.g_check_on)
            row.setTextViewText(R.id.rowCheck, "✓")
            row.setTextColor(R.id.rowText, pal.metinSoluk)
        } else {
            row.setInt(R.id.rowCheck, "setBackgroundResource", R.drawable.g_check_off)
            row.setTextViewText(R.id.rowCheck, "")
            row.setTextColor(R.id.rowText, pal.metin)
        }
        WidgetTema.metin(row, R.id.rowBadge, pal.vurgu)

        val fillIn = Intent().apply {
            putExtra(GlassWidgetBase.EXTRA_ITEM_ID, item.id)
            putExtra(GlassWidgetBase.EXTRA_KIND, kind)
        }
        row.setOnClickFillInIntent(R.id.rowRoot, fillIn)
        return row
    }

    /**
     * null dönmek sistemin kendi "Yükleniyor…" görünümünü kullanmasına yol açar
     * ve veri gecikince ekranda takılı kalır. Kendi hafif satırımızı veriyoruz.
     */
    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_glass_row).apply {
            setTextViewText(R.id.rowText, context.getString(R.string.gl_loading))
            setTextViewText(R.id.rowBadge, "")
            setTextViewText(R.id.rowCheck, "")
        }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
