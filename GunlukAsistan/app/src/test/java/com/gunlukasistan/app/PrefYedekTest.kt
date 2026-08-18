package com.gunlukasistan.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.25 — PrefYedek tarama kapısı [PrefYedek.yedegeGirerMi] testleri.
 *
 * 🔴 Denetim bağlamı:
 * v7.98'den beri yedek, sabit listedeki 19 depoyla sınırlıydı; yeni
 * eklenen modüller (Ekran Atölyesi, Widget Atölyesi, kilit_v1...)
 * yedeğe hiç girmiyordu. v10.25 ile `shared_prefs` klasörü taranıyor
 * ve HARIC dışındaki HER depo yedeğe giriyor.
 *
 * Bu testler iki yönde de garanti verir:
 *   1. Kullanıcı verisi olan depolar ASLA dışarıda kalmaz (kayıp = felaket)
 *   2. Oturum/hassas/türetilmiş depolar ASLA içeri girmez (sapık geri yükleme)
 */
class PrefYedekTest {

    // ── Kullanıcı verisi — yedeğe GİRMELİ ────────────────────────────

    @Test
    fun kullaniciVerisi_eskidenYedeklenenler_kapisAck() {
        // v7.98 listesi — gerileme olmasın
        listOf(
            "mufredat_v1", "hatalarim_v1", "sozluk_v1", "pomodoro_v1",
            "odak_kaydi_v1", "hafta_plan_v1", "sayfa_imi_v1", "koc_v1",
            "kanit_v1", "sayac_ayar_v1", "okuma_ayar_v1", "yedek_sifre_v1",
            "widget_tema_v1", "bildirim_ayar_v1", "gunluk_asistan_gorunum",
            "konu_gorunum_v1", "takip_v1", "butce_v1", "konum_hatirlatma_v1"
        ).forEach {
            assertTrue("$it yedeğe girmeli", PrefYedek.yedegeGirerMi(it))
        }
    }

    @Test
    fun kullaniciVerisi_yeniModuller_artikKapsaniyor() {
        // 🔴 v10.25'te kapanan boşluk: bu depolar hiç yedeklenmiyordu
        listOf(
            "kilit_v1",              // v10.22 PIN kilidi ayarları
            "ana_ekran_duzen_v1",    // v10.18 Ekran Atölyesi
            "widget_atolye_v1",      // v10.16 Widget Atölyesi
            "widget_modul_v1", "widget_eylem_v1", // v10.20
            "wg_dokunma_v1", "wg_filtre_v1", "wg_liste_v1", "wg_secim_v1",
            "bildirim_zaman_v1",     // kullanıcının bildirim saatleri
            "sessiz_turler_v1",      // v10.15 sessiz tür tercihi
            "ge_mikro_gunluk_v1",    // günlük yazıları (İÇERİK)
            "ge_sesli_kutu_v1",      // sesli not METİNLERİ (İÇERİK)
            "basari_kayit_v1", "maskot_gardrop_v1",
            "fo_manzara_v1", "fo_odak_kalkani_v1", "fo_hayalet_v1",
            "uyku_cerceve_v1", "namaz_bildirim_v1", "kurs_hatirlatici",
            "olcme_test_v1", "soru_coz_v1", "feynman_v1",
            "pdf_okuma_v1", "konu_tekrar_v1", "ogrenen_hatirlatici_v1",
            "takvim_v1", "sesli_liste_v1"
        ).forEach {
            assertTrue("$it yedeğe girmeli", PrefYedek.yedegeGirerMi(it))
        }
    }

    @Test
    fun kullaniciVerisi_bilinmeyenYeniModul_varsayilanIc() {
        // 🔑 Sistemin asıl vaadi: gelecekte eklenecek, kodun hiç
        // tanımadığı bir modül adı da yedeğe GİRMELİ (kara liste mantığı)
        assertTrue(PrefYedek.yedegeGirerMi("gelecekteki_modul_v42"))
        assertTrue(PrefYedek.yedegeGirerMi("x"))
    }

    @Test
    fun buyukDepo_kapidanGecer() {
        // konu_anlatim_v1 kapıdan geçer; varsayılan taramaya girmemesi
        // tarama katmanının (BUYUK) işi, sınıflandırıcının değil.
        // Geri yükleme tarafında bu geçiş kritik: şifreli/ekli yedekteki
        // anlatımlar içeri alınabilsin.
        assertTrue(PrefYedek.yedegeGirerMi("konu_anlatim_v1"))
    }

