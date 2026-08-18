package com.gunlukasistan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews

/**
 * v10.13 · ULTRA-30 / B11 — Dinamik widget zemini.
 *
 * ── Sorun ──
 * Köşe yarıçapı ve saydamlık birer kaydırıcı olmak istiyor ama
 * `RemoteViews.setBackgroundResource` yalnız hazır kaynak kabul eder:
 * 4 saydamlık × 4 köşe × 2 zemin = 32 çekmece dosyası (kaynak şişmesi)
 * ya da köşeden vazgeçmek gerekiyordu.
 *
 * ── Çözüm ──
 * Yeni widget ailesinin kökü bir `FrameLayout`; en altta duran
 * `ImageView`'e **üretilmiş bitmap** basılır (`setImageViewBitmap` —
 * RemoteViews'un izinli yolu). Böylece yarıçap ve alfa uçtan uca
 * serbesttir; tek dosya, sıfır şişme.
 *
 * ── Kapsam sınırı (dürüstlük) ──
 * Var olan 12 widget, kökleri hazır shape kaynaklarına (`w_card_*`)
 * bağlı olduğu için saydamlık varyantlarıyla çalışmaya devam eder;
 * köşe kaydırıcısı yeni aileyle sınırlıdır. Bu not ayarda da yazar.
 *
 * Kademe ↔ değer eşlemeleri saf fonksiyonlardır ve birim testlidir.
 */
object WidgetZemin {

    // ---------------- Saf eşlemeler (birim testli) ----------------

    /** Köşe kademesi → dp. Varsayılan 1 (mevcut w_card ile aynı 26 dp). */
    fun koseDp(kademe: Int): Float = when (kademe.coerceIn(0, 3)) {
        0 -> 6f
        1 -> 26f
        2 -> 38f
        else -> 48f
    }

    /** Saydamlık seviyesi → alfa kanalı. (v10.20 öncesi kademe sistemi — geriye dönük) */
    fun saydamlikAlfa(seviye: Int): Int = when (seviye.coerceIn(0, 3)) {
        0 -> 0xFF
        1 -> 0xE0
        2 -> 0xC4
        else -> 0xA8
    }

    /**
     * v10.20 · KULLANICI İSTEĞİ — "sınır koyma": saydamlık artık serbest
     * yüzde (0% = opak … 100% = tamamen görünmez). 0-100 fiziksel aralığın
     * TAMAMIDIR; kademe eşlemesi yok, yazılan değer aynen uygulanır. Saf.
     */
    fun saydamlikYuzdeAlfa(yuzde: Int): Int =
        (255 * (100 - yuzde.coerceIn(0, 100))) / 100

    /**
     * v10.20 · Serbest köşe dp güvenlik tabanı: negatif yarıçap Canvas'ı
     * çökertir (teknik kelepçe — kullanıcı sınırı değil); 2000 dp tavanı
     * ise yalnız anlamsız değerleri tutar. Kademe dayatılmaz. Saf.
     */
    fun koseDpGuvenli(dp: Float): Float = dp.coerceIn(0f, 2000f)

    /** Yazı kademesi → çarpan. Aralık dışı güvenli biçimde 1.0'a düşer. */
    fun yaziCarpan(kademe: Int): Float = when (kademe) {
        0 -> 0.85f
        2 -> 1.15f
        else -> 1.0f
    }

    // ---------------- Bitmap üretimi ----------------

    /**
     * Yuvarlak köşeli zemin bitmap'i üretir.
     *
     * Kare çizilir; başlatıcı (launcher) kendi köşe kırpmasını da
     * uyguladığı için köşe yarıçapı görünürde küçülmez — dp değerleri
     * bu gerçek göz önünde seçildi.
     */
    fun zeminBitmap(
        zeminRenk: Int,
        kenarRenk: Int,
        alfa: Int,
        koseDpDeger: Float,
        kenarPx: Int = 128
    ): Bitmap {
        val bmp = Bitmap.createBitmap(kenarPx, kenarPx, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bmp)
        // Köşe yarıçapı: tuval 96 dp kabul edilerek dp → piksel çevrilir
        val koseRadyal = koseDpDeger / 96f * kenarPx
        val dikdortgen = RectF(2f, 2f, kenarPx - 2f, kenarPx - 2f)
        val dolgu = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(
                alfa.coerceIn(0, 255),
                Color.red(zeminRenk), Color.green(zeminRenk), Color.blue(zeminRenk)
            )
        }
        tuval.drawRoundRect(dikdortgen, koseRadyal, koseRadyal, dolgu)
        val kenar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(
                (alfa / 3).coerceIn(0, 255),
                Color.red(kenarRenk), Color.green(kenarRenk), Color.blue(kenarRenk)
            )
        }
        tuval.drawRoundRect(dikdortgen, koseRadyal, koseRadyal, kenar)
        return bmp
    }

    /**
     * Widget arka plan `ImageView`'ine seçili temayı, SERBEST saydamlığı
     * ve SERBEST köşe yarıçapını uygular (v10.20: kademe sistemi bitti).
     *
     * v10.20'de eski nesil 12 widget da bu yola geçti (`FrameLayout +
     * ImageView` sarmalı); [dolguKokId] verilirse içerik kökünün dolgusu
     * da aynı enjeksiyondan yayılır (eski `saydamlikUygula`'nın görevi).
     */
    fun uygula(
        views: RemoteViews,
        bgId: Int,
        context: Context,
        p: WidgetTema.Palet,
        dolguKokId: Int = 0
    ) {
        try {
            val bmp = zeminBitmap(
                zeminRenk = p.zemin,
                kenarRenk = p.vurgu,
                alfa = saydamlikYuzdeAlfa(WidgetTema.saydamlikPct(context)),
                koseDpDeger = koseDpGuvenli(WidgetTema.koseDpF(context))
            )
            views.setImageViewBitmap(bgId, bmp)
            if (dolguKokId != 0) WidgetAtolye.kokDolguUygula(views, dolguKokId, context)
        } catch (e: Exception) {
            android.util.Log.w("WidgetZemin", "Zemin üretilemedi", e)
        }
    }
}
