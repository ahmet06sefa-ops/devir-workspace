package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.10 · ULTRA-50 — KAPANIŞ MADDESİ C34: Hafta görünümü widget'ı.
 *
 * ── Öneri metni (ve dürüst yorumu) ──
 * "PlanWidget hafta görünümü modu: tek gün listesi yerine 7 sütunlu
 * mini hafta; dolu/boş işaretleri, güne dokununca o günün plan sayfası."
 *
 * Kod taramasında görüldü ki `PlanWidget` **namaz günü** widget'ıdır
 * (dilim işleri yalnızca bugün yaşar; haftalık karşılığı yok). Hafta
 * verisi `HaftaPlan`'da durur: gün bazlı hedef (dk) + ders ataması.
 * Bu yüzden "mod" yerine haftanın gerçek verisine bağlanan **yeni
 * sağlayıcı** kuruldu; önerinin her iki taahhüdü birebir karşılandı:
 *
 *   1. 7 sütunlu mini hafta, dolu/boş işaretleri + kalan görevler
 *   2. Güne dokununca o günün plan sayfası (hafta ekranı, o günün
 *      hedef diyaloğu doğrudan açık gelir — `HaftaPlanActivity.EXTRA_GUN`)
 *
 * ── Hücre içeriği ──
 * Gün adı · tarih · plan işareti · kalan görev rozeti.
 * İşaret: `● 90` (dk hedefli) · `● 📖` (yalnız dersli) · 🌿 (izin
 * günü) · `○` (tanımsız). Kalan görev = o güne tarihi bağlı,
 * tamamlanmamış görev sayısı; 0 ise gizlenir.
 *
 * ── Yenileme ──
 * `WidgetCommon.refreshAll` listesinde: görev tamamlama, gece yarısı,
 * tema değişimi otomatik tazeler. Plan ekranındaki her düzenleme de
 * ayrıca tazeleme çağırır (widget "eski haftada" kalmaz).
 */
class HaftaWidget : AppWidgetProvider() {

