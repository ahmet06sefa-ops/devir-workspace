package com.gunlukasistan.app

import android.content.Context
import java.security.SecureRandom

/**
 * v10.22 — Gizlilik Kilidi kalıcı depo (SharedPreferences).
 *
 * Saklananlar: tuz + hash (PIN'in kendisi ASLA saklanmaz), deneme sayacı,
 * otomatik kilit seçimi ve arka plan damgası. Karar mantığı [KilitMantik]'te;
 * bu sınıf yalnızca okuma/yazma ve süreç-içi oturum bayraklarını tutar.
 */
object KilitDepo {

    private const val PREF = "kilit_v1"
    private const val K_TUZ = "tuz"
    private const val K_HASH = "hash"
    private const val K_HATALAR = "hatalar"
    private const val K_KILIT_BITIS = "kilit_bitis"
    private const val K_ZAMAN_ASIMI = "zaman_asimi"
    private const val K_ARKA_PLAN = "arka_plan"

    /**
     * Süreç-içi oturum bayrakları — bellekte yaşar, diske YAZILMAZ.
     * Süreç ölüp yeniden doğunca hepsi ilk değerine döner; bu bilinçli:
     * soğuk açılış = arkayaGitti true başlar → kilit sorulur.
     */
    object Oturum {
        /** Bu süreçte kilit en az bir kez başarıyla açıldı mı. */
        @Volatile var acik: Boolean = false

        /**
         * Kilit az önce açıldı — hemen ardından gelen ilk resume
         * tekrar kilitlenmemeli (tek seferlik tüketilen bayrak).
         */
        @Volatile var azOnceAcildi: Boolean = false

        /** Uygulama arka plana tamamen gitti mi (soğuk açılışta true). */
        @Volatile var arkayaGitti: Boolean = true
    }

    private val rasgele = SecureRandom()

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun kuruluMu(c: Context): Boolean = prefs(c).getString(K_HASH, null) != null

    fun tuz(c: Context): String = prefs(c).getString(K_TUZ, "") ?: ""

    private fun hash(c: Context): String = prefs(c).getString(K_HASH, "") ?: ""

    /** PIN kurar/değiştirir. Geçersizse false döner ve kayıt değişmez. */
    fun pinKaydet(c: Context, pin: String): Boolean {
        if (!KilitMantik.pinGecerliMi(pin)) return false
        val yeniTuz = KilitMantik.tuzUret(System.nanoTime(), rasgele.nextLong())
        prefs(c).edit()
            .putString(K_TUZ, yeniTuz)
            .putString(K_HASH, KilitMantik.pinHash(pin, yeniTuz))
            .putInt(K_HATALAR, 0)
            .putLong(K_KILIT_BITIS, 0L)
            .apply()
        Oturum.acik = true
        return true
    }

    /** Kilidi tamamen kaldırır (deneme sayacı da silinir). */
    fun kaldir(c: Context) {
        prefs(c).edit()
            .remove(K_TUZ).remove(K_HASH)
            .putInt(K_HATALAR, 0)
            .putLong(K_KILIT_BITIS, 0L)
            .apply()
        Oturum.acik = false
        Oturum.azOnceAcildi = false
    }

    /** PIN doğru mu — sabit zamanlı karşılaştırma. */
    fun pinDogruMu(c: Context, pin: String): Boolean =
        KilitMantik.sabitZamanliEsit(KilitMantik.pinHash(pin, tuz(c)), hash(c))

    // ── Deneme sayacı ─────────────────────────────────────────

    fun denemeDurumu(c: Context): KilitMantik.DenemeDurum =
        KilitMantik.DenemeDurum(
            hatalar = prefs(c).getInt(K_HATALAR, 0),
            kilitBitisMs = prefs(c).getLong(K_KILIT_BITIS, 0L)
        )

    fun yanlisKaydet(c: Context, simdiMs: Long): KilitMantik.DenemeDurum {
        val yeni = KilitMantik.yanlisDeneme(denemeDurumu(c), simdiMs)
        prefs(c).edit()
            .putInt(K_HATALAR, yeni.hatalar)
            .putLong(K_KILIT_BITIS, yeni.kilitBitisMs)
            .apply()
        return yeni
    }

    /** Doğru giriş kaydı — sayaç sıfırlanır, oturum açık işaretlenir. */
    fun dogruKaydet(c: Context) {
        prefs(c).edit().putInt(K_HATALAR, 0).putLong(K_KILIT_BITIS, 0L).apply()
        Oturum.acik = true
        Oturum.azOnceAcildi = true
    }

    // ── Otomatik kilit ────────────────────────────────────────

    fun zamanAsimiMs(c: Context): Long =
        prefs(c).getLong(K_ZAMAN_ASIMI, 300_000L) // varsayılan: 5 dakika

    fun zamanAsimiAyarla(c: Context, ms: Long) {
        prefs(c).edit().putLong(K_ZAMAN_ASIMI, ms).apply()
    }

    fun arkaPlanaGecti(c: Context, ms: Long) {
        prefs(c).edit().putLong(K_ARKA_PLAN, ms).apply()
    }

    fun arkaPlanMs(c: Context): Long = prefs(c).getLong(K_ARKA_PLAN, 0L)

    /** Seçili zaman aşımının dizini (diyalog ön-seçimi için). */
    fun zamanAsimiDizin(c: Context): Int =
        KilitMantik.ZAMAN_ASIMLARI.indexOf(zamanAsimiMs(c)).coerceAtLeast(0)
}
