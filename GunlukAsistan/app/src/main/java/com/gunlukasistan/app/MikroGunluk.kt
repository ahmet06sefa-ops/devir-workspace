package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * v10.14 · ULTRA-30 / E27 — Akşam mikro günlüğü.
 *
 * ── Tarama kanıtı ──
 * Günlük alanı yoktu; `NotesFragment` genel not, v10.9 akşam akışı ise
 * pasif bir özet (kullanıcıya soru sormaz). Artık "Uyuyorum"
 * bildirimindeki "✍ 3 soruyla kapat" düğmesi [MikroGunlukActivity]'yi
 * açar: gün puanı + teşekkür + yarının tek şeyi. 30 saniyede biter.
 *
 * ── Saklama ──
 * Tek SharedPreferences JSON'u; anahtar yyyyMMdd. En fazla 62 kayıt
 * tutulur (iki aylık) — eskileri dizenin solundan budanır.
 *
 * Saf duygu hesapları birim testlidir ([emojiFor], [ortalama], [iyiSayisi]).
 */
object MikroGunluk {

    private const val TAG = "MikroGunluk"
    private const val PREF = "ge_mikro_gunluk_v1"
    private const val KOK = "kayitlar"
    private const val SINIR = 62

    /** Bir gecenin üç kısa cevabı. */
    data class Gunluk(val puan: Int, val tesekkur: String, val yarinTekSey: String)

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Saf duygu hesapları (birim testli) ----------------

    /** 1-5 puan → yüz ifadesi. Aralık dışı güvenle ortaya düşer. */
    fun emojiFor(puan: Int): String = when (puan.coerceIn(1, 5)) {
        1 -> "😞"
        2 -> "😕"
        3 -> "😐"
        4 -> "🙂"
        else -> "😄"
    }

    /** Ortalama puan (boş listede 0). */
    fun ortalama(puanlar: List<Int>): Float =
        if (puanlar.isEmpty()) 0f
        else (puanlar.average() * 10).roundToInt() / 10f

    /** "İyi gün" sayısı: puanı 4+ olan günler. */
    fun iyiSayisi(puanlar: List<Int>): Int = puanlar.count { it >= 4 }

    // ---------------- Saklama ----------------

    fun kaydet(c: Context, gunKey: String, g: Gunluk) {
        try {
            val kok = kokOku(c)
            kok.put(
                gunKey,
                JSONObject()
                    .put("p", g.puan.coerceIn(1, 5))
                    .put("t", g.tesekkur.take(80))
                    .put("y", g.yarinTekSey.take(80))
            )
            // Budama: en fazla SINIR kayıt (anahtarlar tarih sıralı)
            while (kok.length() > SINIR) {
                val ilk = kok.keys().asSequence().sorted().firstOrNull() ?: break
                kok.remove(ilk)
            }
            prefs(c).edit().putString(KOK, kok.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kaydedilemedi", e)
        }
    }

    fun gunluk(c: Context, gunKey: String): Gunluk? {
        return try {
            val o = kokOku(c).optJSONObject(gunKey) ?: return null
            Gunluk(o.optInt("p", 0), o.optString("t", ""), o.optString("y", ""))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Okunamadı", e)
            null
        }
    }

    /** Son [gun] günün kayıtları, eski → yeni (harita soldan sağa akar). */
    fun sonKac(c: Context, gun: Int = 31): List<Pair<String, Gunluk>> = try {
        val kok = kokOku(c)
        kok.keys().asSequence().sorted().toList().takeLast(gun).map { anahtar ->
            val o = kok.getJSONObject(anahtar)
            anahtar to Gunluk(
                o.optInt("p", 0), o.optString("t", ""), o.optString("y", "")
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Liste okunamadı", e)
        emptyList()
    }

    private fun kokOku(c: Context): JSONObject = try {
        JSONObject(prefs(c).getString(KOK, "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }
}
