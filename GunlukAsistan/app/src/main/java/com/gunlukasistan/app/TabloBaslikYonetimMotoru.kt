package com.gunlukasistan.app

import android.content.Context
import android.view.View

/**
 * v10.86 — Uygulama Geneli Tablo ve Kart Konu Başlıkları Yönetim Motoru.
 *
 * Kullanıcının "Bana uygulamanin içindeki tablolarin konu başlıklarını kaldirmani
 * istiyorum. Mesela günlük ilerleme , konularim , odak sesleri vb gibi." talimatı
 * doğrultusunda:
 *
 *  • Uygulamadaki tüm tablo ve kartların konu başlıkları ("Günlük İlerleme",
 *    "Konularım", "Odak Sesleri", "Konu Dağılımı", "Aylık Çizelge" vb.)
 *    varsayılan olarak KAPALIDIR / KALDIRILMIŞTIR (tabloBasliklariGosterilsinMi = false).
 *  • Tüm sekmelerde tablolar ve kartlar başlık çubukları olmadan en sade haliyle çizilir.
 *  • Ayarlar ekranındaki "⚡ Hızlı Kontroller" altından dilediğiniz zaman tek tuşla
 *    açılıp kapatılabilir.
 */
object TabloBaslikYonetimMotoru {

    private const val PREF_NAME = "tablo_baslik_yonetim_v1"
    private const val KEY_GOSTER = "tablo_basliklari_goster"

    /**
     * Tablo konu başlıkları gösterilsin mi?
     * Kullanıcı talimatı doğrultusunda varsayılan olarak KAPALIDIR (false).
     */
    fun tabloBasliklariGosterilsinMi(context: Context? = null): Boolean {
        if (context == null) return false
        return try {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.getBoolean(KEY_GOSTER, false) // Varsayılan: KAPALI ("başlıkları kaldır")
        } catch (_: Exception) {
            false
        }
    }

    fun setTabloBasliklariGosterilsin(context: Context, goster: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_GOSTER, goster).apply()
    }

    /** View.VISIBLE veya View.GONE döndürür (Saf mantık - birim testli). */
    fun baslikGorunurluk(context: Context? = null): Int {
        return if (tabloBasliklariGosterilsinMi(context)) View.VISIBLE else View.GONE
    }

    /** Verilen tüm başlık görünümlerine gizlilik kuralını uygular. */
    fun basliklariUygula(context: Context?, vararg views: View?) {
        val vis = baslikGorunurluk(context)
        views.forEach { v ->
            try {
                v?.visibility = vis
            } catch (_: Exception) { }
        }
    }

    /** Ayarlar ekranında gösterilecek durum metni (Saf mantık - birim testli). */
    fun durumMetniGetir(context: Context? = null): Pair<String, String> {
        val goster = tabloBasliklariGosterilsinMi(context)
        return if (goster) {
            Pair(
                "📑 Tablo ve Kart Konu Başlıklarını Göster (AÇIK)",
                "AÇIK — Günlük ilerleme, Konularım, Odak sesleri vb. tabloların üst konu başlıkları gösteriliyor."
            )
        } else {
            Pair(
                "📑 Tablo ve Kart Konu Başlıklarını Göster (KAPALI)",
                "KAPALI — Günlük ilerleme, Konularım, Odak sesleri vb. tüm tabloların konu başlıkları kaldırıldı. Sade görünüm aktif."
            )
        }
    }
}
