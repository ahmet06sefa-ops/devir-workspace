package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * v10.42 — Kullanıcı maddeleri #5/#6: günlük plan bildirimlerinin ayarı.
 *
 * #5 "Sabah uyandığımda saat kaçta olursa olsun 'uyandın mı' bildirimi
 *    alayım ve o gün ne görev varsa göster."
 * #6 "Akşam uyku saatinden önce 'yarın ne yapmak istersin' diye sorsun."
 *
 * Saf mantık üstte (JVM testli), depo + alarm altta.
 */
object PlanAsistan {

    // ──────────────── Saf mantık (android YOK, JVM testli) ────────────────

    /** 1320 → "22:00" (saf). */
    fun dakikaYaz(dk: Int): String = "%02d:%02d".format(dk / 60, dk % 60)

    /**
     * Günün ilk ekran açılışı "uyanma" sayılır — gece 04:00 öncesi
     * kilit açmalar uyku ortası sayılıp elenir (saf).
     */
    fun sabahPenceresiMi(gunDakikasi: Int): Boolean = gunDakikasi >= 4 * 60

    /** "N görev bekliyor · bugün B · gecikmiş G" özet gövdesi (saf). */
    fun sabahOzet(toplam: Int, bugun: Int, gecikmis: Int): String = buildString {
        append(toplam).append(" görev bekliyor")
        if (bugun > 0) append(" · bugün ").append(bugun)
        if (gecikmis > 0) append(" · gecikmiş ").append(gecikmis)
    }

    /**
     * Akşam alarmının bir sonraki tetik anı: hedef dakika bugün
     * geçmemişse bugün, geçmişse yarın (saf — JVM Calendar).
     */
    fun sonrakiAksam(simdiMs: Long, hedefDk: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = simdiMs }
        val simdiDk = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, hedefDk / 60)
        cal.set(Calendar.MINUTE, hedefDk % 60)
        if (simdiDk >= hedefDk) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    // ──────────────── Depo ────────────────

    private const val PREF = "plan_asistan_v1"
    private const val K_SABAH = "sabah_acik"
    private const val K_AKSAM = "aksam_acik"
    private const val K_AKSAM_DK = "aksam_dk"
    private const val K_SON_SABAH = "son_sabah_gun"

    const val VARSAYILAN_AKSAM_DK = 22 * 60

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun sabahAcik(c: Context): Boolean = p(c).getBoolean(K_SABAH, true)
    fun sabahAcik(c: Context, v: Boolean) = p(c).edit().putBoolean(K_SABAH, v).apply()
    fun aksamAcik(c: Context): Boolean = p(c).getBoolean(K_AKSAM, true)
    fun aksamAcik(c: Context, v: Boolean) = p(c).edit().putBoolean(K_AKSAM, v).apply()
    fun aksamDk(c: Context): Int = p(c).getInt(K_AKSAM_DK, VARSAYILAN_AKSAM_DK)
    fun aksamDk(c: Context, v: Int) = p(c).edit().putInt(K_AKSAM_DK, v.coerceIn(0, 1439)).apply()

    /** Bugün sabah bildirimi gösterildi mi (günde 1 kez). */
    fun sabahGosterildiMi(c: Context, gunAnahtar: Int): Boolean =
        p(c).getInt(K_SON_SABAH, 0) == gunAnahtar

    fun sabahGosterildiIsle(c: Context, gunAnahtar: Int) =
        p(c).edit().putInt(K_SON_SABAH, gunAnahtar).apply()

    // ──────────────── Akşam alarmı (AlarmScheduler deseni) ────────────────

    const val ACTION_AKSAM = "com.gunlukasistan.app.AKSAM_PLAN"
    private const val REQ = 4741

    /**
     * Akşam alarmının PendingIntent'i.
     *
     * v11.13: Açık intent olarak `AksamReceiver` hedeflenir ve action
     * receiver'ın dinlediği `ACTION_AKSAM` (= "AKSAM_PLAN") ile kurulur.
     * `kur` ve `iptal` aynı `pi`'yi kullanır — böylece iptal, kurulan
     * alarmı birebir iptal eder (request kodu 0 yerine REQ tutarlılığı).
     */
    private fun pi(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, REQ,
        Intent(c, AksamReceiver::class.java).apply { action = ACTION_AKSAM },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Günlük akşam alarmını kurar; kapalıysa iptal eder (boot/açılışta çağrılır). */
    fun kur(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!aksamAcik(c)) {
            runCatching { am.cancel(pi(c)) }
            return
        }
        val at = sonrakiAksam(System.currentTimeMillis(), aksamDk(c))
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
            }
        }
    }

    /** Akşam alarmını iptal eder (ayar kapatılınca / kullanıcı iptal edince çağrılır). */
    fun iptal(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { am.cancel(pi(c)) }
    }
}
