package com.gunlukasistan.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * v10.4 · Öneri A9 — Sesli geri sayım (TTS).
 *
 * ── Dürüst not ──
 * Sesli okuma altyapısı `SesliDersServisi` ile ders çalışmada var;
 * sayaç bundan hiç yararlanmıyordu. Bu sınıf, sayaç ekranı açıkken
 * kalan süreyi kulağa söyler: telefon masada dururken ekrana
 * bakmadan "5 dakika kaldı", "10… 5… 3-2-1" duyulur.
 *
 * ── Sınır bilinçli ──
 * Yalnız ekran öndeyken konuşur (arka planda TTS = pil + sürpriz
 * ses; orada bitiş sesi zaten var). Varsayılan **kapalı**;
 * `SayacAyar.tts` ile açılır.
 *
 * ── Saf bölge ──
 * [konusmaMetni] altlık tablo — birim testli. TTS ömrü fragment'e
 * bağlıdır ve testlerde çağrılmaz.
 */
object SayacSes {

    /**
     * Kalan saniyeye karşılık gelen konuşma metni.
     *
     * Eşikler seyrek başlar (kafa şişirmeden haber ver), son 10
     * saniyede sıklaşır: 10 ve 5, ardından 3-2-1. "0" söylenmez —
     * bitişi zil sesi söyler.
     */
    fun konusmaMetni(kalanSn: Int): String? = when (kalanSn) {
        300 -> "Beş dakika kaldı"
        120 -> "İki dakika kaldı"
        60 -> "Bir dakika kaldı"
        30 -> "Otuz saniye"
        10 -> "On"
        5 -> "Beş"
        4 -> "Dört"
        3 -> "Üç"
        2 -> "İki"
        1 -> "Bir"
        else -> null
    }

    /**
     * Bu sınırda söylenmeli mi? Son 10 saniyede söylenmiş olanlar
     * tekrar edilmesin diye fragment bir küme tutar; sadeleştirilmiş
     * karar burada: metin var VE daha önce söylenmemiş.
     */
    fun soylenmeli(kalanSn: Int, soylenenler: Set<Int>): Boolean =
        konusmaMetni(kalanSn) != null && kalanSn !in soylenenler
}

/**
 * Fragment yaşam döngüsüne bağlı ince TTS sarmalayıcısı.
 * Motor hazır olana dek gelen konuşmalar bekletilmez, düşürülür —
 * geri sayım gecikmez.
 */
class SayacSesli(context: Context) {

    private var hazir = false
    private var motor: TextToSpeech? = null

    init {
        runCatching {
            motor = TextToSpeech(context.applicationContext) { durum ->
                if (durum == TextToSpeech.SUCCESS) {
                    motor?.language = Locale("tr", "TR")
                    hazir = true
                }
            }
        }
    }

    fun soyle(metin: String) {
        if (!hazir) return
        runCatching {
            motor?.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "ga_sayac_tts")
        }
    }

    fun kapat() {
        runCatching { motor?.stop(); motor?.shutdown() }
        motor = null
        hazir = false
    }
}
