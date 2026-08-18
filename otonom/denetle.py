#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Günlük Asistan — Otonom Kalite Denetleyici
===========================================
Kaynak kodu tarar, hata ve iyileştirme fırsatlarını bulur, önceliklendirir.
Her turda çalıştırılır; çıktısı bir sonraki geliştirmenin yol haritası olur.

Kullanım:  python3 otonom/denetle.py [--json]
"""
import os, re, sys, json, glob
from collections import defaultdict

SRC = "/home/user/GunlukAsistan/app/src/main"
JAVA = f"{SRC}/java/com/gunlukasistan/app"
RES = f"{SRC}/res"

BULGULAR = []


def bulgu(onem, kategori, dosya, satir, mesaj, oneri=""):
    BULGULAR.append({
        "onem": onem,          # 1=kritik 2=yuksek 3=orta 4=dusuk
        "kategori": kategori,
        "dosya": os.path.basename(dosya) if dosya else "-",
        "satir": satir,
        "mesaj": mesaj,
        "oneri": oneri,
    })


def kt_dosyalar():
    return sorted(glob.glob(f"{JAVA}/*.kt"))


# ─────────────────────────── 1. ÇÖKME RİSKLERİ ───────────────────────────

def denetle_cokme():
    """Uygulamayı çökertebilecek desenleri arar."""
    riskli = [
        (r'\.get\((\d+)\)', "Dizin denetimi olmadan get() — IndexOutOfBounds riski"),
        (r'!!', "!! operatörü — NullPointerException riski"),
        (r'\.first\(\)(?!O)', "first() boş listede çöker — firstOrNull() kullan"),
        (r'\.last\(\)(?!O)', "last() boş listede çöker — lastOrNull() kullan"),
        (r'\.single\(\)', "single() çöker — singleOrNull() kullan"),
        (r'(text|metin|input|str|deger|value|arg)\w*\.toInt\(\)(?!OrNull)',
         "String.toInt() geçersiz metinde çöker — toIntOrNull()"),
        (r'(text|metin|input|str|deger|value|arg)\w*\.toLong\(\)(?!OrNull)',
         "String.toLong() çöker — toLongOrNull()"),
    ]
    for f in kt_dosyalar():
        satirlar = open(f, encoding="utf-8").read().split("\n")
        for i, s in enumerate(satirlar, 1):
            kod = s.split("//")[0]
            if not kod.strip() or kod.strip().startswith("*"):
                continue
            for desen, mesaj in riskli:
                if re.search(desen, kod):
                    # try/catch içinde mi kabaca bak
                    pencere = "\n".join(satirlar[max(0, i - 12):i + 3])
                    korumali = ("try {" in pencere or "?:" in kod or "catch" in pencere
                                or "isEmpty()" in kod or "isNotEmpty()" in kod
                                or "!= null" in kod or "?." in kod
                                or re.search(r'if \([^)]*isEmpty', pencere))
                    if not korumali:
                        bulgu(1, "COKME", f, i, mesaj, "Güvenli sürümle değiştir veya try/catch ekle")


def denetle_substring():
    """Uzunluk denetimi olmadan substring."""
    for f in kt_dosyalar():
        satirlar = open(f, encoding="utf-8").read().split("\n")
        for i, s in enumerate(satirlar, 1):
            kod = s.split("//")[0]
            if ".substring(" not in kod:
                continue
            pencere = "\n".join(satirlar[max(0, i - 8):i + 2])
            if any(k in pencere for k in ["length", "size", "try {", "coerce", "take(", "isEmpty", "isBlank"]):
                continue
            bulgu(2, "COKME", f, i, "substring() uzunluk denetimi olmadan kullanılmış",
                  "length kontrolü ekle veya take()/drop() kullan")


def denetle_kaynak_sizinti():
    """Kapatılmayan kaynaklar."""
    acilis = [("PdfRenderer(", "close()"), ("ParcelFileDescriptor.open", "close()"),
              ("Bitmap.createBitmap", "recycle()")]
    for f in kt_dosyalar():
        icerik = open(f, encoding="utf-8").read()
        for ac, kapat in acilis:
            if ac in icerik and kapat not in icerik:
                bulgu(2, "SIZINTI", f, 0,
                      f"{ac} açılıyor ama {kapat} görünmüyor",
                      f"onDestroy içinde {kapat} çağır")


def denetle_anr():
    """Ana iş parçacığında ağır işlem."""
    agir = ["Thread.sleep(", "URL(", "openConnection", "readBytes()"]
    for f in kt_dosyalar():
        icerik = open(f, encoding="utf-8").read()
        if "Fragment" not in icerik and "Activity" not in icerik:
            continue
        for a in agir:
            if a in icerik and "Thread {" not in icerik and "coroutine" not in icerik.lower() \
               and "executor" not in icerik.lower() and "Handler" not in icerik:
                bulgu(2, "ANR", f, 0,
                      f"UI sınıfında '{a}' — arka plana alınmamış olabilir",
                      "Thread veya Executor içine taşı")
                break


# ─────────────────────────── 2. KAYNAK TUTARLILIĞI ───────────────────────────

def denetle_stringler():
    """Kodda geçen R.string.X gerçekten tanımlı mı."""
    tanimli = set()
    for sf in glob.glob(f"{RES}/values/strings.xml"):
        tanimli |= set(re.findall(r'<string name="([^"]+)"', open(sf, encoding="utf-8").read()))
    kullanilan = set()
    for f in kt_dosyalar() + glob.glob(f"{RES}/layout/*.xml"):
        icerik = open(f, encoding="utf-8").read()
        kullanilan |= set(re.findall(r'R\.string\.(\w+)', icerik))
        kullanilan |= set(re.findall(r'@string/(\w+)', icerik))
    eksik = kullanilan - tanimli
    for e in sorted(eksik):
        bulgu(1, "KAYNAK", "strings.xml", 0, f"R.string.{e} kullanılıyor ama TANIMSIZ",
              f'<string name="{e}">...</string> ekle')
    kullanilmayan = tanimli - kullanilan
    if len(kullanilmayan) > 25:
        bulgu(4, "TEMIZLIK", "strings.xml", 0,
              f"{len(kullanilmayan)} string kullanılmıyor", "Gereksizleri temizle")


def denetle_layout_id():
    """findViewById ile aranan id layout'ta var mı."""
    tum_id = set()
    for alt in ("layout", "menu", "xml", "navigation"):
        for lf in glob.glob(f"{RES}/{alt}/*.xml"):
            tum_id |= set(re.findall(r'android:id="@\+id/(\w+)"', open(lf, encoding="utf-8").read()))
    for f in kt_dosyalar():
        icerik = open(f, encoding="utf-8").read()
        for m in re.finditer(r'R\.id\.(\w+)', icerik):
            if m.group(1) not in tum_id:
                bulgu(1, "KAYNAK", f, 0, f"R.id.{m.group(1)} hiçbir layout'ta yok",
                      "Layout'a ekle veya kodu düzelt")


