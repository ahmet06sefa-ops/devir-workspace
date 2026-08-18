# 🌐 Günlük Asistan v11.22 — Sürüm Notları (Tek APK: 2. Görünüm İçeride Açılır)

**Sürüm:** v11.22 · **versionCode:** 278 · **versionName:** "11.22"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Jetpack Compose + Material 3, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.924 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 146 suite)

---

## 🎯 Bu sürümün teması: İkisini tek APK yap — 2. Görünüm içeride açılsın

Kullanıcı isteği: **"İkisini tek bir APK yap; 2. Görünüm dediğimde Günlük Asistan'ın içinde açsın."**

Bu doğrultuda HabitGenius 2. Görünümü, **tek APK'nın içine** (native Jetpack Compose olarak) gömüldü.
Artık iki ayrı uygulama yok; tek `GunlukAsistan-v11.22.apk` kurulur.

## ✅ Nasıl çalışıyor

1. **Tek APK** (`GunlukAsistan-v11.22.apk`) kurulur.
2. Ayarlar → Görünüm → **"2. Görünüm (Habit Genius)"** seçilince HabitGenius **Günlük Asistan'ın
   içinde** (yeni ekranda) açılır.
3. 2. Görünüm modu aktifken uygulama açılışında da HabitGenius ekranıyla başlar.
4. Sağ üstteki **⚙ Ayarlar** → **"1. Görünüme Geç"** ile klasik görünüme dönülür.

## 📱 İçerideki HabitGenius (Jetpack Compose, tek APK)

Kullanıcının sağladığı tam sürümün Compose karşılığı tek dosyada:
1. **Alışkanlık** (zümrüt) — 7 günlük takvim, streak, checkbox + su sayacı, ısı haritası
2. **Ruh Hali** (mor) — 5 emoji, tetikleyici etiketler, AI çıkarımı
3. **Finans** (mavi) — gradyan bakiye, harcama dağılımı, işlemler
4. **Odaklan** (kırmızı) — 25:00 dairesel sayaç, çalışan geri sayım
5. **Günlük** (kehribar) — notebook alanı + düzenleme çubuğu

## 🧱 Teknik Değişiklikler

- **`app/build.gradle.kts`:** Compose BOM 2024.06.00 + Material 3 + activity-compose.
- **`HabitGeniusCompose.kt`:** `HabitGeniusComposeActivity` + `HabitGeniusMainScreen` + 5 ekran + `@Preview`.
- **`ThemeFragment`:** 2. Görünüm → `HabitGeniusComposeActivity.ac(ctx)` (içeride açar).
- **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa Compose ekranı içeride açılır.
- **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
- **`HabitGeniusComposeTest`:** Activity + MainScreen + SettingsScreen (3 test).
- **Düzeltme:** `onNewIntent` nullable imza.

## 🧪 Testler

- **1.924 test, 0 hata, 0 başarısızlık** (+3 `HabitGeniusComposeTest`).
- Tüm koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) geçiyor.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.22.apk` (TEK APK — 2. Görünüm içeride)
2. `kaynak-v11.22-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.22-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.21 (277):** HabitGenius ayrı uygulama köprüsü + v3.3.1 APK
- **v11.20 (276):** HabitGenius ayrı uygulama + 1. Görünüme Geç ayarı
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
