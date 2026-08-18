package com.gunlukasistan.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * v8.5 — Ana ekran özelleştirme (öneri 16).
 *
 * ── Uzun süredir bekleyen madde ──
 * Bu, önceki 20'lik öneri listesinin de tek eksik maddesiydi (v7.97'de
 * "tek başına bir sürümlük iş" denip ertelenmişti). Gerekçe doğruydu:
 * `fragment_home.xml` 725 satırlık sabit bir XML'di.
 *
 * ── Neden XML tamamen yeniden yazılmadı ──
 * İlk düşünce her kartı ayrı layout dosyasına çıkarıp RecyclerView'a
 * beslemekti. Ama ana ekranda 40 id var ve `HomeFragment` bunların
 * hepsini `view.findViewById` ile arıyor. Parçalara bölmek 564 satırlık
 * fragment'ı baştan yazmak demekti — yüksek risk, sıfır görsel kazanç.
 *
 * **Seçilen yol:** XML'deki 14 üst seviye öğe, mantıksal olarak
 * 8 bloğa gruplandı ve her grup id'li bir `LinearLayout` ile sarıldı.
 * `findViewById` çağrılarının hiçbiri bozulmadı (kontrol edildi:
 * 40 id'nin 40'ı duruyor). Bu sınıf yalnız blokların **sırasını** ve
 * **görünürlüğünü** yönetiyor.
 *
 * ── Selamlama başlığı neden taşınmıyor ──
 * İçinde ayarlar ve zamanlayıcı düğmeleri var; en üstte sabit kalması
 * gerekiyor. Listede hiç görünmüyor.
 *
 * ── Saklama biçimi ──
 * Sıra: "blokHero,blokIstatistik,..." · Gizliler ayrı bir küme.
 * Yeni bir blok eklenirse (gelecekteki sürümlerde) kayıtlı sırada
 * bulunmaz ve otomatik olarak sona eklenir — eski kullanıcı yeni
 * kartı kaybetmez.
 */
object AnaEkranDuzen {

    private const val TAG = "AnaEkranDuzen"
    private const val PREF = "ana_ekran_duzen_v1"
    private const val K_SIRA = "sira"
    private const val K_GIZLI = "gizli"
    private const val K_BOYUT = "boyut"      // v10.18: "kod:kademe" haritası
    private const val K_KATLI = "katli"      // v10.18: katlanmış bloklar kümesi

    /** Bir taşınabilir blok. */
    data class Blok(
        val kod: String,
        val viewId: Int,
        val baslikRes: Int,
        val simge: String,
        /** Gizlenemeyen bloklar (yoksa ekran anlamsız kalır). */
        val zorunlu: Boolean = false,
        /** v10.18: ilk çocuğu başlık sayılıp gövdesi katlanabilen bloklar. */
        val katlanabilir: Boolean = false
    )

    /** Varsayılan sıra — XML'deki sırayla aynı. */
    val bloklar = listOf(
        Blok("hero", R.id.blokHero, R.string.ad_hero, "🎯", zorunlu = true),
        Blok("kurslar", R.id.blokKurslar, R.string.ad_kurslar, "🏗"),
        Blok("rozet", R.id.blokRozet, R.string.ad_rozet, "🏅"),
        Blok("istatistik", R.id.blokIstatistik, R.string.ad_istatistik, "📊", katlanabilir = true),
        Blok("grafik", R.id.blokGrafik, R.string.ad_grafik, "📈"),
        Blok("izgara", R.id.blokIzgara, R.string.ad_izgara, "🔥"),
        Blok("hizli", R.id.blokHizli, R.string.ad_hizli, "⚡", katlanabilir = true),
        Blok("konular", R.id.blokKonular, R.string.ad_konular, "📚", katlanabilir = true)
    )

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------

    /**
     * Kayıtlı sıra. Kayıtta olmayan bloklar sona eklenir.
     *
     * Bu davranış önemli: v8.6'da yeni bir kart eklersek, v8.5'te
     * sırasını özelleştirmiş kullanıcı o kartı görmeye devam eder.
     */
    fun sira(c: Context): List<Blok> = siralaKayittan(p(c).getString(K_SIRA, "") ?: "", bloklar)

