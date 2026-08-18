package com.gunlukasistan.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * v9.5 — Fotoğraftan soru çözme (öneri 25).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN BU MADDE "NEREDEYSE HAZIR"DI
 * ══════════════════════════════════════════════════════════════════
 * 50'lik listede bu maddeyi yazarken şunu belirtmiştim:
 * "`AiClient.gorselDenetim` zaten var, yalnız kanıt denetimi için
 * kullanılıyor. Bağlanması yeterli."
 *
 * Kod taraması bunu doğruladı:
 *   · `AiClient.gorselDenetim(ctx, base64, istem)` — çalışıyor
 *   · `GorselHazirla.base64Uret(ctx, uri)` — netleştirme dahil
 *   · `GorselHazirla.onizleme(ctx, uri)` — ölçekli önizleme
 *   · `FotoKonuAkisi` — kamera/galeri seçim akışı örneği
 *
 * Eksik olan tek şey: soru çözmeye uygun **istem** ve **sonuç
 * ayrıştırma**. Bu dosya onu ekliyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * TASARIM: NEDEN "CEVAP" DEĞİL "ÇÖZÜM"
 * ══════════════════════════════════════════════════════════════════
 * Bir öğrenme uygulamasında soruya doğrudan cevap vermek zararlı.
 * Kullanıcı cevabı kopyalar, öğrenmez. İstem şunu zorunlu kılıyor:
 *
 *   1. Önce sorunun NE İSTEDİĞİ açıklanır
 *   2. Hangi bilgi/formül gerekli, o söylenir
 *   3. Adım adım çözüm — her adımda NEDEN o işlem yapıldı
 *   4. Sonuç
 *   5. Benzer sorularda dikkat edilecek nokta
 *
 * Ayrıca **"ipucu modu"** var: yalnız 1. ve 2. adımı gösterir,
 * çözümü saklar. Kullanıcı önce kendi dener.
 *
 * ── Kaydetme ──
 * Çözülen sorular geçmişe yazılıyor; tekrar sorulduğunda AI'ya
 * gitmeden gösteriliyor (kota tasarrufu). Ayrıca hata defterine
 * eklenebiliyor.
 */
object SoruCoz {

    private const val TAG = "SoruCoz"
    private const val PREF = "soru_coz_v1"
    private const val K_GECMIS = "gecmis_json"

    /** Geçmişte en fazla kaç çözüm tutulur. */
    private const val TAVAN = 60

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════

    /**
     * Bir çözüm kaydı.
     *
     * @param soru AI'nın fotoğraftan okuduğu soru metni
     * @param cozum adım adım çözüm
     * @param sonuc kısa cevap (varsa)
     * @param konu AI'nın tespit ettiği konu
     */
    data class Cozum(
        val id: Long,
        val soru: String,
        val cozum: String,
        val sonuc: String,
        val konu: String,
        val ipucu: String,
        val zaman: Long
    )

    /** AI çağrısının sonucu. */
    data class Sonuc(
        val ok: Boolean,
        val cozum: Cozum? = null,
        val hata: String = ""
    )

    // ══════════════════════════════════════════════════════════
    // İstem
    // ══════════════════════════════════════════════════════════

