package com.gunlukasistan.app

import android.content.Context

/**
 * v10.24 — İleri Sayım (kullanıcı isteği):
 *
 * "Zamanlayıcıda kronometre ve geri sayımın ortasına ileri sayım yeri ekle;
 *  geri sayım gibi olsun ama ileri doğru saysın. Başlatınca durdurunca ders
 *  saati yerine dakikayı eklesin. Bekle dersem bekletsin, dursun orda;
 *  ekranı kapatsam bile sonra devam ettirebileyim."
 *
 * Tasarım damga-temellidir: durum SharedPreferences'ta yaşar; ekran kapansa,
 * süreç ölse bile biriken süre ve çalışma damgası korunur. Geri sayım gibi
 * arka planda da saymaya devam eder (duvar saati). Motor (TimerEngine) ve
 * bildirim altyapısına DOKUNMAZ — v10.19 sayaç dersi: bu bölge kırılgan.
 *
 * Kayıt kanalı v10.19 manuel odakla birebir ayndır (Store.addTodayFocusMinutes
 * + WidgetCommon.refreshAll) — çağrıyı TimerFragment yapar; burası yalnız
 * durum yönetir. Saf mantık üstte (JVM testli), depo altta.
 */
object IleriSayim {

    /** Tek oturum durumu: bekleyen birikim + çalışan bölümün başlangıcı. */
    data class Durum(
        val calisiyor: Boolean,
        val birikenMs: Long,
        val baslangicMs: Long
    )

    // ──────────────── Saf mantık (android YOK, JVM testli) ────────────────

    /**
     * Toplam geçen süre: biriken + (çalışıyorsa) canlı bölüm.
     * Saat kaymasında (simdi < baslangic) canlı bölüm 0'a kenetlenir —
     * asla negatif süre üretmez.
     */
    fun gecenMs(d: Durum, simdiMs: Long): Long =
        if (!d.calisiyor) d.birikenMs
        else d.birikenMs + (simdiMs - d.baslangicMs).coerceAtLeast(0L)

    /** Açık bir oturum var mı (çalışıyor ya da bekleyen birikim). */
    fun oturumVarMi(d: Durum): Boolean = d.calisiyor || d.birikenMs > 0L

    /**
     * Ana düğme geçişi (saf): çalışıyor→BEKLE (bölümü birikenine katlar),
     * değilse→BAŞLAT/DEVAM (yeni bölümü şimdi başlatır, birikim korunur).
     */
    fun anaDugmeGecis(d: Durum, simdiMs: Long): Durum =
        if (d.calisiyor) {
            d.copy(
                calisiyor = false,
                birikenMs = d.birikenMs + (simdiMs - d.baslangicMs).coerceAtLeast(0L),
                baslangicMs = 0L
            )
        } else {
            d.copy(calisiyor = true, baslangicMs = simdiMs)
        }

    /** Dakika döşemesi: 59 sn 999 ms → 0 dk; artık saniyeler kaydedilmez. */
    fun dakikayaDonustur(gecenMs: Long): Int = (gecenMs / 60_000L).toInt()

    /**
     * Tek oturumda 8 saatten uzunu büyük ihtimalle açık unutulmuş sayaçtır —
     * kaydetmeden önce kullanıcıya sorulur (yanlış dev kayıt koruması).
     */
    const val UZUN_OTURUM_DK: Int = 480

    fun onayGerekliMi(dakika: Int): Boolean = dakika > UZUN_OTURUM_DK

    // ──────────────── Geçmiş depo (v10.28 · öneri #62) ────────────────
    //
    // ileri_sayim_v1 "oturum durumu" olduğu için yedek DIŞINDA tutulur;
    // ama tamamlanmış oturum kayıtları kullanıcının birikimidir — ayrı
    // dosyada yaşar ve PrefYedek taramasına doğal olarak girer.

    private const val PREF_GECMIS = "ileri_sayim_gecmis_v1"
    private const val K_GECMIS = "oturumlar"

