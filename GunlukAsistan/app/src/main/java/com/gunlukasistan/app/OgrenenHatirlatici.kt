package com.gunlukasistan.app

import android.content.Context

/**
 * v10.15 · ULTRA-30 / C16 — Öğrenen hatırlatıcının saf çekirdeği.
 *
 * ── Tarama kanıtı ──
 * Tekrar altyapısı v7.70'ten beri var (`Tekrar.gorevYenile`) ama vade
 * saati hep kullanıcının yazdığı saatte kalıyordu; "yapıldı" basma
 * SAATİ hiçbir yerde kaydedilmiyordu (`completedAt` alanı yok).
 *
 * ── Model ──
 * Her "yapıldı" basışında o anın gün-içi dakikası görev başına tutulur
 * (son 8 kayıt). Bir sonraki tekrar planlanırken kayıtların DAİRESEL
 * ortalaması alınır (23:50 + 00:10 sarmasını doğru işler) ve vade
 * kullanıcının saatinden en fazla ±45 dk kaydırılır. Şeffaflık:
 * bildirim genişletilmiş metni "Bu saati son N tamamlamanın
 * ortalamasından öğrendim" diye açıklar.
 *
 * ── Güven sınırları ──
 * · En az 3 kayıt yoksa kaydırma YOK (tek sefer rastlantı olabilir).
 * · Kayıtların dairesel sapması 150 dk'dan büyükse davranış düzensiz
 *   demektir — kaydırma YOK.
 * · Kullanıcı vadeyi elle yeniden kurarsa (öneriden saparsa) sayaç
 *   sıfırlanır: elle müdahale otoritedir.
 *
 * Saf fonksiyonlar birim testlidir.
 */
object OgrenenHatirlatici {

    const val MAKS_KAYIT: Int = 8
    const val MIN_KAYIT: Int = 3
    const val MAKS_KAYDIRIM_DK: Int = 45
    const val MAKS_SAPMA_DK: Double = 150.0

    /**
     * Dairesel ortalama (dakika, 0..1439). Boş listede -1.
     * Her dakika birim çemberde bir vektöre çevrilir; ortalama vektörün
     * açısı sonucu verir — 23:50 ile 00:10 ortalaması 24:00/00:00 çıkar,
     * doğrusal ortalamanın 12:00 saçmalığı yerine.
     */
    fun daireselOrtalama(dakikalar: List<Int>): Int {
        if (dakikalar.isEmpty()) return -1
        var sx = 0.0; var sy = 0.0
        for (d in dakikalar) {
            val r = Math.toRadians(d * 360.0 / 1440.0)
            sx += kotlin.math.cos(r); sy += kotlin.math.sin(r)
        }
        var aci = Math.toDegrees(kotlin.math.atan2(sy, sx))
        if (aci < 0) aci += 360.0
        return ((aci * 1440.0 / 360.0) + 0.5).toInt() % 1440
    }

    /**
     * Dairesel dağılımın büyüklüğü (0..1): 1 = tüm kayıtlar aynı saatte,
     * 0 = tamamen dağınık. Sapma kararı bunun üzerinden verilir.
     */
    fun tutarlilik(dakikalar: List<Int>): Double {
        if (dakikalar.isEmpty()) return 0.0
        var sx = 0.0; var sy = 0.0
        for (d in dakikalar) {
            val r = Math.toRadians(d * 360.0 / 1440.0)
            sx += kotlin.math.cos(r); sy += kotlin.math.sin(r)
        }
        return kotlin.math.hypot(sx, sy) / dakikalar.size
    }

    /** Ortalamanın etrafındaki yaklaşık sapma (dakika). */
    fun sapmaDk(tutarlilik: Double): Double =
        Math.toDegrees(kotlin.math.sqrt(-2.0 * kotlin.math.ln(tutarlilik.coerceIn(1e-9, 1.0)))) * 1440.0 / 360.0

