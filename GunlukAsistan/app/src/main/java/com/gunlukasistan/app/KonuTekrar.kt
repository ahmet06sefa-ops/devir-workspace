package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v9.0 — Konu maddelerinde aralıklı tekrar (öneri 53).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN BU, LİSTENİN EN ÖNEMLİ MADDESİ
 * ══════════════════════════════════════════════════════════════════
 * Uygulamanın çekirdek vaadi "unutmadan tekrar et". Ama v8.9'a kadar
 * aralıklı tekrar YALNIZCA iki yerde vardı:
 *   · `Hatalarim` — yanlış yapılan quiz soruları (v7.83)
 *   · `QuizStore` — ders bazında quiz tekrarı
 *
 * **Konu maddelerinde hiç yoktu.** Kullanıcı bir maddeyi işaretliyor,
 * madde "bitti" oluyor ve bir daha asla karşısına çıkmıyordu. Oysa
 * öğrenmenin işleyişi tam tersi: bir konuyu bir kez okumak onu
 * öğrenmek değil. Ebbinghaus'un unutma eğrisine göre tek tekrarla
 * öğrenilen bilginin %70'i bir haftada kayboluyor.
 *
 * Yani uygulama "çalıştım" demeyi kolaylaştırıyor ama "öğrendim"i
 * garanti etmiyordu.
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN SM-2, NEDEN LEITNER DEĞİL
 * ══════════════════════════════════════════════════════════════════
 * `Hatalarim` sabit Leitner aralıkları kullanıyor: 1·3·7·16·35 gün.
 * Bu quiz soruları için uygun — cevap ya doğru ya yanlış, ikili.
 *
 * Konu maddesi farklı: "Türev kuralları"nı tekrar ettin, ne kadar
 * hatırladın? "Hiç" ile "tamamen" arasında bir yerdesin. SM-2
 * (SuperMemo 2) tam bunun için tasarlandı:
 *
 *   · Kullanıcı 0-5 arası kalite verir (biz 4 seçenek gösteriyoruz)
 *   · Her maddenin kendi **kolaylık katsayısı** (EF) var
 *   · Zor maddeler sık, kolay maddeler seyrek gelir
 *   · Aralık = önceki aralık × EF (üstel büyüme)
 *
 * Sonuç: 200 maddelik müfredatta günde 8-12 madde tekrar gelir,
 * hepsi birden değil. Zorlandığın konular sık, oturmuşlar seyrek.
 *
 * ── Kaynak ──
 * SM-2, 1987'de P.A. Wozniak tarafından yayımlandı ve Anki'nin de
 * temelini oluşturuyor. Otuz yılı aşkın alan testinden geçmiş.
 *
 * ══════════════════════════════════════════════════════════════════
 * VERİ MODELİ
 * ══════════════════════════════════════════════════════════════════
 * Madde kimliği (`SubItem.id`) → tekrar durumu. `Store.Topic` modeline
 * alan EKLENMEDİ; sebep v8.3'teki `KonuGorunum` ile aynı: o sınıf
 * JSON yedeğe giriyor, Room geçişinde kullanılıyor ve 12 dosyada
 * okunuyor. Ayrı depo hem güvenli hem geri uyumlu.
 */
object KonuTekrar {

    private const val TAG = "KonuTekrar"
    private const val PREF = "konu_tekrar_v1"
    private const val K_KAYIT = "kayitlar_json"
    private const val K_ACIK = "acik"

    /** Kullanıcının verdiği hatırlama kalitesi. */
    const val KALITE_UNUTTUM = 0      // hiç hatırlamadım
    const val KALITE_ZOR = 3          // zorlanarak hatırladım
    const val KALITE_IYI = 4          // hatırladım
    const val KALITE_KOLAY = 5        // çok kolaydı

    /** Kolaylık katsayısının alt sınırı. SM-2 standardı 1.3. */
    private const val EF_MIN = 1.3

    /** Bir maddenin öğrenilmiş sayılması için gereken aralık (gün). */
    private const val OGRENILDI_ESIK = 60

    /** Defterde en fazla kaç madde tutulur. */
    private const val TAVAN = 2000

    // ══════════════════════════════════════════════════════════

