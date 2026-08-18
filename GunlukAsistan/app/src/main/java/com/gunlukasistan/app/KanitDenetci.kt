package com.gunlukasistan.app

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * v7.78 — Kanıt fotoğrafını yapay zekâya denetleten katman.
 *
 * ── Görev ──
 * "Bu fotoğraf '{görev}' işinin yapıldığını gerçekten gösteriyor mu?"
 * sorusunu modele sorar ve **kararı ayrıştırır**.
 *
 * ── Neden [AiClient.konuOku] kullanılmadı ──
 * O fonksiyon el yazısı okumaya (`OKUMA_TALIMATI`) sabitlenmiş; başlık +
 * madde listesi döndürür. Burada gereken tamamen farklı: bir yargı
 * (onay/red) + güven + gerekçe. Bu yüzden kendi istemi ve kendi
 * ayrıştırıcısı var; görsel isteği için AiClient'ın alt yapısı
 * ([AiClient.gorselDenetim]) yeniden kullanılıyor.
 *
 * ── Kararsızlık nasıl çözülür ──
 * Model "emin değilim" derse [Kanit.katilik] belirler:
 *   · GEVŞEK → onayla (kullanıcıyı boşuna uğraştırma)
 *   · NORMAL → güven ≥ 50 ise onayla
 *   · SERT   → reddet (kullanıcı "beni zorla" dedi)
 */
object KanitDenetci {

    private const val TAG = "KanitDenetci"

    /**
     * Denetim sonucu.
     *
     * @param calisti istek teknik olarak başarılı mı (ağ/anahtar sorunu yoksa)
     * @param onay model kanıtı kabul etti mi
     * @param guven 0-100
     * @param gerekce kullanıcıya gösterilecek kısa açıklama
     * @param ipucu red durumunda "şunu çek" önerisi
     */
    class Sonuc(
        val calisti: Boolean,
        val onay: Boolean,
        val guven: Int = 0,
        val gerekce: String = "",
        val ipucu: String = "",
        val hata: String = ""
    )

