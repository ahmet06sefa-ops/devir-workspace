package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.37 — Özel öğretmen modunun hafızası.
 *
 * ── Neden ayrı bir depo? ──
 * DersAsistan (v7.31) tek seferlik soru-cevap yapar; hiçbir şey hatırlamaz.
 * Öğretmen modu ise **oturum** yürütür: nerede kaldığını, kullanıcının
 * seviyesini, hangi konuyu anlamadığını bilmek zorunda.
 *
 * ── Saklananlar ──
 *  1. Oturum durumu  — hangi ders, kaçıncı adım, kaç doğru/yanlış
 *  2. Seviye         — ders bazında 1..5 (yanlışta düşer, doğruda çıkar)
 *  3. Zayıf noktalar — anlaşılmayan başlıklar, tekrar anlatım için
 *
 * Veri SharedPreferences'ta JSON olarak durur (projenin genel deseni).
 */
object OgretmenStore {

    private const val TAG = "OgretmenStore"
    private const val PREF = "ogretmen_store"
    private const val K_OTURUMLAR = "oturumlar_json"
    private const val K_SEVIYE = "seviye_json"
    private const val K_ZAYIF = "zayif_json"

    /** Seviye sınırları: 1 = hiç bilmiyor, 5 = ileri düzey. */
    const val SEVIYE_MIN = 1
    const val SEVIYE_MAX = 5
    const val SEVIYE_VARSAYILAN = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    // ═══════════════════════════════════════════════════════════════
    // OTURUM MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir dersin öğretmen oturumu.
     *
     * @param adim kaçıncı anlatım adımındayız (0'dan başlar)
     * @param dogru oturumda verilen doğru cevap sayısı
     * @param yanlis oturumda verilen yanlış cevap sayısı
     * @param tamamlandi ders sonuna gelindi mi
     * @param sonOzet en son anlatılanın kısa özeti — kaldığı yerden devam için
     */
    data class Oturum(
        val lessonId: Long,
        var dersAdi: String,
        var adim: Int = 0,
        var dogru: Int = 0,
        var yanlis: Int = 0,
        var tamamlandi: Boolean = false,
        var sonOzet: String = "",
        var guncellendi: String = ""
    ) {
        /** Oturumdaki başarı yüzdesi. */
        val yuzde: Int
            get() {
                val t = dogru + yanlis
                return if (t == 0) 0 else dogru * 100 / t
            }
    }

    // ═══════════════════════════════════════════════════════════════
    // OTURUM OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    fun oturumlariYukle(context: Context): MutableList<Oturum> {
        val ham = prefs(context).getString(K_OTURUMLAR, "[]") ?: "[]"
        val liste = mutableListOf<Oturum>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Oturum(
                        lessonId = o.optLong("lessonId"),
                        dersAdi = o.optString("dersAdi"),
                        adim = o.optInt("adim"),
                        dogru = o.optInt("dogru"),
                        yanlis = o.optInt("yanlis"),
                        tamamlandi = o.optBoolean("tamamlandi"),
                        sonOzet = o.optString("sonOzet"),
                        guncellendi = o.optString("guncellendi")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Oturumlar okunamadı", e)
        }
        return liste
    }

    fun oturumlariKaydet(context: Context, liste: List<Oturum>) {
        val dizi = JSONArray()
        liste.forEach { o ->
            dizi.put(
                JSONObject()
                    .put("lessonId", o.lessonId)
                    .put("dersAdi", o.dersAdi)
                    .put("adim", o.adim)
                    .put("dogru", o.dogru)
                    .put("yanlis", o.yanlis)
                    .put("tamamlandi", o.tamamlandi)
                    .put("sonOzet", o.sonOzet)
                    .put("guncellendi", o.guncellendi)
            )
        }
        prefs(context).edit().putString(K_OTURUMLAR, dizi.toString()).apply()
    }

    /** Bir dersin oturumu — yoksa null. */
    fun oturum(context: Context, lessonId: Long): Oturum? =
        oturumlariYukle(context).firstOrNull { it.lessonId == lessonId }

    /** Oturumu kaydeder (varsa günceller, yoksa ekler). */
    fun oturumKaydet(context: Context, oturum: Oturum) {
        val liste = oturumlariYukle(context)
        oturum.guncellendi = bugun()
        val i = liste.indexOfFirst { it.lessonId == oturum.lessonId }
        if (i >= 0) liste[i] = oturum else liste.add(oturum)
        // En fazla 100 oturum tut — en eskiyi at
        while (liste.size > 100) liste.removeAt(0)
        oturumlariKaydet(context, liste)
    }

    /** Oturumu siler — "baştan başla" için. */
    fun oturumSil(context: Context, lessonId: Long) {
        oturumlariKaydet(context, oturumlariYukle(context).filterNot { it.lessonId == lessonId })
    }

    /** Yarım kalmış oturumlar — "kaldığın yerden devam" kartı için. */
    fun yarimOturumlar(context: Context): List<Oturum> =
        oturumlariYukle(context)
            .filter { !it.tamamlandi && it.adim > 0 }
            .sortedByDescending { it.guncellendi }

