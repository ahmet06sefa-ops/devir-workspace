package com.gunlukasistan.app

import android.content.Context
import android.view.View
import java.util.Locale

/**
 * v10.77 — KPSS / Sınav Hazırlık Modu Karar & Merkezi Yönetim Motoru.
 *
 * Kullanıcının "Ben suanlik kpss çalışmiyorum onu heryerden kapat acmak icin
 * ayarlardan ayarbileyim ve ayarlarda kpss icin herseyini yonetebilecegim bir yer
 * ayarla ve bütün ayarlarini ordan yapabileceyim." talimatı doğrultusunda:
 *
 *  1. Varsayılan olarak KPSS / Sınav Modu KAPALI'dır (kpssModuAktifMi = false).
 *  2. Kapalıyken Ana Ekran, Bugün, İlerleme ve Brifing ekranlarından KPSS/YKS
 *     modülleri, deneme net barometreleri ve sınav hatırlatıcıları gizlenir;
 *     yerini sade Yaşam Sağlığı, Biyo-Ritim ve Kişisel Görev Asistanı alır.
 *  3. Ayarlar ekranındaki anahtar ile tek tuşta açılıp kapatılır.
 *  4. Ayarlar > "KPSS Merkezi Yönetim & Ayarlar Atölyesi" üzerinden hedef puan,
 *     hedef net, mevcut net, sınav adı ve tüm 9 çalışma atölyesi yönetilir.
 */
object KpssModuKararMotoru {

    private const val PREF_NAME = "kpss_modu_karar_v1"
    private const val KEY_AKTIF = "kpss_modu_aktif_mi"
    private const val KEY_HEDEF_PUAN = "kpss_hedef_puan"
    private const val KEY_HEDEF_NET = "kpss_hedef_net"
    private const val KEY_MEVCUT_NET = "kpss_mevcut_net"
    private const val KEY_SINAV_ADI = "kpss_sinav_adi"

    // ── KPSS MODU AKTİFLİK DURUMU ──
    fun kpssModuAktifMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_AKTIF, false) // Varsayılan: KAPALI ("şu an kpss çalışmıyorum")
    }

    fun setKpssModuAktif(context: Context, aktif: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_AKTIF, aktif).apply()
    }

    // ── GÖRÜNÜRLÜK KARARI ──
    fun kpssGorunurlukKarari(aktifMi: Boolean): Int {
        return if (aktifMi) View.VISIBLE else View.GONE
    }

    fun durumMetniGetir(aktifMi: Boolean): Pair<String, String> {
        return if (aktifMi) {
            Pair(
                "🎓 AÇIK: Sınav Hazırlık Modu (KPSS/YKS vb.)",
                "Ana ekran, bugün, ilerleme ve brifinglerde deneme barometresi ve sınav araçları gösteriliyor"
            )
        } else {
            Pair(
                "🎓 KAPALI: Sınav Modu Gizlendi (Yaşam & Konularım Senkronu Aktif)",
                "Uygulamadan sınav modülleri gizlendi. Yaşam, medikal takip, asistan ve Konularım (Store.loadTopics) devrede."
            )
        }
    }

    // ── MERKEZİ SINAV AYARLARI ──
    fun sinavAdiGetir(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_SINAV_ADI, "Genel Hedef 2026") ?: "Genel Hedef 2026"
    }

    fun sinavAdiKaydet(context: Context, ad: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val temiz = if (ad.isBlank()) "Genel Hedef 2026" else ad.trim()
        sp.edit().putString(KEY_SINAV_ADI, temiz).apply()
    }

    fun hedefPuanGetir(context: Context): Int {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getInt(KEY_HEDEF_PUAN, 450)
    }

    fun hedefPuanKaydet(context: Context, puan: Int) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_HEDEF_PUAN, puan.coerceIn(100, 500)).apply()
    }

    fun hedefNetGetir(context: Context): Float {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getFloat(KEY_HEDEF_NET, 90.0f)
    }

    fun hedefNetKaydet(context: Context, net: Float) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putFloat(KEY_HEDEF_NET, net.coerceIn(0.0f, 120.0f)).apply()
    }

    fun mevcutNetGetir(context: Context): Float {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getFloat(KEY_MEVCUT_NET, 78.5f)
    }

    fun mevcutNetKaydet(context: Context, net: Float) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putFloat(KEY_MEVCUT_NET, net.coerceIn(0.0f, 120.0f)).apply()
    }

    fun merkeziOzetMetniGetir(context: Context): String {
        val aktif = kpssModuAktifMi(context)
        val ad = sinavAdiGetir(context)
        val hPuan = hedefPuanGetir(context)
        val hNet = String.format(Locale.US, "%.1f", hedefNetGetir(context))
        val mNet = String.format(Locale.US, "%.1f", mevcutNetGetir(context))
        val modStr = if (aktif) "AÇIK" else "KAPALI (Gizlendi)"
        return "📍 Sınav: $ad · Mod: $modStr\n🎯 Hedef Puan: $hPuan · Hedef Net: $hNet Net (Mevcut: $mNet Net)"
    }

    // ── BRİFİNG & GÜNDEM FİLTRELEME ──
    fun gundemGorevleriniFiltrele(aktifMi: Boolean, orijinalGorevler: List<String>): List<String> {
        if (aktifMi) return orijinalGorevler
        // KPSS kapalıysa, KPSS veya ders ibarelerini kaldırıp yaşam sağlığı hedefleri koy
        return listOf(
            "2500ml Su ve hidrasyon takibi (#2)",
            "Tansiyon 120/80 mmHg WHO seyrini kontrol et (#6)",
            "16:8 Aralıklı oruç penceresini izle (#10)",
            "Kişisel görev ve bütçe planı"
        )
    }
}
