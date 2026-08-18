package com.gunlukasistan.app

/**
 * v10.15 · ULTRA-30 / C14 — Bildirim türü başına sessiz pencere.
 *
 * ── Tarama kanıtı ──
 * Sessiz saatler TEK global pencereydi (`BildirimMerkezi`: K_SESSIZ_ACIK/
 * BAS/BIT) ve gönderim kapılarında da yalnızca haftalık rapor buna
 * bakıyordu (`WeeklyReportReceiver:43`). Tür başına aralık ve hafta sonu
 * ayrımı yoktu.
 *
 * ── Model ──
 * Dört tür: GÖREV · SAYAÇ · RAPOR · MOTİVASYON (kalan kanallar "diğer"
 * kovasına düşer ve yalnız global pencereye bakar — dürüst kapsam).
 * Her tür için: kapalı / baş–bitiş (dakika) / hafta sonu ayrı mı +
 * hafta sonu baş–bitiş. Tür penceresi tanımsızsa (kapalıysa) o tür için
 * KARARI GLOBAL pencere verir — geriye doğru uyum korunur.
 *
 * Saf bölge birim testlidir ([sessizdeMi]); pref köprüsü altta.
 */
object SessizTurler {

    enum class Tur { GOREV, SAYAC, RAPOR, MOTIVASYON, DIGER }

    /** Türün dakika-cinsinden penceresi. bas == bit → pencere YOK demektir. */
    data class Pencere(
        val acik: Boolean = false,
        val bas: Int = 0,
        val bit: Int = 0,
        val haftaSonuAyrimi: Boolean = false,
        val hsBas: Int = 0,
        val hsBit: Int = 0,
    )

    /**
     * Gece yarısını aşan aralıkları destekler (örn. 22:00–07:00).
     * bas == bit → pencere yok (false). bas < bit → gün içi [bas, bit).
     * bas > bit → sarmalı: [bas, 24:00) ∪ [00:00, bit).
     */
    fun sessizdeMi(dakika: Int, bas: Int, bit: Int): Boolean {
        if (bas == bit) return false
        val dk = ((dakika % 1440) + 1440) % 1440
        return if (bas < bit) dk in bas until bit else dk >= bas || dk < bit
    }

    /** Tür kararı: o anki gün/hafta sonu durumuna göre pencere seçimi. */
    fun turSessizdeMi(p: Pencere, dakika: Int, haftaSonuMu: Boolean, globalBas: Int, globalBit: Int): Boolean {
        if (!p.acik) return sessizdeMi(dakika, globalBas, globalBit)
        return if (p.haftaSonuAyrimi && haftaSonuMu) {
            sessizdeMi(dakika, p.hsBas, p.hsBit)
        } else {
            sessizdeMi(dakika, p.bas, p.bit)
        }
    }

    /** Kanal kimliğinden tür çıkarımı (kapılar tek fonksiyonda birleşir). */
    fun kanaldanTur(kanalId: String?): Tur = when (kanalId) {
        "gorev_hatirlatici_v1", "ch_hatirlatici_v2" -> Tur.GOREV
        "ch_zamanlayici_v2", "ch_zaman_bitis_v2" -> Tur.SAYAC
        "ch_rapor_v2" -> Tur.RAPOR
        "ch_motivasyon_v2" -> Tur.MOTIVASYON
        else -> Tur.DIGER
    }

    // ── Pref köprüsü ────────────────────────────────────────────────

    private const val PREF = "sessiz_turler_v1"

    private fun key(t: Tur) = when (t) {
        Tur.GOREV -> "gorev"; Tur.SAYAC -> "sayac"; Tur.RAPOR -> "rapor"
        Tur.MOTIVASYON -> "motivasyon"; Tur.DIGER -> "diger"
    }

    fun oku(context: android.content.Context, t: Tur): Pencere {
        val sp = context.getSharedPreferences(PREF, 0)
        val k = key(t)
        return Pencere(
            acik = sp.getBoolean("${k}_acik", false),
            bas = sp.getInt("${k}_bas", 23 * 60),
            bit = sp.getInt("${k}_bit", 7 * 60),
            haftaSonuAyrimi = sp.getBoolean("${k}_hs", false),
            hsBas = sp.getInt("${k}_hsbas", 0),
            hsBit = sp.getInt("${k}_hsbit", 9 * 60),
        )
    }

    fun yaz(context: android.content.Context, t: Tur, p: Pencere) {
        val k = key(t)
        context.getSharedPreferences(PREF, 0).edit()
            .putBoolean("${k}_acik", p.acik)
            .putInt("${k}_bas", p.bas).putInt("${k}_bit", p.bit)
            .putBoolean("${k}_hs", p.haftaSonuAyrimi)
            .putInt("${k}_hsbas", p.hsBas).putInt("${k}_hsbit", p.hsBit)
            .apply()
    }

    /**
     * Gönderim kapılarının çağırdığı TEK karar: bu kanal şu an susturulmalı mı?
     * Global pencere `BildirimMerkezi`nden okunur; tür tanımlıysa tablo,
     * değilse global karar verir.
     */
    fun susturMu(context: android.content.Context, kanalId: String?): Boolean {
        val t = kanaldanTur(kanalId)
        val simdi = java.util.Calendar.getInstance()
        val dk = simdi.get(java.util.Calendar.HOUR_OF_DAY) * 60 + simdi.get(java.util.Calendar.MINUTE)
        val hs = simdi.get(java.util.Calendar.DAY_OF_WEEK).let {
            it == java.util.Calendar.SATURDAY || it == java.util.Calendar.SUNDAY
        }
        val (gBas, gBit) = BildirimMerkezi.globalSessizPencere(context)
        return turSessizdeMi(oku(context, t), dk, hs, gBas, gBit)
    }
}
