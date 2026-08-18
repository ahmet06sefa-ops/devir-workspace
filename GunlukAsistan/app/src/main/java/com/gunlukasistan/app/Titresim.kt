package com.gunlukasistan.app

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * v8.2 — Dokunsal geri bildirim (öneri 2).
 *
 * ── Ölçüm ──
 * v8.1'de 166 Kotlin dosyasının HİÇBİRİNDE `performHapticFeedback`
 * çağrısı yoktu. Uygulama parmağın altında hiç cevap vermiyordu:
 * görev işaretlensin, quiz cevaplansın, sayaç başlasın — hepsi sessiz.
 *
 * ── Neden iki katman ──
 * 1. `View.performHapticFeedback` — sistemin kendi kısa dokunuşu.
 *    Kullanıcının sistem ayarındaki "dokunma titreşimi" tercihine
 *    saygı duyar, pil dostudur. Basit onaylar için bu yeter.
 * 2. `Vibrator` ile desen — sistem dokunuşunun ifade edemediği
 *    durumlar için (yanlış cevap çift vuruş, başarı üçlü artan).
 *    Bunlar `HapticFeedbackConstants` ile üretilemez.
 *
 * ── Neden her çağrı runCatching içinde ──
 * Bazı cihazlarda titreşim motoru yok (tablet), bazılarında
 * `VibratorManager` üreticiye özel biçimde davranıyor. Titreşim
 * asla uygulamayı çökertmemeli — süs bir özellik.
 *
 * ── Kullanıcı kapatabilir ──
 * `GorunumAyar.haptikAcik` false ise hiçbir şey yapılmaz.
 */
object Titresim {

    private const val TAG = "Titresim"

    // ------------------------------------------------------------------
    // 1. Katman: sistem dokunuşu (View üzerinden)
    // ------------------------------------------------------------------

    /** Hafif onay — kutucuk işaretleme, sekme değişimi, seçim. */
    fun dokunus(view: View?) {
        if (view == null || !acik(view.context)) return
        runCatching {
            val sabit = if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(sabit)
        }.onFailure { android.util.Log.w(TAG, "dokunus", it) }
    }

    /** Çok hafif — kaydırma eşiği geçilirken, sürükleme sırasında. */
    fun tik(view: View?) {
        if (view == null || !acik(view.context)) return
        runCatching {
            val sabit = if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.CLOCK_TICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(sabit)
        }.onFailure { android.util.Log.w(TAG, "tik", it) }
    }

    /** Uzun basma başladı. */
    fun uzunBasma(view: View?) {
        if (view == null || !acik(view.context)) return
        runCatching { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
            .onFailure { android.util.Log.w(TAG, "uzunBasma", it) }
    }

    /** Ret / geçersiz işlem. */
    fun ret(view: View?) {
        if (view == null || !acik(view.context)) return
        runCatching {
            if (Build.VERSION.SDK_INT >= 30) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                desen(view.context, longArrayOf(0, 28, 70, 28))
            }
        }.onFailure { android.util.Log.w(TAG, "ret", it) }
    }

    // ------------------------------------------------------------------
    // 2. Katman: özel desenler (Vibrator üzerinden)
    // ------------------------------------------------------------------

    /** Doğru cevap — tek net vuruş. */
    fun dogru(context: Context?) {
        context ?: return
        if (!acik(context)) return
        tekVurus(context, 32, 190)
    }

    /** Yanlış cevap — iki kısa vuruş (olumsuz his). */
    fun yanlis(context: Context?) {
        context ?: return
        if (!acik(context)) return
        desen(context, longArrayOf(0, 26, 90, 26))
    }

    /** Başarı / hedef tamamlandı — artan üçlü. */
    fun basari(context: Context?) {
        context ?: return
        if (!acik(context)) return
        desen(context, longArrayOf(0, 22, 60, 30, 60, 48))
    }

    /** Sayaç bitti — güçlü uyarı (bu, ayar kapalıyken de çalışır: alarm niteliğinde). */
    fun uyari(context: Context?) {
        context ?: return
        desen(context, longArrayOf(0, 220, 140, 220, 140, 320), zorla = true)
    }

    /** Sayaç başladı/durdu. */
    fun sayacDurum(context: Context?) {
        context ?: return
        if (!acik(context)) return
        tekVurus(context, 40, 160)
    }

    // ------------------------------------------------------------------
    // Alt seviye
    // ------------------------------------------------------------------

    private fun tekVurus(context: Context, sureMs: Long, siddet: Int) {
        runCatching {
            val v = motor(context) ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(sureMs, siddet.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(sureMs)
            }
        }.onFailure { android.util.Log.w(TAG, "tekVurus", it) }
    }

    private fun desen(context: Context, desen: LongArray, zorla: Boolean = false) {
        if (!zorla && !acik(context)) return
        runCatching {
            val v = motor(context) ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(desen, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(desen, -1)
            }
        }.onFailure { android.util.Log.w(TAG, "desen", it) }
    }

    private fun motor(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            val yonetici = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            yonetici?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }?.takeIf { it.hasVibrator() }
    }.getOrNull()

    private fun acik(context: Context): Boolean = GorunumAyar.haptikAcik(context)
}
