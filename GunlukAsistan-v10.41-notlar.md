# Günlük Asistan v10.41 (kod 197) — Sürüm Notları

## Bu sürümde: kullanıcı maddeleri #1 + #2 + katalog #64 + kayıp kaynak onarımı

### 1️⃣ Madde 1 — İleri sayım artık bildirimde (katalog #64 ✅)
**Şikayet:** "Zamanlayıcının ileri kısmı bildirimlerde çıkmıyor; durdurma, bekletme vb. yok."
- İleri sayım açıkken **kalıcı canlı bildirim**: sistem kronometresiyle saniye saniye kendini günceller (uygulama hiç pil harcamaz — sayacı Android çizer).
- Aksiyon düğmeleri: **⏸ Bekle ⇄ ▶ Devam** ve **⏹ Bitir** — ekran kapalıyken bile çalışır.
- Bitir'de süre odak saatine yazılır + oturum geçmişine işlenir (ekrandaki Bitir ile birebir aynı kayıt kanalı).
- Yeni dosyalar: `IleriSayimBildirim.kt`, `IleriSayimReceiver.kt` (+ bildirim kanalı `ileri_sayim_canli_v1`, LOW sessiz).

### 2️⃣ Madde 2 — Kadran yazıları orantısız büyüktü → kademeli boyut ayarı
**Şikayet:** "Zamanlayıcıdaki yazılar çok orantısız büyük duruyor."
- Kök sebep: kadran metinleri özel View'da **piksel** çiziliyor; uygulamanın yazı ölçeği ayarına hiç uymuyordu (sp sistemi dışı).
- Çözüm: `SayacKadraniView.yaziOlcek` + **Zamanlayıcı Ayarları → "Kadran yazı boyutu"** satırı — 4 kademe: Küçük (×0,80) · Orta (×0,90) · Varsayılan (×1,00) · Büyük (×1,15). Üst/süre/alt metinlerinin üçü de aynı oranda değişir.
- "Her şeyi tek tek elimle yerleştirebileyim" (tam serbest yerleşim editörü) büyük bir ayrı projedir; dürüstlük gereği bu dalgada alınmadı — yol haritasında (v10.44+).

### 🔧 Snapshot onarımı (2. olay — dürüstlük notu)
Tur başında anlık görüntü yine v10.39 öncesine dönmüştü; üstelik `.git` de gitmişti. Kaybolan v10.39/v10.40 kaynakları — `AliskanlikMola`(+#45 Kural21), `AliskanlikNot`(#46), `SeriAnaliz`(#52), Store/HabitsFragment yamaları — sözleşmeler (mevcut testler + prefs anahtarları + dize adları) birebir korunarak **yeniden yazıldı**. gofile indirmesi `error-notPremium` verdiği için zip'ten geri alınamadı; rekonstrüksiyon testlerle doğrulandı. Git sigortası yeniden kuruldu (`febcc3a`).

## Ölçümler
- Derleme: **tek tur yeşil, 14dk04** · EXIT=0
- Testler: **731 test · 0 hata** (+14 yeni test)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔)
- APK md5: `439485c28f52c07bce5104ba4a15436d`

## Bilinçli sınırlar / dürüstlük notları
- Bildirimden Bitir'de ≥8 saatlik dev oturumlar ekranın "emin misin?" sorusu **sorulmadan** kaydedilir (kurtarma önceliği). Ekranda davranış değişmedi.
- Rekonstrüksiyon dosyalar davranış eşdeğeridir; kaybolan orijinallerle metin birebirliği kanıtlanamaz (test ve API sözleşmesi düzeyinde kanıtlı).
- Cihaz doğrulaması her zamanki gibi sizde: bildirim aksiyonları gerçek cihazda denenmelidir (kod derleme+JVM düzeyinde doğrulandı).

## Sıradaki (7 maddelik listenin kalanı)
- **v10.42:** Madde 5 (sabah "uyandın mı" + günün görevleri bildirimi, ekran kilidi açılışından) + Madde 6 (akşam "yarın ne yapmak istersin?" bildirimi).
- **v10.43:** Madde 3 (kendi odak sesin + ezan sesi ekleme, dosyadan seçim) + Madde 2'nin kalanı (yerleşim).
- **v10.44+:** Madde 4 (konu alt maddelerini elle sıralama) + Madde 7 (zamanlayıcı mini mod / PiP).
