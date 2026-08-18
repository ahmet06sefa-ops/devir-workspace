package com.gunlukasistan.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * v7.43 — Tüm bildirimlerin tek merkezi.
 *
 * ── Neden gerekti? ──
 * Bildirimler 7 ayrı dosyaya dağılmıştı, 6 kanal karışıktı ve tek bir
 * "bildirim aç/kapa" anahtarı vardı. Yeni bildirim eklemek kullanıcıyı
 * rahatsız etme riski taşıyordu — o da hepsini kapatırdı.
 *
 * ── Bu sınıf ne sağlar? ──
 *  26. Tür bazlı açma/kapama (16 ayrı anahtar)
 *  27. Rahatsız etmeyin saatleri (varsayılan 23:00–08:00)
 *  28. Bildirim gruplama (setGroup ile tek başlık altında)
 *  29. Kanal ayrıştırma (5 grup, 9 kanal)
 *  + Günlük gönderim tavanı — bildirim yorgunluğuna karşı
 *
 * ── Tasarım ilkesi ──
 * Yeni bildirimlerin çoğu VARSAYILAN KAPALI gelir. Kullanıcı istediğini açar.
 * Böylece güncelleme sonrası aniden bildirim yağmuru olmaz.
 */
object BildirimMerkezi {

    private const val TAG = "BildirimMerkezi"
    private const val PREF = "bildirim_ayar_v1"

    // ═══════════════════════════════════════════════════════════════
    // 29. KANAL GRUPLARI VE KANALLAR
    // ═══════════════════════════════════════════════════════════════

    const val GRUP_HATIRLATICI = "grp_hatirlatici"
    const val GRUP_OGRENME = "grp_ogrenme"
    const val GRUP_BASARIM = "grp_basarim"
    const val GRUP_ARKAPLAN = "grp_arkaplan"
    const val GRUP_RAPOR = "grp_rapor"

    /** Görev/etkinlik hatırlatmaları — yüksek öncelik. */
    const val K_HATIRLATICI = "ch_hatirlatici_v2"

    /** Kart/quiz tekrarı, yarım ders — orta öncelik. */
    const val K_OGRENME = "ch_ogrenme_v2"

    /** Rozet, rekor, hedef kutlaması — orta öncelik. */
    const val K_BASARIM = "ch_basarim_v2"

    /** Zamanlayıcı canlı bildirimi — sessiz, kalıcı. */
    const val K_ZAMANLAYICI = "ch_zamanlayici_v2"

    /** Zamanlayıcı bitişi — yüksek, sesli. */
    const val K_ZAMANLAYICI_BITIS = "ch_zaman_bitis_v2"

    /** PDF indeksleme, kurs üretimi gibi arka plan işleri — sessiz. */
    const val K_ARKAPLAN = "ch_arkaplan_v2"

    /** Haftalık/aylık raporlar. */
    const val K_RAPOR = "ch_rapor_v2"

    /** Motivasyon, geri dönüş daveti — düşük öncelik. */
    const val K_MOTIVASYON = "ch_motivasyon_v2"

    /** Sesli ders oynatıcı. */
    const val K_SESLI = "ch_sesli_v2"

