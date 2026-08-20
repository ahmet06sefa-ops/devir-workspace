package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.39 — Fitness & Egzersiz motoru.
 *
 * Veri kaynağı: **free-exercise-db** (MIT lisanslı açık kaynak egzersiz
 * veritabanı — ~873 egzersiz). JSON, `assets/egzersizler/exercises.json`
 * olarak gömülür ve buradan okunur → **çevrimdışı** çalışır.
 *
 * ── Sorumluluklar ──
 *  · JSON veritabanını assets'ten okuyup `Egzersiz` listesine çevirir.
 *  · Kas grubu / ekipman / arama filtresini sağlar.
 *  · İngilizce veritabanını Türkçe etiketlere çevirir.
 *  · Antrenman kayıtlarını (set/tekrar/ağırlık) SharedPreferences/JSON
 *    ile kalıcı tutar.
 *
 * ── Not ──
 *  · Talimat metinleri veritabanı İngilizce olduğu için İngilizce kalır;
 *    isim, kas grubu, ekipman Türkçe gösterilir.
 */
object FitnessMotor {

    private const val TAG = "FitnessMotor"
    private const val ASSET = "egzersizler/exercises.json"
    private const val PREF = "fitness_v1"
    private const val K_ANTREMANLAR = "antrenmanlar_json"
    private const val TAVAN = 2000

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Türkçe çeviriler
    // ══════════════════════════════════════════════════════════

    private val KAS_TR = mapOf(
        "abdominals" to "Karın",
        "abductors" to "Kaçıranlar",
        "adductors" to "Yaklaştırıcılar",
        "biceps" to "Biceps",
        "calves" to "Baldır",
        "chest" to "Göğüs",
        "forearms" to "Ön Kol",
        "glutes" to "Kalça",
        "hamstrings" to "Arka Bacak",
        "lats" to "Sırt Kanadı",
        "lower back" to "Bel",
        "middle back" to "Orta Sırt",
        "neck" to "Boyun",
        "quadriceps" to "Ön Bacak",
        "shoulders" to "Omuz",
        "traps" to "Trapez",
        "triceps" to "Triceps"
    )

    private val EKIPMAN_TR = mapOf(
        "body only" to "Vücut Ağırlığı",
        "dumbbell" to "Dambıl",
        "barbell" to "Halter",
        "kettlebells" to "Kettlebell",
        "machine" to "Makine",
        "cable" to "Kablo",
        "exercise ball" to "Egzersiz Topu",
        "medicine ball" to "Sağlık Topu",
        "bands" to "Direnç Bandı",
        "foam roll" to "Köpük Rulo",
        "e-z curl bar" to "E-Z Bar",
        "other" to "Diğer",
        "assisted" to "Yardımlı",
        "rope" to "İp",
        "none" to "Yok"
    )

    private val SEVIYE_TR = mapOf(
        "beginner" to "Başlangıç",
        "intermediate" to "Orta",
        "expert" to "İleri"
    )

    private val KATEGORI_TR = mapOf(
        "strength" to "Kuvvet",
        "stretching" to "Esneme",
        "plyometrics" to "Pliometrik",
        "cardio" to "Kardiyo",
        "powerlifting" to "Powerlifting",
        "strongman" to "Strongman",
        "olympic weightlifting" to "Olimpik Halter"
    )

    /** Her kas grubu için bir emoji (görsel dosyası olmadığı için). */
    private val KAS_EMOJI = mapOf(
        "abdominals" to "🫃", "abductors" to "🦵", "adductors" to "🦵",
        "biceps" to "💪", "calves" to "🦵", "chest" to "🫀",
        "forearms" to "💪", "glutes" to "🍑", "hamstrings" to "🦵",
        "lats" to "🏋️", "lower back" to "🫀", "middle back" to "🏋️",
        "neck" to "🧍", "quadriceps" to "🦵", "shoulders" to "🏋️",
        "traps" to "🏋️", "triceps" to "💪"
    )

