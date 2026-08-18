package com.gunlukasistan.app

/**
 * v10.12 · ULTRA-30 / D19 — Nefes stüdyosunun saf (framework'süz) planı.
 *
 * ── Neden ayrı nesne ──
 * Faz takvimi (kaç saniye al/tut/ver) ve halkanın ne kadar büyüyeceği
 * tamamen matematik. Android'siz test edilebilmeleri için bu dosyada
 * hiçbir framework çağrısı yok; [NefesView] yalnızca sonucu çizer.
 *
 * ── Desenler ──
 *   · 4-7-8: uykuya geçiş için klasik sakinleşme ritmi
 *   · Kutu (4-4-4-4): odak öncesi dengeli ritim
 *   · Sakin (4-6): nefes tutmakta zorlananlar için yumuşak sürüm
 *
 * ── Halka ölçeği ──
 * Al fazında 0.55 → 1.00 büyür, ver fazında 1.00 → 0.55 küçülür.
 * Tut/Bekle fazları sabit durur: al sonrası tutmada 1.00, ver sonrası
 * beklemede 0.55. Böylece halkanın hareketi her zaman sürekli olur;
 * faz geçişinde zıplama olmaz.
 */
object NefesProgrami {

    enum class Tip { AL, TUT, VER, BOS }

    data class Faz(val tip: Tip, val sn: Int)

    data class Desen(
        val id: Int,
        val adRes: Int,
        val emoji: String,
        val fazlar: List<Faz>
    ) {
        /** Bir tam döngünün süresi (sn). */
        val donguSn: Int get() = fazlar.sumOf { it.sn }
    }

    const val DESEN_478 = 0
    const val DESEN_KUTU = 1
    const val DESEN_SAKIN = 2

    /** En kısa faz 4 sn — daha altı ritmi bozuyor (kalp atışına benzemiyor). */
    fun desenler(): List<Desen> = listOf(
        Desen(
            DESEN_478, R.string.fo_nefes_478, "🌙",
            listOf(Faz(Tip.AL, 4), Faz(Tip.TUT, 7), Faz(Tip.VER, 8))
        ),
        Desen(
            DESEN_KUTU, R.string.fo_nefes_kutu, "📦",
            listOf(Faz(Tip.AL, 4), Faz(Tip.TUT, 4), Faz(Tip.VER, 4), Faz(Tip.BOS, 4))
        ),
        Desen(
            DESEN_SAKIN, R.string.fo_nefes_sakin, "🍃",
            listOf(Faz(Tip.AL, 4), Faz(Tip.VER, 6))
        )
    )

    fun desen(id: Int): Desen = desenler().firstOrNull { it.id == id } ?: desenler().first()

    /**
     * Döngü içi saniyeden (faz indeksi, faz içi ilerleme) üretir.
     *
     * `oran` her zaman 0..1 aralığında döner; döngü sonunun tam bitiminde
     * son faz (1.0) raporlanır — çağıran döngüyü sarmakla yükümlüdür.
     */
    fun fazBul(d: Desen, donguIciSn: Double): Pair<Int, Double> {
        if (d.donguSn <= 0) return 0 to 0.0
        val sn = donguIciSn.coerceAtLeast(0.0)
        var biriken = 0.0
        d.fazlar.forEachIndexed { i, faz ->
            val son = biriken + faz.sn
            if (sn < son) {
                return i to ((sn - biriken) / faz.sn).coerceIn(0.0, 1.0)
            }
            biriken = son
        }
        return d.fazlar.lastIndex to 1.0
    }

    /**
     * Halkanın dolgu ölçeği (0.55..1.00).
     *
     * Tut fazının değeri **bir önceki faza** bakar: nefes aldıktan sonra
     * tutulan hava dolu (1.0), nefes verdikten sonraki bekleme boş (0.55).
     */
    fun olcek(d: Desen, fazIndex: Int, oran: Double): Float {
        val faz = d.fazlar.getOrNull(fazIndex) ?: return 0.55f
        val t = oran.coerceIn(0.0, 1.0).toFloat()
        return when (faz.tip) {
            Tip.AL -> 0.55f + 0.45f * t
            Tip.VER -> 1.0f - 0.45f * t
            Tip.TUT -> {
                val onceki = d.fazlar.getOrNull(fazIndex - 1)?.tip
                if (onceki == Tip.AL) 1.0f else 0.55f
            }
            Tip.BOS -> 0.55f
        }
    }

    fun tipAdRes(t: Tip): Int = when (t) {
        Tip.AL -> R.string.fo_nefes_al
        Tip.TUT -> R.string.fo_nefes_tut
        Tip.VER -> R.string.fo_nefes_ver
        Tip.BOS -> R.string.fo_nefes_bos
    }
}
