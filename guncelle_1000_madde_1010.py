#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v10.90 — 1000+10 (1010) Madde Eksik & Gelişim Öneri Kataloğu ve Otomasyon Motoru Güncellemesi.
Kullanıcının "10 adet madde öner bunlardan farklı" talebi üzerine,
mevcut 1.000 maddenin üzerine 11. Kategori olarak 10 adet yepyeni, benzersiz
inovasyon maddesi (#1001 - #1010) ekler.
"""

with open("/home/user/uret_1000_madde.py", "r", encoding="utf-8") as f:
    code = f.read()

# 1. KATEGORILER içine 11. kategoriyi ekle
old_kategoriler = '    (10, "Depolama, Yedekleme, Arşiv & Sistem Teşhis")\n]'
new_kategoriler = '    (10, "Depolama, Yedekleme, Arşiv & Sistem Teşhis"),\n    (11, "Özel & Yeni Nesil İnovasyon Önerileri (#1001-#1010)")\n]'
if old_kategoriler in code and "(11," not in code:
    code = code.replace(old_kategoriler, new_kategoriler, 1)

# 2. ISIMLER_BY_KAT içine 11: [...] ekle
old_kat10_end = '        ("Sistem Teşhis ve Pil Raporu", "Uygulamanın pil ve bellek kullanım durumunu analiz edip raporlar.")\n    ]\n}'
new_kat11 = '''        ("Sistem Teşhis ve Pil Raporu", "Uygulamanın pil ve bellek kullanım durumunu analiz edip raporlar.")
    ],
    11: [
        ("NFC/QR Masa Çalışma İstasyonu Check-In (Fiziksel Masaya Dokundur-Başlat)", "Kullanıcının çalışma masasına yapıştırdığı bir NFC etiketine (veya QR koda) telefonu okuttuğu anda, sessiz modu aktifleştirerek otomatik olarak seçili dersin pomodoro sayacını başlatan fiziksel nesnelerin interneti (IoT) entegrasyonu."),
        ("Akustik Çevresel Ses Maskeleme & Pembe/Kahverengi Gürültü Jeneratörü (Ortam Filtresi)", "Sıradan odak sesleri veya radyo yayınları yerine, kafe, kütüphane veya trafik gürültüsünü iptal etmek için algoritmik olarak üretilen, kulak yormayan sürekli pembe (Pink Noise) ve kahverengi (Brown Noise) akustik dalga sentezleyicisi."),
        ("Dokunmatik Haptik (Titreşim) Ritim Metronomu & Sessiz Nabız Rehberi", "Sınav esnasında veya nefes egzersizi yaparken ekrana bakmadan veya ses çıkarmadan, telefonun doğrusal titreşim motoru vasıtasıyla bilek veya el ayasında hissedilen dakikada 60 vuruşluk sessiz dokunsal (haptic) zaman ölçer."),
        ("E-Mürekkep (E-Paper) / Göz Yormayan Saf Siyah Yüksek Kontrast Okuma Modu", "Ekrandaki tüm animasyonları, gölgeleri, gradyanları ve renkleri tek tuşla devre dışı bırakarak e-kitap okuyucu hissi veren, OLED ekranlarda sıfır pil tüketen saf siyah-beyaz ve kalın yazı tipine sahip ultra yüksek kontrast modu."),
        ("Yerel Ağda (Wi-Fi Direct / Hotspot) İnternetsiz Eş Zamanlı Sessiz Çalışma Odası", "Kütüphanede veya aynı evde çalışan arkadaşların internete bağlanmadan Wi-Fi Direct veya yerel Bluetooth mesh ağı üzerinden birbiriyle sessiz pomodoro senkronizasyonu kurabildiği merkeziyetsiz oda sistemi."),
        ("Göz Kırpma & Kamera Tabanlı Biyometrik Yorgunluk / Duraklama Algılayıcı (Opsiyonel)", "Ön kamerayı (tamamen cihaz içinde ve gizliliğe saygılı olarak) kullanarak kullanıcının ekrandan uzaklaştığını veya gözlerinin yorulduğunu saptadığında sayacı otomatik duraklatan ve göz dinlendirme uyarısı veren yapay görme asistanı."),
        ("Kilit Ekranı İçin Dinamik 'Günlük Motivasyon & Kalan Süre' Duvar Kağıdı Üreticisi", "Kullanıcının gün içindeki hedeflerini, güncel pomodoro sayısını ve çalışma serisini canlı olarak şık bir görsel infografik haline getirip telefonun kilit ekranı duvar kağıdına otomatik olarak işleyen statik resim motoru."),
        ("LaTeX & Matematik Formül Destekli Çevrimdışı Markdown Dışa Aktarma (Obsidian/Notion Uyumlu)", "Günlük Asistan'da alınan çalışma notlarının, çözülen soru tiplerinin ve alt konu başlıklarının, LaTeX matematiksel formüllerini ve grafik denklemlerini kaybetmeden tek tuşla Obsidian, Notion veya Logseq uyumlu Markdown klasörü halinde ZIP'lenip dışa aktarılması."),
        ("Ses Tanıma (Offline Voice Command) İle İnternetsiz Sesli Sayaç & Konu Komutları", "İnternet bağlantısı gerektirmeyen hafif cihaz içi ses tanıma algoritmalarıyla 'Sayacı Başlat', 'Mola Ver', 'Matematik Konusunu Aç' gibi Türkçe sesli komutları algılayarak eller serbest çalışma kontrolü sağlayan otomasyon."),
        ("Akıllı Saat (Wear OS) & Bileklik Mikro-Titreşim Senkronizasyon Arayüzü", "Telefon çentik uyarısı dahi vermeden pomodoro bitişinde sadece kullanıcının kolundaki akıllı saate veya bilekliğe hassas titreşim göndererek ders çalışırken çevredeki kimseyi rahatsız etmeden tam odak sağlayan giyilebilir teknoloji köprüsü.")
    ]
}'''
if old_kat10_end in code and "11: [" not in code:
    code = code.replace(old_kat10_end, new_kat11, 1)

# 3. uret_md_katalog() içindeki döngüyü güncelle
old_md_func = '''def uret_md_katalog():
    md = []
    md.append("# 1000-MADDE EKSİK & GELİŞİM ÖNERİ KATALOĞU (#1 - #1000)\\n")
    md.append("**Günlük Asistan (`com.gunlukasistan.app`) — Kapsamlı Mimari, Fonksiyonel, Yapay Zekâ ve Arayüz Öneri Listesi**\\n")
    md.append("Her madde hem bu katalogdan hem de uygulamadaki **Ayarlar > 📋 1000-Madde Kontrol Atölyesi (`BinMaddeKontrolActivity`)** üzerinden işaretlenebilir, seçilebilir ve çalıştırılabilir.\\n\\n---\\n")

    no = 1
    for kat_no, kat_adi in KATEGORILER:
        md.append(f"## Kategori {kat_no}: {kat_adi} (#{no} - #{no+99})\\n")
        sablonlar = ISIMLER_BY_KAT[kat_no]
        for i in range(100):
            s_idx = i % len(sablonlar)
            baslik, aciklama = sablonlar[s_idx]
            sifat = SIFATLAR[i % len(SIFATLAR)]
            ozel_baslik = f"{sifat} {baslik}" if i >= 10 else baslik
            md.append(f"- [ ] **#{no} — {ozel_baslik}:** {aciklama}\\n")
            no += 1
        md.append("\\n---\\n")

    md.append("**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**\\n")
    return "".join(md)'''

new_md_func = '''def uret_md_katalog():
    md = []
    md.append("# 1010-MADDE EKSİK & GELİŞİM ÖNERİ KATALOĞU (#1 - #1010)\\n")
    md.append("**Günlük Asistan (`com.gunlukasistan.app`) — Kapsamlı Mimari, Fonksiyonel, Yapay Zekâ ve Arayüz Öneri Listesi**\\n")
    md.append("Her madde hem bu katalogdan hem de uygulamadaki **Ayarlar > 📋 1000-Madde Kontrol Atölyesi (`BinMaddeKontrolActivity`)** üzerinden işaretlenebilir, seçilebilir ve çalıştırılabilir.\\n\\n---\\n")

    no = 1
    for kat_no, kat_adi in KATEGORILER:
        adet = 10 if kat_no == 11 else 100
        son_no = no + adet - 1
        md.append(f"## Kategori {kat_no}: {kat_adi} (#{no} - #{son_no})\\n")
        sablonlar = ISIMLER_BY_KAT[kat_no]
        for i in range(adet):
            s_idx = i % len(sablonlar)
            baslik, aciklama = sablonlar[s_idx]
            sifat = SIFATLAR[i % len(SIFATLAR)]
            ozel_baslik = f"{sifat} {baslik}" if (i >= 10 and kat_no != 11) else baslik
            md.append(f"- [ ] **#{no} — {ozel_baslik}:** {aciklama}\\n")
            no += 1
        md.append("\\n---\\n")

    md.append("**Günlük Asistan Geliştirici Ekibi · Avrupa/İstanbul**\\n")
    return "".join(md)'''

if old_md_func in code:
    code = code.replace(old_md_func, new_md_func, 1)

# 4. KATEGORI_ISIMLERI içine 11 to ... ekle
old_kat_map = '        10 to "10. Depolama, Yedek & Sistem"\n    )'
new_kat_map = '        10 to "10. Depolama, Yedek & Sistem",\n        11 to "11. Özel İnovasyonlar (#1001-1010)"\n    )'
if old_kat_map in code and '11 to "11.' not in code:
    code = code.replace(old_kat_map, new_kat_map, 1)

# 5. if kat_no < 10: -> if kat_no < len(KATEGORILER):
if "if kat_no < 10:" in code:
    code = code.replace("if kat_no < 10:", "if kat_no < len(KATEGORILER):")

# 6. tumMaddeleriGetir içindeki döngüleri güncelle
old_for_kat = "        for (katNo in 1..10) {"
new_for_kat = "        for (katNo in 1..11) {"
if old_for_kat in code:
    code = code.replace(old_for_kat, new_for_kat)

old_for_100 = '''            for (i in 0 until 100) {
                val sIdx = i % sablonlar.size
                val (bHam, aciklama) = sablonlar[sIdx]
                val sifat = SIFAT_LISTESI[i % SIFAT_LISTESI.size]
                val baslik = if (i >= 10) "$sifat $bHam" else bHam'''

new_for_100 = '''            val adet = if (katNo == 11) 10 else 100
            for (i in 0 until adet) {
                val sIdx = i % sablonlar.size
                val (bHam, aciklama) = sablonlar[sIdx]
                val sifat = SIFAT_LISTESI[i % SIFAT_LISTESI.size]
                val baslik = if (i >= 10 && katNo != 11) "$sifat $bHam" else bHam'''

if old_for_100 in code:
    code = code.replace(old_for_100, new_for_100)

# 7. kategoriyeGoreGetir güncelle
old_kat_filter = "if (kategoriNo <= 0 || kategoriNo > 10) return hepsi"
new_kat_filter = "if (kategoriNo <= 0 || kategoriNo > 11) return hepsi"
if old_kat_filter in code:
    code = code.replace(old_kat_filter, new_kat_filter)

# 8. seciliMaddeleriUygula güncelle
old_modul = "            if (m.kategoriNo == 1) {"
new_modul = "            if (m.kategoriNo == 1 || m.kategoriNo == 11) {"
if old_modul in code and "(m.kategoriNo == 1 || m.kategoriNo == 11)" not in code:
    code = code.replace(old_modul, new_modul)

with open("/home/user/uret_1000_madde.py", "w", encoding="utf-8") as f:
    f.write(code)

print("GÜNCELLEME TAMAMLANDI: /home/user/uret_1000_madde.py başarıyla 1010 madde (11 kategori) için ayarlandı.")
