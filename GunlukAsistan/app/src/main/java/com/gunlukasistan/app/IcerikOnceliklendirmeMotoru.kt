package com.gunlukasistan.app

/**
 * v11.14 — İçerik Önceliklendirme Motoru (Eisenhower Matrisi, SAF, JVM testli).
 *
 * Görevleri "önem" ve "aciliyet" boyutlarına göre dört kadrana ayırır ve
 * öncelik sırası üretir. Klasik Eisenhower tekniğinin saf mantık katmanı:
 *  1. [kadran] — önem/aciliyet → 4 kadran sınıfı.
 *  2. [oncelikPuani] — önem + aciliyet puanından 0..100 öncelik puanı.
 *  3. [sirala] — görev listesini önceliğe göre sıralar.
 *  4. [okunur] — görevlerin okunur öncelik listesini üretir.
 *
 * Saf ve bağımlılıksız; JVM testlerine uygundur.
 */
object IcerikOnceliklendirmeMotoru {

    /** Eisenhower kadran sınıfları. */
    enum class Kadran(val etiket: String) {
        ONEMLI_ACIL("Önemli + Acil — Hemen Yap"),
        ONEMLI_ACIL_DEGIL("Önemli + Acil Değil — Planla"),
        ONEMLI_DEGIL_ACIL("Önemsiz + Acil — Devret"),
        ONEMLI_DEGIL_ACIL_DEGIL("Önemsiz + Acil Değil — Ertela/El")
    }

    data class Gorev(val ad: String, val onem: Int = 5, val aciliyet: Int = 5) {
        val kadran: Kadran get() = kadran(onem, aciliyet)
        val puan: Int get() = oncelikPuani(onem, aciliyet)
    }

    /** Önem/aciliyet (0..10) → Eisenhower kadranı. */
    fun kadran(onem: Int, aciliyet: Int): Kadran {
        val o = onem.coerceIn(0, 10) >= 6
        val a = aciliyet.coerceIn(0, 10) >= 6
        return when {
            o && a -> Kadran.ONEMLI_ACIL
            o && !a -> Kadran.ONEMLI_ACIL_DEGIL
            !o && a -> Kadran.ONEMLI_DEGIL_ACIL
            else -> Kadran.ONEMLI_DEGIL_ACIL_DEGIL
        }
    }

    /** 0..100 öncelik puanı (önem %60, aciliyet %40 ağırlıklı). */
    fun oncelikPuani(onem: Int, aciliyet: Int): Int {
        val o = onem.coerceIn(0, 10)
        val a = aciliyet.coerceIn(0, 10)
        return (o * 10 * 0.6 + a * 10 * 0.4).toInt().coerceIn(0, 100)
    }

    /** Görev listesini öncelik puanına göre azalan sıralar. */
    fun sirala(gorevler: List<Gorev>): List<Gorev> =
        gorevler.sortedByDescending { it.puan }

    /** Önce "önemli acil" olanları öne alarak sıralar (matris önceliği). */
    fun matrisSiralama(gorevler: List<Gorev>): List<Gorev> {
        // Kadran enum sırası zaten öncelik sırasıdır (0 = en öncelikli).
        // Artan index = daha öncelikli, bu yüzden compareBy kullanılır.
        val oncelikSira = Kadran.values().toList()
        return gorevler.sortedWith(compareBy<Gorev> { oncelikSira.indexOf(it.kadran) }
            .thenByDescending { it.puan })
    }

    /** Okunur öncelik listesi metni üretir. */
    fun okunur(gorevler: List<Gorev>): String {
        val siralanmis = matrisSiralama(gorevler)
        if (siralanmis.isEmpty()) return "Önceliklendirilecek görev yok."
        val sb = StringBuilder()
        sb.append("📋 Öncelik sırası:\n")
        siralanmis.forEachIndexed { i, g ->
            sb.append("  ${i + 1}) ${g.ad} — ${g.kadran.etiket} (${g.puan}/100)\n")
        }
        return sb.toString().trim()
    }

    /** Tek görevi kısa okunur tavsiye biçeminde döndürür. */
    fun tekGorevTavsiyesi(g: Gorev): String =
        "「${g.ad}」 → ${g.kadran.etiket} · öncelik ${g.puan}/100"
}
