package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Zamanlayıcı bildirimi (v6.4) — iPhone "Live Activity" benzeri.
 *
 * Kilit ekranında ve bildirim panelinde **canlı sayan** bir kronometre gösterir.
 * Android'in `setUsesChronometer` + `setChronometerCountDown` özelliği sayesinde
 * saniye saniye güncelleme yapmaya gerek kalmaz — sistem kendi sayar, pil yakmaz.
 *
 * Bildirim üzerinde Duraklat/Devam ve Sıfırla butonları vardır.
 */
object TimerNotifier {

    /**
     * v7.89 — Kanal sürümü v1'den v2'ye yükseltildi.
     *
     * ── Neden yeni kimlik ──
     * Eski kanal `IMPORTANCE_LOW` ile oluşturulmuştu. Samsung One UI bu
     * öneme sahip bildirimleri "Sessiz bildirimler" bölümüne indiriyor;
     * panel daraltılmışsa kullanıcı sayacı hiç görmüyordu.
     *
     * Android 8+ bir kanalın önem düzeyi **oluşturulduktan sonra kod ile
     * değiştirilemez**. Tek yol yeni kimlikle kanal açmak; eskisi
     * [ESKI_CHANNEL] üzerinden siliniyor ki ayarlar listesinde ölü kayıt
     * kalmasın.
     */
    const val CHANNEL_ID = "zamanlayici_canli_v2"

    /** v7.88 ve öncesinin kanalı — temizlenecek. */
    private const val ESKI_CHANNEL = "zamanlayici_canli_v1"
    const val NOTIF_ID = 4711

    const val ACTION_TOGGLE = "com.gunlukasistan.app.TIMER_TOGGLE"
    const val ACTION_RESET = "com.gunlukasistan.app.TIMER_RESET"
    const val ACTION_STOP = "com.gunlukasistan.app.TIMER_STOP"

    /** Sistem bildirimlerine izin veriliyor mu? (Android 13+ runtime izni dahil) */
    fun canPost(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
    }

    /** Uygulama içi ayar ve sistem izni birlikte açık mı? */
    fun isReady(context: Context): Boolean =
        Store.getNotifEnabled(context) && canPost(context)

