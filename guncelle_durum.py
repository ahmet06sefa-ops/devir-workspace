#!/usr/bin/env python3
# v10.14 — PROJE-DURUM.md güncelleme
import io

P = "/home/user/DEVIR/PROJE-DURUM.md"
with io.open(P, "r", encoding="utf-8") as f:
    s = f.read()

def degistir(eski, yeni):
    global s
    n = s.count(eski)
    assert n == 1, f"Çapa {n} kez: {eski[:60]!r}"
    s = s.replace(eski, yeni)

degistir(
    "_Son güncelleme: 8 Ağustos 2026 (v10.13)_",
    "_Son güncelleme: 8 Ağustos 2026 (v10.14)_",
)

degistir(
    """## ✅ Şu anki durum: v10.13 tamamlandı — ULTRA-30 Grup B ✔

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **10.13** (versionCode 169) |
| APK | `~/GunlukAsistan-v10.13.apk` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **515 test, 0 başarısız** (v10.12'de 499) |
| Kotlin dosyası | 284 (test 41) |
| Dize sayısı | 3682 |
| Yedek biçimi | 19 |""",
    """## ✅ Şu anki durum: v10.14 tamamlandı — ULTRA-30 Grup E ✔

| Alan | Değer |
|---|---|
| Paket | `com.gunlukasistan.app` |
| Sürüm | **10.14** (versionCode 170) |
| APK | `~/GunlukAsistan-v10.14.apk` |
| İmza | Debug · SHA-256 `5f15d4e7…` — **v5.0 ile aynı anahtar** ✔ (üstüne kurulabilir) |
| Test | **528 test, 0 başarısız** (v10.13'te 515) |
| Kotlin dosyası | 293 (test 42) |
| Dize sayısı | 3746 |
| Yedek biçimi | 19 |""",
)

