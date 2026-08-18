#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""KOD-ATLASI üretici — projeyi tarar, gezinme dosyasını üretir."""
import io, re, glob, os, collections

P = "/home/user/GunlukAsistan/app/src"
JV = P + "/main/java/com/gunlukasistan/app"
TS = P + "/test/java/com/gunlukasistan/app"

def satirlar(yol):
    return io.open(yol, encoding="utf-8").read().splitlines()

O = []
O.append("# 🗺️ Günlük Asistan — KOD ATLASI (özellik → dosya haritası)")
O.append("")
O.append("_Bu dosya `atlas-uret.py` ile üretilir; elle değişen bölümler en alttadır. "
         "Amaç: geriye dönüşleri önlemek, aramadan kod yazmak, teslimi hızlandırmak._")
O.append("")

# ── Kaynak envanteri ──
kaynaklar = sorted(glob.glob(JV + "/*.kt"), key=lambda y: -os.path.getsize(y))
testler = sorted(os.path.basename(y) for y in glob.glob(TS + "/*.kt"))
O.append(f"## Envanter: **{len(kaynaklar)} ana kaynak · {len(testler)} test dosyası**")
O.append("")
O.append("| En büyük 15 dosya | satır |")
O.append("|---|---|")
for y in kaynaklar[:15]:
    ad = os.path.basename(y)
    n = sum(1 for _ in io.open(y, encoding="utf-8"))
    O.append(f"| `{ad}` | {n} |")
O.append("")

# ── Pref anahtarları ──
O.append("## SharedPreferences anahtarları (geri okuma/backup için kritik)")
O.append("")
O.append("| dosya | odak | pref adı |")
O.append("|---|---|---|")
prefler = []
for y in glob.glob(JV + "/*.kt"):
    ad = os.path.basename(y)
    for i, l in enumerate(satirlar(y)):
        m = re.search(r'private const val (PREF\w*|K_\w*) = "(gunluk_asistan\w+|[a-z_0-9]+_v\d|[a-z_]+_json)"', l)
        if m:
            prefler.append((ad, m.group(1), m.group(2)))
gorulen = set()
for ad, sabit, deger in sorted(prefler, key=lambda x: x[2]):
    if deger in gorulen: continue
    gorulen.add(deger)
    O.append(f"| `{ad}` | {sabit} | `{deger}` |")
O.append("")

# ── Dize dalga blokları ──
O.append("## strings.xml dalga önekleri")
O.append("")
s = io.open(P + "/main/res/values/strings.xml", encoding="utf-8").read()
wdalga = collections.Counter(re.findall(r'name="(w\d+)_', s))
O.append("· " + " · ".join(f"`{w}`×{n}" for w, n in sorted(wdalga.items(), key=lambda x: int(x[0][1:]))))
O.append("")

# ── Manifest bileşenleri ──
O.append("## Manifest bileşenleri (receiver/activity/widget)")
O.append("")
m = io.open(P + "/main/AndroidManifest.xml", encoding="utf-8").read()
for tur, desen in (("Receiver", r'android:name="\.(\w+)"[^>]*android:exported'), ("Activity", r'android:name="\.(\w+Activity)"')):
    bulunan = sorted(set(re.findall(desen, m)))
    O.append(f"- **{tur}** ({len(bulunan)}): " + ", ".join(f"`{x}`" for x in bulunan))
O.append("")

# ── Test eşlemesi ──
O.append("## Test ↔ kaynak eşlemesi")
O.append("")
esles = 0
for t in testler:
    base = t[:-7]  # Test.kt
    var = os.path.exists(JV + f"/{base}.kt")
    if var: esles += 1
    O.append(f"- `{t}` → {'✔ ' + base + '.kt' if var else '⚠ kaynağı YOK (saf araç testi olabilir)'}")
O.append(f"\n_Eşleşen: {esles}/{len(testler)}_")
O.append("")

