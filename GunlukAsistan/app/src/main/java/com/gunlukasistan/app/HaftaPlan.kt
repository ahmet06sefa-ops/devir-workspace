package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * v7.96 — Haftalık çalışma planı.
 *
 * ── Kullanıcı isteği (öneri 6) ──
 * "7 günlük ızgara: hangi gün kaç dakika hedef, hangi ders. Koç bu plana
 *  göre hatırlatsın. Şu an hedef her gün aynı."
 *
 * ── [Koc] ile ilişkisi ──
 * `Koc.gunlukHedef` tek bir sayı tutuyor ve her güne aynı uygulanıyordu.
 * Gerçekte hafta içi 60 dk, cumartesi 180 dk, pazar 0 dk çalışmak isteyen
 * biri bunu ifade edemiyordu.
 *
 * Bu sınıf gün bazlı hedef tutuyor. `Koc` önce buraya bakıyor; plan yoksa
 * ya da o gün tanımsızsa eski davranışa (`gunlukHedef`) düşüyor. Böylece
 * mevcut kullanıcılar hiçbir şey fark etmiyor.
 *
 * ── Neden ders de atanabiliyor ──
 * "Pazartesi matematik, salı fizik" gibi bir düzen kurmak isteyen için.
 * Atanmamışsa müfredattaki sıradaki ders kullanılır.
 */
object HaftaPlan {

    private const val TAG = "HaftaPlan"
    private const val PREF = "hafta_plan_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Calendar sabitleri: 1=Pazar … 7=Cumartesi. Görüntü sırası Pzt→Paz. */
    val gunSirasi = intArrayOf(2, 3, 4, 5, 6, 7, 1)

