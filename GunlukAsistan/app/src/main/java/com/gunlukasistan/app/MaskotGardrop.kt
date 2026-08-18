package com.gunlukasistan.app

import android.content.Context

/**
 * v10.11 · ULTRA-30 A5 — Pofi gardırop.
 *
 * ── Fikir ──
 * Böcek bizden biri gibi: katılım kazandırır. Aksesuarlar rozetlerin
 * görünür ödülüdür — hiçbiri satın alınamaz, hepsi emekle açılır.
 *
 * ── Kilit kuralları (hepsi şeffaf, vitrinde yazıyor) ──
 *  🧢 Bere    → mevcut seri ≥ 7 gün
 *  🕶️ Gözlük  → toplam tamamlanan ≥ 250 görev
 *  🧣 Eşarp   → 3 kayıtlı uyku gecesi (v10.9 defteri)
 *  👑 Taç     → en iyi seri ≥ 30 gün
 *
 * ── Neden bu dört eşik ──
 * Kurallar dört ayrı davranışı ödüllendirir: güncellik (seri),
 * birikim (toplam), yeni özellik (uyku), dayanıklılık (rekor).
 * Eşikler [acikMi] içinde saf — birim testli, karar tablosu sabit.
 */
object MaskotGardrop {

    private const val PREF = "maskot_gardrop_v1"
    private const val K_GIYILEN = "giyilen"

    // Karar tablosunun eşikleri (değişirse testler de değişir — bilinçli)
    const val BERE_ESIK = 7        // seri gün
    const val GOZLUK_ESIK = 250    // toplam tamamlanan
    const val ESARP_ESIK = 3       // uyku gecesi
    const val TAC_ESIK = 30        // en iyi seri

    const val BERE = "bere"
    const val GOZLUK = "gozluk"
    const val ESARP = "esarp"
    const val TAC = "tac"

    val ANAHTARLAR = listOf(BERE, GOZLUK, ESARP, TAC)

    data class Giris(
        val seriGun: Int,
        val toplam: Int,
        val uykuGecesi: Int,
        val enIyiSeri: Int
    )

    /** Belirli aksesuar açık mı (saf karar). */
    fun acikMi(anahtar: String, giris: Giris): Boolean = when (anahtar) {
        BERE -> giris.seriGun >= BERE_ESIK
        GOZLUK -> giris.toplam >= GOZLUK_ESIK
        ESARP -> giris.uykuGecesi >= ESARP_ESIK
        TAC -> giris.enIyiSeri >= TAC_ESIK
        else -> false
    }

    /** Tüm ancakların kilit durumu. */
    fun durumlar(giris: Giris): Map<String, Boolean> =
        ANAHTARLAR.associateWith { acikMi(it, giris) }

    fun girisTopla(context: Context): Giris {
        val seri = runCatching { Store.streakInfo(context) }.getOrDefault(0 to 0)
        val toplam = runCatching { Store.allTimeCompletions(context) }.getOrDefault(0)
        val uyku = runCatching {
            UykuCerceve.defter(context).count { it.uykuMs > 0 }
        }.getOrDefault(0)
        return Giris(
            seriGun = seri.first,
            toplam = toplam,
            uykuGecesi = uyku,
            enIyiSeri = seri.second
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // GİYİM
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun giyilenler(context: Context): Set<String> =
        (prefs(context).getString(K_GIYILEN, "") ?: "")
            .split(',')
            .filter { it in ANAHTARLAR }
            .toSet()

    /** Giyer/çıkarır; kilitli bir şey giyilemez (sessiz yok sayma). */
    fun giy(context: Context, anahtar: String, giy: Boolean) {
        if (anahtar !in ANAHTARLAR) return
        if (giy && !acikMi(anahtar, girisTopla(context))) return
        val set = giyilenler(context).toMutableSet()
        if (giy) set.add(anahtar) else set.remove(anahtar)
        prefs(context).edit().putString(K_GIYILEN, set.joinToString(",")).apply()
    }

    fun giyilenMi(context: Context, anahtar: String): Boolean =
        anahtar in giyilenler(context)
}
