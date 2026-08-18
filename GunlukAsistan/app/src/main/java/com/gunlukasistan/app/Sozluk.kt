package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.84 — Terim sözlüğü.
 *
 * ── Neden gerekli ──
 * Anlatım okurken bilinmeyen bir terime rastlayınca kullanıcı ya
 * uygulamadan çıkıp arıyor ya da geçiştiriyor. İkisi de öğrenmeyi
 * bozuyor. Artık kelimeyi seçip "Ne demek?" diyebiliyor; açıklama
 * anında geliyor ve **sözlüğe kaydediliyor**.
 *
 * ── Neden ayrı depo ──
 * Terimler [KonuUretici] anlatım önbelleğinden bağımsız yaşamalı:
 * anlatım "yeniden üret" ile silinse bile öğrenilen terimler kalmalı.
 * Ayrıca aynı terim birden çok konuda geçebilir — tek yerde tutulup
 * her yerden okunuyor.
 *
 * ── Önbellek mantığı ──
 * Bir terim bir kez açıklandıktan sonra AI'ya tekrar sorulmuyor.
 * Aynı terime ikinci kez dokunmak anında cevap veriyor ve kota harcamıyor.
 */
object Sozluk {

    private const val TAG = "Sozluk"
    private const val PREF = "sozluk_v1"
    private const val K_TERIMLER = "terimler_json"

