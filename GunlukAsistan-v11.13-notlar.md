# 🐞 Günlük Asistan v11.13 — Sürüm Notları (Hata Düzeltme)

**Sürüm:** v11.13 · **versionCode:** 269 · **versionName:** "11.13"
**Paket:** `com.gunlukasistan.app` · **Mimari:** Kotlin/Java, Gradle 8.7, JDK 17, Android SDK 34
**Test Durumu:** ✅ **1.870 test / 0 hata / 0 başarısızlık** (saf JVM, %100 başarı)

---

## 🎙️ Bu sürümde eklenen yeni özellik

### 🧹 "Hepsini Düzelt" — Kod Kalitesi Temizliği

Kullanıcı isteği: **"Hepsini düzelt."**

Tarama sonrası bulunan tüm sorunlar giderildi:

- **`bilesenKoduBul` tutarsızlığı:** `EvrenselTasimaVeSuruklemeMotoru` bilinmeyen kart için `HERO_KARTI` döndürüyordu; `SekmeVeVeriTasimaMotoru` `GOREVLER_KARTI` döndürüyordu. Tutarlılık için ikisi de `GOREVLER_KARTI` oldu.
- **`xpDurum` mantık hatası:** Değişkene seviye numarası atanıp "XP" olarak gösteriliyordu; düzeltildi (doğru XP + tek hesaplama).
- **`BasariAnalizMotoru`:** Gereksiz/çift `hedefTamamlama` değişkeni kaldırıldı.
- **Namaz internet çağrısı kaynak sızıntısı:** `internetTazeleArkaPlan` süreçte bir kez çalışacak şekilde `@Volatile` bayrakla korundu.
- **`PlanFragment`:** Kullanılmayan `bugunOdak` hesabı kaldırıldı.
- **`SaglikMotoru`:** `onar` parametresi (bilinçli) için 9 `@Suppress`; gereksiz `as Set<Long>` cast'i kaldırıldı.
- **`AmbientFxView`:** `drawWind`/`drawCricket`/`drawWhite` kullanılmayan `w`/`h` için `@Suppress`.
- **`AsistanKomut`:** 11 kullanılmayan parametre için `@Suppress("UNUSED_PARAMETER")`.

1.870 test / 0 hata. (Kalan uyarılar: eski dosyalarda bilinçli kullanılmayan parametreler — derleme hatası değil.)

### 🔍 Üst Düzey Hata Taraması — Bulunan 2 Gerçek Hata Düzeltildi

Kullanıcı isteği: **"Tüm uygulamayı üst düzey mühendis gibi tara, hata varsa söyle."**

**Tarama sonucu:** 1.870 testin tamamı geçiyor, derleme temiz; çoğu uyarı zararsız ("parametre kullanılmıyor" vb.). Ancak **2 gerçek hata** bulundu ve düzeltildi:

1. **Namaz internet çağrısı kaynak sızıntısı** (`NamazAylikVeriServisi`):
   - `bugunGuncelSaatler` önbellek yokken **her çağrıda** yeni bir `Executor` + ağ isteği başlatıyordu (görüntüleme, plan, alarm birçok yerden çağrılıyordu). Süreç yaşamı boyunca **yalnızca bir kez** çalışacak şekilde bayrak eklendi (gereksiz ağ trafiği + thread sızıntısı giderildi).
2. **`xpDurum` mantık hatası** (`AsistanKomut`):
   - `val toplamXp = OyunlasmaMotoru.seviye(...)` — değişkene **seviye numarası** atanıp "XP" olarak gösteriliyordu; ayrıca `toplamXpGetir` 3 kez çağrılıyordu. Düzeltildi: doğru XP gösteriliyor, tek hesaplama yapılıyor.

**Taranan diğer alanlar (sorun çıkmadı / zararsız):** `onar` parametresi (bilinçli teşhis), `sonDenemeler.first()` (boş kontrolü var), `ozetSayilari[0..3]` (5 eleman döner), `otodurdurRun!!` (güvenli), `textSize` sert kodları (TasarimOlcegiTest istisnalarında), `cardCornerRadius` (sert kodlu değil). Küçük bir tutarsızlık: iki `bilesenKoduBul` varsayılanı farklı (HERO vs GOREVLER) — kritik değil.

### 🗓️ Görev Takvimi / Yaklaşan Görevler

Kullanıcı isteği: **"Yeni bir özellik ekle."**

- **`GorevTakvimiMotoru`** — görevleri tarihe bağlar ve durumlarını sınıflar:
  - `guneAta` (görevi güne bağla), `gunFarki`, `durum` (gecikti/bugün/yarın/ileri), `yaklasanlar`, `gecikenler`.
  - AI komutu: `gorev_takvimi`.
- 5 yeni JVM testi → toplam **1.870 test, 0 hata**.

### 📉 Trend / Eğilim Analiz Motoru

Kullanıcı isteği: **"Devam et."**

