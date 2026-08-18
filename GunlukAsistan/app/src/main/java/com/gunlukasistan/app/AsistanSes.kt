package com.gunlukasistan.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * v11.13 — Yapay zekâ asistanı sesli hale getirir (TTS).
 *
 * Kullanıcı isteği: "Yapay zekayı sesli hale getir benimle konussun asistan
 * kocum gibi". Bu sınıf AI asistanın cevaplarını kulağa okur.
 *
 * ── Tasarım ──
 *  · [konusmaMetni]: saf, JVM testli. Emoji/simge karakterlerini ayıklayıp
 *    metni çok kısaltmadan konuşulabilir hale getirir (TTS emojiyi çirkin
 *    okur; "\u2713 Yap\u0131ld\u0131" gibi ön ekler temizlenir).
 *  · [AsistanSeslendirici]: TextToSpeech sarmalayıcısı. Motor hazır olana
 *    kadar gelen konuşmalar küçük bir kuyrukta bekler, sonra sırayla okunur.
 *    "dur" ile anında kesilir; "kapat" ile motor kapatılır.
 *  · Aç/kapa tercihi [sesAcikMi] / [setSesAcik] — varsayılan AÇIK (kullanıcı
 *    asistanın konuşmasını istedi).
 *
 * ── Sınır ──
 * TTS yalnız asistan ekranı öndeyken yaşar; arka planda konuşmaz
 * (kullanıcı başka yerdeyken sürpriz ses çıkmaz).
 */
object AsistanSes {

    private const val PREF = "asistan_sesli_v1"
    private const val K_ACIK = "acik"
    private const val MAKS_UZUNLUK = 400

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Sesli okuma açık mı? Varsayılan AÇIK (kullanıcı sesli asistan istedi). */
    fun sesAcikMi(context: Context): Boolean = prefs(context).getBoolean(K_ACIK, true)

    fun setSesAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean(K_ACIK, acik).apply()
    }

    /**
     * Konuşulacak metni hazırlar (saf — JVM testli).
     * Emoji ve özel simgeler ayıklanır, gereksiz boşluklar tekilleştirilir,
     * makul bir uzunlukla sınırlanır (TTS çok uzun metni geveleyebilir).
     */
    fun konusmaMetni(ham: String): String {
        if (ham.isBlank()) return ""
        // p{So}=Symbol(Özel) (emoji), p{Cs}=Surrogate; kontrol karakterleri de at.
        val temiz = ham
            .replace(Regex("[\\p{So}\\p{Cs}\\p{Cc}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (temiz.length > MAKS_UZUNLUK) temiz.take(MAKS_UZUNLUK).trim() + "…" else temiz
    }
}

/**
 * Asistan cevaplarını seslendiren TTS sarmalayıcısı.
 * Motor hazır olana dek gelen konuşmalar kuyrukta bekler; hazır olunca
 * sırayla okunur. Kullanıcı "dur" derse anında kesilir.
 */
class AsistanSeslendirici(context: Context) {

    private var hazir = false
    private var motor: TextToSpeech? = null
    private val kuyruk = ArrayDeque<String>()

    init {
        runCatching {
            motor = TextToSpeech(context.applicationContext) { durum ->
                if (durum == TextToSpeech.SUCCESS) {
                    runCatching {
                        motor?.language = Locale("tr", "TR")
                    }
                    hazir = true
                    kuyruguBosalt()
                }
            }
        }
    }

    private fun kuyruguBosalt() {
        while (hazir && kuyruk.isNotEmpty()) {
            val metin = kuyruk.removeFirst()
            runCatching {
                motor?.speak(metin, TextToSpeech.QUEUE_ADD, null, "ga_asistan_tts")
            }
        }
    }

    /** Bir asistan cevabını kuyruğa ekleyip (motor hazırsa) okur. */
    fun konus(ham: String) {
        val metin = AsistanSes.konusmaMetni(ham)
        if (metin.isBlank()) return
        if (hazir) {
            runCatching { motor?.speak(metin, TextToSpeech.QUEUE_ADD, null, "ga_asistan_tts") }
        } else {
            // Motor hazırlanana dek küçük bir kuyruk tut — asla taşmasın
            if (kuyruk.size < 8) kuyruk.addLast(metin)
        }
    }

    /** Anında konuşmayı kes. */
    fun dur() {
        runCatching { motor?.stop() }
    }

    /** Motoru kapat ve kuyruğu boşalt. */
    fun kapat() {
        runCatching { motor?.stop(); motor?.shutdown() }
        motor = null
        hazir = false
        kuyruk.clear()
    }
}