def denetle_manifest():
    """Manifest ile sınıf eşleşmesi."""
    mf = f"{SRC}/AndroidManifest.xml"
    if not os.path.exists(mf):
        return
    icerik = open(mf, encoding="utf-8").read()
    kayitli = set(re.findall(r'android:name="\.(\w+)"', icerik))
    mevcut = set()
    for f in kt_dosyalar():
        mevcut.add(os.path.basename(f)[:-3])
        mevcut |= set(re.findall(r'^\s*(?:open |abstract |final )?class (\w+)',
                                 open(f, encoding="utf-8").read(), re.M))
    for k in kayitli:
        if k not in mevcut:
            bulgu(1, "MANIFEST", "AndroidManifest.xml", 0,
                  f"Manifest'te .{k} kayıtlı ama sınıf yok", "Sınıfı ekle veya kaydı sil")
    # Activity/Service olup manifest'te olmayanlar
    for f in kt_dosyalar():
        ad = os.path.basename(f)[:-3]
        ic = open(f, encoding="utf-8").read()
        if re.search(r'class \w+\s*:\s*\w*(AppCompatActivity|Activity)\b', ic) and ad not in kayitli:
            bulgu(1, "MANIFEST", f, 0, f"{ad} bir Activity ama manifest'te YOK",
                  "<activity> etiketi ekle")
        if re.search(r'class \w+\s*:\s*\w*(Service|TileService)\b', ic) and ad not in kayitli:
            bulgu(1, "MANIFEST", f, 0, f"{ad} bir Service ama manifest'te YOK",
                  "<service> etiketi ekle")


# ─────────────────────────── 3. KULLANICI DENEYİMİ ───────────────────────────