    // ── Hassas / oturum / türetilmiş — yedeğe GİRMEMELİ ─────────────

    @Test
    fun haric_hassas_kapali() {
        assertFalse(PrefYedek.yedegeGirerMi("ai_settings"))
    }

    @Test
    fun haric_oturumDurumu_kapali() {
        // Çalışan sayaç/ileri sayım eski damgayla geri yüklenirse
        // saatlerce sapık süre gösterir — oturum cihazda kalır.
        listOf(
            "timer_engine_v1", "ileri_sayim_v1", "sayac_zincir_v1",
            "wg_ay_ofset_v1", "kritik_alarm_v1"
        ).forEach {
            assertFalse("$it girmemeli", PrefYedek.yedegeGirerMi(it))
        }
    }

    @Test
    fun haric_anaDepoVeAyriKanallar_kapali() {
        // exportJson bunları ayrıca yazıyor; PrefYedek'ten ikinci kez
        // yazılırsa çift-yazım çakışma riski doğar.
        // 🔴 ANA_DEPO v7.98'de yanlış ada işaret ediyordu
        // ("gunluk_asistan_prefs"); gerçek ana depo gunluk_asistan_store.
        listOf(
            "gunluk_asistan_store", "gunluk_asistan_prefs",
            "quiz_store", "kart_store", "ogretmen_store",
            "namaz_v1", "namaz_plan_v1", "zorunlu_uyari_v1",
            "ai_sohbet_v1", "film_v1", "online_v1"
        ).forEach {
            assertFalse("$it girmemeli", PrefYedek.yedegeGirerMi(it))
        }
    }

    @Test
    fun haric_turetilmisVeTani_kapali() {
        listOf(
            "ai_model_cache", "ai_onbellek_v1", "crash_log",
            "veri_gecis_v1", "guncelleme_v1", "arkaplan_is_v1",
            "online_bekci_v1", "kullanim_v1",
            "alarm_sagligi_v1", "bildirim_tani", "bildirim_test_v1",
            "rekor_bildirim_v1", "rozet_bildirim_v1", "yedek_rotasyon_v1"
        ).forEach {
            assertFalse("$it girmemeli", PrefYedek.yedegeGirerMi(it))
        }
    }

    @Test
    fun haric_sistemCopleri_onekleKapaniyor() {
        // shared_prefs taraması sistem kütüphanelerinin dosyalarını da
        // görebilir (WorkManager id defteri gibi) — bunlar yedeğe giremez.
        assertFalse(PrefYedek.yedegeGirerMi("androidx.work.util.id"))
        assertFalse(PrefYedek.yedegeGirerMi("androidx.startup"))
        assertFalse(PrefYedek.yedegeGirerMi("com.google.android.gms.appid"))
        assertFalse(PrefYedek.yedegeGirerMi("WebViewChromiumPrefs"))
    }

    @Test
    fun sinir_bosVeBeyazlik_kapali() {
        assertFalse(PrefYedek.yedegeGirerMi(""))
        assertFalse(PrefYedek.yedegeGirerMi("   "))
    }

    // ── Simetri ilkesi ───────────────────────────────────────────────

    @Test
    fun simetri_hariclerBirbirineKarismaz() {
        // Bir ad ne tam kara listede ne de gizli istisnada çift kayıtlı
        // olsun: kapı tek yerden, yedegeGirerMi'den geçiyor. Bu test
        // sınıfındaki "girmeli" ve "girmemeli" listeleri kesişirse
        // biri yanlış demektir.
        val girmeli = listOf(
            "mufredat_v1", "kilit_v1", "ana_ekran_duzen_v1",
            "widget_atolye_v1", "bildirim_zaman_v1", "ge_mikro_gunluk_v1",
            "konu_anlatim_v1", "gelecekteki_modul_v42"
        )
        val girmemeli = listOf(
            "ai_settings", "timer_engine_v1", "gunluk_asistan_store",
            "quiz_store", "crash_log", "androidx.work.util.id"
        )
        assertTrue(
            "listeler kesişiyor: ${girmeli intersect girmemeli.toSet()}",
            (girmeli intersect girmemeli.toSet()).isEmpty()
        )
    }
}
