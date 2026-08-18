package com.gunlukasistan.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * v10.12 · ULTRA-30 / D22 — Ses manzarası motoru.
 *
 * ── Taramada yakalanan (dürüstlük notu) ──
 * Yağmur/kafe/orman döngüleri ve `res/raw` dosyaları zaten VARDI; her
 * iki ekran (TimerFragment, FullscreenTimerActivity) kendi MediaPlayer'ını
 * kuruyordu. Eksik olan **otomasyondu**: odakla otomatik başlama, molada
 * kısma, sayaç susunca susma. Bu nesne tek MediaPlayer'ı merkezileştirip
 * o otomasyonu ekler.
 *
 * ── Neden tek motor ──
 * İki ekran iki ayrı çalar açarsa aynı anda iki döngü üst üste biner.
 * Artık çalma tek yerden yürür; ekranlar yalnızca durumu GÖSTERİR ve
 * kullanıcının dokunuşunu motora iletir.
 *
 * ── Manuel / otomatik ayrımı ──
 *   · Kullanıcı karttan bir ses açtıysa akış MANUEL sayılır; otomasyon
 *     ona karışmaz (yalnız mola ses kısması uygulanır).
 *   · Otomasyonun başlattığı akış sayaç durunca/duraklayınca susar.
 *   · Kullanıcı manuel olarak durdurursa o oturum boyunca otomasyon
 *     yeniden başlatmaz ([manuelKapatti]); sayaç sıfırlanınca ya da yeni
 *     başlangıçta bayrak temizlenir.
 *
 * Karar tablosu [aksiyon] içinde saf ve birim testlidir.
 */
object SesManzarasi {

    data class Ses(val rawRes: Int, val emoji: String, val adRes: Int)

    /** Kartların ve seçim diyaloğunun ortak listesi (sıra sabit: v10.0'dan beri). */
    val SESLER = listOf(
        Ses(R.raw.yagmur, "🌧️", R.string.sound_rain),
        Ses(R.raw.dalga, "🌊", R.string.sound_wave),
        Ses(R.raw.orman, "🌲", R.string.sound_forest),
        Ses(R.raw.somine, "🔥", R.string.sound_fire),
        Ses(R.raw.ruzgar, "💨", R.string.sound_wind),
        Ses(R.raw.kafe, "☕", R.string.sound_cafe),
        Ses(R.raw.circir, "🦗", R.string.sound_cricket),
        Ses(R.raw.beyaz, "📻", R.string.sound_white)
    )

    private const val PREF = "fo_manzara_v1"
    private const val K_OTO = "oto"
    private const val K_SECIM = "secim"
    private const val K_MOLA_KIS = "mola_kis"
    private const val K_OZEL_URI = "ozel_uri"
    private const val K_OZEL_AD = "ozel_ad"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Ayarlar ----------------

    fun otomatik(c: Context): Boolean = prefs(c).getBoolean(K_OTO, true)
    fun setOtomatik(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean(K_OTO, v).apply()
        if (!v && otomatikCaliyor) kapatInternal()
    }

    /** Seçili manzara indeksi; -1 = seçilmedi; [OZEL_KOD] = kullanıcının dosyası. */
    fun secim(c: Context): Int = secimPure(prefs(c).getInt(K_SECIM, 0), ozelVar(c))

    /** v10.43 · Madde #3: kullanıcının eklediği ses dosyasının seçim kodu. */
    const val OZEL_KOD = 900

    /** Saf seçim doğrulama (birim testli) — özel kod ancak dosya kayıtlıyken geçer. */
    fun secimPure(ham: Int, ozelVarMi: Boolean): Int = when {
        ham == OZEL_KOD -> if (ozelVarMi) OZEL_KOD else -1
        else -> ham.coerceIn(-1, SESLER.lastIndex)
    }

    fun setSecim(c: Context, index: Int) {
        val temiz = if (index == OZEL_KOD && ozelVar(c)) OZEL_KOD
        else index.coerceIn(-1, SESLER.lastIndex)
        prefs(c).edit().putInt(K_SECIM, temiz).apply()
    }

    // ---------------- Madde #3: kullanıcının kendi odak sesi ----------------

    fun ozelUri(c: Context): String = prefs(c).getString(K_OZEL_URI, "") ?: ""

    fun ozelAd(c: Context): String = prefs(c).getString(K_OZEL_AD, "") ?: ""

    fun ozelVar(c: Context): Boolean = ozelUri(c).isNotBlank()

    /** Sistem ses seçiciden dönen dosyayı kaydedip seçime alır; boşsa seçim kalkar. */
    fun setOzel(c: Context, uri: String, ad: String) {
        prefs(c).edit()
            .putString(K_OZEL_URI, uri)
            .putString(K_OZEL_AD, ad)
            .putInt(K_SECIM, if (uri.isBlank()) -1 else OZEL_KOD)
            .apply()
    }

    fun moladaKis(c: Context): Boolean = prefs(c).getBoolean(K_MOLA_KIS, true)
    fun setMoladaKis(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean(K_MOLA_KIS, v).apply()
    }

    fun seciliSes(c: Context): Ses? = SESLER.getOrNull(secim(c))

    // ---------------- Çalma durumu ----------------

    private var player: MediaPlayer? = null

    /** Şu an çalan ses; -1 = sükûnet. Ekranlar buna bakarak kart boyar. */
    var calanIndeks: Int = -1
        private set

