package com.gunlukasistan.app

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews

/**
 * v7.66 — Widget'ları uygulamanın seçili temasıyla boyar.
 *
 * ── Kullanıcının isteği ──
 * "Widgetları uygulama tema renginde yap hepsini"
 *
 * ── Sorun ──
 * Widget renkleri `res/values/widget_colors.xml` içinde **sabit** yazılıydı
 * (krem/karamel paleti). Uygulamada Okyanus, Orman, Ember gibi bir tema
 * ya da 12 vurgu renginden biri seçilse bile ana ekrandaki widget'lar
 * eski krem renginde kalıyordu. Yalnızca gece/gündüz ayrımı çalışıyordu.
 *
 * ── Çözüm ──
 * XML kaynakları çalışma anında değiştirilemez, ama `RemoteViews` üzerinde
 * `setTextColor` / `setColorFilter` / `setInt(..., "setBackgroundColor", ...)`
 * çağrılabilir. Bu sınıf seçili temadan bir [Palet] üretir ve her widget
 * çizildikten sonra renkleri **üstüne yazar**.
 *
 * Arka planlar `GradientDrawable` şeklinde tanımlı olduğu için doğrudan
 * renk atanamaz; bunun yerine kök görünüme tema zeminini `setInt` ile
 * uyguluyoruz ve metin/vurgu renklerini tek tek boyuyoruz.
 *
 * ── Palet nereden geliyor ──
 * · Vurgu rengi: kullanıcı bir **vurgu** seçtiyse ondan, yoksa temanın
 *   kendi halka renginden ([ThemeManager.Spec.ringColor]).
 * · Zemin: temanın kart rengi ([ThemeManager.Spec.cardColor]).
 * · Metin: zeminin parlaklığına göre koyu ya da açık seçilir — böylece
 *   hangi tema seçilirse seçilsin okunabilirlik korunur.
 */
object WidgetTema {

    private const val TAG = "WidgetTema"

    /** Bir widget'ı boyamak için gereken tüm renkler. */
    data class Palet(
        val zemin: Int,
        val zeminAlt: Int,
        val metin: Int,
        val metinSoluk: Int,
        val vurgu: Int,
        val vurguSoluk: Int,
        val yesil: Int,
        val koyuMu: Boolean
    )

    // ═══════════════════════════════════════════════════════════════
    // v7.68 — KULLANICI TERCİHLERİ
    // ═══════════════════════════════════════════════════════════════

    private const val PREF = "widget_tema_v1"

