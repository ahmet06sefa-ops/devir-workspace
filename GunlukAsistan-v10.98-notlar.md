# Günlük Asistan — Sürüm 10.98 (versionCode 254) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Evrensel YouTube Kampı Algılama & Grubun Tamamını Silme Eklentisi

Kullanıcının **"Youtube videolarini internetteki gibi duzgun sekilde ayiramiyorsun düzelt. Youtube da olanlara bile youtube da değil diyorsun. Tek tek silmek yerine grubun tamamini silme eklentisi de ekle."** talimatı doğrultusunda, yapay zekânın YouTube ders kamplarını sınıflandırma ve gruplama motoru **evrensel eğitim veritabanıyla** genişletilmiş, YouTube'da olan ders videolarına asla "YouTube dışı" denilmeyen dinamik kümeleme mimarisi kurulmuş ve her oynatma listesi başlığına **"🗑️ Grubun Tamamını Sil"** butonu entegre edilmiştir.

---

## 📺 Sürüm 10.98'de Yenilenen Özellikler

1. **🤖 Evrensel YouTube Eğitim Kampı & Kanalı Sınıflandırma Motoru (`YapayZekaYoutubeSiralamaMotoru.kt`):**
   - Türkiye'de takip edilen tüm temel eğitim alanları ve YouTube kanalları (Matematik, Geometri, Tarih, Türkçe, Coğrafya, Vatandaşlık, Fizik, Kimya, Biyoloji, İngilizce, Yazılım vb.) eksiksiz olarak ayrı YouTube kamp sekmelerine (grup grup) ayrılır.
   - **"YouTube'da olanlara bile YouTube dışı deme" sorunu çözüldü:** Listede adı geçmeyen farklı ders videoları dahi (Psikoloji, Felsefe, Muhasebe vb.) asla "YouTube dışı" denilmez; dosya adından konusu çıkartılarak **"📺 YouTube Oynatma Listesi: [Konu] Kampı"** başlığıyla kendi bağımsız grubuna dizilir.
   - Yalnızca gerçekten kişisel/eğitim dışı olan kayıtlara (tatil, toplantı, aile, kamera, whatsapp, doğum günü vb.) `"📁 Diğer Yerel & Özel Videolar (YouTube Dışı)"` denilir.

2. **🗑️ Grubun Tamamını Silme Eklentisi (`btnGrubuSil`):**
   - Her oynatma listesi sekmesinin üst künye kartına (`layoutPlaylistKunye`) **"🗑️ Grubun Tamamını Sil"** butonu eklendi.
   - Tek tek video silmek yerine bu butona basarak dilediğiniz oynatma listesi grubunu tüm videolarıyla birlikte anında silebilir ve kitaplığınızı temizleyebilirsiniz.

3. **▶️ Çevrimdışı Native Video Oynatıcı & Tam Video Yönetimi:**
   - Her satırda bulunan `▶️ Oynat`, `🔀 Taşı/Ekle` ve `🗑️ Kaldır` butonları ile videonun internetten değil doğrudan yerel dosyadan açılması (`Intent.createChooser`, Galeri/VLC), başka listelere taşınması/kopyalanması ve sıra numaralarının (#1, #2...) anlık senkronize edilmesi garanti altına alınmıştır.

---

## 🛠️ Birim Test Rejimi: 1.492 Test, 0 Hata (Yeni Rekor)

1. **Yenilenen Test Suite: `YoutubePlaylistTest.kt` (22 Birim Testi):**
   - Evrensel YouTube kamp tanıması, dinamik kamp üretimi, grubun tamamını silme ve çevrimdışı native oynatma kurarlarını doğrulayan 22 test yazıldı:
     - `youtube oynatma listesi yapay zeka cografya ve geometri kamplarini da tanir diger listesine atmaz`
     - `youtube oynatma listesi grubun tamamini silme islemi tum listeyi ve videolari kaldirir`
     - `youtube oynatma listesi yapay zeka toplu dosyalari kpss mat tarih turkce gruplarina ayirir`
     - `youtube oynatma listesi yapay zeka youtube da olmayan videolari diger yerel listesinde toplar`
     - `youtube oynatma listesi videoyu kaldirinca siralamayi yeniden duzenler` vb.
2. **Toplam Başarı:**
   - Proje genelinde **109 test sınıfı, 1.492 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile doğrulandı.

---

## 📦 Sürüm 10.98 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v10.98.apk`](https://gofile.io/d/YtW2n8) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v10.98-yedek.zip`](https://gofile.io/d/1DkXy9) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/qWf7C1) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v10.98 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v10.98-notlar.md`](https://gofile.io/d/kL0Rz2) | Viewer'da ön izlemeye açıldı · Evrensel YouTube algılayıcı ve grup silme dokümantasyonu |

- **APK MD5:** `d237cb7987828b1cbdc3aae08f1b8359`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutulmuştur.
