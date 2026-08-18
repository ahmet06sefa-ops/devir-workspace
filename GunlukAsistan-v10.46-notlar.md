# Günlük Asistan v10.46 (kod 202) — Sürüm Notları

## Bu sürümde: Kullanıcı maddesi #8 (Kullanışlı & Kontrol Edilebilir Mini Sayaç)

### 8️⃣ ▦ Kontrol Edilebilir & Kompakt PiP Yerleşimi
**Şikayet & İstek:** "Mini sayaç oluştururken kontrol edemiyorum sayacı, o özellikleri de küçült ve bana kullanışlı küçük sayaç ver. İnternette araştır, bana güzel bir şekilde kullanışlı minik yap."
- **Neden kontrol edilemiyordu?** Android Picture-in-Picture (PiP) penceresinde normal arayüz butonları dokunma olaylarını (touch events) almaz. Ekrandaki butonlara dokunulduğunda Android bunu algılamayıp yalnızca sistem pencere kontrollerini açar.
- **Çözüm (Canlı PiP Aksiyonları - RemoteAction):** Android'in PiP API'si (`PictureInPictureParams.Builder.setActions`) entegre edildi. PiP penceresinde doğrudan kullanılabilecek 3 canlı RemoteAction butonu tanımlandı:
  1. **▶ Devam / ⏸ Bekle (`ACTION_PIP_PLAY_PAUSE = 101`):** Sayacın durumuna (`TimerEngine.isRunning(ctx)` / `IleriSayim.calismakta(ctx)`) göre otomatik ikon ve işlev değiştirir.
  2. **⏹ Sıfırla / Bitir (`ACTION_PIP_RESET = 102`):** Sayacı sıfırlar veya İleri Sayım oturumunu bitirip kaydeder.
  3. **＋5 dk Uzat (`ACTION_PIP_UZAT = 103`):** Geri sayım modundayken aktif oturumu tek dokunuşla 5 dakika uzatır.
- **Kompakt & Ferah PiP Kadranı ("Özellikleri Küçült"):** 
  - PiP penceresine geçildiğinde işlevsiz ek butonlar (`mainAction`, `resetButton`) gizlendi; pencerede YALNIZCA kadran ve sistem butonları kaldı.
  - `MiniMod.pipOlcegi(pip)` ile kadran **×1,15 ölçeklendirildi** ve dış çerçeve boşlukları (`pipDolguDp(pip) = 4dp`) minimuma indirildi. Küçük PiP penceresinde sayaç rakamları ve halkası büyük, net ve taşmadan ortalanmış hale getirildi.
- **Senkronizasyon:** `TimerFragment` içerisine kayıtlı özel `BroadcastReceiver` (`com.gunlukasistan.app.PIP_KONTROL`), PiP penceresinde basılan her buton anında motora aktarılır ve aksiyon listesini (`pipAksiyonlariniGuncelle()`) senkronize eder.

## Ölçümler
- Derleme: **tek tur yeşil** · EXIT=0
- Testler: **752 test · 0 hata** (+4 yeni birim test: `MiniModTest` aksiyon listeleri, ölçek, dolgu katsayıları)
- İmza SHA-256: `5f15d4e781c3afd1…` (değişmedi ✔ — v5.0 anahtar dizisi)
- APK md5: `ce49993b8e3ae1d255d83bc7aad8aba8`
- APK dosya boyutu: ~26 MB
- Kaynak zip (`kaynak-v10.46-yedek.zip`): ~12 MB

## Bilinçli kararlar / dürüstlük notları
- Android 8.0 (API 26) öncesi cihazlarda PiP ve RemoteAction desteklenmez; bu cihazlarda eski uyarı Toast mesajı korunur (`w45_pip_yok`).
- İleri sayım (`MODE_ILE`), Geri sayım (`MODE_DOWN`) ve Kronometre (`MODE_WATCH`) modlarının tümü PiP canlı kontrollerine bağlanmıştır. Geri sayımda 3 buton (Bekle/Sıfırla/+5dk), kronometre ve ileri sayımda 2 buton (Bekle/Sıfırla) görünür.
