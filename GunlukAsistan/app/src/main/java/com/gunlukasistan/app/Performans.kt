package com.gunlukasistan.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * v7.61 — Donma (ANR / takılma) önleyici altyapı.
 *
 * ── Kullanıcının bildirimi ──
 * "Uygulama arada donuyor düzelt"
 *
 * ── Teşhis ──
 * Donmanın üç kaynağı vardı, üçü de **ana iş parçacığında ağır iş**:
 *
 * 1. **Otomatik yedek** — `Store.saveLogRoot()` her veri değişiminde
 *    `maybeAutoBackup()` çağırıyordu. O da tüm veriyi JSON'a çevirip
 *    (`exportJson`) hem uygulama klasörüne hem **MediaStore üzerinden
 *    İndirilenler'e** yazıyordu. Görev işaretlemek gibi tek bir dokunuş
 *    yüzlerce milisaniye disk G/Ç tetikliyordu. Ekran her seferinde
 *    donuyordu.
 *
 * 2. **Tekrarlı JSON ayrıştırma** — `logRoot()` her çağrıldığında günlük
 *    kaydın tamamını baştan parse ediyor. Tek ekran çiziminde 17 yerden
 *    çağrılabiliyordu.
 *
 * 3. **Widget tazeleme** — kayıt sonrası yayın (broadcast) fırtınası.
 *
 * ── Çözüm ──
 * · [arkaPlan] — ağır işleri tek bir arka plan iş parçacığına taşır
 * · [geciktir] — art arda gelen istekleri tek çağrıda toplar (debounce)
 * · [Store] içindeki yedekleme artık gecikmeli ve arka planda
 *
 * Veri kaybı riski yok: yedek yine alınıyor, sadece 2,5 saniye
 * beklenip son hâli bir kez yazılıyor. Ayrıca uygulama arka plana
 * alınırken ([MainActivity.onStop]) bekleyen yedek hemen tamamlanıyor.
 */
object Performans {

    private const val TAG = "Performans"

    /** Ağır işler için tek iş parçacığı — sıraya girer, çakışmaz. */
    private val havuz = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ga-arkaplan").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    private val anaElci = Handler(Looper.getMainLooper())

    /** Bekleyen gecikmeli işler — anahtar başına tek iş. */
    private val bekleyen = HashMap<String, Runnable>()

    /** İşi arka planda çalıştırır. Hata yutulur, uygulama akışı bozulmaz. */
    fun arkaPlan(is_: () -> Unit) {
        try {
            havuz.execute {
                try {
                    is_()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Arka plan işi başarısız", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İş kuyruğa alınamadı", e)
        }
    }

    /**
     * Aynı [anahtar] ile art arda gelen çağrıları tek işe indirger.
     * Son çağrıdan [gecikmeMs] sonra çalışır.
     *
     * Örnek: kullanıcı 10 görevi hızlıca işaretlerse yedek 10 kez değil
     * 1 kez alınır.
     */
    fun geciktir(anahtar: String, gecikmeMs: Long, is_: () -> Unit) {
        try {
            synchronized(bekleyen) {
                bekleyen.remove(anahtar)?.let { anaElci.removeCallbacks(it) }
                val gorev = Runnable {
                    synchronized(bekleyen) { bekleyen.remove(anahtar) }
                    arkaPlan(is_)
                }
                bekleyen[anahtar] = gorev
                anaElci.postDelayed(gorev, gecikmeMs)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Geciktirilemedi", e)
        }
    }

    /**
     * Bekleyen bir işi hemen çalıştırır (beklemeyi iptal eder).
     * Uygulama arka plana alınırken çağrılır — yedek kaybolmasın.
     */
    fun hemenBitir(anahtar: String) {
        try {
            val gorev = synchronized(bekleyen) { bekleyen.remove(anahtar) } ?: return
            anaElci.removeCallbacks(gorev)
            gorev.run()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Hemen bitirilemedi", e)
        }
    }

    /** Bekleyen tüm işleri hemen çalıştırır. */
    fun tumunuBitir() {
        val anahtarlar = synchronized(bekleyen) { bekleyen.keys.toList() }
        anahtarlar.forEach { hemenBitir(it) }
    }

    /** Ana iş parçacığında çalıştır. */
    fun anaIs(is_: () -> Unit) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) is_()
            else anaElci.post {
                try {
                    is_()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Ana iş başarısız", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ana işe gönderilemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BASİT ÖNBELLEK
    // ═══════════════════════════════════════════════════════════════

    private val onbellek = HashMap<String, Pair<Long, Any>>()

    /**
     * [uretMs] milisaniye boyunca geçerli kalan hesap sonucu döndürür.
     * Aynı ekran çiziminde tekrar tekrar JSON ayrıştırmayı önler.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> onbellekli(anahtar: String, uretMs: Long, uret: () -> T): T {
        val simdi = System.currentTimeMillis()
        synchronized(onbellek) {
            val kayit = onbellek[anahtar]
            if (kayit != null && simdi - kayit.first < uretMs) {
                try {
                    return kayit.second as T
                } catch (_: Exception) {
                    // tip uyuşmazlığı — yeniden üret
                }
            }
        }
        val deger = uret()
        synchronized(onbellek) { onbellek[anahtar] = simdi to deger }
        return deger
    }

    /** Bir önbellek kaydını (veya tümünü) geçersiz kılar. */
    fun onbellegiTemizle(anahtar: String? = null) {
        synchronized(onbellek) {
            if (anahtar == null) onbellek.clear() else onbellek.remove(anahtar)
        }
    }
}
