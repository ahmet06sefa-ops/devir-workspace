# 🗺️ Günlük Asistan — KOD ATLASI (özellik → dosya haritası)

_Bu dosya `atlas-uret.py` ile üretilir; elle değişen bölümler en alttadır. Amaç: geriye dönüşleri önlemek, aramadan kod yazmak, teslimi hızlandırmak._

## Envanter: **343 ana kaynak · 74 test dosyası**

| En büyük 15 dosya | satır |
|---|---|
| `Store.kt` | 2911 |
| `OnlineActivity.kt` | 2091 |
| `SettingsFragment.kt` | 1754 |
| `TimerFragment.kt` | 1842 |
| `AiClient.kt` | 1632 |
| `TakipActivity.kt` | 1650 |
| `MainActivity.kt` | 1536 |
| `TasksFragment.kt` | 1585 |
| `TopicsFragment.kt` | 1309 |
| `SayacAyarActivity.kt` | 1225 |
| `CoursesFragment.kt` | 1072 |
| `NotesFragment.kt` | 1003 |
| `KocActivity.kt` | 1107 |
| `AsistanKomut.kt` | 915 |
| `WidgetTemaActivity.kt` | 932 |

## SharedPreferences anahtarları (geri okuma/backup için kritik)

| dosya | odak | pref adı |
|---|---|---|
| `AiOnbellek.kt` | PREF | `ai_onbellek_v1` |
| `SohbetGecmisi.kt` | PREF | `ai_sohbet_v1` |
| `AlarmSagligi.kt` | PREF | `alarm_sagligi_v1` |
| `AliskanlikMola.kt` | PREF | `aliskanlik_mola_v1` |
| `AliskanlikNot.kt` | PREF | `aliskanlik_not_v1` |
| `AnaEkranDuzen.kt` | PREF | `ana_ekran_duzen_v1` |
| `BaglamProfili.kt` | PREF | `baglam_profili_v1` |
| `Basari.kt` | PREF | `basari_kayit_v1` |
| `BildirimMerkezi.kt` | PREF | `bildirim_ayar_v1` |
| `BildirimOzeti.kt` | PREF | `bildirim_ozeti_v1` |
| `BildirimZamanlayici.kt` | PREF | `bildirim_zaman_v1` |
| `BugunDuzen.kt` | PREF | `bugun_duzen_v1` |
| `Butce.kt` | PREF | `butce_v1` |
| `Mufredat.kt` | K_KAYIT | `ders_kayit_json` |
| `KartStore.kt` | K_DURUM | `durum_json` |
| `Feynman.kt` | PREF | `feynman_v1` |
| `FilmStore.kt` | PREF | `film_v1` |
| `Hayalet.kt` | PREF | `fo_hayalet_v1` |
| `SesManzarasi.kt` | PREF | `fo_manzara_v1` |
| `NefesActivity.kt` | PREF | `fo_nefes_v1` |
| `OdakKalkani.kt` | PREF | `fo_odak_kalkani_v1` |
| `OdakRitim.kt` | PREF | `fo_odak_ritim_v1` |
| `MikroGunluk.kt` | PREF | `ge_mikro_gunluk_v1` |
| `SabahPlani.kt` | PREF | `ge_sabah_plani_v1` |
| `SeneFilmi.kt` | PREF | `ge_sene_filmi_v1` |
| `SesliKutu.kt` | PREF | `ge_sesli_kutu_v1` |
| `CokmeRapor.kt` | K_GECMIS | `gecmis_json` |
| `GorevBekliyor.kt` | PREF | `gorev_bekliyor_v1` |
| `GorevBugunUstte.kt` | PREF | `gorev_bugun_ustte_v1` |
| `GorevErteleme.kt` | PREF | `gorev_erteleme_v1` |
| `GorevGorunum.kt` | PREF | `gorev_gorunum_v1` |
| `Koc.kt` | K_GUNLER | `gun_kayit_json` |
| `Kullanim.kt` | K_GUN_LOG | `gun_log_json` |
| `GunPaneli.kt` | PREF | `gun_paneli_v1` |
| `Guncelleme.kt` | PREF | `guncelleme_v1` |
| `GorunumAyar.kt` | PREF | `gunluk_asistan_gorunum` |
| `Simge.kt` | PREF | `gunluk_asistan_store` |
| `HaftaPlan.kt` | PREF | `hafta_plan_v1` |
| `Hatalarim.kt` | PREF | `hatalarim_v1` |
| `KonumHatirlatma.kt` | K_HATIRLATMALAR | `hatirlatmalar_json` |
| `IleriSayim.kt` | PREF_GECMIS | `ileri_sayim_gecmis_v1` |
| `IleriSayim.kt` | PREF | `ileri_sayim_v1` |
| `SayfaImi.kt` | K_IMLER | `imler_json` |
| `Butce.kt` | K_KALEMLER | `kalemler_json` |
| `Kanit.kt` | PREF | `kanit_v1` |
| `KartStore.kt` | K_KARTLAR | `kartlar_json` |
| `AiOnbellek.kt` | K_KAYITLAR | `kayitlar_json` |
| `KilitDepo.kt` | PREF | `kilit_v1` |
| `KocMesaj.kt` | PREF | `koc_mesaj_v1` |
| `Koc.kt` | PREF | `koc_v1` |
| `KonuUretici.kt` | PREF | `konu_anlatim_v1` |
| `KonuGorunum.kt` | PREF | `konu_gorunum_v1` |
| `KonuTekrar.kt` | PREF | `konu_tekrar_v1` |
| `KonumHatirlatma.kt` | PREF | `konum_hatirlatma_v1` |
| `Kullanim.kt` | PREF | `kullanim_v1` |
| `SesliListe.kt` | K_LISTE | `liste_json` |
| `MaskotGardrop.kt` | PREF | `maskot_gardrop_v1` |
| `Mufredat.kt` | PREF | `mufredat_v1` |
| `NamazBildirim.kt` | PREF | `namaz_bildirim_v1` |
| `NamazPlan.kt` | PREF | `namaz_plan_v1` |
| `NamazVakti.kt` | PREF | `namaz_v1` |
| `NotArsiv.kt` | PREF | `not_arsiv_v1` |
| `NotGorunum.kt` | PREF | `not_gorunum_v1` |
| `NotHatirlatici.kt` | PREF | `not_hatirlat_v1` |
| `NotKilit.kt` | PREF | `not_kilit_v1` |
| `NotRenk.kt` | PREF | `not_renk_v1` |
| `NotSabitle.kt` | PREF | `not_sabitle_v1` |
| `NotSurum.kt` | PREF | `not_surum_v1` |
| `OdakKaydi.kt` | PREF | `odak_kaydi_v1` |
| `Takip.kt` | K_ODEME_LOG | `odeme_log_json` |
| `OgrenenHatirlatici.kt` | PREF | `ogrenen_hatirlatici_v1` |
| `OkumaAyar.kt` | PREF | `okuma_ayar_v1` |
| `OlcmeBekleyen.kt` | PREF | `olcme_bekleyen_v1` |
| `OlcmeTest.kt` | PREF | `olcme_test_v1` |
| `Guncelleme.kt` | K_ONBELLEK | `onbellek_json` |
| `OnlineBekci.kt` | PREF | `online_bekci_v1` |
| `OnlineStore.kt` | PREF | `online_v1` |
| `OdakKaydi.kt` | K_OTURUMLAR | `oturumlar_json` |
| `OdakKalkani.kt` | K_PAKETLER | `paketler_json` |
| `LessonPdfActivity.kt` | PREF | `pdf_okuma_v1` |
| `PlanAsistan.kt` | PREF | `plan_asistan_v1` |
| `NamazPlan.kt` | K_PLAN | `plan_json` |
| `SureAnalizi.kt` | K_POMODORO | `pomodoro_json` |
| `Pomodoro.kt` | PREF | `pomodoro_v1` |
| `BildirimUretici.kt` | PREF_REKOR | `rekor_bildirim_v1` |
| `BildirimUretici.kt` | PREF_ROZET | `rozet_bildirim_v1` |
| `SayacAyar.kt` | PREF | `sayac_ayar_v1` |
| `SayacZincir.kt` | PREF | `sayac_zincir_v1` |
| `Kullanim.kt` | K_SAYAC | `sayaclar_json` |
| `SayfaImi.kt` | PREF | `sayfa_imi_v1` |
| `SesliListe.kt` | PREF | `sesli_liste_v1` |
| `SessizTurler.kt` | PREF | `sessiz_turler_v1` |
| `OgretmenStore.kt` | K_SEVIYE | `seviye_json` |
| `OlcmeTest.kt` | K_SIMULASYON | `simulasyon_json` |
| `KonumHatirlatma.kt` | K_SON_TETIK | `son_tetik_json` |
| `QuizStore.kt` | K_SONUC | `sonuclar_json` |
| `SoruCoz.kt` | PREF | `soru_coz_v1` |
| `QuizStore.kt` | K_SORULAR | `sorular_json` |
| `Sozluk.kt` | PREF | `sozluk_v1` |
| `SureAnalizi.kt` | PREF | `sure_analizi_v1` |
| `SureAnalizi.kt` | K_TAHMIN | `tahminler_json` |
| `Takip.kt` | PREF | `takip_v1` |
| `TakvimKopru.kt` | PREF | `takvim_v1` |
| `QuizStore.kt` | K_TEKRAR | `tekrar_json` |
| `TemaPaketi.kt` | PREF | `tema_paketi_v1` |
| `Sozluk.kt` | K_TERIMLER | `terimler_json` |
| `TimerEngine.kt` | PREF | `timer_engine_v1` |
| `UykuCerceve.kt` | PREF | `uyku_cerceve_v1` |
| `TakvimWidget.kt` | PREF | `wg_ay_ofset_v1` |
| `WidgetDokunma.kt` | PREF | `wg_dokunma_v1` |
| `WidgetFiltre.kt` | PREF | `wg_filtre_v1` |
| `WidgetListe.kt` | PREF | `wg_liste_v1` |
| `WidgetSecim.kt` | PREF | `wg_secim_v1` |
| `WidgetAtolye.kt` | PREF | `widget_atolye_v1` |
| `WidgetEylem.kt` | PREF | `widget_eylem_v1` |
| `ModulWidget.kt` | PREF | `widget_modul_v1` |
| `WidgetTema.kt` | PREF | `widget_tema_v1` |
| `YedekRotasyon.kt` | PREF | `yedek_rotasyon_v1` |
| `KonumHatirlatma.kt` | K_YERLER | `yerler_json` |
| `OgretmenStore.kt` | K_ZAYIF | `zayif_json` |
| `ZorunluUyari.kt` | PREF | `zorunlu_uyari_v1` |

