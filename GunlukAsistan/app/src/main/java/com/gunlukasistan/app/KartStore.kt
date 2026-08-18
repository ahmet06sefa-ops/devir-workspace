package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v7.33 — Bilgi kartları (flashcard) veri katmanı.
 *
 * Quiz'den farkı: kartlar **hızlı tekrar** içindir. Şık yok, sadece
 * ön yüz (soru) ve arka yüz (cevap). Boş 5 dakikada 20 kart çevirirsin.
 *
 * Kendi Leitner sistemi var — QuizStore'dan bağımsız, çünkü kartlar
 * ders bazlı değil **bilgi bazlı** ilerler ve çok daha sık tekrarlanır.
 *
 * Aralıklar quiz'e göre daha sıkı: 1, 2, 4, 8, 16, 45 gün
 */
object KartStore {

    private const val TAG = "KartStore"
    private const val PREF = "kart_store"
    private const val K_KARTLAR = "kartlar_json"
    private const val K_DURUM = "durum_json"
    private const val K_YUKLENEN = "hazir_desteler"

    private val ARALIKLAR = intArrayOf(1, 2, 4, 8, 16, 45)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun gunEkle(gun: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, gun)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    // ═══════════════════════════════════════════════════════════════
    // MODELLER
    // ═══════════════════════════════════════════════════════════════

    /** Tek bir bilgi kartı. */
    data class Kart(
        val id: Long,
        val deste: String,
        val on: String,
        val arka: String,
        val ipucu: String = "",
        val kendi: Boolean = false
    ) {
        val gecerli: Boolean get() = on.isNotBlank() && arka.isNotBlank()
    }

    /** Kartın tekrar durumu. */
    data class Durum(
        val kartId: Long,
        var kutu: Int = 0,
        var sonrakiGun: String = "",
        var dogru: Int = 0,
        var yanlis: Int = 0
    ) {
        val ogrenildi: Boolean get() = kutu >= ARALIKLAR.size - 1
        val basariYuzde: Int
            get() {
                val t = dogru + yanlis
                return if (t == 0) 0 else dogru * 100 / t
            }
    }

    /** Deste özeti — liste ekranı için. */
    data class DesteOzet(
        val ad: String,
        val simge: String,
        val toplam: Int,
        val ogrenilen: Int,
        val bekleyen: Int
    ) {
        val yuzde: Int get() = if (toplam == 0) 0 else ogrenilen * 100 / toplam
    }

    // ═══════════════════════════════════════════════════════════════
    // KART DEPOSU
    // ═══════════════════════════════════════════════════════════════

    fun kartlariYukle(context: Context): MutableList<Kart> {
        val ham = prefs(context).getString(K_KARTLAR, "[]") ?: "[]"
        val liste = mutableListOf<Kart>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Kart(
                        id = o.optLong("id"),
                        deste = o.optString("deste"),
                        on = o.optString("on"),
                        arka = o.optString("arka"),
                        ipucu = o.optString("ipucu", ""),
                        kendi = o.optBoolean("kendi", false)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kartlar okunamadı", e)
        }
        return liste
    }