    /**
     * Bir konu maddesinin tekrar durumu.
     *
     * @param maddeId `Store.SubItem.id`
     * @param konuId hangi konuya ait (listeleme ve renk için)
     * @param tekrarSayisi kaç kez başarıyla tekrar edildi
     * @param aralik şu anki aralık (gün)
     * @param ef kolaylık katsayısı (1.3 - 2.5+)
     * @param sonrakiGun yyyyMMdd — bu tarihte tekrar gelecek
     * @param sonTekrar son tekrar zamanı (ms)
     * @param toplamTekrar başarısızlar dahil toplam
     * @param unutmaSayisi kaç kez sıfırlandı (zorluk göstergesi)
     */
    data class Durum(
        val maddeId: Long,
        var konuId: Long,
        var baslik: String,
        var tekrarSayisi: Int = 0,
        var aralik: Int = 0,
        var ef: Double = 2.5,
        var sonrakiGun: String = "",
        var sonTekrar: Long = 0L,
        var toplamTekrar: Int = 0,
        var unutmaSayisi: Int = 0
    ) {
        /** Uzun aralığa ulaşmış madde — artık nadiren sorulacak. */
        val ogrenildi: Boolean get() = aralik >= OGRENILDI_ESIK

        /** Zorluk göstergesi: düşük EF + çok unutma = zor madde. */
        val zorMu: Boolean get() = ef < 1.8 || unutmaSayisi >= 3
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun gunEkle(gun: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, gun)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    // ══════════════════════════════════════════════════════════
    // Açma / kapama
    // ══════════════════════════════════════════════════════════

    /**
     * Varsayılan **kapalı**.
     *
     * Mevcut kullanıcıların alışkanlığını bozmamak için: bir maddeyi
     * işaretleyip bitirmek isteyenler öyle devam edebilsin. Açan
     * kişi bilinçli olarak "bunları tekrar etmek istiyorum" demiş olur.
     */
    fun acikMi(c: Context): Boolean = prefs(c).getBoolean(K_ACIK, false)

    fun ac(c: Context, deger: Boolean) {
        prefs(c).edit().putBoolean(K_ACIK, deger).apply()
    }

    // ══════════════════════════════════════════════════════════
    // SM-2 çekirdeği — saf fonksiyon, test edilebilir
    // ══════════════════════════════════════════════════════════

    /**
     * SM-2 algoritmasının bir adımı.
     *
     * ── Algoritma ──
     * ```
     * EF' = EF + (0.1 - (5-q) × (0.08 + (5-q) × 0.02))
     * EF' en az 1.3
     *
     * q < 3  → tekrar sayısı sıfırlanır, aralık 1 gün
     * q >= 3 → n=1: 1 gün · n=2: 6 gün · n>2: önceki × EF
     * ```
     *
     * ── Neden `q < 3` sıfırlıyor ──
     * Wozniak'ın bulgusu: hatırlayamadığın bir şeyi "biraz
     * hatırladım" sayıp aralığı uzatmak öğrenmeyi bozuyor.
     * Baştan başlamak daha hızlı sonuç veriyor.
     *
     * @return (yeniTekrarSayisi, yeniAralik, yeniEf)
     */
    fun sm2(
        tekrarSayisi: Int,
        aralik: Int,
        ef: Double,
        kalite: Int
    ): Triple<Int, Int, Double> {
        val q = kalite.coerceIn(0, 5)

        // Kolaylık katsayısını güncelle
        val yeniEf = (ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(EF_MIN)

        if (q < 3) {
            // Unutuldu: baştan başla
            return Triple(0, 1, yeniEf)
        }

        val yeniN = tekrarSayisi + 1
        val yeniAralik = when (yeniN) {
            1 -> 1
            2 -> 6
            else -> Math.round(aralik * yeniEf).toInt().coerceAtLeast(1)
        }
        // Çok uzun aralıklar anlamsız — 1 yıl tavan
        return Triple(yeniN, yeniAralik.coerceAtMost(365), yeniEf)
    }

    // ══════════════════════════════════════════════════════════
    // Kayıt yönetimi
    // ══════════════════════════════════════════════════════════

    fun hepsi(c: Context): MutableList<Durum> {
        val ham = prefs(c).getString(K_KAYIT, "[]") ?: "[]"
        val liste = mutableListOf<Durum>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Durum(
                        maddeId = o.optLong("maddeId"),
                        konuId = o.optLong("konuId"),
                        baslik = o.optString("baslik", ""),
                        tekrarSayisi = o.optInt("n", 0),
                        aralik = o.optInt("aralik", 0),
                        ef = o.optDouble("ef", 2.5),
                        sonrakiGun = o.optString("sonraki", ""),
                        sonTekrar = o.optLong("sonTekrar", 0L),
                        toplamTekrar = o.optInt("toplam", 0),
                        unutmaSayisi = o.optInt("unutma", 0)
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "Kayıtlar okunamadı", it) }
        return liste
    }

    private fun kaydet(c: Context, liste: List<Durum>) {
        runCatching {
            val kirpik = if (liste.size > TAVAN) {
                // Tavanı aşınca öğrenilmişleri at — onlar zaten nadiren gelir
                liste.sortedBy { if (it.ogrenildi) 0 else 1 }.takeLast(TAVAN)
            } else liste

            val dizi = JSONArray()
            kirpik.forEach { d ->
                dizi.put(
                    JSONObject()
                        .put("maddeId", d.maddeId)
                        .put("konuId", d.konuId)
                        .put("baslik", d.baslik)
                        .put("n", d.tekrarSayisi)
                        .put("aralik", d.aralik)
                        .put("ef", d.ef)
                        .put("sonraki", d.sonrakiGun)
                        .put("sonTekrar", d.sonTekrar)
                        .put("toplam", d.toplamTekrar)
                        .put("unutma", d.unutmaSayisi)
                )
            }
            prefs(c).edit().putString(K_KAYIT, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "Kaydedilemedi", it) }
    }

    fun durum(c: Context, maddeId: Long): Durum? =
        hepsi(c).firstOrNull { it.maddeId == maddeId }

    /**
     * Madde tamamlandığında tekrar programına alır.
     *
     * `TopicsFragment` bir alt maddeyi işaretlediğinde çağrılıyor.
     * İlk tekrar yarına planlanıyor.
     */
    fun programaAl(c: Context, maddeId: Long, konuId: Long, baslik: String) {
        if (!acikMi(c)) return
        val liste = hepsi(c)
        if (liste.any { it.maddeId == maddeId }) return   // zaten var

        liste.add(
            Durum(
                maddeId = maddeId,
                konuId = konuId,
                baslik = baslik,
                tekrarSayisi = 0,
                aralik = 1,
                ef = 2.5,
                sonrakiGun = gunEkle(1),
                sonTekrar = System.currentTimeMillis(),
                toplamTekrar = 0
            )
        )
        kaydet(c, liste)
    }

    /** Madde işareti kaldırılırsa programdan çıkar. */
    fun programdanCikar(c: Context, maddeId: Long) {
        val liste = hepsi(c)
        if (liste.removeAll { it.maddeId == maddeId }) kaydet(c, liste)
    }

    /**
     * Tekrar sonucunu kaydeder ve bir sonraki tarihi hesaplar.
     *
     * @return güncellenmiş durum (kullanıcıya "3 gün sonra" demek için)
     */
    fun tekrarSonucu(c: Context, maddeId: Long, kalite: Int): Durum? {
        val liste = hepsi(c)
        val d = liste.firstOrNull { it.maddeId == maddeId } ?: return null

        val (yeniN, yeniAralik, yeniEf) = sm2(d.tekrarSayisi, d.aralik, d.ef, kalite)
        d.tekrarSayisi = yeniN
        d.aralik = yeniAralik
        d.ef = yeniEf
        d.sonrakiGun = gunEkle(yeniAralik)
        d.sonTekrar = System.currentTimeMillis()
        d.toplamTekrar++
        if (kalite < 3) d.unutmaSayisi++

        kaydet(c, liste)
        return d
    }

    // ══════════════════════════════════════════════════════════
    // Sorgular
    // ══════════════════════════════════════════════════════════

    /** Bugün tekrarı gelen maddeler — zor olanlar önce. */
    fun bugunkuler(c: Context): List<Durum> {
        if (!acikMi(c)) return emptyList()
        val b = bugun()
        return hepsi(c)
            .filter { it.sonrakiGun.isNotBlank() && it.sonrakiGun <= b }
            .sortedWith(compareByDescending<Durum> { it.zorMu }.thenBy { it.sonrakiGun })
    }

    fun bugunkuSayi(c: Context): Int = bugunkuler(c).size

    /** Belirli bir konudaki bekleyen tekrarlar. */
    fun konununTekrarlari(c: Context, konuId: Long): List<Durum> =
        bugunkuler(c).filter { it.konuId == konuId }

    /** Programdaki toplam madde. */
    fun toplamSayi(c: Context): Int = hepsi(c).size

    /** Öğrenilmiş (60+ gün aralığa ulaşmış) madde sayısı. */
    fun ogrenilenSayi(c: Context): Int = hepsi(c).count { it.ogrenildi }

    /**
     * v9.0 · Öneri 55 — Unutma eğrisi tahmini.
     *
     * ── Fikir ──
     * Ebbinghaus'un unutma eğrisi: hatırlama olasılığı zamanla üstel
     * olarak düşer. `R = e^(-t/S)` — t geçen süre, S bellek gücü.
     *
     * Bizde S ≈ aralık (SM-2 zaten belleğin ne kadar dayandığını
     * ölçüyor). Böylece "bu maddeyi şu an ne kadar hatırlıyorsun"
     * tahmini yapılabiliyor.
     *
     * @return 0..100 arası tahmini hatırlama yüzdesi
     */
    fun hatirlamaTahmini(d: Durum): Int {
        if (d.sonTekrar <= 0L || d.aralik <= 0) return 100
        val gecenGun = ((System.currentTimeMillis() - d.sonTekrar) / 86_400_000.0)
        if (gecenGun <= 0) return 100
        // S = aralık; t = geçen gün
        val r = Math.exp(-gecenGun / d.aralik.toDouble())
        return (r * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Yakında unutulacak maddeler (henüz vakti gelmemiş ama
     * hatırlama tahmini düşük).
     *
     * "Şu konuyu 3 gün içinde tekrar et yoksa unutacaksın" uyarısı
     * için.
     */
    fun riskliler(c: Context, esik: Int = 60): List<Durum> {
        if (!acikMi(c)) return emptyList()
        val b = bugun()
        return hepsi(c)
            .filter { it.sonrakiGun > b }              // henüz vakti gelmedi
            .filter { hatirlamaTahmini(it) < esik }    // ama unutuluyor
            .sortedBy { hatirlamaTahmini(it) }
    }

    /**
     * v9.0 · Öneri 57 — Karışık tekrar (interleaving).
     *
     * ── Neden karıştırmak öğrenmeyi güçlendiriyor ──
     * Peş peşe aynı konudan çalışmak (blocking) o an kolay geliyor
     * ama kalıcılığı düşük. Konuları karıştırmak (interleaving) zor
     * geliyor ama uzun vadede belirgin şekilde daha iyi sonuç
     * veriyor — bu "istenen zorluk" (desirable difficulty) etkisi.
     *
     * Bu fonksiyon bugünkü tekrarları konu bazında karıştırıyor:
     * aynı konudan iki madde peş peşe gelmiyor.
     */
    fun karisikSira(c: Context): List<Durum> {
        val liste = bugunkuler(c)
        if (liste.size <= 2) return liste

        // Konuya göre grupla
        val gruplar = liste.groupBy { it.konuId }.values
            .map { it.toMutableList() }
            .sortedByDescending { it.size }
            .toMutableList()

        val sonuc = mutableListOf<Durum>()
        var sonKonu = -1L
        while (gruplar.any { it.isNotEmpty() }) {
            // En çok maddesi kalan ve son eklenenden FARKLI konuyu seç
            val aday = gruplar
                .filter { it.isNotEmpty() }
                .sortedByDescending { it.size }
                .firstOrNull { it.first().konuId != sonKonu }
                ?: gruplar.first { it.isNotEmpty() }   // başka çare yok

            val oge = aday.removeAt(0)
            sonuc.add(oge)
            sonKonu = oge.konuId
        }
        return sonuc
    }

    /** Aralığı okunur metne çevirir. */
    fun araliklMetni(c: Context, gun: Int): String = when {
        gun <= 0 -> c.getString(R.string.kt_bugun)
        gun == 1 -> c.getString(R.string.kt_yarin)
        gun < 7 -> c.getString(R.string.kt_gun_sonra, gun)
        gun < 30 -> c.getString(R.string.kt_hafta_sonra, gun / 7)
        gun < 365 -> c.getString(R.string.kt_ay_sonra, gun / 30)
        else -> c.getString(R.string.kt_yil_sonra)
    }

    // ══════════════════════════════════════════════════════════
    // Bakım
    // ══════════════════════════════════════════════════════════

    /**
     * Artık var olmayan maddelerin kayıtlarını siler.
     *
     * Konu veya madde silindiğinde burada kayıt kalıyor. Açılışta
     * bir kez temizlenmesi yeterli.
     */
    fun temizle(c: Context, mevcutMaddeIdler: Set<Long>) {
        runCatching {
            val liste = hepsi(c)
            val once = liste.size
            liste.removeAll { it.maddeId !in mevcutMaddeIdler }
            if (liste.size != once) kaydet(c, liste)
        }.onFailure { android.util.Log.w(TAG, "temizle", it) }
    }

    /** Tanılama özeti. */
    fun ozet(c: Context): JSONObject = JSONObject().apply {
        runCatching {
            put("acik", acikMi(c))
            put("toplam", toplamSayi(c))
            put("bugun", bugunkuSayi(c))
            put("ogrenilen", ogrenilenSayi(c))
            put("riskli", riskliler(c).size)
        }
    }
}
