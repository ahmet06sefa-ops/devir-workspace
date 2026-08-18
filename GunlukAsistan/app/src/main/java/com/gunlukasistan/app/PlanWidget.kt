package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast

/**
 * v7.65 — Vakit Planı widget'ı (4×3).
 *
 * ── Kullanıcının isteği ──
 * "Namaz plan uygulamasını temasında widget ekle ve bütün özellikleri
 *  ekle. Vakit planı ekranının benzeri olsun."
 *
 * ── Plan sekmesinin ana ekran karşılığı ──
 * | Plan sekmesi                | Widget                          |
 * |-----------------------------|---------------------------------|
 * | Sıradaki vakit + geri sayım | Üst şerit                       |
 * | Bugünün 6 vakti             | Alt satır özeti                 |
 * | Aktif dilim + kalan süre    | Dilim şeridi + doluluk çubuğu   |
 * | Dilim işleri, dokun-tamamla | Kaydırılabilir liste            |
 * | + ile iş ekleme             | "+ İş" düğmesi                  |
 * | Bugünkü ilerleme            | 2/5 rozeti                      |
 *
 * ── Neden ayrı widget? ──
 * `NamazWidget` (2×1) yalnızca sıradaki vakti gösteriyor — bilinçli olarak
 * minimalist. Bu widget ise planı yönetmek isteyenler için: ana ekrandan
 * çıkmadan işleri görüp işaretleyebilirsin.
 *
 * ── RemoteViews kısıtı ──
 * Yalnızca izin verilen görünüm türleri kullanıldı (LinearLayout,
 * FrameLayout, TextView, ProgressBar, ListView). `<View>` veya
 * ConstraintLayout widget'ı kırar — v7.40.1'de yaşandı.
 */
class PlanWidget : AppWidgetProvider() {

    companion object {
        const val EXTRA_IS_ID = "w_plan_is_id"
        const val ACTION_TAMAMLA = "com.gunlukasistan.app.PLAN_W_TAMAMLA"

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_plan)

            // ── Namaz modülü kapalıysa yönlendir ──
            if (!NamazVakti.acikMi(context)) {
                views.setTextViewText(R.id.pwNext, context.getString(R.string.pw_baslik))
                views.setTextViewText(R.id.pwLeft, context.getString(R.string.pw_kapali))
                views.setViewVisibility(R.id.pwProgress, View.GONE)
                views.setViewVisibility(R.id.pwAdd, View.GONE)
                views.setViewVisibility(R.id.pwSlot, View.GONE)
                views.setViewVisibility(R.id.pwBar, View.GONE)
                views.setViewVisibility(R.id.pwTimes, View.GONE)
                views.setOnClickPendingIntent(R.id.pwRoot, ayarlariAc(context))
                manager.updateAppWidget(widgetId, views)
                return
            }

