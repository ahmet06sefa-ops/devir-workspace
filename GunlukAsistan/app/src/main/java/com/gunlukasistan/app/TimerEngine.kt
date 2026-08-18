package com.gunlukasistan.app

import android.content.Context
import android.os.SystemClock

/**
 * Zamanlayıcının kalıcı durumu (v6.4).
 *
 * Fragment yok edilse bile (sekme değişimi, geri tuşu, ekran döndürme)
 * sayaç çalışmaya devam eder. Durum SharedPreferences'ta tutulduğu için
 * uygulama tamamen kapatılıp açılsa da kaldığı yerden sürer.
 *
 * Zaman ölçümü `elapsedRealtime` yerine duvar saatiyle (`currentTimeMillis`)
 * yapılır; böylece cihaz uykuya dalsa bile geri sayım doğru ilerler.
 */
object TimerEngine {

    private const val PREF = "timer_engine_v1"
    private const val K_MODE = "mode"
    private const val K_RUNNING = "running"
    private const val K_COUNT_TOTAL = "count_total"
    private const val K_COUNT_REMAIN = "count_remaining"
    private const val K_COUNT_END = "count_end_wall"
    private const val K_WATCH_ACC = "watch_accumulated"
    private const val K_WATCH_START = "watch_start_wall"
    private const val K_SOUND = "sound_index"

    const val MODE_DOWN = 0
    const val MODE_WATCH = 1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Temel durum ----------------

    fun mode(context: Context): Int = prefs(context).getInt(K_MODE, MODE_DOWN)

    fun setMode(context: Context, value: Int) {
        prefs(context).edit().putInt(K_MODE, value).apply()
    }

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(K_RUNNING, false)

    fun totalMs(context: Context): Long =
        prefs(context).getLong(K_COUNT_TOTAL, 25 * 60_000L)

    fun setTotalMs(context: Context, value: Long) {
        prefs(context).edit()
            .putLong(K_COUNT_TOTAL, value)
            .putLong(K_COUNT_REMAIN, value)
            .putBoolean(K_RUNNING, false)
            .apply()
        sayaciYansit(context)
    }

    fun selectedSound(context: Context): Int = prefs(context).getInt(K_SOUND, -1)

    fun setSelectedSound(context: Context, index: Int) {
        prefs(context).edit().putInt(K_SOUND, index).apply()
    }

    // ---------------- Anlık değerler ----------------

    /** Geri sayımda kalan süre (ms). Çalışıyorsa duvar saatinden hesaplanır. */
    fun remainingMs(context: Context): Long {
        val p = prefs(context)
        return if (p.getBoolean(K_RUNNING, false) && p.getInt(K_MODE, MODE_DOWN) == MODE_DOWN) {
            (p.getLong(K_COUNT_END, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            p.getLong(K_COUNT_REMAIN, p.getLong(K_COUNT_TOTAL, 25 * 60_000L))
        }
    }

    /** Kronometrede geçen süre (ms). */
    fun elapsedMs(context: Context): Long {
        val p = prefs(context)
        val acc = p.getLong(K_WATCH_ACC, 0L)
        return if (p.getBoolean(K_RUNNING, false) && p.getInt(K_MODE, MODE_DOWN) == MODE_WATCH) {
            acc + (System.currentTimeMillis() - p.getLong(K_WATCH_START, System.currentTimeMillis()))
        } else {
            acc
        }
    }

    /** Ekranda gösterilecek değer (moda göre kalan ya da geçen süre). */
    fun displayMs(context: Context): Long =
        if (mode(context) == MODE_WATCH) elapsedMs(context) else remainingMs(context)

    /** Geri sayım bitti mi? */
    fun isFinished(context: Context): Boolean =
        mode(context) == MODE_DOWN && isRunning(context) && remainingMs(context) <= 0L

    // ---------------- Kontroller ----------------

    fun start(context: Context) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val e = p.edit().putBoolean(K_RUNNING, true)
        if (p.getInt(K_MODE, MODE_DOWN) == MODE_WATCH) {
            e.putLong(K_WATCH_START, now)
        } else {
            var remain = p.getLong(K_COUNT_REMAIN, p.getLong(K_COUNT_TOTAL, 25 * 60_000L))
            if (remain <= 0L) remain = p.getLong(K_COUNT_TOTAL, 25 * 60_000L)
            e.putLong(K_COUNT_END, now + remain).putLong(K_COUNT_REMAIN, remain)
        }
        e.apply()
        // v8.2 · Öneri 2: sayaç başlarken dokunsal onay
        Titresim.sayacDurum(context)
        // v7.94: odak modu — sayaç çalışırken DND (öneri 7)
        runCatching { Pomodoro.odagiEsitle(context, calisiyor = true) }
        TimerNotifier.show(context)
        // v10.12 · Grup D: manzara otomasyonu (D22) + odak kalkanı gözcüsü (D21)
        runCatching { SesManzarasi.sayacBasladi(context) }
        runCatching { OdakKalkani.esitle(context) }
        sayaciYansit(context)
    }

    fun pause(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean(K_RUNNING, false)) return
        val e = p.edit().putBoolean(K_RUNNING, false)
        if (p.getInt(K_MODE, MODE_DOWN) == MODE_WATCH) {
            e.putLong(K_WATCH_ACC, elapsedMs(context))
        } else {
            e.putLong(K_COUNT_REMAIN, remainingMs(context))
        }
        e.apply()
        // v8.2 · Öneri 2: duraklatma da hissedilsin
        Titresim.sayacDurum(context)
        // v7.94: duraklatınca DND bırakılsın
        runCatching { Pomodoro.odagiEsitle(context, calisiyor = false) }
        TimerNotifier.show(context)
        // v10.12 · Grup D: duraklamada otomatik manzara susar, kalkan bekler
        runCatching { SesManzarasi.esitle(context) }
        runCatching { OdakKalkani.esitle(context) }
        sayaciYansit(context)
    }

