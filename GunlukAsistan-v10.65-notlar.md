# 🏥 GÜNLÜK ASİSTAN — v10.65 (versionCode 221) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **1.106 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`) — Yeni Rekor!**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Yaşam Sağlığı & Finans — Uzman Faz 2: Gelişmiş Medikal, Bütçe, Otonomasyon & Frekans Merkezi (#4..#54 vb.)
Kullanıcının **"Devam et"** talimatı doğrultusunda, 100 maddelik genel öneri katalogumuzda yer alan manuel medikal takipler, 16:8 aralıklı oruç hesaplayıcıları, kişisel borç/alacak ve altın portföyü defterleri, abonelik tasarruf simülatörleri, özel AI prompt kasaları ve binaural odak frekans mikseri **"Yaşam Sağlığı & Finans — Uzman Faz 2"** (`YasamSaglikFinansFaz2.kt`, `YasamSaglikFinansActivity.kt`, `activity_yasam_saglik_finans.xml`) adı altında hayata geçirildi.

Ana ekrandaki **`🏥`** butonuna (veya **Ayarlar > `🏥 Yaşam Sağlığı & Finans — Uzman Faz 2`** satırına) dokunarak açabileceğiniz bu ileri seviye merkez, şu 7 uzman sağlık ve finans alt-sistemini sunar:

### 1. 🏥 Modül 1 (Katalog #4, #6): Tansiyon/Şeker Defteri & 4-7-8 Nefes Rehberi
- **4-7-8 & Kare Nefes Egzersizi (#4):** Odak öncesi veya stres anında 4s al, 7s tut, 8s ver (`4-7-8`) ya da 4s al, 4s tut, 4s ver, 4s tut (`Kare Nefes`) animasyonlu rehberi.
- **Tansiyon & Kan Şekeri Seyir Defteri (#6):** Sistolik/Diastolik tansiyon (`120/80 mmHg`) ve tokluk kan şekeri (`95 mg/dL`) değerlerini WHO sağlık kriterlerine göre değerlendiren ve anında hiper-tansiyon veya hipoglisemi uyarısı veren denetleyici.

### 2. 🥗 Modül 2 (Katalog #8, #10): 16:8 Aralıklı Oruç & Dengeli Öğün Kalori Sayacı
- **Dengeli Öğün Kalori Sayacı (#8):** Kahvaltı, öğle, akşam ve ara öğün kalori yükünü toplayıp günlük hedefle (`2000 kcal`) karşılaştıran ve aşırı yüklemede yürüyüş öneren beslenme sayacı.
- **16:8 Aralıklı Oruç (Intermittent Fasting) Penceresi (#10):** Son öğün saatinize göre (`18:00, 20:00, 22:00`) 16 saatlik açlık penceresinin bitiş saati ve yağ yakım fazını canlı hesaplayan sistem.

### 3. 💰 Modül 3 (Katalog #13, #14): Harcama Limit Radarı & Borç/Alacak Defteri
- **Manuel Günlük Harcama Limiti Radarı (#13):** Günlük harcama limitine (`500 ₺`) göre harcanan tutarı inceleyip %80 aşımında turuncu, %100 aşımında kırmızı alarm veren radar.
- **Kişisel Borç & Alacak Hatırlatma Defteri (#14):** Alacaklı olunan veya borçlanılan tutarları toplayıp net mali konumu (`Net Durum: +700 ₺ Alacaklısınız`) çıkaran defter.

### 4. 💎 Modül 4 (Katalog #15, #16): Kumbara Hedef Metresi & Altın/Döviz Portföyü
- **Kumbara Hedef Metresi (#15):** Belirlenen tasarruf hedefi (`35.000 ₺`) için biriken tutarı ve kalan bakiyeyi % ilerleme çubuğuyla takip ettiren metre.
- **Döviz & Altın Portföy Değerleyici (#16):** Gram altın, USD ve EUR varlıklarının güncel kurlarla Türk Lirası toplam değerini (`14.850 ₺`) hesaplayan varlık defteri.

### 5. ✂️ Modül 5 (Katalog #18): Abonelik Kapatma & Tasarruf Simülatörü
- **Abonelik Tasarruf Simülatörü (#18):** Kullanılmayan bulut depolama, spor salonu veya müzik üyelikleri kapatıldığında yılda toplam kaç ₺ tasarruf edileceğini (`Yılda 11.640 ₺ Tasarruf!`) anında gösteren simülasyon aracı.

### 6. 🤖 Modül 6 (Katalog #29, #30): Özel AI Prompt Kasası & TTS Konuşma Hızı/Pitch Ayarı
- **Özel AI Prompt Kasası (#29):** Kullanıcının AI koça "Her sabah Stoacı felsefi söz söyle ve karmaşık terimleri sade dille anlat" gibi özel kural tanımlamasını sağlayan kasa.
- **TTS Konuşma Hızı & Pitch Ayarı (#30):** Yapay zekanın sesli brifing okuma hızını (`1.00x` - `1.25x`) ve ses tonu perdesini Türkçe doğal vurguyla yöneten ses motoru.

### 7. 🎧 Modül 7 (Katalog #39, #47, #54): Pofi Başarı Rozetleri & Binaural Odak Mikseri
- **Binaural Odak Frekans Mikseri (#47):** Ders çalışırken `40 Hz Gamma` (analitik problem çözme), `14 Hz Beta` (aktif odak), `10 Hz Alpha` (hafıza) ve `4 Hz Delta` (uyku) ses dalgalarını seçtiren mikser.
- **Pofi Maskot Başarı Rozetleri (#39):** "Bronz Çırak", "Gümüş Usta", "Altın Efsane" ve "Gece Kuşu" gibi özel başarı rozetlerini görüntüleyen vitrin.
- **%100 Çevrimdışı Kasa Doğrulaması (#54):** Hiçbir bulut bağımlılığı olmadan tüm medikal, bütçe ve AI talimatlarının yerel AES-şifreli JSON olarak saklandığını garantileyen kalkan.

---

## 🛠 Teknik ve Mimari Gelişmeler
1. **100% Tasarım Ölçeği ve Ripple Uyumu:** XML layoutları harfiyen `@dimen/ga_kose_orta` (16dp), `@dimen/ga_yazi_normal` (15sp) ve `android:foreground="?attr/selectableItemBackground"` kuralları ile tasarlandı.
2. **Ana Ekran Sadeleştirme Desteği (`v10.59`):** Yeni eklenen `openYasamSaglikFinans` (`🏥`) butonu, `AnaEkranButonKarari.ATOLYE_BUTON_IDLERI` listesine entegre edildi. Kullanıcı Ayarlar'dan ana ekran butonlarını kapattığı anda yeni buton da gizlenir.
3. **Rekor Birim Test Başarısı:** `YasamSaglikFinansFaz2Test` bünyesindeki **26 yeni JVM JUnit testi** ile toplam test sayısı **1.106**'ya yükseltildi, **0 hata, 0 başarısızlık** oranı korundu.

---

## 📦 Teslim Paketi (4 Parça Kuralı)
1. **`GunlukAsistan-v10.65.apk`**: Kurulabilir bağımsız APK dosyası.
2. **`kaynak-v10.65-yedek.zip`**: Derleme ve ara önbellek dosyalarından (`build/`, `.gradle/`) arındırılmış tam kaynak kodu arşivi.
3. **`PROJE-DURUM.md`**: Güncellenmiş genel proje durum tablosu ve kilometre taşları.
4. **`GunlukAsistan-v10.65-notlar.md`**: Bu detaylı sürüm notları belgesi.
