# Günlük Asistan v10.53 (kod 209) — Sürüm Notları

## Bu sürümde: 32 Maddelik Tasarım ve Yerleşim Özelleştirme Atölyesi (#1..#32)
**Kullanıcı İsteği:** "Bana tasarımlarla manuel oynama değiştirme vb bir çok tasarım değişikliği yapabilmemi önerisi sun 30 maddede. (Hepsini yap ve daha fazlasını eklemek istersen onları da yap)"
- **Neden ihtiyaç vardı?** Kullanıcılar arayüzü yalnızca standart temalarla kullanmak yerine; her bir rengin, köşe yuvarlaklığının, fontun, boşluğun, kenarlığın, animasyonun ve yerleşimin kontrolünü %100 ellerine almak ve diledikleri gibi manuel oynayıp değiştirmek istedi.
- **Çözüm (`TasarimAtolye.kt` & `TasarimAtolyeActivity`):**
  1. **#1..#5 Renk, Vurgu ve Palet Atölyesi:** Serbest Hex Palet Editörü (`#RRGGBB` veya renk listesi), Kart İç Zemin Saydamlık Kaydırıcısı (`%0..%100`), Konu/Ders özel renk eşleme, durum rengi ton ayarı ve dinamik akşam/gece karartması.
  2. **#6..#10 Kart, Köşe ve Kenarlık Geometrisi:** Serbest köşe yuvarlaklığı (`0dp` keskin, `12dp` küçük, `16dp` standart v2, `24dp` devasa), kenarlık kalınlığı (`0..2dp`), kart iç gölgelendirme (`0/2/6dp`), sol-sağ hizalama takası ve 4 farklı ilerleme göstergesi biçimi (yatay çubuk, kalın çubuk, mini halka, sadece yüzde).
  3. **#11..#15 Tipografi & Metin Ölçeği:** Başlık ve gövde fontlarını ayrı ayrı eşleştirme (Poppins / Atkinson / Lora), başlık ağırlığı (600..800), görev/not satır sınırı (`1..10 satır`), harf aralığı ve saat rozet konumu.
  4. **#16..#20 Ana Sayfa, Bugün ve Liste Yerleşimi:** Sürükle-bırak blok sıralama ve gizleme (`AnaEkranDuzen` / `BugunDuzen`), istatistik şeridi modu (Yatay şerit / 2x2 Izgara / Tek manşet), akordiyon varsayılan durum kilitleri, plan dilim kart stili ve kaydırma jesti aksiyon ataması (`SİL`, `TAMAMLA`, `ERTELE`, `ARŞİVE TAŞI`).
  5. **#21..#25 Zamanlayıcı, Kadran ve Mikro-etkileşimler:** Kadran çapı ve çizgi kalınlığı, saniye akış efekti (Rulo / Düz Metin / Gizli Zen Odak), bitiş efekti (Konfeti / Parlama / Titreşim), odak sesleri görünümü (Kompakt şerit / Izgara / Alt menü) ve ripple/sarsıntı şiddeti.
  6. **#26..#30 Üst Bar, Alt Menü ve Yüzen Şerit:** Alt navigasyon bar yüksekliği/ikon boyutu, üst bar ikon düzenleyicisi, yüzen canlı şerit konum/boyutu, akıllı artı (`+` FAB) butonu konumu ve **"Tek Tıkla Özelleştirme Profili Kaydet / Yükle (#30)"** (JSON panoya kopyalama ve aktarma).
  7. **#31 & #32 Bonus Özellikler:** **Canlı Arayüz Önizleme Aynası (#31)** — yaptığınız her ayar ekranda canlı örnek kart üzerinde anında yansır; **Fabrika Ayarlarına Sıfırla (#32)** — tek dokunuşla tüm 32 parametre varsayılan v10.51/v10.52 standartlarına döner.
- **Erişim Kolaylığı:** Ayarlar ekranında özel satır (`rowTasarimAtolye`), Görünüm ve Hareket ayarlarının en başında kısayol kartı ve ana ekrandaki Ayarlar (⚙) butonuna **uzun basış kısayolu**!

## Ölçümler
- Derleme: **tek tur yeşil** · EXIT=0
- Testler: **850 test · 0 hata** (+22 yeni birim test: `TasarimAtolyeTest`)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔ — v5.0 anahtar dizisi)
- APK md5: `e1b9e642f668eb753672129beb27691c`
- APK dosya boyutu: ~26 MB
- Kaynak zip (`kaynak-v10.53-yedek.zip`): ~12 MB
