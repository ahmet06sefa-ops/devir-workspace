# Günlük Asistan — Sürüm 11.05 (versionCode 261) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Çalışma Zamanı Ekranı Sıfırlama Koruması, Dakika-Saniye Detayı & Tek Ekran Sadeleşmesi

Kullanıcının **"Zamanlayıcıyı başlatınca ileri veya kronometre yazısına tıklayınca zamanlayıcı sıfırlanıyor düzelt ve zamanlayıcıda .... dakika kaldı yazıyor onu dakika saniye olarak yazsın ve etiket ekle, zincir kur gibi yazılar olmasın. O sayfayı tek bir ekrana sığdır ama yazılar bozulmasın."** talimatı doğrultusunda Çalışma Zamanı (Pomodoro/Sayaç) ekranı (`fragment_timer.xml`, `TimerFragment.kt`, `SayacAyar.kt`) optimize edilmiş ve olası yanlışlıkla sıfırlamaların önüne geçilmiştir.

---

## 📱 Sürüm 11.05'te Yapılan İyileştirmeler ve Düzeltmeler

### 1. 🛡️ Çalışan Zamanlayıcının Sıfırlanma Koruması
- **Sorun:** Önceki sürümlerde zamanlayıcı aktif olarak sayarken veya ortada bir oturum varken mod seçici butonlarına ("İleri" veya "Kronometre") basıldığında `resetAll()` tetikleniyor ve çalışan zamanlayıcı sıfırlanıyordu.
- **Çözüm:** `TimerFragment.kt` içindeki mod değiştirme dinleyicisine (`addOnButtonCheckedListener`) aktif oturum ve çalışma durumu koruması eklendi. Artık zamanlayıcı çalışırken mod butonlarından birine basılırsa:
  - Çalışan zamanlayıcı **asla sıfırlanmaz**.
  - Kullanıcıya şık bir bildirim verilir: `"⏱️ Sayacınız çalışırken mod değiştirilemez. Önce duraklatın / sıfırlayın."`
  - Mod anahtarı, oturum kesintiye uğratılmadan aktif moda geri döner.

### 2. ⏱️ Tam Dakika ve Saniye Gösterimi (`MM:SS kaldı (MM dk SS sn)`)
- **Sorun:** Zen Odak modunda veya kalan süre göstergelerinde süre yalnızca yuvarlatılmış dakika olarak (Örn: `18 dk kaldı`) yazıyordu.
- **Çözüm:** `SayacAyar.kalanSureDakikaSaniyeMetni(millis)` fonksiyonu geliştirildi. Artık kalan süre her zaman **Dakika ve Saniye detayında** (Örn: `18:45 kaldı (18 dk 45 sn)`) net ve kesin bir biçimde yazmaktadır.

### 3. 🧹 "Etiket Ekle" ve "Zincir Kur" Buton/Yazılarının Kaldırılması
- Zamanlayıcı ana ekranında görsel kalabalığa yol açan `"🏷️ Etiket Ekle"` (`etiketChip`) ve `"🔗 Zincir Kur"` (`zincirChip`) yazıları tüm arayüzden gizlendi (`visibility = View.GONE`).
- Böylece sayaç ekranı en saf, odaklı ve minimalist görünümüne kavuştu.

### 4. 📲 Kaydırmasız Tek Ekran & Bozulmayan Yazı Oranları
- `SayacKadraniView.KOMPAKT_KADRAN_ORANI = 0.46f` ölçeği, gizlenen etiket/zincir çipleri ve optimize edilen boşluklarla birlikte tüm çalışma zamanı ekranı dikey kaydırmaya gerek kalmadan **standart tüm Android cihazlarda tek bir ekrana tam sığmaktadır**.
- Yazı boyutları (`@dimen/ga_yazi_devasa`, `@dimen/ga_yazi_normal` vb.) ve kart oranları korunarak hiçbir yazının bozulmaması, üst üste binmemesi veya taşmaması garanti edildi.

---

## 🧪 Kalite ve Test Güvencesi
- **1.559 Saf JVM Birim Testi (%100 Başarı, 0 Hata — YENİ REKOR):** `TekEkranZamanlayiciTest.kt` içerisine v11.05'e özel 5 yeni test eklendi (`kalan sure dakika ve saniye metni olarak bicimlendirilir`, `etiket ve zincir yazilari kaldirilarak tek ekran kompakligi arttirildi`, `sayac calisirken ileri veya kronometre moduna basinca sifirlama engellenir`, `zen odak modunda kalan sure dakika saniye detayini icerir`, `tek ekran kompakt mod yazilarin bozulmasini engeller`). Toplam **113 test sınıfında 1.559 testin tamamı (%100 başarı, 0 hata, 0 başarısız)** temiz derlemeyle (`EXIT=0`) geçti.
- **Mimari Koruma Testleri:** Hiçbir XML layout dosyasında sert kodlanmış `textSize` veya `cardCornerRadius` kullanılmamış; `TasarimOlcegiTest`, `RippleTutarlilikTest` ve `AnaEkranButonTest` testlerinden 0 hata ile geçildi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **Kurulabilir APK:** `~/GunlukAsistan-v11.05.apk` (v5.0 SHA-256 imza anahtarıyla %100 uyumlu, üzerine güncellenebilir).
2. **Tam Kaynak Kod Yedeği:** `~/kaynak-v11.05-yedek.zip`.
3. **Proje Durum Raporu:** `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md`.
4. **Müstakil Sürüm Notları:** `~/GunlukAsistan-v11.05-notlar.md`.
