package com.gunlukasistan.app

/**
 * v7.4 — İnşaat mühendisliği yazılım kursları için hazır içerik paketi.
 *
 * Yapı: Kurs → Bölüm → Ders (Udemy düzeni)
 * Kullanıcı bunların üzerine kendi kurslarını/bölümlerini/derslerini ekleyebilir.
 */
object CoursePack {

    /** title, emoji, renkIndeksi, açıklama, bölümler */
    data class PackCourse(
        val title: String,
        val emoji: String,
        val colorIndex: Int,
        val desc: String,
        val sections: List<PackSection>
    )

    /** Bölüm: başlık + ders listesi (ders = başlık|dakika|açıklama) */
    data class PackSection(val title: String, val lessons: List<Triple<String, Int, String>>)

    private fun l(title: String, min: Int, desc: String = "") = Triple(title, min, desc)

    val courses: List<PackCourse> = listOf(

        // ───────────────────────────── AutoCAD 2D ─────────────────────────────
        PackCourse(
            "AutoCAD 2D — Temelden İleri Seviyeye", "📐", 0,
            "Teknik çizimin temeli. Plan, kesit, görünüş çizimi ve kağıda çıktı alma.",
            listOf(
                PackSection(
                    "1. Tanışma ve Arayüz", listOf(
                        l("AutoCAD nedir, inşaatta nerede kullanılır", 12,
                            "Sürüm farkları, LT vs tam sürüm, sistem gereksinimleri"),
                        l("Arayüz turu: Ribbon, komut satırı, model/layout", 18,
                            "Komut satırı en önemli araç — her komut oradan yazılabilir"),
                        l("Fare ve klavye kısayolları", 15,
                            "Orta tuş pan, tekerlek zoom, çift tık zoom extents"),
                        l("Çizim birimlerini ayarlama (UNITS)", 10,
                            "İnşaatta genelde milimetre veya santimetre çalışılır"),
                        l("Dosya biçimleri: DWG, DXF, DWT, DWS", 12,
                            "Şablon dosyası (DWT) oluşturmanın önemi")
                    )
                ),
                PackSection(
                    "2. Temel Çizim Komutları", listOf(
                        l("LINE, PLINE, RAY, XLINE", 20, "Polyline ile çizmenin avantajları"),
                        l("CIRCLE, ARC, ELLIPSE", 16, ""),
                        l("RECTANGLE ve POLYGON", 12, ""),
                        l("Koordinat girişi: mutlak, bağıl, kutupsal", 22,
                            "@100,50 ve @100<45 kullanımı — hız için şart"),
                        l("Dinamik giriş (DYN) kullanımı", 10, ""),
                        l("SPLINE ve serbest eğriler", 12, "")
                    )
                ),
                PackSection(
                    "3. Yardımcı Araçlar (Precision)", listOf(
                        l("OSNAP — nesne kenetleme noktaları", 20,
                            "Endpoint, Midpoint, Intersection, Perpendicular en çok kullanılanlar"),
                        l("ORTHO ve POLAR takip", 12, ""),
                        l("OTRACK — nesne izleme", 14, ""),
                        l("GRID ve SNAP ayarları", 10, ""),
                        l("Referans çizgileri ile hassas çizim", 15, "")
                    )
                ),
                PackSection(
                    "4. Düzenleme Komutları", listOf(
                        l("MOVE, COPY, ROTATE, SCALE", 20, ""),
                        l("TRIM, EXTEND, FILLET, CHAMFER", 22,
                            "Fillet yarıçapı 0 verilerek köşe birleştirme"),
                        l("OFFSET — duvar çizmenin en hızlı yolu", 15, ""),
                        l("MIRROR ve simetrik çizim", 12, ""),
                        l("ARRAY: dikdörtgen, kutupsal, yol boyunca", 20,
                            "Kolon akslarını dizmek için birebir"),
                        l("STRETCH ile toplu boyut değiştirme", 14, ""),
                        l("Grip (tutamak) düzenleme", 12, "")
                    )
                ),
                PackSection(
                    "5. Katmanlar ve Nesne Özellikleri", listOf(
                        l("LAYER mantığı ve katman yöneticisi", 22,
                            "Duvar, kapı, kot, tarama, mobilya ayrı katmanlarda olmalı"),
                        l("Renk, çizgi tipi, çizgi kalınlığı", 18,
                            "Çizgi kalınlığı baskıda kalite farkı yaratır"),
                        l("ByLayer / ByBlock mantığı", 15, ""),
                        l("Katman filtreleri ve durum yöneticisi", 12, ""),
                        l("MATCHPROP — özellik kopyalama", 8, "")
                    )
                ),
                PackSection(
                    "6. Yazı, Ölçülendirme ve Tarama", listOf(
                        l("TEXT ve MTEXT, yazı stilleri", 18, ""),
                        l("Ölçülendirme stili oluşturma (DIMSTYLE)", 25,
                            "Ölçek ile ölçü yazısı boyutu ilişkisi kritik"),
                        l("Doğrusal, açısal, yarıçap ölçülendirme", 20, ""),
                        l("Sürekli (continue) ve temel (baseline) ölçü", 15, ""),
                        l("HATCH — kesit ve malzeme taraması", 20,
                            "Betonarme, dolgu, toprak taramaları"),
                        l("LEADER ve balon numaralandırma", 12, "")
                    )
                ),
                PackSection(
                    "7. Bloklar ve Kütüphane", listOf(
                        l("BLOCK oluşturma ve INSERT", 20, ""),
                        l("Öznitelikli (attribute) bloklar", 22,
                            "Kapı-pencere listesi otomatik çıkarmak için"),
                        l("Dinamik bloklar", 25, ""),
                        l("WBLOCK ve kendi kütüphaneni kurma", 15, ""),
                        l("DesignCenter ve Tool Palettes", 14, "")
                    )
                ),
                PackSection(
                    "8. Layout, Ölçek ve Baskı", listOf(
                        l("Model Space vs Paper Space", 20,
                            "En çok karıştırılan konu — 1/1 modelde çiz, layout'ta ölçekle"),
                        l("Viewport oluşturma ve ölçek verme", 22,
                            "1/50, 1/100, 1/200 mimari ölçekler"),
                        l("Antet (paftalık) hazırlama", 18, ""),
                        l("Çizim ölçeği ve annotative nesneler", 20, ""),
                        l("PLOT — yazıcı ve PDF çıktısı", 20,
                            "CTB dosyası ile çizgi kalınlığı yönetimi"),
                        l("Toplu çıktı (PUBLISH) ve sayfa kurulumu", 15, "")
                    )
                ),
                PackSection(
                    "9. İnşaat Uygulamaları", listOf(
                        l("Uygulama: Kat planı çizimi (baştan sona)", 45,
                            "Akslar → duvarlar → kapı/pencere → ölçülendirme"),
                        l("Uygulama: Kesit ve görünüş çıkarma", 40, ""),
                        l("Uygulama: Kalıp planı çizimi", 40,
                            "Kolon, kiriş, döşeme gösterimi"),
                        l("Uygulama: Vaziyet planı ve kotlar", 30, ""),
                        l("Uygulama: Merdiven detayı", 35, ""),
                        l("Metraj için alan hesabı (AREA, BOUNDARY)", 20, "")
                    )
                ),
                PackSection(
                    "10. Verimlilik ve İleri Konular", listOf(
                        l("XREF — harici referans dosyalar", 22,
                            "Ekip çalışmasında mimari-statik koordinasyonu"),
                        l("Sheet Set Manager", 20, ""),
                        l("Tablolar (TABLE) ve Excel bağlantısı", 18, ""),
                        l("Alan hesabı ve veri çıkarma (DATAEXTRACTION)", 20, ""),
                        l("Kendi kısayollarını tanımlama (PGP dosyası)", 12,
                            "Sık kullandığın komutları tek harfe indir"),
                        l("Script ve LISP'e giriş", 20, ""),
                        l("Çizim temizleme: PURGE, AUDIT, OVERKILL", 15,
                            "Dosya boyutunu küçültür, hataları giderir")
                    )
                )
            )
        ),

        // ───────────────────────────── Revit / BIM ─────────────────────────────
        PackCourse(
            "Revit — BIM ile Yapı Modelleme", "🏗️", 3,
            "3B model üzerinden plan, kesit, görünüş ve metrajın otomatik üretilmesi.",
            listOf(
                PackSection(
                    "1. BIM ve Revit'e Giriş", listOf(
                        l("BIM nedir, CAD'den farkı ne", 18,
                            "Çizim değil model — bir yeri değiştirince her yer güncellenir"),
                        l("Revit arayüzü ve proje tarayıcısı", 20, ""),
                        l("Proje şablonu ve birim ayarları", 15, ""),
                        l("Aile (Family) kavramı: sistem, yüklenebilir, yerinde", 22, ""),
                        l("Seviyeler (Levels) ve akslar (Grids)", 20,
                            "Her şeyin temeli — önce bunlar kurulur")
                    )
                ),
                PackSection(
                    "2. Mimari Modelleme", listOf(
                        l("Duvar oluşturma ve katman yapısı", 25, ""),
                        l("Kapı ve pencere yerleştirme", 18, ""),
                        l("Döşeme (Floor) ve boşluklar", 20, ""),
                        l("Çatı modelleme", 22, ""),
                        l("Merdiven ve korkuluk", 25, ""),
                        l("Tavan ve iç mekan öğeleri", 18, ""),
                        l("Perde duvar (Curtain Wall)", 22, "")
                    )
                ),
                PackSection(
                    "3. Yapısal Modelleme", listOf(
                        l("Yapısal kolon ve kirişler", 25, ""),
                        l("Temeller: tekil, sürekli, radye", 22, ""),
                        l("Döşeme ve perde duvarlar", 20, ""),
                        l("Donatı (rebar) modelleme", 30, ""),
                        l("Yapısal çerçeve ve bağlantılar", 22, ""),
                        l("Analitik model ve statik programa aktarım", 25,
                            "Revit ↔ Robot / SAP2000 köprüsü")
                    )
                ),
                PackSection(
                    "4. Görünümler ve Paftalar", listOf(
                        l("Plan, kesit, görünüş, 3B görünüm", 22, ""),
                        l("Görünüm şablonları ve görünürlük ayarları", 20, ""),
                        l("Detay görünümleri ve çağrı (callout)", 18, ""),
                        l("Ölçülendirme ve etiketleme (tag)", 20, ""),
                        l("Pafta oluşturma ve antet", 20, ""),
                        l("Baskı ve DWG/PDF dışa aktarma", 18, "")
                    )
                ),
                PackSection(
                    "5. Metraj ve Tablolar", listOf(
                        l("Schedule (tablo) oluşturma mantığı", 25,
                            "Revit'in en güçlü yanı — model değişince metraj da değişir"),
                        l("Malzeme metrajı (Material Takeoff)", 22, ""),
                        l("Kapı-pencere listeleri", 15, ""),
                        l("Beton ve donatı metrajı", 25, ""),
                        l("Hesaplanmış parametreler ve formüller", 22, ""),
                        l("Excel'e aktarma", 12, "")
                    )
                ),
                PackSection(
                    "6. Aile (Family) Oluşturma", listOf(
                        l("Aile editörü ve şablon seçimi", 20, ""),
                        l("Referans düzlemler ve parametreler", 25, ""),
                        l("Katı model araçları: extrusion, sweep, blend", 25, ""),
                        l("Tip ve örnek parametreleri", 20, ""),
                        l("Paylaşılan parametreler", 18, ""),
                        l("Uygulama: Özel kolon ailesi yapımı", 30, "")
                    )
                ),
                PackSection(
                    "7. Ekip Çalışması ve Koordinasyon", listOf(
                        l("Worksharing ve merkezi model", 25, ""),
                        l("Bağlantılı modeller (Link Revit/CAD/IFC)", 22, ""),
                        l("Çakışma denetimi (Interference Check)", 20,
                            "Mimari-statik-mekanik çakışmalarını bulma"),
                        l("Kopyala/İzle (Copy-Monitor)", 18, ""),
                        l("Revizyon yönetimi", 15, ""),
                        l("IFC ve açık BIM standartları", 20, "")
                    )
                ),
                PackSection(
                    "8. İleri Konular", listOf(
                        l("Dynamo ile görsel programlama", 35, ""),
                        l("Kütle (Mass) ve kavramsal tasarım", 25, ""),
                        l("Faz ve tasarım seçenekleri", 20, ""),
                        l("Navisworks ile 4B/5B", 25, "Zaman ve maliyet boyutu"),
                        l("Nokta bulutu (point cloud) ile mevcut yapı", 22, "")
                    )
                )
            )
        ),

        // ───────────────────────────── SAP2000 ─────────────────────────────
        PackCourse(
            "SAP2000 — Yapısal Analiz ve Tasarım", "🧮", 4,
            "Çerçeve, kafes, kabuk sistemlerin çözümü; deprem ve rüzgâr yükleri.",
            listOf(
                PackSection(
                    "1. Giriş ve Temel Kavramlar", listOf(
                        l("SAP2000 arayüzü ve birim sistemi", 18, ""),
                        l("Sonlu elemanlar yöntemine kısa bakış", 25, ""),
                        l("Model oluşturma yolları: şablon, ızgara, içe aktarma", 20, ""),
                        l("Malzeme tanımlama (beton, çelik)", 18,
                            "TS 500 ve Eurocode malzeme sınıfları"),
                        l("Kesit tanımlama (frame section)", 22, "")
                    )
                ),
                PackSection(
                    "2. Modelleme", listOf(
                        l("Düğüm ve çubuk eleman girişi", 22, ""),
                        l("Mesnet koşulları (restraints)", 20,
                            "Ankastre, sabit, hareketli — doğru seçim şart"),
                        l("Rijit bağlantı ve mafsal (releases)", 18, ""),
                        l("Kabuk (shell) elemanlar: döşeme ve perde", 25, ""),
                        l("Rijit diyafram tanımı", 20,
                            "Deprem analizinde döşeme davranışı"),
                        l("Grup tanımlama ve seçim teknikleri", 15, "")
                    )
                ),
                PackSection(
                    "3. Yükler", listOf(
                        l("Yük durumu (load pattern) tanımlama", 20, ""),
                        l("Ölü ve hareketli yükler", 18, ""),
                        l("Yük kombinasyonları (TS 500 / TBDY)", 25,
                            "1.4G+1.6Q ve deprem kombinasyonları"),
                        l("Kar ve rüzgâr yükleri", 22, ""),
                        l("Sıcaklık ve oturma etkileri", 18, ""),
                        l("Alan yükünün çubuklara dağıtımı", 20, "")
                    )
                ),
                PackSection(
                    "4. Deprem Analizi", listOf(
                        l("TBDY 2018 esasları", 30,
                            "Türkiye Bina Deprem Yönetmeliği temel kavramları"),
                        l("Eşdeğer deprem yükü yöntemi", 25, ""),
                        l("Mod birleştirme (response spectrum)", 30, ""),
                        l("Tasarım spektrumu tanımlama", 25, ""),
                        l("Kütle kaynağı (mass source) tanımı", 18,
                            "Sık yapılan hata — yanlış kütle yanlış periyot demek"),
                        l("Zaman tanım alanında analiz", 30, ""),
                        l("Göreli kat ötelemesi denetimi", 22, ""),
                        l("Burulma düzensizliği ve A1 kontrolü", 25, "")
                    )
                ),
                PackSection(
                    "5. Analiz ve Sonuç Okuma", listOf(
                        l("Analizi çalıştırma ve hata ayıklama", 20, ""),
                        l("Deformasyon ve mod şekilleri", 20, ""),
                        l("Kesit tesirleri: M, V, N diyagramları", 25, ""),
                        l("Mesnet tepkileri ve denge kontrolü", 18,
                            "Toplam tepki = toplam yük olmalı, ilk kontrol bu"),
                        l("Gerilme konturları (kabuk)", 20, ""),
                        l("Tablo çıktıları ve Excel'e aktarma", 18, "")
                    )
                ),
                PackSection(
                    "6. Betonarme ve Çelik Tasarım", listOf(
                        l("Betonarme tasarım tercihleri (TS 500)", 25, ""),
                        l("Kolon ve kiriş donatı hesabı", 30, ""),
                        l("Çelik tasarım ve kesit optimizasyonu", 28, ""),
                        l("Tasarım denetimi ve oran (D/C) okuma", 22, ""),
                        l("Tasarım çıktılarının raporlanması", 18, "")
                    )
                ),
                PackSection(
                    "7. Uygulamalar", listOf(
                        l("Uygulama: 5 katlı betonarme çerçeve", 50, ""),
                        l("Uygulama: Çelik çatı makası", 40, ""),
                        l("Uygulama: Perdeli-çerçeveli sistem", 50, ""),
                        l("Uygulama: İstinat duvarı modeli", 35, ""),
                        l("Uygulama: Köprü kirişi analizi", 40, "")
                    )
                )
            )
        ),

        // ───────────────────────────── ideCAD / Statik ─────────────────────────────
        PackCourse(
            "ideCAD Statik — Betonarme Proje", "🏢", 2,
            "Türkiye'de yaygın kullanılan yerli statik yazılımı ile uçtan uca proje.",
            listOf(
                PackSection(
                    "1. Giriş", listOf(
                        l("ideCAD arayüzü ve proje kurulumu", 20, ""),
                        l("Yönetmelik ve malzeme seçimi", 18, "TBDY 2018 / TS 500"),
                        l("Akslar ve kat tanımları", 22, ""),
                        l("Mimari plan altlığı okutma (DWG)", 18, "")
                    )
                ),
                PackSection(
                    "2. Taşıyıcı Sistem", listOf(
                        l("Kolon yerleştirme ve boyutlandırma", 25, ""),
                        l("Kiriş tanımlama", 22, ""),
                        l("Döşeme tipleri: plak, kirişsiz, asmolen", 28, ""),
                        l("Perde duvar modelleme", 25, ""),
                        l("Merdiven modelleme", 22, ""),
                        l("Temel tasarımı: tekil, sürekli, radye", 30, "")
                    )
                ),
                PackSection(
                    "3. Yükler ve Analiz", listOf(
                        l("Yük tanımları ve kombinasyonlar", 22, ""),
                        l("Deprem parametreleri girişi", 25,
                            "AFAD deprem haritasından Ss, S1 değerleri"),
                        l("Analiz çalıştırma", 18, ""),
                        l("Yönetmelik denetimleri", 25,
                            "Düzensizlik kontrolleri, göreli ötelenme")
                    )
                ),
                PackSection(
                    "4. Donatı ve Çizimler", listOf(
                        l("Otomatik donatı düzenleme", 25, ""),
                        l("Kolon-kiriş donatı detayları", 28, ""),
                        l("Kalıp planı çıktısı", 22, ""),
                        l("Donatı açılım çizimleri", 25, ""),
                        l("Metraj ve demir listesi", 22, ""),
                        l("Statik hesap raporu üretme", 20, "")
                    )
                )
            )
        ),

        // ───────────────────────────── Excel ─────────────────────────────
        PackCourse(
            "Excel — İnşaat Mühendisi İçin", "📊", 5,
            "Metraj, keşif, hakediş, ilerleme takibi ve hesap tabloları.",
            listOf(
                PackSection(
                    "1. Temeller", listOf(
                        l("Hücre biçimlendirme ve tablo düzeni", 18, ""),
                        l("Mutlak/bağıl referans ($ işareti)", 20,
                            "Formül kopyalarken en kritik konu"),
                        l("Ad tanımlama (named range)", 15, ""),
                        l("Veri doğrulama ve açılır listeler", 18, "")
                    )
                ),
                PackSection(
                    "2. Formüller", listOf(
                        l("TOPLA, ETOPLA, ÇOKETOPLA", 20, ""),
                        l("EĞER ve iç içe koşullar", 22, ""),
                        l("DÜŞEYARA / XLOOKUP", 25,
                            "Poz numarasından birim fiyat çekmek için"),
                        l("İNDİS + KAÇINCI ikilisi", 22, ""),
                        l("YUVARLA, TAVANAYUVARLA (demir boyu hesabı)", 18, ""),
                        l("Metin fonksiyonları: BİRLEŞTİR, PARÇAAL", 18, "")
                    )
                ),
                PackSection(
                    "3. Metraj ve Keşif", listOf(
                        l("Metraj tablosu kurgusu", 25, ""),
                        l("Poz listesi ve birim fiyat bağlama", 25,
                            "Çevre-Şehircilik birim fiyatları"),
                        l("Keşif özeti oluşturma", 22, ""),
                        l("Demir metrajı ve ağırlık hesabı", 28,
                            "Çap² × 0.006165 = kg/m"),
                        l("Beton ve kalıp metrajı", 22, "")
                    )
                ),
                PackSection(
                    "4. Hakediş ve Takip", listOf(
                        l("Hakediş tablosu hazırlama", 30, ""),
                        l("İmalat ilerleme yüzdesi takibi", 22, ""),
                        l("Fiyat farkı hesabı", 25, ""),
                        l("Nakit akış tablosu", 25, "")
                    )
                ),
                PackSection(
                    "5. Analiz ve Görselleştirme", listOf(
                        l("Pivot tablo ile özet çıkarma", 28, ""),
                        l("Grafikler: S-eğrisi, ilerleme grafiği", 25, ""),
                        l("Koşullu biçimlendirme", 18, ""),
                        l("Hedef ara ve senaryo yöneticisi", 20, ""),
                        l("Makro ve VBA'ya giriş", 30, "")
                    )
                )
            )
        ),

        // ───────────────────────────── Civil 3D ─────────────────────────────
        PackCourse(
            "Civil 3D — Altyapı ve Yol", "🛣️", 1,
            "Arazi modelleme, yol geometrisi, kazı-dolgu hesabı ve altyapı hatları.",
            listOf(
                PackSection(
                    "1. Giriş ve Arazi", listOf(
                        l("Civil 3D arayüzü ve Toolspace", 20, ""),
                        l("Nokta (point) verisi içe aktarma", 22,
                            "Total station / GPS verisinden arazi"),
                        l("Yüzey (surface) oluşturma", 25, ""),
                        l("Eş yükselti eğrileri ve etiketleme", 20, ""),
                        l("Yüzey analizleri: eğim, yükseklik", 22, "")
                    )
                ),
                PackSection(
                    "2. Yol Geometrisi", listOf(
                        l("Yatay güzergâh (alignment)", 28, ""),
                        l("Düşey güzergâh (profile)", 28, ""),
                        l("Enkesit (assembly) oluşturma", 25, ""),
                        l("Koridor (corridor) modelleme", 30, ""),
                        l("Enkesit görünümleri üretme", 22, "")
                    )
                ),
                PackSection(
                    "3. Hacim ve Metraj", listOf(
                        l("Kazı-dolgu hacim hesabı", 30,
                            "İki yüzey arası hacim veya enkesit yöntemi"),
                        l("Kütle diyagramı (mass haul)", 25, ""),
                        l("Malzeme listesi ve raporlar", 20, "")
                    )
                ),
                PackSection(
                    "4. Altyapı", listOf(
                        l("Boru ağı (pipe network) tasarımı", 28,
                            "Yağmur suyu ve kanalizasyon hatları"),
                        l("Baca ve boru profilleri", 22, ""),
                        l("Parselleme (parcels)", 22, ""),
                        l("Sayısal arazi modelinden pafta üretimi", 25, "")
                    )
                )
            )
        ),

        // ───────────────────────────── Diğer Yazılımlar ─────────────────────────────
        PackCourse(
            "Diğer Yazılımlar ve Araçlar", "🧰", 6,
            "Sahada ve ofiste işine yarayacak diğer programlara giriş.",
            listOf(
                PackSection(
                    "1. STA4CAD", listOf(
                        l("Arayüz ve proje kurulumu", 20, ""),
                        l("Taşıyıcı sistem girişi", 25, ""),
                        l("Analiz ve donatı çıktıları", 25, "")
                    )
                ),
                PackSection(
                    "2. ETABS", listOf(
                        l("ETABS ve SAP2000 farkı", 15,
                            "ETABS bina odaklı, kat mantığıyla çalışır"),
                        l("Kat kalıp planı girişi", 25, ""),
                        l("Perde ve pier/spandrel tanımı", 25, ""),
                        l("Deprem analizi ve denetimler", 30, "")
                    )
                ),
                PackSection(
                    "3. Plaxis / Geoteknik", listOf(
                        l("Zemin modelleme temelleri", 25, ""),
                        l("Kazı ve iksa analizi", 30, ""),
                        l("Oturma hesabı", 25, "")
                    )
                ),
                PackSection(
                    "4. Proje Yönetimi", listOf(
                        l("MS Project ile iş programı", 30,
                            "Gantt şeması, kritik yol, kaynak atama"),
                        l("Primavera P6 temelleri", 30, ""),
                        l("İlerleme takibi ve S-eğrisi", 22, "")
                    )
                ),
                PackSection(
                    "5. Görselleştirme", listOf(
                        l("SketchUp ile hızlı modelleme", 25, ""),
                        l("Lumion / Twinmotion ile render", 28, ""),
                        l("Photoshop ile pafta sunumu", 22, "")
                    )
                ),
                PackSection(
                    "6. Saha ve Ölçüm", listOf(
                        l("Netcad temelleri", 25, ""),
                        l("GPS ve total station verisi işleme", 25, ""),
                        l("Drone ile fotogrametri", 25, "")
                    )
                )
            )
        )
    )