    /**
     * Soru çözme istemi.
     *
     * ── Neden alan sabiti yok ──
     * v7.80'de AI istemlerinden alan sabitleri kaldırılmıştı
     * ("mühendislik" varsayımı). Aynı kural burada da geçerli:
     * kullanıcı matematik de sorabilir, tarih de, kimya da.
     * İstem konuyu FOTOĞRAFTAN anlamasını söylüyor.
     *
     * ── Neden JSON isteniyor ──
     * Serbest metin ayrıştırmak kırılgan. JSON ile alanlar net
     * ayrılıyor ve "ipucu" ayrı tutulabiliyor.
     */
    private fun istem(ipucuModu: Boolean): String = buildString {
        appendLine("Bu fotoğraftaki soruyu incele.")
        appendLine()
        appendLine("MUTLAK KURALLAR:")
        appendLine("1. Sorunun hangi alana ait olduğunu FOTOĞRAFTAN anla.")
        appendLine("   Kendi uzmanlık alanını varsayma.")
        appendLine("2. Fotoğrafta soru yoksa veya okunamıyorsa bunu açıkça söyle.")
        appendLine("3. Cevabı doğrudan verme — ÖĞRET.")
        appendLine("   Her adımda o işlemi NEDEN yaptığını yaz.")
        appendLine("4. Türkçe yanıtla.")
        appendLine()
        if (ipucuModu) {
            appendLine("İPUCU MODU: Bu sefer çözümü YAPMA.")
            appendLine("Yalnız sorunun ne istediğini ve hangi bilgi/formülün")
            appendLine("gerektiğini söyle. Öğrenci önce kendi denesin.")
            appendLine()
        }
        appendLine("Yanıtı SADECE şu JSON biçiminde ver, başka hiçbir şey yazma:")
        appendLine("{")
        appendLine("  \"okundu\": true,")
        appendLine("  \"konu\": \"sorunun konusu, 2-4 kelime\",")
        appendLine("  \"soru\": \"fotoğraftaki soruyu metin olarak yaz\",")
        appendLine("  \"ipucu\": \"soru ne istiyor, hangi bilgi gerekli — 2-3 cümle\",")
        if (ipucuModu) {
            appendLine("  \"cozum\": \"\",")
            appendLine("  \"sonuc\": \"\"")
        } else {
            appendLine("  \"cozum\": \"adım adım çözüm, her adımda neden\",")
            appendLine("  \"sonuc\": \"kısa cevap\"")
        }
        appendLine("}")
        appendLine()
        appendLine("Fotoğrafta soru yoksa: {\"okundu\": false, \"ipucu\": \"sebep\"}")
    }

    // ══════════════════════════════════════════════════════════
    // Çağrı
    // ══════════════════════════════════════════════════════════

    /**
     * Fotoğraftaki soruyu çözer.
     *
     * **Arka planda çağrılmalı** — ağ isteği içeriyor.
     *
     * @param ipucuModu true ise yalnız yönlendirme verir, çözmez
     */
    fun coz(context: Context, uri: Uri, ipucuModu: Boolean = false): Sonuc {
        if (!AiSettings.isReady(context)) {
            return Sonuc(false, hata = context.getString(R.string.sc_ai_kapali))
        }

        val base64 = runCatching {
            GorselHazirla.base64Uret(context, uri, netlestir = true)
        }.getOrNull()
        if (base64.isNullOrBlank()) {
            return Sonuc(false, hata = context.getString(R.string.sc_foto_okunamadi))
        }

        val yanit = runCatching {
            AiClient.gorselDenetim(context, base64, istem(ipucuModu))
        }.getOrElse {
            android.util.Log.w(TAG, "AI çağrısı", it)
            return Sonuc(false, hata = context.getString(R.string.sc_ai_hata))
        }

        if (!yanit.ok) {
            return Sonuc(false, hata = yanit.text.ifBlank {
                context.getString(R.string.sc_ai_hata)
            })
        }

        return ayristir(context, yanit.text)
    }

    /**
     * AI yanıtını ayrıştırır.
     *
     * Model bazen JSON'u ```json bloğu içine alıyor veya başına
     * açıklama ekliyor. İlk `{` ile son `}` arasını almak bu
     * durumların hepsini çözüyor.
     */
    private fun ayristir(context: Context, ham: String): Sonuc {
        return runCatching {
            val bas = ham.indexOf('{')
            val son = ham.lastIndexOf('}')
            if (bas < 0 || son <= bas) {
                return Sonuc(false, hata = context.getString(R.string.sc_anlasilmadi))
            }
            val o = JSONObject(ham.substring(bas, son + 1))

            if (!o.optBoolean("okundu", true)) {
                val sebep = o.optString("ipucu").ifBlank {
                    context.getString(R.string.sc_soru_yok)
                }
                return Sonuc(false, hata = sebep)
            }

            val c = Cozum(
                id = System.currentTimeMillis(),
                soru = o.optString("soru").trim(),
                cozum = o.optString("cozum").trim(),
                sonuc = o.optString("sonuc").trim(),
                konu = o.optString("konu").trim(),
                ipucu = o.optString("ipucu").trim(),
                zaman = System.currentTimeMillis()
            )
            if (c.soru.isBlank() && c.ipucu.isBlank()) {
                return Sonuc(false, hata = context.getString(R.string.sc_anlasilmadi))
            }
            Sonuc(true, cozum = c)
        }.getOrElse {
            android.util.Log.w(TAG, "Ayrıştırma", it)
            Sonuc(false, hata = context.getString(R.string.sc_anlasilmadi))
        }
    }

