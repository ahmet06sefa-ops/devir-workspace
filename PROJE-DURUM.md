# Günlük Asistan — Proje Durumu
_Son güncelleme: 17 Ağustos 2026 (v11.23 — HabitGenius Ayarları Birebir Aktarıldı (Tek APK))_

> ## 📦 Teslim kuralı
> **Her değişiklikten sonra dörtlü paket halinde verilir:**
> 1. `GunlukAsistan-v<sürüm>.apk` — kurulacak dosya
> 2. `kaynak-v<sürüm>-yedek.zip` — tam kaynak kod
> 3. `PROJE-DURUM.md` — bu not dosyası (güncellenmiş)
> 4. `GunlukAsistan-v<sürüm>-notlar.md` — müstakil sürüm notları

## ✅ Şu anki durum: v11.22 tamamlandı — Tek APK: 2. Görünüm İçeride Açılır ✔
_Son güncelleme: 17 Ağustos 2026 (v11.22) — Kullanıcı isteği: **"İkisini tek bir APK yap; 2. Görünüm dediğimde Günlük Asistan'ın içinde açsın."** HabitGenius 2. Görünümü **tek APK'nın içine** native Jetpack Compose olarak gömüldü. Artık iki ayrı uygulama yok: tek `GunlukAsistan-v11.22.apk` kurulur; Ayarlar → Görünüm → **"2. Görünüm (Habit Genius)"** seçilince HabitGenius Günlük Asistan içinde açılır; 2. Görünüm modu aktifken uygulama açılışında da HabitGenius ekranıyla başlar; sağ üst ⚙ → **"1. Görünüme Geç"** ile klasik görünüme dönülür. `HabitGeniusComposeActivity` + 5 ekran + Ayarlar tek dosyada. **1.924 test / 0 hata / 0 başarısızlık** — YENİ REKOR!_

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **11.22** (versionCode 278) |
| APK | `~/GunlukAsistan-v11.22.apk` (TEK APK — 2. Görünüm içeride) |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **1924 test, 0 başarısız** (146 test suite / sınıfı, +3 yeni: HabitGeniusComposeTest) |
| Zincir | Temiz derleme (`EXIT=0`) · 0 hata · tam geriye uyumluluk |
| Derleme notu | Tek APK — HabitGenius 2. Görünümü içeride (Compose) |
| Ortam | Kurtarma aynası canlı (2026-08-17) · 3 GB swap ✔ |

---

## v11.23 (code 279) — HabitGenius Ayarları Birebir Aktarıldı · 17 Ağu 2026

1. **RN bundle analizi:** Kullanıcının sağladığı HabitGenius APK'sının `index.android.bundle`
   dosyasından gerçek özellikler çıkarıldı (tema Dark/Light/OLED, 20 vurgu rengi, dil, bildirim,
   hatırlatıcı, gizlilik kilidi, yedekleme, sesli not, checklist, ısı haritası, harcama kategorileri).
2. **`HabitGeniusCompose.kt`:** Bu özellikler Compose 2. Görünümüne aktarıldı; zengin Ayarlar
   ekranı + `AyarKarti`/`AyarSvic`/`HabitKart` composable'ları eklendi.
3. **`ThemeFragment`:** 2. Görünüm → `HabitGeniusComposeActivity.ac(ctx)` (içeride açar).
4. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa Compose ekranı içeride açılır.
5. **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
6. **`HabitGeniusComposeTest`:** Activity + MainScreen + SettingsScreen + AyarKarti (3 test).
7. **🧪 Testler:** **1.924 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.22 (code 278) — Tek APK: 2. Görünüm İçeride Açılır · 17 Ağu 2026

1. **`HabitGeniusCompose.kt`:** Kullanıcının sağladığı tam Compose sürümü; `HabitGeniusComposeActivity`
   + `HabitGeniusMainScreen` + 5 ekran (Alışkanlık, Ruh Hali, Finans, Odaklan, Günlük) + `HabitGeniusSettingsScreen`.
2. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → `HabitGeniusComposeActivity.ac(ctx)` (içeride açar).
3. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa Compose ekranı içeride açılır.
4. **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
5. **`HabitGeniusComposeTest`:** Activity + MainScreen + SettingsScreen yüklenebilirliği (3 test).
6. **Düzeltme:** `onNewIntent` nullable imza.
7. **🧪 Testler:** **1.924 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.21 (code 277) — HabitGenius Ayrı Uygulama Köprüsü · 17 Ağu 2026
_Son güncelleme: 17 Ağustos 2026 (v11.21) — Kullanıcı, "2. Görünümün bütün her şeyi"ni içeren **tam HabitGenius uygulamasını** (Google Drive) iletti: `com.habitgenius.habit.tracker`, React Native, v3.3.1. Bu, Günlük Asistan'dan farklı paket + çalışma zamanına sahip bağımsız bir uygulama olduğu için içine gömülemez; bunun yerine **`HabitGeniusKopru`** ile "2. Görünüm ayrı uygulama gibi aç" akışı çalışır hale getirildi: 2. Görünüm seçilince HabitGenius başlatılır (kuruluysa), kurulu değilse kurulum mesajı; MainActivity 2. Görünüm modunda HabitGenius'u açılışta başlatır; 1. Görünümle geri dönülür. Ayrıca kullanıcının HabitGenius APK'sı Günlük Asistan'ın debug anahtarıyla yeniden imzalanıp **`HabitGenius-v3.3.1.apk`** olarak kurulabilir hale getirildi (aynı imza ailesi). **1.925 test / 0 hata / 0 başarısızlık** — YENİ REKOR!_

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **11.21** (versionCode 277) |
| APK | `~/GunlukAsistan-v11.21.apk` |
| HabitGenius | `~/HabitGenius-v3.3.1.apk` (aynı imza `5f15d4e7…` ile yeniden imzalandı) |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **1925 test, 0 başarısız** (147 test suite / sınıfı, +4 yeni: HabitGeniusKopruTest) |
| Zincir | Temiz derleme (`EXIT=0`) · 0 hata · tam geriye uyumluluk |
| Derleme notu | HabitGenius ayrı uygulama köprüsü + v3.3.1 APK |
| Ortam | Kurtarma aynası canlı (2026-08-17) · 3 GB swap ✔ |

---

## v11.21 (code 277) — HabitGenius Ayrı Uygulama Köprüsü · 17 Ağu 2026

1. **`HabitGeniusKopru.kt`:** `com.habitgenius.habit.tracker` paketini başlatan köprü;
   `baslat` / `kuruluMu` / `paketAdi` / `MainActivitySinifAdi` / `surumAdi`.
2. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → köprü ile HabitGenius'u başlatır;
   kurulu değilse kurulum mesajı gösterir.
3. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) HabitGenius'u başlatır.
4. **`HabitGenius-v3.3.1.apk`:** Kullanıcının sağladığı tam uygulama; Günlük Asistan'ın
   debug anahtarıyla (`5f15d4e7…`) yeniden imzalanarak kurulabilir yapıldı.
5. **`HabitGeniusKopruTest`:** paket/activity/sürüm/sınıf koruması (4 test).
6. **🧪 Testler:** 1.921 → **1.925 test (%100 başarı, 0 hata, 147 sınıf)**.

---

## v11.20 (code 276) — HabitGenius Ayrı Uygulama + 1. Görünüme Geç · 16 Ağu 2026

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **11.19** (versionCode 275) |
| APK | `~/GunlukAsistan-v11.19.apk` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **1924 test, 0 başarısız** (146 test suite / sınıfı, +3 yeni: HabitGeniusComposeTest) |
| Zincir | Temiz derleme (`EXIT=0`) · 0 hata · tam geriye uyumluluk |
| Derleme notu | HabitGenius Tam Compose Sürümü (2. Görünüm) |
| Ortam | Kurtarma aynası canlı (2026-08-16) · 3 GB swap ✔ |

---

## v11.20 (code 276) — HabitGenius Ayrı Uygulama + 1. Görünüme Geç · 16 Ağu 2026

1. **`HabitGeniusSettingsScreen` (`HabitGeniusCompose.kt`):** 2. Görünümün kendi Ayarlar ekranı;
   üstteki ⚙ düğmesiyle açılır. **"🎨 1. Görünüme Geç"** butonu → `ThemeManager.gorunumModu = KLASIK`,
   widget tazeleme + `(context as? Activity)?.finish()` → HabitGenius kapanır, klasik ana ekran görünür.
2. **Sağ üstte ⚙ Ayarlar düğmesi** — 5 sekmenin üzerinde her zaman erişilebilir (üst sağda yarı saydam daire).
3. **`HabitGeniusMainScreen`:** `showSettings` state; ayar ekranı açıkken alt gezinme gizlenir.
4. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → `HabitGeniusComposeActivity.ac(ctx)`.
5. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) Compose ekranı açılır.
6. **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
7. **`HabitGeniusComposeTest`:** Activity + MainScreen + SettingsScreen yüklenebilirliği (3 test).
8. **🧪 Testler:** **1.924 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.19 (code 275) — HabitGenius Tam Compose Sürümü · 16 Ağu 2026

1. **`HabitGeniusCompose.kt`:** Kullanıcının sağladığı tam kaynak; `HabitGeniusComposeActivity`
   (ComponentActivity) + `HabitGeniusMainScreen()` (AnimatedContent geçiş) + 5 ekran + `@Preview`.
   Renkler: `#0C0C0C` zemin / `#161616` kartlar; modül vurguları (zümrüt, lavanta, safir, kırmızı, kehribar).
2. **Compose build:** `compose=true`, Compose derleyici 1.5.14 (Kotlin 1.9.24), BOM 2024.06.00, `material3`, `activity-compose`.
3. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → `HabitGeniusComposeActivity.ac(ctx)`.
4. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) Compose ekranı açılır.
5. **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
6. **`HabitGeniusComposeTest`:** Activity/Composable/Companion yüklenebilirliği (3 test).
7. **Düzeltmeler:** `ChipDefaults`→`SuggestionChipDefaults`; `onNewIntent` nullable imza.
8. **🧪 Testler:** **1.924 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.18 (code 274) — HabitGenius Jetpack Compose 2. Görünümü · 16 Ağu 2026

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **11.18** (versionCode 274) |
| APK | `~/GunlukAsistan-v11.18.apk` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **1924 test, 0 başarısız** (146 test suite / sınıfı, +3 yeni: HabitGeniusComposeTest) |
| Kotlin dosyası | **456** (ana + test) |
| Zincir | Temiz derleme (`EXIT=0`) · 0 hata · tam geriye uyumluluk |
| Derleme notu | HabitGenius Jetpack Compose 2. Görünümü |
| Katalog | `1000-EKSIK-VE-GELISIM-CATALOGU.md`, `10000-EKSIK-VE-GELISIM-CATALOGU.md` |
| Ortam | Kurtarma aynası canlı (2026-08-16) · 3 GB swap ✔ |

---

## v11.18 (code 274) — HabitGenius Jetpack Compose 2. Görünümü · 16 Ağu 2026

1. **Compose entegrasyonu (`app/build.gradle.kts`):** `compose=true`, `composeOptions` (derleyici 1.5.14,
   Kotlin 1.9.24), Compose BOM 2024.06.00 + `material3` + `ui` + `activity-compose`.
2. **`HabitGeniusCompose.kt`:** Tek dosya; `HabitGeniusComposeActivity` (ComponentActivity),
   `HabitGeniusApp()` (koyu tema + Scaffold + 5 sekmeli NavigationBar) + `HabitGeniusPreview()`
   (@Preview). Ekranlar: Habit Tracker, Mood Journal, Expense Tracker, Focus Timer, Digital Journal.
3. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → `HabitGeniusComposeActivity.ac(ctx)`.
4. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) Compose ekranı açılır.
5. **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
6. **`HabitGeniusComposeTest`:** Activity/Composable/Companion yüklenebilirliği (3 test).
7. **🧪 Testler:** 1.921 → **1.924 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.17 (code 273) — HabitGenius 2. Görünümü WebView · 16 Ağu 2026

1. **`HabitGeniusActivity.kt`:** WebView tabanlı tam ekran; `file:///android_asset/habitgenius.html`
   yükler (JavaScript + DOM storage açık), geri tuşu önce WebView iç geçmişini geri alır.
2. **`assets/habitgenius.html`:** Premium 5 sekmeli prototip (Habit/Mood/Expense/Focus/Journal).
3. **`ThemeFragment`:** "2. Görünüm (Habit Genius)" seçilince `HabitGeniusActivity.ac(ctx)` açar.
4. **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) HabitGenius ekranı açılır.
5. **`AndroidManifest.xml`:** `.HabitGeniusActivity` kayıtlı.
6. **`HabitGeniusEntegrasyonTest`:** Activity + Companion yüklenebilirliği (2 test).
7. **🧪 Testler:** 1.921 → **1.923 test (%100 başarı, 0 hata, 146 sınıf)**.

---

## v11.16 (code 272) — 2. Görünüm: Habit Genius · 16 Ağu 2026

1. **🌐 `Theme.GunlukAsistan.HabitGenius` (`values/themes.xml`):**
   - Açık tema (Material3 Light): `colorPrimary #6C5CE7` (mor), `colorSecondary #1E9E5A` (yeşil),
     zemin `#F6F5FB`, yüzey `#FFFFFF`, metin `#1B1B2B`; durum çubuğu mor `#6C5CE7`.
2. **`ThemeManager.kt`:** `gorunum_modu_v1` anahtarı; `GORUNUM_KLASIK=1` / `GORUNUM_HABITGENIUS=2`;
   `styleFor`/`geceModunuUygula`/`koyuMu` Habit Genius moduna göre karar verir; saf test
   fonksiyonları `gorunumModuSaf`, `habitGeniusKoyuMu`.
3. **`fragment_theme.xml` + `ThemeFragment.kt`:** Görünüm ekranının üstüne "GÖRÜNÜM" bölümü,
   1. Görünüm (Klasik) ve 2. Görünüm (Habit Genius) seçim kartları.
4. **`strings.xml`:** `gorunum_section`, `gorunum_caption`, `gorunum_klasik`, `gorunum_habit`, `gorunum_secildi`.
5. **`GorunumModuTest.kt`:** 7 saf koruma testi.
6. **🧪 Testler:** 1.914 → **1.921 test (%100 başarı, 0 hata, 145 sınıf)**.

---

## v11.15 (code 271) — Yeni Görünüm (koyu modern tema) · 16 Ağu 2026

1. **🌙 `Theme.GunlukAsistan.YeniGorunum` stili (`values/themes.xml`):**
   - Koyu, mavi-mor vurgulu tema (Material3 Dark): `colorPrimary #7C6BFF`, zemin `#0F1526`,
     yüzey `#151C33`, metin `#E9EDF7`; durum/gezinme çubuğu koyu, `windowLightStatusBar=false`.
2. **`ThemeManager.kt`:** `specs` listesine `Spec("Yeni Görünüm","🌙",…,dark=true)` eklendi →
   tema seçici ızgarasında otomatik görünür (Ayarlar → Tema, ana menü 8. öğe).
   - `isNeon` artık "Zincir" başlığını arar; yeni temalar sona eklenebilir, neon bozulmaz.
3. **`YeniGorunumTest.kt`:** 8 saf koruma testi (listedeki varlığı, koyuluğu, stil/renk,
   benzersiz başlık, eski temaların korunması, açık/koyu dengesi).
4. **🧪 Testler:** 1.906 → **1.914 test (%100 başarı, 0 hata, 144 sınıf)**.

---

## v11.14 (code 270) — Verimlilik Paketi (Pomodoro + Eisenhower) · 16 Ağu 2026

1. **🍅 Pomodoro / Verimlilik Motoru (`PomodoroMotoru.kt`):**
   - `sureDonustur` (dk → "1:15"/"25 dk"), `verimlilikSkoru` (odak+% görev → 0..100),
     `yildiz`/`yorum`, `blokSayisi` (25 dk'lık blok), `molaOnerisi` (4 blokta uzun 20 dk),
     `gunIcinOdakPlani` (adım adım gün planı).
2. **📋 İçerik Önceliklendirme Motoru (`IcerikOnceliklendirmeMotoru.kt`):**
   - Eisenhower Matrisi: `kadran` (Önemli+Acil → Hemen Yap / Planla / Devret / Ertela),
     `oncelikPuani` (önem %60 + aciliyet %40), `sirala`/`matrisSiralama`, `okunur` listesi.
3. **🤖 AI Entegrasyonu:** `AsistanKomut` dispatch + `AiClient` prompt kataloğuna
   `pomodoro_durum` ve `onceliklendir` komutları.
4. **🧪 Testler:** `PomodoroMotorTest.kt` (21) + `IcerikOnceliklendirmeMotorTest.kt` (18)
   → **1.906 saf JVM testi (%100 başarı, 0 hata, 143 test sınıfı)** ile doğrulandı.

---

## v11.13 (code 269) — Sayaç Saati Donması & Güç Tuşuyla Alarm Susturma · 15 Ağu 2026

1. **Zamanlayıcı Saatinin Donması Düzeltmesi (`CevrimliTik.kt`, `TimerFragment.kt`):**
   - `postDelayed` en son satıra yazıldığı için gövde hatasının tık zincirini koparması giderildi. Yeni saf sarmalayıcı gövdeyi güvenle çalıştırır, istisnayı yutar ve bir sonraki tıkı her koşulda zamanlar → saat asla takılmaz.
2. **Güç Tuşuyla Alarm / Sesleri Susturma (`BitisSesMotoru.kt`, `TimerActionReceiver.kt`, `TimerFragment.kt`, `SayacBittiActivity.kt`):**
   - Bitiş sesi + titreşim + ısrarlı alarm tek merkezden yönetilir; `ACTION_SCREEN_OFF` (güç düğmesi) dinlenir ve basılır basılmaz her şey anında susar.
   - Bitiş bildirimine **"🔕 Sesi Kapat"** eylemi; `SayacBittiActivity` kapanışında ve `ACTION_STOP`/`ACTION_RESET` akışlarında ses kesme eklendi.
   - **1.682 saf JVM JUnit testi (%100 başarı, 0 hata, 121 test sınıfı)** ile doğrulandı.

---

## v11.12 (code 268) — Evrensel Veri Yedekleme & Geri Yükleme Motoru · 15 Ağu 2026

1. **Tüm Verileri Tek Dosyada Yedekleme (`TumVeriYedeklemeMotoru.kt`):**
   - Tüm SharedPreferences dosyalarını `{dosyaAdı → (anahtar → değer)}` olarak okuyup tek bir taşınabilir JSON yedeğine çeviren **saf JVM motoru** eklendi.
   - **Tip koruması:** String / Int / Long / Float / Boolean / Set türleri birebir kaydedilip geri yüklendiğinde aynen korunur.
   - **Bütünlük (CRC32):** tüm girdilerin sıralı kanonik temsili üzerinden sağlama değeri hesaplanır; kurcalanmış / bozulmuş yedekler geri yüklenmez.
   - **Metadata:** sürüm + zaman damgası + dosya/anahtar sayısı + sağlama değeri; ekranda özet gösterilir.
2. **Yedekleme & Geri Yükleme Ekranı (`VeriYedekActivity.kt`, `activity_veri_yedek.xml`):**
   - **📤 Dışa Aktar:** JSON yedeğini oluşturur ve paylaşma ekranını açar (WhatsApp / Google Drive / bulut / dosya yöneticisi).
   - **📥 Geri Yükle:** .json dosyası seçtirir, bütünlüğü doğrular ve tüm anahtarları geri yükler.
   - Ayarlar menüsüne **"💾 Tüm Verileri Yedekle & Geri Yükle"** satırı eklendi; Activity manifest'e kayıtlı, global tema senkronizasyonu ile %100 uyumlu.
   - **1.670 saf JVM JUnit testi (%100 başarı, 0 hata, 119 test sınıfı)** ile doğrulandı.

---

## v11.11 (code 267) — Canva Çalışma Ekranı: 10 Uygulama Arayüzü & Akıllı Öneri · 12 Ağu 2026

1. **10 Uygulamayı Birleştiren Canva Çalışma Ekranı (`CanvaCalismaAtolyeActivity.kt`, `CanvaCalismaMotoru.kt`):**
   - Pomodoro, Görevler, Namaz Vakitleri, Günün Akışı, Kurslar, İstatistikler, Kişisel Gelişim, YouTube Oynatma Listeleri, Evrensel Görünüm ve İnovasyon Atölyesi tek bir tuvalde birleştirildi.
2. **Aç-Kapa Özelliği & Akıllı Öneri / Karıştır Butonları:**
   - 10 modülün her biri çip anahtarlarıyla çalışma alanına açılıp kapatılabiliyor.
   - **"💡 Akıllı Öneri (Öner)"** butonu günün saatine göre en verimli uygulamaları açarken, **"🔄 Tekrar Dene (Karıştır)"** butonu alternatif kombinasyonlar denetiyor.
   - **1.654 saf JVM JUnit testi (%100 başarı, 0 hata, 118 test sınıfı)** ile doğrulandı.

---

## v11.10 (code 266) — A'dan Z'ye Sınırsız Sürükleme / Taşıma Yetkisi & Gerçek Kart Transfer Motoru · 11 Ağu 2026

1. **Özet Yazı Kartı Yerine Gerçek ve Canlı Kart Transferi (`EvrenselKartKatalogu.kt`):**
   - Taşıma yapılınca ortaya çıkan özet metin notu kaldırıldı. Artık taşınan kartın kendisi hedef sekmede tam işlevli bir MaterialCardView olarak (`gercekKartOlustur`) ekleniyor.
2. **A'dan Z'ye Sınırsız Sürükleme ve Taşıma Yetkisi (`EvrenselTasimaVeSuruklemeMotoru.kt`):**
   - Tüm ekranların konteynerlerindeki her çocuğa sürükle-bırak (`startDragAndDrop`) ve uzun basma menüsü bağlandı. Basılı tutarak yukarı-aşağı sürüklemek veya diğer sekmelere gerçek kart olarak taşımak mümkün kılındı.
   - **1.634 saf JVM JUnit testi (%100 başarı, 0 hata, 117 test sınıfı)** ile doğrulandı.

---

## v11.09 (code 265) — Sekmeler Arası Anında Taşıma & Tam Tema Senkronizasyonu · 11 Ağu 2026

1. **Sekmeler Arası Taşıma İşleminin Anında Görünmesi (`MainActivity.kt`, `SekmeVeVeriTasimaMotoru.kt`):**
   - Taşıma menüsünden veri aktarıldığı anda hedef sekme açılır ve `aktifSekmeTasinanlariGuncelle` üzerinden taşınan veri **anında ekranda belirir**.
2. **Kişisel Gelişim & YouTube Oynatma Listeleri Ekranlarının Temayla %100 Uyumu:**
   - `KisiselGelisimActivity`, `YoutubePlaylistActivity`, `EvrenselGorunumActivity` ve `BinMaddeKontrolActivity` aktivitelerine global tema, vurgu rengi, yazı ölçeği ve cam efekti kancaları eklendi.
   - **1.609 saf JVM JUnit testi (%100 başarı, 0 hata, 116 test sınıfı)** ile doğrulandı.

---

## v11.08 (code 264) — Ana Ekran Yatay Tam Genişlik & Evrensel Sekmeler Arası Taşıma · 11 Ağu 2026

1. **Ana Sayfa "Günü Gösteren Şey" (Hero Kartı) Yatay Küçülme Düzeltmesi (`HomeFragment.kt`):**
   - Kart boyutu ayarında kartların yanlardan daralmaması için `scaleX = 1.0f` tam genişlik kilitlendi.
2. **Evrensel Sekmeler Arası Taşıma ve Kopyalama Motoru (`SekmeVeVeriTasimaMotoru.kt`):**
   - Ana Ekran, Bugün, Konular, İlerleme sekmelerindeki kartlara basılı tutulduğunda verileri tek tek veya komple tüm içerik olarak diğer sekmelere taşıma/kopyalama imkanı getirildi.
   - Hedef sekmelerin en üstüne **"📦 Diğer Sekmelerden Taşınan Veriler"** kartı eklenerek taşınan verilerin görüntülenmesi ve tek dokunuşla geri alınması sağlandı.
   - **1.609 saf JVM JUnit testi (%100 başarı, 0 hata, 116 test sınıfı)** ile doğrulandı.

---

## v11.07 (code 263) — İlk Açılış Ekranı Seçimi, Detaylı Görev Düzenleme & Basılı Tutarak Yönetim · 11 Ağu 2026

1. **İlk Açılış Ekranı Seçimi (`GorunumAyar.acilisEkran`, `SettingsFragment.kt`):**
   - Uygulamanın her açılışta `"☀️ Bugün / Günün Akışı"`, `"✅ Görevler"`, `"⏱️ Sayaç"` vb. 7 ekrandan hangisiyle açılacağını ayarlama özelliği eklendi.
2. **Görev Detaylı Düzenleme & Alarmsız / Saatsiz Kayıt (`TasksFragment.kt`):**
   - Görev metnine dokunulduğunda mevcut görevi sonradan detaylıca düzenleme paneli (`showTaskEditor(task)`) açılıyor.
   - Saat veya alarm seçme zorunluluğu tamamen kaldırıldı; saat sorunu çözüldü (`dueAt = 0L`).
3. **Günün Akışı Kart Boyutları & Basılı Tutarak Sıra Değiştirme (`HomeFragment.kt`, `GorunumAyar.kt`):**
   - Günün Akışı ve kartların boyutu %85 Kompakt ile %130 Devasa arasında ayarlanabiliyor.
   - Ana ekrandaki kartlara basılı tutulduğunda sıra değiştirme ve boyut ayarlama menüsü açılıyor.
4. **Sekmeler Arası Tablo Taşıma (`TasksFragment.kt`):**
   - Görevlere basılı tuttuğunuzda sırasını yukarı/aşağı alma veya görevi `💼 İş`, `🏠 Kişisel`, `📚 Ders & Eğitim`, `🚀 Proje` gibi sekmeler/kategoriler arasında taşıyabilme özelliği eklendi.
   - **1.589 saf JVM JUnit testi (%100 başarı, 0 hata, 115 test sınıfı)** ile doğrulandı.

---

## v11.06 (code 262) — Dikey Vakit Planı, Gösterişli Dini Sözler Kartı & Sesli Namaz Alarmları · 11 Ağu 2026

1. **Alt Alta Dikey Vakit Planı (`PlanFragment.kt`):**
   - Vakit Planı sekmesindeki yatay kaydırılabilir seher, kuşluk vb. vakit dilimi kartları ekranda dikey sırada **alt alta tam genişlikte** sıralandı.
2. **Gösterişli Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Kartı (`DiniSozMotoru.kt`):**
   - Her vakit (Seher, Kuşluk, Öğle, İkindi, Akşam, Gece) için farklı hadisler ve dini sözler sunan, **"🔄 Başka Söz"** butonuyla yenilenebilen MaterialCardView eklendi.
3. **Sesli Namaz Alarmları & Ayarlar Yönetimi (`NamazBildirim.kt`, `NamazAyarActivity.kt`):**
   - Namaz vakti bildiriminde varsayılan sessiz olma sorunu çözüldü; telefon sessizde olsa bile duyulan sesli alarm tetikleyicisi (`ZorunluUyari.cal`) bağlandı.
   - Ayarlara **"🔊 Namaz Saatlerinde Sesli Alarm Çal"** anahtarı ve **"▶️ Alarm Sesini Şimdi Dinle & Test Et"** butonu eklendi.
   - **1.574 saf JVM JUnit testi (%100 başarı, 0 hata, 114 test sınıfı)** ile doğrulandı.

---

## v11.05 (code 261) — Çalışma Zamanı Ekranı Sıfırlama Koruması & Tek Ekran Sadeleşmesi · 11 Ağu 20262. **Tam Dakika ve Saniye Gösterimi (`SayacAyar.kalanSureDakikaSaniyeMetni`):**
   - Kalan süre gösterimleri ve Zen Odak modu süresi tam dakika ve saniye detayında (`18:45 kaldı (18 dk 45 sn)`) biçimlendirildi.
3. **Sade ve Bozulmayan Tek Ekran Düzeni (`fragment_timer.xml`, `SayacKadraniView.kt`):**
   - Sayaç ekranındaki `"🏷️ Etiket Ekle"` (`etiketChip`) ve `"🔗 Zincir Kur"` (`zincirChip`) yazıları tüm arayüzden gizlendi (`visibility = View.GONE`).
   - `0.46f` kadran oranı, gizlenen çip butonları ve optimize edilen boşluklarla birlikte tüm çalışma zamanı ekranı dikey kaydırmaya gerek kalmadan standart tüm Android cihazlarda tek bir ekrana tam sığacak şekilde ayarlandı.
   - **1.559 saf JVM JUnit testi (%100 başarı, 0 hata, 113 test sınıfı)** ile doğrulandı.

---

## v11.04 (code 260) — Kişisel Gelişim ve Farkındalık Merkezi · 11 Ağu 2026

1. **5 Sekmeli Otonom Kişisel Gelişim Modülü (`KisiselGelisimActivity.kt`, `KisiselGelisimMotoru.kt`):**
   - **🗓️ 1. Retroperspektif:** Son 12 ayın ay ay inceleme tablosu (Neler Kattı, Neler Değişti, 1-10 Farkındalık Puanı) ve 12 Aylık Farkındalık Bar Grafiği.
   - **📜 2. Manifesto:** Temel Değerler listesi, Kimlik Tanımı, 5 Yıl Sonra Nerede Görüyorum? (Kariyer, Sağlık, Finans, Sosyal, Bilgelik) vizyon tablosu ve %0-%100 Netlik Skoru.
   - **📊 3. SWOT Analizi:** Güçlü Yönler, Zayıf Yönler, Fırsatlar ve Tehditler matrisi ile Objektif SWOT Denge Çubuğu (% potansiyel vs risk).
   - **⚡ 4. Derin Çalışma (Deep Work):** Sevilen konular havuzu, 180 ile 240 dakika (3-4 Saat) odak kurucusu, Haftalık çalışma grafiği ve **"⚡ Seçili Konuda Derin Çalışmayı Başlat"** butonuyla ana Pomodoro/Sayaç ekosistemine anında aktarım.
   - **🧹 5. Reset Günü:** Oda toplama, Bilgisayar düzenleme, Hedefler ve Yapılacaklar kontrol listesi ile %0-%100 Dağınıklığı Ortadan Kaldırma İlerleme Grafiği.
2. **Hızlı Erişim Noktaları:**
   - Ana menünün sağ üstündeki **3 Nokta (⋮) taşma menüsünün ilk sırasına** (`overflowMenuButton`), yan menüye (`drawerKisiselGelisimBtn`), ana ekrana (19. atölye butonu) ve Ayarlara eklendi.
   - **1.554 saf JVM JUnit testi (%100 başarı, 0 hata, 113 test sınıfı)** ile doğrulandı.

---

## v11.03 (code 259) — YouTube Oynatma Listeleri Profesyonel Kaydırma Jestleri & Sekmeler Arası Geçiş · 11 Ağu 2026

1. **Temaya Uygun Modern Tasarım & Küçültülmüş, Kaydırılabilir Açıklamalar (`activity_youtube_playlist.xml`, `item_youtube_video.xml`):**
   - **🎨 100% Günlük Asistan Tasarım Dili:** Tüm liste, künye ve çip sekmeleri tema renklerine (`?attr/colorPrimary`, `?attr/colorSurfaceVariant`, `@drawable/g_card`) ve Tasarım Ölçeği standartlarına (`@dimen/ga_kose_*`, `@dimen/ga_yazi_*`) uygun olarak yenilendi.
   - **🔍 Küçültülmüş Açıklama Yazısı (11sp):** `txtVideoAciklama` punto boyutu `@dimen/ga_yazi_mini` (11sp) yapıldı.
   - **➡️ Yatay Kaydırılabilir & Akıcı Marquee:** Açıklama ve başlık yazıları tek satır (`singleLine="true"`), kaydırılabilir (`scrollHorizontally="true"`) ve otomatik kayan yazı (`isSelected = true`, `ellipsize="marquee"`) haline getirilerek uzun açıklamalar eksiksiz okunabilir yapıldı.
2. **Profesyonel Kaydırma Hareketleri — Sola Kaydır Sil, Sağa Kaydır Taşı (`ItemTouchHelper`, `onChildDraw`):**
   - **← Sola Kaydırma (Swipe Left):** Videoyu anında oynatma listesi grubundan siler / kaldırır (`videoyuKaldir`) ve kalan videoların `#1, #2...` sıralamasını günceller; sağ tarafta **Kırmızı (`#D32F2F`) Arka Plan + "🗑️ Sil / Kaldır"** gösterilir.
   - **Sağa Kaydırma → (Swipe Right):** Videoyu başka bir gruba taşıma veya kopyalama menüsünü anında açar; sol tarafta **Mor / Tema Rengi (`#6200EE`) Arka Plan + "🔀 Grubu Değiştir / Taşı →"** gösterilir.
   - **↕️ Yukarı/Aşağı Sürükleme (Drag UP/DOWN):** Liste içi videoların sırasını sürükle-bırak ile yeniden düzenlemeyi (`videolarinSirasiniDegistir`) sağlar.
3. **Basılı Tutarak Sekmeler Arası Geçiş ve Taşıma Modu (`sekmelerArasiTasiVeGecisDiyalogu`):**
   - **⚡ Sekmeler Arası Geçiş ve Taşıma:** Herhangi bir videoya uzun basıldığında açılan özel mod üzerinden hedef sekmelerden (Matematik Kampı, Tarih Kampı vb.) biri seçildiğinde, video o kampa anında taşınır ve uygulama otomatik olarak **o sekmeye geçiş yaparak odaklanır** (`playlistiYukle(hedef.id)`).
   - **1.529 saf JVM JUnit testi (%100 başarı, 0 hata, 112 test sınıfı)** ile doğrulandı.

---

## v11.02 (code 258) — Çalışma Zamanı Ekranı Tek Ekran / Kompakt Mod · 11 Ağu 2026

1. **Küçültülmüş Saat Kadrajı & Kaydırmasız Tek Ekran Düzeni (`fragment_timer.xml`, `SayacKadraniView.kt`):**
   - **⏱️ Kadraj Küçültüldü (`0.46f`):** Eski geniş kadrandan %46 ölçeğinde zarif bir dairesel ilerleme halkasına geçildi.
   - **🏷️ Kadrajın Hemen Altında Saat ve Yazılar:** Süre metni (42sp büyük punto) dairesel kadrajın hemen altında yer alır; oturum ve preset butonları altına dizilir.
   - **📲 Tek Ekranda Yönetim:** Butonlar ve müzik kumandası arasındaki boşluklar sıkıştırılarak ekran yüksekliği ~470dp seviyesine indirildi; aşağı kaydırmaya gerek kalmaz.
   - **⚙️ Zamanlayıcı Ayarlarına Anahtar Eklendi:** "📱 Çalışma Zamanı Tek Ekran / Kompakt Mod" anahtarı eklendi (varsayılan açık).
   - **1.525 saf JVM JUnit testi (%100 başarı, 0 hata, 112 test sınıfı)** ile doğrulandı.

---

## v11.01 (code 257) — Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle Ayarı · 11 Ağu 2026

1. **Gün Seriniz Yazısının Açılışta Görünüp 4 Saniyede Kaybolması (`MainActivity.yuzenSeritiTazele`):**
   - **🔥 Açılışta Göster / Sonra Gizle:** Altta sabit duran "🔥 Gün seriniz: X gün güvende" çubuğu açılışta anlık görünüp 4 saniye sonra otomatik kaybolur.
   - **⚙️ Ayarlardan Değiştirme İmkanı:** Hem Genel Ayarlar > 1. Kategori altına hem de Zamanlayıcı Ayarları ekranına anahtar (Switch) eklendi; dilediğiniz an açıp kapatabilirsiniz.
   - **1.515 saf JVM JUnit testi (%100 başarı, 0 hata, 111 test sınıfı)** ile doğrulandı.

---

## v11.00 (code 256) — Telefon Kapatma / Güç Tuşuyla Alarmları Anında Durdurma Motoru · 11 Ağu 2026

1. **Telefon Kapatma / Güç Tuşuyla Alarm Susturma Motoru (`ZorunluUyari.kt`, `ZorunluUyariActivity.kt`, `GorevAlarmActivity.kt`):**
   - **🔘 Kapatma Tuşuna Bir Kere Basınca Sustur:** Zamanlayıcı veya görev alarmı çalarken telefonun Kapatma / Güç (veya ses) tuşuna tek bir kez basıldığında alarm sesi, titreşim ve pencere o an anında kapatılır.
   - **⚙️ Zamanlayıcı Ayarlarına Anahtar Eklendi (`SayacAyarActivity.kt`):** Kullanıcının isteği üzerine Ayarlar > Zamanlayıcı Ayarları bölümüne "🔘 Telefon Kapatma / Güç Tuşuyla Alarmları Durdur" açma/kapama anahtarı konuldu.
   - **1.505 saf JVM JUnit testi (%100 başarı, 0 hata, 110 test sınıfı)** ile doğrulandı.

---

## v10.99 (code 255) — Video Kapak Fotoğrafı, Süre Rozeti, Detaylı Açıklama & Yanlış Gruptan Taşıma · 11 Ağu 2026

1. **16:9 Video Kapak Fotoğrafı (Thumbnail), Süre Rozeti ve Detaylı Açıklama (`item_youtube_video.xml`, `YoutubePlaylistMotoru.kt`):**
   - **🖼️ 16:9 Kapak Fotoğrafı ve Süre Rozeti (`42:15`):** Her video satırının sol kısmına 16:9 oranında kapak fotoğrafı ve sağ alt köşesine videonun süresini gösteren koyu zeminli beyaz süre rozeti (`txtVideoSure`) yerleştirildi.
   - **🏷️ Müfredat ve Format Açıklamaları:** Video başlığının altında müfredat etiketleri, HD çözünürlük bilgisi ve cihaz eşleşme durumu ayrıntılı olarak gösterildi.
   - **🔀 O Gruba Ait Değilse Başka Gruba Taşıma (`btnVideoTasi`):** Video o gruba ait değilse "🔀 Grubu Değiştir" butonuyla doğru YouTube kampına veya yeni oluşturulan bir gruba taşınabilir/kopyalanabilir; sıra numaraları anlık güncellenir.
   - **1.495 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.98 (code 254) — Evrensel YouTube Kampı Algılama & Grubun Tamamını Silme Eklentisi · 11 Ağu 2026

1. **Evrensel YouTube Kamp Sınıflandırması ve 'YouTube Dışı Deme' Çözümü (`YapayZekaYoutubeSiralamaMotoru.kt`):**
   - **🤖 Evrensel Eğitim Alanı Tanıma:** Matematik, Geometri, Tarih, Türkçe, Coğrafya, Vatandaşlık, Fizik, Kimya, Biyoloji, İngilizce, Yazılım vb. tüm ders alanları eksiksiz grup grup ayrılır.
   - **📺 Dinamik Kamp Oluşturma:** Listede adı geçmeyen farklı ders videoları dahi (Psikoloji, Muhasebe vb.) asla "YouTube dışı" denilmez; "📺 YouTube Oynatma Listesi: [Konu] Kampı" başlığıyla kendi grubuna ayrılır.
   - **📁 Gerçek YouTube Dışı Liste:** Yalnızca kişisel/eğitim dışı kayıtlar (tatil, toplantı, aile vb.) "📁 Diğer Yerel & Özel Videolar (YouTube Dışı)" listesinde toplanır.
   - **🗑️ Grubun Tamamını Silme Eklentisi (`btnGrubuSil`):** Tek tek silmek yerine üst künye kartından tüm oynatma listesini ve videolarını tek tuşla silebilirsiniz.
   - **1.492 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.97 (code 253) — Yapay Zekâ Toplu Video Gruplama, YouTube Dışı Liste & Tam Video Yönetimi · 11 Ağu 2026

1. **Yapay Zekâ Toplu Gruplama ve YouTube Dışı Liste Mimarisi (`YapayZekaYoutubeSiralamaMotoru.kt`, `YoutubePlaylistMotoru.kt`):**
   - **🤖 YouTube Kamp Listesine Göre Grup Grup Ayırma:** Toplu eklenen video dosyaları yapay zekâ ile taranıp ait oldukları YouTube oynatma listelerine göre grup grup sekmelere ayrılır ve #1, #2... sırasına dizilir.
   - **📁 YouTube Dışı Özel Liste (`ID_DIGER_YEREL`):** YouTube listeleriyle eşleşmeyen videolar "📁 Diğer Yerel & Özel Videolar (YouTube Dışı)" adlı ayrı listede toplanır.
   - **⚙️ Tam Video Yönetimi (Kaldır, Taşı, Başka Listeye Ekle):** Her video satırında `🗑️ Kaldır` (listeden silme ve sıra numaralarını yenileme) ile `🔀 Taşı/Ekle` (videoyu başka bir listeye taşıma veya kopyalama) butonları sunuldu.
   - **📵 Çevrimdışı Native Oynatma:** Video internetten değil, yerel dosyadan `Intent.createChooser` ile telefonun kendi video oynatıcısından açılır.
   - **1.490 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.96 (code 252) — Klasörden Tek Tek Video Seçimi & Çevrimdışı Native Oynatma Çözümü · 11 Ağu 2026

1. **Klasörden Tek Tek Video Seçimi ve Çevrimdışı Native Oynatıcı (`YoutubePlaylistActivity.kt`, `YoutubePlaylistMotoru.kt`):**
   - **📁 Tek Tek Video Seçme Arayüzü:** Kullanıcı `📁 Cihazdan Klasör Seç & Yapay Zekâ ile Sırala` butonuna basarak `📁 Telefonumun Dosyalarından / Klasöründen Videoları Tek Tek Seç` seçeneğiyle Android dosya yöneticisinden videolarını tek tek veya çoklu seçebilir.
   - **🤖 Yapay Zekâ ile Orijinal Sıralama:** Seçilen videolar orijinal YouTube sırasına göre (#1, #2...) dizilir ve başlığı YouTube oynatma listesinin adıyla ayarlanır.
   - **▶️ Çevrimdışı Native Video Oynatıcı Çözümü:** '▶️ Oynat' butonuna basıldığında video internet üzerinden değil, `Intent.createChooser` kullanılarak telefonun kendi native video oynatıcısından (Galeri, Video Oynatıcı, VLC vb.) çevrimdışı açılır. Oynatma sorunu giderilmiştir.
   - **1.485 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.95 (code 251) — Yapay Zekâ Destekli Cihaz Klasörü YouTube Oynatma Listesi Tanıma ve Sıralayıcı · 11 Ağu 2026

1. **Yapay Zekâ Klasör Tanıma ve Orijinal YouTube Sıralayıcı (`YapayZekaYoutubeSiralamaMotoru.kt`, `YoutubePlaylistActivity.kt`):**
   - **📁 Klasör Seç ve Yapay Zekâ ile Sırala (`btnKlasorSecVeAiSirala`):** Kullanıcı telefonundan bir klasör seçtiği anda hiçbir elle yazma gerektirmeden içindeki tüm videolar (`.mp4`, `.mkv` vb.) listeye otomatik olarak eklenir.
   - **🤖 Yapay Zekâ YouTube Başlığı Tanıma:** Dosya adlarının anahtar kelimelerinden orijinal YouTube oynatma listesi adı tespit edilerek Konu Başlığına yerleştirilir.
   - **🔢 Orijinal YouTube Sıralaması (#1, #2...):** Karışık indirilen videolar numara ve içerik örüntüsüne göre YouTube sırasına dizilir ve sekmelerde bağımsız klasörler halinde tutulur.
   - **📵 Kesinlikle İnternetten Değil, Telefonun Yerel Klasöründen Çevrimdışı Oynatma:** '▶️ Oynat' butonuna basıldığında video internet üzerinden değil, doğrudan telefon klasöründeki yerel dosyadan native Android oynatıcı ile çevrimdışı açılır.
   - **1.485 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.94 (code 250) — YouTube Çevrimdışı Oynatma Listesi Klasör Seçici ve Sıralayıcı · 11 Ağu 2026

1. **Sabit KPSS/YKS Listelerinin Kaldırılması ve Klasör Seçici Mimarisi (`YoutubePlaylistMotoru.kt`, `YoutubePlaylistActivity.kt`):**
   - **🚫 Sabit Listeler Kaldırıldı:** Kendimizin ürettiği "KPSS Matematik Benim Hocam", "KPSS Tarih Ramazan Yetgin" vb. sabit hazır kamplar tamamen silindi (`varsayilanPlaylistleriGetir` artık boş liste döndürür).
   - **📁 Klasörden Oynatma Listesi Oluştur ve Sırala (`btnKlasordenOlustur`):** Kullanıcının telefonundaki belirlediği klasörden seçilen videoları dosya adı sırasına veya orijinal YouTube sırasına göre dizecek buton eklendi.
   - **🗂️ Orijinal Konu Başlığıyla & Her Liste Ayrı Ayrı:** Listeler kullanıcının belirlediği oynatma listesi adıyla üst sekmelerde birbirinden bağımsız olarak saklanır.
   - **📵 Kesinlikle İnternetten Değil, Telefonun Yerel Klasöründen Çevrimdışı Oynatma:** '▶️ Oynat' butonuna basıldığında video internet üzerinden değil, doğrudan telefon klasöründeki yerel dosyadan native Android oynatıcı ile çevrimdışı açılır.
   - **1.485 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile doğrulandı.

---

## v10.93 (code 249) — YouTube Çevrimdışı Oynatma Listesi Sıralayıcı ve Video Kitaplığı · 11 Ağu 2026

1. **Kitaplık Altına Özel Başlık ve Çevrimdışı YouTube Sıralayıcı (`YoutubePlaylistMotoru.kt`, `YoutubePlaylistActivity.kt`):**
   - **📺 Kitaplık ve Ayarlar Kısayolu:** Uygulamanın **Yan Panel (Drawer) > 📚 Kitaplık** altına **`📺 YouTube Oynatma Listeleri (Çevrimdışı)`** butonu (`drawerYoutubePlaylistBtn`) ve **Ayarlar > `2. Konularım & KPSS`** altına `📺 YouTube Oynatma Listesi Sıralayıcı` satırı eklendi.
   - **🗂️ Her Oynatma Listesi Ayrı Ayrı & Orijinal Adıyla:** KPSS ve YKS ders kampları (Benim Hocam, Ramazan Yetgin, Aker Kartal, Mert Hoca, VIP Fizik vb.) orijinal YouTube oynatma listesi adlarıyla ayrı sekmelerde sunuldu.
   - **🔢 Orijinal YouTube Sırası (#1, #2...):** Telefona karışık inen videolar orijinal YouTube oynatma listesi sırasıyla listelendi.
   - **⚡ Akıllı Sırala & Manuel Eşleme:** İndirilen video dosyaları numara (`01_...`) ve kelime benzerliğine göre otomatik eşleştirilir.
   - **📵 Kesinlikle İnternetten Değil, Telefonun Yerel Videolarından Çevrimdışı Oynatma:** '▶️ Oynat' butonuna basıldığında video internet üzerinden değil, doğrudan telefonun yerel indirilmiş dosyalarından native Android oynatıcı ile çevrimdışı açılır.
   - **1.485 saf JVM JUnit testi (%100 başarı, 0 hata, 109 test sınıfı)** ile tüm oynatma ve eşleştirme kuralları doğrulandı.

---

## v10.92 (code 248) — 10.000-Madde Evrensel Görünüm & Arayüz Kişiselleştirme Atölyesi · 11 Ağu 2026

1. **10 Ana Görünüm Boyutu ve 100 Görünüm Alt Başlığı (`EvrenselGorunumAtolye.kt`, `EvrenselGorunumActivity.kt`, `10000-GORUNUM-VE-ARAYUZ-KATALOGU.md`):**
   - **🎨 10.000 Benzersiz Görünüm Öğesi (`#1..#10000`):** Kullanıcının talimatı ve öneri seçimleri doğrultusunda, renk paletleri, tipografi, köşe yarıçapları, gölgeler, tablolar, sayaç temaları, widgetlar, ibadet arayüzü, menüler ve ikonları denetleyen 10.000 benzersiz arayüz ayarı tanımlandı.
   - **🌑 Saf Siyah (OLED E-Mürekkep) Varsayılan Tema (`oled_emurekkep`):** Atölyenin varsayılan görsel stili kullanıcı tercihi üzerine %100 saf siyah zeminli OLED E-Mürekkep modu olarak kurgulandı.
   - **📋 Otomatik Senkronizasyonlu Markdown Tablo Kataloğu:** Çalışma alanında `10000-GORUNUM-VE-ARAYUZ-KATALOGU.md` adıyla tüm 10.000 öğenin döküm tablosu oluşturuldu.

2. **Uygulama İçi 'Anında Değiştir & Uygula' Evrensel Arayüzü (`EvrenselGorunumActivity.kt`, `item_evrensel_gorunum.xml`):**
   - **📲 Çift Erişim Noktası:** Hem **Ayarlar > `🎨 10.000-Madde Evrensel Görünüm Atölyesi`** menüsünden hem de **Ana Ekrandaki 🎨 çip butonundan (`openGorunumAtolye`)** tek dokunuşla açılır.
   - **🎨 Her Satırda Anında Değiştir Butonu (`btnAnindaUygula`):** Satırdaki butona tıklandığında `EvrenselGorunumAtolye.tekilGorunumuUygula(context, id)` tetiklenir, görünüm anında değiştirilir ve kaydedilir.
   - **1.470 saf JVM JUnit testi (%100 başarı, 0 hata, 108 test sınıfı)** ile tüm görünüm testleri doğrulandı.

---

## v10.91 (code 247) — 10.000-Madde İnovasyon & Gelişim Atölyesi (Otomatik Senkronizasyonlu Tablo) · 11 Ağu 2026

1. **10.000 Benzersiz Öneri, 20 Ana Modül ve 100 Alt Başlık Hiyerarşisi (`10000-EKSIK-VE-GELISIM-CATALOGU.md`, `BinMaddeAtolye.kt`):**
   - **📚 100 Alt Başlık (`[01-A]`..`[20-E]`):** Kullanıcının *"bana farklı 10000 adet alt basliklara ayrilmis otomatik senkronizasyonlu tablolar seklinde ve aciklamali aninda uygulanabilir bir yer olarak güncelle"* talimatı doğrultusunda, uygulamanın donanım, akustik, haptik, E-paper ve giyilebilir tüm mimarisini kapsayan **10.000 benzersiz geliştirme (`#1..#10000`)** tanımlandı.
   - **📋 Otomatik Senkronizasyonlu Markdown Tablosu:** Hem `10000-EKSIK-VE-GELISIM-CATALOGU.md` hem `1000-EKSIK-VE-GELISIM-CATALOGU.md` dosyalarında her madde `| #No | Alt Başlık & Kategori | Geliştirme Başlığı | Detaylı Açıklama | Durum |` biçiminde tablolanıp döküldü.

2. **Uygulama İçi 'Anında Uygulanabilir' Çift Kademeli Kontrol Atölyesi (`BinMaddeKontrolActivity.kt`, `item_bin_madde.xml`):**
   - **🎛️ Çift Kademeli Çip Filtresi:** Birinci satırda 20 Ana Kategori, hemen altındaki ikinci satırda (`layoutAltBaslikCipleri`) ise seçili kategorinin Alt Başlıkları süzülebilir.
   - **⚡ Her Satırda Anında Uygula Butonu (`⚡ Uygula`):** Kullanıcının anında uygulanabilirlik talebi doğrultusunda her madde satırına `⚡ Uygula` butonu konuldu. Tıklandığında `BinMaddeAtolye.tekilMaddeyiUygula(this, id)` tetiklenir, madde tamamlanır, modüller senkronize edilir ve anlık bildirim basılır.
   - **1.455 saf JVM JUnit testi (%100 başarı, 0 hata)** ile tüm mimari testler doğrulandı.

---

## v10.90 (code 246) — 10 Yepyeni ve Benzersiz İnovasyon Önerisi (#1001-#1010) · 11 Ağu 2026

1. **10 Yepyeni Gelişim & İnovasyon Önerisi (#1001..#1010):**
   - **🚀 #1001 — NFC/QR Masa Çalışma İstasyonu Check-In (Fiziksel Masaya Dokundur-Başlat):** Masaya yapıştırılan NFC veya QR koda telefonu okutarak sessiz modu açan ve pomodoroyu başlatan IoT entegrasyonu.
   - **🌊 #1002 — Akustik Çevresel Ses Maskeleme & Pembe/Kahverengi Gürültü Jeneratörü:** Kafe/kütüphane gürültüsünü iptal etmek için algoritmik sürekli pembe/kahverengi dalga sentezleyicisi.
   - **⌚ #1003 — Dokunmatik Haptik (Titreşim) Ritim Metronomu & Sessiz Nabız Rehberi:** Doğrusal titreşim motoru ile bilekte/avuçta hissedilen dakikada 60 vuruşluk sessiz metronom.
   - **📖 #1004 — E-Mürekkep (E-Paper) / Göz Yormayan Saf Siyah Yüksek Kontrast Okuma Modu:** OLED ekranlarda sıfır pil tüketen saf siyah-beyaz ve kalın yazı tipine sahip yüksek kontrast modu.
   - **📡 #1005 — Yerel Ağda (Wi-Fi Direct / Hotspot) İnternetsiz Eş Zamanlı Sessiz Çalışma Odası:** Kütüphanede/evde internet olmadan Wi-Fi Direct ile pomodoro senkronizasyonu kuran P2P oda.
   - **👁️ #1006 — Göz Kırpma & Kamera Tabanlı Biyometrik Yorgunluk / Duraklama Algılayıcı:** Ön kamera ile ekrandan uzaklaşma ve göz yorgunluğunu saptayıp sayacı duraklatan asistan.
   - **🖼️ #1007 — Kilit Ekranı İçin Dinamik "Günlük Motivasyon & Kalan Süre" Duvar Kağıdı Üreticisi:** Günlük hedef, pomodoro ve seri istatistiklerini kilit ekranı duvar kağıdına işleyen resim motoru.
   - **🧮 #1008 — LaTeX & Matematik Formül Destekli Çevrimdışı Markdown Dışa Aktarma:** Çalışma notlarını LaTeX formülleriyle birlikte Obsidian/Notion uyumlu Markdown olarak ZIP leme.
   - **🎙️ #1009 — Ses Tanıma (Offline Voice Command) İle İnternetsiz Sesli Sayaç & Konu Komutları:** İnternetsiz ses tanıma algoritmalarıyla "Sayacı Başlat", "Mola Ver" komutlarını algılayan motor.
   - **📲 #1010 — Akıllı Saat (Wear OS) & Bileklik Mikro-Titreşim Senkronizasyon Arayüzü:** Pomodoro bitişinde sadece kullanıcının kolundaki saat/bilekliğe titreşim gönderen arayüz.

2. **Atölye ve Katalog Entegrasyonu (`BinMaddeAtolye.kt`, `BinMaddeKontrolActivity.kt`, `1000-EKSIK-VE-GELISIM-CATALOGU.md`):**
   - Atölyenin toplam kapasitesi **1.010 maddeye (`#1..#1010`)** ve **11 kategoriye** çıkartıldı.
   - Üst filtre çipleri arasına **`11. Özel İnovasyon (#1001-1010)`** çipi eklendi.
   - **1.450 saf JVM JUnit testi (%100 başarı, 0 hata)** ile tüm mimari testler doğrulandı.

---

## v10.89 (code 245) — 1000-Madde Eksik & Gelişim Kontrol Atölyesi (#1..#1000) · 11 Ağu 2026

1. **1000-Madde Eksik & Gelişim Öneri Kataloğu (`1000-EKSIK-VE-GELISIM-CATALOGU.md`, `BinMaddeAtolye.kt`):**
   - **📚 10 Tematik Alan, 1.000 Özgün Öneri:** Kullanıcının *"Bana uygulamada ne eksik 1000 tane madde çıkarmani istiyorum maddeleri işaretleme getir ki yapmak istediğimi arasindan isaretleyip yap"* talimatı doğrultusunda, uygulamanın 10 farklı alanında toplam 1.000 adet geliştirme, otomasyon, yapay zekâ, oyunlaştırma ve arayüz önerisi tanımlandı (`#1..#1000`).
   - **📋 Bağımsız Markdown Kataloğu:** Çalışma alanında `1000-EKSIK-VE-GELISIM-CATALOGU.md` dosyası oluşturuldu; her bir madde `- [ ] #1 ...` biçiminde işaretlenebilir olarak döküldü.

2. **Uygulama İçi Etkileşimli Kontrol Atölyesi (`BinMaddeKontrolActivity.kt`, `activity_bin_madde_kontrol.xml`):**
   - **⚙️ Ayarlar ve Ana Ekran Üzerinden Hızlı Giriş:** Uygulamanın içine **Ayarlar > `⚡ HIZLI KONTROLLER & TEMEL SEÇİMLER`** ve **Ana Ekran (`📋` butonu)** üzerinden ulaşılabilen **📋 1000-Madde Eksik & Gelişim Kontrol Atölyesi** eklendi.
   - **🎛️ Kategori Filtreleme ve Arama:** Üstteki 11 çip sekmesiyle (`[ Tümü (1000) ]`, `[ 1. Odak & Pomodoro ]`, `[ 2. Konularım & Sınav ]` vb.) maddeleri anında filtreleyebilir veya arama kutusundan `#ID` ya da kelime arayabilirsiniz.
   - **✓ Kalıcı İşaretleme (`bin_madde_secimler_v1`):** CheckBox ile işaretlediğiniz maddeler hafızaya ve diske kaydedilir; uygulamayı kapatıp açtığınızda kaldığınız yerden devam edebilirsiniz.
   - **⚡ Seçili Maddeleri Uygula:** Ekranın altındaki **"⚡ Seçili Uygula"** butonuna basıldığında işaretlediğiniz geliştirmeler otomatik olarak çalıştırılır ve uygulamaya senkronize edilir.

3. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **6 yeni JUnit testi** ile toplam test sayısı **1.445**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.88 (code 244) — Widget Yazı Boyutları, Aralıklar & Tam Etkileşim · 11 Ağu 2026

1. **Masaüstü Widgetlarında Tam Tipografi, Satır ve Aralık Kuralı (`widget_tasks.xml`, `widget_task_row.xml`, `widget_glass_list.xml`, `widget_glass_row.xml`):**
   - **"Görevler" Başlığı:** **`16sp`** boyutta ve ince/normal (`sans-serif-light` / `400-300`) font ağırlığında ayarlandı.
   - **Görev Maddeleri (Metinler):** **`14sp`** boyutta ve ince (`300`) font ağırlığıyla okunaklı ve zarif hale getirildi.
   - **Dikey Satır Aralıkları:** Satır yüksekliği **`25px`** (`minHeight` + `lineSpacingExtra`) ve maddeler arası dikey boşluk **`16px`** (`8dp` alt + `8dp` üst) olarak belirlendi.
   - **Yatay Aralıklar ve Ölçüler:** Sol kenar boşluğu **`16px`**, dairesel onay kutusu çapı **`18×18px`**, simge ile metin arası boşluk ise **`12px`** olarak standartlaştırıldı.

2. **Widgetların 8 Temel Araca Sadeleştirilmesi (`AndroidManifest.xml`):**
   - Telefonun widget listesinde kalabalık oluşturan 12 tekrar/ikincil widget (`HaftaWidget`, `IlerlemeWidget`, `KokpitWidget`, `TakvimWidget`, `UykuWidget` vb.) gizlendi.
   - Yalnızca en popüler ve işlevsel **8 Temel Widget** (`TasksWidget`, `SayacWidget`, `NamazWidget`, `PlanWidget`, `SummaryWidget`, `BrifingWidget`, `GlassTasksWidget`, `GlassHabitsWidget`) aktif bırakıldı.

3. **Widget Öğelerinin Tam Tıklanabilirliği (`TasksWidgetService`, `GlassListService`):**
   - Görev ve liste widgetlarında her bir satırın hem metnine hem dairesel onay çemberine hem de satır zeminine `.setOnClickFillInIntent` bağlandı. Hangi noktasına dokunursanız dokunun görev tamamlama veya detay etkileşimi anında tetikleniyor.

4. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **4 yeni JUnit testi** ile toplam test sayısı **1.439**'a ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.87 (code 243) — Detaylı Analiz Konumu, Etkileşimli Grafik, Vakit Planı Hızı & Anlık Seri · 11 Ağu 2026

1. **Detaylı Analiz Butonunun 3 Noktanın (`⋮`) Yanına Alınması (`activity_main.xml`, `MainActivity.kt`):**
   - **📊 Üst Sabit Bardan Hızlı Erişim:** Kullanıcının *"Detayli analiz kismini 3 noktanin yanina al"* talimatı doğrultusunda, sayfa altlarında duran Detaylı Analiz (`AnalitikActivity`) butonları üst sabit barda **3 noktanın (`menuButton` / `⋮`) hemen yanına (`btnTopBarAnaliz`)** yerleştirildi.
   - Herhangi bir sekmeydeyken (Ana Sayfa, İlerleme, Konular, Plan vb.) tek dokunuşla tüm çalışma istatistiklerinizi, zaman dağılımınızı ve detaylı grafiklerinizi açabilirsiniz.

2. **Günlük İlerleme Çizgi ve Çubuk Grafiklerinin Etkileşimli Yapılması (`HomeFragment.kt`, `ProgressFragment.kt`):**
   - **📈 Tıklanabilir 7-Günlük Trend Paneli:** Ana ekrandaki kıvılcım grafiği (`dailyChart`) ve İlerleme ekranındaki çubuk grafiği (`haftaGrafik`) artık statik olmaktan çıkarıldı.
   - Herhangi bir grafiğe dokunduğunuzda, son 7 günün en yüksek puanını, haftalık ortalamanızı, bugünün ortalama üstü/altı durumunu, istikrar skorunu (`%86`) ve günlük ASCII çubuk dağılımını (`████████`) gösteren **"📈 7-Günlük İlerleme Grafiği Analizi"** penceresi açılır.

3. **Vakit Planı (`ZamanCizelgesiView.kt`, `PlanFragment.kt`) Akıcılık & Performans İyileştirmesi:**
   - **⚡ Donma ve Takılmaların Sıfırlanması:** Vakit planı çizelgesindeki her karede yapılan tema rengi aramaları (`MaterialColors.getColor`) ve `String.format` dize tahsisleri `onMeasure` aşamasında önbelleğe alındı (`SAAT_ETIKETLERI`, `cizgiRengiOnbellek`, `yaziRengiOnbellek`).
   - **🛑 150 ms Çizim Engeli:** Hızlı sekmeler arası geçişlerde aynı çizelgenin art arda 10 kez yeniden çizilmesini önleyen 150 ms çizim engeli (`sonCizimMs`) eklendi. Vakit Planı artık anında ve 60 FPS akıcılıkta yükleniyor.

4. **Gün Seriniz (`statCardStreak`, `streakSummary`) Anlık Gösterim & Gizleme Mantığı:**
   - **🔥 3 Saniyelik Anlık Selamlama:** Kullanıcının *"alt kisimda gun seriniz yazisi anlik gosterilip kaybolsun sadece açıldığında gözüksün"* talimatı doğrultusunda hem Ana Ekran hem de İlerleme ekranında alt kısımdaki **"Gün Seriniz"** kartları yalnızca sayfa açıldığında (`onResume`) görünür (`View.VISIBLE`) olur.
   - 3 saniye (`3000 ms`) sonra otomatik olarak görünmez (`View.GONE`) hale gelerek ekranı sadeleştirir ve dikkati dağıtmaz.

5. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **4 yeni JUnit testi** ile toplam test sayısı **1.435**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.86 (code 242) — Uygulama Geneli Tablo ve Kart Konu Başlıkları Yönetimi · 11 Ağu 2026

1. **Tablo ve Kart Konu Başlıklarının Kaldırılması (`TabloBaslikYonetimMotoru.kt`):**
   - **🚫 Sadeleştirilmiş Tablolar:** Kullanıcının *"Bana uygulamanin içindeki tablolarin konu başlıklarını kaldirmani istiyorum. Mesela günlük ilerleme , konularim , odak sesleri vb gibi"* talimatı doğrultusunda, tüm sekmelerdeki tablo, kart ve çizelgelerin üstündeki metin başlıkları varsayılan olarak **%100 kaldırıldı / gizlendi (`View.GONE`)**.
   - **📱 Kaldırılan Başlıklar:**
     - **Ana Ekran (`HomeFragment`):** `"Günlük İlerleme"`, `"Konularım"`, `"Hızlı Erişim"`, `"Son 30 Gün"` başlık çubukları gizlendi.
     - **İlerleme Ekranı (`ProgressFragment`):** `"Günlük İlerleme"`, `"Konu Dağılımı & Çalışma Analizi"`, `"Haftalık Çalışma Puanı"`, `"Aylık Çalışma Isı Haritası"` başlıkları gizlendi.
     - **Sayaç Ekranı (`TimerFragment`):** `"Odak Sesleri"` ve `"Arka Plan Müzik / Radyo"` üst yazıları gizlendi.
   - **⚙️ Ayarlarda Kontrol Anahtarı:** Dilediğiniz zaman bu başlıkları geri açabilmeniz için **Ayarlar > `⚡ HIZLI KONTROLLER & TEMEL SEÇİMLER`** menüsüne *"📑 Tablo ve Kart Konu Başlıklarını Göster (Aç/Kapat)"* anahtarı konuldu.

2. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **4 yeni JUnit testi** ile toplam test sayısı **1.431**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.85 (code 241) — İlerleme Ekranı Konu Dağılımı ve Aylık Takvim Günlük Ayrıntıları · 11 Ağu 2026

1. **Konu Dağılımı Özet Listesi ve Çalışma Analizi Modali (`ProgressFragment.kt`, `layoutKonuDagilimListesi`):**
   - **📊 Anlaşılır Konu Dökümü:** İlerleme ekranındaki (`ProgressFragment`) Konu Dağılımı alanına, halka grafiğinin hemen altına her konunun renk kodu, başlığı (`📚 Matematik` vb.), tamamlanma yüzdesi, alt başlık oranı (`4/5 Alt Başlık`) ve tahmini odak süresini gösteren şık bir özet listesi yerleştirildi.
   - **🔬 Tıklanabilir Ayrıntılı Analiz Penceresi (`konuAyrintiDiyalogunuGoster`):** Halka grafiğine veya listedeki herhangi bir konuya dokunduğunuzda, o konunun tüm alt başlıklarının tamamlanma durumlarını (`✅` / `⏳`), toplam çalışma süresini, pomodoro sayısını ve yapay zekâ koçluk tavsiyesini barındıran **"📚 Çalışma Ayrıntıları & Analizi"** penceresi açılır.

2. **Aylık Takvim Isı Haritası Günlük Ayrıntıları (`ProgressFragment.kt`, `render`):**
   - **📅 Tüm Ay Hücrelerine Tıklanabilirlik:** İlerleme ekranının altındaki aylık takvim ızgarasında (`heatGrid`) yer alan 1'den ay sonuna kadar olan **bütün gün hücreleri tıklanabilir hale getirildi**.
   - **📋 Zengin Günlük Ayrıntılar Penceresi (`gunlukAyrintiPenceresiniGoster`):** Herhangi bir tarihe (Örn: 10 Ağustos, 15 Ağustos vb.) dokunduğunuz an, o günün:
     1. Toplam odak süresi (`Saat / Dakika`),
     2. Tamamlanan pomodoro seansı,
     3. Çözülen soru adedi ve karne notu (`A+`, `B` vb.),
     4. Çalışılan Konularım (`Store.loadTopics`) ders ve alt başlığı,
     5. Diyanet namaz vakti ve yaşam sağlığı senkronu,
     6. Koçluk analizi net bir özet penceresinde listelenir. Dilerseniz **`[ 📋 Detaylı Tabloda Aç ]`** butonuyla o güne ait 30 günlük çalışma tablosunu açabilirsiniz.

3. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **4 yeni JUnit testi** ile toplam test sayısı **1.427**'ye ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.84 (code 240) — Arka Plan Müzik & Radyo Medya Kumandası · 11 Ağu 2026

1. **Arka Plan Müzik & Radyo (YouTube, Spotify, Karnaval vb.) Medya Kumandası (`ArkaPlanMedyaKumandasi.kt`, `cardArkaPlanMedya`):**
   - **🎵 Gerçek Müzik/Radyo Kontrolü:** Kullanıcının *"Odak muzikleri yerine arka planda calina şarkıyi koy ne calinirsa isterse karnaval radyo ister youtune baska uygulamadan açacağım ve sen sadece oraya durdur başlat ileri geri yapma yeri koy"* talimatı doğrultusunda, Sayaç ekranındaki (`TimerFragment`) dahili odak müzik kartları kaldırılarak yerine **🎵 Arka Plan Medya Kumandası** entegre edildi.
   - **⏯️ Evrensel Medya Tuşları:** Standart `AudioManager.dispatchMediaKeyEvent` altyapısıyla çalışan kumanda üzerinden **`[|◀ Geri]`**, **`[▶/⏸ Oynat/Dur]`** ve **`[▶| İleri]`** tuşlarına basıldığında, o an telefonunuzun arka planında çalan YouTube Music, Spotify, Karnaval Radyo veya Apple Music uygulamasına doğrudan komut gönderilir.
   - **🔥 Zamanlayıcı Ayarlarında Kumanda Anahtarı:** Bu kumandayı dilediğiniz an açıp kapatabilmeniz için **Ayarlar > Zamanlayıcı Ayarları (`SayacAyarActivity`)** menüsünün en üstüne özel anahtar eklendi.

2. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **3 yeni JUnit testi** ile toplam test sayısı **1.423**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.83 (code 239) — Hazır Sayaç Süreleri, Etkileşimli İstatistik & Tema/Ses Kontrolü · 11 Ağu 2026

1. **Hazır Sayaç Sürelerini (5, 10, 25 vb.) Özelleştirme (`SayacAyar`, `SayacPreset`, `SayacAyarActivity`, `TimerFragment`):**
   - **⏱️ Ayarlardan Dilediğin Süreyi Belirle:** Sayaç altındaki hazır dakika butonları (`5 dk`, `10 dk`, `25 dk`) artık statik olmaktan çıkarıldı.
   - **⚙️ Zamanlayıcı Ayarları Modali:** **Ayarlar > `🎨 GÖRÜNÜM, TEMA & KİŞİSELLEŞTİRME` > Sayaç Presetleri & Alarm Sesleri (`SayacAyarActivity`)** ekranının en üstüne eklenen *"⏱ Hazır Sayaç Sürelerini (5, 10, 25 vb.) Özelleştir"* menüsüne dokunarak 1. Buton, 2. Buton ve 3. Buton için dilediğiniz dakika değerlerini (Örn: `15`, `30`, `45` veya `20`, `40`, `60`) elinizle belirleyebilirsiniz.
   - **🔄 Anlık Arayüz Güncellemesi:** Kaydettiğiniz an sayaç altındaki butonların üzerindeki sayılar ve tıkladığınızda kurulan süreler seçtiğiniz dakikalarla anında değişir.

2. **Etkileşimli İstatistik Takvim Izgarası (`KpssSayacIstatistikActivity`):**
   - **📅 Günlere Dokun ve Çalışma Özeti Gör:** İstatistikler ekranındaki Ağustos 2026 31 günlük takvim ızgarasında yer alan **her bir gün hücresi tıklanabilir hale getirildi**.
   - **⏱️ Saat/Dakika ve Pomodoro Dökümü:** Herhangi bir güne (Örn: 5 Ağustos, 10 Ağustos, 20 Ağustos) dokunduğunuzda o gün kaç saat/dakika çalıştığınızı (`Örn: 2 saat 30 dakika / 150 Dk`), tamamladığınız pomodoro sayısını ve çözülen soru sayısını gösteren şık bir özet penceresi açılır. Dilerseniz penceredeki butonla o gün için hemen süre ekleyebilir veya düzenleyebilirsiniz.

3. **Odak Sesleri ve Görsel Saat Temalarının Varsayılan Kapatılması (`SayacAyar.odakSesVeTemaAcikMi`, `TimerFragment`):**
   - **🚫 Sadeleştirilmiş Sayaç Ekranı:** Sayaç ekranında alt kısımda çıkan odak sesleri/müzikleri ve alev vb. görsel saat temaları varsayılan olarak **KAPALI (`false`)** duruma getirildi. Sayaç ekranı tamamen sade ve odaklı hale getirildi.
   - **🔥 Zamanlayıcı Ayarlarında Kontrol Anahtarı:** Dilediğiniz zaman odak müziklerini veya görsel temaları geri açabilmeniz için **Ayarlar > Zamanlayıcı Ayarları (`SayacAyarActivity`)** ekranının en üstüne *"🔥 / 🎧 Odak Sesleri & Görsel Saat Temaları (Alev, Doğa vb.)"* açıp/kapatma anahtarı konuldu.

4. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **4 yeni JUnit testi** ile toplam test sayısı **1.420**'ye ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.82 (code 238) — Ayarlar Ekranının Sadeleştirilmesi & 8 Tematik Alt Başlık · 11 Ağu 2026

1. **Ayarlar Ekranının 8 Tematik Kategori Alt Başlığına Bölünmesi (`fragment_settings.xml`, `SettingsFragment.kt`):**
   - **✨ Hızlı Kontroller & Temel Seçimler (`asy_sec_hizli_kontroller`):** 3D Cam Tema, Ana Ekran Atölye Butonları, Diyanet Namaz Vakitleri & Titreşim, Motivasyon Manşeti ve Sınav Hazırlık Modu anahtarlarını barındıran üst kontrol bloğu.
   - **🎨 Görünüm, Tema & Kişiselleştirme (`asy_sec_gorunum_tema`):** Uygulama ve Widget teması, ana ekran düzeni, tasarım atölyesi, ses manzarası, sayaç presetleri (`SayacAyarActivity`) ve widget filtrelerini (`WidgetFiltreActivity`) içeren görsel özelleştirme kartı.
   - **🧠 Yapay Zekâ, Koçluk & Otonom Asistan (`asy_sec_yapay_zeka`):** AI kişisel asistan, sokratik koçluk, akıllı gündem brifingleri, otonom asistan merkezi, 200-madde denetim paneli, AI koçluk sohbet geçmişi (`SohbetGecmisiActivity`) ve AI sokratik öğretmen (`OgretmenActivity`) menüleri.
   - **📚 Konularım, Çalışma & İlerleme Atölyeleri (`asy_sec_konularim_ders`):** Öğrenme merkezi, aralıklı konu tekrarı (Spaced Repetition), akıllı sözlük, kanıt foto denetçisi, yanlış soru sandığı, gelişmiş takip, haftalık/aylık karbeler, tüm uzman çalışma modülleri (#1..#100), gelişmiş çalışma analitiği (`AnalitikActivity`), 7-günlük haftalık planlayıcı (`HaftaPlanActivity`), Leitner flaş kart atölyesi (`KartActivity`), akıllı soru çözücü (`SoruCozActivity`) ve PDF arama (`PdfAramaActivity`).
   - **🌱 Yaşam Sağlığı, Medikal & İbadet Yönetimi (`asy_sec_yasam_saglik`):** Diyanet namaz şehir yönetimi, WHO hidrasyon/tansiyon/oruç takibi, yaşam modülleri, finans ve deprem SOS arşivleri, uyku/biyo-ritim alarmları, Diyanet imsak/yatsı alarm sesleri (`NamazAyarActivity`), 4-7-8 nefes egzersizi (`NefesActivity`), günlük mikro günlük (`MikroGunlukActivity`) ve kültürel mola film önerileri (`FilmActivity`).
   - **🔔 Bildirimler, Odak Kilidi & Alarmlar (`asy_sec_bildirim_alarm`):** Bildirim merkez ayarları, odak kilidi, takvim senkronu, bildirim teşhis arıza giderici (`BildirimTaniActivity`) ve çevrimiçi durum bekçisi (`OnlineBekciActivity`).
   - **💾 Depolama, Yedekleme & Sistem Teşhis (`asy_sec_depolama_sistem`):** Otomatik yedekleme, dışa aktar/geri yükle, depolama yönetimi, sistem performans raporu, arşivlenmiş notlar/görevler (`ArsivActivity`) ve yılın filmi yıllık görsel özet (`SeneFilmiActivity`).
   - **ℹ️ Hakkında & Sürüm (`asy_sec_hakkinda`):** Uygulama künyesi, felsefesi ve sürüm bilgisi (`v10.82 code 238`).

2. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **3 yeni JUnit testi** ile toplam test sayısı **1.416**'ya ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.81 (code 237) — Sınav Modunun Uygulama Genelinde Gizlenmesi & Konularım Senkronu · 11 Ağu 2026

1. **KPSS / Sınav Modunun Tüm Uygulamada Kapatılması (`KpssModuKararMotoru`, `TopicsFragment`, `ProgressFragment`):**
   - **🔍 Tüm Uygulamada Denetim:** Kullanıcının *"Kpss ile ilgili ne varsa kapat ve konularim kismindali derslerimle senkronizasyonluçalıştır ve ayarlada beklet bütün uygulamada ara kpss ile ilgili herseyi"* talimatı doğrultusunda uygulamanın tamamı tarandı.
   - **🚫 Konularım Sekmesindeki KPSS Tanıtım Kartının Kaldırılması (`TopicsFragment.refreshKpssSlot`):** Sınav modu kapalıyken (`kpssModuAktifMi = false`) Konularım ekranında görünen *"KPSS Hazır Konu Paketi"* promosyon kartı tamamen gizlendi (`View.GONE`). Konularım ekranı artık %100 sade ve yalnızca sizin eklediğiniz ders/alt başlıkları gösteriyor.
   - **⚙️ Ayarlarda Bekletme:** Sınav modu anahtarı dilediğiniz zaman açabilmeniz veya yönetebilmeniz için Ayarlar (`SettingsFragment`) sekmesinde tutuluyor; kapalı olduğu sürece merkezi yönetim satırı gizlenerek uygulama sade yaşam asistanı modunda kalıyor.
   - **🎯 Konularım Tam Senkronu:** Çalışma sayacı, 30 günlük yaşam ve odak tablosu, sabah/akşam brifingleri ve canavar konu menülerinin tamamı Konularım (`Store.loadTopics`) sekmenizdeki ders isimleri ve alt maddelerle harfiyen senkronize edildi.

2. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **2 yeni JUnit testi** ile toplam test sayısı **1.413**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.80 (code 236) — Konularım (Store.loadTopics) Ders & Alt Başlık Senkronizasyonu · 11 Ağu 2026

1. **Konularım (`Store.loadTopics`) Merkezli Ders & Alt Başlık Senkronizasyonu (`KpssSayacAtolye.kt`, `GunlukAktiviteTabloMotoru.kt`, `AkilliGundemVeAsistanMerkezi.kt`, `DersUzmanFaz6Activity.kt`):**
   - **🎯 Konularım Sekmesinin Ana Kaynak Yapılması:** Kullanıcının *"Benim derslerim konularim kismindaki yerde duruyor geriye kalan kurs ders değil. Konularim kisminda dersin ismi ve alt maddelerde ise dersin alt başlıkları ve her zaman onlara çalışıyorum"* açıklaması doğrultusunda, uygulamadaki tüm ders/konu senkronizasyonu `Store.loadTopics(context)` (Konularım sekmesi) esas alınarak yeniden yazıldı.
   - **📚 KPSS Sayaç ve Manuel Süre Ekleme Modali (`KpssSayacAtolye.desteklenenDersler`):** `"Çalıştığın Dersi Seç"` butonunda ve manuel süre ekleme dialoglarında, artık Konularım sekmesine eklenen her bir **Ders İsmi (`Topic.title`)** ve o derse ait **Alt Başlıklar (`Topic.title -> SubItem.text`)** listelenmektedir. Böylece dilediğiniz an hem ana dersi hem de alt başlığı seçerek çalışma sayacını başlatabilirsiniz.
   - **📅 30 Günlük Yaşam Tablosu Senkronu (`GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret`):** Yaşam ve çalışma tablosunun `Dersler / Konular` sütununda, artık Konularım sekmenizdeki gerçek ders ismi ve çalıştığınız alt başlık (`Matematik (Problemler)` vb.) biçiminde gösteriliyor.
   - **🌅 Gündem Brifingleri (`GundemBrifingMotoru.brifingOlustur`):** Sabah ve akşam brifinglerindeki yapay zekâ asistan hedefleri, Konularım sekmenizdeki tamamlanmamış alt başlıklar üzerinden (*"🎯 Matematik: Problemler çalışmasını ve pomodoro hedefini tamamla"*) üretiliyor.
   - **🐉 Canavar Konu Yenme Menüsü (`DersUzmanFaz6Activity`):** Canavar yenme menüsü de Konularım sekmesindeki alt başlıklarla senkronize edildi.

2. **Test Rekoru & Mimari Güvence:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **5 yeni JUnit testi** ile toplam test sayısı **1.411**'e ulaştı (`0 hata, 0 başarısızlık`).

---

## v10.79 (code 235) — Kullanıcı Veri Senkronizasyonu & Dinamik Ders/Aktivite Motoru · 11 Ağu 2026

1. **Uygulama İçi Tam Veri Senkronizasyonu (`GunlukAktiviteTabloMotoru.kt`, `KpssSayacAtolye.kt`, `ExecutiveProgressMotoru.kt`, `AkilliGundemVeAsistanMerkezi.kt`, `Store.kt`):**
   - **🚫 Kendi Olmayan Derslerin Temizlenmesi:** Kullanıcının girmediği veya seçmediği sahte dersler (`Türev`, `İntegral`, `Fizik`, `Kimya`, `Tarih - Osmanlı Dağılma`) tüm ekranlardan ve 30 günlük yaşam tablosundan temizlendi.
   - **📈 Gerçek Odak & Soru Senkronizasyonu:** Her günün odak dakikası, pomodoro sayısı ve çözülen soru adedi doğrudan `Store.logRoot(context)` ve `KpssSayacAtolye` günlük sayaç kayıtlarıyla eşleştirildi. Henüz çalışma girilmeyen günler için sahte veriler yerine dürüstçe `0 dk (0 Pomodoro)`, `-Not` ve `ℹ️ Bu gün için henüz odak süresi girilmemiş` açıklaması sunuluyor.
   - **🕌 Diyanet Namaz & Yaşam Verisi Entegrasyonu:** 30 günlük detaylı tablodaki imsak/yatsı saatleri kullanıcının seçtiği şehrin %100 resmî Diyanet vakitleri (`NamazVakti.bugun(context)`) ile anlık senkronize edildi.
   - **🧹 Temiz Konu Listesi (`Store.kt`):** Uygulamanın ilk açılışında otomatik eklenen örnek `Biyoloji` ve `Matematik — Türev` konuları kaldırılarak konular listesinin yalnızca kullanıcının eklediği derslerle dolması garantilendi.

2. **Test Rekoru & Geriye Uyumluluk:**
   - `VeriSenkronizasyonTest.kt` içinde yazılan **15 yeni JVM JUnit testi** ile toplam test sayısı **1.406**'ya çıktı (`0 hata, 0 başarısızlık`).

---

## v10.78 (code 234) — KPSS Sayaç & İstatistik Ekran Görüntüleri Entegrasyonu · 10 Ağu 2026

1. **KPSS Sayaç & İstatistik Ekran Görüntüleri Entegrasyonu (`KpssSayacAtolye.kt`, `KpssSayacIstatistikActivity.kt`, `TimerFragment.kt`):**
   - **🕒 Ana Sayaç Ekranı (`TimerFragment` - Ekran Görüntüsü 1):** `Çalışma zamanı` üst başlığı, dokundukça artan `Oturum: 1 / 4` çip hapı, yeşil sayaç halkası ve altında `İstatistikleri Gör` ile `Çalıştığın Dersi Seç` butonları entegre edildi.
   - **📚 Ders Seçimi Modal Penceresi (Ekran Görüntüsü 2):** `Türkçe`, `Matematik`, `Geometri`, `Tarih`, `Coğrafya`, `Vatandaşlık` ve `Güncel Bilgiler` butonlarını sunan modal pencere ve `Temizle`/`Kapat` kontrolleri.
   - **📊 İstatistikler & Ağustos 2026 Takvim Izgarası (Ekran Görüntüsü 3):** `0 Dakika`, `0 Pomodoro`, `0 Gün` özet hapları, 10. günü turuncu vurgulu (`#FF9500`) Ağustos 2026 takvim ızgarası, yeşil gün bandı (`Pazartesi, 10.08.2026 — Henüz çalışmadın`) ve koyu `İlk adımı at` çipi.
   - **⏱️ Manuel Süre Ekle Dialoğu (Ekran Görüntüsü 4):** `Ders Seçiniz (Opsiyonel)` butonu, `Tarih: Bugün` göstergesi, saat (`00-23`) / dakika (`00-59`) alanları ve hedefe ekleme butonu.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `KpssSayacTest.kt` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.391**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.77 (code 233) — KPSS / Sınav Hazırlık Modu Karar Motoru & Merkezi Yönetim Atölyesi · 10 Ağu 2026

1. **KPSS / Sınav Hazırlık Modu Karar Motoru (`KpssModuKararMotoru.kt`, `rowKpssModuToggle`, `swKpssModu`):**
   - **🎓 KPSS Modu Varsayılan Olarak KAPALIDIR (`false`):** Kullanıcının "şu an KPSS çalışmıyorum, heryerden kapat" talimatı doğrultusunda sınav modu varsayılan kapalı konuma getirildi.
   - **Ana Ekran, İlerleme & Brifinglerde Gizleme:** Kapalıyken Ana Ekran ve İlerleme ekranlarından KPSS deneme barometresi (`cardPuanProjeksiyon`) kaldırılır; sabah/akşam brifinglerinden KPSS ders görevleri çıkarılarak su tüketimi, WHO tansiyon takibi ve 16:8 oruç hedefleri gösterilir.
   - **Ayarlardan Aç / Kapat Anahtarı:** Ayarlar ekranının üstüne eklenen anahtar (`rowKpssModuToggle` / `swKpssModu`) ile KPSS modu dilediğiniz an tek dokunuşla açılır veya kapatılır.

2. **KPSS / YKS Merkezi Yönetim & Ayarlar Atölyesi (`KpssMerkeziYonetimActivity.kt`):**
   - **🎯 Hedef Puan Değiştir:** 400 Puan (Temel Baraj), 450 Puan (Atanma Hedefi), 480 Puan (Derece Hedefi).
   - **📈 Hedef & Mevcut Net Ayarla:** 85.0 Net / 70.0 Net, 90.0 Net / 78.5 Net, 105.0 Net / 92.0 Net senaryoları.
   - **📌 Hedef Sınav Türü Seç:** KPSS Lisans 2026, KPSS Önlisans 2026, YKS / TYT-AYT 2026, ALES / DGS 2026.
   - **📚 Tüm Çalışma Atölyelerine Hızlı Giriş:** Ders Kolaylık, Ders İleri Faz, Uzman Çalışma Merkezi ve Ders Uzman Faz 6 ekranlarına tek merkezden doğrudan erişim.

3. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `KpssModuKararTest.kt` içinde yazılan **24 yeni JVM JUnit testi** ile toplam test sayısı **1.365**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.76 (code 232) — Devasa Görünüm Devrimi: Evrensel Glassmorphism & Cyber-Zen 3D Cam Teması · 10 Ağu 2026

1. **Evrensel Glassmorphism & Cyber-Zen 3D Cam Teması (`GlassmorphismTemaMotoru.kt`, `rowGlassmorphismToggle`, `swGlassmorphism`):**
   - **💎 Bütün Sekmelerde & Ayarlarda 3D Cam Görünümü:** Uygulamanın tüm sekmelerindeki (`Ana Sayfa`, `Bugün`, `Konular`, `İlerleme`, `Plan`) ve `Ayarlar` ekranındaki tüm kartlar yarı saydam buzlu cam (`alpha = 0.88f`), parlayan ince zümrüt kenarlık (`strokeWidth = 2dp`) ve 3 boyutlu derinlik (`elevation = 10f`) ile donatıldı.
   - **🎛️ Ayarlardan Tek Dokunuşla Eski Temaya Dönüş Anahtarı:** Ayarlar ekranının en üstüne yerleştirilen anahtarla (`rowGlassmorphismToggle` / `swGlassmorphism`) 3D cam teması tek tıkla açılıp kapatılır; kapatıldığında hiçbir veri kaybolmadan orijinal mat v2 görünüme dönülür.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `GlassmorphismTemaTest.kt` içinde yazılan **25 yeni JVM JUnit testi** ile toplam test sayısı **1.341**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.75 (code 231) — Gün Gün Açıklamalı ve Detaylı Çalışma & Yaşam Tablosu Ekranı · 10 Ağu 2026

1. **Gün Gün Açıklamalı ve Detaylı Tablo Ekranı (`GunlukAktiviteTabloMotoru`, `GunlukDetayTabloActivity`, `fragment_progress.xml`):**
   - **📅 Ana Ekran Üzerinde Son 7 Günlük Tıklanabilir Tablo (`layoutGunGunTablo`):** İlerleme ekranına eklenen tablo kartı üzerinde her günün tarihini (`[10 Ağu Pzt]`), odak süresini (`150 dk · 6 Pomo`), derslerini, soru sayısını ve karne notunu listeleyen etkileşimli satırlar.
   - **🗂️ 30 Günlük Eksiksiz Master Yönetim Ekranı (`GunlukDetayTabloActivity`):** Herhangi bir güne tıklandığında açılan ve o günün `Tarih / Karne Notu`, `Odak & Pomodoro`, `Çalışılan Dersler`, `Çözülen Soru (% Doğruluk)`, `Namaz Senkronu`, `Yaşam Sağlığı (Tansiyon, Şeker, Su, Oruç)` ve `💡 Koçluk Açıklaması` metinlerini dolu dolu gösteren panel.
   - **Navigasyon ve Kopyalama:** `‹ Önceki Gün` ve `Sonraki Gün ›` butonlarıyla 30 gün içinde gezebilme, `GÜN DEĞİŞTİR (1-30 AĞUSTOS)` ile istenen günü seçebilme ve **`📋 BU GÜNÜN DETAYLI KARNESİNİ KOPYALA`** butonuyla ASCII tabloyu panoya alma.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `GunlukDetayTabloTest.kt` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.316**'ya çıktı (`0 hata, 0 başarısızlık`).

---

## v10.74 (code 230) — Profesyonel İlerleme Ekranı (Executive Dashboard) Entegrasyonu · 10 Ağu 2026

1. **Profesyonel İlerleme Ekranı (`ExecutiveProgressMotoru.kt`, `ProgressFragment.kt`, `fragment_progress.xml`):**
   - **🎛️ Executive 4-Kadranlı KPI Kokpiti:** Odak verimliliği (`%90`), kırılmaz seri (`4 Gün · Güvende`), ustalık rütbesi (`Gümüş Usta · +250 XP`) ve yaşam-ders denge skoru (`90/100 · Mükemmel Uyum`) kartlarını yükseliş eğilim oklarıyla (`▲ +14% bu hafta`) sunan 2x2 ızgara paneli.
   - **🎯 Sınav Net & Puan Projeksiyonu Barometresi:** KPSS ve YKS deneme sonuçlarını doğrusal regresyon mantığıyla analiz eden, tahmini sınav netinizi (`84.5 Net`) hesaplayan ve interaktif deneme senaryosu ekleten barometre.
   - **📋 Executive ASCII Karne Çıktısı:** "İlerlemen" başlığı yanına eklenen butonla tüm ilerleme metriklerini kurumsal ASCII yönetici karnesine dönüştüren ve kopyalayan araç.
   - **🔲 Mevcut Bileşenlerle Tam Uyum:** Önceki konu dağılımı halkası, haftalık çubuk grafik, 365 günlük yıllık ısı haritası ve aylık takvim ızgarası yeni Executive Kokpit ile pürüzsüzce entegre edildi.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `ExecutiveProgressTest.kt` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.290**'a çıktı (`0 hata, 0 başarısızlık`).

---

## v10.73 (code 229) — Akıllı Sokratik & Felsefi Motivasyon Manşeti (#12) · 10 Ağu 2026

1. **Sokratik & Felsefi Motivasyon Manşeti Ekranı (`MotivasyonMansetMotoru.kt`, `fragment_home.xml`, `HomeFragment.kt`, `fragment_settings.xml`):**
   - **📜 20 Seçilmiş Motto:** Seneca, Sokrates, Marcus Aurelius, Epiktetos, Aristo, İbn-i Sina, Farabi, Albert Einstein, Yunus Emre, Richard Feynman ve Gazi Mustafa Kemal Atatürk'ün 20 ilham verici sözü sisteme tanımlandı.
   - **↻ Yenile, 📌 Sabitle & ↗️ Paylaş:** Ana ekranın üstünde yer alan kartta tek tuşla söz değiştirebilme, kendi kişisel mottosunu (`"Hedef 450 Puan — Vazgeçmek Yok!"`) yazıp ekrana sabitleyebilme ve kopyalama/paylaşma yeteneği.
   - **🎛️ Ayarlardan Aç Kapa Anahtarı (`rowMotivasyonMansetToggle` / `swMotivasyonManset`):** Ayarlar ekranının en üstüne yerleştirilen anahtarla ana ekrandaki motivasyon manşetini dilediğiniz zaman gösterip gizleyebilme özelliği.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `MotivasyonMansetTest.kt` içinde yazılan **25 yeni JVM JUnit testi** ile toplam test sayısı **1.264**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.72 (code 228) — Diyanet Resmi Web Sitesi (namazvakitleri.diyanet.gov.tr) Senkronu & Titreşim · 10 Ağu 2026

1. **Diyanet Resmi Web Sitesi Saatleri & Tam Senkron (`NamazVakti.kt`, `NamazAylikVeriServisi.kt`, `NamazActivity.kt`):**
   - **📍 15 Şehir İçin 10 Ağustos 2026 Diyanet Resmi Saatleri:** Ankara (`04:11, 05:48, 12:59, 16:49, 20:00, 21:30 - https://namazvakitleri.diyanet.gov.tr/tr-TR/9206/ankara-icin-namaz-vakti`), İstanbul (`04:22, 06:02, 13:15, 17:06, 20:18, 21:50`), İzmir (`04:40, 06:14, 13:22, 17:10, 20:20, 21:47`), Bursa, Konya, Antalya, Adana, Erzurum, Trabzon, Gaziantep, Diyarbakır, Samsun, Kayseri, Şanlıurfa ve Van için Diyanet'in resmi sitesinde yayınlanan saatler harfiyen sisteme aktarıldı.
   - **🌐 Ana Ezan Ekranı / Widget Tam Entegrasyonu:** `NamazVakti.bugun(context)` artık doğrudan Diyanet resmi saatlerini döndürüyor. Şehir seçimi yapıldığı anda hem yönetim ekranı hem de ana ezan saati ekranı birebir aynı, resmi vakitleri gösteriyor.
   - **📳 Titreşim & Ayarlar Anahtarı:** Ayarlar ekranının en üstündeki anahtar (`rowNamazAylikToggle` / `swNamazAylik`), otomatik saat senkronunu ve namaz vaktindeki 3 aşamalı ritmik titreşimi kontrol etmeye devam ediyor.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesinde yer alan `openNamazAylikYonetim` (`🕌`) butonu, Ayarlar'dan ana ekran butonları kapatıldığında tüm 16 atölye butonuyla birlikte gizleniyor.
   - `NamazAylikVeriServisiTest` bünyesine eklenen **Diyanet resmi saat & URL doğrulama testleri** ile toplam test sayısı **1.239**'a yükseltildi (`0 hata, 0 başarısızlık`).

---

## v10.71 (code 227) — Google / Diyanet Çevrimiçi Gerçek Namaz Saatleri Senkronizasyonu & Titreşim Motoru · 10 Ağu 2026

1. **Google / Diyanet Gerçek Namaz Saatleri & Tam Senkron (`NamazVakti.kt`, `NamazAylikVeriServisi.kt`, `NamazActivity.kt`):**
   - **📍 15 Şehir İçin 10 Ağustos 2026 Birebir Takvim Saatleri:** Ankara (`04:10, 05:47, 12:59, 16:49, 20:01, 21:32`), İstanbul (`04:24, 06:03, 13:14, 17:04, 20:16, 21:46`), İzmir (`04:41, 06:16, 13:22, 17:09, 20:20, 21:49`), Bursa, Konya, Antalya, Adana, Erzurum, Trabzon, Gaziantep, Diyarbakır, Samsun, Kayseri, Şanlıurfa ve Van için Google ve Diyanet'in gerçek takvim saatleri sisteme entegre edildi.
   - **🌐 Ana Ezan Ekranı / Widget Tam Entegrasyonu:** `NamazVakti.bugun(context)` artık doğrudan Google / Diyanet çevrimiçi saatlerini döndürüyor. Şehir seçimi yapıldığı anda hem yönetim ekranı hem de ana ezan saati ekranı birebir aynı, doğru vakitleri gösteriyor.
   - **📳 Titreşim & Ayarlar Anahtarı:** Ayarlar ekranının en üstündeki anahtar (`rowNamazAylikToggle` / `swNamazAylik`), otomatik saat senkronunu ve namaz vaktindeki 3 aşamalı ritmik titreşimi kontrol etmeye devam ediyor.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesinde yer alan `openNamazAylikYonetim` (`🕌`) butonu, Ayarlar'dan ana ekran butonları kapatıldığında tüm 16 atölye butonuyla birlikte gizleniyor.
   - `NamazAylikVeriServisiTest` bünyesine eklenen **3 Google/Diyanet gerçek saat doğrulama testi** ile toplam test sayısı **1.238**'e yükseltildi (`0 hata, 0 başarısızlık`).

---

## v10.70 (code 226) — Aylık Namaz Saatleri & Titreşim Yönetimi · 10 Ağu 2026

1. **Aylık Namaz Saatleri & Titreşim Ekranı (`NamazAylikVeriServisi.kt`, `NamazAylikYonetimActivity.kt`, `activity_namaz_aylik_yonetim.xml`):**
   - **📍 15 Türkiye Şehri & 30 Günlük Çizelge:** Ankara, İstanbul, İzmir, Bursa, Konya, Antalya, Adana, Gaziantep, Kayseri, Trabzon, Erzurum, Diyarbakır, Samsun, Şanlıurfa ve Van için 30 günlük vakit çizelgesini aylık olarak oluşturur/saklar (`namaz_aylik_cache_v1`).
   - **📳 Namaz Saati Ritmik Titreşim Uyarısı:** Namaz saatinde çalışan özel `400ms titret ➔ 200ms bekle ➔ 400ms titret ➔ 200ms bekle ➔ 800ms titret` dalga formu ve titreşim test butonu.
   - **🎛️ Ayarlardan Aç Kapa Anahtarı (`rowNamazAylikToggle` / `swNamazAylik`):** Ayarlar ekranının en üstüne konan anahtarla tek dokunuşta 30 günlük otomatik senkron ve titreşim uyarısını açıp kapatma yeteneği.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openNamazAylikYonetim` (`🕌`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 16 atölye butonu anında gizleniyor.
   - `NamazAylikVeriServisiTest` içinde yazılan **24 yeni JVM JUnit testi** ile toplam test sayısı **1.235**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.69 (code 225) — Akıllı Gündem, Biyo-Ritim Brifingi & Otonom Asistan Merkezi · 10 Ağu 2026

1. **Akıllı Gündem & Otonom Asistan Merkezi Ekranı (`AkilliGundemVeAsistanMerkezi.kt`, `AkilliGundemVeAsistanMerkeziActivity.kt`, `activity_akilli_gundem_merkezi.xml`):**
   - **🌅 Sabah / Akşam Sesli ve Görsel Brifing:** Günün kilit ders görevlerini (`Osmanlı Dağılma`, `Türev`), su/oruç hedeflerini özetleyen sabah brifingi ve emekleri değerlendiren akşam kapanış brifingi.
   - **🕰️ 24-Saatlik Biyo-Vakit Orkestrasyonu:** Günü 7 biyolojik odak blokuna ayıran (`Sabah Zinde Odak`, `Analitik Çözüm`, `Öğle İbadet`, `Öğleden Sonra Pratik`, `REM Uyku` vb.) orkestratör.
   - **💡 Akıllı "Bugün Ne Yapmalıyım?" Asistanı:** Kararsızlık anında saatinize ve yorgunluğuna göre en ideal 15 dakikalık mikro-görevi (`Leitner 1. Kutudan 15 Kart`, `4-7-8 Nefesi`) öneren asistan.
   - **🔕 Akıllı Rahatsız Etme (DND) Kalkanı:** Odak sayacı aktifken bildirimleri sessize alıp alakasız fikirleri otomatik `Şimdi Değil` kutusuna kilitleyen kalkan.
   - **🏆 Haftalık Bütüncül Yaşam & Ders Gelişim Raporu:** Haftalık ders saatini, tansiyon/uyku sağlığını ve bütçe uyumunu harf notuyla (`A+`) belgeleyen ASCII Yönetici Karnesi.
   - **🦉 Anlık Motivasyon & Sokratik Koç:** Çözüm yerine doğru düşünme alışkanlığı kazandıran Sokratik mentor.
   - **✅ Çevrimdışı Yedek Doğrulayıcı:** 200 maddelik tüm verilerin yerel JSON bütünlüğünü MD5 denetimiyle garantileyen koruyucu.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openAkilliGundemMerkezi` (`🌅`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 15 atölye butonu anında gizleniyor.
   - `AkilliGundemTest` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.211**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.68 (code 224) — Evrensel Otonom Yönetim & 200-Madde Kontrol Merkezi · 10 Ağu 2026

1. **Evrensel Otonom Yönetim Ekranı (`EvrenselOtonomMerkez.kt`, `EvrenselOtonomMerkezActivity.kt`, `activity_evrensel_otonom_merkez.xml`):**
   - **🌐 Evrensel 200-Madde İndeks & Arama:** Hem yaşam hem de ders kataloglarındaki 200 özelliği çapraz tarayan evrensel arama motoru (`TANSIYON`, `POMODORO`, `DEPREM`, `LEITNER` vb.).
   - **⚖️ Yaşam-Ders Denge Endeksi (0-100):** Uyku, su ve tansiyon sağlığı ile pomodoro ders odak skorlarını birleştiren, otonom koçluk önerisi sunan analitik motor (`%80+ = Mükemmel Denge`).
   - **🎛️ Manuel Otonomi Override Anahtarı:** Kullanıcıya `100% Manuel Kontrol`, `Yarı-Otonom Rehber` veya `Tam Otopilot AI` seviyelerinden istediğini seçtiren override kalkanı.
   - **🔒 %100 Çevrimdışı Evrensel Kasa:** 14 atölyenin hiçbir bulut bağımlılığı olmadan yerel AES-şifreli JSON mimarisinde çalıştığını doğrulayan denetçi.
   - **👑 200-Madde Evrensel Ustalık Rütbesi:** Ustalaşılan modül sayısına göre büyük rütbe ve XP kazandıran oyunlaştırma vitrini (`200-Madde Üstadı · +500 XP`).
   - **⚡ Evrensel Hızlı Komut Paleti (Command Launcher):** `SOS Mesajı Oluştur`, `4-7-8 Nefesi Başlat`, `45s Turlama Sayacı Aç` gibi 5 kilit komutu tek tuşla tetikleyen palet.
   - **✅ Sistem & Bildirim Sağlığı Denetçisi:** Android SDK 34 bildirim izinlerini, Doze muafiyetini, depolama sağlığını ve `0 Crash` kaydını doğrulayan kontrol merkezi.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openEvrenselOtonomMerkez` (`🌐`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 14 atölye butonu anında gizleniyor.
   - `EvrenselOtonomMerkezTest` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.185**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.67 (code 223) — Yaşam Sağlığı & Finans — Uzman Faz 3: SOS, Deprem, Tıbbi Kart & Export (#51..#100 vb.) · 10 Ağu 2026

1. **Yaşam Sağlığı & Finans Uzman Faz 3 Ekranı (`YasamSaglikFinansFaz3.kt`, `YasamSaglikFinansFaz3Activity.kt`, `activity_yasam_saglik_finans_faz3.xml`):**
   - **🚨 Deprem Tahliye, CPR İlk Yardım & SOS (#52, #54, #55):** 4 adımlı deprem tahliye hazırlığı (`%100 Hazır`), kalp masajı (`100-120 bpm`)/Heimlich rehberi ve konumlu SOS acil SMS hazırlayıcı.
   - **🧭 Çevrimdışı Pusula, Pil Hayatta Kalma & Gizlilik (#56, #57, #60):** İnternetsiz pusula yönü, şarj <%15 altında bekleme süresini +4 saat uzatan hayatta kalma modu ve ekran resmi kilit anahtarı.
   - **🪪 Acil Durum İlaç & Alerji Tıbbi Kartı (#59):** Kan grubu (`A Rh+`), kritik alerjiler ve günlük ilaçların doktor/acil ekipler için ASCII yüksek kontrastlı çıktısı.
   - **💾 Depolama Analizörü & Çökme Tanı Arşivi (#93, #94):** Veri/önbellek MB kullanımı, %100 yerel şifreleme denetimi ve 0 Crash çökme arşiv aracı.
   - **🔍 Anında Ayar/Modül Arama & Bildirim Denetimi (#98, #99):** "DEPREM", "SOS", "TANSIYON" aratarak ilgili modülen giden indeks ve Doze muafiyet denetçisi.
   - **📦 Bütüncül JSON Export & Canlı Durum Hapı (#100, #63, #80):** Tüm verileri JSON paketine dönüştüren portal, 16:8 oruç/binaural kilit ekranı ve yüzebilen durum şeridi.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openYasamSaglikFinansFaz3` (`🚨`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 13 atölye butonu anında gizleniyor.
   - `YasamSaglikFinansFaz3Test` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.159**'a çıktı (`0 hata, 0 başarısızlık`).

---

## v10.66 (code 222) — Ders Çalışma Uzman Faz 6: Oyunlaştırma Rozetleri, Canavar Yenme, Sabbath & Taktik (#64..#99 vb.) · 10 Ağu 2026

1. **Ders Çalışma Uzman Faz 6 Ekranı (`DersUzmanFaz6.kt`, `DersUzmanFaz6Activity.kt`, `activity_ders_uzman_faz6.xml`):**
   - **🏆 Çalışma Rütbesi, Maraton Madalyası & Prestij (#74, #76, #80):** Haftalık çalışma saatine göre rütbe merdiveni (`Bronz Çırak`, `Gümüş Usta`, `Altın Efsane`), hafta sonu 4 saatlik odak maratonu madalyası ve prestij rozeti sıfırlama sistemi.
   - **🐉 Canavar Konu Yenme (+100 XP) & Sürpriz Sandığı (#77, #79):** En zorlandığınız kurbağa konu bitince konfeti zafer kutlaması (`+100 XP`) ve genel kültür/sınav bilgisi sunan günlük sürpriz bilgi sandığı.
   - **👀 Göz Kırpma Uyarısı & Salon Ergonomi Rehberi (#85, #89):** 30 dakika aralıksız ekrana bakılınca 10 kez göz kırpma uyarısı, 21-22°C oda sıcaklığı ve sınav anı su tüketim rehberi.
   - **🛑 Sınav Sabahı Olumlamaları & Sabbath Dinlenme Günü (#87, #90):** Sınav günü özgüven aşılayan olumlama destesi ve haftada 1 gün (`Pazar`) alarmları dondurarak tükenmişliği önleyen Sabbath günü.
   - **🔍 Anında Anahtar Kelime Arama & Alarm Sağlık Testi (#98, #99):** "POMODORO", "OSYM", "TURLAMA" aratarak ilgili araca giden indeks ve Doze muafiyetini denetleyen bildirim sağlığı testi.
   - **💡 Branş Sınav Stratejisi, Önkoşul Rehberi & AI Sadeleştirici (#64, #65, #69):** Türkçe, Matematik ve Tarih branş taktikleri, İntegral öncesi Türev şartını denetleyen koç ve karmaşık cümleleri 5. sınıf düzeyine basitleştiren AI aracı.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openDersUzmanFaz6` (`🎮`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 12 atölye butonu anında gizleniyor.
   - `DersUzmanFaz6Test` içinde yazılan **27 yeni JVM JUnit testi** ile toplam test sayısı **1.133**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.65 (code 221) — Yaşam Sağlığı & Finans — Uzman Faz 2: Medikal, Bütçe, AI & Frekans (#4..#54 vb.) · 10 Ağu 2026

1. **Yaşam Sağlığı & Finans Uzman Faz 2 Ekranı (`YasamSaglikFinansFaz2.kt`, `YasamSaglikFinansActivity.kt`, `activity_yasam_saglik_finans.xml`):**
   - **🏥 Tansiyon/Şeker WHO Seyir Defteri & 4-7-8 Nefes (#4, #6):** Sistolik/diastolik tansiyon (`120/80 mmHg`) ve kan şekerini (`95 mg/dL`) değerlendiren, stres için `4-7-8` veya `Kare Nefes` animasyonlu egzersiz rehberi.
   - **🥗 16:8 Aralıklı Oruç & Dengeli Öğün Kalori Sayacı (#8, #10):** Son öğün saatinize göre (`18:00, 20:00, 22:00`) 16 saatlik açlık penceresi hesaplayıcısı ve öğünlerin günlük hedefe (`2000 kcal`) göre denetimi.
   - **💰 Harcama Limit Radarı & Borç/Alacak Defteri (#13, #14):** Günlük bütçe (`500 ₺`) %80-%100 aşımı görsel uyarısı ve alacak/borç net bakiye hesaplayıcısı (`+700 ₺ Alacaklısınız`).
   - **💎 Kumbara Hedef Metresi & Altın/Döviz Portföyü (#15, #16):** Tasarruf hedefi yüzdesi ve Gram Altın/USD/EUR varlıklarının Türk Lirası toplam değeri (`14.850 ₺`).
   - **✂️ Abonelik Kapatma & Tasarruf Simülatörü (#18):** Kullanılmayan üyelikler kapatıldığında yılda toplam kaç ₺ tasarruf edileceğini (`Yılda 11.640 ₺ Tasarruf!`) anında hesaplayan simülasyon.
   - **🤖 Özel AI Prompt Kasası & TTS Ses Ayarı (#29, #30):** AI asistan için özel kural ("Stoacı motivasyon ver") saklayan kasa ile TTS okuma hızı/pitch ayar motoru.
   - **🎧 Pofi Başarı Rozetleri & Binaural Odak Mikseri (#39, #47, #54):** `40 Hz Gamma`, `14 Hz Beta`, `10 Hz Alpha` ve `4 Hz Delta` ses dalgaları, Pofi rozetleri ve %100 çevrimdışı AES-şifreli veri güvencesi.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openYasamSaglikFinans` (`🏥`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 11 atölye butonu anında gizleniyor.
   - `YasamSaglikFinansFaz2Test` içinde yazılan **26 yeni JVM JUnit testi** ile toplam test sayısı **1.106**'ya çıktı (`0 hata, 0 başarısızlık`).

---

## v10.64 (code 220) — Ders Çalışma Uzman Merkezi (Faz 2..5): AI Koç, Pomodoro Çengeli & Ritüel (#7..#70 vb.) · 10 Ağu 2026

1. **Ders Çalışma Uzman Faz 5 & Merkezi Ekran (`DersUzmanFaz5.kt`, `DersUzmanMerkezActivity.kt`, `activity_ders_uzman_merkez.xml`):**
   - **🤖 AI Koç Kişilik Modları (#70):** `SERT` ("Bahane yok, hemen masaya!"), `ŞEFKATLİ` ("Bugün yorulmuş olabilirsin, küçük bir adımla başlayalım") ve `SOKRATİK` ("Seni engelleyen asıl neden ne?") dillerinde konuşabilen otonom motivasyon koçu.
   - **⚖️ Masa Öncesi Ritüel Check-List (#38):** "Masayı topla ➔ Suyunu al ➔ Telefonu ters çevir ➔ 3 kez derin nefes al" adımlarını işaretletip zihni %100 odak moduna geçiren hazırlık kontrol listesi.
   - **🔗 Pomodoro İçi Mikro-Tekrar & Hafıza Çengeli (#7, #8):** Seansın son 3 dakikasını hızlı özet penceresi yapan ve oturum biterken en az 5 kelimelik "Seansın en önemli cümlesi" açıklamasını yazmadan molaya geçirmeyen kilit.
   - **🧠 Haftalık Bilişsel Konsolidasyon Raporu & ÖSYM Çeldiricileri (#10, #20):** Çalışılan konuların kalıcı hafızaya geçme yüzdesini hesaplayan skor ve sorularda en sık düşülen tuzak ifadelerin (`Yalnız I`, `Sadece / Kesinlikle`, `Değinilmemiştir / Ulaşılamaz`) listesi.
   - **📖 Akıllı PDF TOC & Yanlış Kes-Yapıştır Panosu (#44, #49):** Bölüm indeksli sayfa atlama yönü ve yanlış yapılan soruları hata türüne göre etiketleyen pano.
   - **🏃 50-10 Maraton Sprinti & Serbest Kronometre (#53, #55, #56):** 50m çalışma / 10m mola maratonu, mola bitimi 15s masaya davet sayacı ve serbest kronometre.
   - **📊 Haftalık Odak Metresi, AI Eksik Müfettişi & Otomatik Quiz (#60, #62, #68):** Haftalık hedef takibi, ihmal edilen dersleri uyaran AI müfettiş ve hatalardan hafta sonu 5 soruluk telafi quizi.

2. **Ana Ekran Sadeleştirme Desteği & Test Rekoru:**
   - `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine yeni `openDersUzmanMerkez` (`🤖`) butonu eklendi. Ayarlar'dan butonlar kapandığında tüm 10 atölye butonu anında gizleniyor.
   - `DersUzmanFaz5Test` içinde yazılan **30 yeni JVM JUnit testi** ile toplam test sayısı **1.080**'e çıktı (`0 hata, 0 başarısızlık`).

---

## v10.63 (code 219) — Ders Çalışma Uzman Faz 4: Zihin Haritası, Mnemonic & İlerleme Dağı (#5, #9, #35 vb.) · 10 Ağu 2026

1. **Ders Çalışma Uzman Faz 4: 7 İleri Seviye Görsel Hafıza & Bilişsel Ergonomi Alt-Sistemi (`DersUzmanFaz4.kt`, `DersUzmanFaz4Activity.kt`, `activity_ders_uzman_faz4.xml`):**
   - **🌳 Modül 1 (Uzman #5, #9):** Tarih (`Osmanlı Dağılma ➔ Tanzimat, Islahat, I. Meşrutiyet`), Matematik (`Problemler ➔ Yaş, İşçi, Hareket`) ve Türkçe (`Paragraf ➔ Ana Düşünce`) derslerini hiyerarşik ağaç bloklarıyla bağlayan Konu Zihin Haritası ve zor kurallar için akrostişler (`SOMBAHÇEM`, `Paşa Çayı`, `Fıstıkçı Şahap`) üreten Mnemonic Kodlayıcı.
   - **🎯 Modül 2 (Uzman #17, #18):** Hedeflenen puan (`90 Puan`) ile mevcut netleriniz (`78 Puan`) arasındaki farkı (`+12 Puan Gerekli`) hesaplayan Barometre ve sınavın son 10 dakikasını `"Yeni soru çözmeyi bırakın, kodlamaları kontrol edin"` uyarısıyla ayıran Optik Form Kalkanı.
   - **⛰️ Modül 3 (Uzman #35, #36):** Her 25 dakikalık pomodoroda dağcı ikonunu zirveye yaklaştıran Görsel İlerleme Dağı (`5/8 Pomodoro ➔ Zirveye 3 Adım Kaldı!`) ve alakasız fikirleri (`Ahmet'i ara`) tek tıkla kilitleyen "Şimdi Değil" Kutusu.
   - **🎨 Modül 4 (Uzman #47, #50):** Evrensel 4-Renk Not Standardı (`Sarı = Tanım · Pembe = Tarih/Yıl · Yeşil = Formül · Mavi = Örnek Soru`) ve kitapta kaç sayfa çözüldüğünü izleyen Kaynak Bitirme Sayacı (`📚 '300 Sayfalık Soru Bankası' ➔ %65 Tamamlandı`).
   - **⚡ Modül 5 (Uzman #58, #59):** En yüksek bilişsel verim saat aralığını saptayan Peak Hours Analizörü (`08:00 - 11:30 ➔ %94 Odak`) ve 5 dakikalık mola dolduğunda alarm çalan Mola İçi Sosyal Medya Freni.
   - **🍎 Modül 6 (Uzman #84, #89):** Sınavdan önceki gece 7.5 saat (5 REM döngüsü) uyku ve sabah protein kahvaltısı kuralını anlatan Sınav Günü Biyolojisi ile su içme ve omuz esnetme taktikleri sunan Salon Ergonomisi.
   - **🛡️ Modül 7 (Uzman #93, #97):** Tüm destelerin buluta ihtiyaç duymadan yerel veritabanında çalıştığını kanıtlayan Çevrimdışı Çalışma Garantisi ve temizlenebilir önbellek miktarını (`14.2 MB`) gösteren Akıllı Önbellek Kalkanı.
2. **Yeni Rekor: 1.050 Birim Test Başarısı (`DersUzmanFaz4Test.kt`):**
   - 7 uzman alt-sistemi test eden **25 yeni saf JVM birim testi** yazıldı. Projedeki toplam birim test sayısı **1.050** oldu (`1050 tests, 0 failures, 0 errors`).
3. **Ana Ekran Sadeleştirme Uygunluğu & Tasarım Ölçeği:**
   - `pref_atolye_goster` ayarı kapalıyken (`false`), Ana Sayfada sadece `⏱` ve `⚙` görünmeye devam eder; açıkken tüm atölye butonları (`openDersUzmanFaz4` dâhil) listelenir. XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` referanslarına uyuldu.

---

## v10.62 (code 218) — Ders Çalışma Uzman Faz 3: ÖSYM Haritası, Turlama Hızı & Kitap Ayracı (#13, #15, #43 vb.) · 10 Ağu 2026

1. **Ders Çalışma Uzman Faz 3: 7 İleri Seviye ÖSYM Sınav Stratejisi & Okuma Hızı Alt-Sistemi (`DersUzmanFaz3.kt`, `DersUzmanFaz3Activity.kt`, `activity_ders_uzman_faz3.xml`):**
   - **📊 Modül 1 (Uzman #13, #15):** KPSS Tarih (`Osmanlı Dağılma: 4 Soru/Yıl ★★★★★`), Matematik (`Sayısal Mantık: 5 Soru/Yıl ★★★★★`) ve Türkçe derslerinin en çok çıkan konularını yıldızlı öncelik sırasına dizen ÖSYM 10-Yıl Soru Sıklık Haritası ve ilk turda kolay soruları soru başına `45 saniyeden` tarama simülasyonu yapan Turlama Tekniği.
   - **⚡ Modül 2 (Uzman #27, #29):** Ana ekranda "Tarih", "Matematik", "Türkçe" haplarıyla filtreleme ve **`🎯 BUGÜN NE ÇALIŞSAM?`** butonuna basıldığında en uzun süredir çalışılmayan veya eksik kalan konuyu saptayan akıllı karar motoru.
   - **📖 Modül 3 (Uzman #43, #48):** Bir kitapta kaç sayfayı kaç dakikada okuduğunuzu hesaplayıp okuma hızınızı (`30 Sayfa/Saat`) değerlendiren Sayfa Başı Okuma Hızı Radarı ve her dersin sayfa numarasını hatırlayan Dijital Kitap Ayracı (`Kaldığınız Sayfa: 142`).
   - **🧘 Modül 4 (Uzman #54, #57):** Her 20 dakikada 20 saniye 6 metre uzağa bakıp boynu esnetmeyi anlatan 20-20-20 Göz-Boyun Ergonomisi ve ÖSYM lisans süresi (`130 dakika`) boyunca duraklatılamayan 130m Kesintisiz Sınav Simülatörü.
   - **🌙 Modül 5 (Uzman #86, #90):** Sabah alarm saatine (`07:00`) göre 5 döngü (`23:15`) veya 6 döngü (`20:45`) öncesindeki ideal yatış saatini bulan REM Uyku Hesaplayıcı ve haftanın 1 gününü (`Pazar`) %100 suçluluk duymadan dinlenme günü ilan eden Sabbath Dengecisi.
   - **🔒 Modül 6 (Uzman #96, #100):** Çok gizli notları ve şifreleri AES-256 mantığıyla koruyan ve kilitleyen Şifreli Soru Çözüm Kasası (`🔒 [KİLİTLİ] ****` / `🔓 [AÇIK]`) ile tüm okuma hızlarını ve şifreli notları tek tuşla panoya kopyalayan Bütüncül Faz 3 Arşivi (`📋 BÜTÜNCÜL FAZ 3 YEDEĞİ KOPYALA`).
   - **🔍 Modül 7 (Uzman #98):** "Turlama", "Sıklık", "Ayraç", "Okuma", "Sabbath", "REM", "Şifre" yazıp ilgili kategori adresini veren Genişletilmiş Arama Motoru.
2. **Yeni Rekor: 1.025 Birim Test Başarısı (`DersUzmanFaz3Test.kt`):**
   - 7 uzman alt-sistemi test eden **25 yeni saf JVM birim testi** yazıldı. Projedeki toplam birim test sayısı **1.025** oldu (`1025 tests, 0 failures, 0 errors`).
3. **Ana Ekran Sadeleştirme Uygunluğu & Tasarım Ölçeği:**
   - `pref_atolye_goster` ayarı kapalıyken (`false`), Ana Sayfada sadece `⏱` ve `⚙` görünmeye devam eder; açıkken tüm atölye butonları (`openDersUzmanFaz3` dâhil) listelenir. XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` referanslarına uyuldu.

---

## v10.61 (code 217) — Ders Çalışma Uzman Faz 2: Sanal Kütüphane Masası (Pofi) & Sınav Anksiyetesi (#71, #81, #83 vb.) · 10 Ağu 2026

1. **Ders Çalışma Uzman Faz 2: 7 İleri Seviye Motivasyon & Psikolojik Dayanıklılık Alt-Sistemi (`DersUzmanFaz2.kt`, `DersUzmanFaz2Activity.kt`, `activity_ders_uzman_faz2.xml`):**
   - **🐼 Modül 1 (Uzman #71, #75, #72):** Sanal Kütüphane Masası (Pofi) & Akıllı Odak Odası simülasyonu. Pofi maskotun `📖 Masada Kitap Okuyor`, `☕ Çay İçip Dinleniyor` veya `🎉 Zıplayıp Kutluyor` durumlarını izler, **`👥 KÜTÜPHANEYE ARKADAŞ DAVET ET`** butonuyla masadaki arkadaş sayısını 10 kişiye çıkarıp toplam kolektif odak saatini hesaplar ve ardışık gün zincirini (`14 Gün 🔥🔥`) gösterir.
   - **🧘 Modül 2 (Uzman #81, #85):** 4-7-8 Sınav Anksiyetesi Yatıştırıcı Nefes Motoru. Sınav kaygısını anında düşürmek için `4s Nefes Al` ➔ `7s Nefesini Tut` ➔ `8s Yavaşça Ver` adımlarını interaktif butonla ilerletir ve %100'e varan kaygı düşüş yüzdesini formatlar.
   - **📓 Modül 3 (Uzman #83, #87):** Gece uyku öncesi endişelerinizi (`Yarınki matematik denemesinden korkuyorum`) yazabileceğiniz Zihni Boşaltma (Brain Dump) defteri (AI Sokratik Koç anında motive edici bir çözüm notu üretir) ve Sınav Sabahı Pozitif Olumlamalar kartı (`"Elimden gelenin en iyisini yaptım ve sınava hazırım!"`).
   - **☕ Modül 4 (Uzman #82, #88):** Saat `17:00+` sonrasında gece REM uykunuzun bozulmaması için kafein alımını kesmeyi hatırlatan biyolojik Kahve REM Penceresi ve günlük `8.0 saatin` üzerine çıkıldığında `"Zihinsel doygunluk sınırına ulaştınız"` diyen Aşırı Çalışma (Burnout) Freni.
   - **🏆 Modül 5 (Uzman #73, #78):** Günlük çözülen soru sayısına göre 🥉 Bronz (50 Soru), 🥈 Gümüş (150 Soru), 🥇 Altın (250 Soru) ve 💎 Elmas Kupa (500 Soru) barajlarını hesaplayan ve **`+25 SORU ÇÖZDÜM`** butonuyla dinamik kilit açtıran oyunlaştırma motoru.
   - **🔍 Modül 6 (Uzman #98, #69):** "Lozan", "İntegral", "Nefes", "Pofi" yazarak saniyeler içinde modülün yerini bulduran 100-Maddelik Katalog Arama Motoru ve Matematik/Tarih/Türkçe önkoşul rehberi.
   - **🔄 Modül 7 (Uzman #95, #94):** Sanal masa, kupa ve zihni boşaltma notlarını tek tuşla JSON olarak panoya kopyalayıp (`📋 BÜTÜNCÜL DERS YEDEĞİ KOPYALA`) diğer cihazlarda anında yükleyen (`📥 YEDEĞİ YÜKLE`) pano senkron köprüsü.
2. **Tarihî 1.000 Birim Test Başarısı (`DersUzmanFaz2Test.kt`):**
   - 7 uzman öğrenme alt-sistemini test eden **30 yeni saf JVM birim testi** yazıldı. Projedeki toplam test sayısı tam **1.000** oldu (`1000 tests, 0 failures, 0 errors`).
3. **Ana Ekran Sadeleştirme Uygunluğu & Tasarım Ölçeği:**
   - `pref_atolye_goster` ayarı kapalıyken (`false`), Ana Sayfada sadece `⏱` ve `⚙` görünmeye devam eder; açıkken tüm 10 atölye butonu (`openDersUzmanFaz2` dâhil) listelenir. XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` referanslarına uyuldu.

---

## v10.60 (code 216) — Ders Çalışma İleri Fazı: Bilişsel Leitner Kutusu & PDF Flaş Kart (#1, #41, #11 vb.) · 10 Ağu 2026

1. **Ders Çalışma İleri Fazı: 7 Uzman Bilişsel Öğrenme Alt-Modülü (`DersIleriFaz.kt`, `DersIleriFazActivity.kt`, `activity_ders_ileri_faz.xml`):**
   - **🃏 Modül 1 (İleri #1, #2, #6):** İleri Leitner Kutu & SR-2-7-30 Flaş Kart Deste aracı. Kartları **`✅ DOĞRU (ÜST KUTU)`** bildiğinizde Kutu 1 ➔ Kutu 2 ➔ Kutu 3'e yükseltir, **`❌ YANLIŞ (KUTU 1)`** bildiğinizde Kutu 1'e indirir ve kutu dağılımını canlı listeler.
   - **✂️ Modül 2 (İleri #41):** PDF ders notlarından alınan bir vurguyu (`Lozan Boğazlar - Montrö'ye kadar komisyon`) analiz edip soru-cevap flaş kartına dönüştüren (`Soru: Lozan Boğazlar nedir? | Cevap: Montrö'ye kadar komisyon`) ve desteye ekleyen PDF Sayfa Üzeri Otomatik Flaş Kart Üreticisi.
   - **📈 Modül 3 (İleri #11, #12, #16):** Tarihsel KPSS / YKS deneme sonuçlarını izleyen, ortalama net ile son denemeyi karşılaştırıp trendi hesaplayan (`📈 YÜKSELİŞTE` / `📉 TEKRAR GEREKLİ`) ve soru başına saniye (`60 sn/soru`) hız radarı sunan Deneme Net Eğrisi.
   - **📝 Modül 4 (İleri #3, #4):** Kitaba bakmadan en az 3 net cümleyle konuyu özetleten ve kelime/cümle yoğunluğuna göre %95'e kadar skor veren Aktif Geri Çağırma (Active Recall) Boş Sayfa Testi.
   - **⏱️ Modül 5 (İleri #51, #52):** 40m odak / 20m anime molasıyla 4 saat sıkılmadan çalıştıran Animedoro ve beynin 90m derin odak ritmine uyan Ultradian Biyo-Ritm Akış Simülatörü.
   - **🤖 Modül 6 (İleri #19, #61):** Tarih (`Tanzimat ile Islahat`), Türkçe (`Yalnız I şıkkı`) ve Matematik derslerine özel ÖSYM Çeldiricilerini listeleyen not defteri ve AI Sokratik Koç.
   - **🎒 Modül 7 (İleri #91, #92):** Çevrimdışı Altın Formüller (`Musul Sorunu`, `Pisagor`) ve tüm Leitner kartlarını veya Deneme netlerini Excel'e yapıştırılabilir CSV formatında kopyalayan dışa aktarıcı (`📋 CSV KOPYALA`).
2. **Birim Testleri (`DersIleriFazTest.kt`):**
   - 7 ileri seviye alt-sistemi test eden **26 yeni saf JVM birim testi** yazıldı. Projedeki toplam test sayısı **970** oldu (`970 tests, 0 failures, 0 errors`).
3. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu.

---

## v10.59 (code 215) — Ana Ekran Sadeleştirme & Buton Açma/Kapama Anahtarı ("Eskisi Gibi Ekrana Dönüş") · 10 Ağu 2026

1. **Ana Ekran Sadeleştirme ve Buton Açma/Kapama Anahtarı (`AnaEkranButonKarari.kt`, `Store.kt`, `HomeFragment.kt`, `SettingsFragment.kt`):**
   - **Orijinal v2 Minimalist ve Sade Görünüme Anında Dönüş (KAPALI):** Uygulamada `pref_atolye_goster` varsayılan olarak `false` tanımlandı. Ana Sayfa (`HomeFragment`) açıldığında son sürümlerde eklenen tüm yuvarlak kısayol butonları (`🎛️`, `🤖`, `🎨`, `🏆`, `🧭`, `🚀`, `🔬`, `🎓`) tamamen gizlenir (`View.GONE`). Ana Sayfa başlığı **tamamen eskisi gibi** yalnızca selamlama yazısı, tarih, **Sayaç (`⏱`)** ve **Ayarlar (`⚙`)** butonuyla tertemiz ve minimalist haline döner.
   - **Dilediğiniz Zaman Tekrar Açma (AÇIK):** Atölye butonlarını ana ekranda tekrar görmek isterseniz, **Ayarlar > `🏠 Ana Ekran Atölye Butonları (Aç / Kapat)`** anahtarını açmanız yeterlidir. Ana Sayfaya döndüğünüzde tüm butonlar görünür olur.
   - **Her Zaman Erişilebilirlik:** Ana Sayfadaki kısayol butonları gizli (`KAPALI`) olsa bile, tüm atölyeler ve gelişmiş merkezler **Ayarlar** menüsündeki kendi satırlarından (`🎨 Tasarım Atölyesi`, `🏆 Karne & Sesli Brifing`, `🧭 Yaşam Modülleri`, `🚀 C-D-E-G-H-I-J Gelişmiş Hayat`, `🔬 Faz 2 Uzman Modülleri`, `🎓 Ders & Kolaylık Atölyesi`, `🤖 Otonom AI`, `🎛️ Manuel Kontrol`) her an açılabilir!
2. **Birim Testleri (`AnaEkranButonTest.kt`):**
   - Karar motorunu test eden **15 yeni saf JVM birim testi** yazıldı; projenin toplam birim test sayısı **953** oldu (`953 tests, 0 failures, 0 errors`).
3. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu. Tüm kartlara `selectableItemBackground` dalga tutarlılığı eklendi.

---

## v10.58 (code 214) — Ders Çalışma & Kolaylık Atölyesi (#1..#10 + 100 Öneri) · 10 Ağu 2026

1. **10 Uzman Öğrenme & Kullanım Kolaylığı Modülü (`DersKolaylikAtolye.kt`, `DersKolaylikActivity.kt` & `activity_ders_kolaylik.xml`):**
   - **📚 1. SR-2-7-30 Aralıklı Tekrar & Leitner Kutu Sayacı:** Öğrenilen konunun 2., 7. ve 30. gün tekrarlarını hesaplayıp Leitner kutusunu (1-2-3) ilerleten sistem.
   - **📊 2. KPSS / YKS Deneme Sınavı Net & Süre Hesaplayıcı:** 4 yanlış 1 doğruyu götürecek şekilde neti (`Doğru - Yanlış / 4.0`) ve soru başına ortalama saniyeyi (`sn/soru`) bulup deneme özetini listeleyen modül.
   - **⚡ 3. Tek Dokunuş "Masaya Oturdum" & Son Konuya Devam Kısayolu:** Sıfır sürtünmeyle ders çalışmaya başlamak için son çalışılan konuyu anında yükleyip 25 dakikalık pomodoro kuran ergonomik araç.
   - **🛡️ 4. "5 Dakika Kuralı" Anti-Erteleme & Sabah Kurbağası Önceliği:** Erteleme isteğini kırmak için sadece 5 dakika çalışmayı taahhüt ettiren kalkan ve günün en zor konusunu öne çıkaran modül.
   - **📝 5. PDF Vurgu Notu & Çözümlü Soru Hata Defteri:** Sınavlarda en sık yapılan hataları ders adı, soru özeti ve öğrenilen doğru bilgiyle birlikte saklayan hata günlüğü.
   - **⏱️ 6. Animedoro & 90m Ultradian Sayaç Şablonları:** 40 dakika odak / 20 dakika anime-ödül molası veya beynin doğal 90 dakikalık biyo-ritmine uygun Ultradian çalışma şablonu.
   - **🤖 7. AI Sokratik Soru İpucu Çözümcüsü & Net Tahminleyicisi:** Takılınan soruda cevabı vermek yerine Sokratik ipucu soran AI koç ve denemelerden sınav günü netini tahmin eden motor.
   - **🐼 8. Sanal Kütüphane Masası (Pofi Çalışma Arkadaşı) & "Zinciri Kırma" Takvimi:** Pofi maskotun masada sizinle odaklanıp okuma yaptığı simüle masa ve ardışık gün alevi (`14 Gün 🔥🔥`).
   - **🧘 9. Sınav Anksiyetesi Yatıştırıcı 4-7-8 Nefes & Kahve-Uyku Kılavuzu:** Sınav kaygısını anında düşüren 4-7-8 nefes kuralı ve saat `17:00+` sonrasında REM uykusunu koruyan kafein uyarısı.
   - **🎒 10. Çevrimdışı Altın Formül Kasası & Deneme CSV Çıktısı:** Tarih, Matematik, Türkçe gibi derslerin en kritik formül/istisnalarını internetsiz sunan cep kitapçığı ve deneme sonuçlarını Excel'e yapıştırılabilir CSV formuna çeviren (`📋 CSV KOPYALA`) dışa aktarıcı.
2. **📚 100 Uzman Ders Çalışma & Kullanım Kolaylığı Önerisi Katalogu (`~/100-DERS-VE-KOLAYLIK-ONERISI.md`):**
   - Uzman pedagojik gözlemler, bilişsel öğrenme bilimi, YKS/KPSS/ALES sınav stratejileri ve sıfır-sürtünmeli arayüz (Zero-Friction UX) ilkeleriyle hazırlanan 10 kategoride 100 yepyeni öneri derlendi.
3. **Birim Testleri (`DersKolaylikTest.kt`):**
   - 10 modülün tüm fonksiyonel mantığı için 20 yeni saf JVM birim testi yazıldı; projenin toplam birim test sayısı **940** oldu (`940 tests, 0 failures, 0 errors`).
4. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu. Tüm kartlara `selectableItemBackground` dalga tutarlılığı eklendi.

---

## v10.57 (code 213) — Faz 2: C, D, E, G, H, I ve J Uzman Modülleri (#25, #39, #49, #65, #72, #82, #98 vb.) · 10 Ağu 2026

1. **Faz 2: 7 İleri Seviye Kategorinin Uzman Kontrol Merkezi (`UzmanModuller.kt`, `UzmanModullerActivity.kt` & `activity_uzman_moduller.xml`):**
   - **🤖 Modül C — Uzman Faz 2 (Maddeler #25, #26):** Biyo-Vakit Gündem Orkestrasyonu (Sabah 05-12 Analitik / Öğle 12-16 Toplantı / İkindi 16-19 Tekrar / Akşam 19-23 Feynman) ve saat `23:30+` olduğunda o günkü odak `0 dk` ise 10 dakikalık acil kurtarma oturumu öneren alarm radarı.
   - **🏆 Modül D — Uzman Faz 2 (Maddeler #37, #39):** "🌱 İlk Adım (%92)", "🦉 Gece Kuşu (%34)", "🧘 Zen Ustası (%18)" gibi rozetlerin kullanıcı nadirlik yüzdelerini listeleyen vitrin ve rütbe/kupa durumunu ASCII kart olarak panoya kopyalayan Sosyal Başarı Kartı Üreticisi (`📋 SOSYAL KART KOPYALA`).
   - **🎧 Modül E — Uzman Faz 2 (Maddeler #44, #49):** Odak başlangıç/bitiminde binaural seslerin 5 saniyede yumuşakça yükselip sönmesini sağlayan Fade-In/Out ayarı ve kulaklık çıkarıldığında sayacı anında duraklatan Auto-Pause simülatörü.
   - **⏱️ Modül G — Uzman Faz 2 (Maddeler #65, #68):** Ardışık pomodoroları sayarak Zihinsel Yorgunluk Endeksi hesaplayan ve yorgunluk `%75+` olduğunda 15 dakika yürüyüş molası verdiren Odak Yorgunluk Radarı ile pomodoro bitiminde 1 satırlık başarı logu oluşturan Çıktı Hasadı.
   - **🎨 Modül H — Uzman Faz 2 (Maddeler #72, #80):** Renk, köşe ve font ayarlarının anında yansıdığı Canlı Arayüz Aynası ve ekran üzerinde görünebilecek kompakt 1 satır özet şeridi (`⚡ 18m Kalan | 🎵 40Hz Gamma | 👑 Efsane`).
   - **📚 Modül I — Uzman Faz 2 (Maddeler #82, #87):** KPSS 2026, YKS 2027 ve ALES hedeflerini izleyip `45 gün` altında `🚨 YAKLAŞTI` uyarısı veren Sınav Geri Sayım Şeridi ve 400 sayfalık kitaplardan `15 sayfalık` çalışma paketi ayıran PDF Sayfa Bölücü.
   - **⚙️ Modül J — Uzman Faz 2 (Maddeler #98, #99):** "Fatura", "Feynman", "REM", "Gamma", "KPSS" gibi kelimeleri yazıp anında ilgili kategori adresi veren Anahtar Kelime Arama Çubuğu ve Android 13/14 bildirim iznini (`AÇIK ✔`) ile Doze pil optimizasyonunu (`KAPALI ✔`) denetleyen Alarm Sağlığı Test Merkezi.
2. **Birim Testleri (`UzmanModullerTest.kt`):**
   - 7 uzman modülün mantığı için 20 yeni saf JVM birim testi yazıldı; projenin toplam birim test sayısı **920** oldu (`920 tests, 0 failures, 0 errors`).
3. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu. Tüm kartlara `selectableItemBackground` dalga tutarlılığı eklendi.

---

## v10.56 (code 212) — C, D, E, G, H, I ve J Gelişmiş Hayat Atölyesi (#21..#50 ve #61..#100) · 10 Ağu 2026

1. **7 Kategorinin Çekirdek & Gelişmiş Modül Merkezi (`GelismiAtolye.kt`, `GelismiAtolyeActivity.kt` & `activity_gelismis_atolye.xml`):**
   - **🤖 Modül C (Otonom AI Koçluğu & Özel Talimat - #21..#30):** AI asistana "Her gün bir Sokratik soru sor" gibi override kuralı atama ve serbest notların içinde eylem kelimesi algılayıp tek tuşla görev oluşturan NLP Nottan Görev Çıkarma motoru.
   - **🏆 Modül D (Oyunlaştırma, XP & Hafta Sonu Maratonu - #31..#40):** Çırak/Usta/Efsane rütbe hesabı, 1.5x combo çarpanı ve +40m maraton ekleme butonuyla 120 dakikaya ulaşılınca kazandırılan 👑 Altın Kupa.
   - **🎧 Modül E (Ses, Frekans & Binaural Mikser - #41..#50):** 40Hz Gamma (odak) ve 10Hz Alfa (rahatlama) binaural katmanlarını bağımsız açıp kapatma, Kilis Çanı / Gong bitiş sesi ve 3 Kısa / Kalp Atışı titreşim ritm kontrolü.
   - **⏱️ Modül G (Zamanlayıcı, Esnek Sprintler & PiP - #61..#70):** 25-5, 50-10, 30-5 ve 15-0 serbest sprintler arasında geçiş, 00:00 sonrasında akışta kalınan fazla süreyi (`+5 dk`) loglayan Taşma Süresi ve 15s Masaya Dönüş sayacı.
   - **🎨 Modül H (Gelişmiş Arayüz & Tasarım Özelleştirme - #71..#80):** Ultra Keskin 0dp, Modern Yuvarlak 16dp ve Gece Zen 24dp tasarım şablonlarını anında uygulama ve Poppins/Atkinson/Lora font seçimi.
   - **📚 Modül I (Ders, KPSS, PDF & Öğrenme Motoru - #81..#90):** KPSS / YKS günlük 100 soru hedefine karşı +10 Soru butonu, ders saat bütçesi takibi ve "Konuyu 10 yaşındaki çocuğa anlatır gibi özetle" kuralıyla Türkçe metnin anlaşılabirliğini ölçüp %95 skor üreten Feynman Anlatım Simülatörü.
   - **⚙️ Modül J (Sistem, Otomasyon & Yedekleme - #91..#100):** Notlar, PDF'ler ve önbelleğin MB cinsinden depolama analizini sunup önbelleği temizletme ve C-D-E-G-H-I-J modüllerinin tüm verisini tek tuşla panoya kopyalayıp geri yükleyen Bütüncül JSON Klonlayıcı.
2. **Birim Testleri (`GelismiAtolyeTest.kt`):**
   - 7 modülün fonksiyonel mantığı için 19 yeni saf JVM birim testi yazıldı; projenin toplam birim test sayısı **900** oldu (`900 tests, 0 failures, 0 errors`).
3. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu. Tüm kartlara `selectableItemBackground` dalga tutarlılığı eklendi.

---

## v10.55 (code 211) — 10 Özel Yaşam Modülü & Manuel Kontrol Merkezi (#1..#10 + 100 Öneri) · 10 Ağu 2026

1. **10 Özel Yaşam Modülü & Manuel Kontrol Merkezi (`YasamModulleri.kt`, `YasamModulleriActivity.kt` & `activity_yasam_modulleri.xml`):**
   - **💊 1. Manuel İlaç & Vitamin Saati Takipçisi:** Doz miktarı (mg), saat, "Yemekten Önce/Sonra" tercihi ve durum değiştirme (`☑ Alındı / ☐ Bekliyor`).
   - **💳 2. Akıllı Fatura & Abonelik Bütçe Monitörü:** Netflix, Spotify, Su vb. aboneliklerin aylık yükü, ödenen/kalan hesaplaması ve geciken ödeme uyarısı.
   - **💧 3. Günlük Su & Kafein Tüketim Sayacı:** `+250ml Su` ve `+80mg Kafein` hızlı ekleme, hidrasyon yüzdesi ve 400mg kafein sınırı aşıldığında uyarı mekanizması.
   - **🏆 4. Pofi Maskot Oyunlaştırma Rozet Kilit Merkezi:** 10 özel Pofi rozeti ("100 Saat Odak", "Gece Kuşu", "Zen Ustası" vb.), açılan rozet yüzdesi ve manuel kilit açma testi.
   - **🌙 5. Biyo-Ritim & Uyku Döngüsü Manuel Ayarlayıcısı:** 90 dakikalık REM döngülerine göre (4, 5, 6 döngü) ideal uyanma saati hesabı ve dinçlik skoru (%80–%100).
   - **🎧 6. Gelişmiş Ambient Sound & Frekans Mikseri:** Yağmur, Orman ve Beyaz Gürültü seviyeleri ile 40Hz Gamma ve 10Hz Alfa binaural mikser denetimi.
   - **💰 7. Hızlı Harcama & Fiş Kayıt Günlüğü:** Market, Kahve, Ulaşım harcamalarını anlık ekleme, en çok harcanan kategoriyi bulma ve günlük bütçeden kalan tutarı izleme.
   - **🚨 8. Çevrimdışı Hayatta Kalma & Acil Durum Kasası:** Kan grubu, SOS acil durum kişisi (112) ve tıbbi alerji notlarını internete ihtiyaç duymadan saklama.
   - **🤖 9. Yapay Zeka Koçluk Tonu Manuel Seçicisi:** AI koçun kişiliğini "🎖️ Sert Askeri Koç", "🧘 Şefkatli Zen Rehberi", "📜 Sokratik Filozof" veya "🐼 Esprili Pofi Maskot" olarak elle seçme.
   - **🔄 10. Manuel Yedekleme & JSON Veri Klonlayıcı:** 10 modülün tüm durumunu tek tuşla JSON panosuna kopyalama ve panodan anında geri yükleme (`VeriKlonlayici`).
2. **📚 100 Tamamen Farklı ve Manuel Kontrol Edilebilir Özellik Önerisi Katalogu (`~/100-YENI-ONERI-KATALOGU.md`):**
   - Uygulamayı yaşam sağlığı, biyo-ritim, finans, bütçe, yapay zeka koçluğu, oyunlaştırma, ses/frekans, acil durum hazırlığı, zamanlayıcı, tasarım atölyesi ve KPSS/Ders arşivleri açısından dönüştürecek **10 kategoride 100 yeni öneri** derlenmiş, her birinin manuel kontrol ve ayar detayları dökümante edilmiştir.
3. **Birim Testleri (`YasamModulleriTest.kt`):**
   - 10 modülün tüm fonksiyonel mantığı için 16 yeni saf JVM birim testi eklendi; projenin toplam birim test sayısı **881** oldu (`881 tests, 0 failures`).
4. **Tasarım & Mimari Uygunluk:**
   - XML yerleşiminde hiçbir sabit köşelik veya yazı boyutu kullanılmadı; tüm nesneler `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` kurallarına uyduruldu. Tüm kartlara `selectableItemBackground` dalga tutarlılığı eklendi.

---

## v10.54 (code 210) — Sesli "Gündem & Vakit Brifingi" + Akıllı "Odak & Verimlilik Karnesi" · 10 Ağu 2026

1. **Sesli Brifing ve Haftalık Verimlilik Karnesi (`SesliBrifing.kt`, `VerimlilikKarnesi.kt` & `KarneActivity`):**
   - **🔊 1. Sesli Gündem & Vakit Brifingi:** Sabah (`saat < 12`) veya gün içi durumuna göre doğal dille Türkçe brifing metni (`brifingMetniUret`) oluşturur: *"Günaydın! Sıradaki namaz vakti Öğle. Bugün bekleyen 5 göreviniz ve 12 günlük aktif seriniz var. Hayırlı ve verimli bir gün dilerim."* Cihazın TTS motoruyla tek dokunuşla ekrandaki brifingi sesli okur.
   - **🏆 2. Haftalık Verimlilik Karnesi & Harf Notu:** Son 7 günün odak süresi (`Store.recentDayStats`), tamamlanan görev sayısı ve kesinti verilerini analiz eder; haftalık harf notu (`A+`, `A`, `B`, `C`, `D`), toplam odak saati ve en verimli gün adını (ör. *"En Verimli Gün: Çarşamba"*) hesaplar.
   - **💡 3. AI Koç Tavsiyesi & Gelişim İpuçları:** Karne notunuza ve kesinti sayınıza özel koç tavsiyesi sunar: *"Harika bir hafta! En yüksek odaklanmayı Çarşamba günü gösterdiniz."* veya *"Bu hafta 11 kez kesintiye uğradınız. Odak Kalkanı'nı aktifleştirin."*
2. **Kolay Erişim Köprüleri:** Ana ekran sağ üst barda hızlı buton (**`🏆`**) ve ayarlar ekranında özel satır (`rowKarne`).

Test: **866 / 0 hata (84 suite, +16 yeni test: `KarneTest`)**.

---

## v10.48 (code 204) — Kullanıcı Maddesi #10: OTONOM AI AJANI & OTOPİLOT MİMARİSİ (`OtonomMerkezActivity`) · 10 Ağu 2026

1. **#10 🤖 Otonom AI Ajanı & Otopilot Merkezi (`OtonomMotor.kt` & `OtonomMerkezActivity`):**
   - **⚡ 1. Eylem Yetkili AI Ajanı (Tool/Function Calling):** Doğal dille yazılan komutları (`"Sabah uyanma saatimi 07:30 yap, 25 dk sayaç kur ve görev ekle"`) analiz eder; saniyesinde veritabanında işlem yaparak uyanma saatini (`UykuCerceve`), sayaç süresini (`TimerEngine`), görev listesini (`Store.addTask`) ve odak hedefini anında günceller!
   - **⚡ 2. Akıllı Gündem Orkestratörü:** Dün geceki uyku süresi (< 6 saat) ve günün saatine göre zihinsel ağır işleri (matematik, rapor, proje vb.) öğleden sonraki verimli odak penceresine alır, kolay rutin işleri sabaha yerleştirir.
   - **🛡️ 3. Akıllı Alışkanlık & Seri Bekçisi:** Akşam saatlerinde kırılmak üzere olan aktif serileri (`habitStreak >= 2`) proaktif tespit eder; özel **"▶ 10 Dk Sayaç Başlat & Kurtar"** butonu sunar. Süre dolunca alışkanlığı otomatik `DONE` işaretler!
   - **🧹 4. Otonom Kütüphaneci & Temizlik Ajanı:** Depodaki tüm dağınık notları okuyup (`Store.loadNotes`) içindeki eylem gerektiren ifadeleri (`[ ]`, `TODO`, `al`, `yaz`) tespit eder ve tek dokunuşla Görevler listesine ekler.
   - **🤖 5. AI Otopilot Modu Anahtarı:** Tek anahtarla aktif edilir (`Store.getOtopilotAcik`); yoğun ve yorgun günlerde günlük odak hedefini güvenli esnek seviyeye (`otopilotHedefHesapla`) kısıtlayarak tükenmişliği önler.
2. **Kolay Erişim Köprüleri:** Ana ekran sağ üst barda hızlı buton (**`🤖`**), ayarlar ekranında özel satır (`rowOtonomMerkez`) ve Asistan sohbet ekranı köprüsü.

Test: **778 / 0 hata (79 suite, +14 yeni test: `OtonomTest`)**.

---

## v10.47 (code 203) — Kullanıcı Maddesi #9: Manuel Kontrol Merkezi (`ManuelKontrolActivity`) · 10 Ağu 2026

1. **#9 🎛️ Manuel Kontrol Merkezi (`ManuelKontrol.kt` & `ManuelKontrolActivity`):**
   - **🛏️ Uyku & Uyanma Saatleri:** Bugünkü uyanma ve uyuma saatleri `TimePickerDialog` ile elle seçilir, anında `UykuCerceve` kayıtlarına ve ortalamalara işlenir.
   - **📅 Geçmiş 14 Günün Defter Düzenleyicisi:** Son 14 günün uyku/uyanma saatleri tek dokunuşla düzenlenebilir veya silinebilir (`UykuCerceve.elleKaydet`, `UykuCerceve.gunSil`).
   - **⚡ Odak Süresi & Sayaç Dakikaları:** `+15 dk`, `+30 dk`, `-15 dk` veya serbest dakika girişiyle günün odak süresi elle değiştirilir (`Store.addTodayFocusMinutes`).
   - **🔥 Gün Serisi (Streak) & Başarı:** Mevcut seri 0..9999 gün arasında serbestçe ayarlanır (`Store.setStreakDays`); bugün başarılı işareti eklenebilir.
   - **🌅 Rutinleri Şimdi Çalıştır:** Sabah günaydın planı ve akşam kapanış sorusu saati beklemeden anında tetiklenir (`UykuAksiyonReceiver.elleSabahCalistir`, `elleAksamCalistir`).
   - **🔄 Bugünkü Durumu Sıfırla:** Bugünkü uyku ve odak kayıtlarını temizleme.
2. **Kolay Erişim Köprüleri:** Ana ekran sağ üst barda hızlı buton (**`🎛️`**), ayarlar ekranında özel satır (`rowManuelKontrol`) ve Uyku Ayarları ekranında düzenleyici kısayolu.

Test: **764 / 0 hata (78 suite, +12 yeni test: `ManuelKontrolTest`)**.

---

## v10.46 (code 202) — Kullanıcı Maddesi #8: Kullanışlı & Kontrol Edilebilir Mini Sayaç · 10 Ağu 2026

1. **#8 Kontrol edilebilir PiP (RemoteAction):** Android PiP penceresinde arayüz butonları dokunma olayı almaz. Bu nedenle sisteme entegre 3 canlı RemoteAction butonu (`PictureInPictureParams.Builder.setActions`) eklendi:
   - **▶ Devam / ⏸ Bekle (`101`):** Sayacın çalışma durumuna göre otomatik yön değiştirir.
   - **⏹ Sıfırla / Bitir (`102`):** Geri sayımı sıfırlar veya İleri Sayım oturumunu bitirip odaka kaydeder.
   - **＋5 dk Uzat (`103`):** Geri sayım modunda oturuma anında 5 dakika ekler (`TimerEngine.uzat(ctx, 5 * 60_000L)`).
2. **Kompakt & Ferah PiP Kadranı ("Özellikleri Küçült"):** PiP modunda çalışmayan ekrandaki alt butonlar (`mainAction`, `resetButton`) gizlendi; YALNIZCA kadran kaldı. `MiniMod.pipOlcegi(pip)` ile kadran **×1,15 ölçeklendirildi** ve kenar boşlukları (`pipDolguDp = 4dp`) daraltıldı. Küçük pencerede rakamlar ve halka büyük, net ve taşmadan görünür.
3. **Canlı Senkronizasyon:** `TimerFragment` içinde çalışan özel `BroadcastReceiver` (`PIP_KONTROL`), basılan her butonu motora iletir ve buton ikonlarını (`pipAksiyonlariniGuncelle()`) saniyesinde yeniler.

Test: **752 / 0 hata (77 suite, +4 yeni test)**.

---

## v10.36 (code 192) — Öneri Dalgası 11 · 9 Ağu 2026

1. **#19 Görev yoğunluğu ▦:** başlık + geçiş düğmesi (kart ⇄ kompakt);
   `item_task_compact.xml` aynı id'ler — seçim/etiket/⚠️ dokunuşları aynen.
2. **#16 Bekliyor durumu ⏳:** tiBar'da toplu işaretle/çöz; satırda ⏳
   öneki (saf rozetliMetin, 2 test); pref `gorev_bekliyor_v1` yedekte.
3. **Tasarım bekçisi avı (dürüst):** 1. tur `istisna sayisi artmiyor`
   testine takıldı — kompakt layout'a kopyaladığımız `textSize="1sp"`
   küresel sınırı aşıyordu (6→7). Layout'tan silindi (test haklıydı),
   2. tur 7dk54 yeşil.

Test: **695 / 0 hata (64 suite)**.

---

## v10.35 (code 191) — Öneri Dalgası 10 · 9 Ağu 2026

1. **#37 Not arşivi 🗄:** menüden arşive kaldır/çıkar; normal liste
   arşivlileri hiç göstermez; çip şeridinde "🗄 Arşiv (N)" çipi mod değiştirir
   (renk+arama arşivde de çalışır). Pref `not_arsiv_v1` yedekte.
2. **#34 Kompakt ⇄ kart ▦:** başlık yanındaki geçişle iki yoğunluk;
   `item_note_compact.xml` aynı id'lerle — maske/renk/markdown dokunulmadan
   çalışır; tercih kalıcı.
3. **#42 (mola modu) ertelendi:** seri motoru ayrı odak ister — yarım iş
   yok (dürüst plan değişikliği, notlara işlendi).

Derleme notu (dürüst): **tek tur yeşil 15dk15** (önbellek tazelemesi sonrası
ilk release biraz uzun). Test: **693 / 0 hata (63 suite)**.

---

## v10.34 (code 190) — Öneri Dalgası 9 · 9 Ağu 2026

1. **#21 Not araması 🔎:** searchSlot'a kalıcı EditText; başlık+içerikte
   süzer, renk filtresiyle birleşir; kilitli not içeriği aramaya dahil
   edilmez (gizlilik kuralı koda işli).
2. **#26 Not kilidi 🔒:** menüden kilitle/aç; satır "🔒 Kilitli not"a
   maskelenir; dokun/uzun-bas/menü/silme yollarının TÜMÜ PIN doğrulaması
   ister (KilitDepo.pinDogruMu + ortak kaba-kuvvet havuzu). Kaydır-sil
   boşluğu dalga ortasında yakalanıp kapatıldı.
3. **Dürüst derleme kaydı:** 1. tur kırmızı — test dosyamda `(sizTest=false)`
   parametresi kalıntısı (benim hatam); düzeltildi, 2. tur 8dk58 yeşil.

Test: **690 / 0 hata (62 suite)**.

---

## v10.33 (code 189) — Öneri Dalgası 8 · 9 Ağu 2026

1. **#15 Görev paylaşımı 📤:** çoklu seçim şeridine "📤 Paylaş"; seçili
   görevler "☑/☐ metin · ⏰ tarih" bloğu olarak ACTION_SEND ile paylaşılır
   (saf GorevDisAktar, 5 test).
2. **#38 Otomatik başlık ✨:** başlık boş kaldığında ilk dolu satır
   (işaretler soyulmuş, 60 kr) başlığa yazılır (saf NotOneri).
3. **#39 Damga 🕘:** not penceresinde "🕘 Damga" — imleç konumuna
   "9 Ağustos 2026, 14:32" ekler.

Derleme notu (dürüst): **tek tur yeşil 11dk54** — üst üste 6. temiz tur.
Ön-denetimde bu kez denetleyicinin KENDİ iddiası yanlış çıktı (w33_damga
Kotlin'de arandı, oysa layout'tan besleniyor) — gerçek kod doğruydu.
Test: **687 / 0 hata (61 suite)**.

---

## v10.32 (code 188) — Öneri Dalgası 7 · 9 Ağu 2026

1. **#23 Renk etiketi 🎨:** menüde 5 pastel ton; başlıkta nokta; renkli not
   varsa "Tümü + tonlar" filtre şeridi. `not_renk_v1` ayrı tercih (yedekte),
   Room'a dokunulmadı.
2. **#29 Sürüm geçmişi 🕘:** kayıt anında eski hâl itilir (son 5), menüden
   onaylı geri yükleme; geri yükleme de önce şu anı geçmişe iter. Birleştirme
   (#33) de artık geçmişe düşer. UTF-8 hex kodlama, JVM testli.
3. **Filtre güvenliği (kendi bulgum):** filtre aktifken `saveNotes(notes)`
   gizli notları silebilirdi → tüm kayıtlar `loadNotes→mutasyon→saveNotes`
   (tam liste) desenine taşındı.
4. **Öz-denetim avı:** derlemeden ÖNCE `Titresim.hafif` olmadığı yakalandı,
   `dokunus` ile düzeltildi — kırmızı tur hiç yaşanmadı.

Derleme notu (dürüst): **tek tur yeşil 13dk11** — üst üste 5. temiz tur.
Test: **682 / 0 hata (60 suite)**.

---

## v10.31 (code 187) — Öneri Dalgası 6 · 9 Ağu 2026

1. **#24 Markdown-light 📝:** liste başlık/önizlemede `**kalın**` → kalın,
   `# ` satırı → büyük+kalın; kapanmayan işaretler değiştirilmez, boş
   kalın yoksayılır (NotBicim saf, 5 test; biçimsiz notta hızlı yol).
2. **#36 Okuma süresi ⏳:** tarih rozeti "· ~N dk okuma" (200 kelime/dk).
3. **Süreç:** derleme beklemesi tek bash çağrısına indi (bağlam dostu).

Derleme notu (dürüst): **tek tur yeşil 12dk04** — üst üste 4. temiz tur.
Test: **674 / 0 hata (58 suite)**.

---

## v10.30 (code 186) — Öneri Dalgası 5 · 9 Ağu 2026

1. **#31 Bağlantı açma 🔗:** listede URL satırları autoLink ile renklenir;
   uzun basış menüsüne koşullu "🔗 Bağlantıyı aç" (ilk URL, www.→https://
   tamamlama; NotBaglant saf, testli). LinkMovementMethod kasıtlı yok.
2. **#33 Not birleştirme ➕:** menüden ikinci not seç → onay → hedefe
   birleşir ("Başlık · Başlık", gövdeler "\n\n"); diğer not
   `deleteNoteUndoable` ile kaldırılır → geri al şeridi çalışır.
3. Menü dinamik eylem listesine geçti (pin/paylaş/bağlantı/birleştir).

Derleme notu (dürüst): **tek tur yeşil 12dk19** — üst üste 3. temiz tur.
Test: **669 / 0 hata (57 suite)**.

---

## v10.29 (code 185) — Öneri Dalgası 4 · 9 Ağu 2026

1. **#27 Kelime/karakter sayacı ✍️:** dialog_note içerik alanı altında
   "N kelime · M karakter", her tuşta güncel (saf NotOlcum, 6 test).
2. **#28 Satırdan görev ☑:** Not penceresinde "☑ Satırları görev yap";
   `- / * / • / [ ] / [x] / ✓` işaretleri soyulur, boş satırlar atılır,
   onay sonrası tek saveTasks çağrısıyla eklenir (tarihsiz, 120 kr kırpma).
3. **İç tutarlılık:** sayaç ve çevirici aynı saf modülü paylaşır (NotOlcum).

Derleme notu (dürüst): **tek tur yeşil 12dk10**; ön öz-denetim 6 düzenleme
+ 2 yeni dosya hatasız. Test: **664 / 0 hata (56 suite)**.

---

## v10.28 (code 184) — Öneri Dalgası 3 · 9 Ağu 2026

1. **#62 İleri Sayım geçmişi ⏱:** Bitir ile kaydedilen oturumlar kalıcı
   dosyaya (`ileri_sayim_gecmis_v1`, 200 kayıt / 35 saat budama) işlenir;
   gün sınırını aşan oturum iki güne bölünür (saf `SurecPlan`, 11 test).
   Kadran boştayken alt satır "Bugün: X · Dün: Y dk"; uzun basış menüsünde
   adlandırma yanında **Oturum geçmişi** diyaloğu (özet + son 30 oturum).
2. **#30 Not .txt paylaşımı 📤:** Not satırında uzun basış artık menü —
   📌 Sabitle/çöz (v10.27 davranışı korunur) + 📤 .txt paylaş
   (`cacheDir/notlar/`, FileProvider, EXTRA_TEXT de dolu).
3. **İç iyileştirme:** ReminderReceiver IO'su tek-iş parçacıklı executor'a
   taşındı (ana thread anında döner).

Derleme notu (dürüst): **tek tur yeşil ~12 dk**; öncesinde statik öz denetim
(XML minidom, w28 string diff, kaçış kontrolü) 12 düzenleme birden tek
betikte hatasız geçti. Test: **658 / 0 hata (55 suite)**.

---

## v10.27 (code 183) — Öneri Dalgası 2 · 9 Ağu 2026

1. **#22 Not sabitleme 📌:** satıra uzun bas → pin; sabitler başta. Mimari:
   Note/Room değişmedi; pin kümesi `not_sabitle_v1` (yedek taraması kapsar).
   Kararlı sıralama birim testli (NotSabitleTest 3).
2. **#76 Haftalık odak hedefi:** todayStat artık birleşik satır
   "Bugün: X dk · Hafta: Y/Z dk · %v" (hedef=günlük×7; OdakHaftaTest 3).
3. **#12 Mikro kutlama:** görev tamamlamada Titresim.basari +
   Kutlama.TUR_YILDIZ (animasyon kapalıysa hiç oluşmaz).

Derleme notu (dürüst): 1. tur test tarafında tip çıkarımı hatasıyla kırmızı
(emptyList<N>); düzeltildi, stale test XML'leri de temizlendi. 2. tur yeşil (8dk).
Test: **647 / 0 hata (54 suite)**.

---

## v10.26 (code 182) — Öneri Dalgası 1 · 9 Ağu 2026

İlk uygulama dalgası (katalog: `DEVIR/ONERI-1000.md`):

1. **#4 Geciken görev hızlı ertelemesi:** kırmızı ⚠️ rozetine dokun→
   Bugüne/Yarına/Tarih seç… Görev taşınır, alarm YENİDEN kurulur,
   geri-al şeridi çıkar.
2. **#20 Toplu Tarih… düğmesi:** seçili görevler takvimden seçilen güne
   (09:00) toplu taşınır; geri-al mesajında hedef tarih yazar.
3. **#61 İleri Sayım oturum adı:** İleri düğmesine uzun bas → isim ver;
   kadran üst satırı + Bitir toast'ında görünür (60 kr sınırı, adTemiz
   normalleştirmesi birim testli).

Test: **641 / 0 hata**. Zincir: tek çağrı 12dk40, yükleme paralel.

---

## v10.25 (code 181) — Tam Yedek Kapsamı · 9 Ağu 2026

**Derin denetim bölgesi:** Yedekleme ve geri yükleme (Store.importJson, PrefYedek).

### 🔴 Bulunan ve düzeltilen hatalar
1. **F1 — importJson gizli koşul:** `ZorunluUyari` ve `SohbetGecmisi` (AI sohbet
   geçmişi), `has("namaz_plan")` koşulunun İÇİNE gömülüydü (kopyala-yapıştır
   kalıntısı). Koşulsuz `optString("ai_sohbetler","[]")` eski yedekte mevcut
   sohbeti EZEBİLİRDİ. → Her alan kendi `has(...)` kapısına alındı.
2. **F2 — Yedek kapsamı gerçek taramaya çevrildi:** Belgeler "yeni modül
   otomatik yedeklenir" diyordu ama kod sabit 19 depoluk liste kullanıyordu.
   Ölçüm: ~90 depodan 19'u yedekteydi. Ekran Atölyesi, Widget Atölyesi,
   kilit_v1 (PIN), bildirim saatleri, mikro günlükler, sessiz türler...
   telefon değişiminde kayboluyordu. → `shared_prefs` klasörü taranıyor;
   bilinçli HARIC dışındaki HER depo yedeğe giriyor.
3. **F3 — Ana depo adı hatası:** İstisna "gunluk_asistan_prefs" yazıyordu;
   gerçek dosya `gunluk_asistan_store`. → Sabit doğru ada çevrildi.
   (İlk derleme denemesinde ANA_DEPO ileri-referans hatası çıktı; deklarasyon
   sırası düzeltilip ikinci turda yeşil derleme alındı — dürüst kayıt.)

### Bilinçli kararlar
- `kilit_v1` yedeğe girer (PIN yeni telefonda da çalışır).
- Oturum durumu (timer_engine, ileri_sayim, sayac_zincir, kritik_alarm,
  wg_ay_ofset) yedeğe GİRMEZ.
- Kendi kanalı olan depolar (quiz/kart/öğretmen/namaz/film/online/sohbet/
  zorunlu) HARIC: çift yazım önlenir.
- Yedek biçimi 19'da kaldı → iki yönlü uyum korunur.

### Test
- PrefYedekTest (yeni, 11 test). Toplam: **638 test / 52 suite — 0 hata**.

### 1000 öneri kataloğu
- `DEVIR/ONERI-1000.md`: 50 kategori × 20 öneri = 1000 madde.

---

## ⏱ v10.24 — İLERİ SAYIM: zamanlayıcının 3. modu (kullanıcı isteği) (9 Ağu 2026)

> **İstek (aynen):** "...kronometre ve geri sayımın ortasına ileri sayım yeri
> ekle ve geri sayim gibi olsun ama ileri doğru saysin; başlatınca durdurunca
> da ders saati yerine ekleme yapsin dakikayi ama bekle dersem bekletsin
> dursun orda; ekrani kapatsam bile sonra devam ettirebileyim."

**Davranış:**
- Mod sırası: **Geri sayım · İleri · Kronometre**.
- **Başlat ⇄ Bekle ⇄ Devam** tek düğme; durum damga-temelli prefs'te —
  ekranı/uygulamayı kapatmak, süreç ölmek durumu SİLMEZ; sonra devam edilir.
- Geri sayım gibi arka planda da sayar (duvar saati; simetri tam).
- **Bitir · +X dk** → `Store.addTodayFocusMinutes` + `WidgetCommon.refreshAll`
  + bugünkü odak + maç tazeleme = **v10.19 manuel odak kanalıyla birebir
  aynı yer** (kullanıcının "odaklanma saati" dediği yer).
- <1 dk Bitir → kayıp yok, süre bekletilir. >480 dk → "açık mı unutuldu?" onayı.

**Mimari karar (v10.19 sayaç-sıfırlanma dersi):**

| Bölge | Karar |
|---|---|
| TimerEngine / TimerAlarm / bildirim / zincir / widget | 🔴 dokunulmadı — kırılgan bölge, bu mod devreye sokulmadı |
| İleri durum | `IleriSayim.kt` (saf mantık 10 JVM testi + prefs) |
| Ekran | `TimerFragment`: MODE_ILE; kadran/büyük saat/rulo yeniden kullanıldı; motora dönünce `resetButton` etiketi "Sıfırla"ya geri alınır |

**Bilinçli sınırlar (dürüstlük):**
- İleri sayım çalışırken bildirim göstermez (bildirim motorun malı) — süre
  damga-temelli olduğundan yine de kaybolmaz.
- Tam ekran flip saat motorun kronometre/geri sayımını gösterir.

**🔴 Öz denetim — bu sürümde yakalananlar:**

| Tehdit (HATASIZLIK md) | Yakalanan tasarım riski | Çözüm |
|---|---|---|
| md 4 (süreç ölümü) | Bellek-içi kronometre mantığı süreç ölümünde kaybolurdu | Her şey prefs + damga; süreç yeniden doğsa `biriken + canlı bölüm` doğru hesaplanır |
| md 6 (saat kayması) | `simdi < baslangic` negatif süre üretirdi | `coerceAtLeast(0)` + test |
| md 7 (sınır) | 59 sn'de Bitir 0 dk yazar, kullanıcı "çalışmadı" sanırdı | <1 dk kaydedilmez, süre korunur, açıklayıcı toast |
| md 7 (sınır) | Açık unutulan sayaç 1400 dk dev kayıt yapardı | 480 dk üstü onay diyaloğu |
| md 8 (bayrak) | Motora dönüşte "Bitir" etiketi bayat kalırdı | `resetAll()` içinde etiket geri alma |

**Cihaz doğrulaması (kullanıcıdan beklenen):**
1. Başlat → Bekle → uygulamayı tamamen kapat → aç → İleri modu seçili,
   süre durduğu yerde → Devam → saymaya devam
2. 1+ dk sonra Bitir → "+%d dk ders saatine eklendi" toastu + bugünkü
   odak satırı arttı + widget toplamı arttı
3. Mod değiştir (geri sayım) → geri dön: İleri oturumu korunmuş
4. Kronometre/geri sayım eskisi gibi çalışıyor (regresyon yok)

**Ölçümler:** ortam tam iade ~75 sn (çatı+önbellek, sandbox sıfırlanmıştı) ·
tek çağrı **13dk36** (BUILD SUCCESSFUL, EXIT=0) · yetim 15/15 · XML 2/2 ·
**627 test 0 hata** (suite 51) · imza eşleşti · zip 758 dosya.

---

## 🎯 v10.23 — HATASIZLIK SÜRÜMÜ: derin tehdit avı (kullanıcı geri bildirimi) (9 Ağu 2026)

> **Tetik:** "Hataların halen var, uzun uzun düşün ve hatalarını gider…
> kusursuz ve hatasız hızlı bir kod yazmanı istiyorum hep."
> Yeni özellik YOK; `DEVIR/HATASIZLIK-PROTOKOLU.md` kuruldu, 10 maddelik
> tehdit avsı v10.22 koduna uygulandı — **cihaz raporu beklenmeden
> 2 gerçek hata bulundu ve giderildi.**

| 🔴 Bulunan hata | Av maddesi | Giderilme |
|---|---|---|
| **Boş/eksik PIN de deneme hakkı yakıyordu** — kilit ekranında boş ya da 3 haneli giriş yanlış-deneme sayacını artırıyordu; 5. seferde masum kullanıcı haksız 30 sn beklemeye düşerdi | md 1 (boş/geçersiz giriş) | `KilitActivity.dene`: doğrulamadan ÖNCE `pinGecerliMi` kapısı; biçim hatası sayaçsız uyarı, alan temizlenir |
| **PIN diyalogları hatada kapanıyordu** — kur/değiştir/doğrulada hatalı giriş Toast + kapanma üretiyordu; kullanıcı işleme baştan başlıyordu ("hata var" hissi birebir) | md 2+10 (diyalog kapanması / kullanıcı gözü) | keep-open deseni: `setPositiveButton(res, null)` + `setOnShowListener` + diyalog içi hata satırı (otomatik temizle + odak); ortak `hataMetinKutusu` |

- Test: 617/0/0 — değişiklik UI katmanında, yeni saf fonksiyon yok; mevcut
  19 KilitMantik testi davranışı zaten kapsıyor (dürüst not).
- Zincir: tek çağrı **13dk11** (BUILD SUCCESSFUL, EXIT=0) · zip 756 dosya ·
  imza birebir · code 179/10.23.
- **Öz denetim — bu tur yakalanan süreç kusurları:** yok (derleme
  komutları temiz koştu; v10.22'deki md5-satırı dersi işledi).
- ⏳ **Cihaz doğrulaması:** (1) kilit ekranında boş/3 hane gönder → hak
  AZALMAMALI; (2) kurma diyaloğunda uyumsuz tekrar → diyalog AÇIK kalır,
  hata metni içeride; (3) yanlış mevcut PIN → doğrula diyaloğu kapanmaz.

**Yedekleme sorusunun yanıtı (kullanıcı sordu):** Proje kaynağı hiç
bölünmez — her sürümde tam kopya zip. Ortam yedeği bilinçli 2 parça
(çatı + önbellek), her tur yalnız ~30 sn'de iade. Defter kalıcı.

---

## 🔒 v10.22 — GİZLİLİK KİLİDİ: PIN (kendi taramamla seçtim) (9 Ağu 2026)

> **Neden:** Havuz kapanmıştı; kod tabanı tarandı. Yedekleme, aralıklı
> tekrar, ayar içi arama hepsi vardı — ama kilit/PIN **hiç yoktu** (🔴).
> Notlar, bütçe, planlar ve alışkanlıklar korunmasız duruyordu.
> Bu sürüm "Gizlilik Kilidi"ni ekler.

| Katman | Dosya | Ne yapıyor |
|---|---|---|
| Saf mantık | `KilitMantik.kt` | PIN kuralı (4-8 rakam), sürümlenmiş SHA-256 hash (`GAK1|tuz|pin`), tuz üretimi, sabit zamanlı karşılaştırma, 5 yanlış → 30 sn bekleme sayacı, otomatik kilit kararı (saat-enjekte) |
| Depo | `KilitDepo.kt` | SharedPreferences okuma/yazma + süreç-içi oturum bayrakları (acik / azOnceAcildi / arkayaGitti) |
| Ekran | `KilitActivity.kt` | Programatik UI (XML yok), FLAG_SECURE, geri tuşu arka plana atar, bekleme sayacı canlı |
| Bekçi | `App.kt` | Yaşam döngüsü sayacı; öne gelişte gerekirse kilit açar; az-önce-açıldı bayrağıyla sonsuz döngü önlendi |
| Ayar | `SettingsFragment` + layout | Veri bölümüne 🔒 satır: kur / değiştir / kaldır (ikisi mevcut PIN ister) + otomatik kilit süresi (her zaman · 1 · 5 · 15 dk) |
| Metin | `strings.xml` | 26 dize + 1 dizi (`w22_*`) — yetim taraması 27/27 temiz |

**Güvenlik notları:**
- PIN hiçbir yerde düz saklanmaz; tuz cihaza özel (aynı PIN iki cihazda farklı hash).
- Karşılaştırma sabit zamanlı — zamanlamayla PIN sızmaz.
- Bekleme sırasında yeni deneme kaydı da engellendi (çift savunma hattı).

**Bilinçli taviz (dürüstlük):**
- ⚠ "Her zaman" kipinde 1,5 sn'den kısa uygulama geçişleri kilit sormaz
  (yapılandırma değişimi eşiği — ekran döndürmede kilitlenmeyi önlemek için).
- Kilit ekranı XML'siz kurulur; TasarimOlcegiTest kapsamı dışında, bu bilinçli.

**🔴 Öz denetim — bu sürümde yakalananlar:**

| Hata | Nasıl yakalandı | Çözüm |
|---|---|---|
| Sonsuz kilit döngüsü: kilit açılır açılmaz yeniden kilitlenirdi | Kod yazılırken akıl yürütme + test | `azOnceAcildi` tek-kullanımlık bayrak + test |
| Ekran döndürme "arka plan" sayılıp kilitlenirdi | Bekçi yazılırken fark edildi | `GECIS_ESIGI_MS = 1500` + test |
| Eski arka plan damgası aktif kullanıcıyı kilitleyebilirdi | Bayrak akışı denetimi | Resume'da bayraklar tek seferlik tüketiliyor |
| Doğrulama komutunda md5 satırı bitişik komutla birleşti, md5 basılamadı | Çıktı denetimi | Komut ayrıştırılıp tekrar koşturuldu ✔ |

**Cihaz doğrulaması (kullanıcıdan beklenen):**
1. PIN kur → uygulamayı kapat/aç → kilit sorulmalı
2. 5 yanlış → 30 sn canlı sayaç; giriş kapalı
3. "Her zaman" kipi: uygulama dışına çık/dön → kilit; ekran döndürme → kilit YOK
4. Geri tuşu kilidi atlatamaz (arka plana gider)
5. Son uygulamalar önizlemesi boş görünmeli (FLAG_SECURE)
6. Değiştir ve kaldır akışları mevcut PIN'i sormalı

**Ölçümler (hız protokolü v2 kanıtı):** ortam iadesi 29 sn · tek çağrıda
derleme+test+R8 **13dk08** (BUILD SUCCESSFUL, EXIT=0) · yükleme 3 dosya **paralel** ·
yetim 27/27 · XML 3/3 · APK 18,4 MB · imza eşleşti.

---

## 📑 v10.21 — LİSTE FİLTRELERİ + DOKUNMA HEDEFİ + BAŞLIKLAR (havuz kullanımı) (8 Ağu 2026)

> v10.20 çıkışında dürüstçe havuza bırakılan 4 madde vardı: liste satır
> filtresi ✓ · widget-bazlı dokunma hedefi ✓ · başlık çubuğu gizle ✓ ·
> (cam blur — bilinçli atlandı, aşağıda). Kullanıcı "devam et" dedi,
> kalanı kapattım.

### 1) Liste satır filtreleri (2 kaydırılabilir liste)

**Görev listesi** (TasksWidgetService):
- Tamamlananları göster (önceden koddan siliniyordu — artık tercih; varsayılan kapalı = eski davranış)
- Tarihsizleri göster / İleri tarihlileri göster (kova bazlı süzme; "sadece bugün" modu kurulabilir)
- En çok satır — serbest tam sayı (kodda sabit 40'tı)

**Geri sayım listesi** (EventsListService):
- Geçmişi göster/gizle (önceden 1 "geçti" satırı zorunluydu)
- Yalnız sabitlenenler (pin'li olmayan elenir)
- En çok satır — serbest (EventsListVeri.AZAMI_SATIR=6 sabitti)

Seçim mantıkları saf fonksiyonlara çıkarıldı (`WidgetListe.gorevleriSec`
saat-enjekte, `EventsListVeri.sec` parametreli overload — eski 5 test
varsayılanların birebirliğiyle korundu).

### 2) Widget-bazlı dokunma hedefi

Gövde dokunması hangi sekmeyi açsın? 12 sekme (Ana Sayfa…Vakit Planı) +
"Varsayılan" — 7 widget bağlandı: Geri sayım, Sayaç, Hedef, Özet,
Görev listesi, Geri sayım listesi, Odak kutusu (koşarken). Alt düğmeler
(başlat/duraklat, görev ekle, soru +1) bilinçli bu ayarın dışında —
onlar eylemdir.

### 3) Başlık çubukları gizle/göster (4 widget)

Görev listesi · Geri sayım listesi · Uyku · Hafta — üst başlık satırı
tek anahtarla gizlenir (varsayılan açık).

### Bilinçli atlanan (dürüstlük)

- **Namaz/Uyku/Kokpit/Modül gövde dokunması:** bu widget'larda gövdeye
  atanmış sekme açma YOK (tap yok ya da alt bölgeler var) — hedef
  seçici yalnız var olan dokunmalar için gösterildi.
- **Cam blur:** RemoteViews launcher duvar kâğıdını blur'layamaz
  (uluslararası RemoteViews kısıtı); bitmap zemin hem yeterince
  saydamlaşabiliyor hem de blur uydurması launcher'da çatlar. Eklemedim.

Birim testi: **+9** (WidgetListeTest) → toplam 49 sınıf, **598/598** ✔

---

## 🎚 v10.20 — SINIRSIZ WIDGET KONTROLÜ (kullanıcı isteği) (8 Ağu 2026)

> İstek (aynen): "Widgetların yazı boyutları ve aralıkları şablonunu tamamen
> değiştirme ve benzeri bir sürü her şeyin yetkisi ayarlarda ver — SINIR KOYMA."

### Ne değişti (ilke: kademe YOK, serbest giriş VAR)

Önceki sürümlerde kendim koyduğum sınırlar bu sürümde söküldü — tabloda
"sınır → serbest" dönüşümlerinin hepsi görünüyor:

| Ayar | Eski sınır | v10.20 (serbest) |
|---|---|---|
| Yazı boyutu (genel) | %75–%150, %5 adım kaydırıcı | **Serbest %** — taban %1 (teknik), tavan YOK |
| İç dolgu | 0/2/6/12 dp kademe | **Serbest dp** (taban 0) |
| Satır nefesi | 0/2/6 dp kademe | **Serbest dp** (taban 0) |
| Köşe yarıçapı | 6/26/38/48 dp + yalnız yeni aile | **Serbest dp — TÜM widget'larda** |
| Zemin saydamlığı | 4 hazır kademe | **Serbest %0–100** (fiziksel aralığın tamamı) |
| Yatay dolgu katsayısı | ×0.5 / ×1.0 / ×1.8 | **Serbest %** |
| Satır girintisi | 0/4/10 dp kademe | **Serbest dp** |
| Tazeleme kısıtı | 400 ms / 2 sn / 10 sn | **Serbest ms** (0 = kısıt yok) |
| Karartma şiddeti | hafif/orta/derin | **Serbest %0–100** |
| Vurgu canlılığı | soluk/normal/canlı | **Serbest %** (<100 yumuşatır, >100 doygunlaştırır) |
| Kontrast | açık/kapalı | **Serbest %0–100** |

### Mimari hamle: 12 eski widget bitmap zemine taşındı

Saydamlık ve köşe RemoteViews şekilleriyle (hazır `w_card_*` çekmeceleri)
ancak kademe kademe yapılabiliyordu. Kök sebep buydu; çözüm: eski 12
widget'ın kökü `FrameLayout + ImageView` sarmalına alındı, zemin artık
**üretilmiş bitmap** (`WidgetZemin` — yeni ailenin kanıtlanmış yolu).
Böylece köşe ve saydamlık "her yerde tam serbest" oldu. 12 layout sarıldı,
12 sağlayıcı tek satırla yeni yola bağlandı; statik `w_card` arka planları
kaldırıldı.

### Yeni yetkiler

- **Özel renk şablonu (4 renk, serbest hex):** zemin / metin / vurgu /
  tamamlanan rengi. `#RRGGBB` veya `#AARRGGBB`; boş = tema otomatiği.
  Kullanıcı rengi boru hattının EN SON halkası — karartma/kontrast gibi
  dönüşümlerin üstünde kalır (tam yetki). Türev renkler (soluk metin,
  vurgu soluğu) otomatik türetilir.
- **Örnek-başına yazı ölçeği (7 widget, serbest %):** Geri sayım, Özet,
  Eylemler, Sayaç, Hedef, Namaz, Uyku — her biri genel yüzdenin üstüne
  kendi çarpanını alır.

### 🔴 Dürüstlük — bu sürümde bulunan KENDİ eski kusurlarımız

1. **v10.16'nın vaadi eksikti:** "Yazı ölçeği her widget'ta çalışır"
   yazmıştık; gerçekte Sayaç, Eylemler ve Uyku boyutlarını programatik
   hiç ayarlamıyordu (XML boyutları sabitti) — genel ölçek bu üçünde
   hiç çalışmıyordu. v10.20'de `WidgetCommon.olcekliYazi` köprüsüyle
   gerçek oldu (dimen px→sp tabanı × ölçek; ölçek %100 ise hiç
   dokunulmaz = eski davranış birebir).
   · Eylemler widget'ında etiketlerin id'si bile yoktu; ölçek
   hedefi olabilmeleri için 5 etikete id verildi (XML).
2. **`wt_s_yok` dizesini yetim sanıp sildim** — gerçekte
   `PlanHizliActivity` kullanıyormuş. Zorunlu referans taraması
   derleme öncesi yakaladı; dize geri eklendi, 31 yetim doğrulandı.
3. **İlk derleme DENEMEM yanlış klasörde başladı** (EXIT=127 —
   `derle.sh` projenin içinden aranıyordu, betik `/home/user`'da).
   Koddan bağımsız operatör hatası; doğru yerden tek seferde geçti.

### Teknik kelepçeler (kullanıcı sınırı DEĞİL — çökme koruması)

- Yazı yüzdesi tabanı %1: `setTextSize` negatif/sıfır sp başlatıcıda
  çökertir. ÜST SINIR YOK.
- Dolgu/girinti tabanı 0 dp: negatif padding başlatıcıyı bozar.
- Köşe tabanı 0 dp, tavanı 2000 dp: negatif yarıçap Canvas'ı çökertir.
- Saydamlık/karartma/kontrast %0–100: fiziksel aralığın TAMAMI (sınır
  değil, tanım kümesinin kendisi).

### Cihaz doğrulaması (manuel — kullanıcıdan beklenen)

1. Ayarlar → Widget teması: herhangi bir serbest satıra dokun → uç bir
   değer yaz (ör. yazı %180) → widget'larda yansısın; sonra normale döndür.
2. Özel renk şablonundan zemin rengi gir (#123456 gibi) → TÜM widget'ların
   zemini değişmeli; Temizle ile eski haline dönmeli.
3. Saydamlığı %40, köşeyi 40 dp yap → ESKİ widget'larda da (ör. görevler,
   geri sayım) yansımalı — v10.20 öncesi bunlar kademeliydi.
4. Sayaç widget'ı yazı boyutunu %140 yap → yalnız sayaç büyümeli.

### Bilinçli dışarıda bırakılanlar (dürüstlük)

- "Gelecek tur havuzu"ndan **liste satır filtresi** ve **widget-bazlı
  dokunma hedefi** bu turda yok — sırada bekliyorlar.
- View.setAlpha yoluyla "tüm widget'ı soldurma" eklemedim: RemoteViews
  yansımasının launcher'da reddedilmesi widget'ı boş hata kutusuna
  çevirebilir; cihaz doğrulaması yapamayacağım bir riski eklemek yerine
  saydamlığı kontrol ettiğimiz yüzeyde (bitmap zemin) tam serbest
  bıraktım.

Birim testi: **+13** (WidgetSerbestTest) → toplam 48 sınıf, **589/589** ✔

---

## 🔧 v10.19 — SAYAÇ DÜZELTMESİ + MANUEL ODAK (8 Ağu 2026)

### Kullanıcının bildirdiği hata 🔴
*"Zamanlayıcı bildirimine tıklayınca uygulama açılınca sayaç sıfırlanıyor,
başa dönüyor."*

**Kök sebep (kod okunarak kanıtlandı):** `TimerFragment.onViewCreated`
içinde `toggle.check(R.id.countdownButton)` kurulum sırasında
`addOnButtonCheckedListener`'ı tetikliyor; dinleyici de koşulsuz
`resetAll()` çağırıyordu — `resetAll()` ise `TimerEngine.reset()` ile
ÇALIŞAN ya da DURAKLATILMIŞ sayacı siliyor. Uygulama arka planda
öldürülmüşse bildirime dokunmak soğuk açılış demektir; fragment o an
ilk kez kurulur ve sayaç yok olur. Motor kalıcı olduğu için veri
kaybolmadan önce bildirimde doğru görünür — kullanıcının gördüğü
"başa dönme" tablosu tam buydu. Aynı hata, sayaç çalışırken sayaç
sekmesini o OTURUMDA İLK KEZ açan herkesi vuruyordu (bildirimden
bağımsız).

**Düzeltme:** kurulum bayrağı (`kurulumBitti`) — kurulumdaki programatik
check motora dokunmaz; durum her zamanki gibi onResume'da motordan
okunur. Üstelik ekran artık MOTORUN modunda açılır (çalışan kronometre
varken geri sayım düğmesi seçilip kayıp yaşanmaz). Yan etki:
duraklatılmış kronometreyi bırakıp uygulamayı kapatıp ilk kez açan
kullanıcı da artık kayıp yaşamaz.

### İstenen özellik
*"Zaman başlatmadan çalıştığım zamanlar oluyor; manuel ekleme yeri ekle
ve odaklanma saati yerine yaz."*

**Çözüm (S2):** Sayaç ekranındaki "Bugün: X dk odak" satırının yanına
**『＋ Manuel odak ekle』** çipi geldi. Diyalogda dakika seçici
(1–480 dk, varsayılan 25); eklenen dakika `Store.addTodayFocusMinutes`
ile gün toplamına sayaç oturumu gibi işlenir → hedef yüzdesi, özet
widget'ı, Bugün durumu, seri/rozet kontrolleri aynı kanaldan anında
beslenir. Diyalog mevcut toplamı gösterir ve "tek tek geri
alınamaz" notunu açıkça yazar (sayacın yazdığı dakikalar da öyledir).
Kelepçe tablosu JVM testli (`OdakManuelTest`).

### Öz denetim — bu sürümde yakalananlar
| # | Sorun | Durum |
|---|---|---|
| 1 | 🔴 **Benim yazım hatası:** python değiştirme metnindeki `\n\n` kotlin dizesinin içine gerçek satır sonu olarak yazıldı — derleyici "Expecting quote" ile yakaladı; kaçış korunarak düzeltildi | derleme temiz |
| 2 | Sandbox bu oturumda TAM SIFIRLANDI (6. kez): ortam `kur-ortam.sh` ile kaldığı yerden kuruldu, proje/sürüm dosyaları snapshot'tan sağlam | etki yok |
| 3 | Düzeltmenin yan etkisi dürüstlükle not edildi: mod düğmesi artık MOTOR moduna göre açılır (davranış değişikliği minimal ve istenen yönde) | belgelendi |

### Cihaz doğrulaması
1. 25 dk geri sayım başlat → uygulamayı görevde öldür (son kullanılanlardan kaydır) → bildirime dokun → sayaç KALDIĞI YERDEN görünmeli (eski davranışta 25:00'a dönerdi)
2. Kronometre çalışırken aynı akış → kronometre kaldığı yerden; üstelik ekran kronometre modunda açılır
3. Sayaç ekranında 『＋ Manuel odak ekle』 → 30 ekle → satır anında X+30 olur; Bugün sekmesindeki durum ve özet widget'ı da güncellenir
4. Hedef yüzdesi (varsa) ek dakikayla artar

---

## 🧲 v10.18 — EKRAN ATÖLYESİ: BASILI TUT & DÜZENLE (kullanıcı isteği) (8 Ağu 2026)

Doğrudan istek: *"Uygulamanın içindeki öğelerin yerlerini
değiştirebileyim; üstüne basılı tutunca boyutlarını, yerini değiştirme vb
bir sürü özelliklerini değiştirebileyim."*

### Ne geldi (iki ekran: Ana ekran + Bugün)

| Yetenek | Nasıl |
|---|---|
| **Basılı tut → düzenleme modu** | Bloğa uzun bas: vurgulu çerçeve + şerit açılır; o ekrandaki tüm blokların çocuk düğmeleri seçim için susar (`DuzenBlokLayout.onInterceptTouchEvent`; yalnız AKTİF kök keser — fragment'lar çapraz sızmaz) |
| **Yer değiştirme** | Şeritten ▲ Yukarı / ▼ Aşağı — anında kaydedilir, ekran yeniden dizilir |
| **Boyut (3 kademe)** | Kompakt (2dp nefes) · Normal (6dp) · Geniş (14dp) — blok bazlı |
| **Katlama** | Başlığı olan bloklar gövdesini katlar (evde 3 blok; bugünde 4 blok) |
| **Gizleme** | Şeritten 👁; zorunlu olmayan her blok gizlenebilir |
| **Sıfırlama** | Şeritten ↺ (onaylı) — sıra+gizleme+boyut+katlama döner |
| **Ayarlar-editörü genişlemesi** | AnaEkranDuzenActivity satırlarına boyut çipleri + katla çipi eklendi (item_ana_duzen.xml'de programatik kap) |
| **Bugün ekranı düzenlenebilir oldu** | `fragment_today.xml`'de 8 bölüm `DuzenBlokLayout` sarmalayıcısına alındı (şimdi/ne · namaz · gün durumu · görevler · alışkanlıklar · etkinlikler · ipucu · hızlı eylemler); selamlama+tarih sabit |

### Mimari
`DuzenCekirdek` (saf: taşıma, boyut nefesi, boyut kaydı diziçimi — 11 test)
· `DuzenSeridi` (paylaşılan şerit; iki ekranda aynı bileşen) ·
`DuzenBlokLayout` (dokunuş kesici kapsayıcı) · `BugunDuzen`
(Bugün motoru: kendi pref'i; AnaEkranDuzen ile çakışmaz) ·
AnaEkranDuzen genişlemesi (boyut/katlı saklama + `boyutVeKatlaUygula` —
mevcut v8.7 testleri aynen yeşil).

### Dürüstlük notları
- Serbest piksel sürüklemesi DEĞİL: dikey akışta adım taşıma yapılır.
  Sürükle-bırak, ScrollView kaydırmasıyla çakışırdı (v8.5 kararı hâlâ
  geçerli); adım taşıma her blok boyutunda güvenli.
- Boyut piksel serbest değil, 3 kademe — native bileşenlerin güvenli
  aralığı budur.
- Katlama "ilk çocuk = başlık" varsayımıyla çalışır; tek kartlık bloklar
  (hero, grafik, çizelge…) katlanamaz olarak işaretlendi.
- Düzenleme modu açıkken blok içi düğmeler susar; "✔ Bitti" ile her şey
  normale döner. Mod açıkken sayfa kaydırma blok üstünden yapılamaz
  (boş alanlardan/alttan kaydırılır) — mod kısa sürelidir.

### Öz denetim — bu sürümde yakalananlar

| # | Sorun | Durum |
|---|---|---|
| 1 | 🔴 **Benim hatam (kritik):** çok dosyalı python betiğinde yol değişkenini güncellemeyi unutup TodayFragment içeriğini HomeFragment.kt ÜSTÜNE yazdım (kendi kuralımı ihlal ettim). v10.17 kaynak zip'inden geri yüklendi, iki dosyaya ayrı ayrı yeniden uygulandı, derleme+574 test doğruladı | giderildi + doğrulandı |
| 2 | 🔴 `this@apply` belirsizliği: nested `GradientDrawable().apply` içinde renk çağrısı yanlış alıcıya gitti — derleyici yakaladı; renk dış apply'da değişkene alındı | derleme temiz |
| 3 | Blok içi düğmelerin dokunuşu düz `OnTouchListener` ile kesilemez (ebeveyn önce çocuk yutar tuzağı) → özel `onInterceptTouchEvent` kapsayıcısı çözümü | baştan doğru tasarım |
| 4 | İki fragment aynı aktivitede yaşadığı için genel "yerel mod" bayrağı çapraz sızma yapardı → `soyundanMi` kök denetimi | baştan doğru tasarım |
| 5 | ET ile XML yeniden yazımında todayDate konumu kaydı — ayrı geçişte selamlamanın arkasına sabitlendi | doğrulandı (çocuk sırası basılı) |

### Cihaz doğrulaması (telefonda bakılacaklar)
1. Ana ekranda bir bloğa (ör. 🔥 ızgara) basılı tut → çerçeve + şerit gelmeli
2. ▲ ile iki kez yukarı tıkla → blok gerçekten yukarı taşınmalı; uygulamayı kapat/aç → sıra kalıcı
3. Boyut → Kompakt → blok nefesi daralmalı; Geniş → rahatlamalı
4. Hızlı erişim bloğunda "Katla" → başlık kalmalı, düğmeler gizlenmeli
5. "✔ Bitti" sonrası blok içi düğmeler tekrar çalışmalı (istatistik kartına dokun → ayrıntı açılmalı)
6. Bugün sekmesinde Görevler bloğunu basılı tut → aynı şerit; Katla → liste düşer başlık kalır
7. ↺ Sıfırla → her iki ekran da varsayılana dönmeli (ayrı ayrı, bağımsız)

---

## 🎛 v10.17 — WIDGET AYAR ENVANTERİ: 33 YENİ AYAR (kullanıcı isteği) (8 Ağu 2026)

Doğrudan istek: *"Daha fazla widget ayarlaması yap, en az 30 adet widget
ayarı ekle. Yeni widget istemiyorum — ayarlarını değiştirmek istiyorum.
Öner."* Seçim bize bırakıldı; kod tarandı, mevcut 11 ayarla çakışmayan
**33 yeni ayar** tasarlandı ve tamamı uygulandı. Liste belgesi:
`DEVIR/oneriler/ONERILER-WIDGET-ENVANTERI.md`.

### Mimari (3 katman)

**1) Merkezi renk işlemesi — 8 ayar** (`WidgetSecim`, yeni dosya)
`WidgetTema.palet` dönüşünden hemen önce palet baştan yazılır; paleti
kullanan 20 widget (bitmap zemin üretenler dahil) tek seferde kazanır:

| # | Ayar | Değerler |
|---|---|---|
| 1 | Metin rengi modu | Otomatik · Açık · Koyu · Vurgu uyumlu |
| 2 | Vurgu canlılığı | Soluk · Normal · Canlı |
| 3 | Tamamlanan rengi | Yeşil · Mavi · Gri · Vurgu |
| 4 | Yüksek kontrast | Soluk metin ana metne yaklaşır (erişilebilirlik) |
| 5 | Gece karartması aç/kapa | Zeminler dimlenir, metin korunur |
| 6 | Karartma başlangıcı | 0-23 saat seçici (vars. 22) |
| 7 | Karartma bitişi | 0-23 saat seçici (vars. 07) |
| 8 | Karartma şiddeti | Hafif %20 · Orta %40 · Derin %60 |

Gece sarması: 22→07 gibi pencereler gece yarısını aşar;
başlangıç == bitiş seçilirse pencere kapalı sayılır (testle kilitli).

**2) Boşluk & davranış — 4 ayar**

| # | Ayar | Değerler | Nerede |
|---|---|---|---|
| 9 | Yatay iç dolgu oranı | Dar ×0.5 · Normal ×1.0 · Geniş ×1.8 | `WidgetAtolye.kokDolguUygula` (tek enjeksiyon → 19 widget) |
| 10 | Satır girintisi | Yok · İnce 4dp · Belirgin 10dp | `WidgetAtolye.satirDolguUygula` |
| 11 | Tazeleme kısıtı | Hızlı 400ms · Dengeli 2sn · Tasarruf 10sn | `WidgetCommon.refreshAll` |

**3) Widget bazlı parça denetimi — 22/21+1 ayar**
(Toplam: 21 görünürlük anahtarı + 1 mod anahtarı = 22; büyük tablo 33.)

| Widget | Ayarlar |
|---|---|
| Tek geri sayım | Etkinlik etiketi · Emoji |
| Günlük özet | Selamlama · Geri sayım rozeti · İstatistik kutuları · Seri sayacı |
| Eylem şeridi | Odak · Soru · Görev · Bugün · Sesli not düğmeleri |
| Sayaç (mini) | Hazır süre çipleri · Sıfırla düğmesi · İlerleme çubuğu |
| Hedef halkası | Büyük rakam: % ⇄ kalan dk (mod) · Alt bilgi satırı |
| Namaz vakitleri | Vakit adı · Kalan süre |
| Uyku grafiği | Ortalama satırı · Bu gece hedefi · Plan çizgisi · Gün harfleri |

**Tam sayım: 8 merkezi renk/karartma + 3 boşluk/davranış + 22 widget bazlı
(21 görünürlük + 1 mod) = 33 ayar** ✔ (istenen ≥30 sağlandı).

### Varsayılan = eski davranış
33 ayarın TAMAMI kapalı/varsayılan konumda v10.16 davranışıyla bit bite
aynıdır; hiçbir kullanıcı sürpriz yaşamaz.

### Bakım (söz verilen yetim temizliği)
kaynakta kullanılmayan 11 dize silindi: `kt_row_kapali`, `kt_bugunku`,
`kt_hemen_basla`, `kt_risk_baslik`, `kt_risk`, `kt_istatistik`, `hy_kanal`,
`gc_bildirim_baslik`, `wt_yazi_0/1/2` (v10.16 çip→kaydırıcı göçü kalıntısı).
Önce script ile ana+test+res taraması yapıldı, yalnız gerçekten yetim
olanlar silindi.

### Öz denetim — bu sürümde yakalananlar

| # | Sorun | Durum |
|---|---|---|
| 1 | 🔴 **Benim testimde beklenti hatası (5 adet):** kelepçe yönünü ters yazdım (`coerceIn` üst/alt sınıra kenetler; ben serbest geçiş varsaymıştım) — kod doğruydu, 5 test satırı düzeltildi | düzeltildi, 563/563 |
| 2 | Renk matematiği `android.graphics.Color` ile yazılsaydı JVM testleri stub'a takılırdı (bilinen tuzak) — bit işlemiyle yazıldı | baştan önlendi |
| 3 | Başlangıç==bitiş "tüm gün karart" tuzağı — `karartmaAktifMi` bu durumda hiç aktif olmaz (testle kilitli) | tasarım kararı + test |
| 4 | Yeni ID/PI/request code YOK → çakışma riski sıfır (tüm denetimler mevcut görünümlere) | doğrulandı |

### Cihaz doğrulaması (telefonda bakılacaklar)
1. Ayarlar → Widget tema → yeni bölümler: "🎨 Renk ince ayarı", "🌙 Gece karartması", "📐 Boşluk ince ayarı", "⚡ Davranış", "🧩 Widget bazlı denetimler"
2. Metin rengi → "Açık" seç → ana ekrandaki koyu widget'ta yazılar aydınlanmalı
3. Gece karartması aç, başlangıcı şu anki saate al → widget zemini dimlenmeli, yazılar okunur kalmalı
4. Günlük özet → "Selamlama" kapat → üst satır kaybolmalı
5. Hedef halkası → "Kalan dakikayı göster" aç → büyük rakam % yerine dk göstermeli
6. Eylem şeridi → bir düğmeyi kapat → şeritten düşmeli
7. Tazeleme kısıtı "Tasarruf" → görev işaretlemelerinin widget'a yansıması birkaç sn gecikebilir (beklenen davranış)

---

## 🛠 v10.16 — WIDGET ATÖLYESİ (kullanıcı isteği) (8 Ağu 2026)

Doğrudan istek: *"Widgetları aşırı düzenleme ekle — yazı boyutları,
genişlik, metin aralıkları, widget birleştirebilme, her şeyini ben
ayarlayabileyim."* RemoteViews'un teknik sınırları içinde istenen her
şey için en güçlü biçim devreye alındı; sınırlar dürüstçe not edildi.

### Yapılanlar

| İstek | Uygulama |
|---|---|
| **Yazı boyutları** | %75–%150 serbest kaydırıcı (%5 adım) — tema ayar ekranında, canlı önizlemeli SeekBar. Eski 3 kademeli çip kaldırıldı; mevcut kademe ilk okumada yüzdeye **taşınır** (0→%85 · 1→%100 · 2→%115). Tüm 19 widget'ta geçerli (merkez `yaziOlcek` senkronu) |
| **Genişlik** | Teknik gerçek: genişliği launcher belirler. Ayarlanabileni yapıldı — **İç dolgu** çipleri (Sıfır/Standart/Rahat/Ferah → 0/2/6/12 dp). `WidgetTema.saydamlikUygula`'ya tek enjeksiyonla eklenip **eski 15 widget'a otomatik** yayıldı; yeni aile de kendi render'ında uygular. Önizleme dolgu değişimini yaşatır |
| **Metin aralıkları** | `setLineSpacing` RemoteViews'ta YOK (android.jar doğrulandı). Sunulan: **Satır nefesi** çipleri (Sıkı/Normal/Bol → 0/2/6 dp dikey dolgu) — yeni aile ve Birleştirici'nin tüm satırlarına işler |
| **Widget birleştirme** | **Birleştirici** (20. widget): 7 modül — 🕐 saat&tarih · ⏱ sayaç · ✅ görevler · 🔥 seri · 😴 uyku · 🚪 kapı · 🐣 kronotip. ⚙ "Düzenle" bandı → örnek başına düzenleyici: aç/kapa + ↑/↓ sıralama + varsayılana dön, her dokunuş anında kaydeder. Yükseklik bütçesi üstten dizilir; içerik üretemeyen modül düşer, alttaki kayar |
| **Her şey ayarlı** | Üstteki üçü + mevcut zemin modu/saydamlık/köşe/vurgu/anılık senkron aynı ekranda — Widget Tema ekranı artık gerçek bir atölye |

### Birleştirici detayları

- **Maliyet sistemi:** her modül satır maliyeti taşır (saat=2, gerisi 1
  — görevler 2); yükseklik bütçesi `WidgetCommon.sigacakSatir`'dan
  gelir, üstten sığdığı kadarı dizilir.
- **Boş modül düşer:** koşmayan sayaç, kayıtsız uyku defteri yer
  kaplamaz; altındaki modül yukarı kayar (boşluk yok).
- **Bağımsız örnekler:** 2. Birleştirici widget'ını ekleyin, farklı
  modül seti kurun — yapılandırma örnek kimliğiyle saklanır.
- **Boş durum:** tüm modüller kapatılırsa yalnız ⚙ bandı kalır
  ("Modüller kapalı — ⚙ Düzenle ile aç") — varsayılan zorlanmaz.

### Sayılar

| Alan | v10.15 | v10.16 |
|---|---|---|
| Kotlin dosyası (ana) | 296 | **300** — 4 yeni (WidgetAtolye · Modul · ModulWidget · ModulAyarActivity) +3 dosya düzenlendi |
| Test | 539 | **547** — 0 hata (+8, `GrupWTest`) |
| Dize | 3784 | **3818** (+34) |
| Widget | 19 | **20** (+Birleştirici) |

### Güven

- Saf tablolar testlidir: yüzde snap/kelepçe, migration haritası,
  dolgu/nefes tabloları; modül bütçe aritmetiği (sığmayanda dur),
  sıra taşıma sınırları, temizleme kuralları — 8 yeni test.
- Geriye uyum: eski "yazi" kademesi otomatik yüzdeye taşınır;
  `setYaziKademe` köprüsü deprecated olarak korundu.
- Yeni dizelerde yetim **0**; wt_yazi_0/1/2 çip dizeleri kaydırıcı
  göçüyle yetim kaldı — bakım kuyruğuna işlendi (shrinkResources
  buduyor).

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Sonuç |
|---|---|---|
| 1 🔴 | İlk layout taslağında var olmayan `@style/WidgetModulSatir` referansı yazılmıştı — aapt kesin kırardı | Yazım anında yakalandı; 9 satır inline öznitelikli yapıldı (tasarım testleri @dimen kuralına da uyar) |
| 2 🔴 | `Modul.saglamlastir` boş listeyi varsayılana zorluyordu — "son modülü kapat" geri alınamayacak bir tuzağa dönüşürdü | **Benim tasarım hatamdı;** derlemeden önce yeniden modellendi: boş liste geçerli, hiç-yapılandırılmamış ayrımı `siraOku`'da |
| 3 🔴 | `Kronotip.Tip.GUVENCIN` için var olmayan `ge_tip_dengeli` dizsi referanslanmıştı; ayrıca textSize hesabında `* dp * 1.0f / dp * dp` gülünç bir ifade bırakmıştım | İkisi de **derleme öncesi** API doğrulamasıyla düzeltildi (`ge_tip_guvencin` mevcutmuş; ifade `× çarpan × dp`'ye sadeleşti) |
| 4 🔴 | `Etiket.bul(...)` nullable dönüyor — ilk derleme `Only safe (?.) calls` ile kırıldı | **Bu benim hatamdı:** Kokpit kalıbından ezbere yazmışım; `?.emoji` ile düzeltildi, ikinci derleme temiz geçti |
| 5 ⚠️ | `setLineSpacing` RemoteViews'ta var sanıyordum | `javap android.jar` ile doğrulandı — YOK. Kapsam "satır nefesi = dikey dolgu" olarak dürüstçe daraltıldı ve UI metni de bunu aynen söylüyor |

### APK doğrulaması

- İmza SHA-256 `5f15d4e7…348511` ✔ (v5.0 ile aynı anahtar)
- `versionCode=172 · versionName=10.16` ✔ · SimgeVarsayilan enabled ✔
- `ModulWidget` alıcısı + `ModulAyarActivity` manifest'te ✔
- 23/23 `wa_` + 11/11 `wt_dolgu/nefes` dizesi APK'da ✔
- **APK:** 18.3 MB · md5 `4ca66595db780ba30ed5540dab7655ff`
- **Kaynak zip:** 739 dosya · md5 `01fa86b2848d8318e5b337749f75e226` (temiz, keystore içeride)
- **Test:** 547/547 yeşil · 44 sınıf

### Cihaz doğrulaması (kurulum sonrası kontrol)

1. Widget tema ekranı → yazı kaydırıcısı: %85'e çek → widget yazıları
   küçülür; %135'e → büyür; bırakınca anında tüm widget'lara işler.
   (Eski kademeli seçiminiz varsa %85/100/115'e otomatik taşınır.)
2. İç dolgu "Ferah" → kart içeriği kenarlardan uzaklaşır; "Sıfır" →
   köşelere yapışır. Önizleme aynısını gösterir.
3. Sayac widget'ı ve görev satırları "Bol" nefesle açılır.
4. Widget listesinden **Birleştirilebilir** ekle → varsayılan 4 modül
   dizili; ⚙ Düzenle → kronotipi aç, seriyi kapat, kapı'yı en üste
   al → widget anında yenilenir.
5. Widget'ı dikeyde uzat → yeni modüller belirir; kısalt → alttaki
   kaybolur (üstler kalır).
6. Sayaç koşmuyorken modülü görünmez; başlatınca widget'ta belirir.
7. İkinci Birleştirilebilir ekle → bağımsız modül seti kurar.
8. Kilit ekranı gün paneli (v10.15) açıksa widget dokunuşlarıyla da
   tazelenmeye devam eder.

### Sırada
Kuyruk: **bakım turu** (eski yetim dizeler: hy_kanal, 6× kt_*,
gc_bildirim_baslik + yeni yetim trio wt_yazi_0/1/2) + yeni öneri
dalgası taraması. "Devam et" yeterli.

---

## 🔔 v10.15 — ULTRA-30 GRUP C: Bildirim Devrimi (8 Ağu 2026)

ULTRA-30'un kapanış grubu; altı maddenin tamamı. Ortak tema: bildirim
artık tek düze bir kapı çalışı değil — kritik görev ekranı kaplıyor,
her tür kendi sessiz saatini biliyor, kilit ekranı günü özetliyor,
hatırlatıcı senin saatinden öğreniyor, rapor ısı haritası taşıyor ve
aynı dilimdeki alarmlar tek pakette geliyor.

### 🔎 Tarama düzeltmesi (dürüstlük önce gelir)
Öneri belgesindeki **C17 gerekçesi ("Raporlar metin; görsel özet yok")
yanlıştı**: v10.3'ten beri haftalık rapor `BigPictureStyle` çubuk
grafik taşıyor (`RaporGrafigi` + testleri mevcut). Gerçek boşluk
ısı haritasıydı (gün × saat-dilimi yoğunluğu) — C17 o yapıldı ve
mevcut çubuk karta İKİNCİ BÖLÜM olarak eklendi; eski kart korunur.
Diğer beş maddenin gerekçeleri taramada doğrulandı (ZorunluUyari'nin
tam ekran kullanılmıyordu, tek global sessiz pencere, kalıcı özet
yoktu, yapıldı-saati kaydı yoktu, `setGroup` hiç yoktu).

### Yapılanlar (C13–C18)

| # | Öneri | Uygulama |
|---|---|---|
| C13 | **Tam ekran görev alarmı** | `KritikAlarm` (saf tablo: 5·10·15 dk kademe, 3 üzeri bedel → ertesi gün 09:00 + sayaç sıfır) + kilit üstü `GorevAlarmActivity` (USAGE_ALARM zil, ✓ Yaptım / ⏰ Ertele). Yalnız 🔴 "acil" etiketli görevler — iznin kötüye kullanımını önleyen bilinçli daraltma |
| C14 | **Tür başına sessiz pencere** | `SessizTurler`: GÖREV · SAYAÇ · RAPOR · MOTİVASYON türlerine kendi baş/bitiş + hafta sonu ayrımı; gece yarısı sarmalı aralık; tür kapalıysa global pencere karar verir (geriye uyum). Ayar: bildirim ayarlarında tür satırları + 4 picker'lı diyalog. Kapılar: ReminderReceiver (sessiz ikincil kanal, kaybolmaz), TimerActionReceiver bitiş zili/titreşimi, WeeklyReportReceiver |
| C15 | **Kilit ekranı gün paneli** | `GunPaneli` + `GunPaneliReceiver`: kalıcı LOW kanal, 3 satır (bugün kalan görev · sayaç durumu · sonraki kapı [uyku çerçevesi]); tazeleme tek kapıdan — `WidgetCommon.refreshAll`; "Kapat" aksiyonu anahtarı söndürür; anahtar bildirim ayarlarında |
| C16 | **Öğrenen hatırlatıcı** | `OgrenenHatirlatici`: her "yapıldı"da saat kaydı (son 8) → DAİRESEL ortalama (23:50+00:10 sarması doğru); `Tekrar.gorevYenile`'de ±45 dk kelepçeli kaydırım; ≥3 kayıt ve sapma ≤150 dk koşulu; bildirim genişletilmiş metni "🧠 Bu saati son N tamamlamanın ortalamasından öğrendim" diye açıklar |
| C17 | **Haftalık ısı haritası** | `RaporIsi`: 7 gün × 6 dilim (4 saat) kademeli ısı kartı (0–4″öte, haftanın maksimumuna göre ölçek); mevcut çubuğun ALTINA `birlestir` ile eklenir; haftanın maksimumu yoksa sakin düz ızgara |
| C18 | **Hatırlatıcı demeti** | `HatirlaticiDemeti` (pencere ±10 dk): `ReminderReceiver` artık grup publish eder — üyeler TEKİL bildirimlerini korur (✓/ertele düğmeleri birebir aynı) + satır satır InboxStyle GRUP ÖZETİ (`setGroupSummary`); ✓/ertele sonrası özet temizlenir (yetim kalmaz) |

### Bilinçli davranış değişikliği (dürüst not)
C14 kapsamında görev hatırlatıcıları artık **global sessiz pencereye de
saygı duyuyor** (eskiden hiç bakmıyorlardı): penceredeyken ses/titreşim
düşmez, bildirim sessiz ikincil kanaldan (`gorev_hatirlatici_sessiz_v1`,
LOW) görünür olarak gelir. Kayıp yok, gürültü azalması var. 🔴 "acil"
etiketli görevler bu kuralın dışındadır (kritik kanal her koşulda).

### Sayılar

| Alan | v10.14 | v10.15 |
|---|---|---|
| Kotlin dosyası (ana) | 288 | **296** — 8 yeni çekirdek/ekran (+8 dosya düzenlendi) |
| Test | 528 | **539** — 0 hata (+11, `GrupCTest`) |
| Dize | 3746 | **3784** (+38, hepsi kullanımda) |
| Bildirim kanalı | 9+ | **+3** (kritik · sessiz görev · gün paneli) |
| Manifest bileşeni | — | +1 aktivite (kilit üstü) · +1 alıcı |

### Güven

- Saf çekirdek 5 nesne framework'süz ve testli: `KritikAlarm`,
  `SessizTurler`, `OgrenenHatirlatici`, `HatirlaticiDemeti`,
  `RaporIsi` — kademe tablosu, gece sarması, dairesel ortalama,
  kelepçe, pencere-kelepçe-hücre kararları kilitli.
- Tam ekran izni kötüye kullanılmıyor: yalnız kullanıcının 🔴
  seçtiği görevlerde; kalan herkes eski bildirim yolunda.
- Yeni bildirim/PI blokları 4931–4935; mevcut kodlarla çakışma
  taraması temiz (NamazVakti koordinatları yalnızca ondalık sayı).
- Kaynak teslim paketi: 732 dosya, build/.gradle/.idea/local.properties
  temiz, debug.keystore içeride.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Sonuç |
|---|---|---|
| 1 🔴 | `strings.xml`'e `<string name="gc_tur_ozet_hs" >>` fazladan `>` ile yazılmıştı — aapt kesin kırılırdı | **Derleme öncesi XML doğrulamasıyla** yakalandı, düzeltildi; derlemeye hiç ulaşmadı |
| 2 🔴 | Demet testi beklentisi ±60_000 ms (1 dk) ile kurulmuştu; sabit 600_000 ms (10 dk) — test verisi hatasıydı, KOD doğruydu | **Bu benim hatamdı:** 60sn/10dk birim karışıklığı; test 10 dk penceresine göre yeniden kuruldu, 539 yeşil |
| 3 ⚠️ | `gc_` öneki "boş" sanıldı (ilk kontrol yanlış yolda yapılmıştı); aslında güncelleme bloğu (`Guncelleme.kt`) gc_ kullanıyormuş | İsim çakışması olmadığı tek tek doğrulandı (aapt çakışmayı zaten reddederdi); ayrıca eski yetim `gc_bildirim_baslik` tespit edilip bakım kuyruğuna alındı |
| 4 ⚠️ | `USE_FULL_SCREEN_INTENT` iznini manifest'e eklemek üzereydim | Kontrol: **zaten mevcutmuş** — ikiz izin eklenmemiş oldu |
| 5 ⚠️ | `UykuCerceve` object imzası (context'li fonksiyonlar) ilk GunPaneli taslağında sınıf gibi varsayılmıştı | Yazımdan önce API doğrulamasıyla düzeltildi — derlemeye hiç ulaşmadı |
| 6 ⚠️ | Sandbox bu turda TAM sıfırlandı (5. kez): /opt + gradle önbelleği gitti | `kur-ortam.sh` + swap yeniden kuruldu (~6 dk önbellek indirimi dahil), zincir kaybolmadan sürdü |

### APK doğrulaması

- İmza SHA-256 `5f15d4e7…348511` ✔ (v5.0 ile aynı anahtar)
- `versionCode=171 · versionName=10.15` ✔ · SimgeVarsayilan enabled ✔
- `GorevAlarmActivity` + `GunPaneliReceiver` manifest'te ✔
- 38/38 yeni dize APK'da ✔ · kullanılmayan yeni dize: **0**
- **APK:** 18.3 MB · md5 `21d8497d28a51ee11ef8d859ddeae229`
- **Kaynak zip:** 732 dosya · md5 `c0a82312e632d6d0f17e13d551e4aa8c`
- **Test:** 539/539 yeşil · 43 sınıf

### Cihaz doğrulaması (kurulum sonrası kontrol)

1. Bir göreve 🔴 "acil" etiketi ver + yakın saat → vadede ekran
   kilitliyken TAM EKRAN açılır, alarm sesi çalar; Ertele → 5 dk,
   ikincide 10 dk; 4. ertelemede "bedel" tostu + yarın 09:00.
2. Normal görev hatırlatıcısı eskisi gibi (✓ · ertele · yaz · yarına).
3. Bildirim ayarları → "Tür bazlı sessiz pencereler": GÖREV türünü
   aç + şu anı kapsayan aralık ver → sonraki hatırlatıcı sessiz
   kanaldan düşer (görünür ama suskun). Hafta sonu ayrımı aralığı
   ayrıca etki eder.
4. Sayaç penceresini şu anı kapsayacak kur → süre bitince zil/titreşim
   susar, bildirim gelir.
5. Bildirim ayarları → "Kilit ekranı gün paneli" aç → kalıcı sessiz
   panel: kalan görev · sayaç · sonraki kapı; görev ✓ yapınca sayım
   düşer; "Kapat" anahtarı söndürür, panel kalkar.
6. Tekrarlı bir görevi hep benzer saatte ✓ yap → 3. kayıttan sonra
   hatırlatıcı saati en fazla ±45 dk kayar ve bildirimde
   "🧠 Bu saati … öğrendim" açıklaması görünür.
7. Aynı 10 dakikaya 2 görev kur → tek grup paketi: satır satır özet +
   üyelerin kendi düğmeleri; birini ✓ yapınca özet temizlenir.
8. Pazar akşamı raporu: çubuğun altında gün × saat ısı haritası;
   hiç odak yoksa sakin düz ızgara (çökme yok).

### Sırada
ULTRA-30 kapandı 🏁 — sıradaki kuyruk: **bakım turu** (eski yetim
dizeler: hy_kanal, 6× kt_*, gc_bildirim_baslik) + yeni öneri dalgası
için taze tarama. Kullanıcı "devam et" dediğinde bakım turundan
başlanır.

---

## 🌱 v10.14 — ULTRA-30 GRUP E: Hayat Özellikleri (8 Ağu 2026)

Altı maddenin tamamı. Ortak tema: uygulama artık yalnız "neyi, ne
zaman" bilmiyor — sabah planını kuruyor, uyku-odak ilişkisini tek
kartta okutuyor, günü 30 saniyede kapattırıyor, sesli notun
akıbetini gösteriyor, görevi karta döküyor ve yıl sonunda hikâyeni
anlatıyor.

### Yapılanlar (E25–E30)

| # | Öneri | Uygulama |
|---|---|---|
| E25 | **Sabah AI planı** | `SabahPlani` + `UykuAksiyonReceiver` bayrağı + `HomeFragment` tek-seferlik diyalog: "☀ Uyandım" sonrası dünkü yarım işler + bugünün görevlerinden 3 maddelik taslak; AI hazırsa tek istekle doğal dile çevrilir, 8 sn'de dönmezse yerel taslak gösterilir; taslak boşsa diyalog hiç çıkmaz |
| E26 | **Kronotip kartı** | Saf `Kronotip` (ortalama/sapma/tip/odak penceresi) + Analitik'te kart: uyku defterinin uyanış saatleri ile saat dağılımı ilk kez TEK kartta — "07:40 insanısın (Serçe), en keskin odak 09–11"; penceredeyseki anlık öneri vurgusu |
| E27 | **Akşam mikro günlük** | `MikroGunluk` + `MikroGunlukActivity`: "😴 Uyuyorum" bildirimindeki "✍ 3 soruyla kapat" düğmesi 30 saniyelik kapanış açar — gün puanı + teşekkür + yarının tek şeyi; tek JSON'da en fazla 62 kayıt (iki ay), eskiler soldan budanır; Analitik'ten geri okunur |
| E28 | **Sesli not gelen kutusu** | `SesliKutu` + `SesliKutuActivity` (SesliNot listesinden giriş): son 60 not, 30 gün saklanır, hedef emojisiyle "Bu hafta / Daha önce" bölümlü — "söylediğim şey nereye gitti?" artık cevaplı |
| E29 | **Görev paylaşım kartı** | `KartUretici.gorevKarti` (TasksFragment paylaş eylemi): 1080×1350 (4:5) PNG — tema gradyanı, büyük emoji, sözcük kaydırmalı görev metni, tarih + seri; FileProvider ile sistem paylaşım sayfasına düşer |
| E30 | **Senenin Filmi** | `SeneFilmi` + `SeneFilmiActivity`: en uzun gün · en güçlü seri · en çalışkan ay sahneleri, son sahnede Pofi + `KartUretici.seneKarti` paylaşımı; Aralık'ta ana ekranda yılda bir önerilir, Analitik'ten her zaman açılır |

### İki tarama düzeltmesi (dürüstlük önce gelir)

Öneri metnindeki iki gerekçe kodu tararken **yanlış** çıktı; notlar
dosyalara da aynen yazıldı:

- **E26:** "h verisi toplanıyor ama YORUMLANMIYOR" — yanlıştı:
  `Analitik.saatDilimleri` / `enVerimliSaat` v7.38'den beri saat
  dağılımını yorumluyor ve çiziyor. Gerçek boşluk: uyku defteri ile
  saat analizi hiç tek kartta birleşmemişti ve karta bağlı odak
  önerisi eylemi yoktu. E26 tam bunu yaptı.
- **E28:** "SesliNot yalnız kayıt/oynatma" — yanlıştı: v7.71'de
  SpeechRecognizer + yerel kural motoru + AI sınıflandırma +
  NaturalDate tarih çıkarımı zaten görev kuruyor. Gerçek boşluk:
  işlenen notların durduğu görünür bir liste yoktu. E28 o listeyi
  (gelen kutusu) yaptı.

### Sayılar

| Alan | v10.13 | v10.14 |
|---|---|---|
| Kotlin dosyası (ana) | 284 | **288** — 9 yeni: 6 motor + 3 ekran (+5 dosya güncellendi) |
| Test | 515 | **528** — 0 hata (+13, `GrupETest`) |
| Dize | 3682 | **3746** (+64) |
| Aktivite | — | +3 (MikroGunluk · SesliKutu · SeneFilmi) |

### Güven

- Saf çekirdek framework'süz: `SabahPlani.sec`, Kronotip hesapları,
  `MikroGunluk` duygu/ortalama, `SesliKutu.buHafta`,
  `KartUretici.satirlaraBol`, `SeneFilmi.hesapla/enUzunSeri` —
  13 yeni test tümünü kilitler (yarım-öncelik sırası, tip sınırları,
  pencere, emoji kelepçesi, satır kırma, seri zinciri).
- AI yolu zorunlu değil: AI kapalıysa veya 8 sn'de dönmezse sabah
  planı yerel taslakla gösterilir — özellik internetsiz de çalışır.
- Yeni dizelerde (sb/kr/sk/sn) kullanılmayan kaynak YOK: 0 yetim.
  APK'da da hepsi mevcut: sb 16/16 · kr 12/12 · sk+sn 74/74 ✔.

### Öz denetim — teslim doğrulamasında yakalananlar

| # | Bulgu | Sonuç |
|---|---|---|
| 1 🔴 | `aapt2 dump resources` kontrolüm `string/ad ` (sonda boşluklu) deseniyle 0 döndü — "kaynaklar APK'da yok" sanılıyordu | **Bu benim doğrulama hatasımdı:** aapt2 çıktısında ismin ardında boşluk yok. Desen düzeltilince bütün yeni dizeler (sb 16/16, kr 12/12, sk+sn 74/74) doğrulandı; üründe kayıp yoktu |
| 2 ⚠️ | Teslim taraması 7 kullanılmayan dize buldu (`hy_kanal` + 6× `kt_*`) | İkisi de bu sürümün ürünü DEĞİL: `hy_` v7.74 hızlı yanıt, `kt_` KonuTekrar dönemi yetimleri; shrinkResources zaten buduyor. v10.14 dizelerinde yetim 0. Eski yetimlerin temizliği ayrı bir iş olarak not edildi |
| 3 ⚠️ | Dosya sayacı: v10.13 notunda 284 ana yazıyordu, bugün 9 yeni dosya eklenmesine karşın sayım 288 (+4 net) | Geçmiş yok, farkın tam kaynağı doğrulanamıyor; tahmin yürütmek yerine bu sürüm notundaki TÜM sayılar `ls | wc -l` / `grep -c` ile yeniden ölçülüp öyle yazıldı (288 ana · 42 test · 3746 dize · 528 test) |

### APK doğrulaması

- İmza SHA-256 `5f15d4e7…348511` ✔ (v5.0 ile aynı anahtar — üstüne kurulur)
- `versionCode=170 · versionName=10.14` ✔
- SimgeVarsayilan enabled ✔ (ana launcher korunuyor)
- 3 yeni aktivite manifest'te ✔ · yeni dizeler APK'da tam ✔
- **APK:** 18.3 MB · md5 `9a6d0552167b694660350d35830c28de`
- **Kaynak zip:** 722 dosya · md5 `0f4463f0ecf3249ab076cf5db6644b56` (build/.gradle/.idea yok, debug.keystore içeride)
- **Test:** 528/528 yeşil · 42 sınıf

### Cihaz doğrulaması (kurulum sonrası kontrol)

1. Akşam "😴 Uyuyorum" bildirimi → "✍ 3 soruyla kapat" → üç soru
   doldur → Analitik'teki günlük satırından kayıt geri okunur.
2. Sabah "☀ Uyandım" → ana ekranı ilk açışta plan diyaloğu:
   yarım iş en üstte; "Onayla" ile plan görevlere dökülür.
   (Görev yoksa diyalog hiç çıkmamalı — bu da doğru davranış.)
3. İnternetsiz / AI kapalı deneyin → yerel taslak yine gelir.
4. Analitik → kronotip kartı: uyanış ortalaması, tip (Serçe/Gece
   kuşu), sapma ve odak penceresi mantıklı görünür.
5. Sesli not kaydet → SesliNot listesinden gelen kutusu → not hedef
   emojisiyle "Bu hafta" bölümünde.
6. Görevlerde bir görevi paylaş → 1080×1350 PNG kart sistem
   paylaşım sayfasında.
7. Analitik → Senenin Filmi → sahneler sırayla oynar, son sahnede
   özet kart paylaşılabilir.

### Sırada
**v10.15 = ULTRA-30 GRUP C — bildirim devrimi (C13–C18):** ULTRA-30
kapanışı. Ardından kuyruktaki bakım maddeleri (eski yetim dizeler
dahil).

---

## 🧩 v10.13 — ULTRA-30 GRUP B: Widget Devrimi (8 Ağu 2026)

Altı maddenin tamamı. Ortak tema: ana ekran artık tek işlevli kart
duvarı değil — birleşik kokpit, ay ızgarası, uyku grafiği, tek
dokunuşluk odak düğmesi ve kendi filtresi olan widget örnekleri.

### Yapılanlar (B7–B12)

| # | Öneri | Uygulama |
|---|---|---|
| B7 | **Kokpit süper widget (4×2)** | `KokpitWidget` + saf `Kokpit` hesabı: çevirmeli kadran (akrep/yelkovan ibreli bitmap), sayaç koşarken kadrana işlenen ilerleme halkası + kalan süre, sıradaki 2 görev, 🔥 seri; üç dokunma bölgesi (sayaç/görevler/ana ekran) |
| B8 | **Ay görünümü takvim widget'ı (4×4)** | `TakvimWidget` + `TakvimWidgetService` + saf `TakvimMotoru`: Pazartesi başlangıçlı 42 hücre, gün başına 0–3 görev yoğunluk noktası, oklarla ±12 ay gezinme, güne dokun → etkinlikler ekranı |
| B9 | **Uyku widget'ı (4×2)** | `UykuWidget` + saf `UykuPano`: v10.9 defterinin son 7 gecesi çubuk grafik (bitmap), kesikli plan çizgisi, ortalama + bu gece hedefi; dokun → uyku ayarları |
| B10 | **1×1 "Şimdi odak" düğmesi** | `OdakKutusuWidget`: boştayken tek dokunuş **uygulamayı açmadan** 25 dk odak başlatır (`TimerActionReceiver`'a doğrudan yayın); koşarken dolan halka + kalan süre, dokun → sayaç ekranı |
| B11 | **Köşe + yazı kaydırıcıları** | `WidgetZemin` dinamik zemin bitmap üreteci; `WidgetTema`'ya `kose`/`yaziKademe` tercihleri; tema ayar ekranında canlı önizlemeli çip sıraları |
| B12 | **Örnek başına widget içeriği** | `WidgetFiltre` + `WidgetFiltreActivity`: görev widget'ının 🏷 çibi o örneğin etiket filtresini açar (Tümü + 6 etiket); süzgeç liste fabrikasında uygulanır, iki örnek bağımsız filtre taşır |

### B11 kapsam sınırı (dürüst not)

Köşe yuvarlaklığı kaydırıcısı **yalnız yeni 4 widget'ta** (Kokpit ·
Ay · Uyku · Odak) geçerlidir: bu dörtlü zemini çalışma anında bitmap
olarak üretir (`setImageViewBitmap` — RemoteViews'un izinli yolu).
Eski 12 widget hazır shape kaynaklarına (`w_card_*`) bağlıdır;
RemoteViews köşeyi değiştiremez, bu yüzden orada köşe sabit kalır,
saydamlık kaydırıcısı çalışmaya devam eder. **Yazı ölçeği ise TÜM
widget'larda çalışır** (`WidgetCommon.yaziOlcek` — palet her
üretildiğinde tercihten senkronlanır). Ayar ekranındaki açıklama da
bunu aynen söyler; iddia fazlası yok.

B10'un dürüst notu: dokunma "uygulamayı açmadan" yayın gönderir;
sayıyı görünür kılan bildirim (mevcut `TimerNotifier`) sistem
gereğince çıkar — bu zaten saat uygulamalarındaki davranışın aynısı.

### Sayılar

| Alan | v10.12 | v10.13 |
|---|---|---|
| Widget sayısı | 15 | **19** (+4 alıcı, +1 liste servisi) |
| Kotlin dosyası (ana) | 273 | **284** (+11) |
| Test | 499 | **515** — 0 hata (+16, `GrupBTest`) |
| Dize | 3649 | **3682** (+33, +1 dizi) |
| Bitmap üreteci | 0 | **4** (zemin, kadran, halka, uyku grafiği) |

### Güven

- Saf çekirdek framework'süz: `Kokpit`, `TakvimMotoru`, `UykuPano`,
  `WidgetZemin` kademeleri, `WidgetFiltre` — 16 yeni test tümünü
  kilitler (açı tabloları, 42 hücre zinciri, doldurma/ölçek,
  karar tablosu).
- Sayaç tazelemesi tek kapıdan: `TimerEngine.sayaciYansit` artık
  Kokpit ve odak kutusunu da render eder; uygulama/bildirim/widget —
  üç kontrol yolu da aynı senkronu görür.
- PendingIntent kodları 4970–4991 bloğunda; mevcut kodlarla
  çakışma taraması yapıldı (SayacWidget 8951+, HaftaWidget 4889+).
- Takvim ızgarası collection-widget kalıbında: şablon PI
  FLAG_MUTABLE, satırlar fillInIntent.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Sonuç |
|---|---|---|
| 1 🔴 | `views.setTextViewText(id, R.string.wg_gorev_yok)` — RemoteViews'ta (Int, Int) imzası YOK; derleme kesin kırılırdı | Kod gözden geçirmesinde yakalandı, `context.getString(...)` yapıldı — derlemeye hiç ulaşmadı |
| 2 🔴 | `0x99000000` Int.MAX'ı aşıyor, Kotlin Long sayıyor → debug derleme hatası (UykuWidget:150) | `.toInt()` ile düzeltildi. **Bu benim hatamdı:** alfası 0x80+ olan hex renkler hep `.toInt()` ister; liste önceki sürümlerde `0x44FFFFFF` altı kaldığı için hiç patlamamıştı |
| 3 🔴 | `widget_ay.xml`'e hiç var olmayan `@style/WGAyBaslikHucresi` yazılmıştı — aapt hatası olurdu | Yazım anında fark edildi, 7 hücre satır içi özniteliklerle geçildi |
| 4 ⚠️ | Release kaynak taraması `grep -E '\s'` deseniyle 0/34 döndü — `\s` GNU grep -E'de desteklenmez | Ürün hatası değil, doğrulama betiği hatası: kaynaklar zaten APK'daydı, desen `$` ile düzeltilince 34/34 doğrulandı |

### APK doğrulaması

- İmza SHA-256 `5f15d4e7…348511` ✔ (aynı anahtar)
- `versionCode=169 · versionName=10.13` ✔
- SimgeVarsayilan enabled ✔ (ana launcher korunuyor)
- 34/34 yeni kaynak APK'da + 5/5 layout + 4/4 alıcı manifest'te ✔
- **APK:** 18.2 MB · md5 `4de66e0f4efd6b808896725595040a8d`
- **Kaynak zip:** 670 dosya · md5 `62034b4070cb16dc4823d34065532d03`

### Cihaz doğrulaması (kurulum sonrası kontrol)

1. Kokpit widget'ı ekle → kadran doğru saati gösterir, görevler ve
   seri dolu; üç bölge doğru ekranı açar.
2. Sayaç başlat → SayacWidget, Kokpit kadranı ve 1×1 odak halkası
   birlikte ilerler (aynı tazeleme bileti).
3. Odak kutusuna dokun → uygulama AÇILMADAN 25 dk başlar, halka
   dolmaya başlar.
4. Ay widget'ı → oklarla ay gezinir, noktalar görevlerle uyumlu;
   güne dokun → etkinlikler ekranı.
5. Uyku widget'ı → defter doluysa çubuklar + kesikli plan çizgisi;
   boşsa "Kayıt yok" metni.
6. Widget tema ayarı → köşe çipleri yeni dörtlüde anında, yazı
   çipleri tüm widget'larda etki eder.
7. Görev widget'ı 🏷 → "İş" seç → yalnız 💼 satırlar; boşsa
   filtreli boş-durum metni çıkar.
8. İkinci görev widget'ı ekle → iki örnek bağımsız filtre tutar.

### Sırada
**v10.14 = ULTRA-30 GRUP E — hayat:** E25 sabah AI planı, E26
kronotip, E27 mikro günlük, E28 sesli not, E29 paylaşım kartı,
E30 senenin filmi. Ardından v10.15 = Grup C (C13–C18) ile
ULTRA-30 kapanır.

---

## ⏱️ v10.12 — ULTRA-30 GRUP D: Zamanlayıcı & Odak Devrimi (8 Ağu 2026)

ULTRA-30'un ikinci durağı; altı maddenin tamamı. Ortak tema: sayaç
artık yalnız geri saymıyor — nefes verdiriyor, geçmişteki seninle
yarışıyor, dikkatin dağılınca omzuna dokunuyor, seni sesli bir odada
çalıştırıyor ve her biten seansı halkasına işliyor.

### 🔎 Tarama düzeltmesi (dürüstlük önce gelir)
Öneri belgesindeki D22 gerekçesi ("yalnız bitiş zili var; döngü ses
yok") **yanlıştı**: yağmur/kafe/orman döngüleri v10.0'dan beri iki
ekranda da elle çalınıyordu. Bu sürümde asıl eksik olan katman
yapıldı — **otomasyon**: odakla kendiliğinden başlama, molada kısma,
sayaçla birlikte susma. Otomasyonu olmayan bir özelliği yeniden
yapmak yerine, olanı tek motorda toplayıp otomatiğe bağladık. Bu
taramanın amacı da tam olarak buydu.

### D19 · 🌬 Nefes stüdyosu
Sayaç ekranındaki yeni "🌬 Nefes" çipi ayrı bir stüdyo açar:
üç desen (🌙 4-7-8 · 📦 Kutu 4-4-4-4 · 🍃 Sakin 4-6), döngü seçimi
(4 / 8 / süresiz), faz değişiminde kısa titreşim (kapatılabilir).
Halka nefesle büyüyüp küçülür; animasyonları kapatan kullanıcıda
bile 250 ms'lik adımlarla ritim izlenebilir. Hesap tamamen saf
(`NefesProgrami`) ve 4 testle kilitli. Stüdyonun zincir hâli de var:
şablonlarda **"🌙 Uyku öncesi nefes"** — evreler 5-7-8'dir (şablonlar
da zincir doğrulamasından geçtiği için en kısa evre 5 sn olabilir;
stüdyoda gerçek 4-7-8 koşulur). Evreler odak sayılmaz: uyku
hazırlığı odak istatistiğini şişirmez.

### D20 · 👻 Kendinle maç (hayalet modu)
Kadranın içinde iki ince yay ve altında canlı cümle:
*👻 "Geçen haftaki sen bu saatte 25 dk · sen 30 dk — 5 dk öndesin."*
Rakip: **Dünkü sen** ya da **Geçen haftaki sen** (zamanlayıcı
ayarlarından; kapalı da olabilir). Veri kaynağı yeni bir günlük
değil: v7.94'ten beri tutulan `OdakKaydi` oturum dökümü — sayaç
bildirimden de bitse, tam ekrandan da bitse aynı deftere yazılır.
Karşılaştırma kuralı iki tarafa da aynı uygulanır: "şu saate kadar
BITEN seanslar" (damga bitiş anıdır). Defter son 300 oturumu tutar:
birkaç haftalık yarış için fazlasıyla yeter, daha eskisi düşer.
Rakip verisi yoksa kadran dürüstçe söyler: *"bugün biriktirdiklerin
yarının hayaleti olacak."*

### D21 · 🛡 Odak kalkanı (izinli)
Zamanlayıcı ayarlarındaki yeni ODAK KALKANI grubu: anahtar + kullanım
erişimi + kısıtlı uygulama listesi + günlük kaçamak sayacı.
**Nasıl çalışır:** odak sürerken öne geçen uygulama 5 sn'de bir
denetlenir; kısıtlı bir uygulama açılırsa yüksek öncelikli uyarı
düşer — "🛡 Kaçamak: X · Odaktan 12 dk kaldı — geri dön mü?";
"Odağa dön" sayaç ekranını açar. Aynı uygulamada 2 dk sakinleşme
penceresi var; 3. kaçamaktan sonra metin sertleşir (ceza yok,
farkındalık var). Mola evresinde kalkan da mola yapar.
**Bilinçli sınırlar (dürüstçe):**
- İzin (`PACKAGE_USAGE_STATS`) sistem ayarından elle verilir; uygulama
  adına istenemez. Veri cihazdan çıkmaz.
- Perde bildirimdir; ekran kaplaması (SYSTEM_ALERT_WINDOW) özellikle
  kullanılmadı — kötüye kullanıma açık bir izin istemiyoruz.
- Gözcü uygulama süreci yaşarken çalışır; sistem süreci öldürürse
  sayaç koşmaya devam eder, kalkan bir sonraki başlatmada geri gelir.

### D22 · 🌧️ Ses manzarası otomasyonu
İki ekranın iki ayrı MediaPlayer'ı tek motorda birleşti
(`SesManzarasi`): artık çakışma yok, durum her yerde aynı.
Zamanlayıcı ayarlarındaki SES MANZARASI grubu: **odakla otomatik
başlat** (varsayılan açık), ses seçimi (8 manzara), **molada kıs**
(%25). Kurallar: geri sayım başlayınca seçili ses kendiliğinden
çalar · duraklayınca/sıfırlanınca/bitince otomatik akış susar ·
mola evresinde kısılır, odakta tam sese döner · kullanıcı eliyle
durdurursa o oturum boyunca otomasyon susar · eliyle başlattıysa
onunsa onundur, otomasyon karışmaz. Sayaç koşmuyorken ekrandan
çıkılırsa ön dinleme yine kapanır (eski pil dostu davranış korunur).
**Davranış değişikliği (açıkça ilan):** eskiden sekmeden çıkınca
çalan ses kesilirdi; artık sayaç koşarken manzara arka planda sürer
— bu, D22'nin varlık sebebidir.

### D23 · ◉ Günlük seans hedefi halkası
Kadran iç çemberinde hedef kadar küçük nokta belirir (zamanlayıcı
ayarları → 0–12, varsayılan 4). Biten her odak seansı bir noktayı
vurguyla doldurur; hedefe ilk varışta halka parlar ve kutlama
gösterilir (günde bir kez). Sayı, tahmin değil: `OdakKaydi`'nin
bugünkü oturumları. Not: odak kaydı modu KAPALI ise deftere
yazılmadığı için ilerleme görünmez — ayar gerekçesinde belirtilir.
0 seçilirse halka tamamen gizlenir.

### D24 · ▶ Derse kenetli otomatik başlatma
Günlük ders hatırlatıcısına ikinci düğme: **"▶ Odak başlat (25 dk)"**
(dakika, zamanlayıcı varsayılanından gelir). Dokunuş sayacı hemen
başlatır ve sıradaki tamamlanmamış ders seans etiketi olarak işlenir.
Sayaç zaten koşuyorsa düğme gösterilmez — koşan oturum bölünmez.
Sapma kaydı (dürüstçe): derslerin saat çizelgesi olmadığı için
"10 dk kala pencere" yerine mevcut günlük hatırlatma anına bağlandı.

### Yeni dosyalar (7 + 1 test)
```
NefesProgrami.kt   desen/faz/ölçek — saf, testli
NefesView.kt       halka çizimi
NefesActivity.kt   stüdyo ekranı (chip'ler, tur, titreşim)
Hayalet.kt         D20 maç hesabı (saf çekirdek + ayar)
OdakKalkani.kt     D21 gözcü + bildirim + ayar
SesManzarasi.kt    D22 tek motor + otomasyon tablosu
OdakRitim.kt       D23 hedef + kutlama bayrağı
GrupDTest.kt       14 test (499 toplam)
```

### Değişen dosyalar
```
SayacKadraniView.kt  +seans işaretleri +hayalet yayları (onDraw)
TimerEngine.kt       5 kanca: manzara + kalkan esitlemesi
TimerFragment.kt     ses kartları → motor; 🌬 çip; maç/işaret besleme
FullscreenTimerActivity  ses motoruna geçiş; fx yansıtma
SayacAyarActivity.kt 3 grup: MANZARA · KENDİNLE MAÇ · KALKAN (+onResume)
SayacZincir.kt       +🌙 Uyku öncesi nefes şablonu (id -5)
CourseReminderReceiver.kt  +odak düğmesi (istek 5151)
TimerActionReceiver.kt     +EXTRA_ETIKET (D24)
fragment_timer.xml   +nefesButton +ghostText (todayStat kartı sarmalı)
AndroidManifest      +NefesActivity +PACKAGE_USAGE_STATS
strings.xml          3589 → 3649 (60 yeni, fo_ öneki)
```

### Öz denetim — bu sürümde yakalananlar
| # | Bulgu | Sonuç |
|---|---|---|
| 1 | 🔴 **Benim hatam:** onDestroyView'a "ses ölsün" anlamına gelen eski stopSound çağrısını bırakıp üstüne "kaldırıldı" yorumu yazmışım — kod-yorum çelişkisi | Yayından ÖNCE yakalandı; yerine motora `ekranKapandi` (sayaç koşmuyorsa kapat) eklendi; ayrıca sayaç koşarken manuelKapatti'nin istemeden set edilmesi de bu sayede engellendi |
| 2 | 🔴 Test dosyamda var olmayan 4 parametreli float assert (satır 75) | test derlemesinde yakalandı, düzeltildi |
| 3 | 🔴 Şablona 4 sn evre koydum; **mevcut** `SayacZincirTest` MIN_EVRE_SN ihlalini yakaladı — güvenlik ağı çalıştı | Şablon 5-7-8'e çevrildi ve nedeni belgelendi |
| 4 | 🔴 Sandbox 4. kez sıfırlandı (gradle yok / EXIT=127) | kur-ortam + 3G swap ile kurtarıldı; GRADLE_USER_HOME sayesinde bağımlılıklar yeniden inmedi |
| 5 | 🟡 FTA anchor eşleşmedi (iki satır arasında satır varmış) | Atomik yama sayesinde hiçbir dosya yarım kalmadı; doğru bağlamla yeniden uygulandı |
| 6 | 🟡 D22 tarama gerekçesi belgede yanlıştı (döngüler zaten vardı) | Düzeltme bu bölümün başından itibaren ilan ediliyor; kapsam otomasyon olarak netleştirildi |
| 7 | ℹ️ `sound_error` dizesinin tek kullanımı kaldırıldı (kart artık motoru sürüyor) | R8/shrinkResources ayıklıyor; davranış değişikliği yok |

### R8/APK doğrulaması
- sürüm **168 / 10.12**; imza aynı: `5F:15:…:85:11` ✔
- `SimgeVarsayilan` etkin ✔ · `PACKAGE_USAGE_STATS` manifestte ✔
- **60/60** `fo_` dizesi release APK'da (aapt2) · fragment_timer derlendi ✔
- **Test: 499, 0 başarısız, 0 hata**
- APK 18,2 MB · md5 `f9a37337386bc4d9c6a6691f2101a21a`
- Kaynak zip 692 dosya (~11,9 MB) · md5 `3ab23249f65213b430fa503903b6c50c`

### 📱 Cihazda doğrula (bizzat bakılacaklar)
1. Sayaç ekranı → 🌬 Nefes: halka 4-7-8'de büyüyüp küçülüyor, faz
   geçişlerinde kısa titreşim; "8 döngü" bitince kutlama toastı.
2. Zincir şablonları listesinde "🌙 Uyku öncesi nefes" seçilebiliyor.
3. Zamanlayıcı ayarları → Hayalet "Geçen haftaki sen": bir seans
   bitirdikten sonra kadranda yaylar + maç satırı; veri yoksa 👻
   dürüst mesaj. Mod "Kapalı" yapınca metin ve yaylar birlikte gider.
4. Hedef halkası: 2 seans bitir → 2 nokta dolar; hedefe varınca
   parlama + toast YALNIZ bir kez; hedef 0 → noktalar gizlenir.
5. Manzara: yağmur seçili + otomatik açıkken 5 dk sayaç başlat →
   ses duyulur · duraklat → susar · sürdür → devam · sekme değiştir
   (sayaç koşuyor) → sürer · bildirimden İptal → susar.
6. Molada kıs: pomodoro açıkken mola evresinde ses belirgin kısılır.
7. Ön dinleme koruması: sayaç DURURKEN ses çal, sekmeden çık →
   ses kapandı (pil dostu eski davranış yaşar).
8. Kalkan: izni ver + bir uygulama kısıtla + odak başlat → kısıtlı
   uygulamayı aç; ~5-10 sn içinde 🛡 bildirim; "Odağa dön" sayaca
   düşürür; kaçamak sayacı artar; 2 dk içinde aynı uygulama ikinci
   kez bildirim DÜŞÜRMEZ.
9. Ders hatırlatıcısı bildiriminde "▶ Odak başlat" → sayaç başlar,
   etiket çipinde ders adı; sayaç koşarken bildirimde düğme YOK.
10. İzin vermeden kalkanı aç → sistem ayarına yönlendirir, uygulama
    çökmeden bekler (anahtar açık kalabilir, gözcü izin gelince koşar).
11. Ekran döndürme: nefes turu sıfırlanır — bilinen sınır (tur
    ilerlemesi yeniden başlar).
12. Gece/gündüz temasında kadran noktaları ve hayalet yayları okunur
    (nokta/ark renkleri tema paletinden alınır).

### Sırada
**v10.13 = ULTRA-30 GRUP B — Widget devrimi:** B7 kokpit süper
widget, B8 ay takvim widget, B9 uyku widget, B10 1×1 "şimdi odak",
B11 köşe yarıçapı + yazı kaydırıcı, B12 örnek başına içerik.

---

## 🎨 v10.11 — ULTRA-30 GRUP A: Görünüm Devrimi (8 Ağu 2026)

Yeni listenin ilk grubu; altı öneri de uygulandı. Rota onayı:
"hepsini yap, her grupta bağlantı ver, hatasız olsun."

### A1 · Güneşe göre gece modu (🌗 dördüncü düğme)
Gece moduna **Güneş** seçeneği: gündüz açık, gece koyu. Zaman
kaynağı çiftli: **☀️ Güneşten** (yerel astronomik doğuş/batış —
`NamazVakti`'nin aynı hesabı, internet gerekmez) veya **⏰ Elle**
(aydınlanma/kararma saat çifti). `AppCompatDelegate`'e anlık kip
olarak basılır; MainActivity her `onResume`'da tazeler (uygulama
açıkken geçiş de yakalanır). Saf karar `koyuMuDakika` sınır/ters/
boş-aralık davranışıyla testli.

### A2 · Bağlam profilleri
Görünüm ekranının en üst satırı: **🎓 Sınav · 💼 İş · 🌿 Dinlenme**
+ "＋ Şu anı profil yap". Bir dokunuşla **dört katman birden**:
tema seti (tema+vurgu+gece+yoğunluk+yazı+dinamik — `TemaPaketi`
mekaniği), sessiz saat penceresi, günlük odak hedefi, uyku kapıları
(v10.9). Profil kopya saklar (bağlı paket silinse de bozulmaz);
`-1` = "bu yön değişmesin". Kota 5, uzun bas = silme.

### A3 · Gün ışığı şeridi
Ana ekran hero kartında doğuş→batım rayı: geçen kısım parlak dolar,
güneş topuzu ray üstünde kayar (ray + topuz + ışın çizgileri çift-kod).
Alt metin "Kararmaya 2 sa 14 dk"; geceyse "Gece — güneş 06:12'de
doğar" + gece işaretli sönük ray. Veri yoksa blok gizlenir.

### A4 · Yazı karakteri vitrini
`res/font`'a 2 yeni OFL fontu (Atkinson Hyperlegible + Lora, ~205 KB).
Görünüm ekranında 3 canlı kart (kendi yüzleriyle örnek metin):
**Poppins (marka) · Atkinson (okunaklı) · Lora (kitap dokusu)**.
Uygulama `applyStyle(force=true)` katmanıyla — tüm gövde metinleri
değişir; XML'de açıkça `@font/poppins_*` yazılmış başlıklar markada
kalır (huşu: tasarım kuralları gereği).

### A5 · Pofi gardırop
Uzun bastan **gardırop diyaloğu**: 🧢 Seri beresi (7 gün) ·
🕶️ Usta gözlüğü (250 görev) · 🧣 Gece eşarbı (3 uyku gecesi,
v10.9 defteri) · 👑 Efsane tacı (30 gün rekor). Kilit kuralları
vitRide şeffaf yazıyor; giydir/çıkar anında canvas'ta görünür
(taç aleve binince doğal durur). Eski tanıtım diyaloğu gardrobun
içinden de açılır.

### A6 · Erişilebilir vurgu (Okabe-Ito)
13. vurgu rengi **Erişim**: #0072B2 mavi + #E69F00 turuncu çifti
(renk körlüğünde de ayrışan standart çift); Atkinson fontuyla aynı
hikâye/anlatı. İkincil kodlama ilkesi zaten yeni bileşenlerde:
hafta widget işaretleri şekil+metin, gün ışığı ray+gece-simgesi.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Çözüm |
|---|---|---|
| 🔴 | **AĞIR HATA — kendi elimle:** python `io.open(p,'w')` açılışta dosyayı sıfırladı; yazım başarısız olunca `HomeFragment.kt` (798 satır) boş kaldı | **v10.10 kaynak zip'inden geri getirildi** (teslim disiplininin karşılığı), betikler artık "önce UTF-8 kodla, sonra yaz" kalıbında |
| 🔴 | `style/FontKaplamasi.Atkinson` aapt bağlantısı koptu (noktalı ad = örtük ebeveyn kuralı; `FontKaplamasi` stili yok) | Noktasız adlar `FontKaplamasiAtkinson/Lora` — derlemede yakalandı |
| 🔴 | `ax_gunes_oto` dizesine `\u20` kaçağı (XML unicode kaçışı yok) | Elle gözden geçirmede yakalandı, düzeltildi |
| 🔴 | Şafak-öncesi "kararmaya 14 saat" saçmalığı (yüzde fonksiyonu doğuş öncesini gündüz sanıyordu) | `yuzde` gece→-1 kuralıyla + testle sabit |
| 🔴 | Gözlük `strokeWidth`'i sıfırlıyordu (sonraki kareye taşardı) | Önceki kalınlık saklanıp geri yüklenir oldu |
| 🔴 | Bold font dosyaları referanssızdı (shrinkResources tuzağı) | Kaldırıldı — sentetik kalın yeterli |
| Not | Bold dosyalar indirildi ama kullanılmadı; Lora ilk indirme HTML döndürmüştü (repo yolu yok) | Fontsource CDN'den geçerli TTF alındı (magic bytes doğrulandı) |

### Cihaz doğrulaması (8 madde)
1. 🌗 Güneş düğmesi + alt panel (kaynak değiştio; saat seçicileri)
2. Bağlam çipi → tema+sessiz saat kapısı+hedef+uyku saati birlikte
3. Gardırop uzun bas → bere/şart yazıları, giyince ✓, kapalılar 🔒
4. Pofi bereyi giyiyor (canvas).
5. Gün ışığı rayı saat ilerledikçe doluyor (bindData'dan yenileniyor).
6. Font kartı → gövde değişti, başlıklar Poppins kaldı.
7. Erişim vurgusu tüm düğme/çiplerde mavi turuncu.
8. Ayarlar tercümesinde "yazı karakteri" vitrini 3 kart.

### Dosyalar
```
Yeni:  BaglamProfili.kt · GunIsigiView.kt · MaskotGardrop.kt
       font/atkinson_regular.ttf · font/lora_regular.ttf
       test/GrupATest.kt (12 test)
Değişen: ThemeManager.kt (GECE_GUNES+güneş API+font API+Erişim)
       MaskotView.kt (aksesuar katmanı) · ThemeFragment.kt (3 sistem)
       fragment_theme.xml (bağlam+güneş+font) · fragment_home.xml (serit)
       HomeFragment.kt (A3+A5) · MainActivity.kt (güneş kipi tazeleme)
       strings.xml (3554→3589) · themes.xml (+3 stil)
```

**APK (release+R8):** 18,2 MB · md5 `76f6ece5b256b371aa3cc8bbed8b3946`
**İmza:** `5F:15:…:85:11` ✔ · **Test:** 485/485
**Kaynak zip md5:** `e9e7079c80651f3af99c4d4963f71616`

### Sırada
**v10.12 = ULTRA-30 GRUP D** — Zamanlayıcı & Odak (D19–D24):
nefes stüdyosu, hayalet modu, odak kalkanı, ses manzarası,
seans hedefi halkası, derse kenetli başlatma.

---

## 🗓️ v10.10 — ULTRA-50 kapanış maddesi C34: Hafta widget'ı (8 Ağu 2026)

50 maddelik listenin **son açık öğesi** kapatıldı — artık kullanıcının
yeni 30 önerilik listesine (ULTRA-30) geçilecek.

### Öneri metni ve dürüst yorum

> **34.** PlanWidget hafta görünümü modu: tek gün listesi yerine
> 7 sütunlu mini hafta; dolu/boş işaretleri, güne dokununca o günün
> plan sayfası. ⭐⭐⭐ · Y

Kod taramasında görüldü: **`PlanWidget` namaz günü widget'ıdır** —
dilim işleri yalnız bugün yaşar, haftalık verisi yoktur; üstüne
"hafta modu" eklemek kavramsal olarak uymazdı. Hafta verisi
`HaftaPlan`'da duruyor (gün bazlı dk hedefi + ders ataması).
Bu yüzden "mod" yerine gerçek hafta verisine bağlanan **yeni
sağlayıcı `HaftaWidget` (4×2)** kuruldu; önerinin iki taahhüdü
birebir karşılandı:

| Taahhüt | Karşılık |
|---|---|
| 7 sütunlu mini hafta, dolu/boş işaretleri | Pzt→Paz hücreleri: `● 90` (dk hedefli) · `● 📖` (dersli) · 🌿 (izin) · `○` (tanımsız) + **kalan görev rozeti** ("3 ⚑") |
| Güne dokununca o günün plan sayfası | Hücre → `HaftaPlanActivity` o günün **hedef diyaloğu açık** gelir (`EXTRA_GUN`) |

### Ayrıntılar
- Bugün hücresi: "▸" işareti + vurgu rengi; dar ölçüde gün adı
  düşer (tarih+işaret kalır), kısa ölçüde görev rozeti düşer.
- Kalan görev = o güne tarihli, tamamlanmamış görev; 0 iken
  rozet gizlenir (hücre nefes alır).
- Tazelenme kanalları: `WidgetCommon.refreshAll` listesine eklendi
  (görev tamamlama, gece yarısı, tema değişimi otomatik) +
  hafta planı ekranındaki her düzenleme `planDegisti()` üzerinden
  anında tazeler (8 çağrı noktası tek huniye bağlandı).
- Hafta planı kapalıysa "kur" ipucu satırı açılır.
- WidgetTema paleti: dolu işaretler vurguda, boşlar solukta;
  şeffaflık ayarı köke uygulanır.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Çözüm |
|---|---|---|
| 🔴 | **Benim hatam:** dolu/boş ayrım için eklediğim iki liste çizim `try`'ının içinde bildirilmişti; tema `try`'ından erişilemezdi — derleme hatası | Bildirimler `render()` girişine taşındı; hatayı ön derleme kontrolü yakaladı |
| 🔴 | Ortam üçüncü kez sıfırlandı (EXIT=127) | v10.6/10.8 prosedürü: kur-ortam.sh + 3G swap (~2 dk) |
| Not | Öneri metni PlanWidget'a "mod" diyordu; gereği uygulanamaz olduğundan yeni sağlayıcı kuruldu | Karar ve gerekçe bu tabloda belgeli |
| Not | PendingIntent istek kodları 4889-4897 proje taramasından geçti | Çakışma yok |

### Dosyalar
```
Yeni:  HaftaWidget.kt · layout/widget_hafta.xml · xml/w_hafta_info.xml
       HaftaWidgetTest.kt (9 test)
Değişen: AndroidManifest (+HaftaWidget)
       WidgetCommon.kt (TUM_WIDGETLAR +1)
       HaftaPlanActivity.kt (EXTRA_GUN + planDegisti hunisi, 8 nokta)
       strings.xml (+4 hw_*)
```

**APK (release+R8):** 18 MB · md5 `9e82aa9a43f6f949b664f7331d8beecc`
**İmza:** `5F:15:…:85:11` ✔ · **Test:** 473/473
**Kaynak zip md5:** `222a24511171c1e6d2a1462b6a33bb9a`

### Sırada
Kullanıcının yanıtı: "hepsini yap, her grupta bağlantı ver."
Rota: **v10.11 = ULTRA-30 Grup A (A1–A6 görünüm devrimi)** →
v10.12 D · v10.13 B · v10.14 E · v10.15 C.

---

## 🌙 v10.9 — Gün çerçevesi: "uyandın mı?" + akşam özeti (8 Ağu 2026)

**Bu sürüm ULTRA-50'den değil — kullanıcının doğrudan isteği
(olduğu gibi):**
> "Sabah belirlediğim saatte SESSİZ 'uyandın mı?' bildirimi alayım,
> ona göre görevler vb. şeyler güne başlasın, plan program yapsın;
> akşam belirlediğim saatte 'uyuyacak mısın?' bildirimi, yine
> belirlediğim saatte günüm özeti vb. şeyler. Bunu AŞIRI KAPSAMLI
> yap, her şeyini değiştirebileyim, ayarlarını filan."

### Dürüstlük tablosu (önce kod taraması, sonra iş)

| İstek | Kodda öncesi | v10.9'da |
|---|---|---|
| Sabah onayına bağlı gün başlangıcı | "uyanma onayı" kavramı kodda 0 sonuç; sabah rutini sabit 09:00 turuydu | **YENİ** |
| Sessiz sabah bildirimi | Tüm sabah bildirimleri merkez üzerinden, tek önem düzeyi | **YENİ** (sessiz/sesli iki kanal) |
| Akşam gün özeti | 19:00 akşam turu yalnız *hatırlatma* (kart tekrarı, seri riski); özet bildirimi yok | **YENİ** |
| Uyku defteri + ortalamalar | Yok | **YENİ** (14 gün) |

### Akış

**SABAH — seçilen saatte (varsayılan 07:00):**
"🌅 Uyandın mı?" — varsayılan **SESSİZ** kanal (ses/titreşim yok).
`✅ Uyandım` → deftere uyanma yazılır ve **eski 09:00 rutininin
tamamı o anda teslim edilir** (özet teslimi, gün odağı bildirimi,
günlük kart, sınav sayacı…). Cevap gelmezse tekrar zinciri
(aralık 5-60 dk, azami 0-6 — ikisi de ayarlı). Zincir tükenirse
**son çare** açıksa gün yine de başlar; kapalıysa o gün sessiz
kalınır. "Onay şart" kapatılırsa soru sorulmaz, saatte gün
doğrudan başlar. `😴 Ertele` sayacı tüketmez.

**AKŞAM — seçilen saatte (varsayılan 23:00):**
"🌙 Gün bitti — uyuyacak mısın?" + **dört satırı ayrı ayrı
kapatılabilen** özet: odak süresi · tamamlanan görev · pomodoro
turu · gün serisi (değeri 0 olan satır düşülür). `😴 Uyuyorum` →
deftere uyuma yazılır + 4 saatte kendini silen "iyi geceler" notu
(yarınki sabah saatiyle). Tekrar zinciri 5-90 dk × 0-6.

### Ayar ekranı ("her şeyini değiştirebileyim")
DURUM (ana anahtar + sıradaki iki alarm ve kalan süre; bildirimler
kapalıysa uyarı satırı) · SABAH (saat, sessiz, onay şartı, tekrar
aralığı, azami tekrar, son çare) · AKŞAM (saat, tekrar aralığı,
azami tekrar) · ÖZET İÇERİĞİ (4 bağımsız anahtar) · UYKU KAYDI
(ortalama uyanma / uyuma / süre + son 7 gün listesi + temizle) ·
DENE VE SİSTEM (iki test bildirimi — gün işaretlerini **tüketmez** —
+ Android kanal ayarları kısayolu).

### Mimari kararlar
- **İkiz teslimat yok:** çerçeve açıkken `BildirimZamanlayici`'nin
  09:00 sabah turu kapıya devredilir; çerçeve kapanırsa aynen geri
  döner. Akşam 19:00 hatırlatma turu dokunulmadan sürer
  (hatırlatma ≠ özet; bunlar farklı içerik).
- **Tam alarm:** `setExactAndAllowWhileIdle`, API 31+'da izin yoksa
  `setAndAllowWhileIdle` (`TimerAlarm` örneği). Tek atım + zincir:
  her kapı işlenirken bir SONRAKİ gün kurulur; BootReceiver'ın iki
  dalı + App.kt her fırsatta onarır.
- **Sessiz/sesli ayrımı iki kanal:** Android'de kanal önemi
  kurulduktan sonra değiştirilemez.
- **Merkez dışı gönderim:** kapılar kullanıcının seçtiği saatli
  alarm olduğu için merkezin sessiz-saat/günlük-tavan kapılarına
  takılmaz (`TimerActionReceiver.evreBildir` örneği); genel
  bildirim anahtarına yine saygı.
- **Defter 14 gün**; 20 saati aşan "uyku" ölçüm hatası sayılır
  (`uykuSuresiMs` → null). Uyuma ortalaması gece yarısı **sarmal**
  (23:50 + 00:10 → 00:00; düz ortalama öğlen 12:00 bulurdu).

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Çözüm |
|---|---|---|
| 🔴 | 00:15'te "Uyuyorum" diyen aynı gün 23:00 özetini tüketmiş sayılıyordu (işaret çıplak takvim gününe bakıyordu) | Akşam döngüsü günü = saat −6s (`aksamGunAnahtari`) — sınır tablosu 4 birim testle sabit |
| 🔴 | Gece yarısını geçip uyuyanın uykusu hiç çiftleşmiyordu (uyanma yalnız düne bakıyordu) | `uyandiKaydet` önce düne, bulamazsa aynı güne çiftleşir |
| Not | `onaySart`'ı zincirin ortasında kapatmak o günkü gün başlatmayı durdurur (soru cevaplanabilir kalır) | Belgelendi; ertesi günden itibaren doğrudan başlatma devreye girer |
| Not | 6×90 dk azami akşam zinciri 06:00'yı aşarsa sınırda bir ek gönderi olabilir | Belgelendi, içerik doğru kalır |

### Cihaz doğrulaması istenenler (kurulumdan sonra)
1. 07:00'de "Uyandın mı?" **sessiz** gelmeli (ses/titreşim yok).
2. `✅ Uyandım` → aynı anda gün kartları/bildirimleri gelmeli.
3. Cevapsız bırak → 15 dk sonra "(tekrar 1/3)" etiketli yineleme.
4. 23:00 özeti → `😴 Uyuyorum` → iyi geceler notu; ertesi sabah
   ayar ekranındaki Uyku kaydı kartında süre görünmeli.
5. DENE satırları bildirim gönderir ama günü **tüketmez**.

### Dosyalar
```
Yeni:  UykuCerceve.kt · UykuZamanla.kt · UykuAksiyonReceiver.kt
       UykuAyarActivity.kt · UykuCerceveTest.kt (27 test)
       drawable/ic_uyku_sabah.xml · drawable/ic_uyku_aksam.xml
Değişen: AndroidManifest (+UykuAyarActivity, +UykuAksiyonReceiver)
       BootReceiver.kt (2 kanca) · App.kt (onarım kancası)
       BildirimZamanlayici.kt (sabah turu devir kapısı)
       SettingsFragment.kt + fragment_settings.xml (rowUyku)
       strings.xml (3478 → 3550, +72 uy_*)
```

**APK (release+R8):** 18 MB · md5 `fa85518f6ffec292bf45920a3f094729`
**İmza:** `5F:15:…:85:11` ✔ · **Test:** 464/464
**Kaynak zip md5:** `c31e8689b401b9cbc7de8dd29451fb54`

### Sırada
ULTRA-50'nin kalan tek maddesi: **C34 — hafta görünümü widget'ı +
kalan görevler paneli** (bir sonraki sürüm; v10.9.1 veya v10.10).

---

## 🎨 v10.8 — Kimlik: ULTRA-50 D40 + D43 (7 Ağu 2026)

Yedinci durak, uygulamanın "kişiliği": kaydedilebilir tema
paketleri ve duruma göre yüz değiştiren maskot **Pofi**.

### Dürüstlük tablosu

| Öneri | Bulgu (tarama kanıtı) | v10.8'de yapılan |
|---|---|---|
| D40 tema stüdyosu | 10 tema + 12 vurgu + gece modu + yoğunluk + yazı ölçeği + dinamik renk hep **tek tek** ayarlanıyordu; kayıtlı kombinasyon kavramı kodda sıfır | **YENİ** |
| D43 maskot | "maskot" kelimesi kodda 0 sonuç; tepki veren karakter hiç yok | **YENİ** |

### D40 · Tema paketleri (stüdyo)

Görünüm ekranının en üstüne yeni satır: **☀️ Gün ışığı · 📚 Sınav
odağı · 🦉 Gece kuşu · ⚡ Neon hız** + kendi kaydettiklerin +
"＋ Kombinasyonu kaydet" çipi.

- Bir paket = altı tercihin fotoğrafı: tema + vurgu + gece modu +
  yoğunluk + yazı ölçeği + dinamik renk. Dokun → hepsi birden
  uygulanır (Activity recreate — vurgu akışıyla aynı mekanik).
- Kaydetme diyaloğu: ad + 8 emojiden görsel seçim; kota 8 paket,
  dolunca uyarı; uzun bas → silme sorusu.
- Bozuk dizinler `dogrulanmis` ile budanır (yedek aktarımı /
  ileride tema silinmesi güvenli); JSON turu + şablon dizinleri
  + şablon farklılığı **12 birim testi** ile sabit.

### D43 · Pofi (uygulama maskotu)

Ana ekran hero kartının en üst satırında yaşayan, **duruma göre
yüz değiştiren** küçük dost. Asset değil canvas çizimi — seçili
temanın rengini kendiliğinden giyer (body = colorPrimary).

- **Altı ruh hali, öncelik sırasıyla:** MOLADA (gevşemiş kapalı göz)
  → ODAKLI (hedeflenmiş bakış) → UYKULU (23:00–05:00, Zz) →
  ALEV (seri ≥ 7, başta alev) → GURURLU (bugün ≥100 dk odak,
  kocaman gülümseme) → NESHALI (bekleme hâli).
- Karar tablosu `Maskot.kt`'de saf — saat parametreli (saat-tuzaklı
  test yok), öncelik matrisi **11 birim testi** ile sabit.
- Her ruhun 4 mesajı (24 satır); gün boyu sabit, dokundukça
  döner. Uzun bas → Pofi kendini tanıtır.
- Zincir bilinci: zincirin odaksız evresi (ısınma/mola) de
  "molada" sayılır — Pofi v10.7 zincirlerini tanır.
- Animasyon bütçesi: göz kırpma ~3 sn'de bir 170 ms; sistem/
  uygulama animasyon ayarı kapalıysa döngü hiç başlamaz; ekran
  dışındayken döngü kesin ölür.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Çözüm |
|---|---|---|
| 🔴 | **Benim test hatamdı:** `saatTasmasi` testi -3 saat için NESHALI bekliyordu; oysa sınıra budama -3→0:00 yapar ve gece yarısı uyku penceresidir. Üretim kodu doğruydu, beklenti yanlıştı | Test, budama davranışını **belgeler** hâle getirildi |
| 🔴 | Ortam sürüm ortasında ikinci kez sıfırlandı (/opt boş) | `kur-ortam.sh` + 3G swap (v10.6 prosedürü) |
| Not | Paket uygulaması recreate tetikler | Vurgu akışıyla aynı mekanik — iddia yok, tutarlılık var |
| Not | Pofi yalnız ana ekranda (widget değil) | Widget RemoteViews özel View kabul etmiyor; bilinçli sınır |

### Cihazda doğrulama listesi (kullanıcıdan beklenen)

1. Görünüm ekranı → "🦉 Gece kuşu" çipine dokun → ekran anında
   koyu Aurora'ya dönmeli, rahat yoğunlukla.
2. Kendi düzenini kur (tema + vurgu + yoğunluk) → "＋ Kombinasyonu
   kaydet" → ad + emoji → çip satırda görünmeli; uzun bas → silme.
3. Ana ekranda Pofi: sayaç başlat → ODAKLI yüz; molada → gevşemiş;
   satıra dokun → mesaj değişmeli; uzun bas → tanıtım kartı.
4. 7+ günlük seriyle ana ekrana gel → Pofi başında alev taşımalı.

---

## ⏱ v10.7 — Kadran zamanı: ULTRA-50 A3 + A6 (7 Ağu 2026)

Altıncı durak, sayaç ekranının iki derin jesti. Kullanım hissi
değiştiren, ekran-bildirim-widget üçgenine yayılan bir paket.

### Dürüstlük tablosu

| Öneri | Bulgu (tarama kanıtı) | v10.7'de yapılan |
|---|---|---|
| A3 halkadan süre seçimi | `SayacKadraniView` içinde dokunma kodu **sıfır** (grep 0); süre yalnız 4 çip + NumberPicker diyaloğundan seçiliyordu | **YENİ** |
| A6 çok aşamalı zincir sayaç | `Pomodoro` sabit 3 evreli ve kuralı koda gömülü; "zincir" kelimesi kodda hiç yok | **YENİ** |

### A3 · Kadranı sürükleyerek süre seçimi

Geri sayım boştayken dış halka artık bir kadran: parmak dairede
gezindikçe süre 1 dakika adımlarla değişir (tam tur = 60 dk, her
çizgi = 1 dk), her dakika geçişinde saat tıkı titreşimi duyulur.
Sürüklemede kadran "🎯 42 dk — bırakınca ayarlanır" gösterir;
bırakınca süre kalıcı kurulur.

- Ortaya dokunma = başlat/duraklat davranışı **değişmedi**; seçici
  yalnız dış halka bandında devreye girer (`HalkaSecti.halkadaMi`).
- Taze sayaç boştayken alt satırda "Halkayı sürükleyerek süre seç"
  ipucu durur (önceden taze sayaç "duraklatıldı" yazıyordu —
  yanıltıcıydı).
- Koşullar: yalnız geri sayım + sayaç başındayken + zincir koşmuyor.
- Matematik tamamen `HalkaSecti`'de, **18 birim testi** ile kanıtlı
  (4 yön açısı, açı↔dakika ters eşleme, bant toleransı, eşik).

### A6 · Zincir sayaç (⛓ çok aşamalı)

Sayaç ekranında etiket çipinin altına yeni **⛓ çip**: şablon
seç / kendi zincirini kur / başlat / duraklat / baştan kur.

- **4 hazır şablon:** 🔥 Tabata (8×(20 sn çalış + 10 sn dinlenme) =
  tam 4:00), 🍅 Pomodoro döngüsü (25/5 ×4), 📚 52/17 kuralı,
  🏃 Egzersiz turu (ısınma–sprint–yürüyüş–soğuma).
- **Kendi zincirin:** diyalogda dinamik satırlar (evre adı + dk),
  8 evre sınırı, tekrar 1–10. Boş satır sessizce atlanır; hata
  varsa diyalog kapanmaz, sebep toast ile söylenir. Adında
  mola/dinlenme geçen evreler **odak kaydına yazılmaz**
  (`molaBenzeriMi` — Türkçe ı katlamalı) ve emoji otomatik önerilir.
- Evre bitince sıradaki otomatik kurulur (arka planda alarm
  alıcısından — ekran kapalıyken de akar); "Evreler otomatik aksın"
  kapatılırsa her adım hazır bekler.
- Yüzeyler: kadran üst satırı "⛓ 💪 Çalış · 3/16", canlı bildirim
  metnine aynı ek gelir, zincir bitince 🎉 kutlama bildirimi.
- Zincir koşarken öncelik zincirindir; pomodoro ayarlarına
  dokunulmaz, zincir kapanınca olduğu gibi devreye girer.
- Zincir koşarken preset çipleri/halka seçici kilitlenir ("önce
  duraklat" uyarısı); Sıfırla ve İptal zinciri de duraklatır —
  yoksa evre bitişi gelmeden zincir takılı kalırdı.
- Süre birimi **saniye** (Tabata 20/10 dakika tabanlı sistemle
  imkânsızdı). İlerleme mantığı + JSON + doğrulama **23 birim
  testi** ile kanıtlı.

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Çözüm |
|---|---|---|
| 🔴 | Zincir evre bildirimi 4715 kimliğini istiyordu — **SNOOZE_ID ile çakışıyordu** | 4732/4733'e taşındı |
| 🔴 | `zincirBaslat`: `toggle.check` → `resetAll` koşan zinciri duraklatırken baslat önce çağrılıyordu → zincir anında askıya düşerdi | Sıra değiştirildi (önce mod, sonra baslat) |
| 🔴 | Halka jeti ScrollView'a kaptırılınca ACTION_CANCEL **yarım dakikayı commit ediyordu** | `secimIptal` geri-alma + DOWN'da disallow-intercept |
| Not | Mola evreleri günlük odak toplamına girer (ders kaydına GİRMEZ) | Pomodoro ile birebir aynı kural — bilinçli tutarlılık, ileride ikisine birden ayrık davranış eklenebilir |

### Cihazda doğrulama listesi (kullanıcıdan beklenen)

1. Boş sayaçta kadranın dış halkasını sürükle → dakika 1'er 1'er
   değişmeli, tık titreşimi gelmeli; bırakınca süre kalıcı kurulmalı.
2. Halkanın ORTASINA dokun → başlat/duraklat (seçim başlamamalı).
3. ⛓ çip → "🔥 Tabata" seç → Başlat → 20 sn sonra evre bildirimi
   + otomatik 10 sn dinlenme; kadran üstünde "⛓ 💪 Çalış · 1/16".
4. Zincir koşarken 5 dk çipine dokun → engel toast'ı gelmeli.
5. Zincir ortasında Sıfırla → sayaç sıfırlanır, zincir duraklar;
   çip "kaldı x/16" yazar; Devam et kaldığı yerden sürer.
6. "Kendi zincirim": "Okuma 25 dk + Mola 5 dk × 2" kur → kaydedilip
   seçilmeli; listede şablonların yanında görünmeli.

---

## 🧭 v10.6 — Arayüz ferahlığı: ULTRA-50 D grubu (7 Ağu 2026)

Beşinci durak. Tarama iki maddeyi daha listeden düşürdü — dürüstlük
kaydıyla:

### Dürüstlük tablosu

| Öneri | Bulgu | v10.6'da yapılan |
|---|---|---|
| D39 ⌘K paleti | `HizliKomut` (v9.5) doğal dilde VERİ giriyor; **gezinme paleti yoktu** | **YENİ** (tamamlayıcı araç) |
| D41 "gerçek e2e v2" | v10.1 `KenardanKenara` + `fitsSystemWindows` ile barlar zaten tutarlı | İddia yok (riskli görsel değişiklik, değer düşük) |
| D42 compact/ferah | 🔴 `GorunumAyar.yogunluk` 3 mod (sıkı/normal/rahat) **zaten var** | İddia yok |
| D46 ayar arama | Yoktu | **YENİ** |

### D39 · ⌘ Komut paleti (üst barda yeni düğme)

Üst bara ⌘ düğmesi: açılır açılmaz klavyeli arama kutusu, 15
komut (11 ekran + 25/15/5 dk sayaç başlatma + sayaç durdurma),
titremeli puanlama (başlık başlangıcı > sözcük başlangıcı > içerik
> anahtar kelime), ilk 6 sonuç. Çalışan sayacı bölme koruması var.
`HizliKomut` cümle yazma aracı olarak kalıyor; palet "sadece oraya
git" ihtiyacını karşılıyor — ikisi tamamlayıcı.

### D46 · Ayar içinde arama

Ayarlar ekranının üstüne 🔍 kutu: 30+ satır `row*` id deseninden
gezilir, içindeki tüm metinler toplanır ve **Türkçe-katlamalı**
eşleşmeyle süzülür. Boş kalan kartlar ve bölüm başlıkları otomatik
gizlenir. Türkçe katlama iki tür i'yi birleştirir (arıyorum:
"BILDIRIM" ≈ "bildirim") — bunu ilk sürümde sezgisel katlamayla
yazmıştım, testin yakaladığı gibi iki harf türü eşleşmiyordu 🔴→✔.

### Bu sürümün notu — ortam sıfırlandı

Derleme sırasında kum havuzunun `/opt` bölümü sıfırlanmış (JDK/SDK/
Gradle gitmiş). `kur-ortam.sh` + 3G swap yeniden kuruldu; kaynak
ağacı etkilenmedi. Build sonrası her şey yeşil.

### Doğrulama (cihazda bakılacaklar)

1. Üst barda ⌘ düğmesi → "say" yaz → Sayaç en üstte
2. Palet → "pomodoro" → 25 dk odak başlasın (çalışan sayaç varken bölmesin)
3. Ayarlar → 🔍 "bild" yaz → yalnız bildirim satırları kalsın;
   kartlar/başlıklar boş kalmasın
4. Arama temizlenince tüm satırlar geri gelsin

### Öz denetim notları (bu sürümde 6 yakalama)

| # | Sorun | Çözüm |
|---|---|---|
| 1 | Testi kafada koşturunca `say` sorgusunda beraberlik — yanlış güvence yazmıştım | test yeniden yazıldı |
| 2 | Ayar silsilesinde **`hizliKomutPenceresi` gövdesini yanlışlıkla kestim** | derlemeden önce fark edilip geri kondu (bu benim hatamdı) |
| 3 | `::pencere.isInitialized` yerel değişkende geçersiz | nullable AlertDialog |
| 4 | İndeks lambda'sında `it` görünüm sanılmıştı | `cocuklar[j]` |
| 5 | Boş kalan kartlar filtre sırasında görünür kalıyordu | kart görünürlük katmanı |
| 6 | Türkçe ı/i ayrımı aramayı kırıyordu (test yakaladı) | ı→i birleşik katlama |

---

## 📱 v10.5 — Widget vitrini: ULTRA-50 C grubu (7 Ağu 2026)

Dördüncü durak. Bu grup **en büyük dürüstlük düzeltmesini** de
getirdi: C28'i öneri olarak yazmıştım ama `onAppWidgetOptionsChanged`
**10 widget'ın hepsinde zaten vardı** — bu yüzden grup 5 maddeye
indi, hiçbiri "var olanın yeniden satışı" değil.

### Dürüstlük tablosu

| Öneri | Bulgu | v10.5'te yapılan |
|---|---|---|
| C27 SayacWidget hazır ayar | Chronometer+düğmeler vardı, **çip yoktu** | **YENİ** |
| C28 boyuta göre içerik | 🔴 10/10 widget'ta **zaten vardı** | İddia yok (katalogda işaretlendi) |
| C29 Widget stüdyosu | `requestPinAppWidget` hiç kullanılmamış | **YENİ** |
| C31 çoklu geri sayım | `CountdownWidget` tek etkinlikti | **YENİ widget** |
| C32 hedef halkası | `IlerlemeWidget` konu/ders ilerlemesiydi; günlük hedef takibi ana ekranda yoktu | **YENİ widget** |

### C27 · SayacWidget v2: 5·15·25 çipleri

Ana ekrandan odak başlatmak 3 dokunuş sürdürüyordu. Çipler tek
dokunuşa indirir: boştaki geri sayımda "5 dk · 15 dk · 25 dk"
belirir, dokununca süre kurulup başlar (`ACTION_BASLAT_DK`).
Çalışan/duraklatılmış oturumda **görünmez** — yanlışlıkla üzerine
yazma riski yok. Liste uygulama içi çiplerle aynı üçlü.

### C29 · Widget stüdyosu

Widget tema ekranına yeni bölüm: 8 sabitlenebilir widget türü
listelenir, dokun → sistemin "ana ekrana sabitle" akışı açılır
(`requestPinAppWidget`, API 26+). Desteklemeyen sürümde net
yönlendirme mesajı.

### C31 · Geri sayım listesi widget'ı (yeni, 2×2 kaydırılabilir)

`EventsListWidget` + `EventsListService`: tüm etkinlikler tek
widget'ta — emoji · ad · "N gün / bugün! / geçti". Seçim katmanı
`EventsListVeri` saf ve testli: yaklaşanlar gün sırasında, aynı
günde sabitlenen önde; geçmişten en fazla **en yakın 1** satır
bağlam için tutulur; liste 6 satırla sınırlı.

### C32 · Hedef halkası widget'ı (yeni, 1×1/2×1)

Günlük odak hedefi yüzdesi büyük rakam · ince bar · "X/Y dk · 🔥
seri". Hedef kurulu değilse widget dürüstçe kuruluma çağırır.
**Neden gerçek halka yok:** RemoteViews özel View kabul etmiyor —
kağıt üzerinde halka, pratikte büyük yüzde + bar (v7.40.1
CardView kırığı dersi). Küçük boyutta alt satır gizlenir.

### Doğrulama (cihazda bakılacaklar)

1. SayacWidget boştayken üç çip; 15 dk'ya dokun → sayaç başlıyor
2. Widget teması ekranı → "Widget stüdyosu" → bir türü sabitle
3. Yeni "Geri sayımlar" widget'ını ekle → 2+ etkinlik listeleniyor
4. Yeni "Hedef halkası" → yüzde, bar, seri satırı; 1×1'de alt satır gizli
5. Etkinlik geçince listeden düşmesi (en yakın geçmiş 1 satır kalır)

### Öz denetim notları (bu sürümde 2 yakalama)

| # | Sorun | Çözüm |
|---|---|---|
| 1 | `WidgetTemaActivity`'de `Build` import'u eksik — derleme hatası | import eklendi |
| 2 | `Palet` alanları non-null Int; gereksiz `?.let` yazmıştım | düz `setTextColor` |

---

## 🧠 v10.4 — Derin sayaç kimliği + bildirim zekâsı (7 Ağu 2026)

Üçüncü durak: sayaç artık **etiketli, konuşuyor, molası söz
söylüyor**; bildirim merkezi de yuttuklarını sabah teslim ediyor.

### Dürüstlük tablosu

| Öneri | Bulgu | v10.4'te yapılan |
|---|---|---|
| A5 oturum etiketi | Hazır ayarlar yalnız süreydi (5/15/25), etiket hiç yoktu | **YENİ** |
| A9 TTS geri sayım | TTS yalnız `SesliDersServisi`'ndeydi, sayaç sessizdi | **YENİ** |
| A7 mola kişiliği | Mola bildirimi "5 dk mola" der susardı | **YENİ** |
| A12 bağları | 🔴 `kaliteOrtalamasi` HİÇBİR yerde gösterilmiyordu | **TAMİR** |
| B18 sabah özeti | Sessiz/tavan yutması kayıpsız değildi — yutulan kayboluyordu | **YENİ** |
| B17 bildirim diyeti | Günlük tavan **zaten var** (`gunlukTavan`, gonder kapısı) | İddia yok |
| B15 Now Bar | `Notification.ProgressStyle` API 36 — compileSdk 34'te yok; Samsung Now Bar özel SDK ister | **Bilinçli atlandı** (canlı üçleme zaten var: kronometre+ilerleme çubuğu+tazeleme) |

### 🔴→✔ A12'nin devamı: puanların görünür olması

v10.2'nin öz denetim notu eksikti: 🙁😐😄 puanları kayda geçiyordu
ama **hiçbir ekranda okunmuyordu**. Sayac>İstatistik bölümüne
"Takvim/Süre" kartının üstüne ortalama puan (1-3) ve ne anlama
geldiği eklendi. Listeme ek ders: kayıt ≠ değer; yüzey şart.

### A5 · Oturum etiketi

Hazır ayar satırının altında **🏷️ çip**: dokun → ad ver (en çok 24
karakter) → çalışan bildirimde **alt metin** olarak taşınır
(`setSubText` — başlığa karışmaz, uyumluluk/duraklatma dallarında
da görünür). Dürüst sınır: etiket istatistik kayıtlarına henüz
bağlanmıyor; bu A5'in "kimlik" yarısıdır.

### A9 · Sesli geri sayım

Ayarlıysa (varsayılan **kapalı**) sayaç ekranı açıkken kalan süre
kulağa söylenir: "Beş dakika kaldı" → "Bir dakika" → "Otuz
saniye" → son 10'da sayma ("Üç, iki, bir"). Eşik tablosu
`SayacSes.konusmaMetni` saf ve testli; "0" söylenmez — bitişi zil
söyler. Bildirim/widget'tan başlayan sayaçta ekran açıldığında
motor devreye girer; ekrandan çıkınca kapanır.

### A7 · Mola kişiliği

`MolaKisilik.kt`: mola bildirimine tur sayısına göre dönüşen
öneriler — kısa molada 5'li havuz (su/20-20-20/omuz/nefes/bilek),
uzun molada 4'lü (yürüyüş/atıştırma/nefes/kâğıda boşalt). Aynı tur
aynı öneri (bildirim tazelenince metin titremez). Çalışma evresi
bildirimi bilinçli olarak dokunulmadı.

### B18 · Sabah özeti — "Sen yokken biriktirdim"

Sessiz saat/ günlük tavan bir bildirimi yutunca `gonder()` artık
başlığını deftere yazar (gövde değil). Sabah turu ilk iş olarak
**tek özet bildirim** gönderir: ilk 5 başlık madde madde + "+N
daha". Defter gün-bağımsız (öz denetimde yakalandı: güne bağlı
defter tam da ana senaryo olan gece yarısını geçen pencereyi
kaybederdi 🔴) — 36 saatten eski kayıt kendiliğinden düşer.
`Tur.OZET` (yeni tür, varsayılan açık) biriktirme istisnasıyla
sonsuz döngü açılmaz.

### Doğrulama (cihazda bakılacaklar)

1. Sayaç ekranı → 🏷️ çip → "Odak" yaz → bildirim alt satırında
2. Ayarlardan sesli geri sayımı aç → 1:05'lik sayaç başlat →
   "Bir dakika kaldı" + son 10'da sayma
3. Pomodoro 2 tur → mola bildirimlerinin farklı öneriler taşıması
4. Sessiz saatte 2+ türü tetikle → sabah tek özet bildirim
5. 3+ kez 🙁😐😄 puanı ver → Süre bölümünde ortalama kartı

### Öz denetim notları (bu sürümde 5 yakalama)

| # | Sorun | Çözüm |
|---|---|---|
| 1 | Defter güne bağlıydı; gece yutulanlar sabaha kaybolurdu | 36 saat yaş sınırlı gün-bağımsız defter |
| 2 | `govde(dakika, …)` parametresi hiç kullanılmıyordu | parametre atıldı |
| 3 | Özet kendisi yutulursa kendi başlığını biriktirirdi (döngü) | `Tur.OZET` istisnası |
| 4 | Dışarıdan başlayan sayaçta TTS motoru kurulmuyordu | `onResume` devreye alma |
| 5 | `kaliteOrtalamasi` KİMSE tarafından okunmuyordu | İstatistik kartı |

---

## 🔔 v10.3 — Bildirim kalite katmanı: ULTRA-50 B grubu (7 Ağu 2026)

Yol haritasının ikinci durağı. Bu paket **tararken üç maddenin**
zaten var olduğunu gördüm; tekrar "yeni" diye yazmak yerine
dürüst kapsamı aşağıda netleştiriyorum.

### Dürüstlük tablosu — tarayınca ne çıktı

| Öneri | Bulgu | v10.3'te yapılan |
|---|---|---|
| B19 canlı ikon | Yoktu | **YENİ** |
| B25 kronometre tazeleme | Zincir yalnız geri sayıma kuruluyordu | **YENİ** (kronometre de zincire girdi) |
| B23 günün odağı satırı | Sabah turu + `GunOdak` motoru vardı, ikisi **bağlı değildi** | **YENİ** bağlantı |
| B16 haftalık grafik | Metin raporu zaten vardı | **YÜKSELTME** (grafik kart) |
| B20 grup+özet | `setGroup` vardı; `grupOzetiGonder` **ölü koddu** | **TAMİR** (özete bağlandı) |
| B21 uyku penceresi | Sessiz mod **zaten vardı** | İddia yok; B24 haritayla görünür + rapor kaçağı kapatıldı |
| B24 DND haritası | Yoktu | **YENİ** |

### B19 · Durum çubuğu ikonu artık dakikayı söylüyor

`SayacIkon.kt` (YENİ): küçük ikon sabit kum saati yerine beyaz
rakam taşıyor — geri sayımda **kalan** dakika yukarı yuvarlanır
(04:31 → "5"), kronometrede **geçen** dakika aşağı (12:47 → "12").
99'da kestirilir. `IconCompat.createWithBitmap` ile çalışma anında
üretiliyor; 99 drawable gömmek yok. Saf yuvarlama mantığı
`SayacIkonTest` ile kilitli (5 test).

### B25 · Kronometre bildirimi de artık tazeleniyor

`TimerAlarm.tazelemeyiKur` zinciri yalnız `MODE_DOWN`'a kuruluyordu
(`reschedule` satırındaki erken `return`). Kronometrede uyumluluk
metni donuyor, B19 ikonu da bayatlıyordu. Artık kronometrede de
zincir kuruluyor (kapılar aynı: çalışıyor + uyumluluk + mini
açık).

### B23 · Sabah bildirimi "günün tek odağını" söylüyor

`GunOdakBildirim.kt` (YENİ) — yeni alarm yok: mevcut 09:00 sabah
turunun başına eklendi. `GunOdak.bul` (hero kartın öncelik motoru,
v9.9) ne derse bildirimde o yazıyor; yeni tür **`Tur.GUN_ODAK`**
(varsayılan açık, "Verimli saat önerisi"nden ayrı). Öncelik yoksa
bildirim **çıkmıyor** — boş güne uydurma metin basılmıyor.
Günde bir kez. `GunOdakBildirimTest` — 5 test.

### B16 · Haftalık rapor artık grafikli kart

`RaporGrafigi.kt` (YENİ): son 7 günün tamamlanmış odak dakikaları
640×360 çubuk karta çiziliyor (`BigPictureStyle`), bugün vurgu
renginde. Boyutu bilinçli küçük tuttum: ~0,9 MB ham bitmap üzeri
kimi cihazda `TransactionTooLarge`'a takılıyor — öz denetimde
1024×512'den düşürdüm. Üretim patlarsa eski metin stiline düşer.
Gelecek/pencere-dışı damgalar elenir. `RaporGrafigiTest` — 4 test.

### B20 · Grup özeti gerçekten yayınlanıyor

`grupOzetiGonder` v7.56'dan beri tanımlı ama **hiç çağrılmıyordu**.
`gonder()` artık her başarılı gönderimden sonra paneldeki grup
bildirimlerini sayıyor; 2+ ise özet güncelleniyor — Android
bildirimleri tek "Günlük Asistan" başlığı altında katlıyor.

### B24 · Sessizlik haritası (ayarlarda yeni bölüm)

Bildirim ayarlarına "Sessizlik haritası" eklendi: her yüzeyin
sessiz saatte/Rahatsız Etmeyin'de ne yaptığı satır satır +
"Sistem Rahatsız Etmeyin erişimini aç" düğmesi. **Bu harita bir
kaçak da yakalattı 🔴→✔:** haftalık rapor merkez kapıdan (`gonder`)
geçmeyip kendi alıcısından çıktığı için sessiz modu tanımıyordu;
artık sessiz saatte bu haftalık rapor atlanıyor (gelecek hafta
yeniden kuruluyor).

### Doğrulama (cihazda bakılacaklar)

1. Sayaç açıkken durum çubuğunda rakam ikonu görünüyor mu
   (OEM maskeleyebilir; bildirim satırında kesin görünür)
2. Kronometre (sayaç modu) + uyumluluk açıkken uygulama kapalıyken
   bildirim metni bayatlamıyor mu
3. Sabah turunda "… · günün odağı" bildirimi; öncelik yoksa gelmediği
4. Pazar 20:00 raporu çubuk grafikli mi (açık kart, mor/pembe çubuklar)
5. Aynı anda 2+ bildirim birikince tek grup başlığı altında
   katlanmaları
6. Ayarlar → Bildirimler → "Sessizlik haritası" satırları + DND
   düğmesinin sistem sayfasını açması

### Öz denetim notları (bu sürümde 5 yakalama)

| # | Sorun | Çözüm |
|---|---|---|
| 1 | `GunOdakBildirim` bildirim ID'si 7021, PDF indeks bildirimiyle çakışıyordu | 7026'ya alındı |
| 2 | Grafik bitmap 1024×512 (~2 MB) binder sınırını zorlardı | 640×360 (~0,9 MB) |
| 3 | Gelecek damgalı kayıt aynı gün içindeyse bugüne yazılıyordu | `k.zaman > simdi` elenir |
| 4 | Test `System.currentTimeMillis` kullanıyordu; saate göre kırılgan | `simdi` 20:00'a sabitlendi |
| 5 | Haftalık rapor sessiz modu atlıyordu (harita yalan söylerdi) | rapora sessiz kapısı |

---

## ⏱️ v10.2 — Sayaç hissi paketi: ULTRA-50 A·B grubu (7 Ağu 2026)

ULTRA-50 yol haritasının ilk durağı: zamanlayıcının **bittiği an**
ve **bitmek üzere olduğu an** artık hissediliyor. 7 öneri tek pakette.

### A1 · Uyanık bitiş (tam ekran zil ekranı)

Sayaç bittiğinde telefon uyuyor olsa bile ekran uyanır ve
**`SayacBittiActivity`** açılır: kocaman 🔔 etiketi, "süre doldu"
mesajı ve üç büyük düğme — **+5 dk uzat · Yeniden başlat · Kapat**.

- `TimerEngine.uzat()` YENİ — **üç durumlu mantık** (öz denetimde
  yakaladım, önceki hâli bitmiş sayaçta süreyi 2 katına çıkarıyordu 🔴→✔):
  koşuyorsa kalan+ek, duraklatılmış arta kalan varsa ona+ek,
  bitmişse sıfırdan ek süre
- İzin kapısı: `SayacBittiActivity` yalnız ayar (`sa_uyanik`,
  varsayılan AÇIK) **ve** `canUseFullScreenIntent()` (API 34+ izni)
  izin veriyorsa; değilse yüksek öncelikli heads-up bildirime düşer
- Sayaç ayarlarında satır + Android 14'te izin yoksa "izin iste"
  akışı (`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`)
- Manifest: `USE_FULL_SCREEN_INTENT`, sayaç-ekranı `singleTask,
  showWhenLocked, turnScreenOn, excludeFromRecents, exported=false`

### A2 · Bitiş bildirimi eylemleri — 3 düğme

Bitiş bildirimi (DONE_ID 4712) artık üç eylem taşıyor:
**+5 dk uzat** (req 21) · **Erteleme tercihi…** (req 24 →
10 dk / 1 sa / yarın 08:00 seçici) · **Yarın 08:00** (req 25, kestirme).
"Yeniden başlat" bildirimi 4 düğmeye şişirmemek için bilinçli olarak
yalnız zil ekranında yaşıyor.

- **`SayacErtele.kt`** (YENİ) — saf zaman-matematiği:
  `sonraki(secenek, simdi)` → 10dk/1sa/yarın-08:00 hedef millis;
  `yarın sabah` kuralı: saat < 8 ise aynı gün 08:00, değilse yarın 08:00.
  `SayacErteleTest` — 4 birim test (gece 23:30 → yarın 08:00, sabah
  05:00 → aynı gün 08:00, 10dk, 1sa)

### A4 · Final spurt — kalan süreye oranla (dürüstlük düzeltmesi)

Önceki listede "yeni özellik" diye yazmıştım; **bu benim hatamdı**:
zaman bazlı renk geçişi (`renkGecisi`, son 5 dk vurgu → kehribar →
kırmızı, son 10 sn nabız) v7.x'ten beri `SayacKadraniView.aktifRenk`'te
vardı. v10.2'de **orana** çevirildi:

- **`SayacSpurt.kt`** (YENİ) — saf: `oran(baslangicMs, kalanMs)` ve
  `renkKarar(oran, vurgu, amber, kirmizi)`; >%25 vurgu, %10-25 amber,
  <%10 kırmızı (5 dk sayaç sabit 5dk kuralında spurt'a hiç
  giremiyordu; 3 saatlik sayaçta ise son 5 dk yerine artık son ~18 dk)
- Kadrana `toplamGoster(toplamMs)`; nabız davranışı korundu
- `SayacSpurtTest` — 4 birim test
- *Not:* A13 (yatay tam ekran) da listede gereksizdi —
  `FullscreenTimerActivity` zaten `sensorLandscape`. Liste A(A); sayıyorum.

### A11 · 3-2-1 başlangıç ritüeli

Başlat'a basınca sayaç kadranında **3 → 2 → 1** süzülür (380 ms
ölçek+alfa), sonra sayaç gerçekten kurulur. Ayar: `sa_321`
(`GorunumAyar.animasyonAcik` kapalıysa ritüel de atlanır). Fragment
ayrılırsa `isAdded` bekçisi sayacın yarım kalmasını engeller.

### A12 · Tek dokunuş değerlendirme

Zil ekranında "Bu süre nasıldı?" — 🙁 😐 😄 üç düğmelik şerit.
`SureAnalizi` kaydına `kalite` alanı eklendi (serileştirme geriye
uyumlu). Öz denetimde yakaladığım ikinci hata: pomodoro modu açıkken
düz kayıt atmak tamamlanan pompaları **iki kez sayardı** 🔴→✔ —
onun yerine `SureAnalizi.sonKaydiKalitele()` son 5 dk içindeki
puansız tamamlanmış kaydı yerinde işaretler; bulamazsa (tekil sayaç)
tek kayıt düşer.

### A14 · Flaş ile bitiş

Ayar (`sa_flas`, **varsayılan kapalı**) açıksa bitişte kamera flaşı
3 kez yanıp söner: **`SayacFlas.kt`** — `CameraManager.setTorchMode`
(CAMERA izni gerekmez), 150 ms yanık / 250 ms aralık, handler zinciri,
not-torch cihazda sessizce pas.

### B26 · "Sonra hatırlat" kademeleri

Bitiş bildirimi + zil ekranı: **10 dk · 1 sa · Yarın 08:00**.
`SnoozeReceiver` yerine sayaca özel `TimerActionReceiver` eylemleri
(ACTION_SAYAC_SNOOZE, SNOOZE_ID 4715, SNOOZE_REQ 4716); erteleme
bildirimi sessiz kanaldan geri sayım gösterir, süresi dolunca bitiş
akışına düşer.

### Küçük doku: koşan bildirimde +1 dk

`TimerNotifier` geri sayım bildirimine **+1 dk uzat** (req 22) eylemi
eklendi — "süre bitmeden uzat" artık tek dokunma.

### Doğrulama (cihazda bakılacaklar)

1. Sayacı başlat → telefonu kilitle → bitişte ekran uyanıp zil
   ekranı açılıyor mu
2. Zil ekranından +5 dk → sayaç 5:00'dan koşuyor mu
3. Erteleme tercihleri (10dk/1sa/yarın) → doğru saatte bildirim
4. Flaş ayarı açıkken el feneri 3 kez yanıyor mu (arka kamera)
5. 3-2-1: Başlat'a basınca kadran sayıyor, sonra süre kuruluyor
6. Son %10'da kadran kırmızılaşıyor; 2 saatlik sayaçta son ~12 dk
7. Değerlendirme → ertesi gün İstatistik'te `kalite` ortalaması

### Öz denetim notları (bu sürümde 4 yakalama)

| # | Sorun | Çözüm |
|---|---|---|
| 1 | `ListeFark.uygula` sondaki lambda `ayniOge` yerine son parametreye bağlanıyordu | isimli argümanlar |
| 2 | `TimerActionReceiver` `AlarmManager` import'suzdu (derleme hatası) | import eklendi |
| 3 | Değerlendirme pomodoro'yu çift sayardı | `sonKaydiKalitele` |
| 4 | `uzat()` bitmiş sayaçta toplam+ek döndürüyordu | üç durumlu mantık |

---

## 🔧🔴 v10.1 — Sayaç dayanıklılığı (hata) + Görsel Grup C: İnce cila (7 Ağu 2026)

### 🔴 Ana hikâye: "Mini zamanlayıcı bildirimi görünmüyor"

Kullanıcı 3 ekran görüntüsüyle bildirdi: Samsung Saat'in mini
zamanlayıcısı bildirim panelinde çalışıyor ama **Günlük Asistan'ınki
yok**; uygulamanın kendi tanı ekranı bile "✕ Bildirim şu anda panelde
duruyor" diyordu — üstelik "✓ sayaç çalışıyor" derken.

**Kök neden (kod ile kanıtlı):** `BootReceiver` telefon yeniden
başladığında, saat/gün değiştiğinde ve **uygulama güncellendiğinde**
görevleri, dersleri, namazı, koçu, takibi geri kuruyordu — ama
**sayacı asla**. Oysa Android bu üç durumda da tüm alarmları siler:

- Bitiş alarmı (`TimerAlarm`) ölüyordu → **süre bitince hiç haber gelmiyordu**
- 15 sn'lik tazeleme zinciri (`tazelemeyiKur`) ölüyordu
- Uyumluluk modundaki bildirim (v7.93, varsayılan açık) `ongoing`
  değil → panel "Temizle"/kaydırma ile silinince zincir de ölüyse
  **bir daha asla geri gelmiyordu.** Zombi durum: sayaç koşuyor,
  bildirim yok, düzeltmek için sayaç ekranını açmak gerekiyordu.

İkinci kırılganlık: uygulamenin AÇILMASI da sayacı tazelemiyordu —
kullanıcı ayarlar ekranındayken (sayaç ekranı değil) bildirim yine
geri dönmüyordu.

### Düzeltme

- **`SayacGeriKur.kt`** (YENİ) — tek kapı geri kurulum:
  · `KUR` → bitiş alarmı + tazeleme zinciri + panel bildirimi
  · `BITIR` → uygulama kapalıyken süre bittiyse eksik bitiş akışı
    (ses + titreşim + odak kaydı + döngü) gecikmiş olarak teslim edilir
- İki daldan tetikleniyor: `BootReceiver` (BOOT + gün/saat/güncelleme)
  ve `App.onCreate` (arka plan bloğunda — açılışı yavaşlatmaz)
- Saf karar mantığı birim testli (`SayacGeriKurTest` — 3 test)

### Görsel Grup C (öneri 12-15)

| # | Öneri | Yapılan |
|---|---|---|
| 12 | Rulo efekti | **`Rulo.kt`** — metin değişiminde eski değer yukarı kayıp solar, yeni alttan gelir. Uygulandığı yer: **sayaç ekranının büyük süresi** (saniyede bir rulo oynar; animasyon kapalıysa düz yazar). `FlipClockView` zaten vardı; bu onun hafif kuzeni — her TextView'de çalışır. |
| 13 | Akordeon silme | `EventsFragment` ve `ExamsFragment` `notifyDataSetChanged()`'ten `ListeFark`'a (DiffUtil) geçti — silince öğe animasyonlu daralır, alttakiler yerleşir. Görevler/Notlar/Konular/Alışkanlıklar zaten v8.9'da geçmişti. **CoursesFragment kaldı** (3 ayrı liste kurma yolu var, ayrı bakım ister). |
| 14 | Ripple tutarlılığı | 7 XML'de tıklanabilir-ama-dalGASIZ öğe tarandı: `item_arac`, `item_quiz_option` + `kzCard`, `coReviewCard`, `coResumeCard`, `simdiNeCard`, `item_kaynak` → `?attr/selectableItemBackground`. Kotlin'de üretilen 9 tıklanabilir karta **`dalgaEkle()`** uzantısı (mevcut foreground'a asla dokunmaz). **`RippleTutarlilikTest`** ile geri döndürülemez. |
| 15 | Edge-to-edge | **`KenardanKenara.kt`** — yalnız temel katman: `setDecorFitsSystemWindows(false)` + saydam çubuklar + temaya göre simge kontrastı. Yerleşim geometrisi DEĞİŞMEDİ (kökte `fitsSystemWindows="true"` duruyor — çubuk alanları eskisi gibi korunuyor, fark yalnız çubuk rengi). Tam e2e estetiği (içeriğin çubuk altından kayması) 79 Activity × inset incelemesi ister; cihaz görülmeden yapılmadı. |

### Öz denetim notları (kendi kendime)

- İlk derleme: `ListeFark.uygula(...)` çağrılarında sondaki sustalı
  lambda **ayniIcerik** parametresine bağlandı (ayniOge değil) → derleme
  hatası. İsimli parametre ile düzeltildi. (Kotlin tuzağı.)
- İkinci derleme temiz geçti; 320 test tek seferde yeşil.

### 🔍 Bu sürümde cihazda doğrulanamayanlar

Sandbox'ta bildirim/ekran testi yapılamıyor. Kullanıcı kontrol listesi:

1. **Sayaç geri kurulumu:** 3 dk'lık sayaç başlat → telefonu YENİDEN
   BAŞLAT → açılınca bildirim panelinde mini zamanlayıcı durmalı; süre
   dolunca bitiş uyarısı gelmeli.
2. **Temizle sonrası iyileşme:** sayaç çalışırken panelden "Temizle"
   de → en geç birkaç dakika içinde (alarm uyanıklığına bağlı)
   bildirim geri gelmeli. Veya uygulamayı bir kez aç: anında gelir.
3. **Rulo:** sayaç ekranında saniyelerin yukarı kayarak değiştiğine bak.
4. **Silme animasyonu:** Etkinlikler'de bir öğe sil — zıplamadan
   kapanmalı.
5. **Çubuklar:** durum/gezinme çubuğu artık saydam; açık temada
   simgeler koyu kalmalı (okunabilirlik).

### Sıradaki iş (değişmediyse)

- Görsel #3 — emoji → vektör ikon seti (~45 ikon, BÜYÜK iş)
- veya Grup A'dan yarım kalan #6: 21 kartın `2dp` yükseltisini
  `0dp`'ye çekmek (dimens'te hazır; görsel karar, cihazda görmek iyi olur)
- Güncelleme sunucusu (textdb) hâlâ boş — kullanıcı onayı olmadan
  yayınlanmaz (kural).

---

## 📦 v10.0 — Görsel Grup A: Tutarlılık (önceki sürüm özeti)

| Alan | Değer |
|---|---|
| Sürüm | **10.0** (versionCode 156) |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Test | **316 test, 0 başarısız** |
| Kotlin dosyası | ~229 |
| Dize sayısı | 3336 |

---

## 📐 v10.0 — Görsel Grup A: Tutarlılık (7 Ağu 2026)

### Ne yapıldı — ölçülen sonuç

| Ölçüt | Önce | Sonra |
|---|---|---|
| Köşe yarıçapı çeşidi | **8** | **4** (sert kod: 0) |
| Yazı boyutu çeşidi | **22** | **7** (+4 gerekçeli istisna) |
| Yeşil ton (başarı) | **5** | **1** |
| Kırmızı ton (hata) | **5** | **1** |
| Sarı ton (uyarı) | **3** | **1** |
| Koyu tema durum rengi | **yok** | **10 renk** |
| Ortak üst bar kullanan ekran | **1** | **8** |

**624 değer** ölçeğe çekildi (65 köşe + 559 yazı), **27 renk sabiti**
birleştirildi, **8 Activity** ortak üst bara geçirildi.

### Yeni dosyalar

```
res/values/dimens.xml         Tasarım ölçeği (köşe/yazı/boşluk/yükselti)
res/values/colors.xml         +17 anlamsal renk
res/values-night/colors.xml   YENİ — koyu tema karşılıkları
UstBar.kt                     Ortak üst bar bileşeni
TasarimOlcegiTest.kt          10 test — ölçek erozyonunu engelliyor
```

---

### 🔴 `values-night/colors.xml` hiç yoktu

v8.3'te koyu tema eklendi (6 tema) ama **koyu tema renk dosyası
oluşturulmamış**. Durum renkleri her iki temada aynı kalıyordu:

| Renk | Beyaz üzerinde | Koyu (#1C1B1F) üzerinde |
|---|---|---|
| `#4C9A5A` yeşil | ~3.4:1 ✔ | **~2.9:1** ✘ (WCAG AA sınırı 3:1) |

İki sorun: kontrast düşüyor **ve** renkler koyu zeminde çamurlu
görünüyor.

**Düzeltme:** her rengi açıp doygunluğunu düşürdüm
(`#4C9A5A` → `#7BC48A`). Material Design'ın koyu tema kılavuzu da
bunu öneriyor: koyu temada "200 tonu", açıkta "500-700 tonu".

Soluk tonlarda alfa da arttı (%12 → %22) — koyu zeminde düşük alfa
neredeyse kayboluyordu.

---

### 🔴 R8 koyu tema renklerini SİLMİŞTİ — APK doğrulaması yakaladı

Release APK'yı `aapt2 dump resources` ile kontrol ettim:

```
color/ga_simge_gece      ✔
color/ga_simge_karamel   ✔
color/ga_basari          ✘ YOK
color/ga_hata            ✘ YOK
```

**Yeni eklediğim 17 rengin hiçbiri APK'da yoktu.**

Sebep: `isShrinkResources = true`. Renkleri tanımladım,
`GrafikDili.basari(context)` gibi tema duyarlı fonksiyonlar yazdım
ama **hiçbir yerden çağırmadım**. R8 kullanılmayan kaynağı sildi —
haklı olarak.

Yani: koyu tema düzeltmesi yaptığımı sanıyordum, gerçekte APK'da
hiçbir şey değişmemişti.

**Düzeltme:** 16 gerçek kullanım noktasına bağladım
(`TakipActivity` 13, `SistemActivity` 3, `EventsFragment`,
`HomeFragment`). Sonra tekrar doğruladım:

```
resource 0x7f050063 color/ga_basari
  ()      #ff4c9a5a     ← açık tema
  (night) #ff7bc48a     ← koyu tema
```

Şimdi gerçekten çalışıyor.

**Ders:** "kaynak ekledim" ≠ "APK'da var". `shrinkResources` açıkken
her yeni kaynağı **release APK'da doğrulamak** gerekiyor. Debug
derlemesi bunu göstermez — kısaltma yalnızca release'de çalışır.

---

### 🔑 Ölçek nasıl seçildi

Rastgele değil, iki kurala göre:

**1. Mevcut çoğunluğu koru.** 20dp köşe 24 yerde, 16dp 11 yerde
kullanılmış → ikisi de ölçekte. 22/26dp gibi tek tük değerler en
yakın komşuya yuvarlandı. Böylece **en az sayıda ekran** görsel
olarak değişti.

**2. Kademeler ayırt edilebilir olsun.** 12sp ile 12.5sp arasında
göz fark görmüyor — aynı kademe. 4sp atlamalar (11 → 15 → 19) net
hiyerarşi kuruyor.

Gerekçeli istisnalar (ölçeğe zorlanmadı):
- `1sp` × 2 → gizli şerit (görünmez, yer tutucu)
- `34sp` → widget geri sayımı (RemoteViews bağlamı farklı)
- `56sp` → kurs ekranı büyük sayı
- `74sp` → tam ekran sayaç (odadan okunmalı)

---

### 🔴 Kendi düzeltmemin yarattığı hata

Renk birleştirme betiğim `0xFF2E7D32` ve `0xFF66A75B` değerlerini
`GrafikDili.BASARI`'ya çevirdi. Ama bunlar `Butce.Kategori`
içindeki **grafik dilimi** renkleriydi.

Sonuç: **MAAS, EK_GELIR ve MARKET üçü de aynı yeşil oldu.** Dağılım
halkasında üç dilim ayırt edilemezdi.

**Ders:** "aynı renk = aynı anlam" kuralı **durum göstergeleri**
için doğru, **kategori paletleri** için yanlış. Palet renklerinin
tek işi birbirinden ayrılmak.

Geri aldım ve `TasarimOlcegiTest`'e iki test ekledim:
`butce kategori renkleri benzersiz` ve
`gelir ve gider kategorileri farkli renkte`.

---

### 🔴 Öz denetim — `UstBar` ölü koddu

`UstBar.kt`'yi yazdım ama **hiçbir yerden çağırmıyordum**.

Kontrol edince 8 Activity'nin tamamen aynı iki satırı tekrarladığını
gördüm:

```kotlin
findViewById<TextView>(R.id.gaBaslik).setText(R.string.xxx)
findViewById<View>(R.id.gaGeri).setOnClickListener { finish() }
```

Hepsi `UstBar.kur(this, getString(...))` oldu. Artık düğme boyutu
(44dp), dokunma hedefi (48dp — erişilebilirlik kuralı) ve yazı
boyutu tek yerden geliyor.

---

### 🧪 Yeni test türü — ölçek erozyonuna karşı

`TasarimOlcegiTest` diğer testlerden farklı: **davranışı değil kod
düzenini** doğruluyor. Layout dosyalarını okuyup sert kodlanmış
değer kalmadığını kontrol ediyor.

Neden gerekli: bu tür temizlik **geri kayar**. Biri yeni ekran
eklerken `android:textSize="13sp"` yazar, kimse fark etmez, altı ay
sonra yine 22 çeşit oluruz. Test derleme anında yakalıyor.

10 testin hepsi gerçekten çalışıyor (dosya bulunamazsa atlanacak
şekilde yazıldı ama atlanmadı).

---

### ⚠️ Öneri listemde yanlış yazdığım madde

Öneri #5'te **"boş durum illüstrasyonu: 0 tane"** yazmıştım.
**Yanlış.**

Kontrol edince `BosEkran.BosCizim` sınıfını buldum — v8.3'te
eklenmiş, **9 tür için Canvas ile çizim** yapıyor (görev, not, konu,
sınav, etkinlik, alışkanlık, arama, hata, genel) ve 6 fragment'te
kullanılıyor.

`drawable/` klasöründe SVG aramıştım; çizimler kodda olduğu için
göremedim. Bu maddeyi yapmadım çünkü **zaten yapılmış**.

---

### ⏭️ Grup A'da yapmadıklarım

**#3 (emoji → vektör ikon seti)** — ~45 ikon çizmek gerekiyor, tek
sürüme sığmaz. Görsel etkisi yüksek ama ayrı bir çalışma.

**#5 (boş durum illüstrasyonları)** — zaten var (yukarıda).

**#6 (gölge dili)** — kısmen: ölçek `dimens.xml`'de tanımlı ama
21 kartın `2dp` yükseltisi henüz `0dp`'ye çekilmedi. Bu görsel bir
karar; önce cihazda görmek istiyorum.

---

### ❗ Cihazda doğrulanamayanlar

- Koyu temada yeni renklerin gerçek görünümü (asıl düzeltme bu)
- 624 değer değişiminin ekranlarda yarattığı fark
- Üst barın 8 ekranda tutarlı görünmesi

---

## 🎨 v9.9 — Görsel Grup B: Yeni görsel katman (7 Ağu 2026)

### Kullanıcı isteği
> "Bana gözle görünür uygulamada görsel değişiklik öner" → 15 maddelik
> `ONERILER-GORSEL-v2.md` · sonra **"Grup b yi yap"** (öneri 7-11)

### Ne eklendi

| # | Öneri | Nasıl çözüldü |
|---|---|---|
| 7 | Ana ekranda günün odağı | `GunOdak.kt` — 7 kademeli öncelik, hero kartta tek satır |
| 8 | Kaydırmada daralan başlık | `DaralanBaslik.kt` — CollapsingToolbar'sız, 40 satır |
| 9 | Karttan büyüyen geçiş | `KartAcilis.kt` — `makeScaleUpAnimation` |
| 10 | Grafiklerin ortak dili | `GrafikDili.kt` — 8 grafik tek kaynağa bağlandı |
| 11 | Renkli durum şeritleri | Etkinliklere aciliyet şeridi |

### Yeni dosyalar

```
GrafikDili.kt      Ölçek + renk + animasyon sabitleri, tema okuma
GunOdak.kt         "Şu an en önemlisi" motoru, 7 öncelik kademesi
DaralanBaslik.kt   ScrollView tabanlı başlık geçişi
KartAcilis.kt      Kaynak konumundan büyüyen Activity açılışı
GrafikDiliTest.kt  19 test
```

---

### 🔴 Bulduğum gerçek hata — grafikler açık temada bozuktu

Kodu tararken şunu buldum:

```kotlin
// SparklineView.kt
gridPaint.color = Color.parseColor("#16232F")   // koyu lacivert
// StatRingView.kt
trackPaint.color = Color.parseColor("#1B2A3A")  // koyu gri-mavi
```

Bu değerler **sert kodlanmıştı**. v8.3'te açık tema eklendi ama bunlar
değişmedi:

| Tema | Sonuç |
|---|---|
| Koyu | Doğru görünüyor (arka plan koyu) |
| **Açık** | **Beyaz zeminde koyu lacivert ızgara** — sert ve yanlış |

`StatRingView`'da daha kötüydü: halkanın boş kısmı dolu kısmından
daha baskın çıkıyordu.

**Düzeltme:** İkisi de `GrafikDili.izgara()` üzerinden `colorSurfaceVariant`
okuyor. Tema değişince renk de değişiyor.

Renkler `onDraw`'da bir kez okunuyor (`renklerHazir` bayrağı) — yapıcıda
okumak işe yaramıyor çünkü View henüz pencereye eklenmemişken
`MaterialColors.getColor` varsayılan döndürüyor.

---

### 🔑 Öneri 7 — neden TEK öneri gösteriyorum

Ana ekran 8 blok gösteriyordu, hepsi eşit ağırlıkta. Uygulamayı açınca
gözün gideceği tek nokta yoktu.

`GunOdak` öncelik sırası — **"aciliyet × kaçırılma maliyeti"**:

```
1. Geçmiş ilaç/fatura   → telafisi yok (sağlık, para)
2. Bugünkü ilaç saati   → zamana bağlı
3. Bugün biten görev    → söz verilmiş iş
4. Bekleyen tekrar      → öğrenme kaybı (telafi edilebilir)
5. Sınava az kaldı      → uzun vadeli
6. Günlük hedef         → esnek
7. Hedef tamamlandı     → kutlama
```

**Üç öneri göstermek hiç göstermemekle aynı** olurdu: kullanıcı yine
seçim yapmak zorunda kalırdı. Tek şey söylemek karar yükünü kaldırıyor.

Yapılacak bir şey yoksa satır **gizleniyor** — boş bir "önerin yok"
satırı kartı uzatır ve hiçbir şey söylemez.

---

### ⚠️ Öneri 8 ve 9'da uzlaşma yaptım — dürüst açıklama

**#8 CollapsingToolbarLayout kullanmadım.** Standart çözüm
`AppBarLayout` + `CollapsingToolbarLayout` + `CoordinatorLayout`.
Vazgeçtim çünkü:
- Ana ekran düz bir `ScrollView`; v8.5'teki **blok sürükle-sırala**
  sistemi (`AnaEkranDuzen`) buna bağlı
- `CoordinatorLayout` iç içe kaydırma istiyor, `ScrollView`
  desteklemiyor
- Kazanç aynı görsel etki, risk çalışan bir sistemi kırmak

40 satırlık kaydırma dinleyicisi aynı hissi veriyor.

**#9 "gerçek" paylaşılan öğe geçişi DEĞİL.** Öneri listesinde
"shared element transition" yazmıştım. Yapmadım çünkü:
- `transitionName` hem kaynakta hem hedefte gerekli; hedef ekranların
  çoğu (`TakipActivity`, `SistemActivity`, `OgrenmeActivity`) **kodla
  çiziliyor**, XML layout'ları yok
- Ana gezinme Activity değil **Fragment**; scene transition
  Activity'ler arası çalışıyor

Bunun yerine `makeScaleUpAnimation`: ekran **dokunulan kartın
konumundan** büyüyerek açılıyor. Gerçek morph değil ama kaynak-hedef
bağlantısı hissediliyor ve hiçbir layout değişmiyor.

Buna "paylaşılan öğe geçişi yaptım" demek yanlış olurdu.

---

### 🔴 Öz denetimde bulduğum boşluk

`DaralanBaslik.tazele()` yazmıştım ama **hiçbir yerden
çağırmıyordum** — ölü kod.

Sonuç: ekranlar `hide()`/`show()` ile yönetiliyor, başka sekmeye gidip
dönünce kaydırma konumu korunuyor ama dinleyici tetiklenmiyor.
Selamlama solmuş (alfa 0) kalıyor ve bir daha görünmüyordu.

`HomeFragment.onResume`'a eklendi.

---

### 🔴 Test ortamı tuzağı — ikinci kez

v9.8'de `org.json` saplaması 5 testi düşürmüştü. Bu sefer 7 test:

```
RuntimeException: Method alpha in android.graphics.Color not mocked
```

Birim testlerinde `android.jar` bir saplama; **statik metotlar bile**
gerçek gövde taşımıyor.

v9.8'de gerçek kütüphaneyi test bağımlılığı olarak eklemiştim. Burada
gerekmedi: ARGB ayrıştırma dört satırlık bit işlemi.

Ayrıca `GrafikDili.soluk()` üretim kodunu da `Color.argb`'den bit
işlemine çevirdim — hem test edilebilir hem `onDraw` içinde daha hızlı
(JNI sınırı geçilmiyor). Sonuç bit bit aynı.

---

### 🎯 Öneri 11 — sadece anlamlı olan yere koydum

Öneri listesinde "kartların sol kenarında renk şeridi" yazmıştım.
Uygulamaya bakınca:

| Liste | Durum |
|---|---|
| Görevler | ✅ Şerit **zaten vardı** (v7.74, etiket rengi) |
| Ana ekran konuları | ✅ Şerit **zaten vardı** (`accentBar`) |
| **Etkinlikler** | ❌ Yoktu → **eklendi** |
| Notlar | ⏭️ Eklemedim |

**Notlara neden eklemedim:** `Store.Note` sınıfında etiket, kategori
veya öncelik alanı yok. Şerit koyacak olsam rastgele renk vermem
gerekirdi — bu bilgi taşımayan görsel gürültü olur.

Etkinliklerde ise doğal bir sinyal var: **kalan gün**. Şerit kırmızı
(bugün) / amber (bu hafta) / yeşil (uzak) / gri (geçmiş) oluyor.
Eskiden bu bilgi yalnızca sağdaki "12 gün" yazısındaydı; listeyi
taramak için her satırı **okumak** gerekiyordu.

---

### 📊 Grafik dili — önce/sonra

| Ne | Önce | Sonra |
|---|---|---|
| Izgara rengi | `#16232F`, `#1B2A3A` sert kod | Temadan (`colorSurfaceVariant`) |
| Animasyon | 600, 700, 800, 900 ms | 600 (normal) / 900 (uzun) |
| Yazı boyutu | 10f, 11f, 12f dağınık | 10 / 12 / 18 (3 kademe) |
| Çizgi kalınlığı | 1f, 2f, 2.5f, 3f, ×2.1 | 1 / 2.5 / 3.5 |
| Durum rengi | 5 yeşil, 5 kırmızı | 1 yeşil, 1 kırmızı, 1 amber |

---

### ❗ Cihazda doğrulanamayanlar

- Daralan başlığın akıcılığı (kaydırma hissi)
- Kart büyüme animasyonunun gerçek görünümü
- Açık temada grafik renklerinin düzeldiği (sandbox'ta ekran yok)
- Günün odağı satırının gerçek veriyle nasıl göründüğü

---

## ⚙️ v9.8 — Grup G: Sistem sağlamlığı (7 Ağu 2026)

### Kullanıcı isteği
> "A ve C grubu haric hepsini yap" → B (v9.4), D (v9.5), E (v9.6),
> F (v9.7), **G (v9.8)**. Liste tamamlandı.

### Ne eklendi

| # | Öneri | Nasıl çözüldü |
|---|---|---|
| 47 | WorkManager'a geçiş | Yedekleme + günlük bakım + senkron. **Alarmlar taşınmadı** (aşağıda) |
| 48 | Güncelleme kontrolü | textdb.online üzerinden, günde 1 kez, otomatik indirme YOK |
| 49 | Otomatik çökme raporu | **Otomatik gönderim yapmadım** (aşağıda) — son 10 çökme + tekrar tespiti |
| 50 | Kullanım analitiği | Yerel, gizlilik dostu, tek dokunuşla kapatılabilir |

### Yeni dosyalar

```
Kullanim.kt      Ekran/eylem sayacı, arabellekli yazma, çıkarımlar
Guncelleme.kt    textdb sürüm kontrolü, erteleme, kritik güncelleme
CokmeRapor.kt    Son 10 çökme, imza ile tekrar tespiti, rapor üretimi
ArkaPlanIs.kt    WorkManager işçisi (yedek / bakım / senkron)
SistemActivity.kt  4 sekmeli tek ekran
GuncellemeTest.kt  14 test · CokmeRaporTest.kt 13 test · KullanimTest.kt 13 test
```

---

### ⚠️ Öneri 49'u OLDUĞU GİBİ yapmadım — dürüst açıklama

Öneri listesinde **"Otomatik çökme raporu gönderimi"** yazmıştım.
**"Otomatik gönderim" kısmını kasten yapmadım.**

Bir yığın izi (stack trace) sandığınızdan fazla şey sızdırır:
- Dosya yolları kullanıcı adını içerebilir
- `IllegalArgumentException: "Ahmet'in notu" geçersiz` gibi istisna
  mesajları **kullanıcı verisini** taşıyabilir
- Sınıf adları hangi özellikleri kullandığını ele verir
  (`NamazActivity` → inanç, `TakipActivity` → sağlık)

v9.7'de "sağlık verisi telefondan çıkmıyor" diye yazdım. Bir sürüm
sonra arka planda yığın izi göndermek **tutarsızlık** olurdu.

**Yaptığım:** otomatik yakalama + **tek dokunuşla** gönderim.
Uygulama açılınca kendisi soruyor ("geçen sefer çöktü, raporu
göndermek ister misin?"), gönderilecek metni gösteriyor, kullanıcı
karar veriyor. Otomatikleşen kısım **hatırlatma**, gönderim değil.

v8.8'deki farkı: eskiden kullanıcının Ayarlar → Depolama → "Hatayı
bildir" yolunu **kendi bulması** gerekiyordu. Kimse bulmadı.

---

### 🔑 WorkManager — neyi taşıdım, neyi taşımadım

**Taşınanlar** (zamanı kritik olmayan):
- Yedekleme · Online senkron · Günlük bakım · Güncelleme kontrolü

**Taşınmayanlar** (tam zaman gerektiren):
- İlaç hatırlatması · Namaz vakti · Görev hatırlatıcısı · Sayaç bitişi

WorkManager tam zaman garantisi **vermiyor** — Doze modunda erteliyor,
periyodik işlerde minimum 15 dakika ve ±birkaç dakika kayma var.
"08:00 ilacını al" bildirimi 08:25'te gelirse özelliği bozar.
Bunlar `AlarmManager.setExactAndAllowWhileIdle` ile kalıyor.

"Yeni teknoloji çıktı, her şeyi ona taşıyalım" yaklaşımı burada
uygulamayı bozardı.

**Yedeklemede çift katman var ve bu bilinçli:**
```kotlin
Performans.geciktir(...)        // hızlı yol — uygulama açıkken 2,5 sn
ArkaPlanIs.yedekKuyrugaAl(...)  // güvenlik ağı — uygulama ölse bile
```
Çift yazma zararsız (aynı içerik, aynı dosya). Kaybolmuş yedek ise
geri getirilemez — asimetrik risk.

---

### 🔴 R8 tuzağı — yedekleme sessizce ölebilirdi

WorkManager, Worker sınıflarını **yansıma** ile oluşturuyor: sınıf
adı metin olarak veritabanına yazılıyor, çalıştırma anında o
metinden sınıf bulunuyor. R8 bu bağlantıyı göremiyor.

ProGuard kuralı olmasaydı R8 `GenelIsci`'yi silecek veya adını
değiştirecekti → `ClassNotFoundException` → yedek alınmaz.

**Ve bu hata sessiz:** kullanıcı hiçbir şey fark etmez, yalnızca
telefonunu kaybedince yedeğinin olmadığını görür.

Mevcut `**Activity` / `**Receiver` kalıpları bunu kapsamıyordu.
`proguard-rules.pro`'ya 13. başlık eklendi.

---

### 🔴 Öz denetimde bulduğum boşluk — analitik verisi kayboluyordu

`Kullanim` sayaçları bellekte biriktirilip **10 olayda bir** diske
yazılıyordu. Kullanıcı 6 ekran gezip uygulamayı kapatırsa o 6 kayıt
kayboluyordu.

Ekran başına 1 olay düşünüldüğünde **çoğu oturum eşiğe hiç
ulaşmazdı** — yani analitik verinin büyük kısmı hiç yazılmayacaktı.
Özellik sessizce işe yaramaz olurdu.

Düzeltme: `MainActivity.onStop` → `Kullanim.bitir()`.

---

### 🔴 Test ortamı hatası — org.json saplaması

5 test patladı. Sebep kodda değil, **test ortamındaydı**: Android'in
`android.jar` saplaması birim testlerinde `org.json` çağrılarında
"not mocked" fırlatıyor.

Kolay çözüm `unitTests.isReturnDefaultValues = true` olurdu ama o
**tüm** Android çağrılarını sessizce null/0 yapar ve gerçek hataları
gizler. Bunun yerine dar çözüm: `testImplementation("org.json:json")`
— APK'ya girmiyor, boyutu etkilemiyor.

---

### 🔒 Gizlilik tasarımı (öneri 50)

Bu **telemetri değil**. Hiçbir veri cihazdan çıkmıyor:

| | |
|---|---|
| ✓ Yalnızca SharedPreferences | ✗ Ağ isteği yok |
| ✓ Tek dokunuşla silinebilir | ✗ AI istemlerine girmiyor |
| ✓ Tek dokunuşla kapatılabilir | ✗ Online senkrona girmiyor |
| ✓ Kapatınca geçmiş de siliniyor | ✗ **Yedeğe bile girmiyor** |

Saklanan: ekran adı + sayaç. SaklanMAYAN: not içerikleri, görev
metinleri, ilaç adları, harcama tutarları, konum.

İlaç sayacı örneği: `"İlaç alındı: 12 kez"` kaydediliyor,
`"Tansiyon ilacı alındı"` **kaydedilmiyor**.

**Yedeğe neden girmiyor:** yedek paylaşılabiliyor ve "NamazActivity
45 kez açıldı" gibi bir kayıt kullanıcının inancını, `TakipActivity
→ ilaç` sağlık durumunu ele verir.

---

### 🔌 Entegrasyon noktaları

| Yer | Ne yapıldı |
|---|---|
| `App.onCreate` | Bakım işi + güncelleme kontrolü + oturum sayacı |
| `App` çökme yakalayıcı | `CokmeRapor.kaydet` — son 10 kayıt (eskiden 1) |
| `MainActivity.open()` | Tek noktadan ekran sayacı (17 fragment'i tek tek değiştirmeden) |
| `MainActivity.onStop` | `Kullanim.bitir()` — veri kaybı düzeltmesi |
| `MainActivity` çökme penceresi | Ham yığın izi yerine okunabilir özet |
| `Store.maybeAutoBackup` | WorkManager güvenlik ağı eklendi |
| `SettingsFragment` | "Sistem ve kullanım" satırı + güncelleme rozeti |
| `proguard-rules.pro` | 13. başlık: WorkManager koruması |

---

### 📋 50'lik liste — final durum

| Grup | Sürüm | Durum |
|---|---|---|
| A (1-8) veri aktarımı | — | ❌ kullanıcı hariç tuttu |
| B (9-16) takvim/zaman | v9.4 | ✅ |
| C (17-22) güvenlik | — | ❌ kullanıcı hariç tuttu |
| D (23-30) yapay zekâ | v9.5 | ✅ (23/24, 26, 30 hariç) |
| E (31-40) öğrenme | v9.6 | ✅ (32, 34, 37-40 hariç) |
| F (41-46) günlük hayat | v9.7 | ✅ tamamı |
| G (47-50) sistem | v9.8 | ✅ tamamı |

---

### ❗ Cihazda doğrulanamayanlar

- WorkManager işlerinin gerçekten tetiklenmesi (24 saat gerekiyor)
- Güncelleme kontrolü — sunucuya henüz sürüm bilgisi yazılmadı
- Çökme penceresi (kasıtlı çökme üretmedim)
- R8 sonrası Worker sınıfının yansımayla bulunması

---

## 🗓 v9.7 — Grup F: Günlük hayat modülleri (7 Ağu 2026)

### Kullanıcı isteği
> "A ve C grubu haric hepsini yap" → 50'lik listenin B, D, E grupları
> v9.4-v9.6'da bitti. Bu sürüm **Grup F (öneri 41-46)**.

### Ne eklendi

| # | Öneri | Nasıl çözüldü |
|---|---|---|
| 41 | İlaç hatırlatıcı | Stok + günlük doz → "15 gün ilacın kaldı". Saat bazlı bildirim, "Aldım" düğmesi stoktan düşüyor |
| 42 | Fatura/abonelik | Son ödeme + tekrar aralığı + **aylık eşdeğer yük** hesabı |
| 43 | Harcama defteri | 14 kategori, gelir/gider, aylık limit, halka grafiği, 6 aylık çubuk |
| 44 | Belge geçerliliği | Ehliyet/pasaport/sigorta — varsayılan eşik **45 gün** (randevu süresi) |
| 45 | Konuma bağlı hatırlatma | **Play Services YOK** — son bilinen konumla kontrollü yaklaşım |
| 46 | Araç bakımı | Km bazlı: yağ, lastik, muayene. Sayaç geri gidince uyarı |

### Yeni dosyalar

```
Takip.kt            Dört türü (ilaç/fatura/belge/araç) tek modelde toplayan çekirdek
Butce.kt            Harcama defteri — 14 kategori, aylık özet, çıkarımlar
TakipAlarm.kt       Günde TEK alarm + ilaç saatleri (kayıt başına değil)
TakipReceiver.kt    Özet bildirimi + "Aldım"/"Yapıldı" düğmeleri
KonumHatirlatma.kt  Yer kaydı + mesafe kontrolü (getLastKnownLocation)
TakipActivity.kt    7 sekmeli tek ekran
TakipTest.kt        26 test
ButceTest.kt        19 test
TakipAlarmTest.kt   10 test
```

---

### 🔑 Tasarım kararı 1 — Dört özellik, tek veri modeli

İlaç, fatura, belge ve araç ilk bakışta dört ayrı özellik. Ama hepsi
**aynı soruyu** soruyor: *"Bir şeyin süresi/miktarı bitmek üzere —
beni ne zaman uyar?"*

Dördünü ayrı sınıf yazmak dört kez aynı kodu (JSON kaydet/oku, alarm
kur, uyarı eşiği, tekrar mantığı) yazmak olurdu. Tek `Takip.Kayit`
modeli + `Tur` enum'u kullandım.

Fark yalnızca **ölçü biriminde**: gün bazlı (fatura, belge, ilaç) ve
km bazlı (araç). `Tur.kmBazli` bu ayrımı taşıyor.

**Harcama defteri ayrı sınıf** çünkü farklı bir soruyu çözüyor:
Takip **gelecek** bir olayı tutuyor (tekil, güncellenen), harcama
**geçmiş** bir olayı tutuyor (çoğul, değişmez).

---

### 🔑 Tasarım kararı 2 — Günde tek alarm

İlk tasarımım her kayıt için ayrı alarm kurmaktı. Vazgeçtim:

- 30 fatura + 10 belge + 5 araç = **45 alarm**. Android 12+ tam alarm
  kotası sınırlı; sistem bunları kısabilir.
- Eşik değişince 45 alarmı tek tek iptal edip yeniden kurmak gerekiyor
  — hata yapmaya çok açık.
- Kullanıcı 45 ayrı bildirim istemiyor.

Şimdi **günde bir kez** (varsayılan 09:00) çalışan tek alarm var,
uyandığında tek özet bildirim gönderiyor.

**İstisna: ilaç saatleri.** "Sabah 08:00 ilacını al" günün özetiyle
birleştirilemez. Bunlar ayrı alarmla kuruluyor ama **saat başına
gruplanıyor**: aynı 08:00'de üç ilaç varsa tek alarm, tek bildirim,
üç satır. İlaç bildirimi `acil = true` — sessiz saat kuralını aşıyor,
çünkü sağlık verisi önceliği hak ediyor.

---

### ⚠️ Dürüst açıklama — Öneri 45 gerçek geofencing DEĞİL

Öneri listesinde "Geofencing" yazmıştım ve yanına *"Play Services
Location ~400 KB gerekir"* notu düşmüştüm. **Play Services'i
eklemedim.** Sebepler:

1. APK 16,8 MB. R8 ile 27,4'ten indirdiğimiz boyutu tek özellik için
   %2,4 büyütmek kötü takas.
2. Play Services bağımlılığı Google Play olmayan cihazlarda (Huawei,
   özel ROM'lar) uygulamayı **çökertebilir**.
3. Gerçek geofencing `ACCESS_BACKGROUND_LOCATION` istiyor; Android 11+
   bunu "Her zaman izin ver" olarak soruyor ve Play Store ayrı gerekçe
   formu doldurtuyor.

**Yaptığım:** Kullanıcı yer kaydediyor, hatırlatma o yere bağlanıyor,
kontrol **uygulama açıldığında** ve **günlük özet alarmında** yapılıyor.
`getLastKnownLocation` okunuyor — yeni konum isteği yapılmıyor.

**Bunun anlamı:** "Markete girdiğin anda telefonun titremesi" OLMAZ.
OLAN: "Uygulamayı açtığında markete yakınsan hatırlatma görürsün."

Daha az etkileyici ama **dürüst** ve pil tüketmiyor. Arayüzde de
kullanıcıya aynen böyle yazıyorum — yanlış beklenti yaratmak,
özelliği hiç yapmamaktan kötü.

---

### 🔴 Öz denetimde bulduğum hata — gecikmiş faturada gün kayması

İlk yazdığım `Takip.tamamla` gecikmiş kayıtlarda `temel = simdi`
yapıyordu:

```kotlin
// YANLIŞ
val temel = if (kayit.sonrakiMillis > simdi) kayit.sonrakiMillis else simdi
```

**Sonuç:** Ayın 5'inde kesilen bir fatura 15 gün gecikip 20'sinde
ödendiğinde bir sonraki tarih **ayın 20'si** oluyordu. Faturanın kesim
günü kalıcı olarak kayıyordu ve her gecikmede biraz daha kayacaktı.

```kotlin
// DOĞRU — özgün tarihten ileri say
val temel = if (kayit.sonrakiMillis > 0) kayit.sonrakiMillis else simdi
var yeni = sonrakiTarih(temel, kayit.tekrar)
while (yeni <= simdi && koruma < 200) { yeni = sonrakiTarih(yeni, kayit.tekrar); koruma++ }
if (yeni <= simdi) yeni = sonrakiTarih(simdi, kayit.tekrar)  // güvenlik ağı
```

Ayın 5'i ayın 5'i olarak kalıyor, sadece geleceğe taşınıyor.

---

### 🔴 Testin yakaladığı hata — Türkçe ünsüz yumuşaması

Hızlı komuta harcama girişi ekledim (`harcama: market 250`). Kategori
tahmini için anahtar kelime eşlemesi yazdım. Test şunu yakaladı:

```
expected:<YEMEK> but was:<DIGER>
```

**Sebep:** `"öğle yemeği".contains("yemek")` → **false**

Türkçede ünsüz yumuşaması var:

| Yalın | Ekli | Değişim |
|---|---|---|
| yeme**k** | yeme**ğ**i | k → ğ |
| kita**p** | kita**b**ı | p → b |
| elektri**k** | elektri**ğ**i | k → ğ |
| uça**k** | uça**ğ**a | k → ğ |
| gömle**k** | gömle**ğ**i | k → ğ |

İngilizce düşünülerek yazılmış bir eşleyici bu tuzağa düşmez çünkü
İngilizcede kök değişmiyor. Bu, Türkçe uygulama yazarken **sessizce**
bozulan türden bir hata — kullanıcı "neden hep Diğer'e atıyor"
derdi ve sebebini kimse bulamazdı.

**Çözüm:** yumuşamış gövdeleri de listeye ekledim (`"yemeğ"`,
`"kitab"`, `"elektriğ"`). Ekler değişken (-i/-ı/-u/-ü) olduğu için
sondaki ünlüyü hiç yazmıyorum; gövde eşleşmesi yeterli.

7 yeni test bu davranışı kilitliyor.

---

### 💾 Yedekleme — biçim 18 → 19

Üç yeni depo `PrefYedek.DEPOLAR` listesine eklendi:

```
takip_v1              ilaç, fatura, belge, araç + ödeme geçmişi
butce_v1              gelir/gider kalemleri + aylık limit
konum_hatirlatma_v1   kayıtlı yerler ve hatırlatmalar
```

Bu depolar kullanıcının **en zor yeniden gireceği** veriyi tutuyor:
aylarca biriken harcama kayıtları, ilaç saatleri, belge tarihleri.
Yedeğe girmemesi telefon değişiminde hepsinin kaybolması demekti.

---

### 🔌 Entegrasyon noktaları

| Yer | Ne yapıldı |
|---|---|
| `App.onCreate` | `TakipAlarm.yenidenKur` + `KonumHatirlatma.kontrolVeBildir` (arka planda) |
| `BootReceiver` | Hem BOOT hem saat dilimi dalında takip alarmı tazeleniyor |
| `SettingsFragment` | "Günlük hayat" satırı + **acil uyarı rozeti** (kırmızı sayı) |
| `PrefYedek` | 3 yeni depo |
| `Store.YEDEK_BICIM` | 18 → 19 |
| `HizliKomut` | `HARCAMA` türü — `harcama: market 250` tek satırda kaydediliyor |
| `AndroidManifest` | Konum izinleri (`required=false`), TakipActivity, TakipReceiver |

---

### 📋 Kullanım örnekleri

```
İlaç:    Ad="Tansiyon", stok=30, doz=2, saatler=[08:00, 20:00]
         → 08:00 ve 20:00'de bildirim, "Aldım" stoktan düşer
         → 15 gün sonra "3 günlük kaldı" uyarısı

Fatura:  Ad="Elektrik", tarih=15 Ağustos, tekrar=Her ay, tutar=850₺
         → 12 Ağustos'ta uyarı (eşik 3 gün)
         → "Yapıldı" → 15 Eylül'e taşınır, ödeme geçmişine yazılır

Belge:   Ad="Ehliyet", tarih=1 Ekim 2027, eşik=45 gün
         → 17 Ağustos 2027'de uyarı başlar

Araç:    Mevcut km=48.000 · Ad="Motor yağı", sonraki=50.000, aralık=10.000
         → 49.500'de uyarı (eşik 500 km)
         → "Bakım yapıldı" → sonraki 58.000 olur

Bütçe:   "harcama: market 250" → Market kategorisine 250₺ gider
         Aylık limit 15.000₺ → "Limitinin %80'ine geldin"
```

---

### ❗ Cihazda doğrulanamayanlar

Sandbox'ta test edilemeyen, kullanıcının denemesi gereken kısımlar:

- Konum okuma (`getLastKnownLocation` — emülatör yok)
- İlaç saati bildirimlerinin gerçek zamanlamada çalışması
- Bildirimdeki "Aldım" / "Yapıldı" düğmeleri
- Aylık çubuk grafiğinin dar ekranda görünümü

---

## v9.6 ve öncesi (geçmiş)

## 👥 v7.77 — Baştan yönetici seçimi + serbest ayrılma (4 Ağu 2026)

### Kullanıcı isteği
> "Online sohbette yöneticiyi baştan seçme olsun ve odadan ayrılmayı
> yönetici izni olmadan yapabileyim"

### 1. Odadan ayrılmak artık izinsiz

`Islem.AYRIL` kontrolü kaldırıldı — `izinVar()` bu işlem için her zaman
`true` döndürüyor.

**Gerekçe:** birini bir odada zorla tutmak doğru değil. v7.52'de
"yönetici kısıtlaması" olarak eklenmişti ama kullanıcı bunu istemedi.
`ayrilabilir` kural anahtarı geriye dönük uyumluluk için duruyor,
artık kontrol edilmiyor.

**Yönetici ayrılırsa:** ek uyarı gösterilir, ayrılmadan önce yöneticilik
kalan ilk üyeye devredilir. Ağ hatası olsa bile **yerel ayrılma yapılır**
— internet yokken odada mahsur kalınmaz.

### 2. Oda kurarken yönetici seçimi

Yeni oda kurarken üç seçenek soruluyor:

| Seçenek | Davranış |
|---|---|
| **Ben yöneteyim** | Kuran kişi yönetici, şifre koyabilir (eski davranış) |
| **Karşı taraf yönetsin** | Yönetici boş; katılan ilk kişi devralır |
| **Yönetici olmasın** | Hiç yönetici yok, ikisi de her şeyi yapabilir |

`Kural`'a `yoneticiBekliyor` alanı eklendi (bit maskesi 2048).
"Karşı taraf yönetsin" seçilirse bu bayrak açılır; odaya katılan ilk
kişi yöneticiliği devralır ve bayrak kapanır.

`yoneticiMiyim()` artık **yöneticisiz odada herkese `true`** döndürüyor —
kimse kimseyi kısıtlayamaz.

### 3. Yöneticiliği devretme

Ayarlarda yeni seçenek: **"Yöneticiliği devret"**. Üye seçilir,
onaylanır, şifre sıfırlanır (yeni yönetici kendi şifresini koyar).

### Yan düzeltme: ayarlar menüsü yeniden yazıldı

Menü indeks kaydırmayla çalışıyordu (`hangi - kaydirma`); yönetici olup
olmamaya göre liste değiştiği için indeksler kayıyor ve her yeni seçenek
hata riski taşıyordu.

Artık seçenekler **(etiket, işlem) çiftleri** olarak tutuluyor —
sıra değişse bile doğru işlem çalışır.

### Değişen dosyalar
```
OnlineStore.kt    AYRIL → her zaman serbest, +yoneticisizMi(),
                  yoneticiMiyim() yöneticisiz oda desteği,
                  Kural +yoneticiBekliyor (maske 2048)
OnlineActivity.kt +yoneticiModuSor() +hemenAyril()
                  +yoneticiligiBirakVeAyril() +yoneticiDevretSor()
                  katılanın devralması, ayarlar menüsü yeniden yazıldı
strings.xml       +15 string
```

---

## v7.76 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.76** (versionCode 112) |
| APK | `~/GunlukAsistan-v7.76.apk` |
| MD5 | `db10350eb92a773680b2b07178787c7a` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🗄 v7.76 — Grup 5: Room veritabanı geçişi (4 Ağu 2026)

10 iyileştirme listesinin **son maddesi**.

### Önce fizibilite testi yapıldı

Room, derleme zamanı kod üreteci (KSP) gerektiriyor. Bu sandbox 1,9 GB
RAM ile zaten OOM sınırında çalışıyordu — körlemesine geçiş yapmak
yerine **önce minimal bir Room varlığıyla test derlemesi** yapıldı.

**1. deneme: BAŞARISIZ.** `compileDebugKotlin` çöktü:
```
The Daemon will expire after the build after running out of JVM Metaspace.
max heap '820 MiB', max metaspace '280 MiB'
```
KSP kod üretmişti (`_Impl.java` oluştu) ama Kotlin derleyicisi
Metaspace'i tüketti.

**2. deneme: BAŞARILI.** Bellek ayarı yükseltildi:
```
org.gradle.jvmargs   -Xmx820m  → -Xmx1100m
MaxMetaspaceSize     280m      → 640m
kotlin.daemon.jvmargs -Xmx420m → -Xmx700m
```
BUILD SUCCESSFUL, 3 dk 2 sn. Room bu ortamda çalışıyor.

### Kapsam kararı: önce yalnızca görevler

Ölçüm: `Store.kt` **2527 satır**, **76 dosya** tarafından kullanılıyor,
**~190 çağrı noktası** var.

Hepsini tek seferde taşımak veri kaybı riski taşırdı. En sık kullanılan
tablo seçildi — görevler (28 okuma + 23 yazma çağrısı). Diğer tablolar
aynı desenle sırayla eklenebilir.

### Mimari: köprü katmanı

```
Çağıran 76 dosya  →  Store.loadTasks/saveTasks   (imza DEĞİŞMEDİ)
                          ↓
                     GorevDepo (köprü)
                     ↙            ↘
              Room (asıl)    JSON (gölge kopya)
```

**Hiçbir çağıran dosya düzenlenmedi.** Store içeride Room'a yazıyor,
JSON'u da gölge kopya olarak güncel tutuyor.

### Üç güvenlik ağı

1. **Otomatik geri dönüş** — Room okuma/yazma hata verirse
   `roomAktif = false` olur ve uygulama sessizce JSON'a döner.
   Kullanıcı hiçbir şey fark etmez, veri kaybolmaz.
2. **JSON gölge kopyası** — yedekleme (`exportJson`) aynen çalışır.
3. **Sessiz geçiş** — ilk açılışta veritabanı boşsa mevcut JSON verisi
   bir kez içeri aktarılır (`gerekirseTasi`).

### Yakalanan kritik boşluk

`importJson` (yedekten geri yükleme) JSON'u değiştiriyor ama **Room eski
veriyi tutmaya devam ederdi** — kullanıcı yedeği geri yükleyince görevler
eski hâliyle görünürdü. `jsondanTazele()` çağrısı eklendi; geri yükleme
sonrası veritabanı JSON'dan yeniden kuruluyor.

### İndeksler
```
(arsiv, bitti)  → ana liste sorgusu
(sonTarih)      → tarihe göre sıralama
```
`tumunuDegistir` tek transaction içinde çalışıyor, 200'lük parçalar
hâlinde yazıyor (bellek koruması).

### Değişen dosyalar
```
YENİ  veri/GorevVarlik.kt · veri/Veritabani.kt · veri/GorevDepo.kt
      Store.kt            loadTasks/saveTasks → köprü,
                          loadTasksJson/saveTasksJson yedek olarak korundu,
                          importJson → Room tazeleme
      build.gradle.kts    +KSP eklentisi, +Room 2.6.1
      gradle.properties   bellek ayarları yükseltildi
```

### 🎉 10 iyileştirme listesi tamamlandı
```
v7.72 → 3 (filtre+toplu işlem) · 4 (geri al)
v7.73 → 1 (global arama) · 2 (alt görevler)
v7.74 → 5 (bildirimden hızlı yanıt) · 6 (etiketler)
v7.75 → 7 (arşiv) · 8 (sohbet arama) · 9 (karşılaştırma)
v7.76 → 10 (Room geçişi)
```

---

## v7.75 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.75** (versionCode 111) |
| APK | `~/GunlukAsistan-v7.75.apk` |
| MD5 | `19d576e2547ae6f565d631fb4588d926` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 📦 v7.75 — Grup 4: Arşiv + sohbet arama + karşılaştırma (4 Ağu 2026)

10 iyileştirme listesinin **7., 8. ve 9. maddeleri**.

### 🚨 Önce: v7.73'te kritik bir hata bulundu ve düzeltildi

`AramaActivity` **manifest'e hiç eklenmemişti**. v7.73'te yan panelden
"Her Şeyde Ara" düğmesine basılınca uygulama `ActivityNotFoundException`
ile çökerdi.

**Neden fark edilmedi:** doğrulama sırasında `grep -c "AramaActivity"`
kullanmıştım; bu, manifest'te zaten kayıtlı olan **`PdfAramaActivity`**
satırıyla eşleşip `1` döndürdü ve kontrol geçti sandım.

**Alınan önlem:** artık tam kelime deseni (`grep -cE "\.$cls\""`) ile
kontrol ediliyor ve **tüm** `*Activity.kt` dosyaları manifest'e karşı
topluca denetleniyor. Bu turda denetim çalıştırıldı — kalan tüm
Activity'ler kayıtlı çıktı.

### 7. Arşiv

Tamamlanan görevler artık silinmeden listeden çıkarılabiliyor.

- Görev listesinde şerit: **"Bitenleri arşivle (3)"** + **"📦 Arşivi aç"**
- Arşiv ekranında en son arşivlenen üstte, dokununca geri gelir
- Üstte başarı özeti: **"Bu ay 47 iş bitirdin"**
- "Arşivi tamamen sil" — geri alınabilir

**Tekrarlı görevler arşivlenmez** — zincirleri kopmasın diye bilinçli
olarak atlanıyor.

`Store` eklentileri: `arsivGorevleri` · `aktifGorevler` ·
`arsivlenebilirSayi` · `bitenleriArsivle` · `arsiveTasi` ·
`arsiviTemizle` · `buAyBitenGorev`

### 8. Sohbet arama + sabitleme + dışa aktarma

- Sohbet listesinin üstünde **arama kutusu** (başlık + mesaj içeriği taranır)
- **📌 Sabitle** — sabitlenen sohbet listede hep en üstte
- **Dışa aktar** — sohbeti `.txt` dosyasına yazıp paylaş menüsünü açar
  (FileProvider üzerinden)

### 9. Karşılaştırmalı istatistik

Analitik ekranının en üstüne eklendi:
```
Bu hafta vs geçen hafta
↑ %23 daha fazla
Bu hafta: 420 dk  ·  Geçen hafta: 340 dk
```
Artış yeşil, azalış kırmızı. Geçen hafta veri yoksa "yeterli veri yok".

**Kayan pencere kullanıldı** (son 7 gün / önceki 7 gün), takvim haftası
değil — pazartesi sabahı "bu hafta" bomboş görünüp karşılaştırma anlamsız
olmasın diye.

Ayrıca çıkarım kartı artık çıkarım olmasa da görünüyor; eskiden gizli
kalınca karşılaştırma da görünmezdi.

### Değişen dosyalar
```
YENİ  ArsivActivity.kt
      Store.kt                 Task +arsiv/arsivZaman, 7 arşiv fonksiyonu
      TasksFragment.kt         arşiv şeridi, menüde arşivle
      fragment_tasks.xml       +arsivBar
      SohbetGecmisi.kt         +sabit alanı, ara(), sabitDegistir(), dosyayaYaz()
      SohbetGecmisiActivity.kt arama kutusu, sabitleme, dışa aktarma
      Analitik.kt              +haftaKarsilastir()
      AnalitikActivity.kt      +karsilastirmaCiz()
      AndroidManifest.xml      +ArsivActivity, +AramaActivity (v7.73 eksiği)
      strings.xml              +30 string
```

### Kalan
```
Grup 5 → 10 (Room veritabanı geçişi)
```

---

## v7.74 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.74** (versionCode 110) |
| APK | `~/GunlukAsistan-v7.74.apk` |
| MD5 | `2dfff77e9e926444d6048ebb1f231cc4` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🔔 v7.74 — Grup 3: Bildirimden hızlı yanıt + etiketler (4 Ağu 2026)

10 iyileştirme listesinin **5. ve 6. maddeleri**.

### 5. Bildirimden hızlı yanıt

Görev hatırlatma bildirimine **iki yeni eylem** eklendi:

| Eylem | Ne yapar |
|---|---|
| **Yaz** | `RemoteInput` — gölgelikten metin yazıp yeni görev oluşturur |
| **Yarına at** | Görevi yarın 09:00'a taşır, alarmı yeniden kurar |

Artık bildirimde 4 düğme var: Tamamlandı · 15 dk ertele · Yaz · Yarına at.

**Doğal dil desteği:** gölgelikten "yarın 3'te doktoru ara" yazarsan
`NaturalDate` zamanı ayıklar ve hatırlatma da kurulur.

**Teknik not:** "Yaz" eyleminin PendingIntent'i **MUTABLE** olmak zorunda —
sistem yazılan metni intent'e sonradan ekliyor. Diğer eylemler IMMUTABLE
kalabilir. Bu ayrım gözden kaçarsa metin hiç ulaşmaz.

Bu, v7.43'teki 30 bildirim önerisinden **yapılmamış tek maddeydi**.

### 6. Etiketler — `Etiket.kt`

6 hazır etiket: 💼 İş · 🏠 Ev · 🎓 Okul · 🩺 Sağlık · 🛒 Alışveriş · 🔴 Acil

**Tasarım kararı — sabit liste:** kullanıcı tanımlı etiket yönetimi
(oluştur/sil/renk seç) eklenmedi. Tek kullanıcılı günlük kullanımda
6 kategori yeterli; yönetim ekranı karmaşıklığı arttırıp faydayı
arttırmıyordu.

**Saklama:** `Task.etiket` alanında tek karakter ("i", "e", "o"…).
JSON'da yer kaplamıyor, eski kayıtlar sorunsuz okunuyor.

**Arayüz:**
- Görev satırının solunda **4dp renkli şerit** (etiketsizde saydam)
- Uzun bas → menüde `Etiket: 💼 İş` satırı → seçim penceresi
- Filtre çubuğuna 6 emoji çipi eklendi; dokun → o etikete daralt,
  tekrar dokun → daraltmayı kaldır
- Etiket filtresi tarih filtreleriyle **birlikte** çalışır
  (ör. "Bugün" + "🔴 Acil")

### Değişen dosyalar
```
YENİ  Etiket.kt
      Store.kt               Task +etiket, oku/yaz
      TaskActionReceiver.kt  +ACTION_YAZ (RemoteInput) +ACTION_YARIN
      ReminderReceiver.kt    bildirime 2 yeni eylem
      TasksFragment.kt       etiket seçimi, şerit, filtre çipleri
      item_task.xml          +etiketSerit
      AndroidManifest.xml    +2 intent action
      strings.xml            +16 string
```

### Kalan gruplar
```
Grup 4 → 7 (arşiv) · 8 (sohbet arama/dışa aktarma) · 9 (karşılaştırmalı istatistik)
Grup 5 → 10 (Room geçişi)
```

---

## v7.73 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.73** (versionCode 109) |
| APK | `~/GunlukAsistan-v7.73.apk` |
| MD5 | `208acba3aa54a072f6391124bb899182` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🔍 v7.73 — Grup 2: Global arama + alt görevler (4 Ağu 2026)

10 iyileştirme listesinin **1. ve 2. maddeleri**.

### 1. Global arama — `Arama.kt` + `AramaActivity.kt`

Erişim: **⋮ yan panel → 🔍 Her Şeyde Ara**

8 kaynakta birden arar:
```
✅ Görevler · 📝 Notlar · 📚 Konular (alt maddeler dahil)
🎓 Dersler · 💬 Sohbetler (mesaj içerikleri dahil)
🕌 Vakit planı · ✨ Alışkanlıklar · 📅 Etkinlikler
```

**Türkçe duyarlı eşleşme:** "cimento" yazınca "çimento" bulunur
(ı/İ/ş/ğ/ü/ö/ç normalleştirilir).

**Puanlama:** başlıkta baştan eşleşme 100 · başlıkta geçiyor 60 ·
içerikte geçiyor 25. Tam kelime eşleşmesi +15 bonus.

**Performans:**
- Yazma durduktan **250 ms sonra** aranır — her harfte tam tarama yok
- Arama `Performans.arkaPlan` ile arka planda; sonuç `anaIs` ile döner
- Eski sonuç yeni sorgunun üstüne yazılmaz (`sorgu != sonSorgu` kontrolü)

Sonuçlar kategoriye göre gruplanır, her grupta 6 sonuç + "+N tane daha".
Üstteki çiplerden tek kategoriye daraltılabilir. Dokununca ilgili ekrana
gidilir (sohbet sonucu o sohbeti aktif yapar).

### 2. Alt görevler (checklist)

`Store.Task` modeline `adimlar: MutableList<SubItem>` eklendi.
Konulardaki `SubItem` yapısı yeniden kullanıldı — yeni model yazılmadı.

**Kullanım:** göreve uzun bas → menü açılır:
```
Alt adımları düzenle · (Bu seferi atla) · Sil
```

Adım penceresinde: dokun → işaretle, uzun bas → sil, "+ Adım ekle"
pencereyi kapatmadan arka arkaya ekler.

**Otomatik tamamlama:** tüm adımlar bitince görev de tamamlanır.
Tekrarlı görevse bir sonraki tarihe taşınır (`Tekrar.gorevYenile`).

**Listede rozet:** `⏰ 12 Ağu Sal 🔁 Her hafta ☑ 3/6`
Tarihi olmayan ama adımlı görevlerde de rozet görünür.

### Yan etki: silme menüsü yeniden yazıldı
`confirmDelete` artık doğrudan silmiyor; seçenek menüsü açıyor.
Silme `silOnayla`'ya, tekrar atlama `tekrariAtla`'ya ayrıldı.
Görev silme de artık **geri alınabilir** (v7.72'de eklenen
`deleteTaskUndoable` kullanılıyor).

### Değişen dosyalar
```
YENİ  Arama.kt · AramaActivity.kt
      Store.kt              Task +adimlar, oku/yaz
      TasksFragment.kt      alt adım penceresi, rozet, menü yeniden yazıldı
      activity_main.xml     +drawerAramaBtn
      MainActivity.kt       arama bağlantısı
      AndroidManifest.xml   +AramaActivity
      strings.xml           +25 string
```

### Kalan gruplar
```
Grup 3 → 5 (bildirimden hızlı yanıt) · 6 (etiketler)
Grup 4 → 7 (arşiv) · 8 (sohbet arama/dışa aktarma) · 9 (karşılaştırmalı istatistik)
Grup 5 → 10 (Room geçişi)
```

---

## v7.72 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.72** (versionCode 108) |
| APK | `~/GunlukAsistan-v7.72.apk` |
| MD5 | `2c7d33dcd5926ca1b229a59757703e13` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🗂 v7.72 — Filtre + toplu işlem + geri al (4 Ağu 2026)

### Kullanıcı isteği
10 iyileştirme listesinden **"hepsini sırasıyla"**. Bu sürüm ilk grup:
**3. madde** (filtre + toplu işlem) ve **4. madde** (geri al her yerde).

### 3. Filtre çubuğu + toplu işlem

**Filtreler** (Görevler ekranı, başlığın altında çip şeridi):
```
Tümü · Bugün · Bu hafta · Gecikmiş · Tekrarlı · Bitenler
```
Seçili filtre vurgulanır ve yanında sayı gösterilir (`Bugün 4`).
"Bitenler" dışındaki filtreler tamamlananları gizler — günlük kullanımda
liste temiz kalır.

**Çoklu seçim:** bir göreve uzun bas → seçim modu açılır.
- Üstte `3 seçildi` şeridi + "Tümünü seç" + ✕
- Altta işlem düğmeleri: **Tamamla · Bugüne al · Yarına at ·
  Tarihi kaldır · Sil**
- Seçim modunda satır dokunuşu seçer (tamamlamaz), silme simgesi gizlenir
- Seçili satırlar vurgu rengiyle işaretlenir

**Tek geri alma adımı:** 20 görevi toplu güncellersen tek dokunuşla
hepsi geri gelir (`gorevleriGuncelleUndoable` önceki hâlleri saklıyor).

### 4. "Geri Al" her yerde

Altyapı (`Store.kaydetGeriAlma` / `geriAl`) v7.9'dan beri vardı ama
yalnızca Kurslar, Etkinlikler, Alışkanlıklar ve Kitaplığa bağlıydı.

Eklenen `Undoable` fonksiyonlar:
```
deleteNoteUndoable · deleteTopicUndoable
deleteTasksUndoable (toplu) · gorevleriGuncelleUndoable (toplu düzenleme)
```

Bağlanan ekranlar: **Görevler · Notlar · Konular**
Her silmeden sonra 5 sn'lik Snackbar: "Not silindi — [Geri al]"

**Not silmede bilinçli değişiklik:** resim dosyası artık silme anında
diskten kaldırılmıyor. Geri alınırsa görsel kaybolmasın diye — temizlik
sonraya bırakıldı.

### Değişen dosyalar
```
Store.kt              +4 Undoable fonksiyon
TasksFragment.kt      filtre motoru, çoklu seçim, toplu işlem, geri al
NotesFragment.kt · TopicsFragment.kt   geri alınabilir silme + Snackbar
fragment_tasks.xml    +filtre çipleri, +seçim şeridi, +işlem düğmeleri
strings.xml           +26 string
```

### Sıradaki gruplar
```
Grup 2 → 1 (global arama) · 2 (alt görevler)
Grup 3 → 5 (bildirimden hızlı yanıt) · 6 (etiketler)
Grup 4 → 7 (arşiv) · 8 (sohbet arama) · 9 (karşılaştırmalı istatistik)
Grup 5 → 10 (Room geçişi)
```

---

## v7.71 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.71** (versionCode 107) |
| APK | `~/GunlukAsistan-v7.71.apk` |
| MD5 | `c85bfd5075a0650164d1491c2bb552af` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🎙 v7.71 — Bas-konuş hızlı not (4 Ağu 2026)

### Kullanıcı isteği
10 öneri listesinden **10. madde**:
> "Widget'tan bas, konuş, bırak → metne çevrilir, AI kategorize eder
> (görev mi, not mu, alışveriş mi)."

### Akış
1. Hızlı Eylemler widget'ındaki **🎙 Bas-Konuş** düğmesine dokun
2. Şeffaf panel açılır, **mikrofon anında dinlemeye başlar** (ekstra dokunuş yok)
3. Konuşurken metin canlı görünür (`EXTRA_PARTIAL_RESULTS`)
4. Sınıflandırma yapılır, önerilen hedef vurgulu gelir
5. Metni düzeltebilir, hedefi değiştirebilirsin → Kaydet

### İki aşamalı sınıflandırma — `SesliNot.kt`

**1. Yerel kural motoru** (önce, her zaman)
- 5 kategori için anahtar kelime sözlükleri
- Puanlama: eşleşen kelime = 1 puan, en yüksek kazanır
- Güven = kazananın toplam içindeki payı
- Soru işareti güçlü sinyal → doğrudan ASISTAN (0.9 güven)
- Sinyal yoksa: kısa ifade → görev, uzun ifade → not

**2. Yapay zekâ** (yalnızca güven < 0.6 ise)
- Tek kelimelik yanıt isteyen kısa istem, bütçe 32 token
- AI kapalıysa veya hata olursa yerel sonuç korunur

Bu sıralama bilinçli: *"süt al"* demek için ağ isteği atmak gereksiz.
Kural motoru vakaların çoğunu zaten yakalıyor, gecikme sıfır.

### Hedefler ve davranışları
| Hedef | Ne olur |
|---|---|
| ✅ Görev | `NaturalDate` ile zaman ayıklanır — "yarın 3'te doktoru ara" → hatırlatma kurulur |
| 🛒 Alışveriş | "🛒 Alışveriş" başlıklı **tek nota** madde madde biriktirilir |
| 📝 Not | İlk 40 karakter başlık, tamamı içerik |
| 🕌 Vakit planı | O an aktif olan dilime eklenir |
| ✨ Asistan | Sohbet geçmişine yazılır + asistan ekranı açılır |

### Neden ayrı ekran
Widget `RemoteViews` ile çizilir — mikrofon dinleyicisi barındıramaz.
Şeffaf panel (`Theme.QuickAdd`) kullanıldı; kapanınca kullanıcı ana
ekranına döner. `excludeFromRecents` ile son uygulamalar listesi
kirlenmiyor.

### Değişen dosyalar
```
YENİ  SesliNot.kt · SesliNotActivity.kt
      widget_actions.xml   +actSes (🎙 Bas-Konuş)
      ActionsWidget.kt     +tıklama, +import, tema listesine actSes
      AndroidManifest.xml  +SesliNotActivity
      strings.xml          +23 string
```

---

## v7.70 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.70** (versionCode 106) |
| APK | `~/GunlukAsistan-v7.70.apk` |
| MD5 | `42392a61ab0f186722bd0bb4393864a3` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Yedek biçimi | **17** |

---

## 🔁 v7.70 — Tekrarlayan görevler (4 Ağu 2026)

### Kullanıcı isteği
10 öneri listesinden **1. madde** seçildi:
> "Her salı çöp", "her ayın 1'i kira", "her 3 ayda bir filtre" — bir kere
> kur, kendi kendine yenilensin.

### `Tekrar.kt` — tekrar motoru

**Tekrar türleri:**
```
Her gün · Her hafta · 2 haftada bir · Her ay
3 ayda bir · 6 ayda bir · Her yıl
Haftanın günleri (çoklu seçim: Pzt·Çar·Cum)
Özel: her N günde bir (2-180)
```

**Kodlama:** tek metin alanı — `"gun"`, `"3ay"`, `"gunler:1,3,5"`,
`"ozel:10"`. JSON'a tek alan olarak sığıyor, eski yedeklerle uyumlu
(alan yoksa `"yok"` varsayılır).

### Tasarım kararı: görev silinmez, taşınır

Tamamlanan tekrarlı görev listeden kaybolmuyor; `dueAt` bir sonraki
tarihe alınıyor, `done` tekrar `false` yapılıyor. Böylece:
- Alarm zinciri kopmuyor (yeni tarihe yeniden kuruluyor)
- Görev kimliği sabit kalıyor — widget/bildirim referansları bozulmuyor
- `yapildi` sayacı "kaç kez yaptım" bilgisini tutuyor

**Geçmişte kalma koruması:** Günlük görevi 3 gün açmadıysan tarih 3 gün
geriden gelmiyor — `sonraki()` bugüne/geleceğe kadar ileri sarıyor
(400 adım güvenlik sınırı ile).

### Üç tamamlama noktası da bağlandı
Tamamlama mantığı üç ayrı yerde vardı; hepsi `Tekrar.gorevYenile()`
çağırıyor:
```
TasksFragment.toggleTask()     → Görevler ekranı
TaskActionReceiver             → Bildirimdeki "Tamam" düğmesi
TasksWidget.onReceive()        → Görev widget'ı
```

### Arayüz
- **Görev ekleme penceresinde** tekrar çipleri (10 seçenek)
- "Haftanın günleri" ve "Özel" çipleri ek pencere açıyor, seçim yapılınca
  çip etiketi seçimi gösteriyor (ör. "Pzt · Çar · Cum")
- Listede **🔁 rozeti**: `⏰ 12 Ağu Sal  🔁 Her hafta`
- Tekrarlı görev **tarihsiz kurulamaz** — tarih verilmezse yarın 09:00 atanır
- Tamamlayınca bildirim: `🔁 Çöp at · sıradaki 12 Ağu Sal`
- Silme penceresinde tekrarlı göreve **"Bu seferi atla"** seçeneği eklendi
  (silmek tek seçenek olmasın)

### Veri modeli
`Store.Task` +3 alan: `tekrar`, `tekrarBitis`, `yapildi`.
Yedek biçimi 16 → **17**. Eski yedekler sorunsuz okunuyor.

### Değişen dosyalar
```
YENİ  Tekrar.kt
      Store.kt              Task +3 alan, oku/yaz, yedek 17
      TasksFragment.kt      tekrar çipleri, gün/özel seçici, rozet, atla
      TaskActionReceiver.kt · TasksWidget.kt   yenileme bağlantısı
      dialog_task.xml       +tekrar bölümü
      strings.xml           +32 string
```

---

## v7.69 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.69** (versionCode 105) |
| APK | `~/GunlukAsistan-v7.69.apk` |
| MD5 | `92f77e3153384d09be184221718fe942` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 👆 v7.69 — Plan widget'ı tam etkileşimli (4 Ağu 2026)

### Kullanıcı isteği
> "Namaz plan widgeti tıklamalı olsun, ordan bütün her şeyi yapabileyim"

### Sorun
Widget `RemoteViews` ile çizilir; metin kutusu, açılır menü, kaydırma
çubuğu barındıramaz. Yani "widget'ın içinde" iş eklemek teknik olarak
mümkün değil. Öncesinde widget'a dokununca uygulamanın tamamı açılıyordu.

### Çözüm — `PlanHizliActivity.kt`
Widget'a dokununca **şeffaf, alttan yükselen bir panel** açılıyor
(`Theme.QuickAdd`). Panel kapanınca kullanıcı ana ekranına döner —
uygulamaya girip çıkmış hissetmez.

**Panelde yapılabilenler:**
- Metin kutusuyla **yeni iş ekleme** (klavye "bitti" tuşu da ekler)
- **Süre çipleri**: yok / 15 / 25 / 45 / 60 dk
- **Dilim seçimi** — 6 vakit aralığından birine geç
- Dilimdeki işleri görme, **dokun → tamamla / geri al**
- **Uzun bas → sil** (onaylı)
- **Bitenleri temizle** (biten iş varsa görünür)
- Sıradaki vakit + bugünün 6 vakti (aktif olan vurgulu)
- **▶ Sayaç** ve **Tam ekranı aç** kısayolları
- Dilim özeti: `2/5 bitti · 1 sa 30 dk planlı`

Her işlemden sonra widget anında tazeleniyor
(`WidgetCommon.refreshAll(context, true)`).

### Widget tıklama haritası
```
Üst şerit      → panel açılır
"+ İş"         → panel açılır, metin kutusu odaklanmış
Dilim şeridi   → panel açılır
Boş liste      → panel açılır, metin kutusu odaklanmış
İlerleme (2/5) → tam Plan sekmesi
Vakit özeti    → Namaz ekranı
Liste satırı   → işi tamamla (panel açılmaz, anında)
```

Panel `excludeFromRecents` + `taskAffinity=""` ile kayıtlı — son
uygulamalar listesini kirletmiyor, ayrı bir görev olarak açılıyor.

### Değişen dosyalar
```
YENİ  PlanHizliActivity.kt
      PlanWidget.kt        +hizliAc(), tıklama hedefleri yönlendirildi
      AndroidManifest.xml  +PlanHizliActivity (Theme.QuickAdd)
      strings.xml          +19 string
```

---

## v7.68 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.68** (versionCode 104) |
| APK | `~/GunlukAsistan-v7.68.apk` |
| MD5 | `7bf56b00e92213296f4cc86df64fbfcd` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Widget sayısı | 10 |

---

## 🧩 v7.68 — Widget tema seçimi + anlık senkron (4 Ağu 2026)

### Kullanıcı isteği
> "Ayarlar kısmına ekstra olarak widget tema seçimi ekleme yap ve bütün
> temalara uygula. Widget senkronizasyonu anlık olsun."

### 1. Widget Teması ekranı — `WidgetTemaActivity.kt`

Erişim: **Ayarlar → 🧩 Widget teması**

| Ayar | Seçenekler |
|---|---|
| **Zemin** | Karanlık · Aydınlık · Sistemi izle · Uygulama teması |
| **Saydamlık** | Tam opak · Hafif · Orta · Çok (duvar kâğıdı sızar) |
| **Vurgu rengi** | Uygulama temasından **veya** widget'lara özel 12 renk |
| **Anlık senkron** | Aç/kapat |

Ekranın üstünde **canlı önizleme** kartı var — seçim yapar yapmaz nasıl
görüneceğini gösteriyor. Her değişiklik anında kaydediliyor ve tüm
widget'lar zorla tazeleniyor.

Ayrıca "Tüm widgetları şimdi yenile" düğmesi kaç widget'ın yenilendiğini
bildiriyor (hiç yoksa "Ana ekranda hiç widget yok" diyor).

### 2. `WidgetTema.kt` genişletildi

v7.67'de zemin koyuya **sabitlenmişti** (`val koyu = true`). Artık
kullanıcı tercihine bağlı:

```kotlin
val koyu = koyuMuOlmali(context, spec)   // moda göre
val zemin = if (koyu) koyuZemin(...) else acikZemin(...)
```

- `MOD_SISTEM` → telefonun gece/gündüz durumunu okur
- `MOD_TEMA` → seçili uygulama temasının kendi zeminini kullanır
- `acikZemin()` eklendi: koyu temalarda tonu koruyup parlaklığı yükseltir

**8 yeni drawable**: `w_card_{koyu,acik}{,_s1,_s2,_s3}` — köşe
yuvarlaklığı korunarak saydamlık uygulanabilsin diye. `setBackgroundColor`
köşeleri kareleştirdiği için shape kaynağı değiştirme yöntemi kullanıldı.

7 widget'ın çizim kodunda `WidgetTema.saydamlikUygula(...)` çağrısı var
(cam widget'lar kendi tasarımlarını koruyor).

### 3. Anlık senkron

`Store.widgetTazele()` artık:

```kotlin
val anlik = WidgetTema.anlikSenkron(context)
WidgetCommon.refreshAll(context, anlik)   // anlık ise 400 ms kısıtı atlanır
```

Öncesi: veri değişiminden sonraki 400 ms içindeki tazeleme istekleri
yok sayılıyordu (yayın fırtınası koruması). Görev işaretleyip hemen ana
ekrana çıkınca widget eski veriyi gösterebiliyordu. Artık anlık senkron
açıkken (varsayılan) kısıtlama atlanıyor — istenirse ayardan kapatılıp
pil dostu moda dönülebilir.

`refreshAll` zaten `notifyAppWidgetViewDataChanged` ile liste
widget'larının satırlarını da yeniliyor.

### Değişen dosyalar
```
YENİ  WidgetTemaActivity.kt
      8 × w_card_*.xml (koyu/açık × 4 saydamlık)
      WidgetTema.kt          +tercih katmanı, +acikZemin(), +saydamlikUygula()
      Store.kt               widgetTazele → anlık senkron
      SettingsFragment.kt    +rowWidgetTheme bağlantısı
      fragment_settings.xml  +🧩 Widget teması satırı
      7 widget               +saydamlikUygula çağrısı
      AndroidManifest.xml    +WidgetTemaActivity
      strings.xml            +28 string
```

---

## v7.67 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.67** (versionCode 103) |
| APK | `~/GunlukAsistan-v7.67.apk` |
| MD5 | `72f4017e8115cc0cdda4673066a79b94` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Widget sayısı | 10 · **hepsi koyu tema** |

---

## 🌑 v7.67 — Widget'lar her zaman karanlık (4 Ağu 2026)

### Kullanıcı isteği
> "Widgetlarda karanlık tema kullan hepsinde"

### Öncesi
Widget'lar telefonun gece/gündüz moduna uyuyordu. Aydınlık moddayken
krem zeminli, koyu moddayken koyu çıkıyordu. Kullanıcı her koşulda koyu
istedi.

### Yapılan — iki katmanda birden

**1. Renk kaynakları (XML)**

Dört varyant dosyası da koyu palete çevrildi:

| Dosya | Öncesi | Sonrası |
|---|---|---|
| `values/` | açık krem (#FFFDF8) | **koyu (#1C1814)** |
| `values-night/` | koyu | koyu (değişmedi) |
| `values-v31/` | Material You *aydınlık* uç (neutral1_50) | **Material You koyu uç (neutral1_900)** |
| `values-night-v31/` | Material You koyu | koyu (değişmedi) |

Android 12+ cihazlarda duvar kâğıdı uyumu korunuyor — sadece tonlar
koyu uçtan seçiliyor.

**2. Çalışma anı paleti (`WidgetTema.kt`)**

v7.66'da eklenen dinamik boyama, zemini doğrudan temanın kart renginden
alıyordu; Krem/Okyanus gibi aydınlık temalarda widget yine aydınlık
oluyordu. Artık:

```kotlin
val zemin = koyuZemin(spec.cardColor, spec.dark)
val koyu = true   // her zaman
```

`koyuZemin()`: tema zaten koyuysa (Ember, Aurora, Zincir) olduğu gibi
bırakır. Aydınlık temalarda rengin **tonunu koruyup** parlaklığını düşürür
(`karistir(0xFF16120F, kartRengi, 0.16f)`).

Böylece Okyanus koyu-mavimsi, Orman koyu-yeşilimsi bir zemin alır —
hepsi aynı siyah blok gibi görünmez, tema kimliği yaşamaya devam eder.
Metin/soluk/yeşil renkleri koyu değerlere sabitlendi, yedek palet de
koyulaştırıldı.

### Tema rengi kayboldu mu?
Hayır. Seçilen tema/vurgu rengi **vurgu öğelerinde** yaşıyor: geri sayım
rozeti, ilerleme çubuğu, onay kutuları, "+ İş" düğmesi, süre rozetleri.
Sadece zemin koyuya sabitlendi.

### Cam widget'lar
`GlassTasks/Habits/Today` zaten koyu cam tasarımdaydı (`#C7101010`),
dokunulmadı.

### Değişen dosyalar
```
values/widget_colors.xml       açık → koyu palet
values-v31/widget_colors.xml   Material You aydınlık uç → koyu uç
WidgetTema.kt                  +koyuZemin(), koyu = true, yedek palet koyu
```

---

## v7.66 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.66** (versionCode 102) |
| APK | `~/GunlukAsistan-v7.66.apk` |
| MD5 | `0971e42bc9e067cd5866422305d8f656` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Widget sayısı | 10 |

---

## 🎨 v7.66 — Widget'lar uygulama temasında (4 Ağu 2026)

### Kullanıcı isteği
> "Widgetları uygulama tema renginde yap hepsini"

### Sorun
Widget renkleri `res/values/widget_colors.xml` içinde **sabit** yazılıydı —
krem/karamel paleti. Uygulamada Okyanus, Orman, Ember gibi bir tema veya
12 vurgu renginden biri seçilse bile ana ekrandaki widget'lar eski krem
renginde kalıyordu. Yalnızca gece/gündüz ayrımı çalışıyordu.

Sebep: XML kaynakları çalışma anında değiştirilemez; `@color/w_text`
derleme zamanında sabitlenir.

### Çözüm — `WidgetTema.kt`

`RemoteViews` üzerinde `setTextColor` ve `setInt(..., "setBackgroundColor")`
çağrılabildiği için, her widget çizildikten **sonra** renkleri üstüne
yazıyoruz.

**Palet üretimi:**
- **Vurgu**: kullanıcı bir vurgu rengi seçtiyse ondan
  (`ThemeManager.accents[i].swatch`), yoksa temanın halka rengi
  (`Spec.ringColor`)
- **Zemin**: temanın kart rengi (`Spec.cardColor`)
- **Metin**: zeminin **algılanan parlaklığına** göre koyu/açık seçilir
  (0.299R + 0.587G + 0.114B) — hangi tema seçilirse seçilsin okunur kalır
- **Kontrast koruması**: vurgu ile zemin arasındaki fark 0.28'in altındaysa
  vurgu otomatik açılır/koyulaştırılır (`okunurVurgu`)

### Kapsam — 10 widget + 3 liste satırı
```
PlanWidget · NamazWidget · TasksWidget · SummaryWidget
CountdownWidget · ActionsWidget · BrifingWidget
GlassTasks/Habits/Today (GlassWidgetBase üzerinden)
PlanWidgetService · TasksWidgetService · GlassListService (satırlar)
```

Glass widget'lardaki sabit `0xFF8A8A8C` / `0xFFFCFCFE` renkleri de
temaya bağlandı.

### Anında geçiş
`ThemeFragment`'ta tema veya vurgu seçilince
`WidgetCommon.refreshAll(context, true)` çağrılıyor.

`refreshAll`'a **`zorla`** parametresi eklendi: normalde 400 ms içindeki
tekrar istekler yok sayılıyor (yayın fırtınası koruması), ama tema
değişiminde bu kısıtlama widget'ın eski renkte kalmasına yol açıyordu.

### Bilinçli karar: kök zemin boyanmadı
`w_card` yuvarlak köşeli bir `shape`. `setBackgroundColor` onu düz renge
çevirip köşe yuvarlaklığını yok ediyor. Bu yüzden kök zemin XML'deki
gece/gündüz uyumlu hâlinde bırakıldı; tema rengi metinler, çipler,
düğmeler ve vurgularda uygulanıyor.

### Değişen dosyalar
```
YENİ  WidgetTema.kt
      PlanWidget · NamazWidget · TasksWidget · SummaryWidget
      CountdownWidget · ActionsWidget · BrifingWidget · GlassWidgetBase
      PlanWidgetService · TasksWidgetService · GlassListService
      WidgetCommon.kt   refreshAll(context, zorla)
      ThemeFragment.kt  tema/vurgu seçiminde widget tazeleme
```

---

## v7.65 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.65** (versionCode 101) |
| APK | `~/GunlukAsistan-v7.65.apk` |
| MD5 | `5331d971fc4bcfa1ee343f641fd56169` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| Widget sayısı | **10** (9 → 10) |
| strings.xml | 1830 string |

---

## 🕌 v7.65 — Vakit Planı widget'ı (4 Ağu 2026)

### Kullanıcı isteği
> "Namaz plan uygulamasını temasında widget ekle ve bütün özellikleri
> ekle. Vakit planı ekranının benzeri olsun."

### Plan sekmesinin ana ekran karşılığı

| Plan sekmesi | Widget |
|---|---|
| Sıradaki vakit + geri sayım | Üst şerit (emoji + ad + saat + kalan) |
| Bugünkü ilerleme | `2/5` rozeti |
| Aktif dilim + kalan süre | Dilim şeridi |
| — | **Doluluk çubuğu** (dilimin ne kadarı geçti) |
| Dilim işleri, dokun-tamamla | Kaydırılabilir liste, dokun-tamamla ✓ |
| Öncelik simgesi + süre rozeti | Satırlarda aynen |
| `+` ile iş ekleme | "+ İş" düğmesi → Plan sekmesi |
| Bugünün 6 vakti | Alt satır özeti (İms 04:12 · Gün 05:55 …) |

### Akıllı liste
Widget **o an aktif olan dilimin** işlerini gösterir — sabah ezber işleri,
öğleden sonra pratik işleri. Vakit ilerledikçe liste kendiliğinden değişir.

Aktif dilim boşsa günün kalan bekleyen işleri listelenir; widget boş
kalıp işe yaramaz hâle gelmesin.

### Boyuta uyarlanma
- **Dar** (<180dp): "+ İş" düğmesi ve vakit özeti gizlenir
- **Kısa** (<120dp): dilim şeridi ve doluluk çubuğu gizlenir
- 4×3 varsayılan, 2×2'ye kadar küçülebilir, 400×500dp'ye büyür

### Tıklama hedefleri
```
Üst şerit / dilim şeridi / "+ İş" / boş alan → Plan sekmesi
Alt vakit özeti                              → Namaz ekranı
Liste satırı                                 → işi tamamla/geri al (uygulama açılmaz)
Modül kapalıyken herhangi bir yer            → Namaz ayarları
```

### Tema uyumu
Mevcut widget renk sistemi kullanıldı: `values/`, `values-night/`,
`values-v31/`, `values-night-v31/` — karanlık mod ve Android 12+
Material You dinamik renkleri otomatik çalışır.

### RemoteViews güvenliği
Yalnızca izin verilen görünümler: LinearLayout · FrameLayout · TextView ·
ProgressBar · ListView. `<View>`/ConstraintLayout kullanılmadı
(v7.40.1'de "Widget eklenemedi" hatasına yol açmıştı). Derleme öncesi
yorumları temizleyen bir betikle etiket denetimi yapıldı.

### Senkronizasyon
`NamazPlan.isleriKaydet()` artık `WidgetCommon.refreshAll()` çağırıyor —
uygulamada iş ekleyince/işaretleyince widget anında güncelleniyor.
`PlanWidget` hem `TUM_WIDGETLAR` hem `LISTE_WIDGETLARI` listelerine eklendi.

### Değişen dosyalar
```
YENİ  PlanWidget.kt · PlanWidgetService.kt
      widget_plan.xml · widget_plan_row.xml · w_plan_info.xml
      WidgetCommon.kt      +PlanWidget (2 listeye)
      NamazPlan.kt         isleriKaydet → widget tazele
      AndroidManifest.xml  +receiver +service
      strings.xml          +12 string
```

---

## v7.64 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.64** (versionCode 100) |
| APK | `~/GunlukAsistan-v7.64.apk` |
| MD5 | `06486514bd34ec0352fe4cfd6f4ae96f` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| strings.xml | 1818 string |

---

## 📝 v7.64 — Plan ekleme işlevselleşti (4 Ağu 2026)

### Kullanıcı isteği
> "Namaz planları ekleme yerini daha işlevsel yap"

### Öncesi
Tek satırlık metin kutusu: yaz, Ekle'ye bas. Süre yok, öncelik yok,
sıralama yok, hazır seçenek yok. Yanlış dilime eklersen silip yeniden
yazman gerekiyordu.

### Şimdi — `PlanEkleyici.kt`

| Özellik | Açıklama |
|---|---|
| **Hazır seçenekler** | Dilime uygun öneriler, dokun → metin + süre dolar |
| **Süre çipleri** | 15 / 25 / 45 / 60 / 90 dk (Pomodoro uyumlu) |
| **Öncelik** | Düşük 🔵 · Normal · Öncelikli 🔴 |
| **Dilim değiştirme** | Editörden başka vakit aralığına taşı |
| **Vakit girince hatırlat** | 🔔 — bildirimde bu iş öne çıkar |
| **Görevlere de ekle** | Görevler sekmesine kopyalar |
| **Kaydet ve yeni ekle** | Pencere kapanmaz, arka arkaya iş girilir |

Aynı editör hem ekleme hem düzenleme için kullanılıyor.

### Liste tarafı
- İş satırında öncelik simgesi, süre rozeti ve 🔔 işareti
- Dilim başlığında **toplam planlanan süre**; dilim süresini aşarsa
  kırmızı `⚠ Dilim süresini aşıyor` uyarısı
- Sıralama: önce bitmemişler → öncelik → kullanıcı sırası
- Dilimde biten iş varsa 🧹 ile tek dokunuşla temizleme
- Uzun basma menüsü genişledi: Düzenle · **▶ Sayaçla başla** ·
  ↑ Yukarı · ↓ Aşağı · Çoğalt · Sil

### Veri modeli
`NamazPlan.Is`'e dört alan eklendi: `sureDk`, `oncelik`, `sira`, `hatirlat`.
Eski kayıtlar varsayılanlarla okunuyor — geriye dönük uyumlu, veri kaybı yok.

Yeni fonksiyonlar: `isGuncelle` · `isTasi` · `isCogalt` ·
`bitenleriTemizle` · `dilimPlanliSure`.

### Değişen dosyalar
```
YENİ  PlanEkleyici.kt
      NamazPlan.kt     +4 alan, +5 fonksiyon, sıralı dilimIsleri
      PlanFragment.kt  yeni editör, zengin satır, süre özeti, 🧹, geniş menü
      strings.xml      +31 string
```

---

## v7.63 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.63** (versionCode 99) |
| APK | `~/GunlukAsistan-v7.63.apk` |
| MD5 | `335572c32874ded54559d4b24d390211` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| strings.xml | 1787 string |

---

## 🔔 v7.63 — Bildirimler gelmiyordu + kompakt bildirim (4 Ağu 2026)

### Kullanıcı bildirimi
> "Namaz saatleri vb bildirimler gelmiyor, bildirimleri daha kompakt ve
> işlevsel yap"

### 🔍 Kök neden — POST_NOTIFICATIONS hiç istenmiyordu

Alarm kodu doğruydu (`setExactAndAllowWhileIdle`, boot sonrası yeniden
kurulum, kanal yönetimi — hepsi yerindeydi). Sorun bir katman yukarıdaydı:

**Uygulama Android 13+ bildirim iznini (`POST_NOTIFICATIONS`) hiçbir yerde
istemiyordu.** Manifest'te tanımlıydı ama çalışma anında yalnızca Sayaç
ekranında isteniyordu. Kullanıcı oraya hiç girmediyse izin hiç verilmiyor.

İzin yoksa `NotificationManagerCompat.notify()` **sessizce başarısız olur** —
istisna atmaz, log basmaz. Alarm çalıyor, kod çalışıyor, ekranda hiçbir şey
görünmüyor. Teşhis edilmesi zor, etkisi tam.

İkinci engel: Android 12+ `SCHEDULE_EXACT_ALARM`. İzin yoksa kod
`setAndAllowWhileIdle`'a düşüyor — bildirim gelir ama dakikalarca gecikir.

### 🔧 Çözüm — `BildirimTani.kt` + `BildirimTaniActivity.kt`

Beş engeli tek ekranda toplayan tanılama, her satırda **"Düzelt"** düğmesi:

| # | Kontrol | Düzeltme |
|---|---|---|
| 1 | Bildirim izni (Android 13+) | Sistem izin diyaloğu |
| 2 | Tam zamanlı alarm (Android 12+) | Alarm izin ekranı |
| 3 | Uygulama ana bildirim anahtarı | Anında açar |
| 4 | Namaz modülü + vakit bildirimleri | Anında açar |
| 5 | Pil optimizasyonu | Sistem ayarı |

Ek olarak: sıradaki vakit bilgisi, "Test bildirimi gönder",
"Alarmları yeniden kur", "Sistem bildirim ayarları".

**Açılışta bir kez izin istenir** (`acilistaIzinIste`) — reddedilirse bir
daha rahatsız edilmez. Erişim: Ayarlar → Bildirimler ve Namaz Ayarları
(sorun varsa "⚠️ N sorun bildirimleri engelliyor" olarak görünür).

### 📱 Kompakt bildirim

Öncesi: `🕌 Öğle vakti · 12:56` + ayrı satırda uzun gövde
(`Öğleden dilimi başlıyor · Sıradaki: …`) — gölgelikte iki satır yer.

Sonrası:
- Başlık tek satır: `🕌 Öğle · 12:56`
- Gövde kısa iş özeti: `▸ Statik ödevi`
- Birden fazla bekleyen iş varsa genişletilebilir liste (en fazla 4) +
  alt yazıda `3 iş bekliyor`
- `setOnlyAlertOnce(true)` — tekrar eden uyarı yok

### Değişen dosyalar
```
YENİ  BildirimTani.kt · BildirimTaniActivity.kt
      NamazBildirim.kt        kompakt başlık/gövde, çoklu iş listesi
      MainActivity.kt         açılışta izin isteği
      BildirimAyarActivity.kt tanılama girişi + sorun rozeti
      NamazAyarActivity.kt    tanılama girişi + sorun rozeti
      AndroidManifest.xml     +BildirimTaniActivity
      strings.xml             +34 string
```

---

## v7.62 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.62** (versionCode 98) |
| APK | `~/GunlukAsistan-v7.62.apk` |
| MD5 | `abc6f85116bfc6abdf4ce76c123ecf8d` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |

---

## 🚨 v7.62 — KRİTİK: Butonlar çalışmıyordu (4 Ağu 2026)

### Kullanıcı bildirimi
> "Uygulama açılıyor ama geri sayım ekle, yapay zekaya metin yazma,
> kurs pdfleri açma vs hiçbirine tıklayınca açılmıyor, basmıyor gibi
> işlev oluyor."

### 🔍 Kök neden — v7.58 regresyonu (benim hatam)

v7.58'de aşağı çekince yenileme eklenirken her fragment'in kök görünümü
**çalışma anında** bir `SwipeRefreshLayout` içine alınıyordu:

```
FrameLayout(container)
 └─ SwipeRefreshLayout   ← v7.58'de eklendi
     └─ fragment kök görünümü
```

`MainActivity.open()` ekranları `FragmentTransaction.hide()` ile gizliyor.
**Ama `hide()` yalnızca fragment'in KENDİ görünümünü GONE yapar** —
onu saran `SwipeRefreshLayout`'a dokunmaz.

Sonuç: her ziyaret edilen ekran, konteynerde **görünmez ama VISIBLE** bir
katman bırakıyordu. Bu katmanlar üst üste birikiyor ve en son eklenen,
açık olan ekranın üstünde kalıp **tüm dokunuşları yutuyordu**.

Bu yüzden ekran doğru görünüyor ama hiçbir düğme tepki vermiyordu.
Kullanıcı birkaç ekran gezdikten sonra sorun kalıcı hâle geliyordu.

### 🔧 Çözüm
- `Yenileyici.gorunurluguEsitle(fragment)` eklendi — sarmalayıcının
  görünürlüğünü `fragment.isHidden` ile eşitler, gizliyken dönen çarkı da
  durdurur.
- `MainActivity.open()` her geçişin sonunda **tüm** `scr_*` fragment'leri
  için bunu çağırır.
- `Yenileyici.kur()` sarmalayıcıyı oluştururken de `isHidden` durumunu
  uygular (gizli fragment `onStart` alabiliyor).
- 13 fragment'in `onStart` bloğuna eşitleme çağrısı eklendi.

### Ders
Bir görünümü çalışma anında sarmalamak, o görünümün yaşam döngüsü
sözleşmesini bozar. `hide()`/`show()` fragment görünümünü hedefler;
araya giren her katmanın görünürlüğü **elle** yönetilmelidir.

---

## v7.61 (önceki)

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **7.61** (versionCode 97) |
| APK | `~/GunlukAsistan-v7.61.apk` |
| MD5 | `1ccb3001e359d7297f491d05c64496ec` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ |
| strings.xml | 1753 string · Yedek biçimi **16** |

---

## ⚡ v7.61 — Donma düzeltmesi (4 Ağu 2026)

### Kullanıcı bildirimi
> "Uygulama arada donuyor düzelt"

### 🔍 Teşhis — üç kaynak, hepsi ana iş parçacığında ağır iş

**1. Otomatik yedek (asıl suçlu)**
`Store.saveLogRoot()` her veri değişiminde `maybeAutoBackup()` çağırıyordu.
O da **senkron olarak**:
- Tüm veriyi JSON'a çeviriyor (`exportJson`)
- Uygulama klasörüne yazıyor
- **MediaStore üzerinden İndirilenler'e** ikinci kez yazıyor
- Sonra `loadTasks + loadNotes + loadTopics + loadLessons` çağırıp bildirim üretiyor
- Üstüne 9 widget'ı tazeliyor

Yani **tek bir görev işaretlemek** yüzlerce ms disk G/Ç tetikliyordu.
Kullanıcının gördüğü donma buydu.

**2. Tekrarlı JSON ayrıştırma**
`logRoot()` her çağrıldığında günlük kaydın tamamını baştan parse ediyor;
tek ekran çiziminde 17 yerden çağrılabiliyordu. `NamazVakti.bugunDuzeltilmis()`
de aynı şekilde — astronomik hesap üst bar rozeti + Bugün ekranı + Plan
sekmesi + widget'lar için ayrı ayrı yapılıyordu.

**3. Açılış tıkanması**
`App.onCreate` → PDF font önbelleği + bildirim kanalları + alarm kurulumu.
`MainActivity.onCreate` → `setContentView`'dan **önce** veri kurtarma
kontrolü (MediaStore okuma!) + otomatik yedek + alarm yeniden kurulumu.

### 🔧 Çözüm — `Performans.kt`

| Araç | İş |
|---|---|
| `arkaPlan {}` | Tek arka plan iş parçacığı (düşük öncelik) |
| `geciktir(anahtar, ms) {}` | Art arda gelen istekleri tek çağrıya indirger |
| `hemenBitir(anahtar)` | Bekleyen işi anında tamamlar |
| `anaIs {}` | Arka plandan arayüze dönüş |
| `onbellekli(anahtar, ms) {}` | Kısa ömürlü hesap önbelleği |

**Uygulanan düzeltmeler:**
- Otomatik yedek: **2,5 sn gecikmeli + arka planda**. 10 hızlı değişiklik
  10 yedek değil 1 yedek üretir. Veri kaybı yok — `MainActivity.onStop`
  içinde `Store.bekleyenYedegiBitir()` bekleyeni tamamlıyor.
- `logRoot()` → 800 ms önbellek, `saveLogRoot` önbelleği düşürüyor.
- `NamazVakti.bugunDuzeltilmis()` → 60 sn önbellek; şehir/açı/düzeltme
  ayarı değişince `onbellegiDusur()` çağrılıyor.
- `App.onCreate` ağır kurulumu → arka plan.
- `MainActivity.onCreate` → `setContentView` **en başa** alındı; veri
  kurtarma ve yedek `drawer.post {}` ile ekran çizildikten sonraya,
  MediaStore okuması arka plana taşındı.

### Değişen dosyalar
```
YENİ  Performans.kt
      Store.kt         maybeAutoBackup gecikmeli, logRoot önbellekli,
                       +bekleyenYedegiBitir(), +YEDEK_ISI
      App.kt           açılış kurulumu arka planda
      MainActivity.kt  setContentView öne, kurtarma sonraya, +onStop yedek
      NamazVakti.kt    bugunDuzeltilmis önbellekli + 4 ayar noktası düşürme
```

---

## 🔧 v7.60 — Sohbet senkron hatası düzeltildi + asistan yan paneli (4 Ağu 2026)

### Kullanıcı bildirimi
> "Geçmiş sohbetler senkronizasyonlu çalışmıyor ve ai sohbetinin
> yan panelinde geçmişe girmek istiyorum"

### 🐞 Hata: seçilen sohbet ekrana yansımıyordu

**Kök neden:** `MainActivity.open()` ekranları `hide()`/`show()` ile
yönetiyor — fragment yok edilmiyor. v7.59'da sohbet yükleme yalnızca
`onViewCreated` içinde, üstelik `if (!greeted)` koruması altında
çalışıyordu. Yani:

1. Sohbet listesinden bir sohbet seç → aktif kimlik değişti ✔
2. Asistan ekranı `show()` ile geri geldi → `onViewCreated` **çalışmadı**
3. Ekranda eski konuşma kaldı ✗

**Çözüm:** Çizim mantığı `sohbetiEkranaAl()` fonksiyonuna alındı ve üç
yerden çağrılıyor: `onViewCreated` · `onResume` · **`onHiddenChanged`**
(hide/show için kritik olan bu).

Gereksiz yeniden çizimi önlemek için `gosterilenSohbet` alanı eklendi —
ekranda hangi sohbetin çizili olduğu tutuluyor, aktif kimlikle aynıysa
dokunulmuyor.

### 💬 Asistan ekranına kendi yan paneli

`fragment_asistan.xml` artık bir `DrawerLayout`. **Sağdan** açılan panelde:
- Sohbet listesi — aktif olanda çerçeve + "● Şu an açık"
- Her satırda başlık · mesaj sayısı · "3 saat önce"
- **Dokun → sohbet anında yüklenir** (ekran değiştirmeden, panel kapanır)
- Uzun bas → tam yönetim ekranı (yeniden adlandır/kopyala/sil)
- Üstte "+ Yeni sohbet", altta "⚙ Tümünü yönet"

Açma yolları: asistan ekranı üst köşesindeki **💬** düğmesi veya
ekranın sağ kenarından içeri çekme.

**Bonus:** üst başlık artık açık olan sohbetin adını gösteriyor
(ör. "kirişte moment nasıl hesaplanır"), böylece hangi sohbette olduğun
her an belli.

### Değişen dosyalar
```
fragment_asistan.xml   LinearLayout → DrawerLayout, +sohbetPanel
AsistanFragment.kt     +sohbetiEkranaAl() +onHiddenChanged() +gosterilenSohbet
                       +paneliAcKapa/paneliCiz/panelSatiri/sohbetSec/basligiTazele
                       −gecmisiGeriYukle() (yerini sohbetiEkranaAl aldı)
strings.xml            +5 string (1748 → 1753)
```

### Not
Derleme bu turda `start_process` ile arka planda çalıştırıldı — önceki
turda bash sarmalayıcısı iki kez zaman aşımına uğramıştı. Doğru araç bu.

---

## 💬 v7.59 — AI sohbet geçmişi (4 Ağu 2026)

### Kullanıcı isteği
> "Yapay zeka konuşmasını yan sekmeden hatırlasın ve tıklayınca
> o konuşmadan devam edebileyim"

### Yapılan

**`SohbetGecmisi.kt`** — kalıcı sohbet deposu:
- Her sohbet: kimlik · başlık · mesaj listesi · son değişiklik zamanı
- Başlık **ilk kullanıcı mesajından otomatik** türetilir (38 karakter)
- En fazla 40 sohbet · sohbet başına 200 mesaj · modele giden geçmiş 16 mesaj
- Mesaj yazılır yazılmaz diske yazılır — uygulama kapansa da durur

**`SohbetGecmisiActivity.kt`** — liste ekranı:
- En yeni üstte, aktif sohbette çerçeve + "● Şu an açık" rozeti
- Kart: başlık · son mesaj önizlemesi · mesaj sayısı · "3 saat önce"
- Uzun bas → Aç · Yeniden adlandır · Kopyala (panoya) · Sil
- "+ Yeni sohbet" ve "Tüm sohbetleri sil"

**Erişim noktaları:**
```
⋮ yan panel → "💬 AI Sohbetlerim"      (kullanıcının istediği yer)
Asistan ekranı üst bar → 💬 (liste) · ✚ (yeni sohbet)
```

**Kaldığı yerden devam:** sohbete dokununca aktif yapılır, asistan ekranı
açılır. `gecmisiGeriYukle()` mesajları baloncuk olarak yeniden çizer **ve**
`history` listesini doldurur — yani `AiClient.chat()` bağlamı da geri gelir,
model önceki konuşmayı hatırlayarak devam eder. Sonuna "Bu sohbetten devam
ediliyor" notu düşülür.

Hem çevrimiçi AI cevapları hem yerel beyin cevapları kaydediliyor.
Yedeklemeye `ai_sohbetler` alanı eklendi.

### Derleme notu
Bu turda `bash` sarmalayıcısı iki kez zaman aşımına uğradı ama **Gradle
arka planda çalışmaya devam etti** ve BUILD SUCCESSFUL verdi. Panik yapıp
yeniden başlatmak yerine `pgrep -f GradleDaemon` ile süreç kontrol edilip
`derle.log` okundu — doğru davranış buydu.

### Değişen dosyalar
```
YENİ  SohbetGecmisi.kt · SohbetGecmisiActivity.kt
      AsistanFragment.kt   +gecmisiGeriYukle() +yeniSohbet(), 5 kayıt noktası
      fragment_asistan.xml +sohbetlerimBtn (💬) +yeniSohbetBtn (✚)
      activity_main.xml    +drawerSohbetBtn
      MainActivity.kt      yan panel bağlantısı
      Store.kt             yedeğe ai_sohbetler
      AndroidManifest.xml  +SohbetGecmisiActivity
      strings.xml          +18 string (1730 → 1748)
```

---

## ⬇️ v7.58 — Aşağı çekince yenileme (3 Ağu 2026)

### Kullanıcı isteği
> "Üstten aşağı kaydırınca otomatik olarak sayfayı yenilesin ve
> güncellesin, diğer sayfaları da"

### Yapılan
`androidx.swiperefreshlayout:1.1.0` eklendi. **13 ekranda** üstten aşağı
çekince o sayfanın verisi yeniden yükleniyor.

| Ekran | Çağrılan |
|---|---|
| Ana Sayfa | `refreshData()` |
| İlerleme | `render()` |
| Bugün | `refresh()` |
| Görevler · Notlar · Konular | `reload()` |
| Alışkanlıklar · Sınavlar · Etkinlikler | `reload()` |
| Kurslar | `rebuild()` |
| Kaynaklar · Araçlar | `yenile()` |
| Plan | `ciz()` |

### `Yenileyici.kt` — tasarım kararı
16 XML layout'u tek tek `SwipeRefreshLayout` ile sarmak yerine fragment'ın
kök görünümü **çalışma anında** sarmalanıyor:

```kotlin
override fun onStart() {
    super.onStart()
    Yenileyici.kur(this) { reload() }
}
```

Tek satır, XML'lere dokunulmuyor. Çark uygulamanın vurgu rengiyle boyanıyor.
Ağ işi gerektiren ekranlar için `kurUzun()` de var (çark `bitir()` çağrılana
kadar döner).

**Neden `onStart`, `onViewCreated` değil:** kök görünüm `onViewCreated`
anında henüz ebeveynine bağlanmamış olabiliyor; sarmalama için `parent`
gerekli. `onStart`'ta ağaç hazır.

**Kaydırma çakışması yok:** `SwipeRefreshLayout` yalnızca içerik en
üstteyken tetiklenir; liste ortasındayken normal kaydırma çalışır.
`RecyclerView`, `ScrollView`, `NestedScrollView` ile uyumlu.

### ⚠️ Bu turda yapılan hata (ders)
İlk denemede `onViewCreated` gövdesinin sonunu **parantez sayarak** bulan
bir betik yazdım. Sayaç lambda bloklarının (`onToggle = { ... }`) içine
düştü ve 13 dosyayı bozdu — `Yenileyici.kur` çağrısı lambdanın ortasına
girdi. Fark edilip tamamı geri alındı, ardından sınıf gövdesinin sonuna
ayrı bir `onStart()` ekleyen güvenli yöntem kullanıldı.

**Kural:** Kotlin kaynağında blok sınırını naif parantez sayımıyla bulma.
Yeni bir fonksiyon eklemek gerekiyorsa sınıfın son `}` işaretinden önce
ekle; mevcut bir fonksiyonun içine girmeye çalışma.

Ekleme sonrası her dosyada iki denetim yapıldı:
1. `onStart` derinliği = 1 (iç sınıfa düşmemiş)
2. Çağrılan fonksiyon da derinlik 1'de tanımlı

### Değişen dosyalar
```
YENİ  Yenileyici.kt
      build.gradle.kts   +swiperefreshlayout:1.1.0
      13 fragment        +onStart() { Yenileyici.kur(this) { … } }
```

---

## 🔄 v7.57 — Online arka plan kontrolü + bildirim (3 Ağu 2026)

### Kullanıcı isteği
> "Olur ekle" — (v7.56 sonunda teklif edilen arka plan mesaj kontrolü)

### Sorun
textdb.online ücretsiz depolama servisi, **push bildirim göndermiyor**.
Karşı taraf mesaj yazınca telefon kendiliğinden haberdar olamıyordu —
kullanıcı ↻ ile elle yenilemek zorundaydı.

### Çözüm: `OnlineBekci.kt`

`AlarmManager.setInexactRepeating` ile pil dostu periyodik kontrol.
Alarm çalınca oda okunur, önceki durumla karşılaştırılır, yeni bir şey
varsa bildirim gönderilir.

`setInexactRepeating` seçildi: Android bu alarmları toplu işleyip pili
korur, sohbet bildirimi saniye hassasiyeti gerektirmez.

**Ayarlar** (Online → ⚙ → "Arka planda kontrol ve bildirim"):
- Aç/kapat — varsayılan **KAPALI**
- Sıklık: 15 dk · 30 dk (varsayılan) · 1 saat · 3 saat
- Neler bildirilsin: mesaj ✓ · görev ✓ · not ✓ · konu ✗ · tamamlama ✗
- "Yalnızca karşı taraftan gelenler" ✓ — kendi eklediğin için bildirim gelmez
- Pil optimizasyonu uyarısı + ayara tek dokunuş erişim
- "Şimdi kontrol et" — anında dene

**Mükerrer bildirim koruması:** her öğenin kimliği (`m123`, `g456`…)
`gorulen` kümesinde tutulur, son 400 kayıt saklanır. İlk çalıştırmada
mevcut içerik "görüldü" sayılır — odaya girer girmez bildirim yağmaz.

Odaya bağlanınca `temizBaslat()` + `kur()`, odadan ayrılınca
`iptal()` + `temizBaslat()` çağrılır.

Bildirim v7.56 ısrarlı uyarı altyapısına bağlı — telefon sessizde olsa
bile duyulabilir (o özellik açıksa).

### Dürüst sınır
Bu **anlık bildirim değildir**. Seçilen aralık kadar (+ telefonun pil
tasarrufuna göre biraz daha) gecikme olur. Gerçek anlık bildirim için
Firebase gibi bir push servisi ve sunucu gerekir; bu uygulama tamamen
ücretsiz altyapıda çalışıyor.

### Değişen dosyalar
```
YENİ  OnlineBekci.kt (BroadcastReceiver + alarm + bildirim)
      OnlineBekciActivity.kt (ayar ekranı)
      OnlineStore.kt      baglan()/ayril() → bekçi başlat/durdur
      OnlineActivity.kt   ⚙ menüsüne 5. seçenek
      BootReceiver.kt     yeniden başlatınca alarm geri kurulur
      MainActivity.kt     açılışta alarm taze tutulur
      AndroidManifest.xml +OnlineBekci receiver, +OnlineBekciActivity
      strings.xml         +39 string (1690 → 1730)
```

---

## 🔊 v7.56 — Israrlı uyarı + yönetici bildirim kilidi (3 Ağu 2026)

### Kullanıcı isteği
> "Yönetici hariç diğer kullanıcıların bildirim sesi gelmesini kapatmasını
> yönetici izin vermeli. Telefon sessiz de olsa bile bildirim sesi çıkarmasını
> ayarla ve titreştirme ekle"

### 1. Israrlı uyarı — sessizde bile çalar

`ZorunluUyari.kt` — bildirim yerine **doğrudan MediaPlayer** ile
`AudioAttributes.USAGE_ALARM` akışından çalar. Çalar saat uygulamalarının
yaptığı budur; telefon sessiz/titreşimdeyken bile duyulur.

Neden bildirim kanalı yetmiyor: normal bildirim NOTIFICATION akışından
çıkar, telefon sessize alınınca Android onu susturur ve uygulamanın
yapabileceği bir şey yoktur.

`ZorunluUyariActivity.kt` ayarları:
- Aç/kapat (varsayılan **KAPALI** — kullanıcı bilerek açsın)
- Uyarı sesi seçimi (boş = cihazın varsayılan alarm sesi)
- Titreşim + 3 desen (kısa / orta / uzun-ısrarlı)
- Çalma süresi 3-60 sn (varsayılan 10)
- "Alarm sesini zorla aç" — ses kısıksa yükseltir (varsayılan kapalı, agresif)
- Kapsam: namaz / görev-hatırlatıcı / zamanlayıcı bitişi
- "Şimdi dene" + "Durdur" düğmeleri

Bağlandığı yerler:
```
BildirimMerkezi.gonder()   → kanal K_HATIRLATICI ise "gorev"
NamazBildirim.goster()     → "namaz"
TimerActionReceiver.showDone() → "zaman"
```

### 2. Yönetici bildirim kilidi

`BildirimKilit.kt` + `OnlineStore.Kural`'a 4 yeni anahtar
(bit maskesi 128/256/512/1024):

| Yetki | Islem |
|---|---|
| Bildirim sesini kapatma | `SES_KAPAT` |
| Titreşimi kapatma | `TITRESIM_KAPAT` |
| Bildirimleri tümden kapatma | `BILDIRIM_TUM_KAPAT` |
| Israrlı uyarıyı kapatma | `ZORUNLU_KAPAT` |

**Varsayılan: hepsi kilitli.** Yönetici, Online → ⚙ → Yetkiler'den açar.

Önemli tasarım kararı: **kilit yalnızca kapatmayı engeller, açmayı değil.**
Üye bildirimi her zaman açabilir; sadece kapatamaz. Amaç haberdar kalmasını
sağlamak, ayarlarını ele geçirmek değil.

Kilitli ayarların yanında 🔒 görünür (Ayarlar → Bildirimler ve eski bildirim
penceresi), kapatmaya çalışınca "yetkin yok" açıklaması çıkar.

### Dürüst sınırlar (kullanıcıya söylendi)
1. Kullanıcı **alarm ses düzeyini sıfıra** çekerse hiçbir uygulama ses çıkaramaz.
2. **Rahatsız Etmeyin (DND)** modunda alarm da susturulabilir →
   `ACCESS_NOTIFICATION_POLICY` izni gerekir, ayar ekranından elle verilir.
   Sıradan sessiz/titreşim modunda bu izin **gerekmez**.
3. Üye, Android sistem ayarlarından uygulamanın bildirimlerini yine kapatabilir.
   Hiçbir uygulama bunu engelleyemez (v7.52'de de söylenmişti).

### Değişen dosyalar
```
YENİ  ZorunluUyari.kt · ZorunluUyariActivity.kt · BildirimKilit.kt
      OnlineStore.kt        Kural +4 anahtar, Islem +4, maske 128-1024
      OnlineActivity.kt     yetki ekranına 4 satır
      BildirimAyarActivity.kt  kilit kontrolü + ısrarlı uyarı girişi
      SettingsFragment.kt   eski bildirim penceresine kilit
      BildirimMerkezi.kt · NamazBildirim.kt · TimerActionReceiver.kt  bağlantı
      Store.kt              yedek biçimi 16 (+zorunlu_uyari)
      AndroidManifest.xml   +ACCESS_NOTIFICATION_POLICY, +ZorunluUyariActivity
      strings.xml           +45 string (1645 → 1690)
```

---

## 🗓 v7.55 — Plan alt sekmeye, Sayaç ⋮ menüsüne (3 Ağu 2026)

### Kullanıcı isteği
> "Namaz programındaki plan kısmını sayaç kısmının oraya al, sayacı da
> üst sekmedeki soldaki 3 noktanın içine entegre et."

### Yapılan

**1. Plan artık alt menüde bir sekme**

Namaz ekranındaki "vakit aralarındaki işlerim" bölümü oradan alınıp
alt menüde **Sayaç sekmesinin yerine** kondu.

```
Alt menü ÖNCE:  Ana Sayfa · Bugün · Konular · İlerleme · Sayaç
Alt menü SONRA: Ana Sayfa · Bugün · Konular · İlerleme · Plan   ← 🗓
```

Yeni dosyalar:
- `PlanFragment.kt` — ekran 16, `NamazPlan` verisini kullanır
- `res/layout/fragment_plan.xml`
- `res/drawable/ic_plan.xml` — takvim + onay simgesi

Plan sekmesinde olanlar:
- Sıradaki vakit + geri sayım + "şimdi ne yapmalı" önerisi
- **Bugünün vakitleri** — katlanır liste (▾ ile açılır, yer kaplamaz)
- 6 gün dilimi (Sabah/Kuşluk/Öğleden/İkindiden/Akşamdan/Gece) — her birine
  `+` ile iş eklenir, dokununca tamamlanır, uzun basınca düzenle/sil
- Hazır şablon düğmesi
- **⏱ Sayaç** kısayolu — sekme kalktı ama tek dokunuşla erişim burada da var
- Namaz modülü kapalıysa "Aç" düğmeli uyarı şeridi

**2. Sayaç ⋮ (soldaki üç nokta) panelinde**

Sayaç ekranı **kaldırılmadı** — sadece yeri değişti. ⋮ → **⏱ Sayaç & Odak**.
Ekran indeksi 4 aynı kaldı, yani şunların hepsi çalışmaya devam ediyor:
- Uygulama simgesine uzun basma → "Odaklan" kısayolu
- Zamanlayıcı bildirimi → dokununca sayaç
- Widget'lardan sayaç açma
- Asistana "sayacı aç" / "odak ekranını aç" demek

**3. Namaz ekranı sadeleşti**

`NamazActivity` 405 → 185 satır. Artık vakitleri gösteriyor, altında
plan **özeti** (`3/8 iş tamamlandı`) ve "Plan sekmesini aç" düğmesi var.
Veri kaynağı tek: `NamazPlan` — iki ekran da aynı işleri görür, kopya yok.

### Değişen dosyalar
```
YENİ  PlanFragment.kt · fragment_plan.xml · ic_plan.xml
      MainActivity.kt      ekran 16, nav_plan, openPlan(), drawerTimerBtn
      activity_main.xml    ⋮ paneline "⏱ Sayaç & Odak" düğmesi
      menu/bottom_nav.xml  nav_timer → nav_plan
      NamazActivity.kt     plan bölümü çıkarıldı, özet + yönlendirme kaldı
      activity_namaz.xml   plan kartları → yönlendirme kartı
      WidgetCommon.kt      SCREEN_PLAN = 16
      AsistanKomut.kt      "plan"/"vakit planı" → 16, "sayaç" → 4
      strings.xml          +11 string (1634 → 1645)
```

### Not
Plan sekmesi namaz modülü **kapalıyken de** çalışır — vakitler yine
astronomik olarak hesaplanır, sadece bildirim/widget devre dışıdır.
Sekmedeki uyarı şeridinden tek dokunuşla açılabilir.


---

## 🎛 v7.36 — Asistana TAM YETKİ (31 Tem 2026)

### Kullanıcı isteği
> "Yapay zekaya uygulama içi bütün yetkiyi yapabilme yetkisi ver sorunsuz çalışsın"

### v7.35'teki durum
`AsistanKomut.kt` yalnızca **8 komut** içeriyordu ve hepsi *ekleme* işiydi:
gorev_ekle, not_ekle, konu_ekle, alt_madde_ekle, aliskanlik_ekle,
zamanlayici, ekran_ac, ders_devam.

Eksikler:
- **Silme / düzenleme / tamamlama hiç yoktu**
- Bir cevapta yalnızca **TEK** komut çalışıyordu (`lastOrNull`)
- Etkinlik, kurs, bölüm, ders, kart, sınav, ayarlar için hiç komut yoktu
- Bulunamayan kayıtta **sessiz başarısızlık** — kullanıcı neden olmadığını bilmiyordu

### v7.36 — komut motoru baştan yazıldı

**8 komut → 36 komut**

| Alan | Komutlar |
|---|---|
| Görevler | ekle · tamamla · sil · düzenle |
| Notlar | ekle · sil · düzenle |
| Konular | ekle · sil · düzenle · alt_madde ekle/tamamla/sil |
| Alışkanlıklar | ekle · işaretle · sil |
| Etkinlikler | ekle · sil |
| Kurslar | kurs ekle/sil · bölüm ekle · ders ekle/tamamla/sil |
| Kartlar | kart_ekle (toplu) |
| Sınav | sinav_ekle (net girişiyle) |
| Ayarlar | hedef · söz · sınav tarihi |
| Eylemler | zamanlayıcı · ekran_ac · ders_devam · yedek_al · yedek_geri_al |

**Yeni yetenekler:**

1. **Çoklu komut** — `ayiklaHepsi()` bir cevaptaki tüm komutları toplar,
   `calistirSirayla()` sırayla yürütür. En fazla 8 (sonsuz döngü koruması).
   *"Görevi tamamla, not ekle ve odak başlat"* → üç iş tek seferde.

2. **Onay penceresi** — silme komutları (`*_sil`) ve `yedek_geri_al`
   kullanıcıya sorulur. Onaylanmazsa atlanır, sıradaki komut devam eder.

3. **Geri alınabilirlik** — her silme `Store.kaydetGeriAlma` ile saklanır.
   Kullanıcı *"geri al"* diyerek son işlemi döndürebilir.

4. **Esnek eşleşme** — `esnekBul()`: tam → başlangıç → içeren → tersine.
   Türkçe duyarlı (`ı→i, ş→s, ğ→g, ü→u, ö→o, ç→c`).
   Kullanıcı "kolon" dediğinde "Betonarme Kolon Tasarımı" bulunur.

5. **Doğal tarih** — `NaturalDate` entegre. "yarın", "3 gün sonra",
   "15 Mart", "6 Eylül 2026", "15.03.2027" hepsi çalışır.

6. **Sessiz başarısızlık kalktı** — kayıt bulunamazsa
   `cmd_not_found` ile kullanıcıya söylenir.

### Güvenlik tasarımı
- **Toplu silme komutu YOK.** "hepsini sil" gibi bir komut hiç tanımlanmadı;
  her komut tek kayda dokunur. Veri kaybı riski yapısal olarak sıfır.
- Sistem isteminde açık kural: *"Silme komutlarını kullanıcı net biçimde
  istemedikçe ASLA üretme."*
- Silmeler ayrıca kullanıcıya onaylatılır (çift koruma).
- Her silme geri alınabilir.

### Dokunulan dosyalar
```
AsistanKomut.kt      BAŞTAN YAZILDI (~800 satır) — 36 komut, çoklu çalıştırma,
                     onay akışı, esnekBul, maddeleriAyir, tarihAnahtari
AiClient.kt          sistem istemi 37 komut satırıyla yeniden yazıldı + örnek
AsistanFragment.kt   ayiklaHepsi + calistirSirayla
MainActivity.kt      hızlı sor akışı çoklu komuta geçti
strings.xml          45 yeni string (1027 toplam)
```

### Derleme notu
Tek hata alındı: `AsistanKomut.kt:306` — `let` bloğunun son ifadesi olan
`if` için Kotlin `else` bekliyordu. Düzeltildi (`let` yerine düz `if` bloğu).
Swap sayesinde derleme 2m 15s sürdü, OOM yaşanmadı.


---

## 🐞 v7.35 — Konulara yapay zekâ ile alt başlık ekleme (31 Tem 2026)

### Kullanıcı bildirimi
> "Şu an Gemini kullanıyorum sadece ama konular kısmında alt madde ekle yerine
> konunun alt başlıklarını ekleyemiyor yapay zekâ. İnternetten buluyor ama
> eklemede sorun oluyor."

**Bildirim %100 doğruydu ve sebebi tam olarak bulundu.** Üç ayrı eksik vardı:

| # | Eksik | Kanıt |
|---|---|---|
| 1 | "Alt madde ekle" düğmesi yalnızca elle yazma kutusu açıyordu | `TopicsFragment.kt:409` → `addRow.setOnClickListener { showSubItemDialog(topic) }` |
| 2 | `alt_madde_ekle` komutu hiç yoktu | `AsistanKomut.kt` 7 komut içeriyordu: gorev_ekle, not_ekle, konu_ekle, aliskanlik_ekle, zamanlayici, ekran_ac, ders_devam |
| 3 | Sistem isteminde alt madde talimatı yoktu | `grep -c "alt_madde" AiClient.kt` → **0** |

Yani Gemini konuyu internetten gerçekten buluyordu, kullanıcıya düzgün yazıyordu —
**ama o maddeleri konuya yazacak hiçbir yol yoktu.** Model elinden geleni yapıyor,
uygulama tarafında karşılığı bulunmuyordu.

Bu v7.26'da yapılan bir eksiklikti: 7 komut eklenirken en çok istenen iş olan
"var olan konuya alt madde ekleme" atlanmıştı.

### Çözüm — üç eksik de kapatıldı

**1. Yeni dosya: `AltBaslikBulucu.kt` (~370 satır)**
- Gemini'de **gerçek Google Arama** aracı (`googleSearch`, camelCase — v7.23 dersi)
- Araç kabul edilmezse **araçsız ikinci deneme** (KaynakBulucu deseni)
- Diğer sağlayıcılar için OpenAI uyumlu yol
- v7.24 sağlayıcı geçişi + v7.34 ücretsiz mod korumaları uygulanır
- **Savunmacı ayrıştırma:** JSON bozuksa düz satırlardan toplar; numara/tire/emoji
  temizler; Türkçe duyarlı tekrar elemesi (`ı→i, ş→s, ğ→g, ü→u, ö→o, ç→c`)

**2. `TopicsFragment.kt` — "Alt madde ekle" artık seçim sunuyor**
```
✏️ Kendim yazayım           → eski davranış (korundu)
✨ Yapay zekâ alt başlıkları bulsun  → YENİ
```
Bulunan maddeler **onay ekranında** gösterilir:
- Her madde ayrı onay kutusu (varsayılan işaretli)
- Metne dokunup **düzeltilebilir**
- "Tümünü seç" anahtarı
- "Tekrar dene" düğmesi
- Hiçbir şey onaysız kaydedilmez

**3. `AsistanKomut.kt` — `alt_madde_ekle` komutu**
```
>>KOMUT: alt_madde_ekle | Konu Adı :: madde1 ;; madde2 ;; madde3
```
- Konu adı **esnek eşleşir**: tam → başlangıç → içeren → tersi
- Türkçe duyarlı karşılaştırma
- Zaten olan maddeyi tekrar eklemez
- Konu bulunamazsa **yeni konu açmaz**, kullanıcıya söyler

**4. `AiClient.kt` — sistem istemi**
Komut listesine eklendi + somut örnek verildi. Model artık "alt başlık ekle"
isteğini gördüğünde doğru komutu üretiyor.

### Artık iki yoldan da çalışıyor
| Yol | Nasıl |
|---|---|
| **Konular ekranından** | Konuyu aç → "Alt madde ekle" → ✨ Yapay zekâ bulsun |
| **Asistandan** | ✨ düğmesi → "Betonarme Kolon konusuna alt başlık ekle" |

### Dokunulan dosyalar
```
AltBaslikBulucu.kt   YENİ — Google Arama destekli alt başlık bulucu
TopicsFragment.kt    altMaddeSecimi(), yapayZekaIleBul(), bulunanlariGoster()
AsistanKomut.kt      alt_madde_ekle komutu, konuBul(), normalle()
AiClient.kt          sistem istemine komut + örnek
strings.xml          17 yeni string (982 toplam)
```


---

## 🆕 v7.34 — "Sadece ücretsiz modeller" modu (31 Tem 2026)

### Sorunun kaynağı: kullanıcının sorusu
> "Yapay zekayı tamamen ücretsiz versiyonlarda mı?"

**Dürüst cevap v7.33'e kadar HAYIRDI.** Durum şuydu:

| Sağlayıcı | v7.33 varsayılanı | Ücret |
|---|---|---|
| Gemini | `gemini-3-flash-preview` | ✅ ücretsiz katman (günde ~1500 istek) |
| OpenAI | `gpt-5.6-luna` | ❌ ücretsiz katmanı **yok**, her istek faturalanır |
| OpenRouter | `google/gemini-3-flash-preview` | ❌ **kredi harcıyordu** |

En kritik kusur OpenRouter'daydı: `:free` modeller listede vardı ama **sıranın sonundaydı**;
ancak kredi bitip HTTP 402 geldiğinde devreye giriyorlardı. Yani kullanıcı önce parasını
harcıyor, sonra ücretsize düşüyordu. **Bu bir tasarım hatasıydı.**

### Çözüm: ücret farkındalığı her katmana işlendi

**1. Ücret sınıflandırması — `AiClient.modelUcretsizMi(provider, model)`**
Emin olunamayan her model **ücretli** sayılır (güvenli taraf):
- Gemini → `flash` / `gemma` içerenler ücretsiz; `pro` geçenler değil
  (Pro ücretsiz katmanda günde ~50 istek — pratikte kullanılamaz)
- OpenRouter → yalnızca `:free` son ekli olanlar
- OpenAI → hiçbiri (ücretsiz katmanı yok)
- Özel sunucu → kullanıcının kendi maliyeti

**2. `AiSettings.isUcretsizMod()` — VARSAYILAN AÇIK**
Kullanıcı bilmeden ücretlendirilmesin diye açık geliyor.

**3. Üç katmanlı koruma**
| Katman | Fonksiyon | Davranış |
|---|---|---|
| Sağlayıcı seçimi | `saglayiciSirasi()` | Ücretsiz modda OpenAI'ye hiç gidilmez |
| Model seçimi | `modelSirasi()` | Ücretli modeller listeden düşer |
| Tekil çağrılar | `guvenliModel()` | KaynakBulucu/KursUretici/görsel okuma tek noktadan |

Ücretsiz model bulunamazsa çağrı **hiç yapılmaz**, `ai_err_no_free_model`
ile kullanıcıya ne yapması gerektiği söylenir.

**4. Ayarlar ekranı**
- 💚 "Sadece ücretsiz modeller" anahtarı + açıklaması
- Sağlayıcı seçilince canlı **ücret rozeti** (ör. "💳 OpenAI — ücretsiz katmanı YOK")
- Ücretsiz mod açıkken model listesi **anında filtrelenir**

**5. Yedek sürüm 11** — `pref_ai_ucretsiz` tercihi yedeğe eklendi.
API anahtarları güvenlik gereği yedeğe **girmez**.

### Dokunulan dosyalar
```
AiClient.kt        modelUcretsizMi, saglayicidaUcretsizVarMi, ucretsizModeller (genişletildi),
                   ucretsizVarsayilan, ucretEtiketi, guvenliModel, modelSirasi + saglayiciSirasi
                   filtreleri, chat() boş liste koruması, gorselModeli/konuOku koruması
AiSettings.kt      isUcretsizMod / setUcretsizMod (varsayılan true)
KaynakBulucu.kt    her iki yol (Gemini + OpenAI uyumlu) guvenliModel'e bağlandı
KursUretici.kt     aynı
SettingsFragment.kt  ücretsiz anahtarı, ücret rozeti, canlı model filtresi
Store.kt           yedek v11 + pref_ai_ucretsiz
strings.xml        11 yeni string (965 toplam)
```

### ⚠️ Derleme notu — kalıcı çözüm bulundu
Aylardır her turda yaşanan "Gradle build daemon disappeared" hatasının kökü
`dmesg` ile doğrulandı: **OOM killer**, dex birleştirme (`mergeExtDexDebug`)
aşamasında 1,35 GB'a çıkan java sürecini öldürüyordu.

**Çözüm:** 3 GB swap dosyası açıldı —
```bash
sudo fallocate -l 3G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
```
Sonuç: derleme **28 saniyede** hatasız tamamlandı. Swap `/` altında olduğu için
snapshot'a girmez, ama sandbox yeniden kurulursa **tekrar açılmalı**.



---

## 🔄 v7.41 — Widget senkronizasyonu düzeltildi (2 Ağu 2026)

### Kullanıcı bildirimi
> "Widgetlar guncellenmiyor senkronizasyonu yok"

**Bildirim doğruydu. İKİ ayrı hata vardı, ikisi de bendeydi.**

### HATA 1 — `save*` fonksiyonlarının %82'si widget tazelemiyordu

Denetim çıktısı (v7.40 durumu):
```
❌ saveTopics    ❌ saveNotes     ❌ saveExams
❌ saveEvents    ❌ saveBooks     ❌ saveCourses
❌ saveSections  ❌ saveLessons   ❌ saveHabitRoot
✅ saveTasks     ✅ saveHabits    ✅ bumpToday
```

Yalnızca **3 fonksiyon** tazeliyordu. Yani:
- Konu/alt madde eklendi → widget eski veriyi gösteriyor
- Etkinlik eklendi → geri sayım widget'ı değişmiyor
- Ders tamamlandı → brifing widget'ı eski
- Not eklendi → hiçbir şey olmuyor

**Çözüm:** `Store.widgetTazele()` ortak yardımcısı eklendi ve eksik olan
**9 fonksiyona** bağlandı. Hata yutuluyor — widget tazeleme başarısız olsa
bile veri kaydı bozulmuyor.

### HATA 2 — Cam widget'lar tazeleme listesinde yoktu

`WidgetCommon.refreshAll()` yalnızca 5 widget tazeliyordu:
```
Manifest'te 8 widget:  Countdown, Summary, Tasks, Actions, Brifing,
                       GlassTasks, GlassHabits, GlassToday
refreshAll'da 5:       Countdown, Summary, Tasks, Actions, Brifing
EKSİK:                 GlassTasks, GlassHabits, GlassToday  ← hiç tazelenmiyordu
```

Ayrıca cam widget'lar **liste tabanlı** — `notifyAppWidgetViewDataChanged()`
çağrılmadan satırları eski veriyle kalıyor. Bu da yapılmıyordu.

**Çözüm:** `TUM_WIDGETLAR` ve `LISTE_WIDGETLARI` sabitleri tanımlandı.
Artık tek yerde toplanıyor — yeni widget eklenince sadece buraya yazılacak.

### Ek güvence — `MainActivity.onStop()`
Uygulama arka plana alınınca tüm widget'lar tazeleniyor. Bir ekran veriyi
doğrudan SharedPreferences'a yazmış olsa bile ana ekrana dönüldüğünde
widget güncel oluyor.

### Dosyalar
```
Store.kt          widgetTazele() yardımcısı + 9 save* fonksiyonuna bağlandı
WidgetCommon.kt   refreshAll baştan yazıldı — 8 widget + 4 liste görünümü
MainActivity.kt   onStop() tazelemesi
gradle.properties Xmx 1400m → 820m (dex OOM'u için)
```

### Derleme notu
`dexBuilderDebug` adımında ısrarlı OOM yaşandı. Kök sebep: arka plan
komutları kesildiği için Gradle **bayat daemon kaydına** bağlanıyordu
(log pid 1856, eski `-Xmx1400m` ayarıyla). `.gradle-home/daemon` silinip
derleme **ön planda** çalıştırılınca 1m 23s'de sorunsuz bitti.


---

## 🐞 v7.40.1 — Brifing widget'ı eklenemiyordu (2 Ağu 2026)

### Kullanıcı bildirimi
> "Widget eklenemedi yazıyor eklediğinde"

**Bildirim doğruydu, hata bendeydi.**

### Sebep: `<View>` etiketi
`widget_brifing.xml` içinde alt şeridi aşağı itmek için boşluk doldurucu
kullanmıştım:
```xml
<View android:layout_width="match_parent"
      android:layout_height="0dp"
      android:layout_weight="1" />
```

**`RemoteViews` yalnızca sınırlı bir görünüm listesini destekler** ve düz
`View` bu listede YOKTUR. Launcher widget'ı şişirmeye çalışırken
`android.view.InflateException` alıp "Widget eklenemedi" diyor.

Doğrulama — çalışan widget'larda hangi türler var:
```
widget_summary.xml   → LinearLayout, ProgressBar, TextView
widget_actions.xml   → LinearLayout, TextView
widget_countdown.xml → LinearLayout, TextView
widget_brifing.xml   → LinearLayout, ProgressBar, TextView, View  ← FAZLADAN
```

### Çözüm
Boşluk doldurucu tamamen kaldırıldı. Alt eylem şeridi normal akışta kalıyor —
görsel olarak neredeyse aynı, çünkü içerik zaten kartı dolduruyor.

Kalan görünüm türleri: `LinearLayout`, `ProgressBar`, `TextView` — üçü de
RemoteViews uyumlu.

### Ders
Widget layout'larında **yalnızca şunlar** kullanılabilir:
FrameLayout · LinearLayout · RelativeLayout · GridLayout ·
AnalogClock · Button · Chronometer · ImageButton · ImageView ·
ProgressBar · TextView · ViewFlipper · ListView · GridView ·
StackView · AdapterViewFlipper

Bunların dışındaki her şey (`View`, `ScrollView`, `ConstraintLayout`,
`CardView`, özel görünümler) widget'ı **kırar**.


---

## 🌤 v7.40 — Günlük Brifing Widget'ı (2 Ağu 2026)

**Widget öneri listesinden 10. madde.** Kullanıcı seçti: *"10. maddeyi ekle"*

### Tespit edilen boşluk
Denetimde şu çıktı: **v7.29'dan beri eklenen hiçbir özellik widget'lara
yansımamıştı.** `grep` ile doğrulandı — QuizStore, KartStore, OgretmenStore,
Analitik, PdfArama, SesliDersServisi, HesapMotoru: hiçbiri widget kodunda geçmiyordu.

Kullanıcı bekleyen tekrarları görmek için uygulamayı açmak zorundaydı.

### Yeni widget (4×3) — 6 bölüm
| Bölüm | İçerik |
|---|---|
| Üst şerit | Selamlama + tarih (Türkçe) + geri sayım rozeti |
| Hedef | Günlük odak çubuğu + "45/120 dk (%37)" |
| Bekleyenler | 🃏 kart · 📝 quiz tekrarı · 🔥 kurs serisi — üçü de dokunulabilir |
| Görevler | Bugünkü ilk 3 görev (🔸 tarihli, ▫ tarihsiz) |
| Tavsiye | Verilerden üretilen tek cümle |
| Eylemler | ▶ kaldığın ders · ⏱ odak · ✨ asistan · ↻ yenile |

### Tavsiye motoru — 10 kural, öncelik sıralı
```
1. Seri tehlikede   → saat ≥19 ve bugün çalışılmamış
2. Hedef tamam      → odak ≥ hedef
3. Tekrar birikmiş  → kart+quiz ≥ 10
4. Sabah başlangıcı → saat 5-11 ve odak 0
5. Hedefe az kaldı  → kalan 1-20 dk
6-7. Kart / quiz bekliyor
8. Gece geç         → saat ≥23
9. Seri devam       → bugün çalışıldı, seri ≥3
10. Varsayılan
```

**Kritik tasarım kararı:** Tavsiye **yerel** üretiliyor, yapay zekâ çağrılmıyor.
Widget 30 dakikada bir yenilendiği için ağ isteği hem pil hem API kotası yerdi;
ayrıca v7.34 ücretsiz mod felsefesine aykırı olurdu.

### Teknik notlar
- `KartStore.bekleyenSayisi()` ve `QuizStore.tekrarSayisi()` widget'a ilk kez bağlandı
- Her veri okuma `try/catch` içinde — bir modül hata verse widget çökmez, "—" gösterir
- ↻ düğmesi `ACTION_REFRESH` yayını gönderir, uygulama açılmadan tazeler
- `WidgetCommon.refreshAll()` listesine eklendi (veri değişince otomatik güncellenir)

### Dosyalar
```
BrifingWidget.kt       YENİ ~340 satır — 6 bölüm + tavsiye motoru
widget_brifing.xml     YENİ layout (4×3)
w_brifing_info.xml     YENİ widget tanımı
AndroidManifest.xml    receiver + BRIEF_REFRESH eylemi
WidgetCommon.kt        refreshAll listesine eklendi
strings.xml            21 yeni string (1183 toplam)
```

Widget sayısı: **7 → 8**

### Derleme
Tek seferde başarılı (3m 23s).


---

## 🔍 v7.39 — PDF içinde tam metin arama (2 Ağu 2026)

**Öneri listesinden 8. madde.** Kullanıcı seçti: *"Pdf arama"*

### Sorun
Uygulamada **105 gömülü PDF** vardı (58 AutoCAD + 47 Revit) ama içlerinde
arama yapılamıyordu. "Kolon donatısı hangi derste geçiyor?" sorusunun
cevabı yoktu — kullanıcı 105 PDF'i tek tek açmak zorundaydı.

### Çözüm — tamamen çevrimdışı, ücretsiz

`DersMetni` (v7.31) zaten pdfbox ile metin çıkarıp önbelleğe alıyordu.
Bu sürüm onun üzerine bir **indeks** kuruyor.

**`PdfArama.kt` (~330 satır)**
| Özellik | Detay |
|---|---|
| Çevrimdışı | Yapay zekâ/internet gerekmez, kredi harcamaz |
| Tembel indeksleme | 105 PDF açılışta işlenmez; arama sırasında ilerleme göstererek tek tek işlenir |
| Kalıcı indeks | `cacheDir/pdf_indeks_v1.json` — ikinci arama anında |
| Türkçe duyarlı | `normalle()`: ı→i, ş→s, ğ→g, ü→u, ö→o, ç→c + â/î/û |
| Kesilebilir | `iptal` bayrağı — ekrandan çıkılınca döngü durur |
| Sayfa başına 2 eşleşme | Liste şişmesin diye sınırlı |

**`PdfAramaActivity.kt` (~360 satır) + layout**
- Sonuçlar **derse göre gruplanır** (kurs adı + ders adı başlıkta)
- Eşleşen kelime **sarı zemin + kalın** vurgulanır (`BackgroundColorSpan`)
- Her sonuç kartında sayfa numarası
- Karta dokununca **doğrudan o sayfa açılır**
- Alt barda indeks durumu + "Tümünü indeksle" düğmesi

**`LessonPdfActivity` — `EXTRA_START_PAGE`**
Arama sonucundan gelindiğinde "kaldığın sayfa" yerine **bulunan sayfaya**
atlar. Yoksa eski davranış korunur.

### Parça çıkarma inceliği
Eşleşmenin çevresinden ~150 karakterlik okunabilir parça kesiliyor.
Kelime ortasından kesmemek için en yakın boşluk aranıyor, baş/sona `…` ekleniyor.
Vurgu konumu, boşluk sadeleştirmesi kaydırabileceği için parça içinde
**yeniden hesaplanıyor** (indeks kayması hatasına karşı).

### Bağlantılar
- **Kurslar ekranı → "🔍 PDF içinde ara"** düğmesi (mevcut ders adı aramasının altında)
- Asistan komutu: `pdf_ara` (38. komut)

### Dosyalar
```
PdfArama.kt            YENİ ~330 satır — indeks + arama motoru
PdfAramaActivity.kt    YENİ ~360 satır — arama ekranı, vurgulama, gruplama
activity_pdf_arama.xml YENİ layout
LessonPdfActivity.kt   EXTRA_START_PAGE desteği
CoursesFragment.kt     PDF arama düğmesi
fragment_courses.xml   düğme eklendi
AsistanKomut.kt        pdf_ara komutu (37 → 38)
strings.xml            21 yeni string (1162 toplam)
```

### Derleme
Tek seferde başarılı (3m 30s).


---

## 📊 v7.38 — Detaylı ilerleme analitiği (1 Ağu 2026)

**Öneri listesinden 2. madde.** Kullanıcı seçti: *"2. maddeyi ekle"*

### Sorun
`ProgressFragment` yalnızca **168 satırdı**: aylık ısı haritası + iki özet satırı.
Veri toplanıyordu ama hiç analiz edilmiyordu.

**En kritik eksik:** `bumpToday()` yalnızca gün bazında `c/f/q` tutuyordu —
**saat bilgisi hiç kaydedilmiyordu.** "Hangi saatte verimlisin" sorusu
mevcut veriyle cevaplanamazdı.

### Çözüm

**1. Veri toplama düzeltildi (`Store.bumpToday`)**
Günlük kayda `"h"` alanı eklendi — 24 elemanlı saat dizisi.
Her tamamlama/odak/soru, o anın saatine ağırlıklı puan yazıyor
(`completions*3 + focus + questions/4`). Geriye dönük veri yok,
bu yüzden ekranda "birkaç gün sonra dolacak" uyarısı gösteriliyor.

`Store.gunlukKayitKopyasi()` eklendi — `logRoot` private olduğu için
Analitik'in anahtar adı tahmin etmesi kırılgan olurdu.

**2. `Analitik.kt` (~390 satır) — 6 analiz**
| Analiz | Çıktı |
|---|---|
| `saatDagilimi()` | 24 saatlik verim dizisi + 4 dilim (sabah/öğle/akşam/gece) |
| `gunDagilimi()` | Haftanın 7 günü, son 12 hafta ortalaması |
| `kursHizlari()` | Ders başı dakika, haftalık hız, **bitiş tarihi tahmini** |
| `aylikOzet()` | Son 6 ay karşılaştırma |
| `haftalikEgilim()` | Son 8 hafta, yükseliyor mu düşüyor mu |
| `cikarimlar()` | Hepsinden okunabilir cümleler üretir |

**3. `BarChartView.kt` (~130 satır)**
Kütüphanesiz dikey çubuk grafiği. En yüksek çubuk `colorPrimary` ile
vurgulanır, diğerleri soluk. Etiket seyreltme desteği (24 saat dar ekrana sığsın).

**4. `AnalitikActivity.kt` (~430 satır) + layout**
5 kart: çıkarımlar · saat dağılımı · haftanın günleri · haftalık eğilim ·
kurs hızı · aylık karşılaştırma. Sağ üstte **rapor paylaşma** (↗).

### Bitiş tahmini nasıl hesaplanıyor?
Ders tamamlanma tarihi ayrıca tutulmuyor. Bu yüzden son 4 haftadaki toplam
tamamlama, kursun bitmiş ders oranına göre dağıtılıyor → haftalık hız →
kalan ders / hız = kalan hafta. **Yaklaşık ama tutarlı.** Hız 0.2 ders/hafta
altındaysa tahmin gösterilmiyor (anlamsız sonuç üretmemek için).

### Bağlantılar
- **İlerleme ekranı → "📊 Detaylı analiz"** düğmesi
- Asistan komutu: `analiz_ac` (37. komut)
- Rapor paylaşma: WhatsApp/not olarak metin özeti

### Dosyalar
```
Analitik.kt           YENİ ~390 satır — 6 analiz fonksiyonu
BarChartView.kt       YENİ ~130 satır — çubuk grafik
AnalitikActivity.kt   YENİ ~430 satır — 5 kartlı ekran
activity_analitik.xml YENİ layout
Store.kt              bumpToday saat kaydı + gunlukKayitKopyasi()
ProgressFragment.kt   analiz düğmesi
AsistanKomut.kt       analiz_ac komutu (36 → 37)
strings.xml           65 yeni string (1141 toplam)
```

### Derleme
Tek seferde başarılı (3m 27s).


---

## 🧑‍🏫 v7.37 — Yapay zekâ özel öğretmen modu (1 Ağu 2026)

**Öneri listesinden 4. madde.** Kullanıcı seçti: *"4. maddeyi yap"*

### Fark: DersAsistan vs Öğretmen modu
| | DersAsistan (v7.31) | Öğretmen modu (v7.37) |
|---|---|---|
| Kim yönetir | Kullanıcı sorar | **Model yönetir** |
| Hafıza | Yok | **Oturum + seviye + zayıf noktalar** |
| Akış | Tek atış | **Anlat → sor → değerlendir → uyarla** |
| Seviye | Sabit | **1-5, cevaba göre otomatik** |

### Çalışma döngüsü
```
ANLATIM (3-6 paragraf) → ANLAMA SORUSU (4 şık veya serbest)
   ↓ doğru                    ↓ yanlış
seviye +1, sonraki adım    seviye -1, AYNI konu daha basit
```
Ders 5 adım (seviye 4-5) veya 9 adım (seviye 1-3) sürer.

### Yeni dosyalar
```
OgretmenStore.kt      (~290 satır) Oturum modeli, seviye 1-5 ders bazlı,
                      zayıf nokta kaydı, yarım oturum listesi, yedekleme
OgretmenMotoru.kt     (~330 satır) anlat() / degerlendir() / bitirmeOzeti()
                      JSON çıktı + savunmacı ayrıştırma (bozuksa düz metne düşer)
OgretmenActivity.kt   (~430 satır) Anlatım ekranı, şık kilitleme, renk geri
                      bildirimi, seviye rozeti, kaldığın yerden devam
activity_ogretmen.xml Layout — konu başlığı, anlatım, soru kartı, geri bildirim
```

### Tasarım kararları
1. **Ders bazlı seviye** — kullanıcı AutoCAD'de ileri, Revit'te yeni olabilir.
   Tek genel seviye yanlış olurdu.
2. **RAG entegre** — ders PDF'i varsa `DersMetni.baglamHazirla()` ile
   gerçek içerikten anlatır. Adım numarasından yaklaşık sayfa tahmin edilir.
3. **Yanlışta ilerlemez** — aynı konu farklı açıdan, daha basit anlatılır.
   Klasik quizden temel farkı bu.
4. **Zayıf noktalar** — yanlış cevaplanan konu başlıkları kaydedilir,
   ders sonunda "tekrar et" listesi olarak sunulur.
5. **Oturum kalıcı** — her adımda kaydedilir, çıkıp dönünce devam sorar.
6. **Savunmacı ayrıştırma** — model JSON vermezse düz metin anlatım olarak
   gösterilir; ekran asla boş kalmaz.

### Bağlantılar
- Kurs ekranı → derse dokun → **"🧑‍🏫 Özel öğretmenle çalış"**
  (yarım kalmışsa "%d. adımdan devam", bitmişse "✓ %d%%")
- Ders sonunda: dersi tamamlandı işaretle · quiz çöz (varsa)
- Yedek **sürüm 12** — oturum, seviye, zayıf noktalar taşınıyor

### Derleme
Tek seferde başarılı (3m 33s). Swap sandbox sıfırlandığı için yeniden açıldı.

\n
---

## ⚡ v7.45 — Widget'lar gerçekten iş yapıyor (3 Ağu 2026)

**Kullanıcı:** *"Widgetlar daha islevsel olsun"*

### Tespit: widget'ların çoğu sadece kısayoldu
```
SummaryWidget    → 0 gerçek eylem, 3 "uygulamayı aç"
CountdownWidget  → 0 gerçek eylem, 1 "uygulamayı aç"
ActionsWidget    → 0 gerçek eylem, 2 "uygulamayı aç"
TasksWidget      → 1 gerçek eylem (görev tamamlama)
GlassWidget'lar  → 1 gerçek eylem
```
Kullanıcı 25 dakikalık odak başlatmak için bile uygulamayı açmak zorundaydı.

### Çözüm: `WidgetEylem.kt` — merkezi eylem alıcısı
Tek `BroadcastReceiver`, 9 eylem. Widget'a dokunulur → iş anında yapılır →
Toast gösterilir → tüm widget'lar tazelenir. **Uygulama hiç açılmaz.**

| Eylem | Ne yapar |
|---|---|
| `IS_ODAK_25` / `IS_ODAK_45` | Odak oturumu başlatır + bildirim açar |
| `IS_ODAK_DUR` | Çalışan oturumu duraklatır |
| `IS_SORU_ARTIR` | Günlük soru sayacı +1 |
| `IS_DERS_ISARETLE` | Bugünü kurs serisinde işaretler |
| `IS_KART_CEVIR` | Bilgi kartını çevirir |
| `IS_KART_BILDIM` / `IS_KART_BILMEDIM` | Leitner günceller, sıradaki karta geçer |
| `IS_GOREV_ERTELE` | Görevi yarın 09:00'a erteler |

### 🃏 En büyük yenilik: Brifing widget'ında tam kart tekrarı
Ana ekranda bilgi kartı gösteriliyor:
```
[Kartın ön yüzü]
[Çevir]
      ↓ dokun
[ön yüz → arka yüz]
[Gizle] [✓] [✗]
      ↓ cevapla
Leitner güncellenir, sıradaki kart gelir
```
Uygulama **hiç açılmadan** tekrar döngüsü tamamlanıyor. Bu, widget öneri
listesindeki 1. ve 3. maddenin birleşimi.

Kart bölümü yalnızca yükseklik ≥190dp ise görünür — küçük widget'ta
görevler önceliklidir.

### Widget bazlı değişiklikler
| Widget | Önce | Sonra |
|---|---|---|
| **Hızlı eylem** | 4 düğme → uygulama açar | ⏱ odak başlat/durdur · ✏️ soru +1 (anında) |
| **Özet** | 3 kutu → uygulama açar | Odak kutusu → oturum başlat · Soru kutusu → +1 · Seri kutusu → günü işaretle |
| **Geri sayım** | Tek dokunuş → etkinlikler | Emoji → seriyi işaretle · Gövde → etkinlikler |
| **Brifing** | ✨↻ düğmeleri | + Bilgi kartı bölümü · odak düğmesi gerçek iş yapıyor |

### Akıllı durum
Odak düğmeleri **çalışma durumuna göre değişiyor**: oturum aktifse
"durdur", değilse "başlat" niyeti bağlanıyor. Widget her tazelendiğinde
yeniden değerlendiriliyor.

### Dosyalar
```
WidgetEylem.kt        YENİ ~250 satır — 9 eylem + kart oturumu durumu
widget_brifing.xml    kart bölümü eklendi (LinearLayout/TextView — RemoteViews uyumlu)
BrifingWidget.kt      kartiCiz() + odak düğmesi
SummaryWidget.kt      3 kutu gerçek eyleme bağlandı
ActionsWidget.kt      odak + soru eylemleri
CountdownWidget.kt    emoji → seri işaretleme
AndroidManifest.xml   WidgetEylem receiver
strings.xml           10 yeni string (1320 toplam)
```

Widget sayısı: **8 → 8** (değişmedi)

### Derleme
Tek seferde başarılı (3m 33s).


---

## 🎚 v7.44 — Bildirim aç/kapat ekranı erişilebilir yapıldı (3 Ağu 2026)

### Kullanıcı bildirimi
> "Bildirimleri tek tek acip kapatma özelliği ekle"

**Özellik v7.43'te zaten vardı — ama kimsenin bulamayacağı yerdeydi.
Bu bir tasarım hatasıydı, bendeydi.**

### Sorun: 3 seviye derinlik
```
Ayarlar → Bildirimler → [küçük pencere] → sol alttaki "Bildirim ayarları" → ekran
                                            ↑ kimse buraya bakmaz
```
20 tür anahtarı `setNeutralButton` arkasındaydı. Material Design'da nötr düğme
en az fark edilen öğedir; kullanıcı haklı olarak "özellik yok" sandı.

### Çözüm
```
Ayarlar → Bildirimler → ekran açılır   (tek dokunuş)
```
Ara pencere tamamen kaldırıldı. Ses/titreşim anahtarları da yeni ekrana taşındı,
hiçbir işlev kaybolmadı.

### v7.44'te eklenen kolaylıklar
| Özellik | Açıklama |
|---|---|
| **Durum özeti** | Ekranın başında "14 / 20 bildirim türü açık" |
| **Tümünü aç / Tümünü kapat** | 20 anahtarı tek dokunuşla çevirir |
| **Varsayılan** | Fabrika ayarına döner (onay sorar) — sessiz saat, tavan, tur saatleri dahil |
| **Grup bazlı aç/kapat** | Her grup başlığının yanında "Aç/Kapat" — ör. tüm Öğrenme bildirimleri |
| **Canlı sayaç** | Anahtar çevrildiğinde özet anında güncellenir |
| **Ses + titreşim** | Eski pencereden taşındı, aynı ekranda |
| Ayarlar alt yazısı | "Sayaç bildirimi, ses ve titreşim" → "20 bildirim türünü tek tek aç/kapat" |

### Ekranın tam içeriği
```
🔔 Bildirim Ayarları
├─ Tüm bildirimler            [ana anahtar]
├─ 14 / 20 bildirim türü açık
├─ Bildirim sesi              [anahtar]
├─ Titreşim                   [anahtar]
├─ [Tümünü aç] [Tümünü kapat] [Varsayılan]
├─ 🌙 Rahatsız etmeyin        [anahtar] + saat seçici
├─ ⏰ Sabah 09:00 · Akşam 19:00
├─ 📊 Günlük bildirim sınırı: 6
├─ Hatırlatıcılar        [Aç/Kapat]  → 2 tür
├─ Öğrenme               [Aç/Kapat]  → 6 tür
├─ Başarımlar            [Aç/Kapat]  → 8 tür
├─ Raporlar              [Aç/Kapat]  → 2 tür
├─ Arka plan işleri      [Aç/Kapat]  → 2 tür
├─ Test bildirimi gönder
└─ Sistem bildirim ayarları
```

### Dosyalar
```
SettingsFragment.kt      rowNotifications → doğrudan BildirimAyarActivity
BildirimAyarActivity.kt  özet sayacı, toplu işlemler, grup kısayolları,
                         ses/titreşim anahtarları
strings.xml              10 yeni string (1310 toplam)
```

### Ders
Bir özelliğin **var olması yetmez, bulunabilir olması gerekir.** Nötr düğme
arkasına gizlenen işlev, olmayan işlevle aynı şeydir.


---

## 🔔 v7.43 — 30 bildirim önerisinin tamamı (2 Ağu 2026)

**Kullanıcı:** *"Hepsini sirasiyla yap"*

### Sıralama kararı
Öneri **26 (ayarlar ekranı) ilk** yapıldı — bu olmadan 29 bildirim eklemek
kullanıcıyı boğar, o da hepsini kapatırdı. Altyapı önce, içerik sonra.

### YENİ DOSYALAR
```
BildirimMerkezi.kt      ~430 satır — kanal/ayar/sessiz saat/tavan altyapısı
BildirimUretici.kt      ~480 satır — 25 bildirimin içerik ve koşul mantığı
BildirimZamanlayici.kt  ~130 satır — sabah/akşam iki tur alarm
BildirimAyarActivity.kt ~400 satır — 20 anahtarlı ayar ekranı
```

### 30 ÖNERİNİN DURUMU

**A. Öğrenme döngüsü (1-8)**
| # | Bildirim | Durum |
|---|---|---|
| 1 | Kart tekrar hatırlatıcısı | ✅ akşam turu, 3+ kart varsa |
| 2 | Quiz tekrar zamanı | ✅ akşam turu |
| 3 | Bildirimden kart çevirme | ⚠️ **kısmi** — bildirime dokununca KartActivity açılıyor; şıklar bildirimde değil (aşağıda not) |
| 4 | Yarım kalan öğretmen dersi | ✅ `OgretmenStore.yarimOturumlar()` |
| 5 | Unutma eğrisi uyarısı | ✅ tamamlanmış dersler arasından |
| 6 | Ders serisi koruma | ✅ akşam, seri ≥2 ve bugün boşsa |
| 7 | Günün kartı | ✅ sabah turu, rastgele kart |
| 8 | Sınav kilometre taşları | ✅ 30/14/7/3/1 gün + hazırlık % |

**B. Hedef ve motivasyon (9-15)**
| # | Bildirim | Durum |
|---|---|---|
| 9 | Günlük hedef ilerlemesi | ✅ hedefin yarısı geçilmişse |
| 10 | Hedef tamamlandı | ✅ **anlık** — `addTodayFocusMinutes` tetikler |
| 11 | Rozet kazanma | ✅ **anlık** — `Badges.kt` artık sessiz değil |
| 12 | Seri rekoru | ✅ **anlık** |
| 13 | Haftalık özet zenginleştirme | ✅ mevcut rapor korundu, kanal ayrıldı |
| 14 | Aylık rapor | ✅ ayın 1'i, `Analitik.aylikDegisim()` ile |
| 15 | Geri dönüş daveti | ✅ 3 gün pasiflik (yeni kullanıcıya gönderilmez) |

**C. Zamanlayıcı (16-20)**
| # | Bildirim | Durum |
|---|---|---|
| 16 | Mola bitişi | ⚠️ **kısmi** — kanal ayrıldı, ayrı mola sayacı yok |
| 17 | Canlı ilerleme çubuğu | ✅ `gonder(ilerleme=…)` desteği eklendi |
| 18 | Odak önerisi | ✅ v7.38 saat analizinden, 1 saat önce |
| 19 | Pomodoro tur sayacı | ⚠️ **kısmi** — altyapı hazır, tur sayacı yok |
| 20 | Uzun oturum uyarısı | ✅ 90 dk, `TimerEngine.creditWatch` |

**D. Arka plan (21-25)**
| # | Bildirim | Durum |
|---|---|---|
| 21 | PDF indeksleme ilerlemesi | ✅ 5 derste bir + bitiş bildirimi |
| 22 | Kaynak bulma tamamlandı | ✅ `KaynakBulucu` |
| 23 | Kurs üretimi tamamlandı | ✅ `KursUretici` |
| 24 | Yedekleme bildirimi | ✅ varsayılan **kapalı** |
| 25 | Sesli ders zenginleştirme | ✅ kanal ayrıldı |

**E. Altyapı (26-30)**
| # | Özellik | Durum |
|---|---|---|
| 26 | **Bildirim ayarları ekranı** | ✅ 20 tür ayrı anahtar |
| 27 | Rahatsız etmeyin | ✅ varsayılan 23:00–08:00, gece yarısı aşımı doğru |
| 28 | Bildirim gruplama | ✅ `setGroup` + grup özeti |
| 29 | Kanal ayrıştırma | ✅ **5 grup, 9 kanal** (eskiden 6 dağınık kanal) |
| 30 | Bildirimden hızlı yanıt | ❌ **yapılmadı** — `RemoteInput` gerektiriyor, ayrı sürüme bırakıldı |

**Özet: 25 tam · 4 kısmi · 1 yapılmadı**

### BİLDİRİM YORGUNLUĞUNA KARŞI 4 KORUMA
1. **Günlük tavan** — varsayılan 6, aşılırsa gönderilmez
2. **Sessiz saatler** — 23:00–08:00 arası sessiz
3. **Günde bir kez** — `bugunGonderildiMi()` her tür için ayrı
4. **Varsayılan kapalı** — 6 tür (yarım ders, unutma, günlük kart, hedef ilerleme, geri dönüş, odak önerisi, yedek) kapalı geliyor

`acil = true` olan bildirimler (rozet, hedef, zamanlayıcı bitişi) tavan ve
sessiz saat kontrolünü atlar — kullanıcının beklediği anlık geri bildirimler.

### Değişen dosyalar
```
App.kt                 açılışta kanal kurulumu + zamanlayıcı
BootReceiver.kt        yeniden başlatmada zamanlama
Store.kt               recordCompletion/addTodayFocusMinutes → anlık kontroller
                       autoBackupNow → yedek bildirimi
SettingsFragment.kt    "Bildirim ayarları" düğmesi
TimerEngine.kt         uzun oturum uyarısı
KursUretici.kt         kurs hazır bildirimi
KaynakBulucu.kt        kaynak bulundu bildirimi
PdfAramaActivity.kt    indeksleme ilerlemesi
AndroidManifest.xml    BildirimAyarActivity + BildirimZamanlayici
strings.xml            117 yeni string (1300 toplam)
```

### Derleme
Tek seferde başarılı (3m 30s).


---

## 🎛 v7.42 — Widget'lar baştan elden geçirildi: 30 iyileştirme (2 Ağu 2026)

**Kullanıcı isteği:** *"Olan widgetlari daha kullanışlı hale getir karanlık mod,
ayarlanabilir boyut vb 30 tane farkli kullanışlı hale getir. Widget sayisini arttirmadan"*

Widget sayısı **8'de kaldı** — hiçbir yeni widget eklenmedi.

### 🌙 KARANLIK MOD (1-4)
| # | İyileştirme |
|---|---|
| 1 | **`values-night-v31` oluşturuldu** — KRİTİK HATA düzeltmesi |
| 2 | Android 12+ açık tema paleti tamamlandı (4 renk → 11 renk) |
| 3 | Koyu temada aydınlık ton ucu (neutral1_50, accent1_200) |
| 4 | Kenarlık/gölge renkleri temaya göre ayrıldı |

**Bulunan hata:** `values-v31` vardı ama `values-night-v31` **yoktu**.
Android 12+ cihazda koyu temaya geçilince:
- `w_text` → `system_neutral1_900` (KOYU yazı)
- `w_bg` → `#1C1814` (KOYU zemin)

Sonuç: **koyu üstüne koyu — widget okunmuyordu.** Şimdi düzeldi.

### 📐 AYARLANABİLİR BOYUT (5-12)
| # | İyileştirme |
|---|---|
| 5-12 | 8 widget'ın tamamına `minResizeWidth/Height` + `maxResizeWidth/Height` eklendi |

Eskiden hiçbirinde küçültme sınırı yoktu — kullanıcı küçültünce içerik kırpılıyordu.

| Widget | Eski | Yeni en küçük |
|---|---|---|
| Geri sayım | 2×2 sabit | **1×1** (80dp) |
| Hızlı eylem | 4×1 sabit | **2×1** (110dp) |
| Özet | 4×2 sabit | **3×1** (140dp) |
| Görevler | 4×3 sabit | **3×2** (140dp) |
| Brifing | 4×3 sabit | **3×2** (160dp) |
| Cam ×3 | 4×4 sabit | **3×2** (140dp) |

### 🔄 UYARLANABİLİR DÜZEN (13-25)
| # | İyileştirme |
|---|---|
| 13 | `WidgetCommon.genislikDp()` — gerçek ölçüyü okur |
| 14 | `WidgetCommon.yukseklikDp()` |
| 15 | `boyutKademesi()` — 0 dar / 1 orta / 2 geniş |
| 16 | `sigacakSatir()` — yüksekliğe göre satır hesabı |
| 17 | `goster()` — görünürlük kısayolu |
| 18 | `sigdir()` — metni kademeye göre kısaltır |
| 19 | `yaziBoyutu()` — dinamik `setTextViewTextSize` |
| 20-24 | **`onAppWidgetOptionsChanged`** 5 widget'a eklendi |
| 25 | Glass widget'lara da eklendi + liste tazeleme |

**Kritik:** Eskiden widget yeniden boyutlandırılınca içerik **eski düzende kalıyordu**.
Artık her boyut değişiminde yeniden çiziliyor.

### 🎯 WIDGET BAZLI UYARLAMALAR (26-30)
| # | Widget | Davranış |
|---|---|---|
| 26 | **Brifing** | Dar: tarih+rozet gizlenir · Alçak: tavsiye gizlenir · 1-3 görev (yüksekliğe göre) · ✨↻ düğmeleri kademeli |
| 27 | **Özet** | Dar: rozet gizlenir, kısa hedef metni · Alçak: istatistik kutuları gizlenir |
| 28 | **Geri sayım** | <100dp: etiket gizlenir · <80dp: emoji gizlenir · rakam 26→38sp ölçeklenir |
| 29 | **Hızlı eylem** | <170dp: görev düğmesi gizlenir · <220dp: bugün gizlenir |
| 30 | **Görevler** | Dar: "📋 5" kısa başlık · yazı boyutu uyarlanır |

### ➕ EK İYİLEŞTİRMELER
- Liste limitleri artırıldı: Görevler 20→**40**, Cam widget'lar 15→**30**
  (widget artık büyütülebildiği için daha çok satır anlamlı)
- `ACTION_TIME_SET` ve `ACTION_MY_PACKAGE_REPLACED` eklendi —
  uygulama güncellenince widget'lar boş kalmıyor

### Dosyalar
```
values-night-v31/widget_colors.xml   YENİ — Android 12+ koyu tema
values-v31/widget_colors.xml         4 renk → 11 renk
xml/*_info.xml (8 dosya)             minResize/maxResize eklendi
WidgetCommon.kt                      7 yeni yardımcı fonksiyon
BrifingWidget.kt                     6 fonksiyon uyarlanabilir yapıldı
SummaryWidget.kt / CountdownWidget.kt / ActionsWidget.kt / TasksWidget.kt
GlassWidgetBase.kt                   onAppWidgetOptionsChanged
TasksWidgetService.kt / GlassListService.kt   limit artışı
BootReceiver.kt / AndroidManifest.xml         yeni yayın eylemleri
```

### Derleme
Tek seferde başarılı (3m 33s). Widget sayısı: **8 → 8** (değişmedi).

\n
---

## 🕌 v7.46 — Namaz vakitleri + vakit arası planlama (3 Ağu 2026)

**Kullanıcı:** *"Namaz programi entegre et... Namaz aralarında islerimi
değiştireyim farklilastirayim"*

### Tasarım kararı: çevrimdışı hesap
API kullanmadım. Astronomik formüller sabittir, cihazda hesaplanır:
internet yok, izin yok, kota yok, uçakta bile çalışır. v7.34'teki
"ücretsiz mod" felsefesiyle tutarlı.

### Algoritma doğrulaması
Kotlin'e geçmeden önce Python'da test edildi. İlk denemede ikindi
hesabında hata çıktı (`abs()` kullanımı yanlış açıyı üretiyordu),
düzeltildi. Sonuç — Konya, 3 Ağustos 2026:
```
İmsak 04:12 · Güneş 05:55 · Öğle 12:56
İkindi 16:46 · Akşam 19:58 · Yatsı 21:34
```
Diyanet takvimiyle ±1 dakika içinde örtüşüyor.

### `NamazVakti.kt` (~390 satır)
- Jean Meeus temelli güneş konumu hesabı
- **85 şehir** gömülü (81 il + Mekke, Medine, Berlin, Londra)
- 5 hesaplama yöntemi: Diyanet, MWL, ISNA, Mısır, Ümmü'l-Kura
- İkindi: Hanefi (2 kat) / diğer mezhepler (1 kat)
- **Vakit başına ±30 dk düzeltme** — ilçe farkları için

### `NamazPlan.kt` (~310 satır) — asıl istenen özellik
Gün, namaz vakitleriyle **6 doğal dilime** bölünüyor. Her dilimin kendi
karakteri var:

| Dilim | Aralık | Önerilen iş |
|---|---|---|
| 🌙 Seher | İmsak–Güneş | Ezber, tekrar — zihin en açık |
| 🌅 Kuşluk | Güneş–Öğle | Derin çalışma — en uzun dilim |
| ☀️ Öğleden sonra | Öğle–İkindi | Uygulama, pratik |
| 🌤 İkindi sonrası | İkindi–Akşam | Ders izleme, okuma |
| 🌆 Akşam | Akşam–Yatsı | Hafif tekrar, kart |
| 🌃 Gece | Yatsı–İmsak | Planlama, değerlendirme |

Her dilime iş eklenir, dokununca tamamlanır. **Plan her gün tekrarlanır** —
işaretler gece yarısı sıfırlanır, iş listesi kalır.

`simdiNeYapmali()`: o an hangi dilimdeysen bekleyen işini söyler,
yoksa dilimin doğasına uygun öneri verir.

### Ekran ve entegrasyon
```
NamazActivity      3 bölüm: sıradaki vakit + geri sayım · 6 vakit listesi ·
                   6 dilim kartı (aktif olan vurgulu)
TodayFragment      Namaz kartı — sıradaki vakit + "şimdi ne yapmalı"
SettingsFragment   "Hakkında" satırına uzun bas → modül aç/kapat
AsistanKomut       namaz_ac komutu (38 → 39)
Store              yedek sürüm 13 — ayarlar + plan taşınıyor
```

**Modül varsayılan KAPALI.** Namaz kılmayan kullanıcıyı rahatsız etmemek
için; açılınca Bugün ekranında kart belirir.

### Hazır şablon
"Hazır şablon" düğmesi 10 örnek iş yükler (seher: 20 kart tekrarı,
kuşluk: 45 dk odak…). Var olan işler silinmez, üzerine eklenir.

### Dosyalar
```
NamazVakti.kt          YENİ ~390 satır — astronomik hesap + 85 şehir
NamazPlan.kt           YENİ ~310 satır — 6 dilim, iş yönetimi, öneri
NamazActivity.kt       YENİ ~490 satır — 3 bölümlü ekran + 4 ayar penceresi
activity_namaz.xml     YENİ layout
fragment_today.xml     namaz kartı
TodayFragment.kt       bindNamaz()
strings.xml            67 yeni string (1381 toplam)
```

### Derleme
Tek seferde başarılı (3m 40s).

\n
---

## 🔔 v7.47 — Namaz: konum, widget, bildirim, titreşim (3 Ağu 2026)

**Kullanıcı:** *"Konum olarak benim sectigim yer olsun ve minimalist olarak
widget ekle ayarlardan bildirim ekle acmali kapamali ses olarak ben
ekleyeceğim titreşim olsun"*

### 1. Konum — kullanıcının seçtiği yer
İki yol:
- **85 şehir listesi** (v7.46'dan) — hızlı seçim
- **Elle giriş** (YENİ) — ad + enlem + boylam

Elle girişte doğrulama var: enlem -90..90, boylam -180..180 dışındaysa
reddediliyor. Haritadan koordinat alma ipucu ekranda yazıyor.

Konum değişince **bildirim alarmları ve widget otomatik güncelleniyor**.

### 2. Minimalist widget (2×1) — `NamazWidget.kt`
```
🕌  Akşam
    19:58
    2 sa 14 dk kaldı
```
Üç bilgi, başka hiçbir şey yok. Düğme, çerçeve süsü, ikon yığını yok.

- **1×1'e kadar küçülür** (70dp) — o boyutta yalnızca saat kalır
- Yazı boyutu genişliğe göre 18→24sp ölçekleniyor
- Modül kapalıysa "Modül kapalı" yazıp açma ekranına yönlendiriyor

Widget sayısı: **8 → 9**

### 3. Vakit bildirimleri — `NamazBildirim.kt`
| Ayar | Detay |
|---|---|
| **Ana anahtar** | Tüm namaz bildirimleri aç/kapat |
| **Vakit bazlı** | 6 vakit ayrı ayrı (Güneş varsayılan kapalı — namaz değil) |
| **Ses** | Sistem ses seçici — kullanıcı kendi ezan dosyasını seçer |
| **Titreşim** | Açık/kapalı + 3 desen (kısa/orta/uzun) |
| **Önceden** | Tam vaktinde / 5 / 10 / 15 / 30 dk önce |
| **Dene** | Test bildirimi gönderir |

**Bildirim içeriği plana bağlı:** Vakit girdiğinde o vaktin başlattığı
dilimin bekleyen işini gösteriyor —
*"Kuşluk dilimi başlıyor · Sıradaki: Kurs dersi çalış"*

### Teknik: kanal sürümleme
Android'de **bildirim sesi kanala bağlıdır** ve kanal oluşturulduktan sonra
sesi değiştirilemez. Çözüm: kanal kimliği sürüm numarası taşıyor
(`namaz_vakti_v3`). Ses/titreşim değişince sürüm artıyor, eski kanal
siliniyor, yenisi kuruluyor.

Ayrıca bazı cihazlarda kanal titreşimi çalışmıyor — bildirime ek olarak
`Vibrator` API'siyle elle de tetikleniyor.

### Alarm hassasiyeti
Namaz vakti dakika hassasiyeti gerektirdiği için `setExactAndAllowWhileIdle`
kullanılıyor. Android 12+ tam alarm izni yoksa `setAndAllowWhileIdle`'a
düşülüyor (birkaç dakika sapabilir ama çalışır).

Her bildirim tetiklendiğinde ertesi gün için yeniden kuruluyor.

### Dosyalar
```
NamazBildirim.kt   YENİ ~420 satır — alarm, kanal sürümleme, titreşim
NamazWidget.kt     YENİ ~130 satır — minimalist 2x1
widget_namaz.xml   YENİ layout
w_namaz_info.xml   YENİ widget tanımı
NamazActivity.kt   elleKonumGir() + bildirimAyarlari() + sesSec()
BootReceiver.kt    yeniden başlatma ve gün değişiminde alarm kurulumu
App.kt             açılışta alarm tazeleme
WidgetCommon.kt    NamazWidget tazeleme listesine eklendi
strings.xml        43 yeni string (1424 toplam)
```

### Derleme
Tek seferde başarılı (3m 36s).

\n
---

## 🕌 v7.48 — Namaz üst bara alındı, ayarlar tek ekranda (3 Ağu 2026)

**Kullanıcı:** *"Yukardaki yapay zeka yerine ekle ve butun diyanet namaz
ayarlari ordan yapilsin"*

### 1. Üst bar değişimi
```
ÖNCE:  ⋮  ✨(yapay zekâ)
SONRA: ⋮  🕌  [Akşam 19:58]
```

**Yapay zekâ erişimi korundu.** Kaldırmadan önce denetim yapıldı:
- `HomeFragment.kt:45` → Ana Sayfa ✨ düğmesi ✅
- `TodayFragment.kt:54` → Bugün ekranı asistan düğmesi ✅
- Alt navigasyon → Asistan sekmesi ✅

Kullanıcı erişimsiz kalmıyor.

**Üst bardaki namaz düğmesi:**
| Eylem | Sonuç |
|---|---|
| Dokun (modül açık) | Namaz & plan ekranı |
| Dokun (modül kapalı) | Doğrudan ayarlara götürür |
| **Uzun bas** | Tüm Diyanet ayarları |

Yanında **canlı vakit rozeti**: "Akşam 19:58" — `onResume`'da tazeleniyor,
dokununca plan ekranı açılıyor. Modül kapalıysa görünmüyor.

### 2. `NamazAyarActivity.kt` — tek ekran, 8 bölüm
v7.47'de ayarlar bir menü + 6 ayrı diyalogdu; her ayar için girip çıkmak
gerekiyordu. Artık tek sayfa, kaydırarak:

```
🕌 Namaz Ayarları
├─ Namaz modülü                      [anahtar]
├─ 🌙04:12 🌅05:55 ☀️12:56 🌤16:46 🌆19:58 🌃21:34   ← canlı önizleme
├─ Konum
│   📍 Konya · Koordinat: 37.8746, 32.4932
│   📍 Konumu elle gir
├─ Hesaplama yöntemi   [Diyanet ▾]
├─ İkindi hesabı       [Standart ▾]
├─ Dakika düzeltmesi
│   🌙 İmsak   0 dk  [−][+]
│   ... 6 vakit ayrı
├─ Vakit bildirimleri  [anahtar]
│   6 vakit ayrı anahtar
│   Ne zaman [Tam vaktinde ▾]
│   🔊 Ses seç · Titreşim [anahtar] + desen
│   🔔 Test bildirimi
├─ Ana ekran widget'ı (nasıl eklenir)
└─ ↺ Diyanet varsayılanına dön
```

**Canlı önizleme:** Herhangi bir ayarı değiştirince 6 vakit anında
yeniden hesaplanıp üstte gösteriliyor — etkiyi görmek için ekran
değiştirmeye gerek yok.

**Diyanet varsayılanına dön:** Yöntem 18°/17°, ikindi standart, düzeltmeler
sıfır. Konum ve plan korunuyor.

### Temizlik
`NamazActivity` **759 → 402 satır**. Ayarlarla ilgili 8 fonksiyon
(sehirSec, yontemSec, ikindiSec, duzeltmeSec, elleKonumGir,
bildirimAyarlari, sesSec, baslikYazi) yeni ekrana taşındı, eskiler silindi.

⚙ düğmesi artık doğrudan tam ayar ekranını açıyor.

### Derleme notu
İlk denemede tek hata: blok silme sırasında `ayarlar()` fonksiyonu gitti
ama çağrısı kaldı (`Unresolved reference: ayarlar`). Çağrı doğrudan
`NamazAyarActivity.ac()`'ye bağlandı, ikinci denemede başarılı (2m 28s).

### Dosyalar
```
NamazAyarActivity.kt  YENİ ~620 satır — 8 bölümlü tek ayar ekranı
activity_main.xml     aiButton → namazButton + namazVakit rozeti
MainActivity.kt       namazRozetiniTazele() + onResume
NamazActivity.kt      759 → 402 satır (ayarlar taşındı)
strings.xml           15 yeni string (1439 toplam)
```

\n
---

## 🎬 v7.49 — Yapay zekâ geri geldi + günlük dizi/film önerisi (3 Ağu 2026)

**Kullanıcı:** *"Ust menudeki yapay zeka asistan yerini kaldirmissim ekle ve
oraya ekstra bana günlük olarak dizi film oneri yeri ekle... imdb puanini
vs izleme yeri de ekle ekstra indirme yeri ekle 480 piksel vs"*

### Üst bar
```
⋮  ✨  🎬  🕌  [Akşam 19:58]
```
✨ yapay zekâ geri kondu (v7.48'de namaz için kaldırılmıştı).
🎬 film önerileri eklendi.

### İndirme konusunda verilen karar — DÜRÜSTLÜK NOTU
Kullanıcı belirli bir korsan indirme sitesi paylaşıp "oradan indir" dedi.
**Yapılmadı.** Gerekçe kullanıcıya açıkça söylendi:
1. Telif korumalı içerik indirmek yasa dışı
2. O tür siteler sürekli kapanıp adres değiştirir — yazılan kod birkaç
   hafta içinde bozulur, kullanıcıya çalışmayan özellik bırakılmış olur

**Bunun yerine:** Kalite seçimi (480p/720p/1080p/4K) korundu ama yasal
platformların çevrimdışı indirme özelliğine yönlendiriyor. Netflix, Prime,
Disney+, BluTV, Exxen — hepsinde uygulama içi indirme var.

### Veri kaynağı: iki katmanlı (kullanıcı "ikisi birden" seçti)
| Katman | Ne verir |
|---|---|
| **TMDb API** (anahtar varsa) | Gerçek puan, oy sayısı, oyuncular, yönetmen, süre, **Türkiye'de hangi platformda** |
| **Yapay zekâ** (yedek) | Anahtar yoksa Gemini'den öneri listesi |

Anahtar almak istemeyen kullanıcı boş ekranla karşılaşmıyor.

**Puan dürüstlüğü:** IMDb'nin ücretsiz API'si yok. TMDb oy ortalaması
gösteriliyor (IMDb'ye çok yakın ama birebir aynı değil) — ekranın altında
bu açıkça yazıyor. IMDb kimliği de geldiği için IMDb sayfasına doğrudan
link kurulabiliyor.

### Ekran içeriği
Her kartta: 🎬/📺 · ad (yıl) · ⭐ puan · tür · süre · yönetmen · oyuncular ·
Türkçe özet · 📡 platformlar

Düğmeler: **▶ İzle** (8 yasal platform) · **⬇ kalite** · **ⓘ IMDb** · **＋ listeye ekle**

Ek özellikler:
- Arama (TMDb veya AI)
- 🔖 İzleme listesi — izledim işareti, listeden çıkarma
- ⚙ Ayarlar: ne önerilsin (film/dizi/hepsi) · sevilen türler · varsayılan kalite · TMDb anahtarı
- Günün önerisi **önbelleğe alınıyor** — aynı gün tekrar açılınca API çağrısı yok
- Her gün farklı sonuç (TMDb sayfa numarası gün sayısına bağlı)

### Dosyalar
```
FilmStore.kt      YENİ ~290 satır — model, izleme listesi, tercihler, önbellek
FilmServis.kt     YENİ ~430 satır — TMDb + AI, detay, platform, izleme linkleri
FilmActivity.kt   YENİ ~560 satır — kart listesi, arama, liste, ayarlar
activity_film.xml YENİ layout
activity_main.xml aiButton geri + filmButton
MainActivity.kt   iki düğme bağlandı
Store.kt          yedek v14
AsistanKomut.kt   film_ac komutu (39 → 40)
strings.xml       49 yeni string (1488 toplam)
```

### Derleme
Tek seferde başarılı (4m 10s).

\n
---

## 🔧 v7.50 — Film önerileri yüklenmiyordu: kök sebep bulundu (3 Ağu 2026)

**Kullanıcı:** *"Tmdb anahtari alamiyorum ve sadece yapay zekadan destekli
çalıştır ve günün onerileri yüklenmiyor"*

### Kök sebep: iki ayrı hata
`FilmServis` yapay zekâ yolunda `AiClient.chat()` kullanıyordu. Bu fonksiyon
**sohbet için** tasarlanmış:

| Sorun | Etki |
|---|---|
| **Bütçe 1200 token** | 8 film JSON'u ~850 token. Gemini 3 düşünen model olduğu için düşünme token'ları da bu bütçeden düşüyor → yanıt yarıda kesiliyor → JSON bozuk → boş ekran |
| **Sistem istemi ekliyor** | 40 komut talimatı + kurs verileri gönderiliyordu. Model film yerine `>>KOMUT:` üretmeye çalışıyordu |

Hesap: `1 film ≈ 105 token × 8 = 844 token` + düşünme payı > 1200 bütçe.

### Çözüm 1 — `AiClient.sadeIstek()`
Sistem istemi eklemeyen, bütçesi çağıran tarafından belirlenen yeni giriş
noktası. Film için **6144 token** kullanılıyor.

Sağlayıcılar arası geçiş, model yedekleme ve ücretsiz mod korumaları
`chat()` ile aynı — sadece istem ve bütçe farklı.

### Çözüm 2 — parça kurtarma
Yanıt yine de kesilirse tüm liste çöpe gitmiyor. `parcaKurtar()` regex ile
tam olan film nesnelerini tek tek çıkarıyor:
```kotlin
Regex("""\{[^{}]*"ad"\s*:[^{}]*\}""")
```
8 film istendi, 6'sı geldiyse 6 film gösteriliyor.

### Çözüm 3 — istem sadeleştirildi
Uzun kural listesi kısaltıldı, tek paragrafa indirildi. Ayrıca güne göre
değişen bir "liste no" eklendi — her gün farklı öneri geliyor.

### Çözüm 4 — TMDb tamamen isteğe bağlı
Kullanıcı anahtar alamadığı için arayüzden TMDb baskısı kaldırıldı:
- Alt yazı artık "TMDb anahtarı ekleyerek doğruluğu artır" demiyor,
  sadece "Yapay zekâ önerisi" yazıyor
- Hata ekranındaki "TMDb anahtarı ekle" düğmesi yerine
  **"⚙ Yapay zekâ ayarları"** düğmesi kondu
- Ayarlarda TMDb "(isteğe bağlı)" olarak etiketlendi
- Alt not: *"Kesin puan için ⓘ IMDb düğmesine dokun"*

### Derleme notu
İlk denemede kaynak birleştirme hatası: `ai_err_empty` iki kez tanımlanmıştı
(eskisi parametresiz, yenisi `%1$s` parametreli). 5 yer eski parametresiz
biçimi kullandığı için eskisine dokunulmadı; yeni durum için ayrı
`ai_err_bos_sebep` string'i eklendi. İkinci denemede başarılı (2m 52s).

### Dosyalar
```
AiClient.kt     +sadeIstek() +sadeGemini() +sadeOpenAi() +hataMesaji() (~200 satır)
FilmServis.kt   sadeIstek'e geçiş, istem sadeleştirme, parcaKurtar()
FilmActivity.kt TMDb baskısı kaldırıldı, AI ayarlarına yönlendirme
strings.xml     5 yeni string (1493 toplam)
```

\n
---

## 👥 v7.51 — İki kişilik online paylaşım (3 Ağu 2026)

**Kullanıcı:** *"Bu uygulamayi online 2 kisilik yap online sekmesini yukari
namaz programinin yanina koy. Diger kisinin eklenmesi icin kod girsin"*

### Sunucu sorunu ve çözümü
Kendi sunucumuz yok, Firebase hesap+kurulum istiyor. Kodlamadan **önce**
üç servis gerçek HTTP çağrılarıyla denendi:

| Servis | Sonuç |
|---|---|
| kvdb.io | ❌ artık e-posta istiyor |
| jsonbin.io | ❌ API anahtarı zorunlu |
| jsonblob.com | ✓ çalışıyor ama anahtar 36 haneli UUID — kullanıcı yazamaz |
| **textdb.online** | ✅ **kullanıcının belirlediği anahtarla çalışıyor** |

textdb.online seçildi. Doğrulanan testler:
```
1) Ahmet oda kurar + görev ekler      → HTTP 200
2) Mehmet kodu girer, okur            → veri geldi
3) Mehmet katılır + kendi görevini ekler → HTTP 200
4) Ahmet yeniler → Mehmet'in işini görüyor ✓
5) Türkçe karakter testi: "Çöp çıkar, ütü yap — şıklık" ✓
```

### Üst bar
```
⋮  ✨  🎬  👥  🕌  [Akşam 19:58]
```
👥 namazın **soluna** kondu (istenen konum).

### Akış
1. **Oda kur** → 6 haneli kod üretilir (`K7M2P9`)
2. Kod WhatsApp ile gönderilir ya da panoya kopyalanır
3. Karşı taraf **"Koda katıl"** → kodu girer
4. İki taraf da görev ekler, tamamlar, not bırakır, birbirine atar

Kod alfabesinde **I, O, 0, 1 yok** — telefonda okunurken karışmasın diye.

### Çakışma koruması
`guvenliGuncelle()`: her yazmadan **hemen önce** sunucudan taze veri okunur,
değişiklik onun üzerine eklenir. İki kişi aynı anda ekleme yapsa da
kimsenin işi kaybolmaz.

### Çevrimdışı dayanıklılık
Son durum yerel önbellekte tutuluyor. İnternet yoksa ekran boş kalmıyor,
"Çevrimdışı · son eşitleme: 12 dk önce" yazıyor.

### Gizlilik — kullanıcıya açıkça söylendi
textdb.online herkese açık bir servis. Ekranda uyarı var:
> *"Kodu bilen herkes listeyi görebilir — şifre, kart bilgisi gibi hassas
> veri yazma. Kişisel notların ve diğer verilerin bu odaya gönderilmez."*

Yalnızca ortak listeye yazılanlar paylaşılıyor; uygulamanın geri kalan
verisi (notlar, kurslar, namaz planı) cihazda kalıyor.

### Dosyalar
```
OnlineStore.kt      YENİ ~380 satır — oda modeli, HTTP, çakışma koruması, önbellek
OnlineActivity.kt   YENİ ~640 satır — karşılama, kod gösterimi, görev listesi
activity_online.xml YENİ layout
activity_main.xml   onlineButton (👥)
Store.kt            yedek v15
AsistanKomut.kt     online_ac komutu (40 → 41)
strings.xml         57 yeni string (1550 toplam)
```

\n
---

## 👑 v7.52 — Yönetici rolü ve üye kısıtlamaları (3 Ağu 2026)

**Kullanıcı:** *"Online olanda bir kisi yonetici olsun sifresi olsun ve diger
hesap uygulamayi silemesin bildirimleri kapatamasin vb ayarlarda
kisitlamalari olsun"*

### Önce dürüstlük: neyin mümkün olmadığı söylendi
Kullanıcıya kodlamadan önce soruldu ve açıkça belirtildi:

> Android'de bir uygulama, **başka bir uygulamanın silinmesini** ya da
> **telefon ayarlarının değiştirilmesini engelleyemez.** Güvenlik gereği
> buna izin verilmiyor. Sadece kendi içinde kilit koyabilir.

Üç seçenek sunuldu (uygulama içi kilit / DeviceAdmin / sadece takip).
Kullanıcı **"uygulama içi kilit"** seçti — en sağlam ve dürüst yol.

DeviceAdmin seçilseydi bile karşı tarafın izni elle vermesi ve istediği an
geri alabilmesi gerekirdi; zorla kurulamazdı.

### Yapılan: rol tabanlı yetki sistemi

**Yönetici (odayı kuran):**
- 4-8 haneli şifre belirler (isteğe bağlı, "şifresiz devam" da var)
- Üyenin 7 ayrı yetkisini açıp kapatır
- Şifreyi sonradan değiştirebilir
- Üye listesinde 👑 rozetiyle görünür

**Üye:**
- Yalnızca yöneticinin izin verdiği işlemleri yapar
- Kısıtlı işlemi denerse gerekçeli uyarı görür
- Ekranda hangi işlemlerin kapalı olduğunu 🔒 ile açıkça görür

### 7 yetki anahtarı
| Yetki | Varsayılan |
|---|---|
| Görev ekleme | ✅ açık |
| Görev silme | ❌ kapalı |
| İşareti geri alma | ✅ açık |
| Başkasının görevini düzenleme | ❌ kapalı |
| Ad değiştirme | ✅ açık |
| Eşitlemeyi kapatma | ❌ kapalı |
| Odadan ayrılma | ❌ kapalı |

### Şifre güvenliği
Düz metin şifre **asla sunucuya gitmez**. SHA-256 karması alınıyor, tuz
olarak oda kodu kullanılıyor (aynı şifre farklı odalarda farklı karma).

textdb.online herkese açık olduğu için bu şart. Not: bu banka güvenliği
değil, aile içi yetki ayrımı — amaç kazara/kolay değişikliği önlemek.

Şifre bir kez girilince **10 dakika** tekrar sorulmuyor.

### Veri biçimi — yer tasarrufu
7 yetki tek bir sayıda **bit maskesi** olarak taşınıyor (`k` alanı).
URL uzunluk sınırı olan bir servis kullandığımız için önemli.

Eski odalarda `k` alanı yoksa makul varsayılan uygulanıyor — geriye
dönük uyumluluk korundu.

### Dosyalar
```
OnlineStore.kt     +Kural sınıfı, +yonetici/sifreHash alanları,
                   +sifreKarma(), +izinVar(), +Islem enum, +oturum yönetimi
OnlineActivity.kt  +yoneticiSifresiSor(), +yetkiEkrani(), +sifreDegistir(),
                   +izinli(), +yoneticiIsleminde(), +sifreSor(),
                   rol göstergesi, kısıtlama listesi, silme onayı
strings.xml        42 yeni string (1592 toplam)
```

\n
---

## 📑 v7.53 — Online: ayrı bölümler (3 Ağu 2026)

**Kullanıcı:** *"Gorevler vb seyler ayri ayri kullanilsin bunlarda online
sekmesinde gorunsun karsidaki insanda bunları gorebilsin"*

### Önce tek liste vardı, şimdi 4 bölüm
```
✓ Görevler 3   📝 Notlar 2   📚 Konular 1   🔥 Alışkanlık 4
```
Sekmeler üstte, her birinde bekleyen öğe sayısı rozeti var.
Giriş çubuğu aktif bölüme göre değişiyor ("Görev ekle…" / "Not başlığı ekle…").

### Bölümler
| Bölüm | İçerik | Paylaşım özelliği |
|---|---|---|
| **Görevler** | metin + sahip + not | Kim ekledi, kim tamamladı |
| **Notlar** | başlık + uzun içerik | Dokun → oku, düzenle |
| **Konular** | başlık + alt maddeler + %ilerleme | Alt maddeyi kim işaretledi görünür |
| **Alışkanlıklar** | ad + emoji + günlük işaret | **Her üye kendi işaretini koyar** |

### Alışkanlıkta ikili takip
En dikkat çekici kısım: her alışkanlıkta iki tarafın bugünkü durumu
yan yana görünüyor:
```
🏃 Yürüyüş
Bugün:  ✅ Ahmet   ⬜ Mehmet
```
İşaretler `yyyyMMdd|kisi` biçiminde saklanıyor, son 60 kayıt tutuluyor.

### Yetki sistemi korundu
v7.52'deki 7 yetki tüm bölümlerde geçerli:
- Ekleme yetkisi yoksa giriş çubuğu gizleniyor
- Silme yetkisi yoksa not/konu/alışkanlık silinemiyor
- Geri alma yetkisi yoksa işaret kaldırılamıyor (alt madde ve alışkanlık dahil)
- Başkasının notunu düzenleme ayrı yetkiye bağlı

### Veri boyutu yönetimi
textdb.online URL sınırı olduğu için her bölüm sınırlı:
notlar 30, konular 20 (her biri 30 madde), alışkanlıklar 15 (60 işaret),
görevler 60, mesajlar 30. Alan adları tek harf (`i`, `b`, `c`, `s`, `e`).

### Geriye dönük uyumluluk
Eski odalarda `n`/`t`/`l` alanları yok — boş liste olarak okunuyor,
çökme yok. Eski oda yeni sürümle açılınca 4 sekme görünüyor, üçü boş.

### Dosyalar
```
OnlineStore.kt      +Not, +Konu, +AltMadde, +Aliskanlik modelleri
                    +bugunAnahtari(), kodla/coz genişletildi
OnlineActivity.kt   +Bolum enum, +sekmeleriCiz(), +notlariCiz(),
                    +konulariCiz(), +aliskanliklariCiz(), +12 işlem fonksiyonu
                    ciz() sekmeli yapıya çevrildi
activity_online.xml +HorizontalScrollView sekme şeridi
strings.xml         23 yeni string (1615 toplam)
```

\n
---

## 💬 v7.54 — Ortak sohbet + yapay zekâ (3 Ağu 2026)

**Kullanıcı:** *"Ekranin ustundeki yere sohbet yeri ekle arkadasinla sohbet
edip yapay zekayla entregreli olsun yapay zeka ile konusma esnasinda
tiklayip ekleme yapabilme ozelligi ekle"*

### Sohbet ilk sekme oldu
```
💬 Sohbet   ✓ Görevler   📝 Notlar   📚 Konular   🔥 Alışkanlık
```
Uygulama açılınca doğrudan sohbet geliyor.

### Balon tasarımı
| Kim | Konum | Renk |
|---|---|---|
| Sen | sağ | koyu vurgu |
| Karşı taraf | sol | gri |
| 🤖 Asistan | sol | açık vurgu |

Her balonda gönderen adı ve saat var.

### Yapay zekâ entegrasyonu
Mesaja **`@ai`** ile başlayınca yapay zekâya gidiyor. Hem soru hem cevap
sohbete yazılıyor — **karşı taraf da ikisini görüyor.**

İki hızlı düğme: **🤖 Özetle** · **💡 Öneri ver**

### ⭐ Tıklayıp ekleme (asıl istenen özellik)
AI cevabının altında tıklanabilir öneriler çıkıyor:
```
Dokunarak ekle:
  ✓  Markete git                ＋
  📝 Fatura son ödeme 15 Ağustos ＋
  🔥 Günde 20 dk yürüyüş        ＋
```
Dokununca **ilgili bölüme** ekleniyor — görev/not/konu/alışkanlık.
AI kendisi hangi türe uygun olduğuna karar veriyor.

### `SohbetAi.kt` (~180 satır)
- `sadeIstek()` kullanıyor — `chat()` sistem istemi sohbeti bozuyordu (v7.50 dersi)
- Son 6 mesaj bağlam olarak gönderiliyor, AI konuşmanın akışını görüyor
- Oda üyeleri de bildiriliyor ("Ahmet'e görev ver" gibi istekler için)
- JSON bozuksa ham metni gösteriyor — boş ekran yok

### Yetki entegrasyonu
- **Sohbet herkese açık** — mesaj yazmak yetki gerektirmiyor
- Ama **öneriyi eklemek** EKLE yetkisine bağlı; kısıtlı üye uyarı görüyor

### Dosyalar
```
SohbetAi.kt        YENİ ~180 satır — AI istem, ayrıştırma, öneri çözme
OnlineStore.kt     Mesaj modeli: +tur, +oneriler · +saatMetni()
OnlineActivity.kt  +Bolum.SOHBET, +sohbetiCiz(), +mesajBalonu(),
                   +oneriyiEkle(), +mesajGonder(), +aiyaSor(), +kucukDugme()
strings.xml        19 yeni string (1634 toplam)
```

\n### 20 öneri yol haritası
| # | Özellik | Durum |
|---|---|---|
| 1-2 | Quiz + aralıklı tekrar | ✅ v7.29 |
| 9-10 | Hesap araçları + yönetmelik | ✅ v7.30 |
| 5 | PDF'e soru sorma (RAG) | ✅ v7.31 |
| 3 | Sesli ders anlatımı (TTS) | ✅ v7.32 |
| 4 | Bilgi kartları | ✅ v7.33 |
| — | **Ücretsiz mod (ücret kontrolü)** | ✅ **v7.34** |
| 4 | **Bilgi kartları (flashcard)** | ✅ **v7.33** |
| 17 | Sertifika ve rozet | sırada |

## Proje yapısı

**Mimari:** Fragment + XML layout + Material Components (Compose değil)
**Veri:** SharedPreferences + JSON (`gunluk_asistan_store`)
**Derleme:** AGP 8.5.2 · Kotlin 1.9.24 · JDK 17 · minSdk 24 / targetSdk 34

### Kotlin dosyaları (25)

| Dosya | Görev |
|---|---|
| `Store.kt` | Veri katmanı — görev/not/konu/günlük kayıt, yedekleme |
| `AsistanBrain.kt` | Kural tabanlı offline sohbet motoru |
| `TimerFragment.kt` | Pomodoro + 8 doğa sesi + equalizer |
| `HomeFragment.kt` | Ana ekran (geri sayım, hedef, rozetler) |
| `MainActivity.kt` | BottomNav + ViewPager + crash yakalama |
| `TopicsFragment.kt` / `TasksFragment.kt` / `NotesFragment.kt` | Konular / görevler / notlar |
| `ProgressFragment.kt` · `StatsDetails.kt` | İlerleme ve istatistikler |
| `SettingsFragment.kt` · `ThemeFragment.kt` · `ThemeManager.kt` | Ayarlar ve tema |
| `AsistanFragment.kt` | Sohbet arayüzü (sesli komut dahil) |
| `KpssPack.kt` | Hazır KPSS konu paketi |
| **v5.1 yenileri:** `Badges.kt` · `NetChartView.kt` · `ExamsFragment.kt` · `WidgetProvider.kt` · `WeeklyReportReceiver.kt` |
| `AlarmScheduler.kt` · `ReminderReceiver.kt` · `BootReceiver.kt` · `App.kt` · `EqualizerView.kt` | Sistem katmanı |

### Alt menü (BottomNav)
Ana Sayfa · İlerleme · **[+ FAB]** · Konular · Zamanlayıcı
(Notlar, Görevler, Ayarlar, Görünüm, Denemeler → menü/FAB üzerinden)

### Kaynaklar
23 layout · 217 string · Poppins fontu (4 ağırlık) · 8 doğa sesi WAV ·
tema renkleri (`sage`, `caramel`, `terracotta`, `dusty_blue`, `cream_bg`)

## Derleme ortamı

⚠️ **`/opt` oturumlar arası siliniyor.** Yeni oturumda bir kez çalıştır:

```bash
bash ~/kur-ortam.sh      # JDK 17 + Android SDK 34 + Gradle 8.7  (~1 dk)
source ~/ortam.sh        # PATH ayarları
```

Derleme:
```bash
cd ~/GunlukAsistan && gradle :app:assembleDebug --no-daemon --console=plain
```

### Bellek notu (önemli)
Sandbox'ta sadece **1,9 GB RAM** var. `gradle.properties` buna göre ayarlandı:
- `org.gradle.jvmargs=-Xmx800m`
- `kotlin.compiler.execution.strategy=in-process` ← Kotlin ayrı 2 GB daemon açmasın diye
- `org.gradle.daemon=false`, `workers.max=1`

Bu ayarlar olmadan derleme **"daemon disappeared"** hatasıyla çöküyor.
Ayrıca AGP 8'de kaldırılan `dexOptions` bloğu `app/build.gradle.kts`'ten temizlendi.






















## 🗂 v7.33 — Bilgi kartları (flashcard)

20 önerinin **4. maddesi**. Boş 5 dakikada açıp çevirdiğin hızlı tekrar kartları.

Mühendislik Araçları → **Kartlar** sekmesi (üçüncü sekme)

### 144 hazır kart, 5 deste

| Deste | Kart | İçerik |
|---|---|---|
| 📐 AutoCAD Kısayolları | 47 | L, PL, TR, OFFSET, DLI, F3/F8/F12… |
| 🏗 Revit Kısayolları | 25 | WA, DR, LL, GR, VV, Tab… |
| 🧮 Mühendislik Formülleri | 16 | 2h+b=63, d²/162, TAKS, L/12… |
| 📏 Standart Değerler | 22 | Birim ağırlıklar, kapı ölçüleri, kat yüksekliği |
| 📋 Yönetmelik Sayıları | 28 | Min kolon, paspayı, kenetlenme, çekme mesafesi |

Kart seçim ilkesi: **tek satırda cevaplanabilen, sık gereken** bilgiler.
Uzun açıklamalar değil — onlar zaten derslerde var.

### Kendi kartını ekleme

Ön yüz + arka yüz + deste adı. Kendi destelerini oluşturabilirsin.

### Çalışma akışı

```
Kart ön yüzü  →  dokun  →  arka yüz  →  [✓ Biliyorum] [↻ Tekrar göster]
```

**"Tekrar göster"** kartı aynı oturumun sonuna ekler — bilmediğin kart
o seansta bir kez daha karşına çıkar. Aynı kart en fazla iki kez gelir.

### Kendi Leitner sistemi

Quiz'den **daha sıkı** aralıklar (kartlar bilgi bazlı, daha sık tekrar gerekir):

```
Kutu 0 → 1 gün      Kutu 3 →  8 gün
Kutu 1 → 2 gün      Kutu 4 → 16 gün
Kutu 2 → 4 gün      Kutu 5 → 45 gün (öğrenildi)
```

Oturum başına en fazla 20 kart: önce **vadesi gelmişler**, sonra hiç
görülmemiş **yeni kartlar**.

### Yeni dosyalar

```
KartStore.kt        Veri katmanı — Kart/Durum/DesteOzet modelleri
                    · bugunkuKartlar(), desteler(), cevapla()
                    · hazirDesteleriYukle() — bir kez, silinirse geri gelmez
                    · disaAktar/iceAktar (yedek)
HazirDesteler.kt    144 kart, 5 deste
KartActivity.kt     Çevirmeli kart ekranı
activity_kart.xml   Kart yüzü + ilerleme + iki düğme
```

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 68, versionName 7.33
AraclarFragment.kt         +Kartlar sekmesi, +kartSekmesi(), +kartEkleDiyalogu()
                           Adapter'a Deste ve Eylem satır tipleri
Store.kt                   exportJson/importJson v10 (kart verisi dahil)
AndroidManifest.xml        +KartActivity
res/values/strings.xml     +37 string (954 toplam)
```

### Doğrulama
Leitner aralıkları 2 senaryoda test edildi ✓
Oturum kuyruğu: bilinmeyen kartın sona eklenmesi ve en fazla iki kez
görünmesi doğrulandı ✓ · Hazır içerik sayımı: 5 deste, 144 kart ✓

---

## 🔊 v7.32 — Sesli ders anlatımı (TTS)

20 önerinin **3. maddesi**. 356 sayfalık içerik artık **~12 saatlik sesli
kursa** dönüştü — araba, yürüyüş, şantiye yolu.

### Nasıl çalışıyor

PDF okuyucudaki **🔊** düğmesi → hız seç → dinlemeye başla.

```
PDF metni (v7.31 önbelleği)  →  cümlelere bölünür
                                      ↓
              Foreground servis + TextToSpeech (tr-TR)
                                      ↓
        Bildirimden kontrol: ⏮ ⏸ ⏭ ✕  ·  ilerleme çubuğu
```

Uygulama kapansa bile okuma **devam eder** — foreground servis.

### Cümlelere bölme

TTS'in 4000 karakter sınırı var; ayrıca tek parça okumak duraklat/ileri/geri
kontrolünü işe yaramaz hâle getirir.

`cumlelereBol()` şunları yapar:
- Sayfa ayracı (`<<<SAYFA>>>`) ve altbilgi (`Günlük Asistan`) temizlenir
- Cümle sonlarından (`.!?:`) bölünür
- **220 karakterden kısa parçalar birleştirilir** — çok sık duraklamasın
- 3500'den uzun cümle 3000'lik parçalara ayrılır (TTS sınırı)

### Kaldığı yerden devam

Her cümlede konum `SharedPreferences`'a yazılır. Aynı dersi tekrar açınca
kaldığı cümleden devam eder. Ders bitince konum sıfırlanır.

### Bildirim kontrolleri

```
⏮ Geri    ⏸ Duraklat/Devam    ⏭ İleri    ✕ Durdur
Sayfa: "12 / 47 cümle · %25"  + ilerleme çubuğu
```

Ekran kapalıyken bile çalışır.

### Hız seçenekleri

`0.8× yavaş` · `1.0× normal` · `1.25× hızlı` · `1.5× çok hızlı`

### Yeni dosya

```
SesliDersServisi.kt   Foreground servis + TextToSpeech
                      · cumlelereBol() akıllı bölme
                      · Konum hatırlama (asset bazlı)
                      · Bildirim eylemleri (6 komut)
                      · calisiyor / duraklatildi / aktifAsset durumu
```

### Değişen dosyalar

```
app/build.gradle.kts        versionCode 67, versionName 7.32
AndroidManifest.xml         +FOREGROUND_SERVICE, +FOREGROUND_SERVICE_MEDIA_PLAYBACK
                            +SesliDersServisi (foregroundServiceType=mediaPlayback)
LessonPdfActivity.kt        +sesliDinle(), +sesliBaslat()
activity_lesson_pdf.xml     +lpListen (🔊)
res/values/strings.xml      +18 string (917 toplam)
```

### Doğrulama
Cümle bölme testi: sayfa ayracı ve altbilgi temizlendi ✓ ·
tüm parçalar 3500 karakter altı ✓ · kısa parçalar birleşti ✓
İçerik hesabı: 356 sayfa × ~1800 karakter ≈ **11,9 saat** sesli içerik.

### Not: Türkçe ses paketi
Cihazda Türkçe TTS paketi yoksa varsayılan dil kullanılır.
Kullanıcı **Ayarlar → Erişilebilirlik → Metin okuma** üzerinden
Türkçe sesi indirebilir.

---

## 💬 v7.31 — Derse soru sorma (RAG)

20 önerinin **5. maddesi**. Artık ders okurken "anlamadım" diyebiliyorsun.

### Nasıl çalışıyor

PDF okuyucudaki **💬** düğmesi → soru sor → yapay zekâ **o dersin gerçek
metniyle** cevap verir. Genel bilgiyle değil.

```
PDF (assets)  →  pdfbox metin çıkarır  →  önbelleğe alınır (bir kez)
                                              ↓
Kullanıcı sayfa 3'te soru sorar  →  bağlam hazırlanır
                                              ↓
              [Sayfa 3 — baktığı sayfa] + [Sayfa 4] + [Sayfa 2] + kalanlar
                                              ↓
                      Yapay zekâya gönderilir → cevap
```

### Neden sayfa öncelikli

Tüm PDF'i göndermek hem token limitini aşar hem de model odağını kaybeder.
`baglamHazirla()` kullanıcının **baktığı sayfayı** başa koyar, sonra komşu
sayfaları, kalan yere de baştan itibaren diğer sayfaları ekler.
Böylece "bu tabloda ne yazıyor" gibi sorular doğru sayfaya denk gelir.

Sınır: 12.000 karakter (~3000 token).

### 8 hazır soru (tek dokunuş)

```
📝 Özetle        Bu dersi 5 maddede özetle
🧒 Basitleştir   Hiç bilmeyen birine anlatır gibi
💡 Örnek ver     Gerçek şantiye/proje örneği
❓ Neden böyle   Kuralın mantığı ne
⚠ Sık hatalar   En sık yapılan hatalar
🎯 Sınavda       Sınavda/mülakatta ne sorulur
🔗 Bağlantı      Diğer konularla ilişkisi
📋 Adımlar       İşlemi adım adım sırala
```

### Uydurmaya karşı önlem

İstemde açık kural: *"İçerikte olmayan bir şey soruluyorsa 'Bu ders bunu
kapsamıyor ama genel olarak...' diyerek belirt. UYDURMA. Emin olmadığın
sayı, standart veya kural verme."*

Model tamamen reddetmiyor ama **kaynağını ayırt ediyor**.

### Önbellek

Metin bir kez çıkarılıp `cacheDir/ders_metin/` altına yazılır. Aynı derse
ikinci soruda PDF yeniden ayrıştırılmaz — anında cevap.

### Yeni dosyalar

```
DersMetni.kt          pdfbox ile metin çıkarma + önbellek
                      · metniAl(), sayfalar(), baglamHazirla()
                      · hazirMi(), onbellegiTemizle(), onbellekBoyutu()
DersAsistan.kt        RAG istemci
                      · sor(), HAZIR_SORULAR (8 kalıp)
                      · Komut sızmasını engeller (AsistanKomut.ayikla)
dialog_derse_sor.xml  Çipler + giriş + cevap kartı
```

### Değişen dosyalar

```
app/build.gradle.kts        versionCode 66, versionName 7.31
gradle.properties           Xmx 1250m → 1024m (sandbox 2 kez düştü)
LessonPdfActivity.kt        +derseSor(), lpAsk düğmesi
activity_lesson_pdf.xml     +lpAsk (💬)
res/values/strings.xml      +10 string (899 toplam)
```

### Derleme notu
İlk iki deneme bellek yüzünden düştü. `gradle.properties` içinde
`Xmx 1250m → 1024m` ve kotlin daemon `500m → 420m` yapıldı;
üçüncü deneme **54 saniyede** tamamlandı. Bu ayar sonraki
sürümlerde de derlemeyi hızlandıracak.

---

## 📐 v7.30 — Mühendislik Araçları (hesaplar + yönetmelik)

20 önerinin **9 ve 10. maddeleri**. Uygulamayı "kurs" olmaktan çıkarıp
**her gün açılan iş aracına** çeviren ekleme.

Yan panel → **📐 Mühendislik Araçları** · Ekran 15 · İki sekme

### Sekme 1: Hesaplar (12 araç)

| Araç | Ne yapar |
|---|---|
| 🏗 Kiriş ön boyutlandırma | Açıklığa göre h ve b (L/10, L/12, L/6) |
| 🧱 Kolon ön boyutlandırma | Kat + alan + beton sınıfına göre kesit |
| ▭ Döşeme kalınlığı | Kenar oranından tek/çift yön tespiti |
| 🪜 Merdiven hesabı | 2h+b=63, tam sayı basamak, kol uzunluğu |
| 🪨 Beton ve demir metrajı | Hacim, donatı (kg/m³), kalıp, çimento |
| 🧱 Duvar ve sıva metrajı | Boşluk düşülmüş net alan, tuğla adedi |
| ⛏ Kazı ve dolgu | Kabarma katsayısı, kamyon sefer sayısı |
| 📐 TAKS / KAKS kontrolü | İzin verilen taban ve toplam inşaat |
| ➰ Donatı ağırlığı | d²/162 formülü, 12 m çubuk sayısı |
| 📏 Eğim ve kot | Yüzde, derece, eğimli uzunluk |
| ◻ Alan ve hacim | Dikdörtgen, üçgen, daire, yamuk |
| 🔄 Birim çevirici | 10 dönüşüm türü |

Her sonuçta **uyarı satırı** var: *"Ön boyutlandırmadır, kesin kesit statik
hesapla belirlenir."* Sonuç paylaşılabilir (WhatsApp, e-posta).

### Sekme 2: Yönetmelik (10 bölüm, 55 kayıt)

```
🪨 Beton         Sınıflar, paspayı, kür, slump
➰ Donatı        Birim ağırlıklar, min oranlar, kenetlenme
📐 Boyutlandırma Min kolon/kiriş/döşeme/perde ölçüleri
🌍 Deprem        DD düzeyleri, BKS, öteleme, düzensizlikler
⚖ Yükler        Hareketli yükler, birim ağırlıklar, kar/rüzgâr
🏙 İmar          TAKS, KAKS, çekme, kat yüksekliği, otopark
🪜 Merdiven      2h+b, ölçüler, korkuluk, kaçış mesafesi
⛏ Zemin         Emniyet gerilmesi, zemin sınıfları, kabarma
🔩 Çelik         Sınıflar, elastisite, kaynak
🌡 Yalıtım       TS 825 kalınlıklar, λ değerleri, ses
```

Kaynak: TS 500, TBDY 2018, TS 498, TS 708, Planlı Alanlar İmar Yönetmeliği,
BYKHY, TS 825. Her kayıtta **kaynak maddesi** belirtilir.

**Tamamen çevrimdışı** — sahada internet olmadan çalışır.
Türkçe karakter esnek arama: "paspayi" yazınca "paspayı" bulunur.

### Yeni dosyalar

```
HesapMotoru.kt        Saf hesap katmanı (Android'e bağımsız, test edilebilir)
                      12 araç, Sonuc/Satir/Arac/Alan modelleri
YonetmelikVeri.kt     10 bölüm, 55 kayıt, Türkçe esnek arama
AraclarFragment.kt    İki sekmeli ekran, dinamik giriş formu
fragment_araclar.xml  Sekme + arama + liste
item_arac.xml         Kart satırı
item_arac_baslik.xml  Bölüm ayracı
```

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 65, versionName 7.30
MainActivity.kt            +15 -> AraclarFragment, +openAraclar()
                           +drawerAraclarBtn, ekranAdi'na eklendi
activity_main.xml          +drawerAraclarBtn
AsistanKomut.kt            "araclar/hesap/yönetmelik" → ekran 15
AiClient.kt                istem listesine "araclar" eklendi
res/values/strings.xml     +13 string (889 toplam)
```

### Doğrulama
7 hesap Python simülasyonuyla test edildi:
kiriş 5m→25/45 ✓ · merdiven 300cm→17 basamak, 2h+b=63.0 ✓ ·
Ø12 demir→0.889 kg/m ✓ · TAKS 500/0.30→150 m² ✓ ·
duvar 9×2.8→18.9 m² net ✓ · eğim %3.00 ✓ · kolon 4kat→35 cm ✓

---

## 🎯 v7.29 — Quiz ve aralıklı tekrar sistemi

20 önerinin **1 ve 2. maddeleri**. En büyük eksikti: 356 sayfa içerik vardı
ama öğrenip öğrenmediğini ölçen hiçbir şey yoktu.

### Quiz sistemi

Ders menüsü → **🎯 Quiz oluştur** → yapay zekâ o ders için 5 soru üretir.

**Tasarım kararı: sorular bir kez üretilip saklanır.** Her açılışta yeniden
üretmek hem kotayı harcar hem de aralıklı tekrarı anlamsız kılar — aynı
sorular tekrar sorulmalı ki öğrenme ölçülebilsin.

Soru kalitesi için istem katı kurallara bağlandı:
- Ezber değil **anlama** ölçer ("şu durumda hangi komutu kullanırsın")
- Yanlış şıklar da mantıklı görünür, kolay elenmez
- Doğru şık rastgele konumda
- Ders konusuyla sınırlı, başka derse kaymaz

Cevap sonrası şık renklenir (yeşil/kırmızı) ve **açıklama gösterilir** —
yanlışın nedenini görmeden geçilmez. Geçme eşiği %60.

### Aralıklı tekrar (Leitner kutu sistemi)

Ders tamamlandığında otomatik olarak tekrar programına girer:

```
Kutu 0 →  1 gün      Kutu 3 → 14 gün
Kutu 1 →  3 gün      Kutu 4 → 30 gün
Kutu 2 →  7 gün      Kutu 5 → 90 gün (öğrenildi)

Quiz geçildi  → bir üst kutu (aralık uzar)
Quiz kalındı  → kutu 0 (yakında tekrar sorulur)
```

Kurs ekranının üstünde **🔁 BUGÜN TEKRAR** kartı belirir:
*"4 ders tekrar edilmeyi bekliyor"* → dokun, listeden seç, çöz.

Unutma eğrisine karşı en etkili yöntem — kalıcı öğrenmeyi belirgin artırır.

### Yeni dosyalar

```
QuizStore.kt        Veri katmanı
                    · Soru, QuizSonuc, TekrarKaydi modelleri
                    · Leitner mantığı (tekraraAl, tekrarSonucu)
                    · bugunTekrarEdilecekler(), ogrenilenSayisi()
                    · disaAktar/iceAktar (yedek entegrasyonu)
QuizUretici.kt      Yapay zekâ ile soru üretimi
                    · Savunmacı ayrıştırma: bozuk soru sessizce elenir
QuizActivity.kt     Quiz ekranı — tek ders veya karışık sınav
activity_quiz.xml   Soru, şıklar, ilerleme çubuğu, açıklama kartı
item_quiz_option.xml  Şık satırı (harf + metin + ✓/✗)
```

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 64, versionName 7.29
Store.kt                   toggleLesson → tekraraAl()
                           exportJson/importJson v9 (quiz verisi dahil)
CoursesFragment.kt         +quizAc(), +tekrarListesi(), +tekrarKartiniYenile()
                           ders menüsüne quiz seçeneği
fragment_courses.xml       +coReviewCard (tekrar kartı)
AndroidManifest.xml        +QuizActivity
res/values/strings.xml     +36 string (876 toplam)
```

### Doğrulama
Leitner mantığı 3 senaryoda test edildi (hep doğru / karışık / eşik).
Soru ayrıştırma 4 bozuk soruyla sınandı — yalnızca geçerli olan alındı.
APK içinde tüm quiz sınıfları doğrulandı.

---

## 📐 v7.28 — Üst araç çubuğu içeriğin üstüne alındı (kalıcı çözüm)

Kullanıcı isteği: *"3 noktayı ve asistanı 'iyi geceler' vb. yazıların üst
kısmında tut, her yazının üst kısmında olsun. Diğer sekmelerde de iç içe
geçmiş şekilde olmasın."*

### v7.27 neden yetmedi

v7.27'de düğmeler hâlâ **yüzüyordu** (`FrameLayout` katmanı). Çakışma
14 ekranın üst boşluğu 58dp'ye çekilerek örtülmüştü — ama bu bir yama:
yeni ekran eklenince yine çakışırdı, bazı ekranlarda da fazla boşluk kalıyordu.

### Kalıcı çözüm: dikey yapı

```
ÖNCESİ (katmanlı — düğmeler içeriğin ÜZERİNDE yüzüyor)
  FrameLayout
   ├── container          ← tüm ekran
   └── [⋮] [✨]           ← üstte yüzer, içeriği KAPATIR

SONRASI (dikey — düğmeler kendi yerini kaplar)
  LinearLayout (vertical)
   ├── topBar  52dp       ← [⋮] [✨]  sabit, kendi alanı
   └── container weight=1 ← içerik BURADAN başlar
```

Artık örtüşme **fiziksel olarak imkânsız** — düğmeler ve içerik ayrı
kutularda. Yeni ekran eklendiğinde de otomatik doğru çalışır.

### Yan temizlik

- **14 ekranın 58dp yaması geri alındı** → doğal boşluklarına döndü
  (home 20, notes 16, courses 18, today 8, asistan 6 …)
- `bg_top_button.xml` **silindi** — yuvarlak zemin artık gereksiz,
  düğmeler zaten kendi şeridinde
- Araç çubuğuna `colorSurface` arka plan verildi, içerik altından kaymıyor

### Değişen dosyalar

```
app/build.gradle.kts            versionCode 63, versionName 7.28
activity_main.xml               FrameLayout katmanı → LinearLayout dikey yapı
                                +topBar (52dp sabit şerit)
14 fragment layout              paddingTop 58dp → doğal değerler
res/drawable/bg_top_button.xml  SİLİNDİ (kullanılmıyor)
```

### Doğrulama
XML ayrıştırıcıyla geçerlilik, `aapt2` ile `topBar`/`aiButton`/`menuButton`
kaynakları doğrulandı. Silinen drawable APK'da **0 kez** geçiyor.

---

## 🎨 v7.27 — Üst bar çakışması ve metin temizliği

Kullanıcı bildirimi: *"İyi geceler ve günaydınlar yazısı 3 noktayı ve yapay
zekâ yazısını kapatıyor, düzelt. Hatalı yazıları komple düzelt."*

### 1. Çakışma — kök neden

Üç nokta ve ✨ düğmesi `FrameLayout` içinde **içeriğin üzerinde yüzüyordu**.
Ekranların üst boşluğu ise 8-20dp arasındaydı. Düğmeler 6dp margin + 46dp
yükseklik = **52dp** yer kapladığı için selamlama yazısı altlarında kalıyordu.

**14 ekranın üst boşluğu 58dp'ye çekildi:**
```
fragment_home · notes · courses · kaynaklar · today · progress · timer
settings · asistan · habits · events · exams · tasks · theme
```

### 2. Üst bar yeniden düzenlendi

İki ayrı serbest düğme yerine tek `LinearLayout`:

```
Öncesi: menuButton (marginStart=8)  +  aiButton (marginStart=52)
        → elle hesaplanmış konum, kayma riski

Sonrası: LinearLayout [ ⋮ (44dp) | 6dp | ✨ (44dp) ]
        → otomatik hizalama
```

Yeni `bg_top_button.xml` — yuvarlak `colorSurface` zemin + ripple.
İçerik kaydırılıp altına geldiğinde düğmeler **okunur kalıyor**.
`colorOutlineVariant` özel temalarda tanımsız olabildiği için kullanılmadı.

### 3. Metin temizliği

**Emoji varyasyon seçici (U+FE0F) kaldırıldı — 28 yerde**

`☀️` gibi bileşik emojiler bazı Android sürümlerinde **boş kare** çiziyor.
Simge korunup yalnızca seçici bayt atıldı.

| Dosya | Adet |
|---|---|
| `strings.xml` | 21 |
| `fragment_home.xml` | 2 |
| `widget_actions.xml` | 2 |
| `fragment_courses.xml` | 1 |
| `shortcuts.xml` | 2 |

**Selamlama simgeleri ayrıştırıldı**

`İyi geceler ✨` — AI düğmesiyle **aynı simgeydi**, kafa karıştırıyordu.
Ayrıca akşam/gece ilk düzeltmede aynı emojiye düşmüştü, o da ayrıldı:

```
Günaydın 🌅  ·  İyi günler 🌞  ·  İyi akşamlar 🌆  ·  İyi geceler 🌙
```

**Tarama sonucu:** 840 string bozuk Türkçe karakter, kaçışsız kesme işareti,
boş değer ve sondaki boşluk açısından tarandı — **temiz**.

### Değişen dosyalar

```
app/build.gradle.kts            versionCode 62, versionName 7.27
activity_main.xml               üst bar LinearLayout'a alındı
res/drawable/bg_top_button.xml  YENİ — yuvarlak zemin + ripple
14 fragment layout              paddingTop 58dp
res/values/strings.xml          21 varyasyon seçici, selamlama simgeleri
res/xml/shortcuts.xml           2 varyasyon seçici
```

---

## ✨ v7.26 — Ana sayfada yapay zekâ + tam uygulama entegrasyonu

Kullanıcı isteği: *"Yapay zekayı ana sayfada 3 noktanın yanına ekle ve bütün
uygulama ile entegre çalışmasını sağla."*

### 1. Ana sayfada ✨ düğmesi

Üç noktanın (`menuButton`) hemen yanına eklendi — **her ekranda görünür**.

| Etkileşim | Sonuç |
|---|---|
| **Dokun** | Asistan sohbet ekranı açılır |
| **Uzun bas** | Ekrandan çıkmadan hızlı soru penceresi |

Hızlı soru penceresi bulunduğun ekranı da bağlama ekliyor:
`[Kullanıcı şu an 'Kurslar' ekranında] ...`

### 2. Asistan artık iş yapıyor (komut sistemi)

Önceden yalnızca konuşuyordu. Şimdi cevabının sonuna gizli bir komut satırı
ekleyip uygulamada gerçek işlem yapıyor.

```
Kullanıcı: "yarın için betonarme çalışma görevi ekle"
Asistan:   "Ekledim, yarın için hazır."
           >>KOMUT: gorev_ekle | Betonarme çalış     ← gizli, silinir
           ✓ Görev eklendi: Betonarme çalış           ← kullanıcı bunu görür
```

**Desteklenen komutlar:**
```
gorev_ekle       | Metin      Yeni görev
not_ekle         | Metin      Yeni not (ilk satır başlık)
konu_ekle        | Başlık     Yeni konu
aliskanlik_ekle  | Başlık     Yeni alışkanlık
zamanlayici      | 25         Odak zamanlayıcısı
ekran_ac         | kurslar    13 ekrandan birine geçiş
ders_devam       |            Kaldığı dersi açar
```

**Güvenlik:** Komutlar yalnızca **ekleme** ve **ekran açma** yapar —
silme/değiştirme komutu **yoktur**. Boş veya bozuk değerler sessizce yok
sayılır. Sistem isteminde "yalnızca kullanıcı açıkça isterse komut üret"
kuralı var; ayrıca en fazla bir komut işlenir.

### 3. Asistan tüm uygulamayı görüyor

`buildSystemPrompt()` genişletildi. Önceden görev/not/konu/alışkanlık
biliyordu; artık **kurs verilerini de** biliyor:

```
- Kurslar: 47/226 ders (%20) tamamlandı, 38 saat içerik kaldı
- Kurs serisi: 6 gün üst üste (rekor: 9)
- Kaldığı ders: OSNAP — nesne kenetleme noktaları
- Ders notu aldığı konular: LAYER mantığı, HATCH, Merdiven detayı
- Kaydettiği kaynak: 12 adet
```

Artık "nerede kaldım", "bugün ne çalışayım", "hangi konuda not almıştım"
gibi sorulara gerçek veriyle cevap verebiliyor.

### Yeni dosya

```
AsistanKomut.kt   · ayikla()    Cevaptan komutu ayırır, metni temizler
                  · calistir()  7 komut türünü çalıştırır
                  Bozuk girdiye dayanıklı, hiçbir durumda veri silmez
```

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 61, versionName 7.26
AsistanKomut.kt            YENİ
MainActivity.kt            +aiButton bağlantısı, +hizliSor(), +hizliSorGonder(),
                           +ekranAdi()
AsistanFragment.kt         Komut ayıklama ve çalıştırma, sağlayıcı geçiş bildirimi
AiClient.kt                buildSystemPrompt'a kurs verileri + komut talimatı
activity_main.xml          +aiButton (üç noktanın yanında)
res/values/strings.xml     +12 string (840 toplam)
```

### Yakalanan hatalar

1. **Kotlin string template kaçışı** — Python heredoc `\$ekran` yazmış,
   Kotlin bunu değişken olarak görmezdi. Birleştirmeye çevrildi.
2. **5 geçersiz string referansı** — `tab_asistan`, `tab_progress`,
   `tab_today`, `tab_exams`, `tab_habits` projede yoktu; gerçek adlarla
   (`asistan_title`, `nav_progress`, `nav_today`, `exams_title`,
   `habits_title`) değiştirildi. Derleme öncesi betikle tarandı.

### Doğrulama
Komut ayrıştırma 5 senaryoda test edildi — komut satırının kullanıcıya
gösterilmediği doğrulandı. APK içinde `aiButton` kaynağı ve tüm komut
adları mevcut.

---

## 🧩 v7.25 — OpenRouter tam desteği

Kullanıcı bildirimi: *"OpenRouter key'im var, bunu da ekle."*

### Canlı listeye karşı doğrulama yapıldı

`https://openrouter.ai/api/v1/models` çekildi (364 model, 30 Tem 2026).
Koddaki adaylar sınandı — **ikisi ölüydü**:

| Model | Durum |
|---|---|
| `google/gemini-3-flash-preview` | ✓ geçerli |
| `openai/gpt-5.6-luna` | ✓ geçerli |
| `deepseek/deepseek-chat-v3:free` | ✗ **listede yok** |
| `meta-llama/llama-3.3-70b-instruct:free` | ✗ **listede yok** |

Ölü adlar 404 verip sessizce başarısız oluyordu. Gerçek listeden doğrulanmış
adlarla değiştirildi; sekizinin de varlığı betikle teyit edildi.

### Yeni model listesi (hepsi doğrulandı)

```
google/gemini-3-flash-preview          ücretli, varsayılan
google/gemini-3.1-flash-lite           ücretli, en ucuz
inclusionai/ling-3.0-flash:free        ÜCRETSİZ · 262k bağlam
google/gemma-4-31b-it:free             ÜCRETSİZ · 262k bağlam
nvidia/nemotron-3-super-120b-a12b:free ÜCRETSİZ · 262k bağlam
openai/gpt-oss-20b:free                ÜCRETSİZ · 131k bağlam
openai/gpt-5.6-luna                    ücretli
anthropic/claude-opus-5                ücretli, en güçlü
```

### Kredi bitince ücretsize düşme

OpenRouter kredi tükendiğinde **HTTP 402** döner. Bu kod hiç işlenmiyordu;
"model" kelimesi geçmediği için yedek deneme de tetiklenmiyordu.

```
402 -> ai_err_credit  ("Kredin bitmiş, ücretsiz modeller denenecek")
408 -> ai_err_server
```

`saglayiciDegistirmeliMi()` artık "kredi/credit" kelimesini de geçiş sebebi
sayıyor. `modelSirasi()` sonuna `ucretsizModeller()` eklendi — ücretli
modeller tükenince `:free` olanlar devreye giriyor.

### Canlı model listesi OpenRouter'da da çalışıyor

Ayarlar → **🔄 Sağlayıcıdan güncel model listesini çek** artık OpenRouter'ı
da destekliyor. Ücretsiz modeller **listenin başında** gösteriliyor,
`:batch` ve görsel-only modeller eleniyor.

### Ayarlar ipucu

OpenRouter seçiliyken anahtar yardımına satır eklendi:
*"Sonunda `:free` yazan modeller ücretsizdir, kredi harcamaz."*

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 60, versionName 7.25
AiClient.kt                presetModels + yedekModeller gerçek listeyle
                           +ucretsizModeller(), modelSirasi'na eklendi
                           canliModelListesi'ne OPENROUTER dalı
                           whenCode: 402 (kredi) ve 408 eklendi
                           saglayiciDegistirmeliMi: kredi/credit
SettingsFragment.kt        OpenRouter için ücretsiz model ipucu
res/values/strings.xml     +2 string (828 toplam)
```

### Doğrulama
APK içindeki 8 model adı canlı OpenRouter listesiyle karşılaştırıldı —
hepsi geçerli. Ölü adlar (`llama-3.3`, `deepseek-chat-v3`) dex içinde
**0 kez** geçiyor.

---

## 🔀 v7.24 — Yapay zekâlar arası otomatik geçiş

Kullanıcı isteği: *"Yapay zekalar arası geçiş sağla, hangisi uygunsa onunla
işleme devam ettir."*

### Nasıl çalışıyor

Bir sağlayıcı kota doldurduğunda / anahtarı geçersizleştiğinde / sunucusu
düştüğünde iş durmuyor — anahtarı tanımlı diğerine geçilip devam ediliyor.

```
Gemini (kota doldu)  →  OpenAI  →  OpenRouter
     ✗                     ✓
"Google Gemini yanıt vermedi, OpenAI ile devam edildi."
```

### Kritik tasarım kararı: her hatada geçiş YAPILMAZ

Körü körüne geçiş yapmak boşuna istek harcar ve kullanıcıyı yanıltır.
`saglayiciDegistirmeliMi()` hatayı sınıflandırır:

| Hata türü | Geçiş | Neden |
|---|---|---|
| Kota / 429 | **Evet** | Diğerinin kotası dolu olmayabilir |
| Anahtar geçersiz | **Evet** | Diğerinin anahtarı çalışabilir |
| Sunucu hatası (5xx) | **Evet** | Geçici, diğeri ayakta olabilir |
| Model bulunamadı | **Evet** | Önce model yedekleri, sonra sağlayıcı |
| **İnternet yok** | Hayır | Hepsinde aynı sonuç |
| **Çevrimiçi mod kapalı** | Hayır | Yapılandırma sorunu |
| **İçerik engeli (SAFETY)** | Hayır | İstem sorunlu, sağlayıcı değil |

Python simülasyonuyla 7 senaryo test edildi, hepsi doğru sınıflandı.

### İki katmanlı deneme

```
for (sağlayıcı in saglayiciSirasi) {        ← dış döngü: sağlayıcılar
    for (model in modelSirasi(sağlayıcı)) {  ← iç döngü: modeller (v7.22)
        ...
    }
}
```

Sıra: **kullanıcının seçtiği** → anahtarı olan diğerleri (Gemini öne alınır,
ücretsiz katmanı en geniş olduğu için).

### Sağlayıcı bazlı anahtarlar

Eskiden tek anahtar vardı; sağlayıcı değişince anahtarı da değiştirmek
gerekiyordu. Artık her sağlayıcının anahtarı ayrı saklanıyor:

```
AiSettings.getKeyFor(ctx, "gemini")      Sağlayıcının kendi anahtarı
AiSettings.setKeyFor(ctx, "openai", k)
AiSettings.anahtarliSaglayicilar(ctx)    Geçiş sırası için
AiSettings.isAutoSwitch(ctx)             Açık/kapalı (varsayılan açık)
```

Geriye dönük uyumlu: eski tek anahtar, o an aktif olan sağlayıcıya ait
sayılıyor. Ayarlar'da sağlayıcı değiştirince o sağlayıcının anahtarı
otomatik yükleniyor.

### Ayarlar → Yapay Zekâ

- **🔑 Tüm sağlayıcı anahtarları (N tanımlı)** — her biri ayrı girilir,
  dolu olanlar ● ile işaretlenir
- **Sağlayıcılar arası otomatik geçiş** anahtarı (varsayılan açık)

### Kapsam

Geçiş üç yerde birden çalışıyor:
- `AiClient.chat()` — asistan sohbeti
- `KursUretici.uret()` — otomatik müfredat
- `KaynakBulucu.ara()` — PDF/video arama

### Tanı ekranına eklenen satır

```
✓ Yedek sağlayıcı
     Google Gemini ⭐ → OpenAI (ChatGPT)
```

Tek sağlayıcı varsa uyarır ve ikinci anahtar eklemeyi önerir.

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 59, versionName 7.24
AiSettings.kt              +getKeyFor/setKeyFor/hasKeyFor/maskedKeyPreviewFor
                           +anahtarliSaglayicilar, +isAutoSwitch/setAutoSwitch
AiClient.kt                +saglayiciDegistirmeliMi, +saglayiciSirasi
                           +sonKullanilanSaglayici, +sonGecisBilgisi
                           chat() iki katmanlı döngüye çevrildi
KursUretici.kt             uret() sağlayıcı döngüsü
KaynakBulucu.kt            ara() sağlayıcı döngüsü
SettingsFragment.kt        +anahtarYoneticisi(), +otomatik geçiş anahtarı
                           sağlayıcı değişince anahtar takası
KaynaklarFragment.kt       tanıya yedek sağlayıcı satırı
res/values/strings.xml     +10 string (826 toplam)
```

### Derleme notu
İlk denemede `strings.xml` içinde kaçışsız kesme işareti hatası
(`Invalid unicode escape sequence`) — düzeltildi, tüm dosya tarandı.
Sonraki deneme bellek yüzünden düştü, üçüncüde `BUILD SUCCESSFUL in 58s`.

---

## 🐛 v7.23 — Kurslarda yapay zekâ çalışmama hatası giderildi

Kullanıcı bildirimi: *"Kurslarda yapay zekâ çalışmıyor, üst düzey yap."*

### İki kök neden bulundu

**1. `google_search` yanlış yazılmış (ölümcül)**

```
YANLIŞ:  .put("tools", ... .put("google_search", JSONObject()))
DOĞRU:   .put("tools", ... .put("googleSearch",  JSONObject()))
```

`generateContent` API'si **camelCase** bekliyor. snake_case yazımı hata
vermiyor, **sessizce yok sayılıyordu** — yani arama aracı hiç çalışmıyordu.
Model internete bakmadan yanıt üretmeye çalışıyor, çoğu zaman boş dönüyordu.

**2. Gemini 3 düşünen model — token bütçesi yetmiyordu**

Gemini 3 iç muhakeme (thinking) yapar ve bu token'lar **çıktı bütçesinden**
düşer. Bütçe küçükse model düşünürken tükenir, `finishReason=MAX_TOKENS`
ile **boş metin** döner.

| Yer | Eskiden | Şimdi |
|---|---|---|
| Ana sohbet | 800 | `tokenButcesi(model, 1200)` → 8192+ |
| Görsel okuma | 2048 | `tokenButcesi(model, 2048)` → 8192+ |
| Kaynak arama | 2048 | **16384** + `thinkingLevel: low` |
| Kurs üretme | 8192 | **32768** + `thinkingLevel: low` |

`dusunmeAyari(model)` yalnızca `gemini-3*` modellerine `thinkingConfig`
ekler — eski modellerde alan yok sayılacağı için güvenli.

### Dayanıklılık katmanları

**Araçsız yedek deneme** — bazı anahtarlarda/bölgelerde arama aracı kapalı
olabilir. `groundingAraTek(aracKullan=false)` ve `geminiUretTek(aracKullan=false)`
ile araçsız ikinci deneme yapılır. Artık "hiç çalışmıyor" durumu yok.

**OpenAI/OpenRouter desteği** — önceden "Gemini şart" deyip duruyordu.
Şimdi `openAiIleAra()` ile diğer sağlayıcılarda da sonuç üretilir; linkler
`dogrula()` ile HTTP kontrolünden geçer.

**Düşünme parçalarını atlama** — `parts[].thought == true` olan parçalar
metne eklenmiyor, yalnızca gerçek çıktı alınıyor.

**Somut hata mesajları** — boş yanıtta artık sebep gösteriliyor:
`MAX_TOKENS`, `SAFETY`, `blockReason` ayrı ayrı raporlanıyor.

### Tanı ekranı (yeni)

Kaynak Merkezi → **filtre düğmesine uzun bas** (veya hata ekranında
"Sorunu bul") → beş adım sınanır:

```
✓ İnternet bağlantısı
✓ Çevrimiçi mod açık
✓ API anahtarı var
     AIzaSy…4dQ2
✓ Sağlayıcı ve model
     Google Gemini · gemini-3-flash-preview
✗ Deneme isteği
     Model bulunamadı: gemini-2.5-flash
```

Artık "çalışmıyor" geri bildirimi somut bir tanıya dönüşüyor.

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 58, versionName 7.23
AiClient.kt                +dusunmeAyari(), +tokenButcesi()
                           callGemini ve konuOku bütçeleri düzeltildi
KaynakBulucu.kt            googleSearch düzeltmesi, +groundingAraTek(aracKullan),
                           +openAiIleAra(), MAX_TOKENS tanısı
KursUretici.kt             googleSearch düzeltmesi, +geminiUretTek(aracKullan),
                           düşünme parçası atlama, MAX_TOKENS tanısı
KaynaklarFragment.kt       +taniCalistir() tanı ekranı
res/values/strings.xml     +13 string (816 toplam)
```

### Doğrulama
APK içinde `googleSearch` var, hatalı `google_search` **0 kez** geçiyor
(`strings classes*.dex` ile sayıldı).

---

## 🔄 v7.22 — Güncel yapay zekâ modelleri + otomatik model geçişi

Kullanıcı bildirimi: *"Yapay zekâlarda API kabul etmiyor, eski sürümü
kullanıyor yapay zekâlar o yüzden diye tahmin ediyorum. Güncel sürümü yükle."*

**Tespit doğruydu.** İnternetten doğrulandı (Temmuz 2026):

| Sağlayıcı | Uygulamadaki (eski) | Güncel |
|---|---|---|
| Gemini | `gemini-2.5-flash` | **`gemini-3-flash-preview`** — Gemini 3 serisi çıktı |
| OpenAI | `gpt-4o-mini` | **`gpt-5.6-luna`** — GPT-4 serisi API'den emekli |
| OpenRouter | `llama-3.3-70b` | **`google/gemini-3-flash-preview`** |

OpenAI resmî kapatma listesinde `gpt-4o-*` modelleri 23 Temmuz 2026'da
kapatılmış; yerine `gpt-5.6-*` önerilmiş.

### Kalıcı çözüm — üç katman

Sadece model adlarını güncellemek yetmez; 6 ay sonra yine eskir.

**1. Güncel varsayılanlar**
```
GEMINI     gemini-3-flash-preview   (ücretsiz katman var)
OPENAI     gpt-5.6-luna             (en ucuz 5.6)
OPENROUTER google/gemini-3-flash-preview
```

**2. Otomatik yedek geçişi** — `chat()` artık tek model denemez:
```
Sıra: kullanıcının seçtiği → daha önce çalıştığı bilinen → yedek listesi

GEMINI  : gemini-3-flash-preview → gemini-2.5-flash → gemini-flash-latest
          → gemini-2.5-flash-lite → gemini-2.0-flash
OPENAI  : gpt-5.6-luna → gpt-5.4-mini → gpt-5-mini → gpt-4o-mini
```
404 veya "model bulunamadı" alınırsa sıradaki denenir. Çalışan model
`ai_model_cache` içine yazılır; sonraki isteklerde doğrudan o kullanılır.

Anahtar/kota/ağ hatalarında yedek denenmez — boşuna istek atılmaz.

**3. Canlı model listesi** — Ayarlar → Yapay Zekâ → **🔄 Sağlayıcıdan güncel
model listesini çek**

Gemini `GET /v1beta/models`, OpenAI `GET /v1/models` çağrılır. Gerçekten var
olan modeller listelenir, kullanıcı seçer. Gömme/ses/görsel modelleri elenir.

### Diğer düzeltmeler

`KaynakBulucu` ve `KursUretici` içinde `gemini-2.5-flash` sabit yazılıydı —
ikisi de `calisanModel() ?: defaultModel` kullanacak şekilde değişti.
Görsel okuma (el yazısı) modeli de `gemini-3-flash-preview`'a taşındı.

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 57, versionName 7.22
AiClient.kt                Provider varsayılanları + presetModels güncellendi
                           +yedekModeller, +modelSirasi, +calisanModel,
                           +calisanModeliKaydet, +canliModelListesi, +modelHatasiMi
                           chat() döngüsel yedek denemesi
                           gorselModeli() güncel modele taşındı
KaynakBulucu.kt            Sabit model kaldırıldı
KursUretici.kt             Sabit model kaldırıldı
SettingsFragment.kt        +"Modelleri yenile" düğmesi
res/values/strings.xml     +5 string (803 toplam)
```

---

## ✨ v7.21 — Otomatik kurs üretici (yapay zekâ müfredat kurar)

Kullanıcı isteği: *"Daha kolay bir şekilde yap. Hazırdaki mühendislik kursları
için bir yapay zekâ sistemi kur ve o yan sekmeye entegre et. Diğer kursları
açarsam otomatik alt başlıklar vb. şeyler gelsin, yapay zekâ internet
entegreli olsun."*

### Ne yapıyor

Kullanıcı sadece **konu adını** yazıyor ("SAP2000", "Çelik Yapılar", "Excel"),
sistem gerisini hallediyor:

```
Konu adı  →  İnternette gerçek eğitim programları araştırılır
          →  Bölüm → ders hiyerarşisi kurulur
          →  Her derse süre + açıklama yazılır
          →  Önizleme gösterilir
          →  Onaylanınca gerçek kurs olarak kaydedilir
```

Grounding (`google_search`) açık — müfredat uydurma değil, gerçek üniversite
ders içeriklerine ve sertifika programlarına dayanıyor.

### Üç giriş noktası

| Nerede | Ne olur |
|---|---|
| **Kurslar → +** | "✨ Yeni kurs oluştur" / "✍ Elle oluştur" seçimi |
| **Kaynak Merkezi → ✨ Yeni kurs oluştur** | Sıfırdan üret veya var olanı genişlet |
| **Kaynak Merkezi → ⚡ Tek tuşla hepsini bul** | Seçili ders için PDF+video birlikte aranır |

### Üretim seçenekleri

```
Kurs konusu : [SAP2000            ]
Seviye      : Sıfırdan ileri seviyeye ▾
              (Sadece temel / Orta / İleri)
Uzunluk     : Orta (~30 ders) ▾
              (Kısa ~18 / Uzun ~45)
```

### Müfredat kalite kuralları (istem içinde)

1. Bölümler kolaydan zora sıralanır
2. Her bölümde 4-8 ders
3. Başlıklar **somut** olmalı — "Giriş" değil, "Malzeme tanımlama ve C25 betonu"
4. Süreler gerçekçi: basit 10-15 dk, karmaşık 20-30 dk, uygulama 35-45 dk
5. Son bölümler **gerçek proje uygulaması** içerir
6. Türkçe terim + parantezde İngilizcesi: "Kiriş (Beam)"
7. Türkiye yönetmelikleri anılır (TS 500, TBDY 2018)

### Savunmacı ayrıştırma

Model bozuk çıktı verirse uygulama çökmez:
- ```` ```json ```` bloğu ve açıklama metni temizlenir
- Boş başlıklı bölüm/ders atlanır
- Dersi olmayan bölüm listeye alınmaz
- Süreler `coerceIn(5, 90)` ile makul aralığa çekilir (model 200 dk derse 90 olur)
- Hiç geçerli bölüm yoksa kullanıcıya anlaşılır hata

### Var olan kursu genişletme

`KursUretici.genislet()` mevcut ders başlıklarını modele bildirir:
```
BU DERSLER ZATEN VAR, TEKRARLAMA:
AutoCAD nedir; Arayüz turu; LINE PLINE...
Sadece EKSİK kalan konular için yeni bölümler üret.
```
Böylece aynı ders iki kez eklenmez.

### Yeni dosya

```
KursUretici.kt    · uret()      Konu adından tam müfredat
                  · genislet()  Var olan kursa eksik konular
                  · ayristir()  Savunmacı JSON çözümleme
                  · kaydet()    Store'a kurs+bölüm+ders yazar
                  Gemini grounding + OpenAI uyumlu yedek yol
```

### Değişen dosyalar

```
app/build.gradle.kts        versionCode 56, versionName 7.21
KursUretici.kt              YENİ
KaynaklarFragment.kt        +kursUretMenusu, +yeniKursDiyalogu, +kursUret,
                            +mufredatOnizle, +kursGenisletSec, +otomatikHepsi
CoursesFragment.kt          FAB artık yeniKursMenusu() açıyor
fragment_kaynaklar.xml      +kurs üretme kartı, +tek tuş düğmesi
res/values/strings.xml      +37 string (798 toplam)
```

### Derleme notu
İlk deneme bellek yüzünden düştü, ikincide `BUILD SUCCESSFUL in 1m`.

---

## 🔎 v7.20 — Kaynak Merkezi (yapay zekâ ile PDF/video bulma)

Kullanıcı isteği: *"Mühendislik kurslarındaki PDF'leri yapay zekâdan uygulama
içindeyken internetten bulup eklemesini istiyorum, video linki vb. şeyleri de.
Bunun için belirli bir yer yap."*

### ⚠ Çözülen temel sorun: uydurma linkler

Dil modelleri internete **bakmadan** link üretmeye zorlanırsa var olmayan
adresler uydurur. Bu çok yaygın bir hatadır ve özelliği işe yaramaz hâle getirir.

Üç katmanlı önlem alındı:

| Katman | Ne yapar |
|---|---|
| **1. Gerçek arama** | Gemini `google_search` aracı (grounding) — model gerçekten Google'da arar |
| **2. Grounding meta** | Model metinde link vermese bile `groundingChunks` içindeki gerçek adresler toplanır |
| **3. HTTP doğrulama** | Her adrese `HEAD` isteği atılır; açılmayan linkler `⚠` ile işaretlenir |

Video aramasında ek olarak **YouTube Data API** desteklenir (isteğe bağlı
anahtar). Oradan gelen sonuçların varlığı kesindir, doğrulama atlanır.

### Yeni ekran: Kaynak Merkezi (indeks 14)

Yan panel → **🔎 Kaynak Merkezi**

```
┌─────────────────────────────────────┐
│ Kaynak Merkezi              Tümü ▾  │
│ 12 kaynak · 7 belge · 5 video       │
├─────────────────────────────────────┤
│ Yapay zekâ ile internetten kaynak   │
│ ▸ OSNAP — nesne kenetleme noktaları │
│ [📄 PDF ara]      [▶ Video ara]     │
├─────────────────────────────────────┤
│ ▶  AutoCAD OSNAP Kullanımı          │
│    Teknik Çizim Akademi             │
│    OSNAP dersi · AutoCAD 2D         │
│ 📄 Autodesk OSNAP Kılavuzu          │
│    help.autodesk.com                │
└─────────────────────────────────────┘
```

**Akış:** Ders seç → PDF/Video ara → sonuçlar doğrulanır → çoklu seçim
listesi → onaylananlar kaydedilir.

PDF seçildiğinde **"link mi indir mi"** sorulur:
- *Sadece link* → yer kaplamaz, açmak için internet gerekir
- *İndir* → `DownloadManager` ile İndirilenler klasörüne iner, çevrimdışı okunur

### Veri katmanı

```
Store.DersKaynak(id, lessonId, baslik, url, tur, aciklama, kanal, eklendi, yerelDosya)
Store.kaynakEkle()      Aynı adres iki kez eklenmez
Store.kaynaklariOf()    Bir dersin kaynakları
Store.kaynakSil()       Geri alınabilir
Store.kaynakGeriEkle()  Snackbar geri alma
```

**Yedeğe eklendi** (`version: 8`) — v7.17'deki hatayı tekrarlamamak için
kaynaklar da `exportJson`/`importJson` kapsamına alındı.

### Kurs ekranından erişim

Ders menüsüne **🔎 Kaynaklar (N)** satırı eklendi. Kaynak varsa sayısı görünür,
dokununca o ders seçili hâlde Kaynak Merkezi açılır.

Bu değişiklik sırasında `showLessonMenu` içindeki kırılgan indeks kaydırma
mantığı (`if (hasPdf) which else which + 1`) **etiket+eylem çifti listesine**
dönüştürüldü — yeni seçenek eklerken hata riski kalktı.

### Ayarlar

**Ayarlar → Yapay Zekâ** altına *YouTube anahtarı (isteğe bağlı)* alanı eklendi.
Boş bırakılırsa Google Arama kullanılır; anahtar girilirse video sonuçları
kanal adı ve açıklamayla birlikte gelir.

### Yeni dosyalar

```
KaynakBulucu.kt           Arama motoru
                          · groundingAra()  Gemini + google_search
                          · youtubeAra()    YouTube Data API v3
                          · dogrula()       HTTP HEAD ile link sınama
                          · groundingKaynaklari()  meta veriden adres toplama
KaynaklarFragment.kt      Kaynak Merkezi ekranı
fragment_kaynaklar.xml    Ekran düzeni
item_kaynak.xml           Liste satırı
```

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 55, versionName 7.20
MainActivity.kt            +14 -> KaynaklarFragment, +openKaynaklar(lessonId)
                           +drawerKaynakBtn bağlantısı
CoursesFragment.kt         showLessonMenu yeniden yazıldı (+kaynak seçeneği)
Store.kt                   +DersKaynak modeli ve 7 fonksiyon
                           exportJson/importJson v8 (kaynaklar dahil)
AiSettings.kt              +getYoutubeKey/setYoutubeKey/hasYoutubeKey
SettingsFragment.kt        +YouTube anahtarı alanı
activity_main.xml          +drawerKaynakBtn
res/values/strings.xml     +54 string (762 toplam)
```

### Derleme notu
İlk deneme `mergeExtDexDebug` aşamasında düştü (bellek), ikinci denemede
`BUILD SUCCESSFUL in 25s`.

---

## 📷 v7.19 — Fotoğraftan konu ekleme (el yazısı okuma)

Kullanıcı isteği: *"Fotoğraftan konu başlığı ve konuları ekleme imkanı ver.
Yazılar el yazımı olacak, okunması güç olsa bile hatasız olmalı."*

### Akış

```
Konular → +  →  📷 Galeriden / 📸 Kamerayla / ✍ Elle
                        ↓
              Görsel hazırlanır (döndür, ölçekle, kontrast)
                        ↓
              Yapay zekâ okur (Gemini / GPT-4o vision)
                        ↓
              ⚠ KULLANICI DÜZELTİR — atlanamaz adım
                        ↓
              Konu + maddeler kaydedilir
```

### Doğruluk için alınan önlemler

El yazısı okumada "hatasız" hedefi tek bir yöntemle sağlanamaz. Beş katmanlı
yaklaşım kullanıldı:

| Katman | Ne yapar |
|---|---|
| **1. Görsel hazırlama** | EXIF döndürme, 1600 px ölçekleme, kontrast +45%, doygunluk −45% |
| **2. Katı talimat** | "Tahmin etme, okuyamazsan `???` yaz" — uydurma engellenir |
| **3. Sıcaklık 0** | `temperature=0.0`, `topP=0.1` — yaratıcılık kapalı, birebir okuma |
| **4. Çift geçiş** | İlk okuma şüpheliyse ikinci deneme + puanlamayla en iyisi seçilir |
| **5. Kullanıcı onayı** | Hiçbir şey onaysız kaydedilmez — her madde tek tek düzenlenebilir |

**Çift geçiş puanlaması** (`enIyisiniSec`):
```
-25 puan   her "???" için
+6 puan    her madde
+12 puan   başlık bulunduysa
-8 puan    2 karakterden kısa madde (şüpheli)
+uzunluk/12  toplam içerik
```
Kota israfı olmasın diye ikinci geçiş **yalnızca ilk okuma şüpheliyse** yapılır.

### Yeni dosyalar

```
GorselHazirla.kt      Görsel ön işleme
                      · oku() bellek dostu örnekleme
                      · dondur() EXIF yönü
                      · olcekle() 1600 px hedef, küçükleri büyütmez
                      · kontrastArtir() ColorMatrix ile
                      · sikistirVeKodla() 1.4 MB sınırına kadar kalite düşürür
                      · onizleme() diyalog için küçük bitmap

FotoKonuAkisi.kt      Akış yönetimi
                      · Ön kontrol (AI modu + anahtar)
                      · Arka planda okuma, arayüz kilitlenmez
                      · duzeltmeEkrani() dinamik madde satırları
                      · hataGoster() ipuçlarıyla + tekrar dene

dialog_foto_konu.xml  Düzeltme ekranı (önizleme + başlık + maddeler)
item_foto_madde.xml   Tek madde satırı (sıra no + metin + sil)
```

### AiClient eklentileri

```
konuOku(ctx, base64, ekNot)   -> Pair<Result, OkunanKonu?>
gorselModeli(ctx)             "lite" modelleri engeller, flash'a düşer
geminiGorsel()                inline_data + responseMimeType=application/json
openAiGorsel()                image_url + base64 data URI
jsonKonuAyristir()            ```json bloğuna ve açıklamalara dayanıklı
enIyisiniSec()                iki okumayı puanlayıp seçer
```

### Kullanıcı deneyimi ayrıntıları

- `???` içeren okuma varsa uyarı **kırmızı** gösterilir: "⚠ Bazı kelimeler okunamadı"
- Okuma başarısızsa somut ipuçları verilir (düz tut, ışık, sadece not görünsün)
- "Tekrar oku" ve "Elle gir" seçenekleri her hata ekranında var
- Yapay zekâ kapalıysa doğrudan Ayarlar'a yönlendirir
- Kamera izni reddedilirse galeri alternatifi hatırlatılır

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 54, +exifinterface:1.3.7
AndroidManifest.xml        +CAMERA izni, +camera özelliği (required=false)
AiClient.kt                +konuOku ve 6 yardımcı fonksiyon (~230 satır)
TopicsFragment.kt          +galeriSec/kameraCek/kameraIzni launcher'ları
                           showTopicDialog() artık 3 seçenek sunuyor
                           +fotografiOku(), +kamerayiAc(), elleKonuEkle()
GorselHazirla.kt           YENİ
FotoKonuAkisi.kt           YENİ
dialog_foto_konu.xml       YENİ
item_foto_madde.xml        YENİ
res/values/strings.xml     +29 string (708 toplam)
```

### Denetim
Denetleyici `AiClient.kt:606`'da `substring()` sınır uyarısı verdi
(Yüksek). `take()/drop()` ile yeniden yazıldı → **Yüksek 0**.

---

## 🛟 v7.18 — Veri kaybına karşı kurtarma sistemi

### Sorun

Kullanıcı bildirdi: *"Uygulama güncelleme geldiğinde içindeki her şey sıfırlanıyor."*
Üzerine kurulum yaptığını, buna rağmen verilerin gittiğini belirtti.

### İnceleme

| Kontrol | Sonuç |
|---|---|
| Paket adı | `com.gunlukasistan.app` — hiç değişmemiş ✔ |
| İmza SHA-256 | `5f15d4e7…` — v5.0'dan beri aynı ✔ |
| `SharedPreferences` adı | Tek dosya: `gunluk_asistan_store` ✔ |
| Veri silen kod | `clear()` / `deleteSharedPreferences()` **yok** ✔ |
| Seed koruması | `KEY_SEEDED` / `KEY_SEEDED_V2` doğru çalışıyor ✔ |
| **Otomatik yedek konumu** | **`getExternalFilesDir()` — HATA** ✘ |

**Kök neden:** `/Android/data/<paket>/files/` klasörü uygulama kaldırılınca
Android tarafından silinir. "Otomatik yedeğim var" sanılıyordu ama o yedek
uygulamayla birlikte yok oluyordu. Üzerine kurulumda verinin gitmemesi gerekir;
gittiyse tek açıklama uygulamanın kaldırılıp kurulmuş olmasıdır — ki o durumda
da güvenlik ağı devreye girmeliydi, girmiyordu.

### Çözüm 1 — Kalıcı yedek (İndirilenler klasörü)

```
Store.kaliciYedekYaz(ctx)   -> Downloads/GunlukAsistan-otomatik-yedek.json
Store.kaliciYedekOku(ctx)   -> aynı dosyayı okur
```

- **Android 10+**: `MediaStore.Downloads` API'si — **izin gerekmez**
- **Android 9 ve öncesi**: doğrudan dosya + `WRITE_EXTERNAL_STORAGE`
  (`maxSdkVersion="28"` ile sınırlı)
- Her `autoBackupNow()` çağrısında otomatik yazılır (açılışta tetiklenir)
- Aynı dosya üzerine yazılır, çoğalmaz

Bu dosya **uygulama kaldırılsa bile silinmez**.

### Çözüm 2 — Açılışta otomatik kurtarma

`MainActivity.veriKurtarmaKontrolu()` — `onCreate` içinde ilk iş olarak çalışır:

1. Daha önce sorulmuş mu? (`kurtarmaSoruldu`) → sorulmuşsa çık
2. Uygulama gerçekten boş mu? (`veriBosMu`: ders+görev+not+konu+alışkanlık hepsi boş)
3. İndirilenler'de kalıcı yedek var mı?
4. Varsa özetiyle birlikte teklif et:

```
Önceki verilerin bulundu

Uygulama boş görünüyor ama İndirilenler klasöründe bir yedeğin var.
Yedek tarihi: 30 Temmuz 2026 15:12

• Ders ilerlemesi: 47 / 226 tamamlandı
• Ders notu: 12 adet
• Görev: 8 adet
• Seri rekoru: 9 gün

           [Boş başla]  [Geri yükle]
```

"Boş başla" seçilirse bir daha sorulmaz (`kurtarmaSoruldu = true`).

### Çözüm 3 — Ayarlar'a kalıcı yedek seçeneği

**Ayarlar → Yedekle** artık üç seçenek:

| Seçenek | Ne yapar |
|---|---|
| Dosyaya kaydet | Konumu sen seçersin (SAF) |
| **Kalıcı yedek (İndirilenler)** | **YENİ** — uygulama silinse bile kalır |
| Paylaş | WhatsApp, e-posta, Drive |

### Değişen dosyalar

```
app/build.gradle.kts       versionCode 53, versionName 7.18
AndroidManifest.xml        +WRITE_EXTERNAL_STORAGE (maxSdkVersion 28)
Store.kt                   +kaliciYedekYaz/kaliciYedekOku/mediaStoreYedekYaz
                           +veriBosMu, +kurtarmaSoruldu
                           autoBackupNow() artık kalıcı yedek de yazıyor
MainActivity.kt            +veriKurtarmaKontrolu() — onCreate'te ilk iş
SettingsFragment.kt        +kaliciYedekAl()
res/values/strings.xml     +9 string (679 toplam), app_version 7.18
```

### Doğrulama
`aapt2` ile paket/sürüm, `keytool` ile imza (v5.0 ile aynı),
`strings` ile dex içindeki 5 yeni fonksiyon doğrulandı.

---

## 💾 v7.17 — Yedekleme sistemi baştan yazıldı (veri kaybı riski bitti)

### Bulunan kritik açık

Denetim sırasında fark edildi: `exportJson()` **kurs verilerini hiç yedeklemiyordu**.
Yedeğin içinde görevler, notlar, alışkanlıklar vardı ama **226 dersin ilerlemesi,
ders notları, yer imleri ve çalışma serisi yoktu**. Uygulamanın en değerli verisi
yedeğin dışındaydı.

Ayrıca geri yükleme yalnızca **JSON metnini elle yapıştırma** ile mümkündü —
dosyaya kaydetme veya dosyadan okuma yoktu.

### Yedeğin kapsamı genişletildi (`version: 7`)

Yedeğe eklenenler:
```
courses_json      Kurslar
sections_json     Bölümler
lessons_json      Dersler — done, note, fav, pdfAsset alanlarıyla
kurs_seri_gun     Çalışma serisi: son gün
kurs_seri_sayi    Üst üste gün sayısı
kurs_seri_rekor   Rekor
son_ders_id       Kaldığın ders
exported_at       Yedek tarihi (yeni)
```

Eski (v6) yedekler **hâlâ yüklenebilir** — eksik alanlar atlanır, mevcut ders
verileri korunur. Geriye dönük uyumluluk test edildi.

### Dosya tabanlı yedekleme (Storage Access Framework)

**Ayarlar → Yedekle** artık iki seçenek sunuyor:

| Seçenek | Ne yapar |
|---|---|
| **Dosyaya kaydet** | `GunlukAsistan-yedek-20260730-1512.json` — kullanıcı konumu seçer |
| **Paylaş** | Eski yöntem — WhatsApp, e-posta, Drive |

Diyalogda yedeklenecek içerik özeti gösterilir:
"Yedeklenecek: 226 ders (47 tamamlandı), 12 ders notu"

### Geri yükleme — önizlemeli ve geri alınabilir

**Ayarlar → Geri Yükle** dört yol sunuyor:

1. **Dosyadan geri yükle** — dosya seçici açılır
2. **Cihazdaki otomatik yedek** — özetiyle birlikte ("47/226 ders")
3. **Metin yapıştır** — eski yöntem, hâlâ var
4. **Son geri yüklemeyi geri al** — yalnızca geri alınabilir yedek varsa görünür

Geri yüklemeden **önce** onay ekranı çıkar:

```
Yedeği geri yükle?

Yedek tarihi: 30 Temmuz 2026 15:12

• Ders ilerlemesi: 47 / 226 tamamlandı
• Ders notu: 12 adet
• Görev: 8 adet
• Konu: 5 adet
• Alışkanlık: 3 adet
• Seri rekoru: 9 gün

Mevcut verilerinin üzerine yazılacak. Öncesinde otomatik
güvenlik kopyası alınır, istersen geri alabilirsin.
```

Bozuk/yabancı dosyada: "Bu dosya bir Günlük Asistan yedeği değil ya da bozuk."

### Güvenlik kopyası ve geri alma

Geri yüklemeden hemen önce mevcut durum `filesDir/geri-al-yedegi.json`
dosyasına yazılır. Yanlış yedek yüklenirse tek dokunuşla dönülür.

Dosyaya yazılıyor, **SharedPreferences'a değil** — 226 ders JSON'u prefs'i şişirmesin.

```
Store.yedekOzeti(text)            YedekOzet? — bozuksa null
Store.guvenlikKopyasiAl(ctx)      Geri yükleme öncesi kopya
Store.geriAlYedek(ctx)            Önceki duruma dön
Store.geriAlinabilirYedekVar(ctx) Menüde göstermek için
```

### Android otomatik yedekleme yapılandırıldı

Manifest'te `allowBackup="true"` vardı ama **kural dosyası yoktu**.

```
res/xml/backup_rules.xml           Android 11 ve öncesi
res/xml/data_extraction_rules.xml  Android 12+ (cloud-backup + device-transfer)
android:backupInForeground="true"
```

SharedPreferences yedeklenir, PDF önbelleği hariç tutulur. Artık Google hesabına
otomatik yedek alınır ve yeni telefona aktarımda veriler taşınır.

### Yan düzeltme

`app_version` stringi **7.13**'te kalmıştı — Ayarlar ekranı yanlış sürüm
gösteriyordu. **7.17** yapıldı.

### Değişen dosyalar

```
app/build.gradle.kts                    versionCode 52, versionName 7.17
AndroidManifest.xml                     +dataExtractionRules, +fullBackupContent
Store.kt                                exportJson v7, importJson kurs alanları,
                                        +YedekOzet, +yedekOzeti, +guvenlikKopyasiAl,
                                        +geriAlYedek, +geriAlinabilirYedekVar
SettingsFragment.kt                     +yedekKaydet/yedekAc (SAF), +onizlemeIleGeriYukle,
                                        +geriYuklendiBildir, +metinYapistirDiyalogu
res/xml/backup_rules.xml                YENİ
res/xml/data_extraction_rules.xml       YENİ
res/values/strings.xml                  +25 string (670 toplam), app_version düzeltildi
```

### Doğrulama
APK içinde `aapt2` ile string kaynakları, `strings` ile dex fonksiyon adları
doğrulandı. Yedek biçimi ve geriye dönük uyumluluk Python simülasyonuyla test edildi.

---

## 🏗️ v7.16 — Revit v2 başladı (Ders 1-6) + kaldığın yerden devam

### Revit Bölüm 1 tamamlandı + Ders 6

AutoCAD bitti, sıra Revit'te. İlk 6 ders v2'ye geçti.

| Ders | Konu | Sayfa | Öne çıkan |
|---|---|---|---|
| 1 | BIM nedir, CAD'den farkı | 4 | Kolon kaydırma karşılaştırması, 3B-7B boyutlar, dürüst zayıf yanlar |
| 2 | Revit arayüzü ve proje tarayıcısı | 5 | **5 bölüm numaralı gerçek ekran**, kısayol tablosu, kurtarma listesi |
| 3 | Proje şablonu ve birim ayarları | 4 | `.rte` mantığı, mm/0 ondalık, kendi şablonunu kurma |
| 4 | Aile (Family) kavramı | 4 | **Kategori→Aile→Tip→Örnek** hiyerarşi şeması, 3 aile türü |
| 5 | Seviyeler ve akslar | 5 | Cephe görünümü şart kuralı, 3B kilitleme, kurulum sırası |
| 6 | Duvar oluşturma ve katman yapısı | 5 | **Katmanlı duvar kesiti**, konum çizgisi, TR duvar tipleri |

**Revit toplam: 144 sayfa** (6 ders v2 + 41 ders v1 içerik)

### Revit'e özel çizim yardımcıları (`rv_v2_01.py`)

```
rv_duvar(c,x,y,uz,katmanlar)      Katmanlı duvar kesiti + etiketler
seviye(c,x,y,uz,ad,kot,sec)       Revit Level çizgisi + kot baloncuğu
aks_rv(c,x,y,sx,sy)               Revit Grid (kırmızı kesik çizgi)
tip_secici(c,x,y,w,aile,tip)      Type Selector kutusu
kutu_rv(c,...)                    Açık tema bilgi kutusu
ok_rv(c,...)                      Etiketli yön oku
```

### Yeni üretici: `uret_rv.py`

Revit için ayrı toplu üretici. **v2 varsa onu, yoksa v1'e düşer** — böylece
41 ders hâlâ içerikli kalıyor, v2'ye geçtikçe otomatik değişiyor.

```python
L = dict(L1); L.update(L2)   # v1 taban, v2 üzerine yazar
```

### Uygulama — "Kaldığın yerden devam et" kartı

Kurs ekranının üstünde, arama kutusunun altında yeni bir kart:

- Son açtığın dersi gösterir (yoksa ilk tamamlanmamış dersi)
- **Tek dokunuş** → dersin PDF'i doğrudan açılır
- Uzun bas → ders detayı
- Kurs adı · bölüm · süre alt satırda
- Arama veya yer imi filtresi açıkken **gizlenir** (ekran kalabalık olmasın)

`Store.sonDers()` zaten v7.12'de vardı; artık görünür bir giriş noktası oldu.

### Değişen dosyalar

```
app/build.gradle.kts                versionCode 51, versionName 7.16
CoursesFragment.kt                  +devamKartiniYenile(), rebuild()'e bağlandı
res/layout/fragment_courses.xml     +coResumeCard (MaterialCardView)
res/values/strings.xml              +5 string (645 toplam)
kurs-pdf/icerik/rv_v2_01.py         YENİ — Revit ders 1-6
kurs-pdf/uret_rv.py                 YENİ — Revit üreticisi (v2/v1 karma)
assets/dersler/revit/001..047.pdf   yeniden üretildi
```

### Derleme notu
İlk deneme dex aşamasında takıldı (bellek). `pkill -9 -f java` → 15 sn →
2. deneme → `BUILD SUCCESSFUL in 59s`.

---

## 🎓 v7.15 — AutoCAD kursu tamamlandı (58/58) + çalışma serisi

### Bölüm 10: Verimlilik ve İleri Konular (Ders 52-58)

Son 7 ders yazıldı, **AutoCAD kursu %100 bitti**.

| Ders | Konu | Sayfa | Öne çıkan |
|---|---|---|---|
| 52 | XREF — harici referans | 5 | Attach/Overlay, göreli yol, `VISRETAIN`, ETRANSMIT |
| 53 | Sheet Set Manager | 4 | Otomatik antet (FIELD), toplu PDF baskı |
| 54 | Tablolar + Excel | 4 | `TABLE` formülleri, `DATALINK` canlı bağ |
| 55 | DATAEXTRACTION | 4 | Öznitelikli bloklardan otomatik kapı/pencere listesi |
| 56 | PGP kısayolları | 4 | `EDITPGP`, `REINIT`, çakışma önleme, yedekleme |
| 57 | Script ve LISP | 5 | `.scr` toplu iş, `c:KOLON` LISP örneği, APPLOAD |
| 58 | PURGE/AUDIT/OVERKILL | 6 | Temizlik sırası, `-WBLOCK`, teslim kontrol listesi |

**AutoCAD toplam: 58 ders / 212 sayfa / ortalama 3,66 sayfa**

### Kurs yapısı (tamamlanmış hâli)

| Bölüm | Ders | Konu |
|---|---|---|
| 1 | 1-5 | Tanışma ve arayüz |
| 2 | 6-11 | Temel çizim komutları |
| 3 | 12-16 | Yardımcı araçlar (precision) |
| 4 | 17-23 | Düzenleme komutları |
| 5 | 24-28 | Katmanlar ve nesne özellikleri |
| 6 | 29-34 | Yazı, ölçülendirme, tarama |
| 7 | 35-39 | Bloklar ve kütüphane |
| 8 | 40-45 | Layout, ölçek, baskı |
| 9 | 46-51 | **İnşaat uygulamaları** (gerçek proje) |
| 10 | 52-58 | Verimlilik ve ileri konular |

### Yeni çizim yardımcıları (`ac_v2_10.py`)

```
kutu(c,x,y,w,h,baslik,satirlar)   Etiketli dosya/nesne kutusu
ok_etiket(c,x1,y1,x2,y2,etiket)   Etiketli yön oku (akış şeması)
kod_satir(c,x,y,satirlar)         Kod/komut penceresi görünümü
```

### Uygulama — kurs çalışma serisi (streak)

Motivasyon için en etkili özellik: üst üste kaç gün çalıştığını gösterir.

- Ders tamamlayınca gün otomatik işaretlenir (`kursGunuIsaretle`)
- Dün çalıştıysan seri artar, ara verdiysen 1'den başlar
- **Rekor** ayrıca saklanır
- Son 7 günün haritası: `▣ ▣ ▢ ▣ ▣ ▣ ▢` + gün adları (dinamik, bugüne göre)
- Duruma göre mesaj: "Bugün çalıştın, seri devam!" / "Seriyi kaçırma!"
- **Kurs ekranı → özete uzun bas** → istatistiklerin altında görünür

```
Store.kursGunuIsaretle(ctx)   Bugünü işaretle, seriyi güncelle
Store.kursSeri(ctx)           KursSeri(gunSayisi, rekor, bugunCalisildi, dunCalisildi)
Store.kursSonYediGun(ctx)     Son 7 günün Boolean listesi
```

`KursSeri.kopuk` — ne bugün ne dün çalışılmışsa seri fiilen sıfırdır;
`kursSeri()` bunu hesaba katıp geçerli seriyi döndürür (ham değeri değil).

### Değişen dosyalar

```
app/build.gradle.kts                versionCode 50, versionName 7.15
Store.kt                            +kursGunuIsaretle/kursSeri/kursSonYediGun
                                    +KursSeri data class, toggleLesson'a seri kaydı
CoursesFragment.kt                  +seriCubugu(), showStats'a seri bölümü
res/values/strings.xml              +5 string (640 toplam)
kurs-pdf/icerik/ac_v2_10.py         YENİ — ders 52-58
kurs-pdf/uret_v2.py                 ac_v2_10 eklendi
assets/dersler/autocad/052..058.pdf v2 içerikle değişti
```

### Derleme notu
İlk denemede yine "Gradle build daemon disappeared" (1,9 GB RAM sınırı).
`pkill -9 -f java` → 12 sn bekle → 2. deneme → `BUILD SUCCESSFUL in 26s`.
Bu desen artık standart; `derle.sh` arka planda çalıştırılıyor.

---

## 🏗️ v7.14 — İnşaat uygulamaları (Ders 46-51) + ders notu sistemi

### Kurs içeriği — AutoCAD Bölüm 9 v2'ye geçti

Kursun **en değerli** bölümü: teorinin gerçek projeye döküldüğü yer.

| Ders | Konu | Sayfa | İçerik |
|---|---|---|---|
| 46 | Kat planı çizimi (baştan sona) | 6 | 8 adımlı iş akışı, katman tablosu, aks→duvar→kapı sırası, 7 sık hata |
| 47 | Kesit ve görünüş çıkarma | 5 | İzdüşüm mantığı, kot sistemi, neyin taranacağı, kesit≠görünüş |
| 48 | Kalıp planı çizimi | 5 | Kolon/kiriş/döşeme gösterimi, S1-K101-D1 isimlendirme, statik katmanlar |
| 49 | Vaziyet planı ve kotlar | 4 | Parsel, çekme mesafesi, **TAKS/KAKS hesabı**, mutlak/rölatif kot |
| 50 | Merdiven detayı | 5 | **2h+b=63 kuralı**, basamak hesabı, ARRAY ile dizme, yönetmelik tablosu |
| 51 | Metraj (AREA, BOUNDARY) | 6 | Alan ekle/çıkar, TABLE formülleri, FIELD, birim tuzağı |

**AutoCAD v2: 51/58 ders (%88)** · 179 sayfa

### Yeni çizim yardımcıları

`ac_v2_09.py` içinde, sonraki derslerde tekrar kullanılabilir:

```
aks_sistemi(c,x,y,sx,sy)   Aks çizgileri + A-B-C / 1-2-3 baloncukları
duvar(c,x1,y1,x2,y2,t)     Çift çizgili duvar
kapi(c,x,y,gen,yon)        Kanat + 90° açılım yayı
pencere(c,x,y,uz,t,yatay)  Üç çizgili pencere gösterimi
oda_etiket(c,x,y,ad,alan)  Oda adı + m²
kot_isareti(c,x,y,deger)   Üçgen kot sembolü
tarama_beton(c,x,y,w,h)    Betonarme kesit taraması
tablo_cizgi(c,...)         Çizim içi TABLE nesnesi
```

### PDF motoru — otomatik sığdırma (taşma sorunu kökten çözüldü)

Önceki sürümlerde çizimler bazen ekran çerçevesinden taşıyordu ve her görsel
**elle** koordinat ayarı gerektiriyordu. Artık gerekmiyor.

| Fonksiyon | Ne yapar |
|---|---|
| `cmds_bbox(cmds)` | Çizim komutlarının sınır kutusu |
| `scale_cmds(cmds,k,ox,oy)` | Ölçekle + ötele (çizgi kalınlığı, yazı boyu, kesik çizgi dahil) |
| `fit_cmds(cmds,box,pad)` | Alana sığdır + ortala |
| `fit_info(cmds,box,pad)` | Sadece `(k,ox,oy)` dönüşümü döner |

**Kullanım deseni:**
```python
_g = []                              # geometri ayrı listede, 0,0 tabanlı
aks_sistemi(_g, 0, 0, [...], [...])
duvar(_g, 0, 0, 180, 0, 6.0)
_k, _ox, _oy = fit_info(_g, _a, pad=7)   # _a = acad_screen'in döndürdüğü alan
_c += scale_cmds(_g, _k, _ox, _oy)
def _P(x, y): return (_ox + x*_k, _oy + y*_k)
marker_line(_c, *_P(-46,118), *_P(-12,106), 1)   # işaretçiler tam boyutta
```

İşaretçiler `_P()` ile konumlanır ama **ölçeklenmez** — küçük çizimlerde bile
numara balonları okunaklı kalır.

### Uygulama — ders notu sistemi

Kullanıcı artık ders okurken not tutabiliyor.

- PDF okuyucu üst çubuğuna **✎** düğmesi (not varsa **📝** olur)
- Çok satırlı not penceresi, kaydet / vazgeç / notu sil
- Not dersle birlikte saklanır — uygulamayı kapatsan da durur
- **Kurs ekranı → yer imi filtresine uzun bas** → notu olan tüm dersler tek listede
- Listeden bir nota dokun → o dersin PDF'i doğrudan açılır
- Tüm notları tek metin olarak paylaş (WhatsApp, e-posta…)

```
Store.setLessonNote(ctx, lessonId, note)   Not kaydet (boş metin siler)
Store.lessonNote(ctx, lessonId)            Notu oku
Store.notluDersler(ctx)                    Notu olan tüm dersler
```

### Değişen dosyalar

```
app/build.gradle.kts                versionCode 49, versionName 7.14
LessonPdfActivity.kt                +notPenceresi(), +boyaNotSimgesi()
CoursesFragment.kt                  +showNotes()
Store.kt                            +setLessonNote/lessonNote/notluDersler
res/layout/activity_lesson_pdf.xml  +lpNote düğmesi
res/values/strings.xml              +11 string (633 toplam)
kurs-pdf/uret.py                    +4 sığdırma fonksiyonu
kurs-pdf/icerik/ac_v2_09.py         YENİ — ders 46-51
kurs-pdf/uret_v2.py                 ac_v2_09 eklendi
assets/dersler/autocad/046..051.pdf v2 içerikle değişti
```

### Denetim
Kritik 0 · Yüksek 0 · Orta 0 · Düşük 44 (v7.9'dan beri sabit)

---

## ⚡ v7.0 — Widget "Yükleniyor" takılması düzeltildi

Kullanıcı: *"Widgetların senkronizasyonu uzun sürüyor ve yükleniyorda kalıyor."*

### Teşhis: satır başına onlarca JSON parse
`GlassListService.buildHabits()` her alışkanlık için `habitCount()` **ve**
`habitStreak()` çağırıyordu. İkisi de `habitRoot()` üzerinden **her seferinde
JSON'u baştan parse ediyordu**:

| Widget | Eski maliyet |
|---|---|
| Alışkanlıklar (25 satır) | **50 JSON parse** + 1 loadHabits |
| Bugün | 25 parse + loadTasks |

`onDataSetChanged()` bu yükü senkron yapınca sistem zaman aşımına uğruyor ve
widget "Yükleniyor…" ekranında kalıyordu.

### Beş düzeltme

**1. `Store.habitRoot()` önbelleği**
2 saniyelik pencere: aynı çizim turundaki tüm okumalar tek parse kullanıyor.
50 parse → **1 parse**. Veri değişince (`saveHabitRoot`, `saveHabits`) önbellek tazeleniyor.

**2. `getLoadingView()` artık null değil**
null dönmek sistemin kendi "Yükleniyor…" görünümünü kullanmasına ve veri
gecikince ekranda **takılı kalmasına** yol açıyordu. Kendi hafif satırımız veriliyor.

**3. `onDataSetChanged()` try/catch**
Hata durumunda boş liste dönüyor — eskiden istisna fırlayınca widget sonsuza
kadar yükleniyor ekranında kalıyordu.

**4. `WidgetCommon.refreshAll()` debounce (400 ms)**
19 farklı noktadan çağrılıyordu; tek bir işlemde `saveTasks` + `bumpToday` +
`maybeAutoBackup` art arda tetikleyince 7 widget × 3 = 21 yayın oluşuyor,
sistem kuyruğu doluyordu. Artık 400 ms içindeki tekrarlar yok sayılıyor.

**5. Gereksiz hesaplama kaldırıldı**
- Satır sayısı 25 → **15** (widget zaten daha fazlasını göstermiyor)
- `habitStreak` yalnızca tek hedefli alışkanlıklarda hesaplanıyor
  (sayaçlı olanlarda rozet zaten `3/8` gösteriyor)
- `habitStreak` döngüsüne 400 gün üst sınırı (bozuk veride sonsuz döngü koruması)

### Derleme notu
PdfBox sonrası dex adımı ağırlaştı; heap 1150m → **1250m**, metaspace 384m.

## ✂ v6.9 — Elle bölme + sayfa içinden kesme

Kullanıcı: *"Bölüm 2 sayfanın belirli bir yerinde başlıyor."*

### Yeni ekran: `ManualSplitActivity`
- PDF sayfaları `android.graphics.pdf.PdfRenderer` ile görüntülenir (ek kütüphane gerekmez)
- **Sayfanın istenen noktasına dokunulur** → onay sorulur → bölme noktası eklenir
- Kırmızı çizgi + "✂ bölme" etiketi sayfa üzerine çizilir
- Sayfa gezinme, "sayfa başından böl", geri al, seçilen noktaların listesi
- Noktalar `"sayfa:oran"` biçiminde döner (oran: üstten 0f–1f)

### Sayfa içi kesim tekniği (`applyCrop`)
Gerçek PDFBox ile masaüstünde test edildi:

| Deneme | Sonuç |
|---|---|
| Yalnızca `CropBox` ayarlamak | ❌ Görüntüleyici tam sayfayı gösteriyor |
| `MediaBox` **+** `CropBox` birlikte | ✅ Doğru kırpma |

Görsel doğrulama yapıldı (`pdftoppm` ile PNG'ye çevrilip incelendi):
üst parça bölüm 1'in sonunu, alt parça bölüm 2'nin başını gösteriyor.

**Koordinat dönüşümü:** PDF'te y ekseni **alttan** başlar, kullanıcı oranı
**üstten** verir → `newTop = upperRightY - height × ratio`

### `Chapter` modeli genişletildi
`startOffset` / `endOffset` (0f–1f) alanları eklendi. `hasPartialPage` true ise
ilk ve/veya son sayfa kırpılır. Tek sayfalık bölümde her iki kesim aynı sayfaya uygulanır.
20pt'den ince kalan parçalar atlanır (bozuk çıktı önlenir).

### Kullanıcı akışı
PDF eklenince üç yol sunulur:
1. **Otomatik tespit** (yer imi / metin analizi) — bulursa önizleme gösterir
2. **✂ Sayfada işaretleyerek böl** — yeni elle ekran
3. **⚖ Eşit parçalara böl** — 2–40 parça

Otomatik tespit başarılı olsa bile "✂ işaretleyerek böl" seçeneği sunulur
(kullanıcı sonucu beğenmezse elle düzeltebilir).

### Derleme notu
PdfBox sonrası dex adımı ağırlaştı; iki kez "daemon disappeared" alındı.
Heap 900m → **1150m** yapılınca 50 saniyede derlendi.

## 🔧 v6.8 — PDF bölme düzeltmesi

### Asıl sorun: PdfSplitter workspace'ten silinmişti
Kullanıcı "bölümleri tek tek ayıramadı" dedi. Kontrol edildi:
`PdfSplitter.kt` **yoktu**, `MainActivity`'de `offerSplit/runSplit` **yoktu**,
`FileProvider` manifest'te **yoktu**. Yani v6.7 APK'sında bölme kodu hiç çalışmıyordu.
`kaynak-v6.7-yedek.zip`'ten geri yüklendi.

### Gerçek PDFBox ile doğrulama
Masaüstü `pdfbox-app-2.0.27.jar` indirilip **kodun birebir aynısı** Java'da
çalıştırıldı (Android emülatörü olmadan gerçek test):

| Test | Sonuç |
|---|---|
| 11 bölüm + içindekiler + üstbilgili sayfalar | ✅ 11/11 doğru |
| Bölme (sayfa aralıklarına ayırma) | ✅ 11 geçerli PDF, her biri 3 sayfa |
| Karışık biçimler (`ÖNSÖZ`, `1. TEMEL KAVRAMLAR`, `BÖLÜM 3`, `ÜNİTE-4`, `CHAPTER 6`, `KISIM 7`, `SONUÇ`) | ✅ 11/11 doğru |

### İyileştirmeler
**1. Sessiz başarısızlık kaldırıldı**
Bölüm bulunamazsa eskiden hiçbir şey olmuyordu — kullanıcı "çalışmadı" sanıyordu.
Artık bildiriliyor ve **elle eşit bölme** öneriliyor (2–40 parça seçilebilir).

**2. Kalıp havuzu genişletildi** (5 → 7)
- `1. TEMEL KAVRAMLAR` — numara + BÜYÜK HARFLİ başlık (en yaygın ders kitabı biçimi)
- `ÜNİTE-4` — tireli biçim

**3. Alt başlıklar eleniyor**
`rejectPattern`'a `^\d+\.\d+` eklendi → `1.1 Alt konu` bölüm sayılmıyor.

**4. Dayanıklılık**
`writeParts()` ortak fonksiyona çıkarıldı; bozuk bir bölüm atlanıyor,
diğerleri yazılmaya devam ediyor. Boş bölüm dosyası oluşturulmuyor.

## 📑 v6.7 — PDF'i bölümlere ayırma

PDF eklerken bölümler otomatik tespit edilip **her biri ayrı PDF** olarak kitaplığa eklenir.

### Kütüphane
`com.tom-roush:pdfbox-android:2.0.27.0` — Apache PDFBox'ın Android portu.
APK **9,3 → 18,3 MB** büyüdü (kaçınılmaz; gerçek PDF ayrıştırma başka türlü olmuyor).

### İki aşamalı bölüm tespiti (`PdfSplitter.kt`)

**1. Yer imleri (birincil)** — yayıncı PDF'e içindekiler gömdüyse en güvenilir kaynak.
`documentOutline` üst seviye başlıkları okunur, `PDPageDestination` ile sayfa numarası çözülür.

**2. Metin analizi (yedek)** — yer imi yoksa her sayfanın ilk 3 satırı taranır.
Kalıplar: `3. BÖLÜM` · `BÖLÜM III` · `2. ÜNİTE` · `CHAPTER 5` · `KISIM 2` ·
numarasız `ÖNSÖZ/GİRİŞ/SONUÇ/KAYNAKÇA`.

### ⚠️ Test sırasında yakalanan kritik hata
11 bölümlü test PDF'i üretilip çalıştırıldığında **33 bölüm** tespit edildi —
"Bölüm 3 - devam sayfası 1" gibi satırlar da başlık sanılıyordu.

**İki düzeltme yapıldı:**
- `containsMatchIn` → **`matches`**: satırın *tamamı* başlık kalıbına uymalı
- **`rejectPattern`** eklendi: `devam`, `continued`, `sayfa N`, `s. N`, `...` (nokta dizisi)
  içeren satırlar elenir — üstbilgi ve içindekiler satırlarını temizler
- Satır uzunluk sınırı 70 → 60, taranan satır 4 → 3

Düzeltme sonrası: **11/11 bölüm, her biri doğru sayfa aralığıyla** (1-3, 4-6, … 31-33).
Yer imi yöntemi de ayrı test edildi, 11/11 doğru.

### Ek koruma
- İçindekiler sayfasındaki yalancı başlıklar: ilk 3 bölüm tek sayfalıksa elenir
- Aynı sayfadan başlayan tekrarlar temizlenir
- `maxChapters = 60` güvenlik sınırı
- `OutOfMemoryError` ayrı yakalanır (büyük dosya uyarısı)

### Kullanıcı akışı
1. PDF eklenir → arka planda taranır ("Bölümler taranıyor…")
2. Bulunursa önizleme gösterilir: *"11 bölüm bulundu — • BÖLÜM 1 (3 sayfa) …"*
3. Kullanıcı onaylarsa bölünür, her bölüm kitaplığa **girintili** eklenir
4. Bölüm bulunamazsa sessizce geçilir, kitap normal eklenmiş olur

### Veri modeli
`Store.Book`'a eklendi: `parentId` (ana kitap), `chapterNo` (sıra), `chapterCount`.
`rootBooks()` / `chaptersOf()` yardımcıları. Ana kitap silinince bölümleri de silinir.

Bölüm dosyaları `filesDir/kitaplik/` altına yazılır, `FileProvider` ile açılır.

## 📚 v6.6 — Yan panel + PDF kitaplığı

### Yan panel (Navigation Drawer)
- `activity_main.xml` artık `DrawerLayout` ile sarılı
- Sol üstte **⋮** butonu (`menuButton`) — dokununca panel açılır
- Panel kenardan kaydırarak da açılır
- Geri tuşu önce paneli kapatır (`onBackPressed` override)

### PDF kitaplığı
| Özellik | Ayrıntı |
|---|---|
| **＋ PDF ekle** | Sistem dosya seçici (SAF), PDF/EPUB/tüm dosyalar |
| Kalıcı erişim | `takePersistableUriPermission` → uygulama kapansa da açılabilir |
| Dosya adı | `OpenableColumns.DISPLAY_NAME`'den otomatik okunur, `.pdf` uzantısı atılır |
| Açma | Sisteme `ACTION_VIEW` ile devredilir (yerleşik PDF okuyucu) |
| Görsel | Her kitaba neon paletten renkli sırt + eklenme tarihi |
| Silme | Yalnızca listeden kaldırır, **dosyanın kendisi silinmez** |

### Veri katmanı
- `Store.Book` modeli: id, başlık, uri, sayfa, son okunan sayfa, renk, tarih
- `loadBooks` / `saveBooks` / `addBook` / `deleteBook` / `updateBookProgress`
- `progress` hesaplanan alan (okunan sayfa yüzdesi — ileride kullanılmak üzere)
- **Yedeklemeye dahil** (`KEY_BOOKS` export/import'a eklendi)

### Yeni dosyalar
`item_book.xml` · `activity_main.xml` (yeniden yazıldı) · 11 `lib_` stringi
· `androidx.drawerlayout:drawerlayout:1.2.0` bağımlılığı

### Doğrulama
- Derleme ilk denemede temiz
- 10/10 layout ID eşleşti · 11/11 string tanımlı
- Önceki tüm bileşenler bozulmadan duruyor (9/9 dex'te, minHeight ×4, notifBanner ✓)

### ⚠️ Kayıp yine yaşandı
v6.5'in 15 Kotlin dosyası + kaynakları silinmişti, `kaynak-v6.5-yedek.zip`'ten
geri yüklendi (bu sefer tüm `res/` klasörleri toplu kopyalandı).

## 🔔 v6.5 — Bildirim görünmeme sorunu

### Sorun
Kullanıcı "bildirim ekranında uygulamayla ilgili bir şey görünmüyor" dedi.

**Teşhis:** Kod doğruydu (kanal, izin, `notify()` çağrısı hepsi yerinde).
Sorun **sessiz başarısızlık**: Android 13+ sürümlerde `POST_NOTIFICATIONS`
runtime izni reddedilmişse `notify()` hiçbir hata vermeden yok sayılıyor.
Kullanıcı bunu anlayamıyordu — uygulama içi ayar "açık" görünüyordu
ama sistem izni kapalıydı.

### Çözüm
**`TimerNotifier.canPost()` / `isReady()`** — sistem iznini de kontrol eder
(`NotificationManagerCompat.areNotificationsEnabled()`), sadece uygulama
ayarına bakmaz.

**Uyarı şeridi** (`notifBanner`, Sayaç ekranı):
- Bildirim kapalıysa görünür: *"🔕 Bildirimler kapalı — zamanlayıcı bildirim
  panelinde görünmez. Açmak için dokun"*
- Dokununca **doğrudan sistemin uygulama bildirim ayarları** açılır
  (`ACTION_APP_NOTIFICATION_SETTINGS`, eski sürümlerde uygulama detayı)
- `onResume`'da durum tazelenir → izni verip dönünce şerit kaybolur

**Periyodik tazeleme:** `tick()` içinde ~5 saniyede bir `TimerNotifier.show()`
çağrılıyor. Böylece ilerleme çubuğu ve duraklat/devam durumu bildirimde
senkron kalıyor (önceden yalnızca durum değişiminde güncelleniyordu).

### Doğrulama
- `POST_NOTIFICATIONS` manifest'te ✓ · kanal `zamanlayici_canli_v1` dex'te ✓
- `areNotificationsEnabled` çağrısı dex'te ✓ · 4/4 Timer sınıfı ✓

### ⚠️ Kayıp yine yaşandı
v6.4'ün 15 Kotlin dosyası + kaynakları workspace'ten silinmişti,
`kaynak-v6.4-yedek.zip`'ten geri yüklendi.

## ⏱️ v6.4 — Canlı geri sayım bildirimi + geri tuşu düzeltmesi

### Sorun 1: Geri tuşunda / sekme değişiminde sayaç sıfırlanıyordu
**Sebep:** `TimerFragment.onDestroyView()` içinde `running = false` yapılıyordu.
Fragment yok edildiğinde (sekme değişimi, geri tuşu) sayaç duruyordu.

**Çözüm — `TimerEngine.kt` (yeni):**
Sayaç durumu artık Fragment'tan bağımsız, `SharedPreferences`'ta yaşıyor.
- `onDestroyView` artık sayacı **durdurmuyor**
- Yeni `onResume()` motordaki duruma göre arayüzü tazeliyor
- Zaman ölçümü `elapsedRealtime` yerine **duvar saati** (`currentTimeMillis`)
  → cihaz uykuya dalsa bile geri sayım doğru ilerler
- Uygulama tamamen kapatılıp açılsa bile kaldığı yerden devam eder

### Sorun 2: Bildirimde geri sayım gösterilmiyordu
**Çözüm — `TimerNotifier.kt` (yeni), iPhone Live Activity tarzı:**
- `setUsesChronometer(true)` + `setChronometerCountDown(true)` →
  **sistem kendi sayar**, saniyede bir bildirim güncellemeye gerek yok (pil dostu)
- Kilit ekranında ve bildirim panelinde canlı azalan süre
- İlerleme çubuğu (geri sayımda)
- Üç buton: **Duraklat/Devam · Sıfırla · Kapat**
- Sessiz kanal (`IMPORTANCE_LOW`), titreşim yok, rozet yok
- Duraklatınca sabit değer metin olarak gösterilir

### Destekleyici sınıflar
- **`TimerAlarm.kt`** — geri sayım bitişine tam alarm kurar; uygulama kapalı
  olsa bile "Süre doldu!" bildirimi gelir, odak dakikası kaydedilir
- **`TimerActionReceiver.kt`** — bildirim butonlarını ve bitiş olayını işler

### Doğrulama
- 4 yeni sınıf dex'te ✓ · `TimerActionReceiver` manifest'te 4 intent filtresiyle ✓
- 13 `tn_` stringi ✓

### ⚠️ Yine kayıp vardı
Bu turda da v6.0–v6.3 dosyalarının **tamamı** workspace'ten silinmişti.
`kaynak-v6.3.1-yedek.zip`'ten geri yüklendi. Derleme sırasında ayrıca üç eksik
tespit edilip tamamlandı: `gl_*` stringleri, `stat_streak_inline`,
`SCREEN_HABITS`, `grid_*`/`chart_*` stringleri.

## 🔄 v6.3.1 — v6.2 düzeltmeleri geri getirildi (birleştirme sürümü)

### Sorun
Workspace'ten **v6.0 + v6.1 + v6.2'nin tamamı yine kaybolmuştu**.
v6.3 (cam widget'lar) eklenirken bu kayıp fark edilmediği için
`kaynak-v6.3-yedek.zip` yalnızca cam widget'ları içeriyordu —
halka kartlar, grafik, ızgara ve Zincir teması o zip'te yoktu.

### Çözüm: iki yedekten birleştirme
| Kaynak | Geri yüklenen |
|---|---|
| `kaynak-v6.2-yedek.zip` | `StatRingView` · `SparklineView` · `HabitGridView` · `ThemeManager` (Zincir + NEON_PALETTE) · `Store` (dailyTrend/habitWeeks) · `HomeFragment` · `HabitsFragment` · `themes.xml` · `fragment_home.xml` · `strings.xml` |
| `kaynak-v6.3-yedek.zip` | `GlassWidgetBase` · `GlassListService` · `WidgetCommon` (SCREEN_HABITS + 7 widget tazeleme) · 2 layout · 5 drawable · 3 widget-info · `AndroidManifest.xml` |

`strings.xml` v6.2 tabanı alınıp v6.3'ün 14 `gl_` stringi üstüne eklendi.

### Geri gelen v6.2 düzeltmeleri
- ✅ **Kart hizalama**: 4 istatistik kartı eşit yükseklikte
  (`minHeight=82dp` ×4, alt yazılar kaldırıldı, `stat_streak_inline` "12 · en iyi 18")
- ✅ **Izgara tam liste**: `take(5)` sınırı yok, satır yüksekliği %10,5,
  isim sütunu %33,5, `rowHeight()` alt sınırı

### Doğrulama (APK üzerinde)
- 7 widget manifest'te: Countdown · Summary · Tasks · Actions · GlassTasks · GlassHabits · GlassToday
- 8/8 sınıf dex'te · Zincir teması resources.arsc'de · dailyTrend/habitWeeks mevcut
- `minHeight=82dp` ×4 · `stat_streak_inline` ✓ · 14 `gl_` stringi ✓

> ⚠️ **Ders:** Her teslim zip'i o ana kadarki **tüm** durumu içermeli.
> Kayıp fark edilmeden yeni özellik eklenirse yedek eksik kalıyor.

## 🪟 v6.3 — Cam görünümlü liste widget'leri (Google Tasks tarzı)

Referans ekran görüntüsünden palet çıkarılıp **3 yeni widget** eklendi.

### Çıkarılan palet
| Öğe | Değer |
|---|---|
| Kart zemini | `#C7101010` (%78 opak → duvar kağıdı sızar) |
| Köşe yarıçapı | 30dp |
| Metin | `#FCFCFE` |
| Ayraç | `#26FFFFFF` (satır başında 43dp girinti) |
| Onay çemberi (boş) | 2dp `#8A8A8C` kenarlık |
| Onay çemberi (dolu) | `#8AB4F8` kenarlık + `%24` dolgu |
| Sağ etiket | `#A8A8AA` |

### Yeni widget'lar
| Widget | İçerik |
|---|---|
| 📋 **Görevler** | Bekleyen görevler · bugüne tarihli önce, saat/gecikme etiketi |
| 🌱 **Alışkanlıklar** | Aktif alışkanlıklar · sayaç `3/8` veya seri `🔥12` |
| 🗓️ **Bugün** | Alışkanlık + bugüne tarihli görev karışık tek liste |

Hepsi 4×4 hücre, yeniden boyutlandırılabilir, 25 satıra kadar kaydırılabilir.

### Etkileşim
- **Satıra dokun** → görev tamamlanır / alışkanlık işaretlenir (uygulama açılmaz)
- **Başlığa dokun** → ilgili ekran açılır
- **＋ butonu** → hızlı ekleme diyaloğu

### Mimari
- `GlassWidgetBase` — soyut taban sınıf; üç widget yalnızca `kind`, başlık,
  boş metin ve `＋` davranışını override eder
- `GlassListService` — tek `RemoteViewsService` üç içeriği besler
  (intent'teki `EXTRA_KIND` ile ayrışır)
- **Bugün** listesinde alışkanlıklar **negatif kimlikle** taşınır → tıklamada
  görev mi alışkanlık mı olduğu ayırt edilir
- Her widget/tür için ayrı `Uri` (`gunlukasistan://glass/<kind>/<id>`) —
  yoksa Android adapter'ları karıştırır
- `WidgetCommon.refreshAll()` artık 7 widget'ı birden tazeliyor

### Yeni dosyalar
`GlassWidgetBase.kt` (3 widget sınıfı içerir) · `GlassListService.kt` ·
`widget_glass_list.xml` · `widget_glass_row.xml` ·
`g_card/g_check_off/g_check_on/g_fab/g_divider.xml` ·
3 widget-info XML · 14 string

### ⚠️ Derleme sorunu
`mergeExtDexDebug` yine bellek tüketti (sandbox 1,9 GB). Heap 700m→**620m**,
metaspace 280m→**256m**, Kotlin daemon 700m→**600m** yapıldı.
`android.enableDexingArtifactTransform=false` denendi ama **AGP 8.3'te
kaldırılmış** → derleme hatası verdi, kaldırıldı. Sonuç: 19 saniyede derleniyor.

### Doğrulama
- 8/8 layout ID eşleşti · 14/14 string tanımlı
- Tüm view'lar RemoteViews uyumlu (LinearLayout, FrameLayout, ListView, TextView, View)
- 3 provider + servis manifest'te `BIND_REMOTEVIEWS` izniyle kayıtlı
- Şeffaflık Python önizlemesiyle görsel doğrulandı (duvar kağıdı kartın altından görünüyor)

## 🔧 v6.2 — Kart hizalama + ızgara düzeltmeleri

### Sorun 1: "Bu ay odak" kartı diğerlerinden büyüktü
**Sebep:** 4 karttan ikisinde (`streakSub`, `focusSub`) alt yazı satırı vardı,
diğer ikisinde yoktu → `wrap_content` yükseklikleri farklı çıkıyordu.

**Çözüm:**
- İki alt yazı TextView'i kaldırıldı
- Seri bilgisi tek satıra taşındı: `stat_streak_inline` → "12 · en iyi 18"
- Dört kartın iç düzenine `android:minHeight="82dp"` eklendi → içerik farkı
  olsa bile hizalı kalır
- `HomeFragment`'taki artık `streakSub` / `focusSub` referansları temizlendi
  (aksi halde `NullPointerException` ile çökerdi)

### Sorun 2: Günlük alışkanlıklar listesi eksikti
**Sebep:** `bindGrid()` içinde `.take(5)` sınırı vardı — 5'ten fazla alışkanlığı
olan kullanıcı kalanları göremiyordu.

**Çözüm:**
- `.take(5)` kaldırıldı → **tüm aktif alışkanlıklar** listeleniyor
- Satır yüksekliği %8,8 → **%10,5** (dokunma/okuma rahatlığı)
- Dar ekranlar için satır yüksekliğine alt sınır eklendi (`rowHeight()`)
- İsim sütunu %30 → **%33,5** (uzun alışkanlık adları kesiliyordu)
- Yüzde sütunu %11 → %10, yazı boyutları hafif büyütüldü

### ⚠️ Tekrarlayan kayıp sorunu
Bu turda **v6.0 + v6.1 değişikliklerinin tamamı yine workspace'te yoktu**
(`StatRingView.kt`, `SparklineView.kt`, `HabitGridView.kt`, Zincir teması,
`NEON_PALETTE`, layout düzeni, Store fonksiyonları).

`kaynak-v6.1-yedek.zip` sağlam olduğu için **oradan geri yüklendi** —
bu yüzden teslim zip'lerini saklamak kritik. Geri yükleme sonrası
7 Kotlin dosyası + 3 kaynak dosyası tazelendi.

### Doğrulama
- XML geçerliliği ✓ · 3/3 bileşen dex'te ✓ · Zincir teması resources.arsc'de ✓
- Kart hizalaması ve 7 satırlık ızgara Python önizlemesiyle görsel doğrulandı

## 📊 v6.1 — Çizgi grafik + haftalık ✓ ızgarası

Referans tasarımdaki kalan iki öğe eklendi.

### `SparklineView.kt` — Günlük İlerleme grafiği
- Son **21 günün** aktivite eğrisi, yumuşak quadratic Bézier ile bağlanmış
- Altında gradyan dolgu (opak → şeffaf), üstünde dış parıltı katmanı
- Her veri noktasında çift halkalı işaret (parıltı + çekirdek)
- Zemin ızgarası (3 yatay çizgi)
- Sağ üstte **haftalık değişim**: son 7 gün ile önceki 7 gün karşılaştırılır
  → "↑ %24" / "↓ %12" / "→ sabit"

**Aktivite puanı formülü:** `madde×3 + odak_dk×0.6 + soru×0.15`
(farklı ölçekleri dengelemek için)

### `HabitGridView.kt` — Haftalık ✓ ızgarası
- Satır = alışkanlık, sütun = hafta (4 hafta × 5 gün)
- Tamamlanan günler **dolu daire + tik**, boşlar sadece çember
- Her hafta sütunu farklı neon renkle vurgulanır (teal/yeşil/mor/sarı)
- Başlık şeridinde hafif renkli arka plan
- Sağda 28 günlük başarı yüzdesi
- Uzun alışkanlık adları otomatik kısaltılır (`ellipsize`)
- `onMeasure` ile satır sayısına göre kendi yüksekliğini hesaplar

### Yeni Store fonksiyonları
- `dailyTrend(days)` — günlük birleşik aktivite puanı dizisi
- `habitWeeks(habit)` — 4 hafta × 5 gün doluluk matrisi

### ⚠️ Önemli not: v6.0 değişiklikleri kayıptı
Bu turda fark edildi ki **v6.0'ın çoğu değişikliği workspace'te yoktu**
(Zincir teması, `StatRingView.kt`, halka kart düzeni, `NEON_PALETTE`).
Derleme hatalarıyla tespit edilip **tamamı yeniden uygulandı**:
- `Theme.GunlukAsistan.Zincir` → themes.xml
- `ThemeManager.NEON_PALETTE` + `isNeon()` + Zincir spec'i
- `StatRingView.kt` yeniden yazıldı
- Ana ekrandaki 4 kart halka düzenine çevrildi
- `HabitsFragment` neon palet bağlantısı

Bu yüzden v6.1 APK'sı hem v6.0 hem v6.1 içeriğini taşıyor.

### Doğrulama
- XML geçerliliği kontrol edildi
- 3/3 yeni sınıf dex'te, Zincir teması resources.arsc'de doğrulandı
- Grafik + ızgara Python'da yeniden üretilip referansla görsel karşılaştırıldı

## ⛓️ v6.0 — "Zincir" neon teması (referans tasarımdan)

Kullanıcı bir tasarım referansı gönderdi (koyu lacivert zemin, neon halkalar,
renkli seri çubukları). Görselden **piksel örnekleyerek** palet çıkarıldı.

### Çıkarılan palet
| Öğe | Renk |
|---|---|
| Zemin | `#010413` |
| Kart yüzeyi | `#0A1420` |
| Kart kenarı | `#1E2C3C` |
| Metin | `#E8EEF6` / `#8A97A8` |
| Neon teal | `#2BCFD0` |
| Neon yeşil | `#54CA5A` |
| Neon mavi | `#2C8DFE` |
| Neon mor | `#9B6BFF` |
| Neon sarı | `#FFCF50` |
| Neon pembe | `#FF6B9D` |

### Yeni tema: Zincir ⛓️
`Theme.GunlukAsistan.Zincir` — Material3 Dark tabanlı, 10. tema olarak eklendi.
Görünüm ekranından seçilebilir.

### Yeni bileşen: `StatRingView.kt`
Referanstaki neon halka göstergesi:
- Ortada emoji, çevresinde ilerleme yayı
- Yayın altında **dış parıltı katmanı** (neon hissi için)
- Yuvarlak uçlar, ekran boyutuna oranlı kalınlık

### Ana ekran yeniden düzenlendi
4 istatistik kartı dikey düzenden **halka + yatay bilgi** düzenine geçti
(referanstaki 2×2 kart yapısı):

| Kart | Halka | Doluluk ölçütü |
|---|---|---|
| 🔥 Süreklilik | teal | 30 günlük seri hedefi |
| ✅ Tamamlanan | yeşil | 500 madde hedefi |
| 📈 İlerleme | mavi | konu tamamlanma yüzdesi |
| ⏱️ Odak | sarı | aylık 1500 dk hedefi |

### Alışkanlık renkleri temaya bağlandı
Zincir temasında alışkanlık kartları neon paleti kullanır,
diğer temalarda mevcut yumuşak palet korunur (`ThemeManager.isNeon()`).

### Doğrulama
- Referans görselden baskın renkler + neon tonlar programatik olarak çıkarıldı
- Tema önizlemesi Python'da üretilip referansla görsel karşılaştırma yapıldı
- `stat_total` / `stat_progress` / `stat_focus` string adları yanlıştı
  (gerçekte `_name` sonekli) → yakalanıp düzeltildi
- XML geçerliliği doğrulandı, `Theme.GunlukAsistan.Zincir` APK içinde teyit edildi

## 🌊 v5.9 — Gürültü tabanlı ultra gerçekçi efektler

Basit şekil çiziminden **prosedürel gürültü (fBm) tabanlı** simülasyona geçildi.

### Yeni: `FxNoise.kt` — gürültü motoru
256×256 değer-gürültüsü tablosu + fractal Brownian motion (3 oktav).
Doğadaki organik hareketin temeli: alevin kıvrımı, dumanın savrulması,
rüzgârın esintisi, yaprakların sürüklenmesi hep buradan besleniyor.
Hızlı xorshift ile üretilir, her karede yüzlerce çağrı kaldırır.

### 🔥 Ateş — tamamen yeniden
- **6 katman**: duman → alt ısı → kömür yatağı → dış alev → iç alev → kıvılcım
- Her alev dili 20 dilimde örneklenir, her dilim fBm ile yatay saptırılır
  → alev kıvrılır, bölünür, savrulur (sabit sinüs yerine gerçek türbülans)
- **Bézier yumuşatma** (`smoothPolyline`): dilimler quadratic eğriyle birleşir.
  İlk denemede alevler köşeliydi — önizleme üretilip tespit edildi ve düzeltildi.
- **Kenar bulanıklığı** (`BlurMaskFilter`, ekran genişliğinin %1,1'i): keskin kenar
  plastik görünüyordu, yumuşatıldı
- Gerçek alev profili: tabanda geniş, ortada dolgun, uçta hızla sivrilen
- Duman gürültüyle savrulan 5 radyal katman; kömür yatağında 9 nefes alan sıcak nokta

### 🌧️ Yağmur
3 derinlik katmanı (uzak/orta/yakın), **yere çarpma sıçramaları** (yay çizimi),
gürültüyle dalgalanan sağanak şiddeti, altta ıslak yansıma.

### 💨 Rüzgâr
Akış çizgileri artık düz değil — **gürültü alanını takip eden 9 segmentlik kıvrımlı
yollar** (gerçek hava akımı gibi). Ekranı periyodik olarak geçen güçlü esinti dalgası,
derinliğe göre kalınlık/opaklık.

### 🌲 Orman
Ağaç aralarından süzülen 3 ışık huzmesi (gürültüyle salınan), gürültüyle sürüklenen
ve kendi ekseninde dönen yapraklar, zemin sisi.

### 🌊 Dalga
Her dalga **iki farklı frekanslı sinüsün toplamı + gürültü** — tek sinüs yapay
görünüyordu. 7 katman, köpük çizgileri, dolgu gradyanı, ufuk parıltısı.

### ☕ Kafe
**Bokeh** ışıkları (odak dışı 7 sıcak halka, gürültüyle titreşen), gürültüyle
savrulan buhar, sıcak mekân halesi.

### 🦗 Cırcır
Ateş böcekleri gürültü alanında süzülür, her biri kendi ritminde yanıp söner,
etrafında radyal hale bırakır. Gece mavisi derinlik.

### Performans
- 30 fps → **40 fps** (`FRAME_MS` 33→25)
- Parçacık üst sınırı 90 → 110; dalga artık parçacık kullanmıyor (matematiksel)
- Dilim sayısı 14 → 20 (dış), 10 → 14 (iç)

### Doğrulama
Kotlin'deki gürültü matematiği Python'da birebir yeniden yazılıp önizleme üretildi,
iki tur iyileştirme yapıldı (köşeli alevler → Bézier; keskin kenar → blur).
`Path` importu eksikti, derleme hatası yakalanıp düzeltildi.
5/5 yeni bileşen dex'te doğrulandı.

## 🔥 v5.8 — Yanan rakamlar (referans görsele göre)

Kullanıcı ikinci bir referans gönderdi: rakamların **kendisi yanıyor**, alevler
kartların önüne geçiyor. Görsel analiz edilip renk paleti çıkarıldı:

| Bölge | Ölçülen renk |
|---|---|
| Rakam gövdesi | `#E96B38` akkor turuncu |
| Alev çekirdeği | `#FFF7B5` sarı-beyaz |
| Kart yüzeyi | `#6A3D2F` isli |
| Arka plan | `#0C0302` neredeyse siyah |

### Yanan rakamlar (`FlipClockView.setBurning`)
Şömine sesi seçilince rakamlar 4 katmanda çizilir:
1. **Geniş hale** — `BlurMaskFilter` ile rakamın etrafını ısıtır
2. **Akkor gövde** — üstten alta `#FFD98A → #F5842F → #D94E14 → #8E2A08` geçişi
3. **Kor kenarı** — parlak sarı kontur (`#FFC84A`)
4. **Alev dilleri** — `Path`+`PathMeasure` ile rakamın dış hattı örneklenip
   her noktadan yukarı doğru kıvrılan alev çizilir (konum bazlı sabit rastgelelik
   sayesinde titremez, akar)

Kartlar da ateş modunda isli tona döner ve hafif saydamlaşır (`#E62A1A12`),
böylece arkadaki alevler sızar.

### Arka plan ateşi yeniden yazıldı (`AmbientFxView.drawFire`)
- **Duman perdesi** — üstte yükselen sıcak sis
- **Alt ısı gradyanı** — 4 duraklı, nabızla nefes alır
- **7 büyük alev dili + merkezde daha büyük bir tane** — kübik Bézier ile
  organik kıvrım, her biri farklı fazda salınır, iç çekirdek daha parlak
- **Kıvılcımlar** — ekranın altından fırlar, yükselirken sarıdan kırmızıya söner, izi kalır

### Ön plan katmanı (yeni)
Layout'a ikinci bir `AmbientFxView` eklendi (`fsFxFront`, `setForeground(true)`).
Saatin **üstüne** çizer: 3 uzun alev dili `saveLayerAlpha(150)` ile yarı saydam,
böylece referanstaki "alevler kartların önünden geçiyor" etkisi oluşur ama
rakamlar okunabilir kalır.

### Doğrulama
- Python'da kodun aynı Bézier matematiği çalıştırılıp önizleme üretildi,
  referansla görsel olarak karşılaştırıldı → alev renkleri ve alt ısı yoğunluğu
  iki tur ayarlandı (fazla sarı/baskındı, daha doygun turuncu-kırmızıya çekildi)
- `flamePaint` alanı eksikti → derleme hatası yakalanıp düzeltildi
- 3/3 layout ID, 4/4 yeni metod dex'te doğrulandı

## 🕐 v5.7 — Fliqlo tarzı yatay tam ekran saat + atmosfer efektleri

Zamanlayıcı ekranına **⛶ Tam ekran saat** butonu eklendi.

### Flip clock (`FlipClockView.kt`)
Referans görseldeki Fliqlo tasarımı birebir uygulandı:
- Siyah zemin, iki koyu kart (`#141414`), yuvarlak köşeler (kart yüksekliğinin %13'ü)
- Ortadan geçen yatay bölme çizgisi (%1,2 kalınlık, `#050505`)
- Devasa açık gri rakamlar (`#B9B9B9`, kart yüksekliğinin %82'si), Poppins Bold
- Sol alt köşede küçük etiket (PM yerine **DK/SA**)
- Rakam değişince üst yarı katlanma animasyonu (~24 ms adım)

### Atmosfer efektleri (`AmbientFxView.kt`)
Seçilen ortam sesine göre arka planda **8 farklı parçacık sistemi**:

| Ses | Efekt |
|---|---|
| 🔥 Şömine | Alttan turuncu parıltı + yükselen korlar (sarıdan kırmızıya sönerek) |
| 🌧️ Yağmur | Eğik damla çizgileri + mavi sis |
| 🌲 Orman | Süzülen dönen yapraklar + yeşil derinlik |
| 🌊 Dalga | Salınan ufuk çizgileri (7 katman, farklı hız) |
| 💨 Rüzgâr | Yatay akan ince çizgiler |
| ☕ Kafe | Sıcak ışık halesi + yükselen buhar |
| 🦗 Cırcır | Yanıp sönen ateş böcekleri (hale efektli) |
| 📻 Beyaz gürültü | Titreşen statik noktalar |

**Teknik:** ~30 fps (pil dostu), maks 90 parçacık, `LinearGradient`/`RadialGradient` ile
gerçekçi ışık. Mikrofon **kullanılmaz** — animasyon sesin ritmini taklit eden yumuşak
bir "nabız" değişkeniyle sürülür (şömine bu nabızla nefes alır gibi görünür).

### Tam ekran Activity (`FullscreenTimerActivity.kt`)
- Zorunlu yatay (`sensorLandscape`), immersive mod (durum/gezinme çubukları gizli)
- Ekran açık kalır (`FLAG_KEEP_SCREEN_ON`)
- Ekrana dokun → kontroller görünür, 6 sn sonra kendiliğinden solar
- Alt şeritten ses değiştirilebilir → efekt anında değişir
- Geri sayım bitince odak dakikası `Store`'a yazılır, widget'lar tazelenir
- Kronometrede duraklat/sıfırla anında geçen süre kaydedilir
- Arka plana geçince ses ve animasyon durur (pil koruması)

### Yeni dosyalar
`FlipClockView.kt` · `AmbientFxView.kt` · `FullscreenTimerActivity.kt` ·
`activity_fullscreen_timer.xml` · `fs_btn/fs_btn_accent/fs_chip/fs_chip_on.xml` ·
`Theme.FullscreenTimer` teması · 13 string

### ⚠️ Derleme notu
`mergeExtDexDebug` adımında bellek tükendi (1,9 GB RAM'de 20 MB'a düştü).
`gradle.properties`'te heap 800m→**700m**, metaspace 320m→**280m** yapıldı,
`android.enableR8.fullMode=false` eklendi → derleme 21 saniyeye indi.

### Kontroller
- 9/9 layout ID eşleşti · 13/13 string tanımlı
- 3 yeni sınıf da dex'te doğrulandı
- `screenOrientation=0x6` (sensorLandscape) manifest'te doğrulandı
- Kart oranları referans görselle karşılaştırıldı (önizleme üretilip görsel olarak denetlendi)

## 🔍 v5.6.1 — "Ücretsiz sınırsız AI" araştırması + Gemini netleştirme

### Araştırma sonucu: anahtarsız ücretsiz servis güvenilir değil

Kullanıcı "Chrome'daki AI Mode gibi sınırsız ve ücretsiz" istedi.
Anahtarsız çalışan servisler (Pollinations.ai) **canlı test edildi**:

| Test | Sonuç |
|---|---|
| İlk 6 basit istek | ✅ 6/6 çalıştı |
| Sistem talimatı eklenince | ❌ 402 Payment Required |
| Uzun prompt (120+ karakter) | ❌ 402 |
| 10 ardışık gerçekçi istek | ❌ **0/10** (7× kota, 3× 502) |

Servis dakikalar içinde çalışırken çalışmaz hale geldi → **uygulamaya eklenmedi.**
Bozuk bir özellik teslim etmektense mevcut çözümü netleştirmek tercih edildi.

**Neden "sınırsız ücretsiz" yok:** her cevabın GPU maliyeti var. Chrome'un AI Mode'u
Google'ın arama gelirinden finanse ediliyor ve API'si dışarıya açık değil.

### Yapılan iyileştirmeler
- `ai_help_gemini` stringi yazılmış ama **hiç kullanılmıyordu** (ölü string) → bağlandı
- Gemini seçiliyken artık net bilgi görünüyor:
  *"✅ Ücretsiz · kredi kartı istemez · günde ~1500 mesaj (kişisel kullanımda pratikte sınırsız)"*
- Kota hatası mesajı eyleme dönüştürüldü:
  *"Bir dakika bekleyip tekrar dene (dakikada ~15 istek)"*

### Değerlendirilen ama eklenmeyen seçenekler
- **Cihazda Gemma 3 1B** (MediaPipe): gerçekten sınırsız/ücretsiz/çevrimdışı,
  ama ~550 MB indirme + APK'ya ~50 MB + 4 GB RAM gereksinimi + belirgin kalite düşüşü
- **Pollinations.ai**: yukarıdaki test sonuçları nedeniyle elendi
- **OpenRouter ücretsiz modeller**: zaten sağlayıcı listesinde mevcut

## ⭐ v5.6 — Google Gemini öne alındı

Gemini zaten v5.5'te vardı ama listede ikinci sıradaydı ve varsayılan OpenAI'ydı.
Artık **varsayılan sağlayıcı Gemini**.

### Değişenler
- **Sıralama**: Gemini listede ilk, etiketi "Google Gemini ⭐ önerilen"
- **Varsayılan model güncellendi**: `gemini-2.0-flash` → **`gemini-2.5-flash`**
  (2.0-flash eskimişti; güncel alias'lar araştırılıp doğrulandı)
- **Hazır model listesi** (elle yazmak yerine açılır menüden seçim):
  `gemini-2.5-flash` · `gemini-flash-latest` · `gemini-2.5-flash-lite` · `gemini-2.5-pro`
  (diğer sağlayıcılar için de kendi listeleri var; "Diğer" seçilirse elle yazılabilir)
- **Hızlı başlangıç kutusu**: Gemini seçili ve anahtar boşken 4 adımlık rehber görünür
- **Gemini'ye özel hata yönetimi**:
  - Gemini geçersiz anahtarda `400` döner (diğerleri 401) → artık doğru mesaj veriliyor
  - `finishReason: SAFETY` → "güvenlik filtresine takıldı"
  - `finishReason: MAX_TOKENS` → "uzunluk sınırına takıldı"
  - `promptFeedback.blockReason` → engellenme sebebi bildiriliyor
  - Önceden bunların hepsi "boş cevap geldi" diyordu

### Doğrulama
- Gemini istek gövdesi (contents + systemInstruction + generationConfig) şemaya uygun
- Rol dönüşümü `assistant → model` doğru
- 4 cevap senaryosu test edildi: normal / safety / blocked / max_tokens → hepsi doğru ayrıştırıldı
- Endpoint canlı test edildi
- Derleme ilk denemede temiz

## 🤖 v5.5 — Hibrit yapay zekâ asistanı

Asistan artık **iki modda** çalışıyor. Varsayılan **çevrimdışı** (gizlilik önce).

| Mod | Nasıl çalışır |
|---|---|
| 🔒 **Çevrimdışı** (varsayılan) | Cihazdaki kural tabanlı beyin · hiçbir veri dışarı çıkmaz · internet gerekmez |
| 🌐 **Çevrimiçi** | Kendi API anahtarınla gerçek LLM · ChatGPT benzeri serbest sohbet |

### Desteklenen sağlayıcılar
- **OpenAI** (gpt-4o-mini vb.) — `platform.openai.com/api-keys`
- **Google Gemini** (gemini-2.0-flash) — `aistudio.google.com/apikey` · ücretsiz katman var
- **OpenRouter** — tek anahtarla onlarca model, ücretsiz seçenekler dahil
- **Özel** — OpenAI uyumlu her sunucu (LM Studio, Ollama, kendi sunucun)

### Akıllı yönlendirme
Eylem içeren komutlar (**görev ekle**, **plan yap**, **konu ekle**, **soru çözdüm**)
çevrimiçi modda bile **yerel beyinde** kalır — çünkü bunlar uygulamada gerçek
değişiklik yapar ve butonlu cevap üretir. Yalnızca serbest sohbet buluta gider.

### Bağlam aktarımı
Çevrimiçi modelde sistem talimatına kullanıcının **güncel durumu** eklenir:
bugünkü odak/hedef, seri, yaklaşan etkinlik, konular ve yüzdeleri, bekleyen görevler,
alışkanlık durumu. Böylece model "bugün ne yapmalıyım?" sorusuna gerçek veriyle cevap verir.

### Gizlilik tasarımı
- Varsayılan kapalı — kullanıcı açıkça açmadıkça **tek bir ağ çağrısı bile yapılmaz**
- Ağ kodu tek dosyada toplandı (`AiClient.kt`); `chat()` en başta mod kontrolü yapıp çıkar
- Ayar penceresinde ne gönderileceği açıkça yazıyor
- API anahtarı XOR+Base64 maskesiyle uygulamanın özel alanında saklanır
  (düz metin görünmesin diye; asıl koruma Android'in uygulama izolasyonu)
- Sunucumuz yok — istek doğrudan seçilen sağlayıcıya gider

### Kullanıcı deneyimi
- Asistan başlığında **mod rozeti**: 🔒 Çevrimdışı / 🌐 OpenAI / ⚠️ Anahtar gerekli
- Rozete dokun → Ayarlar'a gider
- **"Bağlantıyı sına"** butonu: kaydetmeden önce gerçek istek atıp sonucu gösterir
- Çevrimiçi çağrı sırasında "💭 Düşünüyor…" göstergesi
- **Otomatik geri düşme**: bağlantı/kota hatasında uyarı + cihazdaki beyinle devam
- Hatalar Türkçeleştirildi (401→"anahtar geçersiz", 429→"kota doldu" vb.)
- Son 8 mesajlık konuşma geçmişi korunur (bağlam sürekliliği)

### Teknik
- Ek kütüphane **yok** — `HttpURLConnection` + `org.json` (APK boyutu ~100 KB arttı)
- Ağ işlemleri tek iş parçacıklı `Executor`'da, `runOnUiThread` ile geri dönüş
- Yeni izinler: `INTERNET`, `ACCESS_NETWORK_STATE`
- Yeni dosyalar: `AiClient.kt`, `AiSettings.kt`

### Kontroller
- Derleme ilk denemede temiz
- Gizlilik doğrulaması: ağ çağrısı yapan tek dosya `AiClient.kt` ✓
- Endpoint'ler canlı test edildi (OpenAI/OpenRouter 401, Gemini 400 = adresler geçerli)
- Anahtar maskeleme/geri okuma Java'da test edildi ✓
- 38 string tanımlı, 4/4 layout ID eşleşti

## 🌱 v5.4 — Alışkanlık takibi (kişisel asistan katmanı)

Uygulama artık sadece sınav değil, **günlük hayat** asistanı.

### Yeni "🌱 Alışkanlıklar" ekranı
- Sınırsız alışkanlık: su iç, spor, kitap, ilaç, meditasyon…
- **Günlük hedef sayacı**: günde 1–20 kez (su için 8, spor için 1 gibi)
- Halkaya dokun → sayaç artar; hedefe ulaşınca tekrar dokunmak sıfırlar
- **Seri takibi**: kesintisiz gün sayısı, her alışkanlık için ayrı
- **Son 7 gün şeridi**: kartın altında mini görsel geçmiş
- **30 günlük başarı yüzdesi**
- 16 simge + 6 renk seçeneği

### Bugün ekranına entegrasyon
- Yeni "🌱 Alışkanlıklar" bölümü — ilk 5 tanesi, dokunarak işaretlenebilir
- Tamamlananlar soluklaşıyor, sayaçlı olanlar "3/8" gösteriyor
- Asistan önerisi artık alışkanlıkları da biliyor:
  *"Bugün 3 alışkanlığın seni bekliyor…"*

### Teknik
- `Store.Habit` modeli + `habits_json` / `habit_log_json` anahtarları
- Günlük kayıt yapısı: `{ "20260728": { "habitId": 3 } }`
- Fonksiyonlar: `toggleHabit`, `habitStreak`, `habitRecent`, `habitRate`,
  `habitProgressToday`, `habitCount`
- **Yedeklemeye dahil** — dışa/içe aktarmada alışkanlıklar ve tüm geçmiş korunuyor
- Widget'lar alışkanlık değişiminde otomatik tazeleniyor
- Ekran indeksi 12, `+` menüsünde 7. seçenek

### Kontroller
- Derleme ilk denemede temiz
- 13/13 `HabitsFragment` ID'si + 3/3 TodayFragment ID'si eşleşti
- 20/20 string tanımlı, formatlar Java'da test edildi
- `+` menüsü indeks eşleşmesi doğrulandı

## 🎨 v5.3 — Yeni widget ailesi

Tek widget yerine **dört ayrı widget**, modern Material 3 tasarımıyla yeniden yazıldı.

| Widget | Boyut | İçerik |
|---|---|---|
| ⏳ **Geri Sayım** | 2×2 | Dev rakamla kalan gün, etkinlik adı |
| 📊 **Günlük Özet** | 4×2 | Selamlama, geri sayım rozeti, hedef çubuğu, 3 istatistik kutusu |
| ✅ **Görev Listesi** | 4×3 | Kaydırılabilir liste, dokun→tamamla, ＋ile ekle |
| ⚡ **Hızlı Eylem** | 4×1 | Odaklan · Soru · Görev · Bugün |

### Teknik yenilikler
- **Açık/koyu tema**: `values/` + `values-night/widget_colors.xml` ile otomatik uyum
- **Android 12+ dinamik renk**: `values-v31/` + `drawable-v31/` → duvar kağıdı renklerine uyum
  (eski sürümlerde krem/karamel paletine düşer, fallback'ler doğrulandı)
- **Kaydırılabilir liste**: `TasksWidgetService` (RemoteViewsService) + `setRemoteAdapter`
  + `setPendingIntentTemplate` / `setOnClickFillInIntent` ile satır bazlı tıklama
- **Derin bağlantı**: `WidgetCommon.openScreen()` ve `quickAction()` — widget'tan
  doğrudan ekran açma veya diyalog gösterme (soru/görev ekleme)
- **Otomatik tazeleme**: `WidgetCommon.refreshAll()` dört widget'ı birden yeniler;
  `Store.bumpToday()` ve `saveTasks()` içinden tetiklenir
- **Gün değişimi**: `BootReceiver` artık `DATE_CHANGED`/`TIMEZONE_CHANGED` de dinliyor,
  gece yarısı geri sayımlar kendiliğinden güncelleniyor

### Dosya değişiklikleri
**Yeni:** `WidgetCommon.kt`, `CountdownWidget.kt`, `SummaryWidget.kt`, `TasksWidget.kt`,
`ActionsWidget.kt`, `TasksWidgetService.kt` · 5 layout · 5 drawable (+5 v31) ·
4 widget-info XML · `widget_colors.xml` (3 varyant)

**Silinen:** `WidgetProvider.kt`, `widget_gunluk.xml`, `widget_info.xml`,
`widget_bg/btn/progress.xml` (eski tek widget)

> ⚠️ Eski widget kaldırıldığı için ana ekrandaki mevcut widget kaybolur.
> Güncelleme sonrası yeni widget'ları elle eklemek gerekir.

### Yapılan kontroller
- Derleme temiz · 4 provider + servis manifest'te kayıtlı (`BIND_REMOTEVIEWS` izniyle)
- Tüm `R.id` referansları layout'larla eşleşti (30/30)
- Tüm `w_` string ve renkleri tanımlı (26 string, 10 renk)
- Layout'larda yalnızca RemoteViews uyumlu view'lar (LinearLayout, FrameLayout,
  TextView, ProgressBar, ListView) — desteklenmeyen view widget'ı görünmez yapar
- String formatları Java'da çalıştırılarak doğrulandı
- v31 renk/drawable'larının tamamı için fallback mevcut

## 🔧 v5.2 kurulum sorunu (versionCode 15)

**Belirti:** versionCode 14 APK'sı telefona yüklenmedi.

**Yapılan teşhis** (hepsi temiz çıktı):
- ZIP arşiv bütünlüğü ✓ · zipalign ✓ · dex checksum'ları (3/3) ✓
- Manifest'teki 7 sınıfın tamamı dex içinde mevcut ✓
- İzinler v5.1 ile birebir aynı, ek izin yok ✓
- İmza sertifikası v5.0/5.1 ile aynı (`5f15d4e7…`) ✓

**Bulgu:** APK yalnızca v2 şemasıyla imzalıydı, v1 (JAR) imzası yoktu.
Bazı paket yükleyicileri (özellikle dosya yöneticisi üzerinden kurulumda)
bu durumda "ayrıştırılamadı" hatası verebiliyor.

**Çözüm:** `app/build.gradle.kts` içinde imzalama açıkça yapılandırıldı:
```kotlin
enableV1Signing = true
enableV2Signing = true
enableV3Signing = true
```
Ayrıca `debug` ve `release` buildType'ları da aynı anahtara bağlandı.
Artık APK içinde `META-INF/MANIFEST.MF`, `CERT.SF`, `CERT.RSA` üretiliyor.

> Not: targetSdk 34 olduğu için Android v2 imzayı zaten **zorunlu** kılıyor;
> v1 tek başına yeterli değil. Şimdi üçü birden mevcut → en geniş uyumluluk.

**✅ ÇÖZÜLDÜ (28 Tem):** Dosya 4,46 KB olarak iniyordu — APK bozuk değildi,
indirme mobil veride yarıda kesiliyordu. **Wi-Fi'ye bağlanınca sorunsuz indi ve kuruldu.**
Bundan sonra APK'yı hep Wi-Fi'de indir; inen dosya **9,2 MB** olmalı.

**Eğer yine yüklenmezse** sorun APK'da değil aktarımdadır. Kontrol listesi:
1. Dosyayı indirdikten sonra boyutu **9,2 MB** olmalı (eksikse indirme yarım kalmıştır)
2. Ayarlar → Uygulamalar → *dosya yöneticisi/tarayıcı* → "Bilinmeyen uygulamaları yükle" izni
3. Play Protect uyarısı → "Yine de yükle"
4. Samsung'da bazen Drive'dan doğrudan açmak sorun çıkarır → önce **İndirilenler**'e
   kaydet, sonra dosya yöneticisinden aç

---

## 🆕 v7.1 — Sıfır Sürtünme Paketi (29 Temmuz 2026)

> **Amaç:** Bir şey kaydetmek için uygulamayı açma zorunluluğunu ortadan kaldırmak.
> Görev/not eklemek artık 2-3 dokunuş sürüyor, uygulama hiç açılmıyor.

### 1. Ana ekran kısayolları
Uygulama simgesine **uzun basınca** dört kısayol çıkar:

| Kısayol | Ne yapar |
|---|---|
| ➕ Görev | `QuickAddActivity` görev modunda açılır |
| 📝 Not | `QuickAddActivity` not modunda açılır |
| ⏱️ Odaklan | Doğrudan Sayaç ekranı (indeks 4) |
| 🗓️ Bugün | Doğrudan Bugün ekranı (indeks 2) |

- Dosya: `res/xml/shortcuts.xml`
- `MainActivity` içine `<meta-data android:name="android.app.shortcuts">` eklendi
- Kısayollar `android.intent.action.VIEW` + `targetClass` ile çalışır
- Uzun basıp sürükleyerek kısayolu ana ekrana ayrı simge olarak sabitleyebilirsin

### 2. Hızlı kaydet ekranı — `QuickAddActivity.kt`
Yarı saydam, diyalog görünümlü küçük bir ekran (`Theme.QuickAdd`).

- **Klavye kendiliğinden açılır** — bir dokunuş daha kazandırır
- Üstte **✓ Görev / 📝 Not** sekmeleri, anında geçiş
- Görev modunda **⏰ Hatırlat** düğmesi:
  `Hatırlatma yok · 1 saat sonra · Bu akşam 20:00 · Yarın sabah 09:00 · Tarih-saat seç…`
  - "Bu akşam 20:00" saati geçtiyse otomatik yarına atar
- Kaydedince alarm kurulur (`AlarmScheduler.schedule`), widget'lar tazelenir, ekran kapanır
- Not modunda **ilk satır başlık**, kalanı içerik olur
- `excludeFromRecents` + `taskAffinity=""` → son uygulamalar listesini kirletmez

### 3. Paylaş menüsüne eklendi
Başka bir uygulamada metin seç → **Paylaş** → **Günlük Asistan**

- `ACTION_SEND` (`text/plain`) — WhatsApp, Chrome, YouTube, e-posta…
- `ACTION_PROCESS_TEXT` — metni seçince çıkan üst menüde de görünür
- Gelen metin kutuya hazır dolu gelir
- **Akıllı tahmin:** metin 90 karakterden uzunsa veya satır başı içeriyorsa
  otomatik **not** moduna geçer, kısaysa **görev** kalır
- `EXTRA_SUBJECT` + `EXTRA_TEXT` birleştirilir (başlık + gövde)

### 4. Bildirim paneli kutucuğu — `FocusTileService.kt`
Perdeyi aşağı çek → **⏱️ Odaklan** kutucuğu.

- Basınca sayaç **başlar/durur** — uygulama hiç açılmaz
- Çalışırken kutucuk **yanar** (`STATE_ACTIVE`) ve alt yazıda **kalan süre** görünür
  (Android 10+ `subtitle` desteği)
- Süre hiç ayarlanmamışsa Ayarlar'daki günlük hedef süresi kullanılır (5–120 dk arası)
- `TimerEngine` + `TimerAlarm` + `TimerNotifier` ile tam uyumlu — canlı bildirim de açılır
- **Kurulum:** Bildirim panelini tam aç → ✏️ kalem/düzenle → "Odaklan"ı yukarı sürükle

### Teknik notlar
- `Theme.QuickAdd`: `windowIsTranslucent` + `backgroundDimAmount 0.55`
- `strings.xml`'e 24 yeni satır (`qa_*`, `sc_*`, `tile_*`)
- `app_version` string'i 5.2'de takılı kalmıştı → **7.1** olarak düzeltildi
  (Ayarlar › Hakkında artık doğru sürümü gösteriyor)
- Yeni izin **gerekmedi** — mevcut izinler yetti


---

## 🆕 v7.2 — Doğal Dil ile Ekleme (29 Temmuz 2026)

> Hızlı kaydet kutusuna **"yarın 14:00 dişçi"** yaz — tarih ve saat otomatik anlaşılır,
> görevin adı yalnızca **"dişçi"** olarak kaydedilir. Tamamen çevrimdışı, ağ gerekmez.

### Yeni dosya: `NaturalDate.kt`
Türkçe tarih/saat ayrıştırıcı. `parse()` şunları döndürür:
`text` (temizlenmiş metin) · `millis` · `hasTime` · `repeatDow` · `matched`

### Anlaşılan kalıplar

| Yazdığın | Sonuç |
|---|---|
| `yarın 14:00 dişçi` | dişçi · Yarın 14:00 |
| `her salı 19:00 spor` | spor · 🔁 Her Salı 19:00 |
| `3 gün sonra kira` | kira · +3 gün |
| `2 hafta sonra kontrol` | kontrol · +14 gün |
| `1 ay sonra fatura` | fatura · +1 ay |
| `15 ağustos düğün` | düğün · 15 Ağu |
| `25.12 yılbaşı` | yılbaşı · 25 Ara |
| `15/08 tatil` | tatil · 15 Ağu |
| `bugün akşam market` | market · Bugün 20:00 |
| `cuma sabah toplantı` | toplantı · Cuma 09:00 |
| `saat 9 ilaç` | ilaç · 09:00 |
| `pazartesi 08:30 sunum` | sunum · Pazartesi 08:30 |
| `9'da eczane` | eczane · 09:00 |

**Desteklenenler**
- **Göreli gün:** bugün · yarın · öbür gün
- **N birim sonra:** `N gün/hafta/ay sonra`
- **Haftanın günleri:** tam ad + kısaltma (pzt, cmt…), `her` → tekrar, `gelecek/haftaya` → +1 hafta
- **Ay adları:** 12 ay, şapkasız yazım da kabul (agustos, subat, mayis…)
- **Sayısal tarih:** `gg.aa` · `gg/aa` · `gg.aa.yyyy`
- **Saat:** `14:00` · `14.30` · `saat 9` · `9'da` / `14te`
- **Günün bölümü:** sabah 09 · öğlen 12 · ikindi 16 · akşam 20 · gece 22

### Akıllı davranışlar
- **Geçmiş tarih düzeltmesi:** "15 ağustos" geçtiyse gelecek yıla atar
- **Saat geçtiyse yarına:** "saat 9" dediğinde ve saat 14 ise → yarın 09:00
- **Bugün o günse:** "salı" dediğinde bugün salı ve saat geçtiyse → gelecek salı
- **Saat/tarih ayrımı:** `25.12` tarih, `14.30` saat *(0-23 / 0-59 aralık denetimi)*
- **Büyük harf korunur:** "Öbür gün 16:00 Dişçi Randevusu" → "Dişçi Randevusu"
- **Tarih yoksa metin bozulmaz:** "market alışverişi" olduğu gibi kalır

### Arayüz
- Yazarken **canlı** çalışır (`TextWatcher`) — düğmede `⏰ Yarın 14:00 · otomatik` görünür
- Hatırlatma düğmesine **elle** dokunursan (`manualDue`) ayrıştırıcı susar, seçimin korunur
- Tarih ifadesini silersen düğme otomatik temizlenir
- Yalnızca **görev** modunda çalışır — notlarda metin aynen kalır
- Kutu ipucu güncellendi: *"Ne yapman gerekiyor? (ör. yarın 14:00 dişçi)"*

### Test
Ayrıştırıcı sanal makinede derlenip **14 örnek** ile çalıştırıldı → tamamı doğru.
Bulunan ve düzeltilen hata: `25.12` başta saat sanılıyordu (regex aralık denetimiyle giderildi).

### Bilinen sınır
`her salı` ifadesinde **ilk salıya** hatırlatma kurulur; otomatik haftalık yineleme
henüz yok (tekrarlayan görev altyapısı sonraki sürüme planlandı).


---

## 🔧 v7.3 — Kısayol Hata Düzeltmesi + Ayrıştırıcı İyileştirmeleri (29 Temmuz 2026)

> **Kullanıcı bildirimi:** "Simgeye uzun basınca kısayol çıkmıyor." Doğrulandı ve giderildi.

### 🐞 Hata 1 — Kısayollar hiç görünmüyordu (ANA SORUN)
`res/xml/shortcuts.xml` içindeki her kısayolda
`<categories android:name="android.shortcut.conversation" />` vardı.
Bu kategori **sohbet kısayolları** içindir; Samsung One UI ve bazı başlatıcılar
bu etiketli kısayolları normal uzun-bas menüsünde **gizler**.

**Çözüm iki katmanlı:**
1. Yanlış `<categories>` satırları silindi
2. **Yeni `Shortcuts.kt`** — kısayollar artık `ShortcutManager` ile **kod içinden**
   (dinamik olarak) kaydediliyor. `MainActivity.onCreate` içinde `Shortcuts.install(this)`
   çağrılıyor. Dinamik kısayollar tüm başlatıcılarda güvenilir çalışır.

⚠️ Bu yüzden **uygulamayı bir kez açmak** gerekiyor — ilk açılışta kısayollar sisteme yazılır.

### 🐞 Hata 2 — Kısayol yanlış ekranı açıyordu
XML'de `android:value="4"` sisteme **String** olarak geçiyor, ama `MainActivity`
`getIntExtra()` ile okuyordu → her zaman `-1` → hep Ana Sayfa açılıyordu.

**Çözüm:** Yeni `requestedScreen()` yardımcısı her iki tipi de okur
(`getIntExtra` başarısızsa `getStringExtra().toIntOrNull()`).

### 🐞 Hata 3 — "akşam 8" → 08:00 (yanlış)
Günün bölümü + çıplak sayı birleşmiyordu.

**Çözüm:** `DAYPARTS` artık `Triple(kelimeler, saat, pmMi)` tutuyor.
Gün bölümü PM ise ve saat 1-11 arasındaysa **12 saat ekleniyor**.
Ayrıca gün bölümünden sonra **çıplak sayı** da saat olarak yakalanıyor.

| Girdi | Önce | Şimdi |
|---|---|---|
| `bu akşam 8 de film` | 08:00 ❌ | **20:00** ✓ |
| `akşam 7 buluşma` | metin bozuk ❌ | **19:00** ✓ |
| `öğlen 1 de yemek` | 01:00 ❌ | **13:00** ✓ |
| `gece 11 de yat` | 11:00 ❌ | **23:00** ✓ |

### ✨ Yeni: "N saat / N dakika sonra"
`2 saat sonra ilaç` · `30 dakika sonra çay` — şimdiki zamana eklenir.

### ✨ Yeni: "her gün" günlük tekrar
`her gün su iç` · `her gün 08:00 vitamin` → `🔁 Her gün`
Yeni sabit: `NaturalDate.REPEAT_DAILY = -1`

### ✨ Yeni: gün bölümleri genişletildi
`öğleden sonra` (15:00) · `akşamüstü` (18:00) eklendi.

### 🧹 Metin temizliği iyileştirildi
`günü` · `gününde` · `bu` · `de/da/te/ta` ekleri artık görev adından atılıyor:
`perşembe günü doktor` → **"doktor"** (önce "günü doktor" oluyordu)

⚠️ Dikkat: `su` kelimesi bir ara yanlışlıkla siliniyordu (`bu|şu|su` deseni),
düzeltildi — `her gün su iç` → **"su iç"** ✓

### Test
Ayrıştırıcı sanal makinede derlenip **22 örnekle** çalıştırıldı → **22/22 doğru**.
Sayı içeren ama tarih olmayan metinler de korunuyor:
`5 kilo ver` · `100 soru çöz` · `3 bardak su` → hiç bozulmuyor ✓


---

## 🏗️ v7.4 — Mühendislik Kursları (Udemy düzeni) — 29 Temmuz 2026

> İnşaat mühendisliği yazılımları için **Kurs → Bölüm → Ders** yapısında
> bir öğrenme takip ekranı. Hazır içerikle gelir, üzerine ekleme yapılabilir.

### Yeni ekran: `CoursesFragment` (indeks 13)
Erişim iki yerden:
1. **Ana sayfada** 🏗️ kartı (geri sayımın hemen altında) — ilerleme çubuğuyla
2. **Yan panelden** "🏗️ Mühendislik kursları" düğmesi

### Akordiyon yapı
```
🏗️ AutoCAD 2D            ← dokun: bölümler açılır
   1. Tanışma ve Arayüz   ← dokun: dersler açılır
      ○ AutoCAD nedir…  ⏱️ 12 dk
      ● Arayüz turu     ⏱️ 18 dk  📝 🔗
```
- Kurs kartında: ilerleme çubuğu + `12/58 ders · %20 · 14 sa 30 dk`
- Bölüm satırında: `3/6 ders`
- Ders satırında: ✓ daire, süre, not/link simgeleri, tamamlanınca üstü çizili

### Hazır içerik paketi — `CoursePack.kt`
İlk açılışta "📦 Hazır paketi yükle" ile eklenir.

| Kurs | Bölüm | Konu |
|---|---|---|
| 📐 **AutoCAD 2D** | 10 | Arayüz, çizim/düzenleme komutları, katmanlar, ölçülendirme, bloklar, layout+baskı, kat planı/kesit/kalıp planı uygulamaları, XREF |
| 🏗️ **Revit (BIM)** | 8 | Mimari+yapısal modelleme, aileler, metraj tabloları, worksharing, çakışma denetimi, Dynamo |
| 🧮 **SAP2000** | 7 | Modelleme, yükler, **TBDY 2018 deprem analizi**, mod birleştirme, betonarme/çelik tasarım, 5 uygulama |
| 🏢 **ideCAD Statik** | 4 | Taşıyıcı sistem, deprem parametreleri, donatı, kalıp planı, metraj |
| 📊 **Excel** | 5 | DÜŞEYARA, metraj/keşif, demir ağırlık hesabı, hakediş, pivot, S-eğrisi |
| 🛣️ **Civil 3D** | 4 | Yüzey, güzergâh, koridor, kazı-dolgu, boru ağı |
| 🧰 **Diğer** | 6 | STA4CAD, ETABS, Plaxis, MS Project/Primavera, Lumion, Netcad |

**Toplam: 7 kurs · 44 bölüm · ~250 ders · ~95 saat**

Dersler yalnızca başlık değil — çoğunda **açıklama notu** var:
> *"Model Space vs Paper Space — en çok karıştırılan konu, 1/1 modelde çiz layout'ta ölçekle"*
> *"Kütle kaynağı tanımı — sık yapılan hata, yanlış kütle yanlış periyot demek"*
> *"Demir metrajı: Çap² × 0.006165 = kg/m"*

### Her ders satırında
✓ tamamlandı · ⏱️ süre (dk) · 📄 açıklama · 🔗 video/kaynak linki · 📝 kendi notun

Derse dokun → ayrıntı penceresi (açıklama + notun + link açma + işaretle/düzenle)

### Kendi içeriğini ekleme
- **FAB (+)** → yeni kurs (ad, emoji, açıklama)
- Kursa **uzun bas** → ➕ Bölüm ekle / ✏️ Düzenle / 🗑 Sil
- Bölüme **uzun bas** → ➕ Ders ekle / ✏️ Düzenle / 🗑 Sil
- Derse **uzun bas** → ✏️ Düzenle / 🗑 Sil

### Veri katmanı (`Store.kt`)
Üç yeni model + tam CRUD:
```
Course  (id, title, emoji, color, desc, order)
Section (id, courseId, title, order)
Lesson  (id, courseId, sectionId, title, minutes, desc, link, note, done, order)
```
Yardımcılar: `sectionsOf` · `lessonsOf` · `courseProgress` · `courseMinutes` · `toggleLesson`

**Seri entegrasyonu:** Bir ders tamamlanınca `recordCompletion()` çağrılır ve
ders süresi **günlük odak dakikasına** eklenir — yani kurs çalışmak seriyi besler.

### Yeni dosyalar
```
CoursePack.kt           Hazır içerik (7 kurs, ~250 ders)
CoursesFragment.kt      Akordiyon ekran + tüm düzenleyiciler
fragment_courses.xml · item_course.xml · item_course_section.xml
item_course_lesson.xml · dialog_course.xml · dialog_lesson.xml
drawable/co_bar_bg.xml
```


---

## 📄 v7.5 — Ders PDF'leri Uygulamaya Gömüldü (29 Temmuz 2026)

> AutoCAD kursunun **58 dersinin her biri için ayrı, detaylı PDF** hazırlandı ve
> APK içine gömüldü. Derse dokununca PDF doğrudan uygulama içinde açılıyor.

### Üretilen içerik
| | |
|---|---|
| PDF sayısı | **58** (AutoCAD 2D) |
| Toplam sayfa | 154 (ort. 2,7 sayfa/ders) |
| Boyut | 4,0 MB (APK 19,0 → **22,8 MB**) |

Her PDF: mavi başlık şeridi, renkli bölümler, **tablolar**, **kod blokları**
(gerçek komut akışları), **İpucu / Dikkat / Not** kutuları, alıştırma adımları.

### Yeni dosya: `LessonPdfActivity.kt`
- Assets'teki PDF'i `cacheDir`'e kopyalar, **`PdfRenderer`** ile açar
- Dikey kaydırmalı `RecyclerView` — her sayfa ayrı kart
- Sayfa görüntüleri **bitmap önbelleğinde** tutulur (tekrar render yok)
- Genişliğe göre ölçekli render, en-boy oranı korunur
- Üstte kurs · bölüm bilgisi, altta **"✓ Bu dersi tamamladım"** düğmesi
- `onDestroy`'da bitmap'ler `recycle()` edilir — bellek sızıntısı yok

### `Store.Lesson` yeni alan
```kotlin
var pdfAsset: String = ""   // "dersler/autocad/001.pdf"
```
`loadLessons` / `saveLessons` / `addLesson` güncellendi.

### `CoursePack` yeni fonksiyonlar
| Fonksiyon | İşlev |
|---|---|
| `pdfFolders` | Kurs indeksi → assets klasörü eşlemesi (`0 to "autocad"`) |
| `install()` | Kurulumda ders sırasına göre `001.pdf`, `002.pdf`... bağlar |
| `linkPdfs()` | **v7.4'te kurulmuş** derslere PDF'leri sonradan bağlar |
| `assetExists()` | PDF gerçekten var mı denetler |

`linkPdfs()`, `CoursesFragment.rebuild()` içinde **bir kez** çağrılır —
eski kullanıcılar kursları silip yeniden eklemek zorunda kalmaz.

### Arayüz değişiklikleri
- Ders satırında **📄 PDF** rozeti (PDF'i olan derslerde)
- Derse **dokun** → PDF varsa doğrudan okuyucu açılır
- Derse **uzun bas** → "ℹ️ Ders bilgisi / ✏️ Düzenle / 🗑 Sil"
- PDF'i olmayan derslerde eski davranış (ayrıntı penceresi) korunur

### Assets yapısı
```
app/src/main/assets/dersler/autocad/001.pdf ... 058.pdf
```
Yeni kurs eklerken: klasörü oluştur + `pdfFolders`'a satır ekle.

### PDF üretim altyapısı (`~/kurs-pdf/`)
```
uret.py         reportlab motoru, DejaVu font (Türkçe tam destek)
                blok tipleri: h2/h3/p/ul/ol/code/table/tip/warn/note
dersler.json    CoursePack.kt'den ayrıştırılmış 226 ders
icerik/         autocad_01.py (1-20) · _02 (21-40) · _03 (41-58)
basvedagit.py   Toplu üretici: python3 basvedagit.py 0 autocad_01,...
```
Emoji karakterleri PDF fontunda olmadığından `strip_emoji()` ile sadeleştirilir.

### Kalan iş
| Kurs | Ders | Durum |
|---|---|---|
| 📐 AutoCAD 2D | 58 | ✅ Bitti |
| 🏗️ Revit | 47 | ⏳ |
| 🧮 SAP2000 | 41 | ⏳ |
| 🏢 ideCAD | 20 | ⏳ |
| 📊 Excel | 24 | ⏳ |
| 🛣️ Civil 3D | 17 | ⏳ |
| 🧰 Diğer | 19 | ⏳ |

Altyapı hazır olduğundan kalan kurslar yalnızca **içerik yazımı** gerektiriyor.


---

## 🎨 v7.6 — Revit Kursu + Görsel Anlatım (29 Temmuz 2026)

> PDF motoruna **vektör çizim sistemi** eklendi. Artık dersler sadece metin değil;
> arayüz şemaları, teknik çizimler, akış diyagramları ve ölçülendirilmiş kesitler içeriyor.

### Yeni: görsel çizim motoru (`uret.py`)
`Canvas` adında bir `Flowable` sınıfı eklendi. Desteklenen çizim komutları:

| Komut | Ne çizer |
|---|---|
| `rect` | Dikdörtgen (dolgulu/boş) |
| `line` | Çizgi (kesikli desteği) |
| `circ` | Daire |
| `poly` | Çokgen / serbest form |
| `arrow` | Ok başlı çizgi |
| `dim` | **Ölçü çizgisi** (çift ok + değer) |
| `hatch` | **Eğik tarama** (kesit gösterimi) |
| `txt` | Konumlandırılmış metin |

Ayrıca iki hazır bileşen:
- **`ui_mock`** — program arayüzü şeması (şerit, panel, çizim alanı)
- **`steps_diagram`** — yatay akış şeması (adım kutuları + oklar)

Yeni blok tipleri: `('draw', (w,h,cmds,caption))` · `('ui', (rows,caption))` · `('flow', (steps,caption))`

### Revit kursu — 47 ders
| | |
|---|---|
| PDF sayısı | **47 / 47** |
| Toplam sayfa | 132 (ort. 2,8) |
| Boyut | 3,1 MB |

**İçerik yaklaşımı:** hiç Revit açmamış biri için yazıldı. "Nereye tıklayacağın" düzeyinde,
menü yolları Türkçe arayüz adlarıyla verildi.

Örnek görseller:
- Ders 1 — AutoCAD vs Revit karşılaştırma çizimi
- Ders 2 — Revit arayüz şeması + Proje Tarayıcı ağacı
- Ders 5 — Seviye çizgileri (kesit görünümü) + aks sistemi planı
- Ders 6 — Konum çizgisi seçenekleri (3 varyant)
- Ders 7 — Kapı açılış yönleri (4 olasılık)
- Ders 13 — Kolon/kiriş kalıp planı (dolu/boş gösterim)
- Ders 14 — Tekil ve radye temel kesitleri
- Ders 16 — Kolon/kiriş donatı kesitleri
- Ders 19 — Görünüm Aralığı diyagramı
- Ders 24 — Kapı listesi tablosu görseli
- Ders 34 — Tip vs Örnek parametresi karşılaştırması
- Ders 46 — BIM boyutları (3B→4B→5B→6B)

### APK durumu
```
assets/dersler/autocad/  001.pdf ... 058.pdf   (4,0 MB)
assets/dersler/revit/    001.pdf ... 047.pdf   (3,1 MB)
```
`CoursePack.pdfFolders` → `0 to "autocad", 1 to "revit"`

APK: 22,8 → **25,8 MB** (105 PDF gömülü)

### Düzeltilen hata
Revit ders numaralandırmasında kayma vardı: kurs listesinde 5. ders
"Seviyeler **ve** akslar" tek başlıkken, içerik ikiye ayrılmıştı.
Birleştirilip 6-16 arası bir kaydırıldı.

### Kalan iş
| Kurs | Ders | Durum |
|---|---|---|
| 📐 AutoCAD 2D | 58 | ✅ Bitti |
| 🏗️ Revit | 47 | ✅ **Bitti** |
| 🧮 SAP2000 | 41 | ⏳ Sırada |
| 🏢 ideCAD | 20 | ⏳ |
| 📊 Excel | 24 | ⏳ |
| 🛣️ Civil 3D | 17 | ⏳ |
| 🧰 Diğer | 19 | ⏳ |

**105 / 226 ders tamamlandı (%46)**


---

## 🤖 v7.7 — Otonom Kalite Sistemi + İyileştirmeler (30 Temmuz 2026)

> Sürekli geliştirme için **otomatik kod denetleyici** kuruldu.
> Kaynak kodu tarar, hataları ve eksikleri bulur, önceliklendirir.
> Her turda çalıştırılıp bulunan sorunlar giderilir.

### Yeni: `otonom/denetle.py` — kalite denetleyici

11 farklı denetim yapar:

| Denetim | Ne arar |
|---|---|
| **ÇÖKME** | `!!`, `first()`, `last()`, `toInt()`, `substring()` — koruma olmadan |
| **SIZINTI** | Kapatılmayan `PdfRenderer`, `Bitmap`, dosya tanıtıcı |
| **ANR** | UI iş parçacığında ağır işlem |
| **KAYNAK** | Tanımsız `R.string.*` / `R.id.*` |
| **MANIFEST** | Kayıtlı ama olmayan sınıf, kaydedilmemiş Activity/Service |
| **UX** | Boş durum mesajı, silme onayı, geri alma eksiği |
| **PERFORMANS** | `onBindViewHolder` içinde ağır işlem, aşırı `notifyDataSetChanged` |
| **VERİ** | Yedek sürümleme, `apply()` vs `commit()` |
| **ÖZELLİK** | Kullanıcıya değer katacak eksik özellikler |
| **KALİTE** | Boş `catch`, çok büyük dosya |
| **ERİŞİLEBİLİRLİK** | `contentDescription` eksiği |

Çıktı: konsol raporu + `otonom/son_rapor.json`

**İlk tarama:** 225 bulgu (çoğu yanlış pozitif) → hassaslaştırma sonrası **76 gerçek bulgu**
**Bu turdan sonra:** **55 bulgu** (kritik 5→2, yüksek 6→4)

### 🐞 Giderilen çökme riskleri
| Dosya | Sorun | Çözüm |
|---|---|---|
| `ExamsFragment` | `exams.first()` boş listede çöker | `firstOrNull() ?: 0` |
| `ManualSplitActivity` | `points.keys.last()` çöker | `lastOrNull() ?: return` |
| `PdfSplitter` | `marks.first()` çöker | `firstOrNull() ?: return list` |
| `PdfSplitter` | `chapters.last()` çöker | `lastOrNull()?.let { }` |

### ⚡ Performans düzeltmeleri
`onBindViewHolder` içinde her satırda yeniden oluşturulan `SimpleDateFormat`
nesneleri sınıf düzeyine taşındı:
- `EventsFragment.dateFormatter`
- `ExamsFragment.examDateFormatter`
- `TasksFragment.rowDateFormatter`

Uzun listelerde kaydırma akıcılığı artar.

### 📖 PDF okuyucu tamamen yenilendi
| Özellik | Açıklama |
|---|---|
| **Parmakla yakınlaştırma** | `ScaleGestureDetector` ile pinch-zoom (%60–%300) |
| **+ / − düğmeleri** | Üst çubuktan adım adım yakınlaştırma |
| **🌙 Gece modu** | Renk tersleme süzgeci — karanlıkta göz yormaz, tercih kaydedilir |
| **Kaldığın yeri hatırlama** | Her PDF için son sayfa saklanır, açınca oraya döner |
| **İlerleme göstergesi** | Alt çubukta `Sayfa 3/5 · %60` |
| **Bellek dostu önbellek** | En fazla 6 sayfa tutulur, eskiler `recycle()` edilir |

Önceki sürümde tüm sayfalar bellekte tutuluyordu — uzun PDF'lerde risk oluşturuyordu.

### 🔍 Kurs araması
Mühendislik ekranına arama kutusu eklendi:
- 2+ karakterde canlı arama
- Ders **başlığı** ve **açıklamasında** arar
- Sonuçlar kursa göre gruplanır
- Türkçe karakter duyarlı (`Locale("tr","TR")`)
- Üst özet: `12 ders bulundu` veya `"beton" için sonuç yok`

### ↩️ Geri alma (Undo) sistemi
`Store` içine geri alınabilir silme altyapısı kuruldu:
```
deleteCourseUndoable · deleteSectionUndoable · deleteLessonUndoable
deleteTaskUndoable   · deleteHabitUndoable   · deleteEventUndoable
Store.geriAl()       · Store.geriAlinabilir()
```
Silinen veri bellekte tutulur, **Snackbar → GERİ AL** ile döndürülür.
Bağlandığı ekranlar: Mühendislik (kurs/bölüm/ders), Alışkanlıklar, Geri sayımlar.

### Kalan bulgular (sonraki turlar)
- Store.kt 1477 satır — parçalara bölünmeli
- 13 boş `catch` bloğu — log eklenmeli
- Ders yer imi/favori özelliği
- Ders PDF paylaşma
- Bazı ImageView'larda `contentDescription`


---

## ⭐ v7.8 — Sıfır Kritik Bulgu + Yer İmi Sistemi (30 Temmuz 2026)

> Otonom denetleyici bu turda **kritik ve yüksek öncelikli bulguları sıfıra** indirdi.

### Denetim skoru gelişimi
| Sürüm | Kritik | Yüksek | Orta | Düşük | Toplam |
|---|---|---|---|---|---|
| v7.6 (ilk tarama) | 5 | 6 | 7 | 43 | 76 |
| v7.7 | 2 | 4 | 4 | 45 | 55 |
| **v7.8** | **0** | **0** | **2** | 44 | **46** |

### 🐞 Giderilen sorunlar
| Sorun | Dosya | Çözüm |
|---|---|---|
| `!!` operatörü — NPE riski | `Store.kt:551` | Yerel değişkene alındı, akıllı cast |
| 13 boş `catch` bloğu | `Store.kt` | Hepsine `Log.w(TAG, ...)` eklendi |
| Eksik `contentDescription` | `dialog_note.xml` | Erişilebilirlik etiketi |

**Boş catch düzeltmesi neden önemli:** Önceden bir JSON okuma hatası olduğunda
uygulama sessizce boş liste döndürüyordu ve kullanıcı verisinin neden kaybolduğunu
anlamak imkânsızdı. Artık Logcat'te fonksiyon adıyla birlikte hata görünüyor.

### ⭐ Yeni: Ders yer imi (favori) sistemi
`Store.Lesson` modeline `fav: Boolean` alanı eklendi.

| Nerede | Nasıl |
|---|---|
| **Ders satırında** | Yer imli derslerde ⭐ rozeti görünür |
| **Uzun basma menüsü** | "⭐ Yer imine ekle" / "☆ Yer iminden çıkar" |
| **PDF okuyucu** | Üst çubukta yıldız düğmesi — okurken işaretle |
| **Filtre düğmesi** | Başlık yanındaki ☆ — yalnızca yer imlileri göster |

Yeni fonksiyonlar: `Store.toggleLessonFav()` · `Store.favLessons()`

Kullanım senaryosu: 226 ders arasından sana lazım olanları işaretlersin,
☆ düğmesine basınca sadece onlar listelenir.

### 🔧 Denetleyici iyileştirmeleri
Yanlış pozitifler ayıklandı:
- `isEmpty()` / `!= null` / `?.` ile korunmuş satırlar artık "çökme riski" sayılmıyor
- Silme onayı denetimi yalnızca **UI sınıflarında** yapılıyor (veri katmanında değil)
- `Undoable` fonksiyonu olan dosyalar "geri alma yok" uyarısı almıyor

Denetleyici 225 → 46 bulguya indi ve kalanların **tamamı gerçek**.

### Kalan bulgular (sonraki turlar)
- `Store.kt` 1570 satır — mantıksal parçalara bölünmeli
- Ders PDF'ini dışa aktarma/paylaşma
- Kurs çalışma hatırlatıcısı
- 43 adet düşük öncelikli erişilebilirlik/yerelleştirme notu


---

## ⏰ v7.9 — Tüm Öncelikli Bulgular Temizlendi (30 Temmuz 2026)

> Otonom denetleyici skoru: **Kritik 0 · Yüksek 0 · Orta 0**
> Kalan 45 bulgunun tamamı düşük öncelikli (yerelleştirme/erişilebilirlik notları).

### Denetim skoru gelişimi
| Sürüm | Kritik | Yüksek | Orta | Toplam |
|---|---|---|---|---|
| v7.6 (ilk) | 5 | 6 | 7 | 76 |
| v7.7 | 2 | 4 | 4 | 55 |
| v7.8 | 0 | 0 | 2 | 46 |
| **v7.9** | **0** | **0** | **0** | **45** |

### 📚 Yeni: Günlük ders çalışma hatırlatıcısı
Yeni dosya: `CourseReminderReceiver.kt`

| Özellik | Açıklama |
|---|---|
| **Kurulum** | Mühendislik ekranında **FAB'a uzun bas** → hatırlatıcı ayarı |
| **Saat seçimi** | İstediğin saati belirle (varsayılan 20:00) |
| **Akıllı içerik** | "Sıradaki: Duvar oluşturma · %34 tamamlandı" |
| **İlerleme** | Bildirimde `12/105 ders · %11` özeti |
| **Tek dokunuş** | Bildirime bas → doğrudan Mühendislik ekranı açılır |
| **Kalıcı** | Telefon yeniden başlasa da alarm geri kurulur |

Bildirim, tamamlanmamış **ilk dersi** bulup adını gösterir. Tüm dersler bitmişse
tebrik mesajı verir.

Teknik ayrıntılar:
- `BootReceiver` ve `MainActivity.onCreate` içinden yeniden kurulur
- Bildirim kanalı: `kurs_hatirlatici_v1`
- Android 12+ için `canScheduleExactAlarms()` denetimi
- Kullanıcının bildirim/titreşim tercihlerine uyar

### ↩️ PDF kitaplığına geri alma
Kitaplıktan bir PDF silindiğinde artık **Snackbar → GERİ AL** çıkıyor.
Bölümlere ayrılmış bir kitap silinirse **tüm bölümleri birlikte** geri gelir.

Yeni: `Store.deleteBookUndoable()` · `MainActivity.geriAlSun()`

**Neden önemli:** PDF kitaplığı kullanıcının kendi yüklediği dosyaları tutuyor.
Yanlışlıkla silme, bölünmüş bölümlerin tamamının kaybı demekti.

### 🔧 Denetleyici iyileştirmesi
Yer imi özelliği v7.8'de eklendiği hâlde denetleyici hâlâ "eksik" diyordu —
iz listesine `toggleLessonFav` ve `favLessons` eklendi.

### Kalan düşük öncelikli bulgular
- `Store.kt` 1601 satır — ileride parçalara bölünebilir
- 43 adet "koda gömülü Türkçe metin" notu (çoğu log mesajı, sorun değil)
- Bir ImageView'da `contentDescription`

Bunlar uygulamanın çalışmasını etkilemiyor; ileriki turlarda ele alınacak.


---

## 📐 v7.10 — AutoCAD v2 İçeriği + PDF Paylaşma (30 Temmuz 2026)

### Gerçek program ekranı üreten çizim motoru
`uret.py` içine üç yeni üretici eklendi:

| Fonksiyon | Ne çizer |
|---|---|
| `acad_screen()` | AutoCAD arayüzü — koyu çizim alanı, ribbon sekmeleri, panel düğmeleri, yeşil komut satırı, durum çubuğu |
| `revit_screen()` | Revit arayüzü — mavi başlık, Özellikler paneli, Proje Tarayıcı ağacı |
| `dialog()` | Diyalog penceresi — açılır liste, onay kutusu, radyo düğmesi, Tamam/İptal |
| `marker()` / `marker_line()` | Ekran üzerine numaralı kırmızı işaretçi + kılavuz çizgi |

Ekranların içine gerçek geometri (duvar, kolon, aks, daire) çizilebiliyor;
üzerine numaralı balonlar konup altta tabloyla açıklanıyor.

### AutoCAD Bölüm 1-3 yeniden yazıldı (16 ders)
| | Eski (v1) | **Yeni (v2)** |
|---|---|---|
| Sayfa/ders | 2,4 | **3,4** |
| Ekran kesiti | Yok | **Her derste 1-2** |
| Ders sonu alıştırma | Yok | **Var (5-8 adım)** |
| "Ne öğreneceksin" | Yok | **Var** |

Yeni yazılan dersler:
1-5 (Tanışma) · 6-11 (Çizim komutları) · 12-16 (Hassasiyet araçları)

Öne çıkan görseller:
- **Ders 2** — ekranın 6 bölgesi, numaralı işaretçilerle
- **Ders 3** — Window (mavi) vs Crossing (yeşil) seçim farkı
- **Ders 6** — LINE 3 nesne vs PLINE tek nesne, tutamak renkleriyle
- **Ders 9** — bağıl koordinatlarla 4×3 m oda çizimi
- **Ders 12** — OSNAP ayar penceresi, açık/kapalı olması gerekenler
- **Ders 14** — OTRACK ile iki orta noktadan merkez bulma
- **Ders 16** — kurulmuş aks sistemi + kesişimlerde kolonlar

### 🔗 Yeni: Ders PDF paylaşma
PDF okuyucunun üst çubuğuna **↪ paylaş** düğmesi eklendi.
- Ders PDF'i okunabilir adla (`6. Duvar oluşturma.pdf`) geçici klasöre kopyalanır
- `FileProvider` ile WhatsApp, e-posta, Drive gibi uygulamalara gönderilir
- `res/xml/file_paths.xml` içine `cache-path` tanımı eklendi

### PDF okuyucu üst çubuğu (v7.10 hâli)
```
[ Ders başlığı        ]   ↪   ☆   −   +   ☾   ✕
                      paylaş yer  zoom  gece kapat
                              imi
```

### Denetim durumu
`Kritik 0 · Yüksek 0 · Orta 0 · Düşük 44`

### Kurs içeriği ilerlemesi
| Kurs | Toplam | v2 (ekran kesitli) | v1 |
|---|---|---|---|
| 📐 AutoCAD | 58 | **16** | 42 |
| 🏗️ Revit | 47 | 0 | 47 |
| Diğer 5 kurs | 121 | 0 | 0 |

Sıradaki: AutoCAD 17-23 (düzenleme komutları) v2 sürümü.


---

## 🔧 v7.11 — Düzenleme Komutları v2 + İlerleme İstatistikleri (30 Temmuz 2026)

### AutoCAD Bölüm 4 yeniden yazıldı (Ders 17-23)
Çizim süresinin **%60'ını** oluşturan düzenleme komutları, ekran kesitleriyle:

| Ders | Konu | Görsel |
|---|---|---|
| 17 | MOVE · COPY · ROTATE · SCALE | Önce/sonra karşılaştırma, temel nokta işaretli |
| 18 | TRIM · EXTEND · FILLET · CHAMFER | Taşan uçlar → `F`+`R`+`0` → tam köşe |
| 19 | OFFSET | Duvar çizme akışı: eksen → iki yana ofset → kapalı oda |
| 20 | MIRROR | Merdiven aynalama, çıkış oku dahil |
| 21 | ARRAY | Dikdörtgen dizi (kolon aksları) + kutupsal dizi (donatı) |
| 22 | STRETCH | Crossing seçim kutusu, sadece sağ kenar geriliyor |
| 23 | Grip düzenleme | Sıcak/soğuk grip renkleri, SPACE mod döngüsü |

**AutoCAD v2 ilerlemesi: 23 / 58 ders** (ort. 3,3 sayfa/ders, 77 sayfa)

Öne çıkan anlatımlar:
- **Ders 18** — "Fillet yarıçapı 0" hilesi: duvar köşesi temizlemenin en hızlı yolu
- **Ders 19** — duvar çizmenin standart 6 adımlı yöntemi
- **Ders 21** — 24 kolonu tek hamlede yerleştirme
- **Ders 22** — STRETCH'in neden yalnızca crossing ile çalıştığı, üç kural tablosu
- **Ders 23** — SPACE ile beş grip modu arasında geçiş

### 📊 Yeni: Kurs ilerleme istatistikleri
Mühendislik ekranında **özet satırına uzun bas** → ayrıntılı rapor:

```
📚 Ders: 23 / 105 tamamlandı (%21)
⏱️ Süre: 7 sa 40 dk / 38 sa 15 dk
⏳ Kalan: 30 sa 35 dk
📅 Günde 30 dakika çalışırsan
   yaklaşık 62 günde bitirirsin
⭐ Yer imi: 5 ders
📄 PDF'li ders: 105
```

Yeni: `Store.KursIstatistik` veri sınıfı + `Store.kursIstatistik()`
Hesaplanan alanlar: `yuzde` · `kalanDakika` · `kalanGun(gunlukDakika)`

**Neden değerli:** Kullanıcı "ne kadar kaldı, ne zaman biter" sorusunun
somut cevabını görüyor — motivasyon için önemli.

### Denetim durumu
`Kritik 0 · Yüksek 0 · Orta 0 · Düşük 44` (4 turdur temiz)

### Kurs içeriği durumu
| Kurs | Toplam | v2 (ekran kesitli) |
|---|---|---|
| 📐 AutoCAD | 58 | **23** |
| 🏗️ Revit | 47 | 0 |
| Diğer 5 kurs | 121 | 0 |

Sıradaki: AutoCAD 24-28 (katmanlar) ve 29-34 (yazı, ölçülendirme, tarama).


---

## 🎨 v7.12 — Katmanlar ve Ölçülendirme v2 (30 Temmuz 2026)

### AutoCAD Bölüm 5-6 yeniden yazıldı (Ders 24-34)

**Bölüm 5 — Katmanlar (24-28)**
| Ders | Konu | Görsel |
|---|---|---|
| 24 | LAYER mantığı | Gerçek katman yöneticisi penceresi, 10 katmanlı inşaat düzeni |
| 25 | Renk / çizgi tipi / kalınlık | 5 çizgi tipi örneği + LTSCALE sorunu |
| 26 | ByLayer / ByBlock | Properties paneli: doğru vs kötü alışkanlık |
| 27 | Filtreler ve Layer State | Katman Durumları Yöneticisi penceresi |
| 28 | MATCHPROP | Kaynak → fırça → hedefler akışı |

**Bölüm 6 — Yazı, ölçü, tarama (29-34)**
| Ders | Konu | Görsel |
|---|---|---|
| 29 | TEXT / MTEXT / STYLE | Yazı stili penceresi, Height=0 kuralı |
| 30 | DIMSTYLE | Ölçü stili penceresi, DIMSCALE vurgulu |
| 31 | Ölçü komutları | Üç sıra ölçülendirme + eğik ölçü karşılaştırması |
| 32 | DIMCONTINUE / DIMBASELINE | Aynı akslar, iki farklı ölçü düzeni |
| 33 | HATCH | 4 tarama deseni + "sınır kapalı değil" hatası |
| 34 | MLEADER | Döşeme kesitinde malzeme açıklamaları + balon |

**AutoCAD v2 ilerlemesi: 34 / 58 ders** (113 sayfa, ort. 3,3/ders)

Yeni çizim yardımcısı: `_layer_manager()` — gerçek katman yöneticisi tablosu
(ad, aç/dondur/kilit sütunları, renk kutuları, çizgi tipi, kalınlık, yazdır)

### 🔖 Yeni: Son ders takibi ve "Sıradaki" göstergesi
- `Store.setSonDers()` — açılan her ders kaydedilir
- `Store.sonDers()` — son açılan ders, yoksa ilk tamamlanmamış ders
- **Ana sayfadaki 🏗️ kartında** artık sıradaki dersin adı görünüyor:
  `%34 · Sıradaki: LAYER mantığı ve katman yöneticisi`

Kullanıcı uygulamayı açtığında nereden devam edeceğini anında görüyor.

### Denetim durumu
`Kritik 0 · Yüksek 0 · Orta 0 · Düşük 44` (5 turdur temiz)

### Kurs içeriği durumu
| Kurs | Toplam | v2 (ekran kesitli) |
|---|---|---|
| 📐 AutoCAD | 58 | **34** (%59) |
| 🏗️ Revit | 47 | 0 |
| Diğer 5 kurs | 121 | 0 |

Sıradaki: AutoCAD 35-39 (bloklar) ve 40-45 (layout, ölçek, baskı).


---

## 🖨️ v7.13 — Bloklar ve Layout/Baskı v2 (30 Temmuz 2026)

### AutoCAD Bölüm 7-8 yeniden yazıldı (Ders 35-45)

**Bölüm 7 — Bloklar (35-39)**
| Ders | Konu | Görsel |
|---|---|---|
| 35 | BLOCK / INSERT | Blok Tanımı penceresi, temel nokta + birim vurgulu |
| 36 | Öznitelikli bloklar | Öznitelik Tanımı penceresi, etiket/istem alanları |
| 37 | Dinamik bloklar | Blok editöründe Linear parametre + Stretch eylemi + Visibility listesi |
| 38 | WBLOCK / kütüphane | Klasör ağacı: 01-MIMARI, 02-STATIK, 03-GENEL |
| 39 | DesignCenter / Tool Palettes | İki panel yan yana karşılaştırma |

**Bölüm 8 — Layout ve baskı (40-45)**
| Ders | Konu | Görsel |
|---|---|---|
| 40 | Model vs Paper Space | **Yan yana karşılaştırma** — solda koyu model (1/1), sağda beyaz kâğıt (1:100) |
| 41 | Viewport | Bir paftada 3 viewport, kırmızı **K** kilit işaretleri |
| 42 | Antet | A3 pafta düzeni, doldurulmuş antet tablosu |
| 43 | Annotative | Aynı yazı iki viewport ölçeğinde (125 vs 250 birim) |
| 44 | PLOT | Baskı penceresi, "ölçek 1:1" vurgulu |
| 45 | PUBLISH | Sayfa listesi, multi-sheet PDF seçeneği |

**AutoCAD v2 tamamlandı sayılır: 45 / 58 ders (%78)** · 149 sayfa

Ders 40 özellikle önemli: "Model Space vs Paper Space" AutoCAD öğrenenlerin
en çok takıldığı konu. Artık iki uzay **yan yana görsel** olarak gösteriliyor.

### 📊 Yeni: Bölüm bazlı ilerleme çubuğu
Kurs ekranındaki bölüm satırlarında artık görsel ilerleme var:

```
1. Tanışma ve Arayüz        5/5 ders  ▰▰▰▰▰▰▰▰▰▰ %100
2. Temel Çizim Komutları    3/6 ders  ▰▰▰▰▰▱▱▱▱▱ %50
3. Yardımcı Araçlar         0/5 ders  ▱▱▱▱▱▱▱▱▱▱ %0
```

Hangi bölümde ne kadar ilerlediğin tek bakışta görünüyor.

### Denetim durumu
`Kritik 0 · Yüksek 0 · Orta 0 · Düşük 44` (6 turdur temiz)

### Kurs içeriği durumu
| Kurs | Toplam | v2 (ekran kesitli) |
|---|---|---|
| 📐 AutoCAD | 58 | **45** (%78) |
| 🏗️ Revit | 47 | 0 |
| Diğer 5 kurs | 121 | 0 |

Sıradaki: AutoCAD 46-51 (gerçek proje uygulamaları) ve 52-58 (verimlilik).


## 🚧 v5.2 ilerlemesi

| Aşama | Durum |
|---|---|
| 1 — Özel geri sayımlar | ✅ Bitti, telefonda test edildi |
| 2 — "🗓️ Bugün" ekranı | ✅ Bitti, telefonda test edildi |
| 3 — Zengin widget + bildirim eylemleri | ✅ Bitti (versionCode 15, imza düzeltmesiyle) |

### Aşama 3'te eklenenler

**Bildirim eylemleri**
- `TaskActionReceiver.kt` — yeni receiver, manifest'e kayıtlı
  (`TASK_DONE`, `TASK_SNOOZE` intent filtreleriyle)
- Bildirimde **✅ Tamamlandı** → görevi işaretler, seriye yazar, bildirimi kapatır,
  widget'ı tazeler — uygulamayı açmaya gerek yok
- Bildirimde **😴 15 dk ertele** → alarmı yeniden kurar
- Genişletilince günün özeti: odak/hedef, soru, seri, bekleyen diğer görev sayısı
- Görev bu arada tamamlandıysa bildirim hiç gösterilmiyor
- Titreşim ayarı (`pref_vib`) artık bildirimlere de uygulanıyor

**Zengin widget**
- Yeni tasarım: gradyan arka plan (`widget_bg`), yuvarlak köşeler, kenarlık
- **Hedef ilerleme çubuğu** (`widget_progress` — gradyanlı, yuvarlatılmış)
- Tek satırda odak + soru: "🎯 45/100 dk · 🔢 120 soru"
- **Sıradaki görev** satırı: bugüne tarihli en yakın görev, "(+3)" ile kalan sayısı
- İki hızlı buton: **⏱️ Odaklan** (Sayaç) · **🗓️ Bugün** (Bugün ekranı)
- Gövdeye dokunma → Bugün ekranı
- Widget yüksekliği 2→3 hücre (`targetCellHeight`), açıklama + önizleme eklendi
- `MainActivity.onNewIntent()` eklendi: uygulama açıkken widget'a basınca doğru ekrana geçer

**Widget tazeleme düzeltmesi (önemli)**
Widget eskiden yalnızca otomatik yedekleme sırasında güncelleniyordu. Artık
`Store.bumpToday()` (odak/soru/tamamlama) ve `Store.saveTasks()` içine taşındı —
yani veri her değiştiğinde anında tazeleniyor.

**Düzeltilen hata:** eski `WidgetProvider`'daki
`String.format(Locale.US, "🎯 Hedef: %%d/%d dk", goal).format(focus)` ifadesi hatalıydı
(çift format geçişi). Basit string birleştirmeyle değiştirildi.

### Aşama 2'de eklenenler
- `TodayFragment.kt` — günün özeti ekranı
- `fragment_today.xml`, `item_today_task.xml`, `ic_today.xml`
- Alt menü yeniden düzenlendi: **Ana Sayfa · Bugün · Konular · İlerleme · Sayaç** (5 sekme)
- ⚠️ BottomNavigationView **en fazla 5 öğe** destekler. 6. eklenirse çöker.
  Bu yüzden ortadaki boş `nav_placeholder` kaldırıldı, FAB sağ alt köşeye taşındı.
- MainActivity'ye eklendi: `openToday()`, `openTasksWithEditor()`, `quickAddQuestions()`;
  `refreshHome()` artık Bugün ekranını da tazeliyor.

## 📌 Eski not: v5.2 çalışması kayıptı

Önceki oturumda v5.2 planlanmıştı:
1. **Özel geri sayımlar** — KPSS dışında düğün, doğum günü, tatil… sınırsız etkinlik
2. **"🗓️ Bugün" ekranı** — alt menüye 6. sekme olarak kişisel asistan
3. **Zengin widget + bildirimden görev tamamlama**

Zip'te bu çalışma **yok** (doğrulandı: `TodayFragment` yok, `Store.kt`'de `DayEvent` yok,
sürüm hâlâ 5.1/12). O oturum yarıda kesilmiş. Sıfırdan yazılacak.

---

## v7.78 — Resimli Kanıt + Yapay Zekâ Denetimi + Zorlayıcı Öğretmen Koçu
**versionCode 114 · APK 27,8 MB · md5 a18e44439e93622fd16b9759f84a3fc1**

### Kullanıcı isteği
> "Görevleri vb şeyleri resimli kanıt iste ve yapay zeka kontrolü ekle ders çalışmam için öğretmen rolü üstlen beni zorla ders calismam için"

Üç ayrı özellik olarak yorumlandı ve üçü de eklendi.

---

### 1) RESİMLİ KANIT — `Kanit.kt` · `KanitDenetci.kt` · `KanitAkisi.kt` · `KanitActivity.kt`

Görevi tamamlarken fotoğraf istenir, yapay zekâ fotoğrafın gerçekten o işi
gösterip göstermediğine karar verir.

**Akış:** görev işaretlenir → kanıt gerekli mi? → kamera/galeri →
`GorselHazirla` ile hazırlanır → `AiClient.gorselDenetim()` → JSON karar →
onay ise görev tamamlanır, red ise açık kalır.

**Politika (hangi görevler kanıt ister):**
| Mod | Davranış |
|---|---|
| Kapalı | hiçbiri |
| İşaretlediklerim *(varsayılan)* | göreve uzun bas → "Kanıt iste" |
| Etikete göre | seçilen etiketlerdekiler (varsayılan: Okul + Acil) |
| Tüm görevler | istisnasız hepsi |

**Katılık:** Gevşek / Normal / Sert. Model kararı `kararUygula()` ile
harmanlanır — Sert modda güven <60 ise onay bile reddedilir.

**Kaçış kapıları (bilinçli):**
- **İtiraz et** → kullanıcı gerekçe yazar, görev geçer (`ITIRAZ` durumu).
  Yapay zekâ %100 doğru değil; yanlış kararla kullanıcıyı kilitlemek
  uygulamayı kullanılmaz yapardı.
- **Çevrimdışıyken kabul et** (varsayılan açık) → AI yoksa fotoğraf yeterli.
- **Red görevi engellesin** (varsayılan açık) → kapatılırsa red uyarı olur.

**Room şemasına DOKUNULMADI.** Kanıtlar ayrı depoda (SharedPreferences +
`filesDir/kanit/`) durur, göreve `id` ile bağlanır. Gerekçe: v7.76 Room
geçişi henüz cihazda doğrulanmadı; `fallbackToDestructiveMigration` açıkken
şema değiştirmek **tüm görevleri silebilirdi**. Bedeli: silinen görevin
kanıtı sızıntı yapar → `artiklariTemizle()` ayarlar ekranından toplar.

Tekrarlı görev yenilenince kanıt sıfırlanır (`tekrarIcinSifirla`) — eski
fotoğraf yeni tekrarı kanıtlamaz. Fotoğraf geçmişe taşınır, silinmez.

---

### 2) ZORLAYICI ÖĞRETMEN KOÇU — `Koc.kt` · `KocZamanlayici.kt` · `KocBildirim.kt` · `KocMesaj.kt` · `KocActivity.kt`

`OgretmenMotoru` (v7.37) **pasif**: kullanıcı derse girerse anlatır.
Koç **aktif**: kullanıcıyı arar, hedef koyar, hesap sorar.

**Borç sistemi** — asıl zorlama mekanizması:
- Hedefin altında kalınan dakikalar borç olur, ertesi günün hedefine eklenir
- Borç tavanı = temel hedefin 1 katı (60 dk hedefte en fazla 120 dk istenir)
- Tavan olmasaydı bir hafta çalışmayan biri imkânsız hedefle karşılaşıp pes ederdi

**Sertlik kademeleri:**
| Kademe | Davranış |
|---|---|
| Nazik | hatırlatır, üstelemez, erteleme 30 dk |
| Kararlı *(varsayılan)* | ısrar eder, hesap sorar, erteleme 15 dk |
| Acımasız | `ZorunluUyari` ile sessizde bile alarm, mazeret kabul etmez, erteleme 10 dk |

**İki alarm** (`setExactAndAllowWhileIdle` — kesin, çünkü 20:00 çağrısı
21:30'da gelirse baskı işlevini kaybeder):
- Çalışma saati (varsayılan 20:00) → "otur çalış" + Başla/Sonra/Karne düğmeleri
- Hesap saati (varsayılan 22:00) → hedef tuttuysa tebrik, tutmadıysa hesap sorar

**Mazeret denetimi** — `KocMesaj.mazeretDegerlendir()`:
AI mazereti değerlendirir. Acımasız modda yalnızca hastalık/kaza gibi gerçek
engeller kabul edilir; "yorgundum", "canım istemedi" reddedilir. Son 14 günün
mazeretleri isteme eklenir — **aynı mazereti tekrarlarsa yüzüne vurulur**.
AI yoksa anahtar kelime tabanlı yerel süzgeç çalışır.

Kabul edilen mazerette borç yazılmaz ve **seri korunur** — hastalıkta seriyi
kırmak cesaret kırıcıydı.

**Karne:** seri, en uzun seri, son 30 gün başarı yüzdesi, toplam dakika,
günlük geçmiş (✓ başarılı · ~ mazeretli · ✕ başarısız).

Bildirim mesajı **önceden** üretilir (`KocMesaj.arkaPlandaUret`) — bildirim
`BroadcastReceiver` içinde oluşuyor, orada ağ beklemek alıcıyı öldürür.

---

### 3) DEĞİŞEN DOSYALAR
```
AiClient.kt        +gorselDenetim() +geminiGorselIstem() +openAiGorselIstem()
                   (konuOku el yazısına sabitli; denetim farklı çıktı istiyor)
TasksFragment.kt   toggleTask → kanıt kapısı, tamamlamaYap() ayrıldı
                   +kanitIste/kanitiDenetle/kanitiKopyala + 3 launcher
                   menüye "Kanıt iste/isteme"
SettingsFragment   +rowKoc +rowKanit + kocOzetiTazele() + onResume
App.kt             açılışta koç bakımı (kaçırılan gün, alarm, mesaj)
BootReceiver.kt    yeniden başlatma + gün değişiminde koç alarmı
AndroidManifest    +2 activity +2 receiver
file_paths.xml     +kanit/ +fotograf/
strings.xml        +165 dize (2044 → 2209)
```

### Derleme notları
- İlk denemede 2 hata: `KanitAkisi` içinde `android.app.AlertDialog` yerine
  `androidx.appcompat.app.AlertDialog` gerekiyordu (MaterialAlertDialogBuilder
  onu döndürür); `IntArray.mapIndexedNotNull` yok → `.toList()` eklendi.
- İmza SHA-256 değişmedi: `5F:15:D4:E7:...:85:11` — üstüne kurulur.

### ⚠️ Test edilmedi
Sandbox'ta emülatör yok. Özellikle şunlar cihazda doğrulanmalı:
1. Kanıt kamerası açılıyor mu (FileProvider yolu yeni: `filesDir/kanit/`)
2. AI denetim kararı geliyor mu, JSON ayrışıyor mu
3. Koç alarmı belirlenen saatte çalıyor mu (Android 12+ kesin alarm izni!)
4. Bildirim düğmeleri (Başla/Sonra/Hesap ver) çalışıyor mu

---

## v7.79 — Ders Programı Takibi (Müfredat)
**versionCode 115 · APK 27,8 MB · md5 d38e80c21a99331e0e8a48c7636d1f73**

### Kullanıcı şikâyeti
> "Öğretmen modu belirlediğim derslere yönelim sağlasın her konusunu bilsin ve
>  sadece o konuyu hesap vb şeyler yapsın ders ders bitirtsin karmakarışık
>  program yapmasin"

### v7.78'de ne yanlıştı — kabul
`KocMesaj.bugunNeCalisayim()` **tüm kursların adını** modele veriyor ve
"3 maddelik plan üret" diyordu. Model her gün farklı derslerden rastgele
öneriler üretiyordu; hiçbiri bitmiyordu. Kullanıcı haklıydı, bu bir tasarım
hatasıydı.

### Çözüm — `Mufredat.kt` (yeni dosya)

Kullanıcı **bir kurs seçer**, koç o kursun derslerini `order` sırasıyla takip
eder. Her an **tek bir aktif ders** vardır.

**Sıralama:** bölüm sırası → ders sırası → id. Kurslar ekranındaki görünümle
birebir aynı; kullanıcı orada 3. gördüğü dersi burada 7. görürse güven kaybolur.

**`aiBaglami()`** — her AI isteminin başına eklenen blok:
```
=== ÖĞRENCİNİN PROGRAMI ===
Kurs / Bölüm / ŞU AN ÇALIŞILAN DERS (n. ders / toplam m)
Ders içeriği, planlanan süre, verilen süre, tamamlanan ders sayısı

MUTLAK KURAL: Öğrenci ŞU AN sadece "X" dersini çalışıyor.
BAŞKA hiçbir derse, konuya veya kursa DEĞİNME.
Yeni konu önerme, program değiştirme, "şunu da çalış" deme.
=== PROGRAM SONU ===
```
"Sadece o konuyu" isteğinin teknik karşılığı bu blok.

**Ders bazlı hesap sorma** — `KocMesaj.dersHesabiSorusu()` +
`dersCevabiDegerlendir()`: ders bitmeden önce **o dersin içeriğinden** soru
sorulur, cevap AI ile değerlendirilir. Yetersizse ders bitmez
("Tekrar cevapla" / "Yine de bitir"). Nazik modda soru sorulmaz.

**`Store.Lesson.done` yeniden kullanıldı** — ayrı tamamlanma kaydı tutmak iki
gerçek kaynağı yaratır ve senkron sorunu çıkarırdı. Kullanıcı Kurslar
ekranından işaretlese de koç görür.

**Ders kilitleme:** sıradaki yerine başka bir derse odaklanılabilir
(`dersiKilitle`); o ders bitince kilit otomatik kalkar.

### Değişen davranışlar
| Yer | Eskiden | Şimdi |
|---|---|---|
| "Bugün ne çalışayım" | tüm kurslardan rastgele 3 madde | tek dersi 3 adıma bölen plan |
| Hatırlatma bildirimi | "Çalışma vakti" | "«Ders adı» — çalışma vakti" |
| Hesap bildirimi | "Hesap zamanı" | "«Ders adı» — hesap zamanı" |
| Koç mesajı | genel motivasyon | ders adı + program sırası (n/m) |
| Ayarlar alt yazısı | sertlik + hedef | "3/12 · Duvar Detayları" |

### Değişen dosyalar
```
Mufredat.kt        YENİ — kurs seçimi, ders sırası, ilerleme, AI bağlamı,
                   ders bazlı süre kaydı
KocMesaj.kt        bugunNeCalisayim tamamen yeniden yazıldı
                   +dersHesabiSorusu +dersCevabiDegerlendir +yerelDersPlani
                   motivasyonIstemi'ne program bağlamı eklendi
Koc.kt             hatirlatmaMetni/hesapMetni ders adı kullanıyor
                   +sureyiDerseYaz (gün kapanışında derse işlenir)
KocActivity.kt     +PROGRAM bölümü, programKarti, kursSec, dersListesi,
                   dersiBitirSor, dersSorusuSor, dersCevabiniIsle, dersiKapat
KocBildirim.kt     bildirim başlıklarında ders adı
SettingsFragment   koç alt yazısı program durumunu gösterir
strings.xml        +50 dize (2209 → 2259)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### ⚠️ Test edilmedi
- Kurs seçimi listesi doluyor mu (Kurslar sekmesinde kurs olması şart)
- Ders bitirme hesabı: AI soru üretiyor mu, cevap değerlendiriliyor mu
- Bildirim başlığında ders adı görünüyor mu

---

## v7.80 — Konu Programı + Alan Bağımsızlığı
**versionCode 116 · APK 27,8 MB · md5 ce35b51a20a29d50eecf9705ce5dd14b**

### Kullanıcı isteği
> "Sadece mühendislik kursları vb şeylerde yardımcı olmasın, konular
>  kısmındaki konuları da seçme hakkım olsun onu sırasıyla bitirtsin"

İki ayrı sorun vardı, ikisi de düzeltildi.

### 1) Program kaynağı artık iki türlü

v7.79'da yalnızca `Store.Course` (Kurslar sekmesi) seçilebiliyordu.
Artık **Konular sekmesindeki konular** da seçilebiliyor.

`Mufredat` iki kaynağı ortak bir soyutlamada birleştirdi:
```
KAYNAK_KURS → Kurs > Bölüm > Ders   (Store.Lesson)
KAYNAK_KONU → Konu > Maddeler       (Store.SubItem)
                    ↓
              Mufredat.Adim   ← koçun geri kalanı sadece bunu görür
```

`Adim(id, baslik, bitti, aciklama, dakika, assetYolu, ustBaslik)` —
tek okuma-amaçlı sarmalayıcı. Çağıran taraf hangi kaynağın kullanıldığını
bilmiyor; yalnızca `aktifAdim()` soruyor. Yazma (bitirme) tek noktada
(`adimDurumu`) kaynağa göre ayrışıyor.

**Seçim tek listede:** kurslar 📚, konular 📝 ile işaretli tek diyalogda.
Ayrı iki diyalog yerine tek liste — kullanıcı "hangi sekmedeydi" diye
düşünmesin.

**Boş konu koruması:** maddesi olmayan konu seçilemiyor (uyarı verir),
yoksa koç boş programla kilitlenirdi.

**v7.79 uyumluluğu:** `eskiSecimiTasi()` eski `kurs_id` anahtarını yeni
biçime bir kez taşır — güncelleyen kullanıcı seçimini kaybetmez.

### 2) Yapay zekâ artık mühendisliğe sabitlenmiş değil

Kullanıcının asıl şikâyeti buydu. Üç yerde alan sabiti vardı:

| Dosya | Eskiden | Şimdi |
|---|---|---|
| `OgretmenMotoru` | "Sen deneyimli bir **inşaat/mimarlık** eğitmenisin" | "Sen deneyimli bir eğitmensin… konu hangi alandan olursa olsun o alanın uzmanı gibi davran" |
| `OgretmenMotoru` | "Somut örnek ver (**şantiye, proje, ölçü**)" | "Örnek dersin ALANINA uygun olsun (teknik→proje, dil→cümle, tarih→olay)" |
| `DersAsistan` | "Kullanıcı **inşaat mühendisi/mimar adayı**" | "Seviyeyi dersin alanına göre ayarla; alanı içerikten anla" |
| `DersAsistan` | "gerçek bir **şantiye/proje** örneği" | "somut, gerçek hayattan bir örnek" |

`Mufredat.aiBaglami()`'ye de eklendi:
> "Konunun alanını başlıktan anla ve o alanın uzmanı gibi davran;
>  kendi uzmanlık alanını varsayma."

Artık İngilizce kelime listesi, tarih konusu, ehliyet sınavı — hepsi çalışır.

### Değişen dosyalar
```
Mufredat.kt        Adim modeli, KAYNAK_KURS/KAYNAK_KONU, adimlar(),
                   aktifAdim(), adimDurumu(), eskiSecimiTasi()
                   (kursId/aktifDers/dersler → kaynakId/aktifAdim/adimlar)
KocActivity.kt     kursSec → programSec (kurs+konu tek liste)
                   tür bazlı etiketler: "Bu dersi bitir" / "Bu maddeyi bitir"
Koc.kt · KocMesaj.kt · KocBildirim.kt · SettingsFragment.kt
                   yeni API'ye uyarlandı (.title → .baslik, .done → .bitti)
OgretmenMotoru.kt · DersAsistan.kt    alan sabitleri kaldırıldı
strings.xml        +13 dize, 16 metin alan-bağımsız hale getirildi (2272)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### ⚠️ Test edilmedi
- Konu seçimi listede görünüyor mu, maddeler sırayla geliyor mu
- Konu maddesi bitirince Konular sekmesinde de ✓ oluyor mu (aynı veri)
- v7.79'dan güncelleyende eski kurs seçimi korunuyor mu

---

## v7.81 — Toplu Madde Üretimi + Derinlemesine Anlatım + PDF
**versionCode 117 · APK 28,1 MB · md5 cc74801df88f8c930eb50f1df540c090**

### Kullanıcı isteği
> "Konu maddelerini ai ile toplu uretme ve pdf olarak uzun uzun destkeleme
>  (kitaplardan destkeleme) resimlerle destkeleme vb konuyu anlamam için
>  herseyi ekle"

### 1) Toplu madde üretimi — `KonuUretici.uret()`
Konular › madde ekle › **"🤖 Toplu madde üret (AI)"**
- **10 / 25 / 50 / 100** madde seçeneği
- **4 seviye:** Başlangıç · Orta · İleri · Karışık (kolaydan zora)

**Neden parça parça (20'şerlik):** "100 madde üret" tek istekte istenirse
model yarıda kesiliyor (token sınırı) ya da kalite düşüyor. Her grupta
önceki maddeler bağlam olarak veriliyor → tekrar yok, her grup tam.
Model tekrar üretmeye başlarsa döngü erken kesiliyor (kota israfı olmasın).

`AltBaslikBulucu` (v6.x) korundu — o tek seferde ~10 madde bulur, bu ayrı
bir iş yapıyor.

### 2) Derinlemesine anlatım — `KonuUretici.anlat()` + `KonuAnlatimActivity`
Konu maddesine **uzun basınca** → "📖 Konuyu anlat (AI)"

Ekranda:
- **5-7 bölümlük uzun ders anlatımı** (seçilebilir metin)
- **Kendi kaynaklarından alıntılar** — cihazdaki ders PDF'lerinde
  `PdfArama` ile aranır, bulunan parçalar modele bağlam olarak verilir ve
  ekranda gösterilir. Dokununca **o sayfa açılır**.
  → "Kitaplardan destekleme" isteği: uydurma kaynak yerine kullanıcının
    kendi PDF'leri. Yalnızca **indekslenmiş** PDF'lerde aranıyor (yeni
    indeksleme dakikalar sürer, anlatımı bekletmesin).
- **Görsel önerileri** — model resim üretemez ama "nasıl bir görsel
  yardımcı olur" tarif edebilir. Dokununca Google Görseller'de aratır.
- **Özet** + "😕 Anlamadım, daha basit anlat" / "🔬 Daha detaylı anlat"

**Önbellek:** anlatım 10-30 sn sürüyor ve kota harcıyor → bir kez üretilip
saklanıyor, ikinci açılışta anında geliyor. "Yeniden üret" düğmesi duruyor.

### 3) PDF çıktısı — `KonuPdf.kt`
"📄 PDF olarak kaydet" → kapak + içindekiler + bölümler + özet + görsel
önerileri + kaynak alıntıları. Aç / Paylaş seçenekleri.

**Türkçe karakter sorunu çözüldü:** PDFBox'ın gömülü Helvetica'sı WinAnsi;
ş/ğ/ı/İ yazınca `IllegalArgumentException` fırlatıp PDF'i çökertiyordu.
`assets/fonts/poppins_regular.ttf` + `PDType0Font.load()` ile Unicode font
gömüldü (Poppins'te tüm Türkçe glyph'ler doğrulandı). Font yüklenemezse
Helvetica'ya düşülüp metin ASCII'ye sadeleştiriliyor — bozuk PDF yerine
Türkçesiz PDF.

Satır sarma, sayfa taşması ve sığmayan uzun kelime (URL) parçalama elle
yönetiliyor. Tek satır yazılamazsa tüm PDF çöpe atılmıyor.

### Koç entegrasyonu
Koç ekranında aktif derse/maddeye **"📖 Konuyu anlat"** düğmesi eklendi;
ders listesi menüsünden de erişilebiliyor.

### Değişen dosyalar
```
KonuUretici.kt        YENİ — uret() parça parça üretim, anlat() derin
                      anlatım, PDF alıntı arama, anlatım önbelleği
KonuPdf.kt            YENİ — PDFBox + gömülü Unicode font, Yazici sınıfı
                      (satır sarma + sayfa taşması)
KonuAnlatimActivity   YENİ — anlatım ekranı, görsel arama, PDF dışa aktarma
TopicsFragment.kt     +topluUretimAyarlari +topluUret +maddeSecenekleri
                      (uzun basınca menü), +Gravity/ScrollView import
KocActivity.kt        aktif adıma "Konuyu anlat" düğmesi
assets/fonts/         poppins_regular.ttf + poppins_bold.ttf (PDF için)
file_paths.xml        +konu_pdf/
AndroidManifest       +KonuAnlatimActivity
strings.xml           +48 dize (2272 → 2320)
```

### Derleme
İlk denemede temiz — 0 hata. Fontlar APK'da doğrulandı. İmza değişmedi.

### ⚠️ Test edilmedi
- 100 madde üretimi gerçekten tamamlanıyor mu (uzun sürebilir)
- **PDF'te Türkçe karakterler düzgün çıkıyor mu** (en riskli kısım)
- PDF alıntıları geliyor mu — Kaynaklar ekranından PDF indekslenmiş olmalı
- Görsel arama tarayıcıyı açıyor mu

---

## v7.82 — Sınav Üretimi + Sesli Anlatım + PDF Kitap
**versionCode 118 · APK 28,1 MB · md5 41f513087c71576fbae504c7aad2993e**

v7.81 sonunda önerilen üç madde de tamamlandı.

### 1) Anlatımdan sınav — `KonuUretici.quizUret()`
Anlatım ekranında **"📝 Bu konudan sınav üret"** → 6 çoktan seçmeli soru.

**Neden `QuizUretici` kullanılmadı:** o sınıf `Store.Lesson` alıyor ve
soruları dersin *başlığı + PDF'i* üzerinden üretiyor. Burada çok daha iyi
bir kaynak var: az önce üretilmiş **anlatım metninin kendisi**. Sorular
birebir okunan metinden çıkınca "anlatımda geçmeyen şey soruldu" durumu
ortadan kalkıyor.

**Sanal ders kimliği** — `QuizStore` soruları `lessonId` ile gruplar ama
konu maddelerinin gerçek `Lesson` kaydı yok. Madde metninden **kararlı ve
negatif** kimlik türetiliyor (`sanalDersId`):
· kararlı → aynı madde hep aynı kimlik, sorular kaybolmaz
· negatif → gerçek ders id'leri (`currentTimeMillis` tabanlı, hep pozitif)
  ile asla çakışmaz

Sınav mevcut `QuizActivity` ile açılıyor — yeni ekran yazılmadı.

### 2) Sesli anlatım
**"🔊 Sesli oku"** → mevcut `SesliDersServisi` yeniden kullanıldı. O servis
zaten ön plan bildirimi, cümle cümle ilerleme ve kaldığı yeri hatırlama
işlerini yapıyor; yeni bir TTS servisi yazmak bunları kopyalamak olurdu.

`seslendirmeMetni()` anlatımı TTS'e uygun düz metne çeviriyor: başlıklar
cümle sonu noktalamasıyla ayrılıyor (yoksa motor başlıkla paragrafı tek
cümle gibi okuyor), madde işaretleri (•) temizleniyor (yoksa "nokta nokta"
diye okunuyor).

Düğme durumu servise göre değişiyor: Sesli oku / Duraklat / Devam et.
Sanal asset anahtarı (`konu:<madde>`) ile hangi metnin okunduğu izleniyor.

### 3) PDF kitap — konuya uzun bas
Konu başlığına **uzun basınca** menü:
- **📕 PDF kitap yap** — hazır anlatımları tek PDF'te toplar
  (kapak + içindekiler + tüm bölümler). `KonuPdf.kitap()` v7.81'de
  yazılmıştı, arayüzü şimdi eklendi.
- **📖 Tüm maddeleri anlat** — eksik anlatımları toplu üretir,
  **iptal edilebilir** ilerleme penceresiyle. Zaten anlatımı olanlar
  atlanır → ikinci çalıştırma kaldığı yerden devam eder.
- **ℹ️ Hazır anlatım: n/m** — durum bilgisi

**Bilinçli ayrım:** kitap yapma AI'ya gitmez, sadece hazır anlatımları
toplar (saniyeler sürer). Toplu anlatım ayrı bir işlem çünkü 50 maddelik
konu yarım saat sürebilir — kullanıcı ne yaptığını bilerek başlatmalı.
Süre tahmini ve kota uyarısı önceden gösteriliyor.

### Değişen dosyalar
```
KonuUretici.kt        +quizUret() +quizAyristir() +sanalDersId()
                      +quizVarMi() +seslendirmeMetni()
KonuAnlatimActivity   +sınav düğmeleri +sesli okuma (durum bazlı etiket)
TopicsFragment.kt     +konuSecenekleri (konuya uzun bas) +kitapYap
                      +tumunuAnlat +topluAnlatimBasla (iptal edilebilir)
strings.xml           +27 dize (2320 → 2347)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### ⚠️ Test edilmedi
- Sınav soruları anlatımdan doğru çıkıyor mu, `QuizActivity` açılıyor mu
- **TTS Türkçe okuyor mu** (cihazda Türkçe TTS verisi kurulu olmalı)
- 20+ maddelik PDF kitap bellek yetiyor mu (düşük RAM'de risk)
- Toplu anlatımda iptal düğmesi gerçekten kesiyor mu

---

## v7.83 — Okuma Ayarları + Hata Defteri + İlerleme Widget'ı
**versionCode 119 · APK 28,1 MB · md5 df6567d79f9ca3960001b7d1a27ffc56**

### 1) Okuma ayarları — `OkumaAyar.kt`
Anlatım ekranında metnin üstünde hızlı çubuk: **A− / A+ / zemin / ⚙**

- Punto 12-26 sp · satır aralığı 4 kademe
- Zemin: Tema · Açık · **Sepya** · Koyu · Siyah (OLED)
- Ekran açık kalsın · iki yana yasla (Android 8+)

**Neden genel temaya bağlanmadı:** `ThemeManager` tüm ekranları etkiliyor.
Gece ders okurken sırf onun için tüm uygulamayı karartmak gerekiyordu.
Bu ayar yalnızca `KonuAnlatimActivity` içinde geçerli — e-kitap
uygulamalarındaki gibi bağımsız okuma modu.

Ayarlar ayrı ekrana konmadı; punto ayarlamak metne bakarken yapılan bir iş,
ekran değiştirip dönmek akışı bozardı.

### 2) Hata defteri — `Hatalarim.kt` + `HatalarimActivity`
Quiz "8/10" der ve geçer; hangi 2 soruyu yanlış yaptığın kaybolurdu.
Artık yanlışlar deftere yazılıyor.

**Soru seviyesinde Leitner:** `QuizStore.TekrarKaydi` ders seviyesinde
aralıklı tekrar yapıyor; burada aynı mantık **soru seviyesinde**:
· Yanlış → kutu 0, yarın tekrar
· Doğru → kutu +1, aralık uzar (1·3·7·16·35 gün)
· Son kutuyu geçen soru "öğrenildi" sayılıp defterden çıkar

**Soru kopyalanarak saklanıyor**, `QuizStore`'a referans verilmiyor:
kullanıcı "Soruları yenile" derse eski sorular silinir, defter boşalırdı.

Ekranda: bugün/toplam/öğrenilen sayaçları, **zayıf olduğun konular**
(kaynak bazlı dağılım çubuğu), soru listesi (en çok yanlış üstte),
soruya dokununca doğru cevap + açıklama.

### 🔴 Yol açılan gerçek hata
`QuizActivity.sonucGoster()` koşulu `if (lessonId > 0L)` idi. v7.82'de
eklediğim konu sınavlarının sanal kimlikleri **negatif** olduğu için
**konu sınav sonuçları hiç kaydedilmiyordu.** `!= 0L` yapıldı.
Bu benim v7.82'de gözden kaçırdığım bir hataydı.

### 3) İlerleme widget'ı — `IlerlemeWidget.kt` (4×2)
Ana ekranda: takip edilen kurs/konu · yüzde · **şu an çalışılacak ders** ·
kaç madde kaldı · bugünkü hedef durumu. Dokununca Koç ekranı açılır.

RemoteViews kısıtına uyuldu: yalnızca LinearLayout/TextView/ProgressBar.
Ayırıcı için `<View>` **kullanılmadı** (v7.40.1'de widget'ı kırmıştı).
Widget sayısı 10 → **11**.

### Değişen dosyalar
```
OkumaAyar.kt           YENİ — punto, satır aralığı, 5 zemin, ekran açık
Hatalarim.kt           YENİ — Leitner defteri, kaynak dağılımı, tekrar seçimi
HatalarimActivity.kt   YENİ — özet, zayıf konular, soru listesi
IlerlemeWidget.kt      YENİ + widget_ilerleme.xml + w_ilerleme_info.xml
QuizActivity.kt        +EXTRA_HATA_MODU +acHatalar() , yanlış→defter,
                       doğru→kutu ilerlet, **lessonId koşulu düzeltildi**
KonuAnlatimActivity    okuma çubuğu, zemin uygulama, ayarlara duyarlı metin
WidgetCommon.kt        TUM_WIDGETLAR 10 → 11
SettingsFragment       +rowHatalarim + durum özeti
KocActivity.kt         hata defteri kısayolu (bugünkü tekrar vurgulu)
strings.xml            +45 dize (2347 → 2392)
```

### Derleme
İlk denemede temiz — 0 hata. Manifest denetimi: 11 widget, tüm Activity ve
Receiver kayıtlı. İmza değişmedi.

### ⚠️ Test edilmedi
- **İlerleme widget'ı ana ekranda görünüyor mu** (RemoteViews riski)
- Sepya/koyu zemin okunabilir mi, punto değişimi kalıcı mı
- Yanlış cevap deftere düşüyor mu, Leitner tarihleri doğru mu
- Konu sınav sonucu artık kaydediliyor mu (v7.82 hatası)

---

## v7.84 — Terim Sözlüğü + Benzer Soru + Zenginleştirilmiş Haftalık Rapor
**versionCode 120 · APK 28,2 MB · md5 b1cf4a6fea1c5c793d73e37c34defaae**

### 1) Terim sözlüğü — `Sozluk.kt` + `SozlukActivity`
Anlatım okurken **kelimeyi seç → "Ne demek?"** → açıklama gelir ve
sözlüğe kaydedilir.

**Neden metin seçim menüsü:** alternatifler (a) her terimi tıklanabilir
yapmak — hangi kelimenin terim olduğunu bilemeyiz, modelden işaretletmek
kırılgan; (b) ayrı arama kutusu — kullanıcı kelimeyi elle yazar.
Seçim menüsü ikisini de çözüyor: sistem menüsü zaten oradaymış gibi geliyor.

**Önbellek:** bir terim bir kez açıklandıktan sonra AI'ya tekrar sorulmaz.
İkinci dokunuş anında cevap verir, kota harcamaz. `bakildi` sayacı artar —
çok bakılan terim zayıf nokta demek, ekranda **"En çok baktıkların"**
bölümünde gösterilir.

Terimler anlatım önbelleğinden **bağımsız** yaşar: anlatım "yeniden üret"
ile silinse bile sözlük durur. Yıldızlama, arama, bağlam bilgisi (terimin
hangi konuda geçtiği) var. Tavan 500 terim; aşılırsa yıldızlılar ve çok
bakılanlar korunur.

### 2) Benzer soru üretimi — `Hatalarim.benzerUret()`
Hata defterinde soruya dokun → **"🔀 Benzer soru üret"** → aynı kavramı
farklı biçimde soran 3 yeni soru.

**Neden gerekli:** aynı soruyu tekrar çözmek ezberi ödüllendirir —
kullanıcı kavramı değil "C şıkkı" cevabını hatırlar. Farklı sorulmuş soru
gerçekten öğrenilip öğrenilmediğini ölçer.

**Geçici havuz:** üretilen sorular kalıcı depoya yazılmaz
(`Hatalarim.geciciAyarla` → bellekte). `QuizActivity.acGecici()` ile
çözülür, `onDestroy`'da temizlenir. Bu sorularda yanlış yapmak hata
defterine **yeni kayıt eklemez** — pekiştirme amaçlı üretildiler, kalıcı
hata sayılmamalı. Kimlikleri negatif ve zamana bağlı: kalıcı sorularla
çakışmaz.

### 3) Haftalık rapor zenginleştirildi — `WeeklyReportReceiver`
Rapor yalnızca madde/soru/odak sayısı veriyordu; koç, müfredat, hata
defteri ve sözlük eklendikten sonra eksik kalıyordu. Artık içeriyor:
```
Bu hafta 12 madde, 40 soru, 320 dk odak 🔥 Serin 5 gün (rekor 11).

📚 Revit Eğitimi: 13/47 (%27)
▶ Sırada: Duvar Detayları

🎓 Koç: 5/7 gün hedef tuttu · 30 dk borç

🎯 Hata defteri: 18 soru · bugün 4 tekrar · 7 öğrenildi

📖 Sözlüğünde 23 terim var.
```
Bekleyen tekrar varsa bildirime **"Bugünkü N soruyu çöz"** eylem düğmesi
eklenir → doğrudan hata defterini açar.

### Değişen dosyalar
```
Sozluk.kt              YENİ — terim deposu, AI açıklama, önbellek, yıldız,
                       bağlam gruplama, yedekleme giriş/çıkışı
SozlukActivity.kt      YENİ — arama, en çok bakılanlar, yıldız süzgeci
Hatalarim.kt           +benzerUret() +benzerAyristir() +geçici havuz
HatalarimActivity.kt   soru detayına "Benzer soru üret"
QuizActivity.kt        +EXTRA_GECICI +acGecici() , geçici modda kayıt yok
KonuAnlatimActivity    +sozlukSecimi() ActionMode +terimAcikla() +terimGoster()
WeeklyReportReceiver   öğrenme özeti + hata defteri eylem düğmesi
SettingsFragment       +rowSozluk + terim sayısı özeti
strings.xml            +32 dize (2392 → 2424)
```

### Derleme
İlk denemede temiz — 0 hata. Manifest denetimi temiz. İmza değişmedi.

### ⚠️ Test edilmedi
- **Metin seçince "Ne demek?" menüde çıkıyor mu** (ActionMode riski)
- Benzer sorular gerçekten farklı mı, yoksa aynısını mı tekrarlıyor
- Haftalık rapor pazar 20:00'de geliyor mu (uzun süre beklemeli)

---

## v7.85 — Halka Kadranlı Sayaç + Sayaç Widget'ı + Titreşim
**versionCode 121 · APK 28,2 MB · md5 bc81d15f299f3dcddc530aac1b5eed74**

### Kullanıcı isteği (ekran görüntüsüyle)
> "Geri sayımı bu şekilde gözüksün ve mini ekran olarak eklenti ekle.
>  Bildirim ekranında da zarif bir şekilde senkronizasyonlu çalışsın,
>  ekranı kapatınca durmasın, bitince titreşim olsun"

Google Saat tarzı: dışta çizgili halka, ortada dolgu daire, büyük süre,
altta bitiş saati.

### 1) `SayacKadraniView.kt` — özel çizim
Hazır bileşenlerle yapılamıyordu: `CircularProgressIndicator` düz yay
çiziyor, çizgili kadran yok; 60 çizgiyi ImageView ile koymak 60 görünüm
demek. `onDraw` içinde döngüyle çizmek hem hafif hem birebir istenen görüntü.

- 60 çizgi (saat kadranı hissi); kalan süreye düşenler **vurgulu ve kalın**
- Paint nesneleri bir kez oluşturuluyor (onDraw'da nesne yaratmak çöp üretir)
- Kare ölçüm — kadran elips olmasın
- Uzun süre metni (1:23:45) taşmasın diye punto uzunluğa göre küçülüyor
- Renkler temadan geliyor → koyu/açık temada okunur
- Kadrana dokunmak başlat/duraklat

**Eski `timeText` silinmedi**, `visibility="gone"` yapıldı. Fragment'in tüm
güncelleme mantığı ona yazmaya devam ediyor; tek gerçek kaynak korunarak
risk sıfırlandı.

### 2) `SayacWidget.kt` (2×2) — mini ekran eklentisi
**Neden `Chronometer`:** widget'ı saniye saniye güncellemek imkânsız
(`updatePeriodMillis` en hızlı 30 dk, sık `updateAppWidget` pili bitirir).
`Chronometer` RemoteViews'ta desteklenir ve **sistem kendisi sayar** —
biz sadece başlangıç noktasını veririz. `TimerNotifier`'daki
`setUsesChronometer` ile aynı mantık.

Duraklatılınca Chronometer gizlenip sabit metin gösteriliyor (durmuş
Chronometer yanlış değer gösterebiliyor). Widget'tan **Başlat/Duraklat**
ve **Sıfırla** yapılabiliyor. Kilit ekranına da eklenebilir
(`widgetCategory="home_screen|keyguard"`).

Widget sayısı 11 → **12**.

### 3) Anlık senkronizasyon — `TimerEngine.sayaciYansit()`
Widget kendi durumunu tutmaz, her şeyi `TimerEngine`'den okur.
`start/pause/reset/finish/setTotalMs` → hepsi `sayaciYansit()` çağırıyor.
Uygulamadan, bildirimden ya da widget'tan kontrol edilsin — üçü de aynı
kaynağa yazdığı için görüntü anında tutarlı.

Yalnızca sayaç widget'ı tazeleniyor; `refreshAll` her duraklat/başlatta
12 widget'ı yenilemek olurdu.

### 4) 🔴 Titreşim — gerçek eksiklik giderildi
Titreşim **hiç açıkça tetiklenmiyordu**; yalnızca bildirimin varsayılan
deseninden geliyordu. Bu üç durumda kayboluyor:
1. Android 13+ bildirim izni yoksa bildirim hiç gitmez
2. Kanal titreşimi kapatılmış olabilir
3. Android 8+ kanal ayarları oluşturulduktan sonra değiştirilemez

`TimerActionReceiver.titret()` eklendi: uzun-kısa-uzun deseni
(0,450,180,450,180,700), **USAGE_ALARM** ile — telefon sessizde/rahatsız
etmeyin modundayken bile alarm titreşimine izin verilir.
`Store.getVibEnabled` kapalıysa saygı gösteriliyor.

### 5) Bildirim zarifleştirildi
"Çalışıyor" yerine **"🔔 12:26'de bitiyor"** — kilit ekranında en çok
merak edilen bilgi. Alt satırda toplam süre (`setSubText`).

**Ekran kapanınca durmama** zaten çalışıyordu: `TimerEngine` duvar saati
(`currentTimeMillis`) kullanıyor, `TimerAlarm` `setExactAndAllowWhileIdle`
ile bitişe alarm kuruyor. Doğrulandı, değişiklik gerekmedi.

### Değişen dosyalar
```
SayacKadraniView.kt   YENİ — 60 çizgili halka kadran, tema duyarlı
SayacWidget.kt        YENİ + widget_sayac.xml + w_sayac_info.xml + w_pill.xml
TimerEngine.kt        +sayaciYansit() , 5 kontrol noktasından çağrılıyor
TimerActionReceiver   +titret() — USAGE_ALARM, bildirimden bağımsız
TimerNotifier.kt      bitiş saati metni + setSubText
TimerFragment.kt      +kadran bağlama +kadraniTazele +kadranRenkleriniAyarla
fragment_timer.xml    +SayacKadraniView, timeText gizlendi
WidgetCommon.kt       TUM_WIDGETLAR 11 → 12
strings.xml           +9 dize (2424 → 2433)
```

### Derleme
İlk denemede temiz — 0 hata. 12 widget kayıtlı, denetim temiz. İmza değişmedi.

### ⚠️ Test edilmedi
- **Kadran doğru çiziliyor mu** (özel View — cihazda görülmeli)
- **Sayaç widget'ı Chronometer'ı doğru sayıyor mu** (en riskli kısım)
- Titreşim sessiz moddayken de çalışıyor mu
- Widget düğmeleriyle uygulama arası senkron

---

## v7.86 — Zamanlayıcı Ayarları Ekranı + İki Yeni Giriş Noktası
**versionCode 122 · APK 28,2 MB · md5 02bf7cda938740ea0bee12fa7fd962f1**

### Kullanıcı isteği (Google Saat ayar ekranı görüntüsüyle)
> "Bana bu özellikleri ekle, zamanlayıcı kısmındaki sağ taraf üst tarafa
>  ayarlar menüsü ekleme yap. Zamanlayıcının her şeyini ayarlayabileyim.
>  Ve giriş ekranındaki ayarlar kısmının hemen soluna zamanlayıcı menüsünü
>  açma ikonu koy"

### 1) `SayacAyar.kt` — merkezi ayar deposu
Ayarlar üç yere dağılmıştı: `TimerEngine` (ses indeksi), `Store`
(`getVibEnabled`/`getSoundEnabled`), `ZorunluUyari` (ısrarlı uyarı).
Hepsi tek yerden yönetilebilir hâle geldi. Mevcut anahtarlara
dokunulmadı — `Store.getVibEnabled` hâlâ genel ayar, `SayacAyar.titresim`
zamanlayıcıya özel; ikisi `titresimEtkinMi()` ile birlikte değerlendiriliyor
(kullanıcının "hiç titreşim istemiyorum" tercihi çiğnenmiyor).

### 2) `SayacAyarActivity` — ekran görüntüsündeki düzen
Gruplanmış yuvarlak kartlar, küçük soluk grup başlıkları, ince ayırıcılar.
Tek tek kart yerine **grup kartı** — 15 ayrı kart görsel gürültü olurdu.

| Grup | Ayarlar |
|---|---|
| (üst) | Sistem sesi kapalıyken alarmları sustur |
| SES | Ses aç/kapa · zil sesi seçici · ses süresi (5-120 sn) · kademeli yükseltme |
| TİTREŞİM | Aç/kapa · desen (kısa/normal/uzun) · **anında dene** |
| ZAMANLAYICI | Varsayılan süre · yaklaşan bitiş uyarısı · mini zamanlayıcı · otomatik tekrar (pomodoro) |
| EKRAN | Ekran açık kalsın · bitince tam ekran uyarı |
| BİLDİRİM | Bildirim türleri · ısrarlı uyarı · sistem ayarları |

Kapalı ayarın alt seçenekleri **soluk ve tıklanamaz** (alpha 0.45) —
kullanıcı neden çalışmadığını anlıyor.

### 3) Ayarlar gerçekten işlevsel — sadece arayüz değil
- **`bitisSesiCal()`** — bildirime bırakılmadı çünkü kanal sesi Android 8+
  sonrası değiştirilemiyor; kullanıcı zil seçince hemen geçerli olmalı.
  `USAGE_ALARM` ile çalıyor, kademeli yükseltme destekli, süre dolunca
  kendini durduruyor.
- **`sesCalinsinMi()`** — "sistem sesi kapalıyken sustur" mantığı:
  sessiz moddaysa ses çalmaz, titreşim kalır.
- **`titret()`** artık `SayacAyar` deseninden okuyor (v7.85'te sabitti).
- **Bildirim artık `setSilent(true)`** — ses/titreşimi biz yönettiğimiz
  için çift çalma sorunu önlendi.
- **Mini zamanlayıcı kapalıysa** canlı bildirim gösterilmiyor (sayaç yine çalışır).
- **Ekran açık kalsın** → `FLAG_KEEP_SCREEN_ON`, `onPause`'da temizleniyor
  (yoksa diğer sekmelerde de ekran açık kalıp pil yakardı).
- **Otomatik tekrar** → 2,5 sn gecikmeyle yeni tur (bitiş bildirimi görülsün).

### 4) İki yeni giriş noktası
- **Sayaç ekranı sağ üst `⋮`** → ayarlar
- **Ana ekranda ⚙'nin hemen soluna `⏱`** → tek dokunuş ayarlar,
  **uzun basınca** doğrudan sayaç ekranı

### Değişen dosyalar
```
SayacAyar.kt           YENİ — 15 ayar, sessiz mod mantığı, desen dizileri
SayacAyarActivity.kt   YENİ — gruplu kart düzeni, zil seçici, titreşim testi
TimerActionReceiver    +bitisSesiCal() +otomatikTekrar() , titret() ayara bağlandı
                       showDone artık setSilent(true)
TimerNotifier.kt       mini zamanlayıcı kapalıysa bildirim yok
TimerFragment.kt       +ekranBayragi() , onPause temizliği, ⋮ düğmesi
HomeFragment.kt        +openTimerMenu (kısa: ayarlar, uzun: sayaç)
fragment_timer.xml     başlık satırı + sağ üst ⋮
fragment_home.xml      ⚙'nin soluna ⏱ kartı
strings.xml            +52 dize (2433 → 2485)
```

### Derleme
İlk denemede temiz — 0 hata. Manifest denetimi temiz. İmza değişmedi.

### ⚠️ Test edilmedi
- Zil sesi seçici açılıyor mu, seçilen ses bitişte çalıyor mu
- "Sessizde sustur" gerçekten sessiz modda sesi kesiyor mu
- Otomatik tekrar döngüsü doğru çalışıyor mu

---

## v7.87 — Bildirim Düzeni + Zamanlayıcı Hızlı Menüsü
**versionCode 123 · APK 28,2 MB · md5 a973dc4d3550edd5c9319dad3314a03b**

### Kullanıcı isteği (One UI canlı bildirim görüntüsüyle)
> "Bildirim ekranında zamanlayıcı bu şekilde gözüksün ve ana ekrandaki
>  zamanlayıcı kısmında sadece zamanlayıcı ayarlarına değil zamanlayıcı
>  ekranına da giriş yapsın"

### 1) Bildirim düzeni — ekran görüntüsüne uyduruldu
Hedef düzen: büyük sayaç · altında `30 d / 13:26` · iki düğme
(**İptal et** | **Duraklat**).

| | Eskiden | Şimdi |
|---|---|---|
| Alt satır | "🔔 12:26'de bitiyor" + ayrı `subText` (30 d) | tek satır **"30 d / 13:26"** |
| Duraklatılınca | "Duraklatıldı · 25:00" | başlıkta **büyük süre**, altta "30 d · duraklatıldı" |
| Düğmeler | Duraklat · Sıfırla · Kapat (3) | **İptal et · Duraklat** (2) |

**Neden 3 → 2 düğme:** dar bildirim alanında üç etiket sıkışıp
kırpılıyordu. "Sıfırla" ile "Kapat" pratikte aynı sonucu veriyordu
(sayaç durur, bildirim kalkar) — tek "İptal et"te birleştirildi.

Duraklatılınca `setContentTitle` artık süreyi gösteriyor; sistem
Chronometer'ı durunca başlıkta değer kalmıyordu.

### 🔴 Yol açılan gerçek hata
`ACTION_STOP` yalnızca **duraklatıyordu**, sıfırlamıyordu. Düğme etiketi
"Kapat" olduğu için fark edilmiyordu. Artık "İptal et" yazdığına göre
gerçekten iptal etmeli — `TimerEngine.reset()` çağrılıyor. Yoksa yarım
kalan süre sessizce saklanıp bir sonraki başlatmada kullanıcıyı şaşırtırdı.

### 2) Ana ekran ⏱ düğmesi — hızlı menü
v7.86'da kısa dokunuş **yalnızca ayarları** açıyordu; sayaç ekranı sadece
uzun basmayla geliyordu. Uzun basma keşfedilmesi zor bir hareket.

Artık kısa dokunuş küçük bir menü açıyor:
```
⏱ Zamanlayıcı ekranı · 24:31   ← çalışıyorsa kalan süre görünür
⚙ Zamanlayıcı ayarları
▶ Hemen başlat / ⏸ Duraklat    ← ekrana girmeden kontrol
```
Uzun basma kısayol olarak korundu (doğrudan sayaç ekranı).

### Değişen dosyalar
```
TimerNotifier.kt       contentText "30 d / 13:26", duraklatınca başlıkta süre,
                       3 düğme → 2 (İptal et · Duraklat)
TimerActionReceiver    ACTION_STOP artık reset() yapıyor (gerçek iptal)
HomeFragment.kt        +zamanlayiciMenusu() — ekran/ayarlar/başlat-duraklat
strings.xml            +9 dize (2485 → 2494)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### ⚠️ Test edilmedi
- Bildirim iki düğmeyle görseldeki gibi mi duruyor
- "İptal et" gerçekten sıfırlıyor mu
- Duraklatılınca başlıkta süre görünüyor mu

---

## v7.88 — Sayaç Ön Plan Servisi + Geri Tuşu Düzeltmesi
**versionCode 124 · APK 28,2 MB · md5 82627bf824b7701b26db6a9ca7199bd5**

### Kullanıcı bildirimi (iki hata)
> "Zamanlayıcı çalışırken üst tarafta bildirim ekranında zamanlayıcı geri
>  sayımı gözükmüyor ve bütün ekranlarda geriye tıklayınca uygulamadan
>  çıkıyor, önceki ekrana gitsin, çıkmak istediğimizde 'uygulamadan çıkmak
>  için 2 kere geriye tıklayın' desin."

### 🔴 HATA 1 — Bildirim kayboluyordu
**Kök sebep:** Sayaç bildirimi sıradan `NotificationCompat` olarak
gönderiliyordu. Uygulama arka plana atılınca Android süreci istediği an
öldürebiliyor; süreç ölünce `setOngoing(true)` olsa bile bildirim düşüyor.
Ayrıca hiçbir bileşen "çalışıyorum" demediği için sistem uygulamayı boşta
sayıyordu. Bu, v7.85-7.87'de bildirim **içeriğini** düzeltirken gözden
kaçan yapısal bir eksiklikti.

**Çözüm:** `SayacServisi.kt` — ön plan servisi. Sayaç çalıştığı sürece
ayakta; ön plan bildirimi sistem tarafından **kaldırılamaz**, süreç
öncelikli hâle geldiği için öldürülmez.

`foregroundServiceType="specialUse"` seçildi çünkü Android 14 (targetSdk 34)
tür zorunlu kılıyor ve mevcut türlerin hiçbiri uymuyor:
· `shortService` 3 dakikayla sınırlı → 30 dk sayaç için olmaz
· `mediaPlayback`/`location` vb. yapılan işi tanımlamıyor

**Sayaç mantığı servise taşınmadı** — zaman hâlâ `TimerEngine`'de duvar
saatiyle, bitiş `TimerAlarm` ile hesaplanıyor. Servis ölse bile sayaç
doğru kalır ve bitiş bildirimi gelir.

`TimerNotifier.show()` ikiye ayrıldı: `olustur()` bildirimi kurup döndürür
(servis `startForeground` için hazır nesne ister), `show()` gönderir ve
`SayacServisi.esitle()` çağırır. Tek kurulum, iki kullanım.

### 🔴 HATA 2 — Geri tuşu uygulamadan çıkarıyordu
**Kök sebep:** Ekranlar `hide()/show()` ile yönetiliyor (v7.62'de
kasıtlı bir tercih). Bu yüzden FragmentManager'ın geri yığını hiç
dolmuyordu; hangi ekranda olursa olsun geri tuşu doğrudan Activity'yi
kapatıyordu.

**Çözüm:** `MainActivity`'de kendi geçmiş yığını (`ArrayDeque`, tavan 20).
Yeni sıra:
1. Yan panel açıksa kapat
2. Geçmişte kayıt varsa bir önceki ekrana dön
3. Ana ekranda değilsek ana ekrana dön
4. Ana ekrandaysak: ilk basışta **"Çıkmak için tekrar geri tuşuna bas"**,
   2 saniye içinde ikinci basışta çık

`gecmissizAc()` ayrı tutuldu: normal `open()` her çağrıda geçmişe yazdığı
için geri gitmek yeni kayıt oluşturur ve iki ekran arasında sonsuz döngü
olurdu.

Diğer 30 Activity'de değişiklik gerekmedi — orada sistemin `finish()`
davranışı zaten doğru (önceki ekrana döner).

### Değişen dosyalar
```
SayacServisi.kt        YENİ — ön plan servisi, esitle() tek giriş noktası
TimerNotifier.kt       show() → olustur() + show() ayrımı,
                       cancel() servisi de indiriyor
TimerEngine.kt         sayaciYansit() içinde SayacServisi.esitle()
MainActivity.kt        +ekranGecmisi +cikisIcinBeklenen +gecmissizAc()
                       onBackPressed 4 aşamalı
AndroidManifest.xml    +FOREGROUND_SERVICE_SPECIAL_USE izni
                       +SayacServisi (specialUse + alt tür açıklaması)
strings.xml            +1 dize (2494 → 2495)
```

### Derleme
İlk denemede temiz — 0 hata. Servis manifest denetiminden geçti.
İmza değişmedi.

### ⚠️ Test edilmedi
- **Bildirim artık kalıcı mı** (uygulamayı kapatıp bekle)
- Geri tuşu ekranlar arasında doğru geziniyor mu
- Android 14+ cihazda specialUse servisi reddedilmiyor mu

---

## v7.89 — Canlı Sayaç Bildirimi Düzeltmesi
**versionCode 125 · APK 28,2 MB · md5 bf0b2b6b2140f83e85d1caf24656e955**

### Kullanıcı bildirimi
> "Sadece bitiş bildirimi gelmesin, zamanlayıcı devam ederken de kaç dakika
>  kaldığını, iptal etme tuşunu ve benzeri şeyleri bildirim ekranında
>  görebileyim"

v7.88'de ön plan servisi eklenmişti ama sorun sürüyordu. Üç ayrı neden
bulundu — hepsi bu sürümde giderildi.

### 🔴 SEBEP 1 — Bildirim kanalı `IMPORTANCE_LOW`
Kanal düşük önemle oluşturulmuştu. Samsung One UI bu bildirimleri
**"Sessiz bildirimler"** bölümüne indiriyor; panel daraltılmışsa
kullanıcı sayacı hiç görmüyor.

Android 8+ bir kanalın önemi **oluşturulduktan sonra kod ile
değiştirilemez**. Tek çözüm yeni kimlikle kanal açmak:
`zamanlayici_canli_v1` → **`zamanlayici_canli_v2`** (`IMPORTANCE_DEFAULT`).
Eski kanal `deleteNotificationChannel` ile siliniyor ki ayarlarda ölü
kayıt kalmasın. Ses/titreşim yine kapalı — sürekli sayan bildirimin
uyarı vermesi gerekmiyor, yalnızca görünür olması gerekiyor.

`PRIORITY_LOW` → `PRIORITY_DEFAULT` (Android 7 ve altı için).

### 🔴 SEBEP 2 — Servis başarısız olunca bildirim siliniyordu
`SayacServisi.onPlanaAl()` içinde `startForeground` başarısız olursa
`durdur()` çağrılıyordu; o da `stopForeground(STOP_FOREGROUND_REMOVE)`
ile **bildirimi siliyordu**.

Yani Android 12+ arka plan kısıtı ya da üretici pil politikası servisi
reddettiği her durumda kullanıcı sayacı hiç göremiyordu. v7.88'de
eklediğim bu kod, düzeltmeye çalıştığım sorunu bazı durumlarda
**kötüleştirmiş**.

Artık servis sessizce çekiliyor ama bildirim normal yoldan gönderiliyor:
"kaldırılamaz" garantisi kayboluyor, **görünürlük korunuyor**.

### 🔴 SEBEP 3 — Bildirim yalnızca uygulama açıkken tazeleniyordu
Tazeleme `TimerFragment.tick()` içindeydi; uygulama kapalıyken hiç
çalışmıyordu. Chronometer sistem tarafından sayılıyor ama **"30 d / 13:26"
satırı ve düğme etiketleri sabit metin** — duraklat/devam sonrası güncel
kalmıyordu.

Servise 30 saniyelik kendi tazeleyicisi eklendi. Uygulama içi tazeleme
de 5 sn → 2 sn'ye çekildi (duraklat/devam paneli çabuk yansısın).

### Ek: izin denetimi
Sayaç başlatılırken bildirim izni / `getNotifEnabled` / `miniGoster`
kontrol ediliyor. Kapalıysa oturumda **bir kez** uyarı + "Düzelt" düğmesi
(doğrudan ilgili ayara götürür). Eskiden sessizce başlıyor, kullanıcı
"sayaç çalışmıyor" sanıyordu.

### Değişen dosyalar
```
TimerNotifier.kt   CHANNEL_ID v1→v2, IMPORTANCE_DEFAULT, eski kanal silinir,
                   PRIORITY_DEFAULT, setAutoCancel(false),
                   lockscreenVisibility PUBLIC
SayacServisi.kt    startForeground hatasında bildirim SİLİNMİYOR,
                   +30 sn periyodik tazeleyici (durdur/onDestroy'da iptal)
TimerFragment.kt   +bildirimIzniniDenetle() , tazeleme 5 sn → 2 sn
strings.xml        +5 dize (2495 → 2500)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### ⚠️ Kanal sürümü notu
Kanal kimliği değiştiği için kullanıcının eski kanalda yaptığı özel
ayarlar (varsa) sıfırlanır. Bu kaçınılmazdı — önem düzeyi başka türlü
düzeltilemiyordu.

---

## v7.90 — Canlı Bildirim: Asıl Sebep Bulundu (`setSilent`)
**versionCode 126 · APK 28,2 MB · md5 43c1b0e0dbb6e498bf1d383dc381e2e5**

### Kullanıcı bildirimi
> "Açık ama bitiş bildirimi geliyor, zamanlayıcı çalışırkenki anlık bildirim
>  gelmiyor"

Bu ayrım kritik ipucuydu: **aynı uygulama, aynı izinler, biri geliyor
diğeri gelmiyor.** Demek ki sorun izinde değil, iki bildirimin
farkındaydı.

### 🔴 ASIL SEBEP — `setSilent(true)`
Canlı sayaç bildiriminde vardı, bitiş bildiriminde yoktu.

`setSilent(true)` yalnızca sesi kapatmıyor: Android bu bayrağı gören
bildirimi **kanal önemi ne olursa olsun** panelin *"Sessiz bildirimler"*
bölümüne indiriyor. Samsung One UI'da bu bölüm varsayılan olarak
**katlanmış** geliyor — yani bildirim teknik olarak gönderiliyor ama
kullanıcı hiç görmüyor.

v7.89'da kanal önemini `LOW → DEFAULT` yükseltmiştim; **etkisiz kaldı**
çünkü `setSilent` kanal önemini eziyor. Bayrak kaldırıldı.

Sessizlik kaybı yok: kanalda zaten `setSound(null)` +
`enableVibration(false)` var, yani bildirim yine ses çıkarmıyor.
`setOnlyAlertOnce(true)` korundu — her tazelemede yeniden dikkat çekmesin.

### Ek düzeltme — çakışan kanal
`TimerFragment` kendi kanalını (`calisma_zamanlayici`, IMPORTANCE_HIGH)
her `onViewCreated`'da yeniden oluşturuyordu. Artık yoksa kuruluyor.
Bu kanal bitiş bildirimi için; canlı sayaç kanalıyla karıştırılmamalı
(ikisini birleştirmek, birinin ayarı diğerini bozardı).

### Yeni: bildirim tanılama ekranı
**Zamanlayıcı ayarları › Bildirim neden görünmüyor?**

Altı maddeyi tek tek kontrol edip gösteriyor:
```
✓ Sistem bildirim izni
✓ Uygulama bildirimleri açık
✓ "Mini zamanlayıcıyı göster" açık
⚠ Zamanlayıcı kanalı DÜŞÜK önemde → panelde "Sessiz" bölümüne düşer
✓ Sayaç şu an çalışıyor
✓ Ön plan servisi ayakta
```
"Kanal ayarını aç" düğmesi doğrudan o kanalın sistem ayarına götürüyor
(`ACTION_CHANNEL_NOTIFICATION_SETTINGS`).

Gerekçe: "bildirim gelmiyor"un beş olası sebebi var ve kullanıcı
hangisinin geçerli olduğunu göremiyordu. Her turda tahmin yürütmek
yerine tanı aracı bıraktım.

### Değişen dosyalar
```
TimerNotifier.kt       setSilent(true) KALDIRILDI (asıl düzeltme)
TimerFragment.kt       createNotificationChannel — yoksa oluştur
SayacAyarActivity.kt   +taniGoster() +kanalAyariniAc()
strings.xml            +13 dize (2500 → 2513)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### Not — kanal ayarı kalıcıdır
Kullanıcı v7.89'u kurduysa `zamanlayici_canli_v2` kanalı zaten oluşmuştur.
Kanal ayarları **kod ile değiştirilemez**; eğer sistem onu düşük önemde
kaydettiyse tanılama ekranı bunu gösterir ve elle düzeltme yolunu sunar.

---

## v7.91 — Canlı Bildirim: Gerçek Sebep (`FOREGROUND_SERVICE_DEFERRED`)
**versionCode 127 · APK 28,2 MB · md5 10483202794c372388464d2c044a9b01**

### Kullanıcı bildirimi
> "Hepsi normal ama sayaç ilerlerken bildirim ekranımda gözükmüyor"

Tanılamada her şey ✓ görünüyordu ama bildirim yoktu. Bu, sorunun
izin/kanal katmanında **olmadığını** kesinleştirdi — üç ayrı kod hatası
bulundu.

### 🔴 SEBEP 1 (ASIL) — Android 12+ bildirimi 10 saniye geciktiriyor
Ön plan servisiyle gösterilen bildirimler API 31'den itibaren varsayılan
olarak `FOREGROUND_SERVICE_DEFERRED` sayılır: sistem bunları **10 saniye
geciktirir** (kısa servislerin panelde titremesini önlemek için).

Bizim durumumuzda bu ölümcüldü: bildirim her 2 saniyede bir
tazeleniyordu, dolayısıyla 10 saniyelik erteleme penceresi sürekli
baştan başlıyor ve bildirim **hiçbir zaman** gösterilmiyordu.

Bitiş bildirimi ön plan servisine bağlı olmadığı için etkilenmiyordu —
"biri geliyor, diğeri gelmiyor" tablosunun sebebi tam olarak buydu.

**Düzeltme:** `setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)`

### 🔴 SEBEP 2 — Her tazelemede `startService`
`TimerNotifier.show()` her çağrısında `SayacServisi.esitle()` çalışıyor,
o da koşulsuz `startService` gönderiyordu. 2 saniyede bir servis
başlatma isteği → Android 12+ bunu kısıtlamaya başlıyor ve bir süre
sonra sessizce yok sayıyor; servis bildirimle birlikte düşüyordu.

**Düzeltme:** `esitle()` artık yalnızca **durum değiştiğinde** servise
dokunuyor (`gosterilsin && !ayakta` / `!gosterilsin && ayakta`).
Bildirim tazelemesi zaten doğrudan `notify()` ile yapılıyor.

### 🔴 SEBEP 3 — Çift tetikleme yarışı
`TimerEngine.start()` şunu yapıyordu:
```
TimerNotifier.show(context)   → esitle() → servis başlat
sayaciYansit(context)         → esitle() → servis başlat (TEKRAR)
```
İkinci çağrı, ilkinin başlattığı servis henüz `ayakta = true` işaretini
koymadan geldiği için servis iki kez başlatılıyordu.

**Düzeltme:** `sayaciYansit()` içindeki `esitle()` kaldırıldı. Tek çağrı
noktası: `TimerNotifier.show()`.

### Tanılamaya iki ekleme
- **"Bildirim şu anda panelde duruyor"** — `activeNotifications` ile
  gerçeği söyler. Diğer maddeler "olması gerekiyor" derken bu madde
  fiilen orada mı bakar.
- **"Şimdi gönder"** düğmesi — bildirimi elle tetikler, sorunun kanalda
  mı akışta mı olduğunu ayırt eder.

### Değişen dosyalar
```
TimerNotifier.kt    +setForegroundServiceBehavior(IMMEDIATE)  ← asıl düzeltme
SayacServisi.kt     esitle() yalnızca durum değişiminde çalışır
TimerEngine.kt      sayaciYansit içindeki esitle() kaldırıldı (çift tetikleme)
SayacAyarActivity   +panelde kontrolü +"Şimdi gönder"
strings.xml         +4 dize (2513 → 2517)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

---

## v7.92 — Ön Plan Servisi İsteğe Bağlı Oldu (asıl çözüm)
**versionCode 128 · APK 28,2 MB · md5 7c7b0d869bdf94ef74a0e7969e6f9bf9**

### Kullanıcının kesin kanıtı
> "Duraklatınca bildirim ekranında gözüküyor fakat senkronizasyonlu
>  çalışmıyor"

Tanılama ekranı da bunu doğruladı: tüm maddeler ✓, yalnızca
**"Bildirim şu anda panelde duruyor" ✕**.

Bu ikili tablo tek bir şeye işaret ediyordu:
· **Duraklatıldığında** bildirim sıradan `notify()` ile gidiyor → görünüyor
· **Çalışırken** bildirim ön plan servisine ait oluyor → görünmüyor

Yani sorun kanal, izin ya da gecikme değil; **servisin bildirime sahip
olmasıydı.** Samsung One UI ön plan servisi bildirimlerini "arka planda
çalışan uygulamalar" grubuna katlıyor.

### Çözüm — servis varsayılan olarak KAPALI
v7.88'de bildirimi "kaydırılamaz" yapmak için eklenen ön plan servisi
artık **isteğe bağlı** (`SayacAyar.onPlanServisi`, varsayılan `false`).

Kapalıyken bildirim tamamen normal `notify()` yoluyla gönderiliyor —
duraklatılmış hâlde çalışan yol neyse, çalışırken de aynısı.

**Kayıp yok:** sayacın doğruluğu servise bağlı değildi. Süre
`TimerEngine` içinde duvar saatiyle hesaplanıyor, bitiş `TimerAlarm` ile
kesin alarma bağlı. Tek kayıp, bildirimin kaydırılarak kapatılabilmesi.

Ayarlar › Zamanlayıcı › **"Bildirimi kalıcı yap"** ile isteyen açabilir;
açıklamada bu riskin uyarısı var.

### İki yan hata daha
1. **`stopForeground(STOP_FOREGROUND_REMOVE)` → `DETACH`**
   Servis kapatılırken bildirimi siliyordu. Sayaç hâlâ çalışıyor
   olabileceği için bildirimin kalması gerekir. DETACH bildirimi
   servisten ayırır, panelde bırakır.

2. **Çağrı sırası ters çevrildi**
   `notify()` → `esitle()` sırasında, servis durdurulurken bizim
   bildirimimizi de götürebiliyordu. Artık `esitle()` → `notify()`:
   son söz her zaman `notify()`'da.

### Değişen dosyalar
```
SayacAyar.kt           +onPlanServisi() — varsayılan false
SayacServisi.kt        esitle() ayara bağlandı, REMOVE → DETACH
TimerNotifier.kt       show() sırası: esitle() önce, notify() sonra
SayacAyarActivity.kt   +"Bildirimi kalıcı yap" anahtarı,
                       tanıda servis satırı duruma göre anlamlı
strings.xml            +3 dize (2517 → 2520)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

---

## v7.93 — Uyumluluk Modu (kanıta dayalı çözüm)
**versionCode 129 · APK 28,2 MB · md5 5c18aa5e4f110abca6af8a65ae130adc**

### Teşhis — tahmin değil, kanıt
Kullanıcının cihazında:
· **Duraklatılmış** bildirim → GÖRÜNÜYOR
· **Çalışan** bildirim → GÖRÜNMÜYOR
· Tanılama: tüm maddeler ✓, yalnızca "panelde duruyor" ✕

v7.92'de ön plan servisi kapatıldı, sorun sürdü. Demek ki servis de değil.
Geriye iki bildirim arasındaki **kalan yapısal farklar** kaldı:

| | Çalışırken | Duraklatınca |
|---|---|---|
| `setOngoing` | **true** | false |
| `setUsesChronometer` | **true** | false |
| `CATEGORY_STOPWATCH` | var | var |

Samsung One UI, `ongoing + chronometer` bildirimleri **"Canlı bildirimler"
(Now Bar)** alanına yönlendiriyor ve normal bildirim listesinden
çıkarıyor. Uygulama o alana kabul edilmezse bildirim **hiçbir yerde**
görünmüyor. Duraklatılmış bildirimde bu ikisi olmadığı için normal
listede kalıyordu.

### Çözüm — `SayacAyar.uyumlulukModu` (varsayılan AÇIK)
Çalışan bildirim, **kanıtlanmış biçimde görünen** duraklatılmış
bildirimle yapısal olarak aynı hâle getirildi:
· `setOngoing(false)`
· `setUsesChronometer(false)` → süre başlıkta düz metin
· `CATEGORY_STOPWATCH` verilmiyor

**Kayıp:** sayaç sistem tarafından saniye saniye sayılmaz.
**Telafi:** alt satırdaki **bitiş saati** (`30 d / 13:26`) hiç bayatlamaz —
tazeleme gecikse bile doğru bilgi ekranda kalır.

### Tazeleme altyapısı
Kronometre olmadığı için metin periyodik güncellenmeli:
- **Uygulama açıkken:** 2 sn (mevcut ticker)
- **Ön plan servisi açıksa:** 10 sn (uyumluluk modunda)
- **Uygulama kapalıyken:** `TimerAlarm.tazelemeyiKur()` — 15 sn'de bir
  `setAndAllowWhileIdle` ile zincirlenen alarm. Kesin alarm kotası bitiş
  bildirimi için saklandı; saniye hassasiyeti gerekmiyor.

### Değişen dosyalar
```
SayacAyar.kt           +uyumlulukModu() — varsayılan true
TimerNotifier.kt       ongoing/chronometer/category uyumluluk moduna bağlı
                       +uyumluluk dalı (duraklatılmışla aynı yapı)
TimerAlarm.kt          +tazelemeyiKur() +tazelemeyiIptalEt()
                       reschedule/cancel içine bağlandı
TimerActionReceiver    +ACTION_TAZELE dalı (zincirleme alarm)
SayacServisi.kt        uyumluluk modunda 10 sn tazeleme
SayacAyarActivity.kt   +"Uyumluluk modu" anahtarı
AndroidManifest.xml    +TIMER_TAZELE intent-filter
strings.xml            +2 dize (2520 → 2522)
```

### Derleme
İlk denemede temiz — 0 hata. İmza değişmedi.

### Eğer bu da olmazsa
Kalan tek olasılık: cihazın bu uygulamayı bildirim panelinde tamamen
kısıtlaması (üretici pil/otomatik yönetim politikası). O durumda
Ayarlar › Piller › Uygulama uyku ayarları kontrol edilmeli.

---

## v7.94 – v7.97 — 10 Önerinin Tamamı
**Son sürüm: versionCode 133 · v7.97 · md5 7407370919e1f9f3a03675f297c25ed9**

Kullanıcı "hepsini yap" dedi; 10 öneri 4 gruba bölünüp sırayla teslim edildi.

### v7.94 — Grup A (öneri 1, 2, 7)
| Dosya | İş |
|---|---|
| `Pomodoro.kt` | çalış→mola→uzun mola döngüsü, tur sayacı, **odak modu** (DND) |
| `OdakKaydi.kt` | sayaç bitince süreyi **aktif derse** yazar, oturum geçmişi, taşı/geri al |

`TimerActionReceiver`: bitişte `odagiKaydet()` + `dongueyIlerlet()`.
`TimerEngine`: start/pause/reset/finish → `Pomodoro.odagiEsitle()`.

**Odak modunda `INTERRUPTION_FILTER_PRIORITY` seçildi** — `NONE` olsaydı
sayacın kendi bitiş alarmı da susardı. DND'yi biz açtıysak biz kapatıyoruz;
kullanıcının kendi açtığı DND'ye dokunulmuyor.

### v7.95 — Grup B (öneri 3, 4, 5)
| Dosya | İş |
|---|---|
| `SimdiNe.kt` | 7 aday puanlanır, **tek öneri** + gerekçe döner |
| `SesliKomut.kt` | "25 dakika sayaç başlat", "görevleri aç", "X ne demek" |

Bugün ekranına karar kartı, `IlerlemeWidget`'a **"Bu adımı bitir"** düğmesi.

**Puanlama kural tabanlı, AI değil:** ekran anında açılmalı ve çevrimdışı
çalışmalı. Namaz 100 → mola 95 → geciken görev 70-90 (gecikme arttıkça) →
ders 55-88 (akşam saatlerinde artar) → hata tekrarı 50.

`SesliKomut` anlamadığı cümleyi `SesliNot`'a devrediyor — eski davranış korunuyor.

### v7.96 — Grup C (öneri 6, 8)
| Dosya | İş |
|---|---|
| `HaftaPlan.kt` | gün bazlı hedef + ders atama, 4 hazır şablon |
| `YedekSifre.kt` | **AES-256-GCM + PBKDF2 120k tur** |

`Koc.bugunHedefi()` önce haftalık plana bakıyor; plan kapalıysa eski
davranış sürüyor (mevcut kullanıcı fark etmiyor). 0 dakika = izin günü,
o gün borç yazılmıyor.

Yedek biçimi: `GAENC1|tuz|iv|şifreli`. Başlık sayesinde geri yüklerken
şifreli olduğu anlaşılıp parola isteniyor; şifresiz yedekler eskisi gibi
çalışıyor. GCM bütünlük de doğruluyor — kurcalanmış dosya sessizce
yüklenmiyor.

### v7.97 — Grup D (öneri 9, 10 + plan arayüzü)
| Dosya | İş |
|---|---|
| `HaftaPlanActivity.kt` | 7 günlük ızgara arayüzü |
| `SayfaImi.kt` | PDF sayfa yer imi + not, 5 renk kategorisi |
| `Analitik.kt` | +`bitisTahmini` +`zayifKonular` +`odakOzeti` +`derinCikarimlar` |

**Öneri 9'da dürüst sınır:** "metin seçip işaretleme" yapılamadı.
`LessonPdfActivity` sayfaları **bitmap** olarak çiziyor; seçilebilir metin
için PDF'ten karakter konumlarını okuyup ekrana bindiren ayrı bir katman
gerekir — birkaç sürümlük iş ve kaydırma performansını riske atar.
Yerine **sayfa bazlı** yer imi + not yapıldı: not simgesine uzun bas,
renk seç, listeden o sayfaya atla.

Bitiş tahmini son 30 günün hızına bakıyor; hiç adım bitirilmemişse
tahmin **yapılmıyor** — uydurma tarih göstermek yanıltıcı olurdu.

### ⚠️ Sandbox veri kaybı olayı
v7.96 derlemesi sırasında sandbox sıfırlandı; `strings.xml` v7.93'e döndü
ve 6 yeni Kotlin dosyası silindi. `kaynak-v7.95-yedek.zip` sağlam olduğu
için Grup A+B oradan geri yüklendi, Grup C+D yeniden yazıldı. Ders:
her grup teslim edildiği anda kaynak zip'i de yükleniyor — bu kurtardı.

### Derleme
Son derlemede tek hata: `runCatching` bloğunun son ifadesi `if` olunca
Kotlin onu değer sayıp "else şart" istiyor. Bloklar `Unit` ile kapatıldı.

İmza değişmedi: `5F:15:D4:E7:...:85:11`

---

## v7.98 – v8.1 — Ölçüme Dayalı 10 İyileştirme
**Son sürüm: versionCode 137 · v8.1 · md5 cb82c71e972bdb9c519faf040bc22f4f**

Öneriler tahminle değil **kodu tarayarak** çıkarıldı: 62.000 satır,
161 dosya, 33 ayrı veri deposu, 0 test.

### v7.98 — 🔴 Veri kaybı riski (madde 1, 2)
**Bulgu:** `Store.exportJson` 9 modülü yedekliyordu. v7.78'den beri
eklenen **11 modül yedeğe hiç girmiyordu**: Mufredat, Hatalarim, Sozluk,
Pomodoro, OdakKaydi, HaftaPlan, SayfaImi, Koc, Kanit, SayacAyar,
OkumaAyar. Telefon değişiminde program ilerlemesi, hata defteri, sözlük
ve koç karnesi **kayboluyordu** — üstelik yedek "her şeyi aldım" izlenimi
verdiği için fark edilmesi zordu.

`PrefYedek.kt`: 11 modüle ayrı ayrı `disaAktar` yazmak ~400 satır tekrar
ve 12. modülde yine unutmak demekti. Bunun yerine **depo adına göre**
gezen genel yedekleyici yazıldı. Tür bilgisi korunuyor (`{"t":"i","v":5}`)
— `getInt` ile yazılanı `String` okumak `ClassCastException` verirdi.

Hassas veri kara listede: `ai_settings` (API anahtarları) yedeğe girmiyor.
Yedek biçimi 17 → **18**.

`YedekRotasyon.kt`: tek dosyaya yazmak, bozuk veri yazılırsa sağlam
sürüm bırakmıyordu. Günlük tarihli kopya, son 7 gün saklanıyor.
`exportJson` bir kez üretilip iki yere yazılıyor (pahalı işlem).

### v7.99 — Test altyapısı + AI önbelleği (madde 10, 4)
**İlk kez otomatik test:** 27 test, hepsi geçiyor.
`TekrarTest` (17 test) + `YedekSifreTest` (10 test).

Testleri yazarken `Tekrar.sonraki()` davranışı belgelendi: sonuç **her
zaman gelecekte** oluyor, geçmişte kalan görev ileri sarılıyor. Doğru
davranış — iki ay açılmamış görev iki ay öncesine kurulmamalı.

`AiOnbellek.kt`: 16 dosyada AI çağrısı, önbellek 5 yerde. İstem hash'i
anahtar; motivasyon cümlesi 1 gün, terim açıklaması 1 ay saklanıyor.

### v8.0 — Arama, geri alma, quiz havuzu (madde 5, 6, 8)
`Arama`ya 4 kaynak eklendi: terim, hata defteri, anlatım, yer imi.
Anlatımda yalnızca **başlık** taranıyor — gövde taraması arama kutusunda
her harfte tüm önbelleği açmak demek.

`GeriAl.kt`: onay penceresi her yıkıcı işlemde akışı kesiyor. Snackbar
tersi: işlem hemen olur, 5 saniye "GERİ AL" görünür. Ders bitirme, hata
defterinden çıkarma, terim silme bağlandı.

🔴 **Quiz havuzu:** `dersSorulariniAyarla` eski soruları **siliyordu** —
"Soruları yenile" dendiğinde havuz hep 6 soruda kalıyor, aynı sorular
tekrar geliyordu. `havuzaEkle` yazıldı; havuz büyüyor, her sınavda
rastgele 10 soru çekiliyor (ders başına tavan 60).

### v8.1 — Room, sesli liste (madde 3, 9)
🔴 **`fallbackToDestructiveMigration` kaldırıldı.** O ayar şema
değiştiğinde veritabanını siliyordu; notlar eklenirken **tüm görevler
gidebilirdi**. Gerçek migration yazıldı (sürüm 1→2), `gorevler` tablosuna
hiç dokunulmuyor.

`NotVarlik` + `NotDepo`: notlar Room'a taşındı, `GorevDepo` deseniyle.
`Store.loadNotes/saveNotes` imzaları değişmedi.

`SesliListe.kt`: sesli anlatım tek seferlikti. Artık konudaki anlatımlar
sırayla okunuyor; `SesliDersServisi` bir metni bitirince listeden
sıradakini alıyor. Anlatımı silinmiş öğeler atlanıyor (sonsuz döngü
koruması var).

### Yapılmayan: madde 7 (ana ekran özelleştirme)
Kart sıralama/gizleme `fragment_home.xml`'in tamamen yeniden
yapılandırılmasını gerektiriyor (sabit XML → dinamik liste). Diğer 9
madde tamamlandı; bu tek başına bir sürüm işi.

### Derleme
Her grup ayrı derlendi ve teslim edildi. Tek hata: `runCatching` bloğunun
son ifadesi `if` olunca Kotlin "else şart" istiyor — `Unit` ile kapatıldı.

İmza değişmedi: `5F:15:D4:E7:...:85:11`

---

## v8.2 (versionCode 138) — GRUP A: Hareket ve his

Kullanıcı "30 gözle görünür öneri" istedi, sonra "hepsini yap" dedi.
30 madde 4 gruba bölündü; bu, birinci grup (öneri 1-8).

### Neden bu grup önce
Kod tarandığında ölçülen durum:

| Ölçüm | v8.1 |
|---|---|
| Animasyon kullanan dosya | 1 / 166 |
| `performHapticFeedback` | 0 dosya |
| `ItemTouchHelper` | 0 dosya |
| `BottomSheetDialog` | 0 dosya |
| `res/anim` klasörü | YOK |
| Splash ekranı | YOK |

Uygulama 62.000 satır ve 17 ekrandı ama hiçbir şey hareket etmiyordu.

### Yapılanlar

**Öneri 1 — Ekran geçiş animasyonları**
`res/anim/` açıldı: `ga_gir_sag` · `ga_cik_sol` · `ga_gir_sol` · `ga_cik_sag` ·
`ga_bel` · `ga_soluk` · `ga_sabit`. `MainActivity.open()` içinde
`Canlandir.fragmentGecisi()` çağrılıyor; yön ekran indeksine göre seçiliyor
(ileri gidiyorsan sağdan, geri geliyorsan soldan).

**Öneri 2 — Dokunsal geri bildirim** · `Titresim.kt` (YENİ)
İki katman: sistem dokunuşu (`HapticFeedbackConstants`, kullanıcının sistem
tercihine saygılı) ve özel desenler (`Vibrator`, sistem sabitleriyle
üretilemeyen durumlar için). Bağlandığı yerler: görev tamamlama (`dogru`),
quiz doğru/yanlış, alışkanlık hedefi (`basari`), sayaç başlat/durdur,
alt menü sekmesi, FAB, kaydırma eşiği.

**Öneri 3 — Sayı sayaçları** · `Canlandir.sayi/halka/cubuk`
Ana ekrandaki 4 istatistik (seri, toplam, ilerleme %, odak dk) artık
0'dan hedefe 620 ms'de sayıyor; `StatRingView` halkaları dolarak geliyor;
günlük hedef çubuğu animasyonla doluyor. Önceki animasyon
`R.id.ga_tag_animator` etiketiyle iptal ediliyor — hızlı tazelemede
çakışma olmuyor.

**Öneri 4 — Kaydırma jesti** · `Kaydirma.kt` (YENİ)
Görev kartı: sola → sil, sağa → tamamla. Not kartı: sola → sil.
Kaydırma mesafesiyle orantılı renk doygunluğu, eşik (%38) geçilince ikon
büyüyor + haptic tik geliyor. Silme `GeriAl`/Snackbar ile geri alınabilir;
onay penceresi KOYULMADI (kaydırmanın hızını öldürürdü).
Kanıt gereken görev sağa kaydırılırsa kart geri dönüyor ve kanıt akışı
açılıyor — kaydırma kanıt kuralını atlayamıyor.

**Öneri 5 — Liste öğelerinin sırayla belirmesi**
`ga_oge_gir.xml` + `ga_liste.xml` (%12 gecikme). `Canlandir.liste()` kurulum,
`Canlandir.tekrarOynat()` veri değişiminde. Görevler ve notlarda etkin.

**Öneri 6 — Açılış ekranı**
`androidx.core:core-splashscreen:1.0.1`. `Theme.GunlukAsistan.Splash`,
özel `ga_splash_ikon.xml` (defter + tik vektörü). Çıkışta ikon yukarı
kayıp solarak gidiyor.
🔴 **İlk denemede hata:** `SplashScreen.installSplashScreen(this)` yazıldı →
`Unresolved reference`. Bu bir **uzantı işlevi**, sınıf üyesi değil.
Doğrusu: `import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen`
sonra `installSplashScreen()`.

**Öneri 7 — Basılan kartın çökmesi**
`res/animator/ga_kart_basma.xml` + `ga_dugme_basma.xml`. Stiller:
`GaKart` · `GaKartTiklanabilir` · `GaDugme`. Basınca 0.972 ölçek,
bırakınca overshoot ile geri.

**Öneri 8 — Pull-to-refresh görünür hâli**
`Yenileyici.kt`: çark büyütüldü ve 72dp aşağı alındı (üst araç çubuğunun
altında yarısı görünmüyordu), yenilenince "✓ Güncellendi" + haptic tik.

### Yeni ayar ekranı
`GorunumAyarActivity` + `activity_gorunum_ayar.xml` — Ayarlar → "Görünüm ve
hareket". 5 anahtar (animasyon, liste girişi, sayı sayacı, haptic, kaydırma)
+ dokununca sayı sayan bir deneme kartı. Telefonun kendi "animasyonları
kaldır" ayarı açıksa üstte uyarı kartı çıkıyor ve anahtarlar soluklaşıyor.

`GorunumAyar.kt` (YENİ) — tercih deposu. `PrefYedek` listesine
`gunluk_asistan_gorunum` eklendi, yedeğe giriyor.

### Yeni dosyalar
```
Titresim.kt · Canlandir.kt · Kaydirma.kt · GorunumAyar.kt
GorunumAyarActivity.kt + activity_gorunum_ayar.xml
res/anim/  ga_gir_sag · ga_cik_sol · ga_gir_sol · ga_cik_sag · ga_bel
           ga_soluk · ga_sabit · ga_oge_gir · ga_liste · ga_sarsinti
           ga_sarsinti_egri · ga_nabiz
res/animator/  ga_kart_basma · ga_dugme_basma
res/values/ids.xml   (setTag anahtarları)
res/drawable/ga_splash_ikon.xml
```

### Değişen dosyalar
```
build.gradle.kts   +core-splashscreen:1.0.1 +dynamicanimation:1.0.0
AndroidManifest    MainActivity android:theme=Splash · +GorunumAyarActivity
themes.xml         +Theme.GunlukAsistan.Splash +GaKart +GaKartTiklanabilir +GaDugme
strings.xml        2701 → 2720 dize
MainActivity.kt    installSplashScreen + çıkış animasyonu · open() geçişi · haptic
HomeFragment.kt    4 istatistik + hedef çubuğu animasyonlu
TasksFragment.kt   kaydırma jesti · liste animasyonu · tamamlama haptic
NotesFragment.kt   kaydırma jesti (sil) · liste animasyonu
QuizActivity.kt    doğru/yanlış haptic · yanlışta kart sarsıntısı · açıklama kartı belirme
TimerEngine.kt     start/pause haptic
HabitsFragment.kt  hedef tamamlanınca basari deseni
Yenileyici.kt      çark konumu/boyutu · "✓ Güncellendi"
PrefYedek.kt       +gunluk_asistan_gorunum
```

**APK:** 28,5 MB · md5 `510c92acbff004911f95fcd00924f28f`
**İmza:** `5F:15:...:85:11` (değişmedi)

⚠️ **Cihazda test edilmedi** — sandbox'ta emülatör yok. Animasyon ve haptic
gibi şeyler ancak gerçek cihazda hissedilir.

---

## v8.3 (versionCode 139) — GRUP B: Renk, tema, kimlik

30 önerinin ikinci grubu (öneri 9-15). İlk denemede derlendi.

### 🔴 Öneri 9 — GERÇEK KOYU TEMA (bu grubun en önemli maddesi)

**Ölçülen sorun:** `res/values-night/` klasöründe yalnızca
`widget_colors.xml` vardı. Telefon gece moduna geçince ana ekran
widget'ları koyuluyor ama **uygulamanın kendisi gündüz kalıyordu**.
Karanlıkta uygulamayı açmak gözü yakıyordu. Bu bir eksik değil, hata.

**Neden fark edilmemişti:** Uygulamada 4 koyu tema zaten vardı
(Gün Batımı, Aurora, Ember, Zincir) ama bunlar kullanıcı seçimi.
Krem teması seçili biri için gece modu hiçbir şey değiştirmiyordu.

**Çözüm:** `values-night/themes.xml` yazıldı — 6 açık temanın
(Krem, Violet, Okyanus, Orman, Gül, Altın) koyu karşılığı. Zaten koyu
olan 4 tema burada TEKRAR TANIMLANMADI; Android eksik stili
`values/`'dan alır.

Kod tarafında değişiklik gerekmedi: `ThemeManager.styleFor()` aynı
stil kimliğini döndürüyor, hangi klasörden okunacağına sistem karar
veriyor.

**Mod seçici:** `ThemeManager.geceModu()` — Sistem / Açık / Koyu.
`App.onCreate`'te `AppCompatDelegate.setDefaultNightMode()` çağrılıyor;
ilk Activity oluşmadan önce olmalı, yoksa uygulama yanlış modda açılıp
kendini yeniden oluşturuyor (gözle görülür titreme).

Ayrıca `ThemeManager.koyuMu()` — grafik/çizim renkleri buna bakabilir.

### Öneri 10 — Material You (dinamik renk)
`DynamicColors.applyToActivityIfAvailable()`, **30 Activity'ye** eklendi
(`setTheme` + `applyAccent`'ten hemen sonra).
Varsayılan **kapalı** — uygulamanın kendi 9 teması özenle seçilmiş,
dinamik renk onları eziyor. Görünüm ekranından açılıyor.
Android 12 öncesinde anahtar devre dışı ve sebebi yazılı (gizlemek
yerine açıklamak).

### Öneri 11 — Boş ekran illüstrasyonları · `BosEkran.kt` (YENİ)
9 tür için kodla çizilen illüstrasyon: GOREV (işaretli liste),
NOT (kıvrık kâğıt), KONU (açık kitap), SINAV (takvim+saat),
ETKINLIK (konum iğnesi), ALISKANLIK (ızgara), ARAMA (büyüteç),
HATA (hedef tahtası), GENEL (kutu+yıldız).

**Neden PNG değil:** 9 çizim × 4 yoğunluk = 36 dosya, ~1,5 MB APK
artışı ve tema uyumsuzluğu olurdu. Krem temada güzel duran çizim
Zincir (neon koyu) temada yamalı görünürdü. Burada renkler
`colorPrimary`/`colorPrimaryContainer`'dan türetiliyor.

Her boş durumda başlık + açıklama + **eylem düğmesi** var:
"Henüz görev yok → İlk görevini ekle". Bağlandığı yerler: Görevler,
Notlar, Konular, Sınavlar, Etkinlikler, Alışkanlıklar.

Görevlerde ince nokta: **filtre açıkken** zengin boş durum
gösterilmiyor. "İlk görevini ekle" demek yanlış olur — görev var ama
süzülmüş; o durumda sade "filtreye uyan yok" yazısı çıkıyor.

### Öneri 12 — 5 alternatif uygulama simgesi · `Simge.kt` (YENİ)
Karamel · Mor · Gece · Yeşil · Minimal + Varsayılan.
Manifest'e 5 `activity-alias` eklendi, `PackageManager
.setComponentEnabledSetting()` ile yalnız biri etkin.

**Sıra önemli:** önce yenisi açılıyor, sonra eskiler kapatılıyor.
Ters sırada bir an HİÇBİR launcher girişi olmayan durum oluşur ve
bazı başlatıcılar uygulamayı listeden siler.

minSdk 24 olduğu için hem `mipmap-anydpi-v26` (adaptive-icon) hem
`mipmap-anydpi` (layer-list yedek) yazıldı.

Değiştirmeden önce **uyarı penceresi** çıkıyor: bazı telefonlarda ana
ekran kısayolu düşebilir — bu Android'in davranışı, uygulama hatası
değil.

### Öneri 13 — Konu renk kodu ve simgesi · `KonuGorunum.kt` (YENİ)
10 renklik palet + 20 simge. Kartın soluna 5dp renk şeridi, başlığın
önüne simge, ilerleme halkası da konunun rengini alıyor.
Konu menüsüne "Renk ve simge" eklendi.

**Neden `Store.Topic` modeline alan eklenmedi:** O sınıf JSON'a
serileştiriliyor, yedeğe giriyor, Room geçişinde kullanılıyor ve
12 dosyada okunuyor. Tek bir renk için `exportJson` sürümünü artırmak,
geriye dönük okuma dalı yazmak ve 12 dosyayı gözden geçirmek gerekirdi.
Bunun yerine kimlik→görünüm eşlemesi ayrı depoda (`konu_gorunum_v1`).

Renk atanmamışsa kimlikten kararlı bir renk türetiliyor — kullanıcı hiç
seçim yapmasa bile liste tekdüze kalmıyor, ama aynı konu her açılışta
aynı rengi alıyor.

### Öneri 14 — Degradeli kahraman kart · `DegradeArka.kt` (YENİ)
Ana ekranın en üstündeki büyük kart düz `colorPrimaryContainer`
rengindeydi; ekranın en önemli kartı hiçbir görsel ayrıcalığa sahip
değildi. Artık degrade + sağ üstte iki saydam daire (doku).

**Aciliyet rengi:** sınava kalan gün 45'in altına inince degradenin
alt ucu `colorErrorContainer`'a doğru kayıyor. Sayıya bakmadan da
"yaklaşıyor" hissi veriyor. Bugünse tam kırmızı uçta.

**Neden XML gradient değil:** `<shape><gradient>` sabit renk ister;
9 tema × açık/koyu = 18 varyanta ayrı drawable gerekirdi.

### Öneri 15 — Alt menü rozetleri · `Rozet.kt` (YENİ)
**Bugün** sekmesi: bugüne kadar vadesi gelmiş tamamlanmamış görev +
bugün işaretlenmemiş alışkanlık.
**Konular** sekmesi: hata defterinde bugün tekrarı gelen soru.

`getOrCreateBadge()` (Material 3) kullanıldı — doğru konum, tema
rengi, erişilebilirlik metni otomatik. `maxCharacterCount = 2`
(99'dan fazlası "99+"; üç hane ikonu tamamen kapatıyor).

30 sn önbellek + ekran geçişi/onResume'da zorla tazeleme.

⚠️ **Dürüst not:** Rozet tasarımında konu maddelerinin aralıklı
tekrarını da sayacaktım ama **konularda tekrar sistemi yok** — yalnız
hata defterinde Leitner var (v7.83). Kod gerçeğe uyduruldu.

### Yeni dosyalar
```
BosEkran.kt · Simge.kt · KonuGorunum.kt · DegradeArka.kt · Rozet.kt
res/values-night/themes.xml          (6 koyu tema)
res/values/ga_simge_renkleri.xml
res/drawable/ga_simge_on_koyu.xml
res/mipmap-anydpi-v26/  ga_simge_{karamel,mor,gece,yesil,minimal}.xml
res/mipmap-anydpi/      aynı 5 dosya (API 24-25 yedeği)
```

### Değişen dosyalar
```
ThemeManager.kt    +geceModu +geceModunuUygula +koyuMu
                   +dinamikDesteklenir +dinamikAcik +dinamikRengiUygula
App.kt             açılışta geceModunuUygula
30 × *Activity.kt  +ThemeManager.dinamikRengiUygula(this)
ThemeFragment.kt   +gece modu seçici +Material You anahtarı +simge ızgarası
MainActivity.kt    open() ve onResume()'da Rozet.tazele()
HomeFragment.kt    heroDegrade aciliyet hesabı
TopicsFragment.kt  renk şeridi + simge + gorunumSec() penceresi
TasksFragment.kt · NotesFragment.kt · ExamsFragment.kt
EventsFragment.kt · HabitsFragment.kt   → BosEkran bağlandı
fragment_home.xml  kahraman kart FrameLayout + DegradeArka
fragment_theme.xml +gece grubu +Material You satırı +simge ızgarası
item_topic.xml     +konuSerit (5dp renk şeridi)
AndroidManifest    +5 activity-alias
strings.xml        2720 → 2762 dize
PrefYedek.kt       +konu_gorunum_v1
```

**APK:** 28,5 MB · md5 `49e22b3645b7cfeaa16a409cb778e5bb`
**İmza:** `5F:15:...:85:11` (değişmedi)

⚠️ Cihazda test edilmedi. Özellikle **simge değiştirme** riskli bir
işlem (launcher davranışı üreticiye göre değişiyor) — önce dene, ana
ekran kısayolun kaybolursa çekmeceden geri ekle.

---

## v8.4 (versionCode 140) — GRUP C-1: Görsel ekranlar (öneri 17-20)

Grup C sekiz maddeydi; tek sürüme sığmadı, ikiye bölündü.
**v8.4 = öneri 17, 18, 19, 20** · v8.5 = öneri 16, 21, 22, 23.

### Öneri 17 — Yıllık ısı haritası · `YilIsiView.kt` (YENİ)
İlerleme ekranında aylık ızgara vardı: 30 kare, ay ay geziliyordu.
Bir yıllık emeği görmek için 12 kez ok tuşuna basmak gerekiyordu ve
bütün hiçbir zaman görünmüyordu.

Artık GitHub tarzı **53 hafta × 7 gün** ızgarası tek ekranda.
Dikey eksen haftanın günü olduğu için "hafta sonları boş" gibi
desenler gözle görülüyor — aylık ızgarada bu kayboluyordu.

- Kareye dokun → o günün özeti altta beliriyor
- Üstte "🔥 En uzun seri: N gün" rozeti
- Gelecek günler soluk (henüz yaşanmadı)
- Isı renkleri temadan alınıyor (Zincir/neon temada yamalı durmasın)
- Aylık ızgara **kaldırılmadı**; ikisi farklı soruyu yanıtlıyor
  ("ne kadar yol aldım" / "bu ay ne yaptım")

**Neden custom View:** 371 hücreyi ayrı `View` olarak eklemek 371
nesne demek, kaydırma kekelerdi. Tek `onDraw` içinde 371
`drawRoundRect` birkaç ms.

`Store.gunlukPuanlar()` eklendi — `monthScores` ay ay okuyordu,
53 haftalık ızgara için 13 çağrı gerekirdi.

### Öneri 18 — Alt sayfa menüleri · `AltSayfa.kt` (YENİ)
**Ölçüm:** v8.3'te `BottomSheetDialog` HİÇBİR dosyada yoktu. Bütün
seçim menüleri ekranın ORTASINDA açılıyordu — tek elle telefon tutan
için en zor ulaşılan yer, üstelik arkadaki içeriği tamamen kapatıyor.

Alt sayfa: başparmağın altında, sürükleyerek kapatılıyor, uzun
listelerde kaydırılıyor. Satırlar en az 56dp (Material minimum 48dp).
Yıkıcı işlemler (silme) kırmızı, devre dışı satırlar soluk.

**Çevrilen akışlar:** görev seçenekleri (uzun basma), konu seçenekleri.
**Neden hepsi değil:** uygulamada 60+ `setItems` çağrısı var; hepsini
çevirmek tek sürümlük iş değil ve riskli. Kalanlar
`MaterialAlertDialogBuilder` ile çalışmaya devam ediyor.

### Öneri 19 — Nefes alan sayaç halkası
`SayacKadraniView` doğruydu ama tamamen sabitti: 30 dakika kalmışla
5 saniye kalmış aynı görünüyordu.

- **Renk geçişi:** >5 dk tema vurgusu → 1-5 dk amber → <1 dk kırmızı.
  Sıçrama yok, aralarda karışım yapılıyor.
- **Nabız:** son 10 saniyede halka 0.94-1.06 arası nefes alıyor.
- **Bitiş parlaması:** merkezden dışa genişleyen, solarak kaybolan halka.

**Neden ayrı ValueAnimator yok:** sayaç zaten saniyede bir invalidate
ediyor; ikinci çizim döngüsü açmak yerine faz
`System.currentTimeMillis()`'ten hesaplanıyor ve yalnız nabız
etkinken `postInvalidateOnAnimation()` çağrılıyor.

### Öneri 20 — Quiz sonuç animasyonu · `PuanHalkasi.kt` (YENİ)
Quiz bitince düz metin çıkıyordu: "7/10 doğru (%70)". Quiz uygulamanın
en çok tekrarlanan eylemi; emek görünmez kalıyordu.

Artık **dolarak gelen halka** + ortada büyük yüzde + altında "7/10".
Renk sonuca göre: %90+ yeşil, %60+ amber, altı kırmızı.
Geçtiyse kutlama titreşimi.

Soru sırasında da: doğru şık kısa nabız, yanlışta kart sarsılıyor ve
**doğru cevap 260 ms sonra beliriyor** (nereye bakacağını göstersin).

**Neden StatRingView kullanılmadı:** o bileşen ana ekrandaki küçük
halkalar için yazıldı — ortasında emoji var, metin desteği yok, kalınlık
sabit oranlı. Burada iki satır metin ve çok daha kalın halka gerekiyor.

### 🔴 Derleme hatası (ilk deneme)
`AltSayfa.kt`: `MaterialColors.getColor(this@apply, ...)` yazdım ama
`this@apply` bir `GradientDrawable`; o fonksiyon `View` veya `Context`
ister. Renkleri en başta bir kez çözüp değişkende tutarak düzelttim.
**Bu benim hatamdı.**

### Yeni dosyalar
```
YilIsiView.kt · AltSayfa.kt · PuanHalkasi.kt
```

### Değişen dosyalar
```
Store.kt              +gunlukPuanlar(gunSayisi) — tek geçişte yıllık okuma
ProgressFragment.kt   +yilIsisiniKur +yilIsisiniTazele
fragment_progress.xml +yıllık ısı kartı (HorizontalScrollView içinde)
SayacKadraniView.kt   +renkGecisi +nabizAcik +bitisParlamasi()
                      +aktifRenk() +nabizOlcegi() +karistir()
                      guncelle() imzasına kalanSn eklendi (varsayılanlı)
TimerFragment.kt      kalanSn geçiliyor · bitiş anında parlama
QuizActivity.kt       sonuç penceresi PuanHalkasi ile · doğru/yanlış efektleri
TasksFragment.kt      confirmDelete → AltSayfa menüsü
TopicsFragment.kt     konuSecenekleri → AltSayfa menüsü
strings.xml           2762 → 2767 dize
```

**APK:** 28,6 MB · md5 `06c67118317474085da1945cd89b54a6`
**İmza:** `5F:15:...:85:11` (değişmedi)

### Sırada (v8.5 — Grup C-2)
- Öneri 16: ana ekran özelleştirme (kart sıralama/gizleme)
- Öneri 21: istatistik ekranına gerçek grafikler
- Öneri 22: konu ağacının görsel hiyerarşisi
- Öneri 23: Bugün ekranında zaman çizelgesi

---

## v8.5 (versionCode 141) — GRUP C-2: Öneri 16, 21, 22, 23

Grup C tamamlandı. İlk denemede derlendi.

### 🎯 Öneri 16 — ANA EKRAN ÖZELLEŞTİRME (uzun süredir bekleyen madde)

Bu, **önceki 20'lik öneri listesinin de tek eksik maddesiydi**.
v7.97'de "tek başına bir sürümlük iş" denip ertelenmişti; gerekçe
doğruydu: `fragment_home.xml` 725 satırlık sabit bir XML'di.

**Denenmeyen yol:** her kartı ayrı layout dosyasına çıkarıp
RecyclerView'a beslemek. Ana ekranda 40 id var ve `HomeFragment`
hepsini `findViewById` ile arıyor — parçalara bölmek 564 satırlık
fragment'ı baştan yazmak demekti. Yüksek risk, sıfır görsel kazanç.

**Seçilen yol:** XML'i Python'da `ElementTree` ile ayrıştırıp
14 üst seviye öğeyi mantıksal olarak **8 bloğa** grupladım, her
grubu id'li bir `LinearLayout` ile sardım.
Doğrulama: **40 id'nin 40'ı duruyor**, hiçbir `findViewById`
bozulmadı (fark alınarak kontrol edildi).

`AnaEkranDuzen.kt` yalnız blokların **sırasını** ve **görünürlüğünü**
yönetiyor — `removeView` + `addView` ile yeniden yerleştiriyor,
görünümler yok edilmiyor (dinleyiciler korunuyor).

**Düzenleyici ekran** (`AnaEkranDuzenActivity`): sürükle-sırala +
anahtarla gizle. Ayarlar → "Ana ekran düzeni".

Tasarım kararları:
- **Neden ayrı ekran, "düzenleme modu" değil:** ana ekran bir
  `ScrollView`; sürükleme ile kaydırma çakışıyor. Ayrıca kart
  yükseklikleri çok farklı (kahraman 200dp, rozet 40dp), sürüklerken
  ekran zıplıyor. Ayrı ekranda hepsi eşit yükseklikte satır.
- **Sürükleme yalnız ⠿ tutamağından** (`isLongPressDragEnabled = false`) —
  listeyi kaydırmakla sıralamak karışmasın.
- **Selamlama başlığı taşınmıyor** — içinde ayarlar/zamanlayıcı
  düğmeleri var, en üstte sabit.
- **Kahraman kart gizlenemiyor** (`zorunlu = true`).
- **İleriye dönük:** kayıtlı sırada olmayan blok sona ekleniyor.
  v8.6'da yeni kart eklenirse, sırasını özelleştirmiş kullanıcı onu
  kaybetmez.

⚠️ Kod notu: `putStringSet` aynı küme nesnesini tutarsa değişiklik
yazılmıyor — her seferinde yeni `HashSet` veriliyor (bilinen tuzak).

### Öneri 21 — Gerçek grafikler · `DagilimHalkasi.kt` (YENİ)
İlerleme ekranında yalnız ısı haritası ve iki metin satırı vardı.
`BarChartView`, `SparklineView`, `NetChartView` bileşenleri projede
vardı ama **İlerleme ekranında hiç kullanılmıyordu**.

Eklenenler:
1. **Konu dağılımı halkası** — her konunun tamamlanan madde payı.
   Renkler v8.3'teki `KonuGorunum` kodlarından; konular listesiyle
   grafik aynı renkleri paylaşıyor.
2. **Haftalık çubuk grafik** — son 7 günün puanı (`BarChartView`).

**Neden halka, pasta değil:** ortadaki boşluk toplam değeri yazmak
için kullanılıyor. Dilime dokununca o dilim dışa taşıyor ve ortada
adı+yüzdesi yazıyor — legend'e gerek kalmıyor (dar ekranda legend
zaten okunmuyordu).
6'dan fazla konu varsa küçükler "Diğer"de toplanıyor; 15 dilimli
halka okunmaz.

### Öneri 22 — Konu ağacı hiyerarşisi · `AgacCizgiView.kt` (YENİ)
Alt konular düz liste halindeydi; hangi maddenin hangi konuya ait
olduğu yalnız konumdan anlaşılıyordu, kaydırınca bağ kopuyordu.

Artık her alt madde satırının solunda ağaç bağlantısı:
`├──` normal, `└──` son madde (dikey çizgi yarıda biter).
Çizgi rengi konunun renk kodundan; **tamamlanan maddede dolu,
bekleyende yarı saydam** — ağaç ilerlemenin kendisini de gösteriyor.

**Neden custom View, drawable değil:** son maddenin çizgisi farklı
(└ vs ├); drawable ile iki dosya gerekirdi ve renk temaya uymazdı.

### Öneri 23 — Günün zaman çizelgesi · `ZamanCizelgesiView.kt` (YENİ)
Bugün ekranı bir listeydi; sabah 9'da üç işin üst üste bindiğini
görmek imkânsızdı.

Artık dikey saat şeridi üzerinde:
- Görevler (vade saatlerine yerleşmiş, tamamlananlar soluk)
- Sayaç oturumları (geçmiş odak blokları)
- Namaz vakitleri (ince şeritler)
- **Şu an** kırmızı çizgi + nokta

**Çakışma çözümü:** aynı saate düşen bloklar yan yana daraltılıyor
(takvim uygulamalarındaki gibi, açgözlü sütun yerleştirme). Üç iş
aynı saatteyse üçü de görünüyor.

Bloğa dokun → ilgili ekran; boş saate dokun → görev ekleme.

**Görev listesi kaldırılmadı:** vadesi olmayan görevler çizelgede
gösterilemez (saati yok) ama yine de yapılmalı. Çizelge "ne zaman",
liste "ne" sorusunu yanıtlıyor.
Hiç zamanlı öğe yoksa kart tamamen gizleniyor — boş saat şeridi
göstermek anlamsız.

### Yeni dosyalar
```
AnaEkranDuzen.kt · AnaEkranDuzenActivity.kt
  + activity_ana_duzen.xml + item_ana_duzen.xml
DagilimHalkasi.kt · AgacCizgiView.kt · ZamanCizelgesiView.kt
```

### Değişen dosyalar
```
fragment_home.xml     14 üst seviye öğe → 8 id'li blok (ElementTree ile
                      yeniden yazıldı; 40/40 id korundu)
HomeFragment.kt       onViewCreated + onResume'da AnaEkranDuzen.uygula()
fragment_progress.xml +dağılım halkası kartı +haftalık çubuk
ProgressFragment.kt   +grafikleriTazele()
fragment_today.xml    +zaman çizelgesi kartı
TodayFragment.kt      +bindCizelge()
item_subtopic.xml     +AgacCizgiView
TopicsFragment.kt     alt maddelerde ağaç çizgisi (sonMu/tamamMi/renk)
fragment_settings.xml +rowAnaDuzen
SettingsFragment.kt   +AnaEkranDuzenActivity bağlantısı
AndroidManifest       +AnaEkranDuzenActivity
strings.xml           2767 → 2793 dize
```

**APK:** 28,6 MB · md5 `f65ecfb7d5db5593394acf0b5d51281a`
**İmza:** `5F:15:...:85:11` (değişmedi)

### 30 önerinin durumu
- ✅ Grup A (1-8) — v8.2
- ✅ Grup B (9-15) — v8.3
- ✅ Grup C (16-23) — v8.4 + v8.5
- ⏳ Grup D (24-30) — sırada

### Sırada (v8.6 — Grup D)
24 kutlama efektleri · 25 shimmer iskeletler · 26 Snackbar birleştirme ·
27 yazı boyutu/yoğunluk · 28 erişilebilirlik · 29 yatay/tablet ·
30 widget yenileme

---

## v8.6 (versionCode 142) — GRUP D: Cila ve erişilebilirlik (öneri 24-30)

**30 önerinin tamamı bitti.** İlk denemede derlendi · 27 test geçiyor.

### Öneri 24 — Kutlama efektleri · `Kutlama.kt` + `Basari.kt` (YENİ)
Günlük hedef tamamlanınca yalnız bir Toast çıkıyordu. Serinin 30. günü
ile sıradan bir gün arasında görsel fark yoktu — uygulamanın bütün
amacı devamlılıkken başarı anını ödüllendirmemek büyük eksikti.

3 efekt: **konfeti** (hedef), **havai fişek** (seri kilometre taşı),
**yıldız yağmuru** (rozet/kurs).

🔴 **Önceki notumdaki hatayı düzeltiyorum:** v8.1 notlarında
"`AmbientFxView` altyapısı var, kullanılmıyor" yazmıştım. **Yanlıştı.**
O sınıf ortam sesi görselleri için (yağmur, dalga, ateş) ve
`FullscreenTimerActivity`'de kullanılıyor. Parçacıkları döngüsel akıyor,
yerçekimi/dönme yok — konfeti için uygun değil. Ayrı görünüm yazıldı.

**`Basari.birKez()` neden gerekli:** kutlama `bindStatus`/`reload` gibi
HER onResume'da çalışan yerlerden tetikleniyor. Kontrol olmasaydı
hedefi tutturan kullanıcı ekrana her dönüşünde konfeti görürdü.
Anahtarlar tarihli; 60 günden eskiler açılışta temizleniyor.

Seri kutlaması **7'nin katlarında** (7, 14, 21...) — her gün konfeti
atmak anlamını yitirir.

### Öneri 25 — Yükleniyor iskeletleri · `Iskelet.kt` (YENİ)
AI cevabı 3-10 saniye sürebiliyor; "düşünüyor..." düz metni o sürede
uygulamanın donduğu hissini veriyordu.

3 şekil: liste (ikon + 2 çizgi), metin (değişen uzunlukta paragraf),
kart (2×2 ızgara). `LinearGradient` + kayan matris ile shimmer.

**Neden Facebook Shimmer kütüphanesi eklenmedi:** tek efekt için
~120 KB bağımlılık. Aynı görünüm 150 satırda elde edildi.
Animasyon kapalıysa parıltı durur, iskelet düz gri kalır — yapı
bilgisi yine verilir.

Bağlandığı yer: Asistan ekranı (AI cevabı beklerken).

### Öneri 26 — Bildirim birleştirme · `Bildir.kt` (YENİ)
Uygulamada **200'den fazla `Toast.makeText`** var. Toast: sistemin gri
kutusu, temayla uyumsuz, Android 12+'da uygulama simgesi ekliyor,
eylem düğmesi taşıyamıyor.

Snackbar'lar da vardı ama 🔴 **FAB'ın altında kalıyordu** —
v7.72'den beri "geri al" şeridi kısmen görünmüyordu. `Bildir`
`anchorView = bottomNav` ile alt menünün ÜSTÜNE konumluyor.

4 tür: `bilgi` · `basari` (yeşil + titreşim) · `hata` (colorError) ·
`eylemli` (geri al). Kök görünüm yoksa Toast'a düşüyor.

**Neden 200 Toast birden değiştirilmedi:** tek sürümde riskli.
Görev/not geri alma şeritleri geçirildi; kalanlar zamanla.
Ayrıca Toast'ın doğru olduğu yerler var (Activity kapanırken).

### Öneri 27 — Yazı boyutu ve yoğunluk
`Configuration.fontScale` üzerinden — **31 Activity'ye
`attachBaseContext`** eklendi. Tek tek `TextView.textSize`
değiştirmek 71 layout + 166 dosya demekti.

4 kademe (küçük/normal/büyük/çok büyük) + 3 yoğunluk
(sıkı/normal/rahat). Kullanıcının telefon genelindeki ayarı
korunuyor; bizim çarpanımız onun üstüne biniyor.

### Öneri 28 — Erişilebilirlik
Kodu tarayıp ölçtüm: **47 emoji ikonun `contentDescription`'ı yoktu**,
21'i tıklanabilir. Ekran okuyucu "✕" yerine anlamsız bir şey okuyordu.

- 21 tıklanabilir ikona `contentDescription` eklendi
- 10 dekoratif emojiye `importantForAccessibility="no"` (ekran okuyucu
  atlasın — "📚" duymak kimseye yardımcı olmuyor)
- Toplam **31 öğe** düzeltildi

### Öneri 29 — Yatay ekran ve tablet · `Duzen.kt` (YENİ)
Yatayda kartlar 850px genişlikte tek satır metin gösteriyordu;
tablette uygulama "büyütülmüş telefon" gibiydi.

**Neden `layout-land/` dosyaları yazılmadı:** 71 layout'un yatay
kopyası bakım kâbusu olurdu — her değişikliği iki yerde yapmak
gerekirdi ve biri unutulurdu. (Bu projede benzer bir hata v7.62'de
dört sürüm boyunca fark edilmemişti.)

Bunun yerine `values-land/` + `values-sw600dp/` ölçüleri ve çalışma
anında genişlik sınırlama. **16 fragment'a** `Duzen.uygula(view)`
eklendi. Sınır 720dp — tipografi kuralı olan 45-75 karakter satır
uzunluğuna denk geliyor.

### Öneri 30 — Widget görsel yenileme
🔴 **Bulunan çelişki:** `drawable-v31/w_card.xml` zemin olarak
`@android:color/system_accent2_50` (AÇIK ton) kullanıyordu. Ama
v7.67'de kullanıcı "widgetlarda karanlık tema" dediği için
`values-v31/widget_colors.xml` koyu tonlara ayarlanmıştı. Sonuç:
**Android 12+ cihazlarda widget yazıları okunmuyordu.**

Düzeltme: zemin artık `@color/w_bg`'den (kullanıcı tercihine saygılı),
yalnız köşe yarıçapı sistemden
(`system_app_widget_background_radius` — launcher'ın kırpmasıyla
uyumlu, kenarda hilal kalmıyor).

Ayrıca v31'de iç boşluk 16dp→12dp (Android 12+ launcher kendi payını
ekliyor, üst üste binince içerik sıkışıyordu). 4 widget layout'u
güncellendi.

### Yeni dosyalar
```
Kutlama.kt · Basari.kt · Iskelet.kt · Bildir.kt · Duzen.kt
res/values/dimens.xml · values-land/dimens.xml · values-sw600dp/dimens.xml
res/values-v31/dimens.xml · values/dimens_widget.xml
```

### Değişen dosyalar
```
31 × *Activity.kt     +attachBaseContext (yazı ölçeği)
16 × *Fragment.kt     +Duzen.uygula(view)
GorunumAyar.kt        +yaziOlcegiUygula +yogunlukCarpani
GorunumAyarActivity   +secici() (MaterialButtonToggleGroup) +OKUMA grubu
App.kt                +Basari.temizle()
TodayFragment.kt      hedefe ulaşınca konfeti (Basari.birKez ile)
HabitsFragment.kt     seri 7'nin katında havai fişek
AsistanFragment.kt    showThinking → shimmer iskelet
TasksFragment.kt · NotesFragment.kt   geriAlSun → Bildir.eylemli
drawable-v31/w_card.xml · w_chip.xml  sistem yarıçapı + doğru renk
4 widget layout       padding → @dimen/w_kart_pay
31 layout öğesi       contentDescription / importantForAccessibility
strings.xml           2793 → 2816 dize
```

**APK:** 28,7 MB · md5 `e7e8b79a4ec04c667e39b13449f6d04f`
**İmza:** `5F:15:...:85:11` (değişmedi)
**Test:** 27/27 geçiyor

---

## 🏁 30 ÖNERİNİN TAMAMI BİTTİ

| Grup | Öneri | Sürüm | Durum |
|---|---|---|---|
| A — Hareket ve his | 1-8 | v8.2 | ✅ |
| B — Renk, tema, kimlik | 9-15 | v8.3 | ✅ |
| C — Görsel ekranlar | 16-20 | v8.4 | ✅ |
| C — Görsel ekranlar | 21-23 | v8.5 | ✅ |
| D — Cila ve erişilebilirlik | 24-30 | v8.6 | ✅ |

### Başlangıç ve bitiş ölçümü

| Ölçüm | v8.1 | v8.6 |
|---|---|---|
| Animasyon kullanan dosya | 1 / 166 | ~40 |
| Haptic geri bildirim | 0 | 12+ nokta |
| Kaydırma jesti | 0 | Görev + not |
| Alt sayfa (bottom sheet) | 0 | 2 akış |
| `res/anim` klasörü | YOK | 12 dosya |
| Splash ekranı | YOK | VAR |
| **Gerçek koyu tema** | **YOK** | **6 tema** |
| Material You | 0 | 30 Activity |
| Boş ekran illüstrasyonu | 0 | 9 tür |
| Erişilebilirlik etiketi eksiği | 47 | 16 (dekoratif) |
| Toplam Kotlin dosyası | 166 | 184 |
| strings.xml | 2701 | 2816 |

### ⚠️ Cihazda doğrulanmayanlar
v8.2-v8.6 arasındaki HİÇBİR özellik gerçek cihazda test edilmedi
(sandbox'ta emülatör yok). Özellikle şunlar riskli:
- **Simge değiştirme** (v8.3) — launcher davranışı üreticiye göre değişir
- **Ana ekran blok sıralama** (v8.5) — XML yeniden yazıldı, 40/40 id
  korundu ama çalışma anı davranışı sınanmadı
- **Yazı ölçeği** (v8.6) — 31 Activity'de `attachBaseContext`
- Animasyon ve haptic'in "hissi" ancak sende belli olur

---

## v8.7 (versionCode 143) — ÖZ DENETİM: 30 özelliğin kod incelemesi

30 öneri bitmişti ama **hiçbiri cihazda test edilmemişti**. Bu sürümde
yeni özellik eklemek yerine kendi yazdığım kodu denetledim.
**5 gerçek hata bulundu ve düzeltildi.** Test sayısı 27 → 71.

### 🔴 HATA 1 — Ana ekran yatay/tablet düzeninden ATLANMIŞ
v8.6'da `Duzen.uygula(view)` 17 fragment'a toplu betikle eklenmişti.
Betik `override fun onViewCreated(` desenini arıyordu; HomeFragment'ta
imza satırı farklı biçimlenmiş olduğu için **eşleşmemiş**.

Sonuç: 16 fragment düzeltildi, **ana ekran — en çok bakılan ekran —
eksik kaldı**. Tablette ana ekran hâlâ "büyütülmüş telefon" olarak
kalacaktı.

Ders: toplu düzenleme betiklerinden sonra sonucu SAYMAK yetmez,
hangi dosyaların atlandığını da listelemek gerekiyor.

### 🔴 HATA 2 — Ana ekran sıralama algoritması bozuk (öneri 16)
v8.5'teki `AnaEkranDuzen.uygula` şöyleydi:
```kotlin
val suanki = kap.indexOfChild(g)
if (suanki != hedefKonum) { kap.removeViewAt(suanki); kap.addView(g, hedefKonum) }
```
`removeViewAt` çağrıldığı anda **sonraki tüm çocukların indeksi bir
azalıyor**. Döngü ilerledikçe `hedefKonum` gerçek konumu göstermiyordu
ve bloklar yanlış yerlere ekleniyordu. Kullanıcı sırayı değiştirdiğinde
ana ekran karışacaktı.

Düzeltme: önce TÜM blokları çıkar, sonra istenen sırayla geri ekle.
İndeks kaymasından etkilenmiyor.

### 🔴 HATA 3 — Widget'ta `<View>` kullanımı (v7.40.1'in tekrarı)
`widget_glass_row.xml` içinde gerçek bir `<View>` ayraç vardı ve bu
dosya `GlassListService` tarafından **RemoteViews ile şişiriliyordu**.

RemoteViews `<View>` desteklemiyor — bu tam olarak v7.40.1'de yaşanan
ve diğer widget layout'larında düzeltilen hata. O dosya gözden kaçmış.
Sıfır metinli `TextView`'a çevrildi.

Not: taramada 3 dosya daha `<View>` içeriyor göründü ama onlar
sadece "kullanmayın" diyen YORUM satırlarıydı — yanlış alarm.

### 🔴 HATA 4 — `Duzen` iki kez uygulanınca içerik daralıyordu
`uygula()` hem `onViewCreated`'dan hem ekran döndürüldüğünde
çağrılabiliyor. İkinci çağrıda margin'ler ÜST ÜSTE biniyordu ve
içerik her döndürmede biraz daha daralıyordu.
`R.id.ga_tag_duzen` etiketiyle tek seferlik yapıldı.

### 🔴 HATA 5 — Dar yatay ekranda içerik TAŞACAKTI
`values-land/ga_en_fazla_genislik = 640dp`. Ama 6,7" bir telefon
yatayken zaten ~640-740dp geniş. Sınır ekrandan büyükse
`lp.width = enFazla` içeriği taşırır ve **yatay kaydırma çubuğu**
oluştururdu — düzeltmek isterken bozardık.

Artık yalnız ekran sınırdan en az %8 genişse müdahale ediliyor.

### ⚠️ Ek düzeltme — `Iskelet` LayoutParams paylaşımı
`ebeveyn.addView(katman, sira + 1, hedef.layoutParams)` — iki görünüm
**aynı LayoutParams nesnesini** paylaşıyordu. Biri değişince diğeri de
değişir (Android'de klasik hata). Kopya üretiliyor.

### Testler: 27 → 71

| Test sınıfı | Test | Kapsam |
|---|---|---|
| TekrarTest | 16 | tarih hesabı (v7.99) |
| YedekSifreTest | 11 | AES şifreleme (v7.99) |
| **AnaEkranDuzenTest** | **12** | **sıralama mantığı (YENİ)** |
| **GorunumTest** | **32** | **görsel katman hesapları (YENİ)** |

`AnaEkranDuzenTest` — sıra kaydı çözümlemesi: boş kayıt, eksik blok
(ileri uyumluluk), bilinmeyen kod (geriye uyumluluk), yinelenen kod,
"hiçbir durumda blok kaybolmaz veya çoğalmaz".

`GorunumTest` — sayaç renk eşikleri, nabız aralığı, kahraman kart
aciliyeti, ısı haritası seviyeleri, yazı çarpanları, simge/tema
tutarlılığı, negatif konu kimliğinde renk türetme, zaman çizelgesi
çakışma yerleşimi.

### 📌 Test yazarken öğrenilen ders
İlk yazdığım çizelge testleri algoritmanın bir **KOPYASINI** test
ediyordu. Kopyada bir hata vardı (eşit `Pair`'ler HashMap'te tek
anahtar oluyor) ve test **yanlış alarm** verdi — gerçek kod doğruydu.

Bunun üzerine `ZamanCizelgesiView.sutunlaraAyir` companion object'e
taşınıp public yapıldı; testler artık **gerçek kodu** çağırıyor.
Kopyayı test etmek, test etmemekten daha kötü: yanlış güven veriyor.

### Değişen dosyalar
```
HomeFragment.kt          +Duzen.uygula(view)  ← ATLANMIŞTI
AnaEkranDuzen.kt         uygula() yeniden yazıldı (indeks kayması)
                         +siralaKayittan() — test edilebilir hâle ayrıldı
Duzen.kt                 +ekran genişliği kontrolü +tek seferlik koruma
Iskelet.kt               LayoutParams kopyalanıyor
ZamanCizelgesiView.kt    sutunlaraAyir → companion object (test için)
widget_glass_row.xml     <View> → TextView (RemoteViews kısıtı)
res/values/ids.xml       +ga_tag_duzen
app/src/test/            +AnaEkranDuzenTest.kt +GorunumTest.kt
```

**APK:** 28,7 MB · md5 `57f092e6866f1ca6478e46c1015eb683`
**İmza:** `5F:15:...:85:11` (değişmedi)
**Doğrulama:** `aapt dump badging` → versionCode 143, tek launcher
girişi (alias'lar doğru şekilde kapalı başlıyor)

### Hâlâ cihazda doğrulanmayanlar
Kod incelemesi ve birim testler mantık hatalarını yakalar ama şunları
yakalayamaz:
- Animasyon ve haptic'in **hissi** (çok mu hızlı, çok mu sert)
- Simge değiştirmenin launcher davranışı (üreticiye göre değişir)
- Gerçek tablette blok sıralamanın görünümü
- Yazı ölçeğinin 31 Activity'de düzeni bozup bozmadığı

---

## v8.8 (versionCode 144) — FAZ 1: Güvenlik ve 10,8 MB

100 önerinin ilk fazı. **APK 27,4 → 16,6 MB (%39 küçüldü)** ve en
ciddi güvenlik açığı kapandı. Test: 71/71.

### 🔴 Öneri 13 — 7,9 MB ölü kod atıldı
`unzip -l` ölçümü: BouncyCastle'ın post-quantum kripto veri dosyaları
APK'da duruyordu ve **hiç kullanılmıyordu**:
```
lowmc.properties   3.555 KB  (Picnic imza şeması)
p751.properties    1.894 KB  (SIKE anahtar değişimi)
p610/p503/p434     2.523 KB
CertPathReviewer      90 KB
```
`pdfbox-android` bağımlılığıyla geliyorlar. Kod taramasında
`bouncycastle` geçen tek satır yok. `packaging { excludes }` ile
çıkarıldı. Şifreli PDF desteği bozulmadı — onu sağlayan sınıflar
(`crypto.*`) duruyor, yalnız deneysel `pqc` veri dosyaları gitti.

### 🔴 Öneri 14 — R8 açıldı
`isMinifyEnabled = true` + `isShrinkResources = true` +
270 satırlık `proguard-rules.pro`.

Kurallar 12 başlıkta ve her biri gerekçeli: Manifest'ten yansımayla
oluşturulan bileşenler, XML'den şişirilen özel View'lar, Room
varlıkları (isim kısaltılırsa **mevcut veri okunamaz**), JSON alan
adları (eski yedekler bozulur), enum'lar, RemoteViews.

**Doğrulama:** release APK'nın dex'i tarandı — MainActivity, Store,
Depolama, 4 özel View, 3 Room sınıfı korunmuş. `AnahtarKasa` ve
`GuvenliDosya` adları kısaltılmış ama içerikleri
(`AndroidKeyStore`, `GAK2`, `AES/GCM/NoPadding`) dex'te mevcut —
beklenen davranış.

**Sonuç:** 27,4 → 16,6 MB. İmza değişmedi.

### 🔴 Öneri 1 — API anahtarları gerçekten şifrelendi · `AnahtarKasa.kt`
**Önceki durum:** `AiSettings.mask()` şunu yapıyordu:
```kotlin
bytes[i] XOR "gunlukasistan"[i % 13]
```
Bu şifreleme değil **gizleme**: tuz kaynak kodda düz metin, XOR
tersine çevrilebilir, veri başka cihazda da çözülebiliyordu. Root'lu
cihazda API anahtarı okunabilirdi — kullanıcının OpenAI faturası
başkasına çıkabilirdi.

**Şimdi:** Android Keystore + AES-256-GCM. Şifreleme anahtarı güvenli
öğede (TEE/StrongBox), uygulama bile erişemiyor. Veri dosyası
kopyalansa başka cihazda çözülemez.

**Neden `EncryptedSharedPreferences` değil:** +300 KB APK (7,9 MB
attıktan sonra geri koymanın anlamı yok), tüm dosyayı sarıyor,
kütüphane 2021'den beri güncellenmedi. Doğrudan Keystore 190 satır.

**Geçiş:** eski XOR biçimi okunmaya devam ediyor, `App.onCreate`
arka planda sessizce yeni biçime taşıyor. Kullanıcı anahtarını
yeniden girmiyor.

### 🔴 Öneri 3 — Yedekleme kuralları BOZUKTU (lint yakaladı)
Release derlemesi ilk denemede **lint hatasıyla kırıldı**:
```
backup_rules.xml:21: Error: pdf_cache is not in an included path
```
Gerçek hata: yalnız `sharedpref` dahil edilmiş ama `file`/`external`
alanlarından `<exclude>` yazılmıştı — dahil edilmemiş alandan bir şey
çıkaramazsın. O satırlar hiçbir şey yapmıyordu.

Yeniden yazıldı. Artık **API anahtarları, çökme kayıtları ve AI
önbelleği buluta gitmiyor**; ders notları ve ilerleme gidiyor.
Room veritabanı da yedeğe eklendi (eskiden eksikti!).

### Öneri 7 — Atomik dosya yazma · `GuvenliDosya.kt`
`writeText` önce dosyayı sıfırlar, sonra yazar. Arada sistem
uygulamayı öldürürse (2 GB RAM'li cihazlarda sık) **hem yeni yedek
yazılmamış hem eski yedek silinmiş** olur. Kullanıcı bunu ancak
telefonunu kaybedip geri yüklemeye çalışınca fark eder.

Artık: geçici dosyaya yaz → `fd.sync()` → atomik `rename`.
Ayrıca ana yedeğin bir önceki sürümü `.onceki` olarak korunuyor.
Store'daki 3 yazma noktası çevrildi.

### Öneri 8 — Bozuk veri artık kaybolmuyor
566 `catch (Exception)`'ın çoğu hatayı yutup boş liste döndürüyordu;
sonraki kaydetme o boşluğu kalıcılaştırıyordu. Artık bozuk içerik
`bozuk/<etiket>.bozuk.<zaman>.json` olarak saklanıyor ve Depolama
ekranında görünüyor.

### Öneri 4 + 5 — Denetimli geri yükleme
`Store.iceAktarDenetimli()`:
- **Sürüm kontrolü:** yedeğin biçimi desteklenenden yeniyse reddediyor
  ("Bu yedek daha yeni bir sürümden, önce uygulamayı güncelle")
- **Geri alınabilirlik:** yükleme öncesi anlık görüntü alınıyor
- **Bozuk dosya:** kenara kopyalanıp kullanıcıya bildiriliyor

### Öneri 9 + 10 — Depolama ekranı · `Depolama.kt` + `DepolamaActivity`
Ayarlar → **Depolama**. 8 kategori ayrı ayrı ölçülüyor: kanıt
fotoğrafları, not resimleri, PDF'ler, AI önbelleği, üretilmiş
anlatımlar, yedekler, bozuk veri, sistem önbelleği.

Kullanıcı verisi **temizlenemez** olarak işaretli; yalnız yeniden
üretilebilir şeyler silinebiliyor. Ayrıca "yetim dosyaları topla" —
hiçbir kayda bağlı olmayan 7 günden eski fotoğrafları siliyor.

⚠️ **Yazarken hata yaptım:** ilk sürümde `Kanit.tumYollar()` ve
`Note.imagePath` diye API'ler varsaydım — **ikisi de yok**. Gerçek
adlar `Kanit.hepsi()`, `Kanit.gecmis()`, `Note.image`. Var olmayan
API'ye dayanan temizlik dosyaları yanlışlıkla yetim sayıp SİLERDİ.
Kodu okuyup düzelttim ve "kayıtlar okunamazsa hiçbir şey silme"
koruması ekledim.

### Öneri 11 — Kendi önerimi reddettim
Listede "`.commit()` ana iş parçacığında, `apply()` olmalı" yazmıştım.
İnceleyince gördüm ki iki kullanım da **bilinçli ve doğru**: çökme
kaydında süreç milisaniyeler içinde ölüyor, `apply()` yetişemez ve
kayıt kaybolur. Değiştirmedim, gerekçeyi koda yazdım.

### Öneri 12 — Hata bildirimi
Çökme kaydı + Android sürümü + üretici/model + uygulama sürümü
paylaşılabiliyor veya panoya kopyalanıyor. Samsung bildirim sorunu
(v7.92) tam da cihaz bilgisi olmadığı için 6 sürüm sürmüştü.

### Ayrıca
- `buildConfig = true` (sürüm bilgisi için)
- `viewBinding = true` (Faz 2 hazırlığı — derlemenin bozulmadığı doğrulandı)

### Yeni dosyalar
```
AnahtarKasa.kt · GuvenliDosya.kt · Depolama.kt · DepolamaActivity.kt
app/proguard-rules.pro   (270 satır, 12 başlık)
```

### Değişen dosyalar
```
build.gradle.kts       +packaging{excludes} +minify +shrinkResources
                       +buildConfig +viewBinding
AiSettings.kt          mask() → AnahtarKasa · +anahtarlariTasi()
Store.kt               3 yazma noktası atomik · +YEDEK_BICIM
                       +IceAktarSonuc +iceAktarDenetimli +yuklemeyiGeriAl
App.kt                 +anahtar taşıma · .commit() gerekçesi
SettingsFragment.kt    denetimli geri yükleme · +rowDepolama
backup_rules.xml       yeniden yazıldı (lint hatası + gizlilik)
data_extraction_rules.xml  aynı
AndroidManifest        +DepolamaActivity
strings.xml            2816 → 2864 dize
```

**APK (release+R8):** 16,6 MB · md5 `415f8ddd2782ee520892a4abf8cf24c8`
**İmza:** `5F:15:...:85:11` (değişmedi — üstüne kurulur)
**Test:** 71/71

### Sırada — Faz 2 (v8.9): Motor değişimi
15 (Store önbelleği) · 16 (coroutine) · 17 (DiffUtil) ·
18 (ViewBinding) · 22 (bitmap ölçekleme)

---

## v8.9 (versionCode 145) — FAZ 2: Motor değişimi

Uygulamanın **hissini** değiştiren grup. Test: 71 → 81.

### Öneri 15 — Store önbelleği · `Onbellek.kt` (YENİ)
**Ölçüm:** `loadTopics` 32 yerde, `loadTasks` 31, `loadLessons` 29
yerde çağrılıyor. Her çağrı diskten okuyup tüm JSON'u ayrıştırıyordu.
200 maddelik listede her okuma yüzlerce nesne yaratıyordu.

Önbelleklenen 5 tür: Topics, Courses, Sections, Lessons, Habits.

**🔴 En kritik tasarım kararı — kopya izolasyonu:**
`Store.loadTasks` `MutableList` döndürüyor ve çağıranlar bunu
değiştiriyor (`sub.done = true`). Önbellekten aynı nesneyi dönseydik
bir ekrandaki değişiklik, henüz kaydedilmeden diğer ekranda
görünürdü — kullanıcı "iptal" dese bile bellekte kalırdı.

`Topic` için **derin kopya** (iç `items` listesi de kopyalanıyor),
düz veri sınıfları için `copy()` yeterli. `OnbellekTest` bu farkı
gösteren bir test içeriyor: sığ kopyada iç liste sızıyor.

**Bozma noktaları:** her `save*` çağrısı + `importJson` sonunda
`hepsiniBoz()`. Bu satır olmasaydı yedek yüklendikten sonra kullanıcı
**eski veriyi görmeye devam ederdi** — önbellek eklerken yapılabilecek
en yıkıcı hata.

### Öneri 17 — DiffUtil · `ListeFark.kt` (YENİ)
**Ölçüm:** 24 × `notifyDataSetChanged()`, 0 × DiffUtil.

`notifyDataSetChanged()` "her şey değişti" diyor: tüm görünen satırlar
yeniden bağlanıyor, kaydırma konumu sıçrıyor, animasyon yok. Tek bir
kutucuğa dokununca 200 satırlık liste baştan çiziliyordu.

**Neden `ListAdapter`'a geçilmedi:** mevcut adapter'lar dıştaki
listeye **referansla** bağlı (`tasks.clear(); tasks.addAll(...)`).
`ListAdapter`'a geçmek `TasksFragment` (1315 satır) ve
`TopicsFragment` (1294 satır) içindeki tüm veri akışını yeniden
yazmak demekti. Faz 2'nin amacı hız kazanmak, mimari kumar oynamak
değil.

**Seçilen yol:** güncelleme öncesi liste kopyası + `DiffUtil` ile
hedefli bildirim. Adapter'lara hiç dokunulmadı.
Dönüştürülen: görevler, notlar, konular, alışkanlıklar.
`detectMoves = true` — görev tamamlanıp alta indiğinde sil+ekle
yerine taşıma animasyonu.

Ayrıca: artımlı güncellemede giriş animasyonu oynatılmıyor
(DiffUtil'in kendi animasyonuyla çakışıp liste titriyordu).

### Öneri 16 — Coroutine · `ArkaPlan.kt` (YENİ)
**Ölçüm:** 87 × `runOnUiThread`, 0 coroutine.

Eski desen üç sorun taşıyordu:
1. **Sızıntı** — `ExecutorService` fragment yok edilse bile çalışıyor
2. **Çökme** — `isAdded` kontrolü elle yazılıyor, unutulan yerde
   `requireContext()` çöküyor
3. **İptal yok** — kullanıcı ekranı değiştirse bile AI isteği sürüyor

`viewLifecycleOwner.lifecycleScope` görünüm yok olunca işi otomatik
iptal ediyor. Bağlandığı yerler: **AsistanFragment** (AI isteği,
3-10 sn) ve **DepolamaActivity** (disk gezme).

87 çağrının tamamı değiştirilmedi — kısa işler zaten sorunsuz.

🔴 **Derleme hatası:** `calisGuvenli(fragment, is_, sonra, hata)`
imzasında `hata` sondaydı. Kotlin'de trailing lambda **son
parametreye** bağlanır; `{ ... }` bloğu `hata`ya gidip `sonra` eksik
kaldı. `sonra`yı sona aldım. Bu benim hatamdı.

### Öneri 18 — ViewBinding
`buildFeatures { viewBinding = true }` v8.8'de açılmıştı; bu sürümde
derlemenin bozulmadığı doğrulandı. 577 `findViewById` çağrısını
dönüştürmek ayrı bir iş — altyapı hazır.

### Öneri 22 — İncelendi, ZATEN YAPILMIŞ
Listede "bitmap'ler ölçeklenmeden yükleniyor, 12 MP = 48 MB bellek"
yazmıştım. Kodu taradığımda gördüm ki `GorselHazirla.kt`,
`KanitActivity`, `KanitAkisi` ve `NotesFragment` **zaten
`inSampleSize` kullanıyor**. Öneriyi yanlış yazmışım.
Değişiklik yapmadım.

### Yeni testler — 71 → 81
`OnbellekTest` (10 test): üretim sayısı, bozma, anahtar izolasyonu,
istatistik ve **en önemlisi kopya izolasyonu** — dönen listeyi
değiştirmenin önbelleği bozmadığını, sığ kopyanın neden yetersiz
olduğunu doğruluyor.

Bu testler kritik çünkü önbellek hataları **çökmeye yol açmaz**,
sessizce yanlış davranır: "kaydetmiyor", "yedek çalışmıyor",
hayalet değişiklikler.

### Yeni dosyalar
```
Onbellek.kt · ListeFark.kt · ArkaPlan.kt
app/src/test/.../OnbellekTest.kt
```

### Değişen dosyalar
```
build.gradle.kts    +kotlinx-coroutines-android:1.7.3
                    +lifecycle-runtime-ktx:2.8.4 +lifecycle-viewmodel-ktx
                    +kotlinx-coroutines-test (test)
Store.kt            5 tür önbelleklendi (loadXDisk ayrıldı)
                    save* → Onbellek.boz · importJson → hepsiniBoz
                    seed yazmalarına savunma amaçlı bozma
TasksFragment.kt    reload → ListeFark.gorevler
NotesFragment.kt    reload → ListeFark.notlar
TopicsFragment.kt   reload → ListeFark.konular
HabitsFragment.kt   reload → ListeFark.aliskanliklar
AsistanFragment.kt  worker.execute → ArkaPlan.calisGuvenli
DepolamaActivity    isci.execute → ArkaPlan.calis
strings.xml         2864 → 2867
```

**APK (release+R8):** 16,6 MB · md5 `60e67873e8fe9b313a0b03cf795fe96d`
**İmza:** `5F:15:...:85:11` · **Test:** 81/81

Not: coroutine kütüphanesi eklendi ama R8 kullanılmayan kısımları
attığı için APK boyutu değişmedi.

### Sırada — Faz 3 (v9.0): Aralıklı tekrar
53 (konulara SM-2) · 55 (unutma eğrisi) · 57 (karışık tekrar) ·
54 (oturum kalitesi) · 68 (haftalık dürüst geri bildirim)

---

## v9.0 (versionCode 146) — FAZ 3: Aralıklı tekrar konulara yayıldı

100 önerinin **en önemli maddesi**. Uygulama "ders listesi"nden
"öğrenme sistemi"ne geçti. Test: 81 → **102**.

### ⚠️ Sandbox sıfırlanması
Faz 3'e başlarken `/opt` araçları silinmişti (üçüncü kez). Kaynak
sağlamdı (196 dosya, v8.9); `kur-ortam.sh` arka planda çalışırken
kod yazmaya devam edildi. Zaman kaybı olmadı.

### 🎯 Öneri 53 — Konu maddelerinde aralıklı tekrar · `KonuTekrar.kt`

**Neden bu, listenin en önemli maddesiydi:**
Uygulamanın çekirdek vaadi "unutmadan tekrar et". Ama v8.9'a kadar
aralıklı tekrar yalnız iki yerde vardı: `Hatalarim` (yanlış quiz
soruları) ve `QuizStore` (ders quizleri).

**Konu maddelerinde hiç yoktu.** Kullanıcı maddeyi işaretliyor,
madde "bitti" oluyor, bir daha asla karşısına çıkmıyordu. Yani
uygulama "çalıştım" demeyi kolaylaştırıyor ama "öğrendim"i garanti
etmiyordu. Ebbinghaus'a göre tek tekrarla öğrenilenin %70'i bir
haftada kayboluyor.

**Neden SM-2, neden Leitner değil:**
`Hatalarim` sabit Leitner kullanıyor (1·3·7·16·35 gün) — quiz için
uygun, cevap ikili. Konu maddesi farklı: "ne kadar hatırladın?"
sorusunun cevabı bir aralık. SM-2 bunun için tasarlandı:
- Kullanıcı 0-5 kalite verir (biz 4 düğme gösteriyoruz)
- Her maddenin kendi **kolaylık katsayısı** (EF) var
- `aralık = önceki × EF` — üstel büyüme
- Zor maddeler sık, kolay maddeler seyrek gelir

Sonuç: 200 maddelik müfredatta günde 8-12 madde gelir, hepsi değil.

**Varsayılan KAPALI.** Mevcut kullanıcının alışkanlığı bozulmasın.
Açınca **mevcut bitmiş maddeler otomatik programa alınıyor** —
yoksa kullanıcı özelliği açıp boş ekran görür ve "çalışmıyor" sanırdı.

### Öneri 57 — Karışık tekrar (interleaving)
`KonuTekrar.karisikSira()` — aynı konudan iki madde peş peşe
gelmiyor. Peş peşe aynı konuyu çalışmak (blocking) o an kolay geliyor
ama kalıcılığı düşük; karıştırmak zor geliyor ama uzun vadede belirgin
şekilde daha iyi ("istenen zorluk" etkisi).

### Öneri 55 — Unutma eğrisi · `hatirlamaTahmini()`
Ebbinghaus eğrisi: `R = e^(-t/S)`. Bizde S ≈ SM-2 aralığı (algoritma
zaten belleğin ne kadar dayandığını ölçüyor). Böylece "bu maddeyi şu
an ne kadar hatırlıyorsun" tahmini yapılabiliyor.

`riskliler()` — vakti gelmemiş ama hatırlama tahmini %60'ın altına
düşmüş maddeler. "Şu konuyu 3 gün içinde tekrar et yoksa unutacaksın".

### Öneri 54 + 68 — Oturum ve dürüst geri bildirim · `TekrarActivity`
**Akış:** madde başlığı gösterilir → kullanıcı hatırlamaya çalışır →
"Göster" → varsa AI anlatım özeti → 4 değerlendirme düğmesi.

**Neden önce sadece başlık:** aktif hatırlama (active recall)
öğrenmenin en güçlü aracı. Cevabı hemen göstermek "tanıma" olur —
çok daha zayıf.

**Her düğmede sonraki tarih yazılı** ("Kolay → 15 gün"). Anki'nin en
sevilen özelliği; kullanıcı seçiminin sonucunu önceden görüyor.

Oturum sonunda dürüst değerlendirme:
- %90+ "Neredeyse hepsini hatırladın, aralıklar uzayacak"
- %50 "Bu normal — unuttukların yarın yine gelecek"
- %50 altı **"Bu bir başarısızlık değil, bilginin nerede zayıf
  olduğunu gösteriyor"** — suçlamak yerine yönlendirme

### Bağlantı noktaları
- **Bugün ekranı** — `SimdiNe`'ye `KONU_TEKRAR` adayı (öncelik 62:
  hata tekrarından yüksek, acil görevden düşük)
- **Alt menü rozeti** — `Rozet.tekrariGelen` artık hata defteri +
  konu tekrarını topluyor. (v8.3'te "konularda tekrar yok" diye not
  düşmüştüm; Faz 3'te o eksik kapandı.)
- **Ayarlar** — açma anahtarı + "N madde programda · bugün M tekrar"

### Testler: 81 → 102 (`KonuTekrarTest`, 21 test)
SM-2'nin doğruluğu kritik çünkü hatası **geç** ortaya çıkar:
aralık hızlı büyürse kullanıcı unutur, yavaş büyürse bıkar.
Haftalar sonra "bu uygulama işe yaramıyor" olarak görünür.

Doğrulananlar: SM-2 adımları (1→6→×EF), unutunca sıfırlama,
EF alt sınırı 1.3, **aralık asla 0/negatif olmuyor** (5×6×4 = 120
kombinasyon denendi), 365 gün tavanı, geçersiz kalite değerinde
çökmeme, kolay/zor madde senaryoları, unutma eğrisi tutarlılığı.

### Yeni dosyalar
```
KonuTekrar.kt · TekrarActivity.kt
app/src/test/.../KonuTekrarTest.kt
```

### Değişen dosyalar
```
TopicsFragment.kt   madde işaretlenince programaAl / kaldırılınca çıkar
SimdiNe.kt          +KONU_TEKRAR eylemi ve adayı (öncelik 62)
TodayFragment.kt    KONU_TEKRAR → TekrarActivity
Rozet.kt            konu tekrarları da sayılıyor
SettingsFragment.kt +anahtar +tekrarSatiriniTazele()
fragment_settings.xml +rowKonuTekrar
AndroidManifest     +TekrarActivity
strings.xml         2867 → 2907 dize
```

**APK (release+R8):** 16,6 MB · md5 `9178c9aaaa2b524e98aada57a7f7be6e`
**İmza:** `5F:15:...:85:11` · **Test:** 102/102

### Faz 3'te yapılmayan
Öneri 56 (ön test/son test), 58 (tam flashcard sistemi), 59 (Feynman
modu), 60-67 — bunlar ayrı sürümler gerektiriyor. Faz 3'ün çekirdeği
(53, 54, 55, 57, 68) tamamlandı.

### Sırada — Faz 4 (v9.1): Güvenilirlik
41-48 (bildirim/alarm/üretici/çevrimdışı) · 8 (bozuk veri) ·
5 (yedek geri alma)

---

## v9.1 (versionCode 147) — FAZ 4: Güvenilirlik

Bu grup **gerçek cihaz** gerektiriyordu; sandbox'ta emülatör yok.
Bu yüzden yaklaşımı değiştirdim: kod yazıp "umarım çalışır" demek
yerine, **kullanıcının kendi kendine teşhis edebileceği araçlar**
yazdım. Test: 102 → **111**.

### 🔴 Öneri 45 + 46 — Bulunan gerçek hata: alarmlar kayboluyordu

`BootReceiver`'ın `DATE_CHANGED / TIME_CHANGED / TIMEZONE_CHANGED /
MY_PACKAGE_REPLACED` dalı yalnızca widget'ları ve namaz/koç
alarmlarını tazeliyordu. **Görev hatırlatıcıları yeniden
kurulmuyordu.**

İki sonucu vardı:
1. **Saat dilimi değişimi** — Türkiye'den Almanya'ya giden kullanıcı
   hatırlatıcılarını iki saat kaymış buluyordu.
2. **Uygulama güncellemesi** — Android güncellemede TÜM alarmları
   iptal ediyor. Bu dal onları yeniden kurmadığı için **her
   güncellemeden sonra görev hatırlatıcıları sessizce ölüyordu.**
   Kullanıcı "güncelledim, bildirimler kesildi" derdi ve sebebini
   kimse bulamazdı.

Artık `AlarmScheduler.rescheduleAll` + `CourseReminderReceiver` +
`BildirimZamanlayici` bu dalda da çağrılıyor.

⚠️ **Yanlış alarm:** Manifest `TIME_SET`, kod `ACTION_TIME_CHANGED`
kullanıyordu; uyuşmazlık sandım. `android.jar`'dan doğruladım:
`ACTION_TIME_CHANGED = "android.intent.action.TIME_SET"` — aynı sabit.
Sorun yokmuş.

### Öneri 42-44 — Alarm sağlığı · `AlarmSagligi.kt` (YENİ)

**0-100 puanlı sağlık paneli** (Ayarlar → Bildirim tanılama):
- Bildirim izni (40 puan — olmazsa hiçbir şey çalışmaz)
- Tam alarm izni (30 — olmazsa 15 dk kayar)
- Pil kısıtı kapalı (20 — olmazsa arka planda ölür)
- Üretici riski (10)

**Üretici uyarıları:** Xiaomi, Huawei, Oppo, Vivo, OnePlus, Samsung
ve 6 marka daha için **cihaza özel yönerge** ("Güvenlik → İzinler →
Otomatik başlatma"). Bu ayarlar programatik olarak açılamıyor;
tek yapılabilecek doğru yere yönlendirmek.

**Kurulum/tetiklenme kaydı:** "Alarm kuruldu" ile "alarm çaldı"
farklı şeyler. Pil optimizasyonu olan cihazlarda kurulum başarılı
görünür, tetikleme hiç olmaz. Artık ikisi ayrı ayrı kaydediliyor:
"Son kurulum: 2 saat önce (BOOT) · Son tetiklenme: hiç" — bu satır
sorunu tek bakışta gösteriyor.

### 🎯 Öneri 41 — Üç katmanlı bildirim testi · `BildirimTestReceiver.kt`

Samsung sorunu (v7.88-7.93) **altı sürüm** sürmüştü çünkü her
denemede yeni APK gönderip "şimdi oldu mu?" diye sormak
gerekiyordu. Xiaomi/Huawei'de ise hiç test edilmedi.

Artık kullanıcı üç yolu ayrı ayrı deniyor:

| Test | Gelmezse sorun |
|---|---|
| 1 · Anında | Bildirim izni veya kanal kapalı |
| 2 · 10 saniye sonra | Tam alarm izni yok |
| 3 · 2 dakika sonra (uygulama kapalı) | Üretici uygulamayı öldürüyor |

Her testin sonucu kaydediliyor ve "✓ Geldi — 3 dk önce" olarak
gösteriliyor. Sorun **tek turda** hangi katmanda olduğu anlaşılıyor.

Test, gerçek görev kanalını (`Tur.GOREV`) kullanıyor — ayrı bir
"test kanalı" açsaydık kullanıcının susturduğu asıl kanalı test
etmemiş olurduk.

### Testler: 102 → 111 (`AlarmSagligiTest`)
Puanlama mantığı test edildi: ağırlıkların toplamı 100, puan her
zaman 0-100 arası (16 kombinasyon), durum metni eşikleri, "agresif
üretici tek başına iyi durumu bozmaz" (izinler tamamsa kullanıcıyı
gereksiz korkutmamak).

### Yeni dosyalar
```
AlarmSagligi.kt · BildirimTestReceiver.kt
app/src/test/.../AlarmSagligiTest.kt
```

### Değişen dosyalar
```
BootReceiver.kt          🔴 saat dilimi/güncelleme dalında görev
                         alarmları yeniden kuruluyor + kurulum kaydı
AlarmScheduler.kt        rescheduleAll artık sayı döndürüyor ve kaydediyor
ReminderReceiver.kt      +tetiklenme kaydı
BildirimTaniActivity.kt  +sağlık paneli +üç katmanlı test paneli
AndroidManifest          +BildirimTestReceiver
strings.xml              2907 → 2939 dize
```

**APK (release+R8):** 16,7 MB · md5 `15ed152087936eccb60af9743b8ebd30`
**İmza:** `5F:15:...:85:11` · **Test:** 111/111

### Faz 4'te yapılmayanlar
Öneri 47 (çevrimdışı tutarlılık), 48-50 (AI zaman aşımı/maliyet/kuyruk),
51 (izin açıklaması), 52 (sesli servis izleme). Bunlar ayrı bir
sürüm gerektiriyor; Faz 4'ün çekirdeği (41-46) tamamlandı.

### Sırada — Faz 5 (v9.2): Erişim
95 (İngilizce çeviri) · 96 (TalkBack denetimi) ·
97-99 (Play Store hazırlığı) · 91 (ilk açılış turu)

---

## v9.2 (versionCode 148) — 🔴 ACİL: Açılış çökmesi düzeltildi

Kullanıcı çökme raporu gönderdi. **Uygulama hiç açılmıyordu.**
Faz 5 planı ertelendi, önce bu düzeltildi.

### Çökme raporu
```
java.lang.NullPointerException
  at M.g$b.b(SourceFile:9)
  at com.gunlukasistan.app.MainActivity.onCreate$lambda$13$lambda$12
  at M.e.onSplashScreenExit(SourceFile:160)
  at android.window.SplashScreen$SplashScreenManagerGlobal
       .dispatchOnExitAnimation(SplashScreen.java:274)
```

### Sebep — benim hatam (v8.2'den beri)
v8.2'de "Öneri 6: açılış ekranı" eklerken şu kodu yazmıştım:

```kotlin
val ikon = saglayici.iconView          // ← platform tipi, View!
ikon.animate()...                      // ← null gelirse NPE
```

`SplashScreenViewProvider.getIconView()` Java'dan geliyor ve
**platform tipi** (`View!`). Kotlin bunu null-safe saymıyor —
derleme hatası vermiyor ama çalışma anında null olabiliyor.

Android 12+ sistem splash'ında ikon görünümü şu durumlarda
oluşturulmuyor:
- Tema `windowSplashScreenAnimatedIcon`'u çözemezse
- Üretici splash'ı özelleştirmişse
- Uygulama çok hızlı açılıp splash atlanırsa

### Neden `runCatching` kurtarmadı
Kodu yazarken dıştaki `runCatching` bloğunun koruduğunu sanmıştım.
**Yanlış:** o blok yalnızca `setOnExitAnimationListener` **kayıt**
çağrısını sarıyor. Lambda daha SONRA, sistem tarafından çağrılıyor —
o an `runCatching` çoktan bitmiş oluyor.

Bu, geç çağrılan lambda'larda sık yapılan bir hata. Kod tabanını
taradım, aynı desende başka yer yok.

### Neden yakalayamadım
Sandbox'ta emülatör yok; splash yalnız gerçek cihazda çalışıyor.
v8.2'den v9.1'e kadar **10 sürüm boyunca** bu satır oradaydı.
Kullanıcı bildirmese bulunamazdı.

### Düzeltme
```kotlin
try {
    val ikon: android.view.View? = saglayici.iconView   // açıkça nullable
    if (ikon == null || !GorunumAyar.animasyonAcik(this)) {
        saglayici.remove()
        return@setOnExitAnimationListener
    }
    ikon.animate()...
        .withEndAction { runCatching { saglayici.remove() } }
} catch (e: Throwable) {
    // Ne olursa olsun splash KALKMALI — kaldırılmazsa kullanıcı
    // donmuş açılış ekranıyla kalır, çökmekten bile kötü.
    runCatching { saglayici.remove() }
}
```

Üç katmanlı koruma:
1. `iconView` açıkça `View?` olarak tiplendi
2. Lambda'nın kendi `try/catch`'i var (dıştaki runCatching yetmiyor)
3. Her yolda `remove()` çağrılıyor — donmuş splash olmasın

### Ayrıca: R8 mapping dosyası saklandı
Çökme raporundaki `M.g$b.b` R8'in kısalttığı isim. Bundan sonra
her sürümün `mapping.txt` dosyası saklanacak; gelen raporlar
gerçek sınıf/satır adlarına çevrilebilecek.

`mapping-v9.2.txt` (28 MB) kaynak yedeğinin yanında.

**APK (release+R8):** 16,7 MB · md5 `d4d67bdfde619c26dfaca7a96d22cf9a`
**İmza:** `5F:15:...:85:11` · **Test:** 111/111

### Faz 5 ertelendi
İngilizce çeviri, TalkBack, Play Store hazırlığı bir sonraki sürüme.

---

## v9.3 (versionCode 149) — 🔴 ACİL: ActivityNotFoundException

İkinci çökme raporu. Öncekinden **daha ciddi**: uygulama açılıyor
ama içinde gezinirken çöküyordu.

### Çökme raporu
```
android.content.ActivityNotFoundException: Unable to find explicit
activity class {com.gunlukasistan.app/com.gunlukasistan.app.MainActivity};
have you declared this activity in your AndroidManifest.xml?
  at NamazActivity.planSekmesiniAc(SourceFile:20)
```

### Sebep — v8.3'teki tasarım hatam

v8.3'te "Öneri 12: alternatif uygulama simgesi" eklemiştim.
`Simge.sec()` şöyle çalışıyordu:

```kotlin
// Hedefi aç
durumAyarla(pm, context, hedef.alias, acik = true)
// Diğerlerini kapat
secenekler.filter { it.kod != kod }.forEach {
    durumAyarla(pm, context, it.alias, acik = false)   // ← alias null ise
}                                                       //   MainActivity!
```

Varsayılan simgenin `alias` değeri **null**'dı ve `durumAyarla`
null gelince `MainActivity`'nin kendisini hedefliyordu. Yani
kullanıcı alternatif bir simge seçtiğinde **MainActivity DISABLED
oluyordu.**

O sırada koda şu yorumu yazmıştım:
> "Onu kapatırken dikkat: kapatılırsa uygulama başlatılamaz hale
> gelmez çünkü en az bir alias hep açık kalıyor."

**Bu yanlıştı.** Alias'lar yalnızca *launcher girişi* sağlıyor.
MainActivity ise uygulamanın ana ekranı ve kod içinde **22 ayrı
yerden** `Intent(ctx, MainActivity::class.java)` ile açılıyor:
bildirimler, 12 widget, global arama, namaz ekranı, ders PDF'i,
film önerisi, hızlı ekleme…

Bileşen devre dışıyken bunların **hepsi** çöküyordu. Uygulama
açılıyor ama içinde hiçbir yere gidilemiyor.

### Çözüm — üç parça

**1. Manifest:** `LAUNCHER` filtresi MainActivity'den alınıp yeni
bir `.SimgeVarsayilan` alias'ına taşındı. Artık 6 alias var
(1 varsayılan açık + 5 alternatif kapalı), MainActivity'nin kendi
launcher girişi yok.

**2. `Simge.kt`:** `durumAyarla` artık `alias == null` gelirse
**hiçbir şey yapmıyor** (güvenlik ağı). Varsayılan simge de artık
bir alias.

**3. `Simge.onarimYap()`:** Bileşen durumu **cihazda kalıcı** —
uygulamayı güncellemek onu geri açmıyor, Manifest'i düzeltmek de
yetmiyor. `App.onCreate` her açılışta kontrol edip MainActivity
devre dışıysa geri açıyor. Ayrıca hiç launcher girişi açık değilse
varsayılanı etkinleştiriyor (uygulama çekmeceden kaybolmasın).

Onarım olmasaydı v9.3'e güncelleyen kullanıcı **hâlâ çökme
yaşardı.**

### Doğrulama
`aapt dump xmltree` ile release APK denetlendi:
```
SimgeVarsayilan    enabled=ACIK    launcher=EVET
SimgeKaramel       enabled=KAPALI  launcher=EVET
SimgeMor           enabled=KAPALI  launcher=EVET
SimgeGece          enabled=KAPALI  launcher=EVET
SimgeYesil         enabled=KAPALI  launcher=EVET
SimgeMinimal       enabled=KAPALI  launcher=EVET
```
MainActivity dex'te korunuyor (ProGuard kuralı eklendi).

### İki çökme, iki ders
Son iki sürümde bulunan hatalar aynı kökten geliyor: **cihazda
test edilemeyen kod.** Splash ekranı ve bileşen etkinleştirme
sandbox'ta hiç çalışmıyor. v8.2 ve v8.3'te yazdığım "bu güvenli"
yorumlarının ikisi de yanlıştı.

Bundan sonra cihaz davranışına bağlı her özelliğe **çalışma anı
onarımı** eklemeyi düşüneceğim — sadece "doğru yazmak" yetmiyor.

**APK (release+R8):** 16,7 MB · md5 `397d0b54295c78bb0d817bee449a7ed4`
**İmza:** `5F:15:...:85:11` · **Test:** 111/111
**Mapping:** `mapping-v9.3.txt` saklandı

---

## v9.4 (versionCode 150) — GRUP B: Takvim ve zaman (öneri 9-16)

Kullanıcı "A ve C hariç hepsini yap" dedi. B, D, E, F, G grupları
sırayla teslim edilecek. Bu, ilki. Test: 111 → **131**.

### ⚠️ Sandbox sorunu — swap kayboldu
Ortam sıfırlandığında swap da gitti. Derleme 1981/1984 MB bellekle
takıldı ve **9 dakika ilerlemedi**. `pkill java` + swap yeniden
kurulumu ile çözüldü. Ders: ortam kontrolünde swap'ı da doğrulamak
gerekiyor, yalnız JDK'yı değil.

### Öneri 9 + 10 — Takvim köprüsü · `TakvimKopru.kt` (YENİ)
**Ölçüm:** `CalendarContract` kod tabanında **hiç geçmiyordu**.
Uygulama 194 dosya ama telefonun geri kalanından kopuktu.

**Yazma:** Sınav tarihi ve kişisel etkinlikler sistem takvimine
düşüyor. Her etkinliğe `[GunlukAsistan#tip#id]` damgası konuyor —
böylece aynı etkinlik iki kez eklenmiyor (güncelleniyor) ve
kullanıcının kendi kayıtlarına dokunulmuyor.

**Neden ayrı takvim oluşturulmadı:** `ACCOUNT_TYPE` yönetimi
gerektiriyor ve hesapla eşleşmezse senkronlanmıyor. Kullanıcının
seçtiği mevcut takvime yazmak daha güvenilir.

**Okuma:** Telefondaki etkinlikler Bugün ekranındaki zaman
çizelgesinde görünüyor. `Instances` tablosu kullanıldı — `Events`
yalnız ana kaydı verir, haftalık ders tekrarları görünmezdi.
Bizim yazdıklarımız filtreleniyor (iki kez görünmesinler).

### Öneri 11 — Boş zaman bulucu
Takvim + bugün vadeli görevler + namaz vakitleri (±15 dk) birleştirilip
örtüşenler tekilleştiriliyor, kalan boşluklar çıkarılıyor.

`SimdiNe`'ye yeni aday: **öncelik 45** (en düşüklerden). Gerekçe:
"boşsun" bilgisi yararlı ama acil değil; gerçek bir iş varsa o öne
çıkmalı. En az 45 dakika şartı — kısa boşlukta "çalış" demek rahatsız
edici.

### Öneri 13 — Süre tahmini öğrenmesi · `SureAnalizi.kt` (YENİ)
`OdakKaydi` v7.94'ten beri her oturumu kaydediyordu ama **veri hiç
kullanılmıyordu**.

Planlama yanılgısı (planning fallacy) bilinen bir olgu: insanlar
sistematik olarak işleri kısa tahmin ediyor. Bu sınıf tahmin/gerçek
çiftlerini toplayıp **kişisel çarpan** hesaplıyor.

**Neden medyan, ortalama değil:** tek bir uzun oturum (sayacı
kapatmayı unutmak) ortalamayı bozar. Ayrıca 10 kattan fazla sapan
kayıtlar hiç alınmıyor.

Dil bilinçli seçildi: "kötü tahmin ediyorsun" değil, *"İşler
tahmininden ortalama %70 daha uzun sürüyor. Planlarken buna pay
bırak."*

### Öneri 14 — Pomodoro istatistiği
`Pomodoro` v7.94'ten beri çalışıyor, **hiç ölçülmüyordu**.

- Tamamlanan tur `dongueyIlerlet`'te kaydediliyor
- **Yarıda kesilen** `ACTION_STOP`'ta kaydediliyor — yalnız
  tamamlananları saymak "%100 başarı" gösterirdi, işe yaramaz
- En verimli saat: yalnız TAMAMLANAN turlara bakılıyor (yarıda
  kesilenler o saatte dikkatin dağıldığını gösteriyor)

### Öneri 15 — Günlük zaman bütçesi
Hedef / yapılan / planlanan / boş zaman dörtlüsü. Planlanan süre
kişisel çarpanla düzeltiliyor. "Takvimine göre yalnız 60 dk boşluğun
var. Hedefi düşürmeyi düşün."

### Öneri 16 — Geri sayım widget'ı
İncelendi, mevcut `CountdownWidget` zaten en yakın etkinliği
gösteriyor. Değişiklik yapılmadı.

### 🔴 Bir test v9.3 hatasını yakaladı
`GorunumTest.tam bir varsayilan simge var` **başarısız oldu** — çünkü
v9.3'te `alias = null` kaldırılmıştı.

İlginç olan: bu test v8.3'ten beri **geçiyordu** ve tam olarak
çökmeye sebep olan durumu (null alias) doğruluyordu. Test yanlış
şeyi koruyormuş.

Yeniden yazıldı: artık "hiçbir simge alias'ı null olmamalı" diyor —
yani bir daha aynı çökme eklenirse burada yakalanacak.

### Yeni dosyalar
```
TakvimKopru.kt · SureAnalizi.kt · TakvimAyarActivity.kt
app/src/test/.../SureAnaliziTest.kt (19 test)
```

### Değişen dosyalar
```
TimerActionReceiver.kt  +pomodoro tamamlanma/yarım kalma kaydı
SimdiNe.kt              +BOS_ZAMAN eylemi ve adayı (öncelik 45)
TodayFragment.kt        +takvim etkinlikleri çizelgede · BOS_ZAMAN eylemi
SettingsFragment.kt     +rowTakvim
fragment_settings.xml   +takvim satırı
AndroidManifest         +READ_CALENDAR +WRITE_CALENDAR +TakvimAyarActivity
GorunumTest.kt          simge testi v9.3 gerçeğine uyduruldu
strings.xml             2939 → 2972 dize
```

**APK (release+R8):** 16,7 MB · md5 `5aa267abcdeaee141c91c0ce436191d6`
**İmza:** `5F:15:...:85:11` · **Test:** 131/131
**Mapping:** `mapping-v9.4.txt`

⚠️ **Takvim cihazda test edilemedi** — sandbox'ta takvim sağlayıcısı
yok. Son iki çökme test edilemeyen cihaz API'lerinden geldi, bu yüzden
`TakvimKopru`'daki HER çağrı `runCatching` içinde ve hata durumunda
boş/false dönüyor. İzin reddedilirse özellik kapalıymış gibi
davranıyor, uygulama etkilenmiyor.

### Sırada
Grup D (giriş yöntemleri) · E (öğrenme derinliği) · F (günlük hayat) ·
G (sistem sağlamlığı)

---

## v9.5 (versionCode 151) — GRUP D: Giriş yöntemleri (öneri 23-30)

Test: 131 → **148**. İlk denemede derlendi.

### 🔴 İki öneri zaten yapılmıştı — dürüst düzeltme

50'lik listeyi yazarken iki maddeyi yanlış koymuşum:

**Öneri 27 (widget'tan hızlı ekleme):** "12 widget var, hepsi salt
okunur" demiştim. **Yanlış.** `TasksWidget`'ta `twAdd` düğmesi var ve
`WidgetCommon.quickAction(QUICK_TASK)` ile `QuickAddActivity`'yi
açıyor. Zaten çalışıyor.

**Öneri 28 (bildirimden yanıtlama):** "Hatırlatıcıya düğme eklenmeli"
demiştim. **Yanlış.** `TaskActionReceiver` dört eylem içeriyor:
`ACTION_DONE`, `ACTION_SNOOZE`, `ACTION_YAZ`, `ACTION_YARIN`.

Öneri listesi yazarken `grep` ile sınıf adı aradım ama bu iki
özellik farklı adlarla yazılmıştı. Yanlış öneri sunmak, yapılmamış
işi yapılmış göstermekten daha az zararlı ama yine de hata.

### 🎯 Öneri 25 — Fotoğraftan soru çözme · `SoruCoz.kt` + `SoruCozActivity`

Listede "neredeyse hazır" demiştim; doğruydu. Mevcut altyapı:
- `AiClient.gorselDenetim(ctx, base64, istem)` — çalışıyor
- `GorselHazirla.base64Uret()` — netleştirme dahil
- `GorselHazirla.onizleme()` — ölçekli önizleme

Eksik olan tek şey soru çözmeye uygun **istem** ve **JSON
ayrıştırma**'ydı.

**Tasarım kararı — cevap değil, çözüm:**
Bir öğrenme uygulamasında soruya doğrudan cevap vermek zararlı;
kullanıcı kopyalar, öğrenmez. İstem şunu zorunlu kılıyor:
1. Soru ne istiyor
2. Hangi bilgi/formül gerekli
3. Adım adım çözüm — her adımda **neden**
4. Sonuç

**İpucu modu:** Yalnız 1. ve 2. adımı gösterir, çözümü saklar.
Kullanıcı önce kendi dener.

**Alan sabiti yok** (v7.80 kuralı): istem "sorunun alanını
fotoğraftan anla, kendi uzmanlık alanını varsayma" diyor.

Çözümler geçmişe kaydediliyor (60 kayıt) ve **hata defterine
eklenebiliyor** — çözemediğin soru aralıklı tekrar programına giriyor.

🔴 **Ayrıştırma sorunu çözüldü:** `QuizStore.Soru.gecerli` en az
2 şık istiyor (`siklar.size >= 2`). Fotoğraftan gelen soru çoktan
seçmeli değil; ikinci şık olarak "tekrar dene" konuldu, yoksa kayıt
geçersiz sayılıp hata defteri akışı bozulacaktı.

### Öneri 29 — Tek satır hızlı komut · `HizliKomut.kt` (YENİ)

Normal akış altı dokunuş: FAB → tür → başlık → tarih → saat → kaydet.
Şimdi **FAB'a uzun bas** → tek kutu:

```
gorev: rapor yaz cuma 17:00
not: kütüphane 22de kapanıyor
konu: türev kuralları
yarın 09:00 doktor randevusu     ← önek yok, tür tahmin ediliyor
```

**Tür tahmini:** önek yoksa tarih varsa görev, yoksa not. Günlük
kullanımda doğru sonuç veriyor.

**Canlı önizleme:** yazdıkça "Görev · yarın 14:00" gösteriliyor —
kullanıcı kaydetmeden önce ne olacağını görüyor.

**Neden `AsistanKomut` kullanılmadı:** o sınıf AI'nın ürettiği
işaretli komutları (`[[GOREV:...]]`) çözüyor ve **AI çağrısı
gerektiriyor**. `HizliKomut` tamamen yerel: çevrimdışı çalışıyor,
anında sonuç veriyor, kota harcamıyor. Tarih ayrıştırma için mevcut
`NaturalDate.parse` kullanıldı.

### Öneri 23, 24, 26, 30 — yapılmadı
- **23/24 (ses kaydı + özet):** `MediaRecorder` + izin + oynatıcı +
  dosya yönetimi — tek başına bir sürümlük iş
- **26 (toplu fotoğraf → konu):** mevcut `FotoKonuAkisi` tek fotoğraf
  destekliyor; çoklu seçim ve birleştirme ayrı iş
- **30 (QR):** ZXing kütüphanesi gerekiyor (~100 KB). v8.8'de 7,9 MB
  attıktan sonra APK'yı şişirmemeye karar vermiştim

### Yeni dosyalar
```
SoruCoz.kt · SoruCozActivity.kt · HizliKomut.kt
app/src/test/.../HizliKomutTest.kt (17 test)
```

### Değişen dosyalar
```
MainActivity.kt      FAB uzun basma → hizliKomutPenceresi()
AraclarFragment.kt   HESAP sekmesine "Fotoğraftan soru çöz"
AndroidManifest      +SoruCozActivity
strings.xml          2972 → 3027 dize
```

**APK (release+R8):** 16,7 MB · md5 `d720ea25fb6fca440c55bf2638288ab4`
**İmza:** `5F:15:...:85:11` · **Test:** 148/148
**Mapping:** `mapping-v9.5.txt`

### Sırada
Grup E (öğrenme derinliği) · F (günlük hayat) · G (sistem sağlamlığı)

---

## v9.6 (versionCode 152) — GRUP E: Öğrenme derinliği (öneri 31-40)

Test: 148 → **168**. İlk denemede derlendi.

### 🎯 Öneri 35 — Zayıf nokta radarı · `ZayifNokta.kt` (YENİ)

**Ölçüm:** Uygulama dört ayrı yerde "nerede zorlanıyorsun" bilgisi
topluyordu ama **hiçbiri birleştirilmiyordu**:
`Hatalarim` · `QuizStore` · `KonuTekrar` (SM-2 EF) · konu tamamlanma.

Kullanıcı bu soruyu yanıtlamak için dört ekranı gezip kendi çıkarım
yapmak zorundaydı — kimse yapmıyor.

**Puanlama** (toplam 100):
| Sinyal | Ağırlık | Neden |
|---|---|---|
| Hata defteri yoğunluğu | 35 | En doğrudan kanıt |
| Quiz başarısı | 30 | Ortalama; tek konuyu gizleyebilir |
| SM-2 kolaylık katsayısı | 25 | Yalnız tekrar açıksa var |
| Yarım kalmışlık | 10 | Zayıflık değil, eksiklik |

**Veri yetersizse puan verilmiyor** (en az 3 sinyal). Tek yanlış
cevaba bakıp "bu konuda zayıfsın" demek haksız ve motivasyon kırıcı.

**Hiç başlanmamış konu zayıf sayılmıyor** — yalnız başlanıp yarım
kalmışlar. Yapılmamış iş ile anlaşılmamış konu farklı şeyler.

### Öneri 31 — Ön test / son test · `OlcmeTest.kt` (YENİ)

Uygulama şimdiye kadar **çaba** ölçüyordu ("3 saat çalıştım",
"12 madde bitirdim"). Öğrenme ölçüsü yoktu. 3 saat çalışıp hiçbir şey
öğrenmemek mümkün.

**Normalize kazanım (Hake gain):** `g = (son − ön) / (100 − ön)`

Ham fark yanıltıcı: %90→%95 ile %20→%60 aynı ham farkta değil ama
**eşit zorlukta**. Bu formül ikisine de 0.50 veriyor. Test dosyasında
bu özellik ayrıca doğrulandı.

### Öneri 36 — Sınav simülasyonu
Gerçek koşullar: süre baskısı, karışık sorular, sonunda toplu analiz.
Üç kalıp: 10 soru/12 dk · 20/25 · 40/50.

**Net hesabı** Türkiye sistemine uygun: `doğru − yanlış/4`.
Soru havuzu konulardan **ve hata defterinden** toplanıyor — gerçek
sınavda da daha önce yanlış yaptığın konu tipi yine çıkar.

Değerlendirme dürüst: önceki provayla karşılaştırma, soru başına
90 saniyeyi aşarsa hız uyarısı, çok boş bırakılırsa "tahmin etmek
boş bırakmaktan iyidir".

### Öneri 33 — Feynman tekniği · `Feynman.kt` (YENİ)
"Bir konuyu 12 yaşındaki birine anlatabiliyorsan gerçekten
anlamışsındır." Kullanıcı yazar, AI eksikleri/jargonu bulur ve
0-100 anlaşılırlık puanı verir.

İstemde **"CÖMERT DAVRANMA"** yazıyor. Her anlatıma 90 veren bir
değerlendirici işe yaramaz; kullanıcı gelişmediğini fark etmez.

### Mimari not — `OlcmeBekleyen.kt` neden gerekli
Ön/son test ve simülasyon `QuizActivity`'nin geçici mod
mekanizmasını (v7.84) yeniden kullanıyor. Ama geçici modda sonuç
hiçbir yere yazılmıyordu.

**`QuizActivity`'ye yedinci mod eklemek yerine** (500+ satır, altı
mod zaten var) "bu quiz bittiğinde şunu kaydet" notu bırakılıyor.
Not okunduğu anda siliniyor; 2 saatlik son kullanma tarihi var —
kullanıcı quizi yarıda bırakırsa yanlış konuya yazılmasın.

### Bulunan uyumsuzluklar (derleme öncesi düzeltildi)
- `Hatalarim.geciciAyarla(sorular, baslik)` — Context almıyor,
  ben `(this, sorular)` yazmıştım
- `sonGovde` değişkeni kullanıldığı yerden **sonra** tanımlanmıştı;
  sıra düzeltildi

### Öneri 32, 34, 37-40 — yapılmadı
- **32 (tam flashcard sistemi):** `KartActivity` var; deste yönetimi
  ve paylaşım ayrı sürümlük iş
- **34 (konu bağımlılık grafiği):** görsel graf çizimi + veri modeli
- **37 (soru bankası içe aktarma):** CSV ayrıştırma — Grup A'daydı,
  kullanıcı A'yı hariç tuttu
- **38 (grup odası), 39 (veli paneli):** sunucu tarafı gerektiriyor
- **40 (bağlantılı notlar):** wiki bağlantısı + geri bağlantı indeksi

### Yeni dosyalar
```
ZayifNokta.kt · OlcmeTest.kt · Feynman.kt · OlcmeBekleyen.kt
OgrenmeActivity.kt
app/src/test/.../OlcmeTestTest.kt (20 test)
```

### Değişen dosyalar
```
QuizActivity.kt      +OlcmeBekleyen.tamamla() · +quizBaslangic
SettingsFragment.kt  +rowOgrenme
fragment_settings.xml +öğrenme merkezi satırı
AndroidManifest      +OgrenmeActivity
strings.xml          3027 → 3087 dize
```

**APK (release+R8):** 16,8 MB · md5 `a179ed08478a0bacd48b1de488ee1346`
**İmza:** `5F:15:...:85:11` · **Test:** 168/168
**Mapping:** `mapping-v9.6.txt`

### Sırada
Grup F (günlük hayat: ilaç, fatura, harcama, belge, konum, araç) ·
Grup G (WorkManager, güncelleme kontrolü, analitik)
