package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.46 — Namaz vakitlerine göre gün planlama.
 *
 * ── Kullanıcının isteği ──
 * "Namaz aralarında işlerimi değiştireyim farklılaştırayım"
 *
 * ── Fikir ──
 * Gün, namaz vakitleriyle doğal olarak 5 dilime bölünür. Her dilimin
 * kendine özgü bir enerji düzeyi ve süresi vardır:
 *
 *   İmsak → Güneş    : Kısa, sessiz, zihin en açık  → Ezber, tekrar
 *   Güneş → Öğle     : Uzun, yüksek enerji          → Derin çalışma
 *   Öğle  → İkindi   : Orta, tokluk/yorgunluk       → Uygulama, pratik
 *   İkindi→ Akşam    : Orta, ikinci rüzgâr          → Ders, video
 *   Akşam → Yatsı    : Kısa, aile vakti             → Hafif tekrar
 *   Yatsı → İmsak    : Serbest                      → Planlama, okuma
 *
 * Bu sınıf her dilime kullanıcının atadığı işleri saklar ve o an hangi
 * dilimde olduğunu bilerek "şimdi şunu yap" önerisi üretir.
 */
object NamazPlan {

    private const val TAG = "NamazPlan"
    private const val PREF = "namaz_plan_v1"
    private const val K_PLAN = "plan_json"

    // ═══════════════════════════════════════════════════════════════
    // DİLİM MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Gün dilimi — iki vakit arasındaki zaman.
     *
     * @param baslangic dilimi başlatan vakit
     * @param bitis dilimi bitiren vakit
     * @param varsayilanIsRes önerilen iş türü
     */
    enum class Dilim(
        val anahtar: String,
        val baslangic: NamazVakti.Vakit,
        val bitis: NamazVakti.Vakit,
        val adRes: Int,
        val varsayilanIsRes: Int,
        val emoji: String
    ) {
        SABAH(
            "sabah", NamazVakti.Vakit.IMSAK, NamazVakti.Vakit.GUNES,
            R.string.np_d_sabah, R.string.np_i_sabah, "🌙"
        ),
        KUSLUK(
            "kusluk", NamazVakti.Vakit.GUNES, NamazVakti.Vakit.OGLE,
            R.string.np_d_kusluk, R.string.np_i_kusluk, "🌅"
        ),
        OGLEDEN(
            "ogleden", NamazVakti.Vakit.OGLE, NamazVakti.Vakit.IKINDI,
            R.string.np_d_ogleden, R.string.np_i_ogleden, "☀️"
        ),
        IKINDIDEN(
            "ikindiden", NamazVakti.Vakit.IKINDI, NamazVakti.Vakit.AKSAM,
            R.string.np_d_ikindiden, R.string.np_i_ikindiden, "🌤"
        ),
        AKSAMDAN(
            "aksamdan", NamazVakti.Vakit.AKSAM, NamazVakti.Vakit.YATSI,
            R.string.np_d_aksamdan, R.string.np_i_aksamdan, "🌆"
        ),
        GECE(
            "gece", NamazVakti.Vakit.YATSI, NamazVakti.Vakit.IMSAK,
            R.string.np_d_gece, R.string.np_i_gece, "🌃"
        );
    }

    /** Bir dilime atanmış iş. */
    /**
     * Bir dilime atanmış iş.
     *
     * v7.64: süre, öncelik, sıra ve hatırlatma alanları eklendi.
     * Eski kayıtlarda bu alanlar yok — varsayılanlarla okunur.
     */
    data class Is(
        val id: Long,
        var dilim: String,
        var metin: String,
        var tamamlandi: Boolean = false,
        /** Hangi gün tamamlandı (yyyyMMdd) — her gün sıfırlanır. */
        var tamamGun: String = "",
        /** v7.64: tahmini süre (dakika). 0 = belirtilmemiş. */
        var sureDk: Int = 0,
        /** v7.64: 0 düşük · 1 normal · 2 öncelikli. */
        var oncelik: Int = 1,
        /** v7.64: dilim içi sıralama. */
        var sira: Int = 0,
        /** v7.64: vakit bildiriminde öne çıksın mı? */
        var hatirlat: Boolean = false
    ) {
        val oncelikSimgesi: String
            get() = when (oncelik) {
                2 -> "🔴"
                0 -> "🔵"
                else -> ""
            }
    }

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugunKey(): String = java.text.SimpleDateFormat(
        "yyyyMMdd", java.util.Locale.US
    ).format(java.util.Date())

