# Günlük Asistan — Sürüm 10.94 (versionCode 250) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Cihaz Klasöründen YouTube Çevrimdışı Oynatma Listesi Sıralayıcı (Sabit KPSS Listeleri Kaldırıldı)

Kullanıcının **"Hayir bu sekilde değil benim telefonumun dosyalarimin içinde belirlediğim klasörün içindeki videolari youtube playist listesine göre sirala ve konu başlığını playist listesinin başlığına göre koy. Kpss ile ilgili vb kendin urettigjn seyleri ordan kaldır."** talimatı doğrultusunda, uygulama içine önceden yerleştirilen tüm sabit (KPSS, YKS vb.) hazır kamp listeleri **tamamen kaldırılmış**, kullanıcının doğrudan **telefonundaki klasörden** seçtiği ders videolarını YouTube oynatma listesi sıralamasına göre dizecek ve kendi başlığıyla saklayacak dinamik klasör mimarisi (`YoutubePlaylistMotoru.kt`, `YoutubePlaylistActivity.kt`) oluşturulmuştur.

---

## 📺 Sürüm 10.94'te Yenilenen Oynatma Listesi & Klasör Sıralayıcı Özellikleri

1. **🚫 Sabit (KPSS vb.) Hazır Listeler Tamamen Kaldırıldı:**
   - Uygulamanın içinde önceden yer alan "KPSS Matematik Benim Hocam", "KPSS Tarih Ramazan Yetgin" vb. tüm kendimizin ürettiği sabit listeler silinmiştir (`varsayilanPlaylistleriGetir()` artık boş liste döndürür).
   - Ekran ilk açıldığında veya hiç kayıtlı liste olmadığında sade ve bilgilendirici **"📁📺 Henüz Kayıtlı Çevrimdışı Oynatma Listesi Yok"** boş durum ekranı (`layoutBosKitaplik`) karşılar.

2. **📁 Cihaz Klasöründen Videoları Seçme ve Orijinal YouTube Sırasına Dizm:**
   - Ekranda yer alan **"📁 Klasörden Oynatma Listesi Oluştur"** butonu (`btnKlasordenOlustur`), kullanıcının telefonundaki dosya yöneticisinden belirlediği klasörün içindeki videoları (`.mp4`, `.mkv`, `.webm` vb.) seçmesine izin verir.
   - İndirilen ve karışık duran video dosyalarının adlarındaki rakamlar (`01_...`, `1. ...`) veya kullanıcı tarafından girilen YouTube sıra adları baz alınarak videolar orijinal **#1, #2, #3...** sırasına göre tablo halinde dizilir (`klasordenPlaylistOlustur`).

3. **🗂️ Konu Başlığı Orijinal Oynatma Listesi Adıyla & Her Liste Ayrı Ayrı:**
   - Oynatma listesinin konu başlığı, kullanıcının belirlediği orijinal oynatma listesi adıyla (Ör: `"Algoritma ve Veri Yapıları - 2026"`, `"Sistem Programlama Kampı"` vb.) başlığa yerleşir.
   - Her oynatma listesi üstteki çip sekmelerle **ayrı ayrı** filtrelenir; sıralamalar kendi listesinin içinde bağımsız tutulur.

4. **📵 Kesinlikle İnternetten Değil, Telefonun Yerel Klasöründeki Videolardan Çevrimdışı Oynatma:**
   - Satırdaki **"▶️ Oynat"** butonuna basıldığında video internet üzerinden **DEĞİL**, doğrudan kullanıcının belirlediği klasördeki yerel video dosyasından native Android Video Oynatıcı (`Intent.ACTION_VIEW`, `"video/*"`) ile çevrimdışı açılır.

---

## 🛠️ Birim Test Rejimi: 1.485 Test, 0 Hata (Yeni Rekor)

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (+15 Birim Testi):**
   - Sabit listelerin kaldırıldığını, klasörden video listesi alınarak sıralama oluşturulduğunu ve çevrimdışı oynatma kurallarını doğrulayan 15 test yazıldı:
     - `youtube oynatma listesi motoru sabit kpss yks kamplari dondurmez` (`isEmpty()` doğrulaması)
     - `youtube oynatma listesi baslangicta hic ozel liste yoksa bostur`
     - `youtube oynatma listesi klasorden video listesi alarak siralama olusturur`
     - `youtube oynatma listesi konu basligini kullanici secimine gore koyar`
     - `youtube oynatma listesi videolarin siralamasi numara sirasina gore 1den baslar`
     - `youtube oynatma listeleri birbirinden ayri ve bagimsizdir`
     - `youtube oynatma listesi sayisal index cikarici dosya adindan dogru sayiyi bulur`
     - `youtube oynatma listesi videoyu cihazdan oynat internetten degil yerel dosyadan acar` vb.
   - Testler arası izolasyon için `@Before setup() { YoutubePlaylistMotoru.testIcinSifirla() }` mekanizması korundu.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.485 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile kesintisiz doğrulandı.

---

## 📦 Sürüm 10.94 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.94.apk`](https://gofile.io/d/6k6x8E) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.94-yedek.zip`](https://gofile.io/d/uUjUv6) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/Pz6T2n) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.94 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.94-notlar.md`](https://gofile.io/d/9gR4g9) | Viewer'da ön izlemeye açıldı · Klasörden oynatma listesi mimari dokümantasyonu |

- **APK MD5:** `b25f32d9b04b888ecd01c1b9dac60f35`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