    /** Tamamlanan ders sayısı. */
    fun tamamlananSayisi(context: Context): Int =
        oturumlariYukle(context).count { it.tamamlandi }

    // ═══════════════════════════════════════════════════════════════
    // SEVİYE
    // ═══════════════════════════════════════════════════════════════

    private fun seviyeKok(context: Context): JSONObject =
        try {
            JSONObject(prefs(context).getString(K_SEVIYE, "{}") ?: "{}")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Seviye okunamadı", e)
            JSONObject()
        }

    /**
     * Dersin seviyesi (1..5).
     * Ders bazlı tutulur: kullanıcı AutoCAD'de ileri, Revit'te yeni olabilir.
     */
    fun seviye(context: Context, lessonId: Long): Int =
        seviyeKok(context).optInt(lessonId.toString(), SEVIYE_VARSAYILAN)
            .coerceIn(SEVIYE_MIN, SEVIYE_MAX)

    fun seviyeAyarla(context: Context, lessonId: Long, deger: Int) {
        val kok = seviyeKok(context)
        kok.put(lessonId.toString(), deger.coerceIn(SEVIYE_MIN, SEVIYE_MAX))
        prefs(context).edit().putString(K_SEVIYE, kok.toString()).apply()
    }

    /**
     * Cevaba göre seviyeyi ayarlar.
     * Doğru → +1 (en fazla 5), yanlış → -1 (en az 1).
     *
     * Bu, öğretmenin "seni tanıması"nı sağlar: art arda doğru veren
     * kullanıcıya daha derin anlatır, zorlanan kullanıcıya basitleştirir.
     */
    fun seviyeGuncelle(context: Context, lessonId: Long, dogruMu: Boolean): Int {
        val yeni = (seviye(context, lessonId) + if (dogruMu) 1 else -1)
            .coerceIn(SEVIYE_MIN, SEVIYE_MAX)
        seviyeAyarla(context, lessonId, yeni)
        return yeni
    }

    /** Seviyenin okunabilir adı — istemde ve arayüzde kullanılır. */
    fun seviyeAdi(context: Context, seviye: Int): String = when (seviye) {
        1 -> context.getString(R.string.tut_level_1)
        2 -> context.getString(R.string.tut_level_2)
        3 -> context.getString(R.string.tut_level_3)
        4 -> context.getString(R.string.tut_level_4)
        else -> context.getString(R.string.tut_level_5)
    }

    // ═══════════════════════════════════════════════════════════════
    // ZAYIF NOKTALAR
    // ═══════════════════════════════════════════════════════════════

    private fun zayifKok(context: Context): JSONObject =
        try {
            JSONObject(prefs(context).getString(K_ZAYIF, "{}") ?: "{}")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Zayıf noktalar okunamadı", e)
            JSONObject()
        }

    /**
     * Yanlış cevaplanan konuyu kaydeder.
     * Ders sonunda "şunları tekrar et" listesi çıkarmak için kullanılır.
     */
    fun zayifEkle(context: Context, lessonId: Long, konu: String) {
        if (konu.isBlank()) return
        val kok = zayifKok(context)
        val anahtar = lessonId.toString()
        val dizi = kok.optJSONArray(anahtar) ?: JSONArray()
        // Aynı konuyu iki kez yazma
        for (i in 0 until dizi.length()) {
            if (dizi.optString(i).equals(konu, ignoreCase = true)) return
        }
        dizi.put(konu.take(120))
        // Ders başına en fazla 12 zayıf nokta
        if (dizi.length() > 12) {
            val yeni = JSONArray()
            for (i in dizi.length() - 12 until dizi.length()) yeni.put(dizi.optString(i))
            kok.put(anahtar, yeni)
        } else {
            kok.put(anahtar, dizi)
        }
        prefs(context).edit().putString(K_ZAYIF, kok.toString()).apply()
    }

    fun zayifNoktalar(context: Context, lessonId: Long): List<String> {
        val dizi = zayifKok(context).optJSONArray(lessonId.toString()) ?: return emptyList()
        val liste = mutableListOf<String>()
        for (i in 0 until dizi.length()) {
            dizi.optString(i).takeIf { it.isNotBlank() }?.let { liste.add(it) }
        }
        return liste
    }

    fun zayifTemizle(context: Context, lessonId: Long) {
        val kok = zayifKok(context)
        kok.remove(lessonId.toString())
        prefs(context).edit().putString(K_ZAYIF, kok.toString()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME (Store.exportJson sürüm 12)
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject {
        val p = prefs(context)
        return JSONObject()
            .put("oturumlar", p.getString(K_OTURUMLAR, "[]"))
            .put("seviye", p.getString(K_SEVIYE, "{}"))
            .put("zayif", p.getString(K_ZAYIF, "{}"))
    }

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("oturumlar")) e.putString(K_OTURUMLAR, o.optString("oturumlar", "[]"))
            if (o.has("seviye")) e.putString(K_SEVIYE, o.optString("seviye", "{}"))
            if (o.has("zayif")) e.putString(K_ZAYIF, o.optString("zayif", "{}"))
            e.apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Öğretmen verisi içe aktarılamadı", e)
        }
    }
}
