package com.gunlukasistan.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * v7.57 — Online odayı arka planda kontrol eden bekçi.
 *
 * ── Kullanıcının isteği ──
 * "Olur ekle" → arka planda mesaj kontrolü + bildirim
 *
 * ── Neden böyle bir şey gerekti ──
 * textdb.online ücretsiz bir depolama servisi; push bildirim göndermiyor.
 * Yani karşı taraf mesaj yazınca telefonumuz kendiliğinden haberdar olamaz.
 * Tek yol: belirli aralıklarla biz sorup değişiklik var mı bakmak.
 *
 * ── Nasıl çalışıyor ──
 * `AlarmManager.setInexactRepeating` ile pil dostu bir tekrar kurulur
 * (varsayılan 30 dk). Alarm çalınca odayı okur, önceki durumla karşılaştırır
 * ve yeni bir şey varsa bildirim gönderir.
 *
 * `setInexactRepeating` seçildi çünkü Android bu alarmları toplu işleyip
 * pili korur. Sohbet bildirimi saniye hassasiyeti gerektirmez.
 *
 * ── Dürüst sınır ──
 * Bu **anlık bildirim değildir**. Karşı taraf mesaj attıktan sonra
 * seçilen aralık kadar (ve telefonun pil tasarrufuna göre biraz daha)
 * gecikme olabilir. Gerçek anlık bildirim için Firebase gibi bir push
 * servisi ve sunucu gerekir — bu uygulama tamamen ücretsiz altyapıda çalışıyor.
 */
