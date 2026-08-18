package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v7.40 — Günlük brifing widget'ı (4×3).
 *
 * ── Neden gerekti? ──
 * v7.29'dan beri eklenen hiçbir özellik (quiz, bilgi kartları, öğretmen modu,
 * analitik) widget'lara yansımamıştı. Kullanıcı bekleyen tekrarları görmek için
 * uygulamayı açmak zorundaydı.
 *
 * ── Gösterilenler ──
 *  1. Selamlama + tarih + geri sayım rozeti
 *  2. Günlük odak hedefi çubuğu
 *  3. Bekleyen kart · quiz · kurs serisi (dokunulabilir)
 *  4. Bugünkü görevler (en fazla 3)
 *  5. Günün tavsiyesi — verilerden üretilen tek cümle
 *  6. Eylem şeridi: kaldığın dersе devam · odak · asistan · yenile
 *
 * ── Pil tasarımı ──
 * `updatePeriodMillis` 30 dk. Tavsiye metni **yerel olarak** üretilir —
 * yapay zekâ çağrısı YAPILMAZ. Widget'tan ağ isteği atmak pil ve kota yer;
 * ayrıca v7.34 ücretsiz mod felsefesine aykırı olurdu.
 */
class BrifingWidget : AppWidgetProvider() {

    companion object {
        /** Yenile düğmesinin yayın eylemi. */
        const val ACTION_REFRESH = "com.gunlukasistan.app.BRIEF_REFRESH"

        /** Ekran indeksleri — WidgetCommon'da olmayanlar. */
        private const val SCREEN_COURSES = 13
        private const val SCREEN_ASISTAN = 9

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_brifing)

            // v7.42: widget'ın gerçek ölçüsüne göre uyarlanabilir düzen
            val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
            val yukseklik = WidgetCommon.yukseklikDp(manager, widgetId, 180)
            val kademe = WidgetCommon.boyutKademesi(genislik)

