package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * v7.21 — Yapay zekâ ile otomatik kurs müfredatı üretir.
 *
 * Kullanıcı sadece kurs adını yazar ("SAP2000", "Excel", "Zemin Mekaniği"),
 * gerisini bu sınıf halleder:
 *   · İnternette gerçek arama yapar (Gemini google_search)
 *   · Sektörde kullanılan gerçek müfredatı bulur
 *   · Bölüm → ders hiyerarşisi kurar
 *   · Her derse süre ve açıklama yazar
 *   · Doğrudan Store'a kaydeder
 *
 * Tasarım kararı: müfredat "uydurulmuş" olmamalı. Bu yüzden grounding açık —
 * model gerçek eğitim programlarına, üniversite ders içeriklerine ve
 * sertifika programlarına bakarak üretir.
 */
object KursUretici {

    private const val TAG = "KursUretici"

    /** Üretilen müfredattaki tek ders. */
    data class UretilenDers(val baslik: String, val dakika: Int, val aciklama: String)

    /** Üretilen müfredattaki bölüm. */
    data class UretilenBolum(val baslik: String, val dersler: List<UretilenDers>)

    /** Tam müfredat. */
    data class Mufredat(
        val kursAdi: String,
        val emoji: String,
        val aciklama: String,
        val bolumler: List<UretilenBolum>
    ) {
        val dersSayisi: Int get() = bolumler.sumOf { it.dersler.size }
        val toplamDakika: Int get() = bolumler.sumOf { b -> b.dersler.sumOf { it.dakika } }
    }

    class Sonuc(val ok: Boolean, val mesaj: String, val mufredat: Mufredat?)

