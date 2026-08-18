package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator

/**
 * v11.13 — Bitiş sesi & titreşim merkez yöneticisi.
 *
 * Sayaç süresi dolduğunda çalan **döngülü alarm sesini ve titreşimi** tek
 * noktadan yönetir. v11.13'e kadar ses, `TimerActionReceiver.bitisSesiCal`
 * içinde yerel bir `MediaPlayer` ile başlatılıyor ve **ekran kapansa (güç
 * düğmesi) da susmuyordu**. Bu motor:
 *
 *  · Sesi başlattığı anda `Intent.ACTION_SCREEN_OFF` için dinamik bir
 *    yayın alıcısı kaydeder — **güç düğmesine basıldığı anda** ses ve
 *    titreşim anında susar (kullanıcı şikayetinin doğrudan çözümü).
 *  · `durdur()`: sesi durdurur/yayınlar, titreşimi iptal eder, alıcıyı
 *    serbest bırakır ve otomatik-durdurma zamanlayıcılarını temizler.
 *  · Yalnız tek oturum çalabilir; yeni bir bitiş eski sesi önce keser.
 *  · Otomatik süre (`SayacAyar.sesSureSn`) sonunda kendiliğinden susar.
 *
 * `susmaKarari` saf ve JVM test edilebilirdir: ne zaman susması gerektiğini
 * ortak mantıkla karara bağlar.
 */
object BitisSesMotoru {

    private var oynatici: MediaPlayer? = null
    private var rampaHandler: Handler? = null
    private var otodurdurHandler: Handler? = null
    private var otodurdurRun: Runnable? = null

    private var ekranKayitli: Context? = null
    private var ekranKapandiAlici: BroadcastReceiver? = null

    /** Ses şu an çalıyor mu (ekran/bildirim göstergesi için). */
    fun caliyorMu(): Boolean = oynatici != null

    /**
     * Ne zaman susulması gerektiği (saf karar).
     * Herhangi bir koşul doğruysa dur: güç düğmesi (ekran kapandı),
     * kullanıcı "durdur"a bastı ya da süre doldu.
     */
    fun susmaKarari(ekranKapandiMi: Boolean, kullaniciDurdurduMu: Boolean, sureDolduMu: Boolean): Boolean =
        ekranKapandiMi || kullaniciDurdurduMu || sureDolduMu

    /** Bitiş sesini + titreşimi başlatır ve güç düğmesi (ekran kapanınca) susturur. */
    fun cal(context: Context) {
        val ctx = context.applicationContext
        // Önce eski oturum tamamen susmalı
        durdur()

        // ── Güç düğmesi / ekran kapanması dinleyicisi ──
        // ACTION_SCREEN_OFF tam olarak güç düğmesine basıldığında tetiklenir.
        // Kullanıcı "güç tuşuyla alarmı durdur" ayarını kapattıysa saygı göster
        // (uygulamanın diğer alarm ekranlarıyla aynı davranış).
        if (SayacAyar.isKapatmaTusuyleAlarmDurdur(ctx)) {
            val alici = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    if (i?.action == Intent.ACTION_SCREEN_OFF) durdur()
                }
            }
            ekranKayitli = ctx
            ekranKapandiAlici = alici
            runCatching {
                val filtre = IntentFilter(Intent.ACTION_SCREEN_OFF)
                if (Build.VERSION.SDK_INT >= 33) {
                    ctx.registerReceiver(alici, filtre, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    ctx.registerReceiver(alici, filtre)
                }
            }
        }

        // ── Titreşim ──
        if (SayacAyar.titresimEtkinMi(ctx)) {
            runCatching {
                val desen = SayacAyar.desenDizisi(SayacAyar.titresimDeseni(ctx))
                val titresici = if (Build.VERSION.SDK_INT >= 31) {
                    (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? android.os.VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    titresici?.vibrate(
                        android.os.VibrationEffect.createWaveform(desen, 0),
                        SayacAyar.sesNiteligi()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    titresici?.vibrate(desen, 0)
                }
            }
        }

        // ── Ses ──
        if (!SayacAyar.sesCalinsinMi(ctx)) return
        val uri = SayacAyar.cozulmusSesUri(ctx) ?: return
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(SayacAyar.sesNiteligi())
            p.setDataSource(ctx, uri)
            p.isLooping = true

            if (SayacAyar.kademeliSes(ctx)) {
                // Sıfırdan başlayıp yükselt — ani gürültü yerine yumuşak uyarı
                p.setVolume(0.12f, 0.12f)
                var seviye = 0.12f
                val h = Handler(Looper.getMainLooper())
                rampaHandler = h
                val artir = object : Runnable {
                    override fun run() {
                        seviye = (seviye + 0.14f).coerceAtMost(1f)
                        runCatching { p.setVolume(seviye, seviye) }
                        if (seviye < 1f) h.postDelayed(this, 700)
                    }
                }
                h.postDelayed(artir, 700)
            }

            p.setOnPreparedListener { it.start() }
            p.prepareAsync()
            oynatici = p

            // Belirlenen süre sonunda kendiliğinden dur — sonsuza kadar çalmasın
            val h = Handler(Looper.getMainLooper())
            otodurdurHandler = h
            otodurdurRun = Runnable { durdur() }
            h.postDelayed(otodurdurRun!!, SayacAyar.sesSureSn(ctx) * 1000L)
        } catch (e: Exception) {
            android.util.Log.w("BitisSesMotoru", "Bitiş sesi çalınamadı", e)
            durdur()
        }
    }

    /** Ses ve titreşimi anında keser; dinleyiciyi ve zamanlayıcıları temizler. */
    fun durdur() {
        val ctx = ekranKayitli
        // Ekran dinleyicisini kaldır
        ekranKapandiAlici?.let { a ->
            runCatching { ctx?.unregisterReceiver(a) }
        }
        ekranKapandiAlici = null
        ekranKayitli = null

        // Zamanlayıcıları temizle
        rampaHandler?.removeCallbacksAndMessages(null); rampaHandler = null
        otodurdurHandler?.removeCallbacksAndMessages(null); otodurdurHandler = null
        otodurdurRun = null

        // Sesi durdur + serbest bırak
        val p = oynatici
        oynatici = null
        try { p?.stop() } catch (_: Exception) {}
        try { p?.release() } catch (_: Exception) {}

        // v11.13: bitişe eşlik eden ısrarlı alarm (ZorunluUyari) da sussun —
        // güç düğmesi "alarm ve sesler"in tamamını aynı anda keser.
        runCatching { ZorunluUyari.durdur() }

        // Titreşimi iptal et
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                (ctx?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? android.os.VibratorManager)?.defaultVibrator?.cancel()
            }
            @Suppress("DEPRECATION")
            (ctx?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        }
    }
}
