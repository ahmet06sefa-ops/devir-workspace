package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * v7.63 — Bildirim tanılama.
 *
 * ── Kullanıcının bildirimi ──
 * "Namaz saatleri vb bildirimler gelmiyor"
 *
 * ── Teşhis ──
 * Alarm kodu doğruydu, ama uygulama **Android 13+ bildirim iznini
 * (`POST_NOTIFICATIONS`) hiçbir yerde istemiyordu** — yalnızca Sayaç
 * ekranında. İzin verilmemişse `NotificationManagerCompat.notify()`
 * sessizce başarısız olur: alarm çalar, kod çalışır, ama ekranda
 * hiçbir şey görünmez. Kullanıcının yaşadığı tam olarak buydu.
 *
 * İkinci engel: Android 12+ `SCHEDULE_EXACT_ALARM`. İzin yoksa kod
 * `setAndAllowWhileIdle`'a düşüyor — bildirim gelir ama dakikalarca
 * gecikebilir. Namaz vakti için kabul edilemez.
 *
 * Bu sınıf tüm engelleri tek yerde toplar, kullanıcıya listeler ve
 * her birini tek dokunuşla düzeltmesini sağlar.
 */
object BildirimTani {

    private const val TAG = "BildirimTani"

    /** Tek bir kontrol maddesi. */
    data class Madde(
        val tamam: Boolean,
        val baslik: String,
        val aciklama: String = "",
        /** Sorun varsa çalıştırılacak düzeltme; null ise elle çözülür. */
        val duzelt: ((android.app.Activity) -> Unit)? = null
    )

    /**
     * Bildirimlerin çalışması için gereken her şeyi kontrol eder.
     * Sıra önemli: en kritik engel en üstte.
     */
    fun kontrolEt(context: Context): List<Madde> {
        val liste = mutableListOf<Madde>()

        // 1. POST_NOTIFICATIONS — Android 13+ (EN KRİTİK)
        val izinVar = bildirimIzniVar(context)
        liste.add(
            Madde(
                tamam = izinVar,
                baslik = context.getString(
                    if (izinVar) R.string.bt_izin_var else R.string.bt_izin_yok
                ),
                aciklama = if (izinVar) "" else context.getString(R.string.bt_izin_yok_d),
                duzelt = if (izinVar) null else { act -> izinIste(act) }
            )
        )

        // 2. Tam zamanlı alarm — Android 12+
        val tamAlarm = tamAlarmVar(context)
        liste.add(
            Madde(
                tamam = tamAlarm,
                baslik = context.getString(
                    if (tamAlarm) R.string.bt_tam_alarm_var else R.string.bt_tam_alarm_yok
                ),
                aciklama = if (tamAlarm) "" else context.getString(R.string.bt_tam_alarm_yok_d),
                duzelt = if (tamAlarm) null else { act -> tamAlarmAyariniAc(act) }
            )
        )

        // 3. Uygulama içi ana anahtar
        val anaAcik = try {
            Store.getNotifEnabled(context)
        } catch (_: Exception) {
            true
        }
        liste.add(
            Madde(
                tamam = anaAcik,
                baslik = context.getString(
                    if (anaAcik) R.string.bt_ana_acik else R.string.bt_ana_kapali
                ),
                aciklama = if (anaAcik) "" else context.getString(R.string.bt_ana_kapali_d),
                duzelt = if (anaAcik) null else { act ->
                    Store.setNotifEnabled(act, true)
                    try {
                        BildirimZamanlayici.kur(act)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Zamanlayıcı kurulamadı", e)
                    }
                }
            )
        )

        // 4. Namaz bildirimi
        val namazAcik = try {
            NamazVakti.acikMi(context) && NamazBildirim.acikMi(context)
        } catch (_: Exception) {
            false
        }
        val acikVakit = try {
            if (namazAcik) NamazVakti.Vakit.entries.count {
                NamazBildirim.vakitAcik(context, it)
            } else 0
        } catch (_: Exception) {
            0
        }
        liste.add(
            Madde(
                tamam = namazAcik && acikVakit > 0,
                baslik = if (namazAcik) {
                    context.getString(R.string.bt_namaz_acik, acikVakit)
                } else {
                    context.getString(R.string.bt_namaz_kapali)
                },
                aciklama = if (namazAcik) "" else context.getString(R.string.bt_namaz_kapali_d),
                duzelt = if (namazAcik && acikVakit > 0) null else { act ->
                    try {
                        NamazVakti.setAcik(act, true)
                        NamazBildirim.setAcik(act, true)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Namaz açılamadı", e)
                    }
                }
            )
        )

        // 5. Pil optimizasyonu
        val pilSerbest = try {
            OnlineBekci.pilMuafMi(context)
        } catch (_: Exception) {
            true
        }
        liste.add(
            Madde(
                tamam = pilSerbest,
                baslik = context.getString(
                    if (pilSerbest) R.string.bt_pil_serbest else R.string.bt_pil_kisitli
                ),
                aciklama = if (pilSerbest) "" else context.getString(R.string.bt_pil_kisitli_d),
                duzelt = if (pilSerbest) null else { act ->
                    try {
                        OnlineBekci.pilAyariniAc(act)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Pil ayarı açılamadı", e)
                    }
                }
            )
        )

        return liste
    }

    /** Kaç sorun var? */
    fun sorunSayisi(context: Context): Int = kontrolEt(context).count { !it.tamam }

    // ═══════════════════════════════════════════════════════════════
    // TEKİL KONTROLLER
    // ═══════════════════════════════════════════════════════════════

    /** Android 13+ bildirim izni verildi mi? */
    fun bildirimIzniVar(context: Context): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "İzin okunamadı", e)
        true
    }

    /** Tam zamanlı alarm kurulabilir mi? (Android 12+) */
    fun tamAlarmVar(context: Context): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val am = context.getSystemService(Context.ALARM_SERVICE)
                as? android.app.AlarmManager
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Alarm izni okunamadı", e)
        true
    }

    // ═══════════════════════════════════════════════════════════════
    // DÜZELTMELER
    // ═══════════════════════════════════════════════════════════════

    /** Bildirim iznini ister (Android 13+). */
    fun izinIste(activity: android.app.Activity) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    9701
                )
            } else {
                sistemAyariniAc(activity)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İzin istenemedi", e)
            sistemAyariniAc(activity)
        }
    }

    /** Uygulamanın sistem bildirim ayarlarını açar. */
    fun sistemAyariniAc(context: Context) {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= 26) {
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:" + context.packageName))
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sistem ayarı açılamadı", e)
        }
    }

    /** Tam zamanlı alarm izin ekranını açar (Android 12+). */
    fun tamAlarmAyariniAc(context: Context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(android.net.Uri.parse("package:" + context.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Alarm ayarı açılamadı", e)
            sistemAyariniAc(context)
        }
    }

    /**
     * Uygulama açılışında sessizce çağrılır: izin yoksa bir kez ister.
     * Kullanıcı reddederse bir daha rahatsız etmeyiz.
     */
    fun acilistaIzinIste(activity: android.app.Activity) {
        try {
            if (android.os.Build.VERSION.SDK_INT < 33) return
            val p = activity.getSharedPreferences("bildirim_tani", Context.MODE_PRIVATE)
            if (p.getBoolean("soruldu", false)) return
            if (bildirimIzniVar(activity)) return
            p.edit().putBoolean("soruldu", true).apply()
            izinIste(activity)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Açılış izni istenemedi", e)
        }
    }
}
