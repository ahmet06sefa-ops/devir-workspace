package com.gunlukasistan.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * v9.8 — WorkManager ile güvenilir arka plan işleri (öneri 47).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN GEREKLİ — SOMUT SORUN
 * ══════════════════════════════════════════════════════════════════
 * Otomatik yedekleme şöyle çalışıyordu:
 *
 * ```kotlin
 * Performans.geciktir(YEDEK_ISI, 2500L) { autoBackupNow(uygulama) }
 * ```
 *
 * Bu, veri her değiştiğinde 2,5 saniyelik bir zamanlayıcı kuruyor.
 * Uygulama açıkken sorunsuz. Ama şu senaryolarda **yedek alınmıyor**:
 *
 *   1. Kullanıcı görev ekleyip hemen uygulamadan çıkıyor
 *      → `hemenBitir` çağrılıyor ama uygulama zaten kapanmışsa geç
 *   2. Sistem düşük bellekte uygulamayı öldürüyor (2 GB RAM'li
 *      cihazlarda çok sık)
 *      → bekleyen iş kayboluyor, hiç haber yok
 *   3. Yedek yazma başarısız oluyor (disk dolu, izin sorunu)
 *      → tekrar denenmiyor
 *
 * Kullanıcı bunu **ancak telefonunu kaybedip geri yüklemeye
 * çalışınca** fark ediyor. En kötü zamanda.
 *
 * ══════════════════════════════════════════════════════════════════
 * WORKMANAGER NE GETİRİYOR
 * ══════════════════════════════════════════════════════════════════
 *   ✓ İş **diske** kaydediliyor (SQLite) — süreç ölse de duruyor
 *   ✓ Cihaz yeniden başlasa bile çalıştırılıyor
 *   ✓ Başarısızlıkta **üstel geri çekilme** ile tekrar deneniyor
 *   ✓ Kısıtlar tanımlanabiliyor (şarjda, ağ varken, pil doluyken)
 *   ✓ Aynı iş iki kez kuyruğa girmiyor (`ExistingWorkPolicy`)
 *
 * ══════════════════════════════════════════════════════════════════
 * ⚠️ NEYİ TAŞIMADIM — VE NEDEN
 * ══════════════════════════════════════════════════════════════════
 * **Alarmlar taşınmadı.** Bu bilinçli bir karar.
 *
 * WorkManager tam zaman garantisi **vermiyor**. Doze modunda ve
 * uygulama kısıtlamalarında işi erteleyebiliyor. Periyodik işlerde
 * minimum aralık **15 dakika** ve gerçekleşme zamanı ±birkaç dakika
 * kayabiliyor.
 *
 * Bu, şunlar için kabul edilemez:
 *   · İlaç hatırlatması (08:00'de olmalı, 08:25'te değil)
 *   · Namaz vakti bildirimi
 *   · Görev hatırlatıcısı ("toplantı 14:00")
 *   · Sayaç bitişi
 *
 * Bunlar `AlarmManager.setExactAndAllowWhileIdle` ile kalıyor.
 *
 * WorkManager'a taşınanlar — zamanı kritik **olmayan** işler:
 *   · Yedekleme (2 dakika gecikse fark etmez, ama KAYBOLMAMALI)
 *   · Online senkron kontrolü
 *   · Eski dosya temizliği
 *   · Güncelleme kontrolü
 *
 * "Yeni teknoloji çıktı, her şeyi ona taşıyalım" yaklaşımı burada
 * uygulamayı bozardı. Her araç kendi işi için.
 */
object ArkaPlanIs {

    private const val TAG = "ArkaPlanIs"

    /** İş adları — tekil olması `ExistingWorkPolicy` için şart. */
    const val IS_YEDEK = "gunlukasistan_yedek"
    const val IS_BAKIM = "gunlukasistan_bakim"
    const val IS_SENKRON = "gunlukasistan_senkron"

    const val ANAHTAR_TUR = "tur"
    const val TUR_YEDEK = "yedek"
    const val TUR_BAKIM = "bakim"
    const val TUR_SENKRON = "senkron"

    /**
     * WorkManager kullanılabilir mi?
     *
     * Savunmacı kontrol: `WorkManager.getInstance` başlatılmamışsa
     * `IllegalStateException` fırlatıyor. Tüm çağrılar bunu
     * kontrol ediyor ve başarısızlıkta **eski yönteme düşüyor**.
     * Yeni altyapı eskisini kırmamalı.
     */
    private fun yonetici(context: Context): WorkManager? = runCatching {
        WorkManager.getInstance(context.applicationContext)
    }.getOrElse {
        android.util.Log.w(TAG, "WorkManager alınamadı", it)
        null
    }

