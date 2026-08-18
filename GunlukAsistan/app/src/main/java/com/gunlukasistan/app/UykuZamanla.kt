package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * v10.9 — Gün çerçevesi alarm kurulumu.
 *
 * ── Neden ayrı sınıf ──
 * [BildirimZamanlayici] "yaklaşık tekrarlı" iki tur kurar (pil dostu,
 * dakikalarca kayabilir). Uyku kapılarında kullanıcı saati kendisi
 * seçiyor — "07:00" dediyse 07:00'da gelmeli. Bu yüzden burada
 * `setExactAndAllowWhileIdle` + kendini yeniden kuran zincir var.
 *
 * ── Zincir ──
 * Alarm tek atımlık kurulur; kapı işlenince [kur] yeniden çağrılır
 * ve bir SONRAKİ güne kayar. BootReceiver / App.kt da her fırsatta
 * [kur] dediği için zincir koptuğunda kendini onarır.
 *
 * ── API 31+ ──
 * Tam alarm izni yoksa `setAndAllowWhileIdle`'a düşülür
 * ([TimerAlarm] ile aynı örüntü); uygulama çökmez.
 */
object UykuZamanla {

    // PendingIntent istek kodları — proje geneli çakışma denetiminden geçti.
    private const val REQ_SABAH = 4821
    private const val REQ_AKSAM = 4822
    private const val REQ_SABAH_TEKRAR = 4823
    private const val REQ_AKSAM_TEKRAR = 4824

    private fun pending(context: Context, eylem: String, kod: Int): PendingIntent {
        val niyet = Intent(context, UykuAksiyonReceiver::class.java).apply { action = eylem }
        return PendingIntent.getBroadcast(
            context, kod, niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Tek atımlık tam (veya izin yoksa yaklaşık) alarm kurar. */
    private fun kurTek(context: Context, hedefMs: Long, eylem: String, kod: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val p = pending(context, eylem, kod)
        am.cancel(p)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hedefMs, p)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hedefMs, p)
        }
    }

    private fun iptalTek(context: Context, eylem: String, kod: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(pending(context, eylem, kod))
    }

    // ═══════════════════════════════════════════════════════════════
    // ANA KURULUM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sabah ve akşam kapılarının bir sonraki alarmını kurar.
     *
     * Çerçeve kapalıysa — ya da uygulama bildirimleri kapalıysa —
     * her şeyi iptal eder (bu çağrı "tek doğruluk noktası"dır:
     * ayar değiştiren her yol buradan geçer). İç hata yutulur;
     * çağıranı asla düşürmez.
     */
    fun kur(context: Context) {
        try {
            if (!UykuCerceve.acik(context) || !Store.getNotifEnabled(context)) {
                iptalHepsi(context)
                return
            }
            val simdi = System.currentTimeMillis()
            kurTek(
                context,
                UykuCerceve.sonrakiAlarm(simdi, UykuCerceve.sabahDk(context)),
                UykuAksiyonReceiver.ACTION_SABAH, REQ_SABAH
            )
            kurTek(
                context,
                UykuCerceve.sonrakiAlarm(simdi, UykuCerceve.aksamDk(context)),
                UykuAksiyonReceiver.ACTION_AKSAM, REQ_AKSAM
            )
        } catch (e: Exception) {
            android.util.Log.w("UykuZamanla", "Uyku alarmları kurulamadı", e)
        }
    }

    /** Cevap gelmeyen kapının yeniden sorgu alarmını kurar. */
    fun tekrarKur(context: Context, sabahMi: Boolean) {
        try {
            val dk = if (sabahMi) {
                UykuCerceve.tekrarDkSabah(context)
            } else {
                UykuCerceve.tekrarDkAksam(context)
            }
            val hedef = System.currentTimeMillis() + dk * 60_000L
            if (sabahMi) {
                kurTek(context, hedef, UykuAksiyonReceiver.ACTION_SABAH_TEKRAR, REQ_SABAH_TEKRAR)
            } else {
                kurTek(context, hedef, UykuAksiyonReceiver.ACTION_AKSAM_TEKRAR, REQ_AKSAM_TEKRAR)
            }
        } catch (e: Exception) {
            android.util.Log.w("UykuZamanla", "Tekrar alarmı kurulamadı", e)
        }
    }

    /** Cevap gelen kapının bekleyen tekrarını söndürür. */
    fun tekrarIptal(context: Context, sabahMi: Boolean) {
        try {
            if (sabahMi) {
                iptalTek(context, UykuAksiyonReceiver.ACTION_SABAH_TEKRAR, REQ_SABAH_TEKRAR)
            } else {
                iptalTek(context, UykuAksiyonReceiver.ACTION_AKSAM_TEKRAR, REQ_AKSAM_TEKRAR)
            }
        } catch (e: Exception) {
            android.util.Log.w("UykuZamanla", "Tekrar iptal edilemedi", e)
        }
    }

    /** Dört alarmı da siler — çerçeve kapanınca iz kalmasın. */
    fun iptalHepsi(context: Context) {
        try {
            iptalTek(context, UykuAksiyonReceiver.ACTION_SABAH, REQ_SABAH)
            iptalTek(context, UykuAksiyonReceiver.ACTION_AKSAM, REQ_AKSAM)
            iptalTek(context, UykuAksiyonReceiver.ACTION_SABAH_TEKRAR, REQ_SABAH_TEKRAR)
            iptalTek(context, UykuAksiyonReceiver.ACTION_AKSAM_TEKRAR, REQ_AKSAM_TEKRAR)
        } catch (e: Exception) {
            android.util.Log.w("UykuZamanla", "Uyku alarmları silinemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BİLDİRİM KANALLARI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Üç kanalı idempotent kurar (var olan kanala dokunulmaz —
     * Android'de kanal önemi kurulduktan sonra değiştirilemez,
     * bu yüzden sessiz/sesli ayrımı iki ayrı kanalla yapılır).
     */
    fun kanallariKur(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(UykuCerceve.KANAL_SABAH_SESSIZ) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        UykuCerceve.KANAL_SABAH_SESSIZ,
                        context.getString(R.string.uy_kanal_sabah_sessiz),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        enableVibration(false)
                        setSound(null, null)
                    }
                )
            }
            if (nm.getNotificationChannel(UykuCerceve.KANAL_SABAH_SESLI) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        UykuCerceve.KANAL_SABAH_SESLI,
                        context.getString(R.string.uy_kanal_sabah_sesli),
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
            if (nm.getNotificationChannel(UykuCerceve.KANAL_AKSAM) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        UykuCerceve.KANAL_AKSAM,
                        context.getString(R.string.uy_kanal_aksam),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("UykuZamanla", "Uyku kanalları kurulamadı", e)
        }
    }
}
