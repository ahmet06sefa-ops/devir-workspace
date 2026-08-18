package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Tüm widget'ların ortak yardımcıları (v5.3).
 * Ekran açma niyetleri, hızlı eylemler ve toplu tazeleme burada toplanır.
 */
object WidgetCommon {

    /** MainActivity'ye "şu ekranı aç" demek için. */
    const val EXTRA_OPEN_SCREEN = "open_screen"

    /** Uygulama açılır açılmaz bir diyalog göstermek için (ör. soru ekle). */
    const val EXTRA_QUICK_ACTION = "quick_action"

    const val QUICK_QUESTION = "q_add"
    const val QUICK_TASK = "task_add"

    // Ekran indeksleri (MainActivity.createFragment ile aynı)
    const val SCREEN_HOME = 0
    const val SCREEN_TODAY = 2
    const val SCREEN_TIMER = 4
    const val SCREEN_TASKS = 6
    const val SCREEN_EVENTS = 11
    const val SCREEN_HABITS = 12
    /** v7.55: Vakit Planı sekmesi (alt menüde Sayaç'ın yerinde). */
    const val SCREEN_PLAN = 16

    /** Belirli bir ekranı açan PendingIntent üretir. */
    fun openScreen(context: Context, screen: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SCREEN, screen)
            // Aynı requestCode'lu intent'lerin birbirini ezmemesi için
            data = android.net.Uri.parse("gunlukasistan://screen/$screen/$requestCode")
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Uygulamayı açıp doğrudan bir diyalog gösterir (soru/görev ekleme). */
    fun quickAction(context: Context, action: String, screen: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SCREEN, screen)
            putExtra(EXTRA_QUICK_ACTION, action)
            data = android.net.Uri.parse("gunlukasistan://action/$action/$requestCode")
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Bugün 23:59:59 (bugüne tarihli görevleri süzmek için). */
    fun endOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    /** Saate göre selamlama. */
    fun greeting(context: Context): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> context.getString(R.string.w_hi_morning)
            in 12..17 -> context.getString(R.string.w_hi_noon)
            in 18..22 -> context.getString(R.string.w_hi_evening)
            else -> context.getString(R.string.w_hi_night)
        }
    }

    /**
     * Öne çıkan geri sayımın kısa metni.
     * Sabitlenmiş etkinlik > yaklaşan etkinlik > sınav tarihi sırasıyla bakar.
     */
    fun countdownShort(context: Context): String {
        val highlight = Store.highlightEvent(context)
        if (highlight != null) {
            val left = highlight.daysLeft
            return when {
                left > 0 -> "${highlight.emoji} $left gün"
                left == 0 -> "${highlight.emoji} bugün!"
                else -> "${highlight.emoji} geçti"
            }
        }
        val examMillis = Store.getExamDateMillis(context)
        if (examMillis <= 0L) return context.getString(R.string.w_no_event)
        val days = ((examMillis - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
        return if (days > 0) "⏳ $days gün" else "🍀 Sınav!"
    }

    /**
     * Art arda gelen tazeleme isteklerini bastırmak için son çalışma zamanı (v7.0).
     * Tek bir işlemde `saveTasks` + `bumpToday` + `maybeAutoBackup` gibi birden
     * fazla nokta refreshAll çağırıyordu; her biri 7 widget'ı tetikleyince
     * sistem yayın kuyruğu doluyor ve widget'lar "Yükleniyor"da kalıyordu.
     */
    @Volatile private var lastRefresh = 0L

    /**
     * v10.13 · B11: tüm widget metinlerine uygulanan yazı ölçeği.
     * [WidgetTema.palet] her çağrıldığında tercihten senkronlanır;
     * RemoteViews çizimleri birden çok süreçte koşabildiği için
     * doğrudan SharedPreferences okunmaz, bu alan üzerinden geçilir.
     */
    @Volatile var yaziOlcek: Float = 1f

    /**
     * v7.41: Tazelenecek TÜM widget sınıfları.
     *
     * Eskiden bu liste elle yazılıyordu ve iki widget ailesi unutulmuştu:
     *   · Cam widget'lar (GlassTasks/GlassHabits/GlassToday) hiç tazelenmiyordu
     *   · Yeni eklenen widget'ları buraya eklemeyi unutmak kolaydı
     * Artık tek yerde toplandı — yeni widget eklenince sadece buraya yazılır.
     */
    private val TUM_WIDGETLAR = listOf(
        CountdownWidget::class.java,
        SummaryWidget::class.java,
        TasksWidget::class.java,
        ActionsWidget::class.java,
        BrifingWidget::class.java,
        GlassTasksWidget::class.java,
        GlassHabitsWidget::class.java,
        GlassTodayWidget::class.java,
        // v7.47: minimalist namaz widget'ı
        NamazWidget::class.java,
        // v7.65: Vakit Planı widget'ı (Plan sekmesinin ana ekran karşılığı)
        PlanWidget::class.java,
        // v7.83: program ilerleme widget'ı
        IlerlemeWidget::class.java,
        // v7.85: sayaç widget'ı
        SayacWidget::class.java,
        // v10.5 · C31: çoklu geri sayım listesi
        EventsListWidget::class.java,
        // v10.5 · C32: hedef halkası
        HedefWidget::class.java,
        // v10.10 · C34: hafta görünümü
        HaftaWidget::class.java,
        // v10.13 · B7/B8/B9/B10: yeni nesil widget ailesi
        KokpitWidget::class.java,
        TakvimWidget::class.java,
        UykuWidget::class.java,
        OdakKutusuWidget::class.java,
        // v10.16: birleştirilebilir widget (modül ızgarası)
        ModulWidget::class.java
    )

    /**
     * Liste tabanlı widget'lar ve yenilenecek koleksiyon görünümleri.
     * `notifyAppWidgetViewDataChanged` çağrılmazsa satırlar eski veriyle kalır.
     */
    private val LISTE_WIDGETLARI = listOf(
        TasksWidget::class.java to R.id.twList,
        GlassTasksWidget::class.java to R.id.glList,
        GlassHabitsWidget::class.java to R.id.glList,
        GlassTodayWidget::class.java to R.id.glList,
        // v7.65: plan widget'ının iş listesi
        PlanWidget::class.java to R.id.pwList,
        // v10.5 · C31: geri sayım listesi
        EventsListWidget::class.java to R.id.evList,
        // v10.13 · B8: ay ızgarası (yoğunluk noktaları tazelensin)
        TakvimWidget::class.java to R.id.ayGrid
    )


    // ═══════════════════════════════════════════════════════════════
    // v7.42 — BOYUTA UYARLANABİLİR DÜZEN
    // ═══════════════════════════════════════════════════════════════
    //
    // Kullanıcı widget'ı küçülttüğünde içerik kırpılıyor, bazen taşıyordu.
    // Artık her widget kendi ölçüsünü okuyup hangi bölümleri göstereceğine
    // karar veriyor.

    /** Widget'ın o anki genişliği (dp). Okunamazsa varsayılan döner. */
    fun genislikDp(manager: AppWidgetManager, widgetId: Int, varsayilan: Int = 250): Int =
        try {
            val opts = manager.getAppWidgetOptions(widgetId)
            val w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            if (w > 0) w else varsayilan
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Genişlik okunamadı", e)
            varsayilan
        }

    /** Widget'ın o anki yüksekliği (dp). */
    fun yukseklikDp(manager: AppWidgetManager, widgetId: Int, varsayilan: Int = 110): Int =
        try {
            val opts = manager.getAppWidgetOptions(widgetId)
            val h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            if (h > 0) h else varsayilan
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Yükseklik okunamadı", e)
            varsayilan
        }

    /** Boyut kademesi: 0 = çok dar, 1 = orta, 2 = geniş. */
    fun boyutKademesi(genislik: Int): Int = when {
        genislik < 150 -> 0
        genislik < 250 -> 1
        else -> 2
    }

    /**
     * Yüksekliğe göre kaç satır içerik sığar.
     * Başlık ve kenar boşlukları düşülür.
     */
    fun sigacakSatir(yukseklik: Int, satirYuksekligi: Int = 26, baslikPayi: Int = 60): Int =
        ((yukseklik - baslikPayi) / satirYuksekligi).coerceIn(1, 12)

    /** Görünürlük kısayolu — okunabilirlik için. */
    fun goster(views: android.widget.RemoteViews, id: Int, gorunsun: Boolean) {
        views.setViewVisibility(
            id,
            if (gorunsun) android.view.View.VISIBLE else android.view.View.GONE
        )
    }

    /**
     * Metni genişliğe göre kısaltır.
     * Dar widget'ta uzun ders adları satırı taşırıyordu.
     */
    fun sigdir(metin: String, kademe: Int): String {
        val sinir = when (kademe) {
            0 -> 14
            1 -> 24
            else -> 40
        }
        return if (metin.length <= sinir) metin
        else metin.take(sinir - 1).trimEnd() + "…"
    }

    /**
     * v7.42: Widget yazı boyutunu kademeye göre ayarlar.
     * setTextViewTextSize API 16+'da var, güvenli.
     */
    fun yaziBoyutu(
        views: android.widget.RemoteViews,
        id: Int,
        kucuk: Float,
        orta: Float,
        buyuk: Float,
        kademe: Int
    ) {
        val kademeBoyut = when (kademe) {
            0 -> kucuk
            1 -> orta
            else -> buyuk
        }
        // v10.13 · B11: kullanıcının widget yazı ölçeği her metne uygulanır
        val boyut = kademeBoyut * yaziOlcek
        try {
            views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, boyut)
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Yazı boyutu ayarlanamadı", e)
        }
    }

    /**
     * v10.20 · SINIRSIZ KONTROL — XML'de @dimen ile yazılmış metinlere
     * serbest ölçek uygular (Sayaç / Eylemler / Uyku widget'ları boyutlarını
     * programatik hiç ayarlamıyordu; v10.16'nın "her metin ölçeklenir"
     * vaadi bu üçü için eksikti — bu köprüyle gerçekleşti).
     *
     * @param tabanDimen `ga_yazi_*` dimen kaynağı; px → sp çevirimi
     *        scaledDensity ile yapılır (dimen sp olarak tanımlı).
     * @param olcek 1f ise hiç dokunulmaz (varsayılan davranış birebir korunur).
     */
    fun olcekliYazi(
        views: android.widget.RemoteViews,
        context: Context,
        id: Int,
        tabanDimen: Int,
        olcek: Float
    ) {
        if (olcek == 1f) return
        try {
            val tabanSp = context.resources.getDimension(tabanDimen) /
                context.resources.displayMetrics.scaledDensity
            views.setTextViewTextSize(
                id, android.util.TypedValue.COMPLEX_UNIT_SP, tabanSp * olcek
            )
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Ölçekli yazı ayarlanamadı", e)
        }
    }

    /** Tüm widget türlerini tazeler. Veri değişince çağrılır. */

    fun refreshAll(context: Context) = refreshAll(context, false)

    /**
     * @param zorla true ise 400 ms kısıtlaması atlanır.
     *        v7.66: tema değişiminde şart — kullanıcı rengi seçtiği anda
     *        widget'ın yeni renge geçmesi gerekiyor, kısıtlamaya takılırsa
     *        eski renkte kalıyordu.
     */
    fun refreshAll(context: Context, zorla: Boolean) {
        val now = System.currentTimeMillis()
        // v10.17: kısıt süresi kullanıcı seçimli (400 ms / 2 sn / 10 sn; varsayılan 400 ms)
        val kisit = try {
            WidgetSecim.kisitMs(context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Kısıt okunamadı", e)
            400L
        }
        if (!zorla && now - lastRefresh < kisit) return
        lastRefresh = now

        val manager = AppWidgetManager.getInstance(context) ?: return

        TUM_WIDGETLAR.forEach { cls ->
            try {
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetCommon", "Tazelenemedi: " + cls.simpleName, e)
            }
        }

        // v10.15 · C15: kilit ekranı gün paneli de aynı tazelemeden
        // beslenir (görev/sayaç/uyku değişimleri zaten bu kapıdan geçer).
        try {
            GunPaneli.tazele(context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetCommon", "Gun paneli tazelenemedi", e)
        }

        // Liste widget'larının satırlarını da yenile
        LISTE_WIDGETLARI.forEach { (cls, listeId) ->
            try {
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) {
                    manager.notifyAppWidgetViewDataChanged(ids, listeId)
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetCommon", "Liste yenilenemedi: " + cls.simpleName, e)
            }
        }
    }
}
