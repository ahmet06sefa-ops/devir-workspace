package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v9.6 — Feynman tekniği (öneri 33).
 *
 * ══════════════════════════════════════════════════════════════════
 * TEKNİK
 * ══════════════════════════════════════════════════════════════════
 * Richard Feynman'a atfedilen öğrenme yöntemi:
 *
 *   1. Konuyu seç
 *   2. **12 yaşındaki birine anlatıyormuş gibi** yaz/anlat
 *   3. Takıldığın, jargona kaçtığın yerleri bul — orası bilmediğin yer
 *   4. Kaynağa dön, o boşluğu doldur
 *   5. Tekrar anlat
 *
 * ── Neden işe yarıyor ──
 * Bir konuyu "anladım" sanmak kolay. Ama basit dille anlatmak
 * zorunda kaldığında bilgideki delikler ortaya çıkıyor. Jargon
 * kullanmak çoğu zaman anlamamayı gizliyor.
 *
 * ── Uygulamada nasıl ──
 * Kullanıcı yazar (veya sesli söyler — `SesliNot` altyapısı var),
 * AI şunları yapar:
 *   · Eksik/atlanmış noktaları listeler
 *   · Yanlış anlaşılmaları düzeltir
 *   · Fazla jargon kullanılan yerleri işaretler
 *   · 0-100 arası "anlaşılırlık" puanı verir
 *
 * ── Neden AI puanı düşük tutuluyor ──
 * İstemde "cömert davranma" deniyor. Her anlatıma 90 veren bir
 * değerlendirici işe yaramaz; kullanıcı gelişmediğini fark etmez.
 */
object Feynman {

    private const val TAG = "Feynman"
    private const val PREF = "feynman_v1"
    private const val K_KAYIT = "kayitlar_json"

    private const val TAVAN = 80

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════

    /**
     * Bir anlatım denemesi.
     *
     * @param puan 0-100 anlaşılırlık
     * @param eksikler AI'nın bulduğu boşluklar
     * @param jargon fazla teknik bulunan ifadeler
     */
    data class Deneme(
        val id: Long,
        val konu: String,
        val anlatim: String,
        val puan: Int,
        val ozet: String,
        val eksikler: List<String>,
        val jargon: List<String>,
        val zaman: Long
    ) {
        val seviye: Int
            get() = when {
                puan >= 75 -> 2   // iyi anlamışsın
                puan >= 50 -> 1   // kısmen
                else -> 0         // tekrar çalış
            }
    }

    data class Sonuc(val ok: Boolean, val deneme: Deneme? = null, val hata: String = "")

    // ══════════════════════════════════════════════════════════
    // İstem
    // ══════════════════════════════════════════════════════════

    private fun istem(konu: String, anlatim: String): String = buildString {
        appendLine("Bir öğrenci \"$konu\" konusunu kendi cümleleriyle anlattı.")
        appendLine("Feynman tekniğine göre değerlendir.")
        appendLine()
        appendLine("ÖĞRENCİNİN ANLATIMI:")
        appendLine("\"\"\"")
        appendLine(anlatim.take(3000))
        appendLine("\"\"\"")
        appendLine()
        appendLine("MUTLAK KURALLAR:")
        appendLine("1. Konunun alanını BAŞLIKTAN anla, kendi uzmanlık alanını varsayma.")
        appendLine("2. CÖMERT DAVRANMA. Amaç öğrencinin bilgi boşluklarını bulmak.")
        appendLine("   Her anlatıma yüksek puan veren değerlendirme işe yaramaz.")
        appendLine("3. Anlatımda GERÇEKTEN eksik olanı söyle; olmayan hata uydurma.")
        appendLine("4. Jargon kullanılmışsa işaretle — basit dille anlatmak esastır.")
        appendLine("5. Yapıcı ol: neyi eksik bıraktığını söylerken nasıl")
        appendLine("   tamamlayacağını da ima et.")
        appendLine("6. Türkçe yanıtla.")
        appendLine()
        appendLine("Yanıtı SADECE şu JSON biçiminde ver:")
        appendLine("{")
        appendLine("  \"puan\": 0-100 arası anlaşılırlık ve doğruluk,")
        appendLine("  \"ozet\": \"2-3 cümle genel değerlendirme\",")
        appendLine("  \"eksikler\": [\"atlanan veya yanlış anlaşılan nokta\", \"...\"],")
        appendLine("  \"jargon\": [\"fazla teknik kalan ifade\", \"...\"]")
        appendLine("}")
        appendLine()
        appendLine("Anlatım çok kısa veya anlamsızsa puanı düşük ver ve")
        appendLine("eksikler listesinde bunu belirt.")
    }

    // ══════════════════════════════════════════════════════════
    // Değerlendirme
    // ══════════════════════════════════════════════════════════

    /** En az bu kadar karakter yazılmadan değerlendirme yapılmıyor. */
    const val EN_AZ_UZUNLUK = 60