- **`TrendAnalizMotoru`** — son günlerin eğilimini ve tahminini hesaplar:
  - Seri ortalaması, artan/azalan/sabit eğilim tespiti, sonraki gün tahmini.
  - AI komutu: `trend_analiz`.
- 9 yeni JVM testi → toplam **1.865 test, 0 hata**.

### 📈 Başarı / İstatistik Analiz Motoru

Kullanıcı isteği: **"Devam et."**

- **`BasariAnalizMotoru`** — dönemsel başarı raporu üretir:
  - Başarı oranı, istikrar (seri) oranı, durum notu, aylık rapor.
  - AI komutu: `basari_raporu`.
- 5 yeni JVM testi → toplam **1.856 test, 0 hata**.

### 💾 Veri Boyutu / Depolama Temizlik Asistanı

Kullanıcı isteği: **"Devam et."**

- **`VeriBoyutMotoru`** — depolama kullanımını kategori bazlı ölçer, temizlenebilir öğeleri önerir.
  - Okunur boyut metni (B/KB/MB/GB), toplam + temizlenebilir toplam, azalan öneri listesi.
  - AI komutu: `depolama_durum`.
- 7 yeni JVM testi → toplam **1.851 test, 0 hata**.

### 📊 CSV Veri Dışa Aktarma

Kullanıcı isteği: **"Devam et."**

- **`CsvDisAktarMotoru`** — görev ve alışkanlıkları **CSV** (Excel/Google Sheets uyumlu) biçimine çevirir.
  - CSV hücre güvenliği (virgül/tırnak/satır sonu sarımı), görev + alışkanlık satırları, başlık birleştirme.
  - AI komutu: `disa_aktar_csv`.
- 7 yeni JVM testi → toplam **1.844 test, 0 hata**.

### 👥🔔 Sosyal Meydan Okuma + Akıllı Bildirim Filtresi + Kurulum Kılavuzu

Kullanıcı isteği: **"Devam et."** (Yapılamayan büyük maddeler için kılavuz + kalan çekirdekler)

- **👥 Sosyal meydan okuma (`SosyalMeydanOkumaMotoru`)** — üye puanları, sıralama, lider/kazanan, grup hedefine ilerleme. AI komutu: `meydan_okuma`.
- **🔔 Akıllı bildirim filtresi (`BildirimFiltreMotoru`)** — sessiz saatler, önem eşiği, odak içi/gecikme önceliği, bildirim atlama kararı. AI komutu: `bildirim_durum`.
- **📘 Kurulum kılavuzu** — `KURULUM-KILAVUZU-BULUT-WEB-SAGLIK-SOSYAL.md` (bulut/web/sağlık/sosyal/yerelleştirmenin gerçek yayına taşınması, adım adım).
- 11 yeni JVM testi → toplam **1.837 test, 0 hata**.

### 🌐📄 Uygulama Eksikleri Kapatıldı: Çok Dillilik + Zengin Dışa Aktarma + Uyum Belgeleri

Kullanıcı isteği: **"Uygulamadaki eksikleri söyle" → "Hepsini yap."**

Bu turda güvenle ve test edilebilir şekilde eklenenler:

- **🌐 Çok dillilik seçici (`DilSeciciMotoru`)** — Türkçe/İngilizce/Almanca/Fransızca/Arapça/İspanyolca/Rusça; RTL desteği, anahtar tabanlı çeviri köprüsü, yerel dil → seçili dil. AI komutu: `dil_sec`.
- **📄 Zengin veri dışa aktarma (`VeriDisAktarMotoru`)** — günün markdown raporunu üretir (görev + odak + kurs + notlar). AI komutu: `disa_aktar`.
- **🔒 Gizlilik politikası taslağı** — `GIZLILIK-POLITIKASI.md` (Play Store yayını için).
- **🗺️ Gelişim yol haritası** — `GELISIM-YOL-HARITASI.md`.
- 10 yeni JVM testi → toplam **1.826 test, 0 hata**.

**Not:** Gerçek .xml yerelleştirme dosyaları (çeviriler), bulut sunucu, web/desktop yayını, Google Fit SDK ve sosyal ağ hâlâ sunucu/API/cihaz gerektiren yayın adımlarıdır — mantık çekirdekleri hazır (`SenkronMotoru`, `SaglikVeriMotoru`, `TakvimPlanlamaMotoru`).

### ☁️🏃🗓️ Rakip Farkı Çekirdekleri (Bulut / Sağlık / Takvim)

Kullanıcı isteği: **"5 (çok dillilik) hariç hepsini yap."** (Bulut, web, sağlık, takvim)

Gerçek sunucu / Google Fit SDK / web yayını bu sandbox'ta uçtan uca kurulamaz; ancak her özelliğin **zor ve değerli mantık çekirdeği** inşa edildi ve JVM testli — gerçek altyapı eklendiğinde hemen kullanılır:

- **☁️ Bulut senkron / hesap (`SenkronMotoru`)**:
  - Zaman damgalı veri paketleri, iki cihazı birleştirme, çakışma tespiti/çözümü, en yeni sürüm seçme, hesap/cihaz modeli, sunucu istek gövdesi üretici.
  - AI komutu: `hesap_durum`.
