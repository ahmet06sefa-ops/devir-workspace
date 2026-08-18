package com.gunlukasistan.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v8.6 — "Bir kez olsun" kaydı (öneri 24 destek sınıfı).
 *
 * ── Neden gerekli ──
 * Kutlama efektleri `bindStatus`/`reload` gibi HER onResume'da çalışan
 * fonksiyonlardan tetikleniyor. Kontrol olmasaydı hedefi tutturan
 * kullanıcı ekrana her dönüşünde konfeti görürdü — kutlama değil
 * rahatsızlık olurdu.
 *
 * ── Nasıl çalışıyor ──
 * Her olayın bir anahtarı var ("hedef_20260806"). Anahtar daha önce
 * işaretlenmişse [birKez] hiçbir şey yapmaz.
 *
 * ── Temizlik ──
 * Anahtarlar tarih içerdiği için sonsuza kadar birikirdi. 60 günden
 * eski kayıtlar açılışta siliniyor.
 */
object Basari {

    private const val TAG = "Basari"
    private const val PREF = "basari_kayit_v1"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Bugünün anahtar parçası: 20260806. */
    fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /**
     * [anahtar] daha önce işaretlenmemişse [is] çalıştırılır ve
     * anahtar işaretlenir.
     *
     * ```
     * Basari.birKez(ctx, "hedef_" + Basari.bugun()) { Kutlama.goster(...) }
     * ```
     *
     * @return iş çalıştırıldı mı
     */
    fun birKez(context: Context, anahtar: String, is_: () -> Unit): Boolean {
        return runCatching {
            val d = p(context)
            if (d.getBoolean(anahtar, false)) return false
            d.edit().putBoolean(anahtar, true).apply()
            is_()
            true
        }.onFailure { android.util.Log.w(TAG, "birKez", it) }.getOrDefault(false)
    }

    /** Bir anahtarın işaretli olup olmadığını sorar (iş çalıştırmaz). */
    fun isaretliMi(context: Context, anahtar: String): Boolean =
        p(context).getBoolean(anahtar, false)

    /** İşareti kaldırır — test veya sıfırlama için. */
    fun sil(context: Context, anahtar: String) {
        p(context).edit().remove(anahtar).apply()
    }

    /**
     * 60 günden eski tarihli anahtarları siler.
     *
     * `App.onCreate` içinden arka planda çağrılıyor.
     */
    fun temizle(context: Context) {
        runCatching {
            val esik = SimpleDateFormat("yyyyMMdd", Locale.US)
                .format(Date(System.currentTimeMillis() - 60L * 86_400_000L))
            val d = p(context)
            val silinecek = d.all.keys.filter { anahtar ->
                // Sondaki 8 hane tarih mi?
                val tarih = anahtar.takeLast(8)
                tarih.length == 8 && tarih.all { it.isDigit() } && tarih < esik
            }
            if (silinecek.isEmpty()) return
            val e = d.edit()
            silinecek.forEach { e.remove(it) }
            e.apply()
        }.onFailure { android.util.Log.w(TAG, "temizle", it) }
    }
}
