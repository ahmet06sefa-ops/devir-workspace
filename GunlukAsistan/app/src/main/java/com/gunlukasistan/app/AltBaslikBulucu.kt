package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * v7.35 — Bir konunun alt başlıklarını yapay zekâ + Google Arama ile bulur.
 *
 * Neden ayrı bir dosya?
 * v7.34'e kadar "Alt madde ekle" yalnızca elle yazma kutusu açıyordu.
 * Kullanıcı asistana sorup konuları buldurabiliyordu ama sonucu konuya
 * ekleyecek hiçbir yol yoktu. Bu sınıf o boşluğu dolduruyor.
 *
 * Tasarım:
 *  - Gemini'de gerçek Google Arama aracı kullanılır (googleSearch, camelCase).
 *  - Araç kabul edilmezse araçsız ikinci deneme yapılır.
 *  - Diğer sağlayıcılarda OpenAI uyumlu uç nokta kullanılır.
 *  - v7.34 ücretsiz mod korunur: guvenliModel() boş dönerse istek atılmaz.
 *  - Sonuç KULLANICIYA ONAYLATILIR (TopicsFragment). Buradan doğrudan kayıt yok.
 */
object AltBaslikBulucu {

    private const val TAG = "AltBaslikBulucu"

    /** Bulunan alt başlıklar veya hata sebebi. */
    class Sonuc(val ok: Boolean, val mesaj: String, val maddeler: List<String>)

