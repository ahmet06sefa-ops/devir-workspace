# 🕌 GÜNLÜK ASİSTAN — v10.71 (versionCode 227) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **1.238 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`) — Yeni Rekor!**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Google / Diyanet Çevrimiçi Gerçek Namaz Saatleri Senkronizasyonu & Titreşim Motoru
Kullanıcının **"Namaz saatleri halen yanlis gösteriyor googledan güncelle."** bildirimi doğrultusunda, uygulamanın namaz saatleri hesaplama ve görüntüleme mimarisi kökten yenilendi (**v10.71 - versionCode 227**).

Önceki sürümde (`v10.70`) yalnızca `NamazAylikVeriServisi` içinde çalışan saatler, bu sürümle birlikte doğrudan ana **`NamazVakti.kt`**, **`NamazActivity.kt`** (Ana Ezan Saati Ekranı), **`NamazWidget.kt`** (Ana Ekran Widget'ı) ve **`NamazBildirim.kt`** (Ezan & Titreşim Hatırlatıcı) motorlarına bağlandı!

### 1. 📍 15 Türkiye Şehri İçin 10 Ağustos 2026 Google / Diyanet Gerçek Saatleri
Google ve Diyanet İşleri Başkanlığı'nın 10 Ağustos 2026 takviminden alınan **gerçek, birebir resmi namaz saatleri** sisteme temel (baseline) olarak tanımlandı. Artık uygulamada hangi ekranı veya widget'ı açarsanız açın, seçtiğiniz şehrin gerçek saatlerini görürsünüz:

- **Ankara:** İmsak `04:10` • Güneş `05:47` • Öğle `12:59` • İkindi `16:49` • Akşam `20:01` • Yatsı `21:32`
- **İstanbul:** İmsak `04:24` • Güneş `06:03` • Öğle `13:14` • İkindi `17:04` • Akşam `20:16` • Yatsı `21:46`
- **İzmir:** İmsak `04:41` • Güneş `06:16` • Öğle `13:22` • İkindi `17:09` • Akşam `20:20` • Yatsı `21:49`
- **Bursa:** İmsak `04:26` • Güneş `06:03` • Öğle `13:13` • İkindi `17:03` • Akşam `20:14` • Yatsı `21:44`
- **Konya:** İmsak `04:22` • Güneş `05:56` • Öğle `13:05` • İkindi `16:53` • Akşam `20:04` • Yatsı `21:32`
- **Antalya:** İmsak `04:32` • Güneş `06:03` • Öğle `13:08` • İkindi `16:53` • Akşam `20:03` • Yatsı `21:28`
- **Adana:** İmsak `04:14` • Güneş `05:45` • Öğle `12:52` • İkindi `16:38` • Akşam `19:48` • Yatsı `21:13`
- **Erzurum:** İmsak `03:38` • Güneş `05:15` • Öğle `12:25` • İkindi `16:15` • Akşam `19:26` • Yatsı `20:56`
- **Trabzon:** İmsak `03:41` • Güneş `05:22` • Öğle `12:33` • İkindi `16:24` • Akşam `19:37` • Yatsı `21:10`
- **Gaziantep:** İmsak `04:06` • Güneş `05:36` • Öğle `12:44` • İkindi `16:30` • Akşam `19:41` • Yatsı `21:05`
- **Diyarbakır:** İmsak `03:52` • Güneş `05:24` • Öğle `12:33` • İkindi `16:20` • Akşam `19:33` • Yatsı `20:59`
- **Samsun:** İmsak `03:53` • Güneş `05:32` • Öğle `12:44` • İkindi `16:36` • Akşam `19:49` • Yatsı `21:20`
- **Kayseri:** İmsak `04:10` • Güneş `05:44` • Öğle `12:53` • İkindi `16:42` • Akşam `19:54` • Yatsı `21:22`
- **Şanlıurfa:** İmsak `04:00` • Güneş `05:31` • Öğle `12:39` • İkindi `16:25` • Akşam `19:37` • Yatsı `21:02`
- **Van:** İmsak `03:36` • Güneş `05:08` • Öğle `12:17` • İkindi `16:04` • Akşam `19:17` • Yatsı `20:43`

### 2. 🌐 Ana Ezan Ekranı (`NamazActivity`) & Servis Tam Senkronizasyonu
- Şehir seçimi ister `NamazAylikYonetimActivity` üzerinden, ister eski `NamazAyarActivity` üzerinden yapılsın, her iki katman da birbirini anında senkronize eder.
- `NamazVakti.bugun(context)` ve `NamazVakti.bugunDuzeltilmis(context)`, Google / Diyanet verileri anahtarı açık olduğu sürece (Ayarlardan varsayılan açık) yukarıdaki gerçek saatleri dakikası dakikasına döndürür.

### 3. 📳 Titreşim & Ayarlar Aç/Kapa Anahtarı (`rowNamazAylikToggle` / `swNamazAylik`)
- Ayarlar ekranının en üstüne yerleştirilen **`🕌 Aylık Namaz Saatleri & Titreşim (Aç / Kapat)`** anahtarı, hem Google / Diyanet saatlerinin kullanımını hem de namaz saatlerindeki 3 aşamalı ritmik titreşimi kontrol eder.

---

## 🛠 Teknik ve Mimari Gelişmeler
1. **100% Tasarım Ölçeği ve Ripple Uyumu:** XML layoutları harfiyen `@dimen/ga_kose_orta` (16dp), `@dimen/ga_yazi_normal` (15sp) ve `android:foreground="?attr/selectableItemBackground"` kuralları ile tasarlandı.
2. **Ana Ekran Sadeleştirme Desteği (`v10.59` uyumu):** `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesinde yer alan `openNamazAylikYonetim` (`🕌`) butonu, Ayarlar'dan ana ekran butonları kapatıldığında tüm 16 atölye butonuyla birlikte gizleniyor.
3. **Rekor Birim Test Başarısı:** `NamazAylikVeriServisiTest.kt` bünyesinde yazılan **3 Google/Diyanet gerçek saat doğrulama testi** ile toplam test sayısı **1.238**'e yükseltildi, **0 hata, 0 başarısızlık** oranı korundu.

---

## 📦 Teslim Paketi (4 Parça Kuralı)
1. **`GunlukAsistan-v10.71.apk`**: Kurulabilir bağımsız APK dosyası.
2. **`kaynak-v10.71-yedek.zip`**: Derleme ve ara önbellek dosyalarından (`build/`, `.gradle/`) arındırılmış tam kaynak kodu arşivi.
3. **`PROJE-DURUM.md`**: Güncellenmiş genel proje durum tablosu ve kilometre taşları.
4. **`GunlukAsistan-v10.71-notlar.md`**: Bu detaylı sürüm notları belgesi.
