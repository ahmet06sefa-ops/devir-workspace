package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * v10.42 — Kullanıcı maddeleri #5/#6: sabah ve akşam plan bildirimleri.
 *
 * Kanal "gunluk_plan_v1", DEFAULT önem — günde en fazla iki kez gelir,
 * sesli ama tekrarsızdır. İzin yoksa sessizce çıkar.
 */
object GunlukBildirim {

    private const val KANAL = "gunluk_plan_v1"
    private const val ID_SABAH = 4742
    private const val ID_AKSAM = 4743

    private fun kanalKur(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(
                        NotificationChannel(
                            KANAL, c.getString(R.string.w42_kanal),
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
            }
        }
    }

    private fun icerik(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 0, Intent(c, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * #5: "Uyandın mı?" + bugünün görev tablosu. [basliklar] en fazla 4
     * satır olarak InboxStyle'a düşer; gerisi özet metinde sayılır.
     */
    fun sabah(c: Context, toplam: Int, bugun: Int, gecikmis: Int, basliklar: List<String>) {
        if (!TimerNotifier.canPost(c)) return
        kanalKur(c)
        val govde = PlanAsistan.sabahOzet(toplam, bugun, gecikmis)
        val b = NotificationCompat.Builder(c, KANAL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(c.getString(R.string.w42_sabah_baslik))
            .setContentText(govde)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(icerik(c))
        if (basliklar.isNotEmpty()) {
            val stil = NotificationCompat.InboxStyle()
            basliklar.take(4).forEach { stil.addLine("• $it") }
            if (basliklar.size > 4) {
                stil.addLine(c.getString(R.string.w42_dahasi, basliklar.size - 4))
            }
            b.setStyle(stil)
        }
        runCatching { NotificationManagerCompat.from(c).notify(ID_SABAH, b.build()) }
    }

    /** #6: "Yarın ne yapmak istersin?" — iyi geceler öncesi plan daveti. */
    fun aksam(c: Context) {
        if (!TimerNotifier.canPost(c)) return
        kanalKur(c)
        val b = NotificationCompat.Builder(c, KANAL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(c.getString(R.string.w42_aksam_baslik))
            .setContentText(c.getString(R.string.w42_aksam_metin))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(icerik(c))
        runCatching { NotificationManagerCompat.from(c).notify(ID_AKSAM, b.build()) }
    }

    /** Bugüne ait görev ölçümleri (dueAt 0 = tarihsiz → birikmiş sayılır). */
    fun gorevOlcum(c: Context): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val gunBas = cal.timeInMillis
        val gunSon = gunBas + 86_400_000L
        var toplam = 0
        var bugun = 0
        var gecikmis = 0
        Store.loadTasks(c).filter { !it.done }.forEach { t ->
            toplam++
            when {
                t.dueAt in gunBas until gunSon -> bugun++
                t.dueAt in 1 until gunBas -> gecikmis++
            }
        }
        return Triple(toplam, bugun, gecikmis)
    }

    fun gunBasliklari(c: Context): List<String> =
        Store.loadTasks(c).filter { !it.done }.map { it.text }.take(8)
}
