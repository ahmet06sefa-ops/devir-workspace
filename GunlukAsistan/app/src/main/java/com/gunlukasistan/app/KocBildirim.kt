package com.gunlukasistan.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * v7.78 — Koçun bildirimleri.
 *
 * ── Neden [BildirimUretici]'ye eklenmedi ──
 * O dosya 14 farklı bildirim türünü barındırıyor ve hepsi "öğrenme
 * hatırlatması" mantığında: nazik, atlanabilir, günlük tavana tabi.
 * Koç bildirimleri ise **atlanmamalı** — kullanıcı bunu isteyerek açtı.
 * `acil = true` ile gönderilirler; günlük tavan ve sessiz saat kuralları
 * dışındadırlar. Bunları aynı dosyaya koymak iki farklı sözleşmeyi
 * karıştırırdı.
 *
 * ── Eylem düğmeleri ──
 * "Başla" doğrudan sayaç ekranını açar — kullanıcı bildirimi görüp
 * uygulamayı arayıp sekme bulmak zorunda kalmasın. Sürtünme ne kadar
 * azsa çalışma ihtimali o kadar yüksek.
 */
object KocBildirim {

    private const val TAG = "KocBildirim"

    /** Sayaç ekranının indeksi — bkz. [WidgetCommon]. */
    private const val EKRAN_SAYAC = 4

    // ═══════════════════════════════════════════════════════════════
    // ÇALIŞMA ÇAĞRISI
    // ═══════════════════════════════════════════════════════════════

    /**
     * "Otur çalış" bildirimi.
     *
     * @param ertelendi erteleme sonrası tekrar mı geliyor (ton sertleşir)
     */
    fun calismaCagrisi(context: Context, ertelendi: Boolean = false) {
        val kalan = Koc.bugunKalan(context)
        if (kalan <= 0) return

        // v7.79: başlıkta dersin adı olsun — genel "çalışma vakti" değil
        val ders = Mufredat.aktifAdim(context)
        val baslik = when {
            ertelendi -> context.getString(R.string.koc_b_yine_baslik)
            ders != null -> context.getString(R.string.koc_b_ders_baslik, ders.baslik)
            else -> context.getString(R.string.koc_b_baslik)
        }

        val metin = Koc.hatirlatmaMetni(context)

        // Kişiselleştirilmiş cümle varsa onu kullan (AI arka planda üretmiş olabilir)
        val genis = KocMesaj.hazirMesaj(context).ifBlank { metin }

        BildirimMerkezi.gonder(
            context = context,
            tur = BildirimMerkezi.Tur.GOREV,
            id = KocZamanlayici.NOTIF_CALIS,
            baslik = baslik,
            metin = metin,
            genisMetin = genis,
            acil = true,
            eylemler = listOf(
                context.getString(R.string.koc_e_basla) to
                    BildirimMerkezi.ekranAc(context, EKRAN_SAYAC, 8820),
                context.getString(R.string.koc_e_sonra) to ertelePi(context),
                context.getString(R.string.koc_e_karne) to
                    BildirimMerkezi.aktiviteAc(context, KocActivity::class.java, 8822)
            ),
            acilisIntent = BildirimMerkezi.aktiviteAc(
                context, KocActivity::class.java, 8823
            ),
            ilerleme = Koc.bugunCalisilan(context) to Koc.bugunHedefi(context)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜN SONU HESABI
    // ═══════════════════════════════════════════════════════════════

    /** Hedefe ulaşılmadı — hesap sor. */
    fun hesapSor(context: Context) {
        val yapilan = Koc.bugunCalisilan(context)
        val hedef = Koc.bugunHedefi(context)
        val eksik = (hedef - yapilan).coerceAtLeast(0)

        val metin = when (Koc.sertlik(context)) {
            Koc.SERT_NAZIK -> context.getString(R.string.koc_b_hesap_nazik, eksik)
            Koc.SERT_ACIMASIZ -> context.getString(R.string.koc_b_hesap_sert, yapilan, hedef)
            else -> context.getString(R.string.koc_b_hesap, yapilan, hedef)
        }

        val hesapDersi = Mufredat.aktifAdim(context)
        BildirimMerkezi.gonder(
            context = context,
            tur = BildirimMerkezi.Tur.GOREV,
            id = KocZamanlayici.NOTIF_HESAP,
            baslik = if (hesapDersi != null)
                context.getString(R.string.koc_b_hesap_ders, hesapDersi.baslik)
            else context.getString(R.string.koc_b_hesap_baslik),
            metin = metin,
            genisMetin = context.getString(R.string.koc_b_hesap_genis, eksik),
            acil = true,
            eylemler = listOf(
                context.getString(R.string.koc_e_hesap_ver) to
                    hesapPi(context, 8830),
                context.getString(R.string.koc_e_simdi_calis) to
                    BildirimMerkezi.ekranAc(context, EKRAN_SAYAC, 8831)
            ),
            acilisIntent = hesapPi(context, 8832)
        )
    }

    /** Hedef tutturuldu — kısa tebrik. */
    fun tebrik(context: Context) {
        val seri = Koc.seri(context)
        BildirimMerkezi.gonder(
            context = context,
            tur = BildirimMerkezi.Tur.HEDEF_TAMAM,
            id = KocZamanlayici.NOTIF_HESAP,
            baslik = context.getString(R.string.koc_b_tebrik_baslik),
            metin = context.getString(R.string.koc_b_tebrik, Koc.bugunCalisilan(context), seri),
            acil = false,
            acilisIntent = BildirimMerkezi.aktiviteAc(
                context, KocActivity::class.java, 8833
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING INTENT'LER
    // ═══════════════════════════════════════════════════════════════

    private fun ertelePi(context: Context): PendingIntent {
        val intent = Intent(context, KocEylemAlici::class.java).apply {
            action = KocEylemAlici.ACTION_ERTELE
        }
        return PendingIntent.getBroadcast(
            context, 8821, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Hesap ekranını doğrudan açar. */
    private fun hesapPi(context: Context, kod: Int): PendingIntent {
        val intent = Intent(context, KocActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(KocActivity.EXTRA_HESAP, true)
            data = android.net.Uri.parse("gunlukasistan://koc/hesap/$kod")
        }
        return PendingIntent.getActivity(
            context, kod, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
