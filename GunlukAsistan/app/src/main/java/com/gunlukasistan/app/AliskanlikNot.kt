package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject

/**
 * v10.40 — Katalog #46: Güne özel alışkanlık notu.
 *
 * "Bugün neden olmadı?" — kartta halkaya uzun basılınca günün notu
 * açılır; not varsa satırda 📝 rozeti belirir. Boş kayıt notu siler.
 * İç-içe JSON: { "<habitId>": { "<yyyyMMdd>": "not" } }
 * Saf katman (JSONObject) JVM testli, depo altta.
 */
object AliskanlikNot {

    // ──────────────── Saf mantık (JVM testli) ────────────────

    fun kokOku(ham: String?): JSONObject =
        runCatching { JSONObject(ham ?: "{}") }.getOrDefault(JSONObject())

    fun notOkuPure(kok: JSONObject, habitId: Long, gunKey: Int): String =
        kok.optJSONObject(habitId.toString())?.optString(gunKey.toString(), "").orEmpty()

    /** Boş metin girdiyi siler; alışkanlık boşalırsa düğümüyle birlikte gider. */
    fun notYazPure(kok: JSONObject, habitId: Long, gunKey: Int, metin: String): JSONObject {
        val anahtar = habitId.toString()
        val h = kok.optJSONObject(anahtar) ?: JSONObject().also { kok.put(anahtar, it) }
        if (metin.isBlank()) h.remove(gunKey.toString()) else h.put(gunKey.toString(), metin)
        if (h.length() == 0) kok.remove(anahtar)
        return kok
    }

    fun habitNotVarmiPure(kok: JSONObject, habitId: Long): Boolean =
        (kok.optJSONObject(habitId.toString())?.length() ?: 0) > 0

    // ──────────────── Depo (SharedPreferences) ────────────────

    private const val PREF = "aliskanlik_not_v1"
    private const val K = "notlar"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun kok(c: Context) = kokOku(prefs(c).getString(K, "{}"))
    private fun kaydet(c: Context, k: JSONObject) {
        runCatching { prefs(c).edit().putString(K, k.toString()).apply() }
    }

    fun notOku(c: Context, habitId: Long, gunKey: Int): String =
        notOkuPure(kok(c), habitId, gunKey)

    fun varMi(c: Context, habitId: Long, gunKey: Int): Boolean =
        notOku(c, habitId, gunKey).isNotBlank()

    /** Alışkanlığın herhangi bir günde notu var mı (rozet için). */
    fun habitNotVarmi(c: Context, habitId: Long): Boolean =
        habitNotVarmiPure(kok(c), habitId)

    fun notYaz(c: Context, habitId: Long, gunKey: Int, metin: String) {
        kaydet(c, notYazPure(kok(c), habitId, gunKey, metin.trim()))
    }

    fun temizle(c: Context, habitId: Long) {
        val k = kok(c)
        if (k.remove(habitId.toString()) != null) kaydet(c, k)
    }
}
