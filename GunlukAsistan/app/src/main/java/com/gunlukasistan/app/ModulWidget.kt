package com.gunlukasistan.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews

/**
 * v10.16 · KULLANICI İSTEĞİ — Birleştirilebilir widget (4×2 taban).
 *
 * Kullanıcının ⚙ ekranında seçip sıraladığı modüller ([Modul])
 * yükseklik bütçesine sığdığı kadarıyla 8 metin slotuna üstten dizilir.
 * Çalışma anında içerik üretemeyen modül (koşmayan sayaç, uyku kaydı
 * olmayan defter) sessizce DÜŞER; altındaki modül yukarı kayar.
 *
 * ── Dokunuş ──
 * · Gövde: ana ekran.
 * · ⚙ bandı: bu örneğin modül düzenleyicisi ([ModulAyarActivity]) —
 *   aynı widget'ın birden çok örneği bağımsız sıralanabilir.
 */
class ModulWidget : AppWidgetProvider() {

    companion object {
        const val PI_ANA = 4938
        private const val PREF = "widget_modul_v1"
        private fun anahtar(widgetId: Int) = "sira_$widgetId"

        /**
         * Örneğin modül sırası: hiç yapılandırılmadıysa varsayılan 4'lü;
         * yapılandırıldıysa kayıtlı liste (BOŞ da olabilir — kullanıcı
         * tüm modülleri kapattıysa buna saygı duyulur, boş-durum metni
         * gösterilir).
         */
        fun siraOku(context: Context, widgetId: Int): List<String> {
            val sp = context.getSharedPreferences(PREF, 0)
            if (!sp.contains(anahtar(widgetId))) return Modul.varsayilanSira()
            val ham = sp.getString(anahtar(widgetId), "") ?: ""
            return Modul.temizle(ham.split(",").filter { it.isNotBlank() })
        }

        fun siraYaz(context: Context, widgetId: Int, sira: List<String>) {
            context.getSharedPreferences(PREF, 0).edit()
                .putString(anahtar(widgetId), sira.joinToString(",")).apply()
        }

        // ---------------- Satır üreticileri (cihaz okur) ----------------

        private data class Satir(val metin: String, val buyuk: Boolean = false)

        private fun saatBolumu(context: Context): List<Satir> {
            val simdi = System.currentTimeMillis()
            val saat = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(simdi)
            val gun = java.text.SimpleDateFormat("d MMMM EEEE", java.util.Locale("tr")).format(simdi)
            return listOf(Satir("🕐 $saat", buyuk = true), Satir(gun))
        }

        private fun sayacSatiri(context: Context): List<Satir> =
            if (runCatching { TimerEngine.isRunning(context) }.getOrDefault(false)) {
                val t = (TimerEngine.displayMs(context) / 1000).coerceAtLeast(0)
                val kronoMi = runCatching { TimerEngine.mode(context) == TimerEngine.MODE_WATCH }.getOrDefault(false)
                listOf(
                    Satir(
                        context.getString(
                            if (kronoMi) R.string.wa_sayac_krono else R.string.wa_sayac,
                            "%02d:%02d".format(t / 60, t % 60)
                        )
                    )
                )
            } else emptyList()

        private fun gorevSatirlari(context: Context): List<Satir> = runCatching {
            val tum = Store.loadTasks(context).filter { !it.done }
            val simdiki = tum.filter { it.dueAt > 0L }.sortedBy { it.dueAt }
                .plus(tum.filter { it.dueAt <= 0L }).take(2)
            simdiki.map {
                val e = Etiket.bul(it.etiket)?.emoji
                val kisa = if (it.text.length > 34) it.text.take(34) + "…" else it.text
                Satir((e?.let { "$it " } ?: "• ") + kisa)
            }
        }.getOrDefault(emptyList())

        private fun seriSatiri(context: Context): Satir {
            val (seri, _) = runCatching { Store.streakInfo(context) }.getOrDefault(0 to 0)
            return Satir(
                if (seri > 0) context.getString(R.string.wa_seri, seri)
                else context.getString(R.string.wa_seri_yok)
            )
        }

        private fun uykuSatiri(context: Context): Satir? = runCatching {
            UykuCerceve.defter(context).lastOrNull { it.uykuMs > 0L }?.let {
                Satir(context.getString(R.string.wa_uyku, UykuCerceve.sureKisa(it.uykuMs)))
            }
        }.getOrNull()

        private fun kapiSatiri(context: Context): Satir = runCatching {
            val c = java.util.Calendar.getInstance()
            val dk = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
            if (dk < UykuCerceve.aksamDk(context)) {
                Satir(context.getString(R.string.wa_kapi_yatis, UykuCerceve.saatMetni(UykuCerceve.aksamDk(context))))
            } else {
                Satir(context.getString(R.string.wa_kapi_uyanis, UykuCerceve.saatMetni(UykuCerceve.sabahDk(context))))
            }
        }.getOrDefault(Satir(context.getString(R.string.gc_panel_kapi_yok)))

        private fun kronotipSatiri(context: Context): Satir? = runCatching {
            val uyanislar = UykuCerceve.defter(context).mapNotNull { g ->
                if (g.uyandiMs <= 0L) null
                else java.util.Calendar.getInstance().apply { timeInMillis = g.uyandiMs }
                    .let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }
            }
            if (uyanislar.size < 5) return@runCatching null
            val ort = Kronotip.ortUyanis(uyanislar)
            val tip = Kronotip.tip(ort)
            val ad = context.getString(
                when (tip) {
                    Kronotip.Tip.SERCE -> R.string.ge_tip_serce
                    Kronotip.Tip.GECE_KUSU -> R.string.ge_tip_gece
                    else -> R.string.ge_tip_guvencin
                }
            )
            Satir("${Kronotip.tipEmoji(tip)} $ad · ${UykuCerceve.saatMetni(ort)}")
        }.getOrNull()

