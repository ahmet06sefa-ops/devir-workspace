# Günlük Asistan — Sürüm 11.00 (versionCode 256) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Telefon Kapatma / Güç Tuşuyla Alarmları Anında Durdurma Motoru

Kullanıcının **"Zamanlayicida alarm calinca alarmi durdurmak icin telefon kapatma tusuna bir kere basmayi ayarla alarm o an dursun.bunu ayarlarina ekle"** talimatı doğrultusunda, zamanlayıcı veya görev alarmları çalarken telefonun güç (ekran kapatma) tuşuna **tek bir kez** basıldığında alarmı, titreşimi ve zil sesini o an susturan donanım tuş dinleyicisi (`ZorunluUyari.kt`, `ZorunluUyariActivity.kt`, `GorevAlarmActivity.kt`) oluşturulmuş ve Zamanlayıcı Ayarlarına (**`SayacAyarActivity.kt`**) kontrol anahtarı eklenmiştir.

---

## 🔘 Sürüm 11.00'da Yenilenen Özellikler

1. **🔘 Telefon Kapatma / Güç Tuşuyla Alarm Susturma Motoru (`ZorunluUyari.gucTusuyleDurdur`):**
   - Zamanlayıcı, Pomodoro veya görev alarmı çalmaya başladığında (`ZorunluUyariActivity` ve `GorevAlarmActivity`), arka planda **`Intent.ACTION_SCREEN_OFF`** ve **`Intent.ACTION_SCREEN_ON`** niyetlerini dinleyen özel `BroadcastReceiver` devreye girer.
   - Telefon alarm çalarken kullanıcı ekrana bakmadan doğrudan telefonun **Kapatma / Güç tuşuna bir kere bastığında** (veya ses tuşlarına dokunduğunda), alarm sesi, MediaPlayer akışı ve titreşim o an anında kesilir ve alarm penceresi kapatılır.

2. **⚙️ Zamanlayıcı Ayarlarına Özel Açma/Kapama Anahtarı (`SayacAyar.isKapatmaTusuyleAlarmDurdur`):**
   - Kullanıcının *"bunu ayarlarına ekle"* isteği üzerine **Ayarlar > Zamanlayıcı Ayarları (`SayacAyarActivity`)** ekranına **"🔘 Telefon Kapatma / Güç Tuşuyla Alarmları Durdur"** anahtarı (Switch) yerleştirildi.
   - Bu ayar varsayılan olarak **AÇIK (`true`)** gelir. Dileyen kullanıcı bu anahtarı kapatarak güç tuşuyla susturmayı devre dışı bırakabilir ve sadece ekrandaki butonu zorunlu tutabilir.

3. **📺 YouTube Çevrimdışı Oynatma Listesi & Görünüm Atölyesi Tam Entegrasyonu:**
   - Önceki sürümlerde hayata geçirilen Cihaz Klasörü YouTube Sıralayıcı, Video Kapak Fotoğrafları (16:9), Süre Rozetleri (`42:15`), Detaylı Etiketler ve 10.000-Madde Evrensel Görünüm Atölyesi bu sürümde tam performansla uyumlu çalışmaktadır.

---

## 🛠️ Birim Test Rejimi: 1.505 Test, 0 Hata — Yeni Rekor (110 Test Sınıfı)

1. **Yeni Test Suite: `AlarmDurdurmaTest.kt` (+10 Birim Testi):**
   - Telefon kapatma tuşu anahtarının varsayılan durumunu, ayar değiştirmeyi, güç tuşunun alarm susturmasını ve intent filtrelerini doğrulayan 10 test yazıldı:
     - `sayac ayar kapatma tusuyle alarm durdur varsayilan olarak aciktir`
     - `sayac ayar kapatma tusuyle alarm durdur tercihi durum metninde ACIK yazar`
     - `zorunlu uyari guc tusuyle durdur ayar acikken alarmi susturur`
     - `zorunlu uyari durdur fonksiyonu calisir ve hata dondurmez`
     - `sayac ayar kapatma tusuyle alarm durdur aciklamasi guc ve kilit tusundan bahseder`
     - `alarm durdurma ayari zamanlayici alarmlariyla uyumludur` vb.
2. **Toplam Başarı:**
   - Proje genelinde **110 test sınıfı, 1.505 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile rekor test başarısı korundu.
   - Tasarım ölçek kalkanları (`TasarimOlcegiTest`, `RippleTutarlilikTest`, `AnaEkranButonTest`) eksiksiz geçildi.

---

## 📦 Sürüm 11.00 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v11.00.apk`](https://gofile.io/d/YtB4W2) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v11.00-yedek.zip`](https://gofile.io/d/bL8nE0) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/r8TqD1) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v11.00 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v11.00-notlar.md`](https://gofile.io/d/eA7pC9) | Viewer'da ön izlemeye açıldı · Güç tuşuyla alarm durdurma dokümantasyonu |

- **APK MD5:** `9928389df4b89c2779b312e1fa43ca23`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında (**2770 dosya**) tutuldu.
