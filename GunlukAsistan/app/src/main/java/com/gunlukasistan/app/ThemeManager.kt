package com.gunlukasistan.app

import android.content.Context

/**
 * 9 renk temasını yönetir: Krem, Violet, Okyanus, Orman, Gül, Altın +
 * koyu modlar: Gün Batımı, Aurora, Ember.
 * Seçim SharedPreferences'ta saklanır ve anında uygulanır.
 */
object ThemeManager {

    data class Spec(
        val title: String,
        val emoji: String,
        val styleRes: Int,
        val ringColor: Int,
        val cardColor: Int,
        val dark: Boolean
    )

    val specs = listOf(
        Spec("Krem", "🍦", R.style.Theme_GunlukAsistan, 0xFFB08968.toInt(), 0xFFFAF4E8.toInt(), false),
        Spec("Violet", "💜", R.style.Theme_GunlukAsistan_Violet, 0xFF7C5CBF.toInt(), 0xFFF4F1FA.toInt(), false),
        Spec("Okyanus", "🌊", R.style.Theme_GunlukAsistan_Okyanus, 0xFF1489A6.toInt(), 0xFFEEF5F8.toInt(), false),
        Spec("Orman", "🌲", R.style.Theme_GunlukAsistan_Orman, 0xFF3E7A4E.toInt(), 0xFFF0F5EE.toInt(), false),
        Spec("Gül", "🌸", R.style.Theme_GunlukAsistan_Gul, 0xFFD4789C.toInt(), 0xFFFBF1F4.toInt(), false),
        Spec("Altın", "✨", R.style.Theme_GunlukAsistan_Altin, 0xFFA8862A.toInt(), 0xFFFBF6E9.toInt(), false),
        Spec("Gün Batımı", "🌇", R.style.Theme_GunlukAsistan_GunBatimi, 0xFFE8916D.toInt(), 0xFF241B27.toInt(), true),
        Spec("Aurora", "🌌", R.style.Theme_GunlukAsistan_Aurora, 0xFF2EC4A6.toInt(), 0xFF0F1A24.toInt(), true),
        Spec("Ember", "🔥", R.style.Theme_GunlukAsistan_Ember, 0xFFE2603C.toInt(), 0xFF1D1512.toInt(), true),
        Spec("Zincir", "⛓️", R.style.Theme_GunlukAsistan_Zincir, 0xFF2BCFD0.toInt(), 0xFF0A1420.toInt(), true),
        Spec("Yeni Görünüm", "🌙", R.style.Theme_GunlukAsistan_YeniGorunum, 0xFF7C6BFF.toInt(), 0xFF151C33.toInt(), true)
    )

    /**
     * Zincir temasının neon vurgu paleti (v6.0).
     * Halka göstergeler, grafik çizgisi, seri çubukları ve ızgara bu sırayla renklenir.
     */
    val NEON_PALETTE = intArrayOf(
        0xFF2BCFD0.toInt(),  // teal
        0xFF54CA5A.toInt(),  // yeşil
        0xFF2C8DFE.toInt(),  // mavi
        0xFF9B6BFF.toInt(),  // mor
        0xFFFFCF50.toInt(),  // sarı
        0xFFFF6B9D.toInt()   // pembe
    )

    /** Seçili tema Zincir mi? (neon bileşenler buna göre davranır) */
    fun isNeon(context: Context): Boolean {
        // Zincir "Eski Görünüm" öncesi son temaydı; başlığa göre bulunur ki
        // yeni temalar (örn. Yeni Görünüm) eklense de neon mantığı bozulmasın.
        val zincir = specs.indexOfFirst { it.title == "Zincir" }
        return zincir >= 0 && selected(context) == zincir
    }

    private const val KEY = "theme_index_v1"
    private const val KEY_ACCENT = "theme_accent_v1"
    private const val PREF = "gunluk_asistan_store"