    /**
     * Anlatımı değerlendirir. **Arka planda çağrılmalı.**
     */
    fun degerlendir(c: Context, konu: String, anlatim: String): Sonuc {
        if (anlatim.trim().length < EN_AZ_UZUNLUK) {
            return Sonuc(false, hata = c.getString(R.string.fy_cok_kisa, EN_AZ_UZUNLUK))
        }
        if (!AiSettings.isReady(c)) {
            return Sonuc(false, hata = c.getString(R.string.sc_ai_kapali))
        }

        val yanit = runCatching {
            AiClient.sadeIstek(c, istem(konu, anlatim), butce = 2048)
        }.getOrElse {
            android.util.Log.w(TAG, "AI çağrısı", it)
            return Sonuc(false, hata = c.getString(R.string.sc_ai_hata))
        }

        if (!yanit.ok) {
            return Sonuc(false, hata = yanit.text.ifBlank { c.getString(R.string.sc_ai_hata) })
        }

        return ayristir(c, konu, anlatim, yanit.text)
    }

    private fun ayristir(
        c: Context, konu: String, anlatim: String, ham: String
    ): Sonuc = runCatching {
        val bas = ham.indexOf('{')
        val son = ham.lastIndexOf('}')
        if (bas < 0 || son <= bas) {
            return Sonuc(false, hata = c.getString(R.string.sc_anlasilmadi))
        }
        val o = JSONObject(ham.substring(bas, son + 1))

        fun diziOku(ad: String): List<String> {
            val d = o.optJSONArray(ad) ?: return emptyList()
            return (0 until d.length()).mapNotNull {
                d.optString(it).trim().takeIf { s -> s.isNotBlank() }
            }
        }

        val deneme = Deneme(
            id = System.currentTimeMillis(),
            konu = konu,
            anlatim = anlatim,
            puan = o.optInt("puan", 0).coerceIn(0, 100),
            ozet = o.optString("ozet").trim(),
            eksikler = diziOku("eksikler"),
            jargon = diziOku("jargon"),
            zaman = System.currentTimeMillis()
        )
        Sonuc(true, deneme = deneme)
    }.getOrElse {
        android.util.Log.w(TAG, "Ayrıştırma", it)
        Sonuc(false, hata = c.getString(R.string.sc_anlasilmadi))
    }

    // ══════════════════════════════════════════════════════════
    // Geçmiş
    // ══════════════════════════════════════════════════════════

    fun denemeler(c: Context): MutableList<Deneme> {
        val ham = p(c).getString(K_KAYIT, "[]") ?: "[]"
        val liste = mutableListOf<Deneme>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                fun diziOku(ad: String): List<String> {
                    val d = o.optJSONArray(ad) ?: return emptyList()
                    return (0 until d.length()).map { d.optString(it) }
                }
                liste.add(
                    Deneme(
                        id = o.optLong("id"),
                        konu = o.optString("k"),
                        anlatim = o.optString("a"),
                        puan = o.optInt("p"),
                        ozet = o.optString("o"),
                        eksikler = diziOku("e"),
                        jargon = diziOku("j"),
                        zaman = o.optLong("z")
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "denemeler", it) }
        return liste
    }

    fun kaydet(c: Context, deneme: Deneme) {
        runCatching {
            val liste = denemeler(c)
            liste.add(0, deneme)
            val kirpik = if (liste.size > TAVAN) liste.take(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach { d ->
                dizi.put(
                    JSONObject()
                        .put("id", d.id).put("k", d.konu)
                        // Anlatım uzun olabilir; ilk 1500 karakter yeter
                        .put("a", d.anlatim.take(1500))
                        .put("p", d.puan).put("o", d.ozet)
                        .put("e", JSONArray(d.eksikler))
                        .put("j", JSONArray(d.jargon))
                        .put("z", d.zaman)
                )
            }
            p(c).edit().putString(K_KAYIT, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "kaydet", it) }
    }

    /** Bir konudaki gelişim — aynı konuyu tekrar anlatınca. */
    fun gelisim(c: Context, konu: String): Pair<Int, Int>? {
        val liste = denemeler(c)
            .filter { it.konu.equals(konu, ignoreCase = true) }
            .sortedBy { it.zaman }
        if (liste.size < 2) return null
        return liste.first().puan to liste.last().puan
    }

    fun sayi(c: Context): Int = denemeler(c).size

    fun ortalamaPuan(c: Context): Int? {
        val liste = denemeler(c)
        if (liste.isEmpty()) return null
        return liste.map { it.puan }.average().toInt()
    }

    fun sil(c: Context, id: Long) {
        val liste = denemeler(c)
        if (liste.removeAll { it.id == id }) {
            runCatching {
                val dizi = JSONArray()
                liste.forEach { d ->
                    dizi.put(
                        JSONObject()
                            .put("id", d.id).put("k", d.konu).put("a", d.anlatim)
                            .put("p", d.puan).put("o", d.ozet)
                            .put("e", JSONArray(d.eksikler))
                            .put("j", JSONArray(d.jargon)).put("z", d.zaman)
                    )
                }
                p(c).edit().putString(K_KAYIT, dizi.toString()).apply()
            }
        }
    }

    fun temizle(c: Context) {
        p(c).edit().remove(K_KAYIT).apply()
    }
}
