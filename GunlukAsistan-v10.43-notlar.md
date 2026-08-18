# Günlük Asistan v10.43 (kod 199) — Sürüm Notları

## Bu sürümde: kullanıcı maddesi #3 (kendi seslerini ekleme)

### 3️⃣ Odak sesleri → kendi dosyanı ekle 📂
**Şikayet:** "Odak sesleri gerçekçi değil; benim ekleyebileceğim yer olsun; sesi tek tek."
- Zamanlayıcı Ayarları → **Ortam sesi** diyaloğunun sonuna **"📂 Kendi sesim… (dosyadan seç)"** satırı eklendi.
- Sistem ses seçici açılır (Zil sesleri + Dosyalarım entegresi); seçtiğin dosya **döngüsel odak sesi** olarak yerleşir, kart `"📂 Kendi sesim: <ad>"` etiketiyle görünür.
- Seçim kodu 900 ile ayrışır; dosya yoksa seçim güvenle "seçilmedi"ye düşer (saf kuralla birim testli).
- Teknik: `SesManzarasi.cal()` artık çift kaynaklı — paket raw'ları veya `content://` URI; mola kısması (%25 ses) özel seste de çalışır.

### 🕌 Ezan sesi → zaten vardı, kapı görünür oldu
**Buluş (dürüstlük):** Ezan/vakit bildirim sesi seçimi önceden uygulanmıştı (`NamazBildirim.setSes` + kanal sürüm yenileme + `NamazAyarActivity`'de sistem ses seçici). Ama yolu bulunamıyordu.
- Zamanlayıcı Ayarları'na **"Ezan bildirim sesi"** kısayol satırı eklendi — seçili adı gösterir, dokununca Vakit ayarlarına götürür (tek yetkili ekran orası; çift kaynak olmaması için bilinçli yönlendirme).

## Ölçümler
- Derleme: **tek tur yeşil** · EXIT=0 (ilk iki deneme ortamdaki gradle kaybına takıldı → tam kurulum, 🔴 ortam)
- Testler: **740 test · 0 hata** (+3 yeni test)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔)
- APK md5: `67a002d2ec41ece89e9cc99be8316145`

## Bilinçli sınırlar / dürüstlük notları
- "Sesleri tek tek (her slota) ayrı dosya" yerine TEK özel slot açıldı: 8 hazır manzara + 1 kendi dosyan. Çoklu özel slot listesi v10.44+.
- Özel ses çalarken sayaç ekranındaki 8 hazır karttan hiçbiri boyanmaz (kartlar sabit listeye aittir); seçim/etiket doğru görünür.
- Sistem ses seçicide "Dosyalar" sekmesi cihaz üreticisine göre değişir; bazı cihazlarda önce zil listesi açılır, "Başka uygulama ile seç" ile dosyaya gidilir.
- Cihaz doğrulaması sizde: seçtiğin dosyanın uzun süre döngüsel çaldığını telefonda dinle.

## Sıradaki dalgalar
- **v10.44:** Madde 4 — konu alt maddelerini elle sıralama (▲▼ düğmeleri + kalıcı düzen).
- **v10.45:** Madde 7 — zamanlayıcı mini mod (PiP) + madde 2'nin kalanı (yerleşim ayarları).