- **🏃 Sağlık verisi (`SaglikVeriMotoru`)** — Google Fit/Health Connect tarzı:
  - Günlük adım kaydı, hedef yüzdesi, adım yoğunluğu derecesi (ısı haritasına), hedef önerisi.
  - AI komutu: `saglik_hedef`.
- **🗓️ Takvim planlama (`TakvimPlanlamaMotoru`)**:
  - Görevi güne atama, haftalık dengeli dağıtım, okunur plan metni.
  - AI komutu: `takvim_plan`.
- AI prompt'una yeni komutlar eklendi; **15 yeni JVM testi** → toplam **1.816 test, 0 hata**.

**Not:** Gerçek bulut HTTP taşıyıcısı, Google Fit SDK ve web/desktop dağıtımı, sunucu + API anahtarı + cihaz gerektirdiği için ayrı bir yayın adımıdır; buradaki motorlar o entegrasyonun mantığını hazır tutar.

### 🎮 Rakiplerde Olup Sende Olmayanlar Eklendi

Kullanıcı isteği: **"Rakiplerde olup bende olmayanları söyle" → "Hepsini ekle."**

Kod taraması sonucu zaten var olanlar (uygulama kilidi `KilitActivity`/`KilitDepo`, yıl ısı haritası `YilIsiView`, yedek şifreleme `YedekSifre`) tespit edildi; gerçek eksikler eklendi:

- **🎯 Oyunlaştırma (XP / Seviye / Rütbe)** — `OyunlasmaMotoru`:
  - Görev/alışkanlık/odak XP'si, seviye atlama, seviye içi ilerleme, rütbe adları (Başlangıç→Efsane).
  - AI komutu: `xp_durum`.
- **🗓️ Hazır Koçluk Programları** — `KoclukProgramlari`:
  - Ders Çalışma, Erken Kalkma, Odak Ustası gibi hazır adım-adım günlük programlar.
  - AI komutu: `kocluk_programi`.
- **🔥 Alışkanlık Isı Haritası** — `IsiHaritasiMotoru` (saf): günlük puan → 0..4 ısı seviyesi, hafta-benzeri matris (yıl ısı haritası zaten vardı).
- AI prompt'una yeni komutlar eklendi; 17 yeni JVM testi → toplam **1.801 test, 0 hata**.

**Not — hâlâ yapılmayan:** ☁️ **Bulut senkron / hesap / çoklu cihaz** ve 🌍 **Web/masaüstü sürümü** — bunlar kapsamlı ağ/altyapı işi; ayrıca ele alınmalı (sonraki sürüm önerisi).

### 🕌 Namaz Saatleri Artık İnternetten (Gerçek Diyanet) + Vakit Planı Yenilendi

Kullanıcı isteği: **"Namaz saatleri gerçek namaz saatlerine uymuyor; aldığın yeri değiştir ve internetten güncel namaz saatlerini getir. Vakit planı sekmesi aşağı kaydırınca yukarı geri çıkamıyorum; düzelt ve güzel yap."**

- **Namaz vakitleri gerçek internet kaynağına geçirildi:**
  - `NamazInternetServisi` — ücretsiz **Aladhan API** (hesaplama method **13 = Diyanet**) ile bugünün **gerçek vakitlerini** çeker.
  - Görüntüleme + alarmlar artık internet önbelleğini kullanıyor; ağ yoksa **astronomik hesaba** düşer (ekran boş kalmaz).
  - Günde bir kez internetten tazelenir (arka planda, UI bloke olmaz).
- **Vakit Planı sekmesi yenilendi:**
  - ScrollView → **NestedScrollView** (kaydırma çakışması çözüldü, **yukarı çıkış garanti**).
  - Gradyan başlık + vurgu renkli güzel kartlar (`colorPrimaryContainer`).
  - **Kaydırma pozisyonu korunuyor** — içerik yeniden çizilse de aşağıda kaldığın yerden devam edersin.
- 4 yeni JVM testi (internet yanıt ayrıştırma) → toplam **1.784 test, 0 hata**.

### ⚡ Sekmeler Arası Taşıma Düzeltildi

Kullanıcı isteği: **"Sekmeler arası istediğim öğeyi ya da tabloyu taşıma özelliği getir. Varsa da kontrol et durumunu, çünkü yapamıyorum."**

**Durum kontrolü:** Özellik zaten vardı (`EvrenselTasimaVeSuruklemeMotoru` + `SekmeVeVeriTasimaMotoru`), ama bir hata vardı:

- **Kök neden:** `containerIcinSurukleVeTasiKur` içinde `child.id == View.NO_ID && child.tag == null` olan kartlar **atlanıyordu**. Konular/Bugün gibi sekmelerde programatik eklenen kartlar id'siz/tag'siz olduğu için **uzun basış hiç bağlanmıyordu** → "taşıyamıyorum" hissi.
- **Düzeltme:** Motor artık id/tag şartı aramadan **her kartı** uzun basış + sürükle-bırak yetkisiyle bağlıyor (görünür ad "Kart #N" oluyor).
- **Hedef sekmeler güncellendi:** Eski "İlerleme (progress)" hedefi (artık alt menüde yok) yerine **⏱️ Sayaç (timer)** eklendi.
- 6 yeni JVM testi (`bilesenKoduBul` eşleştirme) → toplam **1.780 test, 0 hata**.

**Nasıl kullanılır:** Herhangi bir sekmede bir karta **basılı tut** → menü açılır → "⚡ Bu Kartı Başka Bir Sekmeye Gerçek Kart Olarak Taşı/Kopyala" seç → hedef sekmeyi seç → kart orada gerçek bir bileşen olarak belirir. Kartı **sürükleyip** de sırasını değiştirebilirsin.

### 🔀 Sekme Düzeni Yenilendi: İlerleme → Plan → Sayaç

Kullanıcı isteği: **"İlerleme sekmesinin yerine Plan sekmesini koy. Plan sekmesinin yerine de Sayaç sekmesini koy. Sayaç ekranını oradan açabileyim. İlerleme sekmesini sekmeler arası mantıklı bir şekilde paylaştır."**

- **Alt menü sırası değişti:** Artık **Ana → Bugün → Konular → Plan → Sayaç**.
  - `nav_progress` (İlerleme) kaldırıldı; yerine **`nav_plan`** (Plan).
  - `nav_plan`'ın yerine **`nav_timer`** (Sayaç) eklendi → **Sayaç artık alt menüden tek dokunuşla açılıyor.**
- **İlerleme verisi sekmelere paylaştırıldı:**
  - Plan sekmesinin üstüne **"📊 Genel İlerleme"** özet kartı eklendi (seri, haftalık odak, tamamlanan; dokununca AnalitikActivity açılır).
  - Ana ekranda zaten istatistik kartları vardı (seri/toplam/ilerleme/odak).
  - İlerleme sekmesi (index 1) arka planda erişilebilir kalıyor; içerik kaybolmadı.
- `MainActivity` bottom nav callback + `navItemFor` güncellendi; `bottom_nav.xml` yeniden düzenlendi.
- 1.774 test / 0 hata.

### ⏱️ Zamanlayıcıdaki "Minik Saat Yazısı" Hatası Düzeltildi

Kullanıcı isteği: **"Zamanlayıcıda minik saat yazısı hata veriyor; zamanlayıcıdaki hataları düzelt, hepsini incele."**

**Kök neden:** `timeText` TextView'i layout'ta `visible` bırakılmıştı (yorum açıkça GONE olması gerektiğini söylüyordu). Kadran zaten süreyi kendi içinde çiziyordu ama `timeText` de görünür durumdaydı → **iki süre metni üst üste biniyordu** → "minik saat yazısı" görsel hatası.

**Düzeltmeler:**
- `fragment_timer.xml` → `timeText` artık `gone` (kadran tek gerçek süre kaynağı; çakışma giderildi).
- `SayacKadraniView.onDraw` → kompakt mod koşulu kaldırıldı; kadran **her modda** büyük süreyi kendisi çiziyor (timeText GONE olunca süre hiçbir modda kaybolmuyor).
- `TimerEngine.format` → saat biçimi `formatTime` ile aynı hale getirildi (`%02d:%02d:%02d`); tutarsızlık giderildi.
- 1.774 test / 0 hata.

### 🛠️ Ajan Modu Takılması Düzeltildi

Kullanıcı isteği: **"Sürekli ajan modu deyip zamanlayıcı kısmına giriyor; kodlamasını düzelt ve hatalarını onar."**

**Sorun:** `AjanModu.hedefDk` herhangi bir çıplak sayıyı dakika olarak alıyor ve varsayılan 60 döndürüyordu; tetikleme koşulu `hedefDk >= 30` olduğu için **rastgele sayı içeren her mesaj** ("30 sayfa oku", "5 tane görev") ajan moduna girip zamanlayıcı komutları üretiyordu.

**Düzeltme:**
- `hedefDk` artık yalnızca **açık süre birimi** ("N saat", "N dk", "N dakika") olduğunda sayar; çıplak sayı veya süre yoksa **0** döner.
- Yeni **`ajanModuGerekliMi`** güvenli karar fonksiyonu: yalnızca kullanıcı "ajan" derse VEYA açık süre + çalışma fiili (çalış/odaklan/çöz/plan/hedef/ders) birlikteyse tetiklenir.
- `AsistanFragment` bu güvenli fonksiyonu kullanıyor → **normal sohbet artık zamanlayıcıya takılmıyor**.
- `planaCevir` süre yoksa zamanlayıcı adımı eklemiyor.
- 3 yeni JVM testi → toplam **1.774 test, 0 hata**.

### 🧹 İlk Açılıştaki Atölye Butonları Kaldırıldı

Kullanıcı isteği: **"İlk açılıştaki o 5 veya 6 tane olan atölye butonlarını kaldır."**

