package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v10.14 · ULTRA-30 / E28 — Sesli nottan akıllı görev, iz bırakıyor.
 *
 * ── Tarama düzeltmesi (dürüstlük) ──
 * Öneri "SesliNot yalnız kayıt/oynatma" diyordu — yanlıştı: v7.71'de
 * SpeechRecognizer + yerel kural motoru + AI sınıflandırma + NaturalDate
 * tarih çıkarımı zaten görev kuruyor (`SesliNot.kaydet`).
 * E28'in gerçek boşluğu: işlenen sesli notlar bir listede DURMUYORDU —
 * "söylediğim şey nereye gitti?" sorusunun cevabı yoktu. Gelen kutusu
 * budur: son 60 not (en fazla 30 gün), hedef emoji'siyle listelenir.
 *
 * Saklama tek JSON dizisi; ekleme başa yazar, budama sondan atar.
 * [buHafta] bölümlemesi framework'süzdür ve birim testlidir.
 */
object SesliKutu {

    private const val TAG = "SesliKutu"
    private const val PREF = "ge_sesli_kutu_v1"
    private const val KOK = "liste"
    private const val SINIR = 60
    private const val MAKS_YAS_MS = 30L * 86_400_000L

    data class Not(val ts: Long, val metin: String, val hedef: String)

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Hedef kodu → çekmece emojisi. */
    fun hedefEmoji(hedef: String): String = when (hedef) {
        "GOREV" -> "✅"
        "ALISVERIS" -> "🛒"
        "PLAN" -> "🕌"
        "ASISTAN" -> "🤖"
        else -> "📝"
    }

    fun ekle(c: Context, hedef: SesliNot.Hedef, metin: String) {
        try {
            val temiz = metin.trim()
            if (temiz.isEmpty()) return
            val dizi = diziOku(c)
            val yeni = JSONArray()
            yeni.put(
                JSONObject()
                    .put("t", System.currentTimeMillis())
                    .put("m", temiz.take(120))
                    .put("h", hedef.name)
            )
            val simdi = System.currentTimeMillis()
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                // 30 günden yaşlı izler sessizce budanır
                if (simdi - o.optLong("t", 0L) > MAKS_YAS_MS) continue
                if (yeni.length() >= SINIR) break
                yeni.put(o)
            }
            prefs(c).edit().putString(KOK, yeni.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Eklenemedi", e)
        }
    }

    fun liste(c: Context): List<Not> = try {
        val dizi = diziOku(c)
        (0 until dizi.length()).mapNotNull { i ->
            dizi.optJSONObject(i)?.let { o ->
                Not(o.optLong("t", 0L), o.optString("m", ""), o.optString("h", "NOT"))
            }
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Liste okunamadı", e)
        emptyList()
    }

    private fun diziOku(c: Context): JSONArray = try {
        JSONArray(prefs(c).getString(KOK, "[]") ?: "[]")
    } catch (e: Exception) {
        JSONArray()
    }

    // ---------------- Saf bölümleme (birim testli) ----------------

    /**
     * Listeyi "bu hafta" (son 7 gün) ve "daha eski" diye böler.
     * Girdi sırası korunur (liste zaten yeni → eski gelir).
     */
    fun buHafta(liste: List<Not>, simdiMs: Long): Pair<List<Not>, List<Not>> {
        val esik = simdiMs - 7L * 86_400_000L
        val hafta = liste.filter { it.ts >= esik }
        val eski = liste.filter { it.ts < esik }
        return hafta to eski
    }
}