    /** Kaydırma kararı: yeterli ve düzenli veri var mı? */
    fun kaydirmaliMi(dakikalar: List<Int>): Boolean =
        dakikalar.size >= MIN_KAYIT && sapmaDk(tutarlilik(dakikalar)) <= MAKS_SAPMA_DK

    /**
     * Dakika cinsinden kelepçeli kaydırım: ortalamayı kullanıcının
     * hedefine ±[MAKS_KAYDIRIM_DK] penceresinde yaklaştırır.
     * Gece yarısı sarmasına karşı en kısa dairesel fark kullanılır.
     */
    fun kaydirimDk(ortalama: Int, hedef: Int): Int {
        var fark = ((ortalama - hedef + 720) % 1440) - 720
        if (fark < -720) fark += 1440
        return fark.coerceIn(-MAKS_KAYDIRIM_DK, MAKS_KAYDIRIM_DK)
    }

    /** "Neden bu saat?" metninin verisi: (kayit adedi, kaydirim dk). */
    data class Karar(val adet: Int, val kaydirim: Int, val uygulandi: Boolean)

    fun kararVer(dakikalar: List<Int>, hedefDk: Int): Karar {
        if (!kaydirmaliMi(dakikalar)) return Karar(dakikalar.size, 0, false)
        val ort = daireselOrtalama(dakikalar)
        val k = kaydirimDk(ort, hedefDk)
        return Karar(dakikalar.size, k, k != 0)
    }

    // ── Pref köprüsü ────────────────────────────────────────────────

    private const val PREF = "ogrenen_hatirlatici_v1"

    /** "✓ Yapıldı" basıldı — o anın gün-içi dakikasını kaydet (son 8). */
    fun kaydet(context: Context, gorevId: Long, simdiMs: Long) {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = simdiMs }
        val dk = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
        val sp = context.getSharedPreferences(PREF, 0)
        val k = "k_$gorevId"
        val eski = sp.getString(k, "") ?: ""
        val liste = (if (eski.isBlank()) mutableListOf() else eski.split(",").mapNotNull { it.toIntOrNull() }.toMutableList())
        liste.add(0, dk)
        while (liste.size > MAKS_KAYIT) liste.removeAt(liste.size - 1)
        sp.edit().putString(k, liste.joinToString(",")).apply()
    }

    fun kayitlar(context: Context, gorevId: Long): List<Int> {
        val s = context.getSharedPreferences(PREF, 0).getString("k_$gorevId", "") ?: ""
        return if (s.isBlank()) emptyList() else s.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun sifirla(context: Context, gorevId: Long) {
        context.getSharedPreferences(PREF, 0).edit().remove("k_$gorevId").apply()
    }

    /**
     * Tekrar yenileme kancası (`Tekrar.gorevYenile` içinden çağrılır):
     * öğrenilen kaydırımı yeni vadeye uygular, uygulanan karar metnini
     * (bildirim açıklaması için) pref'e yazar. Elle kurulum tespiti
     * çağıranın sorumluluğundadır (bkz. sınıf başlığı).
     */
    fun uygula(context: Context, gorevId: Long, yeniVadeMs: Long): Long {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = yeniVadeMs }
        val hedefDk = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
        val karar = kararVer(kayitlar(context, gorevId), hedefDk)
        val sp = context.getSharedPreferences(PREF, 0)
        if (!karar.uygulandi) {
            sp.edit().remove("neden_$gorevId").apply()
            return yeniVadeMs
        }
        sp.edit().putInt("neden_$gorevId", (karar.adet shl 16) or (karar.kaydirim and 0xFFFF)).apply()
        return yeniVadeMs + karar.kaydirim * 60_000L
    }

    /** Bildirim açıklaması için son uygulanan karar: (adet, kaydirim)? */
    fun neden(context: Context, gorevId: Long): Pair<Int, Int>? {
        val v = context.getSharedPreferences(PREF, 0).getInt("neden_$gorevId", 0)
        if (v == 0) return null
        var k = v and 0xFFFF
        if (k >= 0x8000) k -= 0x10000
        return (v ushr 16) to k
    }
}