# ── Küratörlü özellik haritası ──
OZELLIK = [
    ("Görevler (liste/ekleme/silme)", "TasksFragment.kt", "Store.loadTasks/saveTasks · tasks_json"),
    ("Görev alarmı", "AlarmScheduler.kt + GorevAlarmActivity.kt", "görev başına RTC_WAKEUP"),
    ("Görev erteleme sayacı", "GorevErteleme.kt", "ISO hafta · tiErtelemeSerit"),
    ("Bugün üstte sabitleme", "GorevBugunUstte.kt", "tiBugunUstte toggle"),
    ("Bekliyor ⏳", "SettingsFragment + TasksFragment", "gorev_bekliyor_v1/kume"),
    ("Not defteri", "NotesFragment.kt", "notes_json"),
    ("Not kilidi (PIN)", "NotKilit.kt", "not_kilit_v1/kilitli · kilit_v1/hash"),
    ("Not renk/etiket", "NotesFragment.kt", "not_renk_v1"),
    ("Not arşiv/sabitle/sürüm/hatırlatıcı", "NotesFragment.kt + NotHatirlatici.kt", "not_arsiv_v1 · not_sabitle_v1 · not_surum_v1 · not_hatirlat_v1"),
    ("Konular + alt maddeler", "TopicsFragment.kt", "topics_json"),
    ("Ders/PDF okuyucu", "PdfReaderActivity.kt + CoursesFragment.kt", ""),
    ("Alışkanlıklar", "HabitsFragment.kt", "habits_json + habit_log_json"),
    ("Alışkanlık mola 🏖", "AliskanlikMola.kt", "aliskanlik_mola_v1 · Store.habitStreak mola-atlamalı"),
    ("21 gün kuralı 🎓", "AliskanlikMola.kt (Kural21)", "habitKural21 bar"),
    ("Gün notu 📝", "AliskanlikNot.kt", "aliskanlik_not_v1 · halkaya uzun basış"),
    ("İkinci seri 🏅", "SeriAnaliz.kt + Store.habitTumGunler", "düzenleyicide satır"),
    ("Zamanlayıcı (geri sayım)", "TimerFragment.kt + TimerEngine.kt", "SayacServisi · TimerAlarm"),
    ("Zamanlayıcı bildirimi", "TimerNotifier.kt + TimerActionReceiver.kt", "zamanlayici_canli_v2 · kronometre"),
    ("İleri sayım", "TimerFragment MODE_ILE + IleriSayim.kt", "ileri_sayim_v1 · ileri_sayim_gecmis_v1"),
    ("İleri sayım bildirimi", "IleriSayimBildirim.kt + IleriSayimReceiver.kt", "ileri_sayim_canli_v1 kanal · chronometer"),
    ("Kadran görünümü", "SayacKadraniView.kt", "özel View · yaziOlcek ×SayacAyar.kadranOlcek"),
    ("Zamanlayıcı ayarları", "SayacAyar.kt + SayacAyarActivity.kt", "sayac_ayar_v1 · tiklanabilirSatir deseni"),
    ("Pomodoro/zincir", "Pomodoro.kt + SayacZincir.kt", ""),
    ("Odak sesleri", "SesManzarasi.kt + OdakKalkani.kt", "SayacAyarActivity manzara seçimi"),
    ("Odak skor/kayıt", "OdakKaydi.kt + Store.addTodayFocusMinutes", "gunluk odak dakikası"),
    ("Puan/koç", "KocActivity.kt + KocZamanlayici.kt", ""),
    ("Tam ekran sayaç", "FullscreenTimerActivity.kt", ""),
    ("Sayac bitti ekranı", "SayacBittiActivity.kt", "SayacErtele + SayacGeriKur"),
    ("Gün planı çerçevesi", "GunOdak.kt + GunOdakBildirim.kt", ""),
    ("Uyku zamanlayıcı", "UykuZamanla.kt", ""),
    ("Ezan/namaz vakitleri", "NamazWidget.kt + VakitPlan", "sayac widgetları"),
    ("Sağlık kontrolü (A-Z) 🩺", "SaglikActivity.kt + SaglikMotoru.kt", "Ayarlar rowSaglik · 21 madde"),
    ("AI asistan", "AiClient.kt + AiSettings.kt + AiOnbellek.kt", "ai_sohbet_v1 · sadeIstek"),
    ("Çökme raporu", "CokmeRapor.kt", "tekrarEdenler"),
    ("Bildirim merkezi/kanallar", "BildirimMerkezi.kt + BildirimZamanlayici.kt", "BildirimTani sağlık API"),
    ("Boot yeniden kurma", "BootReceiver.kt", "AlarmScheduler.rescheduleAll + CourseReminder + PlanAsistan.kur"),
    ("Sabah planı 🌅", "SabahReceiver.kt + GunlukBildirim.kt", "USER_PRESENT · plan_asistan_v1"),
    ("Akşam sorusu 🌙", "AksamReceiver.kt + PlanAsistan.kt", "AKSAM_PLAN alarmı · 22:00 vars."),
    ("Widget'lar", "WidgetCommon.kt + *Widget.kt", "OdakKutusuWidget · NamazWidget"),
    ("Yedek/geri yükleme", "PrefYedek.kt", "yedek taraması"),
    ("Çevrimiçi senkron", "OnlineActivity.kt + OnlineStore.kt", ""),
    ("Tema/yazı ölçeği", "ThemeManager.kt + GorunumAyar.kt", "gunluk_asistan_gorunum · fontScale"),
    ("Kutlama/titreşim", "Kutlama.kt + Titresim.kt", ""),
    ("Liste animasyonları", "ListeFark.kt", ""),
    ("Oturum planları", "SurecPlan.kt", "Oturum json kayıtları"),
]
O.append("## 🔎 Özellik → dosya haritası (küratörlü — koda dokunmadan önce buraya bak)")
O.append("")
O.append("| özellik | ana dosya(lar) | depo/çapa |")
O.append("|---|---|---|")
for a, b, c in OZELLIK:
    O.append(f"| {a} | `{b}` | {c} |")