    fun kartlariKaydet(context: Context, liste: List<Kart>) {
        try {
            val dizi = JSONArray()
            liste.forEach { k ->
                dizi.put(
                    JSONObject()
                        .put("id", k.id).put("deste", k.deste)
                        .put("on", k.on).put("arka", k.arka)
                        .put("ipucu", k.ipucu).put("kendi", k.kendi)
                )
            }
            prefs(context).edit().putString(K_KARTLAR, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kartlar kaydedilemedi", e)
        }
    }

    fun kartEkle(context: Context, deste: String, on: String, arka: String, ipucu: String = "") {
        val liste = kartlariYukle(context)
        liste.add(
            Kart(
                id = System.currentTimeMillis() + liste.size,
                deste = deste.trim().ifBlank { "Kendi kartlarım" },
                on = on.trim(), arka = arka.trim(), ipucu = ipucu.trim(),
                kendi = true
            )
        )
        kartlariKaydet(context, liste)
    }

    fun kartSil(context: Context, kartId: Long) {
        kartlariKaydet(context, kartlariYukle(context).filterNot { it.id == kartId })
        val d = durumlariYukle(context).filterNot { it.kartId == kartId }
        durumlariKaydet(context, d)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEKRAR DURUMU
    // ═══════════════════════════════════════════════════════════════

    fun durumlariYukle(context: Context): MutableList<Durum> {
        val ham = prefs(context).getString(K_DURUM, "[]") ?: "[]"
        val liste = mutableListOf<Durum>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Durum(
                        kartId = o.optLong("id"),
                        kutu = o.optInt("kutu", 0),
                        sonrakiGun = o.optString("sonraki", ""),
                        dogru = o.optInt("d", 0),
                        yanlis = o.optInt("y", 0)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Durumlar okunamadı", e)
        }
        return liste
    }

    fun durumlariKaydet(context: Context, liste: List<Durum>) {
        try {
            val dizi = JSONArray()
            liste.forEach { d ->
                dizi.put(
                    JSONObject()
                        .put("id", d.kartId).put("kutu", d.kutu)
                        .put("sonraki", d.sonrakiGun)
                        .put("d", d.dogru).put("y", d.yanlis)
                )
            }
            prefs(context).edit().putString(K_DURUM, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Durumlar kaydedilemedi", e)
        }
    }

    /**
     * Kart cevabını işler.
     * @param biliyorum kullanıcı bildiğini söylediyse true
     */
    fun cevapla(context: Context, kartId: Long, biliyorum: Boolean) {
        val liste = durumlariYukle(context)
        val d = liste.firstOrNull { it.kartId == kartId }
            ?: Durum(kartId).also { liste.add(it) }

        if (biliyorum) {
            d.kutu = (d.kutu + 1).coerceAtMost(ARALIKLAR.size - 1)
            d.dogru++
        } else {
            // Bilmiyorsa başa dön — yarın tekrar sor
            d.kutu = 0
            d.yanlis++
        }
        d.sonrakiGun = gunEkle(ARALIKLAR[d.kutu])
        durumlariKaydet(context, liste)
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇALIŞMA OTURUMU
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bugün çalışılacak kartları getirir.
     *
     * Öncelik: (1) tekrarı gelmiş kartlar, (2) hiç görülmemiş yeni kartlar.
     * @param deste boşsa tüm desteler
     * @param limit en fazla kaç kart
     */
    fun bugunkuKartlar(context: Context, deste: String = "", limit: Int = 20): List<Kart> {
        val kartlar = kartlariYukle(context)
            .filter { it.gecerli && (deste.isBlank() || it.deste == deste) }
        if (kartlar.isEmpty()) return emptyList()

        val durumlar = durumlariYukle(context).associateBy { it.kartId }
        val b = bugun()

        val vadesi = mutableListOf<Kart>()
        val yeni = mutableListOf<Kart>()

        kartlar.forEach { k ->
            val d = durumlar[k.id]
            when {
                d == null -> yeni.add(k)
                d.ogrenildi -> { /* öğrenildi, atla */ }
                d.sonrakiGun <= b -> vadesi.add(k)
            }
        }
        // Önce vadesi gelenler, sonra yeniler
        return (vadesi.shuffled() + yeni.shuffled()).take(limit)
    }

    fun bekleyenSayisi(context: Context, deste: String = ""): Int =
        bugunkuKartlar(context, deste, 9999).size

    /** Deste listesi — özet bilgilerle. */
    fun desteler(context: Context): List<DesteOzet> {
        val kartlar = kartlariYukle(context).filter { it.gecerli }
        if (kartlar.isEmpty()) return emptyList()
        val durumlar = durumlariYukle(context).associateBy { it.kartId }
        val b = bugun()

        return kartlar.groupBy { it.deste }.map { (ad, grup) ->
            var ogrenilen = 0
            var bekleyen = 0
            grup.forEach { k ->
                val d = durumlar[k.id]
                when {
                    d == null -> bekleyen++
                    d.ogrenildi -> ogrenilen++
                    d.sonrakiGun <= b -> bekleyen++
                }
            }
            DesteOzet(ad, HazirDesteler.simge(ad), grup.size, ogrenilen, bekleyen)
        }.sortedByDescending { it.bekleyen }
    }

    fun toplamKart(context: Context): Int = kartlariYukle(context).count { it.gecerli }

    fun ogrenilenToplam(context: Context): Int =
        durumlariYukle(context).count { it.ogrenildi }

    // ═══════════════════════════════════════════════════════════════
    // HAZIR DESTELER
    // ═══════════════════════════════════════════════════════════════

    /** Hazır desteler bir kez yüklenir; kullanıcı silerse geri gelmez. */
    fun hazirDesteleriYukle(context: Context): Int {
        val p = prefs(context)
        if (p.getBoolean(K_YUKLENEN, false)) return 0

        val mevcut = kartlariYukle(context)
        var eklenen = 0
        HazirDesteler.TUMU.forEach { (deste, kartlar) ->
            kartlar.forEach { (on, arka) ->
                mevcut.add(
                    Kart(
                        id = System.nanoTime() + eklenen,
                        deste = deste, on = on, arka = arka, kendi = false
                    )
                )
                eklenen++
            }
        }
        kartlariKaydet(context, mevcut)
        p.edit().putBoolean(K_YUKLENEN, true).apply()
        return eklenen
    }

    fun hazirYuklendiMi(context: Context): Boolean =
        prefs(context).getBoolean(K_YUKLENEN, false)

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("kartlar", prefs(context).getString(K_KARTLAR, "[]"))
        .put("durum", prefs(context).getString(K_DURUM, "[]"))
        .put("hazir", prefs(context).getBoolean(K_YUKLENEN, false))

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("kartlar")) e.putString(K_KARTLAR, o.optString("kartlar", "[]"))
            if (o.has("durum")) e.putString(K_DURUM, o.optString("durum", "[]"))
            if (o.has("hazir")) e.putBoolean(K_YUKLENEN, o.optBoolean("hazir", false))
            e.apply()
        } catch (ex: Exception) {
            android.util.Log.w(TAG, "Kart verisi geri yüklenemedi", ex)
        }
    }
}