def denetle_ux():
    """Kullanıcı deneyimi eksikleri."""
    for f in kt_dosyalar():
        ad = os.path.basename(f)
        ic = open(f, encoding="utf-8").read()
        # Liste ekranında boş durum var mı
        if "RecyclerView" in ic and "Fragment" in ic:
            if "empty" not in ic.lower() and "Empty" not in ic and "bos" not in ic.lower():
                bulgu(3, "UX", f, 0, "Liste ekranı — boş durum mesajı yok",
                      "Liste boşken açıklayıcı bir mesaj göster")
        # Silme onayı — yalnızca UI sınıflarında anlamlı
        ui_sinifi = ("Fragment" in ic or "AppCompatActivity" in ic)
        if ui_sinifi and re.search(r'Store\.delete\w+\(', ic):
            if "setPositiveButton" not in ic and "confirm" not in ic.lower():
                bulgu(2, "UX", f, 0, "Silme işlemi — onay penceresi yok",
                      "MaterialAlertDialogBuilder ile onay iste")
        # Geri alma
        if (ui_sinifi and "Snackbar" not in ic
                and re.search(r'Store\.delete\w+\(', ic)
                and "Undoable" not in ic):
            bulgu(3, "UX", f, 0, "Silme sonrası geri alma yok",
                  "Snackbar ile 'Geri Al' sun")
        # Erişilebilirlik
        if "ImageView" in ic and "contentDescription" not in ic:
            pass  # layout'ta kontrol edilir

    # layout erişilebilirlik
    for lf in glob.glob(f"{RES}/layout/*.xml"):
        ic = open(lf, encoding="utf-8").read()
        imgs = len(re.findall(r'<ImageView', ic))
        desc = len(re.findall(r'contentDescription', ic))
        if imgs > desc:
            bulgu(4, "ERISILEBILIRLIK", lf, 0,
                  f"{imgs - desc} ImageView'da contentDescription yok",
                  "Ekran okuyucu için açıklama ekle")


def denetle_performans():
    """Performans sorunları."""
    for f in kt_dosyalar():
        ic = open(f, encoding="utf-8").read()
        # onBindViewHolder içinde ağır işlem
        m = re.search(r'onBindViewHolder[\s\S]{0,900}', ic)
        if m:
            govde = m.group(0)
            for agir, mesaj in [("Store.load", "Store.load* çağrısı"),
                                ("SimpleDateFormat(", "SimpleDateFormat oluşturma"),
                                ("Regex(", "Regex derleme")]:
                if agir in govde:
                    bulgu(2, "PERFORMANS", f, 0,
                          f"onBindViewHolder içinde {mesaj} — her satırda tekrarlanır",
                          "Dışarı taşı veya önbelleğe al")
        # notifyDataSetChanged aşırı kullanımı
        n = ic.count("notifyDataSetChanged()")
        if n >= 4:
            bulgu(3, "PERFORMANS", f, 0,
                  f"notifyDataSetChanged() {n} kez — tüm listeyi yeniden çizer",
                  "DiffUtil veya notifyItemChanged kullan")


def denetle_veri_guvenligi():
    """Veri kaybı riskleri."""
    store = f"{JAVA}/Store.kt"
    if os.path.exists(store):
        ic = open(store, encoding="utf-8").read()
        if ".apply()" in ic and ".commit()" not in ic:
            bulgu(3, "VERI", store, 0,
                  "SharedPreferences hep apply() — kritik yazımda commit() güvenli",
                  "Yedekleme/kritik anlarda commit() kullan")
        if "exportJson" in ic and "surum" not in ic and "version" not in ic.lower():
            bulgu(3, "VERI", store, 0,
                  "Yedek JSON'da sürüm bilgisi yok — ileride geri yükleme sorun olur",
                  "exportJson içine sürüm alanı ekle")


# ─────────────────────────── 4. EKSİK ÖZELLİKLER ───────────────────────────

