package com.gunlukasistan.app

import java.security.MessageDigest

/**
 * v10.22 — Gizlilik Kilidi (PIN): saf mantık.
 *
 * Android bağımlılığı YOK — JVM birim testlerinde koşar (android.jar stub
 * tuzağı burada yok; MessageDigest JDK sınıfıdır). Cihaz tarafı:
 * [KilitDepo] (SharedPreferences okuma/yazma) ve [KilitActivity] (arayüz).
 *
 * Tasarım kararları
 * ─────────────────
 * · PIN asla saklanmaz; SHA-256("GAK1|tuz|PIN") saklanır.
 * · Tuz (salt) cihaza özel rastgele üretilir — aynı PIN iki cihazda
 *   farklı hash verir (gökkuşağı tablosu etkisiz).
 * · Karşılaştırma sabit zamanlıdır; `==` ilk farklı karakterde döner ve
 *   zamanlamadan PIN hakkında bilgi sızdırır — burada hep tüm baytlar gezilir.
 * · 5 yanlış denemede 30 sn bekleme; sayaç başarıda sıfırlanır.
 * · Bekleme ve otomatik kilit kararı saat-enjekte edilir → test edilebilir.
 */
object KilitMantik {

    const val PIN_MIN = 4
    const val PIN_MAX = 8
    const val HATA_LIMITI = 5
    const val BEKLEME_MS = 30_000L

    /**
     * Kısa "arka plan" ayrılıkları (ekran döndürme gibi yapılandırma
     * değişimleri) kilit sayılmaz. Etkin eşik = max(zamanAsimi, bu değer).
     */
    const val GECIS_ESIGI_MS = 1_500L

    /** Otomatik kilit seçenekleri (ms). 0 = her arka plan dönüşünde. */
    val ZAMAN_ASIMLARI = longArrayOf(0L, 60_000L, 300_000L, 900_000L)

    /** PIN yalnızca rakam ve 4–8 haneli olabilir. */
    fun pinGecerliMi(pin: String): Boolean =
        pin.length in PIN_MIN..PIN_MAX && pin.all { it in '0'..'9' }

    /** SHA-256 hex (küçük harf, 64 karakter). */
    fun sha256Metin(girdi: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bayt = md.digest(girdi.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bayt.size * 2)
        for (b in bayt) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    /** Sürüm etiketi: ileride biçim değişirse eski hash'ler ayırt edilir. */
    fun pinHash(pin: String, tuz: String): String = sha256Metin("GAK1|$tuz|$pin")

    /**
     * Tuz üretimi — parametreler dışarıdan gelir (testte deterministik).
     * Cihaz tarafı nanoTime + SecureRandom besler.
     */
    fun tuzUret(parca1: Long, parca2: Long): String =
        sha256Metin("TUZ|$parca1|$parca2").substring(0, 16)

    /**
     * Sabit zamanlı dize eşitliği. Uzunluk farkı da sonuca katılır ama
     * döngü hep en kısa kadar sürer — erken çıkış yok.
     */
    fun sabitZamanliEsit(a: String, b: String): Boolean {
        val ab = a.toByteArray(Charsets.UTF_8)
        val bb = b.toByteArray(Charsets.UTF_8)
        var fark = ab.size xor bb.size
        val n = minOf(ab.size, bb.size)
        for (i in 0 until n) fark = fark or (ab[i].toInt() xor bb[i].toInt())
        return fark == 0
    }

    /** Yanlış-deneme sayacı: ardışık hata + bekleme bitiş anı. */
    data class DenemeDurum(val hatalar: Int = 0, val kilitBitisMs: Long = 0L)

    fun beklemedeMi(d: DenemeDurum, simdiMs: Long): Boolean = simdiMs < d.kilitBitisMs

    /** Kalan bekleme (saniye, yukarı yuvarlı); beklemede değilse 0. */
    fun kalanBeklemeSn(d: DenemeDurum, simdiMs: Long): Long =
        if (!beklemedeMi(d, simdiMs)) 0L else (d.kilitBitisMs - simdiMs + 999L) / 1000L

    /** Kalan hak — en az 1 gösterilir (limit anında sayaç zaten sıfırlanır). */
    fun kalanHak(d: DenemeDurum): Int = (HATA_LIMITI - d.hatalar).coerceAtLeast(1)

    /**
     * Yanlış deneme kaydı. Beklemedeyken durum DEĞİŞMEZ (arayüz girişi
     * zaten kilitler; bu ikinci savunma hattı). Limit dolunca sayaç
     * sıfırlanır ve bekleme başlar.
     */
    fun yanlisDeneme(d: DenemeDurum, simdiMs: Long): DenemeDurum {
        if (beklemedeMi(d, simdiMs)) return d
        val yeniHata = d.hatalar + 1
        return if (yeniHata >= HATA_LIMITI) {
            DenemeDurum(hatalar = 0, kilitBitisMs = simdiMs + BEKLEME_MS)
        } else {
            DenemeDurum(hatalar = yeniHata, kilitBitisMs = 0L)
        }
    }

    /** Başarılı giriş — sayaç ve bekleme sıfırlanır. */
    fun dogruDeneme(): DenemeDurum = DenemeDurum()

    /**
     * Otomatik kilit kararı.
     *
     * @param kurulu PIN tanımlı mı
     * @param azOnceAcildi kilit hemen önce başarıyla açıldıysa true —
     *        yoksa "her zaman" kipinde ekran kapanıp açılırken sonsuz
     *        kilit döngüsü olurdu (tasarım arızası, önlemi koda gömülü)
     * @param arkaPlanaGidisMs uygulamanın en son tamamen arka plana gittiği
     *        an; 0 = kayıt yok (soğuk açılış) → kilit gerekli
     * @param zamanAsimiMs 0 = her ayrılışta kilit (yine de kısa yapılandırma
     *        geçişleri [GECIS_ESIGI_MS] ile elenir, döndürmede kilit yok)
     */
    fun kilitGerekliMi(
        kurulu: Boolean,
        azOnceAcildi: Boolean,
        arkaPlanaGidisMs: Long,
        simdiMs: Long,
        zamanAsimiMs: Long
    ): Boolean {
        if (!kurulu) return false
        if (azOnceAcildi) return false
        if (arkaPlanaGidisMs <= 0L) return true
        if (simdiMs < arkaPlanaGidisMs) return false // saat kayması: kilitleme
        val esik = maxOf(zamanAsimiMs, GECIS_ESIGI_MS)
        return (simdiMs - arkaPlanaGidisMs) >= esik
    }
}
