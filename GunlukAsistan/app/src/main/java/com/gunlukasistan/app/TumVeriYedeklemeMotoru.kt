package com.gunlukasistan.app

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32

/**
 * v11.12 — Evrensel Veri Yedekleme & Geri Yükleme Motoru (saf, JVM testli).
 *
 * Android'in `SharedPreferences` içeriklerini (tüm ayarlar, görev/veri
 * anahtarları, Canva modül durumu, tema tercihleri vs.) tek bir taşınabilir
 * JSON yedeğine çevirir ve bu yedekten birebir geri yükler.
 *
 * ── Neden gerekli ──
 * Kullanıcı sık sık "hiçbir veri kaybolmasın" diye tam kopya talep ediyor.
 * Bu motor, uygulama içindeki tüm anahtar/değer çiftlerini cihaz değiştirmede
 * veya yeni sohbet/cihaza devirde kayıpsız taşımayı mümkün kılar.
 *
 * ── Tasarım ──
 * · Android Context BAĞIMSIZ: prefs içeriği `Map<String, Map<String, Any?>>`
 *   olarak verilir; motor saf JVM'de test edilir.
 * · Tip koruması: her değer bir tip etiketiyle kaydedilir; geri yüklemede
 *   aynı tip birebir korunur (String/Int/Long/Float/Boolean/Set).
 * · Bütünlük: tüm girdilerin sıralı kanonik temsili üzerinden CRC32 sağlama
 *   değeri hesaplanır. Bozulmuş / değiştirilmiş yedekler geri yüklenmez.
 * · Sürüm + zaman damgası metadata olarak tutulur.
 */
object TumVeriYedeklemeMotoru {

    const val ANAHTAR = "ga_yedek_v1"
    const val SURUM = 1

    // Tip etiketleri (kısa — dosya boyutunu küçültür).
    private const val T_STRING = "S"
    private const val T_INT = "I"
    private const val T_LONG = "L"
    private const val T_FLOAT = "F"
    private const val T_BOOL = "B"
    private const val T_SET = "ST"

    /** Yedekten okunan özet bilgi (ekran gösterimi için). */
    data class Meta(
        val uygulamaSurum: String,
        val tarihIso: String,
        val dosyaSayisi: Int,
        val kalanSayisi: Int,
        val saglama: String,
        val bayt: Int,
    )

    /**
     * Tüm prefs içeriğini taşınabilir JSON yedeğine çevirir.
     * @param prefsVeri  dosya adı → (anahtar → değer) haritası
     * @param uygulamaSurum  sürüm notuna yazılacak uygulama sürümü (ör. "11.12")
     */
    fun yedekOlustur(prefsVeri: Map<String, Map<String, Any?>>, uygulamaSurum: String): String {
        val kok = JSONObject()
        kok.put("anahtar", ANAHTAR)
        kok.put("surum", SURUM)
        kok.put("uygulamaSurum", uygulamaSurum)
        kok.put("tarihIso", tarihIso())

        val dosyalar = JSONArray()
        var toplamKalan = 0
        for ((adi, kalan) in prefsVeri.entries.sortedBy { it.key }) {
            val dosya = JSONObject()
            dosya.put("adi", adi)
            val icerik = JSONObject()
            for ((k, v) in kalan.entries.sortedBy { it.key }) {
                icerik.put(k, degerNesnesi(v))
            }
            dosya.put("d", icerik)
            dosyalar.put(dosya)
            toplamKalan += kalan.size
        }
        kok.put("dosyalar", dosyalar)
        kok.put("saglama", saglamaHesapla(dosyalar))
        return kok.toString()
    }

    /** Bir değeri {t, v} tip-etiketli nesneye çevirir. */
    private fun degerNesnesi(v: Any?): JSONObject {
        val o = JSONObject()
        when (v) {
            is String -> { o.put("t", T_STRING); o.put("v", v) }
            is Int -> { o.put("t", T_INT); o.put("v", v) }
            is Long -> { o.put("t", T_LONG); o.put("v", v) }
            is Float -> { o.put("t", T_FLOAT); o.put("v", v) }
            is Boolean -> { o.put("t", T_BOOL); o.put("v", v) }
            is Set<*> -> {
                o.put("t", T_SET)
                val a = JSONArray()
                for (s in v) a.put(s?.toString() ?: "")
                o.put("v", a)
            }
            else -> { o.put("t", T_STRING); o.put("v", v?.toString() ?: "") }
        }
        return o
    }