    fun reset(context: Context) {
        val p = prefs(context)
        p.edit()
            .putBoolean(K_RUNNING, false)
            .putLong(K_COUNT_REMAIN, p.getLong(K_COUNT_TOTAL, 25 * 60_000L))
            .putLong(K_WATCH_ACC, 0L)
            .apply()
        runCatching { Pomodoro.odagiEsitle(context, calisiyor = false) }
        TimerNotifier.cancel(context)
        // v10.12 · Grup D: sıfırlamada otomatik akış kapanır, gözcü durur
        runCatching { SesManzarasi.esitle(context) }
        runCatching { OdakKalkani.esitle(context) }
        sayaciYansit(context)
    }

    /** Geri sayım sona erdiğinde çağrılır: odak dakikası yazılır, durum sıfırlanır. */
    fun finish(context: Context) {
        val p = prefs(context)
        val minutes = (p.getLong(K_COUNT_TOTAL, 0L) / 60_000L).toInt()
        if (minutes > 0) {
            Store.addTodayFocusMinutes(context, minutes)
            WidgetCommon.refreshAll(context)
        }
        p.edit()
            .putBoolean(K_RUNNING, false)
            .putLong(K_COUNT_REMAIN, p.getLong(K_COUNT_TOTAL, 25 * 60_000L))
            .apply()
        runCatching { Pomodoro.odagiEsitle(context, calisiyor = false) }
        // v10.12 · Grup D: evre bitişinde köprüler tazelenir
        // (döngü hemen yeni evreyi başlatırsa start() yeniden açar)
        runCatching { SesManzarasi.esitle(context) }
        runCatching { OdakKalkani.esitle(context) }
        sayaciYansit(context)
    }

