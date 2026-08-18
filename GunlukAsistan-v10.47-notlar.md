# Günlük Asistan v10.47 (kod 203) — Sürüm Notları

## Bu sürümde: Kullanıcı maddesi #9 (🎛️ Manuel Kontrol Merkezi)

### 9️⃣ 🎛️ Manuel Kontrol Merkezi (Uyku/uyanma, odak, seri ve rutin yönetimi)
**Şikayet & İstek:** "Uyanma ve uyuma saatlerini elle manuel kontrol edebilmek istiyorum ve bir çok şeyi manuel kontrol edebilme yeri koy."
- **Neden ihtiyaç vardı?** Kullanıcılar uyanma bildirimine o an dokunamayabilir veya uyku saatini değiştirmek/düzeltmek isteyebilir. Ayrıca uygulamadaki otomatik işleyen (odak süresi, gün serisi, rutinler) süreçlere tek bir merkezden elle müdahale etme yetkisi istendi.
- **Çözüm (`ManuelKontrolActivity` & `ManuelKontrol.kt`):**
  1. **🛏️ Uyku & Uyanma Saatleri (Elle Kontrol):**
     - **Bugünkü Uyanma Saatim:** `TimePickerDialog` üzerinden uyanma saati seçilir. Anında `UykuCerceve.uyandiKaydet(ctx, ms)` ve `sabahVerildi` kaydı oluşturulur, uyku süresi güncellenir.
     - **Dünkü / Bu Geceki Uyuma Saatim:** Gece yarısından önce (ör. 23:30) veya sonra (ör. 01:15) seçilen uyku saatleri takvim günü sarmalı (`ManuelKontrol.uyumaZamaniHesapla`) ile doğru güne eşlenir.
     - **Geçmiş 14 Günün Uyku Defteri:** Son 14 günün listesi (`gecmisGunListeYarat`) ekrana açılır. Herhangi bir gün seçilerek **"⏰ Uyanma saatini düzenle"**, **"🌙 Uyuma saatini düzenle"** veya **"🗑️ O günün kaydını sil"** işlemleri uygulanır (`UykuCerceve.elleKaydet`).
  2. **⚡ Odak Süresi & Sayaç Dakikaları (Elle Kontrol):**
     - Bugünkü toplam odak süresi görüntülenir.
     - **`+15 dk`**, **`+30 dk`**, **`-15 dk`** hızlı butonları ve **`✏️ Serbest Dakika Gir`** diyaloğu ile günlük odak süresi istenilen değere ayarlanır (`Store.addTodayFocusMinutes`).
  3. **🔥 Gün Serisi & Başarı Durumu (Elle Kontrol):**
     - **Seriyi Elle Değiştir:** Gün serisi (`Store.setStreakDays`) 0 ile 9999 gün arasında serbestçe değiştirilebilir. Kazara kırılan seriler kolayca geri getirilir.
     - **Bugünü Başarılı Say:** Günlük hedef tamamlandı olarak işaretlenir.
  4. **🌅 Rutinleri Şimdi Çalıştır:**
     - Sabah günaydın planı (`UykuAksiyonReceiver.elleSabahCalistir`) ve akşam kapanış sorusu saati beklenmeden tek dokunuşla anında tetiklenir.
  5. **🔄 Bugünkü Durumu Sıfırla:**
     - Bugünkü tüm uyku ve odak kayıtları tek dokunuşla temizlenip sıfırlanabilir.

## Arayüz Köprüleri (Erişim Kolaylığı)
- **Ana Ekran (`HomeFragment`):** Üst çubuğa, ayarlar (⚙) simgesinin hemen yanına hızlı erişim simgesi (**`🎛️`**) eklendi.
- **Ayarlar Ekranı (`SettingsFragment`):** "Gün çerçevesi (uyku düzeni)" satırının hemen altına **"🎛️ Manuel Kontrol Merkezi"** satırı eklendi.
- **Uyku Ayarları (`UykuAyarActivity`):** "Uyku Kaydı" bölümünün başına doğrudan düzenleyiciye götüren buton eklendi.

## Ölçümler
- Derleme: **tek tur yeşil, ~19 sn artımlı derleme** · EXIT=0
- Testler: **764 test · 0 hata** (+12 yeni birim test: `ManuelKontrolTest`)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔ — v5.0 anahtar dizisi)
- APK md5: `df7615ddc3826302056f1ebc2983b58a`
- APK dosya boyutu: ~26 MB
- Kaynak zip (`kaynak-v10.47-yedek.zip`): ~12 MB