    /** Toplam ders sayısı. */
    fun lessonCount(): Int = courses.sumOf { c -> c.sections.sumOf { it.lessons.size } }

    /** Toplam süre (dakika). */
    fun totalMinutes(): Int =
        courses.sumOf { c -> c.sections.sumOf { s -> s.lessons.sumOf { it.second } } }

    /**
     * v7.5 — Hangi kursun PDF klasörü var.
     * Kurs sırası (0 tabanlı) -> assets klasör adı.
     * PDF dosyaları ders sırasına göre 001.pdf, 002.pdf ... biçiminde adlandırılır.
     */
    private val pdfFolders = mapOf(
        0 to "autocad",
        1 to "revit"
        // Diğer kurslar hazırlandıkça eklenecek:
        // 2 to "sap2000", 3 to "idecad", ...
    )

    /** Hazır kursları veritabanına ekler. Zaten varsa atlar. Eklenen kurs sayısını döndürür. */
    fun install(context: android.content.Context): Int {
        val existing = Store.loadCourses(context).map { it.title }.toSet()
        var added = 0
        for ((ci, pack) in courses.withIndex()) {
            if (existing.contains(pack.title)) continue
            val course = Store.addCourse(context, pack.title, pack.emoji, pack.colorIndex, pack.desc)
            val folder = pdfFolders[ci]
            var lessonNo = 0
            for (ps in pack.sections) {
                val section = Store.addSection(context, course.id, ps.title)
                for ((lTitle, lMin, lDesc) in ps.lessons) {
                    lessonNo++
                    // PDF klasörü tanımlıysa ders sırasına göre dosya yolu üret
                    val asset = if (folder != null) {
                        String.format(java.util.Locale.US, "dersler/%s/%03d.pdf", folder, lessonNo)
                    } else ""
                    Store.addLesson(context, course.id, section.id, lTitle, lMin, lDesc, asset)
                }
            }
            added++
        }
        return added
    }

