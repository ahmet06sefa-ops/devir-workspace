package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v9.7 — Harcama defteri (öneri 43).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN AYRI SINIF
 * ══════════════════════════════════════════════════════════════════
 * [Takip] "bir şeyin süresi bitiyor" sorusunu çözüyor. Harcama
 * defteri farklı bir soruyu çözüyor: **"param nereye gidiyor"**.
 *
 * Veri modeli de farklı:
 *   · Takip kaydı **gelecek** bir olayı tutuyor (tekil, güncellenen)
 *   · Harcama **geçmiş** bir olayı tutuyor (çoğul, değişmez)
 *
 * İkisini tek modele sıkıştırmak her ikisini de bozardı. Ama
 * bağlantı var: bir fatura ödendiğinde [Takip.tamamla] hem ödeme
 * geçmişine hem — istenirse — buraya yazabiliyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * TASARIM KARARLARI
 * ══════════════════════════════════════════════════════════════════
 *
 * ── 1. Neden `Double` değil de kuruş `Long` kullanmadım ──
 * Finansal uygulamalarda kuruş cinsinden `Long` doğru seçim, çünkü
 * `Double` yuvarlama hatası biriktirir (0.1 + 0.2 != 0.3).
 *
 * Ama burada bilinçli olarak `Double` kullanıyorum:
 *   · Bu bir muhasebe programı değil, kişisel bir defter
 *   · Toplamlar en fazla birkaç bin kalem
 *   · `Double` 15 anlamlı basamağa kadar kesin — 999 trilyon TL'ye
 *     kadar kuruş hatası oluşmaz
 *   · Mevcut [Takip.tutar] alanı zaten `Double`, tutarlılık önemli
 *
 * Bunu yazıyorum çünkü ileride biri "neden kuruş kullanmadın"
 * diye sorabilir — cevabı bilinçli bir tercih, dikkatsizlik değil.
 *
 * ── 2. Neden kategoriler sabit liste ──
 * Serbest kategori girişi kullanıcıyı "market" / "Market" /
 * "markt" gibi üç ayrı kategori yaratmaya iter, sonra grafik
 * anlamsızlaşır. 12 sabit kategori + "Diğer" pratikte yetiyor.
 *
 * ── 3. Neden gelir de var ──
 * Yalnızca gider takibi "ne kadar harcadım" der ama "ne kadar
 * kaldı" diyemez. Gelir olmadan aylık bakiye hesaplanamaz ve
 * kullanıcı asıl bunu merak ediyor.
 */
object Butce {

    private const val TAG = "Butce"
    private const val PREF = "butce_v1"
    private const val K_KALEMLER = "kalemler_json"
    private const val K_AYLIK_LIMIT = "aylik_limit"

    /** Yaklaşık 4 yıl günde 1 kalem. */
    private const val TAVAN = 1500

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Kategoriler
    // ══════════════════════════════════════════════════════════

