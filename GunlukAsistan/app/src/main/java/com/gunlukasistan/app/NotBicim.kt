package com.gunlukasistan.app

/**
 * v10.31 · Katalog #24 — markdown-benzeri hafif biçim çözümleyicisi
 * (saf, JVM testli). Üretilen parça listesi Android tarafında Spannable'a
 * çevrilir; burada android bağımlılığı yoktur.
 *
 * Kurallar:
 *  · Satır başı "# " → satırın tamamı BAŞLIK
 *  · `**…**` aralığı → KALIN (kapanmayan çiftler değiştirilmez)
 *  · Boş kalın ("****") yoksayılır
 *  · Liste işaretleri ("- ") v10.29'da zaten görevlere çevrilebiliyor;
 *    gösterimde olduğu gibi bırakılır (duz metin).
 */
object NotBicim {

    enum class Tip { DUZ, KALIN, BASLIK }

    data class Parca(val tip: Tip, val metin: String)

    fun cozumle(metin: String): List<Parca> {
        val out = mutableListOf<Parca>()
        metin.split("\n").forEachIndexed { idx, hamSatir ->
            if (idx > 0) out += Parca(Tip.DUZ, "\n")
            val baslik = hamSatir.startsWith("# ")
            val satir = if (baslik) hamSatir.substring(2) else hamSatir
            val duzTip = if (baslik) Tip.BASLIK else Tip.DUZ
            var i = 0
            var duz = StringBuilder()
            fun duzKapa() {
                if (duz.isNotEmpty()) {
                    out += Parca(duzTip, duz.toString())
                    duz = StringBuilder()
                }
            }
            while (i < satir.length) {
                val j = satir.indexOf("**", i)
                if (j < 0) { duz.append(satir.substring(i)); break }
                val k = satir.indexOf("**", j + 2)
                if (k < 0) { duz.append(satir.substring(i)); break } // kapanmamış: olduğu gibi
                duz.append(satir.substring(i, j))
                if (k > j + 2) {
                    duzKapa()
                    out += Parca(Tip.KALIN, satir.substring(j + 2, k))
                } // boş kalın yoksay
                i = k + 2
            }
            duzKapa()
        }
        return out
    }

    /** Katalog #36 — okunma süresi tahmini: 200 kelime/dk, en az 1 dk. */
    fun okumaDk(metin: String): Int {
        val k = NotOlcum.kelimeS(metin)
        return if (k == 0) 0 else maxOf(1, (k + 199) / 200)
    }
}
