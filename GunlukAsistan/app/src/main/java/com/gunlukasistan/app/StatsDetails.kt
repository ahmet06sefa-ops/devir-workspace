package com.gunlukasistan.app

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Ana Sayfa'daki istatistik kartlarına dokununca açılan ayrıntı diyalogları.
 * Tüm içerik gerçek Store verilerinden üretilir.
 */
object StatsDetails {

    fun showStreak(context: Context) {
        val (current, best) = Store.streakInfo(context)
        val days = Store.recentDayStats(context, 7)
        val sb = StringBuilder()
        sb.append("🔥 Mevcut seri: $current gün\n")
        sb.append("🏆 En iyi seri: $best gün\n")
        val now = Calendar.getInstance()
        sb.append(
            "📅 Bu ay aktif gün: ${
                Store.monthActiveDays(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
            }\n\n"
        )
        sb.append("Son 7 gün:\n")
        days.forEach { (label, c, f) ->
            val mark = if (c > 0 || f > 0) "✅" else "⚪"
            sb.append("$mark $label — $c madde · $f dk odak\n")
        }
        dialog(context, "🔥 Seri Detayı", sb.toString())
    }

    fun showTotal(context: Context) {
        val total = Store.allTimeCompletions(context)
        val week = Store.weekCompletions(context)
        val sb = StringBuilder()
        sb.append("🏅 Tüm zamanlar: $total madde tamamlandı\n")
        sb.append("📆 Son 7 gün: $week madde\n\n")
        val topics = Store.loadTopics(context)
        if (topics.isNotEmpty()) {
            sb.append("Konu bazında tamamlanan:\n")
            topics.sortedByDescending { it.doneCount }.take(6).forEach { t ->
                sb.append("• ${t.title}: ${t.doneCount}/${t.items.size}\n")
            }
        }
        dialog(context, "🏅 Tamamlanan Maddeler", sb.toString())
    }

    fun showProgress(context: Context) {
        val topics = Store.loadTopics(context)
        val allSubs = topics.flatMap { it.items }
        val overall = if (allSubs.isEmpty()) 0 else allSubs.count { it.done } * 100 / allSubs.size
        val sb = StringBuilder()
        sb.append("🎯 Genel ilerleme: %$overall (${allSubs.count { it.done }}/" +
            "${allSubs.size} alt madde)\n\n")
        if (topics.isEmpty()) {
            sb.append("Henüz konu yok — + ile ekle veya KPSS müfredatını yükle!")
        } else {
            topics.sortedBy { it.percent }.forEach { t ->
                val bar = bar(t.percent)
                sb.append("$bar %${t.percent} — ${t.title}\n")
            }
            val weakest = topics.filter { it.items.isNotEmpty() }.minByOrNull { it.percent }
            if (weakest != null && weakest.percent < 50) {
                sb.append("\n🎯 Odak önerisi: \"${weakest.title}\" en geride — bugün ona başla!")
            }
        }
        dialog(context, "🎯 İlerleme Detayı", sb.toString())
    }

    fun showFocus(context: Context) {
        val now = Calendar.getInstance()
        val today = Store.getTodayFocusMinutes(context)
        val week = Store.weekFocus(context)
        val month = Store.monthFocus(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        val last = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = Store.monthFocus(
            context, last.get(Calendar.YEAR), last.get(Calendar.MONTH)
        )
        val allTime = Store.allTimeFocus(context)
        val sb = StringBuilder()
        sb.append("🌞 Bugün: $today dk\n")
        sb.append("📆 Son 7 gün: $week dk\n")
        sb.append("🗓️ Bu ay: $month dk\n")
        sb.append("⏮️ Geçen ay: $prevMonth dk\n")
        sb.append("🏆 Tüm zamanlar: $allTime dk\n")
        if (prevMonth > 0) {
            val change = (month - prevMonth) * 100 / prevMonth
            sb.append("\nGeçen aya göre: %${if (change >= 0) "+$change" else "$change"}")
        }
        dialog(context, "📈 Odaklanma Detayı", sb.toString())
    }

    private fun bar(percent: Int): String {
        val filled = (percent / 10).coerceIn(0, 10)
        return "▓".repeat(filled) + "░".repeat(10 - filled)
    }

    private fun dialog(context: Context, title: String, message: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message.trim())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
