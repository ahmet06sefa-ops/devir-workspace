package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * v7.20 — Ders için internetten kaynak (PDF / video) bulur.
 *
 * ÖNEMLİ TASARIM KARARI — uydurma link sorunu:
 * Dil modelleri internete bakmadan link üretmeye zorlanırsa **var olmayan
 * adresler uydurur**. Bu çok yaygın bir hatadır. Bu yüzden burada model
 * asla "kafadan" link vermiyor:
 *
 *   · PDF/site araması  → Gemini "grounding" (gerçek Google Arama aracı)
 *   · Video araması     → YouTube Data API (anahtar varsa) ya da grounding
 *
 * Her iki yolda da link, arama motorunun döndürdüğü **gerçek** sonuçtur.
 * Ayrıca bulunan adresler `dogrula()` ile HTTP HEAD isteğiyle sınanabilir.
 */
object KaynakBulucu {

    private const val TAG = "KaynakBulucu"

    /** Bulunan tek bir kaynak. */
    data class Kaynak(
        val baslik: String,
        val url: String,
        val tur: Tur,
        val aciklama: String = "",
        val kanal: String = "",
        val sure: String = "",
        var secili: Boolean = false,
        var dogrulandi: Boolean? = null   // null = denenmedi
    )

    enum class Tur { PDF, VIDEO, SAYFA }

    class Sonuc(val ok: Boolean, val mesaj: String, val kaynaklar: List<Kaynak>)

