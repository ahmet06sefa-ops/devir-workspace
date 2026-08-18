package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar

/**
 * v8.6 — Tek tip ekran içi bildirim (öneri 26).
 *
 * ── Sorun ──
 * Uygulamada 200'den fazla `Toast.makeText` çağrısı var. Toast:
 *   · Sistemin gri kutusu — uygulamanın temasıyla hiç uyumlu değil
 *   · Android 12+ sürümlerinde uygulama simgesi de ekliyor, daha da
 *     yabancı duruyor
 *   · Eylem düğmesi taşıyamıyor ("Geri al" koyulamıyor)
 *   · Konumu ayarlanamıyor; alt menünün arkasında kalabiliyor
 *
 * Snackbar'lar da vardı ama **FAB'ın altında kalıyordu** — v7.72'den
 * beri "geri al" şeridi kısmen görünmüyordu.
 *
 * ── Ne yapıyor ──
 * Tek giriş noktası. Snackbar denenir; kök görünüm bulunamazsa
 * Toast'a düşülür (arka plan iş parçacığı, servis vb.).
 * Snackbar alt menünün ÜSTÜNE konumlanıyor (`anchorView`).
 *
 * ── Neden Toast tamamen kaldırılmadı ──
 * 200 çağrıyı tek sürümde değiştirmek riskli. Bu sınıf yeni kod ve
 * en görünür akışlar için; kalanlar zamanla geçirilebilir.
 * Ayrıca Toast'ın gerçekten doğru olduğu yerler var: Activity
 * kapanırken gösterilen mesajlar (Snackbar kökü yok olur).
 */
object Bildir {

    private const val TAG = "Bildir"

    /** Bilgi — nötr renk. */
    fun bilgi(kok: View?, mesaj: String) = goster(kok, mesaj, Tur.BILGI, null, null)

    fun bilgi(kok: View?, mesajRes: Int) =
        kok?.let { bilgi(it, it.context.getString(mesajRes)) }

    /** Başarı — yeşilimsi, kısa titreşim. */
    fun basari(kok: View?, mesaj: String) = goster(kok, mesaj, Tur.BASARI, null, null)

    fun basari(kok: View?, mesajRes: Int) =
        kok?.let { basari(it, it.context.getString(mesajRes)) }

    /** Hata — colorError zemin. */
    fun hata(kok: View?, mesaj: String) = goster(kok, mesaj, Tur.HATA, null, null)

    fun hata(kok: View?, mesajRes: Int) =
        kok?.let { hata(it, it.context.getString(mesajRes)) }

    /** Eylemli bildirim — "Geri al" gibi. */
    fun eylemli(
        kok: View?,
        mesaj: String,
        eylemEtiketi: String,
        eylem: () -> Unit
    ) = goster(kok, mesaj, Tur.BILGI, eylemEtiketi, eylem)

    private enum class Tur { BILGI, BASARI, HATA }

    // ------------------------------------------------------------------

    private fun goster(
        kok: View?,
        mesaj: String,
        tur: Tur,
        eylemEtiketi: String?,
        eylem: (() -> Unit)?
    ) {
        if (kok == null) return
        runCatching {
            val sure = if (eylem != null) 5000 else Snackbar.LENGTH_SHORT
            val cubuk = Snackbar.make(kok, mesaj, Snackbar.LENGTH_SHORT)
            cubuk.duration = sure

            // ---- Konum: alt menünün üstüne ----
            //
            // Snackbar varsayılan olarak ekranın en altına oturuyor ve
            // BottomNavigationView'ün ARKASINDA kalıyordu. anchorView
            // ile menünün üstüne itiliyor. FAB da ayrıca kaçıyor.
            runCatching {
                val etkinlik = kok.context as? Activity
                val nav = etkinlik?.findViewById<View>(R.id.bottomNav)
                if (nav != null && nav.isShown) cubuk.anchorView = nav
            }

            // ---- Renkler ----
            val gorunum = cubuk.view
            when (tur) {
                Tur.BASARI -> {
                    gorunum.setBackgroundColor(0xFF2E5B37.toInt())
                    metinRengi(cubuk, 0xFFDDEFDF.toInt())
                }
                Tur.HATA -> {
                    val hataRengi = MaterialColors.getColor(
                        kok, com.google.android.material.R.attr.colorError, 0xFFB3261E.toInt()
                    )
                    gorunum.setBackgroundColor(hataRengi)
                    metinRengi(cubuk, 0xFFFFFFFF.toInt())
                }
                Tur.BILGI -> {
                    val zemin = MaterialColors.getColor(
                        kok, com.google.android.material.R.attr.colorOnSurface, 0xFF333333.toInt()
                    )
                    val yazi = MaterialColors.getColor(
                        kok, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF.toInt()
                    )
                    gorunum.setBackgroundColor(zemin)
                    metinRengi(cubuk, yazi)
                }
            }

            // Köşeleri yuvarlat — Material 3 görünümü
            runCatching {
                val yg = kok.resources.displayMetrics.density
                val mevcut = gorunum.background
                gorunum.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = yg * 12
                    setColor(
                        when (tur) {
                            Tur.BASARI -> 0xFF2E5B37.toInt()
                            Tur.HATA -> MaterialColors.getColor(
                                kok, com.google.android.material.R.attr.colorError, 0xFFB3261E.toInt()
                            )
                            Tur.BILGI -> MaterialColors.getColor(
                                kok, com.google.android.material.R.attr.colorOnSurface, 0xFF333333.toInt()
                            )
                        }
                    )
                }
                val kenar = (yg * 10).toInt()
                (gorunum.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.apply {
                    setMargins(kenar, kenar, kenar, kenar)
                }
            }

            // ---- Eylem ----
            if (eylemEtiketi != null && eylem != null) {
                cubuk.setAction(eylemEtiketi) {
                    Titresim.dokunus(kok)
                    runCatching { eylem() }
                        .onFailure { android.util.Log.w(TAG, "eylem", it) }
                }
                cubuk.setActionTextColor(
                    MaterialColors.getColor(
                        kok, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFEFE2D0.toInt()
                    )
                )
            }

            if (tur == Tur.BASARI) Titresim.dokunus(kok)
            cubuk.show()
        }.onFailure {
            android.util.Log.w(TAG, "Snackbar gösterilemedi, Toast'a düşülüyor", it)
            runCatching {
                android.widget.Toast
                    .makeText(kok.context, mesaj, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun metinRengi(cubuk: Snackbar, renk: Int) {
        runCatching {
            cubuk.view
                .findViewById<android.widget.TextView>(
                    com.google.android.material.R.id.snackbar_text
                )
                ?.apply {
                    setTextColor(renk)
                    maxLines = 3
                }
        }
    }

    // ------------------------------------------------------------------

    /**
     * Kök görünümü olmayan yerler için (servis, receiver, arka plan).
     * Toast kullanır — başka seçenek yok.
     */
    fun sistemToast(context: Context?, mesaj: String) {
        context ?: return
        runCatching {
            android.widget.Toast
                .makeText(context, mesaj, android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }
}