- `HomeFragment.atolyeButonlariniGuncelle` artık tüm atölye/modül kısayol butonlarını kalıcı olarak **GONE** yapıyor.
- Ana ekranda yalnızca temel butonlar (**⏱ Zamanlayıcı** ve **⚙ Ayarlar**) kalıyor.
- 20 atölye butonu (Manuel Kontrol, Otonom, Tasarım, Karne, Yaşam, Gelişmiş, Uzman, Ders, Sağlık, Namaz, Bin Madde, Kişisel Gelişim, Canva, Görünüm vb.) artık ana ekranın üst satırında görünmüyor.
- Atölye ekranlarına erişim **Ayarlar → atölye satırları** üzerinden devam ediyor (kaybolmadı).
- 1.771 test / 0 hata.

### 🔔 Proaktif Koç Bildirimi Zamanlayıcıya Bağlandı

Kullanıcı isteği: **"Devam et."** (Proaktif koç bildirimlerinin zamanlanması)

- `BildirimMerkezi.Tur.KOC` — ayarlanabilir yeni bildirim türü (varsayılan açık, "Akıllı Koç").
- `BildirimUretici.proaktifKoc` — günün vaktine göre koç mesajını bildirim olarak gönderir (günde 1 kez).
- `BildirimZamanlayici` — sabah (09:00) ve akşam (19:00) turlarına proaktif koçu bağladı.
- Böylece AI artık **isteyene kadar beklemiyor**: sabah ve akşam, senin verinle otomatik koç mesajı bildirimi gelir.
- 1.771 test / 0 hata.

### 🧑‍🏫 "Akıllı Koç" Paketi — Proaktif Bildirim + Haftalık Rapor + Günlük Plan

Kullanıcı isteği: **"Daha iyi yapacak öneri sun" → "Hepsini yap."** (Önerilen 3 öncelikli özellik)

- **Proaktif Akıllı Koç (`KocMotoru`):** AI artık seni isteyene kadar beklemiyor. Günün vaktine göre (sabah/öğle/akşam/gece) ve güncel verine göre **kendiliğinden** koç mesajı üretir: *"Günaydın! Bugün 5 görev seni bekliyor…"*
- **Haftalık Koç Raporu (`HaftalikKocRaporu`):** Haftalık odak, görev, kurs ve seri verisinden yıldızlı derecelendirmeli şık rapor üretir.
- **Akıllı Günlük Plan (`AkilliGunlukPlan`):** Bekleyen görevleri gün içine 50 dk'lık bloklara + molalara bölerek saatli zaman çizelgesi çıkarır.
- **AI komutları:** `koc_mesaj`, `haftalik_rapor`, `akilli_plan` — prompt kataloğuna eklendi.
- 13 yeni saf JVM testi → toplam **1.771 test, 0 hata**.

### ✨ Yapay Zekâ Ekranı Gemini Tarzına Dönüştürüldü

Kullanıcı isteği: **"Yapay zeka ekranını Google Gemini gibi yap."**

- **Gradyan üst bar** (`ai_header_gradient`): modern, yumuşak renk geçişli başlık.
- **Yuvarlak avatar**: sağ üstte temaya uyan 44dp avatar rozeti.
- **Mod rozeti** (`ai_mode_chip`): temaya uyan yuvarlak çip.
- **Gemini tarzı giriş çubuğu**: tam yuvarlak (`ga_kose_tam`) MaterialCardView; içinde metin alanı + mikrofon + yuvarlak mor gönder düğmesi (`ai_send_round`).
- **Sohbet baloncukları**:
  - Asistan mesajı → **solda ✨ avatar** + yüzey rengi kart.
  - Kullanıcı mesajı → **sağda dolu primary (mor) baloncuk**, beyaz metin.
- Tüm görsel değerler `@dimen` (ga_yazi/ga_kose) kullanıldı — `TasarimOlcegiTest` ve `RippleTutarlilikTest` uyumlu (1.758 test geçti).

### 📞 AI Telefon Kontrolü: Ara & Başka Uygulamalara Yaz

Kullanıcı isteği: **"Sadece telefon değil, benim için yazı yazabilir mi, telefonun herhangi bir uygulamasında başka şeyler yaptırabilir miyim?"**

- **`telefon_ara`** (`ACTION_DIAL`): AI "X'i ara" deyince çeviriciyi **numara dolu** açar; tek dokunuşla kullanıcı arar. **İzin gerektirmez**, her cihazda çalışır (güvenli yol).
- **`yaz`** (erişilebilirlik `ACTION_SET_TEXT`): AI, telefonun **herhangi bir uygulamasındaki odaklanmış metin alanına** senin için yazar (WhatsApp mesajı, form, not…). Kullanıcı önce alana dokunur, AI doldurur.
- **`tikla` / `geri` / `ana`**: erişilebilirlik servisi üzerinden diğer uygulamalarda tıklama ve global aksiyonlar (önceki sürümlerden).
- `EkranDokunmaServisi`'ne `odaklanmisaYaz`, `telefonlaAra`, `findEditable` eklendi.
- `AsistanKomut`'a `telefon_ara`, `yaz`; prompt kataloğuna eklendi.
- **Güvenlik:** Yalnızca kullanıcının açtığı **Erişilebilirlik servisi** aktifse çalışır; servis kapalıyken güvenli "aç" mesajı verir.

