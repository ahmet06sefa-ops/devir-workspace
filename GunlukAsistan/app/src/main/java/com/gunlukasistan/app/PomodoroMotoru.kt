package com.gunlukasistan.app

import java.util.Locale

/**
 * v11.14 — Pomodoro / Odak-Verimlilik Motoru (SAF, JVM testli).
 *
 * Kullanıcının odak süresini pomodoro tekniğine çeviren saf hesaplama katmanı:
 *  1. [sureDonustur] — dakikayı "H:D:dk" insan-okunur biçeme çevirir.
 *  2. [verimlilikSkoru] — odak dakikası + tamamlanan görevden 0..100 skor üretir.
 *  3. [gunIcinOdakPlani] — kalan odak dakikasını pomodoro bloklarına böler.
 *  4. [molaOnerisi] — ardışık blok sayısına göre kısa/uzun mola önerir.
 *
 * Tamamen saf ve bağımlılıksız; Android context'i kullanmaz, JVM testlerine uygundur.
 */
object PomodoroMotoru {

    /** Standart odak bloğu uzunluğu (dakika). */
    const val BLOK_DK = 25

    /** Kısa mola (dakika). */
    const val KISA_MOLA_DK = 5

    /** Uzun mola (dakika) — 4 bloktan sonra. */
    const val UZUN_MOLA_DK = 20

    /** Kaç bloktan sonra uzun mola verilir. */
    const val UZUN_MOLA_BLOK = 4

    /** Dakikayı okunur saat:dk biçemine çevirir (örn. 75 → "1:15"). */
    fun sureDonustur(dakika: Int): String {
        val d = dakika.coerceAtLeast(0)
        val saat = d / 60
        val dk = d % 60
        return if (saat > 0) String.format(Locale.US, "%d:%02d", saat, dk) else "$dk dk"
    }

    /**
     * Odak dakikası ve tamamlanan görevden 0..100 verimlilik skoru üretir.
     * Günlük 120 dk odak ve 8 görev "tam verim" referans alınır.
     */
    fun verimlilikSkoru(odakDakika: Int, tamamlananGorev: Int): Int {
        val odakYuzde = (odakDakika.coerceAtLeast(0).toDouble() / 120.0).coerceAtMost(1.0) * 0.7
        val gorevYuzde = (tamamlananGorev.coerceAtLeast(0).toDouble() / 8.0).coerceAtMost(1.0) * 0.3
        return ((odakYuzde + gorevYuzde) * 100).toInt().coerceIn(0, 100)
    }

    /** Verimlilik skorunu yıldız cinsinden değerlendirir (0..5). */
    fun yildiz(skor: Int): Int = when {
        skor >= 90 -> 5
        skor >= 70 -> 4
        skor >= 50 -> 3
        skor >= 25 -> 2
        skor > 0 -> 1
        else -> 0
    }

    /** Verimlilik skoruna göre kısa bir yorum döndürür. */
    fun yorum(skor: Int): String = when {
        skor >= 90 -> "🔥 Muhteşem! Tam verimlilik seviyesindesin."
        skor >= 70 -> "💪 Güçlü bir gün geçiriyorsun, devam et."
        skor >= 50 -> "👌 Ortalamanın üzerindesin, biraz daha odakla."
        skor >= 25 -> "🌱 İyi başlangıç, odak süreni artırabilirsin."
        else -> "🌊 Bugün dinlenme günü olabilir; kendine zaman tanı."
    }

    /**
     * Kalan odak dakikasını kaç tam pomodoro bloğuna bölüneceğini hesaplar.
     */
    fun blokSayisi(odakDakika: Int): Int = (odakDakika.coerceAtLeast(0) / BLOK_DK)

    /**
     * Ardışık blok sayısına göre mola önerisi (dakika + tür).
     */
    fun molaOnerisi(ardisikBlok: Int): MolaOnerisi {
        val a = ardisikBlok.coerceAtLeast(1)
        val uzun = a % UZUN_MOLA_BLOK == 0
        return if (uzun) {
            MolaOnerisi(UZUN_MOLA_DK, "uzun", "4 blok tamamlandı — zihnini dinlendiren uzun bir mola al.")
        } else {
            MolaOnerisi(KISA_MOLA_DK, "kisa", "Kısa bir mola ile enerjini tazele.")
        }
    }

    /** Günün odak planını insan-okunur satırlara döker. */
    fun gunIcinOdakPlani(kalanDakika: Int): String {
        val k = kalanDakika.coerceAtLeast(0)
        val blok = blokSayisi(k)
        if (blok == 0) return "Bugün için yeterli odak süresi kalmamış (en az $BLOK_DK dk gerekli)."
        val sb = StringBuilder()
        sb.append("Bugün kalan $k dk = $blok odak bloğu:\n")
        for (i in 1..blok) {
            val mola = molaOnerisi(i)
            sb.append("  $i) $BLOK_DK dk odak → $mola.sure dk mola (${mola.tur})\n")
        }
        return sb.toString().trim()
    }

    data class MolaOnerisi(
        val sure: Int,
        val tur: String,
        val aciklama: String
    )
}