    /**
     * @param kod JSON'da saklanan sabit — değiştirme
     * @param renk grafik dilimi rengi
     * @param gelirMi true ise gider listesinde görünmez
     */
    enum class Kategori(
        val kod: String,
        val emoji: String,
        val adRes: Int,
        val renk: Int,
        val gelirMi: Boolean = false
    ) {
        MARKET("market", "🛒", R.string.bt_k_market, 0xFF4C9A5A.toInt()),
        YEMEK("yemek", "🍽️", R.string.bt_k_yemek, 0xFFE0733A.toInt()),
        ULASIM("ulasim", "🚌", R.string.bt_k_ulasim, 0xFF3A7BD5.toInt()),
        FATURA("fatura", "🧾", R.string.bt_k_fatura, 0xFFD9534F.toInt()),
        KIRA("kira", "🏠", R.string.bt_k_kira, 0xFF8E5BA6.toInt()),
        SAGLIK("saglik", "💊", R.string.bt_k_saglik, 0xFF2FA8A0.toInt()),
        EGITIM("egitim", "📚", R.string.bt_k_egitim, 0xFF5B6EE1.toInt()),
        GIYIM("giyim", "👕", R.string.bt_k_giyim, 0xFFC2568F.toInt()),
        EGLENCE("eglence", "🎬", R.string.bt_k_eglence, 0xFFE0A33A.toInt()),
        ABONELIK("abonelik", "🔁", R.string.bt_k_abonelik, 0xFF7A8B99.toInt()),
        BIRIKIM("birikim", "🐖", R.string.bt_k_birikim, 0xFF3E8E7E.toInt()),
        DIGER("diger", "•", R.string.bt_k_diger, 0xFF9AA0A6.toInt()),

        // ── Gelir kategorileri ──
        //
        // 🔴 v10.0 · ÖZ DENETİMDE YAKALADIĞIM ÇAKIŞMA:
        // Renk birleştirme betiğim `0xFF2E7D32` ve `0xFF66A75B`
        // değerlerini `GrafikDili.BASARI`'ya çevirdi. Ama bunlar
        // GRAFİK DİLİMİ renkleri — anlam değil KİMLİK taşıyorlar.
        //
        // Sonuç: MAAS, EK_GELIR ve MARKET üçü de aynı yeşil oldu.
        // Dağılım halkasında üç dilim ayırt edilemezdi.
        //
        // Ders: "aynı renk = aynı anlam" kuralı durum göstergeleri
        // için doğru, kategori paletleri için YANLIŞ. Palet
        // renklerinin tek işi birbirinden AYRILMAK.
        MAAS("maas", "💰", R.string.bt_k_maas, 0xFF2E7D32.toInt(), true),
        EK_GELIR("ekgelir", "✨", R.string.bt_k_ekgelir, 0xFF66A75B.toInt(), true);

        companion object {
            fun bul(kod: String?): Kategori = entries.firstOrNull { it.kod == kod } ?: DIGER
            val giderler: List<Kategori> get() = entries.filter { !it.gelirMi }
            val gelirler: List<Kategori> get() = entries.filter { it.gelirMi }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    /**
     * @param tutar her zaman **pozitif** tutulur; yön [gelir] alanında
     *   Negatif tutar saklamak toplama hatalarına açık: bir yerde
     *   `abs()` unutulunca gelir gideri götürür.
     */
    data class Kalem(
        val id: Long,
        val tutar: Double,
        val kategori: Kategori,
        val aciklama: String = "",
        val millis: Long = System.currentTimeMillis(),
        val gelir: Boolean = false
    ) {
        val gunAnahtari: String
            get() = runCatching {
                SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))
            }.getOrDefault("")

        val ayAnahtari: String
            get() = runCatching {
                SimpleDateFormat("yyyyMM", Locale.US).format(Date(millis))
            }.getOrDefault("")
    }

    // ══════════════════════════════════════════════════════════
    // Okuma / yazma
    // ══════════════════════════════════════════════════════════

