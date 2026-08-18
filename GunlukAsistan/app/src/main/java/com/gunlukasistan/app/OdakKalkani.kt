package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v10.12 · ULTRA-30 / D21 — Odak kalkanı (izinli).
 *
 * ── Ne yapar ──
 * Kullanıcı "kullanım erişimi" verdiyse ve kısıtlı uygulama listesi
 * doluysa, odak sayacı koşarken öne geçen uygulamayı 5 saniyede bir
 * denetler. Kısıtlı bir uygulama (ör. sosyal medya) açılırsa nazik bir
 * uyarı düşer: "Odaktan 12 dk kaldı — geri dön mü?". Düğme odak
 * ekranına döndürür.
 *
 * ── İzin ve sınır (dürüstlük notu) ──
 *   · İzin: PACKAGE_USAGE_STATS — sistem ayarlarından elle verilir;
 *     uygulama içinden verilemez. Veri cihazdan ÇIKMAZ.
 *   · Perde = yüksek öncelikli bildirimdir. Ekran üstü kaplama
 *     (SYSTEM_ALERT_WINDOW) istemiyoruz: hem kötüye kullanıma açık hem
 *     Play politikası ağır. Bu bilinçli bir sınırdır.
 *   · Gözcü süreç yaşarken çalışır; sistem süreci öldürürse sayaç
 *     alarmı yaşamaya devam eder ama kalkan susar — yeniden başlatmada
 *     geri gelir.
 *
 * ── Kaçamak sayacı ──
 * Her uyarı günlük kaçamak sayacına yazılır (ayarlardan görünür); aynı
 * uygulama için 2 dakikalık sakinleşme penceresi arka arkaya bildirim
 * yağmurunu önler. 3. kaçamaktan sonra metin sertleşir ama yine nazik
 * kalır — ceza yok, farkındalık var.
 */
object OdakKalkani {

    private const val PREF = "fo_odak_kalkani_v1"
    private const val K_ACIK = "acik"
    private const val K_PAKETLER = "paketler_json"
    private const val K_IHLAL_GUN = "ihlal_gun"
    private const val K_IHLAL_ADET = "ihlal_adet"

    const val NOTIF_ID = 4910
    const val REQ_DON = 4930
    const val CHANNEL_ID = "odak_kalkani_v1"

    /** Aynı uygulama için iki uyarı arası en az süre. */
    const val COOLDOWN_MS = 120_000L

    /** Öne geçen uygulama denetim aralığı. */
    private const val ARALIK_MS = 5_000L

    /** Geriye dönük olay tarama penceresi (son 12 sn). */
    private const val TARAMA_MS = 12_000L

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Ayarlar ----------------

    fun acik(c: Context): Boolean = prefs(c).getBoolean(K_ACIK, false)
    fun setAcik(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean(K_ACIK, v).apply()
        esitle(c)
    }

    fun paketler(c: Context): Set<String> =
        paketleriCoz(prefs(c).getString(K_PAKETLER, ""))

    fun setPaketler(c: Context, liste: Set<String>) {
        prefs(c).edit().putString(K_PAKETLER, paketleriBirlestir(liste)).apply()
        esitle(c)
    }

    // ---------------- Saf yardımcılar (birim testli) ----------------

    fun paketleriCoz(ham: String?): Set<String> =
        ham.orEmpty().split(',').map { it.trim() }.filter { it.length > 2 }.toSet()

    fun paketleriBirlestir(liste: Set<String>): String =
        liste.filter { it.length > 2 }.sorted().joinToString(",")

    /** Uyarı çıkmalı mı — tüm kapılar tek tabloda. */
    fun uyariGerekliMi(
        acik: Boolean,
        izinVar: Boolean,
        kosuyor: Boolean,
        molada: Boolean,
        kisitliMi: Boolean,
        simdiMs: Long,
        sonUyariMs: Long,
        cooldownMs: Long = COOLDOWN_MS
    ): Boolean =
        acik && izinVar && kosuyor && !molada && kisitliMi &&
            (simdiMs - sonUyariMs) >= cooldownMs

    /** Gün değiştiyse sayaç sıfırlanır. */
    fun bugunIhlal(kayitGun: String, bugun: String, adet: Int): Int =
        if (kayitGun == bugun) adet else 0