    /**
     * Fotoğrafı denetler. **Ağ isteği yapar — arka planda çağır.**
     *
     * @param gorevMetni görevin başlığı, modele bağlam olarak verilir
     * @param kullaniciNotu kullanıcının eklediği açıklama (boş olabilir)
     */
    fun denetle(
        context: Context,
        uri: Uri,
        gorevMetni: String,
        kullaniciNotu: String = ""
    ): Sonuc {
        if (!AiSettings.isReady(context)) {
            return Sonuc(
                false, false,
                hata = context.getString(R.string.kn_ai_hazir_degil)
            )
        }

        val base64 = GorselHazirla.base64Uret(context, uri, netlestir = false)
            ?: return Sonuc(
                false, false,
                hata = context.getString(R.string.kn_foto_okunamadi)
            )

        val istem = istemKur(context, gorevMetni, kullaniciNotu)

        return try {
            val cevap = AiClient.gorselDenetim(context, base64, istem)
            if (!cevap.ok) return Sonuc(false, false, hata = cevap.text)
            ayristir(context, cevap.text)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Denetim başarısız", e)
            Sonuc(
                false, false,
                hata = context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen")
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // İSTEM
    // ═══════════════════════════════════════════════════════════════

    private fun istemKur(context: Context, gorev: String, not: String): String {
        val katilik = Kanit.katilik(context)

        val tonTalimati = when (katilik) {
            Kanit.KATI_GEVSEK ->
                "Ölçüt GEVŞEK: Fotoğraf görevle makul biçimde ilişkiliyse kabul et. " +
                    "Küçük şüphelerde kabul yönünde karar ver."
            Kanit.KATI_SERT ->
                "Ölçüt SERT: Kullanıcı seni bilerek katı olman için ayarladı. " +
                    "Fotoğraf işin BİTTİĞİNİ açıkça göstermiyorsa reddet. " +
                    "Alakasız, bulanık, eski ya da ekran görüntüsü olan kareleri reddet. " +
                    "Şüphede kalırsan REDDET."
            else ->
                "Ölçüt NORMAL: Fotoğraf işin yapıldığına dair makul kanıt sunuyorsa kabul et. " +
                    "Tamamen alakasız veya okunamayan kareleri reddet."
        }

        val notSatiri = if (not.isBlank()) "" else "\nKULLANICININ AÇIKLAMASI: $not"

        return """
Sen bir görev denetçisisin. Kullanıcı bir işi bitirdiğini iddia ediyor ve kanıt olarak bu fotoğrafı sundu.

GÖREV: "$gorev"$notSatiri

$tonTalimati

Şunlara dikkat et:
- Fotoğrafta görünen şey görev metniyle ilgili mi?
- İşin TAMAMLANDIĞINA dair belirti var mı (bitmiş ürün, temizlenmiş alan, dolu sayfa, kapalı defter vb.)?
- Fotoğraf kasten yanıltıcı mı (rastgele duvar, tavan, karanlık kare, alakasız ekran)?

SADECE şu JSON'u döndür, başka hiçbir şey yazma:
{"onay": true veya false, "guven": 0-100 arası sayı, "gerekce": "en fazla 15 kelime, Türkçe, kullanıcıya hitaben", "ipucu": "red ise ne çekmeli, en fazla 12 kelime, onay ise boş"}
        """.trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // AYRIŞTIRMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Modelin cevabından kararı çıkarır.
     *
     * Savunmacı: model ```json bloğu, açıklama ya da bozuk JSON
     * döndürebilir. Hiçbiri tutmazsa metinden anahtar kelime aranır;
     * o da olmazsa katılığa göre varsayılan karar verilir.
     */
    private fun ayristir(context: Context, ham: String): Sonuc {
        val json = jsonAyikla(ham)
        if (json != null) {
            val onay = json.optBoolean("onay", false)
            val guven = json.optInt("guven", if (onay) 70 else 30).coerceIn(0, 100)
            val gerekce = json.optString("gerekce").trim()
            val ipucu = json.optString("ipucu").trim()
            return Sonuc(
                calisti = true,
                onay = kararUygula(context, onay, guven),
                guven = guven,
                gerekce = gerekce.ifBlank {
                    context.getString(if (onay) R.string.kn_ger_onay else R.string.kn_ger_red)
                },
                ipucu = ipucu
            )
        }

        // JSON yok — metinden anlamaya çalış
        val metin = ham.lowercase()
        val olumlu = metin.contains("\"onay\": true") || metin.contains("onay: true") ||
            metin.contains("kabul") || metin.contains("onayla")
        val olumsuz = metin.contains("\"onay\": false") || metin.contains("onay: false") ||
            metin.contains("reddet") || metin.contains("alakasız")

        return when {
            olumlu && !olumsuz -> Sonuc(
                true, kararUygula(context, true, 60), 60,
                context.getString(R.string.kn_ger_onay)
            )
            olumsuz -> Sonuc(
                true, kararUygula(context, false, 40), 40,
                context.getString(R.string.kn_ger_red)
            )
            // Hiçbir şey anlaşılmadı: katılığa göre varsayılan
            else -> Sonuc(
                true,
                onay = Kanit.katilik(context) != Kanit.KATI_SERT,
                guven = 0,
                gerekce = context.getString(R.string.kn_ger_belirsiz)
            )
        }
    }

    /**
     * Modelin kararını katılık ayarıyla harmanlar.
     *
     * Model "onay" dese bile güven düşükse SERT modda reddedilir;
     * "red" dese bile GEVŞEK modda güven düşükse (yani model de
     * emin değilse) kullanıcı lehine çevrilir.
     */
    private fun kararUygula(context: Context, modelOnayi: Boolean, guven: Int): Boolean =
        when (Kanit.katilik(context)) {
            Kanit.KATI_GEVSEK -> modelOnayi || guven < 45
            Kanit.KATI_SERT -> modelOnayi && guven >= 60
            else -> if (guven in 1..34) modelOnayi && guven >= 25 else modelOnayi
        }

    /** Metnin içinden ilk geçerli JSON nesnesini çeker. */
    private fun jsonAyikla(ham: String): JSONObject? {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        runCatching { return JSONObject(temiz) }

        val bas = temiz.indexOf('{')
        val son = temiz.lastIndexOf('}')
        if (bas in 0 until son) {
            runCatching { return JSONObject(temiz.substring(bas, son + 1)) }
        }
        return null
    }
}
