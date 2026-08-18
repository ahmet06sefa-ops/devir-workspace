package com.gunlukasistan.app

/**
 * v10.54 — Akıllı "Odak & Verimlilik Karnesi" (Haftalık AI Raporu) saf mantık motoru.
 * Son 7 günün odak, görev ve kesinti verilerini değerlendirir, harf notu ve koç tavsiyesi verir.
 */
object VerimlilikKarnesi {

    data class KarneOzeti(
        val haftalikNot: String,
        val toplamOdakSaat: Float,
        val enVerimliGunAd: String,
        val ortalamaOdakDk: Int,
        val kocTavsiyesi: String
    )

    /**
     * Haftalık (7 günlük) verileri değerlendirerek karne özeti döndürür.
     */
    fun karneAnalizEt(
        haftalikOdakDk: List<Int>,
        haftalikGorevTamam: List<Int>,
        haftalikKesinti: List<Int>
    ): KarneOzeti {
        val gunAdlari = arrayOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        val odakList = if (haftalikOdakDk.size == 7) haftalikOdakDk else List(7) { 0 }
        val gorevList = if (haftalikGorevTamam.size == 7) haftalikGorevTamam else List(7) { 0 }
        val kesintiList = if (haftalikKesinti.size == 7) haftalikKesinti else List(7) { 0 }

        val toplamDk = odakList.sum()
        val toplamSaat = (toplamDk / 60f)
        val ortDk = if (odakList.isNotEmpty()) (toplamDk / 7) else 0

        val maxGunIdx = odakList.indices.maxByOrNull { odakList[it] } ?: 0
        val enVerimliGun = gunAdlari.getOrElse(maxGunIdx) { "Çarşamba" }

        val aktifGunSayisi = odakList.count { it >= 25 }
        val toplamGorev = gorevList.sum()
        val toplamKesinti = kesintiList.sum()

        val not = when {
            aktifGunSayisi >= 6 && toplamDk >= 600 -> "A+"
            aktifGunSayisi >= 5 && toplamDk >= 400 -> "A"
            aktifGunSayisi >= 3 && toplamDk >= 200 -> "B"
            aktifGunSayisi >= 1 && toplamDk >= 50 -> "C"
            else -> "D"
        }

        val tavsiye = when {
            not == "A+" || not == "A" ->
                "Harika bir hafta! En yüksek odaklanmayı $enVerimliGun günü gösterdiniz ($toplamSaat saat toplam). Kesinti sayısını az tutarak bu ritmi koruyun!"
            toplamKesinti >= 10 ->
                "Odaklanma süreniz iyi fakat bu hafta $toplamKesinti kez kesintiye uğradınız. Daha uzun oturumlar için Odak Kalkanı'nı aktifleştirin."
            not == "B" ->
                "İyi bir hafta ($ortDk dk günlük ortalama). $enVerimliGun günkü ritminizi diğer günlere de yayarak 'A' seviyesine çıkabilirsiniz."
            else ->
                "Bu hafta odaklanma süreniz düşük kaldı. Küçük 15 dakikalık Tabata sayacı ile güne başlayarak motivasyonunuzu yükseltebiliriz."
        }

        return KarneOzeti(
            haftalikNot = not,
            toplamOdakSaat = ((toplamSaat * 10).toInt() / 10f),
            enVerimliGunAd = enVerimliGun,
            ortalamaOdakDk = ortDk,
            kocTavsiyesi = tavsiye
        )
    }
}
