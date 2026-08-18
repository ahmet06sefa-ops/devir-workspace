package com.gunlukasistan.app

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.98 — Günlük yedek rotasyonu (öneri 2).
 *
 * ── Sorun ──
 * `Store.autoBackupNow` her seferinde **aynı dosyanın üzerine** yazıyordu
 * (`yedek.json`). Bu tek kopya:
 *   · Bozuk veri yazılırsa (yarım kalan işlem, disk hatası) sağlam sürüm yok
 *   · Kullanıcı yanlışlıkla her şeyi silse, yedek de silinmiş hâli içeriyor
 *   · "Dün nasıldı" diye dönmek imkânsız
 *
 * ── Çözüm ──
 * Günde bir kez tarihli kopya alınır: `yedek-20260806.json`. Son 7 gün
 * saklanır, eskiler silinir.
 *
 * ── Neden günde bir ──
 * `autoBackupNow` her veri değişiminde tetikleniyor (görev işaretlemek
 * bile). Her seferinde yeni dosya açmak diski doldururdu. Günde bir kopya,
 * 7 günlük geçmiş için yeterli ve ~7×200 KB yer kaplıyor.
 *
 * ── Mevcut yedek dosyası korundu ──
 * `yedek.json` eskisi gibi yazılmaya devam ediyor; kurtarma akışı ve
 * "kalıcı yedek" mantığı ona bakıyor. Rotasyon **ek** bir güvenlik ağı.
 */
object YedekRotasyon {

    private const val TAG = "YedekRotasyon"
    private const val PREF = "yedek_rotasyon_v1"
    private const val KLASOR = "yedekler"

    /** Kaç günlük kopya saklanır. */
    private const val SAKLANAN_GUN = 7

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /** Rotasyon klasörü — dış dosyalar dizini (uygulama verisiyle silinmez). */
    private fun klasor(context: Context): File {
        val kok = context.getExternalFilesDir(null) ?: context.filesDir
        return File(kok, KLASOR).apply { if (!exists()) mkdirs() }
    }

    // ═══════════════════════════════════════════════════════════════
    // AYAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Rotasyon açık mı?
     *
     * Varsayılan **açık**: veri kaybı riski, birkaç yüz kilobayt disk
     * kullanımından daha önemli.
     */
    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", true)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bugünün kopyası yoksa alır.
     *
     * `Store.autoBackupNow` içinden çağrılıyor. Günde bir kez iş yapıyor,
     * kalan çağrılarda anında dönüyor — sık tetiklenmesi sorun değil.
     */
    fun gerekirseAl(context: Context, icerik: String) {
        if (!acikMi(context)) return
        val bugun = bugun()
        if (prefs(context).getString("son_gun", "") == bugun) return

        try {
            val dosya = File(klasor(context), "yedek-$bugun.json")
            dosya.writeText(icerik, Charsets.UTF_8)
            prefs(context).edit().putString("son_gun", bugun).apply()
            eskileriSil(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Rotasyon yedeği alınamadı", e)
        }
    }

    /** Tavanı aşan eski kopyaları siler. */
    private fun eskileriSil(context: Context) {
        try {
            val dosyalar = klasor(context).listFiles { f ->
                f.name.startsWith("yedek-") && f.name.endsWith(".json")
            }?.sortedByDescending { it.name } ?: return

            dosyalar.drop(SAKLANAN_GUN).forEach { runCatching { it.delete() } }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Eski yedekler silinemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LİSTELEME / GERİ YÜKLEME
    // ═══════════════════════════════════════════════════════════════

    data class Kopya(val dosya: File, val gun: String, val boyut: Long) {
        /** "6 Ağustos" gibi okunabilir tarih. */
        fun tarihMetni(): String = try {
            val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(gun)
            if (d == null) gun else SimpleDateFormat("d MMMM", Locale("tr")).format(d)
        } catch (e: Exception) {
            gun
        }
    }

    /** Mevcut kopyalar — en yeni önce. */
    fun kopyalar(context: Context): List<Kopya> = try {
        klasor(context).listFiles { f ->
            f.name.startsWith("yedek-") && f.name.endsWith(".json")
        }?.map {
            Kopya(it, it.name.removePrefix("yedek-").removeSuffix(".json"), it.length())
        }?.sortedByDescending { it.gun } ?: emptyList()
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Kopyalar listelenemedi", e)
        emptyList()
    }

    fun kopyaOku(kopya: Kopya): String? = try {
        kopya.dosya.readText(Charsets.UTF_8)
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Kopya okunamadı", e)
        null
    }

    fun toplamBoyut(context: Context): Long = kopyalar(context).sumOf { it.boyut }

    fun temizle(context: Context): Int {
        val liste = kopyalar(context)
        liste.forEach { runCatching { it.dosya.delete() } }
        prefs(context).edit().remove("son_gun").apply()
        return liste.size
    }

    fun boyutMetni(bayt: Long): String = when {
        bayt < 1024 -> "$bayt B"
        bayt < 1024 * 1024 -> "${bayt / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bayt / 1048576.0)
    }

    /** Ayar satırında gösterilecek özet. */
    fun ozet(context: Context): String {
        if (!acikMi(context)) return context.getString(R.string.yr_kapali)
        val liste = kopyalar(context)
        return if (liste.isEmpty()) context.getString(R.string.yr_henuz_yok)
        else context.getString(
            R.string.yr_ozet, liste.size, boyutMetni(toplamBoyut(context))
        )
    }
}