    fun hepsi(context: Context): List<Kalem> = runCatching {
        val dizi = JSONArray(p(context).getString(K_KALEMLER, "[]") ?: "[]")
        val liste = mutableListOf<Kalem>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(
                Kalem(
                    id = o.optLong("id", System.currentTimeMillis()),
                    tutar = kotlin.math.abs(o.optDouble("t", 0.0)),
                    kategori = Kategori.bul(o.optString("k")),
                    aciklama = o.optString("a", ""),
                    millis = o.optLong("z", System.currentTimeMillis()),
                    gelir = o.optBoolean("g", false)
                )
            )
        }
        liste.sortedByDescending { it.millis }
    }.getOrElse {
        android.util.Log.w(TAG, "Kalemler okunamadı", it)
        emptyList()
    }

    fun ekle(context: Context, kalem: Kalem) {
        runCatching {
            val liste = hepsi(context).toMutableList()
            val idx = liste.indexOfFirst { it.id == kalem.id }
            if (idx >= 0) liste[idx] = kalem else liste.add(kalem)
            yaz(context, liste)
            // v9.8: yalnızca sayaç — tutar ve açıklama kaydedilmiyor
            runCatching { Kullanim.eylem(context, Kullanim.Eylem.HARCAMA_EKLE) }
        }.onFailure { android.util.Log.w(TAG, "Kalem eklenemedi", it) }
    }

    fun sil(context: Context, id: Long) {
        yaz(context, hepsi(context).filter { it.id != id })
    }

    private fun yaz(context: Context, liste: List<Kalem>) {
        runCatching {
            val kirpik = liste.sortedByDescending { it.millis }.take(TAVAN)
            val dizi = JSONArray()
            kirpik.forEach { k ->
                dizi.put(
                    JSONObject()
                        .put("id", k.id).put("t", k.tutar)
                        .put("k", k.kategori.kod).put("a", k.aciklama)
                        .put("z", k.millis).put("g", k.gelir)
                )
            }
            p(context).edit().putString(K_KALEMLER, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "Kalemler yazılamadı", it) }
    }

    // ══════════════════════════════════════════════════════════
    // Aylık özet
    // ══════════════════════════════════════════════════════════

    data class AyOzet(
        val ayAnahtari: String,
        val etiket: String,
        val gelir: Double,
        val gider: Double,
        val kalemSayisi: Int
    ) {
        val bakiye: Double get() = gelir - gider

        /** Gelirin yüzde kaçı harcandı? Gelir yoksa null. */
        val harcamaOrani: Int?
            get() = if (gelir <= 0) null
            else ((gider / gelir) * 100).toInt().coerceIn(0, 999)
    }

    /** Ay anahtarı: "202608" biçiminde. */
    fun ayAnahtari(millis: Long = System.currentTimeMillis()): String = runCatching {
        SimpleDateFormat("yyyyMM", Locale.US).format(Date(millis))
    }.getOrDefault("")

    fun ayOzeti(context: Context, ayAnahtari: String = ayAnahtari()): AyOzet {
        val kalemler = hepsi(context).filter { it.ayAnahtari == ayAnahtari }
        return AyOzet(
            ayAnahtari = ayAnahtari,
            etiket = ayEtiketi(ayAnahtari),
            gelir = kalemler.filter { it.gelir }.sumOf { it.tutar },
            gider = kalemler.filter { !it.gelir }.sumOf { it.tutar },
            kalemSayisi = kalemler.size
        )
    }

    /** Son [adet] ayın özeti — grafik için, eskiden yeniye sıralı. */
    fun sonAylar(context: Context, adet: Int = 6): List<AyOzet> {
        val tum = hepsi(context)
        if (tum.isEmpty()) return emptyList()
        val c = Calendar.getInstance()
        val sonuc = mutableListOf<AyOzet>()
        for (i in adet - 1 downTo 0) {
            val g = Calendar.getInstance().apply {
                timeInMillis = c.timeInMillis
                add(Calendar.MONTH, -i)
            }
            val anahtar = ayAnahtari(g.timeInMillis)
            val kalemler = tum.filter { it.ayAnahtari == anahtar }
            sonuc.add(
                AyOzet(
                    anahtar, ayEtiketi(anahtar),
                    kalemler.filter { it.gelir }.sumOf { it.tutar },
                    kalemler.filter { !it.gelir }.sumOf { it.tutar },
                    kalemler.size
                )
            )
        }
        return sonuc
    }

    private fun ayEtiketi(ayAnahtari: String): String = runCatching {
        val y = ayAnahtari.substring(0, 4).toInt()
        val a = ayAnahtari.substring(4, 6).toInt()
        val c = Calendar.getInstance().apply { set(y, a - 1, 1) }
        SimpleDateFormat("MMM", Locale("tr", "TR")).format(c.time)
    }.getOrDefault(ayAnahtari)

    // ══════════════════════════════════════════════════════════
    // Kategori dağılımı
    // ══════════════════════════════════════════════════════════

    data class KategoriPay(
        val kategori: Kategori,
        val toplam: Double,
        val adet: Int,
        val yuzde: Int
    )

    /** Bir ayın gider dağılımı — büyükten küçüğe. */
    fun kategoriDagilimi(
        context: Context,
        ayAnahtari: String = ayAnahtari()
    ): List<KategoriPay> {
        val giderler = hepsi(context).filter { it.ayAnahtari == ayAnahtari && !it.gelir }
        if (giderler.isEmpty()) return emptyList()
        val toplam = giderler.sumOf { it.tutar }
        if (toplam <= 0) return emptyList()

        return giderler.groupBy { it.kategori }
            .map { (kat, liste) ->
                val t = liste.sumOf { it.tutar }
                KategoriPay(kat, t, liste.size, ((t / toplam) * 100).toInt())
            }
            .sortedByDescending { it.toplam }
    }

    // ══════════════════════════════════════════════════════════
    // Aylık limit
    // ══════════════════════════════════════════════════════════

    fun aylikLimit(context: Context): Double =
        p(context).getFloat(K_AYLIK_LIMIT, 0f).toDouble()

    fun aylikLimitAyarla(context: Context, limit: Double) {
        p(context).edit().putFloat(K_AYLIK_LIMIT, limit.toFloat().coerceAtLeast(0f)).apply()
    }

    /**
     * Limit durumu.
     *
     * @return null = limit ayarlanmamış · 0-100+ = kullanılan yüzde
     */
    fun limitDurumu(context: Context): Int? {
        val limit = aylikLimit(context)
        if (limit <= 0) return null
        val gider = ayOzeti(context).gider
        return ((gider / limit) * 100).toInt()
    }

    /**
     * Günlük harcanabilir tutar.
     *
     * Ay sonuna kalan günlere bölüyor. Limit aşıldıysa negatif
     * dönmüyor, 0 dönüyor — "eksi 40 lira harcayabilirsin" anlamsız.
     */
    fun gunlukKalan(context: Context): Double? {
        val limit = aylikLimit(context)
        if (limit <= 0) return null
        val kalanTutar = (limit - ayOzeti(context).gider).coerceAtLeast(0.0)
        val c = Calendar.getInstance()
        val ayGun = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val kalanGun = (ayGun - c.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        return kalanTutar / kalanGun
    }

    // ══════════════════════════════════════════════════════════
    // Çıkarımlar
    // ══════════════════════════════════════════════════════════

    /**
     * Veriden okunabilir gözlemler üretir.
     *
     * Ham sayı göstermek yetmiyor; kullanıcı "%34 market" görünce
     * bunun iyi mi kötü mü olduğunu bilmiyor. Karşılaştırma
     * (geçen aya göre) ve eşik (limitin %80'i) anlam katıyor.
     */
    fun cikarimlar(context: Context): List<String> {
        val sonuc = mutableListOf<String>()
        val buAy = ayOzeti(context)
        if (buAy.kalemSayisi == 0) return sonuc

        // 1. Limit durumu
        limitDurumu(context)?.let { yuzde ->
            when {
                yuzde >= 100 -> sonuc.add(context.getString(R.string.bt_c_limit_asildi, yuzde))
                yuzde >= 80 -> sonuc.add(context.getString(R.string.bt_c_limit_yakin, yuzde))
                else -> gunlukKalan(context)?.let {
                    sonuc.add(context.getString(R.string.bt_c_gunluk, Takip.paraMetni(it)))
                }
            }
        }

        // 2. Geçen aya göre değişim
        val aylar = sonAylar(context, 2)
        if (aylar.size == 2 && aylar[0].gider > 0 && aylar[1].kalemSayisi > 0) {
            val degisim = (((aylar[1].gider - aylar[0].gider) / aylar[0].gider) * 100).toInt()
            if (kotlin.math.abs(degisim) >= 10) {
                sonuc.add(
                    if (degisim > 0) context.getString(R.string.bt_c_artis, degisim)
                    else context.getString(R.string.bt_c_azalis, -degisim)
                )
            }
        }

        // 3. En büyük kategori
        kategoriDagilimi(context).firstOrNull()?.let { pay ->
            if (pay.yuzde >= 25) {
                sonuc.add(
                    context.getString(
                        R.string.bt_c_buyuk_kat,
                        context.getString(pay.kategori.adRes), pay.yuzde
                    )
                )
            }
        }

        // 4. Bakiye
        if (buAy.gelir > 0) {
            sonuc.add(
                if (buAy.bakiye >= 0)
                    context.getString(R.string.bt_c_arti, Takip.paraMetni(buAy.bakiye))
                else
                    context.getString(R.string.bt_c_eksi, Takip.paraMetni(-buAy.bakiye))
            )
        }

        // 5. Abonelik yükü (Takip'ten geliyor — iki modülün buluştuğu yer)
        val abonelik = runCatching { Takip.aylikYuk(context) }.getOrDefault(0.0)
        if (abonelik > 0) {
            sonuc.add(context.getString(R.string.bt_c_abonelik, Takip.paraMetni(abonelik)))
        }

        return sonuc
    }

    /** Bugünün toplam harcaması — hızlı ekleme sonrası gösterilir. */
    fun bugunToplam(context: Context): Double {
        val bugun = runCatching {
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }.getOrDefault("")
        return hepsi(context).filter { it.gunAnahtari == bugun && !it.gelir }.sumOf { it.tutar }
    }

    // ══════════════════════════════════════════════════════════
    // Yedekleme
    // ══════════════════════════════════════════════════════════

    /**
     * NOT: Normal yedeklemede bu fonksiyona GEREK YOK.
     * `PrefYedek` v9.7'den beri `butce_v1` deposunu otomatik
     * yedekliyor. Bu iki fonksiyon seçmeli dışa aktarma
     * (yalnız bu modülü paylaşma) için duruyor.
     */
    fun disaAktar(context: Context): JSONObject = runCatching {
        JSONObject()
            .put("kalemler", JSONArray(p(context).getString(K_KALEMLER, "[]") ?: "[]"))
            .put("limit", aylikLimit(context))
    }.getOrDefault(JSONObject())

    fun iceAktar(context: Context, kok: JSONObject?) {
        if (kok == null) return
        runCatching {
            val e = p(context).edit()
            kok.optJSONArray("kalemler")?.let { e.putString(K_KALEMLER, it.toString()) }
            if (kok.has("limit")) e.putFloat(K_AYLIK_LIMIT, kok.optDouble("limit", 0.0).toFloat())
            e.apply()
        }.onFailure { android.util.Log.w(TAG, "İçe aktarma başarısız", it) }
    }
}
