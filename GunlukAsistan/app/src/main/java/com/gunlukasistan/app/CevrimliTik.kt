package com.gunlukasistan.app

/**
 * v11.13 — Tık çevrimi emniyet sarmalayıcısı (saf, JVM testli).
 *
 * Sayaç ekranındaki 100 ms'lik tık döngüsünün **asla takılı kalmamasını**
 * garanti eder. `govde` içinde hangi satır istisna fırlatırsa fırlatsın,
 * `tik()` "devam et" (false) döndürür ve bir sonraki tık zamanlanır —
 * böylece saat donmaz.
 *
 * ── v11.13 "saat takılıyor" kök nedeni ──
 * Eski `TimerFragment.tick()`'te `handler.postDelayed(ticker, 100)` en son
 * satıra yazılmıştı. Gövde içinde bir hata oluşursa (kadran/zincir güncelleme,
 * maç satırı, bildirim tazeleme vb.) bu satır hiç çalışmıyor, tık zinciri
 * kopuyor ve ekrandaki saat donuyordu. Bu sarmalayıcı hataları yutar ve
 * çevrimi her koşulda sürdürür.
 *
 * ── Sözleşme ──
 * `govde` geri dönüş değeri: `true` = çevrimi bitir (bu tık zamanlanmaz),
 * `false` = devam et (çağıran bir sonraki tıkı zamanlamalı).
 */
class CevrimliTik(
    private val govde: () -> Boolean,
    private val hataRaporla: (Throwable) -> Unit = {},
) {

    /**
     * Bir tık çalıştırır.
     * @return `true` ise çağıran çevrimi bitirmeli (durdurmalı);
     * `false` ise bir sonraki tıkı zamanlamalı.
     */
    fun tik(): Boolean {
        return try {
            govde()
        } catch (e: Exception) {
            hataRaporla(e)
            // Hata asla çevrimi durdurmaz: devam et.
            false
        }
    }
}
