package com.gunlukasistan.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * v7.88 — Sayaç ön plan servisi.
 *
 * ── Sorun ──
 * Geri sayım çalışırken bildirim panelinde sayaç görünmüyordu.
 *
 * Sebep: bildirim sıradan bir `NotificationCompat` olarak gönderiliyordu.
 * Uygulama arka plana atılınca Android süreci istediği an öldürebiliyor;
 * süreç ölünce `setOngoing(true)` olsa bile bildirim düşebiliyor. Ayrıca
 * hiçbir bileşen "ben çalışıyorum" demediği için sistem uygulamayı
 * tamamen boşta sayıyordu.
 *
 * ── Çözüm ──
 * Sayaç çalıştığı sürece bir ön plan servisi ayakta duruyor. Ön plan
 * servisinin bildirimi sistem tarafından **kaldırılamaz**; süreç de
 * öncelikli hâle geldiği için öldürülmüyor.
 *
 * ── Neden `specialUse` türü ──
 * Android 14 (targetSdk 34) ön plan servisi için tür zorunlu kılıyor.
 * Mevcut türler arasında zamanlayıcıya uyan yok:
 *   · `shortService` 3 dakikayla sınırlı — 30 dakikalık sayaç için olmaz
 *   · `mediaPlayback`/`location` vb. yaptığımız işi tanımlamıyor
 * Geriye `specialUse` kalıyor; manifest'te alt tür açıklamasıyla birlikte
 * bildiriliyor.
 *
 * ── Sayaç mantığı burada DEĞİL ──
 * Servis yalnızca bildirimi ayakta tutar. Zamanın kendisi [TimerEngine]
 * içinde duvar saatiyle hesaplanır, bitiş [TimerAlarm] ile alarma bağlıdır.
 * Böylece servis herhangi bir sebeple ölse bile sayaç doğru kalır ve
 * bitiş bildirimi yine gelir.
 */
class SayacServisi : Service() {

