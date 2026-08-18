# Günlük Asistan v10.44 (kod 200) — Sürüm Notları

## Bu sürümde: kullanıcı maddesi #4 (konu alt maddeleri elle sıralama)

### 4️⃣ Alt madde ▲▼ düğmeleri
**Şikayet:** "Konular kısmındaki konulardaki alt maddelerin yerlerini ben ayarlayabileyim."
- Konuyu genişlet → her alt madde satırında ✓ / metin / **▲** / **▼** / ✕ dizilimi.
- ▲ bir üste, ▼ bir alta taşır; **ilk maddede ▲, son maddede ▼ soluk** görünür (tıklansa da bir şey olmaz — sınır koruması testli).
- Sıra anında `Store.saveTopics` ile kalıcı yazılır; liste `ListeFark` diff animasyonuyla yer değiştirir (ekran donmadan).
- Ağaç çizgisi (├/└), işaret durumu, tekrar programı — hepsi taşıma sonrası doğru rebind edilir.
- Taşıma çekirdeği `ListeTasi.kt` saf fonksiyonları (JVM testli: 4 yeni test, 744 toplam).

## Ölçümler
- Derleme: **tek tur yeşil, ~13.5 dk** · EXIT=0 (bu tur derleme doğrudan kabukta koştu)
- Testler: **744 test · 0 hata**
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔)
- APK md5: `b9bc39f721add23515eed5cde3ca262a`

## Bilinçli sınırlar / dürüstlük notları
- Sürükle-bırak (drag) yerine bilinçli olarak **▲▼ düğmesi**: `AgacCizgiView` + tek-bas tik/uzun-bas menüsü ile jest çakışması riskini sıfırlar; isterseniz v10.46+ dalgada drag eklenebilir.
- Konu başlıkları (üst düzey) sıralaması bu madde kapsamı dışında — sadece ALT MADDELER. Üst düzey sıralama isterseniz sıradaki dalgalardan birine konur.
- Cihaz doğrulaması sizde: 2-3 maddeli bir konuda ▲▼ ile oynayıp uygulamayı kapat/aç — sıra korunmalı.

## Sıradaki dalga
- **v10.45:** Madde 7 — zamanlayıcı mini mod (PiP: sayaç çalışırken uygulama küçük pencereye geçer, başka işlerinizi halledersiniz) + madde 2'nin kalanı (yerleşim ayarları).
