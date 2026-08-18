# 🎓 GÜNLÜK ASİSTAN — v10.77 (versionCode 233) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **1.365 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`) — Yeni Rekor!**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: KPSS / Sınav Hazırlık Modu Karar Motoru & Merkezi Yönetim Atölyesi (`KpssModuKararMotoru` & `KpssMerkeziYonetimActivity`)
Kullanıcının **"Ben suanlik kpss çalışmiyorum onu heryerden kapat acmak icin ayarlardan ayarbileyim ve ayarlarda kpss icin herseyini yonetebilecegim bir yer ayarla ve bütün ayarlarini ordan yapabileceyim."** talimatı doğrultusunda, uygulamadaki tüm KPSS, YKS ve sınav hazırlık araçlarını merkezi olarak denetleyen anahtar ve yönetim atölyesi geliştirildi (**v10.77 - versionCode 233**).

### 1. 🎓 KPSS / Sınav Modu Varsayılan Olarak KAPALIDIR (`kpssModuAktifMi = false`)
- Kullanıcının şu an KPSS çalışmadığını belirtmesi üzerine, **KPSS / Sınav Hazırlık Modu** uygulamada varsayılan olarak **KAPALI** konuma getirildi.
- **Kapalı Durumda Ne Olur?**
  - **Ana Ekran (`HomeFragment`):** Sınav Net & Puan Projeksiyonu Barometresi (`cardPuanProjeksiyon`) ekrandan tamamen gizlenir.
  - **İlerleme Ekranı (`ProgressFragment`):** KPSS / YKS deneme net barometresi ekrandan kaldırılır; sade Yaşam Sağlığı ve Biyo-Ritim İlerleme Kokpiti görünür.
  - **Gündem Brifingleri (`GundemBrifingMotoru`):** Sabah ve akşam brifinglerinden KPSS ders görevleri (`Tarih 2 Pomodoro`, `Matematik Türev` vb.) otomatik çıkarılır; yerini su tüketimi (`2500ml Su`), WHO tansiyon takibi (`120/80 mmHg`) ve 16:8 oruç hedefleri alır.

### 2. 🎛️ Ayarlar Ekranında "Aç / Kapat" Anahtarı (`rowKpssModuToggle` / `swKpssModu`)
- **Ayarlar** ekranının üst bölümüne **`🎓 KPSS / YKS Sınav Hazırlık Modu (Aç / Kapat)`** satırı ve anahtarı (`SwitchMaterial`) eklendi.
- **Açık Durum (`true`):** Tek dokunuşla tüm KPSS/YKS deneme barometreleri, rütbeler, Leitner kutuları ve sınav brifingleri anında görünür hale gelir.
- **Kapalı Durum (`false` - Varsayılan):** Sınav modülleri heryerden gizlenir; sade Yaşam, Sağlık ve Kişisel Görev Asistanı çalışır.

### 3. 🏛️ KPSS / YKS Merkezi Yönetim & Ayarlar Atölyesi (`KpssMerkeziYonetimActivity`)
- **Ayarlar** ekranına eklenen **`🎓 KPSS / YKS Merkezi Yönetim & Ayarlar Atölyesi`** satırına dokunarak açabileceğiniz bu özel yönetim ekranından **KPSS ile ilgili her şeyi tek bir yerden** denetleyebilirsiniz:
  1. **🎯 Hedef Puan Değiştir:** 400 Puan (Temel Baraj), 450 Puan (Atanma Hedefi), 480 Puan (Derece Hedefi).
  2. **📈 Hedef & Mevcut Net Ayarla:** 85.0 Net / 70.0 Net, 90.0 Net / 78.5 Net, 105.0 Net / 92.0 Net senaryoları.
  3. **📌 Hedef Sınav Türü Seç:** KPSS Lisans 2026, KPSS Önlisans 2026, YKS / TYT-AYT 2026, ALES / DGS 2026.
  4. **📚 Tüm Sınav Çalışma Atölyelerine Hızlı Giriş:** Ders Kolaylık Atölyesi (#1..#10), Ders İleri Faz (Leitner #1, #41), Uzman Çalışma Merkezi (Faz 2..5) ve Ders Uzman Faz 6 (Rütbe & Taktik) ekranlarına tek tuşla anında erişim.

---

## 🛠 Teknik ve Mimari Gelişmeler
1. **100% Tasarım Ölçeği ve Ripple Uyumu:** XML layoutları harfiyen `@dimen/ga_kose_orta` (16dp), `@dimen/ga_yazi_normal` (15sp) ve `android:foreground="?attr/selectableItemBackground"` kuralları ile tasarlandı.
2. **Rekor Birim Test Başarısı:** `KpssModuKararTest.kt` bünyesinde yazılan **25 yeni JVM JUnit testi** ile toplam test sayısı **1.341'den 1.365'e** yükseltildi. **1.365 testin tamamı 0 hata, 0 başarısızlık ile geçildi (`EXIT=0`).**

---

## 📦 Teslim Paketi (4 Parça Kuralı)
1. **`GunlukAsistan-v10.77.apk`**: Kurulabilir bağımsız APK dosyası.
2. **`kaynak-v10.77-yedek.zip`**: Derleme ve ara önbellek dosyalarından (`build/`, `.gradle/`) arındırılmış tam kaynak kodu arşivi.
3. **`PROJE-DURUM.md`**: Güncellenmiş genel proje durum tablosu ve kilometre taşları.
4. **`GunlukAsistan-v10.77-notlar.md`**: Bu detaylı sürüm notları belgesi.