    fun kasTuru(kod: String): String = KAS_TR[kod] ?: kod
    fun ekipmanTuru(kod: String?): String = EKIPMAN_TR[kod?.lowercase(Locale.ROOT)?.trim() ?: ""] ?: (kod ?: "—")
    fun seviyeTuru(kod: String): String = SEVIYE_TR[kod] ?: kod
    fun kategoriTuru(kod: String): String = KATEGORI_TR[kod] ?: kod
    fun kasEmoji(kod: String): String = KAS_EMOJI[kod] ?: "🏋️"

    /** Sıralı Türkçe kas grubu listesi (UI için). */
    val kasGruplari: List<String> = listOf(
        "abdominals", "abductors", "adductors", "biceps", "calves", "chest",
        "forearms", "glutes", "hamstrings", "lats", "lower back", "middle back",
        "neck", "quadriceps", "shoulders", "traps", "triceps"
    )

    val ekipmanlar: List<String> = listOf(
        "body only", "dumbbell", "barbell", "kettlebells", "machine",
        "cable", "exercise ball", "medicine ball", "bands", "foam roll",
        "e-z curl bar", "other"
    )

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    data class Egzersiz(
        val id: String,
        val isim: String,
        val kaslar: List<String>,
        val ikincilKaslar: List<String>,
        val ekipman: String?,
        val seviye: String,
        val kategori: String,
        val mekanik: String?,
        val talimatlar: List<String>
    )

    data class AntrenmanSeti(val tekrar: Int, val agirlik: Double, val dinlenmeDk: Int = 0)

    data class AntrenmanKaydi(
        val egzersizId: String,
        val egzersizAdi: String,
        val kasKod: String,
        val setler: List<AntrenmanSeti>,
        val tarih: Long
    )

    // ══════════════════════════════════════════════════════════
    // Veritabanı
    // ══════════════════════════════════════════════════════════

    @Volatile private var onbellek: List<Egzersiz>? = null

