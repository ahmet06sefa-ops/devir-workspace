# Günlük Asistan v10.85 (versionCode 241) — Sürüm Notları
**Tarih:** 11 Ağustos 2026  
**Tema:** İlerleme Ekranı Konu Dağılımı ve Aylık Takvim Günlük Ayrıntıları (`com.gunlukasistan.app`)

---

## 🎯 Kullanıcı Talimatı ve Çözüm Özeti

Kullanıcı talimatı:
> *"Ilerleme kisminda konu dagilimi kismini biraz daha anlaşılır yap ve tiklayinca ayrıntılarına girilsin.ilerleme kismindaki aylık tabloyu da günlük tıklama yapınca günlük ayrintilar çıksın hangk tarihte ise"*

### Yapılan Kapsamlı İlerleme Ekranı Özelleştirmeleri:
1. **📊 Konu Dağılımının Anlaşılır Kılınması ve Özet Tablo (`layoutKonuDagilimListesi`):**
   - İlerleme ekranındaki (`ProgressFragment`) Konu Dağılımı alanında, halka grafiğinin hemen altına her bir konunun:
     - Renk kodu noktası (`GradientDrawable.OVAL`),
     - Başlığı (`📚 Matematik`, `📚 Tarih` vb.),
     - Tamamlanma yüzdesi ve bitti/toplam alt başlık oranı (`4/5 Alt Başlık Tamam — %80`),
     - Tahmini odak süresi (`~125 Dk Odak`) bilgilerini yansıtan net bir özet listesi eklendi.

2. **🔬 Tıklanabilir Konu Çalışma Ayrıntıları ve Koçluk Analizi (`konuAyrintiDiyalogunuGoster`):**
   - Halka grafiğine veya listedeki herhangi bir konuya dokunduğunuzda, o konunun:
     - Tüm alt başlıklarının tamamlanma durumlarını (`✅` veya `⏳`),
     - Toplam tahmini odak süresini ve pomodoro oturum sayısını,
     - Yapay zekâ koçluk analizini barındıran **"📚 Çalışma Ayrıntıları & Analizi"** penceresi açılır.

3. **📅 Aylık Takvim Isı Haritası Günlük Ayrıntıları (`gunlukAyrintiPenceresiniGoster`):**
   - İlerleme ekranının alt kısmındaki 31 günlük aylık takvim ısı haritasında (`heatGrid`) yer alan 1'den ay sonuna kadar **bütün gün hücreleri tıklanabilir yapıldı**.
   - Herhangi bir tarihe (Örn: 5 Ağustos, 10 Ağustos, 20 Ağustos) dokunduğunuzda, o günün:
     1. Toplam odak süresini (`Saat / Dakika`),
     2. Pomodoro oturum sayısını,
     3. Çözülen soru adedini ve karne notunu (`A+`, `B` vb.),
     4. Çalışılan Konularım (`Store.loadTopics`) ders ve alt başlığını,
     5. Diyanet namaz vakti ve yaşam sağlığı senkron durumunu,
     6. Yapay zekâ koçluk yorumunu gösteren **"📅 Günlük Çalışma & Yaşam Ayrıntıları"** özet penceresi açılır.
   - Penceredeki **"📋 Detaylı Tabloda Aç"** butonuyla dilediğiniz günün 30 günlük tam tablosuna tek tıkla geçiş yapabilirsiniz.

4. **🔬 Birim Test Rekoru (`VeriSenkronizasyonTest.kt`):**
   - Konu dağılımı yüzdelerini, tahmini odak sürelerini ve takvim anahtarlarını doğrulayan **4 yeni JUnit testi** eklendi.
   - Toplam test sayısı: **1.427 / 1.427 başarılı (`0 hata, 0 başarısızlık`)** — Yeni Tarihi Rekor!

---

## 📦 4-Parça Teslim Paketi

1. `GunlukAsistan-v10.85.apk` (26 MB) — Kurulabilir tam APK
2. `kaynak-v10.85-yedek.zip` (12 MB) — Eksiksiz kaynak kod yedeği
3. `PROJE-DURUM.md` — Güncellenmiş durum tablosu ve sürüm geçmişi
4. `GunlukAsistan-v10.85-notlar.md` — Bu sürüm notu

---
**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**
