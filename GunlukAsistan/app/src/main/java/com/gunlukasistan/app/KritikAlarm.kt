package com.gunlukasistan.app

/**
 * v10.15 · ULTRA-30 / C13 — Tam ekran görev alarmının saf kararları.
 *
 * ── Tarama kanıtı ──
 * Görev hatırlatıcısı v5.2'den beri düz bildirimdi (`ReminderReceiver`:
 * "Tamamlandı" + "15 dk ertele"). `ZorunluUyari` (v7.56) ALARM kanalıyla
 * sessizde bile ses çıkarıyor ama hiçbir görev bu mekaniği kullanmıyordu
 * ve tam ekran (kilit üstü) açılış yoktu.
 *
 * ── Kapsam ──
 * Yalnızca kullanıcının 🔴 "acil" etiketi verdiği görevler tam ekran
 * açılır — `USE_FULL_SCREEN_INTENT` izninin kötüye kullanımını önlemek
 * için bilinçli daraltma (dürüst not: izin dokümanında "yalnız alarm
 * niteliğinde" denir; her görevde açmak uygulama mağazasının da
 * istemediği davranıştır).
 *
 * ── Kademeli erteleme + bedel ──
 * 1. erteleme 5 dk, 2. 10 dk, 3. 15 dk. 3. sınırdan SONRAKİ erteleme
 * isteği "bedel"e düşer: görev o gün artık hatırlatılmaz, yarının sabah
 * penceresine (09:00) taşınır ve erteleme sayacı sıfırlanır. Bedelin
 * amacı caydırıcılık değil karar yorgunluğunu kırmak: sonsuz 5 dk
 * erteleme döngüsü "kritik" sıfatını anlamsızlaştırır.
 *
 * Tüm fonksiyonlar framework'süzdür; birim testleri tabloyu kilitler.
 */
object KritikAlarm {

    /** En fazla bu kadar kademeli erteleme yapılır; sonrası bedel. */
    const val MAKS_ERTELEME: Int = 3

    /** Bedel sonrası görevin taşındığı sabah saati (dakika, 09:00). */
    const val BEDEL_SABAH_DK: Int = 9 * 60

    /**
     * n'inci ertelemenin süresi (dakika). n 1'den başlar.
     * Kademe: 5 · 10 · 15. Maks'ı aşan n'lerde bedel söz konusu olduğu
     * için burada artık dakika üretilmez (0 döner).
     */
    fun ertelemeDakikasi(n: Int): Int = when {
        n <= 0 -> 0
        n == 1 -> 5
        n == 2 -> 10
        n == 3 -> 15
        else -> 0
    }

    /** Bu erteleme isteği bedele mi düşüyor? (n = kaçıncı istek, 1'den) */
    fun bedelGerekliMi(n: Int): Boolean = n > MAKS_ERTELEME

    /** Toplam erteleme tavanı (dk): 5+10+15 = 30. */
    const val TOPLAM_TAVAN_DK: Int = 30

    /**
     * Bir sonraki alarm anı (ms).
     * bedelGerekliMi true ise ertesi gün 09:00, değilse simdi + kademe.
     * Saf aritmetik; gün başı hesabı çağıran taraftan ms cinsinden verilir
     * (`gunBasiMs` = bugün 00:00'ın ms'i — takvim bilgisi dışarıda kalır).
     */
    fun sonrakiUyariMs(
        ertelemeIstegi: Int,
        simdiMs: Long,
        gunBasiMs: Long,
    ): Long {
        if (bedelGerekliMi(ertelemeIstegi)) {
            return gunBasiMs + 24L * 3_600_000L + BEDEL_SABAH_DK * 60_000L
        }
        return simdiMs + ertelemeDakikasi(ertelemeIstegi) * 60_000L
    }
}