    fun gunAnahtari(ms: Long): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))

    // ---------------- İzin ----------------

    /**
     * Kullanım erişimi verilmiş mi: son dakikanın istatistiği boş
     * dönüyorsa izin yoktur (izinsiz çağrı her zaman boş liste verir).
     */
    fun izinVarMi(context: Context): Boolean = runCatching {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val simdi = System.currentTimeMillis()
        val liste = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, simdi - 60_000L, simdi)
        liste != null && liste.isNotEmpty()
    }.getOrDefault(false)

    fun uygulamaAdi(context: Context, paket: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(paket, 0)).toString()
    }.getOrDefault(paket)

    fun bugunkuIhlal(c: Context): Int {
        val p = prefs(c)
        return bugunIhlal(
            p.getString(K_IHLAL_GUN, "") ?: "",
            gunAnahtari(System.currentTimeMillis()),
            p.getInt(K_IHLAL_ADET, 0)
        )
    }

    private fun ihlalKaydet(c: Context): Int {
        val bugun = gunAnahtari(System.currentTimeMillis())
        val p = prefs(c)
        val yeni = bugunIhlal(p.getString(K_IHLAL_GUN, "") ?: "", bugun, p.getInt(K_IHLAL_ADET, 0)) + 1
        p.edit().putString(K_IHLAL_GUN, bugun).putInt(K_IHLAL_ADET, yeni).apply()
        return yeni
    }

    // ---------------- Gözcü ----------------

    private var uygContext: Context? = null
    private var gozcuCalisiyor = false
    private val sonUyari = HashMap<String, Long>()

    // Nesne <clinit> içinde Looper'a dokunulmaz — birim testler saf
    // fonksiyonları çağırdığında Handler kurulmasın diye lazy.
    private val guncelleyici by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    private val tur = object : Runnable {
        override fun run() {
            val c = uygContext
            if (c == null || !gozcuCalisiyor) return
            runCatching { kontrolEt(c) }
            if (gozcuCalisiyor) guncelleyici.postDelayed(this, ARALIK_MS)
        }
    }

    /** Odak sayacıyla aynı yerlerden çağrılır: başlat/duraklat/sıfırla/bitir. */
    fun esitle(context: Context) {
        runCatching {
            uygContext = context.applicationContext
            if (calismali(context)) gozcuBaslat() else gozcuDurdur()
        }
    }

    private fun calismali(c: Context): Boolean =
        acik(c) && TimerEngine.isRunning(c) &&
            !(Pomodoro.acikMi(c) && Pomodoro.molada(c)) &&
            paketler(c).isNotEmpty() && izinVarMi(c)

    private fun gozcuBaslat() {
        if (gozcuCalisiyor) return
        gozcuCalisiyor = true
        guncelleyici.removeCallbacks(tur)
        guncelleyici.postDelayed(tur, ARALIK_MS)
    }

    private fun gozcuDurdur() {
        gozcuCalisiyor = false
        runCatching { guncelleyici.removeCallbacks(tur) }
    }

    /** Öne geçen paketi tespit eder; kısıtlıysa uyarı atar. */
    private fun kontrolEt(c: Context) {
        if (!calismali(c)) {
            gozcuDurdur()
            return
        }
        val simdi = System.currentTimeMillis()
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val olaylar = usm.queryEvents(simdi - TARAMA_MS, simdi) ?: return
        val olay = UsageEvents.Event()
        var onPlan: String? = null
        while (olaylar.hasNextEvent()) {
            olaylar.getNextEvent(olay)
            // API 23+: öne alma olayı (ACTIVITY_RESUMED API 29 — minSdk 24 için bu doğru sabit)
            if (olay.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                onPlan = olay.packageName
            }
        }
        val paket = onPlan ?: return
        if (paket == c.packageName) return
        val kisitli = paketler(c).contains(paket)
        if (!uyariGerekliMi(acik(c), true, TimerEngine.isRunning(c),
                Pomodoro.acikMi(c) && Pomodoro.molada(c),
                kisitli, simdi, sonUyari[paket] ?: 0L)) {
            return
        }
        sonUyari[paket] = simdi
        val adet = ihlalKaydet(c)
        bildir(c, paket, adet)
    }

    // ---------------- Bildirim ----------------

    private fun bildir(c: Context, paket: String, adet: Int) {
        if (!Store.getNotifEnabled(c)) return
        kanal(c)
        val ad = uygulamaAdi(c, paket)
        val kalanDk = ((TimerEngine.remainingMs(c) + 30_000L) / 60_000L).toInt().coerceAtLeast(1)

        val donIntent = Intent(c, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TIMER)
        }
        val donPi = PendingIntent.getActivity(
            c, REQ_DON, donIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val metin = if (adet >= 3) {
            c.getString(R.string.fo_kalkan_metin_cok, adet, ad, kalanDk)
        } else {
            c.getString(R.string.fo_kalkan_metin, kalanDk)
        }

        val b = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(c.getString(R.string.fo_kalkan_baslik, ad))
            .setContentText(metin)
            .setStyle(NotificationCompat.BigTextStyle().bigText(metin))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(donPi)
            .addAction(0, c.getString(R.string.fo_kalkan_don), donPi)

        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, b.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS yok — kalkan sessizce susar (sayaç yine koşar)
        }
    }

    private fun kanal(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                c.getString(R.string.fo_kalkan_kanal),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = c.getString(R.string.fo_kalkan_kanal_aciklama) }
        )
    }
}