    // ═══════════════════════════════════════════════════════════════
    // GENEL GİRİŞ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ders için kaynak arar.
     * @param dersAdi ders başlığı
     * @param kursAdi bağlam için kurs adı (örn. "AutoCAD 2D")
     * @param video true ise video, false ise PDF/doküman aranır
     */
    fun ara(context: Context, dersAdi: String, kursAdi: String, video: Boolean): Sonuc {
        if (!AiSettings.isOnlineMode(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_offline_mode), emptyList())
        }
        if (!AiClient.isOnline(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_no_net), emptyList())
        }

        // YouTube anahtarı varsa video için en güvenilir yol odur
        if (video && AiSettings.hasYoutubeKey(context)) {
            try {
                val yt = youtubeAra(context, dersAdi, kursAdi)
                if (yt.ok && yt.kaynaklar.isNotEmpty()) return yt
            } catch (e: Exception) {
                android.util.Log.w(TAG, "YouTube araması atlandı", e)
            }
        }

        // v7.24: sağlayıcılar arası geçiş — biri olmazsa diğeri denenir
        val saglayicilar = AiClient.saglayiciSirasi(context)
        val ilk = saglayicilar.firstOrNull() ?: AiClient.Provider.GEMINI
        var sonMesaj = ""

        for (saglayici in saglayicilar) {
            if (!AiSettings.hasKeyFor(context, saglayici.id) &&
                !(saglayici == ilk && AiSettings.getApiKey(context).isNotBlank())
            ) continue

            val sonuc = try {
                if (saglayici == AiClient.Provider.GEMINI) {
                    groundingAra(context, dersAdi, kursAdi, video)
                } else {
                    openAiIleAra(context, saglayici, dersAdi, kursAdi, video)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Arama başarısız (${saglayici.id})", e)
                Sonuc(
                    false,
                    context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen"),
                    emptyList()
                )
            }

            if (sonuc.ok && sonuc.kaynaklar.isNotEmpty()) {
                if (saglayici != ilk) {
                    android.util.Log.i(TAG, "Sağlayıcı değişti: ${ilk.id} -> ${saglayici.id}")
                }
                // v7.43: arama bitince bildir (öneri 22)
                try {
                    val videoSayi = if (video) sonuc.kaynaklar.size else 0
                    val pdfSayi = if (video) 0 else sonuc.kaynaklar.size
                    BildirimUretici.kaynakBulundu(context, pdfSayi, videoSayi)
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Kaynak bildirimi gönderilemedi", e)
                }
                return sonuc
            }
            sonMesaj = sonuc.mesaj
            if (!AiClient.saglayiciDegistirmeliMi(sonMesaj)) break
        }
        return Sonuc(
            false,
            sonMesaj.ifBlank { context.getString(R.string.src_err_none) },
            emptyList()
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 1) GEMINI GROUNDING — gerçek Google Arama
    // ═══════════════════════════════════════════════════════════════

    /**
     * v7.23: Önce Google Arama aracıyla; araç kabul edilmezse araçsız dener.
     * Araçsız modda model kendi bilgisinden sonuç verir (link doğrulaması
     * yine yapılır, uydurma adresler elenir).
     */
    private fun groundingAra(
        context: Context,
        dersAdi: String,
        kursAdi: String,
        video: Boolean
    ): Sonuc {
        val ilk = groundingAraTek(context, dersAdi, kursAdi, video, aracKullan = true)
        if (ilk.ok && ilk.kaynaklar.isNotEmpty()) return ilk
        android.util.Log.w(TAG, "Aramalı deneme sonuç vermedi, araçsız deneniyor")
        val ikinci = groundingAraTek(context, dersAdi, kursAdi, video, aracKullan = false)
        return if (ikinci.ok && ikinci.kaynaklar.isNotEmpty()) ikinci else ilk
    }

    private fun groundingAraTek(
        context: Context,
        dersAdi: String,
        kursAdi: String,
        video: Boolean,
        aracKullan: Boolean
    ): Sonuc {
        val key = AiSettings.getApiKey(context)
        if (key.isBlank()) {
            return Sonuc(false, context.getString(R.string.ai_err_no_key), emptyList())
        }
        // v7.24: sağlayıcı seçimi ara() içinde yapılıyor; burada Gemini yolu

        // v7.34: ücretsiz mod açıksa kredi harcayan model seçilmez
        val model = AiClient.guvenliModel(context, AiClient.Provider.GEMINI)
        if (model.isBlank()) {
            return Sonuc(
                false,
                context.getString(
                    R.string.ai_err_no_free_model, AiClient.Provider.GEMINI.label
                ),
                emptyList()
            )
        }
        val url = "${AiClient.Provider.GEMINI.endpoint}/$model:generateContent?key=$key"

        val istem = if (video) {
            """"$kursAdi" kursundaki "$dersAdi" konusunu anlatan Türkçe YouTube videoları bul.

Google'da ara ve GERÇEKTEN BULDUĞUN videoları listele.

Kurallar:
- Sadece aramada karşına çıkan gerçek videoları yaz
- Link uydurma, tahmin etme
- Türkçe içerik öncelikli
- En fazla 6 sonuç

Her sonuç için JSON satırı üret:
{"baslik":"video başlığı","url":"https://www.youtube.com/watch?v=...","kanal":"kanal adı","aciklama":"tek cümle"}

Çıktı biçimi (başka hiçbir şey yazma):
{"sonuclar":[ ... ]}"""
        } else {
            """"$kursAdi" kursundaki "$dersAdi" konusuyla ilgili ücretsiz PDF ders notu, kılavuz veya doküman bul.

Google'da ara ve GERÇEKTEN BULDUĞUN kaynakları listele.

Kurallar:
- Sadece aramada karşına çıkan gerçek adresleri yaz
- Link uydurma, tahmin etme
- Üniversite, kamu kurumu, resmi üretici siteleri öncelikli
- Türkçe kaynaklar öncelikli, yoksa İngilizce
- PDF ise "tur":"pdf", normal sayfaysa "tur":"sayfa"
- En fazla 6 sonuç

Her sonuç için:
{"baslik":"belge adı","url":"https://...","tur":"pdf","aciklama":"tek cümle"}

Çıktı biçimi (başka hiçbir şey yazma):
{"sonuclar":[ ... ]}"""
        }

        val govde = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", istem)))
                )
            )
            // ★ Gerçek Google Arama aracı — uydurma linki engelleyen kısım.
            // v7.23: generateContent API camelCase bekliyor ("googleSearch").
            // snake_case yazımı sessizce yok sayılıyordu.
            .apply {
                if (aracKullan) {
                    put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
                }
            }
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.1)
                    // v7.23: Gemini 3 düşünen model — düşünme token'ları çıktı
                    // bütçesinden düşüyor. Az bütçe verilince yanıt BOŞ dönüyordu.
                    .put("maxOutputTokens", 16384)
                    .put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
            )
            .toString()

        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(govde) }

        val kod = conn.responseCode
        val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
        val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (kod !in 200..299) {
            val detay = try {
                JSONObject(cevap).optJSONObject("error")?.optString("message") ?: ""
            } catch (_: Exception) {
                ""
            }
            return Sonuc(false, context.getString(R.string.src_err_http, kod, detay.take(140)), emptyList())
        }

        val kok = JSONObject(cevap)
        val aday = kok.optJSONArray("candidates")?.optJSONObject(0)
        val parcalar = aday?.optJSONObject("content")?.optJSONArray("parts")
        val metin = buildString {
            if (parcalar != null) {
                for (i in 0 until parcalar.length()) {
                    append(parcalar.optJSONObject(i)?.optString("text").orEmpty())
                }
            }
        }.trim()

        // v7.23: boş yanıt sebebini kullanıcıya açıkla (sessiz başarısızlık olmasın)
        if (metin.isBlank()) {
            val bitis = aday?.optString("finishReason").orEmpty()
            val engel = kok.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            val sebep = when {
                bitis == "MAX_TOKENS" -> context.getString(R.string.src_err_max_tokens)
                engel.isNotBlank() -> context.getString(R.string.ai_err_blocked, engel)
                bitis == "SAFETY" -> context.getString(R.string.ai_err_blocked, "SAFETY")
                else -> context.getString(R.string.src_err_none)
            }
            return Sonuc(false, sebep, emptyList())
        }

        val liste = mutableListOf<Kaynak>()
        liste.addAll(jsonSonuclariAyikla(metin, video))

        // Grounding meta verisinden gerçek kaynak adreslerini de topla.
        // Model metinde link vermese bile arama motorunun bulduğu siteler burada.
        liste.addAll(groundingKaynaklari(aday, video, liste.map { it.url }.toSet()))

        return if (liste.isEmpty()) {
            Sonuc(false, context.getString(R.string.src_err_none), emptyList())
        } else {
            Sonuc(true, "", liste.distinctBy { normalUrl(it.url) }.take(8))
        }
    }

    /**
     * v7.23: OpenAI/OpenRouter yolu. Gerçek arama aracı yoktur; model kendi
     * bilgisinden aday adresler verir, `dogrula()` ölü linkleri eler.
     */
    private fun openAiIleAra(
        context: Context,
        saglayici: AiClient.Provider,
        dersAdi: String,
        kursAdi: String,
        video: Boolean
    ): Sonuc {
        val key = AiSettings.getApiKey(context)
        val hedef = saglayici.endpoint.ifBlank { AiSettings.getCustomEndpoint(context) }
        if (hedef.isBlank()) {
            return Sonuc(false, context.getString(R.string.src_err_none), emptyList())
        }
        val model = AiClient.guvenliModel(context, saglayici)
        if (model.isBlank()) {
            return Sonuc(
                false,
                context.getString(R.string.ai_err_no_free_model, saglayici.label),
                emptyList()
            )
        }

        val istem = if (video) {
            "\"$kursAdi\" kursundaki \"$dersAdi\" konusunu anlatan Türkçe YouTube " +
                "videoları öner. Yalnızca var olduğundan EMİN olduğun, bilinen " +
                "eğitim kanallarının videolarını yaz. Emin değilsen listeleme.\n" +
                "Çıktı: {\"sonuclar\":[{\"baslik\":\"...\",\"url\":\"https://www.youtube.com/watch?v=...\"," +
                "\"kanal\":\"...\",\"aciklama\":\"...\"}]}"
        } else {
            "\"$kursAdi\" kursundaki \"$dersAdi\" konusuyla ilgili ücretsiz PDF ders notu " +
                "veya kılavuz öner. Üniversite ve resmi kurum kaynakları öncelikli. " +
                "Yalnızca var olduğundan EMİN olduğun adresleri yaz.\n" +
                "Çıktı: {\"sonuclar\":[{\"baslik\":\"...\",\"url\":\"https://...\"," +
                "\"tur\":\"pdf\",\"aciklama\":\"...\"}]}"
        }

        return try {
            val govde = JSONObject()
                .put("model", model)
                .put(
                    "messages",
                    JSONArray().put(JSONObject().put("role", "user").put("content", istem))
                )
                .put("temperature", 0.2)
                .put("max_tokens", 2048)
                .toString()

            val conn = (URL(hedef).openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 25000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $key")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(govde) }
            val kod = conn.responseCode
            val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
            val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()

            if (kod !in 200..299) {
                return Sonuc(false, context.getString(R.string.src_err_http, kod, ""), emptyList())
            }
            val metin = JSONObject(cevap).optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content").orEmpty()
            val liste = jsonSonuclariAyikla(metin, video)
            if (liste.isEmpty()) Sonuc(false, context.getString(R.string.src_err_none), emptyList())
            else Sonuc(true, "", liste.distinctBy { normalUrl(it.url) }.take(8))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "OpenAI araması başarısız", e)
            Sonuc(false, context.getString(R.string.ai_err_generic, e.message ?: ""), emptyList())
        }
    }

    /** Grounding meta verisindeki gerçek web adreslerini çıkarır. */
    private fun groundingKaynaklari(
        aday: JSONObject?,
        video: Boolean,
        mevcut: Set<String>
    ): List<Kaynak> {
        val cikti = mutableListOf<Kaynak>()
        try {
            val meta = aday?.optJSONObject("groundingMetadata") ?: return cikti
            val parcalar = meta.optJSONArray("groundingChunks") ?: return cikti
            for (i in 0 until parcalar.length()) {
                val web = parcalar.optJSONObject(i)?.optJSONObject("web") ?: continue
                val u = web.optString("uri", "").trim()
                val b = web.optString("title", "").trim()
                if (u.isBlank() || mevcut.contains(u)) continue
                val tur = when {
                    u.contains("youtube.com", true) || u.contains("youtu.be", true) -> Tur.VIDEO
                    u.endsWith(".pdf", true) -> Tur.PDF
                    else -> Tur.SAYFA
                }
                // Video aranırken sayfa sonucu, PDF aranırken video sonucu gösterme
                if (video && tur != Tur.VIDEO) continue
                if (!video && tur == Tur.VIDEO) continue
                cikti.add(Kaynak(b.ifBlank { u.take(60) }, u, tur))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Grounding meta okunamadı", e)
        }
        return cikti
    }

    /** Modelin metin çıktısındaki JSON sonuçlarını ayıklar. */
    private fun jsonSonuclariAyikla(ham: String, video: Boolean): List<Kaynak> {
        val cikti = mutableListOf<Kaynak>()
        try {
            var s = ham.trim()
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas < 0 || son <= bas || son >= s.length) return cikti
            val o = JSONObject(s.take(son + 1).drop(bas))
            val dizi = o.optJSONArray("sonuclar") ?: return cikti

            for (i in 0 until dizi.length()) {
                val e = dizi.optJSONObject(i) ?: continue
                val u = e.optString("url", "").trim()
                if (!gecerliUrl(u)) continue
                val turMetin = e.optString("tur", "").lowercase()
                val tur = when {
                    u.contains("youtube.com", true) || u.contains("youtu.be", true) -> Tur.VIDEO
                    turMetin == "pdf" || u.endsWith(".pdf", true) -> Tur.PDF
                    else -> Tur.SAYFA
                }
                if (video && tur != Tur.VIDEO) continue
                cikti.add(
                    Kaynak(
                        baslik = e.optString("baslik", "").trim().ifBlank { u.take(60) },
                        url = u,
                        tur = tur,
                        aciklama = e.optString("aciklama", "").trim(),
                        kanal = e.optString("kanal", "").trim()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "JSON sonuçları ayıklanamadı", e)
        }
        return cikti
    }

    // ═══════════════════════════════════════════════════════════════
    // 2) YOUTUBE DATA API — en güvenilir video kaynağı
    // ═══════════════════════════════════════════════════════════════

    private fun youtubeAra(context: Context, dersAdi: String, kursAdi: String): Sonuc {
        val key = AiSettings.getYoutubeKey(context)
        if (key.isBlank()) return Sonuc(false, "", emptyList())

        return try {
            val sorgu = URLEncoder.encode("$kursAdi $dersAdi dersi anlatım", "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet&type=video&maxResults=8&relevanceLanguage=tr" +
                "&regionCode=TR&q=$sorgu&key=$key"

            val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 40000
            }
            val kod = conn.responseCode
            val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
            val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()

            if (kod !in 200..299) {
                android.util.Log.w(TAG, "YouTube API hatası: $kod")
                return Sonuc(false, "", emptyList())
            }

            val ogeler = JSONObject(cevap).optJSONArray("items") ?: return Sonuc(false, "", emptyList())
            val liste = mutableListOf<Kaynak>()
            for (i in 0 until ogeler.length()) {
                val oge = ogeler.optJSONObject(i) ?: continue
                val vid = oge.optJSONObject("id")?.optString("videoId", "").orEmpty()
                if (vid.isBlank()) continue
                val sn = oge.optJSONObject("snippet")
                liste.add(
                    Kaynak(
                        baslik = sn?.optString("title", "").orEmpty().ifBlank { "Video" },
                        url = "https://www.youtube.com/watch?v=$vid",
                        tur = Tur.VIDEO,
                        aciklama = sn?.optString("description", "").orEmpty().take(120),
                        kanal = sn?.optString("channelTitle", "").orEmpty(),
                        // YouTube API'den geldiği için varlığı kesin
                        dogrulandi = true
                    )
                )
            }
            if (liste.isEmpty()) Sonuc(false, "", emptyList())
            else Sonuc(true, "", liste)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "YouTube araması başarısız", e)
            Sonuc(false, "", emptyList())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3) LİNK DOĞRULAMA — adres gerçekten açılıyor mu
    // ═══════════════════════════════════════════════════════════════

    /**
     * HTTP HEAD isteğiyle adresin yaşadığını sınar.
     * Uydurma veya ölü linkleri kullanıcıya göstermeden eler.
     */
    fun dogrula(kaynak: Kaynak): Boolean {
        // YouTube API sonuçları zaten kesin
        if (kaynak.dogrulandi == true) return true
        return try {
            val conn = (URL(kaynak.url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 9000
                readTimeout = 9000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GunlukAsistan")
            }
            val kod = conn.responseCode
            conn.disconnect()
            // Bazı sunucular HEAD'i reddeder (405) ama sayfa vardır
            kod in 200..299 || kod == 405 || kod == 403
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Doğrulama başarısız: ${kaynak.url}", e)
            false
        }
    }

    /** Listedeki tüm kaynakları doğrular, sonucu nesnelere işler. */
    fun hepsiniDogrula(liste: List<Kaynak>) {
        liste.forEach { k ->
            if (k.dogrulandi == null) k.dogrulandi = dogrula(k)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Yardımcılar
    // ═══════════════════════════════════════════════════════════════

    private fun gecerliUrl(u: String): Boolean =
        u.startsWith("http://", true) || u.startsWith("https://", true)

    /** Karşılaştırma için adresi sadeleştirir (aynı link iki kez gelmesin). */
    private fun normalUrl(u: String): String =
        u.trim().trimEnd('/').removePrefix("https://").removePrefix("http://")
            .removePrefix("www.").lowercase()
}