    /**
     * Konu başlığından alt başlık listesi üretir.
     *
     * @param konuBasligi ör. "Betonarme Kolon Tasarımı"
     * @param mevcut zaten eklenmiş maddeler — tekrar önerilmemesi için
     */
    fun bul(context: Context, konuBasligi: String, mevcut: List<String> = emptyList()): Sonuc {
        if (konuBasligi.isBlank()) {
            return Sonuc(false, context.getString(R.string.sub_ai_err_empty), emptyList())
        }
        if (!AiSettings.isOnlineMode(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_offline_mode), emptyList())
        }
        if (!AiClient.isOnline(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_no_net), emptyList())
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Sonuc(false, context.getString(R.string.ai_err_no_key), emptyList())
        }

        // v7.24 deseni: sağlayıcılar arası otomatik geçiş
        val saglayicilar = AiClient.saglayiciSirasi(context)
        val ilk = saglayicilar.firstOrNull() ?: AiClient.Provider.GEMINI
        var sonMesaj = ""

        for (saglayici in saglayicilar) {
            val anahtar = AiSettings.getKeyFor(context, saglayici.id)
                .ifBlank { if (saglayici == ilk) AiSettings.getApiKey(context) else "" }
            if (anahtar.isBlank()) continue

            val sonuc = try {
                if (saglayici == AiClient.Provider.GEMINI) {
                    geminiIle(context, anahtar, konuBasligi, mevcut)
                } else {
                    openAiIle(context, saglayici, anahtar, konuBasligi, mevcut)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alt başlık aranamadı (" + saglayici.id + ")", e)
                Sonuc(
                    false,
                    context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen"),
                    emptyList()
                )
            }

            if (sonuc.ok && sonuc.maddeler.isNotEmpty()) return sonuc
            sonMesaj = sonuc.mesaj
            if (!AiClient.saglayiciDegistirmeliMi(sonMesaj)) break
        }

        return Sonuc(
            false,
            sonMesaj.ifBlank { context.getString(R.string.sub_ai_err_none) },
            emptyList()
        )
    }

    // ─────────────────── İstem ───────────────────

    private fun istemKur(konuBasligi: String, mevcut: List<String>): String {
        val haric = if (mevcut.isEmpty()) "" else {
            "\nBu maddeler ZATEN VAR, tekrar yazma:\n" +
                mevcut.take(40).joinToString("\n") { "- " + it }
        }
        return "\"" + konuBasligi + "\" konusunun alt başlıklarını çıkar.\n\n" +
            "Bu bir çalışma/ders konusu. Güncel kaynaklardan yararlanarak " +
            "konunun mantıklı sırayla öğrenilmesi gereken alt başlıklarını listele.\n\n" +
            "Kurallar:\n" +
            "- Her madde tek satır, kısa ve net olsun (en fazla 80 karakter)\n" +
            "- Öğrenme sırasına göre dizilsin (kolaydan zora)\n" +
            "- 6 ile 14 arası madde üret\n" +
            "- Madde başına numara, tire veya emoji KOYMA\n" +
            "- Türkçe yaz\n" +
            "- Uydurma, konuyla ilgisiz madde ekleme" + haric + "\n\n" +
            "Çıktı biçimi (başka hiçbir şey yazma):\n" +
            "{\"maddeler\":[\"birinci alt başlık\",\"ikinci alt başlık\"]}"
    }

    // ─────────────────── Gemini (Google Arama araçlı) ───────────────────

    private fun geminiIle(
        context: Context,
        key: String,
        konuBasligi: String,
        mevcut: List<String>
    ): Sonuc {
        val ilk = geminiTek(context, key, konuBasligi, mevcut, aracKullan = true)
        if (ilk.ok && ilk.maddeler.isNotEmpty()) return ilk
        // Araç kabul edilmediyse modelin kendi bilgisiyle dene
        android.util.Log.w(TAG, "Aramalı deneme boş döndü, araçsız deneniyor")
        val ikinci = geminiTek(context, key, konuBasligi, mevcut, aracKullan = false)
        return if (ikinci.ok && ikinci.maddeler.isNotEmpty()) ikinci else ilk
    }

    private fun geminiTek(
        context: Context,
        key: String,
        konuBasligi: String,
        mevcut: List<String>,
        aracKullan: Boolean
    ): Sonuc {
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

        val url = AiClient.Provider.GEMINI.endpoint + "/" + model + ":generateContent?key=" + key
        val istem = istemKur(konuBasligi, mevcut)

        val govde = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", istem)))
                )
            )
            .apply {
                // v7.23: camelCase şart — snake_case sessizce yok sayılıyor
                if (aracKullan) {
                    put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
                }
            }
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    // v7.23: Gemini 3 düşünen model — bütçe küçükse boş yanıt döner
                    .put("maxOutputTokens", AiClient.tokenButcesi(model, 2048))
                    .apply {
                        AiClient.dusunmeAyari(model)?.let { put("thinkingConfig", it) }
                    }
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
            return Sonuc(false, hataOku(context, kod, cevap), emptyList())
        }

        val metin = geminiMetniAl(cevap)
        if (metin.isBlank()) {
            return Sonuc(false, context.getString(R.string.sub_ai_err_none), emptyList())
        }
        val maddeler = ayristir(metin, mevcut)
        return if (maddeler.isEmpty()) {
            Sonuc(false, context.getString(R.string.sub_ai_err_none), emptyList())
        } else {
            Sonuc(true, "", maddeler)
        }
    }

    private fun geminiMetniAl(cevap: String): String = try {
        val kokler = JSONObject(cevap).optJSONArray("candidates")
        val parcalar = kokler?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
        val sb = StringBuilder()
        if (parcalar != null) {
            for (i in 0 until parcalar.length()) {
                sb.append(parcalar.optJSONObject(i)?.optString("text", "").orEmpty())
            }
        }
        sb.toString()
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Gemini yanıtı okunamadı", e)
        ""
    }

    // ─────────────────── OpenAI uyumlu ───────────────────

    private fun openAiIle(
        context: Context,
        saglayici: AiClient.Provider,
        key: String,
        konuBasligi: String,
        mevcut: List<String>
    ): Sonuc {
        val hedef = saglayici.endpoint.ifBlank { AiSettings.getCustomEndpoint(context) }
        if (hedef.isBlank()) {
            return Sonuc(false, context.getString(R.string.ai_err_no_endpoint), emptyList())
        }
        val model = AiClient.guvenliModel(context, saglayici)
        if (model.isBlank()) {
            return Sonuc(
                false,
                context.getString(R.string.ai_err_no_free_model, saglayici.label),
                emptyList()
            )
        }

        val govde = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", istemKur(konuBasligi, mevcut))
                )
            )
            .put("temperature", 0.2)
            .put("max_tokens", 1500)
            .toString()

        val conn = (URL(hedef).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer " + key)
            if (saglayici == AiClient.Provider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://gunlukasistan.app")
                setRequestProperty("X-Title", "Gunluk Asistan")
            }
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(govde) }

        val kod = conn.responseCode
        val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
        val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (kod !in 200..299) {
            return Sonuc(false, hataOku(context, kod, cevap), emptyList())
        }

        val metin = try {
            JSONObject(cevap).optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                .orEmpty()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yanıt okunamadı", e)
            ""
        }

        val maddeler = ayristir(metin, mevcut)
        return if (maddeler.isEmpty()) {
            Sonuc(false, context.getString(R.string.sub_ai_err_none), emptyList())
        } else {
            Sonuc(true, "", maddeler)
        }
    }

    // ─────────────────── Ayrıştırma ───────────────────

    /**
     * Modelin çıktısını madde listesine çevirir.
     * Savunmacı: JSON bozuksa düz satırlardan da toplamayı dener.
     */
    fun ayristir(ham: String, mevcut: List<String> = emptyList()): List<String> {
        if (ham.isBlank()) return emptyList()
        val bulunan = mutableListOf<String>()

        // 1) Düzgün JSON yolu
        try {
            var s = ham.trim()
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas >= 0 && son > bas) {
                val o = JSONObject(s.substring(bas, son + 1))
                val dizi = o.optJSONArray("maddeler")
                if (dizi != null) {
                    for (i in 0 until dizi.length()) {
                        temizle(dizi.optString(i, ""))?.let { bulunan.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "JSON ayrıştırılamadı, düz metne düşülüyor", e)
        }

        // 2) JSON boşsa: satır satır topla (model kural dışına çıkmış olabilir)
        if (bulunan.isEmpty()) {
            ham.lines().forEach { satir ->
                val t = satir.trim()
                if (t.isBlank()) return@forEach
                if (t.startsWith("{") || t.startsWith("}") || t.startsWith("```")) return@forEach
                if (t.startsWith("\"maddeler\"")) return@forEach
                temizle(t)?.let { bulunan.add(it) }
            }
        }

        // Tekrarları ve zaten eklenmiş olanları ele
        val mevcutNorm = mevcut.map { normalle(it) }.toSet()
        val gorulen = mutableSetOf<String>()
        return bulunan.filter { madde ->
            val n = normalle(madde)
            if (n.isBlank() || n in mevcutNorm || n in gorulen) false
            else {
                gorulen.add(n)
                true
            }
        }.take(20)
    }

    /** Satır başındaki numara/tire/emoji ve tırnakları temizler. */
    private fun temizle(ham: String): String? {
        var t = ham.trim()
        if (t.isBlank()) return null
        t = t.trim('"', ',', '\u2022', '-', '*', ' ', '\t')
        // "1." / "1)" / "01 -" gibi başlangıçlar
        t = t.replace(Regex("^\\s*\\d{1,2}\\s*[.)\\-:]\\s*"), "")
        t = t.trim('"', ',', ' ')
        if (t.length < 3) return null
        if (t.length > 120) t = t.take(120).trimEnd() + "…"
        // JSON kalıntısı içeren satırları ele
        if (t.contains("\":") || t.startsWith("[") || t.endsWith("]")) return null
        return t
    }

    /** Türkçe duyarlı karşılaştırma anahtarı. */
    private fun normalle(s: String): String =
        s.lowercase(java.util.Locale("tr", "TR"))
            .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim()

    /** HTTP hatasını okunur Türkçe mesaja çevirir. */
    private fun hataOku(context: Context, kod: Int, cevap: String): String {
        val ayrinti = try {
            JSONObject(cevap).optJSONObject("error")?.optString("message", "").orEmpty()
        } catch (_: Exception) {
            ""
        }
        return when (kod) {
            401, 403 -> context.getString(R.string.ai_err_key_bad)
            402 -> context.getString(R.string.ai_err_credit)
            429 -> context.getString(R.string.ai_err_quota)
            in 500..599 -> context.getString(R.string.ai_err_server)
            else -> context.getString(
                R.string.ai_err_generic,
                if (ayrinti.isBlank()) "HTTP " + kod else ayrinti.take(140)
            )
        }
    }
}
