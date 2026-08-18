package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * v10.13 · ULTRA-30 / B7 — Kokpit süper widget (4×2).
 *
 * ── Tarama kanıtı ──
 * 12 widget'ın hepsi tek işlevliydi; birleşik panel yoktu. Bu widget
 * üç dokunma bölgesinden oluşan tek kokpit sunar:
 *
 *   [ ◉ kadran ]   [ sıradaki 2 görev ]   [ 🔥 seri ]
 *      ↓ sayaç          ↓ görevler           ↓ ana ekran
 *
 * ── Kadran ──
 * Kadran bitmap'i hem çevirmeli saati (akrep/yelkovan) hem de sayaç
 * ilerleme halkasını çizer. Canlı saat üstte `TextClock` ile de ayrıca
 * durur (sistem kendisi işler, pil harcamaz). Sayaç koşarken kalan
 * süre halkanın ortasına kadran içine yazılır.
 *
 * ── Tazeleme ──
 * Sayaç koşarken 15 saniyelik mevcut tazeleme bileti
 * ([TimerEngine.sayaciYansit]) bu widget'ı da render eder; dururken
 * olay bazlı [WidgetCommon.refreshAll] yeterlidir.
 */
class KokpitWidget : AppWidgetProvider() {

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

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_kokpit)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.kpBg, context, pal)

                // ── Kadran: saat + sayaç halkası + kalan süre ──
                views.setImageViewBitmap(R.id.kpDial, kadran(context, pal))

                // ── Sıradaki görevler (en çok 2) ──
                val gorevler = siradakiGorevler(context)
                gorevMetni(views, R.id.kpGorev1, gorevler.getOrNull(0), context)
                gorevMetni(views, R.id.kpGorev2, gorevler.getOrNull(1), context)

                // ── Seri bölgesi ──
                val (seri, _) = Store.streakInfo(context)
                views.setTextViewText(
                    R.id.kpSeriSayi,
                    context.getString(R.string.wg_seri_gun, Kokpit.seriGun(seri))
                )

                // ── Boyama ──
                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.kpSeriSayi, R.id.kpSaat),
                    soluklar = intArrayOf(
                        R.id.kpUst, R.id.kpSeriEtiket, R.id.kpGun,
                        R.id.kpGorev1, R.id.kpGorev2
                    )
                )
                WidgetTema.metin(views, R.id.kpSeriYuz, pal.vurgu)

                // ── Üç dokunma bölgesi ──
                views.setOnClickPendingIntent(
                    R.id.kpBolgeSayac,
                    WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TIMER, 4970)
                )
                views.setOnClickPendingIntent(
                    R.id.kpBolgeGorev,
                    WidgetCommon.openScreen(context, WidgetCommon.SCREEN_TASKS, 4971)
                )
                views.setOnClickPendingIntent(
                    R.id.kpBolgeSeri,
                    WidgetCommon.openScreen(context, WidgetCommon.SCREEN_HOME, 4972)
                )
            } catch (e: Exception) {
                android.util.Log.w("KokpitWidget", "Çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }

        /** Görev listesi widget'ıyla aynı sıralama: bugünlü → tarihsiz → ileri. */
        private fun siradakiGorevler(context: Context): List<Store.Task> {
            val all = Store.loadTasks(context).filter { !it.done }
            val son = WidgetCommon.endOfToday()
            val bugunlu = all.filter { it.dueAt in 1..son }.sortedBy { it.dueAt }
            val tarihsiz = all.filter { it.dueAt == 0L }.sortedByDescending { it.createdAt }
            val ileri = all.filter { it.dueAt > son }.sortedBy { it.dueAt }
            return (bugunlu + tarihsiz + ileri).take(2)
        }

        private fun gorevMetni(
            views: RemoteViews, id: Int, gorev: Store.Task?, context: Context
        ) {
            if (gorev == null) {
                views.setTextViewText(id, context.getString(R.string.wg_gorev_yok))
            } else {
                val tanim = Etiket.bul(gorev.etiket)
                val onek = tanim?.let { it.emoji + " " } ?: "• "
                views.setTextViewText(id, onek + gorev.text)
            }
        }

        // ---------------- Kadran bitmap'i ----------------

        private fun kadran(context: Context, pal: WidgetTema.Palet): Bitmap {
            val kenar = 168
            val bmp = Bitmap.createBitmap(kenar, kenar, Bitmap.Config.ARGB_8888)
            val tuval = Canvas(bmp)
            val merkez = kenar / 2f
            val disYaricap = merkez - 4f

            // Zemin daire
            val daire = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = karistir(pal.zemin, if (pal.koyuMu) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), 0.08f)
            }
            tuval.drawCircle(merkez, merkez, disYaricap, daire)

            // Sayaç ilerleme halkası (dış bant)
            val yuzde = if (
                TimerEngine.isRunning(context) &&
                TimerEngine.mode(context) == TimerEngine.MODE_DOWN
            ) {
                Kokpit.yuzde(TimerEngine.remainingMs(context), TimerEngine.totalMs(context))
            } else -1
            if (yuzde >= 0) {
                val halka = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = kenar * 0.055f
                    strokeCap = Paint.Cap.ROUND
                    color = pal.vurgu
                }
                val r = disYaricap - halka.strokeWidth / 2f
                val oval = RectF(merkez - r, merkez - r, merkez + r, merkez + r)
                tuval.drawArc(oval, -90f, 360f * yuzde / 100f, false, halka)
            }

            // Saat çizgileri (12 tane)
            val cizgi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                color = pal.metinSoluk
                strokeWidth = kenar * 0.012f
            }
            for (i in 0 until 12) {
                val aci = Math.toRadians(i * 30.0)
                val bas = disYaricap * 0.80f
                val son = disYaricap * 0.90f
                tuval.drawLine(
                    merkezX(merkez, aci, bas), merkezY(merkez, aci, bas),
                    merkezX(merkez, aci, son), merkezY(merkez, aci, son),
                    cizgi
                )
            }

            // Akrep / yelkovan
            val simdi = Calendar.getInstance()
            val (akrepAci, yelkAci) = Kokpit.acilar(
                simdi.get(Calendar.HOUR_OF_DAY), simdi.get(Calendar.MINUTE)
            )
            val akrepBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                color = pal.metin
                strokeWidth = kenar * 0.030f
            }
            val yelBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                color = pal.metin
                strokeWidth = kenar * 0.018f
            }
            ibre(tuval, merkez, disYaricap * 0.42f, akrepAci, akrepBoya)
            ibre(tuval, merkez, disYaricap * 0.62f, yelkAci, yelBoya)

            // Merkez nokta
            val nokta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = pal.vurgu
            }
            tuval.drawCircle(merkez, merkez, kenar * 0.030f, nokta)

            // Kalan süre — halkanın alt yarısında küçük metin
            if (yuzde >= 0) {
                val yazi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    color = pal.vurgu
                    textSize = kenar * 0.115f
                    isFakeBoldText = true
                }
                tuval.drawText(
                    TimerEngine.format(TimerEngine.remainingMs(context)),
                    merkez, merkez + disYaricap * 0.52f, yazi
                )
            }
            return bmp
        }

        private fun ibre(tuval: Canvas, merkez: Float, boy: Float, derece: Float, boya: Paint) {
            val aci = Math.toRadians(derece.toDouble()) - Math.PI / 2
            tuval.drawLine(
                merkez, merkez,
                (merkez + cos(aci) * boy).toFloat(),
                (merkez + sin(aci) * boy).toFloat(),
                boya
            )
        }

        private fun merkezX(m: Float, aci: Double, yaricap: Float): Float =
            (m + cos(aci) * yaricap).toFloat()

        private fun merkezY(m: Float, aci: Double, yaricap: Float): Float =
            (m + sin(aci) * yaricap).toFloat()

        private fun karistir(a: Int, b: Int, oran: Float): Int {
            val t = oran.coerceIn(0f, 1f)
            val ta = 1f - t
            return android.graphics.Color.rgb(
                (android.graphics.Color.red(a) * ta + android.graphics.Color.red(b) * t).toInt(),
                (android.graphics.Color.green(a) * ta + android.graphics.Color.green(b) * t).toInt(),
                (android.graphics.Color.blue(a) * ta + android.graphics.Color.blue(b) * t).toInt()
            )
        }
    }
}