            ustSeridiCiz(context, views, kademe)
            hedefiCiz(context, views, kademe)
            bekleyenleriCiz(context, views, kademe)
            kartiCiz(context, views, yukseklik)
            gorevleriCiz(context, views, yukseklik)
            tavsiyeCiz(context, views, yukseklik)
            eylemleriCiz(context, views, kademe)

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.brBg, context, pal, R.id.brRoot)
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(
                        R.id.brGreeting, R.id.brCards, R.id.brQuiz, R.id.brStreak,
                        R.id.brCardText, R.id.brTask1, R.id.brTask2, R.id.brTask3,
                        R.id.brResume, R.id.brFocus, R.id.brAsk, R.id.brRefresh
                    ),
                    soluklar = intArrayOf(R.id.brDate, R.id.brGoalText, R.id.brTasksLabel),
                    vurgular = intArrayOf(R.id.brCountdown, R.id.brTip, R.id.brCardDont),
                    cipler = intArrayOf(R.id.brCountdown),
                    yesiller = intArrayOf(R.id.brCardKnow)
                )
                listOf(
                    R.id.brCards, R.id.brQuiz, R.id.brStreak, R.id.brCardBox,
                    R.id.brCardKnow, R.id.brCardDont, R.id.brFocus,
                    R.id.brAsk, R.id.brRefresh
                ).forEach { WidgetTema.notrDugme(views, it, pal) }
                WidgetTema.vurguDugme(views, R.id.brCardFlip, R.id.brCardFlip, pal)
                WidgetTema.vurguDugme(views, R.id.brResume, R.id.brResume, pal)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        // ═══════════════════════════════════════════════════════════
        // 1) ÜST ŞERİT
        // ═══════════════════════════════════════════════════════════

        private fun ustSeridiCiz(context: Context, views: RemoteViews, kademe: Int) {
            views.setTextViewText(R.id.brGreeting, WidgetCommon.greeting(context))
            views.setTextViewText(
                R.id.brDate,
                SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR")).format(Date())
            )
            // Çok dar widget'ta tarih satırı ve rozet gizlenir
            WidgetCommon.goster(views, R.id.brDate, kademe >= 1)
            WidgetCommon.goster(views, R.id.brCountdown, kademe >= 1)
            WidgetCommon.yaziBoyutu(views, R.id.brGreeting, 13f, 14f, 15f, kademe)
            views.setTextViewText(R.id.brCountdown, WidgetCommon.countdownShort(context))
            views.setOnClickPendingIntent(
                R.id.brCountdown,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_EVENTS, 300)
            )
            views.setOnClickPendingIntent(
                R.id.brGreeting,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TODAY, 301)
            )
        }

        // ═══════════════════════════════════════════════════════════
        // 2) GÜNLÜK HEDEF
        // ═══════════════════════════════════════════════════════════

        private fun hedefiCiz(context: Context, views: RemoteViews, kademe: Int) {
            val odak = Store.getTodayFocusMinutes(context)
            val hedef = Store.getGoalMinutes(context)
            val yuzde = if (hedef > 0) (odak * 100 / hedef).coerceIn(0, 100) else 0

            views.setProgressBar(R.id.brProgress, 100, yuzde, false)
            // Dar widget'ta kısa biçim: "45/120 dk"
            views.setTextViewText(
                R.id.brGoalText,
                if (kademe == 0) odak.toString() + "/" + hedef + " dk"
                else context.getString(R.string.w_goal_line, odak, hedef, yuzde)
            )
        }

        // ═══════════════════════════════════════════════════════════
        // 3) BEKLEYEN İŞLER
        // ═══════════════════════════════════════════════════════════

        private fun bekleyenleriCiz(context: Context, views: RemoteViews, kademe: Int) {
            // Bilgi kartları — bugün tekrar bekleyen
            val kart = try {
                KartStore.bekleyenSayisi(context)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Kart sayısı okunamadı", e)
                0
            }
            views.setTextViewText(
                R.id.brCards,
                if (kart > 0) context.getString(R.string.bw_cards, kart)
                else context.getString(R.string.bw_cards_none)
            )
            views.setOnClickPendingIntent(
                R.id.brCards,
                aktiviteAc(context, KartActivity::class.java, 310)
            )

            // Quiz tekrarı
            val quiz = try {
                QuizStore.tekrarSayisi(context)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Quiz sayısı okunamadı", e)
                0
            }
            views.setTextViewText(
                R.id.brQuiz,
                if (quiz > 0) context.getString(R.string.bw_quiz, quiz)
                else context.getString(R.string.bw_quiz_none)
            )
            views.setOnClickPendingIntent(
                R.id.brQuiz,
                WidgetCommon.openScreen(context, SCREEN_COURSES, 311)
            )

            // Kurs serisi
            val seri = try {
                Store.kursSeri(context)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Seri okunamadı", e)
                null
            }
            val seriMetin = when {
                seri == null -> context.getString(R.string.bw_streak, 0)
                seri.bugunCalisildi -> context.getString(R.string.bw_streak_ok, seri.gunSayisi)
                else -> context.getString(R.string.bw_streak, seri.gunSayisi)
            }
            views.setTextViewText(R.id.brStreak, seriMetin)
            // Çok dar widget'ta üç rozet sığmaz — seri gizlenir
            WidgetCommon.goster(views, R.id.brStreak, kademe >= 1)
            views.setOnClickPendingIntent(
                R.id.brStreak,
                WidgetCommon.openScreen(context, SCREEN_COURSES, 312)
            )
        }

        // ═══════════════════════════════════════════════════════════
        // v7.45) BİLGİ KARTI — widget üzerinde tam tekrar döngüsü
        // ═══════════════════════════════════════════════════════════

        /**
         * Bekleyen kart varsa widget'a gömer.
         *
         * Akış: ön yüz görünür → "Çevir" → arka yüz + ✓/✗ düğmeleri →
         * cevap verilince Leitner güncellenir ve sıradaki kart gelir.
         * Uygulama hiç açılmaz.
         *
         * Alçak widget'ta gizlenir — yer yoksa görevler önceliklidir.
         */
        private fun kartiCiz(context: Context, views: RemoteViews, yukseklik: Int) {
            // 4x3 altında yer yok
            if (yukseklik < 190) {
                WidgetCommon.goster(views, R.id.brCardBox, false)
                return
            }

            val kart = WidgetEylem.gecerliKart(context)
            if (kart == null) {
                WidgetCommon.goster(views, R.id.brCardBox, false)
                return
            }

            WidgetCommon.goster(views, R.id.brCardBox, true)
            val acik = WidgetEylem.kartAcikMi(context)

            views.setTextViewText(
                R.id.brCardText,
                if (acik) kart.on + "\n→ " + kart.arka else kart.on
            )
            views.setTextViewText(
                R.id.brCardFlip,
                if (acik) context.getString(R.string.we_kart_gizle)
                else context.getString(R.string.we_kart_cevir)
            )
            views.setOnClickPendingIntent(
                R.id.brCardFlip,
                WidgetEylem.niyet(context, WidgetEylem.IS_KART_CEVIR, 3500)
            )
            views.setOnClickPendingIntent(
                R.id.brCardText,
                WidgetEylem.niyet(context, WidgetEylem.IS_KART_CEVIR, 3501)
            )

            // Cevap düğmeleri yalnızca arka yüz açıkken görünür
            WidgetCommon.goster(views, R.id.brCardKnow, acik)
            WidgetCommon.goster(views, R.id.brCardDont, acik)
            if (acik) {
                views.setOnClickPendingIntent(
                    R.id.brCardKnow,
                    WidgetEylem.niyet(context, WidgetEylem.IS_KART_BILDIM, 3502)
                )
                views.setOnClickPendingIntent(
                    R.id.brCardDont,
                    WidgetEylem.niyet(context, WidgetEylem.IS_KART_BILMEDIM, 3503)
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 4) BUGÜNKÜ GÖREVLER
        // ═══════════════════════════════════════════════════════════

        private fun gorevleriCiz(context: Context, views: RemoteViews, yukseklik: Int) {
            val gunSonu = WidgetCommon.endOfToday()
            val tumGorevler = try {
                Store.loadTasks(context)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Görevler okunamadı", e)
                emptyList()
            }

            // Öncelik: bugüne tarihli/geçmiş > tarihsiz
            val bekleyen = tumGorevler.filter { !it.done }
            val bugunku = bekleyen.filter { it.dueAt in 1..gunSonu }
            val tarihsiz = bekleyen.filter { it.dueAt <= 0L }
            // v7.42: yüksekliğe göre 1-3 görev göster
            val satirAdedi = when {
                yukseklik < 150 -> 1
                yukseklik < 200 -> 2
                else -> 3
            }
            val gosterilecek = (bugunku + tarihsiz).take(satirAdedi)

            views.setTextViewText(
                R.id.brTasksLabel,
                if (bekleyen.isEmpty()) context.getString(R.string.bw_tasks_none)
                else context.getString(R.string.bw_tasks, bekleyen.size)
            )

            val satirlar = intArrayOf(R.id.brTask1, R.id.brTask2, R.id.brTask3)
            satirlar.forEachIndexed { i, id ->
                val gorev = gosterilecek.getOrNull(i)
                if (gorev == null) {
                    views.setViewVisibility(id, View.GONE)
                } else {
                    views.setViewVisibility(id, View.VISIBLE)
                    val isaret = if (gorev.dueAt in 1..gunSonu) "🔸" else "▫"
                    views.setTextViewText(id, isaret + " " + gorev.text)
                }
            }

            views.setOnClickPendingIntent(
                R.id.brTasksLabel,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TASKS, 320)
            )
            satirlar.forEachIndexed { i, id ->
                views.setOnClickPendingIntent(
                    id,
                    WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TASKS, 321 + i)
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 5) GÜNÜN TAVSİYESİ
        // ═══════════════════════════════════════════════════════════

        /**
         * Verilerden tek cümlelik tavsiye üretir.
         *
         * Tasarım kararı: bu metin **yerel** hesaplanır, yapay zekâ çağrılmaz.
         * Widget her 30 dakikada yenilendiği için ağ isteği hem pil hem kota
         * yerdi. Kurallar öncelik sırasına göre değerlendirilir; ilk uyan kazanır.
         */
        private fun tavsiyeCiz(context: Context, views: RemoteViews, yukseklik: Int) {
            // Alçak widget'ta tavsiye satırı gizlenir
            WidgetCommon.goster(views, R.id.brTip, yukseklik >= 170)
            views.setTextViewText(R.id.brTip, tavsiyeUret(context))
            views.setOnClickPendingIntent(
                R.id.brTip,
                WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TODAY, 330)
            )
        }

        private fun tavsiyeUret(context: Context): String {
            return try {
                val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val odak = Store.getTodayFocusMinutes(context)
                val hedef = Store.getGoalMinutes(context)
                val seri = Store.kursSeri(context)
                val kart = KartStore.bekleyenSayisi(context)
                val quiz = QuizStore.tekrarSayisi(context)

                when {
                    // Seri tehlikede — akşam olmuş ve bugün hiç çalışılmamış
                    saat >= 19 && !seri.bugunCalisildi && seri.gunSayisi > 0 ->
                        context.getString(R.string.bw_tip_streak_risk, seri.gunSayisi)

                    // Hedef tamamlandı
                    hedef > 0 && odak >= hedef ->
                        context.getString(R.string.bw_tip_goal_done)

                    // Tekrar birikmiş
                    kart + quiz >= 10 ->
                        context.getString(R.string.bw_tip_backlog, kart + quiz)

                    // Sabah, henüz başlanmamış
                    saat in 5..11 && odak == 0 ->
                        context.getString(R.string.bw_tip_morning)

                    // Hedefe az kaldı
                    hedef > 0 && odak > 0 && (hedef - odak) in 1..20 ->
                        context.getString(R.string.bw_tip_almost, hedef - odak)

                    // Kart bekliyor
                    kart > 0 ->
                        context.getString(R.string.bw_tip_cards, kart)

                    // Quiz bekliyor
                    quiz > 0 ->
                        context.getString(R.string.bw_tip_quiz, quiz)

                    // Gece geç
                    saat >= 23 ->
                        context.getString(R.string.bw_tip_late)

                    // Seri devam ediyor
                    seri.bugunCalisildi && seri.gunSayisi >= 3 ->
                        context.getString(R.string.bw_tip_streak_ok, seri.gunSayisi)

                    else -> context.getString(R.string.bw_tip_default)
                }
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Tavsiye üretilemedi", e)
                context.getString(R.string.bw_tip_default)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 6) EYLEM ŞERİDİ
        // ═══════════════════════════════════════════════════════════

        private fun eylemleriCiz(context: Context, views: RemoteViews, kademe: Int) {
            // Kaldığın derse devam
            val ders = try {
                Store.sonDers(context)
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Son ders okunamadı", e)
                null
            }
            // Dar widget'ta ders adı daha kısa, yardımcı düğmeler gizli
            val adUzunluk = if (kademe == 0) 10 else if (kademe == 1) 16 else 22
            views.setTextViewText(
                R.id.brResume,
                if (ders != null) context.getString(R.string.bw_resume, kisalt(ders.title, adUzunluk))
                else context.getString(R.string.bw_courses)
            )
            WidgetCommon.goster(views, R.id.brAsk, kademe >= 1)
            WidgetCommon.goster(views, R.id.brRefresh, kademe >= 2)
            views.setOnClickPendingIntent(
                R.id.brResume,
                WidgetCommon.openScreen(context, SCREEN_COURSES, 340)
            )

            // v7.45: odak düğmesi doğrudan oturum başlatır/durdurur
            views.setOnClickPendingIntent(
                R.id.brFocus,
                if (TimerEngine.isRunning(context))
                    WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_DUR, 3411)
                else WidgetEylem.niyet(context, WidgetEylem.IS_ODAK_25, 3410)
            )
            views.setOnClickPendingIntent(
                R.id.brAsk,
                WidgetCommon.openScreen(context, SCREEN_ASISTAN, 342)
            )

            // Yenile — uygulamayı açmadan widget'ı tazeler
            val yenile = Intent(context, BrifingWidget::class.java).apply {
                action = ACTION_REFRESH
                data = android.net.Uri.parse("gunlukasistan://brief/refresh")
            }
            views.setOnClickPendingIntent(
                R.id.brRefresh,
                PendingIntent.getBroadcast(
                    context, 343, yenile,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        /** Uygulama içi bir Activity'yi açan PendingIntent. */
        private fun aktiviteAc(
            context: Context,
            sinif: Class<*>,
            istekKodu: Int
        ): PendingIntent {
            val intent = Intent(context, sinif).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = android.net.Uri.parse("gunlukasistan://act/" + istekKodu)
            }
            return PendingIntent.getActivity(
                context, istekKodu, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun kisalt(metin: String, uzunluk: Int): String =
            if (metin.length <= uzunluk) metin else metin.take(uzunluk - 1).trimEnd() + "…"

        /** Bu widget türünün tüm örneklerini tazeler. */
        fun hepsiniTazele(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, BrifingWidget::class.java)
                )
                ids.forEach { render(context, manager, it) }
            } catch (e: Exception) {
                android.util.Log.w("BrifingWidget", "Tazeleme başarısız", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    /**
     * v7.42: Kullanıcı widget'ı yeniden boyutlandırınca içeriği tazele.
     * Bu olmadan büyütme/küçültme sonrası düzen eski ölçüde kalıyordu.
     */
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
        if (intent.action == ACTION_REFRESH) {
            hepsiniTazele(context)
        }
    }
}
