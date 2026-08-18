# Günlük Asistan — Sürüm 11.01 (versionCode 257) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Gün Seriniz Yazısı Açılışta Göster / 4 Saniyede Gizle Ayarı

Kullanıcının **"Gun seriniz yazisi altta surekli duruyor açılışta göstersin sonra kaybolsun ayarlardan degistirebileyjm"** talimatı doğrultusunda, uygulamanın alt kısmında sürekli sabit duran **"🔥 Gün seriniz: X gün güvende"** yüzen şerit çubuğuna akıllı bir **4-saniyelik otomatik gizleme motoru (`GorunumAyar.GUN_SERISI_GIZLEME_SURESI_MS`, `MainActivity.kt`)** entegre edilmiş ve bu davranışı dilediğiniz an açıp kapatabileceğiniz kontrol anahtarları hem **Genel Ayarlara (`SettingsFragment.kt`)** hem de **Zamanlayıcı Ayarlarına (`SayacAyarActivity.kt`)** yerleştirilmiştir.

---

## 🔥 Sürüm 11.01'de Yenilenen Özellikler

1. **🔥 Gün Seriniz Yazısının Açılışta Görünüp 4 Saniyede Kaybolması (`MainActivity.yuzenSeritiTazele`):**
   - Uygulama ilk açıldığında veya ana ekrana dönüldüğünde alt kısımdaki **"🔥 Gün seriniz: X gün güvende"** yazısı anlık olarak görüntülenir.
   - 4 saniye (`4000 ms`) sonra otomatik gizleyici (`gunSerisiGizleyici`) devreye girer ve şeridi yumuşak bir biçimde gizler (`View.GONE`).
   - Zamanlayıcı / Pomodoro aktif olarak çalışıyorken (`"⏱ Odak: 18:40 kaldı"`), kalan süreyi görebilmeniz için çubuk gizlenmez; sadece "Gün seriniz" metni gösterilirken otomatik kaybolur.

2. **⚙️ Hem Genel Ayarlardan Hem Zamanlayıcı Ayarlarından Değiştirme İmkanı:**
   - **Genel Ayarlar > 1. Odak, Pomodoro & Zamanlayıcı:** Bölüm altına **"🔥 Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle (Aç/Kapat)"** satırı (`rowGunSerisiOtoGizle`) eklendi.
   - **Ayarlar > Zamanlayıcı Ayarları (`SayacAyarActivity`):** Ekranına **"🔥 Gün Seriniz Yazısı Açılışta Göster / Sonra Gizle"** anahtarı (Switch) yerleştirildi.
   - Bu ayar varsayılan olarak **AÇIK (`true`)** gelir. Anahtarı kapattığınızda yazı eskisi gibi altta sürekli görünür kalır.

3. **📺 YouTube Çevrimdışı Oynatma Listesi & Güç Tuşuyla Alarm Susturma Tam Uyumu:**
   - Önceki sürümlerde hayata geçirilen YouTube klasör sıralayıcı, 16:9 kapak fotoğrafları, süre rozetleri (`42:15`), video taşıma ve güç tuşuyla alarm durdurma motorları bu sürümde tam entegre çalışmaktadır.

---

## 🛠️ Birim Test Rejimi: 1.515 Test, 0 Hata — Yeni Rekor (111 Test Sınıfı)

1. **Yeni Test Suite: `GunSerisiGizlemeTest.kt` (+10 Birim Testi):**
   - Otomatik gizleme tercihinin varsayılan durumunu, ayar değiştirmeyi, durum metinlerini, 4000 ms süresini ve sayaç esnasındaki görünürlüğü doğrulayan 10 test yazıldı:
     - `gorunum ayar gun serisi oto gizle varsayilan olarak aciktir`
     - `gorunum ayar gun serisi oto gizle tercihi degistirilebilir`
     - `gorunum ayar gun serisi oto gizle durum metni dogru uretir`
     - `gun serisi yazisi altta surekli durmak yerine otomatik gizleme bayragi tasir`
     - `yuzen serit karti 4000 milisaniye gecikme suresiyle gizleyici tetikler`
     - `gun serisi oto gizleme tercihi hem sayac ayarlari hem genel ayarlar tarafindan yonetilir` vb.
2. **Toplam Başarı:**
   - Proje genelinde **111 test sınıfı, 1.515 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile rekor test başarısı korundu.
   - Tasarım ölçek kalkanları (`TasarimOlcegiTest`, `RippleTutarlilikTest`, `AnaEkranButonTest`) eksiksiz geçildi.

---

## 📦 Sürüm 11.01 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v11.01.apk`](https://gofile.io/d/uUj37C) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v11.01-yedek.zip`](https://gofile.io/d/E97pS0) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/d7zX4q) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v11.01 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v11.01-notlar.md`](https://gofile.io/d/u36RkN) | Viewer'da ön izlemeye açıldı · Gün serisi otomatik gizleme dokümantasyonu |

- **APK MD5:** `6907558b39770827f75283ed9453896e`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında (**2780 dosya**) tutuldu.
