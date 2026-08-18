# Günlük Asistan v10.45 (kod 201) — Sürüm Notları

## Bu sürümde: kullanıcı maddesi #7 (zamanlayıcı mini mod)

### 7️⃣ ▦ Mini mod — sayacı küçük pencereye al
**Şikayet:** "Zamanlayıcı açıkken uygulama mini uygulama hâline gelebilsin, diğer taraftan başka işlerimi halledebileyim."
- Zamanlayıcı ekranında **nefes düğmesinin altında** yeni düğme: **"▦ Mini mod — sayacı küçük pencereye al"**.
- Dokununca sistem **Picture-in-Picture** penceresi açılır: ekranda yalnız **kadran + Başlat/Bitir düğmeleri** kalır; sen başka uygulamada gezinirken sayaç yüzen pencerede akmaya devam eder.
- Pencereye dokun → uygulama tam ekrana döner, tüm satırlar geri gelir.
- Dikey telefonda uzun pencere, yatayda 16:9 (saf oran kararı `MiniMod`, JVM testli).
- Geri sayım / kronometre / ileri sayım üçü de damga-temelli çalıştığından PiP'te saniye şaşmaz; ileri sayımın canlı bildirimi de zaten v10.41'den beri açık.
- Cihaz/sürüm desteklemezse (API < 26 veya özellik yok) kibar uyarı çıkar, çökme olmaz.

## Ölçümler
- Derleme: **tek tur yeşil, ~13dk** · EXIT=0
- Testler: **748 test · 0 hata** (+4 yeni test)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔)
- APK md5: `9a48b92c52a88e2dbbc7fff676826d82`

## Bilinçli sınırlar / dürüstlük notları
- PiP penceresinde hazır süre çipleri/preset satırı gizlenir (yer dar); süre seç için tam ekrana dön. Nadiren tick sırasında bir satır görünürlüğü titreşebilir — işlevsel değil, kozmetik (tam ekrana dönünce düzelir).
- PiP davranışı emülatörde değil ancak **gerçek cihazda** tam doğrulanabilir (kod derleme+test kanıtlı).
- 7 maddelik listenin tamamı artık uygulamada: #1 bildirim aksiyonları ✔ · #2 kadran yazı boyutu ✔ · #3 kendi sesin ✔ · #4 alt madde sıralama ✔ · #5 sabah planı ✔ · #6 akşam sorusu ✔ · #7 mini mod ✔. Madde 2'nin "her şeyin yerini tek tek ayarlama" kısmı tek büyük ertelenen iş — sıradaki dalgalara planlandı.

## Sıradaki
- **Katalog dalgalarına dönüş:** #43 alışkanlık başına hatırlatma saati, #44 yıllık ısı haritası, #48 elle sıralama (alışkanlık), #617 eğitim kanadı… ve madde 2'nin yerleşim atölyesi.