    // ══════════════════════════════════════════════════════════
    // Yedekleme
    // ══════════════════════════════════════════════════════════

    /**
     * Yedeklemeyi kuyruğa alır.
     *
     * `ExistingWorkPolicy.REPLACE`: kullanıcı arka arkaya 10 görev
     * eklerse 10 yedek işi kuyruğa girmesin — sonuncusu yeterli.
     *
     * @param gecikmeSn kaç saniye sonra çalışsın (toplu değişiklikleri
     *   biriktirmek için)
     * @return true = kuyruğa alındı, false = WorkManager yok
     */
    fun yedekKuyrugaAl(context: Context, gecikmeSn: Long = 5): Boolean {
        val wm = yonetici(context) ?: return false
        return runCatching {
            val istek = OneTimeWorkRequestBuilder<GenelIsci>()
                .setInputData(workDataOf(ANAHTAR_TUR to TUR_YEDEK))
                .setInitialDelay(gecikmeSn, TimeUnit.SECONDS)
                // Pil düşükken yedek almaya çalışmak kullanıcıya
                // zarar verir; birkaç dakika beklemek sorun değil.
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag(IS_YEDEK)
                .build()
            wm.enqueueUniqueWork(IS_YEDEK, ExistingWorkPolicy.REPLACE, istek)
            true
        }.getOrElse {
            android.util.Log.w(TAG, "Yedek işi kuyruğa alınamadı", it)
            false
        }
    }

    // ══════════════════════════════════════════════════════════
    // Periyodik bakım
    // ══════════════════════════════════════════════════════════

