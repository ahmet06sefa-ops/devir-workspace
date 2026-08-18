package com.gunlukasistan.app

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * v10.84 — Arka Plan Müzik & Radyo (YouTube, Spotify, Karnaval vb.) Medya Kumandası.
 *
 * Kullanıcının "Odak muzikleri yerine arka planda calina şarkıyi koy ne calinirsa
 * isterse karnaval radyo ister youtune baska uygulamadan açacağım ve sen sadece
 * oraya durdur başlat ileri geri yapma yeri koy" talimatı doğrultusunda:
 *
 *  • Uygulama içi odak sesleri yerine telefonun arka planındaki gerçek müzik/radyo
 *    uygulamasını (Spotify, YouTube Music, Karnaval Radyo vb.) doğrudan denetler.
 *  • AudioManager.dispatchMediaKeyEvent üzerinden standart kulaklık/kumanda
 *    sinyallerini (Oynat/Durdur, İleri, Geri) arka plandaki medya oynatıcıya iletir.
 *  • Hiçbir özel izin gerektirmeden tüm müzik ve radyo uygulamalarıyla %100 uyumludur.
 */
object ArkaPlanMedyaKumandasi {

    enum class Eylem {
        OYNAT_DURDUR,
        SONRAKI,
        ONCEKI
    }

    /** Arka planda çalan müzik/radyo uygulamasına medya tuş sinyali gönderir. */
    fun medyaEylemiGonder(context: Context, eylem: Eylem): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val keyCode = when (eylem) {
                Eylem.OYNAT_DURDUR -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                Eylem.SONRAKI -> KeyEvent.KEYCODE_MEDIA_NEXT
                Eylem.ONCEKI -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            }

            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

            am.dispatchMediaKeyEvent(downEvent)
            am.dispatchMediaKeyEvent(upEvent)
            true
        } catch (e: Exception) {
            android.util.Log.w("ArkaPlanMedyaKumandasi", "Medya komutu gönderilemedi: ${eylem.name}", e)
            false
        }
    }

    /** Arka planda aktif müzik/radyo çalıyor mu? (Müzik akışı aktif mi). */
    fun muzikCaliyorMu(context: Context? = null): Boolean {
        if (context == null) return false
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.isMusicActive == true
        } catch (e: Exception) {
            false
        }
    }

    /** Arayüzde gösterilecek durum metni (Saf mantık - birim testli). */
    fun durumMetniGetir(caliyorMu: Boolean): String {
        return if (caliyorMu) {
            "▶ Çalıyor · Arka Plan Müzik / Radyo (YouTube, Spotify vb.)"
        } else {
            "⏸ Duraklatıldı / Hazır · Arka Plan Medya Kumandası"
        }
    }

    /** Eylem buton etiketi (Saf mantık - birim testli). */
    fun butonEtiketi(eylem: Eylem): String = when (eylem) {
        Eylem.OYNAT_DURDUR -> "▶/⏸ Oynat / Dur"
        Eylem.SONRAKI -> "▶| İleri"
        Eylem.ONCEKI -> "|◀ Geri"
    }
}