        private fun satirlariUret(context: Context, anahtar: String): List<Satir> = when (anahtar) {
            "saat" -> saatBolumu(context)
            "sayac" -> sayacSatiri(context)
            "gorevler" -> gorevSatirlari(context)
            "seri" -> listOf(seriSatiri(context))
            "uyku" -> listOfNotNull(uykuSatiri(context))
            "kapi" -> listOf(kapiSatiri(context))
            "kronotip" -> listOfNotNull(kronotipSatiri(context))
            else -> emptyList()
        }

        // ---------------- Render ----------------

        private val SLOT = intArrayOf(
            R.id.mdS1, R.id.mdS2, R.id.mdS3, R.id.mdS4,
            R.id.mdS5, R.id.mdS6, R.id.mdS7, R.id.mdS8
        )

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_modul)
            try {
                val pal = WidgetTema.palet(context)
                WidgetZemin.uygula(views, R.id.modulBg, context, pal)
                WidgetAtolye.kokDolguUygula(views, R.id.modulRoot, context)

                val butce = WidgetCommon.sigacakSatir(
                    WidgetCommon.yukseklikDp(manager, widgetId), 30, 46
                ).coerceIn(2, 8)

                // Üstten sırayla diz: üretemeyen modül düşer (yer kaplamaz),
                // bütçesi yetmeyen atlanır, sonraki sırada değerlendirilir.
                val satirlar = mutableListOf<Satir>()
                for (a in siraOku(context, widgetId)) {
                    val maliyet = Modul.tanim(a)?.satir ?: continue
                    if (maliyet > butce - satirlar.size) continue
                    val uretilen = satirlariUret(context, a).take(maliyet)
                    satirlar.addAll(uretilen)
                    if (satirlar.size >= butce) break
                }
                if (satirlar.isEmpty()) {
                    satirlar.add(Satir(context.getString(R.string.wa_bos)))
                }

                val dp = context.resources.displayMetrics.density
                val carpan = WidgetAtolye.yaziCarpan(context)
                val dolu = satirlar.take(8)
                SLOT.forEachIndexed { i, id ->
                    val s = dolu.getOrNull(i)
                    if (s == null) {
                        views.setViewVisibility(id, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(id, android.view.View.VISIBLE)
                        views.setTextViewText(id, s.metin)
                        views.setTextColor(id, if (s.buyuk) pal.vurgu else pal.metin)
                        views.setTextViewTextSize(
                            id, TypedValue.COMPLEX_UNIT_PX,
                            (if (s.buyuk) 22f else 13f) * carpan * dp
                        )
                        WidgetAtolye.satirDolguUygula(views, id, context)
                    }
                }
                views.setTextColor(R.id.mdAyar, pal.metinSoluk)

                // Dokunuşlar
                views.setOnClickPendingIntent(
                    R.id.modulRoot,
                    PendingIntent.getActivity(
                        context, PI_ANA,
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, WidgetCommon.SCREEN_HOME)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                views.setOnClickPendingIntent(
                    R.id.mdAyar,
                    PendingIntent.getActivity(
                        context, widgetId,
                        Intent(context, ModulAyarActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            data = android.net.Uri.parse("ga://modul/$widgetId")
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("ModulWidget", "Render hatası", e)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, manager, id)
    }
}
