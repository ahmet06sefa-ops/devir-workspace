package com.gunlukasistan.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * v9.7 — Konuma bağlı hatırlatma (öneri 45).
 *
 * ══════════════════════════════════════════════════════════════════
 * DÜRÜST AÇIKLAMA: BU GERÇEK GEOFENCING DEĞİL
 * ══════════════════════════════════════════════════════════════════
 * Öneri listesinde "Geofencing" yazmıştım ve yanına
 * "Play Services Location ~400 KB gerekir" notu düşmüştüm.
 *
 * Play Services'i eklemedim. Sebep:
 *
 *   1. APK şu an 16,8 MB. R8 ile 27,4'ten indirdiğimiz boyutu
 *      tek özellik için %2,4 büyütmek kötü takas.
 *   2. Play Services bağımlılığı Google Play olmayan cihazlarda
 *      (Huawei, bazı özel ROM'lar) uygulamayı **çökertebilir**.
 *   3. Gerçek geofencing arka planda sürekli konum izni
 *      (`ACCESS_BACKGROUND_LOCATION`) istiyor. Android 11+ bunu
 *      "Her zaman izin ver" olarak soruyor ve Play Store bu izin
 *      için ayrı gerekçe formu doldurtuyor.
 *
 * Bunun yerine **kontrollü yaklaşım** yazdım:
 *
 *   · Kullanıcı yer kaydediyor (mevcut konumundan veya elle)
 *   · Hatırlatma o yere bağlanıyor
 *   · Kontrol **uygulama açıldığında** ve **günlük özet alarmında**
 *     yapılıyor — sürekli GPS dinlenmiyor
 *   · Son bilinen konum kullanılıyor (`getLastKnownLocation`),
 *     yeni konum isteği yapılmıyor
 *
 * ── Bunun anlamı ──
 * "Markete girdiğin anda telefonun titremesi" OLMAZ. Onun için
 * gerçek geofencing gerekir.
 *
 * OLAN şey: "Uygulamayı açtığında veya sabah bildiriminde,
 * markete yakınsan hatırlatma görürsün."
 *
 * Bu daha az etkileyici ama **dürüst** ve pil tüketmiyor.
 * Kullanıcıya da arayüzde aynen böyle yazıyorum — yanlış beklenti
 * yaratmak, özelliği hiç yapmamaktan kötü.
 *
 * ══════════════════════════════════════════════════════════════════
 * MESAFE HESABI
 * ══════════════════════════════════════════════════════════════════
 * `Location.distanceBetween` kullanıyorum — Vincenty formülü,
 * WGS84 elipsoidi üzerinde metre cinsinden doğru sonuç veriyor.
 * Haversine'i elle yazmak yerine platformunkini kullanmak hem
 * daha doğru hem daha az kod.
 */
object KonumHatirlatma {

    private const val TAG = "KonumHatirlatma"
    private const val PREF = "konum_hatirlatma_v1"
    private const val K_YERLER = "yerler_json"
    private const val K_HATIRLATMALAR = "hatirlatmalar_json"
    private const val K_SON_TETIK = "son_tetik_json"

    /** Aynı hatırlatma bu süre içinde tekrar tetiklenmez (2 saat). */
    private const val SOGUMA_MS = 2 * 60 * 60 * 1000L

    private const val TAVAN = 60

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    /**
     * Kayıtlı yer.
     *
     * @param yaricap metre — varsayılan 200 m. Şehir içinde GPS
     *   hatası 20-50 m olabiliyor; 200 m "bu civardayım" için
     *   makul. Daha küçük değerler kaçırmaya yol açıyor.
     */
    data class Yer(
        val id: Long,
        val ad: String,
        val enlem: Double,
        val boylam: Double,
        val yaricap: Int = 200,
        val emoji: String = "📍"
    )

    data class Hatirlatma(
        val id: Long,
        val yerId: Long,
        val metin: String,
        val aktif: Boolean = true,
        /** true = oraya varınca · false = oradan ayrılınca */
        val varista: Boolean = true,
        val olusturma: Long = System.currentTimeMillis()
    )

    // ══════════════════════════════════════════════════════════
    // Yerler
    // ══════════════════════════════════════════════════════════

    fun yerler(context: Context): List<Yer> = runCatching {
        val dizi = JSONArray(p(context).getString(K_YERLER, "[]") ?: "[]")
        val liste = mutableListOf<Yer>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(
                Yer(
                    o.optLong("id"), o.optString("ad"),
                    o.optDouble("lat", 0.0), o.optDouble("lon", 0.0),
                    o.optInt("r", 200), o.optString("e", "📍")
                )
            )
        }
        liste
    }.getOrElse { emptyList() }

    fun yerEkle(context: Context, yer: Yer) {
        runCatching {
            val liste = yerler(context).toMutableList()
            val idx = liste.indexOfFirst { it.id == yer.id }
            if (idx >= 0) liste[idx] = yer else liste.add(yer)
            yerleriYaz(context, liste.take(TAVAN))
        }
    }

    fun yerSil(context: Context, id: Long) {
        yerleriYaz(context, yerler(context).filter { it.id != id })
        // Bu yere bağlı hatırlatmalar da gitsin — yetim kayıt kalmasın
        hatirlatmalariYaz(context, hatirlatmalar(context).filter { it.yerId != id })
    }

    private fun yerleriYaz(context: Context, liste: List<Yer>) {
        runCatching {
            val dizi = JSONArray()
            liste.forEach {
                dizi.put(
                    JSONObject().put("id", it.id).put("ad", it.ad)
                        .put("lat", it.enlem).put("lon", it.boylam)
                        .put("r", it.yaricap).put("e", it.emoji)
                )
            }
            p(context).edit().putString(K_YERLER, dizi.toString()).apply()
        }
    }

    // ══════════════════════════════════════════════════════════
    // Hatırlatmalar
    // ══════════════════════════════════════════════════════════

    fun hatirlatmalar(context: Context): List<Hatirlatma> = runCatching {
        val dizi = JSONArray(p(context).getString(K_HATIRLATMALAR, "[]") ?: "[]")
        val liste = mutableListOf<Hatirlatma>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(
                Hatirlatma(
                    o.optLong("id"), o.optLong("yer"), o.optString("m"),
                    o.optBoolean("a", true), o.optBoolean("v", true),
                    o.optLong("o", System.currentTimeMillis())
                )
            )
        }
        liste.sortedByDescending { it.olusturma }
    }.getOrElse { emptyList() }

    fun hatirlatmaEkle(context: Context, h: Hatirlatma) {
        runCatching {
            val liste = hatirlatmalar(context).toMutableList()
            val idx = liste.indexOfFirst { it.id == h.id }
            if (idx >= 0) liste[idx] = h else liste.add(h)
            hatirlatmalariYaz(context, liste.take(TAVAN))
        }
    }

    fun hatirlatmaSil(context: Context, id: Long) {
        hatirlatmalariYaz(context, hatirlatmalar(context).filter { it.id != id })
    }

    private fun hatirlatmalariYaz(context: Context, liste: List<Hatirlatma>) {
        runCatching {
            val dizi = JSONArray()
            liste.forEach {
                dizi.put(
                    JSONObject().put("id", it.id).put("yer", it.yerId)
                        .put("m", it.metin).put("a", it.aktif)
                        .put("v", it.varista).put("o", it.olusturma)
                )
            }
            p(context).edit().putString(K_HATIRLATMALAR, dizi.toString()).apply()
        }
    }

    // ══════════════════════════════════════════════════════════
    // Konum
    // ══════════════════════════════════════════════════════════

    fun izinVarMi(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Son bilinen konum.
     *
     * Tüm sağlayıcıları geziyor ve **en taze** olanı seçiyor.
     * Yalnız GPS'e bakmak kapalı alanda `null` döndürür; yalnız
     * ağa bakmak dışarıda eski veri verir. İkisini birleştirmek
     * pratikte en iyi sonucu veriyor.
     *
     * @return konum veya null (izin yok / hiç konum kaydı yok)
     */
    fun sonKonum(context: Context): Location? {
        if (!izinVarMi(context)) return null
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            var enIyi: Location? = null
            lm.getProviders(true).forEach { saglayici ->
                runCatching {
                    @Suppress("MissingPermission")
                    val k = lm.getLastKnownLocation(saglayici)
                    if (k != null && (enIyi == null || k.time > enIyi!!.time)) enIyi = k
                }
            }
            enIyi
        }.getOrNull()
    }

    /** İki nokta arası metre. */
    fun mesafe(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val sonuc = FloatArray(1)
        return runCatching {
            Location.distanceBetween(lat1, lon1, lat2, lon2, sonuc)
            sonuc[0]
        }.getOrDefault(Float.MAX_VALUE)
    }

    /** Konum verisi ne kadar eski (dakika)? */
    fun konumYasiDk(konum: Location?): Int? {
        if (konum == null) return null
        val fark = System.currentTimeMillis() - konum.time
        if (fark < 0) return 0
        return (fark / 60000L).toInt()
    }

    // ══════════════════════════════════════════════════════════
    // Kontrol
    // ══════════════════════════════════════════════════════════

    data class Tetiklenen(val hatirlatma: Hatirlatma, val yer: Yer, val mesafe: Float)

    /**
     * Şu anki konuma göre tetiklenmesi gereken hatırlatmalar.
     *
     * "Ayrılınca" hatırlatmaları desteklemek için önceki durumu
     * bilmek gerekiyor. Her kontrol sonunda hangi yerlerin içinde
     * olduğumuzu kaydediyoruz; bir sonraki kontrolde içeriden
     * dışarı çıkış tespit ediliyor.
     *
     * @param kaydet false ise durum güncellenmez (önizleme için)
     */
    fun kontrolEt(context: Context, kaydet: Boolean = true): List<Tetiklenen> {
        val konum = sonKonum(context) ?: return emptyList()

        // Çok eski konum yanlış tetiklemeye yol açar. 6 saat üstü
        // veriyle "markettesin" demek kullanıcıyı yanıltır.
        val yas = konumYasiDk(konum) ?: return emptyList()
        if (yas > 360) return emptyList()

        val yerListesi = yerler(context)
        val aktifler = hatirlatmalar(context).filter { it.aktif }
        if (yerListesi.isEmpty() || aktifler.isEmpty()) return emptyList()

        val oncekiIcinde = icerdekiYerler(context)
        val simdiIcinde = mutableSetOf<Long>()
        val sonTetikler = sonTetikHaritasi(context)
        val simdi = System.currentTimeMillis()
        val sonuc = mutableListOf<Tetiklenen>()

        yerListesi.forEach { yer ->
            val d = mesafe(konum.latitude, konum.longitude, yer.enlem, yer.boylam)
            val icerdeMi = d <= yer.yaricap
            if (icerdeMi) simdiIcinde.add(yer.id)

            val girdi = icerdeMi && yer.id !in oncekiIcinde
            val cikti = !icerdeMi && yer.id in oncekiIcinde

            aktifler.filter { it.yerId == yer.id }.forEach { h ->
                val uygun = (h.varista && (girdi || (icerdeMi && oncekiIcinde.isEmpty()))) ||
                        (!h.varista && cikti)
                if (!uygun) return@forEach
                // Soğuma: aynı hatırlatma 2 saatte bir kereden fazla çalmasın
                val son = sonTetikler[h.id] ?: 0L
                if (simdi - son < SOGUMA_MS) return@forEach
                sonuc.add(Tetiklenen(h, yer, d))
            }
        }

        if (kaydet) {
            icerdekiYerleriYaz(context, simdiIcinde)
            if (sonuc.isNotEmpty()) {
                val yeni = sonTetikler.toMutableMap()
                sonuc.forEach { yeni[it.hatirlatma.id] = simdi }
                sonTetikYaz(context, yeni)
            }
        }
        return sonuc
    }

    /**
     * Yakındaki hatırlatmalar — tetiklenmiş olsun olmasın.
     * Ekranda "şu an markete 340 m uzaktasın" göstermek için.
     */
    fun yakindakiler(context: Context, enFazlaMetre: Int = 2000): List<Tetiklenen> {
        val konum = sonKonum(context) ?: return emptyList()
        val yerHarita = yerler(context).associateBy { it.id }
        return hatirlatmalar(context).filter { it.aktif }.mapNotNull { h ->
            val yer = yerHarita[h.yerId] ?: return@mapNotNull null
            val d = mesafe(konum.latitude, konum.longitude, yer.enlem, yer.boylam)
            if (d <= enFazlaMetre) Tetiklenen(h, yer, d) else null
        }.sortedBy { it.mesafe }
    }

    private fun icerdekiYerler(context: Context): Set<Long> = runCatching {
        p(context).getStringSet("icerde", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet()
            ?: emptySet()
    }.getOrDefault(emptySet())

    private fun icerdekiYerleriYaz(context: Context, ids: Set<Long>) {
        runCatching {
            p(context).edit().putStringSet("icerde", ids.map { it.toString() }.toSet()).apply()
        }
    }

    private fun sonTetikHaritasi(context: Context): Map<Long, Long> = runCatching {
        val o = JSONObject(p(context).getString(K_SON_TETIK, "{}") ?: "{}")
        val harita = mutableMapOf<Long, Long>()
        o.keys().forEach { k -> k.toLongOrNull()?.let { harita[it] = o.optLong(k, 0L) } }
        harita
    }.getOrDefault(emptyMap())

    private fun sonTetikYaz(context: Context, harita: Map<Long, Long>) {
        runCatching {
            val o = JSONObject()
            harita.forEach { (k, v) -> o.put(k.toString(), v) }
            p(context).edit().putString(K_SON_TETIK, o.toString()).apply()
        }
    }

    /**
     * Kontrol et ve tetiklenenler için bildirim gönder.
     * Uygulama açılışında ve günlük alarmda çağrılıyor.
     */
    fun kontrolVeBildir(context: Context): Int {
        val tetiklenenler = runCatching { kontrolEt(context) }.getOrDefault(emptyList())
        tetiklenenler.forEachIndexed { i, t ->
            runCatching {
                BildirimMerkezi.gonder(
                    context, BildirimMerkezi.Tur.GOREV,
                    712_000 + i,
                    "${t.yer.emoji} ${t.yer.ad}",
                    t.hatirlatma.metin
                )
            }
        }
        return tetiklenenler.size
    }

    /**
     * NOT: Normal yedeklemede bu fonksiyona GEREK YOK.
     * `PrefYedek` v9.7'den beri `konum_hatirlatma_v1` deposunu otomatik
     * yedekliyor. Bu iki fonksiyon seçmeli dışa aktarma
     * (yalnız bu modülü paylaşma) için duruyor.
     */
    fun disaAktar(context: Context): JSONObject = runCatching {
        JSONObject()
            .put("yerler", JSONArray(p(context).getString(K_YERLER, "[]") ?: "[]"))
            .put("hatirlatmalar", JSONArray(p(context).getString(K_HATIRLATMALAR, "[]") ?: "[]"))
    }.getOrDefault(JSONObject())

    fun iceAktar(context: Context, kok: JSONObject?) {
        if (kok == null) return
        runCatching {
            val e = p(context).edit()
            kok.optJSONArray("yerler")?.let { e.putString(K_YERLER, it.toString()) }
            kok.optJSONArray("hatirlatmalar")?.let { e.putString(K_HATIRLATMALAR, it.toString()) }
            e.apply()
        }
    }
}
