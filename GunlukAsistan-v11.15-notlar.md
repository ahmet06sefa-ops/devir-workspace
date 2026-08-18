# 🌙 Günlük Asistan v11.15 — Sürüm Notları (Yeni Görünüm)

**Sürüm:** v11.15 · **versionCode:** 271 · **versionName:** "11.15"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.914 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 144 suite)

---

## 🎯 Bu sürümün teması: Yeni Görünüm (koyu, modern)

Kullanıcı isteği: **"Uygulama ekranını fotoğraftaki gibi yap (koyu). Şu anki temayı vb.
şeyleri 'Eski Görünüm' diye ayarlara al. Yapacaklarımızı da 'Yeni Görünüm' olarak kaydet."**

Bu doğrultuda:
- Yeni bir **koyu "Yeni Görünüm"** teması eklendi ve ayarlardaki tema seçicisine kaydedildi.
- Mevcut tüm temalar (Krem, Violet, Okyanus, … Zincir) **"Eski Görünüm"** olarak
  aynen ayarlarda duruyor — hiçbir kaybolmadı, tek bir geçiş dahi bozulmadı.
- Kullanıcı Ayarlar → Tema seçicisinden "Yeni Görünüm"ü seçince tüm uygulama bu
  koyu görünüme geçer; isterse tek dokunuşla eski temalarına döner.

## 🌑 Yeni Görünüm — `Theme.GunlukAsistan.YeniGorunum`

Koyu, mavi-mor vurgulu modern tema (Material3 Dark tabanlı):

| Renk alanı | Değer |
|---|---|
| Ana vurgu (`colorPrimary`) | `#7C6BFF` (mor-mavi) |
| Vurgu kapsayıcı (`colorPrimaryContainer`) | `#2A2450` |
| İkincil (`colorSecondary`) | `#6FA8FF` (mavi) |
| Zemin (`android:colorBackground`) | `#0F1526` (koyu lacivert) |
| Yüzey (`colorSurface`) | `#151C33` |
| Yüzey varyantı (`colorSurfaceVariant`) | `#1C2540` |
| Metin (`colorOnSurface`) | `#E9EDF7` |
| İkincil metin (`colorOnSurfaceVariant`) | `#97A0BD` |

- Durum/gezinme çubuğu da koyu (`#0F1526`), açık metin — tam bütünlük.
- Yazı: `poppins_regular` (diğer temalarla aynı aile).

## 🔧 Teknik Değişiklikler

1. **`values/themes.xml`** — `Theme.GunlukAsistan.YeniGorunum` stili eklendi
   (Material3.Dark tabanlı, koyu palet).
2. **`ThemeManager.kt`** — `specs` listesine `Spec("Yeni Görünüm", "🌙", …, dark=true)`
   eklendi → tema seçici ızgarasında otomatik görünür.
   - `isNeon` artık "Zincir" başlığını arar (son tema varsayımı yerine), böylece
     yeni tema eklenmesi Zincir'in neon mantığını bozmaz.
3. **`YeniGorunumTest.kt`** — 8 saf koruma testi:
   - "Yeni Görünüm" listede ve koyu,
   - stil/halka/kart rengi tanımlı,
   - tema başlıkları benzersiz,
   - eski temalar (Krem/Violet/Zincir) ve açık/koyu dengesi korunuyor.

## 🧪 Testler

- 1.906 → **1.914 test, 0 hata** (+8 yeni `YeniGorunumTest`).
- Koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) dahil
  **tümü geçiyor** — görünüm değişikliği sert `textSize`/`cardCornerRadius` içermez, `@dimen` uyumlu.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.15.apk`
2. `kaynak-v11.15-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.15-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme (1.906 test)
- **v11.13 (269):** Hata taraması + Görev Takvimi, Trend, Başarı, CSV, Bulut/Sağlık/Takvim çekirdekleri (1.870 test)
- **v11.12 (268):** Evrensel Veri Yedekleme & Geri Yükleme Motoru (1.670 test)

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
