package com.gunlukasistan.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * v9.7 — Takip bildirimlerini gönderen alıcı.
 *
 * İki tür uyandırma var:
 *
 *   · [TakipAlarm.TUR_GUNLUK] — günde bir kez, tüm bekleyenlerin
 *     özeti. "3 şey dikkat istiyor: Elektrik bugün, Ehliyet 12 gün..."
 *
 *   · [TakipAlarm.TUR_ILAC] — belirli bir saatte alınacak ilaçlar.
 *     Bildirimde "Aldım" düğmesi var; basınca stoktan düşüyor.
 *
 * Her iki durumda da alarm **yeniden kuruluyor** (bir sonraki gün
 * için). `setExact` tekrarlamıyor; tekrarlı alarm (`setRepeating`)
 * kullanmıyorum çünkü Doze modunda güvenilmez ve tam saat garantisi
 * vermiyor.
 */
class TakipReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TakipReceiver"

        const val ACTION_DOZ_ALINDI = "com.gunlukasistan.app.TAKIP_DOZ"
        const val ACTION_TAMAMLA = "com.gunlukasistan.app.TAKIP_TAMAM"
        const val EXTRA_KAYIT_ID = "kayit_id"

        /** Bildirim kimlik aralığı — diğer modüllerle çakışmasın. */
        private const val ID_GUNLUK = 710_000
        private const val ID_ILAC_TABAN = 711_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DOZ_ALINDI -> {
                val id = intent.getLongExtra(EXTRA_KAYIT_ID, -1L)
                if (id > 0) {
                    runCatching {
                        Takip.dozAl(context, id)
                        val kayit = Takip.bul(context, id)
                        // Bildirimi kapat
                        androidx.core.app.NotificationManagerCompat.from(context)
                            .cancel(intent.getIntExtra("bildirim_id", ID_ILAC_TABAN))
                        // Stok bittiyse hemen uyar — "aldım" dedikten sonra
                        // kutunun boşaldığını görmek en doğru an.
                        kayit?.stokGun()?.let { gun ->
                            if (gun <= 2) {
                                BildirimMerkezi.gonder(
                                    context, BildirimMerkezi.Tur.GOREV,
                                    ID_ILAC_TABAN + 900,
                                    context.getString(R.string.tk_stok_baslik),
                                    context.getString(R.string.tk_stok_uyari, kayit.ad, gun)
                                )
                            }
                        }
                    }.onFailure { android.util.Log.w(TAG, "Doz işlenemedi", it) }
                }
                return
            }

            ACTION_TAMAMLA -> {
                val id = intent.getLongExtra(EXTRA_KAYIT_ID, -1L)
                if (id > 0) {
                    runCatching {
                        Takip.tamamla(context, id)
                        androidx.core.app.NotificationManagerCompat.from(context)
                            .cancel(intent.getIntExtra("bildirim_id", ID_GUNLUK))
                    }.onFailure { android.util.Log.w(TAG, "Tamamlanamadı", it) }
                }
                return
            }
        }

        when (intent.getStringExtra(TakipAlarm.EXTRA_TUR)) {
            TakipAlarm.TUR_ILAC -> ilacBildirimi(context, intent.getIntExtra(TakipAlarm.EXTRA_SAAT, -1))
            else -> gunlukOzet(context)
        }

        // Bir sonraki gün için yeniden kur
        runCatching { TakipAlarm.yenidenKur(context) }
    }

    // ══════════════════════════════════════════════════════════

    private fun gunlukOzet(context: Context) {
        runCatching {
            val uyarilar = Takip.uyarilar(context)
            if (uyarilar.isEmpty()) return

            val ilk = uyarilar.first()
            val baslik = if (uyarilar.size == 1)
                context.getString(R.string.tk_bildirim_tek)
            else
                context.getString(R.string.tk_bildirim_coklu, uyarilar.size)

            val satirlar = uyarilar.take(6).joinToString("\n") {
                "${it.kayit.tur.emoji} ${it.kayit.ad} — ${it.mesaj}"
            }
            val kisa = "${ilk.kayit.tur.emoji} ${ilk.kayit.ad} — ${ilk.mesaj}"

            val eylemler = mutableListOf<Pair<String, PendingIntent>>()
            // Tek uyarı varsa doğrudan "tamamlandı" düğmesi koy —
            // birden fazlaysa hangisi olduğu belirsiz, ekranı aç.
            if (uyarilar.size == 1 && ilk.kayit.tur != Takip.Tur.ILAC) {
                val pi = PendingIntent.getBroadcast(
                    context, (ilk.kayit.id % 100000).toInt() + 5000,
                    Intent(context, TakipReceiver::class.java).apply {
                        action = ACTION_TAMAMLA
                        putExtra(EXTRA_KAYIT_ID, ilk.kayit.id)
                        putExtra("bildirim_id", ID_GUNLUK)
                        data = android.net.Uri.parse("gunlukasistan://tamam/${ilk.kayit.id}")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                eylemler.add(context.getString(R.string.tk_yapildi) to pi)
            }

            BildirimMerkezi.gonder(
                context = context,
                tur = BildirimMerkezi.Tur.GOREV,
                id = ID_GUNLUK,
                baslik = baslik,
                metin = kisa,
                genisMetin = satirlar,
                eylemler = eylemler,
                acilisIntent = ekranIntent(context)
            )
        }.onFailure { android.util.Log.w(TAG, "Günlük özet gönderilemedi", it) }
    }

    private fun ilacBildirimi(context: Context, saat: Int) {
        if (saat < 0) return
        runCatching {
            val ilaclar = Takip.turdekiler(context, Takip.Tur.ILAC)
                .filter { k -> k.saatler.any { it / 60 == saat } }
            if (ilaclar.isEmpty()) return

            val bildirimId = ID_ILAC_TABAN + saat
            val satirlar = ilaclar.joinToString("\n") { k ->
                val stok = k.stokGun()
                val ek = if (stok != null && stok <= 7)
                    " · " + context.getString(R.string.tk_stok_gun, stok) else ""
                "💊 ${k.ad}$ek"
            }

            val eylemler = mutableListOf<Pair<String, PendingIntent>>()
            if (ilaclar.size == 1 && ilaclar[0].stok >= 0) {
                val k = ilaclar[0]
                val pi = PendingIntent.getBroadcast(
                    context, (k.id % 100000).toInt() + 6000,
                    Intent(context, TakipReceiver::class.java).apply {
                        action = ACTION_DOZ_ALINDI
                        putExtra(EXTRA_KAYIT_ID, k.id)
                        putExtra("bildirim_id", bildirimId)
                        data = android.net.Uri.parse("gunlukasistan://doz/${k.id}")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                eylemler.add(context.getString(R.string.tk_aldim) to pi)
            }

            BildirimMerkezi.gonder(
                context = context,
                tur = BildirimMerkezi.Tur.GOREV,
                id = bildirimId,
                baslik = context.getString(R.string.tk_ilac_saati, Takip.saatMetni(saat * 60)),
                metin = ilaclar.joinToString(", ") { it.ad },
                genisMetin = satirlar,
                // İlaç saati kaçırılmamalı: sessiz saat kuralını aşıyor.
                // Kullanıcı sabah 07:00 ilacını sessiz saatte olsa da
                // almalı — sağlık verisi burada önceliği hak ediyor.
                acil = true,
                eylemler = eylemler,
                acilisIntent = ekranIntent(context)
            )
        }.onFailure { android.util.Log.w(TAG, "İlaç bildirimi gönderilemedi", it) }
    }

    private fun ekranIntent(context: Context): PendingIntent? = runCatching {
        PendingIntent.getActivity(
            context, 7100,
            Intent(context, TakipActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }.getOrNull()
}
