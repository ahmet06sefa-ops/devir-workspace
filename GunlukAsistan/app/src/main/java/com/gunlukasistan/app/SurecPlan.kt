package com.gunlukasistan.app

import java.util.Calendar

/**
 * v10.28 · Katalog #62 — İleri Sayım oturum kaydı (saf, JVM testli).
 *
 * Sayaç her duraklatıldığında / sıfırlandığında toplanan dakika bu
 * planlayıcıdan geçer: gün sınırını aşan oturumlar iki güne bölünür,
 * 35 saatten eski kayıtlar budanır ve günlük toplamlar hesaplanır.
 *
 * Android bağımlılığı yoktur; [TimerFragment] kayıtları
 * `ileri_sayim_gecmis_v1` tercih dosyasında JSON dizisi olarak tutar:
 * `[{"bitis":1234,"dk":25}, ...]`
 */
object SurecPlan {

    /** Oturumun gün sonuna kadar tutulduğu pencere (35 saat). */
    const val BUDAMA_MS = 35L * 60 * 60 * 1000
    const val KAYIT_SINIR = 200

    data class Oturum(val bitisMs: Long, val dakika: Int)

    // ---------- saf çekirdek ----------

    /**
     * Oturumu kayda işler; gün sınırını aşıyorsa ikiye böler
     * (kalan dakika 23:59'a, taşan 00:00 sonrasına).
     * Liste başa eklenir (yeniler üstte), [KAYIT_SINIR] ile budanır.
     */
    fun kayitEkle(liste: List<Oturum>, bitisMs: Long, dakika: Int, simdi: Long): List<Oturum> {
        val temiz = buda(liste, simdi)
        if (dakika <= 0) return temiz
        val parcalar = gunlereParcala(bitisMs, dakika)
        return (parcalar + temiz).take(KAYIT_SINIR)
    }

    /** [BUDAMA_MS] penceresinden eski kayıtları atar. */
    fun buda(liste: List<Oturum>, simdi: Long): List<Oturum> =
        liste.filter { simdi - it.bitisMs <= BUDAMA_MS }

    /**
     * Oturum süresini takvim günlerine böler.
     * Örnek: 23:50'de biten 30 dk → (dün 23:59 · 10 dk) + (bugün · 20 dk).
     */
    fun gunlereParcala(bitisMs: Long, dakika: Int): List<Oturum> {
        if (dakika <= 0) return emptyList()
        val baslangic = bitisMs - dakika * 60_000L
        val sinir = gunBaslangici(bitisMs) // bitiş gününün 00:00'ı
        return if (baslangic >= sinir) {
            listOf(Oturum(bitisMs, dakika))
        } else {
            // Bugüne düşen: 00:00'dan bitişe kadar
            val bugunDk = ((bitisMs - sinir) / 60_000L).toInt()
            val dunDk = dakika - bugunDk
            val dun = Oturum(sinir - 60_000L, dunDk) // dün 23:59'a yazılır
            val bugun = if (bugunDk > 0) Oturum(bitisMs, bugunDk) else null
            listOfNotNull(bugun, dun)
        }
    }

    /** Bugünün (cihaz takvimi) toplam dakikası. */
    fun bugunToplam(liste: List<Oturum>, simdi: Long): Int =
        liste.filter { it.bitisMs >= gunBaslangici(simdi) }.sumOf { it.dakika }

    /** Dünün toplam dakikası. */
    fun dunToplam(liste: List<Oturum>, simdi: Long): Int {
        val bugunBas = gunBaslangici(simdi)
        val dunBas = bugunBas - 24L * 60 * 60 * 1000
        return liste.filter { it.bitisMs in dunBas until bugunBas }.sumOf { it.dakika }
    }

    /** Verilen milisaniyenin ait olduğu günün 00:00:00.000 zamanı. */
    fun gunBaslangici(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // ---------- JSON köprüsü (tercih katmanı bunu kullanır) ----------

    fun jsonaYaz(liste: List<Oturum>): String {
        val sb = StringBuilder("[")
        liste.forEachIndexed { i, o ->
            if (i > 0) sb.append(',')
            sb.append("{\"bitis\":").append(o.bitisMs).append(",\"dk\":").append(o.dakika).append('}')
        }
        return sb.append(']').toString()
    }

    fun jsondanOku(json: String?): List<Oturum> {
        if (json.isNullOrBlank()) return emptyList()
        val out = mutableListOf<Oturum>()
        // Küçük ve bizim ürettiğimiz biçim; kırılgansa sessizce atlanır.
        val parca = Regex("\\{\\s*\"bitis\"\\s*:\\s*(\\d+)\\s*,\\s*\"dk\"\\s*:\\s*(\\d+)\\s*\\}")
        for (m in parca.findAll(json)) {
            val bitis = m.groupValues[1].toLongOrNull() ?: continue
            val dk = m.groupValues[2].toIntOrNull() ?: continue
            if (dk > 0) out.add(Oturum(bitis, dk))
        }
        return out
    }
}
