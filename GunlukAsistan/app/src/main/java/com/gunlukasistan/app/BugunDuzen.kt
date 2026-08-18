package com.gunlukasistan.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/**
 * v10.18 · EKRAN ATÖLYESİ — "Bugün" ekranının düzen motoru.
 *
 * ── Kullanıcının isteği ──
 * "Uygulamanın içindeki öğelerin yerlerini değiştirebileyim, üstüne
 *  basılı tutunca boyutlarını/yerlerini değiştirebileyim."
 *
 * ── Kapsam ──
 * v8.5'te yalnız ANA EKRAN düzenlenebiliyordu ([AnaEkranDuzen]).
 * Bu sürümde BUGÜN ekranı da aynı yetenekleri kazandı:
 * sıralama · gizleme · boyut nefesi · katlama — üstelik artık ekranın
 * İÇİNDEN, basılı tutarak ([DuzenSeridi]).
 *
 * ── Blok eşlemesi (fragment_today.xml'deki sarmalayıcılar) ──
 * Selamlama + tarih üstte sabit kalır (ayarlar/sayfa kimliği); 8 blok
 * taşınabilir. Sarmalayıcılar bu sürümde `DuzenBlokLayout` olarak
 * eklendi — düzenleme modunda çocuk dokunuşlarını kesmek için.
 *
 * ── Neden ayrı pref ──
 * Ana ekranın kayıtlarına dokunmadan bağımsız düzen: iki ekranın
 * blokları farklıdır; karışma riski sıfır.
 */
object BugunDuzen {

    private const val TAG = "BugunDuzen"
    private const val PREF = "bugun_duzen_v1"
    private const val K_SIRA = "sira"
    private const val K_GIZLI = "gizli"
    private const val K_BOYUT = "boyut"
    private const val K_KATLI = "katli"

    /** Varsayılan sıra — XML'deki sırayla aynı (yukarıdan aşağı). */
    val bloklar = listOf(
        AnaEkranDuzen.Blok("simdi", R.id.blokBugunSimdi, R.string.bd_simdi, "🎯"),
        AnaEkranDuzen.Blok("namaz", R.id.blokBugunNamaz, R.string.bd_namaz, "🕌"),
        AnaEkranDuzen.Blok("durum", R.id.blokBugunDurum, R.string.bd_durum, "📊"),
        AnaEkranDuzen.Blok("gorevler", R.id.blokBugunGorevler, R.string.bd_gorevler, "✅", katlanabilir = true),
        AnaEkranDuzen.Blok("aliskanlik", R.id.blokBugunAliskanlik, R.string.bd_aliskanlik, "🌱", katlanabilir = true),
        AnaEkranDuzen.Blok("etkinlik", R.id.blokBugunEtkinlik, R.string.bd_etkinlik, "📅", katlanabilir = true),
        AnaEkranDuzen.Blok("ipucu", R.id.blokBugunIpucu, R.string.bd_ipucu, "💡"),
        AnaEkranDuzen.Blok("hizli", R.id.blokBugunHizli, R.string.bd_hizli, "⚡", katlanabilir = true)
    )

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Kayıtlı sıra (kayıtta olmayanlar sona — AnaEkranDuzen kuralı). */
    fun sira(c: Context): List<AnaEkranDuzen.Blok> =
        AnaEkranDuzen.siralaKayittan(p(c).getString(K_SIRA, "") ?: "", bloklar)

    fun siraKaydet(c: Context, yeni: List<AnaEkranDuzen.Blok>) {
        p(c).edit().putString(K_SIRA, yeni.joinToString(",") { it.kod }).apply()
    }

    fun gizliler(c: Context): Set<String> =
        p(c).getStringSet(K_GIZLI, emptySet())?.toSet() ?: emptySet()

    fun gizle(c: Context, kod: String, gizli: Boolean) {
        val blok = bloklar.firstOrNull { it.kod == kod } ?: return
        if (blok.zorunlu && gizli) return
        val yeni = gizliler(c).toMutableSet()
        if (gizli) yeni.add(kod) else yeni.remove(kod)
        p(c).edit().putStringSet(K_GIZLI, HashSet(yeni)).apply()
    }

