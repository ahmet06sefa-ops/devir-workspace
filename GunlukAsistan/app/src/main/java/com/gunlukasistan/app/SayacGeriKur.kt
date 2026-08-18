package com.gunlukasistan.app

import android.content.Context
import android.content.Intent

/**
 * v10.1 — Çalışan sayacın altyapısını geri kuran tek kapı.
 *
 * ── Hangi hata düzeltiliyor ──
 * Telefon yeniden başladığında, gün/saat/saat dilimi değiştiğinde ve
 * **uygulama güncellendiğinde** Android tüm alarmları iptal eder.
 * [BootReceiver] görevleri, dersleri, namazı, koçu ve takibi geri
 * kuruyordu ama **sayacı unutmuştu**. Sonuç:
 *
 *   · Bitiş alarmı ölüyor → süre bitiyor, kullanıcıya hiç haber gelmiyor
 *   · 15 sn'lik tazeleme zinciri ([TimerAlarm.tazelemeyiKur]) ölüyor
 *   · Uyumluluk modundaki bildirim ongoing olmadığı için panelden
 *     silinince (Temizle / kaydırma) bir daha asla geri gelmiyor
 *   · Durum prefs'te durduğu için tanı ekranı "✓ sayaç çalışıyor"
 *     diyor ama bildirim panelde yok — kullanıcının v10.0'da
 *     raporladığı tablo tam olarak buydu
 *
 * ── Bu sınıf ne yapar ──
 * Üç tetik noktası, tek işlem:
 *   1. [BootReceiver] (BOOT + gün/saat değişimi + güncelleme)
 *   2. [App.onCreate] — kullanıcı uygulamayı açtı ama sayaç ekranına
 *      girmemiş olabilir; o ekran tazelemiyor
 *   3. Elle de çağrılabilir
 *
 * İşlem:
 *   · Sayaç çalışıyorsa bitiş alarmını ve tazeleme zincirini yeniden kur
 *   · Bildirimi geri koy (çalışıyorsa ve mini açıksa)
 *   · Uygulama kapalıyken süre BİTTİYSE eksik bitişi teslim et:
 *     bitiş akışı (ses + titreşim + odak kaydı) AlarmManager
 *     yerine buradan tetiklenir
 *
 * Böylece sayaç, uygulama hiç açılmasa da yeniden başlatma ve
 * güncelleme sonrasında da doğru çalışır.
 */
object SayacGeriKur {

    /** Geri kurulum kararı — saf mantık, birim test edilebilir. */
    enum class Eylem {
        /** Çalışan sayaç yok — hiçbir şey yapma. */
        YOK,

        /** Sayaç çalışıyor: bitiş + tazeleme alarmlarını kur, bildirimi koy. */
        KUR,

        /** Süre kapalıyken bitmiş: eksik bitiş akışını teslim et. */
        BITIR
    }

    /**
     * Duruma göre yapılacak işi döndürür. Android bağımsız —
     * [TimerEngine] ve [SayacAyar] değerlerini çağıran okur.
     */
    fun karar(calisiyor: Boolean, bitti: Boolean): Eylem = when {
        !calisiyor -> Eylem.YOK
        bitti -> Eylem.BITIR
        else -> Eylem.KUR
    }

    /**
     * Geri kurulumu uygular. Nereden çağrıldığı [sebep] ile izlenir
     * (log ve ileride tanılama için).
     */
    fun esitle(context: Context, sebep: String) {
        try {
            when (karar(TimerEngine.isRunning(context), TimerEngine.isFinished(context))) {
                Eylem.YOK -> Unit

                Eylem.KUR -> {
                    // Bitiş alarmı + (uyumluluksa) tazeleme zinciri
                    TimerAlarm.reschedule(context)
                    // Bildirim panelde değilse geri koy; zaten varsa
                    // notify() aynı kimlikle tazeler — zararsız.
                    TimerNotifier.show(context)
                }

                Eylem.BITIR -> {
                    // Süre, kimse dinlemezken bitti. Bitiş alarmla gelirdi;
                    // alarm silindiğinde bu akış asla çalışmazdı (v10.0 hatası).
                    // [TimerActionReceiver.ACTION_FINISHED] aynen alarm gibi:
                    // TimerEngine.finish + ses + titreşim + odak kaydı +
                    // pomodoro döngüsü.
                    TimerAlarm.cancel(context)
                    context.sendBroadcast(
                        Intent(context, TimerActionReceiver::class.java).apply {
                            action = TimerActionReceiver.ACTION_FINISHED
                        }
                    )
                    android.util.Log.i("SayacGeriKur", "Eksik bitiş teslim edildi ($sebep)")
                }
            }
        } catch (e: Exception) {
            // Geri kurulum asla çöktürmemeli — uygulama açılışının
            // ve önyükleme alıcısının ortasında çalışıyor.
            android.util.Log.w("SayacGeriKur", "Geri kurulamadı ($sebep)", e)
        }
    }
}
