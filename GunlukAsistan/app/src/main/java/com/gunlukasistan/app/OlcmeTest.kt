package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v9.6 — Ön test / son test (öneri 31) ve sınav simülasyonu (öneri 36).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNERİ 31 — NEDEN ÖN TEST / SON TEST
 * ══════════════════════════════════════════════════════════════════
 * Uygulama şu an "kaç madde bitirdin" ve "kaç dakika çalıştın"
 * ölçüyor. İkisi de **çaba** ölçüsü, **öğrenme** ölçüsü değil.
 *
 * 3 saat çalışıp hiçbir şey öğrenmemek mümkün. Tersi de mümkün:
 * 20 dakikada bir konuyu oturtmak.
 *
 * Öğrenme kazanımını ölçmenin tek yolu: **önce ölç, çalış, sonra
 * tekrar ölç**. Aradaki fark gerçek kazanç.
 *
 *     Ön test:  %40
 *     Son test: %85
 *     Kazanım:  +45 puan
 *
 * Bu sayı motivasyon açısından da güçlü: "3 saat çalıştım"dan çok
 * "%40'tan %85'e çıktım" tatmin edici.
 *
 * ── Normalize kazanım ──
 * Ham fark yanıltıcı: %90'dan %95'e çıkmak, %20'den %60'a çıkmaktan
 * daha zor. Eğitim biliminde kullanılan **normalize kazanım**
 * formülü bunu düzeltiyor:
 *
 *     g = (son - ön) / (100 - ön)
 *
 * %90 → %95 için g = 0.50 · %20 → %60 için g = 0.50 — eşit değerde.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNERİ 36 — SINAV SİMÜLASYONU
 * ══════════════════════════════════════════════════════════════════
 * Normal quiz: soru soruyor, cevap veriyorsun, hemen doğru/yanlış
 * gösteriyor, istediğin zaman çıkabiliyorsun.
 *
 * Gerçek sınav öyle değil:
 *   · Süre baskısı var
 *   · Cevabı hemen göremiyorsun
 *   · Geri dönüp değiştirebiliyorsun
 *   · Sonunda toplu analiz
 *
 * Bu sınıf sınav ayarlarını ve sonuç analizini yönetiyor.
 */
object OlcmeTest {

    private const val TAG = "OlcmeTest"
    private const val PREF = "olcme_test_v1"
    private const val K_KAYIT = "kayitlar_json"
    private const val K_SIMULASYON = "simulasyon_json"

    private const val TAVAN = 200

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Öneri 31 — Ön/son test
    // ══════════════════════════════════════════════════════════

    const val TUR_ON = 0
    const val TUR_SON = 1

    /**
     * Bir ölçüm kaydı.
     *
     * @param konuId hangi konu ölçüldü
     * @param tur ön test mi son test mi
     * @param yuzde başarı yüzdesi
     */
    data class Olcum(
        val konuId: Long,
        val konuAdi: String,
        val tur: Int,
        val yuzde: Int,
        val soruSayisi: Int,
        val zaman: Long
    )

