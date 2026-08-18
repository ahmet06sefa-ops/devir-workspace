package com.gunlukasistan.app

import android.content.Context

/**
 * v9.6 — Bekleyen ölçüm köprüsü (öneri 31, 36).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN BU SINIF GEREKLİ
 * ══════════════════════════════════════════════════════════════════
 * Ön/son test ve sınav simülasyonu `QuizActivity`'nin **geçici mod**
 * mekanizmasını (v7.84) yeniden kullanıyor:
 *
 *     Hatalarim.geciciAyarla(ctx, sorular)
 *     QuizActivity.acGecici(ctx, baslik)
 *
 * Geçici modda sonuç hiçbir yere yazılmıyor — tek seferlik
 * pekiştirme için tasarlanmıştı. Ama biz sonucu **ölçüm olarak
 * kaydetmek** istiyoruz.
 *
 * ── Neden QuizActivity'ye parametre eklenmedi ──
 * `QuizActivity` 500+ satır ve altı farklı modu var (normal, tekrar,
 * hata, geçici, karışık, çoklu ders). Yedinci bir mod eklemek o
 * dosyayı daha kırılgan yapardı. Bunun yerine: quiz başlamadan önce
 * "bu quiz bittiğinde şunu kaydet" notu bırakılıyor, quiz bitince
 * not okunup temizleniyor.
 *
 * ── Neden tek seferlik ──
 * Not okunduğu anda siliniyor. Kullanıcı quizi yarıda bırakırsa not
 * kalır ama bir sonraki geçici quizde tüketilir — yanlış konuya
 * yazılabilir. Bu riski azaltmak için notun **son kullanma tarihi**
 * var (2 saat).
 */
object OlcmeBekleyen {

    private const val TAG = "OlcmeBekleyen"
    private const val PREF = "olcme_bekleyen_v1"

    private const val K_TIP = "tip"
    private const val K_KONU_ID = "konu_id"
    private const val K_KONU_AD = "konu_ad"
    private const val K_TUR = "tur"
    private const val K_SORU = "soru_sayisi"
    private const val K_SURE = "sure_dk"
    private const val K_ZAMAN = "zaman"

    private const val TIP_YOK = 0
    private const val TIP_ONSON = 1
    private const val TIP_SIMULASYON = 2

    /** Not bu süreden eskiyse yok sayılıyor. */
    private const val GECERLILIK_MS = 2 * 60 * 60 * 1000L

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Kurulum
    // ══════════════════════════════════════════════════════════

    /** Ön/son test notu bırakır. */
    fun ayarla(c: Context, konuId: Long, konuAd: String, tur: Int, soruSayisi: Int) {
        p(c).edit()
            .putInt(K_TIP, TIP_ONSON)
            .putLong(K_KONU_ID, konuId)
            .putString(K_KONU_AD, konuAd)
            .putInt(K_TUR, tur)
            .putInt(K_SORU, soruSayisi)
            .putLong(K_ZAMAN, System.currentTimeMillis())
            .apply()
    }

    /** Sınav simülasyonu notu bırakır. */
    fun simulasyonAyarla(c: Context, sureDk: Int, soruSayisi: Int) {
        p(c).edit()
            .putInt(K_TIP, TIP_SIMULASYON)
            .putInt(K_SURE, sureDk)
            .putInt(K_SORU, soruSayisi)
            .putLong(K_ZAMAN, System.currentTimeMillis())
            .apply()
    }

    fun temizle(c: Context) {
        p(c).edit().clear().apply()
    }

    /** Bekleyen bir ölçüm var mı? */
    fun varMi(c: Context): Boolean {
        val d = p(c)
        if (d.getInt(K_TIP, TIP_YOK) == TIP_YOK) return false
        val zaman = d.getLong(K_ZAMAN, 0L)
        if (System.currentTimeMillis() - zaman > GECERLILIK_MS) {
            // Süresi geçmiş not — temizle
            temizle(c)
            return false
        }
        return true
    }

    /** Sınav simülasyonu mu bekliyor? (süre göstergesi için) */
    fun simulasyonMu(c: Context): Boolean =
        varMi(c) && p(c).getInt(K_TIP, TIP_YOK) == TIP_SIMULASYON

    fun simulasyonSuresiDk(c: Context): Int = p(c).getInt(K_SURE, 0)

    // ══════════════════════════════════════════════════════════
    // Tüketim
    // ══════════════════════════════════════════════════════════

    /**
     * Quiz bittiğinde çağrılır. Notu okur, sonucu kaydeder, notu siler.
     *
     * `QuizActivity.sonucGoster` içinden çağrılıyor.
     *
     * @param dogru doğru sayısı
     * @param toplam soru sayısı
     * @param sureSn quizde geçen süre
     * @return kullanıcıya gösterilecek ek mesaj, yoksa null
     */
    fun tamamla(c: Context, dogru: Int, toplam: Int, sureSn: Int): String? {
        if (!varMi(c)) return null
        val d = p(c)
        val tip = d.getInt(K_TIP, TIP_YOK)

        return runCatching {
            when (tip) {
                TIP_ONSON -> {
                    val konuId = d.getLong(K_KONU_ID, 0L)
                    val konuAd = d.getString(K_KONU_AD, "") ?: ""
                    val tur = d.getInt(K_TUR, OlcmeTest.TUR_ON)
                    val yuzde = if (toplam == 0) 0 else dogru * 100 / toplam

                    OlcmeTest.olcumKaydet(
                        c,
                        OlcmeTest.Olcum(
                            konuId = konuId,
                            konuAdi = konuAd,
                            tur = tur,
                            yuzde = yuzde,
                            soruSayisi = toplam,
                            zaman = System.currentTimeMillis()
                        )
                    )
                    temizle(c)

                    if (tur == OlcmeTest.TUR_ON) {
                        c.getString(R.string.ob_on_kaydedildi, yuzde)
                    } else {
                        // Son test: kazanımı hesapla ve göster
                        val kazanim = OlcmeTest.kazanimlar(c)
                            .firstOrNull { it.konuId == konuId }
                        if (kazanim != null) {
                            c.getString(
                                R.string.ob_kazanim,
                                kazanim.onYuzde, kazanim.sonYuzde,
                                (kazanim.normalizeKazanim * 100).toInt()
                            )
                        } else {
                            c.getString(R.string.ob_son_kaydedildi, yuzde)
                        }
                    }
                }

                TIP_SIMULASYON -> {
                    val sim = OlcmeTest.Simulasyon(
                        id = System.currentTimeMillis(),
                        baslik = c.getString(R.string.ot_sim_baslik),
                        dogru = dogru,
                        yanlis = (toplam - dogru).coerceAtLeast(0),
                        bos = 0,
                        sureSn = sureSn,
                        zaman = System.currentTimeMillis()
                    )
                    OlcmeTest.simulasyonKaydet(c, sim)
                    temizle(c)
                    OlcmeTest.degerlendirme(c, sim)
                }

                else -> null
            }
        }.onFailure {
            android.util.Log.w(TAG, "tamamla", it)
            temizle(c)
        }.getOrNull()
    }
}