    /** Bitirilen oturumu kayda işler (gün sınırını aşıyorsa ikiye böler). */
    fun gecmiseIsle(ctx: Context, bitisMs: Long, dakika: Int) {
        runCatching {
            val simdi = System.currentTimeMillis()
            val liste = SurecPlan.kayitEkle(gecmis(ctx), bitisMs, dakika, simdi)
            gecmisPref(ctx).edit().putString(K_GECMIS, SurecPlan.jsonaYaz(liste)).apply()
        }
    }

    fun gecmis(ctx: Context): List<SurecPlan.Oturum> =
        runCatching { SurecPlan.jsondanOku(gecmisPref(ctx).getString(K_GECMIS, "[]")) }
            .getOrDefault(emptyList())

    /** Kadran alt satırı için: bugün toplamı. */
    fun bugunToplam(ctx: Context, simdi: Long = System.currentTimeMillis()): Int =
        SurecPlan.bugunToplam(gecmis(ctx), simdi)

    fun dunToplam(ctx: Context, simdi: Long = System.currentTimeMillis()): Int =
        SurecPlan.dunToplam(gecmis(ctx), simdi)

    private fun gecmisPref(ctx: Context) =
        ctx.getSharedPreferences(PREF_GECMIS, Context.MODE_PRIVATE)

    // ──────────────── Depo (SharedPreferences) ────────────────

    private const val PREF = "ileri_sayim_v1"
    private const val K_CALISIYOR = "calisiyor"
    private const val K_BIRIKEN = "biriken_ms"
    private const val K_BASLANGIC = "baslangic_ms"
    /** v10.26 (öneri #61): oturuma verilen isim — kadranda ve bitiş toastında. */
    private const val K_AD = "ad"

    /** Oturum adı üst sınırı (karakter). */
    const val AD_SINIR = 60

    /**
     * v10.26: isim girişini normalleştirir (saf — birim testli).
     * Kenar boşlukları atılır, iç boşluklar tekle iner, üst sınır kesilir.
     * Boşsa "" döner (isimsiz oturum = eski davranış).
     */
    fun adTemiz(girdi: String): String =
        girdi.trim().replace(Regex("\\s+"), " ").take(AD_SINIR).trim()

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun durum(c: Context): Durum = Durum(
        calisiyor = prefs(c).getBoolean(K_CALISIYOR, false),
        birikenMs = prefs(c).getLong(K_BIRIKEN, 0L),
        baslangicMs = prefs(c).getLong(K_BASLANGIC, 0L)
    )

    /** Zamanlayıcı açılırken ileri sayım modu seçilsin mi. */
    fun aktifMi(c: Context): Boolean = oturumVarMi(durum(c))

    fun calismakta(c: Context): Boolean = durum(c).calisiyor

    fun gecenSimdi(c: Context, simdiMs: Long): Long = gecenMs(durum(c), simdiMs)

    /** Bitir düğmesinin yazacağı dakika (henüz kaydedilmedi). */
    fun bekleyenDakika(c: Context, simdiMs: Long): Int =
        dakikayaDonustur(gecenSimdi(c, simdiMs))

    /** Başlat ⇄ Bekle ⇄ Devam — tek giriş noktası. */
    fun anaDugme(c: Context, simdiMs: Long) {
        val yeni = anaDugmeGecis(durum(c), simdiMs)
        prefs(c).edit()
            .putBoolean(K_CALISIYOR, yeni.calisiyor)
            .putLong(K_BIRIKEN, yeni.birikenMs)
            .putLong(K_BASLANGIC, yeni.baslangicMs)
            .apply()
    }

    /** v10.26: oturum ismi — yoksa boş metin. */
    fun ad(c: Context): String = prefs(c).getString(K_AD, "").orEmpty()

    /** v10.26: ismi normalleyip kaydeder; boş gelirse isim silinir. */
    fun adYaz(c: Context, girdi: String) {
        val temiz = adTemiz(girdi)
        prefs(c).edit().apply {
            if (temiz.isEmpty()) remove(K_AD) else putString(K_AD, temiz)
        }.apply()
    }

    /** Kayıt sonrası (ya da vazgeçte) tam sıfırlama. */
    fun sifirla(c: Context) {
        prefs(c).edit()
            .putBoolean(K_CALISIYOR, false)
            .putLong(K_BIRIKEN, 0L)
            .putLong(K_BASLANGIC, 0L)
            .remove(K_AD)
            .apply()
    }
}