class OnlineBekci : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_KONTROL) return
        // Ağ işlemi ana iş parçacığında yapılamaz
        val bitir = goAsync()
        Thread {
            try {
                kontrolEt(context)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Kontrol başarısız", e)
            } finally {
                try {
                    bitir.finish()
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    companion object {

        private const val TAG = "OnlineBekci"
        private const val PREF = "online_bekci_v1"
        const val ACTION_KONTROL = "com.gunlukasistan.app.ONLINE_KONTROL"
        private const val KOD_ALARM = 8701
        private const val KANAL = "ch_online_v1"
        private const val NOTIF_ID = 8710

        // ═══════════════════════════════════════════════════════════
        // AYARLAR
        // ═══════════════════════════════════════════════════════════

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        /** Arka plan kontrolü açık mı? Varsayılan KAPALI — kullanıcı seçsin. */
        fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

        fun setAcik(context: Context, acik: Boolean) {
            prefs(context).edit().putBoolean("acik", acik).apply()
            if (acik) kur(context) else iptal(context)
        }

        /** Kontrol aralığı (dakika): 15 · 30 · 60 · 180. */
        fun siklikDk(context: Context): Int = prefs(context).getInt("siklik", 30)

        fun setSiklikDk(context: Context, dk: Int) {
            prefs(context).edit().putInt("siklik", dk).apply()
            if (acikMi(context)) kur(context)
        }

        // Neler bildirilsin
        fun bilMesaj(context: Context): Boolean = prefs(context).getBoolean("b_mesaj", true)
        fun bilGorev(context: Context): Boolean = prefs(context).getBoolean("b_gorev", true)
        fun bilNot(context: Context): Boolean = prefs(context).getBoolean("b_not", true)
        fun bilKonu(context: Context): Boolean = prefs(context).getBoolean("b_konu", false)
        fun bilTamam(context: Context): Boolean = prefs(context).getBoolean("b_tamam", false)

        /** Kendi eklediklerim için bildirim gelmesin. */
        fun sadeceKarsi(context: Context): Boolean =
            prefs(context).getBoolean("sadece_karsi", true)

        fun setBayrak(context: Context, anahtar: String, deger: Boolean) {
            prefs(context).edit().putBoolean(anahtar, deger).apply()
        }

        fun sonKontrol(context: Context): Long = prefs(context).getLong("son", 0L)

        private fun setSonKontrol(context: Context) {
            prefs(context).edit().putLong("son", System.currentTimeMillis()).apply()
        }

        // ═══════════════════════════════════════════════════════════
        // ALARM
        // ═══════════════════════════════════════════════════════════

        private fun pending(context: Context): PendingIntent {
            val intent = Intent(context, OnlineBekci::class.java).apply {
                action = ACTION_KONTROL
            }
            return PendingIntent.getBroadcast(
                context, KOD_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Tekrarlayan kontrolü kurar. */
        fun kur(context: Context) {
            try {
                if (!acikMi(context)) return
                if (!OnlineStore.bagliMi(context)) return
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return
                val aralik = siklikDk(context) * 60_000L
                am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + aralik,
                    aralik,
                    pending(context)
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm kurulamadı", e)
            }
        }

        fun iptal(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return
                am.cancel(pending(context))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm iptal edilemedi", e)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // KONTROL
        // ═══════════════════════════════════════════════════════════

        /**
         * Odayı okur, önceki durumla karşılaştırır, yenilik varsa bildirir.
         *
         * @param elle true ise kullanıcı "şimdi kontrol et" dedi —
         *             açık/kapalı ayarına bakılmaz
         * @return bulunan yenilik sayısı
         */
        fun kontrolEt(context: Context, elle: Boolean = false): Int {
            if (!elle && !acikMi(context)) return 0
            if (!OnlineStore.bagliMi(context)) return 0

            val sonuc = OnlineStore.oku(context)
            val oda = sonuc.oda
            if (!sonuc.ok || oda == null) {
                android.util.Log.w(TAG, "Oda okunamadı: " + sonuc.mesaj)
                return 0
            }

            val ben = OnlineStore.benimAdim(context)
            val yalnizKarsi = sadeceKarsi(context)
            val gorulen = gorulenKimlikler(context)
            val yeniKimlikler = mutableSetOf<String>()
            val satirlar = mutableListOf<String>()

            // ── Sohbet mesajları ──
            if (bilMesaj(context)) {
                val yeniler = oda.mesajlar.filter { m ->
                    val anahtar = "m" + m.id
                    val bizden = yalnizKarsi && m.kim == ben
                    if (!gorulen.contains(anahtar)) yeniKimlikler.add(anahtar)
                    !gorulen.contains(anahtar) && !bizden && m.metin.isNotBlank()
                }
                if (yeniler.size == 1) {
                    val m = yeniler.first()
                    satirlar.add(
                        context.getString(
                            R.string.ob_bil_mesaj, m.kim, m.metin.take(60)
                        )
                    )
                } else if (yeniler.size > 1) {
                    satirlar.add(context.getString(R.string.ob_bil_mesaj_c, yeniler.size))
                }
            }

            // ── Görevler ──
            if (bilGorev(context)) {
                val yeniler = oda.gorevler.filter { g ->
                    val anahtar = "g" + g.id
                    val bizden = yalnizKarsi && g.sahip == ben
                    if (!gorulen.contains(anahtar)) yeniKimlikler.add(anahtar)
                    !gorulen.contains(anahtar) && !bizden
                }
                if (yeniler.size == 1) {
                    satirlar.add(
                        context.getString(R.string.ob_bil_gorev, yeniler.first().metin.take(60))
                    )
                } else if (yeniler.size > 1) {
                    satirlar.add(context.getString(R.string.ob_bil_gorev_c, yeniler.size))
                }
            }

            // ── Notlar ──
            if (bilNot(context)) {
                oda.notlar.forEach { n ->
                    val anahtar = "n" + n.id
                    val yeni = !gorulen.contains(anahtar)
                    if (yeni) yeniKimlikler.add(anahtar)
                    if (yeni && !(yalnizKarsi && n.sahip == ben)) {
                        satirlar.add(
                            context.getString(R.string.ob_bil_not, n.baslik.take(50))
                        )
                    }
                }
            }

            // ── Konular ──
            if (bilKonu(context)) {
                oda.konular.forEach { k ->
                    val anahtar = "t" + k.id
                    val yeni = !gorulen.contains(anahtar)
                    if (yeni) yeniKimlikler.add(anahtar)
                    if (yeni && !(yalnizKarsi && k.sahip == ben)) {
                        satirlar.add(
                            context.getString(R.string.ob_bil_konu, k.baslik.take(50))
                        )
                    }
                }
            }

            // ── Tamamlanan görevler ──
            if (bilTamam(context)) {
                oda.gorevler.filter { it.tamam }.forEach { g ->
                    val anahtar = "c" + g.id
                    val yeni = !gorulen.contains(anahtar)
                    if (yeni) yeniKimlikler.add(anahtar)
                    if (yeni && !(yalnizKarsi && g.sahip == ben)) {
                        satirlar.add(
                            context.getString(
                                R.string.ob_bil_tamam, g.sahip, g.metin.take(50)
                            )
                        )
                    }
                }
            }

            // Görülenleri kaydet — aynı şey iki kez bildirilmesin
            if (yeniKimlikler.isNotEmpty()) {
                gorulenKaydet(context, gorulen + yeniKimlikler)
            }
            // Önbelleği tazele — uygulama açılınca güncel veri hazır olsun
            try {
                OnlineStore.onbellegeYaz(context, oda)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Önbellek yazılamadı", e)
            }
            setSonKontrol(context)

            // İlk çalıştırma: mevcut her şeyi "görüldü" say, bildirim yağdırma
            if (gorulen.isEmpty()) return 0

            if (satirlar.isNotEmpty()) bildir(context, satirlar)
            return satirlar.size
        }

        /** Daha önce bildirilen öğelerin kimlikleri. */
        private fun gorulenKimlikler(context: Context): Set<String> =
            prefs(context).getStringSet("gorulen", emptySet()) ?: emptySet()

        private fun gorulenKaydet(context: Context, kume: Set<String>) {
            // Sınırsız büyümesin — son 400 kayıt yeter
            val kirpik = if (kume.size > 400) kume.toList().takeLast(400).toSet() else kume
            prefs(context).edit().putStringSet("gorulen", kirpik).apply()
        }

        /** Odaya ilk bağlanıldığında mevcut içeriği "görüldü" işaretler. */
        fun temizBaslat(context: Context) {
            prefs(context).edit().remove("gorulen").apply()
        }

        // ═══════════════════════════════════════════════════════════
        // BİLDİRİM
        // ═══════════════════════════════════════════════════════════

        fun kanalKur(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            try {
                val nm = context.getSystemService(NotificationManager::class.java) ?: return
                if (nm.getNotificationChannel(KANAL) != null) return
                nm.createNotificationChannel(
                    NotificationChannel(
                        KANAL,
                        context.getString(R.string.ob_kanal),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.ob_kanal_desc)
                        group = BildirimMerkezi.GRUP_HATIRLATICI
                    }
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Kanal kurulamadı", e)
            }
        }

        private fun bildir(context: Context, satirlar: List<String>) {
            if (!Store.getNotifEnabled(context)) return
            kanalKur(context)
            try {
                val baslik = context.getString(R.string.on_title)
                val ozet = if (satirlar.size == 1) satirlar.first()
                else context.getString(R.string.ob_bil_karma, satirlar.size)

                val ac = PendingIntent.getActivity(
                    context, 8711,
                    Intent(context, OnlineActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val b = NotificationCompat.Builder(context, KANAL)
                    .setSmallIcon(R.drawable.ic_task_alt)
                    .setContentTitle(baslik)
                    .setContentText(ozet)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(satirlar.joinToString("\n"))
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setAutoCancel(true)
                    .setContentIntent(ac)

                if (!Store.getVibEnabled(context)) b.setVibrate(longArrayOf(0L))
                if (!Store.getSoundEnabled(context)) b.setSound(null)

                NotificationManagerCompat.from(context).notify(NOTIF_ID, b.build())

                // v7.56 altyapısı: telefon sessizde olsa bile duyulsun
                try {
                    ZorunluUyari.bildirimeEslik(context, "gorev")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Israrlı uyarı çalınamadı", e)
                }
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "Bildirim izni yok", e)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Bildirim gönderilemedi", e)
            }
        }

        // ═══════════════════════════════════════════════════════════
        // PİL OPTİMİZASYONU
        // ═══════════════════════════════════════════════════════════

        /** Uygulama pil optimizasyonundan muaf mı? */
        fun pilMuafMi(context: Context): Boolean = try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true
            else {
                val pm = context.getSystemService(Context.POWER_SERVICE)
                    as? android.os.PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Pil durumu okunamadı", e)
            false
        }

        /** Pil optimizasyonu ayarlarını açar. */
        fun pilAyariniAc(context: Context) {
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Pil ayarı açılamadı", e)
                try {
                    context.startActivity(
                        Intent(android.provider.Settings.ACTION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
