package com.gunlukasistan.app

import android.content.Context

/**
 * v10.32 · Katalog #29 — not sürüm geçmişi (son 5 kayıt).
 *
 * Her kayıt: damga + başlık + içerik + görsel adı. Alanlar UTF-8
 * onaltılık (hex) olarak kodlanır — metin parçalayıcıda kaçış derdi
 * hiç yoktur ve kodlama JVM'de de birebir çalışır (test edilebilir).
 * Kayıtlar `not_surum_v1` dosyasında not başına anahtarla tutulur;
 * PrefYedek taramasına doğal olarak girer.
 */
object NotSurum {

    const val SINIR = 5

    data class Kayit(val zaman: Long, val baslik: String, val icerik: String, val goruntu: String)

    // ──────────────── saf mantık (android YOK, JVM testli) ────────────────

    /** Yeni kaydı başa ekler, liste [SINIR] ile budanır. */
    fun it(liste: List<Kayit>, yeni: Kayit): List<Kayit> =
        (listOf(yeni) + liste).take(SINIR)

    /** Üst üste aynı kayıt şişmesin diye: içerik aynıysa zaten sondadır. */
    fun zatenSonMu(liste: List<Kayit>, yeni: Kayit): Boolean =
        liste.firstOrNull()?.let {
            it.baslik == yeni.baslik && it.icerik == yeni.icerik && it.goruntu == yeni.goruntu
        } == true

    private fun hexle(s: String): String =
        s.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

    private fun hexCoz(s: String): String = runCatching {
        String(s.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), Charsets.UTF_8)
    }.getOrDefault("")

    fun metneCevir(liste: List<Kayit>): String =
        liste.joinToString("\n") {
            it.zaman.toString() + "|" + hexle(it.baslik) + "|" + hexle(it.icerik) + "|" + hexle(it.goruntu)
        }

    fun metindenOku(metin: String?): List<Kayit> {
        if (metin.isNullOrBlank()) return emptyList()
        return metin.lines().mapNotNull { satir ->
            val p = satir.split("|")
            if (p.size != 4) return@mapNotNull null
            val z = p[0].toLongOrNull() ?: return@mapNotNull null
            Kayit(z, hexCoz(p[1]), hexCoz(p[2]), hexCoz(p[3]))
        }
    }

    // ──────────────── depo (SharedPreferences) ────────────────

    private const val PREF = "not_surum_v1"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun anahtar(notId: Long) = "g_$notId"

    fun gecmis(ctx: Context, notId: Long): List<Kayit> =
        runCatching { metindenOku(pref(ctx).getString(anahtar(notId), "")) }.getOrDefault(emptyList())

    /** Notun ŞU ANKİ hâlini geçmişe iter; değişmemişse yazmaz. */
    fun gecmiseIt(ctx: Context, not: Store.Note) {
        runCatching {
            val yeni = Kayit(System.currentTimeMillis(), not.title, not.content, not.image)
            val mevcut = gecmis(ctx, not.id)
            if (zatenSonMu(mevcut, yeni)) return@runCatching
            pref(ctx).edit().putString(anahtar(not.id), metneCevir(it(mevcut, yeni))).apply()
        }
    }

    /** Not kalıcı silinirse geçmişini de süpürür. */
    fun temizle(ctx: Context, notId: Long) {
        pref(ctx).edit().remove(anahtar(notId)).apply()
    }
}
