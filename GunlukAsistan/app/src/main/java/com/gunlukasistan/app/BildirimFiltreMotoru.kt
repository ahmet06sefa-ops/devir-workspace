package com.gunlukasistan.app

/**
 * v11.13 — Akıllı bildirim filtresi (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Çok fazla bildirim var, bunaltabilir; akıllı filtreleme
 * zayıf." Bu motor, hangi bildirimlerin gönderileceğine dair öncelik kararları
 * verir: sessiz saat, kullanıcı aktifse erteleme, önem eşiği.
 *
 *  · [SessizDilim] — bir gün içindeki sessiz saat aralığı.
 *  · [sessizMi] — şu an sessiz dilimde mi?
 *  · [atlaMi] — bu bildirim şu an atlanmalı mı (önem + sessiz + aktiflik)?
 *  · [oncelik] — bildirim önemini 0..100 puanlar.
 */
object BildirimFiltreMotoru {

    data class SessizDilim(val basDk: Int, val bitDk: Int)

    /** Şu an sessiz dilimde mi? (dk cinsinden gün saati) */
    fun sessizMi(simdiDk: Int, dilim: SessizDilim?): Boolean {
        if (dilim == null) return false
        return if (dilim.basDk <= dilim.bitDk) {
            simdiDk in dilim.basDk..dilim.bitDk
        } else {
            // gece yarısını aşan dilim (örn. 22:00 → 07:00)
            simdiDk >= dilim.basDk || simdiDk <= dilim.bitDk
        }
    }

    /** Bildirim önceliği 0..100 (içerik başına). */
    fun oncelik(onem: Int, gecikmeVarMi: Boolean, odakIcindeMi: Boolean): Int {
        var p = onem.coerceIn(0, 100)
        if (gecikmeVarMi) p += 10
        if (odakIcindeMi) p += 20
        return p.coerceIn(0, 100)
    }

    /**
     * Bu bildirim şu an atlanmalı mı?
     * Kural: önem < eşik VE (sessiz mod VEYA kullanıcı aktif) ise atla.
     */
    fun atlaMi(
        onem: Int,
        esik: Int,
        sessizModAcik: Boolean,
        kullaniciAktifMi: Boolean
    ): Boolean {
        val dusuk = onem < esik
        return dusuk && (sessizModAcik || kullaniciAktifMi)
    }

    /** Bildirim türünü temsil eden önem puanı (0=önemsiz, 100=kritik). */
    fun turOnemi(tur: String): Int = when {
        tur.contains("alarm") || tur.contains("bitis") || tur.contains("namaz") -> 80
        tur.contains("gorev") || tur.contains("hatirlatma") -> 60
        tur.contains("motivasyon") || tur.contains("koc") -> 30
        tur.contains("ozet") || tur.contains("rozet") -> 20
        else -> 40
    }
}