    /**
     * v8.7 — Sıralama mantığı, test edilebilmesi için ayrıldı.
     *
     * Android bağımlılığı yok; saf liste işlemi. `AnaEkranDuzenTest`
     * bunu doğruluyor — cihazda deneyemediğim için tek güvence bu.
     *
     * Kurallar:
     * · Kayıt boşsa varsayılan sıra
     * · Kayıtta olmayan blok SONA eklenir (ileri sürümde yeni kart
     *   eklenirse eski kullanıcı onu kaybetmesin)
     * · Kayıttaki bilinmeyen kod (kaldırılmış blok) yok sayılır
     * · Yinelenen kod bir kez alınır
     */
    fun siralaKayittan(kayit: String, tumBloklar: List<Blok>): List<Blok> {
        if (kayit.isBlank()) return tumBloklar

        val gorulen = LinkedHashSet<String>()
        val eslesen = mutableListOf<Blok>()
        kayit.split(",").forEach { ham ->
            val kod = ham.trim()
            if (kod.isEmpty() || !gorulen.add(kod)) return@forEach
            tumBloklar.firstOrNull { it.kod == kod }?.let { eslesen.add(it) }
        }
        val eksik = tumBloklar.filter { b -> eslesen.none { it.kod == b.kod } }
        return eslesen + eksik
    }

    fun siraKaydet(c: Context, yeni: List<Blok>) {
        p(c).edit().putString(K_SIRA, yeni.joinToString(",") { it.kod }).apply()
    }

    // ------------------------------------------------------------------

    fun gizliler(c: Context): Set<String> =
        p(c).getStringSet(K_GIZLI, emptySet())?.toSet() ?: emptySet()

    fun gizliMi(c: Context, kod: String): Boolean = kod in gizliler(c)

    fun gizle(c: Context, kod: String, gizli: Boolean) {
        val blok = bloklar.firstOrNull { it.kod == kod } ?: return
        if (blok.zorunlu && gizli) return   // zorunlu blok gizlenemez
        val yeni = gizliler(c).toMutableSet()
        if (gizli) yeni.add(kod) else yeni.remove(kod)
        // putStringSet aynı küme nesnesini tutarsa değişiklik yazılmıyor;
        // her seferinde YENİ bir HashSet vermek gerekiyor (bilinen tuzak).
        p(c).edit().putStringSet(K_GIZLI, HashSet(yeni)).apply()
    }

    fun varsayilanaDon(c: Context) {
        p(c).edit().remove(K_SIRA).remove(K_GIZLI)
            .remove(K_BOYUT).remove(K_KATLI).apply()
    }

    // ------------------------------------------------------------------
    // v10.18 — BOYUT + KATLAMA + TAŞIMA (yerinde düzenleme için)

