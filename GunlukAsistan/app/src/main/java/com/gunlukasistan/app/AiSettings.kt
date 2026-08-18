package com.gunlukasistan.app

import android.content.Context
import android.util.Base64

/**
 * Yapay zekâ ayarları (v5.5).
 *
 * Varsayılan: ÇEVRİMDIŞI. Kullanıcı açıkça açmadıkça hiçbir veri cihazdan çıkmaz.
 * API anahtarı yalnızca bu cihazda, uygulamanın özel alanında saklanır.
 */
object AiSettings {

    private const val PREF = "ai_settings"
    private const val K_ONLINE = "online_mode"
    private const val K_PROVIDER = "provider"
    private const val K_KEY = "api_key_enc"
    private const val K_MODEL = "model"
    private const val K_ENDPOINT = "custom_endpoint"
    private const val K_FALLBACK = "offline_fallback"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Mod ----------------

    /** Çevrimiçi mod açık mı? Varsayılan: kapalı (tam gizlilik). */
    fun isOnlineMode(context: Context): Boolean =
        prefs(context).getBoolean(K_ONLINE, false)

    fun setOnlineMode(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(K_ONLINE, value).apply()
    }

    /** Çevrimiçi başarısız olursa çevrimdışı beyne düşülsün mü? Varsayılan: evet. */
    fun isFallbackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(K_FALLBACK, true)