## strings.xml dalga önekleri

· `w17`×58 · `w20`×26 · `w21`×36 · `w22`×27 · `w23`×15 · `w24`×13 · `w28`×11 · `w29`×6 · `w30`×8 · `w31`×1 · `w32`×11 · `w33`×4 · `w34`×10 · `w35`×7 · `w36`×3 · `w37`×83 · `w38`×19 · `w39`×6 · `w40`×7 · `w41`×15 · `w42`×13

## Manifest bileşenleri (receiver/activity/widget)

- **Receiver** (105): `ActionsWidget`, `AksamReceiver`, `AnaEkranDuzenActivity`, `AnalitikActivity`, `AramaActivity`, `ArsivActivity`, `BildirimAyarActivity`, `BildirimTaniActivity`, `BildirimTestReceiver`, `BildirimZamanlayici`, `BootReceiver`, `BrifingWidget`, `CountdownWidget`, `CourseReminderReceiver`, `DepolamaActivity`, `EventsListService`, `EventsListWidget`, `FilmActivity`, `FocusTileService`, `FullscreenTimerActivity`, `GlassHabitsWidget`, `GlassListService`, `GlassTasksWidget`, `GlassTodayWidget`, `GorevAlarmActivity`, `GorunumAyarActivity`, `GunPaneliReceiver`, `HaftaPlanActivity`, `HaftaWidget`, `HatalarimActivity`, `HedefWidget`, `IleriSayimReceiver`, `IlerlemeWidget`, `KanitActivity`, `KartActivity`, `KilitActivity`, `KocActivity`, `KocEylemAlici`, `KocZamanlayici`, `KokpitWidget`, `KonuAnlatimActivity`, `LessonPdfActivity`, `MainActivity`, `ManualSplitActivity`, `MikroGunlukActivity`, `ModulAyarActivity`, `ModulWidget`, `NamazActivity`, `NamazAyarActivity`, `NamazBildirim`, `NamazWidget`, `NefesActivity`, `NotHatirlaticiReceiver`, `OdakKutusuWidget`, `OgrenmeActivity`, `OgretmenActivity`, `OnlineActivity`, `OnlineBekci`, `OnlineBekciActivity`, `PdfAramaActivity`, `PlanHizliActivity`, `PlanWidget`, `PlanWidgetService`, `QuickAddActivity`, `QuizActivity`, `ReminderReceiver`, `SabahReceiver`, `SaglikActivity`, `SayacAyarActivity`, `SayacBittiActivity`, `SayacServisi`, `SayacWidget`, `SeneFilmiActivity`, `SesliDersServisi`, `SesliKutuActivity`, `SesliNotActivity`, `SimgeGece`, `SimgeKaramel`, `SimgeMinimal`, `SimgeMor`, `SimgeVarsayilan`, `SimgeYesil`, `SistemActivity`, `SohbetGecmisiActivity`, `SoruCozActivity`, `SozlukActivity`, `SummaryWidget`, `TakipActivity`, `TakipReceiver`, `TakvimAyarActivity`, `TakvimWidget`, `TakvimWidgetService`, `TaskActionReceiver`, `TasksWidget`, `TasksWidgetService`, `TekrarActivity`, `TimerActionReceiver`, `UykuAksiyonReceiver`, `UykuAyarActivity`, `UykuWidget`, `WeeklyReportReceiver`, `WidgetEylem`, `WidgetFiltreActivity`, `WidgetTemaActivity`, `ZorunluUyariActivity`
- **Activity** (51): `AnaEkranDuzenActivity`, `AnalitikActivity`, `AramaActivity`, `ArsivActivity`, `BildirimAyarActivity`, `BildirimTaniActivity`, `DepolamaActivity`, `FilmActivity`, `FullscreenTimerActivity`, `GorevAlarmActivity`, `GorunumAyarActivity`, `HaftaPlanActivity`, `HatalarimActivity`, `KanitActivity`, `KartActivity`, `KilitActivity`, `KocActivity`, `KonuAnlatimActivity`, `LessonPdfActivity`, `MainActivity`, `ManualSplitActivity`, `MikroGunlukActivity`, `ModulAyarActivity`, `NamazActivity`, `NamazAyarActivity`, `NefesActivity`, `OgrenmeActivity`, `OgretmenActivity`, `OnlineActivity`, `OnlineBekciActivity`, `PdfAramaActivity`, `PlanHizliActivity`, `QuickAddActivity`, `QuizActivity`, `SaglikActivity`, `SayacAyarActivity`, `SayacBittiActivity`, `SeneFilmiActivity`, `SesliKutuActivity`, `SesliNotActivity`, `SistemActivity`, `SohbetGecmisiActivity`, `SoruCozActivity`, `SozlukActivity`, `TakipActivity`, `TakvimAyarActivity`, `TekrarActivity`, `UykuAyarActivity`, `WidgetFiltreActivity`, `WidgetTemaActivity`, `ZorunluUyariActivity`

