package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Telefon yeniden başlatıldığında kurulu hatırlatıcıları geri kurar
 * ve widget'ları tazeler. Ayrıca gün/saat değişimlerinde de tetiklenir,
 * böylece geri sayımlar gece yarısı kendiliğinden güncellenir.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.rescheduleAll(context)
                CourseReminderReceiver.schedule(context)
                WidgetCommon.refreshAll(context)
                // v7.43: bildirim turlarini yeniden zamanla
                try {
                    BildirimMerkezi.kanallariKur(context)
                    if (Store.getNotifEnabled(context)) BildirimZamanlayici.kur(context)
                    runCatching { NotHatirlatici.yenidenKur(context) }
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Bildirim zamanlanamadı", e)
                }
                // v10.9: gün çerçevesi alarmları yeniden başlatmada
                // kaybolmasın (kur kendi içinde açık/izin denetimi yapar)
                // v10.42: akşam planı alarmı da yeniden kurulur
                runCatching { PlanAsistan.kur(context) }
                try {
                    UykuZamanla.kur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Uyku çerçevesi kurulamadı", e)
                }
                // v7.47: namaz vakti alarmları
                try {
                    NamazBildirim.hepsiniKur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Namaz alarmı kurulamadı", e)
                }
                // v7.57: online arka plan kontrolü
                try {
                    OnlineBekci.kanalKur(context)
                    OnlineBekci.kur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Online bekçi kurulamadı", e)
                }
                // v7.78: koç alarmları — yeniden başlatmada kaybolmasın
                try {
                    if (Koc.acikMi(context)) KocZamanlayici.kur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Koç alarmı kurulamadı", e)
                }
                // v9.7 · Grup F: ilaç/fatura/belge/araç hatırlatıcıları
                try {
                    TakipAlarm.yenidenKur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Takip alarmı kurulamadı", e)
                }
                // v10.1 🔴 DÜZELTME — çalışan sayaç yeniden başlatmada ölüyordu.
                // Bitiş alarmı + tazeleme zinciri + panel bildirimi geri kurulur;
                // kapalıyken bittiyse eksik bitiş teslim edilir.
                try {
                    SayacGeriKur.esitle(context, "BOOT")
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Sayaç geri kurulamadı", e)
                }
                // v9.1 · Öneri 44: yeniden başlatma sonrası kurulum kaydı
                AlarmSagligi.kurulumKaydet(context, "BOOT")
            }
            // ══════════════════════════════════════════════════════
            // v9.1 · Öneri 45 + 46 — Saat dilimi / gün değişimi
            // ══════════════════════════════════════════════════════
            //
            // 🔴 DÜZELTİLEN EKSİK:
            // Bu dal yalnızca widget'ları ve namaz/koç alarmlarını
            // tazeliyordu. GÖREV HATIRLATICILARI yeniden kurulmuyordu.
            //
            // Sonuç: kullanıcı yurt dışına çıkınca veya yaz saati
            // değişince görev alarmları ESKİ saat dilimine göre
            // kalıyordu. Türkiye'den Almanya'ya giden biri
            // hatırlatıcılarını iki saat kaymış buluyordu.
            //
            // `ACTION_MY_PACKAGE_REPLACED` (uygulama güncellemesi)
            // durumu daha kötüydü: Android güncellemede TÜM alarmları
            // iptal ediyor. Bu dal alarmları yeniden kurmadığı için
            // her güncellemeden sonra görev hatırlatıcıları sessizce
            // ölüyordu. Kullanıcı "güncelledim, bildirimler kesildi"
            // derdi ve sebebini kimse bulamazdı.
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetCommon.refreshAll(context)

                // v9.1: görev alarmlarını da yeniden kur.
                // rescheduleAll geçmiş tarihli alarmları atlıyor,
                // yani ikinci kez kurmak zararsız.
                try {
                    AlarmScheduler.rescheduleAll(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Görev alarmları kurulamadı", e)
                }
                try {
                    CourseReminderReceiver.schedule(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Ders hatırlatıcısı kurulamadı", e)
                }
                try {
                    if (Store.getNotifEnabled(context)) BildirimZamanlayici.kur(context)
                    runCatching { NotHatirlatici.yenidenKur(context) }
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Bildirim zamanlanamadı", e)
                }
                // v10.9: gün çerçevesi — saat dilimi / gün değişiminde
                // ve uygulama güncellemesinde kapılar yeniden hesaplanır
                try {
                    UykuZamanla.kur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Uyku çerçevesi kurulamadı", e)
                }
                // v7.47: yeni günün namaz vakitleri
                try {
                    NamazBildirim.hepsiniKur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Namaz alarmı kurulamadı", e)
                }
                // v7.78: yeni günün koç alarmları
                try {
                    if (Koc.acikMi(context)) KocZamanlayici.kur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Koç alarmı kurulamadı", e)
                }
                // v9.7: takip alarmları — saat dilimi değişince ilaç
                // saatleri de kayar; yeniden hesaplanmalı
                try {
                    TakipAlarm.yenidenKur(context)
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Takip alarmı kurulamadı", e)
                }
                // v10.1 🔴 DÜZELTME — Android güncellemede TÜM alarmları
                // iptal ediyor; sayaç da burada geri kurulmalıydı.
                try {
                    SayacGeriKur.esitle(context, intent.action ?: "?")
                } catch (e: Exception) {
                    android.util.Log.w("BootReceiver", "Sayaç geri kurulamadı", e)
                }
                // v9.1 · Öneri 44: kurulum sonucunu kaydet — tanılama
                // ekranı "alarmlar en son ne zaman kuruldu" gösterebilsin
                AlarmSagligi.kurulumKaydet(context, intent.action ?: "?")
            }
        }
    }
}