    fun gunAdi(context: Context, calendarGun: Int): String = context.getString(
        when (calendarGun) {
            Calendar.MONDAY -> R.string.gun_pzt
            Calendar.TUESDAY -> R.string.gun_sal
            Calendar.WEDNESDAY -> R.string.gun_car
            Calendar.THURSDAY -> R.string.gun_per
            Calendar.FRIDAY -> R.string.gun_cum
            Calendar.SATURDAY -> R.string.gun_cmt
            else -> R.string.gun_paz
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // AÇIK / KAPALI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Plan kullanılsın mı?
     *
     * Varsayılan **kapalı**: açık olsaydı mevcut kullanıcıların günlük
     * hedefi habersizce değişirdi.
     */
    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜN HEDEFLERİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * O günün hedefi (dakika). -1 = tanımsız (Koç varsayılanı kullanılır).
     * 0 = izin günü.
     */
    fun hedef(context: Context, calendarGun: Int): Int =
        prefs(context).getInt("h_$calendarGun", -1)

    fun setHedef(context: Context, calendarGun: Int, dakika: Int) {
        prefs(context).edit().putInt("h_$calendarGun", dakika.coerceIn(-1, 600)).apply()
    }

    /** Bugünün planlı hedefi; plan kapalıysa ya da tanımsızsa null. */
    fun bugunHedefi(context: Context): Int? {
        if (!acikMi(context)) return null
        val bugun = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val h = hedef(context, bugun)
        return if (h < 0) null else h
    }

    /** Bugün izin günü mü (plan açık ve hedef 0). */
    fun bugunIzinMi(context: Context): Boolean = bugunHedefi(context) == 0

    // ═══════════════════════════════════════════════════════════════
    // GÜN DERSLERİ
    // ═══════════════════════════════════════════════════════════════

    /** O güne atanmış ders/madde kimliği. 0 = atanmamış. */
    fun ders(context: Context, calendarGun: Int): Long =
        prefs(context).getLong("d_$calendarGun", 0L)

    fun setDers(context: Context, calendarGun: Int, adimId: Long) {
        prefs(context).edit().putLong("d_$calendarGun", adimId).apply()
    }

    /** O güne atanmış dersin adı; yoksa boş. */
    fun dersAdi(context: Context, calendarGun: Int): String {
        val id = ders(context, calendarGun)
        if (id == 0L) return ""
        return Mufredat.adimlar(context).firstOrNull { it.id == id }?.baslik.orEmpty()
    }

    /**
     * Bugün çalışılması gereken adım.
     *
     * Plana ders atanmışsa o, atanmamışsa müfredattaki sıradaki.
     */
    fun bugunDersi(context: Context): Mufredat.Adim? {
        if (acikMi(context)) {
            val bugun = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val id = ders(context, bugun)
            if (id != 0L) {
                Mufredat.adimlar(context).firstOrNull { it.id == id && !it.bitti }
                    ?.let { return it }
            }
        }
        return Mufredat.aktifAdim(context)
    }

    // ═══════════════════════════════════════════════════════════════
    // ÖZET
    // ═══════════════════════════════════════════════════════════════

    /** Haftalık toplam hedef (dakika). */
    fun haftaToplami(context: Context): Int {
        if (!acikMi(context)) return Koc.gunlukHedef(context) * 7
        return gunSirasi.sumOf { g ->
            val h = hedef(context, g)
            if (h < 0) Koc.gunlukHedef(context) else h
        }
    }

    /** Kaç gün çalışma planlandı. */
    fun calismaGunSayisi(context: Context): Int {
        if (!acikMi(context)) return 7
        return gunSirasi.count { g -> hedef(context, g) != 0 }
    }

    fun ozet(context: Context): String {
        if (!acikMi(context)) return context.getString(R.string.hp_kapali)
        val toplam = haftaToplami(context)
        return context.getString(
            R.string.hp_ozet, calismaGunSayisi(context), toplam / 60, toplam % 60
        )
    }

    /** Hazır şablon uygular. */
    fun sablonUygula(context: Context, sablon: Int) {
        val e = prefs(context).edit()
        when (sablon) {
            // Hafta içi yoğun, hafta sonu hafif
            0 -> {
                listOf(2, 3, 4, 5, 6).forEach { e.putInt("h_$it", 90) }
                e.putInt("h_7", 45).putInt("h_1", 0)
            }
            // Her gün eşit
            1 -> gunSirasi.forEach { e.putInt("h_$it", 60) }
            // Hafta sonu yoğun
            2 -> {
                listOf(2, 3, 4, 5, 6).forEach { e.putInt("h_$it", 30) }
                e.putInt("h_7", 180).putInt("h_1", 180)
            }
            // Kademeli artış (pazartesi hafif, cuma yoğun)
            3 -> {
                e.putInt("h_2", 30).putInt("h_3", 45).putInt("h_4", 60)
                    .putInt("h_5", 75).putInt("h_6", 90)
                    .putInt("h_7", 120).putInt("h_1", 0)
            }
        }
        e.putBoolean("acik", true).apply()
    }

    fun sablonAdi(context: Context, s: Int): String = context.getString(
        when (s) {
            1 -> R.string.hp_sablon_esit
            2 -> R.string.hp_sablon_haftasonu
            3 -> R.string.hp_sablon_kademeli
            else -> R.string.hp_sablon_haftaici
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject {
        val o = JSONObject()
        return try {
            o.put("acik", acikMi(context))
            gunSirasi.forEach { g ->
                o.put("h_$g", hedef(context, g))
                o.put("d_$g", ders(context, g))
            }
            o
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Plan dışa aktarılamadı", e)
            o
        }
    }

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            e.putBoolean("acik", o.optBoolean("acik", false))
            gunSirasi.forEach { g ->
                if (o.has("h_$g")) e.putInt("h_$g", o.optInt("h_$g", -1))
                if (o.has("d_$g")) e.putLong("d_$g", o.optLong("d_$g", 0L))
            }
            e.apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Plan içe aktarılamadı", e)
        }
    }
}
