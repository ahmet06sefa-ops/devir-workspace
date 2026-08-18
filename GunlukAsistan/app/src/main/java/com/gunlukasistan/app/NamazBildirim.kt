package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * v7.47 — Namaz vakti bildirimleri.
 *
 * ── Kullanıcı isteği ──
 * "ayarlardan bildirim ekle acmali kapamali ses olarak ben ekleyeceğim
 *  titreşim olsun"
 *
 * ── Tasarım ──
 *  · Her vakit AYRI açılıp kapatılır (6 anahtar)
 *  · Ses kullanıcının kendi dosyası — sistem ses seçici ile
 *  · Titreşim ayrı anahtar, 3 desen seçeneği
 *  · Vakitten önce hatırlatma (0/5/10/15/30 dk)
 *
 * ── Neden ayrı kanal? ──
 * Android'de bildirim sesi KANALA bağlıdır. Kullanıcı sesi değiştirince
 * kanalın yeniden oluşturulması gerekir — bu yüzden kanal kimliği
 * sürüm numarası taşır ve ses değişiminde artırılır.
 */
class NamazBildirim : BroadcastReceiver() {

    companion object {
        const val ACTION_VAKIT = "com.gunlukasistan.app.NAMAZ_VAKIT"
        const val EXTRA_VAKIT = "vakit_anahtar"
        const val EXTRA_ONCE = "once_dk"

        private const val TAG = "NamazBildirim"
        private const val PREF = "namaz_bildirim_v1"
        private const val TEMEL_KOD = 8500
        private const val NOTIF_ID = 8501

        // ═══════════════════════════════════════════════════════════
        // AYARLAR
        // ═══════════════════════════════════════════════════════════

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        /** Namaz bildirimleri toptan açık mı? (v11.06: varsayılan açık) */
        fun acikMi(context: Context): Boolean =
            prefs(context).getBoolean("acik", true)

        /** v11.06: Namaz vakitlerinde sesli alarm çalsın mı? (varsayılan açık) */
        fun sesliAlarmAcik(context: Context): Boolean =
            prefs(context).getBoolean("sesli_alarm_acik", true)

        fun setSesliAlarmAcik(context: Context, acik: Boolean) {
            prefs(context).edit().putBoolean("sesli_alarm_acik", acik).apply()
        }

        fun setAcik(context: Context, acik: Boolean) {
            prefs(context).edit().putBoolean("acik", acik).apply()
            if (acik) hepsiniKur(context) else hepsiniIptal(context)
        }

        /** Belirli bir vakit için bildirim açık mı? */
        fun vakitAcik(context: Context, v: NamazVakti.Vakit): Boolean =
            prefs(context).getBoolean("v_" + v.anahtar, varsayilanAcik(v))

        /** Güneş vakti namaz değil — varsayılan kapalı. */
        private fun varsayilanAcik(v: NamazVakti.Vakit): Boolean =
            v != NamazVakti.Vakit.GUNES

        fun setVakitAcik(context: Context, v: NamazVakti.Vakit, acik: Boolean) {
            prefs(context).edit().putBoolean("v_" + v.anahtar, acik).apply()
            hepsiniKur(context)
        }

        /** Vakitten kaç dakika önce hatırlatılsın (0 = tam vaktinde). */
        fun oncedenDk(context: Context): Int = prefs(context).getInt("once", 0)

        fun setOncedenDk(context: Context, dk: Int) {
            prefs(context).edit().putInt("once", dk.coerceIn(0, 60)).apply()
            hepsiniKur(context)
        }

        // ── Ses ──

        /** Kullanıcının seçtiği ses dosyası (v11.06: varsayılan alarm/bildirim tonu). */
        fun sesUri(context: Context): String {
            val k = prefs(context).getString("ses_uri", null)
            if (!k.isNullOrEmpty()) return k
            return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)?.toString()
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)?.toString()
                ?: ""
        }

        fun sesAdi(context: Context): String =
            prefs(context).getString("ses_adi", "") ?: ""

        /**
         * Sesi kaydeder ve kanalı yeniden oluşturur.
         * Android'de kanal sesi sonradan değiştirilemez — kanal silinip
         * yeni kimlikle kurulmalı.
         */
        fun setSes(context: Context, uri: String, ad: String) {
            val p = prefs(context)
            val surum = p.getInt("kanal_surum", 0) + 1
            p.edit()
                .putString("ses_uri", uri)
                .putString("ses_adi", ad)
                .putInt("kanal_surum", surum)
                .apply()
            kanalYenile(context)
        }

        // ── Titreşim ──

        fun titresimAcik(context: Context): Boolean =
            prefs(context).getBoolean("titresim", true) && NamazAylikVeriServisi.namazTitresimAktifMi(context)

        fun setTitresim(context: Context, acik: Boolean) {
            prefs(context).edit().putBoolean("titresim", acik).apply()
            kanalYenile(context)
        }

        /** 0 = kısa, 1 = orta, 2 = uzun. */
        fun titresimDeseni(context: Context): Int =
            prefs(context).getInt("titresim_desen", 1)

        fun setTitresimDeseni(context: Context, desen: Int) {
            prefs(context).edit().putInt("titresim_desen", desen.coerceIn(0, 2)).apply()
            kanalYenile(context)
        }

        fun desenDizisi(desen: Int): LongArray = when (desen) {
            0 -> longArrayOf(0, 200, 150, 200)
            2 -> longArrayOf(0, 600, 300, 600, 300, 600)
            else -> longArrayOf(0, 400, 200, 400)
        }

        // ═══════════════════════════════════════════════════════════
        // KANAL
        // ═══════════════════════════════════════════════════════════

        private fun kanalId(context: Context): String =
            "namaz_vakti_v" + prefs(context).getInt("kanal_surum", 0)

        /**
         * Kanalı kurar. Ses/titreşim değişince eski kanal silinip
         * yeni kimlikle oluşturulur (Android kısıtlaması).
         */
        fun kanalYenile(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            try {
                // Eski sürümleri temizle
                val guncelId = kanalId(context)
                nm.notificationChannels
                    .filter { it.id.startsWith("namaz_vakti_v") && it.id != guncelId }
                    .forEach { nm.deleteNotificationChannel(it.id) }

                if (nm.getNotificationChannel(guncelId) != null) return

                val kanal = NotificationChannel(
                    guncelId,
                    context.getString(R.string.nb_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.nb_channel_desc)
                    group = BildirimMerkezi.GRUP_HATIRLATICI

                    val uri = sesUri(context)
                    if (uri.isBlank()) {
                        setSound(null, null)
                    } else {
                        setSound(
                            Uri.parse(uri),
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                                .build()
                        )
                    }

                    enableVibration(titresimAcik(context))
                    if (titresimAcik(context)) {
                        vibrationPattern = desenDizisi(titresimDeseni(context))
                    }
                }
                nm.createNotificationChannel(kanal)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Kanal kurulamadı", e)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // ALARM KURULUMU
        // ═══════════════════════════════════════════════════════════

        private fun pending(context: Context, v: NamazVakti.Vakit): PendingIntent {
            val intent = Intent(context, NamazBildirim::class.java).apply {
                action = ACTION_VAKIT
                putExtra(EXTRA_VAKIT, v.anahtar)
                putExtra(EXTRA_ONCE, oncedenDk(context))
                data = Uri.parse("gunlukasistan://namaz/" + v.anahtar)
            }
            return PendingIntent.getBroadcast(
                context, TEMEL_KOD + v.ordinal, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Tüm açık vakitler için alarm kurar.
         * Her bildirim tetiklendiğinde ertesi gün için yeniden kurulur.
         */
        fun hepsiniKur(context: Context) {
            if (!NamazVakti.acikMi(context) || !acikMi(context)) {
                hepsiniIptal(context)
                return
            }
            kanalYenile(context)

            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val once = oncedenDk(context)
            val simdi = System.currentTimeMillis()

            NamazVakti.Vakit.entries.forEach { v ->
                val pi = pending(context, v)
                if (!vakitAcik(context, v)) {
                    try { am.cancel(pi) } catch (_: Exception) {}
                    return@forEach
                }

                try {
                    // Bugünün vakti geçtiyse yarını hesapla
                    val cal = Calendar.getInstance()
                    var gun = NamazVakti.gunFor(context, cal)
                    var dk = gun.dakika(v)
                    if (dk < 0) return@forEach

                    var hedef = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, dk / 60)
                        set(Calendar.MINUTE, dk % 60)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -once)
                    }
                    if (hedef.timeInMillis <= simdi) {
                        val yarin = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        gun = NamazVakti.gunFor(context, yarin)
                        dk = gun.dakika(v)
                        if (dk < 0) return@forEach
                        hedef = yarin.apply {
                            set(Calendar.HOUR_OF_DAY, dk / 60)
                            set(Calendar.MINUTE, dk % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            add(Calendar.MINUTE, -once)
                        }
                    }

                    // Namaz vakti hassas olmalı — tam alarm denenir
                    if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, hedef.timeInMillis, pi
                        )
                    } else {
                        am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, hedef.timeInMillis, pi
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Alarm kurulamadı: " + v.anahtar, e)
                }
            }
        }

        fun hepsiniIptal(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            NamazVakti.Vakit.entries.forEach { v ->
                try { am.cancel(pending(context, v)) } catch (_: Exception) {}
            }
        }

        /** Ayarlar ekranındaki "Dene" düğmesi. */
        fun testGonder(context: Context) {
            kanalYenile(context)
            goster(context, NamazVakti.Vakit.OGLE, 0, test = true)
        }

        // ═══════════════════════════════════════════════════════════
        // BİLDİRİM
        // ═══════════════════════════════════════════════════════════

        fun goster(
            context: Context,
            v: NamazVakti.Vakit,
            onceDk: Int,
            test: Boolean = false
        ) {
            try {
                val gun = NamazVakti.bugunDuzeltilmis(context)
                val saat = gun.saat(v)
                val vakitAdi = context.getString(v.adRes)

                // v7.63: KOMPAKT BASLIK — tek satirda vakit + saat
                // Eskiden "Ogle vakti · 12:56" + ayri uzun govde vardi;
                // bildirim golgeliginde iki satir kapliyordu. Artik
                // baslik tek satir, govde kisa is ozeti.
                val baslik = if (onceDk > 0) {
                    context.getString(R.string.nb_kompakt_once, vakitAdi, onceDk)
                } else {
                    context.getString(R.string.nb_kompakt, vakitAdi, saat)
                }

                // v7.63: Govde — bekleyen isleri kisa liste halinde ver.
                // Tek is varsa tek satir; birden fazlaysa katlanmis liste.
                val dilim = NamazPlan.Dilim.entries.firstOrNull { it.baslangic == v }
                val bekleyen = try {
                    if (dilim != null) {
                        NamazPlan.dilimIsleri(context, dilim).filter { !it.tamamlandi }
                    } else emptyList()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Isler okunamadi", e)
                    emptyList()
                }

                val govde = when {
                    bekleyen.isNotEmpty() ->
                        context.getString(R.string.nb_is_ozet, bekleyen.first().metin)
                    dilim != null -> context.getString(
                        R.string.nb_body_oneri,
                        context.getString(dilim.adRes),
                        context.getString(dilim.varsayilanIsRes)
                    )
                    else -> context.getString(R.string.nb_body_sade, saat)
                }

                // Genisletilince tum bekleyen isler gorunsun (en fazla 4)
                val genisGovde = if (bekleyen.size > 1) {
                    bekleyen.take(4).joinToString("\n") {
                        context.getString(R.string.nb_is_ozet, it.metin)
                    }
                } else govde

                val ac = PendingIntent.getActivity(
                    context, 8600,
                    Intent(context, NamazActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val b = NotificationCompat.Builder(context, kanalId(context))
                    .setSmallIcon(R.drawable.ic_task_alt)
                    .setContentTitle(v.emoji + " " + baslik)
                    .setContentText(govde)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(ac)
                    // v7.63: kompakt gorunum — tek satir, gereksiz yer kaplamaz
                    .setShowWhen(true)
                    .setOnlyAlertOnce(true)
                    .addAction(0, context.getString(R.string.nb_action_plan), ac)

                // Birden fazla bekleyen is varsa genisletilebilir liste
                if (bekleyen.size > 1) {
                    b.setStyle(NotificationCompat.BigTextStyle().bigText(genisGovde))
                    b.setSubText(context.getString(R.string.nb_is_sayisi, bekleyen.size))
                }

                // Android 8 öncesi: ses/titreşim bildirimin kendisinde
                if (Build.VERSION.SDK_INT < 26) {
                    val uri = sesUri(context)
                    if (uri.isNotBlank()) b.setSound(Uri.parse(uri)) else b.setSound(null)
                    if (titresimAcik(context)) {
                        b.setVibrate(desenDizisi(titresimDeseni(context)))
                    }
                }

                NotificationManagerCompat.from(context)
                    .notify(
                        if (test) NOTIF_ID + 90 else NOTIF_ID + v.ordinal,
                        b.build()
                    )

                // v7.56: telefon sessizde olsa bile duyulsun
                try {
                    ZorunluUyari.bildirimeEslik(context, "namaz")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Israrlı uyarı çalınamadı", e)
                }

                // v11.06: Namaz Saatlerinde Sesli Alarm Çal (Ayarlardan Açık İse)
                if (sesliAlarmAcik(context)) {
                    try {
                        ZorunluUyari.cal(context, zorlaCal = true)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Namaz alarm sesi çalınamadı", e)
                    }
                }

                // Kanal titreşimi bazı cihazlarda çalışmıyor — elle de tetikle
                if (titresimAcik(context)) titret(context)
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "Bildirim izni yok", e)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Bildirim gösterilemedi", e)
            }
        }

        /** Titreşim — kanal ayarı çalışmayan cihazlar için yedek. */
        private fun titret(context: Context) {
            try {
                val desen = desenDizisi(titresimDeseni(context))
                val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (v == null || !v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createWaveform(desen, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(desen, -1)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Titreşim çalıştırılamadı", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_VAKIT) return
        try {
            val anahtar = intent.getStringExtra(EXTRA_VAKIT).orEmpty()
            val once = intent.getIntExtra(EXTRA_ONCE, 0)
            val vakit = NamazVakti.Vakit.entries.firstOrNull { it.anahtar == anahtar }
                ?: return

            if (acikMi(context) && vakitAcik(context, vakit)) {
                goster(context, vakit, once)
            }
            // Widget'ta sıradaki vakit değişti
            try {
                WidgetCommon.refreshAll(context)
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bildirim işlenemedi", e)
        } finally {
            // Ertesi gün için yeniden kur
            try {
                hepsiniKur(context)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Yeniden kurulamadı", e)
            }
        }
    }
}