    /** Assets'ten tüm egzersizleri okur (önbellekli). */
    fun tumu(context: Context): List<Egzersiz> {
        onbellek?.let { return it }
        val liste = runCatching {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            val dizi = JSONArray(json)
            (0 until dizi.length()).mapNotNull { i ->
                val o = dizi.getJSONObject(i)
                try {
                    Egzersiz(
                        id = o.optString("id", i.toString()),
                        isim = o.optString("name"),
                        kaslar = o.optJSONArray("primaryMuscles").toStringList(),
                        ikincilKaslar = o.optJSONArray("secondaryMuscles").toStringList(),
                        ekipman = o.optString("equipment").ifBlank { null },
                        seviye = o.optString("level"),
                        kategori = o.optString("category"),
                        mekanik = o.optString("mechanic").ifBlank { null },
                        talimatlar = o.optJSONArray("instructions").toStringList()
                    )
                } catch (_: Exception) { null }
            }
        }.getOrDefault(emptyList())
        onbellek = liste
        return liste
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i -> optString(i).takeIf { it.isNotBlank() } }
    }

    /** Kas grubuna göre filtreler. kasKod boşsa hepsini verir. */
    fun kasGrubunaGore(liste: List<Egzersiz>, kasKod: String?): List<Egzersiz> =
        if (kasKod.isNullOrBlank()) liste
        else liste.filter { it.kaslar.contains(kasKod) }

    /** Ekipmana göre filtreler. ekipman boşsa hepsini verir. */
    fun ekipmanaGore(liste: List<Egzersiz>, ekipman: String?): List<Egzersiz> =
        if (ekipman.isNullOrBlank()) liste
        else liste.filter { it.ekipman?.lowercase(Locale.ROOT) == ekipman.lowercase(Locale.ROOT) }

    /** İsim üzerinde büyük/küçük harf duyarsız arama. */
    fun ara(liste: List<Egzersiz>, sorgu: String): List<Egzersiz> {
        val q = sorgu.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return liste
        return liste.filter { it.isim.lowercase(Locale.ROOT).contains(q) }
    }

    fun egzersizById(context: Context, id: String): Egzersiz? =
        tumu(context).firstOrNull { it.id == id }

    /** Bir egzersizin UI'da gösterilecek kısa özeti (kas + ekipman). */
    fun ozet(e: Egzersiz): String {
        val kas = e.kaslar.joinToString(", ") { kasTuru(it) }
        return listOfNotNull(
            kas.takeIf { it.isNotBlank() },
            ekipmanTuru(e.ekipman)
        ).joinToString(" · ")
    }

    // ══════════════════════════════════════════════════════════
    // Antrenman günlüğü (kalıcı)
    // ══════════════════════════════════════════════════════════

    fun antrenmanlar(context: Context): List<AntrenmanKaydi> = runCatching {
        val ham = p(context).getString(K_ANTREMANLAR, null) ?: return emptyList()
        val dizi = JSONArray(ham)
        (0 until dizi.length()).mapNotNull { i ->
            val o = dizi.getJSONObject(i)
            try {
                val setler = o.optJSONArray("setler").toStringList().mapNotNull { satir ->
                    // "tekrar|agirlik|dinlenmeDk"
                    val b = satir.split("|")
                    if (b.size < 2) null
                    else AntrenmanSeti(b[0].toIntOrNull() ?: 0, b[1].toDoubleOrNull() ?: 0.0, b.getOrNull(2)?.toIntOrNull() ?: 0)
                }
                AntrenmanKaydi(
                    egzersizId = o.optString("id"),
                    egzersizAdi = o.optString("ad"),
                    kasKod = o.optString("kas"),
                    setler = setler,
                    tarih = o.optLong("tarih")
                )
            } catch (_: Exception) { null }
        }
    }.getOrDefault(emptyList())

    fun antrenmanEkle(context: Context, kayit: AntrenmanKaydi) {
        val liste = antrenmanlar(context).toMutableList()
        liste.add(kayit)
        if (liste.size > TAVAN) liste.removeAt(0)

        val dizi = JSONArray()
        liste.forEach { k ->
            val setler = JSONArray()
            k.setler.forEach { s -> setler.put("${s.tekrar}|${s.agirlik}|${s.dinlenmeDk}") }
            dizi.put(JSONObject().apply {
                put("id", k.egzersizId)
                put("ad", k.egzersizAdi)
                put("kas", k.kasKod)
                put("setler", setler)
                put("tarih", k.tarih)
            })
        }
        p(context).edit().putString(K_ANTREMANLAR, dizi.toString()).apply()
    }

    fun antrenmanSil(context: Context, index: Int) {
        val liste = antrenmanlar(context).toMutableList()
        if (index in liste.indices) liste.removeAt(index)
        // Yeniden yaz
        val dizi = JSONArray()
        liste.forEach { k ->
            val setler = JSONArray()
            k.setler.forEach { s -> setler.put("${s.tekrar}|${s.agirlik}|${s.dinlenmeDk}") }
            dizi.put(JSONObject().apply {
                put("id", k.egzersizId); put("ad", k.egzersizAdi); put("kas", k.kasKod)
                put("setler", setler); put("tarih", k.tarih)
            })
        }
        p(context).edit().putString(K_ANTREMANLAR, dizi.toString()).apply()
    }

    /** Bugün yapılan toplam set sayısı (günlük özet için). */
    fun bugunToplamSet(context: Context): Int {
        val gun = gunAnahtari()
        return antrenmanlar(context).count { gunAnahtari(it.tarih) == gun }
    }

    /** Belirli bir egzersizin toplam yapılma sayısı (istatistik için). */
    fun egzersizYapilmaSayisi(context: Context, egzersizId: String): Int =
        antrenmanlar(context).count { it.egzersizId == egzersizId }

    fun gunAnahtari(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}
