package com.gunlukasistan.app

import android.content.Context
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * v8.3 — Alt menü rozetleri (öneri 15).
 *
 * ── Sorun ──
 * Alt menüde 5 sekme var ama hiçbiri "burada iş var" demiyordu.
 * Kullanıcı bekleyen görevi olup olmadığını görmek için Bugün
 * sekmesine girmek zorundaydı. Tekrarı gelen konu maddeleri de
 * öyle — girmeden bilinmiyordu.
 *
 * ── Neden Material rozet ──
 * `BottomNavigationView.getOrCreateBadge()` Material 3'ün kendi
 * rozet bileşenini kullanıyor: doğru konum, doğru boyut, tema
 * renkleriyle uyumlu, erişilebilirlik metni otomatik. Elle çizilen
 * bir nokta bunların hiçbirini vermezdi.
 *
 * ── Neden sayı sınırı ──
 * `maxCharacterCount = 2` → 99'dan fazlası "99+" olur. Üç haneli
 * sayı rozeti sekme ikonunu tamamen kapatıyor.
 *
 * ── Performans ──
 * `tazele()` her ekran geçişinde çağrılıyor. Sayımlar yerel
 * SharedPreferences/JSON'dan okunuyor; ölçülen maliyet birkaç
 * milisaniye. Yine de sonuçlar 30 saniye önbelleklenip gereksiz
 * okuma engelleniyor.
 */
object Rozet {

    private const val TAG = "Rozet"

    /** Önbellek: aynı saniyede birden çok kez sayım yapılmasın. */
    private var sonHesap = 0L
    private var sonBugun = 0
    private var sonKonu = 0
    private const val ONBELLEK_MS = 30_000L

    /**
     * Alt menüdeki rozetleri günceller.
     *
     * @param zorla önbelleği atla (veri değiştiğinde)
     */
    fun tazele(nav: BottomNavigationView?, context: Context, zorla: Boolean = false) {
        nav ?: return
        runCatching {
            val simdi = System.currentTimeMillis()
            if (zorla || simdi - sonHesap > ONBELLEK_MS) {
                sonBugun = bugunBekleyen(context)
                sonKonu = tekrariGelen(context)
                sonHesap = simdi
            }
            uygula(nav, R.id.nav_today, sonBugun)
            uygula(nav, R.id.nav_topics, sonKonu)
        }.onFailure { android.util.Log.w(TAG, "tazele", it) }
    }

    private fun uygula(nav: BottomNavigationView, menuId: Int, sayi: Int) {
        runCatching {
            if (sayi <= 0) {
                nav.removeBadge(menuId)
                return
            }
            val rozet = nav.getOrCreateBadge(menuId)
            rozet.isVisible = true
            rozet.maxCharacterCount = 2
            rozet.number = sayi
            rozet.backgroundColor = com.google.android.material.color.MaterialColors.getColor(
                nav, com.google.android.material.R.attr.colorPrimary, 0xFFB08968.toInt()
            )
            rozet.badgeTextColor = com.google.android.material.color.MaterialColors.getColor(
                nav, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt()
            )
        }.onFailure { android.util.Log.w(TAG, "uygula", it) }
    }

    /**
     * Bugün için bekleyen iş sayısı.
     *
     * Sayılanlar: bugüne kadar (dahil) vadesi gelmiş tamamlanmamış
     * görevler + bugün henüz işaretlenmemiş alışkanlıklar.
     * Vadesiz görevler sayılmıyor — onlar "bugün" değil "bir ara".
     */
    private fun bugunBekleyen(context: Context): Int = runCatching {
        val gunSonu = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis

        val gorev = Store.aktifGorevler(context)
            .count { !it.done && it.dueAt in 1..gunSonu }

        val aliskanlik = runCatching {
            val (yapilan, toplam) = Store.habitProgressToday(context)
            (toplam - yapilan).coerceAtLeast(0)
        }.getOrDefault(0)

        gorev + aliskanlik
    }.getOrDefault(0)

    /**
     * Konular sekmesi rozeti: hata defterinde bugün tekrarı gelen soru.
     *
     * Not: konu maddelerinin kendisinde aralıklı tekrar YOK (yalnız
     * hata defterinde Leitner var, v7.83). Bu yüzden burada sadece
     * `Hatalarim.bugunkuSayi` kullanılıyor. Konulara tekrar eklenirse
     * bu fonksiyona eklenmeli.
     */
    private fun tekrariGelen(context: Context): Int = runCatching {
        // v9.0 · Öneri 53: artık konu maddelerinde de aralıklı tekrar var.
        //
        // v8.3'te bu fonksiyonu yazarken "konularda tekrar sistemi yok"
        // diye not düşmüştüm ve yalnız hata defterini sayıyordu.
        // Faz 3'te KonuTekrar eklendi; rozet artık ikisini birden
        // gösteriyor.
        Hatalarim.bugunkuSayi(context) + KonuTekrar.bugunkuSayi(context)
    }.getOrDefault(0)

    /** Veri değişince çağrılır — önbelleği geçersiz kılar. */
    fun bozulsun() {
        sonHesap = 0L
    }
}