    /** Sözlükte en fazla kaç terim tutulur. */
    private const val TAVAN = 500

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir terim kaydı.
     *
     * @param kisa tek cümlelik tanım — listede ve baloncukta görünür
     * @param uzun detaylı açıklama + örnek
     * @param baglam terimin ilk görüldüğü konu
     * @param bakildi kaç kez açıldı — sık bakılan terim zayıf nokta demek
     */
    data class Terim(
        val terim: String,
        var kisa: String,
        var uzun: String = "",
        var baglam: String = "",
        var eklendi: Long = 0L,
        var bakildi: Int = 1,
        var yildiz: Boolean = false
    )

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    fun hepsi(context: Context): MutableList<Terim> {
        val ham = prefs(context).getString(K_TERIMLER, "[]") ?: "[]"
        val liste = mutableListOf<Terim>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val t = o.optString("t").trim()
                if (t.isBlank()) continue
                liste.add(
                    Terim(
                        terim = t,
                        kisa = o.optString("k"),
                        uzun = o.optString("u"),
                        baglam = o.optString("b"),
                        eklendi = o.optLong("e"),
                        bakildi = o.optInt("s", 1),
                        yildiz = o.optBoolean("y")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sözlük okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Terim>) {
        // Tavanı aşarsa: yıldızlılar ve çok bakılanlar korunur
        val kirpik = if (liste.size <= TAVAN) liste
        else liste.sortedWith(
            compareByDescending<Terim> { it.yildiz }
                .thenByDescending { it.bakildi }
                .thenByDescending { it.eklendi }
        ).take(TAVAN)

        val dizi = JSONArray()
        kirpik.forEach { t ->
            dizi.put(
                JSONObject()
                    .put("t", t.terim).put("k", t.kisa).put("u", t.uzun)
                    .put("b", t.baglam).put("e", t.eklendi)
                    .put("s", t.bakildi).put("y", t.yildiz)
            )
        }
        prefs(context).edit().putString(K_TERIMLER, dizi.toString()).apply()
    }

    /** Türkçe duyarlı normalleştirme — arama ve eşleştirme için. */
    private fun normalle(s: String): String = s.trim().lowercase(Locale("tr"))
        .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
        .replace("ü", "u").replace("ö", "o").replace("ç", "c")
        .replace(Regex("[^a-z0-9 ]"), "")
        .trim()

    fun bul(context: Context, terim: String): Terim? {
        val n = normalle(terim)
        if (n.isBlank()) return null
        return hepsi(context).firstOrNull { normalle(it.terim) == n }
    }

    fun varMi(context: Context, terim: String): Boolean = bul(context, terim) != null

    fun kaydet(context: Context, terim: Terim) {
        val liste = hepsi(context)
        val n = normalle(terim.terim)
        val yer = liste.indexOfFirst { normalle(it.terim) == n }
        if (terim.eklendi == 0L) terim.eklendi = System.currentTimeMillis()
        if (yer >= 0) liste[yer] = terim else liste.add(terim)
        yaz(context, liste)
    }

    /** Terime bakıldığında sayacı artırır. */
    fun bakildiArtir(context: Context, terim: String) {
        val liste = hepsi(context)
        val n = normalle(terim)
        liste.firstOrNull { normalle(it.terim) == n }?.let {
            it.bakildi++
            yaz(context, liste)
        }
    }

    fun yildizDegistir(context: Context, terim: String): Boolean {
        val liste = hepsi(context)
        val n = normalle(terim)
        val t = liste.firstOrNull { normalle(it.terim) == n } ?: return false
        t.yildiz = !t.yildiz
        yaz(context, liste)
        return t.yildiz
    }

    fun sil(context: Context, terim: String) {
        val n = normalle(terim)
        yaz(context, hepsi(context).filterNot { normalle(it.terim) == n })
    }

    fun temizle(context: Context) {
        prefs(context).edit().remove(K_TERIMLER).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // SORGULAR
    // ═══════════════════════════════════════════════════════════════

    fun sayi(context: Context): Int = hepsi(context).size

    fun yildizlilar(context: Context): List<Terim> =
        hepsi(context).filter { it.yildiz }.sortedByDescending { it.eklendi }

    /** En çok bakılan terimler — kullanıcının en zorlandıkları. */
    fun enCokBakilanlar(context: Context, adet: Int = 10): List<Terim> =
        hepsi(context).filter { it.bakildi > 1 }
            .sortedByDescending { it.bakildi }.take(adet)

    fun ara(context: Context, sorgu: String): List<Terim> {
        val n = normalle(sorgu)
        if (n.isBlank()) return hepsi(context).sortedByDescending { it.eklendi }
        return hepsi(context).filter {
            normalle(it.terim).contains(n) || normalle(it.kisa).contains(n)
        }.sortedByDescending { it.eklendi }
    }

    /** Bağlama (konuya) göre gruplu. */
    fun baglamlar(context: Context): List<Pair<String, Int>> =
        hepsi(context).filter { it.baglam.isNotBlank() }
            .groupBy { it.baglam }
            .map { (k, v) -> k to v.size }
            .sortedByDescending { it.second }

    fun tarihMetni(ms: Long): String =
        if (ms <= 0) "" else SimpleDateFormat("d MMM", Locale("tr")).format(Date(ms))

    // ═══════════════════════════════════════════════════════════════
    // YAPAY ZEKÂ İLE AÇIKLAMA
    // ═══════════════════════════════════════════════════════════════

    class Sonuc(
        val ok: Boolean,
        val terim: Terim? = null,
        val hata: String = "",
        /** Önbellekten mi geldi — kullanıcıya "kayıtlıydı" demek için. */
        val onbellekten: Boolean = false
    )

    /**
     * Bir terimi açıklar.
     *
     * Önce sözlüğe bakılır; varsa AI'ya hiç gidilmez (hız + kota).
     * **Ağ isteği yapabilir — arka planda çağır.**
     *
     * @param baglam terimin geçtiği konu — doğru anlamı seçmek için kritik
     */
    fun acikla(
        context: Context,
        terim: String,
        baglam: String = "",
        zorlaYenile: Boolean = false
    ): Sonuc {
        val temiz = terim.trim().take(80)
        if (temiz.length < 2) {
            return Sonuc(false, hata = context.getString(R.string.sz_cok_kisa))
        }

        // Önbellek
        if (!zorlaYenile) {
            bul(context, temiz)?.let {
                bakildiArtir(context, temiz)
                return Sonuc(true, it, onbellekten = true)
            }
        }

        if (!AiSettings.isReady(context)) {
            return Sonuc(false, hata = context.getString(R.string.kn_ai_hazir_degil))
        }

        val baglamSatiri = if (baglam.isBlank()) ""
        else "\nBu terim \"$baglam\" konusu içinde geçiyor. Anlamını BU BAĞLAMA göre ver."

        val istem = """
"$temiz" teriminin ne demek olduğunu bir öğrenciye açıkla.$baglamSatiri

KURALLAR:
1. Terimin ALANINI bağlamdan anla (teknik, dil, tarih, tıp, hukuk...).
   Kendi uzmanlık alanını varsayma.
2. "kisa" tek cümle olsun, en fazla 18 kelime, net bir tanım.
3. "uzun" 2-4 cümle: nasıl kullanılır, neden önemli, somut bir örnek.
4. Terim bir kısaltmaysa açılımını da yaz.
5. Emin değilsen uydurma; "bu bağlamda genellikle ... anlamında kullanılır" de.
6. Türkçe yaz.

SADECE şu JSON'u döndür:
{"kisa":"tek cümlelik tanım","uzun":"detaylı açıklama ve örnek"}
        """.trim()

        return try {
            // v7.99: terim anlamı değişmez — bir ay önbellekte kalabilir
            val cevap = AiOnbellek.getir(context, istem, AiOnbellek.AY) {
                AiClient.sadeIstek(context, istem, butce = 700)
            }
            if (!cevap.ok) return Sonuc(false, hata = cevap.text)

            val json = jsonAyikla(cevap.text)
            val kisa = json?.optString("kisa")?.trim().orEmpty()
            val uzun = json?.optString("uzun")?.trim().orEmpty()

            if (kisa.isBlank() && uzun.isBlank()) {
                // JSON gelmediyse ham metni kullan — boş dönmektense
                val duz = cevap.text.trim().replace(Regex("[{}\\[\\]\"]"), "").take(400)
                if (duz.length < 10) {
                    return Sonuc(false, hata = context.getString(R.string.sz_anlasilmadi))
                }
                val t = Terim(temiz, duz.take(140), duz, baglam, System.currentTimeMillis())
                kaydet(context, t)
                return Sonuc(true, t)
            }

            val t = Terim(
                terim = temiz,
                kisa = kisa.ifBlank { uzun.take(120) },
                uzun = uzun,
                baglam = baglam,
                eklendi = System.currentTimeMillis()
            )
            kaydet(context, t)
            Sonuc(true, t)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Terim açıklanamadı", e)
            Sonuc(false, hata = e.message.orEmpty())
        }
    }

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

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("terimler", prefs(context).getString(K_TERIMLER, "[]"))

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            if (o.has("terimler")) {
                prefs(context).edit()
                    .putString(K_TERIMLER, o.optString("terimler", "[]")).apply()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sözlük içe aktarılamadı", e)
        }
    }
}
