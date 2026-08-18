package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.widget.RemoteViews
import java.util.Calendar
import java.util.Locale

/**
 * v10.13 · ULTRA-30 / B9 — Uyku widget'ı (4×2).
 *
 * ── Tarama kanıtı ──
 * v10.9 uyku defteri verisi hiçbir widget'a açılmamıştı.
 *
 * ── İçerik ──
 * Son 7 gecenin çubuk grafiği (bitmap), kesikli çizgiyle PLANlanan
 * süre (akşam hedefi → sabah hedefi), altta ortalama ve bu gecenin
 * hedef saati. Dokunmak uyku ayarlarını açar.
 *
 * Hafta içi etiketleri: gün adının ilk harfi (Pzt=P, Sal=S …).
 */
class UykuWidget : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_uyku)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.uyBg, context, pal)

                // v10.20: uyku yazıları serbest ölçek (genel × örnek) —
                // XML metinleri ve grafikteki gün harfleri birlikte ölçeklenir
                val uyO = WidgetSecim.ornekFaktor(context, WidgetSecim.ORNEK_UY) * WidgetCommon.yaziOlcek
                WidgetCommon.olcekliYazi(views, context, R.id.uyTitle, R.dimen.ga_yazi_normal, uyO)
                // v10.21: başlık çubuğu kullanıcı denetiminde (varsayılan açık)
                WidgetCommon.goster(views, R.id.uyTitle, WidgetSecim.goster(context, WidgetSecim.W_UY_BASLIK))
                WidgetCommon.olcekliYazi(views, context, R.id.uyOrt, R.dimen.ga_yazi_kucuk, uyO)
                WidgetCommon.olcekliYazi(views, context, R.id.uyHedef, R.dimen.ga_yazi_kucuk, uyO)

                val defter = UykuCerceve.defter(context)
                    .filter { it.uykuMs > 0L }
                    .sortedBy { it.gunKey }
                val geceler = UykuPano.son7(defter.map { it.uykuMs })
                val plan = UykuPano.planMs(
                    UykuCerceve.sabahDk(context), UykuCerceve.aksamDk(context)
                )
                // v10.17: plan çizgisi ve gün harfleri kullanıcı denetiminde
                views.setImageViewBitmap(
                    R.id.uyGrafik,
                    grafik(
                        pal, geceler, plan,
                        planGoster = WidgetSecim.goster(context, WidgetSecim.W_UY_PLAN),
                        harfGoster = WidgetSecim.goster(context, WidgetSecim.W_UY_HARF),
                        olcek = uyO
                    )
                )

                // Ortalama + bu gece hedefi
                val ortalama = defter.takeLast(7).map { it.uykuMs }
                    .let { if (it.isEmpty()) 0L else it.sum() / it.size }
                views.setTextViewText(
                    R.id.uyOrt,
                    if (ortalama > 0L) {
                        context.getString(R.string.wg_uyku_ort, UykuCerceve.sureKisa(ortalama))
                    } else {
                        context.getString(R.string.wg_uyku_veri_yok)
                    }
                )
                views.setTextViewText(
                    R.id.uyHedef,
                    context.getString(
                        R.string.wg_uyku_hedef,
                        UykuCerceve.saatMetni(UykuCerceve.aksamDk(context))
                    )
                )
                // v10.17: ortalama ve hedef satırları kullanıcı denetiminde
                WidgetCommon.goster(views, R.id.uyOrt, WidgetSecim.goster(context, WidgetSecim.W_UY_ORT))
                WidgetCommon.goster(views, R.id.uyHedef, WidgetSecim.goster(context, WidgetSecim.W_UY_HEDEF))

                WidgetTema.uygula(
                    views, pal,
                    metinler = intArrayOf(R.id.uyTitle),
                    soluklar = intArrayOf(R.id.uyOrt, R.id.uyHedef)
                )

                // Gövde → uyku ayarları ekranı
                val niyet = android.content.Intent(context, UykuAyarActivity::class.java)
                views.setOnClickPendingIntent(
                    R.id.uyRoot,
                    android.app.PendingIntent.getActivity(
                        context, 4975, niyet,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("UykuWidget", "Çizilemedi", e)
            }
            manager.updateAppWidget(widgetId, views)
        }

        // ---------------- Çubuk grafik bitmap'i ----------------

        private fun grafik(
            pal: WidgetTema.Palet,
            geceler: List<Long>,
            planMs: Long,
            planGoster: Boolean = true,
            harfGoster: Boolean = true,
            olcek: Float = 1f
        ): Bitmap {
            val gen = 400
            val yuk = 150
            val bmp = Bitmap.createBitmap(gen, yuk, Bitmap.Config.ARGB_8888)
            val tuval = Canvas(bmp)
            val maks = UykuPano.maksMs(planMs, geceler).toFloat()

            val cubuk = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val harfBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = yuk * 0.11f * olcek
                color = pal.metinSoluk
            }
            val yer = yuk * 0.82f   // çubukların taban çizgisi
            val tavan = yuk * 0.06f
            val yarar = yer - tavan
            val adim = gen / 7f

            // Plan çizgisi (kesikli): planlanan sürenin yüksekliği
            val planOran = (planMs / maks).coerceIn(0f, 1f)
            val planY = yer - yarar * planOran
            val planCizgi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = pal.metinSoluk
                pathEffect = DashPathEffect(floatArrayOf(8f, 7f), 0f)
            }
            if (planGoster) tuval.drawLine(0f, planY, gen.toFloat(), planY, planCizgi)

            geceler.forEachIndexed { i, uyku ->
                val sol = adim * i + adim * 0.22f
                val sag = adim * i + adim * 0.78f
                if (uyku > 0L) {
                    val oran = UykuPano.oran(uyku, maks.toLong())
                    val ustY = yer - yarar * oran
                    val planUstu = uyku >= planMs
                    cubuk.color = when {
                        i == geceler.lastIndex -> pal.vurgu
                        planUstu -> pal.yesil
                        else -> (pal.vurgu and 0x00FFFFFF) or 0x99000000.toInt()
                    }
                    tuval.drawRoundRect(
                        sol, ustY, sag, yer, adim * 0.10f, adim * 0.10f, cubuk
                    )
                } else {
                    // Kayıtsız gece: taban üstünde ince boş çizgi işareti
                    cubuk.color = (pal.metinSoluk and 0x00FFFFFF) or 0x33000000
                    tuval.drawRoundRect(sol, yer - yuk * 0.012f, sag, yer, 3f, 3f, cubuk)
                }
                // Gün harfi: defterin son günü = bugün (sağ uç)
                val gun = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, i - (geceler.size - 1))
                }
                val ad = gun.getDisplayName(
                    Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("tr", "TR")
                ) ?: ""
                if (harfGoster) {
                    tuval.drawText(
                        ad.take(1),
                        adim * i + adim / 2f, yuk * 0.97f, harfBoya
                    )
                }
            }
            return bmp
        }
    }
}
