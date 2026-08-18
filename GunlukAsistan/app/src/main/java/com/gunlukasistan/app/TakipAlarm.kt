package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * v9.7 — Takip hatırlatıcılarını sistem alarmına kuran yardımcı.
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN GÜNDE TEK ALARM — KAYIT BAŞINA DEĞİL
 * ══════════════════════════════════════════════════════════════════
 * İlk tasarımım her kayıt için ayrı alarm kurmaktı. Vazgeçtim:
 *
 *   · 30 fatura + 10 belge + 5 araç = 45 alarm. Android 12+ tam
 *     alarm kotası sınırlı; sistem bunları kısabilir.
 *   · Uyarı eşiği değiştiğinde 45 alarmı tek tek iptal edip yeniden
 *     kurmak gerekiyor — hata yapmaya çok açık.
 *   · Kullanıcı 45 ayrı bildirim istemiyor; "bugün 3 şeye bak"
 *     diyen tek bildirim daha faydalı.
 *
 * Bunun yerine **günde bir kez** (varsayılan 09:00) çalışan tek
 * alarm var. Uyandığında [Takip.uyarilar] listesini okuyor ve
 * varsa **tek bir özet bildirim** gönderiyor.
 *
 * İSTİSNA: ilaç saatleri. "Sabah 08:00 ilacını al" bildirimi günün
 * özetiyle birleştirilemez — o ana ait. Bunlar ayrı alarmla
 * kuruluyor ama saat başına gruplanıyor: aynı 08:00'de üç ilaç
 * varsa tek alarm, tek bildirim, üç satır.
 *
 * ══════════════════════════════════════════════════════════════════
 * ALARM KİMLİK ARALIĞI
 * ══════════════════════════════════════════════════════════════════
 * Diğer modüllerle çakışmasın diye ayrılmış aralık:
 *   · 71000        → günlük özet
 *   · 71001-71024  → ilaç saati (71000 + saat)
 *
 * `AlarmScheduler` görev id'lerini `taskId.toInt()` olarak
 * kullanıyor; görev id'leri `System.currentTimeMillis()` tabanlı
 * olduğu için `toInt()` sonrası çok büyük/negatif sayılar çıkıyor,
 * 71000 civarına düşme olasılığı pratikte yok ama yine de
 * dokümante ediyorum.
 */
object TakipAlarm {

    private const val TAG = "TakipAlarm"
    private const val PREF = "takip_v1"

    const val ISTEK_GUNLUK = 71000
    private const val ISTEK_ILAC_TABAN = 71000

    const val EXTRA_TUR = "takip_tur"
    const val TUR_GUNLUK = "gunluk"
    const val TUR_ILAC = "ilac"
    const val EXTRA_SAAT = "saat"

    /** Günlük özet bildiriminin varsayılan saati (dakika cinsinden). */
    private const val VARSAYILAN_SAAT = 9 * 60

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun ozetSaati(context: Context): Int =
        p(context).getInt("ozet_saat", VARSAYILAN_SAAT).coerceIn(0, 1439)

    fun ozetSaatiAyarla(context: Context, dakika: Int) {
        p(context).edit().putInt("ozet_saat", dakika.coerceIn(0, 1439)).apply()
        yenidenKur(context)
    }

    fun acikMi(context: Context): Boolean = p(context).getBoolean("alarm_acik", true)

    fun acikAyarla(context: Context, acik: Boolean) {
        p(context).edit().putBoolean("alarm_acik", acik).apply()
        if (acik) yenidenKur(context) else hepsiniIptalEt(context)
    }

