package com.gunlukasistan.app

/**
 * v11.13 — Adım adım görünür eylem yürütücüsü (SAF MOTOR, JVM testli).
 *
 * Kullanıcı isteği: "Yapay zeka ekrana dokunabilme yetkisi olsun; sesli komut
 * vereyim, AI ekrandan benim için gerçekleştirsin ve hepsini tek tek göreyim."
 *
 * AI'nın ürettiği komut listesini, kullanıcının HER adımı tek tek görebileceği
 * ve onaylayabileceği sıralı bir eylem planına çevirir. Her adımın bir
 * açıklaması (Türkçe, insan tarafından okunur) ve durumu vardır.
 *
 *  · [adimlaraCevir] — komut listesini [EylemAdim] listesine çevirir.
 *  · [aciklama] — bir komutu kısa, gösterilebilir Türkçe metne çevirir.
 *  · [siradaki] / [tamamla] / [gec] — planın hangi adımda olduğunu yönetir.
 *
 * Android'e bağımlı UI katmanı `AsistanFragment` içindedir; bu nesne saf
 * mantığı taşır ve birim testlidir.
 */
object AdimliEylemMotoru {

    /** Tek bir eylem adımı. */
    data class EylemAdim(
        val komut: AsistanKomut.Komut,
        val aciklama: String
    )

    /** Plan durumu: sıradaki adım indeksi + tamamlanan adım sayısı. */
    data class Durum(val siradaki: Int, val toplam: Int) {
        val bitti: Boolean get() = siradaki >= toplam
    }

    /**
     * Komut listesini adımlara çevirir. Boş liste boş plan verir.
     * Her adım insan-okunur bir açıklama taşır (kullanıcı ne olacağını görür).
     */
    fun adimlaraCevir(komutlar: List<AsistanKomut.Komut>): List<EylemAdim> =
        komutlar.map { EylemAdim(it, aciklama(it)) }

    /** Sıradaki adım indeksi (henüz başlamamış planda 0). */
    fun siradaki(plan: List<EylemAdim>): Durum = Durum(0, plan.size)

    /** Bir adım tamamlanınca sonraki adıma geç. */
    fun tamamla(durum: Durum): Durum = Durum((durum.siradaki + 1).coerceAtMost(durum.toplam), durum.toplam)

    /** Kullanıcı adımı atlarsa sonraki adıma geç. */
    fun gec(durum: Durum): Durum = tamamla(durum)

    /**
     * Bir komutu kısa, kullanıcının görebileceği Türkçe açıklamaya çevirir.
     * Bilinmeyen komut → "İşlemi gerçekleştir".
     */
    fun aciklama(komut: AsistanKomut.Komut): String {
        val deger = komut.deger.trim()
        val hedef = deger.split("::").firstOrNull()?.trim().orEmpty().take(40)
        return when (komut.ad) {
            "gorev_ekle" -> "Görev ekle: $hedef"
            "gorev_tamamla" -> "Görevi tamamla: $hedef"
            "gorev_sil" -> "Görevi sil: $hedef"
            "gorev_duzenle" -> "Görevi düzenle: $hedef"
            "not_ekle" -> "Not ekle: $hedef"
            "not_sil" -> "Not sil: $hedef"
            "not_duzenle" -> "Not düzenle: $hedef"
            "konu_ekle" -> "Konu ekle: $hedef"
            "konu_sil" -> "Konu sil: $hedef"
            "konu_duzenle" -> "Konu düzenle: $hedef"
            "alt_madde_ekle" -> "Alt madde ekle: $hedef"
            "alt_madde_tamamla" -> "Alt maddeyi tamamla: $hedef"
            "alt_madde_sil" -> "Alt madde sil: $hedef"
            "aliskanlik_ekle" -> "Alışkanlık ekle: $hedef"
            "aliskanlik_isaretle" -> "Alışkanlığı işaretle: $hedef"
            "aliskanlik_sil" -> "Alışkanlık sil: $hedef"
            "etkinlik_ekle" -> "Etkinlik ekle: $hedef"
            "etkinlik_sil" -> "Etkinlik sil: $hedef"
            "kurs_ekle" -> "Kurs ekle: $hedef"
            "ders_ekle" -> "Ders ekle: $hedef"
            "ders_tamamla" -> "Dersi tamamla: $hedef"
            "sinav_ekle" -> "Sınav ekle: $hedef"
            "kart_ekle" -> "Bilgi kartı ekle: $hedef"
            "hedef_ayarla" -> "Günlük hedefi ayarla: $hedef dk"
            "soz_ayarla" -> "Motivasyon sözünü değiştir"
            "ayar_ses" -> "Ses bildirimleri: ${if (AsistanKomut.evetMi(deger)) "açık" else "kapalı"}"
            "ayar_titresim" -> "Titreşim: ${if (AsistanKomut.evetMi(deger)) "açık" else "kapalı"}"
            "ayar_animasyon" -> "Animasyonlar: ${if (AsistanKomut.evetMi(deger)) "açık" else "kapalı"}"
            "ayar_namaz" -> "Namaz modülü: ${if (AsistanKomut.evetMi(deger)) "açık" else "kapalı"}"
            "ayar_gece" -> "Tema: $deger"
            "widget_yenile" -> "Widget'ları yenile"
            "ozet_ver" -> "Veri özetini göster"
            "zamanlayici" -> "Zamanlayıcı başlat: $deger dk"
            "ekran_ac" -> "Ekran aç: $hedef"
            "atolye_ac" -> "Atölye aç: $hedef"
            "uygulamalar_ac" -> "Uygulamalarım ekranını aç"
            "uygulama_ac" -> "Uygulamayı aç: $hedef"
            "namaz_ac" -> "Namaz ekranını aç"
            "film_ac" -> "Film önerisi ekranını aç"
            "analiz_ac" -> "Analiz ekranını aç"
            "pdf_ara" -> "PDF arama ekranını aç"
            "yedek_al" -> "Yedek al"
            "yedek_geri_al" -> "Son işlemi geri al"
            else -> if (hedef.isBlank()) "İşlemi gerçekleştir" else "İşlemi gerçekleştir: $hedef"
        }
    }
}
