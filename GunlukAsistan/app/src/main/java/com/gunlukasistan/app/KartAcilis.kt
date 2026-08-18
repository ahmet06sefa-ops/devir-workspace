package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.core.app.ActivityOptionsCompat

/**
 * v9.9 — Karttan büyüyen ekran geçişi (görsel öneri 9).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN "GERÇEK" PAYLAŞILAN ÖĞE GEÇİŞİ DEĞİL
 * ══════════════════════════════════════════════════════════════════
 * Öneri listesinde "paylaşılan öğe geçişi" (shared element
 * transition) yazmıştım. Android'in `ActivityOptions
 * .makeSceneTransitionAnimation` API'si bunu yapıyor: karttaki
 * görsel, hedef ekrandaki karşılığına **dönüşüyor**.
 *
 * Bunu tam anlamıyla uygulamadım. Sebep — ve bu dürüst bir sınır:
 *
 *   1. `transitionName` hem kaynak hem hedefte tanımlı olmalı.
 *      Hedef ekranların çoğu (`TakipActivity`, `SistemActivity`,
 *      `OgrenmeActivity`) **kodla çiziliyor**, XML layout'ları yok.
 *      Eşleşecek bir görünüm yok.
 *   2. Uygulamanın ana gezinmesi Activity değil **Fragment**
 *      (`MainActivity.open()` hide/show yapıyor). Scene transition
 *      Activity'ler arası çalışıyor.
 *   3. Doğru yapmak her hedef ekrana özel layout yazmak demek —
 *      40+ ekran.
 *
 * ── Bunun yerine: ActivityOptions.makeScaleUpAnimation ──
 * Yeni ekran **dokunulan kartın konumundan** büyüyerek açılıyor.
 * Gerçek morph değil ama:
 *   · Kaynak-hedef bağlantısı hissediliyor
 *   · Tek satır kod, hiçbir layout değişmiyor
 *   · Her Android sürümünde çalışıyor
 *   · Kodla çizilen ekranlarda da çalışıyor
 *
 * Bu bir uzlaşma ve öyle olduğunu yazıyorum. Kullanıcıya
 * "paylaşılan öğe geçişi yaptım" demek yanlış olurdu.
 */
object KartAcilis {

    private const val TAG = "KartAcilis"

    /**
     * Activity'yi kaynak görünümün konumundan büyüterek açar.
     *
     * ```kotlin
     * KartAcilis.ac(kart, TakipActivity::class.java)
     * ```
     *
     * @param kaynak dokunulan görünüm (kart, satır)
     * @param hedef açılacak Activity sınıfı
     * @param ekstra Intent'e eklenecek değerler
     * @return açılabildiyse true
     */
    fun ac(
        kaynak: View?,
        hedef: Class<*>,
        ekstra: (Intent.() -> Unit)? = null
    ): Boolean {
        val context = kaynak?.context ?: return false
        return runCatching {
            val niyet = Intent(context, hedef)
            ekstra?.invoke(niyet)
            context.startActivity(niyet, secenekler(kaynak))
            // Sistem animasyonu devraldığı için kendi geçişimizi
            // ÇALIŞTIRMIYORUZ. İkisi birden çalışırsa ekran iki kez
            // hareket ediyor ve titriyor.
            true
        }.getOrElse {
            android.util.Log.w(TAG, "Kart açılışı başarısız", it)
            // Geri düşüş: normal açılış
            runCatching {
                val niyet = Intent(context, hedef)
                ekstra?.invoke(niyet)
                context.startActivity(niyet)
                (context as? Activity)?.let { a -> Canlandir.activityGirisi(a) }
                true
            }.getOrDefault(false)
        }
    }

    /**
     * Hazır bir Intent ile açar.
     *
     * Bazı çağıranlar Intent'i kendileri kuruyor (bayraklar,
     * karmaşık ekstralar). Onlar için ayrı giriş.
     */
    fun ac(kaynak: View?, niyet: Intent): Boolean {
        val context = kaynak?.context ?: return false
        return runCatching {
            context.startActivity(niyet, secenekler(kaynak))
            true
        }.getOrElse {
            runCatching {
                context.startActivity(niyet)
                (context as? Activity)?.let { a -> Canlandir.activityGirisi(a) }
                true
            }.getOrDefault(false)
        }
    }

    /**
     * Kaynak görünümün ekrandaki konumundan büyüme animasyonu üretir.
     *
     * ── Neden merkeze göre değil de tam dikdörtgen ──
     * `makeScaleUpAnimation` başlangıç dikdörtgenini alıyor.
     * Kartın **kendi boyutundan** başlatmak, kartın büyüyüp ekranı
     * kapladığı hissini veriyor. Tek noktadan başlatmak (0,0
     * boyutlu) patlama efekti gibi durur.
     *
     * @return animasyon paketi veya null (animasyon kapalıysa)
     */
    private fun secenekler(kaynak: View): android.os.Bundle? {
        // Kullanıcı animasyonları kapattıysa saygı göster
        if (!animasyonAcik(kaynak.context)) return null

        return runCatching {
            val genislik = kaynak.width
            val yukseklik = kaynak.height
            // Ölçülmemiş görünüm (henüz layout olmamış) → animasyon yok
            if (genislik <= 0 || yukseklik <= 0) return null

            ActivityOptionsCompat.makeScaleUpAnimation(
                kaynak, 0, 0, genislik, yukseklik
            ).toBundle()
        }.getOrNull()
    }

    /**
     * Animasyon tercihi.
     *
     * v8.2'de eklenen `GorunumAyar` kullanıcıya animasyonları
     * kapatma seçeneği veriyor; ayrıca sistem genelinde
     * "animasyonları kaldır" erişilebilirlik ayarı var. İkisine de
     * uymak gerekiyor — hareket duyarlılığı olan kullanıcılar için
     * bu bir konfor değil, gereklilik.
     */
    private fun animasyonAcik(context: Context): Boolean = runCatching {
        if (!GorunumAyar.animasyonAcik(context)) return false
        // Sistem ayarı: Settings.Global.ANIMATOR_DURATION_SCALE = 0
        val olcek = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        olcek > 0f
    }.getOrDefault(true)

    /**
     * Kartın ekrandaki dikdörtgeni — hata ayıklama / özel kullanım.
     */
    fun konum(gorunum: View?): Rect? = runCatching {
        if (gorunum == null) return null
        val konum = IntArray(2)
        gorunum.getLocationOnScreen(konum)
        Rect(konum[0], konum[1], konum[0] + gorunum.width, konum[1] + gorunum.height)
    }.getOrNull()
}
