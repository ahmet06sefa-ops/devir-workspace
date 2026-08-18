package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v10.8 · Öneri D40 — Tema paketleri (tema stüdyosu).
 *
 * ── Ne eksikti ──
 * Görünüm ekranı zengin: 10 tema + 12 vurgu + gece modu + yoğunluk +
 * yazı ölçeği + dinamik renk. Ama hepsi **tek tek** ayarlanıyor;
 * "sınav dönemi düzenimi kışlıkla değiştireyim, sonra geri döneyim"
 * demek 5 farklı satıra tekrar dokunmak demekti. Kaydedilip çağrılan
 * kombinasyon yoktu.
 *
 * ── Bu ne yapar ──
 * [Paket] = altı tercihin anlık fotoğrafı (tema, vurgu, gece modu,
 * yoğunluk, yazı ölçeği, dinamik renk). Kullanıcı mevcut düzenini
 * isimlendirip kaydeder; tek dokunuşla geri uygulanır. 4 hazır
 * kombin de kutu içinden çıkar.
 *
 * ── Neden JSON ──
 * Paket listesi değişken uzunlukta; düz anahtar-değere sığmaz.
 * `org.json` Android'de hazır. Şablon adları veri kataloğudur;
 * arayüz metinleri strings.xml'dedir (v10.7 zincir kalıbı).
 */
object TemaPaketi {

    private const val PREF = "tema_paketi_v1"
    private const val K_LISTE = "liste"
    private const val K_SONRAKI_ID = "sonraki_id"

