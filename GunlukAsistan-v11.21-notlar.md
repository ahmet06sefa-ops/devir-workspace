# 🌐 Günlük Asistan v11.21 — Sürüm Notları (HabitGenius Ayrı Uygulama Köprüsü)

**Sürüm:** v11.21 · **versionCode:** 277 · **versionName:** "11.21"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.925 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 147 suite)

---

## 🎯 Bu sürümün teması: HabitGenius Ayrı Uygulamasını 2. Görünüme Bağlama

Kullanıcı, "2. Görünümün bütün her şeyi"ni içeren **tam HabitGenius uygulamasını** bir
Google Drive bağlantısıyla iletti. Bu, `com.habitgenius.habit.tracker` paket adına sahip,
React Native ile geliştirilmiş, **ayrı ve bağımsız bir uygulamadır** (v3.3.1, minSdk 26,
targetSdk 36, RN bundle ~16MB).

Bu uygulama, Günlük Asistan'ın native Kotlin çekirdeğinden **farklı bir paket ve çalışma
zamanına** sahip olduğu için doğrudan "içine gömmek" teknik olarak mümkün değildir. Bunun
yerine kullanıcının isteği olan **"2. Görünüm ayrı bir uygulama gibi açılsın"** akışını
gerçekten çalışır hale getiren bir **köprü** kuruldu:

## 🔗 HabitGenius Köprüsü (`HabitGeniusKopru.kt`)

- **"2. Görünüm (Habit Genius)"** seçildiğinde HabitGenius uygulaması **ayrı bir uygulama
  gibi** başlatılır (kuruluysa).
- Kurulu değilse kullanıcıya **"HabitGenius uygulaması kurulu değil"** mesajı gösterilir.
- **`MainActivity`** — "2. Görünüm" modu aktifken uygulama açılışında HabitGenius'u başlatır.
- **`ThemeFragment`** — 2. Görünüm kartına dokununca köprüyü çağırır.
- **"1. Görünüme Geç"** — Ayarlar → Görünüm'de 1. Görünüm seçilince klasik görünüme döner.

## 📦 İki Kurulabilir APK (aynı debug anahtarı `5f15d4e7…`)

1. **`GunlukAsistan-v11.21.apk`** — ana uygulama (klasik görünüm + köprü).
2. **`HabitGenius-v3.3.1.apk`** — kullanıcının sağladığı tam HabitGenius uygulaması;
   Günlük Asistan'ın debug anahtarıyla **yeniden imzalandı** → ikisi aynı imza ailesinde,
   birlikte sorunsuz çalışır.

### Nasıl kullanılır
1. `HabitGenius-v3.3.1.apk` ve `GunlukAsistan-v11.21.apk` dosyalarını kurun.
2. Günlük Asistan → Ayarlar → Görünüm → **"2. Görünüm (Habit Genius)"** seçin.
3. HabitGenius ayrı uygulaması açılır (tüm ekranlarıyla).
4. Günlük Asistan'a dönmek için ana Günlük Asistan uygulamasını açın ve 1. Görünüme geçin.

## 🧪 Testler

- 1.921 → **1.925 test, 0 hata** (+4 `HabitGeniusKopruTest`: paket adı, activity, sürüm, sınıf).
- Tüm koruma testleri (TasarimOlcegi, RippleTutarlilik, GorunumAtolye, AnaEkranButon) geçiyor.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.21.apk`
2. `kaynak-v11.21-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.21-notlar.md`
5. **`HabitGenius-v3.3.1.apk`** (2. Görünümün ayrı uygulaması)

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.20 (276):** HabitGenius ayrı uygulama + 1. Görünüme Geç ayarı
- **v11.19 (275):** HabitGenius Tam Compose Sürümü (2. Görünüm)
- **v11.14 (270):** Verimlilik Paketi — Pomodoro + Eisenhower önceliklendirme

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
