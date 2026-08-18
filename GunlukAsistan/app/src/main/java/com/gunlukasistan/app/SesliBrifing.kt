package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.54 — Sesli "Gündem & Vakit Brifingi" saf mantık motoru.
 * Günaydın veya gün içi özetini seslendirilmeye hazır doğal dille formatlar.
 */
object SesliBrifing {

    /**
     * Günün brifing metnini doğal dille oluşturur (TTS uyumlu).
     */
    fun brifingMetniUret(
        vakitAd: String,
        kalanGorevSayisi: Int,
        odakDk: Int,
        seri: Int,
        sabahMi: Boolean
    ): String {
        val vAd = vakitAd.ifBlank { "Öğle" }
        val gSayi = kalanGorevSayisi.coerceAtLeast(0)
        val dk = odakDk.coerceAtLeast(0)
        val sr = seri.coerceAtLeast(0)

        if (sabahMi) {
            val seriMetin = if (sr > 0) " ve $sr günlük aktif seriniz var" else ""
            return "Günaydın! Sıradaki namaz vakti $vAd. Bugün bekleyen $gSayi göreviniz$seriMetin. Hayırlı ve verimli bir gün dilerim."
        }
        return "Günün özeti: Bugün toplam $dk dakika odaklandınız ve geride $gSayi görev kaldı. Sıradaki vakit $vAd. Devam edin!"
    }

    /**
     * Brifing kartı başlığını saata göre belirler.
     */
    fun brifingBasligi(sabahMi: Boolean): String {
        return if (sabahMi) "🌅 Sabah Günaydın & Vakit Brifingi" else "☀️ Gün İçi Odak & Gündem Özeti"
    }

    /**
     * Günün verimlilik skorunu 0..100 arasında hesaplar.
     */
    fun ozetSkorHesapla(kalanGorev: Int, odakDk: Int, hedefDk: Int): Int {
        val hedef = hedefDk.coerceAtLeast(25)
        val odakPuan = ((odakDk.toDouble() / hedef) * 70.0).toInt().coerceIn(0, 70)
        val gorevPuan = if (kalanGorev == 0) 30 else (30 - (kalanGorev * 5)).coerceAtLeast(5)
        return (odakPuan + gorevPuan).coerceIn(0, 100)
    }
}