def denetle_eksik_ozellik():
    """Kullanıcı için değerli olabilecek eksikler."""
    tum = ""
    for f in kt_dosyalar():
        tum += open(f, encoding="utf-8").read()

    kontroller = [
        ("arama", ["SearchView", "filter(", "arama"], 2,
         "Ders/kurs araması yok", "Kurs ekranına arama kutusu ekle"),
        ("yer_imi", ["bookmark", "yerImi", "favori", "toggleLessonFav", "favLessons"], 3,
         "Ders yer imi/favori yok", "Beğenilen dersleri işaretleme ekle"),
        ("okuma_ilerlemesi", ["lastPage", "sonSayfa", "readProgress"], 2,
         "PDF'te kaldığın sayfa hatırlanmıyor", "Son okunan sayfayı kaydet"),
        ("gece_modu_pdf", ["invert", "geceModu", "darkPdf", "nightMode", "nightFilter"], 3,
         "PDF gece modu yok", "PDF'i koyu temada gösterme seçeneği"),
        ("yakinlastirma", ["ScaleGestureDetector"], 2,
         "PDF'te parmakla yakınlaştırma yok", "Pinch-zoom ekle"),
        ("paylas_ders", ["ACTION_SEND", "shareLesson"], 4,
         "Ders PDF'ini paylaşma yok", "PDF'i dışa aktar/paylaş"),
        ("kurs_ilerleme_bildirim", ["courseReminder", "kursHatirlat", "CourseReminderReceiver"], 4,
         "Kurs çalışma hatırlatıcısı yok", "Günlük ders hatırlatması"),
    ]
    for anahtar, izler, onem, mesaj, oneri in kontroller:
        if not any(iz in tum for iz in izler):
            bulgu(onem, "OZELLIK", "-", 0, mesaj, oneri)


# ─────────────────────────── 5. KOD KALİTESİ ───────────────────────────

def denetle_kalite():
    for f in kt_dosyalar():
        ic = open(f, encoding="utf-8").read()
        satir = ic.count("\n")
        if satir > 900:
            bulgu(4, "KALITE", f, 0, f"{satir} satır — çok büyük dosya",
                  "Mantıksal parçalara böl")
        bos_catch = len(re.findall(r'catch\s*\([^)]*\)\s*\{\s*\}', ic))
        if bos_catch > 6:
            bulgu(3, "KALITE", f, 0, f"{bos_catch} boş catch bloğu — hatalar sessizce yutuluyor",
                  "En azından log yaz")
        for m in re.finditer(r'"([^"]{25,})"', ic):
            metin = m.group(1)
            if re.search(r'[çğıöşüÇĞİÖŞÜ]', metin) and "R.string" not in metin:
                bulgu(4, "YERELLESTIRME", f, 0,
                      f'Koda gömülü Türkçe metin: "{metin[:40]}..."',
                      "strings.xml'e taşı")
                break


# ─────────────────────────── RAPOR ───────────────────────────

def main():
    for fn in [denetle_cokme, denetle_substring, denetle_kaynak_sizinti, denetle_anr,
               denetle_stringler, denetle_layout_id, denetle_manifest,
               denetle_ux, denetle_performans, denetle_veri_guvenligi,
               denetle_eksik_ozellik, denetle_kalite]:
        try:
            fn()
        except Exception as e:
            print(f"  [denetim hatası] {fn.__name__}: {e}")

    global BULGULAR
    _gorulen = set()
    _tekil = []
    for b in BULGULAR:
        anahtar = (b["kategori"], b["dosya"], b["mesaj"])
        if anahtar in _gorulen:
            continue
        _gorulen.add(anahtar)
        _tekil.append(b)
    BULGULAR = _tekil
    BULGULAR.sort(key=lambda b: (b["onem"], b["kategori"]))

    if "--json" in sys.argv:
        print(json.dumps(BULGULAR, ensure_ascii=False, indent=1))
        return

    say = defaultdict(int)
    for b in BULGULAR:
        say[b["onem"]] += 1

    print("=" * 72)
    print("  GÜNLÜK ASİSTAN — OTONOM KALİTE DENETİMİ")
    print("=" * 72)
    print(f"  KRİTİK: {say[1]}   YÜKSEK: {say[2]}   ORTA: {say[3]}   DÜŞÜK: {say[4]}")
    print(f"  TOPLAM: {len(BULGULAR)} bulgu")
    print("=" * 72)

    etiket = {1: "KRİTİK", 2: "YÜKSEK", 3: "ORTA", 4: "DÜŞÜK"}
    son_onem = None
    for b in BULGULAR:
        if b["onem"] != son_onem:
            print(f"\n### {etiket[b['onem']]} ###")
            son_onem = b["onem"]
        yer = f"{b['dosya']}:{b['satir']}" if b["satir"] else b["dosya"]
        print(f"  [{b['kategori']:16s}] {yer}")
        print(f"      {b['mesaj']}")
        if b["oneri"]:
            print(f"      → {b['oneri']}")

    with open("/home/user/otonom/son_rapor.json", "w", encoding="utf-8") as fp:
        json.dump(BULGULAR, fp, ensure_ascii=False, indent=1)
    print(f"\n  Rapor: /home/user/otonom/son_rapor.json")


if __name__ == "__main__":
    main()
