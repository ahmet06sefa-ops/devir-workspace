package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * v10.42 — Kullanıcı maddesi #6: akşam planı alarmı tetiklenince
 * "Yarın ne yapmak istersin?" bildirimi düşer ve alarm bir sonraki
 * güne yeniden kurulur (tek atımlık alarm deseni).
 */
class AksamReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, i: Intent) {
        if (i.action != PlanAsistan.ACTION_AKSAM) return
        if (PlanAsistan.aksamAcik(c)) GunlukBildirim.aksam(c)
        PlanAsistan.kur(c)
    }
}
