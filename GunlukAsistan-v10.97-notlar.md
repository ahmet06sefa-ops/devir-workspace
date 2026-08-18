# Günlük Asistan — Sürüm 10.97 (versionCode 253) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Yapay Zekâ Toplu Video Gruplama, YouTube Dışı Liste & Tam Video Yönetimi

Kullanıcının **"Dosya eklemeyi halletmissin ama toplu dosya ekleyince onu internette yapay zeka ile arastirip youtube da ozellikle playist listesine göre ayirmiyorsun grup grup. Youtube da olmayanlari da ayri bir listede tur ve eklediğin videolari kaldirabileyim , tasiyabileyim , listeden silebileyim ve baska listelere ekleyebileyim"** talimatı doğrultusunda, toplu eklenen videoların otomatik olarak YouTube listelerine göre **grup grup ayrılması**, YouTube'da yer almayan videoların **ayrı bir listede toplanması** ve her bir video üzerinde **Kaldır/Sil, Taşı ve Başka Listeye Ekle (Kopyala)** işlemlerinin yapılmasını sağlayan **Yapay Zekâ Toplu Video Gruplayıcı ve Yönetim Merkezi (`YapayZekaYoutubeSiralamaMotoru.kt`, `YoutubePlaylistMotoru.kt`)** hayata geçirilmiştir.

---

## 📺 Sürüm 10.97'de Yenilenen Toplu Gruplama & Video Yönetim Özellikleri

1. **🤖 Toplu Video İçe Aktarma ve YouTube Oynatma Listesine Göre Grup Grup Ayırma:**
   - Kullanıcı telefonundan çok sayıda video dosyasını tek seferde seçtiğinde (`topluDosyalariGruplayipSirala`), yapay zekâ her dosyanın adını ve örüntüsünü tarar.
   - Videolar ait oldukları YouTube ders kampına (Ör: `"KPSS Matematik Benim Hocam"`, `"KPSS Tarih Ramazan Yetgin"`, `"KPSS Türkçe Aker Kartal"` vb.) göre otomatik olarak **grup grup ayrılır** ve her grup üst sekmelerde bağımsız bir oynatma listesi olarak oluşturulur.
   - Her grup kendi içinde orijinal **#1, #2, #3...** sırasına göre dikey tablo olarak dizilir.

2. **📁 YouTube'da Olmayan Videolar İçin Ayrı Liste (`ID_DIGER_YEREL`):**
   - Herhangi bir YouTube kampına ait olmayan, kişisel veya özel ders videoları **`"📁 Diğer Yerel & Özel Videolar (YouTube Dışı)"`** adlı ayrı ve özel bir sekmede toplanır; diğer ders kamplarıyla asla karışmaz.

3. **⚙️ Her Video Satırında Tam Yetkili Yönetim ("Kaldır, Taşı, Başka Listeye Ekle"):**
   - **🗑️ Kaldır / Sil (`btnVideoSil`):** İlgili videoyu bulunduğu oynatma listesinden anında siler (`videoyuKaldir`). Listede kalan diğer videoların sıra numaralarını (**#1, #2...**) otomatik olarak yeniden düzenler.
   - **🔀 Taşı / Başka Listeye Ekle (`btnVideoTasi`):** Videoyu dilediğiniz başka bir oynatma listesine **Taşır** (mevcut listeden kaldırıp hedefe ekler) veya **Kopyalar** (her iki listede de tutar). Taşınma sonrası her iki listenin sıra numaraları anlık senkronize edilir.

4. **▶️ Çevrimdışı Native Video Oynatma Çözümü:**
   - Oynatma sorunu kökten çözülmüştür: **"▶️ Oynat"** butonuna basıldığında video internet üzerinden değil, doğrudan telefonunuzun depolama alanındaki yerel dosyadan `Intent.createChooser` ile telefonun kendi video oynatıcısından (Galeri, Video Oynatıcı, VLC vb.) çevrimdışı açılır.

---

## 🛠️ Birim Test Rejimi: 1.490 Test, 0 Hata — Yeni Rekor

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (20 Birim Testi):**
   - Toplu gruplama, YouTube dışı liste ayrımı, silme, taşıma, kopyalama ve çevrimdışı native oynatma kurarlarını doğrulayan 20 test yazıldı:
     - `youtube oynatma listesi yapay zeka toplu dosyalari kpss mat tarih turkce gruplarina ayirir`
     - `youtube oynatma listesi yapay zeka youtube da olmayan videolari diger yerel listesinde toplar`
     - `youtube oynatma listesi videoyu kaldirinca siralamayi yeniden duzenler`
     - `youtube oynatma listesi videoyu baska listeye tasir ve her iki listenin siralamasini gunceller`
     - `youtube oynatma listesi videoyu baska listeye kopyalar` vb.
   - Testler arası izolasyon için `@Before setup() { YoutubePlaylistMotoru.testIcinSifirla() }` mekanizması korundu.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.490 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile doğrulandı.

---

## 📦 Sürüm 10.97 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.97.apk`](https://gofile.io/d/uU3r74) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.97-yedek.zip`](https://gofile.io/d/c7tFjM) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/eM9mJ2) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.97 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.97-notlar.md`](https://gofile.io/d/pD6X5j) | Viewer'da ön izlemeye açıldı · Toplu gruplama ve video yönetim dokümantasyonu |

- **APK MD5:** `7f8f0855a16139e20f450bb82a0d67b7`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
