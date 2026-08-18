# 🚀 Günlük Asistan v11.14 — Sürüm Notları (Verimlilik Paketi)

**Sürüm:** v11.14 · **versionCode:** 270 · **versionName:** "11.14"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.906 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı, 143 suite)

---

## 🎯 Bu sürümün teması: Verimlilik Paketi

Kullanıcı isteği: **"v11.14 ve ilerisine geliştir."**

v11.13'ün zengin AI yetki + koçluk altyapısının üzerine, odak süresi ve görev
önceliklendirmesini birer sağlam motora çeviren **iki yeni saf modül** ve bunların
AI komut entegrasyonu eklendi. Yalnızca saf, test edilebilir, bağımlılıksız
mantık; UI üzerine hiçbir sert kod eklenmedi (koruma testleri uyumlu).

---

## 🍅 1) Pomodoro / Verimlilik Motoru — `PomodoroMotoru.kt`

Kullanıcının odak süresini pomodoro tekniğine dönüştüren saf hesaplama katmanı:

- **`sureDonustur`** — dakikayı okunur "1:15" / "25 dk" biçemine çevirir.
- **`verimlilikSkoru`** — odak dakikası (ağırlıklı) + tamamlanan görevden **0..100** skor üretir.
- **`yildiz` / `yorum`** — skoru 0..5 yıldıza ve motive edici yoruma çevirir.
- **`blokSayisi`** — kalan süreyi 25 dk'lık pomodoro bloklarına böler.
- **`molaOnerisi`** — ardışık blok sayısına göre kısa (5 dk) / uzun (20 dk, 4 blokta) mola önerir.
- **`gunIcinOdakPlani`** — günün kalan odak planını adım adım satırlara döker.

## 📋 2) İçerik Önceliklendirme Motoru — `IcerikOnceliklendirmeMotoru.kt`

Görevleri klasik **Eisenhower Matrisi** ile dört kadrana ayırır ve sıralar:

- **`kadran`** — önem (0..10) × aciliyet (0..10) → 4 kadran sınıfı
  (Önemli+Acil → "Hemen Yap", Önemli+Acil Değil → "Planla",
   Önemsiz+Acil → "Devret", Önemsiz+Acil Değil → "Ertela/El").
- **`oncelikPuani`** — önem %60 + aciliyet %40 ağırlıklı **0..100** puan.
- **`sirala` / `matrisSiralama`** — önce kadran önceliği, sonra puan ile sıralar.
- **`okunur`** — okunur numaralı öncelik listesi üretir.

## 🤖 AI Entegrasyonu — 2 Yeni Komut

`AsistanKomut` dispatch tablosuna + `AiClient` prompt kataloğuna eklendi:

- **`pomodoro_durum`** — bugünün odak dakikası, tamamlanan görev, blok sayısı,
  mola önerisi ve verimlilik skoru/yıldızını tek mesajda özetler.
- **`onceliklendir`** — görevleri Eisenhower matrisine göre sıralar; ek olarak
  belirli bir görevi arayıp tek tavsiye biçeminde gösterebilir.

## 🧪 Testler

- **`PomodoroMotorTest.kt`** — 21 saf JVM testi
- **`IcerikOnceliklendirmeMotorTest.kt`** — 18 saf JVM testi
- Toplam **+36 yeni test** → 1.870 → **1.906 test, 0 hata.**

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.14.apk`
2. `kaynak-v11.14-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.14-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

## 📈 Önceki sürümden özet
- **v11.13 (269):** Hata taraması + Görev Takvimi, Trend, Başarı, Veri Boyut, CSV, Sosyal, Bildirim Filtresi, Çok Dillilik, Bulut/Sağlık/Takvim çekirdekleri (1.870 test)
- **v11.12 (268):** Evrensel Veri Yedekleme & Geri Yükleme Motoru (1.670 test)
- **v11.11 (267):** Canva Çalışma Ekranı — 10 Uygulama Arayüzü, Aç-Kapa, Akıllı Öneri (1.654 test)

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
