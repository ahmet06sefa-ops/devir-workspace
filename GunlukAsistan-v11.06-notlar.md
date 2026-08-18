# Günlük Asistan — Sürüm 11.06 (versionCode 262) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Dikey Vakit Planı, Gösterişli Dini Sözler Kartı & Sesli Namaz Alarmları

Kullanıcının **"Vakit plani sekmesini yatay konumda kaydırılabilir olan seher kuşluk vb sekmeyi alt alta olarak yap. Oraya ekstra olarak dini sozler yeri ekle her vakitte farkli sozler gelsin. Gösterişli olsun.namaz saatlerinde alarm sesi gelmiyor onu düzelt gelsin ayarlayabileyim ayarlardan."** talimatı doğrultusunda Vakit Planı ve Namaz Vakitleri ekranı (`PlanFragment.kt`, `NamazActivity.kt`, `NamazBildirim.kt`, `NamazAyarActivity.kt`) yenilenerek gösterişli bir ibadet, plan ve ilham merkezine dönüştürülmüştür.

---

## 📱 Sürüm 11.06'da Yapılan Yenilikler ve Düzeltmeler

### 1. ↕️ Alt Alta Sıralanan Dikey Vakit Planı ("Seher, Kuşluk vb. Alt Alta")
- **Yatay Kaydırmanın Kaldırılması:** Önceki sürümlerde Seher, Kuşluk, Öğleden Sonra, İkindi Sonrası, Akşam ve Gece vakit dilimleri yatay kaydırılabilir (`HorizontalScrollView`) dar kartlar içinde gösteriliyordu.
- **Tam Genişlikte Alt Alta Kartlar (`PlanFragment.kt`):** Artık tüm vakit dilimi kartları ekranda hiçbir yatay kaydırmaya gerek kalmadan **alt alta dikey sırada** ve tam genişlikte (`MATCH_PARENT`) sunulmaktadır.
- Aktif vakit dilimi en üstte öne çıkarılırken diğer vakit dilimleri hemen altında düzenli ve okunaklı bir şekilde listelenir.

### 2. 🕌✨ Gösterişli Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Kartı (`DiniSozMotoru.kt`)
- **Her Vakte Özel Hikmetli Sözler:** Günün 6 farklı vakit dilimi (`SABAH/Seher`, `KUSLUK/İşrak-Kuşluk`, `OGLEDEN/Öğle`, `IKINDIDEN/İkindi`, `AKSAMDAN/Akşam`, `GECE/Yatsı-Teheccüd`) için özenle seçilmiş 24'ten fazla hadis-i şerif, dua ve hikmetli söz eklendi.
- **Gösterişli Kart Tasarımı (`vaktinSozuKarti`):** Hem **Vakit Planı sekmesinin** en üstüne hem de **Namaz Vakitleri ekranına** ana temaya uygun, şık bordürlü, hilal ikonlu (`"🕌✨"`) ve estetik tipografiye sahip MaterialCardView eklendi.
- **🔄 "Başka Söz" Butonu:** Kullanıcı kart üzerindeki **"🔄 Başka Söz"** butonuna dokunarak o vakte ait farklı hadis ve hikmetli sözleri anında yenileyip okuyabilmektedir.

### 3. 🔊 Namaz Saatlerinde Sesli Alarm Düzeltmesi & Ayar Menüsü
- **Neden Ses Gelmiyordu:** Önceki sürümlerde `ses_uri` varsayılan olarak boş (`""` = sessiz) tanımlanmış ve bildirim servisindeki alarm tetikleyici varsayılan olarak devre dışı kalmıştı.
- **Kesin Çözüm (`NamazBildirim.kt`, `ZorunluUyari.kt`):**
  - Namaz vakti geldiğinde, özel bir MP3 seçilmemiş olsa dahi sistemin standart uyarı/alarm sesi devreye girer.
  - Vakit girdiğinde `ZorunluUyari.cal(context, zorlaCal = true)` tetiklenerek telefon sessizde olsa dahi alarm sesli olarak çalar (güç tuşuna basılarak anında durdurulabilir).
- **⚙️ Ayarlardan Tam Kontrol (`NamazAyarActivity.kt`, `SettingsFragment.kt`):**
  - **"🔊 Namaz Saatlerinde Sesli Alarm Çal"** anahtarı eklendi (`AÇIK / KAPALI`).
  - **"▶️ Alarm Sesini Şimdi Dinle & Test Et (30 sn)"** butonu eklendi. Dokunduğunuz anda namaz alarm sesi çalmaya başlar; böylece ses tonunu ve şiddetini anında test edebilirsiniz.
  - **"🎵 Ses Tonu Seç"** butonu üzerinden istediğiniz ezan, çan veya sistem uyarı sesini atayabilirsiniz.
  - Ana Ayarlar ekranına da **"🔊 Namaz Vakitlerinde Sesli Alarm Çal"** anahtarı eklenerek genel ayarlardan erişim sağlandı.

---

## 🧪 Kalite ve Test Güvencesi
- **1.574 Saf JVM Birim Testi (%100 Başarı, 0 Hata — YENİ REKOR):** `DiniSozTest.kt` adında yeni bir test sınıfı ile 15 saf JVM testi yazıldı (6 vakit dilimi sözleri, kaynak referansları, rastgele söz seçme, güvenli endeks ve varsayılanlar). Toplam **114 test sınıfında 1.574 testin tamamı (%100 başarı, 0 hata, 0 başarısız)** temiz derlemeyle (`EXIT=0`) geçti.
- **Mimari Koruma Testleri:** `TasarimOlcegiTest`, `RippleTutarlilikTest` ve `AnaEkranButonTest` testlerinden 0 hata ile geçildi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **Kurulabilir APK:** `~/GunlukAsistan-v11.06.apk` (v5.0 SHA-256 imza anahtarıyla %100 uyumlu, doğrudan güncellenebilir).
2. **Tam Kaynak Kod Yedeği:** `~/kaynak-v11.06-yedek.zip`.
3. **Proje Durum Raporu:** `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md`.
4. **Müstakil Sürüm Notları:** `~/GunlukAsistan-v11.06-notlar.md`.
