package com.gunlukasistan.app

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LayoutAnimationController
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * v8.2 — Merkezî animasyon yardımcısı (öneri 1, 3, 5).
 *
 * ── Ölçüm ──
 * v8.1'de 166 dosyadan yalnız 1'i (`FullscreenTimerActivity`) animasyon
 * kullanıyordu. Sekmeler çat diye değişiyor, sayılar anında yazılıyor,
 * listeler bir anda beliriyordu. Uygulama zengin ama "ucuz" hissettiriyordu.
 *
 * ── Neden tek bir nesne ──
 * Animasyonu 17 fragment'a tek tek yazmak, her birinde farklı süre ve
 * eğri kullanmak demekti. Buradan geçince hepsi aynı ritimde hareket
 * ediyor ve [GorunumAyar.animasyonAcik] tek noktadan kapatılabiliyor.
 *
 * ── Süre seçimi ──
 * 220 ms geçiş, 260 ms liste öğesi, 600 ms sayaç. Material yönergesi
 * ekran içi geçişler için 200-300 ms öneriyor; altına inince fark
 * edilmiyor, üstüne çıkınca uygulama yavaş hissettiriyor.
 */
object Canlandir {

    private const val TAG = "Canlandir"

    const val SURE_GECIS = 220L
    const val SURE_KISA = 150L
    const val SURE_SAYAC = 620L

    // ------------------------------------------------------------------
    // Sayı sayacı (öneri 3)
    // ------------------------------------------------------------------

    /**
     * Bir tam sayıyı [baslangic]'tan [hedef]'e animasyonla yazar.
     *
     * ```
     * Canlandir.sayi(streakValue, 0, 27) { "$it gün" }
     * ```
     *
     * @param bicim değeri metne çeviren işlev (birim, ayraç vb. ekler)
     */
    fun sayi(
        gorunum: android.widget.TextView?,
        baslangic: Int,
        hedef: Int,
        sure: Long = SURE_SAYAC,
        bicim: (Int) -> String = { it.toString() }
    ) {
        gorunum ?: return
        val ctx = gorunum.context
        if (!GorunumAyar.sayiAnimasyonu(ctx) || baslangic == hedef) {
            gorunum.text = bicim(hedef)
            return
        }
        runCatching {
            // Önceki animasyon sürüyorsa iptal et (hızlı tazelemede çakışma olur)
            (gorunum.getTag(R.id.ga_tag_animator) as? ValueAnimator)?.cancel()
            val anim = ValueAnimator.ofInt(baslangic, hedef).apply {
                duration = sure
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener { gorunum.text = bicim(it.animatedValue as Int) }
            }
            gorunum.setTag(R.id.ga_tag_animator, anim)
            anim.start()
        }.onFailure {
            gorunum.text = bicim(hedef)
            android.util.Log.w(TAG, "sayi", it)
        }
    }

    /** 0'dan hedefe. En sık kullanılan biçim. */
    fun sayi(gorunum: android.widget.TextView?, hedef: Int, bicim: (Int) -> String = { it.toString() }) =
        sayi(gorunum, 0, hedef, SURE_SAYAC, bicim)

    /**
     * Halka göstergesini (StatRingView) 0'dan hedefe doldurur.
     */
    fun halka(halka: StatRingView?, hedef: Int, sure: Long = SURE_SAYAC) {
        halka ?: return
        if (!GorunumAyar.sayiAnimasyonu(halka.context)) {
            halka.progress = hedef
            return
        }
        runCatching {
            (halka.getTag(R.id.ga_tag_animator) as? ValueAnimator)?.cancel()
            val anim = ValueAnimator.ofInt(0, hedef.coerceIn(0, 100)).apply {
                duration = sure
                interpolator = DecelerateInterpolator(1.8f)
                addUpdateListener { halka.progress = it.animatedValue as Int }
            }
            halka.setTag(R.id.ga_tag_animator, anim)
            anim.start()
        }.onFailure {
            halka.progress = hedef
            android.util.Log.w(TAG, "halka", it)
        }
    }

    /** İlerleme çubuğunu animasyonla doldurur. */
    fun cubuk(cubuk: android.widget.ProgressBar?, hedef: Int, sure: Long = SURE_SAYAC) {
        cubuk ?: return
        if (!GorunumAyar.sayiAnimasyonu(cubuk.context)) {
            cubuk.progress = hedef
            return
        }
        runCatching {
            (cubuk.getTag(R.id.ga_tag_animator) as? ValueAnimator)?.cancel()
            val anim = ValueAnimator.ofInt(0, hedef).apply {
                duration = sure
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener { cubuk.progress = it.animatedValue as Int }
            }
            cubuk.setTag(R.id.ga_tag_animator, anim)
            anim.start()
        }.onFailure {
            cubuk.progress = hedef
            android.util.Log.w(TAG, "cubuk", it)
        }
    }

    // ------------------------------------------------------------------
    // Liste girişi (öneri 5)
    // ------------------------------------------------------------------

    /**
     * RecyclerView'a sıralı giriş animasyonu bağlar.
     *
     * Adapter verisi her değiştiğinde tekrar oynatmak için
     * [tekrarOynat] çağrılır; burada yalnız kurulum yapılır.
     */
    fun liste(recycler: RecyclerView?) {
        recycler ?: return
        if (!GorunumAyar.listeAnimasyonu(recycler.context)) {
            recycler.layoutAnimation = null
            return
        }
        runCatching {
            val kontrol: LayoutAnimationController =
                AnimationUtils.loadLayoutAnimation(recycler.context, R.anim.ga_liste)
            recycler.layoutAnimation = kontrol
        }.onFailure { android.util.Log.w(TAG, "liste", it) }
    }

