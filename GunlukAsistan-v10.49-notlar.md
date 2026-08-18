# Günlük Asistan v10.49 (kod 205) — Sürüm Notları

## Bu sürümde: 8 Aşırı İşlevsel Görünüm ve Arayüz Ayarı (#2, #3, #5, #6, #7, #8, #9, #10)
**Kullanıcı İsteği:** "Bana 10 adet uygulamanın bütün görünümünü daha kullanışlı yapmam için ayar öner ama aşırı işlevsel olsun. (1 ve 4 hariç hepsini yap)"
- **Neden ihtiyaç vardı?** Uygulama arayüzü herkesin kullanım tarzına, zihinsel yük durumuna veya parmak ergonomisine aynı oranda uyum sağlayamıyordu. Bu 8 ayar sadece kozmetik değişim değil, **bilişsel yükü, okuma hızını ve gezinme süresini doğrudan iyileştiren** aşırı işlevsel çözümler sunuyor.
- **Çözüm (`GorunumAyar.kt` & `GorunumAyarActivity`):**
  1. **#2 GÖRSEL YOĞUNLUK & FERAHLIK SEÇİCİ (`kartModu`):**
     - Görevler, Notlar ve Etkinlikler listelerinde 3 kademe: *Tam Kart* (açıklamalar görünür), *Kompakt* (`8dp` dolgu), *Minimal Satır* (`4dp` dolgu, etiket ve ikonsuz ultra sade satırlar).
  2. **#3 ÖNCELİK & ACİLİYET VURGUSU (`oncelikVurgu`):**
     - Acil (`priority=3`) ve bekliyor (`⏳`) görevlerde kart vurgusu: *Soluk Nokta*, *5dp Kenar Şeridi* veya *Zemin Parlaması* (kartın arka planını alarm rengiyle tonla).
  3. **#5 TİPOGRAFİ VİTRİNİ (`fontSablon`):**
     - Uygulamanın yazı fontunu amaca göre değiştirme: *Poppins* (modern), *Atkinson Hyperlegible* (göz yormayan, erişilebilir), *Lora* (konu anlatımı için kitap dokusu).
  4. **#6 SATIR NEFESİ & BOŞLUK ATÖLYESİ (`satirNefesiDp`):**
     - Satırlar ve kartlar arasındaki dikey nefes boşluğunu `0dp (Sıkı)`, `6dp (Normal)` ve `12dp (Ferah)` olarak seçebilme.
  5. **#7 ZAMANLAYICI GÖRSEL DAVRANIŞI (`zenOdakMi`):**
     - *Zen Odak Modu:* Sayaç çalışırken saniyelerin akışını, rulo efektlerini ve nabız animasyonlarını gizler; ekranda sakin, hareketsiz `"24 dk kaldı"` yazısı kalır.
  6. **#8 AÇILIŞ EKRANI & KOKPİT GİZLİLİĞİ (`acilisEkran`, `heroGizliMi`):**
     - Uygulamanın ilk açılışta *Ana Ekran*, *Görevler*, *Zamanlayıcı* veya *AI Ajan* sekmesine açılmasını seçme; ana ekrandaki kahraman kartını daraltıp gizleyebilme.
  7. **#9 AKILLI FAB & BUTON ATAMASI (`fabIslev`):**
     - Ekranın sağ altındaki büyük artı (`+`) butonuna *Görev Ekle*, *25 dk Odak Başlat*, *Komut Paleti Aç* veya *AI Ajanı Aç* işlevlerinden birini kalıcı atama.
  8. **#10 YÜZEN CANLI DURUM ŞERİDİ (`yuzenSeritAcik`, `yuzenSeritMetni`):**
     - Tüm ekranların alt menü üzerinde çalışan sayacın kalan süresini (`"⏱ Odak: 18:42 kaldı"`) veya gün serinizi (`"🔥 Gün seriniz: 14 gün güvende"`) gösteren, tıklandığında ilgili ekrana uçuran yüzen ince bar.

## Ölçümler
- Derleme: **tek tur yeşil, ~20 sn artımlı derleme** · EXIT=0
- Testler: **790 test · 0 hata** (+12 yeni birim test: `GorunumAtolyeTest`)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔ — v5.0 anahtar dizisi)
- APK md5: `3e8e96bc829eaeae2c727ce6a77397a8`
- APK dosya boyutu: ~26 MB
- Kaynak zip (`kaynak-v10.49-yedek.zip`): ~12 MB