    /**
     * v7.88 — Bildirimi güncel duruma göre gösterir/tazeler.
     *
     * Gösterim iki yoldan olabiliyor:
     *   · Sayaç çalışıyorsa [SayacServisi] ön plan bildirimi olarak tutar
     *     (sistem kaldıramaz, süreç öldürülmez)
     *   · Duraklatılmışsa sıradan bildirim yeter
     */
    fun show(context: Context) {
        val bildirim = olustur(context)
        if (bildirim == null) {
            cancel(context)
            SayacServisi.esitle(context)
            return
        }

        // v7.92: ÖNCE servisi eşitle, SONRA bildirimi gönder.
        //
        // Ters sırada, servis durdurulurken bildirim panelden kalkabiliyordu
        // (servis kendi bildirimini bırakırken bizimkini de götürüyordu).
        // Bu sırayla son söz her zaman notify()'da — bildirim kesin kalır.
        SayacServisi.esitle(context)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, bildirim)
        } catch (_: Exception) {
        }
    }

    /**
     * Bildirimi kurar ve döndürür — göndermez.
     *
     * [SayacServisi] `startForeground` için hazır bir `Notification`
     * nesnesine ihtiyaç duyuyor; aynı kurulum iki yerde tekrarlanmasın
     * diye gösterimden ayrıldı.
     *
     * @return gösterilecek bildirim, gösterilmemeliyse null
     */
    fun olustur(context: Context): android.app.Notification? {
        if (!Store.getNotifEnabled(context)) return null
        // v7.86: "Mini zamanlayıcıyı göster" kapalıysa canlı bildirim çıkmaz.
        // Sayaç yine de arka planda çalışmaya devam eder; yalnızca sürekli
        // bildirim gösterilmez.
        if (!SayacAyar.miniGoster(context)) return null
        createChannel(context)

        val running = TimerEngine.isRunning(context)
        val countdown = TimerEngine.mode(context) == TimerEngine.MODE_DOWN
        val value = TimerEngine.displayMs(context)

        // Sayaç hiç başlamadıysa bildirim gösterme
        if (!running && ((countdown && value >= TimerEngine.totalMs(context)) ||
                (!countdown && value == 0L))
        ) {
            return null
        }

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TIMER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun action(act: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context, code,
            Intent(context, TimerActionReceiver::class.java).apply { action = act },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (countdown) {
            context.getString(R.string.tn_focus_title)
        } else {
            context.getString(R.string.tn_watch_title)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // v10.3 · B19: durum çubuğu ikonu artık canlı — geri
            // sayımda kalan, kronometrede geçen dakikayı taşır.
            // Tazelenme zinciri (uyumluluk modu) ikonu da günceller.
            .setSmallIcon(SayacIkon.ikonCompat(SayacIkon.gosterilecekSayi(value, countdown)))
            .setContentTitle(title)
            .setContentIntent(open)
            // v7.93: uyumluluk modunda ongoing KULLANILMAZ.
            // Samsung ongoing+kronometreli bildirimleri "Canlı bildirimler"
            // alanına yönlendirip normal listeden çıkarıyor; uygulama o
            // alana kabul edilmezse bildirim hiçbir yerde görünmüyor.
            .setOngoing(running && !SayacAyar.uyumlulukModu(context))
            // v7.90 KRİTİK: setSilent(true) KALDIRILDI.
            //
            // ── Neden canlı bildirim görünmüyordu ──
            // `setSilent(true)` yalnızca sesi kapatmıyor; Android bu bayrağı
            // gören bildirimi **kanal önemi ne olursa olsun** panelin
            // "Sessiz bildirimler" bölümüne indiriyor. Samsung One UI'da bu
            // bölüm varsayılan olarak katlanmış geliyor, dolayısıyla sayaç
            // hiç görünmüyordu. Bitiş bildiriminde bu bayrak olmadığı için
            // o görünüyordu — kullanıcının tarif ettiği tablo tam buydu.
            //
            // Sessizlik zaten kanal düzeyinde sağlanıyor: kanalda
            // setSound(null) + enableVibration(false) var. Yani bu bayrak
            // olmadan da bildirim ses çıkarmıyor.
            //
            // `setOnlyAlertOnce` korunuyor: her tazelemede yeniden
            // "dikkat çekmesin", sadece ilk gösterimde belirsin.
            .setOnlyAlertOnce(true)
            // v7.93: CATEGORY_STOPWATCH bazı arayüzlerde bildirimi özel
            // "canlı" alana yönlendiriyor. Uyumluluk modunda kategori
            // verilmiyor — sıradan bildirim gibi davranılsın.
            .apply {
                if (!SayacAyar.uyumlulukModu(context)) {
                    setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                }
            }
            // v7.89: LOW iken panelde "sessiz" bölümüne düşüyordu
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Sayaç bildirimi kaydırılarak kapatılamasın; kontrol düğmeleri
            // her zaman elinin altında kalsın
            .setAutoCancel(false)
            // v7.91 ASIL DÜZELTME — Android 12+ (API 31) davranışı:
            //
            // Ön plan servisiyle gösterilen bildirimler varsayılan olarak
            // FOREGROUND_SERVICE_DEFERRED kabul edilir ve sistem bunları
            // **10 saniye geciktirir**. Amaç, kısa süreli servislerin
            // panelde titremesini önlemek.
            //
            // Bizim durumumuzda felakete yol açıyordu: bildirim her 2
            // saniyede bir tazelendiği için 10 saniyelik erteleme penceresi
            // sürekli baştan başlıyor ve bildirim **hiçbir zaman**
            // gösterilmiyordu. Bitiş bildirimi ön plan servisine bağlı
            // olmadığı için etkilenmiyordu — kullanıcının tarif ettiği
            // "biri geliyor, diğeri gelmiyor" tablosunun sebebi buydu.
            //
            // IMMEDIATE ile bildirim anında görünür.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // v10.4 · A5: oturum etiketi varsa alt metinde taşınır.
        // Başlığa karışmaz: uyumluluk/duraklatma dalları başlığı süreye
        // ayırdığı için etiket oraya sığmaz; subText her dalda görünür.
        SayacAyar.etiketAltMetin(context).takeIf { it.isNotBlank() }?.let {
            builder.setSubText(it)
        }

        // v10.7 · A6: zincir koşarken adım bilgisi geri sayım metnine
        // eklenir ("⛓ 💪 Çalış · 3/16"). Üç geri sayım dalının da
        // sonuna gelir; kronometreye dokunmaz.
        val zincirEk: String = run {
            if (!SayacZincir.kosuyor(context)) {
                ""
            } else {
                SayacZincir.aktif(context)?.let { z ->
                    val adim = SayacZincir.adim(context)
                    val (n, toplam) = SayacZincir.kacinciAdim(z, adim)
                    val evre = SayacZincir.adimdaki(z, adim)
                    " · " + context.getString(
                        R.string.zk_ust, "${evre.emoji} ${evre.ad}", n, toplam
                    )
                } ?: ""
            }
        }

        val uyumluluk = SayacAyar.uyumlulukModu(context)

        if (running && uyumluluk) {
            // ── v7.93 UYUMLULUK MODU ──
            // Duraklatılmış bildirimle YAPISAL OLARAK AYNI: kronometre yok,
            // ongoing yok. Kanıt: kullanıcının cihazında duraklatılmış
            // bildirim görünüyordu, çalışan görünmüyordu.
            //
            // Süre başlıkta düz metin olarak yazılıyor ve periyodik
            // tazeleniyor. Alt satırdaki bitiş saati hiç bayatlamaz —
            // tazeleme gecikse bile doğru bilgi ekranda kalır.
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
            builder.setContentTitle(
                if (countdown) TimerEngine.format(value) else title
            )
            builder.setContentText(
                if (countdown) {
                    val bitis = System.currentTimeMillis() + value
                    context.getString(
                        R.string.tn_ozet,
                        (TimerEngine.totalMs(context) / 60_000L).toInt(),
                        android.text.format.DateFormat.getTimeFormat(context)
                            .format(java.util.Date(bitis))
                    ) + zincirEk
                } else {
                    context.getString(R.string.tn_running)
                }
            )
        } else if (running) {
            // Sistem kendi sayar: geri sayımda azalır, kronometrede artar
            val base = if (countdown) {
                SystemClock.elapsedRealtime() + value
            } else {
                SystemClock.elapsedRealtime() - value
            }
            builder.setUsesChronometer(true)
                .setWhen(base)
                .setShowWhen(true)
            if (Build.VERSION.SDK_INT >= 24 && countdown) {
                builder.setChronometerCountDown(true)
            }
            builder.setContentText(
                if (countdown) {
                    val bitis = System.currentTimeMillis() + value
                    context.getString(
                        R.string.tn_ozet,
                        (TimerEngine.totalMs(context) / 60_000L).toInt(),
                        android.text.format.DateFormat.getTimeFormat(context)
                            .format(java.util.Date(bitis))
                    ) + zincirEk
                } else {
                    context.getString(R.string.tn_running)
                }
            )
        } else {
            // Duraklatıldı: sistem sayacı durdurulur, değer metne yazılır
            builder.setUsesChronometer(false)
            builder.setContentTitle(
                if (countdown) TimerEngine.format(value) else title
            )
            builder.setContentText(
                if (countdown) {
                    context.getString(
                        R.string.tn_ozet_duraklatildi,
                        (TimerEngine.totalMs(context) / 60_000L).toInt()
                    ) + zincirEk
                } else {
                    context.getString(R.string.tn_paused, TimerEngine.format(value))
                }
            )
        }

        // Geri sayımda ilerleme çubuğu
        if (countdown) {
            val total = TimerEngine.totalMs(context).coerceAtLeast(1L)
            val done = ((total - value) * 100 / total).toInt().coerceIn(0, 100)
            builder.setProgress(100, done, false)
        }

        // v7.87: iki düğme — ekran görüntüsündeki düzen.
        // Eskiden üç düğme vardı (Duraklat/Sıfırla/Kapat); üçü birden
        // dar bildirim alanında sıkışıyor ve etiketleri kırpılıyordu.
        // "Sıfırla" ile "Kapat" pratikte aynı sonucu veriyordu
        // (sayaç durur, bildirim kalkar) — tek "İptal et"te birleştirildi.
        builder.addAction(0, context.getString(R.string.tn_iptal), action(ACTION_STOP, 3))
        builder.addAction(
            0,
            context.getString(if (running) R.string.tn_pause else R.string.tn_resume),
            action(ACTION_TOGGLE, 1)
        )
        // v10.2 · A2: çalışan geri sayıma "+1 dk" — en sık istenen
        // mikro müdahale; sayacı açmadan uzatma.
        if (running && countdown) {
            val uzat = PendingIntent.getBroadcast(
                context, 22,
                Intent(context, TimerActionReceiver::class.java).apply {
                    action = TimerActionReceiver.ACTION_UZAT
                    putExtra(TimerActionReceiver.EXTRA_UZAT_MS, 60_000L)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, context.getString(R.string.tn_uzat_bir), uzat)
        }

        return builder.build()
    }

    fun cancel(context: Context) {
        // v7.88: önce servisi indir — ön plan bildirimi servis ayakta
        // kaldığı sürece sistem tarafından geri getirilir
        try {
            if (SayacServisi.ayakta) {
                context.startService(
                    Intent(context, SayacServisi::class.java).apply {
                        action = SayacServisi.EYLEM_DURDUR
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("TimerNotifier", "Servis durdurulamadı", e)
        }
        try {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID)
        } catch (_: Exception) {
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val yonetici = context.getSystemService(NotificationManager::class.java) ?: return

        // Eski düşük önemli kanalı temizle (bkz. CHANNEL_ID açıklaması)
        runCatching { yonetici.deleteNotificationChannel(ESKI_CHANNEL) }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.tn_channel),
            // DEFAULT: panelde normal bölümde görünür. Ses/titreşim yine
            // kapalı — sürekli sayan bir bildirimin uyarı vermesi gerekmiyor.
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            description = context.getString(R.string.tn_channel_desc)
        }
        yonetici.createNotificationChannel(channel)
    }
}
