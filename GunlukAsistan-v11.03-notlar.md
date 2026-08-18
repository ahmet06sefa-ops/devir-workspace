# Günlük Asistan — Sürüm 11.03 (versionCode 259) Sürüm Notları
_Yayın Tarihi: 11 Ağustos 2026 · Ankara, Türkiye_
_Yapay Zekâ Geliştirici Ekibi · Arena.ai Agent Mode_

---

## 🌟 Sürümün Ana Teması: YouTube Çevrimdışı Oynatma Listeleri Profesyonel Kaydırma Jestleri & Sekmeler Arası Hızlı Geçiş

Kullanıcının **"Çevrimdışı YouTube Oynatma Listeleri & Video Sıralayıcı sekmesini temaya uygun yap ve videolarin aciklama yazilarini kucult ve kaydirilabilir sekilde yap. Videoyu silmek için sola tasimak icin saga kaydirma hareketini kullandirma ekle. Videoyu tasirken basili tut ve sekmeler arasi gecis yapabileyim. Daha profesyonel bir liste olsun."** talimatı doğrultusunda, YouTube Oynatma Listeleri ve Video Sıralayıcı ekranı (`YoutubePlaylistActivity.kt`, `activity_youtube_playlist.xml`, `item_youtube_video.xml`) **100% Günlük Asistan tasarım diline uygun**, modern, jest destekli ve **RecyclerView + ItemTouchHelper** altyapısına sahip profesyonel bir video yönetim merkezine dönüştürülmüştür.

---

## 📱 Sürüm 11.03'te Yenilenen ve Eklenen Özellikler

### 1. 🎨 Temaya Uygun Modern Tasarım & Profesyonel Görsel Arayüz
- **Günlük Asistan Tasarım Dili:** Liste ekranı, üst bar ve çip seçiciler uygulamanın ana renk paletine (`?attr/colorPrimary`, `?attr/colorSurfaceVariant`, `@drawable/g_card`) entegre edilmiştir.
- **Tasarım Ölçeği Uyumlu Layoutlar:** Hiçbir sert kodlanmış (`hardcoded`) köşe yarıçapı veya punto boyutu kullanılmadan, tamamen `@dimen/ga_kose_*` ve `@dimen/ga_yazi_*` mimarisine sadık kalınmıştır.
- **Bilgilendirici Jest İpucu Şeridi:** Ekranın üst kısmına kullanıcıyı jestlerle yönlendiren şık bir rehber çubuğu (`layoutJestBilgi`) eklenmiştir:
  `"💡 İPUCU: ← Sola Kaydır: Sil | Sağa Kaydır →: Grubu Değiştir / Taşı | Basılı Tut: Sekmeler Arası Hızlı Geçiş & Taşıma"`

### 2. 🔍 Küçültülmüş ve Yatay Kaydırılabilir Video Açıklamaları (`txtVideoAciklama`)
- **Daha Minimalist Punto (@dimen/ga_yazi_mini):** Video satırlarındaki etiket ve detay açıklamalarının punto boyutu küçültülerek 11sp (`@dimen/ga_yazi_mini`) yapıldı.
- **Yatay Kayan Yazı / Marquee (`isSelected = true`):** Hem başlık hem de açıklama alanları tek satır (`singleLine="true"`), kaydırılabilir (`scrollHorizontally="true"`) ve otomatik akan kayar yazı (`ellipsize="marquee"`, `marqueeRepeatLimit="marquee_forever"`) hale getirildi. Böylece çok uzun açıklamalar dahi taşmadan, akıcı bir şekilde eksiksiz okunabilmektedir.

### 3. 👆 Profesyonel Kaydırma Hareketleri (Swipe-to-Delete & Swipe-to-Move)
- **← Sola Kaydırma ile Silme (ItemTouchHelper.LEFT):** Herhangi bir videoyu sola kaydırdığınızda video oynatma listesinden anında kaldırılır (`videoyuKaldir`) ve kalan videoların `#1, #2...` sıralaması otomatik olarak yeniden hesaplanır.
- **Sağa Kaydırma ile Taşıma → (ItemTouchHelper.RIGHT):** Herhangi bir videoyu sağa kaydırdığınızda videoyu başka bir kampa taşıma / kopyalama menüsü açılır.
- **Özel Arka Plan Animasyonları (`onChildDraw`):** Sola kaydırdığınızda kırmızı bir arka plan üzerinde `"🗑️ Sil / Kaldır"`, sağa kaydırdığınızda mor / tema rengi bir arka plan üzerinde `"🔀 Grubu Değiştir / Taşı →"` metni ve ikonu belirir.
- **↕️ Sürükle-Bırak Sıra Değiştirme (UP/DOWN):** Liste içerisindeki herhangi bir videoyu yukarı-aşağı sürükleyerek sırasını değiştirebilirsiniz.

### 4. ⚡ Basılı Tutarak Sekmeler Arası Hızlı Geçiş ve Video Taşıma
- **Sekmeler Arası Hızlı Taşıma Modu (`sekmelerArasiTasiVeGecisDiyalogu`):** Herhangi bir videoya uzun basılı tuttuğunuzda ("Basılı Tut"), videoyu üstteki diğer kamp sekmelerinden (Örn: Matematik Kampı, Tarih Kampı, Diğer Yerel Videolar vb.) birine taşıyabileceğiniz özel geçiş menüsü açılır.
- **Anında Sekme Değişimi & Odaklanma:** İstenen hedef sekme seçildiği anda video oraya taşınmakla kalmaz, uygulama otomatik olarak **hedef sekmeye geçiş yaparak odaklanır** (`playlistiYukle(hedef.id)`).

---

## 🧪 Kalite ve Test Güvencesi
- **1.529 Saf JVM Birim Testi (%100 Başarı, 0 Hata):** `YoutubePlaylistTest.kt` içerisine v11.03'e özel 4 yeni test eklendi (`sekmeler arasi tasi ve gecis yap`, `videolarin sirasini degistir`, `video kopyalama ve tasima isleminde sure ile aciklama metinleri eksiksiz aktarilir`, `jest bilgilendirme metni ve ipucu tasimayi icerir`).
- **Tasarım Ölçeği ve Ripple Tutarlılık Testleri (%100 Geçerli):** `TasarimOlcegiTest`, `RippleTutarlilikTest` ve 18 atölye butonlu `AnaEkranButonTest` testlerinden 0 hata ile geçildi.

---

## 📦 Teslimat Paketi (4 Parça Kuralı)
1. **Kurulabilir APK:** `~/GunlukAsistan-v11.03.apk` (v5.0 SHA-256 ile imzalama anahtarı uyumlu, doğrudan güncellenebilir).
2. **Tam Kaynak Kod Yedeği:** `~/kaynak-v11.03-yedek.zip`.
3. **Proje Durum Raporu:** `~/PROJE-DURUM.md` ve `~/uploads/PROJE-DURUM.md`.
4. **Müstakil Sürüm Notları:** `~/GunlukAsistan-v11.03-notlar.md`.