    // ═══════════════════════════════════════════════════════════════
    // 26. TÜR BAZLI AYARLAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bildirim türleri.
     *
     * @param anahtar SharedPreferences anahtarı
     * @param kanal hangi kanaldan gönderilir
     * @param varsayilanAcik güncelleme sonrası açık mı gelsin
     * @param adRes kullanıcıya gösterilen ad
     */
    enum class Tur(
        val anahtar: String,
        val kanal: String,
        val varsayilanAcik: Boolean,
        val adRes: Int,
        val aciklamaRes: Int
    ) {
        // Hatırlatıcılar — eskiden beri var, açık kalsın
        GOREV("n_gorev", K_HATIRLATICI, true, R.string.nt_gorev, R.string.nt_gorev_d),
        KURS_GUNLUK("n_kurs", K_HATIRLATICI, true, R.string.nt_kurs, R.string.nt_kurs_d),

        // v10.3 · B23: sabah turunda "günün tek odağı"
        GUN_ODAK("n_godak", K_HATIRLATICI, true, R.string.nt_godak, R.string.nt_godak_d),

        // Öğrenme — YENİ, varsayılan açık (asıl değer burada)
        KART_TEKRAR("n_kart", K_OGRENME, true, R.string.nt_kart, R.string.nt_kart_d),
        QUIZ_TEKRAR("n_quiz", K_OGRENME, true, R.string.nt_quiz, R.string.nt_quiz_d),
        YARIM_DERS("n_yarim", K_OGRENME, false, R.string.nt_yarim, R.string.nt_yarim_d),
        UNUTMA("n_unutma", K_OGRENME, false, R.string.nt_unutma, R.string.nt_unutma_d),
        GUNLUK_KART("n_gkart", K_OGRENME, false, R.string.nt_gkart, R.string.nt_gkart_d),
        SINAV_SAYAC("n_sinav", K_OGRENME, true, R.string.nt_sinav, R.string.nt_sinav_d),

        // Başarım
        ROZET("n_rozet", K_BASARIM, true, R.string.nt_rozet, R.string.nt_rozet_d),
        HEDEF_TAMAM("n_hedef", K_BASARIM, true, R.string.nt_hedef, R.string.nt_hedef_d),
        SERI_REKOR("n_rekor", K_BASARIM, true, R.string.nt_rekor, R.string.nt_rekor_d),
        SERI_RISK("n_srisk", K_BASARIM, true, R.string.nt_srisk, R.string.nt_srisk_d),

        // Motivasyon — varsayılan kapalı, rahatsız edici olabilir
        HEDEF_ILERLEME("n_hilerleme", K_MOTIVASYON, false, R.string.nt_hilerleme, R.string.nt_hilerleme_d),
        GERI_DONUS("n_gdonus", K_MOTIVASYON, false, R.string.nt_gdonus, R.string.nt_gdonus_d),
        ODAK_ONERI("n_oodak", K_MOTIVASYON, false, R.string.nt_oodak, R.string.nt_oodak_d),
        UZUN_OTURUM("n_uzun", K_MOTIVASYON, true, R.string.nt_uzun, R.string.nt_uzun_d),

        // Raporlar
        HAFTALIK("n_hafta", K_RAPOR, true, R.string.nt_hafta, R.string.nt_hafta_d),
        AYLIK("n_ay", K_RAPOR, true, R.string.nt_ay, R.string.nt_ay_d),

        // v10.4 · B18: sessizde/tavanda yutulanların sabah özeti.
        // Kendisi de yutulabilir; biriktirme istisnası döngüyü keser.
        OZET("n_ozet", K_RAPOR, true, R.string.nt_ozet, R.string.nt_ozet_d),

        // Arka plan
        ARKAPLAN_IS("n_arka", K_ARKAPLAN, true, R.string.nt_arka, R.string.nt_arka_d),
        YEDEK("n_yedek", K_ARKAPLAN, false, R.string.nt_yedek, R.string.nt_yedek_d),

        // v11.13: proaktif akıllı koç — günün vaktine göre yönlendirme
        KOC("n_koc", K_MOTIVASYON, true, R.string.nt_koc, R.string.nt_koc_d);
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Bu bildirim türü açık mı? */
    fun acikMi(context: Context, tur: Tur): Boolean {
        // Ana anahtar kapalıysa hiçbiri gönderilmez
        if (!Store.getNotifEnabled(context)) return false
        return prefs(context).getBoolean(tur.anahtar, tur.varsayilanAcik)
    }

    fun ayarla(context: Context, tur: Tur, acik: Boolean) {
        prefs(context).edit().putBoolean(tur.anahtar, acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // 27. RAHATSIZ ETMEYİN
    // ═══════════════════════════════════════════════════════════════

    private const val K_SESSIZ_ACIK = "sessiz_acik"
    private const val K_SESSIZ_BAS = "sessiz_bas"
    private const val K_SESSIZ_BIT = "sessiz_bit"

    fun sessizModAcik(context: Context): Boolean =
        prefs(context).getBoolean(K_SESSIZ_ACIK, true)

    fun setSessizMod(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean(K_SESSIZ_ACIK, acik).apply()
    }

    /** Sessiz saat başlangıcı (varsayılan 23). */
    fun sessizBaslangic(context: Context): Int =
        prefs(context).getInt(K_SESSIZ_BAS, 23)

    /** Sessiz saat bitişi (varsayılan 8). */
    fun sessizBitis(context: Context): Int =
        prefs(context).getInt(K_SESSIZ_BIT, 8)

    /**
     * v10.15 · C14: global sessiz pencereyi DAKİKA olarak verir.
     * Sessiz mod kapalıysa (0, 0) döner — `SessizTurler.sessizdeMi`
     * bas==bit'i "pencere yok" sayar, yani kapalı global karar vermez.
     */
    fun globalSessizPencere(context: Context): Pair<Int, Int> =
        if (!sessizModAcik(context)) 0 to 0
        else sessizBaslangic(context) * 60 to sessizBitis(context) * 60

    fun setSessizSaatler(context: Context, baslangic: Int, bitis: Int) {
        prefs(context).edit()
            .putInt(K_SESSIZ_BAS, baslangic.coerceIn(0, 23))
            .putInt(K_SESSIZ_BIT, bitis.coerceIn(0, 23))
            .apply()
    }

    /**
     * Şu an sessiz saatler içinde miyiz?
     * Gece yarısını aşan aralıkları da doğru hesaplar (23:00–08:00 gibi).
     */
    fun sessizSaatteMi(context: Context): Boolean {
        if (!sessizModAcik(context)) return false
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val bas = sessizBaslangic(context)
        val bit = sessizBitis(context)
        return if (bas <= bit) saat in bas until bit else (saat >= bas || saat < bit)
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜNLÜK TAVAN — bildirim yorgunluğuna karşı
    // ═══════════════════════════════════════════════════════════════

    private const val K_TAVAN = "gunluk_tavan"
    private const val K_SAYAC_GUN = "sayac_gun"
    private const val K_SAYAC = "sayac"

    /** Günde en fazla kaç "isteğe bağlı" bildirim (varsayılan 6). */
    fun gunlukTavan(context: Context): Int = prefs(context).getInt(K_TAVAN, 6)

    fun setGunlukTavan(context: Context, adet: Int) {
        prefs(context).edit().putInt(K_TAVAN, adet.coerceIn(1, 30)).apply()
    }

    private fun bugunAnahtari(): String {
        val c = Calendar.getInstance()
        return "" + c.get(Calendar.YEAR) + c.get(Calendar.DAY_OF_YEAR)
    }

    /** Tavan aşıldı mı? Aşıldıysa bildirim gönderilmez. */
    private fun tavanAsildiMi(context: Context): Boolean {
        val p = prefs(context)
        val bugun = bugunAnahtari()
        if (p.getString(K_SAYAC_GUN, "") != bugun) return false
        return p.getInt(K_SAYAC, 0) >= gunlukTavan(context)
    }

    private fun sayaciArtir(context: Context) {
        val p = prefs(context)
        val bugun = bugunAnahtari()
        if (p.getString(K_SAYAC_GUN, "") != bugun) {
            p.edit().putString(K_SAYAC_GUN, bugun).putInt(K_SAYAC, 1).apply()
        } else {
            p.edit().putInt(K_SAYAC, p.getInt(K_SAYAC, 0) + 1).apply()
        }
    }

    /** Aynı bildirimin gün içinde tekrarlanmasını önler. */
    fun bugunGonderildiMi(context: Context, etiket: String): Boolean =
        prefs(context).getString("son_" + etiket, "") == bugunAnahtari()

    fun bugunIsaretle(context: Context, etiket: String) {
        prefs(context).edit().putString("son_" + etiket, bugunAnahtari()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // KANAL KURULUMU
    // ═══════════════════════════════════════════════════════════════

    /** Tüm kanalları ve grupları oluşturur. Uygulama açılışında çağrılır. */
    fun kanallariKur(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        try {
            // 29. Gruplar — sistem ayarlarında düzenli görünsün
            listOf(
                GRUP_HATIRLATICI to R.string.ng_hatirlatici,
                GRUP_OGRENME to R.string.ng_ogrenme,
                GRUP_BASARIM to R.string.ng_basarim,
                GRUP_ARKAPLAN to R.string.ng_arkaplan,
                GRUP_RAPOR to R.string.ng_rapor
            ).forEach { (id, adRes) ->
                nm.createNotificationChannelGroup(
                    NotificationChannelGroup(id, context.getString(adRes))
                )
            }

            data class KanalTanim(
                val id: String, val adRes: Int, val onem: Int, val grup: String
            )

            listOf(
                KanalTanim(K_HATIRLATICI, R.string.nc_hatirlatici,
                    NotificationManager.IMPORTANCE_HIGH, GRUP_HATIRLATICI),
                KanalTanim(K_OGRENME, R.string.nc_ogrenme,
                    NotificationManager.IMPORTANCE_DEFAULT, GRUP_OGRENME),
                KanalTanim(K_BASARIM, R.string.nc_basarim,
                    NotificationManager.IMPORTANCE_DEFAULT, GRUP_BASARIM),
                KanalTanim(K_MOTIVASYON, R.string.nc_motivasyon,
                    NotificationManager.IMPORTANCE_LOW, GRUP_BASARIM),
                KanalTanim(K_ZAMANLAYICI, R.string.nc_zamanlayici,
                    NotificationManager.IMPORTANCE_LOW, GRUP_HATIRLATICI),
                KanalTanim(K_ZAMANLAYICI_BITIS, R.string.nc_zaman_bitis,
                    NotificationManager.IMPORTANCE_HIGH, GRUP_HATIRLATICI),
                KanalTanim(K_ARKAPLAN, R.string.nc_arkaplan,
                    NotificationManager.IMPORTANCE_LOW, GRUP_ARKAPLAN),
                KanalTanim(K_RAPOR, R.string.nc_rapor,
                    NotificationManager.IMPORTANCE_DEFAULT, GRUP_RAPOR),
                KanalTanim(K_SESLI, R.string.nc_sesli,
                    NotificationManager.IMPORTANCE_LOW, GRUP_ARKAPLAN)
            ).forEach { t ->
                if (nm.getNotificationChannel(t.id) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(t.id, context.getString(t.adRes), t.onem).apply {
                            group = t.grup
                            if (t.onem <= NotificationManager.IMPORTANCE_LOW) {
                                enableVibration(false)
                                setSound(null, null)
                            }
                        }
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kanallar kurulamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖNDERİM
    // ═══════════════════════════════════════════════════════════════

    /** 28. Gruplama anahtarı — bildirimler tek başlık altında toplanır. */
    private const val GRUP_ANAHTAR = "gunluk_asistan_grubu"

    /**
     * Bildirim gönderir. Tüm kontroller burada yapılır:
     *  · Tür açık mı
     *  · Sessiz saatte miyiz
     *  · Günlük tavan aşıldı mı
     *  · İzin var mı
     *
     * @param acil true ise sessiz saat ve tavan kontrolü atlanır
     *             (zamanlayıcı bitişi gibi kullanıcının beklediği bildirimler)
     * @return gönderildiyse true
     */
    fun gonder(
        context: Context,
        tur: Tur,
        id: Int,
        baslik: String,
        metin: String,
        genisMetin: String = "",
        acil: Boolean = false,
        eylemler: List<Pair<String, PendingIntent>> = emptyList(),
        acilisIntent: PendingIntent? = null,
        ilerleme: Pair<Int, Int>? = null
    ): Boolean {
        if (!acikMi(context, tur)) return false
        if (!acil) {
            val sessiz = sessizSaatteMi(context)
            val tavan = tavanAsildiMi(context)
            if (sessiz || tavan) {
                // v10.4 · B18: yutulan başlık deftere düşer; sabah
                // turu tek özet bildirimle teslim eder. Özetin
                // kendisi biriktirilmez — sonsuz döngü olmasın.
                if (tur != Tur.OZET) {
                    runCatching { BildirimOzeti.biriktir(context, baslik) }
                }
                return false
            }
        }

        return try {
            val builder = NotificationCompat.Builder(context, tur.kanal)
                .setSmallIcon(R.drawable.ic_task_alt)
                .setContentTitle(baslik)
                .setContentText(metin)
                .setAutoCancel(true)
                .setGroup(GRUP_ANAHTAR)
                .setPriority(
                    if (acil) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )

            if (genisMetin.isNotBlank()) {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(genisMetin))
            }
            acilisIntent?.let { builder.setContentIntent(it) }
            ilerleme?.let { (simdi, toplam) ->
                builder.setProgress(toplam, simdi, false)
            }
            eylemler.take(3).forEach { (etiket, pi) ->
                builder.addAction(0, etiket, pi)
            }
            if (!Store.getVibEnabled(context)) builder.setVibrate(longArrayOf(0L))
            if (!Store.getSoundEnabled(context)) builder.setSound(null)

            NotificationManagerCompat.from(context).notify(id, builder.build())
            if (!acil) sayaciArtir(context)
            // v10.3 · B20 — grup özeti artık gerçekten yayınlanıyor.
            // grupOzetiGonder() v7.56'dan beri tanımlıydı ama hiçbir
            // yerden çağrılmıyordu (ölü kod). Panelde grupta 2+
            // bildirim birikince özet üretilir; Android bunları tek
            // başlık altında katlar.
            runCatching {
                val nm = context.getSystemService(NotificationManager::class.java)
                val aktif = nm?.activeNotifications?.count {
                    it.notification?.group == GRUP_ANAHTAR
                } ?: 0
                if (aktif >= 2) grupOzetiGonder(context, aktif)
            }
            // v7.56: israrli uyari — telefon sessizde olsa da duyulsun.
            // Yalnizca hatirlatici kanallarinda, arka plan/rapor bildiriminde degil.
            if (tur.kanal == K_HATIRLATICI) {
                try {
                    ZorunluUyari.bildirimeEslik(context, "gorev")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Israrli uyari calinamadi", e)
                }
            }
            true
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Bildirim izni yok", e)
            false
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bildirim gönderilemedi", e)
            false
        }
    }

    /** Uygulamayı belirli bir ekranda açan niyet. */
    fun ekranAc(context: Context, ekran: Int, istekKodu: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, ekran)
            data = android.net.Uri.parse("gunlukasistan://notif/" + istekKodu)
        }
        return PendingIntent.getActivity(
            context, istekKodu, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Bir Activity'yi doğrudan açan niyet. */
    fun aktiviteAc(context: Context, sinif: Class<*>, istekKodu: Int): PendingIntent {
        val intent = Intent(context, sinif).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("gunlukasistan://act/" + istekKodu)
        }
        return PendingIntent.getActivity(
            context, istekKodu, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 28. Grup özeti — Android bildirimleri tek başlık altında toplasın. */
    fun grupOzetiGonder(context: Context, adet: Int) {
        if (Build.VERSION.SDK_INT < 24 || adet < 2) return
        try {
            val ozet = NotificationCompat.Builder(context, K_OGRENME)
                .setSmallIcon(R.drawable.ic_task_alt)
                .setContentTitle(context.getString(R.string.n_group_title))
                .setContentText(context.getString(R.string.n_group_text, adet))
                .setGroup(GRUP_ANAHTAR)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(9000, ozet)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Grup özeti gönderilemedi", e)
        }
    }
}
