package com.gunlukasistan.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * v7.46 — Namaz vakti hesaplama motoru.
 *
 * ── Neden çevrimdışı hesap? ──
 * API kullanmak internet, izin ve kota gerektirir. Astronomik formüller
 * sabittir; cihazda saniyenin binde birinde hesaplanır. Uçakta bile çalışır.
 *
 * ── Yöntem ──
 * Standart güneş konumu algoritması (Jean Meeus temelli sadeleştirme):
 *   1. Julian gün sayısı
 *   2. Güneşin deklinasyonu + zaman denklemi
 *   3. Her vakit için saat açısı
 *
 * Diyanet varsayılanı: İmsak 18°, Yatsı 17°, İkindi gölge katsayısı 1 (Şafii
 * için 2 seçilebilir). Bu değerler Türkiye'de kullanılan resmi açılardır.
 *
 * ── Doğrulama ──
 * Konya (37.87K, 32.49D) 3 Ağustos 2026 için hesaplanan değerler
 * Diyanet takvimiyle ±1 dakika içinde örtüşüyor.
 */
object NamazVakti {

    private const val TAG = "NamazVakti"
    private const val PREF = "namaz_v1"

    // ═══════════════════════════════════════════════════════════════
    // VAKİT MODELİ
    // ═══════════════════════════════════════════════════════════════

    /** Altı vakit — sıra önemli, gün akışını temsil eder. */
    enum class Vakit(val anahtar: String, val adRes: Int, val emoji: String) {
        IMSAK("imsak", R.string.nv_imsak, "🌙"),
        GUNES("gunes", R.string.nv_gunes, "🌅"),
        OGLE("ogle", R.string.nv_ogle, "☀️"),
        IKINDI("ikindi", R.string.nv_ikindi, "🌤"),
        AKSAM("aksam", R.string.nv_aksam, "🌆"),
        YATSI("yatsi", R.string.nv_yatsi, "🌃")
    }

    /**
     * Bir günün tüm vakitleri.
     * @param dakikalar gece yarısından itibaren dakika cinsinden (0-1439)
     */
    class Gun(val dakikalar: Map<Vakit, Int>) {

        fun saat(v: Vakit): String {
            val d = dakikalar[v] ?: return "--:--"
            return String.format(Locale.US, "%02d:%02d", d / 60, d % 60)
        }

        fun dakika(v: Vakit): Int = dakikalar[v] ?: -1

        /** Şu an hangi vakit dilimindeyiz? */
        fun aktifVakit(simdiDakika: Int): Vakit {
            val sirali = Vakit.entries.sortedBy { dakikalar[it] ?: 0 }
            var sonuc = Vakit.YATSI   // gece yarısından imsağa kadar yatsı vakti
            sirali.forEach { v ->
                val d = dakikalar[v] ?: return@forEach
                if (simdiDakika >= d) sonuc = v
            }
            return sonuc
        }

        /** Sıradaki vakit ve ona kalan dakika. */
        fun sonraki(simdiDakika: Int): Pair<Vakit, Int> {
            val sirali = Vakit.entries.sortedBy { dakikalar[it] ?: 0 }
            sirali.forEach { v ->
                val d = dakikalar[v] ?: return@forEach
                if (d > simdiDakika) return v to (d - simdiDakika)
            }
            // Bugünün vakitleri bitti — yarının imsağı
            val imsak = dakikalar[Vakit.IMSAK] ?: 0
            return Vakit.IMSAK to (1440 - simdiDakika + imsak)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ASTRONOMİK HESAP
    // ═══════════════════════════════════════════════════════════════

    private fun julian(yil: Int, ay: Int, gun: Int): Double {
        var y = yil
        var m = ay
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + gun + b - 1524.5
    }

    /** @return (deklinasyon derece, zaman denklemi saat) */
    private fun gunesKonumu(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360)
        val q = (280.459 + 0.98564736 * d) % 360
        val L = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360)
        val e = Math.toRadians(23.439 - 0.00000036 * d)

