package com.gunlukasistan.app

/**
 * v11.13 — Proaktif Akıllı Koç mesaj üretici (SAF, JVM testli).
 *
 * Kullanıcı isteği: "AI seni isteyene kadar BEKLEMESİN, kendiliğinden
 * yönlendirsin." Bu motor, uygulamanın güncel verisinden günün vaktine göre
 * (sabah / öğle / akşam) proaktif koç mesajları üretir.
 *
 *  · [dilim] — saatten gün dilimi çıkarır (sabah/öğle/akşam/gece).
 *  · [mesaj] — veri + dilimden tek, samimi, eyleme dönüşebilir bir mesaj.
 *  · [baslik] — bildirim başlığı.
 */
object KocMotoru {

    enum class Dilim { SABAH, OGLE, AKSAM, GECE }

    /** Saate (0-23) göre gün dilimi. */
    fun dilim(saat: Int): Dilim = when {
        saat in 5..11 -> Dilim.SABAH
        saat in 12..16 -> Dilim.OGLE
        saat in 17..22 -> Dilim.AKSAM
        else -> Dilim.GECE
    }

    /**
     * Proaktif koç mesajı üretir. Girdiler veriye dayanır (Context yok, saf).
     * @param bekleyenGorev bekleyen görev sayısı
     * @param tamamlananGorev bugün tamamlanan görev sayısı
     * @param bugunOdakDk bugün odaklanılan dakika
     * @param hedefDk günlük odak hedefi
     * @param seriGun günlük çalışma serisi
     */
    fun mesaj(
        dilim: Dilim,
        bekleyenGorev: Int,
        tamamlananGorev: Int,
        bugunOdakDk: Int,
        hedefDk: Int,
        seriGun: Int
    ): String {
        val hedefMetni = if (hedefDk > 0) "$bugunOdakDk/$hedefDk dk odak" else "$bugunOdakDk dk odak"
        return when (dilim) {
            Dilim.SABAH -> {
                when {
                    bekleyenGorev > 3 ->
                        "Günaydın! Bugün $bekleyenGorev görev seni bekliyor. En önemli 1'ini seç, önce onu bitir."
                    bekleyenGorev > 0 ->
                        "Günaydın! Bugün $bekleyenGorev görevin var. Küçük başla, büyük ilerleme gelir."
                    else ->
                        "Günaydın! Görev listen tertemiz. Bugün kendine yeni bir hedef belirlemeye ne dersin?"
                }
            }
            Dilim.OGLE -> {
                when {
                    bugunOdakDk >= hedefDk && hedefDk > 0 ->
                        "Öğle molası ☀️ Hedefini ($hedefMetni) çoktan tamamladın. Harika iş!"
                    bekleyenGorev > 0 ->
                        "Öğle vakti! $bekleyenGorev görev kaldı ve $hedefMetni. Kısa bir odak bloğuyla kaldığın yerden devam et."
                    else ->
                        "Öğle vakti! Günün ortası — $hedefMetni. Devam edersen bugünü güzel kapatırsın."
                }
            }
            Dilim.AKSAM -> {
                when {
                    tamamlananGorev >= 5 ->
                        "Akşam 🎉 Bugün $tamamlananGorev görev tamamladın! Hak ettiğin bir dinlenme."
                    bugunOdakDk >= hedefDk && hedefDk > 0 ->
                        "Akşam! Bugün $tamamlananGorev görev bitirdin, odak hedefini de yakaladın. Süpersin!"
                    bekleyenGorev > 0 ->
                        "Akşam! $bekleyenGorev görev kaldı, $hedefMetni. Yarını güzel başlatmak için 1 tanesini şimdi halledebilirsin."
                    else ->
                        "Akşam! Bugünün özeti: $tamamlananGorev görev tamam, $hedefMetni. Yarın için tek bir öncelik belirle."
                }
            }
            Dilim.GECE -> {
                if (seriGun > 0)
                    "Gece 🌙 $seriGun günlük serin devam ediyor. Bugünü bitirmeden önce yarınki ilk adımını düşün."
                else
                    "Gece 🌙 Bugünün değerlendirmesi: $tamamlananGorev görev, $hedefMetni. Yarın seri başlatmak için küçük bir adım seç."
            }
        }
    }

    /** Bildirim başlığı (dilime göre samimi). */
    fun baslik(dilim: Dilim): String = when (dilim) {
        Dilim.SABAH -> "☀️ Koçunuz: Günaydın"
        Dilim.OGLE -> "🌤️ Koçunuz: Öğle"
        Dilim.AKSAM -> "🌆 Koçunuz: Akşam"
        Dilim.GECE -> "🌙 Koçunuz: Gece"
    }

    /** Bildirim kanalı adı. */
    const val KANAL = "akilli_koc_v1"
}
