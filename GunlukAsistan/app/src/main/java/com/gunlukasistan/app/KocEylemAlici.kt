package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * v7.78 — Koç bildirimindeki düğmelerin alıcısı.
 *
 * Ayrı bir alıcı olmasının sebebi: [KocZamanlayici] alarm tetiklerini
 * işler ve her tetikten sonra kendini yeniden kurar. Düğme basışları
 * alarm değil; oraya karışırsa gereksiz alarm kurulumu tetiklenir.
 */
class KocEylemAlici : BroadcastReceiver() {

    companion object {
        const val ACTION_ERTELE = "com.gunlukasistan.app.KOC_E_ERTELE"

        /** Erteleme süresi — sertliğe göre kısalır. */
        private fun ertelemeDk(context: Context): Int = when (Koc.sertlik(context)) {
            Koc.SERT_NAZIK -> 30
            Koc.SERT_ACIMASIZ -> 10
            else -> 15
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ERTELE -> {
                val dk = ertelemeDk(context)
                KocZamanlayici.ertele(context, dk)
                runCatching {
                    NotificationManagerCompat.from(context)
                        .cancel(KocZamanlayici.NOTIF_CALIS)
                }
                // Erteleme sayısını kaydet — hesap ekranında yüzüne söylenecek
                val p = context.getSharedPreferences("koc_v1", Context.MODE_PRIVATE)
                p.edit().putInt("bugun_erteleme", p.getInt("bugun_erteleme", 0) + 1).apply()

                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.koc_ertelendi, dk),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
