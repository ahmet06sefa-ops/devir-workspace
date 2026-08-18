package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * v10.42 — Kullanıcı maddesi #5: ekran kilidinin gün içindeki İLK
 * açılışı "uyandım" kabul edilir (gece 04:00 öncesi elenir), günde
 * en fazla bir kez sabah plan bildirimi düşer.
 */
class SabahReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_USER_PRESENT) return
        if (!PlanAsistan.sabahAcik(c)) return
        val cal = Calendar.getInstance()
        val gunDk = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (!PlanAsistan.sabahPenceresiMi(gunDk)) return
        val gunAnahtar = AliskanlikMola.gunAnahtari(cal.timeInMillis)
        if (PlanAsistan.sabahGosterildiMi(c, gunAnahtar)) return
        PlanAsistan.sabahGosterildiIsle(c, gunAnahtar)
        val (toplam, bugun, gecikmis) = GunlukBildirim.gorevOlcum(c)
        GunlukBildirim.sabah(c, toplam, bugun, gecikmis, GunlukBildirim.gunBasliklari(c))
    }
}
