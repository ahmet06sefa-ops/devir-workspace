package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * v9.1 — Bildirim testi (öneri 41).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN GEREKLİ
 * ══════════════════════════════════════════════════════════════════
 * Samsung bildirim sorunu (v7.88-v7.93) **altı sürüm** sürdü. Sebep:
 * her denemede kullanıcıya yeni bir APK gönderip "şimdi oldu mu?"
 * diye sormak zorundaydık. Her tur bir gün kaybı.
 *
 * Sorun Samsung'a özel de değildi — Xiaomi, Huawei, Oppo'da hiç
 * test edilmedi. Bir kullanıcı "bildirim gelmiyor" dediğinde
 * elimizde hiçbir veri yok.
 *
 * ══════════════════════════════════════════════════════════════════
 * BU SINIF NE YAPIYOR
 * ══════════════════════════════════════════════════════════════════
 * Üç farklı bildirim yolunu ayrı ayrı deniyor:
 *
 *   1. **Anında** — `NotificationManager.notify()` doğrudan.
 *      Başarısızsa: bildirim izni yok veya kanal kapalı.
 *
 *   2. **Kısa alarm (10 sn)** — `setExactAndAllowWhileIdle`.
 *      Başarısızsa: tam alarm izni yok veya pil kısıtı var.
 *
 *   3. **Uzun alarm (2 dk)** — uygulama arka plandayken.
 *      Başarısızsa: üretici uygulamayı öldürüyor (en sinsi durum).
 *
 * Kullanıcı hangisinin geldiğini işaretliyor; sorun tam olarak
 * hangi katmanda, tek turda anlaşılıyor.
 */
class BildirimTestReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BildirimTest"
        const val ACTION_TEST = "com.gunlukasistan.app.BILDIRIM_TESTI"
        const val EK_TUR = "tur"

        const val TUR_ANINDA = 0
        const val TUR_KISA = 1
        const val TUR_UZUN = 2

        /** Test bildirimlerinin kimlikleri — normal bildirimlerle çakışmasın. */
        private const val ID_TABAN = 990_000

        /**
         * Testi başlatır.
         *
         * @return kullanıcıya gösterilecek durum metni
         */
        fun baslat(context: Context, tur: Int): String {
            return when (tur) {
                TUR_ANINDA -> {
                    goster(context, tur)
                    context.getString(R.string.bt_aninda_gonderildi)
                }
                TUR_KISA -> zamanla(context, tur, 10_000L)
                TUR_UZUN -> zamanla(context, tur, 120_000L)
                else -> ""
            }
        }

        private fun zamanla(context: Context, tur: Int, gecikmeMs: Long): String {
            return runCatching {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pi = PendingIntent.getBroadcast(
                    context,
                    ID_TABAN + tur,
                    Intent(context, BildirimTestReceiver::class.java).apply {
                        action = ACTION_TEST
                        putExtra(EK_TUR, tur)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val hedef = System.currentTimeMillis() + gecikmeMs

                // Tam alarm izni yoksa setWindow'a düş — ama kullanıcıya
                // söyle, yoksa "geç geldi" diye şikâyet eder
                val tamIzin = AlarmSagligi.tamAlarmIzniVar(context)
                if (tamIzin) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hedef, pi)
                } else {
                    am.setWindow(AlarmManager.RTC_WAKEUP, hedef, 60_000L, pi)
                }

                val saniye = (gecikmeMs / 1000).toInt()
                if (tamIzin) {
                    context.getString(R.string.bt_zamanlandi, saniye)
                } else {
                    context.getString(R.string.bt_zamanlandi_kaba, saniye)
                }
            }.getOrElse {
                android.util.Log.w(TAG, "Test alarmı kurulamadı", it)
                context.getString(R.string.bt_kurulamadi)
            }
        }

        /** Test bildirimini gösterir. */
        fun goster(context: Context, tur: Int) {
            runCatching {
                BildirimMerkezi.kanallariKur(context)

                val baslikRes = when (tur) {
                    TUR_ANINDA -> R.string.bt_n_aninda
                    TUR_KISA -> R.string.bt_n_kisa
                    else -> R.string.bt_n_uzun
                }
                val zaman = java.text.SimpleDateFormat(
                    "HH:mm:ss", java.util.Locale.US
                ).format(java.util.Date())

                val niyet = PendingIntent.getActivity(
                    context,
                    ID_TABAN + 100 + tur,
                    Intent(context, BildirimTaniActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Kanal: görev hatırlatıcısıyla aynı kanalı kullanıyoruz.
                // Sebep: test tam da o kanalın çalışıp çalışmadığını
                // ölçmeli. Ayrı bir "test kanalı" açsaydık, kullanıcının
                // sustur[duğu] gerçek kanalı test etmemiş olurduk.
                val kanal = BildirimMerkezi.Tur.GOREV.kanal

                val bildirim = androidx.core.app.NotificationCompat.Builder(context, kanal)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(baslikRes))
                    .setContentText(context.getString(R.string.bt_n_metin, zaman))
                    .setStyle(
                        androidx.core.app.NotificationCompat.BigTextStyle()
                            .bigText(context.getString(R.string.bt_n_uzun_metin, zaman))
                    )
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(niyet)
                    .build()

                androidx.core.app.NotificationManagerCompat.from(context)
                    .notify(ID_TABAN + tur, bildirim)

                // v9.1 · Öneri 44: tetiklenme kaydı
                if (tur != TUR_ANINDA) AlarmSagligi.tetiklendiKaydet(context)

                // Sonucu kaydet — tanılama ekranı "geldi" diyebilsin
                context.getSharedPreferences("bildirim_test_v1", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("son_$tur", System.currentTimeMillis())
                    .apply()
            }.onFailure { android.util.Log.w(TAG, "Test bildirimi gösterilemedi", it) }
        }

        /** Bu türden en son ne zaman bildirim geldi? */
        fun sonGelis(context: Context, tur: Int): Long =
            context.getSharedPreferences("bildirim_test_v1", Context.MODE_PRIVATE)
                .getLong("son_$tur", 0L)

        /** Test kayıtlarını sıfırla. */
        fun sifirla(context: Context) {
            context.getSharedPreferences("bildirim_test_v1", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TEST) return
        val tur = intent.getIntExtra(EK_TUR, TUR_KISA)
        goster(context, tur)
    }
}
