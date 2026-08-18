package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews

/**
 * v10.13 · ULTRA-30 / B10 — 1×1 "Şimdi odak" düğmesi.
 *
 * ── Tarama kanıtı ──
 * ActionsWidget uygulamayı AÇIYOR, SayacWidget çipleri 2×2'de kalıyordu;
 * tek hücrelik "uygulamayı açmadan odak başlat" düğmesi yoktu.
 *
 * ── Davranış ──
 * · Sayaç boştayken dokun → [TimerActionReceiver.ACTION_BASLAT_DK] ile
 *   25 dk'lık geri sayım UYGULAMA AÇILMADAN başlar (yalnız yayın gider).
 * · Sayaç koşarken halka dolmakta olan ilerlemeyi, etiket kalan süreyi
 *   gösterir; dokunmak sayaç ekranını açar.
 *
 * ── Tazeleme ──
 * [TimerEngine.sayaciYansit] sayaç her durum değiştirdiğinde bu widget'ı
 * da render eder; koşu sırasındaki 15 sn'lik mevcut tazeleme bileti
 * halkayı canlı tutar.
 */
class OdakKutusuWidget : AppWidgetProvider() {

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

        /** Tek dokunuşla başlatılan odak süresi (SayacPreset'in en uzun çipi). */
        const val VARSAYILAN_DK = 25

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_odak)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.odakBg, context, pal)

                val kosuyor = TimerEngine.isRunning(context) &&
                    TimerEngine.mode(context) == TimerEngine.MODE_DOWN
                val yuzde = if (kosuyor) {
                    Kokpit.yuzde(
                        TimerEngine.remainingMs(context), TimerEngine.totalMs(context)
                    )
                } else -1

                views.setImageViewBitmap(R.id.odakHalka, halka(pal, yuzde))
                views.setTextViewText(
                    R.id.odakEtiket,
                    if (kosuyor) TimerEngine.format(TimerEngine.remainingMs(context))
                    else context.getString(R.string.wg_odak_baslat)
                )
                WidgetTema.uygula(views, pal, soluklar = intArrayOf(R.id.odakEtiket))
                if (kosuyor) WidgetTema.metin(views, R.id.odakEtiket, pal.vurgu)

                // Dokunma: boştayken başlat (uygulama açılmaz), koşarken sayaç ekranı
                // v10.21: koşarken açılacak sekme kullanıcı seçimli
                val tik = if (kosuyor) {
                    WidgetCommon.openScreen(
                        context,
                        WidgetDokunma.ekran(context, WidgetDokunma.ODAK, WidgetCommon.SCREEN_TIMER),
                        4974
                    )
                } else {
                    PendingIntent.getBroadcast(
                        context, 4973,
                        Intent(context, TimerActionReceiver::class.java).apply {
                            action = TimerActionReceiver.ACTION_BASLAT_DK
                            putExtra(TimerActionReceiver.EXTRA_DAKIKA, VARSAYILAN_DK)
                            // Tek hücre — birden çok örnek de aynı davranır
                            data = android.net.Uri.parse("gunlukasistan://odakbaslat/$widgetId")
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                views.setOnClickPendingIntent(R.id.odakKok, tik)
            } catch (e: Exception) {
                android.util.Log.w("OdakKutusuWidget", "Çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }

        // ---------------- Halka bitmap'i ----------------

        /**
         * İlerleme halkası: boştayken boş daire + "▶", koşarken dolan yay
         * + yüzde. 1×1 hücrede metin okunamaz kadar küçülmesin diye
         * merkez sayısı halkanın içine değil, tam içine sığacak boyda.
         */
        private fun halka(pal: WidgetTema.Palet, yuzde: Int): Bitmap {
            val kenar = 120
            val bmp = Bitmap.createBitmap(kenar, kenar, Bitmap.Config.ARGB_8888)
            val tuval = Canvas(bmp)
            val merkez = kenar / 2f
            val kalinlik = kenar * 0.085f
            val yaricap = merkez - kalinlik - 2f
            val oval = RectF(
                merkez - yaricap, merkez - yaricap,
                merkez + yaricap, merkez + yaricap
            )

            // Alt iz (her zaman)
            val iz = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = kalinlik
                color = (pal.metinSoluk and 0x00FFFFFF) or 0x33000000
            }
            tuval.drawArc(oval, 0f, 360f, false, iz)

            if (yuzde >= 0) {
                val yay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = kalinlik
                    strokeCap = Paint.Cap.ROUND
                    color = pal.vurgu
                }
                tuval.drawArc(oval, -90f, 360f * yuzde / 100f, false, yay)

                val yazi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    color = pal.metin
                    textSize = kenar * 0.24f
                    isFakeBoldText = true
                }
                val metinTaban = merkez - (yazi.descent() + yazi.ascent()) / 2f
                tuval.drawText("%$yuzde", merkez, metinTaban, yazi)
            } else {
                // Boş durum: oynat işareti (küçük üçgen)
                val ucgen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = pal.vurgu
                }
                val yari = kenar * 0.16f
                val yol = android.graphics.Path().apply {
                    moveTo(merkez - yari * 0.5f, merkez - yari)
                    lineTo(merkez - yari * 0.5f, merkez + yari)
                    lineTo(merkez + yari, merkez)
                    close()
                }
                tuval.drawPath(yol, ucgen)
            }
            return bmp
        }
    }
}