    /**
     * Kurs adından tam müfredat üretir.
     *
     * @param kursAdi kullanıcının yazdığı ad ("SAP2000", "Çelik Yapılar"...)
     * @param seviye "temel" | "orta" | "ileri" | "hepsi"
     * @param hedefDers yaklaşık ders sayısı (20-60 arası mantıklı)
     */
    fun uret(
        context: Context,
        kursAdi: String,
        seviye: String = "hepsi",
        hedefDers: Int = 30
    ): Sonuc {
        if (!AiSettings.isOnlineMode(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_offline_mode), null)
        }
        if (!AiClient.isOnline(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_no_net), null)
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Sonuc(false, context.getString(R.string.ai_err_no_key), null)
        }

        // v7.24: bir sağlayıcı olmazsa anahtarı olan diğerine geç
        val saglayicilar = AiClient.saglayiciSirasi(context)
        val ilk = saglayicilar.firstOrNull() ?: AiClient.Provider.GEMINI
        var sonMesaj = ""

        for (saglayici in saglayicilar) {
            val key = AiSettings.getKeyFor(context, saglayici.id)
                .ifBlank { if (saglayici == ilk) AiSettings.getApiKey(context) else "" }
            if (key.isBlank()) continue

            try {
                val ham = if (saglayici == AiClient.Provider.GEMINI) {
                    geminiUret(context, key, kursAdi, seviye, hedefDers)
                } else {
                    digerUret(context, saglayici, key, kursAdi, seviye, hedefDers)
                }
                if (ham.first) {
                    val m = ayristir(ham.second, kursAdi)
                    if (m != null && m.bolumler.isNotEmpty()) {
                        val not = if (saglayici != ilk) {
                            context.getString(R.string.ai_switched, ilk.label, saglayici.label)
                        } else ""
                        // v7.43: arka planda bitince bildir (öneri 23)
                        try {
                            BildirimUretici.kursUretildi(
                                context, m.kursAdi, m.bolumler.sumOf { b -> b.dersler.size }
                            )
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Kurs bildirimi gönderilemedi", e)
                        }
                        return Sonuc(true, not, m)
                    }
                    sonMesaj = context.getString(R.string.gen_err_parse)
                } else {
                    sonMesaj = ham.second
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Müfredat üretilemedi (${saglayici.id})", e)
                sonMesaj = context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen")
            }

            if (!AiClient.saglayiciDegistirmeliMi(sonMesaj)) break
            android.util.Log.w(TAG, "${saglayici.label} olmadı, sıradaki deneniyor")
        }
        return Sonuc(false, sonMesaj.ifBlank { context.getString(R.string.gen_err_parse) }, null)
    }

    // ─────────────────── İstem ───────────────────

    private fun istemKur(kursAdi: String, seviye: String, hedefDers: Int): String {
        val seviyeMetni = when (seviye) {
            "temel" -> "Yalnızca temel ve başlangıç konuları."
            "orta" -> "Temeli bildiği varsayılarak orta seviye konular."
            "ileri" -> "İleri seviye ve uzmanlık konuları."
            else -> "Sıfırdan ileri seviyeye kadar tüm konular."
        }

        return """"$kursAdi" konusu için Türkçe bir eğitim müfredatı hazırla.

ÖNCE İNTERNETTE ARAŞTIR:
- Bu konuda gerçekte hangi eğitimler veriliyor
- Üniversite ders içerikleri neler
- Sertifika programları neleri kapsıyor
- Sektörde hangi beceriler aranıyor

Sonra bulduklarına dayanarak müfredat kur.

HEDEF KİTLE: İnşaat mühendisleri, mimarlar ve mühendislik öğrencileri.
SEVİYE: $seviyeMetni
DERS SAYISI: Yaklaşık $hedefDers ders.

KURALLAR:
1. Bölümler mantıklı sırayla: kolaydan zora, temelden uygulamaya
2. Her bölümde 4-8 ders olsun
3. Ders başlıkları SOMUT olsun — "Giriş" değil, "Malzeme tanımlama ve C25 betonu"
4. Süreler gerçekçi: basit konu 10-15 dk, karmaşık konu 20-30 dk, uygulama 35-45 dk
5. Son bölümler mutlaka GERÇEK PROJE UYGULAMASI içersin
6. Türkçe terim kullan, parantez içinde İngilizcesini ver: "Kiriş (Beam)"
7. Türkiye'deki yönetmelik ve standartlara değin (TS 500, TBDY 2018 vb.)

ÇIKTI — yalnızca bu JSON, başka hiçbir şey yazma:
{
  "kurs": "$kursAdi",
  "emoji": "tek uygun emoji",
  "aciklama": "Kursun ne öğrettiğini anlatan bir cümle",
  "bolumler": [
    {
      "baslik": "1. Bölüm adı",
      "dersler": [
        {"baslik": "Ders adı", "dakika": 20, "aciklama": "Bu derste ne öğrenilecek, tek cümle"}
      ]
    }
  ]
}"""
    }

    // ─────────────────── Gemini (grounding ile) ───────────────────

    /**
     * v7.23: Önce Google Arama aracıyla dener; araç reddedilirse (bazı
     * anahtarlarda/bölgelerde kapalı olabilir) araçsız tekrar dener.
     * Böylece "hiç çalışmıyor" durumu ortadan kalkar.
     */
    private fun geminiUret(
        context: Context,
        key: String,
        kursAdi: String,
        seviye: String,
        hedefDers: Int
    ): Pair<Boolean, String> {
        val ilk = geminiUretTek(context, key, kursAdi, seviye, hedefDers, aracKullan = true)
        if (ilk.first) return ilk
        android.util.Log.w(TAG, "Aramalı deneme başarısız, araçsız deneniyor: ${ilk.second.take(120)}")
        val ikinci = geminiUretTek(context, key, kursAdi, seviye, hedefDers, aracKullan = false)
        return if (ikinci.first) ikinci else ilk
    }

    private fun geminiUretTek(
        context: Context,
        key: String,
        kursAdi: String,
        seviye: String,
        hedefDers: Int,
        aracKullan: Boolean
    ): Pair<Boolean, String> {
        // v7.34: ücretsiz mod açıksa kredi harcayan model seçilmez
        val model = AiClient.guvenliModel(context, AiClient.Provider.GEMINI)
        if (model.isBlank()) {
            return false to context.getString(
                R.string.ai_err_no_free_model, AiClient.Provider.GEMINI.label
            )
        }
        val url = "${AiClient.Provider.GEMINI.endpoint}/$model:generateContent?key=$key"

        val govde = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("role", "user").put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", istemKur(kursAdi, seviye, hedefDers))
                        )
                    )
                )
            )
            // Gerçek araştırma yapsın — uydurma müfredat olmasın.
            // v7.23: camelCase "googleSearch" — snake_case yok sayılıyordu.
            .apply {
                if (aracKullan) {
                    put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
                }
            }
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.35)
                    // v7.23: düşünme token'ları için geniş bütçe + düşük düşünme
                    // seviyesi. Uzun müfredat çıktısı kesilmesin.
                    .put("maxOutputTokens", 32768)
                    .put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
            )
            .toString()

        return httpIstek(context, url, govde, null)
    }

    // ─────────────────── Diğer sağlayıcılar ───────────────────

    private fun digerUret(
        context: Context,
        saglayici: AiClient.Provider,
        key: String,
        kursAdi: String,
        seviye: String,
        hedefDers: Int
    ): Pair<Boolean, String> {
        val hedef = saglayici.endpoint.ifBlank { AiSettings.getCustomEndpoint(context) }
        val model = AiClient.guvenliModel(context, saglayici)
        if (model.isBlank()) {
            return false to context.getString(R.string.ai_err_no_free_model, saglayici.label)
        }

        val govde = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user")
                        .put("content", istemKur(kursAdi, seviye, hedefDers))
                )
            )
            .put("temperature", 0.35)
            .put("max_tokens", 8192)
            .toString()

        return httpIstek(context, hedef, govde, key)
    }

    /** Ortak HTTP çağrısı. authKey null ise Gemini (anahtar URL'de). */
    private fun httpIstek(
        context: Context,
        url: String,
        govde: String,
        authKey: String?
    ): Pair<Boolean, String> {
        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            // Müfredat üretimi uzun sürer — araştırma + uzun çıktı
            readTimeout = 150000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (authKey != null) setRequestProperty("Authorization", "Bearer $authKey")
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
            return false to context.getString(R.string.src_err_http, kod, detay.take(140))
        }

        // Gemini biçimi
        val kok = JSONObject(cevap)
        kok.optJSONArray("candidates")?.optJSONObject(0)?.let { aday ->
            val parcalar = aday.optJSONObject("content")?.optJSONArray("parts")
            val metin = buildString {
                if (parcalar != null) {
                    for (i in 0 until parcalar.length()) {
                        // v7.23: düşünme parçalarını atla, yalnızca gerçek çıktıyı al
                        val p = parcalar.optJSONObject(i) ?: continue
                        if (p.optBoolean("thought", false)) continue
                        append(p.optString("text").orEmpty())
                    }
                }
            }.trim()
            if (metin.isNotBlank()) return true to metin

            // v7.23: boş yanıt — sebebini açıkla
            val bitis = aday.optString("finishReason").orEmpty()
            if (bitis == "MAX_TOKENS") {
                return false to context.getString(R.string.gen_err_max_tokens)
            }
            if (bitis == "SAFETY") {
                return false to context.getString(R.string.ai_err_blocked, "SAFETY")
            }
        }
        kok.optJSONObject("promptFeedback")?.optString("blockReason")
            ?.takeIf { it.isNotBlank() }
            ?.let { return false to context.getString(R.string.ai_err_blocked, it) }
        // OpenAI biçimi
        kok.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return true to it }

        return false to context.getString(R.string.ai_err_empty)
    }

    // ─────────────────── Ayrıştırma ───────────────────

    fun ayristir(ham: String, varsayilanAd: String): Mufredat? {
        return try {
            var s = ham.trim()
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas < 0 || son <= bas || son >= s.length) return null
            val o = JSONObject(s.take(son + 1).drop(bas))

            val bolumDizi = o.optJSONArray("bolumler") ?: return null
            val bolumler = mutableListOf<UretilenBolum>()

            for (i in 0 until bolumDizi.length()) {
                val b = bolumDizi.optJSONObject(i) ?: continue
                val bBaslik = b.optString("baslik", "").trim()
                if (bBaslik.isBlank()) continue

                val dersDizi = b.optJSONArray("dersler") ?: continue
                val dersler = mutableListOf<UretilenDers>()
                for (j in 0 until dersDizi.length()) {
                    val d = dersDizi.optJSONObject(j) ?: continue
                    val dBaslik = d.optString("baslik", "").trim()
                    if (dBaslik.isBlank()) continue
                    dersler.add(
                        UretilenDers(
                            baslik = dBaslik,
                            // Saçma süreleri makul aralığa çek
                            dakika = d.optInt("dakika", 20).coerceIn(5, 90),
                            aciklama = d.optString("aciklama", "").trim()
                        )
                    )
                }
                if (dersler.isNotEmpty()) bolumler.add(UretilenBolum(bBaslik, dersler))
            }
            if (bolumler.isEmpty()) return null

            Mufredat(
                kursAdi = o.optString("kurs", "").trim().ifBlank { varsayilanAd },
                emoji = o.optString("emoji", "").trim().take(3).ifBlank { "📘" },
                aciklama = o.optString("aciklama", "").trim(),
                bolumler = bolumler
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Müfredat ayrıştırılamadı", e)
            null
        }
    }

    // ─────────────────── Kaydetme ───────────────────

    /**
     * Üretilen müfredatı gerçek kurs olarak kaydeder.
     * @return oluşturulan kurs
     */
    fun kaydet(context: Context, m: Mufredat): Store.Course {
        val renk = Store.loadCourses(context).size % 7
        val kurs = Store.addCourse(context, m.kursAdi, m.emoji, renk, m.aciklama)
        m.bolumler.forEach { b ->
            val bolum = Store.addSection(context, kurs.id, b.baslik)
            b.dersler.forEach { d ->
                Store.addLesson(context, kurs.id, bolum.id, d.baslik, d.dakika, d.aciklama)
            }
        }
        return kurs
    }

    /**
     * v7.21: Var olan bir kursa eksik bölüm/ders ekler (genişletme).
     * Mevcut ders başlıklarını modele bildirip tekrar üretmesini engeller.
     */
    fun genislet(
        context: Context,
        kurs: Store.Course,
        ekDers: Int = 12
    ): Sonuc {
        val mevcut = Store.loadLessons(context)
            .filter { it.courseId == kurs.id }
            .joinToString("; ") { it.title }
            .take(2500)

        val ek = if (mevcut.isBlank()) "" else
            "\n\nBU DERSLER ZATEN VAR, TEKRARLAMA:\n$mevcut\n\n" +
                "Sadece EKSİK kalan, henüz işlenmemiş konular için yeni bölümler üret."

        val kursAdi = kurs.title + ek
        return uret(context, kursAdi, "hepsi", ekDers)
    }
}
