# Günlük Asistan v10.86 (versionCode 242) — Sürüm Notları
**Tarih:** 11 Ağustos 2026  
**Tema:** Uygulama Geneli Tablo ve Kart Konu Başlıkları Yönetimi (`com.gunlukasistan.app`)

---

## 🎯 Kullanıcı Talimatı ve Çözüm Özeti

Kullanıcı talimatı:
> *"Bana uygulamanin içindeki tablolarin konu başlıklarını kaldirmani istiyorum. Mesela günlük ilerleme , konularim , odak sesleri vb gibi."*

### Yapılan Kapsamlı Başlık Temizliği & Yönetimi:
1. **🚫 Tüm Tabloların ve Kartların Üst Yazılarının Kaldırılması (`TabloBaslikYonetimMotoru.kt`):**
   - Uygulamadaki tüm sekmeler tarandı ve tablolardan/kartlardan üst konu başlıkları varsayılan olarak **%100 kaldırıldı / gizlendi (`View.GONE`)**.
   - **Kaldırılan Başlıklar:**
     - **Ana Sayfa (`HomeFragment`):** `"Günlük İlerleme"`, `"Konularım"`, `"Hızlı Erişim"`, `"Son 30 Gün"` üst yazıları.
     - **İlerleme Ekranı (`ProgressFragment`):** `"Günlük İlerleme"`, `"Konu Dağılımı & Çalışma Analizi"`, `"Haftalık Çalışma Puanı"`, `"Aylık Çalışma Isı Haritası"` üst yazıları.
     - **Sayaç Ekranı (`TimerFragment`):** `"Odak Sesleri"` ve `"Arka Plan Müzik / Radyo"` üst yazıları.
   - Tablolar, grafikler ve kartlar artık hiçbir gereksiz üst yazı çubuğu olmadan en minimalist ve sade haliyle görüntüleniyor.

2. **⚙️ Ayarlarda Kontrol Anahtarı (`SettingsFragment`, `SayacAyarActivity`):**
   - Dilediğiniz zaman bu başlıkları geri açabilmeniz için **Ayarlar > `⚡ HIZLI KONTROLLER & TEMEL SEÇİMLER`** menüsüne *"📑 Tablo ve Kart Konu Başlıklarını Göster (Aç/Kapat)"* anahtarı eklendi.

3. **🔬 Birim Test Rekoru (`VeriSenkronizasyonTest.kt`):**
   - Tablo konu başlıklarının varsayılan kapalılığını ve durum metni biçimini doğrulayan **4 yeni JUnit testi** eklendi.
   - Toplam test sayısı: **1.431 / 1.431 başarılı (`0 hata, 0 başarısızlık`)** — Yeni Tarihi Rekor!

---

## 📦 4-Parça Teslim Paketi

1. `GunlukAsistan-v10.86.apk` (26 MB) — Kurulabilir tam APK
2. `kaynak-v10.86-yedek.zip` (12 MB) — Eksiksiz kaynak kod yedeği
3. `PROJE-DURUM.md` — Güncellenmiş durum tablosu ve sürüm geçmişi
4. `GunlukAsistan-v10.86-notlar.md` — Bu sürüm notu

---
**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**
