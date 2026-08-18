package com.gunlukasistan.app

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

/**
 * v10.18 · EKRAN ATÖLYESİ — düzenlenebilir blok kapsayıcısı.
 *
 * ── Kullanıcının isteği ──
 * "Öğelerin üstüne basılı tutunca boyutlarını, yerlerini
 * değiştirebileyim."
 *
 * ── Neden özel sınıf gerekti ──
 * Düzenleme modunda bloğun İÇİNDEKİ düğmelere basılınca bile
 * dokunuşun çocuğa gitmemesi, bloğun kendisinin seçilmesi gerekiyor.
 * Bunun yerli yolu `onInterceptTouchEvent`: ebeveyn, dokunuşu
 * çocuğa ULAŞMADAN kesebilir. Düz `LinearLayout` bunu yapmaz;
 * bu sınıf yalnız şunu ekler:
 *
 *   Düzenleme modu açık VE bu ekranın kökü aktif kökse
 *   → dokunuşlar çocuğa gitmez, kapsayıcıya düşer (seçim).
 *
 * Mod kapalıyken davranış düz LinearLayout ile birebir aynıdır —
 * sıfır yan etki.
 */
class DuzenBlokLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val kok = DuzenSeridi.aktifKokTutucu
        // Yalnız AKTİF ekranın blokları keser: iki fragment aynı aktivitede
        // yaşadığı için kök soyundanlığı denetlenir (çapraz sızma yok).
        if (DuzenSeridi.duzenModuAktif && kok != null &&
            DuzenSeridi.soyundanMi(this, kok)
        ) {
            // Düzenleme modunda çocuk düğmeler susar; dokunuş seçimdir.
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }
}
