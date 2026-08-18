package com.gunlukasistan.app

/**
 * v11.13 — Sosyal / arkadaş meydan okuma motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde (Habitica/Habitify) olup bende olmayan sosyal
 * / arkadaş yarışması ekle." Gerçek sunucu yoktur; bu motor, grup meydan
 * okumasının karar mantığını taşır (üye puanları, sıralama, kazanan, hedef).
 *
 *  · [Uye] — ad + tamamlama sayısı + puan.
 *  · [MeydanOkuma] — ad + gün sayısı + üyeler + hedef.
 *  · [sirala] — üyeleri puana göre sıralar.
 *  · [kazanan] — en yüksek puana sahip üyeyi döndürür.
 *  · [hedefOzet] — grup hedefine toplam ilerlemeyi verir.
 */
object SosyalMeydanOkumaMotoru {

    data class Uye(val ad: String, val tamamlama: Int, val bonusPuan: Int = 0) {
        val puan: Int get() = tamamlama * 10 + bonusPuan
    }

    data class MeydanOkuma(
        val ad: String,
        val gunSayisi: Int,
        val hedefTamamlama: Int,
        val uyeler: List<Uye>
    )

    /** Üyeleri puana göre azalan sıralar. */
    fun sirala(uyeler: List<Uye>): List<Uye> = uyeler.sortedByDescending { it.puan }

    /** En yüksek puana sahip üyeyi döndürür; boşsa null. */
    fun kazanan(uyeler: List<Uye>): Uye? = uyeler.maxByOrNull { it.puan }

    /** Grubun hedefe toplam ilerlemesi (0..100). */
    fun hedefOzet(hedefTamamlama: Int, uyeler: List<Uye>): Int {
        if (hedefTamamlama <= 0) return 0
        val toplam = uyeler.sumOf { it.tamamlama }
        return (toplam * 100 / hedefTamamlama).coerceIn(0, 100)
    }

    /** Meydan okuma durumu metni (ör. liderlik tablosu için). */
    fun durumMetni(m: MeydanOkuma): String = buildString {
        append("🏆 ").append(m.ad).append(" (").append(m.gunSayisi).append(" gün)\n")
        append("Grup hedefe %").append(hedefOzet(m.hedefTamamlama, m.uyeler)).append(" ulaştı.\n")
        val s = sirala(m.uyeler)
        s.forEachIndexed { i, u ->
            append("${i + 1}. ").append(u.ad).append(" — ").append(u.tamamlama)
                .append(" tamamlama · ").append(u.puan).append(" puan\n")
        }
        kazanan(s)?.let { append("\n🥇 Lider: ").append(it.ad) }
    }
}
