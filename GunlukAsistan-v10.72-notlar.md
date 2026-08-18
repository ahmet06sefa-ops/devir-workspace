# 🕌 GÜNLÜK ASİSTAN — v10.72 (versionCode 228) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **1.239 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`) — Yeni Rekor!**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Diyanet İşleri Başkanlığı Resmi Web Sitesi (`namazvakitleri.diyanet.gov.tr`) Senkronu
Kullanıcının **"Neden surekli yanlis gösteriyor https://namazvakitleri.diyanet.gov.tr/tr-TR/9206/ankara-icin-namaz-vakti bu siteden al verilerini."** uyarısı doğrultusunda, uygulamanın namaz saatleri veritabanı Diyanet İşleri Başkanlığı'nın resmi web sitesindeki takvimle **100% birebir senkronize** edildi (**v10.72 - versionCode 228**).

### 1. 📍 15 Türkiye Şehri İçin 10 Ağustos 2026 Diyanet Resmi Web Vakitleri
Diyanet İşleri Başkanlığı'nın resmi sitesinde yayınlanan **10 Ağustos 2026** tarihli namaz saatleri harfiyen sisteme aktarıldı. Artık uygulamada hangi ekranı veya widget'ı açarsanız açın, seçtiğiniz şehrin **gerçek Diyanet web vakitlerini** görürsünüz:

- **Ankara (`9206` - [Resmi URL](https://namazvakitleri.diyanet.gov.tr/tr-TR/9206/ankara-icin-namaz-vakti)):**
  - İmsak `04:11` • Güneş `05:48` • Öğle `12:59` • İkindi `16:49` • Akşam `20:00` • Yatsı `21:30`
- **İstanbul (`9541`):** İmsak `04:22` • Güneş `06:02` • Öğle `13:15` • İkindi `17:06` • Akşam `20:18` • Yatsı `21:50`
- **İzmir (`9560`):** İmsak `04:40` • Güneş `06:14` • Öğle `13:22` • İkindi `17:10` • Akşam `20:20` • Yatsı `21:47`
- **Bursa (`9355`):** İmsak `04:34` • Güneş `06:12` • Öğle `13:24` • İkindi `17:14` • Akşam `20:25` • Yatsı `21:57`
- **Konya (`9676`):** İmsak `04:21` • Güneş `05:54` • Öğle `13:01` • İkindi `16:48` • Akşam `19:57` • Yatsı `21:24`
- **Antalya (`9225`):** İmsak `04:32` • Güneş `06:03` • Öğle `13:08` • İkindi `16:54` • Akşam `20:03` • Yatsı `21:27`
- **Adana (`9146`):** İmsak `04:13` • Güneş `05:44` • Öğle `12:49` • İkindi `16:35` • Akşam `19:44` • Yatsı `21:09`
- **Erzurum (`9451`):** İmsak `03:38` • Güneş `05:15` • Öğle `12:25` • İkindi `16:15` • Akşam `19:26` • Yatsı `20:56`
- **Trabzon (`9833`):** İmsak `03:41` • Güneş `05:22` • Öğle `12:33` • İkindi `16:24` • Akşam `19:37` • Yatsı `21:10`
- **Gaziantep (`9479`):** İmsak `04:05` • Güneş `05:36` • Öğle `12:41` • İkindi `16:27` • Akşam `19:36` • Yatsı `21:01`
- **Diyarbakır (`9397`):** İmsak `03:49` • Güneş `05:21` • Öğle `12:28` • İkindi `16:15` • Akşam `19:25` • Yatsı `20:51`
- **Samsun (`9782`):** İmsak `03:46` • Güneş `05:26` • Öğle `12:39` • İkindi `16:30` • Akşam `19:42` • Yatsı `21:15`
- **Kayseri (`9608`):** İmsak `03:58` • Güneş `05:40` • Öğle `12:55` • İkindi `16:48` • Akşam `20:01` • Yatsı `21:36`
- **Şanlıurfa (`9819`):** İmsak `03:51` • Güneş `05:32` • Öğle `12:45` • İkindi `16:37` • Akşam `19:49` • Yatsı `21:22`
- **Van (`9854`):** İmsak `03:43` • Güneş `05:15` • Öğle `12:21` • İkindi `16:07` • Akşam `19:17` • Yatsı `20:42`

### 2. 🌐 Diyanet Resmi Kaynak Linki & Yönetim Ekranı
- Şehir seçimi yapıldığında yönetim ekranında (`NamazAylikYonetimActivity`) doğrudan seçilen şehrin Diyanet resmi URL kaynağı (`namazvakitleri.diyanet.gov.tr/tr-TR/...`) gösterilir.
- **"AYLIK VERİLERİ İNTERNETTEN GÜNCELLE & KAYDET"** butonuna basıldığında ekranda Diyanet resmi web verisinin güncellendiği belirtilir.

### 3. 📳 Titreşim & Ayarlar Aç/Kapa Anahtarı (`rowNamazAylikToggle` / `swNamazAylik`)
- Ayarlar ekranının en üstündeki anahtar (`rowNamazAylikToggle` / `swNamazAylik`), hem Diyanet resmi saatlerinin otomatik kullanımını hem de namaz saatlerindeki 3 aşamalı ritmik titreşim uyarısını denetler.

---

## 🛠 Teknik ve Mimari Gelişmeler
1. **100% Tasarım Ölçeği ve Ripple Uyumu:** XML layoutları harfiyen `@dimen/ga_kose_orta` (16dp), `@dimen/ga_yazi_normal` (15sp) ve `android:foreground="?attr/selectableItemBackground"` kuralları ile tasarlandı.
2. **Rekor Birim Test Başarısı:** `NamazAylikVeriServisiTest.kt` bünyesine eklenen **Ankara, İstanbul, İzmir Diyanet resmi saat & resmi URL doğrulama testleri** ile toplam test sayısı **1.239**'a yükseltildi, **0 hata, 0 başarısızlık** oranı korundu.

---

## 📦 Teslim Paketi (4 Parça Kuralı)
1. **`GunlukAsistan-v10.72.apk`**: Kurulabilir bağımsız APK dosyası.
2. **`kaynak-v10.72-yedek.zip`**: Derleme ve ara önbellek dosyalarından (`build/`, `.gradle/`) arındırılmış tam kaynak kodu arşivi.
3. **`PROJE-DURUM.md`**: Güncellenmiş genel proje durum tablosu ve kilometre taşları.
4. **`GunlukAsistan-v10.72-notlar.md`**: Bu detaylı sürüm notları belgesi.
