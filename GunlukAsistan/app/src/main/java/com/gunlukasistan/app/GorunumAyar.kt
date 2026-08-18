package com.gunlukasistan.app

import android.content.Context

/**
 * v8.2 — Görsel davranış ayarları (Grup A ortak deposu).
 *
 * ── Neden ayrı bir sınıf ──
 * Animasyon ve titreşim herkesin sevdiği şeyler değil: pil ömrü,
 * hareket duyarlılığı (vestibüler rahatsızlık) veya sadece kişisel
 * tercih yüzünden kapatmak isteyen olur. Android'in kendi
 * "animasyonları kaldır" erişilebilirlik ayarı da var; onu da
 * dinliyoruz ([hareketAzalt]).
 *
 * ── Neden Store içine konmadı ──
 * `Store.kt` 2600 satır ve JSON yedeğe giren veri modelini tutuyor.
 * Bunlar davranış tercihi; `PrefYedek` üzerinden yedeğe giriyorlar
 * (v7.98'deki modül listesine eklendi).
 */
object GorunumAyar {

    private const val PREF = "gunluk_asistan_gorunum"

    private const val K_ANIMASYON = "animasyon_acik"
    private const val K_HAPTIK = "haptik_acik"
    private const val K_LISTE_ANIM = "liste_animasyonu"
    private const val K_SAYAC_ANIM = "sayi_animasyonu"
    private const val K_KAYDIRMA = "kaydirma_jesti"
    private const val K_YOGUNLUK = "yogunluk"      // 0 sıkı · 1 normal · 2 rahat
    private const val K_YAZI_OLCEK = "yazi_olcek"  // 0 küçük · 1 normal · 2 büyük · 3 çok büyük

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Animasyon ----------------

    /**
     * Ana anahtar. Sistemin "animasyonları kaldır" ayarı açıksa
     * kullanıcı burada açık bırakmış olsa bile kapalı sayılır —
     * erişilebilirlik tercihi uygulamanın tercihini ezer.
     */
    fun animasyonAcik(c: Context): Boolean =
        p(c).getBoolean(K_ANIMASYON, true) && !hareketAzalt(c)