### 🔧 Yarım Kalanları Tamamlama

Kullanıcı isteği: **"Yarım kalanları tamamla."** (Gemini farkını kapatma listesindeki kalan 2 önemli madde)

- **#1 Native tool / function calling (`FonksiyonCagrisiMotoru`):**
  - `>>KOMUT:` ayrıştırmasına ek olarak, Gemini/OpenAI'nin native **function-calling** çıktısını (yapılandırılmış `functionCall` JSON) uygulamanın komut sistemine çevirir.
  - `fonksiyonTanimi` / `fonksiyonListesi` (12 komut şeması) + `cevabiCoz` (hem `args` hem `arguments` hem basit `deger` biçimini destekler).
  - Ayrıştırma hatalarını en aza indirir; AI'nın asıl gücünü doğru kullanır.
- **#4 Gerçek ekran görüntüsü (piksel) (`EkranYakalamaMotoru` + servise `ekranGoruntusuAl`):**
  - Yalnızca etiket listesi yerine, API 30+ `takeScreenshot` ile **gerçek piksel** ekran görüntüsü yakalanır.
  - `gorselIstemiKur` — görsel modele gidecek istem; `yakinlastir` — piksel matrisini normalize kareye indirger.
  - Görsel model, ekranda NE olduğunu görüp "neye dokunacağına" kendisi karar verir.
- 10 yeni saf JVM testi → toplam **1.758 test, 0 hata**.

### 🌟 Gemini'den Farkını Kapatan "Akıllı Koç" Paketi

Kullanıcı isteği: **"Gemini'den farkı kaldı mı, öneri sun" → "Hepsini yap."**

AI artık genel web Gemini'sinden farklı olarak **uygulamayı gerçekten yöneten ve kullanıcıyı tanıyan bir koç**:

- **A) Ekran görüntüsü → AI görsel karar (`EkranGoruntusuMotoru`):**
  - Ekrandaki tıklanabilir etiketler AI'a verilir; AI hangisine dokunacağına karar verir (`tikla|X`) ve uygulama uygular.
- **B) Kalıcı kullanıcı hafızası (`KullaniciHafizasi`):**
  - Bugünkü odak/seri/hedef/soru → AI her sohbette kullanıcıyı "tanır"; prompt'a "Hafıza: …" satırı eklenir.
- **C) Ajan modu (`AjanModu`):**
  - "4 saat çalış, 20 soru çöz" de → AI 25'er dk odak blokları + görev + özet şeklinde **çok adımlı plan kurar ve adım adım uygular**.
- **D) Konuşma kesme (`KonusmaKesmeMotoru`):**
  - Sesli oturumda "dur/kes/yeter" deyince AI anında susar ve yeni komut dinler.
- Entegrasyon: `AsistanFragment` (ajan modu + kesme), `AiClient.buildSystemPrompt` (hafıza).
- 16 yeni saf JVM testi → toplam **1.748 test, 0 hata**.

### 🖐️ AI Asistana Ekrana Dokunma Yetkisi & Adım Adım Görünür Uygulama

Kullanıcı isteği: **"Yapay zeka asistanı ekrana dokunabilme yetkisi olsun; sesli komut vereyim, AI ekrandan benim için gerçekleştirsin ve hepsini tek tek göreyim."**

- **`AdimliEylemMotoru.kt`** (saf, 10 JVM testli):
  - AI'nın ürettiği komutları insan-okunur **eylem adımlarına** çevirir.
  - Sıra/tamamlama/atlama durumunu yönetir; her adımın Türkçe açıklamasını üretir.
- **`EkranDokunmaServisi.kt`** (AccessibilityService) — **gerçek ekran dokunma yetkisi**:
  - Kullanıcı Sistem → Erişilebilirlik → "Günlük Asistan Ekran Dokunma"yı açınca, AI ekrandaki metin/düğmeye **tıklayabilir**, geri/ana ekran aksiyonu çalıştırabilir.
  - Servis etkin değilse hiçbir şey yapmaz; yalnızca aktif komut listesiyle çalışır (güvenli).
  - Manifest + `accessibility_service_config.xml` + açıklama ekranı eklendi.
- **`AsistanFragment.kt`** — **adım adım görünür uygulama**:
  - Sesli oturumda AI çoklu komut üretince, her adımı **tek tek** ekranda kart olarak gösterir: "1/3 👉 Görev ekle: Proje sunumu" → "✅ …tamam".
  - Kullanıcı her adımı görür; kısa gecikmeyle sıradaki adıma geçer.
- 10 yeni saf JVM testi → toplam **1.732 test, 0 hata**.