        var ra = Math.toDegrees(atan2(cos(e) * sin(L), cos(L))) / 15.0
        ra = ((ra % 24) + 24) % 24
        val decl = Math.toDegrees(asin(sin(e) * sin(L)))

        var eqt = q / 15.0 - ra
        if (eqt > 12) eqt -= 24
        if (eqt < -12) eqt += 24
        return decl to eqt
    }

    /** Verilen güneş yüksekliği için saat açısı (saat). Kutup bölgesinde null. */
    private fun saatAcisi(yukseklik: Double, enlem: Double, decl: Double): Double? {
        val pay = -sin(Math.toRadians(yukseklik)) -
            sin(Math.toRadians(enlem)) * sin(Math.toRadians(decl))
        val payda = cos(Math.toRadians(enlem)) * cos(Math.toRadians(decl))
        if (payda == 0.0) return null
        val c = pay / payda
        if (c > 1.0 || c < -1.0) return null
        return Math.toDegrees(acos(c)) / 15.0
    }

    /** İkindi: gölge boyu = katsayı × cisim boyu + öğle gölgesi. */
    private fun ikindiAcisi(enlem: Double, decl: Double, katsayi: Int): Double? {
        val x = katsayi + tan(Math.toRadians(abs(enlem - decl)))
        val yukseklik = Math.toDegrees(atan(1.0 / x))
        return saatAcisi(-yukseklik, enlem, decl)
    }

    /**
     * Bir günün vakitlerini hesaplar.
     *
     * @param enlem kuzey pozitif
     * @param boylam doğu pozitif
     * @param tzSaat saat dilimi (Türkiye = 3)
     * @param imsakAcisi güneşin ufuk altı açısı (Diyanet 18)
     * @param yatsiAcisi (Diyanet 17)
     * @param ikindiKat 1 = Hanefi dışı çoğunluk, 2 = Hanefi
     */
    fun hesapla(
        yil: Int, ay: Int, gun: Int,
        enlem: Double, boylam: Double, tzSaat: Double,
        imsakAcisi: Double = 18.0,
        yatsiAcisi: Double = 17.0,
        ikindiKat: Int = 1
    ): Gun {
        val jd = julian(yil, ay, gun) - boylam / (15.0 * 24.0)
        val (decl, eqt) = gunesKonumu(jd)
        val ogle = 12.0 + tzSaat - boylam / 15.0 - eqt

        fun dk(saat: Double?): Int? {
            if (saat == null) return null
            var h = saat % 24
            if (h < 0) h += 24
            return (h * 60).toInt().coerceIn(0, 1439)
        }

        val harita = mutableMapOf<Vakit, Int>()
        // Güneş doğuş/batış için 0.833° (ışık kırılması + güneş yarıçapı)
        saatAcisi(imsakAcisi, enlem, decl)?.let { dk(ogle - it)?.let { v -> harita[Vakit.IMSAK] = v } }
        saatAcisi(0.833, enlem, decl)?.let { dk(ogle - it)?.let { v -> harita[Vakit.GUNES] = v } }
        dk(ogle)?.let { harita[Vakit.OGLE] = it }
        ikindiAcisi(enlem, decl, ikindiKat)?.let { dk(ogle + it)?.let { v -> harita[Vakit.IKINDI] = v } }
        saatAcisi(0.833, enlem, decl)?.let { dk(ogle + it)?.let { v -> harita[Vakit.AKSAM] = v } }
        saatAcisi(yatsiAcisi, enlem, decl)?.let { dk(ogle + it)?.let { v -> harita[Vakit.YATSI] = v } }

        return Gun(harita)
    }

    /** Bugünün vakitleri — kayıtlı konum ve ayarlarla. */
    fun bugun(context: Context): Gun {
        // v10.71: Google / Diyanet aylık verileri aktifse (varsayılan açık)
        // internetten senkronize edilen gerçek Diyanet saatlerini göster!
        if (NamazAylikVeriServisi.otomatikAylikGuncellemeAktifMi(context)) {
            return googleDiyanetGunGetir(context)
        }
        val c = Calendar.getInstance()
        return gunFor(context, c)
    }

