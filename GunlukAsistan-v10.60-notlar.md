# 🚀 GÜNLÜK ASİSTAN — v10.60 (versionCode 216) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **970 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`)**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Ders Çalışma İleri Fazı: Bilişsel Leitner Kutusu & PDF Flaş Kart Üretim Merkezi (#1, #41, #11 vb.)
Kullanıcının **"Devam edebilirsin burdan"** talimatı doğrultusunda, bir önceki sürümdeki 100 maddelik ders çalışma katalogundan en kritik görülen **Kategori 1 (Bilişsel Öğrenme / Leitner Kutusu İleri Fazı)** ve **Kategori 5 (PDF Sayfa Üzeri Flaş Kart Üretimi)** ile birlikte **KPSS Deneme Net Eğrisi**, **Active Recall** ve **AI Sokratik Çeldirici** modüllerinin özel alt sayfaları **"Ders Çalışma İleri Fazı"** (`DersIleriFaz.kt`, `DersIleriFazActivity.kt`, `activity_ders_ileri_faz.xml`) adı altında hayata geçirildi.

Ana ekrandaki **`🃏`** butonuna (veya **Ayarlar > `🚀 Ders Çalışma İleri Fazı (Leitner & PDF)`** satırına) dokunarak açabileceğiniz bu ileri seviye merkez, şu 7 uzman öğrenme alt-modülünü sunar:

### 1. 🃏 Modül 1 (İleri #1, #2, #6): İleri Leitner Kutu & SR-2-7-30 Flaş Kart Deste
- **İnteraktif Flaş Kart Deste (Flashcard Deck):** Önemli sınav sorularını önlü-arkalı kartlar halinde çalıştıran deste aracı.
- **Leitner Kutu İlerleme & Düşürme Mantığı:** Kartı **`✅ DOĞRU (ÜST KUTU)`** bildiğinizde kart Kutu 1 ➔ Kutu 2 ➔ Kutu 3'e yükselir; **`❌ YANLIŞ (KUTU 1)`** bildiğinizde doğrudan Kutu 1'e iner.
- **Canlı Kutu Dağılım Özeti:** Hangi kutuda kaç soru olduğunu ekranda anlık listeler (`Kutu 1: 1 Kart | Kutu 2: 1 Kart | Kutu 3: 1 Kart`).

### 2. ✂️ Modül 2 (İleri #41): PDF Sayfa Üzeri Otomatik Flaş Kart & Vurgu Üreticisi
- **Otomatik Soru-Cevap Dönüştürücü:** PDF ders notlarından aldığınız vurgulu bir cümleyi (`Lozan Boğazlar - Montrö'ye kadar uluslararası komisyon`) tek dokunuşla analiz edip soru-cevap flaş kartına dönüştürür (`Soru: Lozan Boğazlar nedir? | Cevap: Montrö'ye kadar uluslararası komisyon`).
- **Desteye Anında Ekleme:** Üretilen kartı anında Leitner Kutu 1'e ekleyerek tekrarlarınıza dâhil eder (`✂️ VURGUDAN OTOMATİK FLAŞ KART ÜRET`).

### 3. 📈 Modül 3 (İleri #11, #12, #16): KPSS / YKS Deneme Sınavı Net Eğrisi & Hız Radarı
- **Çoklu Sınav Net Eğrisi:** Tarihsel deneme sonuçlarınızı listeleyerek ortalama net ile son denemenizi karşılaştırır ve trendi hesaplar (`📈 YÜKSELİŞTE` veya `📉 TEKRAR GEREKLİ`).
- **Soru Başına Saniye Hız Radarı:** 120 soruluk denemede harcanan süreyi saniyeye çevirip soru başına hızı (`60 sn/soru`) raporlar.
- **Anlık Deneme Ekleme:** **`📈 YENİ DENEME EKLE`** butonuyla desteye yeni deneme ekleyip grafiği dinamik günceller.

### 4. 📝 Modül 4 (İleri #3, #4): Aktif Geri Çağırma (Active Recall) & Feynman Testi
- **Active Recall Değerlendirme Motoru:** Çalışılan bir konuyu kitaba bakmadan en az 3 net cümleyle özetlemeyi talep eder.
- **%0–%100 Skoring:** Girilen özetin kelime ve cümle yoğunluğunu analiz edip %95'e varan bilimsel "Active Recall Skoru" verir.

### 5. ⏱️ Modül 5 (İleri #51, #52): Animedoro (40/20) & Ultradian (90/20) Biyo-Ritm
- **Animedoro 40/20:** 40 dakika yüksek odak / 20 dakika anime veya ödül molasıyla 4 saat sıkılmadan çalışma sağlar.
- **Ultradian Ritm 90/20:** İnsan beyninin doğal odak periyodu olan 90 dakika derin odak / 20 dakika tam zihinsel reset şablonu.

### 6. 🤖 Modül 6 (İleri #19, #61): AI Sokratik Koç & ÖSYM Çeldirici Şık Defteri
- **ÖSYM Çeldirici Uyarıları:** Tarih (`Tanzimat ile Islahat fermanını karıştırmayın`), Türkçe (`Yalnız I şıkkına dikkat`) ve Matematik derslerine özel ÖSYM çeldiricilerini listeler.

### 7. 🎒 Modül 7 (İleri #91, #92): Çevrimdışı Altın Formül Kasası & CSV Dışa Aktarıcı
- **Altın Formüller & CSV Kopyala:** Tarih/Matematik/Türkçe formüllerini internetsiz sunar ve tüm Leitner kartlarını veya Deneme netlerini Excel'e yapıştırılabilir CSV formatında panoya kopyalar (`📋 LEITNER CSV KOPYALA` & `📋 NET EĞRİ CSV KOPYALA`).

---

## 🛠️ Teknik Kalite ve Mimarî Koruma
- **Test Seti:** `DersIleriFazTest.kt` içinde 7 alt-sistemi (Leitner kutu yükseltme/düşürme, boş deste sıfır kart, Kutu 3 üst sınır denetimi, PDF vurgu ayırıcıları, deneme net eğrisi yükseliş/düşüş algılama, Active Recall kelime/cümle skoring eşikleri, Animedoro/Ultradian seans adları, ÖSYM çeldirici rehberi ve Leitner/Deneme CSV formatı) test eden **26 yeni saf JVM birim testi** yazıldı. Projedeki toplam test sayısı **970** oldu (`970 tests, 0 failures, 0 errors`).
- **Ana Ekran Sadeleştirme Uygunluğu:** `pref_atolye_goster` ayarı kapalıyken (`false`), Ana Sayfada sadece `⏱` ve `⚙` görünmeye devam eder; açıkken tüm 9 atölye butonu (`openDersIleriFaz` dâhil) listelenir.
- **Tasarım Ölçeği:** Hiçbir sabit dp köşeliği veya sp harf boyutu kullanılmadı; `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` referanslarına bağlı kalındı.
- **Dalga Tutarlılığı:** Tüm tıklanabilir kartlara `selectableItemBackground` eklendi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **APK:** `/home/user/GunlukAsistan-v10.60.apk`
2. **Kaynak Kodu:** `/home/user/kaynak-v10.60-yedek.zip`
3. **Proje Durumu:** `/home/user/PROJE-DURUM.md` ve `/home/user/uploads/PROJE-DURUM.md`
4. **Sürüm Notları:** `/home/user/GunlukAsistan-v10.60-notlar.md`