    companion object {

        private const val TAG = "HaftaWidget"

        private val CELL_IDS = intArrayOf(
            R.id.hw_cell_0, R.id.hw_cell_1, R.id.hw_cell_2, R.id.hw_cell_3,
            R.id.hw_cell_4, R.id.hw_cell_5, R.id.hw_cell_6
        )
        private val DAY_IDS = intArrayOf(
            R.id.hw_day_0, R.id.hw_day_1, R.id.hw_day_2, R.id.hw_day_3,
            R.id.hw_day_4, R.id.hw_day_5, R.id.hw_day_6
        )
        private val DATE_IDS = intArrayOf(
            R.id.hw_date_0, R.id.hw_date_1, R.id.hw_date_2, R.id.hw_date_3,
            R.id.hw_date_4, R.id.hw_date_5, R.id.hw_date_6
        )
        private val PLAN_IDS = intArrayOf(
            R.id.hw_plan_0, R.id.hw_plan_1, R.id.hw_plan_2, R.id.hw_plan_3,
            R.id.hw_plan_4, R.id.hw_plan_5, R.id.hw_plan_6
        )
        private val TASK_IDS = intArrayOf(
            R.id.hw_task_0, R.id.hw_task_1, R.id.hw_task_2, R.id.hw_task_3,
            R.id.hw_task_4, R.id.hw_task_5, R.id.hw_task_6
        )

        // ═══════════════════════════════════════════════════════════
        // SAF MANTIK (birim testli, Context'siz)
        // ═══════════════════════════════════════════════════════════

        /**
         * İçinde bulunulan haftanın Pazartesi'si (yerel 00:00).
         * Pazar günü ÖNCEKİ Pazartesi'ye bağlanır — TR haftası Pzt başlar.
         */
        fun haftaBaslangici(simdiMs: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = simdiMs
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val gun = cal.get(Calendar.DAY_OF_WEEK)
            val fark = if (gun == Calendar.SUNDAY) 6 else gun - Calendar.MONDAY
            cal.add(Calendar.DAY_OF_YEAR, -fark)
            return cal.timeInMillis
        }

        /** [baslangicMs]'ten itibaren 7 günün 00:00 milisaniyeleri. */
        fun gunListesi(baslangicMs: Long): LongArray {
            val cal = Calendar.getInstance()
            cal.timeInMillis = baslangicMs
            return LongArray(7) {
                val ms = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                ms
            }
        }

        /** Aynı takvim günü mü (yerel saat dilimi). */
        fun ayniGunMu(aMs: Long, bMs: Long): Boolean {
            val a = Calendar.getInstance().apply { timeInMillis = aMs }
            val b = Calendar.getInstance().apply { timeInMillis = bMs }
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        }

        /**
         * Hücredeki doluluk işareti:
         *  - plan kapalı / gün tanımsız         → `○`
         *  - dk hedefi atanmış                  → `● 90`
         *  - yalnız ders atanmış (hedef yok)    → `● 📖`
         *  - izin günü (hedef 0)                → 🌿
         */
        fun planIsareti(hedef: Int, dersVar: Boolean, planAcik: Boolean): String = when {
            !planAcik -> "○"
            hedef > 0 -> "● $hedef"
            dersVar -> "● 📖"
            hedef == 0 -> "🌿"
            else -> "○"
        }

        /** Kalan görev rozeti — 0 iken boş (hücre nefes alsın). */
        fun gorevRozeti(kalan: Int): String =
            if (kalan > 0) "$kalan ⚑" else ""

        // ═══════════════════════════════════════════════════════════
        // ÇİZİM
        // ═══════════════════════════════════════════════════════════

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_hafta)
            // Dolu/boş işaret ayrımı: çizim try'ında dolar, tema try'ında okunur.
            // (İki ayrı try olduğu için bildirim dışarıda durmak zorunda —
            //  v10.10 öz denetiminde yakalanan derleme hatası buydu.)
            val doluIsaretler = mutableListOf<Int>()
            val bosIsaretler = mutableListOf<Int>()
            try {
                val simdi = System.currentTimeMillis()
                val baslangic = haftaBaslangici(simdi)
                val gunlerMs = gunListesi(baslangic)
                val planAcik = HaftaPlan.acikMi(context)

                // Hafta aralığı: "3 Ağu – 9 Ağu"
                val okuyucu = SimpleDateFormat("d MMM", Locale("tr", "TR"))
                views.setTextViewText(
                    R.id.hwRange,
                    context.getString(
                        R.string.hw_aralik,
                        okuyucu.format(Date(gunlerMs[0])),
                        okuyucu.format(Date(gunlerMs[6]))
                    )
                )

                // Plan kapalıysa ipucu satırı açılır
                views.setViewVisibility(
                    R.id.hwNote,
                    if (planAcik) View.GONE else View.VISIBLE
                )
                if (!planAcik) {
                    views.setTextViewText(R.id.hwNote, context.getString(R.string.hw_plan_kapali))
                }

                // Kalan görevler: güne tarihli, bitmemiş
                val kalanlar = IntArray(7)
                try {
                    Store.loadTasks(context).forEach { gorev ->
                        if (!gorev.done && gorev.dueAt > 0) {
                            for (i in 0..6) {
                                if (ayniGunMu(gorev.dueAt, gunlerMs[i])) {
                                    kalanlar[i]++
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Görev sayılamadı", e)
                }

                // Hücreler
                val dar = WidgetCommon.genislikDp(manager, widgetId, 250) < 300
                val kisa = WidgetCommon.yukseklikDp(manager, widgetId, 110) < 130
                for (i in 0..6) {
                    val calGun = HaftaPlan.gunSirasi[i]
                    val bugunMu = ayniGunMu(gunlerMs[i], simdi)

                    // Gün adı (bugüne "▸" işareti)
                    val gunAd = HaftaPlan.gunAdi(context, calGun)
                    views.setTextViewText(
                        DAY_IDS[i], if (bugunMu) "▸$gunAd" else gunAd
                    )

                    // Tarih numarası
                    val gunCal = Calendar.getInstance().apply { timeInMillis = gunlerMs[i] }
                    views.setTextViewText(
                        DATE_IDS[i], gunCal.get(Calendar.DAY_OF_MONTH).toString()
                    )

                    // Plan işareti — dolu/boş ayrımı tema rengini de belirler
                    val hedef = if (planAcik) HaftaPlan.hedef(context, calGun) else -1
                    val dersVar = planAcik && HaftaPlan.ders(context, calGun) != 0L
                    val isaret = planIsareti(hedef, dersVar, planAcik)
                    views.setTextViewText(PLAN_IDS[i], isaret)
                    (if (isaret.startsWith("●")) doluIsaretler else bosIsaretler).add(PLAN_IDS[i])

                    // Kalan görev rozeti
                    views.setTextViewText(TASK_IDS[i], gorevRozeti(kalanlar[i]))
                    views.setViewVisibility(
                        TASK_IDS[i],
                        if (kalanlar[i] > 0 && !kisa) View.VISIBLE else View.GONE
                    )

                    // Dar ölçüde gün adı düşer — tarih + işaret yeter
                    views.setViewVisibility(DAY_IDS[i], if (dar) View.GONE else View.VISIBLE)

                    // Dokunuş: o günün plan sayfası (hedef diyaloğu açık)
                    views.setOnClickPendingIntent(
                        CELL_IDS[i], gunuAc(context, calGun, 4891 + i)
                    )
                }

                // Üst şerit → hafta planı ekranı
                views.setOnClickPendingIntent(R.id.hwHeader, ekraniAc(context, 4889))
                views.setOnClickPendingIntent(R.id.hwNote, ekraniAc(context, 4890))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Çizilemedi", e)
            }

            // Tema (v7.66 örneği — kök saydamlığı + metin/soluk/vurgu)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.hwBg, context, pal, R.id.hwRoot)
                // v10.21: başlık çubuğu kullanıcı denetiminde (varsayılan açık)
                WidgetCommon.goster(views, R.id.hwTitle, WidgetSecim.goster(context, WidgetSecim.W_HW_BASLIK))
                val simdi = System.currentTimeMillis()
                val bugunIdx = (0..6).firstOrNull {
                    ayniGunMu(gunListesi(haftaBaslangici(simdi))[it], simdi)
                } ?: -1
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.hwTitle, R.id.hwNote) + DATE_IDS,
                    soluklar = intArrayOf(R.id.hwRange) + DAY_IDS + TASK_IDS +
                        bosIsaretler.toIntArray(),
                    vurgular = doluIsaretler.toIntArray()
                )
                if (bugunIdx >= 0) {
                    // Bugünün tarihi vurguda parlasın
                    WidgetTema.metin(views, DATE_IDS[bugunIdx], pal.vurgu)
                    WidgetTema.metin(views, DAY_IDS[bugunIdx], pal.vurgu)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Tema uygulanamadı", e)
            }

            manager.updateAppWidget(widgetId, views)
        }

        /** O günün plan sayfası — hedef diyaloğu doğrudan açık gelir. */
        private fun gunuAc(context: Context, calendarGun: Int, kod: Int): PendingIntent {
            val niyet = Intent(context, HaftaPlanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(HaftaPlanActivity.EXTRA_GUN, calendarGun)
                data = android.net.Uri.parse("gunlukasistan://haftaw/gun/$calendarGun")
            }
            return PendingIntent.getActivity(
                context, kod, niyet,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun ekraniAc(context: Context, kod: Int): PendingIntent {
            val niyet = Intent(context, HaftaPlanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = android.net.Uri.parse("gunlukasistan://haftaw/ekran/$kod")
            }
            return PendingIntent.getActivity(
                context, kod, niyet,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Bu türün tüm örneklerini tazeler. */
        fun hepsiniTazele(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                manager.getAppWidgetIds(ComponentName(context, HaftaWidget::class.java))
                    .forEach { render(context, manager, it) }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Tazelenemedi", e)
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

    /** Yeniden boyutlandırınca dar/kısa düzen yeniden seçilsin. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, appWidgetManager, appWidgetId)
    }
}
