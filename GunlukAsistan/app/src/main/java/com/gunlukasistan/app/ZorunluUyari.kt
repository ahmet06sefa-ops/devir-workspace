package com.gunlukasistan.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * v7.56 — Israrlı uyarı: telefon **sessizde olsa bile** ses çıkarır.
 *
 * ── Kullanıcının isteği ──
 * "Telefon sessiz de olsa bile bildirim sesi çıkarmasını ayarla ve titreştirme ekle"
 *
 * ── Nasıl çalışıyor (ve neden böyle) ──
 * Normal bildirim sesi NOTIFICATION kanalından çıkar; telefon sessize
 * alınınca Android bu kanalı susturur ve uygulamanın yapabileceği bir şey
 * yoktur. Tek yasal yol **ALARM kanalı**: kullanıcı alarm sesini ayrıca
 * kısmadığı sürece sessiz/titreşim modunda da duyulur — çalar saat
 * uygulamaları da tam olarak bunu yapar.
 *
 * Bu yüzden bildirim yerine doğrudan [MediaPlayer] ile
 * `USAGE_ALARM` akışından çalıyoruz, titreşimi de kendimiz veriyoruz.
 *
 * ── Dürüst sınırlar ──
 * 1. Kullanıcı **alarm ses düzeyini sıfıra** çekerse hiçbir uygulama ses
 *    çıkaramaz. `sesiZorla` açıksa geçici olarak yükseltmeyi deneriz.
 * 2. **Rahatsız Etmeyin (DND)** modunda alarm da susturulabilir. Bunun için
 *    `ACCESS_NOTIFICATION_POLICY` izni gerekir — kullanıcı elle verir.
 *    Sıradan sessiz/titreşim modunda bu izin GEREKMEZ.
 * 3. Ses uygulama süreci yaşarken çalar. Alarm/bildirim tetiklenince
 *    `BroadcastReceiver` içinden çağrıldığı için sorun olmaz.
 */
object ZorunluUyari {

    private const val TAG = "ZorunluUyari"
    private const val PREF = "zorunlu_uyari_v1"

