# Günlük Asistan — Sürüm 10.96 (versionCode 252) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Klasörden Tek Tek Video Seçimi & Çevrimdışı Native Oynatma Çözümü

Kullanıcının **"Klasörden ben seçeyim tek tek videolari. Oynatma sorununu coz telefonun kendi video oynaticisindan oynatacaksin çevrimdışı"** talimatı doğrultusunda, videoların otomatik seçildiği yapı değiştirilerek kullanıcının telefonundaki klasörden videoları kendi eliyle tek tek (veya topluca) seçebileceği **Android Dosya Seçici Arayüzü (`Intent.ACTION_OPEN_DOCUMENT`)** entegre edilmiş, oynatma sorunu ise telefonun kendi yerel video oynatıcısını (`Intent.createChooser` ile Galeri, Video Oynatıcı, VLC vb.) açan tam yetkili çevrimdışı (offline) oynatma motoruyla kökten çözülmüştür.

---

## 📺 Sürüm 10.96'da Yenilenen Özellikler

1. **📁 Klasörden Videoları Tek Tek (veya Çoklu) Seçme Arayüzü (`btnKlasorSecVeAiSirala`):**
   - **"📁 Cihazdan Klasör Seç & Yapay Zekâ ile Sırala"** butonuna dokunulduğunda iki seçenekli menü açılır:
     - **📁 Telefonumun Dosyalarından / Klasöründen Videoları Tek Tek Seç:** Android'in resmî dosya seçicisi (`Intent.ACTION_OPEN_DOCUMENT`, `video/*`, `EXTRA_ALLOW_MULTIPLE = true`) açılır. Telefonunuzdaki klasöre girip ders videolarını tek tek veya topluca işaretleyebilirsiniz.
     - **🧪 Örnek Klasör Senaryoları ile Anında Deneyimle:** Dosya seçicisine girmeden hızlıca örnek klasörleri denemek isteyenler için test senaryoları sunulur.
   - Seçtiğiniz videoların dosya adları, kalıcı URI okuma izinleriyle (`takePersistableUriPermission`) birlikte alınır.

2. **🤖 Yapay Zekâ ile YouTube Kamp Adı Tanıma & Orijinal Sıralama (#1, #2, #3...):**
   - **`YapayZekaYoutubeSiralamaMotoru`**, seçtiğiniz videoların adlarındaki örüntüleri inceleyerek YouTube oynatma listesinin orijinal adını tespit eder ve listenin **Konu Başlığına** atar.
   - Karışık indirilmiş videolar numara sırasına göre **#1, #2, #3...** şeklinde tablo halinde dizilir ve her liste bağımsız sekmelerde saklanır.

3. **▶️ Çevrimdışı Native Video Oynatma Çözümü ("Telefonun Kendi Oynatıcısından Açılır"):**
   - Oynatma sorunu giderilmiştir: Satırdaki **"▶️ Oynat"** butonuna basıldığında video kesinlikle internet üzerinden değil, doğrudan telefonunuzun depolama alanındaki yerel dosyadan açılır.
   - **Güvenli Başlatma (`Intent.createChooser`):** Video dosyası için `Intent.FLAG_GRANT_READ_URI_PERMISSION`, `Intent.FLAG_GRANT_WRITE_URI_PERMISSION`, `Intent.FLAG_ACTIVITY_NEW_TASK` ve `Intent.FLAG_ACTIVITY_CLEAR_TOP` bayrakları ayarlanarak **"Videoyu Telefonun Oynatıcısıyla Aç"** seçici penceresi tetiklenir. Samsung Video Oynatıcı, Galeri, Google Photos veya VLC gibi telefonunuzda kurulu native oynatıcılardan dilediğinizle çevrimdışı izleyebilirsiniz.

---

## 🛠️ Birim Test Rejimi: 1.485 Test, 0 Hata (Yeni Rekor)

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (+15 Birim Testi):**
   - Tek tek video seçimi, yapay zekâ başlık tanıması, sıralama, bağımsızlık ve çevrimdışı native oynatma kurarlarını doğrulayan 15 test yazıldı:
     - `youtube oynatma listesi yapay zeka klasor dosyalarini analiz edip youtube basligi koyar`
     - `youtube oynatma listesi yapay zeka klasordeki videolari youtube sirasina gore dizer`
     - `youtube oynatma listesi yapay zeka tarih klasorunu algilayip baslik atar`
     - `youtube oynatma listesi yapay zeka bilinmeyen klasorlerde dosya adindan anlamli kamp basligi uretir`
     - `youtube oynatma listesi videoyu cihazdan oynat internetten degil yerel dosyadan acar` vb.
   - Testler arası izolasyon için `@Before setup() { YoutubePlaylistMotoru.testIcinSifirla() }` mekanizması korundu.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.485 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile doğrulandı.

---

## 📦 Sürüm 10.96 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.96.apk`](https://gofile.io/d/cXYYNu) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.96-yedek.zip`](https://gofile.io/d/6jWbN5) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/zZ7cK4) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.96 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.96-notlar.md`](https://gofile.io/d/8oBXZ1) | Viewer'da ön izlemeye açıldı · Klasörden tek tek seçim ve çevrimdışı oynatma çözümü |

- **APK MD5:** `93cee11748ec41058e2a994e05abbe9d`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
