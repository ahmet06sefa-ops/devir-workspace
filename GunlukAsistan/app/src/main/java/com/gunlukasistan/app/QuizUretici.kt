package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.29 — Ders içeriğinden quiz sorusu üretir.
 *
 * Tasarım kararı: sorular **bir kez** üretilip saklanır. Her quiz açılışında
 * yeniden üretmek hem kotayı harcar hem de her seferinde farklı soru gelirse
 * aralıklı tekrar anlamını yitirir. Aynı sorular tekrar sorulur ki
 * öğrenip öğrenmediğin ölçülebilsin.
 *
 * Soru kalitesi için istem katı kurallara bağlandı:
 *   · Ezber değil, anlama ölçen sorular
 *   · Şıklar birbirine yakın olmalı (kolay elenmemeli)
 *   · Doğru şık rastgele konumda
 *   · Ders içeriğinde geçmeyen bilgi sorulmaz
 */
object QuizUretici {

    private const val TAG = "QuizUretici"

    class Sonuc(val ok: Boolean, val mesaj: String, val sorular: List<QuizStore.Soru>)

    /**
     * Ders için soru üretir ve kaydeder.
     * @param adet kaç soru üretilsin (3-10 arası mantıklı)
     */
    fun uret(
        context: Context,
        ders: Store.Lesson,
        kursAdi: String,
        bolumAdi: String,
        adet: Int = 5
    ): Sonuc {
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Sonuc(false, context.getString(R.string.ai_err_no_key), emptyList())
        }
        if (!AiClient.isOnline(context)) {
            return Sonuc(false, context.getString(R.string.ai_err_no_net), emptyList())
        }

        val istem = istemKur(ders, kursAdi, bolumAdi, adet)
        val cevap = AiClient.chat(context, istem)
        if (!cevap.ok) return Sonuc(false, cevap.text, emptyList())

        val sorular = ayristir(cevap.text, ders.id)
        if (sorular.isEmpty()) {
            return Sonuc(false, context.getString(R.string.quiz_err_parse), emptyList())
        }
        // v8.0: havuza ekle (öneri 8) — eski sorular korunur
        QuizStore.havuzaEkle(context, ders.id, sorular)
        return Sonuc(true, "", sorular)
    }

    private fun istemKur(
        ders: Store.Lesson,
        kursAdi: String,
        bolumAdi: String,
        adet: Int
    ): String {
        val aciklama = if (ders.desc.isBlank()) "" else "\nDers açıklaması: ${ders.desc}"
        val not = if (ders.note.isBlank()) "" else "\nKullanıcının notu: ${ders.note}"

        return """"$kursAdi" kursu, "$bolumAdi" bölümü, "${ders.title}" dersi için
$adet adet çoktan seçmeli sınav sorusu hazırla.$aciklama$not

HEDEF KİTLE: İnşaat mühendisleri, mimarlar ve mühendislik öğrencileri.

SORU KURALLARI:
1. Ezber değil ANLAMA ölç. "Şu komutun kısayolu nedir" yerine
   "Şu durumda hangi komutu kullanırsın" tarzı sor.
2. Dört şık olsun. Yanlış şıklar da MANTIKLI görünsün — saçma şık koyma,
   kolayca elenmesin.
3. Doğru şıkkın yeri rastgele olsun (hep A veya hep B olmasın).
4. Sorular bu dersin konusuyla SINIRLI kalsın, başka derse kayma.
5. Türkçe terim kullan, gerekiyorsa parantezde İngilizcesi.
6. Her soruya 1 cümlelik açıklama ekle — neden o cevap doğru.
7. Sayısal sorularda gerçekçi mühendislik değerleri kullan.

ÇIKTI — yalnızca bu JSON, başka hiçbir şey yazma:
{"sorular":[
  {"soru":"Soru metni",
   "siklar":["A şıkkı","B şıkkı","C şıkkı","D şıkkı"],
   "dogru":2,
   "aciklama":"Neden bu cevap doğru, tek cümle"}
]}

"dogru" alanı 0-3 arası dizindir (0=ilk şık)."""
    }

    /**
     * Modelin JSON çıktısını Soru listesine çevirir.
     * Bozuk/eksik sorular sessizce atlanır — uygulama çökmez.
     */
    fun ayristir(ham: String, lessonId: Long): List<QuizStore.Soru> {
        val cikti = mutableListOf<QuizStore.Soru>()
        try {
            var s = ham.trim()
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas < 0 || son <= bas || son >= s.length) return cikti

            val kok = JSONObject(s.take(son + 1).drop(bas))
            val dizi = kok.optJSONArray("sorular") ?: return cikti

            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val metin = o.optString("soru", "").trim()
                val sd = o.optJSONArray("siklar") ?: continue
                val siklar = (0 until sd.length())
                    .map { sd.optString(it, "").trim() }
                    .filter { it.isNotBlank() }
                val dogru = o.optInt("dogru", -1)

                // Geçersizleri ele: en az 2 şık, doğru dizin aralıkta olmalı
                if (metin.isBlank() || siklar.size < 2) continue
                if (dogru !in siklar.indices) continue

                cikti.add(
                    QuizStore.Soru(
                        id = System.currentTimeMillis() + i,
                        lessonId = lessonId,
                        metin = metin,
                        siklar = siklar,
                        dogru = dogru,
                        aciklama = o.optString("aciklama", "").trim()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sorular ayrıştırılamadı", e)
        }
        return cikti
    }
}
