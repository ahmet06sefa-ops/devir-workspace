# 🌐 Günlük Asistan v11.17 — Sürüm Notları (HabitGenius 2. Görünümü Gömüldü)

**Sürüm:** v11.17 · **versionCode:** 273 · **versionName:** "11.17"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.923 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 146 suite)

---

## 🎯 Bu sürümün teması: HabitGenius 2. Görünümü Android'e Gömüldü

Kullanıcı isteği: **"HabitGenius prototipini Android uygulamasının 2. Görünümüne WebView ile göm."**

Bu doğrultuda premium 5 sekmeli HabitGenius prototipi, Android uygulamasının içine
**gerçek bir WebView ekranı** olarak gömüldü. "2. Görünüm (Habit Genius)" seçildiğinde
uygulama bu ekranı açar; uygulama "2. Görünüm" modundayken ana ekran yerine de bu ekran
açılır. "1. Görünüm (Klasik)" seçiliyse her şey eskisi gibi çalışır.

## 📱 Neler Gömüldü — `HabitGeniusActivity` + `assets/habitgenius.html`

- **`HabitGeniusActivity.kt`** — WebView tabanlı tam ekran:
  - `file:///android_asset/habitgenius.html` yüklenir (JavaScript + DOM storage açık).
  - Geri tuşu önce WebView iç geçmişini geri alır, sonra ekranı kapatır.
  - Koyu zemin (`#121212`), `styleFor` temalı.
- **`assets/habitgenius.html`** — premium tek-sayfa prototip:
  1. **HABIT TRACKER** (zümrüt) — 7 günlük takvim şeridi, 🔥 streak, checkbox/counter habit kartları + mikro ilerleme çubuğu, GitHub tarzı yıllık ısı haritası.
  2. **MOOD JOURNAL** (mor) — ruh hali emojileri, tıklanabilir Tag Cloud, Mood Analytics + AI insight.
  3. **EXPENSE TRACKER** (mavi) — Total Balance + Cash/Bank/Savings kartları, Donut Chart, renk kodlu işlem listesi.
  4. **FOCUS TIMER** (kırmızı) — 25:00 dairesel halka, çalışan Start/Pause/Reset, kategori seçici.
  5. **DIGITAL JOURNAL** (amber) — editör başlığı + zengin-metin araç çubuğu.

## 🔗 Entegrasyon

1. **`ThemeFragment`** — "2. Görünüm (Habit Genius)" kartına dokununca
   `HabitGeniusActivity.ac(ctx)` ile gömülü ekran hemen açılır.
2. **`MainActivity.onCreate`** — `ThemeManager.habitGeniusMu(this)` doğruysa (soğuk
   açılışta, `savedInstanceState == null`) HabitGenius ekranı açılır → 2. Görünüm modu
   uygulamanın ana deneyimi olur.
3. **`AndroidManifest.xml`** — `.HabitGeniusActivity` kayıtlı, `Theme.GunlukAsistan`.
4. **Koruma testleri** — `HabitGeniusEntegrasyonTest`: Activity ve Companion sınıfının
   yüklenebildiğini doğrular (ActivityNotFoundException'ı önler).

## 🧪 Testler

- 1.921 → **1.923 test, 0 hata** (+2 `HabitGeniusEntegrasyonTest`).
- Tüm koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) geçiyor.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.17.apk`
2. `kaynak-v11.17-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.17-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.16 (272):** 2. Görünüm (Habit Genius teması) + 1. Görünüm Klasik seçimi (1.921 test)
- **v11.15 (271):** Yeni Görünüm (koyu modern tema) — Eski Görünüm ayarlarda (1.914 test)
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme (1.906 test)

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
