package com.gunlukasistan.app

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v9.1 — Alarm ve bildirim sağlığı izleme (öneri 43, 44, 47).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN: SESSİZ BAŞARISIZLIK
 * ══════════════════════════════════════════════════════════════════
 * Uygulama alarm kuruyor, kullanıcı bildirim bekliyor, bildirim
 * gelmiyor. Neden geldiğini kimse bilmiyor. Olası sebepler:
 *
 *   1. **Tam alarm izni geri alındı** (Android 14) — kullanıcı
 *      ayarlardan `SCHEDULE_EXACT_ALARM` iznini kapatabiliyor.
 *      Kod `canScheduleExactAlarms()` kontrolü yapıp sessizce
 *      `setWindow`a düşüyor: alarm 15 dakika kayabiliyor.
 *
 *   2. **Pil optimizasyonu** — Xiaomi/Huawei/Oppo uygulamayı
 *      arka planda öldürüyor. Alarm hiç çalmıyor.
 *
 *   3. **Yeniden başlatmada kurulum başarısız** — `BootReceiver`
 *      çalıştı mı, kaç alarm kurdu, bilinmiyordu.
 *
 *   4. **Uygulama güncellemesi** — Android güncellemede tüm
 *      alarmları iptal ediyor. v9.1'e kadar `MY_PACKAGE_REPLACED`
 *      dalı görev alarmlarını YENİDEN KURMUYORDU.
 *
 * Samsung bildirim sorunu (v7.88-v7.93) tam da bu görünürlük
 * eksikliği yüzünden **altı sürüm** sürmüştü.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM
 * ══════════════════════════════════════════════════════════════════
 * Her alarm kurulumunu kaydet, izin durumunu sorgula, üretici
 * bazlı uyarı ver. Kullanıcı "neden bildirim gelmiyor" dediğinde
 * tek ekranda cevap görsün.
 */
object AlarmSagligi {

    private const val TAG = "AlarmSagligi"
    private const val PREF = "alarm_sagligi_v1"

    private const val K_SON_KURULUM = "son_kurulum"
    private const val K_SON_SEBEP = "son_sebep"
    private const val K_KURULAN = "kurulan_sayi"
    private const val K_SON_TETIK = "son_tetik"
    private const val K_TETIK_SAYI = "tetik_sayi"
    private const val K_IZIN_UYARI = "izin_uyari_gosterildi"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Kayıt (öneri 44)
    // ══════════════════════════════════════════════════════════

    /** Alarm kurulum turu tamamlandığında çağrılır. */
    fun kurulumKaydet(c: Context, sebep: String, adet: Int = -1) {
        runCatching {
            val e = p(c).edit()
                .putLong(K_SON_KURULUM, System.currentTimeMillis())
                .putString(K_SON_SEBEP, sebep)
            if (adet >= 0) e.putInt(K_KURULAN, adet)
            e.apply()
        }.onFailure { android.util.Log.w(TAG, "kurulumKaydet", it) }
    }

    /**
     * Bir alarm gerçekten tetiklendiğinde çağrılır.
     *
     * ── Neden önemli ──
     * "Alarm kuruldu" ile "alarm çaldı" farklı şeyler. Pil
     * optimizasyonu olan cihazlarda kurulum başarılı görünür ama
     * tetikleme hiç olmaz. Bu sayaç ikisi arasındaki farkı
     * görünür kılıyor.
     */
    fun tetiklendiKaydet(c: Context) {
        runCatching {
            val d = p(c)
            d.edit()
                .putLong(K_SON_TETIK, System.currentTimeMillis())
                .putInt(K_TETIK_SAYI, d.getInt(K_TETIK_SAYI, 0) + 1)
                .apply()
        }.onFailure { android.util.Log.w(TAG, "tetiklendiKaydet", it) }
    }

