# Günlük Asistan v10.48 (kod 204) — Sürüm Notları

## Bu sürümde: Kullanıcı maddesi #10 (🤖 OTONOM AI AJANI & OTOPİLOT MİMARİSİ)

### 🔟 🤖 Otonom AI Ajanı, Gündem Orkestratörü, Seri Bekçisi & Otopilot Merkezi
**Şikayet & İstek:** "Farklı öner, uygulamada olmayan yapay zeka ile ilgili olsun, uygulamayı yönetsin. (Hepsini)"
- **Neden ihtiyaç vardı?** Mevcut yapay zeka araçları yalnızca pasif metin tavsiyesi veriyordu. Uygulamanın bir araç olmaktan çıkıp hayatı **aktif olarak yöneten, veritabanını güncelleyen ve kullanıcıyı koruyan** otonom bir asistana dönüşmesi istendi.
- **Çözüm (`OtonomMerkezActivity` & `OtonomMotor.kt`):**
  1. **🤖 Eylem Yetkili AI Ajanı (Tool/Function Calling):**
     - Doğal dil ile yazılan komutlar (`"Sabah uyanma saatimi 07:30 yap, 25 dk sayaç kur ve 'Market alışverişi' görevi ekle"`) `OtonomMotor.ajanKomutuAyristir` ile analiz edilir.
     - Ajan sadece tavsiye yazmaz; **saniyesinde veritabanında işlem yaparak** uyanma saatini (`UykuCerceve`), sayaç süresini (`TimerEngine`), görev listesini (`Store.addTask`) ve odak hedefini anında günceller!
  2. **⚡ Akıllı Gündem Orkestratörü:**
     - Dün geceki uyku sürenizi (`UykuCerceve`) ve günün saatini analiz eder.
     - Az uyku (< 6 saat) durumunda ağır bilişsel işleri (matematik, rapor, proje vb.) öğleden sonraki en verimli odak penceresine alır; kolay rutin işleri (market, arama vb.) sabahın boşluklarına yerleştirir.
  3. **🛡️ Akıllı Alışkanlık & Seri Bekçisi:**
     - Akşam saatlerinde kırılmak üzere olan aktif serileri (`Store.habitStreak >= 2`) proaktif olarak tespit eder.
     - Kırılma riski olan alışkanlık için özel **"▶ 10 Dk Sayaç Başlat & Kurtar"** butonu sunar. Süre dolunca alışkanlığı veritabanında otomatik olarak `DONE` işaretler!
  4. **🧹 Otonom Kütüphaneci & Veritabanı Temizlik Ajanı:**
     - Depodaki tüm dağınık notları okuyup (`Store.loadNotes`) içindeki eylem gerektiren ifadeleri (`[ ]`, `TODO`, `al`, `yaz` vb.) tespit eder ve tek dokunuşla Görevler listesine ekler.
  5. **🤖 AI Otopilot Modu Anahtarı:**
     - Tek anahtarla aktif edilen otopilot modu (`Store.getOtopilotAcik`); yoğun ve yorgun günlerde günlük odak hedefini güvenli esnek seviyeye (`otopilotHedefHesapla`) kısıtlayarak tükenmişliği önler.

## Arayüz Köprüleri (Erişim Kolaylığı)
- **Ana Ekran (`HomeFragment`):** Üst çubuğa, ayarlar (⚙), manuel kontrol (🎛️) ve zamanlayıcı (⏱) simgesinin hemen yanına hızlı erişim simgesi (**`🤖`**) eklendi.
- **Ayarlar Ekranı (`SettingsFragment`):** "Manuel Kontrol Merkezi" satırının hemen altına **"🤖 AI Otopilot & Ajan Merkezi"** satırı eklendi.
- **Tasarım Uyumu:** Tüm butonlar (`ElevatedButton`, `OutlinedButton`), köşe yarıçapı (`@dimen/ga_kose_orta`) ve yazı boyutları TasarimOlcegiTest bekçisinden sıfır hatayla geçti.

## Ölçümler
- Derleme: **tek tur yeşil** · EXIT=0
- Testler: **778 test · 0 hata** (+14 yeni birim test: `OtonomTest`)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔ — v5.0 anahtar dizisi)
- APK md5: `8a34c5f5a9b78d9fb155f158f5eae375`
- APK dosya boyutu: ~26 MB
- Kaynak zip (`kaynak-v10.48-yedek.zip`): ~12 MB
