# Günlük Asistan — Sürüm 11.08 (versionCode 264) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: Ana Ekran Yatay Tam Genişlik Koruması & Evrensel Sekmeler Arası Veri / Kart Taşıma Motoru

Kullanıcının paylaştığı ekran görüntüsü (`Screenshot_20260812_012734_Gnlk Asistan.jpg`) ve **"Ana sayfadaki en bastaki gunu vb seyleri gösteren sey küçüldü yanlardan onu düzelt ve ana ekran, bugun,konular ilerleme gibi sekmelerden verileri veya komple icindekileri tek tek taşıyamıyorum onu hallet."** talimatı doğrultusunda hem arayüzdeki yatay daralma sorunu giderilmiş hem de 4 ana sekme (`Ana Ekran`, `Bugün`, `Konular`, `İlerleme`) arasında evrensel veri/kart taşıma altyapısı kurularak devreye alınmıştır.

---

## 📱 Sürüm 11.08'de Yapılan İyileştirmeler ve Yenilikler

### 1. 🎯 Ana Sayfa "Günü Gösteren Şey" (Hero Kartı) Yatay Küçülme Düzeltmesi
- **Sorun:** Önceki sürümde kart boyutu ölçeği (%85 Kompakt vb.) uygulandığında `blokHero.scaleX = olcek` satırı kartı yatay eksende de küçülterek sağ ve sol kenarlardan boşluk bırakılmasına ("yanlardan küçülmesine") sebep oluyordu.
- **Kesin Çözüm (`HomeFragment.kt`, `SekmeVeVeriTasimaMotoru.kt`):**
  - Tüm ana ekran kartlarında ve özellikle `blokHero` kartında **`scaleX = 1.0f` tam genişlik oranı kalıcı olarak kilitlendi**.
  - Artık kullanıcı kart boyutunu değiştirdiğinde kart sağ ve sol kenarlardan asla daralmaz; ekranı boydan boya tam genişlikte (`MATCH_PARENT`) kaplayarak zarif görünümünü korur. Dikey kompaktlık ise yalnızca iç boşluklar (`paddingTop/Bottom`) üzerinden ayarlanır.

### 2. 🔀 Ana Ekran, Bugün, Konular, İlerleme ve Plan Arası Evrensel Taşıma Motoru
- **Evrensel Taşıma Menüsü (`SekmeVeVeriTasimaMotoru.sekmeArasiTasimaDiyalogu`):**
  - `Ana Ekran (HomeFragment)`, `Bugün / Günün Akışı (TodayFragment)`, `Konular (TopicsFragment)` ve `İlerleme (ProgressFragment)` sekmelerindeki **herhangi bir karta, başlığa veya içeriğe basılı tutulduğunda** taşıma menüsü açılır.
- **3 Esnek Taşıma Yöntemi:**
  - `📦 Komple İçindekileri Taşı (Tüm Kartı / Bölümü Aktar)` — Seçilen bölümü tüm içeriğiyle birlikte hedef sekmeye aktarır.
  - `⚡ Tek Tek Veriyi Taşı (Seçili İçerik / Maddeyi Aktar)` — Yalnızca seçilen belirli bir maddeyi, görevi, konuyu veya notu hedef sekmeye taşır.
  - `➕ Hedef Sekmeye Kopyala (Burada da kalsın)` — Veriyi hem mevcut sekmede tutar hem de hedef sekmeye kopyalar.

### 3. 📦 Hedef Sekmede Taşınan İçerik Kartı (`sekmeTasinanVerileriCiz`)
- Bir sekmeye (`Ana Ekran`, `Bugün`, `Konular`, `İlerleme` veya `Plan`) başka bir sekmeden veri veya kart taşındığında, hedef sekmenin en üstünde **"📦 Diğer Sekmelerden Taşınan Veriler"** kartı belirir.
- Her maddede verinin hangi sekmeden taşındığı (`[☀️ Bugün'den]: ...` vb.) belirtilir ve yanındaki **"✖ Kaldır"** butonuyla taşınan veri tek dokunuşla geri alınabilir.

---

## 🧪 Kalite ve Test Güvencesi
- **1.609 Saf JVM Birim Testi (%100 Başarı, 0 Hata — YENİ REKOR):** `SekmeVeVeriTasimaTest.kt` adında yeni bir test sınıfı ile 20 saf JVM testi yazıldı (sekmeler arası tek tek veya komple içerik taşıma/kopyalama, yatay küçülme önleyici `scaleX = 1.0f` güvencesi, sekmeler arası filtreleme, Türkçe sekme isimleri çevirisi, benzersiz ID üretimi). Toplam **116 test sınıfında 1.609 testin tamamı (%100 başarı, 0 hata, 0 başarısız)** temiz derlemeyle (`EXIT=0`) geçti.
- **Mimari Koruma Testleri:** Hiçbir XML layout dosyasında sert kodlanmış (`hardcoded`) `textSize` veya `cardCornerRadius` kullanılmamış; `TasarimOlcegiTest`, `RippleTutarlilikTest`, `GorunumAtolyeTest` ve `AnaEkranButonTest` testlerinden 0 hata ile geçildi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **Kurulabilir APK:** `~/GunlukAsistan-v11.08.apk` (v5.0 SHA-256 imza anahtarıyla %100 uyumlu, üzerine güncellenebilir).
2. **Tam Kaynak Kod Yedeği:** `~/kaynak-v11.08-yedek.zip`.
3. **Proje Durum Raporu:** `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md`.
4. **Müstakil Sürüm Notları:** `~/GunlukAsistan-v11.08-notlar.md`.
