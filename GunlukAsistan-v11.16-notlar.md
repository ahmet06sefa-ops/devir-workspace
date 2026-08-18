# 🌐 Günlük Asistan v11.16 — Sürüm Notları (2. Görünüm: Habit Genius)

**Sürüm:** v11.16 · **versionCode:** 272 · **versionName:** "11.16"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.921 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 145 suite)

---

## 🎯 Bu sürümün teması: 2. Görünüm — Habit Genius tarzı

Kullanıcı isteği: **"Uygulamayı [Habit Genius](https://play.google.com/store/apps/details?id=com.habitgenius.habit.tracker)
görünümünde yap. Bunu **2. Tema** olarak ayarlara ekle. Mevcut tüm özellikler/temalar
**1. Tema** olarak kalsın. Aralarında seçim yapabileyim."**

Bu doğrultuda:
- **2. Görünüm (Habit Genius)** eklendi — ayarlardaki Görünüm ekranında bir anahtar.
- **1. Görünüm (Klasik)** — mevcut tüm temalar, özellikler ve davranış birebir korundu.
- Kullanıcı Görünüm ekranından istediği görünümü seçer; tek dokunuşla değişir.

## 🌐 2. Görünüm — Habit Genius tarzı tema

Habit Genius'ın görünüm kimliğinden alınan **açık, mor-mavi vurgulu, beyaz zeminli** tema
(Material3 Light tabanlı):

| Renk alanı | Değer | Kaynak |
|---|---|---|
| Ana vurgu (`colorPrimary`) | `#6C5CE7` | Habit Genius mor |
| Vurgu kapsayıcı (`colorPrimaryContainer`) | `#E7E2FB` | açık mor |
| İkincil (`colorSecondary`) | `#1E9E5A` | yeşil onay |
| Zemin (`android:colorBackground`) | `#F6F5FB` | pastel beyaz |
| Yüzey (`colorSurface`) | `#FFFFFF` | beyaz kartlar |
| Metin (`colorOnSurface`) | `#1B1B2B` | koyu lacivert |
| Durum çubuğu | `#6C5CE7` | mor |

- Sabit **açık** tema (koyu moda geçmez), Mor-Yeşil-Mavi üçlü vurgu.
- Tema değişince `@colorPrimary`/`@colorSurface` kullanan tüm ekranlar otomatik uyum sağlar.

## 🎛️ 1. Görünüm / 2. Görünüm Seçimi

Görünüm ekranının üstüne **"GÖRÜNÜM"** bölümü eklendi; iki karttan seçim yapılır:
- **1. Görünüm — Klasik** 🎨 (mevcut tüm temalar: Krem, Violet, …, Zincir)
- **2. Görünüm — Habit Genius** 🌐 (yeni tema)

## 🔧 Teknik Değişiklikler

1. **`ThemeManager.kt`** — `gorunum_modu_v1` anahtarı:
   - `GORUNUM_KLASIK = 1` (varsayılan) / `GORUNUM_HABITGENIUS = 2`.
   - `gorunumModu` / `habitGeniusMu` getter-setter.
   - `styleFor` → Habit Genius seçiliyken `Theme.GunlukAsistan.HabitGenius`.
   - `geceModunuUygula` / `koyuMu` → Habit Genius modunda sabit açık tema.
   - Saf test fonksiyonları: `gorunumModuSaf`, `habitGeniusKoyuMu`.
2. **`values/themes.xml`** — `Theme.GunlukAsistan.HabitGenius` (Material3 Light).
3. **`fragment_theme.xml`** — üstte "GÖRÜNÜM" başlığı + `gorunumRow`.
4. **`ThemeFragment.kt`** — `gorunumSatiriKur` + `gorunumKarti` (1/2 seçim kartları).
5. **`strings.xml`** — `gorunum_section`, `gorunum_caption`, `gorunum_klasik`, `gorunum_habit`, `gorunum_secildi`.
6. **`GorunumModuTest.kt`** — 7 saf koruma testi.

## 🧪 Testler

- 1.914 → **1.921 test, 0 hata** (+7 `GorunumModuTest`).
- Koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) dahil
  **tümü geçiyor** — görünüm değişikliği sert `textSize`/`cardCornerRadius` içermez, `@dimen` uyumlu.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.16.apk`
2. `kaynak-v11.16-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.16-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.15 (271):** Yeni Görünüm (koyu modern tema) — Eski Görünüm ayarlarda (1.914 test)
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme (1.906 test)
- **v11.13 (269):** Hata taraması + Görev Takvimi, Trend, Başarı, CSV, Bulut/Sağlık/Takvim çekirdekleri (1.870 test)

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
