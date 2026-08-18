package com.gunlukasistan.app

import android.content.Context

/**
 * v10.21 · KULLANICI İSTEĞİ — Liste satır filtreleri ("her şeyin yetkisi
 * ayarlarda olsun" turunun kalan havuzu).
 *
 * ── Kapsam ──
 * İki kaydırılabilir liste widget'ına satır süzme ve satır sayısı yetkisi:
 *
 *  GÖREV LİSTESİ (TasksWidgetService):
 *   · Tamamlananları göster    (önceden koddan siliniyordu — gizliydi)
 *   · Tarihsizleri göster      (kova içinde üretiliyor ama gözükmeyebilir)
 *   · İleri tarihlileri göster
 *   · Satır sayısı — serbest (kodda sabit 40'tı; 1-999 arası kullanıcıda)
 *
 *  GERİ SAYIM LİSTESİ (EventsListService):
 *   · Geçmişi göster           (önceden en fazla 1 "geçti" satırı zorunluydu)
 *   · Yalnız sabitlenenler     (pin'li olmayan sınav/fatura elenir)
 *   · Satır sayısı — serbest (EventsListVeri.AZAMI_SATIR=6 sabitti)
 *
 * Seçim mantığı saf fonksiyonlardır; saat enjekte edilir → JVM birim testli
 * (gorevleriSec bugunSonuMs parametresi alır). Ekranı yok eden bir varsayılan
 * değiştirilmez: tüm anahtarların varsayılanı ESKİ davranıştır.
 */
object WidgetListe {

    private const val PREF = "wg_liste_v1"

    // ── Görev listesi anahtarları ──
    const val K_TW_BITEN = "tw_biten"        // tamamlananlar gösterilsin mi (vars.: kapalı)
    const val K_TW_TARIHSIZ = "tw_tarihsiz"  // tarihsiz kovası (vars.: açık)
    const val K_TW_ILERISI = "tw_ilerisi"    // bugünden sonraki kovası (vars.: açık)
    const val K_TW_SATIR = "tw_satir"        // azami satır (vars.: 40)

    // ── Geri sayım listesi anahtarları ──
    const val K_EV_GECMIS = "ev_gecmis"      // geçmiş satırı (vars.: açık)
    const val K_EV_SABIT = "ev_sabit"        // yalnız sabitlenenler (vars.: kapalı)
    const val K_EV_SATIR = "ev_satir"        // azami satır (vars.: 6)

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun gosterBool(c: Context, anahtar: String, varsayilan: Boolean): Boolean =
        prefs(c).getBoolean(anahtar, varsayilan)

    fun setBool(c: Context, anahtar: String, deger: Boolean) {
        prefs(c).edit().putBoolean(anahtar, deger).apply()
    }

    /** Satır sayısı — serbest tam sayı; teknik taban 1 (0 satır boş widget yapar,
     *  kullanıcıya bilerek değil yanlışlıkla sunulmaz). Üst sınır 999 güvenlik
     *  tavanı değil, launcher hafızası içindir; yazılan değer aynen saklanır. */
    fun satir(c: Context, anahtar: String, varsayilan: Int): Int =
        prefs(c).getInt(anahtar, varsayilan).coerceIn(1, 999)

    fun setSatir(c: Context, anahtar: String, deger: Int) {
        prefs(c).edit().putInt(anahtar, deger.coerceIn(1, 999)).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF SEÇİM MANTIĞI (birim testli)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Görev listesinin satırlarını üretir — v7.42'den beri süregelen
     * kova düzeni KORUNUR: [bugüne tarihli (saate göre)] →
     * [tarihsiz (yeniden eskiye)] → [ileri (tarihe göre)].
     *
     * @param bitenleriGoster  false = eski davranış (tamamlananlar hiç gelmez)
     * @param tarihsiziGoster  false = tarihsiz kovası elenir
     * @param ilerisiniGoster  false = bugünden sonrası elenir ("sadece bugün" modu)
     * @param bugunSonuMs      saat enjeksiyonu (testler sabit damga verir)
     */
    fun gorevleriSec(
        gorevler: List<Store.Task>,
        bitenleriGoster: Boolean,
        tarihsiziGoster: Boolean,
        ilerisiniGoster: Boolean,
        limit: Int,
        bugunSonuMs: Long
    ): List<Store.Task> {
        val taban = gorevler.filter { bitenleriGoster || !it.done }
        val bugun = taban.filter { it.dueAt in 1..bugunSonuMs }.sortedBy { it.dueAt }
        val tarihsiz = if (tarihsiziGoster) {
            taban.filter { it.dueAt == 0L }.sortedByDescending { it.createdAt }
        } else emptyList()
        val ileri = if (ilerisiniGoster) {
            taban.filter { it.dueAt > bugunSonuMs }.sortedBy { it.dueAt }
        } else emptyList()
        return (bugun + tarihsiz + ileri).take(limit.coerceIn(1, 999))
    }
}
