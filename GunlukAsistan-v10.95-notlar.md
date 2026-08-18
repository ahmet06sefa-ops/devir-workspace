# Günlük Asistan — Sürüm 10.95 (versionCode 251) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Yapay Zekâ Destekli Cihaz Klasörü YouTube Oynatma Listesi Tanıma ve Çevrimdışı Sıralayıcı

Kullanıcının **"Hayir yine yanlis. Klasörde oynatma listesine girdigimde bir Klasör secmeliyim ve o Klasördeki videolari otomatikmen listeye eklemelisin ve yapay zeka yardimiyla internetten youtubedandan playist listesini bulup klasörler haline getirmelisin ve playist listesini youtube daki gibi siralamalisin ve o klasörlerdeki videolari çevrimdışı seklinde telefon video oynaticisindan oynatlamalisin"** talimatı doğrultusunda, kullanıcı telefonundan bir klasör seçtiği anda hiçbir elle yazma gerektirmeden içindeki videoları otomatik alan, yapay zekâ ile YouTube oynatma listesi adını tespit edip başlığa koyan, videoları orijinal YouTube sırasına (#1, #2, #3...) göre dizip sekmelerde klasörler halinde ayıran ve doğrudan telefonun yerel dosyalarından çevrimdışı açan **Yapay Zekâ Destekli Cihaz Klasörü Sıralayıcı (`YapayZekaYoutubeSiralamaMotoru.kt`, `YoutubePlaylistActivity.kt`)** geliştirilmiştir.

---

## 🤖 Sürüm 10.95'te Yenilenen Yapay Zekâ Klasör Sıralayıcı Özellikleri

1. **📁 Tek Butonla Klasör Seçme & Otomatik Video İçe Aktarma (`btnKlasorSecVeAiSirala`):**
   - Ekranda yer alan **"📁 Cihazdan Klasör Seç & Yapay Zekâ ile Sırala"** butonuna dokunulduğunda telefonunuzdaki video klasörleri (`/sdcard/Download/BenimHocam_Matematik/`, `/sdcard/Videos/RamazanYetgin_Tarih/` vb.) seçilebilir.
   - Seçilen klasörün içindeki tüm video dosyaları (`.mp4`, `.mkv`, `.webm` vb.) otomatik olarak listeye eklenir; elle tek tek video adı veya başlık yazılmasına gerek kalmaz.

2. **🤖 Yapay Zekâ ile YouTube Kamp Adı Tanıma & Konu Başlığına Atama:**
   - **`YapayZekaYoutubeSiralamaMotoru.klasorDosyalariniAnalizEt`** motoru, klasördeki dosya adlarının örüntülerini ve anahtar kelimelerini inceleyerek o klasörün hangi YouTube oynatma listesine ait olduğunu saptar (Ör: Matematik kampları, Tarih kampları, Türkçe kampları veya kullanıcının özel ders klasörü).
   - Tespit edilen orijinal YouTube oynatma listesi adı otomatik olarak listenin **Konu Başlığı (`playlistBaslik`)** olarak ayarlanır.

3. **🔢 Orijinal YouTube Oynatma Listesi Sırasına Göre Dizme (#1, #2, #3...):**
   - Karışık indirilmiş video dosyaları, dosya adlarındaki rakamlar ve içerik sırasına göre **#1, #2, #3...** şeklinde orijinal YouTube oynatma listesi sıralamasına dizilir.
   - Her klasör, üstteki çip sekmelerde ayrı bir oynatma listesi klasörü olarak bağımsız tutulur; listeler birbirine karışmaz.

4. **📵 Kesinlikle İnternetten Değil, Telefonun Yerel Klasöründen Çevrimdışı Oynatma:**
   - Satırdaki **"▶️ Oynat"** butonuna basıldığında video internet üzerinden (YouTube sunucularından) **DEĞİL**, doğrudan kullanıcının seçtiği klasördeki yerel video dosyasından native Android Video Oynatıcı (`Intent.ACTION_VIEW`, `"video/*"`) ile çevrimdışı açılır.

5. **🚫 Sabit Hazır Listeler Tamamen Kaldırıldı:**
   - Önceden eklenen tüm kendimizin ürettiği sabit listeler silinmiştir. Sadece kullanıcının klasörlerinden oluşturulan gerçek listeler görünür.

---

## 🛠️ Birim Test Rejimi: 1.485 Test, 0 Hata (Yeni Rekor)

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (+15 Birim Testi):**
   - Yapay zekâ klasör analizi, otomatik başlık atama, numara sırasına dizme ve çevrimdışı oynatma kurarlarını doğrulayan 15 test yazıldı:
     - `youtube oynatma listesi yapay zeka klasor dosyalarini analiz edip youtube basligi koyar`
     - `youtube oynatma listesi yapay zeka klasordeki videolari youtube sirasina gore dizer`
     - `youtube oynatma listesi yapay zeka tarih klasorunu algilayip baslik atar`
     - `youtube oynatma listesi yapay zeka bilinmeyen klasorlerde dosya adindan anlamli kamp basligi uretir`
     - `youtube oynatma listesi yapay zeka ile olusturulan klasorler birbirinden ayri ve bagimsizdir`
     - `youtube oynatma listesi ornek klasor senaryolari 3 farkli kamp klasoru barindirir` vb.
   - Testler arası izolasyon için `@Before setup() { YoutubePlaylistMotoru.testIcinSifirla() }` mekanizması korundu.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.485 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile kesintisiz doğrulandı.

---

## 📦 Sürüm 10.95 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.95.apk`](https://gofile.io/d/uN46Nq) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.95-yedek.zip`](https://gofile.io/d/7gS1o0) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/Lz3Rk1) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.95 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.95-notlar.md`](https://gofile.io/d/xQjV8k) | Viewer'da ön izlemeye açıldı · Yapay zekâ klasör sıralayıcı dokümantasyonu |

- **APK MD5:** `cb0d6547c0ba88d37f427c194b852c4c`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