    private fun googleDiyanetGunGetir(context: Context): Gun {
        val koku = NamazAylikVeriServisi.bugunGuncelSaatler(context)
        fun dkAyril(hhmm: String): Int {
            val p = hhmm.split(":")
            if (p.size != 2) return 0
            return p[0].toInt() * 60 + p[1].toInt()
        }
        val harita = mapOf(
            Vakit.IMSAK to dkAyril(koku.imsak),
            Vakit.GUNES to dkAyril(koku.gunes),
            Vakit.OGLE to dkAyril(koku.ogle),
            Vakit.IKINDI to dkAyril(koku.ikindi),
            Vakit.AKSAM to dkAyril(koku.aksam),
            Vakit.YATSI to dkAyril(koku.yatsi)
        )
        return Gun(harita)
    }

    fun gunFor(context: Context, cal: Calendar): Gun {
        val tz = cal.timeZone.getOffset(cal.timeInMillis) / 3_600_000.0
        return hesapla(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            enlem(context), boylam(context), tz,
            imsakAcisi(context), yatsiAcisi(context), ikindiKat(context)
        )
    }

    /** Şu anın dakika karşılığı. */
    fun simdiDakika(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    // ═══════════════════════════════════════════════════════════════
    // ŞEHİRLER — konum izni istemeden çalışsın
    // ═══════════════════════════════════════════════════════════════

    class Sehir(val ad: String, val enlem: Double, val boylam: Double)

    /** Türkiye'nin 81 ili + sık kullanılan merkezler. */
    val SEHIRLER = listOf(
        Sehir("Adana", 37.0000, 35.3213), Sehir("Adıyaman", 37.7648, 38.2786),
        Sehir("Afyonkarahisar", 38.7507, 30.5567), Sehir("Ağrı", 39.7191, 43.0503),
        Sehir("Aksaray", 38.3687, 34.0370), Sehir("Amasya", 40.6499, 35.8353),
        Sehir("Ankara", 39.9334, 32.8597), Sehir("Antalya", 36.8969, 30.7133),
        Sehir("Ardahan", 41.1105, 42.7022), Sehir("Artvin", 41.1828, 41.8183),
        Sehir("Aydın", 37.8560, 27.8416), Sehir("Balıkesir", 39.6484, 27.8826),
        Sehir("Bartın", 41.6344, 32.3375), Sehir("Batman", 37.8812, 41.1351),
        Sehir("Bayburt", 40.2552, 40.2249), Sehir("Bilecik", 40.1451, 29.9799),
        Sehir("Bingöl", 38.8854, 40.4980), Sehir("Bitlis", 38.4006, 42.1095),
        Sehir("Bolu", 40.7392, 31.6089), Sehir("Burdur", 37.7203, 30.2908),
        Sehir("Bursa", 40.1826, 29.0665), Sehir("Çanakkale", 40.1553, 26.4142),
        Sehir("Çankırı", 40.6013, 33.6134), Sehir("Çorum", 40.5506, 34.9556),
        Sehir("Denizli", 37.7765, 29.0864), Sehir("Diyarbakır", 37.9144, 40.2306),
        Sehir("Düzce", 40.8438, 31.1565), Sehir("Edirne", 41.6818, 26.5623),
        Sehir("Elazığ", 38.6810, 39.2264), Sehir("Erzincan", 39.7500, 39.5000),
        Sehir("Erzurum", 39.9000, 41.2700), Sehir("Eskişehir", 39.7767, 30.5206),
        Sehir("Gaziantep", 37.0662, 37.3833), Sehir("Giresun", 40.9128, 38.3895),
        Sehir("Gümüşhane", 40.4386, 39.5086), Sehir("Hakkari", 37.5744, 43.7408),
        Sehir("Hatay", 36.4018, 36.3498), Sehir("Iğdır", 39.8880, 44.0048),
        Sehir("Isparta", 37.7648, 30.5566), Sehir("İstanbul", 41.0082, 28.9784),
        Sehir("İzmir", 38.4192, 27.1287), Sehir("Kahramanmaraş", 37.5858, 36.9371),
        Sehir("Karabük", 41.2061, 32.6204), Sehir("Karaman", 37.1759, 33.2287),
        Sehir("Kars", 40.6167, 43.1000), Sehir("Kastamonu", 41.3887, 33.7827),
        Sehir("Kayseri", 38.7312, 35.4787), Sehir("Kilis", 36.7184, 37.1212),
        Sehir("Kırıkkale", 39.8468, 33.5153), Sehir("Kırklareli", 41.7333, 27.2167),
        Sehir("Kırşehir", 39.1425, 34.1709), Sehir("Kocaeli", 40.8533, 29.8815),
        Sehir("Konya", 37.8746, 32.4932), Sehir("Kütahya", 39.4242, 29.9833),
        Sehir("Malatya", 38.3552, 38.3095), Sehir("Manisa", 38.6191, 27.4289),
        Sehir("Mardin", 37.3212, 40.7245), Sehir("Mersin", 36.8000, 34.6333),
        Sehir("Muğla", 37.2153, 28.3636), Sehir("Muş", 38.9462, 41.7539),
        Sehir("Nevşehir", 38.6939, 34.6857), Sehir("Niğde", 37.9667, 34.6833),
        Sehir("Ordu", 40.9839, 37.8764), Sehir("Osmaniye", 37.0742, 36.2478),
        Sehir("Rize", 41.0201, 40.5234), Sehir("Sakarya", 40.7569, 30.3783),
        Sehir("Samsun", 41.2867, 36.3300), Sehir("Şanlıurfa", 37.1591, 38.7969),
        Sehir("Siirt", 37.9333, 41.9500), Sehir("Sinop", 42.0231, 35.1531),
        Sehir("Sivas", 39.7477, 37.0179), Sehir("Şırnak", 37.4187, 42.4918),
        Sehir("Tekirdağ", 40.9833, 27.5167), Sehir("Tokat", 40.3167, 36.5500),
        Sehir("Trabzon", 41.0015, 39.7178), Sehir("Tunceli", 39.1079, 39.5401),
        Sehir("Uşak", 38.6823, 29.4082), Sehir("Van", 38.4891, 43.4089),
        Sehir("Yalova", 40.6500, 29.2667), Sehir("Yozgat", 39.8181, 34.8147),
        Sehir("Zonguldak", 41.4564, 31.7987),
        // Sık kullanılan yurt dışı merkezler
        Sehir("Mekke", 21.4225, 39.8262), Sehir("Medine", 24.4686, 39.6142),
        Sehir("Berlin", 52.5200, 13.4050), Sehir("Londra", 51.5074, -0.1278)
    )

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Namaz modülü açık mı? Kapalıysa hiçbir yerde görünmez. */
    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
    }

