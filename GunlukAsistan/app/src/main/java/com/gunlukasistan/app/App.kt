package com.gunlukasistan.app

import android.app.Application
import android.util.Log

/**
 * Uygulama çöktüğünde hatanın teknik detayını kaydeder.
 * Bir sonraki açılışta MainActivity bu kaydı kullanıcıya gösterir,
 * böylece sorun kolayca raporlanabilir.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // v8.3 · Öneri 9: gece modu tercihi HER ŞEYDEN ÖNCE uygulanmalı.
        //
        // AppCompatDelegate.setDefaultNightMode() ilk Activity
        // oluşturulmadan çağrılmazsa uygulama önce yanlış modda açılıp
        // sonra kendini yeniden oluşturur — gözle görülür bir titreme.
        // Bu çağrı diskten tek bir int okuyor, açılışı yavaşlatmıyor.
        try {
            ThemeManager.geceModunuUygula(this)
        } catch (e: Exception) {
            Log.w("App", "Gece modu uygulanamadı", e)
        }
        // v10.22 — Gizlilik Kilidi yaşam döngüsü bekçisi.
        //
        // PIN kuruluysa uygulama öne gelirken KilitActivity açılır.
        // Kararlar KilitMantik'te (JVM testli); burada yalnızca yaşam
        // döngüsü sayımı ve bayrak tüketimi var:
        //   · soğuk açılışta kilit (arka plan kaydı yokken)
        //   · arka planda seçili süre geçtiyse kilit (0 = her ayrılış,
        //     yine de kısa yapılandırma geçişleri GECIS_ESIGI ile elenir)
        //   · az önce açıldıysa bir resume boyunca kilit yok (sonsuz döngü yok)
        //   · KilitActivity'nin kendisi asla kilitlenmez
        try {
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                private var acikSayisi = 0

                override fun onActivityStarted(activity: android.app.Activity) {
                    acikSayisi++
                }

                override fun onActivityStopped(activity: android.app.Activity) {
                    acikSayisi--
                    if (acikSayisi <= 0) {
                        acikSayisi = 0
                        KilitDepo.Oturum.arkayaGitti = true
                        KilitDepo.arkaPlanaGecti(activity, System.currentTimeMillis())
                    }
                }

                override fun onActivityResumed(activity: android.app.Activity) {
                    if (activity is KilitActivity) return
                    if (!KilitDepo.kuruluMu(activity)) return
                    if (KilitActivity.gosteriliyor) return
                    // Bayraklar tek seferlik tüketilir — kullanıcı aktifken
                    // eski arka plan damgasıyla sürekli kontrol edilmesin.
                    val arkayaGitti = KilitDepo.Oturum.arkayaGitti
                    val azOnce = KilitDepo.Oturum.azOnceAcildi
                    KilitDepo.Oturum.arkayaGitti = false
                    KilitDepo.Oturum.azOnceAcildi = false
                    val gerekli = arkayaGitti && KilitMantik.kilitGerekliMi(
                        kurulu = true,
                        azOnceAcildi = azOnce,
                        arkaPlanaGidisMs = KilitDepo.arkaPlanMs(activity),
                        simdiMs = System.currentTimeMillis(),
                        zamanAsimiMs = KilitDepo.zamanAsimiMs(activity)
                    )
                    if (gerekli) {
                        activity.startActivity(
                            android.content.Intent(activity, KilitActivity::class.java)
                        )
                        activity.overridePendingTransition(0, 0)
                    }
                }

                override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
                override fun onActivityPaused(a: android.app.Activity) {}
                override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
                override fun onActivityDestroyed(a: android.app.Activity) {}
            })
        } catch (e: Exception) {
            Log.w("App", "Kilit bekçisi kurulamadı", e)
        }
        // v7.61 — DONMA DUZELTMESI: acilisi bloklamayan kurulum.
        //
        // Eskiden PDF font onbellegi, bildirim kanallari ve alarm
        // zamanlamalari ana is parcaciginda yapiliyordu; uygulama ilk
        // acilista gorunur sekilde takiliyordu. Hicbiri ilk kare icin
        // gerekli degil — arka planda kurulur.
        Performans.arkaPlan {
            try {
                PdfSplitter.init(this)
            } catch (_: Exception) {
            }
            try {
                BildirimMerkezi.kanallariKur(this)
                if (Store.getNotifEnabled(this)) BildirimZamanlayici.kur(this)
                if (NamazVakti.acikMi(this) && NamazBildirim.acikMi(this)) {
                    NamazBildirim.hepsiniKur(this)
                }
                // v10.9: gün çerçevesi — süreç her doğduğunda
                // kapı alarmları kendini onarır (kur iç denetimli)
                UykuZamanla.kur(this)
            } catch (e: Exception) {
                Log.w("App", "Bildirim altyapısı kurulamadı", e)
            }
                // ══════════════════════════════════════════════════════
            // 🔴 v9.3 ONARIM — MainActivity devre dışı kalmış olabilir
            // ══════════════════════════════════════════════════════
            //
            // v8.3-v9.2 arasında alternatif simge seçen kullanıcıların
            // MainActivity'si DISABLED yapıldı. Bu ayar CİHAZDA kalıcı:
            // uygulamayı güncellemek onu geri açmıyor, Manifest'i
            // düzeltmek de yetmiyor. Çalışma anında elle açmak gerek.
            //
            // Onarım olmadan güncelleyen kullanıcı hâlâ çökme yaşardı.
            try {
                Simge.onarimYap(this)
            } catch (e: Exception) {
                Log.w("App", "Simge onarımı başarısız", e)
            }

            // v8.8 · Öneri 1: eski XOR biçimli API anahtarlarını
            // Keystore şifrelemesine taşı. Kullanıcı fark etmez.
            try {
                val tasinan = AiSettings.anahtarlariTasi(this)
                if (tasinan > 0) Log.i("App", "$tasinan API anahtarı şifrelendi")
            } catch (e: Exception) {
                Log.w("App", "Anahtar taşınamadı", e)
            }
            // v8.6: eski kutlama işaretlerini temizle (60 günden eski)
            try {
                Basari.temizle(this)
            } catch (e: Exception) {
                Log.w("App", "Başarı kaydı temizlenemedi", e)
            }
            // v7.78: koç bakımı — kaçırılan günü kapat, alarmı tazele,
            // motivasyon cümlesini önceden üret (bildirim anında ağ beklenmesin)
            try {
                if (Koc.acikMi(this)) {
                    Koc.gecmisiDenkleştir(this)
                    KocZamanlayici.kur(this)
                    KocMesaj.arkaPlandaUret(this)
                }
            } catch (e: Exception) {
                Log.w("App", "Koç kurulamadı", e)
            }
            // v9.7 · Grup F: takip alarmlarını tazele.
            //
            // Alarmlar cihaz yeniden başlatılınca silinir; BootReceiver
            // bunu yakalıyor ama kullanıcı "Force stop" yaptıysa BOOT
            // yayını da gelmez. Her açılışta yeniden kurmak ucuz
            // (bir AlarmManager çağrısı) ve güvenli.
            try {
                TakipAlarm.yenidenKur(this)
            } catch (e: Exception) {
                Log.w("App", "Takip alarmı kurulamadı", e)
            }
            // v10.1 🔴 DÜZELTME — çalışan sayaç her açılışta geri kurulur.
            //
            // v10.0'da sayaç yalnızca sayaç ekranı açılınca tazeleniyordu.
            // Kullanıcı uygulamayı açıp başka ekranda gezinirse paneldeki
            // bildirim (uyumluluk modunda ongoing değil) silinmişse asla
            // geri gelmiyordu. Artık süreç her doğduğunda altyapı kurulur.
            try {
                SayacGeriKur.esitle(this, "APP")
            } catch (e: Exception) {
                Log.w("App", "Sayaç geri kurulamadı", e)
            }
            // v9.7 · Öneri 45: konum hatırlatmalarını kontrol et.
            //
            // Gerçek geofencing YOK — bu bilinçli. Uygulama her
            // açıldığında son bilinen konuma bakıp yakındaki
            // hatırlatmaları gösteriyoruz. Konum isteği YAPILMIYOR,
            // yalnızca sistemde zaten duran son değer okunuyor;
            // pil maliyeti sıfıra yakın.
            try {
                KonumHatirlatma.kontrolVeBildir(this)
            } catch (e: Exception) {
                Log.w("App", "Konum kontrolü başarısız", e)
            }
            // ══════════════════════════════════════════════════════
            // v9.8 · Grup G — sistem sağlamlığı
            // ══════════════════════════════════════════════════════
            // v9.8 · Öneri 47: günlük bakım işi (WorkManager).
            // KEEP politikası — zaten kuruluysa yeniden kurulmuyor.
            try {
                ArkaPlanIs.bakimiKur(this)
                if (OnlineBekci.acikMi(this)) {
                    ArkaPlanIs.senkronuKur(this, OnlineBekci.siklikDk(this))
                }
            } catch (e: Exception) {
                Log.w("App", "Arka plan işi kurulamadı", e)
            }
            // v9.8 · Öneri 49: v9.7 öncesi tek çökme kaydını
            // yeni geçmiş listesine taşı (bir kerelik).
            try {
                CokmeRapor.eskiKaydiTasi(this)
            } catch (e: Exception) {
                Log.w("App", "Çökme kaydı taşınamadı", e)
            }
            // v9.8 · Öneri 48: günde bir kez güncelleme kontrolü.
            // Sessiz — sonuç bir sonraki ekranda gösteriliyor.
            try {
                Guncelleme.kontrolEt(this)
            } catch (e: Exception) {
                Log.w("App", "Güncelleme kontrolü başarısız", e)
            }
        }

        // v9.8 · Öneri 50: oturum sayacı.
        // Arka plan bloğunun DIŞINDA çünkü uygulama açılışını
        // temsil ediyor; arka plan işi gecikirse sayım kayardı.
        try {
            Kullanim.oturumBasladi(this)
        } catch (e: Exception) {
            Log.w("App", "Kullanım kaydı başarısız", e)
        }
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                // v8.8 · Öneri 11 incelemesi: burada `.commit()` DOĞRU.
                //
                // Öneri listesinde ".commit() ana iş parçacığında, apply()
                // olmalı" yazmıştım. İnceleyince gördüm ki bu iki kullanım
                // (burası ve Store.importJson) bilinçli:
                //   · Süreç birkaç milisaniye içinde ölecek
                //   · `apply()` yazmayı arka plana atar ve YETİŞEMEZ
                //   · Çökme kaydı kaybolur, hata hiç öğrenilemez
                // ANR riski yok çünkü zaten çöküyoruz.
                //
                // Değiştirmedim. Kendi önerimi reddetmek de bir sonuç.
                //
                // v9.8 · Öneri 49: kayıt CokmeRapor'a taşındı.
                // Artık SON 10 çökme saklanıyor (eskiden 1 taneydi) ve
                // tekrar eden hatalar tespit edilebiliyor. `commit()`
                // kuralı orada da geçerli.
                CokmeRapor.kaydet(this, thread.name, error)
            } catch (_: Throwable) {
                // Kayıt başarısız olsa da devam et.
                // `Throwable` yakalıyoruz: çökme anında OutOfMemoryError
                // gelirse `Exception` onu yakalamaz ve zincir kopardı.
            }
            previousHandler?.uncaughtException(thread, error)
        }
    }
}