### 📱 Telefondaki Diğer Uygulamalara Erişim

Kullanıcı isteği: **"Uygulama telefondaki diğer uygulamalara erişim sağlasın."**

- **`UygulamaMotoru.kt`** (saf, 9 JVM testli):
  - Türkçe duyarlı arama/eşleştirme (`filtrle`, `oncelikPuan`, `eslesme`).
  - Paket adından akıllı kategori (`kategori`) — Mesajlaşma, Video, Müzik, Sosyal, Finans…
- **`UygulamalarActivity`** — "📱 Telefondaki Uygulamalarım" ekranı:
  - Yüklü başlatılabilir uygulamaları listeler (simge + ad + paket + kategori).
  - Canlı arama kutusu ile filtreler.
  - Bir uygulamaya dokununca **başlatır**.
  - **🖼️ Galeriden Al / 📁 Dosyadan Al** — sistem seçicisiyle fotoğraf/PDF gibi içeriği nota kaydeder.
- **Manifest:** `LAUNCHER` sorgusu için `<queries>` eklendi (Android 11+).
- **Ayarlar:** "📱 Telefondaki Uygulamalarım" satırı.
- **AI kontrol:** `uygulama_ac | WhatsApp` (eşleşen uygulamayı başlatır) ve `uygulamalar_ac` komutları; prompt kataloğuna eklendi.
- 9 yeni saf JVM testi → toplam **1.722 test, 0 hata**.

### 🤖 Yapay Zekâ Uygulamanın Her Şeyini Kontrol Eder (Genel Kontrol Merkezi)

Kullanıcı isteği: **"Yapay zeka uygulamanın her şeyini kontrol edebilsin."**

Yapay zekâ (`AsistanKomut` + çevrimiçi AI) artık sadece veri değil, **uygulamanın tamamını** doğal dille yönetir:

- **Uygulama ayarları:** `ayar_ses`, `ayar_titresim`, `ayar_animasyon`, `ayar_namaz`, `ayar_gece` (koyu/açık/sistem tema) — AI konuşmadan sesi/titreşimi/temayı/namazı açıp kapatır.
- **Widget & tazeleme:** `widget_yenile` — tüm widget'ları anında günceller.
- **Veri özeti:** `ozet_ver` — AI tüm veri durumunu (görev/not/konu/alışkanlık/kurs + günlük odak) özetler.
- **Tüm atölye/merkez ekranları:** `atolye_ac` — Canva, Kişisel Gelişim, Evrensel Görünüm, Bin Madde, YouTube, Veri Yedek, Depolama, Sohbet Geçmişi, Uyku.
- `ekran_ac` kapsamı genişletildi (takvim, kişisel gelişim…).
- `AiClient.buildSystemPrompt`'a yeni komut kataloğu eklendi → çevrimiçi AI bu komutları üretebilir.
- Güvenlik korundu: silmeler onaya tabi, toplu silme yok; `evetMi`/`geceSecimi` saf ve testli.
- 6 yeni saf JVM testi → toplam **1.713 test, 0 hata**.

### 🎙️ Sesli Yapay Zekâ Asistanı (ChatGPT / Gemini tarzı — kesintisiz çift yönlü)

Kullanıcı isteği: **"Sadece sesli anlatım değil, normal ChatGPT/Gemini gibi sesli asistan kur."**

- **`SesliAsistanModu.kt`** — kesintisiz sesli sohbet çevriminin saf karar katmanı:
  - Konuşma tanıma çıktısını temizler (`sesliSoruTemizle`).
  - Cevap sonrası **tekrar dinle** döngüsünü yönetir (`surekliDinlemeliMi` / `yeniTur` / `turSiniri`).
  - AI cevabını TTS için temiz, sözlü metne çevirir (`konusulabilirCevap` — emoji, kod blokları, "✓ Yapıldı" ön ekleri ayıklanır).
  - Sözlü, kısa "koç gibi" yanıt istemi üretir (`sesliCevapIstemi`).
- **`AsistanFragment.kt`** — **kesintisiz sesli asistan oturumu**:
  - Mikrofon düğmesine dokun → asistan **dinler → düşünür → sesli cevap verir → tekrar dinler** (eller serbest, ChatGPT/Gemini gibi).
  - Uzun basış ya da "🎤 Dinliyorum…" durum çipine dokun → oturumu anında bitirir.
  - Durum göstergesi: 🎤 Dinliyorum / 🧠 Düşünüyorum / 🗣️ Konuşuyorum.
  - Tur sınırı (12) aşılınca otomatik kapanır (sınırsız döngü yok, pil/ses güvenliği).
  - Ekran kapanınca oturum + TTS birlikte durur.
- 10 yeni saf JVM testi → toplam **1.707 test, 0 hata**.

### Sesli Yapay Zekâ Anlatımı (önceki ekleme — metin cevapları sesli okur)

Kullanıcı isteği: **"Yapay zekayı sesli hale getir benimle konussun asistan kocum gibi"**

