# Günlük Asistan v10.39 (kod 195) — Sürüm Notları · Dalga 14 (Alışkanlık dalgası)

Bu dalgada iki katalog maddesi teslim edildi: **#42** ve **#45** (30/1000 ✔).

## 🏖 #42 — Alışkanlık mola modu (ertelenen odak maddesi, şimdi tamam)
- Alışkanlığı düzenleme penceresinde yeni buton: **🏖 Molaya al — seri donar**.
- Tatilde/hastalıkta molaya al: o günler seriyi **ne uzatır ne bozar** — `habitStreak` hesabında mola günleri atlanır; döndüğünde seri kaldığı yerden devam eder.
- **☀️ Moladan dön** deyince açık aralık kalıcı kümeye işlenir (`N gün kapatıldı, seri kaldığı yerden` bildirimi).
- Moladaki alışkanlığın satırında **🏖 rozeti** görünür.
- Teknik not: kırılgan seri motoruna tek satırlık davranış değişikliği — sadece atlama adımı eklendi; koruma sayacı (400) bozuk veride sonsuz döngüyü yine engelliyor. Haftalık mini şerit gerçek işaretleri göstermeye devam eder (bilinçli tercih: şerit "ne yaptığını", seri "zinciri" gösterir).

## 🎓 #45 — "21 gün kuralı" ilerleme çubuğu
- Her alışkanlık satırında, haftalık şeridin altında ince ilerleme çubuğu + alt metinde `21 gün kuralı: N/21` (21'i geçince `tamam 🎓`).
- Yüzde, mevcut kesintisiz seriden hesaplanır; mola günleri seriyi koruduğu için molada çubuk da korunur.

## 📌 Dürüstlük notları
- Sandbox'ta cihaz testi yapamıyorum: derleme + **717 JVM testi yeşil** ✔; mola davranışını cihazında bir alışkanlıkla dene (molaya al → seri satırı aynı kalmalı; 🏖 görünmeli). Sorun olursa ekran görüntüsüyle söyle → anında düzeltme garantisi.

## 🔴 Dürüst kayıt (bu dalgada 1 hatam oldu)
İlk derlemede KDoc yorumunda köşeli parantez `[başlangıç..bugün]` kullandım — Kotlin bunu geçersiz doc-link sanıp derlemeyi kesti. **Bu benim hatamdı**, yorumu düzeltip ikinci turda yeşil aldım. Kırmızı tur kayıtlara işlendi.

## ✅ Doğrulama
| Kalem | Değer |
|-------|-------|
| Derleme | BUILD SUCCESSFUL 12m03s (1🔴 düzeltme sonrası) |
| Test | 717 JVM testi · 0 hata |
| Sürüm | versionCode 195 · versionName 10.39 · minSdk 24 |
| İmza SHA-256 | `5f15d4e781c3afd1e5a1288f4509d4afd8e42a5761bc561184831f7bde348511` (değişmedi ✔) |
| APK md5 | `05c6bf8f360ff709d3395bff1a439009` |

## ⏭ Sonraki dalga (v10.40) adayları
Alışkanlık kanadında kalanlar: #43 alışkanlık başına hatırlatma saati (yeni alıcı gerekir — orta), #44 GitHub tarzı yıllık ısı haritası, #46 "neden yapamadın" notu; ya da bütçe/eğitim kanatlarına geçiş. Varlık doğrulamasıyla.
