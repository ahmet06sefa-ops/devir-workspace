package com.gunlukasistan.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * v10.41 — Kullanıcı maddesi #1: bildirimdeki Bekle/Devam/Bitir düğmeleri.
 *
 * Ekran kapalıyken bile çalışır; durum [IleriSayim] deposunda yaşadığı
 * için bildirimden yapılan geçiş uygulama açıldığında birebir görünür.
 *
 * Bitir kuralı: 1 dakikadan kısa oturumlar kaydedilmeden temizlenir
 * (ekrandaki "çok kısa" davranışının aynısı). ≥8 saatlik dev oturumlarda
 * ekran onay ister; bildirimden bitirmede ise kurtarma önceliklidir —
 * kayıt yapılır (bilinçli sınır, sürüm notlarında beyan edilir).
 */
class IleriSayimReceiver : BroadcastReceiver() {

    override fun onReceive(c: Context, i: Intent) {
        val simdi = System.currentTimeMillis()
        when (i.action) {
            IleriSayimBildirim.ACTION_BEKLET -> {
                IleriSayim.anaDugme(c, simdi)
                IleriSayimBildirim.tazele(c)
            }
            IleriSayimBildirim.ACTION_BITIR -> {
                if (IleriSayim.calismakta(c)) IleriSayim.anaDugme(c, simdi)
                val dk = IleriSayim.bekleyenDakika(c, simdi)
                if (dk >= 1) {
                    Store.addTodayFocusMinutes(c, dk)
                    IleriSayim.gecmiseIsle(c, simdi, dk)
                    runCatching { WidgetCommon.refreshAll(c, false) }
                    Toast.makeText(
                        c, c.getString(R.string.w41_ileri_kaydedildi, dk),
                        Toast.LENGTH_LONG
                    ).show()
                }
                IleriSayim.sifirla(c)
                IleriSayimBildirim.gizle(c)
            }
        }
    }
}
