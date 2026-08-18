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
 * Zamanlayıcı bildirimindeki butonları ve süre bitişini işler (v6.4).
 */
class TimerActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FINISHED = "com.gunlukasistan.app.TIMER_FINISHED"

        /** v7.93: bildirim metnini periyodik tazeleme. */
        const val ACTION_TAZELE = "com.gunlukasistan.app.TIMER_TAZELE"

        // ── v10.2 eylemleri ──────────────────────────────────────
        /** A2: bitiş/çalışan bildirimden süre uzatma. extra: EXTRA_UZAT_MS. */
        const val ACTION_UZAT = "com.gunlukasistan.app.TIMER_UZAT"

        /** A2: bitiş sonrası aynı süreyle yeniden başlatma. */
        const val ACTION_YENIDEN = "com.gunlukasistan.app.TIMER_YENIDEN"

        /** B26: bitişı sonraya ertele. extra: EXTRA_ERTELE_SEC (SayacErtele). */
        const val ACTION_ERTELE = "com.gunlukasistan.app.TIMER_ERTELE"

        /** B26: erteleme vakti geldi — "şimdi başlat" hatırlatması. */
        const val ACTION_SNOOZE_FIRE = "com.gunlukasistan.app.TIMER_SNOOZE_FIRE"

        /** v10.5 · C27: widget çipinden gelen "N dk başlat". */
        const val ACTION_BASLAT_DK = "com.gunlukasistan.app.TIMER_BASLAT_DK"
        const val EXTRA_DAKIKA = "dakika"

        /** v10.12 · D24: bildirimden başlatmada seans etiketi (ör. sıradaki ders). */
        const val EXTRA_ETIKET = "etiket"

        /** v11.13: bitiş sesini/titreşimi bildirimden durdurma. */
        const val ACTION_DURDUR_SES = "com.gunlukasistan.app.TIMER_DURDUR_SES"

        const val EXTRA_UZAT_MS = "uzat_ms"
        const val EXTRA_ERTELE_SEC = "ertele_sec"

        private const val DONE_CHANNEL = "zamanlayici_bitti_v1"
        private const val DONE_ID = 4712
        private const val SNOOZE_ID = 4715
        private const val SNOOZE_REQ = 4716

        /** Bitiş bildirimini panelden kaldırır (uyanık bitiş ekranı da çağırır). */
        fun bitisBildiriminiKapat(context: Context) {
            runCatching {
                NotificationManagerCompat.from(context).cancel(DONE_ID)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerNotifier.ACTION_TOGGLE -> {
                if (TimerEngine.isRunning(context)) {
                    TimerEngine.creditWatch(context)
                    TimerEngine.pause(context)
                } else {
                    TimerEngine.start(context)
                }
                TimerAlarm.reschedule(context)
            }

            TimerNotifier.ACTION_RESET -> {
                TimerEngine.creditWatch(context)
                TimerEngine.reset(context)
                TimerAlarm.cancel(context)
            }

            // v10.5 · C27: widget çipi — süreyi kur ve başlat.
            ACTION_BASLAT_DK -> {
                val dk = intent.getIntExtra(EXTRA_DAKIKA, 0)
                if (dk > 0) {
                    // Koşan oturum bölünmez: çipler boştayken görünür
                    // ama görünmez durumda da (gecikmiş dokunuş) koru.
                    if (!TimerEngine.isRunning(context)) {
                        // v10.12 · D24: derse kenetli başlatma etiketi sayaca işlenir
                        intent.getStringExtra(EXTRA_ETIKET)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { runCatching { SayacAyar.setEtiket(context, it.take(24)) } }
                        TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
                        TimerEngine.setTotalMs(context, dk * 60_000L)
                        TimerEngine.start(context)
                        TimerAlarm.reschedule(context)
                    }
                    TimerNotifier.show(context)
                }
            }

            TimerNotifier.ACTION_STOP -> {
                // v7.87: "İptal et" — sayacı durdur, başa sar, bildirimi kaldır.
                // Eskiden yalnızca duraklatıyordu; bildirimdeki etiket "Kapat"
                // olduğu için fark edilmiyordu. Artık düğme "İptal et" olduğuna
                // göre gerçekten iptal etmeli, yoksa yarım kalan süre sessizce
                // saklanıp kullanıcıyı şaşırtırdı.
                // v10.7 · A6: zincir iptal edildiyse koşu da duraklasın —
                // aksi hâlde evre bitişi gelmeden zincir takılı kalırdı.
                if (SayacZincir.kosuyor(context)) SayacZincir.durdur(context)
                // v9.4 · Öneri 14: yarıda kesilen pomodoro da kaydedilsin.
                // Yalnız tamamlananları saymak "başarı oranı %100"
                // gösterirdi — işe yaramaz bir istatistik.
                runCatching {
                    if (Pomodoro.acikMi(context) && !Pomodoro.molada(context) &&
                        TimerEngine.isRunning(context)
                    ) {
                        val gecen = ((TimerEngine.totalMs(context) -
                            TimerEngine.remainingMs(context)) / 60_000L).toInt()
                        if (gecen > 0) {
                            SureAnalizi.pomodoroKaydet(context, gecen, tamamlandi = false)
                        }
                    }
                }
                TimerEngine.creditWatch(context)
                TimerEngine.reset(context)
                TimerNotifier.cancel(context)
                TimerAlarm.cancel(context)
            }

            ACTION_TAZELE -> {
                // v7.93: uyumluluk modunda süre düz metin olduğu için
                // bildirimin periyodik tazelenmesi gerekiyor. Uygulama
                // kapalıyken de çalışsın diye alarmla tetikleniyor.
                if (TimerEngine.isRunning(context)) {
                    TimerNotifier.show(context)
                    // Bir sonraki tazelemeyi zincirle
                    TimerAlarm.tazelemeyiKur(context)
                } else {
                    TimerAlarm.tazelemeyiIptalEt(context)
                }
            }

            ACTION_UZAT -> {
                // v10.2 · A2: bildirimden süre uzat. Erteleme bekliyorsa iptal.
                val ekMs = intent.getLongExtra(EXTRA_UZAT_MS, 5 * 60_000L)
                TimerEngine.uzat(context, ekMs)
                TimerAlarm.reschedule(context)
                bitisBildiriminiKapat(context)
                runCatching { NotificationManagerCompat.from(context).cancel(SNOOZE_ID) }
            }

            ACTION_YENIDEN -> {
                // v10.2 · A2: aynı süreyle yeniden başlat.
                // start() kalan <= 0 iken toplam süreye sarar (TimerEngine).
                TimerEngine.start(context)
                TimerAlarm.reschedule(context)
                bitisBildiriminiKapat(context)
                runCatching { NotificationManagerCompat.from(context).cancel(SNOOZE_ID) }
            }

            ACTION_ERTELE -> {
                // v10.2 · B26: bitişı sonraya ertele — tam alarmla hatırlat.
                val sec = intent.getIntExtra(EXTRA_ERTELE_SEC, SayacErtele.SEC_ONDK)
                val hedef = SayacErtele.hedefMilis(sec, System.currentTimeMillis())
                runCatching {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val niyet = PendingIntent.getBroadcast(
                        context, SNOOZE_REQ,
                        Intent(context, TimerActionReceiver::class.java).apply {
                            action = ACTION_SNOOZE_FIRE
                            putExtra(EXTRA_ERTELE_SEC, sec)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hedef, niyet)
                    } else {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hedef, niyet)
                    }
                }.onFailure {
                    android.util.Log.w("TimerActionReceiver", "Erteleme kurulamadı", it)
                }
                bitisBildiriminiKapat(context)
            }

            ACTION_DURDUR_SES -> {
                // v11.13: bitiş bildirimindeki "Durdur" düğmesi — alarm sesini
                // ve titreşimi anında keser, bitiş bildirimini kaldırır.
                BitisSesMotoru.durdur()
                bitisBildiriminiKapat(context)
            }

            ACTION_SNOOZE_FIRE -> {
                // v10.2 · B26: erteleme doldu — "şimdi başlat" hatırlatması.
                ertelemeBildirimi(context)
            }

            ACTION_FINISHED -> {
                TimerEngine.finish(context)
                TimerNotifier.cancel(context)
                // v10.2 · A14: flaş çakması (ayarlıysa, cihazda flaş varsa)
                SayacFlas.cal(context)
                // v7.85: süre bitince titret — bildirimden bağımsız.
                // Eskiden titreşim yalnızca bildirimin varsayılan deseninden
                // geliyordu; bildirim izni kapalıysa ya da kanal titreşimi
                // devre dışıysa hiçbir şey hissedilmiyordu.
                // v10.15 · C14: sayaç türü sessiz penceredeyse zil ve titreşim
                // susar; bildirim + flaş yoluna devam eder (işitsel sükunet).
                // v11.13: ses + titreşim artık BitisSesMotoru'ndan çalınır —
                // döngülü alarm sesi ve güç düğmesi (ACTION_SCREEN_OFF) ile
                // anında susturma bu motor sayesinde sağlanır.
                if (!runCatching {
                    SessizTurler.susturMu(context, BildirimMerkezi.K_ZAMANLAYICI_BITIS)
                }.getOrDefault(false)) {
                    BitisSesMotoru.cal(context)
                }
                // v7.94: süreyi aktif derse işle (öneri 1).
                // finish() içindeki günlük toplam korunuyor; bu ek olarak
                // ders bazlı kaydı yapıyor.
                odagiKaydet(context)
                showDone(context)
                // v7.94: pomodoro döngüsü — otomatikTekrar'ın yerini aldı
                dongueyIlerlet(context)
            }
        }
    }

    /**
     * v7.86 — Pomodoro döngüsü: süre bitince aynı süreyle yeniden başlat.
     *
     * Varsayılan kapalı; beklenmedik biçimde yeniden başlayan bir sayaç
     * kullanıcıyı şaşırtır.
     */
    /**
     * v7.94 — Biten sürenin ders kaydı (öneri 1).
     *
     * Kronometre modunda süre zaten `creditWatch` ile işleniyor; burada
     * yalnızca geri sayım ele alınıyor.
     */
    private fun odagiKaydet(context: Context) {
        try {
            if (OdakKaydi.mod(context) == OdakKaydi.MOD_KAPALI) return
            // Molada geçen süre derse yazılmamalı
            if (Pomodoro.acikMi(context) && Pomodoro.molada(context)) return
            // v10.7 · A6: zincir koşarken ders kaydı evrenin niteliğine
            // bakar — "Dinlenme", "Isınma" gibi adım odağa yazılmaz.
            if (SayacZincir.kosuyor(context)) {
                val aktifMi = SayacZincir.aktif(context)
                val evre = aktifMi?.let { SayacZincir.adimdaki(it, SayacZincir.adim(context)) }
                if (evre != null && !evre.odakMi) return
            }

            val dakika = (TimerEngine.totalMs(context) / 60_000L).toInt()
            if (dakika <= 0) return

            // MOD_SOR'da bile burada otomatik yazılır; kullanıcıya soru
            // uygulama açıldığında sorulur (bildirimden diyalog açılamaz).
            // Yanlışsa "son oturumu taşı" ile düzeltilebilir.
            OdakKaydi.kaydet(context, dakika)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Odak kaydedilemedi", e)
        }
    }

    /**
     * v7.94 — Pomodoro döngüsünü ilerletir (öneri 2).
     *
     * Pomodoro kapalıysa eski `otomatikTekrar` davranışına düşer:
     * aynı süreyle yeniden başlat.
     */
    private fun dongueyIlerlet(context: Context) {
        try {
            // v10.7 · A6: zincir koşarken öncelik zincirindir.
            // Pomodoro ayarları korunur; zincir kapanınca kaldığı
            // yerden çalışmaya devam eder.
            if (SayacZincir.kosuyor(context)) {
                zincirIlerlet(context)
                return
            }

            if (Pomodoro.acikMi(context)) {
                // v9.4 · Öneri 14: pomodoro istatistiği.
                //
                // Pomodoro v7.94'ten beri çalışıyor ama HİÇ ölçülmüyordu.
                // Kaç tur tamamlandı, hangi saatte daha verimlisin —
                // hiçbiri bilinmiyordu. Buraya geldiysek çalışma evresi
                // TAM SÜRE bitmiş demektir (kullanıcı iptal etseydi
                // ACTION_STOP'a giderdi).
                runCatching {
                    if (!Pomodoro.molada(context)) {
                        SureAnalizi.pomodoroKaydet(
                            context,
                            (TimerEngine.totalMs(context) / 60_000L).toInt(),
                            tamamlandi = true
                        )
                    }
                }
                val (evre, dakika, otomatik) = Pomodoro.sonrakiEvre(context)
                // Odak modu: molada DND kapanmalı
                Pomodoro.odagiEsitle(context, calisiyor = false)

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching {
                        TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
                        TimerEngine.setTotalMs(context, dakika * 60_000L)
                        if (otomatik) {
                            TimerEngine.start(context)
                            TimerAlarm.reschedule(context)
                            Pomodoro.odagiEsitle(context, calisiyor = true)
                        } else {
                            TimerNotifier.show(context)
                        }
                        evreBildir(context, evre, dakika, otomatik)
                    }
                }, 2500)
                return
            }

            if (!SayacAyar.otomatikTekrar(context)) return
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
                    TimerEngine.start(context)
                    TimerAlarm.reschedule(context)
                }
            }, 2500)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Döngü ilerletilemedi", e)
        }
    }

    /** Yeni evreyi kısa bir bildirimle duyurur. */
    private fun evreBildir(context: Context, evre: Int, dakika: Int, otomatik: Boolean) {
        if (!Store.getNotifEnabled(context)) return
        try {
            val baslik = Pomodoro.evreAdi(context, evre)
            var metin = if (otomatik) {
                context.getString(R.string.pm_basladi, dakika)
            } else {
                context.getString(R.string.pm_hazir, dakika)
            }
            // v10.4 · A7: mola bildirimine dönüşümlü öneri. Çalışma
            // evresinde öneri eklenmez ("başla" sinyali saf kalsın).
            if (evre == Pomodoro.EVRE_KISA_MOLA || evre == Pomodoro.EVRE_UZUN_MOLA) {
                metin = MolaKisilik.govde(
                    metin, Pomodoro.tur(context),
                    uzunMola = evre == Pomodoro.EVRE_UZUN_MOLA
                )
            }
            val bildirim = NotificationCompat.Builder(context, DONE_CHANNEL)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(baslik)
                .setContentText(metin)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(
                                WidgetCommon.EXTRA_OPEN_SCREEN,
                                WidgetCommon.SCREEN_TIMER
                            )
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
            NotificationManagerCompat.from(context).notify(DONE_ID + 1, bildirim)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Evre bildirilemedi", e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // v10.7 · A6 — Zincir sayaç ilerlemesi
    // ══════════════════════════════════════════════════════════════

    /**
     * Zincir evresi bitince sıradakine geçer.
     *
     * Akış [Pomodoro] dalıyla aynı kalıptadır: bitiş hissi (ses,
     * titreşim) bir kez yaşandıktan sonra 2,5 sn'lik nefes payıyla
     * yeni evre kurulur. [SayacZincir.otoDevam] kapalıysa evre
     * hazır bekletilir ve panelden başlatılır.
     */
    private fun zincirIlerlet(context: Context) {
        try {
            val z = SayacZincir.aktif(context) ?: return
            val biten = SayacZincir.adim(context)
            val sonraki = SayacZincir.sonrakiAdim(z, biten)

            if (sonraki == null) {
                // Zincir tükendi — koşuyu kapat, sayacı başa sar
                SayacZincir.durdur(context)
                SayacZincir.sifirla(context)
                zincirBittiBildir(context, z)
                return
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    SayacZincir.setAdim(context, sonraki)
                    val evre = SayacZincir.adimdaki(z, sonraki)
                    val oto = SayacZincir.otoDevam(context)
                    TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
                    TimerEngine.setTotalMs(context, evre.sn * 1000L)
                    if (oto) {
                        TimerEngine.start(context)
                        TimerAlarm.reschedule(context)
                    } else {
                        TimerNotifier.show(context)
                    }
                    zincirEvreBildir(context, z, evre, sonraki, oto)
                }
            }, 2500)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Zincir ilerletilemedi", e)
        }
    }

    /** Yeni zincir evresini kısa bildirimle duyurur. */
    private fun zincirEvreBildir(
        context: Context,
        z: SayacZincir.Zincir,
        evre: SayacZincir.Evre,
        adim: Int,
        otomatik: Boolean
    ) {
        if (!Store.getNotifEnabled(context)) return
        try {
            val (n, toplam) = SayacZincir.kacinciAdim(z, adim)
            val sure = SayacZincir.sureMetni(evre.sn)
            val metin = if (otomatik) {
                context.getString(R.string.zk_basladi, sure, n, toplam)
            } else {
                context.getString(R.string.zk_hazir, sure, n, toplam)
            }
            val bildirim = NotificationCompat.Builder(context, DONE_CHANNEL)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("${z.emoji} ${evre.emoji} ${evre.ad}".trim())
                .setContentText(metin)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(
                                WidgetCommon.EXTRA_OPEN_SCREEN,
                                WidgetCommon.SCREEN_TIMER
                            )
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
            // Evre bildirimi pomodoronunkiyle aynı kimliği paylaşmaz;
            // ikisi de DONE_ID+1'i kullanırsa biri diğerini ezer.
            NotificationManagerCompat.from(context).notify(DONE_ID + 20, bildirim)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Zincir evresi bildirilemedi", e)
        }
    }

    /** Zincirin tamamı bittiğinde kutlama bildirimi. */
    private fun zincirBittiBildir(context: Context, z: SayacZincir.Zincir) {
        if (!Store.getNotifEnabled(context)) return
        try {
            val bildirim = NotificationCompat.Builder(context, DONE_CHANNEL)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.zk_bitti_baslik, z.ad))
                .setContentText(
                    context.getString(
                        R.string.zk_bitti_govde, z.toplamAdim, SayacZincir.sureMetni(z.toplamSn)
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(
                                WidgetCommon.EXTRA_OPEN_SCREEN,
                                WidgetCommon.SCREEN_TIMER
                            )
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
            NotificationManagerCompat.from(context).notify(DONE_ID + 21, bildirim)
        } catch (e: Exception) {
            android.util.Log.w("TimerActionReceiver", "Zincir bitişi bildirilemedi", e)
        }
    }

    /** Süre dolduğunda çalan, sesli/titreşimli bitiş bildirimi. */
    /** v10.2 · A1: tam ekran uyandırma izni (Android 14+ onayı). */
    private fun tamEkranIzniVar(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            try {
                context.getSystemService(NotificationManager::class.java)
                    ?.canUseFullScreenIntent() == true
            } catch (_: Exception) {
                false
            }
        } else {
            true
        }
    }

    /**
     * v10.2 · B26 — Erteleme vakti geldi: "şimdi başlat" hatırlatması.
     *
     * Bitiş bildiriminden farklı kanal kullanmaz; aynı yüksek önemli
     * kanal yeter. Eylem: Yeniden başlat / kapat (autoCancel).
     */
    private fun ertelemeBildirimi(context: Context) {
        if (!Store.getNotifEnabled(context)) return
        val ac = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TIMER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val baslat = PendingIntent.getBroadcast(
            context, 27,
            Intent(context, TimerActionReceiver::class.java).apply {
                action = ACTION_YENIDEN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dakika = (TimerEngine.totalMs(context) / 60_000L).toInt()
        val builder = NotificationCompat.Builder(context, DONE_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.sb_snooze_baslik))
            .setContentText(context.getString(R.string.sb_snooze_metin, dakika))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(ac)
            .addAction(0, context.getString(R.string.sb_snooze_baslat), baslat)

        runCatching {
            NotificationManagerCompat.from(context).notify(SNOOZE_ID, builder.build())
        }
    }

    private fun showDone(context: Context) {
        if (!Store.getNotifEnabled(context)) return
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                DONE_CHANNEL,
                context.getString(R.string.tn_done_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
        }

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_TIMER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = (TimerEngine.totalMs(context) / 60_000L).toInt()
        val builder = NotificationCompat.Builder(context, DONE_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.tn_done_title))
            .setContentText(context.getString(R.string.tn_done_text, minutes))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)

        // ── v10.2 · A1: uyanık bitiş — telefon uyurken tam ekran ──
        //
        // Android 14+'ta FSI izni kullanıcı onayına bağlı
        // (canUseFullScreenIntent). İzin yoksa bildirim yine yüksek
        // önemle gelir; ekransa ancak dokununca açılır.
        if (SayacAyar.uyanikBitis(context) && tamEkranIzniVar(context)) {
            val yakala = PendingIntent.getActivity(
                context, 26,
                Intent(context, SayacBittiActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(yakala, true)
        }

        // ── v10.2 · A2 + B26: bitiş eylemleri ──
        // Üç düğme: +5 dk uzat (oturum sürüyor), 1 sa ertele,
        // yarın sabah ertele. "Yeniden başlat" uyanık bitiş ekranında —
        // bildirim alanı üç düğmeyle sınırlı tutuldu.
        fun eylem(eylemAdi: String, req: Int, sec: Int = -1, uzatMs: Long = 0L): PendingIntent =
            PendingIntent.getBroadcast(
                context, req,
                Intent(context, TimerActionReceiver::class.java).apply {
                    action = eylemAdi
                    if (sec >= 0) putExtra(EXTRA_ERTELE_SEC, sec)
                    if (uzatMs > 0) putExtra(EXTRA_UZAT_MS, uzatMs)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        builder.addAction(
            0, context.getString(R.string.sb_uzat),
            eylem(ACTION_UZAT, 21, uzatMs = 5 * 60_000L)
        )
        builder.addAction(
            0, context.getString(R.string.sb_ertele_1sa),
            eylem(ACTION_ERTELE, 24, sec = SayacErtele.SEC_BIRSA)
        )
        builder.addAction(
            0, context.getString(R.string.sb_ertele_yarin),
            eylem(ACTION_ERTELE, 25, sec = SayacErtele.SEC_YARIN)
        )
        // v11.13: "Durdur" — alarm sesini/titreşimi bildirimden kes (güç
        // düğmesine ulaşamayan kullanıcı için ikinci emniyet ağı).
        builder.addAction(
            0, context.getString(R.string.sb_durdur_ses),
            eylem(ACTION_DURDUR_SES, 29)
        )

        // v7.86: ses ve titreşimi biz yönetiyoruz — bildirim çift çalmasın
        builder.setVibrate(longArrayOf(0L))
        builder.setSilent(true)

        try {
            NotificationManagerCompat.from(context).notify(DONE_ID, builder.build())
            // v7.56: sure bitti — telefon sessizde olsa bile duyulsun
            try {
                ZorunluUyari.bildirimeEslik(context, "zaman")
            } catch (e: Exception) {
                android.util.Log.w("TimerActionReceiver", "Israrli uyari calinamadi", e)
            }
        } catch (_: Exception) {
        }
    }
}