    /** Yedek JSON'unun geçerli ve sağlama-değeri doğru olup olmadığını söyler. */
    fun yedekDogrula(json: String): Boolean {
        return try {
            val kok = JSONObject(json)
            val anahtarUygun = kok.optString("anahtar") == ANAHTAR && kok.optInt("surum") == SURUM
            val dosyalar = kok.optJSONArray("dosyalar")
            if (!anahtarUygun || dosyalar == null) false
            else kok.optString("saglama") == saglamaHesapla(dosyalar)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Yedek JSON'u çözer ve tip korunmuş prefs haritasına geri çevirir.
     * @throws IllegalArgumentException  sağlama veya biçim uyuşmazsa
     */
    fun geriYukle(json: String): Map<String, Map<String, Any?>> {
        val kok = JSONObject(json)
        if (kok.optString("anahtar") != ANAHTAR) {
            throw IllegalArgumentException("Geçersiz yedek: tanınmayan biçim")
        }
        if (kok.optInt("surum") != SURUM) {
            throw IllegalArgumentException("Desteklenmeyen yedek sürümü")
        }
        val dosyalar = kok.optJSONArray("dosyalar") ?: JSONArray()
        val beklenen = kok.optString("saglama")
        if (beklenen.isNotBlank() && beklenen != saglamaHesapla(dosyalar)) {
            throw IllegalArgumentException("Sağlama değeri eşleşmiyor: veri bozulmuş olabilir")
        }
        val sonuc = mutableMapOf<String, Map<String, Any?>>()
        for (i in 0 until dosyalar.length()) {
            val dosya = dosyalar.getJSONObject(i)
            val adi = dosya.optString("adi")
            val icerik = dosya.optJSONObject("d") ?: JSONObject()
            val harita = mutableMapOf<String, Any?>()
            for (k in anahtarListesi(icerik)) {
                val e = icerik.optJSONObject(k) ?: continue
                harita[k] = degerCoz(e)
            }
            if (adi.isNotBlank()) sonuc[adi] = harita
        }
        return sonuc
    }

    /** Yedek JSON'undan özet bilgi çıkarır (ekran için). Geçersizse null. */
    fun metaBilgi(json: String): Meta? {
        return try {
            if (!yedekDogrula(json)) return null
            val kok = JSONObject(json)
            val dosyalar = kok.getJSONArray("dosyalar")
            var kalan = 0
            for (i in 0 until dosyalar.length()) {
                kalan += (dosyalar.getJSONObject(i).optJSONObject("d")?.length() ?: 0)
            }
            Meta(
                uygulamaSurum = kok.optString("uygulamaSurum", "?"),
                tarihIso = kok.optString("tarihIso", "?"),
                dosyaSayisi = dosyalar.length(),
                kalanSayisi = kalan,
                saglama = kok.optString("saglama", ""),
                bayt = json.length,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun degerCoz(e: JSONObject): Any? {
        return when (e.optString("t")) {
            T_STRING -> e.optString("v")
            T_INT -> e.optInt("v")
            T_LONG -> e.optLong("v")
            T_FLOAT -> e.optDouble("v").toFloat()
            T_BOOL -> e.optBoolean("v")
            T_SET -> {
                val a = e.optJSONArray("v")
                val set = linkedSetOf<String>()
                if (a != null) for (j in 0 until a.length()) set.add(a.optString(j))
                set
            }
            else -> e.optString("v")
        }
    }

    /** CRC32 sağlama: tüm girdilerin sıralı kanonik temsili üzerinden. */
    fun saglamaHesapla(dosyalar: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until dosyalar.length()) {
            val dosya = dosyalar.getJSONObject(i)
            sb.append(dosya.optString("adi")).append('|')
            val icerik = dosya.optJSONObject("d")
            if (icerik != null) {
                for (k in anahtarListesi(icerik)) {
                    val e = icerik.optJSONObject(k) ?: continue
                    sb.append(k).append('=').append(e.optString("t"))
                        .append(':').append(e.optString("v")).append(';')
                }
            }
        }
        val crc = CRC32()
        crc.update(sb.toString().toByteArray(Charsets.UTF_8))
        return java.lang.Long.toHexString(crc.value).padStart(8, '0')
    }

    /** JSON nesnesinin anahtarlarını sıralı döndürür (deterministik sağlama için). */
    private fun anahtarListesi(obj: JSONObject): List<String> {
        val names = obj.names() ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until names.length()) list.add(names.optString(i))
        return list.sorted()
    }

    fun tarihIso(now: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(now)
}
