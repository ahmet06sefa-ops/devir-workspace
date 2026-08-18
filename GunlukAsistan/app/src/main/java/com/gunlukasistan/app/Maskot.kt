package com.gunlukasistan.app

/**
 * v10.8 · Öneri D43 — Uygulama maskotu "Pofi"nin ruh hali mantığı.
 *
 * ── Ne eksikti ──
 * Uygulamada tepki veren hiçbir karakter yoktu: statik istatistikler,
 * statik selamlama. Kullanıcı "seri yanıyor" hissini sayıdan
 * okuyordu; duygusal bir yankı yoktu. Pofi, ana ekranın hero
 * kartında yaşayan ve **duruma göre yüz değiştiren** küçük bir
 * karakterdir.
 *
 * ── Karar kuralları ──
 * Öncelik sırası insan sezgisine göredir: ne yapılıyorsa o konuşur.
 * Mola bildirimi çalışmayı, uyku saati coşkuyu ezer — saat 02:00'de
 * "hadi koşalım" diyen maskot güvenilmez olurdu.
 *
 * ── Neden saf object ──
 * Karar tablosunun her hücresi birim testiyle kanıtlanır; çizim
 * ([MaskotView]) ve ekran bağlantısı (HomeFragment) yalnızca
 * sonucu okur. Saat parametre olarak gelir — saat bağımlı test
 * tuzağı yaşamayız (v10.2 dersi).
 */
object Maskot {

    /** Pofi'nin ifade durumları. */
    enum class Ruh {
        /** Sayaç/zincir koşuyor — odaklanmış bakış, düz ağız. */
        ODAKLI,

        /** Pomodoro/zincir molası — kapalı göz, gevşek gülüş. */
        MOLADA,

        /** Seri [ALEV_ESIK]+ gün — parlayan göz + başın üstünde alev. */
        ALEV,

        /** Bugünkü odak [GURUR_ESIK_DK]+ dk — kocaman gülümseme. */
        GURURLU,

        /** Gece saati (23:00–05:00) — kapalı göz + Zz. */
        UYKULU,

        /** Varsayılan neşeli hâl. */
        NESHALI
    }

    /** Karar girdileri. Saat 0..23; ilgisiz alanlar sıfır olabilir. */
    data class Girdi(
        val saat: Int,
        val seriGun: Int,
        val sayacCalisiyor: Boolean,
        val molada: Boolean,
        val bugunOdakDk: Int
    )

    /** Seri bu eşiği geçince Pofi alev alır. */
    const val ALEV_ESIK = 7

    /** Günlük odak bu dakikayı geçince gurur moduna geçer. */
    const val GURUR_ESIK_DK = 100

    /** Uyku penceresi: 23:00 (dahil) → 05:00 (hariç). */
    const val UYKU_BASLANGIC = 23
    const val UYKU_BITIS = 5

    /**
     * Duruma göre ruh halini seçer.
     *
     * Öncelik (üstten alta):
     * 1. Mola — çalışma sinyalinden önce, çünkü molada sayaç da
     *    "çalışıyor" görünür; yanlışlıkla odak yüzü basılmasın.
     * 2. Çalışma — sayaç/zincir koşarken odak yüzü.
     * 3. Uyku — gece mesaisi yoksa coşku gösterme.
     * 4. Alev — uzun seri coşkusu.
     * 5. Gurur — bugünkü emek birikimi.
     * 6. Neşeli — bekleme hâli.
     */
    fun ruhHali(g: Girdi): Ruh {
        val saat = g.saat.coerceIn(0, 23)
        return when {
            g.molada -> Ruh.MOLADA
            g.sayacCalisiyor -> Ruh.ODAKLI
            saat >= UYKU_BASLANGIC || saat < UYKU_BITIS -> Ruh.UYKULU
            g.seriGun >= ALEV_ESIK -> Ruh.ALEV
            g.bugunOdakDk >= GURUR_ESIK_DK -> Ruh.GURURLU
            else -> Ruh.NESHALI
        }
    }

    /**
     * Gün boyu sabit, dokununca ileri dönen mesaj sırası.
     *
     * `gunNo` (yılın günü) + dokunma kayması birlikte aday listesi
     * üzerinde döner — aynı gün aynı mesajla karşılaşmak tesadüf
     * değil tasarımdır; sürekli değişen maskot güvenilmez hissettirir.
     */
    fun mesajSira(gunNo: Int, kayma: Int, adet: Int): Int {
        if (adet <= 0) return 0
        return Math.floorMod(gunNo + kayma, adet)
    }
}
