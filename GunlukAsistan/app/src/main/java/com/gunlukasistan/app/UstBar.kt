package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.color.MaterialColors

/**
 * v10.0 — Ortak üst bar (görsel öneri 4).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN — 40 EKRAN, 40 FARKLI ÜST BAR
 * ══════════════════════════════════════════════════════════════════
 * Kodu taradığımda şunu buldum: her Activity kendi üst barını
 * elle yazmış. Yapı aynı (geri düğmesi + başlık) ama detaylar
 * farklı:
 *
 * | Ekran | Simge | Düğme | Yazı | Alt boşluk |
 * |---|---|---|---|---|
 * | `activity_gorunum_ayar` | ← | 42dp | 18sp | 4dp |
 * | `activity_analitik`     | ✕ | 42dp | 19sp | 4dp |
 * | `activity_namaz`        | ← | 40dp | 20sp | 8dp |
 * | `activity_ana_duzen`    | ← | 42dp | 18sp | 0dp |
 *
 * Ekranlar arası geçerken üst kısım **zıplıyor**: başlık 1-2 piksel
 * kayıyor, düğme boyutu değişiyor. Tek tek bakınca kimse fark
 * etmiyor; arka arkaya gezerken huzursuzluk veriyor.
 *
 * Bir de tutarsızlık var: bazı ekranlar `←` (geri), bazıları `✕`
 * (kapat) kullanıyor — aynı davranış için iki farklı simge.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM — KODLA ÜRETİLEN TEK BİLEŞEN
 * ══════════════════════════════════════════════════════════════════
 * ```kotlin
 * UstBar.kur(this, "Başlık") { finish() }
 * ```
 *
 * ── Neden XML `<include>` değil ──
 * `<include>` her layout dosyasını değiştirmeyi gerektirirdi
 * (40 dosya). Ayrıca kodla çizilen ekranlarda (`TakipActivity`,
 * `SistemActivity`) layout yok — include edecek yer yok.
 *
 * Kodla üretmek her iki durumu da kapsıyor ve mevcut layout'lara
 * dokunmadan **kademeli geçişe** izin veriyor: bir ekranı
 * dönüştürmek tek satır.
 *
 * ── Neden MaterialToolbar değil ──
 * `MaterialToolbar` + `setSupportActionBar` zinciri tema
 * gerektiriyor (`Theme.MaterialComponents.*.NoActionBar`).
 * Uygulamanın 6 özel teması var ve bazıları ActionBar'lı.
 * Toolbar'a geçmek altı temayı da değiştirmek demekti.
 */
object UstBar {

    private const val TAG = "UstBar"

    /** Ölçüler `dimens.xml`'deki tasarım ölçeğinden. */
    private const val DUGME_DP = 44
    private const val YUKSEKLIK_DP = 56
    private const val YAZI_SP = 18f

