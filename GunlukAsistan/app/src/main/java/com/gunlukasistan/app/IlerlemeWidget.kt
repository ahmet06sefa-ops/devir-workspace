package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * v7.83 — Program ilerleme widget'ı (4×2).
 *
 * ── Neden gerekli ──
 * Koç ve müfredat sistemi (v7.79-7.82) uygulamanın içinde kalıyordu.
 * Kullanıcı ana ekranına bakıp "programın neresindeyim, bugün ne
 * çalışacağım" sorusunu cevaplayamıyordu. Bu widget o cevabı veriyor:
 *   · Takip edilen kurs/konu ve tamamlanma yüzdesi
 *   · Şu an çalışılması gereken ders/madde
 *   · Kaç ders kaldı + bugünkü hedefin durumu
 *
 * ── RemoteViews kısıtı ──
 * Yalnızca LinearLayout · FrameLayout · TextView · ImageView ·
 * ProgressBar · ListView · GridView destekleniyor. Bu yüzden düzen sade
 * tutuldu; CardView/ConstraintLayout kullanılsaydı widget hiç
 * görünmezdi (v7.40.1'de yaşandı).
 *
 * ── Dokunma ──
 * Widget'a dokunmak [KocActivity]'yi açar — programın yönetildiği yer.
 */
class IlerlemeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, appWidgetManager, appWidgetId)
    }

    companion object {

        private const val TAG = "IlerlemeWidget"

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_ilerleme)

            try {
                val yukseklik = WidgetCommon.yukseklikDp(manager, widgetId, 110)
                val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
                val kademe = WidgetCommon.boyutKademesi(genislik)

                doldur(context, views, kademe, yukseklik)

                // v7.68: kullanıcının seçtiği widget teması
                runCatching {
                    val pal = WidgetTema.palet(context)
                    WidgetZemin.uygula(views, R.id.ilBg, context, pal, R.id.ilRoot)
                }

                views.setOnClickPendingIntent(
                    R.id.ilRoot,
                    BildirimMerkezi.aktiviteAc(context, KocActivity::class.java, 8901)
                )
                // v7.95: uygulamayı açmadan adımı bitir (öneri 4)
                views.setOnClickPendingIntent(
                    R.id.ilBitir,
                    WidgetEylem.niyet(context, WidgetEylem.IS_ADIM_BITIR, 8902)
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Widget çizilemedi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun doldur(
            context: Context,
            views: RemoteViews,
            kademe: Int,
            yukseklik: Int
        ) {
            // Eski sürümden gelen kurs seçimi varsa taşı
            runCatching { Mufredat.eskiSecimiTasi(context) }

            if (!Mufredat.secildiMi(context)) {
                views.setTextViewText(
                    R.id.ilProgram, context.getString(R.string.il_program_yok)
                )
                views.setTextViewText(R.id.ilYuzde, "")
                views.setProgressBar(R.id.ilBar, 100, 0, false)
                views.setTextViewText(R.id.ilSuAn, "")
                views.setTextViewText(
                    R.id.ilAktif, context.getString(R.string.il_secmek_icin)
                )
                views.setTextViewText(R.id.ilSayac, "")
                views.setTextViewText(R.id.ilHedef, "")
                WidgetCommon.goster(views, R.id.ilBitir, false)
                return
            }

            val konuMu = Mufredat.kaynakTuru(context) == Mufredat.KAYNAK_KONU
            views.setTextViewText(R.id.ilIcon, if (konuMu) "📝" else "📚")
            views.setTextViewText(
                R.id.ilProgram,
                WidgetCommon.sigdir(Mufredat.programAdi(context), kademe)
            )

            val ilerleme = Mufredat.ilerleme(context)
            views.setTextViewText(R.id.ilYuzde, "%" + ilerleme.yuzde)
            views.setProgressBar(R.id.ilBar, 100, ilerleme.yuzde, false)

            if (ilerleme.toplam == 0) {
                views.setTextViewText(R.id.ilSuAn, "")
                views.setTextViewText(
                    R.id.ilAktif, context.getString(R.string.mf_bos_program)
                )
                views.setTextViewText(R.id.ilSayac, "")
                views.setTextViewText(R.id.ilHedef, "")
                WidgetCommon.goster(views, R.id.ilBitir, false)
                return
            }

            if (ilerleme.bittiMi) {
                views.setTextViewText(R.id.ilSuAn, "")
                views.setTextViewText(
                    R.id.ilAktif, context.getString(R.string.il_bitti)
                )
                views.setTextViewText(
                    R.id.ilSayac,
                    context.getString(R.string.il_tamamlandi, ilerleme.toplam)
                )
                views.setTextViewText(R.id.ilHedef, "")
                WidgetCommon.goster(views, R.id.ilBitir, false)
                return
            }

            // Aktif ders
            views.setTextViewText(
                R.id.ilSuAn,
                context.getString(
                    if (konuMu) R.string.mf_su_an_konu else R.string.mf_su_an
                )
            )
            views.setTextViewText(
                R.id.ilAktif, WidgetCommon.sigdir(ilerleme.aktifAd, kademe)
            )

            // Alt satır: sıra bilgisi
            views.setTextViewText(
                R.id.ilSayac,
                context.getString(
                    R.string.il_sira, ilerleme.aktifSira, ilerleme.toplam, ilerleme.kalan
                )
            )

            // Bugünkü hedef — koç açıksa göster
            val hedefMetni = if (Koc.acikMi(context)) {
                val kalan = Koc.bugunKalan(context)
                if (kalan == 0) context.getString(R.string.il_hedef_tamam)
                else context.getString(R.string.il_hedef_kalan, kalan)
            } else ""
            views.setTextViewText(R.id.ilHedef, hedefMetni)

            // Kısa widget'ta alt satırı gizle — taşmasın
            WidgetCommon.goster(views, R.id.ilAltSatir, yukseklik >= 90)
            WidgetCommon.goster(views, R.id.ilSuAn, yukseklik >= 80)
            // Bitir düğmesi yalnızca yeterli yükseklikte ve aktif adım varken
            WidgetCommon.goster(views, R.id.ilBitir, yukseklik >= 120)
        }
    }
}
