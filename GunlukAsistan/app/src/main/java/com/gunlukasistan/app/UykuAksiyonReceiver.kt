package com.gunlukasistan.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * v10.9 — Gün çerçevesi kapıları ve eylem düğmeleri.
 *
 * ── Akış ──
 *   SABAH 07:00 (kullanıcı saati) · "🌅 Uyandın mı?" (varsayılan SESSİZ)
 *      ├── [✅ Uyandım] → deftere yaz, gün başlasın
 *      │     (BildirimUretici.tumKontroller(sabahMi=true) — eski 09:00
 *      │      rutininin tüm içeriği: özet, gün odağı, günlük kart…)
 *      ├── [😴 Ertele] → tekrar aralığı kadar ileri at
 *      └── cevap yok → tekrar zinciri; hepsi tükenirse
 *            · son çare AÇIK: gün yine de başlar
 *            · son çare KAPALI: o gün sessiz kalınır
 *
 *   AKŞAM 23:00 (kullanıcı saati) · "🌙 Gün bitti" + gün özeti
 *   (odak / görev / pomodoro / seri — her satır ayrı kapatılabilir)
 *      ├── [😴 Uyuyorum] → deftere yaz, "iyi geceler" notu
 *      └── cevap yok → tekrar zinciri (sayaç sınırlı)
 *
 * ── Neden merkez dışı gönderim ──
 * Bildirimler [BildirimMerkezi]`nden geçMEden doğrudan postalanır
 * ([TimerActionReceiver.evreBildir] örneği): bu kapılar kullanıcının
 * bizzat seçtiği saatlere kurulu alarmdır; merkezin sessiz saat /
 * günlük tavan kapılarına takılmaları istenmez. Genel anahtar
 * ([Store.getNotifEnabled]) yine de saygı görür.
 */
class UykuAksiyonReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UykuAksiyonReceiver"

        const val ACTION_SABAH = "com.gunlukasistan.app.UYKU_SABAH"
        const val ACTION_AKSAM = "com.gunlukasistan.app.UYKU_AKSAM"
        const val ACTION_SABAH_TEKRAR = "com.gunlukasistan.app.UYKU_SABAH_TEKRAR"
        const val ACTION_AKSAM_TEKRAR = "com.gunlukasistan.app.UYKU_AKSAM_TEKRAR"
        const val ACTION_UYANDIM = "com.gunlukasistan.app.UYKU_UYANDIM"
        const val ACTION_UYUYORUM = "com.gunlukasistan.app.UYKU_UYUYORUM"
        const val ACTION_SABAH_ERTELE = "com.gunlukasistan.app.UYKU_SABAH_ERTELE"
        const val ACTION_AKSAM_ERTELE = "com.gunlukasistan.app.UYKU_AKSAM_ERTELE"
        // Ayar ekranındaki "şimdi dene" satırları — durumu TÜKETMEZ.
        const val ACTION_TEST_SABAH = "com.gunlukasistan.app.UYKU_TEST_SABAH"
        const val ACTION_TEST_AKSAM = "com.gunlukasistan.app.UYKU_TEST_AKSAM"

        // Bildirim eylem düğmelerinin istek kodları (alarm kodlarından ayrı):
        private const val REQ_B_UYANDIM = 4841
        private const val REQ_B_SABAH_ERTELE = 4842
        private const val REQ_B_UYUYORUM = 4843
        private const val REQ_B_AKSAM_ERTELE = 4844

        fun elleSabahCalistir(context: Context) {
            UykuAksiyonReceiver().sabahKapisi(context)
        }

        fun elleAksamCalistir(context: Context) {
            UykuAksiyonReceiver().aksamKapisi(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            UykuZamanla.kanallariKur(context)
            when (intent.action) {
                ACTION_SABAH -> sabahKapisi(context)
                ACTION_SABAH_TEKRAR -> sabahTekrarKapisi(context)
                ACTION_UYANDIM -> uyandim(context)
                ACTION_SABAH_ERTELE -> sabahErtele(context)
                ACTION_AKSAM -> aksamKapisi(context)
                ACTION_AKSAM_TEKRAR -> aksamTekrarKapisi(context)
                ACTION_UYUYORUM -> uyuyorum(context)
                ACTION_AKSAM_ERTELE -> aksamErtele(context)
                ACTION_TEST_SABAH -> sabahSorusu(context, tekrarNo = 0)
                ACTION_TEST_AKSAM -> aksamOzeti(context, tekrarNo = 0)
            }
            // Zincir: her işlem sonrası ana alarmlar bir SONRAKİ güne
            // yeniden kurulur; çerçeve kapandıysa [kur] hepsini siler.
            UykuZamanla.kur(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Uyku aksiyonu işlenemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SABAH AKIŞI
    // ═══════════════════════════════════════════════════════════════

    /** Ana sabah alarmı (kullanıcının seçtiği saat) ateşlendi. */
    fun sabahKapisi(context: Context) {
        val simdi = System.currentTimeMillis()
        if (!UykuCerceve.acik(context) || !Store.getNotifEnabled(context)) return
        if (UykuCerceve.sabahVerildiMi(context, simdi)) return // gün zaten başladı
        UykuCerceve.setSabahTekrar(context, 0)
        if (!UykuCerceve.onaySart(context)) {
            // Soru sorulmuyor — saat gelince gün doğrudan başlar.
            gunuBaslat(context, simdi)
            return
        }
        sabahSorusu(context, tekrarNo = 0)
        if (UykuCerceve.tekrarGerekliMi(
                0, UykuCerceve.maksTekrarSabah(context),
                cevaplandi = false, verildi = false
            )
        ) {
            UykuZamanla.tekrarKur(context, sabahMi = true)
        }
    }

    /** Cevapsız sabah sorusunun tekrar alarmı. */
    private fun sabahTekrarKapisi(context: Context) {
        val simdi = System.currentTimeMillis()
        if (!UykuCerceve.acik(context) || !Store.getNotifEnabled(context)) return
        if (!UykuCerceve.onaySart(context)) return // tekrarlar yalnız onay akışında
        if (UykuCerceve.sabahVerildiMi(context, simdi)) return
        val yapilan = UykuCerceve.sabahTekrar(context)
        val maks = UykuCerceve.maksTekrarSabah(context)
        if (UykuCerceve.tekrarGerekliMi(yapilan, maks, cevaplandi = false, verildi = false)) {
            val yeni = UykuCerceve.sonrakiTekrar(yapilan)
            UykuCerceve.setSabahTekrar(context, yeni)
            sabahSorusu(context, tekrarNo = yeni)
            if (UykuCerceve.tekrarGerekliMi(yeni, maks, cevaplandi = false, verildi = false)) {
                UykuZamanla.tekrarKur(context, sabahMi = true)
            }
        } else {
            // Tüm tekrarlar tükendi, cevap yok:
            if (UykuCerceve.sonCare(context)) {
                gunuBaslat(context, simdi) // gün yine de başlar
            } else {
                // Bugün sessiz kalınır (kullanıcının tercihi, bedeli bu).
                UykuCerceve.sabahVerildi(context, simdi)
                runCatching {
                    NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_SABAH)
                }
            }
        }
    }

    /** [✅ Uyandım] düğmesi — defter kapanır, gün başlar. */
    private fun uyandim(context: Context) {
        val simdi = System.currentTimeMillis()
        UykuCerceve.uyandiKaydet(context, simdi)
        gunuBaslat(context, simdi)
    }

    /** [😴 Ertele] — sayaç YÜRÜMEZ; kullanıcı bilinçli erteledi. */
    private fun sabahErtele(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_SABAH) }
        UykuZamanla.tekrarKur(context, sabahMi = true)
    }

    /**
     * Günün başlatıldığı tek nokta: "uyandım" onayı, onaysız kurulum
     * ve son-çare teslimatı buradan geçer. Eski 09:00 rutininin tüm
     * içeriği ([BildirimUretici.tumKontroller]) teslim edilir.
     */
    private fun gunuBaslat(context: Context, simdi: Long) {
        UykuCerceve.sabahVerildi(context, simdi)
        UykuCerceve.setSabahTekrar(context, 0)
        UykuZamanla.tekrarIptal(context, sabahMi = true)
        runCatching { NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_SABAH) }
        try {
            BildirimUretici.tumKontroller(context, sabahMi = true)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah rutini teslim edilemedi", e)
        }
        // v10.14 · E25: sabah planı taslağı — içerik varsa bayrak kalkar,
        // ana ekran ilk açıldığında tek seferlik diyalog gösterilir.
        try {
            val bugunSon = WidgetCommon.endOfToday()
            val taslak = SabahPlani.sec(
                Store.loadTasks(context).map {
                    SabahPlani.GorevOzet(it.id, it.text, it.dueAt, it.done)
                },
                bugunSon - 86_399_999L, bugunSon
            )
            if (taslak.isNotEmpty()) SabahPlani.beklemeyeAl(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah planı hazırlanamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AKŞAM AKIŞI
    // ═══════════════════════════════════════════════════════════════

    /** Ana akşam alarmı ateşlendi — soru + gün özeti. */
    fun aksamKapisi(context: Context) {
        val simdi = System.currentTimeMillis()
        if (!UykuCerceve.acik(context) || !Store.getNotifEnabled(context)) return
        if (UykuCerceve.aksamVerildiMi(context, simdi)) return
        UykuCerceve.setAksamTekrar(context, 0)
        aksamOzeti(context, tekrarNo = 0)
        if (UykuCerceve.tekrarGerekliMi(
                0, UykuCerceve.maksTekrarAksam(context),
                cevaplandi = false, verildi = false
            )
        ) {
            UykuZamanla.tekrarKur(context, sabahMi = false)
        }
    }

    /** Cevapsız akşam sorusunun tekrar alarmı. */
    private fun aksamTekrarKapisi(context: Context) {
        val simdi = System.currentTimeMillis()
        if (!UykuCerceve.acik(context) || !Store.getNotifEnabled(context)) return
        if (UykuCerceve.aksamVerildiMi(context, simdi)) return
        val yapilan = UykuCerceve.aksamTekrar(context)
        val maks = UykuCerceve.maksTekrarAksam(context)
        if (UykuCerceve.tekrarGerekliMi(yapilan, maks, cevaplandi = false, verildi = false)) {
            val yeni = UykuCerceve.sonrakiTekrar(yapilan)
            UykuCerceve.setAksamTekrar(context, yeni)
            aksamOzeti(context, tekrarNo = yeni)
            if (UykuCerceve.tekrarGerekliMi(yeni, maks, cevaplandi = false, verildi = false)) {
                UykuZamanla.tekrarKur(context, sabahMi = false)
            }
        } else {
            // Akşamda "son çare" yok: özet azami kez gösterildi, gece bitti.
            UykuCerceve.aksamVerildi(context, simdi)
            runCatching {
                NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_AKSAM)
            }
        }
    }

    /** [😴 Uyuyorum] düğmesi — deftere uyuma yazılır. */
    private fun uyuyorum(context: Context) {
        val simdi = System.currentTimeMillis()
        UykuCerceve.uyuduKaydet(context, simdi)
        UykuCerceve.aksamVerildi(context, simdi)
        UykuCerceve.setAksamTekrar(context, 0)
        UykuZamanla.tekrarIptal(context, sabahMi = false)
        runCatching { NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_AKSAM) }
        iyiGeceler(context)
    }

    /** [😴 Ertele] — sayaç yürümez, tek seferlik erteleme. */
    private fun aksamErtele(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(UykuCerceve.BILDIRIM_AKSAM) }
        UykuZamanla.tekrarKur(context, sabahMi = false)
    }

    // ═══════════════════════════════════════════════════════════════
    // BİLDİRİMLER
    // ═══════════════════════════════════════════════════════════════

    private fun yayin(context: Context, eylem: String, kod: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, kod,
            Intent(context, UykuAksiyonReceiver::class.java).setAction(eylem),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun uygulamayiAc(context: Context, kod: Int): PendingIntent =
        PendingIntent.getActivity(
            context, kod,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** "🌅 Uyandın mı?" — varsayılan SESSİZ kanal (kullanıcının isteği). */
    private fun sabahSorusu(context: Context, tekrarNo: Int) {
        try {
            val sessiz = UykuCerceve.sabahSessiz(context)
            val kanal = if (sessiz) UykuCerceve.KANAL_SABAH_SESSIZ else UykuCerceve.KANAL_SABAH_SESLI
            var metin = context.getString(R.string.uy_sabah_metin)
            val maks = UykuCerceve.maksTekrarSabah(context)
            if (tekrarNo > 0 && maks > 0) {
                metin += " " + context.getString(R.string.uy_sabah_tekrar_etiketi, tekrarNo, maks)
            }
            val b = NotificationCompat.Builder(context, kanal)
                .setSmallIcon(R.drawable.ic_uyku_sabah)
                .setContentTitle(context.getString(R.string.uy_sabah_baslik))
                .setContentText(metin)
                .setStyle(NotificationCompat.BigTextStyle().bigText(metin))
                .setPriority(
                    if (sessiz) NotificationCompat.PRIORITY_LOW
                    else NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(uygulamayiAc(context, UykuCerceve.BILDIRIM_SABAH))
                .addAction(
                    0, context.getString(R.string.uy_eylem_uyandim),
                    yayin(context, ACTION_UYANDIM, REQ_B_UYANDIM)
                )
                .addAction(
                    0,
                    context.getString(R.string.uy_eylem_ertele, UykuCerceve.tekrarDkSabah(context)),
                    yayin(context, ACTION_SABAH_ERTELE, REQ_B_SABAH_ERTELE)
                )
            NotificationManagerCompat.from(context).notify(UykuCerceve.BILDIRIM_SABAH, b.build())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah sorusu gönderilemedi", e)
        }
    }

    /**
     * "🌙 Gün bitti — uyuyacak mısın?" + seçili özet satırları.
     * Değeri 0 olan satırlar düşülür; hiç satır kalmazsa yumuşak
     * bir kapanış cümlesi gösterilir.
     */
    private fun aksamOzeti(context: Context, tekrarNo: Int) {
        try {
            val simdi = System.currentTimeMillis()
            val bugun = UykuCerceve.gunKey(simdi)
            val secim = UykuCerceve.ozetSecimi(
                UykuCerceve.ozetOdak(context),
                UykuCerceve.ozetGorev(context),
                UykuCerceve.ozetSeri(context),
                UykuCerceve.ozetZincir(context)
            )
            val satirlar = mutableListOf<String>()
            secim.forEach { parca ->
                when (parca) {
                    UykuCerceve.OzetParca.ODAK -> {
                        val dk = runCatching { Store.getTodayFocusMinutes(context) }.getOrDefault(0)
                        if (dk > 0) {
                            satirlar.add(context.getString(R.string.uy_aksam_satir_odak, dk))
                        }
                    }
                    UykuCerceve.OzetParca.GOREV -> {
                        val n = runCatching {
                            Store.gunlukKayitKopyasi(context)
                                .optJSONObject(bugun)?.optInt("c", 0) ?: 0
                        }.getOrDefault(0)
                        if (n > 0) {
                            satirlar.add(context.getString(R.string.uy_aksam_satir_gorev, n))
                        }
                    }
                    UykuCerceve.OzetParca.ZINCIR -> {
                        val n = runCatching {
                            SureAnalizi.pomodorolar(context).count {
                                it.tamamlandi && UykuCerceve.gunKey(it.zaman) == bugun
                            }
                        }.getOrDefault(0)
                        if (n > 0) {
                            satirlar.add(context.getString(R.string.uy_aksam_satir_zincir, n))
                        }
                    }
                    UykuCerceve.OzetParca.SERI -> {
                        val seri = runCatching { Store.streakInfo(context).first }.getOrDefault(0)
                        if (seri > 0) {
                            satirlar.add(context.getString(R.string.uy_aksam_satir_seri, seri))
                        }
                    }
                }
            }
            var govde = if (satirlar.isEmpty()) {
                context.getString(R.string.uy_aksam_bos)
            } else {
                satirlar.joinToString("\n")
            }
            val maks = UykuCerceve.maksTekrarAksam(context)
            if (tekrarNo > 0 && maks > 0) {
                govde += "\n" + context.getString(R.string.uy_sabah_tekrar_etiketi, tekrarNo, maks)
            }
            val b = NotificationCompat.Builder(context, UykuCerceve.KANAL_AKSAM)
                .setSmallIcon(R.drawable.ic_uyku_aksam)
                .setContentTitle(context.getString(R.string.uy_aksam_baslik))
                .setContentText(govde.replace("\n", " · "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(govde))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(uygulamayiAc(context, UykuCerceve.BILDIRIM_AKSAM))
                .addAction(
                    0, context.getString(R.string.uy_eylem_uyuyorum),
                    yayin(context, ACTION_UYUYORUM, REQ_B_UYUYORUM)
                )
                .addAction(
                    0,
                    context.getString(R.string.uy_eylem_ertele, UykuCerceve.tekrarDkAksam(context)),
                    yayin(context, ACTION_AKSAM_ERTELE, REQ_B_AKSAM_ERTELE)
                )
            NotificationManagerCompat.from(context).notify(UykuCerceve.BILDIRIM_AKSAM, b.build())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Akşam özeti gönderilemedi", e)
        }
    }

    /** "Uyuyorum" notası — sabaha kadar paneli kirletmesin diye 4 saatte söner. */
    private fun iyiGeceler(context: Context) {
        try {
            val sabah = UykuCerceve.saatMetni(UykuCerceve.sabahDk(context))
            val metin = context.getString(R.string.uy_iyi_geceler_metin, sabah)
            val b = NotificationCompat.Builder(context, UykuCerceve.KANAL_AKSAM)
                .setSmallIcon(R.drawable.ic_uyku_aksam)
                .setContentTitle(context.getString(R.string.uy_iyi_geceler_baslik))
                .setContentText(metin)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(uygulamayiAc(context, UykuCerceve.BILDIRIM_IYIGECELER))
                .setTimeoutAfter(4 * 3600_000L)
                // v10.14 · E27: mikro günlük kapısı — günü 3 soruyla kapat
                .addAction(
                    R.drawable.ic_uyku_aksam,
                    context.getString(R.string.ge_gunluk_aksiyon),
                    PendingIntent.getActivity(
                        context, 5000,
                        Intent(context, MikroGunlukActivity::class.java).apply {
                            data = android.net.Uri.parse("gunlukasistan://mikrogunluk")
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            NotificationManagerCompat.from(context)
                .notify(UykuCerceve.BILDIRIM_IYIGECELER, b.build())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İyi geceler notu gönderilemedi", e)
        }
    }
}