    /**
     * v10.2 · Öneri A2 — Çalışan ya da yeni biten sayaca süre ekler.
     *
     * Bildirimdeki "+5 dk" düğmesinden çağrılır. Kalan süreye ekler;
     * sayaç duraklatılmış ya da bitmişse yeniden canlandırır.
     * Toplam (K_COUNT_TOTAL) DEĞİŞMEZ — istatistik "oturum 30 dk"
     * gerçeğini korur; eklenen süre uzatma olarak sayılır.
     *
     * Kronometre (watch) modunda anlamsız — dokunmaz.
     */
    fun uzat(context: Context, ekMs: Long) {
        if (mode(context) != MODE_DOWN) return
        val p = prefs(context)
        val now = System.currentTimeMillis()
        // v10.2 öz denetim: üç durum ayrı ele alınır.
        //   çalışıyor      → kalanın üstüne ekle (12:00 + 1 dk = 13:00)
        //   duraklatılmış  → kalanın üstüne ekle
        //   bitmiş         → YALNIZ ek süre (bittiğinde finish() kalanı
        //                    toplama sarar; oraya eklesek 25+5=30 çıkardı)
        val calisiyordu = isRunning(context)
        val duran = remainingMs(context).coerceAtLeast(0L)
        val toplam_ = totalMs(context)
        val taban = when {
            calisiyordu -> duran
            duran in 1 until toplam_ -> duran
            else -> 0L
        }
        val kalan = taban + ekMs
        p.edit()
            .putLong(K_COUNT_REMAIN, kalan)
            .putLong(K_COUNT_END, now + kalan)
            .putBoolean(K_RUNNING, true)
            .apply()
        runCatching { Pomodoro.odagiEsitle(context, calisiyor = true) }
        TimerNotifier.show(context)
        // v10.12 · Grup D: süre eklenen oturumda köprüler koşmaya devam etsin
        runCatching { SesManzarasi.esitle(context) }
        runCatching { OdakKalkani.esitle(context) }
        sayaciYansit(context)
    }

    /** Kronometrede biriken süreyi odak olarak kaydeder (duraklat/sıfırla anında). */
    fun creditWatch(context: Context) {
        if (mode(context) != MODE_WATCH) return
        val minutes = (elapsedMs(context) / 60_000L).toInt()
        if (minutes > 0) {
            Store.addTodayFocusMinutes(context, minutes)
            WidgetCommon.refreshAll(context)
            // v7.43: 90 dakikayı geçen oturumda mola öner (öneri 20)
            try {
                BildirimUretici.uzunOturumUyarisi(context, minutes)
            } catch (e: Exception) {
                android.util.Log.w("TimerEngine", "Uzun oturum bildirimi", e)
            }
        }
    }

    /**
     * v7.85 — Sayaç durumu değiştiğinde widget'ı tazeler.
     *
     * Widget kendi durumunu tutmaz; her şeyi buradan okur. Uygulamadan,
     * bildirimden ya da widget'tan kontrol edilsin — üçü de bu fonksiyondan
     * geçtiği için görüntü **anında senkron** kalır.
     *
     * Yalnızca sayaç widget'ı tazelenir; tüm widget'ları yenilemek
     * (refreshAll) her duraklat/başlat işleminde gereksiz iş olurdu.
     */
    fun sayaciYansit(context: Context) {
        // v7.91: SayacServisi.esitle() buradan KALDIRILDI.
        //
        // TimerNotifier.show() zaten çağırıyor ve start() ikisini art arda
        // tetikliyordu (show → esitle, sonra sayaciYansit → esitle). İkinci
        // çağrı, ilkinin başlattığı servis henüz `ayakta = true` işaretini
        // koymadan geldiği için servis iki kez başlatılıyor, sistem bunu
        // kısıtlıyordu. Tek çağrı noktası: TimerNotifier.show().
        val yonetici = android.appwidget.AppWidgetManager.getInstance(context)
        try {
            val kimlikler = yonetici.getAppWidgetIds(
                android.content.ComponentName(context, SayacWidget::class.java)
            )
            kimlikler?.forEach { SayacWidget.render(context, yonetici, it) }
        } catch (e: Exception) {
            android.util.Log.w("TimerEngine", "Sayaç widget'ı tazelenemedi", e)
        }
        // v10.13 · B7+B10: kadran ve odak kutusu da sayaçla beslenir —
        // aynı biletle tazelenmezse halka ve kalan süre donuk kalırdı.
        try {
            yonetici.getAppWidgetIds(
                android.content.ComponentName(context, KokpitWidget::class.java)
            )?.forEach { KokpitWidget.render(context, yonetici, it) }
            yonetici.getAppWidgetIds(
                android.content.ComponentName(context, OdakKutusuWidget::class.java)
            )?.forEach { OdakKutusuWidget.render(context, yonetici, it) }
        } catch (e: Exception) {
            android.util.Log.w("TimerEngine", "Kokpit/odak widget'ı tazelenemedi", e)
        }
    }

    /** "mm:ss" veya "s:mm:ss" biçimi. */
    fun format(millis: Long): String {
        val total = millis / 1000
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return if (h > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }
    }
}