    /**
     * Günlük bakım işini kurar.
     *
     * Ne yapıyor:
     *   · Eski yedek kopyalarını temizler
     *   · Kullanım arabelleğini diske yazar
     *   · Güncelleme kontrolü yapar
     *   · Alarmların hâlâ kurulu olduğunu doğrular
     *
     * Neden periyodik: bunların hiçbiri acil değil ama **hiç
     * yapılmazsa** disk şişiyor ve alarmlar sessizce ölüyor.
     *
     * `KEEP` politikası: uygulama her açılışta bunu çağırıyor ama
     * zaten kurulmuşsa yeniden kurmuyor — aksi halde her açılışta
     * sayaç sıfırlanır ve iş hiç çalışmazdı.
     */
    fun bakimiKur(context: Context): Boolean {
        val wm = yonetici(context) ?: return false
        return runCatching {
            val istek = PeriodicWorkRequestBuilder<GenelIsci>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(ANAHTAR_TUR to TUR_BAKIM))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(IS_BAKIM)
                .build()
            wm.enqueueUniquePeriodicWork(
                IS_BAKIM, ExistingPeriodicWorkPolicy.KEEP, istek
            )
            true
        }.getOrElse {
            android.util.Log.w(TAG, "Bakım işi kurulamadı", it)
            false
        }
    }

    /**
     * Online senkron kontrolünü kurar.
     *
     * Yalnızca online özelliği açıksa. Ağ kısıtı var: bağlantı
     * yokken denemek boşuna pil harcar.
     */
    fun senkronuKur(context: Context, aralikDk: Int): Boolean {
        val wm = yonetici(context) ?: return false
        // WorkManager minimum 15 dakika kabul ediyor
        val aralik = aralikDk.coerceAtLeast(15).toLong()
        return runCatching {
            val istek = PeriodicWorkRequestBuilder<GenelIsci>(aralik, TimeUnit.MINUTES)
                .setInputData(workDataOf(ANAHTAR_TUR to TUR_SENKRON))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(IS_SENKRON)
                .build()
            wm.enqueueUniquePeriodicWork(
                IS_SENKRON, ExistingPeriodicWorkPolicy.UPDATE, istek
            )
            true
        }.getOrElse {
            android.util.Log.w(TAG, "Senkron işi kurulamadı", it)
            false
        }
    }

    fun senkronuIptalEt(context: Context) {
        runCatching { yonetici(context)?.cancelUniqueWork(IS_SENKRON) }
    }

    fun hepsiniIptalEt(context: Context) {
        runCatching {
            yonetici(context)?.let {
                it.cancelUniqueWork(IS_YEDEK)
                it.cancelUniqueWork(IS_BAKIM)
                it.cancelUniqueWork(IS_SENKRON)
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Durum
    // ══════════════════════════════════════════════════════════

    data class Durum(val ad: String, val hal: String, val calisiyor: Boolean)

    /**
     * Kurulu işlerin durumu — tanılama ekranı için.
     *
     * `get()` bloklayan bir çağrı; arka planda kullanın.
     */
    fun durumlar(context: Context): List<Durum> {
        val wm = yonetici(context) ?: return emptyList()
        val sonuc = mutableListOf<Durum>()
        listOf(IS_YEDEK to "Yedekleme", IS_BAKIM to "Günlük bakım", IS_SENKRON to "Senkron")
            .forEach { (etiket, ad) ->
                runCatching {
                    val bilgiler = wm.getWorkInfosForUniqueWork(etiket).get()
                    if (bilgiler.isNullOrEmpty()) {
                        sonuc.add(Durum(ad, "kurulu değil", false))
                    } else {
                        val b = bilgiler.first()
                        sonuc.add(
                            Durum(
                                ad,
                                b.state.name.lowercase(),
                                !b.state.isFinished
                            )
                        )
                    }
                }.onFailure { sonuc.add(Durum(ad, "okunamadı", false)) }
            }
        return sonuc
    }

    // ══════════════════════════════════════════════════════════
    // İşçi
    // ══════════════════════════════════════════════════════════

    /**
     * Tek işçi sınıfı, `tur` girdisine göre dallanıyor.
     *
     * ── Neden tek sınıf ──
     * Her iş için ayrı `Worker` sınıfı yazmak Manifest'te ayrı
     * kayıt ve R8'de ayrı `keep` kuralı demek. Üç iş için üç sınıf
     * gereksiz; `inputData` ile dallanmak yeterli ve daha az yer
     * kaplıyor.
     *
     * ── Neden CoroutineWorker ──
     * `Worker` ana iş parçacığını değil ama kendi arka plan
     * parçacığını **bloklar**. `CoroutineWorker` askıya alınabilir
     * ve iptal edilebilir; ayrıca projede zaten coroutine var.
     */
    class GenelIsci(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            val tur = inputData.getString(ANAHTAR_TUR) ?: TUR_BAKIM
            val ctx = applicationContext
            try {
                when (tur) {
                    TUR_YEDEK -> yedekle(ctx)
                    TUR_SENKRON -> senkron(ctx)
                    else -> bakim(ctx)
                }
                Result.success()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "İş başarısız: $tur", e)
                // Üç denemeden sonra pes et — sonsuz tekrar pil yer.
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }

        private fun yedekle(context: Context) {
            Store.autoBackupNow(context)
            runCatching { Kullanim.eylem(context, Kullanim.Eylem.YEDEK_AL) }
        }

        private fun senkron(context: Context) {
            // Online kapalıysa hiçbir şey yapma
            if (!runCatching { OnlineBekci.acikMi(context) }.getOrDefault(false)) return
            runCatching { OnlineBekci.kontrolEt(context) }
        }

        /**
         * Günlük bakım.
         *
         * Her adım ayrı `runCatching` içinde: biri başarısız olursa
         * diğerleri yine de çalışsın. Tek `try` bloğu kullansaydım
         * ilk hata kalan işleri atlardı.
         */
        private fun bakim(context: Context) {
            // 1. Kullanım arabelleğini diske yaz
            runCatching { Kullanim.diskeYaz(context) }

            // 2. Alarmların hâlâ kurulu olduğunu doğrula.
            //    Bazı üreticiler (Xiaomi, Huawei) uygulamayı
            //    "optimize edip" alarmları sessizce iptal ediyor.
            runCatching { AlarmScheduler.rescheduleAll(context) }
            runCatching { TakipAlarm.yenidenKur(context) }

            // 3. Güncelleme kontrolü
            runCatching { Guncelleme.kontrolEt(context) }

            // 4. Eski çökme kayıtlarını taşı (bir kerelik geçiş)
            runCatching { CokmeRapor.eskiKaydiTasi(context) }

            // 5. Konum hatırlatmalarını kontrol et
            runCatching { KonumHatirlatma.kontrolVeBildir(context) }

            // 6. Bakım kaydı — tanılama ekranı bunu gösteriyor
            runCatching {
                context.getSharedPreferences("arkaplan_is_v1", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("son_bakim", System.currentTimeMillis())
                    .apply()
            }
        }
    }

    /** Son bakım ne zaman çalıştı. */
    fun sonBakim(context: Context): Long = runCatching {
        context.getSharedPreferences("arkaplan_is_v1", Context.MODE_PRIVATE)
            .getLong("son_bakim", 0L)
    }.getOrDefault(0L)
}
