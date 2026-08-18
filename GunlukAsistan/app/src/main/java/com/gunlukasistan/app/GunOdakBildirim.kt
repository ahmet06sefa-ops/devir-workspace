package com.gunlukasistan.app

import android.content.Context

/**
 * v10.3 · Öneri B23 — Sabah bildirimine "günün odağı" satırı.
 *
 * ── Dürüst not ──
 * Sabah turu (`BildirimZamanlayici` 09:00) ve odak motoru
 * (`GunOdak`, v9.9 hero kart) zaten vardı. Eksik olan, sabah
 * bildirimlerinin hiçbirinde "bugünün TEK önemli işi şu"
 * denmemesiydi: sabah turu kart/sınav/rapor hatırlatıyor ama
 * öncelik kurmuyordu. Bu sınıf ikisini bağlar — yeni bir alarm
 * zinciri KURULMUYOR, mevcut sabah turuna bir tür ekleniyor.
 *
 * ── Neden yeni Tur ──
 * `Tur.ODAK_ONERI` "verimli saatin yaklaşıyor" bildirimi (varsayılan
 * kapalı). Günün odağı farklı bir söz: "şu iş, şimdi". Ayrı tür
 * olunca kullanıcı ikisini bağımsız açıp kapatabilir.
 *
 * ── Saf bölge ──
 * [metin] ve [gonderilmeli] saf; birim testli.
 */
object GunOdakBildirim {

    // 7020-7024 BildirimUretici'de dolu — çakışmamak için 7026.
    const val BILDIRIM_ID = 7026

    /**
     * Başlık ve gövde üretir.
     *
     * Başlık selamlamayı taşır ("Günaydın ☀️"), içerik tek odak
     * cümlesidir — hero karttakiyle aynı kaynak, aynı öncelik.
     */
    fun baslik(context: Context, selamlama: String): String =
        context.getString(R.string.n_godak_title, selamlama)

    fun govde(emoji: String, odakMetin: String): String =
        if (emoji.isBlank()) odakMetin else "$emoji $odakMetin"

    /**
     * Bildirim gönderilmeli mi?
     *
     * `GOREV` gibi sabah turu 7 bildirim üretebiliyor; odak bildirimi
     * yalnızca gerçekten bir öncelik varsa çıkar. Boş güne zorlama
     * "günün odağı: dinlen" uydurmuyoruz — sessiz kalmak da bilgidir.
     */
    fun gonderilmeli(odakVarMi: Boolean, bugunGonderildi: Boolean): Boolean =
        odakVarMi && !bugunGonderildi

    /** Sabah turundan çağrılır. Başarıyla çıkarsa true. */
    fun dene(context: Context): Boolean {
        val odak = try {
            GunOdak.bul(context)
        } catch (e: Exception) {
            android.util.Log.w("GunOdakBildirim", "Odak bulunamadı", e)
            null
        }
        // Kapı tek yerde: test edilen [gonderilmeli] kararıdır.
        if (!gonderilmeli(
                odak != null,
                BildirimMerkezi.bugunGonderildiMi(context, "godak")
            )
        ) {
            return false
        }
        val secilen = odak ?: return false

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.GUN_ODAK,
            BILDIRIM_ID,
            baslik(context, GunOdak.selamlama(context)),
            govde(secilen.emoji, secilen.metin),
            acilisIntent = if (secilen.ekranIndeksi >= 0) {
                BildirimMerkezi.ekranAc(context, secilen.ekranIndeksi, BILDIRIM_ID)
            } else {
                secilen.aktivite?.let { BildirimMerkezi.aktiviteAc(context, it, BILDIRIM_ID) }
                    ?: BildirimMerkezi.ekranAc(context, 0, BILDIRIM_ID)
            }
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "godak")
        return gonderildi
    }
}
