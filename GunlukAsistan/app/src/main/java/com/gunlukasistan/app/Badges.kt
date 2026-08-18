package com.gunlukasistan.app

import android.content.Context

/**
 * Başarım rozetleri: koşullar gerçek Store istatistiklerinden canlı hesaplanır.
 */
object Badges {

    class Badge(
        val id: String,
        val emoji: String,
        val title: String,
        val desc: String,
        val check: (Context) -> Boolean
    )

    val all = listOf(
        Badge("first_step", "👣", "İlk Adım", "İlk maddeni tamamla") {
            Store.allTimeCompletions(it) >= 1
        },
        Badge("streak_3", "🔥", "Ateşi Yaktın", "3 günlük seri") {
            Store.streakInfo(it).first >= 3
        },
        Badge("streak_7", "⚡", "Haftalık Zincir", "7 günlük seri") {
            Store.streakInfo(it).second >= 7
        },
        Badge("streak_30", "💎", "Efsane Seri", "30 günlük seri") {
            Store.streakInfo(it).second >= 30
        },
        Badge("c100", "🏅", "Yüzlük Kulübü", "100 madde tamamla") {
            Store.allTimeCompletions(it) >= 100
        },
        Badge("c500", "🏆", "Beş Yüzlük Usta", "500 madde tamamla") {
            Store.allTimeCompletions(it) >= 500
        },
        Badge("q500", "🔢", "Soru Makinesi", "Toplam 500 soru çöz") {
            Store.allTimeQuestions(it) >= 500
        },
        Badge("q2000", "🚀", "İki Bin Kulübü", "Toplam 2000 soru çöz") {
            Store.allTimeQuestions(it) >= 2000
        },
        Badge("focus_10h", "⏱️", "On Saatlik Odak", "600 dk odaklanma") {
            Store.allTimeFocus(it) >= 600
        },
        Badge("focus_50h", "🧠", "Elit Odaklanan", "3000 dk odaklanma") {
            Store.allTimeFocus(it) >= 3000
        },
        Badge("topic_full", "💯", "Tam İsabet", "Bir konuyu %100 bitir") { ctx ->
            Store.loadTopics(ctx).any { it.items.isNotEmpty() && it.percent == 100 }
        },
        Badge("kpss_ready", "🎓", "KPSS'li", "KPSS müfredatını takibe başla") { ctx ->
            !KpssPack.hasMissing(ctx) || Store.loadTopics(ctx)
                .any { it.title.startsWith("📖 KPSS") || it.title.startsWith("📜 KPSS") }
        }
    )

    fun earned(context: Context): List<Badge> = all.filter { it.check(context) }

    fun earnedCount(context: Context): Int = earned(context).size
}
