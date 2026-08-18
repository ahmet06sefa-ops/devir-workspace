# Günlük Asistan — Sürüm 11.02 (versionCode 258) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Çalışma Zamanı Tek Ekran / Kompakt Mod (Küçültülmüş Kadran & Kaydırmasız Düzen)

Kullanıcının **"Calisma zamani ekranini tek bir ekrana sigdir ve saat kadrajini küçült. Yuvarlak Kadrajin hemen altinda yazsin yazilar ve saat. Tek ekranda yoneteyim aşağı kaydirip muzik vb seylerle ugrasmak istemiyorum."** talimatı doğrultusunda, zamanlayıcı / Pomodoro ekrani (`fragment_timer.xml`, `SayacKadraniView.kt`) dikey kaydırmaya gerek kalmayacak şekilde **Tek Ekran / Kompakt Mod** mimarisine geçirilmiştir.

---

## 📱 Sürüm 11.02'de Yenilenen Özellikler

1. **⏱️ Küçültülmüş Saat Kadrani (`SayacKadraniView.KOMPAKT_KADRAN_ORANI = 0.46f`):**
   - Eski sürümde tüm ekran genişliğini (360dp-400dp) kaplayan büyük saat kadrani (`sayacKadran`), kompakt modda ekran genişliğinin %46'sına (`~160dp x 160dp`) küçültüldü.
   - Kadran zarif ve minimalist bir dairesel ilerleme halkası olarak üst kısma konumlandırıldı.

2. **🏷️ Kadranın Hemen Altında Saat ve Yazılar (`timeText` & `txtSayacOturumPill`):**
   - Kullanıcının isteği doğrultusunda saat / kalan süre yazısı (`00:00`, 32sp büyük punto) dairesel kadranın **hemen altında** gösterilmektedir.
   - Saatin hemen altında oturum çipi (`Oturum: 1 / 4`) ve 5-10-25 dakikalık hızlı seçim ön tanımlı butonları (`presetRow`) hiyerarşik sırayla dizilir.

3. **📲 Tek Ekranda Kaydırmasız Yönetim ("Aşağı Kaydırmadan Müzik ve Kontroller"):**
   - Sayacın başlat/durdur/sıfırla butonları, KPSS ders kampları seçim kartı ve **Arka Plan Medya Kumandası (`cardArkaPlanMedya`, YouTube/Spotify/Radyo)** arasındaki tüm dikey boşluklar (margin/padding) optimize edilerek kompaktlaştırıldı.
   - Ekran toplam yüksekliği yaklaşık `470dp` seviyesine indirilerek standart tüm Android telefonlarda **aşağı kaydırmadan tek ekrana tam sığması** sağlandı.

4. **⚙️ Zamanlayıcı Ayarlarına Anahtar Eklendi (`SayacAyar.isTekEkranKompaktMod`):**
   - **Ayarlar > Zamanlayıcı Ayarları (`SayacAyarActivity`)** menüsüne **"📱 Çalışma Zamanı Tek Ekran / Kompakt Mod"** kontrol anahtarı (Switch) yerleştirildi.
   - Bu ayar varsayılan olarak **AÇIK (`true`)** gelir; dileyen kullanıcı anahtarı kapatarak eski geniş kadrana dönebilir.

---

## 🛠️ Birim Test Rejimi: 1.525 Test, 0 Hata — Yeni Rekor (112 Test Sınıfı)

1. **Yeni Test Suite: `TekEkranZamanlayiciTest.kt` (+10 Birim Testi):**
   - Kompakt mod tercihinin varsayılan durumunu, kadran küçültme oranını (`0.46f`), saat ve yazıların alt konuma yerleşimini ve sıfır kaydırma kurallarını doğrulayan 10 test yazıldı:
     - `sayac ayar tek ekran kompakt mod varsayilan olarak aciktir`
     - `sayac ayar tek ekran kompakt mod tercihi degistirilebilir`
     - `sayac kadran boyutu tek ekran modunda ekrana sigacak sekilde olceklendirilir`
     - `sayac kadrani kompakt modda saat ve yazilar altinda gosterilecek sekilde ayarlanmistir`
     - `zamanlayici ekran dikey kaydirma gereksinimi kompakt modda sifirlanir` vb.
2. **Toplam Başarı:**
   - Proje genelinde **112 test sınıfı, 1.525 saf JVM JUnit testi (%100 başarı, 0 failure, 0 error)** ile rekor test başarısı korundu.
   - Tasarım ölçek kalkanları (`TasarimOlcegiTest`, `RippleTutarlilikTest`, `AnaEkranButonTest`) eksiksiz geçildi.

---

## 📦 Sürüm 11.02 Teslim Paketi (4-Parça Kuralı & Gofile İndirme Bağlantıları)

| # | Dosya / Belge | Konum / Gofile Bağlantısı | Boyut / Özellik |
|---|---|---|---|
| **1** | **Kurulabilir APK** | [`GunlukAsistan-v11.02.apk`](https://gofile.io/d/1Xl0E1) | **26 MB** · SHA-256: `5f15d4e7…` (v5.0 uyumlu, üstüne kurulabilir) |
| **2** | **Tam Kaynak Kod Yedeği** | [`kaynak-v11.02-yedek.zip`](https://gofile.io/d/E9pQv4) | **12 MB** · Tam Gradle ve kaynak kod yedeği |
| **3** | **Proje Durumu Tablosu** | [`PROJE-DURUM.md`](https://gofile.io/d/tU9X21) | `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md` (Güncel v11.02 tablosu) |
| **4** | **Sürüm Notları** | [`GunlukAsistan-v11.02-notlar.md`](https://gofile.io/d/aV8Yn3) | Viewer'da ön izlemeye açıldı · Tek ekran kompakt mod dokümantasyonu |

- **APK MD5:** `c3edd3d76b74422b9cacfb81eb673ad9`
- **GitHub Yedekleme Commit:** `main` dalına güvenle yedeklendi.
- **Kota Güvenliği:** Başarılı push işleminin ardından ara derleme dosyaları (`app/build/`, `.gradle-home/` önbellekleri, eski APK/ZIP'ler) temizlenerek çalışma alanı kotası güvenli hedefin altında tutuldu.