    // ═══════════════════════════════════════════════════════════════
    // v8.3 · Öneri 9 — GECE MODU
    // ═══════════════════════════════════════════════════════════════
    //
    // ── Ölçülen sorun ──
    // v8.2'ye kadar `res/values-night/` klasöründe yalnızca
    // `widget_colors.xml` vardı. Telefon gece moduna geçtiğinde ana
    // ekran widget'ları koyuluyor ama UYGULAMANIN KENDİSİ gündüz
    // kalıyordu. Karanlıkta uygulamayı açmak gözü yakıyordu.
    //
    // ── Neden fark edilmemişti ──
    // Uygulamada 4 koyu tema zaten vardı (Gün Batımı, Aurora, Ember,
    // Zincir) ama bunlar kullanıcı seçimi. Krem teması seçili biri
    // için gece modu hiçbir şey değiştirmiyordu.
    //
    // ── Çözüm ──
    // `values-night/themes.xml` yazıldı (6 açık temanın koyu karşılığı)
    // + burada bir mod seçici. Kullanıcı isterse sistemi izler,
    // isterse her zaman açık/koyu sabitler.

    private const val KEY_GECE = "gece_modu_v1"

    /** Sistem ayarını izle (varsayılan). */
    const val GECE_SISTEM = 0
    /** Her zaman açık tema. */
    const val GECE_KAPALI = 1
    /** Her zaman koyu tema. */
    const val GECE_ACIK = 2

    /**
     * v10.11 · ULTRA-30 A1 — Güneşe göre: gündüz açık, gece koyu.
     * Varsayılan güneş doğuşu/batışı yerel astronomik hesaptan
     * ([NamazVakti] aynı hesabı yapar), istenirse elle saat verilir.
     */
    const val GECE_GUNES = 3

    // Güneş modu zaman kaynağı
    const val GUNES_AUTO = 0        // astronomik doğuş/batış
    const val GUNES_OZEL = 1        // kullanıcının saat çifti

    private const val KEY_GUNES_KAYNAK = "gunes_kaynak"
    private const val KEY_GUNES_ACIL = "gunes_acil"     // varsayılan 07:00
    private const val KEY_GUNES_KARAN = "gunes_karan"   // varsayılan 19:00

    fun geceModu(context: Context): Int =
        prefs(context).getInt(KEY_GECE, GECE_SISTEM).coerceIn(0, 3)

    fun geceModu(context: Context, deger: Int) {
        prefs(context).edit().putInt(KEY_GECE, deger.coerceIn(0, 3)).apply()
        geceModunuUygula(context)
    }

