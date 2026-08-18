# Günlük Asistan v10.42 (kod 198) — Sürüm Notları

## Bu sürümde: kullanıcı maddeleri #5 + #6

### 5️⃣ Madde 5 — Sabah "Uyandın mı?" + bugünün görevleri
- Günün **ilk ekran kilidi açılışı** uyanma sayılır (gece 04:00 öncesi kilit açmalar elenir — yarı uykulu kontrol bildirim çıkarmaz).
- Günde **en fazla bir kez**, saat kaç olursa olsun: "🌅 Uyandın mı? Günaydın!" başlığıyla **N görev bekliyor · bugün B · gecikmiş G** özeti + ilk görev başlıkları (InboxStyle genişletilebilir).
- Dokununca uygulama açılır. Bildirim izni yoksa sessizce pas.

### 6️⃣ Madde 6 — Akşam "Yarın ne yapmak istersin?" sorusu
- Her akşam **22:00** (varsayılan) tek atımlık RTC_WAKEUP alarmı; tetiklenince bildirim ve kendini ertesi güne yeniden kurar — cihaz yeniden başlasa bile (BootReceiver kancası) kaybolmaz.
- Bildirim: "🌙 Yarın ne yapmak istersin? — Uyumadan önce yarının görevlerini yaz, sabah hazır uyan."

### ⚙️ Yeni ayarlar (Ayarlar → Genel bölümü, senkron satırının üstü)
- **🌅 Sabah planı bildirimi** — dokun: Açık ✓ ⇄ Kapalı
- **🌙 Akşam planı sorusu** — dokun: Açık ⇄ Kapalı · **uzun basış: saat seçimi** (20:00 / 21:00 / 22:00 / 23:00)

### 🔧 Ortam notu (3. snapshot kaybı — dürüstlük)
Tur başında anlık görüntü yine v10.41 **öncesine** dönmüştü; bu kez `kaynak-v10.41-yedek.zip` yerinde duruyordu — v10.41 **birebir zipten** geri yüklendi, git sigortası yeniden kuruldu (`fa8e061`), üstüne v10.42 eklendi (`544957b`).

## Ölçümler
- Derleme: **tek tur yeşil, 13dk** · EXIT=0
- Testler: **737 test · 0 hata** (+6 yeni test: PlanAsistanTest)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔)
- APK md5: `a2c434674cef19819b658af586eead94`

## Bilinçli sınırlar / dürüstlük notları
- "İlk kilit açılışı = uyandım" bir yaklaşıktır; telefonu hiç kapatmadan kalkan kullanıcıda bildirim ilk açılışa düşer (istenilen davranış buydu). Öğle/akşam ilk açılan günlerde de gösterir — günde bir kezle sınırlıdır.
- Akşam bildirimi `setExactAndAllowWhileIdle` (izin varsa) yoksa `setAndAllowWhileIdle` — izinsiz cihazda ±birkaç dakika kayma olabilir.
- Akşam bildirimine dokunmak uygulamayı açar (görev ekleme diyaloğuna doğrudan bağlantı bu dalgada yok — v10.43+).
- Cihaz doğrulaması sizde: sabah bildirimi gerçek kilit açılışında, akşam sorusu gerçek saatte denenmelidir.

## Sıradaki dalgalar (7 maddelik listenin kalanı)
- **v10.43:** Madde 3 — kendi odak sesini dosyadan ekleme + ezan bildirim sesi için aynı sistem.
- **v10.44:** Madde 4 — konu alt maddelerini elle sıralama (▲▼).
- **v10.45:** Madde 7 — zamanlayıcı mini mod (PiP) ve madde 2'nin kalanı (yerleşim ayarları).