    fun boyutKademe(c: Context, kod: String): Int =
        DuzenCekirdek.boyutKayitOku(p(c).getString(K_BOYUT, "") ?: "")[kod] ?: 1

    fun setBoyutKademe(c: Context, kod: String, kademe: Int) {
        val h = DuzenCekirdek.boyutKayitOku(p(c).getString(K_BOYUT, "") ?: "").toMutableMap()
        h[kod] = kademe
        p(c).edit().putString(K_BOYUT, DuzenCekirdek.boyutKayitYaz(h)).apply()
    }

    fun katliMi(c: Context, kod: String): Boolean =
        kod in (p(c).getStringSet(K_KATLI, emptySet()) ?: emptySet())

    fun setKatli(c: Context, kod: String, katli: Boolean) {
        val blok = bloklar.firstOrNull { it.kod == kod } ?: return
        if (!blok.katlanabilir) return
        val yeni = (p(c).getStringSet(K_KATLI, emptySet()) ?: emptySet()).toMutableSet()
        if (katli) yeni.add(kod) else yeni.remove(kod)
        p(c).edit().putStringSet(K_KATLI, HashSet(yeni)).apply()
    }

    fun tasi(c: Context, kod: String, yon: Int) {
        val yeniKodlar = DuzenCekirdek.tasi(sira(c).map { it.kod }, kod, yon)
        siraKaydet(c, yeniKodlar.mapNotNull { k -> bloklar.firstOrNull { it.kod == k } })
    }

    fun varsayilanaDon(c: Context) {
        p(c).edit().remove(K_SIRA).remove(K_GIZLI)
            .remove(K_BOYUT).remove(K_KATLI).apply()
    }

    fun ozellestirilmisMi(c: Context): Boolean {
        val d = p(c)
        return !(d.getString(K_SIRA, "") ?: "").isBlank() ||
            gizliler(c).isNotEmpty() ||
            !(d.getString(K_BOYUT, "") ?: "").isBlank() ||
            !(d.getStringSet(K_KATLI, emptySet()) ?: emptySet()).isEmpty()
    }

    // ------------------------------------------------------------------

    /**
     * Kayıtlı düzeni Bugün ekranına uygular.
     * Algoritma [AnaEkranDuzen.uygula] ile aynıdır (v8.7 düzeltmeli
     * "önce hepsini çıkar, sırayla geri ekle" yöntemi); fark: selamlama
     * + tarih 0-1. konumda sabittir, bloklar 2'den itibaren dizilir.
     */
    fun uygula(c: Context, kok: View?) {
        kok ?: return
        runCatching {
            val kap = kok.findViewById<View>(R.id.blokBugunSimdi)?.parent as? LinearLayout
                ?: return
            val gizli = gizliler(c)
            val sirali = sira(c)

            val cikarilanlar = LinkedHashMap<String, View>()
            sirali.forEach { blok ->
                val g = kok.findViewById<View>(blok.viewId) ?: return@forEach
                if (kap.indexOfChild(g) >= 0) {
                    kap.removeView(g)
                    cikarilanlar[blok.kod] = g
                }
            }

            // Selamlama(0) + tarih(1) sabit; bloklar 2'den başlar
            var konum = 2
            sirali.forEach { blok ->
                val g = cikarilanlar[blok.kod] ?: return@forEach
                g.visibility =
                    if (blok.kod in gizli && !blok.zorunlu) View.GONE else View.VISIBLE
                AnaEkranDuzen.boyutVeKatlaUygula(
                    blok, g, boyutKademe(c, blok.kod), katliMi(c, blok.kod)
                )
                kap.addView(g, konum.coerceAtMost(kap.childCount))
                konum++
            }
        }.onFailure { android.util.Log.w(TAG, "uygula", it) }
    }
}