            try {
                val gun = NamazVakti.bugunDuzeltilmis(context)
                val simdi = NamazVakti.simdiDakika()
                val (sonraki, kalan) = gun.sonraki(simdi)
                val dilim = NamazPlan.aktifDilim(gun, simdi)

                val genislik = WidgetCommon.genislikDp(manager, widgetId, 250)
                val yukseklik = WidgetCommon.yukseklikDp(manager, widgetId, 180)
                val kademe = WidgetCommon.boyutKademesi(genislik)
                val dar = genislik < 180
                val kisa = yukseklik < 120

                // ── Üst şerit ──
                views.setTextViewText(R.id.pwEmoji, sonraki.emoji)
                views.setTextViewText(
                    R.id.pwNext,
                    context.getString(
                        R.string.pw_sonraki,
                        context.getString(sonraki.adRes),
                        gun.saat(sonraki)
                    )
                )
                views.setTextViewText(
                    R.id.pwLeft,
                    context.getString(R.string.pw_kalan, NamazPlan.sureMetni(kalan))
                )
                WidgetCommon.yaziBoyutu(views, R.id.pwNext, 12f, 13f, 14f, kademe)

                // ── Bugünkü ilerleme (2/5) ──
                val (biten, toplam) = NamazPlan.bugunOzet(context)
                if (toplam > 0) {
                    views.setViewVisibility(R.id.pwProgress, View.VISIBLE)
                    views.setTextViewText(
                        R.id.pwProgress,
                        context.getString(R.string.pw_ilerleme, biten, toplam)
                    )
                } else {
                    views.setViewVisibility(R.id.pwProgress, View.GONE)
                }

                // Dar widget'ta "+ İş" düğmesi yer kaplamasın
                views.setViewVisibility(R.id.pwAdd, if (dar) View.GONE else View.VISIBLE)

                // ── Aktif dilim şeridi ──
                val dilimKalan = NamazPlan.kalanDakika(gun, dilim, simdi)
                views.setTextViewText(
                    R.id.pwSlot,
                    context.getString(
                        R.string.pw_dilim_kalan,
                        dilim.emoji + " " + context.getString(dilim.adRes),
                        NamazPlan.sureMetni(dilimKalan)
                    )
                )
                views.setViewVisibility(R.id.pwSlot, if (kisa) View.GONE else View.VISIBLE)

                // ── Doluluk çubuğu: dilimin ne kadarı geçti ──
                val dilimToplam = NamazPlan.dilimSuresi(gun, dilim)
                val yuzde = if (dilimToplam > 0) {
                    (((dilimToplam - dilimKalan).toFloat() / dilimToplam) * 100f)
                        .toInt().coerceIn(0, 100)
                } else 0
                views.setProgressBar(R.id.pwBar, 100, yuzde, false)
                views.setViewVisibility(R.id.pwBar, if (kisa) View.GONE else View.VISIBLE)

                // ── Alt satır: bugünün vakitleri ──
                val vakitOzeti = NamazVakti.Vakit.entries.joinToString(" · ") {
                    context.getString(it.adRes).take(3) + " " + gun.saat(it)
                }
                views.setTextViewText(R.id.pwTimes, vakitOzeti)
                views.setViewVisibility(
                    R.id.pwTimes,
                    if (kisa || dar) View.GONE else View.VISIBLE
                )

                // ── Liste bağlantısı ──
                val servis = Intent(context, PlanWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("gunlukasistan://plan/$widgetId")
                }
                views.setRemoteAdapter(R.id.pwList, servis)
                views.setEmptyView(R.id.pwList, R.id.pwEmpty)

                // Satır dokunuşları için şablon
                val tamamlaIntent = Intent(context, PlanWidget::class.java).apply {
                    action = ACTION_TAMAMLA
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse("gunlukasistan://plantamam/$widgetId")
                }
                views.setPendingIntentTemplate(
                    R.id.pwList,
                    PendingIntent.getBroadcast(
                        context, 8800 + widgetId, tamamlaIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )

                // ── Tıklama hedefleri ──
                // Başlık → Plan sekmesi · "+ İş" → Plan sekmesi (ekleme için)
                // v7.69: widget artik tam etkilesimli — hepsi hizli panele gider.
                // Panel seffaf; kapatinca kullanici ana ekranina doner,
                // uygulamaya girip cikmis hissetmez.
                views.setOnClickPendingIntent(
                    R.id.pwHeader, hizliAc(context, dilim.anahtar, false, 8801)
                )
                // "+ Is" dogrudan metin kutusuna odaklanarak acilir
                views.setOnClickPendingIntent(
                    R.id.pwAdd, hizliAc(context, dilim.anahtar, true, 8802)
                )
                views.setOnClickPendingIntent(
                    R.id.pwSlot, hizliAc(context, dilim.anahtar, false, 8803)
                )
                views.setOnClickPendingIntent(
                    R.id.pwEmpty, hizliAc(context, dilim.anahtar, true, 8804)
                )
                // Vakit ozeti yine namaz ekranina
                views.setOnClickPendingIntent(R.id.pwTimes, namaziAc(context))
                // Ilerleme rozeti → tam Plan sekmesi
                views.setOnClickPendingIntent(R.id.pwProgress, planiAc(context, 8807))
            } catch (e: Exception) {
                android.util.Log.w("PlanWidget", "Çizilemedi", e)
                views.setTextViewText(R.id.pwLeft, "--:--")
            }

            // v7.66: uygulama temasina gore boya
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.pwBg, context, pal, R.id.pwRoot)
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.pwNext, R.id.pwSlot),
                    soluklar = intArrayOf(R.id.pwLeft, R.id.pwEmpty, R.id.pwTimes),
                    vurgular = intArrayOf(R.id.pwProgress),
                    cipler = intArrayOf(R.id.pwSlot)
                )
                WidgetTema.vurguDugme(views, R.id.pwAdd, R.id.pwAdd, pal)
            } catch (e: Exception) {
                android.util.Log.w("PlanWidget", "Tema uygulanamadi", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        /**
         * v7.69: Hizli islem panelini acar.
         *
         * @param dilimAnahtar hangi dilim gosterilsin
         * @param ekle true ise metin kutusuna odaklanir
         */
        private fun hizliAc(
            context: Context,
            dilimAnahtar: String,
            ekle: Boolean,
            kod: Int
        ): PendingIntent {
            val intent = Intent(context, PlanHizliActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PlanHizliActivity.EXTRA_DILIM, dilimAnahtar)
                putExtra(PlanHizliActivity.EXTRA_EKLE, ekle)
                data = android.net.Uri.parse(
                    "gunlukasistan://planhizli/" + dilimAnahtar + "/" + kod
                )
            }
            return PendingIntent.getActivity(
                context, kod, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Ana ekranda Plan sekmesini açar. */
        private fun planiAc(context: Context, kod: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_PLAN)
                data = android.net.Uri.parse("gunlukasistan://planw/$kod")
            }
            return PendingIntent.getActivity(
                context, kod, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun namaziAc(context: Context): PendingIntent {
            val intent = Intent(context, NamazActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = android.net.Uri.parse("gunlukasistan://planwnamaz")
            }
            return PendingIntent.getActivity(
                context, 8805, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun ayarlariAc(context: Context): PendingIntent {
            val intent = Intent(context, NamazAyarActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = android.net.Uri.parse("gunlukasistan://planwayar")
            }
            return PendingIntent.getActivity(
                context, 8806, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Bu türün tüm örneklerini tazeler. */
        fun hepsiniTazele(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                manager.getAppWidgetIds(ComponentName(context, PlanWidget::class.java))
                    .forEach { render(context, manager, it) }
            } catch (e: Exception) {
                android.util.Log.w("PlanWidget", "Tazelenemedi", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    /** Yeniden boyutlandırınca düzen güncellensin. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TAMAMLA) return

        val isId = intent.getLongExtra(EXTRA_IS_ID, 0L)
        if (isId == 0L) return
        try {
            val yeniDurum = NamazPlan.isTamamla(context, isId)
            if (yeniDurum) {
                val metin = NamazPlan.isleriYukle(context)
                    .firstOrNull { it.id == isId }?.metin.orEmpty()
                if (metin.isNotBlank()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.pw_is_tamam, metin),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // Listeyi ve üst şeridi tazele
            val manager = AppWidgetManager.getInstance(context) ?: return
            val idler = manager.getAppWidgetIds(ComponentName(context, PlanWidget::class.java))
            manager.notifyAppWidgetViewDataChanged(idler, R.id.pwList)
            idler.forEach { render(context, manager, it) }
        } catch (e: Exception) {
            android.util.Log.w("PlanWidget", "İş tamamlanamadı", e)
        }
    }
}