    /** Aynı anda tek ses çalsın. */
    private var oynatici: MediaPlayer? = null
    private var titresim: Vibrator? = null
    private val elciler: Handler? by lazy { try { Handler(Looper.getMainLooper()) } catch (_: Exception) { null } }

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Israrlı uyarı açık mı? Varsayılan KAPALI — kullanıcı bilerek açsın. */
    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
    }

    /** Boş = cihazın varsayılan alarm sesi. */
    fun sesUri(context: Context): String = prefs(context).getString("ses_uri", "") ?: ""

    fun sesAdi(context: Context): String = prefs(context).getString("ses_adi", "") ?: ""

    fun setSes(context: Context, uri: String, ad: String) {
        prefs(context).edit().putString("ses_uri", uri).putString("ses_adi", ad).apply()
    }

    fun titresimAcik(context: Context): Boolean =
        prefs(context).getBoolean("titresim", true)

    fun setTitresim(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("titresim", acik).apply()
    }

    /** 0 = kısa · 1 = orta · 2 = uzun (ısrarlı). */
    fun desen(context: Context): Int = prefs(context).getInt("desen", 1)

    fun setDesen(context: Context, d: Int) {
        prefs(context).edit().putInt("desen", d.coerceIn(0, 2)).apply()
    }

    /** Kaç saniye çalsın (3-60). */
    fun sureSn(context: Context): Int = prefs(context).getInt("sure", 10).coerceIn(3, 60)

    fun setSureSn(context: Context, sn: Int) {
        prefs(context).edit().putInt("sure", sn.coerceIn(3, 60)).apply()
    }

    /**
     * Alarm ses düzeyi kısıksa geçici olarak yükseltilsin mi?
     * Varsayılan KAPALI — kullanıcının ayarına izinsiz dokunmak agresif.
     */
    fun sesiZorla(context: Context): Boolean = prefs(context).getBoolean("zorla", false)

    fun setSesiZorla(context: Context, z: Boolean) {
        prefs(context).edit().putBoolean("zorla", z).apply()
    }

    // Kapsam: hangi bildirim türlerinde ısrarlı uyarı kullanılsın
    fun kapsamNamaz(context: Context): Boolean = prefs(context).getBoolean("k_namaz", true)
    fun kapsamGorev(context: Context): Boolean = prefs(context).getBoolean("k_gorev", true)
    fun kapsamZaman(context: Context): Boolean = prefs(context).getBoolean("k_zaman", true)

    fun setKapsam(context: Context, anahtar: String, acik: Boolean) {
        prefs(context).edit().putBoolean(anahtar, acik).apply()
    }

    /** Titreşim deseni — ses eşliğinde tekrarlanır. */
    fun desenDizisi(d: Int): LongArray = when (d) {
        0 -> longArrayOf(0, 250, 200, 250)
        2 -> longArrayOf(0, 700, 350, 700, 350, 700, 350, 700)
        else -> longArrayOf(0, 450, 250, 450, 250, 450)
    }

    // ═══════════════════════════════════════════════════════════════
    // RAHATSIZ ETMEYİN (DND)
    // ═══════════════════════════════════════════════════════════════

    /**
     * DND modunu aşma iznimiz var mı?
     *
     * Not: Bu izin YALNIZCA "Rahatsız Etmeyin" için gerekli.
     * Sıradan sessiz/titreşim modunda alarm akışı zaten çalar.
     */
    fun dndIzniVar(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            nm?.isNotificationPolicyAccessGranted == true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "DND izni okunamadı", e)
            false
        }
    }

    /** Kullanıcıyı DND izin ekranına götürür. */
    fun dndAyarlariniAc(context: Context) {
        try {
            context.startActivity(
                android.content.Intent(
                    android.provider.Settings
                        .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "DND ayarı açılamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇALMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Uyarıyı çalar: alarm akışından ses + titreşim.
     *
     * @param zorlaCal true ise "açık mı" kontrolü atlanır (test düğmesi)
     * @return çalmaya başladıysa true
     */
    fun cal(context: Context, zorlaCal: Boolean = false): Boolean {
        if (!zorlaCal && !acikMi(context)) return false
        return try {
            durdur(context)   // önceki çalıyorsa kes
            val sn = sureSn(context)
            sesiBaslat(context, sn)
            if (titresimAcik(context)) titresimiBaslat(context, sn)
            // Süre dolunca kendiliğinden sussun
            elciler?.postDelayed({ durdur(context) }, sn * 1000L)
            true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Uyarı çalınamadı", e)
            false
        }
    }

    private fun sesiBaslat(context: Context, saniye: Int) {
        val uri: Uri = try {
            val kayitli = sesUri(context)
            if (kayitli.isNotBlank()) Uri.parse(kayitli)
            else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ses adresi okunamadı", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } ?: return

        // İsteğe bağlı: alarm sesi kısıksa yükselt
        if (sesiZorla(context)) alarmSesiniYukselt(context)

        oynatici = MediaPlayer().apply {
            setDataSource(context, uri)
            // ── Kritik nokta ──
            // USAGE_ALARM sayesinde telefon sessizdeyken bile duyulur.
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = saniye > 4    // kısa seslerde tekrarla
            setOnErrorListener { _, _, _ ->
                android.util.Log.w(TAG, "MediaPlayer hatası")
                true
            }
            prepare()
            start()
        }
    }

    private fun titresimiBaslat(context: Context, saniye: Int) {
        val v = titresimAlicisi(context) ?: return
        titresim = v
        val desen = desenDizisi(desen(context))
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // -1 = tekrar yok; süre dolunca durdur() zaten kesiyor
                val tekrar = if (saniye > 4) 0 else -1
                v.vibrate(VibrationEffect.createWaveform(desen, tekrar))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(desen, if (saniye > 4) 0 else -1)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Titreşim verilemedi", e)
        }
    }

    private fun titresimAlicisi(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Titreşim alınamadı", e)
        null
    }

    /**
     * Alarm ses düzeyi 0 ise duyulabilir bir seviyeye çeker.
     * Kullanıcının ayarına dokunduğumuz için varsayılan kapalı.
     */
    private fun alarmSesiniYukselt(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val enYuksek = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val simdiki = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (simdiki < enYuksek / 3) {
                am.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    (enYuksek * 0.6).toInt().coerceAtLeast(1), 0
                )
            }
        } catch (e: Exception) {
            // Bazı cihazlarda DND açıkken SecurityException atar — yutuluyor
            android.util.Log.w(TAG, "Alarm sesi yükseltilemedi", e)
        }
    }

    /** Çalan sesi ve titreşimi durdurur. */
    /**
     * v11.00 — Telefon kapatma / güç tuşuyla alarmı anında durdurma anahtarı kontrolü.
     */
    fun gucTusuyleDurdur(context: Context?): Boolean {
        if (context != null && !SayacAyar.isKapatmaTusuyleAlarmDurdur(context)) {
            return false
        }
        if (context != null) durdur(context)
        return true
    }

    fun durdur(context: Context? = null) {
        try {
            oynatici?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ses durdurulamadı", e)
        }
        oynatici = null
        try {
            titresim?.cancel()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Titreşim durdurulamadı", e)
        }
        titresim = null
    }

    /**
     * Bildirim gönderilirken çağrılır — kapsam açıksa ısrarlı uyarıyı çalar.
     *
     * @param kapsam "namaz" · "gorev" · "zaman"
     */
    fun bildirimeEslik(context: Context, kapsam: String) {
        if (!acikMi(context)) return
        val uygun = when (kapsam) {
            "namaz" -> kapsamNamaz(context)
            "gorev" -> kapsamGorev(context)
            "zaman" -> kapsamZaman(context)
            else -> false
        }
        if (uygun) cal(context)
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): org.json.JSONObject {
        val o = org.json.JSONObject()
        return try {
            o.put("acik", acikMi(context))
            o.put("ses_uri", sesUri(context))
            o.put("ses_adi", sesAdi(context))
            o.put("titresim", titresimAcik(context))
            o.put("desen", desen(context))
            o.put("sure", sureSn(context))
            o.put("zorla", sesiZorla(context))
            o.put("k_namaz", kapsamNamaz(context))
            o.put("k_gorev", kapsamGorev(context))
            o.put("k_zaman", kapsamZaman(context))
            o
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Dışa aktarılamadı", e)
            o
        }
    }

    fun iceAktar(context: Context, o: org.json.JSONObject?) {
        if (o == null) return
        try {
            prefs(context).edit()
                .putBoolean("acik", o.optBoolean("acik", false))
                .putString("ses_uri", o.optString("ses_uri", ""))
                .putString("ses_adi", o.optString("ses_adi", ""))
                .putBoolean("titresim", o.optBoolean("titresim", true))
                .putInt("desen", o.optInt("desen", 1))
                .putInt("sure", o.optInt("sure", 10))
                .putBoolean("zorla", o.optBoolean("zorla", false))
                .putBoolean("k_namaz", o.optBoolean("k_namaz", true))
                .putBoolean("k_gorev", o.optBoolean("k_gorev", true))
                .putBoolean("k_zaman", o.optBoolean("k_zaman", true))
                .apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İçe aktarılamadı", e)
        }
    }
}
