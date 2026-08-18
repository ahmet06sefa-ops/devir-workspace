package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v10.11 · ULTRA-30 A2 — Bağlam profilleri.
 *
 * ── Fikir ──
 * "Sınav haftası başka, iş düzeni başka, izin günü başka yaşanır."
 * Bir dokunuşla altı tercih birden değişir:
 *
 *   1. Tema seti (tema + vurgu + gece + yoğunluk + yazı + dinamik)
 *      → [TemaPaketi.uygula] ile birebir aynı mekanik
 *   2. Sessiz pencere (başlangıç/bitiş saati)
 *   3. Günlük odak hedefi (dakika)
 *   4. Uyku çerçevesi kapıları (sabah/akşam saati)
 *
 * ── Neden kopya, referans değil ──
 * Profil, bağlı olduğu tema paketinin İÇERİĞİNİ SAKLAR; paket
 * sonra silinse/değişse bile profil bozulmaz (ders: v10.8 tema
 * paketi doğrulamasındaki silinme senaryosu).
 *
 * ── Değişmezler ──
 * -1 değeri "bu yön değişmesin" demektir (sessiz saatler, hedef,
 * uyku kapıları). -1'li profil, uygulanırken o ayarlara dokunmaz.
 */
object BaglamProfili {

    private const val PREF = "baglam_profili_v1"
    private const val K_LISTE = "profiller"
    private const val MAKS_OZEL = 5

    // "değişmesin" işareti
    const val DEGISMEZ = -1

