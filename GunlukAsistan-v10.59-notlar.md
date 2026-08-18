# 🏠 GÜNLÜK ASİSTAN — v10.59 (versionCode 215) Sürüm Notları

**Tarih:** 10 Ağustos 2026  
**Derleme:** Gradle 8.7 · Kotlin 1.9.24 · Android SDK 34 (minSdk 24)  
**Test Durumu:** **953 Birim Testinin Tamamı Başarılı (`0 failures, 0 errors`)**  
**Tasarım & Mimari Doğrulama:** `TasarimOlcegiTest` & `RippleTutarlilikTest` — 100% Geçerli

---

## 🌟 Sürümün Ana Özeti: Ana Ekran Sadeleştirme & Buton Açma/Kapama Anahtarı ("Eskisi Gibi Ekrana Dönüş")
Kullanıcının gönderdiği ekran görüntüsündeki **"Ana ekrandaki bunu ayarlardan açma kapama yeri koy, istediğim zaman eskisi gibi ekrana döneyim"** talebi üzerine, Ana Sayfa üzerindeki 8 adet ek atölye ve modül kısayol butonunu tek dokunuşla gösterip gizleyen **Ana Ekran Sadeleştirme ve Buton Anahtarı** (`AnaEkranButonKarari.kt`, `Store.kt`, `HomeFragment.kt`, `SettingsFragment.kt`) geliştirildi.

### 1. 🏠 Orijinal v2 Minimalist ve Sade Görünüme Anında Dönüş
- **Varsayılan Görünüm (KAPALI):** Uygulama güncellendiğinde `pref_atolye_goster` varsayılan olarak `false` tanımlıdır. Ana Sayfa (`HomeFragment`) açıldığında son sürümlerde eklenen tüm yuvarlak kısayol butonları (`🎛️`, `🤖`, `🎨`, `🏆`, `🧭`, `🚀`, `🔬`, `🎓`) tamamen gizlenir (`View.GONE`). Ana Sayfa başlığı **tamamen eskisi gibi** yalnızca selamlama yazısı, tarih, **Sayaç (`⏱`)** ve **Ayarlar (`⚙`)** butonuyla tertemiz ve minimalist haline döner.
- **Dilediğiniz Zaman Tekrar Açma (AÇIK):** Atölye butonlarını ana ekranda tekrar görmek isterseniz, **Ayarlar > `🏠 Ana Ekran Atölye Butonları (Aç / Kapat)`** anahtarını açmanız yeterlidir. Ana Sayfaya döndüğünüzde tüm butonlar görünür olur.
- **Her Zaman Erişilebilirlik:** Ana Sayfadaki kısayol butonları gizli (`KAPALI`) olsa bile, tüm atölyeler ve gelişmiş merkezler **Ayarlar** menüsündeki kendi satırlarından (`🎨 Tasarım Atölyesi`, `🏆 Karne & Sesli Brifing`, `🧭 Yaşam Modülleri`, `🚀 C-D-E-G-H-I-J Gelişmiş Hayat`, `🔬 Faz 2 Uzman Modülleri`, `🎓 Ders & Kolaylık Atölyesi`, `🤖 Otonom AI`, `🎛️ Manuel Kontrol`) her an açılabilir!

---

### 2. 🛠️ Teknik Kalite ve Mimarî Koruma
- **Test Seti:** `AnaEkranButonKarari.kt` karar motorunu test eden **15 yeni saf JVM birim testi** (`AnaEkranButonTest.kt`) yazıldı; kapalı durumda sadece 2 temel butonun (`timer`, `settings`) döndüğü, açık durumda 10 ikonun döndüğü ve görünürlük kararlarının (`VISIBLE`/`GONE`) doğruluğu test edildi. Projedeki toplam test sayısı **953** oldu (`953 tests, 0 failures, 0 errors`).
- **Tasarım Ölçeği:** Hiçbir sabit dp köşeliği veya sp harf boyutu kullanılmadı; `@dimen/ga_kose_orta` ve `@dimen/ga_yazi_*` referanslarına bağlı kalındı.
- **Dalga Tutarlılığı:** Tüm tıklanabilir kartlara `android:foreground="?attr/selectableItemBackground"` tanımlandı.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **APK:** `/home/user/GunlukAsistan-v10.59.apk`
2. **Kaynak Kodu:** `/home/user/kaynak-v10.59-yedek.zip`
3. **Proje Durumu:** `/home/user/PROJE-DURUM.md` ve `/home/user/uploads/PROJE-DURUM.md`
4. **Sürüm Notları:** `/home/user/GunlukAsistan-v10.59-notlar.md`
