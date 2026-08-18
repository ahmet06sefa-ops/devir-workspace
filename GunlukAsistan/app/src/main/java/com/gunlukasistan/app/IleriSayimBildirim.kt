package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * v10.41 — Kullanıcı maddesi #1: ileri sayımın canlı bildirimi.
 *
 * ── Şikayet ──
 * "Zamanlayıcının ileri kısmı bildirimlerde çıkmıyor; durdurma,
 *  bekletme vb. aksiyonlar yok."
 *
 * ── Çözüm ──
 * Oturum açıkken kalıcı (ongoing) bildirim:
 * · Çalışırken [setUsesChronometer] ile saniye saniye kendini günceller
 *   (uygulama hiç tik harcamaz — bildirimi sistem çizer).
 * · Aksiyonlar: Bekle ⇄ Devam ve Bitir — [IleriSayimReceiver] işler.
 * · Kanal LOW önem: ses/vızıltı yok, yalnız panelde durur.
 *
 * İzin yoksa sessizce çıkar ([TimerNotifier.canPost]).
 */
object IleriSayimBildirim {

    private const val KANAL = "ileri_sayim_canli_v1"
    private const val NOTIF_ID = 4730

    const val ACTION_BEKLET = "com.gunlukasistan.app.ILERI_BEKLET"
    const val ACTION_BITIR = "com.gunlukasistan.app.ILERI_BITIR"

    /** "MM:SS", saat varsa "H:MM:SS" (saf — birim testli). */
    fun formatSure(ms: Long): String {
        val toplam = (ms / 1000L).coerceAtLeast(0L)
        val s = toplam % 60L
        val d = (toplam / 60L) % 60L
        val h = toplam / 3600L
        return if (h > 0) "%d:%02d:%02d".format(h, d, s) else "%02d:%02d".format(d, s)
    }

    /** Oturum varsa bildirimi kur/güncelle, yoksa kaldır. */
    fun tazele(c: Context) {
        if (!TimerNotifier.canPost(c)) return
        val d = IleriSayim.durum(c)
        if (!IleriSayim.oturumVarMi(d)) {
            gizle(c)
            return
        }
        runCatching {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, kur(c, d))
        }
    }

    fun gizle(c: Context) {
        runCatching { NotificationManagerCompat.from(c).cancel(NOTIF_ID) }
    }

    private fun kur(c: Context, d: IleriSayim.Durum): android.app.Notification {
        kanalKur(c)
        val simdi = System.currentTimeMillis()
        val gecen = IleriSayim.gecenMs(d, simdi)
        val ad = IleriSayim.ad(c)

        val acma = PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun yayin(eylem: String, kod: Int): PendingIntent = PendingIntent.getBroadcast(
            c, kod,
            Intent(eylem).setPackage(c.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val b = NotificationCompat.Builder(c, KANAL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(
                if (ad.isBlank()) c.getString(R.string.w41_ileri_bild_baslik)
                else c.getString(R.string.w41_ileri_bild_baslik_adli, ad)
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(acma)

        if (d.calisiyor) {
            // Sistem kronometresi kendisi işler — uygulama tik harcamaz
            b.setWhen(simdi - gecen)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
                .addAction(0, c.getString(R.string.w41_ileri_bekle), yayin(ACTION_BEKLET, 1))
        } else {
            b.setContentText(c.getString(R.string.w41_ileri_beklemede, formatSure(gecen)))
                .addAction(0, c.getString(R.string.w41_ileri_devam), yayin(ACTION_BEKLET, 1))
        }
        b.addAction(0, c.getString(R.string.w41_ileri_bitir), yayin(ACTION_BITIR, 2))
        return b.build()
    }

    private fun kanalKur(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                KANAL, c.getString(R.string.w41_ileri_kanal),
                NotificationManager.IMPORTANCE_LOW
            )
            runCatching {
                (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(kanal)
            }
        }
    }
}
