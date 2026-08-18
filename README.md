# Günlük Asistan — Devir Çalışma Alanı (Workspace Yedeği)

Bu depo, **Günlük Asistan (`com.gunlukasistan.app`)** Android projesinin tam kapsamlı geliştirme, derleme, test ve otomasyon çalışma alanı (workspace) yedeğidir.

## 📦 İçerik Özeti

* **`GunlukAsistan/`**: Ana Android projesi (Kotlin, Room, PiP, Bildirim & Zamanlayıcı motorları, 752+ birim test).
* **`DEVIR/`**: 1000 önerilik gelişim kataloğu (`ONERI-1000.md`) ve mimari belgeler.
* **`PROJE-DURUM.md`**: Projenin güncel durumu, test raporları ve sürüm senkronizasyon tablosu (`v10.47` - 8/8 kullanıcı maddesi).
* **`KOD-ATLASI.md`**: Kod tabanının ve modüllerin detaylı mimari haritası.
* **`GunlukAsistan-v*-notlar.md`**: Her sürüm için tutulan müstakil sürüm notları (`v10.36` - `v10.47`).
* **Derleme & Teslim Betikleri**: 
  * `derle.sh` / `derle-release.sh`: Hızlı derleme betikleri.
  * `teslim.sh` / `hizli-teslim.sh`: Sürüm paketleme ve doğrulama betikleri.
  * `test.sh`: Birim test çalıştırma ve doğrulama betiği.
  * `kur-ortam.sh` / `ortam.sh`: Çevrimdışı/sanal ortam kurulum betikleri.
* **`YEDEK/` & `otonom/`**: Otomatik doğrulama ve kurtarma araçları.

## 🛡️ Güvenlik ve Gizlilik (`.gitignore`)

Bu depo, `.gitignore` kuralları uyarınca aşağıdaki hassas ve gereksiz dosyalardan arındırılarak yedeklenmiştir:
- `.env`, şifreler, tokenlar, kişisel erişim anahtarları ve keystore dosyaları (`*.keystore`, `debug.keystore`, `*.jks`, `*.pem`)
- Bağımlılık dizinleri (`node_modules/`, `.gradle-home/`, `.android/`, `.venv/` vb.)
- Derleme çıktıları (`build/`, `dist/`, `out/`, `coverage/`)
- Log dosyaları (`*.log`), geçici dosyalar (`*.tmp`) ve test raporları
- Büyük arşivler ve binary veriler (`*.zip`, `*.apk`, `*.mp4`, `workspace_drive_file` vb.)

## 🚀 Durum

* **Sürüm:** **`v10.47`** (`versionCode: 203`)
* **Birim Test:** **764 Test / 0 Başarısız / 0 Hata**
* **Son Güncelleme:** 10 Ağustos 2026
