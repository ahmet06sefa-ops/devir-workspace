# 🌐 Günlük Asistan v11.20 — Sürüm Notları (HabitGenius Ayrı Uygulama + 1. Görünüme Geç)

**Sürüm:** v11.20 · **versionCode:** 276 · **versionName:** "11.20"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Jetpack Compose + Material 3, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.924 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 146 suite)

---

## 🎯 Bu sürümün teması: 2. Görünüm Ayrı Uygulama Gibi + 1. Görünüme Geç

Kullanıcı isteği: **"2. Görünümü ayrı bir uygulama gibi aç; ama 1. Görünüme geçmek için kendi ayarlarına '1. Görünüme Geç' yeri koy."**

Bu doğrultuda:
- **2. Görünüm (HabitGenius)** artık tam ekran, ayrı bir uygulama deneyimi gibi açılır.
- Kendi **Ayarlar** ekranı (üstteki ⚙ düğmesi) eklendi.
- Ayarlar içinde **"🎨 1. Görünüme Geç"** butonu var → seçilince klasik görünüme döner ve
  HabitGenius ekranı kapanır; alttaki klasik ana ekran açılır.

## ⚙️ Nasıl çalışır

1. 2. Görünüm (Habit Genius) seçilince `HabitGeniusComposeActivity` açılır (5 sekme: Alışkanlık, Ruh Hali, Finans, Odaklan, Günlük).
2. Sağ üstteki **⚙ Ayarlar** düğmesine dokun → Ayarlar ekranı.
3. **"🎨 1. Görünüme Geç"** butonuna dokun → `ThemeManager.gorunumModu = KLASIK` yapılır,
   widget'lar tazelenir ve HabitGenius Activity kapanır → klasik görünüm açık kalır.

## 💎 Kodun Sağladığı Özellikler (korundu)

- **Bento Box tasarımı** — `#161616` kartlar + `16.dp` oval köşeler.
- **OLED & Dark premium tema** — `#0C0C0C` zemin, modül vurguları.
- **5 ekran:** Alışkanlık, Ruh Hali, Finans, Odaklan, Günlük.
- **Geçiş mimarisi** — `AnimatedContent` fade in/out.
- **Canlı durumlar** — Pomodoro gerçek geri sayım, su ±, alışkanlık onay.

## 🧱 Teknik Değişiklikler

- **`HabitGeniusCompose.kt`:** `HabitGeniusComposeActivity` + `HabitGeniusMainScreen()` +
  yeni **`HabitGeniusSettingsScreen`** ("1. Görünüme Geç") + 5 ekran + `@Preview`.
- **`ThemeFragment`:** "2. Görünüm (Habit Genius)" → `HabitGeniusComposeActivity.ac(ctx)`.
- **`MainActivity.onCreate`:** `habitGeniusMu` doğruysa (soğuk açılış) Compose ekranı açılır.
- **`AndroidManifest.xml`:** `.HabitGeniusComposeActivity` kayıtlı.
- **`HabitGeniusComposeTest`:** Activity + MainScreen + SettingsScreen yüklenebilirliği (3 test).
- **Düzeltmeler:** `ChipDefaults`→`SuggestionChipDefaults`; `onNewIntent` nullable imza.

## 🧪 Testler

- **1.924 test, 0 hata, 0 başarısızlık** (+3 `HabitGeniusComposeTest`).
- Tüm koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) geçiyor.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.20.apk`
2. `kaynak-v11.20-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.20-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.19 (275):** HabitGenius Tam Compose Sürümü (2. Görünüm)
- **v11.16 (272):** 2. Görünüm (Habit Genius teması) + 1. Görünüm Klasik seçimi
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
