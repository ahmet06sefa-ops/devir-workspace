# Günlük Asistan v10.38 (kod 194) — Sürüm Notları · Dalga 13

Bu dalgada üç katalog maddesi teslim edildi: **#13**, **#18**, **#25** (28/1000 ✔).

## 📌 #13 — "Bugün bitecek" görevler üstte sabitleme seçeneği
- Görev ekranı başlığındaki **📌 Bugün** düğmesine dokun → seçenek açılır/kapanır (tercih kalıcı).
- Açıkken: vadesi **bugün dolan** ve **vadesi geçmiş** görevler listenin en üstüne sabitlenir (tamamlananlar her zamanki gibi alta iner; tarihsiz görevler asla bölüme girmez).
- Başlık altında bölüm şeridi belirir: `📌 Bugün bitecek: N görev`.
- Teknik: mevcut sıralama karşılaştırıcısına ek öncelik katmanı — veri modeline dokunulmadı.

## 🔁 #18 — Haftalık "en çok ertelenen görev" tespiti ve uyarısı
- Ertelemeler sayılıyor: alarm ekranındaki ⏰ Ertele, toplu seçimden yarına/takvimle ileri taşıma, gecikmiş görev diyaloğundan ileri taşıma (bugüne almak erteleme sayılmaz).
- Haftada **3+ kez** ertelenen görev varsa görev ekranının üstünde uyarı şeridi çıkar: `🔁 Bu hafta en çok ertelenen: «görev» (N kez)`.
- Sayaç **ISO haftasına** göredir; yeni haftada kendiliğinden sıfırlanır. Görev silinirse/arşivlenirse listeden düşer.

## ⏰ #25 — Nota hatırlatıcı bağlama
- Not düzenleme penceresinde yeni **⏰ Hatırlatıcı** butonu (🕘 Damga'nın yanında).
- Tarih + saat seç → o anda "⏰ Not hatırlatıcısı" bildirimi gelir; dokununca uygulama açılır.
- Kurulu hatırlatıcı butonda görünür (`⏰ 12 Ağu 09:30`); tekrar dokununca **Kaldır / Yeniden kur**.
- Geçmiş saat seçimi engellenir; cihaz yeniden başlatılırsa gelecekteki hatırlatıcılar boot sonrası otomatik tekrar kurulur (geçmiş kayıtlar ayıklanır).
- Yeni notta önce kaydetmen gerekir (buton uyarır).

## 📌 Dürüstlük notları
- Sandbox'ta cihaz testi yapamıyorum: derleme + **712 JVM testi yeşil** ✔; bildirim/alarm davranışını cihazında bir kez doğrula (Android 12+'da "tam alarm" izni kapalıysa sistem yaklaşık saat kurar — Sağlık Kontrolü ekranı bunu da raporlar).
- #18 sayacı yerel cihazdadır; uygulama verisi sıfırlanırsa sayaç da sıfırlanır.

## 🔴 Dürüst kayıt (bu dalgada 1 hatam oldu)
İlk derlemede `goreviErtele` fonksiyonunun parametre adını `hedef` varsaydım — gerçek adı `hedefMs`'miş, derleyici yakaladı. **Bu benim hatamdır**; düzeltip ikinci turda yeşil aldım. Kırmızı tur hiz-log'a işlendi.

## ✅ Doğrulama
| Kalem | Değer |
|-------|-------|
| Derleme | BUILD SUCCESSFUL 12m23s (1🔴 düzeltme sonrası) |
| Test | 712 JVM testi · 0 hata |
| Sürüm | versionCode 194 · versionName 10.38 · minSdk 24 |
| İmza SHA-256 | `5f15d4e781c3afd1e5a1288f4509d4afd8e42a5761bc561184831f7bde348511` (değişmedi ✔) |
| APK md5 | `58728b1b0b554023a01ba9bfc9c0285d` |

## ⏭ Sonraki dalga (v10.39) adayları
#42 alışkanlık mola modu (odak dalgası), #14 görev-detay not bağlama veya eğitim/bütçe kategorilerinden yeni maddeler — önce varlık doğrulamasıyla.
