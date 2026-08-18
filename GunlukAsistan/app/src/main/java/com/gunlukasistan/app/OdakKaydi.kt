package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.94 — Sayaç ile ders programı arasındaki köprü.
 *
 * ── Kullanıcı isteği (öneri 1) ──
 * "Sayaç bittiğinde bu süreyi hangi derse yazayım diye sorsun ve süre
 *  otomatik aktif derse işlensin."
 *
 * ── Neden eksikti ──
 * Sayaç ([TimerEngine]) süreyi `Store.addTodayFocusMinutes` ile **günlük
 * toplama** yazıyordu. Müfredat ([Mufredat]) ise ders bazlı süreyi ayrı
 * tutuyordu ve yalnızca gün kapanışında ([Koc.sureyiDerseYaz]) toplu
 * olarak dolduruluyordu. İkisi birbirinden habersizdi:
 *   · Sayaçla 3 ders çalışsan hepsi tek derse yazılıyordu
 *   · Sayaç kullanmadan gün kapatsan hiç çalışmadığın derse süre yazılıyordu
 *
 * Bu sınıf her sayaç oturumunu **bittiği anda** doğru derse işler.
 *
 * ── Oturum geçmişi neden tutuluyor ──
 * "Bugün neye ne kadar verdim" sorusu istatistik ekranında da gerekli.
 * Ayrıca yanlış derse yazılan süre geri alınabilsin diye.
 */
object OdakKaydi {

    private const val TAG = "OdakKaydi"
    private const val PREF = "odak_kaydi_v1"
    private const val K_OTURUMLAR = "oturumlar_json"

    /** Saklanan en fazla oturum sayısı. */
    private const val TAVAN = 300

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    // ═══════════════════════════════════════════════════════════════
    // AYAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sayaç bitince süre nereye yazılsın?
     *
     * 0 = sorma, aktif derse yaz (varsayılan — en az sürtünme)
     * 1 = her seferinde sor
     * 2 = derse hiç yazma (yalnızca günlük toplam)
     */
    const val MOD_OTOMATIK = 0
    const val MOD_SOR = 1
    const val MOD_KAPALI = 2

    fun mod(context: Context): Int =
        prefs(context).getInt("mod", MOD_OTOMATIK).coerceIn(0, 2)

    fun setMod(context: Context, m: Int) {
        prefs(context).edit().putInt("mod", m.coerceIn(0, 2)).apply()
    }

    fun modAdi(context: Context, m: Int): String = context.getString(
        when (m) {
            MOD_SOR -> R.string.ok_mod_sor
            MOD_KAPALI -> R.string.ok_mod_kapali
            else -> R.string.ok_mod_otomatik
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // OTURUM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tek bir çalışma oturumu.
     *
     * @param adimId hangi ders/maddeye yazıldı (0 = yazılmadı)
     * @param baslik o anki adımın adı — ders silinse bile geçmiş okunabilsin
     */
    data class Oturum(
        val zaman: Long,
        val dakika: Int,
        val adimId: Long,
        val baslik: String,
        val gun: String
    )

    fun oturumlar(context: Context): MutableList<Oturum> {
        val ham = prefs(context).getString(K_OTURUMLAR, "[]") ?: "[]"
        val liste = mutableListOf<Oturum>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Oturum(
                        zaman = o.optLong("z"),
                        dakika = o.optInt("d"),
                        adimId = o.optLong("a"),
                        baslik = o.optString("b"),
                        gun = o.optString("g")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Oturumlar okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Oturum>) {
        val dizi = JSONArray()
        liste.takeLast(TAVAN).forEach { o ->
            dizi.put(
                JSONObject()
                    .put("z", o.zaman).put("d", o.dakika)
                    .put("a", o.adimId).put("b", o.baslik).put("g", o.gun)
            )
        }
        prefs(context).edit().putString(K_OTURUMLAR, dizi.toString()).apply()
    }

    /**
     * Tamamlanan oturumu kaydeder ve süreyi derse işler.
     *
     * @param dakika çalışılan süre
     * @param adimId hedef ders/madde (0 ise aktif adım kullanılır)
     * @return yazıldığı adımın adı, yazılmadıysa boş
     */
    fun kaydet(context: Context, dakika: Int, adimId: Long = 0L): String {
        if (dakika <= 0) return ""

        val hedefId: Long
        val baslik: String

        if (adimId != 0L) {
            hedefId = adimId
            baslik = Mufredat.adimlar(context)
                .firstOrNull { it.id == adimId }?.baslik.orEmpty()
        } else {
            val aktif = Mufredat.aktifAdim(context)
            hedefId = aktif?.id ?: 0L
            baslik = aktif?.baslik.orEmpty()
        }

        if (hedefId != 0L) {
            Mufredat.dakikaEkle(context, hedefId, dakika)
        }

        val liste = oturumlar(context)
        liste.add(
            Oturum(System.currentTimeMillis(), dakika, hedefId, baslik, bugun())
        )
        yaz(context, liste)
        return baslik
    }

    /**
     * Son oturumu geri alır — yanlış derse yazıldıysa.
     *
     * Süre dersten düşülür ama günlük toplamdan düşülmez: kullanıcı
     * gerçekten o dakikalarca çalıştı, yalnızca etiketi yanlıştı.
     */
    fun sonuGeriAl(context: Context): Boolean {
        val liste = oturumlar(context)
        val son = liste.lastOrNull() ?: return false
        if (son.adimId != 0L) {
            Mufredat.dakikaEkle(context, son.adimId, -son.dakika)
        }
        yaz(context, liste.dropLast(1))
        return true
    }

    /** Son oturumu başka bir derse taşır. */
    fun sonuTasi(context: Context, yeniAdimId: Long): Boolean {
        val liste = oturumlar(context)
        val son = liste.lastOrNull() ?: return false
        if (son.adimId == yeniAdimId) return false

        if (son.adimId != 0L) Mufredat.dakikaEkle(context, son.adimId, -son.dakika)
        Mufredat.dakikaEkle(context, yeniAdimId, son.dakika)

        val yeniBaslik = Mufredat.adimlar(context)
            .firstOrNull { it.id == yeniAdimId }?.baslik.orEmpty()
        yaz(
            context,
            liste.dropLast(1) + son.copy(adimId = yeniAdimId, baslik = yeniBaslik)
        )
        return true
    }

    // ═══════════════════════════════════════════════════════════════
    // SORGULAR
    // ═══════════════════════════════════════════════════════════════

    fun bugunkuler(context: Context): List<Oturum> =
        oturumlar(context).filter { it.gun == bugun() }

    fun bugunToplamDk(context: Context): Int = bugunkuler(context).sumOf { it.dakika }

    fun bugunOturumSayisi(context: Context): Int = bugunkuler(context).size

    /** Bugün hangi derse ne kadar — istatistik için. */
    fun bugunDagilim(context: Context): List<Pair<String, Int>> =
        bugunkuler(context)
            .filter { it.baslik.isNotBlank() }
            .groupBy { it.baslik }
            .map { (ad, liste) -> ad to liste.sumOf { it.dakika } }
            .sortedByDescending { it.second }

    fun sonOturum(context: Context): Oturum? = oturumlar(context).lastOrNull()

    fun saatMetni(ms: Long): String =
        SimpleDateFormat("HH:mm", Locale("tr")).format(Date(ms))
}
