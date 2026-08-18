package com.gunlukasistan.app

/**
 * v10.2 · Öneri A4 — Final spurt bölge hesabı (saf mantık).
 *
 * ── Dürüst not ──
 * Kadranda bitişe yaklaşınca renk geçişi (amber → kırmızı) ve son
 * 10 saniyede nabız ZATEN vardı (`SayacKadraniView.aktifRenk`,
 * `nabizOlcegi`). Ama eşikler **zaman bazlı**ydı (5 dk / 1 dk):
 * 2 saatlik bir çalışmada spurt bölgesi son 5 dakikayla sınırlı
 * kalıyordu — orantısız.
 *
 * Bu sınıf bölgeyi **oran bazlı** genişletir: uyarı bölgesi artık
 * `max(5 dk, toplamın %10'u)`. 25 dk Pomodoro'da davranış aynı,
 * uzun oturumlarda spurt adil büyür.
 *
 * Görsel işi hâlâ View'da; burada yalnızca sınır matematiği durur —
 * birim testi böyle mümkün.
 */
object SayacSpurt {

    /**
     * Uyarı bölgesinin başlangıcı (saniye).
     *
     * Zaman bazlı zemin 5 dk; oran bazlı tavan toplamın %10'u.
     * Kısa sürelerde (%10 < 5 dk) eski davranış korunur.
     */
    fun uyariBaslangiciSn(toplamMs: Long): Long {
        val zamanZemini = 300L
        val oranTavani = (toplamMs / 1000f * 0.10f).toLong()
        return maxOf(zamanZemini, oranTavani)
    }

    /** Kritik bölge her zaman son 60 saniye. */
    const val KRITIK_SN = 60L

    /**
     * Kalan sürenin spurt seviyesi.
     *
     * @return 0 normal · 1 uyarı (amber bandı) · 2 kritik (kırmızı bandı)
     */
    fun seviye(kalanSn: Long, toplamMs: Long): Int = when {
        kalanSn < 0 -> 0
        kalanSn <= KRITIK_SN -> 2
        kalanSn <= uyariBaslangiciSn(toplamMs) -> 1
        else -> 0
    }

    /**
     * Uyarı bandı içindeki ilerleme (0.0 = banda yeni girdi, 1.0 = kritiğe geldi).
     * Bandın dışındaysa 0. Renk karışımı bu değerle yapılır.
     */
    fun bandOrani(kalanSn: Long, toplamMs: Long): Float {
        if (seviye(kalanSn, toplamMs) == 0) return 0f
        val bas = uyariBaslangiciSn(toplamMs).toFloat()
        val kritik = KRITIK_SN.toFloat()
        if (bas <= kritik) return 1f
        return (1f - (kalanSn - kritik) / (bas - kritik)).coerceIn(0f, 1f)
    }
}
