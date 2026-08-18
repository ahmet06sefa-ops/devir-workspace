# Günlük Asistan v10.80 (versionCode 236) — Sürüm Notları
**Tarih:** 11 Ağustos 2026  
**Tema:** Konularım (`Store.loadTopics`) Merkezli Ders & Alt Başlık Senkronizasyonu (`com.gunlukasistan.app`)

---

## 🎯 Kullanıcı Açıklaması ve Çözüm Özeti

Kullanıcı açıklaması:
> *"Benim derslerim konularim kismindaki yerde duruyor geriye kalan kurs ders değil. Konularim kisminda dersin ismi ve alt maddelerde ise dersin alt başlıkları ve her zaman onlara çalışıyorum. Hepsini tekrardan gozden geçir senkronizasyonlu bir sekilde."*

### Yapılan Kapsamlı Mimari Yeniden Gözden Geçirme & Senkronizasyon:
1. **🎯 Konularım (`Store.loadTopics`) Sekmesinin Tek Yasal Ders Kaynağı Yapılması:**
   - Daha önce bazı modüllerin kontrol ettiği "Kurslar" (`Store.loadCourses`) bölümü yerine, kullanıcının belirttiği gibi **Konularım (`Store.loadTopics`)** sekmesi uygulamadaki tüm derslerin ve çalışılan konuların tek yasal ve otoriter kaynağı yapıldı.
   - Her bir **`Topic.title`** alanı **Ders İsmi** olarak; her bir **`Topic.items -> SubItem.text`** alanı ise **Dersin Alt Başlıkları** olarak işleniyor.

2. **📚 KPSS Sayaç & Manuel Süre Ekleme Modalleri (`KpssSayacAtolye.kt`):**
   - Sayactaki `"Çalıştığın Dersi Seç"` butonunda ve manuel süre ekleme dialoglarında, artık Konularım sekmesinde oluşturduğunuz tüm **Ders İsimleri (`Matematik`, `Tarih`, `Türkçe` vb.)** ve bu derslere ait **Alt Başlıklar (`Matematik -> Problemler`, `Tarih -> Osmanlı Dağılma` vb.)** listeleniyor.
   - Böylece çalışmaya otururken veya manuel süre girerken hem dersin kendisini hem de çalıştığınız alt başlığı tam isabetle seçebiliyorsunuz.

3. **📅 Gün Gün Açıklamalı 30 Günlük Yaşam Tablosu (`GunlukAktiviteTabloMotoru.kt`):**
   - 30 günlük yaşam ve çalışma tablosundaki `Dersler / Konular` sütunu ve günlük koçluk açıklamaları, Konularım sekmesindeki ders ismi ve alt başlığıyla (`Ders İsmi (Alt Başlık)`) senkronize edildi.

4. **🌅 Akıllı Gündem Sabah / Akşam Brifingleri (`AkilliGundemVeAsistanMerkezi.kt`):**
   - Sabah ve akşam brifinglerindeki yapay zekâ asistanı, Konularım sekmesindeki alt başlıklarınızı tarayarak o günkü çalışma hedefinizi öneriyor (*"🎯 Matematik: Problemler çalışmasını ve pomodoro hedefini tamamla"*).

5. **🐉 Canavar Konu Yenme Menüsü (`DersUzmanFaz6Activity.kt`):**
   - Oyunlaştırma bölümündeki canavar konular da Konularım sekmenizle eşleştirildi.

6. **🔬 Birim Test Rekoru (`VeriSenkronizasyonTest.kt`):**
   - Konularım merkezli senkronizasyonu doğrulayan **5 yeni JUnit testi** eklenerek toplam başarılı test sayısı **1.411 / 1.411 (`0 hata, 0 başarısızlık`)** seviyesine ulaştırıldı.

---

## 📦 4-Parça Teslim Paketi

1. `GunlukAsistan-v10.80.apk` (26 MB) — Kurulabilir tam APK
2. `kaynak-v10.80-yedek.zip` (12 MB) — Eksiksiz kaynak kod yedeği
3. `PROJE-DURUM.md` — Güncellenmiş durum tablosu ve sürüm geçmişi
4. `GunlukAsistan-v10.80-notlar.md` — Bu sürüm notu

---
**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**
