package com.gunlukasistan.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * v8.3 — Alternatif uygulama simgesi (öneri 12).
 *
 * ── Nasıl çalışıyor ──
 * Android'de bir uygulamanın simgesi çalışma anında değiştirilemez.
 * Bilinen tek yol: Manifest'te aynı Activity'yi işaret eden birden
 * çok `activity-alias` tanımlamak, her birine farklı `android:icon`
 * vermek ve `PackageManager.setComponentEnabledSetting()` ile
 * yalnız birini etkin bırakmak.
 *
 * ── Bilinen yan etkileri (kullanıcıya söylenmeli) ──
 * 1. Simge değişince launcher kısayolu bir an kaybolabilir; bazı
 *    başlatıcılar uygulamayı ana ekrandan düşürür. Bu Android'in
 *    davranışı, uygulama hatası değil.
 * 2. Değişim anında uygulama kapanabilir — etkin bileşen değişince
 *    sistem süreci yeniden başlatır.
 * Bu yüzden değiştirmeden önce onay soruluyor.
 *
 * ── Neden `DONT_KILL_APP` kullanılıyor ──
 * Süreç öldürülmeden ayar uygulanır; kullanıcı ekranda kalır.
 * Simge birkaç saniye içinde güncellenir.
 */
object Simge {

    private const val TAG = "Simge"
    private const val PREF = "gunluk_asistan_store"
    private const val KEY = "simge_v1"

    /** Manifest'teki alias son eki. Boş olan varsayılan (MainActivity). */
    data class Secenek(
        val kod: String,
        val baslikRes: Int,
        val renk: Int,
        /** Alias tam adı; null ise varsayılan launcher girişi. */
        val alias: String?
    )

    private const val PAKET = "com.gunlukasistan.app"