## Test ↔ kaynak eşlemesi

- `AlarmSagligiTest.kt` → ✔ AlarmSagligi.kt
- `AliskanlikMolaTest.kt` → ✔ AliskanlikMola.kt
- `AliskanlikNotTest.kt` → ✔ AliskanlikNot.kt
- `AnaEkranDuzenTest.kt` → ✔ AnaEkranDuzen.kt
- `AyarAraTest.kt` → ✔ AyarAra.kt
- `BildirimOzetiTest.kt` → ✔ BildirimOzeti.kt
- `ButceTest.kt` → ✔ Butce.kt
- `CokmeRaporTest.kt` → ✔ CokmeRapor.kt
- `DuzenTest.kt` → ✔ Duzen.kt
- `EventsListVeriTest.kt` → ✔ EventsListVeri.kt
- `GorevBekliyorTest.kt` → ✔ GorevBekliyor.kt
- `GorevBugunUstteTest.kt` → ✔ GorevBugunUstte.kt
- `GorevDisAktarTest.kt` → ✔ GorevDisAktar.kt
- `GorevErtelemeTest.kt` → ✔ GorevErteleme.kt
- `GorunumTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrafikDiliTest.kt` → ✔ GrafikDili.kt
- `GrupATest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrupBTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrupCTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrupDTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrupETest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GrupWTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `GunOdakBildirimTest.kt` → ✔ GunOdakBildirim.kt
- `GuncellemeTest.kt` → ✔ Guncelleme.kt
- `HaftaWidgetTest.kt` → ✔ HaftaWidget.kt
- `HalkaSectiTest.kt` → ✔ HalkaSecti.kt
- `HizliKomutTest.kt` → ✔ HizliKomut.kt
- `IleriSayimBildirimTest.kt` → ✔ IleriSayimBildirim.kt
- `IleriSayimTest.kt` → ✔ IleriSayim.kt
- `KilitMantikTest.kt` → ✔ KilitMantik.kt
- `KomutPaletiTest.kt` → ✔ KomutPaleti.kt
- `KonuTekrarTest.kt` → ✔ KonuTekrar.kt
- `KullanimTest.kt` → ✔ Kullanim.kt
- `MaskotTest.kt` → ✔ Maskot.kt
- `MolaKisilikTest.kt` → ✔ MolaKisilik.kt
- `NotArsivTest.kt` → ✔ NotArsiv.kt
- `NotBaglantTest.kt` → ✔ NotBaglant.kt
- `NotBicimTest.kt` → ✔ NotBicim.kt
- `NotHatirlaticiTest.kt` → ✔ NotHatirlatici.kt
- `NotKilitTest.kt` → ✔ NotKilit.kt
- `NotOlcumTest.kt` → ✔ NotOlcum.kt
- `NotRenkTest.kt` → ✔ NotRenk.kt
- `NotSabitleTest.kt` → ✔ NotSabitle.kt
- `NotSurumTest.kt` → ✔ NotSurum.kt
- `OdakHaftaTest.kt` → ✔ OdakHafta.kt
- `OdakManuelTest.kt` → ✔ OdakManuel.kt
- `OlcmeTestTest.kt` → ✔ OlcmeTest.kt
- `OnbellekTest.kt` → ✔ Onbellek.kt
- `PlanAsistanTest.kt` → ✔ PlanAsistan.kt
- `PrefYedekTest.kt` → ✔ PrefYedek.kt
- `RaporGrafigiTest.kt` → ✔ RaporGrafigi.kt
- `RippleTutarlilikTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `SaglikMotoruTest.kt` → ✔ SaglikMotoru.kt
- `SayacAyarOlcekTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `SayacErteleTest.kt` → ✔ SayacErtele.kt
- `SayacGeriKurTest.kt` → ✔ SayacGeriKur.kt
- `SayacIkonTest.kt` → ✔ SayacIkon.kt
- `SayacPresetTest.kt` → ✔ SayacPreset.kt
- `SayacSesTest.kt` → ✔ SayacSes.kt
- `SayacSpurtTest.kt` → ✔ SayacSpurt.kt
- `SayacZincirTest.kt` → ✔ SayacZincir.kt
- `SeriAnalizTest.kt` → ✔ SeriAnaliz.kt
- `SureAnaliziTest.kt` → ✔ SureAnalizi.kt
- `SurecPlanTest.kt` → ✔ SurecPlan.kt
- `TakipAlarmTest.kt` → ✔ TakipAlarm.kt
- `TakipTest.kt` → ✔ Takip.kt
- `TasarimOlcegiTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `TekrarTest.kt` → ✔ Tekrar.kt
- `TemaPaketiTest.kt` → ✔ TemaPaketi.kt
- `UykuCerceveTest.kt` → ✔ UykuCerceve.kt
- `WidgetListeTest.kt` → ✔ WidgetListe.kt
- `WidgetSecimTest.kt` → ✔ WidgetSecim.kt
- `WidgetSerbestTest.kt` → ⚠ kaynağı YOK (saf araç testi olabilir)
- `YedekSifreTest.kt` → ✔ YedekSifre.kt

