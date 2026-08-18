package com.gunlukasistan.app

import android.view.View
import com.google.android.material.snackbar.Snackbar

/**
 * v8.0 — Genel geri alma yardımcısı (öneri 6).
 *
 * ── Sorun ──
 * Görevlerde geri alma vardı (v7.72) ama yeni eklenen yıkıcı işlemlerde
 * yoktu: ders bitirme, konu maddesi silme, hata defterinden çıkarma,
 * haftalık planı sıfırlama, yer imi silme... Yanlışlıkla basılan tek
 * düğme veriyi geri dönüşsüz siliyordu.
 *
 * ── Neden Snackbar ──
 * Onay penceresi (`AlertDialog`) her yıkıcı işlemde akışı kesiyor ve
 * kullanıcıyı yoruyor. Snackbar tersini yapıyor: işlem hemen olur, 5
 * saniye boyunca "GERİ AL" görünür. Doğru basanlar hiç durmaz, yanlış
 * basanlar kurtarır.
 *
 * ── Neden `Store.kaydetGeriAlma` kullanılmadı ──
 * O mekanizma tek bir global "son silme" tutuyor ve yalnızca `Store`
 * içinden çağrılıyor. Bu sınıf herhangi bir ekrandan, herhangi bir
 * işlem için kullanılabiliyor ve görsel geri bildirimi de kendisi
 * yönetiyor.
 */
object GeriAl {

    private const val TAG = "GeriAl"

    /** Geri alma penceresi — Snackbar.LENGTH_LONG ile uyumlu. */
    private const val SURE_MS = 5000

    /**
     * İşlemi geri alınabilir biçimde duyurur.
     *
     * ```
     * GeriAl.sun(view, "Ders bitirildi") {
     *     Mufredat.adimDurumu(ctx, adimId, false)
     * }
     * ```
     *
     * @param kok Snackbar'ın bağlanacağı görünüm
     * @param mesaj kullanıcıya gösterilecek metin
     * @param geriAlindi "GERİ AL"a basılırsa çalışacak iş
     */
    fun sun(kok: View?, mesaj: String, geriAlindi: () -> Unit) {
        if (kok == null) return
        try {
            Snackbar.make(kok, mesaj, Snackbar.LENGTH_LONG)
                .setDuration(SURE_MS)
                .setAction(kok.context.getString(R.string.ga_geri_al)) {
                    runCatching { geriAlindi() }
                        .onFailure { android.util.Log.w(TAG, "Geri alınamadı", it) }
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Snackbar gösterilemedi", e)
        }
    }

    /**
     * Geri alma sonrası ekranı tazelemek gerekiyorsa.
     *
     * Çoğu ekran veriyi yeniden çizmeli; ayrı bir geri çağrı almak
     * her kullanım yerinde iki lambda yazmayı gerektirirdi.
     */
    fun sun(
        kok: View?,
        mesaj: String,
        tazele: (() -> Unit)?,
        geriAlindi: () -> Unit
    ) {
        sun(kok, mesaj) {
            geriAlindi()
            tazele?.invoke()
        }
    }
}
