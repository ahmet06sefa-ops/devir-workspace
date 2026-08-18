package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * v7.78 — Koçun saatini kuran ve çalınca harekete geçen alıcı.
 *
 * ── İki alarm ──
 *   1. **Çalışma saati** — "otur çalış" bildirimi
 *   2. **Hesap saati**   — gün sonu "ne yaptın" hesabı
 *
 * ── Neden `setExactAndAllowWhileIdle` ──
 * [OnlineBekci] `setInexactRepeating` kullanıyor çünkü sohbet kontrolü
 * dakika hassasiyeti gerektirmiyor. Burada tam tersi: kullanıcı "beni
 * zorla" dedi; 20:00 için kurulan hatırlatma 21:30'da gelirse baskı
 * işlevini kaybeder. Bu yüzden kesin alarm kullanılıyor ve her tetikten
 * sonra ertesi gün için yeniden kuruluyor (tekrarlayan kesin alarm yok).
 *
 * ── Doze modu ──
 * `AllowWhileIdle` telefon uykudayken de çalışmayı sağlar. Android bu tür
 * alarmları dakikada birden fazla çalıştırmaz — bizde günde 2 tane var,
 * sorun değil.
 */
class KocZamanlayici : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eylem = intent.action ?: return
        val bitir = goAsync()
        Thread {
            try {
                when (eylem) {
                    ACTION_CALIS -> calismaVakti(context)
                    ACTION_HESAP -> hesapVakti(context)
                    ACTION_ERTELE -> ertelemeyiIsle(context, intent)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Koç tetiklenemedi", e)
            } finally {
                runCatching { bitir.finish() }
                // Her tetikten sonra ertesi günü kur
                runCatching { kur(context) }
            }
        }.start()
    }

    companion object {

        private const val TAG = "KocZamanlayici"

        const val ACTION_CALIS = "com.gunlukasistan.app.KOC_CALIS"
        const val ACTION_HESAP = "com.gunlukasistan.app.KOC_HESAP"
        const val ACTION_ERTELE = "com.gunlukasistan.app.KOC_ERTELE"

        private const val KOD_CALIS = 8801
        private const val KOD_HESAP = 8802
        private const val KOD_ERTELE = 8803

        const val NOTIF_CALIS = 8810
        const val NOTIF_HESAP = 8811

        // ═══════════════════════════════════════════════════════════
        // ALARM KURULUMU
        // ═══════════════════════════════════════════════════════════

        private fun pending(context: Context, eylem: String, kod: Int): PendingIntent {
            val intent = Intent(context, KocZamanlayici::class.java).apply { action = eylem }
            return PendingIntent.getBroadcast(
                context, kod, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Bir sonraki verilen saati bulur (bugün geçtiyse yarın). */
        private fun sonrakiZaman(saat: Int): Long {
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, saat)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= System.currentTimeMillis()) {
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
            return c.timeInMillis
        }

        fun kur(context: Context) {
            try {
                if (!Koc.acikMi(context)) return
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return

                kurTek(am, context, sonrakiZaman(Koc.calismaSaati(context)),
                    pending(context, ACTION_CALIS, KOD_CALIS))
                kurTek(am, context, sonrakiZaman(Koc.hesapSaati(context)),
                    pending(context, ACTION_HESAP, KOD_HESAP))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm kurulamadı", e)
            }
        }

        private fun kurTek(am: AlarmManager, context: Context, zaman: Long, pi: PendingIntent) {
            try {
                // Android 12+ kesin alarm izni kullanıcı tarafından kapatılmış olabilir
                val kesinIzin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.canScheduleExactAlarms()
                } else true

                if (kesinIzin) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
                } else {
                    // İzin yoksa sessizce yaklaşık alarma düş — hiç kurmamaktan iyi
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi)
                }
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "Kesin alarm izni yok", e)
                runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi) }
            }
        }

        fun iptal(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return
                am.cancel(pending(context, ACTION_CALIS, KOD_CALIS))
                am.cancel(pending(context, ACTION_HESAP, KOD_HESAP))
                am.cancel(pending(context, ACTION_ERTELE, KOD_ERTELE))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm iptal edilemedi", e)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // TETİKLENME
        // ═══════════════════════════════════════════════════════════

        /** Çalışma saati geldi. */
        private fun calismaVakti(context: Context) {
            if (!Koc.acikMi(context)) return
            if (!Koc.bugunCalismaGunuMu(context)) return
            // Hedef zaten dolduysa rahatsız etme
            if (Koc.bugunTamamMi(context)) return

            KocBildirim.calismaCagrisi(context)

            // Acımasız modda sessizde bile ses çıkar
            if (Koc.sertlik(context) == Koc.SERT_ACIMASIZ &&
                ZorunluUyari.acikMi(context)
            ) {
                runCatching { ZorunluUyari.cal(context, zorlaCal = true) }
            }
        }

        /** Gün sonu hesabı. */
        private fun hesapVakti(context: Context) {
            if (!Koc.acikMi(context)) return
            if (!Koc.bugunCalismaGunuMu(context)) return
            if (Koc.kapatildiMi(context)) return

            if (Koc.bugunTamamMi(context)) {
                // Başarılı gün — kutla ve sessizce kapat
                Koc.gunuKapat(context)
                KocBildirim.tebrik(context)
            } else {
                // Eksik gün — hesap sor
                KocBildirim.hesapSor(context)
            }
        }

        /** "15 dk sonra" ertelemesi. */
        private fun ertelemeyiIsle(context: Context, intent: Intent) {
            if (!Koc.acikMi(context)) return
            KocBildirim.calismaCagrisi(context, ertelendi = true)
        }

        /**
         * Kullanıcı bildirimden "Sonra" derse çağrılır.
         * @param dakika kaç dakika sonra tekrar sorulacak
         */
        fun ertele(context: Context, dakika: Int) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return
                kurTek(
                    am, context,
                    System.currentTimeMillis() + dakika * 60_000L,
                    pending(context, ACTION_ERTELE, KOD_ERTELE)
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Erteleme kurulamadı", e)
            }
        }
    }
}
