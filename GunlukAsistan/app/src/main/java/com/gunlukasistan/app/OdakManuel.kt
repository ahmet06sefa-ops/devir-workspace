package com.gunlukasistan.app

/**
 * v10.19 · S2 — Manuel odak ekleme yardımcıları (saf — JVM testli).
 *
 * Kullanıcı isteği: "Zaman başlatmadan çalıştığım zamanlar oluyor;
 * manuel ekleme yeri ekle ve odaklanma saati yerine yaz."
 *
 * Eklenen dakikalar `Store.addTodayFocusMinutes` ile gün toplamına,
 * sayaç oturumu gibi işlenir — hedef yüzdesi, widget'lar, seri ve
 * rozet kontrolleri aynı kanaldan beslendiği için ek katman gerekmez.
 */
object OdakManuel {

    /**
     * Tek seferde eklenebilir dakika: 1..480 (8 saat).
     * Üst sınır, yanlışlıkla dev girilen değerleri sekerek keser.
     */
    fun kelepcele(dk: Int): Int = dk.coerceIn(1, 480)
}
