package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v9.7 — Günlük hayat takibi (öneri 41, 42, 44, 46).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN TEK SINIF — DÖRT ÖZELLİK
 * ══════════════════════════════════════════════════════════════════
 * İlaç hatırlatıcı, fatura takibi, belge geçerliliği ve araç bakımı
 * ilk bakışta dört ayrı özellik. Ama veri modeline indiğinde hepsi
 * **aynı soruyu** soruyor:
 *
 *     "Bir şeyin süresi/miktarı bitmek üzere — beni ne zaman uyar?"
 *
 *   · İlaç    → stok azalıyor (30 hap, günde 2 → 15 gün kaldı)
 *   · Fatura  → son ödeme tarihi yaklaşıyor
 *   · Belge   → geçerlilik bitiyor (ehliyet, pasaport, sigorta)
 *   · Araç    → km eşiğine yaklaşıyor (yağ 10.000 km'de bir)
 *
 * Dördünü ayrı sınıf yazmak dört kez aynı kodu (JSON kaydet/oku,
 * alarm kur, uyarı eşiği, tekrar mantığı) yazmak demekti. Bunun
 * yerine **tek `Kayit` modeli + `Tur` enum'u** kullanıyorum.
 *
 * Fark yalnızca **ölçü biriminde**:
 *   · GÜN bazlı  → fatura, belge, ilaç (tarih)
 *   · KM  bazlı  → araç bakımı (sayaç)
 *
 * `Kayit.kmBazli` bu ayrımı taşıyor. Geri kalan her şey ortak.
 *
 * ══════════════════════════════════════════════════════════════════
 * TASARIM KARARLARI
 * ══════════════════════════════════════════════════════════════════
 *
 * ── 1. Neden ilaç stoğunu ayrı alan tuttum ──
 * "3 gün ilacın kaldı" uyarısı için iki şey gerekiyor: kalan adet
 * ve günlük tüketim. Tarih tek başına yetmiyor çünkü kullanıcı bir
 * doz atlarsa stok bitiş tarihi kayar. Bu yüzden [Kayit.stok] ve
 * [Kayit.gunlukDoz] var; bitiş tarihi bunlardan **hesaplanıyor**,
 * sabit tutulmuyor.
 *
 * ── 2. Neden tekrar aralığı gün cinsinden ──
 * Fatura "aylık" olsa da ay uzunluğu değişken (28-31). `Calendar`
 * ile ay ekleyerek doğru sonucu alıyoruz ([TEKRAR_AY]), ama basit
 * durumlar (haftalık ilaç) için gün sayısı yeterli. İkisini de
 * destekliyorum: negatif değer ay, pozitif değer gün demek.
 *
 * ── 3. Neden uyarı eşiği kayıt başına ──
 * Pasaport için 90 gün önce uyarılmak mantıklı (randevu almak
 * gerekiyor), elektrik faturası için 3 gün yeterli. Tek genel
 * ayar ikisini de kötü karşılardı.
 *
 * ── 4. Sağlık verisi telefondan çıkmıyor ──
 * İlaç adları hassas veri. Hiçbir AI isteğine, hiçbir online
 * senkrona girmiyor. Yalnızca yerel SharedPreferences + kullanıcının
 * kendi yedeği.
 */
object Takip {

    private const val TAG = "Takip"
    private const val PREF = "takip_v1"
    private const val K_KAYITLAR = "kayitlar_json"
    private const val K_ODEME_LOG = "odeme_log_json"

    /** Bellek şişmesin diye üst sınır. */
    private const val TAVAN = 300

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Türler
    // ══════════════════════════════════════════════════════════

    /**
     * Takip türü.
     *
     * @param kod JSON'da saklanan sabit kod — **asla değiştirme**,
     *   eski yedekler bu kodla okunuyor
     * @param emoji listede gösterilen simge
     * @param varsayilanEsik kaç gün/km önceden uyarılsın
     * @param kmBazli true ise tarih yerine kilometre sayacı kullanılır
     */
    enum class Tur(
        val kod: String,
        val emoji: String,
        val adRes: Int,
        val varsayilanEsik: Int,
        val kmBazli: Boolean
    ) {
        ILAC("ilac", "💊", R.string.tk_tur_ilac, 3, false),
        FATURA("fatura", "🧾", R.string.tk_tur_fatura, 3, false),
        BELGE("belge", "🪪", R.string.tk_tur_belge, 45, false),
        ARAC("arac", "🚗", R.string.tk_tur_arac, 500, true);

        companion object {
            fun bul(kod: String?): Tur = entries.firstOrNull { it.kod == kod } ?: FATURA
        }
    }

    /** Tekrar yok. */
    const val TEKRAR_YOK = 0

    /**
     * Negatif değerler AY anlamına gelir: -1 = aylık, -3 = üç aylık,
     * -12 = yıllık. Pozitif değerler gün: 7 = haftalık.
     *
     * Ay ekleme `Calendar.add(MONTH)` ile yapılıyor — 31 Ocak + 1 ay
     * = 28/29 Şubat olarak doğru hesaplanıyor.
     */
    const val TEKRAR_AY = -1
    const val TEKRAR_UC_AY = -3
    const val TEKRAR_YIL = -12

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    /**
     * Tek bir takip kaydı.
     *
     * @param id benzersiz kimlik (oluşturma zamanı)
     * @param tur ilaç / fatura / belge / araç
     * @param ad kullanıcının verdiği ad ("Elektrik", "Ehliyet", "Motor yağı")
     * @param sonrakiMillis bir sonraki tarih (km bazlı kayıtlarda kullanılmaz)
     * @param sonrakiKm bir sonraki kilometre eşiği (yalnız km bazlı)
     * @param tekrar 0 = yok · pozitif = gün · negatif = ay
     * @param tekrarKm km bazlı tekrar aralığı (10000 = her 10.000 km'de)
     * @param esik kaç gün/km kala uyarılsın
     * @param tutar fatura tutarı (kuruş değil, TL — 0 = belirtilmemiş)
     * @param stok ilaç kalan adet (-1 = takip edilmiyor)
     * @param gunlukDoz günde kaç adet (ilaç)
     * @param saatler günün hangi saatlerinde (dakika cinsinden: 8*60=480)
     * @param not serbest not
     * @param arsiv true ise listede gösterilmez ama silinmez
     */
    data class Kayit(
        val id: Long,
        val tur: Tur,
        val ad: String,
        val sonrakiMillis: Long = 0L,
        val sonrakiKm: Long = 0L,
        val tekrar: Int = TEKRAR_YOK,
        val tekrarKm: Long = 0L,
        val esik: Int = 3,
        val tutar: Double = 0.0,
        val stok: Int = -1,
        val gunlukDoz: Int = 0,
        val saatler: List<Int> = emptyList(),
        val not: String = "",
        val arsiv: Boolean = false,
        val olusturma: Long = System.currentTimeMillis()
    ) {
        val kmBazli: Boolean get() = tur.kmBazli

        /**
         * Kaç gün kaldı? Negatif = geçmiş.
         *
         * Gün farkı takvim günü üzerinden hesaplanıyor, ham milisaniye
         * bölmesiyle değil: bugün 23:00 ile yarın 01:00 arası 2 saat
         * ama **1 gün** fark var. Ham bölme 0 derdi.
         */
        fun kalanGun(simdi: Long = System.currentTimeMillis()): Int {
            if (kmBazli || sonrakiMillis <= 0L) return Int.MAX_VALUE
            return gunFarki(simdi, sonrakiMillis)
        }

        /** Kaç km kaldı? Araç kayıtları için. */
        fun kalanKm(mevcutKm: Long): Long {
            if (!kmBazli || sonrakiKm <= 0L) return Long.MAX_VALUE
            return sonrakiKm - mevcutKm
        }

        /**
         * İlaç stoğu kaç gün yeter?
         *
         * Günlük doz 0 ise hesaplanamaz — kullanıcı dozu girmemiş
         * demektir, `null` dönüyoruz ki arayüz "stok takibi kapalı"
         * gösterebilsin. Yanlışlıkla 0'a bölmek de engellenmiş olur.
         */
        fun stokGun(): Int? {
            if (stok < 0 || gunlukDoz <= 0) return null
            return stok / gunlukDoz
        }
    }

    /** Ödeme/işlem geçmişi kaydı — "geçen ay ne kadar ödedim". */
    data class OdemeKaydi(
        val kayitId: Long,
        val ad: String,
        val tutar: Double,
        val millis: Long
    )

    // ══════════════════════════════════════════════════════════
    // Okuma / yazma
    // ══════════════════════════════════════════════════════════

    fun hepsi(context: Context): List<Kayit> = runCatching {
        val ham = p(context).getString(K_KAYITLAR, "[]") ?: "[]"
        val dizi = JSONArray(ham)
        val liste = mutableListOf<Kayit>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(oku(o))
        }
        liste
    }.getOrElse {
        android.util.Log.w(TAG, "Kayıtlar okunamadı", it)
        emptyList()
    }

    /** Arşivlenmemiş kayıtlar — normal liste. */
    fun aktifler(context: Context): List<Kayit> = hepsi(context).filter { !it.arsiv }

    fun turdekiler(context: Context, tur: Tur): List<Kayit> =
        aktifler(context).filter { it.tur == tur }

    fun bul(context: Context, id: Long): Kayit? = hepsi(context).firstOrNull { it.id == id }

    private fun oku(o: JSONObject): Kayit {
        val saatDizi = o.optJSONArray("saatler")
        val saatler = mutableListOf<Int>()
        if (saatDizi != null) {
            for (i in 0 until saatDizi.length()) saatler.add(saatDizi.optInt(i, -1))
        }
        return Kayit(
            id = o.optLong("id", System.currentTimeMillis()),
            tur = Tur.bul(o.optString("tur")),
            ad = o.optString("ad", ""),
            sonrakiMillis = o.optLong("sonraki", 0L),
            sonrakiKm = o.optLong("sonrakiKm", 0L),
            tekrar = o.optInt("tekrar", TEKRAR_YOK),
            tekrarKm = o.optLong("tekrarKm", 0L),
            esik = o.optInt("esik", 3),
            tutar = o.optDouble("tutar", 0.0),
            stok = o.optInt("stok", -1),
            gunlukDoz = o.optInt("doz", 0),
            saatler = saatler.filter { it in 0..1439 },
            not = o.optString("not", ""),
            arsiv = o.optBoolean("arsiv", false),
            olusturma = o.optLong("olusturma", System.currentTimeMillis())
        )
    }

    private fun yaz(k: Kayit): JSONObject = JSONObject()
        .put("id", k.id)
        .put("tur", k.tur.kod)
        .put("ad", k.ad)
        .put("sonraki", k.sonrakiMillis)
        .put("sonrakiKm", k.sonrakiKm)
        .put("tekrar", k.tekrar)
        .put("tekrarKm", k.tekrarKm)
        .put("esik", k.esik)
        .put("tutar", k.tutar)
        .put("stok", k.stok)
        .put("doz", k.gunlukDoz)
        .put("saatler", JSONArray(k.saatler))
        .put("not", k.not)
        .put("arsiv", k.arsiv)
        .put("olusturma", k.olusturma)

    private fun kaydet(context: Context, liste: List<Kayit>) {
        runCatching {
            val kirpik = liste.sortedByDescending { it.olusturma }.take(TAVAN)
            val dizi = JSONArray()
            kirpik.forEach { dizi.put(yaz(it)) }
            p(context).edit().putString(K_KAYITLAR, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "Kayıt yazılamadı", it) }
    }

    /** Yeni kayıt ekler veya var olanı günceller (id eşleşmesine göre). */
    fun kaydet(context: Context, kayit: Kayit) {
        val liste = hepsi(context).toMutableList()
        val idx = liste.indexOfFirst { it.id == kayit.id }
        if (idx >= 0) liste[idx] = kayit else liste.add(kayit)
        kaydet(context, liste)
        runCatching { TakipAlarm.yenidenKur(context) }
    }

    fun sil(context: Context, id: Long) {
        kaydet(context, hepsi(context).filter { it.id != id })
        runCatching { TakipAlarm.iptal(context, id) }
    }

    fun arsivle(context: Context, id: Long, arsiv: Boolean) {
        val liste = hepsi(context).map { if (it.id == id) it.copy(arsiv = arsiv) else it }
        kaydet(context, liste)
        runCatching { TakipAlarm.yenidenKur(context) }
    }

    // ══════════════════════════════════════════════════════════
    // İşlemler
    // ══════════════════════════════════════════════════════════

    /**
     * "Yapıldı / ödendi" işareti.
     *
     * Tekrarlı kayıtlarda bir sonraki tarihi hesaplayıp ileri atar;
     * tekrarsızlarda arşive taşır (silmiyor — geçmiş kaybolmasın).
     *
     * Fatura ise ödeme geçmişine de yazıyor: "bu yıl elektriğe ne
     * kadar ödedim" sorusunun cevabı buradan geliyor.
     */
    fun tamamla(context: Context, id: Long, simdi: Long = System.currentTimeMillis()) {
        val kayit = bul(context, id) ?: return

        if (kayit.tur == Tur.FATURA && kayit.tutar > 0) {
            odemeEkle(context, OdemeKaydi(kayit.id, kayit.ad, kayit.tutar, simdi))
        }

        if (kayit.tekrar == TEKRAR_YOK) {
            arsivle(context, id, true)
            return
        }

        // ── Bir sonraki tarih ──
        //
        // 🔴 ÖZ DENETİMDE BULDUĞUM HATA (v9.7 içinde düzeltildi):
        // İlk yazdığım kod gecikmiş kayıtlarda `temel = simdi`
        // yapıyordu. Sonuç: ayın 5'inde ödenen bir fatura 3 ay
        // gecikip 20'sinde ödendiğinde bir sonraki tarih **ayın
        // 20'si** oluyordu. Faturanın kesim günü kalıcı olarak
        // kayıyordu ve her gecikmede biraz daha kayacaktı.
        //
        // Doğrusu: **özgün tarihten** ileri saymak. Ayın 5'i
        // ayın 5'i olarak kalır, sadece geleceğe taşınır.
        val temel = if (kayit.sonrakiMillis > 0) kayit.sonrakiMillis else simdi
        var yeni = sonrakiTarih(temel, kayit.tekrar)

        // Çok gecikmiş kayıtlar için ileri sar. Üst sınır şart:
        // 2 yıl önce kurulmuş günlük bir hatırlatma 700+ tur
        // döndürürdü. Sınıra dayanırsak bugünden hesaplıyoruz —
        // gün kayması, sonsuz döngüye yeğdir.
        var koruma = 0
        while (yeni <= simdi && koruma < 200) {
            yeni = sonrakiTarih(yeni, kayit.tekrar)
            koruma++
        }
        if (yeni <= simdi) yeni = sonrakiTarih(simdi, kayit.tekrar)

        kaydet(context, kayit.copy(sonrakiMillis = yeni))
    }

    /**
     * Verilen tarihe tekrar aralığını ekler.
     *
     * @param tekrar pozitif = gün, negatif = ay
     */
    fun sonrakiTarih(temel: Long, tekrar: Int): Long {
        if (tekrar == TEKRAR_YOK) return temel
        val c = Calendar.getInstance().apply { timeInMillis = temel }
        if (tekrar < 0) c.add(Calendar.MONTH, -tekrar) else c.add(Calendar.DAY_OF_YEAR, tekrar)
        return c.timeInMillis
    }

    /**
     * İlaç dozu alındı — stoktan düşer.
     *
     * @param adet kaç tane alındı (varsayılan 1)
     */
    fun dozAl(context: Context, id: Long, adet: Int = 1) {
        val kayit = bul(context, id) ?: return
        if (kayit.stok < 0) return
        kaydet(context, kayit.copy(stok = (kayit.stok - adet).coerceAtLeast(0)))
        // v9.8: yalnızca SAYAÇ artıyor — ilaç adı KAYDEDİLMİYOR.
        // Sağlık verisi hassas; "ilaç alındı: 12 kez" güvenli,
        // "Tansiyon ilacı alındı" değil.
        runCatching { Kullanim.eylem(context, Kullanim.Eylem.ILAC_ALINDI) }
    }

    /** İlaç kutusu alındı — stok ekler. */
    fun stokEkle(context: Context, id: Long, adet: Int) {
        val kayit = bul(context, id) ?: return
        val mevcut = if (kayit.stok < 0) 0 else kayit.stok
        kaydet(context, kayit.copy(stok = mevcut + adet))
    }

    /**
     * Araç kilometresi güncellendi.
     *
     * Yeni km, bakım eşiğini geçtiyse bir sonraki eşiği otomatik
     * ileri atıyor — kullanıcı "yağ değiştirdim" dediğinde
     * [bakimYapildi] çağrılır, bu fonksiyon yalnızca sayacı okur.
     */
    fun kmGuncelle(context: Context, km: Long) {
        if (km < 0) return
        p(context).edit().putLong("mevcut_km", km).putLong("km_zaman", System.currentTimeMillis()).apply()
    }

    fun mevcutKm(context: Context): Long = p(context).getLong("mevcut_km", 0L)

    fun kmZamani(context: Context): Long = p(context).getLong("km_zaman", 0L)

    /** Bakım yapıldı — bir sonraki eşiği mevcut km + aralık yapar. */
    fun bakimYapildi(context: Context, id: Long, mevcut: Long = mevcutKm(context)) {
        val kayit = bul(context, id) ?: return
        if (!kayit.kmBazli) return
        val aralik = if (kayit.tekrarKm > 0) kayit.tekrarKm else 10_000L
        kaydet(context, kayit.copy(sonrakiKm = mevcut + aralik))
    }

    // ══════════════════════════════════════════════════════════
    // Ödeme geçmişi
    // ══════════════════════════════════════════════════════════

    private fun odemeEkle(context: Context, o: OdemeKaydi) {
        runCatching {
            val dizi = JSONArray(p(context).getString(K_ODEME_LOG, "[]") ?: "[]")
            dizi.put(
                JSONObject()
                    .put("id", o.kayitId).put("ad", o.ad)
                    .put("tutar", o.tutar).put("t", o.millis)
            )
            // Son 500 ödeme yeter — 40 yıllık aylık fatura geçmişi
            val kirp = if (dizi.length() > 500) {
                val yeni = JSONArray()
                for (i in dizi.length() - 500 until dizi.length()) yeni.put(dizi.get(i))
                yeni
            } else dizi
            p(context).edit().putString(K_ODEME_LOG, kirp.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "Ödeme yazılamadı", it) }
    }

    fun odemeler(context: Context): List<OdemeKaydi> = runCatching {
        val dizi = JSONArray(p(context).getString(K_ODEME_LOG, "[]") ?: "[]")
        val liste = mutableListOf<OdemeKaydi>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(
                OdemeKaydi(
                    o.optLong("id"), o.optString("ad"),
                    o.optDouble("tutar", 0.0), o.optLong("t")
                )
            )
        }
        liste.sortedByDescending { it.millis }
    }.getOrElse { emptyList() }

    /**
     * Aylık abonelik yükü.
     *
     * Farklı periyottaki faturaları **aylık eşdeğere** çeviriyor:
     * yıllık 1200 TL sigorta = ayda 100 TL. Tek tek toplamak
     * yanıltıcı olurdu — kullanıcı "ayda ne kadar gidiyor"
     * bilmek istiyor.
     */
    fun aylikYuk(context: Context): Double {
        var toplam = 0.0
        turdekiler(context, Tur.FATURA).forEach { k ->
            if (k.tutar <= 0) return@forEach
            val aylikCarpan = when {
                k.tekrar == TEKRAR_YOK -> 0.0            // tek seferlik, düzenli yük değil
                k.tekrar < 0 -> 1.0 / (-k.tekrar)        // -1 aylık, -12 yıllık
                k.tekrar > 0 -> 30.44 / k.tekrar         // gün bazlı → ortalama ay
                else -> 0.0
            }
            toplam += k.tutar * aylikCarpan
        }
        return toplam
    }

    /** Bu ay ödenen toplam. */
    fun buAyOdenen(context: Context): Double {
        val c = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val basi = c.timeInMillis
        return odemeler(context).filter { it.millis >= basi }.sumOf { it.tutar }
    }

    // ══════════════════════════════════════════════════════════
    // Uyarılar
    // ══════════════════════════════════════════════════════════

    /**
     * Uyarı seviyesi.
     *   0 = sorun yok · 1 = yaklaşıyor · 2 = bugün/geçti
     */
    data class Uyari(
        val kayit: Kayit,
        val seviye: Int,
        val mesaj: String,
        val siralama: Int
    )

    /**
     * Dikkat gerektiren tüm kayıtlar — aciliyete göre sıralı.
     *
     * Üç ayrı sinyali birleştiriyor:
     *   1. Tarih yaklaştı (fatura, belge, ilaç yenileme)
     *   2. Stok azaldı (ilaç)
     *   3. Km eşiğine yaklaşıldı (araç)
     *
     * Bir ilaç hem tarih hem stok açısından uyarı verebilir; bu
     * durumda **daha acil olanı** gösteriyoruz, iki satır yerine.
     */
    fun uyarilar(context: Context, simdi: Long = System.currentTimeMillis()): List<Uyari> {
        val km = mevcutKm(context)
        val sonuc = mutableListOf<Uyari>()

        aktifler(context).forEach { k ->
            val adaylar = mutableListOf<Uyari>()

            // ── Tarih bazlı ──
            if (!k.kmBazli && k.sonrakiMillis > 0) {
                val kalan = k.kalanGun(simdi)
                if (kalan <= k.esik) {
                    val seviye = if (kalan <= 0) 2 else 1
                    val mesaj = when {
                        kalan < 0 -> context.getString(R.string.tk_gecti, -kalan)
                        kalan == 0 -> context.getString(R.string.tk_bugun)
                        else -> context.getString(R.string.tk_kalan_gun, kalan)
                    }
                    adaylar.add(Uyari(k, seviye, mesaj, kalan))
                }
            }

            // ── Stok bazlı (ilaç) ──
            k.stokGun()?.let { gun ->
                if (gun <= k.esik.coerceAtLeast(3)) {
                    val seviye = if (gun <= 1) 2 else 1
                    adaylar.add(
                        Uyari(k, seviye, context.getString(R.string.tk_stok_gun, gun), gun)
                    )
                }
            }

            // ── Km bazlı (araç) ──
            if (k.kmBazli && k.sonrakiKm > 0 && km > 0) {
                val kalanKm = k.sonrakiKm - km
                if (kalanKm <= k.esik) {
                    val seviye = if (kalanKm <= 0) 2 else 1
                    val mesaj = if (kalanKm <= 0)
                        context.getString(R.string.tk_km_gecti, -kalanKm)
                    else context.getString(R.string.tk_kalan_km, kalanKm)
                    // km'yi güne çevirip sıralamada karşılaştırılabilir yap:
                    // günde ~50 km ortalama varsayımı
                    adaylar.add(Uyari(k, seviye, mesaj, (kalanKm / 50).toInt()))
                }
            }

            // Aynı kayıt için en acil uyarıyı al
            adaylar.minByOrNull { it.siralama }?.let { sonuc.add(it) }
        }

        return sonuc.sortedWith(compareByDescending<Uyari> { it.seviye }.thenBy { it.siralama })
    }

    /** Ana ekran rozeti için: kaç acil uyarı var. */
    fun acilSayisi(context: Context): Int =
        runCatching { uyarilar(context).count { it.seviye == 2 } }.getOrDefault(0)

    /** Tek satırlık özet — ana ekranda / widget'ta gösterilir. */
    fun ozet(context: Context): String? {
        val u = runCatching { uyarilar(context) }.getOrDefault(emptyList())
        if (u.isEmpty()) return null
        val ilk = u.first()
        return if (u.size == 1) "${ilk.kayit.tur.emoji} ${ilk.kayit.ad} — ${ilk.mesaj}"
        else context.getString(
            R.string.tk_ozet_coklu,
            "${ilk.kayit.tur.emoji} ${ilk.kayit.ad} — ${ilk.mesaj}", u.size - 1
        )
    }

    // ══════════════════════════════════════════════════════════
    // Biçimlendirme
    // ══════════════════════════════════════════════════════════

    private val trLocale = Locale("tr", "TR")

    fun tarihMetni(millis: Long): String =
        if (millis <= 0) "—"
        else runCatching {
            SimpleDateFormat("d MMMM yyyy", trLocale).format(Date(millis))
        }.getOrDefault("—")

    fun kisaTarih(millis: Long): String =
        if (millis <= 0) "—"
        else runCatching {
            SimpleDateFormat("d MMM", trLocale).format(Date(millis))
        }.getOrDefault("—")

    fun saatMetni(dakika: Int): String =
        String.format(Locale.US, "%02d:%02d", dakika / 60, dakika % 60)

    fun paraMetni(tutar: Double): String =
        if (tutar <= 0) "—"
        else if (tutar >= 1000) String.format(trLocale, "%,.0f ₺", tutar)
        else String.format(trLocale, "%.2f ₺", tutar).replace(".00 ₺", " ₺")

    fun kmMetni(km: Long): String =
        if (km <= 0) "—" else String.format(trLocale, "%,d km", km)

    /**
     * Tekrar aralığının okunabilir adı.
     */
    fun tekrarAdi(context: Context, tekrar: Int): String = when {
        tekrar == TEKRAR_YOK -> context.getString(R.string.tk_tekrar_yok)
        tekrar == -1 -> context.getString(R.string.tk_tekrar_ay)
        tekrar == -3 -> context.getString(R.string.tk_tekrar_3ay)
        tekrar == -6 -> context.getString(R.string.tk_tekrar_6ay)
        tekrar == -12 -> context.getString(R.string.tk_tekrar_yil)
        tekrar < 0 -> context.getString(R.string.tk_tekrar_nay, -tekrar)
        tekrar == 1 -> context.getString(R.string.tk_tekrar_gun)
        tekrar == 7 -> context.getString(R.string.tk_tekrar_hafta)
        else -> context.getString(R.string.tk_tekrar_ngun, tekrar)
    }

    /**
     * İki zaman arasındaki **takvim günü** farkı.
     *
     * Saat/dakika bileşenleri sıfırlanıp karşılaştırılıyor.
     * Yaz saati geçişlerinde 23 veya 25 saatlik günler oluyor;
     * `Calendar` bunu doğru yönetiyor, ham bölme yönetemiyor.
     */
    fun gunFarki(baslangic: Long, bitis: Long): Int {
        val a = Calendar.getInstance().apply {
            timeInMillis = baslangic
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val b = Calendar.getInstance().apply {
            timeInMillis = bitis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fark = b.timeInMillis - a.timeInMillis
        return Math.round(fark / 86_400_000.0).toInt()
    }

    // ══════════════════════════════════════════════════════════
    // Yedekleme
    // ══════════════════════════════════════════════════════════

    /**
     * NOT: Normal yedeklemede bu fonksiyona GEREK YOK.
     * `PrefYedek` v9.7'den beri `takip_v1` deposunu otomatik
     * yedekliyor. Bu iki fonksiyon seçmeli dışa aktarma
     * (yalnız bu modülü paylaşma) için duruyor.
     */
    fun disaAktar(context: Context): JSONObject = runCatching {
        JSONObject()
            .put("kayitlar", JSONArray(p(context).getString(K_KAYITLAR, "[]") ?: "[]"))
            .put("odemeler", JSONArray(p(context).getString(K_ODEME_LOG, "[]") ?: "[]"))
            .put("km", mevcutKm(context))
    }.getOrDefault(JSONObject())

    fun iceAktar(context: Context, kok: JSONObject?) {
        if (kok == null) return
        runCatching {
            val e = p(context).edit()
            kok.optJSONArray("kayitlar")?.let { e.putString(K_KAYITLAR, it.toString()) }
            kok.optJSONArray("odemeler")?.let { e.putString(K_ODEME_LOG, it.toString()) }
            if (kok.has("km")) e.putLong("mevcut_km", kok.optLong("km", 0L))
            e.apply()
        }.onFailure { android.util.Log.w(TAG, "İçe aktarma başarısız", it) }
    }
}