    /**
     * Veri değiştikten sonra giriş animasyonunu yeniden oynatır.
     *
     * `notifyDataSetChanged()` tek başına layoutAnimation'ı tetiklemez;
     * `scheduleLayoutAnimation()` gerekir.
     */
    fun tekrarOynat(recycler: RecyclerView?) {
        recycler ?: return
        if (!GorunumAyar.listeAnimasyonu(recycler.context)) return
        runCatching {
            if (recycler.layoutAnimation == null) liste(recycler)
            recycler.scheduleLayoutAnimation()
        }.onFailure { android.util.Log.w(TAG, "tekrarOynat", it) }
    }

    // ------------------------------------------------------------------
    // Tekil görünüm efektleri
    // ------------------------------------------------------------------

    /** Görünümü solarak + hafif büyüyerek gösterir. */
    fun bel(gorunum: View?, gecikme: Long = 0L) {
        gorunum ?: return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) {
            gorunum.alpha = 1f
            gorunum.visibility = View.VISIBLE
            return
        }
        runCatching {
            gorunum.alpha = 0f
            gorunum.scaleX = 0.96f
            gorunum.scaleY = 0.96f
            gorunum.visibility = View.VISIBLE
            gorunum.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(gecikme)
                .setDuration(SURE_GECIS)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }.onFailure {
            gorunum.alpha = 1f
            gorunum.visibility = View.VISIBLE
        }
    }

    /** Yatay sarsıntı — yanlış cevap, geçersiz giriş. */
    fun sarsit(gorunum: View?) {
        gorunum ?: return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) return
        runCatching {
            val anim = AnimationUtils.loadAnimation(gorunum.context, R.anim.ga_sarsinti)
            // fromXDelta XML'de 0; gerçek genliği burada veriyoruz
            gorunum.startAnimation(anim)
            gorunum.animate()
                .translationX(0f)
                .setDuration(1)
                .start()
        }.onFailure { android.util.Log.w(TAG, "sarsit", it) }
    }

    /**
     * Kod tarafında sarsıntı — XML cycleInterpolator bazı cihazlarda
     * genliği yutuyor. Bu sürüm her yerde çalışır.
     */
    fun sarsitKesin(gorunum: View?, genlik: Float = 14f) {
        gorunum ?: return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) return
        runCatching {
            val yg = gorunum.resources.displayMetrics.density * genlik
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 380
                addUpdateListener {
                    val t = it.animatedValue as Float
                    // Sönümlenen sinüs: 4 salınım, giderek zayıflıyor
                    gorunum.translationX =
                        (yg * Math.sin(t * Math.PI * 4).toFloat() * (1f - t))
                }
            }
            anim.start()
        }.onFailure { android.util.Log.w(TAG, "sarsitKesin", it) }
    }

    /** Nabız — dikkat çekmek için tek atış. */
    fun nabiz(gorunum: View?, olcek: Float = 1.09f) {
        gorunum ?: return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) return
        runCatching {
            gorunum.animate()
                .scaleX(olcek).scaleY(olcek)
                .setDuration(170)
                .withEndAction {
                    gorunum.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(230)
                        .setInterpolator(OvershootInterpolator(2.2f))
                        .start()
                }
                .start()
        }.onFailure { android.util.Log.w(TAG, "nabiz", it) }
    }

    /** Zıplama — başarı anı. */
    fun zipla(gorunum: View?) {
        gorunum ?: return
        if (!GorunumAyar.animasyonAcik(gorunum.context)) return
        runCatching {
            val yuk = gorunum.resources.displayMetrics.density * 9f
            gorunum.animate()
                .translationY(-yuk)
                .setDuration(160)
                .withEndAction {
                    gorunum.animate()
                        .translationY(0f)
                        .setDuration(280)
                        .setInterpolator(OvershootInterpolator(3f))
                        .start()
                }
                .start()
        }.onFailure { android.util.Log.w(TAG, "zipla", it) }
    }

    // ------------------------------------------------------------------
    // Activity geçişleri (öneri 1)
    // ------------------------------------------------------------------

    /** Activity açılırken: yeni ekran sağdan girer. */
    @Suppress("DEPRECATION")
    fun activityGirisi(activity: android.app.Activity?) {
        activity ?: return
        if (!GorunumAyar.animasyonAcik(activity)) return
        runCatching {
            activity.overridePendingTransition(R.anim.ga_gir_sag, R.anim.ga_cik_sol)
        }.onFailure { android.util.Log.w(TAG, "activityGirisi", it) }
    }

    /** Activity kapanırken: ters yön. */
    @Suppress("DEPRECATION")
    fun activityCikisi(activity: android.app.Activity?) {
        activity ?: return
        if (!GorunumAyar.animasyonAcik(activity)) return
        runCatching {
            activity.overridePendingTransition(R.anim.ga_gir_sol, R.anim.ga_cik_sag)
        }.onFailure { android.util.Log.w(TAG, "activityCikisi", it) }
    }

    /**
     * Fragment geçişine animasyon ekler.
     *
     * @param ileri true ise sağdan girer, false ise soldan (geri yönü)
     */
    fun fragmentGecisi(
        tx: androidx.fragment.app.FragmentTransaction,
        context: Context,
        ileri: Boolean = true
    ) {
        if (!GorunumAyar.animasyonAcik(context)) return
        runCatching {
            if (ileri) {
                tx.setCustomAnimations(R.anim.ga_bel, R.anim.ga_soluk)
            } else {
                tx.setCustomAnimations(R.anim.ga_gir_sol, R.anim.ga_cik_sag)
            }
        }.onFailure { android.util.Log.w(TAG, "fragmentGecisi", it) }
    }
}
