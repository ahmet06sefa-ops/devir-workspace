package com.gunlukasistan.app

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.MaterialColors

/**
 * v7.58 — Üstten aşağı çekince yenileme (pull-to-refresh).
 *
 * ── Kullanıcının isteği ──
 * "Üstten aşağı kaydırınca otomatik olarak sayfayı yenilesin ve
 *  güncellesin, diğer sayfaları da"
 *
 * ── Neden bu yaklaşım ──
 * 16 fragment layout'unu tek tek `SwipeRefreshLayout` ile sarmak
 * 16 XML dosyasını değiştirmek demekti — hem hataya açık hem bakımı zor.
 * Bunun yerine fragment'ın kök görünümünü **çalışma anında** sarmalıyoruz.
 * Tek satırla her ekrana eklenebiliyor, XML'lere dokunulmuyor.
 *
 * ── Kullanım ──
 * ```
 * override fun onViewCreated(view: View, s: Bundle?) {
 *     Yenileyici.kur(this) { reload() }
 * }
 * ```
 *
 * ── Kaydırma çakışması ──
 * `SwipeRefreshLayout` yalnızca içerik **en üstteyken** tetiklenir;
 * liste ortasındayken normal kaydırma çalışır. `RecyclerView`,
 * `ScrollView` ve `NestedScrollView` ile uyumludur.
 */
object Yenileyici {

    private const val TAG = "Yenileyici"

    /**
     * Fragment'ın kök görünümünü yenilenebilir hale getirir.
     *
     * @param fragment kök görünümü sarılacak fragment
     * @param yenile çekince çağrılacak iş (ana iş parçacığında)
     * @return oluşturulan katman (isteğe bağlı; null ise sarmalanamadı)
     */
    fun kur(fragment: Fragment, yenile: () -> Unit): SwipeRefreshLayout? {
        return try {
            val kok = fragment.view ?: return null
            val ebeveyn = kok.parent as? ViewGroup ?: return null

            // Zaten sarılmışsa tekrar sarma (yapılandırma değişikliği vb.)
            if (ebeveyn is SwipeRefreshLayout) return ebeveyn

            val sira = ebeveyn.indexOfChild(kok)
            val olculer = kok.layoutParams

            ebeveyn.removeViewAt(sira)

            val katman = SwipeRefreshLayout(fragment.requireContext()).apply {
                layoutParams = olculer
                renkleriUygula(this)
                addView(
                    kok,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                setOnRefreshListener {
                    try {
                        yenile()
                        // v8.2 · Öneri 8: yenilendiği görünsün.
                        //
                        // Eskiden çark bir an dönüp kayboluyordu; veri
                        // zaten yerelden okunduğu için hiçbir şey
                        // değişmemiş gibi duruyordu ve kullanıcı
                        // "yenilendi mi?" diye tekrar çekiyordu.
                        Titresim.tik(this)
                        onayGoster(this)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Yenileme başarısız", e)
                    } finally {
                        // Yenileme eşzamanlı; çarkı hemen kapat
                        isRefreshing = false
                    }
                }
            }

            ebeveyn.addView(katman, sira)
            // v7.62 KRITIK: fragment o an gizliyse katman da gizlenmeli.
            // Aksi halde gorunmez sarmalayici tum konteyneri kaplar ve
            // ustteki ekranin dokunuslarini yutar.
            katman.visibility = if (fragment.isHidden) View.GONE else View.VISIBLE
            katman
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yenileyici kurulamadı", e)
            null
        }
    }

    /**
     * Ağ işi gibi uzun süren yenilemeler için.
     * Çark, [bitir] çağrılana kadar döner.
     *
     * @param yenile `bitir` geri çağrısını alan iş
     */
    fun kurUzun(fragment: Fragment, yenile: (bitir: () -> Unit) -> Unit): SwipeRefreshLayout? {
        val katman = kur(fragment) {} ?: return null
        katman.setOnRefreshListener {
            try {
                yenile { katman.isRefreshing = false }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Yenileme başarısız", e)
                katman.isRefreshing = false
            }
        }
        return katman
    }

    /**
     * v8.2 — "Güncellendi" onayı.
     *
     * Snackbar yerine kısa bir Toast: Snackbar alt menünün üstünde
     * yer kaplıyor ve 2 saniye boyunca FAB'ı itiyor. Yenileme çok
     * sık yapılan bir işlem; bildirimi olabildiğince hafif olmalı.
     */
    private fun onayGoster(katman: SwipeRefreshLayout) {
        runCatching {
            val ctx = katman.context
            android.widget.Toast
                .makeText(ctx, ctx.getString(R.string.gr_yenilendi), android.widget.Toast.LENGTH_SHORT)
                .show()
        }.onFailure { android.util.Log.w(TAG, "Onay gösterilemedi", it) }
    }

    /** Çarkı uygulamanın vurgu rengiyle boyar. */
    private fun renkleriUygula(katman: SwipeRefreshLayout) {
        try {
            val vurgu = MaterialColors.getColor(
                katman, com.google.android.material.R.attr.colorPrimary, 0
            )
            val zemin = MaterialColors.getColor(
                katman, com.google.android.material.R.attr.colorSurface, 0
            )
            katman.setColorSchemeColors(vurgu)
            katman.setProgressBackgroundColorSchemeColor(zemin)
            // v8.2: çark biraz büyük ve biraz aşağıda olsun — üst
            // araç çubuğunun altından çıkıyordu, yarısı görünmüyordu.
            katman.setSize(SwipeRefreshLayout.DEFAULT)
            val yg = katman.resources.displayMetrics.density
            katman.setProgressViewOffset(false, (8 * yg).toInt(), (72 * yg).toInt())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Renk uygulanamadı", e)
        }
    }

    /**
     * v7.62 — Sarmalayicinin gorunurlugunu fragment ile esitler.
     *
     * ── Duzeltilen hata (v7.58 regresyonu) ──
     * Ekranlar `FragmentTransaction.hide()` ile gizleniyor; bu yalnizca
     * FRAGMENT'in kendi gorunumunu GONE yapar. v7.58'de kok gorunum
     * calisma aninda bir `SwipeRefreshLayout` icine alindigi icin
     * sarmalayici VISIBLE kaliyordu: icerigi bos ama tum konteyneri
     * kaplayan seffaf bir katman. Ust uste binen bu katmanlar acik
     * ekranin butonlarina giden dokunuslari yutuyordu — kullanicinin
     * "hicbir butona basmiyor" dedigi sorun buydu.
     *
     * MainActivity.open() her gecin sonunda bunu cagirir.
     */
    fun gorunurluguEsitle(fragment: Fragment) {
        try {
            val katman = bul(fragment.view) ?: return
            val hedef = if (fragment.isHidden) View.GONE else View.VISIBLE
            if (katman.visibility != hedef) katman.visibility = hedef
            // Gizliyken yenileme carki donuyor kalmasin
            if (fragment.isHidden && katman.isRefreshing) katman.isRefreshing = false
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Gorunurluk esitlenemedi", e)
        }
    }

    /**
     * Bir görünümün içindeki `SwipeRefreshLayout`'u bulur.
     * Ekran dışarıdan tazelendiğinde çarkı durdurmak için kullanılır.
     */
    fun bul(view: View?): SwipeRefreshLayout? {
        var p = view?.parent
        while (p != null) {
            if (p is SwipeRefreshLayout) return p
            p = p.parent
        }
        return null
    }
}
