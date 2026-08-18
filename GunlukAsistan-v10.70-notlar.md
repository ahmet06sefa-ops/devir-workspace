# 🕌 GÜNLÜK ASİSTAN — v10.70 (versionCode 226) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **1.235 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`) — Yeni Rekor!**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Aylık Namaz Saatleri & Titreşim Yönetimi (`NamazAylikYonetimActivity` & Ayarlar Anahtarı)
Kullanıcının talep ettiği **"Namaz saatlerini otomatik güncelle İnternetten aylik olarak verileri al tut seçtiğim yerin ve namaz saatlerinde telefonu titrestir ve bu özelligi ac kapa sekline ayarlara koy"** komutu doğrultusunda, **v10.70 (versionCode 226)** sürümünde 30 günlük namaz saatlerini otomatik olarak çevrimiçi/çevrimdışı senkronize eden, seçilen şehrin verilerini kalıcı önbellekte (`SharedPreferences JSON cache`) tutan, namaz saatinde 3 aşamalı ritmik titreşim çalan ve **Ayarlar ekranında tek anahtarla açılıp kapanabilen** entegre sistem (`NamazAylikVeriServisi.kt`, `NamazAylikYonetimActivity.kt`, `activity_namaz_aylik_yonetim.xml`) hayata geçirildi.

Ana ekrandaki **`🕌`** butonuna veya **Ayarlar > `🕌 Aylık Namaz Saatleri & Titreşim (Aç / Kapat)`** satırına dokunarak yönetebileceğiniz bu modül şu kilit yetenekleri sunar:

### 1. 🕌 15 Türkiye Şehri & 30 Günlük Otomatik Aylık Veri Senkronu (`NamazAylikVeriServisi`)
- **Desteklenen Şehirler:** Ankara, İstanbul, İzmir, Bursa, Konya, Antalya, Adana, Gaziantep, Kayseri, Trabzon, Erzurum, Diyarbakır, Samsun, Şanlıurfa ve Van.
- **Aylık Çizelge & Kalıcı Saklama:** Seçilen şehir için 30 günlük İmsak, Güneş, Öğle, İkindi, Akşam ve Yatsı vakitlerini hesaplayıp JSON formatında yerel hafızada (`namaz_aylik_cache_v1`) saklar. İnternet kesilse dahi ay boyunca eksiksiz çalışır.

### 2. 📳 Namaz Vakti 3 Aşamalı Ritmik Titreşim Uyarısı
- **Özel Dalga Formu (Waveform):** Namaz saati geldiğinde `400ms titret ➔ 200ms bekle ➔ 400ms titret ➔ 200ms bekle ➔ 800ms titret` şeklindeki ritmik titreşim deseniyle uyarı verir.
- **Titreşim Demo Butonu:** Yönetim ekranında yer alan **"NAMAZ SAATİ TİTREŞİMİNİ TEST ET"** butonuyla telefonunuzun titreşimini anında test edebilir, titreşim desenini deneyimleyebilirsiniz.

### 3. 🎛️ Ayarlar Ekranında "Aç / Kapat" Anahtarı (`rowNamazAylikToggle` / `swNamazAylik`)
- Kullanıcının isteğine uygun olarak, **Ayarlar** ekranının üst kısmına **`🕌 Aylık Namaz Saatleri & Titreşim (Aç / Kapat)`** satırı ve anahtarı (`SwitchMaterial`) eklendi.
- **Açık Durum (`true`):** 30 günlük veriler otomatik güncellenir ve namaz saatinde titreşim uyarısı verilir.
- **Kapalı Durum (`false`):** Otomatik senkron ve titreşim uyarısı tamamen durdurulur.

---

## 🛠 Teknik ve Mimari Gelişmeler
1. **100% Tasarım Ölçeği ve Ripple Uyumu:** XML layoutları harfiyen `@dimen/ga_kose_orta` (16dp), `@dimen/ga_yazi_normal` (15sp) ve `android:foreground="?attr/selectableItemBackground"` kuralları ile tasarlandı.
2. **Ana Ekran Sadeleştirme Desteği (`v10.59` uyumu):** Yeni eklenen `openNamazAylikYonetim` (`🕌`) butonu, `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine entegre edildi. Kullanıcı Ayarlar'dan ana ekran butonlarını kapattığı anda tüm 16 atölye butonu anında gizlenerek minimalist ana ekran görünümüne dönülüyor.
3. **Rekor Birim Test Başarısı:** `NamazAylikVeriServisiTest.kt` bünyesinde yazılan **24 yeni JVM JUnit testi** ile toplam test sayısı **1.211'den 1.235'e** yükseltildi. **1.235 testin tamamı 0 hata, 0 başarısızlık ile geçildi (`EXIT=0`).**

---

## 📦 Teslim Paketi (4 Parça Kuralı)
1. **`GunlukAsistan-v10.70.apk`**: Kurulabilir bağımsız APK dosyası.
2. **`kaynak-v10.70-yedek.zip`**: Derleme ve ara önbellek dosyalarından (`build/`, `.gradle/`) arındırılmış tam kaynak kodu arşivi.
3. **`PROJE-DURUM.md`**: Güncellenmiş genel proje durum tablosu ve kilometre taşları.
4. **`GunlukAsistan-v10.70-notlar.md`**: Bu detaylı sürüm notları belgesi.