    fun animasyonAcik(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_ANIMASYON, deger).apply()
    }

    /** Kullanıcının kendi tercihi (sistem ayarından bağımsız ham değer). */
    fun animasyonTercihi(c: Context): Boolean = p(c).getBoolean(K_ANIMASYON, true)

    /**
     * Android'in geliştirici/erişilebilirlik ayarında animasyon ölçeği
     * 0'a çekilmişse true. Sistem genelinde hareket istenmiyor demektir.
     */
    fun hareketAzalt(c: Context): Boolean = runCatching {
        val olcek = android.provider.Settings.Global.getFloat(
            c.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        olcek == 0f
    }.getOrDefault(false)

    /** Liste öğelerinin sırayla belirmesi (öneri 5). */
    fun listeAnimasyonu(c: Context): Boolean =
        animasyonAcik(c) && p(c).getBoolean(K_LISTE_ANIM, true)

    fun listeAnimasyonu(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_LISTE_ANIM, deger).apply()
    }

    /** Sayıların 0'dan hedefe sayması (öneri 3). */
    fun sayiAnimasyonu(c: Context): Boolean =
        animasyonAcik(c) && p(c).getBoolean(K_SAYAC_ANIM, true)

    fun sayiAnimasyonu(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_SAYAC_ANIM, deger).apply()
    }

    // ---------------- Dokunsal ----------------

    fun haptikAcik(c: Context): Boolean = p(c).getBoolean(K_HAPTIK, true)

    fun haptikAcik(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_HAPTIK, deger).apply()
    }

    // ---------------- Jest ----------------

    /** Kartlarda sola/sağa kaydırma (öneri 4). */
    fun kaydirmaJesti(c: Context): Boolean = p(c).getBoolean(K_KAYDIRMA, true)

    fun kaydirmaJesti(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_KAYDIRMA, deger).apply()
    }

    // ---------------- Yoğunluk ve yazı (öneri 27 hazırlığı) ----------------

    fun yogunluk(c: Context): Int = p(c).getInt(K_YOGUNLUK, 1).coerceIn(0, 2)

    fun yogunluk(c: Context, deger: Int) {
        p(c).edit().putInt(K_YOGUNLUK, deger.coerceIn(0, 2)).apply()
    }

    fun yaziOlcek(c: Context): Int = p(c).getInt(K_YAZI_OLCEK, 1).coerceIn(0, 3)

    fun yaziOlcek(c: Context, deger: Int) {
        p(c).edit().putInt(K_YAZI_OLCEK, deger.coerceIn(0, 3)).apply()
    }

    /** Yazı ölçeğinin çarpanı. 1.0 = sistem varsayılanı. */
    fun yaziCarpani(c: Context): Float = when (yaziOlcek(c)) {
        0 -> 0.88f
        2 -> 1.15f
        3 -> 1.32f
        else -> 1.0f
    }

    /** Kart aralığı çarpanı (öneri 27). */
    fun yogunlukCarpani(c: Context): Float = when (yogunluk(c)) {
        0 -> 0.7f    // sıkı — ekrana daha çok içerik
        2 -> 1.35f   // rahat — daha ferah
        else -> 1.0f
    }

    /**
     * v8.6 · Öneri 27 — Yazı ölçeğini Activity'ye uygular.
     *
     * ── Neden Configuration üzerinden ──
     * Tek tek `TextView.textSize` değiştirmek 71 layout ve 166 dosya
     * demekti. `Configuration.fontScale` tüm `sp` birimlerini bir
     * kerede ölçekliyor — XML'e hiç dokunmadan.
     *
     * ── Sistem ayarıyla ilişkisi ──
     * Kullanıcının telefon genelindeki yazı boyutu ayarı korunuyor;
     * bizim çarpanımız onun ÜSTÜNE biniyor. Sistemde büyük yazı
     * seçmiş biri buradan "büyük" seçerse daha da büyür — istediği
     * bu zaten.
     *
     * Activity'nin `attachBaseContext` metodundan çağrılmalı.
     */
    fun yaziOlcegiUygula(taban: Context): Context {
        return runCatching {
            val carpan = yaziCarpani(taban)
            if (carpan == 1.0f) return taban
            val yapilandirma = android.content.res.Configuration(taban.resources.configuration)
            yapilandirma.fontScale = taban.resources.configuration.fontScale * carpan
            taban.createConfigurationContext(yapilandirma)
        }.getOrDefault(taban)
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.49 — 8 AŞIRI İŞLEVSEL GÖRÜNÜM AYARI (#2, #3, #5, #6, #7, #8, #9, #10)
    // ═══════════════════════════════════════════════════════════════

    private const val K_KART_MODU = "kart_modu"            // 0: Tam, 1: Kompakt, 2: Minimal Satır
    private const val K_ONCELIK_VURGU = "oncelik_vurgu"    // 0: Nokta, 1: Şerit, 2: Zemin Parlaması
    private const val K_FONT_SABLON = "font_sablon"        // 0: Poppins, 1: Atkinson, 2: Lora
    private const val K_YAZI_YUZDE = "yazi_yuzde"          // 80..150
    private const val K_SATIR_NEFESI = "satir_nefesi"      // 0..16 dp
    private const val K_ZEN_ODAK = "zen_odak"              // false: Canlı Kadran, true: Zen Odak
    private const val K_ACILIS_EKRAN = "acilis_ekran"      // 0: Ana, 1: Görev, 2: Sayaç, 3: Ajan
    private const val K_HERO_GIZLI = "hero_gizli"          // false: Açık, true: Gizli
    private const val K_FAB_ISLEV = "fab_islev"            // 0: Görev Ekle, 1: 25dk Odak, 2: Komut, 3: Ajan
    private const val K_YUZEN_SERIT = "yuzen_serit"        // false: Kapalı, true: Açık

    // 1. #2 Yoğunluk / Zihinsel Ferahlık Seçici
    fun kartModuSinirla(m: Int): Int = m.coerceIn(0, 2)
    fun kartModu(c: Context): Int = kartModuSinirla(p(c).getInt(K_KART_MODU, 0))
    fun setKartModu(c: Context, m: Int) = p(c).edit().putInt(K_KART_MODU, kartModuSinirla(m)).apply()

    // 2. #3 Öncelik Vurgu Seviyesi
    fun oncelikVurguSinirla(v: Int): Int = v.coerceIn(0, 2)
    fun oncelikVurgu(c: Context): Int = oncelikVurguSinirla(p(c).getInt(K_ONCELIK_VURGU, 1))
    fun setOncelikVurgu(c: Context, v: Int) = p(c).edit().putInt(K_ONCELIK_VURGU, oncelikVurguSinirla(v)).apply()

    // 3. #5 Görevine Göre Tipografi Vitrini
    fun fontSablonSinirla(f: Int): Int = f.coerceIn(0, 2)
    fun fontSablon(c: Context): Int = fontSablonSinirla(p(c).getInt(K_FONT_SABLON, 0))
    fun setFontSablon(c: Context, f: Int) = p(c).edit().putInt(K_FONT_SABLON, fontSablonSinirla(f)).apply()

    // 4. #6 Serbest Yazı Ölçeği & Satır Nefesi Atölyesi
    fun yaziYuzdesiSinirla(y: Int): Int = y.coerceIn(80, 150)
    fun yaziYuzdesi(c: Context): Int = yaziYuzdesiSinirla(p(c).getInt(K_YAZI_YUZDE, 100))
    fun setYaziYuzdesi(c: Context, y: Int) = p(c).edit().putInt(K_YAZI_YUZDE, yaziYuzdesiSinirla(y)).apply()
    fun satirNefesiDpSinirla(n: Int): Int = n.coerceIn(0, 16)
    fun satirNefesiDp(c: Context): Int = satirNefesiDpSinirla(p(c).getInt(K_SATIR_NEFESI, 6))
    fun setSatirNefesiDp(c: Context, n: Int) = p(c).edit().putInt(K_SATIR_NEFESI, satirNefesiDpSinirla(n)).apply()

    // 5. #7 Zen Odak vs Canlı Kadran Modu
    fun zenOdakMi(c: Context): Boolean = p(c).getBoolean(K_ZEN_ODAK, false)
    fun setZenOdak(c: Context, zen: Boolean) = p(c).edit().putBoolean(K_ZEN_ODAK, zen).apply()

    // 6. #8 Varsayılan Açılış Ekranı & Kokpit Gizlilik Kontrolü
    fun acilisEkranSinirla(id: Int): Int = id.coerceIn(0, 6)
    fun acilisEkran(c: Context): Int = acilisEkranSinirla(p(c).getInt(K_ACILIS_EKRAN, 0))
    fun setAcilisEkran(c: Context, id: Int) = p(c).edit().putInt(K_ACILIS_EKRAN, acilisEkranSinirla(id)).apply()
    fun acilisEkranAd(c: Context): String = when (acilisEkran(c)) {
        1 -> "✅ Görevler"
        2 -> "⏱️ Sayaç"
        3 -> "🤖 Asistan"
        4 -> "☀️ Bugün / Günün Akışı"
        5 -> "📋 Vakit Planı"
        6 -> "📊 İlerleme"
        else -> "🏠 Ana Ekran (Varsayılan)"
    }
    fun heroGizliMi(c: Context): Boolean = p(c).getBoolean(K_HERO_GIZLI, false)
    fun setHeroGizli(c: Context, gizli: Boolean) = p(c).edit().putBoolean(K_HERO_GIZLI, gizli).apply()

    // 7. #9 Akıllı FAB & Hızlı Buton Özelleştirmesi
    fun fabIslevSinirla(i: Int): Int = i.coerceIn(0, 3)
    fun fabIslev(c: Context): Int = fabIslevSinirla(p(c).getInt(K_FAB_ISLEV, 0))
    fun setFabIslev(c: Context, i: Int) = p(c).edit().putInt(K_FAB_ISLEV, fabIslevSinirla(i)).apply()

    // v11.01 — Gün Serisi Yazısı Açılışta Göster / Sonra Gizle (4 Saniyede Kaybolsun)
    const val GUN_SERISI_GIZLEME_SURESI_MS = 4000L
    private const val K_GUN_SERISI_OTO_GIZLE = "gun_serisi_oto_gizle_v1"

    fun isGunSerisiOtoGizle(c: Context? = null): Boolean {
        if (c == null) return true
        return try { p(c).getBoolean(K_GUN_SERISI_OTO_GIZLE, true) } catch (_: Exception) { true }
    }

    fun setGunSerisiOtoGizle(c: Context? = null, acik: Boolean) {
        if (c == null) return
        try { p(c).edit().putBoolean(K_GUN_SERISI_OTO_GIZLE, acik).apply() } catch (_: Exception) {}
    }

    fun gunSerisiOtoGizleDurumMetni(c: Context? = null): Pair<String, String> {
        val acik = isGunSerisiOtoGizle(c)
        return if (acik) {
            Pair(
                "🔥 Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle (AÇIK)",
                "AÇIK — Alttaki 'Gün seriniz: X gün güvende' yazısı açılışta görünüp 4 saniye sonra otomatik kaybolur."
            )
        } else {
            Pair(
                "🔥 Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle (KAPALI)",
                "KAPALI — 'Gün seriniz' yazısı altta sürekli görünür kalır."
            )
        }
    }

    // 8. #10 Yüzen Canlı Durum Şeridi (Mini Status Bar)
    fun yuzenSeritAcik(c: Context? = null): Boolean {
        if (c == null) return true
        return try { p(c).getBoolean(K_YUZEN_SERIT, true) } catch (_: Exception) { true }
    }
    fun setYuzenSeritAcik(c: Context, acik: Boolean) = p(c).edit().putBoolean(K_YUZEN_SERIT, acik).apply()

    /** v10.49: Yüzen canlı şeritte gösterilecek anlık durum metni (saf üretici). */
    fun yuzenSeritMetniUret(sayacCalisiyor: Boolean, kalanMs: Long, seri: Int): String {
        if (sayacCalisiyor) {
            val kalan = kalanMs.coerceAtLeast(0L)
            val dk = (kalan / 60_000L).toInt()
            val sn = ((kalan % 60_000L) / 1000L).toInt()
            return "⏱ Odak: %02d:%02d kaldı".format(dk, sn)
        }
        if (seri > 0) {
            return "🔥 Gün seriniz: $seri gün güvende"
        }
        return "🤖 AI Otopilot & Ajan Aktif"
    }

    /** v10.49: Yüzen canlı şeritte gösterilecek anlık durum metni. */
    fun yuzenSeritMetni(c: Context): String {
        val sayacCalisiyor = TimerEngine.isRunning(c)
        val kalanMs = if (sayacCalisiyor) TimerEngine.remainingMs(c) else 0L
        val seri = Store.streakInfo(c).first
        return yuzenSeritMetniUret(sayacCalisiyor, kalanMs, seri)
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.51 — SIFIRDAN MİNİMALİST & MODERN ARAYÜZ DEVRİMİ (TASARIM SİSTEMİ v2)
    // ═══════════════════════════════════════════════════════════════

    private const val K_TASARIM_DILI_V2 = "tasarim_dili_v2"
    private const val K_ALT_NAV_INCE = "alt_nav_ince"
    private const val K_OZET_SERID_MODU = "ozet_serid_modu"
    private const val K_TEK_AKI_KARTI = "tek_akis_karti"
    private const val K_KOMPAKT_KONU = "kompakt_konu"
    private const val K_PLAN_HERO = "plan_hero"
    private const val K_SAYAC_ALT_MENU = "sayac_alt_menu"

    fun tasarimDiliV2(c: Context): Boolean = p(c).getBoolean(K_TASARIM_DILI_V2, true)
    fun setTasarimDiliV2(c: Context, v: Boolean) = p(c).edit().putBoolean(K_TASARIM_DILI_V2, v).apply()

    fun altNavInce(c: Context): Boolean = p(c).getBoolean(K_ALT_NAV_INCE, true)
    fun setAltNavInce(c: Context, v: Boolean) = p(c).edit().putBoolean(K_ALT_NAV_INCE, v).apply()

    fun ozetSeridModu(c: Context): Boolean = p(c).getBoolean(K_OZET_SERID_MODU, true)
    fun setOzetSeridModu(c: Context, v: Boolean) = p(c).edit().putBoolean(K_OZET_SERID_MODU, v).apply()

    fun tekAkisKarti(c: Context): Boolean = p(c).getBoolean(K_TEK_AKI_KARTI, true)
    fun setTekAkisKarti(c: Context, v: Boolean) = p(c).edit().putBoolean(K_TEK_AKI_KARTI, v).apply()

    fun kompaktKonu(c: Context): Boolean = p(c).getBoolean(K_KOMPAKT_KONU, true)
    fun setKompaktKonu(c: Context, v: Boolean) = p(c).edit().putBoolean(K_KOMPAKT_KONU, v).apply()

    fun planHeroModu(c: Context): Boolean = p(c).getBoolean(K_PLAN_HERO, true)
    fun setPlanHeroModu(c: Context, v: Boolean) = p(c).edit().putBoolean(K_PLAN_HERO, v).apply()

    fun sayacAltMenu(c: Context): Boolean = p(c).getBoolean(K_SAYAC_ALT_MENU, true)
    fun setSayacAltMenu(c: Context, v: Boolean) = p(c).edit().putBoolean(K_SAYAC_ALT_MENU, v).apply()

    // ---------------- Yedek köprüsü ----------------

    fun disaAktar(c: Context): org.json.JSONObject = org.json.JSONObject().apply {
        val s = p(c)
        put(K_ANIMASYON, s.getBoolean(K_ANIMASYON, true))
        put(K_HAPTIK, s.getBoolean(K_HAPTIK, true))
        put(K_LISTE_ANIM, s.getBoolean(K_LISTE_ANIM, true))
        put(K_SAYAC_ANIM, s.getBoolean(K_SAYAC_ANIM, true))
        put(K_KAYDIRMA, s.getBoolean(K_KAYDIRMA, true))
        put(K_YOGUNLUK, s.getInt(K_YOGUNLUK, 1))
        put(K_YAZI_OLCEK, s.getInt(K_YAZI_OLCEK, 1))
        put(K_KART_MODU, s.getInt(K_KART_MODU, 0))
        put(K_ONCELIK_VURGU, s.getInt(K_ONCELIK_VURGU, 1))
        put(K_FONT_SABLON, s.getInt(K_FONT_SABLON, 0))
        put(K_YAZI_YUZDE, s.getInt(K_YAZI_YUZDE, 100))
        put(K_SATIR_NEFESI, s.getInt(K_SATIR_NEFESI, 6))
        put(K_ZEN_ODAK, s.getBoolean(K_ZEN_ODAK, false))
        put(K_ACILIS_EKRAN, s.getInt(K_ACILIS_EKRAN, 0))
        put(K_HERO_GIZLI, s.getBoolean(K_HERO_GIZLI, false))
        put(K_FAB_ISLEV, s.getInt(K_FAB_ISLEV, 0))
        put(K_YUZEN_SERIT, s.getBoolean(K_YUZEN_SERIT, true))
        put(K_TASARIM_DILI_V2, s.getBoolean(K_TASARIM_DILI_V2, true))
        put(K_ALT_NAV_INCE, s.getBoolean(K_ALT_NAV_INCE, true))
        put(K_OZET_SERID_MODU, s.getBoolean(K_OZET_SERID_MODU, true))
        put(K_TEK_AKI_KARTI, s.getBoolean(K_TEK_AKI_KARTI, true))
        put(K_KOMPAKT_KONU, s.getBoolean(K_KOMPAKT_KONU, true))
        put(K_PLAN_HERO, s.getBoolean(K_PLAN_HERO, true))
        put(K_SAYAC_ALT_MENU, s.getBoolean(K_SAYAC_ALT_MENU, true))
    }

    fun iceAktar(c: Context, j: org.json.JSONObject?) {
        j ?: return
        val e = p(c).edit()
        if (j.has(K_ANIMASYON)) e.putBoolean(K_ANIMASYON, j.optBoolean(K_ANIMASYON, true))
        if (j.has(K_HAPTIK)) e.putBoolean(K_HAPTIK, j.optBoolean(K_HAPTIK, true))
        if (j.has(K_LISTE_ANIM)) e.putBoolean(K_LISTE_ANIM, j.optBoolean(K_LISTE_ANIM, true))
        if (j.has(K_SAYAC_ANIM)) e.putBoolean(K_SAYAC_ANIM, j.optBoolean(K_SAYAC_ANIM, true))
        if (j.has(K_KAYDIRMA)) e.putBoolean(K_KAYDIRMA, j.optBoolean(K_KAYDIRMA, true))
        if (j.has(K_YOGUNLUK)) e.putInt(K_YOGUNLUK, j.optInt(K_YOGUNLUK, 1))
        if (j.has(K_YAZI_OLCEK)) e.putInt(K_YAZI_OLCEK, j.optInt(K_YAZI_OLCEK, 1))
        if (j.has(K_KART_MODU)) e.putInt(K_KART_MODU, j.optInt(K_KART_MODU, 0))
        if (j.has(K_ONCELIK_VURGU)) e.putInt(K_ONCELIK_VURGU, j.optInt(K_ONCELIK_VURGU, 1))
        if (j.has(K_FONT_SABLON)) e.putInt(K_FONT_SABLON, j.optInt(K_FONT_SABLON, 0))
        if (j.has(K_YAZI_YUZDE)) e.putInt(K_YAZI_YUZDE, j.optInt(K_YAZI_YUZDE, 100))
        if (j.has(K_SATIR_NEFESI)) e.putInt(K_SATIR_NEFESI, j.optInt(K_SATIR_NEFESI, 6))
        if (j.has(K_ZEN_ODAK)) e.putBoolean(K_ZEN_ODAK, j.optBoolean(K_ZEN_ODAK, false))
        if (j.has(K_ACILIS_EKRAN)) e.putInt(K_ACILIS_EKRAN, j.optInt(K_ACILIS_EKRAN, 0))
        if (j.has(K_HERO_GIZLI)) e.putBoolean(K_HERO_GIZLI, j.optBoolean(K_HERO_GIZLI, false))
        if (j.has(K_FAB_ISLEV)) e.putInt(K_FAB_ISLEV, j.optInt(K_FAB_ISLEV, 0))
        if (j.has(K_YUZEN_SERIT)) e.putBoolean(K_YUZEN_SERIT, j.optBoolean(K_YUZEN_SERIT, true))
        if (j.has(K_TASARIM_DILI_V2)) e.putBoolean(K_TASARIM_DILI_V2, j.optBoolean(K_TASARIM_DILI_V2, true))
        if (j.has(K_ALT_NAV_INCE)) e.putBoolean(K_ALT_NAV_INCE, j.optBoolean(K_ALT_NAV_INCE, true))
        if (j.has(K_OZET_SERID_MODU)) e.putBoolean(K_OZET_SERID_MODU, j.optBoolean(K_OZET_SERID_MODU, true))
        if (j.has(K_TEK_AKI_KARTI)) e.putBoolean(K_TEK_AKI_KARTI, j.optBoolean(K_TEK_AKI_KARTI, true))
        if (j.has(K_KOMPAKT_KONU)) e.putBoolean(K_KOMPAKT_KONU, j.optBoolean(K_KOMPAKT_KONU, true))
        if (j.has(K_PLAN_HERO)) e.putBoolean(K_PLAN_HERO, j.optBoolean(K_PLAN_HERO, true))
        if (j.has(K_SAYAC_ALT_MENU)) e.putBoolean(K_SAYAC_ALT_MENU, j.optBoolean(K_SAYAC_ALT_MENU, true))
        e.apply()
    }

    // v11.07: Günün akışı vb. kartların boyut ölçeği (0: Kompakt %85, 1: Normal %100, 2: Geniş %115, 3: Devasa %130)
    fun kartBoyutuSinirla(i: Int): Int = i.coerceIn(0, 3)
    fun kartBoyutuOlcegi(c: Context): Int = kartBoyutuSinirla(p(c).getInt("kart_boyutu_olcegi", 1))
    fun setKartBoyutuOlcegi(c: Context, i: Int) = p(c).edit().putInt("kart_boyutu_olcegi", kartBoyutuSinirla(i)).apply()
    fun kartBoyutuOlcegiKatsayisi(c: Context): Float = when (kartBoyutuOlcegi(c)) {
        0 -> 0.85f // Kompakt
        2 -> 1.15f // Geniş
        3 -> 1.30f // Devasa
        else -> 1.0f // Normal (100%)
    }
    fun kartBoyutuAd(c: Context): String = when (kartBoyutuOlcegi(c)) {
        0 -> "Kompakt (%85)"
        2 -> "Geniş (%115)"
        3 -> "Devasa (%130)"
        else -> "Normal (%100)"
    }

    // v11.07: Ana Ekran Kartlarının Özel Sıralaması (Virgül ayraçlı ID listesi string olarak saklanır)
    fun anaEkranSiralama(c: Context): List<String> {
        val s = p(c).getString("ana_ekran_siralama", null) ?: return emptyList()
        return s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    fun setAnaEkranSiralama(c: Context, idler: List<String>) {
        p(c).edit().putString("ana_ekran_siralama", idler.joinToString(",")).apply()
    }
    fun anaEkranSirasiniSifirla(c: Context) {
        p(c).edit().remove("ana_ekran_siralama").apply()
    }
}