    fun sonKurulum(c: Context): Long = p(c).getLong(K_SON_KURULUM, 0L)
    fun sonKurulumSebebi(c: Context): String = p(c).getString(K_SON_SEBEP, "") ?: ""
    fun kurulanSayi(c: Context): Int = p(c).getInt(K_KURULAN, -1)
    fun sonTetik(c: Context): Long = p(c).getLong(K_SON_TETIK, 0L)
    fun tetikSayi(c: Context): Int = p(c).getInt(K_TETIK_SAYI, 0)

    // ══════════════════════════════════════════════════════════
    // İzin durumu (öneri 43)
    // ══════════════════════════════════════════════════════════

    /**
     * Tam zamanlı alarm kurulabiliyor mu?
     *
     * Android 12+ `SCHEDULE_EXACT_ALARM` izni gerektiriyor;
     * Android 14'te kullanıcı bunu geri alabiliyor. İzin yoksa
     * alarmlar `setWindow` ile kuruluyor ve **15 dakikaya kadar
     * kayabiliyor** — sayaç bildirimi için kabul edilemez.
     */
    fun tamAlarmIzniVar(c: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.canScheduleExactAlarms()
    }.getOrDefault(false)

    /** Pil optimizasyonundan muaf mıyız? */
    fun pilKisitsizMi(c: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 23) return true
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(c.packageName)
    }.getOrDefault(false)

    /** Bildirim izni (Android 13+). */
    fun bildirimIzniVar(c: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 33) return true
        androidx.core.content.ContextCompat.checkSelfPermission(
            c, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    // ══════════════════════════════════════════════════════════
    // Üretici uyarıları (öneri 42)
    // ══════════════════════════════════════════════════════════

    /**
     * Agresif pil yönetimi yapan üreticiler.
     *
     * ── Neden bu liste ──
     * dontkillmyapp.com verilerine göre bu üreticiler Android'in
     * standart davranışının ötesinde uygulamaları öldürüyor.
     * Kullanıcının elle "otomatik başlatma" izni vermesi gerekiyor;
     * uygulama bunu programatik olarak yapamıyor — yalnız
     * yönlendirebiliyor.
     */
    private val AGRESIF_URETICILER = mapOf(
        "xiaomi" to "Güvenlik → İzinler → Otomatik başlatma",
        "redmi" to "Güvenlik → İzinler → Otomatik başlatma",
        "poco" to "Güvenlik → İzinler → Otomatik başlatma",
        "huawei" to "Ayarlar → Uygulamalar → Başlatma → Elle yönet",
        "honor" to "Ayarlar → Uygulamalar → Başlatma → Elle yönet",
        "oppo" to "Ayarlar → Pil → Arka plan donduruculuğu → Kapat",
        "realme" to "Ayarlar → Pil → Arka plan donduruculuğu → Kapat",
        "vivo" to "Ayarlar → Pil → Arka planda yüksek güç tüketimi",
        "oneplus" to "Ayarlar → Pil → Pil optimizasyonu → Optimize etme",
        "meizu" to "Ayarlar → Uygulama yönetimi → Arka planda çalıştır",
        "asus" to "Mobil Yönetici → Güç tasarrufu → Otomatik başlatma",
        "samsung" to "Ayarlar → Pil → Arka plan kullanım sınırları"
    )

    /** Bu cihaz agresif pil yönetimi yapıyor mu? */
    fun agresifUreticiMi(): Boolean {
        val u = Build.MANUFACTURER.lowercase(Locale.US)
        return AGRESIF_URETICILER.keys.any { u.contains(it) }
    }

    /** Üreticiye özel yönerge. Bilinmiyorsa null. */
    fun ureticiYonergesi(): String? {
        val u = Build.MANUFACTURER.lowercase(Locale.US)
        val anahtar = AGRESIF_URETICILER.keys.firstOrNull { u.contains(it) } ?: return null
        return AGRESIF_URETICILER[anahtar]
    }

    fun ureticiAdi(): String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

    // ══════════════════════════════════════════════════════════
    // Genel sağlık puanı
    // ══════════════════════════════════════════════════════════

    /** Sağlık durumu — tanılama ekranı bunu gösteriyor. */
    data class Saglik(
        val bildirimIzni: Boolean,
        val tamAlarmIzni: Boolean,
        val pilKisitsiz: Boolean,
        val agresifUretici: Boolean,
        val sonKurulumMs: Long,
        val kurulanAlarm: Int,
        val sonTetikMs: Long,
        val tetikSayisi: Int
    ) {
        /** 0-100 arası. 100 = her şey yolunda. */
        val puan: Int
            get() {
                var p = 0
                if (bildirimIzni) p += 40      // olmazsa hiçbir şey çalışmaz
                if (tamAlarmIzni) p += 30      // olmazsa 15 dk kayar
                if (pilKisitsiz) p += 20       // olmazsa arka planda ölür
                if (!agresifUretici) p += 10   // üretici riski
                return p
            }

        val durumMetni: Int
            get() = when {
                puan >= 90 -> R.string.as_durum_iyi
                puan >= 60 -> R.string.as_durum_orta
                else -> R.string.as_durum_kotu
            }
    }

    fun kontrolEt(c: Context): Saglik = Saglik(
        bildirimIzni = bildirimIzniVar(c),
        tamAlarmIzni = tamAlarmIzniVar(c),
        pilKisitsiz = pilKisitsizMi(c),
        agresifUretici = agresifUreticiMi(),
        sonKurulumMs = sonKurulum(c),
        kurulanAlarm = kurulanSayi(c),
        sonTetikMs = sonTetik(c),
        tetikSayisi = tetikSayi(c)
    )

    // ══════════════════════════════════════════════════════════
    // Yönlendirme
    // ══════════════════════════════════════════════════════════

    /** Tam alarm izni ayar ekranını açar (Android 12+). */
    fun tamAlarmAyariniAc(c: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 31) return false
        c.startActivity(
            android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                android.net.Uri.parse("package:${c.packageName}")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    /**
     * Pil optimizasyonu muafiyeti ister.
     *
     * ── Neden doğrudan istemek riskli ──
     * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` izniyle doğrudan
     * pencere açmak Play Store politikasına aykırı olabiliyor
     * (yalnız alarm/mesajlaşma uygulamaları için serbest).
     * Bunun yerine AYAR EKRANINI açıyoruz; kullanıcı kendi seçiyor.
     */
    fun pilAyariniAc(c: Context): Boolean = runCatching {
        c.startActivity(
            android.content.Intent(
                android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    /** Uygulama bilgisi ekranı (her cihazda çalışır — son çare). */
    fun uygulamaAyariniAc(c: Context): Boolean = runCatching {
        c.startActivity(
            android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${c.packageName}")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    // ══════════════════════════════════════════════════════════
    // Biçimlendirme
    // ══════════════════════════════════════════════════════════

    fun zamanMetni(c: Context, ms: Long): String {
        if (ms <= 0L) return c.getString(R.string.as_hic)
        val fark = System.currentTimeMillis() - ms
        return when {
            fark < 60_000 -> c.getString(R.string.as_az_once)
            fark < 3_600_000 -> c.getString(R.string.as_dk_once, (fark / 60_000).toInt())
            fark < 86_400_000 -> c.getString(R.string.as_saat_once, (fark / 3_600_000).toInt())
            else -> SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(ms))
        }
    }

    /** İzin uyarısı daha önce gösterildi mi? (bir kez göstermek için) */
    fun izinUyarisiGosterildi(c: Context): Boolean = p(c).getBoolean(K_IZIN_UYARI, false)

    fun izinUyarisiIsaretle(c: Context) {
        p(c).edit().putBoolean(K_IZIN_UYARI, true).apply()
    }
}