    /** Çalan akışı otomasyon mu başlattı (true) yoksa kullanıcı mı. */
    private var otomatikCaliyor = false

    /** Kullanıcı eliyle durdurdu — oturum bitene dek otomasyon başlatmaz. */
    private var manuelKapatti = false

    fun caliyorMu(): Boolean = calanIndeks >= 0

    // ---------------- Saf karar (birim testli) ----------------

    enum class Aksiyon { HIC, CAL, KAPAT, SES_UYGULA }

    /**
     * Otomasyon tablosu:
     *   · otomatik kapalı ya da seçim yok → otomatik akış varsa KAPAT
     *   · sayaç koşuyor ve hiçbir şey çalmıyor → kullanıcı bilerek
     *     kapatmadıysa CAL
     *   · koşarken bir şey çalıyor → yalnız SES_UYGULA (mola kısması)
     *   · sayaç durdu/durakladı → yalnız OTOMATİK akış kapanır;
     *     manuel akış kullanıcının elindedir
     */
    fun aksiyon(
        otomatik: Boolean,
        kosuyor: Boolean,
        secimVar: Boolean,
        caliyorMu: Boolean,
        otoCaliyor: Boolean,
        manuelKapatti: Boolean
    ): Aksiyon = when {
        !otomatik || !secimVar -> if (otoCaliyor) Aksiyon.KAPAT else Aksiyon.HIC
        kosuyor -> if (!caliyorMu) {
            if (manuelKapatti) Aksiyon.HIC else Aksiyon.CAL
        } else Aksiyon.SES_UYGULA
        else -> if (otoCaliyor) Aksiyon.KAPAT else Aksiyon.HIC
    }

    /** Mola evresi ses kısması: açıksa %25, kapalıysa tam ses. */
    fun hacim(molada: Boolean, kisAcik: Boolean): Float =
        if (molada && kisAcik) 0.25f else 1.0f

    // ---------------- Motor ----------------

    /** Kullanıcı karttan ses açtı. Otomasyon bu akışı sahiplenmez. */
    fun manuelCal(context: Context, index: Int) {
        if (index !in SESLER.indices) return
        kapatInternal()
        cal(context, index, otomatik = false)
        manuelKapatti = false
    }

    /**
     * Sayaç ekranı yıkılırken çağrılır: sayaç KOŞMUYORSA çalan akış bir ön
     * dinlemeydi — sessizce kapanır (eski davranışla birebir). Sayaç
     * koşuyorsa akış motorda yaşamaya devam eder; [manuelKapatti] ile
     * ilgisi yoktur, otomasyonu bastırmaz.
     */
    fun ekranKapandi(context: Context) {
        if (calanIndeks >= 0 && !TimerEngine.isRunning(context)) {
            kapatInternal()
        }
    }

    /** Kullanıcı karttan sesi kapattı. Oturum boyunca otomasyon susar. */
    fun manuelDur(context: Context) {
        kapatInternal()
        manuelKapatti = TimerEngine.isRunning(context)
    }

    /** Sayaç her BAŞLADIĞINDA: yeni oturum, bayrak temiz, otomasyon serbest. */
    fun sayacBasladi(context: Context) {
        manuelKapatti = false
        esitle(context)
    }

    /** Sayaç her dokunuşunda çağrılır (başlat/duraklat/sıfırla/bitir/uzat). */
    fun esitle(context: Context) {
        runCatching {
            val kosuyor = TimerEngine.isRunning(context)
            val molada = Pomodoro.acikMi(context) && Pomodoro.molada(context)
            if (!kosuyor) manuelKapatti = false
            when (
                aksiyon(
                    otomatik = otomatik(context),
                    kosuyor = kosuyor,
                    secimVar = secim(context) >= 0,
                    caliyorMu = calanIndeks >= 0,
                    otoCaliyor = otomatikCaliyor,
                    manuelKapatti = manuelKapatti
                )
            ) {
                Aksiyon.CAL -> cal(context, secim(context), otomatik = true)
                Aksiyon.KAPAT -> kapatInternal()
                Aksiyon.SES_UYGULA -> runCatching {
                    val h = hacim(molada, moladaKis(context))
                    player?.setVolume(h, h)
                }
                Aksiyon.HIC -> Unit
            }
        }
    }

    private fun cal(context: Context, index: Int, otomatik: Boolean) {
        try {
            // v10.43: kaynak ya paketteki raw döngüsü ya da kullanıcının dosyası
            val afd = if (index == OZEL_KOD) null
            else context.resources.openRawResourceFd(SESLER[index].rawRes) ?: return
            val molada = Pomodoro.acikMi(context) && Pomodoro.molada(context)
            val h = hacim(molada, moladaKis(context))
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (index == OZEL_KOD) {
                    setDataSource(context, android.net.Uri.parse(ozelUri(context)))
                } else {
                    setDataSource(afd!!.fileDescriptor, afd.startOffset, afd.declaredLength)
                    afd.close()
                }
                isLooping = true
                setVolume(h, h)
                prepare()
                start()
            }
            calanIndeks = index
            otomatikCaliyor = otomatik
        } catch (e: Exception) {
            android.util.Log.w("SesManzarasi", "Ses başlatılamadı", e)
            kapatInternal()
        }
    }

    private fun kapatInternal() {
        runCatching { player?.let { if (it.isPlaying) it.stop(); it.release() } }
        player = null
        calanIndeks = -1
        otomatikCaliyor = false
    }
}