_Eşleşen: 63/74_

## 🔎 Özellik → dosya haritası (küratörlü — koda dokunmadan önce buraya bak)

| özellik | ana dosya(lar) | depo/çapa |
|---|---|---|
| Görevler (liste/ekleme/silme) | `TasksFragment.kt` | Store.loadTasks/saveTasks · tasks_json |
| Görev alarmı | `AlarmScheduler.kt + GorevAlarmActivity.kt` | görev başına RTC_WAKEUP |
| Görev erteleme sayacı | `GorevErteleme.kt` | ISO hafta · tiErtelemeSerit |
| Bugün üstte sabitleme | `GorevBugunUstte.kt` | tiBugunUstte toggle |
| Bekliyor ⏳ | `SettingsFragment + TasksFragment` | gorev_bekliyor_v1/kume |
| Not defteri | `NotesFragment.kt` | notes_json |
| Not kilidi (PIN) | `NotKilit.kt` | not_kilit_v1/kilitli · kilit_v1/hash |
| Not renk/etiket | `NotesFragment.kt` | not_renk_v1 |
| Not arşiv/sabitle/sürüm/hatırlatıcı | `NotesFragment.kt + NotHatirlatici.kt` | not_arsiv_v1 · not_sabitle_v1 · not_surum_v1 · not_hatirlat_v1 |
| Konular + alt maddeler | `TopicsFragment.kt` | topics_json |
| Ders/PDF okuyucu | `PdfReaderActivity.kt + CoursesFragment.kt` |  |
| Alışkanlıklar | `HabitsFragment.kt` | habits_json + habit_log_json |
| Alışkanlık mola 🏖 | `AliskanlikMola.kt` | aliskanlik_mola_v1 · Store.habitStreak mola-atlamalı |
| 21 gün kuralı 🎓 | `AliskanlikMola.kt (Kural21)` | habitKural21 bar |
| Gün notu 📝 | `AliskanlikNot.kt` | aliskanlik_not_v1 · halkaya uzun basış |
| İkinci seri 🏅 | `SeriAnaliz.kt + Store.habitTumGunler` | düzenleyicide satır |
| Zamanlayıcı (geri sayım) | `TimerFragment.kt + TimerEngine.kt` | SayacServisi · TimerAlarm |
| Zamanlayıcı bildirimi | `TimerNotifier.kt + TimerActionReceiver.kt` | zamanlayici_canli_v2 · kronometre |
| İleri sayım | `TimerFragment MODE_ILE + IleriSayim.kt` | ileri_sayim_v1 · ileri_sayim_gecmis_v1 |
| İleri sayım bildirimi | `IleriSayimBildirim.kt + IleriSayimReceiver.kt` | ileri_sayim_canli_v1 kanal · chronometer |
| Kadran görünümü | `SayacKadraniView.kt` | özel View · yaziOlcek ×SayacAyar.kadranOlcek |
| Zamanlayıcı ayarları | `SayacAyar.kt + SayacAyarActivity.kt` | sayac_ayar_v1 · tiklanabilirSatir deseni |
| Pomodoro/zincir | `Pomodoro.kt + SayacZincir.kt` |  |
| Odak sesleri | `SesManzarasi.kt + OdakKalkani.kt` | SayacAyarActivity manzara seçimi |
| Odak skor/kayıt | `OdakKaydi.kt + Store.addTodayFocusMinutes` | gunluk odak dakikası |
| Puan/koç | `KocActivity.kt + KocZamanlayici.kt` |  |
| Tam ekran sayaç | `FullscreenTimerActivity.kt` |  |
| Sayac bitti ekranı | `SayacBittiActivity.kt` | SayacErtele + SayacGeriKur |
| Gün planı çerçevesi | `GunOdak.kt + GunOdakBildirim.kt` |  |
| Uyku zamanlayıcı | `UykuZamanla.kt` |  |
| Ezan/namaz vakitleri | `NamazWidget.kt + VakitPlan` | sayac widgetları |
| Sağlık kontrolü (A-Z) 🩺 | `SaglikActivity.kt + SaglikMotoru.kt` | Ayarlar rowSaglik · 21 madde |
| AI asistan | `AiClient.kt + AiSettings.kt + AiOnbellek.kt` | ai_sohbet_v1 · sadeIstek |
| Çökme raporu | `CokmeRapor.kt` | tekrarEdenler |
| Bildirim merkezi/kanallar | `BildirimMerkezi.kt + BildirimZamanlayici.kt` | BildirimTani sağlık API |
| Boot yeniden kurma | `BootReceiver.kt` | AlarmScheduler.rescheduleAll + CourseReminder + PlanAsistan.kur |
| Sabah planı 🌅 | `SabahReceiver.kt + GunlukBildirim.kt` | USER_PRESENT · plan_asistan_v1 |
| Akşam sorusu 🌙 | `AksamReceiver.kt + PlanAsistan.kt` | AKSAM_PLAN alarmı · 22:00 vars. |
| Widget'lar | `WidgetCommon.kt + *Widget.kt` | OdakKutusuWidget · NamazWidget |
| Yedek/geri yükleme | `PrefYedek.kt` | yedek taraması |
| Çevrimiçi senkron | `OnlineActivity.kt + OnlineStore.kt` |  |
| Tema/yazı ölçeği | `ThemeManager.kt + GorunumAyar.kt` | gunluk_asistan_gorunum · fontScale |
| Kutlama/titreşim | `Kutlama.kt + Titresim.kt` |  |
| Liste animasyonları | `ListeFark.kt` |  |
| Oturum planları | `SurecPlan.kt` | Oturum json kayıtları |

