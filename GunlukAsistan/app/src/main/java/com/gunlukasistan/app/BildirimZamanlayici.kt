package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * v7.43 — Günlük bildirim kontrollerini zamanlar.
 *
 * Günde iki tur çalışır:
 *   · SABAH (09:00) — günlük kart, sınav sayacı, odak önerisi, aylık rapor
 *   · AKŞAM (19:00) — kart/quiz tekrarı, yarım ders, seri riski
 *
 * Neden iki tur? Sabah "bugün şunu yap" der, akşam "bunlar kaldı" hatırlatır.
 * Tek tur olsaydı ya çok erken ya çok geç olurdu.
 *
 * Alarmlar `setInexactRepeating` ile kurulur — pil dostu, birkaç dakika
 * sapma kabul edilebilir (bildirimler saniye hassasiyeti gerektirmez).
 */
class BildirimZamanlayici : BroadcastReceiver() {

    companion object {
        const val ACTION_SABAH = "com.gunlukasistan.app.BILDIRIM_SABAH"
        const val ACTION_AKSAM = "com.gunlukasistan.app.BILDIRIM_AKSAM"

        private const val KOD_SABAH = 8101
        private const val KOD_AKSAM = 8102

        private const val PREF = "bildirim_zaman_v1"
        private const val K_SABAH_SAAT = "sabah_saat"
        private const val K_AKSAM_SAAT = "aksam_saat"

        fun sabahSaati(context: Context): Int =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(K_SABAH_SAAT, 9)

        fun aksamSaati(context: Context): Int =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(K_AKSAM_SAAT, 19)

        fun setSaatler(context: Context, sabah: Int, aksam: Int) {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putInt(K_SABAH_SAAT, sabah.coerceIn(0, 23))
                .putInt(K_AKSAM_SAAT, aksam.coerceIn(0, 23))
                .apply()
            kur(context)
        }

        private fun pending(context: Context, eylem: String, kod: Int): PendingIntent {
            val intent = Intent(context, BildirimZamanlayici::class.java).apply {
                action = eylem
            }
            return PendingIntent.getBroadcast(
                context, kod, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Her iki turu da kurar. Açılışta ve ayar değişiminde çağrılır. */
        fun kur(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return

                listOf(
                    Triple(ACTION_SABAH, KOD_SABAH, sabahSaati(context)),
                    Triple(ACTION_AKSAM, KOD_AKSAM, aksamSaati(context))
                ).forEach { (eylem, kod, saat) ->
                    val hedef = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, saat)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    // Pil dostu: yaklaşık tekrarlama yeterli
                    am.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        hedef.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pending(context, eylem, kod)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("BildirimZamanlayici", "Alarm kurulamadı", e)
            }
        }

        fun iptal(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return
                am.cancel(pending(context, ACTION_SABAH, KOD_SABAH))
                am.cancel(pending(context, ACTION_AKSAM, KOD_AKSAM))
            } catch (e: Exception) {
                android.util.Log.w("BildirimZamanlayici", "Alarm iptal edilemedi", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            BildirimMerkezi.kanallariKur(context)
            when (intent.action) {
                ACTION_SABAH -> {
                    // v10.9: gün çerçevesi açıkken sabah rutini
                    // "uyandın mı?" kapısına devredilir. Burada ikiz
                    // teslimat olmasın diye eski 09:00 turu durur;
                    // çerçeve kapanırsa rutin eskisi gibi buradan gelir.
                    if (!UykuCerceve.acik(context)) {
                        BildirimUretici.tumKontroller(context, sabahMi = true)
                    }
                    // v11.13: proaktif akıllı koç
                    BildirimUretici.proaktifKoc(context)
                }
                ACTION_AKSAM -> {
                    BildirimUretici.tumKontroller(context, sabahMi = false)
                    // v11.13: proaktif akıllı koç
                    BildirimUretici.proaktifKoc(context)
                }
            }
            // Bir sonraki günü garantile (bazı cihazlar tekrarı düşürüyor)
            kur(context)
        } catch (e: Exception) {
            android.util.Log.w("BildirimZamanlayici", "Kontroller çalıştırılamadı", e)
        }
    }
}