    /** Kullanıcının kaydedebileceği en fazla paket. */
    const val MAKS = 8

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // VERİ MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Altı tercihin paketi. Dizinler [ThemeManager.specs],
     * [ThemeManager.accents] ve [GorunumAyar] aralıklarına işaret
     * eder; [dogrulanmis] aralık dışını budar.
     */
    data class Paket(
        val id: Long,
        val ad: String,
        val emoji: String,
        val tema: Int,      // ThemeManager.specs dizini
        val vurgu: Int,     // accents dizini, -1 = tema varsayılanı
        val gece: Int,      // 0 sistem · 1 hep açık · 2 hep koyu
        val yogunluk: Int,  // 0 sıkı · 1 normal · 2 rahat
        val yazi: Int,      // 0 küçük · 1 normal · 2 büyük · 3 çok büyük
        val dinamik: Boolean
    ) {
        fun json(): JSONObject = JSONObject()
            .put("id", id)
            .put("ad", ad)
            .put("emoji", emoji)
            .put("tema", tema)
            .put("vurgu", vurgu)
            .put("gece", gece)
            .put("yogunluk", yogunluk)
            .put("yazi", yazi)
            .put("dinamik", dinamik)

        companion object {
            fun jsondan(o: JSONObject): Paket? {
                val ad = o.optString("ad", "").trim()
                if (ad.isEmpty()) return null
                return Paket(
                    id = o.optLong("id", 0L),
                    ad = ad.take(20),
                    emoji = o.optString("emoji", "🎨").take(4),
                    tema = o.optInt("tema", 0),
                    vurgu = o.optInt("vurgu", -1),
                    gece = o.optInt("gece", 0),
                    yogunluk = o.optInt("yogunluk", 1),
                    yazi = o.optInt("yazi", 1),
                    dinamik = o.optBoolean("dinamik", false)
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF MANTIK — doğrulama, JSON, şablonlar
    // ═══════════════════════════════════════════════════════════════

    /**
     * Her alanı yasal aralığa çeker.
     *
     * Yedek içe aktarımında ya da ileride tema sayısı azalırsa elde
     * bozuk dizin kalmaz — [uygula] da bu kapıdan geçer.
     */
    fun dogrulanmis(p: Paket): Paket {
        val temaUst = (ThemeManager.specs.size - 1).coerceAtLeast(0)
        val vurguUst = ThemeManager.accents.size - 1
        return p.copy(
            ad = p.ad.trim().ifEmpty { "Paket" }.take(20),
            emoji = p.emoji.take(4).ifEmpty { "🎨" },
            tema = p.tema.coerceIn(0, temaUst),
            vurgu = p.vurgu.coerceIn(-1, vurguUst),
            gece = p.gece.coerceIn(0, 2),
            yogunluk = p.yogunluk.coerceIn(0, 2),
            yazi = p.yazi.coerceIn(0, 3)
        )
    }

    /** Kayıtlı paketleri tek metne kodlar. */
    fun kodla(liste: List<Paket>): String {
        val dizi = JSONArray()
        liste.forEach { dizi.put(it.json()) }
        return dizi.toString()
    }

    /** [kodla]'nın tersi. Bozuk satırlar atlanır, çöp metin boş verir. */
    fun cozle(metin: String): List<Paket> {
        if (metin.isBlank()) return emptyList()
        return try {
            val dizi = JSONArray(metin)
            val liste = mutableListOf<Paket>()
            for (i in 0 until dizi.length()) {
                dizi.optJSONObject(i)?.let { Paket.jsondan(it) }?.let { liste.add(it) }
            }
            liste
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Dizine eklenme izni — kota [MAKS]. */
    fun eklenebilirMi(mevcutSayi: Int): Boolean = mevcutSayi < MAKS

    /**
     * Hazır kombinler (veri kataloğu — arayüz metinleri strings'de).
     * Dizinler [ThemeManager.specs]/[accents] sırasına sabit;
     * oranın sırası değişirse sablon testi tutuşur.
     */
    fun sablonlar(): List<Paket> = listOf(
        Paket(-1, "Gün ışığı", "☀️", tema = 0, vurgu = 0, gece = 1, yogunluk = 1, yazi = 1, dinamik = false),
        Paket(-2, "Sınav odağı", "📚", tema = 1, vurgu = 4, gece = 0, yogunluk = 0, yazi = 1, dinamik = false),
        Paket(-3, "Gece kuşu", "🦉", tema = 7, vurgu = 11, gece = 2, yogunluk = 2, yazi = 1, dinamik = false),
        Paket(-4, "Neon hız", "⚡", tema = 9, vurgu = -1, gece = 2, yogunluk = 0, yazi = 0, dinamik = false)
    )

    // ═══════════════════════════════════════════════════════════════
    // KALICILIK VE UYGULAMA
    // ═══════════════════════════════════════════════════════════════

    /** Kullanıcının paketleri (şablonlar hariç). */
    fun listele(context: Context): List<Paket> =
        cozle(prefs(context).getString(K_LISTE, "[]") ?: "[]")

    /** Yeni paketi kaydeder; gerçek kimliği döner. Kota dolmuşsa null. */
    fun kaydet(context: Context, taslak: Paket): Paket? {
        val mevcut = listele(context)
        if (!eklenebilirMi(mevcut.size)) return null
        val p = prefs(context)
        val yeniId = p.getLong(K_SONRAKI_ID, 1L)
        val gercek = dogrulanmis(taslak.copy(id = yeniId))
        p.edit()
            .putString(K_LISTE, kodla(mevcut + gercek))
            .putLong(K_SONRAKI_ID, yeniId + 1)
            .apply()
        return gercek
    }

    fun sil(context: Context, id: Long) {
        prefs(context).edit()
            .putString(K_LISTE, kodla(listele(context).filterNot { it.id == id }))
            .apply()
    }

    /** O an ekranda yaşayan altı tercihin fotoğrafı. */
    fun simdikiDurum(context: Context, ad: String, emoji: String): Paket = Paket(
        id = 0,
        ad = ad,
        emoji = emoji,
        tema = ThemeManager.selected(context),
        vurgu = ThemeManager.accentIndex(context),
        gece = ThemeManager.geceModu(context),
        yogunluk = GorunumAyar.yogunluk(context),
        yazi = GorunumAyar.yaziOlcek(context),
        dinamik = ThemeManager.dinamikAcik(context)
    )

    /**
     * Paketi canlı düzene uygular.
     *
     * Sıra önemli değil; her tercih kendi prefs anahtarına yazılır.
     * Görsel yankı (Activity recreate + widget tazeleme) çağıranın
     * işi — burası yalnızca durumu değiştirir.
     */
    fun uygula(context: Context, taslak: Paket) {
        val p = dogrulanmis(taslak)
        ThemeManager.select(context, p.tema)
        ThemeManager.selectAccent(context, p.vurgu)
        ThemeManager.geceModu(context, p.gece)
        ThemeManager.dinamikAcik(context, p.dinamik && ThemeManager.dinamikDesteklenir())
        GorunumAyar.yogunluk(context, p.yogunluk)
        GorunumAyar.yaziOlcek(context, p.yazi)
    }
}
