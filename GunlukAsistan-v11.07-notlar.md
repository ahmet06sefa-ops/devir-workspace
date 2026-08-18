# Günlük Asistan — Sürüm 11.07 (versionCode 263) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: İlk Açılış Ekranı Seçimi, Detaylı Görev Düzenleme & Basılı Tutarak Tam Otonom Yönetim

Kullanıcının **"İlk acildiginda ana ekran degilde baska ekran gözüksün mesela bugün ekrani gözüksün ayarlardan ayarlayabileyim ve gorevler sekmesinde yazdiklarimi sonradan duzenleyebileyim detaylica. Alarm saat vb seyler koymadan devam edebiliyim saat sorununu çöz. Günün akisi yerini vb yerlerin boyutlarini ayarlayabileyim. Yerini basılı tutarak degistirebileyom uygulamadaki herseyin. Sekmeler arasi tablolari basılı tutarak yer değişikliği yapabileyim."** talimatı doğrultusunda Günlük Asistan arayüz ve yönetim ekosistemi %100 özelleştirilebilir, esnek ve otonom bir yapıya kavuşturulmuştur.

---

## 📱 Sürüm 11.07'de Yapılan Yenilikler ve İyileştirmeler

### 1. 🚀 İlk Açılış Ekranı Seçimi ("Mesela Bugün Ekranı Gözüksün")
- **Yedi Farklı Ekran Seçeneği:** Önceki sürümlerde uygulama her zaman `0 -> Ana Ekran` ile açılırken, artık Ayarlar ekranındaki **"🚀 İlk Açılış Ekranı (Varsayılan Sekme)"** menüsünden dilediğiniz ekranı açılış ekranı olarak atayabilirsiniz.
- **Desteklenen Ekranlar:**
  - `☀️ Bugün / Günün Akışı (TodayFragment)` — Kullanıcının talep ettiği varsayılan gün akışı ekranı.
  - `🏠 Ana Ekran (Varsayılan)`
  - `✅ Görevler (TasksFragment)`
  - `⏱️ Sayaç / Zamanlayıcı (TimerFragment)`
  - `📋 Vakit Planı (PlanFragment)`
  - `📊 İlerleme (ProgressFragment)`
  - `🤖 Asistan (AsistanFragment)`

### 2. ✏️ Görevleri Sonradan Detaylıca Düzenleme & Alarmsız / Saatsiz Kayıt
- **Detaylı Görev Düzenleme (`showTaskEditor(task)`):** Görevler sekmesine yazdığınız herhangi bir görevin metnine dokunulduğunda (`holder.itemView.setOnClickListener` / `holder.text.setOnClickListener`) görev editörü açılır ve görevin başlığı, tarihi, tekrar periyodu önceden yüklü şekilde gelir.
- **Alarm / Saat Zorunluluğunun Kaldırılması ("Saat Sorunu Çözüldü"):** Önceki sürümlerde tekrarlı bir görev eklendiğinde veya saat seçilmediğinde sistem zorla saat ataması yapıp alarm kuruyordu. Artık:
  - Hiçbir alarm veya saat seçilmesine gerek olmadan (`dueAt = 0L`), yalnızca görev başlığıyla veya tekrarlı olarak görev eklenebilir ve güncellenebilir.
  - Saat ve alarm tamamen isteğe bağlı (`opsiyonel`) hale getirilmiştir.

### 3. 📐 Günün Akışı ve Kart Boyutlarını Ayarlama ("Boyut Ölçeği")
- **4 Kademeli Kart Boyutu Özelleştirmesi (`GorunumAyar.kartBoyutuOlcegi`):** Ana ekrandaki "☀️ Günün Akışı" kartı ve diğer tüm blokların dikey/yatay ölçek boyutu ayarlanabilir yapıldı:
  - `%85 Kompakt`
  - `%100 Normal`
  - `%115 Geniş`
  - `%130 Devasa`
- Bu ayara hem ana ekrandaki kartlara uzun basarak hem de ayarlar menüsünden erişilebilir.

### 4. 🔀 Basılı Tutarak Her Şeyin Yerini Değiştirme & Sekmeler Arası Tablo Taşıma
- **Ana Ekranda Basılı Tutarak Sıra Değiştirme:** `☀️ Günün Akışı`, `💡 Motivasyon Manşeti`, `🎓 Kurslar`, `📊 İstatistikler` vb. tüm ana ekran kartlarına basılı tutulduğunda (long press) **"🔀 Kart Yönetimi — Sıra & Boyut Ayarı"** menüsü açılır; kartları yukarı veya aşağı taşıyıp kendi sıralamanızı kurgulayabilirsiniz (`anaEkranSiralama`).
- **Görevler ile Sekmeler Arası Tablo Yer Değişikliği (`gorevUzuMenu`):** Görevler sekmesindeki herhangi bir göreve basılı tutulduğunda açılan menü üzerinden:
  - Görevi listede yukarı veya aşağı taşıyabilirsiniz.
  - **"🏷️ Etiket / Kategori Tablosuna Taşı (Sekmeler Arası Değişiklik)"** seçeneğiyle görevi `💼 İş`, `🏠 Kişisel`, `📚 Ders & Eğitim`, `🚀 Proje` vb. sekmeler/kategoriler arasında anında taşıyabilirsiniz.
  - Görevi `⏳ Bekliyor` tablosuna aktarabilir veya oradan çıkarabilirsiniz.

---

## 🧪 Kalite ve Test Güvencesi
- **1.589 Saf JVM Birim Testi (%100 Başarı, 0 Hata — YENİ REKOR):** `AcilisVeGorevDuzenlemeTest.kt` adında yeni bir test sınıfı ile 15 saf JVM testi yazıldı (Açılış ekranı endeksleme 0-6, alarmsız görev kaydı, görev yerinde düzenleme, kart boyutu yüzdeleri, ana ekran virgüle dayalı sıra çözümleme, sekmeler arası etiket ve kategori taşıma). Toplam **115 test sınıfında 1.589 testin tamamı (%100 başarı, 0 hata, 0 başarısız)** temiz derlemeyle (`EXIT=0`) geçti.
- **Mimari Koruma Testleri:** Hiçbir XML layout dosyasında sert kodlanmış (`hardcoded`) `textSize` veya `cardCornerRadius` kullanılmamış; `TasarimOlcegiTest`, `RippleTutarlilikTest`, `GorunumAtolyeTest` ve `AnaEkranButonTest` testlerinden 0 hata ile geçildi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **Kurulabilir APK:** `~/GunlukAsistan-v11.07.apk` (v5.0 SHA-256 imza anahtarıyla %100 uyumlu, üzerine güncellenebilir).
2. **Tam Kaynak Kod Yedeği:** `~/kaynak-v11.07-yedek.zip`.
3. **Proje Durum Raporu:** `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md`.
4. **Müstakil Sürüm Notları:** `~/GunlukAsistan-v11.07-notlar.md`.
