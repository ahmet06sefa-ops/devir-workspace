package com.gunlukasistan.app

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v8.9 — Yaşam döngüsüne bağlı arka plan işleri (öneri 16).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖLÇÜLEN SORUN
 * ══════════════════════════════════════════════════════════════════
 * Kod tabanında **87 × `runOnUiThread`** ve 6 dosyada elle
 * `Executors` kullanımı vardı. **0 coroutine.**
 *
 * Tipik desen şuydu:
 *
 *     worker.execute {
 *         val sonuc = AiClient.chat(context, ...)      // 3-10 saniye
 *         activity?.runOnUiThread {
 *             if (!isAdded) return@runOnUiThread       // elle koruma
 *             gorunumuGuncelle(sonuc)
 *         }
 *     }
 *
 * Üç ayrı sorun:
 *
 * 1. **Sızıntı riski.** `worker` bir `ExecutorService`; fragment yok
 *    edilse bile iş devam ediyor. Uzun bir AI isteği sırasında
 *    kullanıcı ekranı kapatırsa iş boşuna sürüyor ve `context`
 *    referansı tutuluyor.
 *
 * 2. **Elle koruma unutuluyor.** `if (!isAdded) return` yazmayı
 *    unutan her yer, fragment yok edildikten sonra
 *    `requireContext()` çağırıp **çöküyor**. Kod tabanında bu
 *    korumanın olmadığı yerler var.
 *
 * 3. **İptal yok.** Kullanıcı ekranı değiştirse bile ağ isteği
 *    tamamlanana kadar sürüyor; pil ve veri boşa gidiyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM
 * ══════════════════════════════════════════════════════════════════
 * `viewLifecycleOwner.lifecycleScope` — görünüm yok edildiğinde iş
 * **otomatik iptal ediliyor**. `isAdded` kontrolüne gerek kalmıyor
 * çünkü iptal edilen coroutine zaten devam etmiyor.
 *
 * ── Neden tüm 87 çağrı birden değiştirilmedi ──
 * Riskli ve gereksiz. Bu sınıf yeni kod ve **en uzun süren işler**
 * için: AI istekleri, PDF açma, depolama ölçümü, toplu üretim.
 * Kısa işler (`runOnUiThread` ile bir metin güncelleme) zaten
 * sorunsuz çalışıyor.
 *
 * ── Neden `Dispatchers.IO` ──
 * Yaptığımız işlerin çoğu disk okuma veya ağ; CPU değil. `IO`
 * havuzu bu iş türü için boyutlandırılmış.
 */
object ArkaPlan {

    private const val TAG = "ArkaPlan"

    /**
     * Uygulama ömrü boyunca yaşayan kapsam.
     *
     * `App` içinden çağrılan, ekrandan bağımsız işler için
     * (bildirim kurulumu, önbellek temizliği). `SupervisorJob`:
     * bir işin çökmesi diğerlerini iptal etmesin.
     */
    val uygulamaKapsami = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ══════════════════════════════════════════════════════════
    // Fragment
    // ══════════════════════════════════════════════════════════

    /**
     * Arka planda [is] çalıştırır, sonucu ana iş parçacığında
     * [sonra] ile verir.
     *
     * Fragment'ın **görünümü** yok edildiğinde otomatik iptal olur.
     *
     * ```
     * ArkaPlan.calis(this, is = { Depolama.olc(ctx) }) { kalemler ->
     *     ciz(kalemler)   // burada isAdded kontrolüne gerek yok
     * }
     * ```
     */
    fun <T> calis(
        fragment: Fragment,
        is_: suspend () -> T,
        sonra: (T) -> Unit
    ): Job? = runCatching {
        // viewLifecycleOwner: görünüm yok olunca iptal.
        // (lifecycleScope kullansaydık fragment geri yığında beklerken
        //  iş devam ederdi ve görünüm referansı sızardı.)
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val sonuc = withContext(Dispatchers.IO) { is_() }
            sonra(sonuc)
        }
    }.onFailure {
        android.util.Log.w(TAG, "Fragment kapsamı alınamadı", it)
    }.getOrNull()

    /**
     * Hata yönetimli sürüm — iş başarısız olursa [hata] çağrılır.
     *
     * Ağ isteklerinde şart: kullanıcı sonsuza kadar bekleyen bir
     * yükleme göstergesiyle kalmamalı.
     */
    // NOT (v8.9): `sonra` parametresi SONDA duruyor. Kotlin'de
    // trailing lambda son parametreye bağlanır; `hata` sonda olsaydı
    // `ArkaPlan.calisGuvenli(this, is_={...}) { ... }` yazımında
    // blok hataya giderdi ve `sonra` eksik kalırdı — ilk denemede
    // tam bu hatayı aldım ("None of the following functions can be
    // called with the arguments supplied").
    fun <T> calisGuvenli(
        fragment: Fragment,
        is_: suspend () -> T,
        hata: (Throwable) -> Unit = { android.util.Log.w(TAG, "İş başarısız", it) },
        sonra: (T) -> Unit
    ): Job? = runCatching {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sonuc = withContext(Dispatchers.IO) { is_() }
                sonra(sonuc)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // İptal normal akış — sessizce geç
                throw e
            } catch (e: Throwable) {
                hata(e)
            }
        }
    }.getOrNull()

    // ══════════════════════════════════════════════════════════
    // Activity
    // ══════════════════════════════════════════════════════════

    fun <T> calis(
        activity: AppCompatActivity,
        is_: suspend () -> T,
        sonra: (T) -> Unit
    ): Job? = runCatching {
        activity.lifecycleScope.launch {
            val sonuc = withContext(Dispatchers.IO) { is_() }
            if (!activity.isFinishing && !activity.isDestroyed) sonra(sonuc)
        }
    }.getOrNull()

    fun <T> calisGuvenli(
        activity: AppCompatActivity,
        is_: suspend () -> T,
        hata: (Throwable) -> Unit = { android.util.Log.w(TAG, "İş başarısız", it) },
        sonra: (T) -> Unit
    ): Job? = runCatching {
        activity.lifecycleScope.launch {
            try {
                val sonuc = withContext(Dispatchers.IO) { is_() }
                if (!activity.isFinishing && !activity.isDestroyed) sonra(sonuc)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!activity.isFinishing) hata(e)
            }
        }
    }.getOrNull()

    // ══════════════════════════════════════════════════════════
    // Ekrandan bağımsız
    // ══════════════════════════════════════════════════════════

    /**
     * Sonucu ekrana yansımayan işler (kayıt yazma, önbellek
     * temizliği, alarm kurulumu).
     *
     * ── Uyarı ──
     * Bu kapsam uygulama ömrü boyunca yaşıyor; içinde `Context`
     * tutarken **Application context** kullanın, Activity değil.
     */
    fun sessiz(is_: suspend () -> Unit): Job = uygulamaKapsami.launch {
        try {
            is_()
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Sessiz iş başarısız", e)
        }
    }
}