    fun isleriYukle(context: Context): MutableList<Is> {
        val liste = mutableListOf<Is>()
        try {
            val dizi = JSONArray(prefs(context).getString(K_PLAN, "[]") ?: "[]")
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Is(
                        id = o.optLong("id"),
                        dilim = o.optString("dilim"),
                        metin = o.optString("metin"),
                        tamamlandi = o.optBoolean("tamam"),
                        tamamGun = o.optString("gun"),
                        sureDk = o.optInt("sure", 0),
                        oncelik = o.optInt("onc", 1),
                        sira = o.optInt("sira", 0),
                        hatirlat = o.optBoolean("hat", false)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Plan okunamadı", e)
        }
        // Dünden kalan işaretleri temizle — plan her gün tekrarlanır
        val bugun = bugunKey()
        liste.forEach { if (it.tamamlandi && it.tamamGun != bugun) {
            it.tamamlandi = false; it.tamamGun = ""
        } }
        return liste
    }

    fun isleriKaydet(context: Context, liste: List<Is>) {
        val dizi = JSONArray()
        liste.forEach { i ->
            dizi.put(
                JSONObject()
                    .put("id", i.id)
                    .put("dilim", i.dilim)
                    .put("metin", i.metin)
                    .put("tamam", i.tamamlandi)
                    .put("gun", i.tamamGun)
                    .put("sure", i.sureDk)
                    .put("onc", i.oncelik)
                    .put("sira", i.sira)
                    .put("hat", i.hatirlat)
            )
        }
        prefs(context).edit().putString(K_PLAN, dizi.toString()).apply()
        // v7.65: plan degisince ana ekrandaki widget da guncellensin
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Widget tazelenemedi", e)
        }
    }

    /**
     * v7.64: Dilimdeki isler — once tamamlanmamislar, sonra oncelik,
     * sonra kullanicinin verdigi sira.
     */
    fun dilimIsleri(context: Context, dilim: Dilim): List<Is> =
        isleriYukle(context)
            .filter { it.dilim == dilim.anahtar }
            .sortedWith(
                compareBy({ it.tamamlandi }, { -it.oncelik }, { it.sira }, { it.id })
            )

    /** v7.64: Detayli is ekleme. Eski cagrilar varsayilanlarla calisir. */
    fun isEkle(
        context: Context,
        dilim: Dilim,
        metin: String,
        sureDk: Int = 0,
        oncelik: Int = 1,
        hatirlat: Boolean = false
    ): Is? {
        if (metin.isBlank()) return null
        val liste = isleriYukle(context)
        val enBuyukSira = liste.filter { it.dilim == dilim.anahtar }
            .maxOfOrNull { it.sira } ?: 0
        val yeni = Is(
            id = System.currentTimeMillis() + liste.size,
            dilim = dilim.anahtar,
            metin = metin.trim().take(120),
            sureDk = sureDk.coerceIn(0, 600),
            oncelik = oncelik.coerceIn(0, 2),
            sira = enBuyukSira + 1,
            hatirlat = hatirlat
        )
        liste.add(yeni)
        isleriKaydet(context, liste)
        return yeni
    }

    /** v7.64: Var olan isin tum alanlarini gunceller. */
    fun isGuncelle(
        context: Context,
        isId: Long,
        metin: String,
        sureDk: Int,
        oncelik: Int,
        hatirlat: Boolean,
        dilim: Dilim? = null
    ) {
        if (metin.isBlank()) return
        val liste = isleriYukle(context)
        liste.firstOrNull { it.id == isId }?.apply {
            this.metin = metin.trim().take(120)
            this.sureDk = sureDk.coerceIn(0, 600)
            this.oncelik = oncelik.coerceIn(0, 2)
            this.hatirlat = hatirlat
            dilim?.let { this.dilim = it.anahtar }
        }
        isleriKaydet(context, liste)
    }