bolum = """## 🌱 v10.14 — ULTRA-30 GRUP E: Hayat Özellikleri (8 Ağu 2026)

Son durağın bir öncesi; altı maddenin tamamı. Ortak tema: uygulama
artık yalnız görev tutmuyor — sabah gün planı teklif ediyor, uyku
ritminden kimliğini okuyor, günü üç soruyla kapatıyor, sesli notların
arkasında iz bırakıyor, başarıyı karta çevirip paylaşıyor ve yılı
bir film gibi oynatıyor.

### Yapılanlar (E25–E30)

| # | Öneri | Uygulama |
|---|---|---|
| E25 | **Sabah AI planı** | Saf `SabahPlani` seçimi (2 yarım + bugünlüler, en çok 3) → "uyandım" sonrası bayrak → ana ekranda tek seferlik diyalog; AI hazırsa 8 sn içinde doğal dile çevrilir, dönmezse yerel taslak kalır. "Görevlere işle" yarım işleri bugüne taşır ve alarmını kurar |
| E26 | **Kronotip kartı** | Saf `Kronotip` (ort. uyanış, yayılım, serçe/güvercin/gece-kuşu, odak penceresi): Analitik ekranında yeni kart; penceredeyse tek dokunaçla 25 dk odak, dışındaysa pencereye hatırlatma görevi kurar |
| E27 | **Akşam mikro günlük** | İyi geceler bildirimindeki "✍ 3 soruyla kapat" → `MikroGunlukActivity` (puan + teşekkür + yarının tek şeyi); Analitik'te 30 günlük duygu haritası şeridi + ortalama/iyi-gün özeti |
| E28 | **Sesli gelen kutusu** | `SesliKutu`: işlenen her sesli not iz bırakır (60 kayıt / 30 gün); `SesliKutuActivity` bu hafta / daha eski diye böler, satırdan hedef ekrana gider; bas-konuş-bırak ekranına giriş düğmesi |
| E29 | **Görev paylaşım kartı** | Görev menüsündeki "🖼 Kart olarak paylaş" → tema gradyanlı 1080×1350 PNG (durum rozeti, kaydırmalı metin, seri altı bant) → FileProvider ile paylaşım |
| E30 | **Senenin Filmi** | Saf `SeneFilmi` (seri zinciri, en çalışkan ay, rekor gün): Aralık'ta ana ekranda yılda bir önerilir, Analitik'ten her zaman açılır; 5 sahne alfa geçişli oynatıcı, finalde Pofi ve paylaşılabilir özet kartı |

### Tarama düzeltmeleri (kod tarandıktan sonra öneri metni revize edildi)

| Öneri | Öneri metni ne diyordu | Kod ne diyordu |
|---|---|---|
| E26 | "h verisi toplanıyor ama YORUMLANMIYOR" | Yanlıştı — v7.38 `Analitik` saat dağılımını yorumlayıp çiziyordu. Gerçek boşluk: uyku defteri + saat analizi hiç TEK KARTA girmemişti + eylem bağlantısı yoktu. E26 onu yaptı |
| E28 | "SesliNot yalnız kayıt/oynatma" | Yanlıştı — v7.71 sınıflandırma + AI + NaturalDate tarihi zaten vardı. Gerçek boşluk: işlenen notların İZİ yoktu. E28 gelen kutusunu yaptı |

### Sayılar

| Alan | v10.13 | v10.14 |
|---|---|---|
| Kotlin dosyası (ana) | 284 | **293** (+9) |
| Aktivite | 50 | **53** (+3: günlük, kutu, film) |
| Test | 515 | **528** — 0 hata (+13, `GrupETest`) |
| Dize | 3682 | **3746** (+64) |
| Bildirim eylemi | — | +"✍ 3 soruyla kapat" (iyi geceler) |

### Güven

- Saf çekirdekler framework'süz: `SabahPlani.sec`, `Kronotip`,
  `MikroGunluk` duygu hesapları, `SesliKutu.buHafta`,
  `KartUretici.satirlaraBol`, `SeneFilmi.hesapla/enUzunSeri/gunSonrasi`
  — 13 yeni test hepsini kilitler (sert kırma, üç nokta sınırı,
  yıl-taşmalı seri, pencere kelepçeleri).
- Sabah planı bayraklı ve gün-tarihli: bayrak yalnızca taslak doluysa
  kalkar, yalnızca o gün bir kez gösterilir.
- Mikro günlük 62 kayıtlık öz budamalı; sesli kutu 60 kayıt / 30 gün —
  depolar şişmez.
- Aralık filmi yılda bir kez önerilir (`hy_sene_filmi_v1.onerilen`),
  aktif gün < 5 ise kurulmaz (hikâye yokken sahne yok).

### Öz denetim — bu sürümde yakalananlar

| # | Bulgu | Sonuç |
|---|---|---|
| 1 🔴 | `hy_` öneki ZATEN DOLUYMUŞ (6 eski string) — 64 yeni stringi aynı önekle yazıyordum | Atomik betik assert'i ilk dosyada durdu, **tek bayt bile yazılmadı**; tüm önek `ge_` yapıldı. Sistem çalıştı |
| 2 🔴 | Aynı Int-taşması tuzağı ÜÇÜNCÜ kez: `0x80FFFFFF` (KartUretici) — alfa ≥ 0x80 hex → Long | Debug derleme hatası, `.toInt()` ile düzeltildi; tüm yeni dosyalar aynı desenle tarandı, başka kalmadı. **Bu benim hatamdı, tekrarlandı:** artık her yeni dosya öncesi bu grep kalıcı çalışma listesinde |
| 3 🔴 | Expression-body fonksiyonda `return null` (MikroGunluk.gunluk) — derleme hatası | Blok gövdeye çevrildi |
| 4 🔴 | Üç nokta mantığı TERSDİ: `bitti` bayrağı satır eklendikten SONRA okunuyordu → biten metne de "…" ekleniyordu | 2 test kırmızı yakaladı (kayıt!), bayrak eklemelerden önceye taşındı. Testler sürümü kurtardı |

### APK doğrulaması

- İmza SHA-256 `5f15d4e7…348511` ✔ (aynı anahtar)
- `versionCode=170 · versionName=10.14` ✔
- SimgeVarsayilan enabled ✔
- 64/64 yeni kaynak APK'da + 3/3 yeni aktivite manifest'te ✔
- **APK:** 18.3 MB · md5 `9a6d0552167b694660350d35830c28de`
- **Kaynak zip:** 680 dosya · md5 `0f4463f0ecf3249ab076cf5db6644b56`

### Cihaz doğrulaması (kurulum sonrası kontrol)

1. "Uyandım" de → uygulamayı aç → üç maddelik plan diyaloğu gelir,
   "Görevlere işle" yarım işi bugüne taşır.
2. AI kapalıysa da aynı diyalog yerel dille gelir (çevrimdışı güven).
3. İyi geceler bildiriminde "✍ 3 soruyla kapat" düğmesi görünür →
   puan + teşekkür + yarın kaydı → Analitik'te şeritte emoji çıkar.
4. Analitik → Kronotip kartı: 5+ gece kaydıysa tip + pencere ve
   duruma göre ya odak başlatma ya hatırlatma düğmesi.
5. Bir göreve uzun bas → "🖼 Kart olarak paylaş" → tema renkli PNG
   paylaşım menüsü açılır.
6. Sesli not kaydet → bas-konuş-bırak ekranında "📥 Gelen kutusu"
   belirir → not orada; satırdan hedefe gidilir.
7. Analitik → "🎬 Senenin Filmi" → sahneler kendiliğinden akar,
   dokun atlarsın, sonda Pofi + paylaşım kartı.
8. Sayaç/widget akışları v10.13'teki gibi (geri adım yok —
   528 testin tamamı yeşil).

### Sırada
**v10.15 = ULTRA-30 GRUP C (C13–C18) — son durak:** listedeki
kalan altı C maddesiyle ULTRA-30 tamamen kapanır.

---

"""

degistir(
    "## 🧩 v10.13 — ULTRA-30 GRUP B: Widget Devrimi (8 Ağu 2026)",
    bolum + "## 🧩 v10.13 — ULTRA-30 GRUP B: Widget Devrimi (8 Ağu 2026)",
)

with io.open(P, "wb") as f:
    f.write(s.encode("utf-8", "strict"))
print("PROJE-DURUM.md güncellendi")

# ONERILER: GRUP E işareti + yol haritası
Q = "/home/user/DEVIR/oneriler/ONERILER-ULTRA-30.md"
with io.open(Q, "r", encoding="utf-8") as f:
    t = f.read()
assert t.count("## GRUP E — Hayat Özellikleri") == 1
t = t.replace("## GRUP E — Hayat Özellikleri", "## GRUP E — Hayat Özellikleri ✅ (v10.14)")
if "E25–E30" in t and "v10.14 E" in t:
    t = t.replace("E25–E30", "E25–E30 ✔")
with io.open(Q, "wb") as f:
    f.write(t.encode("utf-8", "strict"))
print("ONERILER-ULTRA-30.md işaretlendi")
