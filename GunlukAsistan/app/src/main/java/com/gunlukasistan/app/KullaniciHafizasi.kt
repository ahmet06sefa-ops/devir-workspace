package com.gunlukasistan.app

/**
 * v11.13 — Kalıcı kullanıcı hafızası profili (SAF, JVM testli).
 *
 * Kullanıcı isteği: AI "seni tanıyan" bir koç olsun; genel web Gemini'sinden
 * farklı olarak geçmişini hatırlasın. Bu motor, uygulamanın verisinden kısa
 * bir "hafıza profili" çıkarır: kim, ne kadar çalışıyor, serisi, hedefleri.
 *
 *  · [profilMetni] — bugünkü veriden tek satırlık kalıcı bağlam üretir.
 *  · [haftalikOzet] — son 7 günün odak toplamı üzerinden kısa özet.
 *  · [hatirlaticiAnahtari] — günlük hatırlatıcılar için kararlı anahtar.
 *
 * Hafıza string'i [AiClient.buildSystemPrompt]'a eklenir; böylece Gemini her
 * sohbette kullanıcıyı "tanır". Veri cihazda üretilir (gizlilik korunur).
 */
object KullaniciHafizasi {

    /** Bugünkü odak + seri + hedef + sorudan tek paragraflık hafıza profili. */
    fun profilMetni(bugunOdakDk: Int, hedefDk: Int, seriGun: Int, enIyiSeri: Int, bugunSoru: Int): String {
        val hedefDurum = if (hedefDk > 0) {
            val oran = (bugunOdakDk * 100.0 / hedefDk).toInt().coerceIn(0, 999)
            if (bugunOdakDk >= hedefDk) "hedefine ulaştı" else "hedefinin %$oran'ına ulaştı"
        } else "hedef belirlemedi"
        val seriMetni = if (seriGun > 0) "$seriGun günlük çalışma serisi (en iyi $enIyiSeri)"
        else "bugün seri başlatmadı"
        return buildString {
            append("Kullanıcı bugün $bugunOdakDk dk odaklandı, $hedefDurum. ")
            append("$seriMetni. Bugün $bugunSoru soru çözdü.")
        }
    }

    /** Son 7 günün toplam odak dakikasından kısa bir değerlendirme cümlesi. */
    fun haftalikOzet(haftaOdakDk: Int): String = when {
        haftaOdakDk >= 1000 -> "Haftalık yoğun çalışma dönemi — harika istikrar."
        haftaOdakDk >= 500 -> "Haftalık orta-üstü çalışma temposu."
        haftaOdakDk >= 200 -> "Haftalık düşük-orta çalışma; artırılabilir."
        else -> "Hafta sakin geçti; yeni bir başlangıç için güzel fırsat."
    }

    /**
     * Günlük hatırlatıcı/bağlam anahtarı: tarih temelli kararlı bir etiket.
     * Aynı gün içinde tekrar çağrılsa aynı değeri döndürür (günlük özet kararlılığı).
     */
    fun hatirlaticiAnahtari(gunNo: Int, ay: Int, yil: Int): String =
        "%04d-%02d-%02d".format(yil, ay, gunNo)
}