    data class Profil(
        val id: Long,
        val ad: String,
        val emoji: String,
        // Tema seti (TemaPaketi.Paket ile aynı alanlar)
        val tema: Int,
        val vurgu: Int,
        val gece: Int,
        val yogunluk: Int,
        val yazi: Int,
        val dinamik: Boolean,
        // Genişleme (DEGISMEZ/-1 = uygulanmaz)
        val sessizBas: Int,
        val sessizBit: Int,
        val hedefDk: Int,
        val sabahDk: Int,
        val aksamDk: Int
    ) {
        fun json(): JSONObject = JSONObject()
            .put("id", id).put("ad", ad).put("emoji", emoji)
            .put("te", tema).put("vu", vurgu).put("ge", gece)
            .put("yo", yogunluk).put("ya", yazi).put("di", dinamik)
            .put("sb", sessizBas).put("si", sessizBit)
            .put("he", hedefDk).put("sa", sabahDk).put("ak", aksamDk)

        companion object {
            fun jsondan(o: JSONObject): Profil? {
                val ad = o.optString("ad", "").trim()
                if (ad.isEmpty()) return null
                return Profil(
                    id = o.optLong("id", 0L),
                    ad = ad.take(20),
                    emoji = o.optString("emoji", "🎭").take(4),
                    tema = o.optInt("te", 0),
                    vurgu = o.optInt("vu", -1),
                    gece = o.optInt("ge", 0),
                    yogunluk = o.optInt("yo", 1),
                    yazi = o.optInt("ya", 1),
                    dinamik = o.optBoolean("di", false),
                    sessizBas = o.optInt("sb", DEGISMEZ),
                    sessizBit = o.optInt("si", DEGISMEZ),
                    hedefDk = o.optInt("he", DEGISMEZ),
                    sabahDk = o.optInt("sa", DEGISMEZ),
                    aksamDk = o.optInt("ak", DEGISMEZ)
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HAZIR PROFİLLER
    // ═══════════════════════════════════════════════════════════════

    fun sablonlar(): List<Profil> = listOf(
        Profil(
            id = -1, ad = "Sınav", emoji = "🎓",
            tema = 9, vurgu = 11, gece = ThemeManager.GECE_ACIK,
            yogunluk = 0, yazi = 1, dinamik = false,
            sessizBas = 22, sessizBit = 7,
            hedefDk = 90, sabahDk = 390, aksamDk = 1320
        ),
        Profil(
            id = -2, ad = "İş", emoji = "💼",
            tema = 2, vurgu = 4, gece = ThemeManager.GECE_SISTEM,
            yogunluk = 1, yazi = 1, dinamik = false,
            sessizBas = 20, sessizBit = 8,
            hedefDk = 60, sabahDk = 450, aksamDk = 1275
        ),
        Profil(
            id = -3, ad = "Dinlenme", emoji = "🌿",
            tema = 3, vurgu = 6, gece = ThemeManager.GECE_SISTEM,
            yogunluk = 2, yazi = 2, dinamik = false,
            sessizBas = DEGISMEZ, sessizBit = DEGISMEZ,
            hedefDk = 30, sabahDk = 510, aksamDk = 1410
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // KAYIT
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun listele(context: Context): List<Profil> {
        val ham = prefs(context).getString(K_LISTE, "[]") ?: "[]"
        return try {
            val dizi = JSONArray(ham)
            val liste = mutableListOf<Profil>()
            for (i in 0 until dizi.length()) {
                dizi.optJSONObject(i)?.let { Profil.jsondan(it) }?.let { liste.add(it) }
            }
            liste
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Kullanıcının şu anki durumunu profil taslağı yapar. */
    fun simdikiDurum(context: Context, ad: String, emoji: String): Profil {
        val paket = TemaPaketi.simdikiDurum(context, ad, emoji)
        return Profil(
            id = System.currentTimeMillis(),
            ad = ad, emoji = emoji,
            tema = paket.tema, vurgu = paket.vurgu, gece = paket.gece,
            yogunluk = paket.yogunluk, yazi = paket.yazi, dinamik = paket.dinamik,
            sessizBas = BildirimMerkezi.sessizBaslangic(context),
            sessizBit = BildirimMerkezi.sessizBitis(context),
            hedefDk = Koc.gunlukHedef(context),
            sabahDk = UykuCerceve.sabahDk(context),
            aksamDk = UykuCerceve.aksamDk(context)
        )
    }

    /** Kaydeder; kota doluysa null döner. */
    fun kaydet(context: Context, taslak: Profil): Profil? {
        val liste = listele(context).toMutableList()
        if (liste.size >= MAKS_OZEL) return null
        val yeni = taslak.copy(id = System.currentTimeMillis())
        liste.add(yeni)
        val dizi = JSONArray()
        liste.forEach { dizi.put(it.json()) }
        prefs(context).edit().putString(K_LISTE, dizi.toString()).apply()
        return yeni
    }

    fun sil(context: Context, id: Long) {
        val liste = listele(context).filter { it.id != id }
        val dizi = JSONArray()
        liste.forEach { dizi.put(it.json()) }
        prefs(context).edit().putString(K_LISTE, dizi.toString()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // UYGULAMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Profili yaşayan sisteme basar.
     *
     * Sıralama kritik: tema seti (recreate'i tetikleyecek) önce,
     * sayısal ayarlar sonra — kullanıcı geri döndüğünde her şey
     * tutarlı bulunur. Alarm/zamanlayıcı etkileri son dokunuşta
     * tazelenir. Activity recreate'i ÇAĞIRAN yapar (TemaPaketi
     * akışıyla aynı mekanik, ThemeFragment'teki eş ile tutarlı).
     */
    fun uygula(context: Context, profil: Profil) {
        // 1) Tema seti — TemaPaketi ile aynı boru (JSON doğrulaması orada)
        TemaPaketi.uygula(
            context,
            TemaPaketi.Paket(
                id = 0L, ad = profil.ad, emoji = profil.emoji,
                tema = profil.tema, vurgu = profil.vurgu, gece = profil.gece,
                yogunluk = profil.yogunluk, yazi = profil.yazi,
                dinamik = profil.dinamik
            )
        )
        // 2) Sessiz pencere
        if (profil.sessizBas != DEGISMEZ && profil.sessizBit != DEGISMEZ) {
            runCatching {
                BildirimMerkezi.setSessizMod(context, true)
                BildirimMerkezi.setSessizSaatler(context, profil.sessizBas, profil.sessizBit)
            }
        }
        // 3) Günlük odak hedefi
        if (profil.hedefDk != DEGISMEZ) {
            runCatching { Koc.setGunlukHedef(context, profil.hedefDk) }
        }
        // 4) Uyku çerçevesi kapıları + alarm tazeleme
        if (profil.sabahDk != DEGISMEZ) {
            runCatching { UykuCerceve.setSabahDk(context, profil.sabahDk) }
        }
        if (profil.aksamDk != DEGISMEZ) {
            runCatching { UykuCerceve.setAksamDk(context, profil.aksamDk) }
        }
        runCatching { UykuZamanla.kur(context) }
        runCatching { WidgetCommon.refreshAll(context, true) }
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF KISIM (birim testli)
    // ═══════════════════════════════════════════════════════════════

    /** Profilin değerleri sınırlar içinde mi (yedek aktarımı koruması). */
    fun dogrulanmis(p: Profil): Boolean {
        if (p.ad.isBlank() || p.ad.length > 20) return false
        if (p.gece !in 0..3) return false
        if (p.yogunluk !in 0..2 || p.yazi !in 0..3) return false
        if (p.sessizBas != DEGISMEZ && p.sessizBas !in 0..23) return false
        if (p.sessizBit != DEGISMEZ && p.sessizBit !in 0..23) return false
        if (p.hedefDk != DEGISMEZ && p.hedefDk !in 0..600) return false
        if (p.sabahDk != DEGISMEZ && p.sabahDk !in 0..1439) return false
        if (p.aksamDk != DEGISMEZ && p.aksamDk !in 0..1439) return false
        return true
    }
}