    val secenekler = listOf(
        // v9.3: varsayılan da artık bir ALIAS. Eskiden null'dı ve
        // MainActivity'nin kendisini hedefliyordu — o da kapatılınca
        // uygulama içi geçişler kırılıyordu.
        Secenek("varsayilan", R.string.sm_varsayilan, 0xFF00897B.toInt(), "$PAKET.SimgeVarsayilan"),
        Secenek("karamel", R.string.sm_karamel, 0xFFB08968.toInt(), "$PAKET.SimgeKaramel"),
        Secenek("mor", R.string.sm_mor, 0xFF7C5CBF.toInt(), "$PAKET.SimgeMor"),
        Secenek("gece", R.string.sm_gece, 0xFF111A2B.toInt(), "$PAKET.SimgeGece"),
        Secenek("yesil", R.string.sm_yesil, 0xFF3E7A4E.toInt(), "$PAKET.SimgeYesil"),
        Secenek("minimal", R.string.sm_minimal, 0xFFFAF4E8.toInt(), "$PAKET.SimgeMinimal")
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun secili(c: Context): String = prefs(c).getString(KEY, "varsayilan") ?: "varsayilan"

    fun seciliIndeks(c: Context): Int =
        secenekler.indexOfFirst { it.kod == secili(c) }.coerceAtLeast(0)

    /**
     * Simgeyi değiştirir.
     *
     * Önce yenisini etkinleştirip sonra eskileri kapatıyoruz. Ters
     * sırada yapılsaydı bir an HİÇBİR launcher girişi olmayan bir
     * durum oluşur ve bazı başlatıcılar uygulamayı listeden silerdi.
     *
     * @return başarılı mı
     */
    fun sec(context: Context, kod: String): Boolean = runCatching {
        val hedef = secenekler.firstOrNull { it.kod == kod } ?: return false
        val pm = context.packageManager

        // ══════════════════════════════════════════════════════════
        // 🔴 v9.3 ÇÖKME DÜZELTMESİ — ActivityNotFoundException
        // ══════════════════════════════════════════════════════════
        //
        // Kullanıcı raporu:
        //   android.content.ActivityNotFoundException: Unable to find
        //   explicit activity class {com.gunlukasistan.app/
        //   com.gunlukasistan.app.MainActivity}
        //     at NamazActivity.planSekmesiniAc
        //
        // ── Sebep (v8.3'ten beri) ──
        // Alternatif simge seçilince bu fonksiyon MainActivity'yi
        // DISABLED yapıyordu. v8.3'te şu yorumu yazmıştım:
        //
        //   "Onu kapatırken dikkat: kapatılırsa uygulama başlatılamaz
        //    hale gelmez çünkü en az bir alias hep açık kalıyor."
        //
        // Bu YANLIŞTI. Alias'lar yalnızca LAUNCHER GİRİŞİ sağlıyor.
        // MainActivity ise uygulamanın ana ekranı ve kod içinde
        // 22 AYRI YERDEN `Intent(context, MainActivity::class.java)`
        // ile açılıyor: bildirimler, widget'lar, arama, namaz ekranı,
        // ders PDF'i, film önerisi...
        //
        // Bileşen devre dışıyken bu intent'lerin HEPSİ
        // ActivityNotFoundException atıyor. Yani alternatif simge
        // seçen kullanıcının uygulaması, bildirime dokunduğunda
        // veya ekranlar arası geçişte çöküyordu.
        //
        // ── Doğru yaklaşım ──
        // MainActivity ASLA devre dışı bırakılmaz. Onun LAUNCHER
        // intent-filter'ı Manifest'te duruyor ama bu bir sorun değil:
        // varsayılan dışında bir simge seçildiğinde iki launcher
        // girişi görünür — bunu önlemek için MainActivity'nin
        // launcher filtresi ayrı bir alias'a taşındı (SimgeVarsayilan).
        //
        // Artık kapatılan/açılan yalnızca ALIAS'lar; MainActivity
        // her zaman etkin kalıyor.

        // 1) Hedef alias'ı aç
        durumAyarla(pm, context, hedef.alias, acik = true)

        // 2) Diğer ALIAS'ları kapat (MainActivity'ye dokunulmuyor)
        secenekler
            .filter { it.kod != kod }
            .forEach { durumAyarla(pm, context, it.alias, acik = false) }

        prefs(context).edit().putString(KEY, kod).apply()
        true
    }.onFailure { android.util.Log.w(TAG, "Simge değiştirilemedi", it) }.getOrDefault(false)

    /**
     * v9.3 — Onarım: MainActivity yanlışlıkla kapatılmışsa geri açar.
     *
     * v8.3-v9.2 arasında alternatif simge seçen kullanıcıların
     * MainActivity'si DISABLED kaldı. Uygulama güncellense bile bu
     * ayar cihazda kalıcı — Manifest'i düzeltmek yetmiyor, çalışma
     * anında geri açmak gerekiyor.
     *
     * `App.onCreate` içinden her açılışta çağrılıyor (ucuz işlem).
     */
    fun onarimYap(context: Context) {
        runCatching {
            val pm = context.packageManager
            val ana = ComponentName(context, MainActivity::class.java)
            val durum = pm.getComponentEnabledSetting(ana)
            if (durum == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                android.util.Log.w(TAG, "MainActivity devre dışıydı — geri açılıyor")
                pm.setComponentEnabledSetting(
                    ana,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            // Hiçbir launcher girişi açık değilse varsayılanı aç —
            // yoksa uygulama çekmecede görünmez olur
            val herhangiAcik = secenekler.any { s ->
                val alias = s.alias ?: return@any false
                runCatching {
                    pm.getComponentEnabledSetting(
                        ComponentName(context.packageName, alias)
                    ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }.getOrDefault(false)
            }
            if (!herhangiAcik) {
                val varsayilan = secenekler.firstOrNull { it.kod == "varsayilan" }?.alias
                if (varsayilan != null) {
                    durumAyarla(pm, context, varsayilan, acik = true)
                    prefs(context).edit().putString(KEY, "varsayilan").apply()
                }
            }
        }.onFailure { android.util.Log.w(TAG, "Onarım başarısız", it) }
    }

    private fun durumAyarla(
        pm: PackageManager,
        context: Context,
        alias: String?,
        acik: Boolean
    ) {
        // 🔴 v9.3: alias null ise İŞLEM YAPMA.
        //
        // Eskiden burada MainActivity'nin kendisi hedefleniyordu ve
        // kapatılınca uygulama içi tüm geçişler çöküyordu. Artık
        // varsayılan simge de bir alias (SimgeVarsayilan); MainActivity
        // hiçbir koşulda devre dışı bırakılmıyor.
        val bilesen = if (alias == null) {
            android.util.Log.w(TAG, "MainActivity devre dışı bırakılamaz — atlandı")
            return
        } else {
            ComponentName(context.packageName, alias)
        }
        val durum = if (acik) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(bilesen, durum, PackageManager.DONT_KILL_APP)
        }.onFailure { android.util.Log.w(TAG, "Bileşen durumu: $alias", it) }
    }
}
