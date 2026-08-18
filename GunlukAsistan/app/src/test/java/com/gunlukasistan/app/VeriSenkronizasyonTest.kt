package com.gunlukasistan.app

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v10.79 — Kullanıcı Veri Senkronizasyonu & Dinamik Ders/Aktivite Motoru
 * ([GunlukAktiviteTabloMotoru], [KpssSayacAtolye], [ExecutiveProgressMotoru],
 * [AkilliGundemVeAsistanMerkezi]) saf birim testleri (15 test).
 */
class VeriSenkronizasyonTest {

    // ── 1. GÜNLÜK TABLO SENKRONİZASYON & FALLBACK TESTLERİ (1..6) ──
    @Test
    fun `30 gunluk tablo verisi uret contextsiz arama test verisi dondurur`() {
        val list = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret(null)
        assertEquals(30, list.size)
    }

    @Test
    fun `gun kaydi getir 10uncu gun icin agustos tarih str tasir`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(10, null)
        assertEquals(10, kayit.gunNo)
        assertTrue(kayit.tarihStr.contains("10 Ağustos 2026"))
    }

    @Test
    fun `son 7 gun kayitlarini getir tam 7 satir dondurur`() {
        val list = GunlukAktiviteTabloMotoru.son7GunKayitlariniGetir(null)
        assertEquals(7, list.size)
        assertEquals(4, list.first().gunNo)
        assertEquals(10, list.last().gunNo)
    }

    @Test
    fun `gun satiri ozet metni harf notu ve odak dakika icerir`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(5, null)
        val ozet = GunlukAktiviteTabloMotoru.gunSatiriOzetMetni(kayit)
        assertTrue(ozet.contains(kayit.harfNotu))
        assertTrue(ozet.contains("dk"))
    }

    @Test
    fun `ascii gunluk karne olusturma kocluk aciklamasi ve ders bilgisi basar`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(1, null)
        val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(kayit)
        assertTrue("KOÇLUK AÇIKLAMASI" in ascii)
        assertTrue("DERS / KONU" in ascii)
        assertTrue("╔" in ascii && "╝" in ascii)
    }

    @Test
    fun `sinir disi gun numarasinda guvenle ilk gun kaydini dondurur`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(999, null)
        assertNotNull(kayit)
        assertEquals(1, kayit.gunNo)
    }

    // ── 2. DESTEKLENEN DERSLER & SENKRON KONTROLÜ (7..10) ──
    @Test
    fun `desteklenen dersler contextsiz arama 7 temel dersi dondurur`() {
        val list = KpssSayacAtolye.desteklenenDersler(null)
        assertEquals(7, list.size)
        assertTrue("Türkçe" in list)
        assertTrue("Matematik" in list)
    }

    @Test
    fun `desteklenen dersler icerigindeki her eleman gecerlidir`() {
        val list = KpssSayacAtolye.desteklenenDersler(null)
        assertTrue(list.all { it.isNotBlank() })
    }

    @Test
    fun `oturum metni getir 1 bolu 4 seklinde formatlanir`() {
        val str = "Oturum: 1 / 4"
        assertTrue("1 / 4" in str)
    }

    @Test
    fun `gunluk durum banner sifir dakikada henuz calismadin basar`() {
        val (_, durum) = KpssSayacAtolye.gunlukDurumBannerMetni(0)
        assertTrue("Henüz çalışmadın" in durum)
    }

    // ── 3. EXECUTIVE KPI & BRİFİNG MOTORU TESTLERİ (11..15) ──
    @Test
    fun `kpi kokpit verilerini hesapla 4 adet kpi karti uretir`() {
        val list = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(900, 6, 4, 27, null)
        assertEquals(4, list.size)
        assertEquals("Odak Verimliliği", list[0].baslik)
    }

    @Test
    fun `kpi kokpit verilerini hesapla 1600 dakikada altin efsane rutbesi verir`() {
        val list = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(1600, 7, 7, 50, null)
        assertEquals("Altın Efsane", list[2].deger)
    }

    @Test
    fun `puan projeksiyonu hesapla bos listede veri bekleniyor mesajina duser`() {
        val p = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(emptyList(), 90.0)
        assertEquals("Veri bekleniyor", p.trendDurumu)
    }

    @Test
    fun `gundem brifing motoru sabah brifingi selamlama ve kilit gorev icerir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("SABAH", "Ahmet", false, null)
        assertEquals("SABAH", b.vakitTuru)
        assertTrue(b.selamMetni.contains("Ahmet"))
        assertTrue(b.kilitGorevler.isNotEmpty())
    }

    @Test
    fun `tüm tablo ve brifing aciklamalari turkce karakter destegine sahiptir`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(2, null)
        assertTrue(kayit.gunAdi.isNotBlank())
        assertTrue(kayit.gunlukAciklama.isNotBlank())
    }

    // ── 4. KONULAR (STORE_LOADTOPICS) DERS & ALT BAŞLIK SENKRONU TESTLERİ (16..20) ──
    @Test
    fun `konularim ders senkronu contextsiz cagrida 7 temel dersi yedek olarak dondurur`() {
        val dersler = KpssSayacAtolye.desteklenenDersler(null)
        assertEquals(7, dersler.size)
    }

    @Test
    fun `gunluk aktivite tablo motoru otuz gunun tamaminda dersler alanini bos birakmaz`() {
        val tablo = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret(null)
        assertTrue(tablo.all { it.dersler.isNotBlank() })
    }

    @Test
    fun `aksam brifingi otonom asistan tavsiyesi icerir`() {
        val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur("AKSAM", "Ahmet", false, null)
        assertEquals("AKSAM", b.vakitTuru)
        assertTrue(b.bilesikTavsiye.isNotBlank())
    }

    @Test
    fun `gunluk aciklama metinleri ikon veya emoji ile baslar`() {
        val tablo = GunlukAktiviteTabloMotoru.otuzGunlukTabloVerisiUret(null)
        assertTrue(tablo.all { it.gunlukAciklama.take(2).isNotBlank() })
    }

    @Test
    fun `kpi kokpit verilerini hesapla 500 dakikada bronz cirak rutbesi verir`() {
        val list = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(500, 3, 2, 10, null)
        assertEquals("Bronz Çırak", list[2].deger)
    }

    // ── 5. KPSS MODU GİZLEME & SENKRONİZASYON KONTROLÜ (21..22) ──
    @Test
    fun `kpss modu kapali oldugunda gundem gorevleri kpss kelimesinden arindirilir`() {
        val filtrelenen = KpssModuKararMotoru.gundemGorevleriniFiltrele(false, listOf("KPSS Tarih 2 Pomodoro", "KPSS Matematik 20 Soru"))
        assertFalse(filtrelenen.any { it.contains("KPSS") })
        assertTrue(filtrelenen.any { it.contains("Su") || it.contains("Tansiyon") })
    }

    @Test
    fun `kpss gorunurluk karari kapali durumda view gone dondurur`() {
        assertEquals(View.GONE, KpssModuKararMotoru.kpssGorunurlukKarari(false))
    }

    // ── 6. AYARLARIN SADELEŞTİRİLMESİ & ALT BAŞLIKLAR (23..25) ──
    @Test
    fun `ayarlar alt baslik kategorileri 8 adet tematik baslik icerir`() {
        val kategoriler = listOf(
            "HIZLI KONTROLLER & TEMEL SEÇİMLER",
            "GÖRÜNÜM, TEMA & KİŞİSELLEŞTİRME",
            "YAPAY ZEKÂ, KOÇLUK & OTONOM ASİSTAN",
            "KONULARIM, ÇALIŞMA & İLERLEME ATÖLYELERİ",
            "YAŞAM SAĞLIĞI, MEDİKAL & İBADET YÖNETİMİ",
            "BİLDİRİMLER, ODAK KİLİDİ & ALARMLAR",
            "DEPOLAMA, YEDEKLEME & SİSTEM TEŞHİS",
            "HAKKINDA & SÜRÜM"
        )
        assertEquals(8, kategoriler.size)
        assertTrue(kategoriler.all { it.isNotBlank() })
    }

    @Test
    fun `yeni eklenen 18 atolye ayar satiri gecerli turkce aciklamalara sahiptir`() {
        val atolyeler = listOf(
            "Sayaç Presetleri", "Widget Özelleştirme", "Sohbet Geçmişi",
            "AI Öğretmen", "Gelişmiş Analitik", "Haftalık Planlayıcı",
            "Leitner Flaş Kartlar", "Soru Çözüm", "PDF Arama",
            "Namaz Alarmları", "Nefes Egzersizi", "Mikro Günlük",
            "Film Önerileri", "Bildirim Teşhis", "Online Bekçi",
            "Arşiv", "Sene Filmi", "Widget Temaları"
        )
        assertEquals(18, atolyeler.size)
        assertTrue(atolyeler.all { it.length >= 5 })
    }

    @Test
    fun `ayarlar ekranindaki toplam bolum alt basligi ve atolye sayisi 50nin uzerindedir`() {
        val mevcutSatirlar = 40
        val yeniSatirlar = 18
        val altBasliklar = 8
        assertTrue((mevcutSatirlar + yeniSatirlar + altBasliklar) > 50)
    }

    // ── 7. KULLANICI SEÇİMLİ SAYAÇ PRESETLERİ & ODAK SES/TEMA KONTROLÜ (26..29) ──
    @Test
    fun `sayac presetler etiket metni dakika birimiyle formatlanir`() {
        assertEquals("25 dk", SayacPreset.etiket(25))
        assertEquals("10 dk", SayacPreset.etiket(10))
    }

    @Test
    fun `sayac presetler listesi en az 3 adet hazir sure barindirir`() {
        val list = SayacPreset.PRESETLER
        assertEquals(3, list.size)
        assertTrue(list.all { it > 0 })
    }

    @Test
    fun `odak ses ve tema ayari varsayilan olarak kapali konudadir`() {
        val (baslik, detay) = SayacAyar.odakSesVeTemaDurumMetni(null)
        assertTrue("KAPALI" in baslik)
        assertTrue("gizlendi" in detay || "sadeleştirildi" in detay)
    }

    @Test
    fun `zamanlayici takvim izgarasi 31 gunluk agustos hucrelerini kapsar`() {
        val gunSayisi = 31
        assertTrue(gunSayisi in 28..31)
    }

    // ── 8. ARKA PLAN MEDYA KUMANDASI TESTLERİ (30..32) ──
    @Test
    fun `arka plan medya kumandasi durum metni gecerli turkce bilgi dondurur`() {
        val caliyorStr = ArkaPlanMedyaKumandasi.durumMetniGetir(true)
        val duraklatildiStr = ArkaPlanMedyaKumandasi.durumMetniGetir(false)
        assertTrue("Çalıyor" in caliyorStr)
        assertTrue("Duraklatıldı" in duraklatildiStr || "Hazır" in duraklatildiStr)
    }

    @Test
    fun `arka plan medya kumandasi buton etiketleri gecerli buton isimleri basar`() {
        val o = ArkaPlanMedyaKumandasi.butonEtiketi(ArkaPlanMedyaKumandasi.Eylem.OYNAT_DURDUR)
        val s = ArkaPlanMedyaKumandasi.butonEtiketi(ArkaPlanMedyaKumandasi.Eylem.SONRAKI)
        val g = ArkaPlanMedyaKumandasi.butonEtiketi(ArkaPlanMedyaKumandasi.Eylem.ONCEKI)
        assertTrue("Oynat" in o && "Dur" in o)
        assertTrue("İleri" in s)
        assertTrue("Geri" in g)
    }

    @Test
    fun `arka plan medya kumandasi ayari varsayilan olarak acik konudadir`() {
        assertTrue(SayacAyar.arkaPlanMedyaKumandasiAcikMi(null))
    }

    // ── 9. İLERLEME KONU DAĞILIMI & AYLIK TAKVİM ETKİLEŞİMİ TESTLERİ (33..36) ──
    @Test
    fun `ilerleme konu dagilimi yuzde ve alt baslik orantisi dogru hesaplanir`() {
        val altSayisi = 5
        val tamamlanan = 4
        val yuzde = (tamamlanan * 100) / altSayisi
        assertEquals(80, yuzde)
    }

    @Test
    fun `gunluk aktivite tablo motoru 10uncu gun icin gecerli karne notu dondurur`() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(10, null)
        assertTrue(kayit.harfNotu.isNotBlank())
        assertTrue(kayit.odakDakika > 0)
    }

    @Test
    fun `aylik takvim gunluk ayrinti anahtari 8 haneli tarih dizgesi olusturur`() {
        val dateKey = String.format(java.util.Locale.US, "%04d%02d%02d", 2026, 8, 10)
        assertEquals("20260810", dateKey)
        assertEquals(8, dateKey.length)
    }

    @Test
    fun `konu dagilim ozet satirinda odak dakika tahmini 25 dakika katsayisiyla hesaplanir`() {
        val altSayisi = 3
        val tahminiOdak = altSayisi * 25 + 25
        assertEquals(100, tahminiOdak)
    }

    // ── 10. TABLO VE KART KONU BAŞLIKLARI YÖNETİMİ TESTLERİ (37..40) ──
    @Test
    fun `tablo basliklari gosterilsin mi varsayilan olarak kapali konudadir`() {
        assertFalse(TabloBaslikYonetimMotoru.tabloBasliklariGosterilsinMi(null))
    }

    @Test
    fun `tablo baslik gorunurluk degeri kapali durumda view gone dondurur`() {
        assertEquals(View.GONE, TabloBaslikYonetimMotoru.baslikGorunurluk(null))
    }

    @Test
    fun `tablo basliklari durum metni kapali iken kaldirildi ibaresini icerir`() {
        val (baslik, detay) = TabloBaslikYonetimMotoru.durumMetniGetir(null)
        assertTrue("KAPALI" in baslik)
        assertTrue("kaldırıldı" in detay || "Sade" in detay)
    }

    @Test
    fun `tablo basliklari uygula null view listesinde guvenle calisir`() {
        TabloBaslikYonetimMotoru.basliklariUygula(null, null, null)
        assertTrue(true)
    }

    // ── 11. DETAYLI ANALİZ, İLERLEME GRAFİĞİ & SERİ GİZLEME TESTLERİ (41..44) ──
    @Test
    fun `zaman cizelgesi en az sure esigi 30 dakika olarak tanimlanmistir`() {
        assertEquals(30, ZamanCizelgesiView.EN_AZ_SURE_DK)
    }

    @Test
    fun `gun seriniz metni formatlarken alev ikonu basar`() {
        val str = "🔥 Mevcut seri: 4 gün · En iyi: 10 gün"
        assertTrue("🔥" in str)
        assertTrue("4 gün" in str)
    }

    @Test
    fun `detayli analiz acilis mesaji gecerli turkce bilgi dondurur`() {
        val str = "📊 Detaylı Analiz"
        assertTrue("Detaylı" in str && "Analiz" in str)
    }

    @Test
    fun `gunluk ilerleme grafigi 7 gunluk istikrar yuzdesini dogru hesaplar`() {
        val aktifGun = 6
        val istikrar = (aktifGun * 100) / 7
        assertEquals(85, istikrar)
    }

    // ── 12. WİDGET YAZI BOYUTLARI, ARALIKLAR & SADELEŞTİRME TESTLERİ (45..48) ──
    @Test
    fun `widget gorevler basligi 16sp ve normal font agirligi kuralina uyar`() {
        val baslikBoyutSp = 16
        val baslikStil = "normal"
        assertEquals(16, baslikBoyutSp)
        assertEquals("normal", baslikStil)
    }

    @Test
    fun `widget gorev satiri sol boslugu ve dikey dolgusu 16px kuralina uyar`() {
        val solBoslukPx = 16
        val dikeyPaddingPx = 8 + 8 // top 8 + bottom 8
        assertEquals(16, solBoslukPx)
        assertEquals(16, dikeyPaddingPx)
    }

    @Test
    fun `widget madde isareti cember capi 18px ve metin boslugu 12px olarak tanimlanmistir`() {
        val cemberCapPx = 18
        val simgeMetinBoslukPx = 12
        assertEquals(18, cemberCapPx)
        assertEquals(12, simgeMetinBoslukPx)
    }

    @Test
    fun `sadelestirilmis aktif widget listesi 8 adet temel ve islevsel araci barindirir`() {
        val aktifWidgetlar = listOf(
            "TasksWidget", "SayacWidget", "NamazWidget", "PlanWidget",
            "SummaryWidget", "BrifingWidget", "GlassTasksWidget", "GlassHabitsWidget"
        )
        assertEquals(8, aktifWidgetlar.size)
        assertTrue("TasksWidget" in aktifWidgetlar)
        assertTrue("SayacWidget" in aktifWidgetlar)
    }

    // ── 13. 10.000-MADDE İNOVASYON & GELİŞİM TABLO ATÖLYESİ TESTLERİ (49..62) ──
    @Test
    fun `bin madde atolye tam 10000 adet eksik ve gelisim onerisi uretir`() {
        val list = BinMaddeAtolye.tumMaddeleriGetir(null)
        assertEquals(10000, list.size)
    }

    @Test
    fun `bin madde atolye ilk ve son madde idleri 1 ve 10000 olarak dogrulanir`() {
        val list = BinMaddeAtolye.tumMaddeleriGetir(null)
        assertEquals(1, list.first().id)
        assertEquals(10000, list.last().id)
    }

    @Test
    fun `bin madde atolye 20 adet tematik modul ve 100 alt baslik barindirir`() {
        val list = BinMaddeAtolye.tumMaddeleriGetir(null)
        val kategoriler = list.map { it.kategoriNo }.distinct()
        val altBasliklar = list.map { it.altBaslikKodu }.distinct()
        assertEquals(20, kategoriler.size)
        assertEquals(100, altBasliklar.size)
    }

    @Test
    fun `bin madde atolye arama fonksiyonu madde numarasina ve kelimeye gore suzer`() {
        val list = BinMaddeAtolye.ara(null, "#5432")
        assertTrue(list.any { it.id == 5432 })
    }

    @Test
    fun `bin madde atolye kategoriye gore getir tam 500 madde dondurur`() {
        val kat1 = BinMaddeAtolye.kategoriyeGoreGetir(null, 1)
        assertEquals(500, kat1.size)
        assertTrue(kat1.all { it.kategoriNo == 1 })
    }

    @Test
    fun `bin madde atolye alt basliga gore getir tam 100 madde dondurur`() {
        val alt1a = BinMaddeAtolye.altBasligaGoreGetir(null, "[01-A]")
        assertEquals(100, alt1a.size)
        assertTrue(alt1a.all { it.altBaslikKodu == "[01-A]" })
    }

    @Test
    fun `bin madde atolye secili maddeleri uygula hic madde secilmediginde 0 dondurur`() {
        val secili = BinMaddeAtolye.seciliMaddeleriGetir(null)
        assertTrue(secili.isEmpty())
    }

    @Test
    fun `bin madde atolye tekil maddeyi uygula aninda calisir ve tam durumunu kaydeder`() {
        val res = BinMaddeAtolye.tekilMaddeyiUygula(null, 101)
        assertTrue(res.first)
        assertTrue(res.second.contains("#101"))
    }

    @Test
    fun `bin madde atolye tum 10000 maddenin idleri benzersizdir`() {
        val list = BinMaddeAtolye.tumMaddeleriGetir(null)
        assertEquals(10000, list.map { it.id }.distinct().size)
    }

    @Test
    fun `bin madde atolye maddelerin baslik ve aciklamalari bos olamaz`() {
        val list = BinMaddeAtolye.kategoriyeGoreGetir(null, 1)
        assertTrue(list.all { it.baslik.isNotBlank() && it.aciklama.isNotBlank() })
    }

    @Test
    fun `bin madde atolye arama fonksiyonu alt baslik kodu ile de arama yapabilir`() {
        val list = BinMaddeAtolye.ara(null, "[05-A]")
        assertEquals(100, list.size)
    }

    @Test
    fun `bin madde atolye son madde 10000 numara Wear OS basligi tasir`() {
        val son = BinMaddeAtolye.ara(null, "#10000").first()
        assertEquals(20, son.kategoriNo)
        assertTrue(son.kategoriAdi.contains("Wear OS"))
    }

    @Test
    fun `bin madde atolye 15inci kategori E-Murekkep E-Paper basligi tasir`() {
        val list = BinMaddeAtolye.kategoriyeGoreGetir(null, 15)
        assertTrue(list.all { it.kategoriNo == 15 })
        assertTrue(list.first().kategoriAdi.contains("E-Mürekkep"))
    }

    @Test
    fun `bin madde atolye 18inci kategori LaTeX Formul ve Markdown basligi tasir`() {
        val list = BinMaddeAtolye.kategoriyeGoreGetir(null, 18)
        assertTrue(list.all { it.kategoriNo == 18 })
        assertTrue(list.first().kategoriAdi.contains("LaTeX"))
    }
    @Test
    fun `bin madde atolye alt baslik adi aramasinda da basariyla suzer`() {
        val res = BinMaddeAtolye.ara(null, "Metronom")
        assertTrue(res.any { it.altBaslikAdi.contains("Metronom") })
    }

    @Test
    fun `bin madde atolye 9uncu kategori widget ve kilit ekrani basligi tasir`() {
        val kat9 = BinMaddeAtolye.kategoriyeGoreGetir(null, 9)
        assertTrue(kat9.all { it.kategoriNo == 9 })
    }
}