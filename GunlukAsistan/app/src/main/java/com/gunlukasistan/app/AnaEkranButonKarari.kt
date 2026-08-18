package com.gunlukasistan.app

import android.view.View

/**
 * v10.59 — Ana Ekran Sadeleştirme ve Buton Açma/Kapama Karar Motoru.
 *
 * Kullanıcının talep ettiği "Ana ekrandaki bunu ayarlardan açma kapama yeri koy,
 * istediğim zaman eskisi gibi ekrana döneyim" fonksiyonunu yöneten saf mantık katmanı.
 */
object AnaEkranButonKarari {

    val ATOLYE_BUTON_IDLERI = listOf(
        "openManuelKontrol",
        "openOtonomMerkez",
        "openTasarimAtolye",
        "openKarne",
        "openYasamModulleri",
        "openGelismiAtolye",
        "openUzmanModuller",
        "openDersKolaylik",
        "openDersIleriFaz",
        "openDersUzmanMerkez",
        "openYasamSaglikFinans",
        "openDersUzmanFaz6",
        "openYasamSaglikFinansFaz3",
        "openEvrenselOtonomMerkez",
        "openAkilliGundemMerkezi",
        "openNamazAylikYonetim",
        "openBinMaddeAtolye",
        "openGorunumAtolye",
        "openKisiselGelisimAtolye",
        "openCanvaAtolye"
    )

    fun aktifButonIdleri(atolyeGosterMi: Boolean): List<String> {
        val temel = listOf("openTimerMenu", "openSettings")
        return if (atolyeGosterMi) {
            temel + ATOLYE_BUTON_IDLERI
        } else {
            temel
        }
    }

    fun durumMetniGetir(atolyeGosterMi: Boolean): String {
        return if (atolyeGosterMi) {
            "🏠 Ana Ekran Butonları: AÇIK (10 Kısayol Gösteriliyor)"
        } else {
            "🏠 Ana Ekran Butonları: KAPALI (Orijinal Sade & Minimalist Görünüm)"
        }
    }

    fun altMetinGetir(atolyeGosterMi: Boolean): String {
        return if (atolyeGosterMi) {
            "Ana ekranda tüm atölye kısayol butonları gösteriliyor (10 ikon)"
        } else {
            "Ana ekran orijinal sade & minimalist görünüme döndü (2 ikon: Sayaç & Ayarlar)"
        }
    }

    fun butonGorunurlukKarari(atolyeGosterMi: Boolean): Int {
        return if (atolyeGosterMi) View.VISIBLE else View.GONE
    }
}
