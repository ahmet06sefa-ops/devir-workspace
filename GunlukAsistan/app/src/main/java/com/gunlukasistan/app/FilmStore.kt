package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.49 — Günlük dizi/film önerisi veri katmanı.
 *
 * ── Veri kaynağı: iki katmanlı ──
 *  1. TMDb API (varsa) — gerçek puan, poster, oyuncu, platform bilgisi
 *  2. Yapay zekâ (yedek) — anahtar yoksa Gemini'den öneri
 *
 * Kullanıcı "ikisi birden" dedi: TMDb anahtarı varsa gerçek veri gelir,
 * yoksa yapay zekâya düşülür. Böylece anahtar almadan da çalışır.
 *
 * ── Neden TMDb? ──
 * IMDb'nin halka açık ücretsiz API'si yok. TMDb ücretsiz, Türkçe destekli
 * ve IMDb kimliğini de veriyor — böylece IMDb sayfasına link kurulabiliyor.
 */
object FilmStore {

    private const val TAG = "FilmStore"
    private const val PREF = "film_v1"

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir film veya dizi.
     *
     * @param tur "film" veya "dizi"
     * @param puan 0-10 arası, TMDb oy ortalaması
     * @param imdbId IMDb sayfası için (tt1234567)
     * @param platformlar izlenebilecek yasal servisler
     */
    data class Yapim(
        val id: Long,
        val ad: String,
        val orijinalAd: String = "",
        val tur: String = "film",
        val yil: String = "",
        val puan: Double = 0.0,
        val oySayisi: Int = 0,
        val ozet: String = "",
        val turler: String = "",
        val sure: String = "",
        val yonetmen: String = "",
        val oyuncular: String = "",
        val posterUrl: String = "",
        val tmdbId: Int = 0,
        val imdbId: String = "",
        val platformlar: String = "",
        /** Kullanıcı işaretleri */
        var izlendi: Boolean = false,
        var listede: Boolean = false,
        var eklendi: Long = 0L
    ) {
        val puanMetni: String
            get() = if (puan <= 0.0) "—" else String.format(Locale.US, "%.1f", puan)

        val basligiTam: String
            get() = if (yil.isBlank()) ad else ad + " (" + yil + ")"
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugunKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    /** TMDb API anahtarı — boşsa yapay zekâya düşülür. */
    fun tmdbAnahtar(context: Context): String =
        prefs(context).getString("tmdb_key", "") ?: ""

    fun setTmdbAnahtar(context: Context, k: String) {
        prefs(context).edit().putString("tmdb_key", k.trim()).apply()
    }

    fun tmdbVarMi(context: Context): Boolean = tmdbAnahtar(context).isNotBlank()

    /** Tercih edilen tür: hepsi / film / dizi */
    fun turTercihi(context: Context): String =
        prefs(context).getString("tur", "hepsi") ?: "hepsi"

    fun setTurTercihi(context: Context, t: String) {
        prefs(context).edit().putString("tur", t).apply()
    }

    /** Sevilen türler (aksiyon, komedi…) — öneri kişiselleşsin diye. */
    fun sevilenTurler(context: Context): String =
        prefs(context).getString("sevilen", "") ?: ""

    fun setSevilenTurler(context: Context, t: String) {
        prefs(context).edit().putString("sevilen", t.trim()).apply()
    }

    /** İndirme/izleme kalitesi tercihi. */
    fun kalite(context: Context): String =
        prefs(context).getString("kalite", "1080p") ?: "1080p"

    fun setKalite(context: Context, k: String) {
        prefs(context).edit().putString("kalite", k).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜNÜN ÖNERİSİ — önbellek
    // ═══════════════════════════════════════════════════════════════

    /**
     * Günün önerileri diskte saklanır: aynı gün tekrar açılınca
     * yeniden API çağrısı yapılmaz, kota ve pil harcanmaz.
     */
    fun gununOnerileri(context: Context): List<Yapim> {
        val p = prefs(context)
        if (p.getString("gun", "") != bugunKey()) return emptyList()
        return listeCoz(p.getString("gunun", "[]") ?: "[]")
    }

    fun gununOnerileriniKaydet(context: Context, liste: List<Yapim>) {
        prefs(context).edit()
            .putString("gun", bugunKey())
            .putString("gunun", listeKodla(liste))
            .apply()
    }

    /** Bugün için öneri üretilmiş mi? */
    fun bugunVarMi(context: Context): Boolean = gununOnerileri(context).isNotEmpty()

    // ═══════════════════════════════════════════════════════════════
    // İZLEME LİSTESİ
    // ═══════════════════════════════════════════════════════════════

    fun listeyiYukle(context: Context): MutableList<Yapim> =
        listeCoz(prefs(context).getString("liste", "[]") ?: "[]").toMutableList()

    fun listeyiKaydet(context: Context, liste: List<Yapim>) {
        prefs(context).edit().putString("liste", listeKodla(liste)).apply()
    }

    /** Listeye ekler veya çıkarır. @return listede mi */
    fun listeyeAlDegistir(context: Context, y: Yapim): Boolean {
        val liste = listeyiYukle(context)
        val mevcut = liste.firstOrNull { it.ad == y.ad && it.yil == y.yil }
        return if (mevcut != null) {
            liste.remove(mevcut)
            listeyiKaydet(context, liste)
            false
        } else {
            liste.add(0, y.copy(listede = true, eklendi = System.currentTimeMillis()))
            listeyiKaydet(context, liste)
            true
        }
    }

    fun listedeMi(context: Context, y: Yapim): Boolean =
        listeyiYukle(context).any { it.ad == y.ad && it.yil == y.yil }

    /** İzlendi işaretini çevirir. */
    fun izlendiDegistir(context: Context, y: Yapim): Boolean {
        val liste = listeyiYukle(context)
        val i = liste.indexOfFirst { it.ad == y.ad && it.yil == y.yil }
        if (i < 0) {
            liste.add(0, y.copy(izlendi = true, listede = true,
                eklendi = System.currentTimeMillis()))
            listeyiKaydet(context, liste)
            return true
        }
        liste[i].izlendi = !liste[i].izlendi
        listeyiKaydet(context, liste)
        return liste[i].izlendi
    }

    fun izlendiMi(context: Context, y: Yapim): Boolean =
        listeyiYukle(context).firstOrNull { it.ad == y.ad && it.yil == y.yil }?.izlendi == true

    // ═══════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════

    private fun listeKodla(liste: List<Yapim>): String {
        val dizi = JSONArray()
        liste.forEach { y ->
            dizi.put(
                JSONObject()
                    .put("id", y.id).put("ad", y.ad).put("oad", y.orijinalAd)
                    .put("tur", y.tur).put("yil", y.yil)
                    .put("puan", y.puan).put("oy", y.oySayisi)
                    .put("ozet", y.ozet).put("turler", y.turler)
                    .put("sure", y.sure).put("yonetmen", y.yonetmen)
                    .put("oyuncular", y.oyuncular).put("poster", y.posterUrl)
                    .put("tmdb", y.tmdbId).put("imdb", y.imdbId)
                    .put("platform", y.platformlar)
                    .put("izlendi", y.izlendi).put("listede", y.listede)
                    .put("eklendi", y.eklendi)
            )
        }
        return dizi.toString()
    }

    private fun listeCoz(json: String): List<Yapim> {
        val liste = mutableListOf<Yapim>()
        try {
            val dizi = JSONArray(json)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Yapim(
                        id = o.optLong("id"),
                        ad = o.optString("ad"),
                        orijinalAd = o.optString("oad"),
                        tur = o.optString("tur", "film"),
                        yil = o.optString("yil"),
                        puan = o.optDouble("puan", 0.0),
                        oySayisi = o.optInt("oy"),
                        ozet = o.optString("ozet"),
                        turler = o.optString("turler"),
                        sure = o.optString("sure"),
                        yonetmen = o.optString("yonetmen"),
                        oyuncular = o.optString("oyuncular"),
                        posterUrl = o.optString("poster"),
                        tmdbId = o.optInt("tmdb"),
                        imdbId = o.optString("imdb"),
                        platformlar = o.optString("platform"),
                        izlendi = o.optBoolean("izlendi"),
                        listede = o.optBoolean("listede"),
                        eklendi = o.optLong("eklendi")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Liste çözülemedi", e)
        }
        return liste
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("liste", prefs(context).getString("liste", "[]"))
        .put("tur", turTercihi(context))
        .put("sevilen", sevilenTurler(context))
        .put("kalite", kalite(context))
    // Not: TMDb anahtarı güvenlik gereği yedeğe girmez

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("liste")) e.putString("liste", o.optString("liste", "[]"))
            if (o.has("tur")) e.putString("tur", o.optString("tur", "hepsi"))
            if (o.has("sevilen")) e.putString("sevilen", o.optString("sevilen", ""))
            if (o.has("kalite")) e.putString("kalite", o.optString("kalite", "1080p"))
            e.apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İçe aktarılamadı", e)
        }
    }
}