    /**
     * Seçili modu `AppCompatDelegate`'e bildirir.
     *
     * Uygulama açılışında (App.onCreate) ve ayar değişiminde çağrılır.
     * `AppCompatDelegate` açık Activity'leri kendisi yeniden oluşturur.
     */
    fun geceModunuUygula(context: Context) {
        runCatching {
            val kip = when (geceModu(context)) {
                GECE_KAPALI -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                GECE_ACIK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                // A1: güneş ayarı delegate'e "anlık fotoğraf" olarak basılır.
                // App açıkken geçişin kendiliğinden olması için MainActivity
                // her onResume'da yeniden uygular; süreç doğunca App.kt de.
                GECE_GUNES -> if (gunesKoyuMu(context)) {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                }
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(kip)
        }.onFailure { android.util.Log.w("ThemeManager", "Gece modu uygulanamadı", it) }
    }

    /** Şu an koyu görünümde miyiz? (grafik renkleri buna bakar) */
    fun koyuMu(context: Context): Boolean {
        // Kullanıcı zaten koyu bir tema seçtiyse mod ne olursa olsun koyu
        if (specs.getOrNull(selected(context))?.dark == true) return true
        return when (geceModu(context)) {
            GECE_KAPALI -> false
            GECE_ACIK -> true
            GECE_GUNES -> gunesKoyuMu(context)
            else -> runCatching {
                val kip = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                kip == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }.getOrDefault(false)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A1 — GÜNEŞE GÖRE GECE MODU
    // ═══════════════════════════════════════════════════════════════

    /** Zaman kaynağı: [GUNES_AUTO] astronomik, [GUNES_OZEL] elle. */
    fun gunesKaynak(context: Context): Int =
        prefs(context).getInt(KEY_GUNES_KAYNAK, GUNES_AUTO).coerceIn(0, 1)

    fun gunesKaynak(context: Context, kaynak: Int) {
        prefs(context).edit().putInt(KEY_GUNES_KAYNAK, kaynak.coerceIn(0, 1)).apply()
        geceModunuUygula(context)
    }

    /** Elle aydınlanma dakikası (07:00 = 420). */
    fun gunesAcilDk(context: Context): Int =
        prefs(context).getInt(KEY_GUNES_ACIL, 420).coerceIn(0, 1439)

    fun gunesAcilDk(context: Context, dk: Int) {
        prefs(context).edit().putInt(KEY_GUNES_ACIL, dk.coerceIn(0, 1439)).apply()
        geceModunuUygula(context)
    }

    /** Elle kararma dakikası (19:00 = 1140). */
    fun gunesKaranDk(context: Context): Int =
        prefs(context).getInt(KEY_GUNES_KARAN, 1140).coerceIn(0, 1439)

    fun gunesKaranDk(context: Context, dk: Int) {
        prefs(context).edit().putInt(KEY_GUNES_KARAN, dk.coerceIn(0, 1439)).apply()
        geceModunuUygula(context)
    }

    /**
     * [simdiDk] koyu mu?
     *
     * Aydınlanma [acilDk]'da başlar, [karanDk]'da biter (dahil değil).
     * Gece yarısını AŞAN aralık bilinçli desteklenmez: kararma saati
     * aydınlanmadan küçükse takas edilir — kullanıcı "23'te kararsın
     * 06'da açılsın" istiyorsa aralık zaten gece yarısını aşıyordur,
     * takasla aynı sonuca gelinir (testlerde sabit).
     */
    fun koyuMuDakika(acilDk: Int, karanDk: Int, simdiDk: Int): Boolean {
        if (acilDk == karanDk) return false // boş aralık: hep açık
        val (a, k) = if (acilDk < karanDk) acilDk to karanDk else karanDk to acilDk
        return simdiDk < a || simdiDk >= k
    }

    /** Şu anki güneş-kaynaklı koyular kararı. */
    fun gunesKoyuMu(context: Context): Boolean {
        val simdi = java.util.Calendar.getInstance().let {
            it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
        }
        if (gunesKaynak(context) == GUNES_OZEL) {
            return koyuMuDakika(gunesAcilDk(context), gunesKaranDk(context), simdi)
        }
        // Astronomik: NamazVakti'nin aynı güneş hesabı — doğuş GUNES, batış AKSAM.
        val (acil, karan) = runCatching {
            val gun = NamazVakti.bugun(context)
            gun.dakika(NamazVakti.Vakit.GUNES) to gun.dakika(NamazVakti.Vakit.AKSAM)
        }.getOrDefault(-1 to -1)
        return if (acil >= 0 && karan > acil) {
            koyuMuDakika(acil, karan, simdi)
        } else {
            koyuMuDakika(420, 1140, simdi) // hesap tutmazsa sabit pencere
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v8.3 · Öneri 10 — MATERIAL YOU (dinamik renk)
    // ═══════════════════════════════════════════════════════════════
    //
    // Android 12+ cihazlarda duvar kâğıdından türetilen renk paleti.
    // Uygulama telefonun geri kalanıyla aynı aileden görünür.
    //
    // ── Neden isteğe bağlı ──
    // Uygulamanın kendi 9 teması özenle seçilmiş; dinamik renk onları
    // ezer. Varsayılan KAPALI, isteyen açar.
    //
    // ── Neden `applyToActivitiesIfAvailable` değil ──
    // O yöntem her Activity'ye otomatik uygular ve bizim
    // `setTheme(ThemeManager.styleFor())` çağrımızla çakışır. Bunun
    // yerine her Activity kendi setTheme'inden SONRA
    // [dinamikRengiUygula] çağırıyor — sıra bizde kalıyor.

    private const val KEY_DINAMIK = "dinamik_renk_v1"

    /** Cihaz dinamik rengi destekliyor mu? (Android 12+) */
    fun dinamikDesteklenir(): Boolean = runCatching {
        com.google.android.material.color.DynamicColors.isDynamicColorAvailable()
    }.getOrDefault(false)

    fun dinamikAcik(context: Context): Boolean =
        dinamikDesteklenir() && prefs(context).getBoolean(KEY_DINAMIK, false)

    fun dinamikAcik(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean(KEY_DINAMIK, deger).apply()
    }

    /**
     * Seçili temanın üstüne duvar kâğıdı paletini uygular.
     *
     * Activity'de `setTheme(styleFor(this))` ve `applyAccent(this)`
     * çağrılarından SONRA, `super.onCreate()`'ten ÖNCE çağrılmalı.
     */
    fun dinamikRengiUygula(activity: android.app.Activity) {
        if (!dinamikAcik(activity)) return
        runCatching {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(activity)
        }.onFailure { android.util.Log.w("ThemeManager", "Dinamik renk uygulanamadı", it) }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A4 — YAZI KARAKTERİ KATMANI
    // ═══════════════════════════════════════════════════════════════
    //
    // Tüm gövde metinleri tema fontFamily'sinden gelir. Bu katman
    // applyStyle(force=true) ile basılır; XML'de @font/poppins_* ile
    // AÇIKÇA işaretlenmiş başlıklar marka yüzünde kalır (tuşlar, hero
    // sayılar — bilinçli, marka tutarlılığı).

    private const val KEY_YAZI_TUR = "yazi_turu_v1"

    const val YAZI_POPPINS = 0
    const val YAZI_ATKINSON = 1
    const val YAZI_LORA = 2

    fun yaziTur(context: Context): Int =
        prefs(context).getInt(KEY_YAZI_TUR, YAZI_POPPINS).coerceIn(0, 2)

    fun yaziTur(context: Context, tur: Int) {
        prefs(context).edit().putInt(KEY_YAZI_TUR, tur.coerceIn(0, 2)).apply()
    }

    /** Seçime denk gelen tema katmanı (0 = katman istemez). */
    fun yaziKaplamaRes(context: Context): Int = when (yaziTur(context)) {
        YAZI_ATKINSON -> R.style.FontKaplamasiAtkinson
        YAZI_LORA -> R.style.FontKaplamasiLora
        else -> 0
    }

    // ---------------- Kendi rengini seç (vurgu katmanı) ----------------

    data class Accent(
        val title: String,
        val styleRes: Int,
        val swatch: Int
    )

    val accents = listOf(
        Accent("Karamel", R.style.Theme_GunlukAsistan_AccentKaramel, 0xFFB08968.toInt()),
        Accent("Vişne", R.style.Theme_GunlukAsistan_AccentVisne, 0xFFA4283F.toInt()),
        Accent("Gül Kurusu", R.style.Theme_GunlukAsistan_AccentGulKurusu, 0xFFC26A8D.toInt()),
        Accent("Lavanta", R.style.Theme_GunlukAsistan_AccentLavanta, 0xFF8A7BD8.toInt()),
        Accent("İndigo", R.style.Theme_GunlukAsistan_AccentIndigo, 0xFF4C5FCE.toInt()),
        Accent("Deniz", R.style.Theme_GunlukAsistan_AccentDeniz, 0xFF1E86B0.toInt()),
        Accent("Nane", R.style.Theme_GunlukAsistan_AccentNane, 0xFF1F9E8A.toInt()),
        Accent("Zümrüt", R.style.Theme_GunlukAsistan_AccentZumrut, 0xFF2F7D46.toInt()),
        Accent("Limon", R.style.Theme_GunlukAsistan_AccentLimon, 0xFF97A822.toInt()),
        Accent("Mango", R.style.Theme_GunlukAsistan_AccentMango, 0xFFD98A1F.toInt()),
        Accent("Mercan", R.style.Theme_GunlukAsistan_AccentMercan, 0xFFE0674F.toInt()),
        Accent("Gece", R.style.Theme_GunlukAsistan_AccentGece, 0xFF7FA6E8.toInt()),
        // v10.11 · ULTRA-30 A6: erişilebilir vurgu (Okabe-Ito)
        Accent("Erişim", R.style.Theme_GunlukAsistan_AccentErisim, 0xFF0072B2.toInt())
    )

    fun accentIndex(context: Context): Int = prefs(context).getInt(KEY_ACCENT, -1)

    fun selectAccent(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_ACCENT, index).apply()
    }

    /** Seçili vurguyu mevcut temanın üstüne uygular (Activity'de setTheme'den hemen sonra çağrılır). */
    fun applyAccent(context: Context) {
        // v10.11 · ULTRA-30 A4: yazı karakteri katmanı önce basılır;
        // vurgu onun üstüne gelir (aynı attr çakışmaz, biri font diğeri renk)
        runCatching {
            val kaplama = yaziKaplamaRes(context)
            if (kaplama != 0) context.theme.applyStyle(kaplama, true)
        }
        val i = accentIndex(context)
        if (i in accents.indices) {
            context.theme.applyStyle(accents[i].styleRes, true)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun selected(context: Context): Int =
        prefs(context).getInt(KEY, 0).coerceIn(specs.indices)

    fun styleFor(context: Context): Int = specs[selected(context)].styleRes

    fun select(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY, index.coerceIn(specs.indices)).apply()
    }
}