O.append("")

# ── Elle bakım bölümü ──
O.append("""---

# ✍️ Elle bakım — hız desenleri (aramadan kod yaz)

## Teslim boru hattı (her sürüm, sırayla)
1. `bash /home/user/YEDEK/butunluk.sh` — 60 sn denetim (EKSIK satırı varsa çaresi satırda yazar)
2. Kaynak zipten geri yükleme: `unzip -oq /home/user/kaynak-v<SON>-yedek.zip -d /home/user/GunlukAsistan`
3. `.git` kayıpsa: `bash /home/user/YEDEK/git-sigorta.sh geri` (bundle'dan tam geçmiş)
4. Derleme: `bash /home/user/hizli-teslim.sh` (~13 dk, `hizli.log` sonunda `EXIT=0`)
5. Teslim: `bash /home/user/yukle-gofile.sh <apk> <zip> <notlar> <PROJE-DURUM.md>` → `yukle-sonuc.txt`
6. Tur sonu: `bash /home/user/YEDEK/git-sigorta.sh yenile` + `bash /home/user/YEDEK/kaynak-ayna.sh`

## Uzaktan kurtarma katmanları (geriye dönüş zinciri — sırayla dene)
1. **gofile** (kullanıcıya link; biz API'den indiremiyoruz `error-notPremium`)
2. **filebin kaynak aynası** `kaynak-gunluk-*` (YEDEK/kaynak-ayna.sh indir — md5 kanıtlı, ~6 gün ömür, her tura başlarken tazelenir)
3. **git bundle** `YEDEK/repo.gitbundle` (tam geçmiş)
4. **yerel zip** `/home/user/kaynak-v*-yedek.zip` (çoğu kayıpta hayatta kalır)

## Yeni özellik kalıpları (kanıtlanmış 6 desen)
- **Saf motor + test**: `object X` üstte android'siz fonksiyonlar → `XTest.kt` (org.json JVM'de gerçek!)
- **Ayar**: `SayacAyar` deseni — `private const val PREF`, get/set, `kadranCarpani` gibi saf eşleme + `SayacAyarActivity`'ye `tiklanabilirSatir`/`anahtarSatiri` + `ciz()`
- **Bildirim aksiyonu**: `TimerNotifier` deseni — kanal + `PendingIntent.getBroadcast(...setPackage)` + manifest `<receiver exported="false"><intent-filter><action/>`
- **Günlük alarm**: `PlanAsistan.kur` deseni — setExact/AndAllowWhileIdle RTC_WAKEUP + tetikte **yeniden kur** + `BootReceiver` kancası
- **Ayarlar satırı**: `fragment_settings.xml` `rowSaglik` klonu (emoji + başlık @dimen/ga_yazi_orta + alt @dimen/ga_yazi_kucuk + "›") — ham `Nsp` YASAK (TasarimOlcegiTest), hep `@dimen/ga_yazi_*`
- **Fragment dizesi**: `strings.xml` sonuna `w<XX>_` önekli blok — `%1$d` formatlı, lint güvenli (çıplak `%` YOK)

## Bilinen kırmızılar (2 kez yaşandı, üçüncüyü yaşama)
- Expression-body fonksiyonda `return` kullanılamaz
- KDoc köşeli parantez `[x..y]` dok-link sanılır → derleme hatası
- Parametre adını uydurma: önce `grep "fun f(" dosya` ile gerçek imzayı oku
- Snapshot kaybı: tur sonuna doğru yazılan dosyalar uçabilir → her tur SONU `git-sigorta.sh yenile` + kaynak-ayna

## Sürüm defteri (teslim linkleri)
Ayrıntılı tablo: `YEDEK/BAGLANTI-DEFTERI.md` — her sürümde APK/zip/notlar/md5 satırı.
""")

cikti = "\n".join(O)
io.open("/home/user/DEVIR/KOD-ATLASI.md", "w", encoding="utf-8").write(cikti)
print("ATLAS_UZUNLUK", len(cikti))