- **`AsistanSes.kt`** — TTS sarmalayıcısı:
  - AI asistanın cevaplarını **kulağa okur** (TextToSpeech, Türkçe).
  - Motor hazır olana dek konuşmalar küçük bir kuyrukta bekler; hazır olunca sırayla okunur.
  - `konusmaMetni` (saf, testli): emoji/simge karakterleri ayıklanır, çoklu boşluk tekilleşir, uzun cevaplar sınırlanır → TTS temiz okur.
  - Aç/kapa tercihi (`AsistanSes.sesAcikMi`) — **varsayılan AÇIK**.
- **`AsistanFragment.kt`** entegrasyonu:
  - Yeni asistan cevabı geldiğinde otomatik sesli okunur (koç gibi karşılar).
  - Başlıktaki **🔊/🔇** düğmesiyle tek dokunuşta aç/kapat; kapatınca çalan ses anında kesilir.
  - Ekran kapanınca TTS de kapanır (arka planda sürpriz ses çıkmaz).
- 7 yeni saf JVM testi → toplam **1.697 test, 0 hata**.

---

## 🎯 Önceki bu sürümde düzeltilen iki önemli sorun

### 1️⃣ Zamanlayıcıdaki saat takılıyor (donuyor)

**Kök neden:** `TimerFragment.tick()` içinde `handler.postDelayed(ticker, 100)` en son satıra yazılmıştı. Gövde içindeki herhangi bir istisna (kadran/zincir güncelleme, maç satırı, bildirim tazeleme vb.) oluştuğunda bu satır hiç çalışmıyor, 100 ms'lik tık zinciri kopuyor ve ekrandaki saat **sessizce donuyordu**.

**Çözüm:**
- Yeni **`CevrimliTik`** (saf, test edilebilir) sarmalayıcısı: gövde ne olursa olsun, istisna fırlatsa bile `tik()` "devam et" döndürür; bir sonraki tık her koşulda zamanlanır → **saat asla takılmaz**.
- `tick()` gövdesi ayrıldı ve `postDelayed` artık her akışta (hata olsa bile) garantileniyor.
- Hatalar log'a yazılıp yutuluyor; akış korunuyor.

### 2️⃣ Güç düğmesine basınca alarm ve sesler susmuyor

**Kök neden:** Bitiş sesi, `TimerActionReceiver.bitisSesiCal` içinde **yerel `MediaPlayer`** ile sonsuz döngüde çalınıyordu; güç düğmesinin tetiklediği `ACTION_SCREEN_OFF` (ekran kapanması) hiç dinlenmiyordu. Titreşim de iptal edilmiyordu. Ayrıca bitişe eşlik eden ısrarlı alarm (`ZorunluUyari`) da aynı şekilde susmuyordu.

**Çözüm:**
- Yeni **`BitisSesMotoru`** merkez yöneticisi:
  - Bitiş sesini **döngülü alarm** olarak çalar (ses + titreşim tek noktadan).
  - Ses başlatıldığında `ACTION_SCREEN_OFF` için dinamik alıcı kaydeder → **güç düğmesine basılır basılmaz** ses ve titreşim anında susar.
  - `durdur()`: sesi durdurur/yayınlar, titreşimi iptal eder, ısrarlı alarmı da (`ZorunluUyari.durdur`) keser, alıcıyı ve zamanlayıcıları temizler.
  - Otomatik süre (`SayacAyar.sesSureSn`) sonunda kendiliğinden susar.
  - Uygulamanın mevcut **"güç tuşuyla alarmı durdur"** ayarına (`isKapatmaTusuyleAlarmDurdur`) saygı gösterir.
- Bitiş bildirimine **"🔕 Sesi Kapat"** eylemi eklendi (ikinci emniyet ağı — bildirimden anında susturma).
- `SayacBittiActivity` kapatılınca / ekran yok edilince ses otomatik susuyor.
- `ACTION_STOP` / `ACTION_RESET` akışlarına da ses kesme eklendi.

---

## 📦 Teslim Paketi
1. `GunlukAsistan-v11.13.apk`
2. `kaynak-v11.13-yedek.zip`
3. `PROJE-DURUM.md`
4. `GunlukAsistan-v11.13-notlar.md`

*Gofile yedek linkleri ve GitHub (`https://github.com/ahmet06sefa-ops/devir-workspace.git`, main dalı) en güncel haliyle güncellendi.*

---

## 📈 Önceki sürümden özet
- **v11.12 (268):** Evrensel Veri Yedekleme & Geri Yükleme Motoru (1.670 test)
- **v11.11 (267):** Canva Çalışma Ekranı — 10 Uygulama Arayüzü, Aç-Kapa, Akıllı Öneri & Tekrar Dene
- **v11.10 (266):** A'dan Z'ye gerçek kart taşıma / sürükleme yetkisi ve Evrensel Kart Kataloğu

*Mimari koruma: TasarimOlcegiTest, RippleTutarlilikTest, GorunumAtolyeTest, AnaEkranButonTest — hepsi başarılı.*
