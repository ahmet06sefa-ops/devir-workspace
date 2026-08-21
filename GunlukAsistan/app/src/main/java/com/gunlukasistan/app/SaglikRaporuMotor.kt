package com.gunlukasistan.app

import android.content.Context

/**
 * v11.58 — Haftalık sağlık raporu motoru.
 *
 * Kas Sistemi, Beslenme, Uyku, Ruh Hali ve Bütçe modüllerinden son 7 günlük
 * veriyi özetleyen, paylaşılabilir bir rapor metni üretir. Saf metotlar
 * test edilebilir.
 */
object SaglikRaporuMotor {

    /** Raporun ana başlığı. */
    fun baslik(): String = "📋 Günlük Asistan — Haftalık Sağlık Raporu"

    /**
     * Son 7 gün verilerini özetleyen rapor metnini üretir.
     * @return Rapor metni; veri yoksa ilgili satırlar "kayıt yok" der.
     */
    fun rapor(context: Context): String {
        val sb = StringBuilder()
        sb.append(baslik()).append("\n")
        sb.append("═".repeat(30)).append("\n")

        // Kas Sistemi
        val antrenmanSayisi = FitnessMotor.antrenmanlar(context).size
        val bugunSet = FitnessMotor.bugunToplamSet(context)
        sb.append("\n💪 Kas Sistemi\n")
        sb.append("  Antrenman kaydı: $antrenmanSayisi\n")
        sb.append("  Bugünkü set: $bugunSet\n")

        // Beslenme
        val kalori = BeslenmeMotor.bugunKalori(context)
        val hedef = BeslenmeMotor.kaloriHedefi(context)
        val su = BeslenmeMotor.suBardak(context)
        sb.append("\n🍽️ Beslenme\n")
        sb.append("  Bugünkü kalori: $kalori / $hedef kcal\n")
        sb.append("  Su: $su bardak (${su * 250} ml)\n")

        // Uyku
        val uykuSayisi = UykuMotor.son7GunKayitSayisi(context)
        sb.append("\n😴 Uyku\n")
        if (uykuSayisi == 0) {
            sb.append("  Son 7 günde kayıt yok\n")
        } else {
            val ort = UykuMotor.son7GunOrtalamaDakika(context).toInt()
            val kalite = UykuMotor.son7GunOrtalamaKalite(context)
            sb.append("  Kayıtlı gece: $uykuSayisi\n")
            sb.append("  Ortalama süre: ${UykuMotor.sureMetni(ort)}\n")
            sb.append("  Ortalama kalite: ${"%.1f".format(kalite)}/5\n")
        }

        // Ruh hali
        val moodSayisi = MoodMotor.son7GunKayitSayisi(context)
        sb.append("\n🎭 Ruh Hali\n")
        if (moodSayisi == 0) {
            sb.append("  Son 7 günde kayıt yok\n")
        } else {
            val ortMood = MoodMotor.son7GunOrtalama(context)
            sb.append("  Kayıt sayısı: $moodSayisi\n")
            sb.append("  Ortalama: ${MoodMotor.emoji(ortMood.toInt())} ${"%.1f".format(ortMood)}/5\n")
        }

        // Bütçe
        val gider = runCatching {
            Butce.ayOzeti(context)?.gider ?: 0.0
        }.getOrDefault(0.0)
        sb.append("\n💰 Bu ay harcama\n")
        sb.append("  ${"%.0f".format(gider)} TL\n")

        sb.append("\n═".repeat(30)).append("\n")
        sb.append("Günlük Asistan ile oluşturuldu ❤️")
        return sb.toString()
    }
}
