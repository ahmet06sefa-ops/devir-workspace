package com.gunlukasistan.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.12 · ULTRA-30 / D20 — Kendinle maç (hayalet modu).
 *
 * ── Fikir ──
 * Kadranın içinde iki ince yay: biri GEÇMİŞ GÜNÜN aynı saate kadar
 * bitirdiği odaka, diğeri BUGÜNÜN aynı saate kadarki odak. Kullanıcı
 * dünkü/geçen haftaki kendisiyle yarışır: "geçen salının 8 dk
 * önündesin".
 *
 * ── Veri kaynağı (dürüstlük notu) ──
 * Oturum günlüğü [OdakKaydi] zaten tutuluyordu (v7.94). Zaman damgası
 * oturumun BİTİŞ anıdır; karşılaştırma "bu saate kadar biten seanslar"
 * üzerinden yapılır ve iki tarafa da aynı kural uygulanır — adildir.
 * Günlüğün 300 kayıt tavanı yüzünden birkaç hafta ötesi otomatik
 * düşer; "geçen hafta" rakibi bu sınır içinde rahatça yaşar.
 *
 * Matematik kısım framework'süz ve birim testlidir.
 */
object Hayalet {

    const val MOD_KAPALI = 0
    const val MOD_DUN = 1
    const val MOD_HAFTA = 2

    private const val PREF = "fo_hayalet_v1"
    private const val K_MOD = "mod"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun mod(c: Context): Int = prefs(c).getInt(K_MOD, MOD_HAFTA).coerceIn(0, 2)
    fun setMod(c: Context, m: Int) {
        prefs(c).edit().putInt(K_MOD, m.coerceIn(0, 2)).apply()
    }

    // ---------------- Saf hesap ----------------

    fun gunAnahtari(ms: Long): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))

    /** Rakip gün: DÜN = 1 gün geri, HAFTA = 7 gün geri (aynı hafta içi gün). */
    fun rakipGunAnahtari(mod: Int, simdiMs: Long): String {
        val geri = if (mod == MOD_DUN) 1 else 7
        val cal = Calendar.getInstance().apply {
            timeInMillis = simdiMs
            add(Calendar.DAY_OF_YEAR, -geri)
        }
        return gunAnahtari(cal.timeInMillis)
    }

    /** Gün içi dakika (00:00 → 0, 23:59 → 1439). */
    fun gununDakikasi(ms: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Verilen günde, günün [sinirDakika]'sına kadar BİTMİŞ oturumların
     * toplam dakikası. `oturumlar` = (bitişZamanıMs, dakika) çiftleri.
     */
    fun buSaateKadarDk(oturumlar: List<Pair<Long, Int>>, gunKey: String, sinirDakika: Int): Int =
        oturumlar.filter { gunAnahtari(it.first) == gunKey && gununDakikasi(it.first) <= sinirDakika }
            .sumOf { it.second }

    /** Günün tamamındaki odak (hayalet yayın tam boyu buna oranlanır). */
    fun tamGunDk(oturumlar: List<Pair<Long, Int>>, gunKey: String): Int =
        oturumlar.filter { gunAnahtari(it.first) == gunKey }.sumOf { it.second }

    data class Mac(
        val rakipDk: Int,   // rakibin bu saate kadar bitirdiği
        val senDk: Int,     // senin bu saate kadar bitirdiğin
        val rakipTam: Int,  // rakibin tüm günü
        val senTam: Int     // senin şu ana kadarki tüm günün
    ) {
        val fark: Int get() = senDk - rakipDk

        /** Rakip yay: gününün ne kadarı bu saate kadardı (0..1). */
        val rakipOran: Float
            get() = if (rakipTam <= 0) {
                0f
            } else (rakipDk.toFloat() / rakipTam).coerceIn(0f, 1f)

        /** Senin yayın: aynı ölçeğe (rakip tam gün) vurulur — dürüst yarış. */
        val senOran: Float
            get() = if (rakipTam <= 0) {
                0f
            } else (senDk.toFloat() / rakipTam).coerceIn(0f, 1f)
    }

    /** Mod kapalıysa null; değilse anlık maç durumu. */
    fun mac(context: Context, simdiMs: Long = System.currentTimeMillis()): Mac? {
        val m = mod(context)
        if (m == MOD_KAPALI) return null
        val tum = OdakKaydi.oturumlar(context).map { it.zaman to it.dakika }
        val sinir = gununDakikasi(simdiMs)
        return Mac(
            rakipDk = buSaateKadarDk(tum, rakipGunAnahtari(m, simdiMs), sinir),
            senDk = buSaateKadarDk(tum, gunAnahtari(simdiMs), sinir),
            rakipTam = tamGunDk(tum, rakipGunAnahtari(m, simdiMs)),
            senTam = tamGunDk(tum, gunAnahtari(simdiMs))
        )
    }

    fun rakipAd(context: Context): String = context.getString(
        if (mod(context) == MOD_DUN) R.string.fo_hayalet_dun else R.string.fo_hayalet_hafta
    )

    /** Kart altındaki canlı maç cümlesi. */
    fun metin(context: Context, m: Mac): String {
        if (m.rakipTam <= 0) return context.getString(R.string.fo_hayalet_yok)
        val ad = rakipAd(context)
        return when {
            m.fark > 0 -> context.getString(R.string.fo_hayalet_onde, ad, m.rakipDk, m.senDk, m.fark)
            m.fark < 0 -> context.getString(R.string.fo_hayalet_geride, ad, m.rakipDk, m.senDk, -m.fark)
            else -> context.getString(R.string.fo_hayalet_beraber, ad, m.senDk)
        }
    }
}