    /** v7.64: Isi listede yukari/asagi tasir. */
    fun isTasi(context: Context, isId: Long, yukari: Boolean) {
        val liste = isleriYukle(context)
        val bu = liste.firstOrNull { it.id == isId } ?: return
        val kardesler = liste.filter { it.dilim == bu.dilim }
            .sortedWith(compareBy({ -it.oncelik }, { it.sira }, { it.id }))
        val indeks = kardesler.indexOfFirst { it.id == isId }
        val hedef = if (yukari) indeks - 1 else indeks + 1
        if (indeks < 0 || hedef < 0 || hedef >= kardesler.size) return
        val digeri = kardesler[hedef]
        val gecici = bu.sira
        bu.sira = digeri.sira
        digeri.sira = gecici
        // Ayni siraya dusmesinler
        if (bu.sira == digeri.sira) {
            if (yukari) bu.sira -= 1 else bu.sira += 1
        }
        isleriKaydet(context, liste)
    }

    /** v7.64: Isi cogaltir. */
    fun isCogalt(context: Context, isId: Long) {
        val liste = isleriYukle(context)
        val bu = liste.firstOrNull { it.id == isId } ?: return
        liste.add(
            bu.copy(
                id = System.currentTimeMillis() + liste.size,
                tamamlandi = false,
                tamamGun = "",
                sira = bu.sira + 1
            )
        )
        isleriKaydet(context, liste)
    }

    /** v7.64: Bir dilimdeki tamamlanmis isleri siler. */
    fun bitenleriTemizle(context: Context, dilim: Dilim): Int {
        val liste = isleriYukle(context)
        val silinecek = liste.filter { it.dilim == dilim.anahtar && it.tamamlandi }
        if (silinecek.isEmpty()) return 0
        isleriKaydet(context, liste - silinecek.toSet())
        return silinecek.size
    }

    /** v7.64: Dilimdeki toplam planlanmis sure (dakika). */
    fun dilimPlanliSure(context: Context, dilim: Dilim): Int =
        dilimIsleri(context, dilim).filter { !it.tamamlandi }.sumOf { it.sureDk }

    fun isSil(context: Context, isId: Long) {
        isleriKaydet(context, isleriYukle(context).filterNot { it.id == isId })
    }

    fun isTamamla(context: Context, isId: Long): Boolean {
        val liste = isleriYukle(context)
        val i = liste.firstOrNull { it.id == isId } ?: return false
        i.tamamlandi = !i.tamamlandi
        i.tamamGun = if (i.tamamlandi) bugunKey() else ""
        isleriKaydet(context, liste)
        // Tamamlanan iş günlük sayaca yazılsın
        if (i.tamamlandi) {
            try {
                Store.recordCompletion(context)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Sayaç yazılamadı", e)
            }
        }
        return i.tamamlandi
    }

    fun isDuzenle(context: Context, isId: Long, yeniMetin: String) {
        if (yeniMetin.isBlank()) return
        val liste = isleriYukle(context)
        liste.firstOrNull { it.id == isId }?.metin = yeniMetin.trim().take(120)
        isleriKaydet(context, liste)
    }

    /** v11.07: İşi başka bir vakit dilimi / sekmesine taşır (sekmeler arası tablo yer değişikliği). */
    fun dilimDegistir(context: Context, isId: Long, yeniDilim: Dilim): Boolean {
        val liste = isleriYukle(context)
        val i = liste.firstOrNull { it.id == isId } ?: return false
        i.dilim = yeniDilim.anahtar
        isleriKaydet(context, liste)
        return true
    }

    // ═══════════════════════════════════════════════════════════════
    // ZAMAN HESABI
    // ═══════════════════════════════════════════════════════════════

    /** Bir dilimin başlangıç ve bitiş dakikası. */
    fun dilimAraligi(gun: NamazVakti.Gun, dilim: Dilim): Pair<Int, Int> {
        val bas = gun.dakika(dilim.baslangic)
        val bit = gun.dakika(dilim.bitis)
        return bas to bit
    }

    /** Dilimin süresi (dakika). Gece dilimi gün aşırıdır. */
    fun dilimSuresi(gun: NamazVakti.Gun, dilim: Dilim): Int {
        val (bas, bit) = dilimAraligi(gun, dilim)
        if (bas < 0 || bit < 0) return 0
        return if (dilim == Dilim.GECE) (1440 - bas + bit) else (bit - bas).coerceAtLeast(0)
    }

    /** Şu an hangi dilimdeyiz? */
    fun aktifDilim(gun: NamazVakti.Gun, simdiDakika: Int): Dilim {
        Dilim.entries.forEach { d ->
            val (bas, bit) = dilimAraligi(gun, d)
            if (bas < 0 || bit < 0) return@forEach
            val icinde = if (d == Dilim.GECE) {
                simdiDakika >= bas || simdiDakika < bit
            } else {
                simdiDakika in bas until bit
            }
            if (icinde) return d
        }
        return Dilim.GECE
    }

