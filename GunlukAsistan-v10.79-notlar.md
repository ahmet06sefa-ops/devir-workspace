# Günlük Asistan v10.79 (versionCode 235) — Sürüm Notları
**Tarih:** 11 Ağustos 2026  
**Tema:** Kullanıcı Veri Senkronizasyonu & Dinamik Ders/Aktivite Motoru (`com.gunlukasistan.app`)

---

## 🎯 Kullanıcı Talebi ve Çözüm Özeti

Kullanıcı geri bildirimi:
> *"Uygulama icinde veri senkronizasyonu eksik mesela benim olmayan dersleri ekleme yapmissin ya da gun gun aciklamali yaşam tablosundaki veriler benim değil ve bir cok yerde benim olmayan girdigim dışında olaylar var düzelt bul hepsini"*

### Yapılan Kapsamlı Senkronizasyon Çalışmaları:
1. **🚫 Sahte / Kullanıcıya Ait Olmayan Derslerin Temizlenmesi (`GunlukAktiviteTabloMotoru.kt`, `KpssSayacAtolye.kt`, `DersUzmanFaz6Activity.kt`):**
   - 30 günlük Gün Gün Açıklamalı Yaşam ve Odak Tablosu, Executive Kokpit, Gündem Brifingleri ve Canavar Konu menülerinde yer alan sabit sahte veriler (`Tarih - Osmanlı Dağılma`, `Matematik - Türev`, `İntegral`, `Fizik`, `Kimya`) tamamen temizlendi.
   - Artık uygulamadaki tüm listeler ve tablolar **yalnızca kullanıcının Store üzerine girdiği gerçek dersleri (`Store.loadCourses`)** ve **KpssSayacAtolye'de seçtiği dersi** gösteriyor.

2. **📈 Gerçek Zamanlı Odak & Soru Çözümü Senkronu (`GunlukAktiviteTabloMotoru.kt`, `ExecutiveProgressMotoru.kt`):**
   - 30 günlük tablonun her satırı doğrudan kullanıcının günlük kayıt kökünden (`Store.logRoot`) okunan gerçek odak süresi (`f`), tamamlanan madde (`c`) ve çözülen soru sayısı (`q`) verileriyle anlık eşleştirildi.
   - Çalışma veya soru çözümü girilmeyen günler için sahte "120 dakika" veya "85 soru" verileri yerine dürüstçe `0 dk (0 Pomodoro)`, `0 Soru`, `-Not` ve *"Bu gün için henüz odak süresi veya çalışma kaydı girilmemiş"* açıklaması sunuluyor.

3. **🕌 Resmî Diyanet Namaz Vakitleri Senkronu (`GunlukAktiviteTabloMotoru.kt`, `NamazVakti.kt`):**
   - 30 günlük yaşam ve ibadet tablosundaki imsak, öğle ve yatsı saatleri kullanıcının ayarlardan seçtiği şehrin %100 resmî Diyanet namaz saatleri (`NamazVakti.bugun(context)`) ile senkronize edildi.

4. **🧹 İlk Açılış Temizliği (`Store.kt`):**
   - Uygulamanın ilk yüklenişinde `seedTopicsIfNeeded` tarafından konular listesine otomatik eklenen örnek `Biyoloji` ve `Matematik — Türev` başlıkları kaldırıldı. Konular ekranı tamamen temiz başlatılıyor ve yalnızca kullanıcının girdiği konular ekranda kalıyor.

5. **🔬 100% Birim Test Başarısı & Yeni Rekor (`VeriSenkronizasyonTest.kt`):**
   - Senkronizasyon motorunun hem gerçek Android `Context` ile canlı çalışmasını hem de contextsiz (`null`) saf JVM birim testi senaryolarını doğrulayan **15 yeni JUnit testi** eklendi.
   - Toplam test sayısı: **1.406 / 1.406 başarılı (`0 hata, 0 başarısızlık`)** — Yeni Tarihi Rekor!

---

## 📦 4-Parça Teslim Paketi

1. `GunlukAsistan-v10.79.apk` (26 MB) — Kurulabilir tam APK
2. `kaynak-v10.79-yedek.zip` (12 MB) — Eksiksiz kaynak kod yedeği
3. `PROJE-DURUM.md` — Güncel durum tablosu ve sürüm geçmişi
4. `GunlukAsistan-v10.79-notlar.md` — Bu sürüm notu

---
**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**
