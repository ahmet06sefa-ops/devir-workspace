package com.gunlukasistan.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * v7.45 — Widget'lardan uygulama AÇILMADAN iş yapan merkezi alıcı.
 *
 * ── Sorun ──
 * Denetim şunu gösterdi:
 *   SummaryWidget    → 0 gerçek eylem, 3 "uygulamayı aç"
 *   CountdownWidget  → 0 gerçek eylem, 1 "uygulamayı aç"
 *   ActionsWidget    → 0 gerçek eylem, 2 "uygulamayı aç"
 *
 * Yani widget'ların çoğu sadece bir kısayoldu. Kullanıcı odak başlatmak
 * için bile uygulamayı açmak zorundaydı.
 *
 * ── Çözüm ──
 * Tüm hızlı eylemler tek bir BroadcastReceiver'da toplandı. Widget'a
 * dokunulduğunda iş anında yapılır, kısa bir Toast gösterilir ve tüm
 * widget'lar tazelenir. Uygulama hiç açılmaz.
 *
 * ── Neden tek alıcı? ──
 * Her widget'a ayrı receiver yazmak manifest'i şişirir ve kod tekrarı
 * yaratırdı. Tek alıcı + eylem parametresi hem sade hem genişletilebilir.
 */
class WidgetEylem : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.gunlukasistan.app.WIDGET_EYLEM"
        const val EXTRA_IS = "eylem_is"
        const val EXTRA_DEGER = "eylem_deger"

        // ── Zamanlayıcı ──
        const val IS_ODAK_25 = "odak25"
        const val IS_ODAK_45 = "odak45"
        const val IS_ODAK_DUR = "odak_dur"

        // ── Sayaçlar ──
        const val IS_SORU_ARTIR = "soru+"
        const val IS_DERS_ISARETLE = "ders_isaretle"

        /** v7.95: program adımını widget'tan bitir (öneri 4). */
        const val IS_ADIM_BITIR = "adim_bitir"

        // ── Bilgi kartı ──
        const val IS_KART_CEVIR = "kart_cevir"
        const val IS_KART_BILDIM = "kart_bildim"
        const val IS_KART_BILMEDIM = "kart_bilmedim"

        // ── Görev ──
        const val IS_GOREV_ERTELE = "gorev_ertele"

        /** Widget düğmesi için hazır PendingIntent üretir. */
        fun niyet(context: Context, is_: String, istekKodu: Int, deger: Long = 0L): PendingIntent {
            val intent = Intent(context, WidgetEylem::class.java).apply {
                action = ACTION
                putExtra(EXTRA_IS, is_)
                if (deger != 0L) putExtra(EXTRA_DEGER, deger)
                // Her eylem ayrı Uri almalı, yoksa PendingIntent'ler birbirini ezer
                data = android.net.Uri.parse("gunlukasistan://eylem/" + is_ + "/" + istekKodu)
            }
            return PendingIntent.getBroadcast(
                context, istekKodu, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // ═══════════════════════════════════════════════════════════
        // KART OTURUMU — widget üzerinde kart çevirme durumu
        // ═══════════════════════════════════════════════════════════

        private const val PREF = "widget_eylem_v1"
        private const val K_KART_ID = "kart_id"
        private const val K_KART_ACIK = "kart_acik"

        /** Widget'ta gösterilen kartın kimliği (0 = henüz seçilmedi). */
        fun aktifKartId(context: Context): Long =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_KART_ID, 0L)

        /** Kartın arka yüzü açık mı? */
        fun kartAcikMi(context: Context): Boolean =
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(K_KART_ACIK, false)

        fun kartAyarla(context: Context, kartId: Long, acik: Boolean) {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong(K_KART_ID, kartId)
                .putBoolean(K_KART_ACIK, acik)
                .apply()
        }

        /**
         * Widget'ta gösterilecek kartı verir.
         * Kayıtlı kart hâlâ geçerliyse onu, değilse sıradakini seçer.
         */
        fun gecerliKart(context: Context): KartStore.Kart? {
            return try {
                val bekleyenler = KartStore.bugunkuKartlar(context, limit = 40)
                if (bekleyenler.isEmpty()) return null
                val kayitli = aktifKartId(context)
                bekleyenler.firstOrNull { it.id == kayitli } ?: bekleyenler.first().also {
                    kartAyarla(context, it.id, false)
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetEylem", "Kart okunamadı", e)
                null
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val is_ = intent.getStringExtra(EXTRA_IS).orEmpty()
        val deger = intent.getLongExtra(EXTRA_DEGER, 0L)

        try {
            when (is_) {
                IS_ODAK_25 -> odakBaslat(context, 25)
                IS_ODAK_45 -> odakBaslat(context, 45)
                IS_ODAK_DUR -> odakDurdur(context)
                IS_SORU_ARTIR -> soruArtir(context)
                IS_DERS_ISARETLE -> dersIsaretle(context)
                IS_KART_CEVIR -> kartCevir(context)
                IS_KART_BILDIM -> kartCevapla(context, true)
                IS_KART_BILMEDIM -> kartCevapla(context, false)
                IS_GOREV_ERTELE -> gorevErtele(context, deger)
                IS_ADIM_BITIR -> adimBitir(context)
                else -> return
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Eylem başarısız: " + is_, e)
        }

        // Her eylemden sonra tüm widget'lar güncel olmalı
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Tazeleme başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ZAMANLAYICI
    // ═══════════════════════════════════════════════════════════════

    /** Uygulama açılmadan odak oturumu başlatır. */
    private fun odakBaslat(context: Context, dakika: Int) {
        TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
        TimerEngine.setTotalMs(context, dakika * 60_000L)
        TimerEngine.reset(context)
        TimerEngine.start(context)
        try {
            TimerNotifier.show(context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Zamanlayıcı bildirimi açılamadı", e)
        }
        mesaj(context, context.getString(R.string.we_odak_basladi, dakika))
    }

    private fun odakDurdur(context: Context) {
        TimerEngine.pause(context)
        mesaj(context, context.getString(R.string.we_odak_durdu))
    }

    // ═══════════════════════════════════════════════════════════════
    // SAYAÇLAR
    // ═══════════════════════════════════════════════════════════════

    /** Çözülen soru sayısını 1 artırır — en sık yapılan işlem. */
    private fun soruArtir(context: Context) {
        Store.addQuestions(context, 1)
        val toplam = Store.getTodayQuestions(context)
        mesaj(context, context.getString(R.string.we_soru, toplam))
    }

    /** Bugünü kurs serisinde işaretler. */
    private fun dersIsaretle(context: Context) {
        Store.kursGunuIsaretle(context)
        val seri = Store.kursSeri(context)
        mesaj(context, context.getString(R.string.we_ders, seri.gunSayisi))
    }

    // ═══════════════════════════════════════════════════════════════
    // BİLGİ KARTI — widget üzerinde tam tekrar döngüsü
    // ═══════════════════════════════════════════════════════════════

    private fun kartCevir(context: Context) {
        val kart = gecerliKart(context) ?: return
        kartAyarla(context, kart.id, !kartAcikMi(context))
    }

    /**
     * Kartı cevaplar ve sıradakine geçer.
     * Leitner sistemi güncellenir — uygulama açılmadan gerçek tekrar yapılır.
     */
    private fun kartCevapla(context: Context, biliyorum: Boolean) {
        val kart = gecerliKart(context) ?: return
        KartStore.cevapla(context, kart.id, biliyorum)

        // Sıradaki karta geç
        val kalanlar = KartStore.bugunkuKartlar(context, limit = 40)
            .filter { it.id != kart.id }
        if (kalanlar.isEmpty()) {
            kartAyarla(context, 0L, false)
            mesaj(context, context.getString(R.string.we_kart_bitti))
        } else {
            kartAyarla(context, kalanlar.first().id, false)
            mesaj(
                context,
                if (biliyorum) context.getString(R.string.we_kart_bildim, kalanlar.size)
                else context.getString(R.string.we_kart_bilmedim, kalanlar.size)
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖREV
    // ═══════════════════════════════════════════════════════════════

    /** Görevi yarına erteler. */
    private fun gorevErtele(context: Context, gorevId: Long) {
        if (gorevId == 0L) return
        val liste = Store.loadTasks(context)
        val gorev = liste.firstOrNull { it.id == gorevId } ?: return
        val yarin = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
        }.timeInMillis
        gorev.dueAt = yarin
        Store.saveTasks(context, liste)
        try {
            AlarmScheduler.schedule(context, gorev.id, gorev.text, yarin)
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Alarm kurulamadı", e)
        }
        mesaj(context, context.getString(R.string.we_ertelendi))
    }

    // ═══════════════════════════════════════════════════════════════
    // ORTAK
    // ═══════════════════════════════════════════════════════════════

    private fun mesaj(context: Context, metin: String) {
        try {
            Toast.makeText(context, metin, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Toast gösterilemedi", e)
        }
    }

    /**
     * v7.95 — Aktif program adımını widget'tan tamamlar (öneri 4).
     *
     * ── Neden onay sorulmuyor ──
     * Widget'tan diyalog açılamaz. Yanlışlıkla basılırsa Koç ekranından
     * "Bitmedi olarak işaretle" ile geri alınabiliyor; kayıp yok.
     *
     * ── Neden hesap sorulmuyor ──
     * Koç ekranındaki "Bu dersi bitir" akışı soru soruyor. Widget hızlı
     * erişim noktası; oradan bitirmek bilinçli olarak sürtünmesiz.
     */
    private fun adimBitir(context: Context) {
        if (!Mufredat.secildiMi(context)) {
            bildir(context, context.getString(R.string.mf_kurs_secilmedi))
            return
        }
        val aktif = Mufredat.aktifAdim(context)
        if (aktif == null) {
            bildir(context, context.getString(R.string.mf_aktif_yok))
            return
        }

        val sonraki = Mufredat.aktifAdimiBitir(context)
        bildir(
            context,
            if (sonraki == null) {
                context.getString(R.string.mf_program_tamam)
            } else {
                context.getString(R.string.il_bitti_sirada, aktif.baslik, sonraki.baslik)
            }
        )
    }

    /** Widget eyleminin sonucunu kısa bildirimle duyurur. */
    private fun bildir(context: Context, mesaj: String) {
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context, mesaj, android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetEylem", "Bildirilemedi", e)
        }
    }
}
