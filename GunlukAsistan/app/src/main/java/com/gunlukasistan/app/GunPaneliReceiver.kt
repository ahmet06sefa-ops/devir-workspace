package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * v10.15 · ULTRA-30 / C15 — Gün paneli "Kapat" aksiyonu.
 * Anahtarı söndürür ve kalıcı bildirimi kaldırır.
 */
class GunPaneliReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_KAPAT) {
            GunPaneli.ayarla(context, false)
        }
    }

    companion object {
        const val ACTION_KAPAT = "com.gunlukasistan.app.GUN_PANELI_KAPAT"
    }
}
