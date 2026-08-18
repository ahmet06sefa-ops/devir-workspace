package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.39 — Katalog #42: Alışkanlık mola modu.
 *
 * ── Kullanıcının isteği ──
 * "Alışkanlıkta ara verince serim sıfırlanmasın; tatil/hastalıkta seri
 *  donsun, dönünce kaldığı yerden devam etsin."
 *
 * ── Model ──
 * Her alışkanlığın mola durumu prefs'te yaşar:
 *   aliskanlik_mola_v1 → { "<habitId>": { "baslangic": 20260808,
 *                                          "kapatilanlar": "20260801,20260802" } }
 * · Açık aralık: baslangic > 0 iken o günden itibaren her gün molada
 *   sayılır (seri donar, kırılmaz).
 * · Kapalı set: moladan dönülünce geçen günler kapatilanlar kümesine
 *   taşınır; seri hesabı bu günleri de atlar.
 * Saf mantık üstte (JVM testli), depo altta.
 */
object AliskanlikMola {

    // ──────────────── Saf mantık (android YOK, JVM testli) ────────────────

    /** 8 haneli gün anahtarı (yyyyMMdd) — Store.dayKey ile aynı biçim. */
    fun gunAnahtari(ms: Long): Int =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms)).toInt()

    /** Molada sayılma kuralı: kapalı sette üye ya da açık aralıkta (≥ başlangıç). */
    fun moladaMiPure(bugun: Int, baslangic: Int, kapatilanlar: Set<Int>): Boolean =
        bugun in kapatilanlar || (baslangic > 0 && bugun >= baslangic)

    fun setYaz(gunler: Set<Int>): String = gunler.sorted().joinToString(",")

    fun setOku(ham: String?): Set<Int> =
        ham.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 19000101..29991231 }.toSet()

    /** İki gün anahtarı arasındaki tüm günler (uçlar dahil; takvim güvenli). */
    fun gunAraligi(bas: Int, son: Int): List<Int> {
        if (bas <= 0 || son < bas) return emptyList()
        val bicim = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cal = Calendar.getInstance()
        return runCatching {
            cal.time = bicim.parse(bas.toString())!!
            val out = ArrayList<Int>()
            var anahtar = bas
            while (anahtar <= son && out.size < 1000) {
                out.add(anahtar)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                anahtar = bicim.format(cal.time).toInt()
            }
            out
        }.getOrDefault(emptyList())
    }

    // ──────────────── Depo (SharedPreferences) ────────────────

    private const val PREF = "aliskanlik_mola_v1"
    private const val K_KOK = "molalar"
    private const val K_BAS = "baslangic"
    private const val K_KAPALI = "kapatilanlar"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun kok(c: Context): JSONObject =
        runCatching { JSONObject(prefs(c).getString(K_KOK, "{}") ?: "{}") }
            .getOrDefault(JSONObject())

    private fun dugum(c: Context, habitId: Long): JSONObject? =
        runCatching { kok(c).optJSONObject(habitId.toString()) }.getOrNull()

    private fun kaydet(c: Context, habitId: Long, baslangic: Int, kapatilanlar: Set<Int>) {
        runCatching {
            val k = kok(c)
            if (baslangic <= 0 && kapatilanlar.isEmpty()) {
                k.remove(habitId.toString())
            } else {
                k.put(
                    habitId.toString(),
                    JSONObject().put(K_BAS, baslangic).put(K_KAPALI, setYaz(kapatilanlar))
                )
            }
            prefs(c).edit().putString(K_KOK, k.toString()).apply()
        }
    }

    /** Açık mola başlangıcı (0 = açık mola yok). */
    fun baslangic(c: Context, habitId: Long): Int =
        dugum(c, habitId)?.optInt(K_BAS, 0) ?: 0

    fun kapatilanlar(c: Context, habitId: Long): Set<Int> =
        setOku(dugum(c, habitId)?.optString(K_KAPALI, ""))

    /** Açık aralık var mı (moladan henüz dönülmemiş). */
    fun aktifMi(c: Context, habitId: Long): Boolean = baslangic(c, habitId) > 0

    /** Verilen gün molada mı (kapalı set + açık aralık). */
    fun moladaMi(c: Context, habitId: Long, gunKey: Int): Boolean =
        moladaMiPure(gunKey, baslangic(c, habitId), kapatilanlar(c, habitId))

    /** Molayı başlat — zaten açıksa mevcut aralık korunur. */
    fun al(c: Context, habitId: Long, simdiMs: Long = System.currentTimeMillis()) {
        if (aktifMi(c, habitId)) return
        kaydet(c, habitId, gunAnahtari(simdiMs), kapatilanlar(c, habitId))
    }

    /**
     * Moladan dön: [baslangic, dün] aralığı kapalı sete taşınır, açık
     * aralık sıfırlanır.
     * @return yeni kapanan gün sayısı (toast metnindeki "%d gün").
     */
    fun don(c: Context, habitId: Long, simdiMs: Long = System.currentTimeMillis()): Int {
        val bas = baslangic(c, habitId)
        if (bas <= 0) return 0
        val dunCal = Calendar.getInstance().apply {
            timeInMillis = simdiMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val dun = gunAnahtari(dunCal.timeInMillis)
        val eski = kapatilanlar(c, habitId)
        val yeni = gunAraligi(bas, dun).filterNot { it in eski }
        kaydet(c, habitId, 0, eski + yeni.toSet())
        return yeni.size
    }
}

/**
 * v10.39 — Katalog #45: 21 gün kuralı.
 * Alışkanlık 21 tamamlanmış güne ulaşınca "oturdu" sayılır; karttaki
 * ince bar ve 🎓 N/21 rozeti bu sayacı gösterir.
 */
object Kural21 {
    const val HEDEF = 21
    fun gun(tamamlanan: Int): Int = tamamlanan.coerceIn(0, HEDEF)
    fun yuzde(tamamlanan: Int): Int = gun(tamamlanan) * 100 / HEDEF
}