    // ══════════════════════════════════════════════════════════
    // Geçmiş
    // ══════════════════════════════════════════════════════════

    fun gecmis(c: Context): MutableList<Cozum> {
        val ham = p(c).getString(K_GECMIS, "[]") ?: "[]"
        val liste = mutableListOf<Cozum>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Cozum(
                        id = o.optLong("id"),
                        soru = o.optString("soru"),
                        cozum = o.optString("cozum"),
                        sonuc = o.optString("sonuc"),
                        konu = o.optString("konu"),
                        ipucu = o.optString("ipucu"),
                        zaman = o.optLong("z")
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "gecmis", it) }
        return liste
    }

    fun kaydet(c: Context, cozum: Cozum) {
        runCatching {
            val liste = gecmis(c)
            liste.add(0, cozum)
            val kirpik = if (liste.size > TAVAN) liste.take(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject()
                        .put("id", it.id).put("soru", it.soru)
                        .put("cozum", it.cozum).put("sonuc", it.sonuc)
                        .put("konu", it.konu).put("ipucu", it.ipucu)
                        .put("z", it.zaman)
                )
            }
            p(c).edit().putString(K_GECMIS, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "kaydet", it) }
    }

    fun sil(c: Context, id: Long) {
        val liste = gecmis(c)
        if (liste.removeAll { it.id == id }) {
            runCatching {
                val dizi = JSONArray()
                liste.forEach {
                    dizi.put(
                        JSONObject()
                            .put("id", it.id).put("soru", it.soru)
                            .put("cozum", it.cozum).put("sonuc", it.sonuc)
                            .put("konu", it.konu).put("ipucu", it.ipucu)
                            .put("z", it.zaman)
                    )
                }
                p(c).edit().putString(K_GECMIS, dizi.toString()).apply()
            }
        }
    }

    fun temizle(c: Context) {
        p(c).edit().remove(K_GECMIS).apply()
    }

    fun sayi(c: Context): Int = gecmis(c).size

    /**
     * Çözümü hata defterine ekler.
     *
     * "Bu soruyu çözemedim" diyen kullanıcı için: soru hata
     * defterine girer ve aralıklı tekrar programına alınır.
     */
    fun hataDefterineEkle(c: Context, cozum: Cozum): Boolean = runCatching {
        // QuizStore.Soru.gecerli en az 2 şık istiyor (`siklar.size >= 2`).
        // Fotoğraftan gelen soru çoktan seçmeli değil; ikinci şık olarak
        // "çözümü gör" konuyor. Böylece kayıt geçerli sayılıyor ve
        // hata defteri akışı bozulmuyor.
        val soru = QuizStore.Soru(
            id = cozum.id,
            lessonId = 0L,
            metin = cozum.soru.ifBlank { cozum.konu },
            siklar = listOf(
                cozum.sonuc.ifBlank { c.getString(R.string.sc_cozumu_gor) },
                c.getString(R.string.sc_tekrar_dene)
            ),
            dogru = 0,
            aciklama = cozum.cozum
        )
        Hatalarim.yanlisEkle(c, soru, cozum.konu.ifBlank { c.getString(R.string.sc_baslik) })
        true
    }.onFailure { android.util.Log.w(TAG, "hataDefterineEkle", it) }.getOrDefault(false)
}