    private fun am(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pi(context: Context, istek: Int, tur: String, saat: Int = -1): PendingIntent {
        val i = Intent(context, TakipReceiver::class.java).apply {
            putExtra(EXTRA_TUR, tur)
            if (saat >= 0) putExtra(EXTRA_SAAT, saat)
            // Aynı sınıfa giden farklı extra'lı Intent'ler PendingIntent
            // tarafından "eşit" sayılır (extras karşılaştırmaya girmez).
            // Farklı istek kodu kullanmak bu yüzden şart; ayrıca data
            // alanını da ayırıyoruz ki FLAG_UPDATE_CURRENT karışmasın.
            data = android.net.Uri.parse("gunlukasistan://takip/$tur/$saat")
        }
        return PendingIntent.getBroadcast(
            context, istek, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Tüm takip alarmlarını yeniden kurar.
     *
     * Kayıt eklendiğinde/silindiğinde, önyükleme sonrasında ve
     * saat dilimi değişiminde çağrılıyor.
     *
     * @return kurulan alarm sayısı
     */
    fun yenidenKur(context: Context): Int {
        if (!acikMi(context)) {
            hepsiniIptalEt(context)
            return 0
        }
        var sayi = 0

        // ── 1. Günlük özet ──
        runCatching {
            val hedef = sonrakiGunlukSaat(ozetSaati(context))
            kur(context, hedef, pi(context, ISTEK_GUNLUK, TUR_GUNLUK))
            sayi++
        }.onFailure { android.util.Log.w(TAG, "Günlük alarm kurulamadı", it) }

        // ── 2. İlaç saatleri ──
        // Önce eski ilaç alarmlarını temizle: kullanıcı 08:00'i
        // silmişse alarm kalmasın.
        for (saat in 0..23) {
            runCatching {
                am(context).cancel(pi(context, ISTEK_ILAC_TABAN + 1 + saat, TUR_ILAC, saat))
            }
        }

        val ilacSaatleri = runCatching {
            Takip.turdekiler(context, Takip.Tur.ILAC)
                .flatMap { it.saatler }
                .map { it / 60 }
                .distinct()
                .filter { it in 0..23 }
        }.getOrDefault(emptyList())

        ilacSaatleri.forEach { saat ->
            runCatching {
                val dakika = Takip.turdekiler(context, Takip.Tur.ILAC)
                    .flatMap { it.saatler }
                    .filter { it / 60 == saat }
                    .minOrNull() ?: (saat * 60)
                val hedef = sonrakiGunlukSaat(dakika)
                kur(context, hedef, pi(context, ISTEK_ILAC_TABAN + 1 + saat, TUR_ILAC, saat))
                sayi++
            }.onFailure { android.util.Log.w(TAG, "İlaç alarmı kurulamadı: $saat", it) }
        }

        runCatching { AlarmSagligi.kurulumKaydet(context, "TAKIP", sayi) }
        return sayi
    }

    fun hepsiniIptalEt(context: Context) {
        runCatching { am(context).cancel(pi(context, ISTEK_GUNLUK, TUR_GUNLUK)) }
        for (saat in 0..23) {
            runCatching {
                am(context).cancel(pi(context, ISTEK_ILAC_TABAN + 1 + saat, TUR_ILAC, saat))
            }
        }
    }

    /**
     * Tek kayıt iptali.
     *
     * Günde tek alarm kullandığımız için aslında kayıt bazlı iptal
     * gerekmiyor — sadece yeniden kurmak yeterli. İmzayı koruyorum
     * ki [Takip.sil] çağrısı okunabilir kalsın.
     */
    fun iptal(context: Context, kayitId: Long) {
        yenidenKur(context)
    }

    private fun kur(context: Context, atMillis: Long, hedef: PendingIntent) {
        val yonetici = am(context)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !yonetici.canScheduleExactAlarms()) {
                yonetici.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, hedef)
            } else {
                yonetici.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, hedef)
            }
        } catch (e: SecurityException) {
            runCatching {
                yonetici.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, hedef)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Alarm kurulamadı", e)
        }
    }

    /**
     * Bugünün verilen dakikası geçtiyse yarına kurar.
     *
     * Saniye ve milisaniye sıfırlanıyor: aksi halde alarm
     * 09:00:37.412 gibi bir anda çalar ve her yeniden kurulumda
     * saniyeler birikerek kayar.
     */
    fun sonrakiGunlukSaat(dakika: Int, simdi: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = simdi
            set(Calendar.HOUR_OF_DAY, dakika / 60)
            set(Calendar.MINUTE, dakika % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= simdi) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }
}