---

# ✍️ Elle bakım — hız desenleri (aramadan kod yaz)

## Teslim boru hattı (her sürüm, sırayla)
1. `bash /home/user/YEDEK/butunluk.sh` — 60 sn denetim (EKSIK satırı varsa çaresi satırda yazar)
2. Kaynak zipten geri yükleme: `unzip -oq /home/user/kaynak-v<SON>-yedek.zip -d /home/user/GunlukAsistan`
3. `.git` kayıpsa: `bash /home/user/YEDEK/git-sigorta.sh geri` (bundle'dan tam geçmiş)
4. Derleme: `bash /home/user/hizli-teslim.sh` (~13 dk, `hizli.log` sonunda `EXIT=0`)
5. Teslim: `bash /home/user/yukle-gofile.sh <apk> <zip> <notlar> <PROJE-DURUM.md>` → `yukle-sonuc.txt`
6. Tur sonu: `bash /home/user/YEDEK/git-sigorta.sh yenile` + `bash /home/user/YEDEK/kaynak-ayna.sh`

## Uzaktan kurtarma katmanları (geriye dönüş zinciri — sırayla dene)
1. **gofile** (kullanıcıya link; biz API'den indiremiyoruz `error-notPremium`)
2. **filebin kaynak aynası** `kaynak-gunluk-*` (YEDEK/kaynak-ayna.sh indir — md5 kanıtlı, ~6 gün ömür, her tura başlarken tazelenir)
3. **git bundle** `YEDEK/repo.gitbundle` (tam geçmiş)
4. **yerel zip** `/home/user/kaynak-v*-yedek.zip` (çoğu kayıpta hayatta kalır)

## Yeni özellik kalıpları (kanıtlanmış 6 desen)
- **Saf motor + test**: `object X` üstte android'siz fonksiyonlar → `XTest.kt` (org.json JVM'de gerçek!)
- **Ayar**: `SayacAyar` deseni — `private const val PREF`, get/set, `kadranCarpani` gibi saf eşleme + `SayacAyarActivity`'ye `tiklanabilirSatir`/`anahtarSatiri` + `ciz()`
- **Bildirim aksiyonu**: `TimerNotifier` deseni — kanal + `PendingIntent.getBroadcast(...setPackage)` + manifest `<receiver exported="false"><intent-filter><action/>`
- **Günlük alarm**: `PlanAsistan.kur` deseni — setExact/AndAllowWhileIdle RTC_WAKEUP + tetikte **yeniden kur** + `BootReceiver` kancası
- **Ayarlar satırı**: `fragment_settings.xml` `rowSaglik` klonu (emoji + başlık @dimen/ga_yazi_orta + alt @dimen/ga_yazi_kucuk + "›") — ham `Nsp` YASAK (TasarimOlcegiTest), hep `@dimen/ga_yazi_*`
- **Fragment dizesi**: `strings.xml` sonuna `w<XX>_` önekli blok — `%1$d` formatlı, lint güvenli (çıplak `%` YOK)

## Bilinen kırmızılar (2 kez yaşandı, üçüncüyü yaşama)
- Expression-body fonksiyonda `return` kullanılamaz
- KDoc köşeli parantez `[x..y]` dok-link sanılır → derleme hatası
- Parametre adını uydurma: önce `grep "fun f(" dosya` ile gerçek imzayı oku
- Snapshot kaybı: tur sonuna doğru yazılan dosyalar uçabilir → her tur SONU `git-sigorta.sh yenile` + kaynak-ayna

## Sürüm defteri (teslim linkleri)
Ayrıntılı tablo: `YEDEK/BAGLANTI-DEFTERI.md` — her sürümde APK/zip/notlar/md5 satırı.