    /**
     * v7.5 — Zaten kurulu kurslara PDF yollarını sonradan bağlar.
     * Kullanıcı v7.4'te kursları eklediyse, güncellemede PDF'ler bağlanır.
     */
    fun linkPdfs(context: android.content.Context): Int {
        val allCourses = Store.loadCourses(context)
        val lessons = Store.loadLessons(context)
        var linked = 0
        for ((ci, pack) in courses.withIndex()) {
            val folder = pdfFolders[ci] ?: continue
            val course = allCourses.firstOrNull { it.title == pack.title } ?: continue
            val sections = Store.sectionsOf(context, course.id)
            var lessonNo = 0
            for (section in sections) {
                val mine = lessons.filter { it.sectionId == section.id }.sortedBy { it.order }
                for (lesson in mine) {
                    lessonNo++
                    if (lesson.pdfAsset.isBlank()) {
                        lesson.pdfAsset = String.format(
                            java.util.Locale.US, "dersler/%s/%03d.pdf", folder, lessonNo
                        )
                        linked++
                    }
                }
            }
        }
        if (linked > 0) Store.saveLessons(context, lessons)
        return linked
    }

    /** Verilen asset yolu gerçekten var mı? */
    fun assetExists(context: android.content.Context, path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }

    /** Henüz eklenmemiş hazır kurs var mı? */
    fun hasMissing(context: android.content.Context): Boolean {
        val existing = Store.loadCourses(context).map { it.title }.toSet()
        return courses.any { !existing.contains(it.title) }
    }
}
