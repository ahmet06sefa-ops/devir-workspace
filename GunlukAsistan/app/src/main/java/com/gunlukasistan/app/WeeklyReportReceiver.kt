package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * Her pazar akşamı haftalık çalışma raporunu bildirim olarak gönderir.
 */
class WeeklyReportReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "haftalik_rapor_v1"

        /** Bir sonraki Pazar 20:00'in zaman damgasını döndürür. */
        fun nextSundayEvening(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayDow = cal.get(Calendar.DAY_OF_WEEK)
            var add = (Calendar.SUNDAY - todayDow + 7) % 7
            if (add == 0 && cal.timeInMillis <= System.currentTimeMillis()) add = 7
            cal.add(Calendar.DAY_OF_YEAR, add)
            return cal.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // v10.3 · B24: rapor artık sessiz saat haritasının parçası.
        // Sessiz saatteysek bu haftalık raporu atlıyoruz; bir sonraki
        // hafta yeniden kuruluyor (aşağıda). Bilinçli: gece 20:00'de
        // uyanık kullanıcıya zaten gelir, uyuyana biriktirmez.
        // v10.15 · C14: rapor artık kendi tür penceresine bakar;
        // tür penceresi tanımsızsa global karar aynen geçerli kalır.
        val sessizdeCakma = try {
            SessizTurler.susturMu(context, BildirimMerkezi.K_RAPOR)
        } catch (_: Exception) { false }
        if (Store.getNotifEnabled(context) && !sessizdeCakma) {
            createChannel(context)
            val (streak, best) = Store.streakInfo(context)
            val weekC = Store.weekCompletions(context)
            val weekF = Store.weekFocus(context)
            val weekQ = Store.weekQuestions(context)

            // v7.84: rapor artık öğrenme verilerini de içeriyor.
            // Eskiden yalnızca madde/soru/odak sayısı vardı; koç, müfredat,
            // hata defteri ve sözlük eklendikten sonra rapor eksik kalıyordu.
            val kisa = "Bu hafta $weekC madde, $weekQ soru, $weekF dk odak 🔥 " +
                "Serin $streak gün (rekor $best)."

            val text = buildString {
                append(kisa)
                try {
                    // Program ilerlemesi
                    if (Mufredat.secildiMi(context)) {
                        val il = Mufredat.ilerleme(context)
                        if (il.toplam > 0) {
                            append("\n\n📚 ")
                            append(Mufredat.programAdi(context))
                            append(": ").append(il.biten).append("/").append(il.toplam)
                            append(" (%").append(il.yuzde).append(")")
                            if (!il.bittiMi && il.aktifAd.isNotBlank()) {
                                append("\n▶ Sırada: ").append(il.aktifAd.take(40))
                            }
                        }
                    }

                    // Koç karnesi
                    if (Koc.acikMi(context)) {
                        val k = Koc.karne(context, 7)
                        append("\n\n🎓 Koç: ")
                        append(k.basariliGun).append("/").append(k.toplamGun)
                        append(" gün hedef tuttu")
                        if (k.borc > 0) append(" · ").append(k.borc).append(" dk borç")
                    }

                    // Hata defteri
                    val h = Hatalarim.ozet(context)
                    if (h.toplam > 0 || h.ogrenilen > 0) {
                        append("\n\n🎯 Hata defteri: ").append(h.toplam).append(" soru")
                        if (h.bugun > 0) append(" · bugün ").append(h.bugun).append(" tekrar")
                        if (h.ogrenilen > 0) {
                            append(" · ").append(h.ogrenilen).append(" öğrenildi")
                        }
                    }

                    // Sözlük
                    val sz = Sozluk.sayi(context)
                    if (sz > 0) append("\n\n📖 Sözlüğünde ").append(sz).append(" terim var.")
                } catch (e: Exception) {
                    android.util.Log.w("WeeklyReport", "Öğrenme özeti eklenemedi", e)
                }
                append("\n\nYeni haftaya bomba gibi gir! 💪")
            }

            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // v7.84: rapordan doğrudan hata defterine geçiş
            val hataAc = PendingIntent.getActivity(
                context, 1,
                Intent(context, HatalarimActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // v10.3 · B16: rapor artık grafikli kart taşıyor.
            // Son 7 günün tamamlanmış odak dakikaları çubuk grafik
            // olarak BigPictureStyle'a girer; üretim başarısız olursa
            // eski metin stiline düşer.
            val grafikStil: NotificationCompat.Style = run {
                val dakikalar = runCatching {
                    RaporGrafigi.gunlukOdakDakikalari(SureAnalizi.pomodorolar(context))
                }.getOrNull()
                val bmp = dakikalar?.let { d ->
                    runCatching { RaporGrafigi.olustur(d) }.getOrNull()?.let { cubuk ->
                        // v10.15 · C17: çubuk kartın altına ısı haritası
                        // (gün × saat dilimi) — üretimi bozulursa çubukla kal.
                        runCatching {
                            val m = RaporIsi.gunSaatMatrisi(SureAnalizi.pomodorolar(context))
                            RaporIsi.birlestir(cubuk, RaporIsi.olustur(m, RaporIsi.matrisMaks(m)))
                        }.getOrDefault(cubuk)
                    }
                }
                if (bmp != null) {
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bmp)
                        .setSummaryText(kisa)
                } else {
                    NotificationCompat.BigTextStyle().bigText(text)
                }
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("📬 Haftalık raporun hazır!")
                .setContentText(kisa)
                .setStyle(grafikStil)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .apply {
                    // Tekrar edilecek soru varsa kısayol göster
                    runCatching {
                        val bekleyen = Hatalarim.bugunkuSayi(context)
                        if (bekleyen > 0) {
                            addAction(
                                0,
                                context.getString(R.string.ht_tekrar_et, bekleyen),
                                hataAc
                            )
                        }
                    }
                }
                .build()
            try {
                NotificationManagerCompat.from(context).notify(99, notification)
            } catch (_: Exception) {
            }
        }
        // Gelecek haftanın raporunu şimdiden kur
        AlarmScheduler.scheduleWeeklyReport(context)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.weekly_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
