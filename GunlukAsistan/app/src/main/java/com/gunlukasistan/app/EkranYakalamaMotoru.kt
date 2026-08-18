package com.gunlukasistan.app

/**
 * v11.13 — Gerçek ekran görüntüsü (piksel) → görsel model karar motoru.
 *
 * "Yarım kalan" öneri #4: önceki sürüm yalnızca ekran ETİKETLERİNİ gönderiyordu.
 * Bu motor, API 30+ [AccessibilityService.captureScreenshot] ile gerçek
 * piksel ekran görüntüsünü görsel modele gönderir; model "neye dokunacaksın"
 * kararını görselden verir.
 *
 * Bu dosya SAF karar katmanını taşır:
 *  · [gorselIstemiKur] — yakalanan görüntü için görsel model istemi.
 *  · [kucukKareEn] — görsel modele uygun küçük kare ölçek (varsayılan).
 *  · [yakinlastir] — ham piksel kare verisini normalize edilmiş kare listesine çevirir.
 *
 * Gerçek yakalama ([AccessibilityService.takeScreenshot]) ve görsel model
 * çağrısı ([AiClient.konuOku]) ayrı katmanlarda.
 */
object EkranYakalamaMotoru {

    /** Görsel model için istem: ekran görüntüsünde ne olduğunu anlat, hangi öğeye dokunulacağını söyle. */
    fun gorselIstemiKur(kullaniciAmac: String): String =
        "Bu bir ekran görüntüsü. Kullanıcı amacı: \"$kullaniciAmac\". " +
            "Ekranda ne olduğunu kısaca anlat ve kullanıcının amacı için hangi " +
            "tıklanabilir öğeye dokunman gerektiğini söyle. Cevabını SADECE şu " +
            "biçimde ver: tikla|ÖĞE. Uygun öğe yoksa: tikla|YOK"

    /** Görsel modellere yaygın giriş boyutu (kare). */
    const val KUCUK_KARE_EN = 512

    /**
     * Ham piksel matrisini (gri tonlama 0..255) küçük bir kareye indirger
     * ve "satır: değerler" biçiminde normalleştirilmiş özet üretir.
     * Yalnızca karar motoruna örnek vermek içindir; gerçek giriş base64'tir.
     */
    fun yakinlastir(satirlar: List<IntArray>, hedefEn: Int = KUCUK_KARE_EN): List<String> {
        if (satirlar.isEmpty()) return emptyList()
        val h = satirlar.size
        val w = satirlar[0].size
        val boyut = hedefEn
        val sonuc = mutableListOf<String>()
        for (y in 0 until boyut) {
            val sy = (y * h / boyut).coerceIn(0, h - 1)
            val sb = StringBuilder()
            for (x in 0 until boyut) {
                val sx = (x * w / boyut).coerceIn(0, w - 1)
                val v = satirlar[sy][sx]
                sb.append(if (v > 127) '1' else '0')
            }
            sonuc.add(sb.toString())
        }
        return sonuc
    }
}