    companion object {
        private const val TAG = "SayacServisi"

        const val EYLEM_BASLAT = "sayac.servis.baslat"
        const val EYLEM_DURDUR = "sayac.servis.durdur"
        const val EYLEM_TAZELE = "sayac.servis.tazele"

        /** Servis şu an ayakta mı — gereksiz başlatma çağrısı yapılmasın. */
        @Volatile
        var ayakta = false
            private set

        /**
         * Sayaç durumuna göre servisi başlatır ya da durdurur.
         *
         * [TimerEngine] her durum değişiminde bunu çağırır; çağıranın
         * servisin ayakta olup olmadığını bilmesine gerek yok.
         */
        fun esitle(context: Context) {
            try {
                val calisiyor = TimerEngine.isRunning(context)
                // v7.92: servis artık isteğe bağlı ve varsayılan KAPALI.
                //
                // Samsung One UI'da ön plan servisinin sahip olduğu bildirim
                // panelde görünmüyordu; sistem onu "arka planda çalışan
                // uygulamalar" grubuna katlıyordu. Kullanıcının kanıtı:
                // duraklatınca (sıradan notify) görünüyor, çalışırken
                // (servis) görünmüyordu.
                //
                // Servis kapalıyken bildirim tamamen normal notify() ile
                // gönderiliyor — görünürlük garanti. Sayacın doğruluğu
                // etkilenmiyor: süre TimerEngine'de duvar saatiyle,
                // bitiş TimerAlarm ile kesin alarma bağlı.
                val gosterilsin = calisiyor &&
                    SayacAyar.miniGoster(context) &&
                    SayacAyar.onPlanServisi(context)

                // v7.91: yalnızca DURUM DEĞİŞTİĞİNDE servise dokun.
                //
                // Eskiden her bildirim tazelemesinde (2 sn'de bir)
                // startService çağrılıyordu. Android 12+ sık servis
                // başlatmayı kısıtlıyor ve bir süre sonra çağrıları
                // sessizce yok sayıyor; servis de bildirimle birlikte
                // düşüyordu.
                when {
                    gosterilsin && !ayakta -> gonder(context, EYLEM_BASLAT, onPlan = true)
                    !gosterilsin && ayakta -> gonder(context, EYLEM_DURDUR, onPlan = false)
                    // Zaten doğru durumda — hiçbir şey yapma.
                    // Bildirim tazelemesi TimerNotifier.show() içinde
                    // doğrudan notify() ile yapılıyor, servise gerek yok.
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Servis eşitlenemedi", e)
            }
        }

        private fun gonder(context: Context, eylem: String, onPlan: Boolean) {
            val niyet = Intent(context, SayacServisi::class.java).apply { action = eylem }
            try {
                if (onPlan && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(niyet)
                } else {
                    context.startService(niyet)
                }
            } catch (e: Exception) {
                // Android 12+ arka plandan servis başlatmayı kısıtlayabilir.
                // Bu ölümcül değil: bildirim yine de normal yoldan gönderilir.
                android.util.Log.w(TAG, "Servis başlatılamadı", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            EYLEM_DURDUR -> {
                durdur()
                return START_NOT_STICKY
            }
            else -> {
                onPlanaAl()
                // v7.89: bildirimi düzenli tazele. Chronometer sistem
                // tarafından sayılıyor ama "30 d / 13:26" satırı ve düğme
                // etiketleri sabit metin — duraklat/devam sonrası güncel
                // kalmaları için periyodik yenileme gerekiyor.
                tazelemeyiKur()
            }
        }
        return START_STICKY
    }

    private val tazeleyici = android.os.Handler(android.os.Looper.getMainLooper())

    private val tazelemeIsi = object : Runnable {
        override fun run() {
            if (!ayakta) return
            if (!TimerEngine.isRunning(applicationContext)) {
                durdur()
                return
            }
            runCatching {
                TimerNotifier.olustur(applicationContext)?.let {
                    androidx.core.app.NotificationManagerCompat
                        .from(applicationContext)
                        .notify(TimerNotifier.NOTIF_ID, it)
                }
            }
            // v7.93: uyumluluk modunda kronometre yok — süre düz metin
            // olduğu için daha sık tazelenmeli. Normal modda sistem zaten
            // sayıyor, seyrek tazeleme yeterli.
            tazeleyici.postDelayed(
                this,
                if (SayacAyar.uyumlulukModu(applicationContext)) 10_000L else 30_000L
            )
        }
    }

    private fun tazelemeyiKur() {
        tazeleyici.removeCallbacks(tazelemeIsi)
        tazeleyici.postDelayed(tazelemeIsi, 30_000L)
    }

    /**
     * Bildirimi ön plana alır.
     *
     * `startForeground` çağrısı Android 12+ üzerinde başarısız olabilir
     * (arka plandan başlatma kısıtı). Hata yakalanıyor ve servis sessizce
     * kapanıyor — bildirim yine [TimerNotifier] üzerinden gösteriliyor,
     * yalnızca "kaldırılamaz" garantisi olmuyor.
     */
    private fun onPlanaAl() {
        val bildirim = TimerNotifier.olustur(this)
        if (bildirim == null) {
            // Gösterilecek bir şey yok (sayaç durmuş ya da mini kapalı)
            durdur()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    TimerNotifier.NOTIF_ID,
                    bildirim,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(TimerNotifier.NOTIF_ID, bildirim)
            }
            ayakta = true
        } catch (e: Exception) {
            // v7.89 KRİTİK DÜZELTME:
            // Eskiden burada durdur() çağrılıyordu; o da
            // stopForeground(STOP_FOREGROUND_REMOVE) ile **bildirimi
            // siliyordu**. Yani servis başlatılamadığı her durumda
            // (Android 12+ arka plan kısıtı, üretici pil politikası)
            // kullanıcı sayacı hiç göremiyordu.
            //
            // Artık servis sessizce çekiliyor ama bildirim normal yoldan
            // gönderiliyor. "Kaldırılamaz" garantisi kayboluyor, görünürlük
            // korunuyor — ikisinden biri seçilecekse görünürlük önemli.
            android.util.Log.w(TAG, "Ön plana alınamadı, normal bildirime düşülüyor", e)
            ayakta = false
            runCatching {
                androidx.core.app.NotificationManagerCompat.from(this)
                    .notify(TimerNotifier.NOTIF_ID, bildirim)
            }
            runCatching { stopSelf() }
        }
    }

    private fun durdur() {
        ayakta = false
        runCatching { tazeleyici.removeCallbacks(tazelemeIsi) }
        try {
            // v7.92: REMOVE değil DETACH.
            //
            // REMOVE bildirimi siliyordu. Servis kapatıldığında (ya da
            // kullanıcı ön plan servisini devre dışı bıraktığında) sayaç
            // hâlâ çalışıyor olabilir; bildirimin kaybolmaması gerekir.
            // DETACH bildirimi servisten ayırır ama panelde bırakır;
            // gerçek silme [TimerNotifier.cancel] işi.
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ön plandan çıkılamadı", e)
        }
        runCatching { stopSelf() }
    }

    override fun onDestroy() {
        super.onDestroy()
        ayakta = false
        runCatching { tazeleyici.removeCallbacks(tazelemeIsi) }
    }

    /**
     * Kullanıcı uygulamayı son kullanılanlardan kaydırırsa.
     *
     * Sayaç çalışmaya devam etmeli — servis kendini kapatmıyor. Ama
     * sayaç durmuşsa artık ayakta kalmasının anlamı yok.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!TimerEngine.isRunning(this)) durdur()
    }
}
