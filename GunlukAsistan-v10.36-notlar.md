# Günlük Asistan v10.36 (code 192) — Sürüm Notları

**Tarih:** 2026-08-09 · **Dalga:** 1000-öneri kataloğu, dalga 11 — görev ekranı güçlendi

## Yenilikler

### 1. Katalog #19 — Görev listesi yoğunluğu ▦
- Notlardaki kanıtlanmış desen (v10.35) görevlere taşındı: başlık yanındaki
  geçişle **Kart ⇄ Kompakt**.
- Kompaktta tek bakışta daha çok görev: daraltılmış kart, orta metin,
  2 satırla sınırlı; tarih rozeti, ⚠️ gecikme dokunuşu, etiket seridi,
  seçim vurgusu — hepsi aynı id'lerle birebir çalışır.
- Tercih kalıcı (`gorev_gorunum_v1`, yedekte).

### 2. Katalog #16 — "Bekliyor" durumu ⏳
- Çoklu seçim şeridine **"⏳ Bekliyor"** düğmesi: başkasından dönüş
  beklenen görevleri tek dokunuşla işaretle (karışık seçimde tümü
  bekliyor yapılır; hepsi zaten işaretliyse topluca kaldırılır).
- Bekleyen görevler listede **⏳ rozetli** görünür — dikkat dağıtmadan
  "top sende değil" işareti.
- Mimari: modele dokunmadan ayrı tercih (`gorev_bekliyor_v1`, yedekte).

## Test
- Yeni: `GorevBekliyorTest` — 2 test (rozet mantığı).
- Toplam: **695 JVM testi, 0 hata**.

## Bilinçli sınırlar
- "Bekliyor" bir görünüm bayrağıdır: bildirim zamanlamasını ve tekrar
  kuralını değiştirmez; tamamlanınca otomatik kalkmaz (seçip kaldırılırsa
  temizlenir).
