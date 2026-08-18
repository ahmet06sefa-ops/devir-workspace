# Günlük Asistan — Sürüm 10.99 (versionCode 255) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Video Kapak Fotoğrafı, Süre Rozeti, Detaylı Açıklama & Yanlış Gruptan Taşıma

Kullanıcının **"Yanlarinda kapak fotoğraflari kac dakika oldukları vs vs gibi açıklamalari olsun eklediğim videolarin. Videolari tasiyabileyjm o gruba ait değilse"** talimatı doğrultusunda, oynatma listesindeki her video satırı, YouTube arayüzündeki gibi 16:9 oranında **kapak fotoğrafı (thumbnail)**, sağ alt köşesinde **video süresi (42:15)**, altında **müfredat/format açıklamaları** barındıran şık bir kart düzenine yükseltilmiş; yanlış gruba düşen videoların doğru YouTube kampına aktarılması için **"🔀 Grubu Değiştir / Taşı"** motoru entegre edilmiştir.

---

## 📺 Sürüm 10.99'da Yenilenen Özellikler

1. **🖼️ 16:9 Kapak Fotoğrafı (Thumbnail) ve Süre Rozeti (`42:15`):**
   - Her video satırının sol kısmına 16:9 oranında kapak fotoğrafı çerçevesi (`imgVideoKapak`) eklendi.
   - Kapak fotoğrafının sağ alt köşesine, tıpkı YouTube arayüzündeki gibi videonun kaç dakika/saniye olduğunu gösteren koyu zeminli beyaz **Süre Rozeti (`txtVideoSure`, Ör: `42:15`)** yerleştirildi.

2. **🏷️ Detaylı Video Açıklaması ve Format Etiketleri:**
   - Video başlığı ve sıra numarasının hemen altında videonun içeriğini ve biçimini belirten **Açıklama Satırı (`txtVideoAciklama`, Ör: `🏷️ ÖSYM Müfredatı ile Uygun Ders Videosu · HD 1080p MP4`)** gösterilir.
   - Videonun telefonunuzda eşleşip eşleşmediği yeşil/turuncu renk kodlarıyla net biçimde vurgulanır.

3. **🔀 "O Gruba Ait Değilse Videoları Taşıyabilme" Özelliği (`btnVideoTasi`):**
   - Bir video satırında **"🔀 Grubu Değiştir"** butonuna dokunduğunuzda, videonun o anki gruba ait olmaması durumunda taşınabileceği tüm diğer YouTube kampları ve listeleri açılır.
   - Dilediğiniz gruba **TAŞI** (mevcut gruptan çıkarıp doğru gruba al) veya **KOPYALA** diyebilir ya da o an **"➕ Yeni YouTube Kampı / Oynatma Listesi Oluştur ve Oraya Taşı"** seçeneğiyle yeni grup açıp aktarabilirsiniz.
   - Taşıma sonrasında kaynak grup ve hedef grup sıra numaraları (**#1, #2...**) otomatik senkronize edilir.

4. **▶️ Çevrimdışı Native Video Oynatıcı Çözümü:**
   - Videolar internetten değil doğrudan telefonunuzun yerel dosyalarından native Android oynatıcısıyla (Galeri, Video Oynatıcı, VLC vb.) çevrimdışı açılmaya devam eder.

---

## 🛠️ Birim Test Rejimi: 1.495 Test, 0 Hata — Yeni Rekor

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (25 Birim Testi):**
   - Video kapak, süre, açıklama, yanlış gruptan taşıma ve JSON kalıcılığını doğrulayan 25 test yazıldı:
     - `youtube oynatma listesi video nesneleri kapak sure ve aciklama bilgilerini saklar`
     - `youtube oynatma listesi videoyu baska gruba tasiyinca kaynak gruptan silinip hedef grupta siralanir`
     - `youtube oynatma listesi json kaydetme ve okumada sure ile aciklamayi korur`
     - `youtube oynatma listesi yapay zeka toplu dosyalari kpss mat tarih turkce gruplarina ayirir` vb.
   - Testler arası izolasyon için `@Before setup() { YoutubePlaylistMotoru.testIcinSifirla() }` mekanizması korundu.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.495 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile doğrulandı.

---

## 📦 Sürüm 10.99 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.99.apk`](https://gofile.io/d/6jJww9) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.99-yedek.zip`](https://gofile.io/d/3kM2y1) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/qE6Lw0) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.99 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.99-notlar.md`](https://gofile.io/d/rJ9pG4) | Viewer'da ön izlemeye açıldı · Kapak fotoğrafı, süre ve taşıma dokümantasyonu |

- **APK MD5:** `ca34b2bb2cfe68e2164b7df6bd6c6b8c`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