    /**
     * Var olan bir üst barı standartlaştırır.
     *
     * XML'de zaten geri düğmesi ve başlık varsa (çoğu ekran)
     * onları bulup ölçek değerlerine çekiyor. Yeni görünüm
     * oluşturmuyor — mevcut hiyerarşiyi bozmamak için.
     *
     * @param dugme geri/kapat görünümü
     * @param baslik başlık TextView'ı
     */
    fun duzelt(dugme: View?, baslik: TextView?) {
        runCatching {
            val yg = (dugme ?: baslik)?.resources?.displayMetrics?.density ?: return
            dugme?.let {
                val boyut = (DUGME_DP * yg).toInt()
                it.layoutParams = it.layoutParams?.apply {
                    width = boyut
                    height = boyut
                }
                // Erişilebilirlik: dokunma hedefi en az 48dp olmalı.
                // 44dp görünüm + padding ile 48'e tamamlanıyor.
                it.minimumWidth = (48 * yg).toInt()
                it.minimumHeight = (48 * yg).toInt()
            }
            baslik?.apply {
                textSize = YAZI_SP
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }.onFailure { android.util.Log.w(TAG, "Üst bar düzeltilemedi", it) }
    }

    /**
     * Sıfırdan üst bar oluşturur — kodla çizilen ekranlar için.
     *
     * @param context Activity bağlamı
     * @param baslik gösterilecek metin
     * @param kapatSimgesi true ise ✕, false ise ← kullanır
     * @param sagEylem isteğe bağlı sağ üst düğme (metin, iş)
     * @param geri geri düğmesine basılınca çalışacak iş
     */
    fun olustur(
        context: Context,
        baslik: String,
        kapatSimgesi: Boolean = false,
        sagEylem: Pair<String, () -> Unit>? = null,
        geri: () -> Unit
    ): LinearLayout {
        val yg = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kok = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(YUKSEKLIK_DP)
            setPadding(dp(8), dp(8), dp(16), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        kok.addView(TextView(context).apply {
            text = if (kapatSimgesi) "✕" else "←"
            textSize = 20f
            gravity = Gravity.CENTER
            contentDescription = context.getString(
                if (kapatSimgesi) R.string.cd_kapat else R.string.back
            )
            setTextColor(renk(this, com.google.android.material.R.attr.colorOnSurface))
            setBackgroundResource(seciliZemin(context, sinirsiz = true))
            layoutParams = LinearLayout.LayoutParams(dp(DUGME_DP), dp(DUGME_DP))
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            setOnClickListener { Titresim.dokunus(it); geri() }
        })

        kok.addView(TextView(context).apply {
            text = baslik
            textSize = YAZI_SP
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(renk(this, com.google.android.material.R.attr.colorOnSurface))
            runCatching {
                typeface = androidx.core.content.res.ResourcesCompat
                    .getFont(context, R.font.poppins_semibold)
            }
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(4) }
        })

        sagEylem?.let { (etiket, is_) ->
            kok.addView(TextView(context).apply {
                text = etiket
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(renk(this, com.google.android.material.R.attr.colorPrimary))
                setBackgroundResource(seciliZemin(context, sinirsiz = false))
                setPadding(dp(12), dp(8), dp(12), dp(8))
                minimumHeight = dp(48)
                setOnClickListener { Titresim.dokunus(it); is_() }
            })
        }

        return kok
    }

    /**
     * Activity'nin standart üst barını kurar.
     *
     * `activity_gorunum_ayar` kalıbını kullanan 8+ ekran için
     * kısayol: id'leri bulup [duzelt] çağırıyor.
     *
     * @return başlık TextView'ı (sonradan değiştirmek isteyenler için)
     */
    fun kur(activity: Activity, baslikMetni: String, geri: (() -> Unit)? = null): TextView? =
        runCatching {
            val dugme = activity.findViewById<View>(R.id.gaGeri)
            val baslik = activity.findViewById<TextView>(R.id.gaBaslik)
            baslik?.text = baslikMetni
            duzelt(dugme, baslik)
            dugme?.setOnClickListener {
                Titresim.dokunus(it)
                if (geri != null) geri() else activity.finish()
            }
            baslik
        }.getOrNull()

    // ══════════════════════════════════════════════════════════

    private fun renk(gorunum: View, attr: Int): Int = runCatching {
        MaterialColors.getColor(gorunum, attr, 0)
    }.getOrDefault(0)

    /**
     * Dokunma dalgası (ripple) arka planı.
     *
     * Tema özniteliğini çalışma anında çözüyor: `?attr/` XML'de
     * çalışıyor ama kodda `setBackgroundResource` doğrudan
     * öznitelik kabul etmiyor.
     */
    private fun seciliZemin(context: Context, sinirsiz: Boolean): Int = runCatching {
        val attr = if (sinirsiz) android.R.attr.selectableItemBackgroundBorderless
        else android.R.attr.selectableItemBackground
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        tv.resourceId
    }.getOrDefault(0)
}
