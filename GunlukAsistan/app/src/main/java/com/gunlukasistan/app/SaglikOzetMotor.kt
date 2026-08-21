package com.gunlukasistan.app

import android.content.Context

/**
 * v11.56 — Sağlık & fitness özet motoru.
 *
 * Kas Sistemi, Beslenme, Uyku ve Bütçe modüllerindeki veriyi tek bir
 * "sağlık karnesi" görünümünde özetlemek için okuma yardımcıları.
 */
object SaglikOzetMotor {

    data class KarnedeSatir(
        val emoji: String,
        val baslik: String,
        val detay: String
    )

    /**
     * Tüm modüllerden özet satırları toplar.
     * Her satır bir modülün "bugün" özetini verir; veri yoksa yönlendirir.
     */
    fun karnesi(context: Context): List<KarnedeSatir> {
        val satirlar = mutableListOf<KarnedeSatir>()

        // Kas Sistemi
        val kasBugun = FitnessMotor.bugunToplamSet(context)
        val kasToplam = FitnessMotor.antrenmanlar(context).size
        satirlar.add(
            if (kasToplam == 0) KarnedeSatir("💪", "Kas Sistemi", "Henüz antrenman kaydın yok")
            else KarnedeSatir("💪", "Kas Sistemi", "Bugün $kasBugun set · Toplam $kasToplam kayıt")
        )

        // Beslenme
        val kalori = BeslenmeMotor.bugunKalori(context)
        val hedef = BeslenmeMotor.kaloriHedefi(context)
        val kalan = hedef - kalori
        satirlar.add(KarnedeSatir("🍽️", "Beslenme", "$kalori / $hedef kcal" +
            (if (kalan >= 0) " ($kalan kaldı)" else " (${-kalan} fazla)")))

        // Su
        val su = BeslenmeMotor.suBardak(context)
        satirlar.add(KarnedeSatir("💧", "Su", "$su bardak (${su * 250} ml)"))

        // Uyku
        val uykuSayisi = UykuMotor.son7GunKayitSayisi(context)
        if (uykuSayisi == 0) {
            satirlar.add(KarnedeSatir("😴", "Uyku", "Son 7 günde kayıt yok"))
        } else {
            val ort = UykuMotor.son7GunOrtalamaDakika(context).toInt()
            val kalite = UykuMotor.son7GunOrtalamaKalite(context)
            satirlar.add(KarnedeSatir("😴", "Uyku", "Ort ${UykuMotor.sureMetni(ort)} · Kalite ${"%.1f".format(kalite)}/5"))
        }

        // Bütçe
        val gider = runCatching {
            Butce.ayOzeti(context)?.gider ?: 0.0
        }.getOrDefault(0.0)
        satirlar.add(KarnedeSatir("💰", "Bu ay harcama", "${"%.0f".format(gider)} TL"))

        return satirlar
    }
}
