package com.gunlukasistan.app

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * v10.2 · Öneri A14 — Bitişte flaş çakması.
 *
 * ── Neden ──
 * Kütüphane, sınıf, toplantı: ses kapalı, titreşim hissedilmedi —
 * ışık kalıyor. Samsung Saat'in flaş uyarısının karşılığı.
 *
 * ── Sınırlar (dürüst) ──
 *   · Flaşı olmayan cihazda sessizce devre dışı kalır
 *   · `setTorchMode` kamera izni GEREKTİRMEZ (API 23+ istisnası);
 *     ama kamera başka uygulamadayken açılamaz — o an atlanır
 *   · Varsayılan kapalı; kullanıcı ayarlardan açar ([SayacAyar.flasBildirim])
 *
 * Desen: 3 kısa çak (150 ms açık / 250 ms kapalı). Kalıcı açık
 * bırakma yok; her yol kapatmayla biter.
 */
object SayacFlas {

    private const val CAKIM_SAYISI = 3
    private const val ACIK_MS = 150L
    private const val KAPALI_MS = 250L

    fun cal(context: Context) {
        if (!SayacAyar.flasBildirim(context)) return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return
        runCatching {
            val yonetici = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val kimlik = yonetici.cameraIdList.firstOrNull() ?: return@runCatching
            val handler = Handler(Looper.getMainLooper())
            var adim = 0
            val is_ = object : Runnable {
                override fun run() {
                    runCatching {
                        val acikMi = adim % 2 == 0
                        yonetici.setTorchMode(kimlik, acikMi)
                        adim++
                        if (adim < CAKIM_SAYISI * 2) {
                            handler.postDelayed(this, if (acikMi) ACIK_MS else KAPALI_MS)
                        } else {
                            // Her yol kapanışla biter — flaş asla açık kalmasın
                            runCatching { yonetici.setTorchMode(kimlik, false) }
                        }
                    }
                }
            }
            handler.post(is_)
        }.onFailure {
            android.util.Log.w("SayacFlas", "Flaş çakılamadı", it)
        }
    }
}