    /** Bloğun boyut kademesi (0=Kompakt · 1=Normal varsayılan · 2=Geniş). */
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
        if (!blok.katlanabilir) return   // katlanamayan bloğa sessizlik
        val yeni = (p(c).getStringSet(K_KATLI, emptySet()) ?: emptySet()).toMutableSet()
        if (katli) yeni.add(kod) else yeni.remove(kod)
        p(c).edit().putStringSet(K_KATLI, HashSet(yeni)).apply()
    }

    /** Bloku sırada bir yukarı/aşağı taşır (yerinde düzenleme ▲▼). */
    fun tasi(c: Context, kod: String, yon: Int) {
        val yeniKodlar = DuzenCekirdek.tasi(sira(c).map { it.kod }, kod, yon)
        siraKaydet(c, yeniKodlar.mapNotNull { k -> bloklar.firstOrNull { it.kod == k } })
    }

    /** Katlanmış blok çocuklarının görünürlük kaydı (oturum içi). */
    private val katlamaKaydi = java.util.WeakHashMap<View, IntArray>()

    /**
     * v10.18 — Bloğun boyut nefesini ve katlama durumunu uygular.
     * Bugün ekranının motoru (BugunDuzen) da aynı uygulayıcıyı kullanır.
     */
    fun boyutVeKatlaUygula(blok: Blok, g: View, kademe: Int, katli: Boolean) {
        runCatching {
            val px = (DuzenCekirdek.boyutNefesDp(kademe) *
                g.resources.displayMetrics.density).toInt()
            g.setPadding(g.paddingLeft, px, g.paddingRight, px)
            if (g !is ViewGroup || !blok.katlanabilir) return@runCatching
            if (katli) {
                // İlk katlamada mevcut görünürlükleri sakla — uygulamanın
                // kasıtlı gizlediği çocuklar açılırken zorla gösterilmez.
                if (!katlamaKaydi.containsKey(g)) {
                    katlamaKaydi[g] = IntArray(g.childCount) { i -> g.getChildAt(i).visibility }
                }
                for (i in 1 until g.childCount) {
                    g.getChildAt(i).visibility = View.GONE
                }
            } else {
                katlamaKaydi.remove(g)?.let { kayit ->
                    for (i in 1 until minOf(g.childCount, kayit.size)) {
                        g.getChildAt(i).visibility = kayit[i]
                    }
                }
            }
        }.onFailure { android.util.Log.w(TAG, "boyutVeKatlaUygula", it) }
    }


    /** Varsayılandan sapma var mı? (ayar satırında gösteriliyor) */
    fun ozellestirilmisMi(c: Context): Boolean {
        val d = p(c)
        return !(d.getString(K_SIRA, "") ?: "").isBlank() ||
            gizliler(c).isNotEmpty() ||
            !(d.getString(K_BOYUT, "") ?: "").isBlank() ||
            !(d.getStringSet(K_KATLI, emptySet()) ?: emptySet()).isEmpty()
    }

    // ------------------------------------------------------------------

    /**
     * Kayıtlı düzeni ana ekrana uygular.
     *
     * `HomeFragment.onViewCreated` ve düzen değişiminden sonra çağrılır.
     *
     * ── Neden removeView + addView ──
     * `LinearLayout`'ta çocuk sırasını değiştirmenin başka yolu yok.
     * Görünümler yok edilmiyor, yalnız yeniden yerleştiriliyor — içindeki
     * durum (dinleyiciler, metinler) korunuyor.
     */
    fun uygula(c: Context, kok: View?) {
        kok ?: return
        runCatching {
            val kap = kok.findViewById<View>(R.id.blokHero)?.parent as? LinearLayout ?: return
            val gizli = gizliler(c)

            // ── 🔴 v8.7 DÜZELTMESİ: sıralama algoritması yeniden yazıldı ──
            //
            // v8.5'teki yöntem hatalıydı:
            //     val suanki = kap.indexOfChild(g)
            //     if (suanki != hedefKonum) { removeViewAt(suanki); addView(g, hedefKonum) }
            //
            // `removeViewAt` çağrıldığı anda ONDAN SONRAKİ tüm çocukların
            // indeksi bir azalıyor. Döngünün ilerleyen adımlarında
            // `hedefKonum` artık gerçek konumu göstermiyordu ve bloklar
            // yanlış yerlere ekleniyordu. Kullanıcı sırayı değiştirdiğinde
            // ana ekran karışırdı.
            //
            // Doğru yöntem: önce TÜM blokları çıkar, sonra istenen sırayla
            // geri ekle. İndeks kaymasından etkilenmiyor ve niyeti
            // okumak da kolay.

            val siraliBloklar = sira(c)

            // 1) Blokları kaptan çıkar (görünümler yok edilmiyor,
            //    yalnız ebeveynden ayrılıyor — dinleyiciler korunuyor)
            val cikarilanlar = LinkedHashMap<String, View>()
            siraliBloklar.forEach { blok ->
                val g = kok.findViewById<View>(blok.viewId) ?: return@forEach
                if (kap.indexOfChild(g) >= 0) {
                    kap.removeView(g)
                    cikarilanlar[blok.kod] = g
                }
            }

            // 2) İstenen sırayla geri ekle.
            //    Selamlama başlığı 0. sırada kaldı; bloklar 1'den başlıyor.
            var konum = 1
            siraliBloklar.forEach { blok ->
                val g = cikarilanlar[blok.kod] ?: return@forEach
                g.visibility =
                    if (blok.kod in gizli && !blok.zorunlu) View.GONE else View.VISIBLE
                // v10.18: boyut nefesi + katlama da aynı geçişte işlenir
                boyutVeKatlaUygula(blok, g, boyutKademe(c, blok.kod), katliMi(c, blok.kod))
                kap.addView(g, konum.coerceAtMost(kap.childCount))
                konum++
            }
        }.onFailure { android.util.Log.w(TAG, "uygula", it) }
    }
}
