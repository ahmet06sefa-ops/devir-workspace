package com.gunlukasistan.app

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * v8.6 — Yatay ekran ve tablet düzeni (öneri 29).
 *
 * ── Sorun ──
 * Telefonu yatay çevirince kartlar ekranı boydan boya kaplıyordu:
 * 850px genişlikte tek satır metin okumak zor. Tablette daha kötü —
 * 10 inç ekranda uygulama "büyütülmüş telefon" gibi duruyordu.
 *
 * ── Neden layout-land/ dosyaları yazılmadı ──
 * 71 layout'un yatay kopyasını tutmak bakım kâbusu olurdu: her
 * değişikliği iki yerde yapmak gerekirdi ve biri unutulurdu (bu proje
 * geçmişinde benzer bir hata v7.62'de dört sürüm boyunca fark
 * edilmemişti).
 *
 * Bunun yerine `values-land/` ve `values-sw600dp/` içinde ölçü
 * değerleri tanımlandı; bu sınıf da içerik genişliğini çalışma anında
 * sınırlıyor. Tek kod yolu, üç ekran boyutu.
 *
 * ── Okunabilirlik sınırı ──
 * Tipografi kuralı: satır uzunluğu 45-75 karakteri geçmemeli. 720dp
 * bu aralığa denk geliyor.
 */
object Duzen {

    private const val TAG = "Duzen"

    /** Şu an yatay mı? */
    fun yatayMi(c: Context): Boolean =
        c.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** Ekran en az 600dp genişlikte mi (tablet)? */
    fun tabletMi(c: Context): Boolean =
        c.resources.configuration.smallestScreenWidthDp >= 600

    /**
     * Bir kaydırılabilir içeriğin genişliğini sınırlar ve ortalar.
     *
     * Geniş ekranda içerik ortada bir sütun halinde kalıyor, iki yanda
     * boşluk oluşuyor. Telefonda (`ga_en_fazla_genislik = 0`) hiçbir
     * şey yapılmıyor.
     *
     * @param icerik ScrollView'ün doğrudan çocuğu (genellikle LinearLayout)
     */
    fun genisligiSinirla(icerik: View?) {
        icerik ?: return
        runCatching {
            val ctx = icerik.context
            val enFazla = ctx.resources.getDimensionPixelSize(R.dimen.ga_en_fazla_genislik)
            if (enFazla <= 0) return

            // 🔴 KRİTİK KONTROL (v8.7'de eklendi):
            //
            // `values-land/` 640dp sınır veriyor. Ama 6,7" bir telefon
            // yatayken zaten ~640-740dp geniş oluyor. Sınır ekrandan
            // BÜYÜK ya da ona çok yakınsa `lp.width = enFazla` içeriği
            // taşırır ve yatay kaydırma çubuğu oluşur — düzeltmek
            // isterken bozardık.
            //
            // Sadece ekran sınırdan belirgin şekilde genişse müdahale
            // ediyoruz (en az %8 fark).
            val ekranGenislik = ctx.resources.displayMetrics.widthPixels
            if (ekranGenislik < enFazla * 1.08f) return

            val lp = icerik.layoutParams ?: return
            when (lp) {
                is FrameLayout.LayoutParams -> {
                    lp.width = enFazla
                    lp.gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                is ViewGroup.MarginLayoutParams -> {
                    // Ebeveyn FrameLayout değilse yan boşlukla ortala
                    val pay = ((ekranGenislik - enFazla) / 2).coerceAtLeast(0)
                    lp.leftMargin = pay
                    lp.rightMargin = pay
                }
                else -> return
            }
            icerik.layoutParams = lp
        }.onFailure { android.util.Log.w(TAG, "genisligiSinirla", it) }
    }

    /**
     * Fragment'ın kök görünümüne uygular.
     *
     * `ScrollView > LinearLayout` yapısındaki ekranlarda içeriği bulur.
     */
    fun uygula(kok: View?) {
        kok ?: return
        runCatching {
            val ctx = kok.context
            if (!yatayMi(ctx) && !tabletMi(ctx)) return

            // 🔴 v8.7: iki kez uygulanmayı önle.
            //
            // `uygula` hem onViewCreated'dan hem (ekran döndürülünce)
            // yeniden oluşturmadan çağrılabiliyor. İkinci çağrıda
            // margin'ler ÜST ÜSTE binerdi ve içerik giderek daralırdı.
            if (kok.getTag(R.id.ga_tag_duzen) == true) return
            kok.setTag(R.id.ga_tag_duzen, true)

            // Kök bir ScrollView ise ilk çocuğunu sınırla
            val hedef = when (kok) {
                is android.widget.ScrollView -> kok.getChildAt(0)
                is androidx.core.widget.NestedScrollView -> kok.getChildAt(0)
                else -> kok
            }
            genisligiSinirla(hedef)

            // Yoğunluk tercihini de uygula (öneri 27)
            yogunluguUygula(hedef)
        }.onFailure { android.util.Log.w(TAG, "uygula", it) }
    }

    /**
     * v8.6 · Öneri 27 — Kart aralığı tercihi.
     *
     * Kap görünümün dikey iç boşluğunu çarpanla ölçekliyor. Tek tek
     * kartların margin'ine dokunmak 71 layout demekti; kabın padding'i
     * aynı ferahlık/sıkılık hissini veriyor.
     */
    fun yogunluguUygula(kap: View?) {
        kap ?: return
        runCatching {
            val carpan = GorunumAyar.yogunlukCarpani(kap.context)
            if (carpan == 1.0f) return
            val tag = kap.getTag(R.id.ga_tag_onceki_deger) as? Int
            val temelUst = tag ?: kap.paddingTop
            if (tag == null) kap.setTag(R.id.ga_tag_onceki_deger, temelUst)
            kap.setPadding(
                kap.paddingLeft,
                (temelUst * carpan).toInt(),
                kap.paddingRight,
                kap.paddingBottom
            )
        }.onFailure { android.util.Log.w(TAG, "yogunluguUygula", it) }
    }
}