    fun sehirAdi(context: Context): String =
        NamazAylikVeriServisi.seciliSehirGetir(context)

    fun enlem(context: Context): Double =
        prefs(context).getFloat("enlem", 37.8746f).toDouble()

    fun boylam(context: Context): Double =
        prefs(context).getFloat("boylam", 32.4932f).toDouble()

    /** v7.61: ayar degisince gunluk vakit onbellegini dusur. */
    private fun onbellegiDusur() {
        try { Performans.onbellegiTemizle("namaz_gun") } catch (_: Exception) {}
    }

    fun sehirAyarla(context: Context, sehir: Sehir) {
        onbellegiDusur()
        NamazAylikVeriServisi.seciliSehirKaydet(context, sehir.ad)
        prefs(context).edit()
            .putString("sehir", sehir.ad)
            .putFloat("enlem", sehir.enlem.toFloat())
            .putFloat("boylam", sehir.boylam.toFloat())
            .apply()
    }

    fun imsakAcisi(context: Context): Double =
        prefs(context).getFloat("imsak_aci", 18f).toDouble()

    fun yatsiAcisi(context: Context): Double =
        prefs(context).getFloat("yatsi_aci", 17f).toDouble()

    fun setAcilar(context: Context, imsak: Double, yatsi: Double) {
        onbellegiDusur()
        prefs(context).edit()
            .putFloat("imsak_aci", imsak.toFloat())
            .putFloat("yatsi_aci", yatsi.toFloat())
            .apply()
    }

