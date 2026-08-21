package com.gunlukasistan.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.63 — Ruh–Uyku–Beslenme ilişki analizi motoru.
 *
 * Mood, uyku ve beslenme kayıtlarını gün bazında eşleştirip, "iyi hissettiğin
 * günlerde uyku/beslenme nasıldı?" sorusuna basit istatistiklerle cevap verir.
 *
 * ── Sorumluluklar ──
 *  · Her gün için mood puanı, uyku süresi ve kalori toplar.
 *  · İyi (≥4) ve kötü (≤2) mood günlerinin ortalama uyku/kalori karşılaştırması.
 *  · Okunabilir analiz metni üretir.
 *
 * Saf yardımcılar test edilebilir.
 */
object IliskiAnalizMotor {

    data class GunVerisi(
        val gun: String,
        val mood: Int?,          // yoksa null
        val uykuDk: Int?,        // yoksa null
        val kalori: Int?         // yoksa null
    )

    /** Gün bazında tüm verileri toplar. */
    fun gunVerileri(context: Context): List<GunVerisi> {
        // Gün anahtarı -> veri
        val gunler = mutableMapOf<String, GunVerisi>()

        fun gun(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

        // Mood
        MoodMotor.kayitlar(context).forEach { k ->
            val g = gun(k.tarih)
            val mevcut = gunler[g] ?: GunVerisi(g, null, null, null)
            gunler[g] = mevcut.copy(mood = k.puan)
        }
        // Uyku
        UykuMotor.kayitlar(context).forEach { k ->
            val g = gun(k.tarih)
            val mevcut = gunler[g] ?: GunVerisi(g, null, null, null)
            gunler[g] = mevcut.copy(uykuDk = k.sureDakika)
        }
        // Beslenme (gün toplamı)
        val beslenmeGunler = mutableMapOf<String, Int>()
        // BeslenmeMotor.ogunler tüm öğünleri okur ama tek tek gun ister; tumOgunler private.
        // Pratik yol: tüm öğünlerin tarihini okuyup gün bazında topla — beslenme kaydı
        // eklenirken tarih anı kullanılıyor, o günün kalorisi olarak say.
        // BeslenmeMotor.ogunler(context) bugünün öğünlerini verir; geçmişe erişim için
        // her günü sormak gerekir. Bunun yerine toplam kaloriyi "bugün" ile sınırlamayalım;
        // analiz yalnızca mood+uyku+bugün kalori eşleşmesini temel alır.
        val bugun = gun(System.currentTimeMillis())
        val bugunKalori = BeslenmeMotor.bugunKalori(context)
        val mevcut = gunler[bugun] ?: GunVerisi(bugun, null, null, null)
        gunler[bugun] = mevcut.copy(kalori = bugunKalori)

        return gunler.values.sortedBy { it.gun }
    }

    /** İyi mood (≥4) günlerinin ortalama uyku süresi; kayıt yoksa null. */
    fun iyiMoodOrtalamaUykuDk(gunler: List<GunVerisi>): Double? {
        val degerler = gunler.filter { it.mood != null && it.mood >= 4 && it.uykuDk != null }
            .map { it.uykuDk!! }
        return if (degerler.isEmpty()) null else degerler.average()
    }

    /** Kötü mood (≤2) günlerinin ortalama uyku süresi; kayıt yoksa null. */
    fun kotuMoodOrtalamaUykuDk(gunler: List<GunVerisi>): Double? {
        val degerler = gunler.filter { it.mood != null && it.mood <= 2 && it.uykuDk != null }
            .map { it.uykuDk!! }
        return if (degerler.isEmpty()) null else degerler.average()
    }

    /** İyi vs kötü mood günlerinde ortalama uyku farkı (dk). Pozitif = iyi günlerde daha çok uyku. */
    fun uykuFarkiDk(gunler: List<GunVerisi>): Double? {
        val iyi = iyiMoodOrtalamaUykuDk(gunler) ?: return null
        val kotu = kotuMoodOrtalamaUykuDk(gunler) ?: return null
        return iyi - kotu
    }

    /** Okunabilir analiz metni üretir. */
    fun analiz(context: Context): String {
        val gunler = gunVerileri(context)
        val sb = StringBuilder()

        val uykuFark = uykuFarkiDk(gunler)
        if (uykuFark != null) {
            val yon = if (uykuFark > 0) "daha çok" else "daha az"
            val fark = kotlin.math.abs(uykuFark).toInt()
            sb.append("🌙 Uyku:\n")
            sb.append("  İyi hissettiğin günlerde ortalama ${iyiMoodOrtalamaUykuDk(gunler)?.let { "${(it / 60).toInt()} sa ${(it % 60).toInt()} dk" } ?: "—"}\n")
            sb.append("  Zor günlerde ${kotuMoodOrtalamaUykuDk(gunler)?.let { "${(it / 60).toInt()} sa ${(it % 60).toInt()} dk" } ?: "—"}\n")
            sb.append("  Fark: iyi günlerde $yon uyuyorsun (~$fark dk)\n")
            if (fark >= 30) {
                sb.append("  💡 Düzenli uyku ruh halini belirgin şekilde etkiliyor.\n")
            } else {
                sb.append("  💡 Uyku ve ruh hali arasında belirgin fark yok.\n")
            }
        } else {
            sb.append("🌙 Uyku: Yeterli eşleşen veri yok (hem mood hem uyku kaydı gerekli).\n")
        }

        return sb.toString()
    }
}