    /** Zemin modu. */
    const val MOD_KOYU = 0
    const val MOD_ACIK = 1
    const val MOD_SISTEM = 2
    const val MOD_TEMA = 3

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Zemin modu — varsayılan koyu (v7.67'de kullanıcı böyle istemişti). */
    fun mod(context: Context): Int =
        prefs(context).getInt("mod", MOD_KOYU).coerceIn(0, 3)

    fun setMod(context: Context, m: Int) {
        prefs(context).edit().putInt("mod", m.coerceIn(0, 3)).apply()
    }

    /** Saydamlık: 0 = opak · 1 = hafif · 2 = orta · 3 = çok. */
    fun saydamlik(context: Context): Int =
        prefs(context).getInt("saydam", 0).coerceIn(0, 3)

    fun setSaydamlik(context: Context, s: Int) {
        prefs(context).edit().putInt("saydam", s.coerceIn(0, 3)).apply()
    }

    // ────────────────────────────────────────────────────────────
    // v10.20 · SINIRSIZ KONTROL — serbest saydamlık ve köşe
    // ────────────────────────────────────────────────────────────

    private const val K_SAYDAM_PCT = "saydam_pct"
    private const val K_KOSE_DP = "kose_dp"

    /**
     * v10.20: zemin saydamlığı serbest yüzde (0 = opak … 100 = görünmez).
     * Eski 4 kademeli anahtar ilk okumada yüzdeye taşınır. Tüm widget'lar
     * artık bitmap zeminli olduğu için her değer GERÇEKTEN uygulanır —
     * %57 yazan %57 görür (kademe yuvarlaması yok).
     */
    fun saydamlikPct(context: Context): Int {
        val sp = prefs(context)
        if (sp.contains(K_SAYDAM_PCT)) return sp.getInt(K_SAYDAM_PCT, 0).coerceIn(0, 100)
        return listOf(0, 12, 23, 34)[saydamlik(context)]
    }

    fun setSaydamlikPct(context: Context, pct: Int) {
        prefs(context).edit().putInt(K_SAYDAM_PCT, pct.coerceIn(0, 100)).apply()
    }

    /**
     * v10.20: köşe yarıçapı serbest dp. Eski kademe (6/26/38/48) korunur.
     * Teknik tavan 2000 dp (negatif Canvas'ı çökertir — tek kelepçe).
     */
    fun koseDpF(context: Context): Float {
        val sp = prefs(context)
        if (sp.contains(K_KOSE_DP)) return sp.getInt(K_KOSE_DP, 26)
            .coerceIn(0, 2000).toFloat()
        return WidgetZemin.koseDp(kose(context))
    }

    fun setKoseDpF(context: Context, dp: Int) {
        prefs(context).edit().putInt(K_KOSE_DP, dp.coerceIn(0, 2000)).apply()
    }

    /**
     * v10.13 · B11: köşe yuvarlaklığı kademesi (0-3).
     *
     * Kapsam notu (dürüstlük): hazır shape kaynaklarına (`w_card_*`)
     * bağlı eski widget'lara köşe UYGULANAMAZ — RemoteViews köşeyi
     * değiştiremez. Yeni nesil aile (Kokpit · Ay · Uyku · Odak)
     * zemini bitmap olarak ürettiği için tam destek oradadır;
     * saydamlık her iki ailede de çalışır. Ayar ekranında da yazar.
     */
    fun kose(context: Context): Int =
        prefs(context).getInt("kose", 1).coerceIn(0, 3)

    fun setKose(context: Context, k: Int) {
        prefs(context).edit().putInt("kose", k.coerceIn(0, 3)).apply()
    }

    /** v10.13 · B11: widget yazı ölçeği kademesi (0 = küçük … 2 = büyük). */
    fun yaziKademe(context: Context): Int =
        prefs(context).getInt("yazi", 1).coerceIn(0, 2)

    @Deprecated("v10.16: serbest yüzde kaydırıcısı — WidgetAtolye.setYaziYuzde")
    fun setYaziKademe(context: Context, y: Int) {
        // Geriye dönük köprü: eski kademe çağrıları yüzdeye çevrilir.
        WidgetAtolye.setYaziYuzde(context, WidgetAtolye.kademeToYuzde(y.coerceIn(0, 2)))
    }

    /** Widget'lara özel vurgu rengi (-1 = uygulama temasından al). */
    fun ozelVurgu(context: Context): Int = prefs(context).getInt("vurgu", -1)

    fun setOzelVurgu(context: Context, idx: Int) {
        prefs(context).edit().putInt("vurgu", idx).apply()
    }

    /** Veri değişince widget'lar anında yenilensin mi? */
    fun anlikSenkron(context: Context): Boolean =
        prefs(context).getBoolean("anlik", true)

    fun setAnlikSenkron(context: Context, a: Boolean) {
        prefs(context).edit().putBoolean("anlik", a).apply()
    }

    /** Seçili moda göre zemin koyu mu olmalı? */
    private fun koyuMuOlmali(context: Context, spec: ThemeManager.Spec): Boolean =
        when (mod(context)) {
            MOD_ACIK -> false
            MOD_SISTEM -> sistemKoyuMu(context)
            MOD_TEMA -> spec.dark || karanlikMi(spec.cardColor)
            else -> true   // MOD_KOYU
        }

    private fun sistemKoyuMu(context: Context): Boolean = try {
        val mask = context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        mask == android.content.res.Configuration.UI_MODE_NIGHT_YES
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Sistem modu okunamadı", e)
        true
    }

    /** Seçili tema + vurgudan palet üretir. */
    fun palet(context: Context): Palet {
        // v10.13 · B11: yazı ölçeği tercihi her çizim yolunda senkronlansın
        // (WidgetCommon.yaziBoyutu framework'süz çağrıldığı için SharedPreferences
        // yerine bu volatile alan üzerinden okur).
        // v10.16: 3 kademeli çip yerine %75–%150 serbest kaydırıcı okunuyor
        // (eski kademe, ilk okumada WidgetAtolye tarafından yüzdeye taşınır).
        WidgetCommon.yaziOlcek = WidgetAtolye.yaziCarpan(context)
        return try {
            val spec = ThemeManager.specs[ThemeManager.selected(context)]
            // v7.68: widget'a özel vurgu seçildiyse onu kullan
            val ozel = ozelVurgu(context)
            val vurguIdx = if (ozel in ThemeManager.accents.indices) ozel
            else ThemeManager.accentIndex(context)
            val vurgu = if (vurguIdx in ThemeManager.accents.indices) {
                ThemeManager.accents[vurguIdx].swatch
            } else {
                spec.ringColor
            }

            // v7.67 — Widget'lar HER ZAMAN koyu.
            //
            // Kullanicinin istegi: "Widgetlarda karanlik tema kullan hepsinde"
            //
            // Onceden zemin dogrudan temanin kart renginden geliyordu; Krem,
            // Okyanus gibi aydinlik temalarda widget da aydinlik oluyordu.
            // Artik aydinlik bir tema secilse bile zemin koyulastiriliyor;
            // temanin kimligi VURGU renginde yasamaya devam ediyor.
            // v7.68: zemin artık kullanıcının seçtiği moda göre
            val koyu = koyuMuOlmali(context, spec)
            val zemin = if (koyu) koyuZemin(spec.cardColor, spec.dark)
            else acikZemin(spec.cardColor, spec.dark)

            // v10.17: palet, dönmeden önce Widget Ayar Envanteri'nin
            // merkezi işlemesinden geçer (metin modu · kontrast ·
            // gece karartması · vurgu canlılığı · tamamlanan rengi) —
            // paleti kullanan 20 widget tek seferde kazanır.
            WidgetSecim.uygula(
                context,
                Palet(
                    zemin = zemin,
                    zeminAlt = karistir(zemin, if (koyu) Color.WHITE else Color.BLACK, 0.07f),
                    metin = if (koyu) 0xFFF3EDE6.toInt() else 0xFF2E2A25.toInt(),
                    metinSoluk = if (koyu) 0xFFA79C90.toInt() else 0xFF7D766C.toInt(),
                    vurgu = okunurVurgu(vurgu, zemin, koyu),
                    vurguSoluk = karistir(zemin, vurgu, if (koyu) 0.24f else 0.18f),
                    yesil = if (koyu) 0xFFA3BE96.toInt() else 0xFF5E7A52.toInt(),
                    koyuMu = koyu
                )
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Palet üretilemedi", e)
            WidgetSecim.uygula(context, varsayilanPalet())
        }
    }

    /**
     * v7.67: Temanin zeminini koyu karsiligina cevirir.
     *
     * Tema zaten koyuysa (Ember, Aurora, Zincir…) oldugu gibi kullanilir.
     * Aydinlik temalarda (Krem, Okyanus, Orman…) rengin TONU korunup
     * parlakligi dusurulur — boylece widget koyu olur ama secilen temanin
     * karakterini tasir; hepsi ayni siyah blok gibi gorunmez.
     */
    private fun koyuZemin(kartRengi: Int, zatenKoyu: Boolean): Int {
        if (zatenKoyu || karanlikMi(kartRengi)) return kartRengi
        // Aydinlik kart rengini koyu bir tabana karistir — ton korunur
        return karistir(0xFF16120F.toInt(), kartRengi, 0.16f)
    }

    /** v7.67: yedek palet de koyu. */
    /**
     * v7.68: Temanın zeminini aydınlık karşılığına çevirir.
     * Koyu temalarda (Ember, Aurora…) tonu koruyup parlaklığı yükseltir.
     */
    private fun acikZemin(kartRengi: Int, zatenKoyu: Boolean): Int {
        if (!zatenKoyu && !karanlikMi(kartRengi)) return kartRengi
        return karistir(0xFFFBF7F2.toInt(), kartRengi, 0.14f)
    }

    private fun varsayilanPalet() = Palet(
        zemin = 0xFF1C1814.toInt(),
        zeminAlt = 0xFF241E18.toInt(),
        metin = 0xFFF5EDE3.toInt(),
        metinSoluk = 0xFFA99C8C.toInt(),
        vurgu = 0xFFE0B183.toInt(),
        vurguSoluk = 0xFF3A2E23.toInt(),
        yesil = 0xFFA3BE96.toInt(),
        koyuMu = true
    )

    // ═══════════════════════════════════════════════════════════════
    // RENK YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    /** Rengin algılanan parlaklığı (0 = siyah, 1 = beyaz). */
    private fun parlaklik(renk: Int): Float {
        val r = Color.red(renk) / 255f
        val g = Color.green(renk) / 255f
        val b = Color.blue(renk) / 255f
        // İnsan gözü yeşile daha duyarlı — ağırlıklı ortalama
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun karanlikMi(renk: Int): Boolean = parlaklik(renk) < 0.5f

    /** İki rengi [oran] kadar karıştırır. */
    private fun karistir(a: Int, b: Int, oran: Float): Int {
        val t = oran.coerceIn(0f, 1f)
        return Color.rgb(
            ((Color.red(a) * (1 - t)) + (Color.red(b) * t)).toInt().coerceIn(0, 255),
            ((Color.green(a) * (1 - t)) + (Color.green(b) * t)).toInt().coerceIn(0, 255),
            ((Color.blue(a) * (1 - t)) + (Color.blue(b) * t)).toInt().coerceIn(0, 255)
        )
    }

    /**
     * Vurgu rengini zemine karşı okunur hâle getirir.
     *
     * Koyu zeminde soluk vurgu, açık zeminde parlak vurgu okunmaz.
     * Gerekirse rengi açar veya koyulaştırır.
     */
    private fun okunurVurgu(vurgu: Int, zemin: Int, koyu: Boolean): Int {
        val fark = kotlin.math.abs(parlaklik(vurgu) - parlaklik(zemin))
        if (fark >= 0.28f) return vurgu
        return if (koyu) karistir(vurgu, Color.WHITE, 0.42f)
        else karistir(vurgu, Color.BLACK, 0.32f)
    }

    /** Şeffaflık ekler (0-255). */
    private fun saydam(renk: Int, alfa: Int): Int =
        Color.argb(alfa.coerceIn(0, 255), Color.red(renk), Color.green(renk), Color.blue(renk))

    // ═══════════════════════════════════════════════════════════════
    // BOYAMA
    // ═══════════════════════════════════════════════════════════════

    /** Bir görünümün metin rengini ayarlar. */
    fun metin(views: RemoteViews, id: Int, renk: Int) {
        try {
            views.setTextColor(id, renk)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Metin boyanamadı", e)
        }
    }

    /** Bir görünümün arka plan rengini ayarlar. */
    fun zemin(views: RemoteViews, id: Int, renk: Int) {
        try {
            views.setInt(id, "setBackgroundColor", renk)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Zemin boyanamadı", e)
        }
    }

    /**
     * Widget kökünü tema zeminiyle boyar.
     *
     * Not: `w_card` yuvarlak köşeli bir shape. `setBackgroundColor` çağrısı
     * onu düz renge çevirir ve köşeler kaybolur. Bunun yerine köşeleri
     * korumak için `setColorFilter` benzeri bir yol yok — bu yüzden kökü
     * boyamıyoruz; XML'deki `w_card` zaten gece/gündüz uyumlu.
     * Bunun yerine iç öğeleri boyuyoruz, zemin nötr kalıyor.
     */
    fun kokZemin(views: RemoteViews, id: Int, p: Palet) {
        // v7.68: köşe yuvarlaklığı için hazır 9-patch yerine, seçilen moda
        // uygun shape kaynağı atanıyor; ardından renk tonu üstüne yazılıyor.
        //
        // setBackgroundColor köşeleri kareleştirdiği için doğrudan
        // kullanılamıyor. Bunun yerine setBackgroundResource ile koyu/açık
        // varyantlardan biri seçiliyor, saydamlık ise setInt("setAlpha")
        // ile ayarlanıyor.
        try {
            views.setInt(
                id, "setBackgroundResource",
                if (p.koyuMu) R.drawable.w_card_koyu else R.drawable.w_card_acik
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kök zemin ayarlanamadı", e)
        }
    }

    /**
     * Ortak öğeleri tek çağrıda boyar.
     *
     * @param metinler normal metin renginde olacak görünümler
     * @param soluklar ikincil metinler
     * @param vurgular vurgu renginde olacaklar
     * @param cipler zemin-alt renginde arka planı olanlar
     */
    fun uygula(
        views: RemoteViews,
        p: Palet,
        metinler: IntArray = intArrayOf(),
        soluklar: IntArray = intArrayOf(),
        vurgular: IntArray = intArrayOf(),
        cipler: IntArray = intArrayOf(),
        yesiller: IntArray = intArrayOf()
    ) {
        metinler.forEach { metin(views, it, p.metin) }
        soluklar.forEach { metin(views, it, p.metinSoluk) }
        vurgular.forEach { metin(views, it, p.vurgu) }
        yesiller.forEach { metin(views, it, p.yesil) }
        cipler.forEach { zemin(views, it, p.zeminAlt) }
    }

    /** Vurgu zeminli düğme (ör. "+ Görev"). */
    fun vurguDugme(views: RemoteViews, zeminId: Int, metinId: Int, p: Palet) {
        zemin(views, zeminId, p.vurguSoluk)
        metin(views, metinId, p.vurgu)
    }

    /** Hafif zeminli düğme. */
    fun notrDugme(views: RemoteViews, zeminId: Int, p: Palet) {
        zemin(views, zeminId, saydam(p.metin, if (p.koyuMu) 28 else 18))
    }
}