    fun setFallbackEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(K_FALLBACK, value).apply()
    }

    // ---------------- Sağlayıcı ----------------

    fun getProviderId(context: Context): String =
        prefs(context).getString(K_PROVIDER, AiClient.Provider.GEMINI.id)
            ?: AiClient.Provider.GEMINI.id

    fun setProviderId(context: Context, id: String) {
        prefs(context).edit().putString(K_PROVIDER, id).apply()
    }

    fun getModel(context: Context): String =
        prefs(context).getString(K_MODEL, "") ?: ""

    fun setModel(context: Context, model: String) {
        prefs(context).edit().putString(K_MODEL, model.trim()).apply()
    }

    fun getCustomEndpoint(context: Context): String =
        prefs(context).getString(K_ENDPOINT, "") ?: ""

    fun setCustomEndpoint(context: Context, url: String) {
        prefs(context).edit().putString(K_ENDPOINT, url.trim()).apply()
    }

    // ---------------- API anahtarı ----------------

    /**
     * Anahtar hafif bir maskeyle saklanır. Bu askeri düzeyde şifreleme değildir;
     * amaç düz metin olarak göze çarpmamasıdır. Anahtar zaten yalnızca
     * uygulamanın özel dizininde durur ve başka uygulamalar erişemez.
     */
    /**
     * v8.8 · Öneri 1 — Anahtar saklama artık gerçek şifreleme.
     *
     * ── Önceki durum ──
     * Burada XOR + sabit tuz ("gunlukasistan") vardı. Bu şifreleme
     * değil GİZLEME: tuz kaynak kodda düz metin, XOR tersine
     * çevrilebilir, veri başka cihazda da çözülebiliyordu.
     * Root'lu cihazda API anahtarı okunabilirdi — kullanıcının
     * OpenAI faturası başkasına çıkabilirdi.
     *
     * ── Şimdi ──
     * [AnahtarKasa] Android Keystore ile AES-256-GCM kullanıyor.
     * Şifreleme anahtarı güvenli öğede (TEE/StrongBox), uygulama
     * bile ona erişemiyor. Veri dosyası kopyalansa başka cihazda
     * çözülemez.
     *
     * ── Geçiş ──
     * Eski XOR biçimiyle kaydedilmiş anahtarlar okunmaya devam
     * ediyor ([AnahtarKasa.coz] iki biçimi de anlıyor) ve ilk
     * yazmada otomatik olarak yeni biçime geçiyorlar. Kullanıcı
     * anahtarını yeniden girmek zorunda değil.
     */
    private fun mask(text: String, decode: Boolean): String =
        if (decode) AnahtarKasa.coz(text) else AnahtarKasa.sifrele(text)

    /**
     * v8.8 — Eski biçimli anahtarları sessizce yeni biçime taşır.
     *
     * `App.onCreate` içinden arka planda bir kez çağrılıyor.
     * Okuma zaten iki biçimi de destekliyor; bu, dosyada eski
     * (zayıf) biçimin kalmasını engelliyor.
     *
     * @return taşınan anahtar sayısı
     */
    fun anahtarlariTasi(context: Context): Int {
        var sayac = 0
        runCatching {
            val p = prefs(context)
            val e = p.edit()
            // Sağlayıcı bazlı anahtarlar + eski tek anahtar
            val adaylar = p.all.keys.filter {
                it.startsWith("api_key_") || it == K_KEY
            }
            adaylar.forEach { alan ->
                val saklanan = p.getString(alan, "") ?: ""
                if (saklanan.isBlank()) return@forEach
                if (AnahtarKasa.yeniBicimMi(saklanan)) return@forEach
                // Eski biçim: çöz ve yeniden şifrele
                val acik = AnahtarKasa.coz(saklanan)
                if (acik.isBlank()) return@forEach
                val yeni = AnahtarKasa.sifrele(acik)
                if (AnahtarKasa.yeniBicimMi(yeni)) {
                    e.putString(alan, yeni)
                    sayac++
                }
            }
            if (sayac > 0) e.apply()
        }.onFailure { android.util.Log.w("AiSettings", "Anahtar taşıma", it) }
        return sayac
    }

    fun getApiKey(context: Context): String {
        val stored = prefs(context).getString(K_KEY, "") ?: ""
        if (stored.isBlank()) return ""
        return mask(stored, decode = true)
    }

    fun setApiKey(context: Context, key: String) {
        val clean = key.trim()
        if (clean.isBlank()) {
            prefs(context).edit().remove(K_KEY).apply()
        } else {
            prefs(context).edit().putString(K_KEY, mask(clean, decode = false)).apply()
        }
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    // ═══════════════════════════════════════════════════════════════
    // v7.24 — SAĞLAYICI BAZLI ANAHTARLAR (otomatik geçiş için)
    // ═══════════════════════════════════════════════════════════════
    //
    // Eskiden tek anahtar vardı; aktif sağlayıcı değişince onu da
    // değiştirmek gerekiyordu. Artık her sağlayıcının anahtarı ayrı
    // saklanıyor, böylece biri tükendiğinde diğerine geçilebiliyor.

    private fun keyOf(providerId: String) = "api_key_" + providerId

    /** Belirli bir sağlayıcının anahtarı. */
    fun getKeyFor(context: Context, providerId: String): String {
        val p = prefs(context)
        val stored = p.getString(keyOf(providerId), "") ?: ""
        if (stored.isNotBlank()) return mask(stored, decode = true)
        // Geriye dönük: eski tek anahtar, o an aktif sağlayıcıya aitti
        if (providerId == getProviderId(context)) {
            val eski = p.getString(K_KEY, "") ?: ""
            if (eski.isNotBlank()) return mask(eski, decode = true)
        }
        return ""
    }

    fun setKeyFor(context: Context, providerId: String, key: String) {
        val clean = key.trim()
        val e = prefs(context).edit()
        if (clean.isBlank()) e.remove(keyOf(providerId))
        else e.putString(keyOf(providerId), mask(clean, decode = false))
        e.apply()
        // Aktif sağlayıcıysa eski alanı da güncel tut (diğer kodlar okuyor)
        if (providerId == getProviderId(context)) setApiKey(context, clean)
    }

    fun hasKeyFor(context: Context, providerId: String): Boolean =
        getKeyFor(context, providerId).isNotBlank()

    fun maskedKeyPreviewFor(context: Context, providerId: String): String {
        val key = getKeyFor(context, providerId)
        if (key.isBlank()) return ""
        if (key.length <= 12) return "•".repeat(key.length)
        return key.take(6) + "…" + key.takeLast(4)
    }

    /** v7.24: Anahtarı olan tüm sağlayıcı kimlikleri — geçiş sırası için. */
    fun anahtarliSaglayicilar(context: Context): List<String> =
        AiClient.Provider.entries
            .map { it.id }
            .filter { hasKeyFor(context, it) }

    // ── v7.34: Sadece ücretsiz modeller ──

    private const val K_UCRETSIZ = "sadece_ucretsiz"

    /**
     * Açıkken uygulama kredi/para harcayan hiçbir modeli çağırmaz.
     * VARSAYILAN AÇIK — kullanıcı bilmeden ücretlendirilmesin.
     *
     * Kapatmak için: Ayarlar → Yapay Zekâ → "Sadece ücretsiz modeller".
     */
    fun isUcretsizMod(context: Context): Boolean =
        prefs(context).getBoolean(K_UCRETSIZ, true)

    fun setUcretsizMod(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(K_UCRETSIZ, value).apply()
    }

    // ── Otomatik sağlayıcı geçişi anahtarı ──

    private const val K_AUTO_SWITCH = "auto_provider_switch"

    /** v7.24: Bir sağlayıcı çalışmazsa diğerine geçilsin mi (varsayılan açık). */
    fun isAutoSwitch(context: Context): Boolean =
        prefs(context).getBoolean(K_AUTO_SWITCH, true)

    fun setAutoSwitch(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(K_AUTO_SWITCH, value).apply()
    }

    /** Ekranda göstermek için: sk-abc…xyz9 */
    fun maskedKeyPreview(context: Context): String {
        val key = getApiKey(context)
        if (key.isBlank()) return ""
        if (key.length <= 12) return "•".repeat(key.length)
        return key.take(6) + "…" + key.takeLast(4)
    }

    /** Çevrimiçi mod gerçekten kullanılabilir mi (mod açık + anahtar var)? */
    fun isReady(context: Context): Boolean = isOnlineMode(context) && hasApiKey(context)

    // ---------------- v7.20: YouTube Data API (isteğe bağlı) ----------------
    //
    // Gemini grounding video de bulabiliyor ama YouTube API kullanılırsa
    // sonuçlar daha isabetli olur: süre, kanal, izlenme bilgisi gelir ve
    // linkin gerçekten var olduğu garantidir.

    private const val K_YT_KEY = "youtube_key_enc"

    fun getYoutubeKey(context: Context): String {
        val stored = prefs(context).getString(K_YT_KEY, "") ?: ""
        if (stored.isBlank()) return ""
        return mask(stored, decode = true)
    }

    fun setYoutubeKey(context: Context, key: String) {
        val clean = key.trim()
        if (clean.isBlank()) {
            prefs(context).edit().remove(K_YT_KEY).apply()
        } else {
            prefs(context).edit().putString(K_YT_KEY, mask(clean, decode = false)).apply()
        }
    }

    fun hasYoutubeKey(context: Context): Boolean = getYoutubeKey(context).isNotBlank()

    fun maskedYoutubePreview(context: Context): String {
        val key = getYoutubeKey(context)
        if (key.isBlank()) return ""
        if (key.length <= 12) return "•".repeat(key.length)
        return key.take(6) + "…" + key.takeLast(4)
    }
}
