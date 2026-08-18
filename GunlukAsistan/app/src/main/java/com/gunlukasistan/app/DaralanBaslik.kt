package com.gunlukasistan.app

import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView

/**
 * v9.9 — Kaydırmada daralan başlık (görsel öneri 8).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN CollapsingToolbarLayout KULLANMADIM
 * ══════════════════════════════════════════════════════════════════
 * Standart çözüm `AppBarLayout` + `CollapsingToolbarLayout` +
 * `CoordinatorLayout` üçlüsü. Denemedim değil — vazgeçtim:
 *
 *   · Ana ekran (`fragment_home`) düz bir `ScrollView`. Üçlüye
 *     geçmek layout'un tamamını yeniden yazmak demek: 8 blok,
 *     `AnaEkranDuzen` sürükle-sırala sistemi ve `blokHero`
 *     görünürlük mantığı buna bağlı.
 *   · `CoordinatorLayout` iç içe kaydırma (nested scrolling)
 *     gerektiriyor; `ScrollView` bunu desteklemiyor,
 *     `NestedScrollView`'a geçmek gerekiyor.
 *   · Kazanç: aynı görsel etki. Risk: v8.5'te yazılan blok
 *     sıralama sistemini kırmak.
 *
 * **Bunun yerine kaydırma dinleyicisi.** 40 satır kod, hiçbir
 * layout değişmiyor, aynı his.
 *
 * ══════════════════════════════════════════════════════════════════
 * NASIL ÇALIŞIYOR
 * ══════════════════════════════════════════════════════════════════
 * Kaydırma miktarına göre 0..1 arası bir oran hesaplanıyor:
 *
 * ```
 * oran = 0   → tepe:   büyük başlık görünür, üst bar saydam
 * oran = 1   → aşağı:  büyük başlık kaybolur, üst bar dolu
 * ```
 *
 * Büyük başlık **yukarı kayarak** soluyor, küçük başlık
 * **aşağıdan gelerek** beliriyor. İki hareket ters yönde olduğu
 * için göz "yerine oturdu" hissi alıyor.
 *
 * ── Neden alfa değil de translationY + alfa birlikte ──
 * Yalnız alfa kullanmak metni "hayalet" gibi soldurur, ucuz
 * görünür. Hafif bir dikey kayma (12dp) hareketi fiziksel yapıyor.
 */
object DaralanBaslik {

    private const val TAG = "DaralanBaslik"

    /** Bu mesafeden sonra geçiş tamamlanmış sayılır (dp). */
    private const val MESAFE_DP = 110f

    /** Büyük başlığın kayacağı mesafe (dp). */
    private const val KAYMA_DP = 14f

    /**
     * Kaydırma bağlar.
     *
     * @param kaydirici izlenecek ScrollView
     * @param buyukBaslik tepedeki büyük başlık (yukarı kayıp solacak)
     * @param kucukBaslik üst bardaki küçük başlık (belirecek) — null olabilir
     * @param arkaPlan üst bar arka planı (soluk → dolu) — null olabilir
     */
    fun bagla(
        kaydirici: ScrollView?,
        buyukBaslik: View?,
        kucukBaslik: TextView? = null,
        arkaPlan: View? = null
    ) {
        if (kaydirici == null) return
        val yg = kaydirici.resources.displayMetrics.density
        val mesafe = MESAFE_DP * yg
        val kayma = KAYMA_DP * yg

        // Başlangıç durumu
        kucukBaslik?.alpha = 0f
        arkaPlan?.alpha = 0f

        runCatching {
            kaydirici.viewTreeObserver.addOnScrollChangedListener {
                // Görünüm yok edildiyse dokunma — Fragment geri
                // yığındayken dinleyici hâlâ tetiklenebiliyor ve
                // `requireContext()` çöküyordu (v8.9 dersi).
                if (!kaydirici.isAttachedToWindow) return@addOnScrollChangedListener
                uygula(kaydirici.scrollY, mesafe, kayma, buyukBaslik, kucukBaslik, arkaPlan)
            }
        }.onFailure { android.util.Log.w(TAG, "Dinleyici eklenemedi", it) }
    }

    /** `NestedScrollView` sürümü — bazı ekranlar bunu kullanıyor. */
    fun bagla(
        kaydirici: NestedScrollView?,
        buyukBaslik: View?,
        kucukBaslik: TextView? = null,
        arkaPlan: View? = null
    ) {
        if (kaydirici == null) return
        val yg = kaydirici.resources.displayMetrics.density
        val mesafe = MESAFE_DP * yg
        val kayma = KAYMA_DP * yg

        kucukBaslik?.alpha = 0f
        arkaPlan?.alpha = 0f

        runCatching {
            kaydirici.setOnScrollChangeListener { _: NestedScrollView?, _: Int, y: Int, _: Int, _: Int ->
                uygula(y, mesafe, kayma, buyukBaslik, kucukBaslik, arkaPlan)
            }
        }.onFailure { android.util.Log.w(TAG, "Dinleyici eklenemedi", it) }
    }

    /**
     * Oranı hesaplayıp görünümlere uygular.
     *
     * ── Neden animasyon yok ──
     * Bu fonksiyon kaydırma sırasında saniyede ~60 kez çağrılıyor.
     * `ViewPropertyAnimator` başlatmak her karede yeni bir animasyon
     * kuyruğa alır ve takılmaya yol açar. Değerler doğrudan
     * atanıyor; hareketin kendisi zaten parmağın hareketi.
     */
    private fun uygula(
        y: Int,
        mesafe: Float,
        kayma: Float,
        buyukBaslik: View?,
        kucukBaslik: TextView?,
        arkaPlan: View?
    ) {
        runCatching {
            val oran = (y / mesafe).coerceIn(0f, 1f)

            buyukBaslik?.let {
                it.alpha = 1f - oran
                it.translationY = -kayma * oran
            }
            kucukBaslik?.let {
                it.alpha = oran
                // Küçük başlık aşağıdan geliyor: ters yön
                it.translationY = kayma * (1f - oran)
            }
            arkaPlan?.alpha = oran
        }
    }

    /**
     * Görünümü sıfırlar — ekran yeniden gösterilince.
     *
     * Fragment `hide()`/`show()` ile yönetiliyor; geri dönüldüğünde
     * kaydırma konumu korunuyor ama dinleyici tetiklenmiyor.
     * Sıfırlamazsak başlık yanlış durumda kalıyor.
     */
    fun tazele(
        kaydirici: ScrollView?,
        buyukBaslik: View?,
        kucukBaslik: TextView? = null,
        arkaPlan: View? = null
    ) {
        if (kaydirici == null) return
        val yg = kaydirici.resources.displayMetrics.density
        uygula(
            kaydirici.scrollY, MESAFE_DP * yg, KAYMA_DP * yg,
            buyukBaslik, kucukBaslik, arkaPlan
        )
    }
}