    fun olcumler(c: Context): MutableList<Olcum> {
        val ham = p(c).getString(K_KAYIT, "[]") ?: "[]"
        val liste = mutableListOf<Olcum>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Olcum(
                        konuId = o.optLong("k"),
                        konuAdi = o.optString("ad"),
                        tur = o.optInt("t"),
                        yuzde = o.optInt("y"),
                        soruSayisi = o.optInt("s"),
                        zaman = o.optLong("z")
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "olcumler", it) }
        return liste
    }

    fun olcumKaydet(c: Context, olcum: Olcum) {
        runCatching {
            val liste = olcumler(c)
            liste.add(olcum)
            val kirpik = if (liste.size > TAVAN) liste.takeLast(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject()
                        .put("k", it.konuId).put("ad", it.konuAdi)
                        .put("t", it.tur).put("y", it.yuzde)
                        .put("s", it.soruSayisi).put("z", it.zaman)
                )
            }
            p(c).edit().putString(K_KAYIT, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "olcumKaydet", it) }
    }

    /**
     * Bir konunun öğrenme kazanımı.
     *
     * @param onYuzde ilk ölçüm
     * @param sonYuzde son ölçüm
     * @param hamFark basit çıkarma
     * @param normalizeKazanim 0..1 arası — 1.0 = mükemmel öğrenme
     */
    data class Kazanim(
        val konuId: Long,
        val konuAdi: String,
        val onYuzde: Int,
        val sonYuzde: Int,
        val onZaman: Long,
        val sonZaman: Long
    ) {
        val hamFark: Int get() = sonYuzde - onYuzde

        /**
         * Normalize kazanım (Hake gain).
         *
         * `g = (son - ön) / (100 - ön)`
         *
         * Ön test zaten %100'se bölme sıfıra düşer; o durumda
         * öğrenilecek bir şey kalmamış demektir, 1.0 dönüyoruz.
         */
        val normalizeKazanim: Double
            get() {
                if (onYuzde >= 100) return 1.0
                return ((sonYuzde - onYuzde).toDouble() / (100 - onYuzde))
                    .coerceIn(-1.0, 1.0)
            }

        /** Eğitim biliminde kabul gören eşikler. */
        val seviye: Int
            get() = when {
                normalizeKazanim >= 0.7 -> 2   // yüksek
                normalizeKazanim >= 0.3 -> 1   // orta
                else -> 0                      // düşük
            }
    }

    /** Konu bazında ön/son eşleştirmesi. */
    fun kazanimlar(c: Context): List<Kazanim> = runCatching {
        val hepsi = olcumler(c)
        hepsi.groupBy { it.konuId }.mapNotNull { (konuId, liste) ->
            // İlk ÖN test ve son SON test eşleştiriliyor.
            // Kullanıcı birden çok kez ölçebilir; en anlamlı çift bu.
            val on = liste.filter { it.tur == TUR_ON }.minByOrNull { it.zaman }
                ?: return@mapNotNull null
            val son = liste.filter { it.tur == TUR_SON && it.zaman > on.zaman }
                .maxByOrNull { it.zaman } ?: return@mapNotNull null
            Kazanim(
                konuId = konuId,
                konuAdi = on.konuAdi.ifBlank { son.konuAdi },
                onYuzde = on.yuzde,
                sonYuzde = son.yuzde,
                onZaman = on.zaman,
                sonZaman = son.zaman
            )
        }.sortedByDescending { it.sonZaman }
    }.onFailure { android.util.Log.w(TAG, "kazanimlar", it) }.getOrDefault(emptyList())

    /** Bu konuda ön test yapıldı mı? (arayüz hangi düğmeyi göstersin) */
    fun onTestVarMi(c: Context, konuId: Long): Boolean =
        olcumler(c).any { it.konuId == konuId && it.tur == TUR_ON }

    /** Bu konu için bekleyen son test var mı? */
    fun sonTestBekliyorMu(c: Context, konuId: Long): Boolean {
        val liste = olcumler(c).filter { it.konuId == konuId }
        val on = liste.filter { it.tur == TUR_ON }.minByOrNull { it.zaman } ?: return false
        return liste.none { it.tur == TUR_SON && it.zaman > on.zaman }
    }

    /** Ortalama normalize kazanım — genel öğrenme verimliliği. */
    fun ortalamaKazanim(c: Context): Double? {
        val liste = kazanimlar(c)
        if (liste.isEmpty()) return null
        return liste.map { it.normalizeKazanim }.average()
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 36 — Sınav simülasyonu
    // ══════════════════════════════════════════════════════════

    /**
     * Sınav ayarları.
     *
     * @param soruSayisi kaç soru
     * @param sureDk toplam süre (0 = sınırsız)
     * @param cevaplariGizle sonuna kadar doğru/yanlış gösterilmesin
     * @param karisik konular karıştırılsın mı (interleaving)
     */
    data class Ayar(
        val soruSayisi: Int = 20,
        val sureDk: Int = 30,
        val cevaplariGizle: Boolean = true,
        val karisik: Boolean = true
    )

    /** Bir sınav sonucu. */
    data class Simulasyon(
        val id: Long,
        val baslik: String,
        val dogru: Int,
        val yanlis: Int,
        val bos: Int,
        val sureSn: Int,
        val zaman: Long
    ) {
        val toplam: Int get() = dogru + yanlis + bos
        val yuzde: Int get() = if (toplam == 0) 0 else dogru * 100 / toplam

        /** Net = doğru − yanlış/4 (Türkiye sınav sistemi). */
        val net: Double get() = dogru - (yanlis / 4.0)

        /** Soru başına ortalama saniye. */
        val soruBasiSn: Int get() = if (toplam == 0) 0 else sureSn / toplam
    }

    fun simulasyonlar(c: Context): MutableList<Simulasyon> {
        val ham = p(c).getString(K_SIMULASYON, "[]") ?: "[]"
        val liste = mutableListOf<Simulasyon>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Simulasyon(
                        id = o.optLong("id"),
                        baslik = o.optString("b"),
                        dogru = o.optInt("d"),
                        yanlis = o.optInt("y"),
                        bos = o.optInt("bo"),
                        sureSn = o.optInt("s"),
                        zaman = o.optLong("z")
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "simulasyonlar", it) }
        return liste
    }

    fun simulasyonKaydet(c: Context, s: Simulasyon) {
        runCatching {
            val liste = simulasyonlar(c)
            liste.add(0, s)
            val kirpik = if (liste.size > 50) liste.take(50) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject()
                        .put("id", it.id).put("b", it.baslik)
                        .put("d", it.dogru).put("y", it.yanlis).put("bo", it.bos)
                        .put("s", it.sureSn).put("z", it.zaman)
                )
            }
            p(c).edit().putString(K_SIMULASYON, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "simulasyonKaydet", it) }
    }

    /**
     * Sınav için soru havuzu hazırlar.
     *
     * ── Kaynak sırası ──
     * 1. Konuların quiz havuzları (`QuizStore.havuzdanSinav`)
     * 2. Hata defteri (zayıf noktalar sınavda da çıkmalı)
     *
     * ── Neden hata defterinden de alınıyor ──
     * Gerçek sınavda daha önce yanlış yaptığın konu tipi yine
     * çıkar. Simülasyonun bunu yansıtması lazım.
     */
    fun sorulariHazirla(c: Context, ayar: Ayar): List<QuizStore.Soru> = runCatching {
        val havuz = mutableListOf<QuizStore.Soru>()

        // Konu maddelerinin sanal kimliklerinden topla
        Store.loadTopics(c).forEach { konu ->
            konu.items.forEach { madde ->
                runCatching {
                    havuz.addAll(QuizStore.havuzdanSinav(c, -madde.id, adet = 5))
                }
            }
        }

        // Hata defterinden ekle — zayıf noktalar sınavda da olsun
        runCatching {
            Hatalarim.hepsi(c).take(ayar.soruSayisi / 3).forEach { h ->
                havuz.add(h.soruya())
            }
        }

        val benzersiz = havuz.distinctBy { it.metin.trim().lowercase() }
            .filter { it.gecerli }

        if (ayar.karisik) benzersiz.shuffled().take(ayar.soruSayisi)
        else benzersiz.take(ayar.soruSayisi)
    }.onFailure { android.util.Log.w(TAG, "sorulariHazirla", it) }.getOrDefault(emptyList())

    /** Sınav için yeterli soru var mı? */
    fun havuzYeterliMi(c: Context, enAz: Int = 10): Boolean =
        sorulariHazirla(c, Ayar(soruSayisi = enAz)).size >= enAz

    /**
     * Sınav sonucu değerlendirmesi — dürüst ve yönlendirici.
     */
    fun degerlendirme(c: Context, s: Simulasyon): String {
        val onceki = simulasyonlar(c).filter { it.id != s.id }
        return buildString {
            append(
                when {
                    s.yuzde >= 85 -> c.getString(R.string.ot_sim_harika)
                    s.yuzde >= 60 -> c.getString(R.string.ot_sim_iyi)
                    s.yuzde >= 40 -> c.getString(R.string.ot_sim_orta)
                    else -> c.getString(R.string.ot_sim_zayif)
                }
            )
            // Önceki sınavla karşılaştır
            val sonOnceki = onceki.firstOrNull()
            if (sonOnceki != null) {
                val fark = s.yuzde - sonOnceki.yuzde
                append("\n\n")
                append(
                    when {
                        fark > 5 -> c.getString(R.string.ot_sim_yukseldi, fark)
                        fark < -5 -> c.getString(R.string.ot_sim_dustu, -fark)
                        else -> c.getString(R.string.ot_sim_sabit)
                    }
                )
            }
            // Hız uyarısı
            if (s.soruBasiSn > 90 && s.toplam >= 5) {
                append("\n\n")
                append(c.getString(R.string.ot_sim_yavas, s.soruBasiSn))
            }
            // Boş bırakma uyarısı
            if (s.bos > s.toplam / 4) {
                append("\n\n")
                append(c.getString(R.string.ot_sim_bos, s.bos))
            }
        }
    }

    // ══════════════════════════════════════════════════════════

    fun temizle(c: Context) {
        p(c).edit().clear().apply()
    }

    fun ozet(c: Context): JSONObject = JSONObject().apply {
        runCatching {
            put("olcum", olcumler(c).size)
            put("kazanim", kazanimlar(c).size)
            put("simulasyon", simulasyonlar(c).size)
            put("ort_kazanim", ortalamaKazanim(c) ?: 0.0)
        }
    }
}