    /** Aktif dilimde kaç dakika kaldı? */
    fun kalanDakika(gun: NamazVakti.Gun, dilim: Dilim, simdiDakika: Int): Int {
        val (_, bit) = dilimAraligi(gun, dilim)
        if (bit < 0) return 0
        return if (dilim == Dilim.GECE && simdiDakika >= bit) {
            1440 - simdiDakika + bit
        } else {
            (bit - simdiDakika).coerceAtLeast(0)
        }
    }

    /** "1s 25dk" biçiminde okunabilir süre. */
    fun sureMetni(dakika: Int): String = when {
        dakika <= 0 -> "—"
        dakika < 60 -> dakika.toString() + " dk"
        dakika % 60 == 0 -> (dakika / 60).toString() + " sa"
        else -> (dakika / 60).toString() + " sa " + (dakika % 60) + " dk"
    }

    // ═══════════════════════════════════════════════════════════════
    // ÖNERİ ÜRETİCİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * "Şu an ne yapmalıyım?" sorusunun cevabı.
     *
     * Öncelik:
     *   1. Bu dilime atanmış tamamlanmamış iş varsa onu söyle
     *   2. Yoksa dilimin doğasına uygun genel öneri ver
     */
    fun simdiNeYapmali(context: Context): String {
        return try {
            val gun = NamazVakti.bugunDuzeltilmis(context)
            val simdi = NamazVakti.simdiDakika()
            val dilim = aktifDilim(gun, simdi)
            val kalan = kalanDakika(gun, dilim, simdi)

            val bekleyen = dilimIsleri(context, dilim).filter { !it.tamamlandi }
            if (bekleyen.isNotEmpty()) {
                context.getString(
                    R.string.np_oneri_is,
                    context.getString(dilim.adRes),
                    bekleyen.first().metin,
                    sureMetni(kalan)
                )
            } else {
                context.getString(
                    R.string.np_oneri_bos,
                    context.getString(dilim.adRes),
                    context.getString(dilim.varsayilanIsRes),
                    sureMetni(kalan)
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Öneri üretilemedi", e)
            ""
        }
    }

    /** Bugünkü plan özeti — kaç iş var, kaçı bitti. */
    fun bugunOzet(context: Context): Pair<Int, Int> {
        val hepsi = isleriYukle(context)
        return hepsi.count { it.tamamlandi } to hepsi.size
    }

    /**
     * Hazır plan şablonu yükler — kullanıcı sıfırdan yazmasın.
     * Var olan işlerin üzerine eklenir, silme yapılmaz.
     */
    fun sablonYukle(context: Context) {
        val mevcut = isleriYukle(context)
        val sablon = mapOf(
            Dilim.SABAH to listOf(R.string.np_s_sabah1, R.string.np_s_sabah2),
            Dilim.KUSLUK to listOf(R.string.np_s_kusluk1, R.string.np_s_kusluk2),
            Dilim.OGLEDEN to listOf(R.string.np_s_ogleden1, R.string.np_s_ogleden2),
            Dilim.IKINDIDEN to listOf(R.string.np_s_ikindiden1, R.string.np_s_ikindiden2),
            Dilim.AKSAMDAN to listOf(R.string.np_s_aksamdan1),
            Dilim.GECE to listOf(R.string.np_s_gece1)
        )
        var sira = 0
        sablon.forEach { (dilim, isler) ->
            isler.forEach { res ->
                val metin = context.getString(res)
                // Aynı metin zaten varsa ekleme
                if (mevcut.none { it.dilim == dilim.anahtar && it.metin == metin }) {
                    mevcut.add(
                        Is(
                            id = System.currentTimeMillis() + (sira++),
                            dilim = dilim.anahtar,
                            metin = metin
                        )
                    )
                }
            }
        }
        isleriKaydet(context, mevcut)
    }

    fun tumunuTemizle(context: Context) {
        prefs(context).edit().remove(K_PLAN).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): String =
        prefs(context).getString(K_PLAN, "[]") ?: "[]"

    fun iceAktar(context: Context, json: String) {
        try {
            JSONArray(json)   // geçerlilik kontrolü
            prefs(context).edit().putString(K_PLAN, json).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Plan içe aktarılamadı", e)
        }
    }
}
