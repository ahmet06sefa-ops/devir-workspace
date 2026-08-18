# Günlük Asistan v10.40 (kod 196) — Sürüm Notları · Dalga 15

Bu dalgada iki katalog maddesi teslim edildi: **#46** ve **#52** (32/1000 ✔).

## 📝 #46 — Alışkanlık gün notu ("bugün neden olmadı?")
- Alışkanlık satırına **uzun bas** → o güne not yaz: "Yağmur vardı, yürüyemedim" gibi.
- Not varsa satır alt metninde **📝** görünür; uzun basıp düzenleyebilir veya "Notu sil" ile kaldırabilirsin.
- Notlar cihazda tek JSON haritasında tutulur (alışkanlık → gün → not).

## 🏅 #52 — "En uzun ikinci seri" kaydı
- Alışkanlık düzenleme penceresinde seri arşivi: `🏅 En uzun seri: X gün · ikinci en uzun: Y gün`.
- Tüm geçmiş taranır (süren seri de sayılır); alışkanlığın ilk kayıtlı gününden başlar, 400 gün güvenlik sınırı var.
- Motivasyon: rekoru kırmak için ikinci rekorunu da görürsün.

## 🚨 Bu turun altyapı olayı (dürüst kayıt — ve çözümü)
Bu tur sandbox dosya anlık-görüntüsünden **3 v10.39 kaynak dosyası eksik döndü** (Store.kt mola yaması, HabitsFragment mola/21-gün bağlantıları ve AliskanlikMola.kt; v10.39 zip'i de aynı eksiklikle yazılmış). **APK'lar etkilenmedi** — cihazındaki v10.39'da mola modu zaten çalışıyordu. Tespit edip üç parçayı birebir geri yazdım, **724 testle kanıtladım**. Kalıcı önlem olarak projeye **git sigortası** kuruldu: artık her dalga commit'leniyor (`7f2b692` → `9357020`); sonraki turlarda sessiz kayıp `git status` ile anında yakalanacak.
*(v10.39 zip linki tarihsel olarak duruyor; o paketen AliskanlikMola.kt eksiktir — güncel ve bütün kaynak v10.40 zip'indedir.)*

## 🔴 Dürüst kayıt (derleme hatası)
İlk v10.40 yazımında bir XML-komşusu ankörü eşleşmedi, script güvenlikli şekilde durdu (yarım yazım olmadı); kurtarma sırasında neden de ortaya çıktı. Sonrasında tek turda derleme yeşil.

## ✅ Doğrulama
| Kalem | Değer |
|-------|-------|
| Derleme | BUILD SUCCESSFUL 14m28s (kurtarma sonrası tek tur yeşil) |
| Test | 724 JVM testi · 0 hata |
| Sürüm | versionCode 196 · versionName 10.40 · minSdk 24 |
| İmza SHA-256 | `5f15d4e781c3afd1e5a1288f4509d4afd8e42a5761bc561184831f7bde348511` (değişmedi ✔) |
| APK md5 | `4b2e9361e28dc3dee147f182cdcd27eb` |
| Kaynak sigortası | git commit `9357020` |

## ⏭ Sonraki dalga (v10.41) adayları
#43 alışkanlık başına hatırlatma saati, #44 yıllık ısı haritası, #48 elle sıralama — ya da bütçe/eğitim kanatları. Varlık doğrulaması `git status` ile başlayacak.