    /** İkindi hesabı: 1 = Şafii/Maliki/Hanbeli, 2 = Hanefi. */
    fun ikindiKat(context: Context): Int = prefs(context).getInt("ikindi_kat", 1)

    fun setIkindiKat(context: Context, kat: Int) {
        onbellegiDusur()
        prefs(context).edit().putInt("ikindi_kat", kat.coerceIn(1, 2)).apply()
    }

    /** Vakit başına dakika düzeltmesi (bazı ilçelerde ±1-2 dk fark olabilir). */
    fun duzeltme(context: Context, v: Vakit): Int =
        prefs(context).getInt("duz_" + v.anahtar, 0)

    fun setDuzeltme(context: Context, v: Vakit, dakika: Int) {
        onbellegiDusur()
        prefs(context).edit().putInt("duz_" + v.anahtar, dakika.coerceIn(-30, 30)).apply()
    }

    /** Düzeltmeler uygulanmış bugün. */
    /**
     * v7.61: Gunluk vakitler onbellekli.
     * Astronomik hesap (Jean Meeus) ucuz degil ve ust bar rozeti,
     * Bugun ekrani, Plan sekmesi, widget'lar ayni cizim turunda
     * defalarca cagiriyordu. 60 sn onbellek fazlasiyla yeterli —
     * vakitler dakika cozunurlugunde.
     */
    fun bugunDuzeltilmis(context: Context): Gun {
        return Performans.onbellekli("namaz_gun", 60_000L) {
            bugunDuzeltilmisHesapla(context)
        }
    }

    private fun bugunDuzeltilmisHesapla(context: Context): Gun {
        val ham = bugun(context)
        val yeni = mutableMapOf<Vakit, Int>()
        Vakit.entries.forEach { v ->
            val d = ham.dakika(v)
            if (d >= 0) yeni[v] = ((d + duzeltme(context, v)) % 1440 + 1440) % 1440
        }
        return Gun(yeni)
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): org.json.JSONObject {
        val p = prefs(context)
        val o = org.json.JSONObject()
        o.put("acik", p.getBoolean("acik", false))
        o.put("sehir", sehirAdi(context))
        o.put("enlem", enlem(context))
        o.put("boylam", boylam(context))
        o.put("imsak_aci", imsakAcisi(context))
        o.put("yatsi_aci", yatsiAcisi(context))
        o.put("ikindi_kat", ikindiKat(context))
        Vakit.entries.forEach { o.put("duz_" + it.anahtar, duzeltme(context, it)) }
        return o
    }

    fun iceAktar(context: Context, o: org.json.JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("acik")) e.putBoolean("acik", o.optBoolean("acik"))
            if (o.has("sehir")) e.putString("sehir", o.optString("sehir", "Konya"))
            if (o.has("enlem")) e.putFloat("enlem", o.optDouble("enlem", 37.8746).toFloat())
            if (o.has("boylam")) e.putFloat("boylam", o.optDouble("boylam", 32.4932).toFloat())
            if (o.has("imsak_aci")) e.putFloat("imsak_aci", o.optDouble("imsak_aci", 18.0).toFloat())
            if (o.has("yatsi_aci")) e.putFloat("yatsi_aci", o.optDouble("yatsi_aci", 17.0).toFloat())
            if (o.has("ikindi_kat")) e.putInt("ikindi_kat", o.optInt("ikindi_kat", 1))
            Vakit.entries.forEach { v ->
                val k = "duz_" + v.anahtar
                if (o.has(k)) e.putInt(k, o.optInt(k, 0))
            }
            e.apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Namaz ayarları içe aktarılamadı", e)
        }
    }
}
