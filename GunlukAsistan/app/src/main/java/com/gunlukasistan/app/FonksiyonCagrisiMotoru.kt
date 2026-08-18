package com.gunlukasistan.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * v11.13 — Native function-calling adaptörü (SAF, JVM testli).
 *
 * "Yarım kalan" öneri #1: uygulama şu ana kadar `>>KOMUT:` satırlarını
 * ayrıştırıyordu. Bu adaptör, Gemini/OpenAI'nin native **function-calling**
 * çıktısını (yapılandırılmış `functionCall` JSON'u) uygulamanın komut
 * sistemine çevirir. Böylece ayrıştırma hatası en aza iner.
 *
 *  · [fonksiyonTanimi] — tek bir komutun modelin görebileceği fonksiyon şeması.
 *  · [fonksiyonListesi] — tüm komutların şemaları.
 *  · [cevabiCoz] — modelin `functionCall` JSON dizisini [AsistanKomut.Komut] listesine çevirir.
 */
object FonksiyonCagrisiMotoru {

    /** Bir komut için modelin görebileceği OpenAPI-stili fonksiyon şeması. */
    fun fonksiyonTanimi(komutAd: String, aciklama: String, parametre: String): JSONObject =
        JSONObject()
            .put("name", "uygulama_" + komutAd.replace('_', '_'))
            .put("description", aciklama)
            .put(
                "parameters",
                JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject().put(
                            "deger",
                            JSONObject()
                                .put("type", "string")
                                .put("description", parametre)
                        )
                    )
                    .put("required", JSONArray().put("deger"))
            )

    /** Yaygın komutların fonksiyon şema listesi (prompt'a / isteğe eklenir). */
    fun fonksiyonListesi(): List<JSONObject> = listOf(
        fonksiyonTanimi("gorev_ekle", "Yeni bir görev ekler.", "Görev metni (isteğe bağlı tarih '::' ile)"),
        fonksiyonTanimi("gorev_tamamla", "Bir görevi tamamlar.", "Görev adı"),
        fonksiyonTanimi("gorev_sil", "Bir görevi siler.", "Görev adı"),
        fonksiyonTanimi("not_ekle", "Yeni bir not ekler.", "Not başlığı '::' içerik"),
        fonksiyonTanimi("konu_ekle", "Yeni bir konu ekler.", "Konu başlığı '::' maddeler"),
        fonksiyonTanimi("aliskanlik_ekle", "Yeni bir alışkanlık ekler.", "Alışkanlık adı"),
        fonksiyonTanimi("ekran_ac", "Bir uygulama ekranını açar.", "Ekran adı (kurslar/gorevler/ayarlar…)"),
        fonksiyonTanimi("ayar_ses", "Ses bildirimlerini ayarlar.", "acik veya kapanik"),
        fonksiyonTanimi("ayar_gece", "Temayı ayarlar.", "koyu/acik/sistem"),
        fonksiyonTanimi("zamanlayici", "Zamanlayıcı başlatır.", "Dakika"),
        fonksiyonTanimi("ozet_ver", "Veri durumunu özetler.", ""),
        fonksiyonTanimi("yedek_al", "Yedek alır.", "")
    )

    /**
     * Modelin `functionCall` JSON dizisini komut listesine çevirir.
     * Desteklenen iki biçim:
     *   1. Gemini/OpenAI stili: `[{"name":"uygulama_gorev_ekle","args":{"deger":"..."}}]`
     *   2. Basit `[{"name":"gorev_ekle","deger":"..."}]`
     * Geçersiz girdi → boş liste.
     */
    fun cevabiCoz(ham: String): List<AsistanKomut.Komut> {
        if (ham.isBlank()) return emptyList()
        val dizi = try { JSONArray(ham) } catch (_: Exception) { return emptyList() }
        val sonuc = mutableListOf<AsistanKomut.Komut>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            var ad = o.optString("name").trim()
            // "uygulama_gorev_ekle" → "gorev_ekle"
            ad = ad.removePrefix("uygulama_").trim()
            if (ad.isBlank()) continue
            val deger = when {
                o.has("args") && o.optJSONObject("args") != null ->
                    o.optJSONObject("args").optString("deger", "")
                o.has("arguments") && o.optJSONObject("arguments") != null ->
                    o.optJSONObject("arguments").optString("deger", "")
                else -> o.optString("deger", "")
            }
            sonuc.add(AsistanKomut.Komut(ad, deger.trim()))
        }
        return sonuc
    }
}
