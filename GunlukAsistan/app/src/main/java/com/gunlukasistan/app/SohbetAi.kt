package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject

/**
 * v7.54 — Ortak sohbette yapay zekâ.
 *
 * ── Kullanıcı isteği ──
 * "arkadasinla sohbet edip yapay zekayla entregreli olsun yapay zeka ile
 *  konusma esnasinda tiklayip ekleme yapabilme ozelligi ekle"
 *
 * ── Nasıl çalışır ──
 * Sohbette `@ai` ile başlayan mesaj yapay zekâya gider. AI hem cevap verir
 * hem de eklenebilir öğeler önerir. Öneriler mesajın altında düğme olarak
 * çıkar; dokununca ilgili bölüme (görev/not/konu/alışkanlık) eklenir.
 *
 * ── Neden sadeIstek? ──
 * `chat()` sistem istemine 40 komut talimatı ve kurs verilerini ekliyor;
 * sohbet bağlamında bu hem gereksiz hem de modeli şaşırtıyor (v7.50'de
 * film önerilerinde aynı sorun yaşandı). Burada temiz istem kullanılıyor.
 */
object SohbetAi {

    private const val TAG = "SohbetAi"

    /** AI'ya soru sormak için mesaj öneki. */
    const val TETIK = "@ai"

    /** Bir mesaj yapay zekâya mı yönelik? */
    fun aiyaMi(metin: String): Boolean =
        metin.trim().lowercase().startsWith(TETIK)

    /** Tetikleyiciyi ayıklayıp saf soruyu döndürür. */
    fun soruyuAyikla(metin: String): String =
        metin.trim().removePrefix(TETIK).removePrefix("@AI").trim()

    class Cevap(
        val ok: Boolean,
        val metin: String,
        /** "tur|metin" biçiminde eklenebilir öneriler. */
        val oneriler: List<String> = emptyList()
    )

    /**
     * Sohbet bağlamıyla birlikte AI'ya sorar.
     *
     * @param sonMesajlar son birkaç mesaj — AI konuşmanın akışını görsün
     * @param uyeler odadaki kişiler — "Ahmet'e görev ver" gibi istekler için
     */
    fun sor(
        context: Context,
        soru: String,
        sonMesajlar: List<OnlineStore.Mesaj>,
        uyeler: List<String>
    ): Cevap {
        if (soru.isBlank()) {
            return Cevap(false, context.getString(R.string.sa_bos_soru))
        }

        val gecmis = if (sonMesajlar.isEmpty()) "" else buildString {
            append("\nSON KONUŞMA:\n")
            sonMesajlar.takeLast(6).forEach { m ->
                append(if (m.aiMi) "Asistan" else m.kim).append(": ")
                append(m.metin.take(160)).append("\n")
            }
        }

        val kisiler = if (uyeler.isEmpty()) "" else
            "\nOdadaki kişiler: " + uyeler.joinToString(", ")

        val istem = """Sen iki kişilik ortak bir çalışma/aile uygulamasında sohbete katılan
yardımcı bir asistansın. Kısa, samimi ve işe yarar cevap ver.$kisiler$gecmis

SORU: $soru

Yanıtını SADECE şu JSON biçiminde ver:
{"cevap":"kısa cevabın (en fazla 5 cümle)","oneriler":[{"tur":"gorev","metin":"eklenebilir madde"}]}

KURALLAR:
1. "cevap" Türkçe, sohbet diliyle, kısa olsun. Madde madde yazma, düz konuş.
2. "oneriler" listeye eklenebilecek somut maddeler. En fazla 4 tane.
3. "tur" şunlardan biri: "gorev" (yapılacak iş), "not" (bilgi/hatırlatma),
   "konu" (çok adımlı iş), "alis" (tekrarlanan alışkanlık).
4. Öneri uygun değilse "oneriler" boş dizi olsun — zorlama.
5. Kullanıcı sadece sohbet ediyorsa öneri verme, sadece cevapla.
6. JSON dışında hiçbir şey yazma. Kod bloğu işareti kullanma."""

        val sonuc = AiClient.sadeIstek(context, istem, 2048)
        if (!sonuc.ok) return Cevap(false, sonuc.text)

        return ayristir(sonuc.text)
    }

    /**
     * Savunmacı ayrıştırma.
     * JSON bozuksa ham metni cevap olarak gösterir — kullanıcı boş ekran görmez.
     */
    private fun ayristir(ham: String): Cevap {
        try {
            val (komutsuz, _) = AsistanKomut.ayikla(ham)
            var s = komutsuz.trim().ifBlank { ham.trim() }
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas >= 0 && son > bas) {
                val o = JSONObject(s.substring(bas, son + 1))
                val cevap = o.optString("cevap").trim()
                if (cevap.isNotBlank()) {
                    val oneriler = mutableListOf<String>()
                    o.optJSONArray("oneriler")?.let { d ->
                        for (i in 0 until minOf(d.length(), 4)) {
                            val n = d.optJSONObject(i) ?: continue
                            val tur = n.optString("tur").trim().lowercase()
                            val metin = n.optString("metin").trim()
                            if (metin.isBlank()) continue
                            val gecerliTur = when {
                                tur.startsWith("gor") -> "gorev"
                                tur.startsWith("not") -> "not"
                                tur.startsWith("kon") -> "konu"
                                tur.startsWith("al") -> "alis"
                                else -> "gorev"
                            }
                            oneriler.add(gecerliTur + "|" + metin.take(100))
                        }
                    }
                    return Cevap(true, cevap, oneriler)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "AI yanıtı ayrıştırılamadı", e)
        }
        // JSON gelmediyse ham metni göster — hiç cevap vermemekten iyidir
        val duz = ham.trim().take(600)
        return if (duz.isBlank()) Cevap(false, "") else Cevap(true, duz)
    }

    /** Öneri satırını (tür, metin) çiftine ayırır. */
    fun oneriCoz(satir: String): Pair<String, String> {
        val i = satir.indexOf('|')
        return if (i <= 0) "gorev" to satir
        else satir.substring(0, i) to satir.substring(i + 1)
    }

    /** Öneri türünün ekrandaki etiketi. */
    fun turEtiketi(context: Context, tur: String): String = context.getString(
        when (tur) {
            "not" -> R.string.sa_tur_not
            "konu" -> R.string.sa_tur_konu
            "alis" -> R.string.sa_tur_alis
            else -> R.string.sa_tur_gorev
        }
    )

    /** Öneri türünün simgesi. */
    fun turSimgesi(tur: String): String = when (tur) {
        "not" -> "📝"
        "konu" -> "📚"
        "alis" -> "🔥"
        else -> "✓"
    }
}
